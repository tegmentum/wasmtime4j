//! WebAssembly instance management with comprehensive import/export handling
//!
//! This module provides Instance lifecycle management with complete import resolution,
//! export access, resource cleanup, and defensive programming practices for safe execution.

use crate::data_segment::DataSegmentManager;
use crate::element_segment::ElementSegmentManager;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::interop::ReentrantLock;
use crate::module::{ExportKind, FunctionSignature, ImportKind, Module, ModuleValueType};
use crate::store::{Store, StoreData};
use std::collections::HashMap;
use std::sync::Arc;
use std::time::Instant;
use wasmtime::{
    Extern, ExternType, Func, FuncType, Global, Instance as WasmtimeInstance, Memory, SharedMemory,
    Table, Val, ValType as WasmtimeValType,
};

/// Extracts the full error chain from an anyhow::Error to capture nested error messages.
///
/// When a host function traps, Wasmtime wraps the error in a Trap. The top-level message
/// is just "error while executing at wasm backtrace", but the actual error message from
/// the host function is nested in the error chain. This function extracts all messages
/// to ensure the original error (like "test-panic") is included.
fn extract_error_chain(e: &anyhow::Error) -> String {
    use std::fmt::Write;

    let mut message = String::new();
    let _ = write!(message, "{}", e);

    // Walk the error chain to find nested error messages
    let mut source = e.source();
    while let Some(err) = source {
        let err_msg = err.to_string();
        // Skip generic messages that don't add value
        if !err_msg.is_empty()
            && !err_msg.starts_with("error while executing")
            && !err_msg.contains("wasm backtrace")
        {
            // Check if this message is already included (avoid duplicates)
            if !message.contains(&err_msg) {
                let _ = write!(message, ": {}", err_msg);
            }
        }
        source = err.source();
    }

    message
}

/// Thread-safe wrapper around Wasmtime instance with comprehensive lifecycle management
///
/// CRITICAL: Uses custom ReentrantLock to allow same-thread reentrant access during WASM execution.
#[derive(Debug)]
pub struct Instance {
    inner: Arc<ReentrantLock<WasmtimeInstance>>,
    metadata: InstanceMetadata,
    imports_map: HashMap<String, ImportBinding>,
    exports_map: HashMap<String, ExportBinding>,
    /// Element segment manager for table.init() support
    element_segment_manager: Arc<ElementSegmentManager>,
    /// Data segment manager for memory.init() support
    data_segment_manager: Arc<DataSegmentManager>,
}

/// Lifecycle state of a WebAssembly instance
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InstanceState {
    /// Instance is being created but not yet ready for use
    Creating = 0,
    /// Instance has been successfully created and is ready for use
    Created = 1,
    /// Instance is currently executing WebAssembly code
    Running = 2,
    /// Instance execution has been suspended or paused
    Suspended = 3,
    /// Instance is in an error state due to execution failure
    Error = 4,
    /// Instance has been explicitly disposed and resources cleaned up
    Disposed = 5,
    /// Instance is being destroyed (cleanup in progress)
    Destroying = 6,
}

/// Instance metadata and resource tracking
#[derive(Debug, Clone)]
pub struct InstanceMetadata {
    /// Module name or identifier
    pub name: String,
    /// Timestamp when this instance was created
    pub created_at: Instant,
    /// Number of exported functions
    pub export_count: usize,
    /// Number of imported functions
    pub import_count: usize,
    /// Memory usage in bytes
    pub memory_bytes: usize,
    /// Function call count for performance tracking
    pub function_calls: u64,
    /// Whether this instance has been disposed
    pub disposed: bool,
    /// Current lifecycle state
    pub state: InstanceState,
    /// Thread ID that created this instance
    pub creator_thread_id: std::thread::ThreadId,
    /// Whether cleanup has been performed
    pub cleaned_up: bool,
}

/// Import binding information for validation and resolution
#[derive(Debug, Clone)]
pub struct ImportBinding {
    /// Module name that provides this import
    pub module: String,
    /// Name within the providing module
    pub name: String,
    /// Type of the imported item
    pub import_type: ImportKind,
    /// Whether this import has been resolved
    pub resolved: bool,
}

/// Export binding information for type-safe invocation
#[derive(Debug, Clone)]
pub struct ExportBinding {
    /// Name of the exported item
    pub name: String,
    /// Type of the exported item
    pub export_type: ExportKind,
    /// Whether this export is currently accessible
    pub accessible: bool,
}

/// Parameter value for WebAssembly function calls
#[derive(Debug, Clone, PartialEq)]
pub enum WasmValue {
    /// 32-bit integer
    I32(i32),
    /// 64-bit integer
    I64(i64),
    /// 32-bit floating point
    F32(f32),
    /// 64-bit floating point
    F64(f64),
    /// 128-bit SIMD vector (as bytes)
    V128([u8; 16]),
    /// External reference - stores host object reference ID
    ExternRef(Option<i64>),
    /// Function reference - stores function reference ID
    FuncRef(Option<i64>),
}

/// FFI-safe representation of WasmValue for cross-language interop.
/// Layout: 4-byte tag + 16-byte value (total 20 bytes).
/// Tags: 0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FuncRef, 6=ExternRef
#[repr(C)]
#[derive(Clone, Copy)]
pub struct FfiWasmValue {
    /// Type tag
    pub tag: i32,
    /// Value bytes (interpretation depends on tag)
    pub value: [u8; 16],
}

impl FfiWasmValue {
    /// Create an FfiWasmValue from a WasmValue
    pub fn from_wasm_value(wv: &WasmValue) -> Self {
        let mut value = [0u8; 16];
        let tag = match wv {
            WasmValue::I32(v) => {
                value[..4].copy_from_slice(&v.to_ne_bytes());
                0
            }
            WasmValue::I64(v) => {
                value[..8].copy_from_slice(&v.to_ne_bytes());
                1
            }
            WasmValue::F32(v) => {
                value[..4].copy_from_slice(&v.to_ne_bytes());
                2
            }
            WasmValue::F64(v) => {
                value[..8].copy_from_slice(&v.to_ne_bytes());
                3
            }
            WasmValue::V128(bytes) => {
                value.copy_from_slice(bytes);
                4
            }
            WasmValue::FuncRef(opt) => {
                if let Some(id) = opt {
                    value[..8].copy_from_slice(&id.to_ne_bytes());
                }
                5 // FuncRef uses tag 5
            }
            WasmValue::ExternRef(opt) => {
                if let Some(id) = opt {
                    value[..8].copy_from_slice(&id.to_ne_bytes());
                }
                6 // ExternRef uses tag 6
            }
        };
        FfiWasmValue { tag, value }
    }

    /// Convert to a WasmValue
    ///
    /// # Safety Note
    /// The `try_into().expect()` calls below are safe because `self.value` is always
    /// a `[u8; 16]` array, so slicing to 4 or 8 bytes always succeeds.
    pub fn to_wasm_value(&self) -> WasmValue {
        match self.tag {
            0 => {
                // I32
                let v = i32::from_ne_bytes(
                    self.value[..4]
                        .try_into()
                        .expect("value is [u8; 16], slice to 4 always valid"),
                );
                WasmValue::I32(v)
            }
            1 => {
                // I64
                let v = i64::from_ne_bytes(
                    self.value[..8]
                        .try_into()
                        .expect("value is [u8; 16], slice to 8 always valid"),
                );
                WasmValue::I64(v)
            }
            2 => {
                // F32
                let v = f32::from_ne_bytes(
                    self.value[..4]
                        .try_into()
                        .expect("value is [u8; 16], slice to 4 always valid"),
                );
                WasmValue::F32(v)
            }
            3 => {
                // F64
                let v = f64::from_ne_bytes(
                    self.value[..8]
                        .try_into()
                        .expect("value is [u8; 16], slice to 8 always valid"),
                );
                WasmValue::F64(v)
            }
            4 => {
                // V128
                let mut bytes = [0u8; 16];
                bytes.copy_from_slice(&self.value);
                WasmValue::V128(bytes)
            }
            5 => {
                // FuncRef
                let id = i64::from_ne_bytes(
                    self.value[..8]
                        .try_into()
                        .expect("value is [u8; 16], slice to 8 always valid"),
                );
                if id == 0 {
                    WasmValue::FuncRef(None)
                } else {
                    WasmValue::FuncRef(Some(id))
                }
            }
            6 => {
                // ExternRef
                let id = i64::from_ne_bytes(
                    self.value[..8]
                        .try_into()
                        .expect("value is [u8; 16], slice to 8 always valid"),
                );
                if id == 0 {
                    WasmValue::ExternRef(None)
                } else {
                    WasmValue::ExternRef(Some(id))
                }
            }
            _ => WasmValue::I32(0), // Default fallback
        }
    }
}

/// Result from WebAssembly function execution
#[derive(Debug)]
pub struct ExecutionResult {
    /// Return values from the function
    pub values: Vec<WasmValue>,
    /// Fuel consumed during execution (if enabled)
    pub fuel_consumed: Option<u64>,
    /// Execution time in nanoseconds
    pub execution_time_ns: u64,
}

impl Instance {
    /// Create a new WebAssembly instance with comprehensive import resolution and validation
    pub fn new(store: &mut Store, module: &Module, imports: &[Extern]) -> WasmtimeResult<Self> {
        // Validate inputs
        module.validate()?;

        // Validate imports against module requirements
        let required_imports = module.required_imports();
        if imports.len() != required_imports.len() {
            return Err(WasmtimeError::ImportExport {
                message: format!(
                    "Import count mismatch: expected {}, got {}",
                    required_imports.len(),
                    imports.len()
                ),
            });
        }

        // Create instance with defensive error handling
        // CRITICAL: Use direct Store access with ReentrantMutex for reentrant access
        let mut store_guard = store.try_lock_store()?;

        // CRITICAL FIX for cross-Engine instantiation error:
        // Wasmtime validates Module/Store compatibility using Arc::ptr_eq() on the Engine Arc.
        // Even though Module and Store are created from the same wasmtime4j Engine wrapper,
        // Wasmtime's Module::new() and Store::new() internally clone the Engine Arc differently,
        // resulting in different Arc pointers.
        //
        // SOLUTION: The Store wrapper maintains a reference to the wasmtime4j Engine (store.engine),
        // which should be the SAME Engine instance used to compile the Module.
        // Since both Module and Store hold references to the same wasmtime4j Engine wrapper,
        // and that wrapper contains the Arc<WasmtimeEngine>, the Arc::ptr_eq() check should pass.
        //
        // The previous Linker approach was a workaround that introduced its own bugs.
        // Direct Instance::new() is the correct approach when Arc sharing is handled properly.
        let instance =
            WasmtimeInstance::new(&mut *store_guard, module.inner(), imports).map_err(|e| {
                WasmtimeError::Instance {
                    message: format!("Failed to create instance: {}", e),
                }
            })?;

        // Build comprehensive metadata and mappings
        // Need to create a mutable context from the store
        use wasmtime::AsContextMut;
        let (metadata, imports_map, exports_map) = Self::build_instance_data(
            &instance,
            &mut (*store_guard).as_context_mut(),
            module,
            imports.len(),
        )?;

        // Create element segment manager from module's element segments
        let element_segment_manager =
            Arc::new(ElementSegmentManager::new(module.element_segments.clone()));

        // Create data segment manager from module's data segments
        let data_segment_manager = Arc::new(DataSegmentManager::new(module.data_segments.clone()));

        Ok(Instance {
            inner: Arc::new(ReentrantLock::new(instance)),
            metadata,
            imports_map,
            exports_map,
            element_segment_manager,
            data_segment_manager,
        })
    }

    /// Create instance with validated import resolution
    pub fn new_with_imports(
        store: &mut Store,
        module: &Module,
        imports: &HashMap<String, HashMap<String, Extern>>,
    ) -> WasmtimeResult<Self> {
        // Validate all required imports are provided
        let required_imports = module.required_imports();
        let mut resolved_imports = Vec::new();

        for import_info in required_imports {
            let module_imports =
                imports
                    .get(&import_info.module)
                    .ok_or_else(|| WasmtimeError::ImportExport {
                        message: format!("Missing import module: {}", import_info.module),
                    })?;

            let import_item = module_imports.get(&import_info.name).ok_or_else(|| {
                WasmtimeError::ImportExport {
                    message: format!(
                        "Missing import: {}.{}",
                        import_info.module, import_info.name
                    ),
                }
            })?;

            // Validate import type compatibility
            Self::validate_import_compatibility(&import_info.import_type, import_item, store)?;

            resolved_imports.push(import_item.clone());
        }

        Self::new(store, module, &resolved_imports)
    }

