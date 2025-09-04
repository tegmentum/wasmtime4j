//! # Shared FFI Architecture
//!
//! This module provides a unified trait-based architecture for converting between
//! Rust types and FFI-compatible types, supporting both JNI and Panama interfaces.
//!
//! ## Design Principles
//!
//! - **Zero-cost abstractions**: All conversions are compile-time resolved
//! - **Type safety**: Invalid conversions caught at compile time
//! - **Defensive programming**: All operations include validation and error handling
//! - **Extensibility**: Easy to add new types using the same trait patterns
//!
//! ## Core Traits
//!
//! - `ParameterConverter<T>`: Bidirectional conversion between FFI and native types
//! - `ReturnValueConverter<T>`: Consistent return value handling with error codes
//!
//! ## Usage Example
//!
//! ```rust
//! use crate::shared_ffi::{ParameterConverter, FfiStrategy};
//! use crate::engine::Strategy;
//!
//! // Convert FFI parameter to native type with validation
//! let strategy = FfiStrategy::from_ffi(1)?; // Cranelift
//! let native_strategy = strategy.to_native();
//! ```

use crate::engine::{Strategy, OptLevel, WasmFeature};
use crate::error::{WasmtimeResult, WasmtimeError};
use std::os::raw::c_void;

/// Standard FFI return codes
pub const FFI_SUCCESS: i32 = 0;
pub const FFI_ERROR: i32 = -1;

/// Trait for converting between FFI-compatible and native Rust types
///
/// This trait provides bidirectional conversion with compile-time type safety
/// and runtime validation. All implementations must handle error cases gracefully.
pub trait ParameterConverter<T> {
    /// Convert from FFI representation to native type with validation
    fn from_ffi(value: i32) -> WasmtimeResult<T>;
    
    /// Convert from native type to FFI representation
    fn to_ffi(enum_value: T) -> i32;
    
    /// Validate FFI value without conversion (for early parameter checking)
    fn validate(value: i32) -> WasmtimeResult<()>;
}

/// Trait for handling return value conversions across FFI boundaries
///
/// Provides consistent patterns for different return value types while
/// maintaining proper error propagation and resource management.
pub trait ReturnValueConverter<T> {
    /// Convert result to (error_code, value) tuple for operations returning values
    fn to_ffi_result(result: WasmtimeResult<T>) -> (i32, T) where T: Default;
    
    /// Convert result to pointer for operations returning boxed resources
    fn to_ffi_ptr(result: WasmtimeResult<Box<T>>) -> *mut c_void;
    
    /// Convert result to simple error code for success/failure operations
    fn to_ffi_code(result: WasmtimeResult<()>) -> i32;
}

/// FFI-compatible Strategy enum representation
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FfiStrategy {
    Auto = 0,
    Cranelift = 1,
}

impl ParameterConverter<Strategy> for FfiStrategy {
    fn from_ffi(value: i32) -> WasmtimeResult<Strategy> {
        match value {
            0 => Ok(Strategy::Auto),
            1 => Ok(Strategy::Cranelift),
            _ => Err(WasmtimeError::InvalidParameter(
                format!("Invalid strategy value: {}. Expected 0 (Auto) or 1 (Cranelift)", value)
            )),
        }
    }
    
    fn to_ffi(enum_value: Strategy) -> i32 {
        match enum_value {
            Strategy::Auto => 0,
            Strategy::Cranelift => 1,
        }
    }
    
    fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0 | 1 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter(
                format!("Invalid strategy value: {}. Expected 0 (Auto) or 1 (Cranelift)", value)
            )),
        }
    }
}

impl FfiStrategy {
    /// Convert FFI strategy to native Strategy enum
    pub fn to_native(self) -> Strategy {
        match self {
            FfiStrategy::Auto => Strategy::Auto,
            FfiStrategy::Cranelift => Strategy::Cranelift,
        }
    }
    
    /// Create FFI strategy from native Strategy enum
    pub fn from_native(strategy: Strategy) -> Self {
        match strategy {
            Strategy::Auto => FfiStrategy::Auto,
            Strategy::Cranelift => FfiStrategy::Cranelift,
        }
    }
}

/// FFI-compatible OptLevel enum representation
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FfiOptLevel {
    None = 0,
    Speed = 1,
    SpeedAndSize = 2,
}

