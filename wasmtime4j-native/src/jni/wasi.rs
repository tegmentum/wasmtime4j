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

/// Set network configuration on a WASI context
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeSetNetworkConfig(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    allow_network: jni::sys::jboolean,
    allow_tcp: jni::sys::jboolean,
    allow_udp: jni::sys::jboolean,
    allow_ip_name_lookup: jni::sys::jboolean,
) {
    jni_utils::jni_try_void(&mut env, || {
        let (ctx, _) = unsafe {
            &mut *(context_handle as *mut (WasiContext, WasiFileDescriptorManager))
        };
        ctx.config.allow_network = allow_network != 0;
        ctx.config.allow_tcp = allow_tcp != 0;
        ctx.config.allow_udp = allow_udp != 0;
        ctx.config.allow_ip_name_lookup = allow_ip_name_lookup != 0;
        ctx.network_enabled = allow_network != 0;
        ctx.rebuild_context()?;
        Ok(())
    });
}

/// Set whether blocking the current thread is allowed
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeSetAllowBlocking(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    allow: jni::sys::jboolean,
) {
    jni_utils::jni_try_void(&mut env, || {
        let (ctx, _) = unsafe {
            &mut *(context_handle as *mut (WasiContext, WasiFileDescriptorManager))
        };
        ctx.config.allow_blocking_current_thread = allow != 0;
        ctx.rebuild_context()?;
        Ok(())
    });
}

/// Set the insecure random seed for deterministic testing
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeSetInsecureRandomSeed(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    seed_lo: jlong,
    seed_hi: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let (ctx, _) = unsafe {
            &mut *(context_handle as *mut (WasiContext, WasiFileDescriptorManager))
        };
        ctx.config.insecure_random_seed = (seed_hi as u128) << 64 | (seed_lo as u64 as u128);
        ctx.rebuild_context()?;
        Ok(())
    });
}
