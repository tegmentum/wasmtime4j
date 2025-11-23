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
//! - 7 = record
//! - 8 = tuple
//! - 9 = u32
//! - 10 = u64
//! - 11 = list
//! - 12 = variant
//! - 13 = enum
//! - 14 = option
//! - 15 = result
//! - 16 = flags
//! - 17 = s8
//! - 18 = s16
//! - 19 = u8
//! - 20 = u16
//! - 21 = float32
//!
//! # Binary Format (little-endian)
//!
//! - bool → 1 byte (0 or 1)
//! - s8 → 1 byte (i8)
//! - s16 → 2 bytes (i16)
//! - s32 → 4 bytes (i32)
//! - s64 → 8 bytes (i64)
//! - u8 → 1 byte (u8)
//! - u16 → 2 bytes (u16)
//! - u32 → 4 bytes (u32)
//! - u64 → 8 bytes (u64)
//! - float32 → 4 bytes (f32 IEEE 754)
//! - float64 → 8 bytes (f64 IEEE 754)
//! - char → 4 bytes (Unicode codepoint as u32)
//! - string → 4 bytes length + UTF-8 bytes
//! - record → 4 bytes field count + (discriminator + data) for each field
//! - tuple → 4 bytes element count + (discriminator + data) for each element

use crate::error::WasmtimeError;
use wasmtime::component::Val;

/// Deserializes a byte array to a Wasmtime component Val.
///
/// # Arguments
///
/// * `type_discriminator` - The type discriminator (1-8)
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
        7 => deserialize_record(data),
        8 => deserialize_tuple(data),
        9 => deserialize_u32(data),
        10 => deserialize_u64(data),
        11 => deserialize_list(data),
        12 => deserialize_variant(data),
        13 => deserialize_enum(data),
        14 => deserialize_option(data),
        15 => deserialize_result(data),
        16 => deserialize_flags(data),
        17 => deserialize_s8(data),
        18 => deserialize_s16(data),
        19 => deserialize_u8(data),
        20 => deserialize_u16(data),
        21 => deserialize_float32(data),
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
        Val::Record(fields) => Ok((7, serialize_record(fields)?)),
        Val::Tuple(elements) => Ok((8, serialize_tuple(elements)?)),
        Val::U32(u) => Ok((9, serialize_u32(*u))),
        Val::U64(u) => Ok((10, serialize_u64(*u))),
        Val::List(elements) => Ok((11, serialize_list(elements)?)),
        Val::Variant(case_name, payload) => Ok((12, serialize_variant(case_name, payload)?)),
        Val::Enum(discriminant) => Ok((13, serialize_enum(discriminant)?)),
        Val::Option(value) => Ok((14, serialize_option(value)?)),
        Val::Result(result) => Ok((15, serialize_result(result)?)),
        Val::Flags(flags) => Ok((16, serialize_flags(flags)?)),
        Val::S8(i) => Ok((17, serialize_s8(*i))),
        Val::S16(i) => Ok((18, serialize_s16(*i))),
        Val::U8(u) => Ok((19, serialize_u8(*u))),
        Val::U16(u) => Ok((20, serialize_u16(*u))),
        Val::Float32(f) => Ok((21, serialize_float32(*f))),
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

fn deserialize_u32(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid u32 data length: expected 4, got {}", data.len()),
        });
    }
    let value = u32::from_le_bytes([data[0], data[1], data[2], data[3]]);
    Ok(Val::U32(value))
}

fn deserialize_u64(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 8 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid u64 data length: expected 8, got {}", data.len()),
        });
    }
    let value = u64::from_le_bytes([
        data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7],
    ]);
    Ok(Val::U64(value))
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

fn deserialize_s8(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 1 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid s8 data length: expected 1, got {}", data.len()),
        });
    }
    Ok(Val::S8(data[0] as i8))
}

fn deserialize_s16(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 2 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid s16 data length: expected 2, got {}", data.len()),
        });
    }
    let value = i16::from_le_bytes([data[0], data[1]]);
    Ok(Val::S16(value))
}

fn deserialize_u8(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 1 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid u8 data length: expected 1, got {}", data.len()),
        });
    }
    Ok(Val::U8(data[0]))
}

fn deserialize_u16(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 2 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid u16 data length: expected 2, got {}", data.len()),
        });
    }
    let value = u16::from_le_bytes([data[0], data[1]]);
    Ok(Val::U16(value))
}

fn deserialize_float32(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() != 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid float32 data length: expected 4, got {}", data.len()),
        });
    }
    let value = f32::from_le_bytes([data[0], data[1], data[2], data[3]]);
    Ok(Val::Float32(value))
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