    /// Create instance with no imports (for simple modules)
    pub fn new_without_imports(store: &mut Store, module: &Module) -> WasmtimeResult<Self> {
        Self::new(store, module, &[])
    }

    /// Wrap an existing wasmtime::Instance (for use with Linker)
    ///
    /// This constructor is used when the instance was created by a Linker,
    /// which automatically resolves and applies imports.
    pub fn from_wasmtime_instance(
        wasmtime_instance: WasmtimeInstance,
        store: &mut Store,
        module: &Module,
    ) -> WasmtimeResult<Self> {
        // Build metadata using the module info
        let mut store_guard = store.try_lock_store()?;
        use wasmtime::AsContextMut;
        let (metadata, imports_map, exports_map) = Self::build_instance_data(
            &wasmtime_instance,
            &mut (*store_guard).as_context_mut(),
            module,
            0, // Import count is 0 since linker handled them
        )?;
        drop(store_guard);

        // Create element segment manager from module's element segments
        let element_segment_manager =
            Arc::new(ElementSegmentManager::new(module.element_segments.clone()));

        // Create data segment manager from module's data segments
        let data_segment_manager = Arc::new(DataSegmentManager::new(module.data_segments.clone()));

        Ok(Instance {
            inner: Arc::new(ReentrantLock::new(wasmtime_instance)),
            metadata,
            imports_map,
            exports_map,
            element_segment_manager,
            data_segment_manager,
        })
    }

    /// Build comprehensive instance metadata and binding maps
    fn build_instance_data(
        _instance: &WasmtimeInstance,
        _ctx: &mut wasmtime::StoreContextMut<StoreData>,
        module: &Module,
        _import_count: usize,
    ) -> WasmtimeResult<(
        InstanceMetadata,
        HashMap<String, ImportBinding>,
        HashMap<String, ExportBinding>,
    )> {
        // Build export bindings map by querying wasmtime module directly
        // This works correctly for both compiled and deserialized modules
        // (deserialized modules have empty metadata, but wasmtime module has exports)
        let mut exports_map = HashMap::new();

        // Query the wasmtime module's exports directly
        // This is critical for deserialized modules where metadata.exports is empty
        for export in module.inner().exports() {
            let export_type = match export.ty() {
                wasmtime::ExternType::Func(func_ty) => {
                    let params: Vec<ModuleValueType> = func_ty
                        .params()
                        .map(|t| Self::convert_val_type(t.clone()))
                        .collect::<WasmtimeResult<Vec<_>>>()?;
                    let returns: Vec<ModuleValueType> = func_ty
                        .results()
                        .map(|t| Self::convert_val_type(t.clone()))
                        .collect::<WasmtimeResult<Vec<_>>>()?;
                    ExportKind::Function(FunctionSignature { params, returns })
                }
                wasmtime::ExternType::Global(global_ty) => {
                    let value_type = Self::convert_val_type(global_ty.content().clone())?;
                    let is_mutable = global_ty.mutability() == wasmtime::Mutability::Var;
                    ExportKind::Global(value_type, is_mutable)
                }
                wasmtime::ExternType::Memory(mem_ty) => ExportKind::Memory(
                    mem_ty.minimum(),
                    mem_ty.maximum(),
                    mem_ty.is_64(),
                    mem_ty.is_shared(),
                ),
                wasmtime::ExternType::Table(table_ty) => {
                    let elem_type = Self::convert_ref_type(&table_ty.element())?;
                    ExportKind::Table(
                        elem_type,
                        table_ty.minimum() as u32,
                        table_ty.maximum().map(|m| m as u32),
                    )
                }
                wasmtime::ExternType::Tag(_) => {
                    // Skip tags for now, they're not commonly used
                    continue;
                }
            };

            let binding = ExportBinding {
                name: export.name().to_string(),
                export_type,
                accessible: true,
            };
            exports_map.insert(export.name().to_string(), binding);
        }

        // Build import bindings map from module metadata
        let mut imports_map = HashMap::new();
        for import_info in module.required_imports() {
            let key = format!("{}:{}", import_info.module, import_info.name);
            let binding = ImportBinding {
                module: import_info.module.clone(),
                name: import_info.name.clone(),
                import_type: import_info.import_type.clone(),
                resolved: true, // Assume resolved if instance created successfully
            };
            imports_map.insert(key, binding);
        }

        let export_count = exports_map.len();
        let import_count = imports_map.len();

        let metadata = InstanceMetadata {
            name: module
                .metadata()
                .name
                .clone()
                .unwrap_or_else(|| "unnamed".to_string()),
            created_at: Instant::now(),
            export_count,
            import_count,
            memory_bytes: 0, // Will be calculated when memory is accessed
            function_calls: 0,
            disposed: false,
            state: InstanceState::Created,
            creator_thread_id: std::thread::current().id(),
            cleaned_up: false,
        };

        Ok((metadata, imports_map, exports_map))
    }

    /// Validate import compatibility between required and provided
    fn validate_import_compatibility(
        required: &ImportKind,
        provided: &Extern,
        store: &Store,
    ) -> WasmtimeResult<()> {
        match (required, provided) {
            (ImportKind::Function(req_sig), Extern::Func(func)) => store.with_context_ro(|ctx| {
                let func_type = func.ty(&ctx);
                let provided_sig = Self::convert_func_type(&func_type)?;
                if !Self::signatures_compatible(req_sig, &provided_sig) {
                    return Err(WasmtimeError::ImportExport {
                        message: "Function signature mismatch".to_string(),
                    });
                }
                Ok(())
            }),
            (ImportKind::Global(req_type, req_mut), Extern::Global(global)) => store
                .with_context_ro(|ctx| {
                    let global_type = global.ty(&ctx);
                    let provided_type = Self::convert_val_type(global_type.content().clone())?;
                    let provided_mut =
                        matches!(global_type.mutability(), wasmtime::Mutability::Var);

                    if req_type != &provided_type || (req_mut > &provided_mut) {
                        return Err(WasmtimeError::ImportExport {
                            message: "Global type mismatch".to_string(),
                        });
                    }
                    Ok(())
                }),
            (
                ImportKind::Memory(_req_min, _req_max, _req_64, _req_shared),
                Extern::Memory(_memory),
            ) => {
                // Memory compatibility validation could be more sophisticated
                Ok(())
            }
            (ImportKind::Table(_req_elem, _req_min, _req_max), Extern::Table(_table)) => {
                // Table compatibility validation could be more sophisticated
                Ok(())
            }
            (_, Extern::SharedMemory(_)) => Err(WasmtimeError::ImportExport {
                message: "SharedMemory imports not supported".to_string(),
            }),
            (_, Extern::Tag(_)) => Err(WasmtimeError::ImportExport {
                message: "Tag imports not supported".to_string(),
            }),
            _ => Err(WasmtimeError::ImportExport {
                message: "Import type mismatch".to_string(),
            }),
        }
    }

    /// EXPERIMENTAL: Call function by re-instantiating (tests context lifetime theory)
    pub fn call_export_function_with_reinstantiation(
        &mut self,
        store: &mut Store,
        module: &Module,
        name: &str,
        params: &[WasmValue],
    ) -> WasmtimeResult<ExecutionResult> {
        // Create a fresh instance in this store context
        let mut wasm_params_converted = Vec::new();
        for param in params {
            wasm_params_converted.push(Self::wasm_value_to_val(param)?);
        }

        let result = store.with_context(|mut ctx| {
            // Fresh instantiation
            let fresh_instance =
                WasmtimeInstance::new(&mut ctx, module.inner(), &[]).map_err(|e| {
                    WasmtimeError::Instance {
                        message: format!("Failed to create fresh instance: {}", e),
                    }
                })?;

            let export = fresh_instance.get_export(&mut ctx, name).ok_or_else(|| {
                WasmtimeError::ImportExport {
                    message: format!("Export '{}' not found", name),
                }
            })?;

            match export {
                Extern::Func(func) => {
                    let func_type = func.ty(&ctx);
                    let mut results = Vec::with_capacity(func_type.results().len());
                    for return_type in func_type.results() {
                        let default_val = match return_type {
                            WasmtimeValType::I32 => Val::I32(0),
                            WasmtimeValType::I64 => Val::I64(0),
                            WasmtimeValType::F32 => Val::F32(0.0_f32.to_bits()),
                            WasmtimeValType::F64 => Val::F64(0.0_f64.to_bits()),
                            WasmtimeValType::V128 => Val::V128(wasmtime::V128::from(0u128)),
                            WasmtimeValType::Ref(_) => Val::ExternRef(None),
                        };
                        results.push(default_val);
                    }

                    func.call(&mut ctx, &wasm_params_converted, &mut results)
                        .map_err(|e| WasmtimeError::Runtime {
                            message: extract_error_chain(&e),
                            backtrace: None,
                        })?;
                    Ok(results)
                }
                _ => Err(WasmtimeError::Function {
                    message: format!("Export '{}' is not a function", name),
                }),
            }
        })?;

        // Convert results back
        let wasm_values: Vec<WasmValue> = result
            .iter()
            .map(|v| Self::val_to_wasm_value(v.clone()))
            .collect::<WasmtimeResult<Vec<_>>>()?;

        Ok(ExecutionResult {
            values: wasm_values,
            fuel_consumed: None,
            execution_time_ns: 0,
        })
    }

