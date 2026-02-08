//! Tests for the memory module

use super::*;
use crate::engine::Engine;
use crate::error::WasmtimeResult;

// Use the global shared engine to reduce wasmtime GLOBAL_CODE registry accumulation
fn shared_engine() -> Engine {
    crate::engine::get_shared_engine()
}

#[test]
fn test_memory_creation() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    assert_eq!(memory.size_pages(&store).unwrap(), 1);
    assert_eq!(memory.size_bytes(&store).unwrap(), 65536);
}

#[test]
fn test_memory_growth() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let previous_pages = memory.grow(&mut store, 1).expect("Failed to grow memory");

    assert_eq!(previous_pages, 1);
    assert_eq!(memory.size_pages(&store).unwrap(), 2);
}

#[test]
fn test_bounds_checking() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    // Should succeed - within bounds
    let result = memory.read_bytes(&store, 0, 100);
    assert!(result.is_ok());

    // Should fail - out of bounds
    let result = memory.read_bytes(&store, 65536, 1);
    assert!(result.is_err());
    assert!(matches!(result.unwrap_err(), WasmtimeError::Memory { .. }));
}

#[test]
fn test_memory_statistics() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    // Perform some operations
    memory.read_bytes(&store, 0, 100).expect("Read failed");
    memory.write_bytes(&mut store, 0, &[1, 2, 3, 4]).expect("Write failed");

    let usage = memory.get_usage(&store).expect("Failed to get usage");
    assert_eq!(usage.current_pages, 1);
    assert_eq!(usage.read_count, 1);
    assert_eq!(usage.write_count, 1);
}

#[test]
fn test_memory_registry() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");
    let registry = MemoryRegistry::new();

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let memory_id = registry.register(memory).expect("Failed to register memory");

    let retrieved = registry.get(memory_id).expect("Failed to get memory");
    assert_eq!(retrieved.size_pages(&store).unwrap(), 1);

    registry.unregister(memory_id).expect("Failed to unregister memory");
    assert!(registry.get(memory_id).is_err());
}

#[test]
fn test_memory_handle_validation() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let validated_ptr =
        core::create_validated_memory(memory).expect("Failed to create validated memory");

    // Validate the handle works
    unsafe {
        assert!(
            core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_ok()
        );
        let memory_ref = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
        assert!(memory_ref.is_ok());
    }

    // Test destruction and use-after-free detection
    unsafe {
        core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);

        // Now validation should fail
        assert!(
            core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_err()
        );
    }
}

#[test]
fn test_null_pointer_validation() {
    unsafe {
        // Test null pointer validation
        assert!(core::validate_memory_handle(std::ptr::null()).is_err());
        assert!(core::validate_store_handle(std::ptr::null()).is_err());

        // Test getting memory from null pointer
        let result = core::get_memory_ref(std::ptr::null());
        assert!(result.is_err());
    }
}

#[test]
fn test_invalid_pointer_validation() {
    unsafe {
        // Test with invalid but non-null pointer
        let invalid_ptr = 0xDEADBEEF as *const std::os::raw::c_void;
        assert!(core::validate_memory_handle(invalid_ptr).is_err());

        // Test getting memory from invalid pointer
        let result = core::get_memory_ref(invalid_ptr);
        assert!(result.is_err());
    }
}

#[test]
fn test_memory_access_counting() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let validated_ptr =
        core::create_validated_memory(memory).expect("Failed to create validated memory");

    unsafe {
        let validated_memory = &*(validated_ptr as *const core::ValidatedMemory);

        // Initial access count should be 0
        assert_eq!(validated_memory.get_access_count(), 0);

        // Access the memory a few times
        let _memory1 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
        let _memory2 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);
        let _memory3 = core::get_memory_ref(validated_ptr as *const std::os::raw::c_void);

        // Access count should have incremented (note: validation also increments count)
        assert!(validated_memory.get_access_count() >= 3);

        // Cleanup
        core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
    }
}

