//! Test module to validate core runtime implementation completion
//!
//! This module provides comprehensive tests for the completed core runtime implementations

#[cfg(test)]
mod tests {
    use crate::engine::Engine;
    use crate::store::Store;
    use crate::module::Module;
    use crate::instance::{Instance, WasmValue};
    use crate::error::WasmtimeError;

    /// Test enhanced error mapping functionality
    #[test]
    fn test_enhanced_error_mapping() {
        // Test compilation error enhancement
        let enhanced = WasmtimeError::enhance_compilation_error_message("invalid magic number");
        assert!(enhanced.to_string().contains("Enhanced compilation error"));

        // Test runtime error enhancement
        let enhanced = WasmtimeError::enhance_runtime_error_message("stack overflow occurred");
        assert!(enhanced.to_string().contains("Enhanced runtime error"));
    }

    /// Test fuel tracking implementation
    #[test]
    fn test_fuel_tracking_completion() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::builder()
            .fuel_limit(1000)
            .build(&engine)
            .expect("Failed to create store with fuel");

        // Verify fuel tracking works
        let initial_fuel = store.fuel_remaining().expect("Failed to get fuel");
        assert!(initial_fuel.is_some());
        assert_eq!(initial_fuel.unwrap(), 1000);

        // Test fuel consumption
        let consumed = store.consume_fuel(100).expect("Failed to consume fuel");
        assert_eq!(consumed, 100);

        let remaining = store.fuel_remaining().expect("Failed to get remaining fuel");
        assert_eq!(remaining.unwrap(), 900);
    }

    /// Test function invocation with proper result initialization
    #[test]
    fn test_function_invocation_completion() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        // Simple WAT module with multiple return types
        let wat = r#"
            (module
                (func (export "test_i32") (result i32) i32.const 42)
                (func (export "test_i64") (result i64) i64.const 123456789)
                (func (export "test_f32") (result f32) f32.const 3.14)
                (func (export "test_multi") (result i32 i64)
                    i32.const 10
                    i64.const 20)
            )
        "#;

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");
        let mut instance = Instance::new_without_imports(&mut store, &module)
            .expect("Failed to create instance");

        // Test i32 function
        let result = instance.call_export_function(&mut store, "test_i32", &[])
            .expect("Failed to call test_i32");
        assert_eq!(result.values.len(), 1);
        match &result.values[0] {
            WasmValue::I32(val) => assert_eq!(*val, 42),
            _ => panic!("Expected I32 result"),
        }

        // Test multi-value function (tests proper result vector initialization)
        let result = instance.call_export_function(&mut store, "test_multi", &[])
            .expect("Failed to call test_multi");
        assert_eq!(result.values.len(), 2);
        match (&result.values[0], &result.values[1]) {
            (WasmValue::I32(val1), WasmValue::I64(val2)) => {
                assert_eq!(*val1, 10);
                assert_eq!(*val2, 20);
            },
            _ => panic!("Expected I32 and I64 results"),
        }

        // Verify execution time tracking
        assert!(result.execution_time_ns > 0);
    }

    /// Test WASI integration completion
    #[test]
    fn test_wasi_integration_completion() {
        use crate::wasi::WasiContext;
        use std::os::raw::c_void;

        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        // Create a WASI context
        let wasi_ctx = WasiContext::new().expect("Failed to create WASI context");

        // Test WASI context integration with store
        let ctx_ptr = Box::into_raw(Box::new(wasi_ctx)) as *mut c_void;
        let store_ptr = &mut store as *mut Store as *mut c_void;

        unsafe {
            // Test integration function
            let result = crate::wasi::wasi_ctx_add_to_store(ctx_ptr, store_ptr);
            assert_eq!(result, 0); // Success

            // Test retrieval function
            let retrieved = crate::wasi::wasi_ctx_get_from_store(store_ptr);
            assert!(!retrieved.is_null());

            // Test existence check
            let has_wasi = crate::wasi::wasi_ctx_store_has_wasi(store_ptr);
            assert_eq!(has_wasi, 1); // Has WASI context

            // Cleanup
            let _recovered_ctx = Box::from_raw(ctx_ptr as *mut WasiContext);
        }
    }

    /// Test memory operations are not stubbed
    #[test]
    fn test_memory_operations_completion() {
        let engine = Engine::new().expect("Failed to create engine");
        let mut store = Store::new(&engine).expect("Failed to create store");

        let memory = crate::memory::Memory::new(&mut store, 1)
            .expect("Failed to create memory");

        // Test memory operations work (not stubbed)
        let test_data = vec![1, 2, 3, 4, 5];
        memory.write_bytes(&mut store, 0, &test_data)
            .expect("Failed to write memory");

        let read_data = memory.read_bytes(&store, 0, 5)
            .expect("Failed to read memory");
        assert_eq!(read_data, test_data);

        // Test bounds checking
        let result = memory.read_bytes(&store, 65536, 1);
        assert!(result.is_err());

        // Test memory growth
        let previous_pages = memory.grow(&mut store, 1)
            .expect("Failed to grow memory");
        assert_eq!(previous_pages, 1);
        assert_eq!(memory.size_pages(&store).unwrap(), 2);

        // Test usage statistics
        let usage = memory.get_usage(&store).expect("Failed to get usage");
        assert_eq!(usage.current_pages, 2);
        assert!(usage.read_count > 0);
        assert!(usage.write_count > 0);
    }

    /// Test module analyzer improvements
    #[test]
    fn test_module_analyzer_completion() {
        let engine = Engine::new().expect("Failed to create engine");

        // Module with control flow for complexity testing
        let wat = r#"
            (module
                (func (export "complex") (param i32) (result i32)
                    local.get 0
                    i32.const 10
                    i32.lt_s
                    (if (result i32)
                        (then
                            local.get 0
                            i32.const 1
                            i32.add)
                        (else
                            local.get 0
                            i32.const 2
                            i32.mul)))
            )
        "#;

        let module = Module::compile_wat(&engine, wat).expect("Failed to compile module");

        // Test that module metadata extraction works
        let metadata = module.metadata();
        assert_eq!(metadata.exports.len(), 1);
        assert_eq!(metadata.exports[0].name, "complex");

        // Verify enhanced function analysis (not placeholder)
        assert_eq!(metadata.functions.len(), 1);
    }
}

