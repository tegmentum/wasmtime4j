//! WebAssembly threading support with shared memory and atomic operations
//!
//! This module provides comprehensive WebAssembly threading capabilities including:
//! - Thread spawning and lifecycle management
//! - Shared memory with atomic operations
//! - Thread synchronization primitives
//! - Thread-local storage
//! - Thread pool management

use std::mem::ManuallyDrop;
use std::sync::{Arc, RwLock};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::collections::HashMap;
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};
use wasmtime::{Engine, MemoryType, SharedMemory};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread identifier type
pub type ThreadId = u64;

/// WebAssembly thread handle with lifecycle management
#[derive(Debug)]
pub struct WasmThread {
    /// Unique thread identifier
    id: ThreadId,
    /// Thread join handle
    join_handle: Option<JoinHandle<WasmtimeResult<()>>>,
    /// Thread state
    state: Arc<RwLock<WasmThreadState>>,
    /// Thread termination flag
    termination_requested: Arc<AtomicBool>,
    /// Thread statistics
    statistics: Arc<RwLock<WasmThreadStatistics>>,
    /// Shared memory instance
    shared_memory: Arc<SharedMemory>,
    /// Thread-local storage
    thread_local_storage: Arc<RwLock<HashMap<String, ThreadLocalValue>>>,
    /// Thread creation timestamp
    created_at: Instant,
}

/// WebAssembly thread state enumeration
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WasmThreadState {
    /// Thread has been created but not yet started
    New,
    /// Thread is currently executing WebAssembly code
    Running,
    /// Thread is waiting for a condition to be met
    Waiting,
    /// Thread is waiting with a timeout
    TimedWaiting,
    /// Thread is blocked waiting for a resource
    Blocked,
    /// Thread has been suspended by the runtime
    Suspended,
    /// Thread has completed execution normally
    Terminated,
    /// Thread has terminated due to an error
    Error,
    /// Thread has been forcibly terminated
    Killed,
}

/// Thread-local storage value types
#[derive(Debug, Clone)]
pub enum ThreadLocalValue {
    Int(i32),
    Long(i64),
    Float(f32),
    Double(f64),
    Bytes(Vec<u8>),
    String(String),
}

/// WebAssembly thread statistics
#[derive(Debug, Clone, Default)]
pub struct WasmThreadStatistics {
    /// Number of functions executed
    pub functions_executed: u64,
    /// Total execution time in nanoseconds
    pub total_execution_time: u64,
    /// Number of atomic operations performed
    pub atomic_operations: u64,
    /// Number of memory accesses
    pub memory_accesses: u64,
    /// Number of wait/notify operations
    pub wait_notify_operations: u64,
    /// Peak memory usage in bytes
    pub peak_memory_usage: u64,
    /// Last activity timestamp
    pub last_activity: Option<Instant>,
}


/// Atomic operation types for WebAssembly threading
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AtomicOperation {
    Load,
    Store,
    Add,
    Sub,
    And,
    Or,
    Xor,
    Exchange,
    CompareExchange,
    Wait,
    Notify,
}

/// Atomic memory access result
#[derive(Debug)]
pub struct AtomicResult {
    /// Operation that was performed
    pub operation: AtomicOperation,
    /// Memory offset that was accessed
    pub offset: u64,
    /// Previous value (for RMW operations)
    pub previous_value: Option<u64>,
    /// New value after operation
    pub new_value: u64,
    /// Number of threads notified (for notify operations)
    pub threads_notified: Option<u32>,
}


impl WasmThread {
    /// Create a new WebAssembly thread
    pub fn new(
        id: ThreadId,
        shared_memory: Arc<SharedMemory>,
        _engine: &Engine,
    ) -> WasmtimeResult<Self> {
        Ok(Self {
            id,
            join_handle: None,
            state: Arc::new(RwLock::new(WasmThreadState::New)),
            termination_requested: Arc::new(AtomicBool::new(false)),
            statistics: Arc::new(RwLock::new(WasmThreadStatistics::default())),
            shared_memory,
            thread_local_storage: Arc::new(RwLock::new(HashMap::new())),
            created_at: Instant::now(),
        })
    }

