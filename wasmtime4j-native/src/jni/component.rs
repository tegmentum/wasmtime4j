//! JNI bindings for Component operations (WASI Preview 2)
//!
//! This module provides JNI bindings for WebAssembly Component Model operations
//! including component creation, instantiation, and function invocation.

use jni::objects::{JByteArray, JClass, JObject, JString};
use jni::sys::{jboolean, jbyteArray, jint, jintArray, jlong, jlongArray, jobjectArray, jstring};
use jni::JNIEnv;

use crate::component::Component;
use crate::error::jni_utils;

/// Temporary env-gated phase profiler for decomposing component-invoke cost.
/// Enabled when the `W4J_PROFILE` env var is set. Prototype instrumentation only.
pub(crate) mod invoke_profile {
    use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
    use std::time::Instant;

    pub static SETUP: AtomicU64 = AtomicU64::new(0);
    pub static ARGDESER: AtomicU64 = AtomicU64::new(0);
    pub static GETFUNC: AtomicU64 = AtomicU64::new(0);
    pub static TY: AtomicU64 = AtomicU64::new(0);
    pub static CALL: AtomicU64 = AtomicU64::new(0);
    pub static REPLY: AtomicU64 = AtomicU64::new(0);
    pub static COUNT: AtomicU64 = AtomicU64::new(0);

    pub fn enabled() -> bool {
        static ENABLED: AtomicBool = AtomicBool::new(false);
        static INIT: AtomicBool = AtomicBool::new(false);
        if !INIT.swap(true, Ordering::Relaxed) {
            ENABLED.store(std::env::var("W4J_PROFILE").is_ok(), Ordering::Relaxed);
        }
        ENABLED.load(Ordering::Relaxed)
    }

    #[inline]
    pub fn add(acc: &AtomicU64, since: Instant) -> Instant {
        let now = Instant::now();
        acc.fetch_add(now.duration_since(since).as_nanos() as u64, Ordering::Relaxed);
        now
    }

    pub fn tick_and_maybe_report() {
        let n = COUNT.fetch_add(1, Ordering::Relaxed) + 1;
        if n % 500_000 == 0 {
            let g = |a: &AtomicU64| a.load(Ordering::Relaxed) as f64 / n as f64;
            eprintln!(
                "[W4J_PROFILE n={}] setup={:.0} argdeser={:.0} getfunc={:.0} ty={:.0} call={:.0} reply={:.0} (ns/call)",
                n, g(&SETUP), g(&ARGDESER), g(&GETFUNC), g(&TY), g(&CALL), g(&REPLY)
            );
        }
    }
}

/// Process-global cache of the JNI references used to build the invoke reply
/// (`Object[]{Integer, byte[]}`). Resolving `java/lang/Object`, `java/lang/Integer`
/// and the `Integer(int)` constructor via `find_class` / `get_method_id` on every
/// call was the single largest component-invoke cost (~1.6 us of ~4.7 us). These
/// handles are stable for the life of the JVM, so we resolve them once.
pub(crate) mod reply_cache {
    use crate::error::WasmtimeError;
    use jni::objects::{GlobalRef, JMethodID};
    use jni::JNIEnv;
    use std::sync::OnceLock;

    pub struct ReplyRefs {
        /// Global ref keeping `java/lang/Object` loaded; used as the array element class.
        pub object_class: GlobalRef,
        /// Global ref keeping `java/lang/Integer` loaded; target of `new_object_unchecked`.
        pub integer_class: GlobalRef,
        /// Cached `Integer(int)` constructor id (valid while `integer_class` stays loaded).
        pub integer_ctor: JMethodID,
    }

    // GlobalRef is Send+Sync; JMethodID is declared Send+Sync by the jni crate.
    static REFS: OnceLock<ReplyRefs> = OnceLock::new();

    pub fn get(env: &mut JNIEnv) -> Result<&'static ReplyRefs, WasmtimeError> {
        if let Some(r) = REFS.get() {
            return Ok(r);
        }
        let integer_class = env
            .find_class("java/lang/Integer")
            .map_err(|e| WasmtimeError::JniError(format!("find Integer: {}", e)))?;
        let integer_ctor = env
            .get_method_id(&integer_class, "<init>", "(I)V")
            .map_err(|e| WasmtimeError::JniError(format!("Integer(int) ctor: {}", e)))?;
        let object_class = env
            .find_class("java/lang/Object")
            .map_err(|e| WasmtimeError::JniError(format!("find Object: {}", e)))?;
        let integer_ref = env
            .new_global_ref(&integer_class)
            .map_err(|e| WasmtimeError::JniError(format!("global Integer: {}", e)))?;
        let object_ref = env
            .new_global_ref(&object_class)
            .map_err(|e| WasmtimeError::JniError(format!("global Object: {}", e)))?;
        // Ignore a benign race: if another thread set it first, keep the winner.
        let _ = REFS.set(ReplyRefs {
            object_class: object_ref,
            integer_class: integer_ref,
            integer_ctor,
        });
        Ok(REFS.get().unwrap())
    }
}

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
    let wasm_data_result = env
        .convert_byte_array(unsafe { JByteArray::from_raw(wasm_bytes) })
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java byte array: {}", e),
        });

    jni_utils::jni_try_ptr(&mut env, || {
        // Use enhanced component engine since nativeCreateComponentEngine creates EnhancedComponentEngine
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
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
        let engine =
            unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };

        // This returns instance ID - instance is stored in engine's HashMap
        // to maintain proper Wasmtime ownership (Engine/Store/Instance must stay together)
        Ok(engine.instantiate_component(component)? as i64)
    })
}

