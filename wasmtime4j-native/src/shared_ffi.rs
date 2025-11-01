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

use wasmtime::{Strategy, OptLevel};
use crate::engine::WasmFeature;
use crate::error::{WasmtimeResult, WasmtimeError};
use std::os::raw::c_void;

/// Standard FFI return codes
pub const FFI_SUCCESS: i32 = 0;
/// Standard FFI error return code
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
#[allow(missing_docs)]
pub enum FfiStrategy {
    Auto = 0,
    Cranelift = 1,
}

impl ParameterConverter<Strategy> for FfiStrategy {
    fn from_ffi(value: i32) -> WasmtimeResult<Strategy> {
        match value {
            0 => Ok(Strategy::Auto),
            1 => Ok(Strategy::Cranelift),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid strategy value: {}. Expected 0 (Auto) or 1 (Cranelift)", value)
            }),
        }
    }
    
    fn to_ffi(enum_value: Strategy) -> i32 {
        match enum_value {
            Strategy::Auto => 0,
            Strategy::Cranelift => 1,
            _ => 1, // Default to Cranelift for unknown strategies
        }
    }
    
    fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0 | 1 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid strategy value: {}. Expected 0 (Auto) or 1 (Cranelift)", value)
            }),
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
            _ => FfiStrategy::Cranelift, // Default to Cranelift for unknown strategies
        }
    }
}

/// FFI-compatible OptLevel enum representation
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[allow(missing_docs)]
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
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid optimization level: {}. Expected 0 (None), 1 (Speed), or 2 (SpeedAndSize)", value)
            }),
        }
    }
    
    fn to_ffi(enum_value: OptLevel) -> i32 {
        match enum_value {
            OptLevel::None => 0,
            OptLevel::Speed => 1,
            OptLevel::SpeedAndSize => 2,
            _ => 1, // Default to Speed for any new variants
        }
    }
    
    fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0..=2 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid optimization level: {}. Expected 0 (None), 1 (Speed), or 2 (SpeedAndSize)", value)
            }),
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
            _ => FfiOptLevel::Speed, // Default to Speed for any new variants
        }
    }
}

/// FFI-compatible WasmFeature enum representation
#[repr(i32)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[allow(missing_docs)]
pub enum FfiWasmFeature {
    Threads = 0,
    ReferenceTypes = 1,
    Simd = 2,
    BulkMemory = 3,
    MultiValue = 4,
    MultiMemory = 5,
    TailCall = 6,
    RelaxedSimd = 7,
    FunctionReferences = 8,
    Gc = 9,
    Exceptions = 10,
    Memory64 = 11,
    ExtendedConst = 12,
    ComponentModel = 13,
    CustomPageSizes = 14,
    WideArithmetic = 15,
    StackSwitching = 16,
    SharedEverythingThreads = 17,
    ComponentModelAsync = 18,
    ComponentModelAsyncBuiltins = 19,
    ComponentModelAsyncStackful = 20,
    ComponentModelErrorContext = 21,
    ComponentModelGc = 22,
}