#[test]
fn test_handle_registry_diagnostics() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    // Create some memory handles
    let memory1 = Memory::new(&mut store, 1).expect("Failed to create memory 1");
    let memory2 = Memory::new(&mut store, 1).expect("Failed to create memory 2");

    let validated_ptr1 =
        core::create_validated_memory(memory1).expect("Failed to create validated memory 1");
    let validated_ptr2 =
        core::create_validated_memory(memory2).expect("Failed to create validated memory 2");

    // Verify handles are valid and accessible
    assert!(!validated_ptr1.is_null(), "validated_ptr1 should not be null");
    assert!(!validated_ptr2.is_null(), "validated_ptr2 should not be null");

    // Access the memories to verify they work
    unsafe {
        let mem1 = core::get_memory_ref(validated_ptr1 as *const std::os::raw::c_void);
        let mem2 = core::get_memory_ref(validated_ptr2 as *const std::os::raw::c_void);
        assert!(mem1.is_ok(), "Should be able to access memory 1");
        assert!(mem2.is_ok(), "Should be able to access memory 2");
    }

    // Verify diagnostics function works (just check it returns valid data)
    let (handle_count, access_count) =
        core::get_memory_handle_diagnostics().expect("Failed to get diagnostics");
    assert!(
        handle_count >= 2,
        "Should have at least our 2 handles, got {}",
        handle_count
    );
    assert!(
        access_count >= 2,
        "Should have at least our 2 accesses, got {}",
        access_count
    );

    // Cleanup our handles
    unsafe {
        core::destroy_memory(validated_ptr1 as *mut std::os::raw::c_void);
        core::destroy_memory(validated_ptr2 as *mut std::os::raw::c_void);
    }

    // Verify our handles are no longer accessible after cleanup
    unsafe {
        let mem1_after = core::get_memory_ref(validated_ptr1 as *const std::os::raw::c_void);
        let mem2_after = core::get_memory_ref(validated_ptr2 as *const std::os::raw::c_void);
        assert!(mem1_after.is_err(), "Memory 1 should be destroyed");
        assert!(mem2_after.is_err(), "Memory 2 should be destroyed");
    }
}

#[test]
fn test_corrupted_handle_detection() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let validated_ptr =
        core::create_validated_memory(memory).expect("Failed to create validated memory");

    unsafe {
        // First validate it works
        assert!(
            core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void).is_ok()
        );

        // Corrupt the magic number by directly manipulating memory
        let validated_memory_ptr = validated_ptr as *mut u64; // First field is magic
        *validated_memory_ptr = 0xBADCAFE; // Wrong magic

        // Now validation should fail due to corrupted magic
        let result = core::validate_memory_handle(validated_ptr as *const std::os::raw::c_void);
        assert!(result.is_err());
        let error_msg = result.unwrap_err().to_string();
        assert!(error_msg.contains("invalid magic number"));

        // Cleanup (this will also fail validation but still clean up)
        core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
    }
}

#[test]
fn test_thread_safety_basic() {
    use std::thread;

    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let validated_ptr =
        core::create_validated_memory(memory).expect("Failed to create validated memory");

    let ptr_copy = validated_ptr as usize;

    // Spawn threads to access the handle concurrently
    let handles: Vec<_> = (0..4)
        .map(|_| {
            let ptr = ptr_copy;
            thread::spawn(move || unsafe {
                for _ in 0..10 {
                    let result =
                        core::validate_memory_handle(ptr as *const std::os::raw::c_void);
                    // We don't assert success here because the memory might be destroyed
                    // by another thread, but validation should not crash
                    let _ = result;
                }
            })
        })
        .collect();

    // Wait for all threads to complete
    for handle in handles {
        handle.join().expect("Thread panicked");
    }

    // Cleanup
    unsafe {
        core::destroy_memory(validated_ptr as *mut std::os::raw::c_void);
    }
}

// === Typed I/O operations tests ===

#[test]
fn test_read_typed_u8() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    memory.write_bytes(&mut store, 0, &[42u8]).expect("Write failed");
    let value: u8 = memory
        .read_typed(&store, 0, MemoryDataType::U8)
        .expect("Read failed");
    assert_eq!(value, 42u8, "U8 read should return the written value");
}

