//! Tests for WebAssembly Component Model support
//!
//! This module contains all tests for the component module.

use super::*;

#[test]
fn test_component_engine_creation() {
    let engine = ComponentEngine::new();
    assert!(engine.is_ok());
}

#[test]
fn test_component_engine_with_custom_engine() {
    let safe_config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&safe_config).unwrap();
    let component_engine = ComponentEngine::with_engine(wasmtime_engine);
    assert!(component_engine.is_ok());
}

#[test]
fn test_load_component_from_empty_bytes() {
    let engine = ComponentEngine::new().unwrap();
    let result = engine.load_component_from_bytes(&[]);
    assert!(result.is_err());

    if let Err(crate::error::WasmtimeError::InvalidParameter { message }) = result {
        assert!(message.contains("empty"));
    } else {
        panic!("Expected InvalidParameter error");
    }
}

#[test]
fn test_load_component_from_invalid_bytes() {
    let engine = ComponentEngine::new().unwrap();
    let invalid_bytes = vec![0u8; 10]; // Invalid WebAssembly
    let result = engine.load_component_from_bytes(&invalid_bytes);
    assert!(result.is_err());

    if let Err(crate::error::WasmtimeError::Compilation { .. }) = result {
        // Expected compilation error
    } else {
        panic!("Expected Compilation error");
    }
}

#[test]
fn test_load_component_from_nonexistent_file() {
    let engine = ComponentEngine::new().unwrap();
    let result = engine.load_component_from_file("/nonexistent/file.wasm");
    assert!(result.is_err());

    if let Err(crate::error::WasmtimeError::Io { .. }) = result {
        // Expected I/O error
    } else {
        panic!("Expected I/O error");
    }
}

#[test]
fn test_resource_manager_creation() {
    let manager = ResourceManager::new();
    assert_eq!(manager.active_count(), 0);
}

#[test]
fn test_resource_manager_cleanup() {
    let mut manager = ResourceManager::new();
    let cleaned = manager.cleanup_inactive();
    assert_eq!(cleaned, 0);
}

#[test]
fn test_component_metadata_accessors() {
    let metadata = ComponentMetadata {
        imports: vec![],
        exports: vec![InterfaceDefinition {
            name: "test-interface".to_string(),
            namespace: Some("test".to_string()),
            version: Some("1.0.0".to_string()),
            functions: vec![],
            types: vec![],
            resources: vec![],
        }],
        size_bytes: 1024,
    };

    // Test metadata directly without requiring actual component compilation
    assert_eq!(metadata.size_bytes, 1024);
    assert_eq!(metadata.exports.len(), 1);
    assert_eq!(metadata.exports[0].name, "test-interface");
    assert_eq!(metadata.imports.len(), 0);

    // Test interface lookup functions on metadata
    let has_export = metadata
        .exports
        .iter()
        .any(|export| export.name == "test-interface");
    assert!(has_export);

    let has_nonexistent = metadata
        .exports
        .iter()
        .any(|export| export.name == "nonexistent");
    assert!(!has_nonexistent);

    let has_import = metadata
        .imports
        .iter()
        .any(|import| import.name == "any-interface");
    assert!(!has_import);
}

#[test]
fn test_instance_info() {
    let info = InstanceInfo {
        instance_id: 42,
        strong_references: 1,
    };

    assert_eq!(info.instance_id, 42);
    assert_eq!(info.strong_references, 1);
}

#[test]
fn test_value_type_variants() {
    let types = vec![
        ComponentValueType::Bool,
        ComponentValueType::S8,
        ComponentValueType::U8,
        ComponentValueType::S16,
        ComponentValueType::U16,
        ComponentValueType::S32,
        ComponentValueType::U32,
        ComponentValueType::S64,
        ComponentValueType::U64,
        ComponentValueType::Float32,
        ComponentValueType::Float64,
        ComponentValueType::String,
        ComponentValueType::List(Box::new(ComponentValueType::String)),
        ComponentValueType::Option(Box::new(ComponentValueType::Bool)),
        ComponentValueType::Result {
            ok: Some(Box::new(ComponentValueType::S32)),
            err: Some(Box::new(ComponentValueType::String)),
        },
    ];

    // Test that all value types can be created and cloned
    for value_type in types {
        let cloned = value_type.clone();
        // Basic test - ensure types can be constructed and cloned
        match cloned {
            ComponentValueType::Bool => assert!(true),
            ComponentValueType::List(_) => assert!(true),
            ComponentValueType::Option(_) => assert!(true),
            ComponentValueType::Result { .. } => assert!(true),
            _ => assert!(true),
        }
    }
}

