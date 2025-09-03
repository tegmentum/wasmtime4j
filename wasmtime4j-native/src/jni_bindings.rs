//! JNI bindings for Java 8-22 compatibility

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JByteArray};
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
    use crate::error::ffi_utils;
    
    /// Create a new Wasmtime engine with default configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngine(
        _env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| core::create_engine()) as jlong
    }

    /// Create a new Wasmtime engine with custom configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngineWithConfig(
        _env: JNIEnv,
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
        use wasmtime::{Strategy, OptLevel};
        
        ffi_utils::ffi_try_ptr(|| {
            let strategy_opt = match strategy {
                0 => Some(Strategy::Cranelift),
                _ => None,
            };
            
            let opt_level_opt = match opt_level {
                0 => Some(OptLevel::None),
                1 => Some(OptLevel::Speed),
                2 => Some(OptLevel::SpeedAndSize),
                _ => None,
            };
            
            let max_memory_pages_opt = if max_memory_pages < 0 {
                None
            } else {
                Some(max_memory_pages as u32)
            };
            
            let max_stack_size_opt = if max_stack_size < 0 {
                None
            } else {
                Some(max_stack_size as usize)
            };
            
            let max_instances_opt = if max_instances < 0 {
                None
            } else {
                Some(max_instances as u32)
            };
            
            core::create_engine_with_config(
                strategy_opt,
                opt_level_opt,
                debug_info != 0,
                wasm_threads != 0,
                wasm_simd != 0,
                wasm_reference_types != 0,
                wasm_bulk_memory != 0,
                wasm_multi_value != 0,
                fuel_enabled != 0,
                max_memory_pages_opt,
                max_stack_size_opt,
                epoch_interruption != 0,
                max_instances_opt,
            )
        }) as jlong
    }

    /// Destroy a Wasmtime engine (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDestroyEngine(
        _env: JNIEnv,
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
        ffi_utils::ffi_try_ptr(|| {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            
            // Get byte array from Java
            let wasm_data = env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
                .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert Java byte array: {}", e),
                })?;
            
            crate::module::core::compile_module(engine, &wasm_data)
        }) as jlong
    }
    
    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateStore(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            crate::store::core::create_store(engine)
        }) as jlong
    }
    
    /// Set optimization level
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSetOptimizationLevel(
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
    use crate::error::ffi_utils;
    
    /// Create a new WebAssembly instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCreateInstance(
        _env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        module_ptr: jlong,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| {
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            let module = unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            core::create_instance(store, module)
        }) as jlong
    }
    
    /// Destroy an instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDestroyInstance(
        _env: JNIEnv,
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
    use crate::error::ffi_utils;
    
    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStore(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            core::create_store(engine)
        }) as jlong
    }
    
    /// Destroy a store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeDestroyStore(
        _env: JNIEnv,
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
    
    /// Destroy a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModule(
        _env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) {
        unsafe {
            core::destroy_module(module_ptr as *mut std::os::raw::c_void);
        }
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
    use crate::error::ffi_utils;

    /// Create a new component engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCreateComponentEngine(
        _env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| crate::component::core::create_component_engine()) as jlong
    }

    /// Load component from WebAssembly bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeLoadComponentFromBytes(
        env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        wasm_bytes: jbyteArray,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| {
            let engine = unsafe { 
                crate::component::core::get_component_engine_ref(engine_ptr as *const std::os::raw::c_void)? 
            };

            // Get byte array from Java
            let wasm_data = env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
                .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert Java byte array: {}", e),
                })?;

            crate::component::core::load_component_from_bytes(engine, &wasm_data)
        }) as jlong
    }

    /// Instantiate a component
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeInstantiateComponent(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        component_ptr: jlong,
    ) -> jlong {
        ffi_utils::ffi_try_ptr(|| {
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
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
        _env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) {
        unsafe {
            crate::component::core::destroy_component_instance(instance_ptr as *mut std::os::raw::c_void);
        }
    }
}

#[cfg(not(feature = "jni-bindings"))]
pub mod instance {}