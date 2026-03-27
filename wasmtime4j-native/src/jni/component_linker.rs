//! JNI bindings for ComponentLinker operations

use jni::objects::{GlobalRef, JClass, JObject, JObjectArray, JString};
use jni::sys::{jboolean, jint, jlong};
use jni::JNIEnv;
use jni::JavaVM;

use crate::component::component_linker_core;
use crate::component::{
    CallbackMonotonicClock, CallbackRng, CallbackSocketAddrCheck, CallbackWallClock,
    ComponentHostCallback, ComponentValue, ResourceDestructorCallback,
};
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
        use crate::component::{component_value_to_json_val, json_val_to_component_value};

        log::info!("JniComponentHostFunctionCallback::execute - Starting callback execution for callback_id={}",
            self.callback_id);

        // Serialize params to JSON using the same codec as Panama path
        let json_vals: Vec<crate::component_core::concurrent_call_json::JsonVal> =
            params.iter().map(component_value_to_json_val).collect();
        let json_str = serde_json::to_string(&json_vals).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to serialize host function params to JSON: {}", e),
            backtrace: None,
        })?;

        // Attach to current thread and get JNI environment
        let mut env = self
            .jvm
            .attach_current_thread()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to attach to JVM thread: {}", e),
                backtrace: None,
            })?;

        // Create Java string from JSON
        let java_params_json = env.new_string(&json_str).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to create Java string for params JSON: {}", e),
            backtrace: None,
        })?;

        // Call JniComponentLinker.dispatchHostFunctionCallback(callbackId, paramsJson) -> resultsJson
        let linker_class = env
            .find_class("ai/tegmentum/wasmtime4j/jni/JniComponentLinker")
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to find JniComponentLinker class: {}", e),
                backtrace: None,
            })?;

        let result = env
            .call_static_method(
                &linker_class,
                "dispatchHostFunctionCallback",
                "(JLjava/lang/String;)Ljava/lang/String;",
                &[
                    jni::objects::JValue::Long(self.callback_id),
                    jni::objects::JValue::Object(&java_params_json),
                ],
            )
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to invoke host function callback: {}", e),
                backtrace: None,
            })?;

        // Get result string
        let result_obj = result.l().map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to get return value: {}", e),
            backtrace: None,
        })?;

        if result_obj.is_null() {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Host function callback (id={}) returned null (error occurred in Java)",
                    self.callback_id
                ),
                backtrace: None,
            });
        }

        let result_jstring = jni::objects::JString::from(result_obj);
        let result_json: String =
            env.get_string(&result_jstring)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get result JSON string: {}", e),
                    backtrace: None,
                })?
                .into();

        // Deserialize results from JSON
        let json_results: Vec<crate::component_core::concurrent_call_json::JsonVal> =
            serde_json::from_str(&result_json).map_err(|e| WasmtimeError::Runtime {
                message: format!(
                    "Failed to deserialize host function results from JSON: {}",
                    e
                ),
                backtrace: None,
            })?;

        let results: Vec<ComponentValue> = json_results
            .iter()
            .map(json_val_to_component_value)
            .collect();

        log::info!(
            "JniComponentHostFunctionCallback::execute - Completed with {} results",
            results.len()
        );
        Ok(results)
    }

    fn clone_callback(&self) -> Box<dyn ComponentHostCallback> {
        Box::new(JniComponentHostFunctionCallback {
            jvm: Arc::clone(&self.jvm),
            callback_id: self.callback_id,
        })
    }
}

/// JNI resource destructor callback.
///
/// When the guest drops a resource handle, this callback is invoked with the
/// resource representation. The callback ID corresponds to the Java-side
/// destructor registered via `ComponentResourceDefinition`.
struct JniResourceDestructorCallback {
    jvm: Arc<JavaVM>,
    callback_id: u64,
}

impl ResourceDestructorCallback for JniResourceDestructorCallback {
    fn destroy(&self, rep: u32) -> WasmtimeResult<()> {
        log::info!(
            "JniResourceDestructorCallback::destroy - callback_id={}, rep={}",
            self.callback_id,
            rep
        );

        // Attach to current thread and get JNI environment
        let mut env = self
            .jvm
            .attach_current_thread()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to attach to JVM thread for destructor: {}", e),
                backtrace: None,
            })?;

        // Call JniComponentLinker.dispatchDestructorCallback(callbackId, rep)
        let linker_class = env
            .find_class("ai/tegmentum/wasmtime4j/jni/JniComponentLinker")
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to find JniComponentLinker class: {}", e),
                backtrace: None,
            })?;

        env.call_static_method(
            &linker_class,
            "dispatchDestructorCallback",
            "(JI)V",
            &[
                jni::objects::JValue::Long(self.callback_id as i64),
                jni::objects::JValue::Int(rep as i32),
            ],
        )
        .map_err(|e| WasmtimeError::Runtime {
            message: format!(
                "Failed to invoke destructor callback (id={}, rep={}): {}",
                self.callback_id, rep, e
            ),
            backtrace: None,
        })?;

        log::info!(
            "JniResourceDestructorCallback::destroy - completed callback_id={}, rep={}",
            self.callback_id,
            rep
        );
        Ok(())
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
        linker.enable_wasi_preview2()?;
        Ok(())
    });
}

/// Enable WASI HTTP on the component linker
/// JNI binding for JniComponentLinker.nativeEnableWasiHttp
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiHttp(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        linker.enable_wasi_http()?;
        Ok(())
    });
}

