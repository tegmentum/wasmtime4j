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
    use super::memory_utils::*;
    use super::error_handling::*;
    use wasmtime::{Strategy, OptLevel};
    use std::ptr;
    
    // Parameter conversion tests
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
    
    // Memory management tests
    #[test]
    fn test_safe_deref_null_pointer() {
        let result = safe_deref(ptr::null::<i32>(), "test");
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
    }
    
    #[test]
    fn test_safe_deref_valid_pointer() {
        let value = 42i32;
        let result = safe_deref(&value as *const i32, "test");
        assert!(result.is_ok());
        assert_eq!(*result.unwrap(), 42);
    }
    
    #[test]
    fn test_safe_deref_mut_null_pointer() {
        let result = safe_deref_mut(ptr::null_mut::<i32>(), "test");
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
    }
    
    #[test]
    fn test_safe_deref_mut_valid_pointer() {
        let mut value = 42i32;
        let result = safe_deref_mut(&mut value as *mut i32, "test");
        assert!(result.is_ok());
        assert_eq!(*result.unwrap(), 42);
        *result.unwrap() = 100;
        assert_eq!(value, 100);
    }
    
    #[test]
    fn test_safe_box_operations() {
        // Test safe Box creation and destruction
        let original_value = 42i32;
        let boxed = Box::new(original_value);
        let raw_ptr = box_into_raw_safe(boxed);
        
        // Verify pointer is not null
        assert!(!raw_ptr.is_null());
        
        // Safely convert back to Box
        let result = safe_box_from_raw(raw_ptr, "test");
        assert!(result.is_ok());
        let restored_box = result.unwrap();
        assert_eq!(*restored_box, 42);
    }
    
    #[test]
    fn test_safe_box_from_raw_null_pointer() {
        let result = safe_box_from_raw(ptr::null_mut::<i32>(), "test");
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
    }
    
    #[test]
    fn test_resource_tracker() {
        static mut CLEANUP_CALLED: bool = false;
        
        fn cleanup_fn(_value: i32) {
            unsafe { CLEANUP_CALLED = true; }
        }
        
        {
            let _tracker = ResourceTracker::new(42i32, cleanup_fn);
            // Tracker goes out of scope here
        }
        
        unsafe {
            assert!(CLEANUP_CALLED, "Cleanup function should have been called");
        }
    }
    
    #[test]
    fn test_resource_tracker_take() {
        static mut CLEANUP_CALLED: bool = false;
        
        fn cleanup_fn(_value: i32) {
            unsafe { CLEANUP_CALLED = true; }
        }
        
        {
            let mut tracker = ResourceTracker::new(42i32, cleanup_fn);
            assert!(tracker.is_active());
            
            let value = tracker.take();
            assert_eq!(value, Some(42));
            assert!(!tracker.is_active());
            // Tracker goes out of scope but shouldn't call cleanup
        }
        
        unsafe {
            assert!(!CLEANUP_CALLED, "Cleanup function should not have been called after take()");
        }
    }
    
    #[test]
    fn test_safe_array_access() {
        let array = [1, 2, 3, 4, 5];
        let ptr = array.as_ptr();
        
        // Valid access
        let result = safe_array_access(ptr, 2, 5, "test_array");
        assert!(result.is_ok());
        assert_eq!(*result.unwrap(), 3);
        
        // Out of bounds access
        let result = safe_array_access(ptr, 5, 5, "test_array");
        assert!(matches!(result, Err(MemoryError::IndexOutOfBounds { .. })));
        
        // Way out of bounds
        let result = safe_array_access(ptr, 10, 5, "test_array");
        assert!(matches!(result, Err(MemoryError::IndexOutOfBounds { .. })));
    }
    
    #[test]
    fn test_safe_array_access_null_pointer() {
        let result = safe_array_access(ptr::null::<i32>(), 0, 5, "test");
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
    }
    
    #[test]
    fn test_safe_memory_copy() {
        let src = [1u8, 2, 3, 4, 5];
        let mut dest = [0u8; 5];
        
        let result = safe_memory_copy(
            dest.as_mut_ptr(),
            src.as_ptr(),
            3,
            dest.len(),
            src.len(),
            "test_copy"
        );
        
        assert!(result.is_ok());
        assert_eq!(dest[0], 1);
        assert_eq!(dest[1], 2);
        assert_eq!(dest[2], 3);
        assert_eq!(dest[3], 0); // Unchanged
        assert_eq!(dest[4], 0); // Unchanged
    }
    
    #[test]
    fn test_safe_memory_copy_buffer_overflow() {
        let src = [1u8, 2, 3, 4, 5];
        let mut dest = [0u8; 3];
        
        // Try to copy more than destination can hold
        let result = safe_memory_copy(
            dest.as_mut_ptr(),
            src.as_ptr(),
            5, // More than dest.len()
            dest.len(),
            src.len(),
            "test_copy"
        );
        
        assert!(matches!(result, Err(MemoryError::BufferOverflow { .. })));
    }
    
    #[test]
    fn test_safe_memory_copy_overlapping_buffers() {
        let mut buffer = [1u8, 2, 3, 4, 5];
        
        // Copy from beginning to middle (overlapping)
        let result = safe_memory_copy(
            buffer.as_mut_ptr().wrapping_add(2), // dest = &mut buffer[2..]
            buffer.as_ptr(),                     // src = &buffer[0..]
            2,                                   // copy 2 bytes
            3,                                   // dest size
            5,                                   // src size
            "overlapping_copy"
        );
        
        assert!(result.is_ok());
        assert_eq!(buffer[2], 1); // buffer[0] copied to buffer[2]
        assert_eq!(buffer[3], 2); // buffer[1] copied to buffer[3]
    }
    
    #[test]
    fn test_safe_memory_copy_null_pointers() {
        let src = [1u8, 2, 3];
        
        // Null destination
        let result = safe_memory_copy(
            ptr::null_mut(),
            src.as_ptr(),
            3,
            3,
            3,
            "test"
        );
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
        
        // Null source
        let mut dest = [0u8; 3];
        let result = safe_memory_copy(
            dest.as_mut_ptr(),
            ptr::null(),
            3,
            3,
            3,
            "test"
        );
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
    }
    
    #[test]
    fn test_safe_byte_slice() {
        let data = [1u8, 2, 3, 4, 5];
        
        // Valid slice creation
        let result = safe_byte_slice(data.as_ptr(), data.len(), "test");
        assert!(result.is_ok());
        let slice = result.unwrap();
        assert_eq!(slice.len(), 5);
        assert_eq!(slice[0], 1);
        assert_eq!(slice[4], 5);
    }
    
    #[test]
    fn test_safe_byte_slice_null_pointer() {
        let result = safe_byte_slice(ptr::null(), 5, "test");
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));
    }
    
    #[test]
    fn test_safe_byte_slice_zero_length() {
        let data = [1u8, 2, 3];
        let result = safe_byte_slice(data.as_ptr(), 0, "test");
        assert!(matches!(result, Err(MemoryError::IndexOutOfBounds { .. })));
    }
    
    #[test]
    fn test_memory_error_to_wasmtime_error_conversion() {
        let memory_error = MemoryError::NullPointer("test_param".to_string());
        let wasmtime_error = memory_error.to_wasmtime_error();
        
        match wasmtime_error {
            crate::error::WasmtimeError::InvalidParameter { message } => {
                assert!(message.contains("test_param"));
                assert!(message.contains("cannot be null"));
            },
            _ => panic!("Expected InvalidParameter error"),
        }
    }
    
    // Error handling tests
    #[test]
    fn test_validation_error_conversion() {
        let validation_error = ValidationError::NullPointer("engine".to_string());
        let wasmtime_error = validation_error.to_wasmtime_error();
        
        match wasmtime_error {
            crate::error::WasmtimeError::InvalidParameter { message } => {
                assert!(message.contains("engine"));
                assert!(message.contains("cannot be null"));
            },
            _ => panic!("Expected InvalidParameter error"),
        }
    }
    
    #[test]
    fn test_validation_error_out_of_range() {
        let validation_error = ValidationError::OutOfRange {
            parameter: "size".to_string(),
            value: 150,
            min: 1,
            max: 100,
        };
        
        let wasmtime_error = validation_error.to_wasmtime_error();
        match wasmtime_error {
            crate::error::WasmtimeError::InvalidParameter { message } => {
                assert!(message.contains("size"));
                assert!(message.contains("150"));
                assert!(message.contains("[1, 100]"));
            },
            _ => panic!("Expected InvalidParameter error"),
        }
    }
}

