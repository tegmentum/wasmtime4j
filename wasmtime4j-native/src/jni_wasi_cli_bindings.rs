//! JNI bindings for WASI Preview 2 CLI operations
//!
//! This module provides JNI functions for wasi:cli interfaces including
//! environment variables, command-line arguments, standard I/O streams, and program exit.

use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jlong, jobjectArray, jstring};
use jni::JNIEnv;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::WasiPreview2Context;

/// Get all environment variables
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiEnvironment_nativeGetAll(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jobjectArray {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return JObject::null().into_raw();
        }
        &*ptr
    };

    // Get environment variables from context
    let env_map = match context.environment.read() {
        Ok(env_map) => env_map,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to read environment variables: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Convert to array of "key=value" strings
    let env_strings: Vec<String> = env_map
        .iter()
        .map(|(k, v)| format!("{}={}", k, v))
        .collect();

    // Create Java String array
    let string_class = match env.find_class("java/lang/String") {
        Ok(cls) => cls,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to find String class: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    let array = match env.new_object_array(env_strings.len() as i32, string_class, JObject::null()) {
        Ok(arr) => arr,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create array: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Populate array
    for (i, env_str) in env_strings.iter().enumerate() {
        let java_str = match env.new_string(env_str) {
            Ok(s) => s,
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to create string: {}", e),
                );
                return JObject::null().into_raw();
            }
        };

        if let Err(e) = env.set_object_array_element(&array, i as i32, java_str) {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to set array element: {}", e),
            );
            return JObject::null().into_raw();
        }
    }

    array.into_raw()
}

/// Get single environment variable
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiEnvironment_nativeGet(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    name: JString,
) -> jstring {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return JObject::null().into_raw();
        }
        &*ptr
    };

    // Get name string
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("Invalid name string: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Get environment variables from context
    let env_map = match context.environment.read() {
        Ok(env_map) => env_map,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to read environment variables: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Look up the variable
    match env_map.get(&name_str) {
        Some(value) => {
            match env.new_string(value) {
                Ok(java_str) => java_str.into_raw(),
                Err(e) => {
                    let _ = env.throw_new(
                        "java/lang/RuntimeException",
                        format!("Failed to create string: {}", e),
                    );
                    JObject::null().into_raw()
                }
            }
        }
        None => JObject::null().into_raw(),
    }
}

/// Get command-line arguments
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiEnvironment_nativeGetArguments(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jobjectArray {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return JObject::null().into_raw();
        }
        &*ptr
    };

    // Get arguments from context
    let args = match context.arguments.read() {
        Ok(args) => args,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to read arguments: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Create Java String array
    let string_class = match env.find_class("java/lang/String") {
        Ok(cls) => cls,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to find String class: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    let array = match env.new_object_array(args.len() as i32, string_class, JObject::null()) {
        Ok(arr) => arr,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create array: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Populate array
    for (i, arg) in args.iter().enumerate() {
        let java_str = match env.new_string(arg) {
            Ok(s) => s,
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to create string: {}", e),
                );
                return JObject::null().into_raw();
            }
        };

        if let Err(e) = env.set_object_array_element(&array, i as i32, java_str) {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to set array element: {}", e),
            );
            return JObject::null().into_raw();
        }
    }

    array.into_raw()
}

/// Get initial working directory
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiEnvironment_nativeGetInitialCwd(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jstring {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return JObject::null().into_raw();
        }
        &*ptr
    };

    // Get initial_cwd from context
    let cwd = match context.initial_cwd.read() {
        Ok(cwd) => cwd,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to read initial_cwd: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Return the cwd if set, or null
    match cwd.as_ref() {
        Some(path) => {
            match env.new_string(path) {
                Ok(java_str) => java_str.into_raw(),
                Err(e) => {
                    let _ = env.throw_new(
                        "java/lang/RuntimeException",
                        format!("Failed to create string: {}", e),
                    );
                    JObject::null().into_raw()
                }
            }
        }
        None => JObject::null().into_raw(),
    }
}

/// Get stdin stream
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiStdio_nativeGetStdin(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return 0;
    }

    // Get context from handle
    let _context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // TODO: Implement actual WASI Preview 2 stdio get_stdin
    let _ = env.throw_new(
        "java/lang/UnsupportedOperationException",
        "get_stdin not yet implemented",
    );
    0
}

/// Get stdout stream
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiStdio_nativeGetStdout(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return 0;
    }

    // Get context from handle
    let _context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // TODO: Implement actual WASI Preview 2 stdio get_stdout
    let _ = env.throw_new(
        "java/lang/UnsupportedOperationException",
        "get_stdout not yet implemented",
    );
    0
}

/// Get stderr stream
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiStdio_nativeGetStderr(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return 0;
    }

    // Get context from handle
    let _context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return 0;
        }
        &*ptr
    };

    // TODO: Implement actual WASI Preview 2 stdio get_stderr
    let _ = env.throw_new(
        "java/lang/UnsupportedOperationException",
        "get_stderr not yet implemented",
    );
    0
}

/// Exit with status code
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_cli_JniWasiExit_nativeExit(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    status_code: jint,
) -> jint {
    // Validate parameters
    if context_handle == 0 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    // Get context from handle
    let _context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            let _ = env.throw_new("java/lang/NullPointerException", "Context pointer is null");
            return -1;
        }
        &*ptr
    };

    // TODO: Implement actual WASI Preview 2 exit
    let _ = env.throw_new(
        "java/lang/UnsupportedOperationException",
        "exit not yet implemented",
    );
    -1
}
