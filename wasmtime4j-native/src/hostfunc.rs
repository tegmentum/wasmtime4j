//! Host function support for bidirectional WebAssembly-Java communication
//!
//! This module provides the native implementation for host functions, allowing Java code
//! to provide functions that can be called from WebAssembly modules. It handles parameter
//! marshalling, type validation, and callback management with defensive programming
//! practices throughout.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::instance::WasmValue;
use crate::store::StoreData;
use crate::table::core::{
    get_function_reference_with_store_check, register_function_reference, remove_function_reference,
};
use std::collections::HashMap;
use std::mem::MaybeUninit;
use std::sync::{Arc, Mutex};
use wasmtime::{Func, FuncType, HeapType, RefType, Val, ValRaw, ValType};

/// Compare ValType values since they don't implement PartialEq
fn valtype_eq(a: &ValType, b: &ValType) -> bool {
    match (a, b) {
        (ValType::I32, ValType::I32) => true,
        (ValType::I64, ValType::I64) => true,
        (ValType::F32, ValType::F32) => true,
        (ValType::F64, ValType::F64) => true,
        (ValType::V128, ValType::V128) => true,
        (ValType::Ref(ra), ValType::Ref(rb)) => {
            // Compare ref types by heap type category and nullability
            ra.is_nullable() == rb.is_nullable()
                && format!("{:?}", ra.heap_type()) == format!("{:?}", rb.heap_type())
        }
        _ => false,
    }
}

/// Registry for managing host function callbacks to prevent GC
static HOST_FUNCTION_REGISTRY: std::sync::OnceLock<Mutex<HashMap<u64, Arc<HostFunction>>>> =
    std::sync::OnceLock::new();
static NEXT_HOST_FUNCTION_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

fn get_host_function_registry() -> &'static Mutex<HashMap<u64, Arc<HostFunction>>> {
    HOST_FUNCTION_REGISTRY.get_or_init(|| Mutex::new(HashMap::new()))
}

/// Host function wrapper with callback management
pub struct HostFunction {
    /// Unique identifier for this host function
    pub id: u64,
    /// Human-readable name for debugging
    pub name: String,
    /// WebAssembly function type signature
    pub func_type: FuncType,
    /// Callback interface for language-specific implementations
    callback: Box<dyn HostFunctionCallback>,
    /// Optimization hints for caller context usage
    pub caller_context_usage: CallerContextUsage,
    /// Whether this function requires caller context at all
    pub requires_caller_context: bool,
    /// Whether this instance is the registry owner (should remove from registry on drop).
    /// Only the original instance created by new_with_optimization is the owner.
    /// Clones are NOT owners and should not try to remove from registry.
    is_registry_owner: bool,
}

/// Caller context usage patterns for optimization
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum CallerContextUsage {
    /// No caller context features are used (maximum optimization)
    None,
    /// Only export access is used
    ExportsOnly,
    /// Only fuel tracking is used
    FuelOnly,
    /// Only epoch deadlines are used
    EpochOnly,
    /// Export access and fuel tracking are used
    ExportsAndFuel,
    /// All caller context features are used
    Full,
}

impl CallerContextUsage {
    /// Checks if this usage pattern includes export access
    pub fn uses_exports(&self) -> bool {
        matches!(self, Self::ExportsOnly | Self::ExportsAndFuel | Self::Full)
    }

    /// Checks if this usage pattern includes fuel tracking
    pub fn uses_fuel(&self) -> bool {
        matches!(self, Self::FuelOnly | Self::ExportsAndFuel | Self::Full)
    }

    /// Checks if this usage pattern includes epoch deadlines
    pub fn uses_epoch(&self) -> bool {
        matches!(self, Self::EpochOnly | Self::Full)
    }

    /// Checks if no caller context features are used
    pub fn is_none(&self) -> bool {
        matches!(self, Self::None)
    }
}

impl std::fmt::Debug for HostFunction {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("HostFunction")
            .field("id", &self.id)
            .field("name", &self.name)
            .field("func_type", &self.func_type)
            .field("callback", &"<callback>")
            .finish()
    }
}

impl Clone for HostFunction {
    fn clone(&self) -> Self {
        Self {
            id: self.id,
            name: self.name.clone(),
            func_type: self.func_type.clone(),
            callback: self.callback.clone_callback(),
            caller_context_usage: self.caller_context_usage,
            requires_caller_context: self.requires_caller_context,
            // Clones are NOT registry owners - only the original should remove from registry
            is_registry_owner: false,
        }
    }
}

