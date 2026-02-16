//! JNI error conversion utilities
//!
//! This module provides utilities for converting Wasmtime errors to JNI exceptions
//! and handling JNI-specific error scenarios.

use super::ffi_utils::clear_last_error;
use super::{ErrorCode, WasmtimeError, WasmtimeResult};
use std::os::raw::c_void;

/// Clear the last error (stub for JNI compatibility)
#[allow(dead_code)]
fn jni_clear_last_error() {
    // For now, this is a no-op since we don't maintain global error state
    // This could be extended to clear thread-local error storage if needed
}

/// Convert WasmtimeError to JNI exception class name
pub fn error_to_exception_class(error: &WasmtimeError) -> &'static str {
    match error {
        WasmtimeError::Compilation { .. } => {
            "ai/tegmentum/wasmtime4j/exception/CompilationException"
        }
        WasmtimeError::Validation { .. } => "ai/tegmentum/wasmtime4j/exception/ValidationException",
        WasmtimeError::Runtime { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::Memory { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::Function { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::Type { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::InvalidParameter { .. } => "java/lang/IllegalArgumentException",
        WasmtimeError::Io { .. } => "java/io/IOException",
        WasmtimeError::Component { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::Interface { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::EngineConfig { .. } => {
            "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException"
        }
        WasmtimeError::Store { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::Instance { .. } => {
            "ai/tegmentum/wasmtime4j/exception/InstantiationException"
        }
        WasmtimeError::Instantiation { .. } => "ai/tegmentum/wasmtime4j/exception/LinkingException",
        WasmtimeError::ImportExport { .. } => "ai/tegmentum/wasmtime4j/exception/LinkingException",
        WasmtimeError::Resource { .. } => "ai/tegmentum/wasmtime4j/exception/ResourceException",
        WasmtimeError::Concurrency { .. } => {
            "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException"
        }
        WasmtimeError::Wasi { .. } => "ai/tegmentum/wasmtime4j/exception/WasiException",
        WasmtimeError::Security { .. } => "ai/tegmentum/wasmtime4j/exception/SecurityException",
        WasmtimeError::Internal { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
        WasmtimeError::Execution { .. } => "ai/tegmentum/wasmtime4j/exception/TrapException",
        WasmtimeError::ExportNotFound { .. } => {
            "ai/tegmentum/wasmtime4j/exception/LinkingException"
        }
        WasmtimeError::Multiple { .. } => "ai/tegmentum/wasmtime4j/exception/WasmException",
        WasmtimeError::TypeMismatch { .. } => {
            "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException"
        }
        _ => "ai/tegmentum/wasmtime4j/exception/WasmException",
    }
}

#[cfg(feature = "jni-bindings")]
/// Throw JNI exception with proper error information
pub fn throw_jni_exception(env: &mut jni::JNIEnv, error: &WasmtimeError) {
    use jni::objects::JValue;

    let class_name = error_to_exception_class(error);
    let message = error.to_string();

    // Handle exceptions that require enum parameters
    let result = match error {
        WasmtimeError::ImportExport { .. }
        | WasmtimeError::ExportNotFound { .. }
        | WasmtimeError::Instantiation { .. } => {
            // LinkingException requires LinkingErrorType enum
            throw_linking_exception(env, &message)
        }
        WasmtimeError::Execution { .. } => {
            // TrapException requires TrapType enum
            throw_trap_exception(env, &message)
        }
        _ => {
            // For other exceptions, use simple String constructor
            env.throw_new(class_name, &message)
        }
    };

    if let Err(e) = result {
        // Fallback to RuntimeException if specific exception class doesn't exist
        log::error!(
            "Failed to throw specific exception {}: {:?}, using RuntimeException",
            class_name,
            e
        );
        if let Err(e2) = env.throw_new("java/lang/RuntimeException", &message) {
            log::error!("Failed to throw fallback RuntimeException: {:?}", e2);
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Helper to throw LinkingException with LinkingErrorType.UNKNOWN
fn throw_linking_exception(env: &mut jni::JNIEnv, message: &str) -> Result<(), jni::errors::Error> {
    use jni::objects::{JThrowable, JValue};

    // Get LinkingErrorType.UNKNOWN enum value
    let error_type_class =
        env.find_class("ai/tegmentum/wasmtime4j/exception/LinkingException$LinkingErrorType")?;
    let unknown_field = env.get_static_field(
        error_type_class,
        "UNKNOWN",
        "Lai/tegmentum/wasmtime4j/exception/LinkingException$LinkingErrorType;",
    )?;

    // Create LinkingException(LinkingErrorType, String)
    let exception_class = env.find_class("ai/tegmentum/wasmtime4j/exception/LinkingException")?;
    let message_jstring = env.new_string(message)?;

    let exception_obj = env.new_object(
        exception_class,
        "(Lai/tegmentum/wasmtime4j/exception/LinkingException$LinkingErrorType;Ljava/lang/String;)V",
        &[(&unknown_field).into(), JValue::Object(&message_jstring)]
    )?;

    env.throw(JThrowable::from(exception_obj))
}

#[cfg(feature = "jni-bindings")]
/// Helper to throw TrapException with TrapType.UNKNOWN
fn throw_trap_exception(env: &mut jni::JNIEnv, message: &str) -> Result<(), jni::errors::Error> {
    use jni::objects::{JThrowable, JValue};

    // Get TrapType.UNKNOWN enum value
    let error_type_class =
        env.find_class("ai/tegmentum/wasmtime4j/exception/TrapException$TrapType")?;
    let unknown_field = env.get_static_field(
        error_type_class,
        "UNKNOWN",
        "Lai/tegmentum/wasmtime4j/exception/TrapException$TrapType;",
    )?;

    // Create TrapException(TrapType, String)
    let exception_class = env.find_class("ai/tegmentum/wasmtime4j/exception/TrapException")?;
    let message_jstring = env.new_string(message)?;

    let exception_obj = env.new_object(
        exception_class,
        "(Lai/tegmentum/wasmtime4j/exception/TrapException$TrapType;Ljava/lang/String;)V",
        &[(&unknown_field).into(), JValue::Object(&message_jstring)],
    )?;

    env.throw(JThrowable::from(exception_obj))
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning pointer for success
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try_ptr<F, T>(env: &mut jni::JNIEnv, operation: F) -> *mut c_void
where
    F: FnOnce() -> WasmtimeResult<Box<T>>,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation));
    match result {
        Ok(Ok(boxed)) => {
            clear_last_error();
            Box::into_raw(boxed) as *mut c_void
        }
        Ok(Err(error)) => {
            throw_jni_exception(env, &error);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            log::error!("Native panic in JNI call: {}", panic_msg);
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
            std::ptr::null_mut()
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning error code
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try_code<F>(env: &mut jni::JNIEnv, operation: F) -> i32
where
    F: FnOnce() -> WasmtimeResult<()>,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation));
    match result {
        Ok(Ok(())) => {
            clear_last_error();
            ErrorCode::Success as i32
        }
        Ok(Err(error)) => {
            let code = error.to_error_code();
            throw_jni_exception(env, &error);
            code as i32
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            log::error!("Native panic in JNI call: {}", panic_msg);
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
            ErrorCode::RuntimeError as i32
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning typed result
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try<F, T>(env: &mut jni::JNIEnv, operation: F) -> (ErrorCode, T)
where
    F: FnOnce() -> WasmtimeResult<T>,
    T: Default,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation));
    match result {
        Ok(Ok(value)) => {
            clear_last_error();
            (ErrorCode::Success, value)
        }
        Ok(Err(error)) => {
            let code = error.to_error_code();
            throw_jni_exception(env, &error);
            (code, T::default())
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            log::error!("Native panic in JNI call: {}", panic_msg);
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
            (ErrorCode::RuntimeError, T::default())
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning default value on error
pub fn jni_try_default<F, T>(_env: &jni::JNIEnv, default_value: T, operation: F) -> T
where
    F: FnOnce() -> WasmtimeResult<T>,
{
    match operation() {
        Ok(result) => {
            clear_last_error();
            result
        }
        Err(error) => {
            // For now, we can't throw exceptions from a ref to env
            // This would need refactoring in the calling code
            log::error!("Error in jni_try_default: {:?}", error);
            default_value
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning boolean result
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try_bool<F>(env: &mut jni::JNIEnv, operation: F) -> bool
where
    F: FnOnce() -> WasmtimeResult<bool>,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation));
    match result {
        Ok(Ok(value)) => {
            clear_last_error();
            value
        }
        Ok(Err(error)) => {
            throw_jni_exception(env, &error);
            false
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
            false
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning default value on error
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try_with_default<F, T>(env: &mut jni::JNIEnv, default_value: T, operation: F) -> T
where
    F: FnOnce() -> WasmtimeResult<T>,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation));
    match result {
        Ok(Ok(value)) => {
            clear_last_error();
            value
        }
        Ok(Err(error)) => {
            throw_jni_exception(env, &error);
            default_value
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
            default_value
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning void
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try_void<F>(env: &mut jni::JNIEnv, operation: F)
where
    F: FnOnce() -> WasmtimeResult<()>,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(operation));
    match result {
        Ok(Ok(())) => {
            clear_last_error();
        }
        Ok(Err(error)) => {
            throw_jni_exception(env, &error);
        }
        Err(panic_info) => {
            // Convert panic to a WasmtimeError and throw as Java exception
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
        }
    }
}

#[cfg(feature = "jni-bindings")]
/// Execute operation with JNI exception throwing, returning JNI object as raw jobject
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn jni_try_object<F>(env: &mut jni::JNIEnv, operation: F) -> jni::sys::jobject
where
    F: FnOnce(&mut jni::JNIEnv) -> WasmtimeResult<jni::objects::JObject<'static>>,
{
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| operation(env)));
    match result {
        Ok(Ok(value)) => {
            clear_last_error();
            value.as_raw()
        }
        Ok(Err(error)) => {
            throw_jni_exception(env, &error);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            throw_jni_exception(env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Convert error to error code for FFI interfaces
pub fn error_to_code(error: &WasmtimeError) -> i32 {
    error.to_error_code() as i32
}

#[cfg(feature = "jni-bindings")]
/// Get bytes from a JNI byte array
///
/// Converts a Java byte array (JByteArray) to a Rust Vec<u8>.
/// Returns an error if the array cannot be read.
pub fn get_byte_array_bytes(
    env: &jni::JNIEnv,
    array: &jni::objects::JByteArray,
) -> Result<Vec<u8>, WasmtimeError> {
    // Get the length of the array
    let len = env.get_array_length(array).map_err(|e| {
        log::error!("Failed to get byte array length: {}", e);
        WasmtimeError::InvalidParameter {
            message: format!("Failed to get byte array length: {}", e),
        }
    })?;

    if len < 0 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Byte array has negative length".to_string(),
        });
    }

    let len = len as usize;

    // Create a buffer to hold the bytes
    let mut buffer: Vec<i8> = vec![0i8; len];

    // Copy the bytes from Java to Rust
    env.get_byte_array_region(array, 0, &mut buffer)
        .map_err(|e| {
            log::error!("Failed to get byte array region: {}", e);
            WasmtimeError::InvalidParameter {
                message: format!("Failed to get byte array region: {}", e),
            }
        })?;

    // Convert i8 to u8
    let result: Vec<u8> = buffer.into_iter().map(|b| b as u8).collect();

    Ok(result)
}
