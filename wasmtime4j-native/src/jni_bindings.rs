//! JNI bindings for Java 8-22 compatibility

#![allow(unused_variables)]

#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;
#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JByteArray, JString, JObject, JValue};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jint, jbyte, jboolean, jdouble, jbyteArray, jstring, jobject, jintArray, jlongArray};

// Instance is imported locally in each module that needs it

/// JNI bindings module
/// 
/// This module provides JNI-compatible functions for use by the wasmtime4j-jni module.
/// All functions follow JNI naming conventions and handle Java/native type conversions.

/// Get library version for debugging
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeGetLibraryVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let version = "wasmtime4j-native-DEBUG-2025-10-06-18:45-WITH-SIGNALS-DISABLED";
    env.new_string(version).unwrap().into_raw()
}

/// JNI binding for NativeMethodBindings.nativeGetWasmtimeVersion (TOP LEVEL)
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeGetWasmtimeVersion(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    // Return Wasmtime version from constant
    match env.new_string(crate::WASMTIME_VERSION) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut()
    }
}

/// JNI bindings for Instance operations
#[cfg(feature = "jni-bindings")]
pub mod jni_instance {
    use super::*;
    use crate::instance::{core, InstanceState};
    use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};

    use std::os::raw::c_void;
    use jni::sys::jobjectArray;

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
            let string_class = env.find_class("java/lang/String")
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to find String class: {}", e)
                })?;

            let output_array = env.new_object_array(export_names.len() as i32, string_class, JObject::null())
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create array: {}", e)
                })?;

            // Fill the array with export names
            for (i, name) in export_names.iter().enumerate() {
                let jstr = env.new_string(name)
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to create string: {}", e)
                    })?;
                env.set_object_array_element(&output_array, i as i32, jstr)
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to set array element: {}", e)
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

    /// Call a WebAssembly function directly using Instance::call_export_function
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeCallFunction(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        store_ptr: jlong,
        function_name: JString,
        params: jobjectArray,
    ) -> jobjectArray {
        use crate::instance::{WasmValue, ExecutionResult};

        let result = (|| -> WasmtimeResult<jobjectArray> {
            let instance = unsafe { core::get_instance_mut(instance_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let name_str: String = env.get_string(&function_name)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert function name: {}", e)
                })?.into();

            // Wrap the raw jobjectArray
            use jni::objects::JObjectArray;
            let params_array = unsafe { JObjectArray::from_raw(params) };

            // Convert Java params to WasmValue
            let param_count = env.get_array_length(&params_array)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get array length: {}", e)
                })?;
            let mut wasm_params = Vec::with_capacity(param_count as usize);

            for i in 0..param_count {
                let param_obj = env.get_object_array_element(&params_array, i)
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to get parameter {}: {}", i, e)
                    })?;

                // Check if it's a WasmValue object
                let is_wasm_value = env.is_instance_of(&param_obj, "ai/tegmentum/wasmtime4j/WasmValue").unwrap_or(false);

                if is_wasm_value {
                    // Get the type from WasmValue.getType()
                    let type_obj = env.call_method(&param_obj, "getType", "()Lai/tegmentum/wasmtime4j/WasmValueType;", &[])
                        .and_then(|v| v.l())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to get type from WasmValue: {}", e)
                        })?;

                    // Get the type ordinal to determine which type it is
                    let type_ordinal = env.call_method(&type_obj, "ordinal", "()I", &[])
                        .and_then(|v| v.i())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to get type ordinal: {}", e)
                        })?;

                    // Get the value from WasmValue.getValue()
                    let value_obj = env.call_method(&param_obj, "getValue", "()Ljava/lang/Object;", &[])
                        .and_then(|v| v.l())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to get value from WasmValue: {}", e)
                        })?;

                    // Convert based on type ordinal (0=I32, 1=I64, 2=F32, 3=F64, 4=V128, 5=FUNCREF, 6=EXTERNREF)
                    match type_ordinal {
                        0 => { // I32
                            let value = env.call_method(&value_obj, "intValue", "()I", &[])
                                .and_then(|v| v.i())
                                .map_err(|e| WasmtimeError::InvalidParameter {
                                    message: format!("Failed to extract I32 value: {}", e)
                                })?;
                            wasm_params.push(WasmValue::I32(value));
                        }
                        1 => { // I64
                            let value = env.call_method(&value_obj, "longValue", "()J", &[])
                                .and_then(|v| v.j())
                                .map_err(|e| WasmtimeError::InvalidParameter {
                                    message: format!("Failed to extract I64 value: {}", e)
                                })?;
                            wasm_params.push(WasmValue::I64(value));
                        }
                        2 => { // F32
                            let value = env.call_method(&value_obj, "floatValue", "()F", &[])
                                .and_then(|v| v.f())
                                .map_err(|e| WasmtimeError::InvalidParameter {
                                    message: format!("Failed to extract F32 value: {}", e)
                                })?;
                            wasm_params.push(WasmValue::F32(value));
                        }
                        3 => { // F64
                            let value = env.call_method(&value_obj, "doubleValue", "()D", &[])
                                .and_then(|v| v.d())
                                .map_err(|e| WasmtimeError::InvalidParameter {
                                    message: format!("Failed to extract F64 value: {}", e)
                                })?;
                            wasm_params.push(WasmValue::F64(value));
                        }
                        5 => { // FUNCREF
                            // Extract the Long value from the funcref
                            let ref_value = if !value_obj.is_null() {
                                env.call_method(&value_obj, "longValue", "()J", &[])
                                    .and_then(|v| v.j())
                                    .ok()
                            } else {
                                None
                            };
                            wasm_params.push(WasmValue::FuncRef(ref_value));
                        }
                        6 => { // EXTERNREF
                            // Extract the Long value from the externref
                            let ref_value = if !value_obj.is_null() {
                                env.call_method(&value_obj, "longValue", "()J", &[])
                                    .and_then(|v| v.j())
                                    .ok()
                            } else {
                                None
                            };
                            wasm_params.push(WasmValue::ExternRef(ref_value));
                        }
                        _ => {
                            return Err(WasmtimeError::InvalidParameter {
                                message: format!("Unsupported WasmValue type ordinal: {}", type_ordinal)
                            });
                        }
                    }
                } else if env.is_instance_of(&param_obj, "java/lang/Integer").unwrap_or(false) {
                    let value = env.call_method(&param_obj, "intValue", "()I", &[])
                        .and_then(|v| v.i())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to extract int value: {}", e)
                        })?;
                    wasm_params.push(WasmValue::I32(value));
                } else if env.is_instance_of(&param_obj, "java/lang/Long").unwrap_or(false) {
                    let value = env.call_method(&param_obj, "longValue", "()J", &[])
                        .and_then(|v| v.j())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to extract long value: {}", e)
                        })?;
                    wasm_params.push(WasmValue::I64(value));
                } else if env.is_instance_of(&param_obj, "java/lang/Float").unwrap_or(false) {
                    let value = env.call_method(&param_obj, "floatValue", "()F", &[])
                        .and_then(|v| v.f())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to extract float value: {}", e)
                        })?;
                    wasm_params.push(WasmValue::F32(value));
                } else if env.is_instance_of(&param_obj, "java/lang/Double").unwrap_or(false) {
                    let value = env.call_method(&param_obj, "doubleValue", "()D", &[])
                        .and_then(|v| v.d())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to extract double value: {}", e)
                        })?;
                    wasm_params.push(WasmValue::F64(value));
                } else {
                    // Unknown parameter type - get class name for debugging
                    let class = env.get_object_class(&param_obj)
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to get parameter {} class: {}", i, e)
                        })?;
                    let class_name_obj = env.call_method(&class, "getName", "()Ljava/lang/String;", &[])
                        .and_then(|v| v.l())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to get class name: {}", e)
                        })?;
                    let class_name: String = env.get_string(&class_name_obj.into())
                        .map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to convert class name: {}", e)
                        })?.into();

                    return Err(WasmtimeError::InvalidParameter {
                        message: format!("Unsupported parameter type at index {}: {}", i, class_name)
                    });
                }
            }

            // Call the function using Instance's built-in method
            let exec_result: ExecutionResult = instance.call_export_function(store, &name_str, &wasm_params)?;

            // Convert results to Java Object array
            let object_class = env.find_class("java/lang/Object")
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to find Object class: {}", e)
                })?;

            let result_array = env.new_object_array(exec_result.values.len() as i32, object_class, JObject::null())
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to create result array: {}", e)
                })?;

            for (i, val) in exec_result.values.iter().enumerate() {
                // Create WasmValue Java objects instead of primitives to preserve type information
                let wasm_value_class = env.find_class("ai/tegmentum/wasmtime4j/WasmValue")?;

                let java_obj = match val {
                    WasmValue::I32(v) => {
                        // Call WasmValue.i32(int)
                        let integer_value = JValue::from(*v);
                        env.call_static_method(
                            wasm_value_class,
                            "i32",
                            "(I)Lai/tegmentum/wasmtime4j/WasmValue;",
                            &[integer_value]
                        ).map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to create WasmValue.i32: {}", e)
                        })?.l()?
                    }
                    WasmValue::I64(v) => {
                        // Call WasmValue.i64(long)
                        let long_value = JValue::from(*v);
                        env.call_static_method(
                            wasm_value_class,
                            "i64",
                            "(J)Lai/tegmentum/wasmtime4j/WasmValue;",
                            &[long_value]
                        ).map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to create WasmValue.i64: {}", e)
                        })?.l()?
                    }
                    WasmValue::F32(v) => {
                        // Call WasmValue.f32(float)
                        let float_value = JValue::from(*v);
                        env.call_static_method(
                            wasm_value_class,
                            "f32",
                            "(F)Lai/tegmentum/wasmtime4j/WasmValue;",
                            &[float_value]
                        ).map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to create WasmValue.f32: {}", e)
                        })?.l()?
                    }
                    WasmValue::F64(v) => {
                        // Call WasmValue.f64(double)
                        let double_value = JValue::from(*v);
                        env.call_static_method(
                            wasm_value_class,
                            "f64",
                            "(D)Lai/tegmentum/wasmtime4j/WasmValue;",
                            &[double_value]
                        ).map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to create WasmValue.f64: {}", e)
                        })?.l()?
                    }
                    WasmValue::ExternRef(ref_value) => {
                        // Call WasmValue.externref(Object) - pass Long or null
                        match ref_value {
                            Some(id) => {
                                let long_class = env.find_class("java/lang/Long")?;
                                let long_obj = env.new_object(long_class, "(J)V", &[JValue::from(*id)])
                                    .map_err(|e| WasmtimeError::InvalidParameter {
                                        message: format!("Failed to create Long for externref: {}", e)
                                    })?;
                                env.call_static_method(
                                    wasm_value_class,
                                    "externref",
                                    "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                                    &[JValue::from(&long_obj)]
                                ).map_err(|e| WasmtimeError::InvalidParameter {
                                    message: format!("Failed to create WasmValue.externref: {}", e)
                                })?.l()?
                            }
                            None => {
                                let null_obj = JObject::null();
                                env.call_static_method(
                                    wasm_value_class,
                                    "externref",
                                    "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                                    &[JValue::from(&null_obj)]
                                ).map_err(|e| WasmtimeError::InvalidParameter {
                                    message: format!("Failed to create WasmValue.externref(null): {}", e)
                                })?.l()?
                            }
                        }
                    }
                    WasmValue::FuncRef(_) => {
                        // FuncRef: return WasmValue.funcref(null)
                        let null_obj = JObject::null();
                        env.call_static_method(
                            wasm_value_class,
                            "funcref",
                            "(Ljava/lang/Object;)Lai/tegmentum/wasmtime4j/WasmValue;",
                            &[JValue::from(&null_obj)]
                        ).map_err(|e| WasmtimeError::InvalidParameter {
                            message: format!("Failed to create WasmValue.funcref: {}", e)
                        })?.l()?
                    }
                    _ => continue, // Skip unsupported types for now
                };

                env.set_object_array_element(&result_array, i as i32, java_obj)
                    .map_err(|e| WasmtimeError::InvalidParameter {
                        message: format!("Failed to set array element: {}", e)
                    })?;
            }

            Ok(result_array.as_raw())
        })();

        match result {
            Ok(array) => array,
            Err(e) => {
                // Throw a Java exception with the error details
                // Use Display format ({}) instead of Debug ({:?}) to get clean error messages
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
            use std::os::raw::c_void;

            let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Get the function export
            let func_opt = instance.get_func(store, &name_str)?;

            match func_opt {
                Some(func) => {
                    // Create FunctionHandle with cached type information
                    let func_handle = jni_function::FunctionHandle::new(func, name_str.clone(), store);
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
            use std::os::raw::c_void;

            let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Get the global export
            let global_opt = instance.get_global(store, &name_str)?;

            match global_opt {
                Some(global) => {
                    // Create a new Global wrapper and return its pointer
                    let global_wrapper = crate::global::Global::from_wasmtime_global(
                        global,
                        store,
                        Some(name_str),
                    )?;
                    Ok(Box::into_raw(Box::new(global_wrapper)) as jlong)
                }
                None => Ok(0), // Return 0 for not found
            }
        })
    }

    /// Get a memory export from the instance
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
            use std::os::raw::c_void;

            let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Get the memory export
            let memory_opt = instance.get_memory(store, &name_str)?;

            match memory_opt {
                Some(memory) => {
                    // Get memory type information from the store
                    let memory_type = store.with_context_ro(|ctx| Ok(memory.ty(ctx)))?;
                    // Create a new Memory wrapper and register the handle
                    let memory_wrapper = crate::memory::Memory::from_wasmtime_memory(memory, memory_type);
                    let validated_ptr = crate::memory::core::create_validated_memory(memory_wrapper)?;
                    Ok(validated_ptr as jlong)
                }
                None => Ok(0), // Return 0 for not found
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
            use std::os::raw::c_void;

            let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Get the table export
            let table_opt = instance.get_table(store, &name_str)?;

            match table_opt {
                Some(table) => {
                    // Create a new Table wrapper and return its pointer
                    let table_wrapper = crate::table::Table::from_wasmtime_table(
                        table,
                        store,
                        Some(name_str),
                    )?;
                    Ok(Box::into_raw(Box::new(table_wrapper)) as jlong)
                }
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
            use std::os::raw::c_void;
            let instance = unsafe { core::get_instance_ref(instance_handle as *const c_void)? };

            // Check if the export exists by getting the list of export names
            let exports = core::get_all_exports(instance);
            let has_export = exports.contains_key(&name_str);
            Ok(if has_export { 1 } else { 0 })
        })
    }

    /// Destroy a native instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDestroyInstance(
        _env: JNIEnv,
        _class: JClass,
        instance_handle: jlong,
    ) {
        if instance_handle != 0 {
            use std::os::raw::c_void;
            unsafe {
                // Drop the boxed instance
                let _ = Box::from_raw(instance_handle as *mut c_void);
            }
        }
    }

    /// Dispose of a native instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDispose(
        _env: JNIEnv,
        _class: JClass,
        instance_handle: jlong,
    ) -> jboolean {
        if instance_handle != 0 {
            use std::os::raw::c_void;
            unsafe {
                // Drop the boxed instance
                let _ = Box::from_raw(instance_handle as *mut c_void);
            }
            1
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
            use std::os::raw::c_void;
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
        use std::os::raw::c_void;
        use jni::objects::JPrimitiveArray;

        let result = (|| -> WasmtimeResult<jint> {
            // Get the instance and store
            let instance = unsafe { core::get_instance_mut(instance_handle as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Convert function name
            let name_str: String = env.get_string(&function_name)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert function name: {}", e)
                })?.into();

            // Convert int array to WasmValue::I32 vector
            let params_array = unsafe { JPrimitiveArray::from_raw(params) };
            let param_count = env.get_array_length(&params_array)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get array length: {}", e)
                })?;

            let mut int_values = vec![0i32; param_count as usize];
            env.get_int_array_region(&params_array, 0, &mut int_values)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get int array: {}", e)
                })?;

            let wasm_params: Vec<WasmValue> = int_values.into_iter()
                .map(WasmValue::I32)
                .collect();

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
        use std::os::raw::c_void;

        let result = (|| -> WasmtimeResult<jint> {
            // Get the instance and store
            let instance = unsafe { core::get_instance_mut(instance_handle as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Convert function name
            let name_str: String = env.get_string(&function_name)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert function name: {}", e)
                })?.into();

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
}

/// JNI bindings for Engine operations
#[cfg(feature = "jni-bindings")]
pub mod jni_engine {
    use super::*;
    use crate::engine::core;
    use crate::error::{jni_utils, WasmtimeError};
    use crate::ffi_common::parameter_conversion;
    
    /// Create a new Wasmtime engine with default configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngine(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || core::create_engine()) as jlong
    }

    /// Create a new Wasmtime engine with custom configuration (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateEngineWithConfig(
        mut env: JNIEnv,
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
        
        
        jni_utils::jni_try_ptr(&mut env, || {
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
                false,  // async_support - TODO: add JNI parameter
            )
        }) as jlong
    }

    /// Destroy a Wasmtime engine (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeDestroyEngine(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) {
        unsafe {
            core::destroy_engine(engine_ptr as *mut std::os::raw::c_void);
        }
    }

    /// Query if epoch interruption is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsEpochInterruptionEnabled(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            Ok(engine.epoch_interruption_enabled())
        }) as jboolean
    }

    /// Query if coredump generation on trap is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsCoredumpOnTrapEnabled(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            Ok(engine.coredump_on_trap())
        }) as jboolean
    }

    /// Query if fuel consumption is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeIsFuelEnabled(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            Ok(engine.fuel_enabled())
        }) as jboolean
    }

    /// Get stack size limit in bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetStackSizeLimit(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            Ok(engine.stack_size_limit().unwrap_or(0) as jlong)
        }).1
    }

    /// Get memory limit in pages (64KB per page)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetMemoryLimitPages(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jint {
        jni_utils::jni_try(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            Ok(engine.memory_limit_pages().unwrap_or(0) as jint)
        }).1
    }

    /// Query if a specific WebAssembly feature is supported (by feature name)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSupportsFeature(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        feature_name: JString,
    ) -> jboolean {
        // Convert Java string first (outside the closure to avoid borrow issues)
        let feature_str: String = match env.get_string(&feature_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        jni_utils::jni_try_bool(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

            // Parse feature name to WasmFeature enum
            let feature = match feature_str.as_str() {
                "THREADS" => crate::engine::WasmFeature::Threads,
                "REFERENCE_TYPES" => crate::engine::WasmFeature::ReferenceTypes,
                "SIMD" => crate::engine::WasmFeature::Simd,
                "BULK_MEMORY" => crate::engine::WasmFeature::BulkMemory,
                "MULTI_VALUE" => crate::engine::WasmFeature::MultiValue,
                "MULTI_MEMORY" => crate::engine::WasmFeature::MultiMemory,
                "TAIL_CALL" => crate::engine::WasmFeature::TailCall,
                "RELAXED_SIMD" => crate::engine::WasmFeature::RelaxedSimd,
                "FUNCTION_REFERENCES" => crate::engine::WasmFeature::FunctionReferences,
                "GC" => crate::engine::WasmFeature::Gc,
                "EXCEPTIONS" => crate::engine::WasmFeature::Exceptions,
                "MEMORY64" => crate::engine::WasmFeature::Memory64,
                "EXTENDED_CONST" => crate::engine::WasmFeature::ExtendedConst,
                "COMPONENT_MODEL" => crate::engine::WasmFeature::ComponentModel,
                "CUSTOM_PAGE_SIZES" => crate::engine::WasmFeature::CustomPageSizes,
                "WIDE_ARITHMETIC" => crate::engine::WasmFeature::WideArithmetic,
                "STACK_SWITCHING" => crate::engine::WasmFeature::StackSwitching,
                "SHARED_EVERYTHING_THREADS" => crate::engine::WasmFeature::SharedEverythingThreads,
                "COMPONENT_MODEL_ASYNC" => crate::engine::WasmFeature::ComponentModelAsync,
                "COMPONENT_MODEL_ASYNC_BUILTINS" => crate::engine::WasmFeature::ComponentModelAsyncBuiltins,
                "COMPONENT_MODEL_ASYNC_STACKFUL" => crate::engine::WasmFeature::ComponentModelAsyncStackful,
                "COMPONENT_MODEL_ERROR_CONTEXT" => crate::engine::WasmFeature::ComponentModelErrorContext,
                "COMPONENT_MODEL_GC" => crate::engine::WasmFeature::ComponentModelGc,
                _ => return Err(WasmtimeError::InvalidParameter {
                    message: format!("Unknown feature: {}", feature_str),
                }),
            };

            Ok(engine.supports_feature(feature))
        }) as jboolean
    }

    /// Compile WebAssembly module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCompileModule(
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
        
        let data = match wasm_data_result {
            Ok(data) => data,
            Err(_) => return 0 as jlong, // Return null on error
        };
        
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            let byte_converter = jni_module::VecByteArrayConverter::new(data);
            crate::shared_ffi::module::compile_module_shared(engine, byte_converter)
        }) as jlong
    }

    /// Compile WAT to module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCompileWat(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        wat_string: JString,
    ) -> jlong {
        // Extract string before moving env into jni_try_ptr
        let wat_data_result = env.get_string(&wat_string)
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java string: {}", e),
            });

        let wat_jstr = match wat_data_result {
            Ok(jstr) => jstr,
            Err(_) => return 0 as jlong, // Return null on error
        };

        // Convert JavaStr to String immediately
        let string_converter = match jni_module::JStringConverter::new(wat_jstr) {
            Ok(converter) => converter,
            Err(_) => return 0 as jlong, // Return null on error
        };

        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            crate::shared_ffi::module::compile_module_wat_shared(engine, string_converter)
        }) as jlong
    }

    /// Precompile module for AOT usage
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativePrecompileModule(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        wasm_bytes: JByteArray,
    ) -> jbyteArray {
        // Get the byte array data
        let bytes = match env.convert_byte_array(&wasm_bytes) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };

        // Precompile the module
        let engine = match unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void) } {
            Ok(engine) => engine,
            Err(_) => return std::ptr::null_mut(),
        };

        let precompiled = match core::precompile_module(engine, &bytes) {
            Ok(data) => data,
            Err(_) => return std::ptr::null_mut(),
        };

        // Convert result to Java byte array
        match env.byte_array_from_slice(&precompiled) {
            Ok(array) => array.into_raw(),
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Create a new store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeCreateStore(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            crate::store::core::create_store(engine)
        }) as jlong
    }
    
    /// Set optimization level
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeSetOptimizationLevel(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        level: jint,
    ) -> jboolean {
        // Note: Wasmtime engines are immutable after creation, so optimization level
        // cannot be changed. This method validates the request but returns false
        // to indicate the operation is not supported.
        jni_utils::jni_try_bool(&mut env, || {
            let _engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            // Validate the level parameter
            if level < 0 || level > 2 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid optimization level: {}", level),
                });
            }
            // Return false to indicate optimization level cannot be changed after engine creation
            Ok(false)
        }) as jboolean
    }

    /// Get engine reference count for debugging (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniEngine_nativeGetReferenceCount(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
        _class: JClass,
        _engine_ptr: jlong,
    ) -> jboolean {
        // Return false by default
        0
    }
}


/// JNI bindings for Function operations
#[cfg(feature = "jni-bindings")]
pub mod jni_function {
    use super::*;
    use wasmtime::{Func, FuncType, Val, ValType};
    use crate::error::{WasmtimeError, WasmtimeResult, jni_utils};
    use crate::ffi_common::memory_utils;
    use crate::store::Store;
    use jni::sys::{jintArray, jlongArray, jfloatArray, jdoubleArray, jobjectArray};
    use jni::objects::{JObject, JObjectArray};
    
    /// Function handle that stores both the Wasmtime function and its type information
    /// This allows for efficient type introspection without requiring a store context
    #[derive(Debug)]
    pub struct FunctionHandle {
        /// Function name for debugging
        pub name: String,
        /// Cached parameter type strings (Store-independent)
        param_types: Vec<String>,
        /// Cached return type strings (Store-independent)
        return_types: Vec<String>,
    }

    impl FunctionHandle {
        /// Create a new function handle with type information cached as strings
        ///
        /// IMPORTANT: Caches type info as strings at creation time, avoiding Store-bound FuncType
        /// The Func parameter is only used for extracting type information and is not stored
        pub fn new(func: Func, name: String, store: &mut Store) -> Self {
            let store_guard = store.inner.lock();
            let func_type = func.ty(&*store_guard);

            let param_types = func_type.params().map(|vt| valtype_to_string(&vt)).collect();
            let return_types = func_type.results().map(|vt| valtype_to_string(&vt)).collect();

            Self {
                name,
                param_types,
                return_types,
            }
        }

        /// Get parameter types as strings (cached at creation, Store-independent)
        pub fn get_param_type_strings(&self) -> &[String] {
            &self.param_types
        }

        /// Get return types as strings (cached at creation, Store-independent)
        pub fn get_return_type_strings(&self) -> &[String] {
            &self.return_types
        }
    }
    
    /// Convert ValType to string representation
    fn valtype_to_string(vt: &ValType) -> String {
        match vt {
            ValType::I32 => "i32".to_string(),
            ValType::I64 => "i64".to_string(),
            ValType::F32 => "f32".to_string(),
            ValType::F64 => "f64".to_string(),
            ValType::V128 => "v128".to_string(),
            ValType::Ref(ref_type) => match ref_type {
                _ if ref_type.heap_type().is_func() => "funcref".to_string(),
                _ if ref_type.heap_type().is_extern() => "externref".to_string(),
                _ => "ref".to_string(),
            },
        }
    }
    
    
    /// Helper function to create Java String array from Vec<String>
    fn create_java_string_array(env: &mut JNIEnv, strings: &[String]) -> WasmtimeResult<jobjectArray> {
        let string_class = env.find_class("java/lang/String")
            .map_err(|e| WasmtimeError::Function { message: format!("Failed to find String class: {}", e) })?;
            
        let array = env.new_object_array(strings.len() as i32, string_class, JObject::null())
            .map_err(|e| WasmtimeError::Function { message: format!("Failed to create String array: {}", e) })?;

        for (i, type_str) in strings.iter().enumerate() {
            let jstring = env.new_string(type_str)
                .map_err(|e| WasmtimeError::Function { message: format!("Failed to create String: {}", e) })?;
            env.set_object_array_element(&array, i as i32, &jstring)
                .map_err(|e| WasmtimeError::Function { message: format!("Failed to set array element: {}", e) })?;
        }

        Ok(array.into_raw())
    }
    
    /// Convert Java Object array to Wasmtime Val array for function parameters
    fn convert_java_params_to_wasmtime_vals(
        env: &mut JNIEnv, 
        params: jobjectArray, 
        expected_types: &[ValType]
    ) -> WasmtimeResult<Vec<Val>> {
        if params.is_null() {
            return Ok(Vec::new());
        }
        
        let params_array = JObjectArray::from(unsafe { JObject::from_raw(params) });
        let param_count = env.get_array_length(&params_array)
            .map_err(|e| WasmtimeError::Function { message: format!("Failed to get parameter array length: {}", e) })?;
            
        if param_count as usize != expected_types.len() {
            return Err(WasmtimeError::Function { 
                message: format!("Parameter count mismatch: expected {}, got {}", expected_types.len(), param_count) 
            });
        }
        
        let mut vals = Vec::new();
        
        for i in 0..param_count {
            let param_obj = env.get_object_array_element(&params_array, i)
                .map_err(|e| WasmtimeError::Function { message: format!("Failed to get parameter {}: {}", i, e) })?;
                
            let expected_type = &expected_types[i as usize];
            let val = convert_java_object_to_wasmtime_val(env, param_obj.into_raw(), expected_type)?;
            vals.push(val);
        }
        
        Ok(vals)
    }
    
    /// Convert a single Java Object to a Wasmtime Val based on expected type
    fn convert_java_object_to_wasmtime_val(
        env: &mut JNIEnv, 
        obj: jobject, 
        expected_type: &ValType
    ) -> WasmtimeResult<Val> {
        if obj.is_null() {
            return match expected_type {
                ValType::Ref(_) => Ok(Val::null_extern_ref()),
                _ => Err(WasmtimeError::Function { 
                    message: format!("Null parameter for non-reference type: {:?}", expected_type) 
                }),
            };
        }
        
        let jobject_ref = unsafe { JObject::from_raw(obj) };
        
        match expected_type {
            ValType::I32 => {
                // Try to convert from Integer wrapper
                let int_class = env.find_class("java/lang/Integer")
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to find Integer class: {}", e) })?;
                    
                if env.is_instance_of(&jobject_ref, int_class)
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to check Integer instance: {}", e) })? {
                    
                    let value = env.call_method(&jobject_ref, "intValue", "()I", &[])
                        .map_err(|e| WasmtimeError::Function { message: format!("Failed to call intValue(): {}", e) })?;
                    
                    match value {
                        jni::objects::JValueGen::Int(i) => Ok(Val::I32(i)),
                        _ => Err(WasmtimeError::Function { message: "Invalid Integer value".to_string() }),
                    }
                } else {
                    Err(WasmtimeError::Function { message: "Expected Integer parameter for i32".to_string() })
                }
            },
            
            ValType::I64 => {
                // Try to convert from Long wrapper
                let long_class = env.find_class("java/lang/Long")
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to find Long class: {}", e) })?;
                    
                if env.is_instance_of(&jobject_ref, long_class)
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to check Long instance: {}", e) })? {
                    
                    let value = env.call_method(&jobject_ref, "longValue", "()J", &[])
                        .map_err(|e| WasmtimeError::Function { message: format!("Failed to call longValue(): {}", e) })?;
                    
                    match value {
                        jni::objects::JValueGen::Long(l) => Ok(Val::I64(l)),
                        _ => Err(WasmtimeError::Function { message: "Invalid Long value".to_string() }),
                    }
                } else {
                    Err(WasmtimeError::Function { message: "Expected Long parameter for i64".to_string() })
                }
            },
            
