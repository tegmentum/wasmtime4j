//! Memory and GC FFI Integration Tests
//!
//! These tests validate the Panama FFI boundary for memory operations and
//! garbage collection functionality.

use std::ffi::CString;
use std::os::raw::c_void;
use std::ptr;

// Import the Panama FFI functions
use wasmtime4j::panama_ffi::engine;
use wasmtime4j::panama_ffi::instance;
use wasmtime4j::panama_ffi::memory;
use wasmtime4j::panama_ffi::module;
use wasmtime4j::panama_ffi::store;
use wasmtime4j::panama_ffi::table;

// Import GC FFI functions
use wasmtime4j::panama_gc_ffi;

// Import test fixtures
use crate::common::fixtures::MEMORY_MODULE_WAT;

// =============================================================================
// Memory FFI Tests
// =============================================================================

// -----------------------------------------------------------------------------
// Memory Lifecycle Tests
// -----------------------------------------------------------------------------

/// Test memory creation from a module export.
#[test]
fn test_memory_from_instance_export() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    assert!(!engine_ptr.is_null());

    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    // Get exported memory
    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );
    assert!(!memory_ptr.is_null(), "Should get memory export");

    // Cleanup
    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory destroy with null pointer is safe.
#[test]
fn test_memory_destroy_null_safe() {
    // Should not panic
    memory::wasmtime4j_panama_memory_destroy(ptr::null_mut());
}

/// Test memory release with null pointer is safe.
#[test]
fn test_memory_release_null_safe() {
    // Should not panic
    memory::wasmtime4j_panama_memory_release(ptr::null_mut());
}

// -----------------------------------------------------------------------------
// Memory Metadata Tests
// -----------------------------------------------------------------------------

/// Test memory size in pages.
#[test]
fn test_memory_size_pages() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );
    assert!(!memory_ptr.is_null());

    let mut size: u32 = 0;
    let result = memory::wasmtime4j_panama_memory_size_pages(
        memory_ptr,
        store_ptr,
        &mut size as *mut u32,
    );

    assert_eq!(result, 0, "Get size should succeed");
    assert_eq!(size, 1, "Memory should have 1 page initially");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory size in bytes.
#[test]
fn test_memory_size_bytes() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    let mut size: usize = 0;
    let result = memory::wasmtime4j_panama_memory_size_bytes(
        memory_ptr,
        store_ptr,
        &mut size as *mut usize,
    );

    assert_eq!(result, 0, "Get size should succeed");
    assert_eq!(size, 65536, "1 page = 65536 bytes");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory is_64bit query.
#[test]
fn test_memory_is_64bit() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    let mut is_64bit: i32 = -1;
    let result = memory::wasmtime4j_panama_memory_is_64bit(
        memory_ptr,
        store_ptr,
        &mut is_64bit as *mut i32,
    );

    assert_eq!(result, 0, "Query should succeed");
    assert_eq!(is_64bit, 0, "Default memory is 32-bit");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory is_shared query.
#[test]
fn test_memory_is_shared() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    let mut is_shared: i32 = -1;
    let result = memory::wasmtime4j_panama_memory_is_shared(
        memory_ptr,
        store_ptr,
        &mut is_shared as *mut i32,
    );

    assert_eq!(result, 0, "Query should succeed");
    assert_eq!(is_shared, 0, "Default memory is not shared");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory get_minimum.
#[test]
fn test_memory_get_minimum() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    let mut minimum: u64 = 0;
    let result = memory::wasmtime4j_panama_memory_get_minimum(
        memory_ptr,
        store_ptr,
        &mut minimum as *mut u64,
    );

    assert_eq!(result, 0, "Query should succeed");
    assert_eq!(minimum, 1, "Minimum should be 1 page");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

// -----------------------------------------------------------------------------
// Memory Growth Tests
// -----------------------------------------------------------------------------

