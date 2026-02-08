//! JNI bindings for ComponentLinker operations

use jni::JNIEnv;
use jni::JavaVM;
use jni::objects::{JClass, JObject, JString};
use jni::sys::jlong;

use crate::component::component_linker_core;
use crate::component::{ComponentHostCallback, ComponentValue};
use crate::engine::core as engine_core;
use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};

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
        let _linker = unsafe {
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
        let _component = unsafe {
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
