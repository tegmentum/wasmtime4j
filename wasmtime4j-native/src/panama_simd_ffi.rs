// Panama FFI bindings for SIMD operations
//
// This module provides Panama Foreign Function Interface exports for WebAssembly SIMD operations.
// These functions are called from Java code using Panama FFI and delegate to the shared SIMD module.

use crate::simd::{SIMDConfig, SIMDOperations};
use crate::error::WasmtimeError;

/// SIMD vector addition (Panama FFI export)
///
/// # Safety
/// - runtime_handle must be a valid runtime handle (unused but kept for API consistency)
/// - a_data and b_data must point to valid 16-byte arrays
/// - result_data must point to a writable 16-byte array
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_add(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1; // Null pointer error
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2, // SIMD initialization error
        };

        match simd_ops.add(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0 // Success
            }
            Err(_) => -3, // Operation error
        }
    }
}

/// SIMD vector subtraction (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_subtract(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.subtract(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD vector multiplication (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_multiply(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.multiply(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD vector division (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_divide(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.divide(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD bitwise AND (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_and(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.and(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD bitwise OR (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_or(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.or(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD bitwise XOR (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_xor(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.xor(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD bitwise NOT (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_not(
    _runtime_handle: i64,
    a_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.not(&a_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}
