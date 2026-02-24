//! JNI bindings for Store operations

use jni::objects::{JClass, JObject, JValue};
use jni::sys::{jboolean, jint, jlong, jobjectArray};
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::store::core;

use std::os::raw::c_void;

/// Create a new store with default configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStore(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };
        let store = core::create_store(engine)?;
        let store_ptr = store.as_ref() as *const _ as *const c_void;
        crate::memory::core::register_store_handle(store_ptr)?;
        Ok(store)
    }) as jlong
}

/// Create a new store with custom configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateStoreWithConfig(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    fuel_limit: jlong,             // 0 = no limit
    memory_limit_bytes: jlong,     // 0 = no limit
    execution_timeout_secs: jlong, // 0 = no timeout
    max_instances: jint,           // 0 = no limit
    max_table_elements: jint,      // 0 = no limit
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };

        use crate::ffi_common::parameter_conversion::{zero_to_none_u32, zero_to_none_u64, zero_to_none_usize};

        let store = core::create_store_with_config(
            engine,
            zero_to_none_u64(fuel_limit),
            zero_to_none_usize(memory_limit_bytes),
            zero_to_none_u64(execution_timeout_secs),
            zero_to_none_usize(max_instances as i64),
            zero_to_none_u32(max_table_elements),
            None,  // max_tables
            None,  // max_memories
            false, // trap_on_grow_failure
        )?;
        let store_ptr = store.as_ref() as *const _ as *const c_void;
        crate::memory::core::register_store_handle(store_ptr)?;
        Ok(store)
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
        let module = unsafe { crate::module::core::get_module_ref(module_ptr as *const c_void)? };
        let store = core::create_store_for_module(module)?;
        let store_ptr = store.as_ref() as *const _ as *const c_void;
        crate::memory::core::register_store_handle(store_ptr)?;
        Ok(store)
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::add_fuel(store, fuel as u64)?;
        Ok(true)
    }) as jboolean
}

/// Set fuel to a specific amount (replaces current fuel)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetFuel(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    fuel: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::set_fuel(store, fuel as u64)?;
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        let actual_consumed = core::consume_fuel(store, fuel_to_consume as u64)?;
        Ok(actual_consumed as jlong)
    })
}

/// Set fuel async yield interval
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetFuelAsyncYieldInterval(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    interval: jlong,
) {
    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::set_fuel_async_yield_interval(store, interval as u64)?;
        Ok(())
    });
}

/// Set epoch deadline for interruption
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetEpochDeadline(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    ticks: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::set_epoch_deadline(store, ticks as u64);
        Ok(true)
    }) as jboolean
}

/// Configure store to trap on epoch deadline
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeEpochDeadlineTrap(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::epoch_deadline_trap(store)?;
        Ok(())
    });
}

// Static storage for JNI epoch callbacks
static JNI_EPOCH_CALLBACKS: std::sync::OnceLock<
    std::sync::Mutex<std::collections::HashMap<i64, JniEpochCallbackContext>>,
> = std::sync::OnceLock::new();

struct JniEpochCallbackContext {
    jvm: std::sync::Arc<jni::JavaVM>,
    jni_store_global: jni::objects::GlobalRef,
}

fn get_jni_epoch_callbacks(
) -> &'static std::sync::Mutex<std::collections::HashMap<i64, JniEpochCallbackContext>> {
    JNI_EPOCH_CALLBACKS.get_or_init(|| std::sync::Mutex::new(std::collections::HashMap::new()))
}

fn register_jni_epoch_callback(
    callback_id: i64,
    jvm: std::sync::Arc<jni::JavaVM>,
    jni_store_global: jni::objects::GlobalRef,
) {
    let mut callbacks = get_jni_epoch_callbacks()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    callbacks.insert(
        callback_id,
        JniEpochCallbackContext {
            jvm,
            jni_store_global,
        },
    );
}

pub(crate) fn unregister_jni_epoch_callback(callback_id: i64) {
    let mut callbacks = get_jni_epoch_callbacks()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    callbacks.remove(&callback_id);
}