#[test]
fn test_read_typed_u16() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: u16 = 0x1234;
    memory
        .write_bytes(&mut store, 0, &test_value.to_le_bytes())
        .expect("Write failed");
    let value: u16 = memory
        .read_typed(&store, 0, MemoryDataType::U16Le)
        .expect("Read failed");
    assert_eq!(value, test_value, "U16 read should return the written value");
}

#[test]
fn test_read_typed_u32() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: u32 = 0x12345678;
    memory
        .write_bytes(&mut store, 0, &test_value.to_le_bytes())
        .expect("Write failed");
    let value: u32 = memory
        .read_typed(&store, 0, MemoryDataType::U32Le)
        .expect("Read failed");
    assert_eq!(value, test_value, "U32 read should return the written value");
}

#[test]
fn test_read_typed_u64() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: u64 = 0x123456789ABCDEF0;
    memory
        .write_bytes(&mut store, 8, &test_value.to_le_bytes())
        .expect("Write failed");
    let value: u64 = memory
        .read_typed(&store, 8, MemoryDataType::U64Le)
        .expect("Read failed");
    assert_eq!(value, test_value, "U64 read should return the written value");
}

#[test]
fn test_write_typed_u8() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    memory
        .write_typed(&mut store, 0, 99u8, MemoryDataType::U8)
        .expect("Write failed");
    let bytes = memory.read_bytes(&store, 0, 1).expect("Read failed");
    assert_eq!(bytes[0], 99u8, "Written U8 should be readable as bytes");
}

#[test]
fn test_write_typed_u16() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: u16 = 0xABCD;
    memory
        .write_typed(&mut store, 0, test_value, MemoryDataType::U16Le)
        .expect("Write failed");
    let bytes = memory.read_bytes(&store, 0, 2).expect("Read failed");
    assert_eq!(
        u16::from_le_bytes([bytes[0], bytes[1]]),
        test_value,
        "Written U16 should be readable as bytes"
    );
}

#[test]
fn test_write_typed_u32() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: u32 = 0xDEADBEEF;
    memory
        .write_typed(&mut store, 0, test_value, MemoryDataType::U32Le)
        .expect("Write failed");
    let bytes = memory.read_bytes(&store, 0, 4).expect("Read failed");
    let read_value = u32::from_le_bytes([bytes[0], bytes[1], bytes[2], bytes[3]]);
    assert_eq!(
        read_value, test_value,
        "Written U32 should be readable as bytes"
    );
}

#[test]
fn test_write_typed_u64() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: u64 = 0xCAFEBABEDEADBEEF;
    memory
        .write_typed(&mut store, 8, test_value, MemoryDataType::U64Le)
        .expect("Write failed");
    let bytes = memory.read_bytes(&store, 8, 8).expect("Read failed");
    let read_value = u64::from_le_bytes([
        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
    ]);
    assert_eq!(
        read_value, test_value,
        "Written U64 should be readable as bytes"
    );
}

#[test]
fn test_read_typed_i8() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: i8 = -42;
    memory
        .write_bytes(&mut store, 0, &[test_value as u8])
        .expect("Write failed");
    let value: i8 = memory
        .read_typed(&store, 0, MemoryDataType::I8)
        .expect("Read failed");
    assert_eq!(
        value, test_value,
        "I8 read should return the written negative value"
    );
}

#[test]
fn test_read_typed_i16() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: i16 = -12345;
    memory
        .write_bytes(&mut store, 0, &test_value.to_le_bytes())
        .expect("Write failed");
    let value: i16 = memory
        .read_typed(&store, 0, MemoryDataType::I16Le)
        .expect("Read failed");
    assert_eq!(
        value, test_value,
        "I16 read should return the written negative value"
    );
}

#[test]
fn test_read_typed_i32() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: i32 = -123456789;
    memory
        .write_bytes(&mut store, 0, &test_value.to_le_bytes())
        .expect("Write failed");
    let value: i32 = memory
        .read_typed(&store, 0, MemoryDataType::I32Le)
        .expect("Read failed");
    assert_eq!(
        value, test_value,
        "I32 read should return the written negative value"
    );
}

#[test]
fn test_read_typed_i64() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: i64 = -9223372036854775807;
    memory
        .write_bytes(&mut store, 8, &test_value.to_le_bytes())
        .expect("Write failed");
    let value: i64 = memory
        .read_typed(&store, 8, MemoryDataType::I64Le)
        .expect("Read failed");
    assert_eq!(
        value, test_value,
        "I64 read should return the written negative value"
    );
}

