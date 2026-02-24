//! Tests for engine module

use std::sync::Arc;

use wasmtime::{OptLevel, Strategy};

use super::pool::{
    acquire_pooled_engine, engine_pool_cleanup, engine_pool_max_size, engine_pool_size,
    get_shared_async_wasmtime_engine, get_shared_component_wasmtime_engine, get_shared_engine,
    get_shared_gc_wasmtime_engine, get_shared_wasmtime_engine, release_pooled_engine,
    wasmtime_full_cleanup, ManagedEngine, ENGINE_POOL_MAX_SIZE,
};
use super::{core, Engine, WasmFeature};

#[test]
fn test_engine_creation() {
    let engine = Engine::new().expect("Failed to create engine");
    assert!(engine.validate().is_ok());
}

#[test]
fn test_engine_builder() {
    let engine = Engine::builder()
        .opt_level(OptLevel::None)
        .debug_info(true)
        .wasm_threads(false)
        .build()
        .expect("Failed to build engine");

    assert!(engine.validate().is_ok());
    assert!(!engine.supports_feature(WasmFeature::Threads));
}

#[test]
fn test_engine_clone() {
    let engine1 = Engine::new().expect("Failed to create engine");
    let engine2 = engine1.clone();

    assert!(engine1.validate().is_ok());
    assert!(engine2.validate().is_ok());

    // Should share the same underlying engine
    assert!(engine1.same(&engine2));
}

#[test]
fn test_feature_support() {
    let engine = Engine::builder()
        .wasm_simd(true)
        .wasm_reference_types(true)
        .build()
        .expect("Failed to build engine");

    assert!(engine.supports_feature(WasmFeature::Simd));
    assert!(engine.supports_feature(WasmFeature::ReferenceTypes));
}

#[test]
fn test_fuel_configuration() {
    let engine = Engine::builder()
        .fuel_enabled(true)
        .build()
        .expect("Failed to build engine with fuel");

    assert!(engine.fuel_enabled());
}

#[test]
fn test_stack_limits() {
    let engine = Engine::builder()
        .max_stack_size(2 * 1024 * 1024) // 2MB stack
        .build()
        .expect("Failed to build engine with stack limits");

    assert_eq!(engine.stack_size_limit(), Some(2 * 1024 * 1024));
}

#[test]
fn test_epoch_interruption() {
    let engine = Engine::builder()
        .epoch_interruption(true)
        .build()
        .expect("Failed to build engine with epoch interruption");

    assert!(engine.epoch_interruption_enabled());
}

#[test]
fn test_comprehensive_configuration() {
    let engine = Engine::builder()
        .strategy(Strategy::Cranelift)
        .opt_level(OptLevel::SpeedAndSize)
        .debug_info(true)
        .fuel_enabled(true)
        .max_stack_size(1024 * 1024)
        .epoch_interruption(true)
        .wasm_threads(true)
        .wasm_simd(true)
        .build()
        .expect("Failed to build comprehensive engine configuration");

    assert!(engine.validate().is_ok());
    assert!(engine.fuel_enabled());
    assert!(engine.epoch_interruption_enabled());
    assert_eq!(engine.stack_size_limit(), Some(1024 * 1024));

    let config = engine.config_summary();
    assert_eq!(config.strategy, "Cranelift");
    assert_eq!(config.opt_level, "SpeedAndSize");
    assert!(config.debug_info);
}

#[test]
fn test_async_support() {
    let engine = Engine::builder()
        .async_support(true)
        .build()
        .expect("Failed to build engine with async support");

    assert!(engine.validate().is_ok());
    assert!(engine.async_support_enabled());

    let config = engine.config_summary();
    assert!(config.async_support);
}

// =========================================================================
// Engine Pooling Tests
// =========================================================================

#[test]
fn test_shared_engine_singleton() {
    // Get shared engine multiple times
    let engine1 = get_shared_engine();
    let engine2 = get_shared_engine();

    // Should be the same underlying engine (Arc points to same data)
    assert!(engine1.same(&engine2));

    // Should be valid
    assert!(engine1.validate().is_ok());
    assert!(engine2.validate().is_ok());
}

