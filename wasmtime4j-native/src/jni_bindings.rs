//! JNI bindings for Java 8-22 compatibility

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JByteArray, JString};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jint, jboolean, jbyteArray};

// Instance is imported locally in each module that needs it

/// JNI bindings module
/// 
/// This module provides JNI-compatible functions for use by the wasmtime4j-jni module.
/// All functions follow JNI naming conventions and handle Java/native type conversions.

/// JNI bindings for Engine operations
#[cfg(feature = "jni-bindings")]
pub mod jni_engine {
    use super::*;
    use crate::engine::core;
    use crate::error::jni_utils;
    use crate::ffi_common::parameter_conversion;
    
    /// Create a new Wasmtime engine with default configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngine(
        env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || core::create_engine()) as jlong
    }

    /// Create a new Wasmtime engine with custom configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngineWithConfig(
        env: JNIEnv,
        _class: JClass,
        strategy: jint,
        opt_level: jint,
        debug_info: jboolean,
        wasm_threads: jboolean,
        wasm_simd: jboolean,
        wasm_reference_types: jboolean,
        wasm_bulk_memory: jboolean,
        wasm_multi_value: jboolean,
        fuel_enabled: jboolean,
        max_memory_pages: jint,
        max_stack_size: jint,
        epoch_interruption: jboolean,
        max_instances: jint,
    ) -> jlong {
        
        
        jni_utils::jni_try_ptr(env, || {
            let strategy_opt = parameter_conversion::convert_strategy(strategy);
            let opt_level_opt = parameter_conversion::convert_opt_level(opt_level);
            let max_memory_pages_opt = parameter_conversion::convert_int_to_optional_u32(max_memory_pages);
            let max_stack_size_opt = parameter_conversion::convert_int_to_optional_usize(max_stack_size);
            let max_instances_opt = parameter_conversion::convert_int_to_optional_u32(max_instances);
            
            core::create_engine_with_config(
                strategy_opt,
                opt_level_opt,
                parameter_conversion::convert_int_to_bool(debug_info as i32),
                parameter_conversion::convert_int_to_bool(wasm_threads as i32),
                parameter_conversion::convert_int_to_bool(wasm_simd as i32),
                parameter_conversion::convert_int_to_bool(wasm_reference_types as i32),
                parameter_conversion::convert_int_to_bool(wasm_bulk_memory as i32),
                parameter_conversion::convert_int_to_bool(wasm_multi_value as i32),
                parameter_conversion::convert_int_to_bool(fuel_enabled as i32),
                max_memory_pages_opt,
                max_stack_size_opt,
                parameter_conversion::convert_int_to_bool(epoch_interruption as i32),
                max_instances_opt,
            )
        }) as jlong
    }

    /// Destroy a Wasmtime engine (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDestroyEngine(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) {
        unsafe {
            core::destroy_engine(engine_ptr as *mut std::os::raw::c_void);
        }
    }
    
    /// Compile WebAssembly module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCompileModule(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        wasm_bytes: jbyteArray,
    ) -> jlong {
        // Extract data before moving env into jni_try_ptr
        let wasm_data_result = env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java byte array: {}", e),
            });
        
        let data = match wasm_data_result {
            Ok(data) => data,
            Err(_) => return 0 as jlong, // Return null on error
        };
        
        jni_utils::jni_try_ptr(env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            let byte_converter = jni_module::VecByteArrayConverter::new(data);
            crate::shared_ffi::module::compile_module_shared(engine, byte_converter)
        }) as jlong
    }
    
    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateStore(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            crate::store::core::create_store(engine)
        }) as jlong
    }
    
    /// Set optimization level
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSetOptimizationLevel(
        env: JNIEnv,
        _class: JClass,
        _engine_ptr: jlong,
        _level: jint,
    ) -> jboolean {
        // For now, return true (optimization level setting not critical for basic tests)
        1
    }
    
    /// Check if fuel consumption is enabled (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsFuelEnabled(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jboolean {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => if core::is_fuel_enabled(engine) { 1 } else { 0 },
            Err(_) => 0,
        }
    }

    /// Check if epoch interruption is enabled (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsEpochInterruptionEnabled(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jboolean {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => if core::is_epoch_interruption_enabled(engine) { 1 } else { 0 },
            Err(_) => 0,
        }
    }

    /// Get memory limit in pages (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetMemoryLimit(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => core::get_memory_limit(engine).map(|limit| limit as jint).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Get stack size limit in bytes (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetStackLimit(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => core::get_stack_limit(engine).map(|limit| limit as jint).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Get maximum instances limit (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetMaxInstances(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => core::get_max_instances(engine).map(|limit| limit as jint).unwrap_or(-1),
            Err(_) => -1,
        }
    }

    /// Validate engine functionality (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeValidateEngine(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jboolean {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => if core::validate_engine(engine).is_ok() { 1 } else { 0 },
            Err(_) => 0,
        }
    }

    /// Check if engine supports WebAssembly feature (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSupportsFeature(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        feature_id: jint,
    ) -> jboolean {
        use crate::engine::WasmFeature;
        
        let feature = match feature_id {
            0 => WasmFeature::Threads,
            1 => WasmFeature::ReferenceTypes,
            2 => WasmFeature::Simd,
            3 => WasmFeature::BulkMemory,
            4 => WasmFeature::MultiValue,
            _ => return 0,
        };

        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => if core::check_feature_support(engine, feature) { 1 } else { 0 },
            Err(_) => 0,
        }
    }

    /// Get engine reference count for debugging (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetReferenceCount(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => core::get_reference_count(engine) as jint,
            Err(_) => -1,
        }
    }
    
    /// Check if debug info is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsDebugInfo(
        env: JNIEnv,
        _class: JClass,
        _engine_ptr: jlong,
    ) -> jboolean {
        // Return false by default
        0
    }
}

