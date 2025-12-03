//! Panama FFI bindings for WASI Preview 2 I/O streams
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI Preview 2 I/O stream operations (wasi:io).
//!
//! All functions use C calling conventions and handle memory management appropriately.

use std::os::raw::{c_long, c_void};
use std::slice;

use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_io_helpers;

/// Read data from a WASI input stream (non-blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the input stream (stream_id as pointer)
/// - `length`: Maximum number of bytes to read
/// - `out_buffer`: Pointer to buffer where data will be written
/// - `out_length`: Pointer to write the actual number of bytes read
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_input_stream_read(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    length: c_long,
    out_buffer: *mut u8,
    out_length: *mut c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || out_buffer.is_null() || out_length.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::read_from_stream(context, stream_id, length as usize, false) {
        Ok(data) => {
            let copy_len = data.len().min(length as usize);
            unsafe {
                std::ptr::copy_nonoverlapping(data.as_ptr(), out_buffer, copy_len);
                *out_length = copy_len as c_long;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Read data from a WASI input stream (blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the input stream (stream_id as pointer)
/// - `length`: Maximum number of bytes to read
/// - `out_buffer`: Pointer to buffer where data will be written
/// - `out_length`: Pointer to write the actual number of bytes read
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_input_stream_blocking_read(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    length: c_long,
    out_buffer: *mut u8,
    out_length: *mut c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || out_buffer.is_null() || out_length.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::read_from_stream(context, stream_id, length as usize, true) {
        Ok(data) => {
            let copy_len = data.len().min(length as usize);
            unsafe {
                std::ptr::copy_nonoverlapping(data.as_ptr(), out_buffer, copy_len);
                *out_length = copy_len as c_long;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Skip bytes in a WASI input stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the input stream (stream_id as pointer)
/// - `length`: Number of bytes to skip
/// - `out_skipped`: Pointer to write the actual number of bytes skipped
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_input_stream_skip(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    length: c_long,
    out_skipped: *mut c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || out_skipped.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::skip_in_stream(context, stream_id, length as u64, false) {
        Ok(skipped) => {
            unsafe {
                *out_skipped = skipped as c_long;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Create a pollable for a WASI input stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the input stream (stream_id as pointer)
///
/// # Returns
/// Pointer to the pollable on success, null on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_input_stream_subscribe(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
) -> *mut c_void {
    if context_handle.is_null() || stream_handle.is_null() {
        return std::ptr::null_mut();
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return std::ptr::null_mut();
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::create_output_stream_pollable(context, stream_id) {
        Ok(pollable_id) => pollable_id as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Close a WASI input stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the input stream (stream_id as pointer)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_input_stream_close(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::close_stream(context, stream_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Check how many bytes can be written to a WASI output stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `out_capacity`: Pointer to write the available capacity
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_check_write(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    out_capacity: *mut c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || out_capacity.is_null() {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::check_write_capacity(context, stream_id) {
        Ok(capacity) => {
            unsafe {
                *out_capacity = capacity as c_long;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Write data to a WASI output stream (non-blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `buffer`: Pointer to the data to write
/// - `length`: Number of bytes to write
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_write(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    buffer: *const u8,
    length: c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || buffer.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;
    let data = unsafe { slice::from_raw_parts(buffer, length as usize) };

    match wasi_io_helpers::write_to_stream(context, stream_id, data, false) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Write data and flush a WASI output stream (blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `buffer`: Pointer to the data to write
/// - `length`: Number of bytes to write
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_blocking_write_and_flush(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    buffer: *const u8,
    length: c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || buffer.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;
    let data = unsafe { slice::from_raw_parts(buffer, length as usize) };

    match wasi_io_helpers::write_to_stream(context, stream_id, data, true) {
        Ok(()) => match wasi_io_helpers::flush_stream(context, stream_id, true) {
            Ok(()) => 0,
            Err(_) => -1,
        },
        Err(_) => -1,
    }
}

/// Flush a WASI output stream (non-blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_flush(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::flush_stream(context, stream_id, false) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Flush a WASI output stream (blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_blocking_flush(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::flush_stream(context, stream_id, true) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Write zero bytes to a WASI output stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `length`: Number of zero bytes to write
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_write_zeroes(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    length: c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::write_zeroes_to_stream(context, stream_id, length as u64, false) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Write zero bytes and flush a WASI output stream (blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `length`: Number of zero bytes to write
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_blocking_write_zeroes_and_flush(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
    length: c_long,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::write_zeroes_to_stream(context, stream_id, length as u64, true) {
        Ok(()) => match wasi_io_helpers::flush_stream(context, stream_id, true) {
            Ok(()) => 0,
            Err(_) => -1,
        },
        Err(_) => -1,
    }
}

/// Splice data from input stream to output stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `output_stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `input_stream_handle`: Pointer to the input stream (stream_id as pointer)
/// - `length`: Maximum number of bytes to splice
/// - `out_spliced`: Pointer to write the actual number of bytes spliced
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_splice(
    context_handle: *mut c_void,
    output_stream_handle: *mut c_void,
    input_stream_handle: *mut c_void,
    length: c_long,
    out_spliced: *mut c_long,
) -> i32 {
    if context_handle.is_null() || output_stream_handle.is_null() || input_stream_handle.is_null() || out_spliced.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let dest_stream_id = output_stream_handle as u64;
    let source_stream_id = input_stream_handle as u64;

    match wasi_io_helpers::splice_streams(context, dest_stream_id, source_stream_id, length as u64, false) {
        Ok(spliced) => {
            unsafe {
                *out_spliced = spliced as c_long;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Splice data from input stream to output stream (blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `output_stream_handle`: Pointer to the output stream (stream_id as pointer)
/// - `input_stream_handle`: Pointer to the input stream (stream_id as pointer)
/// - `length`: Maximum number of bytes to splice
/// - `out_spliced`: Pointer to write the actual number of bytes spliced
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_blocking_splice(
    context_handle: *mut c_void,
    output_stream_handle: *mut c_void,
    input_stream_handle: *mut c_void,
    length: c_long,
    out_spliced: *mut c_long,
) -> i32 {
    if context_handle.is_null() || output_stream_handle.is_null() || input_stream_handle.is_null() || out_spliced.is_null() || length < 0 {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let dest_stream_id = output_stream_handle as u64;
    let source_stream_id = input_stream_handle as u64;

    match wasi_io_helpers::splice_streams(context, dest_stream_id, source_stream_id, length as u64, true) {
        Ok(spliced) => {
            unsafe {
                *out_spliced = spliced as c_long;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Create a pollable for a WASI output stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
///
/// # Returns
/// Pointer to the pollable on success, null on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_subscribe(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
) -> *mut c_void {
    if context_handle.is_null() || stream_handle.is_null() {
        return std::ptr::null_mut();
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return std::ptr::null_mut();
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::create_output_stream_pollable(context, stream_id) {
        Ok(pollable_id) => pollable_id as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Close a WASI output stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: Pointer to the output stream (stream_id as pointer)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_output_stream_close(
    context_handle: *mut c_void,
    stream_handle: *mut c_void,
) -> i32 {
    if context_handle.is_null() || stream_handle.is_null() {
        return -1;
    }

    let context = unsafe {
        let ptr = context_handle as *const WasiPreview2Context;
        if ptr.is_null() {
            return -1;
        }
        &*ptr
    };

    let stream_id = stream_handle as u64;

    match wasi_io_helpers::close_stream(context, stream_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Block until a WASI pollable is ready
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `pollable_handle`: Pointer to the pollable (pollable_id as pointer)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_pollable_block(
    context_handle: *mut c_void,
    pollable_handle: *mut c_void,
) -> i32 {
    if context_handle.is_null() || pollable_handle.is_null() {
        return -1;
    }

    let context = unsafe { &*(context_handle as *const crate::wasi_preview2::WasiPreview2Context) };
    let pollable_id = pollable_handle as u64;

    // Block on the pollable with no timeout
    match crate::wasi_io_helpers::block_on_pollable(context, pollable_id, None) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Check if a WASI pollable is ready (non-blocking)
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `pollable_handle`: Pointer to the pollable (pollable_id as pointer)
/// - `out_ready`: Pointer to write readiness status (1 = ready, 0 = not ready)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_pollable_ready(
    context_handle: *mut c_void,
    pollable_handle: *mut c_void,
    out_ready: *mut i32,
) -> i32 {
    if context_handle.is_null() || pollable_handle.is_null() || out_ready.is_null() {
        return -1;
    }

    let context = unsafe { &*(context_handle as *const crate::wasi_preview2::WasiPreview2Context) };
    let pollable_id = pollable_handle as u64;

    match crate::wasi_io_helpers::check_pollable_ready(context, pollable_id) {
        Ok(ready) => {
            unsafe {
                *out_ready = if ready { 1 } else { 0 };
            }
            0
        }
        Err(_) => -1,
    }
}

/// Close a WASI pollable
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `pollable_handle`: Pointer to the pollable (pollable_id as pointer)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_pollable_close(
    context_handle: *mut c_void,
    pollable_handle: *mut c_void,
) -> i32 {
    if context_handle.is_null() || pollable_handle.is_null() {
        return -1;
    }

    let context = unsafe { &*(context_handle as *const crate::wasi_preview2::WasiPreview2Context) };
    let pollable_id = pollable_handle as u64;

    match crate::wasi_io_helpers::close_pollable(context, pollable_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}