impl ParameterConverter<OptLevel> for FfiOptLevel {
    fn from_ffi(value: i32) -> WasmtimeResult<OptLevel> {
        match value {
            0 => Ok(OptLevel::None),
            1 => Ok(OptLevel::Speed),
            2 => Ok(OptLevel::SpeedAndSize),
            _ => Err(WasmtimeError::InvalidParameter(
                format!("Invalid optimization level: {}. Expected 0 (None), 1 (Speed), or 2 (SpeedAndSize)", value)
            )),
        }
    }
    
    fn to_ffi(enum_value: OptLevel) -> i32 {
        match enum_value {
            OptLevel::None => 0,
            OptLevel::Speed => 1,
            OptLevel::SpeedAndSize => 2,
        }
    }
    
    fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0..=2 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter(
                format!("Invalid optimization level: {}. Expected 0 (None), 1 (Speed), or 2 (SpeedAndSize)", value)
            )),
        }
    }
}

impl FfiOptLevel {
    /// Convert FFI optimization level to native OptLevel enum
    pub fn to_native(self) -> OptLevel {
        match self {
            FfiOptLevel::None => OptLevel::None,
            FfiOptLevel::Speed => OptLevel::Speed,
            FfiOptLevel::SpeedAndSize => OptLevel::SpeedAndSize,
        }
    }
    
    /// Create FFI optimization level from native OptLevel enum
    pub fn from_native(opt_level: OptLevel) -> Self {
        match opt_level {
            OptLevel::None => FfiOptLevel::None,
            OptLevel::Speed => FfiOptLevel::Speed,
            OptLevel::SpeedAndSize => FfiOptLevel::SpeedAndSize,
        }
    }
}

/// FFI-compatible WasmFeature enum representation
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FfiWasmFeature {
    Threads = 0,
    ReferenceTypes = 1,
    Simd = 2,
    BulkMemory = 3,
    MultiValue = 4,
}

impl ParameterConverter<WasmFeature> for FfiWasmFeature {
    fn from_ffi(value: i32) -> WasmtimeResult<WasmFeature> {
        match value {
            0 => Ok(WasmFeature::Threads),
            1 => Ok(WasmFeature::ReferenceTypes),
            2 => Ok(WasmFeature::Simd),
            3 => Ok(WasmFeature::BulkMemory),
            4 => Ok(WasmFeature::MultiValue),
            _ => Err(WasmtimeError::InvalidParameter(
                format!("Invalid WASM feature: {}. Expected 0-4", value)
            )),
        }
    }
    
    fn to_ffi(enum_value: WasmFeature) -> i32 {
        match enum_value {
            WasmFeature::Threads => 0,
            WasmFeature::ReferenceTypes => 1,
            WasmFeature::Simd => 2,
            WasmFeature::BulkMemory => 3,
            WasmFeature::MultiValue => 4,
        }
    }
    
    fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0..=4 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter(
                format!("Invalid WASM feature: {}. Expected 0-4", value)
            )),
        }
    }
}

impl FfiWasmFeature {
    /// Convert FFI WASM feature to native WasmFeature enum
    pub fn to_native(self) -> WasmFeature {
        match self {
            FfiWasmFeature::Threads => WasmFeature::Threads,
            FfiWasmFeature::ReferenceTypes => WasmFeature::ReferenceTypes,
            FfiWasmFeature::Simd => WasmFeature::Simd,
            FfiWasmFeature::BulkMemory => WasmFeature::BulkMemory,
            FfiWasmFeature::MultiValue => WasmFeature::MultiValue,
        }
    }
    
    /// Create FFI WASM feature from native WasmFeature enum
    pub fn from_native(feature: WasmFeature) -> Self {
        match feature {
            WasmFeature::Threads => FfiWasmFeature::Threads,
            WasmFeature::ReferenceTypes => FfiWasmFeature::ReferenceTypes,
            WasmFeature::Simd => FfiWasmFeature::Simd,
            WasmFeature::BulkMemory => FfiWasmFeature::BulkMemory,
            WasmFeature::MultiValue => FfiWasmFeature::MultiValue,
        }
    }
}