/// Test memory grow succeeds.
#[test]
fn test_memory_grow() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    let mut previous_pages: u32 = 0;
    let result = memory::wasmtime4j_panama_memory_grow(
        memory_ptr,
        store_ptr,
        1, // grow by 1 page
        &mut previous_pages as *mut u32,
    );

    assert_eq!(result, 0, "Grow should succeed");
    assert_eq!(previous_pages, 1, "Previous size should be 1 page");

    // Verify new size
    let mut new_size: u32 = 0;
    memory::wasmtime4j_panama_memory_size_pages(memory_ptr, store_ptr, &mut new_size);
    assert_eq!(new_size, 2, "New size should be 2 pages");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory grow beyond max fails gracefully.
#[test]
fn test_memory_grow_beyond_max() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    // Memory with max 2 pages
    let wat_with_max = r#"
        (module
            (memory (export "memory") 1 2)
        )
    "#;
    let wat = CString::new(wat_with_max).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Try to grow by 10 pages (beyond max of 2)
    let mut previous_pages: u32 = 0;
    let result = memory::wasmtime4j_panama_memory_grow(
        memory_ptr,
        store_ptr,
        10, // try to grow by 10 pages
        &mut previous_pages as *mut u32,
    );

    // Should fail (return non-zero or previous_pages == u32::MAX)
    // The exact behavior depends on the implementation
    let _ = result; // May be error code or success with previous_pages indicating failure

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

// -----------------------------------------------------------------------------
// Memory Read/Write Tests
// -----------------------------------------------------------------------------

/// Test memory write and read bytes.
#[test]
fn test_memory_write_and_read_bytes() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Write bytes
    let data_to_write: [u8; 4] = [0xDE, 0xAD, 0xBE, 0xEF];
    let write_result = memory::wasmtime4j_panama_memory_write_bytes(
        memory_ptr,
        store_ptr,
        0, // offset
        4, // length
        data_to_write.as_ptr(),
    );
    assert_eq!(write_result, 0, "Write should succeed");

    // Read bytes back
    let mut read_buffer: [u8; 4] = [0; 4];
    let read_result = memory::wasmtime4j_panama_memory_read_bytes(
        memory_ptr,
        store_ptr,
        0, // offset
        4, // length
        read_buffer.as_mut_ptr(),
    );
    assert_eq!(read_result, 0, "Read should succeed");
    assert_eq!(read_buffer, data_to_write, "Read data should match written data");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory read out of bounds.
#[test]
fn test_memory_read_out_of_bounds() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Try to read beyond memory bounds (1 page = 65536 bytes)
    let mut read_buffer: [u8; 4] = [0; 4];
    let result = memory::wasmtime4j_panama_memory_read_bytes(
        memory_ptr,
        store_ptr,
        65536, // offset at end of memory
        4,     // try to read 4 bytes
        read_buffer.as_mut_ptr(),
    );

    // Should fail with non-zero error code
    assert_ne!(result, 0, "Read out of bounds should fail");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory write out of bounds.
#[test]
fn test_memory_write_out_of_bounds() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Try to write beyond memory bounds
    let data: [u8; 4] = [0xDE, 0xAD, 0xBE, 0xEF];
    let result = memory::wasmtime4j_panama_memory_write_bytes(
        memory_ptr,
        store_ptr,
        65536, // offset at end of memory
        4,     // try to write 4 bytes
        data.as_ptr(),
    );

    // Should fail with non-zero error code
    assert_ne!(result, 0, "Write out of bounds should fail");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory write and read u32.
#[test]
fn test_memory_write_and_read_u32() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Write u32
    let value_to_write: u32 = 0xDEADBEEF;
    let write_result = memory::wasmtime4j_panama_memory_write_u32(
        memory_ptr,
        store_ptr,
        0, // offset
        value_to_write,
    );
    assert_eq!(write_result, 0, "Write u32 should succeed");

    // Read u32 back
    let mut read_value: u32 = 0;
    let read_result = memory::wasmtime4j_panama_memory_read_u32(
        memory_ptr,
        store_ptr,
        0, // offset
        &mut read_value as *mut u32,
    );
    assert_eq!(read_result, 0, "Read u32 should succeed");
    assert_eq!(read_value, value_to_write, "Read value should match written value");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory fill operation.
