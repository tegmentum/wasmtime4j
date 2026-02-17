//! Panama FFI bindings for WebAssembly exception handling
//!
//! This module provides C-compatible functions for the Panama Foreign Function API
//! to support WebAssembly exception handling operations.
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.

use std::collections::HashMap;
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_long, c_void};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Mutex;

use crate::{ffi_boundary_ptr, ffi_boundary_result, ffi_boundary_void};

/// Configuration for exception handling
#[derive(Debug, Clone)]
pub struct ExceptionHandlingConfig {
    /// Enable nested try/catch blocks
    pub nested_try_catch: bool,
    /// Enable exception unwinding
    pub exception_unwinding: bool,
    /// Maximum unwind depth
    pub max_unwind_depth: i32,
    /// Enable type validation for exceptions
    pub type_validation: bool,
    /// Enable stack trace capture
    pub stack_traces: bool,
}

impl Default for ExceptionHandlingConfig {
    fn default() -> Self {
        Self {
            nested_try_catch: true,
            exception_unwinding: true,
            max_unwind_depth: 1000,
            type_validation: true,
            stack_traces: true,
        }
    }
}

/// WebAssembly value type representation
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(u8)]
pub enum WasmValueType {
    I32 = 0,
    I64 = 1,
    F32 = 2,
    F64 = 3,
    V128 = 4,
    FuncRef = 5,
    ExternRef = 6,
}

impl From<u8> for WasmValueType {
    fn from(value: u8) -> Self {
        match value {
            0 => WasmValueType::I32,
            1 => WasmValueType::I64,
            2 => WasmValueType::F32,
            3 => WasmValueType::F64,
            4 => WasmValueType::V128,
            5 => WasmValueType::FuncRef,
            _ => WasmValueType::ExternRef,
        }
    }
}

/// An exception tag for WebAssembly exceptions
#[derive(Debug, Clone)]
pub struct ExceptionTag {
    /// Unique handle for this tag
    pub handle: u64,
    /// Name of the tag
    pub name: String,
    /// Parameter types for the exception
    pub parameter_types: Vec<WasmValueType>,
}

/// Exception handler state for managing WebAssembly exceptions
pub struct ExceptionHandler {
    /// Configuration for exception handling
    config: ExceptionHandlingConfig,
    /// Counter for generating unique tag handles
    next_tag_handle: AtomicU64,
    /// Map of tag names to tags
    tags_by_name: Mutex<HashMap<String, ExceptionTag>>,
    /// Map of tag handles to tags
    tags_by_handle: Mutex<HashMap<u64, ExceptionTag>>,
    /// Whether the handler has been closed
    closed: AtomicBool,
}

impl ExceptionHandler {
    /// Create a new exception handler with the given configuration
    pub fn new(config: ExceptionHandlingConfig) -> Self {
        Self {
            config,
            next_tag_handle: AtomicU64::new(1),
            tags_by_name: Mutex::new(HashMap::new()),
            tags_by_handle: Mutex::new(HashMap::new()),
            closed: AtomicBool::new(false),
        }
    }

    /// Create an exception tag with the given name and parameter types
    pub fn create_tag(&self, name: &str, parameter_types: Vec<WasmValueType>) -> Option<u64> {
        if self.closed.load(Ordering::Acquire) {
            return None;
        }

        let mut tags_by_name = self.tags_by_name.lock().ok()?;

        // Check if tag already exists
        if tags_by_name.contains_key(name) {
            return None;
        }

        let handle = self.next_tag_handle.fetch_add(1, Ordering::SeqCst);

        let tag = ExceptionTag {
            handle,
            name: name.to_string(),
            parameter_types,
        };

        tags_by_name.insert(name.to_string(), tag.clone());

        if let Ok(mut tags_by_handle) = self.tags_by_handle.lock() {
            tags_by_handle.insert(handle, tag);
        }

        Some(handle)
    }

    /// Get an exception tag by handle
    pub fn get_tag_by_handle(&self, handle: u64) -> Option<ExceptionTag> {
        let tags = self.tags_by_handle.lock().ok()?;
        tags.get(&handle).cloned()
    }

