//! FFI Lifecycle and Error Handling Tests
//!
//! These tests focus on cross-cutting concerns like resource lifecycle management,
//! destroy order dependencies, memory leak detection, and error code validation
//! across the Panama FFI boundary.

use std::ffi::CString;
use std::os::raw::c_void;
use std::ptr;

// Import the Panama FFI functions
use wasmtime4j::panama::engine;
use wasmtime4j::panama::instance;
use wasmtime4j::panama::module;
use wasmtime4j::panama::store;

// Import the ffi_common module for destroyed pointers registry
use wasmtime4j::ffi_common::resource_destruction::{
    clear_destroyed_pointers, is_fake_pointer, DESTROYED_POINTERS,
};

// Import test fixtures
use crate::common::fixtures::{ARITHMETIC_MODULE_WAT, NOP_MODULE_WAT};

// =============================================================================
// Cross-cutting Lifecycle Tests (~12 tests)
// =============================================================================

/// Test full lifecycle: Engine -> Store -> Module -> Instance creation and destruction.
#[test]
fn test_full_lifecycle_engine_store_module_instance() {
    // Create resources in order
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    assert!(!engine_ptr.is_null(), "Engine creation failed");

    let mut store_ptr: *mut c_void = ptr::null_mut();
    let store_result = store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    assert_eq!(store_result, 0, "Store creation failed");
    assert!(!store_ptr.is_null());

    let wat = CString::new(ARITHMETIC_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let module_result =
        module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    assert_eq!(module_result, 0, "Module compilation failed");
    assert!(!module_ptr.is_null());

    let mut instance_ptr: *mut c_void = ptr::null_mut();
    let instance_result =
        instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);
    assert_eq!(instance_result, 0, "Instance creation failed");
    assert!(!instance_ptr.is_null());

    // Destroy in reverse order
    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test that destroying instance before store is safe.
#[test]
fn test_destroy_order_instance_before_store() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);

    let wat = CString::new(NOP_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);

    let mut instance_ptr: *mut c_void = ptr::null_mut();
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    // Destroy instance first (correct order)
    instance::wasmtime4j_panama_instance_destroy(instance_ptr);

    // Then store
    store::wasmtime4j_panama_store_destroy(store_ptr);

    // Module can be destroyed at any point as it doesn't depend on store
    module::wasmtime4j_panama_module_destroy(module_ptr);

    // Engine last
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test that destroying store before engine is safe.
#[test]
fn test_destroy_order_store_before_engine() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);

    // Destroy store before engine
    store::wasmtime4j_panama_store_destroy(store_ptr);

    // Then engine
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test 100 create/destroy cycles to detect memory leaks.
#[test]
fn test_resource_leak_100_cycles() {
    for i in 0..100 {
        let engine_ptr = engine::wasmtime4j_panama_engine_create();
        assert!(
            !engine_ptr.is_null(),
            "Engine creation failed at iteration {}",
            i
        );

        let mut store_ptr: *mut c_void = ptr::null_mut();
        store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
        assert!(
            !store_ptr.is_null(),
            "Store creation failed at iteration {}",
            i
        );

        let wat = CString::new(NOP_MODULE_WAT).unwrap();
        let mut module_ptr: *mut c_void = ptr::null_mut();
        module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
        assert!(
            !module_ptr.is_null(),
            "Module compilation failed at iteration {}",
            i
        );

        let mut instance_ptr: *mut c_void = ptr::null_mut();
        instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);
        assert!(
            !instance_ptr.is_null(),
            "Instance creation failed at iteration {}",
            i
        );

        // Cleanup
        instance::wasmtime4j_panama_instance_destroy(instance_ptr);
        module::wasmtime4j_panama_module_destroy(module_ptr);
        store::wasmtime4j_panama_store_destroy(store_ptr);
        engine::wasmtime4j_panama_engine_destroy(engine_ptr);
    }

    // If we reach here without running out of memory, no leaks detected
}

/// Test destroyed pointers registry tracks properly.
#[test]
fn test_destroyed_pointers_registry_behavior() {
    // Clear the registry before test
    let _cleared = clear_destroyed_pointers();

    // Create and destroy an engine
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    assert!(!engine_ptr.is_null());

    // Before destruction, pointer should not be in the registry
    {
        let destroyed = DESTROYED_POINTERS.lock().unwrap();
        let ptr_addr = engine_ptr as usize;
        assert!(
            !destroyed.contains(&ptr_addr),
            "Pointer should not be in registry before destruction"
        );
    }

    // Destroy the engine
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);

    // After successful destruction, the address should be removed from the registry
    // (to allow address reuse by the allocator)
    {
        let destroyed = DESTROYED_POINTERS.lock().unwrap();
        let ptr_addr = engine_ptr as usize;
        // The safe_destroy function removes the address after successful destruction
        assert!(
            !destroyed.contains(&ptr_addr),
            "Pointer should be removed after successful destruction"
        );
    }
}