#[test]
fn test_read_typed_f32() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: f32 = 3.14159;
    memory
        .write_bytes(&mut store, 0, &test_value.to_bits().to_le_bytes())
        .expect("Write failed");
    let value: f32 = memory
        .read_typed(&store, 0, MemoryDataType::F32Le)
        .expect("Read failed");
    assert!(
        (value - test_value).abs() < 0.00001,
        "F32 read should return the written float value"
    );
}

#[test]
fn test_read_typed_f64() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let test_value: f64 = 2.718281828459045;
    memory
        .write_bytes(&mut store, 8, &test_value.to_bits().to_le_bytes())
        .expect("Write failed");
    let value: f64 = memory
        .read_typed(&store, 8, MemoryDataType::F64Le)
        .expect("Read failed");
    assert!(
        (value - test_value).abs() < 0.0000001,
        "F64 read should return the written float value"
    );
}

// === Factory methods tests ===

#[test]
fn test_from_wasmtime_memory() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let wat = "(module (memory (export \"mem\") 1 4))";
    let module =
        crate::module::Module::compile_wat(&engine, wat).expect("Failed to compile module");
    let instance = crate::instance::Instance::new_without_imports(&mut store, &module)
        .expect("Failed to create instance");

    let mem_export = instance
        .get_export_info("mem")
        .expect("Memory export not found");

    match &mem_export.export_type {
        crate::module::ExportKind::Memory(min, max, _, _) => {
            assert!(*min > 0, "Memory should have minimum pages");
            assert!(max.is_some(), "Memory should have maximum pages");
        }
        _ => panic!("Expected memory export"),
    }
}

#[test]
fn test_is_shared_memory_regular() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    assert!(
        !memory.is_shared_memory(),
        "Regular memory should not be shared"
    );
}

// === Memory metadata tests ===

#[test]
fn test_get_type() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 2).expect("Failed to create memory");
    let mem_type = memory.get_type(&store).expect("Failed to get memory type");

    assert_eq!(
        mem_type.minimum(),
        2,
        "Memory type should have initial pages as minimum"
    );
}

#[test]
fn test_data_size() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let size = memory.data_size(&store).expect("Failed to get data size");

    assert_eq!(size, 65536, "1 page should be 64KB");
}

#[test]
fn test_as_wasmtime_memory() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let wasmtime_mem = memory.as_wasmtime_memory();

    assert!(
        wasmtime_mem.is_some(),
        "Regular memory should return Some for as_wasmtime_memory"
    );
}

#[test]
fn test_memory_limits() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let config = MemoryConfig {
        initial_pages: 1,
        maximum_pages: Some(10),
        is_shared: false,
        memory_index: 0,
        name: Some("limited".to_string()),
    };
    let memory = Memory::new_with_config(&mut store, config).expect("Failed to create memory");

    let mem_config = memory.get_config();
    assert_eq!(mem_config.initial_pages, 1, "Initial pages should be 1");
    assert_eq!(
        mem_config.maximum_pages,
        Some(10),
        "Maximum pages should be 10"
    );
}

// === PlatformMemoryConfig builder tests ===

#[test]
fn test_config_builder_defaults() {
    let config = PlatformMemoryConfig::default();

    assert!(
        config.enable_huge_pages,
        "Huge pages should be enabled by default"
    );
    assert_eq!(
        config.numa_node, -1,
        "NUMA node should default to -1 (automatic)"
    );
    assert!(
        config.enable_compression,
        "Compression should be enabled by default"
    );
    assert!(
        config.enable_deduplication,
        "Deduplication should be enabled by default"
    );
    assert!(
        config.enable_leak_detection,
        "Leak detection should be enabled by default"
    );
    assert_eq!(
        config.alignment, 64,
        "Default alignment should be 64 (cache line)"
    );
    assert_eq!(
        config.page_size,
        PageSize::Default,
        "Page size should default to Default"
    );
}

