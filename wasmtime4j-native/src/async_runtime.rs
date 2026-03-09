//! # Async Runtime Management
//!
//! This module provides a global Tokio async runtime for wasmtime4j, enabling async WebAssembly
//! operations including async function calls, async module compilation, and callback notifications
//! for Java integration.
//!
//! ## Architecture
//!
//! - **Global Runtime**: Single multi-threaded Tokio runtime shared across all operations
//! - **JNI Bridge**: Completes Java CompletableFutures from Tokio threads via JNI GlobalRef
//! - **Panama Bridge**: Completes Java CompletableFutures via C function pointer callbacks
//! - **Defensive Programming**: Comprehensive error handling and resource cleanup
//!
//! ## CompletableFuture Bridge
//!
//! The bridge enables truly async operations where:
//! 1. Java creates a `CompletableFuture<T>` and passes it (or a callback) to native
//! 2. Native spawns a Tokio task calling Wasmtime's `*_async()` method
//! 3. On completion, the Tokio task completes the Java future via JNI or callback
//!
//! ### JNI Path
//! - `JNI_OnLoad` caches the `JavaVM` globally
//! - Tokio threads attach to JVM via `attach_current_thread_as_daemon()`
//! - `CompletableFuture.complete()` / `completeExceptionally()` called via JNI
//!
//! ### Panama Path
//! - Java passes a C-compatible completion callback (upcall stub)
//! - Tokio thread calls the callback directly; JVM auto-attaches for upcall stubs
//!
//! ## Safety Guarantees
//!
//! All async operations are designed to prevent JVM crashes through:
//! - Input validation before async task submission
//! - Timeout mechanisms to prevent infinite waiting
//! - Graceful error handling and resource cleanup
//! - Thread-safe callback dispatch to Java

use log::{debug, error, info, warn};
use std::sync::LazyLock;
use std::ffi::{c_char, c_void, CStr, CString};
use std::mem::ManuallyDrop;
use std::os::raw::{c_int, c_uint};
use std::sync::Arc;
use std::time::Duration;
use tokio::runtime::{Handle, Runtime};

use crate::error::WasmtimeError;
use crate::instance::{Instance, WasmValue};
use crate::store::Store;

// ================================================================================================
// Global Async Runtime
// ================================================================================================

