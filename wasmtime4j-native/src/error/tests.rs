//! Tests for error handling module

use super::*;

#[test]
fn test_error_codes() {
    let error = WasmtimeError::Compilation {
        message: "test".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::CompilationError));
}

#[test]
fn test_c_string_conversion() {
    let error = WasmtimeError::Runtime {
        message: "test error".to_string(),
        backtrace: None,
    };
    let c_str = error.to_c_string();
    assert!(c_str.to_str().unwrap().contains("test error"));
}

#[test]
fn test_ffi_error_handling() {
    use ffi_utils::*;

    let result: (ErrorCode, ()) = ffi_try(|| {
        Err(WasmtimeError::InvalidParameter {
            message: "test".to_string(),
        })
    });

    assert!(matches!(result.0, ErrorCode::InvalidParameterError));

    let message = get_last_error_message();
    assert!(!message.is_null());

    unsafe {
        free_error_message(message);
    }
}

#[test]
fn test_thread_safety_error_handling() {
    use ffi_utils::*;
    use std::sync::atomic::{AtomicUsize, Ordering};
    use std::sync::Arc;
    use std::thread;

    // Test concurrent error setting and retrieval across multiple threads
    let error_count = Arc::new(AtomicUsize::new(0));
    let success_count = Arc::new(AtomicUsize::new(0));

    let handles: Vec<_> = (0..10)
        .map(|i| {
            let error_count = Arc::clone(&error_count);
            let success_count = Arc::clone(&success_count);

            thread::spawn(move || {
                // Each thread performs multiple error operations
                for j in 0..100 {
                    // Set an error
                    let result: (ErrorCode, ()) = ffi_try(|| {
                        if (i + j) % 3 == 0 {
                            Err(WasmtimeError::InvalidParameter {
                                message: format!("Thread {} iteration {}", i, j),
                            })
                        } else {
                            Ok(())
                        }
                    });

                    match result.0 {
                        ErrorCode::Success => {
                            success_count.fetch_add(1, Ordering::SeqCst);
                            // Verify no error is pending
                            assert!(!has_pending_error());
                        }
                        ErrorCode::InvalidParameterError => {
                            error_count.fetch_add(1, Ordering::SeqCst);
                            // Verify error is pending
                            assert!(has_pending_error());

                            // Get error stats
                            let (has_error, stats) = get_error_stats();
                            assert!(has_error);
                            assert!(stats.contains("Thread"));

                            // Clear error
                            clear_last_error();
                            assert!(!has_pending_error());
                        }
                        _ => panic!("Unexpected error code: {:?}", result.0),
                    }
                }
            })
        })
        .collect();

    // Wait for all threads to complete
    for handle in handles {
        handle.join().expect("thread should not panic");
    }

    let total_errors = error_count.load(Ordering::SeqCst);
    let total_success = success_count.load(Ordering::SeqCst);

    // Verify that we processed all operations correctly
    assert_eq!(total_errors + total_success, 1000);

    // Each thread runs 100 iterations, every 3rd should be an error
    // So we expect approximately 333 errors
    assert!(total_errors >= 300 && total_errors <= 350);
}

#[test]
fn test_panic_safety_error_handling() {
    use ffi_utils::*;

    // Test that error handling doesn't panic under stress
    for _ in 0..1000 {
        let result: (ErrorCode, ()) = ffi_try(|| {
            Err(WasmtimeError::Internal {
                message: "Stress test error".to_string(),
            })
        });

        assert!(matches!(result.0, ErrorCode::InternalError));

        let message = get_last_error_message();
        assert!(!message.is_null());

        unsafe {
            free_error_message(message);
        }

        clear_last_error();
        assert!(!has_pending_error());
    }
}

#[test]
fn test_error_aggregation() {
    use ffi_utils::*;

    // Test ErrorCollector functionality
    let mut collector = ErrorCollector::with_operation("test_batch_operation".to_string());

    // Add multiple errors
    collector.add_error(WasmtimeError::Compilation {
        message: "Compilation failed".to_string(),
    });
    collector.add_error(WasmtimeError::Validation {
        message: "Validation failed".to_string(),
    });

    assert!(collector.has_errors());
    assert_eq!(collector.error_count(), 2);

    // Convert to result
    let result = collector.into_result();
    assert!(result.is_err());

    if let Err(WasmtimeError::Multiple { summary, errors }) = result {
        assert!(summary.contains("test_batch_operation"));
        assert_eq!(errors.len(), 2);
    } else {
        panic!("Expected Multiple error");
    }
}