/// Test that safe_destroy returns false for null pointer.
#[test]
fn test_safe_destroy_null_returns_false() {
    // Null destroy calls should be safe and just return without action
    // We can't directly call safe_destroy from tests, but we test via the FFI functions
    engine::wasmtime4j_panama_engine_destroy(ptr::null_mut());
    store::wasmtime4j_panama_store_destroy(ptr::null_mut());
    module::wasmtime4j_panama_module_destroy(ptr::null_mut());
    instance::wasmtime4j_panama_instance_destroy(ptr::null_mut());
    // If we reach here without panic, the test passes
}

/// Test that multiple null destroys are safe.
/// Note: After a valid pointer is successfully destroyed, the memory is freed
/// and the address is removed from the registry to allow allocator reuse.
/// Attempting to destroy the same pointer again is undefined behavior at the
/// Rust level because we'd be freeing freed memory. We only test null safety.
#[test]
fn test_safe_destroy_multiple_null_destroys_safe() {
    // Multiple null destroys should not panic
    engine::wasmtime4j_panama_engine_destroy(ptr::null_mut());
    engine::wasmtime4j_panama_engine_destroy(ptr::null_mut());
    engine::wasmtime4j_panama_engine_destroy(ptr::null_mut());
    store::wasmtime4j_panama_store_destroy(ptr::null_mut());
    module::wasmtime4j_panama_module_destroy(ptr::null_mut());
    instance::wasmtime4j_panama_instance_destroy(ptr::null_mut());
}

/// Test that concurrent registry access is thread-safe.
/// Multiple threads try to mark the same address as destroyed simultaneously.
#[test]
fn test_registry_concurrent_access() {
    use std::sync::Arc;
    use std::thread;

    let fake_ptr: *mut c_void = 0x7FFF_0000_0000_5000 as *mut c_void;
    let ptr_addr = fake_ptr as usize;
    let ptr_addr_arc = Arc::new(ptr_addr);

    // Spawn multiple threads that all try to insert the same address
    let handles: Vec<_> = (0..10)
        .map(|_| {
            let addr = Arc::clone(&ptr_addr_arc);
            thread::spawn(move || {
                let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
                destroyed.insert(*addr);
            })
        })
        .collect();

    // Wait for all threads
    for handle in handles {
        handle.join().unwrap();
    }

    // Verify address is in registry (inserted by at least one thread)
    {
        let destroyed = DESTROYED_POINTERS.lock().unwrap();
        assert!(
            destroyed.contains(&ptr_addr),
            "Address should be in registry after concurrent inserts"
        );
    }

    // Clean up
    {
        let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
        destroyed.remove(&ptr_addr);
    }
}

/// Test fake pointer detection for low addresses.
#[test]
fn test_fake_pointer_detection_low_address() {
    // Addresses below MIN_VALID_ADDRESS (0x1000) should be detected as fake
    assert!(is_fake_pointer(0x0), "Address 0 should be fake");
    assert!(is_fake_pointer(0x100), "Address 0x100 should be fake");
    assert!(is_fake_pointer(0xFFF), "Address 0xFFF should be fake");

    // Addresses at or above MIN_VALID_ADDRESS might be valid
    // (but we can't be certain without the magic prefix check)
    // These should not be flagged as fake unless they have the magic prefix
    assert!(
        !is_fake_pointer(0x10000000),
        "High address without magic should not be fake"
    );
}

/// Test fake pointer detection for magic prefix.
#[test]
fn test_fake_pointer_detection_magic_prefix() {
    // Addresses with the magic prefix should be detected as fake
    const FAKE_POINTER_MAGIC: usize = 0x1234560000000000;

    assert!(
        is_fake_pointer(FAKE_POINTER_MAGIC | 1),
        "Magic prefix address should be fake"
    );
    assert!(
        is_fake_pointer(FAKE_POINTER_MAGIC | 0xFFFF),
        "Magic prefix address should be fake"
    );
}