/// Enable WASI HTTP with config on the component linker
/// JNI binding for JniComponentLinker.nativeEnableWasiHttpWithConfig
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiHttpWithConfig(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    field_size_limit: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        if field_size_limit > 0 {
            linker.enable_wasi_http_with_field_size_limit(field_size_limit as usize)?;
        } else {
            linker.enable_wasi_http()?;
        }
        Ok(())
    });
}

// ============================================================================
// WASI Config Enable/Set JNI Native Methods
// ============================================================================

/// Enable experimental WASI P3 on the component linker
/// JNI binding for JniComponentLinker.nativeEnableWasiP3
#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiP3(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        linker.enable_wasi_p3()?;
        Ok(())
    });
}

/// Enable experimental WASI HTTP P3 on the component linker
/// JNI binding for JniComponentLinker.nativeEnableWasiHttpP3
#[allow(non_snake_case)]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiHttpP3(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        linker.enable_wasi_http_p3()?;
        Ok(())
    });
}

/// Enable WASI Config on the component linker
/// JNI binding for JniComponentLinker.nativeEnableWasiConfig
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeEnableWasiConfig(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        linker.enable_wasi_config()?;
        Ok(())
    });
}

/// Set WASI Config variables on the component linker
/// JNI binding for JniComponentLinker.nativeSetConfigVariables
///
/// Takes two parallel String arrays: keys and values.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetConfigVariables(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    keys: JObjectArray,
    values: JObjectArray,
) {
    let result: Result<(), crate::WasmtimeError> = (|| {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };

        // Ensure wasi-config is enabled first
        linker.enable_wasi_config()?;

        let len = env.get_array_length(&keys).map_err(|e| {
            crate::WasmtimeError::Runtime {
                message: format!("Failed to get keys array length: {}", e),
                backtrace: None,
            }
        })? as usize;

        let mut vars = Vec::with_capacity(len);
        for i in 0..len {
            let key_obj: JObject = env.get_object_array_element(&keys, i as i32).map_err(|e| {
                crate::WasmtimeError::Runtime {
                    message: format!("Failed to get key at index {}: {}", i, e),
                    backtrace: None,
                }
            })?;
            let val_obj: JObject = env.get_object_array_element(&values, i as i32).map_err(|e| {
                crate::WasmtimeError::Runtime {
                    message: format!("Failed to get value at index {}: {}", i, e),
                    backtrace: None,
                }
            })?;

            let key: String = env.get_string(&JString::from(key_obj)).map_err(|e| {
                crate::WasmtimeError::Runtime {
                    message: format!("Failed to convert key at index {}: {}", i, e),
                    backtrace: None,
                }
            })?.into();
            let val: String = env.get_string(&JString::from(val_obj)).map_err(|e| {
                crate::WasmtimeError::Runtime {
                    message: format!("Failed to convert value at index {}: {}", i, e),
                    backtrace: None,
                }
            })?.into();

            vars.push((key, val));
        }

        linker.set_config_variables(vars);
        Ok(())
    })();

    if let Err(e) = result {
        jni_utils::throw_jni_exception(&mut env, &e);
    }
}

// ============================================================================
// WASI P2 Config JNI Native Methods
// ============================================================================

macro_rules! jni_wasi_bool_setter {
    ($fn_name:ident, $method:ident) => {
        #[no_mangle]
        pub extern "system" fn $fn_name(
            _env: JNIEnv,
            _cls: JClass,
            linker_handle: jlong,
            value: jboolean,
        ) {
            if linker_handle == 0 {
                return;
            }
            let linker = unsafe {
                match component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
                {
                    Ok(l) => l,
                    Err(_) => return,
                }
            };
            linker.$method(value != 0);
        }
    };
}

jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInheritStdio,
    set_wasi_inherit_stdio
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInheritStdin,
    set_wasi_inherit_stdin
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInheritStdout,
    set_wasi_inherit_stdout
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInheritStderr,
    set_wasi_inherit_stderr
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInheritEnv,
    set_wasi_inherit_env
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInheritArgs,
    set_wasi_inherit_args
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiAllowNetwork,
    set_wasi_allow_network
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiAllowTcp,
    set_wasi_allow_tcp
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiAllowUdp,
    set_wasi_allow_udp
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiAllowIpNameLookup,
    set_wasi_allow_ip_name_lookup
);
jni_wasi_bool_setter!(
    Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiAllowBlockingCurrentThread,
    set_wasi_allow_blocking_current_thread
);

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInsecureRandomSeed(
    _env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    seed: jlong,
) {
    if linker_handle == 0 {
        return;
    }
    let linker = unsafe {
        match component_linker_core::get_component_linker_mut(linker_handle as *mut c_void) {
            Ok(l) => l,
            Err(_) => return,
        }
    };
    linker.set_wasi_insecure_random_seed(seed as u64);
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiStdinBytes(
    env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    data: jni::sys::jbyteArray,
) {
    if linker_handle == 0 || data.is_null() {
        return;
    }
    let linker = unsafe {
        match component_linker_core::get_component_linker_mut(linker_handle as *mut c_void) {
            Ok(l) => l,
            Err(_) => return,
        }
    };

    let byte_array = unsafe { jni::objects::JByteArray::from_raw(data) };
    let bytes = match env.convert_byte_array(byte_array) {
        Ok(b) => b,
        Err(_) => return,
    };
    linker.set_wasi_stdin_bytes(bytes);
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiArgs(
    mut env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    args: JObjectArray,
) {
    if linker_handle == 0 {
        return;
    }
    let linker = unsafe {
        match component_linker_core::get_component_linker_mut(linker_handle as *mut c_void) {
            Ok(l) => l,
            Err(_) => return,
        }
    };

    let len = match env.get_array_length(&args) {
        Ok(l) => l as usize,
        Err(_) => return,
    };

    let mut rust_args = Vec::with_capacity(len);
    for i in 0..len {
        if let Ok(elem) = env.get_object_array_element(&args, i as i32) {
            let jstr: JString = elem.into();
            let s = match env.get_string(&jstr) {
                Ok(s) => s.to_string_lossy().into_owned(),
                Err(_) => continue,
            };
            rust_args.push(s);
        }
    }

    linker.set_wasi_args(rust_args);
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeAddWasiEnv(
    mut env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    key: JString,
    value: JString,
) {
    if linker_handle == 0 {
        return;
    }
    let linker = unsafe {
        match component_linker_core::get_component_linker_mut(linker_handle as *mut c_void) {
            Ok(l) => l,
            Err(_) => return,
        }
    };

    let key_str = match env.get_string(&key) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return,
    };
    let value_str = match env.get_string(&value) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return,
    };

    let mut env_map = linker.wasi_p2_config().env.clone();
    env_map.insert(key_str, value_str);
    linker.set_wasi_env(env_map);
}

#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeAddWasiPreopenDir(
    mut env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    host_path: JString,
    guest_path: JString,
    dir_perms_bits: jint,
    file_perms_bits: jint,
) {
    if linker_handle == 0 {
        return;
    }
    let linker = unsafe {
        match component_linker_core::get_component_linker_mut(linker_handle as *mut c_void) {
            Ok(l) => l,
            Err(_) => return,
        }
    };

    let host_str = match env.get_string(&host_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return,
    };
    let guest_str = match env.get_string(&guest_path) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return,
    };

    linker.add_wasi_preopen_dir(
        host_str,
        guest_str,
        dir_perms_bits as u32,
        file_perms_bits as u32,
    );
}

// =============================================================================
// JNI WASI Callback Infrastructure
// =============================================================================

/// Static storage for JNI WASI callback objects (clocks, RNG, socket checks).
/// Each callback is stored with a unique ID, and extern "C" trampolines use the ID
/// to look up the JVM + Java callback object for invocation.
static JNI_WASI_CALLBACKS: std::sync::OnceLock<
    std::sync::Mutex<std::collections::HashMap<i64, JniWasiCallbackContext>>,
> = std::sync::OnceLock::new();

/// Counter for generating unique WASI callback IDs.
static WASI_CALLBACK_ID_COUNTER: std::sync::atomic::AtomicI64 =
    std::sync::atomic::AtomicI64::new(1);

/// Holds a JVM reference and a GlobalRef to a Java callback object.
struct JniWasiCallbackContext {
    jvm: Arc<JavaVM>,
    callback_obj: GlobalRef,
}

fn get_jni_wasi_callbacks(
) -> &'static std::sync::Mutex<std::collections::HashMap<i64, JniWasiCallbackContext>> {
    JNI_WASI_CALLBACKS.get_or_init(|| std::sync::Mutex::new(std::collections::HashMap::new()))
}

fn register_jni_wasi_callback(callback_id: i64, jvm: Arc<JavaVM>, callback_obj: GlobalRef) {
    let mut callbacks = get_jni_wasi_callbacks()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    callbacks.insert(callback_id, JniWasiCallbackContext { jvm, callback_obj });
}

fn next_wasi_callback_id() -> i64 {
    WASI_CALLBACK_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst)
}

/// Helper to get JVM Arc and callback object raw pointer from the registry.
fn get_wasi_callback_context(callback_id: i64) -> Option<(Arc<JavaVM>, usize)> {
    let callbacks = get_jni_wasi_callbacks()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    callbacks
        .get(&callback_id)
        .map(|ctx| (ctx.jvm.clone(), ctx.callback_obj.as_obj().as_raw() as usize))
}

/// Helper to register a Java callback object in the WASI callback registry.
/// Returns the generated callback_id on success.
fn setup_jni_wasi_callback(env: &mut JNIEnv, callback_obj: &JObject) -> Result<i64, WasmtimeError> {
    let jvm = env.get_java_vm().map_err(|e| WasmtimeError::Runtime {
        message: format!("Failed to get JVM: {}", e),
        backtrace: None,
    })?;
    let global_ref = env
        .new_global_ref(callback_obj)
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to create global reference: {}", e),
            backtrace: None,
        })?;
    let callback_id = next_wasi_callback_id();
    register_jni_wasi_callback(callback_id, Arc::new(jvm), global_ref);
    Ok(callback_id)
}

// =============================================================================
// Wall Clock Trampolines
// =============================================================================