/// Dispatch function for JNI epoch callbacks
fn jni_epoch_callback_dispatch(callback_id: i64, _epoch: u64) -> i64 {
    // Clone the JVM Arc and global ref outside the lock to avoid holding the lock during JNI calls
    let (jvm, global_ref_ptr) = {
        let callbacks = match get_jni_epoch_callbacks().lock() {
            Ok(cb) => cb,
            Err(e) => {
                log::error!("Failed to lock epoch callbacks: {}", e);
                return -1; // Trap
            }
        };

        match callbacks.get(&callback_id) {
            Some(ctx) => {
                // Clone the Arc<JavaVM> and store the raw pointer to the global ref
                // We'll create a new GlobalRef from the same object in the attached thread
                (
                    ctx.jvm.clone(),
                    ctx.jni_store_global.as_obj().as_raw() as usize,
                )
            }
            None => {
                log::warn!("No JNI epoch callback found for ID: {}", callback_id);
                return -1; // Trap
            }
        }
    };

    // Attach to JVM and call the Java callback
    let result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            // Reconstruct the JObject from the raw pointer
            // Safety: The global ref is kept alive by the JniEpochCallbackContext
            let jni_store_obj =
                unsafe { jni::objects::JObject::from_raw(global_ref_ptr as jni::sys::jobject) };

            // Call onEpochDeadlineReached on the JniStore object
            match env.call_method(
                &jni_store_obj,
                "onEpochDeadlineReached",
                "(J)J",
                &[jni::objects::JValue::Long(callback_id)],
            ) {
                Ok(result) => {
                    match result.j() {
                        Ok(delta) => delta,
                        Err(e) => {
                            log::error!("Failed to get epoch callback result: {}", e);
                            -1 // Trap
                        }
                    }
                }
                Err(e) => {
                    // Check for Java exception
                    if env.exception_check().unwrap_or(false) {
                        let _ = env.exception_clear();
                    }
                    log::error!("Failed to call onEpochDeadlineReached: {}", e);
                    -1 // Trap
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach to JVM for epoch callback: {}", e);
            -1 // Trap
        }
    };
    result
}

/// Configure epoch deadline callback with actual Java callback support
///
/// This sets up a real callback that will invoke the Java `onEpochDeadlineReached` method
/// on the JniStore object when the epoch deadline is reached.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetEpochDeadlineCallback(
    mut env: JNIEnv,
    jni_store_obj: jni::objects::JObject, // 'this' object for instance methods
    store_ptr: jlong,
) {
    // Get JavaVM reference for later callback use
    let jvm = match env.get_java_vm() {
        Ok(vm) => std::sync::Arc::new(vm),
        Err(e) => {
            log::error!("Failed to get JavaVM: {}", e);
            return;
        }
    };

    // Create a global reference to the JniStore object to prevent GC
    let jni_store_global = match env.new_global_ref(&jni_store_obj) {
        Ok(global) => global,
        Err(e) => {
            log::error!("Failed to create global reference to JniStore: {}", e);
            return;
        }
    };

    // Register the JNI context for this callback
    // Use the store pointer as the callback ID for lookup
    let callback_id = store_ptr;
    register_jni_epoch_callback(callback_id, jvm, jni_store_global);

    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };

        // Create epoch callback function that will dispatch to JNI
        extern "C" fn jni_callback(callback_id: i64, epoch: u64) -> i64 {
            jni_epoch_callback_dispatch(callback_id, epoch)
        }

        // Set the epoch callback with function pointer
        core::epoch_deadline_callback_with_fn(store, jni_callback, callback_id)?;
        Ok(())
    });
}

/// Clear epoch deadline callback and clean up JNI resources
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeClearEpochDeadlineCallback(
    mut env: JNIEnv,
    _jni_store_obj: jni::objects::JObject, // 'this' object for instance methods
    store_ptr: jlong,
) {
    // Unregister the JNI callback context
    unregister_jni_epoch_callback(store_ptr);

    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        // Reset to trap behavior (clear callback)
        core::epoch_deadline_trap(store)?;
        Ok(())
    });
}