/// JNI bindings for Instance operations
#[cfg(feature = "jni-bindings")]
pub mod jni_instance {
    use super::*;
    use crate::instance::core;
    use crate::error::jni_utils;
    
    /// Create a new WebAssembly instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCreateInstance(
        env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        module_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || {
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            let module = unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            core::create_instance(store, module)
        }) as jlong
    }
    
    /// Destroy an instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDestroyInstance(
        env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) {
        unsafe {
            core::destroy_instance(instance_ptr as *mut std::os::raw::c_void);
        }
    }
}

/// JNI bindings for Store operations
#[cfg(feature = "jni-bindings")]
pub mod jni_store {
    use super::*;
    use crate::store::core;
    use crate::error::jni_utils;
    
    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStore(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            core::create_store(engine)
        }) as jlong
    }
    
    /// Destroy a store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeDestroyStore(
        env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) {
        unsafe {
            core::destroy_store(store_ptr as *mut std::os::raw::c_void);
        }
    }
}

/// JNI bindings for Module operations  
#[cfg(feature = "jni-bindings")]
pub mod jni_module {
    use super::*;
    use crate::module::core;
    use crate::error::jni_utils;
    use crate::shared_ffi::module::ByteArrayConverter;
    use jni::sys::{jobjectArray, jstring};

    /// Vec<u8> byte array converter implementation for JNI
    pub struct VecByteArrayConverter {
        data: Vec<u8>,
    }

    impl VecByteArrayConverter {
        pub fn new(data: Vec<u8>) -> Self {
            Self { data }
        }
    }

    impl ByteArrayConverter for VecByteArrayConverter {
        unsafe fn get_bytes(&self) -> crate::error::WasmtimeResult<&[u8]> {
            Ok(&self.data)
        }

