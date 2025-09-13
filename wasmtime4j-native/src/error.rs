//! Comprehensive error handling for the wasmtime4j native library
//!
//! This module provides defensive error handling that prevents JVM crashes
//! and provides consistent error reporting across JNI and Panama FFI interfaces.

use std::ffi::CString;
use std::os::raw::c_char;
use std::collections::HashSet;
use thiserror::Error;
use once_cell::sync::Lazy;
use wasmtime::{Trap, WasmBacktrace};

/// Comprehensive error types for wasmtime4j operations
#[derive(Error, Debug)]
pub enum WasmtimeError {
    /// WebAssembly compilation errors
    #[error("Compilation failed: {message}")]
    Compilation { 
        /// Error message describing the compilation failure
        message: String 
    },

    /// WebAssembly validation errors
    #[error("Module validation failed: {message}")]
    Validation { 
        /// Error message describing the validation failure
        message: String 
    },

    /// WebAssembly module errors
    #[error("Module error: {message}")]
    Module { 
        /// Error message describing the module error
        message: String 
    },

    /// WebAssembly runtime errors and traps
    #[error("Runtime error: {message}")]
    Runtime { 
        /// Error message describing the runtime error
        message: String,
        /// Optional WebAssembly backtrace for debugging
        backtrace: Option<WasmBacktrace>,
    },

    /// Engine configuration errors
    #[error("Engine configuration error: {message}")]
    EngineConfig { 
        /// Error message describing the engine configuration issue
        message: String 
    },

    /// Store creation and management errors
    #[error("Store error: {message}")]
    Store { 
        /// Error message describing the store-related error
        message: String 
    },

    /// WebAssembly instance creation and management errors
    #[error("Instance error: {message}")]
    Instance { 
        /// Error message describing the instance-related error
        message: String 
    },

    /// Memory access and management errors
    #[error("Memory error: {message}")]
    Memory { 
        /// Error message describing the memory access error
        message: String 
    },

    /// Function invocation errors
    #[error("Function invocation failed: {message}")]
    Function { 
        /// Error message describing the function invocation error
        message: String 
    },

    /// Import/Export resolution errors
    #[error("Import/Export error: {message}")]
    ImportExport { 
        /// Error message describing the import/export resolution error
        message: String 
    },

    /// Type conversion and validation errors
    #[error("Type error: {message}")]
    Type { 
        /// Error message describing the type conversion or validation error
        message: String 
    },

    /// Resource management errors
    #[error("Resource error: {message}")]
    Resource { 
        /// Error message describing the resource management error
        message: String 
    },

    /// I/O and file system errors
    #[error("I/O error: {source}")]
    Io { 
        #[from] 
        /// The underlying I/O error that occurred
        source: std::io::Error 
    },

    /// Null pointer or invalid parameter errors
    #[error("Invalid parameter: {message}")]
    InvalidParameter { 
        /// Error message describing the invalid parameter
        message: String 
    },

    /// Threading and concurrency errors
    #[error("Concurrency error: {message}")]
    Concurrency { 
        /// Error message describing the concurrency issue
        message: String 
    },

    /// WASI-specific errors (for future use)
    #[error("WASI error: {message}")]
    Wasi { 
        /// Error message describing the WASI-related error
        message: String 
    },

    /// Component model specific errors
    #[error("Component error: {message}")]
    Component { 
        /// Error message describing the component-related error
        message: String 
    },

    /// WIT interface definition and binding errors
    #[error("Interface error: {message}")]
    Interface { 
        /// Error message describing the interface-related error
        message: String 
    },

    /// Unexpected internal errors
    #[error("Internal error: {message}")]
    Internal { 
        /// Error message describing the internal system error
        message: String 
    },

    /// Function execution errors
    #[error("Function execution failed: {message}")]
    Execution { 
        /// Error message describing the execution error
        message: String 
    },

    /// Export not found errors
    #[error("Export not found: {name}")]
    ExportNotFound { 
        /// Name of the export that was not found
        name: String 
    },
}

/// Result type for wasmtime4j operations
pub type WasmtimeResult<T> = Result<T, WasmtimeError>;

