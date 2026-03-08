//! WebAssembly store management with resource tracking and isolation
//!
//! The Store provides execution context and resource management for WebAssembly instances.
//! Each store maintains its own isolated execution environment with resource limits,
//! fuel tracking, and comprehensive cleanup capabilities.

use crate::engine::Engine;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::hostfunc::{HostFunction, HostFunctionCallback};
use crate::interop::ReentrantLock;
use crate::module::Module;
use std::ffi::CString;
use std::future::Future;
use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};
#[cfg(feature = "wasi-http")]
use wasmtime::component::ResourceTable;
use wasmtime::{
    AsContext, AsContextMut, CallHook, DebugEvent, DebugHandler, Func, FuncType,
    Store as WasmtimeStore, StoreContext, StoreContextMut, StoreLimits as WasmtimeStoreLimits,
    StoreLimitsBuilder as WasmtimeStoreLimitsBuilder,
};
use wasmtime_wasi::p1::WasiP1Ctx;
use wasmtime_wasi::p2::pipe::MemoryOutputPipe;
#[cfg(feature = "wasi-http")]
use wasmtime_wasi_http::WasiHttpCtx;
#[cfg(feature = "wasi-nn")]
use wasmtime_wasi_nn::witx::WasiNnCtx;

/// Store ID counter for unique identification (atomic for lock-free increment)
static STORE_ID_COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

/// Thread-safe wrapper around Wasmtime store with resource management
///
/// CRITICAL: Uses custom ReentrantLock to allow same-thread reentrant access during WASM execution.
/// This is essential because Wasmtime needs to access the Store context multiple times
/// during function calls, and non-reentrant Mutex causes traps at function entry.
pub struct Store {
    /// Unique identifier for this store
    id: u64,
    pub(crate) inner: Arc<ReentrantLock<WasmtimeStore<StoreData>>>,
    /// Reference to the Engine to ensure same Engine Arc is used for validation
    /// CRITICAL: Wasmtime validates Module/Store compatibility using Arc::ptr_eq(),
    /// so we must maintain a reference to the same Engine (which contains Arc<WasmtimeEngine>)
    engine: Engine,
    metadata: StoreMetadata,
}

/// Store execution metadata and resource tracking
#[derive(Debug, Clone)]
pub struct StoreMetadata {
    /// Timestamp when this store was created
    pub created_at: Instant,
    /// Optional fuel limit for execution metering
    pub fuel_limit: Option<u64>,
    /// Optional memory limit in bytes to prevent resource exhaustion
    pub memory_limit_bytes: Option<usize>,
    /// Optional timeout for store operations to prevent hanging
    pub execution_timeout: Option<Duration>,
    /// Number of instances created in this store
    pub instance_count: usize,
    /// Closed flag to prevent use-after-close
    pub is_closed: Arc<std::sync::atomic::AtomicBool>,
}

/// Store-specific data for host function context
pub struct StoreData {
    /// Store ID for cross-store validation (accessible from Caller<StoreData> in callbacks)
    pub store_id: u64,
    /// Optional user-defined data attached to the store
    pub user_data: Option<Box<dyn std::any::Any + Send + Sync>>,
    /// Resource limits and quotas for this store
    pub resource_limits: ResourceLimits,
    /// Current execution state and statistics
    pub execution_state: ExecutionState,
    /// Optional WASI Preview 1 context stored directly for linker access
    pub wasi_ctx: Option<WasiP1Ctx>,
    /// Optional captured stdout pipe
    pub wasi_stdout_pipe: Option<MemoryOutputPipe>,
    /// Optional captured stderr pipe
    pub wasi_stderr_pipe: Option<MemoryOutputPipe>,
    /// Optional WASI file descriptor manager
    pub wasi_fd_manager: Option<crate::wasi::WasiFileDescriptorManager>,
    /// Optional WASI HTTP context for HTTP requests
    #[cfg(feature = "wasi-http")]
    pub wasi_http_ctx: Option<WasiHttpCtx>,
    /// Resource table for WASI HTTP (required by WasiHttpView trait)
    #[cfg(feature = "wasi-http")]
    pub resource_table: ResourceTable,
    /// Optional WASI-NN context for neural network inference
    #[cfg(feature = "wasi-nn")]
    pub wasi_nn_ctx: Option<WasiNnCtx>,
    /// Optional static resource limits built from Wasmtime's StoreLimitsBuilder.
    /// Applied via store.limiter() when resource limits are configured.
    pub wasmtime_store_limits: Option<WasmtimeStoreLimits>,
    /// Optional callback-based resource limiter for dynamic resource limiting
    pub callback_resource_limiter: Option<CallbackResourceLimiter>,
    /// Optional async callback-based resource limiter for dynamic resource limiting
    pub callback_resource_limiter_async: Option<CallbackResourceLimiterAsync>,
    /// Whether the engine has epoch interruption enabled (cached for caller access)
    pub epoch_interruption_enabled: bool,
    /// Whether the engine has async support enabled (cached for caller access)
    pub async_enabled: bool,
}

impl StoreData {
    /// Create a new StoreData with the given resource limits and all other fields at defaults.
    pub fn new(store_id: u64, resource_limits: ResourceLimits) -> Self {
        Self {
            store_id,
            user_data: None,
            resource_limits,
            execution_state: ExecutionState::default(),
            wasi_ctx: None,
            wasi_stdout_pipe: None,
            wasi_stderr_pipe: None,
            wasi_fd_manager: None,
            #[cfg(feature = "wasi-http")]
            wasi_http_ctx: None,
            #[cfg(feature = "wasi-http")]
            resource_table: ResourceTable::new(),
            #[cfg(feature = "wasi-nn")]
            wasi_nn_ctx: None,
            wasmtime_store_limits: None,
            callback_resource_limiter: None,
            callback_resource_limiter_async: None,
            epoch_interruption_enabled: false,
            async_enabled: false,
        }
    }
}

impl StoreMetadata {
    /// Create a new StoreMetadata with the given limits and a fresh creation timestamp.
    pub fn new(
        fuel_limit: Option<u64>,
        memory_limit_bytes: Option<usize>,
        execution_timeout: Option<Duration>,
    ) -> Self {
        Self {
            created_at: Instant::now(),
            fuel_limit,
            memory_limit_bytes,
            execution_timeout,
            instance_count: 0,
            is_closed: Arc::new(std::sync::atomic::AtomicBool::new(false)),
        }
    }
}

impl std::fmt::Debug for StoreData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let mut debug = f.debug_struct("StoreData");
        debug
            .field("user_data", &self.user_data.as_ref().map(|_| "<user_data>"))
            .field("resource_limits", &self.resource_limits)
            .field("execution_state", &self.execution_state)
            .field("wasi_ctx", &self.wasi_ctx.as_ref().map(|_| "<WasiP1Ctx>"))
            .field(
                "wasi_stdout_pipe",
                &self.wasi_stdout_pipe.as_ref().map(|_| "<pipe>"),
            )
            .field(
                "wasi_stderr_pipe",
                &self.wasi_stderr_pipe.as_ref().map(|_| "<pipe>"),
            )
            .field(
                "wasi_fd_manager",
                &self.wasi_fd_manager.as_ref().map(|_| "<fd_manager>"),
            );
        #[cfg(feature = "wasi-http")]
        debug
            .field(
                "wasi_http_ctx",
                &self.wasi_http_ctx.as_ref().map(|_| "<WasiHttpCtx>"),
            )
            .field("resource_table", &"<ResourceTable>");
        #[cfg(feature = "wasi-nn")]
        debug.field(
            "wasi_nn_ctx",
            &self.wasi_nn_ctx.as_ref().map(|_| "<WasiNnCtx>"),
        );
        debug.field(
            "wasmtime_store_limits",
            &self
                .wasmtime_store_limits
                .as_ref()
                .map(|_| "<WasmtimeStoreLimits>"),
        );
        debug.field(
            "callback_resource_limiter",
            &self
                .callback_resource_limiter
                .as_ref()
                .map(|_| "<CallbackResourceLimiter>"),
        );
        debug.field(
            "callback_resource_limiter_async",
            &self
                .callback_resource_limiter_async
                .as_ref()
                .map(|_| "<CallbackResourceLimiterAsync>"),
        );
        debug.finish()
    }
}

impl Clone for StoreData {
    fn clone(&self) -> Self {
        StoreData {
            store_id: self.store_id,
            user_data: None, // Can't clone arbitrary Any type, set to None
            resource_limits: self.resource_limits.clone(),
            execution_state: self.execution_state.clone(),
            wasi_ctx: None, // WasiP1Ctx doesn't implement Clone, each store needs its own
            wasi_stdout_pipe: self.wasi_stdout_pipe.clone(),
            wasi_stderr_pipe: self.wasi_stderr_pipe.clone(),
            wasi_fd_manager: None, // FD manager is store-specific
            #[cfg(feature = "wasi-http")]
            wasi_http_ctx: None, // WasiHttpCtx is store-specific
            #[cfg(feature = "wasi-http")]
            resource_table: ResourceTable::new(), // Fresh table for cloned store
            #[cfg(feature = "wasi-nn")]
            wasi_nn_ctx: None, // WasiNnCtx is store-specific
            wasmtime_store_limits: None, // Limiter is store-specific, must be re-registered
            callback_resource_limiter: None, // Limiter is store-specific, must be re-registered
            callback_resource_limiter_async: None, // Async limiter is store-specific
            epoch_interruption_enabled: self.epoch_interruption_enabled,
            async_enabled: self.async_enabled,
        }
    }
}

#[cfg(feature = "wasi-http")]
impl wasmtime_wasi_http::WasiHttpView for StoreData {
    fn ctx(&mut self) -> &mut WasiHttpCtx {
        self.wasi_http_ctx.get_or_insert_with(WasiHttpCtx::new)
    }

    fn table(&mut self) -> &mut ResourceTable {
        &mut self.resource_table
    }
}

/// Resource limits and quotas
#[derive(Debug, Clone)]
pub struct ResourceLimits {
    /// Maximum memory allocation in bytes
    pub max_memory_bytes: Option<usize>,
    /// Maximum number of table elements across all tables
    pub max_table_elements: Option<u32>,
    /// Maximum number of WebAssembly instances in this store
    pub max_instances: Option<usize>,
    /// Maximum number of tables that can be created
    pub max_tables: Option<usize>,
    /// Maximum number of memories that can be created
    pub max_memories: Option<usize>,
    /// Whether memory/table growth failures should trap instead of returning -1
    pub trap_on_grow_failure: bool,
}

/// Resource limiter backed by C function pointer callbacks.
///
/// Resource limiter backed by C function pointer callbacks.
///
/// Implements both wasmtime's `ResourceLimiter` and `ResourceLimiterAsync` traits by delegating
/// to extern "C" function pointers, allowing Java code (via JNI or Panama FFI) to make dynamic
/// resource allocation decisions at runtime.
///
/// The callbacks are synchronous from Rust's perspective — the async trait impl simply wraps
/// the same synchronous calls, since Java resolves them immediately.
#[derive(Clone, Copy)]
pub struct CallbackResourceLimiter {
    /// Callback invoked when linear memory is about to grow.
    /// Parameters: callback_id, current_bytes, desired_bytes, maximum_bytes
    /// Returns: non-zero to allow, zero to deny
    memory_growing_fn: extern "C" fn(i64, u64, u64, u64) -> i32,
    /// Callback invoked when a table is about to grow.
    /// Parameters: callback_id, current_elements, desired_elements, maximum_elements
    /// Returns: non-zero to allow, zero to deny
    table_growing_fn: extern "C" fn(i64, u32, u32, u32) -> i32,
    /// Optional callback invoked when a memory grow operation fails after being allowed.
    /// Parameters: callback_id, error_message (null-terminated C string)
    memory_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
    /// Optional callback invoked when a table grow operation fails after being allowed.
    /// Parameters: callback_id, error_message (null-terminated C string)
    table_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
    /// Identifier passed back to callbacks so the Java side can identify which limiter is being called.
    callback_id: i64,
}

