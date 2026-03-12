//! Fuzz target for WIT/WASM value serialization round-trip stability.
//!
//! This target tests that serialization produces stable output by performing
//! a double round-trip: deserialize -> serialize -> deserialize -> serialize,
//! then asserting the two serialized outputs are byte-identical.

#![no_main]

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use wasmtime4j::wit_value_marshal::{deserialize_to_val, serialize_from_val};

#[derive(Debug, Arbitrary)]
struct WitSerializeInput {
    type_tag: u8,
    payload: Vec<u8>,
    nesting_depth: u8,
}

fuzz_target!(|input: WitSerializeInput| {
    // Cap nesting depth at 4 to avoid excessive recursion
    let _nesting_depth = input.nesting_depth.min(4);

    // Map type_tag into valid range 1-22
    let type_discriminator = (input.type_tag % 22 + 1) as i32;

    // First deserialize: try to construct a Val from the fuzzed payload
    let val = match deserialize_to_val(type_discriminator, &input.payload) {
        Ok(v) => v,
        Err(_) => return,
    };

    // First serialize: convert the Val back to bytes
    let (rt_type_1, rt_data_1) = match serialize_from_val(&val) {
        Ok(result) => result,
        Err(_) => return,
    };

    // Second deserialize: reconstruct a Val from the first serialized output
    let val2 = match deserialize_to_val(rt_type_1, &rt_data_1) {
        Ok(v) => v,
        Err(_) => {
            panic!(
                "Failed to deserialize previously serialized data: type={}, len={}",
                rt_type_1,
                rt_data_1.len()
            );
        }
    };

    // Second serialize: convert the second Val back to bytes
    let (rt_type_2, rt_data_2) = match serialize_from_val(&val2) {
        Ok(result) => result,
        Err(_) => {
            panic!(
                "Failed to serialize a Val that was deserialized from serialized data: type={}",
                rt_type_1
            );
        }
    };

    // Assert round-trip stability: both serialized outputs must be identical
    assert_eq!(
        rt_type_1, rt_type_2,
        "Type discriminator diverged on double round-trip: first={}, second={}",
        rt_type_1, rt_type_2
    );
    assert_eq!(
        rt_data_1, rt_data_2,
        "Serialized bytes diverged on double round-trip for type={}: first_len={}, second_len={}",
        rt_type_1,
        rt_data_1.len(),
        rt_data_2.len()
    );
});
