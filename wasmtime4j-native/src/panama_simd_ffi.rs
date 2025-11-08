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

/// SIMD equality comparison (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_equals(
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

        match simd_ops.equals(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD less than comparison (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_less_than(
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

        match simd_ops.less_than(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD greater than comparison (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_greater_than(
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

        match simd_ops.greater_than(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD saturated addition (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_add_saturated(
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

        match simd_ops.add_saturated(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD square root (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_sqrt(
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

        match simd_ops.sqrt(&a_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD reciprocal (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_reciprocal(
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

        match simd_ops.reciprocal(&a_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD reciprocal square root (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_rsqrt(
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

        match simd_ops.rsqrt(&a_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD fused multiply-add (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_fma(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    c_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || c_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let c_slice = std::slice::from_raw_parts(c_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        let mut c_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);
        c_bytes.copy_from_slice(c_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };
        let c_v128 = crate::simd::V128 { data: c_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.fma(&a_v128, &b_v128, &c_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD fused multiply-subtract (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_fms(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    c_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || c_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let c_slice = std::slice::from_raw_parts(c_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        let mut c_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);
        c_bytes.copy_from_slice(c_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };
        let c_v128 = crate::simd::V128 { data: c_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.fms(&a_v128, &b_v128, &c_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD shuffle (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_shuffle(
    _runtime_handle: i64,
    a_data: *const u8,
    b_data: *const u8,
    indices_data: *const u8,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || b_data.is_null() || indices_data.is_null() || result_data.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);
        let b_slice = std::slice::from_raw_parts(b_data, 16);
        let indices_slice = std::slice::from_raw_parts(indices_data, 16);
        let result_slice = std::slice::from_raw_parts_mut(result_data, 16);

        let mut a_bytes = [0u8; 16];
        let mut b_bytes = [0u8; 16];
        let mut indices_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);
        b_bytes.copy_from_slice(b_slice);
        indices_bytes.copy_from_slice(indices_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };
        let b_v128 = crate::simd::V128 { data: b_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.shuffle(&a_v128, &b_v128, &indices_bytes) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD relaxed addition (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_relaxed_add(
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

        match simd_ops.relaxed_add(&a_v128, &b_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD extract lane i32 (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_extract_lane_i32(
    _runtime_handle: i64,
    a_data: *const u8,
    lane_index: i32,
    result: *mut i32,
) -> i32 {
    if a_data.is_null() || result.is_null() {
        return -1;
    }

    if lane_index < 0 || lane_index > 3 {
        return -4; // Invalid lane index
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);

        let mut a_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.extract_lane_i32(&a_v128, lane_index as u8) {
            Ok(value) => {
                *result = value;
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD replace lane i32 (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_replace_lane_i32(
    _runtime_handle: i64,
    a_data: *const u8,
    lane_index: i32,
    value: i32,
    result_data: *mut u8,
) -> i32 {
    if a_data.is_null() || result_data.is_null() {
        return -1;
    }

    if lane_index < 0 || lane_index > 3 {
        return -4; // Invalid lane index
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

        match simd_ops.replace_lane_i32(&a_v128, lane_index as u8, value) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD convert i32 to f32 (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_convert_i32_to_f32(
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

        match simd_ops.convert_i32_to_f32(&a_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD convert f32 to i32 (Panama FFI export)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_convert_f32_to_i32(
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

        match simd_ops.convert_f32_to_i32(&a_v128) {
            Ok(result) => {
                result_slice.copy_from_slice(&result.data);
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD horizontal sum reduction (Panama FFI export)
/// Returns the sum of all i32 lanes in the vector
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_horizontal_sum_i32(
    _runtime_handle: i64,
    a_data: *const u8,
    result: *mut i32,
) -> i32 {
    if a_data.is_null() || result.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);

        let mut a_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.reduce_sum_i32(&a_v128) {
            Ok(value) => {
                *result = value;
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD horizontal min reduction (Panama FFI export)
/// Returns the minimum of all i32 lanes in the vector
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_horizontal_min_i32(
    _runtime_handle: i64,
    a_data: *const u8,
    result: *mut i32,
) -> i32 {
    if a_data.is_null() || result.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);

        let mut a_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.reduce_min_i32(&a_v128) {
            Ok(value) => {
                *result = value;
                0
            }
            Err(_) => -3,
        }
    }
}

/// SIMD horizontal max reduction (Panama FFI export)
/// Returns the maximum of all i32 lanes in the vector
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_simd_horizontal_max_i32(
    _runtime_handle: i64,
    a_data: *const u8,
    result: *mut i32,
) -> i32 {
    if a_data.is_null() || result.is_null() {
        return -1;
    }

    unsafe {
        let a_slice = std::slice::from_raw_parts(a_data, 16);

        let mut a_bytes = [0u8; 16];
        a_bytes.copy_from_slice(a_slice);

        let a_v128 = crate::simd::V128 { data: a_bytes };

        let config = SIMDConfig::default();
        let simd_ops = match SIMDOperations::new(config) {
            Ok(ops) => ops,
            Err(_) => return -2,
        };

        match simd_ops.reduce_max_i32(&a_v128) {
            Ok(value) => {
                *result = value;
                0
            }
            Err(_) => -3,
        }
    }
}
