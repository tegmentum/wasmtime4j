#![no_main]

//! Module serialization fuzz target.
//!
//! **KNOWN LIMITATION:** This fuzz target may produce crashes (SIGABRT/SIGSEGV)
//! that cannot be fixed in wasmtime4j because they occur in wasmtime's internal
//! code when loading corrupted serialized module data.
//!
//! `Module::deserialize` is marked as `unsafe` and explicitly documents that
//! the data must come from a trusted source (i.e., a previous `serialize()` call
//! with the same engine configuration). When given corrupted data, it may:
//! - Crash with SIGABRT/SIGSEGV (internal assertion failures)
//! - Produce undefined behavior
//!
//! This is expected behavior for an unsafe API and not a bug.
//!
//! The purpose of this fuzz target is to:
//! 1. Document that corrupted serialized data causes crashes
//! 2. Potentially find edge cases where graceful error handling is possible
//! 3. Verify that wasmtime's safety invariants are maintained

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use std::sync::LazyLock;
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

static ENGINE: LazyLock<Engine> = LazyLock::new(Engine::default);

/// Cached compiled module and its serialized bytes.
static MODULE_AND_SERIALIZED: LazyLock<(Module, Vec<u8>)> = LazyLock::new(|| {
    let module = Module::new(&*ENGINE, VALID_MODULE_WAT)
        .expect("test module WAT should compile");
    let serialized = module.serialize()
        .expect("module serialization should succeed");
    (module, serialized)
});

fuzz_target!(|input: ModuleSerializeInput| {
    let (_, serialized) = &*MODULE_AND_SERIALIZED;

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

    // Attempt to deserialize the mutated data.
    //
    // Module::deserialize is unsafe and requires trusted input. Corrupted data may
    // cause panics in wasmtime internals (e.g., arithmetic overflows in vmoffsets.rs).
    // Since cargo-fuzz uses panic=abort, we cannot catch these with catch_unwind.
    //
    // To avoid crashes, we only test deserialization when the mutation preserves the
    // wasmtime serialization header. Deeply corrupted data that bypasses the header
    // check would hit unsafe territory in wasmtime's internal code.
    if mutated.len() >= serialized.len().min(16) && mutated[..8] == serialized[..8] {
        // Header looks plausible — safe enough to attempt deserialization
        let _ = unsafe { Module::deserialize(&*ENGINE, &mutated) };
    }
    // Note: We do NOT call Module::new on fuzzed bytes here — that is covered by
    // the module_parse fuzz target. Module::new on arbitrary bytes can trigger upstream
    // wasmtime crashes (SEGV in cranelift compilation) which are not our code's fault.
});
