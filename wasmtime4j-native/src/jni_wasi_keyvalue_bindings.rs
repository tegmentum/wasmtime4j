//! JNI bindings for WASI keyvalue operations
//!
//! This module provides JNI wrapper functions for use by the wasmtime4j-jni module
//! to access WASI keyvalue operations (wasi:keyvalue).

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jbyteArray, jlong, jobject, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

use crate::jni_get_ref;
use crate::wasi_keyvalue_helpers::WasiKeyValueContext;

// =============================================================================
// Context Management
// =============================================================================

/// Creates a new WASI keyvalue context
///
/// Returns a handle to the context, or 0 on error
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeCreateContext(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    match WasiKeyValueContext::new() {
        Ok(ctx) => {
            let boxed = Box::new(ctx);
            Box::into_raw(boxed) as jlong
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to create keyvalue context: {}", e),
            );
            0
        }
    }
}

/// Destroys a WASI keyvalue context
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeDestroyContext(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle != 0 {
        unsafe {
            let _ = Box::from_raw(handle as *mut WasiKeyValueContext);
        }
    }
}

/// Gets the context ID
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeGetContextId(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", 0);
    ctx.id() as jlong
}

/// Checks if the context is valid
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeIsContextValid(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    // Special case: validity check should not throw, just return false for invalid handles
    if handle == 0 {
        return JNI_FALSE;
    }
    let ptr = handle as *const WasiKeyValueContext;
    if ptr.is_null() {
        return JNI_FALSE;
    }
    let ctx = unsafe { &*ptr };
    if ctx.is_valid() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

// =============================================================================
// Basic CRUD Operations
// =============================================================================

/// Gets a value by key
///
/// Returns the value as a byte array, or null if not found
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeGet<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: jlong,
    key: JString<'local>,
) -> jbyteArray {
    let ctx = jni_get_ref!(
        env,
        handle,
        WasiKeyValueContext,
        "context",
        std::ptr::null_mut()
    );

    // Get the key string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Failed to get key string: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    // Get the value
    match ctx.get(&key_str) {
        Ok(Some(value)) => match env.byte_array_from_slice(&value) {
            Ok(arr) => arr.into_raw(),
            Err(e) => {
                let _ = env.throw_new(
                    "ai/tegmentum/wasmtime4j/exception/WasmException",
                    format!("Failed to create byte array: {}", e),
                );
                std::ptr::null_mut()
            }
        },
        Ok(None) => std::ptr::null_mut(),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get value: {}", e),
            );
            std::ptr::null_mut()
        }
    }
}

/// Sets a value for a key
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeSet(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    key: JString,
    value: JByteArray,
) -> jboolean {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", JNI_FALSE);

    // Get the key string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Failed to get key string: {}", e),
            );
            return JNI_FALSE;
        }
    };

    // Get the value bytes
    let value_vec = if value.is_null() {
        Vec::new()
    } else {
        match env.convert_byte_array(value) {
            Ok(v) => v,
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/IllegalArgumentException",
                    format!("Failed to get value bytes: {}", e),
                );
                return JNI_FALSE;
            }
        }
    };

    // Set the value
    match ctx.set(&key_str, value_vec) {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to set value: {}", e),
            );
            JNI_FALSE
        }
    }
}

/// Deletes a key
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeDelete(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    key: JString,
) -> jboolean {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", JNI_FALSE);

    // Get the key string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Failed to get key string: {}", e),
            );
            return JNI_FALSE;
        }
    };

    // Delete the key
    match ctx.delete(&key_str) {
        Ok(deleted) => {
            if deleted {
                JNI_TRUE
            } else {
                JNI_FALSE
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to delete key: {}", e),
            );
            JNI_FALSE
        }
    }
}

/// Checks if a key exists
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeExists(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    key: JString,
) -> jboolean {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", JNI_FALSE);

    // Get the key string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Failed to get key string: {}", e),
            );
            return JNI_FALSE;
        }
    };

    // Check if key exists
    match ctx.exists(&key_str) {
        Ok(exists) => {
            if exists {
                JNI_TRUE
            } else {
                JNI_FALSE
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to check key existence: {}", e),
            );
            JNI_FALSE
        }
    }
}

// =============================================================================
// Atomic Operations
// =============================================================================

/// Atomically increments a numeric value
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeIncrement(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    key: JString,
    delta: jlong,
) -> jlong {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", 0);

    // Get the key string
    let key_str: String = match env.get_string(&key) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Failed to get key string: {}", e),
            );
            return 0;
        }
    };

    // Increment the value
    match ctx.increment(&key_str, delta) {
        Ok(new_value) => new_value as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to increment value: {}", e),
            );
            0
        }
    }
}

// =============================================================================
// Store Information
// =============================================================================

/// Gets the number of entries in the store
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeSize(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jlong {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", 0);

    match ctx.size() {
        Ok(size) => size as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get size: {}", e),
            );
            0
        }
    }
}

/// Clears all entries from the store
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeClear(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jboolean {
    let ctx = jni_get_ref!(env, handle, WasiKeyValueContext, "context", JNI_FALSE);

    match ctx.clear() {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to clear store: {}", e),
            );
            JNI_FALSE
        }
    }
}

/// Gets all keys as a JSON array string
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeKeys<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    handle: jlong,
) -> jobject {
    let ctx = jni_get_ref!(
        env,
        handle,
        WasiKeyValueContext,
        "context",
        std::ptr::null_mut()
    );

    match ctx.keys() {
        Ok(keys) => {
            // Convert to JSON string for simplicity
            let json = serde_json::to_string(&keys).unwrap_or_else(|_| "[]".to_string());
            match env.new_string(&json) {
                Ok(s) => s.into_raw(),
                Err(e) => {
                    let _ = env.throw_new(
                        "ai/tegmentum/wasmtime4j/exception/WasmException",
                        format!("Failed to create string: {}", e),
                    );
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get keys: {}", e),
            );
            std::ptr::null_mut()
        }
    }
}

// =============================================================================
// Availability Check
// =============================================================================

/// Checks if WASI keyvalue support is available
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_keyvalue_JniWasiKeyValue_nativeIsAvailable(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    JNI_TRUE
}