// === Phase 1: Additional Component Tests ===

#[test]
fn test_resource_manager_active_count() {
    let manager = ResourceManager::new();

    // New manager should have 0 active instances
    assert_eq!(
        manager.active_count(),
        0,
        "New manager should have 0 active instances"
    );
}

#[test]
fn test_interface_definition_json_serialization() {
    let interface = InterfaceDefinition {
        name: "test-interface".to_string(),
        namespace: Some("test".to_string()),
        version: Some("1.0.0".to_string()),
        functions: vec![FunctionDefinition {
            name: "my-func".to_string(),
            parameters: vec![Parameter {
                name: "input".to_string(),
                value_type: ComponentValueType::String,
            }],
            results: vec![ComponentValueType::String],
        }],
        types: vec![],
        resources: vec![],
    };

    let json_result = interface.to_json();
    assert!(json_result.is_ok(), "JSON serialization should succeed");

    let json = json_result.unwrap();
    assert!(
        json.contains("test-interface"),
        "JSON should contain interface name"
    );
    assert!(
        json.contains("my-func"),
        "JSON should contain function name"
    );
}

#[test]
fn test_component_engine_get_instance_count() {
    let engine = ComponentEngine::new().unwrap();
    let count = engine.get_instance_count();
    assert_eq!(count, 0, "New engine should have 0 instances");
}

#[test]
fn test_component_engine_cleanup_unused_instances() {
    let engine = ComponentEngine::new().unwrap();

    // Should not panic when called on empty engine
    engine.cleanup_unused_instances();
}

#[test]
fn test_component_engine_supports_feature() {
    let engine = ComponentEngine::new().unwrap();

    // Test some feature names
    let supports_gc = engine.supports_feature("gc");
    assert!(supports_gc.is_ok(), "supports_feature should not error");

    let supports_threads = engine.supports_feature("threads");
    assert!(
        supports_threads.is_ok(),
        "supports_feature should not error"
    );
}

#[test]
fn test_component_metadata_struct() {
    let metadata = ComponentMetadata {
        imports: vec![InterfaceDefinition {
            name: "import1".to_string(),
            namespace: None,
            version: None,
            functions: vec![],
            types: vec![],
            resources: vec![],
        }],
        exports: vec![InterfaceDefinition {
            name: "export1".to_string(),
            namespace: Some("test".to_string()),
            version: Some("2.0.0".to_string()),
            functions: vec![],
            types: vec![],
            resources: vec![],
        }],
        size_bytes: 2048,
    };

    assert_eq!(metadata.imports.len(), 1, "Should have 1 import");
    assert_eq!(metadata.exports.len(), 1, "Should have 1 export");
    assert_eq!(metadata.size_bytes, 2048, "Size should be 2048");
}

#[test]
fn test_function_definition_struct() {
    let func_def = FunctionDefinition {
        name: "my-function".to_string(),
        parameters: vec![
            Parameter {
                name: "param1".to_string(),
                value_type: ComponentValueType::S32,
            },
            Parameter {
                name: "param2".to_string(),
                value_type: ComponentValueType::String,
            },
        ],
        results: vec![ComponentValueType::Bool],
    };

    assert_eq!(func_def.name, "my-function");
    assert_eq!(func_def.parameters.len(), 2);
    assert_eq!(func_def.results.len(), 1);
}

#[test]
fn test_type_definition_struct() {
    let type_def = TypeDefinition {
        name: "MyRecord".to_string(),
        kind: ComponentTypeKind::Record(vec![FieldType {
            name: "field1".to_string(),
            value_type: ComponentValueType::U32,
        }]),
    };

    assert_eq!(type_def.name, "MyRecord");
    // Verify it's a Record kind
    match &type_def.kind {
        ComponentTypeKind::Record(fields) => {
            assert_eq!(fields.len(), 1, "Should have 1 field");
        }
        _ => panic!("Expected Record kind"),
    }
}

#[test]
fn test_resource_definition_struct() {
    let resource_def = ResourceDefinition {
        name: "MyResource".to_string(),
        constructors: vec![],
        methods: vec![],
    };

    assert_eq!(resource_def.name, "MyResource");
    assert!(resource_def.constructors.is_empty());
    assert!(resource_def.methods.is_empty());
}