impl ParameterConverter<WasmFeature> for FfiWasmFeature {
    fn from_ffi(value: i32) -> WasmtimeResult<WasmFeature> {
        match value {
            0 => Ok(WasmFeature::Threads),
            1 => Ok(WasmFeature::ReferenceTypes),
            2 => Ok(WasmFeature::Simd),
            3 => Ok(WasmFeature::BulkMemory),
            4 => Ok(WasmFeature::MultiValue),
            5 => Ok(WasmFeature::MultiMemory),
            6 => Ok(WasmFeature::TailCall),
            7 => Ok(WasmFeature::RelaxedSimd),
            8 => Ok(WasmFeature::FunctionReferences),
            9 => Ok(WasmFeature::Gc),
            10 => Ok(WasmFeature::Exceptions),
            11 => Ok(WasmFeature::Memory64),
            12 => Ok(WasmFeature::ExtendedConst),
            13 => Ok(WasmFeature::ComponentModel),
            14 => Ok(WasmFeature::CustomPageSizes),
            15 => Ok(WasmFeature::WideArithmetic),
            16 => Ok(WasmFeature::StackSwitching),
            17 => Ok(WasmFeature::SharedEverythingThreads),
            18 => Ok(WasmFeature::ComponentModelAsync),
            19 => Ok(WasmFeature::ComponentModelAsyncBuiltins),
            20 => Ok(WasmFeature::ComponentModelAsyncStackful),
            21 => Ok(WasmFeature::ComponentModelErrorContext),
            22 => Ok(WasmFeature::ComponentModelGc),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid WASM feature: {}. Expected 0-22", value)
            }),
        }
    }
    
    fn to_ffi(enum_value: WasmFeature) -> i32 {
        match enum_value {
            WasmFeature::Threads => 0,
            WasmFeature::ReferenceTypes => 1,
            WasmFeature::Simd => 2,
            WasmFeature::BulkMemory => 3,
            WasmFeature::MultiValue => 4,
            WasmFeature::MultiMemory => 5,
            WasmFeature::TailCall => 6,
            WasmFeature::RelaxedSimd => 7,
            WasmFeature::FunctionReferences => 8,
            WasmFeature::Gc => 9,
            WasmFeature::Exceptions => 10,
            WasmFeature::Memory64 => 11,
            WasmFeature::ExtendedConst => 12,
            WasmFeature::ComponentModel => 13,
            WasmFeature::CustomPageSizes => 14,
            WasmFeature::WideArithmetic => 15,
            WasmFeature::StackSwitching => 16,
            WasmFeature::SharedEverythingThreads => 17,
            WasmFeature::ComponentModelAsync => 18,
            WasmFeature::ComponentModelAsyncBuiltins => 19,
            WasmFeature::ComponentModelAsyncStackful => 20,
            WasmFeature::ComponentModelErrorContext => 21,
            WasmFeature::ComponentModelGc => 22,
        }
    }

    fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0..=22 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid WASM feature: {}. Expected 0-22", value)
            }),
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
            FfiWasmFeature::MultiMemory => WasmFeature::MultiMemory,
            FfiWasmFeature::TailCall => WasmFeature::TailCall,
            FfiWasmFeature::RelaxedSimd => WasmFeature::RelaxedSimd,
            FfiWasmFeature::FunctionReferences => WasmFeature::FunctionReferences,
            FfiWasmFeature::Gc => WasmFeature::Gc,
            FfiWasmFeature::Exceptions => WasmFeature::Exceptions,
            FfiWasmFeature::Memory64 => WasmFeature::Memory64,
            FfiWasmFeature::ExtendedConst => WasmFeature::ExtendedConst,
            FfiWasmFeature::ComponentModel => WasmFeature::ComponentModel,
            FfiWasmFeature::CustomPageSizes => WasmFeature::CustomPageSizes,
            FfiWasmFeature::WideArithmetic => WasmFeature::WideArithmetic,
            FfiWasmFeature::StackSwitching => WasmFeature::StackSwitching,
            FfiWasmFeature::SharedEverythingThreads => WasmFeature::SharedEverythingThreads,
            FfiWasmFeature::ComponentModelAsync => WasmFeature::ComponentModelAsync,
            FfiWasmFeature::ComponentModelAsyncBuiltins => WasmFeature::ComponentModelAsyncBuiltins,
            FfiWasmFeature::ComponentModelAsyncStackful => WasmFeature::ComponentModelAsyncStackful,
            FfiWasmFeature::ComponentModelErrorContext => WasmFeature::ComponentModelErrorContext,
            FfiWasmFeature::ComponentModelGc => WasmFeature::ComponentModelGc,
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
            WasmFeature::MultiMemory => FfiWasmFeature::MultiMemory,
            WasmFeature::TailCall => FfiWasmFeature::TailCall,
            WasmFeature::RelaxedSimd => FfiWasmFeature::RelaxedSimd,
            WasmFeature::FunctionReferences => FfiWasmFeature::FunctionReferences,
            WasmFeature::Gc => FfiWasmFeature::Gc,
            WasmFeature::Exceptions => FfiWasmFeature::Exceptions,
            WasmFeature::Memory64 => FfiWasmFeature::Memory64,
            WasmFeature::ExtendedConst => FfiWasmFeature::ExtendedConst,
            WasmFeature::ComponentModel => FfiWasmFeature::ComponentModel,
            WasmFeature::CustomPageSizes => FfiWasmFeature::CustomPageSizes,
            WasmFeature::WideArithmetic => FfiWasmFeature::WideArithmetic,
            WasmFeature::StackSwitching => FfiWasmFeature::StackSwitching,
            WasmFeature::SharedEverythingThreads => FfiWasmFeature::SharedEverythingThreads,
            WasmFeature::ComponentModelAsync => FfiWasmFeature::ComponentModelAsync,
            WasmFeature::ComponentModelAsyncBuiltins => FfiWasmFeature::ComponentModelAsyncBuiltins,
            WasmFeature::ComponentModelAsyncStackful => FfiWasmFeature::ComponentModelAsyncStackful,
            WasmFeature::ComponentModelErrorContext => FfiWasmFeature::ComponentModelErrorContext,
            WasmFeature::ComponentModelGc => FfiWasmFeature::ComponentModelGc,
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
            Err(WasmtimeError::InvalidParameter {
                message: format!("{} cannot be null", name)
            })
        } else {
            Ok(())
        }
    }
    
    /// Validate array bounds
    pub fn validate_array_bounds(array_len: usize, index: usize, name: &str) -> WasmtimeResult<()> {
        if index >= array_len {
            Err(WasmtimeError::InvalidParameter {
                message: format!("{} index {} out of bounds (length: {})", name, index, array_len)
            })
        } else {
            Ok(())
        }
    }
    
    /// Validate that a slice has valid bounds
    pub fn validate_slice_bounds<T>(slice: &[T], start: usize, len: usize, name: &str) -> WasmtimeResult<()> {
        if start >= slice.len() || start.saturating_add(len) > slice.len() {
            Err(WasmtimeError::InvalidParameter {
                message: format!("{} slice bounds invalid: start={}, len={}, slice_len={}", name, start, len, slice.len())
            })
        } else {
            Ok(())
        }
    }
}

/// Error code mapping utilities
pub mod error_mapping {
    use crate::error::WasmtimeError;
    
    /// Map WasmtimeError to standardized FFI error codes
    pub fn map_error_to_code(_error: &WasmtimeError) -> i32 {
        // All errors map to FFI_ERROR (-1) for simplicity and consistency
        // Detailed error information is available through error message retrieval
        super::FFI_ERROR
    }
    
    /// Check if an error should be propagated to FFI boundary
    pub fn should_propagate_error(_error: &WasmtimeError) -> bool {
        // All errors should be propagated as return codes, never as panics
        true
    }
}

/// Component operations shared between JNI and Panama FFI implementations
///
/// This module provides consolidated implementations for WebAssembly Component Model operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
pub mod component {
    use crate::component::{ComponentEngine, Component};
    use crate::error::{WasmtimeResult, WasmtimeError};
    use std::os::raw::c_void;
    use std::sync::Arc;
    use super::{PointerReturnConverter, ReturnValueConverter, validation};

    /// Shared implementation for creating component engine
    pub fn create_component_engine_shared() -> WasmtimeResult<Box<ComponentEngine>> {
        crate::component::core::create_component_engine()
    }

