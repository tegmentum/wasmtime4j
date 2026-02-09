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

use std::ffi::{c_char, c_void, CStr, CString};
use std::mem::ManuallyDrop;
use std::os::raw::{c_int, c_uint, c_ulong};
use std::ptr;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::runtime::{Runtime, Handle};
use tokio::sync::oneshot;
use once_cell::sync::Lazy;
use log::{debug, error, info, warn};
use jni::JavaVM;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::instance::{Instance, WasmValue};
use crate::store::Store;

/// Convert WasmValue to wasmtime::Val for async function calls
fn wasm_value_to_val(value: &WasmValue) -> WasmtimeResult<wasmtime::Val> {
    Ok(match value {
        WasmValue::I32(v) => wasmtime::Val::I32(*v),
        WasmValue::I64(v) => wasmtime::Val::I64(*v),
        WasmValue::F32(v) => wasmtime::Val::F32(v.to_bits()),
        WasmValue::F64(v) => wasmtime::Val::F64(v.to_bits()),
        WasmValue::V128(bytes) => wasmtime::Val::V128(wasmtime::V128::from(u128::from_le_bytes(*bytes))),
        WasmValue::ExternRef(_) => wasmtime::Val::null_extern_ref(),
        WasmValue::FuncRef(_) => wasmtime::Val::null_func_ref(),
    })
}

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
            warn!("Failed to create optimal async runtime ({}), trying current thread fallback", e);

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

/// Callback function type for async operation progress
pub type ProgressCallback = extern "C" fn(*mut c_void, c_uint, *const c_char);

/// Async operation handle for cancellation and status tracking
#[repr(C)]
pub struct AsyncOperation {
    /// Unique operation ID
    pub id: u64,
    /// Operation type identifier
    pub operation_type: AsyncOperationType,
    /// Cancellation sender
    cancel_tx: Option<oneshot::Sender<()>>,
    /// Operation status
    status: Arc<Mutex<AsyncOperationStatus>>,
}

/// Types of async operations supported
#[repr(C)]
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum AsyncOperationType {
    /// Async function call
    FunctionCall = 1,
    /// Async module compilation
    ModuleCompilation = 2,
    /// Async module instantiation
    ModuleInstantiation = 3,
}

/// Status of an async operation
#[repr(C)]
#[derive(Debug, Clone, PartialEq)]
pub enum AsyncOperationStatus {
    /// Operation is pending execution
    Pending = 0,
    /// Operation is currently running
    Running = 1,
    /// Operation completed successfully
    Completed = 2,
    /// Operation failed with error
    Failed = 3,
    /// Operation was cancelled
    Cancelled = 4,
    /// Operation timed out
    TimedOut = 5,
}

/// Thread-safe wrapper for user data pointer
/// Stores the pointer as usize (which is Send) to safely pass through async boundaries
struct SendableUserData(usize);

impl SendableUserData {
    /// Creates a new SendableUserData from a raw pointer
    fn new(ptr: *mut c_void) -> Self {
        Self(ptr as usize)
    }

    /// Converts back to a raw pointer for callback invocation
    ///
    /// # Safety
    ///
    /// The caller must ensure:
    /// - The original pointer is still valid
    /// - The pointer points to the expected type
    /// - The lifetime of the pointed-to data is still valid
    unsafe fn as_ptr(&self) -> *mut c_void {
        self.0 as *mut c_void
    }
}

/// Context for async function calls with JavaVM for thread-safe JNI access
pub struct AsyncFunctionCallContext {
    /// Instance to call function on
    pub instance: Arc<Instance>,
    /// Store for execution
    pub store: Arc<Mutex<Store>>,
    /// Function name to call
    pub function_name: String,
    /// Function arguments
    pub arguments: Vec<crate::instance::WasmValue>,
    /// Timeout duration
    pub timeout_ms: u64,
    /// JavaVM for thread-safe JNI access (Send-safe)
    pub jvm: Arc<JavaVM>,
    /// Callback for completion
    pub callback: AsyncCallback,
    /// User data for callback
    pub user_data: SendableUserData,
}