/// Configure epoch deadline async yield and update
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeEpochDeadlineAsyncYieldAndUpdate(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    delta_ticks: jlong,
) {
    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::epoch_deadline_async_yield_and_update(store, delta_ticks as u64)?;
        Ok(())
    });
}

/// Validate store functionality
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeValidate(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_bool(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        let stats = core::get_execution_stats(store)?;
        Ok(stats.total_execution_time.as_micros() as jlong)
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        let stats = core::get_execution_stats(store)?;
        Ok(stats.fuel_consumed as jlong)
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        let usage = core::get_memory_usage(store)?;
        Ok(usage.execution_count as jlong)
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
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
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        let metadata = core::get_store_metadata(store);
        Ok(metadata.execution_timeout.map(|d| d.as_secs()).unwrap_or(0) as jlong)
    })
}

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

    let i32_val = env
        .get_object_array_element(&array_obj, 0)
        .ok()
        .and_then(|obj| {
            if obj.is_null() {
                None
            } else {
                env.call_method(&obj, "intValue", "()I", &[])
                    .ok()
                    .and_then(|v| v.i().ok())
            }
        })
        .unwrap_or(0);

    let i64_val = env
        .get_object_array_element(&array_obj, 1)
        .ok()
        .and_then(|obj| {
            if obj.is_null() {
                None
            } else {
                env.call_method(&obj, "longValue", "()J", &[])
                    .ok()
                    .and_then(|v| v.j().ok())
            }
        })
        .unwrap_or(0);

    let f32_val = env
        .get_object_array_element(&array_obj, 2)
        .ok()
        .and_then(|obj| {
            if obj.is_null() {
                None
            } else {
                env.call_method(&obj, "floatValue", "()F", &[])
                    .ok()
                    .and_then(|v| v.f().ok())
            }
        })
        .unwrap_or(0.0);

    let f64_val = env
        .get_object_array_element(&array_obj, 3)
        .ok()
        .and_then(|obj| {
            if obj.is_null() {
                None
            } else {
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
                    env.get_byte_array_region(&byte_array, 0, &mut i8_bytes)
                        .ok();
                    let bytes: [u8; 16] = i8_bytes.map(|b| b as u8);
                    Some(bytes)
                } else {
                    None
                };
                (v128, None)
            } else {
                // It's a Long for funcref/externref
                let ref_val = env
                    .call_method(&obj, "longValue", "()J", &[])
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
        let store = unsafe { crate::store::core::get_store_ref(store_handle as *const c_void)? };

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
        use crate::error::WasmtimeError;
        use wasmtime::{RefType, ValType};

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
            is_64: false,
            memory_index: 0,
            name: None,
        };

        // Create the memory
        let memory = crate::memory::Memory::new_with_config(store, memory_config)?;

        // Register the memory handle for validation and return the pointer
        let validated_ptr = crate::memory::core::create_validated_memory(memory)?;
        Ok(validated_ptr as jlong)
    })
}

/// Create a new WebAssembly linear memory with full type parameters
///
/// Supports Memory64 (is_64=1) and shared memory (is_shared=1) in a single call.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCreateMemoryWithType(
    mut env: JNIEnv,
    _class: JClass,
    store_handle: jlong,
    initial_pages: jlong,
    max_pages: jlong,
    is_shared: jint,
    is_64: jint,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Extract store from handle
        let store = unsafe { core::get_store_mut(store_handle as *mut c_void)? };

        // Convert max_pages (-1 means unlimited)
        let max_pages_opt = if max_pages == -1 {
            None
        } else {
            Some(max_pages as u64)
        };

        // Create memory using MemoryConfig with all type parameters
        let memory_config = crate::memory::MemoryConfig {
            initial_pages: initial_pages as u64,
            maximum_pages: max_pages_opt,
            is_shared: is_shared != 0,
            is_64: is_64 != 0,
            memory_index: 0,
            name: None,
        };

        // Create the memory
        let memory = crate::memory::Memory::new_with_config(store, memory_config)?;

        // Register the memory handle for validation and return the pointer
        let validated_ptr = crate::memory::core::create_validated_memory(memory)?;
        Ok(validated_ptr as jlong)
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
        // Extract store from handle
        let store = unsafe { core::get_store_mut(store_handle as *mut c_void)? };

        // Create memory configuration with shared flag enabled
        let memory_config = crate::memory::MemoryConfig {
            initial_pages: initial_pages as u64,
            maximum_pages: Some(max_pages as u64), // Shared memory requires max pages
            is_shared: true,
            is_64: false,
            memory_index: 0,
            name: None,
        };

        // Create the shared memory
        let memory = crate::memory::Memory::new_with_config(store, memory_config)?;

        // Register the memory handle for validation and return the pointer
        let validated_ptr = crate::memory::core::create_validated_memory(memory)?;
        Ok(validated_ptr as jlong)
    })
}

