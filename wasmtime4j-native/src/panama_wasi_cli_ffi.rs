//! Panama FFI bindings for WASI CLI operations
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI CLI operations (wasi:cli).
//!
//! All functions use C calling conventions and handle memory management appropriately.
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.

use std::ffi::CString;
use std::os::raw::{c_char, c_int, c_void};
use std::ptr;

use crate::ffi_boundary_i32;
use crate::wasi::WasiContext;

/// Get all environment variables
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_env_vars`: Pointer to buffer for environment variable pairs (null-terminated strings)
/// - `out_env_vars_len`: Pointer to write total length of environment variables buffer
///
/// # Returns
/// 0 on success, -1 on error
///
/// # Memory Management
/// The caller is responsible for freeing the returned string using wasmtime4j_panama_free_string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_environment_get_all(
    context_handle: *mut c_void,
    out_env_vars: *mut *mut c_char,
    out_env_vars_len: *mut c_int,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_env_vars.is_null() || out_env_vars_len.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let env_map = match context.environment_rw.read() {
            Ok(env_map) => env_map,
            Err(_) => return Ok(-1),
        };

        let env_strings: Vec<String> = env_map
            .iter()
            .map(|(k, v)| format!("{}={}", k, v))
            .collect();

        let json_array = format!(
            "[{}]",
            env_strings
                .iter()
                .map(|s| format!("\"{}\"", s.replace("\"", "\\\"")))
                .collect::<Vec<_>>()
                .join(",")
        );

        match CString::new(json_array) {
            Ok(c_str) => {
                let len = c_str.as_bytes().len() as c_int;
                unsafe {
                    *out_env_vars = c_str.into_raw();
                    *out_env_vars_len = len;
                }
                Ok(0)
            }
            Err(_) => Ok(-1),
        }
    })
}

/// Get a specific environment variable
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `name`: Environment variable name (UTF-8)
/// - `name_len`: Length of name string
/// - `out_value`: Pointer to buffer for variable value
/// - `out_value_len`: Pointer to write value length
///
/// # Returns
/// 0 on success, -1 on error, 1 if not found
///
/// # Memory Management
/// The caller is responsible for freeing the returned string using wasmtime4j_panama_free_string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_environment_get(
    context_handle: *mut c_void,
    name: *const c_char,
    name_len: c_int,
    out_value: *mut *mut c_char,
    out_value_len: *mut c_int,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null()
            || name.is_null()
            || out_value.is_null()
            || out_value_len.is_null()
        {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let name_str = unsafe {
            let slice = std::slice::from_raw_parts(name as *const u8, name_len as usize);
            match std::str::from_utf8(slice) {
                Ok(s) => s,
                Err(_) => return Ok(-1),
            }
        };

        let env_map = match context.environment_rw.read() {
            Ok(env_map) => env_map,
            Err(_) => return Ok(-1),
        };

        match env_map.get(name_str) {
            Some(value) => match CString::new(value.as_str()) {
                Ok(c_str) => {
                    let len = c_str.as_bytes().len() as c_int;
                    unsafe {
                        *out_value = c_str.into_raw();
                        *out_value_len = len;
                    }
                    Ok(0)
                }
                Err(_) => Ok(-1),
            },
            None => {
                unsafe {
                    *out_value = ptr::null_mut();
                    *out_value_len = 0;
                }
                Ok(1)
            }
        }
    })
}

/// Get command-line arguments
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_args`: Pointer to buffer for arguments (null-terminated strings)
/// - `out_args_len`: Pointer to write total length of arguments buffer
///
/// # Returns
/// 0 on success, -1 on error
///
/// # Memory Management
/// The caller is responsible for freeing the returned string using wasmtime4j_panama_free_string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_environment_get_arguments(
    context_handle: *mut c_void,
    out_args: *mut *mut c_char,
    out_args_len: *mut c_int,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_args.is_null() || out_args_len.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let args = match context.arguments_rw.read() {
            Ok(args) => args,
            Err(_) => return Ok(-1),
        };

        let json_array = format!(
            "[{}]",
            args.iter()
                .map(|s| format!("\"{}\"", s.replace("\"", "\\\"")))
                .collect::<Vec<_>>()
                .join(",")
        );

        match CString::new(json_array) {
            Ok(c_str) => {
                let len = c_str.as_bytes().len() as c_int;
                unsafe {
                    *out_args = c_str.into_raw();
                    *out_args_len = len;
                }
                Ok(0)
            }
            Err(_) => Ok(-1),
        }
    })
}