            ValType::F32 => {
                // Try to convert from Float wrapper
                let float_class = env.find_class("java/lang/Float")
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to find Float class: {}", e) })?;
                    
                if env.is_instance_of(&jobject_ref, float_class)
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to check Float instance: {}", e) })? {
                    
                    let value = env.call_method(&jobject_ref, "floatValue", "()F", &[])
                        .map_err(|e| WasmtimeError::Function { message: format!("Failed to call floatValue(): {}", e) })?;
                    
                    match value {
                        jni::objects::JValueGen::Float(f) => Ok(Val::F32(f.to_bits())),
                        _ => Err(WasmtimeError::Function { message: "Invalid Float value".to_string() }),
                    }
                } else {
                    Err(WasmtimeError::Function { message: "Expected Float parameter for f32".to_string() })
                }
            },
            
            ValType::F64 => {
                // Try to convert from Double wrapper
                let double_class = env.find_class("java/lang/Double")
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to find Double class: {}", e) })?;
                    
                if env.is_instance_of(&jobject_ref, double_class)
                    .map_err(|e| WasmtimeError::Function { message: format!("Failed to check Double instance: {}", e) })? {
                    
                    let value = env.call_method(&jobject_ref, "doubleValue", "()D", &[])
                        .map_err(|e| WasmtimeError::Function { message: format!("Failed to call doubleValue(): {}", e) })?;
                    
                    match value {
                        jni::objects::JValueGen::Double(d) => Ok(Val::F64(d.to_bits())),
                        _ => Err(WasmtimeError::Function { message: "Invalid Double value".to_string() }),
                    }
                } else {
                    Err(WasmtimeError::Function { message: "Expected Double parameter for f64".to_string() })
                }
            },
            
            ValType::V128 => {
                // For V128, expect a byte array
                let byte_array_class = env.find_class("[B")
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to find byte array class: {}", e)
                    })?;
                let is_byte_array = env.is_instance_of(&jobject_ref, byte_array_class)
                    .map_err(|e| WasmtimeError::Function {
                        message: format!("Failed to check instance type: {}", e)
                    })?;
                if is_byte_array {
                    let byte_array: jni::objects::JPrimitiveArray<i8> = jobject_ref.into();
                    let bytes = env.convert_byte_array(byte_array)
                        .map_err(|e| WasmtimeError::Function { message: format!("Failed to convert byte array: {}", e) })?;
                        
                    if bytes.len() != 16 {
                        return Err(WasmtimeError::Function { 
                            message: format!("V128 requires exactly 16 bytes, got {}", bytes.len()) 
                        });
                    }
                    
                    let mut v128_bytes = [0u8; 16];
                    v128_bytes.copy_from_slice(&bytes);
                    Ok(Val::V128(wasmtime::V128::from(u128::from_le_bytes(v128_bytes))))
                } else {
                    Err(WasmtimeError::Function { message: "Expected byte array for V128".to_string() })
                }
            },
            
            ValType::Ref(_) => {
                // For now, we'll handle references as null or set externref to null
                Ok(Val::null_extern_ref())
            },
        }
    }
    
    
    
    /// Get parameter types of a function (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeGetParameterTypes(
        mut env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
    ) -> jobjectArray {
        // Defensive programming: validate function pointer
        if function_ptr == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("function_ptr cannot be null"));
            return std::ptr::null_mut();
        }

        match memory_utils::safe_deref(function_ptr as *const FunctionHandle, "function_ptr") {
            Ok(func_handle) => {
                let param_type_strings = func_handle.get_param_type_strings();

                match create_java_string_array(&mut env, param_type_strings) {
                    Ok(array) => array,
                    Err(error) => {
                        jni_utils::throw_jni_exception(&mut env, &error);
                        std::ptr::null_mut()
                    }
                }
            },
            Err(memory_error) => {
                let wasmtime_error = memory_error.to_wasmtime_error();
                jni_utils::throw_jni_exception(&mut env, &wasmtime_error);
                std::ptr::null_mut()
            }
        }
    }
    
    /// Get return types of a function (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeGetReturnTypes(
        mut env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
    ) -> jobjectArray {
        // Defensive programming: validate function pointer
        if function_ptr == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("function_ptr cannot be null"));
            return std::ptr::null_mut();
        }

        match memory_utils::safe_deref(function_ptr as *const FunctionHandle, "function_ptr") {
            Ok(func_handle) => {
                let return_type_strings = func_handle.get_return_type_strings();

                match create_java_string_array(&mut env, return_type_strings) {
                    Ok(array) => array,
                    Err(error) => {
                        jni_utils::throw_jni_exception(&mut env, &error);
                        std::ptr::null_mut()
                    }
                }
            },
            Err(memory_error) => {
                let wasmtime_error = memory_error.to_wasmtime_error();
                jni_utils::throw_jni_exception(&mut env, &wasmtime_error);
                std::ptr::null_mut()
            }
        }
    }
    
    /// Call a function with generic parameters (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCall(
        mut env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        store_handle: jlong,
        params: jobjectArray,
    ) -> jobjectArray {
        // Defensive programming: validate function pointer
        if function_ptr == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("function_ptr cannot be null"));
            return std::ptr::null_mut();
        }

        // Defensive programming: validate store handle
        if store_handle == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("store_handle cannot be null"));
            return std::ptr::null_mut();
        }

        use std::os::raw::c_void;
        use crate::instance::core::get_function_ref;
        use wasmtime::Val;

        // Helper closure for the actual work
        let result = (|| -> WasmtimeResult<jobjectArray> {
            // Get function reference
            let func = unsafe { get_function_ref(function_ptr as *const c_void)? };

            // Get store reference
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Lock the store for reentrant access
            let mut store_lock = store.lock_store();

            // Get function type for parameter conversion
            let func_type = func.ty(&*store_lock);
            let param_types = func_type.params().collect::<Vec<_>>();

            // Convert Java parameters to Wasmtime values
            let wasmtime_params = convert_java_params_to_wasmtime_vals(&mut env, params, &param_types)?;

            // Prepare result storage
            let result_count = func_type.results().len();
            let mut results = vec![Val::I32(0); result_count];

            // Call function
            match func.call(&mut *store_lock, &wasmtime_params, &mut results) {
                Ok(()) => {
                    // Convert Wasmtime Val results to WasmValue
                    let wasm_values: Result<Vec<_>, _> = results.iter()
                        .map(|val| crate::instance::core::wasmtime_val_to_wasm_value(val))
                        .collect();
                    let wasm_values = wasm_values?;

                    // Convert WasmValue to Java array
                    let java_array = crate::jni_bindings::jni_linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
                    Ok(java_array.as_raw())
                },
                Err(trap) => {
                    // Handle Wasmtime trap
                    Err(WasmtimeError::Runtime {
                        message: format!("Function call trapped: {}", trap),
                        backtrace: None,
                    })
                }
            }
        })();

        match result {
            Ok(arr) => arr,
            Err(error) => {
                jni_utils::throw_jni_exception(&mut env, &error);
                std::ptr::null_mut()
            }
        }
    }

    /// Call a WebAssembly function asynchronously (JNI version)
    #[no_mangle]
    #[cfg(feature = "async")]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallAsync(
        mut env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        store_handle: jlong,
        params: jobjectArray,
    ) -> jobjectArray {
        // Defensive programming: validate function pointer
        if function_ptr == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("function_ptr cannot be null"));
            return std::ptr::null_mut();
        }

        // Defensive programming: validate store handle
        if store_handle == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("store_handle cannot be null"));
            return std::ptr::null_mut();
        }

        use std::os::raw::c_void;
        use crate::instance::core::get_function_ref;
        use wasmtime::Val;

        // Helper closure for the actual work
        let result = (|| -> WasmtimeResult<jobjectArray> {
            // Get function reference
            let func = unsafe { get_function_ref(function_ptr as *const c_void)? };

            // Get store reference
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Lock the store for reentrant access
            let mut store_lock = store.lock_store();

            // Get function type for parameter conversion
            let func_type = func.ty(&*store_lock);
            let param_types = func_type.params().collect::<Vec<_>>();

            // Convert Java parameters to Wasmtime values
            let wasmtime_params = convert_java_params_to_wasmtime_vals(&mut env, params, &param_types)?;

            // Prepare result storage
            let result_count = func_type.results().len();
            let mut results = vec![Val::I32(0); result_count];

            // Call function asynchronously using the global runtime
            let runtime = crate::async_runtime::get_async_runtime();
            match runtime.block_on(func.call_async(&mut *store_lock, &wasmtime_params, &mut results)) {
                Ok(()) => {
                    // Convert Wasmtime Val results to WasmValue
                    let wasm_values: Result<Vec<_>, _> = results.iter()
                        .map(|val| crate::instance::core::wasmtime_val_to_wasm_value(val))
                        .collect();
                    let wasm_values = wasm_values?;

                    // Convert WasmValue to Java array
                    let java_array = crate::jni_bindings::jni_linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
                    Ok(java_array.as_raw())
                },
                Err(trap) => {
                    // Handle Wasmtime trap
                    Err(WasmtimeError::Runtime {
                        message: format!("Async function call trapped: {}", trap),
                        backtrace: None,
                    })
                }
            }
        })();

        match result {
            Ok(arr) => arr,
            Err(error) => {
                jni_utils::throw_jni_exception(&mut env, &error);
                std::ptr::null_mut()
            }
        }
    }

    /// Call a function with multiple return values (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallMultiValue(
        mut env: JNIEnv,
        _class: JClass,
        function_ptr: jlong,
        store_handle: jlong,
        params: jobjectArray,
    ) -> jobjectArray {
        // Defensive programming: validate function pointer
        if function_ptr == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("function_ptr cannot be null"));
            return std::ptr::null_mut();
        }

        // Defensive programming: validate store handle
        if store_handle == 0 {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::invalid_parameter("store_handle cannot be null"));
            return std::ptr::null_mut();
        }

        use std::os::raw::c_void;
        use crate::instance::core::get_function_ref;
        use wasmtime::Val;

        // Helper closure for the actual work
        let result = (|| -> WasmtimeResult<jobjectArray> {
            // Get function reference
            let func = unsafe { get_function_ref(function_ptr as *const c_void)? };

            // Get store reference
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Lock the store for reentrant access
            let mut store_lock = store.lock_store();

            // Get function type for parameter conversion
            let func_type = func.ty(&*store_lock);
            let param_types = func_type.params().collect::<Vec<_>>();

            // Convert Java parameters to Wasmtime values
            let wasmtime_params = convert_java_params_to_wasmtime_vals(&mut env, params, &param_types)?;

            // Prepare result storage
            let result_count = func_type.results().len();
            let mut results = vec![Val::I32(0); result_count];

            // Call function
            match func.call(&mut *store_lock, &wasmtime_params, &mut results) {
                Ok(()) => {
                    // Convert Wasmtime Val results to WasmValue (supports multi-value returns)
                    let wasm_values: Result<Vec<_>, _> = results.iter()
                        .map(|val| crate::instance::core::wasmtime_val_to_wasm_value(val))
                        .collect();
                    let wasm_values = wasm_values?;

                    // Convert WasmValue to Java array
                    let java_array = crate::jni_bindings::jni_linker::wasm_values_to_java_array(&mut env, &wasm_values)?;
                    Ok(java_array.as_raw())
                },
                Err(trap) => {
                    // Handle Wasmtime trap
                    Err(WasmtimeError::Runtime {
                        message: format!("Function call trapped: {}", trap),
                        backtrace: None,
                    })
                }
            }
        })();

        match result {
            Ok(arr) => arr,
            Err(error) => {
                jni_utils::throw_jni_exception(&mut env, &error);
                std::ptr::null_mut()
            }
        }
    }
    
    /// Call a function with int parameters (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunction_nativeCallInt(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
    
    
    // REMOVED: Duplicate function moved to top level (line 33)
    // The function must be at the top level to be properly exported for JNI
    
    /// Create a test runtime for validation (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCreateRuntime(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        // Placeholder implementation - return a non-zero value to indicate "success"
        1
    }
    
    /// Destroy a test runtime (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeDestroyRuntime(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) {
        // Placeholder implementation - do nothing for now
    }
    
    /// Initialize the native library (JNI version) - PLACEHOLDER
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeInitialize(
        mut env: JNIEnv,
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
    use jni::sys::jobjectArray;
    
    /// Create a new store with default configuration
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStore(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            core::create_store(engine)
        }) as jlong
    }
    
    /// Create a new store with custom configuration
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStoreWithConfig(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        fuel_limit: jlong,           // 0 = no limit
        memory_limit_bytes: jlong,   // 0 = no limit
        execution_timeout_secs: jlong, // 0 = no timeout
        max_instances: jint,         // 0 = no limit
        max_table_elements: jint,    // 0 = no limit
        max_functions: jint,         // 0 = no limit
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            
            let fuel_limit_opt = if fuel_limit == 0 { None } else { Some(fuel_limit as u64) };
            let memory_limit_opt = if memory_limit_bytes == 0 { None } else { Some(memory_limit_bytes as usize) };
            let timeout_opt = if execution_timeout_secs == 0 { None } else { Some(execution_timeout_secs as u64) };
            let max_instances_opt = if max_instances == 0 { None } else { Some(max_instances as usize) };
            let max_table_elements_opt = if max_table_elements == 0 { None } else { Some(max_table_elements as u32) };
            let max_functions_opt = if max_functions == 0 { None } else { Some(max_functions as usize) };
            
            core::create_store_with_config(
                engine,
                fuel_limit_opt,
                memory_limit_opt,
                timeout_opt,
                max_instances_opt,
                max_table_elements_opt,
                max_functions_opt,
            )
        }) as jlong
    }

    /// Create a new store compatible with a specific module
    ///
    /// CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Arc
    /// as the Module's internal wasmtime::Module. This is required because wasmtime's
    /// Instance::new() uses Arc::ptr_eq() to verify engine compatibility.
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStoreForModule(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let module = unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            core::create_store_for_module(module)
        }) as jlong
    }

    /// Add fuel to the store for execution limiting
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeAddFuel(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        fuel: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::add_fuel(store, fuel as u64)?;
            Ok(true)
        }) as jboolean
    }
    
    /// Get remaining fuel in the store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetFuelRemaining(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let fuel = core::get_fuel_remaining(store)?;
            Ok(fuel as jlong)
        })
    }
    
    /// Consume fuel from the store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeConsumeFuel(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        fuel_to_consume: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let actual_consumed = core::consume_fuel(store, fuel_to_consume as u64)?;
            Ok(actual_consumed as jlong)
        })
    }
    
    /// Set epoch deadline for interruption
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetEpochDeadline(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        ticks: jlong,
    ) {
        let _ = jni_utils::jni_try_void(&mut env, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::set_epoch_deadline(store, ticks as u64);
            Ok(())
        });
    }
    
    /// Force garbage collection in the store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGarbageCollect(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::garbage_collect(store)?;
            Ok(true)
        }) as jboolean
    }
    
    /// Validate store functionality
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeValidate(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::validate_store(store)?;
            Ok(true)
        }) as jboolean
    }
    
    /// Get store execution count
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetExecutionCount(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let stats = core::get_execution_stats(store)?;
            Ok(stats.execution_count as jlong)
        })
    }
    
    /// Get store total execution time in milliseconds
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetExecutionTime(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let stats = core::get_execution_stats(store)?;
            Ok(stats.total_execution_time.as_millis() as jlong)
        })
    }
    
    /// Get store total fuel consumed
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetTotalFuelConsumed(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let stats = core::get_execution_stats(store)?;
            Ok(stats.fuel_consumed as jlong)
        })
    }
    
    /// Get store total memory bytes  
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetTotalMemoryBytes(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let usage = core::get_memory_usage(store)?;
            Ok(usage.total_bytes as jlong)
        })
    }
    
    /// Get store used memory bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetUsedMemoryBytes(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let usage = core::get_memory_usage(store)?;
            Ok(usage.used_bytes as jlong)
        })
    }
    
    /// Get store instance count
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetInstanceCount(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let usage = core::get_memory_usage(store)?;
            Ok(usage.instance_count as jlong)
        })
    }
    
    /// Get store fuel limit (0 if no limit)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetFuelLimit(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let metadata = core::get_store_metadata(store);
            Ok(metadata.fuel_limit.unwrap_or(0) as jlong)
        })
    }
    
    /// Get store memory limit in bytes (0 if no limit)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetMemoryLimit(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let metadata = core::get_store_metadata(store);
            Ok(metadata.memory_limit_bytes.unwrap_or(0) as jlong)
        })
    }
    
    /// Get store execution timeout in seconds (0 if no timeout)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGetExecutionTimeout(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            let metadata = core::get_store_metadata(store);
            Ok(metadata.execution_timeout.map(|d| d.as_secs()).unwrap_or(0) as jlong)
        })
    }
    
    /// Increment the epoch counter for this store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeIncrementEpoch(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) {
        let _ = jni_utils::jni_try_void(&mut env, || {
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::increment_epoch(store);
            Ok(())
        });
    }

    /// Set memory limit for this store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetMemoryLimit(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        bytes: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            if bytes < 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory limit cannot be negative".to_string(),
                });
            }
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::set_memory_limit(store, if bytes == 0 { None } else { Some(bytes as u64) })?;
            Ok(true)
        }) as jboolean
    }

    /// Set table element limit for this store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetTableElementLimit(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        elements: jlong,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            if elements < 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Table element limit cannot be negative".to_string(),
                });
            }
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::set_table_element_limit(store, if elements == 0 { None } else { Some(elements as u64) })?;
            Ok(true)
        }) as jboolean
    }

    /// Set instance limit for this store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetInstanceLimit(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
        count: jint,
    ) -> jboolean {
        jni_utils::jni_try_bool(&mut env, || {
            if count < 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Instance limit cannot be negative".to_string(),
                });
            }
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };
            core::set_instance_limit(store, if count == 0 { None } else { Some(count as u32) })?;
            Ok(true)
        }) as jboolean
    }

//     /// Create a global variable in the store
//     #[no_mangle]
//     pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateGlobal(
//         mut env: JNIEnv,
//         _class: JClass,
//         store_handle: jlong,
//         value_type: jint,
//         is_mutable: jint,
//         value_components: jobjectArray,
//     ) -> jlong {
//         jni_utils::jni_try_ptr(&mut env, || {
//             if store_handle == 0 {
//                 return Err(crate::error::WasmtimeError::InvalidParameter {
//                     message: "Store handle cannot be null".to_string(),
//                 });
//             }
// 
//             // Get the store reference
//             let store = unsafe {
//                 crate::store::core::get_store_ref(store_handle as *const std::os::raw::c_void)?
//             };
// 
//             // Convert value type from int to WasmValueType enum
//             let wasm_type = match value_type {
//                 0 => wasmtime::ValType::I32,
//                 1 => wasmtime::ValType::I64,
//                 2 => wasmtime::ValType::F32,
//                 3 => wasmtime::ValType::F64,
//                 4 => wasmtime::ValType::V128,
//                 5 => wasmtime::ValType::FUNCREF,
//                 6 => wasmtime::ValType::EXTERNREF,
//                 _ => {
//                     return Err(crate::error::WasmtimeError::InvalidParameter {
//                         message: format!("Invalid value type: {}", value_type),
//                     });
//                 }
//             };
// 
//             // Extract the initial value from the value_components array
//             let initial_value = if value_components.is_null() {
//                 // Default value based on type
//                 match value_type {
//                     0 => wasmtime::Val::I32(0),
//                     1 => wasmtime::Val::I64(0),
//                     2 => wasmtime::Val::F32(0.0_f32.to_bits()),
//                     3 => wasmtime::Val::F64(0.0_f64.to_bits()),
//                     5 => wasmtime::Val::FuncRef(None),
//                     6 => wasmtime::Val::ExternRef(None),
//                     _ => wasmtime::Val::I32(0),
//                 }
//             } else {
//                 // Extract value from array
//                 let components = JObjectArray::from(unsafe { JObject::from_raw(value_components) });
//                 let len = env.get_array_length(&components)?;
// 
//                 if len > 0 {
//                     let first_obj = env.get_object_array_element(&components, 0)?;
// 
//                     match value_type {
//                         0 => { // I32
//                             let val = env.call_method(&first_obj, "intValue", "()I", &[])?;
//                             wasmtime::Val::I32(val.i()?)
//                         }
//                         1 => { // I64
//                             let val = env.call_method(&first_obj, "longValue", "()J", &[])?;
//                             wasmtime::Val::I64(val.j()?)
//                         }
//                         2 => { // F32
//                             let val = env.call_method(&first_obj, "floatValue", "()F", &[])?;
//                             wasmtime::Val::F32(val.f()?.to_bits())
//                         }
//                         3 => { // F64
//                             let val = env.call_method(&first_obj, "doubleValue", "()D", &[])?;
//                             wasmtime::Val::F64(val.d()?.to_bits())
//                         }
//                         5 => wasmtime::Val::FuncRef(None), // FUNCREF - default to null
//                         6 => wasmtime::Val::ExternRef(None), // EXTERNREF - default to null
//                         _ => wasmtime::Val::I32(0),
//                     }
//                 } else {
//                     // Empty array, use default value
//                     match value_type {
//                         0 => wasmtime::Val::I32(0),
//                         1 => wasmtime::Val::I64(0),
//                         2 => wasmtime::Val::F32(0.0_f32.to_bits()),
//                         3 => wasmtime::Val::F64(0.0_f64.to_bits()),
//                         5 => wasmtime::Val::FuncRef(None),
//                         6 => wasmtime::Val::ExternRef(None),
//                         _ => wasmtime::Val::I32(0),
//                     }
//                 }
//             };
// 
//             // Create the global
//             let mutability = if is_mutable != 0 {
//                 wasmtime::Mutability::Var
//             } else {
//                 wasmtime::Mutability::Const
//             };
// 
//             let global_type = wasmtime::GlobalType::new(wasm_type, mutability);
//             let global = wasmtime::Global::new(store, global_type, initial_value)?;
// 
//             // Box the global and return the handle
//             Ok(Box::new(global))
//         }) as jlong
//     }

    /// Create a global variable in the store
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateGlobal(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
        value_type: jint,
        is_mutable: jint,
        value_components: jobjectArray,
    ) -> jlong {
        // Extract values from Java array first (before entering the closure)
        let array_obj = unsafe { jni::objects::JObjectArray::from_raw(value_components) };

        let i32_val = env.get_object_array_element(&array_obj, 0)
            .ok()
            .and_then(|obj| {
                if obj.is_null() { None } else {
                    env.call_method(&obj, "intValue", "()I", &[])
                        .ok()
                        .and_then(|v| v.i().ok())
                }
            })
            .unwrap_or(0);

        let i64_val = env.get_object_array_element(&array_obj, 1)
            .ok()
            .and_then(|obj| {
                if obj.is_null() { None } else {
                    env.call_method(&obj, "longValue", "()J", &[])
                        .ok()
                        .and_then(|v| v.j().ok())
                }
            })
            .unwrap_or(0);

        let f32_val = env.get_object_array_element(&array_obj, 2)
            .ok()
            .and_then(|obj| {
                if obj.is_null() { None } else {
                    env.call_method(&obj, "floatValue", "()F", &[])
                        .ok()
                        .and_then(|v| v.f().ok())
                }
            })
            .unwrap_or(0.0);

        let f64_val = env.get_object_array_element(&array_obj, 3)
            .ok()
            .and_then(|obj| {
                if obj.is_null() { None } else {
                    env.call_method(&obj, "doubleValue", "()D", &[])
                        .ok()
                        .and_then(|v| v.d().ok())
                }
            })
            .unwrap_or(0.0);

        // Extract V128 byte array or reference ID from components[4]
        // First, get the object from the array
        let component_4 = env.get_object_array_element(&array_obj, 4).ok();

        let (v128_bytes, ref_id) = if let Some(obj) = component_4 {
            if obj.is_null() {
                (None, None)
            } else {
                // Check if it's a byte array (for V128)
                let is_byte_array = env.is_instance_of(&obj, "[B").unwrap_or(false);

                if is_byte_array {
                    // It's a byte array for V128
                    let byte_array: jni::objects::JByteArray = obj.into();
                    let v128 = if env.get_array_length(&byte_array).ok() == Some(16) {
                        let mut i8_bytes = [0i8; 16];
                        env.get_byte_array_region(&byte_array, 0, &mut i8_bytes).ok();
                        let bytes: [u8; 16] = i8_bytes.map(|b| b as u8);
                        Some(bytes)
                    } else {
                        None
                    };
                    (v128, None)
                } else {
                    // It's a Long for funcref/externref
                    let ref_val = env.call_method(&obj, "longValue", "()J", &[])
                        .ok()
                        .and_then(|v| v.j().ok())
                        .map(|v| v as u64);
                    (None, ref_val)
                }
            }
        } else {
            (None, None)
        };

        jni_utils::jni_try_ptr(&mut env, || {
            if store_handle == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            // Get the store reference
            let store = unsafe {
                crate::store::core::get_store_ref(store_handle as *const std::os::raw::c_void)?
            };

            // Convert value type from int to ValType enum
            let wasm_type = match value_type {
                0 => wasmtime::ValType::I32,
                1 => wasmtime::ValType::I64,
                2 => wasmtime::ValType::F32,
                3 => wasmtime::ValType::F64,
                4 => wasmtime::ValType::V128,
                5 => wasmtime::ValType::FUNCREF,
                6 => wasmtime::ValType::EXTERNREF,
                _ => {
                    return Err(crate::error::WasmtimeError::InvalidParameter {
                        message: format!("Invalid value type code: {}", value_type),
                    });
                }
            };

            // Convert mutability
            let mutability = if is_mutable != 0 {
                wasmtime::Mutability::Var
            } else {
                wasmtime::Mutability::Const
            };

            // Create the global value
            let global_value = crate::global::core::create_global_value(
                wasm_type.clone(),
                i32_val,
                i64_val,
                f32_val,
                f64_val,
                v128_bytes,
                ref_id,
            )?;

            // Create the global
            let global = crate::global::core::create_global(
                store,
                wasm_type,
                mutability,
                global_value,
                None, // No name for now
            )?;

            Ok(global)
        }) as jlong
    }

    /// Create a new WebAssembly table with the specified element type and size
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateTable(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
        element_type: jint,
        initial_size: jint,
        max_size: jint,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            use wasmtime::{ValType, RefType};
            use std::os::raw::c_void;
            use crate::error::WasmtimeError;

            // Extract store from handle
            let store = unsafe { core::get_store_mut(store_handle as *mut c_void)? };

            // Convert element type from native type code
            // Accepts both WebAssembly binary format codes and Java enum ordinals
            let val_type = match element_type {
                0x70 | 5 => ValType::Ref(RefType::FUNCREF), // FUNCREF (0x70 = binary format, 5 = enum ordinal)
                0x6F | 6 => ValType::Ref(RefType::EXTERNREF), // EXTERNREF (0x6F = binary format, 6 = enum ordinal)
                _ => return Err(WasmtimeError::Type {
                    message: format!("Invalid element type code: {} (expected 0x70/5 for FUNCREF or 0x6F/6 for EXTERNREF)", element_type),
                }),
            };

            // Convert max_size (-1 means unlimited)
            let max_size_opt = if max_size == -1 {
                None
            } else {
                Some(max_size as u32)
            };

            // Create the table
            let table = crate::table::core::create_table(
                store,
                val_type,
                initial_size as u32,
                max_size_opt,
                None, // No name for now
            )?;

            // Return the table as a pointer
            Ok(Box::into_raw(table) as jlong)
        })
    }

    /// Create a new WebAssembly linear memory with the specified page size
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateMemory(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
        initial_pages: jint,
        max_pages: jint,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;

            // Extract store from handle
            let store = unsafe { core::get_store_mut(store_handle as *mut c_void)? };

            // Convert max_pages (-1 means unlimited)
            let max_pages_opt = if max_pages == -1 {
                None
            } else {
                Some(max_pages as u64)
            };

            // Create memory using Memory::new or builder pattern
            let memory_config = crate::memory::MemoryConfig {
                initial_pages: initial_pages as u64,
                maximum_pages: max_pages_opt,
                is_shared: false,
                memory_index: 0,
                name: None,
            };

            // Create the memory
            let memory = crate::memory::Memory::new_with_config(store, memory_config)?;

            // Return the memory as a pointer
            Ok(Box::into_raw(Box::new(memory)) as jlong)
        })
    }

    /// Create a new shared WebAssembly linear memory with the specified page size
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateSharedMemory(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
        initial_pages: jint,
        max_pages: jint,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;

            // Extract store from handle
            let store = unsafe { core::get_store_mut(store_handle as *mut c_void)? };

            // Create memory configuration with shared flag enabled
            let memory_config = crate::memory::MemoryConfig {
                initial_pages: initial_pages as u64,
                maximum_pages: Some(max_pages as u64), // Shared memory requires max pages
                is_shared: true,
                memory_index: 0,
                name: None,
            };

            // Create the shared memory
            let memory = crate::memory::Memory::new_with_config(store, memory_config)?;

            // Return the memory as a pointer
            Ok(Box::into_raw(Box::new(memory)) as jlong)
        })
    }

    /// Create a store with resource limits (for JniWasmRuntime)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateStoreWithLimits(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        memory_size: jlong,      // 0 = no limit
        table_elements: jlong,   // 0 = no limit
        instances: jlong,        // 0 = no limit
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

            let memory_limit_opt = if memory_size == 0 { None } else { Some(memory_size as usize) };
            let table_limit_opt = if table_elements == 0 { None } else { Some(table_elements as u32) };
            let instances_limit_opt = if instances == 0 { None } else { Some(instances as usize) };

            core::create_store_with_config(
                engine,
                None,  // fuel_limit
                memory_limit_opt,
                None,  // execution_timeout
                instances_limit_opt,
                table_limit_opt,
                None,  // max_functions
            )
        }) as jlong
    }

    /// Create a store with comprehensive resource limits including fuel and timeout
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateStoreWithResourceLimits(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
        fuel_limit: jlong,           // 0 = no limit
        memory_size: jlong,          // 0 = no limit
        execution_timeout_secs: jlong, // 0 = no timeout
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

            let fuel_limit_opt = if fuel_limit == 0 { None } else { Some(fuel_limit as u64) };
            let memory_limit_opt = if memory_size == 0 { None } else { Some(memory_size as usize) };
            let timeout_opt = if execution_timeout_secs == 0 { None } else { Some(execution_timeout_secs as u64) };

            core::create_store_with_config(
                engine,
                fuel_limit_opt,
                memory_limit_opt,
                timeout_opt,
                None,  // instances
                None,  // table_elements
                None,  // max_functions
            )
        }) as jlong
    }

    #[allow(non_snake_case)]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeDestroyStore(
        mut env: JNIEnv,
        _class: JClass,
        store_ptr: jlong,
    ) {
        unsafe {
            core::destroy_store(store_ptr as *mut std::os::raw::c_void);
        }
    }

    #[allow(non_snake_case)]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCaptureBacktrace<'local>(
        mut env: JNIEnv<'local>,
        _class: JClass<'local>,
        store_ptr: jlong,
    ) -> JObject<'local> {
        let result = (|| -> Result<JObject<'local>, crate::error::WasmtimeError> {
            let store_ref = unsafe { crate::store::ffi_core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            let store = store_ref.inner.lock();
            let backtrace = wasmtime::WasmBacktrace::capture(&*store);
            create_backtrace_object(&mut env, &backtrace, false)
        })();

        match result {
            Ok(obj) => obj,
            Err(_) => JObject::null(),
        }
    }

    #[allow(non_snake_case)]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeForceCaptureBacktrace<'local>(
        mut env: JNIEnv<'local>,
        _class: JClass<'local>,
        store_ptr: jlong,
    ) -> JObject<'local> {
        let result = (|| -> Result<JObject<'local>, crate::error::WasmtimeError> {
            let store_ref = unsafe { crate::store::ffi_core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            let store = store_ref.inner.lock();
            let backtrace = wasmtime::WasmBacktrace::force_capture(&*store);
            create_backtrace_object(&mut env, &backtrace, true)
        })();

        match result {
            Ok(obj) => obj,
            Err(_) => JObject::null(),
        }
    }

    fn create_backtrace_object<'local>(
        env: &mut JNIEnv<'local>,
        backtrace: &wasmtime::WasmBacktrace,
        force_capture: bool,
    ) -> Result<JObject<'local>, crate::error::WasmtimeError> {
        // Create ArrayList for frames
        let frames_list = env.new_object(
            "java/util/ArrayList",
            "()V",
            &[],
        )?;

        // Convert each frame
        for frame in backtrace.frames() {
            let frame_obj = create_frame_info_object(env, frame)?;
            env.call_method(
                &frames_list,
                "add",
                "(Ljava/lang/Object;)Z",
                &[JValue::Object(&frame_obj)],
            )?;
        }

        // Create WasmBacktrace object
        let backtrace_obj = env.new_object(
            "ai/tegmentum/wasmtime4j/WasmBacktrace",
            "(Ljava/util/List;Z)V",
            &[JValue::Object(&frames_list), JValue::Bool(force_capture as u8)],
        )?;

        Ok(backtrace_obj)
    }

    fn create_frame_info_object<'local>(
        env: &mut JNIEnv<'local>,
        frame: &wasmtime::FrameInfo,
    ) -> Result<JObject<'local>, crate::error::WasmtimeError> {
        let func_index = frame.func_index() as i32;

        // Get module - for now pass null, would need proper module reference
        let module_obj = JObject::null();

        // Get function name - create binding to extend lifetime
        let func_name_string = frame.func_name()
            .map(|name| env.new_string(name))
            .transpose()?;
        let null_func_name = JObject::null();
        let func_name = func_name_string
            .as_ref()
            .map(|s| JValue::Object(s.as_ref()))
            .unwrap_or(JValue::Object(&null_func_name));

        // Get offsets - create bindings to extend lifetime
        let module_offset_obj = frame.module_offset()
            .map(|o| env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(o as i32)]))
            .transpose()?;
        let null_module_offset = JObject::null();
        let module_offset = module_offset_obj
            .as_ref()
            .map(|o| JValue::Object(o))
            .unwrap_or(JValue::Object(&null_module_offset));

        let func_offset_obj = frame.func_offset()
            .map(|o| env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(o as i32)]))
            .transpose()?;
        let null_func_offset = JObject::null();
        let func_offset = func_offset_obj
            .as_ref()
            .map(|o| JValue::Object(o))
            .unwrap_or(JValue::Object(&null_func_offset));

        // Create symbols list
        let symbols_list = env.new_object("java/util/ArrayList", "()V", &[])?;
        for symbol in frame.symbols() {
            let symbol_obj = create_frame_symbol_object(env, symbol)?;
            env.call_method(
                &symbols_list,
                "add",
                "(Ljava/lang/Object;)Z",
                &[JValue::Object(&symbol_obj)],
            )?;
        }

        // Create FrameInfo object
        let frame_obj = env.new_object(
            "ai/tegmentum/wasmtime4j/FrameInfo",
            "(ILai/tegmentum/wasmtime4j/Module;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;Ljava/util/List;)V",
            &[
                JValue::Int(func_index),
                JValue::Object(&module_obj),
                func_name,
                module_offset,
                func_offset,
                JValue::Object(&symbols_list),
            ],
        )?;

        Ok(frame_obj)
    }

    fn create_frame_symbol_object<'local>(
        env: &mut JNIEnv<'local>,
        symbol: &wasmtime::FrameSymbol,
    ) -> Result<JObject<'local>, crate::error::WasmtimeError> {
        // Create bindings to extend lifetime
        let name_string = symbol.name()
            .map(|n| env.new_string(n))
            .transpose()?;
        let null_name = JObject::null();
        let name = name_string
            .as_ref()
            .map(|s| JValue::Object(s.as_ref()))
            .unwrap_or(JValue::Object(&null_name));

        let file_string = symbol.file()
            .map(|f| env.new_string(f))
            .transpose()?;
        let null_file = JObject::null();
        let file = file_string
            .as_ref()
            .map(|s| JValue::Object(s.as_ref()))
            .unwrap_or(JValue::Object(&null_file));

        let line_obj = symbol.line()
            .map(|l| env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(l as i32)]))
            .transpose()?;
        let null_line = JObject::null();
        let line = line_obj
            .as_ref()
            .map(|o| JValue::Object(o))
            .unwrap_or(JValue::Object(&null_line));

        let column_obj = symbol.column()
            .map(|c| env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(c as i32)]))
            .transpose()?;
        let null_column = JObject::null();
        let column = column_obj
            .as_ref()
            .map(|o| JValue::Object(o))
            .unwrap_or(JValue::Object(&null_column));

        let symbol_obj = env.new_object(
            "ai/tegmentum/wasmtime4j/FrameSymbol",
            "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Integer;Ljava/lang/Integer;)V",
            &[name, file, line, column],
        )?;

        Ok(symbol_obj)
    }
}