/// Trait for language-specific host function callback implementations
pub trait HostFunctionCallback: Send + Sync {
    /// Execute the host function with parameter marshalling
    fn execute(&self, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>>;

    /// Clone the callback (for implementing Clone on HostFunction)
    fn clone_callback(&self) -> Box<dyn HostFunctionCallback>;
}

/// Result of host function parameter marshalling
#[derive(Debug, Clone)]
pub struct MarshallingResult {
    /// Successfully marshalled parameters
    pub params: Vec<WasmValue>,
    /// Any validation warnings or issues
    pub warnings: Vec<String>,
}

/// Host function type validation and creation utilities
pub struct HostFunctionBuilder {
    name: String,
    param_types: Vec<ValType>,
    return_types: Vec<ValType>,
    callback: Option<Box<dyn HostFunctionCallback + Send + Sync>>,
}

impl HostFunction {
    /// Create a new host function with the given signature and callback
    pub fn new(
        name: String,
        func_type: FuncType,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
    ) -> WasmtimeResult<Arc<Self>> {
        Self::new_with_optimization(
            name,
            func_type,
            callback,
            CallerContextUsage::Full, // Default to full usage for backward compatibility
            true,                     // Assume requires caller context by default
        )
    }

    /// Create a new host function with optimization hints
    pub fn new_with_optimization(
        name: String,
        func_type: FuncType,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
        caller_context_usage: CallerContextUsage,
        requires_caller_context: bool,
    ) -> WasmtimeResult<Arc<Self>> {
        let id = NEXT_HOST_FUNCTION_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        let host_func = Arc::new(HostFunction {
            id,
            name,
            func_type,
            callback,
            caller_context_usage,
            requires_caller_context,
            // Original is the registry owner - it will remove itself from registry on drop
            is_registry_owner: true,
        });

        // Register to prevent GC
        {
            let mut registry =
                get_host_function_registry()
                    .lock()
                    .map_err(|e| WasmtimeError::Concurrency {
                        message: format!("Failed to lock host function registry: {}", e),
                    })?;
            registry.insert(id, Arc::clone(&host_func));
        }

        log::debug!(
            "Created host function with ID: {} (caller context: {:?}, required: {})",
            id,
            caller_context_usage,
            requires_caller_context
        );
        Ok(host_func)
    }

    /// Create a Wasmtime Func for this host function
    pub fn create_wasmtime_func(
        &self,
        store: &mut wasmtime::Store<StoreData>,
    ) -> WasmtimeResult<Func> {
        let host_func_id = self.id;
        let func_type = self.func_type.clone();
        let requires_caller = self.requires_caller_context;
        let usage = self.caller_context_usage;

        let func = Func::new(store, func_type, move |mut caller, params, results| {
            log::debug!(
                "[HOSTFUNC] Host function callback entered, id={}",
                host_func_id
            );

            // Get the store_id from the caller's StoreData for store affinity validation
            let store_id = caller.data().store_id;

            // Look up the host function in the registry
            let host_function = {
                let registry = get_host_function_registry().lock().map_err(|e| {
                    log::error!("[HOSTFUNC] Failed to lock registry: {}", e);
                    wasmtime::Error::msg(format!("Failed to lock host function registry: {}", e))
                })?;

                registry.get(&host_func_id).cloned().ok_or_else(|| {
                    log::error!(
                        "[HOSTFUNC] Host function not found in registry: {}",
                        host_func_id
                    );
                    wasmtime::Error::msg(format!(
                        "Host function not found in registry: {}",
                        host_func_id
                    ))
                })?
            };

            log::debug!(
                "[HOSTFUNC] Found host function, name={}",
                host_function.name
            );

            // Optimize execution based on caller context requirements
            if !requires_caller {
                // Zero-overhead path: no caller context needed
                log::debug!(
                    "[HOSTFUNC] Zero-overhead path, marshaling {} params",
                    params.len()
                );
                let (wasm_params, temp_ids) = marshal_params_from_wasmtime(params, store_id)?;
                let wasm_results = host_function.callback.execute(&wasm_params).map_err(|e| {
                    log::error!("[HOSTFUNC] Callback execution failed: {}", e);
                    wasmtime::Error::msg(format!("Host function execution failed: {}", e))
                })?;
                // Clean up temporary funcref registrations from param marshalling
                cleanup_temp_funcref_ids(&temp_ids);
                log::debug!(
                    "[HOSTFUNC] Callback returned {} results",
                    wasm_results.len()
                );
                marshal_results_to_wasmtime(&wasm_results, results, store_id)?;
            } else {
                // Full caller context path
                log::debug!(
                    "[HOSTFUNC] Full caller context path, marshaling {} params",
                    params.len()
                );
                let (wasm_params, temp_ids) = marshal_params_from_wasmtime(params, store_id)?;

                // Create minimal caller context based on usage pattern
                let _context =
                    create_optimized_caller_context(&mut caller, usage).map_err(|e| {
                        log::error!("[HOSTFUNC] Failed to create caller context: {}", e);
                        wasmtime::Error::msg(format!("Failed to create caller context: {}", e))
                    })?;

                let wasm_results = host_function.callback.execute(&wasm_params).map_err(|e| {
                    log::error!("[HOSTFUNC] Callback execution failed: {}", e);
                    wasmtime::Error::msg(format!("Host function execution failed: {}", e))
                })?;
                // Clean up temporary funcref registrations from param marshalling
                cleanup_temp_funcref_ids(&temp_ids);
                log::debug!(
                    "[HOSTFUNC] Callback returned {} results",
                    wasm_results.len()
                );

                marshal_results_to_wasmtime(&wasm_results, results, store_id)?;
            }

            log::debug!("[HOSTFUNC] Host function callback completed successfully");
            Ok(())
        });

        Ok(func)
    }

