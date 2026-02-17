//! Shared FFI utilities for both JNI and Panama interfaces
//!
//! This module provides common functionality that eliminates code duplication
//! between JNI and Panama FFI implementations while maintaining thread safety
//! and defensive programming practices.

use super::{ErrorCode, WasmtimeError, WasmtimeResult};
use std::ffi::CString;
use std::os::raw::{c_char, c_void};

// Store last error for FFI error handling
thread_local! {
    static LAST_ERROR: std::cell::RefCell<Option<WasmtimeError>> = std::cell::RefCell::new(None);
}

/// Set last error for FFI retrieval with defensive error handling
pub fn set_last_error(error: WasmtimeError) {
    // Use panic-safe error setting to prevent issues in multi-threaded contexts
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR.with(|e| {
            match e.try_borrow_mut() {
                Ok(mut error_ref) => {
                    *error_ref = Some(error);
                }
                Err(_) => {
                    // If borrow_mut fails (should be rare), log the error
                    // but don't propagate panic to prevent JVM crashes
                    log::warn!("Failed to set last error due to borrow check failure");
                }
            }
        });
    }));

    if result.is_err() {
        log::error!("Panic occurred while setting last error - prevented JVM crash");
    }
}

/// Get last error message as C string with defensive error handling
/// Returns null if no error or if error retrieval fails
pub fn get_last_error_message() -> *mut c_char {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR.with(|e| match e.try_borrow() {
            Ok(error_ref) => match error_ref.as_ref() {
                Some(error) => error.to_c_string().into_raw(),
                None => std::ptr::null_mut(),
            },
            Err(_) => {
                log::warn!("Failed to get last error due to borrow check failure");
                std::ptr::null_mut()
            }
        })
    }));

    match result {
        Ok(ptr) => ptr,
        Err(_) => {
            log::error!("Panic occurred while getting last error - prevented JVM crash");
            std::ptr::null_mut()
        }
    }
}

/// Clear last error with defensive error handling
pub fn clear_last_error() {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR.with(|e| match e.try_borrow_mut() {
            Ok(mut error_ref) => {
                *error_ref = None;
            }
            Err(_) => {
                log::warn!("Failed to clear last error due to borrow check failure");
            }
        });
    }));

    if result.is_err() {
        log::error!("Panic occurred while clearing last error - prevented JVM crash");
    }
}

/// Free error message C string with defensive error handling
///
/// # Safety
/// The caller must ensure that `message` was originally created by `get_last_error_message`.
pub unsafe fn free_error_message(message: *mut c_char) {
    if message.is_null() {
        return;
    }

    // Use panic-safe memory deallocation to prevent JVM crashes
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        drop(CString::from_raw(message));
    }));

    if result.is_err() {
        log::error!(
            "Panic occurred while freeing error message at {:p} - potential memory leak",
            message
        );
    }
}

/// Check if there are any pending errors in thread-local storage
/// This is useful for debugging thread safety issues
pub fn has_pending_error() -> bool {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR.with(|e| match e.try_borrow() {
            Ok(error_ref) => error_ref.is_some(),
            Err(_) => {
                log::warn!("Failed to check pending error due to borrow check failure");
                false
            }
        })
    }));

    match result {
        Ok(has_error) => has_error,
        Err(_) => {
            log::error!("Panic occurred while checking pending error - prevented JVM crash");
            false
        }
    }
}

/// Get error statistics for debugging thread safety issues
pub fn get_error_stats() -> (bool, String) {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR.with(|e| match e.try_borrow() {
            Ok(error_ref) => match error_ref.as_ref() {
                Some(error) => (
                    true,
                    format!("Error: {} (Code: {:?})", error, error.to_error_code()),
                ),
                None => (false, "No error".to_string()),
            },
            Err(_) => (
                false,
                "Borrow check failed - potential threading issue".to_string(),
            ),
        })
    }));

    match result {
        Ok((has_error, message)) => (has_error, message),
        Err(_) => (false, "Panic occurred during error stats check".to_string()),
    }
}

/// Error aggregation utility for collecting multiple errors
pub struct ErrorCollector {
    errors: Vec<WasmtimeError>,
    operation_name: Option<String>,
}

impl ErrorCollector {
    /// Create a new error collector
    pub fn new() -> Self {
        Self {
            errors: Vec::new(),
            operation_name: None,
        }
    }