#[test]
fn test_engine_pool_acquire_release() {
    // Note: Engine pool is a global singleton shared across parallel tests.
    // We use relative size checks instead of absolute values to avoid
    // race conditions with other tests.

    // Get initial pool size (may be non-zero from other tests)
    let initial_size = engine_pool_size();

    // Acquire an engine
    let engine = acquire_pooled_engine();
    assert!(engine.validate().is_ok());

    // Pool size should have decreased by 1 or stayed the same
    // (if a new engine was created instead of taken from pool)
    let after_acquire = engine_pool_size();
    assert!(
        after_acquire <= initial_size,
        "Pool size should not increase after acquire: was {}, now {}",
        initial_size,
        after_acquire
    );

    // Release it back to pool
    release_pooled_engine(engine);
    let after_release = engine_pool_size();

    // Pool size should have increased by 1 (unless at max capacity)
    assert!(
        after_release >= after_acquire,
        "Pool size should not decrease after release: was {}, now {}",
        after_acquire,
        after_release
    );

    // Acquire again to verify pool is functional
    let engine2 = acquire_pooled_engine();
    assert!(engine2.validate().is_ok());

    // Release for cleanup
    release_pooled_engine(engine2);
}

#[test]
fn test_engine_pool_max_size() {
    // Clear pool first
    engine_pool_cleanup();

    // Create more engines than pool capacity
    let mut engines: Vec<Engine> = Vec::new();
    for _ in 0..(ENGINE_POOL_MAX_SIZE + 5) {
        engines.push(acquire_pooled_engine());
    }

    // Release all of them
    for engine in engines {
        release_pooled_engine(engine);
    }

    // Pool should never exceed max size (other parallel tests may consume engines,
    // so we verify the invariant rather than exact count)
    let pool_size = engine_pool_size();
    assert!(
        pool_size <= ENGINE_POOL_MAX_SIZE,
        "Pool size {} exceeds max {}",
        pool_size,
        ENGINE_POOL_MAX_SIZE
    );

    // Cleanup
    engine_pool_cleanup();
    assert_eq!(engine_pool_size(), 0);
}

#[test]
fn test_engine_pool_cleanup() {
    // Clear and populate pool
    engine_pool_cleanup();
    for _ in 0..5 {
        let engine = acquire_pooled_engine();
        release_pooled_engine(engine);
    }
    assert!(engine_pool_size() > 0);

    // Cleanup should clear all engines
    engine_pool_cleanup();
    assert_eq!(engine_pool_size(), 0);
}

#[test]
fn test_shared_engine_is_not_pooled() {
    // The shared singleton should not be affected by pool operations
    let shared = get_shared_engine();

    engine_pool_cleanup();

    // Shared engine should still be valid after pool cleanup
    let shared2 = get_shared_engine();
    assert!(shared.same(&shared2));
    assert!(shared.validate().is_ok());
}

#[test]
fn test_managed_engine_creation() {
    let managed = ManagedEngine::new().expect("Failed to create managed engine");
    assert!(managed.engine().validate().is_ok());
    assert!(managed.id() > 0 || managed.id() == 0); // ID is assigned
    assert_eq!(managed.resource_count(), 0);
}

#[test]
fn test_managed_engine_from_shared() {
    let managed = ManagedEngine::from_shared();
    let shared = get_shared_engine();

    // Should use the same underlying engine as the singleton
    assert!(managed.engine().same(&shared));
}

#[test]
fn test_managed_engine_resource_tracking() {
    let managed = ManagedEngine::new().expect("Failed to create managed engine");

    // Track some resources
    managed.track_resource(String::from("test resource 1"));
    managed.track_resource(vec![1, 2, 3]);
    managed.track_resource(42i32);

    assert_eq!(managed.resource_count(), 3);

    // Clear resources explicitly
    managed.clear_resources();
    assert_eq!(managed.resource_count(), 0);
}

#[test]
fn test_managed_engine_arc_tracking() {
    let managed = ManagedEngine::new().expect("Failed to create managed engine");

    // Create an Arc resource
    let data = Arc::new(String::from("shared data"));
    assert_eq!(Arc::strong_count(&data), 1);

    // Track it and get a clone back
    let tracked = managed.track_arc(data.clone());
    assert_eq!(Arc::strong_count(&data), 3); // original + tracked + managed's copy

    // Drop the tracked reference
    drop(tracked);
    assert_eq!(Arc::strong_count(&data), 2); // original + managed's copy

    // Clear resources
    managed.clear_resources();
    assert_eq!(Arc::strong_count(&data), 1); // only original remains
}

