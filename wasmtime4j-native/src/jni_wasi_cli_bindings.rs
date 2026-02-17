//! JNI bindings for WASI CLI operations
//!
//! This module provides JNI functions for wasi:cli interfaces including
//! environment variables, command-line arguments, standard I/O streams, and program exit.
//!
//! Note: These bindings work with WasiContext (Preview 1) to provide CLI functionality.

use jni::objects::{JClass, JObject, JString};
use jni::sys::{jint, jlong, jobjectArray, jstring};
use jni::JNIEnv;

use crate::wasi::{WasiContext, WasiStreamInfo, WasiStreamStatusInfo, WasiStreamTypeInfo};
use crate::{jni_deref_ptr, jni_validate_handle};

/// Minimum valid pointer value - pointers below this are almost certainly invalid
/// as they fall within the first page which is never mapped on most systems.
/// This helps detect corrupted handles early and fail with a clear error message
/// instead of crashing the JVM.
const MIN_VALID_POINTER: u64 = 0x1000; // 4KB page size

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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null().into_raw());
    let context = jni_deref_ptr!(
        env,
        context_handle,
        WasiContext,
        "Context",
        JObject::null().into_raw()
    );

    // Get environment variables from context
    let env_map = &context.environment;

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

    let array = match env.new_object_array(env_strings.len() as i32, string_class, JObject::null())
    {
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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null().into_raw());
    let context = jni_deref_ptr!(
        env,
        context_handle,
        WasiContext,
        "Context",
        JObject::null().into_raw()
    );

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
    let env_map = &context.environment;

    // Look up the variable
    match env_map.get(&name_str) {
        Some(value) => match env.new_string(value) {
            Ok(java_str) => java_str.into_raw(),
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to create string: {}", e),
                );
                JObject::null().into_raw()
            }
        },
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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null().into_raw());
    let context = jni_deref_ptr!(
        env,
        context_handle,
        WasiContext,
        "Context",
        JObject::null().into_raw()
    );

    // Get arguments from context
    let args = &context.arguments;

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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null().into_raw());
    let context = jni_deref_ptr!(
        env,
        context_handle,
        WasiContext,
        "Context",
        JObject::null().into_raw()
    );

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
        Some(path) => match env.new_string(path) {
            Ok(java_str) => java_str.into_raw(),
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to create string: {}", e),
                );
                JObject::null().into_raw()
            }
        },
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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", 0);

    // Additional validation: check pointer is in reasonable range (not corrupted)
    if (context_handle as u64) < MIN_VALID_POINTER {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            format!("Invalid context handle: suspiciously small value 0x{:x}. This suggests the WasiContext native handle is corrupted or was not properly created.", context_handle),
        );
        return 0;
    }

    let context = jni_deref_ptr!(env, context_handle, WasiContext, "Context", 0);

    // Standard stdin handle ID
    const STDIN_HANDLE_ID: u32 = 1;

    // Ensure stdin stream is registered in the streams map
    {
        let mut streams = match context.streams.write() {
            Ok(streams) => streams,
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to acquire streams lock: {}", e),
                );
                return 0;
            }
        };

        // Create stdin stream if not already registered
        if !streams.contains_key(&STDIN_HANDLE_ID) {
            let stdin_stream = WasiStreamInfo {
                id: STDIN_HANDLE_ID,
                stream_type: WasiStreamTypeInfo::InputStream,
                buffer: Vec::new(),
                status: WasiStreamStatusInfo::Ready,
                resource_id: None,
            };
            streams.insert(STDIN_HANDLE_ID, stdin_stream);
        }
    }

    // Update stdin_handle in context
    if let Ok(mut handle_opt) = context.stdin_handle.write() {
        *handle_opt = Some(STDIN_HANDLE_ID as u64);
    }

    STDIN_HANDLE_ID as jlong
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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", 0);

    // Additional validation: check pointer is in reasonable range (not corrupted)
    if (context_handle as u64) < MIN_VALID_POINTER {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            format!("Invalid context handle: suspiciously small value 0x{:x}. This suggests the WasiContext native handle is corrupted or was not properly created.", context_handle),
        );
        return 0;
    }

    let context = jni_deref_ptr!(env, context_handle, WasiContext, "Context", 0);

    // Standard stdout handle ID
    const STDOUT_HANDLE_ID: u32 = 2;

    // Ensure stdout stream is registered in the streams map
    {
        let mut streams = match context.streams.write() {
            Ok(streams) => streams,
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to acquire streams lock: {}", e),
                );
                return 0;
            }
        };

        // Create stdout stream if not already registered
        if !streams.contains_key(&STDOUT_HANDLE_ID) {
            let stdout_stream = WasiStreamInfo {
                id: STDOUT_HANDLE_ID,
                stream_type: WasiStreamTypeInfo::OutputStream,
                buffer: Vec::new(),
                status: WasiStreamStatusInfo::Ready,
                resource_id: None,
            };
            streams.insert(STDOUT_HANDLE_ID, stdout_stream);
        }
    }

    // Update stdout_handle in context
    if let Ok(mut handle_opt) = context.stdout_handle.write() {
        *handle_opt = Some(STDOUT_HANDLE_ID as u64);
    }

    STDOUT_HANDLE_ID as jlong
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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", 0);

    // Additional validation: check pointer is in reasonable range (not corrupted)
    if (context_handle as u64) < MIN_VALID_POINTER {
        let _ = env.throw_new(
            "java/lang/IllegalArgumentException",
            format!("Invalid context handle: suspiciously small value 0x{:x}. This suggests the WasiContext native handle is corrupted or was not properly created.", context_handle),
        );
        return 0;
    }

    let context = jni_deref_ptr!(env, context_handle, WasiContext, "Context", 0);

    // Standard stderr handle ID
    const STDERR_HANDLE_ID: u32 = 3;

    // Ensure stderr stream is registered in the streams map
    {
        let mut streams = match context.streams.write() {
            Ok(streams) => streams,
            Err(e) => {
                let _ = env.throw_new(
                    "java/lang/RuntimeException",
                    format!("Failed to acquire streams lock: {}", e),
                );
                return 0;
            }
        };

        // Create stderr stream if not already registered
        if !streams.contains_key(&STDERR_HANDLE_ID) {
            let stderr_stream = WasiStreamInfo {
                id: STDERR_HANDLE_ID,
                stream_type: WasiStreamTypeInfo::OutputStream,
                buffer: Vec::new(),
                status: WasiStreamStatusInfo::Ready,
                resource_id: None,
            };
            streams.insert(STDERR_HANDLE_ID, stderr_stream);
        }
    }

    // Update stderr_handle in context
    if let Ok(mut handle_opt) = context.stderr_handle.write() {
        *handle_opt = Some(STDERR_HANDLE_ID as u64);
    }

    STDERR_HANDLE_ID as jlong
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
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", -1);
    let context = jni_deref_ptr!(env, context_handle, WasiContext, "Context", -1);

    // Store the exit code in the context
    match context.exit_code.write() {
        Ok(mut exit_code_opt) => {
            *exit_code_opt = Some(status_code);
            status_code
        }
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to write exit code: {}", e),
            );
            -1
        }
    }
}