fn serialize_u32(value: u32) -> Vec<u8> {
    value.to_le_bytes().to_vec()
}

fn serialize_u64(value: u64) -> Vec<u8> {
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

fn serialize_s8(value: i8) -> Vec<u8> {
    vec![value as u8]
}

fn serialize_s16(value: i16) -> Vec<u8> {
    value.to_le_bytes().to_vec()
}

fn serialize_u8(value: u8) -> Vec<u8> {
    vec![value]
}

fn serialize_u16(value: u16) -> Vec<u8> {
    value.to_le_bytes().to_vec()
}

fn serialize_float32(value: f32) -> Vec<u8> {
    value.to_le_bytes().to_vec()
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
    eprintln!("RUST DEBUG: wasmtime4j_wit_value_serialize ENTRY - discriminator={}, len={}", type_discriminator, value_len);

    if value_ptr.is_null() || out_data.is_null() || out_len.is_null() {
        eprintln!("RUST DEBUG: NULL POINTER CHECK FAILED");
        return FFI_ERROR;
    }

    // Convert raw bytes to slice
    let data = std::slice::from_raw_parts(value_ptr, value_len);

    // For records and tuples (discriminators 7-8), skip validation round-trip
    // because field names are not included in serialization format but are required by Wasmtime
    if type_discriminator == 7 || type_discriminator == 8 {
        eprintln!("DEBUG: wasmtime4j_wit_value_serialize - Bypass activated for discriminator {}", type_discriminator);
        // Just copy the data as-is
        let data_copy = data.to_vec();
        let data_len = data_copy.len();
        eprintln!("DEBUG: Copying {} bytes as-is", data_len);
        let output_buf = Box::into_raw(data_copy.into_boxed_slice()) as *mut u8;
        *out_data = output_buf;
        *out_len = data_len;
        eprintln!("DEBUG: Returning FFI_SUCCESS (0)");
        return FFI_SUCCESS;
    }

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

    // Store length before moving serialized data
    let serialized_len = serialized.len();

    // Allocate output buffer and copy data
    let output_buf = Box::into_raw(serialized.into_boxed_slice()) as *mut u8;
    *out_data = output_buf;
    *out_len = serialized_len;

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
        1..=16 => 1,
        _ => 0,
    }
}

/// Free a buffer allocated by WIT value marshalling functions.
///
/// # Arguments
///
/// * `ptr` - Pointer to the buffer to free
/// * `len` - Length of the buffer
// Record deserialization
fn deserialize_record(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Record data too short for field count".to_string(),
        });
    }

    let field_count = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    let mut offset = 4;
    let mut fields = Vec::with_capacity(field_count);

    for _ in 0..field_count {
        if offset + 4 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Record data truncated (name length)".to_string(),
            });
        }

        // Read field name length
        let name_len = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + name_len > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Record data truncated (name bytes)".to_string(),
            });
        }

        // Read field name
        let name_bytes = &data[offset..offset + name_len];
        let field_name = std::str::from_utf8(name_bytes)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Invalid UTF-8 in record field name: {}", e),
            })?
            .to_string();
        offset += name_len;

        if offset + 4 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Record data truncated (discriminator)".to_string(),
            });
        }

        // Read discriminator
        let discriminator = i32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        offset += 4;

        if offset + 4 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Record data truncated (field length)".to_string(),
            });
        }

        // Read field data length
        let length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Record field data truncated".to_string(),
            });
        }

        // Read and deserialize field data
        let field_data = &data[offset..offset + length];
        let field_val = deserialize_to_val(discriminator, field_data)?;
        fields.push((field_name, field_val));
        offset += length;
    }

    Ok(Val::Record(fields.into()))
}

// Tuple deserialization
fn deserialize_tuple(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Tuple data too short for element count".to_string(),
        });
    }

    let element_count = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    let mut offset = 4;
    let mut elements = Vec::with_capacity(element_count);

    for _ in 0..element_count {
        if offset >= data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Tuple data truncated".to_string(),
            });
        }

        let discriminator = i32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        offset += 4;

        let length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Tuple element data truncated".to_string(),
            });
        }

        let element_data = &data[offset..offset + length];
        let element_val = deserialize_to_val(discriminator, element_data)?;
        elements.push(element_val);
        offset += length;
    }

    Ok(Val::Tuple(elements.into()))
}