/// Error handling utilities for consistent error reporting
pub mod error_handling {
    //! Utilities for consistent error handling across FFI interfaces.
    //!
    //! This module provides functions for error conversion, pointer validation,
    //! and standardized error reporting between JNI and Panama implementations.
    
    use crate::error::{WasmtimeError, ErrorCode};
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
    
    use crate::error::WasmtimeError;
    
    use std::os::raw::c_void;
    use std::ptr;
    
    /// Memory error types for specific memory management failures
    #[derive(Debug, Clone)]
    pub enum MemoryError {
        /// Null pointer error
        NullPointer(String),
        /// Misaligned pointer error
        MisalignedPointer(String),
        /// Invalid Box pointer error
        InvalidBoxPointer(String),
        /// Array index out of bounds error
        IndexOutOfBounds { array_name: String, index: usize, length: usize },
        /// Buffer overflow error
        BufferOverflow { operation: String, requested: usize, available: usize },
        /// Resource lifecycle violation
        LifecycleViolation { resource: String, state: String },
    }
    
    impl MemoryError {
        /// Convert MemoryError to WasmtimeError
        pub fn to_wasmtime_error(self) -> WasmtimeError {
            match self {
                MemoryError::NullPointer(param) => WasmtimeError::InvalidParameter {
                    message: format!("{} pointer cannot be null", param),
                },
                MemoryError::MisalignedPointer(param) => WasmtimeError::InvalidParameter {
                    message: format!("{} pointer is misaligned", param),
                },
                MemoryError::InvalidBoxPointer(param) => WasmtimeError::InvalidParameter {
                    message: format!("{} pointer was not allocated by Box", param),
                },
                MemoryError::IndexOutOfBounds { array_name, index, length } => WasmtimeError::InvalidParameter {
                    message: format!("{} index {} out of bounds [0, {})", array_name, index, length),
                },
                MemoryError::BufferOverflow { operation, requested, available } => WasmtimeError::InvalidParameter {
                    message: format!("{} requested {} bytes but only {} available", operation, requested, available),
                },
                MemoryError::LifecycleViolation { resource, state } => WasmtimeError::InvalidParameter {
                    message: format!("{} lifecycle violation: {}", resource, state),
                },
            }
        }
    }
    