/// Instantiate a component with a host-supplied WASI capability policy (preopens / env /
/// network), registering the instance in the component's engine so it is invocable.
///
/// This is the native backing for capability-confined component instantiation: the host
/// passes the granted preopen dirs (host path, guest path, dir/file permission bits), env
/// vars, stdio inheritance, and a network allow flag. Everything not granted is denied.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeInstantiateComponentWithWasi(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    component_ptr: jlong,
    host_paths: jni::objects::JObjectArray,
    guest_paths: jni::objects::JObjectArray,
    dir_perm_bits: jni::objects::JIntArray,
    file_perm_bits: jni::objects::JIntArray,
    env_keys: jni::objects::JObjectArray,
    env_vals: jni::objects::JObjectArray,
    inherit_stdout: jboolean,
    inherit_stderr: jboolean,
    allow_network: jboolean,
    fuel_limit: jlong,
    max_memory_bytes: jlong,
    epoch_deadline: jlong,
) -> jlong {
    fn read_strings(env: &mut JNIEnv, arr: &jni::objects::JObjectArray) -> Vec<String> {
        let len = env.get_array_length(arr).unwrap_or(0);
        let mut out = Vec::with_capacity(len.max(0) as usize);
        for i in 0..len {
            if let Ok(elem) = env.get_object_array_element(arr, i) {
                let jstr: JString = elem.into();
                let s = match env.get_string(&jstr) {
                    Ok(s) => s.to_string_lossy().into_owned(),
                    Err(_) => continue,
                };
                out.push(s);
            }
        }
        out
    }
    fn read_ints(env: &mut JNIEnv, arr: &jni::objects::JIntArray) -> Vec<i32> {
        let len = env.get_array_length(arr).unwrap_or(0).max(0) as usize;
        let mut buf = vec![0i32; len];
        let _ = env.get_int_array_region(arr, 0, &mut buf);
        buf
    }

    let hosts = read_strings(&mut env, &host_paths);
    let guests = read_strings(&mut env, &guest_paths);
    let dperms = read_ints(&mut env, &dir_perm_bits);
    let fperms = read_ints(&mut env, &file_perm_bits);
    let ekeys = read_strings(&mut env, &env_keys);
    let evals = read_strings(&mut env, &env_vals);

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let engine = unsafe {
            &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine)
        };
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };

        let mut cfg = crate::component::WasiP2Config::default();
        cfg.allow_network = allow_network != 0;
        cfg.inherit_stdout = inherit_stdout != 0;
        cfg.inherit_stderr = inherit_stderr != 0;
        for i in 0..hosts.len() {
            let d = dperms.get(i).copied().unwrap_or(1) as u32;
            let f = fperms.get(i).copied().unwrap_or(1) as u32;
            let guest = guests.get(i).cloned().unwrap_or_else(|| hosts[i].clone());
            cfg.preopened_dirs.push((hosts[i].clone(), guest, d, f));
        }
        for i in 0..ekeys.len() {
            cfg.env
                .insert(ekeys[i].clone(), evals.get(i).cloned().unwrap_or_default());
        }

        let wasi_ctx = cfg.build_wasi_ctx();
        // Negative = no cap (run unlimited); >= 0 = the requested budget.
        let opt = |v: jlong| if v < 0 { None } else { Some(v as u64) };
        Ok(engine.instantiate_component_with_wasi(
            component,
            wasi_ctx,
            opt(fuel_limit),
            opt(max_memory_bytes),
            opt(epoch_deadline),
        )? as jlong)
    })
}

/// Get the number of exported interfaces from a component
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentExportCount(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if component_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component pointer is null".to_string(),
            });
        }
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.metadata.exports.len() as jint)
    })
}

/// Get the number of imports required by a component
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentImportCount(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if component_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component pointer is null".to_string(),
            });
        }
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.metadata.imports.len() as jint)
    })
}

