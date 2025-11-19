/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//! WIT value marshalling between Java binary format and Wasmtime component values.
//!
//! This module provides bidirectional conversion between the serialized WIT values from Java
//! and the `wasmtime::component::Val` type used by the Wasmtime component runtime.
//!
//! # Type Discriminators
//!
//! - 1 = bool
//! - 2 = s32
//! - 3 = s64
//! - 4 = float64
//! - 5 = char (Unicode codepoint)
//! - 6 = string
//!
//! # Binary Format (little-endian)
//!
//! - bool → 1 byte (0 or 1)
//! - s32 → 4 bytes (i32)
//! - s64 → 8 bytes (i64)
//! - float64 → 8 bytes (f64 IEEE 754)
//! - char → 4 bytes (Unicode codepoint as u32)
//! - string → 4 bytes length + UTF-8 bytes

use crate::error::WasmtimeError;
use wasmtime::component::Val;

/// Deserializes a byte array to a Wasmtime component Val.
///
/// # Arguments
///
/// * `type_discriminator` - The type discriminator (1-6)
/// * `data` - The serialized byte array
///
/// # Returns
///
/// A Wasmtime component Val
///
/// # Errors
///
/// Returns an error if:
/// - The type discriminator is invalid
/// - The data length is incorrect for the type
/// - The data cannot be deserialized
pub fn deserialize_to_val(type_discriminator: i32, data: &[u8]) -> Result<Val, WasmtimeError> {
    match type_discriminator {
        1 => deserialize_bool(data),
        2 => deserialize_s32(data),
        3 => deserialize_s64(data),
        4 => deserialize_float64(data),
        5 => deserialize_char(data),
        6 => deserialize_string(data),
        _ => Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid type discriminator: {}", type_discriminator),
        }),
    }
}

/// Serializes a Wasmtime component Val to binary format.
///
/// # Arguments
///
/// * `val` - The Wasmtime component Val to serialize
///
/// # Returns
///
/// A tuple of (type_discriminator, data)
///
/// # Errors
///
/// Returns an error if the Val type is not supported
pub fn serialize_from_val(val: &Val) -> Result<(i32, Vec<u8>), WasmtimeError> {
    match val {
        Val::Bool(b) => Ok((1, serialize_bool(*b))),
        Val::S32(i) => Ok((2, serialize_s32(*i))),
        Val::S64(i) => Ok((3, serialize_s64(*i))),
        Val::Float64(f) => Ok((4, serialize_float64(*f))),
        Val::Char(c) => Ok((5, serialize_char(*c))),
        Val::String(s) => Ok((6, serialize_string(s))),
        _ => Err(WasmtimeError::Runtime {
            message: format!("Unsupported Val type for serialization: {:?}", val),
            backtrace: None,
        }),
    }
}

// Deserialization functions

fn deserialize_bool(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 1 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid bool data length: expected 1, got {}", data.len()),
        });
    }
    Ok(Val::Bool(data[0] != 0))
}

fn deserialize_s32(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid s32 data length: expected 4, got {}", data.len()),
        });
    }
    let value = i32::from_le_bytes([data[0], data[1], data[2], data[3]]);
    Ok(Val::S32(value))
}

fn deserialize_s64(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 8 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid s64 data length: expected 8, got {}", data.len()),
        });
    }
    let value = i64::from_le_bytes([
        data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7],
    ]);
    Ok(Val::S64(value))
}

fn deserialize_float64(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 8 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid float64 data length: expected 8, got {}", data.len()),
        });
    }
    let value = f64::from_le_bytes([
        data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7],
    ]);
    Ok(Val::Float64(value))
}

fn deserialize_char(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid char data length: expected 4, got {}", data.len()),
        });
    }
    let codepoint = u32::from_le_bytes([data[0], data[1], data[2], data[3]]);
    let c = char::from_u32(codepoint).ok_or_else(|| WasmtimeError::InvalidParameter {
        message: format!("Invalid Unicode codepoint: 0x{:08X}", codepoint),
    })?;
    Ok(Val::Char(c))
}

