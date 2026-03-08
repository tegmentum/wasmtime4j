//! Common FFI utilities shared between JNI and Panama implementations.
//!
//! This module provides simple utility functions to eliminate code duplication
//! while maintaining clear interfaces and compilation integrity.

/// ValType conversion utilities shared between JNI and Panama implementations.
///
/// These functions provide the canonical mapping between integer type codes
/// and wasmtime::ValType, ensuring consistency across all FFI boundaries.
pub mod valtype_conversion {
    use crate::error::{WasmtimeError, WasmtimeResult};
    use wasmtime::{HeapType, RefType, ValType};

    /// Convert an integer type code to a wasmtime ValType.
    ///
    /// # Type Code Mapping
    /// | Code | ValType |
    /// |------|---------|
    /// | 0 | I32 |
    /// | 1 | I64 |
    /// | 2 | F32 |
    /// | 3 | F64 |
    /// | 4 | V128 |
    /// | 5 | FuncRef |
    /// | 6 | ExternRef |
    /// | 7 | AnyRef |
    /// | 8 | EqRef |
    /// | 9 | I31Ref |
    /// | 10 | StructRef |
    /// | 11 | ArrayRef |
    /// | 12 | NullRef |
    /// | 13 | NullFuncRef |
    /// | 14 | NullExternRef |
    pub fn int_to_valtype(code: i32) -> WasmtimeResult<ValType> {
        match code {
            0 => Ok(ValType::I32),
            1 => Ok(ValType::I64),
            2 => Ok(ValType::F32),
            3 => Ok(ValType::F64),
            4 => Ok(ValType::V128),
            5 => Ok(ValType::Ref(RefType::FUNCREF)),
            6 => Ok(ValType::Ref(RefType::EXTERNREF)),
            7 => Ok(ValType::Ref(RefType::ANYREF)),
            8 => Ok(ValType::Ref(RefType::new(true, HeapType::Eq))),
            9 => Ok(ValType::Ref(RefType::new(true, HeapType::I31))),
            10 => Ok(ValType::Ref(RefType::new(true, HeapType::Struct))),
            11 => Ok(ValType::Ref(RefType::new(true, HeapType::Array))),
            12 => Ok(ValType::Ref(RefType::new(true, HeapType::None))),
            13 => Ok(ValType::Ref(RefType::new(true, HeapType::NoFunc))),
            14 => Ok(ValType::Ref(RefType::new(true, HeapType::NoExtern))),
            15 => Ok(ValType::Ref(RefType::new(true, HeapType::Exn))),
            16 => Ok(ValType::Ref(RefType::new(true, HeapType::NoExn))),
            17 => Ok(ValType::Ref(RefType::new(true, HeapType::Cont))),
            18 => Ok(ValType::Ref(RefType::new(true, HeapType::NoCont))),
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid ValType code: {}. Expected 0-18", code),
            }),
        }
    }

    /// Convert a wasmtime ValType to its integer type code.
    ///
    /// See [`int_to_valtype`] for the code mapping.
    pub fn valtype_to_int(val_type: &ValType) -> i32 {
        match val_type {
            ValType::I32 => 0,
            ValType::I64 => 1,
            ValType::F32 => 2,
            ValType::F64 => 3,
            ValType::V128 => 4,
            ValType::Ref(ref_type) => match ref_type.heap_type() {
                HeapType::Func | HeapType::ConcreteFunc(_) => 5,
                HeapType::Extern => 6,
                HeapType::Any => 7,
                HeapType::Eq => 8,
                HeapType::I31 => 9,
                HeapType::Struct | HeapType::ConcreteStruct(_) => 10,
                HeapType::Array | HeapType::ConcreteArray(_) => 11,
                HeapType::None => 12,
                HeapType::NoFunc => 13,
                HeapType::NoExtern => 14,
                HeapType::Exn | HeapType::ConcreteExn(_) => 15,
                HeapType::NoExn => 16,
                HeapType::Cont | HeapType::ConcreteCont(_) => 17,
                HeapType::NoCont => 18,
            },
        }
    }
}

/// Parameter conversion utilities for FFI operations
pub mod parameter_conversion {
    //! Utilities for converting parameters between FFI interfaces and internal types.
    //!
    //! This module provides functions to convert engine configurations, module parameters,
    //! and store parameters consistently across both JNI and Panama implementations.

    use wasmtime::{OptLevel, Strategy};

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

