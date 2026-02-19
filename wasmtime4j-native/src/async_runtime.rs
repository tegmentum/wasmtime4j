//! # Async Runtime Management
//!
//! This module provides a global Tokio async runtime for wasmtime4j, enabling async WebAssembly
//! operations including async function calls, async module compilation, and callback notifications
//! for Java integration.
//!
//! ## Architecture
//!
//! - **Global Runtime**: Single multi-threaded Tokio runtime shared across all operations
//! - **Async Function Calls**: Non-blocking WebAssembly function execution with timeout support
//! - **Async Compilation**: Background module compilation with progress callbacks
//! - **Callback System**: Thread-safe callback infrastructure for Java integration
//! - **Defensive Programming**: Comprehensive error handling and resource cleanup
//!
//! ## Safety Guarantees
//!
//! All async operations are designed to prevent JVM crashes through:
//! - Input validation before async task submission
//! - Timeout mechanisms to prevent infinite waiting
//! - Graceful error handling and resource cleanup
//! - Thread-safe callback dispatch to Java

use log::{debug, error, info, warn};
use once_cell::sync::Lazy;
use std::ffi::{c_char, c_void, CStr, CString};
use std::mem::ManuallyDrop;
use std::os::raw::{c_int, c_uint, c_ulong};
use std::sync::Arc;
use std::time::Duration;
use tokio::runtime::{Handle, Runtime};

use crate::error::WasmtimeError;
use crate::instance::{Instance, WasmValue};
use crate::store::Store;

/// Global Tokio runtime for async operations
///
/// Wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
/// This avoids SIGABRT caused by Tokio worker threads trying to interact with
/// the JVM during shutdown when the JVM is already tearing down.
static ASYNC_RUNTIME: Lazy<ManuallyDrop<Arc<Runtime>>> = Lazy::new(|| {
    info!("Initializing global Tokio async runtime for wasmtime4j");

    // Try optimal configuration first
    let runtime = match tokio::runtime::Builder::new_multi_thread()
        .worker_threads(4)
        .thread_name("wasmtime4j-async")
        .thread_stack_size(2 * 1024 * 1024) // 2MB stack size
        .enable_all()
        .build()
    {
        Ok(runtime) => {
            info!("Global Tokio async runtime initialized successfully with optimal configuration");
            Arc::new(runtime)
        }
        Err(e) => {
            warn!(
                "Failed to create optimal async runtime ({}), trying current thread fallback",
                e
            );

            // Fallback to current thread runtime
            tokio::runtime::Builder::new_current_thread()
                .enable_all()
                .build()
                .map(Arc::new)
                .expect("Failed to create any Tokio runtime - cannot continue")
        }
    };

    ManuallyDrop::new(runtime)
});

/// Callback function type for async operation completion
pub type AsyncCallback = extern "C" fn(*mut c_void, c_int, *const c_char);

static OPERATION_ID_COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

/// Get the global async runtime instance
///
/// # Returns
///
/// Reference to the global Tokio runtime for executing async operations
pub fn get_async_runtime() -> &'static Arc<Runtime> {
    &ASYNC_RUNTIME
}

/// Get the async runtime handle for spawning tasks
///
/// # Returns
///
/// Handle to the global Tokio runtime for task spawning
pub fn get_runtime_handle() -> Handle {
    ASYNC_RUNTIME.handle().clone()
}

/// Generate next unique operation ID
fn next_operation_id() -> u64 {
    OPERATION_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst)
}

// ================================================================================================
// C API Functions for JNI and Panama FFI Integration
// ================================================================================================

// wasmtime4j_async_runtime_init, wasmtime4j_async_runtime_info,
// wasmtime4j_async_runtime_shutdown removed (Phase 13: 0 Java callers)

