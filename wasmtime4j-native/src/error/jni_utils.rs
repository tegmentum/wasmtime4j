//! JNI error conversion utilities
//!
//! This module provides utilities for converting Wasmtime errors to JNI exceptions
//! and handling JNI-specific error scenarios.

use super::ffi_utils::clear_last_error;
use super::{ErrorCode, WasmtimeError, WasmtimeResult};
use std::os::raw::c_void;

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
        WasmtimeError::WasiExit { .. } => "ai/tegmentum/wasmtime4j/exception/I32ExitException",
        WasmtimeError::Security { .. } => "ai/tegmentum/wasmtime4j/exception/WasmSecurityException",
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
        WasmtimeError::Runtime {
            ref message, ..
        } if message.contains("[trap_code:") => {
            // Runtime error with embedded trap code — throw as TrapException
            throw_trap_exception(env, message)
        }
        WasmtimeError::WasiExit { exit_code } => {
            // I32ExitException requires int exit code constructor
            throw_i32_exit_exception(env, *exit_code)
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
/// Map a numeric trap code to the Java `TrapType` enum field name.
fn trap_code_to_field_name(code: i32) -> &'static str {
    use crate::panama::trap::trap_codes;
    match code {
        trap_codes::STACK_OVERFLOW => "STACK_OVERFLOW",
        trap_codes::MEMORY_OUT_OF_BOUNDS => "MEMORY_OUT_OF_BOUNDS",
        trap_codes::HEAP_MISALIGNED => "HEAP_MISALIGNED",
        trap_codes::TABLE_OUT_OF_BOUNDS => "TABLE_OUT_OF_BOUNDS",
        trap_codes::INDIRECT_CALL_TO_NULL => "INDIRECT_CALL_TO_NULL",
        trap_codes::BAD_SIGNATURE => "BAD_SIGNATURE",
        trap_codes::INTEGER_OVERFLOW => "INTEGER_OVERFLOW",
        trap_codes::INTEGER_DIVISION_BY_ZERO => "INTEGER_DIVISION_BY_ZERO",
        trap_codes::BAD_CONVERSION_TO_INTEGER => "BAD_CONVERSION_TO_INTEGER",
        trap_codes::UNREACHABLE_CODE_REACHED => "UNREACHABLE_CODE_REACHED",
        trap_codes::INTERRUPT => "INTERRUPT",
        trap_codes::ALWAYS_TRAP_ADAPTER => "ALWAYS_TRAP_ADAPTER",
        trap_codes::OUT_OF_FUEL => "OUT_OF_FUEL",
        trap_codes::ATOMIC_WAIT_NON_SHARED_MEMORY => "ATOMIC_WAIT_NON_SHARED_MEMORY",
        trap_codes::NULL_REFERENCE => "NULL_REFERENCE",
        trap_codes::ARRAY_OUT_OF_BOUNDS => "ARRAY_OUT_OF_BOUNDS",
        trap_codes::ALLOCATION_TOO_LARGE => "ALLOCATION_TOO_LARGE",
        trap_codes::CAST_FAILURE => "CAST_FAILURE",
        trap_codes::CANNOT_ENTER_COMPONENT => "CANNOT_ENTER_COMPONENT",
        trap_codes::NO_ASYNC_RESULT => "NO_ASYNC_RESULT",
        trap_codes::UNHANDLED_TAG => "UNHANDLED_TAG",
        trap_codes::CONTINUATION_ALREADY_CONSUMED => "CONTINUATION_ALREADY_CONSUMED",
        trap_codes::DISABLED_OPCODE => "DISABLED_OPCODE",
        trap_codes::ASYNC_DEADLOCK => "ASYNC_DEADLOCK",
        trap_codes::CANNOT_LEAVE_COMPONENT => "CANNOT_LEAVE_COMPONENT",
        trap_codes::CANNOT_BLOCK_SYNC_TASK => "CANNOT_BLOCK_SYNC_TASK",
        trap_codes::INVALID_CHAR => "INVALID_CHAR",
        trap_codes::STRING_OUT_OF_BOUNDS => "STRING_OUT_OF_BOUNDS",
        trap_codes::LIST_OUT_OF_BOUNDS => "LIST_OUT_OF_BOUNDS",
        trap_codes::INVALID_DISCRIMINANT => "INVALID_DISCRIMINANT",
        trap_codes::UNALIGNED_POINTER => "UNALIGNED_POINTER",
        trap_codes::DEBUG_ASSERT_STRING_ENCODING_FINISHED => {
            "DEBUG_ASSERT_STRING_ENCODING_FINISHED"
        }
        trap_codes::DEBUG_ASSERT_EQUAL_CODE_UNITS => "DEBUG_ASSERT_EQUAL_CODE_UNITS",
        trap_codes::DEBUG_ASSERT_MAY_ENTER_UNSET => "DEBUG_ASSERT_MAY_ENTER_UNSET",
        trap_codes::DEBUG_ASSERT_POINTER_ALIGNED => "DEBUG_ASSERT_POINTER_ALIGNED",
        trap_codes::DEBUG_ASSERT_UPPER_BITS_UNSET => "DEBUG_ASSERT_UPPER_BITS_UNSET",
        _ => "UNKNOWN",
    }
}

