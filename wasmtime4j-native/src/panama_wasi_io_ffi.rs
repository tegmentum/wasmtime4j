//! Panama FFI bindings for WASI I/O streams
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI I/O stream operations (wasi:io).
//!
//! All functions use C calling conventions and handle memory management appropriately.
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.
//!
//! ## Phase 2 Consolidation
//!
//! Stream operations now use the unified `wasi_stream_ops` trait-based abstraction,
//! eliminating code duplication between Panama FFI, JNI, and Preview 2 implementations.

use std::os::raw::{c_long, c_void};
use std::panic::AssertUnwindSafe;
use std::slice;

use crate::error::ffi_utils::ffi_try_code;
use crate::ffi_boundary_ptr;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi::WasiContext;
use crate::wasi_stream_ops::{
    check_write_capacity_generic, close_stream_generic, flush_stream_generic,
    read_from_stream_generic, skip_in_stream_generic, splice_streams_generic,
    write_to_stream_generic, write_zeroes_to_stream_generic,
};

// ============================================================================
// Wrapper functions that delegate to generic implementations
// ============================================================================

/// Read data from a stream (uses generic trait-based implementation)
#[inline]
fn read_from_stream(
    context: &WasiContext,
    stream_id: u64,
    length: usize,
    blocking: bool,
) -> WasmtimeResult<Vec<u8>> {
    read_from_stream_generic(context, stream_id, length, blocking)
}

/// Skip bytes in a stream (uses generic trait-based implementation)
#[inline]
fn skip_in_stream(
    context: &WasiContext,
    stream_id: u64,
    length: u64,
    blocking: bool,
) -> WasmtimeResult<u64> {
    skip_in_stream_generic(context, stream_id, length, blocking)
}

/// Close a stream (uses generic trait-based implementation)
#[inline]
fn close_stream(context: &WasiContext, stream_id: u64) -> WasmtimeResult<()> {
    close_stream_generic(context, stream_id)
}

/// Check write capacity for an output stream (uses generic trait-based implementation)
#[inline]
fn check_write_capacity(context: &WasiContext, stream_id: u64) -> WasmtimeResult<u64> {
    check_write_capacity_generic(context, stream_id)
}

/// Write data to a stream (uses generic trait-based implementation)
#[inline]
fn write_to_stream(
    context: &WasiContext,
    stream_id: u64,
    data: &[u8],
    blocking: bool,
) -> WasmtimeResult<()> {
    write_to_stream_generic(context, stream_id, data, blocking)
}

/// Flush a stream (uses generic trait-based implementation)
#[inline]
fn flush_stream(context: &WasiContext, stream_id: u64, blocking: bool) -> WasmtimeResult<()> {
    flush_stream_generic(context, stream_id, blocking)
}

/// Write zeroes to a stream (uses generic trait-based implementation)
#[inline]
fn write_zeroes_to_stream(
    context: &WasiContext,
    stream_id: u64,
    length: u64,
    blocking: bool,
) -> WasmtimeResult<()> {
    write_zeroes_to_stream_generic(context, stream_id, length, blocking)
}

/// Splice data between streams (uses generic trait-based implementation)
#[inline]
fn splice_streams(
    context: &WasiContext,
    dest_stream_id: u64,
    source_stream_id: u64,
    length: u64,
    blocking: bool,
) -> WasmtimeResult<u64> {
    splice_streams_generic(context, dest_stream_id, source_stream_id, length, blocking)
}

/// Create a pollable for a stream, registering it in the pollable registry.
fn create_stream_pollable(context: &WasiContext, stream_id: u64) -> WasmtimeResult<u64> {
    let pollable_id = context
        .next_operation_id
        .fetch_add(1, std::sync::atomic::Ordering::SeqCst) as u32;
    let pollable = crate::wasi_preview2::WasiPollable::new(pollable_id, stream_id);
    let mut pollables = context
        .pollables
        .write()
        .map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock pollable registry for write: {}", e),
        })?;
    pollables.insert(pollable_id, pollable);
    Ok(pollable_id as u64)
}

/// Check if a pollable is ready by looking up the pollable registry.
///
/// For stream pollables, readiness is determined by whether the associated
/// stream is open and not closed. For timer pollables, readiness is based
/// on elapsed time.
fn check_pollable_ready(context: &WasiContext, pollable_id: u64) -> WasmtimeResult<bool> {
    let pollables = context
        .pollables
        .read()
        .map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock pollable registry for read: {}", e),
        })?;

    if let Some(pollable) = pollables.get(&(pollable_id as u32)) {
        // Check timer-based readiness first
        if pollable.is_ready() {
            return Ok(true);
        }
        // For stream pollables, check if the associated stream is open
        use crate::wasi_stream_ops::{WasiStreamContext, WasiStreamEntry};
        let streams = context.streams_read()?;
        if let Some(stream) = streams.get(&(pollable.resource_id as u32)) {
            Ok(!stream.is_closed())
        } else {
            // Stream no longer exists — treat as ready (closed/EOF)
            Ok(true)
        }
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: format!("Pollable {} not found in registry", pollable_id),
        })
    }
}

/// Block on a pollable until it becomes ready or the timeout expires.
fn block_on_pollable(
    context: &WasiContext,
    pollable_id: u64,
    timeout: Option<u64>,
) -> WasmtimeResult<()> {
    let deadline =
        timeout.map(|ms| std::time::Instant::now() + std::time::Duration::from_millis(ms));
    loop {
        match check_pollable_ready(context, pollable_id) {
            Ok(true) => return Ok(()),
            Ok(false) => {
                if let Some(dl) = deadline {
                    if std::time::Instant::now() >= dl {
                        return Ok(()); // Timed out — return without error
                    }
                }
                // Brief sleep to avoid busy-spinning
                std::thread::sleep(std::time::Duration::from_millis(1));
            }
            Err(e) => return Err(e),
        }
    }
}