    /// Create a new error collector with operation name
    pub fn with_operation(operation: String) -> Self {
        Self {
            errors: Vec::new(),
            operation_name: Some(operation),
        }
    }

    /// Add an error to the collection
    pub fn add_error(&mut self, error: WasmtimeError) {
        self.errors.push(error);
    }

    /// Add multiple errors to the collection
    pub fn add_errors<I>(&mut self, errors: I)
    where
        I: IntoIterator<Item = WasmtimeError>,
    {
        self.errors.extend(errors);
    }

    /// Add a result to the collection, collecting the error if it's an Err
    pub fn add_result<T>(&mut self, result: WasmtimeResult<T>) -> Option<T> {
        match result {
            Ok(value) => Some(value),
            Err(error) => {
                self.add_error(error);
                None
            }
        }
    }

    /// Check if any errors have been collected
    pub fn has_errors(&self) -> bool {
        !self.errors.is_empty()
    }

    /// Get the number of collected errors
    pub fn error_count(&self) -> usize {
        self.errors.len()
    }

    /// Convert to result, returning aggregated error if any errors were collected
    pub fn into_result(self) -> WasmtimeResult<()> {
        if self.errors.is_empty() {
            Ok(())
        } else {
            let summary = match &self.operation_name {
                Some(op) => format!("Operation '{}' failed", op),
                None => "Multiple operations failed".to_string(),
            };

            Err(WasmtimeError::Multiple {
                summary,
                errors: self.errors,
            })
        }
    }

    /// Convert to result with value, returning aggregated error if any errors were collected
    pub fn into_result_with_value<T>(self, value: T) -> WasmtimeResult<T> {
        if self.errors.is_empty() {
            Ok(value)
        } else {
            let summary = match &self.operation_name {
                Some(op) => format!("Operation '{}' failed", op),
                None => "Multiple operations failed".to_string(),
            };

            Err(WasmtimeError::Multiple {
                summary,
                errors: self.errors,
            })
        }
    }

    /// Get all collected errors
    pub fn get_errors(&self) -> &[WasmtimeError] {
        &self.errors
    }
}

impl Default for ErrorCollector {
    fn default() -> Self {
        Self::new()
    }
}

/// Execute multiple operations and collect any errors
pub fn try_all<F, T>(operations: Vec<F>) -> WasmtimeResult<Vec<T>>
where
    F: FnOnce() -> WasmtimeResult<T>,
{
    let mut collector = ErrorCollector::new();
    let mut results = Vec::new();

    for operation in operations {
        match operation() {
            Ok(result) => results.push(result),
            Err(error) => collector.add_error(error),
        }
    }

    if collector.has_errors() {
        Err(WasmtimeError::multiple(collector.errors))
    } else {
        Ok(results)
    }
}

/// Execute multiple operations and collect any errors, continuing even if some fail
pub fn try_all_continue<F, T>(operations: Vec<F>) -> (Vec<T>, Option<WasmtimeError>)
where
    F: FnOnce() -> WasmtimeResult<T>,
{
    let mut collector = ErrorCollector::new();
    let mut results = Vec::new();

    for operation in operations {
        if let Some(result) = collector.add_result(operation()) {
            results.push(result);
        }
    }

    let error = if collector.has_errors() {
        Some(WasmtimeError::multiple(collector.errors))
    } else {
        None
    };

    (results, error)
}

/// Execute operation with FFI error handling
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn ffi_try<F, T>(operation: F) -> (ErrorCode, T)
where
    F: FnOnce() -> WasmtimeResult<T> + std::panic::UnwindSafe,
    T: Default,
{
    let result = std::panic::catch_unwind(operation);
    match result {
        Ok(Ok(value)) => {
            clear_last_error();
            (ErrorCode::Success, value)
        }
        Ok(Err(error)) => {
            let code = error.to_error_code();
            set_last_error(error);
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
            log::error!("Native panic in FFI call: {}", panic_msg);
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            set_last_error(error);
            (ErrorCode::RuntimeError, T::default())
        }
    }
}

/// Execute operation with FFI error handling, returning pointer for success
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn ffi_try_ptr<F, T>(operation: F) -> *mut c_void
where
    F: FnOnce() -> WasmtimeResult<Box<T>> + std::panic::UnwindSafe,
{
    let result = std::panic::catch_unwind(operation);
    match result {
        Ok(Ok(boxed)) => {
            clear_last_error();
            Box::into_raw(boxed) as *mut c_void
        }
        Ok(Err(error)) => {
            set_last_error(error);
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
            log::error!("Native panic in FFI call: {}", panic_msg);
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            set_last_error(error);
            std::ptr::null_mut()
        }
    }
}