    /// Create an unchecked Wasmtime Func for this host function.
    ///
    /// Uses `Func::new_unchecked()` which bypasses per-call type validation for
    /// better performance. The caller is responsible for ensuring type correctness.
    ///
    /// # Safety
    ///
    /// The function type must accurately describe the parameters and results.
    pub fn create_wasmtime_func_unchecked(
        &self,
        store: &mut wasmtime::Store<StoreData>,
    ) -> WasmtimeResult<Func> {
        let host_func_id = self.id;
        let func_type = self.func_type.clone();

        // Capture param/result types for ValRaw marshaling
        let param_types: Vec<ValType> = func_type.params().collect();
        let result_types: Vec<ValType> = func_type.results().collect();

        let func = unsafe {
            Func::new_unchecked(store, func_type, move |mut caller, args_and_results| {
                let store_id = caller.data().store_id;

                // Look up the host function in the registry
                let host_function = {
                    let registry = get_host_function_registry().lock().map_err(|e| {
                        wasmtime::Error::msg(format!(
                            "Failed to lock host function registry: {}",
                            e
                        ))
                    })?;
                    registry.get(&host_func_id).cloned().ok_or_else(|| {
                        wasmtime::Error::msg(format!(
                            "Host function not found in registry: {}",
                            host_func_id
                        ))
                    })?
                };

                // Read params from ValRaw BEFORE writing results (they share the buffer)
                let (wasm_params, temp_ids) =
                    marshal_params_from_valraw(args_and_results, &param_types, store_id)?;

                // Execute the callback
                let wasm_results = host_function.callback.execute(&wasm_params).map_err(|e| {
                    wasmtime::Error::msg(format!("Host function execution failed: {}", e))
                })?;

                // Clean up temporary funcref registrations
                cleanup_temp_funcref_ids(&temp_ids);

                // Write results back to the shared buffer
                marshal_results_to_valraw(
                    &wasm_results,
                    args_and_results,
                    &result_types,
                    store_id,
                )?;

                Ok(())
            })
        };

        Ok(func)
    }

    /// Create an async Wasmtime Func for this host function
    #[cfg(feature = "async")]
    pub fn create_wasmtime_func_async(
        &self,
        store: &mut wasmtime::Store<StoreData>,
    ) -> WasmtimeResult<Func> {
        use std::future::Future;

        let host_func_id = self.id;
        let func_type = self.func_type.clone();
        let requires_caller = self.requires_caller_context;
        let usage = self.caller_context_usage;

        let func = Func::new_async(
            store,
            func_type,
            move |mut caller, params: &[Val], results: &mut [Val]| {
                Box::new(async move {
                    let store_id = caller.data().store_id;

                    // Look up the host function in the registry
                    let host_function = {
                        let registry = get_host_function_registry().lock().map_err(|e| {
                            wasmtime::Error::msg(format!(
                                "Failed to lock host function registry: {}",
                                e
                            ))
                        })?;

                        registry.get(&host_func_id).cloned().ok_or_else(|| {
                            wasmtime::Error::msg(format!(
                                "Host function not found in registry: {}",
                                host_func_id
                            ))
                        })?
                    };

                    // Optimize execution based on caller context requirements
                    if !requires_caller {
                        // Zero-overhead path: no caller context needed
                        let (wasm_params, temp_ids) =
                            marshal_params_from_wasmtime(params, store_id)?;
                        let wasm_results =
                            host_function.callback.execute(&wasm_params).map_err(|e| {
                                wasmtime::Error::msg(format!(
                                    "Host function execution failed: {}",
                                    e
                                ))
                            })?;
                        cleanup_temp_funcref_ids(&temp_ids);
                        marshal_results_to_wasmtime(&wasm_results, results, store_id)?;
                    } else {
                        // Full caller context path
                        let (wasm_params, temp_ids) =
                            marshal_params_from_wasmtime(params, store_id)?;

                        // Create minimal caller context based on usage pattern
                        let _context = create_optimized_caller_context(&mut caller, usage)?;

                        let wasm_results =
                            host_function.callback.execute(&wasm_params).map_err(|e| {
                                wasmtime::Error::msg(format!(
                                    "Host function execution failed: {}",
                                    e
                                ))
                            })?;
                        cleanup_temp_funcref_ids(&temp_ids);

                        marshal_results_to_wasmtime(&wasm_results, results, store_id)?;
                    }

                    Ok(())
                }) as Box<dyn Future<Output = Result<(), wasmtime::Error>> + Send>
            },
        );

        Ok(func)
    }

    /// Get optimization information for this host function
    pub fn get_optimization_info(&self) -> (CallerContextUsage, bool) {
        (self.caller_context_usage, self.requires_caller_context)
    }