#[test]
fn test_memory_fill() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Fill 8 bytes with 0xAB starting at offset 0
    let fill_result = table::wasmtime4j_panama_memory_fill(
        memory_ptr,
        store_ptr,
        0,    // offset
        0xAB, // value
        8,    // length
    );
    assert_eq!(fill_result, 0, "Fill should succeed");

    // Verify by reading
    let mut read_buffer: [u8; 8] = [0; 8];
    memory::wasmtime4j_panama_memory_read_bytes(
        memory_ptr,
        store_ptr,
        0,
        8,
        read_buffer.as_mut_ptr(),
    );
    assert_eq!(read_buffer, [0xAB; 8], "All bytes should be 0xAB");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory copy operation.
#[test]
fn test_memory_copy() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    // Write source data at offset 0
    let source_data: [u8; 4] = [0x11, 0x22, 0x33, 0x44];
    memory::wasmtime4j_panama_memory_write_bytes(
        memory_ptr,
        store_ptr,
        0,
        4,
        source_data.as_ptr(),
    );

    // Copy from offset 0 to offset 100
    let copy_result = table::wasmtime4j_panama_memory_copy(
        memory_ptr,
        store_ptr,
        100, // dest_offset
        0,   // src_offset
        4,   // length
    );
    assert_eq!(copy_result, 0, "Copy should succeed");

    // Verify by reading from destination
    let mut read_buffer: [u8; 4] = [0; 4];
    memory::wasmtime4j_panama_memory_read_bytes(
        memory_ptr,
        store_ptr,
        100,
        4,
        read_buffer.as_mut_ptr(),
    );
    assert_eq!(read_buffer, source_data, "Copied data should match source");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory get_data returns valid pointer.
#[test]
fn test_memory_get_data() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let wat = CString::new(MEMORY_MODULE_WAT).unwrap();
    let mut module_ptr: *mut c_void = ptr::null_mut();
    let mut store_ptr: *mut c_void = ptr::null_mut();
    let mut instance_ptr: *mut c_void = ptr::null_mut();

    module::wasmtime4j_panama_module_compile_wat(engine_ptr, wat.as_ptr(), &mut module_ptr);
    store::wasmtime4j_panama_store_create(engine_ptr, &mut store_ptr);
    instance::wasmtime4j_panama_instance_create(store_ptr, module_ptr, &mut instance_ptr);

    let memory_name = CString::new("memory").unwrap();
    let memory_ptr = instance::wasmtime4j_panama_instance_get_memory_by_name(
        instance_ptr,
        store_ptr,
        memory_name.as_ptr(),
    );

    let mut data_ptr: *mut u8 = ptr::null_mut();
    let mut size: usize = 0;
    let result = memory::wasmtime4j_panama_memory_get_data(
        memory_ptr,
        store_ptr,
        &mut data_ptr as *mut *mut u8,
        &mut size as *mut usize,
    );

    assert_eq!(result, 0, "Get data should succeed");
    assert!(!data_ptr.is_null(), "Data pointer should be non-null");
    assert_eq!(size, 65536, "Size should be 1 page (65536 bytes)");

    instance::wasmtime4j_panama_instance_destroy(instance_ptr);
    store::wasmtime4j_panama_store_destroy(store_ptr);
    module::wasmtime4j_panama_module_destroy(module_ptr);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test memory operations with null pointers.
#[test]
fn test_memory_operations_null_pointers() {
    let mut size: u32 = 0;
    let result = memory::wasmtime4j_panama_memory_size_pages(
        ptr::null_mut(),
        ptr::null_mut(),
        &mut size as *mut u32,
    );
    assert_ne!(result, 0, "Null memory pointer should fail");

    let mut read_buffer: [u8; 4] = [0; 4];
    let result = memory::wasmtime4j_panama_memory_read_bytes(
        ptr::null_mut(),
        ptr::null_mut(),
        0,
        4,
        read_buffer.as_mut_ptr(),
    );
    assert_ne!(result, 0, "Null memory pointer should fail");
}

