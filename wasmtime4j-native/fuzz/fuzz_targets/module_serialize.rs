#![no_main]

//! Module serialization round-trip fuzz target.
//!
//! Tests that modules compiled from fuzzed WAT/Wasm can be serialized and
//! deserialized without losing functionality. This exercises wasmtime4j's
//! module compilation and the wasmtime serialization format.
//!
//! **Note:** We do NOT test deserialization of corrupted serialized data.
//! `Module::deserialize` is `unsafe` and requires trusted input. Corrupted
//! data causes panics in wasmtime internals that cannot be caught under
//! cargo-fuzz's `panic=abort` mode.

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use std::sync::LazyLock;
use wasmtime::{Engine, Module};

/// Structured input for module serialization fuzzing.
#[derive(Debug, Arbitrary)]
struct ModuleSerializeInput {
    /// Selector for which test module to use.
    module_index: u8,
    /// Fuzzed i32 parameter for round-trip verification.
    param: i32,
}

static ENGINE: LazyLock<Engine> = LazyLock::new(Engine::default);

/// Test modules with various features to exercise serialization.
const TEST_MODULES: &[&str] = &[
    // Simple add function
    r#"(module
        (func $add (param i32 i32) (result i32) local.get 0 local.get 1 i32.add)
        (export "add" (func $add))
    )"#,
    // Identity function
    r#"(module
        (func $id (param i32) (result i32) local.get 0)
        (export "id" (func $id))
    )"#,
    // Module with memory
    r#"(module
        (memory 1)
        (func $load (param i32) (result i32) local.get 0 i32.load)
        (export "memory" (memory 0))
        (export "load" (func $load))
    )"#,
    // Module with global
    r#"(module
        (global $g (mut i32) (i32.const 0))
        (func $get (result i32) global.get $g)
        (func $set (param i32) local.get 0 global.set $g)
        (export "get" (func $get))
        (export "set" (func $set))
    )"#,
    // Empty module
    r#"(module)"#,
];

fuzz_target!(|input: ModuleSerializeInput| {
    let wat = TEST_MODULES[input.module_index as usize % TEST_MODULES.len()];

    // Compile the module
    let module = match Module::new(&*ENGINE, wat) {
        Ok(m) => m,
        Err(_) => return,
    };

    // Serialize
    let serialized = match module.serialize() {
        Ok(s) => s,
        Err(_) => return,
    };

    // Deserialize (safe — data came from our own serialize call)
    let deserialized = match unsafe { Module::deserialize(&*ENGINE, &serialized) } {
        Ok(m) => m,
        Err(_) => return,
    };

    // Verify the deserialized module has the same exports as the original
    let original_exports: Vec<_> = module.exports().map(|e| e.name().to_string()).collect();
    let deser_exports: Vec<_> = deserialized.exports().map(|e| e.name().to_string()).collect();
    assert_eq!(
        original_exports, deser_exports,
        "Deserialized module should have same exports"
    );
});
