//! Comprehensive error handling for the wasmtime4j native library
//!
//! This module provides defensive error handling that prevents JVM crashes
//! and provides consistent error reporting across JNI and Panama FFI interfaces.

use std::ffi::CString;
use thiserror::Error;
use wasmtime::{Trap, WasmBacktrace};

pub mod ffi_utils;
pub mod jni_utils;

#[cfg(test)]
mod tests;

// Re-export commonly used types and functions for backward compatibility
pub use ffi_utils::{
    clear_last_error, free_error_message, get_last_error_message, set_last_error, ErrorCollector,
};

/// Comprehensive error types for wasmtime4j operations
#[derive(Error, Debug)]
pub enum WasmtimeError {
    /// WebAssembly compilation errors
    #[error("Compilation failed: {message}")]
    Compilation {
        /// Error message describing the compilation failure
        message: String,
    },

    /// WebAssembly validation errors
    #[error("Module validation failed: {message}")]
    Validation {
        /// Error message describing the validation failure
        message: String,
    },

    /// WebAssembly module errors
    #[error("Module error: {message}")]
    Module {
        /// Error message describing the module error
        message: String,
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
        message: String,
    },

    /// Store creation and management errors
    #[error("Store error: {message}")]
    Store {
        /// Error message describing the store-related error
        message: String,
    },

    /// WebAssembly instance creation and management errors
    #[error("Instance error: {message}")]
    Instance {
        /// Error message describing the instance-related error
        message: String,
    },

    /// WebAssembly function errors
    #[error("Function error: {message}")]
    Function {
        /// Error message describing the function error
        message: String,
    },

    /// WebAssembly memory errors
    #[error("Memory error: {message}")]
    Memory {
        /// Error message describing the memory error
        message: String,
    },

    /// WebAssembly table errors
    #[error("Table error: {message}")]
    Table {
        /// Error message describing the table error
        message: String,
    },

    /// WebAssembly global errors
    #[error("Global error: {message}")]
    Global {
        /// Error message describing the global error
        message: String,
    },

    /// WebAssembly linker errors
    #[error("Linker error: {message}")]
    Linker {
        /// Error message describing the linker error
        message: String,
    },

    /// Import/Export resolution errors
    #[error("Import/Export error: {message}")]
    ImportExport {
        /// Error message describing the import/export resolution error
        message: String,
    },

    /// Type conversion and validation errors
    #[error("Type error: {message}")]
    Type {
        /// Error message describing the type conversion or validation error
        message: String,
    },

    /// Resource management errors
    #[error("Resource error: {message}")]
    Resource {
        /// Error message describing the resource management error
        message: String,
    },

    /// I/O and file system errors
    #[error("I/O error: {source}")]
    Io {
        #[from]
        /// The underlying I/O error that occurred
        source: std::io::Error,
    },

    /// Null pointer or invalid parameter errors
    #[error("Invalid parameter: {message}")]
    InvalidParameter {
        /// Error message describing the invalid parameter
        message: String,
    },

    /// Threading and concurrency errors
    #[error("Concurrency error: {message}")]
    Concurrency {
        /// Error message describing the concurrency issue
        message: String,
    },

    /// WASI-specific errors (for future use)
    #[error("WASI error: {message}")]
    Wasi {
        /// Error message describing the WASI-related error
        message: String,
    },

    /// Security and permission violations
    #[error("Security error: {message}")]
    Security {
        /// Error message describing the security violation
        message: String,
    },

    /// Component model specific errors
    #[error("Component error: {message}")]
    Component {
        /// Error message describing the component-related error
        message: String,
    },

    /// WIT interface definition and binding errors
    #[error("Interface error: {message}")]
    Interface {
        /// Error message describing the interface-related error
        message: String,
    },

    /// Unexpected internal errors
    #[error("Internal error: {message}")]
    Internal {
        /// Error message describing the internal system error
        message: String,
    },

    /// Function execution errors
    #[error("Function execution failed: {message}")]
    Execution {
        /// Error message describing the execution error
        message: String,
    },