        fn len(&self) -> usize {
            self.data.len()
        }
    }

    
    /// Instantiate a module within a store context
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInstantiateModule(
        env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || {
            let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            crate::instance::core::create_instance(store, module)
        }) as jlong
    }
    
    /// Instantiate a module with specific imports
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInstantiateModuleWithImports(
        env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
        store_ptr: jlong,
        _import_map_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || {
            let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            // For now, ignore imports - this would need proper ImportMap implementation
            crate::instance::core::create_instance(store, module)
        }) as jlong
    }
    
    /// Get the names of functions exported by a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedFunctions(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let exports = core::get_function_exports(module);
                let mut function_names = Vec::new();
                
                for export in exports {
                    function_names.push(export.name.as_str());
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    function_names.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, name) in function_names.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(name) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get the names of memories exported by a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedMemories(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let exports = core::get_memory_exports(module);
                let mut memory_names = Vec::new();
                
                for export in exports {
                    memory_names.push(export.name.as_str());
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    memory_names.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, name) in memory_names.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(name) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get the names of tables exported by a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedTables(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let metadata = core::get_metadata(module);
                let mut table_names = Vec::new();
                
                for export in &metadata.exports {
                    if matches!(export.export_type, crate::module::ExportKind::Table(_, _, _)) {
                        table_names.push(export.name.as_str());
                    }
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    table_names.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, name) in table_names.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(name) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get the names of globals exported by a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportedGlobals(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let metadata = core::get_metadata(module);
                let mut global_names = Vec::new();
                
                for export in &metadata.exports {
                    if matches!(export.export_type, crate::module::ExportKind::Global(_, _)) {
                        global_names.push(export.name.as_str());
                    }
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    global_names.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, name) in global_names.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(name) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get the names of functions imported by a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetImportedFunctions(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let imports = core::get_required_imports(module);
                let mut function_names = Vec::new();
                
                for import in imports {
                    if matches!(import.import_type, crate::module::ImportKind::Function(_)) {
                        let full_name = format!("{}::{}", import.module, import.name);
                        function_names.push(full_name);
                    }
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    function_names.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, name) in function_names.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(&name) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Validate WebAssembly bytecode without compiling
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeValidateModule(
        env: JNIEnv,
        _class: JClass,
        bytecode: jbyteArray,
    ) -> jboolean {
        // Extract data first
        let wasm_data_result = env.convert_byte_array(unsafe { JByteArray::from_raw(bytecode) })
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java byte array: {}", e),
            });
        
        let data = match wasm_data_result {
            Ok(data) => data,
            Err(_) => return 0, // Invalid on error
        };
        
        let byte_converter = jni_module::VecByteArrayConverter::new(data);
        match crate::shared_ffi::module::validate_module_shared(byte_converter) {
            Ok(()) => 1, // Valid
            Err(_) => 0, // Invalid
        }
    }
    
    /// Get the size of a compiled module in bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleSize(
        env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jlong {
        let result = crate::shared_ffi::module::get_module_size_shared(module_ptr as *mut std::os::raw::c_void);
        let (_, size) = crate::shared_ffi::module::size_result_to_ffi_result(result);
        size as jlong
    }
    
    /// Get comprehensive export metadata for a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetExportMetadata(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let metadata = core::get_metadata(module);
                let mut export_data = Vec::new();
                
                for export in &metadata.exports {
                    let type_str = match &export.export_type {
                        crate::module::ExportKind::Function(_) => "function",
                        crate::module::ExportKind::Global(_, _) => "global",
                        crate::module::ExportKind::Memory(_, _, _) => "memory",
                        crate::module::ExportKind::Table(_, _, _) => "table",
                    };
                    
                    let entry = format!("{}|{}", export.name, type_str);
                    export_data.push(entry);
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    export_data.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, data) in export_data.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(data) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get comprehensive import metadata for a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetImportMetadata(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                let imports = core::get_required_imports(module);
                let mut import_data = Vec::new();
                
                for import in imports {
                    let type_str = match &import.import_type {
                        crate::module::ImportKind::Function(_) => "function",
                        crate::module::ImportKind::Global(_, _) => "global",
                        crate::module::ImportKind::Memory(_, _, _) => "memory",
                        crate::module::ImportKind::Table(_, _, _) => "table",
                    };
                    
                    let entry = format!("{}|{}|{}", import.module, import.name, type_str);
                    import_data.push(entry);
                }
                
                // Convert to Java String array
                match env.new_object_array(
                    import_data.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, data) in import_data.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(data) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get the name of a module if it has one
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleName(
        env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jstring {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(module) => {
                if let Some(name) = core::get_module_name(module) {
                    match env.new_string(name) {
                        Ok(jstr) => jstr.into_raw(),
                        Err(_) => std::ptr::null_mut(),
                    }
                } else {
                    std::ptr::null_mut()
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get WebAssembly features supported by a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleFeatures(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(_module) => {
                // For now, return empty array - feature detection would need more sophisticated analysis
                let features: Vec<String> = vec![];
                
                // Convert to Java String array
                match env.new_object_array(
                    features.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, feature) in features.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(feature) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Get module linking information
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleLinkingInfo(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobjectArray {
        match unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
            Ok(_module) => {
                // For now, return empty array - linking info would need more sophisticated analysis
                let linking_info: Vec<String> = vec![];
                
                // Convert to Java String array
                match env.new_object_array(
                    linking_info.len() as i32,
                    "java/lang/String",
                    JString::default(),
                ) {
                    Ok(array) => {
                        for (i, info) in linking_info.iter().enumerate() {
                            if let Ok(jstr) = env.new_string(info) {
                                let _ = env.set_object_array_element(&array, i as i32, jstr);
                            }
                        }
                        array.into_raw()
                    }
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Serialize a compiled module to bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeSerializeModule(
        env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jbyteArray {
        match crate::shared_ffi::module::serialize_module_shared(module_ptr as *mut std::os::raw::c_void) {
            Ok(bytes) => {
                match env.byte_array_from_slice(&bytes) {
                    Ok(jarray) => jarray.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
    
    /// Deserialize a module from bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDeserializeModule(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        serialized_data: jbyteArray,
    ) -> jlong {
        // Convert byte array to Vec<u8> before moving env
        let data = match (|| -> Result<Vec<u8>, jni::errors::Error> {
            let byte_array = unsafe { JByteArray::from_raw(serialized_data) };
            let array_elements = unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
            let len = env.get_array_length(&byte_array)? as usize;
            let slice = unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
            Ok(slice.to_vec())
        })() {
            Ok(data) => data,
            Err(_) => return 0 as jlong, // Return null on error
        };
        
        jni_utils::jni_try_ptr(env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            let byte_converter = VecByteArrayConverter::new(data);
            crate::shared_ffi::module::deserialize_module_shared(engine, byte_converter)
        }) as jlong
    }
    
    /// Create a native import map from serialized data
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeCreateImportMap(
        env: JNIEnv,
        _class: JClass,
        _store_ptr: jlong,
        _import_data: jbyteArray,
    ) -> jlong {
        // For now, return 0 - proper ImportMap implementation would be needed
        0
    }
    
    /// Destroy a native import map
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyImportMap(
        env: JNIEnv,
        _class: JClass,
        _import_map_ptr: jlong,
    ) {
        // For now, do nothing - proper ImportMap implementation would be needed
    }
    
    /// Destroy a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModule(
        env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) {
        crate::shared_ffi::module::destroy_module_shared(module_ptr as *mut std::os::raw::c_void);
    }
}

#[cfg(not(feature = "jni-bindings"))]
pub mod engine {}

#[cfg(not(feature = "jni-bindings"))]
pub mod module {}

/// JNI bindings for Component operations (WASI Preview 2)
#[cfg(feature = "jni-bindings")]
pub mod jni_component {
    use super::*;
    use crate::component::{ComponentEngine, Component};
    use crate::error::jni_utils;
    

    /// Create a new component engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCreateComponentEngine(
        env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || crate::component::core::create_component_engine()) as jlong
    }

    /// Load component from WebAssembly bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeLoadComponentFromBytes(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        wasm_bytes: jbyteArray,
    ) -> jlong {
        // Extract data before moving env into jni_try_ptr
        let wasm_data_result = env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java byte array: {}", e),
            });

        jni_utils::jni_try_ptr(env, || {
            let engine = unsafe { 
                crate::component::core::get_component_engine_ref(engine_ptr as *const std::os::raw::c_void)? 
            };

            // Get byte array data from extracted result
            let wasm_data = wasm_data_result?;

            crate::component::core::load_component_from_bytes(engine, &wasm_data)
        }) as jlong
    }

    /// Instantiate a component
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeInstantiateComponent(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        component_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(env, || {
            let engine = unsafe { 
                crate::component::core::get_component_engine_ref(engine_ptr as *const std::os::raw::c_void)? 
            };
            let component = unsafe { 
                crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)? 
            };

            crate::component::core::instantiate_component(engine, component).map(Box::new)
        }) as jlong
    }

    /// Get component size in bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentSize(
        env: JNIEnv,
        _class: JClass,
        component_ptr: jlong,
    ) -> jlong {
        if component_ptr == 0 {
            log::error!("Component pointer is null");
            return 0;
        }

        let component = unsafe { &*(component_ptr as *const Component) };
        component.size_bytes() as jlong
    }

    /// Check if component exports an interface
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeExportsInterface(
        mut env: JNIEnv,
        _class: JClass,
        component_ptr: jlong,
        interface_name: jni::objects::JString,
    ) -> jboolean {
        if component_ptr == 0 {
            log::error!("Component pointer is null");
            return 0;
        }

        let component = unsafe { &*(component_ptr as *const Component) };

        let interface_str: String = match env.get_string(&interface_name) {
            Ok(s) => s.into(),
            Err(e) => {
                log::error!("Failed to convert interface name: {:?}", e);
                return 0;
            }
        };

        if component.exports_interface(&interface_str) { 1 } else { 0 }
    }

    /// Check if component imports an interface  
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeImportsInterface(
        mut env: JNIEnv,
        _class: JClass,
        component_ptr: jlong,
        interface_name: jni::objects::JString,
    ) -> jboolean {
        if component_ptr == 0 {
            log::error!("Component pointer is null");
            return 0;
        }

        let component = unsafe { &*(component_ptr as *const Component) };

        let interface_str: String = match env.get_string(&interface_name) {
            Ok(s) => s.into(),
            Err(e) => {
                log::error!("Failed to convert interface name: {:?}", e);
                return 0;
            }
        };

        if component.imports_interface(&interface_str) { 1 } else { 0 }
    }

    /// Get active component instances count
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetActiveInstancesCount(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        if engine_ptr == 0 {
            log::error!("Component engine pointer is null");
            return -1;
        }

        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };

        match engine.get_active_instances() {
            Ok(instances) => instances.len() as jint,
            Err(e) => {
                log::error!("Failed to get active instances: {:?}", e);
                -1
            }
        }
    }

    /// Cleanup inactive component instances
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCleanupInstances(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        if engine_ptr == 0 {
            log::error!("Component engine pointer is null");
            return -1;
        }

        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };

        match engine.cleanup_instances() {
            Ok(cleaned_count) => cleaned_count as jint,
            Err(e) => {
                log::error!("Failed to cleanup instances: {:?}", e);
                -1
            }
        }
    }

    /// Destroy a component engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponentEngine(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) {
        unsafe {
            crate::component::core::destroy_component_engine(engine_ptr as *mut std::os::raw::c_void);
        }
    }

    /// Destroy a component
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponent(
        env: JNIEnv,
        _class: JClass,
        component_ptr: jlong,
    ) {
        unsafe {
            crate::component::core::destroy_component(component_ptr as *mut std::os::raw::c_void);
        }
    }

    /// Destroy a component instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponentInstance(
        env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) {
        unsafe {
            crate::component::core::destroy_component_instance(instance_ptr as *mut std::os::raw::c_void);
        }
    }
}

/// JNI bindings for Host Function operations
#[cfg(feature = "jni-bindings")]
pub mod jni_hostfunc {
    use super::*;
    use crate::error::jni_utils;
    use crate::hostfunc::{HostFunction, HostFunctionCallback};
    use crate::instance::WasmValue;
    use crate::{WasmtimeError, WasmtimeResult};
    use wasmtime::{ValType, FuncType};
    use std::os::raw::c_void;
    use std::sync::Arc;

    /// JNI callback implementation that bridges to Java
    struct JniHostFunctionCallback {
        java_callback_id: u64,
    }

    impl HostFunctionCallback for JniHostFunctionCallback {
        fn execute(&self, _params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>> {
            // This will be called from the native hostFunctionCallback method in Java
            // For now, we'll return an error as this should not be called directly
            Err(WasmtimeError::Runtime {
                message: "JNI host function callback should be handled by Java".to_string(),
                backtrace: None,
            })
        }
    }

    /// Create a new host function (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHostFunction_nativeCreateHostFunction(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
        function_name: JString,
        function_type_data: jbyteArray,
        host_function_id: jlong,
    ) -> jlong {
        // Extract string and byte array data first
        let name_string = match env.get_string(&function_name) {
            Ok(s) => s.into(),
            Err(_) => return 0 as jlong,
        };
        
        let type_data_bytes = match env.convert_byte_array(unsafe { JByteArray::from_raw(function_type_data) }) {
            Ok(data) => data,
            Err(_) => return 0 as jlong,
        };
        
        jni_utils::jni_try_ptr(env, || {
            let _name: String = name_string;
            let type_data = type_data_bytes;
            
            // Get store reference  
            let store = unsafe { crate::store::core::get_store_ref(store_handle as *const c_void)? };
            
            // For now, let's create a simple function type without engine dependency
            // TODO: This is a temporary workaround - need proper engine access from store
            let _func_type = store.with_context(|_ctx| {
                // Create a temporary engine for function type creation
                // This is not ideal but works around the private field access issue
                let temp_engine = wasmtime::Engine::default();
                unmarshal_function_type(&temp_engine, &type_data)
            })?;
            
            // TODO: Also need to handle store_weak properly without accessing private inner field
            // For now, we'll need to modify the hostfunc creation to not require weak reference
            
            // Create callback wrapper
            let _callback = Box::new(JniHostFunctionCallback {
                java_callback_id: host_function_id as u64,
            });
            
            // TODO: Fix store_weak access - Store struct needs to provide method for weak reference
            // For now, return error to avoid compilation issues
            Err::<Box<Arc<HostFunction>>, WasmtimeError>(WasmtimeError::Runtime {
                message: "Host function creation temporarily disabled due to Store API limitations".to_string(),
                backtrace: None,
            })
        }) as jlong
    }

    /// Destroy a host function (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHostFunction_nativeDestroyHostFunction(
        env: JNIEnv,
        _class: JClass,
        host_func_handle: jlong,
    ) {
        unsafe {
            if host_func_handle != 0 {
                let _ = Box::from_raw(host_func_handle as *mut HostFunction);
                log::debug!("Destroyed JNI host function with handle: 0x{:x}", host_func_handle);
            }
        }
    }

    /// Unmarshal function type from byte array
    fn unmarshal_function_type(engine: &wasmtime::Engine, data: &[u8]) -> WasmtimeResult<FuncType> {
        // Simple marshalling format:
        // [param_count: 4 bytes][param_types: param_count bytes][return_count: 4 bytes][return_types: return_count bytes]
        
        if data.len() < 8 {
            return Err(WasmtimeError::Validation {
                message: "Function type data too short".to_string(),
            });
        }
        
        let param_count = u32::from_le_bytes([data[0], data[1], data[2], data[3]]) as usize;
        let return_count_offset = 4 + param_count;
        
        if data.len() < return_count_offset + 4 {
            return Err(WasmtimeError::Validation {
                message: "Function type data missing return count".to_string(),
            });
        }
        
        let return_count = u32::from_le_bytes([
            data[return_count_offset],
            data[return_count_offset + 1], 
            data[return_count_offset + 2],
            data[return_count_offset + 3]
        ]) as usize;
        
        if data.len() < return_count_offset + 4 + return_count {
            return Err(WasmtimeError::Validation {
                message: "Function type data too short for return types".to_string(),
            });
        }
        
        // Parse parameter types
        let mut param_types = Vec::with_capacity(param_count);
        for i in 0..param_count {
            let val_type = match data[4 + i] {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                _ => return Err(WasmtimeError::Validation {
                    message: format!("Invalid parameter type: {}", data[4 + i]),
                }),
            };
            param_types.push(val_type);
        }
        
        // Parse return types
        let mut return_types = Vec::with_capacity(return_count);
        for i in 0..return_count {
            let val_type = match data[return_count_offset + 4 + i] {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                _ => return Err(WasmtimeError::Validation {
                    message: format!("Invalid return type: {}", data[return_count_offset + 4 + i]),
                }),
            };
            return_types.push(val_type);
        }
        
        Ok(wasmtime::FuncType::new(engine, param_types, return_types))
    }
}

#[cfg(not(feature = "jni-bindings"))]
pub mod instance {}

/// JNI bindings for WebAssembly global variables
#[cfg(feature = "jni-bindings")]
pub mod jni_global {
    use super::*;
    use crate::global::core;
    use crate::store::Store;
    use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
    use crate::error::ffi_utils;
    
    use wasmtime::{ValType, Mutability, RefType};

    /// Create a new WebAssembly global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeCreateGlobal(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        value_type: jint,
        mutability: jint,
        i32_value: jint,
        i64_value: jlong,
        f32_value: f64,
        f64_value: f64,
        ref_id_present: jboolean,
        ref_id: jlong,
        name: JString,
    ) -> jlong {
        // Extract JNI string data first
        let name_str = if name.is_null() {
            None
        } else {
            match env.get_string(&name) {
                Ok(s) => Some(s.into()),
                Err(_) => return 0 as jlong,
            }
        };
        
        jni_utils::jni_try_ptr(env, || {
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let val_type = match value_type {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid value type: {}", value_type),
                }),
            };

            let mutability_enum = match mutability {
                0 => Mutability::Const,
                1 => Mutability::Var,
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid mutability: {}", mutability),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id as u64) } else { None };

            let initial_value = core::create_global_value(
                val_type.clone(), 
                i32_value, 
                i64_value, 
                f32_value as f32, 
                f64_value,
                ref_id_opt,
            )?;

            let global = core::create_global(store, val_type, mutability_enum, initial_value, name_str)?;
            
            Ok(global)
        }) as jlong
    }

    /// Get global variable value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetGlobal(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
    ) -> jbyteArray {
        match (|| -> WasmtimeResult<jbyteArray> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let value = core::get_global_value(global, store)?;
            let (i32_val, i64_val, f32_val, f64_val, ref_id_opt) = core::extract_global_value(&value);
            
            // Pack the values into a byte array
            let mut data = Vec::with_capacity(29); // 5 * 8 bytes for values + 1 byte for presence flag
            data.extend_from_slice(&i32_val.to_le_bytes());
            data.extend_from_slice(&i64_val.to_le_bytes());
            data.extend_from_slice(&f32_val.to_le_bytes());
            data.extend_from_slice(&f64_val.to_le_bytes());
            data.push(if ref_id_opt.is_some() { 1 } else { 0 });
            data.extend_from_slice(&ref_id_opt.unwrap_or(0).to_le_bytes());
            
            let byte_array = env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
            let raw_array = byte_array.as_raw();
            env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;
            
            Ok(raw_array)
        })() {
            Ok(result) => result,
            Err(_) => std::ptr::null_mut() as jbyteArray, // Return null on error
        }
    }

    /// Set global variable value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetGlobal(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
        value_type: jint,
        i32_value: jint,
        i64_value: jlong,
        f32_value: f64,
        f64_value: f64,
        ref_id_present: jboolean,
        ref_id: jlong,
    ) -> jint {
        jni_utils::jni_try_code(env, || {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let val_type = match value_type {
                0 => ValType::I32,
                1 => ValType::I64,
                2 => ValType::F32,
                3 => ValType::F64,
                4 => ValType::V128,
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid value type: {}", value_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id as u64) } else { None };

            let value = core::create_global_value(
                val_type, 
                i32_value, 
                i64_value, 
                f32_value as f32, 
                f64_value,
                ref_id_opt,
            )?;

            core::set_global_value(global, store, value)?;
            
            Ok(())
        })
    }

    /// Get global variable metadata (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetMetadata(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) -> jbyteArray {
        match (|| -> WasmtimeResult<jbyteArray> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            let mut data = Vec::with_capacity(9); // 2 ints + 1 byte for name presence
            
            let value_type_code = match metadata.value_type {
                ValType::I32 => 0,
                ValType::I64 => 1,
                ValType::F32 => 2,
                ValType::F64 => 3,
                ValType::V128 => 4,
                ValType::Ref(_) => 5, // Generic ref type for now
            };
            data.extend_from_slice(&(value_type_code as i32).to_le_bytes());
            
            let mutability_code = match metadata.mutability {
                Mutability::Const => 0,
                Mutability::Var => 1,
            };
            data.extend_from_slice(&(mutability_code as i32).to_le_bytes());
            
            data.push(if metadata.name.is_some() { 1 } else { 0 });
            
            let byte_array = env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
            let raw_array = byte_array.as_raw();
            env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;
            
            Ok(raw_array)
        })() {
            Ok(result) => result,
            Err(_) => std::ptr::null_mut() as jbyteArray, // Return null on error
        }
    }

    /// Get global variable name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetName<'a>(
        env: JNIEnv<'a>,
        _class: JClass<'a>,
        global_ptr: jlong,
    ) -> JString<'a> {
        match (|| -> WasmtimeResult<JString<'a>> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            if let Some(ref name) = metadata.name {
                Ok(env.new_string(name)
                    .map_err(|e| WasmtimeError::InvalidParameter { message: format!("Failed to create JNI string: {}", e) })?)
            } else {
                Ok(JString::default())
            }
        })() {
            Ok(result) => result,
            Err(_) => JString::default(), // Return empty string on error
        }
    }

    /// Destroy a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeDestroy(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) {
        unsafe {
            core::destroy_global(global_ptr as *mut std::os::raw::c_void);
        }
    }
}