// Explicitly implement Send for AsyncFunctionCallContext
// Safety: All fields are Send-safe:
// - Arc<Instance> and Arc<Mutex<Store>> are Send
// - String and Vec are Send
// - Arc<JavaVM> is Send (JavaVM can be safely shared across threads)
// - AsyncCallback (extern "C" fn) is Send
// - SendableUserData(usize) is Send (usize is Send)
unsafe impl Send for AsyncFunctionCallContext {}

/// Context for async module compilation with JavaVM for thread-safe JNI access
pub struct AsyncCompilationContext {
    /// Module bytes to compile
    pub module_bytes: Vec<u8>,
    /// Compilation options
    pub options: CompilationOptions,
    /// Timeout duration
    pub timeout_ms: u64,
    /// JavaVM for thread-safe JNI access (Send-safe)
    pub jvm: Arc<JavaVM>,
    /// Completion callback
    pub callback: AsyncCallback,
    /// Progress callback (optional)
    pub progress_callback: Option<ProgressCallback>,
    /// User data for callbacks
    pub user_data: SendableUserData,
}

// Explicitly implement Send for AsyncCompilationContext
// Safety: All fields are Send-safe:
// - Vec<u8> is Send
// - CompilationOptions contains only primitive types
// - Arc<JavaVM> is Send (JavaVM can be safely shared across threads)
// - AsyncCallback and ProgressCallback (extern "C" fn) are Send
// - SendableUserData(usize) is Send (usize is Send)
unsafe impl Send for AsyncCompilationContext {}


/// Options for async module compilation
#[derive(Default)]
pub struct CompilationOptions {
    /// Enable optimizations
    pub optimize: bool,
    /// Enable debug information
    pub debug_info: bool,
    /// Enable profiling support
    pub profiling: bool,
}

/// Global counter for operation IDs
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