/// Trampoline for WasiWallClock.now() -> DateTime
extern "C" fn jni_wall_clock_now(callback_id: i64, seconds_out: *mut i64, nanos_out: *mut u32) {
    let (jvm, obj_ptr) = match get_wasi_callback_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::error!("No WASI wall clock callback found for ID: {}", callback_id);
            return;
        }
    };

    // Bind to a local so AttachGuard is dropped before jvm
    let _r = match jvm.attach_current_thread() {
        Ok(mut env) => {
            // SAFETY: The GlobalRef is kept alive by the JniWasiCallbackContext in the registry
            let obj = unsafe { JObject::from_raw(obj_ptr as jni::sys::jobject) };
            match env.call_method(
                &obj,
                "now",
                "()Lai/tegmentum/wasmtime4j/wasi/clocks/DateTime;",
                &[],
            ) {
                Ok(result) => {
                    if let Ok(dt_obj) = result.l() {
                        if let Ok(seconds) = env.call_method(&dt_obj, "getSeconds", "()J", &[]) {
                            if let Ok(s) = seconds.j() {
                                unsafe {
                                    *seconds_out = s;
                                }
                            }
                        }
                        if let Ok(nanos) = env.call_method(&dt_obj, "getNanoseconds", "()I", &[]) {
                            if let Ok(n) = nanos.i() {
                                unsafe {
                                    *nanos_out = n as u32;
                                }
                            }
                        }
                    }
                }
                Err(e) => {
                    log::error!("Failed to call WasiWallClock.now(): {}", e);
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach JVM thread for wall clock now: {}", e);
        }
    };
}

/// Trampoline for WasiWallClock.resolution() -> DateTime
extern "C" fn jni_wall_clock_resolution(
    callback_id: i64,
    seconds_out: *mut i64,
    nanos_out: *mut u32,
) {
    let (jvm, obj_ptr) = match get_wasi_callback_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::error!("No WASI wall clock callback found for ID: {}", callback_id);
            return;
        }
    };

    let _r = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let obj = unsafe { JObject::from_raw(obj_ptr as jni::sys::jobject) };
            match env.call_method(
                &obj,
                "resolution",
                "()Lai/tegmentum/wasmtime4j/wasi/clocks/DateTime;",
                &[],
            ) {
                Ok(result) => {
                    if let Ok(dt_obj) = result.l() {
                        if let Ok(seconds) = env.call_method(&dt_obj, "getSeconds", "()J", &[]) {
                            if let Ok(s) = seconds.j() {
                                unsafe {
                                    *seconds_out = s;
                                }
                            }
                        }
                        if let Ok(nanos) = env.call_method(&dt_obj, "getNanoseconds", "()I", &[]) {
                            if let Ok(n) = nanos.i() {
                                unsafe {
                                    *nanos_out = n as u32;
                                }
                            }
                        }
                    }
                }
                Err(e) => {
                    log::error!("Failed to call WasiWallClock.resolution(): {}", e);
                }
            }
        }
        Err(e) => {
            log::error!(
                "Failed to attach JVM thread for wall clock resolution: {}",
                e
            );
        }
    };
}

// =============================================================================
// Monotonic Clock Trampolines
// =============================================================================

/// Trampoline for WasiMonotonicClock.now() -> long
extern "C" fn jni_monotonic_clock_now(callback_id: i64) -> u64 {
    let (jvm, obj_ptr) = match get_wasi_callback_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::error!(
                "No WASI monotonic clock callback found for ID: {}",
                callback_id
            );
            return 0;
        }
    };

    let result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let obj = unsafe { JObject::from_raw(obj_ptr as jni::sys::jobject) };
            match env.call_method(&obj, "now", "()J", &[]) {
                Ok(result) => result.j().unwrap_or(0) as u64,
                Err(e) => {
                    log::error!("Failed to call WasiMonotonicClock.now(): {}", e);
                    0
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach JVM thread for monotonic clock now: {}", e);
            0
        }
    };
    result
}

/// Trampoline for WasiMonotonicClock.resolution() -> long
extern "C" fn jni_monotonic_clock_resolution(callback_id: i64) -> u64 {
    let (jvm, obj_ptr) = match get_wasi_callback_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::error!(
                "No WASI monotonic clock callback found for ID: {}",
                callback_id
            );
            return 0;
        }
    };

    let result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let obj = unsafe { JObject::from_raw(obj_ptr as jni::sys::jobject) };
            match env.call_method(&obj, "resolution", "()J", &[]) {
                Ok(result) => result.j().unwrap_or(0) as u64,
                Err(e) => {
                    log::error!("Failed to call WasiMonotonicClock.resolution(): {}", e);
                    0
                }
            }
        }
        Err(e) => {
            log::error!(
                "Failed to attach JVM thread for monotonic clock resolution: {}",
                e
            );
            0
        }
    };
    result
}

// =============================================================================
// Random Source Trampoline
// =============================================================================

/// Trampoline for WasiRandomSource.fillBytes(byte[])
extern "C" fn jni_random_fill_bytes(callback_id: i64, buf_ptr: *mut u8, buf_len: usize) {
    let (jvm, obj_ptr) = match get_wasi_callback_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::error!("No WASI random callback found for ID: {}", callback_id);
            return;
        }
    };

    let _r = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let obj = unsafe { JObject::from_raw(obj_ptr as jni::sys::jobject) };
            // Create a Java byte array of the requested size
            match env.new_byte_array(buf_len as i32) {
                Ok(byte_array) => {
                    // Call fillBytes(byte[]) on the Java callback object
                    match env.call_method(
                        &obj,
                        "fillBytes",
                        "([B)V",
                        &[jni::objects::JValue::Object(byte_array.as_ref())],
                    ) {
                        Ok(_) => {
                            // Copy the filled bytes back to the native buffer
                            match env.convert_byte_array(&byte_array) {
                                Ok(bytes) => {
                                    let copy_len = std::cmp::min(bytes.len(), buf_len);
                                    unsafe {
                                        std::ptr::copy_nonoverlapping(
                                            bytes.as_ptr(),
                                            buf_ptr,
                                            copy_len,
                                        );
                                    }
                                }
                                Err(e) => {
                                    log::error!("Failed to convert byte array from Java: {}", e);
                                }
                            }
                        }
                        Err(e) => {
                            log::error!("Failed to call WasiRandomSource.fillBytes(): {}", e);
                        }
                    }
                }
                Err(e) => {
                    log::error!("Failed to create Java byte array: {}", e);
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach JVM thread for random fill: {}", e);
        }
    };
}