/// Test that multiple stores from the same engine work correctly.
#[test]
fn test_multiple_stores_same_engine() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    assert!(!engine_ptr.is_null());

    // Create multiple stores from the same engine
    let mut store_ptrs: [*mut c_void; 5] = [ptr::null_mut(); 5];

    for (i, store_ptr) in store_ptrs.iter_mut().enumerate() {
        let result = store::wasmtime4j_panama_store_create(engine_ptr, store_ptr);
        assert_eq!(result, 0, "Store {} creation should succeed", i);
        assert!(
            !store_ptr.is_null(),
            "Store {} pointer should be non-null",
            i
        );
    }

    // Destroy all stores
    for store_ptr in store_ptrs.iter() {
        store::wasmtime4j_panama_store_destroy(*store_ptr);
    }

    // Destroy engine
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test that multiple modules from the same engine work correctly.
#[test]
fn test_multiple_modules_same_engine() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    assert!(!engine_ptr.is_null());

    let wat = CString::new(NOP_MODULE_WAT).unwrap();

    // Create multiple modules from the same engine
    let mut module_ptrs: [*mut c_void; 5] = [ptr::null_mut(); 5];

    for (i, module_ptr) in module_ptrs.iter_mut().enumerate() {
        let result =
            module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), module_ptr);
        assert_eq!(result, 0, "Module {} compilation should succeed", i);
        assert!(
            !module_ptr.is_null(),
            "Module {} pointer should be non-null",
            i
        );
    }

    // Destroy all modules
    for module_ptr in module_ptrs.iter() {
        module::wasmtime4j_panama_module_destroy(*module_ptr);
    }

    // Destroy engine
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test concurrent-like creation (rapid sequential creates).
#[test]
fn test_rapid_sequential_creates() {
    // Rapidly create and collect resources, then destroy all at once
    let mut engines: Vec<*mut c_void> = Vec::new();
    let mut stores: Vec<*mut c_void> = Vec::new();
    let mut modules: Vec<*mut c_void> = Vec::new();

    let wat = CString::new(NOP_MODULE_WAT).unwrap();

    for _ in 0..20 {
        let engine = engine::wasmtime4j_panama_engine_create();
        assert!(!engine.is_null());

        let mut store: *mut c_void = ptr::null_mut();
        store::wasmtime4j_panama_store_create(engine, &mut store);
        assert!(!store.is_null());

        let mut module: *mut c_void = ptr::null_mut();
        module::wasmtime4j_panama_module_compile_wat(engine, wat.as_ptr(), &mut module);
        assert!(!module.is_null());

        engines.push(engine);
        stores.push(store);
        modules.push(module);
    }

    // Now destroy all in reverse order
    for module in modules.iter().rev() {
        module::wasmtime4j_panama_module_destroy(*module);
    }

    for store in stores.iter().rev() {
        store::wasmtime4j_panama_store_destroy(*store);
    }

    for engine in engines.iter().rev() {
        engine::wasmtime4j_panama_engine_destroy(*engine);
    }
}

// =============================================================================
// Error Handling Tests (~13 tests)
// =============================================================================