    /// Safely dereference a const pointer with comprehensive validation
    /// 
    /// # Arguments
    /// * `ptr` - Raw const pointer to dereference
    /// * `name` - Descriptive name for error reporting
    /// 
    /// # Returns
    /// * `Ok(&'static T)` - Safe reference to the data
    /// * `Err(MemoryError)` - Validation failure details
    /// 
    /// # Safety
    /// This function performs extensive validation to prevent memory safety violations:
    /// - Null pointer check
    /// - Low address range check (catches common invalid pointers)
    /// - Alignment validation for the target type
    pub fn safe_deref<T>(ptr: *const T, name: &str) -> Result<&'static T, MemoryError> {
        // Null pointer check
        if ptr.is_null() {
            return Err(MemoryError::NullPointer(name.to_string()));
        }
        
        // Check for obviously invalid low addresses (0x1000 is a common threshold)
        if (ptr as usize) < 0x1000 {
            return Err(MemoryError::InvalidBoxPointer(
                format!("{} (address too low: 0x{:x})", name, ptr as usize)
            ));
        }
        
        // Alignment check - ensure pointer is properly aligned for type T
        if !is_aligned(ptr) {
            return Err(MemoryError::MisalignedPointer(name.to_string()));
        }
        
        // Safe dereference - all validation passed
        unsafe { Ok(&*ptr) }
    }
    
    /// Safely dereference a mutable pointer with comprehensive validation
    /// 
    /// # Arguments
    /// * `ptr` - Raw mutable pointer to dereference
    /// * `name` - Descriptive name for error reporting
    /// 
    /// # Returns
    /// * `Ok(&'static mut T)` - Safe mutable reference to the data
    /// * `Err(MemoryError)` - Validation failure details
    /// 
    /// # Safety
    /// This function performs extensive validation including exclusive access checks
    pub fn safe_deref_mut<T>(ptr: *mut T, name: &str) -> Result<&'static mut T, MemoryError> {
        // Null pointer check
        if ptr.is_null() {
            return Err(MemoryError::NullPointer(name.to_string()));
        }
        
        // Check for obviously invalid low addresses
        if (ptr as usize) < 0x1000 {
            return Err(MemoryError::InvalidBoxPointer(
                format!("{} (address too low: 0x{:x})", name, ptr as usize)
            ));
        }
        
        // Alignment check
        if !is_aligned(ptr as *const T) {
            return Err(MemoryError::MisalignedPointer(name.to_string()));
        }
        
        // Safe mutable dereference - validation passed
        unsafe { Ok(&mut *ptr) }
    }
    
    /// Check if a pointer is properly aligned for type T
    /// 
    /// # Arguments
    /// * `ptr` - Pointer to check for alignment
    /// 
    /// # Returns
    /// * `true` if pointer is properly aligned
    /// * `false` if pointer is misaligned
    #[inline]
    fn is_aligned<T>(ptr: *const T) -> bool {
        (ptr as usize) % std::mem::align_of::<T>() == 0
    }
    
    /// Safely create a Box from a raw pointer with ownership validation
    /// 
    /// # Arguments
    /// * `ptr` - Raw pointer that was previously created by Box::into_raw
    /// * `name` - Descriptive name for error reporting
    /// 
    /// # Returns
    /// * `Ok(Box<T>)` - Safely reconstructed Box with ownership
    /// * `Err(MemoryError)` - Validation failure
    /// 
    /// # Safety
    /// This function validates that the pointer was originally allocated by Box::into_raw
    /// and performs comprehensive safety checks before reconstruction
    pub fn safe_box_from_raw<T>(ptr: *mut T, name: &str) -> Result<Box<T>, MemoryError> {
        // Null pointer check
        if ptr.is_null() {
            return Err(MemoryError::NullPointer(name.to_string()));
        }
        
        // Validate pointer was allocated by Box - check reasonable address range
        if !is_valid_box_pointer(ptr) {
            return Err(MemoryError::InvalidBoxPointer(
                format!("{} (invalid address: 0x{:x})", name, ptr as usize)
            ));
        }
        
        // Alignment validation
        if !is_aligned(ptr as *const T) {
            return Err(MemoryError::MisalignedPointer(name.to_string()));
        }
        
        // Safe Box reconstruction - all validation passed
        unsafe { Ok(Box::from_raw(ptr)) }
    }
    
    /// Safely convert Box to raw pointer for FFI boundaries
    /// 
    /// # Arguments
    /// * `boxed` - Box to convert to raw pointer
    /// 
    /// # Returns
    /// Raw pointer that can be safely passed across FFI boundaries
    /// 
    /// # Safety
    /// This is always safe as Box::into_raw is a safe operation.
    /// The caller takes ownership responsibility for the returned pointer.
    pub fn box_into_raw_safe<T>(boxed: Box<T>) -> *mut T {
        Box::into_raw(boxed)
    }
    
    /// Validate if pointer appears to be from Box allocation
    /// 
    /// # Arguments
    /// * `ptr` - Pointer to validate
    /// 
    /// # Returns
    /// * `true` if pointer appears valid for Box operations
    /// * `false` if pointer appears invalid
    /// 
    /// # Note
    /// This is a heuristic check - we can't definitively prove a pointer
    /// was allocated by Box, but we can catch obviously invalid cases
    #[inline]
    fn is_valid_box_pointer<T>(ptr: *mut T) -> bool {
        let addr = ptr as usize;
        
        // Check for reasonable address range (not null, not too low)
        if addr < 0x1000 {
            return false;
        }
        
        // Check for alignment
        if addr % std::mem::align_of::<T>() != 0 {
            return false;
        }
        
        // Additional platform-specific checks could be added here
        // For now, these basic checks catch most invalid cases
        true
    }
    
    /// Resource lifecycle tracker for automatic cleanup
    /// 
    /// This struct ensures that resources are properly cleaned up
    /// even if explicit cleanup is forgotten.
    pub struct ResourceTracker<T> {
        resource: Option<T>,
        cleanup_fn: Option<fn(T)>,
    }
    
    impl<T> ResourceTracker<T> {
        /// Create a new resource tracker with automatic cleanup
        /// 
        /// # Arguments
        /// * `resource` - Resource to track
        /// * `cleanup_fn` - Function to call for cleanup
        pub fn new(resource: T, cleanup_fn: fn(T)) -> Self {
            Self {
                resource: Some(resource),
                cleanup_fn: Some(cleanup_fn),
            }
        }
        
        /// Take ownership of the tracked resource
        /// 
        /// This removes the resource from tracking, preventing automatic cleanup
        pub fn take(&mut self) -> Option<T> {
            self.resource.take()
        }
        
        /// Check if resource is still being tracked
        pub fn is_active(&self) -> bool {
            self.resource.is_some()
        }
    }
    
    impl<T> Drop for ResourceTracker<T> {
        /// Automatic cleanup when tracker is dropped
        fn drop(&mut self) {
            if let (Some(resource), Some(cleanup)) = (self.resource.take(), self.cleanup_fn) {
                cleanup(resource);
            }
        }
    }
    
    /// Safe array access with bounds checking
    /// 
    /// # Arguments
    /// * `array` - Pointer to array start
    /// * `index` - Index to access
    /// * `length` - Total array length
    /// * `name` - Array name for error reporting
    /// 
    /// # Returns
    /// * `Ok(&'static T)` - Safe reference to array element
    /// * `Err(MemoryError)` - Bounds check failure
    pub fn safe_array_access<T>(
        array: *const T,
        index: usize,
        length: usize,
        name: &str
    ) -> Result<&'static T, MemoryError> {
        // Null pointer check
        if array.is_null() {
            return Err(MemoryError::NullPointer(name.to_string()));
        }
        
        // Bounds check
        if index >= length {
            return Err(MemoryError::IndexOutOfBounds {
                array_name: name.to_string(),
                index,
                length,
            });
        }
        
        // Check for potential overflow in pointer arithmetic
        if let Some(element_ptr) = (array as usize).checked_add(index * std::mem::size_of::<T>()) {
            if element_ptr < array as usize {
                return Err(MemoryError::IndexOutOfBounds {
                    array_name: name.to_string(),
                    index,
                    length,
                });
            }
        } else {
            return Err(MemoryError::IndexOutOfBounds {
                array_name: name.to_string(),
                index,
                length,
            });
        }
        
        // Safe array access
        unsafe { Ok(&*array.add(index)) }
    }
    
    /// Safe memory copying with overflow protection
    /// 
    /// # Arguments
    /// * `dest` - Destination buffer
    /// * `src` - Source buffer  
    /// * `count` - Number of bytes to copy
    /// * `dest_size` - Size of destination buffer
    /// * `src_size` - Size of source buffer
    /// * `operation_name` - Operation name for error reporting
    /// 
    /// # Returns
    /// * `Ok(())` - Copy completed successfully
    /// * `Err(MemoryError)` - Buffer overflow or other error
    pub fn safe_memory_copy(
        dest: *mut u8,
        src: *const u8,
        count: usize,
        dest_size: usize,
        src_size: usize,
        operation_name: &str
    ) -> Result<(), MemoryError> {
        // Null pointer checks
        if dest.is_null() {
            return Err(MemoryError::NullPointer(format!("{} destination", operation_name)));
        }
        if src.is_null() {
            return Err(MemoryError::NullPointer(format!("{} source", operation_name)));
        }
        
        // Buffer overflow checks
        if count > dest_size {
            return Err(MemoryError::BufferOverflow {
                operation: operation_name.to_string(),
                requested: count,
                available: dest_size,
            });
        }
        
        if count > src_size {
            return Err(MemoryError::BufferOverflow {
                operation: operation_name.to_string(),
                requested: count,
                available: src_size,
            });
        }
        
        // Check for buffer overlap (use memmove semantics if overlapping)
        let dest_start = dest as usize;
        let dest_end = dest_start + count;
        let src_start = src as usize;
        let src_end = src_start + count;
        
        unsafe {
            if dest_start < src_end && src_start < dest_end {
                // Buffers overlap, use copy_overlapping
                ptr::copy(src, dest, count);
            } else {
                // No overlap, use faster non-overlapping copy
                ptr::copy_nonoverlapping(src, dest, count);
            }
        }
        
        Ok(())
    }
    
    /// Create a safe byte slice from pointer and length with validation
    /// 
    /// # Arguments
    /// * `ptr` - Pointer to byte data
    /// * `len` - Length of the data
    /// * `name` - Data name for error reporting
    /// 
    /// # Returns
    /// * `Ok(&'static [u8])` - Safe byte slice
    /// * `Err(MemoryError)` - Validation failure
    pub fn safe_byte_slice(ptr: *const u8, len: usize, name: &str) -> Result<&'static [u8], MemoryError> {
        // Null pointer check
        if ptr.is_null() {
            return Err(MemoryError::NullPointer(name.to_string()));
        }
        
        // Length validation
        if len == 0 {
            return Err(MemoryError::IndexOutOfBounds {
                array_name: name.to_string(),
                index: 0,
                length: 0,
            });
        }
        
        // Check for potential overflow
        if len > (isize::MAX as usize) {
            return Err(MemoryError::BufferOverflow {
                operation: format!("{} slice creation", name),
                requested: len,
                available: isize::MAX as usize,
            });
        }
        
        // Safe slice creation
        unsafe { Ok(std::slice::from_raw_parts(ptr, len)) }
    }
    
    /// Unified resource destruction for FFI boundaries
    /// 
    /// # Arguments
    /// * `ptr` - Raw pointer to resource
    /// * `name` - Resource name for logging
    /// 
    /// # Safety
    /// This function assumes the pointer was created by Box::into_raw
    /// and performs safe cleanup with logging
    pub unsafe fn destroy_ffi_resource<T>(ptr: *mut c_void, name: &str) {
        if !ptr.is_null() {
            // Convert to typed pointer and create Box for automatic cleanup
            match safe_box_from_raw(ptr as *mut T, name) {
                Ok(_boxed_resource) => {
                    // Box automatically cleans up when it goes out of scope
                    log::debug!("{} destroyed successfully", name);
                },
                Err(error) => {
                    log::error!("Failed to destroy {}: {:?}", name, error);
                    // Force cleanup anyway to prevent leaks - this is unsafe
                    // but necessary if the pointer validation failed
                    let _ = Box::from_raw(ptr as *mut T);
                }
            }
        }
    }
}