impl CallbackResourceLimiter {
    fn do_memory_growing(
        &mut self,
        current: usize,
        desired: usize,
        maximum: Option<usize>,
    ) -> wasmtime::Result<bool> {
        let max_val = maximum.map(|m| m as u64).unwrap_or(u64::MAX);
        let result =
            (self.memory_growing_fn)(self.callback_id, current as u64, desired as u64, max_val);
        Ok(result != 0)
    }

    fn do_table_growing(
        &mut self,
        current: usize,
        desired: usize,
        maximum: Option<usize>,
    ) -> wasmtime::Result<bool> {
        let max_val = maximum.map(|m| m as u32).unwrap_or(u32::MAX);
        let result =
            (self.table_growing_fn)(self.callback_id, current as u32, desired as u32, max_val);
        Ok(result != 0)
    }

    fn do_memory_grow_failed(&mut self, error: wasmtime::Error) -> wasmtime::Result<()> {
        if let Some(callback) = self.memory_grow_failed_fn {
            if let Ok(c_msg) = CString::new(error.to_string()) {
                callback(self.callback_id, c_msg.as_ptr());
            }
        }
        Ok(())
    }

    fn do_table_grow_failed(&mut self, error: wasmtime::Error) -> wasmtime::Result<()> {
        if let Some(callback) = self.table_grow_failed_fn {
            if let Ok(c_msg) = CString::new(error.to_string()) {
                callback(self.callback_id, c_msg.as_ptr());
            }
        }
        Ok(())
    }
}

impl wasmtime::ResourceLimiter for CallbackResourceLimiter {
    fn memory_growing(
        &mut self,
        current: usize,
        desired: usize,
        maximum: Option<usize>,
    ) -> wasmtime::Result<bool> {
        self.do_memory_growing(current, desired, maximum)
    }

    fn table_growing(
        &mut self,
        current: usize,
        desired: usize,
        maximum: Option<usize>,
    ) -> wasmtime::Result<bool> {
        self.do_table_growing(current, desired, maximum)
    }

    fn memory_grow_failed(&mut self, error: wasmtime::Error) -> wasmtime::Result<()> {
        self.do_memory_grow_failed(error)
    }

    fn table_grow_failed(&mut self, error: wasmtime::Error) -> wasmtime::Result<()> {
        self.do_table_grow_failed(error)
    }
}

/// Type alias for async resource limiter — uses the same struct since callbacks are synchronous.
pub type CallbackResourceLimiterAsync = CallbackResourceLimiter;

#[async_trait::async_trait]
impl wasmtime::ResourceLimiterAsync for CallbackResourceLimiter {
    async fn memory_growing(
        &mut self,
        current: usize,
        desired: usize,
        maximum: Option<usize>,
    ) -> wasmtime::Result<bool> {
        self.do_memory_growing(current, desired, maximum)
    }

    async fn table_growing(
        &mut self,
        current: usize,
        desired: usize,
        maximum: Option<usize>,
    ) -> wasmtime::Result<bool> {
        self.do_table_growing(current, desired, maximum)
    }

    fn memory_grow_failed(&mut self, error: wasmtime::Error) -> wasmtime::Result<()> {
        self.do_memory_grow_failed(error)
    }

    fn table_grow_failed(&mut self, error: wasmtime::Error) -> wasmtime::Result<()> {
        self.do_table_grow_failed(error)
    }
}

/// Execution state tracking
#[derive(Debug, Clone)]
pub struct ExecutionState {
    /// Total number of function executions in this store
    pub execution_count: u64,
    /// Timestamp of the most recent execution
    pub last_execution: Option<Instant>,
    /// Cumulative time spent executing WebAssembly code
    pub total_execution_time: Duration,
    /// Total amount of fuel consumed by executions
    pub fuel_consumed: u64,
}

/// Builder for creating configured stores
#[derive(Debug)]
pub struct StoreBuilder {
    /// Optional fuel limit for execution metering
    fuel_limit: Option<u64>,
    /// Optional memory limit in bytes
    memory_limit_bytes: Option<usize>,
    /// Optional execution timeout
    execution_timeout: Option<Duration>,
    /// Resource limits configuration
    resource_limits: ResourceLimits,
}

impl Store {
    /// Create store with default configuration
    pub fn new(engine: &Engine) -> WasmtimeResult<Self> {
        StoreBuilder::new().build(engine)
    }

    /// Create store with default configuration, returning an error on allocation failure.
    ///
    /// Unlike `new`, this method uses `wasmtime::Store::try_new` internally, which
    /// returns an error instead of panicking if the store allocation fails (e.g., out of memory).
    pub fn try_new(engine: &Engine) -> WasmtimeResult<Self> {
        StoreBuilder::new().try_build(engine)
    }

    /// Create store that is compatible with the given module
    ///
    /// CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Arc
    /// as the Module's internal wasmtime::Module. This is required because wasmtime's
    /// Instance::new() uses Arc::ptr_eq() to verify engine compatibility.
    ///
    /// Use this method when creating a Store specifically for instantiating a Module.
    ///
    /// # Arguments
    /// * `module` - The Module that this Store will be used with for instantiation
    ///
    /// # Returns
    /// A new Store that is guaranteed to be compatible with the given Module
    pub fn for_module(module: &Module) -> WasmtimeResult<Self> {
        StoreBuilder::new().build_for_module(module)
    }

    /// Get the unique identifier for this store
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Check if this store has async support enabled.
    ///
    /// Async-enabled stores are required for Wasmtime's `*_async()` operations
    /// (e.g., `call_async`, `instantiate_async`). The async flag is inherited from
    /// the Engine's `async_support` configuration at store creation time.
    pub fn is_async(&self) -> bool {
        match self.try_lock_store() {
            Ok(guard) => guard.data().async_enabled,
            Err(_) => false,
        }
    }

    // ===== Debugging API =====

    /// Check if single-step mode is active.
    ///
    /// Returns false if guest debugging is not enabled or if the store is closed.
    pub fn is_single_step(&self) -> bool {
        match self.try_lock_store() {
            Ok(store) => store.is_single_step(),
            Err(_) => false,
        }
    }

    /// Get the number of active breakpoints.
    ///
    /// Returns None if guest debugging is not enabled, or the count otherwise.
    pub fn breakpoint_count(&self) -> WasmtimeResult<Option<usize>> {
        let store = self.try_lock_store()?;
        let result = {
            match store.breakpoints() {
                Some(iter) => Some(iter.count()),
                None => None,
            }
        };
        Ok(result)
    }

    /// Edit breakpoints on this store.
    ///
    /// This requires guest debugging to be enabled. The callback receives a
    /// BreakpointEdit which allows adding/removing breakpoints and toggling single-step.
    pub fn edit_breakpoints<F>(&self, editor_fn: F) -> WasmtimeResult<bool>
    where
        F: FnOnce(&mut wasmtime::BreakpointEdit<'_>),
    {
        let mut store = self.try_lock_store()?;
        let has_debug = {
            let maybe_edit = store.edit_breakpoints();
            if let Some(mut edit) = maybe_edit {
                editor_fn(&mut edit);
                true
            } else {
                false
            }
        };
        Ok(has_debug)
    }

    /// Snapshot all debug exit frames from the store.
    ///
    /// Returns None if guest debugging is not enabled, or an empty vec if no frames.
    /// Each frame is represented as [func_index, pc, num_locals, num_stacks].
    pub fn debug_exit_frames(&self) -> WasmtimeResult<Option<Vec<[i32; 4]>>> {
        let mut store = self.try_lock_store()?;
        let cursor = store.debug_frames();
        let Some(mut cursor) = cursor else {
            return Ok(None);
        };
        let mut frames = Vec::new();
        loop {
            cursor.move_to_parent();
            if cursor.done() {
                break;
            }
            let (func_index, pc) = cursor
                .wasm_function_index_and_pc()
                .map(|(fi, pc)| (fi.as_u32() as i32, pc as i32))
                .unwrap_or((-1, -1));
            let num_locals = cursor.num_locals() as i32;
            let num_stacks = cursor.num_stacks() as i32;
            frames.push([func_index, pc, num_locals, num_stacks]);
        }
        Ok(Some(frames))
    }

    /// Check if the store has been closed.
    ///
    /// Returns an error if the store is closed, preventing use-after-close bugs.
    /// This should be called at the start of any method that accesses self.inner.
    #[inline]
    fn check_not_closed(&self) -> WasmtimeResult<()> {
        if self
            .metadata
            .is_closed
            .load(std::sync::atomic::Ordering::SeqCst)
        {
            return Err(WasmtimeError::Store {
                message: format!("Store {} has been closed and cannot be used", self.id),
            });
        }
        Ok(())
    }

    /// Check if this store has been closed
    pub fn is_closed(&self) -> bool {
        self.metadata
            .is_closed
            .load(std::sync::atomic::Ordering::SeqCst)
    }

    /// Get a reference to the engine used by this store
    ///
    /// This is needed for creating FuncTypes in FFI bindings
    pub(crate) fn engine(&self) -> &Engine {
        &self.engine
    }

    /// Create store builder for custom configuration
    pub fn builder() -> StoreBuilder {
        StoreBuilder::new()
    }

    /// Get direct mutable access to the underlying Store
    ///
    /// This provides direct access to the WasmtimeStore without closure-based
    /// context wrapping. Uses custom ReentrantLock to allow same-thread reentrant access.
    ///
    /// CRITICAL: This method is essential for avoiding WASM execution traps.
    /// The ReentrantLock allows Wasmtime to access the Store multiple times
    /// during function execution on the same thread.
    ///
    /// # Panics
    /// Panics if the store has been closed, to prevent use-after-free crashes.
    pub fn lock_store(&self) -> crate::interop::ReentrantLockGuard<'_, WasmtimeStore<StoreData>> {
        if self
            .metadata
            .is_closed
            .load(std::sync::atomic::Ordering::SeqCst)
        {
            panic!("Store {} has been closed and cannot be used", self.id);
        }
        self.inner.lock()
    }

