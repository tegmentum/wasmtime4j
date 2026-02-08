//! Panama FFI bindings for Caller operations
//!
//! This module provides C-compatible functions for accessing WebAssembly
//! execution context from host functions, enabling memory access, fuel
//! management, and export introspection.

use std::os::raw::{c_char, c_int, c_ulong, c_void};
use crate::caller::core;
use crate::error::ffi_utils;
use crate::store::StoreData;
use wasmtime::Caller as WasmtimeCaller;

/// Get fuel consumed by the caller if fuel metering is enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_fuel(
    caller_ptr: *mut c_void,
    fuel_out: *mut c_ulong,
) -> c_int {
    if caller_ptr.is_null() || fuel_out.is_null() {
        return -1; // Error: null pointer
    }

    let result = (|| -> crate::WasmtimeResult<c_int> {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_fuel(caller)? {
            Some(fuel) => {
                unsafe { *fuel_out = fuel; }
                Ok(0) // Success
            }
            None => Ok(-1), // Fuel metering not enabled
        }
    })();

    match result {
        Ok(code) => code,
        Err(e) => {
            ffi_utils::set_last_error(e);
            -1
        }
    }
}

/// Get fuel remaining in the caller if fuel metering is enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_fuel_remaining(
    caller_ptr: *mut c_void,
    fuel_out: *mut c_ulong,
) -> c_int {
    if caller_ptr.is_null() || fuel_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        match core::caller_get_fuel_remaining(caller)? {
            Some(fuel) => {
                unsafe { *fuel_out = fuel; }
                Ok(()) // Success
            }
            None => Ok(()), // Fuel metering not enabled
        }
    })
}

/// Add fuel to the caller (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_add_fuel(
    caller_ptr: *mut c_void,
    fuel: c_ulong,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_add_fuel(caller, fuel)?;
        Ok(()) // Success
    })
}

/// Set epoch deadline for the caller (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_set_epoch_deadline(
    caller_ptr: *mut c_void,
    deadline: c_ulong,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        core::caller_set_epoch_deadline(caller, deadline)?;
        Ok(()) // Success
    })
}

/// Check if the caller has an active epoch deadline (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_has_epoch_deadline(
    caller_ptr: *mut c_void,
) -> c_int {
    if caller_ptr.is_null() {
        return -1; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
    match core::caller_has_epoch_deadline(caller) {
        Ok(true) => 1, // Has deadline
        Ok(false) => 0, // No deadline
        Err(_) => -1, // Error occurred
    }
}

/// Check if caller has an export with the given name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_has_export(
    caller_ptr: *mut c_void,
    name: *const c_char,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() {
        return -1; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1, // Invalid UTF-8
    };

    match core::caller_has_export(caller, name_str) {
        Ok(true) => 1, // Has export
        Ok(false) => 0, // No export
        Err(_) => -1, // Error occurred
    }
}

/// Get memory export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_memory(
    caller_ptr: *mut c_void,
    name: *const c_char,
    memory_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || memory_out.is_null() {
        return -1; // Error: null pointer
    }

    let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
    let name_str = match unsafe { std::ffi::CStr::from_ptr(name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1, // Invalid UTF-8
    };

    match core::caller_get_memory(caller, name_str) {
        Ok(Some(memory)) => {
            unsafe { *memory_out = Box::into_raw(Box::new(memory)) as *mut c_void; }
            1 // Memory found
        }
        Ok(None) => {
            unsafe { *memory_out = std::ptr::null_mut(); }
            0 // No memory export with this name
        }
        Err(_) => -1, // Error occurred
    }
}

/// Get function export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_function(
    caller_ptr: *mut c_void,
    name: *const c_char,
    function_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || function_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = unsafe { std::ffi::CStr::from_ptr(name) }.to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

        match core::caller_get_function(caller, name_str)? {
            Some(function) => {
                unsafe { *function_out = Box::into_raw(Box::new(function)) as *mut c_void; }
                Ok(()) // Success
            }
            None => {
                unsafe { *function_out = std::ptr::null_mut(); }
                Ok(()) // No function export with this name
            }
        }
    })
}

/// Get global export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_global(
    caller_ptr: *mut c_void,
    name: *const c_char,
    global_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || global_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = unsafe { std::ffi::CStr::from_ptr(name) }.to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

        match core::caller_get_global(caller, name_str)? {
            Some(global) => {
                unsafe { *global_out = Box::into_raw(Box::new(global)) as *mut c_void; }
                Ok(()) // Success
            }
            None => {
                unsafe { *global_out = std::ptr::null_mut(); }
                Ok(()) // No global export with this name
            }
        }
    })
}

/// Get table export from caller by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_caller_get_table(
    caller_ptr: *mut c_void,
    name: *const c_char,
    table_out: *mut *mut c_void,
) -> c_int {
    if caller_ptr.is_null() || name.is_null() || table_out.is_null() {
        return -1; // Error: null pointer
    }

    ffi_utils::ffi_try_code(|| {
        let caller = unsafe { &mut *(caller_ptr as *mut WasmtimeCaller<'_, StoreData>) };
        let name_str = unsafe { std::ffi::CStr::from_ptr(name) }.to_str()
            .map_err(|e| crate::error::WasmtimeError::Utf8Error { message: e.to_string() })?;

        match core::caller_get_table(caller, name_str)? {
            Some(table) => {
                unsafe { *table_out = Box::into_raw(Box::new(table)) as *mut c_void; }
                Ok(()) // Success
            }
            None => {
                unsafe { *table_out = std::ptr::null_mut(); }
                Ok(()) // No table export with this name
            }
        }
    })
}