    /// Call exported function with comprehensive type checking and parameter conversion
    pub fn call_export_function(
        &mut self,
        store: &mut Store,
        name: &str,
        params: &[WasmValue],
    ) -> WasmtimeResult<ExecutionResult> {
        // Check if instance is disposed or cleaned up
        if self.metadata.disposed || self.metadata.cleaned_up {
            return Err(WasmtimeError::Instance {
                message: "Cannot call function on disposed or cleaned up instance".to_string(),
            });
        }

        // Validate cross-thread access
        self.validate_thread_access()?;

        // Set state to running during execution
        let previous_state = self.metadata.state;
        self.metadata.state = InstanceState::Running;

        // Validate export exists and is a function
        let export_binding =
            self.exports_map
                .get(name)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!(
                        "Export '{}' not found. Available exports: {:?}",
                        name,
                        self.exports_map.keys().collect::<Vec<_>>()
                    ),
                })?;

        let function_sig = match &export_binding.export_type {
            ExportKind::Function(sig) => sig,
            _ => {
                return Err(WasmtimeError::Function {
                    message: format!("Export '{}' is not a function", name),
                })
            }
        };

        // Validate parameter count
        if params.len() != function_sig.params.len() {
            return Err(WasmtimeError::Function {
                message: format!(
                    "Parameter count mismatch: expected {}, got {}",
                    function_sig.params.len(),
                    params.len()
                ),
            });
        }

        // Save parameters for conversion after we have store context
        // (needed for externref/funcref which require Store to create)
        let params_to_convert = params.to_vec();

        // Get function and execute with timing
        let start_time = Instant::now();
        let instance = self.inner.lock();

        // Get fuel before execution for tracking
        let fuel_before = store.fuel_remaining().ok().flatten();

        // CRITICAL: Use direct Store access with ReentrantMutex
        // This allows same-thread reentrant access needed by Wasmtime during function execution
        let mut store_guard = store.try_lock_store()?;

        let export = instance
            .get_export(&mut *store_guard, name)
            .ok_or_else(|| WasmtimeError::ImportExport {
                message: format!("Export '{}' not found in wasmtime instance", name),
            })?;

        let result = match export {
            Extern::Func(func) => {
                // Get function type to determine result types
                let func_type = func.ty(&*store_guard);

                // Validate parameter types
                for (i, (provided, expected)) in params_to_convert
                    .iter()
                    .zip(&function_sig.params)
                    .enumerate()
                {
                    if !Self::value_type_matches(provided, expected) {
                        return Err(WasmtimeError::Function {
                            message: format!(
                                "Parameter {} type mismatch: expected {:?}, got {:?}",
                                i, expected, provided
                            ),
                        });
                    }
                }

                // Convert parameters WITH store context for externref/funcref
                let mut wasm_params = Vec::with_capacity(params_to_convert.len());
                for param in &params_to_convert {
                    match param {
                        WasmValue::I32(v) => wasm_params.push(Val::I32(*v)),
                        WasmValue::I64(v) => wasm_params.push(Val::I64(*v)),
                        WasmValue::F32(v) => wasm_params.push(Val::F32(v.to_bits())),
                        WasmValue::F64(v) => wasm_params.push(Val::F64(v.to_bits())),
                        WasmValue::V128(bytes) => wasm_params
                            .push(Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes)))),
                        WasmValue::ExternRef(ref_id) => {
                            // For externref, we need to create a proper ExternRef with store context
                            use wasmtime::ExternRef;
                            let externref_val = match ref_id {
                                Some(id) => {
                                    // Create ExternRef wrapping the i64 ID
                                    let externref = ExternRef::new(&mut *store_guard, *id)
                                        .map_err(|e| WasmtimeError::Runtime {
                                            message: format!("Failed to create ExternRef: {}", e),
                                            backtrace: None,
                                        })?;
                                    Val::ExternRef(Some(externref))
                                }
                                None => Val::ExternRef(None),
                            };
                            wasm_params.push(externref_val);
                        }
                        WasmValue::FuncRef(_) => {
                            // FuncRef requires actual Func handle - not implementable with just an ID
                            // For now, pass null funcref
                            wasm_params.push(Val::FuncRef(None));
                        }
                    }
                }

                // Pre-allocate results with correct types
                let mut results = Vec::with_capacity(func_type.results().len());
                for return_type in func_type.results() {
                    let default_val = match return_type {
                        WasmtimeValType::I32 => Val::I32(0),
                        WasmtimeValType::I64 => Val::I64(0),
                        WasmtimeValType::F32 => Val::F32(0.0_f32.to_bits()),
                        WasmtimeValType::F64 => Val::F64(0.0_f64.to_bits()),
                        WasmtimeValType::V128 => Val::V128(wasmtime::V128::from(0u128)),
                        WasmtimeValType::Ref(_) => Val::ExternRef(None),
                    };
                    results.push(default_val);
                }

                // Try using typed function for no-param i32 return case
                if wasm_params.is_empty() && results.len() == 1 {
                    if let Val::I32(_) = results[0] {
                        match func.typed::<(), i32>(&*store_guard) {
                            Ok(typed_func) => match typed_func.call(&mut *store_guard, ()) {
                                Ok(result) => Ok(vec![Val::I32(result)]),
                                Err(e) => Err(WasmtimeError::Runtime {
                                    message: extract_error_chain(&e),
                                    backtrace: None,
                                }),
                            },
                            Err(_e) => {
                                // Fall through to untyped call
                                match func.call(&mut *store_guard, &wasm_params, &mut results) {
                                    Ok(_) => Ok(results),
                                    Err(e) => Err(WasmtimeError::Runtime {
                                        message: extract_error_chain(&e),
                                        backtrace: None,
                                    }),
                                }
                            }
                        }
                    } else {
                        match func.call(&mut *store_guard, &wasm_params, &mut results) {
                            Ok(_) => Ok(results),
                            Err(e) => Err(WasmtimeError::Runtime {
                                message: extract_error_chain(&e),
                                backtrace: None,
                            }),
                        }
                    }
                } else {
                    match func.call(&mut *store_guard, &wasm_params, &mut results) {
                        Ok(_) => Ok(results),
                        Err(e) => {
                            // Extract the full error chain to capture host function error messages
                            // The root cause of a trap is often the host function error which contains
                            // the actual error message we want to propagate (e.g., "test-panic")
                            let error_message = extract_error_chain(&e);

                            Err(WasmtimeError::Runtime {
                                message: error_message,
                                backtrace: None,
                            })
                        }
                    }
                }
            }
            _ => Err(WasmtimeError::Function {
                message: format!("Export '{}' is not a function", name),
            }),
        }
        .map_err(|e| {
            // Set error state on execution failure
            self.metadata.state = InstanceState::Error;
            e
        })?;

        // Convert results back to WasmValue WITH store context for externref extraction
        // This must happen BEFORE we drop store_guard
        let values: Vec<WasmValue> = result
            .iter()
            .map(|val| {
                match val {
                    Val::ExternRef(Some(ext_ref)) => {
                        // Extract the wrapped i64 value from ExternRef using store context
                        // Note: ext_ref.data() returns Result<Option<&dyn Any>, Error>
                        match ext_ref.data(&*store_guard) {
                            Ok(Some(data)) => {
                                // Try to downcast to i64
                                if let Some(&id) = data.downcast_ref::<i64>() {
                                    Ok(WasmValue::ExternRef(Some(id)))
                                } else {
                                    // Not an i64, return None
                                    Ok(WasmValue::ExternRef(None))
                                }
                            }
                            Ok(None) => Ok(WasmValue::ExternRef(None)),
                            Err(e) => Err(WasmtimeError::Runtime {
                                message: format!("Failed to extract externref data: {}", e),
                                backtrace: None,
                            }),
                        }
                    }
                    Val::ExternRef(None) => Ok(WasmValue::ExternRef(None)),
                    // For non-externref values, use the standard conversion
                    _ => Self::val_to_wasm_value(val.clone()),
                }
            })
            .collect::<WasmtimeResult<Vec<_>>>()?;

        // Drop store_guard now that we're done with it
        drop(store_guard);

        let execution_time_ns = start_time.elapsed().as_nanos() as u64;

        // Get fuel after execution for tracking
        let fuel_after = store.fuel_remaining().ok().flatten();

        let fuel_consumed = match (fuel_before, fuel_after) {
            (Some(before), Some(after)) => Some(after.saturating_sub(before)),
            _ => None,
        };

        // Increment function call count
        self.metadata.function_calls += 1;

        // Restore previous state
        self.metadata.state = previous_state;

        Ok(ExecutionResult {
            values,
            fuel_consumed,
            execution_time_ns,
        })
    }

    /// Get exported function by name (for direct Wasmtime usage)
    pub fn get_func(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Func>> {
        let instance = self.inner.lock();

        let mut store_guard = store.try_lock_store()?;
        let export = instance.get_export(&mut *store_guard, name);
        match export {
            Some(Extern::Func(func)) => Ok(Some(func)),
            _ => Ok(None),
        }
    }

    /// Get exported global by name
    pub fn get_global(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Global>> {
        let instance = self.inner.lock();

        let mut store_guard = store.try_lock_store()?;
        let export = instance.get_export(&mut *store_guard, name);
        match export {
            Some(Extern::Global(global)) => Ok(Some(global)),
            _ => Ok(None),
        }
    }

    /// Get exported memory by name
    pub fn get_memory(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Memory>> {
        let instance = self.inner.lock();

        let mut store_guard = store.try_lock_store()?;
        let export = instance.get_export(&mut *store_guard, name);
        match export {
            Some(Extern::Memory(memory)) => Ok(Some(memory)),
            _ => Ok(None),
        }
    }

    /// Get exported shared memory by name
    ///
    /// This method is used to retrieve shared memory exports from modules that use
    /// the WebAssembly threads proposal. Shared memory is different from regular
    /// memory in that it can be accessed from multiple threads simultaneously.
    pub fn get_shared_memory(
        &self,
        store: &mut Store,
        name: &str,
    ) -> WasmtimeResult<Option<SharedMemory>> {
        let instance = self.inner.lock();

        let mut store_guard = store.try_lock_store()?;
        let export = instance.get_export(&mut *store_guard, name);
        match export {
            Some(Extern::SharedMemory(shared_memory)) => Ok(Some(shared_memory)),
            _ => Ok(None),
        }
    }

    /// Get exported table by name
    pub fn get_table(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Table>> {
        let instance = self.inner.lock();

        let mut store_guard = store.try_lock_store()?;
        let export = instance.get_export(&mut *store_guard, name);
        match export {
            Some(Extern::Table(table)) => Ok(Some(table)),
            _ => Ok(None),
        }
    }

    /// Get element segment manager for table.init() operations
    pub fn get_element_segment_manager(&self) -> &Arc<ElementSegmentManager> {
        &self.element_segment_manager
    }

    /// Get data segment manager for memory.init() operations
    pub fn get_data_segment_manager(&self) -> &Arc<DataSegmentManager> {
        &self.data_segment_manager
    }

    /// List all exports
    pub fn exports(&self, store: &mut Store) -> WasmtimeResult<Vec<String>> {
        let instance = self.inner.lock();

        let mut store_guard = store.try_lock_store()?;
        let exports = instance
            .exports(&mut *store_guard)
            .map(|export| export.name().to_string())
            .collect();
        Ok(exports)
    }

    /// Dispose instance and clean up resources
    pub fn dispose(&mut self) -> WasmtimeResult<()> {
        // Mark as disposed to prevent further operations
        self.metadata.disposed = true;
        self.metadata.state = InstanceState::Disposed;

        // Clear export and import maps
        self.exports_map.clear();
        self.imports_map.clear();

        // The Arc<Mutex<WasmtimeInstance>> will be cleaned up when the last reference is dropped
        Ok(())
    }

    /// Get the current lifecycle state of this instance
    pub fn get_state(&self) -> InstanceState {
        self.metadata.state
    }

    /// Set the lifecycle state of this instance
    pub fn set_state(&mut self, state: InstanceState) {
        self.metadata.state = state;
    }

    /// Perform comprehensive resource cleanup
    pub fn cleanup(&mut self) -> WasmtimeResult<bool> {
        if self.metadata.cleaned_up {
            return Ok(false); // Already cleaned up
        }

        // Set state to destroying during cleanup
        self.metadata.state = InstanceState::Destroying;

        // Clear all maps and references
        self.exports_map.clear();
        self.imports_map.clear();

        // Mark as disposed and cleaned up
        self.metadata.disposed = true;
        self.metadata.cleaned_up = true;
        self.metadata.state = InstanceState::Disposed;

        Ok(true)
    }

    /// Check if instance has been cleaned up
    pub fn is_cleaned_up(&self) -> bool {
        self.metadata.cleaned_up
    }

    /// Validate cross-thread access
    pub fn validate_thread_access(&self) -> WasmtimeResult<()> {
        let current_thread = std::thread::current().id();
        if current_thread != self.metadata.creator_thread_id {
            log::warn!(
                "Cross-thread access detected: instance created on thread {:?}, accessed from thread {:?}",
                self.metadata.creator_thread_id,
                current_thread
            );
            // For now, just log a warning. In a stricter implementation, this could return an error
        }
        Ok(())
    }

    /// Check if instance has been disposed
    pub fn is_disposed(&self) -> bool {
        self.metadata.disposed
    }

    /// Get instance metadata
    pub fn metadata(&self) -> &InstanceMetadata {
        &self.metadata
    }

    /// Get export information by name
    pub fn get_export_info(&self, name: &str) -> Option<&ExportBinding> {
        self.exports_map.get(name)
    }

    /// Get all exports
    pub fn all_exports(&self) -> &HashMap<String, ExportBinding> {
        &self.exports_map
    }

    /// Get import information by key (module:name format)
    pub fn get_import_info(&self, module: &str, name: &str) -> Option<&ImportBinding> {
        let key = format!("{}:{}", module, name);
        self.imports_map.get(&key)
    }

    /// Get all imports
    pub fn all_imports(&self) -> &HashMap<String, ImportBinding> {
        &self.imports_map
    }

    /// Get access to the inner wasmtime instance (for internal use)
    pub(crate) fn inner(&self) -> &Arc<ReentrantLock<WasmtimeInstance>> {
        &self.inner
    }

    /// Validate instance is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Instance {
                message: "Instance has been disposed".to_string(),
            });
        }

        if let Some(_guard) = self.inner.try_lock() {
            Ok(())
        } else {
            Err(WasmtimeError::Concurrency {
                message: "Instance is locked and may be corrupted".to_string(),
            })
        }
    }

    /// Convert FuncType to FunctionSignature
    fn convert_func_type(func_type: &FuncType) -> WasmtimeResult<FunctionSignature> {
        let params = func_type
            .params()
            .map(Self::convert_val_type)
            .collect::<WasmtimeResult<Vec<_>>>()?;

        let returns = func_type
            .results()
            .map(Self::convert_val_type)
            .collect::<WasmtimeResult<Vec<_>>>()?;

        Ok(FunctionSignature { params, returns })
    }

    /// Convert wasmtime ValType to our ModuleValueType
    fn convert_val_type(val_type: WasmtimeValType) -> WasmtimeResult<ModuleValueType> {
        match val_type {
            WasmtimeValType::I32 => Ok(ModuleValueType::I32),
            WasmtimeValType::I64 => Ok(ModuleValueType::I64),
            WasmtimeValType::F32 => Ok(ModuleValueType::F32),
            WasmtimeValType::F64 => Ok(ModuleValueType::F64),
            WasmtimeValType::V128 => Ok(ModuleValueType::V128),
            WasmtimeValType::Ref(ref_type) => Self::convert_ref_type(&ref_type),
        }
    }

    /// Convert RefType to ModuleValueType
    fn convert_ref_type(ref_type: &wasmtime::RefType) -> WasmtimeResult<ModuleValueType> {
        match ref_type.heap_type() {
            wasmtime::HeapType::Extern => Ok(ModuleValueType::ExternRef),
            wasmtime::HeapType::Func | wasmtime::HeapType::ConcreteFunc(_) => {
                Ok(ModuleValueType::FuncRef)
            }
            // GC proposal heap types
            wasmtime::HeapType::Any | wasmtime::HeapType::Exn => Ok(ModuleValueType::AnyRef),
            wasmtime::HeapType::Eq => Ok(ModuleValueType::EqRef),
            wasmtime::HeapType::I31 => Ok(ModuleValueType::I31Ref),
            wasmtime::HeapType::Struct | wasmtime::HeapType::ConcreteStruct(_) => {
                Ok(ModuleValueType::StructRef)
            }
            wasmtime::HeapType::Array | wasmtime::HeapType::ConcreteArray(_) => {
                Ok(ModuleValueType::ArrayRef)
            }
            wasmtime::HeapType::None => Ok(ModuleValueType::NullRef),
            wasmtime::HeapType::NoFunc => Ok(ModuleValueType::NullFuncRef),
            wasmtime::HeapType::NoExtern => Ok(ModuleValueType::NullExternRef),
            _ => Err(WasmtimeError::Type {
                message: format!("Unsupported reference type: {:?}", ref_type),
            }),
        }
    }

    /// Check if WasmValue matches expected ModuleValueType
    fn value_type_matches(value: &WasmValue, expected: &ModuleValueType) -> bool {
        match (value, expected) {
            (WasmValue::I32(_), ModuleValueType::I32) => true,
            (WasmValue::I64(_), ModuleValueType::I64) => true,
            (WasmValue::F32(_), ModuleValueType::F32) => true,
            (WasmValue::F64(_), ModuleValueType::F64) => true,
            (WasmValue::V128(_), ModuleValueType::V128) => true,
            (WasmValue::ExternRef(_), ModuleValueType::ExternRef) => true,
            (WasmValue::FuncRef(_), ModuleValueType::FuncRef) => true,
            _ => false,
        }
    }

    /// Convert WasmValue to wasmtime Val
    fn wasm_value_to_val(value: &WasmValue) -> WasmtimeResult<Val> {
        match value {
            WasmValue::I32(v) => Ok(Val::I32(*v)),
            WasmValue::I64(v) => Ok(Val::I64(*v)),
            WasmValue::F32(v) => Ok(Val::F32(v.to_bits())),
            WasmValue::F64(v) => Ok(Val::F64(v.to_bits())),
            WasmValue::V128(bytes) => {
                Ok(Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes))))
            }
            WasmValue::ExternRef(_ref_id) => {
                // ExternRef support is limited - creating externrefs requires Store context
                // For now, always pass NULL to prevent parameter count mismatch
                Ok(Val::ExternRef(None))
            }
            WasmValue::FuncRef(ref_id) => {
                // Convert funcref ID to Func handle using the function registry
                if let Some(id) = ref_id {
                    use crate::table::core::get_function_reference;
                    // Convert i64 to u64 for registry lookup
                    let u_id = *id as u64;
                    if let Some(func) = get_function_reference(u_id)? {
                        Ok(Val::FuncRef(Some(func)))
                    } else {
                        Ok(Val::FuncRef(None))
                    }
                } else {
                    Ok(Val::FuncRef(None))
                }
            }
        }
    }

    /// Convert wasmtime Val to WasmValue
    fn val_to_wasm_value(val: Val) -> WasmtimeResult<WasmValue> {
        match val {
            Val::I32(v) => Ok(WasmValue::I32(v)),
            Val::I64(v) => Ok(WasmValue::I64(v)),
            Val::F32(bits) => Ok(WasmValue::F32(f32::from_bits(bits))),
            Val::F64(bits) => Ok(WasmValue::F64(f64::from_bits(bits))),
            Val::V128(v) => Ok(WasmValue::V128(u128::from(v).to_le_bytes())),
            Val::ExternRef(_ext_ref) => {
                // ExternRef data extraction requires Store context
                // For now, just preserve None
                Ok(WasmValue::ExternRef(None))
            }
            Val::FuncRef(func_ref) => {
                // Extract funcref and register it to get an ID
                if let Some(func) = func_ref {
                    use crate::table::core::register_function_reference;
                    let id = register_function_reference(func)?;
                    // Convert u64 to i64 for WasmValue storage
                    Ok(WasmValue::FuncRef(Some(id as i64)))
                } else {
                    Ok(WasmValue::FuncRef(None))
                }
            }
            Val::AnyRef(_) => Ok(WasmValue::ExternRef(None)),
            Val::ExnRef(_) => Ok(WasmValue::ExternRef(None)),
            Val::ContRef(_) => Ok(WasmValue::ExternRef(None)),
        }
    }

    /// Check if two function signatures are compatible
    fn signatures_compatible(sig1: &FunctionSignature, sig2: &FunctionSignature) -> bool {
        sig1.params == sig2.params && sig1.returns == sig2.returns
    }
}