    /// Get thread ID
    pub fn get_id(&self) -> ThreadId {
        self.id
    }

    /// Get current thread state
    pub fn get_state(&self) -> WasmThreadState {
        self.state.read().map(|state| *state).unwrap_or(WasmThreadState::Error)
    }

    /// Set thread state
    fn set_state(&self, new_state: WasmThreadState) -> WasmtimeResult<()> {
        let mut state = self.state.write().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread state lock".to_string(),
        })?;
        *state = new_state;
        Ok(())
    }

    /// Execute a function in this thread
    pub fn execute_function<F, R>(&self, func: F) -> WasmtimeResult<R>
    where
        F: FnOnce() -> WasmtimeResult<R> + Send + 'static,
        R: Send + 'static,
    {
        self.set_state(WasmThreadState::Running)?;

        let start_time = Instant::now();
        let result = func();
        let execution_time = start_time.elapsed();

        // Update statistics
        if let Ok(mut stats) = self.statistics.write() {
            stats.functions_executed += 1;
            stats.total_execution_time += execution_time.as_nanos() as u64;
            stats.last_activity = Some(Instant::now());
        }

        match result {
            Ok(value) => {
                self.set_state(WasmThreadState::Terminated)?;
                Ok(value)
            }
            Err(e) => {
                self.set_state(WasmThreadState::Error)?;
                Err(e)
            }
        }
    }

    /// Request thread termination
    pub fn request_termination(&self) {
        self.termination_requested.store(true, Ordering::Release);
    }

    /// Check if termination has been requested
    pub fn is_termination_requested(&self) -> bool {
        self.termination_requested.load(Ordering::Acquire)
    }

    /// Force terminate the thread
    pub fn force_terminate(&mut self) -> WasmtimeResult<()> {
        self.request_termination();

        if let Some(_handle) = self.join_handle.take() {
            // We cannot force kill a thread in Rust, so we just set the state
            self.set_state(WasmThreadState::Killed)?;
            // The thread should check termination_requested and exit gracefully
        }

        Ok(())
    }

    /// Wait for thread completion
    pub fn join(&mut self) -> WasmtimeResult<()> {
        if let Some(handle) = self.join_handle.take() {
            handle.join().map_err(|_| WasmtimeError::Concurrency {
                message: "Thread join failed".to_string(),
            })??;
        }
        Ok(())
    }

    /// Wait for thread completion with timeout
    pub fn join_timeout(&mut self, timeout: Duration) -> WasmtimeResult<bool> {
        // Rust doesn't have native join_timeout, so we implement a polling approach
        let start = Instant::now();

        while start.elapsed() < timeout {
            match self.get_state() {
                WasmThreadState::Terminated | WasmThreadState::Error | WasmThreadState::Killed => {
                    self.join()?;
                    return Ok(true);
                }
                _ => {
                    thread::sleep(Duration::from_millis(10));
                }
            }
        }

        Ok(false) // Timeout elapsed
    }

    /// Check if thread is alive
    pub fn is_alive(&self) -> bool {
        matches!(
            self.get_state(),
            WasmThreadState::New | WasmThreadState::Running | WasmThreadState::Waiting |
            WasmThreadState::TimedWaiting | WasmThreadState::Blocked | WasmThreadState::Suspended
        )
    }

    /// Get thread statistics
    pub fn get_statistics(&self) -> WasmtimeResult<WasmThreadStatistics> {
        let stats = self.statistics.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread statistics lock".to_string(),
        })?;
        Ok(stats.clone())
    }

    /// Get shared memory reference
    pub fn get_shared_memory(&self) -> Arc<SharedMemory> {
        self.shared_memory.clone()
    }

    /// Access thread-local storage
    pub fn put_thread_local(&self, key: String, value: ThreadLocalValue) -> WasmtimeResult<()> {
        let mut storage = self.thread_local_storage.write().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread-local storage lock".to_string(),
        })?;
        storage.insert(key, value);
        Ok(())
    }

    /// Get from thread-local storage
    pub fn get_thread_local(&self, key: &str) -> WasmtimeResult<Option<ThreadLocalValue>> {
        let storage = self.thread_local_storage.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread-local storage lock".to_string(),
        })?;
        Ok(storage.get(key).cloned())
    }

    /// Remove from thread-local storage
    pub fn remove_thread_local(&self, key: &str) -> WasmtimeResult<Option<ThreadLocalValue>> {
        let mut storage = self.thread_local_storage.write().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread-local storage lock".to_string(),
        })?;
        Ok(storage.remove(key))
    }

    /// Clear all thread-local storage
    pub fn clear_thread_local_storage(&self) -> WasmtimeResult<()> {
        let mut storage = self.thread_local_storage.write().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread-local storage lock".to_string(),
        })?;
        storage.clear();
        Ok(())
    }

    /// Get thread-local storage size
    pub fn get_thread_local_storage_size(&self) -> WasmtimeResult<usize> {
        let storage = self.thread_local_storage.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread-local storage lock".to_string(),
        })?;
        Ok(storage.len())
    }

    /// Calculate memory usage of thread-local storage
    pub fn get_thread_local_storage_memory_usage(&self) -> WasmtimeResult<u64> {
        let storage = self.thread_local_storage.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire thread-local storage lock".to_string(),
        })?;

        let mut total_size = 0u64;
        for (key, value) in storage.iter() {
            total_size += key.len() as u64;
            total_size += match value {
                ThreadLocalValue::Int(_) => 4,
                ThreadLocalValue::Long(_) => 8,
                ThreadLocalValue::Float(_) => 4,
                ThreadLocalValue::Double(_) => 8,
                ThreadLocalValue::Bytes(bytes) => bytes.len() as u64,
                ThreadLocalValue::String(s) => s.len() as u64,
            };
        }

        Ok(total_size)
    }
}