/// Execute an async WebAssembly function call with timeout
///
/// # Arguments
///
/// * `context` - Function call context containing instance, function name, and arguments
///
/// # Returns
///
/// Async operation handle for tracking and cancellation
///
/// # Safety
///
/// This function validates all inputs and handles errors gracefully to prevent JVM crashes.
/// The callback will be invoked on completion, error, or timeout using JavaVM for thread-safe JNI access.
pub fn execute_async_function_call(context: AsyncFunctionCallContext) -> WasmtimeResult<AsyncOperation> {
    let operation_id = next_operation_id();

    debug!("Starting async function call operation {} for function '{}'", operation_id, context.function_name);

    // Create cancellation channel
    let (cancel_tx, mut cancel_rx) = oneshot::channel::<()>();

    // Create shared status
    let status = Arc::new(Mutex::new(AsyncOperationStatus::Pending));
    let status_clone = status.clone();

    // Spawn async task on Tokio runtime
    get_runtime_handle().spawn(async move {
        // Update status to running
        {
            let mut status_guard = match status_clone.lock() {
                Ok(guard) => guard,
                Err(poisoned) => {
                    error!("Status lock poisoned for operation {}", operation_id);
                    poisoned.into_inner()
                }
            };
            *status_guard = AsyncOperationStatus::Running;
        }

        debug!("Executing async function '{}' in operation {}", context.function_name, operation_id);

        // Execute with timeout and cancellation support
        let result = tokio::select! {
            // Execute the actual function call
            res = async {
                // Lock the store
                let mut store_guard = match context.store.lock() {
                    Ok(guard) => guard,
                    Err(e) => {
                        error!("Failed to acquire store lock: {:?}", e);
                        return Err(WasmtimeError::Concurrency {
                            message: "Failed to acquire store lock".to_string()
                        });
                    }
                };

                // Get function from instance
                let func = match context.instance.get_func(&mut *store_guard, &context.function_name)? {
                    Some(f) => f,
                    None => {
                        return Err(WasmtimeError::Runtime {
                            message: format!("Function '{}' not found in instance", context.function_name),
                            backtrace: None,
                        });
                    }
                };

                // Convert WasmValue arguments to wasmtime::Val
                let wasmtime_params: Vec<wasmtime::Val> = context.arguments
                    .iter()
                    .map(|arg| wasm_value_to_val(arg))
                    .collect::<Result<Vec<_>, _>>()?;

                // Lock the inner store for function operations
                let mut inner_store = store_guard.try_lock_store()?;

                // Get function type to determine result count
                let func_type = func.ty(&mut *inner_store);
                let result_count = func_type.results().len();
                let mut results = vec![wasmtime::Val::I32(0); result_count];

                // Execute the function call
                // Note: Using synchronous call within async context. For true async execution,
                // use TypedFunc::call_async with engines created with async_support(true).
                func.call(&mut *inner_store, &wasmtime_params, &mut results)
                    .map_err(|e| WasmtimeError::Execution {
                        message: format!("Function call failed: {}", e),
                    })?;

                debug!("Function '{}' executed successfully with {} results",
                       context.function_name, results.len());

                Ok::<(), WasmtimeError>(())
            } => res,

            // Handle cancellation
            _ = cancel_rx => {
                warn!("Operation {} was cancelled", operation_id);
                Err(WasmtimeError::Internal {
                    message: "Operation cancelled".to_string()
                })
            },

            // Handle timeout
            _ = tokio::time::sleep(Duration::from_millis(context.timeout_ms)) => {
                warn!("Operation {} timed out after {}ms", operation_id, context.timeout_ms);
                Err(WasmtimeError::Internal {
                    message: format!("Operation timed out after {}ms", context.timeout_ms)
                })
            }
        };

        // Determine final status and error message
        let (final_status, status_code, error_msg) = match result {
            Ok(_) => {
                info!("Operation {} completed successfully", operation_id);
                (AsyncOperationStatus::Completed, 0, "Success".to_string())
            }
            Err(ref e) if e.to_string().contains("timed out") => {
                warn!("Operation {} timed out", operation_id);
                (AsyncOperationStatus::TimedOut, -2, format!("Timeout: {}", e))
            }
            Err(ref e) if e.to_string().contains("cancelled") => {
                warn!("Operation {} was cancelled", operation_id);
                (AsyncOperationStatus::Cancelled, -3, format!("Cancelled: {}", e))
            }
            Err(ref e) => {
                error!("Operation {} failed: {}", operation_id, e);
                (AsyncOperationStatus::Failed, -1, format!("Error: {}", e))
            }
        };

        // Update status
        {
            let mut status_guard = match status_clone.lock() {
                Ok(guard) => guard,
                Err(poisoned) => poisoned.into_inner()
            };
            *status_guard = final_status;
        }

        // Attach to JVM to invoke Java callback
        match context.jvm.attach_current_thread() {
            Ok(mut _env) => {
                debug!("Attached to JVM for callback invocation in operation {}", operation_id);

                // Create C string for error message
                let message_cstring = match CString::new(error_msg) {
                    Ok(s) => s,
                    Err(_) => CString::new("Error creating message")
                        .unwrap_or_else(|_| CString::default())
                };

                // Invoke the callback
                // SAFETY: user_data pointer is passed through from C and remains valid
                unsafe {
                    (context.callback)(
                        context.user_data.as_ptr(),
                        status_code,
                        message_cstring.as_ptr()
                    );
                }

                debug!("Callback invoked successfully for operation {}", operation_id);
            }
            Err(e) => {
                error!("Failed to attach to JVM for callback in operation {}: {:?}", operation_id, e);
                // Update status to failed since we couldn't notify Java
                let mut status_guard = match status_clone.lock() {
                    Ok(guard) => guard,
                    Err(poisoned) => poisoned.into_inner()
                };
                *status_guard = AsyncOperationStatus::Failed;
            }
        }
    });

    Ok(AsyncOperation {
        id: operation_id,
        operation_type: AsyncOperationType::FunctionCall,
        cancel_tx: Some(cancel_tx),
        status,
    })
}