    /// Shared implementation for loading component from bytes
    pub fn load_component_from_bytes_shared<B>(
        engine_ptr: *mut c_void,
        component_bytes: B
    ) -> WasmtimeResult<Box<Component>>
    where
        B: super::module::ByteArrayConverter,
    {
        validation::validate_not_null(engine_ptr, "component_engine")?;
        
        if component_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Component bytes cannot be empty".to_string()
            });
        }

        let engine = unsafe { crate::component::core::get_component_engine_ref(engine_ptr)? };
        let bytes = unsafe { component_bytes.get_bytes()? };
        crate::component::core::load_component_from_bytes(engine, bytes)
    }

    /// Shared implementation for component instantiation
    pub fn instantiate_component_shared(
        engine_ptr: *mut c_void,
        component_ptr: *mut c_void
    ) -> WasmtimeResult<*mut c_void> {
        validation::validate_not_null(engine_ptr, "component_engine")?;
        validation::validate_not_null(component_ptr, "component")?;

        let engine = unsafe { crate::component::core::get_component_engine_ref(engine_ptr)? };
        let component = unsafe { crate::component::core::get_component_ref(component_ptr)? };
        
        let instance = crate::component::core::instantiate_component(engine, component)?;
        Ok(Arc::into_raw(instance) as *mut c_void)
    }

    /// Shared implementation for getting component size
    pub fn get_component_size_shared(component_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(component_ptr, "component")?;
        
        let component = unsafe { crate::component::core::get_component_ref(component_ptr)? };
        Ok(crate::component::core::get_component_size(component))
    }

    /// Shared implementation for getting component exports count
    pub fn get_component_exports_count_shared(component_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(component_ptr, "component")?;
        
        let component = unsafe { crate::component::core::get_component_ref(component_ptr)? };
        Ok(crate::component::core::get_export_count(component))
    }

    /// Shared implementation for getting component imports count
    pub fn get_component_imports_count_shared(component_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(component_ptr, "component")?;
        
        let component = unsafe { crate::component::core::get_component_ref(component_ptr)? };
        Ok(crate::component::core::get_import_count(component))
    }

    /// Shared implementation for getting active instances count
    pub fn get_active_instances_count_shared(engine_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(engine_ptr, "component_engine")?;
        
        let engine = unsafe { crate::component::core::get_component_engine_ref(engine_ptr)? };
        let instances = crate::component::core::get_active_instances(engine)?;
        Ok(instances.len())
    }

    /// Shared implementation for cleanup instances
    pub fn cleanup_instances_shared(engine_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(engine_ptr, "component_engine")?;
        
        let engine = unsafe { crate::component::core::get_component_engine_ref(engine_ptr)? };
        let instances_before = crate::component::core::get_active_instances(engine)?.len();
        // Note: The actual cleanup of inactive instances would need to be implemented
        // in the component engine, currently we just return the current count
        Ok(instances_before)
    }

    /// Shared implementation for component engine destruction
    pub fn destroy_component_engine_shared(engine_ptr: *mut c_void) {
        if !engine_ptr.is_null() {
            unsafe { crate::component::core::destroy_component_engine(engine_ptr); }
        }
    }

    /// Shared implementation for component destruction
    pub fn destroy_component_shared(component_ptr: *mut c_void) {
        if !component_ptr.is_null() {
            unsafe { crate::component::core::destroy_component(component_ptr); }
        }
    }

    /// Shared implementation for component instance destruction
    pub fn destroy_component_instance_shared(instance_ptr: *mut c_void) {
        if !instance_ptr.is_null() {
            unsafe { crate::component::core::destroy_component_instance(instance_ptr); }
        }
    }

    /// Helper function to convert create result to FFI pointer
    pub fn create_result_to_ffi_ptr(result: WasmtimeResult<Box<ComponentEngine>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert load result to FFI pointer
    pub fn load_result_to_ffi_ptr(result: WasmtimeResult<Box<Component>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert instantiate result to FFI code and pointer
    pub fn instantiate_result_to_ffi_result(result: WasmtimeResult<*mut c_void>) -> (i32, *mut c_void) {
        match result {
            Ok(ptr) => (super::FFI_SUCCESS, ptr),
            Err(_) => (super::FFI_ERROR, std::ptr::null_mut()),
        }
    }

    /// Helper function to convert size result to FFI result tuple
    pub fn size_result_to_ffi_result(result: WasmtimeResult<usize>) -> (i32, usize) {
        match result {
            Ok(size) => (super::FFI_SUCCESS, size),
            Err(_) => (super::FFI_ERROR, 0),
        }
    }

    /// Helper function to convert count result to FFI result tuple
    pub fn count_result_to_ffi_result(result: WasmtimeResult<usize>) -> (i32, i32) {
        match result {
            Ok(count) => (super::FFI_SUCCESS, count as i32),
            Err(_) => (super::FFI_ERROR, -1),
        }
    }
}

/// Memory operations shared between JNI and Panama FFI implementations
/// 
/// This module provides consolidated implementations for WebAssembly memory operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
pub mod memory {
    use crate::memory::{Memory, MemoryBuilder, MemoryConfig, MemoryUsage, MemoryDataType, MemoryRegistry};
    use crate::store::Store;
    use crate::error::WasmtimeResult;
    use std::os::raw::c_void;
    use std::sync::Arc;
    use super::{PointerReturnConverter, ReturnValueConverter, IntegerReturnConverter, validation};

    /// Shared implementation for creating memory
    pub fn create_memory_shared(
        store_ptr: *mut c_void,
        initial_pages: u32
    ) -> WasmtimeResult<Box<Memory>> {
        validation::validate_not_null(store_ptr, "store")?;
        
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        Memory::new(store, initial_pages as u64).map(|memory| Box::new(memory))
    }

    /// Shared implementation for creating memory with configuration
    pub fn create_memory_with_config_shared<S>(
        store_ptr: *mut c_void,
        initial_pages: u32,
        maximum_pages: i32,
        is_shared: bool,
        memory_index: u32,
        name: S
    ) -> WasmtimeResult<Box<Memory>>
    where
        S: super::module::StringConverter,
    {
        validation::validate_not_null(store_ptr, "store")?;
        
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        
        let mut builder = MemoryBuilder::new(initial_pages as u64);
        
        if maximum_pages >= 0 {
            builder = builder.maximum_pages(maximum_pages as u64);
        }
        
        if is_shared {
            builder = builder.shared();
        }
        
        builder = builder.memory_index(memory_index);
        
        if !name.is_empty() {
            let name_str = unsafe { name.get_string()? };
            builder = builder.name(name_str);
        }
        
        let config = MemoryConfig::from(builder);
        
        Memory::new_with_config(store, config).map(|memory| Box::new(memory))
    }

    /// Shared implementation for getting memory size in pages
    pub fn get_memory_size_pages_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void
    ) -> WasmtimeResult<u64> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &*(store_ptr as *const Store) };
        memory.size_pages(store)
    }

    /// Shared implementation for getting memory size in bytes
    pub fn get_memory_size_bytes_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void
    ) -> WasmtimeResult<usize> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &*(store_ptr as *const Store) };
        memory.size_bytes(store)
    }

    /// Shared implementation for growing memory
    pub fn grow_memory_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        additional_pages: u64
    ) -> WasmtimeResult<u64> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        memory.grow(store, additional_pages)
    }

    /// Shared implementation for reading memory bytes
    pub fn read_memory_bytes_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        length: usize
    ) -> WasmtimeResult<Vec<u8>> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &*(store_ptr as *const Store) };
        memory.read_bytes(store, offset, length)
    }

    /// Shared implementation for writing memory bytes
    pub fn write_memory_bytes_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        data: &[u8]
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        memory.write_bytes(store, offset, data)
    }

    /// Shared implementation for reading typed memory data
    pub fn read_memory_u32_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize
    ) -> WasmtimeResult<u32> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &*(store_ptr as *const Store) };
        memory.read_typed::<u32>(store, offset, MemoryDataType::U32Le)
    }

    /// Shared implementation for writing typed memory data
    pub fn write_memory_u32_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void,
        offset: usize,
        value: u32
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        memory.write_typed(store, offset, value, MemoryDataType::U32Le)
    }

    /// Shared implementation for getting memory usage
    pub fn get_memory_usage_shared(
        memory_ptr: *mut c_void,
        store_ptr: *mut c_void
    ) -> WasmtimeResult<MemoryUsage> {
        validation::validate_not_null(memory_ptr, "memory")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let memory = unsafe { &*(memory_ptr as *const Memory) };
        let store = unsafe { &*(store_ptr as *const Store) };
        memory.get_usage(store)
    }

    /// Shared implementation for creating memory registry
    pub fn create_memory_registry_shared() -> WasmtimeResult<Box<MemoryRegistry>> {
        Ok(Box::new(MemoryRegistry::new()))
    }

    /// Shared implementation for registering memory
    pub fn register_memory_shared(
        registry_ptr: *mut c_void,
        memory_ptr: *mut c_void
    ) -> WasmtimeResult<u32> {
        validation::validate_not_null(registry_ptr, "memory_registry")?;
        validation::validate_not_null(memory_ptr, "memory")?;
        
        let registry = unsafe { &*(registry_ptr as *const MemoryRegistry) };
        let memory = unsafe { (memory_ptr as *const Memory).read() };
        registry.register(memory)
    }

    /// Shared implementation for getting memory from registry
    pub fn get_memory_from_registry_shared(
        registry_ptr: *mut c_void,
        memory_id: u32
    ) -> WasmtimeResult<Arc<Memory>> {
        validation::validate_not_null(registry_ptr, "memory_registry")?;
        
        let registry = unsafe { &*(registry_ptr as *const MemoryRegistry) };
        registry.get(memory_id)
    }

    /// Shared implementation for memory destruction
    pub fn destroy_memory_shared(memory_ptr: *mut c_void) {
        if !memory_ptr.is_null() {
            unsafe { 
                let _ = Box::from_raw(memory_ptr as *mut Memory);
            }
        }
    }

    /// Shared implementation for memory registry destruction
    pub fn destroy_memory_registry_shared(registry_ptr: *mut c_void) {
        if !registry_ptr.is_null() {
            unsafe { 
                let _ = Box::from_raw(registry_ptr as *mut MemoryRegistry);
            }
        }
    }

    /// Helper function to convert create result to FFI pointer
    pub fn create_result_to_ffi_ptr(result: WasmtimeResult<Box<Memory>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert registry create result to FFI code and pointer
    pub fn registry_create_result_to_ffi_result(result: WasmtimeResult<Box<MemoryRegistry>>) -> (i32, *mut c_void) {
        match result {
            Ok(registry) => (super::FFI_SUCCESS, Box::into_raw(registry) as *mut c_void),
            Err(_) => (super::FFI_ERROR, std::ptr::null_mut()),
        }
    }

    /// Helper function to convert size result to FFI result tuple
    pub fn size_result_to_ffi_result(result: WasmtimeResult<u64>) -> (i32, u64) {
        match result {
            Ok(size) => (super::FFI_SUCCESS, size),
            Err(_) => (super::FFI_ERROR, 0),
        }
    }

    /// Helper function to convert bytes size result to FFI result tuple
    pub fn bytes_size_result_to_ffi_result(result: WasmtimeResult<usize>) -> (i32, usize) {
        match result {
            Ok(size) => (super::FFI_SUCCESS, size),
            Err(_) => (super::FFI_ERROR, 0),
        }
    }

    /// Helper function to convert u32 result to FFI result tuple
    pub fn u32_result_to_ffi_result(result: WasmtimeResult<u32>) -> (i32, u32) {
        match result {
            Ok(value) => (super::FFI_SUCCESS, value),
            Err(_) => (super::FFI_ERROR, 0),
        }
    }

    /// Helper function to convert register result to FFI result tuple
    pub fn register_result_to_ffi_result(result: WasmtimeResult<u32>) -> (i32, u32) {
        match result {
            Ok(id) => (super::FFI_SUCCESS, id),
            Err(_) => (super::FFI_ERROR, u32::MAX),
        }
    }

    /// Helper function to convert operation result to FFI error code
    pub fn operation_result_to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        IntegerReturnConverter::to_ffi_code(result)
    }
}