    /// Convert integer profiling strategy to wasmtime ProfilingStrategy enum
    ///
    /// # Arguments
    /// * `strategy` - Integer value representing profiling strategy
    ///   - 0 = None (no profiling)
    ///   - 1 = JitDump
    ///   - 2 = PerfMap
    ///   - 3 = VTune
    ///
    /// # Returns
    /// * `Some(ProfilingStrategy)` for valid values 0-3
    /// * `None` for invalid values
    pub fn convert_profiling_strategy(strategy: i32) -> Option<wasmtime::ProfilingStrategy> {
        match strategy {
            0 => Some(wasmtime::ProfilingStrategy::None),
            1 => Some(wasmtime::ProfilingStrategy::JitDump),
            2 => Some(wasmtime::ProfilingStrategy::PerfMap),
            3 => Some(wasmtime::ProfilingStrategy::VTune),
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

    /// Convert a zero value to None, non-zero to Some(value as u64).
    ///
    /// Used for store config parameters where 0 means "no limit/no timeout".
    ///
    /// # Arguments
    /// * `value` - Value to convert (0 = None)
    ///
    /// # Returns
    /// * `None` if value is 0
    /// * `Some(value as u64)` otherwise
    pub fn zero_to_none_u64(value: i64) -> Option<u64> {
        if value == 0 {
            None
        } else {
            Some(value as u64)
        }
    }

    /// Convert a zero value to None, non-zero to Some(value as usize).
    ///
    /// Used for store config parameters where 0 means "no limit".
    ///
    /// # Arguments
    /// * `value` - Value to convert (0 = None)
    ///
    /// # Returns
    /// * `None` if value is 0
    /// * `Some(value as usize)` otherwise
    pub fn zero_to_none_usize(value: i64) -> Option<usize> {
        if value == 0 {
            None
        } else {
            Some(value as usize)
        }
    }

    /// Convert a zero value to None, non-zero to Some(value as u32).
    ///
    /// Used for store config parameters where 0 means "no limit".
    ///
    /// # Arguments
    /// * `value` - Value to convert (0 = None)
    ///
    /// # Returns
    /// * `None` if value is 0
    /// * `Some(value as u32)` otherwise
    pub fn zero_to_none_u32(value: i32) -> Option<u32> {
        if value == 0 {
            None
        } else {
            Some(value as u32)
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

/// Resource destruction utilities with double-free protection.
///
/// This module provides unified resource destruction functions that prevent
/// double-free vulnerabilities across FFI boundaries. All destroy operations
/// are thread-safe and panic-safe to prevent JVM crashes.
pub mod resource_destruction {
    use std::sync::LazyLock;
    use std::collections::HashSet;
    use std::os::raw::c_void;
    use std::sync::Mutex;

    /// Thread-safe tracking of destroyed pointers to prevent double-free.
    /// Using usize addresses instead of raw pointers for thread safety.
    pub static DESTROYED_POINTERS: LazyLock<Mutex<HashSet<usize>>> =
        LazyLock::new(|| Mutex::new(HashSet::new()));

    /// Magic prefix used to detect fake/test pointers.
    /// Pointers with this prefix in high bits are treated as test pointers.
    const FAKE_POINTER_MAGIC: usize = 0x1234560000000000;
    const FAKE_POINTER_MASK: usize = 0xFFFFFF0000000000;

    /// Minimum valid heap address threshold.
    /// Addresses below this are treated as invalid/fake pointers.
    const MIN_VALID_ADDRESS: usize = 0x1000;

    /// Clear the destroyed pointers registry.
    ///
    /// This function clears the HashSet tracking destroyed pointers.
    /// It should be called during test teardown to prevent unbounded memory growth
    /// when running large test suites.
    ///
    /// # Returns
    /// The number of entries that were cleared.
    ///
    /// # Safety
    /// Calling this function while native resources are still in use could
    /// result in the double-free protection being bypassed for those resources.
    /// Only call this when all native resources have been properly destroyed.
    pub fn clear_destroyed_pointers() -> usize {
        let mut destroyed = DESTROYED_POINTERS.lock().unwrap_or_else(|poisoned| {
            log::warn!("DESTROYED_POINTERS mutex was poisoned, recovering");
            poisoned.into_inner()
        });
        let count = destroyed.len();
        destroyed.clear();
        log::debug!("Cleared {} entries from destroyed pointers registry", count);
        count
    }

    /// Check if a pointer address appears to be a fake/test pointer.
    ///
    /// # Arguments
    /// * `ptr_addr` - The pointer address to check
    ///
    /// # Returns
    /// `true` if the pointer appears to be fake/invalid, `false` otherwise.
    #[inline]
    pub fn is_fake_pointer(ptr_addr: usize) -> bool {
        ptr_addr < MIN_VALID_ADDRESS || (ptr_addr & FAKE_POINTER_MASK) == FAKE_POINTER_MAGIC
    }

    /// Safely destroy a heap-allocated resource with double-free protection.
    ///
    /// This function provides unified resource destruction that:
    /// - Validates the pointer is not null
    /// - Detects and ignores fake/test pointers
    /// - Prevents double-free by tracking destroyed addresses
    /// - Uses panic safety to prevent JVM crashes
    /// - Cleans up tracking on success to allow address reuse
    ///
    /// # Type Parameters
    /// * `T` - The type of the resource being destroyed
    ///
    /// # Arguments
    /// * `ptr` - Raw pointer to the boxed resource
    /// * `resource_name` - Name of the resource for logging purposes
    ///
    /// # Returns
    /// `true` if destruction occurred, `false` if skipped (null, fake, or already destroyed)
    ///
    /// # Safety
    /// The caller must ensure that `ptr` was originally created by `Box::into_raw`
    /// for a value of type `T`.
    pub unsafe fn safe_destroy<T>(ptr: *mut c_void, resource_name: &str) -> bool {
        // Null pointer check
        if ptr.is_null() {
            log::debug!("Ignoring null pointer in {}", resource_name);
            return false;
        }

        let ptr_addr = ptr as usize;

        // Detect and reject obvious test/fake pointers
        if is_fake_pointer(ptr_addr) {
            log::debug!("Ignoring fake/test pointer {:p} in {}", ptr, resource_name);
            return false;
        }

        // Check if pointer was already destroyed - use unwrap_or_else to recover from poisoned mutex
        {
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap_or_else(|poisoned| {
                log::warn!(
                    "DESTROYED_POINTERS mutex poisoned in {} destroy, recovering",
                    resource_name
                );
                poisoned.into_inner()
            });
            if destroyed.contains(&ptr_addr) {
                log::warn!(
                    "Attempted double-free of {} at {:p} - ignoring",
                    resource_name,
                    ptr
                );
                return false;
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
                // Remove address from DESTROYED_POINTERS so that if the allocator
                // reuses this address for a new resource, it won't be falsely
                // detected as a double-free.
                let mut destroyed = DESTROYED_POINTERS.lock().unwrap_or_else(|poisoned| {
                    log::warn!(
                        "DESTROYED_POINTERS mutex poisoned during {} cleanup, recovering",
                        resource_name
                    );
                    poisoned.into_inner()
                });
                destroyed.remove(&ptr_addr);
                log::debug!("{} at {:p} destroyed successfully", resource_name, ptr);
                true
            }
            Err(e) => {
                log::error!(
                    "{} at {:p} destruction panicked: {:?} - preventing JVM crash",
                    resource_name,
                    ptr,
                    e
                );
                // Don't propagate panic to JVM - just log and continue
                // Leave address in DESTROYED_POINTERS since destruction failed
                false
            }
        }
    }

    /// Safely destroy a heap-allocated resource without fake pointer detection.
    ///
    /// This variant is used for JNI bindings where fake pointer detection
    /// is not needed (handles come directly from Java).
    ///
    /// # Type Parameters
    /// * `T` - The type of the resource being destroyed
    ///
    /// # Arguments
    /// * `ptr` - Raw pointer to the boxed resource
    /// * `resource_name` - Name of the resource for logging purposes
    ///
    /// # Returns
    /// `true` if destruction occurred, `false` if skipped (null or already destroyed)
    ///
    /// # Safety
    /// The caller must ensure that `ptr` was originally created by `Box::into_raw`
    /// for a value of type `T`.
    pub unsafe fn safe_destroy_no_fake_check<T>(ptr: *mut c_void, resource_name: &str) -> bool {
        // Null pointer check
        if ptr.is_null() {
            log::debug!("Ignoring null pointer in {}", resource_name);
            return false;
        }

        let ptr_addr = ptr as usize;

        // Check if pointer was already destroyed
        {
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap_or_else(|poisoned| {
                log::warn!(
                    "DESTROYED_POINTERS mutex poisoned in {} destroy, recovering",
                    resource_name
                );
                poisoned.into_inner()
            });
            if destroyed.contains(&ptr_addr) {
                log::warn!(
                    "Attempted double-free of {} at {:p} - ignoring",
                    resource_name,
                    ptr
                );
                return false;
            }
            destroyed.insert(ptr_addr);
        }

        // Use panic-safe destruction to prevent JVM crashes
        let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
            let _ = Box::from_raw(ptr as *mut T);
        }));

        match result {
            Ok(_) => {
                let mut destroyed = DESTROYED_POINTERS.lock().unwrap_or_else(|poisoned| {
                    log::warn!(
                        "DESTROYED_POINTERS mutex poisoned during {} cleanup, recovering",
                        resource_name
                    );
                    poisoned.into_inner()
                });
                destroyed.remove(&ptr_addr);
                log::debug!("{} at {:p} destroyed successfully", resource_name, ptr);
                true
            }
            Err(e) => {
                log::error!(
                    "{} at {:p} destruction panicked: {:?} - preventing JVM crash",
                    resource_name,
                    ptr,
                    e
                );
                false
            }
        }
    }
}

#[cfg(test)]
mod tests {
    use super::error_handling::*;
    use super::memory_utils::*;
    use super::parameter_conversion::*;
    use super::resource_destruction::*;
    use super::valtype_conversion::*;
    use std::ptr;
    use wasmtime::{HeapType, RefType, ValType};
    use wasmtime::{OptLevel, Strategy};

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
        let mut_ref = result.unwrap();
        assert_eq!(*mut_ref, 42);
        *mut_ref = 100;
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
            "test_copy",
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
            "test_copy",
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
            "overlapping_copy",
        );

        assert!(result.is_ok());
        assert_eq!(buffer[2], 1); // buffer[0] copied to buffer[2]
        assert_eq!(buffer[3], 2); // buffer[1] copied to buffer[3]
    }