    /// Try to get direct mutable access to the underlying Store
    ///
    /// Returns an error if the store has been closed.
    /// Use this method when you need to handle the closed case gracefully.
    pub fn try_lock_store(
        &self,
    ) -> WasmtimeResult<crate::interop::ReentrantLockGuard<'_, WasmtimeStore<StoreData>>> {
        self.check_not_closed()?;
        Ok(self.inner.lock())
    }

    /// Execute function with store context
    ///
    /// NOTE: This method creates temporary context lifetimes and should NOT be
    /// used for WASM function calls in JNI/Panama environments. Use `lock_store()`
    /// instead for direct Store access.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn with_context<T, F>(&self, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&mut StoreContextMut<StoreData>) -> WasmtimeResult<T>,
    {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        let start_time = Instant::now();

        // Check timeout before execution
        if let Some(timeout) = self.metadata.execution_timeout {
            let elapsed = start_time.duration_since(self.metadata.created_at);
            if elapsed > timeout {
                return Err(WasmtimeError::Runtime {
                    message: "Store execution timeout exceeded".to_string(),
                    backtrace: None,
                });
            }
        }

        let result = func(&mut store.as_context_mut());

        // Update execution statistics
        let execution_time = start_time.elapsed();
        store.data_mut().execution_state.execution_count += 1;
        store.data_mut().execution_state.last_execution = Some(start_time);
        store.data_mut().execution_state.total_execution_time += execution_time;

        result
    }

    /// Execute function with read-only store context
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn with_context_ro<T, F>(&self, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&StoreContext<StoreData>) -> WasmtimeResult<T>,
    {
        self.check_not_closed()?;
        let store = self.inner.lock();
        func(&store.as_context())
    }

    /// Get store metadata
    pub fn metadata(&self) -> &StoreMetadata {
        &self.metadata
    }

    /// Add fuel to the store for execution limiting
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn add_fuel(&self, fuel: u64) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        // Get current fuel and add to it
        let current_fuel = store.get_fuel().unwrap_or(0);
        let new_fuel = current_fuel.saturating_add(fuel);

        store
            .set_fuel(new_fuel)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to add fuel: {}", e),
                backtrace: None,
            })?;

        Ok(())
    }

    /// Set fuel to a specific amount (replaces current fuel)
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_fuel(&self, fuel: u64) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        store.set_fuel(fuel).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to set fuel: {}", e),
            backtrace: None,
        })?;

        Ok(())
    }

    /// Get remaining fuel
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn fuel_remaining(&self) -> WasmtimeResult<Option<u64>> {
        self.check_not_closed()?;
        let store = self.inner.lock();

        Ok(Some(store.get_fuel().unwrap_or(0)))
    }

    /// Consume fuel from the store
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn consume_fuel(&self, fuel: u64) -> WasmtimeResult<u64> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        let current_fuel = store.get_fuel().unwrap_or(0);
        if current_fuel < fuel {
            return Err(WasmtimeError::Runtime {
                message: format!(
                    "Insufficient fuel: requested {} but only {} available",
                    fuel, current_fuel
                ),
                backtrace: None,
            });
        }

        let remaining = current_fuel - fuel;
        store
            .set_fuel(remaining)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to consume fuel: {}", e),
                backtrace: None,
            })?;
        Ok(remaining)
    }

    /// Set the fuel async yield interval
    ///
    /// Configures how often async WebAssembly execution should yield based on
    /// fuel consumption. A value of 0 disables this feature (maps to None).
    /// Silently succeeds if async support is not enabled on the engine, since
    /// the value is tracked Java-side for configuration purposes.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_fuel_async_yield_interval(&self, interval: u64) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        let interval_opt = if interval == 0 { None } else { Some(interval) };
        store
            .fuel_async_yield_interval(interval_opt)
            .map_err(|e| WasmtimeError::Store {
                message: format!("Failed to set fuel async yield interval: {}", e),
            })
    }

    /// Get the hostcall fuel limit.
    ///
    /// Returns the configured amount of "hostcall fuel" for guest-to-host
    /// component calls, either the default value (128 MiB) or the last value
    /// set via `set_hostcall_fuel`.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn hostcall_fuel(&self) -> WasmtimeResult<usize> {
        self.check_not_closed()?;
        let store = self.inner.lock();
        Ok(store.hostcall_fuel())
    }

    /// Set the hostcall fuel limit.
    ///
    /// Configures the fuel limit for data transfers during guest-to-host
    /// component calls. The fuel value roughly corresponds to the maximum
    /// number of bytes a guest may transfer to the host in a single call,
    /// serving as a denial-of-service mitigation mechanism.
    /// The default is 128 MiB.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_hostcall_fuel(&self, fuel: usize) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        store.set_hostcall_fuel(fuel);
        Ok(())
    }

    /// Set epoch deadline for interruption
    ///
    /// # Panics
    /// This method will silently fail if the store has been closed.
    /// Use `set_epoch_deadline_checked` for explicit error handling.
    pub fn set_epoch_deadline(&self, ticks: u64) {
        if self.check_not_closed().is_err() {
            return;
        }
        let mut store = self.inner.lock();
        store.set_epoch_deadline(ticks);
    }

    /// Get execution statistics
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn execution_stats(&self) -> WasmtimeResult<ExecutionState> {
        self.check_not_closed()?;
        let store = self.inner.lock();

        Ok(store.data().execution_state.clone())
    }

    /// Validate store is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Check if store has been closed
        self.check_not_closed()?;

        // Check if store can be locked (not corrupted)
        if let Some(_guard) = self.inner.try_lock() {
            Ok(())
        } else {
            Err(WasmtimeError::Concurrency {
                message: "Store is locked and may be corrupted".to_string(),
            })
        }
    }

    /// Force garbage collection in the store
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn gc(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        let _ = store.gc(None); // Pass None for manual GC trigger
        Ok(())
    }

    /// Perform garbage collection asynchronously.
    ///
    /// This spawns a Tokio task that calls gc_async on the Wasmtime store,
    /// cooperatively yielding during collection if async yielding is configured.
    pub async fn gc_async(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        store.gc_async(None).await;
        Ok(())
    }

    /// Check if there is a pending exception in the store
    ///
    /// Returns false if the store has been closed.
    pub fn has_pending_exception(&self) -> bool {
        if self.check_not_closed().is_err() {
            return false;
        }
        let store = self.inner.lock();
        store.has_pending_exception()
    }

    /// Take and remove the pending exception from the store, if any.
    /// Returns the exception as an OwnedRooted handle that can be stored across FFI boundaries.
    ///
    /// Returns None if the store has been closed.
    pub fn take_pending_exception(&self) -> Option<wasmtime::OwnedRooted<wasmtime::ExnRef>> {
        if self.check_not_closed().is_err() {
            return None;
        }
        let mut store = self.inner.lock();
        store.take_pending_exception().and_then(|exn| {
            let mut scope = wasmtime::RootScope::new(&mut *store);
            exn.to_owned_rooted(&mut scope).ok()
        })
    }

    /// Set epoch deadline with async yield and update
    ///
    /// Configures the store to yield when the epoch deadline is reached during
    /// async execution, then automatically update the deadline by the given delta.
    ///
    /// # Arguments
    /// * `delta` - The number of ticks to add to the deadline after yielding
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn epoch_deadline_async_yield_and_update(&self, delta: u64) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        store.epoch_deadline_async_yield_and_update(delta);
        Ok(())
    }

    /// Configure the store to trap when the epoch deadline is reached
    ///
    /// When the epoch counter exceeds the deadline set via `set_epoch_deadline`,
    /// WebAssembly execution will trap with an error.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn epoch_deadline_trap(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        store.epoch_deadline_trap();
        Ok(())
    }

    /// Configure an epoch deadline callback with a function pointer.
    ///
    /// When the epoch counter exceeds the deadline, the provided callback
    /// function will be invoked to determine how to proceed.
    ///
    /// # Arguments
    /// * `callback_fn` - A C-compatible function pointer that takes a callback ID and epoch,
    ///                   and returns:
    ///                   - Positive value: continue execution with that many ticks added to deadline
    ///                   - Negative value: trap execution
    /// * `callback_id` - An ID to pass to the callback function (used to identify the Java callback)
    ///
    /// # Safety
    /// The callback function must be valid for the lifetime of the Store.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn epoch_deadline_callback_with_fn(
        &self,
        callback_fn: extern "C" fn(callback_id: i64, epoch: u64) -> i64,
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        log::debug!(
            "Registering epoch deadline callback: callback_id={}, fn_ptr={:?}",
            callback_id,
            callback_fn as *const ()
        );

        // Configure the Wasmtime epoch deadline callback
        // The callback receives StoreContextMut and returns Result<UpdateDeadline, Error>
        store.epoch_deadline_callback(move |_store_ctx| {
            log::trace!(
                "Epoch deadline reached, invoking callback: callback_id={}",
                callback_id
            );

            // Invoke the external callback
            let result = callback_fn(callback_id, 0); // epoch counter not easily accessible here

            log::trace!(
                "Epoch callback returned: result={}, callback_id={}",
                result,
                callback_id
            );

            if result > 0 {
                // Continue execution with the returned delta
                log::trace!("Continuing execution with delta={}", result);
                Ok(wasmtime::UpdateDeadline::Continue(result as u64))
            } else if result < 0 {
                // Yield execution with the negated value as delta ticks
                let delta = (-result) as u64;
                log::trace!("Yielding execution with delta={}", delta);
                Ok(wasmtime::UpdateDeadline::Yield(delta))
            } else {
                // Return an error to trap execution
                log::trace!("Trapping execution");
                Err(wasmtime::Error::msg(
                    "Epoch deadline callback requested trap",
                ))
            }
        });

        Ok(())
    }

    /// Sets a debug handler via an FFI callback function.
    ///
    /// The callback function receives a callback_id and an event code:
    /// - 0 = HostcallError
    /// - 1 = CaughtExceptionThrown
    /// - 2 = UncaughtExceptionThrown
    /// - 3 = Trap
    /// - 4 = Breakpoint
    /// - 5 = EpochYield
    ///
    /// # Safety
    /// The callback function must be valid for the lifetime of the Store.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_debug_handler_with_fn(
        &self,
        callback_fn: extern "C" fn(callback_id: i64, event_code: i32),
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        let handler = FfiDebugHandler {
            callback_fn,
            callback_id,
        };
        store.set_debug_handler(handler);
        Ok(())
    }

    /// Clears the debug handler.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn clear_debug_handler(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        store.clear_debug_handler();
        Ok(())
    }

    /// Get memory usage statistics
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn memory_usage(&self) -> WasmtimeResult<MemoryUsage> {
        self.check_not_closed()?;
        let store = self.inner.lock();

        Ok(MemoryUsage {
            execution_count: store.data().execution_state.execution_count as usize,
        })
    }

    /// Create a host function that can be imported by WebAssembly modules
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn create_host_function(
        &self,
        name: String,
        func_type: FuncType,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
    ) -> WasmtimeResult<(u64, Func)> {
        self.check_not_closed()?;
        // Create the host function wrapper
        let host_function = HostFunction::new(name, func_type, callback)?;
        let host_function_id = host_function.id();

        // Create the Wasmtime Func
        let wasmtime_func = {
            let mut store = self.inner.lock();
            host_function.create_wasmtime_func(&mut store)?
        };

        Ok((host_function_id, wasmtime_func))
    }

    /// Create an unchecked host function that skips per-call type validation
    ///
    /// This is identical to `create_host_function` but uses `Func::new_unchecked`
    /// internally, which skips Wasmtime's per-call type validation for better performance.
    /// The caller is responsible for ensuring type correctness.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn create_host_function_unchecked(
        &self,
        name: String,
        func_type: FuncType,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
    ) -> WasmtimeResult<(u64, Func)> {
        self.check_not_closed()?;
        let host_function = HostFunction::new(name, func_type, callback)?;
        let host_function_id = host_function.id();

        let wasmtime_func = {
            let mut store = self.inner.lock();
            host_function.create_wasmtime_func_unchecked(&mut store)?
        };

        Ok((host_function_id, wasmtime_func))
    }

    /// Create a function reference from a host function
    /// Returns only the handle ID - the FunctionReference wrapper is created on the Java side
    pub fn create_function_reference(
        &self,
        name: String,
        func_type: FuncType,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
    ) -> WasmtimeResult<u64> {
        let (_host_id, func) = self.create_host_function(name, func_type, callback)?;

        // Register the function in the table reference registry so it can be
        // looked up when stored in globals and used with call_indirect
        // Return the registry ID, not the host function ID
        use crate::table::core::register_function_reference;
        let registry_id = register_function_reference(func, self.id)?;

        Ok(registry_id)
    }

    /// Set WASI context for this store by building a fresh WasiP1Ctx from configuration
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_wasi_context(
        &self,
        wasi_context: &crate::wasi::WasiContext,
        fd_manager: crate::wasi::WasiFileDescriptorManager,
    ) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        // Build a fresh WasiP1Ctx from the configuration
        let (wasi_ctx, stdout_pipe, stderr_pipe) = wasi_context.build_fresh_p1_ctx()?;

        // Store directly in StoreData
        let data = store.data_mut();
        data.wasi_ctx = Some(wasi_ctx);
        data.wasi_stdout_pipe = stdout_pipe;
        data.wasi_stderr_pipe = stderr_pipe;
        data.wasi_fd_manager = Some(fd_manager);

        Ok(())
    }

    /// Check if this store has WASI context
    ///
    /// Returns false if the store has been closed.
    pub fn has_wasi_context(&self) -> bool {
        if self.check_not_closed().is_err() {
            return false;
        }
        let store = self.inner.lock();
        store.data().wasi_ctx.is_some()
    }

    /// Set WASI-NN context for this store to enable neural network inference
    ///
    /// This creates a WasiNnCtx with the available backends and an empty registry.
    /// WebAssembly modules can then use wasi-nn imports to load models and run inference.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    #[cfg(feature = "wasi-nn")]
    pub fn set_wasi_nn_context(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        // Get available backends from wasmtime-wasi-nn
        let backends = wasmtime_wasi_nn::backend::list();
        let registry = wasmtime_wasi_nn::InMemoryRegistry::new();

        // Create the WasiNnCtx
        let wasi_nn_ctx = WasiNnCtx::new(backends, registry.into());

        // Store in StoreData
        let data = store.data_mut();
        data.wasi_nn_ctx = Some(wasi_nn_ctx);

        log::debug!("WASI-NN context set on store");
        Ok(())
    }

    /// Check if this store has WASI-NN context
    ///
    /// Returns false if the store has been closed.
    #[cfg(feature = "wasi-nn")]
    pub fn has_wasi_nn_context(&self) -> bool {
        if self.check_not_closed().is_err() {
            return false;
        }
        let store = self.inner.lock();
        store.data().wasi_nn_ctx.is_some()
    }

    /// Remove WASI-NN context from this store
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    #[cfg(feature = "wasi-nn")]
    pub fn remove_wasi_nn_context(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();
        let data = store.data_mut();
        data.wasi_nn_ctx = None;
        Ok(())
    }

    /// Get captured stdout data from WASI execution
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn get_wasi_stdout(&self) -> WasmtimeResult<Option<Vec<u8>>> {
        self.check_not_closed()?;
        let store = self.inner.lock();
        if let Some(pipe) = &store.data().wasi_stdout_pipe {
            let contents = pipe.contents();
            Ok(Some(contents.to_vec()))
        } else {
            Ok(None)
        }
    }

    /// Get captured stderr data from WASI execution
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn get_wasi_stderr(&self) -> WasmtimeResult<Option<Vec<u8>>> {
        self.check_not_closed()?;
        let store = self.inner.lock();
        if let Some(pipe) = &store.data().wasi_stderr_pipe {
            let contents = pipe.contents();
            Ok(Some(contents.to_vec()))
        } else {
            Ok(None)
        }
    }

    /// Remove WASI context from this store
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn remove_wasi_context(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        let data = store.data_mut();
        data.wasi_ctx = None;
        data.wasi_stdout_pipe = None;
        data.wasi_stderr_pipe = None;
        data.wasi_fd_manager = None;
        Ok(())
    }

    /// Set a callback-based resource limiter on this store.
    ///
    /// The limiter's callbacks will be invoked each time a WebAssembly linear memory
    /// or table needs to grow, allowing dynamic resource allocation decisions.
    ///
    /// # Arguments
    /// * `memory_growing_fn` - Callback for memory grow requests
    /// * `table_growing_fn` - Callback for table grow requests
    /// * `memory_grow_failed_fn` - Optional callback for memory grow failures
    /// * `table_grow_failed_fn` - Optional callback for table grow failures
    /// * `callback_id` - Identifier passed to callbacks for Java-side dispatch
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_resource_limiter(
        &self,
        memory_growing_fn: extern "C" fn(i64, u64, u64, u64) -> i32,
        table_growing_fn: extern "C" fn(i64, u32, u32, u32) -> i32,
        memory_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
        table_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        log::debug!(
            "Setting resource limiter on store {}: callback_id={}",
            self.id,
            callback_id
        );

        store.data_mut().callback_resource_limiter = Some(CallbackResourceLimiter {
            memory_growing_fn,
            table_growing_fn,
            memory_grow_failed_fn,
            table_grow_failed_fn,
            callback_id,
        });

        store.limiter(|data| {
            data.callback_resource_limiter
                .as_mut()
                .expect("CallbackResourceLimiter was set but is missing")
        });

        Ok(())
    }

    /// Sets an async resource limiter on this store.
    ///
    /// Requires the engine to be configured with `async_support(true)`.
    /// Uses `Store::limiter_async()` instead of `Store::limiter()`.
    ///
    /// # Arguments
    /// Same as `set_resource_limiter` but registers with the async limiter path.
    pub fn set_resource_limiter_async(
        &self,
        memory_growing_fn: extern "C" fn(i64, u64, u64, u64) -> i32,
        table_growing_fn: extern "C" fn(i64, u32, u32, u32) -> i32,
        memory_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
        table_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        log::debug!(
            "Setting async resource limiter on store {}: callback_id={}",
            self.id,
            callback_id
        );

        store.data_mut().callback_resource_limiter_async = Some(CallbackResourceLimiterAsync {
            memory_growing_fn,
            table_growing_fn,
            memory_grow_failed_fn,
            table_grow_failed_fn,
            callback_id,
        });

        store.limiter_async(|data| {
            data.callback_resource_limiter_async
                .as_mut()
                .expect("CallbackResourceLimiterAsync was set but is missing")
        });

        Ok(())
    }

    /// Set a call hook on the store.
    ///
    /// This installs a no-op call hook on the underlying wasmtime Store,
    /// enabling the call hook machinery. The actual callback dispatch happens
    /// on the Java side.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_call_hook(&self) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        log::debug!("Setting call hook on store {}", self.id);

        store.call_hook(|_ctx, _hook| Ok(()));

        Ok(())
    }

    /// Clear the call hook on the store.
    ///
    /// Replaces the active call hook with a no-op, effectively disabling it.
    /// Wasmtime does not provide a way to remove a call hook entirely, so this
    /// installs a trivial no-op hook instead.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn clear_call_hook(&self) -> WasmtimeResult<()> {
        log::debug!("Clearing call hook on store {}", self.id);
        self.set_call_hook()
    }

    /// Set a call hook with a function pointer callback.
    ///
    /// When WebAssembly transitions between host and wasm code, the provided
    /// callback function will be invoked with the callback ID and hook type.
    ///
    /// # Arguments
    /// * `callback_fn` - A C-compatible function pointer that takes a callback ID and hook type,
    ///                   and returns 0 for OK or non-zero to trap.
    /// * `callback_id` - An ID to pass to the callback function (used to identify the Java callback)
    ///
    /// Hook type values:
    /// * 0 = CallingWasm
    /// * 1 = ReturningFromWasm
    /// * 2 = CallingHost
    /// * 3 = ReturningFromHost
    ///
    /// # Safety
    /// The callback function must be valid for the lifetime of the Store.
    ///
    /// # Errors
    /// Returns an error if the store has been closed.
    pub fn set_call_hook_with_fn(
        &self,
        callback_fn: extern "C" fn(callback_id: i64, hook_type: i32) -> i32,
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        self.check_not_closed()?;
        let mut store = self.inner.lock();

        log::debug!(
            "Registering call hook callback: callback_id={}, fn_ptr={:?}",
            callback_id,
            callback_fn as *const ()
        );

        store.call_hook(move |_ctx, hook| {
            let hook_type = match hook {
                CallHook::CallingWasm => 0,
                CallHook::ReturningFromWasm => 1,
                CallHook::CallingHost => 2,
                CallHook::ReturningFromHost => 3,
            };

            log::trace!(
                "Call hook fired: callback_id={}, hook_type={}",
                callback_id,
                hook_type
            );

            let result = callback_fn(callback_id, hook_type);
            if result == 0 {
                Ok(())
            } else {
                Err(wasmtime::Error::msg(format!(
                    "Call hook requested trap (result={})",
                    result
                )))
            }
        });

        Ok(())
    }
}