/// Global operations shared between JNI and Panama FFI implementations
/// 
/// This module provides consolidated implementations for WebAssembly global operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
pub mod global {
    use crate::global::{Global, GlobalValue, GlobalMetadata, core};
    
    use crate::store::Store;
    use crate::error::{WasmtimeResult, WasmtimeError};
    use std::os::raw::c_void;
    use wasmtime::{ValType, Mutability};
    use super::{PointerReturnConverter, ReturnValueConverter, IntegerReturnConverter, validation};

    /// FFI-compatible representation of GlobalValue
    #[repr(C)]
    #[derive(Debug, Clone, Copy)]
    #[allow(missing_docs)]
    pub struct FfiGlobalValue {
        pub value_type: i32,  // 0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FUNCREF, 6=EXTERNREF
        pub i32_value: i32,
        pub i64_value: i64,
        pub f32_value: f32,
        pub f64_value: f64,
        pub v128_value: u128,
    }

    impl FfiGlobalValue {
        /// Convert FFI global value to native GlobalValue
        pub fn to_native(self) -> WasmtimeResult<GlobalValue> {
            match self.value_type {
                0 => Ok(GlobalValue::I32(self.i32_value)),
                1 => Ok(GlobalValue::I64(self.i64_value)),
                2 => Ok(GlobalValue::F32(self.f32_value)),
                3 => Ok(GlobalValue::F64(self.f64_value)),
                4 => Ok(GlobalValue::V128(self.v128_value.to_le_bytes())),
                5 => Ok(GlobalValue::FuncRef(None)),
                6 => Ok(GlobalValue::ExternRef(None)),
                7 => Ok(GlobalValue::AnyRef(None)),
                _ => Err(WasmtimeError::InvalidParameter {
                message:
                    format!("Invalid global value type: {}", self.value_type)
                }),
            }
        }