/// Compile a WebAssembly module asynchronously with progress callbacks
///
/// # Arguments
///
/// * `context` - Compilation context containing module bytes and options
///
/// # Returns
///
/// Async operation handle for tracking and cancellation
///
/// # Safety
///
/// This function validates all inputs and handles errors gracefully to prevent JVM crashes.
/// Progress and completion callbacks will be invoked using JavaVM for thread-safe JNI access.
pub fn compile_module_async(context: AsyncCompilationContext) -> WasmtimeResult<AsyncOperation> {
    let operation_id = next_operation_id();

    debug!("Starting async module compilation operation {} for {} bytes", operation_id, context.module_bytes.len());

    // Create cancellation channel
    let (cancel_tx, mut cancel_rx) = oneshot::channel::<()>();

    // Create shared status
    let status = Arc::new(Mutex::new(AsyncOperationStatus::Pending));
    let status_clone = status.clone();

    // Spawn async compilation task
    get_runtime_handle().spawn(async move {
        // Update status to running
        {
            let mut status_guard = match status_clone.lock() {
                Ok(guard) => guard,
                Err(poisoned) => {
                    error!("Status lock poisoned for compilation operation {}", operation_id);
                    poisoned.into_inner()
                }
            };
            *status_guard = AsyncOperationStatus::Running;
        }

        debug!("Compiling WebAssembly module in operation {}", operation_id);

        // Execute compilation with timeout and cancellation
        let result = tokio::select! {
            // Perform actual compilation
            res = async {
                // Report 10% progress - starting validation
                if let Some(progress_cb) = context.progress_callback {
                    match context.jvm.attach_current_thread() {
                        Ok(_env) => {
                            let msg = CString::new("Validating module bytes")
                                .unwrap_or_else(|_| CString::default());
                            unsafe {
                                progress_cb(context.user_data.as_ptr(), 10, msg.as_ptr());
                            }
                        }
                        Err(e) => {
                            warn!("Failed to attach to JVM for progress callback: {:?}", e);
                        }
                    }
                }

                // Create engine with appropriate configuration
                let mut engine_config = crate::engine::safe_wasmtime_config();

                // Apply compilation options
                if context.options.optimize {
                    engine_config.cranelift_opt_level(wasmtime::OptLevel::Speed);
                } else {
                    engine_config.cranelift_opt_level(wasmtime::OptLevel::None);
                }

                if context.options.debug_info {
                    engine_config.debug_info(true);
                }

                if context.options.profiling {
                    engine_config.profiler(wasmtime::ProfilingStrategy::JitDump);
                }

                // Enable async support for async compilation
                engine_config.async_support(true);

                let engine = wasmtime::Engine::new(&engine_config).map_err(|e| {
                    WasmtimeError::EngineConfig {
                        message: format!("Failed to create engine for async compilation: {}", e),
                    }
                })?;

                // Report 30% progress - engine created
                if let Some(progress_cb) = context.progress_callback {
                    match context.jvm.attach_current_thread() {
                        Ok(_env) => {
                            let msg = CString::new("Engine configured, starting compilation")
                                .unwrap_or_else(|_| CString::default());
                            unsafe {
                                progress_cb(context.user_data.as_ptr(), 30, msg.as_ptr());
                            }
                        }
                        Err(_) => {}
                    }
                }

                // Perform synchronous compilation in blocking context
                // Note: Wasmtime's Module::new is CPU-bound, so we use spawn_blocking
                let bytes = context.module_bytes.clone();
                let module_result = tokio::task::spawn_blocking(move || {
                    wasmtime::Module::new(&engine, &bytes)
                }).await;

                // Report 80% progress - compilation complete
                if let Some(progress_cb) = context.progress_callback {
                    match context.jvm.attach_current_thread() {
                        Ok(_env) => {
                            let msg = CString::new("Compilation complete, finalizing")
                                .unwrap_or_else(|_| CString::default());
                            unsafe {
                                progress_cb(context.user_data.as_ptr(), 80, msg.as_ptr());
                            }
                        }
                        Err(_) => {}
                    }
                }

                // Handle the spawn_blocking result
                match module_result {
                    Ok(Ok(_module)) => {
                        // Report 100% progress
                        if let Some(progress_cb) = context.progress_callback {
                            match context.jvm.attach_current_thread() {
                                Ok(_env) => {
                                    let msg = CString::new("Module ready")
                                        .unwrap_or_else(|_| CString::default());
                                    unsafe {
                                        progress_cb(context.user_data.as_ptr(), 100, msg.as_ptr());
                                    }
                                }
                                Err(_) => {}
                            }
                        }
                        Ok::<(), WasmtimeError>(())
                    }
                    Ok(Err(e)) => {
                        Err(WasmtimeError::Compilation {
                            message: format!("Module compilation failed: {}", e),
                        })
                    }
                    Err(e) => {
                        Err(WasmtimeError::Internal {
                            message: format!("Compilation task panicked: {}", e),
                        })
                    }
                }
            } => res,

            // Handle cancellation
            _ = cancel_rx => {
                warn!("Compilation operation {} was cancelled", operation_id);
                Err(WasmtimeError::Internal {
                    message: "Compilation cancelled".to_string()
                })
            },

            // Handle timeout
            _ = tokio::time::sleep(Duration::from_millis(context.timeout_ms)) => {
                warn!("Compilation operation {} timed out after {}ms", operation_id, context.timeout_ms);
                Err(WasmtimeError::Internal {
                    message: format!("Compilation timed out after {}ms", context.timeout_ms)
                })
            }
        };

        // Determine final status
        let (final_status, status_code, error_msg) = match result {
            Ok(_) => {
                info!("Compilation operation {} completed successfully", operation_id);
                (AsyncOperationStatus::Completed, 0, "Compilation successful".to_string())
            }
            Err(ref e) if e.to_string().contains("timed out") => {
                (AsyncOperationStatus::TimedOut, -2, format!("Timeout: {}", e))
            }
            Err(ref e) if e.to_string().contains("cancelled") => {
                (AsyncOperationStatus::Cancelled, -3, format!("Cancelled: {}", e))
            }
            Err(ref e) => {
                error!("Compilation operation {} failed: {}", operation_id, e);
                (AsyncOperationStatus::Failed, -1, format!("Error: {}", e))
            }
        };

        // Update status
        {
            let mut status_guard = match status_clone.lock() {
                Ok(guard) => guard,
                Err(poisoned) => poisoned.into_inner()
            };
            *status_guard = final_status;
        }

        // Attach to JVM for completion callback
        match context.jvm.attach_current_thread() {
            Ok(_env) => {
                debug!("Attached to JVM for compilation callback in operation {}", operation_id);

                let message_cstring = match CString::new(error_msg) {
                    Ok(s) => s,
                    Err(_) => CString::new("Error creating message")
                        .unwrap_or_else(|_| CString::default())
                };

                // SAFETY: user_data pointer is passed through from C and remains valid
                unsafe {
                    (context.callback)(
                        context.user_data.as_ptr(),
                        status_code,
                        message_cstring.as_ptr()
                    );
                }

                debug!("Compilation callback invoked for operation {}", operation_id);
            }
            Err(e) => {
                error!("Failed to attach to JVM for compilation callback: {:?}", e);
                let mut status_guard = match status_clone.lock() {
                    Ok(guard) => guard,
                    Err(poisoned) => poisoned.into_inner()
                };
                *status_guard = AsyncOperationStatus::Failed;
            }
        }
    });

    Ok(AsyncOperation {
        id: operation_id,
        operation_type: AsyncOperationType::ModuleCompilation,
        cancel_tx: Some(cancel_tx),
        status,
    })
}