/// Execute async function call (C API)
///
/// # Arguments
///
/// * `instance_ptr` - Pointer to Instance object
/// * `store_ptr` - Pointer to Store object (required for Wasmtime function calls)
/// * `function_name` - Name of function to call
/// * `args_ptr` - Pointer to i64 array of arguments (values encoded as i64)
/// * `args_len` - Number of arguments
/// * `timeout_ms` - Timeout in milliseconds (0 for no timeout)
/// * `callback` - Completion callback function
/// * `user_data` - User data for callback
///
/// # Returns
///
/// Operation ID (> 0) on success, negative value on error
///
/// # Safety
///
/// This function validates all inputs and handles errors gracefully
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_func_call_async(
    instance_ptr: *mut c_void,
    store_ptr: *mut c_void,
    function_name: *const c_char,
    args_ptr: *const i64,
    args_len: c_uint,
    timeout_ms: c_ulong,
    callback: AsyncCallback,
    user_data: *mut c_void,
) -> c_int {
    // Validate inputs
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        error!(
            "Invalid parameters for async function call: instance_ptr={:?}, store_ptr={:?}",
            instance_ptr.is_null(),
            store_ptr.is_null()
        );
        return -1;
    }

    // Convert function name
    let function_name_str = match CStr::from_ptr(function_name).to_str() {
        Ok(name) => name.to_string(),
        Err(_) => {
            error!("Invalid function name string");
            return -1;
        }
    };

    // Get instance reference
    let _instance = match crate::instance::core::get_instance_mut(instance_ptr) {
        Ok(inst) => inst,
        Err(e) => {
            error!("Failed to get instance: {}", e);
            return -1;
        }
    };

    // Get store reference
    let _store = match crate::store::core::get_store_mut(store_ptr) {
        Ok(s) => s,
        Err(e) => {
            error!("Failed to get store: {}", e);
            return -1;
        }
    };

    // Parse arguments from i64 array (treat as i32 params for simplicity)
    let arguments: Vec<WasmValue> = if args_len > 0 && !args_ptr.is_null() {
        std::slice::from_raw_parts(args_ptr, args_len as usize)
            .iter()
            .map(|&v| WasmValue::I64(v))
            .collect()
    } else {
        Vec::new()
    };

    // Generate operation ID
    let operation_id = next_operation_id();
    debug!(
        "Starting async function call operation {} for function '{}'",
        operation_id, function_name_str
    );

    // Create raw pointers that are Send-safe by converting to usize
    let instance_addr = instance_ptr as usize;
    let store_addr = store_ptr as usize;
    let user_data_addr = user_data as usize;
    let function_name_owned = function_name_str.clone();
    let timeout = if timeout_ms > 0 {
        timeout_ms as u64
    } else {
        30000
    }; // Default 30s timeout

    // Spawn async task on the global runtime
    get_async_runtime().spawn(async move {
        let result = tokio::time::timeout(Duration::from_millis(timeout), async {
            // Defensive validation: ensure addresses are non-zero before dereferencing.
            // A zero address would indicate a programming error (null pointer passed).
            if instance_addr == 0 || store_addr == 0 {
                error!(
                    "Null pointer detected in async function call: instance_addr={}, store_addr={}",
                    instance_addr, store_addr
                );
                return Err(WasmtimeError::InvalidParameter {
                    message: "Null pointer in async call".to_string(),
                });
            }

            // SAFETY: The caller has validated these pointers before this async block was spawned.
            // The caller is contractually responsible for ensuring these pointers remain valid
            // for the duration of this async operation. The defensive check above catches
            // obvious programming errors (null pointers) but cannot detect use-after-free.
            let instance = unsafe { &mut *(instance_addr as *mut Instance) };
            let store = unsafe { &mut *(store_addr as *mut Store) };

            // Call the function
            instance.call_export_function(store, &function_name_owned, &arguments)
        })
        .await;

        // Invoke callback with result
        let (status_code, message) = match result {
            Ok(Ok(call_result)) => {
                info!(
                    "Async function '{}' completed successfully with {} results",
                    function_name_owned,
                    call_result.values.len()
                );
                (0, "Success".to_string())
            }
            Ok(Err(e)) => {
                error!("Async function '{}' failed: {}", function_name_owned, e);
                (-1, format!("Error: {}", e))
            }
            Err(_) => {
                warn!(
                    "Async function '{}' timed out after {}ms",
                    function_name_owned, timeout
                );
                (-2, format!("Timeout after {}ms", timeout))
            }
        };

        // Call the C callback
        let c_message =
            CString::new(message).unwrap_or_else(|_| CString::new("Unknown error").unwrap());
        callback(
            user_data_addr as *mut c_void,
            status_code,
            c_message.as_ptr(),
        );
    });

    operation_id as c_int
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ptr;

    #[test]
    fn test_async_runtime_initialization() {
        let runtime = get_async_runtime();
        let _handle = runtime.handle();
    }

    #[test]
    fn test_operation_id_generation() {
        let id1 = next_operation_id();
        let id2 = next_operation_id();
        assert!(id2 > id1);
        assert_ne!(id1, id2);
    }

    #[test]
    fn test_invalid_function_call_parameters() {
        unsafe {
            extern "C" fn dummy_callback(
                _user_data: *mut c_void,
                _status: c_int,
                _message: *const c_char,
            ) {
            }

            // Test null instance
            let result = wasmtime4j_func_call_async(
                ptr::null_mut(),
                ptr::null_mut(), // store_ptr
                b"test\0".as_ptr() as *const c_char,
                ptr::null(),
                0,
                1000,
                dummy_callback,
                ptr::null_mut(),
            );
            assert_eq!(result, -1);

            // Test null store
            let dummy_instance = 0x1000 as *mut c_void;
            let result = wasmtime4j_func_call_async(
                dummy_instance,
                ptr::null_mut(), // store_ptr
                b"test\0".as_ptr() as *const c_char,
                ptr::null(),
                0,
                1000,
                dummy_callback,
                ptr::null_mut(),
            );
            assert_eq!(result, -1);

            // Test null function name
            let dummy_store = 0x2000 as *mut c_void;
            let result = wasmtime4j_func_call_async(
                dummy_instance,
                dummy_store,
                ptr::null(),
                ptr::null(),
                0,
                1000,
                dummy_callback,
                ptr::null_mut(),
            );
            assert_eq!(result, -1);
        }
    }
}