/// Create a store with resource limits (for JniWasmRuntime)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateStoreWithLimits(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    memory_size: jlong,       // 0 = no limit
    table_elements: jlong,    // 0 = no limit
    instances: jlong,         // 0 = no limit
    tables: jlong,            // 0 = no limit
    memories: jlong,          // 0 = no limit
    trap_on_grow_failure: jboolean, // 0 = false, non-zero = true
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };

        use crate::ffi_common::parameter_conversion::{zero_to_none_u32, zero_to_none_usize};

        let store = core::create_store_with_config(
            engine,
            None, // fuel_limit
            zero_to_none_usize(memory_size),
            None, // execution_timeout
            zero_to_none_usize(instances),
            zero_to_none_u32(table_elements as i32),
            zero_to_none_usize(tables),
            zero_to_none_usize(memories),
            trap_on_grow_failure != 0,
        )?;
        let store_ptr = store.as_ref() as *const _ as *const c_void;
        crate::memory::core::register_store_handle(store_ptr)?;
        Ok(store)
    }) as jlong
}

/// Create a store with comprehensive resource limits including fuel and timeout
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateStoreWithResourceLimits(
    mut env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    fuel_limit: jlong,             // 0 = no limit
    memory_size: jlong,            // 0 = no limit
    execution_timeout_secs: jlong, // 0 = no timeout
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };

        use crate::ffi_common::parameter_conversion::{zero_to_none_u64, zero_to_none_usize};

        let store = core::create_store_with_config(
            engine,
            zero_to_none_u64(fuel_limit),
            zero_to_none_usize(memory_size),
            zero_to_none_u64(execution_timeout_secs),
            None,  // instances
            None,  // table_elements
            None,  // max_tables
            None,  // max_memories
            false, // trap_on_grow_failure
        )?;
        let store_ptr = store.as_ref() as *const _ as *const c_void;
        crate::memory::core::register_store_handle(store_ptr)?;
        Ok(store)
    }) as jlong
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeDestroyStore(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    if store_ptr != 0 {
        // Unregister the store handle from memory module before destroying
        let _ = crate::memory::core::unregister_store_handle(store_ptr as *const c_void);
    }
    unsafe {
        core::destroy_store(store_ptr as *mut c_void);
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeCaptureBacktrace<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    store_ptr: jlong,
) -> JObject<'local> {
    let result = (|| -> Result<JObject<'local>, crate::error::WasmtimeError> {
        let store_ref = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };
        let store = store_ref.inner.lock();
        let backtrace = wasmtime::WasmBacktrace::capture(&*store);
        create_backtrace_object(&mut env, &backtrace, false)
    })();

    match result {
        Ok(obj) => obj,
        Err(_) => JObject::null(),
    }
}

#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeForceCaptureBacktrace<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    store_ptr: jlong,
) -> JObject<'local> {
    let result = (|| -> Result<JObject<'local>, crate::error::WasmtimeError> {
        let store_ref = unsafe { crate::store::core::get_store_mut(store_ptr as *mut c_void)? };
        let store = store_ref.inner.lock();
        let backtrace = wasmtime::WasmBacktrace::force_capture(&*store);
        create_backtrace_object(&mut env, &backtrace, true)
    })();

    match result {
        Ok(obj) => obj,
        Err(_) => JObject::null(),
    }
}