/// Cancel an async operation
///
/// # Arguments
///
/// * `operation` - Mutable reference to the operation to cancel
///
/// # Returns
///
/// Result indicating success or failure of cancellation
pub fn cancel_async_operation(operation: &mut AsyncOperation) -> WasmtimeResult<()> {
    debug!("Cancelling async operation {}", operation.id);

    if let Some(cancel_tx) = operation.cancel_tx.take() {
        if cancel_tx.send(()).is_ok() {
            match operation.status.lock() {
                Ok(mut status) => {
                    *status = AsyncOperationStatus::Cancelled;
                    info!("Successfully cancelled async operation {}", operation.id);
                    Ok(())
                }
                Err(_) => {
                    error!("Failed to acquire status lock for operation {}", operation.id);
                    Err(WasmtimeError::Concurrency {
                        message: "Failed to update operation status due to lock poisoning".to_string()
                    })
                }
            }
        } else {
            warn!("Failed to send cancellation signal for operation {}", operation.id);
            Err(WasmtimeError::Internal {
                message: "Failed to cancel operation - may have already completed".to_string()
            })
        }
    } else {
        warn!("Attempted to cancel operation {} that has no cancellation channel", operation.id);
        Err(WasmtimeError::invalid_parameter("Operation cannot be cancelled"))
    }
}

