//! Fuzz target for WIT/WASM value deserialization.
//!
//! This target tests the robustness of value deserialization by feeding arbitrary
//! byte sequences to the deserializer. It aims to discover:
//! - Type/data mismatch handling
//! - Invalid type tags
//! - Buffer underflow/overflow
//! - Count field manipulation
//! - Memory safety issues in deserialization code

#![no_main]

use libfuzzer_sys::fuzz_target;

fuzz_target!(|data: &[u8]| {
    // Test raw bytes directly - the deserializer should handle any input gracefully
    // This exercises all code paths including:
    // - Invalid count values (too large, causing allocation issues)
    // - Invalid type tags
    // - Truncated data for each type
    // - Mismatched count vs actual data
    let _ = wasmtime4j::value_serialization::deserialize_values(data);
});