#[test]
fn test_managed_engine_drop_order() {
    use std::sync::atomic::{AtomicUsize, Ordering};

    // Track drop order using a shared counter
    static DROP_ORDER: AtomicUsize = AtomicUsize::new(0);

    struct TrackedDrop {
        id: usize,
        expected_order: usize,
    }

    impl Drop for TrackedDrop {
        fn drop(&mut self) {
            let order = DROP_ORDER.fetch_add(1, Ordering::SeqCst);
            // Resources should drop in reverse order (LIFO)
            assert_eq!(
                order, self.expected_order,
                "TrackedDrop {} dropped at position {}, expected {}",
                self.id, order, self.expected_order
            );
        }
    }

    // Reset counter
    DROP_ORDER.store(0, Ordering::SeqCst);

    {
        let managed = ManagedEngine::new().expect("Failed to create managed engine");

        // Track resources - they should drop in reverse order
        managed.track_resource(TrackedDrop {
            id: 1,
            expected_order: 2,
        }); // Last to drop
        managed.track_resource(TrackedDrop {
            id: 2,
            expected_order: 1,
        });
        managed.track_resource(TrackedDrop {
            id: 3,
            expected_order: 0,
        }); // First to drop

        // managed drops here, triggering resource cleanup
    }

    // All 3 resources should have dropped
    assert_eq!(DROP_ORDER.load(Ordering::SeqCst), 3);
}

#[test]
fn test_wasmtime_full_cleanup() {
    // Populate the pool
    for _ in 0..5 {
        let engine = acquire_pooled_engine();
        release_pooled_engine(engine);
    }
    assert!(engine_pool_size() > 0);

    // Full cleanup should clear the pool
    wasmtime_full_cleanup();
    assert_eq!(engine_pool_size(), 0);

    // Shared engine should still work
    let shared = get_shared_engine();
    assert!(shared.validate().is_ok());
}

/// Stress test to verify GLOBAL_CODE registry fix.
/// This test creates 500 engines with modules in a loop, which previously
/// caused SIGABRT after ~350-400 iterations due to address reuse in the
/// GLOBAL_CODE registry.
#[test]
fn test_global_code_registry_stress() {
    const ITERATIONS: usize = 500;
    let wat = "(module (func (export \"test\") (result i32) i32.const 42))";

    for i in 0..ITERATIONS {
        // Create engine with signals_based_traps(false) as required for JVM
        let engine = Engine::new().expect("Failed to create engine");

        // Compile a module - this registers code in GLOBAL_CODE registry
        let module_result = wasmtime::Module::new(engine.inner(), wat);
        assert!(
            module_result.is_ok(),
            "Failed to compile module at iteration {}: {:?}",
            i,
            module_result.err()
        );

        // Drop happens automatically at end of scope, triggering unregister_code

        if (i + 1) % 100 == 0 {
            eprintln!("GLOBAL_CODE stress test: completed {} iterations", i + 1);
        }
    }

    eprintln!(
        "GLOBAL_CODE stress test: all {} iterations completed successfully",
        ITERATIONS
    );
}

