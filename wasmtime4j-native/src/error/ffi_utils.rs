//! Shared FFI utilities for both JNI and Panama interfaces
//!
//! This module provides common functionality that eliminates code duplication
//! between JNI and Panama FFI implementations while maintaining thread safety
//! and defensive programming practices.

use super::{ErrorCode, WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::ffi::CString;
use std::os::raw::{c_char, c_void};
use std::sync::{Arc, Mutex};

/// Enhanced error context for better diagnostics and debugging
#[derive(Debug, Clone)]
pub struct ErrorContext {
    /// The underlying error
    pub error: WasmtimeError,
    /// The operation that was being performed
    pub operation: Option<String>,
    /// The file where the error occurred
    pub file: Option<String>,
    /// The line number where the error occurred
    pub line: Option<u32>,
    /// When the error occurred
    pub timestamp: std::time::Instant,
    /// The thread ID where the error occurred
    pub thread_id: String,
    /// Optional stack trace
    pub stack_trace: Option<String>,
}

impl ErrorContext {
    /// Create a new error context
    pub fn new(error: WasmtimeError) -> Self {
        Self {
            error,
            operation: None,
            file: None,
            line: None,
            timestamp: std::time::Instant::now(),
            thread_id: format!("{:?}", std::thread::current().id()),
            stack_trace: None,
        }
    }

    /// Add operation context
    pub fn with_operation(mut self, operation: String) -> Self {
        self.operation = Some(operation);
        self
    }

    /// Add file and line location
    pub fn with_location(mut self, file: String, line: u32) -> Self {
        self.file = Some(file);
        self.line = Some(line);
        self
    }

    /// Add stack trace
    pub fn with_stack_trace(mut self, stack_trace: String) -> Self {
        self.stack_trace = Some(stack_trace);
        self
    }
}

// Store last error with enhanced context for FFI error handling
thread_local! {
    static LAST_ERROR: std::cell::RefCell<Option<WasmtimeError>> = std::cell::RefCell::new(None);
    static LAST_ERROR_CONTEXT: std::cell::RefCell<Option<ErrorContext>> = std::cell::RefCell::new(None);
}

/// Resource handle type for thread-safe resource management
pub type ResourceHandle = u64;

/// Thread-safe resource registry for managing native resources
static RESOURCE_REGISTRY: once_cell::sync::Lazy<Arc<Mutex<HashMap<ResourceHandle, Box<dyn std::any::Any + Send + Sync>>>>> =
    once_cell::sync::Lazy::new(|| Arc::new(Mutex::new(HashMap::new())));

static NEXT_HANDLE: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

/// Register a resource and return a handle for thread-safe access
pub fn register_resource<T: 'static + Send + Sync>(resource: T) -> WasmtimeResult<ResourceHandle> {
    let handle = NEXT_HANDLE.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

    let mut registry = RESOURCE_REGISTRY.lock().map_err(|_| {
        WasmtimeError::Concurrency {
            message: "Failed to acquire resource registry lock".to_string(),
        }
    })?;

    registry.insert(handle, Box::new(resource));
    Ok(handle)
}

/// Get a resource by handle with thread-safe access
pub fn get_resource<T: 'static + Send + Sync>(handle: ResourceHandle) -> WasmtimeResult<Arc<Mutex<T>>> {
    let registry = RESOURCE_REGISTRY.lock().map_err(|_| {
        WasmtimeError::Concurrency {
            message: "Failed to acquire resource registry lock".to_string(),
        }
    })?;

    let resource = registry.get(&handle).ok_or_else(|| {
        WasmtimeError::Resource {
            message: format!("Resource handle {} not found", handle),
        }
    })?;

    let typed_resource = resource.downcast_ref::<Arc<Mutex<T>>>().ok_or_else(|| {
        WasmtimeError::Type {
            message: "Resource type mismatch".to_string(),
        }
    })?;

    Ok(Arc::clone(typed_resource))
}

/// Remove a resource by handle
pub fn unregister_resource(handle: ResourceHandle) -> WasmtimeResult<()> {
    let mut registry = RESOURCE_REGISTRY.lock().map_err(|_| {
        WasmtimeError::Concurrency {
            message: "Failed to acquire resource registry lock".to_string(),
        }
    })?;

    registry.remove(&handle).ok_or_else(|| {
        WasmtimeError::Resource {
            message: format!("Resource handle {} not found", handle),
        }
    })?;

    Ok(())
}