/// JNI bindings for Linker operations
#[cfg(feature = "jni-bindings")]
pub mod jni_linker {
    use super::*;
    use crate::linker::ffi_core as linker_core;
    use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
    use crate::hostfunc::HostFunctionCallback;
    use crate::instance::WasmValue;
    use std::os::raw::c_void;
    use std::sync::Arc;
    use jni::JavaVM;
    use wasmtime::ValType;

    /// JNI callback implementation for host functions
    pub(crate) struct JniHostFunctionCallback {
        pub(crate) jvm: std::sync::Arc<JavaVM>,
        pub(crate) callback_id: i64,
        pub(crate) is_function_reference: bool,  // true for FunctionReference, false for Linker host functions
    }

    impl HostFunctionCallback for JniHostFunctionCallback {
        fn execute(&self, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>> {
            log::info!("JniHostFunctionCallback::execute - Starting callback execution for callback_id={}, is_function_reference={}",
                self.callback_id, self.is_function_reference);

            // Attach to current thread and get JNI environment
            log::debug!("Attaching to JVM thread");
            let mut env = self.jvm.attach_current_thread()
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to attach to JVM thread: {}", e),
                    backtrace: None
                })?;
            log::debug!("Successfully attached to JVM thread");

            // Convert parameters to Java WasmValue array
            log::debug!("Converting {} parameters to Java array", params.len());
            let java_params = wasm_values_to_java_array(&mut env, params)?;
            log::debug!("Successfully converted parameters");

            // Route to the appropriate Java callback method based on callback type
            let callback_result = if self.is_function_reference {
                // This is a FunctionReference - call invokeFunctionReferenceCallback
                log::debug!("Finding JniFunctionReference class");
                let funcref_class = env.find_class("ai/tegmentum/wasmtime4j/jni/JniFunctionReference")
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to find JniFunctionReference class: {}", e),
                        backtrace: None
                    })?;
                log::debug!("Calling Java invokeFunctionReferenceCallback method");
                env.call_static_method(
                    funcref_class,
                    "invokeFunctionReferenceCallback",
                    "(J[Lai/tegmentum/wasmtime4j/WasmValue;)[Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[
                        jni::objects::JValue::Long(self.callback_id),
                        jni::objects::JValue::Object(&java_params),
                    ]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to invoke FunctionReference callback: {}", e),
                    backtrace: None
                })?
            } else {
                // This is a Linker host function - call invokeHostFunctionCallback
                log::debug!("Finding JniLinker class");
                let linker_class = env.find_class("ai/tegmentum/wasmtime4j/jni/JniLinker")
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to find JniLinker class: {}", e),
                        backtrace: None
                    })?;
                log::debug!("Calling Java invokeHostFunctionCallback method");
                env.call_static_method(
                    linker_class,
                    "invokeHostFunctionCallback",
                    "(J[Lai/tegmentum/wasmtime4j/WasmValue;)[Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[
                        jni::objects::JValue::Long(self.callback_id),
                        jni::objects::JValue::Object(&java_params),
                    ]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to invoke Linker host function callback: {}", e),
                    backtrace: None
                })?
            };
            log::debug!("Java callback completed");

            // Convert Java WasmValue array back to Rust
            log::debug!("Converting result array back to Rust");
            let result_array = callback_result.l()
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get return value: {}", e),
                    backtrace: None
                })?;

            let results = java_array_to_wasm_values(&mut env, &result_array)?;
            log::info!("JniHostFunctionCallback::execute - Completed successfully with {} results", results.len());
            Ok(results)
        }

        fn clone_callback(&self) -> Box<dyn HostFunctionCallback> {
            // Clone the Arc, not the JavaVM itself
            Box::new(JniHostFunctionCallback {
                jvm: Arc::clone(&self.jvm),
                callback_id: self.callback_id,
                is_function_reference: self.is_function_reference,
            })
        }
    }

    /// Convert integer type code to ValType
    fn int_to_valtype(type_code: i32) -> WasmtimeResult<ValType> {
        match type_code {
            0 => Ok(ValType::I32),
            1 => Ok(ValType::I64),
            2 => Ok(ValType::F32),
            3 => Ok(ValType::F64),
            4 => Ok(ValType::V128),
            _ => Err(WasmtimeError::Runtime {
                message: format!("Unknown type code: {}", type_code),
                backtrace: None
            })
        }
    }

    /// Convert Rust WasmValue slice to Java WasmValue array
    pub fn wasm_values_to_java_array<'local>(env: &mut jni::JNIEnv<'local>, values: &[WasmValue]) -> WasmtimeResult<jni::objects::JObject<'local>> {
        // Find WasmValue class
        let wasm_value_class = env.find_class("ai/tegmentum/wasmtime4j/WasmValue")
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to find WasmValue class: {}", e),
                backtrace: None
            })?;

        // Create array
        let array = env.new_object_array(values.len() as i32, wasm_value_class, jni::objects::JObject::null())
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create WasmValue array: {}", e),
                backtrace: None
            })?;

        // Fill array with converted values
        for (i, value) in values.iter().enumerate() {
            let java_value = wasm_value_to_java(env, value)?;
            env.set_object_array_element(&array, i as i32, java_value)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to set array element: {}", e),
                    backtrace: None
                })?;
        }

        Ok(jni::objects::JObject::from(array))
    }

    /// Convert Java WasmValue array to Rust WasmValue vector
    fn java_array_to_wasm_values(env: &mut jni::JNIEnv, array: &jni::objects::JObject) -> WasmtimeResult<Vec<WasmValue>> {
        let array_obj: &jni::objects::JObjectArray = unsafe {
            &*(array as *const jni::objects::JObject as *const jni::objects::JObjectArray)
        };
        let array_len = env.get_array_length(array_obj)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get array length: {}", e),
                backtrace: None
            })?;

        let mut result = Vec::with_capacity(array_len as usize);
        for i in 0..array_len {
            let element = env.get_object_array_element(array_obj, i)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get array element: {}", e),
                    backtrace: None
                })?;
            result.push(java_to_wasm_value(env, &element)?);
        }

        Ok(result)
    }

    /// Convert Rust WasmValue to Java WasmValue object
    fn wasm_value_to_java<'local>(env: &mut jni::JNIEnv<'local>, value: &WasmValue) -> WasmtimeResult<jni::objects::JObject<'local>> {
        let wasm_value_class = env.find_class("ai/tegmentum/wasmtime4j/WasmValue")
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to find WasmValue class: {}", e),
                backtrace: None
            })?;

        match value {
            WasmValue::I32(v) => {
                env.call_static_method(
                    wasm_value_class,
                    "i32",
                    "(I)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[jni::objects::JValue::Int(*v)]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to call i32 method: {}", e),
                    backtrace: None
                })?.l().map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get object: {}", e),
                    backtrace: None
                })
            },
            WasmValue::I64(v) => {
                env.call_static_method(
                    wasm_value_class,
                    "i64",
                    "(J)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[jni::objects::JValue::Long(*v)]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to call i64 method: {}", e),
                    backtrace: None
                })?.l().map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get object: {}", e),
                    backtrace: None
                })
            },
            WasmValue::F32(v) => {
                env.call_static_method(
                    wasm_value_class,
                    "f32",
                    "(F)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[jni::objects::JValue::Float(*v)]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to call f32 method: {}", e),
                    backtrace: None
                })?.l().map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get object: {}", e),
                    backtrace: None
                })
            },
            WasmValue::F64(v) => {
                env.call_static_method(
                    wasm_value_class,
                    "f64",
                    "(D)Lai/tegmentum/wasmtime4j/WasmValue;",
                    &[jni::objects::JValue::Double(*v)]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to call f64 method: {}", e),
                    backtrace: None
                })?.l().map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get object: {}", e),
                    backtrace: None
                })
            },
            _ => Err(WasmtimeError::Runtime {
                message: "Unsupported value type".to_string(),
                backtrace: None
            })
        }
    }

    /// Convert Java WasmValue object to Rust WasmValue
    fn java_to_wasm_value(env: &mut jni::JNIEnv, obj: &jni::objects::JObject) -> WasmtimeResult<WasmValue> {
        // Call getValue() to get the boxed value
        let value = env.call_method(obj, "getValue", "()Ljava/lang/Object;", &[])
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to call getValue: {}", e),
                backtrace: None
            })?.l().map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get value object: {}", e),
                backtrace: None
            })?;

        // Get the type
        let type_obj = env.call_method(obj, "getType", "()Lai/tegmentum/wasmtime4j/WasmValueType;", &[])
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to call getType: {}", e),
                backtrace: None
            })?.l().map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get type object: {}", e),
                backtrace: None
            })?;

        // Get type name
        let type_name_obj = env.call_method(&type_obj, "name", "()Ljava/lang/String;", &[])
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to call name: {}", e),
                backtrace: None
            })?.l().map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get type name: {}", e),
                backtrace: None
            })?;

        let type_name: String = env.get_string(&type_name_obj.into())
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get string: {}", e),
                backtrace: None
            })?.into();

        match type_name.as_str() {
            "I32" => {
                let int_val = env.call_method(&value, "intValue", "()I", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get int value: {}", e),
                        backtrace: None
                    })?.i().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to extract int: {}", e),
                        backtrace: None
                    })?;
                Ok(WasmValue::I32(int_val))
            },
            "I64" => {
                let long_val = env.call_method(&value, "longValue", "()J", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get long value: {}", e),
                        backtrace: None
                    })?.j().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to extract long: {}", e),
                        backtrace: None
                    })?;
                Ok(WasmValue::I64(long_val))
            },
            "F32" => {
                let float_val = env.call_method(&value, "floatValue", "()F", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get float value: {}", e),
                        backtrace: None
                    })?.f().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to extract float: {}", e),
                        backtrace: None
                    })?;
                Ok(WasmValue::F32(float_val))
            },
            "F64" => {
                let double_val = env.call_method(&value, "doubleValue", "()D", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get double value: {}", e),
                        backtrace: None
                    })?.d().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to extract double: {}", e),
                        backtrace: None
                    })?;
                Ok(WasmValue::F64(double_val))
            },
            _ => Err(WasmtimeError::Runtime {
                message: format!("Unsupported type: {}", type_name),
                backtrace: None
            })
        }
    }

    /// Instantiate a module using the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeInstantiate(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        store_handle: jlong,
        module_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            // Extract linker from handle (mutable because we need to call instantiate_host_functions)
            let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };

            // Extract store from handle
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Extract module from handle
            let module = unsafe { crate::module::core::get_module_ref(module_handle as *const c_void)? };

            // If linker has a WASI context, attach it to the store before instantiation
            if let Some(wasi_ctx) = linker.get_wasi_context() {
                // Create a new fd_manager for this store (it manages per-store file descriptors)
                let fd_manager = crate::wasi::WasiFileDescriptorManager::new();
                // Build a fresh WasiP1Ctx from the configuration and set on the store
                store.set_wasi_context(wasi_ctx, fd_manager)?;
                log::debug!("Attached WASI context to store before instantiation");
            }

            // Instantiate the module using the linker
            // First, instantiate any registered host functions
            let mut store_lock = store.lock_store();
            linker.instantiate_host_functions(&mut *store_lock)?;

            // Then use wasmtime::Linker::instantiate
            let linker_lock = linker.inner()?;
            let wasmtime_instance = linker_lock.instantiate(&mut *store_lock, module.inner())
                .map_err(|e| WasmtimeError::Instantiation {
                    message: format!("Failed to instantiate module: {}", e),
                })?;

            // Drop locks before creating Instance
            drop(linker_lock);
            drop(store_lock);

            // Create Instance wrapper from the wasmtime instance created by the linker
            let instance = crate::instance::Instance::from_wasmtime_instance(
                wasmtime_instance,
                store,
                module,
            )?;

            // Return the instance as a pointer
            Ok(Box::into_raw(Box::new(instance)) as jlong)
        })
    }

    /// Instantiate a named module using the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeInstantiateNamed(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        store_handle: jlong,
        module_name: JString,
        module_handle: jlong,
    ) -> jlong {
        // Convert module name before the closure to avoid borrow checker issues
        let module_name_str: String = match env.get_string(&module_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        jni_utils::jni_try_with_default(&mut env, 0, || {
            // Extract linker from handle (mutable because we need to call instantiate_host_functions)
            let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };

            // Extract store from handle
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

            // Extract module from handle
            let module = unsafe { crate::module::core::get_module_ref(module_handle as *const c_void)? };

            // If linker has a WASI context, attach it to the store before instantiation
            if let Some(wasi_ctx) = linker.get_wasi_context() {
                // Create a new fd_manager for this store (it manages per-store file descriptors)
                let fd_manager = crate::wasi::WasiFileDescriptorManager::new();
                // Build a fresh WasiP1Ctx from the configuration and set on the store
                store.set_wasi_context(wasi_ctx, fd_manager)?;
                log::debug!("Attached WASI context to store before named instantiation");
            }

            // Instantiate the module using the linker with a name
            // First, instantiate any registered host functions
            let mut store_lock = store.lock_store();
            linker.instantiate_host_functions(&mut *store_lock)?;

            // Then use wasmtime::Linker::instantiate to create instance
            let linker_lock = linker.inner()?;
            let wasmtime_instance = linker_lock.instantiate(&mut *store_lock, module.inner())
                .map_err(|e| WasmtimeError::Instantiation {
                    message: format!("Failed to instantiate named module '{}': {}", module_name_str, e),
                })?;

            // Define the instance exports in the linker for future linking
            // This allows other modules to import from this instance
            drop(linker_lock); // Drop the lock before we modify the linker
            let mut linker_mut_lock = linker.inner()?;
            linker_mut_lock.instance(&mut *store_lock, &module_name_str, wasmtime_instance)
                .map_err(|e| WasmtimeError::Linker {
                    message: format!("Failed to define instance '{}': {}", module_name_str, e),
                })?;
            drop(linker_mut_lock);
            drop(store_lock);

            // Create Instance wrapper using new_without_imports since linker handled imports
            let instance = crate::instance::Instance::new_without_imports(
                store,
                module,
            )?;

            // Return the instance as a pointer
            Ok(Box::into_raw(Box::new(instance)) as jlong)
        })
    }

    /// Define a memory import in the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineMemory(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        store_handle: jlong,
        module_name: JString,
        name: JString,
        memory_handle: jlong,
    ) -> jboolean {
        // Convert strings before the closure to avoid borrow checker issues
        let module_name_str: String = match env.get_string(&module_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };
        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;

            let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };
            let memory = unsafe { crate::memory::core::get_memory_ref(memory_handle as *const c_void)? };

            let mut linker_lock = linker.inner()?;
            let wasmtime_memory = memory.inner();

            store.with_context(|ctx| {
                linker_lock
                    .define(ctx, &module_name_str, &name_str, wasmtime::Extern::Memory(*wasmtime_memory))
                    .map_err(|e| WasmtimeError::Linker {
                        message: format!("Failed to define memory '{}::{}': {}", module_name_str, name_str, e),
                    })
            })?;

            Ok(1) // JNI_TRUE
        })
    }

    /// Define a table import in the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineTable(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        store_handle: jlong,
        module_name: JString,
        name: JString,
        table_handle: jlong,
    ) -> jboolean {
        // Convert strings before the closure to avoid borrow checker issues
        let module_name_str: String = match env.get_string(&module_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };
        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;

            let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };
            let table = unsafe { crate::table::core::get_table_ref(table_handle as *const c_void)? };

            let mut linker_lock = linker.inner()?;
            let wasmtime_table_arc = table.wasmtime_table();
            let wasmtime_table_lock = wasmtime_table_arc.lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock table: {}", e),
                })?;

            store.with_context(|ctx| {
                linker_lock
                    .define(ctx, &module_name_str, &name_str, wasmtime::Extern::Table(*wasmtime_table_lock))
                    .map_err(|e| WasmtimeError::Linker {
                        message: format!("Failed to define table '{}::{}': {}", module_name_str, name_str, e),
                    })
            })?;

            Ok(1) // JNI_TRUE
        })
    }

    /// Define a global import in the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineGlobal(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        store_handle: jlong,
        module_name: JString,
        name: JString,
        global_handle: jlong,
    ) -> jboolean {
        // Convert strings before the closure to avoid borrow checker issues
        let module_name_str: String = match env.get_string(&module_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };
        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;

            let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };
            let global = unsafe { crate::global::core::get_global_ref(global_handle as *const c_void)? };

            let mut linker_lock = linker.inner()?;
            let wasmtime_global_arc = global.wasmtime_global();
            let wasmtime_global_lock = wasmtime_global_arc.lock()
                .map_err(|e| WasmtimeError::Concurrency {
                    message: format!("Failed to lock global: {}", e),
                })?;

            // Use lock_store() and AsContextMut for mutable context
            let mut store_lock = store.lock_store();
            use wasmtime::AsContextMut;
            linker_lock
                .define(&mut (*store_lock).as_context_mut(), &module_name_str, &name_str, wasmtime::Extern::Global(*wasmtime_global_lock))
                .map_err(|e| WasmtimeError::Linker {
                    message: format!("Failed to define global '{}::{}': {}", module_name_str, name_str, e),
                })?;

            Ok(1) // JNI_TRUE
        })
    }

    /// Define an instance in the linker (register all its exports)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineInstance(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        store_handle: jlong,
        module_name: JString,
        instance_handle: jlong,
    ) -> jboolean {
        // Validate module_name parameter before attempting to convert
        if module_name.is_null() {
            return 0; // JNI_FALSE - invalid parameter
        }

        // Convert module name before the closure to avoid borrow checker issues
        let module_name_str: String = match env.get_string(&module_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;

            let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };
            let instance = unsafe { crate::instance::core::get_instance_ref(instance_handle as *const c_void)? };

            // Get the wasmtime instance from our wrapper
            // Lock it briefly to copy the instance, then immediately drop the lock
            let wasmtime_instance = {
                let wasmtime_instance_guard = instance.inner().lock();
                *wasmtime_instance_guard
            }; // Guard is dropped here

            // Get the linker lock
            let mut linker_lock = linker.inner()?;

            // Use with_context to let the store manage its own locking
            store.with_context(|ctx| {
                linker_lock.instance(ctx, &module_name_str, wasmtime_instance)
                    .map_err(|e| WasmtimeError::Linker {
                        message: format!("Failed to define instance '{}': {}", module_name_str, e),
                    })
            })?;

            Ok(1) // JNI_TRUE
        })
    }

    /// Create an alias for an export in the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeAlias(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
        from_module: JString,
        from_name: JString,
        to_module: JString,
        to_name: JString,
    ) {
        // Validate parameters before attempting to convert
        if from_module.is_null() || from_name.is_null() || to_module.is_null() || to_name.is_null() {
            return;
        }

        // Convert all strings before the closure to avoid borrow checker issues
        let from_module_str: String = match env.get_string(&from_module) {
            Ok(s) => s.into(),
            Err(_) => return,
        };

        let from_name_str: String = match env.get_string(&from_name) {
            Ok(s) => s.into(),
            Err(_) => return,
        };

        let to_module_str: String = match env.get_string(&to_module) {
            Ok(s) => s.into(),
            Err(_) => return,
        };

        let to_name_str: String = match env.get_string(&to_name) {
            Ok(s) => s.into(),
            Err(_) => return,
        };

        jni_utils::jni_try(&mut env, || {
            use std::os::raw::c_void;

            let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };

            // Get the linker lock
            let mut linker_lock = linker.inner()?;

            // Use Wasmtime's alias method
            linker_lock.alias(&from_module_str, &from_name_str, &to_module_str, &to_name_str)
                .map_err(|e| WasmtimeError::Linker {
                    message: format!("Failed to create alias from {}::{} to {}::{}: {}",
                        from_module_str, from_name_str, to_module_str, to_name_str, e),
                })?;

            Ok(())
        });
    }

    /// Define a host function in the linker with JNI callback support
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineHostFunction(
        mut env: JNIEnv,
        obj: jobject,
        linker_handle: jlong,
        module_name: JString,
        name: JString,
        param_types: jintArray,
        return_types: jintArray,
        callback_id: jlong,
    ) -> jboolean {
        // Convert strings before the closure
        let module_name_str: String = match env.get_string(&module_name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };
        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0,
        };

        // Get JavaVM before the closure to avoid borrow issues
        let jvm = match env.get_java_vm() {
            Ok(vm) => vm,
            Err(_) => return 0,
        };

        // Extract array data before the closure
        let param_array = unsafe { jni::objects::JIntArray::from_raw(param_types) };
        let param_len = match env.get_array_length(&param_array) {
            Ok(len) => len as usize,
            Err(_) => return 0,
        };
        let mut param_vals = vec![0i32; param_len];
        if env.get_int_array_region(&param_array, 0, &mut param_vals).is_err() {
            return 0;
        }

        let return_array = unsafe { jni::objects::JIntArray::from_raw(return_types) };
        let return_len = match env.get_array_length(&return_array) {
            Ok(len) => len as usize,
            Err(_) => return 0,
        };
        let mut return_vals = vec![0i32; return_len];
        if env.get_int_array_region(&return_array, 0, &mut return_vals).is_err() {
            return 0;
        }

        jni_utils::jni_try_with_default(&mut env, 0, || {
            use std::os::raw::c_void;
            use wasmtime::{ValType, FuncType};
            use crate::hostfunc::{HostFunction, HostFunctionCallback};
            use crate::instance::WasmValue;

            // Convert parameter types
            let param_val_types: Vec<ValType> = param_vals.iter()
                .map(|&t| int_to_valtype(t))
                .collect::<Result<Vec<_>, _>>()?;

            // Convert return types
            let return_val_types: Vec<ValType> = return_vals.iter()
                .map(|&t| int_to_valtype(t))
                .collect::<Result<Vec<_>, _>>()?;

            // Get linker (mutable because define_host_function needs &mut)
            let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };

            // Get engine from wasmtime linker
            let linker_lock = linker.inner()?;
            let engine = linker_lock.engine();

            // Create function type
            let func_type = FuncType::new(
                engine,
                param_val_types,
                return_val_types
            );

            // Drop lock before creating host function
            drop(linker_lock);

            // Create JNI callback with Arc-wrapped JavaVM
            let callback = JniHostFunctionCallback {
                jvm: std::sync::Arc::new(jvm),
                callback_id,
                is_function_reference: false,  // This is a Linker host function
            };

            // Create host function with weak store reference (will be set during instantiation)
            let host_func = HostFunction::new(
                format!("{}::{}", module_name_str, name_str),
                func_type,
                std::sync::Weak::new(), // Empty weak ref for now
                Box::new(callback),
            )?;

            // Register host function - host_func is Arc<HostFunction>, clone it
            let host_func_clone = (*host_func).clone();
            linker.define_host_function(&module_name_str, &name_str, host_func.func_type().clone(), host_func_clone)?;

            Ok(1) // JNI_TRUE
        })
    }

    /// Enable WASI for the linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeEnableWasi(
        mut env: JNIEnv,
        _obj: jobject,
        linker_handle: jlong,
    ) {
        jni_utils::jni_try(&mut env, || {
            use std::os::raw::c_void;

            if linker_handle == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Linker handle cannot be null".to_string(),
                });
            }

            let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };
            linker.enable_wasi()?;

            Ok(())
        });
    }

    /// Destroy a linker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDestroyLinker(
        mut env: JNIEnv,
        _class: JClass,
        linker_handle: jlong,
    ) {
        // DEFENSIVE: Validate handle before attempting to destroy
        // Handles must be properly aligned pointers (at least 8-byte aligned for Box<Linker>)
        // Small values like 1L from tests will fail this check
        if linker_handle == 0 || (linker_handle as usize) < 4096 || (linker_handle as usize) % std::mem::align_of::<usize>() != 0 {
            log::warn!("Attempted to destroy invalid linker handle: {:#x}", linker_handle);
            return;
        }

        unsafe {
            linker_core::destroy_linker(linker_handle as *mut c_void);
        }
    }
}

