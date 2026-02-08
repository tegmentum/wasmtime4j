//! JNI bindings for Linker operations

use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jlong, jint, jboolean, jobject, jintArray};
use jni::JavaVM;

use crate::linker::core as linker_core;
use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
use crate::hostfunc::HostFunctionCallback;
use crate::instance::WasmValue;

use std::os::raw::c_void;
use std::sync::Arc;
use wasmtime::{RefType, ValType};

/// Extract the message from a pending Java exception and clear it.
/// Returns the exception message or a default message if extraction fails.
fn extract_and_clear_java_exception(env: &mut jni::JNIEnv) -> String {
    // Get the pending exception
    let exception = match env.exception_occurred() {
        Ok(ex) if !ex.is_null() => ex,
        _ => {
            // No exception or failed to get it - clear any pending state and return default
            let _ = env.exception_clear();
            return "Unknown Java exception".to_string();
        }
    };

    // Clear the exception so we can make JNI calls
    let _ = env.exception_clear();

    // Try to extract the exception message by calling getMessage()
    let message = match env.call_method(&exception, "getMessage", "()Ljava/lang/String;", &[]) {
        Ok(msg_value) => {
            match msg_value.l() {
                Ok(msg_obj) if !msg_obj.is_null() => {
                    match env.get_string((&msg_obj).into()) {
                        Ok(java_str) => java_str.to_string_lossy().into_owned(),
                        Err(_) => "Failed to convert exception message".to_string(),
                    }
                }
                _ => "Exception with null message".to_string(),
            }
        }
        Err(_) => {
            // If getMessage() fails, try to get the class name
            match env.get_object_class(&exception) {
                Ok(cls) => {
                    match env.call_method(&cls, "getName", "()Ljava/lang/String;", &[]) {
                        Ok(name_value) => {
                            match name_value.l() {
                                Ok(name_obj) if !name_obj.is_null() => {
                                    match env.get_string((&name_obj).into()) {
                                        Ok(java_str) => format!("Exception of type: {}", java_str.to_string_lossy()),
                                        Err(_) => "Unknown exception type".to_string(),
                                    }
                                }
                                _ => "Unknown exception".to_string(),
                            }
                        }
                        Err(_) => "Unknown exception".to_string(),
                    }
                }
                Err(_) => "Unknown exception".to_string(),
            }
        }
    };

    // Check if there's another pending exception from our getMessage call and clear it
    if env.exception_check().unwrap_or(false) {
        let _ = env.exception_clear();
    }

    message
}

