//! Panama FFI bindings for WASI Preview 2 random operations
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI Preview 2 random operations (wasi:random).
//!
//! All functions use C calling conventions and handle memory management appropriately.
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.

use std::os::raw::{c_int, c_longlong, c_uchar, c_void};
use std::ptr;

use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_random_helpers;
use crate::{ffi_boundary_i32, ffi_boundary_void};

/// Get cryptographically-secure random bytes
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `len`: Number of random bytes to generate
/// - `out_bytes`: Pointer to buffer for random bytes
/// - `out_bytes_len`: Pointer to write actual number of bytes generated
///
/// # Returns
/// 0 on success, -1 on error
///
/// # Memory Management
/// The caller is responsible for freeing the returned buffer using wasmtime4j_panama_free_buffer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_random_get_bytes(
    context_handle: *mut c_void,
    len: c_longlong,
    out_bytes: *mut *mut c_uchar,
    out_bytes_len: *mut c_longlong,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_bytes.is_null() || out_bytes_len.is_null() {
            return Ok(-1);
        }

        if len < 0 {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiPreview2Context;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let bytes = match wasi_random_helpers::get_random_bytes(context, len as u64) {
            Ok(b) => b,
            Err(_) => return Ok(-1),
        };

        let bytes_len = bytes.len();
        let buffer = unsafe {
            let ptr = libc::malloc(bytes_len) as *mut c_uchar;
            if ptr.is_null() {
                return Ok(-1);
            }
            ptr::copy_nonoverlapping(bytes.as_ptr(), ptr, bytes_len);
            ptr
        };

        unsafe {
            *out_bytes = buffer;
            *out_bytes_len = bytes_len as c_longlong;
        }

        Ok(0)
    })
}

/// Get a cryptographically-secure random u64 value
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_value`: Pointer to write the random u64 value
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_random_get_u64(
    context_handle: *mut c_void,
    out_value: *mut c_longlong,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_value.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiPreview2Context;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        match wasi_random_helpers::get_random_u64(context) {
            Ok(value) => {
                unsafe {
                    *out_value = value as c_longlong;
                }
                Ok(0)
            }
            Err(_) => Ok(-1),
        }
    })
}

/// Free a buffer allocated by wasi random functions
///
/// # Parameters
/// - `buffer`: Pointer to buffer to free
///
/// # Safety
/// The buffer must have been allocated by one of the wasi random functions.
/// Double-freeing or freeing invalid pointers will cause undefined behavior.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_random_free_buffer(buffer: *mut c_uchar) {
    ffi_boundary_void!({
        if !buffer.is_null() {
            unsafe {
                libc::free(buffer as *mut c_void);
            }
        }
    })
}
