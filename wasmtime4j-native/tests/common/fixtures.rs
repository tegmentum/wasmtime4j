//! Common WAT fixtures for wasmtime4j-native tests.
//!
//! This module provides reusable WAT code snippets and module generators
//! for various test scenarios.

/// Minimal valid WASM module (8 bytes magic header only).
pub const MINIMAL_MODULE: &[u8] = &[0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00];

/// Empty module with no exports.
pub const EMPTY_MODULE_WAT: &str = "(module)";

/// Simple module with a no-op function.
pub const NOP_MODULE_WAT: &str = r#"(module (func (export "nop")))"#;

/// Module with basic integer arithmetic functions.
pub const ARITHMETIC_MODULE_WAT: &str = r#"
(module
    (func (export "add_i32") (param i32 i32) (result i32)
        local.get 0
        local.get 1
        i32.add)

    (func (export "sub_i32") (param i32 i32) (result i32)
        local.get 0
        local.get 1
        i32.sub)

    (func (export "mul_i32") (param i32 i32) (result i32)
        local.get 0
        local.get 1
        i32.mul)

    (func (export "div_i32") (param i32 i32) (result i32)
        local.get 0
        local.get 1
        i32.div_s)

    (func (export "add_i64") (param i64 i64) (result i64)
        local.get 0
        local.get 1
        i64.add)
)
"#;

/// Module with floating point operations.
pub const FLOAT_MODULE_WAT: &str = r#"
(module
    (func (export "add_f32") (param f32 f32) (result f32)
        local.get 0
        local.get 1
        f32.add)

    (func (export "add_f64") (param f64 f64) (result f64)
        local.get 0
        local.get 1
        f64.add)

    (func (export "sqrt_f64") (param f64) (result f64)
        local.get 0
        f64.sqrt)
)
"#;

/// Module with memory operations.
pub const MEMORY_MODULE_WAT: &str = r#"
(module
    (memory (export "memory") 1)

    (func (export "store_i32") (param i32 i32)
        local.get 0
        local.get 1
        i32.store)

    (func (export "load_i32") (param i32) (result i32)
        local.get 0
        i32.load)

    (func (export "memory_size") (result i32)
        memory.size)

    (func (export "memory_grow") (param i32) (result i32)
        local.get 0
        memory.grow)
)
"#;

/// Module with global variables.
pub const GLOBALS_MODULE_WAT: &str = r#"
(module
    (global $counter (mut i32) (i32.const 0))
    (global $const_val i32 (i32.const 42))

    (func (export "get_counter") (result i32)
        global.get $counter)

    (func (export "set_counter") (param i32)
        local.get 0
        global.set $counter)

    (func (export "increment_counter") (result i32)
        global.get $counter
        i32.const 1
        i32.add
        global.set $counter
        global.get $counter)

    (func (export "get_const") (result i32)
        global.get $const_val)
)
"#;

/// Module that traps on division by zero.
pub const TRAP_MODULE_WAT: &str = r#"
(module
    (func (export "div_by_zero") (result i32)
        i32.const 10
        i32.const 0
        i32.div_s)

    (func (export "unreachable_trap")
        unreachable)

    (func (export "safe_div") (param i32 i32) (result i32)
        local.get 0
        local.get 1
        i32.div_s)
)
"#;

/// Module with table and indirect calls.
pub const TABLE_MODULE_WAT: &str = r#"
(module
    (type $i32_to_i32 (func (param i32) (result i32)))

    (table (export "table") 2 funcref)

    (func $double (param i32) (result i32)
        local.get 0
        i32.const 2
        i32.mul)

    (func $triple (param i32) (result i32)
        local.get 0
        i32.const 3
        i32.mul)

    (elem (i32.const 0) $double $triple)

    (func (export "call_indirect") (param i32 i32) (result i32)
        local.get 0
        local.get 1
        call_indirect (type $i32_to_i32))
)
"#;

/// Module with multiple return values.
pub const MULTI_VALUE_MODULE_WAT: &str = r#"
(module
    (func (export "swap") (param i32 i32) (result i32 i32)
        local.get 1
        local.get 0)

    (func (export "divmod") (param i32 i32) (result i32 i32)
        local.get 0
        local.get 1
        i32.div_s
        local.get 0
        local.get 1
        i32.rem_s)

    (func (export "triple_return") (result i32 i64 f32)
        i32.const 1
        i64.const 2
        f32.const 3.0)
)
"#;