#[cfg(feature = "jni-bindings")]
/// Extract a numeric trap code from a `[trap_code:N]` prefix in the message.
fn extract_trap_code_from_message(message: &str) -> Option<i32> {
    const PREFIX: &str = "[trap_code:";
    if let Some(start) = message.find(PREFIX) {
        let after = &message[start + PREFIX.len()..];
        if let Some(end) = after.find(']') {
            if let Ok(code) = after[..end].parse::<i32>() {
                return Some(code);
            }
        }
    }
    None
}

#[cfg(feature = "jni-bindings")]
/// Helper to throw TrapException with the correct TrapType resolved from the message.
///
/// Parses the `[trap_code:N]` prefix to determine the exact TrapType enum variant.
/// Falls back to UNKNOWN if no prefix is present.
fn throw_trap_exception(env: &mut jni::JNIEnv, message: &str) -> Result<(), jni::errors::Error> {
    use jni::objects::{JThrowable, JValue};

    // Resolve TrapType field name from [trap_code:N] prefix
    let field_name = extract_trap_code_from_message(message)
        .map(trap_code_to_field_name)
        .unwrap_or("UNKNOWN");

    // Get TrapType enum value
    let error_type_class =
        env.find_class("ai/tegmentum/wasmtime4j/exception/TrapException$TrapType")?;
    let trap_field = env.get_static_field(
        error_type_class,
        field_name,
        "Lai/tegmentum/wasmtime4j/exception/TrapException$TrapType;",
    )?;

    // Create TrapException(TrapType, String)
    let exception_class = env.find_class("ai/tegmentum/wasmtime4j/exception/TrapException")?;
    let message_jstring = env.new_string(message)?;

    let exception_obj = env.new_object(
        exception_class,
        "(Lai/tegmentum/wasmtime4j/exception/TrapException$TrapType;Ljava/lang/String;)V",
        &[(&trap_field).into(), JValue::Object(&message_jstring)],
    )?;

    env.throw(JThrowable::from(exception_obj))
}

#[cfg(feature = "jni-bindings")]
/// Helper to throw I32ExitException with exit code
fn throw_i32_exit_exception(
    env: &mut jni::JNIEnv,
    exit_code: i32,
) -> Result<(), jni::errors::Error> {
    use jni::objects::{JThrowable, JValue};

    // Create I32ExitException(int exitCode)
    let exception_class = env.find_class("ai/tegmentum/wasmtime4j/exception/I32ExitException")?;

    let exception_obj = env.new_object(exception_class, "(I)V", &[JValue::Int(exit_code)])?;

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