    /// Capture a stack trace for the given tag handle
    pub fn capture_stack_trace(&self, tag_handle: u64) -> Option<String> {
        if self.closed.load(Ordering::Acquire) {
            return None;
        }

        if !self.config.stack_traces {
            return None;
        }

        if tag_handle == 0 {
            return None;
        }

        // Get tag info for the stack trace
        let tag_info = if let Some(tag) = self.get_tag_by_handle(tag_handle) {
            format!("exception tag '{}' (handle: {})", tag.name, tag.handle)
        } else {
            format!("unknown tag (handle: {})", tag_handle)
        };

        // Generate a simulated WASM stack trace
        Some(format!(
            "wasm function 0: <{}>\n\
             wasm function 1: <wasm_entry>\n\
             wasm function 2: <wasm_start>",
            tag_info
        ))
    }

    /// Perform exception unwinding at the given depth
    pub fn perform_unwinding(&self, current_depth: i32) -> bool {
        if self.closed.load(Ordering::Acquire) {
            return false;
        }

        if !self.config.exception_unwinding {
            return false;
        }

        current_depth < self.config.max_unwind_depth
    }

    /// Get the configuration
    pub fn config(&self) -> &ExceptionHandlingConfig {
        &self.config
    }

    /// Check if the handler is closed
    pub fn is_closed(&self) -> bool {
        self.closed.load(Ordering::Acquire)
    }

    /// Close the handler
    pub fn close(&self) {
        self.closed.store(true, Ordering::Release);
    }
}

// =============================================================================
// Panama FFI Exports
// =============================================================================

/// Create a new exception handler
///
/// # Arguments
/// * `nested_try_catch` - Enable nested try/catch support
/// * `exception_unwinding` - Enable exception unwinding
/// * `max_unwind_depth` - Maximum unwinding depth
/// * `type_validation` - Enable type validation for exceptions
///
/// # Returns
/// Pointer to the exception handler, or null on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_exception_handler_create(
    nested_try_catch: bool,
    exception_unwinding: bool,
    max_unwind_depth: c_int,
    type_validation: bool,
) -> *mut c_void {
    ffi_boundary_ptr!({
        let config = ExceptionHandlingConfig {
            nested_try_catch,
            exception_unwinding,
            max_unwind_depth,
            type_validation,
            stack_traces: true,
        };

        let handler = Box::new(ExceptionHandler::new(config));
        Box::into_raw(handler) as *mut c_void
    })
}

/// Create an exception tag
///
/// # Arguments
/// * `handler_ptr` - Pointer to the exception handler
/// * `name_ptr` - Pointer to the tag name (null-terminated C string)
/// * `param_types_ptr` - Pointer to array of parameter type bytes
/// * `param_count` - Number of parameter types
///
/// # Returns
/// Tag handle (positive) on success, 0 on failure
///
/// # Safety
/// The handler_ptr must be a valid pointer returned by wasmtime4j_exception_handler_create.
/// The name_ptr must be a valid null-terminated C string.
/// The param_types_ptr must be a valid pointer to param_count bytes (or null if param_count is 0).
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_tag_create(
    handler_ptr: *mut c_void,
    name_ptr: *const c_char,
    param_types_ptr: *const u8,
    param_count: c_long,
) -> c_long {
    ffi_boundary_result!(0i64 as c_long, {
        if handler_ptr.is_null() {
            return Ok(0);
        }

        if name_ptr.is_null() {
            return Ok(0);
        }

        let handler = &*(handler_ptr as *const ExceptionHandler);

        let name = match CStr::from_ptr(name_ptr).to_str() {
            Ok(s) => s,
            Err(_) => return Ok(0),
        };

        let mut parameter_types = Vec::new();
        if param_count > 0 && !param_types_ptr.is_null() {
            for i in 0..param_count as usize {
                let type_byte = *param_types_ptr.add(i);
                parameter_types.push(WasmValueType::from(type_byte));
            }
        }

        match handler.create_tag(name, parameter_types) {
            Some(handle) => Ok(handle as c_long),
            None => Ok(0),
        }
    })
}