    /// Export not found errors
    #[error("Export not found: {name}")]
    ExportNotFound {
        /// Name of the export that was not found
        name: String,
    },

    /// Multiple errors that occurred during complex operations
    #[error("Multiple errors occurred: {summary}")]
    Multiple {
        /// Summary of the aggregated errors
        summary: String,
        /// List of individual errors
        errors: Vec<WasmtimeError>,
    },

    /// WebAssembly instantiation errors
    #[error("Instantiation error: {message}")]
    Instantiation {
        /// Error message describing the instantiation error
        message: String,
    },

    /// Caller context errors
    #[error("Caller context error: {message}")]
    CallerContextError {
        /// Error message describing the caller context error
        message: String,
    },

    /// Type mismatch errors
    #[error("Type mismatch: expected {expected}, got {actual}")]
    TypeMismatch {
        /// Expected type description
        expected: String,
        /// Actual type description
        actual: String,
    },

    /// UTF-8 encoding errors
    #[error("UTF-8 error: {message}")]
    Utf8Error {
        /// Error message describing the UTF-8 error
        message: String,
    },

    /// WASI proc_exit was called with an exit code.
    /// The error message format "exit_code:{exit_code}" is parsed by Java ErrorMapper.
    #[error("exit_code:{exit_code}")]
    WasiExit {
        /// The exit code passed to proc_exit
        exit_code: i32,
    },

    /// Unsupported feature errors
    #[error("Unsupported feature: {message}")]
    UnsupportedFeature {
        /// Error message describing the unsupported feature error
        message: String,
    },

    /// WAST execution errors
    #[error("WAST execution error: {0}")]
    WastExecutionError(String),

    /// JNI-specific errors
    #[error("JNI error: {0}")]
    JniError(String),
}

