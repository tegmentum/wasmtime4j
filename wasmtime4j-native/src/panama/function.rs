//! Panama FFI bindings for WebAssembly function operations
//!
//! This module provides C-compatible functions for creating, inspecting,
//! and calling WebAssembly functions through the Panama Foreign Function Interface.

use std::os::raw::{c_char, c_int, c_void};
use crate::instance::core;
use crate::store::Store;
use crate::error::ffi_utils;

/// Get function from instance export (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_get(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    name: *const c_char,
    func_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let instance = unsafe { core::get_instance_ref(instance_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };
        let name_str = unsafe { ffi_utils::c_char_to_string(name)? };

        let func_opt = core::get_function_export(instance, store, &name_str)?;

        match func_opt {
            Some(func) => {
                unsafe {
                    *func_ptr = Box::into_raw(Box::new(func)) as *mut c_void;
                }
                Ok(())
            }
            None => Err(crate::error::WasmtimeError::ExportNotFound {
                name: name_str,
            }),
        }
    })
}

/// Get function parameter types (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_get_param_types(
    func_ptr: *mut c_void,
    store_ptr: *mut c_void,
    types_ptr: *mut *mut c_int,
    count_ptr: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let func = unsafe { core::get_function_ref(func_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let param_types = core::get_function_param_types(func, store)?;
        let count = param_types.len();

        if count > 0 {
            let types_array = param_types.into_boxed_slice();
            unsafe {
                *types_ptr = Box::into_raw(types_array) as *mut c_int;
                *count_ptr = count;
            }
        } else {
            unsafe {
                *types_ptr = std::ptr::null_mut();
                *count_ptr = 0;
            }
        }

        Ok(())
    })
}

/// Get function result types (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_get_result_types(
    func_ptr: *mut c_void,
    store_ptr: *mut c_void,
    types_ptr: *mut *mut c_int,
    count_ptr: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let func = unsafe { core::get_function_ref(func_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let result_types = core::get_function_result_types(func, store)?;
        let count = result_types.len();

        if count > 0 {
            let types_array = result_types.into_boxed_slice();
            unsafe {
                *types_ptr = Box::into_raw(types_array) as *mut c_int;
                *count_ptr = count;
            }
        } else {
            unsafe {
                *types_ptr = std::ptr::null_mut();
                *count_ptr = 0;
            }
        }

        Ok(())
    })
}

/// Call WebAssembly function (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_call(
    func_ptr: *mut c_void,
    store_ptr: *mut c_void,
    params_ptr: *const c_void,
    param_count: usize,
    results_ptr: *mut c_void,
    result_count: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let func = unsafe { core::get_function_ref(func_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr_mut::<Store>(store_ptr, "store")? };

        // Convert parameters from C representation to Rust WasmValue
        let params = if param_count > 0 {
            unsafe {
                core::convert_params_from_ffi(params_ptr, param_count)?
            }
        } else {
            Vec::new()
        };

        // Call the function
        let results = core::call_function(func, store, &params)?;

        // Convert results back to C representation
        if result_count > 0 && !results.is_empty() {
            unsafe {
                core::convert_results_to_ffi(&results, results_ptr, result_count)?;
            }
        }

        Ok(())
    })
}

/// Get function type (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_type(
    func_ptr: *mut c_void,
    store_ptr: *mut c_void,
) -> *mut c_void {
    match ffi_utils::ffi_try_ptr_result(|| {
        let func = unsafe { core::get_function_ref(func_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let func_type = core::get_function_type(func, store)?;
        Ok(Box::new(func_type))
    }) {
        Ok(boxed_type) => Box::into_raw(boxed_type) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Free function type (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_type_destroy(func_type_ptr: *mut c_void) {
    if !func_type_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(func_type_ptr as *mut wasmtime::FuncType);
        }
    }
}

/// Free parameter/result types array (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_free_types_array(types_ptr: *mut c_int, count: usize) {
    if !types_ptr.is_null() && count > 0 {
        unsafe {
            let _ = Box::from_raw(std::slice::from_raw_parts_mut(types_ptr, count));
        }
    }
}

/// Destroy a function handle (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_func_destroy(func_ptr: *mut c_void) {
    if !func_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(func_ptr as *mut wasmtime::Func);
        }
    }
}
