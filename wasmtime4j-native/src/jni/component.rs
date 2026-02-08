//! JNI bindings for Component operations (WASI Preview 2)
//!
//! This module provides JNI bindings for WebAssembly Component Model operations
//! including component creation, instantiation, and function invocation.

use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JByteArray};
use jni::sys::{jlong, jint, jboolean, jstring, jbyteArray, jintArray, jobjectArray};

use crate::component::Component;
use crate::error::jni_utils;

/// Create a new component engine
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCreateComponentEngine(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    // Use EnhancedComponentEngine which supports ComponentInstanceInfo with store field
    jni_utils::jni_try_ptr(&mut env, || {
        crate::component_core::core::create_enhanced_component_engine()
    }) as jlong
}

/// Load component from WebAssembly bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeLoadComponentFromBytes(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    wasm_bytes: jbyteArray,
) -> jlong {
    // Extract data before moving env into jni_try_ptr
    let wasm_data_result = env.convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java byte array: {}", e),
        });

    jni_utils::jni_try_ptr(&mut env, || {
        // Use enhanced component engine since nativeCreateComponentEngine creates EnhancedComponentEngine
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(engine_ptr as *const std::os::raw::c_void)?
        };

        // Get byte array data from extracted result
        let wasm_data = wasm_data_result?;

        crate::component_core::core::load_component_from_bytes_enhanced(engine, &wasm_data)
    }) as jlong
}

/// Instantiate a component
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeInstantiateComponent(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    component_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Use EnhancedComponentEngine which stores instances in HashMap
        let engine = unsafe {
            &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine)
        };
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };

        // This returns instance ID - instance is stored in engine's HashMap
        // to maintain proper Wasmtime ownership (Engine/Store/Instance must stay together)
        Ok(engine.instantiate_component(component)? as i64)
    })
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

    // Use EnhancedComponentEngine since nativeCreateComponentEngine creates EnhancedComponentEngine
    let engine = unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };

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

    // Use EnhancedComponentEngine since nativeCreateComponentEngine creates EnhancedComponentEngine
    let engine = unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };

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
        // Use enhanced component engine since nativeCreateComponentEngine creates EnhancedComponentEngine
        crate::component_core::core::destroy_enhanced_component_engine(engine_ptr as *mut std::os::raw::c_void);
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

/// Destroy a component instance by removing it from the engine's HashMap
///
/// This properly releases the instance by removing it from the engine,
/// allowing the Store and Instance to be dropped correctly.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponentInstance(
    _env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
) {
    use std::io::Write;
    let log = |msg: &str| {
        if let Ok(mut f) = std::fs::OpenOptions::new().create(true).append(true).open("/tmp/wasmtime4j_debug.log") {
            let _ = writeln!(f, "[nativeDestroyComponentInstance] {}", msg);
            let _ = f.flush();
        }
    };

    log(&format!("called: engine_ptr={}, instance_id={}", engine_ptr, instance_id));

    // Validate parameters
    if engine_ptr == 0 {
        log("ERROR: null engine pointer");
        return;
    }
    if instance_id == 0 {
        log("ERROR: zero instance ID");
        return;
    }

    // Get engine reference and remove instance from HashMap
    let engine = unsafe {
        &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine)
    };

    log("About to call engine.remove_instance()");
    let result = engine.remove_instance(instance_id as u64);
    log(&format!("remove_instance returned: {:?}", result));

    if let Err(e) = result {
        log(&format!("Error: {:?}", e));
    }
    log("nativeDestroyComponentInstance completed");
}