/// Enhanced logging utility for performance monitoring
pub struct PerformanceLogger {
    operation: String,
    start_time: std::time::Instant,
    context: String,
}

impl PerformanceLogger {
    /// Start monitoring an operation
    pub fn start(operation: String) -> Self {
        log::debug!("Starting operation: {}", operation);
        Self {
            operation,
            start_time: std::time::Instant::now(),
            context: String::new(),
        }
    }

    /// Start monitoring an operation with context
    pub fn start_with_context(operation: String, context: String) -> Self {
        log::debug!("Starting operation: {} [{}]", operation, context);
        Self {
            operation,
            start_time: std::time::Instant::now(),
            context,
        }
    }

    /// Finish the operation with success
    pub fn finish_success(self) {
        let duration = self.start_time.elapsed();
        if self.context.is_empty() {
            log::info!("Operation '{}' completed successfully in {:?}", self.operation, duration);
        } else {
            log::info!("Operation '{}' [{}] completed successfully in {:?}",
                self.operation, self.context, duration);
        }
    }

    /// Finish the operation with error
    pub fn finish_error(self, error: &WasmtimeError) {
        let duration = self.start_time.elapsed();
        if self.context.is_empty() {
            log::warn!("Operation '{}' failed after {:?}: {}", self.operation, duration, error);
        } else {
            log::warn!("Operation '{}' [{}] failed after {:?}: {}",
                self.operation, self.context, duration, error);
        }

        // Log the error with appropriate level
        error.log_error_with_context(&self.operation, &self.context);
    }

    /// Add checkpoint logging during operation
    pub fn checkpoint(&self, message: &str) {
        let duration = self.start_time.elapsed();
        if self.context.is_empty() {
            log::debug!("Operation '{}' checkpoint at {:?}: {}", self.operation, duration, message);
        } else {
            log::debug!("Operation '{}' [{}] checkpoint at {:?}: {}",
                self.operation, self.context, duration, message);
        }
    }

    /// Check if operation is taking too long and log warning
    pub fn check_timeout(&self, warning_threshold: std::time::Duration) {
        let duration = self.start_time.elapsed();
        if duration > warning_threshold {
            if self.context.is_empty() {
                log::warn!("Operation '{}' is taking longer than expected: {:?} > {:?}",
                    self.operation, duration, warning_threshold);
            } else {
                log::warn!("Operation '{}' [{}] is taking longer than expected: {:?} > {:?}",
                    self.operation, self.context, duration, warning_threshold);
            }
        }
    }
}