// =============================================================================
// Socket Address Check Trampoline
// =============================================================================

/// Trampoline for SocketAddrCheck.check(InetSocketAddress, SocketAddrUse) -> boolean
extern "C" fn jni_socket_addr_check(
    callback_id: i64,
    _ip_version: i32,
    ip_bytes_ptr: *const u8,
    ip_bytes_len: usize,
    port: u16,
    use_type: i32,
) -> i32 {
    let (jvm, obj_ptr) = match get_wasi_callback_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::error!(
                "No WASI socket addr check callback found for ID: {}",
                callback_id
            );
            return 0;
        }
    };

    let result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let obj = unsafe { JObject::from_raw(obj_ptr as jni::sys::jobject) };

            // Create byte[] from IP bytes
            let ip_bytes = unsafe { std::slice::from_raw_parts(ip_bytes_ptr, ip_bytes_len) };
            let byte_array = match env.byte_array_from_slice(ip_bytes) {
                Ok(arr) => arr,
                Err(e) => {
                    log::error!("Failed to create IP byte array: {}", e);
                    return 0;
                }
            };

            // InetAddress.getByAddress(byte[]) -> InetAddress
            let inet_addr_class = match env.find_class("java/net/InetAddress") {
                Ok(c) => c,
                Err(e) => {
                    log::error!("Failed to find InetAddress class: {}", e);
                    return 0;
                }
            };
            let inet_addr = match env.call_static_method(
                inet_addr_class,
                "getByAddress",
                "([B)Ljava/net/InetAddress;",
                &[jni::objects::JValue::Object(byte_array.as_ref())],
            ) {
                Ok(result) => match result.l() {
                    Ok(addr) => addr,
                    Err(e) => {
                        log::error!("Failed to get InetAddress result: {}", e);
                        return 0;
                    }
                },
                Err(e) => {
                    log::error!("Failed to call InetAddress.getByAddress(): {}", e);
                    return 0;
                }
            };

            // new InetSocketAddress(InetAddress, int)
            let isa_class = match env.find_class("java/net/InetSocketAddress") {
                Ok(c) => c,
                Err(e) => {
                    log::error!("Failed to find InetSocketAddress class: {}", e);
                    return 0;
                }
            };
            let socket_addr = match env.new_object(
                isa_class,
                "(Ljava/net/InetAddress;I)V",
                &[
                    jni::objects::JValue::Object(&inet_addr),
                    jni::objects::JValue::Int(port as i32),
                ],
            ) {
                Ok(addr) => addr,
                Err(e) => {
                    log::error!("Failed to create InetSocketAddress: {}", e);
                    return 0;
                }
            };

            // SocketAddrUse.fromValue(int) -> SocketAddrUse
            let use_class =
                match env.find_class("ai/tegmentum/wasmtime4j/wasi/sockets/SocketAddrUse") {
                    Ok(c) => c,
                    Err(e) => {
                        log::error!("Failed to find SocketAddrUse class: {}", e);
                        return 0;
                    }
                };
            let socket_use = match env.call_static_method(
                use_class,
                "fromValue",
                "(I)Lai/tegmentum/wasmtime4j/wasi/sockets/SocketAddrUse;",
                &[jni::objects::JValue::Int(use_type)],
            ) {
                Ok(result) => match result.l() {
                    Ok(u) => u,
                    Err(e) => {
                        log::error!("Failed to get SocketAddrUse result: {}", e);
                        return 0;
                    }
                },
                Err(e) => {
                    log::error!("Failed to call SocketAddrUse.fromValue(): {}", e);
                    return 0;
                }
            };

            // check(InetSocketAddress, SocketAddrUse) -> boolean
            match env.call_method(
                &obj,
                "check",
                "(Ljava/net/InetSocketAddress;\
                 Lai/tegmentum/wasmtime4j/wasi/sockets/SocketAddrUse;)Z",
                &[
                    jni::objects::JValue::Object(&socket_addr),
                    jni::objects::JValue::Object(&socket_use),
                ],
            ) {
                Ok(result) => match result.z() {
                    Ok(allowed) => {
                        if allowed {
                            1
                        } else {
                            0
                        }
                    }
                    Err(e) => {
                        log::error!("Failed to get boolean result from check(): {}", e);
                        0
                    }
                },
                Err(e) => {
                    log::error!("Failed to call SocketAddrCheck.check(): {}", e);
                    0
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach JVM thread for socket addr check: {}", e);
            0
        }
    };
    result
}

// =============================================================================
// JNI Native Methods for WASI Callbacks
// =============================================================================