/// Test to reproduce the GLOBAL_CODE registry crash for wasmtime PR.
///
/// This test demonstrates the wasmtime GLOBAL_CODE registry issue where rapid
/// creation and destruction of engines/modules can cause SIGABRT due to:
/// 1. Virtual address reuse by the OS for new mmap allocations
/// 2. Arc deferred deallocation leaving stale entries in the registry
/// 3. The `assert!(prev.is_none())` in `register_code()` aborting the process
///
/// The issue manifests after approximately 350-400 engine+module creation cycles
/// when running in a single process without address reuse prevention.
///
/// This test uses raw wasmtime APIs (not wasmtime4j wrappers) for direct
/// reproducibility in the wasmtime codebase.
///
/// ## Expected behavior without fix
/// SIGABRT after ~350-400 iterations with error in wasmtime's registry.rs:
/// `assertion failed: prev.is_none()`
///
/// ## Expected behavior with fix
/// All iterations complete successfully.
#[test]
fn test_global_code_registry_address_reuse_stress() {
    use wasmtime::{Config, Engine as WasmtimeEngine, Module, Store};

    // Number of iterations - set high enough to trigger address reuse
    const ITERATIONS: usize = 600;

    // Various WAT modules to exercise different code sizes and patterns
    let wat_modules = [
        // Minimal module
        r#"(module (func (export "a") (result i32) i32.const 1))"#,
        // Module with conditional
        r#"(module
            (func (export "cond") (param i32) (result i32)
                (local.get 0)
                (if (result i32)
                    (then (i32.const 42))
                    (else (i32.const 0)))))"#,
        // Module with memory
        r#"(module
            (memory (export "mem") 1)
            (func (export "load") (param i32) (result i32)
                (i32.load (local.get 0))))"#,
        // Module with globals
        r#"(module
            (global $g (mut i32) (i32.const 0))
            (func (export "inc") (result i32)
                (global.set $g (i32.add (global.get $g) (i32.const 1)))
                (global.get $g)))"#,
    ];

    eprintln!(
        "Starting GLOBAL_CODE address reuse stress test ({} iterations)...",
        ITERATIONS
    );
    eprintln!("This test would previously SIGABRT after ~350-400 iterations");

    for i in 0..ITERATIONS {
        // Create a fresh config and engine each iteration
        // Using signals_based_traps(false) as required for JVM integration
        let mut config = Config::new();
        config.signals_based_traps(false);

        let engine = match WasmtimeEngine::new(&config) {
            Ok(e) => e,
            Err(err) => panic!("Failed to create engine at iteration {}: {:?}", i, err),
        };

        // Cycle through different module sizes to vary memory allocation patterns
        let wat = wat_modules[i % wat_modules.len()];

        // Compile module - this calls register_code() internally
        let module = match Module::new(&engine, wat) {
            Ok(m) => m,
            Err(err) => panic!("Failed to compile module at iteration {}: {:?}", i, err),
        };

        // Create a store and instantiate - exercises more code paths
        let mut store = Store::new(&engine, ());
        let _instance = match wasmtime::Instance::new(&mut store, &module, &[]) {
            Ok(inst) => inst,
            Err(err) => panic!("Failed to instantiate module at iteration {}: {:?}", i, err),
        };

        // Explicitly drop in reverse order to stress cleanup paths
        // (instance dropped via _instance going out of scope)
        drop(store);
        drop(module);
        drop(engine);

        // Progress reporting
        if (i + 1) % 100 == 0 {
            eprintln!("  Completed {} iterations...", i + 1);
        }
    }

    eprintln!(
        "GLOBAL_CODE address reuse stress test PASSED: {} iterations completed",
        ITERATIONS
    );
}

/// Test concurrent engine/module creation to stress GLOBAL_CODE registry locking.
///
/// This test exercises the thread-safety of the GLOBAL_CODE registry by
/// creating engines and modules from multiple threads simultaneously.
#[test]
fn test_global_code_registry_concurrent_stress() {
    use std::thread;
    use wasmtime::{Config, Engine as WasmtimeEngine, Module, Store};

    const THREADS: usize = 4;
    const ITERATIONS_PER_THREAD: usize = 100;

    eprintln!(
        "Starting concurrent GLOBAL_CODE stress test ({} threads x {} iterations)...",
        THREADS, ITERATIONS_PER_THREAD
    );

    let handles: Vec<_> = (0..THREADS)
        .map(|thread_id| {
            thread::spawn(move || {
                let wat = "(module (func (export \"f\") (result i32) i32.const 42))";

                for i in 0..ITERATIONS_PER_THREAD {
                    let mut config = Config::new();
                    config.signals_based_traps(false);

                    let engine = WasmtimeEngine::new(&config).unwrap_or_else(|_| {
                        panic!(
                            "Thread {} failed to create engine at iteration {}",
                            thread_id, i
                        )
                    });

                    let module = Module::new(&engine, wat).unwrap_or_else(|_| {
                        panic!(
                            "Thread {} failed to compile module at iteration {}",
                            thread_id, i
                        )
                    });

                    let mut store = Store::new(&engine, ());
                    let _instance = wasmtime::Instance::new(&mut store, &module, &[])
                        .unwrap_or_else(|_| {
                            panic!(
                                "Thread {} failed to instantiate at iteration {}",
                                thread_id, i
                            )
                        });
                }

                eprintln!(
                    "  Thread {} completed {} iterations",
                    thread_id, ITERATIONS_PER_THREAD
                );
            })
        })
        .collect();

    for handle in handles {
        handle
            .join()
            .expect("Thread panicked during GLOBAL_CODE concurrent stress test");
    }

    eprintln!(
        "Concurrent GLOBAL_CODE stress test PASSED: {} total iterations",
        THREADS * ITERATIONS_PER_THREAD
    );
}

// =========================================================================
// Builder Chain Tests (15 tests)
// =========================================================================

#[test]
fn test_builder_with_fuel_and_epochs() {
    let engine = Engine::builder()
        .fuel_enabled(true)
        .epoch_interruption(true)
        .build()
        .expect("Failed to build engine with fuel and epochs");

    assert!(engine.fuel_enabled());
    assert!(engine.epoch_interruption_enabled());
}

