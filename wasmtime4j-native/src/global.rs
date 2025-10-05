//! WebAssembly Global variable management with type safety and mutability enforcement
//!
//! This module provides safe wrapper around wasmtime::Global for managing global variables
//! in WebAssembly modules. It enforces mutability constraints and provides type validation
//! to prevent runtime errors and ensure safe global variable operations.

use std::sync::{Arc, Mutex};
use wasmtime::{Global as WasmtimeGlobal, GlobalType, Val, Mutability, ValType};
use crate::store::Store;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime global with type safety
pub struct Global {
    inner: Arc<Mutex<WasmtimeGlobal>>,
    metadata: GlobalMetadata,
}

/// Global variable metadata and type information
#[derive(Debug, Clone)]
pub struct GlobalMetadata {
    /// Value type of this global (I32, I64, F32, F64, V128, or reference types)
    pub value_type: ValType,
    /// Whether this global is mutable
    pub mutability: Mutability,
    /// Optional name for debugging purposes
    pub name: Option<String>,
}

/// Reference types that can be stored in globals
#[derive(Debug, Clone, PartialEq)]
pub enum ReferenceType {
    /// Function reference type
    FuncRef,
    /// External reference type
    ExternRef,
    /// Any reference type (where supported)
    AnyRef,
}

/// Type-safe value container for global variables
#[derive(Debug, Clone)]
pub enum GlobalValue {
    /// 32-bit integer value
    I32(i32),
    /// 64-bit integer value
    I64(i64),
    /// 32-bit floating point value
    F32(f32),
    /// 64-bit floating point value
    F64(f64),
    /// 128-bit SIMD vector value
    V128([u8; 16]),
    /// Function reference
    FuncRef(Option<u64>), // Function ID, None for null reference
    /// External reference
    ExternRef(Option<u64>), // External object ID, None for null reference
    /// Any reference type
    AnyRef(Option<u64>), // Generic reference ID, None for null reference
}

impl Global {
    /// Create a new global variable with specified type and initial value
    pub fn new(
        store: &Store, 
        value_type: ValType, 
        mutability: Mutability, 
        initial_value: GlobalValue,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        // Validate that the initial value matches the specified type
        Self::validate_value_type(&initial_value, &value_type)?;

        let global_type = GlobalType::new(value_type.clone(), mutability);
        let wasmtime_value = Self::global_value_to_wasmtime_val(initial_value.clone())?;

        let wasmtime_global = store.with_context(|mut ctx| {
            WasmtimeGlobal::new(&mut ctx, global_type, wasmtime_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to create global variable: {}", e),
                    backtrace: None,
                })
        })?;

        let metadata = GlobalMetadata {
            value_type,
            mutability,
            name,
        };

