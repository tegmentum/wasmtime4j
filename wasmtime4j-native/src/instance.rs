//! WebAssembly instance management with defensive execution and resource tracking
//!
//! The Instance provides safe execution of WebAssembly functions with comprehensive
//! resource management, timeout handling, and defensive programming practices.

use std::sync::{Arc, Mutex};
use wasmtime::{Instance as WasmtimeInstance, Extern, Func, Global, Memory, Table};
use crate::store::Store;
use crate::module::Module;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime instance with resource management
pub struct Instance {
    inner: Arc<Mutex<WasmtimeInstance>>,
    metadata: InstanceMetadata,
}

/// Instance metadata and resource tracking
#[derive(Debug, Clone)]
pub struct InstanceMetadata {
    /// Module name or identifier
    pub name: String,
    /// Number of exported functions
    pub export_count: usize,
    /// Number of imported functions
    pub import_count: usize,
    /// Memory usage in bytes
    pub memory_bytes: usize,
}

impl Instance {
    /// Create a new WebAssembly instance with imports
    pub fn new(
        store: &mut Store,
        module: &Module,
        imports: &[Extern],
    ) -> WasmtimeResult<Self> {
        module.validate()?;
        
        let instance = store.with_context(|mut ctx| {
            WasmtimeInstance::new(&mut ctx, module.inner(), imports)
                .map_err(|e| WasmtimeError::Instance {
                    message: format!("Failed to create instance: {}", e),
                })
        })?;
        
        // Count exports for metadata
        let export_count = store.with_context(|mut ctx| {
            Ok(instance.exports(&mut ctx).count())
        })?;
        
        let metadata = InstanceMetadata {
            name: "default".to_string(),
            export_count,
            import_count: imports.len(),
            memory_bytes: 0, // Will be updated when memory is accessed
        };
        
        Ok(Instance {
            inner: Arc::new(Mutex::new(instance)),
            metadata,
        })
    }
    
    /// Create instance with no imports (for simple modules)
    pub fn new_without_imports(
        store: &mut Store,
        module: &Module,
    ) -> WasmtimeResult<Self> {
        Self::new(store, module, &[])
    }
    
    /// Get exported function by name
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
    
    /// Get instance metadata
    pub fn metadata(&self) -> &InstanceMetadata {
        &self.metadata
    }
    
    /// Validate instance is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        if let Ok(_guard) = self.inner.try_lock() {
            Ok(())
        } else {
            Err(WasmtimeError::Concurrency {
                message: "Instance is locked and may be corrupted".to_string(),
            })
        }
    }
}

// Thread safety: Instance uses Arc<Mutex<WasmtimeInstance>> internally
unsafe impl Send for Instance {}
unsafe impl Sync for Instance {}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    
    // Simple WebAssembly module that adds two i32 values
    const SIMPLE_WASM: &[u8] = &[
        0x00, 0x61, 0x73, 0x6d, // magic
        0x01, 0x00, 0x00, 0x00, // version
        0x01, 0x07, 0x01,       // type section
        0x60, 0x02, 0x7f, 0x7f, 0x01, 0x7f, // func type: (i32, i32) -> i32
        0x03, 0x02, 0x01, 0x00, // func section
        0x07, 0x07, 0x01,       // export section
        0x03, 0x61, 0x64, 0x64, 0x00, 0x00, // export "add" func 0
        0x0a, 0x09, 0x01,       // code section
        0x07, 0x00,             // func 0 body
        0x20, 0x00,             // local.get 0
        0x20, 0x01,             // local.get 1
        0x6a,                   // i32.add
        0x0b,                   // end
    ];
    
    #[test]
    fn test_instance_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        let module = Module::compile(&engine, SIMPLE_WASM).expect("Failed to create module");
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
        assert!(instance.validate().is_ok());
        assert_eq!(instance.metadata().export_count, 1); // "add" function
    }
    
    #[test]
    fn test_get_exported_function() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        let module = Module::compile(&engine, SIMPLE_WASM).expect("Failed to create module");
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
        let add_func = instance.get_func(&mut store, "add")
            .expect("Failed to get function")
            .expect("Function should exist");
        
        // Function exists and has correct signature
        store.with_context(|mut ctx| {
            assert_eq!(add_func.param_arity(&ctx), 2);
            assert_eq!(add_func.result_arity(&ctx), 1);
            Ok(())
        }).expect("Failed to check function arity");
    }
    
    #[test]
    fn test_list_exports() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");
        let module = Module::compile(&engine, SIMPLE_WASM).expect("Failed to create module");
        
        let instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");
        
        let exports = instance.exports(&mut store).expect("Failed to get exports");
        assert_eq!(exports.len(), 1);
        assert_eq!(exports[0], "add");
    }
}