/// JNI binding for Store.gc() - triggers garbage collection
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeGc(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::garbage_collect(store)
    });
}

fn create_backtrace_object<'local>(
    env: &mut JNIEnv<'local>,
    backtrace: &wasmtime::WasmBacktrace,
    force_capture: bool,
) -> Result<JObject<'local>, crate::error::WasmtimeError> {
    // Create ArrayList for frames
    let frames_list = env.new_object("java/util/ArrayList", "()V", &[])?;

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
        &[
            JValue::Object(&frames_list),
            JValue::Bool(force_capture as u8),
        ],
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
    let func_name_string = frame
        .func_name()
        .map(|name| env.new_string(name))
        .transpose()?;
    let null_func_name = JObject::null();
    let func_name = func_name_string
        .as_ref()
        .map(|s| JValue::Object(s.as_ref()))
        .unwrap_or(JValue::Object(&null_func_name));

    // Get offsets - create bindings to extend lifetime
    let module_offset_obj = frame
        .module_offset()
        .map(|o| env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(o as i32)]))
        .transpose()?;
    let null_module_offset = JObject::null();
    let module_offset = module_offset_obj
        .as_ref()
        .map(|o| JValue::Object(o))
        .unwrap_or(JValue::Object(&null_module_offset));

    let func_offset_obj = frame
        .func_offset()
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
    let name_string = symbol.name().map(|n| env.new_string(n)).transpose()?;
    let null_name = JObject::null();
    let name = name_string
        .as_ref()
        .map(|s| JValue::Object(s.as_ref()))
        .unwrap_or(JValue::Object(&null_name));

    let file_string = symbol.file().map(|f| env.new_string(f)).transpose()?;
    let null_file = JObject::null();
    let file = file_string
        .as_ref()
        .map(|s| JValue::Object(s.as_ref()))
        .unwrap_or(JValue::Object(&null_file));

    let line_obj = symbol
        .line()
        .map(|l| env.new_object("java/lang/Integer", "(I)V", &[JValue::Int(l as i32)]))
        .transpose()?;
    let null_line = JObject::null();
    let line = line_obj
        .as_ref()
        .map(|o| JValue::Object(o))
        .unwrap_or(JValue::Object(&null_line));

    let column_obj = symbol
        .column()
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

// ============================================================================
// Resource Limiter JNI Callbacks
// ============================================================================

// Static storage for JNI resource limiter callbacks
static JNI_RESOURCE_LIMITER_CALLBACKS: std::sync::OnceLock<
    std::sync::Mutex<std::collections::HashMap<i64, JniResourceLimiterContext>>,
> = std::sync::OnceLock::new();

struct JniResourceLimiterContext {
    jvm: std::sync::Arc<jni::JavaVM>,
    jni_store_global: jni::objects::GlobalRef,
}

fn get_jni_resource_limiter_callbacks(
) -> &'static std::sync::Mutex<std::collections::HashMap<i64, JniResourceLimiterContext>> {
    JNI_RESOURCE_LIMITER_CALLBACKS
        .get_or_init(|| std::sync::Mutex::new(std::collections::HashMap::new()))
}

fn register_jni_resource_limiter(
    callback_id: i64,
    jvm: std::sync::Arc<jni::JavaVM>,
    jni_store_global: jni::objects::GlobalRef,
) {
    let mut callbacks = get_jni_resource_limiter_callbacks()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    callbacks.insert(
        callback_id,
        JniResourceLimiterContext {
            jvm,
            jni_store_global,
        },
    );
}

pub(crate) fn unregister_jni_resource_limiter(callback_id: i64) {
    let mut callbacks = get_jni_resource_limiter_callbacks()
        .lock()
        .unwrap_or_else(|poisoned| poisoned.into_inner());
    callbacks.remove(&callback_id);
}

/// Helper to get JVM and GlobalRef raw pointer from the registry
fn get_limiter_context(callback_id: i64) -> Option<(std::sync::Arc<jni::JavaVM>, usize)> {
    let callbacks = get_jni_resource_limiter_callbacks().lock().ok()?;
    callbacks.get(&callback_id).map(|ctx| {
        (
            ctx.jvm.clone(),
            ctx.jni_store_global.as_obj().as_raw() as usize,
        )
    })
}