impl Clone for WasmtimeError {
    fn clone(&self) -> Self {
        match self {
            WasmtimeError::Compilation { message } => WasmtimeError::Compilation {
                message: message.clone(),
            },
            WasmtimeError::Validation { message } => WasmtimeError::Validation {
                message: message.clone(),
            },
            WasmtimeError::Module { message } => WasmtimeError::Module {
                message: message.clone(),
            },
            // WasmBacktrace doesn't implement Clone, so we preserve backtrace info in the message
            WasmtimeError::Runtime { message, backtrace } => {
                let full_message = match backtrace {
                    Some(bt) => format!("{}\nBacktrace: {:?}", message, bt),
                    None => message.clone(),
                };
                WasmtimeError::Runtime {
                    message: full_message,
                    backtrace: None,
                }
            }
            WasmtimeError::EngineConfig { message } => WasmtimeError::EngineConfig {
                message: message.clone(),
            },
            WasmtimeError::Store { message } => WasmtimeError::Store {
                message: message.clone(),
            },
            WasmtimeError::Instance { message } => WasmtimeError::Instance {
                message: message.clone(),
            },
            WasmtimeError::Function { message } => WasmtimeError::Function {
                message: message.clone(),
            },
            WasmtimeError::Memory { message } => WasmtimeError::Memory {
                message: message.clone(),
            },
            WasmtimeError::Table { message } => WasmtimeError::Table {
                message: message.clone(),
            },
            WasmtimeError::Global { message } => WasmtimeError::Global {
                message: message.clone(),
            },
            WasmtimeError::Linker { message } => WasmtimeError::Linker {
                message: message.clone(),
            },
            WasmtimeError::Component { message } => WasmtimeError::Component {
                message: message.clone(),
            },
            WasmtimeError::Interface { message } => WasmtimeError::Interface {
                message: message.clone(),
            },
            WasmtimeError::Internal { message } => WasmtimeError::Internal {
                message: message.clone(),
            },
            WasmtimeError::Execution { message } => WasmtimeError::Execution {
                message: message.clone(),
            },
            WasmtimeError::ExportNotFound { name } => {
                WasmtimeError::ExportNotFound { name: name.clone() }
            }
            WasmtimeError::Multiple { summary, errors } => WasmtimeError::Multiple {
                summary: summary.clone(),
                errors: errors.clone(),
            },
            WasmtimeError::Instantiation { message } => WasmtimeError::Instantiation {
                message: message.clone(),
            },
            WasmtimeError::CallerContextError { message } => WasmtimeError::CallerContextError {
                message: message.clone(),
            },
            WasmtimeError::TypeMismatch { expected, actual } => WasmtimeError::TypeMismatch {
                expected: expected.clone(),
                actual: actual.clone(),
            },
            WasmtimeError::Utf8Error { message } => WasmtimeError::Utf8Error {
                message: message.clone(),
            },
            WasmtimeError::UnsupportedFeature { message } => WasmtimeError::UnsupportedFeature {
                message: message.clone(),
            },
            // std::io::Error doesn't implement Clone, convert to Internal variant
            WasmtimeError::Io { source } => WasmtimeError::Internal {
                message: format!("I/O error: {} (kind: {:?})", source, source.kind()),
            },
            WasmtimeError::ImportExport { message } => WasmtimeError::ImportExport {
                message: message.clone(),
            },
            WasmtimeError::Type { message } => WasmtimeError::Type {
                message: message.clone(),
            },
            WasmtimeError::Resource { message } => WasmtimeError::Resource {
                message: message.clone(),
            },
            WasmtimeError::InvalidParameter { message } => WasmtimeError::InvalidParameter {
                message: message.clone(),
            },
            WasmtimeError::Concurrency { message } => WasmtimeError::Concurrency {
                message: message.clone(),
            },
            WasmtimeError::Wasi { message } => WasmtimeError::Wasi {
                message: message.clone(),
            },
            WasmtimeError::Security { message } => WasmtimeError::Security {
                message: message.clone(),
            },
            WasmtimeError::WasiExit { exit_code } => WasmtimeError::WasiExit {
                exit_code: *exit_code,
            },
            WasmtimeError::WastExecutionError(message) => {
                WasmtimeError::WastExecutionError(message.clone())
            }
            WasmtimeError::JniError(message) => WasmtimeError::JniError(message.clone()),
        }
    }
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
    /// Security and permission violation error
    SecurityError = -16,
    /// Component model error
    ComponentError = -17,
    /// Interface definition or binding error
    InterfaceError = -18,
    /// Network operation error
    NetworkError = -19,
    /// Process execution error
    ProcessError = -20,
    /// Internal system error
    InternalError = -21,
    /// Security violation error
    SecurityViolation = -22,
    /// Invalid data format error
    InvalidData = -23,
    /// I/O operation error
    IOError = -24,
    /// Unsupported operation error
    UnsupportedOperation = -25,
    /// Operation would block (non-blocking I/O)
    WouldBlock = -26,
    /// WASI proc_exit was called
    WasiExit = -27,
    /// Out-of-bounds or invalid memory access
    MemoryAccessError = -28,
    /// GC heap out of memory
    GcHeapOom = -29,
    /// Pooling allocator concurrency limit exceeded
    PoolConcurrencyLimit = -30,
    /// Unknown import could not be resolved
    UnknownImport = -31,
    /// Resource table operation failed
    ResourceTableError = -32,
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
    /// Create error from string (migration helper for GC implementation)
    pub fn from_string(msg: impl Into<String>) -> Self {
        WasmtimeError::Runtime {
            message: msg.into(),
            backtrace: None,
        }
    }

