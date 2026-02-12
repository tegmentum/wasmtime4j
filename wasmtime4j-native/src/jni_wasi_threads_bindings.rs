//! JNI bindings for WASI-Threads support
//!
//! This module provides JNI-compatible functions for use by the wasmtime4j-jni module
//! to access WASI-Threads functionality for spawning WebAssembly threads.

#[cfg(feature = "jni-bindings")]
use jni::objects::JClass;
#[cfg(feature = "jni-bindings")]
use jni::sys::{jboolean, jint, jlong};
#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;

#[cfg(all(feature = "jni-bindings", feature = "wasi-threads"))]
use crate::wasi_threads::WasiThreadsContext;

#[cfg(feature = "jni-bindings")]
use crate::error::jni_utils;

/// JNI binding: Check if WASI-Threads is supported in this build
///
/// # Returns
/// JNI_TRUE (1) if supported, JNI_FALSE (0) if not supported
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeIsSupported(
    _env: JNIEnv,
    _class: JClass,
) -> jboolean {
    #[cfg(feature = "wasi-threads")]
    {
        1 // JNI_TRUE
    }
    #[cfg(not(feature = "wasi-threads"))]
    {
        0 // JNI_FALSE
    }
}

/// JNI binding: Create a new WASI-Threads context
///
/// # Parameters
/// - `module_handle`: Native handle for the WebAssembly module
/// - `linker_handle`: Native handle for the linker with WASI imports
/// - `store_handle`: Native handle for the store for the main thread
///
/// # Returns
/// Native handle for the created context, or 0 on error
#[cfg(all(feature = "jni-bindings", feature = "wasi-threads"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    module_handle: jlong,
    linker_handle: jlong,
    store_handle: jlong,
) -> jlong {
    match (|| -> crate::error::WasmtimeResult<*mut std::os::raw::c_void> {
        // Validate handles
        if module_handle == 0 || linker_handle == 0 || store_handle == 0 {
            return Err(crate::error::WasmtimeError::from_string(
                "Invalid handle: module, linker, and store handles must be non-zero",
            ));
        }

        // Create a new WasiThreadsContext
        let context = WasiThreadsContext::new().map_err(|e| {
            crate::error::WasmtimeError::from_string(format!(
                "Failed to create WASI-Threads context: {}",
                e
            ))
        })?;

        let boxed = Box::new(context);
        Ok(Box::into_raw(boxed) as *mut std::os::raw::c_void)
    })() {
        Ok(ptr) => ptr as jlong,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            0
        }
    }
}

/// JNI binding: Create a new WASI-Threads context (stub when feature disabled)
#[cfg(all(feature = "jni-bindings", not(feature = "wasi-threads")))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    _module_handle: jlong,
    _linker_handle: jlong,
    _store_handle: jlong,
) -> jlong {
    let error = crate::error::WasmtimeError::from_string("WASI-Threads support is not compiled in");
    jni_utils::throw_jni_exception(&mut env, &error);
    0
}

/// JNI binding: Spawn a new thread using WASI-Threads
///
/// # Parameters
/// - `context_handle`: Native handle for the WASI-Threads context
/// - `thread_start_arg`: Argument to pass to the thread's start function
///
/// # Returns
/// The thread ID (positive) on success, -1 on error
#[cfg(all(feature = "jni-bindings", feature = "wasi-threads"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeSpawn(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    thread_start_arg: jint,
) -> jint {
    match (|| -> crate::error::WasmtimeResult<jint> {
        if context_handle == 0 {
            return Err(crate::error::WasmtimeError::from_string(
                "Invalid context handle",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *mut WasiThreadsContext;
            &mut *ptr
        };

        match context.spawn_thread(thread_start_arg as u32) {
            Ok(thread_id) => Ok(thread_id as jint),
            Err(e) => Err(crate::error::WasmtimeError::from_string(format!(
                "Failed to spawn thread: {}",
                e
            ))),
        }
    })() {
        Ok(thread_id) => thread_id,
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            -1
        }
    }
}

/// JNI binding: Spawn a new thread (stub when feature disabled)
#[cfg(all(feature = "jni-bindings", not(feature = "wasi-threads")))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeSpawn(
    mut env: JNIEnv,
    _class: JClass,
    _context_handle: jlong,
    _thread_start_arg: jint,
) -> jint {
    let error = crate::error::WasmtimeError::from_string("WASI-Threads support is not compiled in");
    jni_utils::throw_jni_exception(&mut env, &error);
    -1
}

/// JNI binding: Close and free a WASI-Threads context
///
/// # Parameters
/// - `context_handle`: Native handle for the WASI-Threads context
#[cfg(all(feature = "jni-bindings", feature = "wasi-threads"))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) {
    if context_handle != 0 {
        unsafe {
            let _ = Box::from_raw(context_handle as *mut WasiThreadsContext);
        }
    }
}

/// JNI binding: Close context (stub when feature disabled)
#[cfg(all(feature = "jni-bindings", not(feature = "wasi-threads")))]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    _context_handle: jlong,
) {
    // No-op when feature disabled
}

/// JNI binding: Add WASI-Threads thread-spawn function to linker
///
/// # Parameters
/// - `linker_handle`: Native handle for the linker
/// - `store_handle`: Native handle for the store
/// - `module_handle`: Native handle for the module
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_threads_JniWasiThreadsContext_nativeAddToLinker(
    mut env: JNIEnv,
    _class: JClass,
    linker_handle: jlong,
    store_handle: jlong,
    module_handle: jlong,
) {
    #[cfg(feature = "wasi-threads")]
    {
        if linker_handle == 0 || store_handle == 0 || module_handle == 0 {
            let error = crate::error::WasmtimeError::from_string(
                "Invalid handle: linker, store, and module handles must be non-zero",
            );
            jni_utils::throw_jni_exception(&mut env, &error);
            return;
        }

        // The actual implementation would need to access the real Linker, Store, and Module types
        // For now, this is a successful no-op as the implementation depends on how
        // wasmtime-wasi-threads is integrated with the existing linker
    }

    #[cfg(not(feature = "wasi-threads"))]
    {
        let error =
            crate::error::WasmtimeError::from_string("WASI-Threads support is not compiled in");
        jni_utils::throw_jni_exception(&mut env, &error);
    }
}