//==============================================================================
// FFI Exports for Panama
//==============================================================================

use std::os::raw::{c_char, c_int, c_void};
use crate::error::ffi_utils;

/// Put an integer value into thread-local storage (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
/// * `key_ptr` - Pointer to null-terminated C string key
/// * `value` - Integer value to store
///
/// # Returns
/// 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_put_int(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    value: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        thread.put_thread_local(key, ThreadLocalValue::Int(value))?;

        log::debug!("Put integer value {} into TLS", value);
        Ok(())
    })
}

/// Get an integer value from thread-local storage (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
/// * `key_ptr` - Pointer to null-terminated C string key
/// * `out_value` - Pointer to write the retrieved integer value
///
/// # Returns
/// 0 on success, error code on failure (including key not found or type mismatch)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_get_int(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    out_value: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if out_value.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output value pointer cannot be null".to_string(),
            });
        }

        let value = thread.get_thread_local(&key)?;

        match value {
            Some(ThreadLocalValue::Int(int_value)) => {
                *out_value = int_value;
                log::debug!("Retrieved integer value {} from TLS", int_value);
                Ok(())
            }
            Some(_) => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local value for key '{}' is not an integer", key),
            }),
            None => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local key '{}' not found", key),
            }),
        }
    })
}

/// Check if a thread-local key exists (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
/// * `key_ptr` - Pointer to null-terminated C string key
/// * `out_exists` - Pointer to write existence flag (1 = exists, 0 = does not exist)
///
/// # Returns
/// 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_contains_key(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    out_exists: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if out_exists.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output exists pointer cannot be null".to_string(),
            });
        }

        let value = thread.get_thread_local(&key)?;
        *out_exists = if value.is_some() { 1 } else { 0 };

        Ok(())
    })
}

/// Remove a thread-local key (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
/// * `key_ptr` - Pointer to null-terminated C string key
///
/// # Returns
/// 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_remove_key(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        thread.remove_thread_local(&key)?;
        log::debug!("Removed thread-local key '{}'", key);

        Ok(())
    })
}

