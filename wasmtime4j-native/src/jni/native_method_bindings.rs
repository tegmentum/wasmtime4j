//! JNI bindings for NativeMethodBindings validation and initialization.
//!
//! These functions are called during the one-time library initialization flow in
//! `NativeMethodBindings.java`. They serve two purposes:
//! 1. Smoke-test the JNI bridge by creating and destroying a runtime handle
//! 2. Perform native-side initialization (logging setup)

use jni::objects::JClass;
use jni::sys::jlong;
use jni::JNIEnv;

/// Create a runtime handle for JNI validation.
///
/// Called by `NativeMethodBindings.validateNativeMethods()` to verify the native
/// library is correctly linked. Creates a lightweight runtime handle that should
/// be immediately destroyed via `nativeDestroyRuntime`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCreateRuntime(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    let handle = Box::new(crate::jni::runtime::WasmtimeRuntime::new());
    Box::into_raw(handle) as jlong
}

/// Destroy a runtime handle created by `nativeCreateRuntime`.
///
/// Called by `NativeMethodBindings.validateNativeMethods()` immediately after
/// `nativeCreateRuntime` to clean up the smoke-test handle.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeDestroyRuntime(
    _env: JNIEnv,
    _class: JClass,
    runtime_handle: jlong,
) {
    if runtime_handle == 0 {
        log::warn!("NativeMethodBindings.nativeDestroyRuntime: null handle");
        return;
    }
    unsafe {
        let _runtime = Box::from_raw(runtime_handle as *mut crate::jni::runtime::WasmtimeRuntime);
    }
}

/// Initialize the native library.
///
/// Called by `NativeMethodBindings.performNativeInitialization()` during
/// the one-time library initialization flow. Sets up the logging subsystem.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeInitialize(
    _env: JNIEnv,
    _class: JClass,
) {
    // Initialize the logging subsystem. Duplicate calls are harmless.
    let _ = env_logger::try_init();
    log::debug!("Native library initialized via NativeMethodBindings");
}