/// Helper function for batch conversion of WASM features
pub fn convert_wasm_features(ffi_features: &[i32]) -> WasmtimeResult<Vec<WasmFeature>> {
    ffi_features
        .iter()
        .map(|&value| FfiWasmFeature::from_ffi(value))
        .collect()
}

/// Helper function for batch validation of WASM features without conversion
pub fn validate_wasm_features(ffi_features: &[i32]) -> WasmtimeResult<()> {
    for &value in ffi_features {
        FfiWasmFeature::validate(value)?;
    }
    Ok(())
}

/// Boolean return value converter
pub struct BooleanReturnConverter;

impl ReturnValueConverter<bool> for BooleanReturnConverter {
    fn to_ffi_result(result: WasmtimeResult<bool>) -> (i32, bool) {
        match result {
            Ok(value) => (FFI_SUCCESS, value),
            Err(_) => (FFI_ERROR, false),
        }
    }
    
    fn to_ffi_ptr(_result: WasmtimeResult<Box<bool>>) -> *mut c_void {
        // Booleans are not returned as pointers
        std::ptr::null_mut()
    }
    
    fn to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        match result {
            Ok(()) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        }
    }
}

/// Integer return value converter
pub struct IntegerReturnConverter;

impl ReturnValueConverter<i32> for IntegerReturnConverter {
    fn to_ffi_result(result: WasmtimeResult<i32>) -> (i32, i32) {
        match result {
            Ok(value) => (FFI_SUCCESS, value),
            Err(_) => (FFI_ERROR, -1),
        }
    }
    
    fn to_ffi_ptr(_result: WasmtimeResult<Box<i32>>) -> *mut c_void {
        // Integers are not typically returned as pointers in this API
        std::ptr::null_mut()
    }
    
    fn to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        match result {
            Ok(()) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        }
    }
}

/// Pointer return value converter for resource handles
pub struct PointerReturnConverter;

impl<T> ReturnValueConverter<T> for PointerReturnConverter {
    fn to_ffi_result(result: WasmtimeResult<T>) -> (i32, T) 
    where 
        T: Default 
    {
        match result {
            Ok(value) => (FFI_SUCCESS, value),
            Err(_) => (FFI_ERROR, T::default()),
        }
    }
    
    fn to_ffi_ptr(result: WasmtimeResult<Box<T>>) -> *mut c_void {
        match result {
            Ok(boxed_value) => Box::into_raw(boxed_value) as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    fn to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        match result {
            Ok(()) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        }
    }
}

/// Validation utilities module
pub mod validation {
    use crate::error::{WasmtimeResult, WasmtimeError};
    
    /// Validate that a pointer is not null
    pub fn validate_not_null<T>(ptr: *const T, name: &str) -> WasmtimeResult<()> {
        if ptr.is_null() {
            Err(WasmtimeError::InvalidParameter(format!("{} cannot be null", name)))
        } else {
            Ok(())
        }
    }
    
    /// Validate array bounds
    pub fn validate_array_bounds(array_len: usize, index: usize, name: &str) -> WasmtimeResult<()> {
        if index >= array_len {
            Err(WasmtimeError::InvalidParameter(
                format!("{} index {} out of bounds (length: {})", name, index, array_len)
            ))
        } else {
            Ok(())
        }
    }
    
    /// Validate that a slice has valid bounds
    pub fn validate_slice_bounds<T>(slice: &[T], start: usize, len: usize, name: &str) -> WasmtimeResult<()> {
        if start >= slice.len() || start.saturating_add(len) > slice.len() {
            Err(WasmtimeError::InvalidParameter(
                format!("{} slice bounds invalid: start={}, len={}, slice_len={}", name, start, len, slice.len())
            ))
        } else {
            Ok(())
        }
    }
}

/// Error code mapping utilities
pub mod error_mapping {
    use crate::error::WasmtimeError;
    
    /// Map WasmtimeError to standardized FFI error codes
    pub fn map_error_to_code(error: &WasmtimeError) -> i32 {
        // All errors map to FFI_ERROR (-1) for simplicity and consistency
        // Detailed error information is available through error message retrieval
        super::FFI_ERROR
    }
    