/// Error codes for C FFI interface (Panama)
#[repr(i32)]
#[derive(Debug, Clone, Copy)]
pub enum ErrorCode {
    /// Operation completed successfully
    Success = 0,
    /// WebAssembly compilation failed
    CompilationError = -1,
    /// WebAssembly module validation failed
    ValidationError = -2,
    /// WebAssembly runtime error occurred
    RuntimeError = -3,
    /// Engine configuration error
    EngineConfigError = -4,
    /// Store creation or management error
    StoreError = -5,
    /// Instance creation or management error
    InstanceError = -6,
    /// Memory access or allocation error
    MemoryError = -7,
    /// Function invocation error
    FunctionError = -8,
    /// Import or export resolution error
    ImportExportError = -9,
    /// Type conversion or validation error
    TypeError = -10,
    /// Resource management error
    ResourceError = -11,
    /// I/O operation error
    IoError = -12,
    /// Invalid parameter provided
    InvalidParameterError = -13,
    /// Threading or concurrency error
    ConcurrencyError = -14,
    /// WASI-related error
    WasiError = -15,
    /// Component model error
    ComponentError = -16,
    /// Interface definition or binding error
    InterfaceError = -17,
    /// Internal system error
    InternalError = -18,
}

// The impl WasmtimeError block is defined below to avoid duplication

#[cfg(feature = "jni-bindings")]
impl From<jni::errors::Error> for WasmtimeError {
    fn from(error: jni::errors::Error) -> Self {
        WasmtimeError::Internal {
            message: format!("JNI error: {}", error),
        }
    }
}

impl WasmtimeError {
    /// Get error message as C string for FFI
    pub fn to_c_string(&self) -> CString {
        CString::new(self.to_string()).unwrap_or_else(|_| {
            CString::new("Error message contains null bytes").unwrap()
        })
    }

    /// Convert error to error code for FFI
    pub fn to_error_code(&self) -> ErrorCode {
        match self {
            WasmtimeError::Compilation { .. } => ErrorCode::CompilationError,
            WasmtimeError::Validation { .. } => ErrorCode::ValidationError,
            WasmtimeError::Module { .. } => ErrorCode::ValidationError,
            WasmtimeError::Runtime { .. } => ErrorCode::RuntimeError,
            WasmtimeError::EngineConfig { .. } => ErrorCode::EngineConfigError,
            WasmtimeError::Store { .. } => ErrorCode::StoreError,
            WasmtimeError::Instance { .. } => ErrorCode::InstanceError,
            WasmtimeError::Memory { .. } => ErrorCode::MemoryError,
            WasmtimeError::Function { .. } => ErrorCode::FunctionError,
            WasmtimeError::ImportExport { .. } => ErrorCode::ImportExportError,
            WasmtimeError::Type { .. } => ErrorCode::TypeError,
            WasmtimeError::Resource { .. } => ErrorCode::ResourceError,
            WasmtimeError::Io { .. } => ErrorCode::IoError,
            WasmtimeError::InvalidParameter { .. } => ErrorCode::InvalidParameterError,
            WasmtimeError::Concurrency { .. } => ErrorCode::ConcurrencyError,
            WasmtimeError::Wasi { .. } => ErrorCode::WasiError,
            WasmtimeError::Component { .. } => ErrorCode::ComponentError,
            WasmtimeError::Interface { .. } => ErrorCode::InterfaceError,
            WasmtimeError::Internal { .. } => ErrorCode::InternalError,
            WasmtimeError::Execution { .. } => ErrorCode::FunctionError,
            WasmtimeError::ExportNotFound { .. } => ErrorCode::ImportExportError,
        }
    }

    /// Create from Wasmtime trap
    pub fn from_trap(trap: Trap) -> Self {
        WasmtimeError::Runtime {
            message: trap.to_string(),
            backtrace: None, // trace() method removed in wasmtime 36.0.2
        }
    }

    /// Create from Wasmtime compilation error
    pub fn from_compilation_error(error: wasmtime::Error) -> Self {
        WasmtimeError::Compilation {
            message: error.to_string(),
        }
    }

    /// Create invalid parameter error with defensive checks
    pub fn invalid_parameter<S: Into<String>>(message: S) -> Self {
        WasmtimeError::InvalidParameter {
            message: message.into(),
        }
    }
}

/// Defensive parameter validation macros
#[macro_export]
macro_rules! validate_not_null {
    ($ptr:expr, $name:expr) => {
        if $ptr.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                format!("{} cannot be null", $name)
            ));
        }
    };
}