/// Build a `wasmtime::StoreLimits` from our `ResourceLimits` and apply it to the store
/// via `store.limiter()`. This ensures configured limits are actually enforced by Wasmtime.
fn apply_resource_limits(
    wasmtime_store: &mut WasmtimeStore<StoreData>,
    resource_limits: &ResourceLimits,
) {
    let has_limits = resource_limits.max_memory_bytes.is_some()
        || resource_limits.max_table_elements.is_some()
        || resource_limits.max_instances.is_some()
        || resource_limits.max_tables.is_some()
        || resource_limits.max_memories.is_some()
        || resource_limits.trap_on_grow_failure;

    if has_limits {
        let mut builder = WasmtimeStoreLimitsBuilder::new();
        if let Some(mem) = resource_limits.max_memory_bytes {
            builder = builder.memory_size(mem);
        }
        if let Some(elements) = resource_limits.max_table_elements {
            builder = builder.table_elements(elements as usize);
        }
        if let Some(instances) = resource_limits.max_instances {
            builder = builder.instances(instances);
        }
        if let Some(tables) = resource_limits.max_tables {
            builder = builder.tables(tables);
        }
        if let Some(memories) = resource_limits.max_memories {
            builder = builder.memories(memories);
        }
        if resource_limits.trap_on_grow_failure {
            builder = builder.trap_on_grow_failure(true);
        }

        let wt_limits = builder.build();
        wasmtime_store.data_mut().wasmtime_store_limits = Some(wt_limits);
        wasmtime_store.limiter(|data| {
            data.wasmtime_store_limits
                .as_mut()
                .expect("wasmtime_store_limits was set but is missing")
        });
    }
}

impl StoreBuilder {
    /// Create new store builder
    fn new() -> Self {
        StoreBuilder {
            fuel_limit: None,
            memory_limit_bytes: None,
            execution_timeout: None,
            resource_limits: ResourceLimits {
                max_memory_bytes: None,
                max_table_elements: None,
                max_instances: None,
                max_tables: None,
                max_memories: None,
                trap_on_grow_failure: false,
            },
        }
    }

    /// Set fuel limit for execution metering
    pub fn fuel_limit(mut self, limit: u64) -> Self {
        self.fuel_limit = Some(limit);
        self
    }

    /// Set memory limit in bytes
    pub fn memory_limit(mut self, limit: usize) -> Self {
        self.memory_limit_bytes = Some(limit);
        self.resource_limits.max_memory_bytes = Some(limit);
        self
    }

    /// Set execution timeout
    pub fn execution_timeout(mut self, timeout: Duration) -> Self {
        self.execution_timeout = Some(timeout);
        self
    }

    /// Set maximum number of instances
    pub fn max_instances(mut self, limit: usize) -> Self {
        self.resource_limits.max_instances = Some(limit);
        self
    }

    /// Set maximum number of table elements
    pub fn max_table_elements(mut self, limit: u32) -> Self {
        self.resource_limits.max_table_elements = Some(limit);
        self
    }

    /// Set maximum number of tables
    pub fn max_tables(mut self, limit: usize) -> Self {
        self.resource_limits.max_tables = Some(limit);
        self
    }

    /// Set maximum number of memories
    pub fn max_memories(mut self, limit: usize) -> Self {
        self.resource_limits.max_memories = Some(limit);
        self
    }

    /// Set whether growth failures should trap
    pub fn trap_on_grow_failure(mut self, trap: bool) -> Self {
        self.resource_limits.trap_on_grow_failure = trap;
        self
    }