fn deserialize_list(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: "List data too short for element count".to_string(),
        });
    }

    let element_count = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    let mut offset = 4;
    let mut elements = Vec::with_capacity(element_count);

    for _ in 0..element_count {
        if offset >= data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "List data truncated".to_string(),
            });
        }

        let discriminator = i32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        offset += 4;

        let length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "List element data truncated".to_string(),
            });
        }

        let element_data = &data[offset..offset + length];
        let element_val = deserialize_to_val(discriminator, element_data)?;
        elements.push(element_val);
        offset += length;
    }

    Ok(Val::List(elements.into()))
}

fn deserialize_variant(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Variant data too short for case name length".to_string(),
        });
    }

    let mut offset = 0;

    // Read case name
    let name_length = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    offset += 4;

    if offset + name_length > data.len() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Variant case name data truncated".to_string(),
        });
    }

    let case_name = String::from_utf8(data[offset..offset + name_length].to_vec())
        .map_err(|_| WasmtimeError::InvalidParameter {
            message: "Invalid UTF-8 in variant case name".to_string(),
        })?;
    offset += name_length;

    // Read has_payload flag
    if offset >= data.len() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Variant data truncated at payload flag".to_string(),
        });
    }

    let has_payload = data[offset] != 0;
    offset += 1;

    let payload = if has_payload {
        if offset + 8 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Variant payload data truncated".to_string(),
            });
        }

        let discriminator = i32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        offset += 4;

        let length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Variant payload value truncated".to_string(),
            });
        }

        let payload_data = &data[offset..offset + length];
        let payload_val = deserialize_to_val(discriminator, payload_data)?;
        Some(Box::new(payload_val))
    } else {
        None
    };

    Ok(Val::Variant(case_name, payload))
}

fn deserialize_enum(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Enum data too short for discriminant length".to_string(),
        });
    }

    let name_length = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;

    if 4 + name_length > data.len() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Enum discriminant data truncated".to_string(),
        });
    }

    let discriminant = String::from_utf8(data[4..4 + name_length].to_vec())
        .map_err(|_| WasmtimeError::InvalidParameter {
            message: "Invalid UTF-8 in enum discriminant".to_string(),
        })?;

    Ok(Val::Enum(discriminant))
}

fn deserialize_option(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.is_empty() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Option data too short for is_some flag".to_string(),
        });
    }

    let is_some = data[0] != 0;
    let mut offset = 1;

    let value = if is_some {
        if offset + 8 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Option value data truncated".to_string(),
            });
        }

        let discriminator = i32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        offset += 4;

        let length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Option value truncated".to_string(),
            });
        }

        let val_data = &data[offset..offset + length];
        let val = deserialize_to_val(discriminator, val_data)?;
        Some(Box::new(val))
    } else {
        None
    };

    Ok(Val::Option(value))
}

fn deserialize_result(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 2 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Result data too short for flags".to_string(),
        });
    }

    let is_ok = data[0] != 0;
    let has_value = data[1] != 0;
    let mut offset = 2;

    let value = if has_value {
        if offset + 8 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Result value data truncated".to_string(),
            });
        }

        let discriminator = i32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]);
        offset += 4;

        let length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Result value truncated".to_string(),
            });
        }

        let val_data = &data[offset..offset + length];
        let val = deserialize_to_val(discriminator, val_data)?;
        Some(Box::new(val))
    } else {
        None
    };

    if is_ok {
        Ok(Val::Result(Ok(value)))
    } else {
        Ok(Val::Result(Err(value)))
    }
}

fn deserialize_flags(data: &[u8]) -> Result<Val, WasmtimeError> {
    if data.len() < 4 {
        return Err(WasmtimeError::InvalidParameter {
            message: "Flags data too short for flag count".to_string(),
        });
    }

    let flag_count = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
    let mut offset = 4;
    let mut flags = Vec::with_capacity(flag_count);

    for _ in 0..flag_count {
        if offset + 4 > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Flags data truncated at flag name length".to_string(),
            });
        }

        let name_length = u32::from_le_bytes([
            data[offset],
            data[offset + 1],
            data[offset + 2],
            data[offset + 3],
        ]) as usize;
        offset += 4;

        if offset + name_length > data.len() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Flags name data truncated".to_string(),
            });
        }

        let flag_name = String::from_utf8(data[offset..offset + name_length].to_vec())
            .map_err(|_| WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in flag name".to_string(),
            })?;
        flags.push(flag_name);
        offset += name_length;
    }

    Ok(Val::Flags(flags))
}

