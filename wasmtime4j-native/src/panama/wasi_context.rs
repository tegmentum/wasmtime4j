//! WASI Context Configuration FFI module for Panama
//!
//! This module provides Panama FFI bindings for configuring WASI contexts,
//! including environment variables, arguments, stdio handling, and directory access.

use std::os::raw::{c_char, c_int, c_void};

/// Set command-line arguments for the WASI context (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_set_argv(
    ctx_ptr: *mut c_void,
    argv: *const *const c_char,
    argc: usize,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_set_argv(ctx_ptr, argv, argc) }
}

/// Set a single environment variable for the WASI context (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_set_env(
    ctx_ptr: *mut c_void,
    key: *const c_char,
    value: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() || key.is_null() || value.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_set_env(ctx_ptr, key, value) }
}

/// Inherit environment from host process (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_inherit_env(ctx_ptr: *mut c_void) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_inherit_env(ctx_ptr) }
}

/// Inherit stdio from host process (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_inherit_stdio(ctx_ptr: *mut c_void) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_inherit_stdio(ctx_ptr) }
}

/// Set stdin to read from file (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_set_stdin(
    ctx_ptr: *mut c_void,
    path: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_set_stdin(ctx_ptr, path) }
}

/// Set stdin bytes for the WASI context (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_set_stdin_bytes(
    ctx_ptr: *mut c_void,
    data: *const u8,
    len: usize,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_set_stdin_bytes(ctx_ptr, data, len) }
}

/// Set stdout to write to file (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_set_stdout(
    ctx_ptr: *mut c_void,
    path: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_set_stdout(ctx_ptr, path) }
}

/// Set stderr to write to file (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_set_stderr(
    ctx_ptr: *mut c_void,
    path: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_set_stderr(ctx_ptr, path) }
}

/// Enable output capture for stdout/stderr (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_enable_output_capture(
    ctx_ptr: *mut c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_enable_output_capture(ctx_ptr) }
}

/// Get captured stdout content (Panama FFI)
/// Returns pointer to captured data, caller must free with wasmtime4j_wasi_free_capture_buffer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_get_stdout_capture(
    ctx_ptr: *const c_void,
    data_len_out: *mut usize,
) -> *mut u8 {
    if ctx_ptr.is_null() || data_len_out.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_get_stdout_capture(ctx_ptr, data_len_out) }
}

/// Get captured stderr content (Panama FFI)
/// Returns pointer to captured data, caller must free with wasmtime4j_wasi_free_capture_buffer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_get_stderr_capture(
    ctx_ptr: *const c_void,
    data_len_out: *mut usize,
) -> *mut u8 {
    if ctx_ptr.is_null() || data_len_out.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_get_stderr_capture(ctx_ptr, data_len_out) }
}

/// Free capture buffer allocated by get_stdout/stderr_capture (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_free_capture_buffer(data: *mut u8) {
    if !data.is_null() {
        unsafe { crate::wasi::wasmtime4j_wasi_free_capture_buffer(data) }
    }
}

/// Check if stdout capture is available (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_has_stdout_capture(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_has_stdout_capture(ctx_ptr) }
}

/// Check if stderr capture is available (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_has_stderr_capture(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_has_stderr_capture(ctx_ptr) }
}

/// Preopen a directory for the WASI context (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_preopen_dir(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() || host_path.is_null() || guest_path.is_null() {
        return -1;
    }
    unsafe { crate::wasi::wasmtime4j_wasi_context_preopen_dir(ctx_ptr, host_path, guest_path) }
}

/// Preopen a directory as readonly (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_preopen_dir_readonly(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() || host_path.is_null() || guest_path.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi::wasmtime4j_wasi_context_preopen_dir_readonly(ctx_ptr, host_path, guest_path)
    }
}

/// Preopen a directory with specific permissions (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_context_preopen_dir_with_perms(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
    can_read: c_int,
    can_write: c_int,
    can_create: c_int,
) -> c_int {
    if ctx_ptr.is_null() || host_path.is_null() || guest_path.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi::wasmtime4j_wasi_context_preopen_dir_with_perms(
            ctx_ptr,
            host_path,
            guest_path,
            can_read,
            can_write,
            can_create,
        )
    }
}