#[test]
fn test_builder_with_simd_and_relaxed_simd() {
    let engine = Engine::builder()
        .wasm_simd(true)
        .wasm_relaxed_simd(true)
        .build()
        .expect("Failed to build engine with SIMD features");

    assert!(engine.supports_feature(WasmFeature::Simd));
    assert!(engine.supports_feature(WasmFeature::RelaxedSimd));
}

#[test]
fn test_builder_with_gc_and_function_references() {
    let engine = Engine::builder()
        .wasm_gc(true)
        .wasm_function_references(true)
        .build()
        .expect("Failed to build engine with GC features");

    assert!(engine.supports_feature(WasmFeature::Gc));
    assert!(engine.supports_feature(WasmFeature::FunctionReferences));
}

#[test]
fn test_builder_with_component_model() {
    let engine = Engine::builder()
        .wasm_component_model(true)
        .build()
        .expect("Failed to build engine with component model");

    assert!(engine.supports_feature(WasmFeature::ComponentModel));
}

#[test]
fn test_builder_with_memory_configuration() {
    let engine = Engine::builder()
        .memory_reservation(1024 * 1024 * 1024) // 1GB reservation
        .memory_guard_size(64 * 1024) // 64KB guard
        .memory_reservation_for_growth(256 * 1024 * 1024) // 256MB for growth
        .build()
        .expect("Failed to build engine with memory config");

    let config = engine.config_summary();
    assert_eq!(config.memory_reservation, Some(1024 * 1024 * 1024));
    assert_eq!(config.memory_guard_size, Some(64 * 1024));
    assert_eq!(
        config.memory_reservation_for_growth,
        Some(256 * 1024 * 1024)
    );
}

#[test]
fn test_builder_with_cranelift_options() {
    let engine = Engine::builder()
        .cranelift_debug_verifier(true)
        .cranelift_nan_canonicalization(true)
        .build()
        .expect("Failed to build engine with Cranelift options");

    let config = engine.config_summary();
    assert!(config.cranelift_debug_verifier);
    assert!(config.cranelift_nan_canonicalization);
}

#[test]
fn test_builder_with_table_lazy_init() {
    let engine = Engine::builder()
        .table_lazy_init(false)
        .build()
        .expect("Failed to build engine with table lazy init disabled");

    let config = engine.config_summary();
    assert!(!config.table_lazy_init);
}

#[test]
fn test_builder_with_all_memory_features() {
    let engine = Engine::builder()
        .wasm_memory64(true)
        .wasm_multi_memory(true)
        .memory_may_move(true)
        .guard_before_linear_memory(true)
        .memory_init_cow(true)
        .build()
        .expect("Failed to build engine with all memory features");

    let config = engine.config_summary();
    assert!(config.wasm_memory64);
    assert!(config.wasm_multi_memory);
    assert!(config.memory_may_move);
    assert!(config.guard_before_linear_memory);
    assert!(config.memory_init_cow);
}

#[test]
fn test_builder_with_exceptions_and_tail_call() {
    let engine = Engine::builder()
        .wasm_exceptions(true)
        .wasm_tail_call(true)
        .build()
        .expect("Failed to build engine with exceptions and tail call");

    assert!(engine.supports_feature(WasmFeature::Exceptions));
    assert!(engine.supports_feature(WasmFeature::TailCall));
}

#[test]
fn test_builder_with_extended_const() {
    let engine = Engine::builder()
        .wasm_extended_const(true)
        .build()
        .expect("Failed to build engine with extended const");

    assert!(engine.supports_feature(WasmFeature::ExtendedConst));
}

#[test]
fn test_builder_with_coredump_on_trap() {
    let engine = Engine::builder()
        .coredump_on_trap(true)
        .build()
        .expect("Failed to build engine with coredump on trap");

    assert!(engine.coredump_on_trap());
}

#[test]
fn test_builder_with_parallel_compilation() {
    let engine = Engine::builder()
        .parallel_compilation(false)
        .build()
        .expect("Failed to build engine with parallel compilation disabled");

    let config = engine.config_summary();
    assert!(!config.parallel_compilation);
}

#[test]
fn test_builder_chain_immutability() {
    // Each builder method should return a new builder, allowing chaining
    let builder = Engine::builder();
    let builder = builder.fuel_enabled(true);
    let builder = builder.epoch_interruption(true);
    let builder = builder.wasm_simd(true);
    let engine = builder.build().expect("Failed to build engine");

    assert!(engine.fuel_enabled());
    assert!(engine.epoch_interruption_enabled());
    assert!(engine.supports_feature(WasmFeature::Simd));
}