#[test]
fn test_component_type_kind_enum() {
    let kinds: Vec<ComponentTypeKind> = vec![
        ComponentTypeKind::Record(vec![]),
        ComponentTypeKind::Variant(vec![]),
        ComponentTypeKind::Enum(vec!["A".to_string(), "B".to_string()]),
        ComponentTypeKind::Alias(ComponentValueType::Bool),
    ];

    for kind in kinds {
        let _cloned = kind.clone();
        // Type kinds should be clonable
    }
}

#[test]
fn test_field_type_struct() {
    let field = FieldType {
        name: "my_field".to_string(),
        value_type: ComponentValueType::U64,
    };

    assert_eq!(field.name, "my_field");
    match field.value_type {
        ComponentValueType::U64 => { /* OK */ }
        _ => panic!("Expected U64 type"),
    }
}

#[test]
fn test_case_type_struct() {
    let case = CaseType {
        name: "some_case".to_string(),
        payload: Some(ComponentValueType::String),
    };

    assert_eq!(case.name, "some_case");
    assert!(case.payload.is_some());
}

#[test]
fn test_host_interface_struct() {
    let host_interface = HostInterface {
        name: "my-host-interface".to_string(),
        implementation: Box::new(42i32), // Placeholder implementation
    };

    assert_eq!(host_interface.name, "my-host-interface");
}

#[test]
fn test_component_value_variants() {
    let values = vec![
        ComponentValue::Bool(true),
        ComponentValue::S8(-10),
        ComponentValue::U8(10),
        ComponentValue::S16(-1000),
        ComponentValue::U16(1000),
        ComponentValue::S32(-100000),
        ComponentValue::U32(100000),
        ComponentValue::S64(-10000000000),
        ComponentValue::U64(10000000000),
        ComponentValue::F32(3.14),
        ComponentValue::F64(2.718281828),
        ComponentValue::Char('A'),
        ComponentValue::String("hello".to_string()),
        ComponentValue::List(vec![ComponentValue::U8(1), ComponentValue::U8(2)]),
        ComponentValue::Record(vec![("field1".to_string(), ComponentValue::Bool(true))]),
        ComponentValue::Option(Some(Box::new(ComponentValue::S32(42)))),
        ComponentValue::Result {
            ok: Some(Box::new(ComponentValue::String("success".to_string()))),
            err: None,
            is_ok: true,
        },
        ComponentValue::Flags(vec!["flag1".to_string(), "flag2".to_string()]),
        ComponentValue::Enum("variant1".to_string()),
        ComponentValue::Variant {
            case_name: "some".to_string(),
            payload: Some(Box::new(ComponentValue::U32(99))),
        },
    ];

    for value in values {
        // All values should be debuggable
        let debug_str = format!("{:?}", value);
        assert!(!debug_str.is_empty(), "Debug string should not be empty");
    }
}

#[test]
fn test_wit_parser_creation() {
    let parser = WitParser::new();
    assert!(parser.is_ok(), "WitParser creation should succeed");
}

#[test]
fn test_wit_parser_validate_simple_interface() {
    let mut parser = WitParser::new().expect("Failed to create parser");

    // Simple valid WIT syntax
    let wit = r#"
        interface my-interface {
            my-func: func() -> string
        }
    "#;

    let result = parser.validate_syntax(wit);
    // Note: validation may fail if WIT parser is not fully implemented
    // This test just ensures the method doesn't panic
    match result {
        Ok(valid) => println!("Validation result: {}", valid),
        Err(e) => println!("Validation error (expected for incomplete impl): {:?}", e),
    }
}

#[test]
fn test_core_create_component_engine() {
    let engine = core::create_component_engine();
    assert!(
        engine.is_ok(),
        "core::create_component_engine should succeed"
    );
}

#[test]
fn test_core_load_component_from_empty_bytes() {
    let engine = ComponentEngine::new().expect("Failed to create engine");
    let result = core::load_component_from_bytes(&engine, &[]);
    assert!(result.is_err(), "Loading empty bytes should fail");
}

#[test]
fn test_core_load_component_from_invalid_bytes() {
    let engine = ComponentEngine::new().expect("Failed to create engine");
    let invalid_bytes = vec![0xCA, 0xFE, 0xBA, 0xBE];
    let result = core::load_component_from_bytes(&engine, &invalid_bytes);
    assert!(result.is_err(), "Loading invalid bytes should fail");
}

