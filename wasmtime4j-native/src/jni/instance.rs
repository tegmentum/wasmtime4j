//! JNI bindings for Instance operations

use jni::objects::{JClass, JObject, JString, JValue};
use jni::sys::{jboolean, jint, jintArray, jlong, jobject, jobjectArray};
use jni::JNIEnv;

use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
use crate::instance::{core, InstanceState};

use std::os::raw::c_void;

/// Get the current lifecycle state of an instance (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetState(
    mut env: JNIEnv,
    _obj: jobject,
    instance_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        let instance = unsafe { core::get_instance_ref(instance_ptr as *const c_void)? };
        let state = core::get_instance_state(instance);
        Ok(state as i32)
    })
}

/// Perform comprehensive resource cleanup for an instance (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCleanupResources(
    mut env: JNIEnv,
    _obj: jobject,
    instance_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_mut(instance_ptr as *mut c_void)? };
        let cleaned_up = core::cleanup_instance_resources(instance)?;
        Ok(if cleaned_up { 1 } else { 0 })
    })
}

/// Check if instance has been cleaned up (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeIsCleanedUp(
    mut env: JNIEnv,
    _obj: jobject,
    instance_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_ptr as *const c_void)? };
        let is_cleaned = core::is_instance_cleaned_up(instance);
        Ok(if is_cleaned { 1 } else { 0 })
    })
}

/// Validate cross-thread instance access (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeValidateThreadAccess(
    mut env: JNIEnv,
    _obj: jobject,
    instance_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_ptr as *const c_void)? };
        match core::validate_instance_thread_access(instance) {
            Ok(_) => Ok(1),
            Err(_) => Ok(0),
        }
    })
}

/// Set the lifecycle state of an instance (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeSetState(
    mut env: JNIEnv,
    _obj: jobject,
    instance_ptr: jlong,
    state: jint,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_mut(instance_ptr as *mut c_void)? };
        let instance_state = match state {
            0 => InstanceState::Creating,
            1 => InstanceState::Created,
            2 => InstanceState::Running,
            3 => InstanceState::Suspended,
            4 => InstanceState::Error,
            5 => InstanceState::Disposed,
            6 => InstanceState::Destroying,
            _ => return Ok(0), // Invalid state
        };
        core::set_instance_state(instance, instance_state);
        Ok(1)
    })
}

/// Get all export names from an instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetExportNames<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    instance_ptr: jlong,
) -> jobjectArray {
    let result = (|| -> WasmtimeResult<jobjectArray> {
        let instance = unsafe { core::get_instance_ref(instance_ptr as *const c_void)? };
        let exports = core::get_all_exports(instance);

        // Get the keys (export names) from the HashMap
        let export_names: Vec<String> = exports.keys().cloned().collect();

        // Create a Java String array
        let string_class =
            env.find_class("java/lang/String")
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to find String class: {}", e),
                })?;

        let output_array = env
            .new_object_array(export_names.len() as i32, string_class, JObject::null())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create array: {}", e),
            })?;

        // Fill the array with export names
        for (i, name) in export_names.iter().enumerate() {
            let jstr = env
                .new_string(name)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create string: {}", e),
                })?;
            env.set_object_array_element(&output_array, i as i32, jstr)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to set array element: {}", e),
                })?;
        }

        Ok(output_array.as_raw())
    })();

    match result {
        Ok(array) => array,
        Err(_) => {
            // Return empty array on error - find String class again since it was moved
            if let Ok(string_class) = env.find_class("java/lang/String") {
                if let Ok(empty) = env.new_object_array(0, string_class, JObject::null()) {
                    return empty.as_raw();
                }
            }
            std::ptr::null_mut()
        }
    }
}

// ========================================================================
// Helper functions for Java/Wasm value conversion
// ========================================================================