// Module-scope trampolines that delegate to JNI dispatch functions.
// Shared by both sync and async resource limiter JNI bindings.
extern "C" fn jni_memory_growing_trampoline(callback_id: i64, current: u64, desired: u64, maximum: u64) -> i32 {
    jni_memory_growing_dispatch(callback_id, current, desired, maximum)
}

extern "C" fn jni_table_growing_trampoline(callback_id: i64, current: u32, desired: u32, maximum: u32) -> i32 {
    jni_table_growing_dispatch(callback_id, current, desired, maximum)
}

extern "C" fn jni_memory_grow_failed_trampoline(callback_id: i64, error: *const std::os::raw::c_char) {
    jni_memory_grow_failed_dispatch(callback_id, error)
}

extern "C" fn jni_table_grow_failed_trampoline(callback_id: i64, error: *const std::os::raw::c_char) {
    jni_table_grow_failed_dispatch(callback_id, error)
}

/// Dispatch function for JNI memory growing callbacks
fn jni_memory_growing_dispatch(callback_id: i64, current: u64, desired: u64, maximum: u64) -> i32 {
    let (jvm, global_ref_ptr) = match get_limiter_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::warn!("No JNI resource limiter found for ID: {}", callback_id);
            return 0; // Deny
        }
    };

    let result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let jni_store_obj =
                unsafe { jni::objects::JObject::from_raw(global_ref_ptr as jni::sys::jobject) };

            match env.call_method(
                &jni_store_obj,
                "onMemoryGrowing",
                "(JJJ)Z",
                &[
                    JValue::Long(current as i64),
                    JValue::Long(desired as i64),
                    JValue::Long(maximum as i64),
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
                        log::error!("Failed to get memoryGrowing result: {}", e);
                        0 // Deny
                    }
                },
                Err(e) => {
                    if env.exception_check().unwrap_or(false) {
                        let _ = env.exception_clear();
                    }
                    log::error!("Failed to call onMemoryGrowing: {}", e);
                    0 // Deny
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach to JVM for memoryGrowing: {}", e);
            0 // Deny
        }
    };
    result
}

/// Dispatch function for JNI table growing callbacks
fn jni_table_growing_dispatch(callback_id: i64, current: u32, desired: u32, maximum: u32) -> i32 {
    let (jvm, global_ref_ptr) = match get_limiter_context(callback_id) {
        Some(ctx) => ctx,
        None => {
            log::warn!("No JNI resource limiter found for ID: {}", callback_id);
            return 0; // Deny
        }
    };

    let result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let jni_store_obj =
                unsafe { jni::objects::JObject::from_raw(global_ref_ptr as jni::sys::jobject) };

            match env.call_method(
                &jni_store_obj,
                "onTableGrowing",
                "(III)Z",
                &[
                    JValue::Int(current as i32),
                    JValue::Int(desired as i32),
                    JValue::Int(maximum as i32),
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
                        log::error!("Failed to get tableGrowing result: {}", e);
                        0 // Deny
                    }
                },
                Err(e) => {
                    if env.exception_check().unwrap_or(false) {
                        let _ = env.exception_clear();
                    }
                    log::error!("Failed to call onTableGrowing: {}", e);
                    0 // Deny
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach to JVM for tableGrowing: {}", e);
            0 // Deny
        }
    };
    result
}

/// Dispatch function for JNI memory grow failed callbacks
fn jni_memory_grow_failed_dispatch(callback_id: i64, error: *const std::os::raw::c_char) {
    let (jvm, global_ref_ptr) = match get_limiter_context(callback_id) {
        Some(ctx) => ctx,
        None => return,
    };

    let error_str = if !error.is_null() {
        unsafe { std::ffi::CStr::from_ptr(error) }
            .to_string_lossy()
            .to_string()
    } else {
        "unknown error".to_string()
    };

    let _result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let jni_store_obj =
                unsafe { jni::objects::JObject::from_raw(global_ref_ptr as jni::sys::jobject) };

            if let Ok(jni_error) = env.new_string(&error_str) {
                let _ = env.call_method(
                    &jni_store_obj,
                    "onMemoryGrowFailed",
                    "(Ljava/lang/String;)V",
                    &[JValue::Object(&jni_error)],
                );
                if env.exception_check().unwrap_or(false) {
                    let _ = env.exception_clear();
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach to JVM for memoryGrowFailed: {}", e);
        }
    };
}