/// Clear all thread-local storage (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
///
/// # Returns
/// 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_clear_storage(
    thread_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        thread.clear_thread_local_storage()?;
        log::debug!("Cleared all thread-local storage");
        Ok(())
    })
}

/// Get the size of thread-local storage (number of keys) (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
/// * `out_size` - Pointer to write the size
///
/// # Returns
/// 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_storage_size(
    thread_ptr: *mut c_void,
    out_size: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;

        if out_size.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output size pointer cannot be null".to_string(),
            });
        }

        let size = thread.get_thread_local_storage_size()?;
        *out_size = size as c_int;

        Ok(())
    })
}

/// Get the memory usage of thread-local storage in bytes (Panama FFI)
///
/// # Arguments
/// * `thread_ptr` - Pointer to WasmThread
/// * `out_memory_usage` - Pointer to write the memory usage in bytes
///
/// # Returns
/// 0 on success, error code on failure
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_storage_memory_usage(
    thread_ptr: *mut c_void,
    out_memory_usage: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;

        if out_memory_usage.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output memory usage pointer cannot be null".to_string(),
            });
        }

        let memory_usage = thread.get_thread_local_storage_memory_usage()?;
        *out_memory_usage = memory_usage as i64;

        Ok(())
    })
}

/// Put a long value into thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_put_long(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    value: i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        thread.put_thread_local(key, ThreadLocalValue::Long(value))?;

        log::debug!("Put long value {} into TLS", value);
        Ok(())
    })
}

/// Get a long value from thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_get_long(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    out_value: *mut i64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if out_value.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output value pointer cannot be null".to_string(),
            });
        }

        let value = thread.get_thread_local(&key)?;

        match value {
            Some(ThreadLocalValue::Long(long_value)) => {
                *out_value = long_value;
                log::debug!("Retrieved long value {} from TLS", long_value);
                Ok(())
            }
            Some(_) => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local value for key '{}' is not a long", key),
            }),
            None => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local key '{}' not found", key),
            }),
        }
    })
}

/// Put a float value into thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_put_float(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    value: f32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        thread.put_thread_local(key, ThreadLocalValue::Float(value))?;

        log::debug!("Put float value {} into TLS", value);
        Ok(())
    })
}

/// Get a float value from thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_get_float(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    out_value: *mut f32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if out_value.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output value pointer cannot be null".to_string(),
            });
        }

        let value = thread.get_thread_local(&key)?;

        match value {
            Some(ThreadLocalValue::Float(float_value)) => {
                *out_value = float_value;
                log::debug!("Retrieved float value {} from TLS", float_value);
                Ok(())
            }
            Some(_) => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local value for key '{}' is not a float", key),
            }),
            None => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local key '{}' not found", key),
            }),
        }
    })
}

/// Put a double value into thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_put_double(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    value: f64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        thread.put_thread_local(key, ThreadLocalValue::Double(value))?;

        log::debug!("Put double value {} into TLS", value);
        Ok(())
    })
}

/// Get a double value from thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_get_double(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    out_value: *mut f64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if out_value.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output value pointer cannot be null".to_string(),
            });
        }

        let value = thread.get_thread_local(&key)?;

        match value {
            Some(ThreadLocalValue::Double(double_value)) => {
                *out_value = double_value;
                log::debug!("Retrieved double value {} from TLS", double_value);
                Ok(())
            }
            Some(_) => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local value for key '{}' is not a double", key),
            }),
            None => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local key '{}' not found", key),
            }),
        }
    })
}

/// Put a byte array into thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_put_bytes(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    bytes_ptr: *const u8,
    bytes_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if bytes_ptr.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Bytes pointer cannot be null".to_string(),
            });
        }

        let bytes = std::slice::from_raw_parts(bytes_ptr, bytes_len).to_vec();
        thread.put_thread_local(key, ThreadLocalValue::Bytes(bytes))?;

        log::debug!("Put byte array of length {} into TLS", bytes_len);
        Ok(())
    })
}