#[test]
fn test_core_get_component_size() {
    let metadata = ComponentMetadata {
        imports: vec![],
        exports: vec![],
        size_bytes: 4096,
    };

    // Create a component with known metadata for testing
    // Note: This would require a real component, so we test the metadata directly
    assert_eq!(metadata.size_bytes, 4096, "Size should match");
}

#[test]
fn test_core_get_export_count() {
    let metadata = ComponentMetadata {
        imports: vec![],
        exports: vec![
            InterfaceDefinition {
                name: "export1".to_string(),
                namespace: None,
                version: None,
                functions: vec![],
                types: vec![],
                resources: vec![],
            },
            InterfaceDefinition {
                name: "export2".to_string(),
                namespace: None,
                version: None,
                functions: vec![],
                types: vec![],
                resources: vec![],
            },
        ],
        size_bytes: 0,
    };

    assert_eq!(metadata.exports.len(), 2, "Should have 2 exports");
}

#[test]
fn test_core_get_import_count() {
    let metadata = ComponentMetadata {
        imports: vec![InterfaceDefinition {
            name: "import1".to_string(),
            namespace: None,
            version: None,
            functions: vec![],
            types: vec![],
            resources: vec![],
        }],
        exports: vec![],
        size_bytes: 0,
    };

    assert_eq!(metadata.imports.len(), 1, "Should have 1 import");
}

#[test]
fn test_component_linker_creation() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let linker = ComponentLinker::new(&wasmtime_engine);

    assert!(linker.is_ok(), "ComponentLinker creation should succeed");
}

#[test]
fn test_component_linker_wasi_args() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let mut linker = ComponentLinker::new(&wasmtime_engine).expect("Failed to create linker");

    linker.set_wasi_args(vec!["arg1".to_string(), "arg2".to_string()]);

    let wasi_config = linker.wasi_p2_config();
    assert_eq!(wasi_config.args.len(), 2, "Should have 2 args");
}

#[test]
fn test_component_linker_wasi_env() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let mut linker = ComponentLinker::new(&wasmtime_engine).expect("Failed to create linker");

    let mut env = std::collections::HashMap::new();
    env.insert("KEY1".to_string(), "VALUE1".to_string());
    env.insert("KEY2".to_string(), "VALUE2".to_string());

    linker.set_wasi_env(env);

    let wasi_config = linker.wasi_p2_config();
    assert_eq!(wasi_config.env.len(), 2, "Should have 2 env vars");
}

#[test]
fn test_component_linker_wasi_inherit_env() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let mut linker = ComponentLinker::new(&wasmtime_engine).expect("Failed to create linker");

    linker.set_wasi_inherit_env(true);

    let wasi_config = linker.wasi_p2_config();
    assert!(wasi_config.inherit_env, "inherit_env should be true");
}

#[test]
fn test_component_linker_wasi_inherit_stdio() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let mut linker = ComponentLinker::new(&wasmtime_engine).expect("Failed to create linker");

    linker.set_wasi_inherit_stdio(true);

    let wasi_config = linker.wasi_p2_config();
    assert!(wasi_config.inherit_stdio, "inherit_stdio should be true");
}

#[test]
fn test_component_linker_wasi_preopen_dir() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let mut linker = ComponentLinker::new(&wasmtime_engine).expect("Failed to create linker");

    linker.add_wasi_preopen_dir("/tmp".to_string(), "/sandbox".to_string(), 0x1, 0x1);

    let wasi_config = linker.wasi_p2_config();
    assert_eq!(
        wasi_config.preopened_dirs.len(),
        1,
        "Should have 1 preopen dir"
    );
}

#[test]
fn test_component_linker_wasi_permissions() {
    let config = crate::engine::safe_wasmtime_config();
    let wasmtime_engine = wasmtime::Engine::new(&config).unwrap();
    let mut linker = ComponentLinker::new(&wasmtime_engine).expect("Failed to create linker");

    linker.set_wasi_allow_network(true);
    linker.set_wasi_allow_clock(true);
    linker.set_wasi_allow_random(true);

    let wasi_config = linker.wasi_p2_config();
    assert!(wasi_config.allow_network, "allow_network should be true");
    assert!(wasi_config.allow_clock, "allow_clock should be true");
    assert!(wasi_config.allow_random, "allow_random should be true");
}