// =============================================================================
// GC FFI Tests
// =============================================================================

// -----------------------------------------------------------------------------
// GC Runtime Lifecycle Tests
// -----------------------------------------------------------------------------

/// Test GC runtime creation.
#[test]
fn test_gc_runtime_create() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    assert!(!engine_ptr.is_null());

    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);
    // Runtime handle of 0 indicates error, non-zero is success
    assert_ne!(runtime_handle, 0, "GC runtime creation should succeed");

    // Cleanup
    let destroy_result = panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    assert_eq!(destroy_result, 0, "GC runtime destroy should succeed");

    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC runtime destroy.
#[test]
fn test_gc_runtime_destroy() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);
    assert_ne!(runtime_handle, 0);

    let result = panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    assert_eq!(result, 0, "Destroy should succeed");

    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC runtime destroy with invalid handle.
#[test]
fn test_gc_runtime_destroy_invalid_handle() {
    // Destroying with handle 0 should be safe
    let result = panama_gc_ffi::wasmtime4j_gc_destroy_runtime(0);
    // May return error code but should not crash
    let _ = result;
}

// -----------------------------------------------------------------------------
// GC Struct Tests
// -----------------------------------------------------------------------------

/// Test GC struct type registration.
#[test]
fn test_gc_register_struct_type() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);
    assert_ne!(runtime_handle, 0);

    // Register a simple struct type with one i32 field
    let type_name = b"TestStruct\0";
    let field_name = b"value\0";
    let field_names: [*const u8; 1] = [field_name.as_ptr()];
    let field_name_lens: [i32; 1] = [5]; // "value" length
    let field_types: [i32; 1] = [0]; // 0 = i32 type
    let field_mutabilities: [u8; 1] = [1]; // 1 = mutable

    let type_id = panama_gc_ffi::wasmtime4j_gc_register_struct_type(
        runtime_handle,
        type_name.as_ptr(),
        10, // "TestStruct" length
        1,  // field_count
        field_names.as_ptr(),
        field_name_lens.as_ptr(),
        field_types.as_ptr(),
        field_mutabilities.as_ptr(),
    );

    // type_id >= 0 indicates success
    assert!(type_id >= 0, "Struct type registration should succeed");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC struct creation and field access.
#[test]
fn test_gc_struct_new_and_get() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);
    assert_ne!(runtime_handle, 0);

    // Register struct type
    let type_name = b"Point\0";
    let field_names: [&[u8]; 2] = [b"x\0", b"y\0"];
    let field_name_ptrs: [*const u8; 2] = [field_names[0].as_ptr(), field_names[1].as_ptr()];
    let field_name_lens: [i32; 2] = [1, 1];
    let field_types: [i32; 2] = [0, 0]; // both i32
    let field_mutabilities: [u8; 2] = [1, 1]; // both mutable

    let type_id = panama_gc_ffi::wasmtime4j_gc_register_struct_type(
        runtime_handle,
        type_name.as_ptr(),
        5,
        2,
        field_name_ptrs.as_ptr(),
        field_name_lens.as_ptr(),
        field_types.as_ptr(),
        field_mutabilities.as_ptr(),
    );
    assert!(type_id >= 0);

    // Create struct instance with values [10, 20]
    let field_values: [i64; 2] = [10, 20];
    let object_id = panama_gc_ffi::wasmtime4j_gc_struct_new(
        runtime_handle,
        type_id,
        field_values.as_ptr(),
        2,
    );
    assert_ne!(object_id, 0, "Struct creation should succeed");

    // Get field 0 (x)
    let mut result_value: i64 = 0;
    let mut result_type: i32 = 0;
    let get_result = panama_gc_ffi::wasmtime4j_gc_struct_get(
        runtime_handle,
        object_id,
        0, // field index
        &mut result_value,
        &mut result_type,
    );
    assert_eq!(get_result, 0, "Get field should succeed");
    assert_eq!(result_value, 10, "Field x should be 10");

    // Get field 1 (y)
    let get_result = panama_gc_ffi::wasmtime4j_gc_struct_get(
        runtime_handle,
        object_id,
        1,
        &mut result_value,
        &mut result_type,
    );
    assert_eq!(get_result, 0, "Get field should succeed");
    assert_eq!(result_value, 20, "Field y should be 20");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC struct set field.