/// Get export interface name by index
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentExportName(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    index: jint,
) -> jstring {
    if component_ptr == 0 {
        return std::ptr::null_mut();
    }

    let name = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<String> {
        let component = unsafe { &*(component_ptr as *const Component) };
        let idx = index as usize;
        if idx >= component.metadata.exports.len() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Export index {} out of bounds (len={})", idx, component.metadata.exports.len()),
            });
        }
        Ok(component.metadata.exports[idx].name.clone())
    })) {
        Ok(Ok(n)) => n,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    match env.new_string(&name) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get import interface name by index
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentImportName(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    index: jint,
) -> jstring {
    if component_ptr == 0 {
        return std::ptr::null_mut();
    }

    let name = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<String> {
        let component = unsafe { &*(component_ptr as *const Component) };
        let idx = index as usize;
        if idx >= component.metadata.imports.len() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Import index {} out of bounds (len={})", idx, component.metadata.imports.len()),
            });
        }
        Ok(component.metadata.imports[idx].name.clone())
    })) {
        Ok(Ok(n)) => n,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    match env.new_string(&name) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get component size in bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentSize(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if component_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component pointer is null".to_string(),
            });
        }
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.size_bytes() as jlong)
    })
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
        return 0;
    }

    let interface_str: String = match env.get_string(&interface_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.exports_interface(&interface_str))
    }) as jboolean
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
        return 0;
    }

    let interface_str: String = match env.get_string(&interface_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.imports_interface(&interface_str))
    }) as jboolean
}

/// Get active component instances count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetActiveInstancesCount(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if engine_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component engine pointer is null".to_string(),
            });
        }
        let engine = unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };
        let instances = engine.get_active_instances()?;
        Ok(instances.len() as jint)
    })
}

/// Cleanup inactive component instances
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCleanupInstances(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if engine_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component engine pointer is null".to_string(),
            });
        }
        let engine = unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };
        let cleaned_count = engine.cleanup_instances()?;
        Ok(cleaned_count as jint)
    })
}

/// Destroy a component engine
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponentEngine(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        unsafe {
            crate::component_core::core::destroy_enhanced_component_engine(
                engine_ptr as *mut std::os::raw::c_void,
            );
        }
        Ok(())
    });
}

/// Destroy a component
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponent(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        unsafe {
            crate::component::core::destroy_component(component_ptr as *mut std::os::raw::c_void);
        }
        Ok(())
    });
}