#[test]
fn test_memory_builder_chain() {
    let builder = MemoryBuilder::new(2)
        .maximum_pages(100)
        .shared()
        .memory_index(1)
        .name("test-memory");

    let config: MemoryConfig = builder.into();
    assert_eq!(config.initial_pages, 2, "Initial pages should be 2");
    assert_eq!(
        config.maximum_pages,
        Some(100),
        "Maximum pages should be 100"
    );
    assert!(config.is_shared, "Memory should be shared");
    assert_eq!(config.memory_index, 1, "Memory index should be 1");
    assert_eq!(
        config.name,
        Some("test-memory".to_string()),
        "Name should be set"
    );
}

// === MemoryRegistry tests ===

#[test]
fn test_registry_list_memories() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");
    let registry = MemoryRegistry::new();

    let config1 = MemoryConfig {
        initial_pages: 1,
        maximum_pages: None,
        is_shared: false,
        memory_index: 0,
        name: Some("memory0".to_string()),
    };
    let config2 = MemoryConfig {
        initial_pages: 1,
        maximum_pages: None,
        is_shared: false,
        memory_index: 1,
        name: Some("memory1".to_string()),
    };

    let memory1 =
        Memory::new_with_config(&mut store, config1).expect("Failed to create memory 1");
    let memory2 =
        Memory::new_with_config(&mut store, config2).expect("Failed to create memory 2");

    let id1 = registry.register(memory1).expect("Failed to register memory 1");
    let id2 = registry.register(memory2).expect("Failed to register memory 2");

    assert_eq!(id1, 0, "Memory 1 should have index 0");
    assert_eq!(id2, 1, "Memory 2 should have index 1");

    let ids = registry.list_memories().expect("Failed to list memories");
    assert!(ids.contains(&id1), "Registry should contain memory 0");
    assert!(ids.contains(&id2), "Registry should contain memory 1");

    registry.unregister(id1).expect("Failed to unregister memory 0");
    registry.unregister(id2).expect("Failed to unregister memory 1");
}

#[test]
fn test_registry_multiple_registrations() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");
    let registry = MemoryRegistry::new();

    let configs = [
        MemoryConfig {
            initial_pages: 1,
            maximum_pages: None,
            is_shared: false,
            memory_index: 0,
            name: None,
        },
        MemoryConfig {
            initial_pages: 1,
            maximum_pages: None,
            is_shared: false,
            memory_index: 1,
            name: None,
        },
        MemoryConfig {
            initial_pages: 1,
            maximum_pages: None,
            is_shared: false,
            memory_index: 2,
            name: None,
        },
        MemoryConfig {
            initial_pages: 1,
            maximum_pages: None,
            is_shared: false,
            memory_index: 3,
            name: None,
        },
    ];

    let mut ids = Vec::new();
    for config in configs {
        let memory = Memory::new_with_config(&mut store, config).expect("Failed to create memory");
        let id = registry.register(memory).expect("Failed to register memory");
        ids.push(id);
    }

    let unique_ids: std::collections::HashSet<_> = ids.iter().collect();
    assert_eq!(
        unique_ids.len(),
        ids.len(),
        "All registered IDs should be unique"
    );
    assert_eq!(ids, vec![0, 1, 2, 3], "IDs should match memory indices");

    for id in ids {
        registry.unregister(id).expect("Failed to unregister memory");
    }
}

// === Memory growth and limit tests ===

#[test]
fn test_memory_growth_exceeds_limit() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let config = MemoryConfig {
        initial_pages: 1,
        maximum_pages: Some(2),
        is_shared: false,
        memory_index: 0,
        name: None,
    };
    let memory = Memory::new_with_config(&mut store, config).expect("Failed to create memory");

    let result = memory.grow(&mut store, 1);
    assert!(result.is_ok(), "Growing from 1 to 2 pages should succeed");

    let result = memory.grow(&mut store, 1);
    assert!(result.is_err(), "Growing beyond limit should fail");
}

#[test]
fn test_memory_config_validation_initial_exceeds_max() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let config = MemoryConfig {
        initial_pages: 10,
        maximum_pages: Some(5),
        is_shared: false,
        memory_index: 0,
        name: None,
    };
    let result = Memory::new_with_config(&mut store, config);

    assert!(result.is_err(), "Initial pages exceeding max should fail");
}