/// JNI callback implementation for host functions
pub struct JniHostFunctionCallback {
    pub jvm: std::sync::Arc<JavaVM>,
    pub callback_id: i64,
    pub is_function_reference: bool,  // true for FunctionReference, false for Linker host functions
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
            let result = env.call_static_method(
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
            })?;

            // Check for pending Java exception (thrown by host function)
            if env.exception_check().unwrap_or(false) {
                let exception_message = extract_and_clear_java_exception(&mut env);
                log::warn!("Java host function threw exception: {}", exception_message);
                return Err(WasmtimeError::Runtime {
                    message: format!("Host function exception: {}", exception_message),
                    backtrace: None,
                });
            }
            result
        } else {
            // This is a Linker host function - call invokeHostFunctionCallback
            log::debug!("Finding JniLinker class");
            let linker_class = env.find_class("ai/tegmentum/wasmtime4j/jni/JniLinker")
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to find JniLinker class: {}", e),
                    backtrace: None
                })?;
            log::debug!("Calling Java invokeHostFunctionCallback method");
            let result = env.call_static_method(
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
            })?;

            // Check for pending Java exception (thrown by host function)
            if env.exception_check().unwrap_or(false) {
                let exception_message = extract_and_clear_java_exception(&mut env);
                log::warn!("Java host function threw exception: {}", exception_message);
                return Err(WasmtimeError::Runtime {
                    message: format!("Host function exception: {}", exception_message),
                    backtrace: None,
                });
            }
            result
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
        5 => Ok(ValType::Ref(RefType::FUNCREF)),
        6 => Ok(ValType::Ref(RefType::EXTERNREF)),
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
        "FUNCREF" => {
            // Check if value is null (null funcref)
            if value.is_null() {
                Ok(WasmValue::FuncRef(None))
            } else {
                // Get the FunctionReference ID
                let id = env.call_method(&value, "getId", "()J", &[])
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to call getId on FunctionReference: {}", e),
                        backtrace: None
                    })?.j().map_err(|e| WasmtimeError::Runtime {
                        message: format!("Failed to extract funcref id: {}", e),
                        backtrace: None
                    })?;
                Ok(WasmValue::FuncRef(Some(id)))
            }
        },
        "EXTERNREF" => {
            // ExternRef - not fully supported yet, return None
            Ok(WasmValue::ExternRef(None))
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

        // If linker has a WASI context and the store doesn't already have one,
        // attach the linker's context to the store before instantiation.
        // The store may already have a WASI context if the caller (e.g., Panama runtime)
        // explicitly set it before calling instantiate.
        if let Some(wasi_ctx) = linker.get_wasi_context() {
            if !store.has_wasi_context() {
                let fd_manager = crate::wasi::WasiFileDescriptorManager::new();
                store.set_wasi_context(wasi_ctx, fd_manager)?;
                log::debug!("Attached linker's WASI context to store before instantiation");
            } else {
                log::debug!("Store already has WASI context, skipping linker context attachment");
            }
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
        let linker = unsafe { linker_core::get_linker_ref(linker_handle as *const c_void)? };
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };
        let memory = unsafe { crate::memory::core::get_memory_ref(memory_handle as *const c_void)? };

        let mut linker_lock = linker.inner()?;

        // Handle both regular and shared memory
        store.with_context(|ctx| {
            let extern_memory = if let Some(wasmtime_memory) = memory.inner() {
                wasmtime::Extern::Memory(*wasmtime_memory)
            } else if let Some(wasmtime_shared_memory) = memory.inner_shared() {
                wasmtime::Extern::SharedMemory(wasmtime_shared_memory.clone())
            } else {
                return Err(WasmtimeError::Linker {
                    message: format!("Memory '{}::{}' has invalid variant", module_name_str, name_str),
                });
            };

            linker_lock
                .define(ctx, &module_name_str, &name_str, extern_memory)
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
        use wasmtime::{ValType, FuncType};
        use crate::hostfunc::HostFunction;
        use crate::error::debug_log;

        debug_log("nativeDefineHostFunction starting...");

        // Convert parameter types
        let param_val_types: Vec<ValType> = param_vals.iter()
            .map(|&t| int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        // Convert return types
        let return_val_types: Vec<ValType> = return_vals.iter()
            .map(|&t| int_to_valtype(t))
            .collect::<Result<Vec<_>, _>>()?;

        debug_log("Getting linker...");
        // Get linker (mutable because define_host_function needs &mut)
        let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };

        debug_log("Acquiring linker inner lock...");
        // Get engine from wasmtime linker
        let linker_lock = linker.inner()?;
        debug_log("Got linker inner lock, getting engine...");
        let engine = linker_lock.engine();

        debug_log("Creating FuncType...");
        // Create function type
        let func_type = FuncType::new(
            engine,
            param_val_types,
            return_val_types
        );

        debug_log("Dropping linker lock...");
        // Drop lock before creating host function
        drop(linker_lock);
        debug_log("Linker lock dropped");

        debug_log("Creating JNI callback...");
        // Create JNI callback with Arc-wrapped JavaVM
        let callback = JniHostFunctionCallback {
            jvm: std::sync::Arc::new(jvm),
            callback_id,
            is_function_reference: false,  // This is a Linker host function
        };

        debug_log("Creating HostFunction...");
        // Create host function
        let host_func = HostFunction::new(
            format!("{}::{}", module_name_str, name_str),
            func_type,
            Box::new(callback),
        )?;
        debug_log("HostFunction created");

        debug_log("Calling define_host_function...");
        // Register host function - host_func is Arc<HostFunction>, clone it
        let host_func_clone = (*host_func).clone();
        linker.define_host_function(&module_name_str, &name_str, host_func.func_type().clone(), host_func_clone)?;
        debug_log("define_host_function complete");

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

/// Define unknown imports as traps
///
/// Implements any function imports of the module that are not already defined
/// with functions that trap when called.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineUnknownImportsAsTraps(
    mut env: JNIEnv,
    _obj: jobject,
    linker_handle: jlong,
    _store_handle: jlong,
    module_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Get mutable linker reference
        let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };

        // Get store reference - needed to flush pending host functions
        let store = unsafe { crate::store::core::get_store_mut(_store_handle as *mut c_void)? };

        // Flush any pending host functions to the wasmtime linker before
        // calling define_unknown_imports_as_traps, so wasmtime knows which
        // imports are already satisfied and only traps truly unknown ones.
        {
            let mut store_lock = store.lock_store();
            linker.instantiate_host_functions(&mut *store_lock)?;
        }

        // Get module reference and clone the inner wasmtime module
        let module = unsafe { crate::module::core::get_module_ref(module_handle as *const c_void)? };
        let wasmtime_module = module.inner().clone();

        linker.define_unknown_imports_as_traps_wasmtime(&wasmtime_module)?;

        Ok(1) // JNI_TRUE
    })
}