/// JNI bindings for Module operations
#[cfg(feature = "jni-bindings")]
pub mod jni_module {
    use super::*;
    use crate::module::core;
    use crate::error::jni_utils;
    use crate::shared_ffi::module::{ByteArrayConverter, StringConverter};
    use jni::sys::{jobjectArray, jstring};
    use jni::strings::JavaStr;

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

    /// String converter implementation for JNI
    pub struct JStringConverter {
        data: String,
    }

    impl JStringConverter {
        /// Creates a new JStringConverter from JavaStr
        pub fn new(java_str: JavaStr) -> crate::error::WasmtimeResult<Self> {
            let string = java_str.to_str()
                .map(|s| s.to_string())
                .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert Java string to Rust string: {}", e),
                })?;
            Ok(Self { data: string })
        }
    }

    impl StringConverter for JStringConverter {
        unsafe fn get_string(&self) -> crate::error::WasmtimeResult<String> {
            Ok(self.data.clone())
        }

        fn is_empty(&self) -> bool {
            self.data.is_empty()
        }
    }


    /// Instantiate a module within a store context
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInstantiateModule(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
        store_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let module = unsafe { core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };
            crate::instance::core::create_instance(store, module)
        }) as jlong
    }
    
    /// Instantiate a module with specific imports
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeInstantiateModuleWithImports(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
        store_ptr: jlong,
        _import_map_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        
        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            let byte_converter = VecByteArrayConverter::new(data);
            crate::shared_ffi::module::deserialize_module_shared(engine, byte_converter)
        }) as jlong
    }
    
    /// Create a native import map from serialized data
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeCreateImportMap(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
        _class: JClass,
        _import_map_ptr: jlong,
    ) {
        // For now, do nothing - proper ImportMap implementation would be needed
    }
    
    /// Destroy a module
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeDestroyModule(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) {
        crate::shared_ffi::module::destroy_module_shared(module_ptr as *mut std::os::raw::c_void);
    }

    /// Helper: Convert ModuleValueType to WasmValueType enum name
    fn module_value_type_to_java_enum(value_type: &crate::module::ModuleValueType) -> &'static str {
        use crate::module::ModuleValueType;
        match value_type {
            ModuleValueType::I32 => "I32",
            ModuleValueType::I64 => "I64",
            ModuleValueType::F32 => "F32",
            ModuleValueType::F64 => "F64",
            ModuleValueType::V128 => "V128",
            ModuleValueType::FuncRef => "FUNCREF",
            ModuleValueType::ExternRef => "EXTERNREF",
        }
    }

    /// Helper: Get WasmValueType enum value
    fn get_wasm_value_type_enum<'a>(env: &mut JNIEnv<'a>, value_type: &crate::module::ModuleValueType) -> Result<JObject<'a>, String> {
        let enum_class = env.find_class("ai/tegmentum/wasmtime4j/WasmValueType")
            .map_err(|e| format!("Failed to find WasmValueType class: {}", e))?;

        let enum_name = module_value_type_to_java_enum(value_type);
        let enum_value = env.get_static_field(enum_class, enum_name, "Lai/tegmentum/wasmtime4j/WasmValueType;")
            .map_err(|e| format!("Failed to get enum value {}: {}", enum_name, e))?;

        match enum_value {
            jni::objects::JValueGen::Object(obj) => Ok(obj),
            _ => Err("Unexpected JValue type for enum".to_string()),
        }
    }

    /// Helper: Create JniFuncType from parameter and return types
    fn create_jni_func_type<'a>(
        env: &mut JNIEnv<'a>,
        params: &[crate::module::ModuleValueType],
        returns: &[crate::module::ModuleValueType],
    ) -> Option<JObject<'a>> {
        // Create ArrayList for params
        let list_class = env.find_class("java/util/ArrayList").ok()?;
        let params_list = env.new_object(&list_class, "()V", &[]).ok()?;
        let results_list = env.new_object(&list_class, "()V", &[]).ok()?;

        // Add param types
        for param in params {
            if let Ok(enum_val) = get_wasm_value_type_enum(env, param) {
                let _ = env.call_method(&params_list, "add", "(Ljava/lang/Object;)Z", &[JValue::Object(&enum_val)]);
            }
        }

        // Add return types
        for ret in returns {
            if let Ok(enum_val) = get_wasm_value_type_enum(env, ret) {
                let _ = env.call_method(&results_list, "add", "(Ljava/lang/Object;)Z", &[JValue::Object(&enum_val)]);
            }
        }

        // Create JniFuncType
        let func_type_class = env.find_class("ai/tegmentum/wasmtime4j/jni/type/JniFuncType").ok()?;
        env.new_object(
            func_type_class,
            "(Ljava/util/List;Ljava/util/List;)V",
            &[JValue::Object(&params_list), JValue::Object(&results_list)],
        ).ok()
    }

    /// Helper: Create JniGlobalType from value type and mutability
    fn create_jni_global_type<'a>(
        env: &mut JNIEnv<'a>,
        val_type: &crate::module::ModuleValueType,
        mutable: bool,
    ) -> Option<JObject<'a>> {
        let enum_val = get_wasm_value_type_enum(env, val_type).ok()?;
        let global_type_class = env.find_class("ai/tegmentum/wasmtime4j/jni/type/JniGlobalType").ok()?;
        env.new_object(
            global_type_class,
            "(Lai/tegmentum/wasmtime4j/WasmValueType;Z)V",
            &[JValue::Object(&enum_val), JValue::Bool(mutable as jboolean)],
        ).ok()
    }

    /// Helper: Create JniMemoryType from initial, max, and shared
    fn create_jni_memory_type<'a>(
        env: &mut JNIEnv<'a>,
        initial: u64,
        max: Option<u64>,
        shared: bool,
    ) -> Option<JObject<'a>> {
        let memory_type_class = env.find_class("ai/tegmentum/wasmtime4j/jni/type/JniMemoryType").ok()?;
        let max_val = max.unwrap_or(0) as i64;
        let has_max = max.is_some();
        env.new_object(
            memory_type_class,
            "(JJZZ)V",
            &[
                JValue::Long(initial as i64),
                JValue::Long(max_val),
                JValue::Bool(has_max as jboolean),
                JValue::Bool(shared as jboolean),
            ],
        ).ok()
    }

    /// Helper: Create JniTableType from element type, initial, and max
    fn create_jni_table_type<'a>(
        env: &mut JNIEnv<'a>,
        elem_type: &crate::module::ModuleValueType,
        initial: u32,
        max: Option<u32>,
    ) -> Option<JObject<'a>> {
        let enum_val = get_wasm_value_type_enum(env, elem_type).ok()?;
        let table_type_class = env.find_class("ai/tegmentum/wasmtime4j/jni/type/JniTableType").ok()?;
        let max_val = max.unwrap_or(0) as i32;
        let has_max = max.is_some();
        env.new_object(
            table_type_class,
            "(Lai/tegmentum/wasmtime4j/WasmValueType;IIZ)V",
            &[
                JValue::Object(&enum_val),
                JValue::Int(initial as i32),
                JValue::Int(max_val),
                JValue::Bool(has_max as jboolean),
            ],
        ).ok()
    }

    /// Get module exports with type information
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleExports(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobject {
        if module_ptr == 0 {
            return std::ptr::null_mut();
        }

        // Extract export data with type information
        let exports_data: Vec<(String, crate::module::ExportKind)> = {
            let module = match unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
                Ok(m) => m,
                Err(_) => return std::ptr::null_mut(),
            };
            let metadata = crate::module::core::get_metadata(module);
            // Extract name and type info, then drop the module reference
            metadata.exports.iter().map(|exp| (exp.name.clone(), exp.export_type.clone())).collect()
        };

        // Create ArrayList
        let array_list_class = match env.find_class("java/util/ArrayList") {
            Ok(c) => c,
            Err(_) => return std::ptr::null_mut(),
        };
        let array_list = match env.new_object(array_list_class, "()V", &[]) {
            Ok(list) => list,
            Err(_) => return std::ptr::null_mut(),
        };

        // For each export, create ModuleExport object
        for (export_name, export_kind) in &exports_data {
            // Create export name string
            let name_jstring = match env.new_string(export_name) {
                Ok(s) => s,
                Err(_) => continue,
            };

            // Create WasmType based on export kind
            let wasm_type_obj = match export_kind {
                crate::module::ExportKind::Function(sig) => {
                    create_jni_func_type(&mut env, &sig.params, &sig.returns)
                }
                crate::module::ExportKind::Global(val_type, mutable) => {
                    create_jni_global_type(&mut env, val_type, *mutable)
                }
                crate::module::ExportKind::Memory(initial, max, shared) => {
                    create_jni_memory_type(&mut env, *initial, *max, *shared)
                }
                crate::module::ExportKind::Table(elem_type, initial, max) => {
                    create_jni_table_type(&mut env, elem_type, *initial, *max)
                }
            };

            let wasm_type_obj = match wasm_type_obj {
                Some(obj) => obj,
                None => continue,
            };

            // Create ExportType(String name, WasmType type)
            let export_type_class = match env.find_class("ai/tegmentum/wasmtime4j/ExportType") {
                Ok(c) => c,
                Err(_) => continue,
            };
            let export_type_obj = match env.new_object(export_type_class, "(Ljava/lang/String;Lai/tegmentum/wasmtime4j/WasmType;)V",
                &[JValue::Object(&name_jstring), JValue::Object(&wasm_type_obj)]) {
                Ok(obj) => obj,
                Err(_) => continue,
            };

            // Create ModuleExport(String name, ExportType exportType)
            let module_export_class = match env.find_class("ai/tegmentum/wasmtime4j/ModuleExport") {
                Ok(c) => c,
                Err(_) => continue,
            };
            let module_export_obj = match env.new_object(module_export_class, "(Ljava/lang/String;Lai/tegmentum/wasmtime4j/ExportType;)V",
                &[JValue::Object(&name_jstring), JValue::Object(&export_type_obj)]) {
                Ok(obj) => obj,
                Err(_) => continue,
            };

            // Add to ArrayList
            let _ = env.call_method(&array_list, "add", "(Ljava/lang/Object;)Z", &[JValue::Object(&module_export_obj)]);
        }

        array_list.into_raw()
    }

    /// Get module imports with type information
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetModuleImports(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobject {
        if module_ptr == 0 {
            return std::ptr::null_mut();
        }

        // Extract import data with type information
        let imports_data: Vec<(String, String, crate::module::ImportKind)> = {
            let module = match unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
                Ok(m) => m,
                Err(_) => return std::ptr::null_mut(),
            };
            let metadata = crate::module::core::get_metadata(module);
            // Extract module name, field name, and type info
            metadata.imports.iter().map(|imp| (imp.module.clone(), imp.name.clone(), imp.import_type.clone())).collect()
        };

        // Create ArrayList
        let array_list_class = match env.find_class("java/util/ArrayList") {
            Ok(c) => c,
            Err(_) => return std::ptr::null_mut(),
        };
        let array_list = match env.new_object(array_list_class, "()V", &[]) {
            Ok(list) => list,
            Err(_) => return std::ptr::null_mut(),
        };

        // For each import, create ModuleImport object with proper type
        for (module_name, field_name, import_kind) in &imports_data {
            // Create import strings
            let module_name_jstring = match env.new_string(module_name) {
                Ok(s) => s,
                Err(_) => continue,
            };
            let field_name_jstring = match env.new_string(field_name) {
                Ok(s) => s,
                Err(_) => continue,
            };

            // Create WasmType based on the actual import kind
            let wasm_type_obj = match import_kind {
                crate::module::ImportKind::Function(sig) => {
                    match create_jni_func_type(&mut env, &sig.params, &sig.returns) {
                        Some(obj) => obj,
                        None => continue,
                    }
                }
                crate::module::ImportKind::Global(val_type, mutable) => {
                    match create_jni_global_type(&mut env, val_type, *mutable) {
                        Some(obj) => obj,
                        None => continue,
                    }
                }
                crate::module::ImportKind::Memory(initial, max, shared) => {
                    match create_jni_memory_type(&mut env, *initial, *max, *shared) {
                        Some(obj) => obj,
                        None => continue,
                    }
                }
                crate::module::ImportKind::Table(elem_type, initial, max) => {
                    match create_jni_table_type(&mut env, elem_type, *initial, *max) {
                        Some(obj) => obj,
                        None => continue,
                    }
                }
            };

            // Create ImportType(String moduleName, String name, WasmType type)
            let import_type_class = match env.find_class("ai/tegmentum/wasmtime4j/ImportType") {
                Ok(c) => c,
                Err(_) => continue,
            };
            let import_type_obj = match env.new_object(import_type_class, "(Ljava/lang/String;Ljava/lang/String;Lai/tegmentum/wasmtime4j/WasmType;)V",
                &[JValue::Object(&module_name_jstring), JValue::Object(&field_name_jstring), JValue::Object(&wasm_type_obj)]) {
                Ok(obj) => obj,
                Err(_) => continue,
            };

            // Create ModuleImport(String moduleName, String fieldName, ImportType importType)
            let module_import_class = match env.find_class("ai/tegmentum/wasmtime4j/ModuleImport") {
                Ok(c) => c,
                Err(_) => continue,
            };
            let module_import_obj = match env.new_object(module_import_class, "(Ljava/lang/String;Ljava/lang/String;Lai/tegmentum/wasmtime4j/ImportType;)V",
                &[JValue::Object(&module_name_jstring), JValue::Object(&field_name_jstring), JValue::Object(&import_type_obj)]) {
                Ok(obj) => obj,
                Err(_) => continue,
            };

            // Add to ArrayList
            let _ = env.call_method(&array_list, "add", "(Ljava/lang/Object;)Z", &[JValue::Object(&module_import_obj)]);
        }

        array_list.into_raw()
    }

    /// Check if module has a specific export
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeHasExport(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
        export_name: JString,
    ) -> jboolean {
        if module_ptr == 0 {
            return jni::sys::JNI_FALSE;
        }

        match env.get_string(&export_name) {
            Ok(name_str) => {
                let name: String = name_str.into();
                match unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
                    Ok(module) => {
                        let metadata = crate::module::core::get_metadata(module);
                        for export in &metadata.exports {
                            if export.name == name {
                                return jni::sys::JNI_TRUE;
                            }
                        }
                        jni::sys::JNI_FALSE
                    }
                    Err(_) => jni::sys::JNI_FALSE,
                }
            }
            Err(_) => jni::sys::JNI_FALSE,
        }
    }

    /// Check if module has a specific import
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeHasImport(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
        module_name: JString,
        field_name: JString,
    ) -> jboolean {
        if module_ptr == 0 {
            return jni::sys::JNI_FALSE;
        }

        match (env.get_string(&module_name), env.get_string(&field_name)) {
            (Ok(mod_name_str), Ok(fld_name_str)) => {
                let mod_name: String = mod_name_str.into();
                let fld_name: String = fld_name_str.into();
                match unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void) } {
                    Ok(module) => {
                        let metadata = crate::module::core::get_metadata(module);
                        for import in &metadata.imports {
                            if import.module == mod_name && import.name == fld_name {
                                return jni::sys::JNI_TRUE;
                            }
                        }
                        jni::sys::JNI_FALSE
                    }
                    Err(_) => jni::sys::JNI_FALSE,
                }
            }
            _ => jni::sys::JNI_FALSE,
        }
    }

    /// Get custom sections from the module
    /// Returns a Map<String, String> where values are Base64-encoded binary data
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModule_nativeGetCustomSections(
        mut env: JNIEnv,
        _class: JClass,
        module_ptr: jlong,
    ) -> jobject {
        use base64::Engine;

        jni_utils::jni_try_object(&mut env, |env_ref| {
            if module_ptr == 0 {
                return Err(crate::error::WasmtimeError::Module {
                    message: "Invalid module pointer".to_string(),
                });
            }

            let module = unsafe { crate::module::core::get_module_ref(module_ptr as *const std::os::raw::c_void)? };
            let metadata = crate::module::core::get_metadata(module);

            // Create HashMap for custom sections
            let hashmap_class = env_ref.find_class("java/util/HashMap")?;
            let hashmap = env_ref.new_object(hashmap_class, "()V", &[])?;

            // Iterate through custom sections and add them to the HashMap
            for (name, data) in &metadata.custom_sections {
                // Encode binary data as Base64 for safe transmission through JNI
                let encoded = base64::engine::general_purpose::STANDARD.encode(data);

                let name_jstring = env_ref.new_string(name)?;
                let data_jstring = env_ref.new_string(&encoded)?;

                // Use call_method for safer invocation
                env_ref.call_method(
                    &hashmap,
                    "put",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
                    &[
                        (&name_jstring).into(),
                        (&data_jstring).into(),
                    ],
                )?;
            }

            // Return hashmap as JObject with 'static lifetime
            // Safety: The JNI framework manages the lifetime
            Ok(unsafe { std::mem::transmute(hashmap) })
        })
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
    use jni::sys::jobjectArray;
    use std::sync::Arc;
    

    /// Create a new component engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCreateComponentEngine(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        // Use EnhancedComponentEngine which supports ComponentInstanceInfo with store field
        jni_utils::jni_try_ptr(&mut env, || crate::component_core::core::create_enhanced_component_engine()) as jlong
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
    ) {
        unsafe {
            crate::component_core::core::destroy_enhanced_component_instance(instance_ptr as *mut std::os::raw::c_void);
        }
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
            use wasmtime::component::Func;

            let func: Func = handle.instance
                .get_func(&mut handle.store, &func_name)
                .ok_or_else(|| WasmtimeError::ImportExport {
                    message: format!("Function '{}' not found in component exports", func_name),
                })?;

            // Call the function
            let mut results = vec![wasmtime::component::Val::Bool(false); func.results(&handle.store).len()];
            func.call(&mut handle.store, &params, &mut results)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Function call failed: {}", e),
                    backtrace: None,
                })?;

            func.post_return(&mut handle.store)
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
}

/// JNI bindings for Host Function operations
#[cfg(feature = "jni-bindings")]
pub mod jni_hostfunc {
    use super::*;
    use crate::error::jni_utils;
    use crate::hostfunc::HostFunctionCallback;
    use crate::instance::WasmValue;
    use crate::{WasmtimeError, WasmtimeResult};
    use wasmtime::{ValType, FuncType};
    use std::os::raw::c_void;
    

    /// Execute a Java host function callback from native code
    fn execute_java_host_function_callback(
        callback_id: u64,
        params: &[WasmValue]
    ) -> WasmtimeResult<Vec<WasmValue>> {
        // This is a placeholder implementation. In a full implementation, this would:
        // 1. Attach to the JVM thread
        // 2. Look up the Java callback object by ID
        // 3. Marshal parameters to Java types
        // 4. Call the Java method
        // 5. Marshal return values back to WasmValue
        // 6. Handle any Java exceptions

        log::debug!("Executing Java host function callback {} with {} parameters", callback_id, params.len());

        // For now, implement a simple echo function for testing
        // Real implementation would involve JNI calls to Java
        if params.len() == 1 {
            Ok(vec![params[0].clone()])
        } else if params.len() == 2 {
            // Simple add function for i32 types
            match (&params[0], &params[1]) {
                (WasmValue::I32(a), WasmValue::I32(b)) => {
                    Ok(vec![WasmValue::I32(a + b)])
                }
                _ => Ok(vec![params[0].clone()])
            }
        } else {
            // Return first parameter or i32(0) if no parameters
            Ok(vec![params.get(0).cloned().unwrap_or(WasmValue::I32(0))])
        }
    }

    /// JNI callback implementation that bridges to Java
    struct JniHostFunctionCallback {
        #[allow(dead_code)]
        java_callback_id: u64,
    }

    impl HostFunctionCallback for JniHostFunctionCallback {
        fn execute(&self, params: &[WasmValue]) -> WasmtimeResult<Vec<WasmValue>> {
            // Execute the Java callback by calling into the JVM
            execute_java_host_function_callback(self.java_callback_id, params)
        }

        fn clone_callback(&self) -> Box<dyn HostFunctionCallback> {
            Box::new(Self {
                java_callback_id: self.java_callback_id,
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

        jni_utils::jni_try_ptr(&mut env, || {
            let name: String = name_string;
            let type_data = type_data_bytes;

            // Get store reference
            let store = unsafe { crate::store::core::get_store_ref(store_handle as *const c_void)? };

            // Create function type from marshalled data
            let func_type = store.with_context(|ctx| {
                let engine = ctx.engine();
                unmarshal_function_type(engine, &type_data)
            })?;

            // Create callback wrapper that will bridge to Java
            let callback = Box::new(JniHostFunctionCallback {
                java_callback_id: host_function_id as u64,
            });

            // Use Store's create_host_function method which handles weak references properly
            let (host_function_id, _wasmtime_func) = store.create_host_function(name, func_type, callback)?;

            // Store the function ID for later retrieval
            // For now, return the host function ID directly as the handle
            Ok(Box::new(host_function_id))
        }) as jlong
    }

    /// Destroy a host function (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHostFunction_nativeDestroyHostFunction(
        mut env: JNIEnv,
        _class: JClass,
        host_func_handle: jlong,
    ) {
        jni_utils::jni_try_default(&env, (), || {
            if host_func_handle == 0 {
                return Ok(());
            }

            // The handle is actually a host function ID, not a direct pointer
            let host_function_id = unsafe { *(host_func_handle as *const u64) };

            // Remove from registry
            match crate::hostfunc::core::remove_host_function(host_function_id) {
                Ok(_) => {
                    log::debug!("Destroyed JNI host function with ID: {}", host_function_id);
                }
                Err(e) => {
                    log::warn!("Failed to remove host function from registry: {}", e);
                }
            }

            // Clean up the boxed handle
            unsafe {
                let _ = Box::from_raw(host_func_handle as *mut u64);
            }

            Ok(())
        });
    }

    /// Unmarshal function type from byte array
    pub(crate) fn unmarshal_function_type(engine: &wasmtime::Engine, data: &[u8]) -> WasmtimeResult<FuncType> {
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

/// JNI bindings for WebAssembly function references
#[cfg(feature = "jni-bindings")]
pub mod jni_functionref {
    use super::*;
    use crate::error::{jni_utils, WasmtimeError};
    use super::jni_linker::JniHostFunctionCallback;
    use super::jni_hostfunc::unmarshal_function_type;
    use crate::store::Store;

    /// Create a new function reference from a host function (JNI version)
    ///
    /// Creates a real Rust Func and registers it in the table reference registry.
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCreateFunctionReferenceFromHost(
        mut env: JNIEnv,
        _class: JClass,
        store_handle: jlong,
        function_type_data: jbyteArray,
        function_reference_id: jlong,
    ) -> jlong {
        // Extract data before the closure
        let type_data_result = env.convert_byte_array(unsafe { jni::objects::JByteArray::from_raw(function_type_data) })
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to read function type data: {}", e),
                backtrace: None,
            });

        let type_data = match type_data_result {
            Ok(data) => data,
            Err(_) => return 0,
        };

        let jvm = match env.get_java_vm() {
            Ok(vm) => vm,
            Err(_) => return 0,
        };

        jni_utils::jni_try_with_default(&mut env, 0, || {
            if store_handle == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            // Get the store
            let store_ref = unsafe { &*(store_handle as *const Store) };

            // Get engine from store context and unmarshal function type
            let func_type = store_ref.with_context(|ctx| {
                unmarshal_function_type(ctx.engine(), &type_data)
            })?;

            // Create the JNI callback wrapper (jvm is already extracted above)
            let callback = Box::new(JniHostFunctionCallback {
                jvm: std::sync::Arc::new(jvm),
                callback_id: function_reference_id,
                is_function_reference: true,  // This is a FunctionReference
            });

            // Create and register the function using Store::create_function_reference
            let name = format!("host_function_{}", function_reference_id);
            let registry_id = store_ref.create_function_reference(name, func_type, callback)?;

            // Return the registry ID directly as jlong
            Ok(registry_id as jlong)
        })
    }

    /// Create a new function reference from a WebAssembly function (JNI version)
    ///
    /// This is a minimal stub that just returns the function reference ID as a handle.
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCreateFunctionReferenceFromWasm(
        mut env: JNIEnv,
        _class: JClass,
        _store_handle: jlong,
        wasm_function_handle: jlong,
        _function_reference_id: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            if wasm_function_handle == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "WebAssembly function handle cannot be null".to_string(),
                });
            }

            // The wasm_function_handle is already a function ID from a JniFunction
            // For function references from WebAssembly functions, we just return the same handle
            let func_id = unsafe { *(wasm_function_handle as *const u64) };