/// Execute operation with FFI error handling, returning Result<Box<T>> for pointer operations
pub fn ffi_try_ptr_result<F, T>(operation: F) -> WasmtimeResult<Box<T>>
where
    F: FnOnce() -> WasmtimeResult<T>,
{
    operation().map(Box::new)
}

/// Execute operation with FFI error handling, returning error code
/// Uses catch_unwind to prevent panics from crashing the JVM
pub fn ffi_try_code<F>(operation: F) -> i32
where
    F: FnOnce() -> WasmtimeResult<()> + std::panic::UnwindSafe,
{
    let result = std::panic::catch_unwind(operation);
    match result {
        Ok(Ok(())) => {
            clear_last_error();
            ErrorCode::Success as i32
        }
        Ok(Err(error)) => {
            let code = error.to_error_code();
            set_last_error(error);
            code as i32
        }
        Err(panic_info) => {
            // Convert panic to a WasmtimeError and store it
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            log::error!("Native panic in FFI call: {}", panic_msg);
            let error = WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            set_last_error(error);
            ErrorCode::RuntimeError as i32
        }
    }
}

/// Safe pointer dereference with validation
///
/// # Safety
/// The caller must ensure that `ptr` points to a valid value of type `T`.
pub unsafe fn deref_ptr<T>(ptr: *const c_void, name: &str) -> WasmtimeResult<&'static T> {
    if ptr.is_null() {
        return Err(WasmtimeError::invalid_parameter(format!(
            "{} pointer cannot be null",
            name
        )));
    }
    Ok(&*(ptr as *const T))
}

/// Safe mutable pointer dereference with validation
///
/// # Safety
/// The caller must ensure that `ptr` points to a valid value of type `T`.
pub unsafe fn deref_ptr_mut<T>(ptr: *mut c_void, name: &str) -> WasmtimeResult<&'static mut T> {
    if ptr.is_null() {
        return Err(WasmtimeError::invalid_parameter(format!(
            "{} pointer cannot be null",
            name
        )));
    }
    Ok(&mut *(ptr as *mut T))
}

/// Safe slice creation from raw pointer with validation
///
/// # Safety
/// The caller must ensure that `ptr` points to a valid array of `len` elements of type `T`.
pub unsafe fn slice_from_raw_parts<T>(
    ptr: *const T,
    len: usize,
    name: &str,
) -> WasmtimeResult<&'static [T]> {
    if ptr.is_null() {
        return Err(WasmtimeError::invalid_parameter(format!(
            "{} pointer cannot be null",
            name
        )));
    }
    if len == 0 {
        return Err(WasmtimeError::invalid_parameter(format!(
            "{} length cannot be zero",
            name
        )));
    }
    Ok(std::slice::from_raw_parts(ptr, len))
}

// Re-export resource destruction utilities from ffi_common for backwards compatibility
pub use crate::ffi_common::resource_destruction::{
    clear_destroyed_pointers, is_fake_pointer, safe_destroy, safe_destroy_no_fake_check,
    DESTROYED_POINTERS,
};

/// Safely destroy a boxed resource from raw pointer with double-free protection.
///
/// This is a backwards-compatible wrapper around `safe_destroy` that uses the
/// consolidated implementation in `ffi_common::resource_destruction`.
///
/// # Safety
/// The caller must ensure that `ptr` was originally created by `Box::into_raw`
/// for a value of type `T`.
pub unsafe fn destroy_resource<T>(ptr: *mut c_void, name: &str) {
    let _ = safe_destroy::<T>(ptr, name);
}

/// Comprehensive test cleanup function to reset global state between tests.
///
/// This function should be called at the end of each test (or test suite) to
/// prevent state accumulation that can cause heap corruption when running
/// many tests together.
///
/// # Returns
/// A tuple of (destroyed_pointers_cleared, memory_handles_cleared, store_handles_cleared)
///
/// # Safety
/// Only call this when all test resources have been properly dropped.
/// Calling while resources are still in use will cause validation errors.
#[cfg(test)]
pub fn cleanup_test_state() -> (usize, usize, usize) {
    // Clear destroyed pointers tracking
    let destroyed_count = clear_destroyed_pointers();

    // Clear memory and store handle registries
    let (memory_count, store_count) = match crate::memory::core::clear_handle_registries() {
        Ok(()) => {
            // Get counts before clearing (approximate)
            (0, 0) // We don't track individual counts in clear_handle_registries
        }
        Err(e) => {
            log::warn!("Failed to clear handle registries: {}", e);
            (0, 0)
        }
    };

    log::debug!(
        "Test cleanup: cleared {} destroyed pointers, {} memory handles, {} store handles",
        destroyed_count,
        memory_count,
        store_count
    );

    (destroyed_count, memory_count, store_count)
}

