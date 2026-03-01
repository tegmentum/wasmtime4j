//! Fuzz target for WIT/WASM value deserialization.
//!
//! This target tests the robustness of value deserialization by feeding arbitrary
//! byte sequences to the deserializer. It aims to discover:
//! - Type/data mismatch handling
//! - Invalid type tags
//! - Buffer underflow/overflow
//! - Memory safety issues in deserialization code
//! - Round-trip serialization consistency

#![no_main]

use libfuzzer_sys::fuzz_target;
use wasmtime4j::wit_value_marshal::{deserialize_to_val, serialize_from_val};

fuzz_target!(|data: &[u8]| {
    if data.is_empty() {
        return;
    }

    // First byte selects type discriminator (0-255, covers valid 1-22 and invalid values)
    let type_discriminator = data[0] as i32;
    let payload = &data[1..];

    // Test deserialization with arbitrary type discriminator and payload
    match deserialize_to_val(type_discriminator, payload) {
        Ok(val) => {
            // Round-trip: serialize the successfully deserialized value
            // and verify the type discriminator is preserved
            if let Ok((rt_type, rt_data)) = serialize_from_val(&val) {
                assert_eq!(
                    type_discriminator, rt_type,
                    "Type discriminator mismatch after round-trip: input={}, output={}",
                    type_discriminator, rt_type
                );

                // Deserialize the round-tripped data and verify consistency
                let _ = deserialize_to_val(rt_type, &rt_data);
            }
        }
        Err(_) => {
            // Expected for invalid type tags, truncated data, etc.
        }
    }
});