fn deserialize_string(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid string data length: expected at least 4, got {}", data.len()),
        });
    }

    let length = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;

    if data.len() != 4 + length {
        return Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Invalid string data: length prefix says {}, but data has {} bytes",
                length,
                data.len() - 4
            ),
        });
    }

    let s = std::str::from_utf8(&data[4..])
        .map_err(|e| WasmtimeError::InvalidParameter {
            message: format!("Invalid UTF-8 in string data: {}", e),
        })?
        .to_string();

    Ok(Val::String(s.into()))
}

// Serialization functions

fn serialize_bool(value: bool) -> Vec<u8> {
    vec![if value { 1 } else { 0 }]
}

fn serialize_s32(value: i32) -> Vec<u8> {
    value.to_le_bytes().to_vec()
}

fn serialize_s64(value: i64) -> Vec<u8> {
    value.to_le_bytes().to_vec()
}

fn serialize_float64(value: f64) -> Vec<u8> {
    value.to_le_bytes().to_vec()
}

fn serialize_char(value: char) -> Vec<u8> {
    (value as u32).to_le_bytes().to_vec()
}

fn serialize_string(value: &str) -> Vec<u8> {
    let utf8_bytes = value.as_bytes();
    let length = utf8_bytes.len() as u32;

    let mut data = Vec::with_capacity(4 + utf8_bytes.len());
    data.extend_from_slice(&length.to_le_bytes());
    data.extend_from_slice(utf8_bytes);
    data
}

// C-ABI exports for Java FFI

use std::os::raw::c_int;

const FFI_SUCCESS: c_int = 0;
const FFI_ERROR: c_int = -1;

/// Serialize a WIT value to binary format.
///
/// # Arguments
///
/// * `type_discriminator` - The type discriminator (1-6)
/// * `value_ptr` - Pointer to the value data
/// * `value_len` - Length of the value data
/// * `out_data` - Output buffer for serialized data
/// * `out_len` - Output parameter for data length
///
/// # Safety
///
/// This function is unsafe because it operates on raw pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_value_serialize(
    type_discriminator: c_int,
    value_ptr: *const u8,
    value_len: usize,
    out_data: *mut *mut u8,
    out_len: *mut usize,
) -> c_int {
    if value_ptr.is_null() || out_data.is_null() || out_len.is_null() {
        return FFI_ERROR;
    }

    // Convert raw bytes to slice
    let data = std::slice::from_raw_parts(value_ptr, value_len);

    // Deserialize to Val
    let val = match deserialize_to_val(type_discriminator, data) {
        Ok(v) => v,
        Err(_) => return FFI_ERROR,
    };

    // Serialize back (validation round-trip)
    let (_, serialized) = match serialize_from_val(&val) {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    // Allocate output buffer and copy data
    let output_buf = Box::into_raw(serialized.into_boxed_slice()) as *mut u8;
    *out_data = output_buf;
    *out_len = value_len;

    FFI_SUCCESS
}

/// Deserialize a WIT value from binary format.
///
/// # Arguments
///
/// * `type_discriminator` - The type discriminator (1-6)
/// * `data_ptr` - Pointer to the serialized data
/// * `data_len` - Length of the data
/// * `out_value` - Output buffer for deserialized value
/// * `out_len` - Output parameter for value length
///
/// # Safety
///
/// This function is unsafe because it operates on raw pointers.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_value_deserialize(
    type_discriminator: c_int,
    data_ptr: *const u8,
    data_len: usize,
    out_value: *mut *mut u8,
    out_len: *mut usize,
) -> c_int {
    if data_ptr.is_null() || out_value.is_null() || out_len.is_null() {
        return FFI_ERROR;
    }

    // Convert raw bytes to slice
    let data = std::slice::from_raw_parts(data_ptr, data_len);

    // Deserialize to Val
    let val = match deserialize_to_val(type_discriminator, data) {
        Ok(v) => v,
        Err(_) => return FFI_ERROR,
    };

    // Serialize to output format
    let (_, serialized) = match serialize_from_val(&val) {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    // Allocate output buffer and copy data
    let len = serialized.len();
    let output_buf = Box::into_raw(serialized.into_boxed_slice()) as *mut u8;
    *out_value = output_buf;
    *out_len = len;

    FFI_SUCCESS
}

/// Validate a type discriminator.
///
/// # Arguments
///
/// * `type_discriminator` - The type discriminator to validate
///
/// # Returns
///
/// 1 if valid, 0 if invalid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_value_validate_discriminator(
    type_discriminator: c_int,
) -> c_int {
    match type_discriminator {
        1..=6 => 1,
        _ => 0,
    }
}

