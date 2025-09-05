//! Common FFI utilities shared between JNI and Panama implementations.
//!
//! This module provides simple utility functions to eliminate code duplication
//! while maintaining clear interfaces and compilation integrity.

/// Parameter conversion utilities for FFI operations
pub mod parameter_conversion {
    //! Utilities for converting parameters between FFI interfaces and internal types.
    //!
    //! This module provides functions to convert engine configurations, module parameters,
    //! and store parameters consistently across both JNI and Panama implementations.
    
    use wasmtime::{Strategy, OptLevel};
    
    /// Convert integer strategy value to wasmtime Strategy enum
    /// 
    /// # Arguments
    /// * `strategy` - Integer value representing compilation strategy (0 = Cranelift)
    /// 
    /// # Returns
    /// * `Some(Strategy::Cranelift)` for value 0
    /// * `None` for all other values
    pub fn convert_strategy(strategy: i32) -> Option<Strategy> {
        match strategy {
            0 => Some(Strategy::Cranelift),
            _ => None,
        }
    }
    
    /// Convert integer optimization level to wasmtime OptLevel enum
    /// 
    /// # Arguments  
    /// * `opt_level` - Integer value representing optimization level
    ///   - 0 = None (no optimization)
    ///   - 1 = Speed (optimize for speed)
    ///   - 2 = SpeedAndSize (optimize for speed and size)
    /// 
    /// # Returns
    /// * `Some(OptLevel)` for valid values 0-2
    /// * `None` for invalid values
    pub fn convert_opt_level(opt_level: i32) -> Option<OptLevel> {
        match opt_level {
            0 => Some(OptLevel::None),
            1 => Some(OptLevel::Speed), 
            2 => Some(OptLevel::SpeedAndSize),
            _ => None,
        }
    }
    
    /// Convert integer to optional u32, treating negative values as None
    /// 
    /// # Arguments
    /// * `value` - Integer value to convert
    /// 
    /// # Returns
    /// * `Some(value as u32)` for non-negative values
    /// * `None` for negative values
    pub fn convert_int_to_optional_u32(value: i32) -> Option<u32> {
        if value < 0 {
            None
        } else {
            Some(value as u32)
        }
    }
    
    /// Convert integer to optional usize, treating negative values as None
    /// 
    /// # Arguments
    /// * `value` - Integer value to convert
    /// 
    /// # Returns
    /// * `Some(value as usize)` for non-negative values
    /// * `None` for negative values
    pub fn convert_int_to_optional_usize(value: i32) -> Option<usize> {
        if value < 0 {
            None
        } else {
            Some(value as usize)
        }
    }
    
    /// Convert integer boolean (0/1) to Rust boolean
    /// 
    /// # Arguments
    /// * `value` - Integer value to convert (0 = false, non-zero = true)
    /// 
    /// # Returns
    /// * `true` for non-zero values
    /// * `false` for zero values
    pub fn convert_int_to_bool(value: i32) -> bool {
        value != 0
    }
}

#[cfg(test)]
mod tests {
    use super::parameter_conversion::*;
    use wasmtime::{Strategy, OptLevel};
    
    #[test]
    fn test_strategy_conversion() {
        assert_eq!(convert_strategy(0), Some(Strategy::Cranelift));
        assert_eq!(convert_strategy(1), None);
        assert_eq!(convert_strategy(-1), None);
        assert_eq!(convert_strategy(999), None);
    }
    
    #[test]
    fn test_opt_level_conversion() {
        assert_eq!(convert_opt_level(0), Some(OptLevel::None));
        assert_eq!(convert_opt_level(1), Some(OptLevel::Speed));
        assert_eq!(convert_opt_level(2), Some(OptLevel::SpeedAndSize));
        assert_eq!(convert_opt_level(3), None);
        assert_eq!(convert_opt_level(-1), None);
    }
    
    #[test]
    fn test_int_to_optional_u32_conversion() {
        assert_eq!(convert_int_to_optional_u32(0), Some(0));
        assert_eq!(convert_int_to_optional_u32(123), Some(123));
        assert_eq!(convert_int_to_optional_u32(-1), None);
        assert_eq!(convert_int_to_optional_u32(-999), None);
    }
    
    #[test]
    fn test_int_to_optional_usize_conversion() {
        assert_eq!(convert_int_to_optional_usize(0), Some(0));
        assert_eq!(convert_int_to_optional_usize(456), Some(456));
        assert_eq!(convert_int_to_optional_usize(-1), None);
        assert_eq!(convert_int_to_optional_usize(-789), None);
    }
    
    #[test]
    fn test_int_to_bool_conversion() {
        assert_eq!(convert_int_to_bool(0), false);
        assert_eq!(convert_int_to_bool(1), true);
        assert_eq!(convert_int_to_bool(-1), true);
        assert_eq!(convert_int_to_bool(999), true);
    }
}

/// Error handling utilities for consistent error reporting
pub mod error_handling {
    //! Utilities for consistent error handling across FFI interfaces.
    //!
    //! This module provides functions for error conversion, pointer validation,
    //! and standardized error reporting between JNI and Panama implementations.
    