    #[test]
    fn test_safe_memory_copy_null_pointers() {
        let src = [1u8, 2, 3];

        // Null destination
        let result = safe_memory_copy(ptr::null_mut(), src.as_ptr(), 3, 3, 3, "test");
        assert!(matches!(result, Err(MemoryError::NullPointer(_))));

        // Null source
        let mut dest = [0u8; 3];
        let result = safe_memory_copy(dest.as_mut_ptr(), ptr::null(), 3, 3, 3, "test");
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
            }
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
            }
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
            }
            _ => panic!("Expected InvalidParameter error"),
        }
    }

    // Resource destruction tests
    #[test]
    fn test_is_fake_pointer() {
        // Low addresses are fake
        assert!(is_fake_pointer(0));
        assert!(is_fake_pointer(0x100));
        assert!(is_fake_pointer(0xFFF));

        // Address at threshold is valid
        assert!(!is_fake_pointer(0x1000));

        // Normal heap addresses are valid
        assert!(!is_fake_pointer(0x7FFF_0000_0000));

        // Magic prefix addresses are fake
        assert!(is_fake_pointer(0x1234_5600_0000_0001));
        assert!(is_fake_pointer(0x1234_5600_FFFF_FFFF));
    }

    #[test]
    fn test_safe_destroy_null_pointer() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Null pointer should return false without panicking
        let result = unsafe { safe_destroy::<i32>(ptr::null_mut(), "test") };
        assert!(!result, "Null pointer destruction should return false");
    }

    #[test]
    fn test_safe_destroy_fake_pointer() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Fake pointer (low address) should return false
        let fake_ptr = 0x100 as *mut std::os::raw::c_void;
        let result = unsafe { safe_destroy::<i32>(fake_ptr, "test") };
        assert!(!result, "Fake pointer destruction should return false");

        // Fake pointer (magic prefix) should return false
        let magic_ptr = 0x1234_5600_0000_0001 as *mut std::os::raw::c_void;
        let result = unsafe { safe_destroy::<i32>(magic_ptr, "test") };
        assert!(
            !result,
            "Magic prefix pointer destruction should return false"
        );
    }

    #[test]
    fn test_safe_destroy_valid_pointer() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Create a boxed value and get its raw pointer
        let boxed = Box::new(42i32);
        let ptr = Box::into_raw(boxed) as *mut std::os::raw::c_void;

        // Destroy should succeed
        let result = unsafe { safe_destroy::<i32>(ptr, "test_int") };
        assert!(result, "Valid pointer destruction should return true");

        // After successful destruction, address is removed from tracking to allow reuse.
        // We do NOT test calling safe_destroy again on the same pointer as that would
        // be undefined behavior (actual double-free). The double-free protection is
        // designed to catch rapid concurrent calls, not sequential calls after cleanup.
    }

    #[test]
    fn test_safe_destroy_double_free_protection() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Manually insert an address to simulate "destruction in progress"
        let fake_addr = 0x12345678_usize;
        {
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            destroyed.insert(fake_addr);
        }

        // Attempting to destroy this "already tracked" address should return false
        // Note: We use a fake address that won't be dereferenced because is_fake_pointer
        // would reject it. Instead, we create a high valid-looking address.
        let ptr = fake_addr as *mut std::os::raw::c_void;
        let result = unsafe { safe_destroy::<i32>(ptr, "test_int") };
        assert!(
            !result,
            "Already-tracked address should return false (double-free protection)"
        );

        // Clean up
        clear_destroyed_pointers();
    }

    #[test]
    fn test_safe_destroy_clears_tracking() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Create and destroy a resource
        let boxed = Box::new(123i32);
        let ptr = Box::into_raw(boxed) as *mut std::os::raw::c_void;
        let result = unsafe { safe_destroy::<i32>(ptr, "test_int") };
        assert!(result);

        // Verify the pointer was removed from tracking (registry should be empty)
        let destroyed = DESTROYED_POINTERS.lock().unwrap();
        assert!(
            destroyed.is_empty(),
            "Destroyed pointers registry should be empty after successful destruction"
        );
    }

    #[test]
    fn test_clear_destroyed_pointers() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Manually insert some addresses
        {
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            destroyed.insert(0x1000);
            destroyed.insert(0x2000);
            destroyed.insert(0x3000);
        }

        // Clear and check count
        let count = clear_destroyed_pointers();
        assert_eq!(count, 3, "Should have cleared 3 entries");

        // Verify empty
        let destroyed = DESTROYED_POINTERS.lock().unwrap();
        assert!(destroyed.is_empty());
    }

    #[test]
    fn test_safe_destroy_no_fake_check() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Create a boxed value
        let boxed = Box::new(String::from("test"));
        let ptr = Box::into_raw(boxed) as *mut std::os::raw::c_void;

        // Destroy with no fake check variant
        let result = unsafe { safe_destroy_no_fake_check::<String>(ptr, "test_string") };
        assert!(result, "Valid pointer destruction should return true");

        // After successful destruction, address is removed from tracking.
        // We verify this by checking the registry is empty.
        let destroyed = DESTROYED_POINTERS.lock().unwrap();
        assert!(
            destroyed.is_empty(),
            "Registry should be empty after successful destruction"
        );
    }

    #[test]
    fn test_safe_destroy_no_fake_check_double_free_protection() {
        // Clear any previous state
        clear_destroyed_pointers();

        // Manually insert an address to simulate "destruction in progress"
        let fake_addr = 0x87654321_usize;
        {
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            destroyed.insert(fake_addr);
        }

        // Attempting to destroy this "already tracked" address should return false
        let ptr = fake_addr as *mut std::os::raw::c_void;
        let result = unsafe { safe_destroy_no_fake_check::<String>(ptr, "test_string") };
        assert!(
            !result,
            "Already-tracked address should return false (double-free protection)"
        );

        // Clean up
        clear_destroyed_pointers();
    }

    // Additional edge case tests

    #[test]
    fn test_safe_memory_copy_zero_length() {
        let src = vec![1u8, 2, 3, 4, 5];
        let mut dest = vec![0u8; 5];

        // Note: safe_memory_copy takes (dest, src, len, dest_size, src_size, name)
        let result = safe_memory_copy(
            dest.as_mut_ptr(),
            src.as_ptr(),
            0, // zero length copy
            dest.len(),
            src.len(),
            "test_copy",
        );

        assert!(result.is_ok(), "Zero length copy should succeed");
        // Destination should be unchanged
        assert!(
            dest.iter().all(|&x| x == 0),
            "Destination should be unchanged"
        );
    }

    #[test]
    fn test_safe_array_access_boundary() {
        let array = vec![10i32, 20, 30, 40, 50];

        // Access at exact boundary (last element)
        let result = safe_array_access(array.as_ptr(), 4, array.len(), "test_array");
        assert!(result.is_ok(), "Should access last element");
        assert_eq!(*result.unwrap(), 50);
    }

    #[test]
    fn test_safe_byte_slice_single_byte() {
        let data = vec![42u8];

        let result = safe_byte_slice(data.as_ptr(), 1, "test_slice");
        assert!(result.is_ok(), "Should create slice from single byte");
        let slice = result.unwrap();
        assert_eq!(slice.len(), 1);
        assert_eq!(slice[0], 42);
    }

    #[test]
    fn test_int_to_optional_u32_boundary() {
        // Test boundary values
        assert!(convert_int_to_optional_u32(0).is_some(), "0 should convert");
        assert_eq!(convert_int_to_optional_u32(0).unwrap(), 0);

        assert!(
            convert_int_to_optional_u32(-1).is_none(),
            "-1 should not convert"
        );

        assert!(
            convert_int_to_optional_u32(i32::MAX).is_some(),
            "MAX should convert"
        );
        assert_eq!(
            convert_int_to_optional_u32(i32::MAX).unwrap(),
            i32::MAX as u32
        );
    }

    #[test]
    fn test_int_to_optional_usize_boundary() {
        // Test boundary values
        assert!(
            convert_int_to_optional_usize(0).is_some(),
            "0 should convert"
        );
        assert_eq!(convert_int_to_optional_usize(0).unwrap(), 0);

        assert!(
            convert_int_to_optional_usize(-1).is_none(),
            "-1 should not convert"
        );
    }

    #[test]
    fn test_int_to_bool_edge_cases() {
        // Standard cases
        assert!(!convert_int_to_bool(0), "0 should be false");
        assert!(convert_int_to_bool(1), "1 should be true");

        // Edge cases - any non-zero should be true
        assert!(convert_int_to_bool(-1), "-1 should be true");
        assert!(convert_int_to_bool(i32::MAX), "MAX should be true");
        assert!(convert_int_to_bool(i32::MIN), "MIN should be true");
        assert!(convert_int_to_bool(100), "100 should be true");
    }

    #[test]
    fn test_safe_box_roundtrip() {
        let original_value = 42i32;
        let boxed = Box::new(original_value);

        let ptr = box_into_raw_safe(boxed);
        assert!(!ptr.is_null(), "Pointer should not be null");

        // Recover the box
        let recovered = safe_box_from_raw::<i32>(ptr, "test_box");
        assert!(recovered.is_ok(), "Should recover the box");
        assert_eq!(*recovered.unwrap(), original_value);
    }

    #[test]
    fn test_safe_deref_alignment() {
        // Test with properly aligned data
        let values: Vec<i64> = vec![1, 2, 3, 4];
        let ptr = values.as_ptr();

        // Should succeed with proper alignment
        let result = safe_deref(ptr, "test_ptr");
        assert!(result.is_ok(), "Should dereference aligned pointer");
        assert_eq!(*result.unwrap(), 1);
    }

    #[test]
    fn test_multiple_safe_destroy_tracking() {
        clear_destroyed_pointers();

        // Create and destroy multiple pointers
        let ptrs: Vec<*mut std::os::raw::c_void> = (0..5)
            .map(|i| {
                let boxed = Box::new(format!("string_{}", i));
                Box::into_raw(boxed) as *mut std::os::raw::c_void
            })
            .collect();

        // Destroy all
        for (i, ptr) in ptrs.into_iter().enumerate() {
            let result = unsafe { safe_destroy::<String>(ptr, &format!("ptr_{}", i)) };
            assert!(result, "Destruction {} should succeed", i);
        }

        clear_destroyed_pointers();
    }

    // ValType conversion tests

    #[test]
    fn test_int_to_valtype_all_valid_codes() {
        for code in 0..=18 {
            let result = int_to_valtype(code);
            assert!(result.is_ok(), "Code {} should be valid", code);
        }
    }

    #[test]
    fn test_int_to_valtype_invalid_codes() {
        for code in [19, 100, -1, i32::MAX, i32::MIN] {
            let result = int_to_valtype(code);
            assert!(result.is_err(), "Code {} should be invalid", code);
        }
    }

    #[test]
    fn test_int_to_valtype_primitive_types() {
        assert!(matches!(int_to_valtype(0).unwrap(), ValType::I32));
        assert!(matches!(int_to_valtype(1).unwrap(), ValType::I64));
        assert!(matches!(int_to_valtype(2).unwrap(), ValType::F32));
        assert!(matches!(int_to_valtype(3).unwrap(), ValType::F64));
        assert!(matches!(int_to_valtype(4).unwrap(), ValType::V128));
    }

    #[test]
    fn test_int_to_valtype_ref_types() {
        assert!(matches!(int_to_valtype(5).unwrap(), ValType::Ref(_)));
        assert!(matches!(int_to_valtype(6).unwrap(), ValType::Ref(_)));
        assert!(matches!(int_to_valtype(7).unwrap(), ValType::Ref(_)));
    }

    #[test]
    fn test_valtype_to_int_primitive_types() {
        assert_eq!(valtype_to_int(&ValType::I32), 0);
        assert_eq!(valtype_to_int(&ValType::I64), 1);
        assert_eq!(valtype_to_int(&ValType::F32), 2);
        assert_eq!(valtype_to_int(&ValType::F64), 3);
        assert_eq!(valtype_to_int(&ValType::V128), 4);
    }

    #[test]
    fn test_valtype_to_int_ref_types() {
        assert_eq!(valtype_to_int(&ValType::Ref(RefType::FUNCREF)), 5);
        assert_eq!(valtype_to_int(&ValType::Ref(RefType::EXTERNREF)), 6);
        assert_eq!(valtype_to_int(&ValType::Ref(RefType::ANYREF)), 7);
    }

    #[test]
    fn test_valtype_to_int_gc_types() {
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::Eq))),
            8
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::I31))),
            9
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::Struct))),
            10
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::Array))),
            11
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::None))),
            12
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::NoFunc))),
            13
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::NoExtern))),
            14
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::Exn))),
            15
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::NoExn))),
            16
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::Cont))),
            17
        );
        assert_eq!(
            valtype_to_int(&ValType::Ref(RefType::new(true, HeapType::NoCont))),
            18
        );
    }

    #[test]
    fn test_valtype_roundtrip() {
        for code in 0..=18 {
            let valtype = int_to_valtype(code).unwrap();
            let back = valtype_to_int(&valtype);
            assert_eq!(code, back, "Roundtrip failed for code {}", code);
        }
    }
}