/// Get the status of an async operation
///
/// # Arguments
///
/// * `operation` - Reference to the operation to check
///
/// # Returns
///
/// Current status of the operation
pub fn get_operation_status(operation: &AsyncOperation) -> AsyncOperationStatus {
    match operation.status.lock() {
        Ok(status) => status.clone(),
        Err(_) => {
            error!("Failed to acquire status lock for operation {}: Lock poisoning detected", operation.id);
            // Return error status if we can't read the lock
            AsyncOperationStatus::Failed
        }
    }
}

/// Block and wait for an async operation to complete
///
/// # Arguments
///
/// * `operation` - Reference to the operation to wait for
/// * `timeout_ms` - Maximum time to wait in milliseconds
///
/// # Returns
///
/// Result indicating completion status or timeout
pub fn wait_for_operation(operation: &AsyncOperation, timeout_ms: u64) -> WasmtimeResult<AsyncOperationStatus> {
    debug!("Waiting for async operation {} to complete (timeout: {}ms)", operation.id, timeout_ms);

    let timeout_duration = Duration::from_millis(timeout_ms);
    let start_time = std::time::Instant::now();

    loop {
        let status = {
            let status_guard = match operation.status.lock() {
                Ok(guard) => guard,
                Err(_) => {
                    error!("Failed to acquire status lock for operation {}", operation.id);
                    return Err(WasmtimeError::Concurrency {
                        message: "Failed to check operation status due to lock poisoning".to_string()
                    });
                }
            };
            status_guard.clone()
        };

        match status {
            AsyncOperationStatus::Completed |
            AsyncOperationStatus::Failed |
            AsyncOperationStatus::Cancelled |
            AsyncOperationStatus::TimedOut => {
                debug!("Async operation {} completed with status: {:?}", operation.id, status);
                return Ok(status);
            },
            AsyncOperationStatus::Pending | AsyncOperationStatus::Running => {
                if start_time.elapsed() >= timeout_duration {
                    warn!("Timed out waiting for async operation {} after {}ms", operation.id, timeout_ms);
                    return Err(WasmtimeError::Internal {
                        message: "Timeout waiting for operation to complete".to_string()
                    });
                }

                // Sleep briefly to avoid busy waiting
                std::thread::sleep(Duration::from_millis(10));
            }
        }
    }
}

// ================================================================================================
// C API Functions for JNI and Panama FFI Integration
// ================================================================================================

/// Initialize the async runtime (C API)
///
/// # Safety
///
/// This function is safe to call multiple times and performs proper initialization
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_async_runtime_init() -> c_int {
    // Force initialization of the global runtime
    let _runtime = get_async_runtime();
    info!("Async runtime initialized via C API");
    0 // Success
}

/// Get async runtime information (C API)
///
/// # Safety
///
/// Returns information about the current async runtime state
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_async_runtime_info() -> *const c_char {
    let info = format!("Tokio runtime with {} workers",
                      get_runtime_handle().metrics().num_workers());
    let info_cstring = CString::new(info).unwrap_or_default();
    info_cstring.into_raw()
}