/// Destroy a component instance by removing it from the engine's HashMap
///
/// This properly releases the instance by removing it from the engine,
/// allowing the Store and Instance to be dropped correctly.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyComponentInstance(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if engine_ptr == 0 || instance_id == 0 {
            return Ok(());
        }
        let engine = unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };
        let _ = engine.remove_instance(instance_id as u64);
        Ok(())
    });
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

        let prof = invoke_profile::enabled();
        let mut _t = std::time::Instant::now();

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
        let engine =
            unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };

        // Lock the instances HashMap and get mutable reference
        let mut instances = engine
            .instances
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances write lock".to_string(),
            })?;

        let handle = instances.get_mut(&(instance_id as u64)).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Instance ID {} not found in engine", instance_id),
            }
        })?;

        // Convert function name
        let func_name_jstring: JString = unsafe { JString::from_raw(function_name) };
        let func_name: String = env
            .get_string(&func_name_jstring)
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
        let discriminators: Vec<i32> =
            unsafe { std::slice::from_raw_parts(discriminators.as_ptr(), discriminators.len()) }
                .to_vec();

        let param_count = discriminators.len();

        // Convert param_data to JObjectArray
        let param_data_obj: JObject = unsafe { JObject::from_raw(param_data) };
        let param_data_typed = jni::objects::JObjectArray::from(param_data_obj);

        if prof {
            _t = invoke_profile::add(&invoke_profile::SETUP, _t);
        }

        // Deserialize parameters to Val
        let mut params = Vec::with_capacity(param_count);
        for i in 0..param_count {
            let data_obj = env
                .get_object_array_element(&param_data_typed, i as i32)
                .map_err(|e| WasmtimeError::InvalidParameter {
                    message: format!("Failed to get parameter {} data: {}", i, e),
                })?;

            let data_array = jni::objects::JByteArray::from(data_obj);
            let data_bytes = env.convert_byte_array(data_array).map_err(|e| {
                WasmtimeError::InvalidParameter {
                    message: format!("Failed to convert parameter {} data: {}", i, e),
                }
            })?;

            let val = crate::wit_value_marshal::deserialize_to_val(discriminators[i], &data_bytes)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to deserialize parameter {}: {}", i, e),
                    backtrace: None,
                })?;

            params.push(val);
        }

        if prof {
            _t = invoke_profile::add(&invoke_profile::ARGDESER, _t);
        }

        // Get the function export and call it
        // Store and Instance are kept together following Wasmtime's ownership model
        // Access through ManuallyDrop - we need ONE mutable ref so Rust can see disjoint field borrows
        use wasmtime::component::Func;

        // Get a single mutable reference so Rust can split borrows on different fields
        let handle_ref = &mut *handle;

        // Get the function using disjoint field borrows. Top-level exports resolve by name;
        // functions nested inside an exported interface are addressed as
        // "<interface>#<func>" (e.g. "fiji:jvm/jvm@0.1.0#create-vm") and require drilling
        // through the interface's export index — wasmtime's name-based get_func only sees
        // top-level exports.
        let func: Func = match handle_ref.instance.get_func(&mut handle_ref.store, &func_name) {
            Some(f) => f,
            None => {
                let (iface, fname) =
                    func_name.rsplit_once('#').ok_or_else(|| WasmtimeError::ImportExport {
                        message: format!("Function '{}' not found in component exports", func_name),
                    })?;
                let (_, iface_idx) = handle_ref
                    .instance
                    .get_export(&mut handle_ref.store, None, iface)
                    .ok_or_else(|| WasmtimeError::ImportExport {
                        message: format!("Exported interface '{}' not found", iface),
                    })?;
                let (_, func_idx) = handle_ref
                    .instance
                    .get_export(&mut handle_ref.store, Some(&iface_idx), fname)
                    .ok_or_else(|| WasmtimeError::ImportExport {
                        message: format!("Function '{}' not found in interface '{}'", fname, iface),
                    })?;
                handle_ref
                    .instance
                    .get_func(&mut handle_ref.store, &func_idx)
                    .ok_or_else(|| WasmtimeError::ImportExport {
                        message: format!("Export '{}' is not a function", func_name),
                    })?
            }
        };

        if prof {
            _t = invoke_profile::add(&invoke_profile::GETFUNC, _t);
        }

        // Call the function - need a fresh mutable reference
        let handle_ref = &mut *handle;
        // Re-apply per-call resource budgets so a fuel_per_call / deadline_ms cap is a fresh budget
        // for THIS call, not a per-instance-lifetime budget that, once spent, traps every later
        // call. Only the high-level (policy) path sets these; other paths leave them as instantiated.
        if let Some(fuel) = handle_ref.per_call_fuel {
            let _ = handle_ref.store.set_fuel(fuel);
        }
        if let Some(deadline) = handle_ref.per_call_epoch_deadline {
            handle_ref.store.set_epoch_deadline(deadline);
        }
        let results_len = func.ty(&handle_ref.store).results().len();
        let mut results = vec![wasmtime::component::Val::Bool(false); results_len];
        if prof {
            _t = invoke_profile::add(&invoke_profile::TY, _t);
        }
        func.call(&mut handle_ref.store, &params, &mut results)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Function call failed: {}", e),
                backtrace: None,
            })?;
        if prof {
            _t = invoke_profile::add(&invoke_profile::CALL, _t);
        }

        // wasmtime 45 performs post-return cleanup automatically; the explicit
        // `Func::post_return` call is no longer required.

        // Handle void return (no results)
        if results.is_empty() {
            // Return null for void functions
            return Ok(jni::objects::JObject::null());
        }

        // Serialize first result
        let (result_discriminator, result_data) =
            crate::wit_value_marshal::serialize_from_val(&results[0]).map_err(|e| {
                WasmtimeError::Runtime {
                    message: format!("Failed to serialize result: {}", e),
                    backtrace: None,
                }
            })?;

        // Create Java Object array with [discriminator, data].
        // Reuse process-global Object/Integer class + ctor handles instead of
        // resolving them via find_class / GetMethodID on every call.
        let refs = reply_cache::get(env)?;
        let object_class =
            unsafe { jni::objects::JClass::from_raw(refs.object_class.as_raw()) };
        let integer_class =
            unsafe { jni::objects::JClass::from_raw(refs.integer_class.as_raw()) };

        let result_array = env
            .new_object_array(2, &object_class, jni::objects::JObject::null())
            .map_err(|e| {
                WasmtimeError::JniError(format!("Failed to create result array: {}", e))
            })?;

        // Set discriminator (as Integer) via the cached constructor id.
        let integer_obj = unsafe {
            env.new_object_unchecked(
                &integer_class,
                refs.integer_ctor,
                &[jni::objects::JValue::Int(result_discriminator).as_jni()],
            )
        }
        .map_err(|e| WasmtimeError::JniError(format!("Failed to create Integer object: {}", e)))?;

        env.set_object_array_element(&result_array, 0, integer_obj)
            .map_err(|e| {
                WasmtimeError::JniError(format!("Failed to set discriminator in array: {}", e))
            })?;

        // Set data (as byte array)
        let data_jarray = env
            .byte_array_from_slice(&result_data)
            .map_err(|e| WasmtimeError::JniError(format!("Failed to create byte array: {}", e)))?;

        env.set_object_array_element(&result_array, 1, jni::objects::JObject::from(data_jarray))
            .map_err(|e| WasmtimeError::JniError(format!("Failed to set data in array: {}", e)))?;

        if prof {
            invoke_profile::add(&invoke_profile::REPLY, _t);
            invoke_profile::tick_and_maybe_report();
        }

        // Convert to static lifetime for jni_try_object return
        Ok(unsafe { jni::objects::JObject::from_raw(result_array.as_raw()) })
    })
}