/// Global Tokio runtime for async operations
///
/// Wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
/// This avoids SIGABRT caused by Tokio worker threads trying to interact with
/// the JVM during shutdown when the JVM is already tearing down.
static ASYNC_RUNTIME: LazyLock<ManuallyDrop<Arc<Runtime>>> = LazyLock::new(|| {
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
// JNI CompletableFuture Bridge
// ================================================================================================

/// Global JavaVM cache, set once at JNI_OnLoad or on first JNI call.
///
/// This is used by Tokio worker threads to attach to the JVM when completing
/// CompletableFutures from async operations.
#[cfg(feature = "jni-bindings")]
static CACHED_JVM: std::sync::OnceLock<Arc<jni::JavaVM>> = std::sync::OnceLock::new();

/// Cache the JavaVM for later use by async bridge operations.
///
/// Called from `JNI_OnLoad` or lazily from the first JNI call that needs async support.
/// Subsequent calls are no-ops (OnceLock ensures single initialization).
#[cfg(feature = "jni-bindings")]
pub fn cache_jvm(jvm: Arc<jni::JavaVM>) {
    let _ = CACHED_JVM.set(jvm);
}

/// Get the cached JavaVM reference.
///
/// Returns `None` if `cache_jvm` has not been called yet (library loaded via Panama
/// or JNI_OnLoad hasn't fired).
#[cfg(feature = "jni-bindings")]
pub fn get_cached_jvm() -> Option<&'static Arc<jni::JavaVM>> {
    CACHED_JVM.get()
}

/// JNI_OnLoad entry point — called by the JVM when the native library is loaded via
/// `System.loadLibrary()`.
///
/// Caches the JavaVM globally so that Tokio worker threads can later attach to the JVM
/// to complete CompletableFutures from async operations.
///
/// # Safety
///
/// This function is called by the JVM's native library loading mechanism. The `vm` pointer
/// is guaranteed valid by the JVM for the lifetime of the library.
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn JNI_OnLoad(
    vm: jni::JavaVM,
    _reserved: *mut std::ffi::c_void,
) -> jni::sys::jint {
    cache_jvm(Arc::new(vm));
    debug!("JNI_OnLoad: JavaVM cached for async CompletableFuture bridge");
    jni::sys::JNI_VERSION_1_8
}

/// Spawn an async task on the Tokio runtime that completes a Java CompletableFuture via JNI.
///
/// The `future_ref` is a JNI GlobalRef to a `java.util.concurrent.CompletableFuture` object.
/// When the task completes:
/// - On success: calls `CompletableFuture.complete(result)` where result is a boxed Java object
/// - On failure: calls `CompletableFuture.completeExceptionally(exception)`
///
/// The GlobalRef is dropped (releasing the JNI reference) after completion.
///
/// # Arguments
///
/// * `jvm` - The JavaVM to attach Tokio threads to
/// * `future_ref` - JNI GlobalRef to the CompletableFuture
/// * `task` - The async task producing either a serialized result (i64) or an error string
///
/// # Type Parameters
///
/// * `F` - Future type that produces `Result<i64, String>`. The i64 result is an opaque
///   value whose interpretation depends on the caller (e.g., a native pointer, a status code,
///   or a serialized value).
#[cfg(feature = "jni-bindings")]
pub fn spawn_jni_completable_future<F>(
    jvm: Arc<jni::JavaVM>,
    future_ref: jni::objects::GlobalRef,
    task: F,
) where
    F: std::future::Future<Output = Result<i64, String>> + Send + 'static,
{
    get_async_runtime().spawn(async move {
        let result = task.await;

        // Attach this Tokio thread to the JVM as a daemon thread.
        // Daemon threads don't prevent JVM shutdown and permanent attachment avoids
        // repeated attach/detach overhead for Tokio worker threads handling multiple tasks.
        let mut env = match jvm.attach_current_thread_as_daemon() {
            Ok(env) => env,
            Err(e) => {
                error!(
                    "Failed to attach Tokio thread to JVM for CompletableFuture completion: {}",
                    e
                );
                // Cannot complete the future — the GlobalRef will leak but this is
                // preferable to a JVM crash.
                return;
            }
        };

        match result {
            Ok(value) => {
                // Box the i64 result as a java.lang.Long for CompletableFuture.complete(Object)
                let boxed = match env.new_object("java/lang/Long", "(J)V", &[value.into()]) {
                    Ok(obj) => obj,
                    Err(e) => {
                        error!("Failed to box async result as Long: {}", e);
                        let _ = env.exception_clear();
                        return;
                    }
                };

                if let Err(e) = env.call_method(
                    &future_ref,
                    "complete",
                    "(Ljava/lang/Object;)Z",
                    &[(&boxed).into()],
                ) {
                    error!("Failed to call CompletableFuture.complete(): {}", e);
                    let _ = env.exception_clear();
                }
            }
            Err(error_message) => {
                // Create a RuntimeException and call completeExceptionally
                let msg = match env.new_string(&error_message) {
                    Ok(s) => s,
                    Err(e) => {
                        error!("Failed to create error message string: {}", e);
                        let _ = env.exception_clear();
                        return;
                    }
                };

                let exception = match env.new_object(
                    "java/lang/RuntimeException",
                    "(Ljava/lang/String;)V",
                    &[(&msg).into()],
                ) {
                    Ok(obj) => obj,
                    Err(e) => {
                        error!("Failed to create RuntimeException: {}", e);
                        let _ = env.exception_clear();
                        return;
                    }
                };

                if let Err(e) = env.call_method(
                    &future_ref,
                    "completeExceptionally",
                    "(Ljava/lang/Throwable;)Z",
                    &[(&exception).into()],
                ) {
                    error!(
                        "Failed to call CompletableFuture.completeExceptionally(): {}",
                        e
                    );
                    let _ = env.exception_clear();
                }
            }
        }

        // GlobalRef is dropped here, releasing the JNI reference.
        // The thread is still attached as daemon, so DeleteGlobalRef succeeds.
        drop(future_ref);
    });
}

// ================================================================================================
// Panama CompletableFuture Bridge (Callback-based)
// ================================================================================================

/// Completion callback type for Panama async operations.
///
/// Called from a Tokio worker thread when an async operation completes. The JVM
/// automatically attaches the calling thread for Panama upcall stubs (Java 22+).
///
/// # Arguments
///
/// * `callback_data` - Opaque identifier that Java uses to locate the CompletableFuture
///   (typically an ID into a ConcurrentHashMap)
/// * `success` - 1 if the operation succeeded, 0 if it failed
/// * `result` - On success: the result value (interpretation depends on operation type).
///   On failure: 0.
/// * `error_msg` - On failure: pointer to a null-terminated UTF-8 error message.
///   On success: null pointer. The string is only valid for the duration of the callback.
pub type PanamaAsyncCompletionCallback =
    extern "C" fn(callback_data: i64, success: c_int, result: i64, error_msg: *const c_char);

/// Spawn an async task on the Tokio runtime that completes via a Panama callback.
///
/// When the task completes, the callback is invoked from the Tokio worker thread.
/// The JVM handles thread attachment automatically for Panama upcall stubs.
///
/// # Arguments
///
/// * `callback` - C function pointer (Panama upcall stub) for completion notification
/// * `callback_data` - Opaque data passed back to the callback (e.g., future ID)
/// * `task` - The async task producing either an i64 result or an error string
///
/// # Safety
///
/// The `callback` function pointer must remain valid for the lifetime of the async task.
/// For Panama upcall stubs, this means the backing Arena must outlive the operation.
pub fn spawn_panama_completable_future<F>(
    callback: PanamaAsyncCompletionCallback,
    callback_data: i64,
    task: F,
) where
    F: std::future::Future<Output = Result<i64, String>> + Send + 'static,
{
    get_async_runtime().spawn(async move {
        let result = task.await;

        match result {
            Ok(value) => {
                callback(callback_data, 1, value, std::ptr::null());
            }
            Err(error_message) => {
                let c_msg = CString::new(error_message)
                    .unwrap_or_else(|_| CString::new("Unknown error").unwrap());
                callback(callback_data, 0, 0, c_msg.as_ptr());
                // c_msg lives until end of this block, so the pointer is valid during callback
            }
        }
    });
}

// ================================================================================================
// Legacy C API Functions (existing functionality preserved)
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
    timeout_ms: u64,
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
        timeout_ms
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
    fn test_panama_async_completion_callback() {
        use std::sync::atomic::{AtomicI64, Ordering};

        static CALLBACK_RESULT: AtomicI64 = AtomicI64::new(-1);
        static CALLBACK_SUCCESS: std::sync::atomic::AtomicI32 =
            std::sync::atomic::AtomicI32::new(-1);

        extern "C" fn test_callback(
            _callback_data: i64,
            success: c_int,
            result: i64,
            _error_msg: *const c_char,
        ) {
            CALLBACK_SUCCESS.store(success, Ordering::SeqCst);
            CALLBACK_RESULT.store(result, Ordering::SeqCst);
        }

        // Spawn a successful async task
        spawn_panama_completable_future(test_callback, 42, async { Ok(123) });

        // Wait for completion
        std::thread::sleep(Duration::from_millis(100));

        assert_eq!(CALLBACK_SUCCESS.load(Ordering::SeqCst), 1);
        assert_eq!(CALLBACK_RESULT.load(Ordering::SeqCst), 123);
    }

    #[test]
    fn test_panama_async_completion_error() {
        use std::sync::atomic::{AtomicI32, AtomicI64, Ordering};
        use std::sync::Mutex;

        static ERR_SUCCESS: AtomicI32 = AtomicI32::new(-1);
        static ERR_RESULT: AtomicI64 = AtomicI64::new(-1);
        static ERR_MSG: LazyLock<Mutex<String>> = LazyLock::new(|| Mutex::new(String::new()));

        extern "C" fn error_callback(
            _callback_data: i64,
            success: c_int,
            result: i64,
            error_msg: *const c_char,
        ) {
            ERR_SUCCESS.store(success, Ordering::SeqCst);
            ERR_RESULT.store(result, Ordering::SeqCst);
            if !error_msg.is_null() {
                let msg = unsafe { CStr::from_ptr(error_msg) }
                    .to_str()
                    .unwrap_or("?")
                    .to_string();
                *ERR_MSG.lock().unwrap() = msg;
            }
        }

        // Spawn a failing async task
        spawn_panama_completable_future(error_callback, 99, async {
            Err("test error".to_string())
        });

        // Wait for completion
        std::thread::sleep(Duration::from_millis(100));

        assert_eq!(ERR_SUCCESS.load(Ordering::SeqCst), 0);
        assert_eq!(ERR_RESULT.load(Ordering::SeqCst), 0);
        assert_eq!(*ERR_MSG.lock().unwrap(), "test error");
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