// Thread safety: Instance uses Arc<Mutex<WasmtimeInstance>> internally
unsafe impl Send for Instance {}
unsafe impl Sync for Instance {}

/// Shared core functions for instance operations used by both JNI and Panama interfaces
///
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use crate::module::Module;
    use crate::store::Store;
    use crate::validate_ptr_not_null;
    use std::os::raw::c_void;
    use wasmtime::Extern;

    /// Core function to create a new WebAssembly instance with comprehensive import resolution
    pub fn create_instance_with_imports(
        store: &mut Store,
        module: &Module,
        imports: &[Extern],
    ) -> WasmtimeResult<Box<Instance>> {
        Instance::new(store, module, imports).map(Box::new)
    }

    /// Core function to create instance with validated import resolution from hash map
    pub fn create_instance_with_import_map(
        store: &mut Store,
        module: &Module,
        imports: &HashMap<String, HashMap<String, Extern>>,
    ) -> WasmtimeResult<Box<Instance>> {
        Instance::new_with_imports(store, module, imports).map(Box::new)
    }

    /// Core function to create a new WebAssembly instance without imports
    pub fn create_instance(store: &mut Store, module: &Module) -> WasmtimeResult<Box<Instance>> {
        Instance::new_without_imports(store, module).map(Box::new)
    }

    /// Core function to instantiate a WebAssembly module with no imports (alias for create_instance)
    pub fn instantiate_module(
        mut store: &mut Store,
        module: &Module,
        imports: &[wasmtime::Extern],
    ) -> WasmtimeResult<Box<Instance>> {
        if imports.is_empty() {
            Instance::new_without_imports(&mut store, module).map(Box::new)
        } else {
            Instance::new(&mut store, module, imports).map(Box::new)
        }
    }

    /// Core function to call exported function with type checking and parameter conversion
    pub fn call_exported_function(
        instance: &mut Instance,
        store: &mut Store,
        name: &str,
        params: &[WasmValue],
    ) -> WasmtimeResult<ExecutionResult> {
        instance.call_export_function(store, name, params)
    }

    /// Core function to validate instance pointer and get reference
    pub unsafe fn get_instance_ref(
        instance_ptr: *const c_void,
    ) -> WasmtimeResult<&'static Instance> {
        validate_ptr_not_null!(instance_ptr, "instance");
        Ok(&*(instance_ptr as *const Instance))
    }

    /// Core function to validate instance pointer and get mutable reference
    pub unsafe fn get_instance_mut(
        instance_ptr: *mut c_void,
    ) -> WasmtimeResult<&'static mut Instance> {
        validate_ptr_not_null!(instance_ptr, "instance");
        Ok(&mut *(instance_ptr as *mut Instance))
    }

    /// Core function to get exported function by name
    pub fn get_exported_function(
        instance: &Instance,
        store: &mut Store,
        name: &str,
    ) -> WasmtimeResult<Option<wasmtime::Func>> {
        instance.get_func(store, name)
    }

    /// Core function to get exported global by name
    pub fn get_exported_global(
        instance: &Instance,
        store: &mut Store,
        name: &str,
    ) -> WasmtimeResult<Option<wasmtime::Global>> {
        instance.get_global(store, name)
    }

    /// Core function to get exported memory by name
    pub fn get_exported_memory(
        instance: &Instance,
        store: &mut Store,
        name: &str,
    ) -> WasmtimeResult<Option<wasmtime::Memory>> {
        instance.get_memory(store, name)
    }

    /// Core function to get exported table by name
    pub fn get_exported_table(
        instance: &Instance,
        store: &mut Store,
        name: &str,
    ) -> WasmtimeResult<Option<wasmtime::Table>> {
        instance.get_table(store, name)
    }

    /// Core function to list all exports
    pub fn list_exports(instance: &Instance, store: &mut Store) -> WasmtimeResult<Vec<String>> {
        instance.exports(store)
    }

    /// Core function to get instance metadata
    pub fn get_instance_metadata(instance: &Instance) -> &InstanceMetadata {
        instance.metadata()
    }

    /// Core function to validate instance functionality
    pub fn validate_instance(instance: &Instance) -> WasmtimeResult<()> {
        instance.validate()
    }

    /// Core function to dispose instance with proper resource cleanup  
    pub fn dispose_instance(instance: &mut Instance) -> WasmtimeResult<()> {
        instance.dispose()
    }

    /// Core function to destroy an instance (safe cleanup)
    ///
    /// Uses the consolidated `safe_destroy` utility from `ffi_common::resource_destruction`
    /// which provides double-free protection, fake pointer detection, and panic safety.
    pub unsafe fn destroy_instance(instance_ptr: *mut c_void) {
        use crate::ffi_common::resource_destruction::safe_destroy;
        let _ = safe_destroy::<Instance>(instance_ptr, "Instance");
    }

    /// Core function to check if instance has been disposed
    pub fn is_instance_disposed(instance: &Instance) -> bool {
        instance.is_disposed()
    }

    /// Core function to check if instance has a specific export
    pub fn has_export(instance: &Instance, name: &str) -> bool {
        instance.get_export_info(name).is_some()
    }

    /// Core function to get export information
    pub fn get_export_information<'a>(
        instance: &'a Instance,
        name: &str,
    ) -> Option<&'a ExportBinding> {
        instance.get_export_info(name)
    }

    /// Core function to get all exports information
    pub fn get_all_exports(instance: &Instance) -> &HashMap<String, ExportBinding> {
        instance.all_exports()
    }

    /// Core function to get import information  
    pub fn get_import_information<'a>(
        instance: &'a Instance,
        module: &str,
        name: &str,
    ) -> Option<&'a ImportBinding> {
        instance.get_import_info(module, name)
    }

    /// Core function to get all imports information
    pub fn get_all_imports(instance: &Instance) -> &HashMap<String, ImportBinding> {
        instance.all_imports()
    }

    /// Core function to get export count
    pub fn get_export_count(instance: &Instance) -> usize {
        instance.metadata().export_count
    }

    /// Core function to get import count
    pub fn get_import_count(instance: &Instance) -> usize {
        instance.metadata().import_count
    }

    /// Core function to get memory usage in bytes
    pub fn get_memory_bytes(instance: &Instance) -> usize {
        instance.metadata().memory_bytes
    }

    /// Core function to get instance name
    pub fn get_instance_name(instance: &Instance) -> &str {
        &instance.metadata().name
    }

    /// Core function to get function call count
    pub fn get_function_call_count(instance: &Instance) -> u64 {
        instance.metadata().function_calls
    }

    /// Core function to get instance creation timestamp
    pub fn get_creation_time(instance: &Instance) -> Instant {
        instance.metadata().created_at
    }

    /// Core function to get instance state
    pub fn get_instance_state(instance: &Instance) -> InstanceState {
        instance.get_state()
    }

    /// Core function to set instance state
    pub fn set_instance_state(instance: &mut Instance, state: InstanceState) {
        instance.set_state(state);
    }

    /// Core function to cleanup instance resources
    pub fn cleanup_instance_resources(instance: &mut Instance) -> WasmtimeResult<bool> {
        instance.cleanup()
    }

    /// Core function to check if instance has been cleaned up
    pub fn is_instance_cleaned_up(instance: &Instance) -> bool {
        instance.is_cleaned_up()
    }

    /// Core function to validate thread access
    pub fn validate_instance_thread_access(instance: &Instance) -> WasmtimeResult<()> {
        instance.validate_thread_access()
    }

    /// Core function to get instance creator thread ID
    pub fn get_instance_creator_thread_id(instance: &Instance) -> std::thread::ThreadId {
        instance.metadata().creator_thread_id
    }

    /// Helper function to create WasmValue from primitive types for JNI/Panama bindings
    pub fn create_i32_value(value: i32) -> WasmValue {
        WasmValue::I32(value)
    }

    /// Creates an i64 WebAssembly value
    pub fn create_i64_value(value: i64) -> WasmValue {
        WasmValue::I64(value)
    }

    /// Creates an f32 WebAssembly value
    pub fn create_f32_value(value: f32) -> WasmValue {
        WasmValue::F32(value)
    }

    /// Creates an f64 WebAssembly value
    pub fn create_f64_value(value: f64) -> WasmValue {
        WasmValue::F64(value)
    }

    /// Creates a v128 WebAssembly value from bytes
    pub fn create_v128_value(bytes: [u8; 16]) -> WasmValue {
        WasmValue::V128(bytes)
    }

    /// Creates an externref WebAssembly value with optional ID
    pub fn create_externref_value(ref_id: Option<i64>) -> WasmValue {
        WasmValue::ExternRef(ref_id)
    }

    /// Creates a funcref WebAssembly value with optional ID
    pub fn create_funcref_value(ref_id: Option<i64>) -> WasmValue {
        WasmValue::FuncRef(ref_id)
    }

    /// Helper functions to extract values from WasmValue for JNI/Panama bindings
    pub fn extract_i32_value(value: &WasmValue) -> WasmtimeResult<i32> {
        match value {
            WasmValue::I32(v) => Ok(*v),
            _ => Err(WasmtimeError::Type {
                message: "Expected I32 value".to_string(),
            }),
        }
    }

    /// Extracts i64 value from WebAssembly value
    pub fn extract_i64_value(value: &WasmValue) -> WasmtimeResult<i64> {
        match value {
            WasmValue::I64(v) => Ok(*v),
            _ => Err(WasmtimeError::Type {
                message: "Expected I64 value".to_string(),
            }),
        }
    }

    /// Extracts f32 value from WebAssembly value
    pub fn extract_f32_value(value: &WasmValue) -> WasmtimeResult<f32> {
        match value {
            WasmValue::F32(v) => Ok(*v),
            _ => Err(WasmtimeError::Type {
                message: "Expected F32 value".to_string(),
            }),
        }
    }

    /// Extracts f64 value from WebAssembly value
    pub fn extract_f64_value(value: &WasmValue) -> WasmtimeResult<f64> {
        match value {
            WasmValue::F64(v) => Ok(*v),
            _ => Err(WasmtimeError::Type {
                message: "Expected F64 value".to_string(),
            }),
        }
    }

    /// Extracts v128 value bytes from WebAssembly value
    pub fn extract_v128_value(value: &WasmValue) -> WasmtimeResult<[u8; 16]> {
        match value {
            WasmValue::V128(bytes) => Ok(*bytes),
            _ => Err(WasmtimeError::Type {
                message: "Expected V128 value".to_string(),
            }),
        }
    }

    // Function-specific operations for Panama FFI bindings

    /// Get function export by name - returns wasmtime::Func
    pub fn get_function_export(
        instance: &Instance,
        store: &mut Store,
        name: &str,
    ) -> WasmtimeResult<Option<wasmtime::Func>> {
        instance.get_func(store, name)
    }

    /// Validate function pointer and get reference
    pub unsafe fn get_function_ref(
        func_ptr: *const c_void,
    ) -> WasmtimeResult<&'static wasmtime::Func> {
        validate_ptr_not_null!(func_ptr, "function");
        Ok(&*(func_ptr as *const wasmtime::Func))
    }

    /// Get function parameter types as integer array
    pub fn get_function_param_types(
        func: &wasmtime::Func,
        store: &Store,
    ) -> WasmtimeResult<Vec<i32>> {
        store.with_context(|ctx| {
            let func_type = func.ty(ctx);
            let param_types: Vec<i32> = func_type
                .params()
                .map(|param_type| wasmtime_val_type_to_int(param_type))
                .collect();
            Ok(param_types)
        })
    }

    /// Get function result types as integer array
    pub fn get_function_result_types(
        func: &wasmtime::Func,
        store: &Store,
    ) -> WasmtimeResult<Vec<i32>> {
        store.with_context(|ctx| {
            let func_type = func.ty(ctx);
            let result_types: Vec<i32> = func_type
                .results()
                .map(|result_type| wasmtime_val_type_to_int(result_type))
                .collect();
            Ok(result_types)
        })
    }

    /// Get function type
    pub fn get_function_type(
        func: &wasmtime::Func,
        store: &Store,
    ) -> WasmtimeResult<wasmtime::FuncType> {
        store.with_context(|ctx| Ok(func.ty(ctx)))
    }

    /// Call function with parameters and return results
    pub fn call_function(
        func: &wasmtime::Func,
        store: &mut Store,
        params: &[WasmValue],
    ) -> WasmtimeResult<Vec<WasmValue>> {
        // Convert WasmValue parameters to wasmtime::Val
        let wasmtime_params: Vec<wasmtime::Val> = params
            .iter()
            .map(|param| wasm_value_to_wasmtime_val(param))
            .collect::<Result<Vec<_>, _>>()?;

        // Prepare result vector
        let results = store.with_context(|mut ctx| {
            let func_type = func.ty(&ctx);
            let mut results = vec![wasmtime::Val::I32(0); func_type.results().len()];

            func.call(&mut ctx, &wasmtime_params, &mut results)
                .map_err(|e| WasmtimeError::Execution {
                    message: format!("Function call failed: {}", e),
                })?;

            Ok(results)
        })?;

        // Convert results back to WasmValue
        results
            .into_iter()
            .map(|result| wasmtime_val_to_wasm_value(&result))
            .collect::<Result<Vec<_>, _>>()
    }

    /// Convert parameters from FFI representation (FfiWasmValue layout)
    pub unsafe fn convert_params_from_ffi(
        params_ptr: *const c_void,
        param_count: usize,
    ) -> WasmtimeResult<Vec<WasmValue>> {
        if params_ptr.is_null() || param_count == 0 {
            return Ok(Vec::new());
        }

        // Read parameters as FfiWasmValue array (20 bytes each)
        let ffi_params = std::slice::from_raw_parts(params_ptr as *const FfiWasmValue, param_count);

        // Convert FfiWasmValue to WasmValue
        Ok(ffi_params.iter().map(|ffi| ffi.to_wasm_value()).collect())
    }

    /// Convert results to FFI representation (FfiWasmValue layout)
    pub unsafe fn convert_results_to_ffi(
        results: &[WasmValue],
        results_ptr: *mut c_void,
        result_count: usize,
    ) -> WasmtimeResult<()> {
        if results_ptr.is_null() || results.is_empty() {
            return Ok(());
        }

        let count = std::cmp::min(results.len(), result_count);

        // Write results as FfiWasmValue array (20 bytes each)
        let ffi_results = std::slice::from_raw_parts_mut(results_ptr as *mut FfiWasmValue, count);

        for (i, result) in results.iter().take(count).enumerate() {
            ffi_results[i] = FfiWasmValue::from_wasm_value(result);
        }

        Ok(())
    }

    /// Convert WasmValue to wasmtime::Val
    fn wasm_value_to_wasmtime_val(value: &WasmValue) -> WasmtimeResult<wasmtime::Val> {
        Ok(match value {
            WasmValue::I32(v) => wasmtime::Val::I32(*v),
            WasmValue::I64(v) => wasmtime::Val::I64(*v),
            WasmValue::F32(v) => wasmtime::Val::F32((*v).to_bits()),
            WasmValue::F64(v) => wasmtime::Val::F64((*v).to_bits()),
            WasmValue::V128(bytes) => {
                wasmtime::Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes)))
            }
            WasmValue::ExternRef(_ref_id) => {
                // ExternRef creation requires Store context
                // Always pass NULL for now
                wasmtime::Val::null_extern_ref()
            }
            WasmValue::FuncRef(_) => wasmtime::Val::null_func_ref(),
        })
    }

    /// Convert wasmtime::Val to WasmValue
    pub fn wasmtime_val_to_wasm_value(val: &wasmtime::Val) -> WasmtimeResult<WasmValue> {
        Ok(match val {
            wasmtime::Val::I32(v) => WasmValue::I32(*v),
            wasmtime::Val::I64(v) => WasmValue::I64(*v),
            wasmtime::Val::F32(v) => WasmValue::F32(f32::from_bits(*v)),
            wasmtime::Val::F64(v) => WasmValue::F64(f64::from_bits(*v)),
            wasmtime::Val::V128(v) => WasmValue::V128(v.as_u128().to_le_bytes()),
            wasmtime::Val::FuncRef(_) => WasmValue::FuncRef(None),
            wasmtime::Val::ExternRef(_ext_ref) => {
                // ExternRef data extraction requires Store context
                // For now, just preserve None
                WasmValue::ExternRef(None)
            }
            wasmtime::Val::AnyRef(_) => WasmValue::ExternRef(None),
            wasmtime::Val::ExnRef(_) => WasmValue::ExternRef(None),
            wasmtime::Val::ContRef(_) => WasmValue::ExternRef(None),
        })
    }

    /// Convert wasmtime::ValType to integer representation
    fn wasmtime_val_type_to_int(val_type: wasmtime::ValType) -> i32 {
        match val_type {
            wasmtime::ValType::I32 => 0,
            wasmtime::ValType::I64 => 1,
            wasmtime::ValType::F32 => 2,
            wasmtime::ValType::F64 => 3,
            wasmtime::ValType::V128 => 4,
            wasmtime::ValType::Ref(ref_type) => {
                // Check the heap type to determine funcref vs externref
                match ref_type.heap_type() {
                    wasmtime::HeapType::Func => 5,      // FUNCREF
                    wasmtime::HeapType::Extern => 6,    // EXTERNREF
                    wasmtime::HeapType::Any => 7,       // ANYREF
                    wasmtime::HeapType::Eq => 8,        // EQREF
                    wasmtime::HeapType::I31 => 9,       // I31REF
                    wasmtime::HeapType::Struct => 10,   // STRUCTREF
                    wasmtime::HeapType::Array => 11,    // ARRAYREF
                    wasmtime::HeapType::None => 12,     // NULLREF
                    wasmtime::HeapType::NoFunc => 13,   // NULLFUNCREF
                    wasmtime::HeapType::NoExtern => 14, // NULLEXTERNREF
                    _ => 6, // Default to EXTERNREF for other/unknown types
                }
            }
        }
    }

    /// Core function to get exports as a vector (for FFI use)
    pub fn get_exports(instance: &Instance) -> Vec<ExportBinding> {
        instance.exports_map.values().cloned().collect()
    }

    /// Core function to get instance metadata (alias for backwards compatibility)
    pub fn get_metadata(instance: &Instance) -> &InstanceMetadata {
        instance.metadata()
    }

    /// Core function to check if instance is disposed (alias for backwards compatibility)
    pub fn is_disposed(instance: &Instance) -> bool {
        instance.is_disposed()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;

    #[test]
    fn test_instance_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        // Simple WAT module for testing
        let wat = "(module (func (export \"add\") (param i32 i32) (result i32) 
                     local.get 0 local.get 1 i32.add))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to create module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert!(instance.validate().is_ok());
        assert_eq!(instance.metadata().export_count, 1); // "add" function
        assert!(!instance.is_disposed());
    }

    #[test]
    fn test_export_function_call() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"add\") (param i32 i32) (result i32)
                     local.get 0 local.get 1 i32.add))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to create module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        // Call the function with type checking
        let params = vec![WasmValue::I32(5), WasmValue::I32(3)];
        let result = instance
            .call_export_function(&mut store, "add", &params)
            .expect("Failed to call function");

        assert_eq!(result.values.len(), 1);
        match &result.values[0] {
            WasmValue::I32(value) => assert_eq!(*value, 8),
            _ => panic!("Expected I32 result"),
        }
    }

    #[test]
    fn test_export_information() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module 
                     (func (export \"add\") (param i32 i32) (result i32) 
                       local.get 0 local.get 1 i32.add)
                     (memory (export \"mem\") 1))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to create module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        // Check export information
        assert!(instance.get_export_info("add").is_some());
        assert!(instance.get_export_info("mem").is_some());
        assert!(instance.get_export_info("nonexistent").is_none());

        let add_export = instance.get_export_info("add").unwrap();
        match &add_export.export_type {
            ExportKind::Function(sig) => {
                assert_eq!(sig.params.len(), 2);
                assert_eq!(sig.returns.len(), 1);
            }
            _ => panic!("Expected function export"),
        }
    }

    #[test]
    fn test_instance_disposal() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to create module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert!(!instance.is_disposed());

        // Dispose the instance
        instance.dispose().expect("Failed to dispose instance");
        assert!(instance.is_disposed());

        // Should not be able to call functions after disposal
        let params = vec![];
        let result = instance.call_export_function(&mut store, "test", &params);
        assert!(result.is_err());
    }

    #[test]
    fn test_type_validation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"add\") (param i32 i32) (result i32)
                     local.get 0 local.get 1 i32.add))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to create module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        // Test parameter count mismatch
        let wrong_param_count = vec![WasmValue::I32(5)]; // Should be 2 params
        let result = instance.call_export_function(&mut store, "add", &wrong_param_count);
        assert!(result.is_err());

        // Test parameter type mismatch
        let wrong_param_types = vec![WasmValue::F32(5.0), WasmValue::I32(3)]; // Should be i32, i32
        let result = instance.call_export_function(&mut store, "add", &wrong_param_types);
        assert!(result.is_err());
    }

    #[test]
    fn test_wasm_value_conversion() {
        // Test WasmValue creation and extraction
        let i32_val = WasmValue::I32(42);
        let i64_val = WasmValue::I64(123456789);
        let f32_val = WasmValue::F32(3.14);
        let f64_val = WasmValue::F64(2.71828);

        // Test extraction
        assert_eq!(core::extract_i32_value(&i32_val).unwrap(), 42);
        assert_eq!(core::extract_i64_value(&i64_val).unwrap(), 123456789);
        assert_eq!(core::extract_f32_value(&f32_val).unwrap(), 3.14);
        assert_eq!(core::extract_f64_value(&f64_val).unwrap(), 2.71828);

        // Test type mismatch errors
        assert!(core::extract_i64_value(&i32_val).is_err());
        assert!(core::extract_f32_value(&i64_val).is_err());
    }

    // === Phase 1: Additional Instance Tests ===

    #[test]
    fn test_instance_with_memory_export() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (memory (export \"mem\") 1 4)
                     (func (export \"get_mem_size\") (result i32)
                       memory.size))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        // Should have 2 exports: mem and get_mem_size
        assert_eq!(instance.metadata().export_count, 2, "Should have 2 exports");

        let mem_export = instance.get_export_info("mem");
        assert!(mem_export.is_some(), "Should have memory export");
    }

    #[test]
    fn test_instance_with_global_export() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (global (export \"answer\") i32 (i32.const 42))
                     (func (export \"get_answer\") (result i32)
                       global.get 0))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert_eq!(instance.metadata().export_count, 2, "Should have 2 exports");

        let global_export = instance.get_export_info("answer");
        assert!(global_export.is_some(), "Should have global export");
    }

    #[test]
    fn test_instance_with_table_export() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (table (export \"tbl\") 1 funcref))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert_eq!(instance.metadata().export_count, 1, "Should have 1 export");

        let table_export = instance.get_export_info("tbl");
        assert!(table_export.is_some(), "Should have table export");
    }

    #[test]
    fn test_get_func_nonexistent() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"add\") (param i32 i32) (result i32)
                     local.get 0 local.get 1 i32.add))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let nonexistent = instance.get_export_info("nonexistent");
        assert!(nonexistent.is_none(), "Should not find nonexistent export");
    }

    #[test]
    fn test_metadata_accessors() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"f1\") (result i32) i32.const 1)
                     (func (export \"f2\") (result i32) i32.const 2)
                     (func (export \"f3\") (result i32) i32.const 3))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let metadata = instance.metadata();
        assert_eq!(metadata.export_count, 3, "Should have 3 exports");
        assert_eq!(metadata.import_count, 0, "Should have 0 imports");
        assert!(!metadata.disposed, "Should not be disposed");
        assert_eq!(
            metadata.state,
            InstanceState::Created,
            "State should be Created"
        );
    }

    #[test]
    fn test_validate_instance() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let validation = instance.validate();
        assert!(validation.is_ok(), "Instance validation should succeed");
    }

    #[test]
    fn test_is_disposed_after_disposal() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert!(!instance.is_disposed(), "Should not be disposed initially");

        instance.dispose().expect("Failed to dispose");

        assert!(instance.is_disposed(), "Should be disposed after dispose()");
    }

    #[test]
    fn test_call_after_dispose_fails() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        instance.dispose().expect("Failed to dispose");

        let result = instance.call_export_function(&mut store, "test", &[]);
        assert!(result.is_err(), "Should fail to call after disposal");
    }

    #[test]
    fn test_all_exports_multiple() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"add\") (param i32 i32) (result i32)
                       local.get 0 local.get 1 i32.add)
                     (func (export \"sub\") (param i32 i32) (result i32)
                       local.get 0 local.get 1 i32.sub)
                     (memory (export \"mem\") 1)
                     (global (export \"val\") i32 (i32.const 100)))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let exports = instance.exports(&mut store).expect("Failed to get exports");
        assert_eq!(exports.len(), 4, "Should have 4 exports");

        assert!(
            exports.contains(&"add".to_string()),
            "Should have 'add' export"
        );
        assert!(
            exports.contains(&"sub".to_string()),
            "Should have 'sub' export"
        );
        assert!(
            exports.contains(&"mem".to_string()),
            "Should have 'mem' export"
        );
        assert!(
            exports.contains(&"val".to_string()),
            "Should have 'val' export"
        );
    }

    #[test]
    fn test_call_function_with_multiple_results() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"multi\") (result i32 i64)
                       i32.const 42
                       i64.const 100))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let result = instance
            .call_export_function(&mut store, "multi", &[])
            .expect("Failed to call function");

        assert_eq!(result.values.len(), 2, "Should have 2 return values");
        assert_eq!(
            result.values[0],
            WasmValue::I32(42),
            "First result should be 42"
        );
        assert_eq!(
            result.values[1],
            WasmValue::I64(100),
            "Second result should be 100"
        );
    }

    #[test]
    fn test_call_function_with_f32_params() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"addf32\") (param f32 f32) (result f32)
                       local.get 0 local.get 1 f32.add))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let params = vec![WasmValue::F32(2.5), WasmValue::F32(3.5)];
        let result = instance
            .call_export_function(&mut store, "addf32", &params)
            .expect("Failed to call function");

        match &result.values[0] {
            WasmValue::F32(v) => {
                assert!((v - 6.0).abs() < 0.001, "Result should be ~6.0");
            }
            _ => panic!("Expected F32 result"),
        }
    }

    #[test]
    fn test_call_function_with_f64_params() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"mulf64\") (param f64 f64) (result f64)
                       local.get 0 local.get 1 f64.mul))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let params = vec![WasmValue::F64(2.5), WasmValue::F64(4.0)];
        let result = instance
            .call_export_function(&mut store, "mulf64", &params)
            .expect("Failed to call function");

        match &result.values[0] {
            WasmValue::F64(v) => {
                assert!((v - 10.0).abs() < 0.001, "Result should be ~10.0");
            }
            _ => panic!("Expected F64 result"),
        }
    }

    #[test]
    fn test_call_function_with_i64_params() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"add64\") (param i64 i64) (result i64)
                       local.get 0 local.get 1 i64.add))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let params = vec![WasmValue::I64(1000000000000), WasmValue::I64(2000000000000)];
        let result = instance
            .call_export_function(&mut store, "add64", &params)
            .expect("Failed to call function");

        match &result.values[0] {
            WasmValue::I64(v) => {
                assert_eq!(*v, 3000000000000, "Result should be 3000000000000");
            }
            _ => panic!("Expected I64 result"),
        }
    }

    #[test]
    fn test_ffi_wasm_value_roundtrip() {
        // Test I32 roundtrip
        let i32_val = WasmValue::I32(-12345);
        let ffi_val = FfiWasmValue::from_wasm_value(&i32_val);
        let back = ffi_val.to_wasm_value();
        assert_eq!(i32_val, back, "I32 roundtrip should preserve value");

        // Test I64 roundtrip
        let i64_val = WasmValue::I64(-9876543210);
        let ffi_val = FfiWasmValue::from_wasm_value(&i64_val);
        let back = ffi_val.to_wasm_value();
        assert_eq!(i64_val, back, "I64 roundtrip should preserve value");

        // Test F32 roundtrip
        let f32_val = WasmValue::F32(3.14159);
        let ffi_val = FfiWasmValue::from_wasm_value(&f32_val);
        let back = ffi_val.to_wasm_value();
        match (f32_val, back) {
            (WasmValue::F32(a), WasmValue::F32(b)) => {
                assert!(
                    (a - b).abs() < 0.00001,
                    "F32 roundtrip should preserve value"
                );
            }
            _ => panic!("Type mismatch"),
        }

        // Test F64 roundtrip
        let f64_val = WasmValue::F64(2.718281828);
        let ffi_val = FfiWasmValue::from_wasm_value(&f64_val);
        let back = ffi_val.to_wasm_value();
        match (f64_val, back) {
            (WasmValue::F64(a), WasmValue::F64(b)) => {
                assert!(
                    (a - b).abs() < 0.0000001,
                    "F64 roundtrip should preserve value"
                );
            }
            _ => panic!("Type mismatch"),
        }

        // Test V128 roundtrip
        let v128_val = WasmValue::V128([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]);
        let ffi_val = FfiWasmValue::from_wasm_value(&v128_val);
        let back = ffi_val.to_wasm_value();
        assert_eq!(v128_val, back, "V128 roundtrip should preserve value");

        // Test FuncRef roundtrip
        let funcref_val = WasmValue::FuncRef(Some(42));
        let ffi_val = FfiWasmValue::from_wasm_value(&funcref_val);
        let back = ffi_val.to_wasm_value();
        assert_eq!(funcref_val, back, "FuncRef roundtrip should preserve value");

        // Test ExternRef roundtrip
        let externref_val = WasmValue::ExternRef(Some(99));
        let ffi_val = FfiWasmValue::from_wasm_value(&externref_val);
        let back = ffi_val.to_wasm_value();
        assert_eq!(
            externref_val, back,
            "ExternRef roundtrip should preserve value"
        );
    }

    #[test]
    fn test_ffi_wasm_value_null_refs() {
        // Test null FuncRef
        let null_funcref = WasmValue::FuncRef(None);
        let ffi_val = FfiWasmValue::from_wasm_value(&null_funcref);
        let back = ffi_val.to_wasm_value();
        // The roundtrip converts None to 0 and back
        match back {
            WasmValue::FuncRef(None) => { /* OK */ }
            _ => panic!("Null FuncRef should roundtrip correctly"),
        }

        // Test null ExternRef
        let null_externref = WasmValue::ExternRef(None);
        let ffi_val = FfiWasmValue::from_wasm_value(&null_externref);
        let back = ffi_val.to_wasm_value();
        match back {
            WasmValue::ExternRef(None) => { /* OK */ }
            _ => panic!("Null ExternRef should roundtrip correctly"),
        }
    }

    #[test]
    fn test_extract_value_type_errors() {
        let i32_val = WasmValue::I32(42);
        let i64_val = WasmValue::I64(100);
        let f32_val = WasmValue::F32(1.0);
        let f64_val = WasmValue::F64(2.0);

        // All cross-type extractions should fail
        assert!(
            core::extract_i64_value(&i32_val).is_err(),
            "i64 from i32 should fail"
        );
        assert!(
            core::extract_f32_value(&i32_val).is_err(),
            "f32 from i32 should fail"
        );
        assert!(
            core::extract_f64_value(&i32_val).is_err(),
            "f64 from i32 should fail"
        );

        assert!(
            core::extract_i32_value(&i64_val).is_err(),
            "i32 from i64 should fail"
        );
        assert!(
            core::extract_f32_value(&i64_val).is_err(),
            "f32 from i64 should fail"
        );
        assert!(
            core::extract_f64_value(&i64_val).is_err(),
            "f64 from i64 should fail"
        );

        assert!(
            core::extract_i32_value(&f32_val).is_err(),
            "i32 from f32 should fail"
        );
        assert!(
            core::extract_i64_value(&f32_val).is_err(),
            "i64 from f32 should fail"
        );
        assert!(
            core::extract_f64_value(&f32_val).is_err(),
            "f64 from f32 should fail"
        );

        assert!(
            core::extract_i32_value(&f64_val).is_err(),
            "i32 from f64 should fail"
        );
        assert!(
            core::extract_i64_value(&f64_val).is_err(),
            "i64 from f64 should fail"
        );
        assert!(
            core::extract_f32_value(&f64_val).is_err(),
            "f32 from f64 should fail"
        );
    }

    #[test]
    fn test_instance_state_lifecycle() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert_eq!(
            instance.metadata().state,
            InstanceState::Created,
            "Initial state should be Created"
        );
    }

    #[test]
    fn test_empty_module_instance() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module)";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert_eq!(
            instance.metadata().export_count,
            0,
            "Empty module should have no exports"
        );
        assert_eq!(
            instance.metadata().import_count,
            0,
            "Empty module should have no imports"
        );
    }

    #[test]
    fn test_instance_created_thread_id() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert_eq!(
            instance.metadata().creator_thread_id,
            std::thread::current().id(),
            "Creator thread ID should match current thread"
        );
    }

    #[test]
    fn test_core_create_and_destroy_instance() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            core::create_instance(&mut store, &module).expect("Failed to create instance via core");

        assert!(!instance.is_disposed(), "Instance should not be disposed");

        // Get raw pointer for destruction
        let ptr = Box::into_raw(instance) as *mut std::ffi::c_void;

        unsafe {
            core::destroy_instance(ptr);
        }
        // After destruction, the memory is freed - we don't access it
    }

    #[test]
    fn test_core_has_export() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"myFunc\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert!(
            core::has_export(&instance, "myFunc"),
            "Should have 'myFunc' export"
        );
        assert!(
            !core::has_export(&instance, "noSuchFunc"),
            "Should not have 'noSuchFunc' export"
        );
    }

    #[test]
    fn test_core_get_exports() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module
                     (func (export \"f1\") (result i32) i32.const 1)
                     (func (export \"f2\") (result i32) i32.const 2))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let exports = core::get_exports(&instance);
        assert_eq!(exports.len(), 2, "Should have 2 exports");
    }

    #[test]
    fn test_core_get_metadata() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        let metadata = core::get_metadata(&instance);
        assert_eq!(metadata.export_count, 1, "Should have 1 export");
        assert!(!metadata.disposed, "Should not be disposed");
    }

    #[test]
    fn test_core_dispose_instance() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        let mut instance =
            Instance::new_without_imports(&mut store, &module).expect("Failed to create instance");

        assert!(
            !core::is_disposed(&instance),
            "Should not be disposed initially"
        );

        core::dispose_instance(&mut instance).expect("Failed to dispose");

        assert!(
            core::is_disposed(&instance),
            "Should be disposed after core::dispose_instance"
        );
    }
}