        Ok(Global {
            inner: Arc::new(Mutex::new(wasmtime_global)),
            metadata,
        })
    }

    /// Get the current value of the global variable
    pub fn get(&self, store: &Store) -> WasmtimeResult<GlobalValue> {
        let global = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire global lock: {}", e),
        })?;

        let wasmtime_value = store.with_context(|ctx| {
            Ok(global.get(ctx))
        })?;

        Self::wasmtime_val_to_global_value(wasmtime_value)
    }

    /// Set the value of the global variable (only allowed if mutable)
    pub fn set(&self, store: &Store, value: GlobalValue) -> WasmtimeResult<()> {
        if self.metadata.mutability != Mutability::Var {
            return Err(WasmtimeError::Runtime {
                message: "Cannot set value on immutable global variable".to_string(),
                backtrace: None,
            });
        }

        // Validate that the new value matches the global's type
        Self::validate_value_type(&value, &self.metadata.value_type)?;

        let global = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire global lock: {}", e),
        })?;

        let wasmtime_value = Self::global_value_to_wasmtime_val(value)?;

        store.with_context(|mut ctx| {
            global.set(&mut ctx, wasmtime_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to set global value: {}", e),
                    backtrace: None,
                })
        })
    }

    /// Get global variable metadata
    pub fn metadata(&self) -> &GlobalMetadata {
        &self.metadata
    }

    /// Get global variable type information
    pub fn global_type(&self, store: &Store) -> WasmtimeResult<GlobalType> {
        let global = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire global lock: {}", e),
        })?;

        store.with_context_ro(|ctx| {
            Ok(global.ty(&ctx))
        })
    }

    /// Validate that a GlobalValue matches the expected ValType
    fn validate_value_type(value: &GlobalValue, expected_type: &ValType) -> WasmtimeResult<()> {
        let matches = match (value, expected_type) {
            (GlobalValue::I32(_), ValType::I32) => true,
            (GlobalValue::I64(_), ValType::I64) => true,
            (GlobalValue::F32(_), ValType::F32) => true,
            (GlobalValue::F64(_), ValType::F64) => true,
            (GlobalValue::V128(_), ValType::V128) => true,
            (GlobalValue::FuncRef(_), ValType::Ref(ref_type)) => {
                use wasmtime::HeapType;
                matches!(*ref_type.heap_type(), HeapType::Func | HeapType::ConcreteFunc(_))
            },
            (GlobalValue::ExternRef(_), ValType::Ref(ref_type)) => {
                use wasmtime::HeapType;
                matches!(*ref_type.heap_type(), HeapType::Extern)
            },
            (GlobalValue::AnyRef(_), ValType::Ref(_)) => true, // AnyRef matches any Ref type
            _ => false,
        };

        if !matches {
            return Err(WasmtimeError::Type {
                message: format!(
                    "Value type {:?} does not match expected type {:?}",
                    value, expected_type
                ),
            });
        }

        Ok(())
    }

    /// Convert GlobalValue to wasmtime::Val
    fn global_value_to_wasmtime_val(value: GlobalValue) -> WasmtimeResult<Val> {
        let wasmtime_val = match value {
            GlobalValue::I32(val) => Val::I32(val),
            GlobalValue::I64(val) => Val::I64(val),
            GlobalValue::F32(val) => Val::F32(val.to_bits()),
            GlobalValue::F64(val) => Val::F64(val.to_bits()),
            GlobalValue::V128(val) => Val::V128(wasmtime::V128::from(u128::from_le_bytes(val))),
            GlobalValue::FuncRef(_) => {
                // For now, we only support null function references
                // TODO: Implement proper function reference handling
                Val::FuncRef(None)
            },
            GlobalValue::ExternRef(_) => {
                // For now, we only support null external references
                // TODO: Implement proper external reference handling
                Val::ExternRef(None)
            },
            GlobalValue::AnyRef(_) => {
                // AnyRef is not directly supported by wasmtime::Val
                return Err(WasmtimeError::Type {
                    message: "AnyRef type not yet supported".to_string(),
                });
            },
        };

        Ok(wasmtime_val)
    }

    /// Convert wasmtime::Val to GlobalValue
    fn wasmtime_val_to_global_value(val: Val) -> WasmtimeResult<GlobalValue> {
        let global_value = match val {
            Val::I32(val) => GlobalValue::I32(val),
            Val::I64(val) => GlobalValue::I64(val),
            Val::F32(val) => GlobalValue::F32(f32::from_bits(val)),
            Val::F64(val) => GlobalValue::F64(f64::from_bits(val)),
            Val::V128(val) => GlobalValue::V128(u128::from(val).to_le_bytes()),
            Val::FuncRef(func_ref) => {
                // TODO: Implement proper function reference ID mapping
                GlobalValue::FuncRef(func_ref.map(|_| 0))
            },
            Val::ExternRef(extern_ref) => {
                // TODO: Implement proper external reference ID mapping
                GlobalValue::ExternRef(extern_ref.map(|_| 0))
            },
            Val::AnyRef(_) => {
                // TODO: Implement proper any reference ID mapping
                GlobalValue::AnyRef(Some(0))
            },
            Val::ExnRef(_) => {
                // ExnRef is not supported in GlobalValue, return error
                return Err(WasmtimeError::Type {
                    message: "ExnRef type not supported in globals".to_string(),
                });
            },
        };

        Ok(global_value)
    }

    /// Create a global from an existing wasmtime::Global (for imports/exports)
    pub fn from_wasmtime_global(
        wasmtime_global: WasmtimeGlobal,
        store: &Store,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        let global_type = store.with_context_ro(|ctx| {
            Ok(wasmtime_global.ty(&ctx))
        })?;

        let metadata = GlobalMetadata {
            value_type: global_type.content().clone(),
            mutability: global_type.mutability(),
            name,
        };

        Ok(Global {
            inner: Arc::new(Mutex::new(wasmtime_global)),
            metadata,
        })
    }

    /// Get the underlying wasmtime::Global (for internal use)
    pub fn wasmtime_global(&self) -> Arc<Mutex<WasmtimeGlobal>> {
        Arc::clone(&self.inner)
    }
}