    use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
    use std::os::raw::c_void;
    use wasmtime::Error as WasmtimeEngineError;
    
    /// Standardized error information structure for FFI operations
    #[derive(Debug, Clone)]
    pub struct ErrorInfo {
        /// Numeric error code for FFI interfaces
        pub code: i32,
        /// Human-readable error message
        pub message: String,
        /// Optional debug information for development
        pub debug_info: Option<String>,
    }
    
    /// Validation error types for parameter checking
    #[derive(Debug, Clone)]
    pub enum ValidationError {
        /// Null pointer parameter
        NullPointer(String),
        /// Parameter value out of valid range
        OutOfRange { parameter: String, value: i32, min: i32, max: i32 },
        /// Invalid string parameter
        InvalidString { parameter: String, reason: String },
        /// Invalid pointer parameter
        InvalidPointer { parameter: String, reason: String },
        /// Invalid handle value
        InvalidHandle { parameter: String, value: i64 },
    }
    
    impl ValidationError {
        /// Convert validation error to WasmtimeError
        pub fn to_wasmtime_error(self) -> WasmtimeError {
            match self {
                ValidationError::NullPointer(param) => WasmtimeError::InvalidParameter {
                    message: format!("{} pointer cannot be null", param),
                },
                ValidationError::OutOfRange { parameter, value, min, max } => WasmtimeError::InvalidParameter {
                    message: format!("{} value {} is out of range [{}, {}]", parameter, value, min, max),
                },
                ValidationError::InvalidString { parameter, reason } => WasmtimeError::InvalidParameter {
                    message: format!("{} string is invalid: {}", parameter, reason),
                },
                ValidationError::InvalidPointer { parameter, reason } => WasmtimeError::InvalidParameter {
                    message: format!("{} pointer is invalid: {}", parameter, reason),
                },
                ValidationError::InvalidHandle { parameter, value } => WasmtimeError::InvalidParameter {
                    message: format!("{} handle 0x{:x} is invalid", parameter, value),
                },
            }
        }
    }
    
    /// Convert Wasmtime engine errors to consistent error information
    pub fn convert_wasmtime_error(error: WasmtimeEngineError) -> ErrorInfo {
        let error_str = error.to_string();
        
        // Classify the error type based on content
        let (code, category) = if error_str.contains("compilation") || error_str.contains("compile") {
            (ErrorCode::CompilationError as i32, "Compilation")
        } else if error_str.contains("validation") || error_str.contains("validate") {
            (ErrorCode::ValidationError as i32, "Validation")
        } else if error_str.contains("runtime") || error_str.contains("trap") {
            (ErrorCode::RuntimeError as i32, "Runtime")
        } else if error_str.contains("memory") {
            (ErrorCode::MemoryError as i32, "Memory")
        } else if error_str.contains("function") {
            (ErrorCode::FunctionError as i32, "Function")
        } else if error_str.contains("import") || error_str.contains("export") {
            (ErrorCode::ImportExportError as i32, "ImportExport")
        } else if error_str.contains("type") {
            (ErrorCode::TypeError as i32, "Type")
        } else {
            (ErrorCode::RuntimeError as i32, "Runtime")
        };
        
        ErrorInfo {
            code,
            message: format!("{} error: {}", category, error_str),
            debug_info: Some(format!("Original Wasmtime error: {}", error_str)),
        }
    }
    
    /// Convert WasmtimeError to consistent ErrorInfo
    pub fn convert_internal_error(error: &WasmtimeError) -> ErrorInfo {
        ErrorInfo {
            code: error.to_error_code() as i32,
            message: error.to_string(),
            debug_info: Some(format!("{:?}", error)),
        }
    }
    
    /// Validate and safely dereference const pointers
    pub fn validate_pointer<T>(ptr: *const T, name: &str) -> Result<&'static T, ValidationError> {
        if ptr.is_null() {
            return Err(ValidationError::NullPointer(name.to_string()));
        }
        
        // Additional safety check - ensure pointer is not obviously invalid
        if (ptr as usize) < 0x1000 {
            return Err(ValidationError::InvalidPointer {
                parameter: name.to_string(),
                reason: "Pointer value too low (likely invalid)".to_string(),
            });
        }
        