/// Dispatch function for JNI table grow failed callbacks
fn jni_table_grow_failed_dispatch(callback_id: i64, error: *const std::os::raw::c_char) {
    let (jvm, global_ref_ptr) = match get_limiter_context(callback_id) {
        Some(ctx) => ctx,
        None => return,
    };

    let error_str = if !error.is_null() {
        unsafe { std::ffi::CStr::from_ptr(error) }
            .to_string_lossy()
            .to_string()
    } else {
        "unknown error".to_string()
    };

    let _result = match jvm.attach_current_thread() {
        Ok(mut env) => {
            let jni_store_obj =
                unsafe { jni::objects::JObject::from_raw(global_ref_ptr as jni::sys::jobject) };

            if let Ok(jni_error) = env.new_string(&error_str) {
                let _ = env.call_method(
                    &jni_store_obj,
                    "onTableGrowFailed",
                    "(Ljava/lang/String;)V",
                    &[JValue::Object(&jni_error)],
                );
                if env.exception_check().unwrap_or(false) {
                    let _ = env.exception_clear();
                }
            }
        }
        Err(e) => {
            log::error!("Failed to attach to JVM for tableGrowFailed: {}", e);
        }
    };
}

/// Set resource limiter with JNI callback support
///
/// This sets up callbacks that invoke Java methods on the JniStore object
/// when memory or table growth is requested.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetResourceLimiter(
    mut env: JNIEnv,
    jni_store_obj: JObject,
    store_ptr: jlong,
) {
    // Get JavaVM reference
    let jvm = match env.get_java_vm() {
        Ok(vm) => std::sync::Arc::new(vm),
        Err(e) => {
            log::error!("Failed to get JavaVM for resource limiter: {}", e);
            return;
        }
    };

    // Create global reference to prevent GC
    let jni_store_global = match env.new_global_ref(&jni_store_obj) {
        Ok(global) => global,
        Err(e) => {
            log::error!("Failed to create global reference for resource limiter: {}", e);
            return;
        }
    };

    let callback_id = store_ptr;
    register_jni_resource_limiter(callback_id, jvm, jni_store_global);

    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };

        core::set_resource_limiter(
            store,
            callback_id,
            jni_memory_growing_trampoline,
            jni_table_growing_trampoline,
            Some(jni_memory_grow_failed_trampoline),
            Some(jni_table_grow_failed_trampoline),
        )?;
        Ok(())
    });
}

/// Clear resource limiter and clean up JNI resources
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeClearResourceLimiter(
    _env: JNIEnv,
    _jni_store_obj: JObject,
    store_ptr: jlong,
) {
    unregister_jni_resource_limiter(store_ptr);
}

/// Set async resource limiter with JNI callback support
///
/// Same as nativeSetResourceLimiter but uses the async limiter path.
/// Requires the engine to be configured with async_support(true).
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetResourceLimiterAsync(
    mut env: JNIEnv,
    jni_store_obj: JObject,
    store_ptr: jlong,
) {
    // Get JavaVM reference
    let jvm = match env.get_java_vm() {
        Ok(vm) => std::sync::Arc::new(vm),
        Err(e) => {
            log::error!("Failed to get JavaVM for async resource limiter: {}", e);
            return;
        }
    };

    // Create global reference to prevent GC
    let jni_store_global = match env.new_global_ref(&jni_store_obj) {
        Ok(global) => global,
        Err(e) => {
            log::error!(
                "Failed to create global reference for async resource limiter: {}",
                e
            );
            return;
        }
    };

    let callback_id = store_ptr;
    register_jni_resource_limiter(callback_id, jvm, jni_store_global);

    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };

        core::set_resource_limiter_async(
            store,
            callback_id,
            jni_memory_growing_trampoline,
            jni_table_growing_trampoline,
            Some(jni_memory_grow_failed_trampoline),
            Some(jni_table_grow_failed_trampoline),
        )?;
        Ok(())
    });
}