/// Validates that slice access is within bounds to prevent buffer overruns
/// 
/// This macro performs defensive bounds checking to ensure that accessing
/// a slice with the given offset and length will not cause a panic or 
/// undefined behavior.
/// 
/// # Arguments
/// 
/// * `$slice` - The slice to validate
/// * `$offset` - Starting offset for the access
/// * `$length` - Length of data to access
/// 
/// # Returns
/// 
/// Returns `WasmtimeError::InvalidParameter` if bounds check fails
#[macro_export]
macro_rules! validate_slice_bounds {
    ($slice:expr, $offset:expr, $length:expr) => {
        if $offset + $length > $slice.len() {
            return Err(WasmtimeError::invalid_parameter(
                format!("Slice bounds check failed: offset {} + length {} > slice length {}", 
                    $offset, $length, $slice.len())
            ));
        }
    };
}

/// Validates that a C-style pointer is not null and returns error if it is
/// 
/// This macro performs defensive null pointer checking for FFI operations
/// to prevent segmentation faults and undefined behavior.
/// 
/// # Arguments
/// 
/// * `$ptr` - The pointer to validate
/// * `$name` - Name of the parameter for error messages
/// 
/// # Returns
/// 
/// Returns `WasmtimeError::InvalidParameter` if pointer is null
#[macro_export]
macro_rules! validate_ptr_not_null {
    ($ptr:expr, $name:expr) => {
        if $ptr.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                format!("{} pointer cannot be null", $name)
            ));
        }
    };
}

/// Validates that an array/slice is not empty
/// 
/// This macro performs defensive validation to ensure arrays or slices
/// have at least one element before processing.
/// 
/// # Arguments
/// 
/// * `$collection` - The array or slice to validate
/// * `$name` - Name of the parameter for error messages
/// 
/// # Returns
/// 
/// Returns `WasmtimeError::InvalidParameter` if collection is empty
#[macro_export]
macro_rules! validate_not_empty {
    ($collection:expr, $name:expr) => {
        if $collection.is_empty() {
            return Err(WasmtimeError::invalid_parameter(
                format!("{} cannot be empty", $name)
            ));
        }
    };
}

/// Validates that a handle value is valid (non-zero for pointers)
/// 
/// This macro performs defensive validation to ensure handle values
/// represent valid resources.
/// 
/// # Arguments
/// 
/// * `$handle` - The handle value to validate
/// * `$name` - Name of the parameter for error messages
/// 
/// # Returns
/// 
/// Returns `WasmtimeError::InvalidParameter` if handle is invalid
#[macro_export]
macro_rules! validate_handle {
    ($handle:expr, $name:expr) => {
        if $handle == 0 {
            return Err(WasmtimeError::invalid_parameter(
                format!("{} handle is invalid", $name)
            ));
        }
    };
}

/// Shared FFI utilities for both JNI and Panama interfaces
/// 
/// This module provides common functionality that eliminates code duplication
/// between JNI and Panama FFI implementations while maintaining thread safety
/// and defensive programming practices.
pub mod ffi_utils {
    use super::*;
    use std::sync::{Arc, Mutex};
    use std::collections::HashMap;
    use std::os::raw::c_void;