        /// Convert native GlobalValue to FFI representation
        pub fn from_native(value: &GlobalValue) -> Self {
            match value {
                GlobalValue::I32(v) => FfiGlobalValue {
                    value_type: 0,
                    i32_value: *v,
                    i64_value: 0,
                    f32_value: 0.0,
                    f64_value: 0.0,
                    v128_value: 0,
                },
                GlobalValue::I64(v) => FfiGlobalValue {
                    value_type: 1,
                    i32_value: 0,
                    i64_value: *v,
                    f32_value: 0.0,
                    f64_value: 0.0,
                    v128_value: 0,
                },
                GlobalValue::F32(v) => FfiGlobalValue {
                    value_type: 2,
                    i32_value: 0,
                    i64_value: 0,
                    f32_value: *v,
                    f64_value: 0.0,
                    v128_value: 0,
                },
                GlobalValue::F64(v) => FfiGlobalValue {
                    value_type: 3,
                    i32_value: 0,
                    i64_value: 0,
                    f32_value: 0.0,
                    f64_value: *v,
                    v128_value: 0,
                },
                GlobalValue::V128(v) => FfiGlobalValue {
                    value_type: 4,
                    i32_value: 0,
                    i64_value: 0,
                    f32_value: 0.0,
                    f64_value: 0.0,
                    v128_value: u128::from_le_bytes(*v),
                },
                GlobalValue::FuncRef(_) => FfiGlobalValue {
                    value_type: 5,
                    i32_value: 0,
                    i64_value: 0,
                    f32_value: 0.0,
                    f64_value: 0.0,
                    v128_value: 0,
                },
                GlobalValue::ExternRef(_) => FfiGlobalValue {
                    value_type: 6,
                    i32_value: 0,
                    i64_value: 0,
                    f32_value: 0.0,
                    f64_value: 0.0,
                    v128_value: 0,
                },
                GlobalValue::AnyRef(_) => FfiGlobalValue {
                    value_type: 7,
                    i32_value: 0,
                    i64_value: 0,
                    f32_value: 0.0,
                    f64_value: 0.0,
                    v128_value: 0,
                },
            }
        }
    }