// ============================================================================
// Call Hook JNI Functions
// ============================================================================

/// Set a call hook on the store
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetCallHook(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::set_call_hook(store)?;
        Ok(())
    });
}

/// Clear the call hook from the store
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeClearCallHook(
    mut env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    let _ = jni_utils::jni_try_void(&mut env, || {
        let store = unsafe { core::get_store_ref(store_ptr as *const c_void)? };
        core::clear_call_hook(store)?;
        Ok(())
    });
}

/// Set an async call hook on the store
/// Delegates to the sync version since Java handles async dispatch.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetCallHookAsync(
    env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetCallHook(env, _class, store_ptr);
}

/// Clear the async call hook from the store
/// Delegates to the sync version since Java handles async dispatch.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeClearCallHookAsync(
    env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) {
    Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeClearCallHook(env, _class, store_ptr);
}

// ===== Debugging API =====

/// Check if single-step mode is active
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeIsSingleStep(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) -> jboolean {
    match unsafe { core::get_store_ref(store_ptr as *const c_void) } {
        Ok(store) => {
            if store.is_single_step() {
                1
            } else {
                0
            }
        }
        Err(_) => 0,
    }
}

/// Check if store has async support enabled
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeIsAsync(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) -> jboolean {
    match unsafe { core::get_store_ref(store_ptr as *const c_void) } {
        Ok(store) => {
            if store.is_async() {
                1
            } else {
                0
            }
        }
        Err(_) => 0,
    }
}

/// Get the number of active breakpoints
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeBreakpointCount(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
) -> jint {
    match unsafe { core::get_store_ref(store_ptr as *const c_void) } {
        Ok(store) => match store.breakpoint_count() {
            Ok(Some(count)) => count as jint,
            Ok(None) => -1,
            Err(_) => -2,
        },
        Err(_) => -2,
    }
}

/// Add a breakpoint
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeAddBreakpoint(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    module_ptr: jlong,
    pc: jint,
) -> jint {
    let module = match unsafe { crate::module::core::get_module_ref(module_ptr as *const c_void) } {
        Ok(m) => m,
        Err(_) => return -1,
    };
    match unsafe { core::get_store_ref(store_ptr as *const c_void) } {
        Ok(store) => {
            let wasm_module = module.inner().clone();
            match store.edit_breakpoints(|edit| {
                let _ = edit.add_breakpoint(&wasm_module, pc as u32);
            }) {
                Ok(true) => 0,
                Ok(false) => 1,
                Err(_) => -1,
            }
        }
        Err(_) => -1,
    }
}

/// Remove a breakpoint
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeRemoveBreakpoint(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    module_ptr: jlong,
    pc: jint,
) -> jint {
    let module = match unsafe { crate::module::core::get_module_ref(module_ptr as *const c_void) } {
        Ok(m) => m,
        Err(_) => return -1,
    };
    match unsafe { core::get_store_ref(store_ptr as *const c_void) } {
        Ok(store) => {
            let wasm_module = module.inner().clone();
            match store.edit_breakpoints(|edit| {
                let _ = edit.remove_breakpoint(&wasm_module, pc as u32);
            }) {
                Ok(true) => 0,
                Ok(false) => 1,
                Err(_) => -1,
            }
        }
        Err(_) => -1,
    }
}

/// Set single-step mode
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniStore_nativeSetSingleStep(
    _env: JNIEnv,
    _class: JClass,
    store_ptr: jlong,
    enabled: jboolean,
) -> jint {
    match unsafe { core::get_store_ref(store_ptr as *const c_void) } {
        Ok(store) => {
            let enable = enabled != 0;
            match store.edit_breakpoints(|edit| {
                let _ = edit.single_step(enable);
            }) {
                Ok(true) => 0,
                Ok(false) => 1,
                Err(_) => -1,
            }
        }
        Err(_) => -1,
    }
}
