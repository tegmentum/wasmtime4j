//! WebAssembly Global variable management with type safety and mutability enforcement
//!
//! This module provides safe wrapper around wasmtime::Global for managing global variables
//! in WebAssembly modules. It enforces mutability constraints and provides type validation
//! to prevent runtime errors and ensure safe global variable operations.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;
use std::sync::{Arc, Mutex};
use wasmtime::{Global as WasmtimeGlobal, GlobalType, Mutability, Val, ValType};

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
    /// Any reference type (WasmGC)
    AnyRef(Option<u64>), // Generic reference ID, None for null reference
    /// Equality-testable reference (WasmGC)
    EqRef(Option<u64>), // EqRef ID, None for null reference
    /// 31-bit integer reference (WasmGC)
    I31Ref(Option<i32>), // i31 value (31-bit signed), None for null reference
    /// Struct reference (WasmGC) - requires struct type definition
    StructRef(Option<u64>), // Struct instance ID, None for null reference
    /// Array reference (WasmGC) - requires array type definition
    ArrayRef(Option<u64>), // Array instance ID, None for null reference
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
        let wasmtime_value = Self::global_value_to_wasmtime_val(initial_value.clone(), store)?;

        let wasmtime_global = store.with_context(|mut ctx| {
            WasmtimeGlobal::new(&mut ctx, global_type, wasmtime_value).map_err(|e| {
                WasmtimeError::Runtime {
                    message: format!("Failed to create global variable: {}", e),
                    backtrace: None,
                }
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

        let wasmtime_value = store.with_context(|ctx| Ok(global.get(ctx)))?;

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

        let wasmtime_value = Self::global_value_to_wasmtime_val(value, store)?;

        store.with_context(|mut ctx| {
            global
                .set(&mut ctx, wasmtime_value)
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

        store.with_context_ro(|ctx| Ok(global.ty(&ctx)))
    }

    /// Validate that a GlobalValue matches the expected ValType
    fn validate_value_type(value: &GlobalValue, expected_type: &ValType) -> WasmtimeResult<()> {
        use wasmtime::HeapType;

        let matches = match (value, expected_type) {
            (GlobalValue::I32(_), ValType::I32) => true,
            (GlobalValue::I64(_), ValType::I64) => true,
            (GlobalValue::F32(_), ValType::F32) => true,
            (GlobalValue::F64(_), ValType::F64) => true,
            (GlobalValue::V128(_), ValType::V128) => true,
            (GlobalValue::FuncRef(_), ValType::Ref(ref_type)) => {
                matches!(
                    *ref_type.heap_type(),
                    HeapType::Func | HeapType::ConcreteFunc(_) | HeapType::NoFunc
                )
            }
            (GlobalValue::ExternRef(_), ValType::Ref(ref_type)) => {
                matches!(*ref_type.heap_type(), HeapType::Extern | HeapType::NoExtern)
            }
            (GlobalValue::AnyRef(_), ValType::Ref(ref_type)) => {
                matches!(*ref_type.heap_type(), HeapType::Any | HeapType::None)
            }
            (GlobalValue::EqRef(_), ValType::Ref(ref_type)) => {
                matches!(*ref_type.heap_type(), HeapType::Eq)
            }
            (GlobalValue::I31Ref(_), ValType::Ref(ref_type)) => {
                matches!(*ref_type.heap_type(), HeapType::I31)
            }
            (GlobalValue::StructRef(_), ValType::Ref(ref_type)) => {
                matches!(
                    *ref_type.heap_type(),
                    HeapType::Struct | HeapType::ConcreteStruct(_)
                )
            }
            (GlobalValue::ArrayRef(_), ValType::Ref(ref_type)) => {
                matches!(
                    *ref_type.heap_type(),
                    HeapType::Array | HeapType::ConcreteArray(_)
                )
            }
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
    fn global_value_to_wasmtime_val(value: GlobalValue, store: &Store) -> WasmtimeResult<Val> {
        let wasmtime_val = match value {
            GlobalValue::I32(val) => Val::I32(val),
            GlobalValue::I64(val) => Val::I64(val),
            GlobalValue::F32(val) => Val::F32(val.to_bits()),
            GlobalValue::F64(val) => Val::F64(val.to_bits()),
            GlobalValue::V128(val) => Val::V128(wasmtime::V128::from(u128::from_le_bytes(val))),
            GlobalValue::FuncRef(func_id) => {
                if let Some(id) = func_id {
                    // Look up function reference in the table reference registry
                    use crate::table::core::get_function_reference;
                    if let Some(func) = get_function_reference(id)? {
                        Val::FuncRef(Some(func))
                    } else {
                        // Function not found in registry, return null
                        log::warn!("Funcref ID {} not found in registry", id);
                        Val::FuncRef(None)
                    }
                } else {
                    Val::FuncRef(None)
                }
            }
            GlobalValue::ExternRef(extern_id) => {
                if extern_id.is_some() {
                    log::warn!("Non-null ExternRef in global discarded; Store context required");
                }
                Val::ExternRef(None)
            }
            GlobalValue::AnyRef(ref_id) => {
                if ref_id.is_some() {
                    log::warn!("Non-null AnyRef in global discarded; Store context required");
                }
                Val::AnyRef(None)
            }
            GlobalValue::EqRef(_ref_id) => {
                // EqRef supports null values directly
                // Non-null EqRef would require Store context for GC-managed objects
                Val::AnyRef(None) // EqRef is a subtype of AnyRef
            }
            GlobalValue::I31Ref(maybe_value) => {
                // i31ref can hold actual 31-bit integer values
                if let Some(value) = maybe_value {
                    // Create an i31 value and wrap it in AnyRef
                    store.with_context(|mut ctx| {
                        let i31 = wasmtime::I31::wrapping_i32(value);
                        let anyref = wasmtime::AnyRef::from_i31(&mut ctx, i31);
                        Ok(Val::AnyRef(Some(anyref)))
                    })?
                } else {
                    Val::AnyRef(None) // null i31ref
                }
            }
            GlobalValue::StructRef(_ref_id) => {
                // StructRef requires a concrete struct type from a module
                // Only null values can be created without a module instance
                Val::AnyRef(None)
            }
            GlobalValue::ArrayRef(_ref_id) => {
                // ArrayRef requires a concrete array type from a module
                // Only null values can be created without a module instance
                Val::AnyRef(None)
            }
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
                if let Some(func) = func_ref {
                    // Register the function in the table reference registry and get its ID
                    // Use store_id 0 since we don't have store context in global value conversion
                    use crate::table::core::register_function_reference;
                    let id = register_function_reference(func, 0)?;
                    GlobalValue::FuncRef(Some(id))
                } else {
                    GlobalValue::FuncRef(None)
                }
            }
            Val::ExternRef(extern_ref) => {
                if extern_ref.is_some() {
                    log::warn!("Non-null ExternRef value discarded; Store context required");
                }
                GlobalValue::ExternRef(None)
            }
            Val::AnyRef(any_ref) => {
                if any_ref.is_some() {
                    log::warn!("Non-null AnyRef value discarded; Store context required");
                }
                GlobalValue::AnyRef(None)
            }
            Val::ExnRef(_) => {
                // ExnRef is not supported in GlobalValue, return error
                return Err(WasmtimeError::Type {
                    message: "ExnRef type not supported in globals".to_string(),
                });
            }
            Val::ContRef(_) => {
                // ContRef is not supported in GlobalValue, return error
                return Err(WasmtimeError::Type {
                    message: "ContRef type not supported in globals".to_string(),
                });
            }
        };

        Ok(global_value)
    }

    /// Create a global from an existing wasmtime::Global (for imports/exports)
    pub fn from_wasmtime_global(
        wasmtime_global: WasmtimeGlobal,
        store: &Store,
        name: Option<String>,
    ) -> WasmtimeResult<Self> {
        let global_type = store.with_context_ro(|ctx| Ok(wasmtime_global.ty(&ctx)))?;

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
    use crate::validate_ptr_not_null;
    use std::os::raw::c_void;

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
    pub fn set_global_value(
        global: &Global,
        store: &Store,
        value: GlobalValue,
    ) -> WasmtimeResult<()> {
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
    ///
    /// Uses the consolidated `safe_destroy` utility from `ffi_common::resource_destruction`
    /// which provides double-free protection, fake pointer detection, and panic safety.
    pub unsafe fn destroy_global(global_ptr: *mut c_void) {
        use crate::ffi_common::resource_destruction::safe_destroy;
        let _ = safe_destroy::<Global>(global_ptr, "Global");
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

                match *ref_type.heap_type() {
                    HeapType::Func | HeapType::ConcreteFunc(_) => GlobalValue::FuncRef(ref_id),
                    HeapType::NoFunc => GlobalValue::FuncRef(None),
                    HeapType::Extern => GlobalValue::ExternRef(ref_id),
                    HeapType::NoExtern => GlobalValue::ExternRef(None),
                    HeapType::Eq => GlobalValue::EqRef(ref_id),
                    HeapType::I31 => GlobalValue::I31Ref(ref_id.map(|id| id as i32)),
                    HeapType::Struct | HeapType::ConcreteStruct(_) => {
                        GlobalValue::StructRef(ref_id)
                    }
                    HeapType::Array | HeapType::ConcreteArray(_) => GlobalValue::ArrayRef(ref_id),
                    HeapType::None => GlobalValue::AnyRef(None),
                    _ => GlobalValue::AnyRef(ref_id),
                }
            }
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
            GlobalValue::V128(val) => (
                0,
                i64::from_le_bytes([
                    val[0], val[1], val[2], val[3], val[4], val[5], val[6], val[7],
                ]),
                0.0,
                0.0,
                None,
            ),
            GlobalValue::FuncRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            GlobalValue::ExternRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            GlobalValue::AnyRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            // WasmGC reference types
            GlobalValue::EqRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            GlobalValue::I31Ref(val) => (val.unwrap_or(0), 0, 0.0, 0.0, None),
            GlobalValue::StructRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
            GlobalValue::ArrayRef(ref_id) => (0, 0, 0.0, 0.0, *ref_id),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use crate::store::Store;

    // Use the global shared engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn shared_engine() -> Engine {
        crate::engine::get_shared_engine()
    }

    #[test]
    fn test_global_creation() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Var,
            GlobalValue::I32(42),
            Some("test_global".to_string()),
        )
        .expect("Failed to create global");

        assert!(matches!(global.metadata().value_type, ValType::I32));
        assert_eq!(global.metadata().mutability, Mutability::Var);
        assert_eq!(global.metadata().name, Some("test_global".to_string()));
    }

    #[test]
    fn test_global_get_set() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Var,
            GlobalValue::I32(42),
            None,
        )
        .expect("Failed to create global");

        // Test getting initial value
        let value = global.get(&store).expect("Failed to get global value");
        match value {
            GlobalValue::I32(val) => assert_eq!(val, 42),
            _ => panic!("Expected I32 value"),
        }

        // Test setting new value
        global
            .set(&store, GlobalValue::I32(100))
            .expect("Failed to set global value");

        // Test getting updated value
        let value = global.get(&store).expect("Failed to get global value");
        match value {
            GlobalValue::I32(val) => assert_eq!(val, 100),
            _ => panic!("Expected I32 value"),
        }
    }

    #[test]
    fn test_immutable_global() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Const,
            GlobalValue::I32(42),
            None,
        )
        .expect("Failed to create global");

        // Test that setting fails on immutable global
        let result = global.set(&store, GlobalValue::I32(100));
        assert!(result.is_err());
    }

    #[test]
    fn test_type_validation() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let global = Global::new(
            &store,
            ValType::I32,
            Mutability::Var,
            GlobalValue::I32(42),
            None,
        )
        .expect("Failed to create global");

        // Test that setting wrong type fails
        let result = global.set(&store, GlobalValue::I64(100));
        assert!(result.is_err());
    }

    #[test]
    fn test_different_value_types() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Test I64 global
        let global_i64 = Global::new(
            &store,
            ValType::I64,
            Mutability::Var,
            GlobalValue::I64(123456789),
            None,
        )
        .expect("Failed to create I64 global");

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
        )
        .expect("Failed to create F32 global");

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
        )
        .expect("Failed to create F64 global");

        let value = global_f64.get(&store).expect("Failed to get F64 value");
        match value {
            GlobalValue::F64(val) => assert!((val - 2.71828).abs() < 0.00001),
            _ => panic!("Expected F64 value"),
        }
    }

    #[test]
    fn test_gc_type_globals() {
        use wasmtime::RefType;

        // Create engine with GC enabled
        let engine = Engine::builder()
            .wasm_gc(true)
            .wasm_function_references(true)
            .gc_support(true)
            .build()
            .expect("Failed to create GC-enabled engine");
        let store = Store::new(&engine).expect("Failed to create store");

        // Test ANYREF global (HeapType::Any)
        let anyref_result = Global::new(
            &store,
            ValType::Ref(RefType::ANYREF),
            Mutability::Var,
            GlobalValue::AnyRef(None),
            Some("test_anyref".to_string()),
        );
        assert!(
            anyref_result.is_ok(),
            "Failed to create anyref global: {:?}",
            anyref_result.err()
        );

        // Test EQREF global (HeapType::Eq)
        let eqref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::Eq)),
            Mutability::Var,
            GlobalValue::EqRef(None),
            Some("test_eqref".to_string()),
        );
        assert!(
            eqref_result.is_ok(),
            "Failed to create eqref global: {:?}",
            eqref_result.err()
        );

        // Test I31REF global (HeapType::I31)
        let i31ref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::I31)),
            Mutability::Var,
            GlobalValue::I31Ref(None),
            Some("test_i31ref".to_string()),
        );
        assert!(
            i31ref_result.is_ok(),
            "Failed to create i31ref global: {:?}",
            i31ref_result.err()
        );

        // Test STRUCTREF global (HeapType::Struct)
        let structref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::Struct)),
            Mutability::Var,
            GlobalValue::StructRef(None),
            Some("test_structref".to_string()),
        );
        assert!(
            structref_result.is_ok(),
            "Failed to create structref global: {:?}",
            structref_result.err()
        );

        // Test ARRAYREF global (HeapType::Array)
        let arrayref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::Array)),
            Mutability::Var,
            GlobalValue::ArrayRef(None),
            Some("test_arrayref".to_string()),
        );
        assert!(
            arrayref_result.is_ok(),
            "Failed to create arrayref global: {:?}",
            arrayref_result.err()
        );

        // Test NULLREF global (HeapType::None)
        let nullref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::None)),
            Mutability::Var,
            GlobalValue::AnyRef(None),
            Some("test_nullref".to_string()),
        );
        assert!(
            nullref_result.is_ok(),
            "Failed to create nullref global: {:?}",
            nullref_result.err()
        );

        // Test NULLFUNCREF global (HeapType::NoFunc)
        let nullfuncref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::NoFunc)),
            Mutability::Var,
            GlobalValue::FuncRef(None),
            Some("test_nullfuncref".to_string()),
        );
        assert!(
            nullfuncref_result.is_ok(),
            "Failed to create nullfuncref global: {:?}",
            nullfuncref_result.err()
        );

        // Test NULLEXTERNREF global (HeapType::NoExtern)
        let nullexternref_result = Global::new(
            &store,
            ValType::Ref(RefType::new(true, wasmtime::HeapType::NoExtern)),
            Mutability::Var,
            GlobalValue::ExternRef(None),
            Some("test_nullexternref".to_string()),
        );
        assert!(
            nullexternref_result.is_ok(),
            "Failed to create nullexternref global: {:?}",
            nullexternref_result.err()
        );
    }
}
