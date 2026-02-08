//! Async Runtime JNI bindings
//!
//! This module provides JNI bindings for async runtime operations,
//! including initialization, shutdown, and asynchronous function execution.

use std::sync::atomic::{AtomicI32, Ordering};
use std::thread;

use jni::objects::{JByteArray, JClass, JObject, JObjectArray, JString, JValue};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;

use crate::async_runtime::{get_async_runtime, get_runtime_handle};
use crate::engine::get_shared_wasmtime_engine;

/// Initialize the async runtime
/// JNI binding for JniAsyncRuntime.nativeAsyncRuntimeInit
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeAsyncRuntimeInit(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    // Force initialization of the global async runtime
    let _runtime = get_async_runtime();
    log::info!("Async runtime initialized via JNI");
    0
}

/// Get async runtime information
/// JNI binding for JniAsyncRuntime.nativeAsyncRuntimeInfo
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeAsyncRuntimeInfo(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let handle = get_runtime_handle();
    let info = format!("Tokio runtime with {} workers", handle.metrics().num_workers());
    match env.new_string(&info) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Shutdown the async runtime
/// JNI binding for JniAsyncRuntime.nativeAsyncRuntimeShutdown
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeAsyncRuntimeShutdown(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    log::info!("Async runtime shutdown requested via JNI");
    // Note: We don't actually shut down the global runtime as it may be needed
    // by other operations. The runtime will be cleaned up when the process exits.
    0
}

/// Execute a function call asynchronously
/// JNI binding for JniAsyncRuntime.nativeFuncCallAsync
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeFuncCallAsync(
    mut env: JNIEnv,
    _class: JClass,
    instance_ptr: jlong,
    function_name: JString,
    _args: JObjectArray,
    _timeout_ms: jlong,
    _callback: JObject,
    _user_data: JObject,
) -> jint {
    // Validate inputs
    if instance_ptr == 0 {
        log::error!("Invalid instance pointer for async function call");
        return -1;
    }

    // Get function name as Rust string
    let func_name: String = match env.get_string(&function_name) {
        Ok(name) => name.into(),
        Err(e) => {
            log::error!("Failed to get function name: {:?}", e);
            return -1;
        }
    };

    log::debug!("Async function call requested for function: {}", func_name);

    // REQUIRES: Full Wasmtime async integration
    // - Store must be created with async_support enabled in Engine config
    // - Func::call_async requires async Store context
    // - JNI callback must be converted to GlobalRef for safe cross-thread use
    // - Currently infrastructure exists in async_runtime.rs, needs wiring
    log::warn!("Async function calls require async Store configuration - returning error");
    -1
}

/// Compile a module asynchronously
/// JNI binding for JniAsyncRuntime.nativeModuleCompileAsync
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeModuleCompileAsync(
    mut env: JNIEnv,
    _class: JClass,
    module_bytes: JByteArray,
    _timeout_ms: jlong,
    completion_callback: JObject,
    progress_callback: JObject,
    _user_data: JObject,
) -> jint {
    static OPERATION_COUNTER: AtomicI32 = AtomicI32::new(1);

    // Get module bytes
    let bytes = match env.convert_byte_array(&module_bytes) {
        Ok(b) => b,
        Err(e) => {
            log::error!("Failed to get module bytes: {:?}", e);
            return -1;
        }
    };

    if bytes.is_empty() {
        log::error!("Empty module bytes for async compilation");
        return -1;
    }

    log::info!("Async module compilation requested for {} bytes", bytes.len());

    // Get JavaVM for thread-safe callback invocation
    let jvm = match env.get_java_vm() {
        Ok(vm) => vm,
        Err(e) => {
            log::error!("Failed to get JavaVM: {:?}", e);
            return -1;
        }
    };

    // Create global refs for callbacks (thread-safe)
    let completion_global = match env.new_global_ref(&completion_callback) {
        Ok(g) => g,
        Err(e) => {
            log::error!("Failed to create global ref for completion callback: {:?}", e);
            return -1;
        }
    };

    let progress_global = if !progress_callback.is_null() {
        match env.new_global_ref(&progress_callback) {
            Ok(g) => Some(g),
            Err(e) => {
                log::warn!("Failed to create global ref for progress callback: {:?}", e);
                None
            }
        }
    } else {
        None
    };

    let operation_id = OPERATION_COUNTER.fetch_add(1, Ordering::SeqCst);

    // Spawn thread to do compilation asynchronously
    thread::spawn(move || {
        // Attach this thread to JVM
        let mut guard = match jvm.attach_current_thread() {
            Ok(g) => g,
            Err(e) => {
                log::error!("Failed to attach thread to JVM: {:?}", e);
                return;
            }
        };

        // Report progress if callback provided
        if let Some(ref progress_ref) = progress_global {
            // Create Integer object first to avoid borrow conflict
            let progress_obj = guard
                .new_object("java/lang/Integer", "(I)V", &[JValue::Int(50)])
                .unwrap_or_else(|_| JObject::null());

            if let Err(e) = guard.call_method(
                progress_ref.as_obj(),
                "accept",
                "(Ljava/lang/Object;)V",
                &[JValue::Object(&progress_obj)],
            ) {
                log::warn!("Failed to invoke progress callback: {:?}", e);
            }
        }

        // Use the shared wasmtime engine to avoid GLOBAL_CODE accumulation
        let engine = get_shared_wasmtime_engine();

        let compile_result = wasmtime::Module::new(&engine, &bytes);

        match compile_result {
            Ok(_module) => {
                log::info!("Async compilation succeeded for operation {}", operation_id);
                invoke_completion_callback(&mut guard, &completion_global, true, "Compilation successful");
            }
            Err(e) => {
                log::error!("Async compilation failed for operation {}: {:?}", operation_id, e);
                invoke_completion_callback(
                    &mut guard,
                    &completion_global,
                    false,
                    &format!("Compilation failed: {}", e),
                );
            }
        }
    });

    operation_id
}

/// Helper function to invoke the Java completion callback with an AsyncResult
fn invoke_completion_callback(
    env: &mut jni::JNIEnv,
    callback: &jni::objects::GlobalRef,
    success: bool,
    message: &str,
) {
    // Find OperationStatus enum class
    let status_class = match env.find_class("ai/tegmentum/wasmtime4j/async/AsyncRuntime$OperationStatus") {
        Ok(c) => c,
        Err(e) => {
            log::error!("Failed to find OperationStatus class: {:?}", e);
            return;
        }
    };

    // Get the appropriate enum value (COMPLETED or FAILED)
    let status_field_name = if success { "COMPLETED" } else { "FAILED" };
    let status_sig = "Lai/tegmentum/wasmtime4j/async/AsyncRuntime$OperationStatus;";
    let status_value = match env.get_static_field(&status_class, status_field_name, status_sig) {
        Ok(v) => match v.l() {
            Ok(obj) => obj,
            Err(e) => {
                log::error!("Failed to get OperationStatus value as object: {:?}", e);
                return;
            }
        },
        Err(e) => {
            log::error!("Failed to get OperationStatus.{}: {:?}", status_field_name, e);
            return;
        }
    };

    // Find the AsyncResult class (nested inside AsyncRuntime)
    let result_class = match env.find_class("ai/tegmentum/wasmtime4j/async/AsyncRuntime$AsyncResult") {
        Ok(c) => c,
        Err(e) => {
            log::error!("Failed to find AsyncResult class: {:?}", e);
            return;
        }
    };

    // Create message string
    let message_jstr = match env.new_string(message) {
        Ok(s) => s,
        Err(e) => {
            log::error!("Failed to create message string: {:?}", e);
            return;
        }
    };

    // Create AsyncResult instance with correct constructor:
    // AsyncResult(OperationStatus status, int statusCode, String message, Object result)
    let status_code = if success { 0 } else { 1 };
    let result_obj = match env.new_object(
        result_class,
        "(Lai/tegmentum/wasmtime4j/async/AsyncRuntime$OperationStatus;ILjava/lang/String;Ljava/lang/Object;)V",
        &[
            JValue::Object(&status_value),
            JValue::Int(status_code),
            JValue::Object(&message_jstr),
            JValue::Object(&JObject::null()),
        ],
    ) {
        Ok(obj) => obj,
        Err(e) => {
            log::error!("Failed to create AsyncResult: {:?}", e);
            return;
        }
    };

    // Invoke the Consumer.accept method
    if let Err(e) = env.call_method(
        callback.as_obj(),
        "accept",
        "(Ljava/lang/Object;)V",
        &[JValue::Object(&result_obj)],
    ) {
        log::error!("Failed to invoke completion callback: {:?}", e);
    }
}
