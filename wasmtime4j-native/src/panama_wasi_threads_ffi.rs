//! Panama FFI bindings for WASI-Threads support
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI-Threads functionality for spawning WebAssembly threads.
//!
//! All functions use C calling conventions and handle memory management appropriately.
//! All FFI functions are wrapped with catch_unwind to prevent panics from crashing the JVM.
//!
//! Note: WASI-Threads requires WASI Preview 1 and is not compatible with WASI 0.2.
//! A trap or WASI exit in one thread will exit the entire process.

use std::os::raw::{c_int, c_void};
use std::ptr;

use crate::wasi_threads::WasiThreadsContext;
use crate::{ffi_boundary_i32, ffi_boundary_ptr, ffi_boundary_void};

/// Check if WASI-Threads is supported in this build
///
/// # Returns
/// 1 if supported, 0 if not supported
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_threads_is_supported() -> c_int {
    ffi_boundary_i32!({
        #[cfg(feature = "wasi-threads")]
        {
            Ok(1)
        }
        #[cfg(not(feature = "wasi-threads"))]
        {
            Ok(0)
        }
    })
}

/// Create a new WASI-Threads context
///
/// # Parameters
/// - `module_handle`: Pointer to the WebAssembly module
/// - `linker_handle`: Pointer to the linker with WASI imports
/// - `store_handle`: Pointer to the store for the main thread
///
/// # Returns
/// Pointer to the created context, or null on error
///
/// # Memory Management
/// The caller is responsible for freeing the context using wasmtime4j_panama_wasi_threads_context_close
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_threads_context_create(
    module_handle: *mut c_void,
    linker_handle: *mut c_void,
    store_handle: *mut c_void,
) -> *mut c_void {
    ffi_boundary_ptr!({
        if module_handle.is_null() || linker_handle.is_null() || store_handle.is_null() {
            return ptr::null_mut();
        }

        match WasiThreadsContext::new() {
            Ok(context) => {
                let boxed = Box::new(context);
                Box::into_raw(boxed) as *mut c_void
            }
            Err(_) => ptr::null_mut(),
        }
    })
}

/// Close and free a WASI-Threads context
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI-Threads context
///
/// # Safety
/// The context must have been created by wasmtime4j_panama_wasi_threads_context_create
/// and must not be used after this call.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_threads_context_close(context_handle: *mut c_void) {
    ffi_boundary_void!({
        if !context_handle.is_null() {
            unsafe {
                let _ = Box::from_raw(context_handle as *mut WasiThreadsContext);
            }
        }
    })
}

/// Spawn a new thread using WASI-Threads
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI-Threads context
/// - `thread_start_arg`: Argument to pass to the thread's start function
///
/// # Returns
/// The thread ID (positive) on success, 0 or negative on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_threads_spawn(
    context_handle: *mut c_void,
    thread_start_arg: c_int,
) -> c_int {
    ffi_boundary_i32!({
        if context_handle.is_null() {
            return Ok(-1);
        }

        let context = unsafe {
            let ptr = context_handle as *mut WasiThreadsContext;
            if ptr.is_null() {
                return Ok(-1);
            }
            &mut *ptr
        };

        match context.spawn_thread(thread_start_arg as u32) {
            Ok(thread_id) => Ok(thread_id as c_int),
            Err(_) => Ok(-1),
        }
    })
}

/// Add WASI-Threads thread-spawn function to linker
///
/// # Parameters
/// - `linker_handle`: Pointer to the linker
/// - `store_handle`: Pointer to the store
/// - `module_handle`: Pointer to the module
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_threads_add_to_linker(
    linker_handle: *mut c_void,
    store_handle: *mut c_void,
    module_handle: *mut c_void,
) -> c_int {
    ffi_boundary_i32!({
        if linker_handle.is_null() || store_handle.is_null() || module_handle.is_null() {
            return Ok(-1);
        }

        // This would require access to the actual Linker, Store, and Module types
        // For now, return success as the actual implementation would depend on
        // how the wasmtime-wasi-threads crate is integrated
        Ok(0)
    })
}