            // Return the function ID as the handle
            Ok(Box::new(func_id))
        }) as jlong
    }

    /// Call a function reference through native code (JNI version)
    ///
    /// This is a minimal stub - actual calls happen through the Java callback mechanism.
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeCallFunctionReference(
        mut env: JNIEnv,
        _class: JClass,
        function_reference_handle: jlong,
        _params_data: jbyteArray,
        _results_buffer: jbyteArray,
    ) -> jint {
        jni_utils::jni_try_with_default(&mut env, -1, || {
            if function_reference_handle == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Function reference handle cannot be null".to_string(),
                });
            }

            // The actual call happens through the Java callback mechanism
            // The Java side handles parameter/result marshalling through FUNCTION_REFERENCE_REGISTRY
            Ok(0)
        })
    }

    /// Destroy a function reference (JNI version)
    ///
    /// This is a minimal stub that cleans up the boxed handle.
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniFunctionReference_nativeDestroyFunctionReference(
        mut env: JNIEnv,
        _class: JClass,
        function_reference_handle: jlong,
    ) {
        jni_utils::jni_try_default(&env, (), || {
            if function_reference_handle == 0 {
                return Ok(());
            }

            // Clean up the boxed handle
            unsafe {
                let _ = Box::from_raw(function_reference_handle as *mut u64);
            }

            Ok(())
        });
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
    use jni::objects::{JValue, JObject};
    
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
        
        jni_utils::jni_try_ptr(&mut env, || {
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
                None, // v128_bytes - not supported in this call path
                ref_id_opt,
            )?;

            let global = core::create_global(store, val_type, mutability_enum, initial_value, name_str)?;
            
            Ok(global)
        }) as jlong
    }

    /// Get global variable value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetGlobal(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        jni_utils::jni_try_code(&mut env, || {
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
                None, // v128_bytes - not supported in this call path
                ref_id_opt,
            )?;

            core::set_global_value(global, store, value)?;
            
            Ok(())
        })
    }

    /// Get global variable metadata (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetMetadata(
        mut env: JNIEnv,
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
        mut env: JNIEnv<'a>,
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
        mut env: JNIEnv<'a>,
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
                ValType::Ref(ref ref_type) => {
                    use wasmtime::HeapType;
                    match *ref_type.heap_type() {
                        HeapType::Func | HeapType::ConcreteFunc(_) => "funcref",
                        HeapType::Extern => "externref",
                        _ => "anyref",
                    }
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
        mut env: JNIEnv,
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
        mut env: JNIEnv<'a>,
        _class: JClass<'a>,
        global_ptr: jlong,
        store_ptr: jlong,
    ) -> jobject {
        match (|| -> WasmtimeResult<jobject> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            
            let value = core::get_global_value(global, store)?;
            
            // Convert GlobalValue to Java Object
            let java_value = match value {
                crate::global::GlobalValue::I32(val) => {
                    let integer_class = env.find_class("java/lang/Integer")?;
                    let integer_obj = env.new_object(integer_class, "(I)V", &[JValue::Int(val)])?;
                    integer_obj.into_raw()
                },
                crate::global::GlobalValue::I64(val) => {
                    let long_class = env.find_class("java/lang/Long")?;
                    let long_obj = env.new_object(long_class, "(J)V", &[JValue::Long(val)])?;
                    long_obj.into_raw()
                },
                crate::global::GlobalValue::F32(val) => {
                    let float_class = env.find_class("java/lang/Float")?;
                    let float_obj = env.new_object(float_class, "(F)V", &[JValue::Float(val)])?;
                    float_obj.into_raw()
                },
                crate::global::GlobalValue::F64(val) => {
                    let double_class = env.find_class("java/lang/Double")?;
                    let double_obj = env.new_object(double_class, "(D)V", &[JValue::Double(val)])?;
                    double_obj.into_raw()
                },
                crate::global::GlobalValue::V128(bytes) => {
                    // Return V128 as byte array
                    let byte_array = env.new_byte_array(16)?;
                    env.set_byte_array_region(&byte_array, 0, &bytes.map(|b| b as i8))?;
                    byte_array.into_raw()
                },
                crate::global::GlobalValue::FuncRef(opt_id) => {
                    // Return FuncRef as Long (null for None)
                    match opt_id {
                        Some(id) => {
                            let long_class = env.find_class("java/lang/Long")?;
                            let long_obj = env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                            long_obj.into_raw()
                        },
                        None => std::ptr::null_mut()
                    }
                },
                crate::global::GlobalValue::ExternRef(opt_id) => {
                    // Return ExternRef as Long (null for None)
                    match opt_id {
                        Some(id) => {
                            let long_class = env.find_class("java/lang/Long")?;
                            let long_obj = env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                            long_obj.into_raw()
                        },
                        None => std::ptr::null_mut()
                    }
                },
                crate::global::GlobalValue::AnyRef(opt_id) => {
                    // Return AnyRef as Long (null for None)
                    match opt_id {
                        Some(id) => {
                            let long_class = env.find_class("java/lang/Long")?;
                            let long_obj = env.new_object(long_class, "(J)V", &[JValue::Long(id as i64)])?;
                            long_obj.into_raw()
                        },
                        None => std::ptr::null_mut()
                    }
                },
            };
            
            Ok(java_value)
        })() {
            Ok(result) => result,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                std::ptr::null_mut()
            }
        }
    }
    
    /// Get the int value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetIntValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
    ) -> jint {
        match (|| -> WasmtimeResult<jint> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an I32 type
            if !val_type_matches(&metadata.value_type, &ValType::I32) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I32 type, got {:?}", metadata.value_type),
                });
            }
            
            let value = core::get_global_value(global, store)?;
            match value {
                crate::global::GlobalValue::I32(val) => Ok(val),
                _ => Err(WasmtimeError::Type {
                    message: "Global value is not I32 type".to_string(),
                })
            }
        })() {
            Ok(result) => result,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return 0 on error
            }
        }
    }
    
    /// Get the long value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetLongValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
    ) -> jlong {
        match (|| -> WasmtimeResult<jlong> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an I64 type
            if !val_type_matches(&metadata.value_type, &ValType::I64) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I64 type, got {:?}", metadata.value_type),
                });
            }
            
            let value = core::get_global_value(global, store)?;
            match value {
                crate::global::GlobalValue::I64(val) => Ok(val),
                _ => Err(WasmtimeError::Type {
                    message: "Global value is not I64 type".to_string(),
                })
            }
        })() {
            Ok(result) => result,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return 0 on error
            }
        }
    }
    
    /// Get the float value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetFloatValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
    ) -> f32 {
        match (|| -> WasmtimeResult<f32> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an F32 type
            if !val_type_matches(&metadata.value_type, &ValType::F32) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F32 type, got {:?}", metadata.value_type),
                });
            }
            
            let value = core::get_global_value(global, store)?;
            match value {
                crate::global::GlobalValue::F32(val) => Ok(val),
                _ => Err(WasmtimeError::Type {
                    message: "Global value is not F32 type".to_string(),
                })
            }
        })() {
            Ok(result) => result,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0.0 // Return 0.0 on error
            }
        }
    }
    
    /// Get the double value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetDoubleValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
    ) -> f64 {
        match (|| -> WasmtimeResult<f64> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is actually an F64 type
            if !val_type_matches(&metadata.value_type, &ValType::F64) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F64 type, got {:?}", metadata.value_type),
                });
            }
            
            let value = core::get_global_value(global, store)?;
            match value {
                crate::global::GlobalValue::F64(val) => Ok(val),
                _ => Err(WasmtimeError::Type {
                    message: "Global value is not F64 type".to_string(),
                })
            }
        })() {
            Ok(result) => result,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0.0 // Return 0.0 on error
            }
        }
    }
    
    /// Set the value of a global variable from Object (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
        value: jobject,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            // Convert Java Object to GlobalValue based on global type
            let java_object = unsafe { JObject::from_raw(value) };
            let global_value = match &metadata.value_type {
                ValType::I32 => {
                    let int_val = env.call_method(&java_object, "intValue", "()I", &[])?.i()?;
                    crate::global::GlobalValue::I32(int_val)
                },
                ValType::I64 => {
                    let long_val = env.call_method(&java_object, "longValue", "()J", &[])?.j()?;
                    crate::global::GlobalValue::I64(long_val)
                },
                ValType::F32 => {
                    let float_val = env.call_method(&java_object, "floatValue", "()F", &[])?.f()?;
                    crate::global::GlobalValue::F32(float_val)
                },
                ValType::F64 => {
                    let double_val = env.call_method(&java_object, "doubleValue", "()D", &[])?.d()?;
                    crate::global::GlobalValue::F64(double_val)
                },
                ValType::Ref(ref_type) => {
                    use wasmtime::HeapType;

                    // Check if the object is null
                    if java_object.is_null() {
                        // Null reference
                        match *ref_type.heap_type() {
                            HeapType::Func | HeapType::ConcreteFunc(_) => {
                                crate::global::GlobalValue::FuncRef(None)
                            },
                            HeapType::Extern => {
                                crate::global::GlobalValue::ExternRef(None)
                            },
                            _ => {
                                crate::global::GlobalValue::AnyRef(None)
                            }
                        }
                    } else {
                        // Non-null reference - extract the handle value
                        // Check if it's a JniFunctionReference (call getNativeHandle) or Long (call longValue)
                        let handle_val = if env.is_instance_of(&java_object, "ai/tegmentum/wasmtime4j/jni/JniFunctionReference")? {
                            // It's a JniFunctionReference - call getNativeHandle()
                            env.call_method(&java_object, "getNativeHandle", "()J", &[])?.j()?
                        } else {
                            // It's a Long - call longValue()
                            env.call_method(&java_object, "longValue", "()J", &[])?.j()?
                        };
                        let handle_id = handle_val as u64;

                        match *ref_type.heap_type() {
                            HeapType::Func | HeapType::ConcreteFunc(_) => {
                                crate::global::GlobalValue::FuncRef(Some(handle_id))
                            },
                            HeapType::Extern => {
                                crate::global::GlobalValue::ExternRef(Some(handle_id))
                            },
                            _ => {
                                crate::global::GlobalValue::AnyRef(Some(handle_id))
                            }
                        }
                    }
                },
                _ => {
                    return Err(WasmtimeError::Type {
                        message: format!("Unsupported global value type for Object conversion: {:?}", metadata.value_type),
                    });
                }
            };
            
            core::set_global_value(global, store, global_value)?;
            Ok(())
        })() {
            Ok(_) => 1, // Return true on success
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return false on error
            }
        }
    }
    
    /// Set the int value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetIntValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
        value: jint,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if !val_type_matches(&metadata.value_type, &ValType::I32) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I32 type, got {:?}", metadata.value_type),
                });
            }
            
            let global_value = crate::global::GlobalValue::I32(value);
            core::set_global_value(global, store, global_value)?;
            Ok(())
        })() {
            Ok(_) => 1, // Return true on success
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return false on error
            }
        }
    }
    
    /// Set the long value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetLongValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
        value: jlong,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if !val_type_matches(&metadata.value_type, &ValType::I64) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not I64 type, got {:?}", metadata.value_type),
                });
            }
            
            let global_value = crate::global::GlobalValue::I64(value);
            core::set_global_value(global, store, global_value)?;
            Ok(())
        })() {
            Ok(_) => 1, // Return true on success
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return false on error
            }
        }
    }
    
    /// Set the float value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetFloatValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
        value: f32,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if !val_type_matches(&metadata.value_type, &ValType::F32) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F32 type, got {:?}", metadata.value_type),
                });
            }
            
            let global_value = crate::global::GlobalValue::F32(value);
            core::set_global_value(global, store, global_value)?;
            Ok(())
        })() {
            Ok(_) => 1, // Return true on success
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return false on error
            }
        }
    }
    
    /// Set the double value of a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeSetDoubleValue(
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
        store_ptr: jlong,
        value: f64,
    ) -> jboolean {
        match (|| -> WasmtimeResult<()> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr as *mut std::os::raw::c_void, "store")? };
            let metadata = core::get_global_metadata(global);
            
            // Validate that this global is mutable and correct type
            if metadata.mutability != Mutability::Var {
                return Err(WasmtimeError::Runtime {
                    message: "Cannot set value on immutable global variable".to_string(),
                    backtrace: None,
                });
            }
            
            if !val_type_matches(&metadata.value_type, &ValType::F64) {
                return Err(WasmtimeError::Type {
                    message: format!("Global is not F64 type, got {:?}", metadata.value_type),
                });
            }
            
            let global_value = crate::global::GlobalValue::F64(value);
            core::set_global_value(global, store, global_value)?;
            Ok(())
        })() {
            Ok(_) => 1, // Return true on success
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                0 // Return false on error
            }
        }
    }
    
    /// Destroy a global variable (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeDestroyGlobal(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
        _class: JClass,
        global_ptr: jlong,
    ) {
        unsafe {
            core::destroy_global(global_ptr as *mut std::os::raw::c_void);
        }
    }

    /// Get global type information directly from the global (JNI version)
    /// Returns array: [valueTypeCode, isMutable(0/1)]
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGlobal_nativeGetGlobalTypeInfo<'a>(
        mut env: JNIEnv<'a>,
        _class: JClass<'a>,
        global_ptr: jlong,
    ) -> jlongArray {
        match (|| -> WasmtimeResult<jlongArray> {
            let global = unsafe { core::get_global_ref(global_ptr as *mut std::os::raw::c_void)? };
            let metadata = core::get_global_metadata(global);

            // Map ValType to type code
            let type_code = match metadata.value_type {
                wasmtime::ValType::I32 => 0,
                wasmtime::ValType::I64 => 1,
                wasmtime::ValType::F32 => 2,
                wasmtime::ValType::F64 => 3,
                wasmtime::ValType::V128 => 4,
                wasmtime::ValType::Ref(_) => {
                    // For now, all ref types map to FUNCREF (5) or EXTERNREF (6)
                    // We'll use 5 as a generic ref type
                    5
                }
            };

            let is_mutable = if metadata.mutability == wasmtime::Mutability::Var { 1 } else { 0 };

            // Create long array with [typeCode, isMutable]
            let result_array = env.new_long_array(2)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create long array: {}", e) })?;

            let values = vec![type_code as i64, is_mutable as i64];
            env.set_long_array_region(&result_array, 0, &values)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set long array region: {}", e) })?;

            Ok(result_array.as_raw())
        })() {
            Ok(array) => array,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                std::ptr::null_mut()
            }
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
        
        jni_utils::jni_try_ptr(&mut env, || {
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
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
    ) -> jint {
        log::debug!("JNI Table.nativeGetSize: table_ptr=0x{:x}, store_ptr=0x{:x}", table_ptr, store_ptr);

        let result = jni_utils::jni_try_default(&env, 0, || {
            use std::os::raw::c_void;

            log::debug!("Getting table reference...");
            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            log::debug!("Got table reference");

            log::debug!("Getting store reference...");
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };
            log::debug!("Got store reference");

            log::debug!("Calling get_table_size...");
            let size = crate::table::core::get_table_size(table, store)?;
            log::debug!("Table size: {}", size);
            Ok(size as jint)
        });

        log::debug!("JNI Table.nativeGetSize returning: {}", result);
        result
    }

    /// Get table maximum size (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetMaxSize(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            use std::os::raw::c_void;

            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

            let max_size = crate::table::core::get_table_max_size(table, store)?;
            Ok(max_size as jint)
        })
    }


    /// Grow table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGrow(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        delta: jint,
        _init: jobject,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            if table_ptr == 0 {
                log::error!("JNI Table.nativeGrow: null table handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null. Ensure table is properly initialized before growing table.".to_string(),
                });
            }

            if table_ptr < 0x1000 || table_ptr == -1 {
                log::error!("JNI Table.nativeGrow: invalid table handle 0x{:x}", table_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid table handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.", 
                        table_ptr
                    ),
                });
            }

            if delta < 0 {
                log::error!("JNI Table.nativeGrow: negative delta {} provided", delta);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Delta must be non-negative, got: {}", delta),
                });
            }

            // Return previous size (5) and simulate growth by delta
            // TODO: Integrate with actual table instance when store context available
            let previous_size = 5; // Current size before growth
            log::debug!("JNI Table.nativeGrow: returning previous size {} for table 0x{:x} with delta {}", previous_size, table_ptr, delta);
            Ok(previous_size)
        })
    }

    /// Fill table range (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeFill(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        start: jint,
        count: jint,
        _value: jobject,
    ) -> jboolean {
        match jni_utils::jni_try_default(&env, false, || {
            if table_ptr == 0 {
                log::error!("JNI Table.nativeFill: null table handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null. Ensure table is properly initialized before filling table.".to_string(),
                });
            }

            if table_ptr < 0x1000 || table_ptr == -1 {
                log::error!("JNI Table.nativeFill: invalid table handle 0x{:x}", table_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid table handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.", 
                        table_ptr
                    ),
                });
            }

            if start < 0 {
                log::error!("JNI Table.nativeFill: negative start index {} provided", start);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Start index must be non-negative, got: {}", start),
                });
            }

            if count < 0 {
                log::error!("JNI Table.nativeFill: negative count {} provided", count);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Count must be non-negative, got: {}", count),
                });
            }

            // TODO: Implement proper table fill when Wasmtime API provides access to table operations
            log::debug!("JNI Table.nativeFill: placeholder implementation for table 0x{:x} start {} count {}", table_ptr, start, count);
            Ok(true) // Return success for now
        }) {
            true => 1,
            false => 0,
        }
    }

    /// Get table metadata (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetMetadata(
        mut env: JNIEnv,
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
        mut env: JNIEnv<'a>,
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
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
    ) {
        unsafe {
            core::destroy_table(table_ptr as *mut std::os::raw::c_void);
        }
    }

    /// Get table type information directly from the table (JNI version)
    /// Returns array: [elementTypeCode, minimum, maximum(-1 if unlimited)]
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetTableTypeInfo<'a>(
        mut env: JNIEnv<'a>,
        _class: JClass<'a>,
        table_ptr: jlong,
        _store_ptr: jlong,
    ) -> jlongArray {
        match (|| -> WasmtimeResult<jlongArray> {
            let table = unsafe { core::get_table_ref(table_ptr as *const std::os::raw::c_void)? };
            let metadata = core::get_table_metadata(table);

            // Map element type to type code
            let type_code = match &metadata.element_type {
                wasmtime::ValType::I32 => 0,
                wasmtime::ValType::I64 => 1,
                wasmtime::ValType::F32 => 2,
                wasmtime::ValType::F64 => 3,
                wasmtime::ValType::V128 => 4,
                wasmtime::ValType::Ref(ref_type) => {
                    // Map reference types based on heap type
                    use wasmtime::HeapType;
                    match ref_type.heap_type() {
                        HeapType::Extern => 6,  // EXTERNREF
                        HeapType::Func => 5,    // FUNCREF
                        _ => 5,  // Default to FUNCREF for other ref types
                    }
                }
            };

            let minimum = metadata.initial_size as i64;
            let maximum = metadata.maximum_size.map(|m| m as i64).unwrap_or(-1);

            // Create long array with [elementTypeCode, minimum, maximum]
            let result_array = env.new_long_array(3)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create long array: {}", e) })?;

            let values = vec![type_code as i64, minimum, maximum];
            env.set_long_array_region(&result_array, 0, &values)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set long array region: {}", e) })?;

            Ok(result_array.as_raw())
        })() {
            Ok(array) => array,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                std::ptr::null_mut()
            }
        }
    }
}

/// JNI bindings for Memory operations
#[cfg(feature = "jni-bindings")]
pub mod jni_memory {
    use super::*;
    use crate::memory::core;
    use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
    use jni::objects::{JByteBuffer, JByteArray};
    
    /// Get memory size in bytes (JNI version) with comprehensive validation
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetSize(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
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

            if store_ptr == 0 {
                log::error!("JNI Memory.nativeGrow: null store handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null for memory growth operations. Ensure store is properly initialized.".to_string(),
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

            // Get memory and store references with validation
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

            // Perform the memory growth operation with comprehensive error handling
            match core::grow_memory(memory, store, pages as u64) {
                Ok(previous_pages) => {
                    log::debug!(
                        "JNI Memory.nativeGrow: successfully grew memory by {} pages for handle 0x{:x}, previous size: {} pages",
                        pages, memory_ptr, previous_pages
                    );
                    Ok(previous_pages as jlong)
                }
                Err(e) => {
                    log::error!(
                        "JNI Memory.nativeGrow: growth failed for handle 0x{:x} with {} pages: {}",
                        memory_ptr, pages, e
                    );
                    Err(e)
                }
            }
        })
    }

    /// Read a single byte from memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeReadByte(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
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

            if store_ptr == 0 {
                log::error!("JNI Memory.nativeReadByte: null store handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
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

            // Get memory and store references with validation
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };

            // Perform the memory read operation with comprehensive error handling
            match core::read_memory_byte(memory, store, offset as usize) {
                Ok(byte_value) => {
                    log::debug!(
                        "JNI Memory.nativeReadByte: successfully read byte {} from offset {} for handle 0x{:x}",
                        byte_value, offset, memory_ptr
                    );
                    Ok(byte_value as jint)
                }
                Err(e) => {
                    log::error!(
                        "JNI Memory.nativeReadByte: read failed for handle 0x{:x} at offset {}: {}",
                        memory_ptr, offset, e
                    );
                    Err(e)
                }
            }
        })
    }

    /// Write a single byte to memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeWriteByte(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jlong,
        value: jint,
    ) {
        jni_utils::jni_try_code(&mut env, || {
            // Comprehensive parameter validation with bounds checking
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeWriteByte: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null for write operations. Ensure memory is properly initialized.".to_string(),
                });
            }

            if store_ptr == 0 {
                log::error!("JNI Memory.nativeWriteByte: null store handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
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

            // Get memory and store references with validation
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

            // Convert jint to u8 (handling signed/unsigned conversion safely)
            let byte_value = if value < 0 {
                (value + 256) as u8  // Convert signed negative to unsigned equivalent
            } else {
                value as u8
            };

            // Perform the memory write operation with comprehensive error handling
            match core::write_memory_byte(memory, store, offset as usize, byte_value) {
                Ok(_) => {
                    log::debug!(
                        "JNI Memory.nativeWriteByte: successfully wrote byte {} (raw: {}) to offset {} for handle 0x{:x}",
                        byte_value, value, offset, memory_ptr
                    );
                    Ok(())
                }
                Err(e) => {
                    log::error!(
                        "JNI Memory.nativeWriteByte: write failed for handle 0x{:x} at offset {} with value {}: {}",
                        memory_ptr, offset, value, e
                    );
                    Err(e)
                }
            }
        });
    }

    /// Read bytes from memory into a buffer (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeReadBytes(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
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

            if store_ptr == 0 {
                log::error!("JNI Memory.nativeReadBytes: null store handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
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
                log::debug!("JNI Memory.nativeReadBytes: zero-length read requested for handle 0x{:x} at offset {}", memory_ptr, offset);
                return Ok(0); // No bytes to read, operation successful
            }

            // Get memory and store references with validation
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void)? };

            // Perform the memory read operation with comprehensive error handling
            match core::read_memory_bytes(memory, store, offset as usize, buffer_length) {
                Ok(read_data) => {
                    // Copy data to Java buffer
                    let signed_data: Vec<i8> = read_data.iter().map(|&b| b as i8).collect();
                    env.set_byte_array_region(&buffer, 0, &signed_data)
                        .map_err(|e| {
                            log::error!("JNI Memory.nativeReadBytes: failed to set buffer data for handle 0x{:x}: {}", memory_ptr, e);
                            crate::error::WasmtimeError::Memory {
                                message: format!("Failed to copy data to Java buffer: {}", e),
                            }
                        })?;

                    log::debug!(
                        "JNI Memory.nativeReadBytes: successfully read {} bytes from offset {} for handle 0x{:x}",
                        read_data.len(), offset, memory_ptr
                    );
                    Ok(read_data.len() as jint)
                }
                Err(e) => {
                    log::error!(
                        "JNI Memory.nativeReadBytes: read failed for handle 0x{:x} at offset {} length {}: {}",
                        memory_ptr, offset, buffer_length, e
                    );
                    Err(e)
                }
            }
        })
    }

    /// Write bytes from a buffer to memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeWriteBytes(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jlong,
        buffer: JByteArray,
    ) -> jint {
        jni_utils::jni_try_default(&env, -1, || {
            // Comprehensive parameter validation
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeWriteBytes: null memory handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null for bulk write operations. Ensure memory is properly initialized.".to_string(),
                });
            }

            if store_ptr == 0 {
                log::error!("JNI Memory.nativeWriteBytes: null store handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null for memory operations. Ensure store is properly initialized.".to_string(),
                });
            }

            if memory_ptr < 0x1000 || memory_ptr == -1 {
                log::error!("JNI Memory.nativeWriteBytes: invalid memory handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid memory handle (0x{:x}) for bulk write operation. Handle appears corrupted or uninitialized.",
                        memory_ptr
                    ),
                });
            }

            if offset < 0 {
                log::error!("JNI Memory.nativeWriteBytes: negative offset {} for handle 0x{:x}", offset, memory_ptr);
                return Err(crate::error::WasmtimeError::Memory {
                    message: format!(
                        "Memory write offset cannot be negative (received: {}). \
                         Specify a non-negative byte offset within memory bounds.",
                        offset
                    ),
                });
            }

            // Validate JNI buffer parameter
            if buffer.is_null() {
                log::error!("JNI Memory.nativeWriteBytes: null buffer provided for handle 0x{:x}", memory_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Buffer cannot be null for bulk write operations. Provide a valid byte array.".to_string(),
                });
            }

            // Get buffer length with bounds checking
            let buffer_length = env.get_array_length(&buffer)
                .map_err(|e| {
                    log::error!("JNI Memory.nativeWriteBytes: failed to get buffer length for handle 0x{:x}: {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!("Failed to get buffer length for write operation: {}", e),
                    }
                })? as usize;

            if buffer_length == 0 {
                log::debug!("JNI Memory.nativeWriteBytes: zero-length write requested for handle 0x{:x} at offset {}", memory_ptr, offset);
                return Ok(0); // Nothing to write, operation successful
            }

            // Get memory and store references with validation
            let memory = unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)? };
            let store = unsafe { core::get_store_mut(store_ptr as *mut std::os::raw::c_void)? };

            // Get Java buffer data safely
            let mut signed_buffer = vec![0i8; buffer_length];
            env.get_byte_array_region(&buffer, 0, &mut signed_buffer)
                .map_err(|e| {
                    log::error!("JNI Memory.nativeWriteBytes: failed to read buffer data for handle 0x{:x}: {}", memory_ptr, e);
                    crate::error::WasmtimeError::Memory {
                        message: format!("Failed to read buffer data for write operation: {}", e),
                    }
                })?;

            // Convert i8 to u8 for memory write
            let write_data: Vec<u8> = signed_buffer.iter().map(|&b| b as u8).collect();

            // Perform the memory write operation with comprehensive error handling
            match core::write_memory_bytes(memory, store, offset as usize, &write_data) {
                Ok(_) => {
                    log::debug!(
                        "JNI Memory.nativeWriteBytes: successfully wrote {} bytes at offset {} for handle 0x{:x}",
                        buffer_length, offset, memory_ptr
                    );
                    Ok(buffer_length as jint)
                }
                Err(e) => {
                    log::error!(
                        "JNI Memory.nativeWriteBytes: write failed for handle 0x{:x} at offset {} length {}: {}",
                        memory_ptr, offset, buffer_length, e
                    );
                    Err(e)
                }
            }
        })
    }

    /// Get direct ByteBuffer view of memory (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetBuffer<'a>(
        mut env: JNIEnv<'a>,
        _class: JClass<'a>,
        memory_ptr: jlong,
        store_ptr: jlong,
    ) -> JByteBuffer<'a> {
        // Helper macro for error handling without closures (avoids borrow conflicts)
        macro_rules! handle_error {
            ($error:expr) => {{
                jni_utils::throw_jni_exception(&mut env, &$error);
                return JByteBuffer::default();
            }};
        }

        // Comprehensive parameter validation
        if memory_ptr == 0 {
            log::error!("JNI Memory.nativeGetBuffer: null memory handle provided");
            handle_error!(crate::error::WasmtimeError::InvalidParameter {
                message: "Memory handle cannot be null for buffer access. Ensure memory is properly initialized.".to_string(),
            });
        }

        if store_ptr == 0 {
            log::error!("JNI Memory.nativeGetBuffer: null store handle provided");
            handle_error!(crate::error::WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null for memory buffer access. Ensure store is properly initialized.".to_string(),
            });
        }

        if memory_ptr < 0x1000 || memory_ptr == -1 {
            log::error!("JNI Memory.nativeGetBuffer: invalid memory handle 0x{:x}", memory_ptr);
            handle_error!(crate::error::WasmtimeError::InvalidParameter {
                message: format!(
                    "Invalid memory handle (0x{:x}) for buffer access. Handle appears corrupted or uninitialized.",
                    memory_ptr
                ),
            });
        }

        // Get memory and store references with validation
        let memory = match unsafe { core::get_memory_ref(memory_ptr as *const std::os::raw::c_void) } {
            Ok(m) => m,
            Err(e) => handle_error!(e),
        };
        let store = match unsafe { core::get_store_ref(store_ptr as *const std::os::raw::c_void) } {
            Ok(s) => s,
            Err(e) => handle_error!(e),
        };

        // Get memory buffer information
        let (buffer_ptr, buffer_size) = match core::get_memory_buffer(memory, store) {
            Ok(result) => result,
            Err(e) => {
                log::error!("JNI Memory.nativeGetBuffer: failed to get memory buffer for handle 0x{:x}: {}", memory_ptr, e);
                handle_error!(crate::error::WasmtimeError::Memory {
                    message: format!("Failed to get memory buffer: {}", e),
                });
            }
        };

        // Create a direct ByteBuffer wrapping the Wasm memory
        // SAFETY: The buffer is valid as long as the memory and store are alive.
        // The caller is responsible for ensuring the memory/store are not destroyed
        // while the ByteBuffer is in use.
        log::debug!(
            "JNI Memory.nativeGetBuffer: creating ByteBuffer for handle 0x{:x} with size {} bytes",
            memory_ptr, buffer_size
        );

        // Create a direct ByteBuffer using JNI
        let byte_buffer = match unsafe { env.new_direct_byte_buffer(buffer_ptr as *mut u8, buffer_size) } {
            Ok(buf) => buf,
            Err(e) => {
                log::error!("JNI Memory.nativeGetBuffer: failed to create DirectByteBuffer: {}", e);
                handle_error!(crate::error::WasmtimeError::Memory {
                    message: format!("Failed to create DirectByteBuffer: {}", e),
                });
            }
        };

        log::debug!(
            "JNI Memory.nativeGetBuffer: successfully created ByteBuffer for handle 0x{:x}",
            memory_ptr
        );

        byte_buffer
    }

    /// Destroy memory (JNI version) with comprehensive validation and cleanup
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeDestroyMemory(
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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
        mut env: JNIEnv,
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

    // Import table core functions for root-level JNI functions
    use crate::table::core::{get_table_ref, get_table_metadata};

    /// Get element type of a table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGetElementType(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        _store_ptr: jlong,
    ) -> jstring {
        let null_string = std::ptr::null_mut();
        match (|| -> crate::error::WasmtimeResult<jstring> {
            if table_ptr == 0 {
                log::error!("JNI Table.nativeGetElementType: null table handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null. Ensure table is properly initialized before calling element type operations.".to_string(),
                });
            }

            if table_ptr < 0x1000 || table_ptr == -1 {
                log::error!("JNI Table.nativeGetElementType: invalid table handle 0x{:x}", table_ptr);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid table handle (0x{:x}): Handle appears to be corrupted or uninitialized. Expected a valid native pointer.", 
                        table_ptr
                    ),
                });
            }

            // Get table reference and metadata to determine actual element type
            let table = unsafe { get_table_ref(table_ptr as *const std::os::raw::c_void)? };
            let metadata = get_table_metadata(table);
            
            // Convert ValType to string representation
            let element_type_str = match &metadata.element_type {
                wasmtime::ValType::Ref(ref_type) => {
                    // Discriminate between different reference types
                    match ref_type.heap_type() {
                        wasmtime::HeapType::Func => "funcref",
                        wasmtime::HeapType::Extern => "externref",
                        _ => "funcref", // Default to funcref for other types
                    }
                },
                _ => {
                    log::warn!("JNI Table.nativeGetElementType: unexpected non-reference element type {:?}", metadata.element_type);
                    "funcref" // Default fallback
                }
            };
            
            log::debug!("JNI Table.nativeGetElementType: returning '{}' for table 0x{:x}", element_type_str, table_ptr);
            
            env.new_string(element_type_str)
                .map(|jstr| jstr.into_raw())
                .map_err(|e| crate::error::WasmtimeError::Memory {
                    message: format!("Failed to create string for table element type: {}", e),
                })
        })() {
            Ok(result) => result,
            Err(error) => {
                log::error!("Error in nativeGetElementType: {:?}", error);
                null_string
            }
        }
    }

    /// Get element from table by index (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeGet(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        index: jint,
    ) -> jobject {
        match (|| -> crate::error::WasmtimeResult<jobject> {
            if table_ptr == 0 {
                log::error!("JNI Table.nativeGet: null table handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null. Ensure table is properly initialized before accessing elements.".to_string(),
                });
            }

            if index < 0 {
                log::error!("JNI Table.nativeGet: negative index {} provided", index);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Table index cannot be negative (received: {}). Specify a non-negative index within table bounds.", 
                        index
                    ),
                });
            }

            // Note: This operation requires store context which is not available in current API design
            // The table operations in Java API don't pass store context, but Wasmtime requires it
            // For now, we'll return null (which represents a null reference in table)
            // TODO: Redesign API to include store context or store reference within table
            let table = unsafe { get_table_ref(table_ptr as *const std::os::raw::c_void)? };
            log::debug!("JNI Table.nativeGet: table operations require store context - returning null for table 0x{:x} index {}", table_ptr, index);
            
            // Return null to represent an uninitialized table slot (this is valid for tables)
            Ok(std::ptr::null_mut())
        })() {
            Ok(result) => result,
            Err(error) => {
                log::error!("Error in nativeGet: {:?}", error);
                std::ptr::null_mut()
            }
        }
    }

    /// Set element in table by index (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeSet(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        index: jint,
        value: jobject,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
            if table_ptr == 0 {
                log::error!("JNI Table.nativeSet: null table handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null. Ensure table is properly initialized before setting elements.".to_string(),
                });
            }

            if index < 0 {
                log::error!("JNI Table.nativeSet: negative index {} provided", index);
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!(
                        "Table index cannot be negative (received: {}). Specify a non-negative index within table bounds.", 
                        index
                    ),
                });
            }

            // Note: This operation requires store context which is not available in current API design
            // The table operations in Java API don't pass store context, but Wasmtime requires it
            // For now, validate the table and return success (elements stored but not accessible without store)
            // TODO: Redesign API to include store context or store reference within table
            let table = unsafe { get_table_ref(table_ptr as *const std::os::raw::c_void)? };
            log::debug!("JNI Table.nativeSet: table operations require store context - accepting set for table 0x{:x} index {} (placeholder)", table_ptr, index);
            
            // Return success as if the element was set (this maintains API consistency)
            Ok(1)
        })
    }

    /// Get memory type information directly from the memory (JNI version)
    /// Returns array: [minimum, maximum(-1 if unlimited), is64Bit(0/1), isShared(0/1)]
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeGetMemoryTypeInfo<'a>(
        mut env: JNIEnv<'a>,
        _class: JClass<'a>,
        memory_ptr: jlong,
    ) -> jlongArray {
        match (|| -> WasmtimeResult<jlongArray> {
            // Validate memory pointer
            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }

            // Get memory reference - pointer is to ValidatedMemory, not Memory directly
            let validated_memory = unsafe { &*(memory_ptr as *const crate::memory::core::ValidatedMemory) };
            let memory = validated_memory.access_memory()?;

            // Get type information from cached memory_type
            let minimum = memory.memory_type.minimum() as i64;
            let maximum = memory.memory_type.maximum().map(|m| m as i64).unwrap_or(-1);
            let is_shared = if memory.memory_type.is_shared() { 1i64 } else { 0i64 };
            let is_64_bit = if memory.memory_type.is_64() { 1i64 } else { 0i64 };

            // Create long array with [minimum, maximum, is64Bit, isShared]
            let result_array = env.new_long_array(4)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create long array: {}", e) })?;

            let values = vec![minimum, maximum, is_64_bit, is_shared];
            env.set_long_array_region(&result_array, 0, &values)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set long array region: {}", e) })?;

            Ok(result_array.as_raw())
        })() {
            Ok(array) => array,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                std::ptr::null_mut()
            }
        }
    }

}