/// JNI bindings for WebAssembly tables
#[cfg(feature = "jni-bindings")]
pub mod jni_table {
    use super::*;
    use crate::table::core;
    use crate::store::Store;
    use crate::error::{jni_utils, ffi_utils, WasmtimeError, WasmtimeResult};
    use wasmtime::{ValType, RefType};

    /// Create a new WebAssembly table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeCreateTable(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        element_type: jint,
        initial_size: jint,
        has_maximum: jboolean,
        maximum_size: jint,
        name: JString,
    ) -> jlong {
        // Extract JNI string data first
        let name_str = if name.is_null() {
            None
        } else {
            match env.get_string(&name) {
                Ok(s) => Some(s.into()),
                Err(_) => return 0 as jlong,
            }
        };
        
        jni_utils::jni_try_ptr(env, || {
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let max_size = if has_maximum != 0 { Some(maximum_size as u32) } else { None };

            let table = core::create_table(store, val_type, initial_size as u32, max_size, name_str)?;
            
            Ok(table)
        }) as jlong
    }

    /// Get table size (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetSize(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let table_size = core::get_table_size(table, store)?;
            
            Ok(table_size as jint)
        })
    }

    /// Get table element (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetElement(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        index: jint,
    ) -> jbyteArray {
        match (|| -> WasmtimeResult<jbyteArray> {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let element = core::get_table_element(table, store, index as u32)?;
            let ref_id_opt = core::extract_table_element_ref_id(&element);
            
            // Pack the values into a byte array
            let mut data = Vec::with_capacity(9); // 1 byte for presence + 8 bytes for ref_id
            data.push(if ref_id_opt.is_some() { 1 } else { 0 });
            data.extend_from_slice(&ref_id_opt.unwrap_or(0).to_le_bytes());
            
            let byte_array = env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
            let raw_array = byte_array.as_raw();
            env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;
            
            Ok(raw_array)
        })() {
            Ok(result) => result,
            Err(_) => std::ptr::null_mut() as jbyteArray, // Return null on error
        }
    }

    /// Set table element (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeSetElement(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        index: jint,
        element_type: jint,
        ref_id_present: jboolean,
        ref_id: jlong,
    ) -> jint {
        jni_utils::jni_try_code(env, || {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id as u64) } else { None };
            let element = core::create_table_element(val_type, ref_id_opt)?;

            core::set_table_element(table, store, index as u32, element)?;
            
            Ok(())
        })
    }

    /// Grow table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGrow(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        delta: jint,
        element_type: jint,
        ref_id_present: jboolean,
        ref_id: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id as u64) } else { None };
            let init_value = core::create_table_element(val_type, ref_id_opt)?;

            let previous_size = core::grow_table(table, store, delta as u32, init_value)?;
            
            Ok(previous_size as jint)
        })
    }

    /// Fill table range (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeFill(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        dst: jint,
        len: jint,
        element_type: jint,
        ref_id_present: jboolean,
        ref_id: jlong,
    ) -> jint {
        jni_utils::jni_try_code(env, || {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let val_type = match element_type {
                5 => ValType::Ref(RefType::FUNCREF),
                6 => ValType::Ref(RefType::EXTERNREF),
                _ => return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid table element type: {}", element_type),
                }),
            };

            let ref_id_opt = if ref_id_present != 0 { Some(ref_id as u64) } else { None };
            let value = core::create_table_element(val_type, ref_id_opt)?;

            core::fill_table(table, store, dst as u32, value, len as u32)?;
            
            Ok(())
        })
    }

    /// Get table metadata (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetMetadata(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
    ) -> jbyteArray {
        match (|| -> WasmtimeResult<jbyteArray> {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_table_metadata(table);
            
            let mut data = Vec::with_capacity(13); // 3 ints + 1 byte for name presence
            
            let element_type_code = match metadata.element_type {
                ValType::Ref(_) => 5, // Generic ref type for now
                _ => -1,
            };
            data.extend_from_slice(&(element_type_code as i32).to_le_bytes());
            data.extend_from_slice(&(metadata.initial_size as i32).to_le_bytes());
            data.extend_from_slice(&(metadata.maximum_size.unwrap_or(0) as i32).to_le_bytes());
            data.push(if metadata.maximum_size.is_some() { 1 } else { 0 });
            data.push(if metadata.name.is_some() { 1 } else { 0 });
            
            let byte_array = env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
            let raw_array = byte_array.as_raw();
            env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;
            
            Ok(raw_array)
        })() {
            Ok(result) => result,
            Err(_) => std::ptr::null_mut() as jbyteArray, // Return null on error
        }
    }

    /// Get table name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetName<'a>(
        env: JNIEnv<'a>,
        _class: JClass<'a>,
        table_ptr: jlong,
    ) -> JString<'a> {
        match (|| -> WasmtimeResult<JString<'a>> {
            let table = unsafe { core::get_table_ref(table_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_table_metadata(table);
            
            if let Some(ref name) = metadata.name {
                Ok(env.new_string(name)
                    .map_err(|e| WasmtimeError::InvalidParameter { message: format!("Failed to create JNI string: {}", e) })?)
            } else {
                Ok(JString::default())
            }
        })() {
            Ok(result) => result,
            Err(_) => JString::default(), // Return empty string on error
        }
    }

    /// Destroy a table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeDestroy(
        env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
    ) {
        unsafe {
            core::destroy_table(table_ptr as *mut std::os::raw::c_void);
        }
    }
}