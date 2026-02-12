//! Custom assertion helpers for wasmtime4j-native tests.
//!
//! This module provides specialized assertions for WebAssembly testing scenarios
//! that go beyond standard Rust assertions.

use wasmtime4j::error::WasmtimeError;
use wasmtime4j::instance::WasmValue;

/// Asserts that a result is an error containing the specified substring.
pub fn assert_error_contains<T: std::fmt::Debug>(
    result: Result<T, WasmtimeError>,
    expected_substring: &str,
) {
    match result {
        Err(e) => {
            let error_msg = e.to_string();
            assert!(
                error_msg
                    .to_lowercase()
                    .contains(&expected_substring.to_lowercase()),
                "Expected error containing '{}', got: {}",
                expected_substring,
                error_msg
            );
        }
        Ok(value) => panic!(
            "Expected error containing '{}', but got Ok({:?})",
            expected_substring, value
        ),
    }
}

/// Asserts that a WasmValue matches expected I32 value.
pub fn assert_i32(value: &WasmValue, expected: i32) {
    match value {
        WasmValue::I32(v) => assert_eq!(*v, expected, "I32 value mismatch"),
        other => panic!("Expected I32({}), got: {:?}", expected, other),
    }
}

/// Asserts that a WasmValue matches expected I64 value.
pub fn assert_i64(value: &WasmValue, expected: i64) {
    match value {
        WasmValue::I64(v) => assert_eq!(*v, expected, "I64 value mismatch"),
        other => panic!("Expected I64({}), got: {:?}", expected, other),
    }
}

/// Asserts that a WasmValue matches expected F32 value within epsilon.
pub fn assert_f32(value: &WasmValue, expected: f32) {
    assert_f32_epsilon(value, expected, f32::EPSILON);
}

/// Asserts that a WasmValue matches expected F32 value within specified epsilon.
pub fn assert_f32_epsilon(value: &WasmValue, expected: f32, epsilon: f32) {
    match value {
        WasmValue::F32(v) => {
            assert!(
                (*v - expected).abs() < epsilon,
                "F32 value mismatch: {} != {} (epsilon: {})",
                v,
                expected,
                epsilon
            );
        }
        other => panic!("Expected F32({}), got: {:?}", expected, other),
    }
}

/// Asserts that a WasmValue matches expected F64 value within epsilon.
pub fn assert_f64(value: &WasmValue, expected: f64) {
    assert_f64_epsilon(value, expected, f64::EPSILON);
}

/// Asserts that a WasmValue matches expected F64 value within specified epsilon.
pub fn assert_f64_epsilon(value: &WasmValue, expected: f64, epsilon: f64) {
    match value {
        WasmValue::F64(v) => {
            assert!(
                (*v - expected).abs() < epsilon,
                "F64 value mismatch: {} != {} (epsilon: {})",
                v,
                expected,
                epsilon
            );
        }
        other => panic!("Expected F64({}), got: {:?}", expected, other),
    }
}

/// Asserts that a result is a trap with optional message check.
pub fn assert_trap<T: std::fmt::Debug>(result: Result<T, WasmtimeError>) {
    match result {
        Err(e) => {
            let error_str = e.to_string().to_lowercase();
            assert!(
                error_str.contains("trap")
                    || error_str.contains("wasm trap")
                    || error_str.contains("unreachable"),
                "Expected a trap error, got: {}",
                e
            );
        }
        Ok(value) => panic!("Expected a trap, but got Ok({:?})", value),
    }
}

/// Asserts that a result is a trap containing the specified message.
pub fn assert_trap_message<T: std::fmt::Debug>(
    result: Result<T, WasmtimeError>,
    expected_msg: &str,
) {
    match result {
        Err(e) => {
            let error_str = e.to_string().to_lowercase();
            assert!(
                error_str.contains(&expected_msg.to_lowercase()),
                "Expected trap containing '{}', got: {}",
                expected_msg,
                e
            );
        }
        Ok(value) => panic!(
            "Expected a trap containing '{}', but got Ok({:?})",
            expected_msg, value
        ),
    }
}

/// Asserts that the result vector has the expected length.
pub fn assert_result_count(values: &[WasmValue], expected_count: usize) {
    assert_eq!(
        values.len(),
        expected_count,
        "Expected {} return values, got {}",
        expected_count,
        values.len()
    );
}

/// Asserts that an operation completed within the specified duration.
pub fn assert_duration_under(
    start: std::time::Instant,
    max_duration: std::time::Duration,
    operation_name: &str,
) {
    let elapsed = start.elapsed();
    assert!(
        elapsed < max_duration,
        "{} took {:?}, expected under {:?}",
        operation_name,
        elapsed,
        max_duration
    );
}

/// Asserts that memory contents at offset match expected bytes.
pub fn assert_memory_bytes(memory_data: &[u8], offset: usize, expected: &[u8]) {
    let actual = &memory_data[offset..offset + expected.len()];
    assert_eq!(
        actual, expected,
        "Memory mismatch at offset {}: {:?} != {:?}",
        offset, actual, expected
    );
}

/// Asserts that a pointer is valid (non-null and properly aligned).
pub fn assert_valid_pointer<T>(ptr: *const T) {
    assert!(!ptr.is_null(), "Pointer should not be null");
    assert!(
        (ptr as usize) % std::mem::align_of::<T>() == 0,
        "Pointer should be properly aligned for type"
    );
}

/// Asserts that a handle value is valid (non-zero, non-negative).
pub fn assert_valid_handle(handle: i64) {
    assert!(handle > 0, "Handle should be positive, got: {}", handle);
}

/// Asserts that a result is Ok and returns the value.
pub fn assert_ok<T, E: std::fmt::Debug>(result: Result<T, E>) -> T {
    match result {
        Ok(v) => v,
        Err(e) => panic!("Expected Ok, got Err: {:?}", e),
    }
}

/// Asserts that a result is Err and returns the error.
pub fn assert_err<T: std::fmt::Debug, E>(result: Result<T, E>) -> E {
    match result {
        Ok(v) => panic!("Expected Err, got Ok: {:?}", v),
        Err(e) => e,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_assert_i32() {
        assert_i32(&WasmValue::I32(42), 42);
    }

    #[test]
    #[should_panic(expected = "Expected I32")]
    fn test_assert_i32_wrong_type() {
        assert_i32(&WasmValue::I64(42), 42);
    }

    #[test]
    fn test_assert_f32_epsilon() {
        assert_f32_epsilon(&WasmValue::F32(3.14159), 3.14159, 0.0001);
    }

    #[test]
    fn test_assert_result_count() {
        let values = vec![WasmValue::I32(1), WasmValue::I32(2)];
        assert_result_count(&values, 2);
    }

    #[test]
    fn test_assert_valid_handle() {
        assert_valid_handle(1);
        assert_valid_handle(100);
    }

    #[test]
    #[should_panic(expected = "should be positive")]
    fn test_assert_invalid_handle() {
        assert_valid_handle(0);
    }

    #[test]
    fn test_assert_ok() {
        let result: Result<i32, &str> = Ok(42);
        assert_eq!(assert_ok(result), 42);
    }

    #[test]
    fn test_assert_err() {
        let result: Result<i32, &str> = Err("error");
        assert_eq!(assert_err(result), "error");
    }
}
