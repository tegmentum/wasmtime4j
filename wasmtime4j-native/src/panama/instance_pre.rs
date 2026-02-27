//! InstancePre management FFI module for Panama
//!
//! This module provides Panama FFI bindings for managing pre-instantiated WebAssembly modules,
//! allowing for faster instantiation of modules that have already been validated and prepared.

use std::os::raw::{c_int, c_void};

/// Instantiate from an InstancePre (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_instantiate(
    instance_pre_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> *mut c_void {
    if instance_pre_ptr.is_null() || store_ptr.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_instantiate(instance_pre_ptr, store_ptr) }
}

/// Asynchronously instantiate from an InstancePre (Panama FFI)
///
/// Requires the engine to be configured with `async_support(true)`.
#[cfg(feature = "async")]
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_instantiate_async(
    instance_pre_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> *mut c_void {
    if instance_pre_ptr.is_null() || store_ptr.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_instantiate_async(instance_pre_ptr, store_ptr) }
}

/// Check if InstancePre is valid (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_is_valid(
    instance_pre_ptr: *const c_void,
) -> c_int {
    if instance_pre_ptr.is_null() {
        return 0;
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_is_valid(instance_pre_ptr) }
}

/// Get instance count from InstancePre (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_instance_count(
    instance_pre_ptr: *const c_void,
) -> u64 {
    if instance_pre_ptr.is_null() {
        return 0;
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_instance_count(instance_pre_ptr) }
}

/// Get preparation time in nanoseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_preparation_time_ns(
    instance_pre_ptr: *const c_void,
) -> u64 {
    if instance_pre_ptr.is_null() {
        return 0;
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_preparation_time_ns(instance_pre_ptr) }
}

/// Get average instantiation time in nanoseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_avg_instantiation_time_ns(
    instance_pre_ptr: *const c_void,
) -> u64 {
    if instance_pre_ptr.is_null() {
        return 0;
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_avg_instantiation_time_ns(instance_pre_ptr) }
}

/// Get module handle from InstancePre (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_get_module(
    instance_pre_ptr: *const c_void,
) -> *mut c_void {
    if instance_pre_ptr.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::linker::wasmtime4j_instance_pre_get_module(instance_pre_ptr) }
}

/// Destroy an InstancePre (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_pre_destroy(instance_pre_ptr: *mut c_void) {
    if !instance_pre_ptr.is_null() {
        unsafe { crate::linker::wasmtime4j_instance_pre_destroy(instance_pre_ptr) }
    }
}