    /// Get the function type signature
    pub fn func_type(&self) -> &FuncType {
        &self.func_type
    }

    /// Get the function name
    pub fn name(&self) -> &str {
        &self.name
    }

    /// Get the function ID
    pub fn id(&self) -> u64 {
        self.id
    }
}

impl Drop for HostFunction {
    fn drop(&mut self) {
        // Only the registry owner should remove from registry.
        // Clones should NOT try to remove, as this could cause:
        // 1. Removing the original while it's still in use
        // 2. Deadlock if we're holding the registry lock and the removed Arc's
        //    drop causes another HostFunction drop that tries to lock again
        if !self.is_registry_owner {
            log::debug!(
                "HostFunction clone {} dropped (not registry owner)",
                self.id
            );
            return;
        }

        // Remove from registry when dropping the owner
        // Note: We need to be careful here to avoid deadlock. The registry contains
        // Arc<HostFunction>, so removing might cause another drop if we hold the
        // last reference. We release the lock before that can happen.
        let removed = {
            if let Ok(mut registry) = get_host_function_registry().lock() {
                registry.remove(&self.id)
            } else {
                None
            }
        };
        // Lock is released here, so if `removed` Arc drops and triggers another
        // HostFunction drop, it won't deadlock.
        if removed.is_some() {
            log::debug!("Removed host function {} from registry", self.id);
        }
    }
}

impl HostFunctionBuilder {
    /// Create a new host function builder
    pub fn new(name: impl Into<String>) -> Self {
        Self {
            name: name.into(),
            param_types: Vec::new(),
            return_types: Vec::new(),
            callback: None,
        }
    }

    /// Add a parameter type
    pub fn param(mut self, val_type: ValType) -> Self {
        self.param_types.push(val_type);
        self
    }

    /// Add multiple parameter types
    pub fn params(mut self, val_types: &[ValType]) -> Self {
        self.param_types.extend_from_slice(val_types);
        self
    }

    /// Add a return type
    pub fn result(mut self, val_type: ValType) -> Self {
        self.return_types.push(val_type);
        self
    }

    /// Add multiple return types
    pub fn results(mut self, val_types: &[ValType]) -> Self {
        self.return_types.extend_from_slice(val_types);
        self
    }

    /// Set the callback implementation
    pub fn callback(mut self, callback: Box<dyn HostFunctionCallback + Send + Sync>) -> Self {
        self.callback = Some(callback);
        self
    }