/// Serialize a component to bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeSerializeComponent(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jbyteArray {
    if component_ptr == 0 {
        return std::ptr::null_mut();
    }

    let bytes = match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<Vec<u8>> {
        let component = unsafe { &*(component_ptr as *const Component) };
        component.serialize()
    })) {
        Ok(Ok(b)) => b,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return std::ptr::null_mut();
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            return std::ptr::null_mut();
        }
    };

    match env.byte_array_from_slice(&bytes) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Deserialize a component from bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDeserializeComponent(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    serialized_data: jbyteArray,
) -> jlong {
    let data_result = env
        .convert_byte_array(unsafe { JByteArray::from_raw(serialized_data) })
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java byte array: {}", e),
        });

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let data = data_result?;
        if data.is_empty() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Serialized data cannot be empty".to_string(),
            });
        }

        // Engine pointer is a ComponentEngine (from nativeCreateComponentEngine which creates
        // EnhancedComponentEngine, but we need the wasmtime Engine from it)
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };

        let component = Component::deserialize(engine.engine(), &data)?;
        Ok(Box::into_raw(Box::new(component)) as jlong)
    })
}

/// Deserialize a component from a file
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDeserializeComponentFile(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    file_path: JString,
) -> jlong {
    let path_result = env
        .get_string(&file_path)
        .map(|s| s.to_string_lossy().into_owned())
        .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
            message: format!("Failed to convert Java string: {}", e),
        });

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let path = path_result?;
        if path.is_empty() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "File path cannot be empty".to_string(),
            });
        }

        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };

        let component = crate::component::core::deserialize_component_file(engine.engine(), &path)?;
        Ok(Box::into_raw(component) as jlong)
    })
}

/// Get component resources required
///
/// Returns a long array with 4 elements:
/// [num_memories, max_initial_memory_size, num_tables, max_initial_table_size]
/// -2 means unavailable (resources_required returned None), -1 means unbounded
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentResourcesRequired(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jlongArray {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jlongArray> {
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };

        let (num_mem, max_mem, num_tab, max_tab) =
            crate::component::core::get_component_resources_required(component);

        let values = [num_mem as i64, max_mem, num_tab as i64, max_tab];

        let arr = env.new_long_array(4).map_err(|e| crate::error::WasmtimeError::Internal {
            message: format!("Failed to create long array: {}", e),
        })?;
        env.set_long_array_region(&arr, 0, &values)
            .map_err(|e| crate::error::WasmtimeError::Internal {
                message: format!("Failed to set long array region: {}", e),
            })?;
        Ok(arr.into_raw())
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Check if a component instance has a specific function export
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentInstanceHasFunc(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    function_name: jstring,
) -> jboolean {
    // Extract function name string before entering closure
    let name = match unsafe { env.get_string_unchecked(&JString::from_raw(function_name)) } {
        Ok(s) => String::from(s),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, move || {
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };
        Ok(
            if engine.has_component_instance_func(instance_id as u64, &name)? {
                1
            } else {
                0
            },
        )
    })
}

/// Look up a core module exported by a component instance
///
/// Returns a pointer to the module (as jlong), or 0 if not found
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentInstanceGetModule(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    module_name: jstring,
) -> jlong {
    // Extract module name string before entering closure
    let name = match unsafe { env.get_string_unchecked(&JString::from_raw(module_name)) } {
        Ok(s) => String::from(s),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, move || {
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };
        match engine.get_component_instance_module(instance_id as u64, &name)? {
            Some(module) => {
                let boxed = Box::new(module);
                Ok(Box::into_raw(boxed) as jlong)
            }
            None => Ok(0),
        }
    })
}

/// Check if a resource type is exported by a component instance
///
/// Returns 1 if found, 0 if not found
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentInstanceHasResource(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    resource_name: jstring,
) -> jboolean {
    // Extract resource name string before entering closure
    let name = match unsafe { env.get_string_unchecked(&JString::from_raw(resource_name)) } {
        Ok(s) => String::from(s),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, move || {
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };
        Ok(
            if engine.get_component_instance_resource(instance_id as u64, &name)? {
                1
            } else {
                0
            },
        )
    })
}

