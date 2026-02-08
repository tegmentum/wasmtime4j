//! Panama FFI bindings for WebAssembly instance operations.
//!
//! This module provides C-compatible functions for instantiating WebAssembly
//! modules, invoking exported functions, and managing runtime execution state.

use std::os::raw::{c_char, c_int, c_void};

use crate::error::ffi_utils;

/// Instantiate a WebAssembly module (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_create(
    store_ptr: *mut c_void,
    module_ptr: *mut c_void,
    instance_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };
        let module = unsafe { crate::module::core::get_module_ref(module_ptr)? };

        let instance = crate::instance::core::create_instance(store, module)?;

        unsafe {
            // SAFETY IMPROVEMENT: Using safe Box management utilities
            *instance_ptr = crate::ffi_common::memory_utils::box_into_raw_safe(instance) as *mut c_void;
        }

        Ok(())
    })
}

/// Destroy a WebAssembly instance (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_destroy(instance_ptr: *mut c_void) {
    unsafe {
        crate::instance::core::destroy_instance(instance_ptr);
    }
}

/// Check if instance has specific export (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_has_export(
    instance_ptr: *const c_void,
    name: *const c_char,
) -> c_int {
    if instance_ptr.is_null() || name.is_null() {
        return -1;
    }
    unsafe { crate::instance::wasmtime4j_instance_has_export(instance_ptr, name) }
}

/// Get number of exports in instance (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_export_count(
    instance_ptr: *const c_void,
) -> usize {
    if instance_ptr.is_null() {
        return 0;
    }
    unsafe { crate::instance::wasmtime4j_instance_export_count(instance_ptr) }
}

/// Dispose instance resources (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_dispose(instance_ptr: *mut c_void) -> c_int {
    if instance_ptr.is_null() {
        return -1;
    }
    unsafe { crate::instance::wasmtime4j_instance_dispose(instance_ptr) }
}

/// Check if instance is disposed (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_is_disposed(
    instance_ptr: *const c_void,
) -> c_int {
    if instance_ptr.is_null() {
        return -1;
    }
    unsafe { crate::instance::wasmtime4j_instance_is_disposed(instance_ptr) }
}

/// Call exported function with i32 parameters (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_call_i32_function(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    params: *const i32,
    param_count: usize,
) -> i32 {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return 0;
    }
    unsafe {
        crate::instance::wasmtime4j_instance_call_i32_function(
            instance_ptr,
            store_ptr,
            function_name,
            params,
            param_count,
        )
    }
}

/// Call exported function with no parameters that returns i32 (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_call_i32_function_no_params(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
) -> i32 {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return 0;
    }
    unsafe {
        crate::instance::wasmtime4j_instance_call_i32_function_no_params(
            instance_ptr,
            store_ptr,
            function_name,
        )
    }
}

/// Get instance creation timestamp in microseconds since epoch (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_created_at_micros(
    instance_ptr: *const c_void,
) -> u64 {
    if instance_ptr.is_null() {
        return 0;
    }
    unsafe { crate::instance::wasmtime4j_instance_created_at_micros(instance_ptr) }
}

/// Get instance metadata export count (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_metadata_export_count(
    instance_ptr: *const c_void,
) -> usize {
    if instance_ptr.is_null() {
        return 0;
    }
    unsafe { crate::instance::wasmtime4j_instance_metadata_export_count(instance_ptr) }
}

/// Call instance function with WasmValue parameters (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_call_function(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    params_ptr: *const c_void,
    param_count: usize,
    results_ptr: *mut c_void,
    max_results: usize,
) -> isize {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return -1;
    }
    unsafe {
        crate::instance::wasmtime4j_instance_call_function(
            instance_ptr,
            store_ptr,
            function_name,
            params_ptr,
            param_count,
            results_ptr,
            max_results,
        )
    }
}

/// Call instance function asynchronously with timeout (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_call_function_async(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    params_ptr: *const c_void,
    param_count: usize,
    results_ptr: *mut c_void,
    max_results: usize,
    timeout_ms: u64,
) -> isize {
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        return -1;
    }
    unsafe {
        crate::instance::wasmtime4j_instance_call_function_async(
            instance_ptr,
            store_ptr,
            function_name,
            params_ptr,
            param_count,
            results_ptr,
            max_results,
            timeout_ms,
        )
    }
}

/// Get exported memory by name (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_memory_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        crate::instance::wasmtime4j_instance_get_memory_by_name(instance_ptr, store_ptr, name)
    }
}

/// Get exported table by name (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_table_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        crate::instance::wasmtime4j_instance_get_table_by_name(instance_ptr, store_ptr, name)
    }
}

/// Get exported global by name (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_global_by_name(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        crate::instance::wasmtime4j_instance_get_global_by_name(instance_ptr, store_ptr, name)
    }
}

/// Get exported global by name, properly wrapped for linker use (Panama FFI)
///
/// Unlike `wasmtime4j_panama_instance_get_global_by_name` which returns a raw wasmtime::Global,
/// this function wraps the global in the proper Global struct that can be used with
/// `wasmtime4j_panama_linker_define_global`.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_instance_get_global_wrapped(
    instance_ptr: *const c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
) -> *mut c_void {
    use crate::global::Global;

    if instance_ptr.is_null() || store_ptr.is_null() || name.is_null() {
        return std::ptr::null_mut();
    }

    ffi_utils::ffi_try_ptr(|| {
        let name_str = unsafe {
            std::ffi::CStr::from_ptr(name)
                .to_str()
                .map_err(|e| crate::error::WasmtimeError::Utf8Error {
                    message: e.to_string(),
                })?
        };

        let instance = unsafe { crate::instance::core::get_instance_ref(instance_ptr)? };
        let store = unsafe { crate::store::core::get_store_mut(store_ptr)? };

        // Get the raw wasmtime global from the instance
        let wasmtime_global = crate::instance::core::get_exported_global(instance, store, name_str)?
            .ok_or_else(|| crate::error::WasmtimeError::ExportNotFound {
                name: name_str.to_string(),
            })?;

        // Wrap it in our Global struct so it can be used with the linker
        let wrapped_global = Global::from_wasmtime_global(
            wasmtime_global,
            store,
            Some(name_str.to_string()),
        )?;

        Ok(Box::new(wrapped_global))
    })
}