/// Integration test for complete core runtime operations
#[test]
fn test_complete_runtime_integration() {
    let engine = crate::engine::Engine::new().expect("Failed to create engine");
    let mut store = crate::store::Store::builder()
        .fuel_limit(10000)
        .memory_limit(1024 * 1024)
        .build(&engine)
        .expect("Failed to create configured store");

    // Complex WAT module testing multiple features
    let wat = r#"
        (module
            (memory (export "memory") 1)
            (func (export "factorial") (param i32) (result i32)
                local.get 0
                i32.const 1
                i32.le_s
                (if (result i32)
                    (then i32.const 1)
                    (else
                        local.get 0
                        local.get 0
                        i32.const 1
                        i32.sub
                        call 0
                        i32.mul)))
            (func (export "write_read") (param i32 i32) (result i32)
                local.get 0
                local.get 1
                i32.store
                local.get 0
                i32.load))
    "#;

    let module = crate::module::Module::compile_wat(&engine, wat)
        .expect("Failed to compile complex module");

    let mut instance = crate::instance::Instance::new_without_imports(&mut store, &module)
        .expect("Failed to create instance");

    // Test factorial function (tests recursion and fuel consumption)
    let params = vec![crate::instance::WasmValue::I32(5)];
    let result = instance.call_export_function(&mut store, "factorial", &params)
        .expect("Failed to call factorial");

    match &result.values[0] {
        crate::instance::WasmValue::I32(val) => assert_eq!(*val, 120), // 5! = 120
        _ => panic!("Expected I32 result"),
    }

    // Verify fuel was consumed
    assert!(result.fuel_consumed.is_some());
    assert!(result.execution_time_ns > 0);

    // Test memory access function
    let memory = instance.get_memory(&mut store, "memory")
        .expect("Failed to get memory")
        .expect("Memory export not found");

    let params = vec![
        crate::instance::WasmValue::I32(100), // address
        crate::instance::WasmValue::I32(42),  // value
    ];
    let result = instance.call_export_function(&mut store, "write_read", &params)
        .expect("Failed to call write_read");

    match &result.values[0] {
        crate::instance::WasmValue::I32(val) => assert_eq!(*val, 42),
        _ => panic!("Expected I32 result"),
    }
}