/// Close a pollable by removing it from the registry.
fn close_pollable(context: &WasiContext, pollable_id: u64) -> WasmtimeResult<()> {
    let mut pollables = context
        .pollables
        .write()
        .map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to lock pollable registry for write: {}", e),
        })?;
    pollables.remove(&(pollable_id as u32));
    Ok(())
}

// ============================================================================
// FFI Functions
// ============================================================================

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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null()
            || stream_handle.is_null()
            || out_buffer.is_null()
            || out_length.is_null()
            || length < 0
        {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, stream_handle, out_buffer, and out_length must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        let data = read_from_stream(context, stream_id, length as usize, false)?;
        let copy_len = data.len().min(length as usize);
        unsafe {
            std::ptr::copy_nonoverlapping(data.as_ptr(), out_buffer, copy_len);
            *out_length = copy_len as c_long;
        }
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null()
            || stream_handle.is_null()
            || out_buffer.is_null()
            || out_length.is_null()
            || length < 0
        {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, stream_handle, out_buffer, and out_length must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        let data = read_from_stream(context, stream_id, length as usize, true)?;
        let copy_len = data.len().min(length as usize);
        unsafe {
            std::ptr::copy_nonoverlapping(data.as_ptr(), out_buffer, copy_len);
            *out_length = copy_len as c_long;
        }
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null()
            || stream_handle.is_null()
            || out_skipped.is_null()
            || length < 0
        {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, stream_handle, and out_skipped must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        let skipped = skip_in_stream(context, stream_id, length as u64, false)?;
        unsafe {
            *out_skipped = skipped as c_long;
        }
        Ok(())
    }))
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
    ffi_boundary_ptr!({
        if context_handle.is_null() || stream_handle.is_null() {
            return std::ptr::null_mut();
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return std::ptr::null_mut();
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        match create_stream_pollable(context, stream_id) {
            Ok(pollable_id) => pollable_id as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        }
    })
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and stream_handle must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        close_stream(context, stream_id)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() || out_capacity.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, stream_handle, and out_capacity must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        let capacity = check_write_capacity(context, stream_id)?;
        unsafe {
            *out_capacity = capacity as c_long;
        }
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() || buffer.is_null() || length < 0 {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, stream_handle, and buffer must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;
        let data = unsafe { slice::from_raw_parts(buffer, length as usize) };

        write_to_stream(context, stream_id, data, false)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() || buffer.is_null() || length < 0 {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, stream_handle, and buffer must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;
        let data = unsafe { slice::from_raw_parts(buffer, length as usize) };

        write_to_stream(context, stream_id, data, true)?;
        flush_stream(context, stream_id, true)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and stream_handle must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        flush_stream(context, stream_id, false)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and stream_handle must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        flush_stream(context, stream_id, true)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() || length < 0 {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and stream_handle must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        write_zeroes_to_stream(context, stream_id, length as u64, false)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() || length < 0 {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and stream_handle must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        write_zeroes_to_stream(context, stream_id, length as u64, true)?;
        flush_stream(context, stream_id, true)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null()
            || output_stream_handle.is_null()
            || input_stream_handle.is_null()
            || out_spliced.is_null()
            || length < 0
        {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, output_stream_handle, input_stream_handle, and out_spliced must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let dest_stream_id = output_stream_handle as u64;
        let source_stream_id = input_stream_handle as u64;

        let spliced = splice_streams(
            context,
            dest_stream_id,
            source_stream_id,
            length as u64,
            false,
        )?;
        unsafe {
            *out_spliced = spliced as c_long;
        }
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null()
            || output_stream_handle.is_null()
            || input_stream_handle.is_null()
            || out_spliced.is_null()
            || length < 0
        {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, output_stream_handle, input_stream_handle, and out_spliced must not be null, and length must not be negative",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let dest_stream_id = output_stream_handle as u64;
        let source_stream_id = input_stream_handle as u64;

        let spliced = splice_streams(
            context,
            dest_stream_id,
            source_stream_id,
            length as u64,
            true,
        )?;
        unsafe {
            *out_spliced = spliced as c_long;
        }
        Ok(())
    }))
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
    ffi_boundary_ptr!({
        if context_handle.is_null() || stream_handle.is_null() {
            return std::ptr::null_mut();
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return std::ptr::null_mut();
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        match create_stream_pollable(context, stream_id) {
            Ok(pollable_id) => pollable_id as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        }
    })
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || stream_handle.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and stream_handle must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };

        let stream_id = stream_handle as u64;

        close_stream(context, stream_id)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || pollable_handle.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and pollable_handle must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };
        let pollable_id = pollable_handle as u64;

        block_on_pollable(context, pollable_id, None)?;
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || pollable_handle.is_null() || out_ready.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle, pollable_handle, and out_ready must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };
        let pollable_id = pollable_handle as u64;

        let ready = check_pollable_ready(context, pollable_id)?;
        unsafe {
            *out_ready = if ready { 1 } else { 0 };
        }
        Ok(())
    }))
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
    ffi_try_code(AssertUnwindSafe(|| {
        if context_handle.is_null() || pollable_handle.is_null() {
            return Err(WasmtimeError::invalid_parameter(
                "context_handle and pollable_handle must not be null",
            ));
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Err(WasmtimeError::invalid_parameter(
                    "context_handle must not be null",
                ));
            }
            &*ptr
        };
        let pollable_id = pollable_handle as u64;

        close_pollable(context, pollable_id)?;
        Ok(())
    }))
}