/// Get a component export index for efficient repeated lookups.
///
/// Returns a boxed ComponentExportIndex pointer as jlong, or 0 if not found.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetExportIndex(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    instance_index_ptr: jlong,
    name: jstring,
) -> jlong {
    let name_str = match unsafe { env.get_string_unchecked(&JString::from_raw(name)) } {
        Ok(s) => String::from(s),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, move || {
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };

        let instance = if instance_index_ptr == 0 {
            None
        } else {
            Some(unsafe {
                &*(instance_index_ptr as *const wasmtime::component::ComponentExportIndex)
            })
        };

        match crate::component::core::get_export_index(component, instance, &name_str) {
            Some(boxed_index) => Ok(Box::into_raw(boxed_index) as jlong),
            None => Ok(0),
        }
    })
}

/// Destroy a component export index.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeDestroyExportIndex(
    mut env: JNIEnv,
    _class: JClass,
    index_ptr: jlong,
) {
    jni_utils::jni_try_with_default(&mut env, (), || {
        if index_ptr != 0 {
            unsafe {
                crate::component::core::destroy_export_index(index_ptr as *mut std::os::raw::c_void);
            }
        }
        Ok(())
    });
}

/// Check if a component instance has a function at the given export index.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentInstanceHasFuncByIndex(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    index_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, 0, move || {
        if index_ptr == 0 {
            return Ok(0);
        }

        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };

        let export_index =
            unsafe { &*(index_ptr as *const wasmtime::component::ComponentExportIndex) };

        match engine.has_component_instance_func_by_index(instance_id as u64, export_index) {
            Ok(true) => Ok(1),
            Ok(false) => Ok(0),
            Err(_) => Ok(0),
        }
    })
}

/// Run concurrent component function calls.
///
/// Takes a JSON string containing the batch of calls and returns a JSON string
/// containing the results.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeRunConcurrentCalls(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    json_input: JString,
) -> jstring {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<jstring, crate::error::WasmtimeError> {
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };

        let json_str: String = env
            .get_string(&json_input)
            .map_err(|e| crate::error::WasmtimeError::InvalidParameter {
                message: format!("Failed to get JSON string: {:?}", e),
            })?
            .into();

        let calls = crate::component_core::concurrent_call_json::deserialize_calls(&json_str)?;

        let results = engine.run_concurrent_calls(instance_id as u64, calls)?;

        let json_result = crate::component_core::concurrent_call_json::serialize_results(&results)?;

        env.new_string(&json_result)
            .map(|s| s.into_raw())
            .map_err(|e| crate::error::WasmtimeError::Internal {
                message: format!("Failed to create result string: {:?}", e),
            })
    })) {
        Ok(Ok(result)) => result,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// JNI: Get full component type as JSON string.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetFullComponentTypeJson(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    engine_ptr: jlong,
) -> jstring {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<jstring, crate::error::WasmtimeError> {
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };
        let engine = unsafe {
            crate::component::core::get_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };
        let json = crate::component::core::get_full_component_type_json(component, &engine.engine)?;

        env.new_string(&json)
            .map(|s| s.into_raw())
            .map_err(|e| crate::error::WasmtimeError::Internal {
                message: format!("Failed to create result string: {:?}", e),
            })
    })) {
        Ok(Ok(result)) => result,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// JNI: Get substituted component type as JSON string.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetSubstitutedComponentTypeJson(
    mut env: JNIEnv,
    _class: JClass,
    linker_ptr: jlong,
    component_ptr: jlong,
) -> jstring {
    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> Result<jstring, crate::error::WasmtimeError> {
        let linker = unsafe {
            if linker_ptr == 0 {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: "Linker pointer cannot be null".to_string(),
                });
            }
            &*(linker_ptr as *const crate::component::ComponentLinker)
        };
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };
        let json = crate::component::core::get_substituted_component_type_json(linker, component)?;

        env.new_string(&json)
            .map(|s| s.into_raw())
            .map_err(|e| crate::error::WasmtimeError::Internal {
                message: format!("Failed to create result string: {:?}", e),
            })
    })) {
        Ok(Ok(result)) => result,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Compile a component from WAT text
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeCompileComponentWat(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    wat_text: JString,
) -> jlong {
    let wat_str = match env.get_string(&wat_text) {
        Ok(s) => String::from(s),
        Err(_) => return 0,
    };

    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };

        if wat_str.is_empty() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "WAT text cannot be empty".to_string(),
            });
        }

        let bytes = wat::parse_str(&wat_str).map_err(|e| crate::error::WasmtimeError::Compilation {
            message: format!("Failed to parse WAT: {}", e),
        })?;

        crate::component_core::core::load_component_from_bytes_enhanced(engine, &bytes)
    }) as jlong
}

