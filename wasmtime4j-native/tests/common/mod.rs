//! Common test utilities for wasmtime4j-native integration tests.
//!
//! This module provides reusable test infrastructure including:
//! - TestEngine: A wrapper for Engine/Store lifecycle management
//! - compile_wat! macro: Convenient inline WAT compilation
//! - assert_wasm_trap! macro: Trap assertion helper
//! - Common test fixtures and helpers

pub mod assertions;
pub mod fixtures;

use wasmtime4j::engine::Engine;
use wasmtime4j::store::Store;
use wasmtime4j::module::Module;
use wasmtime4j::instance::Instance;
use wasmtime4j::error::WasmtimeError;

/// A test harness that manages Engine, Store, and optional Module lifecycle.
///
/// Provides convenient setup/teardown for integration tests while ensuring
/// proper resource cleanup.
pub struct TestEngine {
    engine: Engine,
    store: Option<Store>,
}

impl TestEngine {
    /// Create a new test engine with default configuration.
    pub fn new() -> Result<Self, WasmtimeError> {
        let engine = Engine::new()?;
        Ok(Self {
            engine,
            store: None,
        })
    }

    /// Create a new test engine with custom configuration.
    pub fn with_config(config_fn: impl FnOnce(&mut wasmtime::Config)) -> Result<Self, WasmtimeError> {
        let mut config = wasmtime::Config::new();
        config_fn(&mut config);
        let engine = Engine::with_config(config)?;
        Ok(Self {
            engine,
            store: None,
        })
    }

    /// Get a reference to the underlying engine.
    pub fn engine(&self) -> &Engine {
        &self.engine
    }

    /// Get or create a store for this engine.
    pub fn store(&mut self) -> Result<&mut Store, WasmtimeError> {
        if self.store.is_none() {
            self.store = Some(Store::new(&self.engine)?);
        }
        Ok(self.store.as_mut().unwrap())
    }

    /// Create a new store (useful when you need multiple stores).
    pub fn new_store(&self) -> Result<Store, WasmtimeError> {
        Store::new(&self.engine)
    }

    /// Create a store with fuel enabled.
    pub fn store_with_fuel(&mut self, fuel: u64) -> Result<&mut Store, WasmtimeError> {
        self.store = Some(Store::builder()
            .fuel_limit(fuel)
            .build(&self.engine)?);
        Ok(self.store.as_mut().unwrap())
    }

    /// Compile a WAT string into a module.
    pub fn compile_wat(&self, wat: &str) -> Result<Module, WasmtimeError> {
        Module::compile_wat(&self.engine, wat)
    }

    /// Compile WASM bytes into a module.
    pub fn compile_wasm(&self, wasm: &[u8]) -> Result<Module, WasmtimeError> {
        Module::compile(&self.engine, wasm)
    }

    /// Instantiate a module with no imports.
    pub fn instantiate(&mut self, module: &Module) -> Result<Instance, WasmtimeError> {
        let store = self.store()?;
        Instance::new_without_imports(store, module)
    }

    /// Reset the store (create a fresh one).
    pub fn reset_store(&mut self) -> Result<(), WasmtimeError> {
        self.store = Some(Store::new(&self.engine)?);
        Ok(())
    }
}

impl Default for TestEngine {
    fn default() -> Self {
        Self::new().expect("Failed to create default TestEngine")
    }
}

/// Macro for compiling inline WAT code in tests.
///
/// # Example
/// ```ignore
/// let module = compile_wat!(engine, r#"
///     (module
///         (func (export "add") (param i32 i32) (result i32)
///             local.get 0
///             local.get 1
///             i32.add))
/// "#);
/// ```
#[macro_export]
macro_rules! compile_wat {
    ($engine:expr, $wat:expr) => {
        wasmtime4j::module::Module::compile_wat($engine, $wat)
            .expect("Failed to compile WAT")
    };
}