#[test]
fn test_memory_config_validation_zero_initial() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let config = MemoryConfig {
        initial_pages: 0,
        maximum_pages: None,
        is_shared: false,
        memory_index: 0,
        name: None,
    };
    let result = Memory::new_with_config(&mut store, config);

    assert!(result.is_err(), "Zero initial pages should fail");
}

// === Alignment error tests ===

#[test]
fn test_typed_read_alignment_error_u16() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let result: WasmtimeResult<u16> = memory.read_typed(&store, 1, MemoryDataType::U16Le);
    assert!(
        result.is_err(),
        "Reading u16 from offset 1 should fail due to alignment"
    );
}

#[test]
fn test_typed_read_alignment_error_u32() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let result: WasmtimeResult<u32> = memory.read_typed(&store, 3, MemoryDataType::U32Le);
    assert!(
        result.is_err(),
        "Reading u32 from offset 3 should fail due to alignment"
    );
}

#[test]
fn test_typed_read_alignment_error_u64() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let result: WasmtimeResult<u64> = memory.read_typed(&store, 4, MemoryDataType::U64Le);
    assert!(
        result.is_err(),
        "Reading u64 from offset 4 should fail due to alignment"
    );
}

// === Core module function tests ===

#[test]
fn test_core_get_memory_size() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 2).expect("Failed to create memory");
    let size = core::get_memory_size(&memory, &store).expect("Failed to get memory size");

    assert_eq!(size, 2 * 65536, "2 pages should be 128KB");
}

#[test]
fn test_core_grow_memory() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");
    let prev_pages = core::grow_memory(&memory, &mut store, 2).expect("Failed to grow memory");

    assert_eq!(prev_pages, 1, "Previous page count should be 1");

    let new_pages = core::get_memory_page_count(&memory, &store).expect("Failed to get page count");
    assert_eq!(new_pages, 3, "New page count should be 3");
}

#[test]
fn test_core_read_write_memory_bytes() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let data = vec![1, 2, 3, 4, 5];
    core::write_memory_bytes(&memory, &mut store, 100, &data).expect("Failed to write bytes");

    let read_data = core::read_memory_bytes(&memory, &store, 100, 5).expect("Failed to read bytes");
    assert_eq!(read_data, data, "Read data should match written data");
}

#[test]
fn test_core_read_write_memory_byte() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    core::write_memory_byte(&memory, &mut store, 256, 42).expect("Failed to write byte");
    let value = core::read_memory_byte(&memory, &store, 256).expect("Failed to read byte");

    assert_eq!(value, 42, "Read byte should match written byte");
}

#[test]
fn test_core_memory_copy() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let data = vec![10, 20, 30, 40, 50];
    memory.write_bytes(&mut store, 0, &data).expect("Failed to write bytes");

    core::memory_copy(&memory, &mut store, 100, 0, 5).expect("Failed to copy memory");

    let copied = memory.read_bytes(&store, 100, 5).expect("Failed to read bytes");
    assert_eq!(copied, data, "Copied data should match original");
}

#[test]
fn test_core_memory_fill() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    core::memory_fill(&memory, &mut store, 50, 0xAB, 10).expect("Failed to fill memory");

    let filled = memory.read_bytes(&store, 50, 10).expect("Failed to read bytes");
    assert!(
        filled.iter().all(|&b| b == 0xAB),
        "All bytes should be 0xAB"
    );
}

#[test]
fn test_core_get_memory_buffer() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let (ptr, len) = core::get_memory_buffer(&memory, &store).expect("Failed to get buffer");

    assert!(!ptr.is_null(), "Buffer pointer should not be null");
    assert_eq!(len, 65536, "Buffer length should be 64KB for 1 page");
}

#[test]
fn test_core_memory_is_shared() {
    let engine = shared_engine();
    let mut store = Store::new(&engine).expect("Failed to create store");

    let memory = Memory::new(&mut store, 1).expect("Failed to create memory");

    let is_shared = core::memory_is_shared(&memory, &store).expect("Failed to check shared");
    assert!(!is_shared, "Regular memory should not be shared");
}