/// Check if a component has a specific export by name
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentHasExport(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    export_name: JString,
) -> jboolean {
    if component_ptr == 0 {
        return 0;
    }

    let name_str: String = match env.get_string(&export_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.exports_interface(&name_str))
    }) as jboolean
}

/// Check if a component requires a specific import by name
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentHasImport(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    import_name: JString,
) -> jboolean {
    if component_ptr == 0 {
        return 0;
    }

    let name_str: String = match env.get_string(&import_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let component = unsafe { &*(component_ptr as *const Component) };
        Ok(component.imports_interface(&name_str))
    }) as jboolean
}

/// Validate a component against a WIT interface
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentValidate(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    wit_interface: JString,
) -> jboolean {
    if component_ptr == 0 {
        return 0;
    }

    let wit_str: String = match env.get_string(&wit_interface) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_bool(&mut env, || {
        let component = unsafe { &*(component_ptr as *const Component) };
        component.validate_wit_interface(&wit_str)
    }) as jboolean
}

/// Cleanup unused component instances in the engine (alternate entry point)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentEngineCleanupInstances(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jint {
    jni_utils::jni_try_with_default(&mut env, -1, || {
        if engine_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Component engine pointer is null".to_string(),
            });
        }
        let engine = unsafe { &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine) };
        let cleaned_count = engine.cleanup_instances()?;
        Ok(cleaned_count as jint)
    })
}

/// Check if a component engine supports a specific feature
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentEngineSupportsFeature(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    feature_name: JString,
) -> jboolean {
    if engine_ptr == 0 {
        return 0;
    }

    let feature_str: String = match env.get_string(&feature_name) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    // Component Model features supported by this implementation
    let supported_features = [
        "component-model",
        "wit-interfaces",
        "component-linking",
        "resource-management",
        "interface-validation",
    ];

    if supported_features.contains(&feature_str.as_str()) {
        1
    } else {
        0
    }
}

/// Get interface definition for a component export as JSON
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentGetExportInterface(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
    export_name: JString,
) -> jstring {
    if component_ptr == 0 {
        return std::ptr::null_mut();
    }

    let name_str: String = match env.get_string(&export_name) {
        Ok(s) => s.into(),
        Err(_) => return std::ptr::null_mut(),
    };

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<Option<String>> {
        let component = unsafe { &*(component_ptr as *const Component) };
        match component.get_export_interface(&name_str)? {
            Some(interface_def) => {
                let json = interface_def.to_json().map_err(|e| crate::error::WasmtimeError::Internal {
                    message: format!("Failed to serialize interface: {}", e),
                })?;
                Ok(Some(json))
            }
            None => Ok(None),
        }
    })) {
        Ok(Ok(Some(json))) => {
            match env.new_string(&json) {
                Ok(s) => s.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
        Ok(Ok(None)) => std::ptr::null_mut(),
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            std::ptr::null_mut()
        }
    }
}

/// Look up a general export by name on a component instance.
///
/// Returns a long array [kindCode, exportIndexPtr] on success, or null if not found.
/// Kind codes: 0=ComponentFunc, 1=CoreFunc, 2=Module, 3=Component,
/// 4=ComponentInstance, 5=Type, 6=Resource.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentInstanceGetExport(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    parent_index_ptr: jlong,
    name: JString,
) -> jlongArray {
    let name_str = match env.get_string(&name) {
        Ok(s) => String::from(s),
        Err(_) => return std::ptr::null_mut(),
    };

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jlongArray> {
        let engine = unsafe {
            crate::component_core::core::get_enhanced_component_engine_ref(
                engine_ptr as *const std::os::raw::c_void,
            )?
        };

        let parent_index = if parent_index_ptr == 0 {
            None
        } else {
            Some(unsafe {
                &*(parent_index_ptr as *const wasmtime::component::ComponentExportIndex)
            })
        };

        match engine.get_component_instance_export(instance_id as u64, parent_index, &name_str)? {
            Some((kind, boxed_index)) => {
                let index_ptr = Box::into_raw(boxed_index) as i64;
                let values = [kind as i64, index_ptr];

                let arr = env.new_long_array(2).map_err(|e| crate::error::WasmtimeError::Internal {
                    message: format!("Failed to create long array: {}", e),
                })?;
                env.set_long_array_region(&arr, 0, &values)
                    .map_err(|e| crate::error::WasmtimeError::Internal {
                        message: format!("Failed to set long array region: {}", e),
                    })?;
                Ok(arr.into_raw())
            }
            None => Ok(std::ptr::null_mut()),
        }
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            jni_utils::throw_panic_as_exception(&mut env, panic_info);
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for Component.imageRange() - returns long[2] = [start, end]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeGetComponentImageRange(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jlongArray {
    if component_ptr == 0 {
        let _ = jni_utils::throw_jni_exception(
            &mut env,
            &crate::error::WasmtimeError::InvalidParameter {
                message: "Component pointer cannot be null".to_string(),
            },
        );
        return std::ptr::null_mut();
    }

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<jlongArray> {
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };
        let (start, end) = crate::component::core::get_component_image_range(component);
        let values = [start as i64, end as i64];

        let arr = env.new_long_array(2).map_err(|e| crate::error::WasmtimeError::Internal {
            message: format!("Failed to create long array: {}", e),
        })?;
        env.set_long_array_region(&arr, 0, &values)
            .map_err(|e| crate::error::WasmtimeError::Internal {
                message: format!("Failed to set long array region: {}", e),
            })?;
        Ok(arr.into_raw())
    })) {
        Ok(Ok(array)) => array,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// JNI binding for Component.initializeCopyOnWriteImage()
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeInitializeCopyOnWriteImage(
    mut env: JNIEnv,
    _class: JClass,
    component_ptr: jlong,
) -> jboolean {
    if component_ptr == 0 {
        return 0;
    }

    match std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| -> crate::error::WasmtimeResult<()> {
        let component = unsafe {
            crate::component::core::get_component_ref(component_ptr as *const std::os::raw::c_void)?
        };
        crate::component::core::initialize_copy_on_write_image(component)
    })) {
        Ok(Ok(())) => 1,
        Ok(Err(e)) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            0
        }
        Err(panic_info) => {
            let panic_msg = if let Some(s) = panic_info.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = panic_info.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic occurred in native code".to_string()
            };
            let error = crate::error::WasmtimeError::from_string(format!("Native panic: {}", panic_msg));
            jni_utils::throw_jni_exception(&mut env, &error);
            0
        }
    }
}