/// Get initial working directory
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_cwd`: Pointer to buffer for working directory path
/// - `out_cwd_len`: Pointer to write path length
///
/// # Returns
/// 0 on success, -1 on error, 1 if not set
///
/// # Memory Management
/// The caller is responsible for freeing the returned string using wasmtime4j_panama_free_string
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_environment_get_initial_cwd(
    context_handle: *mut c_void,
    out_cwd: *mut *mut c_char,
    out_cwd_len: *mut c_int,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_cwd.is_null() || out_cwd_len.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let cwd = match context.initial_cwd.read() {
            Ok(cwd) => cwd,
            Err(_) => return Ok(-1),
        };

        match cwd.as_ref() {
            Some(path) => match CString::new(path.as_str()) {
                Ok(c_str) => {
                    let len = c_str.as_bytes().len() as c_int;
                    unsafe {
                        *out_cwd = c_str.into_raw();
                        *out_cwd_len = len;
                    }
                    Ok(0)
                }
                Err(_) => Ok(-1),
            },
            None => {
                unsafe {
                    *out_cwd = ptr::null_mut();
                    *out_cwd_len = 0;
                }
                Ok(1)
            }
        }
    })
}

/// Get stdin stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_stream_handle`: Pointer to write the stdin stream handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_stdio_get_stdin(
    context_handle: *mut c_void,
    out_stream_handle: *mut *mut c_void,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_stream_handle.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let stdin_handle = match context.stdin_handle.read() {
            Ok(handle_opt) => match *handle_opt {
                Some(handle) => handle as usize,
                None => 1usize,
            },
            Err(_) => return Ok(-1),
        };

        unsafe {
            *out_stream_handle = stdin_handle as *mut c_void;
        }
        Ok(0)
    })
}

/// Get stdout stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_stream_handle`: Pointer to write the stdout stream handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_stdio_get_stdout(
    context_handle: *mut c_void,
    out_stream_handle: *mut *mut c_void,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_stream_handle.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let stdout_handle = match context.stdout_handle.read() {
            Ok(handle_opt) => match *handle_opt {
                Some(handle) => handle as usize,
                None => 2usize,
            },
            Err(_) => return Ok(-1),
        };

        unsafe {
            *out_stream_handle = stdout_handle as *mut c_void;
        }
        Ok(0)
    })
}

/// Get stderr stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `out_stream_handle`: Pointer to write the stderr stream handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_stdio_get_stderr(
    context_handle: *mut c_void,
    out_stream_handle: *mut *mut c_void,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() || out_stream_handle.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        let stderr_handle = match context.stderr_handle.read() {
            Ok(handle_opt) => match *handle_opt {
                Some(handle) => handle as usize,
                None => 3usize,
            },
            Err(_) => return Ok(-1),
        };

        unsafe {
            *out_stream_handle = stderr_handle as *mut c_void;
        }
        Ok(0)
    })
}

/// Exit the program with a status code
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `status_code`: Exit status code (0 for success, non-zero for failure)
///
/// # Returns
/// The status code on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_exit(
    context_handle: *mut c_void,
    status_code: c_int,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *const WasiContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &*ptr
        };

        match context.exit_code.write() {
            Ok(mut exit_code_opt) => {
                *exit_code_opt = Some(status_code);
                Ok(status_code)
            }
            Err(_) => Ok(-1),
        }
    })
}