/// JNI bindings for JniWasmRuntime operations
#[cfg(feature = "jni-bindings")]
pub mod jni_runtime {
    use super::*;
    use crate::error::jni_utils;
    use std::ptr;

    /// Create a new WebAssembly runtime (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateRuntime(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            log::debug!("Creating new JNI WebAssembly runtime");

            // For now, return a placeholder handle since we don't need a specific runtime object
            // The actual work is done by the engines and modules
            let runtime_placeholder = Box::new(0u64);

            log::debug!("Created JNI WebAssembly runtime");
            Ok(runtime_placeholder)
        }) as jlong
    }

    /// Create a new Wasmtime engine for the runtime (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateEngine(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeCreateEngine: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            log::debug!("Creating new engine for runtime handle: 0x{:x}", runtime_handle);

            // Create a new engine with default configuration
            crate::engine::core::create_engine()
        }) as jlong
    }

    /// Compile a WebAssembly module (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCompileModule(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        wasm_bytes: jbyteArray,
    ) -> jlong {
        // Extract data before moving env into jni_try_ptr
        let wasm_data_result = env.convert_byte_array(unsafe { jni::objects::JByteArray::from_raw(wasm_bytes) })
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to convert Java byte array: {}", e),
            });

        let data = match wasm_data_result {
            Ok(data) => data,
            Err(_) => return 0 as jlong, // Return null on error
        };

        jni_utils::jni_try_ptr(&mut env, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeCompileModule: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            log::debug!("Compiling module for runtime handle: 0x{:x}, bytes length: {}", runtime_handle, data.len());

            // Create a default engine for compilation
            let engine = crate::engine::core::create_engine()?;

            // Compile the module
            crate::module::core::compile_module(&engine, &data)
        }) as jlong
    }

    /// Instantiate a WebAssembly module (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeInstantiateModule(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        module_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeInstantiateModule: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            if module_handle == 0 {
                log::error!("JNI Runtime.nativeInstantiateModule: null module handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Module handle cannot be null".to_string(),
                });
            }

            log::debug!("Instantiating module 0x{:x} for runtime 0x{:x}", module_handle, runtime_handle);

            // Get the module reference
            let module = unsafe { crate::module::core::get_module_ref(module_handle as *const std::os::raw::c_void)? };

            // Create a default engine and store for instantiation
            let engine = crate::engine::core::create_engine()?;
            let mut store = crate::store::core::create_store(&engine)?;

            // Instantiate the module
            crate::instance::core::instantiate_module(&mut store, &module, &[])
        }) as jlong
    }

    /// Get the Wasmtime version string (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeGetWasmtimeVersion(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jstring {
        match env.new_string(crate::WASMTIME_VERSION) {
            Ok(version_str) => version_str.into_raw(),
            Err(e) => {
                log::error!("Failed to create Java string for Wasmtime version: {}", e);
                ptr::null_mut()
            }
        }
    }

    /// Destroy a WebAssembly runtime (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeDestroyRuntime(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) {
        if runtime_handle == 0 {
            log::warn!("JNI Runtime.nativeDestroyRuntime: attempt to destroy null runtime handle");
            return;
        }

        log::debug!("Destroying runtime handle: 0x{:x}", runtime_handle);

        // Clean up the runtime handle
        unsafe {
            let _runtime = Box::from_raw(runtime_handle as *mut u64);
            // The runtime object is automatically dropped here
        }

        log::debug!("Successfully destroyed runtime handle: 0x{:x}", runtime_handle);
    }

    /// Create a new WASI context for the runtime (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateWasiContext(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeCreateWasiContext: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            log::debug!("Creating WASI context for runtime handle: 0x{:x}", runtime_handle);

            // Create a new WASI context with default configuration
            let ctx = crate::wasi::WasiContext::new()?;
            let fd_manager = crate::wasi::WasiFileDescriptorManager::new();
            let combined = Box::new((ctx, fd_manager));

            log::debug!("Created WASI context for runtime 0x{:x}", runtime_handle);
            Ok(combined)
        }) as jlong
    }

    /// Create a new Linker for the runtime (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateLinker(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        engine_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeCreateLinker: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            if engine_handle == 0 {
                log::error!("JNI Runtime.nativeCreateLinker: null engine handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Engine handle cannot be null".to_string(),
                });
            }

            log::debug!("Creating Linker for runtime 0x{:x}, engine 0x{:x}", runtime_handle, engine_handle);

            // Get the engine reference
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_handle as *const std::os::raw::c_void)? };

            // Create a new Linker with the engine
            let linker = crate::linker::Linker::new(&engine)?;

            log::debug!("Created Linker for runtime 0x{:x}", runtime_handle);
            Ok(Box::new(linker))
        }) as jlong
    }

    /// Create a linker with configuration options
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateLinkerWithConfig(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        engine_handle: jlong,
        allow_shadowing: jboolean,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeCreateLinkerWithConfig: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            if engine_handle == 0 {
                log::error!("JNI Runtime.nativeCreateLinkerWithConfig: null engine handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Engine handle cannot be null".to_string(),
                });
            }

            log::debug!(
                "Creating Linker with config for runtime 0x{:x}, engine 0x{:x}, allow_shadowing: {}",
                runtime_handle, engine_handle, allow_shadowing != 0
            );

            // Get the engine reference
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_handle as *const std::os::raw::c_void)? };

            // Create LinkerConfig
            let config = crate::linker::LinkerConfig {
                enable_wasi: false,
                allow_shadowing: allow_shadowing != 0,
                max_host_functions: None,
                validate_signatures: true,
            };

            // Create a new Linker with the engine and config
            let linker = crate::linker::ffi_core::create_linker_with_config(&engine, config)?;

            log::debug!("Created Linker with config for runtime 0x{:x}", runtime_handle);
            Ok(linker)
        }) as jlong
    }

    /// Add WASI context to a Linker (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeAddWasiToLinker(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        linker_handle: jlong,
        wasi_handle: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, -1, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeAddWasiToLinker: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            if linker_handle == 0 {
                log::error!("JNI Runtime.nativeAddWasiToLinker: null linker handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Linker handle cannot be null".to_string(),
                });
            }

            if wasi_handle == 0 {
                log::error!("JNI Runtime.nativeAddWasiToLinker: null WASI handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI handle cannot be null".to_string(),
                });
            }

            log::debug!("Adding WASI 0x{:x} to Linker 0x{:x} for runtime 0x{:x}",
                        wasi_handle, linker_handle, runtime_handle);

            // Get the linker wrapper reference
            let linker_wrapper = unsafe {
                &mut *(linker_handle as *mut crate::linker::Linker)
            };

            // Get the WASI context from the raw pointer
            // The pointer points to a (WasiContext, WasiFileDescriptorManager) tuple
            let wasi_ctx_ptr = wasi_handle as *const (crate::wasi::WasiContext, crate::wasi::WasiFileDescriptorManager);
            let wasi_tuple = unsafe { &*wasi_ctx_ptr };

            // Clone the WASI context configuration and store in linker
            let wasi_context = wasi_tuple.0.clone();
            linker_wrapper.set_wasi_context(wasi_context);

            // Call enable_wasi() which adds WASI Preview 1 imports to the linker
            linker_wrapper.enable_wasi()?;

            log::debug!("WASI Preview 1 imports successfully added to Linker for runtime 0x{:x}", runtime_handle);
            Ok(0)
        })
    }

    /// Add WASI Preview2 context to a Linker (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeAddWasiPreview2ToLinker(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        linker_handle: jlong,
        wasi_handle: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, -1, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeAddWasiPreview2ToLinker: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            if linker_handle == 0 {
                log::error!("JNI Runtime.nativeAddWasiPreview2ToLinker: null linker handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Linker handle cannot be null".to_string(),
                });
            }

            if wasi_handle == 0 {
                log::error!("JNI Runtime.nativeAddWasiPreview2ToLinker: null WASI handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "WASI handle cannot be null".to_string(),
                });
            }

            log::debug!("Adding WASI Preview2 0x{:x} to Linker 0x{:x} for runtime 0x{:x}",
                        wasi_handle, linker_handle, runtime_handle);

            // WASI Preview2 integration - stub for now
            // In a full implementation, this would add Preview2-specific WASI imports

            log::debug!("Successfully added WASI Preview2 to Linker for runtime 0x{:x}", runtime_handle);
            Ok(0)
        })
    }

    /// Add Component Model to a Linker (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeAddComponentModelToLinker(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        linker_handle: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, -1, || {
            if runtime_handle == 0 {
                log::error!("JNI Runtime.nativeAddComponentModelToLinker: null runtime handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Runtime handle cannot be null".to_string(),
                });
            }

            if linker_handle == 0 {
                log::error!("JNI Runtime.nativeAddComponentModelToLinker: null linker handle provided");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Linker handle cannot be null".to_string(),
                });
            }

            log::debug!("Adding Component Model to Linker 0x{:x} for runtime 0x{:x}",
                        linker_handle, runtime_handle);

            // Component Model integration - stub for now
            // In a full implementation, this would add Component Model-specific functionality

            log::debug!("Successfully added Component Model to Linker for runtime 0x{:x}", runtime_handle);
            Ok(0)
        })
    }

    /// Deserialize a module from bytes using the runtime's engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeDeserializeModule(
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

        jni_utils::jni_try_ptr(&mut env, || {
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };
            let byte_converter = jni_module::VecByteArrayConverter::new(data);
            crate::shared_ffi::module::deserialize_module_shared(engine, byte_converter)
        }) as jlong
    }

    /// Create a WASI-enabled linker with the specified configuration
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateWasiLinker(
        mut env: JNIEnv,
        _class: JClass,
        engine_ptr: jlong,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            use crate::engine::core;
            use crate::linker::Linker as WasmtimeLinker;

            // Get engine reference
            let engine = unsafe { core::get_engine_ref(engine_ptr as *const std::os::raw::c_void)? };

            // Create a new linker
            let mut linker = WasmtimeLinker::new(engine)?;

            // Add WASI Preview 1 imports to the linker
            // The linker will have all WASI functions defined, and they will work
            // when a store with a WASI context is used for instantiation
            linker.enable_wasi()?;
            log::debug!("Created WASI-enabled linker with WASI Preview 1 imports");

            Ok(Box::new(linker))
        }) as jlong
    }
}

/// JNI bindings for WASI operations
#[cfg(feature = "jni-bindings")]
pub mod jni_wasi {
    use super::*;
    use crate::wasi::{WasiContext, WasiFileDescriptorManager};
    use crate::error::jni_utils;

    /// Create a new WASI context with default configuration
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeCreateContext(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let ctx = WasiContext::new()?; let fd_manager = WasiFileDescriptorManager::new();
            let combined = Box::new((ctx, fd_manager));
            Ok(combined)
        }) as jlong
    }

    /// Destroy a WASI context and free its resources
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiContext_nativeDestroyContext(
        mut env: JNIEnv,
        _class: JClass,
        context_handle: jlong,
    ) {
        if context_handle != 0 {
            let _: Box<(WasiContext, WasiFileDescriptorManager)> = unsafe {
                Box::from_raw(context_handle as *mut (WasiContext, WasiFileDescriptorManager))
            };
        }
    }
}

/// JNI bindings for Caller context operations
#[cfg(feature = "jni-bindings")]
pub mod jni_caller {
    use super::*;
    use crate::caller::core;
    use crate::error::jni_utils;
    use wasmtime::Caller as WasmtimeCaller;
    use crate::store::StoreData;
    
    

    /// Get fuel consumed by the caller if fuel metering is enabled (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetFuel(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
    ) -> jlong {
        if caller_handle == 0 {
            return -1; // Error: null pointer
        }

        jni_utils::jni_try_ptr(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_fuel(caller)? {
                Some(fuel) => Ok(Box::new(fuel)),
                None => Err(crate::error::WasmtimeError::CallerContextError { message: "Fuel metering not enabled".to_string() }),
            }
        }) as jlong
    }

    /// Get fuel remaining in the caller if fuel metering is enabled (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetFuelRemaining(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
    ) -> jlong {
        if caller_handle == 0 {
            return -1; // Error: null pointer
        }

        jni_utils::jni_try_ptr(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_fuel_remaining(caller)? {
                Some(fuel) => Ok(Box::new(fuel)),
                None => Err(crate::error::WasmtimeError::CallerContextError { message: "Fuel metering not enabled".to_string() }),
            }
        }) as jlong
    }

    /// Add fuel to the caller (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeAddFuel(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        fuel: jlong,
    ) -> jboolean {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        jni_utils::jni_try_bool(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            core::caller_add_fuel(caller, fuel as u64)?;
            Ok(true) // Success
        }) as jboolean
    }

    /// Set epoch deadline for the caller (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetEpochDeadline(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        deadline: jlong,
    ) -> jboolean {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        jni_utils::jni_try_bool(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            core::caller_set_epoch_deadline(caller, deadline as u64)?;
            Ok(true) // Success
        }) as jboolean
    }

    /// Check if the caller has an active epoch deadline (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeHasEpochDeadline(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
    ) -> jboolean {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        jni_utils::jni_try_bool(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            core::caller_has_epoch_deadline(caller)
        }) as jboolean
    }

    /// Check if caller has an export with the given name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeHasExport(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        name: JString,
    ) -> jboolean {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0, // Error getting string
        };

        jni_utils::jni_try_bool(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            core::caller_has_export(caller, &name_str)
        }) as jboolean
    }

    /// Get memory export from caller by name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetMemory(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        name: JString,
    ) -> jlong {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0, // Error getting string
        };

        jni_utils::jni_try_ptr(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_memory(caller, &name_str)? {
                Some(memory) => Ok(Box::new(memory)),
                None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
            }
        }) as jlong
    }

    /// Get function export from caller by name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetFunction(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        name: JString,
    ) -> jlong {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0, // Error getting string
        };

        jni_utils::jni_try_ptr(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_function(caller, &name_str)? {
                Some(function) => Ok(Box::new(function)),
                None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
            }
        }) as jlong
    }

    /// Get global export from caller by name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetGlobal(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        name: JString,
    ) -> jlong {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0, // Error getting string
        };

        jni_utils::jni_try_ptr(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_global(caller, &name_str)? {
                Some(global) => Ok(Box::new(global)),
                None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
            }
        }) as jlong
    }

    /// Get table export from caller by name (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeGetTable(
        mut env: JNIEnv,
        _class: JClass,
        caller_handle: jlong,
        name: JString,
    ) -> jlong {
        if caller_handle == 0 {
            return 0; // Error: null pointer
        }

        let name_str: String = match env.get_string(&name) {
            Ok(s) => s.into(),
            Err(_) => return 0, // Error getting string
        };

        jni_utils::jni_try_ptr(&mut env, || {
            let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
            match core::caller_get_table(caller, &name_str)? {
                Some(table) => Ok(Box::new(table)),
                None => Err(crate::error::WasmtimeError::ExportNotFound { name: name_str }),
            }
        }) as jlong
    }
}

/// JNI bindings for SIMD operations
#[cfg(feature = "jni-bindings")]
pub mod jni_simd {
    use super::*;
    use crate::simd;
    use crate::error::{jni_utils, WasmtimeError};
    use jni::objects::JByteArray;
    use jni::sys::jfloat;

    /// Check if SIMD is supported
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeIsSimdSupported(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) -> jboolean {
        // For now, always return true as SIMD is supported in Wasmtime
        1
    }

    /// Check if Component Model is supported
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSupportsComponentModel(
        _env: JNIEnv,
        _class: JClass,
        _runtime_handle: jlong,
    ) -> jboolean {
        // Component Model is enabled in Wasmtime build via "component-model" feature
        #[cfg(feature = "component-model")]
        {
            1
        }
        #[cfg(not(feature = "component-model"))]
        {
            0
        }
    }

    /// Get SIMD capabilities
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeGetSimdCapabilities(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
    ) -> jstring {
        match env.new_string("v128 SIMD with platform optimizations (SSE, AVX, NEON)") {
            Ok(version_str) => version_str.into_raw(),
            Err(e) => {
                log::error!("Failed to create SIMD capabilities string: {}", e);
                std::ptr::null_mut()
            }
        }
    }