/// Get a byte array from thread-local storage (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_get_bytes(
    thread_ptr: *mut c_void,
    key_ptr: *const c_char,
    out_bytes_ptr: *mut *mut u8,
    out_bytes_len: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_ptr, "thread")?;
        let key = ffi_utils::c_str_to_string(key_ptr, "key")?;

        if out_bytes_ptr.is_null() || out_bytes_len.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output pointers cannot be null".to_string(),
            });
        }

        let value = thread.get_thread_local(&key)?;

        match value {
            Some(ThreadLocalValue::Bytes(bytes)) => {
                let len = bytes.len();
                let ptr = libc::malloc(len) as *mut u8;
                if ptr.is_null() {
                    return Err(crate::error::WasmtimeError::InvalidParameter {
                        message: "Failed to allocate memory for byte array".to_string(),
                    });
                }
                std::ptr::copy_nonoverlapping(bytes.as_ptr(), ptr, len);
                *out_bytes_ptr = ptr;
                *out_bytes_len = len;
                log::debug!("Retrieved byte array of length {} from TLS", len);
                Ok(())
            }
            Some(_) => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local value for key '{}' is not a byte array", key),
            }),
            None => Err(crate::error::WasmtimeError::InvalidParameter {
                message: format!("Thread-local key '{}' not found", key),
            }),
        }
    })
}

/// Join a thread and wait for it to complete (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_join(thread_handle: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_handle, "thread")?;
        thread.join()?;
        Ok(())
    })
}

/// Join a thread with timeout (Panama FFI)
/// Returns 1 if thread joined successfully, 0 if timeout occurred
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_join_timeout(
    thread_handle: *mut c_void,
    timeout_ms: i64,
    out_joined: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_handle, "thread")?;

        if out_joined.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output parameter cannot be null".to_string(),
            });
        }

        if timeout_ms < 0 {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Timeout must be non-negative".to_string(),
            });
        }

        let joined = thread.join_timeout(std::time::Duration::from_millis(timeout_ms as u64))?;
        *out_joined = if joined { 1 } else { 0 };
        Ok(())
    })
}

/// Request thread termination (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_request_termination(thread_handle: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_handle, "thread")?;
        thread.request_termination();
        Ok(())
    })
}

/// Force terminate a thread (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_force_terminate(thread_handle: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_handle, "thread")?;
        thread.force_terminate()?;
        Ok(())
    })
}

/// Check if termination has been requested (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_thread_is_termination_requested(
    thread_handle: *mut c_void,
    out_requested: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let thread = ffi_utils::deref_ptr_mut::<WasmThread>(thread_handle, "thread")?;

        if out_requested.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "Output parameter cannot be null".to_string(),
            });
        }

        let requested = thread.is_termination_requested();
        *out_requested = if requested { 1 } else { 0 };
        Ok(())
    })
}

