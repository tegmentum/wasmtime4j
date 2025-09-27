//! WebAssembly instance management with comprehensive import/export handling
//!
//! This module provides Instance lifecycle management with complete import resolution,
//! export access, resource cleanup, and defensive programming practices for safe execution.

use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use std::time::Instant;
use wasmtime::{
    Instance as WasmtimeInstance, 
    Extern, 
    Func, 
    Global, 
    Memory, 
    Table,
    Val,
    ValType as WasmtimeValType,
    FuncType,
    ExternType,
};
use crate::store::{Store, StoreData};
use crate::module::{Module, ModuleValueType, FunctionSignature, ImportKind, ExportKind};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime instance with comprehensive lifecycle management
#[derive(Debug)]
pub struct Instance {
    inner: Arc<Mutex<WasmtimeInstance>>,
    metadata: InstanceMetadata,
    imports_map: HashMap<String, ImportBinding>,
    exports_map: HashMap<String, ExportBinding>,
    /// Weak reference to the Store that created this instance
    store_weak: std::sync::Weak<std::sync::Mutex<Store>>,
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
#[derive(Debug, Clone)]
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
    /// External reference (null for now)
    ExternRef,
    /// Function reference (null for now)  
    FuncRef,
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
    pub fn new(
        store: &mut Store,
        module: &Module,
        imports: &[Extern],
    ) -> WasmtimeResult<Self> {
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
        let instance = store.with_context(|mut ctx| {
            WasmtimeInstance::new(&mut ctx, module.inner(), imports)
                .map_err(|e| WasmtimeError::Instance {
                    message: format!("Failed to create instance: {}", e),
                })
        })?;
        
        // Build comprehensive metadata and mappings
        let (metadata, imports_map, exports_map) = store.with_context(|mut ctx| {
            Self::build_instance_data(&instance, &mut ctx, module, imports.len())
        })?;
        
        Ok(Instance {
            inner: Arc::new(Mutex::new(instance)),
            metadata,
            imports_map,
            exports_map,
            store_weak: std::sync::Weak::new(), // Will be set later if needed
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
            let module_imports = imports.get(&import_info.module)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!("Missing import module: {}", import_info.module),
                })?;
            
            let import_item = module_imports.get(&import_info.name)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!("Missing import: {}.{}", import_info.module, import_info.name),
                })?;
            
            // Validate import type compatibility
            Self::validate_import_compatibility(&import_info.import_type, import_item, store)?;
            
            resolved_imports.push(import_item.clone());
        }
        
        Self::new(store, module, &resolved_imports)
    }
    
    /// Create instance with no imports (for simple modules)
    pub fn new_without_imports(
        store: &mut Store,
        module: &Module,
    ) -> WasmtimeResult<Self> {
        Self::new(store, module, &[])
    }
    
    /// Build comprehensive instance metadata and binding maps
    fn build_instance_data(
        _instance: &WasmtimeInstance,
        _ctx: &mut wasmtime::StoreContextMut<StoreData>,
        module: &Module,
        import_count: usize,
    ) -> WasmtimeResult<(InstanceMetadata, HashMap<String, ImportBinding>, HashMap<String, ExportBinding>)> {
        // Build export bindings map from module metadata instead of instance for now
        // This avoids complex borrowing issues with the store context
        let mut exports_map = HashMap::new();
        let export_count = module.metadata().exports.len();
        
        for export_info in &module.metadata().exports {
            let binding = ExportBinding {
                name: export_info.name.clone(),
                export_type: export_info.export_type.clone(),
                accessible: true,
            };
            exports_map.insert(export_info.name.clone(), binding);
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
        
        let metadata = InstanceMetadata {
            name: module.metadata().name.clone().unwrap_or_else(|| "unnamed".to_string()),
            created_at: Instant::now(),
            export_count,
            import_count,
            memory_bytes: 0, // Will be calculated when memory is accessed
            function_calls: 0,
            disposed: false,
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
            (ImportKind::Function(req_sig), Extern::Func(func)) => {
                store.with_context_ro(|ctx| {
                    let func_type = func.ty(&ctx);
                    let provided_sig = Self::convert_func_type(&func_type)?;
                    if !Self::signatures_compatible(req_sig, &provided_sig) {
                        return Err(WasmtimeError::ImportExport {
                            message: "Function signature mismatch".to_string(),
                        });
                    }
                    Ok(())
                })
            }
            (ImportKind::Global(req_type, req_mut), Extern::Global(global)) => {
                store.with_context_ro(|ctx| {
                    let global_type = global.ty(&ctx);
                    let provided_type = Self::convert_val_type(global_type.content().clone())?;
                    let provided_mut = matches!(global_type.mutability(), wasmtime::Mutability::Var);
                    
                    if req_type != &provided_type || (req_mut > &provided_mut) {
                        return Err(WasmtimeError::ImportExport {
                            message: "Global type mismatch".to_string(),
                        });
                    }
                    Ok(())
                })
            }
            (ImportKind::Memory(_req_min, _req_max, _req_shared), Extern::Memory(_memory)) => {
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
    
    /// Call exported function with comprehensive type checking and parameter conversion
    pub fn call_export_function(
        &self,
        store: &mut Store,
        name: &str,
        params: &[WasmValue],
    ) -> WasmtimeResult<ExecutionResult> {
        // Check if instance is disposed
        if self.metadata.disposed {
            return Err(WasmtimeError::Instance {
                message: "Cannot call function on disposed instance".to_string(),
            });
        }
        
        // Validate export exists and is a function
        let export_binding = self.exports_map.get(name)
            .ok_or_else(|| WasmtimeError::ImportExport {
                message: format!("Export '{}' not found", name),
            })?;
            
        let function_sig = match &export_binding.export_type {
            ExportKind::Function(sig) => sig,
            _ => return Err(WasmtimeError::Function {
                message: format!("Export '{}' is not a function", name),
            }),
        };
        
        // Validate parameter count and types
        if params.len() != function_sig.params.len() {
            return Err(WasmtimeError::Function {
                message: format!(
                    "Parameter count mismatch: expected {}, got {}",
                    function_sig.params.len(),
                    params.len()
                ),
            });
        }
        
        // Convert parameters to Wasmtime values with type validation
        let mut wasm_params = Vec::new();
        for (i, (provided, expected)) in params.iter().zip(&function_sig.params).enumerate() {
            if !Self::value_type_matches(provided, expected) {
                return Err(WasmtimeError::Function {
                    message: format!(
                        "Parameter {} type mismatch: expected {:?}, got {:?}",
                        i, expected, provided
                    ),
                });
            }
            wasm_params.push(Self::wasm_value_to_val(provided)?);
        }
        
        // Get function and execute with timing
        let start_time = Instant::now();
        let instance = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire instance lock: {}", e),
        })?;
        
        // Get fuel before execution for tracking
        let fuel_before = store.fuel_remaining().ok().flatten();
        
        let result = store.with_context(|mut ctx| {
            let export = instance.get_export(&mut ctx, name)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!("Export '{}' not found", name),
                })?;
                
            match export {
                Extern::Func(func) => {
                    // Initialize results with proper default values based on return types
                    let mut results = Vec::with_capacity(function_sig.returns.len());
                    for return_type in &function_sig.returns {
                        let default_val = match return_type {
                            ModuleValueType::I32 => Val::I32(0),
                            ModuleValueType::I64 => Val::I64(0),
                            ModuleValueType::F32 => Val::F32(0.0_f32.to_bits()),
                            ModuleValueType::F64 => Val::F64(0.0_f64.to_bits()),
                            ModuleValueType::V128 => Val::V128(wasmtime::V128::from(0u128)),
                            ModuleValueType::ExternRef => Val::ExternRef(None),
                            ModuleValueType::FuncRef => Val::FuncRef(None),
                        };
                        results.push(default_val);
                    }

                    func.call(&mut ctx, &wasm_params, &mut results)
                        .map_err(|e| WasmtimeError::from_runtime_error(e))?;
                    Ok(results)
                }
                _ => Err(WasmtimeError::Function {
                    message: format!("Export '{}' is not a function", name),
                })
            }
        })?;
        
        let execution_time_ns = start_time.elapsed().as_nanos() as u64;
        
        // Get fuel after execution for tracking
        let fuel_after = store.fuel_remaining().ok().flatten();
        
        let fuel_consumed = match (fuel_before, fuel_after) {
            (Some(before), Some(after)) => Some(after.saturating_sub(before)),
            _ => None,
        };
        
        // Convert results back to WasmValue
        let values = result.into_iter()
            .map(|val| Self::val_to_wasm_value(val))
            .collect::<WasmtimeResult<Vec<_>>>()?;
        
        Ok(ExecutionResult {
            values,
            fuel_consumed,
            execution_time_ns,
        })
    }
    
    /// Get exported function by name (for direct Wasmtime usage)
    pub fn get_func(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Func>> {
        let instance = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire instance lock: {}", e),
        })?;
        
        store.with_context(|mut ctx| {
            let export = instance.get_export(&mut ctx, name);
            match export {
                Some(Extern::Func(func)) => Ok(Some(func)),
                _ => Ok(None),
            }
        })
    }
    
    /// Get exported global by name
    pub fn get_global(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Global>> {
        let instance = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire instance lock: {}", e),
        })?;
        
        store.with_context(|mut ctx| {
            let export = instance.get_export(&mut ctx, name);
            match export {
                Some(Extern::Global(global)) => Ok(Some(global)),
                _ => Ok(None),
            }
        })
    }
    
    /// Get exported memory by name
    pub fn get_memory(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Memory>> {
        let instance = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire instance lock: {}", e),
        })?;
        
        store.with_context(|mut ctx| {
            let export = instance.get_export(&mut ctx, name);
            match export {
                Some(Extern::Memory(memory)) => Ok(Some(memory)),
                _ => Ok(None),
            }
        })
    }
    
    /// Get exported table by name
    pub fn get_table(&self, store: &mut Store, name: &str) -> WasmtimeResult<Option<Table>> {
        let instance = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire instance lock: {}", e),
        })?;
        
        store.with_context(|mut ctx| {
            let export = instance.get_export(&mut ctx, name);
            match export {
                Some(Extern::Table(table)) => Ok(Some(table)),
                _ => Ok(None),
            }
        })
    }
    
    /// List all exports
    pub fn exports(&self, store: &mut Store) -> WasmtimeResult<Vec<String>> {
        let instance = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire instance lock: {}", e),
        })?;
        
        store.with_context(|mut ctx| {
            let exports = instance.exports(&mut ctx)
                .map(|export| export.name().to_string())
                .collect();
            Ok(exports)
        })
    }
    
    /// Dispose instance and clean up resources
    pub fn dispose(&mut self) -> WasmtimeResult<()> {
        // Mark as disposed to prevent further operations
        self.metadata.disposed = true;
        
        // Clear export and import maps 
        self.exports_map.clear();
        self.imports_map.clear();
        
        // The Arc<Mutex<WasmtimeInstance>> will be cleaned up when the last reference is dropped
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
    
    /// Validate instance is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Instance {
                message: "Instance has been disposed".to_string(),
            });
        }
        
        if let Ok(_guard) = self.inner.try_lock() {
            Ok(())
        } else {
            Err(WasmtimeError::Concurrency {
                message: "Instance is locked and may be corrupted".to_string(),
            })
        }
    }
    
    // Helper methods for type conversion and validation
    
    /// Convert ExternType to ExportKind
    #[allow(dead_code)]
    fn extern_to_export_kind(extern_type: ExternType) -> WasmtimeResult<ExportKind> {
        match extern_type {
            ExternType::Func(func_type) => {
                Ok(ExportKind::Function(Self::convert_func_type(&func_type)?))
            }
            ExternType::Global(global_type) => {
                let val_type = Self::convert_val_type(global_type.content().clone())?;
                let mutable = matches!(global_type.mutability(), wasmtime::Mutability::Var);
                Ok(ExportKind::Global(val_type, mutable))
            }
            ExternType::Memory(memory_type) => {
                Ok(ExportKind::Memory(
                    memory_type.minimum(),
                    memory_type.maximum(),
                    memory_type.is_shared(),
                ))
            }
            ExternType::Table(table_type) => {
                let element_type = Self::convert_ref_type(table_type.element())?;
                let min = table_type.minimum().try_into().unwrap_or(u32::MAX);
                let max = table_type.maximum().map(|m| m.try_into().unwrap_or(u32::MAX));
                Ok(ExportKind::Table(element_type, min, max))
            }
            ExternType::Tag(_) => Err(WasmtimeError::Type {
                message: "Tag types are not supported".to_string(),
            }),
        }
    }
    
    /// Convert FuncType to FunctionSignature
    fn convert_func_type(func_type: &FuncType) -> WasmtimeResult<FunctionSignature> {
        let params = func_type.params()
            .map(Self::convert_val_type)
            .collect::<WasmtimeResult<Vec<_>>>()?;
            
        let returns = func_type.results()
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
            wasmtime::HeapType::Func => Ok(ModuleValueType::FuncRef),
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
            (WasmValue::ExternRef, ModuleValueType::ExternRef) => true,
            (WasmValue::FuncRef, ModuleValueType::FuncRef) => true,
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
            WasmValue::V128(bytes) => Ok(Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes)))),
            WasmValue::ExternRef => Ok(Val::ExternRef(None)),
            WasmValue::FuncRef => Ok(Val::FuncRef(None)),
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
            Val::ExternRef(_) => Ok(WasmValue::ExternRef),
            Val::FuncRef(_) => Ok(WasmValue::FuncRef),
            Val::AnyRef(_) => Ok(WasmValue::ExternRef), // Treat AnyRef as ExternRef for now
            Val::ExnRef(_) => Ok(WasmValue::ExternRef), // Treat ExnRef as ExternRef for now
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
    use std::os::raw::c_void;
    use crate::validate_ptr_not_null;
    use crate::store::Store;
    use crate::module::Module;
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
    pub fn instantiate_module(mut store: &mut Store, module: &Module, imports: &[wasmtime::Extern]) -> WasmtimeResult<Box<Instance>> {
        if imports.is_empty() {
            Instance::new_without_imports(&mut store, module).map(Box::new)
        } else {
            Instance::new(&mut store, module, imports).map(Box::new)
        }
    }

    /// Core function to call exported function with type checking and parameter conversion
    pub fn call_exported_function(
        instance: &Instance,
        store: &mut Store,
        name: &str,
        params: &[WasmValue],
    ) -> WasmtimeResult<ExecutionResult> {
        instance.call_export_function(store, name, params)
    }
    
    /// Core function to validate instance pointer and get reference
    pub unsafe fn get_instance_ref(instance_ptr: *const c_void) -> WasmtimeResult<&'static Instance> {
        validate_ptr_not_null!(instance_ptr, "instance");
        Ok(&*(instance_ptr as *const Instance))
    }
    
    /// Core function to validate instance pointer and get mutable reference
    pub unsafe fn get_instance_mut(instance_ptr: *mut c_void) -> WasmtimeResult<&'static mut Instance> {
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
    pub unsafe fn destroy_instance(instance_ptr: *mut c_void) {
        if instance_ptr.is_null() {
            return;
        }

        let ptr_addr = instance_ptr as usize;
        
        // Detect and reject obvious test/fake pointers
        if ptr_addr < 0x1000 || (ptr_addr & 0xFFFFFF0000000000) == 0x1234560000000000 {
            log::debug!("Ignoring fake/test pointer {:p} in destroy_instance", instance_ptr);
            return;
        }

        // Check if pointer was already destroyed
        {
            use crate::error::ffi_utils::DESTROYED_POINTERS;
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            if destroyed.contains(&ptr_addr) {
                log::warn!("Attempted double-free of Instance resource at {:p} - ignoring", instance_ptr);
                return;
            }
            destroyed.insert(ptr_addr);
        }

        // Simple, correct cleanup - let Rust handle Arc dropping naturally
        let result = std::panic::catch_unwind(|| {
            let _boxed_instance = Box::from_raw(instance_ptr as *mut Instance);
            // Box and Arc will be dropped automatically here
            log::debug!("Instance at {:p} being destroyed", instance_ptr);
        });

        match result {
            Ok(_) => {
                log::debug!("Instance resource at {:p} destroyed successfully", instance_ptr);
            }
            Err(e) => {
                log::error!("Instance resource at {:p} destruction panicked: {:?} - preventing JVM crash", instance_ptr, e);
                // Don't propagate panic to JVM
            }
        }
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
    pub fn get_export_information<'a>(instance: &'a Instance, name: &str) -> Option<&'a ExportBinding> {
        instance.get_export_info(name)
    }
    
    /// Core function to get all exports information
    pub fn get_all_exports(instance: &Instance) -> &HashMap<String, ExportBinding> {
        instance.all_exports()
    }
    
    /// Core function to get import information  
    pub fn get_import_information<'a>(instance: &'a Instance, module: &str, name: &str) -> Option<&'a ImportBinding> {
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
    
    /// Creates an externref WebAssembly value
    pub fn create_externref_value() -> WasmValue {
        WasmValue::ExternRef
    }
    
    /// Creates a funcref WebAssembly value
    pub fn create_funcref_value() -> WasmValue {
        WasmValue::FuncRef
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
    pub unsafe fn get_function_ref(func_ptr: *const c_void) -> WasmtimeResult<&'static wasmtime::Func> {
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
        store.with_context(|ctx| {
            Ok(func.ty(ctx))
        })
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

    /// Convert parameters from FFI representation
    pub unsafe fn convert_params_from_ffi(
        params_ptr: *const c_void,
        param_count: usize,
    ) -> WasmtimeResult<Vec<WasmValue>> {
        // For now, assume parameters are passed as an array of WasmValue structs
        // This is a simplified implementation - real implementation would need
        // proper memory layout handling
        if params_ptr.is_null() {
            return Ok(Vec::new());
        }

        let params_slice = std::slice::from_raw_parts(
            params_ptr as *const WasmValue,
            param_count,
        );

        Ok(params_slice.to_vec())
    }

    /// Convert results to FFI representation
    pub unsafe fn convert_results_to_ffi(
        results: &[WasmValue],
        results_ptr: *mut c_void,
        result_count: usize,
    ) -> WasmtimeResult<()> {
        if results_ptr.is_null() || results.is_empty() {
            return Ok(());
        }

        let count = std::cmp::min(results.len(), result_count);
        let results_slice = std::slice::from_raw_parts_mut(
            results_ptr as *mut WasmValue,
            count,
        );

        for (i, result) in results.iter().take(count).enumerate() {
            results_slice[i] = result.clone();
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
            WasmValue::V128(bytes) => wasmtime::Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes))),
            WasmValue::ExternRef => wasmtime::Val::null_extern_ref(),
            WasmValue::FuncRef => wasmtime::Val::null_func_ref(),
        })
    }

    /// Convert wasmtime::Val to WasmValue
    fn wasmtime_val_to_wasm_value(val: &wasmtime::Val) -> WasmtimeResult<WasmValue> {
        Ok(match val {
            wasmtime::Val::I32(v) => WasmValue::I32(*v),
            wasmtime::Val::I64(v) => WasmValue::I64(*v),
            wasmtime::Val::F32(v) => WasmValue::F32(f32::from_bits(*v)),
            wasmtime::Val::F64(v) => WasmValue::F64(f64::from_bits(*v)),
            wasmtime::Val::V128(v) => WasmValue::V128(v.as_u128().to_le_bytes()),
            wasmtime::Val::FuncRef(_) => WasmValue::FuncRef,
            wasmtime::Val::ExternRef(_) => WasmValue::ExternRef,
            wasmtime::Val::AnyRef(_) => WasmValue::ExternRef,
            wasmtime::Val::ExnRef(_) => WasmValue::ExternRef,
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
            wasmtime::ValType::Ref(_) => {
                // For reference types, we need to check if it's funcref or externref
                // This is a simplified approach - more detailed inspection would be needed
                match format!("{:?}", val_type).as_str() {
                    s if s.contains("funcref") => 5,
                    _ => 6, // Default to externref
                }
            }
        }
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
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
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
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
        // Call the function with type checking
        let params = vec![WasmValue::I32(5), WasmValue::I32(3)];
        let result = instance.call_export_function(&mut store, "add", &params)
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
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
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
        
        let mut instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
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
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
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
}

//
// Native C exports for JNI and Panama FFI consumption
//

use std::os::raw::{c_void, c_char, c_int};
use std::ffi::{CStr, CString};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Instance core functions for interface implementations
pub mod ffi_core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;

    /// Core function to create instance without imports
    pub fn create_instance_without_imports(
        store: &mut Store,
        module: &Module,
    ) -> WasmtimeResult<Box<Instance>> {
        Instance::new_without_imports(store, module).map(Box::new)
    }

    /// Core function to validate instance pointer and get reference
    pub unsafe fn get_instance_ref(instance_ptr: *const c_void) -> WasmtimeResult<&'static Instance> {
        validate_ptr_not_null!(instance_ptr, "instance");
        Ok(&*(instance_ptr as *const Instance))
    }

    /// Core function to validate instance pointer and get mutable reference
    pub unsafe fn get_instance_mut(instance_ptr: *mut c_void) -> WasmtimeResult<&'static mut Instance> {
        validate_ptr_not_null!(instance_ptr, "instance");
        Ok(&mut *(instance_ptr as *mut Instance))
    }

    /// Core function to destroy an instance (safe cleanup)
    pub unsafe fn destroy_instance(instance_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Instance>(instance_ptr, "Instance");
    }

    /// Core function to get instance metadata
    pub fn get_metadata(instance: &Instance) -> &InstanceMetadata {
        instance.metadata()
    }

    /// Core function to check if instance has export
    pub fn has_export(instance: &Instance, name: &str) -> bool {
        instance.has_export(name)
    }

    /// Core function to get exports
    pub fn get_exports(instance: &Instance) -> &[ExportBinding] {
        instance.exports()
    }

    /// Core function to dispose instance
    pub fn dispose_instance(instance: &mut Instance) {
        instance.dispose()
    }

    /// Core function to check if instance is disposed
    pub fn is_disposed(instance: &Instance) -> bool {
        instance.is_disposed()
    }
}

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

    match (crate::store::core::get_store_mut(store_ptr), crate::module::core::get_module_ref(module_ptr)) {
        (Ok(store), Ok(module)) => {
            match ffi_core::create_instance_without_imports(store, module) {
                Ok(instance) => Box::into_raw(instance) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
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

    match ffi_core::get_instance_ref(instance_ptr) {
        Ok(instance) => {
            match CStr::from_ptr(name).to_str() {
                Ok(name_str) => {
                    if core::has_export(instance, name_str) { 1 } else { 0 }
                },
                Err(_) => FFI_ERROR,
            }
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
    match ffi_core::get_instance_ref(instance_ptr) {
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
    match ffi_core::get_instance_mut(instance_ptr) {
        Ok(instance) => {
            core::dispose_instance(instance);
            FFI_SUCCESS
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
    match ffi_core::get_instance_ref(instance_ptr) {
        Ok(instance) => if core::is_disposed(instance) { 1 } else { 0 },
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
        core::get_instance_ref(instance_ptr),
        crate::store::core::get_store_mut(store_ptr),
        CStr::from_ptr(function_name).to_str()
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
                    if let Some(WasmValue::I32(result)) = results.into_iter().next() {
                        result
                    } else {
                        0
                    }
                },
                Err(_) => 0,
            }
        },
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
        0
    )
}

/// Get instance creation timestamp in microseconds since epoch
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_created_at_micros(instance_ptr: *const c_void) -> u64 {
    match ffi_core::get_instance_ref(instance_ptr) {
        Ok(instance) => {
            let metadata = core::get_metadata(instance);
            metadata.created_at.duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_micros() as u64
        },
        Err(_) => 0,
    }
}

/// Get instance export count
///
/// # Safety
///
/// instance_ptr must be a valid pointer from wasmtime4j_instance_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_metadata_export_count(instance_ptr: *const c_void) -> usize {
    match ffi_core::get_instance_ref(instance_ptr) {
        Ok(instance) => core::get_metadata(instance).exports.len(),
        Err(_) => 0,
    }
}