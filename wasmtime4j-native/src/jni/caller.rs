//! JNI bindings for Caller context operations

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::{jlong, jboolean};

use wasmtime::Caller as WasmtimeCaller;

use crate::caller::core;
use crate::error::{jni_utils, WasmtimeError};
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
            None => Err(WasmtimeError::CallerContextError { message: "Fuel metering not enabled".to_string() }),
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
            None => Err(WasmtimeError::CallerContextError { message: "Fuel metering not enabled".to_string() }),
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
            None => Err(WasmtimeError::ExportNotFound { name: name_str }),
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
            None => Err(WasmtimeError::ExportNotFound { name: name_str }),
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
            None => Err(WasmtimeError::ExportNotFound { name: name_str }),
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
            None => Err(WasmtimeError::ExportNotFound { name: name_str }),
        }
    }) as jlong
}