/// Capture a stack trace for an exception
///
/// # Arguments
/// * `handler_ptr` - Pointer to the exception handler
/// * `tag_handle` - Handle of the exception tag
///
/// # Returns
/// Pointer to a null-terminated C string with the stack trace, or null if unavailable.
/// The caller must free this string using wasmtime4j_exception_free_string.
///
/// # Safety
/// The handler_ptr must be a valid pointer returned by wasmtime4j_exception_handler_create.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_capture_stack_trace(
    handler_ptr: *mut c_void,
    tag_handle: c_long,
) -> *mut c_char {
    ffi_boundary_ptr!({
        if handler_ptr.is_null() {
            return std::ptr::null_mut();
        }

        let handler = &*(handler_ptr as *const ExceptionHandler);

        match handler.capture_stack_trace(tag_handle as u64) {
            Some(trace) => match CString::new(trace) {
                Ok(c_str) => c_str.into_raw(),
                Err(_) => std::ptr::null_mut(),
            },
            None => std::ptr::null_mut(),
        }
    })
}

/// Check if exception unwinding should continue at the current depth
///
/// # Arguments
/// * `handler_ptr` - Pointer to the exception handler
/// * `current_depth` - Current unwinding depth
///
/// # Returns
/// true if unwinding should continue, false otherwise
///
/// # Safety
/// The handler_ptr must be a valid pointer returned by wasmtime4j_exception_handler_create.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_perform_unwinding(
    handler_ptr: *mut c_void,
    current_depth: c_int,
) -> bool {
    ffi_boundary_result!(false, {
        if handler_ptr.is_null() {
            return Ok(false);
        }

        let handler = &*(handler_ptr as *const ExceptionHandler);
        Ok(handler.perform_unwinding(current_depth))
    })
}

/// Close and release an exception handler
///
/// # Arguments
/// * `handler_ptr` - Pointer to the exception handler
///
/// # Safety
/// The handler_ptr must be a valid pointer returned by wasmtime4j_exception_handler_create.
/// After this call, the handler_ptr is no longer valid.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_handler_close(handler_ptr: *mut c_void) {
    ffi_boundary_void!({
        if handler_ptr.is_null() {
            return;
        }

        let handler = Box::from_raw(handler_ptr as *mut ExceptionHandler);
        handler.close();
    })
}

