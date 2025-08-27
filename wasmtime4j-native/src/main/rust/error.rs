//! Error handling for the native library

use thiserror::Error;

/// Native library errors
#[derive(Error, Debug)]
pub enum WasmtimeError {
    /// Compilation errors
    #[error("Compilation error: {0}")]
    Compilation(String),

    /// Runtime errors
    #[error("Runtime error: {0}")]
    Runtime(String),

    /// Validation errors
    #[error("Validation error: {0}")]
    Validation(String),

    /// I/O errors
    #[error("I/O error: {0}")]
    Io(#[from] std::io::Error),

    /// Generic errors
    #[error("Generic error: {0}")]
    Generic(String),
}

/// Result type for the native library
pub type WasmtimeResult<T> = Result<T, WasmtimeError>;

/// Convert errors to error codes for C FFI
pub fn error_to_code(error: &WasmtimeError) -> i32 {
    match error {
        WasmtimeError::Compilation(_) => -1,
        WasmtimeError::Runtime(_) => -2,
        WasmtimeError::Validation(_) => -3,
        WasmtimeError::Io(_) => -4,
        WasmtimeError::Generic(_) => -5,
    }
}