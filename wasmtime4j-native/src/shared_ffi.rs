//! # Shared FFI Architecture
//!
//! This module provides shared FFI types, return value converters, validation utilities,
//! and consolidated implementations for WebAssembly operations shared between JNI and
//! Panama interfaces.
//!
//! ## Design Principles
//!
//! - **Defensive programming**: All operations include validation and error handling
//! - **Code sharing**: Consolidated implementations eliminate duplication between JNI/Panama
//! - **Type safety**: FFI enum types provide safe conversion with validation
//!
//! ## Core Components
//!
//! - `ReturnValueConverter<T>`: Consistent return value handling with error codes
//! - `FfiWasmFeature`: FFI-compatible enum representation
//! - `validation`: Pointer and bounds validation utilities

use crate::engine::WasmFeature;
use crate::error::{WasmtimeError, WasmtimeResult};
use std::os::raw::c_void;


/// Standard FFI return codes
pub const FFI_SUCCESS: i32 = 0;
/// Standard FFI error return code
pub const FFI_ERROR: i32 = -1;

/// Trait for handling return value conversions across FFI boundaries
///
/// Provides consistent patterns for different return value types while
/// maintaining proper error propagation and resource management.
pub trait ReturnValueConverter<T> {
    /// Convert result to (error_code, value) tuple for operations returning values
    fn to_ffi_result(result: WasmtimeResult<T>) -> (i32, T)
    where
        T: Default;

    /// Convert result to pointer for operations returning boxed resources
    fn to_ffi_ptr(result: WasmtimeResult<Box<T>>) -> *mut c_void;

    /// Convert result to simple error code for success/failure operations
    fn to_ffi_code(result: WasmtimeResult<()>) -> i32;
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
    ComponentModelThreading = 23,
}

impl FfiWasmFeature {
    /// Convert from FFI representation to native WasmFeature with validation
    pub fn from_ffi(value: i32) -> WasmtimeResult<WasmFeature> {
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
            23 => Ok(WasmFeature::ComponentModelThreading),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid WASM feature: {}. Expected 0-23", value),
            }),
        }
    }

    /// Validate FFI value without conversion (for early parameter checking)
    pub fn validate(value: i32) -> WasmtimeResult<()> {
        match value {
            0..=23 => Ok(()),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid WASM feature: {}. Expected 0-23", value),
            }),
        }
    }

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
            FfiWasmFeature::ComponentModelThreading => WasmFeature::ComponentModelThreading,
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
            WasmFeature::ComponentModelThreading => FfiWasmFeature::ComponentModelThreading,
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
        T: Default,
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
    use crate::error::{WasmtimeError, WasmtimeResult};

    /// Validate that a pointer is not null
    pub fn validate_not_null<T>(ptr: *const T, name: &str) -> WasmtimeResult<()> {
        if ptr.is_null() {
            Err(WasmtimeError::InvalidParameter {
                message: format!("{} cannot be null", name),
            })
        } else {
            Ok(())
        }
    }

    /// Validate array bounds
    pub fn validate_array_bounds(array_len: usize, index: usize, name: &str) -> WasmtimeResult<()> {
        if index >= array_len {
            Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "{} index {} out of bounds (length: {})",
                    name, index, array_len
                ),
            })
        } else {
            Ok(())
        }
    }

    /// Validate that a slice has valid bounds
    pub fn validate_slice_bounds<T>(
        slice: &[T],
        start: usize,
        len: usize,
        name: &str,
    ) -> WasmtimeResult<()> {
        if start >= slice.len() || start.saturating_add(len) > slice.len() {
            Err(WasmtimeError::InvalidParameter {
                message: format!(
                    "{} slice bounds invalid: start={}, len={}, slice_len={}",
                    name,
                    start,
                    len,
                    slice.len()
                ),
            })
        } else {
            Ok(())
        }
    }
}