/// Guard struct that automatically cleans up test state when dropped.
///
/// Use this at the start of tests that create wasmtime resources to ensure
/// cleanup happens even if the test panics.
///
/// # Example
/// ```ignore
/// #[test]
/// fn my_test() {
///     let _guard = TestCleanupGuard::new();
///     // ... test code that creates Engine/Store/Module/Instance ...
///     // cleanup happens automatically when _guard is dropped
/// }
/// ```
#[cfg(test)]
pub struct TestCleanupGuard {
    test_name: &'static str,
}

#[cfg(test)]
impl TestCleanupGuard {
    /// Create a new test cleanup guard
    pub fn new(test_name: &'static str) -> Self {
        Self { test_name }
    }
}

#[cfg(test)]
impl Drop for TestCleanupGuard {
    fn drop(&mut self) {
        let (destroyed, _memory, _store) = cleanup_test_state();
        if destroyed > 0 {
            log::debug!(
                "TestCleanupGuard({}): cleaned up {} destroyed pointers",
                self.test_name,
                destroyed
            );
        }
    }
}

/// Convert C string to Rust string with validation
///
/// # Safety
/// The caller must ensure that `c_str` points to a valid null-terminated C string.
pub unsafe fn c_str_to_string(c_str: *const c_char, name: &str) -> WasmtimeResult<String> {
    if c_str.is_null() {
        return Err(WasmtimeError::invalid_parameter(format!(
            "{} C string cannot be null",
            name
        )));
    }

    std::ffi::CStr::from_ptr(c_str)
        .to_str()
        .map_err(|e| WasmtimeError::Type {
            message: format!("Failed to convert C string {}: {}", name, e),
        })
        .map(|s| s.to_string())
}

/// Convert C char pointer to Rust string
///
/// # Safety
/// The caller must ensure that `c_str` points to a valid null-terminated C string.
pub unsafe fn c_char_to_string(c_str: *const c_char) -> WasmtimeResult<String> {
    c_str_to_string(c_str, "parameter")
}

/// Convert Rust string to C char pointer (allocated)
pub fn string_to_c_char(s: String) -> WasmtimeResult<*mut c_char> {
    match std::ffi::CString::new(s) {
        Ok(c_string) => Ok(c_string.into_raw()),
        Err(_) => Err(WasmtimeError::InvalidParameter {
            message: "String contains null bytes".to_string(),
        }),
    }
}

// ============================================================================
// FFI Panic Boundary Macros
// ============================================================================
// These macros wrap FFI function bodies with catch_unwind to prevent panics
// from crossing the FFI boundary and crashing the JVM.

/// Wrap an FFI function body that returns i32 (error code).
/// Panics are caught and converted to error code -5 (RuntimeError).
///
/// # Example
/// ```ignore
/// #[no_mangle]
/// pub extern "C" fn my_ffi_function(arg: i32) -> i32 {
///     ffi_boundary_i32!({
///         // function body here
///         if arg < 0 {
///             return Err(WasmtimeError::invalid_parameter("arg must be positive"));
///         }
///         Ok(0) // success
///     })
/// }
/// ```
#[macro_export]
macro_rules! ffi_boundary_i32 {
    ($body:expr) => {{
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body));
        match result {
            Ok(Ok(code)) => code,
            Ok(Err(error)) => {
                $crate::error::ffi_utils::set_last_error(error);
                $crate::error::ErrorCode::RuntimeError as i32
            }
            Err(panic_info) => {
                let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = panic_info.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "Unknown panic occurred in native code".to_string()
                };
                log::error!("Native panic in FFI call: {}", panic_msg);
                let error = $crate::error::WasmtimeError::from_string(format!(
                    "Native panic: {}",
                    panic_msg
                ));
                $crate::error::ffi_utils::set_last_error(error);
                $crate::error::ErrorCode::RuntimeError as i32
            }
        }
    }};
}

