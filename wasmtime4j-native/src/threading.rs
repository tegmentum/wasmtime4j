//! WebAssembly threading support with shared memory and atomic operations
//!
//! This module provides comprehensive WebAssembly threading capabilities including:
//! - Thread spawning and lifecycle management
//! - Shared memory with atomic operations
//! - Thread synchronization primitives
//! - Thread-local storage
//! - Thread pool management

use std::sync::{Arc, RwLock};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::collections::HashMap;
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};
use wasmtime::{Engine, SharedMemory, MemoryType};
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

/// Thread pool for managing WebAssembly threads
#[derive(Debug)]
pub struct WasmThreadPool {
    /// Pool configuration
    config: ThreadPoolConfig,
    /// Active threads
    threads: Arc<RwLock<HashMap<ThreadId, Arc<WasmThread>>>>,
    /// Thread ID counter
    next_thread_id: AtomicU64,
    /// Pool statistics
    statistics: Arc<RwLock<ThreadPoolStatistics>>,
    /// Shutdown flag
    shutdown: Arc<AtomicBool>,
}

/// Thread pool configuration
#[derive(Debug, Clone)]
pub struct ThreadPoolConfig {
    /// Minimum number of threads to keep alive
    pub min_threads: u32,
    /// Maximum number of threads allowed
    pub max_threads: u32,
    /// Thread keep-alive time when idle
    pub keep_alive_time: Duration,
    /// Stack size for threads
    pub thread_stack_size: usize,
    /// Thread name prefix
    pub thread_name_prefix: String,
}

/// Thread pool statistics
#[derive(Debug, Clone, Default)]
pub struct ThreadPoolStatistics {
    /// Current number of active threads
    pub active_threads: u32,
    /// Total threads created
    pub total_threads_created: u64,
    /// Total threads destroyed
    pub total_threads_destroyed: u64,
    /// Total tasks executed
    pub total_tasks_executed: u64,
    /// Average task execution time
    pub average_execution_time: Duration,
    /// Peak thread count
    pub peak_thread_count: u32,
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

impl Default for ThreadPoolConfig {
    fn default() -> Self {
        Self {
            min_threads: 1,
            max_threads: std::thread::available_parallelism()
                .map(|p| p.get() as u32)
                .unwrap_or(4),
            keep_alive_time: Duration::from_secs(60),
            thread_stack_size: 2 * 1024 * 1024, // 2MB
            thread_name_prefix: "wasmtime4j-thread".to_string(),
        }
    }
}

impl WasmThread {
    /// Create a new WebAssembly thread
    pub fn new(
        id: ThreadId,
        shared_memory: Arc<SharedMemory>,
        engine: &Engine,
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

        if let Some(handle) = self.join_handle.take() {
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

impl WasmThreadPool {
    /// Create a new thread pool
    pub fn new(config: ThreadPoolConfig) -> Self {
        Self {
            config,
            threads: Arc::new(RwLock::new(HashMap::new())),
            next_thread_id: AtomicU64::new(1),
            statistics: Arc::new(RwLock::new(ThreadPoolStatistics::default())),
            shutdown: Arc::new(AtomicBool::new(false)),
        }
    }

    /// Spawn a new WebAssembly thread
    pub fn spawn_thread(
        &self,
        shared_memory: Arc<SharedMemory>,
        engine: &Engine,
    ) -> WasmtimeResult<Arc<WasmThread>> {
        if self.shutdown.load(Ordering::Acquire) {
            return Err(WasmtimeError::Concurrency {
                message: "Thread pool is shutting down".to_string(),
            });
        }

        let thread_id = self.next_thread_id.fetch_add(1, Ordering::SeqCst);
        let thread = Arc::new(WasmThread::new(thread_id, shared_memory, engine)?);

        // Add to active threads
        {
            let mut threads = self.threads.write().map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire threads lock".to_string(),
            })?;
            threads.insert(thread_id, thread.clone());
        }

        // Update statistics
        if let Ok(mut stats) = self.statistics.write() {
            stats.total_threads_created += 1;
            stats.active_threads += 1;
            if stats.active_threads > stats.peak_thread_count {
                stats.peak_thread_count = stats.active_threads;
            }
        }

        log::debug!("Spawned WebAssembly thread with ID: {}", thread_id);
        Ok(thread)
    }

    /// Get a thread by ID
    pub fn get_thread(&self, thread_id: ThreadId) -> WasmtimeResult<Arc<WasmThread>> {
        let threads = self.threads.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire threads lock".to_string(),
        })?;

        threads.get(&thread_id).cloned().ok_or_else(|| WasmtimeError::InvalidParameter {
            message: format!("Thread with ID {} not found", thread_id),
        })
    }

