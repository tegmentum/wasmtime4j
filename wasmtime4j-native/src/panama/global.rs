//! Panama FFI bindings for WebAssembly global variables
//!
//! This module provides C-compatible functions for creating, getting, setting,
//! and managing WebAssembly global variables through the Panama Foreign Function Interface.

use crate::error::ffi_utils;
use crate::global::core;
use crate::store::Store;
use std::os::raw::{c_char, c_int, c_ulong, c_void};
use wasmtime::Mutability;

/// Create a new WebAssembly global variable (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_global_create(
    store_ptr: *mut c_void,
    value_type: c_int,
    mutability: c_int,
    i32_value: c_int,
    i64_value: c_ulong,
    f32_value: f64, // Use f64 for F32 to avoid precision loss in FFI
    f64_value: f64,
    ref_id_present: c_int,
    ref_id: c_ulong,
    v128_bytes_ptr: *const u8, // Pointer to 16-byte V128 value (can be null)
    name_ptr: *const c_char,
    global_ptr: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = crate::ffi_common::valtype_conversion::int_to_valtype(value_type)?;

        let mutability_enum = match mutability {
            0 => Mutability::Const,
            1 => Mutability::Var,
            _ => {
                return Err(crate::error::WasmtimeError::InvalidParameter {
                    message: format!("Invalid mutability: {}", mutability),
                })
            }
        };

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id)
        } else {
            None
        };

        // Extract V128 bytes if provided
        let v128_bytes = if !v128_bytes_ptr.is_null() {
            let mut bytes = [0u8; 16];
            unsafe {
                std::ptr::copy_nonoverlapping(v128_bytes_ptr, bytes.as_mut_ptr(), 16);
            }
            Some(bytes)
        } else {
            None
        };

        let initial_value = core::create_global_value(
            val_type.clone(),
            i32_value,
            i64_value as i64,
            f32_value as f32,
            f64_value,
            v128_bytes,
            ref_id_opt,
        )?;

        let name = if name_ptr.is_null() {
            None
        } else {
            Some(unsafe { ffi_utils::c_char_to_string(name_ptr)? })
        };

        let global = core::create_global(store, val_type, mutability_enum, initial_value, name)?;

        unsafe {
            *global_ptr = Box::into_raw(global) as *mut c_void;
        }

        Ok(())
    })
}

/// Get global variable value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_global_get(
    global_ptr: *mut c_void,
    store_ptr: *mut c_void,
    i32_value: *mut c_int,
    i64_value: *mut c_ulong,
    f32_value: *mut f64,
    f64_value: *mut f64,
    ref_id_present: *mut c_int,
    ref_id: *mut c_ulong,
    v128_bytes_ptr: *mut u8, // Output pointer for 16-byte V128 value (can be null)
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let global = unsafe { core::get_global_ref(global_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let value = core::get_global_value(global, store)?;
        let (i32_val, i64_val, f32_val, f64_val, ref_id_opt) = core::extract_global_value(&value);

        // Handle V128 separately to get all 16 bytes
        let v128_bytes = match &value {
            crate::global::GlobalValue::V128(bytes) => Some(*bytes),
            _ => None,
        };

        unsafe {
            if !i32_value.is_null() {
                *i32_value = i32_val;
            }
            if !i64_value.is_null() {
                *i64_value = i64_val as c_ulong;
            }
            if !f32_value.is_null() {
                *f32_value = f32_val as f64;
            }
            if !f64_value.is_null() {
                *f64_value = f64_val;
            }
            if !ref_id_present.is_null() {
                *ref_id_present = if ref_id_opt.is_some() { 1 } else { 0 };
            }
            if !ref_id.is_null() {
                *ref_id = ref_id_opt.unwrap_or(0);
            }
            if !v128_bytes_ptr.is_null() {
                if let Some(bytes) = v128_bytes {
                    std::ptr::copy_nonoverlapping(bytes.as_ptr(), v128_bytes_ptr, 16);
                }
            }
        }

        Ok(())
    })
}

/// Set global variable value (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_global_set(
    global_ptr: *mut c_void,
    store_ptr: *mut c_void,
    value_type: c_int,
    i32_value: c_int,
    i64_value: c_ulong,
    f32_value: f64,
    f64_value: f64,
    ref_id_present: c_int,
    ref_id: c_ulong,
    v128_bytes_ptr: *const u8, // Pointer to 16-byte V128 value (can be null)
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let global = unsafe { core::get_global_ref(global_ptr)? };
        let store = unsafe { ffi_utils::deref_ptr::<Store>(store_ptr, "store")? };

        let val_type = crate::ffi_common::valtype_conversion::int_to_valtype(value_type)?;

        let ref_id_opt = if ref_id_present != 0 {
            Some(ref_id)
        } else {
            None
        };

        // Extract V128 bytes if provided
        let v128_bytes = if !v128_bytes_ptr.is_null() {
            let mut bytes = [0u8; 16];
            unsafe {
                std::ptr::copy_nonoverlapping(v128_bytes_ptr, bytes.as_mut_ptr(), 16);
            }
            Some(bytes)
        } else {
            None
        };

        let value = core::create_global_value(
            val_type,
            i32_value,
            i64_value as i64,
            f32_value as f32,
            f64_value,
            v128_bytes,
            ref_id_opt,
        )?;

        core::set_global_value(global, store, value)?;

        Ok(())
    })
}

/// Get global variable metadata (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_global_metadata(
    global_ptr: *mut c_void,
    value_type: *mut c_int,
    mutability: *mut c_int,
    name_ptr: *mut *mut c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let global = unsafe { core::get_global_ref(global_ptr)? };
        let metadata = core::get_global_metadata(global);

        unsafe {
            if !value_type.is_null() {
                *value_type = crate::ffi_common::valtype_conversion::valtype_to_int(&metadata.value_type);
            }
            if !mutability.is_null() {
                *mutability = match metadata.mutability {
                    Mutability::Const => 0,
                    Mutability::Var => 1,
                };
            }
            if !name_ptr.is_null() {
                *name_ptr = if let Some(ref name) = metadata.name {
                    ffi_utils::string_to_c_char(name.clone())?
                } else {
                    std::ptr::null_mut()
                };
            }
        }

        Ok(())
    })
}

/// Destroy a global variable (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_global_destroy(global_ptr: *mut c_void) {
    unsafe {
        core::destroy_global(global_ptr);
    }
}
