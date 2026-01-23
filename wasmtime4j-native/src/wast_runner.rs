/// WAST test execution module
///
/// This module provides functionality to execute WAST (WebAssembly Test) files
/// using Wasmtime's native WAST parser and test runner. This ensures 100%
/// compatibility with Wasmtime's own test execution behavior.

use anyhow::{Context, Result};
use std::path::Path;
use wasmtime::*;
use wasmtime_wast::WastContext;

/// Result of executing a single WAST directive
#[derive(Debug, Clone)]
pub struct WastDirectiveResult {
    pub line_number: usize,
    pub passed: bool,
    pub error_message: Option<String>,
}

/// Results from executing a complete WAST file
#[derive(Debug, Clone)]
pub struct WastExecutionResult {
    pub file_path: String,
    pub total_directives: usize,
    pub passed_directives: usize,
    pub failed_directives: usize,
    pub directive_results: Vec<WastDirectiveResult>,
    pub execution_error: Option<String>,
}

impl WastExecutionResult {
    /// Check if all directives passed
    pub fn all_passed(&self) -> bool {
        self.execution_error.is_none() && self.failed_directives == 0
    }

    /// Get pass rate as percentage
    pub fn pass_rate(&self) -> f64 {
        if self.total_directives == 0 {
            return 100.0;
        }
        (self.passed_directives as f64 / self.total_directives as f64) * 100.0
    }
}

/// Execute a WAST file and return detailed results
pub fn execute_wast_file(file_path: &str) -> Result<WastExecutionResult> {
    let path = Path::new(file_path);

    if !path.exists() {
        return Ok(WastExecutionResult {
            file_path: file_path.to_string(),
            total_directives: 0,
            passed_directives: 0,
            failed_directives: 0,
            directive_results: vec![],
            execution_error: Some(format!("File not found: {}", file_path)),
        });
    }

    // Create Wasmtime engine with default configuration
    // Note: async_support is disabled for synchronous WAST execution
    let mut config = Config::new();
    config.wasm_multi_value(true);
    config.wasm_multi_memory(true);
    config.wasm_bulk_memory(true);
    config.wasm_reference_types(true);
    config.wasm_simd(true);
    config.wasm_relaxed_simd(true);
    config.wasm_threads(true);
    config.wasm_tail_call(true);
    config.wasm_function_references(true);
    config.wasm_gc(true);
    config.wasm_custom_page_sizes(true);
    config.wasm_component_model(true);
    config.wasm_exceptions(true);
    config.wasm_wide_arithmetic(true);

    // CRITICAL: Disable signal-based traps to avoid conflict with JVM signal handlers
    // JVM and Wasmtime both install SIGSEGV/SIGILL handlers which conflict
    // This forces explicit bounds checks instead of using signals
    config.signals_based_traps(false);

    // Configure stack size for proper overflow handling
    config.max_wasm_stack(2 * 1024 * 1024); // 2 MiB

    let engine = Engine::new(&config)
        .context("Failed to create Wasmtime engine")?;

    // Create WAST context for synchronous test execution
    let mut wast_context = WastContext::new(&engine, wasmtime_wast::Async::No, |store| {
        let _ = store;
    });

    // Register spectest infrastructure for both core and component model
    // This is optional - some tests may not need spectest functions
    if let Err(e) = wast_context.register_spectest(&wasmtime_wast::SpectestConfig {
        use_shared_memory: false,
        suppress_prints: true,
    }) {
        // Log but continue - spectest is optional for many tests
        eprintln!("Warning: Could not register spectest infrastructure: {}", e);
    }

    // Execute the WAST file
    match wast_context.run_file(path) {
        Ok(()) => {
            // All directives passed
            Ok(WastExecutionResult {
                file_path: file_path.to_string(),
                total_directives: 1, // We don't have access to individual directive count
                passed_directives: 1,
                failed_directives: 0,
                directive_results: vec![WastDirectiveResult {
                    line_number: 0,
                    passed: true,
                    error_message: None,
                }],
                execution_error: None,
            })
        }
        Err(e) => {
            // Execution failed
            Ok(WastExecutionResult {
                file_path: file_path.to_string(),
                total_directives: 1,
                passed_directives: 0,
                failed_directives: 1,
                directive_results: vec![WastDirectiveResult {
                    line_number: 0,
                    passed: false,
                    error_message: Some(format!("{:#}", e)),
                }],
                execution_error: Some(format!("{:#}", e)),
            })
        }
    }
}