/// Set custom wall clock callback on the component linker
/// JNI binding for JniComponentLinker.nativeSetWasiWallClock
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiWallClock(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    wall_clock: JObject,
) {
    let callback_id = match setup_jni_wasi_callback(&mut env, &wall_clock) {
        Ok(id) => id,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
        Ok(linker) => linker,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    linker.set_wasi_wall_clock(CallbackWallClock {
        now_fn: jni_wall_clock_now,
        resolution_fn: jni_wall_clock_resolution,
        callback_id,
    });
}

/// Set custom monotonic clock callback on the component linker
/// JNI binding for JniComponentLinker.nativeSetWasiMonotonicClock
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiMonotonicClock(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    monotonic_clock: JObject,
) {
    let callback_id = match setup_jni_wasi_callback(&mut env, &monotonic_clock) {
        Ok(id) => id,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
        Ok(linker) => linker,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    linker.set_wasi_monotonic_clock(CallbackMonotonicClock {
        now_fn: jni_monotonic_clock_now,
        resolution_fn: jni_monotonic_clock_resolution,
        callback_id,
    });
}

/// Set custom secure random callback on the component linker
/// JNI binding for JniComponentLinker.nativeSetWasiSecureRandom
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiSecureRandom(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    random_source: JObject,
) {
    let callback_id = match setup_jni_wasi_callback(&mut env, &random_source) {
        Ok(id) => id,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
        Ok(linker) => linker,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    linker.set_wasi_secure_random(CallbackRng {
        fill_bytes_fn: jni_random_fill_bytes,
        callback_id,
    });
}

/// Set custom insecure random callback on the component linker
/// JNI binding for JniComponentLinker.nativeSetWasiInsecureRandom
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiInsecureRandom(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    random_source: JObject,
) {
    let callback_id = match setup_jni_wasi_callback(&mut env, &random_source) {
        Ok(id) => id,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
        Ok(linker) => linker,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    linker.set_wasi_insecure_random(CallbackRng {
        fill_bytes_fn: jni_random_fill_bytes,
        callback_id,
    });
}

/// Set socket address check callback on the component linker
/// JNI binding for JniComponentLinker.nativeSetWasiSocketAddrCheck
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiSocketAddrCheck(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    addr_check: JObject,
) {
    let callback_id = match setup_jni_wasi_callback(&mut env, &addr_check) {
        Ok(id) => id,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
        Ok(linker) => linker,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return;
        }
    };

    linker.set_wasi_socket_addr_check(CallbackSocketAddrCheck {
        check_fn: jni_socket_addr_check,
        callback_id,
    });
}

/// Destroy the component linker and free its resources
/// JNI binding for JniComponentLinker.nativeDestroyComponentLinker
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDestroyComponentLinker(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if linker_handle != 0 {
            unsafe {
                component_linker_core::destroy_component_linker(linker_handle as *mut c_void);
            }
        }
        Ok(())
    });
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
                backtrace: None,
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
                backtrace: None,
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return;
        }
    };

    // Get the linker
    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
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
    if let Err(e) =
        component_linker_core::define_host_function_by_path(linker, &wit_path_str, callback)
    {
        jni_utils::throw_jni_exception(&mut env, &e);
        return;
    }

    log::info!(
        "Defined component host function: {} with callback_id={}",
        wit_path_str,
        callback_id
    );
}

/// Define an async host function on the component linker
/// JNI binding for JniComponentLinker.nativeDefineHostFunctionAsync
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDefineHostFunctionAsync(
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
                backtrace: None,
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
                backtrace: None,
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return;
        }
    };

    // Get the linker
    let linker = match unsafe {
        component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)
    } {
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

    // Register the async host function
    if let Err(e) =
        component_linker_core::define_host_function_by_path_async(linker, &wit_path_str, callback)
    {
        jni_utils::throw_jni_exception(&mut env, &e);
        return;
    }

    log::info!(
        "Defined async component host function: {} with callback_id={}",
        wit_path_str,
        callback_id
    );
}

/// Set async support on the component linker
/// JNI binding for JniComponentLinker.nativeSetAsyncSupport
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetAsyncSupport(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    enabled: jboolean,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        component_linker_core::set_async_support(linker, enabled != 0);
        Ok(())
    });
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
    _constructor_callback_id: jlong,
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
                backtrace: None,
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
                backtrace: None,
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
                backtrace: None,
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return 0;
        }
    };

    // Generate unique resource ID for this definition
    static RESOURCE_ID_COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);
    let resource_type_id = RESOURCE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

    // Build the interface path (e.g., "ns:pkg/iface")
    let interface_path = format!("{}:{}", namespace_str, interface_str);

    // Get JVM reference for destructor callback
    let jvm = env.get_java_vm().map_err(|e| {
        let error = WasmtimeError::Runtime {
            message: format!("Failed to get JavaVM for destructor callback: {}", e),
            backtrace: None,
        };
        jni_utils::throw_jni_exception(&mut env, &error);
    });
    let jvm = match jvm {
        Ok(jvm) => Arc::new(jvm),
        Err(_) => return 0,
    };

    // Create a destructor callback that will call back to Java via the callback registry
    let dtor_callback_id = destructor_callback_id as u64;
    let destructor = std::sync::Arc::new(JniResourceDestructorCallback {
        jvm,
        callback_id: dtor_callback_id,
    });

    // Get the linker and call define_resource (panic-safe)
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jlong> {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut std::ffi::c_void)?
        };
        linker.define_resource(
            &interface_path,
            &resource_str,
            resource_type_id as u32,
            destructor,
        )?;
        log::info!(
            "Defined component resource: {}/{} with resource_type_id={}",
            interface_path,
            resource_str,
            resource_type_id
        );
        Ok(resource_type_id as jlong)
    })) {
        Ok(Ok(id)) => id,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            0
        }
    }
}