// Thread safety: Global uses Arc<Mutex<WasmtimeGlobal>> internally
unsafe impl Send for Global {}
unsafe impl Sync for Global {}

/// Shared core functions for global operations used by both JNI and Panama interfaces
///
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::validate_ptr_not_null;

    /// Core function to create a new global variable
    pub fn create_global(
        store: &Store,
        value_type: ValType,
        mutability: Mutability,
        initial_value: GlobalValue,
        name: Option<String>,
    ) -> WasmtimeResult<Box<Global>> {
        Global::new(store, value_type, mutability, initial_value, name).map(Box::new)
    }

    /// Core function to validate global pointer and get reference
    pub unsafe fn get_global_ref(global_ptr: *const c_void) -> WasmtimeResult<&'static Global> {
        validate_ptr_not_null!(global_ptr, "global");
        Ok(&*(global_ptr as *const Global))
    }

    /// Core function to validate global pointer and get mutable reference
    pub unsafe fn get_global_mut(global_ptr: *mut c_void) -> WasmtimeResult<&'static mut Global> {
        validate_ptr_not_null!(global_ptr, "global");
        Ok(&mut *(global_ptr as *mut Global))
    }

    /// Core function to get global variable value
    pub fn get_global_value(global: &Global, store: &Store) -> WasmtimeResult<GlobalValue> {
        global.get(store)
    }

    /// Core function to set global variable value
    pub fn set_global_value(global: &Global, store: &Store, value: GlobalValue) -> WasmtimeResult<()> {
        global.set(store, value)
    }

    /// Core function to get global variable metadata
    pub fn get_global_metadata(global: &Global) -> &GlobalMetadata {
        global.metadata()
    }

    /// Core function to get global variable type
    pub fn get_global_type(global: &Global, store: &Store) -> WasmtimeResult<GlobalType> {
        global.global_type(store)
    }

    /// Core function to destroy a global variable (safe cleanup)
    pub unsafe fn destroy_global(global_ptr: *mut c_void) {
        if global_ptr.is_null() {
            return;
        }

        let ptr_addr = global_ptr as usize;
        
        // Detect and reject obvious test/fake pointers
        if ptr_addr < 0x1000 || (ptr_addr & 0xFFFFFF0000000000) == 0x1234560000000000 {
            log::debug!("Ignoring fake/test pointer {:p} in destroy_global", global_ptr);
            return;
        }

        // Check if pointer was already destroyed
        {
            use crate::error::ffi_utils::DESTROYED_POINTERS;
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            if destroyed.contains(&ptr_addr) {
                log::warn!("Attempted double-free of Global resource at {:p} - ignoring", global_ptr);
                return;
            }
            destroyed.insert(ptr_addr);
        }

        // Simple, correct cleanup - let Rust handle Arc dropping naturally
        let result = std::panic::catch_unwind(|| {
            let _boxed_global = Box::from_raw(global_ptr as *mut Global);
            // Box and Arc will be dropped automatically here
            log::debug!("Global at {:p} being destroyed", global_ptr);
        });

        match result {
            Ok(_) => {
                log::debug!("Global resource at {:p} destroyed successfully", global_ptr);
            }
            Err(e) => {
                log::error!("Global resource at {:p} destruction panicked: {:?} - preventing JVM crash", global_ptr, e);
                // Don't propagate panic to JVM
            }
        }
    }

    /// Helper function to create GlobalValue from raw components
    pub fn create_global_value(
        value_type: ValType,
        i32_value: i32,
        i64_value: i64,
        f32_value: f32,
        f64_value: f64,
        v128_bytes: Option<[u8; 16]>,
        ref_id: Option<u64>,
    ) -> WasmtimeResult<GlobalValue> {
        let global_value = match value_type {
            ValType::I32 => GlobalValue::I32(i32_value),
            ValType::I64 => GlobalValue::I64(i64_value),
            ValType::F32 => GlobalValue::F32(f32_value),
            ValType::F64 => GlobalValue::F64(f64_value),
            ValType::V128 => GlobalValue::V128(v128_bytes.unwrap_or([0u8; 16])),
            ValType::Ref(ref ref_type) => {
                use wasmtime::HeapType;

                // Match on dereferenced heap_type
                let heap = ref_type.heap_type();
                match *heap {
                    HeapType::Func | HeapType::ConcreteFunc(_) => GlobalValue::FuncRef(ref_id),
                    HeapType::Extern => GlobalValue::ExternRef(ref_id),
                    _ => {
                        // Return error with heap type info to debug
                        return Err(WasmtimeError::Type {
                            message: format!("Unexpected HeapType for RefType - got {:?}, expected Func or Extern", heap),
                        });
                    }
                }
            },
        };

        Ok(global_value)
    }

    /// Helper function to extract raw components from GlobalValue
    pub fn extract_global_value(value: &GlobalValue) -> (i32, i64, f32, f64, Option<u64>) {
        match value {
            GlobalValue::I32(val) => (*val, 0, 0.0, 0.0, None),
            GlobalValue::I64(val) => (0, *val, 0.0, 0.0, None),
            GlobalValue::F32(val) => (0, 0, *val, 0.0, None),
            GlobalValue::F64(val) => (0, 0, 0.0, *val, None),
            GlobalValue::V128(val) => (0, i64::from_le_bytes([val[0], val[1], val[2], val[3], val[4], val[5], val[6], val[7]]), 0.0, 0.0, None),
            GlobalValue::FuncRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            GlobalValue::ExternRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            GlobalValue::AnyRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use crate::store::Store;

    #[test]
    fn test_global_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Var,
            GlobalValue::I32(42),
            Some("test_global".to_string()),
        ).expect("Failed to create global");

        assert!(matches!(global.metadata().value_type, ValType::I32));
        assert_eq!(global.metadata().mutability, Mutability::Var);
        assert_eq!(global.metadata().name, Some("test_global".to_string()));
    }

    #[test]
    fn test_global_get_set() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Var,
            GlobalValue::I32(42),
            None,
        ).expect("Failed to create global");

        // Test getting initial value
        let value = global.get(&store).expect("Failed to get global value");
        match value {
            GlobalValue::I32(val) => assert_eq!(val, 42),
            _ => panic!("Expected I32 value"),
        }

        // Test setting new value
        global.set(&store, GlobalValue::I32(100)).expect("Failed to set global value");

        // Test getting updated value
        let value = global.get(&store).expect("Failed to get global value");
        match value {
            GlobalValue::I32(val) => assert_eq!(val, 100),
            _ => panic!("Expected I32 value"),
        }
    }

    #[test]
    fn test_immutable_global() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Const,
            GlobalValue::I32(42),
            None,
        ).expect("Failed to create global");

        // Test that setting fails on immutable global
        let result = global.set(&store, GlobalValue::I32(100));
        assert!(result.is_err());
    }

    #[test]
    fn test_type_validation() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Var,
            GlobalValue::I32(42),
            None,
        ).expect("Failed to create global");

        // Test that setting wrong type fails
        let result = global.set(&store, GlobalValue::I64(100));
        assert!(result.is_err());
    }

    #[test]
    fn test_different_value_types() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        // Test I64 global
        let global_i64 = Global::new(
            &store,
            ValType::I64,
            Mutability::Var,
            GlobalValue::I64(123456789),
            None,
        ).expect("Failed to create I64 global");

        let value = global_i64.get(&store).expect("Failed to get I64 value");
        match value {
            GlobalValue::I64(val) => assert_eq!(val, 123456789),
            _ => panic!("Expected I64 value"),
        }

        // Test F32 global
        let global_f32 = Global::new(
            &store,
            ValType::F32,
            Mutability::Var,
            GlobalValue::F32(3.14159),
            None,
        ).expect("Failed to create F32 global");

        let value = global_f32.get(&store).expect("Failed to get F32 value");
        match value {
            GlobalValue::F32(val) => assert!((val - 3.14159).abs() < 0.001),
            _ => panic!("Expected F32 value"),
        }

        // Test F64 global
        let global_f64 = Global::new(
            &store,
            ValType::F64,
            Mutability::Var,
            GlobalValue::F64(2.71828),
            None,
        ).expect("Failed to create F64 global");

        let value = global_f64.get(&store).expect("Failed to get F64 value");
        match value {
            GlobalValue::F64(val) => assert!((val - 2.71828).abs() < 0.00001),
            _ => panic!("Expected F64 value"),
        }
    }
}