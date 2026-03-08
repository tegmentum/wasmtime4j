//! Panama FFI bindings for WAST execution support
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to execute WAST (WebAssembly Test) files using Wasmtime's native WAST parser.
//!
//! Results are returned as JSON strings to avoid complex C struct marshalling.
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.

use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use std::panic::AssertUnwindSafe;

use crate::error::ffi_utils::ffi_try_code;
use crate::wast_runner::{execute_wast_buffer, execute_wast_file};

/// Execute a WAST file from disk and return results as JSON.
///
/// # Parameters
/// - `file_path`: Null-terminated C string with the path to the WAST file
/// - `result_json`: Output pointer for the JSON result string (caller must free with
///   `wasmtime4j_panama_wast_free_result`)
///
/// # Returns
/// 0 on success, non-zero on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wast_execute_file(
    file_path: *const c_char,
    result_json: *mut *mut c_char,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if file_path.is_null() || result_json.is_null() {
            return Err(crate::error::WasmtimeError::invalid_parameter(
                "file_path and result_json must not be null",
            ));
        }

        let file_path_str = unsafe { CStr::from_ptr(file_path) }.to_str().map_err(|e| {
            crate::error::WasmtimeError::invalid_parameter(format!(
                "Invalid UTF-8 in file path: {}",
                e
            ))
        })?;

        let result = execute_wast_file(file_path_str).map_err(|e| {
            crate::error::WasmtimeError::from_string(format!("WAST execution failed: {:#}", e))
        })?;

        let json = serde_json::to_string(&result).map_err(|e| {
            crate::error::WasmtimeError::from_string(format!(
                "Failed to serialize WAST result: {}",
                e
            ))
        })?;

        let c_json = CString::new(json).map_err(|e| {
            crate::error::WasmtimeError::from_string(format!(
                "Failed to create C string from JSON: {}",
                e
            ))
        })?;

        unsafe {
            *result_json = c_json.into_raw();
        }

        Ok(())
    }))
}

/// Execute WAST content from a byte buffer and return results as JSON.
///
/// # Parameters
/// - `filename`: Null-terminated C string with the filename for error reporting
/// - `content`: Pointer to the WAST content bytes
/// - `content_len`: Length of the content buffer
/// - `result_json`: Output pointer for the JSON result string (caller must free with
///   `wasmtime4j_panama_wast_free_result`)
///
/// # Returns
/// 0 on success, non-zero on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wast_execute_buffer(
    filename: *const c_char,
    content: *const u8,
    content_len: usize,
    result_json: *mut *mut c_char,
) -> i32 {
    ffi_try_code(AssertUnwindSafe(|| {
        if filename.is_null() || content.is_null() || result_json.is_null() {
            return Err(crate::error::WasmtimeError::invalid_parameter(
                "filename, content, and result_json must not be null",
            ));
        }

        if content_len == 0 {
            return Err(crate::error::WasmtimeError::invalid_parameter(
                "content_len must be greater than 0",
            ));
        }

        let filename_str = unsafe { CStr::from_ptr(filename) }.to_str().map_err(|e| {
            crate::error::WasmtimeError::invalid_parameter(format!(
                "Invalid UTF-8 in filename: {}",
                e
            ))
        })?;

        let content_slice = unsafe { std::slice::from_raw_parts(content, content_len) };

        let result = execute_wast_buffer(filename_str, content_slice).map_err(|e| {
            crate::error::WasmtimeError::from_string(format!("WAST execution failed: {:#}", e))
        })?;

        let json = serde_json::to_string(&result).map_err(|e| {
            crate::error::WasmtimeError::from_string(format!(
                "Failed to serialize WAST result: {}",
                e
            ))
        })?;

        let c_json = CString::new(json).map_err(|e| {
            crate::error::WasmtimeError::from_string(format!(
                "Failed to create C string from JSON: {}",
                e
            ))
        })?;

        unsafe {
            *result_json = c_json.into_raw();
        }

        Ok(())
    }))
}

/// Free a JSON result string allocated by the WAST execution functions.
///
/// # Parameters
/// - `result_json`: Pointer to the JSON string to free (may be null, which is a no-op)
///
/// # Safety
/// The pointer must have been returned by `wasmtime4j_panama_wast_execute_file` or
/// `wasmtime4j_panama_wast_execute_buffer`.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wast_free_result(result_json: *mut c_char) {
    if !result_json.is_null() {
        unsafe {
            drop(CString::from_raw(result_json));
        }
    }
}