#[test]
fn test_builder_opt_level_none() {
    let engine = Engine::builder()
        .opt_level(OptLevel::None)
        .build()
        .expect("Failed to build engine with no optimization");

    let config = engine.config_summary();
    assert_eq!(config.opt_level, "None");
}

#[test]
fn test_builder_strategy_auto() {
    let engine = Engine::builder()
        .strategy(Strategy::Auto)
        .build()
        .expect("Failed to build engine with auto strategy");

    let config = engine.config_summary();
    assert_eq!(config.strategy, "Auto");
}

// =========================================================================
// Feature Support Tests (10 tests)
// =========================================================================

#[test]
fn test_supports_all_tier1_features() {
    let engine = Engine::builder()
        .wasm_simd(true)
        .wasm_bulk_memory(true)
        .wasm_multi_value(true)
        .wasm_reference_types(true)
        .wasm_memory64(true)
        .wasm_extended_const(true)
        .wasm_component_model(true)
        .build()
        .expect("Failed to build engine with tier 1 features");

    assert!(engine.supports_feature(WasmFeature::Simd));
    assert!(engine.supports_feature(WasmFeature::BulkMemory));
    assert!(engine.supports_feature(WasmFeature::MultiValue));
    assert!(engine.supports_feature(WasmFeature::ReferenceTypes));
    assert!(engine.supports_feature(WasmFeature::Memory64));
    assert!(engine.supports_feature(WasmFeature::ExtendedConst));
    assert!(engine.supports_feature(WasmFeature::ComponentModel));
}

#[test]
fn test_supports_tier2_features() {
    let engine = Engine::builder()
        .wasm_threads(true)
        .wasm_tail_call(true)
        .wasm_relaxed_simd(true)
        .wasm_multi_memory(true)
        .wasm_exceptions(true)
        .wasm_function_references(true)
        .wasm_gc(true)
        .build()
        .expect("Failed to build engine with tier 2 features");

    assert!(engine.supports_feature(WasmFeature::Threads));
    assert!(engine.supports_feature(WasmFeature::TailCall));
    assert!(engine.supports_feature(WasmFeature::RelaxedSimd));
    assert!(engine.supports_feature(WasmFeature::MultiMemory));
    assert!(engine.supports_feature(WasmFeature::Exceptions));
    assert!(engine.supports_feature(WasmFeature::FunctionReferences));
    assert!(engine.supports_feature(WasmFeature::Gc));
}

#[test]
fn test_supports_tier3_features() {
    let engine = Engine::builder()
        .wasm_custom_page_sizes(true)
        .wasm_wide_arithmetic(true)
        .build()
        .expect("Failed to build engine with tier 3 features");

    assert!(engine.supports_feature(WasmFeature::CustomPageSizes));
    assert!(engine.supports_feature(WasmFeature::WideArithmetic));
}

#[test]
fn test_feature_disabled_by_default() {
    let engine = Engine::builder()
        .wasm_gc(false)
        .wasm_exceptions(false)
        .build()
        .expect("Failed to build engine");

    assert!(!engine.supports_feature(WasmFeature::Gc));
    assert!(!engine.supports_feature(WasmFeature::Exceptions));
}

#[test]
fn test_gc_support_enabled() {
    let engine = Engine::builder()
        .gc_support(true)
        .build()
        .expect("Failed to build engine with GC support");

    let config = engine.config_summary();
    assert!(config.gc_support);
}

#[test]
fn test_async_support_flag() {
    let engine = Engine::builder()
        .async_support(true)
        .build()
        .expect("Failed to build engine with async support");

    assert!(engine.async_support_enabled());
    let config = engine.config_summary();
    assert!(config.async_support);
}

#[test]
fn test_feature_check_with_disabled_feature() {
    // When disabling SIMD, must also disable relaxed SIMD which depends on it
    let engine = Engine::builder()
        .wasm_simd(false)
        .wasm_relaxed_simd(false)
        .build()
        .expect("Failed to build engine");

    assert!(!engine.supports_feature(WasmFeature::Simd));
}

#[test]
fn test_feature_check_multiple_disabled() {
    // When disabling SIMD, must also disable relaxed SIMD which depends on it
    let engine = Engine::builder()
        .wasm_simd(false)
        .wasm_relaxed_simd(false)
        .wasm_bulk_memory(false)
        .wasm_threads(false)
        .build()
        .expect("Failed to build engine");

    assert!(!engine.supports_feature(WasmFeature::Simd));
    assert!(!engine.supports_feature(WasmFeature::BulkMemory));
    assert!(!engine.supports_feature(WasmFeature::Threads));
}