/// Execute WAST content from a string buffer
pub fn execute_wast_buffer(filename: &str, content: &[u8]) -> Result<WastExecutionResult> {
    // Create Wasmtime engine with default configuration
    // Note: async_support is disabled for synchronous WAST execution
    let mut config = Config::new();
    config.wasm_multi_value(true);
    config.wasm_multi_memory(true);
    config.wasm_bulk_memory(true);
    config.wasm_reference_types(true);
    config.wasm_simd(true);
    config.wasm_relaxed_simd(true);
    config.wasm_threads(true);
    config.wasm_tail_call(true);
    config.wasm_function_references(true);
    config.wasm_gc(true);
    config.wasm_custom_page_sizes(true);
    config.wasm_component_model(true);
    config.wasm_exceptions(true);
    config.wasm_wide_arithmetic(true);

    // CRITICAL: Disable signal-based traps to avoid conflict with JVM signal handlers
    // JVM and Wasmtime both install SIGSEGV/SIGILL handlers which conflict
    // This forces explicit bounds checks instead of using signals
    config.signals_based_traps(false);

    // Configure stack size for proper overflow handling
    config.max_wasm_stack(2 * 1024 * 1024); // 2 MiB

    let engine = Engine::new(&config)
        .context("Failed to create Wasmtime engine")?;

    // Create WAST context for synchronous test execution
    let mut wast_context = WastContext::new(&engine, wasmtime_wast::Async::No, |store| {
        let _ = store;
    });

    // Register spectest infrastructure for both core and component model
    // This is optional - some tests may not need spectest functions
    if let Err(e) = wast_context.register_spectest(&wasmtime_wast::SpectestConfig {
        use_shared_memory: false,
        suppress_prints: true,
    }) {
        // Log but continue - spectest is optional for many tests
        eprintln!("Warning: Could not register spectest infrastructure: {}", e);
    }

    // Execute the WAST buffer using run_wast
    let result = match wast_context.run_wast(filename, content) {
        Ok(()) => {
            // All directives passed
            WastExecutionResult {
                file_path: filename.to_string(),
                total_directives: 1,
                passed_directives: 1,
                failed_directives: 0,
                directive_results: vec![WastDirectiveResult {
                    line_number: 0,
                    passed: true,
                    error_message: None,
                }],
                execution_error: None,
            }
        }
        Err(e) => {
            // Execution failed
            WastExecutionResult {
                file_path: filename.to_string(),
                total_directives: 1,
                passed_directives: 0,
                failed_directives: 1,
                directive_results: vec![WastDirectiveResult {
                    line_number: 0,
                    passed: false,
                    error_message: Some(format!("{:#}", e)),
                }],
                execution_error: Some(format!("{:#}", e)),
            }
        }
    };

    Ok(result)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_execute_simple_wast() {
        let wast = br#"
            (module
                (func (export "add") (param i32 i32) (result i32)
                    local.get 0
                    local.get 1
                    i32.add
                )
            )
            (assert_return (invoke "add" (i32.const 1) (i32.const 2)) (i32.const 3))
        "#;

        let result = execute_wast_buffer("test.wast", wast).unwrap();
        assert!(result.all_passed(), "Expected WAST execution to pass");
    }

    #[test]
    fn test_execute_failing_wast() {
        let wast = br#"
            (module
                (func (export "add") (param i32 i32) (result i32)
                    local.get 0
                    local.get 1
                    i32.add
                )
            )
            (assert_return (invoke "add" (i32.const 1) (i32.const 2)) (i32.const 999))
        "#;

        let result = execute_wast_buffer("test.wast", wast).unwrap();
        assert!(!result.all_passed(), "Expected WAST execution to fail");
    }
}