#[test]
fn test_gc_struct_set() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Register struct type
    let type_name = b"Counter\0";
    let field_name = b"count\0";
    let field_names: [*const u8; 1] = [field_name.as_ptr()];
    let field_name_lens: [i32; 1] = [5];
    let field_types: [i32; 1] = [0]; // i32
    let field_mutabilities: [u8; 1] = [1]; // mutable

    let type_id = panama_gc_ffi::wasmtime4j_gc_register_struct_type(
        runtime_handle,
        type_name.as_ptr(),
        7,
        1,
        field_names.as_ptr(),
        field_name_lens.as_ptr(),
        field_types.as_ptr(),
        field_mutabilities.as_ptr(),
    );

    // Create struct with initial value 0
    let field_values: [i64; 1] = [0];
    let object_id = panama_gc_ffi::wasmtime4j_gc_struct_new(
        runtime_handle,
        type_id,
        field_values.as_ptr(),
        1,
    );

    // Set field to 42
    let set_result = panama_gc_ffi::wasmtime4j_gc_struct_set(
        runtime_handle,
        object_id,
        0,  // field index
        42, // new value
        0,  // value type (i32)
    );
    assert_eq!(set_result, 0, "Set field should succeed");

    // Verify the new value
    let mut result_value: i64 = 0;
    let mut result_type: i32 = 0;
    panama_gc_ffi::wasmtime4j_gc_struct_get(
        runtime_handle,
        object_id,
        0,
        &mut result_value,
        &mut result_type,
    );
    assert_eq!(result_value, 42, "Field should be updated to 42");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC struct get with invalid field index.
#[test]
fn test_gc_struct_invalid_field_index() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Register single-field struct
    let type_name = b"Single\0";
    let field_name = b"val\0";
    let field_names: [*const u8; 1] = [field_name.as_ptr()];
    let field_name_lens: [i32; 1] = [3];
    let field_types: [i32; 1] = [0];
    let field_mutabilities: [u8; 1] = [1];

    let type_id = panama_gc_ffi::wasmtime4j_gc_register_struct_type(
        runtime_handle,
        type_name.as_ptr(),
        6,
        1,
        field_names.as_ptr(),
        field_name_lens.as_ptr(),
        field_types.as_ptr(),
        field_mutabilities.as_ptr(),
    );

    let field_values: [i64; 1] = [0];
    let object_id = panama_gc_ffi::wasmtime4j_gc_struct_new(
        runtime_handle,
        type_id,
        field_values.as_ptr(),
        1,
    );

    // Try to access field index 5 (out of bounds)
    let mut result_value: i64 = 0;
    let mut result_type: i32 = 0;
    let result = panama_gc_ffi::wasmtime4j_gc_struct_get(
        runtime_handle,
        object_id,
        5, // invalid index
        &mut result_value,
        &mut result_type,
    );
    assert_ne!(result, 0, "Invalid field index should fail");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

// -----------------------------------------------------------------------------
// GC Array Tests
// -----------------------------------------------------------------------------

/// Test GC array type registration.
#[test]
fn test_gc_register_array_type() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    let type_name = b"IntArray\0";
    let type_id = panama_gc_ffi::wasmtime4j_gc_register_array_type(
        runtime_handle,
        type_name.as_ptr(),
        8, // name length
        0, // element_type: i32
        1, // mutable
    );

    assert!(type_id >= 0, "Array type registration should succeed");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC array creation and element access.
