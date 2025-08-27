//! Comprehensive error handling for the wasmtime4j native library
//!
//! This module provides defensive error handling that prevents JVM crashes
//! and provides consistent error reporting across JNI and Panama FFI interfaces.

use std::ffi::{CStr, CString};
use std::os::raw::c_char;
use thiserror::Error;
use wasmtime::{Trap, WasmBacktrace};

/// Comprehensive error types for wasmtime4j operations
#[derive(Error, Debug)]
pub enum WasmtimeError {
    /// WebAssembly compilation errors
    #[error("Compilation failed: {message}")]
    Compilation { message: String },

    /// WebAssembly validation errors
    #[error("Module validation failed: {message}")]
    Validation { message: String },

    /// WebAssembly runtime errors and traps
    #[error("Runtime error: {message}")]
    Runtime { 
        message: String,
        backtrace: Option<WasmBacktrace>,
    },

    /// Engine configuration errors
    #[error("Engine configuration error: {message}")]
    EngineConfig { message: String },

    /// Store creation and management errors
    #[error("Store error: {message}")]
    Store { message: String },

    /// Memory access and management errors
    #[error("Memory error: {message}")]
    Memory { message: String },

    /// Function invocation errors
    #[error("Function invocation failed: {message}")]
    Function { message: String },

    /// Import/Export resolution errors
    #[error("Import/Export error: {message}")]
    ImportExport { message: String },

    /// Type conversion and validation errors
    #[error("Type error: {message}")]
    Type { message: String },

    /// Resource management errors
    #[error("Resource error: {message}")]
    Resource { message: String },

    /// I/O and file system errors
    #[error("I/O error: {source}")]
    Io { 
        #[from] 
        source: std::io::Error 
    },

    /// Null pointer or invalid parameter errors
    #[error("Invalid parameter: {message}")]
    InvalidParameter { message: String },

    /// Threading and concurrency errors
    #[error("Concurrency error: {message}")]
    Concurrency { message: String },

    /// WASI-specific errors (for future use)
    #[error("WASI error: {message}")]
    Wasi { message: String },

    /// Unexpected internal errors
    #[error("Internal error: {message}")]
    Internal { message: String },
}

/// Result type for wasmtime4j operations
pub type WasmtimeResult<T> = Result<T, WasmtimeError>;

/// Error codes for C FFI interface (Panama)
#[repr(i32)]
#[derive(Debug, Clone, Copy)]
pub enum ErrorCode {
    Success = 0,
    CompilationError = -1,
    ValidationError = -2,
    RuntimeError = -3,
    EngineConfigError = -4,
    StoreError = -5,
    MemoryError = -6,
    FunctionError = -7,
    ImportExportError = -8,
    TypeError = -9,
    ResourceError = -10,
    IoError = -11,
    InvalidParameterError = -12,
    ConcurrencyError = -13,
    WasiError = -14,
    InternalError = -15,
}

impl WasmtimeError {
    /// Convert to error code for FFI
    pub fn to_error_code(&self) -> ErrorCode {
        match self {
            WasmtimeError::Compilation { .. } => ErrorCode::CompilationError,
            WasmtimeError::Validation { .. } => ErrorCode::ValidationError,
            WasmtimeError::Runtime { .. } => ErrorCode::RuntimeError,
            WasmtimeError::EngineConfig { .. } => ErrorCode::EngineConfigError,
            WasmtimeError::Store { .. } => ErrorCode::StoreError,
            WasmtimeError::Memory { .. } => ErrorCode::MemoryError,
            WasmtimeError::Function { .. } => ErrorCode::FunctionError,
            WasmtimeError::ImportExport { .. } => ErrorCode::ImportExportError,
            WasmtimeError::Type { .. } => ErrorCode::TypeError,
            WasmtimeError::Resource { .. } => ErrorCode::ResourceError,
            WasmtimeError::Io { .. } => ErrorCode::IoError,
            WasmtimeError::InvalidParameter { .. } => ErrorCode::InvalidParameterError,
            WasmtimeError::Concurrency { .. } => ErrorCode::ConcurrencyError,
            WasmtimeError::Wasi { .. } => ErrorCode::WasiError,
            WasmtimeError::Internal { .. } => ErrorCode::InternalError,
        }
    }

    /// Get error message as C string for FFI
    pub fn to_c_string(&self) -> CString {
        CString::new(self.to_string()).unwrap_or_else(|_| {
            CString::new("Error message contains null bytes").unwrap()
        })
    }

    /// Create from Wasmtime trap
    pub fn from_trap(trap: Trap) -> Self {
        WasmtimeError::Runtime {
            message: trap.to_string(),
            backtrace: trap.trace(),
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

/// Error handling utilities for FFI interfaces
pub mod ffi_utils {
    use super::*;

    /// Store last error for FFI error handling
    thread_local! {
        static LAST_ERROR: std::cell::RefCell<Option<WasmtimeError>> = std::cell::RefCell::new(None);
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
                clear_last_error();
                (ErrorCode::Success, result)
            }
            Err(error) => {
                let code = error.to_error_code();
                set_last_error(error);
                (code, T::default())
            }
        }
    }
}

/// JNI error conversion utilities
pub mod jni_utils {
    use super::*;

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
            _ => "ai/tegmentum/wasmtime4j/WasmException",
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
        
        let result = ffi_try(|| {
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