    // Store last error for FFI error handling
    thread_local! {
        static LAST_ERROR: std::cell::RefCell<Option<WasmtimeError>> = std::cell::RefCell::new(None);
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

    /// Set last error for FFI retrieval
    pub fn set_last_error(error: WasmtimeError) {
        LAST_ERROR.with(|e| {
            *e.borrow_mut() = Some(error);
        });
    }

    /// Get last error message as C string
    /// Returns null if no error
    pub fn get_last_error_message() -> *mut c_char {
        LAST_ERROR.with(|e| {
            match e.borrow().as_ref() {
                Some(error) => error.to_c_string().into_raw(),
                None => std::ptr::null_mut(),
            }
        })
    }

    /// Clear last error
    pub fn clear_last_error() {
        LAST_ERROR.with(|e| {
            *e.borrow_mut() = None;
        });
    }

    /// Free error message C string
    pub unsafe fn free_error_message(message: *mut c_char) {
        if !message.is_null() {
            drop(CString::from_raw(message));
        }
    }

    /// Execute operation with FFI error handling
    pub fn ffi_try<F, T>(operation: F) -> (ErrorCode, T)
    where
        F: FnOnce() -> WasmtimeResult<T>,
        T: Default,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                (ErrorCode::Success, result)
            }
            Err(error) => {
                let code = error.to_error_code();
                set_last_error(error);
                (code, T::default())
            }
        }
    }
    
    /// Execute operation with FFI error handling, returning pointer for success
    pub fn ffi_try_ptr<F, T>(operation: F) -> *mut c_void
    where
        F: FnOnce() -> WasmtimeResult<Box<T>>,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                Box::into_raw(result) as *mut c_void
            }
            Err(error) => {
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
    pub fn ffi_try_code<F>(operation: F) -> i32
    where
        F: FnOnce() -> WasmtimeResult<()>,
    {
        match operation() {
            Ok(()) => {
                jni_utils::clear_last_error();
                ErrorCode::Success as i32
            }
            Err(error) => {
                let code = error.to_error_code();
                set_last_error(error);
                code as i32
            }
        }
    }
    
    /// Safe pointer dereference with validation
    pub unsafe fn deref_ptr<T>(ptr: *const c_void, name: &str) -> WasmtimeResult<&'static T> {
        if ptr.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                format!("{} pointer cannot be null", name)
            ));
        }
        Ok(&*(ptr as *const T))
    }
    
    /// Safe mutable pointer dereference with validation
    pub unsafe fn deref_ptr_mut<T>(ptr: *mut c_void, name: &str) -> WasmtimeResult<&'static mut T> {
        if ptr.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                format!("{} pointer cannot be null", name)
            ));
        }
        Ok(&mut *(ptr as *mut T))
    }
    
    /// Safe slice creation from raw pointer with validation
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
    
    /// Thread-safe tracking of destroyed pointers to prevent double-free
    /// Using usize addresses instead of raw pointers for thread safety
    pub(crate) static DESTROYED_POINTERS: Lazy<Mutex<HashSet<usize>>> = 
        Lazy::new(|| Mutex::new(HashSet::new()));

    /// Safely destroy a boxed resource from raw pointer with double-free protection
    pub unsafe fn destroy_resource<T>(ptr: *mut c_void, name: &str) {
        if ptr.is_null() {
            return;
        }

        // Acquire lock and check if this pointer was already destroyed
        let ptr_addr = ptr as usize;
        {
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            if destroyed.contains(&ptr_addr) {
                log::warn!("Attempted double-free of {} resource at {:p} - ignoring", name, ptr);
                return;
            }
            // Mark this pointer as destroyed before releasing the lock
            destroyed.insert(ptr_addr);
        }

        // Use panic-safe destruction to prevent JVM crashes
        let result = std::panic::catch_unwind(|| {
            let _ = Box::from_raw(ptr as *mut T);
        });

        match result {
            Ok(_) => {
                log::debug!("{} at {:p} destroyed successfully", name, ptr);
            }
            Err(e) => {
                log::error!("{} at {:p} destruction panicked: {:?} - preventing JVM crash", name, ptr, e);
                // Don't propagate panic to JVM - just log and continue
            }
        }
    }
    
    /// Convert C string to Rust string with validation
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
}

/// JNI error conversion utilities
pub mod jni_utils {
    use super::*;
    use std::os::raw::c_void;

    /// Clear the last error (stub for JNI compatibility)
    pub fn clear_last_error() {
        // For now, this is a no-op since we don't maintain global error state
        // This could be extended to clear thread-local error storage if needed
    }