/// Convert a single Java object to WasmValue
///
/// Handles WasmValue objects, boxed primitives (Integer, Long, Float, Double),
/// and returns an error for unsupported types.
fn convert_java_object_to_wasm_value(
    env: &mut JNIEnv,
    param_obj: &JObject,
    index: i32,
) -> WasmtimeResult<crate::instance::WasmValue> {
    use crate::instance::WasmValue;

    // Check if it's a WasmValue object
    let is_wasm_value = env
        .is_instance_of(param_obj, "ai/tegmentum/wasmtime4j/WasmValue")
        .unwrap_or(false);

    if is_wasm_value {
        // Get the type from WasmValue.getType()
        let type_obj = env
            .call_method(
                param_obj,
                "getType",
                "()Lai/tegmentum/wasmtime4j/WasmValueType;",
                &[],
            )
            .and_then(|v| v.l())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get type from WasmValue: {}", e),
            })?;

        // Get the type ordinal to determine which type it is
        let type_ordinal = env
            .call_method(&type_obj, "ordinal", "()I", &[])
            .and_then(|v| v.i())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get type ordinal: {}", e),
            })?;

        // Get the value from WasmValue.getValue()
        let value_obj = env
            .call_method(param_obj, "getValue", "()Ljava/lang/Object;", &[])
            .and_then(|v| v.l())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get value from WasmValue: {}", e),
            })?;

        // Convert based on type ordinal (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FUNCREF, 6=EXTERNREF)
        match type_ordinal {
            0 => {
                // I32
                let value = env
                    .call_method(&value_obj, "intValue", "()I", &[])
                    .and_then(|v| v.i())
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to extract I32 value: {}", e),
                    })?;
                Ok(WasmValue::I32(value))
            }
            1 => {
                // I64
                let value = env
                    .call_method(&value_obj, "longValue", "()J", &[])
                    .and_then(|v| v.j())
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to extract I64 value: {}", e),
                    })?;
                Ok(WasmValue::I64(value))
            }
            2 => {
                // F32
                let value = env
                    .call_method(&value_obj, "floatValue", "()F", &[])
                    .and_then(|v| v.f())
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to extract F32 value: {}", e),
                    })?;
                Ok(WasmValue::F32(value))
            }
            3 => {
                // F64
                let value = env
                    .call_method(&value_obj, "doubleValue", "()D", &[])
                    .and_then(|v| v.d())
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to extract F64 value: {}", e),
                    })?;
                Ok(WasmValue::F64(value))
            }
            5 => {
                // FUNCREF
                let ref_value = if !value_obj.is_null() {
                    env.call_method(&value_obj, "getId", "()J", &[])
                        .and_then(|v| v.j())
                        .ok()
                        .or_else(|| {
                            env.call_method(&value_obj, "longValue", "()J", &[])
                                .and_then(|v| v.j())
                                .ok()
                        })
                } else {
                    None
                };
                Ok(WasmValue::FuncRef(ref_value))
            }
            6 => {
                // EXTERNREF
                let ref_value = if !value_obj.is_null() {
                    env.call_method(&value_obj, "longValue", "()J", &[])
                        .and_then(|v| v.j())
                        .ok()
                } else {
                    None
                };
                Ok(WasmValue::ExternRef(ref_value))
            }
            _ => Err(WasmtimeError::InvalidParameter {
                message: format!("Unsupported WasmValue type ordinal: {}", type_ordinal),
            }),
        }
    } else if env
        .is_instance_of(param_obj, "java/lang/Integer")
        .unwrap_or(false)
    {
        let value = env
            .call_method(param_obj, "intValue", "()I", &[])
            .and_then(|v| v.i())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to extract int value: {}", e),
            })?;
        Ok(WasmValue::I32(value))
    } else if env
        .is_instance_of(param_obj, "java/lang/Long")
        .unwrap_or(false)
    {
        let value = env
            .call_method(param_obj, "longValue", "()J", &[])
            .and_then(|v| v.j())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to extract long value: {}", e),
            })?;
        Ok(WasmValue::I64(value))
    } else if env
        .is_instance_of(param_obj, "java/lang/Float")
        .unwrap_or(false)
    {
        let value = env
            .call_method(param_obj, "floatValue", "()F", &[])
            .and_then(|v| v.f())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to extract float value: {}", e),
            })?;
        Ok(WasmValue::F32(value))
    } else if env
        .is_instance_of(param_obj, "java/lang/Double")
        .unwrap_or(false)
    {
        let value = env
            .call_method(param_obj, "doubleValue", "()D", &[])
            .and_then(|v| v.d())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to extract double value: {}", e),
            })?;
        Ok(WasmValue::F64(value))
    } else {
        // Unknown parameter type - get class name for debugging
        let class =
            env.get_object_class(param_obj)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get parameter {} class: {}", index, e),
                })?;
        let class_name_obj = env
            .call_method(&class, "getName", "()Ljava/lang/String;", &[])
            .and_then(|v| v.l())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get class name: {}", e),
            })?;
        let class_name: String = env
            .get_string(&class_name_obj.into())
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert class name: {}", e),
            })?
            .into();

        Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Unsupported parameter type at index {}: {}",
                index, class_name
            ),
        })
    }
}