/// Module with control flow (loops and branches).
pub const CONTROL_FLOW_MODULE_WAT: &str = r#"
(module
    (func (export "factorial") (param i64) (result i64)
        (local $result i64)
        (local.set $result (i64.const 1))
        (block $done
            (loop $loop
                (br_if $done (i64.le_s (local.get 0) (i64.const 1)))
                (local.set $result (i64.mul (local.get $result) (local.get 0)))
                (local.set 0 (i64.sub (local.get 0) (i64.const 1)))
                (br $loop)))
        (local.get $result))

    (func (export "fibonacci") (param i32) (result i32)
        (if (result i32) (i32.le_s (local.get 0) (i32.const 1))
            (then (local.get 0))
            (else
                (i32.add
                    (call 1 (i32.sub (local.get 0) (i32.const 1)))
                    (call 1 (i32.sub (local.get 0) (i32.const 2)))))))

    (func (export "loop_sum") (param i32) (result i32)
        (local $sum i32)
        (local $i i32)
        (local.set $sum (i32.const 0))
        (local.set $i (i32.const 0))
        (block $break
            (loop $continue
                (br_if $break (i32.ge_s (local.get $i) (local.get 0)))
                (local.set $sum (i32.add (local.get $sum) (local.get $i)))
                (local.set $i (i32.add (local.get $i) (i32.const 1)))
                (br $continue)))
        (local.get $sum))
)
"#;

/// Module requiring an import.
pub const IMPORT_MODULE_WAT: &str = r#"
(module
    (import "env" "external_func" (func $external (param i32) (result i32)))

    (func (export "call_external") (param i32) (result i32)
        local.get 0
        call $external)
)
"#;

/// Module with an infinite loop (for testing fuel/timeout).
pub const INFINITE_LOOP_WAT: &str = r#"
(module
    (func (export "infinite_loop")
        (loop $forever
            (br $forever)))

    (func (export "busy_loop") (param i32) (result i32)
        (local $i i32)
        (local.set $i (i32.const 0))
        (block $break
            (loop $continue
                (br_if $break (i32.ge_s (local.get $i) (local.get 0)))
                (local.set $i (i32.add (local.get $i) (i32.const 1)))
                (br $continue)))
        (local.get $i))
)
"#;

/// Generate a module with N exported functions.
pub fn generate_many_exports(count: usize) -> String {
    let mut wat = String::from("(module\n");
    for i in 0..count {
        wat.push_str(&format!(
            "    (func (export \"func_{}\") (result i32) i32.const {})\n",
            i, i
        ));
    }
    wat.push(')');
    wat
}

/// Generate a module with specified memory size (in pages).
pub fn generate_memory_module(initial_pages: u32, max_pages: Option<u32>) -> String {
    let memory_decl = match max_pages {
        Some(max) => format!("(memory (export \"memory\") {} {})", initial_pages, max),
        None => format!("(memory (export \"memory\") {})", initial_pages),
    };
    format!(
        r#"(module
    {}
    (func (export "size") (result i32) memory.size)
    (func (export "grow") (param i32) (result i32) local.get 0 memory.grow)
)"#,
        memory_decl
    )
}

/// Generate a module with multiple globals.
pub fn generate_globals_module(mut_count: usize, immut_count: usize) -> String {
    let mut wat = String::from("(module\n");

    for i in 0..mut_count {
        wat.push_str(&format!(
            "    (global $mut_{} (mut i32) (i32.const {}))\n",
            i, i
        ));
        wat.push_str(&format!(
            "    (func (export \"get_mut_{}\") (result i32) global.get $mut_{})\n",
            i, i
        ));
        wat.push_str(&format!(
            "    (func (export \"set_mut_{}\") (param i32) local.get 0 global.set $mut_{})\n",
            i, i
        ));
    }

    for i in 0..immut_count {
        wat.push_str(&format!(
            "    (global $const_{} i32 (i32.const {}))\n",
            i, i + 100
        ));
        wat.push_str(&format!(
            "    (func (export \"get_const_{}\") (result i32) global.get $const_{})\n",
            i, i
        ));
    }

    wat.push(')');
    wat
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_minimal_module_is_valid() {
        // Should be 8 bytes (magic + version)
        assert_eq!(MINIMAL_MODULE.len(), 8);
        // Check magic number
        assert_eq!(&MINIMAL_MODULE[0..4], b"\0asm");
    }

    #[test]
    fn test_generate_many_exports() {
        let wat = generate_many_exports(3);
        assert!(wat.contains("func_0"));
        assert!(wat.contains("func_1"));
        assert!(wat.contains("func_2"));
    }

    #[test]
    fn test_generate_memory_module() {
        let wat = generate_memory_module(1, Some(10));
        assert!(wat.contains("memory"));
        assert!(wat.contains("1"));
        assert!(wat.contains("10"));
    }

    #[test]
    fn test_generate_globals_module() {
        let wat = generate_globals_module(2, 3);
        assert!(wat.contains("mut_0"));
        assert!(wat.contains("mut_1"));
        assert!(wat.contains("const_0"));
        assert!(wat.contains("const_1"));
        assert!(wat.contains("const_2"));
    }
}