    /// Build the host function
    pub fn build(self, engine: &wasmtime::Engine) -> WasmtimeResult<Arc<HostFunction>> {
        let callback = self.callback.ok_or_else(|| WasmtimeError::Validation {
            message: "Host function callback not set".to_string(),
        })?;

        let func_type = FuncType::new(engine, self.param_types, self.return_types);

        HostFunction::new(self.name, func_type, callback)
    }
}

/// Marshal parameters from Wasmtime Val to WasmValue
///
/// Returns the marshalled parameters and a list of temporary funcref registry IDs
/// that should be cleaned up after the callback completes.
fn marshal_params_from_wasmtime(
    params: &[Val],
    store_id: u64,
) -> Result<(Vec<WasmValue>, Vec<u64>), wasmtime::Error> {
    let mut wasm_params = Vec::with_capacity(params.len());
    let mut temp_funcref_ids = Vec::new();

    for param in params {
        let wasm_value = match param {
            Val::I32(v) => WasmValue::I32(*v),
            Val::I64(v) => WasmValue::I64(*v),
            Val::F32(v) => WasmValue::F32(f32::from_bits(*v)),
            Val::F64(v) => WasmValue::F64(f64::from_bits(*v)),
            Val::V128(v) => WasmValue::V128(u128::from(*v).to_le_bytes()),
            Val::FuncRef(func_ref) => {
                // Extract funcref and register it temporarily to get an ID
                if let Some(func) = func_ref {
                    let id = register_function_reference(func.clone(), store_id).map_err(|e| {
                        WasmtimeError::Execution {
                            message: format!("Failed to register function reference: {:?}", e),
                        }
                    })?;
                    // Track for cleanup after callback completes
                    temp_funcref_ids.push(id);
                    // Convert u64 to i64 for WasmValue storage
                    WasmValue::FuncRef(Some(id as i64))
                } else {
                    WasmValue::FuncRef(None)
                }
            }
            Val::ExternRef(_ext_ref) => {
                // ExternRef data extraction requires Store context
                // For now, just preserve None
                WasmValue::ExternRef(None)
            }
            Val::AnyRef(_) => WasmValue::ExternRef(None),
            Val::ExnRef(_) => WasmValue::ExternRef(None),
            Val::ContRef(_) => WasmValue::ContRef,
        };
        wasm_params.push(wasm_value);
    }

    Ok((wasm_params, temp_funcref_ids))
}

/// Clean up temporary funcref IDs registered during parameter marshalling
fn cleanup_temp_funcref_ids(ids: &[u64]) {
    for &id in ids {
        let _ = remove_function_reference(id);
    }
}

/// Marshal results from WasmValue to Wasmtime Val
fn marshal_results_to_wasmtime(
    wasm_results: &[WasmValue],
    results: &mut [Val],
    store_id: u64,
) -> Result<(), wasmtime::Error> {
    if wasm_results.len() != results.len() {
        return Err(wasmtime::Error::msg(format!(
            "Host function returned {} values, expected {}",
            wasm_results.len(),
            results.len()
        )));
    }

    for (wasm_result, wasmtime_result) in wasm_results.iter().zip(results.iter_mut()) {
        log::debug!(
            "Marshalling result: wasm_result={:?}, wasmtime_result={:?}",
            wasm_result,
            &*wasmtime_result
        );
        *wasmtime_result = match wasm_result {
            WasmValue::I32(v) => Val::I32(*v),
            WasmValue::I64(v) => Val::I64(*v),
            WasmValue::F32(v) => Val::F32(v.to_bits()),
            WasmValue::F64(v) => Val::F64(v.to_bits()),
            WasmValue::V128(v) => Val::V128(wasmtime::V128::from(u128::from_le_bytes(*v))),
            WasmValue::FuncRef(ref_id) => {
                // Convert funcref ID to Func handle using the function registry
                // with store affinity validation
                if let Some(id) = ref_id {
                    let func = get_function_reference_with_store_check(*id as u64, store_id)
                        .map_err(|e| {
                            wasmtime::Error::msg(format!(
                                "Failed to get function reference: {:?}",
                                e
                            ))
                        })?
                        .ok_or_else(|| {
                            wasmtime::Error::msg(format!("Invalid function reference ID: {}", id))
                        })?;
                    Val::FuncRef(Some(func))
                } else {
                    Val::FuncRef(None)
                }
            }
            WasmValue::ExternRef(_ref_id) => {
                // ExternRef creation requires Store context
                // Always return NULL for now
                Val::ExternRef(None)
            }
            WasmValue::ContRef => {
                // ContRef values are opaque with no public API
                Val::ContRef(None)
            }
        };
    }

    Ok(())
}

/// Marshal parameters from ValRaw to WasmValue (for unchecked path)
///
/// Reads params from the `args_and_results` buffer using the provided type information.
/// Must be called BEFORE writing results since params and results share the buffer.
fn marshal_params_from_valraw(
    args_and_results: &mut [MaybeUninit<ValRaw>],
    param_types: &[ValType],
    store_id: u64,
) -> Result<(Vec<WasmValue>, Vec<u64>), wasmtime::Error> {
    let mut wasm_params = Vec::with_capacity(param_types.len());
    let mut temp_funcref_ids = Vec::new();

    for (i, param_type) in param_types.iter().enumerate() {
        let raw = unsafe { args_and_results[i].assume_init_ref() };
        let wasm_value = match param_type {
            ValType::I32 => WasmValue::I32(raw.get_i32()),
            ValType::I64 => WasmValue::I64(raw.get_i64()),
            ValType::F32 => WasmValue::F32(f32::from_bits(raw.get_f32())),
            ValType::F64 => WasmValue::F64(f64::from_bits(raw.get_f64())),
            ValType::V128 => WasmValue::V128(raw.get_v128().to_le_bytes()),
            ValType::Ref(ref_type) => {
                if ref_type.heap_type().is_func() {
                    // FuncRef handling
                    let ptr = raw.get_funcref();
                    if ptr.is_null() {
                        WasmValue::FuncRef(None)
                    } else {
                        // We can't reconstruct a Func from a raw pointer in the unchecked path
                        // without more context, so treat as null for safety
                        WasmValue::FuncRef(None)
                    }
                } else {
                    // ExternRef/AnyRef/etc - data extraction requires Store context
                    WasmValue::ExternRef(None)
                }
            }
        };
        wasm_params.push(wasm_value);
    }

    Ok((wasm_params, temp_funcref_ids))
}

/// Marshal results from WasmValue to ValRaw (for unchecked path)
///
/// Writes results to the beginning of the `args_and_results` buffer.
fn marshal_results_to_valraw(
    wasm_results: &[WasmValue],
    args_and_results: &mut [MaybeUninit<ValRaw>],
    result_types: &[ValType],
    _store_id: u64,
) -> Result<(), wasmtime::Error> {
    if wasm_results.len() != result_types.len() {
        return Err(wasmtime::Error::msg(format!(
            "Host function returned {} values, expected {}",
            wasm_results.len(),
            result_types.len()
        )));
    }

    for (i, wasm_result) in wasm_results.iter().enumerate() {
        let raw = &mut args_and_results[i];
        match wasm_result {
            WasmValue::I32(v) => *raw = MaybeUninit::new(ValRaw::i32(*v)),
            WasmValue::I64(v) => *raw = MaybeUninit::new(ValRaw::i64(*v)),
            WasmValue::F32(v) => *raw = MaybeUninit::new(ValRaw::f32(v.to_bits())),
            WasmValue::F64(v) => *raw = MaybeUninit::new(ValRaw::f64(v.to_bits())),
            WasmValue::V128(v) => *raw = MaybeUninit::new(ValRaw::v128(u128::from_le_bytes(*v))),
            WasmValue::FuncRef(ref_id) => {
                if ref_id.is_some() {
                    // For safety, null funcrefs when going through unchecked path
                    *raw = MaybeUninit::new(ValRaw::funcref(std::ptr::null_mut()));
                } else {
                    *raw = MaybeUninit::new(ValRaw::funcref(std::ptr::null_mut()));
                }
            }
            WasmValue::ExternRef(_) => {
                *raw = MaybeUninit::new(ValRaw::externref(0));
            }
            WasmValue::ContRef => {
                // ContRef is opaque - use null externref as raw representation
                *raw = MaybeUninit::new(ValRaw::externref(0));
            }
        }
    }

    Ok(())
}

/// Validate parameter types match function signature
pub fn validate_parameter_types(
    params: &[WasmValue],
    expected_types: &[ValType],
) -> WasmtimeResult<MarshallingResult> {
    let warnings = Vec::new();

    if params.len() != expected_types.len() {
        return Err(WasmtimeError::Validation {
            message: format!(
                "Parameter count mismatch: got {}, expected {}",
                params.len(),
                expected_types.len()
            ),
        });
    }

    for (i, (param, expected_type)) in params.iter().zip(expected_types.iter()).enumerate() {
        let param_type = match param {
            WasmValue::I32(_) => ValType::I32,
            WasmValue::I64(_) => ValType::I64,
            WasmValue::F32(_) => ValType::F32,
            WasmValue::F64(_) => ValType::F64,
            WasmValue::V128(_) => ValType::V128,
            WasmValue::FuncRef(_) => ValType::Ref(RefType::FUNCREF),
            WasmValue::ExternRef(_) => ValType::Ref(RefType::EXTERNREF),
            WasmValue::ContRef => ValType::Ref(RefType::new(true, HeapType::Cont)),
        };

        if !valtype_eq(&param_type, expected_type) {
            return Err(WasmtimeError::Validation {
                message: format!(
                    "Parameter {} type mismatch: got {:?}, expected {:?}",
                    i, param_type, expected_type
                ),
            });
        }
    }

    Ok(MarshallingResult {
        params: params.to_vec(),
        warnings,
    })
}

/// Validate return types match function signature
pub fn validate_return_types(
    results: &[WasmValue],
    expected_types: &[ValType],
) -> WasmtimeResult<()> {
    if results.len() != expected_types.len() {
        return Err(WasmtimeError::Validation {
            message: format!(
                "Return count mismatch: got {}, expected {}",
                results.len(),
                expected_types.len()
            ),
        });
    }

    for (i, (result, expected_type)) in results.iter().zip(expected_types.iter()).enumerate() {
        let result_type = match result {
            WasmValue::I32(_) => ValType::I32,
            WasmValue::I64(_) => ValType::I64,
            WasmValue::F32(_) => ValType::F32,
            WasmValue::F64(_) => ValType::F64,
            WasmValue::V128(_) => ValType::V128,
            WasmValue::FuncRef(_) => ValType::Ref(RefType::FUNCREF),
            WasmValue::ExternRef(_) => ValType::Ref(RefType::EXTERNREF),
            WasmValue::ContRef => ValType::Ref(RefType::new(true, HeapType::Cont)),
        };

        if !valtype_eq(&result_type, expected_type) {
            return Err(WasmtimeError::Validation {
                message: format!(
                    "Return {} type mismatch: got {:?}, expected {:?}",
                    i, result_type, expected_type
                ),
            });
        }
    }

    Ok(())
}

/// Shared core functions for host function operations used by both JNI and Panama interfaces
pub mod core {
    use super::*;

