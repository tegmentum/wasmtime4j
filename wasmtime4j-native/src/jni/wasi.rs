//! JNI bindings for WASI context operations

use jni::objects::JClass;
use jni::sys::jlong;
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::wasi::{WasiContext, WasiFileDescriptorManager};

/// Create a new WASI context with default configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeCreateContext(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let ctx = WasiContext::new()?;
        let fd_manager = WasiFileDescriptorManager::new();
        let combined = Box::new((ctx, fd_manager));
        Ok(combined)
    }) as jlong
}

/// Destroy a WASI context and free its resources
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeDestroyContext(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) {
    if context_handle != 0 {
        let _: Box<(WasiContext, WasiFileDescriptorManager)> = unsafe {
            Box::from_raw(context_handle as *mut (WasiContext, WasiFileDescriptorManager))
        };
    }
}
