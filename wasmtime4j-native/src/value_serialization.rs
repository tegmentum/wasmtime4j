//! Value serialization for cross-thread WASM value passing
//!
//! This module provides serialization and deserialization of Wasmtime Val types
//! to byte arrays for passing values across thread boundaries and JNI.

use crate::error::{WasmtimeError, WasmtimeResult};
use wasmtime::{Val, V128};

/// Type tags for serialized values
#[repr(u8)]
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ValueType {
    I32 = 0x01,
    I64 = 0x02,
    F32 = 0x03,
    F64 = 0x04,
    V128 = 0x05,
    FuncRef = 0x06,
    ExternRef = 0x07,
}

impl ValueType {
    fn from_u8(byte: u8) -> WasmtimeResult<Self> {
        match byte {
            0x01 => Ok(ValueType::I32),
            0x02 => Ok(ValueType::I64),
            0x03 => Ok(ValueType::F32),
            0x04 => Ok(ValueType::F64),
            0x05 => Ok(ValueType::V128),
            0x06 => Ok(ValueType::FuncRef),
            0x07 => Ok(ValueType::ExternRef),
            _ => Err(WasmtimeError::Validation {
                message: format!("Invalid value type tag: 0x{:02x}", byte),
            }),
        }
    }
}

/// Serialize a slice of WASM values to bytes
///
/// Format: [count: u32][value1][value2]...[valueN]
/// Where each value is: [type_tag: u8][value_bytes]
///
/// # Examples
///
/// ```ignore
/// let values = vec![Val::I32(42), Val::I64(100)];
/// let bytes = serialize_values(&values)?;
/// ```
pub fn serialize_values(values: &[Val]) -> WasmtimeResult<Vec<u8>> {
    let mut buffer = Vec::new();

    // Write count
    buffer.extend_from_slice(&(values.len() as u32).to_le_bytes());

    // Write each value
    for value in values {
        serialize_value(value, &mut buffer)?;
    }

    Ok(buffer)
}

/// Serialize a single value to the buffer
fn serialize_value(value: &Val, buffer: &mut Vec<u8>) -> WasmtimeResult<()> {
    match value {
        Val::I32(v) => {
            buffer.push(ValueType::I32 as u8);
            buffer.extend_from_slice(&v.to_le_bytes());
        }
        Val::I64(v) => {
            buffer.push(ValueType::I64 as u8);
            buffer.extend_from_slice(&v.to_le_bytes());
        }
        Val::F32(v) => {
            buffer.push(ValueType::F32 as u8);
            buffer.extend_from_slice(&v.to_le_bytes());
        }
        Val::F64(v) => {
            buffer.push(ValueType::F64 as u8);
            buffer.extend_from_slice(&v.to_le_bytes());
        }
        Val::V128(v) => {
            buffer.push(ValueType::V128 as u8);
            buffer.extend_from_slice(&v.as_u128().to_le_bytes());
        }
        Val::FuncRef(_) => {
            buffer.push(ValueType::FuncRef as u8);
            // FuncRef: serialize as null (0x00) since we can't serialize function references
            buffer.push(0x00);
        }
        Val::ExternRef(_) => {
            buffer.push(ValueType::ExternRef as u8);
            // ExternRef: serialize as null (0x00) since we can't serialize extern references
            buffer.push(0x00);
        }
        Val::AnyRef(_) | Val::ExnRef(_) | Val::ContRef(_) => {
            return Err(WasmtimeError::Validation {
                message: format!("Cannot serialize value type: {:?}", value),
            });
        }
    }

    Ok(())
}

/// Deserialize bytes back into WASM values
///
/// # Examples
///
/// ```ignore
/// let bytes = serialize_values(&values)?;
/// let deserialized = deserialize_values(&bytes)?;
/// assert_eq!(values, deserialized);
/// ```
pub fn deserialize_values(bytes: &[u8]) -> WasmtimeResult<Vec<Val>> {
    if bytes.len() < 4 {
        return Err(WasmtimeError::Validation {
            message: "Buffer too small to contain value count".to_string(),
        });
    }

    // Read count
    let count = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]) as usize;
    let mut offset = 4;
    let mut values = Vec::with_capacity(count);

    // Read each value
    for _ in 0..count {
        let (value, bytes_read) = deserialize_value(&bytes[offset..])?;
        values.push(value);
        offset += bytes_read;
    }

    Ok(values)
}