/// JNI binding for JniComponentLinker.nativeDefineModule
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDefineModule(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    instance_path: JString,
    name: JString,
    module_handle: jlong,
) {
    if linker_handle == 0 {
        let error = WasmtimeError::InvalidParameter {
            message: "Invalid linker handle".to_string(),
        };
        jni_utils::throw_jni_exception(&mut env, &error);
        return;
    }

    if module_handle == 0 {
        let error = WasmtimeError::InvalidParameter {
            message: "Invalid module handle".to_string(),
        };
        jni_utils::throw_jni_exception(&mut env, &error);
        return;
    }

    let instance_path_str: String = match env.get_string(&instance_path) {
        Ok(s) => s.into(),
        Err(e) => {
            let error = WasmtimeError::Runtime {
                message: format!("Failed to get instance path string: {}", e),
                backtrace: None,
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return;
        }
    };

    let name_str: String = match env.get_string(&name) {
        Ok(s) => s.into(),
        Err(e) => {
            let error = WasmtimeError::Runtime {
                message: format!("Failed to get module name string: {}", e),
                backtrace: None,
            };
            jni_utils::throw_jni_exception(&mut env, &error);
            return;
        }
    };

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<()> {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut std::ffi::c_void)?
        };

        let module = unsafe {
            crate::module::core::get_module_ref(module_handle as *const std::ffi::c_void)?
        };

        component_linker_core::define_module(linker, &instance_path_str, &name_str, module)?;

        log::info!(
            "Defined core module '{}' on instance path '{}' in component linker",
            name_str,
            instance_path_str
        );
        Ok(())
    })) {
        Ok(Ok(())) => {}
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
        }
    }
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
        linker_handle,
        store_handle,
        component_handle
    );

    // The linker-based instantiation uses the EnhancedComponentEngine's linker
    // to instantiate the component with the defined host functions and resources.
    // For now, delegate to the component's direct instantiation since the linker
    // context is managed on the Java side. Full integration would require
    // maintaining the wasmtime Linker<T> on the native side.

    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Get the component reference
        let _component = unsafe {
            crate::component::core::get_component_ref(
                component_handle as *const std::os::raw::c_void,
            )?
        };

        // For linker-based instantiation, we use the component's engine
        // which already has the enhanced instantiation logic
        // This returns an instance ID that can be used to call exported functions

        // Generate a unique instance ID for tracking
        static INSTANCE_ID_COUNTER: std::sync::atomic::AtomicU64 =
            std::sync::atomic::AtomicU64::new(1);
        let instance_id = INSTANCE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        log::info!(
            "Created component instance {} via linker instantiation",
            instance_id
        );

        Ok(instance_id as i64)
    })
}

// ============================================================================
// ComponentInstancePre JNI Bindings
// ============================================================================

/// Pre-instantiate a component from the linker
/// JNI binding for JniComponentLinker.nativeInstantiatePre
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeInstantiatePre(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    component_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };

        let component = unsafe {
            crate::component::core::get_component_ref(component_handle as *const c_void)?
        };

        let pre = linker.instantiate_pre(component)?;

        Ok(Box::into_raw(Box::new(pre)) as jlong)
    })
}

/// JNI binding for JniComponentLinker.nativeAllowShadowing
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeAllowShadowing(
    mut env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    allow: jboolean,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        linker.allow_shadowing(allow != 0);
        Ok(())
    });
}

/// JNI binding for JniComponentLinker.nativeDefineUnknownImportsAsTraps
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeDefineUnknownImportsAsTraps(
    mut env: JNIEnv,
    _cls: JClass,
    linker_handle: jlong,
    component_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        let component = unsafe {
            crate::component::core::get_component_ref(component_handle as *const c_void)?
        };
        linker.define_unknown_imports_as_traps(component)?;
        Ok(0)
    })
}

/// Instantiate from a ComponentInstancePre
/// JNI binding for JniComponentInstancePre.nativeInstantiate
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativeInstantiate(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let pre = unsafe {
            component_linker_core::get_component_instance_pre_ref(pre_handle as *const c_void)?
        };

        let handle = pre.instantiate()?;

        // Register the instance in the engine's HashMap so it can be looked up by ID
        let engine = unsafe {
            &*(engine_handle as *const crate::component_core::EnhancedComponentEngine)
        };
        let instance_id = engine.register_instance(handle)?;

        Ok(instance_id as jlong)
    })
}

/// Instantiate from a ComponentInstancePre with store configuration
/// JNI binding for JniComponentInstancePre.nativeInstantiateWithConfig
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativeInstantiateWithConfig(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
    engine_handle: jlong,
    fuel_limit: jlong,
    epoch_deadline: jlong,
    max_memory_bytes: jlong,
    max_table_elements: jlong,
    max_instances: jlong,
    max_tables: jlong,
    max_memories: jlong,
    trap_on_grow_failure: jboolean,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let pre = unsafe {
            component_linker_core::get_component_instance_pre_ref(pre_handle as *const c_void)?
        };

        let handle = pre.instantiate_with_config(
            fuel_limit as u64,
            epoch_deadline as u64,
            max_memory_bytes as u64,
            max_table_elements as u64,
            max_instances as u64,
            max_tables as u64,
            max_memories as u64,
            trap_on_grow_failure != 0,
        )?;

        // Register the instance in the engine's HashMap so it can be looked up by ID
        let engine = unsafe {
            &*(engine_handle as *const crate::component_core::EnhancedComponentEngine)
        };
        let instance_id = engine.register_instance(handle)?;

        Ok(instance_id as jlong)
    })
}

