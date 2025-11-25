//! Panama FFI bindings for WASI Preview 2 clocks operations
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI Preview 2 clocks operations (wasi:clocks).
//!
//! All functions use C calling conventions and handle memory management appropriately.

use std::ffi::CString;
use std::os::raw::{c_char, c_int, c_longlong, c_void};

use crate::wasi_clocks_helpers;
use crate::wasi_preview2::WasiPreview2Context;

/// Get the current monotonic clock instant in nanoseconds
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_instant`: Pointer to write the instant value (nanoseconds)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_monotonic_clock_now(
    context_handle: *mut c_void,
    out_instant: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_instant.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_clocks_helpers::monotonic_now(context) {
        Ok(instant) => {
            unsafe {
                *out_instant = instant as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get the monotonic clock resolution in nanoseconds
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_resolution`: Pointer to write the resolution value (nanoseconds)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_monotonic_clock_resolution(
    context_handle: *mut c_void,
    out_resolution: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_resolution.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_clocks_helpers::monotonic_resolution(context) {
        Ok(resolution) => {
            unsafe {
                *out_resolution = resolution as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Subscribe to monotonic clock at a specific instant
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `when`: The instant to subscribe to (nanoseconds)
/// - `out_pollable_id`: Pointer to write the pollable ID
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant(
    context_handle: *mut c_void,
    when: c_longlong,
    out_pollable_id: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_pollable_id.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_clocks_helpers::monotonic_subscribe_instant(context, when as u64) {
        Ok(pollable_id) => {
            unsafe {
                *out_pollable_id = pollable_id as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Subscribe to monotonic clock for a duration
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `duration`: The duration to wait (nanoseconds)
/// - `out_pollable_id`: Pointer to write the pollable ID
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration(
    context_handle: *mut c_void,
    duration: c_longlong,
    out_pollable_id: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_pollable_id.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_clocks_helpers::monotonic_subscribe_duration(context, duration as u64) {
        Ok(pollable_id) => {
            unsafe {
                *out_pollable_id = pollable_id as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get the current wall clock time
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_seconds`: Pointer to write seconds since Unix epoch
/// - `out_nanoseconds`: Pointer to write nanoseconds component
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_wall_clock_now(
    context_handle: *mut c_void,
    out_seconds: *mut c_longlong,
    out_nanoseconds: *mut c_int,
) -> c_int {
    if context_handle.is_null() || out_seconds.is_null() || out_nanoseconds.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_clocks_helpers::wall_clock_now(context) {
        Ok(datetime) => {
            unsafe {
                *out_seconds = datetime.seconds as c_longlong;
                *out_nanoseconds = datetime.nanoseconds as c_int;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get the wall clock resolution
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_seconds`: Pointer to write seconds component of resolution
/// - `out_nanoseconds`: Pointer to write nanoseconds component of resolution
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_wall_clock_resolution(
    context_handle: *mut c_void,
    out_seconds: *mut c_longlong,
    out_nanoseconds: *mut c_int,
) -> c_int {
    if context_handle.is_null() || out_seconds.is_null() || out_nanoseconds.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Call helper function
    match wasi_clocks_helpers::wall_clock_resolution(context) {
        Ok(datetime) => {
            unsafe {
                *out_seconds = datetime.seconds as c_longlong;
                *out_nanoseconds = datetime.nanoseconds as c_int;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get timezone UTC offset in seconds for a specific datetime
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `seconds`: Seconds since Unix epoch
/// - `nanoseconds`: Nanoseconds component
/// - `out_offset`: Pointer to write UTC offset in seconds
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_timezone_utc_offset(
    context_handle: *mut c_void,
    seconds: c_longlong,
    nanoseconds: c_int,
    out_offset: *mut c_int,
) -> c_int {
    if context_handle.is_null() || out_offset.is_null() {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Create DateTime
    let datetime = wasi_clocks_helpers::DateTime {
        seconds: seconds as u64,
        nanoseconds: nanoseconds as u32,
    };

    // Call helper function
    match wasi_clocks_helpers::timezone_utc_offset(context, datetime) {
        Ok(offset) => {
            unsafe {
                *out_offset = offset;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get timezone display information for a specific datetime
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `seconds`: Seconds since Unix epoch
/// - `nanoseconds`: Nanoseconds component
/// - `out_utc_offset`: Pointer to write UTC offset in seconds
/// - `out_name`: Pointer to write timezone name buffer pointer (must be freed by caller)
/// - `out_name_len`: Pointer to write timezone name length
/// - `out_in_dst`: Pointer to write DST status (1 = true, 0 = false)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_timezone_display(
    context_handle: *mut c_void,
    seconds: c_longlong,
    nanoseconds: c_int,
    out_utc_offset: *mut c_int,
    out_name: *mut *mut c_char,
    out_name_len: *mut c_longlong,
    out_in_dst: *mut c_char,
) -> c_int {
    if context_handle.is_null()
        || out_utc_offset.is_null()
        || out_name.is_null()
        || out_name_len.is_null()
        || out_in_dst.is_null()
    {
        return -1;
    }

    // Get context from handle
    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    // Create DateTime
    let datetime = wasi_clocks_helpers::DateTime {
        seconds: seconds as u64,
        nanoseconds: nanoseconds as u32,
    };

    // Call helper function
    match wasi_clocks_helpers::timezone_display(context, datetime) {
        Ok(display) => {
            // Convert name to C string
            let name_cstring = match CString::new(display.name) {
                Ok(s) => s,
                Err(_) => return -1,
            };
            let name_bytes = name_cstring.into_bytes_with_nul();
            let name_len = name_bytes.len() as c_longlong;

            // Allocate buffer for name
            let name_ptr = unsafe {
                let ptr = libc::malloc(name_len as usize) as *mut c_char;
                if ptr.is_null() {
                    return -1;
                }
                std::ptr::copy_nonoverlapping(
                    name_bytes.as_ptr() as *const c_char,
                    ptr,
                    name_len as usize,
                );
                ptr
            };

            // Set output parameters
            unsafe {
                *out_utc_offset = display.utc_offset_seconds;
                *out_name = name_ptr;
                *out_name_len = name_len;
                *out_in_dst = if display.in_daylight_saving_time { 1 } else { 0 };
            }
            0
        }
        Err(_) => -1,
    }
}

/// Free a timezone name buffer allocated by wasmtime4j_panama_wasi_timezone_display
///
/// # Parameters
/// - `name_ptr`: Pointer to the timezone name buffer to free
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_timezone_free_name(name_ptr: *mut c_char) {
    if !name_ptr.is_null() {
        unsafe {
            libc::free(name_ptr as *mut c_void);
        }
    }
}