    /// Core function to create a host function builder
    pub fn create_host_function_builder(name: &str) -> Box<HostFunctionBuilder> {
        Box::new(HostFunctionBuilder::new(name))
    }

    /// Core function to add parameter type to builder
    pub fn builder_add_param(builder: &mut HostFunctionBuilder, val_type: ValType) {
        builder.param_types.push(val_type);
    }

    /// Core function to add return type to builder
    pub fn builder_add_result(builder: &mut HostFunctionBuilder, val_type: ValType) {
        builder.return_types.push(val_type);
    }

    /// Core function to get host function from registry
    pub fn get_host_function(id: u64) -> WasmtimeResult<Arc<HostFunction>> {
        let registry =
            get_host_function_registry()
                .lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock host function registry: {}", e),
                })?;

        registry
            .get(&id)
            .cloned()
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Host function not found: {}", id),
            })
    }

    /// Core function to remove host function from registry
    pub fn remove_host_function(id: u64) -> WasmtimeResult<()> {
        let mut registry =
            get_host_function_registry()
                .lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock host function registry: {}", e),
                })?;

        registry
            .remove(&id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Host function not found for removal: {}", id),
            })?;

        Ok(())
    }

    /// Core function to get registry statistics
    pub fn get_registry_stats() -> WasmtimeResult<(usize, u64)> {
        let registry =
            get_host_function_registry()
                .lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock host function registry: {}", e),
                })?;

        let count = registry.len();
        let next_id = NEXT_HOST_FUNCTION_ID.load(std::sync::atomic::Ordering::SeqCst);

        Ok((count, next_id))
    }

    /// Convert a Func to its raw funcref pointer for low-level handle passing
    pub fn func_to_raw(
        func: &Func,
        store: &mut crate::store::Store,
    ) -> WasmtimeResult<*mut std::os::raw::c_void> {
        let mut store_guard = store.try_lock_store()?;
        Ok(func.to_raw(&mut *store_guard))
    }

    /// Reconstruct a Func from a raw funcref pointer
    ///
    /// Returns None if the raw value is null
    pub fn func_from_raw(
        store: &mut crate::store::Store,
        raw: *mut std::os::raw::c_void,
    ) -> WasmtimeResult<Option<Func>> {
        let mut store_guard = store.try_lock_store()?;
        // Safety: The raw value must have been obtained from func_to_raw
        // on the same store. Invalid values may cause undefined behavior.
        Ok(unsafe { Func::from_raw(&mut *store_guard, raw) })
    }
}