/// Close an async val handle (Future/Stream/ErrorContext) in the AsyncValRegistry.
///
/// This removes the handle from the global registry, dropping the stored Val.
/// Safe to call with handles that have already been consumed or closed (no-op).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeAsyncValClose(
    _env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    if handle <= 0 {
        return;
    }
    crate::component::async_val_close(handle as u64);
}

/// Drop a ResourceAny held in the global resource registry.
///
/// Takes the resource from the registry and calls resource_drop on it
/// using the store associated with the given component instance.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeResourceAnyDrop(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    instance_id: jlong,
    resource_handle: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        if engine_ptr == 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Engine pointer is null".to_string(),
            });
        }
        let engine = unsafe {
            &*(engine_ptr as *const crate::component_core::EnhancedComponentEngine)
        };
        engine.resource_any_drop(instance_id as u64, resource_handle as u64)
    });
}

/// Parse a WAVE-encoded string into a component value.
///
/// Returns an Object[] with 2 elements: [Integer typeDiscriminator, byte[] data]
#[cfg(feature = "wave")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponent_nativeComponentValFromWave(
    mut env: JNIEnv,
    _class: JClass,
    type_name: jstring,
    wave_str: jstring,
) -> jobjectArray {
    jni_utils::jni_try_object(&mut env, |env| {
        use crate::error::WasmtimeError;

        let type_name_jstr: JString = unsafe { JString::from_raw(type_name) };
        let type_name_str: String = env
            .get_string(&type_name_jstr)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert type name: {}", e),
            })?
            .into();

        let wave_jstr: JString = unsafe { JString::from_raw(wave_str) };
        let wave_string: String = env
            .get_string(&wave_jstr)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to convert WAVE string: {}", e),
            })?
            .into();

        let (type_disc, data) =
            crate::component::component_val_from_wave(&type_name_str, &wave_string)?;

        // Create result array: [Integer, byte[]]
        let object_class = env.find_class("java/lang/Object").map_err(|e| {
            WasmtimeError::Runtime {
                message: format!("Failed to find Object class: {}", e),
                backtrace: None,
            }
        })?;

        let result = env
            .new_object_array(2, &object_class, JObject::null())
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create result array: {}", e),
                backtrace: None,
            })?;

        // Set type discriminator
        let disc_obj = env
            .new_object(
                "java/lang/Integer",
                "(I)V",
                &[jni::objects::JValue::Int(type_disc)],
            )
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create Integer: {}", e),
                backtrace: None,
            })?;

        env.set_object_array_element(&result, 0, &disc_obj)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to set discriminator: {}", e),
                backtrace: None,
            })?;

        // Set data bytes
        let byte_array = env
            .new_byte_array(data.len() as i32)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create byte array: {}", e),
                backtrace: None,
            })?;

        env.set_byte_array_region(&byte_array, 0, unsafe {
            std::slice::from_raw_parts(data.as_ptr() as *const i8, data.len())
        })
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to set byte array data: {}", e),
            backtrace: None,
        })?;

        env.set_object_array_element(&result, 1, &byte_array)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to set data: {}", e),
                backtrace: None,
            })?;

        Ok(result.into_raw())
    })
}