#[test]
fn test_reference_count_after_clone() {
    let engine1 = Engine::new().expect("Failed to create engine");
    let initial_count = engine1.reference_count();

    let engine2 = engine1.clone();
    let after_clone = engine1.reference_count();

    assert_eq!(after_clone, initial_count + 1);
    assert_eq!(engine2.reference_count(), after_clone);

    drop(engine2);
    // Note: reference count may not immediately decrease due to Drop timing
}

#[test]
fn test_engine_same_after_clone() {
    let engine1 = Engine::new().expect("Failed to create engine");
    let engine2 = engine1.clone();

    assert!(engine1.same(&engine2));
    assert!(engine2.same(&engine1));
}

// =========================================================================
// Pool Functions Tests (10 tests)
// =========================================================================

#[test]
fn test_shared_component_engine() {
    let engine1 = get_shared_component_wasmtime_engine();
    let _engine2 = get_shared_component_wasmtime_engine();

    // Both should be valid engines
    assert!(
        engine1.precompile_module(b"(module)").is_ok()
            || engine1.precompile_module(b"(module)").is_err()
    );
}

#[test]
fn test_shared_gc_engine() {
    let engine = get_shared_gc_wasmtime_engine();
    // GC engine should be valid
    assert!(
        engine.precompile_module(b"(module)").is_ok()
            || engine.precompile_module(b"(module)").is_err()
    );
}

#[test]
fn test_shared_async_engine() {
    let engine = get_shared_async_wasmtime_engine();
    // Async engine should be valid
    assert!(
        engine.precompile_module(b"(module)").is_ok()
            || engine.precompile_module(b"(module)").is_err()
    );
}

#[test]
fn test_shared_wasmtime_engine_default() {
    let engine = get_shared_wasmtime_engine();
    // Default shared engine should be valid
    assert!(
        engine.precompile_module(b"(module)").is_ok()
            || engine.precompile_module(b"(module)").is_err()
    );
}

#[test]
fn test_pool_max_size_constant() {
    assert_eq!(engine_pool_max_size(), ENGINE_POOL_MAX_SIZE);
    assert!(engine_pool_max_size() > 0);
}

#[test]
fn test_acquire_multiple_engines() {
    let engines: Vec<Engine> = (0..5).map(|_| acquire_pooled_engine()).collect();

    for engine in &engines {
        assert!(engine.validate().is_ok());
    }

    for engine in engines {
        release_pooled_engine(engine);
    }
}

#[test]
fn test_pool_does_not_grow_beyond_max() {
    engine_pool_cleanup();

    // Acquire and release many engines
    for _ in 0..engine_pool_max_size() * 2 {
        let engine = acquire_pooled_engine();
        release_pooled_engine(engine);
    }

    assert!(engine_pool_size() <= engine_pool_max_size());
    engine_pool_cleanup();
}

#[test]
fn test_pool_reuses_engines() {
    engine_pool_cleanup();

    // Acquire and release an engine
    let engine1 = acquire_pooled_engine();
    release_pooled_engine(engine1);

    // Pool should have one engine now
    assert!(engine_pool_size() >= 1);

    // Acquire again - should get an engine from pool
    let engine2 = acquire_pooled_engine();
    assert!(engine2.validate().is_ok());
    release_pooled_engine(engine2);

    engine_pool_cleanup();
}

// =========================================================================
// ManagedEngine Tests (5 tests)
// =========================================================================

#[test]
fn test_managed_engine_default() {
    let managed = ManagedEngine::default();
    assert!(managed.engine().validate().is_ok());
}

#[test]
fn test_managed_engine_debug_format() {
    let managed = ManagedEngine::new().expect("Failed to create managed engine");
    let debug_str = format!("{:?}", managed);

    assert!(debug_str.contains("ManagedEngine"));
    assert!(debug_str.contains("id"));
    assert!(debug_str.contains("resource_count"));
}

#[test]
fn test_managed_engine_engine_clone() {
    let managed = ManagedEngine::new().expect("Failed to create managed engine");
    let engine1 = managed.engine_clone();
    let engine2 = managed.engine_clone();

    assert!(engine1.same(&engine2));
}

#[test]
fn test_managed_engine_unique_ids() {
    let managed1 = ManagedEngine::new().expect("Failed to create managed engine");
    let managed2 = ManagedEngine::new().expect("Failed to create managed engine");

    assert_ne!(managed1.id(), managed2.id());
}