/// Macro to automatically time and log operations
#[macro_export]
macro_rules! timed_operation {
    ($operation:expr, $body:expr) => {{
        let logger = crate::error::ffi_utils::PerformanceLogger::start($operation.to_string());
        let result = $body;
        match &result {
            Ok(_) => logger.finish_success(),
            Err(error) => logger.finish_error(error),
        }
        result
    }};
    ($operation:expr, $context:expr, $body:expr) => {{
        let logger = crate::error::ffi_utils::PerformanceLogger::start_with_context(
            $operation.to_string(), $context.to_string());
        let result = $body;
        match &result {
            Ok(_) => logger.finish_success(),
            Err(error) => logger.finish_error(error),
        }
        result
    }};
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
        LAST_ERROR.with(|e| {
            match e.try_borrow() {
                Ok(error_ref) => {
                    match error_ref.as_ref() {
                        Some(error) => error.to_c_string().into_raw(),
                        None => std::ptr::null_mut(),
                    }
                }
                Err(_) => {
                    log::warn!("Failed to get last error due to borrow check failure");
                    std::ptr::null_mut()
                }
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
        LAST_ERROR.with(|e| {
            match e.try_borrow_mut() {
                Ok(mut error_ref) => {
                    *error_ref = None;
                }
                Err(_) => {
                    log::warn!("Failed to clear last error due to borrow check failure");
                }
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
        log::error!("Panic occurred while freeing error message at {:p} - potential memory leak", message);
    }
}

/// Check if there are any pending errors in thread-local storage
/// This is useful for debugging thread safety issues
pub fn has_pending_error() -> bool {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR.with(|e| {
            match e.try_borrow() {
                Ok(error_ref) => error_ref.is_some(),
                Err(_) => {
                    log::warn!("Failed to check pending error due to borrow check failure");
                    false
                }
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
        LAST_ERROR.with(|e| {
            match e.try_borrow() {
                Ok(error_ref) => {
                    match error_ref.as_ref() {
                        Some(error) => (true, format!("Error: {} (Code: {:?})", error, error.to_error_code())),
                        None => (false, "No error".to_string()),
                    }
                }
                Err(_) => {
                    (false, "Borrow check failed - potential threading issue".to_string())
                }
            }
        })
    }));

    match result {
        Ok((has_error, message)) => (has_error, message),
        Err(_) => (false, "Panic occurred during error stats check".to_string()),
    }
}

/// Set error context with enhanced diagnostic information
pub fn set_error_context(context: ErrorContext) {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        // Set both the basic error and the enhanced context
        LAST_ERROR.with(|e| {
            if let Ok(mut error_ref) = e.try_borrow_mut() {
                *error_ref = Some(context.error.clone());
            }
        });

        LAST_ERROR_CONTEXT.with(|ctx| {
            if let Ok(mut context_ref) = ctx.try_borrow_mut() {
                *context_ref = Some(context);
            }
        });
    }));

    if result.is_err() {
        log::error!("Panic occurred while setting error context - prevented JVM crash");
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

/// Get the enhanced error context if available
pub fn get_error_context() -> Option<ErrorContext> {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR_CONTEXT.with(|ctx| {
            match ctx.try_borrow() {
                Ok(context_ref) => context_ref.clone(),
                Err(_) => {
                    log::warn!("Failed to get error context due to borrow check failure");
                    None
                }
            }
        })
    }));

    match result {
        Ok(context) => context,
        Err(_) => {
            log::error!("Panic occurred while getting error context - prevented JVM crash");
            None
        }
    }
}

/// Clear error context
pub fn clear_error_context() {
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        LAST_ERROR_CONTEXT.with(|ctx| {
            if let Ok(mut context_ref) = ctx.try_borrow_mut() {
                *context_ref = None;
            }
        });
    }));

    if result.is_err() {
        log::error!("Panic occurred while clearing error context - prevented JVM crash");
    }
}

/// Create error context with file and line information
#[macro_export]
macro_rules! error_context {
    ($error:expr) => {
        ErrorContext::new($error)
            .with_location(file!().to_string(), line!())
    };
    ($error:expr, $operation:expr) => {
        ErrorContext::new($error)
            .with_operation($operation.to_string())
            .with_location(file!().to_string(), line!())
    };
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
        return Err(WasmtimeError::invalid_parameter(
            format!("{} pointer cannot be null", name)
        ));
    }
    Ok(&*(ptr as *const T))
}

/// Safe mutable pointer dereference with validation
///
/// # Safety
/// The caller must ensure that `ptr` points to a valid value of type `T`.
pub unsafe fn deref_ptr_mut<T>(ptr: *mut c_void, name: &str) -> WasmtimeResult<&'static mut T> {
    if ptr.is_null() {
        return Err(WasmtimeError::invalid_parameter(
            format!("{} pointer cannot be null", name)
        ));
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
    name: &str
) -> WasmtimeResult<&'static [T]> {
    if ptr.is_null() {
        return Err(WasmtimeError::invalid_parameter(
            format!("{} pointer cannot be null", name)
        ));
    }
    if len == 0 {
        return Err(WasmtimeError::invalid_parameter(
            format!("{} length cannot be zero", name)
        ));
    }
    Ok(std::slice::from_raw_parts(ptr, len))
}

// Re-export resource destruction utilities from ffi_common for backwards compatibility
pub use crate::ffi_common::resource_destruction::{
    DESTROYED_POINTERS,
    clear_destroyed_pointers,
    safe_destroy,
    safe_destroy_no_fake_check,
    is_fake_pointer,
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
        destroyed_count, memory_count, store_count
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
            log::debug!("TestCleanupGuard({}): cleaned up {} destroyed pointers", self.test_name, destroyed);
        }
    }
}

/// Convert C string to Rust string with validation
///
/// # Safety
/// The caller must ensure that `c_str` points to a valid null-terminated C string.
pub unsafe fn c_str_to_string(c_str: *const c_char, name: &str) -> WasmtimeResult<String> {
    if c_str.is_null() {
        return Err(WasmtimeError::invalid_parameter(
            format!("{} C string cannot be null", name)
        ));
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
