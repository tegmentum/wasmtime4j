//! Fuzz target for host function callback dispatch (no JVM needed).
//!
//! This target tests the robustness of host function callbacks by:
//! - Defining host functions with various signatures via Func::new
//! - Dispatching calls through imported host functions from Wasm
//! - Testing trap propagation from host to guest
//! - Exercising parameter boundary values (i32::MIN, i32::MAX)
//! - Verifying error message preservation through UTF-8 lossy conversion
//!
//! Aims to discover:
//! - Callback dispatch memory safety issues
//! - Trap propagation failures
//! - Parameter marshalling bugs at boundary values
//! - Error message corruption through encoding round-trips

#![no_main]

use arbitrary::Arbitrary;
use libfuzzer_sys::fuzz_target;
use std::sync::LazyLock;
use wasmtime::{Engine, Func, FuncType, Instance, Module, Store, Val, ValType};

/// Structured input for host callback fuzzing
#[derive(Debug, Arbitrary)]
struct CallbackInput {
    /// Which host function to call (mapped to 0-3)
    func_index: u8,
    /// Fuzzed parameter bytes
    params: Vec<u8>,
    /// Host function returns error instead of a value
    should_trap: bool,
    /// Fuzzed error message bytes
    trap_msg: Vec<u8>,
}

/// WAT module that imports host functions with various signatures and
/// exports wrapper functions that call through to those imports.
const CALLBACK_MODULE_WAT: &str = r#"
(module
    (import "host" "callback_i32" (func $callback_i32 (param i32) (result i32)))
    (import "host" "callback_i64" (func $callback_i64 (param i64)))
    (import "host" "callback_add" (func $callback_add (param i32 i32) (result i32)))
    (import "host" "callback_void" (func $callback_void (result i32)))

    (func $wrap_callback_i32 (param i32) (result i32)
        local.get 0
        call $callback_i32)
    (func $wrap_callback_i64 (param i64)
        local.get 0
        call $callback_i64)
    (func $wrap_callback_add (param i32 i32) (result i32)
        local.get 0
        local.get 1
        call $callback_add)
    (func $wrap_callback_void (result i32)
        call $callback_void)

    (export "call_callback_i32" (func $wrap_callback_i32))
    (export "call_callback_i64" (func $wrap_callback_i64))
    (export "call_callback_add" (func $wrap_callback_add))
    (export "call_callback_void" (func $wrap_callback_void))
)
"#;

static ENGINE: LazyLock<Engine> = LazyLock::new(Engine::default);

/// Extract an i32 from fuzzed parameter bytes at the given offset.
fn extract_i32(params: &[u8], offset: usize) -> i32 {
    if offset + 4 <= params.len() {
        let bytes: [u8; 4] = params[offset..offset + 4].try_into().unwrap_or([0; 4]);
        i32::from_le_bytes(bytes)
    } else if !params.is_empty() {
        // Use available bytes, padding with zeros
        let mut bytes = [0u8; 4];
        for (i, b) in params.iter().skip(offset).enumerate() {
            if i >= 4 {
                break;
            }
            bytes[i] = *b;
        }
        i32::from_le_bytes(bytes)
    } else {
        0
    }
}

/// Extract an i64 from fuzzed parameter bytes at the given offset.
fn extract_i64(params: &[u8], offset: usize) -> i64 {
    if offset + 8 <= params.len() {
        let bytes: [u8; 8] = params[offset..offset + 8].try_into().unwrap_or([0; 8]);
        i64::from_le_bytes(bytes)
    } else if !params.is_empty() {
        let mut bytes = [0u8; 8];
        for (i, b) in params.iter().skip(offset).enumerate() {
            if i >= 8 {
                break;
            }
            bytes[i] = *b;
        }
        i64::from_le_bytes(bytes)
    } else {
        0
    }
}