/// Macro for asserting that a WASM operation traps.
///
/// # Example
/// ```ignore
/// assert_wasm_trap!(instance.call_export_function(&mut store, "div_by_zero", &[]));
/// ```
#[macro_export]
macro_rules! assert_wasm_trap {
    ($expr:expr) => {
        match $expr {
            Err(e) => {
                let error_str = e.to_string().to_lowercase();
                assert!(
                    error_str.contains("trap") || error_str.contains("wasm trap"),
                    "Expected a trap error, got: {:?}",
                    e
                );
            }
            Ok(result) => panic!("Expected a trap, but got success: {:?}", result),
        }
    };
    ($expr:expr, $trap_msg:expr) => {
        match $expr {
            Err(e) => {
                let error_str = e.to_string().to_lowercase();
                assert!(
                    error_str.contains($trap_msg.to_lowercase().as_str()),
                    "Expected trap containing '{}', got: {:?}",
                    $trap_msg,
                    e
                );
            }
            Ok(result) => panic!("Expected a trap containing '{}', but got success: {:?}", $trap_msg, result),
        }
    };
}

/// Macro for asserting WASM value equality with type checking.
#[macro_export]
macro_rules! assert_wasm_value {
    ($value:expr, i32, $expected:expr) => {
        match $value {
            wasmtime4j::instance::WasmValue::I32(v) => {
                assert_eq!(v, $expected, "I32 value mismatch");
            }
            other => panic!("Expected I32, got: {:?}", other),
        }
    };
    ($value:expr, i64, $expected:expr) => {
        match $value {
            wasmtime4j::instance::WasmValue::I64(v) => {
                assert_eq!(v, $expected, "I64 value mismatch");
            }
            other => panic!("Expected I64, got: {:?}", other),
        }
    };
    ($value:expr, f32, $expected:expr) => {
        match $value {
            wasmtime4j::instance::WasmValue::F32(v) => {
                assert!((v - $expected).abs() < f32::EPSILON, "F32 value mismatch: {} != {}", v, $expected);
            }
            other => panic!("Expected F32, got: {:?}", other),
        }
    };
    ($value:expr, f64, $expected:expr) => {
        match $value {
            wasmtime4j::instance::WasmValue::F64(v) => {
                assert!((v - $expected).abs() < f64::EPSILON, "F64 value mismatch: {} != {}", v, $expected);
            }
            other => panic!("Expected F64, got: {:?}", other),
        }
    };
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_engine_creates_successfully() {
        let engine = TestEngine::new();
        assert!(engine.is_ok());
    }

    #[test]
    fn test_ref_null_none_heap_type() {
        // Enable GC for this test
        let mut config = wasmtime::Config::new();
        config.wasm_gc(true);
        config.wasm_function_references(true);

        let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();

        // WAT module with (ref null none) table element type
        let wat = r#"(module
          (table $t 10 (ref null none))
          (func (export "f") (result (ref null none))
            (i32.const 99)
            (table.get $t)
          )
        )"#;

        let module = wasmtime::Module::new(&wasmtime_engine, wat).unwrap();

        // Check the table export types
        for export in module.exports() {
            println!("Export: {:?}", export.name());
            match export.ty() {
                wasmtime::ExternType::Func(func_type) => {
                    println!("  Function type:");
                    for (i, result) in func_type.results().enumerate() {
                        println!("    Result {}: {:?}", i, result);
                        if let wasmtime::ValType::Ref(ref_type) = result {
                            println!("      RefType: {:?}", ref_type);
                            println!("      HeapType: {:?}", ref_type.heap_type());
                            println!("      Is HeapType::None: {}", matches!(ref_type.heap_type(), wasmtime::HeapType::None));
                        }
                    }
                }
                wasmtime::ExternType::Table(table_type) => {
                    println!("  Table type:");
                    println!("    Element: {:?}", table_type.element());
                    println!("    HeapType: {:?}", table_type.element().heap_type());
                    println!("    Is HeapType::None: {}", matches!(table_type.element().heap_type(), wasmtime::HeapType::None));
                }
                _ => {}
            }
        }
    }

    #[test]
    fn test_store_creation() {
        let mut engine = TestEngine::new().unwrap();
        let store = engine.store();
        assert!(store.is_ok());
    }

    #[test]
    fn test_compile_simple_wat() {
        let engine = TestEngine::new().unwrap();
        let wat = r#"(module (func (export "nop")))"#;
        let result = engine.compile_wat(wat);
        assert!(result.is_ok());
    }

    #[test]
    fn test_store_with_fuel() {
        let mut engine = TestEngine::new().unwrap();
        let result = engine.store_with_fuel(1000);
        assert!(result.is_ok());
    }
}