    /// Convert WasmtimeError to JNI exception class name
    pub fn error_to_exception_class(error: &WasmtimeError) -> &'static str {
        match error {
            WasmtimeError::Compilation { .. } => "ai/tegmentum/wasmtime4j/WasmCompilationException",
            WasmtimeError::Validation { .. } => "ai/tegmentum/wasmtime4j/WasmValidationException",
            WasmtimeError::Runtime { .. } => "ai/tegmentum/wasmtime4j/WasmRuntimeException",
            WasmtimeError::Memory { .. } => "ai/tegmentum/wasmtime4j/WasmMemoryException",
            WasmtimeError::Function { .. } => "ai/tegmentum/wasmtime4j/WasmFunctionException",
            WasmtimeError::Type { .. } => "ai/tegmentum/wasmtime4j/WasmTypeException",
            WasmtimeError::InvalidParameter { .. } => "java/lang/IllegalArgumentException",
            WasmtimeError::Io { .. } => "java/io/IOException",
            WasmtimeError::Component { .. } => "ai/tegmentum/wasmtime4j/WasmComponentException",
            WasmtimeError::Interface { .. } => "ai/tegmentum/wasmtime4j/WasmInterfaceException",
            WasmtimeError::EngineConfig { .. } => "ai/tegmentum/wasmtime4j/WasmEngineException",
            WasmtimeError::Store { .. } => "ai/tegmentum/wasmtime4j/WasmStoreException",
            WasmtimeError::Instance { .. } => "ai/tegmentum/wasmtime4j/WasmInstanceException",
            WasmtimeError::ImportExport { .. } => "ai/tegmentum/wasmtime4j/WasmImportExportException",
            WasmtimeError::Resource { .. } => "ai/tegmentum/wasmtime4j/WasmResourceException",
            WasmtimeError::Concurrency { .. } => "ai/tegmentum/wasmtime4j/WasmConcurrencyException",
            WasmtimeError::Wasi { .. } => "ai/tegmentum/wasmtime4j/WasiException",
            WasmtimeError::Internal { .. } => "ai/tegmentum/wasmtime4j/WasmInternalException",
            WasmtimeError::Execution { .. } => "ai/tegmentum/wasmtime4j/WasmExecutionException",
            WasmtimeError::ExportNotFound { .. } => "ai/tegmentum/wasmtime4j/WasmExportNotFoundException",
            _ => "ai/tegmentum/wasmtime4j/WasmException",
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Throw JNI exception with proper error information
    pub fn throw_jni_exception(env: &mut jni::JNIEnv, error: &WasmtimeError) {
        let class_name = error_to_exception_class(error);
        let message = error.to_string();
        
        if let Err(e) = env.throw_new(class_name, &message) {
            // Fallback to RuntimeException if specific exception class doesn't exist
            log::error!("Failed to throw specific exception {}: {:?}, using RuntimeException", class_name, e);
            if let Err(e2) = env.throw_new("java/lang/RuntimeException", &message) {
                log::error!("Failed to throw fallback RuntimeException: {:?}", e2);
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning pointer for success
    pub fn jni_try_ptr<F, T>(mut env: jni::JNIEnv, operation: F) -> *mut c_void
    where
        F: FnOnce() -> WasmtimeResult<Box<T>>,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                Box::into_raw(result) as *mut c_void
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
                std::ptr::null_mut()
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning error code
    pub fn jni_try_code<F>(mut env: jni::JNIEnv, operation: F) -> i32
    where
        F: FnOnce() -> WasmtimeResult<()>,
    {
        match operation() {
            Ok(()) => {
                jni_utils::clear_last_error();
                ErrorCode::Success as i32
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
                error.to_error_code() as i32
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning typed result
    pub fn jni_try<F, T>(mut env: jni::JNIEnv, operation: F) -> (ErrorCode, T)
    where
        F: FnOnce() -> WasmtimeResult<T>,
        T: Default,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                (ErrorCode::Success, result)
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
                (error.to_error_code(), T::default())
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
                jni_utils::clear_last_error();
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
    pub fn jni_try_bool<F>(mut env: jni::JNIEnv, operation: F) -> bool
    where
        F: FnOnce() -> WasmtimeResult<bool>,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                result
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
                false
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning default value on error
    pub fn jni_try_with_default<F, T>(mut env: jni::JNIEnv, default_value: T, operation: F) -> T
    where
        F: FnOnce() -> WasmtimeResult<T>,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                result
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
                default_value
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning void
    pub fn jni_try_void<F>(mut env: jni::JNIEnv, operation: F)
    where
        F: FnOnce() -> WasmtimeResult<()>,
    {
        match operation() {
            Ok(()) => {
                jni_utils::clear_last_error();
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning JNI object as raw jobject
    pub fn jni_try_object<F>(mut env: jni::JNIEnv, operation: F) -> jni::sys::jobject
    where
        F: FnOnce() -> WasmtimeResult<jni::objects::JObject<'static>>,
    {
        match operation() {
            Ok(result) => {
                jni_utils::clear_last_error();
                result.as_raw()
            }
            Err(error) => {
                throw_jni_exception(&mut env, &error);
                std::ptr::null_mut()
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_error_codes() {
        let error = WasmtimeError::Compilation { 
            message: "test".to_string() 
        };
        assert!(matches!(error.to_error_code(), ErrorCode::CompilationError));
    }

    #[test]
    fn test_c_string_conversion() {
        let error = WasmtimeError::Runtime { 
            message: "test error".to_string(),
            backtrace: None,
        };
        let c_str = error.to_c_string();
        assert!(c_str.to_str().unwrap().contains("test error"));
    }

    #[test]
    fn test_ffi_error_handling() {
        use ffi_utils::*;
        
        let result: (ErrorCode, ()) = ffi_try(|| {
            Err(WasmtimeError::InvalidParameter { 
                message: "test".to_string() 
            })
        });
        
        assert!(matches!(result.0, ErrorCode::InvalidParameterError));
        
        let message = get_last_error_message();
        assert!(!message.is_null());
        
        unsafe {
            free_error_message(message);
        }
    }
}