/// Convert Java Object array to Vec<WasmValue>
fn convert_java_params_to_wasm(
    env: &mut JNIEnv,
    params_array: &jni::objects::JObjectArray,
) -> WasmtimeResult<Vec<crate::instance::WasmValue>> {
    let param_count =
        env.get_array_length(params_array)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get array length: {}", e),
            })?;

    let mut wasm_params = Vec::with_capacity(param_count as usize);

    for i in 0..param_count {
        let param_obj = env.get_object_array_element(params_array, i).map_err(|e| {
            WasmtimeError::InvalidParameter {
                message: format!("Failed to get parameter {}: {}", i, e),
            }
        })?;

        let wasm_value = convert_java_object_to_wasm_value(env, &param_obj, i)?;
        wasm_params.push(wasm_value);
    }

    Ok(wasm_params)
}

/// Convert a single WasmValue to Java object (WasmValue)
fn convert_wasm_value_to_java<'a>(
    env: &mut JNIEnv<'a>,
    val: &crate::instance::WasmValue,
) -> WasmtimeResult<JObject<'a>> {
    use crate::instance::WasmValue;

    let wasm_value_class = env
        .find_class("ai/tegmentum/wasmtime4j/WasmValue")
        .map_err(|e| WasmtimeError::InvalidParameter {
            message: format!("Failed to find WasmValue class: {}", e),
        })?;

    match val {
        WasmValue::I32(v) => {
            let integer_value = JValue::from(*v);
            env.call_static_method(
                wasm_value_class,
                "i32",
                "(I)Lai/tegmentum/wasmtime4j/WasmValue;",
                &[integer_value],
            )
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create WasmValue.i32: {}", e),
            })?
            .l()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get WasmValue.i32 result: {}", e),
            })
        }
        WasmValue::I64(v) => {
            let long_value = JValue::from(*v);
            env.call_static_method(
                wasm_value_class,
                "i64",
                "(J)Lai/tegmentum/wasmtime4j/WasmValue;",
                &[long_value],
            )
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create WasmValue.i64: {}", e),
            })?
            .l()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get WasmValue.i64 result: {}", e),
            })
        }
        WasmValue::F32(v) => {
            let float_value = JValue::from(*v);
            env.call_static_method(
                wasm_value_class,
                "f32",
                "(F)Lai/tegmentum/wasmtime4j/WasmValue;",
                &[float_value],
            )
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create WasmValue.f32: {}", e),
            })?
            .l()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get WasmValue.f32 result: {}", e),
            })
        }
        WasmValue::F64(v) => {
            let double_value = JValue::from(*v);
            env.call_static_method(
                wasm_value_class,
                "f64",
                "(D)Lai/tegmentum/wasmtime4j/WasmValue;",
                &[double_value],
            )
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create WasmValue.f64: {}", e),
            })?
            .l()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get WasmValue.f64 result: {}", e),
            })
        }
        WasmValue::ExternRef(ref_value) => match ref_value {
            Some(id) => {
                let long_class = env.find_class("java/lang/Long")?;
                let long_obj = env
                    .new_object(long_class, "(J)V", &[JValue::from(*id)])
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to create Long for externref: {}", e),
                    })?;
                env.call_static_method(
                    wasm_value_class,
                    "externref",
                    "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[JValue::from(&long_obj)],
                )
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create WasmValue.externref: {}", e),
                })?
                .l()
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get WasmValue.externref result: {}", e),
                })
            }
            None => {
                let null_obj = JObject::null();
                env.call_static_method(
                    wasm_value_class,
                    "externref",
                    "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[JValue::from(&null_obj)],
                )
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create WasmValue.externref(null): {}", e),
                })?
                .l()
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get WasmValue.externref(null) result: {}", e),
                })
            }
        },
        WasmValue::FuncRef(ref_id) => match ref_id {
            Some(id) => {
                let long_class = env.find_class("java/lang/Long").map_err(|e| {
                    WasmtimeError::InvalidParameter {
                        message: format!("Failed to find Long class: {}", e),
                    }
                })?;
                let long_obj = env
                    .new_object(long_class, "(J)V", &[JValue::Long(*id)])
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to create Long object: {}", e),
                    })?;
                env.call_static_method(
                    wasm_value_class,
                    "funcref",
                    "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[JValue::from(&long_obj)],
                )
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create WasmValue.funcref: {}", e),
                })?
                .l()
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get WasmValue.funcref result: {}", e),
                })
            }
            None => {
                let null_obj = JObject::null();
                env.call_static_method(
                    wasm_value_class,
                    "funcref",
                    "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[JValue::from(&null_obj)],
                )
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create WasmValue.funcref(null): {}", e),
                })?
                .l()
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get WasmValue.funcref(null) result: {}", e),
                })
            }
        },
        _ => Ok(JObject::null()), // Skip unsupported types
    }
}