    /// Build store with current configuration
    pub fn build(self, engine: &Engine) -> WasmtimeResult<Store> {
        engine.validate()?;

        // Acquire compile lock to prevent race conditions during concurrent store creation.
        // This is critical when multiple threads create stores from the same engine,
        // especially when using shared memory features.
        let _compile_guard = engine.acquire_compile_lock();

        // Generate unique store ID (lock-free)
        let store_id = STORE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        let resource_limits = self.resource_limits.clone();
        let mut store_data = StoreData::new(store_id, self.resource_limits);
        store_data.epoch_interruption_enabled = engine.epoch_interruption_enabled();
        store_data.async_enabled = engine.async_support_enabled();
        let mut wasmtime_store = WasmtimeStore::new(engine.inner(), store_data);

        // Apply resource limits via Wasmtime's StoreLimits if any are configured
        apply_resource_limits(&mut wasmtime_store, &resource_limits);

        // Configure fuel if specified OR if Engine requires it
        if engine.fuel_enabled() {
            let fuel = self.fuel_limit.unwrap_or(u64::MAX);
            wasmtime_store
                .set_fuel(fuel)
                .map_err(|e| WasmtimeError::Store {
                    message: format!("Failed to set initial fuel: {}", e),
                })?;
        }

        let metadata = StoreMetadata::new(
            self.fuel_limit,
            self.memory_limit_bytes,
            self.execution_timeout,
        );

        Ok(Store {
            id: store_id,
            inner: Arc::new(ReentrantLock::new(wasmtime_store)),
            engine: engine.clone(),
            metadata,
        })
    }

    /// Build store with current configuration using try_new for OOM-safe allocation.
    ///
    /// Unlike `build`, this method uses `wasmtime::Store::try_new` internally, which
    /// returns an error instead of panicking if the store allocation fails.
    pub fn try_build(self, engine: &Engine) -> WasmtimeResult<Store> {
        engine.validate()?;

        let _compile_guard = engine.acquire_compile_lock();

        let store_id = STORE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        let resource_limits = self.resource_limits.clone();
        let mut store_data = StoreData::new(store_id, self.resource_limits);
        store_data.epoch_interruption_enabled = engine.epoch_interruption_enabled();
        store_data.async_enabled = engine.async_support_enabled();
        let mut wasmtime_store =
            WasmtimeStore::try_new(engine.inner(), store_data).map_err(|e| {
                WasmtimeError::Store {
                    message: format!("Failed to allocate store: {}", e),
                }
            })?;

        apply_resource_limits(&mut wasmtime_store, &resource_limits);

        if engine.fuel_enabled() {
            let fuel = self.fuel_limit.unwrap_or(u64::MAX);
            wasmtime_store
                .set_fuel(fuel)
                .map_err(|e| WasmtimeError::Store {
                    message: format!("Failed to set initial fuel: {}", e),
                })?;
        }

        let metadata = StoreMetadata::new(
            self.fuel_limit,
            self.memory_limit_bytes,
            self.execution_timeout,
        );

        Ok(Store {
            id: store_id,
            inner: Arc::new(ReentrantLock::new(wasmtime_store)),
            engine: engine.clone(),
            metadata,
        })
    }

    /// Build store using the wasmtime Engine from a Module
    ///
    /// CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Arc
    /// as the Module's internal wasmtime::Module. This is required because wasmtime's
    /// Instance::new() uses Arc::ptr_eq() to verify engine compatibility.
    ///
    /// # Arguments
    /// * `module` - The Module that this Store will be used with for instantiation
    ///
    /// # Returns
    /// A new Store that is guaranteed to be compatible with the given Module
    ///
    /// # Why This Method Exists
    ///
    /// When you call:
    /// - `Module::compile(engine, bytes)` - wasmtime internally clones the engine Arc
    /// - `Store::new(engine)` - wasmtime internally clones the engine Arc again
    ///
    /// Even though both use the same wasmtime4j Engine, the internal Arc pointers differ.
    /// This method uses `module.inner().engine()` to get the EXACT Arc that wasmtime
    /// is using internally, ensuring Arc::ptr_eq() succeeds during instantiation.
    pub fn build_for_module(self, module: &Module) -> WasmtimeResult<Store> {
        // Acquire compile lock to prevent race conditions during concurrent store creation.
        // This is critical when multiple threads create stores from the same engine,
        // especially when using shared memory features.
        let _compile_guard = module.engine().acquire_compile_lock();

        // Generate unique store ID (lock-free)
        let store_id = STORE_ID_COUNTER.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        let resource_limits = self.resource_limits.clone();
        let mut store_data = StoreData::new(store_id, self.resource_limits);
        store_data.epoch_interruption_enabled = module.engine().epoch_interruption_enabled();
        store_data.async_enabled = module.engine().async_support_enabled();

        // CRITICAL: Use the wasmtime Engine from the Module's internal wasmtime::Module
        // This ensures the Store's internal Arc matches the Module's internal Arc
        let wasmtime_engine = module.wasmtime_engine();
        let mut wasmtime_store = WasmtimeStore::new(wasmtime_engine, store_data);

        // Apply resource limits via Wasmtime's StoreLimits if any are configured
        apply_resource_limits(&mut wasmtime_store, &resource_limits);

        // Configure fuel if the engine has it enabled
        // Check via the module's engine reference
        if module.engine().fuel_enabled() {
            let fuel = self.fuel_limit.unwrap_or(u64::MAX);
            wasmtime_store
                .set_fuel(fuel)
                .map_err(|e| WasmtimeError::Store {
                    message: format!("Failed to set initial fuel: {}", e),
                })?;
        }

        let metadata = StoreMetadata::new(
            self.fuel_limit,
            self.memory_limit_bytes,
            self.execution_timeout,
        );

        Ok(Store {
            id: store_id,
            inner: Arc::new(ReentrantLock::new(wasmtime_store)),
            engine: module.engine().clone(),
            metadata,
        })
    }
}

/// Memory usage statistics for a store.
///
/// Note: Wasmtime does not expose per-store memory aggregation, so only
/// `execution_count` is tracked. The removed `total_bytes` and `used_bytes`
/// fields always returned 0.
#[derive(Debug, Clone)]
pub struct MemoryUsage {
    /// Number of executions performed in this store
    pub execution_count: usize,
}

impl Default for ResourceLimits {
    fn default() -> Self {
        ResourceLimits {
            max_memory_bytes: Some(64 * 1024 * 1024), // 64MB default
            max_table_elements: Some(10000),
            max_instances: Some(100),
            max_tables: None,
            max_memories: None,
            trap_on_grow_failure: false,
        }
    }
}

impl Default for ExecutionState {
    fn default() -> Self {
        ExecutionState {
            execution_count: 0,
            last_execution: None,
            total_execution_time: Duration::new(0, 0),
            fuel_consumed: 0,
        }
    }
}

// Thread safety: Store uses Arc<Mutex<WasmtimeStore>> internally
unsafe impl Send for Store {}
unsafe impl Sync for Store {}

impl Drop for Store {
    fn drop(&mut self) {
        log::debug!("Store {} dropped", self.id);
    }
}

