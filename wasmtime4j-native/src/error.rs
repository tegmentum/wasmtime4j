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

    /// Security and permission violations
    #[error("Security error: {message}")]
    Security {
        /// Error message describing the security violation
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

    /// Network-related errors
    #[error("Network error: {message}")]
    Network {
        /// Error message describing the network error
        message: String,
    },

    /// Process-related errors
    #[error("Process error: {message}")]
    Process {
        /// Error message describing the process error
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

    /// Function execution errors
    #[error("Execution error: {message}")]
    ExecutionError {
        /// Error message describing the execution error
        message: String,
    },

    /// Serialization/deserialization errors
    #[error("Serialization error: {message}")]
    SerializationError {
        /// Error message describing the serialization error
        message: String,
    },

    /// Invalid state errors
    #[error("Invalid state: {message}")]
    InvalidState {
        /// Error message describing the invalid state
        message: String,
    },

    /// Validation errors
    #[error("Validation error: {message}")]
    ValidationError {
        /// Error message describing the validation error
        message: String,
    },

    /// Runtime errors
    #[error("Runtime error: {message}")]
    RuntimeError {
        /// Error message describing the runtime error
        message: String,
    },

    /// Invalid data errors
    #[error("Invalid data: {message}")]
    InvalidData {
        /// Error message describing the invalid data error
        message: String,
    },

    /// I/O errors with custom message
    #[error("I/O error: {message}")]
    IO {
        /// Error message describing the I/O error
        message: String,
    },

    /// UTF-8 encoding errors
    #[error("UTF-8 error: {message}")]
    Utf8Error {
        /// Error message describing the UTF-8 error
        message: String,
    },

    /// Invalid operation errors
    #[error("Invalid operation: {message}")]
    InvalidOperation {
        /// Error message describing the invalid operation
        message: String,
    },

    /// Access denied errors
    #[error("Access denied: {message}")]
    AccessDenied {
        /// Error message describing the access denied error
        message: String,
    },

    /// Timeout errors
    #[error("Timeout: {message}")]
    Timeout {
        /// Error message describing the timeout error
        message: String,
    },

    /// Resource limit errors
    #[error("Resource limit: {message}")]
    ResourceLimit {
        /// Error message describing the resource limit error
        message: String,
    },

    /// Cryptographic errors
    #[error("Cryptographic error: {message}")]
    Cryptographic {
        /// Error message describing the cryptographic error
        message: String,
    },

    /// Serialization errors (alternative naming)
    #[error("Serialization error: {message}")]
    Serialization {
        /// Error message describing the serialization error
        message: String,
    },

    /// Quota exceeded errors
    #[error("Quota exceeded: {message}")]
    QuotaExceeded {
        /// Error message describing the quota exceeded error
        message: String,
    },

    /// Unsupported feature errors
    #[error("Unsupported feature: {message}")]
    UnsupportedFeature {
        /// Error message describing the unsupported feature error
        message: String,
    },

    /// System errors
    #[error("System error: {message}")]
    SystemError {
        /// Error message describing the system error
        message: String,
    },

    /// Compilation errors (alternative naming)
    #[error("Compilation error: {message}")]
    CompilationError {
        /// Error message describing the compilation error
        message: String,
    },

    /// Deadlock detection error
    #[error("Operation would cause deadlock: {message}")]
    WouldDeadlock {
        /// Error message describing the potential deadlock
        message: String,
    },
}

impl Clone for WasmtimeError {
    fn clone(&self) -> Self {
        match self {
            WasmtimeError::Compilation { message } => WasmtimeError::Compilation { message: message.clone() },
            WasmtimeError::Validation { message } => WasmtimeError::Validation { message: message.clone() },
            WasmtimeError::Module { message } => WasmtimeError::Module { message: message.clone() },
            WasmtimeError::Runtime { message, backtrace: _ } => WasmtimeError::Runtime { message: message.clone(), backtrace: None },
            WasmtimeError::EngineConfig { message } => WasmtimeError::EngineConfig { message: message.clone() },
            WasmtimeError::Store { message } => WasmtimeError::Store { message: message.clone() },
            WasmtimeError::Instance { message } => WasmtimeError::Instance { message: message.clone() },
            WasmtimeError::Function { message } => WasmtimeError::Function { message: message.clone() },
            WasmtimeError::Memory { message } => WasmtimeError::Memory { message: message.clone() },
            WasmtimeError::Table { message } => WasmtimeError::Table { message: message.clone() },
            WasmtimeError::Global { message } => WasmtimeError::Global { message: message.clone() },
            WasmtimeError::Linker { message } => WasmtimeError::Linker { message: message.clone() },
            WasmtimeError::Component { message } => WasmtimeError::Component { message: message.clone() },
            WasmtimeError::Interface { message } => WasmtimeError::Interface { message: message.clone() },
            WasmtimeError::Internal { message } => WasmtimeError::Internal { message: message.clone() },
            WasmtimeError::Execution { message } => WasmtimeError::Execution { message: message.clone() },
            WasmtimeError::ExportNotFound { name } => WasmtimeError::ExportNotFound { name: name.clone() },
            WasmtimeError::Multiple { summary, errors } => WasmtimeError::Multiple { summary: summary.clone(), errors: errors.clone() },
            WasmtimeError::Instantiation { message } => WasmtimeError::Instantiation { message: message.clone() },
            WasmtimeError::Network { message } => WasmtimeError::Network { message: message.clone() },
            WasmtimeError::Process { message } => WasmtimeError::Process { message: message.clone() },
            WasmtimeError::CallerContextError { message } => WasmtimeError::CallerContextError { message: message.clone() },
            WasmtimeError::TypeMismatch { expected, actual } => WasmtimeError::TypeMismatch { expected: expected.clone(), actual: actual.clone() },
            WasmtimeError::ExecutionError { message } => WasmtimeError::ExecutionError { message: message.clone() },
            WasmtimeError::SerializationError { message } => WasmtimeError::SerializationError { message: message.clone() },
            WasmtimeError::InvalidState { message } => WasmtimeError::InvalidState { message: message.clone() },
            WasmtimeError::ValidationError { message } => WasmtimeError::ValidationError { message: message.clone() },
            WasmtimeError::RuntimeError { message } => WasmtimeError::RuntimeError { message: message.clone() },
            WasmtimeError::InvalidData { message } => WasmtimeError::InvalidData { message: message.clone() },
            WasmtimeError::IO { message } => WasmtimeError::IO { message: message.clone() },
            WasmtimeError::Utf8Error { message } => WasmtimeError::Utf8Error { message: message.clone() },
            WasmtimeError::InvalidOperation { message } => WasmtimeError::InvalidOperation { message: message.clone() },
            WasmtimeError::AccessDenied { message } => WasmtimeError::AccessDenied { message: message.clone() },
            WasmtimeError::Timeout { message } => WasmtimeError::Timeout { message: message.clone() },
            WasmtimeError::ResourceLimit { message } => WasmtimeError::ResourceLimit { message: message.clone() },
            WasmtimeError::Cryptographic { message } => WasmtimeError::Cryptographic { message: message.clone() },
            WasmtimeError::Serialization { message } => WasmtimeError::Serialization { message: message.clone() },
            WasmtimeError::QuotaExceeded { message } => WasmtimeError::QuotaExceeded { message: message.clone() },
            WasmtimeError::UnsupportedFeature { message } => WasmtimeError::UnsupportedFeature { message: message.clone() },
            WasmtimeError::SystemError { message } => WasmtimeError::SystemError { message: message.clone() },
            WasmtimeError::CompilationError { message } => WasmtimeError::CompilationError { message: message.clone() },
            // Handle the special Io variant with source field
            WasmtimeError::Io { source } => WasmtimeError::InvalidParameter { message: format!("I/O error: {}", source) },
            // Handle missing Clone cases for existing variants
            WasmtimeError::ImportExport { message } => WasmtimeError::ImportExport { message: message.clone() },
            WasmtimeError::Type { message } => WasmtimeError::Type { message: message.clone() },
            WasmtimeError::Resource { message } => WasmtimeError::Resource { message: message.clone() },
            WasmtimeError::InvalidParameter { message } => WasmtimeError::InvalidParameter { message: message.clone() },
            WasmtimeError::Concurrency { message } => WasmtimeError::Concurrency { message: message.clone() },
            WasmtimeError::Wasi { message } => WasmtimeError::Wasi { message: message.clone() },
            WasmtimeError::Security { message } => WasmtimeError::Security { message: message.clone() },
            WasmtimeError::WouldDeadlock { message } => WasmtimeError::WouldDeadlock { message: message.clone() },
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
            WasmtimeError::Security { .. } => ErrorCode::SecurityError,
            WasmtimeError::Component { .. } => ErrorCode::ComponentError,
            WasmtimeError::Interface { .. } => ErrorCode::InterfaceError,
            WasmtimeError::Internal { .. } => ErrorCode::InternalError,
            WasmtimeError::Execution { .. } => ErrorCode::FunctionError,
            WasmtimeError::ExportNotFound { .. } => ErrorCode::ImportExportError,
            WasmtimeError::Multiple { .. } => ErrorCode::InternalError,
            WasmtimeError::Instantiation { .. } => ErrorCode::InstanceError,
            WasmtimeError::Network { .. } => ErrorCode::NetworkError,
            WasmtimeError::Process { .. } => ErrorCode::ProcessError,
            WasmtimeError::CallerContextError { .. } => ErrorCode::FunctionError,
            WasmtimeError::TypeMismatch { .. } => ErrorCode::TypeError,
            WasmtimeError::ExecutionError { .. } => ErrorCode::FunctionError,
            WasmtimeError::SerializationError { .. } => ErrorCode::InternalError,
            WasmtimeError::InvalidState { .. } => ErrorCode::InternalError,
            WasmtimeError::ValidationError { .. } => ErrorCode::ValidationError,
            WasmtimeError::RuntimeError { .. } => ErrorCode::RuntimeError,
            WasmtimeError::Table { .. } => ErrorCode::RuntimeError,
            WasmtimeError::Global { .. } => ErrorCode::RuntimeError,
            WasmtimeError::Linker { .. } => ErrorCode::ImportExportError,
            WasmtimeError::InvalidData { .. } => ErrorCode::InvalidParameterError,
            WasmtimeError::IO { .. } => ErrorCode::IoError,
            WasmtimeError::Utf8Error { .. } => ErrorCode::InvalidParameterError,
            WasmtimeError::InvalidOperation { .. } => ErrorCode::InvalidParameterError,
            WasmtimeError::AccessDenied { .. } => ErrorCode::SecurityViolation,
            WasmtimeError::Timeout { .. } => ErrorCode::RuntimeError,
            WasmtimeError::ResourceLimit { .. } => ErrorCode::ResourceError,
            WasmtimeError::Cryptographic { .. } => ErrorCode::SecurityViolation,
            WasmtimeError::Serialization { .. } => ErrorCode::InternalError,
            WasmtimeError::QuotaExceeded { .. } => ErrorCode::ResourceError,
            WasmtimeError::UnsupportedFeature { .. } => ErrorCode::UnsupportedOperation,
            WasmtimeError::SystemError { .. } => ErrorCode::InternalError,
            WasmtimeError::CompilationError { .. } => ErrorCode::CompilationError,
            WasmtimeError::WouldDeadlock { .. } => ErrorCode::ConcurrencyError,
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
            format!("{} errors: {}", error_vec.len(),
                error_vec.iter()
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
                log::error!("SECURITY VIOLATION in '{}' [{}]: {}", operation, context, self);
            }
            WasmtimeError::Internal { .. } | WasmtimeError::Concurrency { .. } => {
                log::error!("Critical error in '{}' [{}]: {}", operation, context, self);
            }
            WasmtimeError::Multiple { summary, errors } => {
                log::error!("Multiple errors in '{}' [{}]: {}", operation, context, summary);
                for (i, error) in errors.iter().enumerate() {
                    log::error!("  {} Error {}: {}", context, i + 1, error);
                }
            }
            _ => {
                log::warn!("Error in '{}' [{}]: {}", operation, context, self);
            }
        }
    }

    /// Get performance metrics for the error (how long it might take to recover)
    pub fn get_recovery_time_estimate(&self) -> std::time::Duration {
        match self {
            WasmtimeError::Compilation { .. } | WasmtimeError::Validation { .. } => {
                // Compilation/validation errors require module reload - significant time
                std::time::Duration::from_millis(100)
            }
            WasmtimeError::Runtime { .. } | WasmtimeError::Function { .. } => {
                // Runtime errors often recoverable quickly
                std::time::Duration::from_millis(10)
            }
            WasmtimeError::Memory { .. } | WasmtimeError::Resource { .. } => {
                // Memory/resource errors may require GC - moderate time
                std::time::Duration::from_millis(50)
            }
            WasmtimeError::Security { .. } => {
                // Security errors require careful recovery - significant time
                std::time::Duration::from_millis(200)
            }
            WasmtimeError::Multiple { errors, .. } => {
                // Multiple errors - sum of individual recovery times
                errors.iter()
                    .map(|e| e.get_recovery_time_estimate())
                    .fold(std::time::Duration::ZERO, |acc, time| acc + time)
            }
            _ => {
                // Default recovery time
                std::time::Duration::from_millis(25)
            }
        }
    }

    /// Enhance compilation error message with additional context
    pub fn enhance_compilation_error_message(base_message: &str) -> Self {
        WasmtimeError::Compilation {
            message: format!("Enhanced compilation error: {}", base_message),
        }
    }

    /// Enhance runtime error message with additional context
    pub fn enhance_runtime_error_message(base_message: &str) -> Self {
        WasmtimeError::Runtime {
            message: format!("Enhanced runtime error: {}", base_message),
            backtrace: None,
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
            return Err(WasmtimeError::InvalidParameter {
                message: format!("{} pointer cannot be null", $name)
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
                message: format!("{} pointer cannot be null", $name)
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

    /// Enhanced error context for better diagnostics and debugging
    #[derive(Debug, Clone)]
    pub struct ErrorContext {
        pub error: WasmtimeError,
        pub operation: Option<String>,
        pub file: Option<String>,
        pub line: Option<u32>,
        pub timestamp: std::time::Instant,
        pub thread_id: String,
        pub stack_trace: Option<String>,
    }

    impl ErrorContext {
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

        pub fn with_operation(mut self, operation: String) -> Self {
            self.operation = Some(operation);
            self
        }

        pub fn with_location(mut self, file: String, line: u32) -> Self {
            self.file = Some(file);
            self.line = Some(line);
            self
        }

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
    
    /// Execute operation with FFI error handling, returning pointer for success
    pub fn ffi_try_ptr<F, T>(operation: F) -> *mut c_void
    where
        F: FnOnce() -> WasmtimeResult<Box<T>>,
    {
        match operation() {
            Ok(result) => {
                clear_last_error();
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
                clear_last_error();
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
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = Box::from_raw(ptr as *mut T);
        }));

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
            WasmtimeError::Compilation { .. } => "ai/tegmentum/wasmtime4j/exception/CompilationException",
            WasmtimeError::Validation { .. } => "ai/tegmentum/wasmtime4j/exception/ValidationException",
            WasmtimeError::Runtime { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Memory { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Function { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Type { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::InvalidParameter { .. } => "java/lang/IllegalArgumentException",
            WasmtimeError::Io { .. } => "java/io/IOException",
            WasmtimeError::Component { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Interface { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::EngineConfig { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Store { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Instance { .. } => "ai/tegmentum/wasmtime4j/exception/InstantiationException",
            WasmtimeError::Instantiation { .. } => "ai/tegmentum/wasmtime4j/exception/LinkingException",
            WasmtimeError::ImportExport { .. } => "ai/tegmentum/wasmtime4j/exception/LinkingException",
            WasmtimeError::Resource { .. } => "ai/tegmentum/wasmtime4j/exception/ResourceException",
            WasmtimeError::Concurrency { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Wasi { .. } => "ai/tegmentum/wasmtime4j/exception/WasiException",
            WasmtimeError::Security { .. } => "ai/tegmentum/wasmtime4j/exception/SecurityException",
            WasmtimeError::Internal { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::Execution { .. } => "ai/tegmentum/wasmtime4j/exception/TrapException",
            WasmtimeError::ExportNotFound { .. } => "ai/tegmentum/wasmtime4j/exception/LinkingException",
            WasmtimeError::Multiple { .. } => "ai/tegmentum/wasmtime4j/exception/WasmException",
            WasmtimeError::TypeMismatch { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::ExecutionError { .. } => "ai/tegmentum/wasmtime4j/exception/TrapException",
            WasmtimeError::SerializationError { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::InvalidState { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            WasmtimeError::ValidationError { .. } => "ai/tegmentum/wasmtime4j/exception/ValidationException",
            WasmtimeError::RuntimeError { .. } => "ai/tegmentum/wasmtime4j/exception/WasmRuntimeException",
            _ => "ai/tegmentum/wasmtime4j/exception/WasmException",
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Throw JNI exception with proper error information
    pub fn throw_jni_exception(env: &mut jni::JNIEnv, error: &WasmtimeError) {
        use jni::objects::{JObject, JValue};
        use jni::signature::{Primitive, ReturnType};

        let class_name = error_to_exception_class(error);
        let message = error.to_string();

        // Handle exceptions that require enum parameters
        let result = match error {
            WasmtimeError::ImportExport { .. } | WasmtimeError::ExportNotFound { .. } | WasmtimeError::Instantiation { .. } => {
                // LinkingException requires LinkingErrorType enum
                throw_linking_exception(env, &message)
            }
            WasmtimeError::Execution { .. } | WasmtimeError::ExecutionError { .. } => {
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
            log::error!("Failed to throw specific exception {}: {:?}, using RuntimeException", class_name, e);
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
        let error_type_class = env.find_class("ai/tegmentum/wasmtime4j/exception/LinkingException$LinkingErrorType")?;
        let unknown_field = env.get_static_field(
            error_type_class,
            "UNKNOWN",
            "Lai/tegmentum/wasmtime4j/exception/LinkingException$LinkingErrorType;"
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
        let error_type_class = env.find_class("ai/tegmentum/wasmtime4j/exception/TrapException$TrapType")?;
        let unknown_field = env.get_static_field(
            error_type_class,
            "UNKNOWN",
            "Lai/tegmentum/wasmtime4j/exception/TrapException$TrapType;"
        )?;

        // Create TrapException(TrapType, String)
        let exception_class = env.find_class("ai/tegmentum/wasmtime4j/exception/TrapException")?;
        let message_jstring = env.new_string(message)?;

        let exception_obj = env.new_object(
            exception_class,
            "(Lai/tegmentum/wasmtime4j/exception/TrapException$TrapType;Ljava/lang/String;)V",
            &[(&unknown_field).into(), JValue::Object(&message_jstring)]
        )?;

        env.throw(JThrowable::from(exception_obj))
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning pointer for success
    pub fn jni_try_ptr<F, T>(env: &mut jni::JNIEnv, operation: F) -> *mut c_void
    where
        F: FnOnce() -> WasmtimeResult<Box<T>>,
    {
        match operation() {
            Ok(result) => {
                clear_last_error();
                Box::into_raw(result) as *mut c_void
            }
            Err(error) => {
                throw_jni_exception(env, &error);
                std::ptr::null_mut()
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning error code
    pub fn jni_try_code<F>(env: &mut jni::JNIEnv, operation: F) -> i32
    where
        F: FnOnce() -> WasmtimeResult<()>,
    {
        match operation() {
            Ok(()) => {
                clear_last_error();
                ErrorCode::Success as i32
            }
            Err(error) => {
                throw_jni_exception(env, &error);
                error.to_error_code() as i32
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning typed result
    pub fn jni_try<F, T>(env: &mut jni::JNIEnv, operation: F) -> (ErrorCode, T)
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
                throw_jni_exception(env, &error);
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
    pub fn jni_try_bool<F>(env: &mut jni::JNIEnv, operation: F) -> bool
    where
        F: FnOnce() -> WasmtimeResult<bool>,
    {
        match operation() {
            Ok(result) => {
                clear_last_error();
                result
            }
            Err(error) => {
                throw_jni_exception(env, &error);
                false
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning default value on error
    pub fn jni_try_with_default<F, T>(env: &mut jni::JNIEnv, default_value: T, operation: F) -> T
    where
        F: FnOnce() -> WasmtimeResult<T>,
    {
        match operation() {
            Ok(result) => {
                clear_last_error();
                result
            }
            Err(error) => {
                throw_jni_exception(env, &error);
                default_value
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning void
    pub fn jni_try_void<F>(env: &mut jni::JNIEnv, operation: F)
    where
        F: FnOnce() -> WasmtimeResult<()>,
    {
        match operation() {
            Ok(()) => {
                clear_last_error();
            }
            Err(error) => {
                throw_jni_exception(env, &error);
            }
        }
    }

    #[cfg(feature = "jni-bindings")]
    /// Execute operation with JNI exception throwing, returning JNI object as raw jobject
    pub fn jni_try_object<F>(env: &mut jni::JNIEnv, operation: F) -> jni::sys::jobject
    where
        F: FnOnce(&mut jni::JNIEnv) -> WasmtimeResult<jni::objects::JObject<'static>>,
    {
        match operation(env) {
            Ok(result) => {
                clear_last_error();
                result.as_raw()
            }
            Err(error) => {
                throw_jni_exception(env, &error);
                std::ptr::null_mut()
            }
        }
    }

    /// Convert error to error code for FFI interfaces
    pub fn error_to_code(error: &WasmtimeError) -> i32 {
        error.to_error_code() as i32
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

    #[test]
    fn test_thread_safety_error_handling() {
        use ffi_utils::*;
        use std::thread;
        use std::sync::Arc;
        use std::sync::atomic::{AtomicUsize, Ordering};

        // Test concurrent error setting and retrieval across multiple threads
        let error_count = Arc::new(AtomicUsize::new(0));
        let success_count = Arc::new(AtomicUsize::new(0));

        let handles: Vec<_> = (0..10).map(|i| {
            let error_count = Arc::clone(&error_count);
            let success_count = Arc::clone(&success_count);

            thread::spawn(move || {
                // Each thread performs multiple error operations
                for j in 0..100 {
                    // Set an error
                    let result: (ErrorCode, ()) = ffi_try(|| {
                        if (i + j) % 3 == 0 {
                            Err(WasmtimeError::InvalidParameter {
                                message: format!("Thread {} iteration {}", i, j)
                            })
                        } else {
                            Ok(())
                        }
                    });

                    match result.0 {
                        ErrorCode::Success => {
                            success_count.fetch_add(1, Ordering::SeqCst);
                            // Verify no error is pending
                            assert!(!has_pending_error());
                        }
                        ErrorCode::InvalidParameterError => {
                            error_count.fetch_add(1, Ordering::SeqCst);
                            // Verify error is pending
                            assert!(has_pending_error());

                            // Get error stats
                            let (has_error, stats) = get_error_stats();
                            assert!(has_error);
                            assert!(stats.contains("Thread"));

                            // Clear error
                            clear_last_error();
                            assert!(!has_pending_error());
                        }
                        _ => panic!("Unexpected error code: {:?}", result.0),
                    }
                }
            })
        }).collect();

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }

        let total_errors = error_count.load(Ordering::SeqCst);
        let total_success = success_count.load(Ordering::SeqCst);

        // Verify that we processed all operations correctly
        assert_eq!(total_errors + total_success, 1000);

        // Each thread runs 100 iterations, every 3rd should be an error
        // So we expect approximately 333 errors
        assert!(total_errors >= 300 && total_errors <= 350);
    }

    #[test]
    fn test_panic_safety_error_handling() {
        use ffi_utils::*;

        // Test that error handling doesn't panic under stress
        for _ in 0..1000 {
            let result: (ErrorCode, ()) = ffi_try(|| {
                Err(WasmtimeError::Internal {
                    message: "Stress test error".to_string(),
                })
            });

            assert!(matches!(result.0, ErrorCode::InternalError));

            let message = get_last_error_message();
            assert!(!message.is_null());

            unsafe {
                free_error_message(message);
            }

            clear_last_error();
            assert!(!has_pending_error());
        }
    }

    #[test]
    fn test_concurrent_resource_management() {
        use ffi_utils::*;
        use std::thread;
        use std::sync::Arc;

        let handles: Vec<_> = (0..5).map(|i| {
            thread::spawn(move || {
                // Test resource registration and cleanup
                for j in 0..20 {
                    let test_data = format!("Thread {} Data {}", i, j);

                    match register_resource(test_data.clone()) {
                        Ok(handle) => {
                            // Verify resource can be retrieved
                            match get_resource::<String>(handle) {
                                Ok(arc_mutex) => {
                                    let data = arc_mutex.lock().unwrap();
                                    assert_eq!(*data, test_data);
                                }
                                Err(e) => panic!("Failed to get resource: {:?}", e),
                            }

                            // Clean up resource
                            match unregister_resource(handle) {
                                Ok(_) => {}
                                Err(e) => panic!("Failed to unregister resource: {:?}", e),
                            }
                        }
                        Err(e) => panic!("Failed to register resource: {:?}", e),
                    }
                }
            })
        }).collect();

        for handle in handles {
            handle.join().unwrap();
        }
    }

    #[test]
    fn test_error_aggregation() {
        use ffi_utils::*;

        // Test ErrorCollector functionality
        let mut collector = ErrorCollector::with_operation("test_batch_operation".to_string());

        // Add multiple errors
        collector.add_error(WasmtimeError::Compilation {
            message: "Compilation failed".to_string()
        });
        collector.add_error(WasmtimeError::Validation {
            message: "Validation failed".to_string()
        });

        assert!(collector.has_errors());
        assert_eq!(collector.error_count(), 2);

        // Convert to result
        let result = collector.into_result();
        assert!(result.is_err());

        if let Err(WasmtimeError::Multiple { summary, errors }) = result {
            assert!(summary.contains("test_batch_operation"));
            assert_eq!(errors.len(), 2);
        } else {
            panic!("Expected Multiple error");
        }
    }

    #[test]
    fn test_error_aggregation_single_error() {
        let error = WasmtimeError::Runtime {
            message: "Single error".to_string(),
            backtrace: None,
        };

        let multiple = WasmtimeError::multiple(vec![error.clone()]);
        assert!(multiple.is_multiple());
        assert_eq!(multiple.error_count(), 1);

        let individual_errors = multiple.get_individual_errors();
        assert_eq!(individual_errors.len(), 1);
    }

    #[test]
    fn test_error_recovery_time_estimates() {
        let compilation_error = WasmtimeError::Compilation {
            message: "Test".to_string()
        };
        let runtime_error = WasmtimeError::Runtime {
            message: "Test".to_string(),
            backtrace: None,
        };

        // Compilation errors should take longer to recover
        assert!(compilation_error.get_recovery_time_estimate() > runtime_error.get_recovery_time_estimate());

        // Multiple errors should sum recovery times
        let multiple = WasmtimeError::multiple(vec![compilation_error.clone(), runtime_error.clone()]);
        let expected_time = compilation_error.get_recovery_time_estimate() + runtime_error.get_recovery_time_estimate();
        assert_eq!(multiple.get_recovery_time_estimate(), expected_time);
    }

    #[test]
    fn test_try_all_operations() {
        use ffi_utils::*;

        // Test successful operations
        let operations = vec![
            || -> WasmtimeResult<i32> { Ok(1) },
            || -> WasmtimeResult<i32> { Ok(2) },
            || -> WasmtimeResult<i32> { Ok(3) },
        ];

        let result = try_all(operations);
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), vec![1, 2, 3]);

        // Test operations with failures
        let operations_with_failures = vec![
            || -> WasmtimeResult<i32> { Ok(1) },
            || -> WasmtimeResult<i32> { Err(WasmtimeError::Runtime {
                message: "Error 1".to_string(),
                backtrace: None,
            }) },
            || -> WasmtimeResult<i32> { Ok(3) },
            || -> WasmtimeResult<i32> { Err(WasmtimeError::Validation {
                message: "Error 2".to_string(),
            }) },
        ];

        let result = try_all(operations_with_failures);
        assert!(result.is_err());

        if let Err(WasmtimeError::Multiple { errors, .. }) = result {
            assert_eq!(errors.len(), 2);
        } else {
            panic!("Expected Multiple error");
        }
    }

    #[test]
    fn test_try_all_continue_operations() {
        use ffi_utils::*;

        let operations = vec![
            || -> WasmtimeResult<i32> { Ok(1) },
            || -> WasmtimeResult<i32> { Err(WasmtimeError::Runtime {
                message: "Error 1".to_string(),
                backtrace: None,
            }) },
            || -> WasmtimeResult<i32> { Ok(3) },
            || -> WasmtimeResult<i32> { Err(WasmtimeError::Validation {
                message: "Error 2".to_string(),
            }) },
        ];

        let (results, error) = try_all_continue(operations);
        assert_eq!(results, vec![1, 3]); // Only successful results
        assert!(error.is_some());

        if let Some(WasmtimeError::Multiple { errors, .. }) = error {
            assert_eq!(errors.len(), 2);
        } else {
            panic!("Expected Multiple error");
        }
    }

    #[test]
    fn test_defensive_error_handling() {
        use ffi_utils::*;

        // Test that error handling functions are defensive against invalid inputs
        unsafe {
            free_error_message(std::ptr::null_mut()); // Should not crash
        }

        // Test stats functions don't panic
        let (has_error, stats) = get_error_stats();
        assert!(!has_error);
        assert!(stats.contains("No error"));

        assert!(!has_pending_error());

        // Set and clear error multiple times
        for _ in 0..10 {
            let result: (ErrorCode, ()) = ffi_try(|| {
                Err(WasmtimeError::Validation {
                    message: "Test validation error".to_string(),
                })
            });
            assert!(matches!(result.0, ErrorCode::ValidationError));
            assert!(has_pending_error());

            clear_last_error();
            assert!(!has_pending_error());
        }
    }
}