    /// Shared implementation for creating global variable
    pub fn create_global_shared(
        store_ptr: *mut c_void,
        value_type: i32,
        is_mutable: bool,
        initial_value: FfiGlobalValue
    ) -> WasmtimeResult<Box<Global>> {
        validation::validate_not_null(store_ptr, "store")?;
        
        let store = unsafe { &*(store_ptr as *const Store) };
        let native_value = initial_value.to_native()?;
        
        // Convert FFI value type to native ValType
        let val_type = match value_type {
            0 => ValType::I32,
            1 => ValType::I64,
            2 => ValType::F32,
            3 => ValType::F64,
            4 => ValType::V128,
            5 | 6 | 7 => ValType::ANYREF,
            _ => return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid value type: {}", value_type)
            }),
        };

        let mutability = if is_mutable { 
            Mutability::Var 
        } else { 
            Mutability::Const 
        };

        core::create_global(store, val_type, mutability, native_value, None)
    }

    /// Shared implementation for getting global variable value
    pub fn get_global_value_shared(
        global_ptr: *mut c_void,
        store_ptr: *mut c_void
    ) -> WasmtimeResult<FfiGlobalValue> {
        validation::validate_not_null(global_ptr, "global")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let global = unsafe { core::get_global_ref(global_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        
        let value = core::get_global_value(global, store)?;
        Ok(FfiGlobalValue::from_native(&value))
    }

    /// Shared implementation for setting global variable value
    pub fn set_global_value_shared(
        global_ptr: *mut c_void,
        store_ptr: *mut c_void,
        value: FfiGlobalValue
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(global_ptr, "global")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let global = unsafe { core::get_global_ref(global_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        let native_value = value.to_native()?;
        
        core::set_global_value(global, store, native_value)
    }

    /// Shared implementation for getting global metadata
    pub fn get_global_metadata_shared(global_ptr: *mut c_void) -> WasmtimeResult<GlobalMetadata> {
        validation::validate_not_null(global_ptr, "global")?;
        
        let global = unsafe { core::get_global_ref(global_ptr)? };
        Ok(core::get_global_metadata(global).clone())
    }

    /// Shared implementation for global destruction
    pub fn destroy_global_shared(global_ptr: *mut c_void) {
        if !global_ptr.is_null() {
            unsafe { core::destroy_global(global_ptr); }
        }
    }

    /// Helper function to convert create result to FFI pointer
    pub fn create_result_to_ffi_ptr(result: WasmtimeResult<Box<Global>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert get value result to FFI result tuple
    pub fn get_value_result_to_ffi_result(result: WasmtimeResult<FfiGlobalValue>) -> (i32, FfiGlobalValue) {
        match result {
            Ok(value) => (super::FFI_SUCCESS, value),
            Err(_) => (super::FFI_ERROR, FfiGlobalValue {
                value_type: -1,
                i32_value: 0,
                i64_value: 0,
                f32_value: 0.0,
                f64_value: 0.0,
                v128_value: 0,
            }),
        }
    }

    /// Helper function to convert operation result to FFI error code
    pub fn operation_result_to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        IntegerReturnConverter::to_ffi_code(result)
    }
}

/// Table operations shared between JNI and Panama FFI implementations
/// 
/// This module provides consolidated implementations for WebAssembly table operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
pub mod table {
    use crate::table::{Table, TableElement, TableMetadata, core};
    use wasmtime::{ValType, RefType};
    
    use crate::store::Store;
    use crate::error::{WasmtimeResult, WasmtimeError};
    use std::os::raw::c_void;
    use super::{PointerReturnConverter, ReturnValueConverter, IntegerReturnConverter, validation};

    /// FFI-compatible representation of TableElement
    #[repr(C)]
    #[derive(Debug, Clone, Copy)]
    #[allow(missing_docs)]
    pub struct FfiTableElement {
        pub element_type: i32,  // 0=FuncRef, 1=ExternRef, 2=AnyRef
        pub ptr_value: *mut c_void,  // For storing reference pointers
    }

    impl FfiTableElement {
        /// Convert FFI table element to native TableElement
        pub fn to_native(self) -> WasmtimeResult<TableElement> {
            match self.element_type {
                0 => Ok(TableElement::FuncRef(None)),
                1 => Ok(TableElement::ExternRef(None)),
                2 => Ok(TableElement::AnyRef(None)),
                _ => Err(WasmtimeError::InvalidParameter {
                message:
                    format!("Invalid table element type: {}", self.element_type)
                }),
            }
        }

        /// Convert native TableElement to FFI representation
        pub fn from_native(element: &TableElement) -> Self {
            match element {
                TableElement::FuncRef(_) => FfiTableElement {
                    element_type: 0,
                    ptr_value: std::ptr::null_mut(),
                },
                TableElement::ExternRef(_) => FfiTableElement {
                    element_type: 1,
                    ptr_value: std::ptr::null_mut(),
                },
                TableElement::AnyRef(_) => FfiTableElement {
                    element_type: 2,
                    ptr_value: std::ptr::null_mut(),
                },
            }
        }
    }

    /// Shared implementation for creating table
    pub fn create_table_shared(
        store_ptr: *mut c_void,
        element_type: i32,
        initial_size: u32,
        maximum_size: i32
    ) -> WasmtimeResult<Box<Table>> {
        validation::validate_not_null(store_ptr, "store")?;
        
        let store = unsafe { &*(store_ptr as *const Store) };
        
        // Convert FFI element type to ValType 
        let val_type = match element_type {
            0 => ValType::Ref(RefType::FUNCREF),
            1 => ValType::Ref(RefType::EXTERNREF), 
            2 => ValType::Ref(RefType::FUNCREF), // Default AnyRef to FuncRef for now
            _ => return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid element type: {}", element_type)
            }),
        };

        let max_size = if maximum_size < 0 { None } else { Some(maximum_size as u32) };
        
        core::create_table(store, val_type, initial_size, max_size, None)
    }

    /// Shared implementation for getting table size
    pub fn get_table_size_shared(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void
    ) -> WasmtimeResult<u32> {
        validation::validate_not_null(table_ptr, "table")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        core::get_table_size(table, store)
    }

    /// Shared implementation for getting table element
    pub fn get_table_element_shared(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        index: u32
    ) -> WasmtimeResult<FfiTableElement> {
        validation::validate_not_null(table_ptr, "table")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        
        let element = core::get_table_element(table, store, index)?;
        Ok(FfiTableElement::from_native(&element))
    }

    /// Shared implementation for setting table element
    pub fn set_table_element_shared(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        index: u32,
        element: FfiTableElement
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(table_ptr, "table")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        let native_element = element.to_native()?;
        
        core::set_table_element(table, store, index, native_element)
    }

    /// Shared implementation for growing table
    pub fn grow_table_shared(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        delta: u32,
        init_element: FfiTableElement
    ) -> WasmtimeResult<u32> {
        validation::validate_not_null(table_ptr, "table")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        let native_init = init_element.to_native()?;
        
        core::grow_table(table, store, delta, native_init)
    }

    /// Shared implementation for filling table
    pub fn fill_table_shared(
        table_ptr: *mut c_void,
        store_ptr: *mut c_void,
        start_index: u32,
        count: u32,
        fill_element: FfiTableElement
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(table_ptr, "table")?;
        validation::validate_not_null(store_ptr, "store")?;
        
        let table = unsafe { core::get_table_ref(table_ptr)? };
        let store = unsafe { &*(store_ptr as *const Store) };
        let native_fill = fill_element.to_native()?;
        
        core::fill_table(table, store, start_index, native_fill, count)
    }

    /// Shared implementation for getting table metadata
    pub fn get_table_metadata_shared(table_ptr: *mut c_void) -> WasmtimeResult<TableMetadata> {
        validation::validate_not_null(table_ptr, "table")?;
        
        let table = unsafe { core::get_table_ref(table_ptr)? };
        Ok(core::get_table_metadata(table).clone())
    }

    /// Shared implementation for table destruction
    pub fn destroy_table_shared(table_ptr: *mut c_void) {
        if !table_ptr.is_null() {
            unsafe { core::destroy_table(table_ptr); }
        }
    }

    /// Helper function to convert create result to FFI pointer
    pub fn create_result_to_ffi_ptr(result: WasmtimeResult<Box<Table>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert size result to FFI result tuple
    pub fn size_result_to_ffi_result(result: WasmtimeResult<u32>) -> (i32, u32) {
        match result {
            Ok(size) => (super::FFI_SUCCESS, size),
            Err(_) => (super::FFI_ERROR, 0),
        }
    }

    /// Helper function to convert get element result to FFI result tuple
    pub fn get_element_result_to_ffi_result(result: WasmtimeResult<FfiTableElement>) -> (i32, FfiTableElement) {
        match result {
            Ok(element) => (super::FFI_SUCCESS, element),
            Err(_) => (super::FFI_ERROR, FfiTableElement {
                element_type: -1,
                ptr_value: std::ptr::null_mut(),
            }),
        }
    }

    /// Helper function to convert operation result to FFI error code
    pub fn operation_result_to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        IntegerReturnConverter::to_ffi_code(result)
    }

    /// Helper function to convert grow result to FFI result tuple
    pub fn grow_result_to_ffi_result(result: WasmtimeResult<u32>) -> (i32, u32) {
        match result {
            Ok(previous_size) => (super::FFI_SUCCESS, previous_size),
            Err(_) => (super::FFI_ERROR, u32::MAX),
        }
    }
}

/// Host function operations shared between JNI and Panama FFI implementations
/// 
/// This module provides consolidated implementations for WebAssembly host function operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
/// 
/// Note: Callback handling remains interface-specific due to different calling conventions.
pub mod hostfunc {
    use crate::hostfunc::{HostFunction, HostFunctionBuilder, core};
    use crate::error::{WasmtimeResult, WasmtimeError};
    use std::os::raw::c_void;
    use std::sync::Arc;
    use wasmtime::ValType;
    use super::{PointerReturnConverter, ReturnValueConverter, IntegerReturnConverter, validation};

    /// Shared implementation for creating host function builder
    pub fn create_host_function_builder_shared<S>(name: S) -> WasmtimeResult<Box<HostFunctionBuilder>>
    where
        S: super::module::StringConverter,
    {
        if name.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message:
                "Host function name cannot be empty".to_string()
            });
        }

        let name_str = unsafe { name.get_string()? };
        Ok(core::create_host_function_builder(&name_str))
    }

    /// Shared implementation for adding parameter type to builder
    pub fn add_param_type_shared(
        builder_ptr: *mut c_void,
        val_type: i32
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(builder_ptr, "host_function_builder")?;
        
        let builder = unsafe { &mut *(builder_ptr as *mut HostFunctionBuilder) };
        
        let wasmtime_val_type = match val_type {
            0 => ValType::I32,
            1 => ValType::I64,
            2 => ValType::F32,
            3 => ValType::F64,
            4 => ValType::V128,
            5 => ValType::ANYREF,
            _ => return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid parameter type: {}", val_type)
            }),
        };
        
        core::builder_add_param(builder, wasmtime_val_type);
        Ok(())
    }

    /// Shared implementation for adding return type to builder
    pub fn add_return_type_shared(
        builder_ptr: *mut c_void,
        val_type: i32
    ) -> WasmtimeResult<()> {
        validation::validate_not_null(builder_ptr, "host_function_builder")?;
        
        let builder = unsafe { &mut *(builder_ptr as *mut HostFunctionBuilder) };
        
        let wasmtime_val_type = match val_type {
            0 => ValType::I32,
            1 => ValType::I64,
            2 => ValType::F32,
            3 => ValType::F64,
            4 => ValType::V128,
            5 => ValType::ANYREF,
            _ => return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid return type: {}", val_type)
            }),
        };
        
        core::builder_add_result(builder, wasmtime_val_type);
        Ok(())
    }

    /// Shared implementation for getting host function from registry
    pub fn get_host_function_shared(id: u64) -> WasmtimeResult<Arc<HostFunction>> {
        core::get_host_function(id)
    }

    /// Shared implementation for removing host function from registry
    pub fn remove_host_function_shared(id: u64) -> WasmtimeResult<()> {
        core::remove_host_function(id)
    }

    /// Shared implementation for getting registry statistics
    pub fn get_registry_stats_shared() -> WasmtimeResult<(usize, u64)> {
        core::get_registry_stats()
    }

    /// Shared implementation for host function builder destruction
    pub fn destroy_builder_shared(builder_ptr: *mut c_void) {
        if !builder_ptr.is_null() {
            unsafe { 
                let _ = Box::from_raw(builder_ptr as *mut HostFunctionBuilder);
            }
        }
    }

    /// Helper function to convert create builder result to FFI pointer
    pub fn create_builder_result_to_ffi_ptr(result: WasmtimeResult<Box<HostFunctionBuilder>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert operation result to FFI error code
    pub fn operation_result_to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        IntegerReturnConverter::to_ffi_code(result)
    }

    /// Helper function to convert registry stats result to FFI result tuple
    pub fn stats_result_to_ffi_result(result: WasmtimeResult<(usize, u64)>) -> (i32, usize, u64) {
        match result {
            Ok((count, next_id)) => (super::FFI_SUCCESS, count, next_id),
            Err(_) => (super::FFI_ERROR, 0, 0),
        }
    }
}