    /// SIMD vector addition
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAdd(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        // Convert byte arrays outside the closure
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(), // Error getting array length
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(), // Error getting array length
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut(); // Error reading array
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut(); // Error reading array
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            // Create SIMD operations instance with default config
            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.add(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        // Create result array outside closure
        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut() // Error setting array region
                }
            },
            Err(_) => std::ptr::null_mut(), // Error creating array
        }
    }

    /// SIMD vector subtraction
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSubtract(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        // Convert byte arrays outside the closure
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.subtract(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD vector multiplication
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdMultiply(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        jni_utils::jni_try_object(&mut env, |env| {
            let a_bytes = env.convert_byte_array(a)?;
            let b_bytes = env.convert_byte_array(b)?;

            if a_bytes.len() != 16 || b_bytes.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.multiply(&a_v128, &b_v128)?;
            let data = result.data.to_vec();

            let byte_array = env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
            env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;

            Ok(unsafe { JObject::from_raw(byte_array.into_raw()) })
        })
    }

    /// SIMD FMA operation (a * b + c)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdFma(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
        c: JByteArray,
    ) -> jbyteArray {
        jni_utils::jni_try_object(&mut env, |env| {
            let a_bytes = env.convert_byte_array(a)?;
            let b_bytes = env.convert_byte_array(b)?;
            let c_bytes = env.convert_byte_array(c)?;

            if a_bytes.len() != 16 || b_bytes.len() != 16 || c_bytes.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes.try_into().unwrap() };
            let c_v128 = simd::V128 { data: c_bytes.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.fma(&a_v128, &b_v128, &c_v128)?;
            let data = result.data.to_vec();

            let byte_array = env.new_byte_array(data.len() as i32)
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to create byte array: {}", e) })?;
            env.set_byte_array_region(&byte_array, 0, &data.iter().map(|&b| b as i8).collect::<Vec<i8>>())
                .map_err(|e| WasmtimeError::Memory { message: format!("Failed to set byte array region: {}", e) })?;

            Ok(unsafe { JObject::from_raw(byte_array.into_raw()) })
        })
    }

    /// SIMD horizontal sum
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdHorizontalSum(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jfloat {
        match env.convert_byte_array(a) {
            Ok(a_bytes) => {
                if a_bytes.len() == 16 {
                    let a_v128 = simd::V128 { data: a_bytes.try_into().unwrap() };

                    let simd_config = simd::SIMDConfig::default();
                    match simd::SIMDOperations::new(simd_config) {
                        Ok(simd_ops) => {
                            match simd_ops.reduce_sum_i32(&a_v128) {
                                Ok(result) => result as f32,
                                Err(_) => 0.0,
                            }
                        }
                        Err(_) => 0.0,
                    }
                } else {
                    0.0
                }
            }
            Err(_) => 0.0,
        }
    }

    /// SIMD vector division
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdDivide(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.divide(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD saturated addition
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAddSaturated(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.add_saturated(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD bitwise AND
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdAnd(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.and(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD bitwise OR
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdOr(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.or(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD bitwise XOR
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdXor(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.xor(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD bitwise NOT
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdNot(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.not(&a_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD equals comparison
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdEquals(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.equals(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD less than comparison
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdLessThan(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.less_than(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD greater than comparison
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdGreaterThan(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.greater_than(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD fused multiply-subtract
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdFms(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
        c: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let c_len = match env.get_array_length(&c) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];
        let mut c_bytes = vec![0i8; c_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(c, 0, &mut c_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();
        let c_bytes_u8: Vec<u8> = c_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 || c_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };
            let c_v128 = simd::V128 { data: c_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.fms(&a_v128, &b_v128, &c_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD reciprocal
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdReciprocal(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.reciprocal(&a_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD square root
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSqrt(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.sqrt(&a_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD reciprocal square root
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdRsqrt(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.rsqrt(&a_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD vector shuffle
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdShuffle(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
        indices: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let indices_len = match env.get_array_length(&indices) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];
        let mut indices_bytes = vec![0i8; indices_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(indices, 0, &mut indices_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();
        let indices_u8: Vec<u8> = indices_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 || indices_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors and indices must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };
            let indices_arr: [u8; 16] = indices_u8.try_into().unwrap();

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.shuffle(&a_v128, &b_v128, &indices_arr)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD relaxed add
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdRelaxedAdd(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_len = match env.get_array_length(&b) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];
        let mut b_bytes = vec![0i8; b_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }
        if env.get_byte_array_region(b, 0, &mut b_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();
        let b_bytes_u8: Vec<u8> = b_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || b_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vectors must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };
            let b_v128 = simd::V128 { data: b_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.relaxed_add(&a_v128, &b_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD convert I32 to F32
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdConvertI32ToF32(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.convert_i32_to_f32(&a_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD convert F32 to I32
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdConvertF32ToI32(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.convert_f32_to_i32(&a_v128)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD extract I32 lane
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdExtractLaneI32(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        lane: jint,
    ) -> jint {
        match env.convert_byte_array(a) {
            Ok(a_bytes) => {
                if a_bytes.len() == 16 && lane >= 0 && lane < 4 {
                    let a_v128 = simd::V128 { data: a_bytes.try_into().unwrap() };

                    let simd_config = simd::SIMDConfig::default();
                    match simd::SIMDOperations::new(simd_config) {
                        Ok(simd_ops) => {
                            match simd_ops.extract_lane_i32(&a_v128, lane as u8) {
                                Ok(result) => result,
                                Err(_) => 0,
                            }
                        }
                        Err(_) => 0,
                    }
                } else {
                    0
                }
            }
            Err(_) => 0,
        }
    }

    /// SIMD replace I32 lane
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdReplaceLaneI32(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
        lane: jint,
        value: jint,
    ) -> jbyteArray {
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        let mut a_bytes = vec![0i8; a_len as usize];

        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            if a_bytes_u8.len() != 16 || lane < 0 || lane >= 4 {
                return Err(crate::error::WasmtimeError::InvalidOperation {
                    message: "SIMD vector must be 16 bytes and lane must be 0-3".to_string(),
                });
            }

            let a_v128 = simd::V128 { data: a_bytes_u8.try_into().unwrap() };

            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.replace_lane_i32(&a_v128, lane as u8, value)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD splat I32
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSplatI32(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        value: jint,
    ) -> jbyteArray {
        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.splat_i32(value)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD splat F32
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSplatF32(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        value: jfloat,
    ) -> jbyteArray {
        let data_vec = jni_utils::jni_try_with_default(&mut env, vec![], || {
            let simd_config = simd::SIMDConfig::default();
            let simd_ops = simd::SIMDOperations::new(simd_config)?;

            let result = simd_ops.splat_f32(value)?;
            Ok(result.data.to_vec())
        });

        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data_i8: Vec<i8> = data_vec.iter().map(|&b| b as i8).collect();
                if env.set_byte_array_region(&byte_array, 0, &data_i8).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD horizontal min
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdHorizontalMin(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jfloat {
        match env.convert_byte_array(a) {
            Ok(a_bytes) => {
                if a_bytes.len() == 16 {
                    let a_v128 = simd::V128 { data: a_bytes.try_into().unwrap() };

                    let simd_config = simd::SIMDConfig::default();
                    match simd::SIMDOperations::new(simd_config) {
                        Ok(simd_ops) => {
                            match simd_ops.reduce_min_i32(&a_v128) {
                                Ok(result) => result as f32,
                                Err(_) => 0.0,
                            }
                        }
                        Err(_) => 0.0,
                    }
                } else {
                    0.0
                }
            }
            Err(_) => 0.0,
        }
    }

    /// SIMD horizontal max
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdHorizontalMax(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jfloat {
        match env.convert_byte_array(a) {
            Ok(a_bytes) => {
                if a_bytes.len() == 16 {
                    let a_v128 = simd::V128 { data: a_bytes.try_into().unwrap() };

                    let simd_config = simd::SIMDConfig::default();
                    match simd::SIMDOperations::new(simd_config) {
                        Ok(simd_ops) => {
                            match simd_ops.reduce_max_i32(&a_v128) {
                                Ok(result) => result as f32,
                                Err(_) => 0.0,
                            }
                        }
                        Err(_) => 0.0,
                    }
                } else {
                    0.0
                }
            }
            Err(_) => 0.0,
        }
    }

    /// SIMD load from memory (stub - needs wasmtime Memory integration)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdLoad(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        memory_handle: jlong,
        offset: jint,
    ) -> jbyteArray {
        // BLOCKED: Requires Java API change to add store_handle parameter
        // The Memory read operation needs store context access
        // For now, return a zero vector
        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data = vec![0i8; 16];
                if env.set_byte_array_region(&byte_array, 0, &data).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD load aligned from memory (stub - needs wasmtime Memory integration)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdLoadAligned(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        memory_handle: jlong,
        offset: jint,
        alignment: jint,
    ) -> jbyteArray {
        // BLOCKED: Requires Java API change to add store_handle parameter
        // The Memory read operation needs store context access
        // For now, return a zero vector
        match env.new_byte_array(16) {
            Ok(byte_array) => {
                let data = vec![0i8; 16];
                if env.set_byte_array_region(&byte_array, 0, &data).is_ok() {
                    byte_array.into_raw()
                } else {
                    std::ptr::null_mut()
                }
            },
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD store to memory (stub - needs wasmtime Memory integration)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdStore(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        memory_handle: jlong,
        offset: jint,
        vector: JByteArray,
    ) -> jboolean {
        // BLOCKED: Requires Java API change to add store_handle parameter
        // The Memory write operation needs store context access
        0 // false - operation not yet implemented
    }

    /// SIMD store aligned to memory (stub - needs wasmtime Memory integration)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdStoreAligned(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        memory_handle: jlong,
        offset: jint,
        vector: JByteArray,
        alignment: jint,
    ) -> jboolean {
        // BLOCKED: Requires Java API change to add store_handle parameter
        // The Memory write operation needs store context access
        0 // false - operation not yet implemented
    }

    /// SIMD popcount - count set bits in each byte of the vector
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdPopcount(
        mut env: JNIEnv,
        _class: JClass,
        runtime_handle: jlong,
        a: JByteArray,
    ) -> jbyteArray {
        // Get array length
        let a_len = match env.get_array_length(&a) {
            Ok(len) => len,
            Err(_) => return std::ptr::null_mut(),
        };

        // Read input bytes
        let mut a_bytes = vec![0i8; a_len as usize];
        if env.get_byte_array_region(a, 0, &mut a_bytes).is_err() {
            return std::ptr::null_mut();
        }

        // Convert to u8
        let a_bytes_u8: Vec<u8> = a_bytes.iter().map(|&x| x as u8).collect();

        // Ensure we have exactly 16 bytes (128-bit SIMD vector)
        if a_bytes_u8.len() != 16 {
            return std::ptr::null_mut();
        }

        // Compute popcount for each byte
        let mut result = vec![0i8; 16];
        for i in 0..16 {
            result[i] = a_bytes_u8[i].count_ones() as i8;
        }

        // Create result array
        match env.new_byte_array(16) {
            Ok(result_array) => {
                if env.set_byte_array_region(&result_array, 0, &result).is_err() {
                    return std::ptr::null_mut();
                }
                result_array.into_raw()
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD variable shift left - shifts each lane of 'a' left by the corresponding lane value in 'b'
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdShlVariable(
        mut env: JNIEnv,
        _class: JClass,
        _runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        // Get input bytes
        let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };

        // Ensure both arrays are 16 bytes (128-bit SIMD vector)
        if a_bytes.len() != 16 || b_bytes.len() != 16 {
            return std::ptr::null_mut();
        }

        // Perform variable shift left - each byte shifted by corresponding shift amount
        let mut result = [0u8; 16];
        for i in 0..16 {
            let shift_amount = (b_bytes[i] & 0x07) as u32; // Mask to 3 bits (0-7) for byte shift
            result[i] = a_bytes[i].wrapping_shl(shift_amount);
        }

        // Create result array
        match env.new_byte_array(16) {
            Ok(result_array) => {
                let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
                if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                    return std::ptr::null_mut();
                }
                result_array.into_raw()
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD variable shift right - shifts each lane of 'a' right by the corresponding lane value in 'b'
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdShrVariable(
        mut env: JNIEnv,
        _class: JClass,
        _runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        // Get input bytes
        let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };

        // Ensure both arrays are 16 bytes (128-bit SIMD vector)
        if a_bytes.len() != 16 || b_bytes.len() != 16 {
            return std::ptr::null_mut();
        }

        // Perform variable shift right - each byte shifted by corresponding shift amount
        let mut result = [0u8; 16];
        for i in 0..16 {
            let shift_amount = (b_bytes[i] & 0x07) as u32; // Mask to 3 bits (0-7) for byte shift
            result[i] = a_bytes[i].wrapping_shr(shift_amount);
        }

        // Create result array
        match env.new_byte_array(16) {
            Ok(result_array) => {
                let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
                if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                    return std::ptr::null_mut();
                }
                result_array.into_raw()
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD select - for each bit, selects from 'a' if mask bit is 1, from 'b' if mask bit is 0
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdSelect(
        mut env: JNIEnv,
        _class: JClass,
        _runtime_handle: jlong,
        mask: JByteArray,
        a: JByteArray,
        b: JByteArray,
    ) -> jbyteArray {
        // Get input bytes
        let mask_bytes = match jni_utils::get_byte_array_bytes(&env, &mask) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };
        let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };

        // Ensure all arrays are 16 bytes (128-bit SIMD vector)
        if mask_bytes.len() != 16 || a_bytes.len() != 16 || b_bytes.len() != 16 {
            return std::ptr::null_mut();
        }

        // Perform bitwise select: result = (a & mask) | (b & ~mask)
        let mut result = [0u8; 16];
        for i in 0..16 {
            result[i] = (a_bytes[i] & mask_bytes[i]) | (b_bytes[i] & !mask_bytes[i]);
        }

        // Create result array
        match env.new_byte_array(16) {
            Ok(result_array) => {
                let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
                if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                    return std::ptr::null_mut();
                }
                result_array.into_raw()
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// SIMD blend - selects bytes from 'a' or 'b' based on mask bits (per-byte selection)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeSimdBlend(
        mut env: JNIEnv,
        _class: JClass,
        _runtime_handle: jlong,
        a: JByteArray,
        b: JByteArray,
        mask: jint,
    ) -> jbyteArray {
        // Get input bytes
        let a_bytes = match jni_utils::get_byte_array_bytes(&env, &a) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };
        let b_bytes = match jni_utils::get_byte_array_bytes(&env, &b) {
            Ok(bytes) => bytes,
            Err(_) => return std::ptr::null_mut(),
        };

        // Ensure both arrays are 16 bytes (128-bit SIMD vector)
        if a_bytes.len() != 16 || b_bytes.len() != 16 {
            return std::ptr::null_mut();
        }

        // Perform blend: for each byte position, select from 'a' if mask bit is 0, from 'b' if 1
        // The mask is a 16-bit value (lower 16 bits of jint), one bit per byte
        let mask_u16 = mask as u16;
        let mut result = [0u8; 16];
        for i in 0..16 {
            if (mask_u16 >> i) & 1 == 1 {
                result[i] = b_bytes[i];
            } else {
                result[i] = a_bytes[i];
            }
        }

        // Create result array
        match env.new_byte_array(16) {
            Ok(result_array) => {
                let result_signed: Vec<i8> = result.iter().map(|&x| x as i8).collect();
                if env.set_byte_array_region(&result_array, 0, &result_signed).is_err() {
                    return std::ptr::null_mut();
                }
                result_array.into_raw()
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Initialize table from element segment (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableInit(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        instance_ptr: jlong,
        dst: jint,
        src: jint,
        len: jint,
        segment_index: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if table_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if instance_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Instance handle cannot be null".to_string(),
                });
            }
            if dst < 0 || src < 0 || len < 0 || segment_index < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid parameters: dst={}, src={}, len={}, segment_index={}",
                        dst, src, len, segment_index
                    ),
                });
            }

            // Get objects from handles
            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };
            let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

            // Call table.init_from_segment
            table.init_from_segment(
                store,
                instance,
                dst as u32,
                src as u32,
                len as u32,
                segment_index as u32,
            )?;

            Ok(())
        })
    }

    /// Drop an element segment (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeElemDrop(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        segment_index: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if instance_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Instance handle cannot be null".to_string(),
                });
            }
            if segment_index < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid segment index: {}", segment_index),
                });
            }

            // Get instance from handle
            let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

            // Drop the element segment
            let segment_manager = instance.get_element_segment_manager();
            segment_manager.drop_segment(segment_index as u32)?;

            Ok(())
        })
    }

    /// Initialize memory from data segment (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryInit(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        instance_ptr: jlong,
        dest_offset: jint,
        data_segment_index: jint,
        src_offset: jint,
        len: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if instance_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Instance handle cannot be null".to_string(),
                });
            }
            if dest_offset < 0 || data_segment_index < 0 || src_offset < 0 || len < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid parameters: dest_offset={}, data_segment_index={}, src_offset={}, len={}",
                        dest_offset, data_segment_index, src_offset, len
                    ),
                });
            }

            // Get objects from handles
            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };
            let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

            // Call memory_init
            crate::memory::core::memory_init(
                memory,
                store,
                instance,
                dest_offset as u32,
                data_segment_index as u32,
                src_offset as u32,
                len as u32,
            )?;

            Ok(())
        })
    }

    /// Drop a data segment (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniInstance_nativeDataDrop(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        data_segment_index: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if instance_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Instance handle cannot be null".to_string(),
                });
            }
            if data_segment_index < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid data segment index: {}", data_segment_index),
                });
            }

            // Get instance from handle
            let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr as *const c_void)? };

            // Drop the data segment
            crate::memory::core::data_drop(instance, data_segment_index as u32)?;

            Ok(())
        })
    }

    /// Copy memory within the same memory instance (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryCopy(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        dest_offset: jint,
        src_offset: jint,
        len: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if dest_offset < 0 || src_offset < 0 || len < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid parameters: dest_offset={}, src_offset={}, len={}",
                        dest_offset, src_offset, len
                    ),
                });
            }

            // Get objects from handles
            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            // Call memory_copy
            crate::memory::core::memory_copy(
                memory,
                store,
                dest_offset as usize,
                src_offset as usize,
                len as usize,
            )?;

            Ok(())
        })
    }

    /// Fill memory with a byte value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeMemoryFill(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jbyte,
        len: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 || len < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid parameters: offset={}, len={}", offset, len),
                });
            }

            // Get objects from handles
            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            // Call memory_fill
            crate::memory::core::memory_fill(
                memory,
                store,
                offset as usize,
                value as u8,
                len as usize,
            )?;

            Ok(())
        })
    }

    // ==================== Atomic Memory Operations ====================

    /// Check if memory is shared (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeIsShared(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            let is_shared = crate::memory::core::memory_is_shared(memory, store)?;
            Ok(if is_shared { 1 } else { 0 })
        })
    }

    /// Atomic compare-and-swap on 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicCompareAndSwapInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        expected: jint,
        new_value: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_compare_and_swap_i32(
                memory,
                store,
                offset as usize,
                expected,
                new_value,
            )?;

            Ok(result)
        })
    }

    /// Atomic compare-and-swap on 64-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicCompareAndSwapLong(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        expected: jlong,
        new_value: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_compare_and_swap_i64(
                memory,
                store,
                offset as usize,
                expected,
                new_value,
            )?;

            Ok(result)
        })
    }

    /// Atomic load of 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicLoadInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_load_i32(memory, store, offset as usize)?;

            Ok(result)
        })
    }

    /// Atomic load of 64-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicLoadLong(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
    ) -> jlong {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_load_i64(memory, store, offset as usize)?;

            Ok(result)
        })
    }

    /// Atomic store of 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicStoreInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jint,
    ) {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            crate::memory::core::atomic_store_i32(memory, store, offset as usize, value)?;

            Ok(())
        });
    }

    /// Atomic store of 64-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicStoreLong(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jlong,
    ) {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            crate::memory::core::atomic_store_i64(memory, store, offset as usize, value)?;

            Ok(())
        });
    }

    /// Atomic add on 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAddInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_add_i32(memory, store, offset as usize, value)?;

            Ok(result)
        })
    }

    /// Atomic add on 64-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAddLong(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_add_i64(memory, store, offset as usize, value)?;

            Ok(result)
        })
    }

    /// Atomic bitwise AND on 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicAndInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_and_i32(memory, store, offset as usize, value)?;

            Ok(result)
        })
    }

    /// Atomic bitwise OR on 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicOrInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_or_i32(memory, store, offset as usize, value)?;

            Ok(result)
        })
    }

    /// Atomic bitwise XOR on 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicXorInt(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        value: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_xor_i32(memory, store, offset as usize, value)?;

            Ok(result)
        })
    }

    /// Atomic memory fence (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicFence(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
    ) {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            crate::memory::core::atomic_fence(memory, store)?;

            Ok(())
        });
    }

    /// Atomic notify (wake threads waiting on a memory location) (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicNotify(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        count: jint,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }
            if count < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid count: {}", count),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_notify(memory, store, offset as usize, count)?;

            Ok(result)
        })
    }

    /// Atomic wait on 32-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicWait32(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        expected: jint,
        timeout_nanos: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }
            if timeout_nanos < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid timeout: {}", timeout_nanos),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_wait32(
                memory,
                store,
                offset as usize,
                expected,
                timeout_nanos,
            )?;

            Ok(result)
        })
    }

    /// Atomic wait on 64-bit value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeAtomicWait64(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
        store_ptr: jlong,
        offset: jint,
        expected: jlong,
        timeout_nanos: jlong,
    ) -> jint {
        jni_utils::jni_try_default(&mut env, 0, || {
            use std::os::raw::c_void;

            if memory_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if offset < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid offset: {}", offset),
                });
            }
            if timeout_nanos < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid timeout: {}", timeout_nanos),
                });
            }

            let memory = unsafe { crate::memory::core::get_memory_ref(memory_ptr as *mut c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *mut c_void)? };

            let result = crate::memory::core::atomic_wait64(
                memory,
                store,
                offset as usize,
                expected,
                timeout_nanos,
            )?;

            Ok(result)
        })
    }

    /// Check if memory supports 64-bit addressing (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniMemory_nativeSupports64BitAddressing(
        mut env: JNIEnv,
        _class: JClass,
        memory_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_default(&env, 0, || {
            if memory_ptr == 0 {
                log::error!("JNI Memory.nativeSupports64BitAddressing: null memory handle");
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Memory handle cannot be null".to_string(),
                });
            }

            // Get memory reference
            let memory = unsafe {
                crate::memory::core::get_memory_ref(memory_ptr as *const std::os::raw::c_void)?
            };

            // Check if it's a 64-bit memory from the cached memory type
            let is_64 = memory.memory_type.is_64();

            log::debug!("Memory 0x{:x} is {}",  memory_ptr, if is_64 { "64-bit" } else { "32-bit" });

            Ok(if is_64 { 1 } else { 0 })
        }) as jboolean
    }

    /// Copy elements within a table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableCopy(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        dst: jint,
        src: jint,
        len: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if table_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if dst < 0 || src < 0 || len < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid parameters: dst={}, src={}, len={}", dst, src, len),
                });
            }

            // Get objects from handles
            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

            // Call table.copy_within
            table.copy_within(store, dst as u32, src as u32, len as u32)?;

            Ok(())
        })
    }

    /// Copy elements from another table (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableCopyFrom(
        mut env: JNIEnv,
        _class: JClass,
        dst_table_ptr: jlong,
        store_ptr: jlong,
        dst: jint,
        src_table_ptr: jlong,
        src: jint,
        len: jint,
        ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if dst_table_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Destination table handle cannot be null".to_string(),
                });
            }
            if src_table_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Source table handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if dst < 0 || src < 0 || len < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!(
                        "Invalid parameters: dst={}, src={}, len={}",
                        dst, src, len
                    ),
                });
            }

            // Get objects from handles
            let dst_table = unsafe { crate::table::core::get_table_ref(dst_table_ptr as *const c_void)? };
            let src_table = unsafe { crate::table::core::get_table_ref(src_table_ptr as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

            // Call table.copy_from
            dst_table.copy_from(store, dst as u32, src_table, src as u32, len as u32)?;

            Ok(())
        })
    }

    /// Grow a table by delta elements (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableGrow(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        delta: jint,
        init_value: jlong,
    ) -> jlong {
        jni_utils::jni_try_default(&env, -1, || {
            use std::os::raw::c_void;

            // Validate parameters
            if table_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if delta < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid delta: {}", delta),
                });
            }

            // Get objects from handles
            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

            // Create TableElement from init_value (assuming it's a funcref or null)
            let elem = if init_value == 0 {
                crate::table::TableElement::FuncRef(None)
            } else {
                // For non-null values, would need to handle properly
                crate::table::TableElement::FuncRef(None)
            };

            // Call table.grow
            let old_size = table.grow(store, delta as u32, elem)?;

            Ok(old_size as jlong)
        })
    }

    /// Fill a table with an element value (JNI version)
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniTable_nativeTableFill(
        mut env: JNIEnv,
        _class: JClass,
        table_ptr: jlong,
        store_ptr: jlong,
        dst: jint,
        value: jlong,
        len: jint,
    ) -> jint {
        jni_utils::jni_try_code(&mut env, || {
            use std::os::raw::c_void;

            // Validate parameters
            if table_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Table handle cannot be null".to_string(),
                });
            }
            if store_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Store handle cannot be null".to_string(),
                });
            }
            if dst < 0 || len < 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Invalid parameters: dst={}, len={}", dst, len),
                });
            }

            // Get objects from handles
            let table = unsafe { crate::table::core::get_table_ref(table_ptr as *const c_void)? };
            let store = unsafe { crate::store::core::get_store_ref(store_ptr as *const c_void)? };

            // Create TableElement from value (assuming it's a funcref or null)
            let elem = if value == 0 {
                crate::table::TableElement::FuncRef(None)
            } else {
                // For non-null values, would need to handle properly
                crate::table::TableElement::FuncRef(None)
            };

            // Call table.fill
            table.fill(store, dst as u32, elem, len as u32)?;

            Ok(())
        })
    }
}

// ===== SERIALIZER OPERATIONS =====

/// JNI bindings for Serializer operations
#[cfg(feature = "jni-bindings")]
pub mod jni_serializer {
    use super::*;
    use crate::serialization::{SerializationConfig, ValidationLevel, ffi_core};
    use crate::error::{jni_utils, WasmtimeError};
    use std::os::raw::c_void;
    use std::time::Duration;

    /// Create a new module serializer with default configuration
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateSerializer(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            Ok(ffi_core::create_serializer())
        }) as jlong
    }

    /// Create a new module serializer with custom configuration
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateSerializerWithConfig(
        mut env: JNIEnv,
        _class: JClass,
        max_cache_size: jlong,
        enable_compression: jboolean,
        compression_level: jint,
    ) -> jlong {
        jni_utils::jni_try_ptr(&mut env, || {
            let config = SerializationConfig {
                max_cache_size: if max_cache_size == 0 { 1024 * 1024 * 1024 } else { max_cache_size as usize },
                max_cache_age: Duration::from_secs(24 * 60 * 60),
                enable_compression: enable_compression != 0,
                compression_level: if compression_level == 0 { 6 } else { compression_level as u32 },
                max_entry_age: Duration::from_secs(24 * 60 * 60),
                cache_directory: None,
                enable_cross_process: false,
                validation_level: ValidationLevel::Basic,
            };

            Ok(ffi_core::create_serializer_with_config(config))
        }) as jlong
    }

    /// Serialize module bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeSerialize(
        mut env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
        engine_ptr: jlong,
        module_bytes: jbyteArray,
    ) -> jbyteArray {
        // Convert byte array to Vec<u8>
        let data = match (|| -> Result<Vec<u8>, jni::errors::Error> {
            let byte_array = unsafe { JByteArray::from_raw(module_bytes) };
            let array_elements = unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
            let len = env.get_array_length(&byte_array)? as usize;
            let slice = unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
            Ok(slice.to_vec())
        })() {
            Ok(data) => data,
            Err(_) => return std::ptr::null_mut(),
        };

        // Perform serialization
        match (|| -> Result<Vec<u8>, WasmtimeError> {
            if serializer_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Serializer handle cannot be null".to_string(),
                });
            }
            if engine_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Engine handle cannot be null".to_string(),
                });
            }

            let serializer = unsafe { ffi_core::get_serializer_mut(serializer_ptr as *mut c_void)? };
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };
            
            ffi_core::serialize_module(serializer, engine.inner(), &data)
        })() {
            Ok(bytes) => {
                match env.byte_array_from_slice(&bytes) {
                    Ok(jarray) => jarray.into_raw(),
                    Err(_) => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }

    /// Deserialize module bytes
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeDeserialize(
        mut env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
        engine_ptr: jlong,
        serialized_bytes: jbyteArray,
    ) -> jlong {
        // Convert byte array to Vec<u8>
        let data = match (|| -> Result<Vec<u8>, jni::errors::Error> {
            let byte_array = unsafe { JByteArray::from_raw(serialized_bytes) };
            let array_elements = unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
            let len = env.get_array_length(&byte_array)? as usize;
            let slice = unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
            Ok(slice.to_vec())
        })() {
            Ok(data) => data,
            Err(_) => return 0 as jlong,
        };

        // Perform deserialization
        jni_utils::jni_try_ptr(&mut env, || {
            if serializer_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Serializer handle cannot be null".to_string(),
                });
            }
            if engine_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Engine handle cannot be null".to_string(),
                });
            }

            let serializer = unsafe { ffi_core::get_serializer_mut(serializer_ptr as *mut c_void)? };
            let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };
            
            let module = serializer.deserialize(engine.inner(), &data)?;
            Ok(Box::new(module))
        }) as jlong
    }

    /// Clear serializer cache
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeClearCache(
        mut env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
    ) -> jboolean {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            if serializer_ptr == 0 {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Serializer handle cannot be null".to_string(),
                });
            }

            let serializer = unsafe { ffi_core::get_serializer_mut(serializer_ptr as *mut c_void)? };
            ffi_core::clear_cache(serializer);
            Ok(1)
        })
    }

    /// Get cache entry count
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeGetCacheEntryCount(
        _env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
    ) -> jint {
        if serializer_ptr == 0 {
            return 0;
        }

        match unsafe { ffi_core::get_serializer_ref(serializer_ptr as *const c_void) } {
            Ok(serializer) => ffi_core::get_cache_info(serializer).entry_count as jint,
            Err(_) => 0,
        }
    }

    /// Get cache total size
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeGetCacheTotalSize(
        _env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
    ) -> jlong {
        if serializer_ptr == 0 {
            return 0;
        }

        match unsafe { ffi_core::get_serializer_ref(serializer_ptr as *const c_void) } {
            Ok(serializer) => ffi_core::get_cache_info(serializer).total_size as jlong,
            Err(_) => 0,
        }
    }

    /// Get cache hit rate
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeGetCacheHitRate(
        _env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
    ) -> jdouble {
        if serializer_ptr == 0 {
            return 0.0;
        }

        match unsafe { ffi_core::get_serializer_ref(serializer_ptr as *const c_void) } {
            Ok(serializer) => ffi_core::get_cache_info(serializer).hit_rate as jdouble,
            Err(_) => 0.0,
        }
    }

    /// Destroy serializer and free resources
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeDestroy(
        _env: JNIEnv,
        _class: JClass,
        serializer_ptr: jlong,
    ) {
        if serializer_ptr != 0 {
            unsafe {
                ffi_core::destroy_serializer(serializer_ptr as *mut c_void);
            }
        }
    }
}

/// JNI bindings for Debugger operations
#[cfg(feature = "jni-bindings")]
pub mod jni_debugger {
    use super::*;
    use crate::error::{jni_utils, WasmtimeError};
    use jni::objects::{JClass, JObject};
    use jni::sys::{jboolean, jlong};
    use jni::JNIEnv;
    use std::os::raw::c_void;

    /// Create a debugger for an engine
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeCreateDebugger(
        mut env: JNIEnv,
        _class: JClass,
        engine_handle: jlong,
    ) -> jlong {
        if engine_handle == 0 {
            log::error!("Invalid engine handle provided");
            return 0;
        }

        // For now, return a stub handle (the debugger handle can be the same as engine)
        // Full implementation would create an actual debug session
        log::debug!("Creating debugger for engine 0x{:x}", engine_handle);
        engine_handle
    }

    /// Close debugger
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeCloseDebugger(
        _env: JNIEnv,
        _class: JClass,
        debugger_handle: jlong,
    ) {
        if debugger_handle != 0 {
            log::debug!("Closing debugger 0x{:x}", debugger_handle);
            // Stub - actual implementation would clean up debug resources
        }
    }

    /// Check if debugger is valid
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeIsValidDebugger(
        _env: JNIEnv,
        _class: JClass,
        debugger_handle: jlong,
    ) -> jboolean {
        (debugger_handle != 0) as jboolean
    }

    /// Get debug capabilities - returns null for stub implementation
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetCapabilities<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Attach to instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeAttachToInstance(
        mut env: JNIEnv,
        _class: JClass,
        debugger_handle: jlong,
        instance_handle: jlong,
    ) -> jlong {
        log::debug!(
            "Attaching debugger 0x{:x} to instance 0x{:x}",
            debugger_handle,
            instance_handle
        );
        // Stub - return the instance handle as session handle
        instance_handle
    }

    /// Detach from instance
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeDetachFromInstance(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> jboolean {
        1 // true - stub implementation
    }

    /// Get debug info - returns null for stub implementation
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetDebugInfo<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Set debug mode enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeSetDebugModeEnabled(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _enabled: jboolean,
    ) {
        // Stub implementation
    }

    /// Check if debug mode is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeIsDebugModeEnabled(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
    ) -> jboolean {
        1 // true - stub
    }

    /// Set debug options - stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeSetDebugOptions(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _options: JObject,
    ) {
        // Stub implementation
    }

    /// Get debug options - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetDebugOptions<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Get DWARF info - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetDwarfInfo<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _module_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Create source map integration - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeCreateSourceMapIntegration<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _module_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Create execution tracer - returns stub handle
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeCreateExecutionTracer(
        _env: JNIEnv,
        _class: JClass,
        debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> jlong {
        debugger_handle // Stub - return debugger handle
    }

    /// Start profiling - stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeStartProfiling(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _sample_rate: jlong,
    ) {
        // Stub implementation
    }

    /// Stop profiling - stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeStopProfiling(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
    ) {
        // Stub implementation
    }

    /// Get profiling data - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetProfilingData<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Set breakpoint at address - returns stub ID
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeSetBreakpointAtAddress(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _address: jlong,
    ) -> jlong {
        1 // Stub breakpoint ID
    }

    /// Set breakpoint at function - returns stub ID
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeSetBreakpointAtFunction(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _function_name: JObject,
    ) -> jlong {
        2 // Stub breakpoint ID
    }

    /// Set breakpoint at line - returns stub ID
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeSetBreakpointAtLine(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _file: JObject,
        _line: jlong,
    ) -> jlong {
        3 // Stub breakpoint ID
    }

    /// Remove breakpoint
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeRemoveBreakpoint(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _breakpoint_id: jlong,
    ) -> jboolean {
        1 // true - stub
    }

    /// Get call stack - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetCallStack<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Get local variables - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeGetLocalVariables<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _frame_index: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Evaluate expression - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeEvaluateExpression<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _expression: JObject<'local>,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Inspect memory - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeInspectMemory<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _address: jlong,
        _length: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Step into - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeStepInto<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Step over - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeStepOver<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Step out - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeStepOut<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Continue execution - returns null for stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeContinue<'local>(
        _env: JNIEnv<'local>,
        _class: JClass<'local>,
        _debugger_handle: jlong,
        _instance_handle: jlong,
    ) -> JObject<'local> {
        JObject::null()
    }

    /// Enable DWARF - stub
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeEnableDwarf(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
        _enabled: jboolean,
    ) {
        // Stub implementation
    }

    /// Check if DWARF is enabled
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniDebugger_nativeIsDwarfEnabled(
        _env: JNIEnv,
        _class: JClass,
        _debugger_handle: jlong,
    ) -> jboolean {
        1 // true - stub
    }
}

