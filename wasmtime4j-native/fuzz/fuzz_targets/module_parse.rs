//! Fuzz target for WebAssembly module parsing.
//!
//! This target tests the robustness of WASM/WAT parsing by feeding arbitrary
//! byte sequences to the parser. It aims to discover:
//! - Malformed magic bytes handling
//! - Invalid section length handling
//! - Type index out of bounds
//! - Circular type references
//! - Maximum limits exceeded
//! - Memory safety issues in parsing code

#![no_main]

use libfuzzer_sys::fuzz_target;
use std::sync::LazyLock;
use wasmtime::{Engine, Module};

static ENGINE: LazyLock<Engine> = LazyLock::new(Engine::default);

fuzz_target!(|data: &[u8]| {
    // Test 1: Parse arbitrary bytes as WASM binary
    // This should never panic - only return Ok/Err
    let _ = Module::new(&*ENGINE, data);

    // Test 2: If data is valid UTF-8, also try parsing as WAT text format
    if let Ok(text) = std::str::from_utf8(data) {
        // WAT parsing should also gracefully handle malformed input
        let _ = wat::parse_str(text);
    }

    // Test 3: Try to validate the module without instantiation
    // This exercises the validation paths separately
    let _ = Module::validate(&*ENGINE, data);
});