/// Convert Vec<WasmValue> to Java Object array
fn convert_wasm_results_to_java<'a>(
    env: &mut JNIEnv<'a>,
    results: &[crate::instance::WasmValue],
) -> WasmtimeResult<jni::objects::JObjectArray<'a>> {
    let object_class =
        env.find_class("java/lang/Object")
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to find Object class: {}", e),
            })?;

    let result_array = env
        .new_object_array(results.len() as i32, object_class, JObject::null())
        .map_err(|e| WasmtimeError::InvalidParameter {
            message: format!("Failed to create result array: {}", e),
        })?;

    for (i, val) in results.iter().enumerate() {
        let java_obj = convert_wasm_value_to_java(env, val)?;
        if !java_obj.is_null() {
            env.set_object_array_element(&result_array, i as i32, java_obj)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to set array element: {}", e),
                })?;
        }
    }

    Ok(result_array)
}

// ========================================================================
// End of helper functions
// ========================================================================

/// Call a WebAssembly function directly using Instance::call_export_function
///
/// This function has been refactored to use helper functions for Java/Wasm
/// value conversion, improving readability and maintainability.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCallFunction(
    mut env: JNIEnv,
    _class: JClass,
    instance_ptr: jlong,
    store_ptr: jlong,
    function_name: JString,
    params: jobjectArray,
) -> jobjectArray {
    use crate::instance::ExecutionResult;

    let result = (|| -> WasmtimeResult<jobjectArray> {
        // Get instance and store references
        let instance = unsafe { core::get_instance_mut(instance_ptr as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

        // Convert function name
        let name_str: String = env
            .get_string(&function_name)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert function name: {}", e),
            })?
            .into();

        // Convert Java parameters to WasmValue
        use jni::objects::JObjectArray;
        let params_array = unsafe { JObjectArray::from_raw(params) };
        let wasm_params = convert_java_params_to_wasm(&mut env, &params_array)?;

        // Call the function
        let exec_result: ExecutionResult =
            instance.call_export_function(store, &name_str, &wasm_params)?;

        // Convert results to Java Object array
        let result_array = convert_wasm_results_to_java(&mut env, &exec_result.values)?;

        Ok(result_array.as_raw())
    })();

    match result {
        Ok(array) => array,
        Err(e) => {
            let error_msg = format!("{}", e);
            let _ = env.throw_new("java/lang/RuntimeException", error_msg);
            std::ptr::null_mut()
        }
    }
}

/// Get a function export from the instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetFunction(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    name: JString,
) -> jlong {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Get the function export
        let func_opt = instance.get_func(store, &name_str)?;

        match func_opt {
            Some(func) => {
                // Create FunctionHandle with cached type information
                let func_handle =
                    crate::jni::function::FunctionHandle::new(func, name_str.clone(), store);
                Ok(Box::into_raw(Box::new(func_handle)) as jlong)
            }
            None => Ok(0), // Return 0 for not found
        }
    })
}

