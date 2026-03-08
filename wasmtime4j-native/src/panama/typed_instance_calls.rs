//! Typed C exports for common function call signatures.
//!
//! These bypass the 20-byte tagged union `WasmValue` representation entirely,
//! accepting and returning primitives directly. Combined with `Func::typed()`,
//! this eliminates all intermediate allocations on the Rust side.

use std::ffi::CStr;
use std::os::raw::{c_char, c_int, c_void};

use crate::error::{ffi_utils, WasmtimeError, WasmtimeResult};
use crate::instance::core as instance_core;
use crate::store::core as store_core;
use crate::store::Store;
use wasmtime::Func;

/// Resolve an exported function by name from an instance.
unsafe fn resolve_export_func(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
) -> WasmtimeResult<(Func, &'static mut Store)> {
    let instance = instance_core::get_instance_mut(instance_ptr)?;
    let store = store_core::get_store_mut(store_ptr)?;
    let name = CStr::from_ptr(function_name)
        .to_str()
        .map_err(|e| WasmtimeError::Function {
            message: format!("Invalid UTF-8 in function name: {}", e),
        })?;

    let func = instance
        .get_func_cached(store, name)?
        .ok_or_else(|| WasmtimeError::Function {
            message: format!("Export '{}' not found", name),
        })?;

    // Re-borrow store since get_func borrows it mutably then releases
    let store = store_core::get_store_mut(store_ptr)?;
    Ok((func, store))
}

/// Call a function with signature `() -> ()`.
///
/// Returns 0 on success, -1 on error (with error in thread-local).
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_void(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let (func, store) =
            resolve_export_func(instance_ptr, store_ptr, function_name)?;
        let mut store_guard = store.try_lock_store()?;
        let typed = func
            .typed::<(), ()>(&*store_guard)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Type mismatch for void call: {}", e),
            })?;
        typed
            .call(&mut *store_guard, ())
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("{}", e),
                backtrace: None,
            })
    })
}

/// Call a function with signature `(i32) -> i32`.
///
/// Writes result to `result_ptr`. Returns 0 on success, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_i32_to_i32(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    arg: i32,
    result_ptr: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let (func, store) =
            resolve_export_func(instance_ptr, store_ptr, function_name)?;
        let mut store_guard = store.try_lock_store()?;
        let typed = func
            .typed::<i32, i32>(&*store_guard)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Type mismatch for i32->i32 call: {}", e),
            })?;
        let result = typed
            .call(&mut *store_guard, arg)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("{}", e),
                backtrace: None,
            })?;
        if !result_ptr.is_null() {
            std::ptr::write(result_ptr, result);
        }
        Ok(())
    })
}

/// Call a function with signature `(i32, i32) -> i32`.
///
/// Writes result to `result_ptr`. Returns 0 on success, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_i32i32_to_i32(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    arg1: i32,
    arg2: i32,
    result_ptr: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let (func, store) =
            resolve_export_func(instance_ptr, store_ptr, function_name)?;
        let mut store_guard = store.try_lock_store()?;
        let typed = func
            .typed::<(i32, i32), i32>(&*store_guard)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Type mismatch for (i32,i32)->i32 call: {}", e),
            })?;
        let result = typed
            .call(&mut *store_guard, (arg1, arg2))
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("{}", e),
                backtrace: None,
            })?;
        if !result_ptr.is_null() {
            std::ptr::write(result_ptr, result);
        }
        Ok(())
    })
}

/// Call a function with signature `(i64) -> i64`.
///
/// Writes result to `result_ptr`. Returns 0 on success, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_i64_to_i64(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    arg: i64,
    result_ptr: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let (func, store) =
            resolve_export_func(instance_ptr, store_ptr, function_name)?;
        let mut store_guard = store.try_lock_store()?;
        let typed = func
            .typed::<i64, i64>(&*store_guard)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Type mismatch for i64->i64 call: {}", e),
            })?;
        let result = typed
            .call(&mut *store_guard, arg)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("{}", e),
                backtrace: None,
            })?;
        if !result_ptr.is_null() {
            std::ptr::write(result_ptr, result);
        }
        Ok(())
    })
}

/// Call a function with signature `(f64) -> f64`.
///
/// Writes result to `result_ptr`. Returns 0 on success, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_f64_to_f64(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    arg: f64,
    result_ptr: *mut f64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let (func, store) =
            resolve_export_func(instance_ptr, store_ptr, function_name)?;
        let mut store_guard = store.try_lock_store()?;
        let typed = func
            .typed::<f64, f64>(&*store_guard)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Type mismatch for f64->f64 call: {}", e),
            })?;
        let result = typed
            .call(&mut *store_guard, arg)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("{}", e),
                backtrace: None,
            })?;
        if !result_ptr.is_null() {
            std::ptr::write(result_ptr, result);
        }
        Ok(())
    })
}

/// Call a function with signature `() -> i32`.
///
/// Writes result to `result_ptr`. Returns 0 on success, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_instance_call_to_i32(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    result_ptr: *mut i32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let (func, store) =
            resolve_export_func(instance_ptr, store_ptr, function_name)?;
        let mut store_guard = store.try_lock_store()?;
        let typed = func
            .typed::<(), i32>(&*store_guard)
            .map_err(|e| WasmtimeError::Function {
                message: format!("Type mismatch for ()->i32 call: {}", e),
            })?;
        let result = typed
            .call(&mut *store_guard, ())
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("{}", e),
                backtrace: None,
            })?;
        if !result_ptr.is_null() {
            std::ptr::write(result_ptr, result);
        }
        Ok(())
    })
}