#[test]
fn test_error_aggregation_single_error() {
    let error = WasmtimeError::Runtime {
        message: "Single error".to_string(),
        backtrace: None,
    };

    let multiple = WasmtimeError::multiple(vec![error.clone()]);
    assert!(multiple.is_multiple());
    assert_eq!(multiple.error_count(), 1);

    let individual_errors = multiple.get_individual_errors();
    assert_eq!(individual_errors.len(), 1);
}

#[test]
fn test_try_all_operations() {
    use ffi_utils::*;

    // Test successful operations
    let operations = vec![
        || -> WasmtimeResult<i32> { Ok(1) },
        || -> WasmtimeResult<i32> { Ok(2) },
        || -> WasmtimeResult<i32> { Ok(3) },
    ];

    let result = try_all(operations);
    assert!(result.is_ok());
    assert_eq!(result.unwrap(), vec![1, 2, 3]);

    // Test operations with failures
    let operations_with_failures = vec![
        || -> WasmtimeResult<i32> { Ok(1) },
        || -> WasmtimeResult<i32> {
            Err(WasmtimeError::Runtime {
                message: "Error 1".to_string(),
                backtrace: None,
            })
        },
        || -> WasmtimeResult<i32> { Ok(3) },
        || -> WasmtimeResult<i32> {
            Err(WasmtimeError::Validation {
                message: "Error 2".to_string(),
            })
        },
    ];

    let result = try_all(operations_with_failures);
    assert!(result.is_err());

    if let Err(WasmtimeError::Multiple { errors, .. }) = result {
        assert_eq!(errors.len(), 2);
    } else {
        panic!("Expected Multiple error");
    }
}

#[test]
fn test_try_all_continue_operations() {
    use ffi_utils::*;

    let operations = vec![
        || -> WasmtimeResult<i32> { Ok(1) },
        || -> WasmtimeResult<i32> {
            Err(WasmtimeError::Runtime {
                message: "Error 1".to_string(),
                backtrace: None,
            })
        },
        || -> WasmtimeResult<i32> { Ok(3) },
        || -> WasmtimeResult<i32> {
            Err(WasmtimeError::Validation {
                message: "Error 2".to_string(),
            })
        },
    ];

    let (results, error) = try_all_continue(operations);
    assert_eq!(results, vec![1, 3]); // Only successful results
    assert!(error.is_some());

    if let Some(WasmtimeError::Multiple { errors, .. }) = error {
        assert_eq!(errors.len(), 2);
    } else {
        panic!("Expected Multiple error");
    }
}

#[test]
fn test_defensive_error_handling() {
    use ffi_utils::*;

    // Test that error handling functions are defensive against invalid inputs
    unsafe {
        free_error_message(std::ptr::null_mut()); // Should not crash
    }

    // Test stats functions don't panic
    let (has_error, stats) = get_error_stats();
    assert!(!has_error);
    assert!(stats.contains("No error"));

    assert!(!has_pending_error());

    // Set and clear error multiple times
    for _ in 0..10 {
        let result: (ErrorCode, ()) = ffi_try(|| {
            Err(WasmtimeError::Validation {
                message: "Test validation error".to_string(),
            })
        });
        assert!(matches!(result.0, ErrorCode::ValidationError));
        assert!(has_pending_error());

        clear_last_error();
        assert!(!has_pending_error());
    }
}

// =========================================================================
// Error Variant Construction Tests (10 tests)
// =========================================================================

#[test]
fn test_error_variant_compilation() {
    let error = WasmtimeError::Compilation {
        message: "Syntax error".to_string(),
    };
    assert!(error.to_string().contains("Syntax error"));
    assert!(error.to_string().contains("Compilation"));
}

#[test]
fn test_error_variant_validation() {
    let error = WasmtimeError::Validation {
        message: "Invalid module".to_string(),
    };
    assert!(error.to_string().contains("Invalid module"));
}

#[test]
fn test_error_variant_runtime_with_backtrace() {
    let error = WasmtimeError::Runtime {
        message: "Trap occurred".to_string(),
        backtrace: None,
    };
    assert!(error.to_string().contains("Trap occurred"));
}

#[test]
fn test_error_variant_engine_config() {
    let error = WasmtimeError::EngineConfig {
        message: "Invalid config".to_string(),
    };
    assert!(matches!(
        error.to_error_code(),
        ErrorCode::EngineConfigError
    ));
}

#[test]
fn test_error_variant_store() {
    let error = WasmtimeError::Store {
        message: "Store error".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::StoreError));
}

#[test]
fn test_error_variant_instance() {
    let error = WasmtimeError::Instance {
        message: "Instantiation failed".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::InstanceError));
}