//
// Native C exports for JNI and Panama FFI consumption
//

use crate::shared_ffi::{FFI_ERROR, FFI_SUCCESS};
use std::ffi::CStr;
use std::os::raw::{c_char, c_int, c_void};

/// Create a new instance without imports
///
/// # Safety
///
/// store_ptr and module_ptr must be valid pointers
/// Returns pointer to instance that must be freed with wasmtime4j_instance_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_new_without_imports(
    store_ptr: *mut c_void,
    module_ptr: *const c_void,
) -> *mut c_void {
    if store_ptr.is_null() || module_ptr.is_null() {
        return std::ptr::null_mut();
    }

    match (
        crate::store::core::get_store_mut(store_ptr),
        crate::module::core::get_module_ref(module_ptr),
    ) {
        (Ok(store), Ok(module)) => match core::create_instance(store, module) {
            Ok(instance) => Box::into_raw(instance) as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        },
        _ => std::ptr::null_mut(),
    }
}

/// Destroy instance and free resources
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_destroy(instance_ptr: *mut c_void) {
    if !instance_ptr.is_null() {
        core::destroy_instance(instance_ptr);
    }
}

/// Alias for wasmtime4j_instance_new_without_imports (Panama FFI compatibility)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_create(
    store_ptr: *mut c_void,
    module_ptr: *const c_void,
) -> *mut c_void {
    wasmtime4j_instance_new_without_imports(store_ptr, module_ptr)
}

