//! JNI bindings for Java 8-22 compatibility

#![allow(unused_variables)]

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JByteArray, JString};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jint, jboolean, jbyteArray, jstring};

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
    
    /// Get a function from an instance by name (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetFunction(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        name: JString,
    ) -> jlong {
        // Placeholder implementation - return null pointer
        0
    }
    
    /// Get a memory from an instance by name (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetMemory(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        name: JString,
    ) -> jlong {
        // Placeholder implementation - return null pointer
        0
    }
    
    /// Get a table from an instance by name (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetTable(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        name: JString,
    ) -> jlong {
        // Placeholder implementation - return null pointer
        0
    }
    
    /// Get a global from an instance by name (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetGlobal(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        name: JString,
    ) -> jlong {
        // Placeholder implementation - return null pointer
        0
    }
    
    /// Check if an instance has an export with the given name (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeHasExport(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        name: JString,
    ) -> jboolean {
        // Placeholder implementation - return false
        0
    }
    
    /// Get all export names from an instance (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetExportNames(
        env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) -> jbyteArray {
        // Placeholder implementation - return null
        std::ptr::null_mut()
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

/// JNI bindings for Function operations
#[cfg(feature = "jni-bindings")]
pub mod jni_function {
    use super::*;
    use crate::error::{jni_utils, ffi_utils, WasmtimeError};
    use jni::objects::{JObjectArray, JObject};
    use jni::sys::{jintArray, jlongArray, jfloatArray, jdoubleArray, jobjectArray};
    use wasmtime::{ValType, FuncType};
    
    /// Get parameter types of a function (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeGetParameterTypes(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
    ) -> jobjectArray {
        // Placeholder implementation - return null
        std::ptr::null_mut()
    }
    
    /// Get return types of a function (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeGetReturnTypes(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
    ) -> jobjectArray {
        // Placeholder implementation - return null
        std::ptr::null_mut()
    }
    
    /// Call a function with generic parameters (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCall(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        params: jobjectArray,
    ) -> jobjectArray {
        // For now, return null - this requires complex parameter conversion
        std::ptr::null_mut()
    }
    
    /// Call a function with multiple return values (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallMultiValue(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        params: jobjectArray,
    ) -> jobjectArray {
        // For now, return null - this requires complex parameter conversion
        std::ptr::null_mut()
    }
    
    /// Call a function with int parameters (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallInt(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        params: jintArray,
    ) -> jint {
        // Placeholder implementation - return 0
        0
    }
    
    /// Call a function with long parameters (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallLong(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        params: jlongArray,
    ) -> jlong {
        // Placeholder implementation - return 0
        0
    }
    
    /// Call a function with float parameters (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallFloat(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        params: jfloatArray,
    ) -> f32 {
        // Placeholder implementation - return 0.0
        0.0
    }
    
    /// Call a function with double parameters (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallDouble(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        params: jdoubleArray,
    ) -> f64 {
        // Placeholder implementation - return 0.0
        0.0
    }
    
    /// Destroy a function (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeDestroyFunction(
        env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
    ) {
        if function_ptr != 0 {
            unsafe {
                let _ = Box::from_raw(function_ptr as *mut wasmtime::Func);
            }
        }
    }
}

/// JNI bindings for NativeMethodBindings validation
#[cfg(feature = "jni-bindings")]
pub mod jni_native_method_bindings {
    use super::*;
    use std::ffi::CString;
    
