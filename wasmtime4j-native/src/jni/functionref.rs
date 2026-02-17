//! JNI bindings for WebAssembly function references
//!
//! This module provides JNI bindings for creating and managing function references
//! that can be used with WebAssembly tables and indirect calls.

use jni::objects::JClass;
use jni::sys::{jbyteArray, jint, jlong};
use jni::JNIEnv;

use super::hostfunc::unmarshal_function_type;
use super::linker::JniHostFunctionCallback;
use crate::error::{jni_utils, WasmtimeError};
use crate::store::Store;

/// Create a new function reference from a host function (JNI version)
///
/// Creates a real Rust Func and registers it in the table reference registry.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCreateFunctionReferenceFromHost(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    function_type_data: jbyteArray,
    function_reference_id: jlong,
) -> jlong {
    // Extract data before the closure
    let type_data_result = env
        .convert_byte_array(unsafe { jni::objects::JByteArray::from_raw(function_type_data) })
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to read function type data: {}", e),
            backtrace: None,
        });

    let type_data = match type_data_result {
        Ok(data) => data,
        Err(_) => return 0,
    };

    let jvm = match env.get_java_vm() {
        Ok(vm) => vm,
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        if store_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        // Get the store
        let store_ref = unsafe { &*(store_handle as *const Store) };

        // Get engine from store context and unmarshal function type
        let func_type =
            store_ref.with_context(|ctx| unmarshal_function_type(ctx.engine(), &type_data))?;

        // Create the JNI callback wrapper (jvm is already extracted above)
        let callback = Box::new(JniHostFunctionCallback {
            jvm: std::sync::Arc::new(jvm),
            callback_id: function_reference_id,
            is_function_reference: true, // This is a FunctionReference
        });

        // Create and register the function using Store::create_function_reference
        let name = format!("host_function_{}", function_reference_id);
        let registry_id = store_ref.create_function_reference(name, func_type, callback)?;

        // Return the registry ID directly as jlong
        Ok(registry_id as jlong)
    })
}

/// Create a new function reference from a WebAssembly function (JNI version)
///
/// This is a minimal stub that just returns the function reference ID as a handle.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCreateFunctionReferenceFromWasm(
    mut env: JNIEnv,
    _class: JClass,
    _store_handle: jlong,
    wasm_function_handle: jlong,
    _function_reference_id: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        if wasm_function_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly function handle cannot be null".to_string(),
            });
        }

        // The wasm_function_handle is already a function ID from a JniFunction
        // For function references from WebAssembly functions, we just return the same handle
        let func_id = unsafe { *(wasm_function_handle as *const u64) };

        // Return the function ID as the handle
        Ok(Box::new(func_id))
    }) as jlong
}

/// Call a function reference through native code (JNI version)
///
/// This is a minimal stub - actual calls happen through the Java callback mechanism.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCallFunctionReference(
    mut env: JNIEnv,
    _class: JClass,
    function_reference_handle: jlong,
    _params_data: jbyteArray,
    _results_buffer: jbyteArray,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if function_reference_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Function reference handle cannot be null".to_string(),
            });
        }

        // The actual call happens through the Java callback mechanism
        // The Java side handles parameter/result marshalling through FUNCTION_REFERENCE_REGISTRY
        Ok(0)
    })
}

/// Destroy a function reference (JNI version)
///
/// This is a minimal stub that cleans up the boxed handle.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeDestroyFunctionReference(
    env: JNIEnv,
    _class: JClass,
    function_reference_handle: jlong,
) {
    jni_utils::jni_try_default(&env, (), || {
        if function_reference_handle == 0 {
            return Ok(());
        }

        // Clean up the boxed handle
        unsafe {
            let _ = Box::from_raw(function_reference_handle as *mut u64);
        }

        Ok(())
    });
}