/// Check if instance has specific export
///
/// # Safety
///
/// instance_ptr must be valid, name must be a valid null-terminated C string
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_has_export(
    instance_ptr: *const c_void,
    name: *const c_char,
) -> c_int {
    if instance_ptr.is_null() || name.is_null() {
        return FFI_ERROR;
    }

    match core::get_instance_ref(instance_ptr) {
        Ok(instance) => match CStr::from_ptr(name).to_str() {
            Ok(name_str) => {
                if core::has_export(instance, name_str) {
                    1
                } else {
                    0
                }
            }
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Get number of exports in instance
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_export_count(instance_ptr: *const c_void) -> usize {
    match core::get_instance_ref(instance_ptr) {
        Ok(instance) => core::get_exports(instance).len(),
        Err(_) => 0,
    }
}

/// Dispose instance resources
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_dispose(instance_ptr: *mut c_void) -> c_int {
    match core::get_instance_mut(instance_ptr) {
        Ok(instance) => match core::dispose_instance(instance) {
            Ok(_) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Check if instance is disposed
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_is_disposed(instance_ptr: *const c_void) -> c_int {
    match core::get_instance_ref(instance_ptr) {
        Ok(instance) => {
            if core::is_disposed(instance) {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Call exported function with simple integer parameters
///
/// # Safety
///
/// All pointers must be valid, params array must have correct size
/// Returns the i32 result or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_i32_function(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    params: *const i32,
    param_count: usize,
) -> i32 {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return 0;
    }

    match (
        core::get_instance_mut(instance_ptr as *mut c_void),
        crate::store::core::get_store_mut(store_ptr),
        CStr::from_ptr(function_name).to_str(),
    ) {
        (Ok(instance), Ok(store), Ok(name_str)) => {
            let param_values: Vec<WasmValue> = if param_count > 0 && !params.is_null() {
                std::slice::from_raw_parts(params, param_count)
                    .iter()
                    .map(|&p| WasmValue::I32(p))
                    .collect()
            } else {
                Vec::new()
            };

            match instance.call_export_function(store, name_str, &param_values) {
                Ok(results) => {
                    if let Some(WasmValue::I32(result)) = results.values.into_iter().next() {
                        result
                    } else {
                        0
                    }
                }
                Err(_) => 0,
            }
        }
        _ => 0,
    }
}

/// Call exported function with no parameters that returns i32
///
/// # Safety
///
/// All pointers must be valid
/// Returns the i32 result or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_i32_function_no_params(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
) -> i32 {
    wasmtime4j_instance_call_i32_function(
        instance_ptr,
        store_ptr,
        function_name,
        std::ptr::null(),
        0,
    )
}

/// Get instance creation timestamp in microseconds since epoch
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_created_at_micros(instance_ptr: *const c_void) -> u64 {
    match core::get_instance_ref(instance_ptr) {
        Ok(instance) => {
            let metadata = core::get_metadata(instance);
            metadata.created_at.elapsed().as_micros() as u64
        }
        Err(_) => 0,
    }
}

/// Get instance export count
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_metadata_export_count(
    instance_ptr: *const c_void,
) -> usize {
    match core::get_instance_ref(instance_ptr) {
        Ok(instance) => core::get_metadata(instance).export_count,
        Err(_) => 0,
    }
}

/// Call instance function with WasmValue parameters (Panama FFI compatibility)
///
/// # Safety
///
/// All pointers must be valid, params must point to WasmValue array of size param_count
/// Results buffer must have space for at least max_results WasmValues
///
/// Returns number of actual results (>= 0), or -1 on error. Fills results_ptr with return values
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_function(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    params_ptr: *const c_void,
    param_count: usize,
    results_ptr: *mut c_void,
    max_results: usize,
) -> isize {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return -1;
    }

    let instance_result = core::get_instance_mut(instance_ptr);
    let store_result = crate::store::core::get_store_mut(store_ptr);
    let name_result = CStr::from_ptr(function_name).to_str();

    match (instance_result, store_result, name_result) {
        (Ok(instance), Ok(store), Ok(name_str)) => {
            // Convert parameters from FFI WasmValue array
            // Layout: tag (4 bytes) + value (16 bytes max) = 20 bytes per WasmValue
            let params: Vec<WasmValue> = if param_count > 0 && !params_ptr.is_null() {
                let mut result = Vec::with_capacity(param_count);
                let base_ptr = params_ptr as *const u8;

                for i in 0..param_count {
                    let offset = i * 20;
                    let tag = std::ptr::read(base_ptr.add(offset) as *const i32);

                    let value = match tag {
                        0 => {
                            // I32
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i32);
                            WasmValue::I32(val)
                        }
                        1 => {
                            // I64
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i64);
                            WasmValue::I64(val)
                        }
                        2 => {
                            // F32
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const f32);
                            WasmValue::F32(val)
                        }
                        3 => {
                            // F64
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const f64);
                            WasmValue::F64(val)
                        }
                        4 => {
                            // V128
                            let mut bytes = [0u8; 16];
                            std::ptr::copy_nonoverlapping(
                                base_ptr.add(offset + 4),
                                bytes.as_mut_ptr(),
                                16,
                            );
                            WasmValue::V128(bytes)
                        }
                        5 => {
                            // FUNCREF
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i64);
                            WasmValue::FuncRef(if val == 0 { None } else { Some(val) })
                        }
                        6 => {
                            // EXTERNREF
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i64);
                            WasmValue::ExternRef(if val == 0 { None } else { Some(val) })
                        }
                        _ => {
                            // Invalid tag - return error
                            return -1;
                        }
                    };
                    result.push(value);
                }
                result
            } else {
                Vec::new()
            };

            // Call the function
            match instance.call_export_function(store, name_str, &params) {
                Ok(execution_result) => {
                    let result_values = execution_result.values;
                    let actual_count = result_values.len().min(max_results);

                    // Copy results to output buffer
                    // Layout: tag (4 bytes) + value (16 bytes max) = 20 bytes per WasmValue
                    if actual_count > 0 && !results_ptr.is_null() {
                        for (i, value) in result_values.iter().take(actual_count).enumerate() {
                            let base_offset = i * 20;
                            let dest_ptr = results_ptr as *mut u8;

                            // Write tag and value based on type
                            match value {
                                WasmValue::I32(val) => {
                                    // tag = 0
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 0);
                                    // value at offset +4
                                    std::ptr::write(
                                        dest_ptr.add(base_offset + 4) as *mut i32,
                                        *val,
                                    );
                                }
                                WasmValue::I64(val) => {
                                    // tag = 1
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 1);
                                    // value at offset +4
                                    std::ptr::write(
                                        dest_ptr.add(base_offset + 4) as *mut i64,
                                        *val,
                                    );
                                }
                                WasmValue::F32(val) => {
                                    // tag = 2
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 2);
                                    // value at offset +4
                                    std::ptr::write(
                                        dest_ptr.add(base_offset + 4) as *mut f32,
                                        *val,
                                    );
                                }
                                WasmValue::F64(val) => {
                                    // tag = 3
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 3);
                                    // value at offset +4
                                    std::ptr::write(
                                        dest_ptr.add(base_offset + 4) as *mut f64,
                                        *val,
                                    );
                                }
                                WasmValue::V128(bytes) => {
                                    // tag = 4
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 4);
                                    // value at offset +4 (16 bytes)
                                    std::ptr::copy_nonoverlapping(
                                        bytes.as_ptr(),
                                        dest_ptr.add(base_offset + 4),
                                        16,
                                    );
                                }
                                WasmValue::FuncRef(opt_id) => {
                                    // tag = 5
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 5);
                                    // value at offset +4 (i64 reference ID or 0 for null)
                                    let id = opt_id.unwrap_or(0);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i64, id);
                                }
                                WasmValue::ExternRef(opt_id) => {
                                    // tag = 6
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 6);
                                    // value at offset +4 (i64 reference ID or 0 for null)
                                    let id = opt_id.unwrap_or(0);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i64, id);
                                }
                            }
                        }
                    }

                    actual_count as isize
                }
                Err(e) => {
                    crate::error::ffi_utils::set_last_error(e);
                    -1
                }
            }
        }
        _ => -1,
    }
}