#[test]
fn test_error_variant_memory() {
    let error = WasmtimeError::Memory {
        message: "Out of memory".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::MemoryError));
}

#[test]
fn test_error_variant_function() {
    let error = WasmtimeError::Function {
        message: "Function not found".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::FunctionError));
}

#[test]
fn test_error_variant_import_export() {
    let error = WasmtimeError::ImportExport {
        message: "Missing import".to_string(),
    };
    assert!(matches!(
        error.to_error_code(),
        ErrorCode::ImportExportError
    ));
}

#[test]
fn test_error_variant_type() {
    let error = WasmtimeError::Type {
        message: "Type mismatch".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::TypeError));
}

// =========================================================================
// Error Classification Tests (8 tests)
// =========================================================================

#[test]
fn test_is_multiple_true() {
    let error = WasmtimeError::multiple(vec![
        WasmtimeError::Compilation {
            message: "Error 1".to_string(),
        },
        WasmtimeError::Validation {
            message: "Error 2".to_string(),
        },
    ]);
    assert!(error.is_multiple());
}

#[test]
fn test_is_multiple_false() {
    let error = WasmtimeError::Compilation {
        message: "Single error".to_string(),
    };
    assert!(!error.is_multiple());
}

#[test]
fn test_error_count_single() {
    let error = WasmtimeError::Runtime {
        message: "Error".to_string(),
        backtrace: None,
    };
    assert_eq!(error.error_count(), 1);
}

#[test]
fn test_error_count_multiple() {
    let error = WasmtimeError::multiple(vec![
        WasmtimeError::Compilation {
            message: "1".to_string(),
        },
        WasmtimeError::Validation {
            message: "2".to_string(),
        },
        WasmtimeError::Runtime {
            message: "3".to_string(),
            backtrace: None,
        },
    ]);
    assert_eq!(error.error_count(), 3);
}

#[test]
fn test_get_individual_errors_single() {
    let error = WasmtimeError::Compilation {
        message: "Single".to_string(),
    };
    let individuals = error.get_individual_errors();
    assert_eq!(individuals.len(), 1);
}

#[test]
fn test_get_individual_errors_multiple() {
    let error = WasmtimeError::multiple(vec![
        WasmtimeError::Compilation {
            message: "1".to_string(),
        },
        WasmtimeError::Validation {
            message: "2".to_string(),
        },
    ]);
    let individuals = error.get_individual_errors();
    assert_eq!(individuals.len(), 2);
}

#[test]
fn test_multiple_with_empty_errors() {
    let error = WasmtimeError::multiple(vec![]);
    assert!(error.is_multiple());
    assert_eq!(error.error_count(), 0);
}

#[test]
fn test_multiple_summary_truncation() {
    let error = WasmtimeError::multiple(vec![
        WasmtimeError::Compilation {
            message: "Error 1".to_string(),
        },
        WasmtimeError::Compilation {
            message: "Error 2".to_string(),
        },
        WasmtimeError::Compilation {
            message: "Error 3".to_string(),
        },
        WasmtimeError::Compilation {
            message: "Error 4".to_string(),
        },
        WasmtimeError::Compilation {
            message: "Error 5".to_string(),
        },
    ]);

    if let WasmtimeError::Multiple { summary, .. } = error {
        assert!(summary.contains("5 errors"));
        assert!(summary.contains("..."));
    } else {
        panic!("Expected Multiple error");
    }
}

// =========================================================================
// Error Code Mapping Tests (7 tests)
// =========================================================================

#[test]
fn test_to_error_code_all_variants() {
    let mappings = [
        (
            WasmtimeError::Compilation {
                message: String::new(),
            },
            ErrorCode::CompilationError,
        ),
        (
            WasmtimeError::Validation {
                message: String::new(),
            },
            ErrorCode::ValidationError,
        ),
        (
            WasmtimeError::Runtime {
                message: String::new(),
                backtrace: None,
            },
            ErrorCode::RuntimeError,
        ),
        (
            WasmtimeError::Store {
                message: String::new(),
            },
            ErrorCode::StoreError,
        ),
        (
            WasmtimeError::Instance {
                message: String::new(),
            },
            ErrorCode::InstanceError,
        ),
        (
            WasmtimeError::Memory {
                message: String::new(),
            },
            ErrorCode::MemoryError,
        ),
        (
            WasmtimeError::Function {
                message: String::new(),
            },
            ErrorCode::FunctionError,
        ),
        (
            WasmtimeError::Security {
                message: String::new(),
            },
            ErrorCode::SecurityError,
        ),
    ];

    for (error, expected_code) in mappings {
        assert_eq!(
            std::mem::discriminant(&error.to_error_code()),
            std::mem::discriminant(&expected_code),
            "Error {:?} should map to {:?}",
            error,
            expected_code
        );
    }
}