/// Get a global export from the instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetGlobal(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    name: JString,
) -> jlong {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Get the global export
        let global_opt = instance.get_global(store, &name_str)?;

        match global_opt {
            Some(global) => {
                // Create a new Global wrapper and return its pointer
                let global_wrapper =
                    crate::global::Global::from_wasmtime_global(global, store, Some(name_str))?;
                Ok(Box::into_raw(Box::new(global_wrapper)) as jlong)
            }
            None => Ok(0), // Return 0 for not found
        }
    })
}

/// Get a memory export from the instance
///
/// This method tries to get regular memory first. If not found, it also tries
/// to get shared memory (for modules using the threads proposal).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetMemory(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    name: JString,
) -> jlong {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // First, try to get regular memory export
        let memory_opt = instance.get_memory(store, &name_str)?;

        if let Some(memory) = memory_opt {
            // Found regular memory - wrap it
            let memory_type = store.with_context_ro(|ctx| Ok(memory.ty(ctx)))?;
            let memory_wrapper = crate::memory::Memory::from_wasmtime_memory(memory, memory_type);
            let validated_ptr = crate::memory::core::create_validated_memory(memory_wrapper)?;
            return Ok(validated_ptr as jlong);
        }

        // Regular memory not found - try shared memory
        let shared_memory_opt = instance.get_shared_memory(store, &name_str)?;

        if let Some(shared_memory) = shared_memory_opt {
            // Found shared memory - wrap it using the shared memory constructor
            let memory_wrapper = crate::memory::Memory::from_shared_memory(shared_memory);
            let validated_ptr = crate::memory::core::create_validated_memory(memory_wrapper)?;
            return Ok(validated_ptr as jlong);
        }

        // Neither regular nor shared memory found
        Ok(0)
    })
}

/// Get a shared memory export from the instance by name
///
/// Unlike nativeGetMemory, this ONLY returns shared memory exports (not regular memories).
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetSharedMemory(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    name: JString,
) -> jlong {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        match instance.get_shared_memory(store, &name_str)? {
            Some(shared_memory) => {
                let memory_wrapper = crate::memory::Memory::from_shared_memory(shared_memory);
                let validated_ptr = crate::memory::core::create_validated_memory(memory_wrapper)?;
                Ok(validated_ptr as jlong)
            }
            None => Ok(0),
        }
    })
}

/// Get a table export from the instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetTable(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    name: JString,
) -> jlong {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Get the table export
        let table_opt = instance.get_table(store, &name_str)?;

        match table_opt {
            Some(table) => {
                // Create a new Table wrapper and return its pointer
                let table_wrapper =
                    crate::table::Table::from_wasmtime_table(table, store, Some(name_str))?;
                Ok(Box::into_raw(Box::new(table_wrapper)) as jlong)
            }
            None => Ok(0), // Return 0 for not found
        }
    })
}

/// Get a tag export from the instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetTag(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    name: JString,
) -> jlong {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Get the tag export
        let tag_opt = instance.get_tag(store, &name_str)?;

        match tag_opt {
            Some(tag) => Ok(Box::into_raw(Box::new(tag)) as jlong),
            None => Ok(0), // Return 0 for not found
        }
    })
}

/// Check if an instance has a specific export
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeHasExport(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    name: JString,
) -> jboolean {
    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };

        // Check if the export exists by getting the list of export names
        let exports = core::get_all_exports(instance);
        let has_export = exports.contains_key(&name_str);
        Ok(if has_export { 1 } else { 0 })
    })
}

/// Destroy a native instance with double-free protection
///
/// Uses the consolidated `safe_destroy_no_fake_check` utility which provides
/// double-free protection and panic safety for JNI calls.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDestroyInstance(
    _env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
) {
    if instance_handle != 0 {
        use crate::ffi_common::resource_destruction::safe_destroy_no_fake_check;
        unsafe {
            let _ = safe_destroy_no_fake_check::<crate::instance::Instance>(
                instance_handle as *mut c_void,
                "JNI Instance",
            );
        }
    }
}