/// Call a WebAssembly function asynchronously with timeout support
///
/// This function uses `Func::call_async` with the global Tokio runtime and blocks
/// waiting for the result. Requires the Engine to be created with `async_support(true)`.
///
/// # Arguments
/// * `instance_ptr` - Pointer to the Instance
/// * `store_ptr` - Pointer to the Store
/// * `function_name` - Name of the exported function to call
/// * `params_ptr` - Pointer to array of WasmValue parameters (20 bytes each)
/// * `param_count` - Number of parameters
/// * `results_ptr` - Output buffer for results (20 bytes per WasmValue)
/// * `max_results` - Maximum number of results the buffer can hold
/// * `timeout_ms` - Timeout in milliseconds (0 = no timeout)
///
/// # Returns
/// Number of actual results (>= 0) on success, or negative value on error:
/// * -1: Invalid parameters or general error
/// * -2: Timeout exceeded
/// * -3: Function not found
/// * -4: Async not supported (engine not configured for async)
///
/// # Safety
/// This function is unsafe because it dereferences raw pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_function_async(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    params_ptr: *const c_void,
    param_count: usize,
    results_ptr: *mut c_void,
    max_results: usize,
    timeout_ms: u64,
) -> isize {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return -1;
    }

    match (
        core::get_instance_mut(instance_ptr),
        crate::store::core::get_store_mut(store_ptr),
        CStr::from_ptr(function_name).to_str(),
    ) {
        (Ok(instance), Ok(store), Ok(name_str)) => {
            // Convert parameters from FFI WasmValue array
            // Layout: tag (4 bytes) + value (16 bytes max) = 20 bytes per WasmValue
            let params: Vec<WasmValue> = if param_count > 0 && !params_ptr.is_null() {
                let mut result = Vec::with_capacity(param_count);
                let base_ptr = params_ptr as *const u8;

                for i in 0..param_count {
                    let offset = i * 20;
                    let tag = std::ptr::read(base_ptr.add(offset) as *const i32);

                    let value = match tag {
                        0 => {
                            // I32
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i32);
                            WasmValue::I32(val)
                        }
                        1 => {
                            // I64
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i64);
                            WasmValue::I64(val)
                        }
                        2 => {
                            // F32
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const f32);
                            WasmValue::F32(val)
                        }
                        3 => {
                            // F64
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const f64);
                            WasmValue::F64(val)
                        }
                        4 => {
                            // V128
                            let mut bytes = [0u8; 16];
                            std::ptr::copy_nonoverlapping(
                                base_ptr.add(offset + 4),
                                bytes.as_mut_ptr(),
                                16,
                            );
                            WasmValue::V128(bytes)
                        }
                        5 => {
                            // FUNCREF
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i64);
                            WasmValue::FuncRef(if val == 0 { None } else { Some(val) })
                        }
                        6 => {
                            // EXTERNREF
                            let val = std::ptr::read(base_ptr.add(offset + 4) as *const i64);
                            WasmValue::ExternRef(if val == 0 { None } else { Some(val) })
                        }
                        _ => {
                            // Invalid tag - return error
                            return -1;
                        }
                    };
                    result.push(value);
                }
                result
            } else {
                Vec::new()
            };

            // Get the function from the instance
            let func = match instance.get_func(store, name_str) {
                Ok(Some(f)) => f,
                Ok(None) => return -3, // Function not found
                Err(_) => return -1,
            };

            // Convert WasmValue params to wasmtime::Val
            let wasmtime_params: Vec<Val> = params
                .iter()
                .map(|wv| match wv {
                    WasmValue::I32(v) => Val::I32(*v),
                    WasmValue::I64(v) => Val::I64(*v),
                    WasmValue::F32(v) => Val::F32(v.to_bits()),
                    WasmValue::F64(v) => Val::F64(v.to_bits()),
                    WasmValue::V128(bytes) => {
                        Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes)))
                    }
                    WasmValue::FuncRef(_) => Val::null_func_ref(),
                    WasmValue::ExternRef(_) => Val::null_extern_ref(),
                })
                .collect();

            // Get function type to determine result count
            let mut store_lock = match store.try_lock_store() {
                Ok(guard) => guard,
                Err(_) => return -1, // Store closed or unavailable
            };
            let func_type = func.ty(&mut *store_lock);
            let result_count = func_type.results().len();
            let mut results = vec![Val::I32(0); result_count];

            // Get the async runtime
            let runtime = crate::async_runtime::get_async_runtime();

            // Execute the async call with optional timeout
            let call_future = func.call_async(&mut *store_lock, &wasmtime_params, &mut results);

            let call_result = if timeout_ms > 0 {
                // Execute with timeout
                runtime.block_on(async {
                    tokio::time::timeout(std::time::Duration::from_millis(timeout_ms), call_future)
                        .await
                })
            } else {
                // Execute without timeout - wrap in Ok to match timeout result type
                Ok(runtime.block_on(call_future))
            };

            // Handle the result
            match call_result {
                Ok(Ok(())) => {
                    // Success - copy results to output buffer
                    let actual_count = results.len().min(max_results);

                    if actual_count > 0 && !results_ptr.is_null() {
                        for (i, val) in results.iter().take(actual_count).enumerate() {
                            let base_offset = i * 20;
                            let dest_ptr = results_ptr as *mut u8;

                            match val {
                                Val::I32(v) => {
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 0);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i32, *v);
                                }
                                Val::I64(v) => {
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 1);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i64, *v);
                                }
                                Val::F32(v) => {
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 2);
                                    std::ptr::write(
                                        dest_ptr.add(base_offset + 4) as *mut f32,
                                        f32::from_bits(*v),
                                    );
                                }
                                Val::F64(v) => {
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 3);
                                    std::ptr::write(
                                        dest_ptr.add(base_offset + 4) as *mut f64,
                                        f64::from_bits(*v),
                                    );
                                }
                                Val::V128(v) => {
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 4);
                                    let bytes = u128::from(*v).to_le_bytes();
                                    std::ptr::copy_nonoverlapping(
                                        bytes.as_ptr(),
                                        dest_ptr.add(base_offset + 4),
                                        16,
                                    );
                                }
                                Val::FuncRef(_func_ref) => {
                                    // tag = 5, value = 0 (null funcref, we don't have ID tracking here)
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 5);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i64, 0);
                                }
                                Val::ExternRef(_extern_ref) => {
                                    // tag = 6, value = 0 (null externref, we don't have ID tracking here)
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 6);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i64, 0);
                                }
                                _ => {
                                    // Other ref types - treat as externref
                                    std::ptr::write(dest_ptr.add(base_offset) as *mut i32, 6);
                                    std::ptr::write(dest_ptr.add(base_offset + 4) as *mut i64, 0);
                                }
                            }
                        }
                    }

                    actual_count as isize
                }
                Ok(Err(e)) => {
                    // Async call failed - check if it's an async support error
                    let err_msg = e.to_string();
                    if err_msg.contains("async support") || err_msg.contains("async_support") {
                        -4 // Async not supported
                    } else {
                        log::error!("Async function call failed: {}", e);
                        -1
                    }
                }
                Err(_) => {
                    // Timeout
                    log::warn!("Async function call timed out after {}ms", timeout_ms);
                    -2
                }
            }
        }
        _ => -1,
    }
}