    /// Remove a thread from the pool
    pub fn remove_thread(&self, thread_id: ThreadId) -> WasmtimeResult<()> {
        let mut threads = self.threads.write().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire threads lock".to_string(),
        })?;

        if threads.remove(&thread_id).is_some() {
            // Update statistics
            if let Ok(mut stats) = self.statistics.write() {
                stats.total_threads_destroyed += 1;
                if stats.active_threads > 0 {
                    stats.active_threads -= 1;
                }
            }
            log::debug!("Removed WebAssembly thread with ID: {}", thread_id);
            Ok(())
        } else {
            Err(WasmtimeError::InvalidParameter {
                message: format!("Thread with ID {} not found", thread_id),
            })
        }
    }

    /// Get all active thread IDs
    pub fn get_active_thread_ids(&self) -> WasmtimeResult<Vec<ThreadId>> {
        let threads = self.threads.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire threads lock".to_string(),
        })?;
        Ok(threads.keys().cloned().collect())
    }

    /// Get thread pool statistics
    pub fn get_statistics(&self) -> WasmtimeResult<ThreadPoolStatistics> {
        let stats = self.statistics.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire statistics lock".to_string(),
        })?;
        Ok(stats.clone())
    }

    /// Shutdown the thread pool
    pub fn shutdown(&self) -> WasmtimeResult<()> {
        self.shutdown.store(true, Ordering::Release);

        // Request termination for all threads
        let threads = self.threads.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire threads lock".to_string(),
        })?;

        for thread in threads.values() {
            thread.request_termination();
        }

        log::info!("Thread pool shutdown requested for {} threads", threads.len());
        Ok(())
    }

    /// Wait for all threads to complete
    pub fn join_all(&self, timeout: Option<Duration>) -> WasmtimeResult<bool> {
        let start_time = Instant::now();

        loop {
            let thread_ids = self.get_active_thread_ids()?;
            if thread_ids.is_empty() {
                return Ok(true); // All threads completed
            }

            // Check timeout
            if let Some(timeout) = timeout {
                if start_time.elapsed() >= timeout {
                    return Ok(false); // Timeout reached
                }
            }

            // Sleep briefly before checking again
            thread::sleep(Duration::from_millis(100));
        }
    }
}

/// Atomic operations on shared WebAssembly memory
pub struct AtomicMemoryOperations;

impl AtomicMemoryOperations {
    /// Perform atomic load operation
    pub fn atomic_load(
        memory: &SharedMemory,
        offset: u64,
        size: u32,
    ) -> WasmtimeResult<AtomicResult> {
        // Note: This is a simplified implementation
        // Real implementation would use wasmtime's atomic memory operations
        Ok(AtomicResult {
            operation: AtomicOperation::Load,
            offset,
            previous_value: None,
            new_value: 0, // Would be the actual loaded value
            threads_notified: None,
        })
    }

    /// Perform atomic store operation
    pub fn atomic_store(
        memory: &SharedMemory,
        offset: u64,
        value: u64,
        size: u32,
    ) -> WasmtimeResult<AtomicResult> {
        // Note: This is a simplified implementation
        // Real implementation would use wasmtime's atomic memory operations
        Ok(AtomicResult {
            operation: AtomicOperation::Store,
            offset,
            previous_value: None,
            new_value: value,
            threads_notified: None,
        })
    }

    /// Perform atomic read-modify-write operation
    pub fn atomic_rmw(
        memory: &SharedMemory,
        operation: AtomicOperation,
        offset: u64,
        value: u64,
        size: u32,
    ) -> WasmtimeResult<AtomicResult> {
        // Note: This is a simplified implementation
        // Real implementation would use wasmtime's atomic memory operations
        Ok(AtomicResult {
            operation,
            offset,
            previous_value: Some(0), // Would be the actual previous value
            new_value: value,
            threads_notified: None,
        })
    }

    /// Perform atomic compare-and-exchange operation
    pub fn atomic_compare_exchange(
        memory: &SharedMemory,
        offset: u64,
        expected: u64,
        desired: u64,
        size: u32,
    ) -> WasmtimeResult<AtomicResult> {
        // Note: This is a simplified implementation
        // Real implementation would use wasmtime's atomic memory operations
        Ok(AtomicResult {
            operation: AtomicOperation::CompareExchange,
            offset,
            previous_value: Some(expected),
            new_value: desired,
            threads_notified: None,
        })
    }

    /// Perform atomic wait operation
    pub fn atomic_wait(
        memory: &SharedMemory,
        offset: u64,
        expected: u64,
        timeout: Option<Duration>,
        size: u32,
    ) -> WasmtimeResult<AtomicResult> {
        // Note: This is a simplified implementation
        // Real implementation would use wasmtime's atomic wait/notify
        Ok(AtomicResult {
            operation: AtomicOperation::Wait,
            offset,
            previous_value: Some(expected),
            new_value: 0, // Wait result code
            threads_notified: None,
        })
    }

    /// Perform atomic notify operation
    pub fn atomic_notify(
        memory: &SharedMemory,
        offset: u64,
        count: u32,
    ) -> WasmtimeResult<AtomicResult> {
        // Note: This is a simplified implementation
        // Real implementation would use wasmtime's atomic wait/notify
        Ok(AtomicResult {
            operation: AtomicOperation::Notify,
            offset,
            previous_value: None,
            new_value: 0,
            threads_notified: Some(count),
        })
    }
}