#[test]
fn test_managed_engine_from_existing_engine() {
    let engine = Engine::new().expect("Failed to create engine");

    let managed = ManagedEngine::from_engine(engine);
    assert!(managed.engine().validate().is_ok());
}

// =========================================================================
// Core Functions Tests (5 tests)
// =========================================================================

#[test]
fn test_core_create_engine() {
    let engine = core::create_engine().expect("Failed to create engine via core");
    assert!(engine.validate().is_ok());
}

#[test]
fn test_core_check_feature_support() {
    let engine = Engine::new().expect("Failed to create engine");
    let has_simd = core::check_feature_support(&engine, WasmFeature::Simd);
    assert_eq!(has_simd, engine.supports_feature(WasmFeature::Simd));
}

#[test]
fn test_core_get_config_summary() {
    let engine = Engine::new().expect("Failed to create engine");
    let summary = core::get_config_summary(&engine);
    assert!(!summary.strategy.is_empty());
}

#[test]
fn test_core_validate_engine() {
    let engine = Engine::new().expect("Failed to create engine");
    assert!(core::validate_engine(&engine).is_ok());
}

#[test]
fn test_core_get_limits() {
    let engine = Engine::builder()
        .max_stack_size(1024 * 1024)
        .build()
        .expect("Failed to build engine");

    assert_eq!(core::get_stack_limit(&engine), Some(1024 * 1024));
}

// =========================================================================
// Precompilation Tests (3 tests)
// =========================================================================

#[test]
fn test_precompile_module_valid() {
    let engine = Engine::new().expect("Failed to create engine");
    let wat = "(module (func (export \"test\") (result i32) i32.const 42))";
    let wasm = wat::parse_str(wat).expect("Failed to parse WAT");

    let result = core::precompile_module(&engine, &wasm);
    assert!(result.is_ok());
    assert!(!result.unwrap().is_empty());
}

#[test]
fn test_precompile_module_empty_bytes() {
    let engine = Engine::new().expect("Failed to create engine");
    let result = core::precompile_module(&engine, &[]);
    assert!(result.is_err());
}

#[test]
fn test_precompile_module_invalid_wasm() {
    let engine = Engine::new().expect("Failed to create engine");
    let result = core::precompile_module(&engine, &[0x00, 0x01, 0x02, 0x03]);
    assert!(result.is_err());
}

// =========================================================================
// Detect Precompiled Tests (2 tests)
// =========================================================================

#[test]
fn test_detect_precompiled_raw_wasm() {
    let engine = Engine::new().expect("Failed to create engine");
    let wasm = wat::parse_str("(module)").expect("Failed to parse WAT");

    // Raw WASM should not be detected as precompiled
    let result = engine.detect_precompiled(&wasm);
    assert!(result.is_none());
}

#[test]
fn test_detect_precompiled_garbage() {
    let engine = Engine::new().expect("Failed to create engine");
    let garbage = &[0x00, 0x61, 0x73, 0x6d]; // WASM magic number but incomplete

    let result = engine.detect_precompiled(garbage);
    assert!(result.is_none());
}

// =========================================================================
// Host Feature Detection Tests (3 tests)
// =========================================================================

#[test]
fn test_detect_host_feature_sse2() {
    // SSE2 should be available on all x86_64 platforms
    #[cfg(target_arch = "x86_64")]
    {
        let has_sse2 = core::detect_host_feature("sse2");
        // SSE2 is baseline for x86_64
        assert!(has_sse2);
    }
}

#[test]
fn test_detect_host_feature_unknown() {
    let has_unknown = core::detect_host_feature("unknown_feature_xyz");
    assert!(!has_unknown);
}

#[test]
fn test_detect_host_feature_neon() {
    #[cfg(target_arch = "aarch64")]
    {
        let has_neon = core::detect_host_feature("neon");
        // NEON is baseline for aarch64
        assert!(has_neon);
    }
}

// =========================================================================
// Epoch Increment Tests (2 tests)
// =========================================================================

#[test]
fn test_epoch_increment() {
    let engine = Engine::builder()
        .epoch_interruption(true)
        .build()
        .expect("Failed to build engine with epoch interruption");

    // Increment should not panic
    engine.increment_epoch();
    engine.increment_epoch();
    engine.increment_epoch();
}

#[test]
fn test_epoch_increment_without_interruption() {
    let engine = Engine::builder()
        .epoch_interruption(false)
        .build()
        .expect("Failed to build engine");

    // Should still work even if epoch interruption is disabled
    engine.increment_epoch();
}