/// Shared core functions for store operations used by both JNI and Panama interfaces
///
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use crate::engine::Engine;
    use crate::validate_ptr_not_null;
    use std::os::raw::c_void;
    use std::time::Duration;

    /// Core function to create a new store with default configuration
    pub fn create_store(engine: &Engine) -> WasmtimeResult<Box<Store>> {
        let store = Store::new(engine)?;
        Ok(Box::new(store))
    }

    /// Core function to create a store with custom configuration
    pub fn create_store_with_config(
        engine: &Engine,
        fuel_limit: Option<u64>,
        memory_limit_bytes: Option<usize>,
        execution_timeout_secs: Option<u64>,
        max_instances: Option<usize>,
        max_table_elements: Option<u32>,
        max_tables: Option<usize>,
        max_memories: Option<usize>,
        trap_on_grow_failure: bool,
    ) -> WasmtimeResult<Box<Store>> {
        let mut builder = Store::builder();

        if let Some(fuel) = fuel_limit {
            builder = builder.fuel_limit(fuel);
        }

        if let Some(memory) = memory_limit_bytes {
            builder = builder.memory_limit(memory);
        }

        if let Some(timeout_secs) = execution_timeout_secs {
            builder = builder.execution_timeout(Duration::from_secs(timeout_secs));
        }

        if let Some(instances) = max_instances {
            builder = builder.max_instances(instances);
        }

        if let Some(table_elements) = max_table_elements {
            builder = builder.max_table_elements(table_elements);
        }

        if let Some(tables) = max_tables {
            builder = builder.max_tables(tables);
        }

        if let Some(memories) = max_memories {
            builder = builder.max_memories(memories);
        }

        if trap_on_grow_failure {
            builder = builder.trap_on_grow_failure(true);
        }

        builder.build(engine).map(Box::new)
    }

    /// Core function to create a store with OOM-safe allocation
    ///
    /// Unlike `create_store`, this returns an error instead of panicking on allocation failure.
    pub fn try_create_store(engine: &Engine) -> WasmtimeResult<Box<Store>> {
        let store = Store::try_new(engine)?;
        Ok(Box::new(store))
    }

    /// Core function to create a store compatible with a specific module
    ///
    /// CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Arc
    /// as the Module's internal wasmtime::Module. This is required because wasmtime's
    /// Instance::new() uses Arc::ptr_eq() to verify engine compatibility.
    pub fn create_store_for_module(module: &Module) -> WasmtimeResult<Box<Store>> {
        let store = Store::for_module(module)?;
        Ok(Box::new(store))
    }

    /// Core function to validate store pointer and get reference
    pub unsafe fn get_store_ref(store_ptr: *const c_void) -> WasmtimeResult<&'static Store> {
        validate_ptr_not_null!(store_ptr, "store");
        Ok(&*(store_ptr as *const Store))
    }

    /// Core function to validate store pointer and get mutable reference
    pub unsafe fn get_store_mut(store_ptr: *mut c_void) -> WasmtimeResult<&'static mut Store> {
        validate_ptr_not_null!(store_ptr, "store");
        Ok(&mut *(store_ptr as *mut Store))
    }

    /// Core function to add fuel to a store
    pub fn add_fuel(store: &Store, fuel: u64) -> WasmtimeResult<()> {
        store.add_fuel(fuel)
    }

    /// Core function to set fuel to a specific amount
    pub fn set_fuel(store: &Store, fuel: u64) -> WasmtimeResult<()> {
        store.set_fuel(fuel)
    }

    /// Core function to get remaining fuel
    pub fn get_fuel_remaining(store: &Store) -> WasmtimeResult<u64> {
        store.fuel_remaining().map(|opt| opt.unwrap_or(0))
    }

    /// Core function to consume fuel from a store
    pub fn consume_fuel(store: &Store, fuel: u64) -> WasmtimeResult<u64> {
        store.consume_fuel(fuel)
    }

    /// Core function to set fuel async yield interval
    pub fn set_fuel_async_yield_interval(store: &Store, interval: u64) -> WasmtimeResult<()> {
        store.set_fuel_async_yield_interval(interval)
    }

    /// Core function to get hostcall fuel limit
    pub fn get_hostcall_fuel(store: &Store) -> WasmtimeResult<usize> {
        store.hostcall_fuel()
    }

    /// Core function to set hostcall fuel limit
    pub fn set_hostcall_fuel(store: &Store, fuel: usize) -> WasmtimeResult<()> {
        store.set_hostcall_fuel(fuel)
    }

    /// Core function to set epoch deadline for interruption
    pub fn set_epoch_deadline(store: &Store, ticks: u64) {
        store.set_epoch_deadline(ticks)
    }

    /// Core function to configure epoch deadline trap
    pub fn epoch_deadline_trap(store: &Store) -> WasmtimeResult<()> {
        store.epoch_deadline_trap()
    }

    /// Core function to configure epoch deadline callback with function pointer
    ///
    /// # Arguments
    /// * `store` - The store to configure
    /// * `callback_fn` - A C-compatible function pointer that takes a callback ID and epoch,
    ///                   and returns positive value to continue or negative to trap
    /// * `callback_id` - An ID to pass to the callback function
    pub fn epoch_deadline_callback_with_fn(
        store: &Store,
        callback_fn: extern "C" fn(callback_id: i64, epoch: u64) -> i64,
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        store.epoch_deadline_callback_with_fn(callback_fn, callback_id)
    }

    /// Core function to configure epoch deadline async yield and update
    pub fn epoch_deadline_async_yield_and_update(store: &Store, delta: u64) -> WasmtimeResult<()> {
        store.epoch_deadline_async_yield_and_update(delta)
    }

    /// Core function to get execution statistics
    pub fn get_execution_stats(store: &Store) -> WasmtimeResult<ExecutionState> {
        store.execution_stats()
    }

    /// Core function to get memory usage statistics
    pub fn get_memory_usage(store: &Store) -> WasmtimeResult<MemoryUsage> {
        store.memory_usage()
    }

    /// Core function to force garbage collection
    pub fn garbage_collect(store: &Store) -> WasmtimeResult<()> {
        store.gc()
    }

    /// Core function to get store metadata
    pub fn get_store_metadata(store: &Store) -> &StoreMetadata {
        store.metadata()
    }

    /// Core function to validate store functionality
    pub fn validate_store(store: &Store) -> WasmtimeResult<()> {
        store.validate()
    }

    /// Core function to destroy a store (safe cleanup)
    ///
    /// Uses the consolidated `safe_destroy` utility from `ffi_common::resource_destruction`
    /// which provides double-free protection, fake pointer detection, and panic safety.
    pub unsafe fn destroy_store(store_ptr: *mut c_void) {
        use crate::ffi_common::resource_destruction::safe_destroy;
        let _ = safe_destroy::<Store>(store_ptr, "Store");
    }

    /// Core function to execute with store context
    pub fn with_store_context<T, F>(store: &Store, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&mut wasmtime::StoreContextMut<StoreData>) -> WasmtimeResult<T>,
    {
        store.with_context(func)
    }

    /// Core function to set a callback-based resource limiter on a store
    pub fn set_resource_limiter(
        store: &Store,
        callback_id: i64,
        memory_growing_fn: extern "C" fn(i64, u64, u64, u64) -> i32,
        table_growing_fn: extern "C" fn(i64, u32, u32, u32) -> i32,
        memory_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
        table_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
    ) -> WasmtimeResult<()> {
        store.set_resource_limiter(
            memory_growing_fn,
            table_growing_fn,
            memory_grow_failed_fn,
            table_grow_failed_fn,
            callback_id,
        )
    }

    /// Core function to set an async callback-based resource limiter on a store
    pub fn set_resource_limiter_async(
        store: &Store,
        callback_id: i64,
        memory_growing_fn: extern "C" fn(i64, u64, u64, u64) -> i32,
        table_growing_fn: extern "C" fn(i64, u32, u32, u32) -> i32,
        memory_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
        table_grow_failed_fn: Option<extern "C" fn(i64, *const std::os::raw::c_char)>,
    ) -> WasmtimeResult<()> {
        store.set_resource_limiter_async(
            memory_growing_fn,
            table_growing_fn,
            memory_grow_failed_fn,
            table_grow_failed_fn,
            callback_id,
        )
    }

    /// Core function to set a call hook on a store
    pub fn set_call_hook(store: &Store) -> WasmtimeResult<()> {
        store.set_call_hook()
    }

    /// Core function to clear a call hook on a store
    pub fn clear_call_hook(store: &Store) -> WasmtimeResult<()> {
        store.clear_call_hook()
    }

    /// Core function to set a call hook with a function pointer callback
    ///
    /// # Arguments
    /// * `store` - The store to configure
    /// * `callback_fn` - A C-compatible function pointer that takes a callback ID and hook type,
    ///                   and returns 0 for OK or non-zero to trap
    /// * `callback_id` - An ID to pass to the callback function
    pub fn set_call_hook_with_fn(
        store: &Store,
        callback_fn: extern "C" fn(callback_id: i64, hook_type: i32) -> i32,
        callback_id: i64,
    ) -> WasmtimeResult<()> {
        store.set_call_hook_with_fn(callback_fn, callback_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use std::time::Duration;

    // Use the global shared engine to reduce wasmtime GLOBAL_CODE registry accumulation
    fn shared_engine() -> Engine {
        crate::engine::get_shared_engine()
    }

    #[test]
    fn test_store_creation() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");
        assert!(store.validate().is_ok());
    }

    #[test]
    fn test_store_builder() {
        let engine = shared_engine();
        let store = Store::builder()
            .fuel_limit(1000)
            .memory_limit(1024 * 1024)
            .execution_timeout(Duration::from_secs(30))
            .max_instances(10)
            .build(&engine)
            .expect("Failed to build store");

        assert!(store.validate().is_ok());
        assert_eq!(store.metadata().fuel_limit, Some(1000));
        assert_eq!(store.metadata().memory_limit_bytes, Some(1024 * 1024));
        assert_eq!(
            store.metadata().execution_timeout,
            Some(Duration::from_secs(30))
        );
    }

    #[test]
    fn test_fuel_management() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine with fuel enabled");
        let store = Store::builder()
            .fuel_limit(1000)
            .build(&engine)
            .expect("Failed to build store");

        // Test adding fuel
        assert!(store.add_fuel(500).is_ok());

        // Test fuel remaining
        let remaining = store
            .fuel_remaining()
            .expect("Failed to get fuel remaining");
        assert!(remaining.is_some());

        // Test consuming fuel - returns remaining fuel after consumption
        let remaining_after = store.consume_fuel(100).expect("Failed to consume fuel");
        assert_eq!(remaining_after, 1400); // 1500 - 100 = 1400 remaining
    }

    #[test]
    fn test_execution_context() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let result = store.with_context(|_ctx| Ok(42i32));

        assert_eq!(result.unwrap(), 42);

        // Check execution stats were updated
        let stats = store.execution_stats().expect("Failed to get stats");
        assert_eq!(stats.execution_count, 1);
    }

    #[test]
    fn test_memory_usage() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let usage = store.memory_usage().expect("Failed to get memory usage");
        assert_eq!(usage.execution_count, 0); // No instances created yet
    }

    #[test]
    fn test_garbage_collection() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // GC should succeed without error
        assert!(store.gc().is_ok());
    }

    #[test]
    fn test_store_core_functions() {
        use crate::store::core;

        let engine = shared_engine();

        // Test core store creation
        let store = core::create_store(&engine).expect("Failed to create store via core");

        // Test core store validation
        let store_ref = unsafe {
            core::get_store_ref(store.as_ref() as *const Store as *const std::os::raw::c_void)
                .expect("Failed to get store reference")
        };
        assert!(core::validate_store(store_ref).is_ok());

        // Test core metadata access
        let metadata = core::get_store_metadata(store_ref);
        assert!(metadata.fuel_limit.is_none()); // Default store has no fuel limit

        // Test core garbage collection
        assert!(core::garbage_collect(store_ref).is_ok());
    }

    #[test]
    fn test_store_with_config_core() {
        use crate::store::core;

        let engine = shared_engine();

        // Test core store creation with config
        let store = core::create_store_with_config(
            &engine,
            Some(2000),            // fuel_limit
            Some(2 * 1024 * 1024), // memory_limit_bytes
            Some(60),              // execution_timeout_secs
            Some(20),              // max_instances
            Some(5000),            // max_table_elements
            None,                  // max_tables
            None,                  // max_memories
            false,                 // trap_on_grow_failure
        )
        .expect("Failed to create store with config via core");

        let store_ref = unsafe {
            core::get_store_ref(store.as_ref() as *const Store as *const std::os::raw::c_void)
                .expect("Failed to get store reference")
        };

        // Verify configuration was applied
        let metadata = core::get_store_metadata(store_ref);
        assert_eq!(metadata.fuel_limit, Some(2000));
        assert_eq!(metadata.memory_limit_bytes, Some(2 * 1024 * 1024));
        assert_eq!(metadata.execution_timeout, Some(Duration::from_secs(60)));
    }

    #[test]
    fn test_fuel_management_core() {
        use crate::store::core;

        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine with fuel enabled");
        let store = core::create_store_with_config(
            &engine,
            Some(1000),
            None,
            None,
            None,
            None,
            None,
            None,
            false,
        )
        .expect("Failed to create store with fuel");

        let store_ref = unsafe {
            core::get_store_ref(store.as_ref() as *const Store as *const std::os::raw::c_void)
                .expect("Failed to get store reference")
        };

        // Test adding fuel through core
        assert!(core::add_fuel(store_ref, 500).is_ok());

        // Test getting remaining fuel
        let remaining = core::get_fuel_remaining(store_ref).expect("Failed to get fuel remaining");
        assert!(remaining > 0);

        // Test consuming fuel - returns remaining fuel after consumption
        let remaining_after = core::consume_fuel(store_ref, 100).expect("Failed to consume fuel");
        assert_eq!(remaining_after, 1400); // 1500 - 100 = 1400 remaining

        // Test setting epoch deadline
        core::set_epoch_deadline(store_ref, 1000);
    }

    #[test]
    fn test_execution_statistics_core() {
        use crate::store::core;

        let engine = shared_engine();
        let store = core::create_store(&engine).expect("Failed to create store");

        let store_ref = unsafe {
            core::get_store_ref(store.as_ref() as *const Store as *const std::os::raw::c_void)
                .expect("Failed to get store reference")
        };

        // Initially, execution count should be 0
        let initial_stats = core::get_execution_stats(store_ref).expect("Failed to get stats");
        let initial_count = initial_stats.execution_count;

        // Execute something with the store context
        let result = core::with_store_context(store_ref, |_ctx| Ok(42i32));
        assert_eq!(result.unwrap(), 42);

        // Execution count should have increased
        let updated_stats =
            core::get_execution_stats(store_ref).expect("Failed to get updated stats");
        assert!(updated_stats.execution_count > initial_count);

        // Test memory usage stats
        let memory_usage = core::get_memory_usage(store_ref).expect("Failed to get memory usage");
        assert_eq!(
            memory_usage.execution_count,
            updated_stats.execution_count as usize
        );
    }

    #[test]
    fn test_defensive_pointer_validation() {
        use crate::store::core;

        // Test null pointer handling
        let result = unsafe { core::get_store_ref(std::ptr::null()) };
        assert!(result.is_err());

        let result = unsafe { core::get_store_mut(std::ptr::null_mut()) };
        assert!(result.is_err());
    }

    #[test]
    fn test_store_destroy() {
        use crate::store::core;

        let engine = shared_engine();
        let store = core::create_store(&engine).expect("Failed to create store");
        let store_ptr = Box::into_raw(store) as *mut std::os::raw::c_void;

        // Test safe destruction
        unsafe {
            core::destroy_store(store_ptr);
        }

        // Test destroying null pointer (should not crash)
        unsafe {
            core::destroy_store(std::ptr::null_mut());
        }
    }

    #[test]
    fn test_resource_limits() {
        let engine = shared_engine();
        let store = Store::builder()
            .fuel_limit(5000)
            .memory_limit(4 * 1024 * 1024)
            .max_instances(50)
            .max_table_elements(8000)
            .build(&engine)
            .expect("Failed to build store with resource limits");

        // Verify all limits are set correctly
        let metadata = store.metadata();
        assert_eq!(metadata.fuel_limit, Some(5000));
        assert_eq!(metadata.memory_limit_bytes, Some(4 * 1024 * 1024));

        // Test store validation passes
        assert!(store.validate().is_ok());
    }

    #[test]
    fn test_epoch_deadline() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Setting epoch deadline should not fail
        store.set_epoch_deadline(5000);
    }

    #[test]
    fn test_fuel_edge_cases() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine with fuel enabled");
        let store = Store::builder()
            .fuel_limit(100)
            .build(&engine)
            .expect("Failed to build store with fuel");

        // Test consuming more fuel than available returns error
        let result = store.consume_fuel(150);
        assert!(
            result.is_err(),
            "Should error when consuming more fuel than available"
        );

        // Test fuel remaining unchanged after failed consumption
        let remaining = store
            .fuel_remaining()
            .expect("Failed to get fuel remaining");
        assert_eq!(remaining, Some(100)); // Unchanged since consume_fuel(150) failed

        // Test consuming exactly available fuel succeeds
        let remaining_after = store.consume_fuel(100).expect("Should consume all available fuel");
        assert_eq!(remaining_after, 0);

        // Test consuming from empty store errors
        let result = store.consume_fuel(1);
        assert!(result.is_err(), "Should error when no fuel available");
    }

    #[test]
    fn test_thread_safety() {
        use std::sync::Arc;
        use std::thread;

        // Engine is thread-safe and can be shared across threads
        let engine = Arc::new(
            Engine::builder()
                .fuel_enabled(true)
                .build()
                .expect("Failed to create engine with fuel enabled"),
        );

        // Each thread gets its own Store (Wasmtime Store is NOT thread-safe)
        let handles: Vec<_> = (0..5)
            .map(|i| {
                let engine_clone = Arc::clone(&engine);
                thread::spawn(move || {
                    // Each thread creates its own Store from the shared Engine
                    let store = Store::builder()
                        .fuel_limit(10000)
                        .build(&engine_clone)
                        .expect("Failed to build store");

                    // Each thread adds some fuel to its own store
                    store
                        .add_fuel(100 * (i + 1) as u64)
                        .expect("Failed to add fuel");

                    // Each thread validates its store
                    store.validate().expect("Store validation failed");

                    // Each thread gets execution stats from its store
                    let _stats = store.execution_stats().expect("Failed to get stats");

                    i * 10
                })
            })
            .collect();

        // Wait for all threads to complete
        for handle in handles {
            let result = handle.join().expect("Thread panicked");
            assert!(result < 50); // Basic sanity check
        }
    }

    #[test]
    fn test_store_for_module() {
        use crate::module::Module;

        let engine = shared_engine();

        // A simple WebAssembly module that exports an add function
        let wasm_bytes = wat::parse_str(
            r#"
            (module
                (func $add (param $a i32) (param $b i32) (result i32)
                    local.get $a
                    local.get $b
                    i32.add)
                (export "add" (func $add))
            )
        "#,
        )
        .expect("Failed to parse WAT");

        // Compile module
        let module = Module::compile(&engine, &wasm_bytes).expect("Failed to compile module");

        // Create store using for_module - this should ensure Arc compatibility
        let mut store = Store::for_module(&module).expect("Failed to create store for module");

        // Verify store is valid
        assert!(store.validate().is_ok(), "Store should be valid");

        // The key test: verify the store can actually instantiate the module
        // This would fail with "cross-engine" error if Arc pointers don't match
        use crate::instance::Instance;
        let instance = Instance::new(&mut store, &module, &[])
            .expect("Failed to instantiate module - this verifies Arc compatibility");

        // Verify we can get the export
        let exports = instance.exports(&mut store).expect("Failed to get exports");
        assert!(
            exports.contains(&"add".to_string()),
            "Should have 'add' export"
        );
    }

    #[test]
    fn test_store_builder_for_module() {
        use crate::module::Module;

        let engine = shared_engine();

        // A simple WebAssembly module
        let wasm_bytes = wat::parse_str(
            r#"
            (module
                (func $nop)
                (export "nop" (func $nop))
            )
        "#,
        )
        .expect("Failed to parse WAT");

        // Compile module
        let module = Module::compile(&engine, &wasm_bytes).expect("Failed to compile module");

        // Create store using builder with for_module
        let mut store = Store::builder()
            .memory_limit(1024 * 1024)
            .build_for_module(&module)
            .expect("Failed to build store for module");

        // Verify store is valid
        assert!(store.validate().is_ok(), "Store should be valid");

        // Verify configuration was applied
        assert_eq!(store.metadata().memory_limit_bytes, Some(1024 * 1024));

        // Verify instantiation works
        use crate::instance::Instance;
        let _instance = Instance::new(&mut store, &module, &[])
            .expect("Failed to instantiate module with configured store");
    }

    #[cfg(feature = "wasi-nn")]
    #[test]
    fn test_wasi_nn_context_setup() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Initially no wasi-nn context
        assert!(
            !store.has_wasi_nn_context(),
            "Store should not have wasi-nn context initially"
        );

        // Set wasi-nn context
        store
            .set_wasi_nn_context()
            .expect("Failed to set wasi-nn context");

        // Now should have context
        assert!(
            store.has_wasi_nn_context(),
            "Store should have wasi-nn context after setting"
        );

        // List available backends
        let backends = wasmtime_wasi_nn::backend::list();
        println!("Available wasi-nn backends count: {}", backends.len());

        // At minimum, we should be able to create the context
        // The actual backends depend on what's installed (OpenVINO, ONNX, etc.)
    }

    #[cfg(feature = "wasi-nn")]
    #[test]
    fn test_wasi_nn_linker_integration() {
        use crate::linker::Linker;

        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Set wasi-nn context first
        store
            .set_wasi_nn_context()
            .expect("Failed to set wasi-nn context");

        // Create linker and enable wasi-nn
        let mut linker = Linker::new(&engine).expect("Failed to create linker");
        linker
            .enable_wasi_nn()
            .expect("Failed to enable wasi-nn on linker");

        println!("WASI-NN successfully integrated with linker");
    }

    // =========================================================================
    // Store Builder Extended Tests (10 tests)
    // =========================================================================

    #[test]
    fn test_store_builder_default() {
        let engine = shared_engine();
        let store = Store::builder()
            .build(&engine)
            .expect("Failed to build store");

        // Default store should have no limits
        let metadata = store.metadata();
        assert_eq!(metadata.fuel_limit, None);
        assert_eq!(metadata.memory_limit_bytes, None);
        assert_eq!(metadata.execution_timeout, None);
    }

    #[test]
    fn test_store_builder_fuel_limit_zero() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::builder()
            .fuel_limit(0)
            .build(&engine)
            .expect("Failed to build store");

        // Fuel limit of 0 should be valid
        assert_eq!(store.metadata().fuel_limit, Some(0));
    }

    #[test]
    fn test_store_builder_large_memory_limit() {
        let engine = shared_engine();
        let store = Store::builder()
            .memory_limit(usize::MAX / 2)
            .build(&engine)
            .expect("Failed to build store");

        assert_eq!(store.metadata().memory_limit_bytes, Some(usize::MAX / 2));
    }

    #[test]
    fn test_store_builder_short_timeout() {
        let engine = shared_engine();
        let store = Store::builder()
            .execution_timeout(Duration::from_millis(1))
            .build(&engine)
            .expect("Failed to build store");

        assert_eq!(
            store.metadata().execution_timeout,
            Some(Duration::from_millis(1))
        );
    }

    #[test]
    fn test_store_builder_all_resource_limits() {
        let engine = shared_engine();
        let store = Store::builder()
            .max_instances(100)
            .max_table_elements(10000)
            .build(&engine)
            .expect("Failed to build store");

        assert!(store.validate().is_ok());
    }

    #[test]
    fn test_store_unique_ids() {
        let engine = shared_engine();
        let store1 = Store::new(&engine).expect("Failed to create store 1");
        let store2 = Store::new(&engine).expect("Failed to create store 2");
        let store3 = Store::new(&engine).expect("Failed to create store 3");

        assert_ne!(store1.id(), store2.id());
        assert_ne!(store2.id(), store3.id());
        assert_ne!(store1.id(), store3.id());
    }

    #[test]
    fn test_store_id_monotonic() {
        let engine = shared_engine();
        let store1 = Store::new(&engine).expect("Failed to create store 1");
        let store2 = Store::new(&engine).expect("Failed to create store 2");

        assert!(store2.id() > store1.id());
    }

    // =========================================================================
    // Fuel Management Extended Tests (5 tests)
    // =========================================================================

    #[test]
    fn test_fuel_set_and_get() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::builder()
            .fuel_limit(1000)
            .build(&engine)
            .expect("Failed to build store");

        store.set_fuel(500).expect("Failed to set fuel");
        let remaining = store.fuel_remaining().expect("Failed to get fuel");
        assert_eq!(remaining, Some(500));
    }

    #[test]
    fn test_fuel_saturation_add() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::builder()
            .fuel_limit(u64::MAX)
            .build(&engine)
            .expect("Failed to build store");

        // Adding to large fuel should saturate, not overflow
        store.set_fuel(u64::MAX - 10).expect("Failed to set fuel");
        store.add_fuel(100).expect("Failed to add fuel");

        let remaining = store.fuel_remaining().expect("Failed to get fuel");
        assert!(remaining.is_some());
    }

    #[test]
    fn test_fuel_multiple_operations() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::builder()
            .fuel_limit(1000)
            .build(&engine)
            .expect("Failed to build store");

        store.add_fuel(100).expect("Failed to add fuel");
        store.add_fuel(100).expect("Failed to add fuel");
        store.consume_fuel(50).expect("Failed to consume fuel");
        store.add_fuel(50).expect("Failed to add fuel");

        let remaining = store.fuel_remaining().expect("Failed to get fuel");
        // 100 + 100 - 50 + 50 = 200
        assert!(remaining.is_some());
    }

    #[test]
    fn test_fuel_async_yield_interval_with_fuel() {
        // Wasmtime allows setting fuel_async_yield_interval even without async support
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::builder()
            .fuel_limit(1000)
            .build(&engine)
            .expect("Failed to build store");

        let result = store.set_fuel_async_yield_interval(100);
        assert!(
            result.is_ok(),
            "Setting fuel async yield interval should succeed with fuel enabled: {:?}",
            result.err()
        );

        // Disabling with 0 should also work
        let result = store.set_fuel_async_yield_interval(0);
        assert!(
            result.is_ok(),
            "Disabling fuel async yield interval should succeed: {:?}",
            result.err()
        );
    }

    #[test]
    fn test_fuel_async_yield_interval_without_fuel() {
        // Without fuel enabled, the call should still not panic — errors are
        // propagated rather than silently ignored
        let engine = Engine::builder()
            .build()
            .expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        // This may succeed or error depending on Wasmtime's internal validation,
        // but must not panic or silently swallow errors
        let _result = store.set_fuel_async_yield_interval(100);
    }

    #[cfg(feature = "async")]
    #[test]
    fn test_fuel_async_yield_interval_success_with_async() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .async_support(true)
            .build()
            .expect("Failed to create async engine");
        let store = Store::builder()
            .fuel_limit(1000)
            .build(&engine)
            .expect("Failed to build store");

        let result = store.set_fuel_async_yield_interval(100);
        assert!(
            result.is_ok(),
            "Setting fuel async yield interval should succeed with async support: {:?}",
            result.err()
        );
    }

    // =========================================================================
    // Epoch Deadline Tests (3 tests)
    // =========================================================================

    #[test]
    fn test_epoch_deadline_zero() {
        let engine = Engine::builder()
            .epoch_interruption(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        // Setting zero deadline should work
        store.set_epoch_deadline(0);
    }

    #[test]
    fn test_epoch_deadline_large() {
        let engine = Engine::builder()
            .epoch_interruption(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        store.set_epoch_deadline(u64::MAX);
    }

    #[test]
    fn test_epoch_deadline_multiple() {
        let engine = Engine::builder()
            .epoch_interruption(true)
            .build()
            .expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        store.set_epoch_deadline(100);
        store.set_epoch_deadline(200);
        store.set_epoch_deadline(50);
        // Last set value should be active
    }

    // =========================================================================
    // Execution Statistics Tests (5 tests)
    // =========================================================================

    #[test]
    fn test_execution_stats_initial() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let stats = store.execution_stats().expect("Failed to get stats");
        assert_eq!(stats.execution_count, 0);
        assert!(stats.last_execution.is_none());
        assert_eq!(stats.total_execution_time, Duration::ZERO);
    }

    #[test]
    fn test_execution_stats_after_context() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        store
            .with_context(|_ctx| Ok(()))
            .expect("Failed to execute");
        store
            .with_context(|_ctx| Ok(()))
            .expect("Failed to execute");
        store
            .with_context(|_ctx| Ok(()))
            .expect("Failed to execute");

        let stats = store.execution_stats().expect("Failed to get stats");
        assert_eq!(stats.execution_count, 3);
    }

    #[test]
    fn test_execution_stats_time_increases() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        store
            .with_context(|_ctx| {
                std::thread::sleep(Duration::from_millis(1));
                Ok(())
            })
            .expect("Failed to execute");

        let stats = store.execution_stats().expect("Failed to get stats");
        assert!(stats.total_execution_time >= Duration::from_millis(1));
    }

    #[test]
    fn test_memory_usage_default() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        let usage = store.memory_usage().expect("Failed to get memory usage");
        assert_eq!(usage.execution_count, 0);
    }

    #[test]
    fn test_store_gc_multiple() {
        let engine = shared_engine();
        let store = Store::new(&engine).expect("Failed to create store");

        // Multiple GC calls should not fail
        store.gc().expect("First GC failed");
        store.gc().expect("Second GC failed");
        store.gc().expect("Third GC failed");
    }
}