    /// Get error message as C string for FFI
    pub fn to_c_string(&self) -> CString {
        CString::new(self.to_string()).unwrap_or_else(|_| {
            CString::new("Error message contains null bytes").unwrap_or_else(|_| CString::default())
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
            WasmtimeError::Security { .. } => ErrorCode::SecurityError,
            WasmtimeError::Component { .. } => ErrorCode::ComponentError,
            WasmtimeError::Interface { .. } => ErrorCode::InterfaceError,
            WasmtimeError::Internal { .. } => ErrorCode::InternalError,
            WasmtimeError::Execution { .. } => ErrorCode::FunctionError,
            WasmtimeError::ExportNotFound { .. } => ErrorCode::ImportExportError,
            WasmtimeError::Multiple { .. } => ErrorCode::InternalError,
            WasmtimeError::Instantiation { .. } => ErrorCode::InstanceError,
            WasmtimeError::CallerContextError { .. } => ErrorCode::FunctionError,
            WasmtimeError::TypeMismatch { .. } => ErrorCode::TypeError,
            WasmtimeError::Table { .. } => ErrorCode::RuntimeError,
            WasmtimeError::Global { .. } => ErrorCode::RuntimeError,
            WasmtimeError::Linker { .. } => ErrorCode::ImportExportError,
            WasmtimeError::Utf8Error { .. } => ErrorCode::InvalidParameterError,
            WasmtimeError::UnsupportedFeature { .. } => ErrorCode::UnsupportedOperation,
            WasmtimeError::WastExecutionError(..) => ErrorCode::ValidationError,
            WasmtimeError::JniError(..) => ErrorCode::InternalError,
            WasmtimeError::WasiExit { .. } => ErrorCode::WasiExit,
        }
    }

    /// Create from Wasmtime trap
    pub fn from_trap(trap: Trap) -> Self {
        WasmtimeError::Runtime {
            message: trap.to_string(),
            backtrace: None, // trace() method removed in wasmtime 36.0.2
        }
    }

    /// Create from a wasmtime::Error, detecting I32Exit for WASI proc_exit.
    ///
    /// This method checks if the error is a WASI I32Exit before converting to a
    /// generic Runtime error. This allows Java callers to distinguish between a
    /// clean WASI exit and a crash.
    ///
    /// When `coredump_on_trap(true)` is configured and a trap produces a WasmCoreDump,
    /// this method registers the coredump in the global registry and embeds the
    /// coredump ID as a `[coredump:ID]` prefix in the error message. The Java error
    /// mappers parse this prefix to attach the coredump ID to TrapException.
    pub fn from_wasmtime_error(error: wasmtime::Error) -> Self {
        // Check for WASI I32Exit (only when wasi feature is enabled)
        #[cfg(feature = "wasi")]
        if let Some(exit) = error.downcast_ref::<wasmtime_wasi::I32Exit>() {
            return WasmtimeError::WasiExit { exit_code: exit.0 };
        }
        // Extract trap message before potentially moving the error into the coredump registry
        let trap_msg = error
            .downcast_ref::<Trap>()
            .map(|t| format!("WebAssembly trap: {}", t));
        let has_coredump = error.downcast_ref::<wasmtime::WasmCoreDump>().is_some();

        if let Some(msg) = trap_msg {
            if has_coredump {
                // Register the error (which contains the WasmCoreDump) in the coredump registry
                let coredump_id = crate::coredump::register_error(error, msg.clone());
                return WasmtimeError::Runtime {
                    message: format!("[coredump:{}]{}", coredump_id, msg),
                    backtrace: None,
                };
            }
            return WasmtimeError::Runtime {
                message: msg,
                backtrace: None,
            };
        }
        // Generic runtime error
        WasmtimeError::Runtime {
            message: error.to_string(),
            backtrace: None,
        }
    }

    /// Create a Runtime error from a wasmtime::Error, with coredump extraction.
    ///
    /// This is a convenience function for `.map_err()` call sites that convert
    /// a wasmtime::Error into WasmtimeError::Runtime. It checks for WasmCoreDump
    /// in the error chain and registers it if present.
    pub fn runtime_from_wasmtime(error: wasmtime::Error) -> Self {
        Self::from_wasmtime_error(error)
    }

    /// Create from Wasmtime compilation error
    pub fn from_compilation_error(error: wasmtime::Error) -> Self {
        // Use anyhow's chain to get all error causes
        let mut msg = String::new();
        for (i, cause) in error.chain().enumerate() {
            if i == 0 {
                msg.push_str(&format!("{}", cause));
            } else {
                msg.push_str(&format!("\n  Caused by: {}", cause));
            }
        }
        WasmtimeError::Compilation { message: msg }
    }

    /// Create invalid parameter error with defensive checks
    pub fn invalid_parameter<S: Into<String>>(message: S) -> Self {
        WasmtimeError::InvalidParameter {
            message: message.into(),
        }
    }

    /// Create security violation error
    pub fn security_violation<S: Into<String>>(message: S) -> Self {
        WasmtimeError::Security {
            message: message.into(),
        }
    }

    /// Create resource management error
    pub fn resource_error<S: Into<String>>(message: S) -> Self {
        WasmtimeError::Resource {
            message: message.into(),
        }
    }

    /// Create aggregated error from multiple errors
    pub fn multiple<I>(errors: I) -> Self
    where
        I: IntoIterator<Item = WasmtimeError>,
    {
        let error_vec: Vec<WasmtimeError> = errors.into_iter().collect();
        let summary = if error_vec.is_empty() {
            "No errors".to_string()
        } else if error_vec.len() == 1 {
            error_vec[0].to_string()
        } else {
            format!(
                "{} errors: {}",
                error_vec.len(),
                error_vec
                    .iter()
                    .take(3)
                    .map(|e| e.to_string())
                    .collect::<Vec<_>>()
                    .join("; ")
                    + if error_vec.len() > 3 { "..." } else { "" }
            )
        };

        WasmtimeError::Multiple {
            summary,
            errors: error_vec,
        }
    }

    /// Get individual errors from aggregated error
    pub fn get_individual_errors(&self) -> Vec<&WasmtimeError> {
        match self {
            WasmtimeError::Multiple { errors, .. } => errors.iter().collect(),
            _ => vec![self],
        }
    }

    /// Check if this is an aggregated error
    pub fn is_multiple(&self) -> bool {
        matches!(self, WasmtimeError::Multiple { .. })
    }

    /// Get the count of individual errors
    pub fn error_count(&self) -> usize {
        match self {
            WasmtimeError::Multiple { errors, .. } => errors.len(),
            _ => 1,
        }
    }

    /// Log the error with appropriate level based on error type
    pub fn log_error(&self) {
        match self {
            WasmtimeError::Security { message } => {
                log::error!("SECURITY VIOLATION: {}", message);
            }
            WasmtimeError::Resource { message } => {
                log::warn!("Resource management error: {}", message);
            }
            WasmtimeError::Concurrency { message } => {
                log::error!("Concurrency error: {}", message);
            }
            WasmtimeError::Internal { message } => {
                log::error!("Internal error: {}", message);
            }
            WasmtimeError::Multiple { summary, errors } => {
                log::error!("Multiple errors occurred: {}", summary);
                for (i, error) in errors.iter().enumerate() {
                    log::error!("  Error {}: {}", i + 1, error);
                }
            }
            WasmtimeError::Compilation { message } => {
                log::warn!("WebAssembly compilation failed: {}", message);
            }
            WasmtimeError::Validation { message } => {
                log::warn!("WebAssembly validation failed: {}", message);
            }
            WasmtimeError::Runtime { message, .. } => {
                log::info!("WebAssembly runtime error: {}", message);
            }
            _ => {
                log::info!("Operation error: {}", self);
            }
        }
    }

    /// Log the error with custom context
    pub fn log_error_with_context(&self, operation: &str, context: &str) {
        match self {
            WasmtimeError::Security { .. } => {
                log::error!(
                    "SECURITY VIOLATION in '{}' [{}]: {}",
                    operation,
                    context,
                    self
                );
            }
            WasmtimeError::Internal { .. } | WasmtimeError::Concurrency { .. } => {
                log::error!("Critical error in '{}' [{}]: {}", operation, context, self);
            }
            WasmtimeError::Multiple { summary, errors } => {
                log::error!(
                    "Multiple errors in '{}' [{}]: {}",
                    operation,
                    context,
                    summary
                );
                for (i, error) in errors.iter().enumerate() {
                    log::error!("  {} Error {}: {}", context, i + 1, error);
                }
            }
            _ => {
                log::warn!("Error in '{}' [{}]: {}", operation, context, self);
            }
        }
    }
}

/// Defensive parameter validation macros
#[macro_export]
macro_rules! validate_not_null {
    ($ptr:expr, $name:expr) => {
        if $ptr.is_null() {
            return Err(WasmtimeError::invalid_parameter(format!(
                "{} cannot be null",
                $name
            )));
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
            return Err(WasmtimeError::invalid_parameter(format!(
                "Slice bounds check failed: offset {} + length {} > slice length {}",
                $offset,
                $length,
                $slice.len()
            )));
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
            return Err(WasmtimeError::InvalidParameter {
                message: format!("{} pointer cannot be null", $name),
            });
        }
    };
}

/// Validates that a pointer is not null for C functions returning c_int
///
/// This macro performs defensive validation for C functions that return
/// error codes as c_int instead of Result types.
///
/// # Returns
///
/// Returns error code as i32 if pointer is null
#[macro_export]
macro_rules! validate_ptr_not_null_c {
    ($ptr:expr, $name:expr) => {
        if $ptr.is_null() {
            log::error!("{} pointer cannot be null", $name);
            let error = WasmtimeError::InvalidParameter {
                message: format!("{} pointer cannot be null", $name),
            };
            return error.to_error_code() as i32;
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
            return Err(WasmtimeError::invalid_parameter(format!(
                "{} cannot be empty",
                $name
            )));
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
            return Err(WasmtimeError::invalid_parameter(format!(
                "{} handle is invalid",
                $name
            )));
        }
    };
}

// ============================================================================
// JNI Parameter Validation Macros
// ============================================================================
// These macros provide consistent parameter validation for JNI functions,
// reducing code duplication and ensuring consistent error handling.

/// Validates a JNI handle, throwing IllegalArgumentException if null/zero.
/// Returns the specified default value on validation failure.
///
/// # Usage
/// ```text
/// jni_validate_handle!(env, context_handle, "context", 0);
/// ```
#[macro_export]
#[cfg(feature = "jni-bindings")]
macro_rules! jni_validate_handle {
    ($env:expr, $handle:expr, $name:literal) => {
        if $handle == 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalArgumentException",
                concat!("Invalid ", $name, " handle: null"),
            );
            return Default::default();
        }
    };
    ($env:expr, $handle:expr, $name:literal, $ret:expr) => {
        if $handle == 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalArgumentException",
                concat!("Invalid ", $name, " handle: null"),
            );
            return $ret;
        }
    };
}