#[test]
fn test_to_error_code_wasi() {
    let error = WasmtimeError::Wasi {
        message: "WASI error".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::WasiError));
}

#[test]
fn test_to_error_code_component() {
    let error = WasmtimeError::Component {
        message: "Component error".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::ComponentError));
}

#[test]
fn test_to_error_code_interface() {
    let error = WasmtimeError::Interface {
        message: "Interface error".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::InterfaceError));
}

#[test]
fn test_to_error_code_concurrency() {
    let error = WasmtimeError::Concurrency {
        message: "Deadlock".to_string(),
    };
    assert!(matches!(error.to_error_code(), ErrorCode::ConcurrencyError));
}

// =========================================================================
// Error Helper Functions Tests (5 tests)
// =========================================================================

#[test]
fn test_invalid_parameter_helper() {
    let error = WasmtimeError::invalid_parameter("Bad value");
    assert!(matches!(error, WasmtimeError::InvalidParameter { .. }));
    assert!(error.to_string().contains("Bad value"));
}

#[test]
fn test_security_violation_helper() {
    let error = WasmtimeError::security_violation("Access denied");
    assert!(matches!(error, WasmtimeError::Security { .. }));
    assert!(error.to_string().contains("Access denied"));
}

#[test]
fn test_resource_error_helper() {
    let error = WasmtimeError::resource_error("Out of memory");
    assert!(matches!(error, WasmtimeError::Resource { .. }));
    assert!(error.to_string().contains("Out of memory"));
}

#[test]
fn test_from_string_helper() {
    let error = WasmtimeError::from_string("Custom error");
    assert!(matches!(error, WasmtimeError::Runtime { .. }));
    assert!(error.to_string().contains("Custom error"));
}

// =========================================================================
// Error Clone Tests (5 tests)
// =========================================================================

#[test]
fn test_clone_compilation_error() {
    let error = WasmtimeError::Compilation {
        message: "Original".to_string(),
    };
    let cloned = error.clone();
    assert_eq!(error.to_string(), cloned.to_string());
}

#[test]
fn test_clone_runtime_error() {
    let error = WasmtimeError::Runtime {
        message: "Runtime error".to_string(),
        backtrace: None,
    };
    let cloned = error.clone();
    assert!(cloned.to_string().contains("Runtime error"));
}

#[test]
fn test_clone_multiple_error() {
    let error = WasmtimeError::multiple(vec![
        WasmtimeError::Compilation {
            message: "1".to_string(),
        },
        WasmtimeError::Validation {
            message: "2".to_string(),
        },
    ]);
    let cloned = error.clone();
    assert_eq!(error.error_count(), cloned.error_count());
}

#[test]
fn test_clone_type_mismatch_error() {
    let error = WasmtimeError::TypeMismatch {
        expected: "i32".to_string(),
        actual: "i64".to_string(),
    };
    let cloned = error.clone();
    assert!(cloned.to_string().contains("i32"));
    assert!(cloned.to_string().contains("i64"));
}

#[test]
fn test_clone_export_not_found() {
    let error = WasmtimeError::ExportNotFound {
        name: "my_func".to_string(),
    };
    let cloned = error.clone();
    assert!(cloned.to_string().contains("my_func"));
}

// =========================================================================
// C String Conversion Tests (3 tests)
// =========================================================================

#[test]
fn test_to_c_string_basic() {
    let error = WasmtimeError::Compilation {
        message: "Test error".to_string(),
    };
    let c_str = error.to_c_string();
    assert!(c_str.to_str().unwrap().contains("Test error"));
}

#[test]
fn test_to_c_string_unicode() {
    let error = WasmtimeError::Compilation {
        message: "Unicode test: αβγ".to_string(),
    };
    let c_str = error.to_c_string();
    assert!(c_str.to_str().unwrap().contains("αβγ"));
}

#[test]
fn test_to_c_string_empty_message() {
    let error = WasmtimeError::Compilation {
        message: String::new(),
    };
    let c_str = error.to_c_string();
    // Should still produce a valid CString
    assert!(c_str.to_str().is_ok());
}

// =========================================================================
// Additional Error Type Tests (2 tests)
// =========================================================================

#[test]
fn test_linker_error() {
    let error = WasmtimeError::Linker {
        message: "Linker error".to_string(),
    };
    assert!(matches!(
        error.to_error_code(),
        ErrorCode::ImportExportError
    ));
}