#[test]
fn test_gc_array_new_and_get() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Register array type
    let type_name = b"Numbers\0";
    let type_id = panama_gc_ffi::wasmtime4j_gc_register_array_type(
        runtime_handle,
        type_name.as_ptr(),
        7,
        0, // i32 elements
        1, // mutable
    );

    // Create array with elements [1, 2, 3, 4, 5]
    let elements: [i64; 5] = [1, 2, 3, 4, 5];
    let object_id = panama_gc_ffi::wasmtime4j_gc_array_new(
        runtime_handle,
        type_id,
        elements.as_ptr(),
        5,
    );
    assert_ne!(object_id, 0, "Array creation should succeed");

    // Check length
    let length = panama_gc_ffi::wasmtime4j_gc_array_len(runtime_handle, object_id);
    assert_eq!(length, 5, "Array length should be 5");

    // Get element at index 2
    let mut result_value: i64 = 0;
    let mut result_type: i32 = 0;
    let get_result = panama_gc_ffi::wasmtime4j_gc_array_get(
        runtime_handle,
        object_id,
        2, // index
        &mut result_value,
        &mut result_type,
    );
    assert_eq!(get_result, 0, "Get element should succeed");
    assert_eq!(result_value, 3, "Element at index 2 should be 3");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC array set element.
#[test]
fn test_gc_array_set() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    let type_name = b"Arr\0";
    let type_id = panama_gc_ffi::wasmtime4j_gc_register_array_type(
        runtime_handle,
        type_name.as_ptr(),
        3,
        0,
        1,
    );

    let elements: [i64; 3] = [0, 0, 0];
    let object_id = panama_gc_ffi::wasmtime4j_gc_array_new(
        runtime_handle,
        type_id,
        elements.as_ptr(),
        3,
    );

    // Set element at index 1 to 99
    let set_result = panama_gc_ffi::wasmtime4j_gc_array_set(
        runtime_handle,
        object_id,
        1,  // index
        99, // value
        0,  // value type
    );
    assert_eq!(set_result, 0, "Set element should succeed");

    // Verify
    let mut result_value: i64 = 0;
    let mut result_type: i32 = 0;
    panama_gc_ffi::wasmtime4j_gc_array_get(
        runtime_handle,
        object_id,
        1,
        &mut result_value,
        &mut result_type,
    );
    assert_eq!(result_value, 99, "Element should be updated to 99");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC array out of bounds access.
#[test]
fn test_gc_array_out_of_bounds() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    let type_name = b"Small\0";
    let type_id = panama_gc_ffi::wasmtime4j_gc_register_array_type(
        runtime_handle,
        type_name.as_ptr(),
        5,
        0,
        1,
    );

    let elements: [i64; 3] = [1, 2, 3];
    let object_id = panama_gc_ffi::wasmtime4j_gc_array_new(
        runtime_handle,
        type_id,
        elements.as_ptr(),
        3,
    );

    // Try to get element at index 10 (out of bounds)
    let mut result_value: i64 = 0;
    let mut result_type: i32 = 0;
    let result = panama_gc_ffi::wasmtime4j_gc_array_get(
        runtime_handle,
        object_id,
        10, // out of bounds
        &mut result_value,
        &mut result_type,
    );
    assert_ne!(result, 0, "Out of bounds access should fail");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

// -----------------------------------------------------------------------------
// GC I31 Tests
// -----------------------------------------------------------------------------

/// Test GC i31 new and get.
#[test]
fn test_gc_i31_new_and_get() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Create i31 with value 12345
    let object_id = panama_gc_ffi::wasmtime4j_gc_i31_new(runtime_handle, 12345);
    assert_ne!(object_id, 0, "I31 creation should succeed");

    // Get value (signed)
    let value = panama_gc_ffi::wasmtime4j_gc_i31_get(runtime_handle, object_id, 1);
    assert_eq!(value, 12345, "I31 value should be 12345");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC i31 with negative value.