/// Validates multiple JNI handles at once, throwing IllegalArgumentException if any are null/zero.
///
/// # Usage
/// ```text
/// jni_validate_handles!(env, 0, context_handle => "context", stream_id => "stream");
/// ```
#[macro_export]
#[cfg(feature = "jni-bindings")]
macro_rules! jni_validate_handles {
    ($env:expr, $ret:expr, $($handle:expr => $name:literal),+ $(,)?) => {
        $(
            if $handle == 0 {
                let _ = $env.throw_new(
                    "java/lang/IllegalArgumentException",
                    concat!("Invalid ", $name, " handle: null")
                );
                return $ret;
            }
        )+
    };
}

/// Validates that a value is non-negative, throwing IllegalArgumentException if negative.
///
/// # Usage
/// ```text
/// jni_validate_non_negative!(env, offset, "Offset", 0);
/// ```
#[macro_export]
#[cfg(feature = "jni-bindings")]
macro_rules! jni_validate_non_negative {
    ($env:expr, $value:expr, $name:literal) => {
        if $value < 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalArgumentException",
                concat!($name, " cannot be negative"),
            );
            return Default::default();
        }
    };
    ($env:expr, $value:expr, $name:literal, $ret:expr) => {
        if $value < 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalArgumentException",
                concat!($name, " cannot be negative"),
            );
            return $ret;
        }
    };
}

