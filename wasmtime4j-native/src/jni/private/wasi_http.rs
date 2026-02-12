//! WASI HTTP JNI bindings
//!
//! This module provides JNI bindings for WASI HTTP functionality,
//! allowing Java code to create and manage WASI HTTP contexts.

use jni::objects::{JClass, JObject};
use jni::sys::{jboolean, jlong};
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::wasi_http::{WasiHttpConfig, WasiHttpContext};

/// Create a new WASI HTTP context
/// JNI binding for JniWasiHttpContext.nativeCreate
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    _config: JObject,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Create a default WASI HTTP context
        let config = WasiHttpConfig::default();
        let ctx = WasiHttpContext::new(config)?;

        log::info!("Created WASI HTTP context with ID: {}", ctx.id());

        Ok(Box::into_raw(Box::new(ctx)) as jlong)
    })
}

/// Check if WASI HTTP context is valid
/// JNI binding for JniWasiHttpContext.nativeIsValid
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeIsValid(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jboolean {
    if ctx_handle == 0 {
        return 0;
    }

    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    if ctx.is_valid() {
        1
    } else {
        0
    }
}

/// Reset WASI HTTP context statistics
/// JNI binding for JniWasiHttpContext.nativeResetStats
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeResetStats(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) {
    if ctx_handle == 0 {
        return;
    }

    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.reset_stats();
    log::debug!("Reset WASI HTTP context stats");
}

/// Free WASI HTTP context
/// JNI binding for JniWasiHttpContext.nativeFree
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeFree(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) {
    if ctx_handle == 0 {
        return;
    }

    unsafe {
        let _ = Box::from_raw(ctx_handle as *mut WasiHttpContext);
    }
    log::debug!("Freed WASI HTTP context");
}