/// Get exported memory by name
/// Returns wrapped Memory handle or null if not found
///
/// IMPORTANT: This function returns a wrapped crate::memory::Memory, NOT a raw wasmtime::Memory.
/// All callers must treat the returned pointer as a crate::memory::Memory pointer.
/// This function handles both regular and shared memory exports (threads proposal).
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_get_memory_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }

    let name_str = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match (
        core::get_instance_ref(instance_ptr),
        crate::store::core::get_store_mut(store_ptr),
    ) {
        (Ok(instance), Ok(store)) => {
            // First try to get regular memory
            match core::get_exported_memory(instance, store, name_str) {
                Ok(Some(wasmtime_memory)) => {
                    // Get the memory type from the wasmtime::Memory using store context
                    let memory_type_result =
                        store.with_context_ro(|ctx| Ok(wasmtime_memory.ty(&ctx)));

                    match memory_type_result {
                        Ok(memory_type) => {
                            // Wrap the raw wasmtime::Memory in our Memory wrapper
                            let wrapped_memory = crate::memory::Memory::from_wasmtime_memory(
                                wasmtime_memory,
                                memory_type,
                            );

                            // Wrap in ValidatedMemory to match JNI's nativeGetMemory behavior
                            match crate::memory::core::create_validated_memory(wrapped_memory) {
                                Ok(validated_ptr) => validated_ptr as *mut c_void,
                                Err(_) => std::ptr::null_mut(),
                            }
                        }
                        Err(_) => std::ptr::null_mut(),
                    }
                }
                _ => {
                    // Regular memory not found, try shared memory (for threads proposal)
                    // SharedMemory is exported as Extern::SharedMemory, not Extern::Memory
                    match instance.get_shared_memory(store, name_str) {
                        Ok(Some(shared_memory)) => {
                            // Wrap shared memory in our Memory wrapper
                            let wrapped_memory =
                                crate::memory::Memory::from_shared_memory(shared_memory);

                            // Wrap in ValidatedMemory to match JNI's nativeGetMemory behavior
                            match crate::memory::core::create_validated_memory(wrapped_memory) {
                                Ok(validated_ptr) => validated_ptr as *mut c_void,
                                Err(_) => std::ptr::null_mut(),
                            }
                        }
                        _ => std::ptr::null_mut(),
                    }
                }
            }
        }
        _ => std::ptr::null_mut(),
    }
}

/// Get exported table by name
/// Returns wrapped Table handle or null if not found
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_get_table_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    log::debug!(
        "wasmtime4j_instance_get_table_by_name: instance_ptr={:?}, store_ptr={:?}",
        instance_ptr,
        store_ptr
    );

    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        log::debug!("wasmtime4j_instance_get_table_by_name: null pointer argument");
        return std::ptr::null_mut();
    }

    let name_str = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };
    log::debug!(
        "wasmtime4j_instance_get_table_by_name: looking for table '{}'",
        name_str
    );

    match (
        core::get_instance_ref(instance_ptr),
        crate::store::core::get_store_mut(store_ptr),
    ) {
        (Ok(instance), Ok(store)) => {
            match core::get_exported_table(instance, store, name_str) {
                Ok(Some(wasmtime_table)) => {
                    log::debug!("wasmtime4j_instance_get_table_by_name: found wasmtime table");
                    // Wrap the raw wasmtime::Table in our Table struct
                    match crate::table::Table::from_wasmtime_table(
                        wasmtime_table,
                        store,
                        Some(name_str.to_string()),
                    ) {
                        Ok(wrapped_table) => {
                            let boxed = Box::new(wrapped_table);
                            Box::into_raw(boxed) as *mut c_void
                        }
                        Err(e) => {
                            log::error!("Failed to wrap exported table '{}': {}", name_str, e);
                            std::ptr::null_mut()
                        }
                    }
                }
                _ => {
                    log::debug!("wasmtime4j_instance_get_table_by_name: table not found");
                    std::ptr::null_mut()
                }
            }
        }
        _ => {
            log::debug!("wasmtime4j_instance_get_table_by_name: failed to get instance/store refs");
            std::ptr::null_mut()
        }
    }
}

/// Get exported global by name
/// Returns global handle or null if not found
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_get_global_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }

    let name_str = match std::ffi::CStr::from_ptr(name).to_str() {
        Ok(s) => s,
        Err(_) => return std::ptr::null_mut(),
    };

    match (
        core::get_instance_ref(instance_ptr),
        crate::store::core::get_store_mut(store_ptr),
    ) {
        (Ok(instance), Ok(store)) => match core::get_exported_global(instance, store, name_str) {
            Ok(Some(global)) => Box::into_raw(Box::new(global)) as *mut c_void,
            _ => std::ptr::null_mut(),
        },
        _ => std::ptr::null_mut(),
    }
}