        // TODO: Additional platform-specific validation could be added here
        unsafe { Ok(&*ptr) }
    }
    
    /// Validate and safely dereference mutable pointers
    pub fn validate_mut_pointer<T>(ptr: *mut T, name: &str) -> Result<&'static mut T, ValidationError> {
        if ptr.is_null() {
            return Err(ValidationError::NullPointer(name.to_string()));
        }
        
        // Additional safety check - ensure pointer is not obviously invalid
        if (ptr as usize) < 0x1000 {
            return Err(ValidationError::InvalidPointer {
                parameter: name.to_string(),
                reason: "Pointer value too low (likely invalid)".to_string(),
            });
        }
        
        // TODO: Additional platform-specific validation could be added here
        unsafe { Ok(&mut *ptr) }
    }
    
    /// Validate numeric parameters are within acceptable ranges
    pub fn validate_range(value: i32, min: i32, max: i32, name: &str) -> Result<i32, ValidationError> {
        if value < min || value > max {
            return Err(ValidationError::OutOfRange {
                parameter: name.to_string(),
                value,
                min,
                max,
            });
        }
        Ok(value)
    }
    
    /// Validate string parameters for safety and encoding
    pub fn validate_string_parameter(ptr: *const i8, name: &str) -> Result<String, ValidationError> {
        if ptr.is_null() {
            return Err(ValidationError::NullPointer(name.to_string()));
        }
        
        unsafe {
            let c_str = std::ffi::CStr::from_ptr(ptr);
            
            // Check for reasonable length limits (prevent potential DoS)
            let bytes = c_str.to_bytes();
            if bytes.len() > 65536 {
                return Err(ValidationError::InvalidString {
                    parameter: name.to_string(),
                    reason: "String too long (>64KB)".to_string(),
                });
            }
            
            // Validate UTF-8 encoding
            match c_str.to_str() {
                Ok(valid_str) => Ok(valid_str.to_string()),
                Err(e) => Err(ValidationError::InvalidString {
                    parameter: name.to_string(),
                    reason: format!("Invalid UTF-8 encoding: {}", e),
                }),
            }
        }
    }
    
    /// Validate handle values (typically pointers cast to integers)
    pub fn validate_handle(handle: i64, name: &str) -> Result<i64, ValidationError> {
        if handle == 0 {
            return Err(ValidationError::InvalidHandle {
                parameter: name.to_string(),
                value: handle,
            });
        }
        
        // Additional validation for obvious invalid handles
        if handle > 0 && (handle as usize) < 0x1000 {
            return Err(ValidationError::InvalidHandle {
                parameter: name.to_string(),
                value: handle,
            });
        }
        
        Ok(handle)
    }
    
    /// Validate byte array parameters with bounds checking
    pub fn validate_byte_array(ptr: *const u8, len: usize, name: &str) -> Result<&'static [u8], ValidationError> {
        if ptr.is_null() {
            return Err(ValidationError::NullPointer(name.to_string()));
        }
        
        if len == 0 {
            return Err(ValidationError::InvalidString {
                parameter: name.to_string(),
                reason: "Array length cannot be zero".to_string(),
            });
        }
        
        // Prevent potential overflow issues
        if len > (isize::MAX as usize) {
            return Err(ValidationError::InvalidString {
                parameter: name.to_string(),
                reason: "Array length too large".to_string(),
            });
        }
        
        unsafe { Ok(std::slice::from_raw_parts(ptr, len)) }
    }
    
    /// Validate void pointer and cast to specific type
    pub fn validate_void_pointer_as<T>(ptr: *mut c_void, name: &str) -> Result<&'static mut T, ValidationError> {
        validate_mut_pointer(ptr as *mut T, name)
    }
    
    /// Validate const void pointer and cast to specific type
    pub fn validate_const_void_pointer_as<T>(ptr: *const c_void, name: &str) -> Result<&'static T, ValidationError> {
        validate_pointer(ptr as *const T, name)
    }
    
    /// Format consistent error message for FFI interfaces
    pub fn format_error_message(error_info: &ErrorInfo, context: &str) -> String {
        if let Some(ref debug_info) = error_info.debug_info {
            format!("{}: {} (Debug: {})", context, error_info.message, debug_info)
        } else {
            format!("{}: {}", context, error_info.message)
        }
    }
    
    /// Create standardized error info from validation error
    pub fn validation_error_to_info(error: ValidationError) -> ErrorInfo {
        let wasmtime_error = error.to_wasmtime_error();
        ErrorInfo {
            code: wasmtime_error.to_error_code() as i32,
            message: wasmtime_error.to_string(),
            debug_info: Some(format!("Validation error: {:?}", wasmtime_error)),
        }
    }
}

/// Memory management utilities for safe FFI operations
pub mod memory_utils {
    //! Utilities for safe memory management in FFI operations.
    //!
    //! This module provides functions for safe pointer dereferencing, memory lifecycle
    //! management, and bounds checking to ensure memory safety across FFI boundaries.
    
    /// Placeholder function for safe pointer dereferencing
    /// 
    /// This function will be implemented to safely dereference pointers
    /// received from FFI calls with proper validation.
    pub fn safe_dereference_pointer() {
        // Placeholder for safe pointer dereferencing
        // Will be implemented in subsequent phases
    }
    
    /// Placeholder function for memory lifecycle management
    /// 
    /// This function will be implemented to manage memory lifecycle
    /// across FFI boundaries safely.
    pub fn manage_memory_lifecycle() {
        // Placeholder for memory lifecycle management
        // Will be implemented in subsequent phases
    }
    
    /// Placeholder function for bounds checking
    /// 
    /// This function will be implemented to perform bounds checking
    /// on memory operations in FFI contexts.
    pub fn check_bounds() {
        // Placeholder for bounds checking
        // Will be implemented in subsequent phases
    }
}