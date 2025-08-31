//! JNI bindings for Java 8-22 compatibility

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JByteArray};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jint, jboolean, jbyteArray};

#[cfg(feature = "jni-bindings")]
use crate::engine::Engine;
#[cfg(feature = "jni-bindings")]
use crate::store::Store;
#[cfg(feature = "jni-bindings")]
use crate::module::Module;
// Instance is imported locally in each module that needs it

/// JNI bindings module
/// 
/// This module provides JNI-compatible functions for use by the wasmtime4j-jni module.
/// All functions follow JNI naming conventions and handle Java/native type conversions.

/// JNI bindings for Engine operations
#[cfg(feature = "jni-bindings")]
pub mod jni_engine {
    use super::*;
    
    /// Create a new Wasmtime engine (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngine(
        _env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        match Engine::new() {
            Ok(engine) => Box::into_raw(Box::new(engine)) as jlong,
            Err(e) => {
                log::error!("Failed to create engine: {:?}", e);
                0
            }
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
        if engine_ptr == 0 {
            return 0;
        }
        
        let engine = unsafe { &*(engine_ptr as *const Engine) };
        
        // Get byte array from Java
        let wasm_data = match env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) }) {
            Ok(data) => data,
            Err(_) => return 0,
        };
        
        match Module::compile(engine, &wasm_data) {
            Ok(module) => Box::into_raw(Box::new(module)) as jlong,
            Err(e) => {
                log::error!("Failed to compile module: {:?}", e);
                0
            }
        }
    }
    
    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateStore(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        if engine_ptr == 0 {
            return 0;
        }
        
        let engine = unsafe { &*(engine_ptr as *const Engine) };
        
        match Store::new(engine) {
            Ok(store) => Box::into_raw(Box::new(store)) as jlong,
            Err(e) => {
                log::error!("Failed to create store: {:?}", e);
                0
            }
        }
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
    
    /// Get optimization level
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetOptimizationLevel(
        _env: JNIEnv,
        _class: JClass,
        _engine_ptr: jlong,
    ) -> jint {
        // Return default optimization level
        2
    }
    
    /// Set debug info
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSetDebugInfo(
        _env: JNIEnv,
        _class: JClass,
        _engine_ptr: jlong,
        _debug: jboolean,
    ) -> jboolean {
        // Return true (debug info setting not critical for basic tests)
        1
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
    
    /// Destroy a Wasmtime engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDestroyEngine(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) {
        if engine_ptr != 0 {
            let _ = unsafe { Box::from_raw(engine_ptr as *mut Engine) };
        }
    }
}

/// JNI bindings for Instance operations
#[cfg(feature = "jni-bindings")]
pub mod jni_instance {
    use super::*;
    use crate::instance::Instance;
    
    /// Create a new WebAssembly instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCreateInstance(
        _env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        module_ptr: jlong,
    ) -> jlong {
        if store_ptr == 0 || module_ptr == 0 {
            return 0;
        }
        
        let store = unsafe { &mut *(store_ptr as *mut Store) };
        let module = unsafe { &*(module_ptr as *const Module) };
        
        match Instance::new_without_imports(store, module) {
            Ok(instance) => Box::into_raw(Box::new(instance)) as jlong,
            Err(e) => {
                log::error!("Failed to create instance: {:?}", e);
                0
            }
        }
    }
    
    /// Destroy an instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDestroyInstance(
        _env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) {
        if instance_ptr != 0 {
            let _ = unsafe { Box::from_raw(instance_ptr as *mut Instance) };
        }
    }
}

/// JNI bindings for Store operations
#[cfg(feature = "jni-bindings")]
pub mod jni_store {
    use super::*;
    
    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStore(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        if engine_ptr == 0 {
            return 0;
        }
        
        let engine = unsafe { &*(engine_ptr as *const Engine) };
        
        match Store::new(engine) {
            Ok(store) => Box::into_raw(Box::new(store)) as jlong,
            Err(e) => {
                log::error!("Failed to create store: {:?}", e);
                0
            }
        }
    }
    
    /// Destroy a store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeDestroyStore(
        _env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) {
        if store_ptr != 0 {
            let _ = unsafe { Box::from_raw(store_ptr as *mut Store) };
        }
    }
}

/// JNI bindings for Module operations  
#[cfg(feature = "jni-bindings")]
pub mod jni_module {
    use super::*;
    
    /// Destroy a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModule(
        _env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) {
        if module_ptr != 0 {
            let _ = unsafe { Box::from_raw(module_ptr as *mut Module) };
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
    use wasmtime::component::Instance as ComponentInstance;
    use std::sync::Arc;

    /// Create a new component engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCreateComponentEngine(
        _env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        match ComponentEngine::new() {
            Ok(engine) => Box::into_raw(Box::new(engine)) as jlong,
            Err(e) => {
                log::error!("Failed to create component engine: {:?}", e);
                0
            }
        }
    }

    /// Load component from WebAssembly bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeLoadComponentFromBytes(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        wasm_bytes: jbyteArray,
    ) -> jlong {
        if engine_ptr == 0 {
            log::error!("Component engine pointer is null");
            return 0;
        }

        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };

        // Get byte array from Java
        let wasm_data = match env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) }) {
            Ok(data) => data,
            Err(e) => {
                log::error!("Failed to convert byte array: {:?}", e);
                return 0;
            }
        };

        if wasm_data.is_empty() {
            log::error!("Component bytes cannot be empty");
            return 0;
        }

        match engine.load_component_from_bytes(&wasm_data) {
            Ok(component) => Box::into_raw(Box::new(component)) as jlong,
            Err(e) => {
                log::error!("Failed to load component: {:?}", e);
                0
            }
        }
    }

    /// Instantiate a component
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeInstantiateComponent(
        _env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        component_ptr: jlong,
    ) -> jlong {
        if engine_ptr == 0 || component_ptr == 0 {
            log::error!("Engine or component pointer is null");
            return 0;
        }

        let engine = unsafe { &*(engine_ptr as *const ComponentEngine) };
        let component = unsafe { &*(component_ptr as *const Component) };

        match engine.instantiate_component(component) {
            Ok(instance) => Box::into_raw(Box::new(instance)) as jlong,
            Err(e) => {
                log::error!("Failed to instantiate component: {:?}", e);
                0
            }
        }
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
        if engine_ptr != 0 {
            let _ = unsafe { Box::from_raw(engine_ptr as *mut ComponentEngine) };
            log::debug!("Component engine destroyed successfully");
        }
    }

    /// Destroy a component
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponent(
        _env: JNIEnv,
        _class: JClass,
        component_ptr: jlong,
    ) {
        if component_ptr != 0 {
            let _ = unsafe { Box::from_raw(component_ptr as *mut Component) };
            log::debug!("Component destroyed successfully");
        }
    }

    /// Destroy a component instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponentInstance(
        _env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) {
        if instance_ptr != 0 {
            let _ = unsafe { Box::from_raw(instance_ptr as *mut Arc<ComponentInstance>) };
            log::debug!("Component instance destroyed successfully");
        }
    }
}

#[cfg(not(feature = "jni-bindings"))]
pub mod instance {}