/// Dispose of a native instance with double-free protection
///
/// Uses the consolidated `safe_destroy_no_fake_check` utility which provides
/// double-free protection and panic safety for JNI calls.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDispose(
    _env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
) -> jboolean {
    if instance_handle != 0 {
        use crate::ffi_common::resource_destruction::safe_destroy_no_fake_check;
        let destroyed = unsafe {
            safe_destroy_no_fake_check::<crate::instance::Instance>(
                instance_handle as *mut c_void,
                "JNI Instance",
            )
        };
        if destroyed {
            1
        } else {
            0
        }
    } else {
        0
    }
}

/// Check if an instance has been disposed
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeIsDisposed(
    _env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
) -> jboolean {
    // If handle is 0, it's been disposed
    if instance_handle == 0 {
        1
    } else {
        0
    }
}

/// Get the creation timestamp of an instance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetCreatedAtMicros(
    _env: JNIEnv,
    _class: JClass,
    _instance_handle: jlong,
) -> jlong {
    // Return current time in microseconds as a placeholder
    use std::time::{SystemTime, UNIX_EPOCH};
    match SystemTime::now().duration_since(UNIX_EPOCH) {
        Ok(duration) => duration.as_micros() as jlong,
        Err(_) => 0,
    }
}

/// Get metadata export count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetMetadataExportCount(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };

        // Count the number of exports
        let exports = core::get_all_exports(instance);
        let count = exports.len() as jlong;
        Ok(count)
    })
}

/// Call an i32 function with i32 parameters
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCallI32Function(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    function_name: JString,
    params: jintArray,
) -> jint {
    use crate::instance::WasmValue;
    use jni::objects::JPrimitiveArray;

    let result = (|| -> WasmtimeResult<jint> {
        // Get the instance and store
        let instance = unsafe { core::get_instance_mut(instance_handle as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Convert function name
        let name_str: String = env
            .get_string(&function_name)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert function name: {}", e),
            })?
            .into();

        // Convert int array to WasmValue::I32 vector
        let params_array = unsafe { JPrimitiveArray::from_raw(params) };
        let param_count =
            env.get_array_length(&params_array)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get array length: {}", e),
                })?;

        let mut int_values = vec![0i32; param_count as usize];
        env.get_int_array_region(&params_array, 0, &mut int_values)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to get int array: {}", e),
            })?;

        let wasm_params: Vec<WasmValue> = int_values.into_iter().map(WasmValue::I32).collect();

        // Call the function
        let exec_result = instance.call_export_function(store, &name_str, &wasm_params)?;

        // Extract the first result as i32
        if exec_result.values.is_empty() {
            return Err(WasmtimeError::Function {
                message: "Function returned no values".to_string(),
            });
        }

        match exec_result.values[0] {
            WasmValue::I32(value) => Ok(value),
            _ => Err(WasmtimeError::Function {
                message: "Function did not return an i32 value".to_string(),
            }),
        }
    })();

    match result {
        Ok(value) => value,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            -1
        }
    }
}

/// Call an i32 function with no parameters
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCallI32FunctionNoParams(
    mut env: JNIEnv,
    _class: JClass,
    instance_handle: jlong,
    store_handle: jlong,
    function_name: JString,
) -> jint {
    use crate::instance::WasmValue;

    let result = (|| -> WasmtimeResult<jint> {
        // Get the instance and store
        let instance = unsafe { core::get_instance_mut(instance_handle as *mut c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Convert function name
        let name_str: String = env
            .get_string(&function_name)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert function name: {}", e),
            })?
            .into();

        // Call the function with no parameters
        let exec_result = instance.call_export_function(store, &name_str, &[])?;

        // Extract the first result as i32
        if exec_result.values.is_empty() {
            return Err(WasmtimeError::Function {
                message: "Function returned no values".to_string(),
            });
        }

        match exec_result.values[0] {
            WasmValue::I32(value) => Ok(value),
            _ => Err(WasmtimeError::Function {
                message: "Function did not return an i32 value".to_string(),
            }),
        }
    })();

    match result {
        Ok(value) => value,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            -1
        }
    }
}