/// Safely dereferences a JNI handle to a pointer, throwing NullPointerException if null.
/// Returns the dereferenced value on success.
///
/// # Usage
/// ```text
/// let context = jni_deref_ptr!(env, context_handle, WasiContext, "Context", 0);
/// ```
///
/// # Safety
/// The handle must have been obtained from a valid pointer allocation.
#[macro_export]
#[cfg(feature = "jni-bindings")]
macro_rules! jni_deref_ptr {
    ($env:expr, $handle:expr, $type:ty, $name:literal) => {{
        let ptr = $handle as *const $type;
        if ptr.is_null() {
            let _ = $env.throw_new(
                "java/lang/NullPointerException",
                concat!($name, " pointer is null"),
            );
            return Default::default();
        }
        unsafe { &*ptr }
    }};
    ($env:expr, $handle:expr, $type:ty, $name:literal, $ret:expr) => {{
        let ptr = $handle as *const $type;
        if ptr.is_null() {
            let _ = $env.throw_new(
                "java/lang/NullPointerException",
                concat!($name, " pointer is null"),
            );
            return $ret;
        }
        unsafe { &*ptr }
    }};
    (mut $env:expr, $handle:expr, $type:ty, $name:literal) => {{
        let ptr = $handle as *mut $type;
        if ptr.is_null() {
            let _ = $env.throw_new(
                "java/lang/NullPointerException",
                concat!($name, " pointer is null"),
            );
            return Default::default();
        }
        unsafe { &mut *ptr }
    }};
    (mut $env:expr, $handle:expr, $type:ty, $name:literal, $ret:expr) => {{
        let ptr = $handle as *mut $type;
        if ptr.is_null() {
            let _ = $env.throw_new(
                "java/lang/NullPointerException",
                concat!($name, " pointer is null"),
            );
            return $ret;
        }
        unsafe { &mut *ptr }
    }};
}