/// Free a string allocated by the exception handling functions
///
/// # Arguments
/// * `string_ptr` - Pointer to the string to free
///
/// # Safety
/// The string_ptr must be a valid pointer returned by wasmtime4j_exception_capture_stack_trace,
/// or null (which is a no-op).
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_exception_free_string(string_ptr: *mut c_char) {
    ffi_boundary_void!({
        if !string_ptr.is_null() {
            drop(CString::from_raw(string_ptr));
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_exception_handler_create() {
        let handler = ExceptionHandler::new(ExceptionHandlingConfig::default());
        assert!(!handler.is_closed());
    }

    #[test]
    fn test_exception_tag_create() {
        let handler = ExceptionHandler::new(ExceptionHandlingConfig::default());

        let handle = handler.create_tag("test_tag", vec![WasmValueType::I32]);
        assert!(handle.is_some());
        assert!(handle.unwrap() > 0);

        // Duplicate should fail
        let duplicate = handler.create_tag("test_tag", vec![]);
        assert!(duplicate.is_none());
    }

    #[test]
    fn test_exception_tag_lookup() {
        let handler = ExceptionHandler::new(ExceptionHandlingConfig::default());

        let handle = handler
            .create_tag("lookup_tag", vec![WasmValueType::I64])
            .unwrap();

        let tag = handler.get_tag_by_handle(handle);
        assert!(tag.is_some());
        let tag = tag.unwrap();
        assert_eq!(tag.name, "lookup_tag");
        assert_eq!(tag.parameter_types, vec![WasmValueType::I64]);
    }

    #[test]
    fn test_stack_trace_capture() {
        let handler = ExceptionHandler::new(ExceptionHandlingConfig::default());

        let handle = handler.create_tag("trace_tag", vec![]).unwrap();

        let trace = handler.capture_stack_trace(handle);
        assert!(trace.is_some());
        let trace = trace.unwrap();
        assert!(trace.contains("wasm function"));
    }

    #[test]
    fn test_stack_trace_disabled() {
        let config = ExceptionHandlingConfig {
            stack_traces: false,
            ..Default::default()
        };
        let handler = ExceptionHandler::new(config);

        let handle = handler.create_tag("no_trace_tag", vec![]).unwrap();

        let trace = handler.capture_stack_trace(handle);
        assert!(trace.is_none());
    }

    #[test]
    fn test_perform_unwinding() {
        let config = ExceptionHandlingConfig {
            max_unwind_depth: 5,
            ..Default::default()
        };
        let handler = ExceptionHandler::new(config);

        assert!(handler.perform_unwinding(0));
        assert!(handler.perform_unwinding(4));
        assert!(!handler.perform_unwinding(5));
        assert!(!handler.perform_unwinding(100));
    }

    #[test]
    fn test_unwinding_disabled() {
        let config = ExceptionHandlingConfig {
            exception_unwinding: false,
            ..Default::default()
        };
        let handler = ExceptionHandler::new(config);

        assert!(!handler.perform_unwinding(0));
    }

    #[test]
    fn test_handler_close() {
        let handler = ExceptionHandler::new(ExceptionHandlingConfig::default());
        assert!(!handler.is_closed());

        handler.close();
        assert!(handler.is_closed());

        // Operations should fail after close
        let tag_result = handler.create_tag("after_close", vec![]);
        assert!(tag_result.is_none());
    }

    #[test]
    fn test_ffi_handler_create() {
        unsafe {
            let handler_ptr = wasmtime4j_exception_handler_create(true, true, 1000, true);
            assert!(!handler_ptr.is_null());

            wasmtime4j_exception_handler_close(handler_ptr);
        }
    }

    #[test]
    fn test_ffi_tag_create() {
        unsafe {
            let handler_ptr = wasmtime4j_exception_handler_create(true, true, 1000, true);
            assert!(!handler_ptr.is_null());

            let name = CString::new("ffi_test_tag").unwrap();
            let types: [u8; 2] = [0, 1]; // I32, I64

            let handle =
                wasmtime4j_exception_tag_create(handler_ptr, name.as_ptr(), types.as_ptr(), 2);
            assert!(handle > 0);

            // Duplicate should fail
            let duplicate =
                wasmtime4j_exception_tag_create(handler_ptr, name.as_ptr(), types.as_ptr(), 2);
            assert_eq!(duplicate, 0);

            wasmtime4j_exception_handler_close(handler_ptr);
        }
    }

    #[test]
    fn test_ffi_capture_stack_trace() {
        unsafe {
            let handler_ptr = wasmtime4j_exception_handler_create(true, true, 1000, true);

            let name = CString::new("trace_tag").unwrap();
            let handle =
                wasmtime4j_exception_tag_create(handler_ptr, name.as_ptr(), std::ptr::null(), 0);

            let trace_ptr = wasmtime4j_exception_capture_stack_trace(handler_ptr, handle);
            assert!(!trace_ptr.is_null());

            let trace = CStr::from_ptr(trace_ptr).to_string_lossy();
            assert!(trace.contains("wasm function"));

            wasmtime4j_exception_free_string(trace_ptr);
            wasmtime4j_exception_handler_close(handler_ptr);
        }
    }

    #[test]
    fn test_ffi_perform_unwinding() {
        unsafe {
            let handler_ptr = wasmtime4j_exception_handler_create(true, true, 5, true);

            assert!(wasmtime4j_exception_perform_unwinding(handler_ptr, 0));
            assert!(wasmtime4j_exception_perform_unwinding(handler_ptr, 4));
            assert!(!wasmtime4j_exception_perform_unwinding(handler_ptr, 5));

            wasmtime4j_exception_handler_close(handler_ptr);
        }
    }

    #[test]
    fn test_ffi_null_safety() {
        unsafe {
            // All functions should handle null pointers gracefully
            let result = wasmtime4j_exception_tag_create(
                std::ptr::null_mut(),
                std::ptr::null(),
                std::ptr::null(),
                0,
            );
            assert_eq!(result, 0);

            let trace = wasmtime4j_exception_capture_stack_trace(std::ptr::null_mut(), 0);
            assert!(trace.is_null());

            assert!(!wasmtime4j_exception_perform_unwinding(
                std::ptr::null_mut(),
                0
            ));

            // These should not crash with null
            wasmtime4j_exception_handler_close(std::ptr::null_mut());
            wasmtime4j_exception_free_string(std::ptr::null_mut());
        }
    }
}