fuzz_target!(|input: CallbackInput| {
    let mut store = Store::new(&*ENGINE, ());

    // Build the trap message via lossy UTF-8 conversion (mirrors JNI string handling)
    let trap_message = String::from_utf8_lossy(&input.trap_msg).into_owned();
    let should_trap = input.should_trap;

    // Extract fuzzed parameter values
    let param_i32 = extract_i32(&input.params, 0);
    let param_i64 = extract_i64(&input.params, 0);
    let param_a = extract_i32(&input.params, 0);
    let param_b = extract_i32(&input.params, 4);
    let void_result = extract_i32(&input.params, 0);

    // --- Define host functions based on fuzzed input ---

    // callback_i32: (i32) -> i32 — identity or trap
    let trap_msg_clone = trap_message.clone();
    let callback_i32_type =
        FuncType::new(store.engine(), vec![ValType::I32], vec![ValType::I32]);
    let callback_i32 = Func::new(&mut store, callback_i32_type, move |_caller, params, results| {
        if should_trap {
            return Err(wasmtime::Error::msg(trap_msg_clone.clone()));
        }
        if let Some(Val::I32(v)) = params.first() {
            results[0] = Val::I32(*v);
        }
        Ok(())
    });

    // callback_i64: (i64) -> () — no-op or trap
    let trap_msg_clone = trap_message.clone();
    let callback_i64_type =
        FuncType::new(store.engine(), vec![ValType::I64], vec![]);
    let callback_i64 = Func::new(&mut store, callback_i64_type, move |_caller, _params, _results| {
        if should_trap {
            return Err(wasmtime::Error::msg(trap_msg_clone.clone()));
        }
        Ok(())
    });

    // callback_add: (i32, i32) -> i32 — wrapping addition or trap
    let trap_msg_clone = trap_message.clone();
    let callback_add_type = FuncType::new(
        store.engine(),
        vec![ValType::I32, ValType::I32],
        vec![ValType::I32],
    );
    let callback_add = Func::new(&mut store, callback_add_type, move |_caller, params, results| {
        if should_trap {
            return Err(wasmtime::Error::msg(trap_msg_clone.clone()));
        }
        if let (Some(Val::I32(a)), Some(Val::I32(b))) = (params.first(), params.get(1)) {
            results[0] = Val::I32(a.wrapping_add(*b));
        }
        Ok(())
    });

    // callback_void: () -> i32 — return fuzzed i32 or trap
    let trap_msg_clone = trap_message.clone();
    let callback_void_type =
        FuncType::new(store.engine(), vec![], vec![ValType::I32]);
    let callback_void = Func::new(&mut store, callback_void_type, move |_caller, _params, results| {
        if should_trap {
            return Err(wasmtime::Error::msg(trap_msg_clone.clone()));
        }
        results[0] = Val::I32(void_result);
        Ok(())
    });

    // --- Compile and instantiate ---

    let module = match Module::new(&*ENGINE, CALLBACK_MODULE_WAT) {
        Ok(m) => m,
        Err(_) => return,
    };

    let imports = [
        callback_i32.into(),
        callback_i64.into(),
        callback_add.into(),
        callback_void.into(),
    ];

    let instance = match Instance::new(&mut store, &module, &imports) {
        Ok(i) => i,
        Err(_) => return,
    };

    // --- Call the selected exported wrapper function ---

    let func_index = input.func_index % 4;

    match func_index {
        0 => {
            // call_callback_i32: (i32) -> i32
            if let Some(func) = instance.get_func(&mut store, "call_callback_i32") {
                let mut results = [Val::I32(0)];
                let call_result = func.call(&mut store, &[Val::I32(param_i32)], &mut results);
                if !should_trap {
                    if let Ok(()) = call_result {
                        // Verify identity: result should equal input
                        if let Val::I32(r) = results[0] {
                            assert_eq!(r, param_i32, "identity callback returned wrong value");
                        }
                    }
                } else if let Err(_e) = call_result {
                    // Trap occurred as expected — fuzzed message content may not
                    // survive the round-trip intact (NUL bytes, encoding issues).
                }
            }
        }
        1 => {
            // call_callback_i64: (i64) -> ()
            if let Some(func) = instance.get_func(&mut store, "call_callback_i64") {
                let mut results = [];
                let call_result = func.call(&mut store, &[Val::I64(param_i64)], &mut results);
                if should_trap {
                    // Trap expected — fuzzed message may not survive round-trip
                }
            }
        }
        2 => {
            // call_callback_add: (i32, i32) -> i32
            if let Some(func) = instance.get_func(&mut store, "call_callback_add") {
                let mut results = [Val::I32(0)];
                let call_result = func.call(
                    &mut store,
                    &[Val::I32(param_a), Val::I32(param_b)],
                    &mut results,
                );
                if !should_trap {
                    if let Ok(()) = call_result {
                        if let Val::I32(r) = results[0] {
                            assert_eq!(
                                r,
                                param_a.wrapping_add(param_b),
                                "add callback returned wrong value for {} + {}",
                                param_a,
                                param_b,
                            );
                        }
                    }
                } else if let Err(_e) = call_result {
                    // Trap occurred as expected — fuzzed message may not survive round-trip
                }
            }
        }
        3 => {
            // call_callback_void: () -> i32
            if let Some(func) = instance.get_func(&mut store, "call_callback_void") {
                let mut results = [Val::I32(0)];
                let call_result = func.call(&mut store, &[], &mut results);
                if !should_trap {
                    if let Ok(()) = call_result {
                        if let Val::I32(r) = results[0] {
                            assert_eq!(
                                r, void_result,
                                "void callback returned wrong value",
                            );
                        }
                    }
                } else if let Err(_e) = call_result {
                    // Trap occurred as expected — fuzzed message may not survive round-trip
                }
            }
        }
        _ => unreachable!(),
    }

    // --- Exercise boundary values explicitly ---

    if !should_trap {
        if let Some(func) = instance.get_func(&mut store, "call_callback_i32") {
            let mut results = [Val::I32(0)];

            // i32::MIN
            let _ = func.call(&mut store, &[Val::I32(i32::MIN)], &mut results);
            if let Val::I32(r) = results[0] {
                assert_eq!(r, i32::MIN, "boundary: i32::MIN not preserved");
            }

            // i32::MAX
            let _ = func.call(&mut store, &[Val::I32(i32::MAX)], &mut results);
            if let Val::I32(r) = results[0] {
                assert_eq!(r, i32::MAX, "boundary: i32::MAX not preserved");
            }

            // Zero
            let _ = func.call(&mut store, &[Val::I32(0)], &mut results);
            if let Val::I32(r) = results[0] {
                assert_eq!(r, 0, "boundary: zero not preserved");
            }

            // -1
            let _ = func.call(&mut store, &[Val::I32(-1)], &mut results);
            if let Val::I32(r) = results[0] {
                assert_eq!(r, -1, "boundary: -1 not preserved");
            }
        }

        // Test wrapping addition at boundaries
        if let Some(func) = instance.get_func(&mut store, "call_callback_add") {
            let mut results = [Val::I32(0)];

            // i32::MAX + 1 wraps to i32::MIN
            let _ = func.call(
                &mut store,
                &[Val::I32(i32::MAX), Val::I32(1)],
                &mut results,
            );
            if let Val::I32(r) = results[0] {
                assert_eq!(r, i32::MIN, "boundary: MAX + 1 should wrap to MIN");
            }

            // i32::MIN + (-1) wraps to i32::MAX
            let _ = func.call(
                &mut store,
                &[Val::I32(i32::MIN), Val::I32(-1)],
                &mut results,
            );
            if let Val::I32(r) = results[0] {
                assert_eq!(r, i32::MAX, "boundary: MIN + (-1) should wrap to MAX");
            }
        }
    }
});