/// Test that FFI_SUCCESS is 0.
#[test]
fn test_error_code_success_is_zero() {
    // Create a valid engine - result should be non-null (success)
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    // For functions that return pointers, non-null is success
    assert!(
        !engine_ptr.is_null(),
        "Success should return non-null pointer"
    );

    // For functions that return int codes
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let result = store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    assert_eq!(result, 0, "FFI_SUCCESS should be 0");

    store::wasmtime4j_panama_store_destroy(store_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test that invalid parameter returns negative error code.
#[test]
fn test_error_code_invalid_param_negative() {
    // Operations on null pointers should return negative error codes
    let result = engine::wasmtime4j_panama_engine_is_fuel_enabled(ptr::null_mut());
    assert!(result < 0, "Invalid param should return negative code");

    let result = engine::wasmtime4j_panama_engine_validate(ptr::null_mut());
    assert!(result < 0, "Invalid param should return negative code");

    let result = store::wasmtime4j_panama_store_validate(ptr::null_mut());
    assert!(result != 0, "Invalid param should return non-zero code");
}

/// Test compilation error returns non-zero.
#[test]
fn test_error_code_compilation_error() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let invalid_wasm: [u8; 4] = [0x00, 0x01, 0x02, 0x03];
    let mut module_ptr: *mut c_void = ptr::null_mut();

    let result = module::wasmtime4j_panama_module_compile(
        engine_ptr,
        invalid_wasm.as_ptr(),
        invalid_wasm.len(),
        &mut module_ptr,
    );

    assert_ne!(result, 0, "Compilation error should return non-zero code");
    assert!(
        module_ptr.is_null(),
        "Failed compilation should return null module"
    );

    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test null pointer validation.
#[test]
fn test_validate_pointer_null() {
    // All these should handle null gracefully
    assert_eq!(
        engine::wasmtime4j_panama_engine_is_fuel_enabled(ptr::null_mut()),
        -1
    );
    assert_eq!(
        engine::wasmtime4j_panama_engine_validate(ptr::null_mut()),
        -1
    );
    assert_eq!(
        module::wasmtime4j_panama_module_get_export_count(ptr::null_mut()),
        -1
    );
}

/// Test that accessing a freed pointer is not safe to test.
/// Note: After destruction, the pointer is freed and the address removed from
/// the registry. We cannot safely test operations on freed pointers as this
/// would be undefined behavior. Instead, we test that null pointers are handled.
#[test]
fn test_validate_null_pointer_operations() {
    // Operations on null pointers should return error codes
    let result = engine::wasmtime4j_panama_engine_is_fuel_enabled(ptr::null_mut());
    assert_eq!(result, -1, "Null pointer should return error");

    let result = engine::wasmtime4j_panama_engine_validate(ptr::null_mut());
    assert_eq!(result, -1, "Null pointer should return error");

    let result = store::wasmtime4j_panama_store_validate(ptr::null_mut());
    assert_ne!(result, 0, "Null pointer should return error");
}

/// Test null string validation.
#[test]
fn test_validate_string_null() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();

    // Compile with null WAT string
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let result =
        module::wasmtime4j_panama_module_compile_wat(engine_ptr, ptr::null(), &mut module_ptr);
    assert_ne!(result, 0, "Null string should fail compilation");

    // Has export with null name
    let wat = CString::new(NOP_MODULE_WAT).unwrap();
    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);

    if !module_ptr.is_null() {
        let result = module::wasmtime4j_panama_module_has_export(module_ptr, ptr::null());
        // Should return 0 (not found) or -1 (error), not crash
        assert!(result <= 0, "Null export name should not be found");
        module::wasmtime4j_panama_module_destroy(module_ptr);
    }

    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test null byte array validation.
#[test]
fn test_validate_byte_array_null() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let mut module_ptr: *mut c_void = ptr::null_mut();

    // Compile with null bytes
    let result =
        module::wasmtime4j_panama_module_compile(engine_ptr, ptr::null(), 0, &mut module_ptr);
    assert_ne!(result, 0, "Null bytes should fail compilation");
    assert!(module_ptr.is_null(), "Module should be null on failure");

    // Validate with null bytes
    let result = module::wasmtime4j_panama_module_validate(ptr::null(), 0);
    assert_ne!(result, 0, "Null bytes should fail validation");

    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test zero handle validation.
#[test]
fn test_validate_handle_zero() {
    // Zero/null handle operations should be safe
    let null_ptr: *mut c_void = ptr::null_mut();

    // These should all handle null/zero gracefully
    engine::wasmtime4j_panama_engine_destroy(null_ptr);
    store::wasmtime4j_panama_store_destroy(null_ptr);
    module::wasmtime4j_panama_module_destroy(null_ptr);
    instance::wasmtime4j_panama_instance_destroy(null_ptr);
    // If we reach here without panic, test passes
}

/// Test store operations with null engine pointer.
#[test]
fn test_store_create_null_engine() {
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let result = store::wasmtime4j_panama_store_create(ptr::null_mut(), &mut store_ptr);
    assert_ne!(result, 0, "Store creation with null engine should fail");
    assert!(
        store_ptr.is_null(),
        "Store pointer should be null on failure"
    );
}

/// Test module operations with null engine pointer.
#[test]
fn test_module_compile_null_engine() {
    let wat = CString::new(NOP_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();

    let result = module::wasmtime4j_panama_module_compile_wat(
        ptr::null_mut(),
        wat.as_ptr(),
        &mut module_ptr,
    );
    assert_ne!(result, 0, "Module compilation with null engine should fail");
    assert!(
        module_ptr.is_null(),
        "Module pointer should be null on failure"
    );
}

/// Test instance operations with null pointers.
#[test]
fn test_instance_create_null_pointers() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(NOP_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);

    // Null store
    let mut instance_ptr: *mut c_void = ptr::null_mut();
    let result =
        instance::wasmtime4j_panama_instance_create(ptr::null_mut(), module_ptr, &mut instance_ptr);
    assert_ne!(result, 0, "Instance creation with null store should fail");

    // Null module
    let result =
        instance::wasmtime4j_panama_instance_create(store_ptr, ptr::null_mut(), &mut instance_ptr);
    assert_ne!(result, 0, "Instance creation with null module should fail");

    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}