/// Module operations shared between JNI and Panama FFI implementations
/// 
/// This module provides consolidated implementations for WebAssembly module operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
pub mod module {
    use crate::engine::Engine;
    use crate::module::{Module, core};
    use crate::error::{WasmtimeResult, WasmtimeError};
    use std::os::raw::c_void;
    use super::{PointerReturnConverter, ReturnValueConverter, IntegerReturnConverter, validation};

    /// Trait for handling byte array conversion in module operations
    /// 
    /// Different FFI interfaces (JNI vs Panama) handle byte arrays differently.
    /// This trait provides a consistent interface for byte array access.
    pub trait ByteArrayConverter {
        /// Get a slice to the byte data with validation
        unsafe fn get_bytes(&self) -> WasmtimeResult<&[u8]>;
        
        /// Get the length of the byte array
        fn len(&self) -> usize;
        
        /// Check if the byte array is empty
        fn is_empty(&self) -> bool {
            self.len() == 0
        }
    }

    /// Trait for handling string conversion in module operations
    /// 
    /// Different FFI interfaces handle strings differently (JString vs char*).
    /// This trait provides a consistent interface for string access.
    pub trait StringConverter {
        /// Get string content with validation
        unsafe fn get_string(&self) -> WasmtimeResult<String>;
        
        /// Check if string is empty or null
        fn is_empty(&self) -> bool;
    }