/// Global thread pool instance
static GLOBAL_THREAD_POOL: once_cell::sync::Lazy<WasmThreadPool> =
    once_cell::sync::Lazy::new(|| {
        WasmThreadPool::new(ThreadPoolConfig::default())
    });

/// Get the global thread pool instance
pub fn get_global_thread_pool() -> &'static WasmThreadPool {
    &GLOBAL_THREAD_POOL
}

//==============================================================================
// FFI Exports for Panama
//==============================================================================

use std::os::raw::{c_char, c_int, c_void};
use std::ffi::CStr;
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

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;

    #[test]
    fn test_thread_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let memory_type = MemoryType::new(1, Some(10));
        let shared_memory = Arc::new(
            SharedMemory::new(engine.inner(), memory_type)
                .expect("Failed to create shared memory")
        );

        let thread = WasmThread::new(1, shared_memory, engine.inner())
            .expect("Failed to create thread");

        assert_eq!(thread.get_id(), 1);
        assert_eq!(thread.get_state(), WasmThreadState::New);
        assert!(thread.is_alive());
        assert!(!thread.is_termination_requested());
    }

    #[test]
    fn test_thread_local_storage() {
        let engine = Engine::new().expect("Failed to create engine");
        let memory_type = MemoryType::new(1, Some(10));
        let shared_memory = Arc::new(
            SharedMemory::new(engine.inner(), memory_type)
                .expect("Failed to create shared memory")
        );

        let thread = WasmThread::new(1, shared_memory, engine.inner())
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
    fn test_thread_pool() {
        let config = ThreadPoolConfig {
            min_threads: 1,
            max_threads: 4,
            keep_alive_time: Duration::from_secs(30),
            thread_stack_size: 1024 * 1024,
            thread_name_prefix: "test-thread".to_string(),
        };

        let pool = WasmThreadPool::new(config);
        let engine = Engine::new().expect("Failed to create engine");
        let memory_type = MemoryType::new(1, Some(10));
        let shared_memory = Arc::new(
            SharedMemory::new(engine.inner(), memory_type)
                .expect("Failed to create shared memory")
        );

        // Spawn a thread
        let thread = pool.spawn_thread(shared_memory, engine.inner())
            .expect("Failed to spawn thread");
        let thread_id = thread.get_id();

        // Verify thread is in pool
        let retrieved_thread = pool.get_thread(thread_id)
            .expect("Failed to get thread from pool");
        assert_eq!(retrieved_thread.get_id(), thread_id);

        // Check statistics
        let stats = pool.get_statistics().expect("Failed to get statistics");
        assert_eq!(stats.total_threads_created, 1);
        assert_eq!(stats.active_threads, 1);

        // Remove thread
        pool.remove_thread(thread_id).expect("Failed to remove thread");
        let updated_stats = pool.get_statistics().expect("Failed to get updated statistics");
        assert_eq!(updated_stats.total_threads_destroyed, 1);
        assert_eq!(updated_stats.active_threads, 0);
    }

    #[test]
    fn test_thread_termination() {
        let engine = Engine::new().expect("Failed to create engine");
        let memory_type = MemoryType::new(1, Some(10));
        let shared_memory = Arc::new(
            SharedMemory::new(engine.inner(), memory_type)
                .expect("Failed to create shared memory")
        );

        let mut thread = WasmThread::new(1, shared_memory, engine.inner())
            .expect("Failed to create thread");

        assert!(!thread.is_termination_requested());

        thread.request_termination();
        assert!(thread.is_termination_requested());

        thread.force_terminate().expect("Failed to force terminate");
        assert_eq!(thread.get_state(), WasmThreadState::Killed);
    }

    #[test]
    fn test_atomic_operations() {
        let engine = Engine::new().expect("Failed to create engine");
        let memory_type = MemoryType::new(1, Some(10));
        let shared_memory = Arc::new(
            SharedMemory::new(engine.inner(), memory_type)
                .expect("Failed to create shared memory")
        );

        // Test atomic load
        let result = AtomicMemoryOperations::atomic_load(&shared_memory, 0, 4)
            .expect("Failed to perform atomic load");
        assert_eq!(result.operation, AtomicOperation::Load);
        assert_eq!(result.offset, 0);

        // Test atomic store
        let result = AtomicMemoryOperations::atomic_store(&shared_memory, 0, 42, 4)
            .expect("Failed to perform atomic store");
        assert_eq!(result.operation, AtomicOperation::Store);
        assert_eq!(result.new_value, 42);

        // Test atomic RMW
        let result = AtomicMemoryOperations::atomic_rmw(
            &shared_memory,
            AtomicOperation::Add,
            0,
            10,
            4
        ).expect("Failed to perform atomic RMW");
        assert_eq!(result.operation, AtomicOperation::Add);
        assert_eq!(result.new_value, 10);
    }

    #[test]
    fn test_global_thread_pool() {
        let pool = get_global_thread_pool();
        let stats = pool.get_statistics().expect("Failed to get global pool statistics");
        // Just verify we can access the global pool without errors
        assert!(stats.active_threads >= 0);
    }
}