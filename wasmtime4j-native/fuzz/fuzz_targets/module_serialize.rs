#![no_main]

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use wasmtime::{Engine, Module};

/// Structured input for module serialization fuzzing.
///
/// Tests serialize/corrupt/deserialize round-trips with various mutation strategies.
#[derive(Debug, Arbitrary)]
struct ModuleSerializeInput {
    /// Offset within serialized bytes to apply mutation.
    mutation_offset: u16,
    /// Byte value used for bit flip or overwrite mutations.
    mutation_byte: u8,
    /// Length to truncate serialized output to.
    truncate_len: u16,
    /// Operation selector: 0=bit flip, 1=truncate, 2=append garbage, 3=zero out range.
    operation: u8,
    /// Extra bytes used for append-garbage operation.
    extra_bytes: Vec<u8>,
}

const VALID_MODULE_WAT: &str = r#"
(module
    (func $add (param i32 i32) (result i32) local.get 0 local.get 1 i32.add)
    (func $identity (param i32) (result i32) local.get 0)
    (memory 1)
    (export "add" (func $add))
    (export "identity" (func $identity))
    (export "memory" (memory 0))
)
"#;

fuzz_target!(|input: ModuleSerializeInput| {
    let engine = Engine::default();

    // Compile a valid module
    let module = match Module::new(&engine, VALID_MODULE_WAT) {
        Ok(m) => m,
        Err(_) => return,
    };

    // Serialize the module
    let serialized = match module.serialize() {
        Ok(s) => s,
        Err(_) => return,
    };

    if serialized.is_empty() {
        return;
    }

    // Apply mutation based on operation type
    let mutated = match input.operation % 4 {
        0 => {
            // Bit flip at offset
            let mut data = serialized.clone();
            let idx = (input.mutation_offset as usize) % data.len();
            let flip = if input.mutation_byte == 0 { 0xFF } else { input.mutation_byte };
            data[idx] ^= flip;
            data
        }
        1 => {
            // Truncate
            let len = (input.truncate_len as usize) % serialized.len();
            let len = len.max(1);
            serialized[..len].to_vec()
        }
        2 => {
            // Append garbage bytes
            let mut data = serialized.clone();
            let garbage: Vec<u8> = input.extra_bytes.iter()
                .take(256)
                .copied()
                .collect();
            data.extend_from_slice(&garbage);
            data
        }
        3 => {
            // Zero out a range
            let mut data = serialized.clone();
            let start = (input.mutation_offset as usize) % data.len();
            let zero_len = ((input.mutation_byte as usize) % 32 + 1).min(data.len() - start);
            for byte in &mut data[start..start + zero_len] {
                *byte = 0;
            }
            data
        }
        _ => unreachable!(),
    };

    // Attempt to deserialize the mutated data
    // Safety: deserialize requires trust that the bytes came from a compatible engine.
    // We intentionally pass corrupted data to verify it doesn't crash.
    let _ = unsafe { Module::deserialize(&engine, &mutated) };
});
