//! Fuzz target for WebAssembly function call operations.
//!
//! This target tests the robustness of function invocation by:
//! - Creating modules with various function signatures
//! - Calling functions with various argument types/values
//! - Testing type mismatches
//!
//! Aims to discover:
//! - Type mismatch handling
//! - Parameter validation issues
//! - Return value handling
//! - Trap handling
//! - Memory safety issues in function call code

#![no_main]

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use std::sync::LazyLock;
use wasmtime::{Engine, Func, FuncType, Instance, Module, Store, Val, ValType};

/// Structured input for function call fuzzing
#[derive(Debug, Arbitrary)]
struct FuncCallInput {
    /// Which test function to call (0-3)
    func_index: u8,
    /// Raw argument data
    args: Vec<u8>,
    /// Number of i32 args to use
    i32_count: u8,
    /// Number of i64 args to use
    i64_count: u8,
    /// Number of f32 args to use
    f32_count: u8,
    /// Number of f64 args to use
    f64_count: u8,
}

/// Minimal WASM module with test functions:
/// - func 0: (i32) -> i32 (identity)
/// - func 1: (i64) -> i64 (identity)
/// - func 2: (i32, i32) -> i32 (add)
/// - func 3: () -> i32 (constant)
const TEST_MODULE_WAT: &str = r#"
(module
    (func $identity_i32 (param i32) (result i32)
        local.get 0)
    (func $identity_i64 (param i64) (result i64)
        local.get 0)
    (func $add (param i32 i32) (result i32)
        local.get 0
        local.get 1
        i32.add)
    (func $const (result i32)
        i32.const 42)
    (export "identity_i32" (func $identity_i32))
    (export "identity_i64" (func $identity_i64))
    (export "add" (func $add))
    (export "const" (func $const))
)
"#;

static ENGINE: LazyLock<Engine> = LazyLock::new(Engine::default);
static MODULE: LazyLock<Module> = LazyLock::new(|| {
    Module::new(&*ENGINE, TEST_MODULE_WAT).expect("test module WAT should compile")
});

fuzz_target!(|input: FuncCallInput| {
    let mut store = Store::new(&*ENGINE, ());

    // Instantiate from cached module
    let instance = match Instance::new(&mut store, &*MODULE, &[]) {
        Ok(i) => i,
        Err(_) => return,
    };

    // Get test functions
    let funcs = [
        instance.get_func(&mut store, "identity_i32"),
        instance.get_func(&mut store, "identity_i64"),
        instance.get_func(&mut store, "add"),
        instance.get_func(&mut store, "const"),
    ];

    let func_index = (input.func_index % 4) as usize;
    if let Some(func) = &funcs[func_index] {
        // Build arguments from fuzz input
        let args = build_args(&input);

        // Call with potentially wrong arguments - should return error, not panic
        let mut results = vec![Val::I32(0); 4]; // Over-allocate results
        let _ = func.call(&mut store, &args, &mut results);
    }

    // Also test type mismatches with host functions
    test_host_function_calls(&mut store, &input);
});

fn build_args(input: &FuncCallInput) -> Vec<Val> {
    let mut args = Vec::new();
    let mut offset = 0;

    // Add i32 values
    for _ in 0..(input.i32_count % 8) {
        let val = if offset + 4 <= input.args.len() {
            let bytes: [u8; 4] = input.args[offset..offset + 4].try_into().unwrap_or([0; 4]);
            i32::from_le_bytes(bytes)
        } else {
            0
        };
        args.push(Val::I32(val));
        offset += 4;
    }

    // Add i64 values
    for _ in 0..(input.i64_count % 8) {
        let val = if offset + 8 <= input.args.len() {
            let bytes: [u8; 8] = input.args[offset..offset + 8].try_into().unwrap_or([0; 8]);
            i64::from_le_bytes(bytes)
        } else {
            0
        };
        args.push(Val::I64(val));
        offset += 8;
    }

    // Add f32 values
    for _ in 0..(input.f32_count % 8) {
        let val = if offset + 4 <= input.args.len() {
            let bytes: [u8; 4] = input.args[offset..offset + 4].try_into().unwrap_or([0; 4]);
            f32::from_le_bytes(bytes)
        } else {
            0.0
        };
        args.push(Val::F32(val.to_bits()));
        offset += 4;
    }

    // Add f64 values
    for _ in 0..(input.f64_count % 8) {
        let val = if offset + 8 <= input.args.len() {
            let bytes: [u8; 8] = input.args[offset..offset + 8].try_into().unwrap_or([0; 8]);
            f64::from_le_bytes(bytes)
        } else {
            0.0
        };
        args.push(Val::F64(val.to_bits()));
        offset += 8;
    }

    args
}

fn test_host_function_calls<T>(store: &mut Store<T>, input: &FuncCallInput) {
    // Create host functions with various signatures
    let func_type = FuncType::new(
        store.engine(),
        vec![ValType::I32, ValType::I32],
        vec![ValType::I32],
    );

    // Create a host function
    let host_func = Func::new(&mut *store, func_type, |_caller, params, results| {
        // Simple addition
        if let (Some(Val::I32(a)), Some(Val::I32(b))) = (params.first(), params.get(1)) {
            results[0] = Val::I32(a.wrapping_add(*b));
        }
        Ok(())
    });

    // Try calling with fuzzed arguments
    let args = build_args(input);
    let mut results = vec![Val::I32(0)];
    let _ = host_func.call(&mut *store, &args, &mut results);
}