/// Define unknown imports as default values
///
/// Implements any function imports of the module that are not already defined
/// with functions that return default values (zero for numbers, null for references).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeDefineUnknownImportsAsDefaultValues(
    mut env: JNIEnv,
    _obj: jobject,
    linker_handle: jlong,
    store_handle: jlong,
    module_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Get mutable linker reference
        let linker = unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void)? };

        // Get store reference
        let store = unsafe { crate::store::core::get_store_mut(store_handle as *mut c_void)? };

        // Flush pending host functions before defining defaults
        {
            let mut store_lock = store.lock_store();
            linker.instantiate_host_functions(&mut *store_lock)?;
        }

        // Get module reference
        let module = unsafe { crate::module::core::get_module_ref(module_handle as *const c_void)? };

        // Lock store and call the method
        let mut store_lock = store.lock_store();
        linker.define_unknown_imports_as_default_values(&mut *store_lock, module)?;

        Ok(1) // JNI_TRUE
    })
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

/// Set whether name shadowing is allowed in the linker
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeAllowShadowing(
    mut env: JNIEnv,
    _class: JClass,
    linker_handle: jlong,
    allow: jboolean,
) {
    // DEFENSIVE: Validate handle
    if linker_handle == 0 || (linker_handle as usize) < 4096 || (linker_handle as usize) % std::mem::align_of::<usize>() != 0 {
        log::warn!("Attempted to set allow_shadowing on invalid linker handle: {:#x}", linker_handle);
        return;
    }

    match unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void) } {
        Ok(linker) => {
            if let Err(e) = linker.set_allow_shadowing(allow != 0) {
                log::error!("Failed to set allow_shadowing: {:?}", e);
            }
        }
        Err(e) => {
            log::error!("Failed to get linker for allow_shadowing: {:?}", e);
        }
    }
}

/// Set whether unknown exports are allowed during instantiation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniLinker_nativeAllowUnknownExports(
    mut env: JNIEnv,
    _class: JClass,
    linker_handle: jlong,
    allow: jboolean,
) {
    // DEFENSIVE: Validate handle
    if linker_handle == 0 || (linker_handle as usize) < 4096 || (linker_handle as usize) % std::mem::align_of::<usize>() != 0 {
        log::warn!("Attempted to set allow_unknown_exports on invalid linker handle: {:#x}", linker_handle);
        return;
    }

    match unsafe { linker_core::get_linker_mut(linker_handle as *mut c_void) } {
        Ok(linker) => {
            if let Err(e) = linker.set_allow_unknown_exports(allow != 0) {
                log::error!("Failed to set allow_unknown_exports: {:?}", e);
            }
        }
        Err(e) => {
            log::error!("Failed to get linker for allow_unknown_exports: {:?}", e);
        }
    }
}
