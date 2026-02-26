//! JNI bindings for Caller context operations

use jni::objects::{JClass, JString};
use jni::sys::{jboolean, jint, jintArray, jlong, jsize};
use jni::JNIEnv;

use wasmtime::Caller as WasmtimeCaller;

use crate::caller::core;
use crate::error::jni_utils;
use crate::store::StoreData;

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

    jni_utils::jni_try_with_default(&mut env, -1i64, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_fuel_remaining(caller)? {
            Some(fuel) => Ok(fuel as i64),
            None => Ok(-1i64), // Fuel metering not enabled
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
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_add_fuel(caller, fuel as u64)
    });
}

/// Set fuel to a specific value for the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetFuel(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    fuel: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_fuel(caller, fuel as u64)
    });
}

/// Set epoch deadline for the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetEpochDeadline(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    deadline: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_epoch_deadline(caller, deadline as u64)
    });
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

/// Set fuel async yield interval for the caller's store (JNI version)
#[no_mangle]
#[allow(non_snake_case)]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeSetFuelAsyncYieldInterval(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
    interval: jlong,
) {
    if caller_handle == 0 {
        return;
    }

    jni_utils::jni_try_void(&mut env, || {
        let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_fuel_async_yield_interval(caller, interval as u64)
    });
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

/// Get debug exit frames from the caller (JNI version)
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniCaller_nativeDebugExitFrames(
    mut env: JNIEnv,
    _class: JClass,
    caller_handle: jlong,
) -> jintArray {
    let null_array = std::ptr::null_mut();
    if caller_handle == 0 {
        return null_array;
    }

    let caller = unsafe { &mut *(caller_handle as *mut WasmtimeCaller<'_, StoreData>) };

    match core::caller_debug_exit_frames(caller) {
        Ok(Some(frames)) => {
            if frames.is_empty() {
                env.new_int_array(0)
                    .map(|a| a.into_raw())
                    .unwrap_or(null_array)
            } else {
                let flat_len = frames.len() * 4;
                let mut flat_data: Vec<jint> = Vec::with_capacity(flat_len);
                for frame in &frames {
                    flat_data.push(frame[0]);
                    flat_data.push(frame[1]);
                    flat_data.push(frame[2]);
                    flat_data.push(frame[3]);
                }
                match env.new_int_array(flat_len as jsize) {
                    Ok(array) => {
                        if env.set_int_array_region(&array, 0, &flat_data).is_ok() {
                            array.into_raw()
                        } else {
                            null_array
                        }
                    }
                    Err(_) => null_array,
                }
            }
        }
        Ok(None) => null_array,
        Err(_) => null_array,
    }
}
