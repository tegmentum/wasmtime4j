// Copyright 2025 Tegmentum AI
//
// Licensed under the Apache License, Version 2.0

//! JNI bindings for guest profiler operations.
//!
//! Bridges JNI types to the C-ABI functions in `crate::guest_profiler`.

use std::ffi::{c_char, c_int, c_long, c_ulong, c_void, CString};

use jni::objects::{JClass, JLongArray, JObjectArray, JString};
use jni::sys::{jbyteArray, jint, jlong};
use jni::JNIEnv;

/// Creates a new guest profiler.
///
/// JNI bridge for `JniGuestProfiler.nativeGuestProfilerCreate`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGuestProfiler_nativeGuestProfilerCreate(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
    module_name: JString,
    interval_nanos: jlong,
    module_handles: JLongArray,
    module_names: JObjectArray,
) -> jlong {
    // Convert module name to CString
    let name_str = match env.get_string(&module_name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return 0,
    };
    let c_name = match CString::new(name_str) {
        Ok(s) => s,
        Err(_) => return 0,
    };

    // Get module handle count
    let count = match env.get_array_length(&module_handles) {
        Ok(len) => len as usize,
        Err(_) => return 0,
    };

    // Extract module handles (jlong[]) into a Vec of raw pointers
    let mut handle_values = vec![0i64; count];
    if count > 0 {
        if env
            .get_long_array_region(&module_handles, 0, &mut handle_values)
            .is_err()
        {
            return 0;
        }
    }
    let module_ptrs: Vec<*const c_void> =
        handle_values.iter().map(|&h| h as *const c_void).collect();

    // Extract module names (String[]) into Vec of CString
    let mut c_names: Vec<CString> = Vec::with_capacity(count);
    for i in 0..count {
        let elem = match env.get_object_array_element(&module_names, i as i32) {
            Ok(e) => e,
            Err(_) => return 0,
        };
        let jstr: JString = elem.into();
        let s = match env.get_string(&jstr) {
            Ok(s) => s.to_string_lossy().into_owned(),
            Err(_) => return 0,
        };
        match CString::new(s) {
            Ok(cs) => c_names.push(cs),
            Err(_) => return 0,
        }
    }
    let name_ptrs: Vec<*const c_char> = c_names.iter().map(|cs| cs.as_ptr()).collect();

    unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_new(
            engine_handle as *const c_void,
            c_name.as_ptr(),
            interval_nanos as c_long,
            module_ptrs.as_ptr(),
            name_ptrs.as_ptr(),
            count as c_int,
        ) as jlong
    }
}

/// Creates a new guest profiler for a component.
///
/// JNI bridge for `JniGuestProfiler.nativeGuestProfilerCreateComponent`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGuestProfiler_nativeGuestProfilerCreateComponent(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
    component_name: JString,
    interval_nanos: jlong,
    component_handle: jlong,
    extra_module_handles: JLongArray,
    extra_module_names: JObjectArray,
) -> jlong {
    // Convert component name to CString
    let name_str = match env.get_string(&component_name) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => return 0,
    };
    let c_name = match CString::new(name_str) {
        Ok(s) => s,
        Err(_) => return 0,
    };

    // Get extra module count
    let count = match env.get_array_length(&extra_module_handles) {
        Ok(len) => len as usize,
        Err(_) => return 0,
    };

    // Extract extra module handles
    let mut handle_values = vec![0i64; count];
    if count > 0 {
        if env
            .get_long_array_region(&extra_module_handles, 0, &mut handle_values)
            .is_err()
        {
            return 0;
        }
    }
    let module_ptrs: Vec<*const c_void> =
        handle_values.iter().map(|&h| h as *const c_void).collect();

    // Extract extra module names
    let mut c_names: Vec<CString> = Vec::with_capacity(count);
    for i in 0..count {
        let elem = match env.get_object_array_element(&extra_module_names, i as i32) {
            Ok(e) => e,
            Err(_) => return 0,
        };
        let jstr: JString = elem.into();
        let s = match env.get_string(&jstr) {
            Ok(s) => s.to_string_lossy().into_owned(),
            Err(_) => return 0,
        };
        match CString::new(s) {
            Ok(cs) => c_names.push(cs),
            Err(_) => return 0,
        }
    }
    let name_ptrs: Vec<*const c_char> = c_names.iter().map(|cs| cs.as_ptr()).collect();

    unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_new_component(
            engine_handle as *const c_void,
            c_name.as_ptr(),
            interval_nanos as c_long,
            component_handle as *const c_void,
            module_ptrs.as_ptr(),
            name_ptrs.as_ptr(),
            count as c_int,
        ) as jlong
    }
}

/// Collects a stack sample.
///
/// JNI bridge for `JniGuestProfiler.nativeGuestProfilerSample`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGuestProfiler_nativeGuestProfilerSample(
    _env: JNIEnv,
    _class: JClass,
    profiler_handle: jlong,
    store_handle: jlong,
    delta_nanos: jlong,
) -> jint {
    unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_sample(
            profiler_handle as *mut c_void,
            store_handle as *mut c_void,
            delta_nanos as c_long,
        ) as jint
    }
}

/// Records a call hook transition.
///
/// JNI bridge for `JniGuestProfiler.nativeGuestProfilerCallHook`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGuestProfiler_nativeGuestProfilerCallHook(
    _env: JNIEnv,
    _class: JClass,
    profiler_handle: jlong,
    store_handle: jlong,
    hook_kind: jint,
) -> jint {
    unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_call_hook(
            profiler_handle as *mut c_void,
            store_handle as *mut c_void,
            hook_kind as c_int,
        ) as jint
    }
}

/// Finishes profiling and returns the profile data as a byte array.
///
/// JNI bridge for `JniGuestProfiler.nativeGuestProfilerFinish`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGuestProfiler_nativeGuestProfilerFinish(
    mut env: JNIEnv,
    _class: JClass,
    profiler_handle: jlong,
) -> jbyteArray {
    let mut data_ptr: *mut u8 = std::ptr::null_mut();
    let mut data_len: c_ulong = 0;

    let result = unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_finish(
            profiler_handle as *mut c_void,
            &mut data_ptr,
            &mut data_len,
        )
    };

    if result != 0 || data_ptr.is_null() || data_len == 0 {
        return std::ptr::null_mut();
    }

    // Copy data into a JNI byte array
    let slice = unsafe { std::slice::from_raw_parts(data_ptr, data_len as usize) };
    let jarray = match env.byte_array_from_slice(slice) {
        Ok(arr) => arr.into_raw(),
        Err(_) => std::ptr::null_mut(),
    };

    // Free the Rust-allocated data
    unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_free_data(data_ptr, data_len);
    }

    jarray
}

/// Destroys the native profiler.
///
/// JNI bridge for `JniGuestProfiler.nativeGuestProfilerDestroy`.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniGuestProfiler_nativeGuestProfilerDestroy(
    _env: JNIEnv,
    _class: JClass,
    profiler_handle: jlong,
) {
    unsafe {
        crate::guest_profiler::wasmtime4j_guest_profiler_destroy(profiler_handle as *mut c_void);
    }
}