/// Wrap an FFI function body that returns isize.
/// Panics are caught and converted to -1.
#[macro_export]
macro_rules! ffi_boundary_isize {
    ($body:expr) => {{
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body));
        match result {
            Ok(value) => value,
            Err(panic_info) => {
                let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = panic_info.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "Unknown panic occurred in native code".to_string()
                };
                log::error!("Native panic in FFI call: {}", panic_msg);
                let error = $crate::error::WasmtimeError::from_string(format!(
                    "Native panic: {}",
                    panic_msg
                ));
                $crate::error::ffi_utils::set_last_error(error);
                -1isize
            }
        }
    }};
}

/// Wrap an FFI function body that returns a raw pointer (*mut c_void or similar).
/// Panics are caught and converted to null pointer.
///
/// # Example
/// ```ignore
/// #[no_mangle]
/// pub extern "C" fn create_something() -> *mut c_void {
///     ffi_boundary_ptr!({
///         let obj = Box::new(MyObject::new());
///         Box::into_raw(obj) as *mut c_void
///     })
/// }
/// ```
#[macro_export]
macro_rules! ffi_boundary_ptr {
    ($body:expr) => {{
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body));
        match result {
            Ok(ptr) => ptr,
            Err(panic_info) => {
                let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = panic_info.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "Unknown panic occurred in native code".to_string()
                };
                log::error!("Native panic in FFI call: {}", panic_msg);
                let error = $crate::error::WasmtimeError::from_string(format!(
                    "Native panic: {}",
                    panic_msg
                ));
                $crate::error::ffi_utils::set_last_error(error);
                std::ptr::null_mut()
            }
        }
    }};
}

/// Wrap an FFI function body that returns void (no return value).
/// Panics are caught and logged but don't propagate.
#[macro_export]
macro_rules! ffi_boundary_void {
    ($body:expr) => {{
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body));
        if let Err(panic_info) = result {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            log::error!("Native panic in FFI call: {}", panic_msg);
            let error =
                $crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            $crate::error::ffi_utils::set_last_error(error);
        }
    }};
}

/// Wrap an FFI function body that returns bool/jboolean.
/// Panics are caught and converted to false.
#[macro_export]
macro_rules! ffi_boundary_bool {
    ($body:expr) => {{
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body));
        match result {
            Ok(value) => value,
            Err(panic_info) => {
                let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = panic_info.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "Unknown panic occurred in native code".to_string()
                };
                log::error!("Native panic in FFI call: {}", panic_msg);
                let error = $crate::error::WasmtimeError::from_string(format!(
                    "Native panic: {}",
                    panic_msg
                ));
                $crate::error::ffi_utils::set_last_error(error);
                false
            }
        }
    }};
}

/// Wrap an FFI function body that returns a Result<T, WasmtimeError>.
/// Converts to default value on error or panic, and stores error in last_error.
///
/// # Example
/// ```ignore
/// #[no_mangle]
/// pub extern "C" fn get_value(handle: *mut c_void) -> i64 {
///     ffi_boundary_result!(0i64, {
///         let obj = unsafe { &*(handle as *const MyObject) };
///         Ok(obj.get_value())
///     })
/// }
/// ```
#[macro_export]
macro_rules! ffi_boundary_result {
    ($default:expr, $body:expr) => {{
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| $body));
        match result {
            Ok(Ok(value)) => {
                $crate::error::ffi_utils::clear_last_error();
                value
            }
            Ok(Err(error)) => {
                $crate::error::ffi_utils::set_last_error(error);
                $default
            }
            Err(panic_info) => {
                let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                    s.to_string()
                } else if let Some(s) = panic_info.downcast_ref::<String>() {
                    s.clone()
                } else {
                    "Unknown panic occurred in native code".to_string()
                };
                log::error!("Native panic in FFI call: {}", panic_msg);
                let error = $crate::error::WasmtimeError::from_string(format!(
                    "Native panic: {}",
                    panic_msg
                ));
                $crate::error::ffi_utils::set_last_error(error);
                $default
            }
        }
    }};
}

// Re-export macros at module level for easier access
pub use crate::ffi_boundary_bool;
pub use crate::ffi_boundary_i32;
pub use crate::ffi_boundary_isize;
pub use crate::ffi_boundary_ptr;
pub use crate::ffi_boundary_result;
pub use crate::ffi_boundary_void;