//
// Native C exports for JNI and Panama FFI consumption
//

use crate::shared_ffi::{FFI_ERROR, FFI_SUCCESS};
use std::os::raw::{c_int, c_void};

/// Create a new store with engine
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
/// Returns pointer to store that must be freed with wasmtime4j_store_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_new(engine_ptr: *const c_void) -> *mut c_void {
    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => match core::create_store(engine) {
            Ok(store) => Box::into_raw(store) as *mut c_void,
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Create a new store compatible with a specific module
///
/// CRITICAL: This ensures the Store's internal wasmtime::Store uses the SAME Arc
/// as the Module's internal wasmtime::Module. This is required because wasmtime's
/// Instance::new() uses Arc::ptr_eq() to verify engine compatibility.
///
/// # Safety
///
/// module_ptr must be a valid pointer from wasmtime4j_module_new
/// Returns pointer to store that must be freed with wasmtime4j_store_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_new_for_module(module_ptr: *const c_void) -> *mut c_void {
    if module_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let module = &*(module_ptr as *const Module);
    match core::create_store_for_module(module) {
        Ok(store) => Box::into_raw(store) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Destroy store and free resources
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_destroy(store_ptr: *mut c_void) {
    if !store_ptr.is_null() {
        core::destroy_store(store_ptr);
    }
}

/// Alias for wasmtime4j_store_new (Panama FFI compatibility)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_create(engine_ptr: *const c_void) -> *mut c_void {
    wasmtime4j_store_new(engine_ptr)
}

/// Validate store is still functional
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_validate(store_ptr: *const c_void) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => match core::validate_store(store) {
            Ok(_) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Check if this store has async support enabled
///
/// # Returns
///
/// 1 if async is enabled, 0 if not, negative on error
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_is_async(store_ptr: *const c_void) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => {
            if store.is_async() {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Add fuel to store for execution metering
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_add_fuel(store_ptr: *const c_void, fuel: u64) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => match core::add_fuel(store, fuel) {
            Ok(_) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Consume fuel from store
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
/// Returns remaining fuel after consumption, or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_consume_fuel(store_ptr: *const c_void, fuel: u64) -> u64 {
    match core::get_store_ref(store_ptr) {
        Ok(store) => match core::consume_fuel(store, fuel) {
            Ok(consumed) => consumed,
            Err(_) => 0,
        },
        Err(_) => 0,
    }
}

/// Set WASI context on a store (Panama FFI)
///
/// This function attaches a WASI context to a store, which is required before
/// instantiating modules that import WASI functions.
///
/// # Safety
///
/// - store_ptr must be a valid pointer from wasmtime4j_store_new
/// - wasi_ctx_ptr must be a valid pointer from wasmtime4j_wasi_context_create
///
/// # Returns
/// - 0 on success
/// - non-zero on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_set_wasi_context(
    store_ptr: *mut c_void,
    wasi_ctx_ptr: *const c_void,
) -> c_int {
    if store_ptr.is_null() || wasi_ctx_ptr.is_null() {
        return -1;
    }

    // Get the store from pointer
    let store = match core::get_store_ref(store_ptr) {
        Ok(s) => s,
        Err(_) => {
            return -2;
        }
    };

    // The WASI context pointer points to a WasiContext
    let wasi_ctx = &*(wasi_ctx_ptr as *const crate::wasi::WasiContext);

    // Create a new fd_manager for this store
    let fd_manager = crate::wasi::WasiFileDescriptorManager::new();

    // Set WASI context on the store (builds a fresh WasiP1Ctx from configuration)
    match store.set_wasi_context(wasi_ctx, fd_manager) {
        Ok(()) => 0,
        Err(_) => -3,
    }
}

/// Check if store has WASI context attached (Panama FFI)
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
///
/// # Returns
/// - 1 if store has WASI context
/// - 0 if store does not have WASI context
/// - -1 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_has_wasi_context(store_ptr: *const c_void) -> c_int {
    if store_ptr.is_null() {
        return -1;
    }

    match core::get_store_ref(store_ptr) {
        Ok(store) => {
            if store.has_wasi_context() {
                1
            } else {
                0
            }
        }
        Err(_) => -1,
    }
}

// ===== Debugging FFI Functions =====

/// Check if single-step mode is active
///
/// # Returns
///
/// 1 if single-step is active, 0 if not, negative on error
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_is_single_step(store_ptr: *const c_void) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => {
            if store.is_single_step() {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Get the number of active breakpoints
///
/// # Returns
///
/// Number of breakpoints (>= 0), or -1 if debugging not enabled, -2 on error
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_breakpoint_count(store_ptr: *const c_void) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => match store.breakpoint_count() {
            Ok(Some(count)) => count as c_int,
            Ok(None) => -1, // debugging not enabled
            Err(_) => -2,
        },
        Err(_) => -2,
    }
}

/// Enable or disable single-step mode
///
/// # Returns
///
/// 0 on success, 1 if debugging not enabled, negative on error
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_set_single_step(
    store_ptr: *const c_void,
    enabled: c_int,
) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => {
            let enable = enabled != 0;
            match store.edit_breakpoints(|edit| {
                let _ = edit.single_step(enable);
            }) {
                Ok(true) => FFI_SUCCESS,
                Ok(false) => 1, // debugging not enabled
                Err(_) => FFI_ERROR,
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Add a breakpoint at a specific module and program counter
///
/// # Returns
///
/// 0 on success, 1 if debugging not enabled, negative on error
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
/// module_ptr must be a valid pointer from wasmtime4j_module_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_add_breakpoint(
    store_ptr: *const c_void,
    module_ptr: *const c_void,
    pc: u32,
) -> c_int {
    if module_ptr.is_null() {
        return FFI_ERROR;
    }

    let module = match crate::module::core::get_module_ref(module_ptr) {
        Ok(m) => m,
        Err(_) => return FFI_ERROR,
    };

    match core::get_store_ref(store_ptr) {
        Ok(store) => {
            let wasm_module = module.inner().clone();
            match store.edit_breakpoints(|edit| {
                let _ = edit.add_breakpoint(&wasm_module, pc);
            }) {
                Ok(true) => FFI_SUCCESS,
                Ok(false) => 1,
                Err(_) => FFI_ERROR,
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Remove a breakpoint at a specific module and program counter
///
/// # Returns
///
/// 0 on success, 1 if debugging not enabled, negative on error
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
/// module_ptr must be a valid pointer from wasmtime4j_module_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_remove_breakpoint(
    store_ptr: *const c_void,
    module_ptr: *const c_void,
    pc: u32,
) -> c_int {
    if module_ptr.is_null() {
        return FFI_ERROR;
    }

    let module = match crate::module::core::get_module_ref(module_ptr) {
        Ok(m) => m,
        Err(_) => return FFI_ERROR,
    };

    match core::get_store_ref(store_ptr) {
        Ok(store) => {
            let wasm_module = module.inner().clone();
            match store.edit_breakpoints(|edit| {
                let _ = edit.remove_breakpoint(&wasm_module, pc);
            }) {
                Ok(true) => FFI_SUCCESS,
                Ok(false) => 1,
                Err(_) => FFI_ERROR,
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Snapshot debug exit frames from the store.
///
/// # Returns
///
/// 0 on success, -1 if debugging not enabled, -2 on error.
/// Frame count is written to out_count. If out_data is non-null, frame data
/// is written as a flat array of i32s: [func_index, pc, num_locals, num_stacks]
/// per frame.
///
/// To use: call once with out_data=null to get count, allocate,
/// then call again with out_data pointing to a buffer of count*4 i32s.
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new.
/// out_data, if non-null, must point to a buffer of at least count*4 i32s.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_debug_exit_frames(
    store_ptr: *const c_void,
    out_data: *mut i32,
    out_count: *mut i32,
) -> c_int {
    match core::get_store_ref(store_ptr) {
        Ok(store) => match store.debug_exit_frames() {
            Ok(Some(frames)) => {
                if !out_count.is_null() {
                    *out_count = frames.len() as i32;
                }
                if !out_data.is_null() {
                    for (i, frame) in frames.iter().enumerate() {
                        let base = i * 4;
                        *out_data.add(base) = frame[0];
                        *out_data.add(base + 1) = frame[1];
                        *out_data.add(base + 2) = frame[2];
                        *out_data.add(base + 3) = frame[3];
                    }
                }
                FFI_SUCCESS
            }
            Ok(None) => {
                if !out_count.is_null() {
                    *out_count = 0;
                }
                -1 // debugging not enabled
            }
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// FFI-compatible debug handler that dispatches debug events through a callback function.
///
/// This struct implements the Wasmtime `DebugHandler` trait and forwards events
/// to a C-compatible function pointer with a callback ID for identifying the Java handler.
#[derive(Clone)]
struct FfiDebugHandler {
    callback_fn: extern "C" fn(callback_id: i64, event_code: i32),
    callback_id: i64,
}

// Safety: The callback function pointer is valid for the lifetime of the store
// and the callback dispatches through thread-safe Java mechanisms.
unsafe impl Send for FfiDebugHandler {}
unsafe impl Sync for FfiDebugHandler {}

impl DebugHandler for FfiDebugHandler {
    type Data = StoreData;

    fn handle(
        &self,
        _store: StoreContextMut<'_, StoreData>,
        event: DebugEvent<'_>,
    ) -> impl Future<Output = ()> + Send {
        let event_code = match event {
            DebugEvent::HostcallError(_) => 0,
            DebugEvent::CaughtExceptionThrown(_) => 1,
            DebugEvent::UncaughtExceptionThrown(_) => 2,
            DebugEvent::Trap(_) => 3,
            DebugEvent::Breakpoint => 4,
            DebugEvent::EpochYield => 5,
        };
        (self.callback_fn)(self.callback_id, event_code);
        std::future::ready(())
    }
}