/// Create an optimized caller context based on usage patterns
fn create_optimized_caller_context<T>(
    _caller: &mut wasmtime::Caller<'_, T>,
    usage: CallerContextUsage,
) -> Result<(), wasmtime::Error>
where
    T: Send + 'static,
{
    // This function could create minimal caller context objects
    // based on what features are actually needed.
    // For now, we'll just validate that the caller is available.

    match usage {
        CallerContextUsage::None => {
            // No context needed, maximum optimization
            Ok(())
        }
        CallerContextUsage::ExportsOnly => {
            // Exports are accessible through the caller context
            Ok(())
        }
        CallerContextUsage::FuelOnly => {
            // Fuel tracking is available through the caller context
            Ok(())
        }
        CallerContextUsage::EpochOnly => {
            // Epoch deadline functionality - always available
            Ok(())
        }
        CallerContextUsage::ExportsAndFuel => {
            // Validate both exports and fuel
            // Fuel tracking is available through the caller context
            Ok(())
        }
        CallerContextUsage::Full => {
            // Full validation - all features should be available
            // Fuel tracking is available through the caller context
            Ok(())
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use crate::store::Store;
    use wasmtime::ValType;

    // Use the global shared engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn shared_engine() -> Engine {
        crate::engine::get_shared_engine()
    }

    struct TestCallback;
    impl HostFunctionCallback for TestCallback {
        fn execute(&self, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>> {
            // Simple add function for testing
            if params.len() != 2 {
                return Err(WasmtimeError::Validation {
                    message: "Expected 2 parameters".to_string(),
                });
            }

            match (&params[0], &params[1]) {
                (WasmValue::I32(a), WasmValue::I32(b)) => Ok(vec![WasmValue::I32(a + b)]),
                _ => Err(WasmtimeError::Validation {
                    message: "Expected i32 parameters".to_string(),
                }),
            }
        }

        fn clone_callback(&self) -> Box<dyn HostFunctionCallback> {
            Box::new(TestCallback)
        }
    }

    #[test]
    fn test_host_function_creation() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Create function type
        let func_type = wasmtime::FuncType::new(
            &engine.inner(),
            vec![ValType::I32, ValType::I32],
            vec![ValType::I32],
        );

        // Create host function using Store's method
        let callback = Box::new(TestCallback);
        let (host_func_id, _wasmtime_func) = store
            .create_host_function("test_add".to_string(), func_type, callback)
            .expect("Failed to create host function");

        // Verify function is in registry
        let host_func = core::get_host_function(host_func_id)
            .expect("Failed to retrieve host function from registry");

        assert_eq!(host_func.name(), "test_add");
        assert_eq!(host_func.func_type().params().len(), 2);
        assert_eq!(host_func.func_type().results().len(), 1);
    }

    #[test]
    fn test_parameter_validation() {
        let params = vec![WasmValue::I32(42), WasmValue::I32(100)];
        let expected_types = vec![ValType::I32, ValType::I32];

        let result = validate_parameter_types(&params, &expected_types);
        assert!(result.is_ok());

        let marshalling_result = result.unwrap();
        assert_eq!(marshalling_result.params.len(), 2);
        assert!(marshalling_result.warnings.is_empty());
    }

    #[test]
    fn test_parameter_validation_type_mismatch() {
        let params = vec![WasmValue::I32(42), WasmValue::F32(1.0)];
        let expected_types = vec![ValType::I32, ValType::I32];

        let result = validate_parameter_types(&params, &expected_types);
        assert!(result.is_err());
    }

    #[test]
    fn test_parameter_validation_count_mismatch() {
        let params = vec![WasmValue::I32(42)];
        let expected_types = vec![ValType::I32, ValType::I32];

        let result = validate_parameter_types(&params, &expected_types);
        assert!(result.is_err());
    }

    #[test]
    fn test_return_validation() {
        let results = vec![WasmValue::I32(142)];
        let expected_types = vec![ValType::I32];

        let result = validate_return_types(&results, &expected_types);
        assert!(result.is_ok());
    }

    #[test]
    fn test_registry_operations() {
        // Note: We don't compare absolute registry counts because other tests
        // may be running in parallel and modifying the global registry.
        // Instead, we verify the specific function's lifecycle.

        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Create function type
        let func_type =
            wasmtime::FuncType::new(&engine.inner(), vec![ValType::I32], vec![ValType::I32]);

        // Create host function using Store's method
        let callback = Box::new(TestCallback);
        let (host_func_id, _wasmtime_func) = store
            .create_host_function("test_registry_ops".to_string(), func_type, callback)
            .expect("Failed to create host function");

        // Verify function exists in registry and has correct name
        let retrieved =
            core::get_host_function(host_func_id).expect("Function should exist after creation");
        assert_eq!(retrieved.name(), "test_registry_ops");

        // Remove from registry - retrieved keeps a reference so the drop doesn't
        // happen while the registry lock is held (which would cause deadlock)
        assert!(
            core::remove_host_function(host_func_id).is_ok(),
            "Remove should succeed"
        );

        // Drop retrieved here - this triggers HostFunction::drop which also tries
        // to remove from registry, but the function is already gone so it's a no-op
        drop(retrieved);

        // Verify function no longer exists in registry
        let after_remove = core::get_host_function(host_func_id);
        assert!(
            after_remove.is_err(),
            "Function should not exist after removal"
        );
    }

    #[test]
    fn test_host_function_callback_execution() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Create function type for add function
        let func_type = wasmtime::FuncType::new(
            &engine.inner(),
            vec![ValType::I32, ValType::I32],
            vec![ValType::I32],
        );

        // Create host function
        let callback = Box::new(TestCallback);
        let (host_func_id, _wasmtime_func) = store
            .create_host_function("test_add".to_string(), func_type, callback)
            .expect("Failed to create host function");

        // Get the host function and test callback execution
        let host_func =
            core::get_host_function(host_func_id).expect("Failed to retrieve host function");

        // Test callback execution
        let params = vec![WasmValue::I32(10), WasmValue::I32(20)];
        let results = host_func
            .callback
            .execute(&params)
            .expect("Failed to execute callback");

        assert_eq!(results.len(), 1);
        assert_eq!(results[0], WasmValue::I32(30));

        // Clean up
        core::remove_host_function(host_func_id).unwrap();
    }

    #[test]
    fn test_host_function_error_handling() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Create function type
        let func_type = wasmtime::FuncType::new(
            &engine.inner(),
            vec![ValType::I32, ValType::I32],
            vec![ValType::I32],
        );

        // Create host function
        let callback = Box::new(TestCallback);
        let (host_func_id, _wasmtime_func) = store
            .create_host_function("test_add".to_string(), func_type, callback)
            .expect("Failed to create host function");

        let host_func =
            core::get_host_function(host_func_id).expect("Failed to retrieve host function");

        // Test with wrong parameter count
        let params = vec![WasmValue::I32(10)]; // Only one parameter
        let result = host_func.callback.execute(&params);
        assert!(result.is_err());

        // Test with wrong parameter types
        let params = vec![WasmValue::F32(10.0), WasmValue::I32(20)];
        let result = host_func.callback.execute(&params);
        assert!(result.is_err());

        // Clean up
        core::remove_host_function(host_func_id).unwrap();
    }

    #[test]
    fn test_multiple_host_functions() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Create multiple host functions with different signatures
        let func_type1 = wasmtime::FuncType::new(
            &engine.inner(),
            vec![ValType::I32, ValType::I32],
            vec![ValType::I32],
        );

        let func_type2 =
            wasmtime::FuncType::new(&engine.inner(), vec![ValType::I32], vec![ValType::I32]);

        // Create first host function
        let callback1 = Box::new(TestCallback);
        let (host_func_id1, _) = store
            .create_host_function("add_function".to_string(), func_type1, callback1)
            .expect("Failed to create first host function");

        // Create second host function
        let callback2 = Box::new(TestCallback);
        let (host_func_id2, _) = store
            .create_host_function("echo_function".to_string(), func_type2, callback2)
            .expect("Failed to create second host function");

        // Verify both functions exist and have different IDs
        assert_ne!(host_func_id1, host_func_id2);

        let host_func1 = core::get_host_function(host_func_id1).unwrap();
        let host_func2 = core::get_host_function(host_func_id2).unwrap();

        assert_eq!(host_func1.name(), "add_function");
        assert_eq!(host_func2.name(), "echo_function");
        assert_eq!(host_func1.func_type().params().len(), 2);
        assert_eq!(host_func2.func_type().params().len(), 1);

        // Clean up
        core::remove_host_function(host_func_id1).unwrap();
        core::remove_host_function(host_func_id2).unwrap();
    }
}
