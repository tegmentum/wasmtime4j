//! JNI bindings for coredump introspection.
//!
//! These functions provide JNI-compatible access to the coredump registry.

use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;

use crate::coredump;

/// Free a coredump entry from the registry.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpFree(
    _env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
) -> jint {
    if coredump::remove(coredump_id as u64) {
        0
    } else {
        -1
    }
}

/// Get the frame count for a coredump.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetFrameCount(
    _env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
) -> jint {
    coredump::with_coredump(coredump_id as u64, |cd| cd.frames().len() as jint).unwrap_or(-1)
}

/// Get the trap message for a coredump.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetTrapMessage(
    mut env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
) -> jstring {
    match coredump::get_trap_message(coredump_id as u64) {
        Some(msg) => match env.new_string(&msg) {
            Ok(s) => s.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get the name of a coredump.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetName(
    mut env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
) -> jstring {
    let name = coredump::with_coredump(coredump_id as u64, |cd| {
        let modules = cd.modules();
        if modules.is_empty() {
            return None;
        }
        modules[0].name().map(|s| s.to_string())
    });

    match name.flatten() {
        Some(n) => match env.new_string(&n) {
            Ok(s) => s.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get all frames as a JSON array string.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetAllFrames(
    mut env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
) -> jstring {
    let json = coredump::with_coredump(coredump_id as u64, |cd| {
        let frames = cd.frames();
        let frame_jsons: Vec<String> = frames
            .iter()
            .map(|f| crate::panama::coredump::frame_to_json(f))
            .collect();
        format!("[{}]", frame_jsons.join(","))
    });

    match json {
        Some(s) => match env.new_string(&s) {
            Ok(js) => js.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get frame info for a specific frame as a JSON string.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetFrameInfo(
    mut env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
    frame_index: jint,
) -> jstring {
    let json = coredump::with_coredump(coredump_id as u64, |cd| {
        let frames = cd.frames();
        if frame_index < 0 || (frame_index as usize) >= frames.len() {
            return None;
        }
        Some(crate::panama::coredump::frame_to_json(
            &frames[frame_index as usize],
        ))
    });

    match json.flatten() {
        Some(s) => match env.new_string(&s) {
            Ok(js) => js.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Serialize a coredump to the standard binary format.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpSerialize(
    mut env: JNIEnv,
    _class: JClass,
    coredump_id: jlong,
    store_handle: jlong,
    name: JString,
) -> jni::sys::jbyteArray {
    let name_str = match env.get_string(&name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return std::ptr::null_mut(),
    };

    let store = unsafe { &mut *(store_handle as *mut wasmtime::Store<crate::store::StoreData>) };

    let result = coredump::with_coredump(coredump_id as u64, |cd| cd.serialize(store, &name_str));

    match result {
        Some(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(arr) => arr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        None => std::ptr::null_mut(),
    }
}

/// Get the total count of coredumps in the registry.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetCount(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    coredump::count() as jint
}

/// Get all coredump IDs as a JSON array string.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpGetAllIds(
    mut env: JNIEnv,
    _class: JClass,
) -> jstring {
    let ids = coredump::all_ids();
    let json = format!(
        "[{}]",
        ids.iter()
            .map(|id| id.to_string())
            .collect::<Vec<_>>()
            .join(",")
    );
    match env.new_string(&json) {
        Ok(s) => s.into_raw(),
        Err(_) => std::ptr::null_mut(),
    }
}

/// Clear all coredumps from the registry.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_nativelib_NativeMethodBindings_nativeCoredumpClearAll(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    coredump::clear_all();
    0
}