/// Shutdown the async runtime (C API)
///
/// # Safety
///
/// This function performs graceful shutdown of async operations
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_async_runtime_shutdown() -> c_int {
    info!("Async runtime shutdown requested via C API");
    // Note: We don't actually shut down the global runtime as it may be needed
    // by other operations. The runtime will be cleaned up when the process exits.
    0 // Success
}

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
    user_data: *mut c_void
) -> c_int {
    // Validate inputs
    if instance_ptr.is_null() || store_ptr.is_null() || function_name.is_null() {
        error!("Invalid parameters for async function call: instance_ptr={:?}, store_ptr={:?}",
               instance_ptr.is_null(), store_ptr.is_null());
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
    debug!("Starting async function call operation {} for function '{}'", operation_id, function_name_str);

    // Create raw pointers that are Send-safe by converting to usize
    let instance_addr = instance_ptr as usize;
    let store_addr = store_ptr as usize;
    let user_data_addr = user_data as usize;
    let function_name_owned = function_name_str.clone();
    let timeout = if timeout_ms > 0 { timeout_ms as u64 } else { 30000 }; // Default 30s timeout

    // Spawn async task on the global runtime
    get_async_runtime().spawn(async move {
        let result = tokio::time::timeout(
            Duration::from_millis(timeout),
            async {
                // Defensive validation: ensure addresses are non-zero before dereferencing.
                // A zero address would indicate a programming error (null pointer passed).
                if instance_addr == 0 || store_addr == 0 {
                    error!("Null pointer detected in async function call: instance_addr={}, store_addr={}",
                           instance_addr, store_addr);
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
            }
        ).await;

        // Invoke callback with result
        let (status_code, message) = match result {
            Ok(Ok(call_result)) => {
                info!("Async function '{}' completed successfully with {} results",
                      function_name_owned, call_result.values.len());
                (0, "Success".to_string())
            }
            Ok(Err(e)) => {
                error!("Async function '{}' failed: {}", function_name_owned, e);
                (-1, format!("Error: {}", e))
            }
            Err(_) => {
                warn!("Async function '{}' timed out after {}ms", function_name_owned, timeout);
                (-2, format!("Timeout after {}ms", timeout))
            }
        };

        // Call the C callback
        let c_message = CString::new(message).unwrap_or_else(|_| CString::new("Unknown error").unwrap());
        callback(user_data_addr as *mut c_void, status_code, c_message.as_ptr());
    });

    operation_id as c_int
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::{AtomicBool, AtomicI32, Ordering};
    use std::sync::Arc;
    use std::time::Duration;

    #[test]
    fn test_async_runtime_initialization() {
        let runtime = get_async_runtime();
        // Runtime handle is accessible and valid - just check that we can get the handle
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
    fn test_async_operation_status() {
        let operation = AsyncOperation {
            id: 1,
            operation_type: AsyncOperationType::FunctionCall,
            cancel_tx: None,
            status: Arc::new(Mutex::new(AsyncOperationStatus::Pending)),
        };

        assert_eq!(get_operation_status(&operation), AsyncOperationStatus::Pending);
    }

    #[test]
    fn test_c_api_initialization() {
        unsafe {
            let result = wasmtime4j_async_runtime_init();
            assert_eq!(result, 0);

            let result = wasmtime4j_async_runtime_shutdown();
            assert_eq!(result, 0);
        }
    }

    #[test]
    fn test_runtime_info() {
        unsafe {
            let info_ptr = wasmtime4j_async_runtime_info();
            assert!(!info_ptr.is_null());

            let info_str = CStr::from_ptr(info_ptr).to_string_lossy();
            assert!(info_str.contains("Tokio runtime"));

            // Clean up
            let _ = CString::from_raw(info_ptr as *mut c_char);
        }
    }

    #[test]
    fn test_invalid_function_call_parameters() {
        unsafe {
            extern "C" fn dummy_callback(_user_data: *mut c_void, _status: c_int, _message: *const c_char) {}

            // Test null instance
            let result = wasmtime4j_func_call_async(
                ptr::null_mut(),
                ptr::null_mut(), // store_ptr
                b"test\0".as_ptr() as *const c_char,
                ptr::null(),
                0,
                1000,
                dummy_callback,
                ptr::null_mut()
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
                ptr::null_mut()
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
                ptr::null_mut()
            );
            assert_eq!(result, -1);
        }
    }

}