/// Module operations shared between JNI and Panama FFI implementations
///
/// This module provides consolidated implementations for WebAssembly module operations,
/// eliminating code duplication between interface implementations while maintaining
/// defensive programming practices and consistent error handling.
pub mod module {
    use super::{validation, IntegerReturnConverter, PointerReturnConverter, ReturnValueConverter};
    use crate::engine::Engine;
    use crate::error::{WasmtimeError, WasmtimeResult};
    use crate::module::{core, Module};
    use std::os::raw::c_void;

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
    pub fn compile_module_shared<B>(engine: &Engine, wasm_bytes: B) -> WasmtimeResult<Box<Module>>
    where
        B: ByteArrayConverter,
    {
        // Validate byte array
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WebAssembly bytes cannot be empty".to_string(),
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
        wat_string: S,
    ) -> WasmtimeResult<Box<Module>>
    where
        S: StringConverter,
    {
        // Validate string
        if wat_string.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WAT string cannot be empty".to_string(),
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
                message: "WebAssembly bytes cannot be empty".to_string(),
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
        serialized_bytes: B,
    ) -> WasmtimeResult<Box<Module>>
    where
        B: ByteArrayConverter,
    {
        // Validate byte array
        if serialized_bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Serialized module bytes cannot be empty".to_string(),
            });
        }

        // Get bytes safely and deserialize
        let bytes = unsafe { serialized_bytes.get_bytes()? };
        core::deserialize_module(engine, bytes)
    }

    /// Shared implementation for module deserialization from file
    ///
    /// Deserializes a module from a file path using memory-mapped I/O.
    /// This is more efficient than reading the file first for large modules.
    pub fn deserialize_module_file_shared(
        engine: &Engine,
        path: &str,
    ) -> WasmtimeResult<Box<Module>> {
        // Validate path is not empty
        if path.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "File path cannot be empty".to_string(),
            });
        }

        // Validate file exists
        let path_ref = std::path::Path::new(path);
        if !path_ref.exists() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("File does not exist: {}", path),
            });
        }

        // Call Module::deserialize_file
        Module::deserialize_file(engine, path_ref).map(|m| Box::new(m))
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
    pub fn has_export_shared<S>(module_ptr: *mut c_void, export_name: S) -> WasmtimeResult<bool>
    where
        S: StringConverter,
    {
        validation::validate_not_null(module_ptr, "module")?;

        if export_name.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Export name cannot be empty".to_string(),
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
        Ok(function_exports
            .into_iter()
            .map(|exp| exp.name.clone())
            .collect())
    }

    /// Shared implementation for getting memory export names
    pub fn get_memory_exports_shared(module_ptr: *mut c_void) -> WasmtimeResult<Vec<String>> {
        validation::validate_not_null(module_ptr, "module")?;

        let module = unsafe { core::get_module_ref(module_ptr)? };
        let memory_exports = core::get_memory_exports(module);
        Ok(memory_exports
            .into_iter()
            .map(|exp| exp.name.clone())
            .collect())
    }

    /// Shared implementation for getting required import names
    pub fn get_required_imports_shared(module_ptr: *mut c_void) -> WasmtimeResult<Vec<String>> {
        validation::validate_not_null(module_ptr, "module")?;

        let module = unsafe { core::get_module_ref(module_ptr)? };
        let imports = core::get_required_imports(module);
        Ok(imports
            .iter()
            .map(|imp| format!("{}::{}", imp.module, imp.name))
            .collect())
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
            unsafe {
                core::destroy_module(module_ptr);
            }
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
    fn test_wasm_feature_conversion() {
        // Test valid conversions
        assert_eq!(FfiWasmFeature::from_ffi(0).unwrap(), WasmFeature::Threads);
        assert_eq!(
            FfiWasmFeature::from_ffi(1).unwrap(),
            WasmFeature::ReferenceTypes
        );
        assert_eq!(
            FfiWasmFeature::from_ffi(4).unwrap(),
            WasmFeature::MultiValue
        );

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

        let (code, value) =
            BooleanReturnConverter::to_ffi_result(Err(WasmtimeError::InvalidParameter {
                message: "test".to_string(),
            }));
        assert_eq!(code, FFI_ERROR);
        assert_eq!(value, false);

        // Test error code converter
        assert_eq!(BooleanReturnConverter::to_ffi_code(Ok(())), FFI_SUCCESS);
        assert_eq!(
            BooleanReturnConverter::to_ffi_code(Err(WasmtimeError::InvalidParameter {
                message: "test".to_string()
            })),
            FFI_ERROR
        );
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

    #[test]
    fn test_wasm_feature_all_values() {
        // Test all valid WasmFeature values (0-23)
        for i in 0..=23 {
            let result = FfiWasmFeature::from_ffi(i);
            assert!(result.is_ok(), "Feature {} should be valid", i);
        }
    }

    #[test]
    fn test_wasm_feature_invalid_values() {
        // Test invalid feature values (valid range is 0-22)
        for invalid in [24, 100, -1, i32::MAX, i32::MIN] {
            assert!(
                FfiWasmFeature::from_ffi(invalid).is_err(),
                "Feature {} should be invalid",
                invalid
            );
        }
    }

    #[test]
    fn test_convert_wasm_features_empty() {
        let features: Vec<i32> = vec![];
        let result = convert_wasm_features(&features);
        assert!(result.is_ok(), "Empty features should succeed");
        assert_eq!(result.unwrap().len(), 0, "Should return empty vec");
    }

    #[test]
    fn test_convert_wasm_features_duplicates() {
        // Converting duplicates should work (validation doesn't filter)
        let features = vec![0, 0, 0, 1, 1];
        let result = convert_wasm_features(&features);
        assert!(result.is_ok(), "Duplicate features should succeed");
        assert_eq!(
            result.unwrap().len(),
            5,
            "Should convert all including duplicates"
        );
    }

    #[test]
    fn test_convert_wasm_features_all_valid() {
        let features: Vec<i32> = (0..=11).collect();
        let result = convert_wasm_features(&features);
        assert!(result.is_ok(), "All valid features should succeed");
        assert_eq!(result.unwrap().len(), 12, "Should convert all 12 features");
    }

    #[test]
    fn test_validate_wasm_features_empty() {
        let features: &[i32] = &[];
        assert!(
            validate_wasm_features(features).is_ok(),
            "Empty features should validate"
        );
    }

    #[test]
    fn test_null_pointer_validation_with_message() {
        use validation::*;

        let null_ptr = std::ptr::null::<i32>();
        let result = validate_not_null(null_ptr, "my_important_pointer");
        assert!(result.is_err());

        let err_msg = result.unwrap_err().to_string();
        assert!(
            err_msg.contains("my_important_pointer"),
            "Error message should contain parameter name"
        );
    }

    #[test]
    fn test_array_bounds_edge_cases() {
        use validation::*;

        // Note: validate_array_bounds checks: index >= array_len

        // Exact boundary - index at array_len fails (10 >= 10 is true)
        assert!(
            validate_array_bounds(10, 10, "arr").is_err(),
            "Exact boundary should fail"
        );

        // Valid last element access (9 >= 10 is false)
        assert!(
            validate_array_bounds(10, 9, "arr").is_ok(),
            "Last element should succeed"
        );

        // Off by one
        assert!(
            validate_array_bounds(10, 11, "arr").is_err(),
            "Off by one should fail"
        );

        // Zero length array - any index fails (0 >= 0 is true)
        assert!(
            validate_array_bounds(0, 0, "arr").is_err(),
            "Zero length array fails any index"
        );

        // Zero index with items (0 >= 5 is false)
        assert!(
            validate_array_bounds(5, 0, "arr").is_ok(),
            "Zero index should succeed"
        );
    }

    #[test]
    fn test_slice_bounds_empty_slice() {
        use validation::*;

        let empty_slice: &[i32] = &[];

        // Note: validate_slice_bounds checks: start >= slice.len() || start + len > slice.len()
        // For empty slice (len=0): start=0 >= 0 is true, so it returns error
        assert!(
            validate_slice_bounds(empty_slice, 0, 0, "empty").is_err(),
            "Empty slice fails bounds check"
        );

        // Any non-zero range should also fail
        assert!(validate_slice_bounds(empty_slice, 0, 1, "empty").is_err());
    }

    #[test]
    fn test_slice_bounds_single_element() {
        use validation::*;

        let single = &[42];

        assert!(
            validate_slice_bounds(single, 0, 1, "single").is_ok(),
            "Full range should succeed"
        );
        assert!(
            validate_slice_bounds(single, 0, 0, "single").is_ok(),
            "Empty range should succeed"
        );
        // start=1 >= len=1 is true, so this fails
        assert!(
            validate_slice_bounds(single, 1, 0, "single").is_err(),
            "End position fails bounds check"
        );
        assert!(
            validate_slice_bounds(single, 0, 2, "single").is_err(),
            "Beyond end should fail"
        );
    }

    #[test]
    fn test_boolean_return_converter_false_result() {
        let (code, value) = BooleanReturnConverter::to_ffi_result(Ok(false));
        assert_eq!(
            code, FFI_SUCCESS,
            "Success should return success code even for false"
        );
        assert_eq!(value, false, "Should preserve false value");
    }

    #[test]
    fn test_pointer_return_converter_null() {
        let result: WasmtimeResult<Box<bool>> = Err(WasmtimeError::InvalidParameter {
            message: "test".to_string(),
        });
        let ptr = BooleanReturnConverter::to_ffi_ptr(result);
        assert!(ptr.is_null(), "Error should return null pointer");
    }

    #[test]
    fn test_ffi_constants() {
        assert_eq!(FFI_SUCCESS, 0, "FFI_SUCCESS should be 0");
        assert_eq!(FFI_ERROR, -1, "FFI_ERROR should be -1");
        assert_ne!(
            FFI_SUCCESS, FFI_ERROR,
            "Success and error codes should differ"
        );
    }

    // =========================================================================
    // FfiWasmFeature Native Conversion Tests (8 tests)
    // =========================================================================

    #[test]
    fn test_ffi_wasm_feature_to_native_basic() {
        assert_eq!(FfiWasmFeature::Threads.to_native(), WasmFeature::Threads);
        assert_eq!(FfiWasmFeature::Simd.to_native(), WasmFeature::Simd);
        assert_eq!(
            FfiWasmFeature::BulkMemory.to_native(),
            WasmFeature::BulkMemory
        );
        assert_eq!(
            FfiWasmFeature::MultiValue.to_native(),
            WasmFeature::MultiValue
        );
    }

    #[test]
    fn test_ffi_wasm_feature_to_native_advanced() {
        assert_eq!(FfiWasmFeature::Gc.to_native(), WasmFeature::Gc);
        assert_eq!(
            FfiWasmFeature::Exceptions.to_native(),
            WasmFeature::Exceptions
        );
        assert_eq!(FfiWasmFeature::TailCall.to_native(), WasmFeature::TailCall);
        assert_eq!(FfiWasmFeature::Memory64.to_native(), WasmFeature::Memory64);
    }

    #[test]
    fn test_ffi_wasm_feature_from_native_basic() {
        assert_eq!(
            FfiWasmFeature::from_native(WasmFeature::Threads),
            FfiWasmFeature::Threads
        );
        assert_eq!(
            FfiWasmFeature::from_native(WasmFeature::Simd),
            FfiWasmFeature::Simd
        );
        assert_eq!(
            FfiWasmFeature::from_native(WasmFeature::ReferenceTypes),
            FfiWasmFeature::ReferenceTypes
        );
    }

    #[test]
    fn test_ffi_wasm_feature_from_native_component_model() {
        assert_eq!(
            FfiWasmFeature::from_native(WasmFeature::ComponentModel),
            FfiWasmFeature::ComponentModel
        );
        assert_eq!(
            FfiWasmFeature::from_native(WasmFeature::ComponentModelAsync),
            FfiWasmFeature::ComponentModelAsync
        );
        assert_eq!(
            FfiWasmFeature::from_native(WasmFeature::ComponentModelGc),
            FfiWasmFeature::ComponentModelGc
        );
    }

    #[test]
    fn test_ffi_wasm_feature_roundtrip_all() {
        let features = [
            WasmFeature::Threads,
            WasmFeature::ReferenceTypes,
            WasmFeature::Simd,
            WasmFeature::BulkMemory,
            WasmFeature::MultiValue,
            WasmFeature::MultiMemory,
            WasmFeature::TailCall,
            WasmFeature::RelaxedSimd,
            WasmFeature::FunctionReferences,
            WasmFeature::Gc,
            WasmFeature::Exceptions,
            WasmFeature::Memory64,
            WasmFeature::ExtendedConst,
            WasmFeature::ComponentModel,
            WasmFeature::CustomPageSizes,
            WasmFeature::WideArithmetic,
            WasmFeature::StackSwitching,
            WasmFeature::SharedEverythingThreads,
            WasmFeature::ComponentModelAsync,
            WasmFeature::ComponentModelAsyncBuiltins,
            WasmFeature::ComponentModelAsyncStackful,
            WasmFeature::ComponentModelErrorContext,
            WasmFeature::ComponentModelGc,
            WasmFeature::ComponentModelThreading,
        ];

        for feature in features {
            let ffi = FfiWasmFeature::from_native(feature);
            let native = ffi.to_native();
            assert_eq!(native, feature, "Roundtrip failed for {:?}", feature);
        }
    }

    #[test]
    fn test_ffi_wasm_feature_repr_values_complete() {
        assert_eq!(FfiWasmFeature::Threads as i32, 0);
        assert_eq!(FfiWasmFeature::ComponentModelGc as i32, 22);
        assert_eq!(FfiWasmFeature::ComponentModelThreading as i32, 23);
    }

    #[test]
    fn test_ffi_wasm_feature_clone_and_copy() {
        let feature = FfiWasmFeature::Gc;
        let cloned = feature.clone();
        let copied = feature;
        assert_eq!(feature, cloned);
        assert_eq!(feature, copied);
    }

    #[test]
    fn test_ffi_wasm_feature_debug_format() {
        let feature = FfiWasmFeature::Simd;
        let debug_str = format!("{:?}", feature);
        assert!(debug_str.contains("Simd"));
    }

    // =========================================================================
    // Integer Return Converter Tests (5 tests)
    // =========================================================================

    #[test]
    fn test_integer_return_converter_success() {
        let result: WasmtimeResult<i32> = Ok(42);
        let (code, value) = IntegerReturnConverter::to_ffi_result(result);
        assert_eq!(code, FFI_SUCCESS);
        assert_eq!(value, 42);
    }

    #[test]
    fn test_integer_return_converter_error() {
        let result: WasmtimeResult<i32> = Err(WasmtimeError::InvalidParameter {
            message: "test".to_string(),
        });
        let (code, value) = IntegerReturnConverter::to_ffi_result(result);
        assert_eq!(code, FFI_ERROR);
        assert_eq!(value, -1);
    }

    #[test]
    fn test_integer_return_converter_ffi_code_success() {
        let result: WasmtimeResult<()> = Ok(());
        let code = IntegerReturnConverter::to_ffi_code(result);
        assert_eq!(code, FFI_SUCCESS);
    }

    #[test]
    fn test_integer_return_converter_ffi_code_error() {
        let result: WasmtimeResult<()> = Err(WasmtimeError::Internal {
            message: "test".to_string(),
        });
        let code = IntegerReturnConverter::to_ffi_code(result);
        assert_eq!(code, FFI_ERROR);
    }

    #[test]
    fn test_integer_return_converter_zero_value() {
        let result: WasmtimeResult<i32> = Ok(0);
        let (code, value) = IntegerReturnConverter::to_ffi_result(result);
        assert_eq!(code, FFI_SUCCESS);
        assert_eq!(value, 0);
    }

    // =========================================================================
    // Pointer Return Converter Tests (5 tests)
    // =========================================================================

    #[test]
    fn test_pointer_return_converter_success_ptr() {
        let boxed = Box::new(42i32);
        let result: WasmtimeResult<Box<i32>> = Ok(boxed);
        let ptr = PointerReturnConverter::to_ffi_ptr(result);
        assert!(!ptr.is_null());

        // Clean up
        unsafe {
            let _ = Box::from_raw(ptr as *mut i32);
        }
    }

    #[test]
    fn test_pointer_return_converter_error_ptr() {
        let result: WasmtimeResult<Box<i32>> = Err(WasmtimeError::InvalidParameter {
            message: "test".to_string(),
        });
        let ptr = PointerReturnConverter::to_ffi_ptr(result);
        assert!(ptr.is_null());
    }

    #[test]
    fn test_pointer_return_converter_ffi_result_success() {
        let result: WasmtimeResult<i32> = Ok(100);
        let (code, value) = PointerReturnConverter::to_ffi_result(result);
        assert_eq!(code, FFI_SUCCESS);
        assert_eq!(value, 100);
    }

    #[test]
    fn test_pointer_return_converter_ffi_result_error() {
        let result: WasmtimeResult<i32> = Err(WasmtimeError::Internal {
            message: "test".to_string(),
        });
        let (code, value) = PointerReturnConverter::to_ffi_result(result);
        assert_eq!(code, FFI_ERROR);
        assert_eq!(value, 0); // Default value
    }

    #[test]
    fn test_pointer_return_converter_ffi_code() {
        let success: WasmtimeResult<()> = Ok(());
        let error: WasmtimeResult<()> = Err(WasmtimeError::Internal {
            message: "test".to_string(),
        });

        assert_eq!(
            <PointerReturnConverter as ReturnValueConverter<i32>>::to_ffi_code(success),
            FFI_SUCCESS
        );
        assert_eq!(
            <PointerReturnConverter as ReturnValueConverter<i32>>::to_ffi_code(error),
            FFI_ERROR
        );
    }

    // =========================================================================
    // Validation Module Extended Tests (10 tests)
    // =========================================================================

    #[test]
    fn test_validate_not_null_with_different_types() {
        use validation::*;

        let int_ptr: *const i32 = &42;
        let float_ptr: *const f64 = &3.14;
        let byte_ptr: *const u8 = &0u8;

        assert!(validate_not_null(int_ptr, "int").is_ok());
        assert!(validate_not_null(byte_ptr, "byte").is_ok());
        assert!(validate_not_null(float_ptr, "float").is_ok());
    }

    #[test]
    fn test_validate_not_null_different_error_messages() {
        use validation::*;

        let null_ptr: *const i32 = std::ptr::null();

        let err1 = validate_not_null(null_ptr, "engine_ptr").unwrap_err();
        let err2 = validate_not_null(null_ptr, "store_handle").unwrap_err();

        assert!(err1.to_string().contains("engine_ptr"));
        assert!(err2.to_string().contains("store_handle"));
    }

    #[test]
    fn test_validate_array_bounds_multiple_accesses() {
        use validation::*;

        let array_len = 100;

        // First element
        assert!(validate_array_bounds(array_len, 0, "arr").is_ok());
        // Middle element
        assert!(validate_array_bounds(array_len, 50, "arr").is_ok());
        // Last element
        assert!(validate_array_bounds(array_len, 99, "arr").is_ok());
        // One past end
        assert!(validate_array_bounds(array_len, 100, "arr").is_err());
    }

    #[test]
    fn test_validate_array_bounds_large_array() {
        use validation::*;

        let large_len = usize::MAX / 2;
        assert!(validate_array_bounds(large_len, 0, "large").is_ok());
        assert!(validate_array_bounds(large_len, large_len - 1, "large").is_ok());
        assert!(validate_array_bounds(large_len, large_len, "large").is_err());
    }

    #[test]
    fn test_validate_slice_bounds_normal_ranges() {
        use validation::*;

        let slice = &[1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

        // Valid ranges
        assert!(validate_slice_bounds(slice, 0, 5, "s").is_ok());
        assert!(validate_slice_bounds(slice, 5, 5, "s").is_ok());
        assert!(validate_slice_bounds(slice, 0, 10, "s").is_ok());
        assert!(validate_slice_bounds(slice, 9, 1, "s").is_ok());
    }

    #[test]
    fn test_validate_slice_bounds_invalid_ranges() {
        use validation::*;

        let slice = &[1, 2, 3, 4, 5];

        // Start beyond end
        assert!(validate_slice_bounds(slice, 5, 0, "s").is_err());
        // Length beyond slice
        assert!(validate_slice_bounds(slice, 3, 5, "s").is_err());
        // Start + len overflow
        assert!(validate_slice_bounds(slice, 4, 10, "s").is_err());
    }

    #[test]
    fn test_validate_slice_bounds_zero_length_read() {
        use validation::*;

        let slice = &[1, 2, 3, 4, 5];

        // Zero-length reads should succeed within bounds
        assert!(validate_slice_bounds(slice, 0, 0, "s").is_ok());
        assert!(validate_slice_bounds(slice, 3, 0, "s").is_ok());
        assert!(validate_slice_bounds(slice, 4, 0, "s").is_ok());
    }

    #[test]
    fn test_validate_slice_bounds_error_message_content() {
        use validation::*;

        let slice = &[1, 2, 3];
        let result = validate_slice_bounds(slice, 2, 5, "my_data");

        let err_msg = result.unwrap_err().to_string();
        assert!(err_msg.contains("my_data"));
        assert!(err_msg.contains("start=2"));
        assert!(err_msg.contains("len=5"));
        assert!(err_msg.contains("slice_len=3"));
    }

}