/// Deserialize a single value from the buffer
///
/// Returns the value and the number of bytes consumed
fn deserialize_value(bytes: &[u8]) -> WasmtimeResult<(Val, usize)> {
    if bytes.is_empty() {
        return Err(WasmtimeError::Validation {
            message: "Buffer too small to contain value type tag".to_string(),
        });
    }

    let type_tag = ValueType::from_u8(bytes[0])?;
    let mut offset = 1;

    let value = match type_tag {
        ValueType::I32 => {
            if bytes.len() < offset + 4 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for I32 value".to_string(),
                });
            }
            let value = i32::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
            ]);
            offset += 4;
            Val::I32(value)
        }
        ValueType::I64 => {
            if bytes.len() < offset + 8 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for I64 value".to_string(),
                });
            }
            let value = i64::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
                bytes[offset + 4],
                bytes[offset + 5],
                bytes[offset + 6],
                bytes[offset + 7],
            ]);
            offset += 8;
            Val::I64(value)
        }
        ValueType::F32 => {
            if bytes.len() < offset + 4 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for F32 value".to_string(),
                });
            }
            let value = u32::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
            ]);
            offset += 4;
            Val::F32(value)
        }
        ValueType::F64 => {
            if bytes.len() < offset + 8 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for F64 value".to_string(),
                });
            }
            let value = u64::from_le_bytes([
                bytes[offset],
                bytes[offset + 1],
                bytes[offset + 2],
                bytes[offset + 3],
                bytes[offset + 4],
                bytes[offset + 5],
                bytes[offset + 6],
                bytes[offset + 7],
            ]);
            offset += 8;
            Val::F64(value)
        }
        ValueType::V128 => {
            if bytes.len() < offset + 16 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for V128 value".to_string(),
                });
            }
            let mut value_bytes = [0u8; 16];
            value_bytes.copy_from_slice(&bytes[offset..offset + 16]);
            offset += 16;
            let value = u128::from_le_bytes(value_bytes);
            Val::V128(V128::from(value))
        }
        ValueType::FuncRef => {
            if bytes.len() < offset + 1 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for FuncRef value".to_string(),
                });
            }
            offset += 1;
            // FuncRef is always deserialized as null
            Val::FuncRef(None)
        }
        ValueType::ExternRef => {
            if bytes.len() < offset + 1 {
                return Err(WasmtimeError::Validation {
                    message: "Buffer too small for ExternRef value".to_string(),
                });
            }
            offset += 1;
            // ExternRef is always deserialized as null
            Val::ExternRef(None)
        }
    };

    Ok((value, offset))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_serialize_deserialize_i32() {
        let values = vec![Val::I32(0), Val::I32(42), Val::I32(-100), Val::I32(i32::MAX), Val::I32(i32::MIN)];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        for (original, deserialized) in values.iter().zip(deserialized.iter()) {
            if let (Val::I32(o), Val::I32(d)) = (original, deserialized) {
                assert_eq!(o, d);
            } else {
                panic!("Type mismatch");
            }
        }
    }

    #[test]
    fn test_serialize_deserialize_i64() {
        let values = vec![Val::I64(0), Val::I64(42), Val::I64(-100), Val::I64(i64::MAX), Val::I64(i64::MIN)];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        for (original, deserialized) in values.iter().zip(deserialized.iter()) {
            if let (Val::I64(o), Val::I64(d)) = (original, deserialized) {
                assert_eq!(o, d);
            } else {
                panic!("Type mismatch");
            }
        }
    }

    #[test]
    fn test_serialize_deserialize_f32() {
        let values = vec![
            Val::F32(0.0_f32.to_bits()),
            Val::F32(3.14_f32.to_bits()),
            Val::F32((-2.71_f32).to_bits()),
            Val::F32(f32::MAX.to_bits()),
            Val::F32(f32::MIN.to_bits()),
        ];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        for (original, deserialized) in values.iter().zip(deserialized.iter()) {
            if let (Val::F32(o), Val::F32(d)) = (original, deserialized) {
                assert_eq!(o, d);
            } else {
                panic!("Type mismatch");
            }
        }
    }

    #[test]
    fn test_serialize_deserialize_f64() {
        let values = vec![
            Val::F64(0.0_f64.to_bits()),
            Val::F64(3.14159265359_f64.to_bits()),
            Val::F64((-2.71828_f64).to_bits()),
            Val::F64(f64::MAX.to_bits()),
            Val::F64(f64::MIN.to_bits()),
        ];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        for (original, deserialized) in values.iter().zip(deserialized.iter()) {
            if let (Val::F64(o), Val::F64(d)) = (original, deserialized) {
                assert_eq!(o, d);
            } else {
                panic!("Type mismatch");
            }
        }
    }

    #[test]
    fn test_serialize_deserialize_v128() {
        let values = vec![
            Val::V128(V128::from(0u128)),
            Val::V128(V128::from(u128::from_le_bytes([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16]))),
            Val::V128(V128::from(u128::MAX)),
        ];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        for (original, deserialized) in values.iter().zip(deserialized.iter()) {
            if let (Val::V128(o), Val::V128(d)) = (original, deserialized) {
                assert_eq!(o.as_u128(), d.as_u128());
            } else {
                panic!("Type mismatch");
            }
        }
    }

    #[test]
    fn test_serialize_deserialize_mixed_types() {
        let values = vec![
            Val::I32(42),
            Val::I64(100),
            Val::F32(3.14_f32.to_bits()),
            Val::F64(2.71828_f64.to_bits()),
            Val::V128(V128::from(u128::from_le_bytes([0xDE, 0xAD, 0xBE, 0xEF, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]))),
        ];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());

        if let (Val::I32(o), Val::I32(d)) = (&values[0], &deserialized[0]) {
            assert_eq!(o, d);
        } else {
            panic!("I32 mismatch");
        }

        if let (Val::I64(o), Val::I64(d)) = (&values[1], &deserialized[1]) {
            assert_eq!(o, d);
        } else {
            panic!("I64 mismatch");
        }

        if let (Val::F32(o), Val::F32(d)) = (&values[2], &deserialized[2]) {
            assert_eq!(o, d);
        } else {
            panic!("F32 mismatch");
        }

        if let (Val::F64(o), Val::F64(d)) = (&values[3], &deserialized[3]) {
            assert_eq!(o, d);
        } else {
            panic!("F64 mismatch");
        }

        if let (Val::V128(o), Val::V128(d)) = (&values[4], &deserialized[4]) {
            assert_eq!(o.as_u128(), d.as_u128());
        } else {
            panic!("V128 mismatch");
        }
    }

    #[test]
    fn test_serialize_deserialize_empty() {
        let values: Vec<Val> = vec![];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        assert_eq!(deserialized.len(), 0);
    }

    #[test]
    fn test_deserialize_invalid_type_tag() {
        let bytes = vec![
            0x04, 0x00, 0x00, 0x00, // count = 4
            0xFF, // invalid type tag
        ];
        let result = deserialize_values(&bytes);
        assert!(result.is_err());
    }

    #[test]
    fn test_deserialize_truncated_buffer() {
        let bytes = vec![
            0x01, 0x00, 0x00, 0x00, // count = 1
            0x02, // I64 type tag
            0x42, // only 1 byte instead of 8
        ];
        let result = deserialize_values(&bytes);
        assert!(result.is_err());
    }

    #[test]
    fn test_funcref_serialization() {
        let values = vec![Val::FuncRef(None)];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        if let Val::FuncRef(None) = deserialized[0] {
            // Success
        } else {
            panic!("Expected FuncRef(None)");
        }
    }

    #[test]
    fn test_externref_serialization() {
        let values = vec![Val::ExternRef(None)];
        let serialized = serialize_values(&values).unwrap();
        let deserialized = deserialize_values(&serialized).unwrap();

        assert_eq!(values.len(), deserialized.len());
        if let Val::ExternRef(None) = deserialized[0] {
            // Success
        } else {
            panic!("Expected ExternRef(None)");
        }
    }
}
