//! JNI bindings for WASI Preview 2 random operations
//!
//! This module provides JNI functions for wasi:random interface including
//! cryptographically-secure random byte generation and random u64 values.

use jni::objects::JClass;
use jni::sys::{jbyteArray, jlong};
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_random_helpers;
use crate::{jni_deref_ptr, jni_validate_handle, jni_validate_non_negative};

/// Get cryptographically-secure random bytes
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_random_JniWasiRandom_nativeGetRandomBytes(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    len: jlong,
) -> jbyteArray {
    // Validate parameters using macros
    jni_validate_handle!(env, context_handle, "context", std::ptr::null_mut());
    jni_validate_non_negative!(env, len, "Length", std::ptr::null_mut());
    let context = jni_deref_ptr!(
        env,
        context_handle,
        WasiPreview2Context,
        "Context",
        std::ptr::null_mut()
    );

    // Call helper function
    let bytes = match wasi_random_helpers::get_random_bytes(context, len as u64) {
        Ok(b) => b,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get random bytes: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    // Create byte array
    let array = match env.new_byte_array(bytes.len() as i32) {
        Ok(arr) => arr,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create byte array: {}", e),
            );
            return std::ptr::null_mut();
        }
    };

    // Convert Vec<u8> to &[i8] for JNI
    let signed_bytes: Vec<i8> = bytes.iter().map(|&b| b as i8).collect();
    if let Err(e) = env.set_byte_array_region(&array, 0, &signed_bytes) {
        let _ = env.throw_new(
            "java/lang/RuntimeException",
            format!("Failed to set array values: {}", e),
        );
        return std::ptr::null_mut();
    }

    array.into_raw()
}

/// Get a cryptographically-secure random u64 value
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_random_JniWasiRandom_nativeGetRandomU64(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", -1);
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", -1);

    // Call helper function
    match wasi_random_helpers::get_random_u64(context) {
        Ok(value) => value as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get random u64: {}", e),
            );
            -1
        }
    }
}