/// Invoke a component function with marshalled WIT values
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentInvokeFunction(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    function_name: jstring,
    param_type_discriminators: jintArray,
    param_data: jobjectArray,
) -> jobjectArray {
    jni_utils::jni_try_object(&mut env, |env| {
        use crate::error::WasmtimeError;

        // Validate parameters
        if engine_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Engine pointer is null".to_string(),
            });
        }
        if instance_id == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Instance ID is zero".to_string(),
            });
        }

        // Get engine and look up instance from HashMap
        // This maintains proper Wasmtime ownership (Engine/Store/Instance stay together)
        let engine = unsafe {
            &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine)
        };

        // Lock the instances HashMap and get mutable reference
        let mut instances = engine.instances.write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances write lock".to_string(),
            })?;

        let handle = instances.get_mut(&(instance_id as u64))
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Instance ID {} not found in engine", instance_id),
            })?;

        // Convert function name
        let func_name_jstring: JString = unsafe { JString::from_raw(function_name) };
        let func_name: String = env.get_string(&func_name_jstring)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert function name: {}", e),
            })?
            .into();

        // Convert parameter arrays - use int_array properly
        let discriminators_obj: JObject = unsafe { JObject::from_raw(param_type_discriminators) };
        let discriminators_array = jni::objects::JIntArray::from(discriminators_obj);
        let discriminators = unsafe {
            env.get_array_elements(&discriminators_array, jni::objects::ReleaseMode::NoCopyBack)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get type discriminators: {}", e),
                })?
        };
        let discriminators: Vec<i32> = unsafe {
            std::slice::from_raw_parts(discriminators.as_ptr(), discriminators.len())
        }.to_vec();

        let param_count = discriminators.len();

        // Convert param_data to JObjectArray
        let param_data_obj: JObject = unsafe { JObject::from_raw(param_data) };
        let param_data_typed = jni::objects::JObjectArray::from(param_data_obj);

        // Deserialize parameters to Val
        let mut params = Vec::with_capacity(param_count);
        for i in 0..param_count {
            let data_obj = env.get_object_array_element(&param_data_typed, i as i32)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get parameter {} data: {}", i, e),
                })?;

            let data_array = jni::objects::JByteArray::from(data_obj);
            let data_bytes = env.convert_byte_array(data_array)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert parameter {} data: {}", i, e),
                })?;

            let val = crate::wit_value_marshal::deserialize_to_val(discriminators[i], &data_bytes)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to deserialize parameter {}: {}", i, e),
                    backtrace: None,
                })?;

            params.push(val);
        }

        // Get the function export and call it
        // Store and Instance are kept together following Wasmtime's ownership model
        // Access through ManuallyDrop - we need ONE mutable ref so Rust can see disjoint field borrows
        use wasmtime::component::Func;

        // Get a single mutable reference so Rust can split borrows on different fields
        let handle_ref = &mut *handle;

        // Get the function using disjoint field borrows
        let func: Func = handle_ref.instance.get_func(&mut handle_ref.store, &func_name)
            .ok_or_else(|| WasmtimeError::ImportExport {
                message: format!("Function '{}' not found in component exports", func_name),
            })?;

        // Call the function - need a fresh mutable reference
        let handle_ref = &mut *handle;
        let results_len = func.ty(&handle_ref.store).results().len();
        let mut results = vec![wasmtime::component::Val::Bool(false); results_len];
        func.call(&mut handle_ref.store, &params, &mut results)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Function call failed: {}", e),
                backtrace: None,
            })?;

        let handle_ref = &mut *handle;
        func.post_return(&mut handle_ref.store)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Post-return failed: {}", e),
                backtrace: None,
            })?;

        // Handle void return (no results)
        if results.is_empty() {
            // Return null for void functions
            return Ok(jni::objects::JObject::null());
        }

        // Serialize first result
        let (result_discriminator, result_data) = crate::wit_value_marshal::serialize_from_val(&results[0])
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to serialize result: {}", e),
                backtrace: None,
            })?;

        // Create Java Object array with [discriminator, data]
        let object_class = env.find_class("java/lang/Object")
            .map_err(|e| WasmtimeError::JniError(format!("Failed to find Object class: {}", e)))?;

        let result_array = env.new_object_array(2, object_class, jni::objects::JObject::null())
            .map_err(|e| WasmtimeError::JniError(format!("Failed to create result array: {}", e)))?;

        // Set discriminator (as Integer)
        let integer_class = env.find_class("java/lang/Integer")
            .map_err(|e| WasmtimeError::JniError(format!("Failed to find Integer class: {}", e)))?;
        let integer_obj = env.new_object(
            integer_class,
            "(I)V",
            &[jni::objects::JValue::Int(result_discriminator)],
        ).map_err(|e| WasmtimeError::JniError(format!("Failed to create Integer object: {}", e)))?;

        env.set_object_array_element(&result_array, 0, integer_obj)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to set discriminator in array: {}", e)))?;

        // Set data (as byte array)
        let data_jarray = env.byte_array_from_slice(&result_data)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to create byte array: {}", e)))?;

        env.set_object_array_element(&result_array, 1, jni::objects::JObject::from(data_jarray))
            .map_err(|e| WasmtimeError::JniError(format!("Failed to set data in array: {}", e)))?;

        // Convert to static lifetime for jni_try_object return
        Ok(unsafe { jni::objects::JObject::from_raw(result_array.as_raw()) })
    })
}
