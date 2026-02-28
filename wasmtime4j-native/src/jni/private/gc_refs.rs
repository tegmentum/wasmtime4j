//! GC reference raw conversion JNI bindings
//!
//! Provides JNI functions for ExternRef raw conversions using the module
//! execution store. AnyRef operations go through the JNI GC runtime bindings
//! (jni_gc_bindings.rs) since AnyRef's native data lives in the GC runtime's
//! internal store.

use jni::objects::JClass;
use jni::sys::jlong;
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::error::WasmtimeError;

// ============================================================================
// ExternRef Raw Conversions
// ============================================================================

/// JNI binding for nativeExternRefToRaw
/// Converts ExternRef data (i64) to a raw u32 GC heap index.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeExternRefToRaw(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    externref_data: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        use wasmtime::{ExternRef, RootScope};

        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        let externref = ExternRef::new(&mut scope, externref_data).map_err(|e| {
            WasmtimeError::Internal {
                message: format!("Failed to create ExternRef: {}", e),
            }
        })?;
        let raw = externref
            .to_raw(&mut scope)
            .map_err(|e| WasmtimeError::Internal {
                message: format!("Failed to convert ExternRef to raw: {}", e),
            })?;

        Ok(raw as jlong)
    })
}

/// JNI binding for nativeExternRefFromRaw
/// Creates ExternRef from a raw u32 GC heap index, returns the i64 data.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeExternRefFromRaw(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    raw: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, i64::MIN, || {
        use wasmtime::{ExternRef, RootScope};

        let store =
            unsafe { crate::store::core::get_store_ref(store_handle as *const std::ffi::c_void)? };
        let mut store_guard = store.try_lock_store()?;

        let mut scope = RootScope::new(&mut *store_guard);
        match ExternRef::from_raw(&mut scope, raw as u32) {
            Some(rooted) => {
                let data = rooted.data(&scope).map_err(|e| WasmtimeError::Internal {
                    message: format!("Failed to extract ExternRef data: {}", e),
                })?;
                match data {
                    Some(any) => match any.downcast_ref::<i64>() {
                        Some(&id) => Ok(id),
                        None => Ok(i64::MIN),
                    },
                    None => Ok(i64::MIN),
                }
            }
            None => Ok(i64::MIN),
        }
    })
}