    /// Check if an error should be propagated to FFI boundary
    pub fn should_propagate_error(error: &WasmtimeError) -> bool {
        // All errors should be propagated as return codes, never as panics
        true
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    
    #[test]
    fn test_strategy_conversion() {
        // Test valid conversions
        assert_eq!(FfiStrategy::from_ffi(0).unwrap(), Strategy::Auto);
        assert_eq!(FfiStrategy::from_ffi(1).unwrap(), Strategy::Cranelift);
        
        // Test invalid conversion
        assert!(FfiStrategy::from_ffi(99).is_err());
        
        // Test bidirectional conversion
        assert_eq!(FfiStrategy::to_ffi(Strategy::Auto), 0);
        assert_eq!(FfiStrategy::to_ffi(Strategy::Cranelift), 1);
        
        // Test validation
        assert!(FfiStrategy::validate(0).is_ok());
        assert!(FfiStrategy::validate(1).is_ok());
        assert!(FfiStrategy::validate(99).is_err());
    }
    
    #[test]
    fn test_opt_level_conversion() {
        // Test valid conversions
        assert_eq!(FfiOptLevel::from_ffi(0).unwrap(), OptLevel::None);
        assert_eq!(FfiOptLevel::from_ffi(1).unwrap(), OptLevel::Speed);
        assert_eq!(FfiOptLevel::from_ffi(2).unwrap(), OptLevel::SpeedAndSize);
        
        // Test invalid conversion
        assert!(FfiOptLevel::from_ffi(99).is_err());
        
        // Test bidirectional conversion
        assert_eq!(FfiOptLevel::to_ffi(OptLevel::None), 0);
        assert_eq!(FfiOptLevel::to_ffi(OptLevel::Speed), 1);
        assert_eq!(FfiOptLevel::to_ffi(OptLevel::SpeedAndSize), 2);
    }
    
    #[test]
    fn test_wasm_feature_conversion() {
        // Test valid conversions
        assert_eq!(FfiWasmFeature::from_ffi(0).unwrap(), WasmFeature::Threads);
        assert_eq!(FfiWasmFeature::from_ffi(1).unwrap(), WasmFeature::ReferenceTypes);
        assert_eq!(FfiWasmFeature::from_ffi(4).unwrap(), WasmFeature::MultiValue);
        
        // Test invalid conversion
        assert!(FfiWasmFeature::from_ffi(99).is_err());
        
        // Test batch conversion
        let features = vec![0, 1, 2];
        let result = convert_wasm_features(&features);
        assert!(result.is_ok());
        assert_eq!(result.unwrap().len(), 3);
        
        // Test batch validation
        assert!(validate_wasm_features(&[0, 1, 2, 3, 4]).is_ok());
        assert!(validate_wasm_features(&[0, 99]).is_err());
    }
    
    #[test]
    fn test_return_value_converters() {
        // Test boolean converter
        let (code, value) = BooleanReturnConverter::to_ffi_result(Ok(true));
        assert_eq!(code, FFI_SUCCESS);
        assert_eq!(value, true);
        
        let (code, value) = BooleanReturnConverter::to_ffi_result(Err(WasmtimeError::InvalidParameter("test".to_string())));
        assert_eq!(code, FFI_ERROR);
        assert_eq!(value, false);
        
        // Test error code converter
        assert_eq!(BooleanReturnConverter::to_ffi_code(Ok(())), FFI_SUCCESS);
        assert_eq!(BooleanReturnConverter::to_ffi_code(Err(WasmtimeError::InvalidParameter("test".to_string()))), FFI_ERROR);
    }
    
    #[test]
    fn test_validation_utilities() {
        use validation::*;
        
        // Test null pointer validation
        let valid_ptr = &42 as *const i32;
        let null_ptr = std::ptr::null::<i32>();
        
        assert!(validate_not_null(valid_ptr, "test_ptr").is_ok());
        assert!(validate_not_null(null_ptr, "test_ptr").is_err());
        
        // Test array bounds validation
        assert!(validate_array_bounds(10, 5, "test_array").is_ok());
        assert!(validate_array_bounds(10, 15, "test_array").is_err());
        
        // Test slice bounds validation
        let slice = &[1, 2, 3, 4, 5];
        assert!(validate_slice_bounds(slice, 1, 3, "test_slice").is_ok());
        assert!(validate_slice_bounds(slice, 3, 5, "test_slice").is_err());
    }
}