    /// Shared implementation for module compilation from bytes
    /// 
    /// This function provides unified module compilation logic that works
    /// with any byte array representation through the ByteArrayConverter trait.
    pub fn compile_module_shared<B>(
        engine: &Engine,
        wasm_bytes: B
    ) -> WasmtimeResult<Box<Module>>
    where
        B: ByteArrayConverter,
    {
        // Validate byte array
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message:
                "WebAssembly bytes cannot be empty".to_string()
            });
        }

        // Get bytes safely and compile
        let bytes = unsafe { wasm_bytes.get_bytes()? };
        core::compile_module(engine, bytes)
    }

    /// Shared implementation for module compilation from bytes
    ///
    /// This function provides unified WASM bytecode compilation logic that works
    /// with any byte array representation through the ByteArrayConverter trait.
    pub fn compile_module_bytes_shared<B>(
        engine: &Engine,
        wasm_bytes: B
    ) -> WasmtimeResult<Box<Module>>
    where
        B: ByteArrayConverter,
    {
        // Validate byte array
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly bytes cannot be empty".to_string()
            });
        }

        // Get bytes safely and compile
        let bytes = unsafe { wasm_bytes.get_bytes()? };
        core::compile_module(engine, bytes)
    }

    /// Shared implementation for module compilation from WAT
    ///
    /// This function provides unified WAT compilation logic that works
    /// with any string representation through the StringConverter trait.
    pub fn compile_module_wat_shared<S>(
        engine: &Engine,
        wat_string: S
    ) -> WasmtimeResult<Box<Module>>
    where
        S: StringConverter,
    {
        // Validate string
        if wat_string.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message:
                "WAT string cannot be empty".to_string()
            });
        }

        // Get string safely and compile
        let wat = unsafe { wat_string.get_string()? };
        core::compile_module_wat(engine, &wat)
    }

    /// Shared implementation for module validation
    /// 
    /// Validates WebAssembly bytecode without compilation using shared logic.
    pub fn validate_module_shared<B>(wasm_bytes: B) -> WasmtimeResult<()>
    where
        B: ByteArrayConverter,
    {
        // Validate byte array
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message:
                "WebAssembly bytes cannot be empty".to_string()
            });
        }

        // Get bytes safely and validate
        let bytes = unsafe { wasm_bytes.get_bytes()? };
        core::validate_module_bytes(bytes)
    }

    /// Shared implementation for module serialization
    /// 
    /// Serializes a compiled module to bytes for caching purposes.
    pub fn serialize_module_shared(module_ptr: *mut c_void) -> WasmtimeResult<Vec<u8>> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        core::serialize_module(module)
    }

    /// Shared implementation for module deserialization
    /// 
    /// Deserializes a module from bytes using shared logic.
    pub fn deserialize_module_shared<B>(
        engine: &Engine,
        serialized_bytes: B
    ) -> WasmtimeResult<Box<Module>>
    where
        B: ByteArrayConverter,
    {
        // Validate byte array
        if serialized_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message:
                "Serialized module bytes cannot be empty".to_string()
            });
        }

        // Get bytes safely and deserialize
        let bytes = unsafe { serialized_bytes.get_bytes()? };
        core::deserialize_module(engine, bytes)
    }

    /// Shared implementation for getting module size
    pub fn get_module_size_shared(module_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        Ok(core::get_module_size(module))
    }

    /// Shared implementation for getting module name
    pub fn get_module_name_shared(module_ptr: *mut c_void) -> WasmtimeResult<Option<String>> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        Ok(core::get_module_name(module).map(String::from))
    }

    /// Shared implementation for getting export count
    pub fn get_export_count_shared(module_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        Ok(core::get_export_count(module))
    }

    /// Shared implementation for getting import count
    pub fn get_import_count_shared(module_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        Ok(core::get_import_count(module))
    }

    /// Shared implementation for getting function count
    pub fn get_function_count_shared(module_ptr: *mut c_void) -> WasmtimeResult<usize> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        Ok(core::get_function_count(module))
    }

    /// Shared implementation for checking if module has export
    pub fn has_export_shared<S>(
        module_ptr: *mut c_void,
        export_name: S
    ) -> WasmtimeResult<bool>
    where
        S: StringConverter,
    {
        validation::validate_not_null(module_ptr, "module")?;
        
        if export_name.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message:
                "Export name cannot be empty".to_string()
            });
        }

        let module = unsafe { core::get_module_ref(module_ptr)? };
        let name = unsafe { export_name.get_string()? };
        Ok(core::has_export(module, &name))
    }

    /// Shared implementation for getting function export names
    pub fn get_function_exports_shared(module_ptr: *mut c_void) -> WasmtimeResult<Vec<String>> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        let function_exports = core::get_function_exports(module);
        Ok(function_exports.into_iter().map(|exp| exp.name.clone()).collect())
    }

    /// Shared implementation for getting memory export names
    pub fn get_memory_exports_shared(module_ptr: *mut c_void) -> WasmtimeResult<Vec<String>> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        let memory_exports = core::get_memory_exports(module);
        Ok(memory_exports.into_iter().map(|exp| exp.name.clone()).collect())
    }

    /// Shared implementation for getting required import names
    pub fn get_required_imports_shared(module_ptr: *mut c_void) -> WasmtimeResult<Vec<String>> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        let imports = core::get_required_imports(module);
        Ok(imports.iter().map(|imp| format!("{}::{}", imp.module, imp.name)).collect())
    }

    /// Shared implementation for module validation (defensive check)
    pub fn validate_module_functionality_shared(module_ptr: *mut c_void) -> WasmtimeResult<()> {
        validation::validate_not_null(module_ptr, "module")?;
        
        let module = unsafe { core::get_module_ref(module_ptr)? };
        core::validate_module(module)
    }

    /// Shared implementation for module destruction
    pub fn destroy_module_shared(module_ptr: *mut c_void) {
        if !module_ptr.is_null() {
            unsafe { core::destroy_module(module_ptr); }
        }
    }

    /// Helper function to convert compile result to FFI pointer
    pub fn compile_result_to_ffi_ptr(result: WasmtimeResult<Box<Module>>) -> *mut c_void {
        PointerReturnConverter::to_ffi_ptr(result)
    }

    /// Helper function to convert validation result to FFI error code
    pub fn validation_result_to_ffi_code(result: WasmtimeResult<()>) -> i32 {
        IntegerReturnConverter::to_ffi_code(result)
    }

    /// Helper function to convert size result to FFI result tuple
    pub fn size_result_to_ffi_result(result: WasmtimeResult<usize>) -> (i32, usize) {
        match result {
            Ok(size) => (super::FFI_SUCCESS, size),
            Err(_) => (super::FFI_ERROR, 0),
        }
    }

    /// Helper function to convert count result to FFI result tuple
    pub fn count_result_to_ffi_result(result: WasmtimeResult<usize>) -> (i32, i32) {
        match result {
            Ok(count) => (super::FFI_SUCCESS, count as i32),
            Err(_) => (super::FFI_ERROR, -1),
        }
    }

    /// Helper function to convert boolean result to FFI result tuple
    pub fn bool_result_to_ffi_result(result: WasmtimeResult<bool>) -> (i32, bool) {
        match result {
            Ok(value) => (super::FFI_SUCCESS, value),
            Err(_) => (super::FFI_ERROR, false),
        }
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
        
        let (code, value) = BooleanReturnConverter::to_ffi_result(Err(WasmtimeError::InvalidParameter {
                message:"test".to_string()}));
        assert_eq!(code, FFI_ERROR);
        assert_eq!(value, false);
        
        // Test error code converter
        assert_eq!(BooleanReturnConverter::to_ffi_code(Ok(())), FFI_SUCCESS);
        assert_eq!(BooleanReturnConverter::to_ffi_code(Err(WasmtimeError::InvalidParameter {
                message:"test".to_string()})), FFI_ERROR);
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