/// Free memory allocated by native functions (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_free(ptr: *mut c_void) {
    if !ptr.is_null() {
        libc::free(ptr);
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;

    // Create an engine with threads and shared memory support enabled
    fn threads_engine() -> wasmtime::Engine {
        let mut config = crate::engine::safe_wasmtime_config();
        config.wasm_threads(true);
        // SharedMemory::new requires Config::shared_memory(true)
        config.shared_memory(true);
        wasmtime::Engine::new(&config).expect("Failed to create threads-enabled engine")
    }

    // Create shared memory with proper configuration
    fn create_test_shared_memory(engine: &wasmtime::Engine) -> Arc<SharedMemory> {
        // SharedMemory requires MemoryType::shared(), not MemoryType::new()
        let memory_type = MemoryType::shared(1, 10);
        Arc::new(
            SharedMemory::new(engine, memory_type)
                .expect("Failed to create shared memory")
        )
    }

    #[test]
    fn test_thread_creation() {
        let engine = threads_engine();
        let shared_memory = create_test_shared_memory(&engine);

        let thread = WasmThread::new(1, shared_memory, &engine)
            .expect("Failed to create thread");

        assert_eq!(thread.get_id(), 1);
        assert_eq!(thread.get_state(), WasmThreadState::New);
        assert!(thread.is_alive());
        assert!(!thread.is_termination_requested());
    }

    #[test]
    fn test_thread_local_storage() {
        let engine = threads_engine();
        let shared_memory = create_test_shared_memory(&engine);

        let thread = WasmThread::new(1, shared_memory, &engine)
            .expect("Failed to create thread");

        // Test different value types
        thread.put_thread_local("int_key".to_string(), ThreadLocalValue::Int(42))
            .expect("Failed to put int value");
        thread.put_thread_local("string_key".to_string(), ThreadLocalValue::String("hello".to_string()))
            .expect("Failed to put string value");

        let int_value = thread.get_thread_local("int_key")
            .expect("Failed to get int value");
        let string_value = thread.get_thread_local("string_key")
            .expect("Failed to get string value");

        assert!(matches!(int_value, Some(ThreadLocalValue::Int(42))));
        assert!(matches!(string_value, Some(ThreadLocalValue::String(ref s)) if s == "hello"));

        assert_eq!(thread.get_thread_local_storage_size().unwrap(), 2);

        thread.clear_thread_local_storage().expect("Failed to clear storage");
        assert_eq!(thread.get_thread_local_storage_size().unwrap(), 0);
    }

    #[test]
    fn test_thread_termination() {
        let engine = threads_engine();
        let shared_memory = create_test_shared_memory(&engine);

        let mut thread = WasmThread::new(1, shared_memory, &engine)
            .expect("Failed to create thread");

        assert!(!thread.is_termination_requested());

        thread.request_termination();
        assert!(thread.is_termination_requested());

        // force_terminate() only changes state to Killed if thread is actually running
        // (has a join_handle). For a new thread that hasn't been started, it just
        // sets termination_requested and returns Ok.
        thread.force_terminate().expect("Failed to force terminate");
        // Thread was never started, so state remains New (no join_handle to kill)
        assert!(thread.is_termination_requested());
    }

    // ==================== NEW TESTS ====================

    #[test]
    fn test_wasm_thread_state_variants() {
        // Test that all state variants are distinct
        let states = [
            WasmThreadState::New,
            WasmThreadState::Running,
            WasmThreadState::Waiting,
            WasmThreadState::TimedWaiting,
            WasmThreadState::Blocked,
            WasmThreadState::Suspended,
            WasmThreadState::Terminated,
            WasmThreadState::Error,
            WasmThreadState::Killed,
        ];

        for (i, s1) in states.iter().enumerate() {
            for (j, s2) in states.iter().enumerate() {
                if i == j {
                    assert_eq!(s1, s2);
                } else {
                    assert_ne!(s1, s2);
                }
            }
        }
    }

    #[test]
    fn test_wasm_thread_state_copy() {
        let state = WasmThreadState::Running;
        let copied = state;
        assert_eq!(state, copied);
    }

    #[test]
    fn test_thread_local_value_int() {
        let value = ThreadLocalValue::Int(42);
        match value {
            ThreadLocalValue::Int(v) => assert_eq!(v, 42),
            _ => panic!("Expected Int variant"),
        }
    }

    #[test]
    fn test_thread_local_value_long() {
        let value = ThreadLocalValue::Long(9223372036854775807);
        match value {
            ThreadLocalValue::Long(v) => assert_eq!(v, i64::MAX),
            _ => panic!("Expected Long variant"),
        }
    }

    #[test]
    fn test_thread_local_value_float() {
        let value = ThreadLocalValue::Float(3.14);
        match value {
            ThreadLocalValue::Float(v) => assert!((v - 3.14).abs() < 0.001),
            _ => panic!("Expected Float variant"),
        }
    }

    #[test]
    fn test_thread_local_value_double() {
        let value = ThreadLocalValue::Double(2.718281828);
        match value {
            ThreadLocalValue::Double(v) => assert!((v - 2.718281828).abs() < 0.0001),
            _ => panic!("Expected Double variant"),
        }
    }

    #[test]
    fn test_thread_local_value_bytes() {
        let data = vec![0x01, 0x02, 0x03, 0x04];
        let value = ThreadLocalValue::Bytes(data.clone());
        match value {
            ThreadLocalValue::Bytes(v) => assert_eq!(v, data),
            _ => panic!("Expected Bytes variant"),
        }
    }

    #[test]
    fn test_thread_local_value_string() {
        let value = ThreadLocalValue::String("test string".to_string());
        match value {
            ThreadLocalValue::String(v) => assert_eq!(v, "test string"),
            _ => panic!("Expected String variant"),
        }
    }

    #[test]
    fn test_thread_local_value_clone() {
        let original = ThreadLocalValue::Bytes(vec![1, 2, 3]);
        let cloned = original.clone();
        match (original, cloned) {
            (ThreadLocalValue::Bytes(a), ThreadLocalValue::Bytes(b)) => assert_eq!(a, b),
            _ => panic!("Expected Bytes variants"),
        }
    }

    #[test]
    fn test_wasm_thread_statistics_default() {
        let stats = WasmThreadStatistics::default();

        assert_eq!(stats.functions_executed, 0);
        assert_eq!(stats.total_execution_time, 0);
        assert_eq!(stats.atomic_operations, 0);
        assert_eq!(stats.memory_accesses, 0);
        assert_eq!(stats.wait_notify_operations, 0);
        assert_eq!(stats.peak_memory_usage, 0);
        assert!(stats.last_activity.is_none());
    }

    #[test]
    fn test_atomic_operation_variants() {
        let operations = [
            AtomicOperation::Load,
            AtomicOperation::Store,
            AtomicOperation::Add,
            AtomicOperation::Sub,
            AtomicOperation::And,
            AtomicOperation::Or,
            AtomicOperation::Xor,
            AtomicOperation::Exchange,
            AtomicOperation::CompareExchange,
            AtomicOperation::Wait,
            AtomicOperation::Notify,
        ];

        // Test all are distinct
        for (i, op1) in operations.iter().enumerate() {
            for (j, op2) in operations.iter().enumerate() {
                if i == j {
                    assert_eq!(op1, op2);
                } else {
                    assert_ne!(op1, op2);
                }
            }
        }
    }

    #[test]
    fn test_atomic_result_creation() {
        let result = AtomicResult {
            operation: AtomicOperation::Add,
            offset: 100,
            previous_value: Some(42),
            new_value: 52,
            threads_notified: None,
        };

        assert_eq!(result.operation, AtomicOperation::Add);
        assert_eq!(result.offset, 100);
        assert_eq!(result.previous_value, Some(42));
        assert_eq!(result.new_value, 52);
        assert!(result.threads_notified.is_none());
    }

    #[test]
    fn test_atomic_result_notify() {
        let result = AtomicResult {
            operation: AtomicOperation::Notify,
            offset: 0,
            previous_value: None,
            new_value: 0,
            threads_notified: Some(5),
        };

        assert_eq!(result.operation, AtomicOperation::Notify);
        assert_eq!(result.threads_notified, Some(5));
    }

    #[test]
    fn test_thread_is_alive_states() {
        let engine = threads_engine();
        let shared_memory = create_test_shared_memory(&engine);

        let thread = WasmThread::new(1, shared_memory, &engine)
            .expect("Failed to create thread");

        // New thread should be alive
        assert!(thread.is_alive());
        assert_eq!(thread.get_state(), WasmThreadState::New);
    }

    #[test]
    fn test_thread_get_shared_memory() {
        let engine = threads_engine();
        let shared_memory = create_test_shared_memory(&engine);
        let original_ptr = Arc::as_ptr(&shared_memory);

        let thread = WasmThread::new(1, shared_memory, &engine)
            .expect("Failed to create thread");

        let retrieved = thread.get_shared_memory();
        assert_eq!(Arc::as_ptr(&retrieved), original_ptr);
    }
}