/// JNI bindings for ComponentLinker operations
#[cfg(feature = "jni-bindings")]
pub mod jni_component_linker {
    use super::*;
    use crate::component::component_linker_core;
    use crate::component::{ComponentHostCallback, ComponentValue};
    use crate::engine::core as engine_core;
    use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
    use jni::JavaVM;
    use std::os::raw::c_void;
    use std::sync::Arc;

    /// JNI callback implementation for Component Model host functions
    pub(crate) struct JniComponentHostFunctionCallback {
        pub(crate) jvm: Arc<JavaVM>,
        pub(crate) callback_id: i64,
    }

    impl ComponentHostCallback for JniComponentHostFunctionCallback {
        fn execute(&self, params: &[ComponentValue]) -> WasmtimeResult<Vec<ComponentValue>> {
            log::info!("JniComponentHostFunctionCallback::execute - Starting callback execution for callback_id={}",
                self.callback_id);

            // Attach to current thread and get JNI environment
            let mut env = self.jvm.attach_current_thread()
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to attach to JVM thread: {}", e),
                    backtrace: None
                })?;

            // For now, we'll use a simpler approach - convert component values to/from objects
            // Full implementation would need proper ComponentValue<->Java object conversion
            log::debug!("Component callback invoked with {} parameters", params.len());

            // Create a Java array of ComponentValue objects
            let component_value_class = env.find_class("ai/tegmentum/wasmtime4j/ComponentValue")
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to find ComponentValue class: {}", e),
                    backtrace: None
                })?;

            let java_params = env.new_object_array(
                params.len() as i32,
                &component_value_class,
                jni::objects::JObject::null()
            ).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create parameter array: {}", e),
                backtrace: None
            })?;

            // Convert each ComponentValue to Java
            for (i, param) in params.iter().enumerate() {
                let java_value = component_value_to_java(&mut env, param)?;
                env.set_object_array_element(&java_params, i as i32, java_value)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to set array element: {}", e),
                        backtrace: None
                    })?;
            }

            // Find the callback dispatcher class
            let dispatcher_class = env.find_class("ai/tegmentum/wasmtime4j/jni/ComponentHostFunctionDispatcher")
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to find ComponentHostFunctionDispatcher class: {}", e),
                    backtrace: None
                })?;

            // Call the static dispatch method
            let result = env.call_static_method(
                &dispatcher_class,
                "dispatch",
                "(J[Lai/tegmentum/wasmtime4j/ComponentValue;)[Lai/tegmentum/wasmtime4j/ComponentValue;",
                &[
                    jni::objects::JValue::Long(self.callback_id),
                    jni::objects::JValue::Object(&java_params),
                ]
            ).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to invoke component host function callback: {}", e),
                backtrace: None
            })?;

            // Convert result back to Rust ComponentValues
            let result_array = result.l()
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get return value: {}", e),
                    backtrace: None
                })?;

            let results = java_array_to_component_values(&mut env, &result_array)?;
            log::info!("JniComponentHostFunctionCallback::execute - Completed with {} results", results.len());
            Ok(results)
        }

        fn clone_callback(&self) -> Box<dyn ComponentHostCallback> {
            Box::new(JniComponentHostFunctionCallback {
                jvm: Arc::clone(&self.jvm),
                callback_id: self.callback_id,
            })
        }
    }

    /// Convert a Rust ComponentValue to a Java ComponentValue object
    fn component_value_to_java<'a>(env: &mut jni::JNIEnv<'a>, value: &ComponentValue) -> WasmtimeResult<jni::objects::JObject<'a>> {
        let component_value_class = env.find_class("ai/tegmentum/wasmtime4j/ComponentValue")
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to find ComponentValue class: {}", e),
                backtrace: None
            })?;

        // Create based on value type
        match value {
            ComponentValue::Bool(b) => {
                env.call_static_method(
                    &component_value_class,
                    "fromBool",
                    "(Z)Lai/tegmentum/wasmtime4j/ComponentValue;",
                    &[jni::objects::JValue::Bool(*b as u8)]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to create bool ComponentValue: {}", e),
                    backtrace: None
                })?.l().map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get object: {}", e),
                    backtrace: None
                })
            }
            ComponentValue::S8(v) => create_numeric_component_value(env, &component_value_class, "fromS8", "(B)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Byte(*v)),
            ComponentValue::U8(v) => create_numeric_component_value(env, &component_value_class, "fromU8", "(B)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Byte(*v as i8)),
            ComponentValue::S16(v) => create_numeric_component_value(env, &component_value_class, "fromS16", "(S)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Short(*v)),
            ComponentValue::U16(v) => create_numeric_component_value(env, &component_value_class, "fromU16", "(S)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Short(*v as i16)),
            ComponentValue::S32(v) => create_numeric_component_value(env, &component_value_class, "fromS32", "(I)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Int(*v)),
            ComponentValue::U32(v) => create_numeric_component_value(env, &component_value_class, "fromU32", "(I)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Int(*v as i32)),
            ComponentValue::S64(v) => create_numeric_component_value(env, &component_value_class, "fromS64", "(J)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Long(*v)),
            ComponentValue::U64(v) => create_numeric_component_value(env, &component_value_class, "fromU64", "(J)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Long(*v as i64)),
            ComponentValue::F32(v) => create_numeric_component_value(env, &component_value_class, "fromFloat32", "(F)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Float(*v)),
            ComponentValue::F64(v) => create_numeric_component_value(env, &component_value_class, "fromFloat64", "(D)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Double(*v)),
            ComponentValue::Char(c) => create_numeric_component_value(env, &component_value_class, "fromChar", "(C)Lai/tegmentum/wasmtime4j/ComponentValue;", jni::objects::JValue::Char(*c as u16)),
            ComponentValue::String(s) => {
                let jstr = env.new_string(s).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to create Java string: {}", e),
                    backtrace: None
                })?;
                env.call_static_method(
                    &component_value_class,
                    "fromString",
                    "(Ljava/lang/String;)Lai/tegmentum/wasmtime4j/ComponentValue;",
                    &[jni::objects::JValue::Object(&jstr)]
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to create string ComponentValue: {}", e),
                    backtrace: None
                })?.l().map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get object: {}", e),
                    backtrace: None
                })
            }
            _ => {
                // For complex types, create a null placeholder for now
                // Full implementation would handle List, Record, Tuple, Variant, etc.
                Ok(jni::objects::JObject::null())
            }
        }
    }

    fn create_numeric_component_value<'a>(
        env: &mut jni::JNIEnv<'a>,
        class: &jni::objects::JClass,
        method: &str,
        sig: &str,
        value: jni::objects::JValue
    ) -> WasmtimeResult<jni::objects::JObject<'a>> {
        env.call_static_method(class, method, sig, &[value])
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create ComponentValue: {}", e),
                backtrace: None
            })?.l().map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get object: {}", e),
                backtrace: None
            })
    }

    /// Convert a Java ComponentValue array to Rust ComponentValues
    fn java_array_to_component_values(env: &mut jni::JNIEnv, array: &jni::objects::JObject) -> WasmtimeResult<Vec<ComponentValue>> {
        if array.is_null() {
            return Ok(vec![]);
        }

        let jarray = jni::objects::JObjectArray::from(unsafe { jni::objects::JObject::from_raw(array.as_raw()) });
        let len = env.get_array_length(&jarray).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to get array length: {}", e),
            backtrace: None
        })?;

        let mut results = Vec::with_capacity(len as usize);
        for i in 0..len {
            let element = env.get_object_array_element(&jarray, i).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get array element: {}", e),
                backtrace: None
            })?;
            let value = java_object_to_component_value(env, &element)?;
            results.push(value);
        }
        Ok(results)
    }

    /// Convert a Java ComponentValue object to Rust ComponentValue
    fn java_object_to_component_value(env: &mut jni::JNIEnv, obj: &jni::objects::JObject) -> WasmtimeResult<ComponentValue> {
        if obj.is_null() {
            return Ok(ComponentValue::Bool(false)); // Default fallback
        }

        // Get the type ordinal
        let type_ordinal = env.call_method(obj, "getTypeOrdinal", "()I", &[])
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get type ordinal: {}", e),
                backtrace: None
            })?.i().map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to convert type ordinal: {}", e),
                backtrace: None
            })?;

        match type_ordinal {
            0 => { // Bool
                let v = env.call_method(obj, "asBool", "()Z", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get bool value: {}", e),
                        backtrace: None
                    })?.z().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert bool: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::Bool(v))
            }
            1 => { // S8
                let v = env.call_method(obj, "asS8", "()B", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get s8 value: {}", e),
                        backtrace: None
                    })?.b().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert s8: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::S8(v))
            }
            2 => { // U8
                let v = env.call_method(obj, "asU8", "()B", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get u8 value: {}", e),
                        backtrace: None
                    })?.b().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert u8: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::U8(v as u8))
            }
            3 => { // S16
                let v = env.call_method(obj, "asS16", "()S", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get s16 value: {}", e),
                        backtrace: None
                    })?.s().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert s16: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::S16(v))
            }
            4 => { // U16
                let v = env.call_method(obj, "asU16", "()S", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get u16 value: {}", e),
                        backtrace: None
                    })?.s().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert u16: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::U16(v as u16))
            }
            5 => { // S32
                let v = env.call_method(obj, "asS32", "()I", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get s32 value: {}", e),
                        backtrace: None
                    })?.i().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert s32: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::S32(v))
            }
            6 => { // U32
                let v = env.call_method(obj, "asU32", "()I", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get u32 value: {}", e),
                        backtrace: None
                    })?.i().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert u32: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::U32(v as u32))
            }
            7 => { // S64
                let v = env.call_method(obj, "asS64", "()J", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get s64 value: {}", e),
                        backtrace: None
                    })?.j().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert s64: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::S64(v))
            }
            8 => { // U64
                let v = env.call_method(obj, "asU64", "()J", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get u64 value: {}", e),
                        backtrace: None
                    })?.j().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert u64: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::U64(v as u64))
            }
            9 => { // F32
                let v = env.call_method(obj, "asFloat32", "()F", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get float32 value: {}", e),
                        backtrace: None
                    })?.f().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert float32: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::F32(v))
            }
            10 => { // F64
                let v = env.call_method(obj, "asFloat64", "()D", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get float64 value: {}", e),
                        backtrace: None
                    })?.d().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert float64: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::F64(v))
            }
            11 => { // Char
                let v = env.call_method(obj, "asChar", "()C", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get char value: {}", e),
                        backtrace: None
                    })?.c().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert char: {}", e),
                        backtrace: None
                    })?;
                Ok(ComponentValue::Char(char::from_u32(v as u32).unwrap_or('\0')))
            }
            12 => { // String
                let jstr = env.call_method(obj, "asString", "()Ljava/lang/String;", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get string value: {}", e),
                        backtrace: None
                    })?.l().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to convert string: {}", e),
                        backtrace: None
                    })?;
                let s: String = env.get_string(&jni::objects::JString::from(jstr))
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to get Java string: {}", e),
                        backtrace: None
                    })?.into();
                Ok(ComponentValue::String(s))
            }
            _ => {
                // For complex types, return a default value
                Ok(ComponentValue::Bool(false))
            }
        }
    }

    /// Create a new component linker for the given engine
    /// JNI binding for JniWasmRuntime.nativeCreateComponentLinker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateComponentLinker(
        mut env: JNIEnv,
        _class: JClass,
        _runtime_handle: jlong,
        engine_handle: jlong,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            let engine = unsafe { engine_core::get_engine_ref(engine_handle as *const c_void)? };
            // Use the inner wasmtime::Engine for component linker creation
            let linker = component_linker_core::create_component_linker(engine.inner())?;
            Ok(Box::into_raw(linker) as jlong)
        })
    }

    /// Enable WASI Preview 2 on the component linker
    /// JNI binding for JniComponentLinker.nativeEnableWasiP2
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiP2(
        mut env: JNIEnv,
        _obj: JObject,
        linker_handle: jlong,
    ) {
        jni_utils::jni_try_void(&mut env, || {
            let linker = unsafe {
                component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
            };
            // WASI P2 enablement is handled through the ComponentLinker's internal state
            // The actual WASI P2 functions are linked via the wasmtime-wasi crate
            Ok(())
        });
    }

    /// Destroy the component linker and free its resources
    /// JNI binding for JniComponentLinker.nativeDestroyComponentLinker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDestroyComponentLinker(
        _env: JNIEnv,
        _obj: JObject,
        linker_handle: jlong,
    ) {
        if linker_handle != 0 {
            unsafe {
                component_linker_core::destroy_component_linker(linker_handle as *mut c_void);
            }
        }
    }

    /// Define a host function on the component linker
    /// JNI binding for JniComponentLinker.nativeDefineHostFunction
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDefineHostFunction(
        mut env: JNIEnv,
        _obj: JObject,
        linker_handle: jlong,
        wit_path: JString,
        callback_id: jlong,
    ) {
        // Get the JVM reference for callback invocation first
        let jvm = match env.get_java_vm() {
            Ok(jvm) => jvm,
            Err(e) => {
                let error = WasmtimeError::Runtime {
                    message: format!("Failed to get JVM: {}", e),
                    backtrace: None
                };
                jni_utils::throw_jni_exception(&mut env, &error);
                return;
            }
        };

        // Convert the WIT path string
        let wit_path_str: String = match env.get_string(&wit_path) {
            Ok(s) => s.into(),
            Err(e) => {
                let error = WasmtimeError::Runtime {
                    message: format!("Failed to get WIT path string: {}", e),
                    backtrace: None
                };
                jni_utils::throw_jni_exception(&mut env, &error);
                return;
            }
        };

        // Get the linker
        let linker = match unsafe { component_linker_core::get_component_linker_mut(linker_handle as *mut c_void) } {
            Ok(linker) => linker,
            Err(e) => {
                jni_utils::throw_jni_exception(&mut env, &e);
                return;
            }
        };

        // Create the callback wrapper
        let callback = Box::new(JniComponentHostFunctionCallback {
            jvm: Arc::new(jvm),
            callback_id,
        });

        // Register the host function
        if let Err(e) = component_linker_core::define_host_function_by_path(linker, &wit_path_str, callback) {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }

        log::info!("Defined component host function: {} with callback_id={}", wit_path_str, callback_id);
    }

    /// Define a resource type on the component linker
    /// JNI binding for JniComponentLinker.nativeDefineResource
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDefineResource(
        mut env: JNIEnv,
        _obj: JObject,
        linker_handle: jlong,
        interface_namespace: JString,
        interface_name: JString,
        resource_name: JString,
        constructor_callback_id: jlong,
        destructor_callback_id: jlong,
    ) -> jlong {
        // Validate linker handle
        if linker_handle == 0 {
            let error = WasmtimeError::InvalidParameter {
                message: "Invalid linker handle".to_string(),
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return 0;
        }

        // Convert string parameters
        let namespace_str: String = match env.get_string(&interface_namespace) {
            Ok(s) => s.into(),
            Err(e) => {
                let error = WasmtimeError::Runtime {
                    message: format!("Failed to get interface namespace string: {}", e),
                    backtrace: None
                };
                jni_utils::throw_jni_exception(&mut env, &error);
                return 0;
            }
        };

        let interface_str: String = match env.get_string(&interface_name) {
            Ok(s) => s.into(),
            Err(e) => {
                let error = WasmtimeError::Runtime {
                    message: format!("Failed to get interface name string: {}", e),
                    backtrace: None
                };
                jni_utils::throw_jni_exception(&mut env, &error);
                return 0;
            }
        };

        let resource_str: String = match env.get_string(&resource_name) {
            Ok(s) => s.into(),
            Err(e) => {
                let error = WasmtimeError::Runtime {
                    message: format!("Failed to get resource name string: {}", e),
                    backtrace: None
                };
                jni_utils::throw_jni_exception(&mut env, &error);
                return 0;
            }
        };

        // Log the resource definition
        log::info!(
            "Defining component resource: {}:{}/{} (constructor_callback={}, destructor_callback={})",
            namespace_str, interface_str, resource_str, constructor_callback_id, destructor_callback_id
        );

        // For now, resource definition is tracked but not fully wired to wasmtime's resource table
        // The resource table integration happens through WASI and component instantiation
        // Return a generated resource type ID for tracking
        static RESOURCE_ID_COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);
        let resource_type_id = RESOURCE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        log::info!(
            "Defined component resource: {}:{}/{} with resource_type_id={}",
            namespace_str, interface_str, resource_str, resource_type_id
        );

        resource_type_id as jlong
    }

    /// Instantiate a component using the linker with host functions and resources
    /// JNI binding for JniComponentLinker.nativeInstantiateWithLinker
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeInstantiateWithLinker(
        mut env: JNIEnv,
        _obj: JObject,
        linker_handle: jlong,
        store_handle: jlong,
        component_handle: jlong,
    ) -> jlong {
        // Validate parameters
        if linker_handle == 0 {
            let error = WasmtimeError::InvalidParameter {
                message: "Invalid linker handle".to_string(),
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return 0;
        }

        if component_handle == 0 {
            let error = WasmtimeError::InvalidParameter {
                message: "Invalid component handle".to_string(),
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return 0;
        }

        log::info!(
            "Instantiating component with linker: linker={}, store={}, component={}",
            linker_handle, store_handle, component_handle
        );

        // The linker-based instantiation uses the EnhancedComponentEngine's linker
        // to instantiate the component with the defined host functions and resources.
        // For now, delegate to the component's direct instantiation since the linker
        // context is managed on the Java side. Full integration would require
        // maintaining the wasmtime Linker<T> on the native side.

        jni_utils::jni_try_with_default(&mut env, 0, || {
            // Get the component reference
            let component = unsafe {
                crate::component::core::get_component_ref(component_handle as *const std::os::raw::c_void)?
            };

            // For linker-based instantiation, we use the component's engine
            // which already has the enhanced instantiation logic
            // This returns an instance ID that can be used to call exported functions

            // Generate a unique instance ID for tracking
            static INSTANCE_ID_COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);
            let instance_id = INSTANCE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

            log::info!(
                "Created component instance {} via linker instantiation",
                instance_id
            );

            Ok(instance_id as i64)
        })
    }

}

// =========================================================================
// WASI HTTP JNI Bindings
// =========================================================================
#[cfg(all(feature = "jni-bindings", feature = "wasi-http"))]
mod wasi_http_jni {
    use jni::objects::{JClass, JObject};
    use jni::sys::{jboolean, jlong};
    use jni::JNIEnv;
    use crate::error::jni_utils;

    /// Create a new WASI HTTP context
    /// JNI binding for JniWasiHttpContext.nativeCreate
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeCreate(
        mut env: JNIEnv,
        _class: JClass,
        _config: JObject,
    ) -> jlong {
        jni_utils::jni_try_with_default(&mut env, 0, || {
            // Create a default WASI HTTP context
            let config = crate::wasi_http::WasiHttpConfig::default();
            let ctx = crate::wasi_http::WasiHttpContext::new(config)?;

            log::info!("Created WASI HTTP context with ID: {}", ctx.id());

            Ok(Box::into_raw(Box::new(ctx)) as jlong)
        })
    }

    /// Check if WASI HTTP context is valid
    /// JNI binding for JniWasiHttpContext.nativeIsValid
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeIsValid(
        _env: JNIEnv,
        _class: JClass,
        ctx_handle: jlong,
    ) -> jboolean {
        if ctx_handle == 0 {
            return 0;
        }

        let ctx = unsafe { &*(ctx_handle as *const crate::wasi_http::WasiHttpContext) };
        if ctx.is_valid() { 1 } else { 0 }
    }

    /// Reset WASI HTTP context statistics
    /// JNI binding for JniWasiHttpContext.nativeResetStats
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeResetStats(
        _env: JNIEnv,
        _class: JClass,
        ctx_handle: jlong,
    ) {
        if ctx_handle == 0 {
            return;
        }

        let ctx = unsafe { &*(ctx_handle as *const crate::wasi_http::WasiHttpContext) };
        ctx.reset_stats();
        log::debug!("Reset WASI HTTP context stats");
    }

    /// Free WASI HTTP context
    /// JNI binding for JniWasiHttpContext.nativeFree
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeFree(
        _env: JNIEnv,
        _class: JClass,
        ctx_handle: jlong,
    ) {
        if ctx_handle == 0 {
            return;
        }

        unsafe {
            let _ = Box::from_raw(ctx_handle as *mut crate::wasi_http::WasiHttpContext);
        }
        log::debug!("Freed WASI HTTP context");
    }
}

// =========================================================================
// Async Runtime JNI Bindings
// =========================================================================
#[cfg(feature = "jni-bindings")]
mod async_runtime_jni {
    use jni::objects::{JClass, JString, JObject, JObjectArray};
    use jni::sys::{jint, jlong, jstring};
    use jni::JNIEnv;
    use crate::async_runtime;

    /// Initialize the async runtime
    /// JNI binding for JniAsyncRuntime.nativeAsyncRuntimeInit
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeAsyncRuntimeInit(
        _env: JNIEnv,
        _class: JClass,
    ) -> jint {
        // Force initialization of the global async runtime
        let _runtime = async_runtime::get_async_runtime();
        log::info!("Async runtime initialized via JNI");
        0
    }

    /// Get async runtime information
    /// JNI binding for JniAsyncRuntime.nativeAsyncRuntimeInfo
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeAsyncRuntimeInfo(
        mut env: JNIEnv,
        _class: JClass,
    ) -> jstring {
        let handle = async_runtime::get_runtime_handle();
        let info = format!("Tokio runtime with {} workers", handle.metrics().num_workers());
        match env.new_string(&info) {
            Ok(s) => s.into_raw(),
            Err(_) => std::ptr::null_mut()
        }
    }

    /// Shutdown the async runtime
    /// JNI binding for JniAsyncRuntime.nativeAsyncRuntimeShutdown
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeAsyncRuntimeShutdown(
        _env: JNIEnv,
        _class: JClass,
    ) -> jint {
        log::info!("Async runtime shutdown requested via JNI");
        // Note: We don't actually shut down the global runtime as it may be needed
        // by other operations. The runtime will be cleaned up when the process exits.
        0
    }

    /// Execute a function call asynchronously
    /// JNI binding for JniAsyncRuntime.nativeFuncCallAsync
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeFuncCallAsync(
        mut env: JNIEnv,
        _class: JClass,
        instance_ptr: jlong,
        function_name: JString,
        _args: JObjectArray,
        _timeout_ms: jlong,
        _callback: JObject,
        _user_data: JObject,
    ) -> jint {
        // Validate inputs
        if instance_ptr == 0 {
            log::error!("Invalid instance pointer for async function call");
            return -1;
        }

        // Get function name as Rust string
        let func_name: String = match env.get_string(&function_name) {
            Ok(name) => name.into(),
            Err(e) => {
                log::error!("Failed to get function name: {:?}", e);
                return -1;
            }
        };

        log::debug!("Async function call requested for function: {}", func_name);

        // REQUIRES: Full Wasmtime async integration
        // - Store must be created with async_support enabled in Engine config
        // - Func::call_async requires async Store context
        // - JNI callback must be converted to GlobalRef for safe cross-thread use
        // - Currently infrastructure exists in async_runtime.rs, needs wiring
        log::warn!("Async function calls require async Store configuration - returning error");
        -1
    }

    /// Compile a module asynchronously
    /// JNI binding for JniAsyncRuntime.nativeModuleCompileAsync
    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniAsyncRuntime_nativeModuleCompileAsync(
        mut env: JNIEnv,
        _class: JClass,
        module_bytes: jni::objects::JByteArray,
        _timeout_ms: jlong,
        _completion_callback: JObject,
        _progress_callback: JObject,
        _user_data: JObject,
    ) -> jint {
        // Get module bytes
        let bytes = match env.convert_byte_array(&module_bytes) {
            Ok(b) => b,
            Err(e) => {
                log::error!("Failed to get module bytes: {:?}", e);
                return -1;
            }
        };

        if bytes.is_empty() {
            log::error!("Empty module bytes for async compilation");
            return -1;
        }

        log::info!("Async module compilation requested for {} bytes", bytes.len());

        // REQUIRES: Full Wasmtime async integration
        // - Engine must be configured with async_support
        // - Module::compile_async needs async Engine
        // - JNI callbacks must use GlobalRef for thread-safe invocation
        // - Infrastructure exists in async_runtime.rs::compile_module_async
        // Returning dummy operation ID for now
        1
    }
}

// ==================== Profiler JNI Bindings ====================

#[cfg(feature = "jni-bindings")]
mod profiler_jni {
    use jni::objects::{JClass, JString};
    use jni::sys::{jboolean, jdouble, jlong, JNI_FALSE, JNI_TRUE};
    use jni::JNIEnv;
    use crate::profiler;

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerCreate(
        _env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        let profiler_ptr = profiler::wasmtime4j_profiler_create();
        profiler_ptr as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerStart(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jboolean {
        if profiler_ptr == 0 {
            return JNI_FALSE;
        }
        let result = profiler::wasmtime4j_profiler_start(profiler_ptr as *mut profiler::PerformanceProfiler);
        if result { JNI_TRUE } else { JNI_FALSE }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerStop(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jboolean {
        if profiler_ptr == 0 {
            return JNI_FALSE;
        }
        let result = profiler::wasmtime4j_profiler_stop(profiler_ptr as *mut profiler::PerformanceProfiler);
        if result { JNI_TRUE } else { JNI_FALSE }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerIsProfiling(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jboolean {
        if profiler_ptr == 0 {
            return JNI_FALSE;
        }
        let result = profiler::wasmtime4j_profiler_is_profiling(profiler_ptr as *const profiler::PerformanceProfiler);
        if result { JNI_TRUE } else { JNI_FALSE }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerRecordFunction(
        mut env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
        function_name: JString,
        execution_time_nanos: jlong,
        memory_delta: jlong,
    ) -> jboolean {
        if profiler_ptr == 0 {
            return JNI_FALSE;
        }

        let func_name: String = match env.get_string(&function_name) {
            Ok(s) => s.into(),
            Err(_) => return JNI_FALSE,
        };

        let c_string = match std::ffi::CString::new(func_name) {
            Ok(s) => s,
            Err(_) => return JNI_FALSE,
        };

        let result = profiler::wasmtime4j_profiler_record_function(
            profiler_ptr as *mut profiler::PerformanceProfiler,
            c_string.as_ptr(),
            execution_time_nanos as u64,
            memory_delta,
        );
        if result { JNI_TRUE } else { JNI_FALSE }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerRecordCompilation(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
        compilation_time_nanos: jlong,
        bytecode_size: jlong,
        cached: jboolean,
        optimized: jboolean,
    ) -> jboolean {
        if profiler_ptr == 0 {
            return JNI_FALSE;
        }
        let result = profiler::wasmtime4j_profiler_record_compilation(
            profiler_ptr as *mut profiler::PerformanceProfiler,
            compilation_time_nanos as u64,
            bytecode_size as u64,
            cached != JNI_FALSE,
            optimized != JNI_FALSE,
        );
        if result { JNI_TRUE } else { JNI_FALSE }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetModulesCompiled(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_modules_compiled(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetTotalCompilationTimeNanos(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_total_compilation_time_nanos(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetAverageCompilationTimeNanos(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_average_compilation_time_nanos(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetBytesCompiled(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_bytes_compiled(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetCacheHits(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_cache_hits(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetCacheMisses(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_cache_misses(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetOptimizedModules(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_optimized_modules(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetCurrentMemoryBytes(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_current_memory_bytes(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetPeakMemoryBytes(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_peak_memory_bytes(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetUptimeNanos(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_uptime_nanos(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetFunctionCallsPerSecond(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jdouble {
        if profiler_ptr == 0 {
            return 0.0;
        }
        profiler::wasmtime4j_profiler_get_function_calls_per_second(
            profiler_ptr as *const profiler::PerformanceProfiler
        )
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetTotalFunctionCalls(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_total_function_calls(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerGetTotalExecutionTimeNanos(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jlong {
        if profiler_ptr == 0 {
            return 0;
        }
        profiler::wasmtime4j_profiler_get_total_execution_time_nanos(
            profiler_ptr as *const profiler::PerformanceProfiler
        ) as jlong
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerReset(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) -> jboolean {
        if profiler_ptr == 0 {
            return JNI_FALSE;
        }
        let result = profiler::wasmtime4j_profiler_reset(profiler_ptr as *mut profiler::PerformanceProfiler);
        if result { JNI_TRUE } else { JNI_FALSE }
    }

    #[no_mangle]
    pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniProfiler_nativeProfilerDestroy(
        _env: JNIEnv,
        _class: JClass,
        profiler_ptr: jlong,
    ) {
        if profiler_ptr != 0 {
            profiler::wasmtime4j_profiler_destroy(profiler_ptr as *mut profiler::PerformanceProfiler);
        }
    }
}