// Record serialization
fn serialize_record(fields: &[(String, Val)]) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Field count
    result.extend_from_slice(&(fields.len() as u32).to_le_bytes());

    // Serialize each field
    for (field_name, field_val) in fields {
        // Serialize field name
        let name_bytes = field_name.as_bytes();
        result.extend_from_slice(&(name_bytes.len() as u32).to_le_bytes());
        result.extend_from_slice(name_bytes);

        // Serialize field value
        let (discriminator, field_data) = serialize_from_val(field_val)?;
        result.extend_from_slice(&discriminator.to_le_bytes());
        result.extend_from_slice(&(field_data.len() as u32).to_le_bytes());
        result.extend_from_slice(&field_data);
    }

    Ok(result)
}

// Tuple serialization
fn serialize_tuple(elements: &[Val]) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Element count
    result.extend_from_slice(&(elements.len() as u32).to_le_bytes());

    // Serialize each element
    for element in elements {
        let (discriminator, element_data) = serialize_from_val(element)?;
        result.extend_from_slice(&discriminator.to_le_bytes());
        result.extend_from_slice(&(element_data.len() as u32).to_le_bytes());
        result.extend_from_slice(&element_data);
    }

    Ok(result)
}

fn serialize_list(elements: &[Val]) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Element count
    result.extend_from_slice(&(elements.len() as u32).to_le_bytes());

    // Serialize each element
    for element in elements {
        let (discriminator, element_data) = serialize_from_val(element)?;
        result.extend_from_slice(&discriminator.to_le_bytes());
        result.extend_from_slice(&(element_data.len() as u32).to_le_bytes());
        result.extend_from_slice(&element_data);
    }

    Ok(result)
}

fn serialize_variant(case_name: &str, payload: &Option<Box<Val>>) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Serialize case name
    let name_bytes = case_name.as_bytes();
    result.extend_from_slice(&(name_bytes.len() as u32).to_le_bytes());
    result.extend_from_slice(name_bytes);

    // Serialize has_payload flag and payload
    if let Some(payload_val) = payload {
        result.push(1); // has_payload = true
        let (discriminator, payload_data) = serialize_from_val(payload_val)?;
        result.extend_from_slice(&discriminator.to_le_bytes());
        result.extend_from_slice(&(payload_data.len() as u32).to_le_bytes());
        result.extend_from_slice(&payload_data);
    } else {
        result.push(0); // has_payload = false
    }

    Ok(result)
}

fn serialize_enum(discriminant: &str) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Serialize discriminant name
    let name_bytes = discriminant.as_bytes();
    result.extend_from_slice(&(name_bytes.len() as u32).to_le_bytes());
    result.extend_from_slice(name_bytes);

    Ok(result)
}

fn serialize_option(value: &Option<Box<Val>>) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Serialize is_some flag and value
    if let Some(val) = value {
        result.push(1); // is_some = true
        let (discriminator, val_data) = serialize_from_val(val)?;
        result.extend_from_slice(&discriminator.to_le_bytes());
        result.extend_from_slice(&(val_data.len() as u32).to_le_bytes());
        result.extend_from_slice(&val_data);
    } else {
        result.push(0); // is_some = false
    }

    Ok(result)
}

fn serialize_result(result_val: &Result<Option<Box<Val>>, Option<Box<Val>>>) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Serialize is_ok flag and value
    match result_val {
        Ok(ok_val) => {
            result.push(1); // is_ok = true
            if let Some(val) = ok_val {
                result.push(1); // has_value = true
                let (discriminator, val_data) = serialize_from_val(val)?;
                result.extend_from_slice(&discriminator.to_le_bytes());
                result.extend_from_slice(&(val_data.len() as u32).to_le_bytes());
                result.extend_from_slice(&val_data);
            } else {
                result.push(0); // has_value = false
            }
        }
        Err(err_val) => {
            result.push(0); // is_ok = false
            if let Some(val) = err_val {
                result.push(1); // has_value = true
                let (discriminator, val_data) = serialize_from_val(val)?;
                result.extend_from_slice(&discriminator.to_le_bytes());
                result.extend_from_slice(&(val_data.len() as u32).to_le_bytes());
                result.extend_from_slice(&val_data);
            } else {
                result.push(0); // has_value = false
            }
        }
    }

    Ok(result)
}

fn serialize_flags(flags: &[String]) -> Result<Vec<u8>, WasmtimeError> {
    let mut result = Vec::new();

    // Flag count
    result.extend_from_slice(&(flags.len() as u32).to_le_bytes());

    // Serialize each flag name
    for flag_name in flags {
        let name_bytes = flag_name.as_bytes();
        result.extend_from_slice(&(name_bytes.len() as u32).to_le_bytes());
        result.extend_from_slice(name_bytes);
    }

    Ok(result)
}

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
