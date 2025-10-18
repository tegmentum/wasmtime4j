//! JNI bindings for WAST execution support

#![allow(unused_variables)]

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JString, JObject, JValue};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jint, jboolean, jstring, jobject, jobjectArray};

#[cfg(feature = "jni-bindings")]
use crate::wast_runner::{execute_wast_file, execute_wast_buffer, WastExecutionResult};
#[cfg(feature = "jni-bindings")]
use crate::error::jni_utils;

/// Execute a WAST file and return results
///
/// # Arguments
/// * `file_path` - Path to the WAST file to execute
///
/// # Returns
/// * `jobject` - WastExecutionResult Java object containing execution results
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWastRunner_nativeExecuteWastFile(
    mut env: JNIEnv,
    _class: JClass,
    file_path: JString,
) -> jobject {
    // Convert Java string to Rust string
    let file_path_str: String = match env.get_string(&file_path) {
        Ok(s) => s.into(),
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &crate::error::WasmtimeError::JniError(format!("Failed to get file path: {}", e)));
            return std::ptr::null_mut();
        }
    };

    // Execute WAST file
    let result = match execute_wast_file(&file_path_str) {
        Ok(r) => r,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &crate::error::WasmtimeError::WastExecutionError(format!("WAST execution failed: {}", e)));
            return std::ptr::null_mut();
        }
    };

    // Convert result to Java object
    match wast_result_to_java(&mut env, result) {
        Ok(obj) => obj,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Execute WAST content from a byte buffer
///
/// # Arguments
/// * `filename` - Name to use for error reporting
/// * `content` - WAST content as byte array
///
/// # Returns
/// * `jobject` - WastExecutionResult Java object containing execution results
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWastRunner_nativeExecuteWastBuffer(
    mut env: JNIEnv,
    _class: JClass,
    filename: JString,
    content: jni::objects::JByteArray,
) -> jobject {
    // Convert Java string to Rust
    let filename_str: String = match env.get_string(&filename) {
        Ok(s) => s.into(),
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &crate::error::WasmtimeError::JniError(format!("Failed to get filename: {}", e)));
            return std::ptr::null_mut();
        }
    };

    // Convert byte array to Rust Vec<u8>
    let content_bytes = match env.convert_byte_array(&content) {
        Ok(bytes) => bytes,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &crate::error::WasmtimeError::JniError(format!("Failed to get content: {}", e)));
            return std::ptr::null_mut();
        }
    };

    // Execute WAST buffer
    let result = match execute_wast_buffer(&filename_str, &content_bytes) {
        Ok(r) => r,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &crate::error::WasmtimeError::WastExecutionError(format!("WAST execution failed: {}", e)));
            return std::ptr::null_mut();
        }
    };

    // Convert result to Java object
    match wast_result_to_java(&mut env, result) {
        Ok(obj) => obj,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Convert WastExecutionResult to Java object
#[cfg(feature = "jni-bindings")]
fn wast_result_to_java<'local>(env: &mut JNIEnv<'local>, result: WastExecutionResult) -> crate::error::WasmtimeResult<jobject> {
    // Find WastExecutionResult class
    let result_class = env.find_class("ai/tegmentum/wasmtime4j/jni/WastExecutionResult")
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to find WastExecutionResult class: {}", e)))?;

    // Create Java string for file path
    let file_path_jstring = env.new_string(&result.file_path)
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to create file path string: {}", e)))?;

    // Create Java string for execution error (if present) or null
    let error_jstring_obj: JObject = if let Some(ref error) = result.execution_error {
        env.new_string(error)
            .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to create error string: {}", e)))?
            .into()
    } else {
        JObject::null()
    };

    // Convert directive results to Java array
    let directive_results_array = directive_results_to_java_array(env, &result.directive_results)?;

    // Find constructor
    let ctor_sig = "(Ljava/lang/String;IIILjava/lang/String;[Lai/tegmentum/wasmtime4j/jni/WastDirectiveResult;)V";
    let ctor = env.get_method_id(&result_class, "<init>", ctor_sig)
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to find constructor: {}", e)))?;

    // Create Java object
    let file_path_obj: JObject = file_path_jstring.into();
    let directive_array_obj: JObject = directive_results_array;
    let java_result = unsafe {
        env.new_object_unchecked(
            &result_class,
            ctor,
            &[
                JValue::Object(&file_path_obj).as_jni(),
                JValue::Int(result.total_directives as i32).as_jni(),
                JValue::Int(result.passed_directives as i32).as_jni(),
                JValue::Int(result.failed_directives as i32).as_jni(),
                JValue::Object(&error_jstring_obj).as_jni(),
                JValue::Object(&directive_array_obj).as_jni(),
            ],
        )
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to create WastExecutionResult object: {}", e)))?
    };

    Ok(java_result.into_raw())
}

/// Convert directive results to Java array
#[cfg(feature = "jni-bindings")]
fn directive_results_to_java_array<'local>(
    env: &mut JNIEnv<'local>,
    results: &[crate::wast_runner::WastDirectiveResult],
) -> crate::error::WasmtimeResult<JObject<'local>> {
    // Find WastDirectiveResult class
    let directive_class = env.find_class("ai/tegmentum/wasmtime4j/jni/WastDirectiveResult")
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to find WastDirectiveResult class: {}", e)))?;

    // Create array
    let array_len = results.len() as i32;
    let array = env.new_object_array(array_len, &directive_class, JObject::null())
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to create directive results array: {}", e)))?;

    // Find constructor
    let ctor_sig = "(IZLjava/lang/String;)V";
    let ctor = env.get_method_id(&directive_class, "<init>", ctor_sig)
        .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to find WastDirectiveResult constructor: {}", e)))?;

    // Populate array
    for (i, directive_result) in results.iter().enumerate() {
        // Create error message string (if present) or null
        let error_msg_obj: JObject = if let Some(ref error) = directive_result.error_message {
            env.new_string(error)
                .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to create error message: {}", e)))?
                .into()
        } else {
            JObject::null()
        };

        // Create directive result object
        let directive_obj = unsafe {
            env.new_object_unchecked(
                &directive_class,
                ctor,
                &[
                    JValue::Int(directive_result.line_number as i32).as_jni(),
                    JValue::Bool(if directive_result.passed { 1 } else { 0 }).as_jni(),
                    JValue::Object(&error_msg_obj).as_jni(),
                ],
            )
            .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to create WastDirectiveResult object: {}", e)))?
        };

        // Set array element
        env.set_object_array_element(&array, i as i32, directive_obj)
            .map_err(|e| crate::error::WasmtimeError::JniError(format!("Failed to set array element: {}", e)))?;
    }

    Ok(array.into())
}