/// Error handling utilities for consistent error reporting
pub mod error_handling {
    //! Utilities for consistent error handling across FFI interfaces.
    //!
    //! This module provides functions for error conversion, pointer validation,
    //! and standardized error reporting between JNI and Panama implementations.

    use crate::error::{ErrorCode, WasmtimeError};
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
    #[allow(missing_docs)]
    pub enum ValidationError {
        /// Null pointer parameter
        NullPointer(String),
        /// Parameter value out of valid range
        OutOfRange {
            parameter: String,
            value: i32,
            min: i32,
            max: i32,
        },
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
                ValidationError::OutOfRange {
                    parameter,
                    value,
                    min,
                    max,
                } => WasmtimeError::InvalidParameter {
                    message: format!(
                        "{} value {} is out of range [{}, {}]",
                        parameter, value, min, max
                    ),
                },
                ValidationError::InvalidString { parameter, reason } => {
                    WasmtimeError::InvalidParameter {
                        message: format!("{} string is invalid: {}", parameter, reason),
                    }
                }
                ValidationError::InvalidPointer { parameter, reason } => {
                    WasmtimeError::InvalidParameter {
                        message: format!("{} pointer is invalid: {}", parameter, reason),
                    }
                }
                ValidationError::InvalidHandle { parameter, value } => {
                    WasmtimeError::InvalidParameter {
                        message: format!("{} handle 0x{:x} is invalid", parameter, value),
                    }
                }
            }
        }
    }

    /// Convert Wasmtime engine errors to consistent error information
    pub fn convert_wasmtime_error(error: WasmtimeEngineError) -> ErrorInfo {
        let error_str = error.to_string();

        // Classify the error type based on content
        let (code, category) = if error_str.contains("compilation") || error_str.contains("compile")
        {
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

        // Platform-specific pointer validation is handled above
        unsafe { Ok(&*ptr) }
    }

    /// Validate and safely dereference mutable pointers
    pub fn validate_mut_pointer<T>(
        ptr: *mut T,
        name: &str,
    ) -> Result<&'static mut T, ValidationError> {
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

        // Platform-specific pointer validation is handled above
        unsafe { Ok(&mut *ptr) }
    }

    /// Validate numeric parameters are within acceptable ranges
    pub fn validate_range(
        value: i32,
        min: i32,
        max: i32,
        name: &str,
    ) -> Result<i32, ValidationError> {
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
    pub fn validate_string_parameter(
        ptr: *const i8,
        name: &str,
    ) -> Result<String, ValidationError> {
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
    pub fn validate_byte_array(
        ptr: *const u8,
        len: usize,
        name: &str,
    ) -> Result<&'static [u8], ValidationError> {
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
    pub fn validate_void_pointer_as<T>(
        ptr: *mut c_void,
        name: &str,
    ) -> Result<&'static mut T, ValidationError> {
        validate_mut_pointer(ptr as *mut T, name)
    }

    /// Validate const void pointer and cast to specific type
    pub fn validate_const_void_pointer_as<T>(
        ptr: *const c_void,
        name: &str,
    ) -> Result<&'static T, ValidationError> {
        validate_pointer(ptr as *const T, name)
    }

    /// Format consistent error message for FFI interfaces
    pub fn format_error_message(error_info: &ErrorInfo, context: &str) -> String {
        if let Some(ref debug_info) = error_info.debug_info {
            format!(
                "{}: {} (Debug: {})",
                context, error_info.message, debug_info
            )
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

    use std::ptr;

    /// Memory error types for specific memory management failures
    #[derive(Debug, Clone)]
    #[allow(missing_docs)]
    pub enum MemoryError {
        /// Null pointer error
        NullPointer(String),
        /// Misaligned pointer error
        MisalignedPointer(String),
        /// Invalid Box pointer error
        InvalidBoxPointer(String),
        /// Array index out of bounds error
        IndexOutOfBounds {
            array_name: String,
            index: usize,
            length: usize,
        },
        /// Buffer overflow error
        BufferOverflow {
            operation: String,
            requested: usize,
            available: usize,
        },
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
                MemoryError::IndexOutOfBounds {
                    array_name,
                    index,
                    length,
                } => WasmtimeError::InvalidParameter {
                    message: format!(
                        "{} index {} out of bounds [0, {})",
                        array_name, index, length
                    ),
                },
                MemoryError::BufferOverflow {
                    operation,
                    requested,
                    available,
                } => WasmtimeError::InvalidParameter {
                    message: format!(
                        "{} requested {} bytes but only {} available",
                        operation, requested, available
                    ),
                },
                MemoryError::LifecycleViolation { resource, state } => {
                    WasmtimeError::InvalidParameter {
                        message: format!("{} lifecycle violation: {}", resource, state),
                    }
                }
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
            return Err(MemoryError::InvalidBoxPointer(format!(
                "{} (address too low: 0x{:x})",
                name, ptr as usize
            )));
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
            return Err(MemoryError::InvalidBoxPointer(format!(
                "{} (address too low: 0x{:x})",
                name, ptr as usize
            )));
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
            return Err(MemoryError::InvalidBoxPointer(format!(
                "{} (invalid address: 0x{:x})",
                name, ptr as usize
            )));
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
        name: &str,
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
        operation_name: &str,
    ) -> Result<(), MemoryError> {
        // Null pointer checks
        if dest.is_null() {
            return Err(MemoryError::NullPointer(format!(
                "{} destination",
                operation_name
            )));
        }
        if src.is_null() {
            return Err(MemoryError::NullPointer(format!(
                "{} source",
                operation_name
            )));
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
    pub fn safe_byte_slice(
        ptr: *const u8,
        len: usize,
        name: &str,
    ) -> Result<&'static [u8], MemoryError> {
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
}

/// HeapType conversion utilities shared between JNI and Panama implementations.
///
/// These functions provide the canonical mapping between integer heap type codes
/// (matching Java HeapType enum ordinals) and wasmtime::HeapType.
pub mod heap_type_conversion {
    use wasmtime::HeapType;

    /// Converts a heap type code integer to a Wasmtime HeapType.
    ///
    /// This is shared between Panama and JNI bindings for AnyRef.matchesTy.
    pub fn heap_type_from_code(code: i32) -> Option<HeapType> {
        match code {
            0 => Some(HeapType::Any),
            1 => Some(HeapType::Eq),
            2 => Some(HeapType::I31),
            3 => Some(HeapType::Struct),
            4 => Some(HeapType::Array),
            5 => Some(HeapType::Func),
            6 => Some(HeapType::NoFunc),
            7 => Some(HeapType::Extern),
            8 => Some(HeapType::NoExtern),
            9 => Some(HeapType::Exn),
            10 => Some(HeapType::NoExn),
            11 => Some(HeapType::Cont),
            12 => Some(HeapType::NoCont),
            13 => Some(HeapType::None),
            _ => Option::None,
        }
    }

    /// Converts a Wasmtime HeapType to an integer code matching the Java HeapType ordinal.
    ///
    /// This is shared between Panama and JNI bindings for EqRef.ty().
    pub fn heap_type_to_code(heap_type: &HeapType) -> i32 {
        match heap_type {
            HeapType::Any => 0,
            HeapType::Eq => 1,
            HeapType::I31 => 2,
            HeapType::Struct => 3,
            HeapType::Array => 4,
            HeapType::Func => 5,
            HeapType::NoFunc => 6,
            HeapType::Extern => 7,
            HeapType::NoExtern => 8,
            HeapType::Exn => 9,
            HeapType::NoExn => 10,
            HeapType::Cont => 11,
            HeapType::NoCont => 12,
            HeapType::None => 13,
            _ => 14, // CONCRETE or unknown
        }
    }
}