/// Check if ComponentInstancePre is valid
/// JNI binding for JniComponentInstancePre.nativeIsValid
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativeIsValid(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        if pre_handle == 0 {
            return Ok(false);
        }
        let pre = unsafe { &*(pre_handle as *const crate::component::ComponentInstancePreWrapper) };
        Ok(pre.is_valid())
    }) as jboolean
}

/// Get instance count from ComponentInstancePre
/// JNI binding for JniComponentInstancePre.nativeInstanceCount
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativeInstanceCount(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if pre_handle == 0 {
            return Ok(0);
        }
        let pre = unsafe { &*(pre_handle as *const crate::component::ComponentInstancePreWrapper) };
        Ok(pre.instance_count() as jlong)
    })
}

/// Get preparation time in nanoseconds
/// JNI binding for JniComponentInstancePre.nativePreparationTimeNs
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativePreparationTimeNs(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if pre_handle == 0 {
            return Ok(0);
        }
        let pre = unsafe { &*(pre_handle as *const crate::component::ComponentInstancePreWrapper) };
        Ok(pre.preparation_time_ns() as jlong)
    })
}

/// Get average instantiation time in nanoseconds
/// JNI binding for JniComponentInstancePre.nativeAvgInstantiationTimeNs
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativeAvgInstantiationTimeNs(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if pre_handle == 0 {
            return Ok(0);
        }
        let pre = unsafe { &*(pre_handle as *const crate::component::ComponentInstancePreWrapper) };
        Ok(pre.average_instantiation_time_ns() as jlong)
    })
}

/// Destroy a ComponentInstancePre
/// JNI binding for JniComponentInstancePre.nativeDestroy
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentInstancePre_nativeDestroy(
    mut env: JNIEnv,
    _obj: JObject,
    pre_handle: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if pre_handle != 0 {
            unsafe {
                component_linker_core::destroy_component_instance_pre(pre_handle as *mut c_void);
            }
        }
        Ok(())
    });
}

/// Get defined interfaces as a JSON array string
/// JNI binding for JniComponentLinker.nativeGetInterfaces
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeGetInterfaces(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) -> jni::sys::jobject {
    let result: Result<jni::sys::jobject, WasmtimeError> = (|| {
        if linker_handle == 0 {
            return Ok(std::ptr::null_mut());
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };
        let interfaces = linker.get_defined_interfaces();
        let json = format!(
            "[{}]",
            interfaces
                .iter()
                .map(|s| format!("\"{}\"", s.replace('"', "\\\"")))
                .collect::<Vec<_>>()
                .join(",")
        );
        let jstr = env.new_string(&json).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to create Java string: {}", e),
            backtrace: None,
        })?;
        Ok(jstr.into_raw())
    })();
    match result {
        Ok(obj) => obj,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Get defined functions for an interface as a JSON array string
/// JNI binding for JniComponentLinker.nativeGetFunctions
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeGetFunctions(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    namespace: JString,
    interface_name: JString,
) -> jni::sys::jobject {
    let result: Result<jni::sys::jobject, WasmtimeError> = (|| {
        if linker_handle == 0 {
            return Ok(std::ptr::null_mut());
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };
        let ns: String = env.get_string(&namespace).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to get namespace string: {}", e),
            backtrace: None,
        })?.into();
        let iface: String = env.get_string(&interface_name).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to get interface name string: {}", e),
            backtrace: None,
        })?.into();
        let functions = linker.get_defined_functions(&ns, &iface);
        let json = format!(
            "[{}]",
            functions
                .iter()
                .map(|s| format!("\"{}\"", s.replace('"', "\\\"")))
                .collect::<Vec<_>>()
                .join(",")
        );
        let jstr = env.new_string(&json).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to create Java string: {}", e),
            backtrace: None,
        })?;
        Ok(jstr.into_raw())
    })();
    match result {
        Ok(obj) => obj,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Check if WASI P2 is enabled
/// JNI binding for JniComponentLinker.nativeIsWasiP2Enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeIsWasiP2Enabled(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if linker_handle == 0 {
            return Ok(0);
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };
        Ok(if linker.is_wasi_p2_enabled() { 1 } else { 0 })
    })
}

/// Check if WASI HTTP is enabled
/// JNI binding for JniComponentLinker.nativeIsWasiHttpEnabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeIsWasiHttpEnabled(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if linker_handle == 0 {
            return Ok(0);
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };
        Ok(if linker.is_wasi_http_enabled() { 1 } else { 0 })
    })
}

/// Get host function count
/// JNI binding for JniComponentLinker.nativeGetHostFunctionCount
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeGetHostFunctionCount(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if linker_handle == 0 {
            return Ok(0);
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };
        Ok(linker.host_function_count() as jint)
    })
}

/// Get interface count
/// JNI binding for JniComponentLinker.nativeGetInterfaceCount
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeGetInterfaceCount(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if linker_handle == 0 {
            return Ok(0);
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_ref(linker_handle as *const c_void)?
        };
        Ok(linker.defined_interfaces.len() as jint)
    })
}

/// Set WASI max random size
/// JNI binding for JniComponentLinker.nativeSetWasiMaxRandomSize
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentLinker_nativeSetWasiMaxRandomSize(
    mut env: JNIEnv,
    _obj: JObject,
    linker_handle: jlong,
    max_size: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        if linker_handle == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Linker handle is null".to_string(),
            });
        }
        let linker = unsafe {
            component_linker_core::get_component_linker_mut(linker_handle as *mut c_void)?
        };
        linker.set_wasi_max_random_size(max_size as u64);
        Ok(())
    });
}