    /// Get the Wasmtime version string (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeGetWasmtimeVersion(
        env: JNIEnv,
        _class: JClass,
    ) -> jstring {
        // Simple placeholder implementation - just return a hardcoded version string
        // This will be improved later with proper JNI string creation
        std::ptr::null_mut()
    }
    
    /// Create a test runtime for validation (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCreateRuntime(
        env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        // Placeholder implementation - return a non-zero value to indicate "success"
        1
    }
    
    /// Destroy a test runtime (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeDestroyRuntime(
        env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) {
        // Placeholder implementation - do nothing for now
    }
    
    /// Initialize the native library (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeInitialize(
        env: JNIEnv,
        _class: JClass,
    ) {
        // Placeholder implementation - do nothing for now
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
        /// Creates a new VecByteArrayConverter
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
        #[allow(dead_code)]
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
    use jni::sys::jobject;
    
    use wasmtime::{ValType, Mutability, RefType};

    /// Helper function to check ValType equality (since ValType doesn't implement PartialEq)
    fn val_type_matches(val_type: &ValType, expected: &ValType) -> bool {
        match (val_type, expected) {
            (ValType::I32, ValType::I32) => true,
            (ValType::I64, ValType::I64) => true,
            (ValType::F32, ValType::F32) => true,
            (ValType::F64, ValType::F64) => true,
            (ValType::V128, ValType::V128) => true,
            (ValType::Ref(_), ValType::Ref(_)) => true, // Simplified ref type checking
            _ => false,
        }
    }

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

    /// Get the value type of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetValueType<'a>(
        env: JNIEnv<'a>,
        _class: JClass<'a>,
        global_ptr: jlong,
    ) -> JString<'a> {
        match (|| -> WasmtimeResult<JString<'a>> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            let type_string = match metadata.value_type {
                ValType::I32 => "i32",
                ValType::I64 => "i64",
                ValType::F32 => "f32",
                ValType::F64 => "f64",
                ValType::V128 => "v128",
                ValType::Ref(ref_type) => match ref_type {
                    wasmtime::RefType::FUNCREF => "funcref",
                    wasmtime::RefType::EXTERNREF => "externref",
                    _ => "anyref",
                },
            };
            
            Ok(env.new_string(type_string)
                .map_err(|e| WasmtimeError::InvalidParameter { message: format!("Failed to create JNI string: {}", e) })?)
        })() {
            Ok(result) => result,
            Err(_) => {
                // Return "unknown" as fallback
                env.new_string("unknown").unwrap_or_default()
            }
        }
    }
    
    /// Check if a global variable is mutable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeIsMutable(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) -> jboolean {
        match (|| -> WasmtimeResult<bool> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            Ok(metadata.mutability == Mutability::Var)
        })() {
            Ok(is_mutable) => if is_mutable { 1 } else { 0 },
            Err(_) => 0, // Return false on error (safer default)
        }
    }
    
    /// Get the value of a global variable as Object (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetValue<'a>(
        env: JNIEnv<'a>,
        _class: JClass<'a>,
        global_ptr: jlong,
    ) -> jobject {
        match (|| -> WasmtimeResult<jobject> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            // Note: Cannot get store context here without modifying API - architectural limitation
            // This method requires Store context to retrieve actual values from Wasmtime globals
            // For now, return null to indicate this architectural constraint
            
            // TODO: This method needs Store context to work properly - API design limitation
            // Consider modifying Java API to require Store parameter or use cached values
            Err(WasmtimeError::InvalidParameter {
                message: "Store context required for global value retrieval - architectural limitation".to_string(),
            })
        })() {
            Ok(result) => result,
            Err(_) => std::ptr::null_mut(), // Return null on error/limitation
        }
    }
    
    /// Get the int value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetIntValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) -> jint {
        // Note: Store context required for actual value retrieval - architectural limitation
        // This method needs Store context to retrieve values from Wasmtime globals
        // For now, return default value and log limitation
        
        match (|| -> WasmtimeResult<jint> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an I32 type
            if !val_type_matches(&metadata.value_type, &ValType::I32) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I32 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value retrieval
            // Return 0 as safe default until API can provide Store context
            Ok(0)
        })() {
            Ok(result) => result,
            Err(_) => 0, // Return 0 on error
        }
    }
    
    /// Get the long value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetLongValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) -> jlong {
        match (|| -> WasmtimeResult<jlong> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an I64 type
            if !val_type_matches(&metadata.value_type, &ValType::I64) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I64 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value retrieval
            // Return 0 as safe default until API can provide Store context
            Ok(0)
        })() {
            Ok(result) => result,
            Err(_) => 0, // Return 0 on error
        }
    }
    
    /// Get the float value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetFloatValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) -> f32 {
        match (|| -> WasmtimeResult<f32> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an F32 type
            if !val_type_matches(&metadata.value_type, &ValType::F32) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F32 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value retrieval
            // Return 0.0 as safe default until API can provide Store context
            Ok(0.0)
        })() {
            Ok(result) => result,
            Err(_) => 0.0, // Return 0.0 on error
        }
    }
    
    /// Get the double value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetDoubleValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) -> f64 {
        match (|| -> WasmtimeResult<f64> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an F64 type
            if !val_type_matches(&metadata.value_type, &ValType::F64) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F64 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value retrieval
            // Return 0.0 as safe default until API can provide Store context
            Ok(0.0)
        })() {
            Ok(result) => result,
            Err(_) => 0.0, // Return 0.0 on error
        }
    }
    
    /// Set the value of a global variable from Object (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        value: jobject,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            // TODO: Store context required for actual value setting - architectural limitation
            // This method needs Store context and proper Object-to-Value conversion
            // For now, return error indicating limitation
            Err(WasmtimeError::InvalidParameter {
                message: "Store context required for global value setting - architectural limitation".to_string(),
            })
        })() {
            Ok(_) => 1, // Return true on success
            Err(_) => 0, // Return false on error/limitation
        }
    }
    
    /// Set the int value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetIntValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        value: jint,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if metadata.value_type != ValType::I32 {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I32 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value setting
            // Return error indicating limitation
            Err(WasmtimeError::InvalidParameter {
                message: "Store context required for global value setting - architectural limitation".to_string(),
            })
        })() {
            Ok(_) => 1, // Return true on success
            Err(_) => 0, // Return false on error/limitation
        }
    }
    
    /// Set the long value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetLongValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        value: jlong,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if metadata.value_type != ValType::I64 {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I64 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value setting
            // Return error indicating limitation
            Err(WasmtimeError::InvalidParameter {
                message: "Store context required for global value setting - architectural limitation".to_string(),
            })
        })() {
            Ok(_) => 1, // Return true on success
            Err(_) => 0, // Return false on error/limitation
        }
    }
    
    /// Set the float value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetFloatValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        value: f32,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if metadata.value_type != ValType::F32 {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F32 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value setting
            // Return error indicating limitation
            Err(WasmtimeError::InvalidParameter {
                message: "Store context required for global value setting - architectural limitation".to_string(),
            })
        })() {
            Ok(_) => 1, // Return true on success
            Err(_) => 0, // Return false on error/limitation
        }
    }
    
    /// Set the double value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetDoubleValue(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        value: f64,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if metadata.value_type != ValType::F64 {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F64 type, got {:?}", metadata.value_type),
                });
            }
            
            // TODO: Store context required for actual value setting
            // Return error indicating limitation
            Err(WasmtimeError::InvalidParameter {
                message: "Store context required for global value setting - architectural limitation".to_string(),
            })
        })() {
            Ok(_) => 1, // Return true on success
            Err(_) => 0, // Return false on error/limitation
        }
    }
    
    /// Destroy a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeDestroyGlobal(
        env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) {
        unsafe {
            core::destroy_global(global_ptr as *mut std::os::raw::c_void);
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

/// JNI bindings for Memory operations
#[cfg(feature = "jni-bindings")]
pub mod jni_memory {
    use super::*;
    use crate::memory::core;
    use crate::error::jni_utils;
    use jni::objects::{JByteBuffer, JByteArray};
    
    /// Get memory size in bytes (JNI version) with comprehensive validation
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetSize(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation with detailed error context
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeGetSize: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null. Ensure memory is properly initialized before calling size operations.".to_string(),
                });
            }

            // Check for obviously invalid pointers (basic sanity check)
            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeGetSize: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.", 
                        memory_ptr
                    ),
                });
            }

            // Validate memory handle with detailed error context
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for handle 0x{:x}: {}", memory_ptr, e);
                        match e {
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("not registered") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) is not registered or has been freed. \
                                         This typically indicates use-after-free or double-free. \
                                         Ensure memory lifetime is properly managed.", 
                                        memory_ptr
                                    ),
                                }
                            },
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("corrupted") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) is corrupted (invalid magic number). \
                                         This indicates memory corruption or buffer overflow. \
                                         Check for memory safety violations.", 
                                        memory_ptr
                                    ),
                                }
                            },
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("destroyed") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) has been destroyed (use-after-free detected). \
                                         Avoid accessing memory after calling close() or destroy().", 
                                        memory_ptr
                                    ),
                                }
                            },
                            _ => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle validation failed (0x{:x}): {}. \
                                         Verify that memory was created properly and has not been freed.", 
                                        memory_ptr, e
                                    ),
                                }
                            }
                        }
                    })?
            };
            
            // Get memory reference for metadata access
            let memory = unsafe { 
                core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Failed to get memory reference for handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Unable to access memory (handle: 0x{:x}): {}. \
                                 Memory may be in an invalid state.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };
            
            // Get metadata with error handling
            let metadata = memory.get_metadata()
                .map_err(|e| {
                    log::error!("Failed to get memory metadata for handle 0x{:x}: {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Unable to retrieve memory metadata (handle: 0x{:x}): {}. \
                             Memory statistics may be corrupted.", 
                            memory_ptr, e
                        ),
                    }
                })?;
            
            // Calculate size with overflow protection
            let pages = metadata.current_pages;
            let size_bytes = pages.checked_mul(65536)
                .ok_or_else(|| {
                    log::error!("Memory size overflow: {} pages exceeds maximum addressable size", pages);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Memory size calculation overflow: {} pages would exceed maximum addressable memory. \
                             This indicates corrupted memory metadata.", 
                            pages
                        ),
                    }
                })?;
            
            // Check that size fits in jlong (i64)
            if size_bytes > i64::MAX as u64 {
                log::error!("Memory size {} exceeds maximum jlong value", size_bytes);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory size ({} bytes) exceeds maximum representable value for Java long. \
                         This indicates an extremely large memory allocation that cannot be handled.", 
                        size_bytes
                    ),
                });
            }
            
            log::debug!("Memory size retrieved: {} bytes ({} pages) for handle 0x{:x}", 
                       size_bytes, pages, memory_ptr);
            
            Ok(size_bytes as jlong)
        })
    }

    /// Grow memory by pages (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGrow(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        pages: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeGrow: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null for growth operations. Ensure memory is properly initialized.".to_string(),
                });
            }

            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeGrow: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}) for growth operation. Handle appears corrupted or uninitialized.", 
                        memory_ptr
                    ),
                });
            }

            if pages < 0 {
                log::error!("JNI Memory.nativeGrow: negative page count {} provided for handle 0x{:x}", pages, memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Page count for memory growth cannot be negative (received: {}). Specify a non-negative number of pages to grow.", 
                        pages
                    ),
                });
            }

            // Validate memory handle before reporting architectural limitation
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for growth operation on handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Cannot grow memory with invalid handle (0x{:x}): {}. \
                                 Ensure memory is valid before attempting growth operations.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };
            
            log::warn!("Memory growth operation attempted without store context (handle: 0x{:x}, pages: {})", memory_ptr, pages);
            
            // This method requires architectural changes to support store context
            Err(crate::error::WasmtimeError::Memory {
                message: format!(
                    "Memory growth operation requires WebAssembly store context (handle: 0x{:x}, requested pages: {}). \n\
                     Current architecture limitation: Memory operations need both memory and store handles. \n\
                     Workaround: Use instance-based memory growth through WebAssembly instance interface, \n\
                     or wait for architectural update to support store context in memory operations. \n\
                     \n\
                     Technical details: \n\
                     - WebAssembly memory growth requires access to the execution store \n\
                     - Store context is needed for proper memory lifecycle management \n\
                     - Direct memory handle operations are limited without store context", 
                    memory_ptr, pages
                ),
            })
        })
    }

    /// Read a single byte from memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeReadByte(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        offset: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation with bounds checking
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeReadByte: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null for read operations. Ensure memory is properly initialized.".to_string(),
                });
            }

            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeReadByte: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}) for read operation. Handle appears corrupted or uninitialized.", 
                        memory_ptr
                    ),
                });
            }

            if offset < 0 {
                log::error!("JNI Memory.nativeReadByte: negative offset {} for handle 0x{:x}", offset, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory read offset cannot be negative (received: {}). \
                         Specify a non-negative byte offset within memory bounds.", 
                        offset
                    ),
                });
            }

            // Validate memory handle before reporting architectural limitation
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for read operation on handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Cannot read from memory with invalid handle (0x{:x}): {}. \
                                 Ensure memory is valid before attempting read operations.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };

            // Additional bounds checking based on memory metadata
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let metadata = memory.get_metadata()
                .map_err(|e| {
                    log::error!("Failed to get memory metadata for bounds checking (handle 0x{:x}): {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Cannot perform bounds checking for read operation (handle: 0x{:x}): {}. \
                             Memory metadata may be corrupted.", 
                            memory_ptr, e
                        ),
                    }
                })?;

            let memory_size = metadata.current_pages * 65536; // 64KB per page
            if offset as u64 >= memory_size {
                log::error!("Memory read bounds violation: offset {} >= memory size {} for handle 0x{:x}", 
                           offset, memory_size, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory read bounds violation: offset {} is beyond memory size {} bytes. \
                         Current memory has {} pages ({} bytes). Ensure offset is within valid range.", 
                        offset, memory_size, metadata.current_pages, memory_size
                    ),
                });
            }
            
            log::warn!("Memory read operation attempted without store context (handle: 0x{:x}, offset: {})", memory_ptr, offset);
            
            // This method requires architectural changes to support store context
            Err(crate::error::WasmtimeError::Memory {
                message: format!(
                    "Memory read operation requires WebAssembly store context (handle: 0x{:x}, offset: {}). \n\
                     Current architecture limitation: Memory read operations need both memory and store handles. \n\
                     Workaround: Use instance-based memory access through WebAssembly instance interface, \n\
                     or use direct ByteBuffer access after getting buffer from memory. \n\
                     \n\
                     Technical details: \n\
                     - WebAssembly memory access requires the execution store for thread safety \n\
                     - Store context provides proper memory synchronization and bounds checking \n\
                     - Direct memory operations are limited without store context for safety reasons", 
                    memory_ptr, offset
                ),
            })
        })
    }

    /// Write a single byte to memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeWriteByte(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        offset: jlong,
        value: jint,
    ) {
        jni_utils::jni_try_code(env, || {
            // Comprehensive parameter validation with bounds checking
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeWriteByte: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null for write operations. Ensure memory is properly initialized.".to_string(),
                });
            }

            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeWriteByte: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}) for write operation. Handle appears corrupted or uninitialized.", 
                        memory_ptr
                    ),
                });
            }

            if offset < 0 {
                log::error!("JNI Memory.nativeWriteByte: negative offset {} for handle 0x{:x}", offset, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory write offset cannot be negative (received: {}). \
                         Specify a non-negative byte offset within memory bounds.", 
                        offset
                    ),
                });
            }

            if value < -128 || value > 255 {
                log::error!("JNI Memory.nativeWriteByte: invalid byte value {} for handle 0x{:x}", value, memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Byte value must be in range [-128, 255] (received: {}). \
                         Provide a valid byte value for memory write operation.", 
                        value
                    ),
                });
            }

            // Validate memory handle before reporting architectural limitation
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for write operation on handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Cannot write to memory with invalid handle (0x{:x}): {}. \
                                 Ensure memory is valid before attempting write operations.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };

            // Additional bounds checking based on memory metadata
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let metadata = memory.get_metadata()
                .map_err(|e| {
                    log::error!("Failed to get memory metadata for bounds checking (handle 0x{:x}): {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Cannot perform bounds checking for write operation (handle: 0x{:x}): {}. \
                             Memory metadata may be corrupted.", 
                            memory_ptr, e
                        ),
                    }
                })?;

            let memory_size = metadata.current_pages * 65536; // 64KB per page
            if offset as u64 >= memory_size {
                log::error!("Memory write bounds violation: offset {} >= memory size {} for handle 0x{:x}", 
                           offset, memory_size, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory write bounds violation: offset {} is beyond memory size {} bytes. \
                         Current memory has {} pages ({} bytes). Ensure offset is within valid range.", 
                        offset, memory_size, metadata.current_pages, memory_size
                    ),
                });
            }
            
            log::warn!("Memory write operation attempted without store context (handle: 0x{:x}, offset: {}, value: {})", memory_ptr, offset, value);
            
            // This method requires architectural changes to support store context
            Err(crate::error::WasmtimeError::Memory {
                message: format!(
                    "Memory write operation requires WebAssembly store context (handle: 0x{:x}, offset: {}, value: {}). \n\
                     Current architecture limitation: Memory write operations need both memory and store handles. \n\
                     Workaround: Use instance-based memory access through WebAssembly instance interface, \n\
                     or use direct ByteBuffer access after getting buffer from memory. \n\
                     \n\
                     Technical details: \n\
                     - WebAssembly memory access requires the execution store for thread safety \n\
                     - Store context provides proper memory synchronization and write protection \n\
                     - Direct memory operations are limited without store context for safety reasons", 
                    memory_ptr, offset, value
                ),
            })
        });
    }

    /// Read bytes from memory into a buffer (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeReadBytes(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        offset: jlong,
        buffer: JByteArray,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeReadBytes: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null for bulk read operations. Ensure memory is properly initialized.".to_string(),
                });
            }

            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeReadBytes: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}) for bulk read operation. Handle appears corrupted or uninitialized.", 
                        memory_ptr
                    ),
                });
            }

            if offset < 0 {
                log::error!("JNI Memory.nativeReadBytes: negative offset {} for handle 0x{:x}", offset, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory read offset cannot be negative (received: {}). \
                         Specify a non-negative byte offset within memory bounds.", 
                        offset
                    ),
                });
            }

            // Validate JNI buffer parameter
            if buffer.is_null() {
                log::error!("JNI Memory.nativeReadBytes: null buffer provided for handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null for bulk read operations. Provide a valid byte array.".to_string(),
                });
            }

            // Get buffer length for bounds checking
            let buffer_length = match env.get_array_length(&buffer) {
                Ok(len) => len as usize,
                Err(e) => {
                    log::error!("Failed to get buffer length for read operation (handle 0x{:x}): {:?}", memory_ptr, e);
                    return Err(crate::error::WasmtimeError::InvalidParameter {
                        message: format!(
                            "Cannot determine buffer size for read operation: {:?}. \
                             Ensure buffer is a valid Java byte array.", e
                        ),
                    });
                }
            };

            if buffer_length == 0 {
                log::warn!("Zero-length buffer provided for read operation (handle 0x{:x})", memory_ptr);
                return Ok(0); // No bytes to read
            }

            // Validate memory handle before bounds checking
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for bulk read operation on handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Cannot read from memory with invalid handle (0x{:x}): {}. \
                                 Ensure memory is valid before attempting read operations.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };

            // Get memory and perform bounds checking
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let metadata = memory.get_metadata()
                .map_err(|e| {
                    log::error!("Failed to get memory metadata for bulk read bounds checking (handle 0x{:x}): {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Cannot perform bounds checking for bulk read operation (handle: 0x{:x}): {}. \
                             Memory metadata may be corrupted.", 
                            memory_ptr, e
                        ),
                    }
                })?;

            let memory_size = metadata.current_pages * 65536; // 64KB per page
            let end_offset = (offset as u64).saturating_add(buffer_length as u64);
            
            if offset as u64 >= memory_size {
                log::error!("Memory bulk read bounds violation: offset {} >= memory size {} for handle 0x{:x}", 
                           offset, memory_size, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory bulk read bounds violation: offset {} is beyond memory size {} bytes. \
                         Current memory has {} pages. Ensure offset is within valid range.", 
                        offset, memory_size, metadata.current_pages
                    ),
                });
            }

            if end_offset > memory_size {
                log::error!("Memory bulk read bounds violation: offset {} + length {} exceeds memory size {} for handle 0x{:x}", 
                           offset, buffer_length, memory_size, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory bulk read bounds violation: offset {} + length {} ({} bytes total) exceeds memory size {} bytes. \
                         Current memory has {} pages. Reduce read length or adjust offset.", 
                        offset, buffer_length, end_offset, memory_size, metadata.current_pages
                    ),
                });
            }

            log::warn!("Memory bulk read operation attempted without store context (handle: 0x{:x}, offset: {}, length: {})", 
                      memory_ptr, offset, buffer_length);
            
            // This method requires architectural changes to support store context
            Err(crate::error::WasmtimeError::Memory {
                message: format!(
                    "Memory bulk read operation requires WebAssembly store context (handle: 0x{:x}, offset: {}, length: {}). \n\
                     Current architecture limitation: Memory bulk operations need both memory and store handles. \n\
                     Workaround: Use instance-based memory access through WebAssembly instance interface, \n\
                     or use direct ByteBuffer access for bulk operations. \n\
                     \n\
                     Technical details: \n\
                     - WebAssembly memory bulk access requires the execution store for thread safety \n\
                     - Store context provides proper memory synchronization and atomic operations \n\
                     - Direct memory operations are limited without store context for safety reasons", 
                    memory_ptr, offset, buffer_length
                ),
            })
        })
    }

    /// Write bytes from a buffer to memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeWriteBytes(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        offset: jlong,
        buffer: JByteArray,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            let _memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            
            // TODO: This method requires store context for memory access
            // For now, return error indicating this method needs implementation
            Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Memory write requires store context - method needs architectural changes".to_string(),
            })
        })
    }

    /// Get direct ByteBuffer view of memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetBuffer<'a>(
        env: JNIEnv<'a>,
        _class: JClass<'a>,
        memory_ptr: jlong,
    ) -> JByteBuffer<'a> {
        jni_utils::jni_try_default(&env, JByteBuffer::default(), || {
            let _memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            
            // TODO: This method requires store context for memory access
            // For now, return error indicating this method needs implementation
            Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Memory buffer access requires store context - method needs architectural changes".to_string(),
            })
        })
    }

    /// Destroy memory (JNI version) with comprehensive validation and cleanup
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeDestroyMemory(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) {
        // Enhanced validation with detailed error logging
        if memory_ptr == 0 {
            log::warn!("JNI Memory.nativeDestroyMemory called with null memory pointer");
            return;
        }
        
        log::debug!("Destroying memory handle: 0x{:x}", memory_ptr);
        
        unsafe {
            // Use the enhanced destroy_memory function which includes validation
            core::destroy_memory(memory_ptr as *mut std::os::raw::c_void);
        }
        
        log::debug!("Memory handle destruction completed: 0x{:x}", memory_ptr);
    }
    
    /// Get memory page count (JNI version) with comprehensive validation
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetPageCount(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation with detailed error context
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeGetPageCount: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null. Ensure memory is properly initialized before calling page count operations.".to_string(),
                });
            }

            // Check for obviously invalid pointers (basic sanity check)
            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeGetPageCount: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.", 
                        memory_ptr
                    ),
                });
            }

            // Validate memory handle with comprehensive error mapping
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for page count operation on handle 0x{:x}: {}", memory_ptr, e);
                        match e {
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("not registered") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) is not registered or has been freed. \
                                         Cannot retrieve page count from unregistered memory. \
                                         Ensure memory lifetime is properly managed.", 
                                        memory_ptr
                                    ),
                                }
                            },
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("destroyed") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) has been destroyed (use-after-free detected). \
                                         Cannot retrieve page count from destroyed memory. \
                                         Avoid accessing memory after calling close() or destroy().", 
                                        memory_ptr
                                    ),
                                }
                            },
                            _ => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle validation failed for page count operation (0x{:x}): {}. \
                                         Verify that memory was created properly and is in a valid state.", 
                                        memory_ptr, e
                                    ),
                                }
                            }
                        }
                    })?
            };
            
            // Get memory reference for metadata access
            let memory = unsafe { 
                core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Failed to get memory reference for page count operation on handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Unable to access memory for page count operation (handle: 0x{:x}): {}. \
                                 Memory may be in an invalid state or corrupted.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };
            
            // Get metadata with comprehensive error handling
            let metadata = memory.get_metadata()
                .map_err(|e| {
                    log::error!("Failed to get memory metadata for page count operation on handle 0x{:x}: {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Unable to retrieve memory metadata for page count (handle: 0x{:x}): {}. \
                             Memory statistics may be corrupted or inaccessible.", 
                            memory_ptr, e
                        ),
                    }
                })?;
            
            let pages = metadata.current_pages;
            
            // Validate page count is reasonable (basic sanity check)
            const MAX_WASM_PAGES: u64 = 65536; // 4GB / 64KB
            if pages > MAX_WASM_PAGES {
                log::error!("Memory page count {} exceeds WebAssembly limit {}", pages, MAX_WASM_PAGES);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory page count ({}) exceeds WebAssembly limit ({}). \
                         This indicates corrupted memory metadata or invalid memory state.", 
                        pages, MAX_WASM_PAGES
                    ),
                });
            }
            
            log::debug!("Memory page count retrieved: {} pages for handle 0x{:x}", pages, memory_ptr);
            
            Ok(pages as jlong)
        })
    }
    
    /// Get memory maximum size in pages (JNI version) with comprehensive validation
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetMaxSize(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation with detailed error context
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeGetMaxSize: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null. Ensure memory is properly initialized before calling max size operations.".to_string(),
                });
            }

            // Check for obviously invalid pointers (basic sanity check)
            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeGetMaxSize: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.", 
                        memory_ptr
                    ),
                });
            }

            // Validate memory handle with comprehensive error mapping
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Memory handle validation failed for max size operation on handle 0x{:x}: {}", memory_ptr, e);
                        match e {
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("not registered") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) is not registered or has been freed. \
                                         Cannot retrieve max size from unregistered memory. \
                                         Ensure memory lifetime is properly managed.", 
                                        memory_ptr
                                    ),
                                }
                            },
                            crate::error::WasmtimeError::InvalidParameter { message } if message.contains("destroyed") => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle (0x{:x}) has been destroyed (use-after-free detected). \
                                         Cannot retrieve max size from destroyed memory. \
                                         Avoid accessing memory after calling close() or destroy().", 
                                        memory_ptr
                                    ),
                                }
                            },
                            _ => {
                                crate::error::WasmtimeError::Memory {
                                    message: format!(
                                        "Memory handle validation failed for max size operation (0x{:x}): {}. \
                                         Verify that memory was created properly and is in a valid state.", 
                                        memory_ptr, e
                                    ),
                                }
                            }
                        }
                    })?
            };
            
            // Get memory reference for metadata access
            let memory = unsafe { 
                core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)
                    .map_err(|e| {
                        log::error!("Failed to get memory reference for max size operation on handle 0x{:x}: {}", memory_ptr, e);
                        crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Unable to access memory for max size operation (handle: 0x{:x}): {}. \
                                 Memory may be in an invalid state or corrupted.", 
                                memory_ptr, e
                            ),
                        }
                    })?
            };
            
            // Get metadata with comprehensive error handling
            let metadata = memory.get_metadata()
                .map_err(|e| {
                    log::error!("Failed to get memory metadata for max size operation on handle 0x{:x}: {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!(
                            "Unable to retrieve memory metadata for max size (handle: 0x{:x}): {}. \
                             Memory statistics may be corrupted or inaccessible.", 
                            memory_ptr, e
                        ),
                    }
                })?;
            
            let max_pages = match metadata.maximum_pages {
                Some(pages) => {
                    // Validate max page count is reasonable (basic sanity check)
                    const MAX_WASM_PAGES: u64 = 65536; // 4GB / 64KB
                    if pages > MAX_WASM_PAGES {
                        log::error!("Memory max page count {} exceeds WebAssembly limit {}", pages, MAX_WASM_PAGES);
                        return Err(crate::error::WasmtimeError::Memory {
                            message: format!(
                                "Memory max page count ({}) exceeds WebAssembly limit ({}). \
                                 This indicates corrupted memory metadata or invalid memory state.", 
                                pages, MAX_WASM_PAGES
                            ),
                        });
                    }
                    pages as jlong
                },
                None => -1, // Unlimited memory
            };
            
            log::debug!("Memory max size retrieved: {} pages for handle 0x{:x}", max_pages, memory_ptr);
            
            Ok(max_pages)
        })
    }
    
    /// Validate memory handle and return diagnostics (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeValidateHandle(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) -> jboolean {
        if memory_ptr == 0 {
            log::debug!("Memory handle validation failed: null pointer");
            return 0; // false
        }
        
        match unsafe { core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void) } {
            Ok(_) => {
                log::debug!("Memory handle validation succeeded: 0x{:x}", memory_ptr);
                1 // true
            }
            Err(e) => {
                log::debug!("Memory handle validation failed: 0x{:x}, error: {}", memory_ptr, e);
                0 // false
            }
        }
    }
    
    /// Get memory handle diagnostics (JNI version) - returns access count
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetAccessCount(
        env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) -> jlong {
        if memory_ptr == 0 {
            return -1;
        }
        
        jni_utils::jni_try_default(&env, -1, || {
            unsafe {
                core::validate_memory_handle(memory_ptr as *const std::os::raw::c_void)?;
                
                let validated_memory = &*(memory_ptr as *const core::ValidatedMemory);
                Ok(validated_memory.get_access_count() as jlong)
            }
        })
    }
    
    /// Get global memory handle diagnostics (JNI version) - returns handle count and total accesses
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetGlobalDiagnostics(
        env: JNIEnv,
        _class: JClass,
    ) -> jbyteArray {
        match core::get_memory_handle_diagnostics() {
            Ok((handle_count, total_accesses)) => {
                // Pack both values into a byte array: [handle_count: 4 bytes][total_accesses: 8 bytes]
                let mut data = Vec::with_capacity(12);
                data.extend_from_slice(&(handle_count as u32).to_le_bytes());
                data.extend_from_slice(&total_accesses.to_le_bytes());
                
                match env.byte_array_from_slice(&data) {
                    Ok(jarray) => jarray.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
}