/// Validates a JNI handle and dereferences it in one step.
/// Combines jni_validate_handle and jni_deref_ptr for convenience.
///
/// # Usage
/// ```text
/// let context = jni_get_ref!(env, context_handle, WasiContext, "context", 0);
/// ```
///
/// # Safety
/// The handle must have been obtained from a valid pointer allocation.
#[macro_export]
#[cfg(feature = "jni-bindings")]
macro_rules! jni_get_ref {
    ($env:expr, $handle:expr, $type:ty, $name:literal) => {{
        if $handle == 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalArgumentException",
                concat!("Invalid ", $name, " handle: null"),
            );
            return Default::default();
        }
        let ptr = $handle as *const $type;
        if ptr.is_null() {
            let _ = $env.throw_new(
                "java/lang/NullPointerException",
                concat!(stringify!($type), " pointer is null"),
            );
            return Default::default();
        }
        unsafe { &*ptr }
    }};
    ($env:expr, $handle:expr, $type:ty, $name:literal, $ret:expr) => {{
        if $handle == 0 {
            let _ = $env.throw_new(
                "java/lang/IllegalArgumentException",
                concat!("Invalid ", $name, " handle: null"),
            );
            return $ret;
        }
        let ptr = $handle as *const $type;
        if ptr.is_null() {
            let _ = $env.throw_new(
                "java/lang/NullPointerException",
                concat!(stringify!($type), " pointer is null"),
            );
            return $ret;
        }
        unsafe { &*ptr }
    }};
}

/// Handles a Result by throwing a JNI exception on error and returning a default value.
///
/// # Usage
/// ```text
/// let value = jni_try!(env, some_operation(), 0);
/// ```
#[macro_export]
#[cfg(feature = "jni-bindings")]
macro_rules! jni_try {
    ($env:expr, $result:expr) => {
        match $result {
            Ok(val) => val,
            Err(e) => {
                let _ = $env.throw_new(
                    "ai/tegmentum/wasmtime4j/exception/WasmException",
                    format!("{}", e),
                );
                return Default::default();
            }
        }
    };
    ($env:expr, $result:expr, $ret:expr) => {
        match $result {
            Ok(val) => val,
            Err(e) => {
                let _ = $env.throw_new(
                    "ai/tegmentum/wasmtime4j/exception/WasmException",
                    format!("{}", e),
                );
                return $ret;
            }
        }
    };
    ($env:expr, $result:expr, $exception_class:literal, $ret:expr) => {
        match $result {
            Ok(val) => val,
            Err(e) => {
                let _ = $env.throw_new($exception_class, format!("{}", e));
                return $ret;
            }
        }
    };
}