/// Free a buffer allocated by WIT value marshalling functions.
///
/// # Arguments
///
/// * `ptr` - Pointer to the buffer to free
/// * `len` - Length of the buffer
///
/// # Safety
///
/// This function is unsafe because it operates on raw pointers.
/// The pointer must have been allocated by a WIT value marshalling function.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wit_value_free_buffer(ptr: *mut u8, len: usize) {
    if !ptr.is_null() && len > 0 {
        let _ = Box::from_raw(std::slice::from_raw_parts_mut(ptr, len));
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_bool_roundtrip() {
        let val = Val::Bool(true);
        let (discriminator, data) = serialize_from_val(&val).unwrap();
        assert_eq!(discriminator, 1);
        assert_eq!(data, vec![1]);

        let deserialized = deserialize_to_val(discriminator, &data).unwrap();
        match deserialized {
            Val::Bool(b) => assert!(b),
            _ => panic!("Expected Bool"),
        }
    }

    #[test]
    fn test_s32_roundtrip() {
        let val = Val::S32(42);
        let (discriminator, data) = serialize_from_val(&val).unwrap();
        assert_eq!(discriminator, 2);

        let deserialized = deserialize_to_val(discriminator, &data).unwrap();
        match deserialized {
            Val::S32(i) => assert_eq!(i, 42),
            _ => panic!("Expected S32"),
        }
    }

    #[test]
    fn test_s64_roundtrip() {
        let val = Val::S64(9223372036854775807);
        let (discriminator, data) = serialize_from_val(&val).unwrap();
        assert_eq!(discriminator, 3);

        let deserialized = deserialize_to_val(discriminator, &data).unwrap();
        match deserialized {
            Val::S64(i) => assert_eq!(i, 9223372036854775807),
            _ => panic!("Expected S64"),
        }
    }

    #[test]
    fn test_float64_roundtrip() {
        let val = Val::Float64(3.14159);
        let (discriminator, data) = serialize_from_val(&val).unwrap();
        assert_eq!(discriminator, 4);

        let deserialized = deserialize_to_val(discriminator, &data).unwrap();
        match deserialized {
            Val::Float64(f) => assert_eq!(f, 3.14159),
            _ => panic!("Expected Float64"),
        }
    }

    #[test]
    fn test_char_roundtrip() {
        let val = Val::Char('A');
        let (discriminator, data) = serialize_from_val(&val).unwrap();
        assert_eq!(discriminator, 5);

        let deserialized = deserialize_to_val(discriminator, &data).unwrap();
        match deserialized {
            Val::Char(c) => assert_eq!(c, 'A'),
            _ => panic!("Expected Char"),
        }
    }

    #[test]
    fn test_string_roundtrip() {
        let val = Val::String("hello world".to_string().into());
        let (discriminator, data) = serialize_from_val(&val).unwrap();
        assert_eq!(discriminator, 6);

        let deserialized = deserialize_to_val(discriminator, &data).unwrap();
        match deserialized {
            Val::String(s) => assert_eq!(s.to_string(), "hello world"),
            _ => panic!("Expected String"),
        }
    }

    #[test]
    fn test_invalid_discriminator() {
        let result = deserialize_to_val(99, &[0]);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_bool_length() {
        let result = deserialize_to_val(1, &[0, 1]);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_s32_length() {
        let result = deserialize_to_val(2, &[0, 1]);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_char_codepoint() {
        // 0xD800 is not a valid Unicode codepoint (surrogate range)
        let data = vec![0x00, 0xD8, 0x00, 0x00];
        let result = deserialize_to_val(5, &data);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_string_length() {
        // Length says 10 bytes, but we only provide 5
        let data = vec![10, 0, 0, 0, b'h', b'e', b'l', b'l', b'o'];
        let result = deserialize_to_val(6, &data);
        assert!(result.is_err());
    }

    #[test]
    fn test_invalid_utf8() {
        // Invalid UTF-8 sequence
        let data = vec![4, 0, 0, 0, 0xFF, 0xFF, 0xFF, 0xFF];
        let result = deserialize_to_val(6, &data);
        assert!(result.is_err());
    }
}