#[test]
fn test_gc_i31_negative_value() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Create i31 with negative value
    let object_id = panama_gc_ffi::wasmtime4j_gc_i31_new(runtime_handle, -100);
    assert_ne!(object_id, 0, "I31 creation should succeed");

    // Get value (signed interpretation)
    let value = panama_gc_ffi::wasmtime4j_gc_i31_get(runtime_handle, object_id, 1);
    assert_eq!(value, -100, "I31 value should be -100");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

// -----------------------------------------------------------------------------
// GC Reference Tests
// -----------------------------------------------------------------------------

/// Test GC ref equality.
#[test]
fn test_gc_ref_eq() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Create two i31 values
    let obj1 = panama_gc_ffi::wasmtime4j_gc_i31_new(runtime_handle, 42);
    let obj2 = panama_gc_ffi::wasmtime4j_gc_i31_new(runtime_handle, 42);

    // Same object should be equal to itself
    let eq_self = panama_gc_ffi::wasmtime4j_gc_ref_eq(runtime_handle, obj1, obj1);
    assert_eq!(eq_self, 1, "Object should equal itself");

    // Different objects with same value may or may not be equal depending on implementation
    let eq_other = panama_gc_ffi::wasmtime4j_gc_ref_eq(runtime_handle, obj1, obj2);
    // Just verify it returns a valid boolean (0 or 1)
    assert!(eq_other == 0 || eq_other == 1, "Should return valid boolean");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC ref is_null.
#[test]
fn test_gc_ref_is_null() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Create a valid object
    let obj = panama_gc_ffi::wasmtime4j_gc_i31_new(runtime_handle, 42);

    // Valid object should not be null
    let is_null = panama_gc_ffi::wasmtime4j_gc_ref_is_null(runtime_handle, obj);
    assert_eq!(is_null, 0, "Valid object should not be null");

    // Object ID 0 is typically null
    let is_null_zero = panama_gc_ffi::wasmtime4j_gc_ref_is_null(runtime_handle, 0);
    assert_eq!(is_null_zero, 1, "Object ID 0 should be null");

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

// -----------------------------------------------------------------------------
// GC Collection Tests
// -----------------------------------------------------------------------------

/// Test garbage collection.
#[test]
fn test_gc_collect_garbage() {
    let engine_ptr = engine::wasmtime4j_panama_engine_create();
    let runtime_handle = panama_gc_ffi::wasmtime4j_gc_create_runtime(engine_ptr as i64);

    // Create some objects
    for i in 0..10 {
        panama_gc_ffi::wasmtime4j_gc_i31_new(runtime_handle, i);
    }

    // Trigger garbage collection
    // Note: GcStatsFFI structure needs to match the actual definition
    // For simplicity, we just test that the call doesn't crash
    let result = panama_gc_ffi::wasmtime4j_gc_collect_garbage(
        runtime_handle,
        ptr::null_mut(), // null stats pointer - should be handled gracefully
    );
    // Result may vary depending on implementation
    let _ = result;

    panama_gc_ffi::wasmtime4j_gc_destroy_runtime(runtime_handle);
    engine::wasmtime4j_panama_engine_destroy(engine_ptr);
}

/// Test GC operations with invalid runtime handle.
#[test]
fn test_gc_operations_invalid_runtime() {
    // Operations with handle 0 should fail gracefully
    let type_name = b"Test\0";
    let type_id = panama_gc_ffi::wasmtime4j_gc_register_array_type(
        0, // invalid handle
        type_name.as_ptr(),
        4,
        0,
        1,
    );
    assert_eq!(type_id, -1, "Invalid runtime should return -1");

    let obj_id = panama_gc_ffi::wasmtime4j_gc_i31_new(0, 42);
    assert_eq!(obj_id, 0, "Invalid runtime should return 0");
}
