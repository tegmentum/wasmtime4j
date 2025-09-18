//! Host function support for bidirectional WebAssembly-Java communication
//!
//! This module provides the native implementation for host functions, allowing Java code
//! to provide functions that can be called from WebAssembly modules. It handles parameter
//! marshalling, type validation, and callback management with defensive programming
//! practices throughout.

use std::sync::{Arc, Mutex, Weak};
use std::collections::HashMap;
use wasmtime::{Func, FuncType, Val, ValType, RefType};
use crate::store::StoreData;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::instance::WasmValue;

/// Compare ValType values since they don't implement PartialEq
fn valtype_eq(a: &ValType, b: &ValType) -> bool {
    match (a, b) {
        (ValType::I32, ValType::I32) => true,
        (ValType::I64, ValType::I64) => true,
        (ValType::F32, ValType::F32) => true,
        (ValType::F64, ValType::F64) => true,
        (ValType::V128, ValType::V128) => true,
        _ => false,
    }
}

/// Registry for managing host function callbacks to prevent GC
static HOST_FUNCTION_REGISTRY: std::sync::OnceLock<Mutex<HashMap<u64, Arc<HostFunction>>>> = std::sync::OnceLock::new();
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
    /// Weak reference to avoid circular dependency with store
    #[allow(dead_code)]
    store_weak: Weak<Mutex<wasmtime::Store<StoreData>>>,
    /// Callback interface for language-specific implementations
    callback: Box<dyn HostFunctionCallback>,
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
            store_weak: self.store_weak.clone(),
            callback: self.callback.clone_callback(),
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
        store_weak: Weak<Mutex<wasmtime::Store<StoreData>>>,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
    ) -> WasmtimeResult<Arc<Self>> {
        let id = NEXT_HOST_FUNCTION_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        
        let host_func = Arc::new(HostFunction {
            id,
            name,
            func_type,
            store_weak,
            callback,
        });

        // Register to prevent GC
        {
            let mut registry = get_host_function_registry().lock().map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to lock host function registry: {}", e),
            })?;
            registry.insert(id, Arc::clone(&host_func));
        }

        log::debug!("Created host function with ID: {}", id);
        Ok(host_func)
    }

    /// Create a Wasmtime Func for this host function
    pub fn create_wasmtime_func(&self, store: &mut wasmtime::Store<StoreData>) -> WasmtimeResult<Func> {
        let host_func_id = self.id;
        let func_type = self.func_type.clone();
        
        let func = Func::new(store, func_type, move |_caller, params, results| {
            // Look up the host function in the registry
            let host_function = {
                let registry = get_host_function_registry().lock().map_err(|e| {
                    anyhow::anyhow!("Failed to lock host function registry: {}", e)
                })?;
                
                registry.get(&host_func_id).cloned().ok_or_else(|| {
                    anyhow::anyhow!("Host function not found in registry: {}", host_func_id)
                })?
            };

            // Marshal parameters from Wasmtime Val to WasmValue
            let wasm_params = marshal_params_from_wasmtime(params)?;
            
            // Execute the callback
            let wasm_results = host_function.callback.execute(&wasm_params).map_err(|e| {
                anyhow::anyhow!("Host function execution failed: {}", e)
            })?;
            
            // Marshal results back to Wasmtime Val
            marshal_results_to_wasmtime(&wasm_results, results)?;
            
            Ok(())
        });

        Ok(func)
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
        // Remove from registry when dropping
        if let Ok(mut registry) = get_host_function_registry().lock() {
            registry.remove(&self.id);
            log::debug!("Removed host function from registry: {}", self.id);
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
    pub fn build(
        self,
        engine: &wasmtime::Engine,
        store_weak: Weak<Mutex<wasmtime::Store<StoreData>>>,
    ) -> WasmtimeResult<Arc<HostFunction>> {
        let callback = self.callback.ok_or_else(|| WasmtimeError::Validation {
            message: "Host function callback not set".to_string(),
        })?;

        let func_type = FuncType::new(engine, self.param_types, self.return_types);
        
        HostFunction::new(self.name, func_type, store_weak, callback)
    }
}

/// Marshal parameters from Wasmtime Val to WasmValue
fn marshal_params_from_wasmtime(params: &[Val]) -> Result<Vec<WasmValue>, anyhow::Error> {
    let mut wasm_params = Vec::with_capacity(params.len());
    
    for param in params {
        let wasm_value = match param {
            Val::I32(v) => WasmValue::I32(*v),
            Val::I64(v) => WasmValue::I64(*v),
            Val::F32(v) => WasmValue::F32(f32::from_bits(*v)),
            Val::F64(v) => WasmValue::F64(f64::from_bits(*v)),
            Val::V128(v) => WasmValue::V128(u128::from(*v).to_le_bytes()),
            Val::FuncRef(_) => return Err(anyhow::anyhow!("FuncRef parameters not yet supported")),
            Val::ExternRef(_) => return Err(anyhow::anyhow!("ExternRef parameters not yet supported")),
            Val::AnyRef(_) => return Err(anyhow::anyhow!("AnyRef parameters not yet supported")),
            Val::ExnRef(_) => return Err(anyhow::anyhow!("ExnRef parameters not yet supported")),
        };
        wasm_params.push(wasm_value);
    }
    
    Ok(wasm_params)
}

/// Marshal results from WasmValue to Wasmtime Val
fn marshal_results_to_wasmtime(
    wasm_results: &[WasmValue],
    results: &mut [Val],
) -> Result<(), anyhow::Error> {
    if wasm_results.len() != results.len() {
        return Err(anyhow::anyhow!(
            "Host function returned {} values, expected {}",
            wasm_results.len(),
            results.len()
        ));
    }

    for (wasm_result, wasmtime_result) in wasm_results.iter().zip(results.iter_mut()) {
        match (wasm_result, wasmtime_result) {
            (WasmValue::I32(v), Val::I32(ref mut r)) => *r = *v,
            (WasmValue::I64(v), Val::I64(ref mut r)) => *r = *v,
            (WasmValue::F32(v), Val::F32(ref mut r)) => *r = v.to_bits(),
            (WasmValue::F64(v), Val::F64(ref mut r)) => *r = v.to_bits(),
            (WasmValue::V128(v), Val::V128(ref mut r)) => *r = wasmtime::V128::from(u128::from_le_bytes(*v)),
            _ => return Err(anyhow::anyhow!(
                "Type mismatch in host function result: {:?}",
                wasm_result
            )),
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
            WasmValue::FuncRef => ValType::Ref(RefType::FUNCREF),
            WasmValue::ExternRef => ValType::Ref(RefType::EXTERNREF),
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
            WasmValue::FuncRef => ValType::Ref(RefType::FUNCREF),
            WasmValue::ExternRef => ValType::Ref(RefType::EXTERNREF),
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
        let registry = get_host_function_registry().lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock host function registry: {}", e),
        })?;
        
        registry.get(&id).cloned().ok_or_else(|| WasmtimeError::InvalidParameter {
            message: format!("Host function not found: {}", id),
        })
    }
    
    /// Core function to remove host function from registry
    pub fn remove_host_function(id: u64) -> WasmtimeResult<()> {
        let mut registry = get_host_function_registry().lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock host function registry: {}", e),
        })?;
        
        registry.remove(&id).ok_or_else(|| WasmtimeError::InvalidParameter {
            message: format!("Host function not found for removal: {}", id),
        })?;
        
        Ok(())
    }
    
    /// Core function to get registry statistics
    pub fn get_registry_stats() -> WasmtimeResult<(usize, u64)> {
        let registry = get_host_function_registry().lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock host function registry: {}", e),
        })?;
        
        let count = registry.len();
        let next_id = NEXT_HOST_FUNCTION_ID.load(std::sync::atomic::Ordering::SeqCst);
        
        Ok((count, next_id))
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use crate::store::Store;
    use wasmtime::ValType;

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
                (WasmValue::I32(a), WasmValue::I32(b)) => {
                    Ok(vec![WasmValue::I32(a + b)])
                }
                _ => Err(WasmtimeError::Validation {
                    message: "Expected i32 parameters".to_string(),
                }),
            }
        }
    }

    #[test]
    fn test_host_function_builder() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");
        let store_weak = Arc::downgrade(&store.inner);
        
        let host_func = HostFunctionBuilder::new("test_add")
            .param(ValType::I32)
            .param(ValType::I32)
            .result(ValType::I32)
            .callback(Box::new(TestCallback))
            .build(&engine, store_weak)
            .expect("Failed to build host function");

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
        let (initial_count, _) = core::get_registry_stats().unwrap();
        
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");
        let store_weak = Arc::downgrade(&store.inner);
        
        // Create host function - should register automatically
        let host_func = HostFunctionBuilder::new("test")
            .param(ValType::I32)
            .result(ValType::I32)
            .callback(Box::new(TestCallback))
            .build(&engine, store_weak)
            .expect("Failed to build host function");
        
        let host_func_id = host_func.id();
        
        // Check registry count increased
        let (count_after_create, _) = core::get_registry_stats().unwrap();
        assert_eq!(count_after_create, initial_count + 1);
        
        // Retrieve from registry
        let retrieved = core::get_host_function(host_func_id);
        assert!(retrieved.is_ok());
        
        // Drop should remove from registry
        drop(host_func);
        
        // Give a moment for Drop to execute
        std::thread::sleep(std::time::Duration::from_millis(10));
        
        // Check registry count decreased
        let (final_count, _) = core::get_registry_stats().unwrap();
        assert_eq!(final_count, initial_count);
    }
}