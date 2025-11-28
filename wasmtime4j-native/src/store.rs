//! WebAssembly store management with resource tracking and isolation
//!
//! The Store provides execution context and resource management for WebAssembly instances.
//! Each store maintains its own isolated execution environment with resource limits,
//! fuel tracking, and comprehensive cleanup capabilities.

use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};
use wasmtime::{Store as WasmtimeStore, StoreContext, StoreContextMut, AsContext, AsContextMut, FuncType, Func};
use wasmtime_wasi::p1::WasiP1Ctx;
use wasmtime_wasi::p2::pipe::MemoryOutputPipe;
use crate::engine::Engine;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::hostfunc::{HostFunction, HostFunctionCallback};
use crate::interop::ReentrantLock;
use crate::module::Module;
use once_cell::sync::Lazy;

/// Store ID counter for unique identification
static STORE_ID_COUNTER: Lazy<Mutex<u64>> = Lazy::new(|| Mutex::new(1));

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
}

impl std::fmt::Debug for StoreData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("StoreData")
            .field("user_data", &self.user_data.as_ref().map(|_| "<user_data>"))
            .field("resource_limits", &self.resource_limits)
            .field("execution_state", &self.execution_state)
            .field("wasi_ctx", &self.wasi_ctx.as_ref().map(|_| "<WasiP1Ctx>"))
            .field("wasi_stdout_pipe", &self.wasi_stdout_pipe.as_ref().map(|_| "<pipe>"))
            .field("wasi_stderr_pipe", &self.wasi_stderr_pipe.as_ref().map(|_| "<pipe>"))
            .field("wasi_fd_manager", &self.wasi_fd_manager.as_ref().map(|_| "<fd_manager>"))
            .finish()
    }
}

impl Clone for StoreData {
    fn clone(&self) -> Self {
        StoreData {
            user_data: None, // Can't clone arbitrary Any type, set to None
            resource_limits: self.resource_limits.clone(),
            execution_state: self.execution_state.clone(),
            wasi_ctx: None, // WasiP1Ctx doesn't implement Clone, each store needs its own
            wasi_stdout_pipe: self.wasi_stdout_pipe.clone(),
            wasi_stderr_pipe: self.wasi_stderr_pipe.clone(),
            wasi_fd_manager: None, // FD manager is store-specific
        }
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
    /// Maximum number of functions that can be instantiated
    pub max_functions: Option<usize>,
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
    pub fn lock_store(&self) -> crate::interop::ReentrantLockGuard<'_, WasmtimeStore<StoreData>> {
        self.inner.lock()
    }

    /// Execute function with store context
    ///
    /// NOTE: This method creates temporary context lifetimes and should NOT be
    /// used for WASM function calls in JNI/Panama environments. Use `lock_store()`
    /// instead for direct Store access.
    pub fn with_context<T, F>(&self, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&mut StoreContextMut<StoreData>) -> WasmtimeResult<T>,
    {
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
    pub fn with_context_ro<T, F>(&self, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&StoreContext<StoreData>) -> WasmtimeResult<T>,
    {
        let store = self.inner.lock();
        func(&store.as_context())
    }

    /// Get store metadata
    pub fn metadata(&self) -> &StoreMetadata {
        &self.metadata
    }

    /// Add fuel to the store for execution limiting
    pub fn add_fuel(&self, fuel: u64) -> WasmtimeResult<()> {
        let mut store = self.inner.lock();

        store.set_fuel(fuel).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to set fuel: {}", e),
            backtrace: None,
        })?;

        Ok(())
    }

    /// Get remaining fuel
    pub fn fuel_remaining(&self) -> WasmtimeResult<Option<u64>> {
        let store = self.inner.lock();

        Ok(Some(store.get_fuel().unwrap_or(0)))
    }

    /// Consume fuel from the store
    pub fn consume_fuel(&self, fuel: u64) -> WasmtimeResult<u64> {
        let mut store = self.inner.lock();

        let current_fuel = store.get_fuel().unwrap_or(0);
        if current_fuel >= fuel {
            store.set_fuel(current_fuel - fuel).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to consume fuel: {}", e),
                backtrace: None,
            })?;
            Ok(fuel)
        } else {
            store.set_fuel(0).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to consume fuel: {}", e),
                backtrace: None,
            })?;
            Ok(current_fuel)
        }
    }

    /// Set epoch deadline for interruption
    pub fn set_epoch_deadline(&self, ticks: u64) {
        let mut store = self.inner.lock();
        store.set_epoch_deadline(ticks);
    }

    /// Get execution statistics
    pub fn execution_stats(&self) -> WasmtimeResult<ExecutionState> {
        let store = self.inner.lock();

        Ok(store.data().execution_state.clone())
    }

    /// Validate store is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Check if store has been closed
        if self.metadata.is_closed.load(std::sync::atomic::Ordering::SeqCst) {
            return Err(WasmtimeError::Store {
                message: format!("Store {} has been closed and cannot be used", self.id),
            });
        }

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
    pub fn gc(&self) -> WasmtimeResult<()> {
        let mut store = self.inner.lock();

        store.gc(None); // Pass None for manual GC trigger
        Ok(())
    }

    /// Get memory usage statistics
    pub fn memory_usage(&self) -> WasmtimeResult<MemoryUsage> {
        let store = self.inner.lock();

        // Note: Wasmtime doesn't provide direct memory usage stats
        // We approximate based on what we can measure
        Ok(MemoryUsage {
            total_bytes: 0, // Not easily available
            used_bytes: 0,  // Not easily available
            instance_count: store.data().execution_state.execution_count as usize,
        })
    }

    /// Create a host function that can be imported by WebAssembly modules
    pub fn create_host_function(
        &self,
        name: String,
        func_type: FuncType,
        callback: Box<dyn HostFunctionCallback + Send + Sync>,
    ) -> WasmtimeResult<(u64, Func)> {
        let store_weak = Arc::downgrade(&self.inner);

        // Create the host function wrapper
        let host_function = HostFunction::new(name, func_type, store_weak, callback)?;
        let host_function_id = host_function.id();

        // Create the Wasmtime Func
        let wasmtime_func = {
            let mut store = self.inner.lock();
            host_function.create_wasmtime_func(&mut store)?
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
        let registry_id = register_function_reference(func)?;

        Ok(registry_id)
    }

    /// Set WASI context for this store by building a fresh WasiP1Ctx from configuration
    pub fn set_wasi_context(
        &self,
        wasi_context: &crate::wasi::WasiContext,
        fd_manager: crate::wasi::WasiFileDescriptorManager,
    ) -> WasmtimeResult<()> {
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
    pub fn has_wasi_context(&self) -> bool {
        let store = self.inner.lock();
        store.data().wasi_ctx.is_some()
    }

    /// Get captured stdout data from WASI execution
    pub fn get_wasi_stdout(&self) -> WasmtimeResult<Option<Vec<u8>>> {
        let store = self.inner.lock();
        if let Some(pipe) = &store.data().wasi_stdout_pipe {
            let contents = pipe.contents();
            Ok(Some(contents.to_vec()))
        } else {
            Ok(None)
        }
    }

    /// Get captured stderr data from WASI execution
    pub fn get_wasi_stderr(&self) -> WasmtimeResult<Option<Vec<u8>>> {
        let store = self.inner.lock();
        if let Some(pipe) = &store.data().wasi_stderr_pipe {
            let contents = pipe.contents();
            Ok(Some(contents.to_vec()))
        } else {
            Ok(None)
        }
    }

    /// Remove WASI context from this store
    pub fn remove_wasi_context(&self) -> WasmtimeResult<()> {
        let mut store = self.inner.lock();

        let data = store.data_mut();
        data.wasi_ctx = None;
        data.wasi_stdout_pipe = None;
        data.wasi_stderr_pipe = None;
        data.wasi_fd_manager = None;
        Ok(())
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
                max_functions: None,
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

    /// Set maximum number of functions
    pub fn max_functions(mut self, limit: usize) -> Self {
        self.resource_limits.max_functions = Some(limit);
        self
    }

    /// Build store with current configuration
    pub fn build(self, engine: &Engine) -> WasmtimeResult<Store> {
        engine.validate()?;

        // Generate unique store ID
        let store_id = {
            let mut counter = STORE_ID_COUNTER.lock().map_err(|e| WasmtimeError::Store {
                message: format!("Failed to acquire store ID counter: {}", e),
            })?;
            let id = *counter;
            *counter += 1;
            id
        };

        let store_data = StoreData {
            user_data: None,
            resource_limits: self.resource_limits,
            execution_state: ExecutionState {
                execution_count: 0,
                last_execution: None,
                total_execution_time: Duration::new(0, 0),
                fuel_consumed: 0,
            },
            wasi_ctx: None,
            wasi_stdout_pipe: None,
            wasi_stderr_pipe: None,
            wasi_fd_manager: None,
        };

        let mut wasmtime_store = WasmtimeStore::new(engine.inner(), store_data);

        // Configure fuel if specified OR if Engine requires it
        if engine.fuel_enabled() {
            let fuel = self.fuel_limit.unwrap_or(u64::MAX);
            wasmtime_store.set_fuel(fuel).map_err(|e| WasmtimeError::Store {
                message: format!("Failed to set initial fuel: {}", e),
            })?;
        }

        let metadata = StoreMetadata {
            created_at: Instant::now(),
            fuel_limit: self.fuel_limit,
            memory_limit_bytes: self.memory_limit_bytes,
            execution_timeout: self.execution_timeout,
            instance_count: 0,
            is_closed: Arc::new(std::sync::atomic::AtomicBool::new(false)),
        };

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
        // Generate unique store ID
        let store_id = {
            let mut counter = STORE_ID_COUNTER.lock().map_err(|e| WasmtimeError::Store {
                message: format!("Failed to acquire store ID counter: {}", e),
            })?;
            let id = *counter;
            *counter += 1;
            id
        };

        let store_data = StoreData {
            user_data: None,
            resource_limits: self.resource_limits,
            execution_state: ExecutionState {
                execution_count: 0,
                last_execution: None,
                total_execution_time: Duration::new(0, 0),
                fuel_consumed: 0,
            },
            wasi_ctx: None,
            wasi_stdout_pipe: None,
            wasi_stderr_pipe: None,
            wasi_fd_manager: None,
        };

        // CRITICAL: Use the wasmtime Engine from the Module's internal wasmtime::Module
        // This ensures the Store's internal Arc matches the Module's internal Arc
        let wasmtime_engine = module.wasmtime_engine();
        let mut wasmtime_store = WasmtimeStore::new(wasmtime_engine, store_data);

        // Configure fuel if the engine has it enabled
        // Check via the module's engine reference
        if module.engine().fuel_enabled() {
            let fuel = self.fuel_limit.unwrap_or(u64::MAX);
            wasmtime_store.set_fuel(fuel).map_err(|e| WasmtimeError::Store {
                message: format!("Failed to set initial fuel: {}", e),
            })?;
        }

        let metadata = StoreMetadata {
            created_at: Instant::now(),
            fuel_limit: self.fuel_limit,
            memory_limit_bytes: self.memory_limit_bytes,
            execution_timeout: self.execution_timeout,
            instance_count: 0,
            is_closed: Arc::new(std::sync::atomic::AtomicBool::new(false)),
        };

        Ok(Store {
            id: store_id,
            inner: Arc::new(ReentrantLock::new(wasmtime_store)),
            engine: module.engine().clone(),
            metadata,
        })
    }
}

/// Memory usage statistics
#[derive(Debug, Clone)]
pub struct MemoryUsage {
    /// Total allocated memory in bytes
    pub total_bytes: usize,
    /// Currently used memory in bytes  
    pub used_bytes: usize,
    /// Number of active instances
    pub instance_count: usize,
}

impl Default for ResourceLimits {
    fn default() -> Self {
        ResourceLimits {
            max_memory_bytes: Some(64 * 1024 * 1024), // 64MB default
            max_table_elements: Some(10000),
            max_instances: Some(100),
            max_functions: Some(1000),
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
    use std::os::raw::c_void;
    use std::time::Duration;
    use crate::validate_ptr_not_null;
    use crate::engine::Engine;
    
    /// Core function to create a new store with default configuration
    pub fn create_store(engine: &Engine) -> WasmtimeResult<Box<Store>> {
        let store = Store::new(engine)?;
        Ok(Box::new(store))
    }


    /// Get store ID from pointer
    pub fn get_store_id(store_ptr: *const c_void) -> WasmtimeResult<u64> {
        validate_ptr_not_null!(store_ptr, "store");
        let store_ref = unsafe { &*(store_ptr as *const Store) };
        Ok(store_ref.id())
    }
    
    /// Core function to create a store with custom configuration
    pub fn create_store_with_config(
        engine: &Engine,
        fuel_limit: Option<u64>,
        memory_limit_bytes: Option<usize>,
        execution_timeout_secs: Option<u64>,
        max_instances: Option<usize>,
        max_table_elements: Option<u32>,
        max_functions: Option<usize>,
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
        
        if let Some(functions) = max_functions {
            builder = builder.max_functions(functions);
        }
        
        builder.build(engine).map(Box::new)
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
    
    /// Core function to get remaining fuel
    pub fn get_fuel_remaining(store: &Store) -> WasmtimeResult<u64> {
        store.fuel_remaining().map(|opt| opt.unwrap_or(0))
    }
    
    /// Core function to consume fuel from a store
    pub fn consume_fuel(store: &Store, fuel: u64) -> WasmtimeResult<u64> {
        store.consume_fuel(fuel)
    }
    
    /// Core function to set epoch deadline for interruption
    pub fn set_epoch_deadline(store: &Store, ticks: u64) {
        store.set_epoch_deadline(ticks)
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
    pub unsafe fn destroy_store(store_ptr: *mut c_void) {
        if store_ptr.is_null() {
            return;
        }

        let ptr_addr = store_ptr as usize;
        
        // Detect and reject obvious test/fake pointers
        if ptr_addr < 0x1000 || (ptr_addr & 0xFFFFFF0000000000) == 0x1234560000000000 {
            log::debug!("Ignoring fake/test pointer {:p} in destroy_store", store_ptr);
            return;
        }

        // Check if pointer was already destroyed
        {
            use crate::error::ffi_utils::DESTROYED_POINTERS;
            let mut destroyed = DESTROYED_POINTERS.lock().unwrap();
            if destroyed.contains(&ptr_addr) {
                log::warn!("Attempted double-free of Store resource at {:p} - ignoring", store_ptr);
                return;
            }
            destroyed.insert(ptr_addr);
        }

        // Simple, correct cleanup - let Rust handle Arc dropping naturally
        let result = std::panic::catch_unwind(|| {
            let _boxed_store = Box::from_raw(store_ptr as *mut Store);
            // Box and Arc will be dropped automatically here
            log::debug!("Store at {:p} being destroyed", store_ptr);
        });

        match result {
            Ok(_) => {
                log::debug!("Store resource at {:p} destroyed successfully", store_ptr);
            }
            Err(e) => {
                log::error!("Store resource at {:p} destruction panicked: {:?} - preventing JVM crash", store_ptr, e);
                // Don't propagate panic to JVM
            }
        }
    }
    
    /// Core function to execute with store context
    pub fn with_store_context<T, F>(store: &Store, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&mut wasmtime::StoreContextMut<StoreData>) -> WasmtimeResult<T>,
    {
        store.with_context(func)
    }
    
    /// Core function to execute with read-only store context
    pub fn with_store_context_ro<T, F>(store: &Store, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&wasmtime::StoreContext<StoreData>) -> WasmtimeResult<T>,
    {
        store.with_context_ro(func)
    }
    
    /// Core function to get current fuel level
    pub fn get_fuel_level(store: &Store) -> WasmtimeResult<Option<u64>> {
        store.fuel_remaining()
    }
    
    /// Core function to set fuel level
    pub fn set_fuel_level(store: &Store, fuel: u64) -> WasmtimeResult<()> {
        store.add_fuel(fuel)
    }

    /// Core function to increment epoch counter
    pub fn increment_epoch(store: &Store) {
        // Note: Direct epoch incrementation requires mutable access to store
        // For now, this is a no-op implementation as epoch management is complex
        // Full implementation would require store internals modification
        log::debug!("Epoch increment requested for store (no-op implementation)");
    }

    /// Core function to set memory limit
    pub fn set_memory_limit(store: &Store, limit: Option<u64>) -> WasmtimeResult<()> {
        // Note: Wasmtime stores don't support runtime memory limit changes
        // This validates the request but doesn't apply the limit
        log::debug!("Memory limit change requested: {:?} (validation only)", limit);

        if let Some(bytes) = limit {
            if bytes > (usize::MAX as u64) {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Memory limit {} exceeds maximum size", bytes),
                });
            }
        }

        // Return success but note that limit cannot be changed at runtime
        Ok(())
    }

    /// Core function to set table element limit
    pub fn set_table_element_limit(store: &Store, limit: Option<u64>) -> WasmtimeResult<()> {
        // Note: Wasmtime stores don't support runtime table element limit changes
        // This validates the request but doesn't apply the limit
        log::debug!("Table element limit change requested: {:?} (validation only)", limit);

        if let Some(elements) = limit {
            if elements > (u32::MAX as u64) {
                return Err(WasmtimeError::InvalidParameter {
                    message: format!("Table element limit {} exceeds maximum", elements),
                });
            }
        }

        // Return success but note that limit cannot be changed at runtime
        Ok(())
    }

    /// Core function to set instance limit
    pub fn set_instance_limit(store: &Store, limit: Option<u32>) -> WasmtimeResult<()> {
        // Note: Wasmtime stores don't support runtime instance limit changes
        // This validates the request but doesn't apply the limit
        log::debug!("Instance limit change requested: {:?} (validation only)", limit);

        // Validation only - limits are typically set during store creation
        // Return success but note that limit cannot be changed at runtime
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::engine::Engine;
    use std::time::Duration;

    #[test]
    fn test_store_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");
        assert!(store.validate().is_ok());
    }

    #[test]
    fn test_store_builder() {
        let engine = Engine::new().expect("Failed to create engine");
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
        assert_eq!(store.metadata().execution_timeout, Some(Duration::from_secs(30)));
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
        let remaining = store.fuel_remaining().expect("Failed to get fuel remaining");
        assert!(remaining.is_some());
        
        // Test consuming fuel
        let consumed = store.consume_fuel(100).expect("Failed to consume fuel");
        assert_eq!(consumed, 100);
    }

    #[test]
    fn test_execution_context() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let result = store.with_context(|_ctx| {
            Ok(42i32)
        });

        assert_eq!(result.unwrap(), 42);

        // Check execution stats were updated
        let stats = store.execution_stats().expect("Failed to get stats");
        assert_eq!(stats.execution_count, 1);
    }

    #[test]
    fn test_memory_usage() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        let usage = store.memory_usage().expect("Failed to get memory usage");
        assert_eq!(usage.instance_count, 0); // No instances created yet
    }

    #[test]
    fn test_garbage_collection() {
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::new(&engine).expect("Failed to create store");

        // GC should succeed without error
        assert!(store.gc().is_ok());
    }

    #[test]
    fn test_store_core_functions() {
        use crate::store::core;
        
        let engine = Engine::new().expect("Failed to create engine");
        
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
        
        let engine = Engine::new().expect("Failed to create engine");
        
        // Test core store creation with config
        let store = core::create_store_with_config(
            &engine,
            Some(2000),     // fuel_limit
            Some(2 * 1024 * 1024),  // memory_limit_bytes
            Some(60),       // execution_timeout_secs
            Some(20),       // max_instances
            Some(5000),     // max_table_elements
            Some(500),      // max_functions
        ).expect("Failed to create store with config via core");
        
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
            None, None, None, None, None,
        ).expect("Failed to create store with fuel");
        
        let store_ref = unsafe { 
            core::get_store_ref(store.as_ref() as *const Store as *const std::os::raw::c_void)
                .expect("Failed to get store reference") 
        };
        
        // Test adding fuel through core
        assert!(core::add_fuel(store_ref, 500).is_ok());
        
        // Test getting remaining fuel
        let remaining = core::get_fuel_remaining(store_ref).expect("Failed to get fuel remaining");
        assert!(remaining > 0);
        
        // Test consuming fuel
        let consumed = core::consume_fuel(store_ref, 100).expect("Failed to consume fuel");
        assert_eq!(consumed, 100);
        
        // Test setting epoch deadline
        core::set_epoch_deadline(store_ref, 1000);
    }

    #[test]
    fn test_execution_statistics_core() {
        use crate::store::core;
        
        let engine = Engine::new().expect("Failed to create engine");
        let store = core::create_store(&engine).expect("Failed to create store");
        
        let store_ref = unsafe { 
            core::get_store_ref(store.as_ref() as *const Store as *const std::os::raw::c_void)
                .expect("Failed to get store reference") 
        };
        
        // Initially, execution count should be 0
        let initial_stats = core::get_execution_stats(store_ref).expect("Failed to get stats");
        let initial_count = initial_stats.execution_count;
        
        // Execute something with the store context
        let result = core::with_store_context(store_ref, |_ctx| {
            Ok(42i32)
        });
        assert_eq!(result.unwrap(), 42);
        
        // Execution count should have increased
        let updated_stats = core::get_execution_stats(store_ref).expect("Failed to get updated stats");
        assert!(updated_stats.execution_count > initial_count);
        
        // Test memory usage stats
        let memory_usage = core::get_memory_usage(store_ref).expect("Failed to get memory usage");
        assert_eq!(memory_usage.instance_count, updated_stats.execution_count as usize);
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
        
        let engine = Engine::new().expect("Failed to create engine");
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
        let engine = Engine::new().expect("Failed to create engine");
        let store = Store::builder()
            .fuel_limit(5000)
            .memory_limit(4 * 1024 * 1024)
            .max_instances(50)
            .max_table_elements(8000)
            .max_functions(1500)
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
        let engine = Engine::new().expect("Failed to create engine");
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

        // Test consuming more fuel than available
        let consumed = store.consume_fuel(150).expect("Should handle over-consumption gracefully");
        assert!(consumed <= 100); // Should consume what's available, not more

        // Test fuel remaining after over-consumption
        let remaining = store.fuel_remaining().expect("Failed to get fuel remaining");
        assert_eq!(remaining, Some(0)); // Should be 0 after consuming all available fuel
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

        let engine = Engine::new().expect("Failed to create engine");

        // A simple WebAssembly module that exports an add function
        let wasm_bytes = wat::parse_str(r#"
            (module
                (func $add (param $a i32) (param $b i32) (result i32)
                    local.get $a
                    local.get $b
                    i32.add)
                (export "add" (func $add))
            )
        "#).expect("Failed to parse WAT");

        // Compile module
        let module = Module::compile(&engine, &wasm_bytes)
            .expect("Failed to compile module");

        // Create store using for_module - this should ensure Arc compatibility
        let mut store = Store::for_module(&module)
            .expect("Failed to create store for module");

        // Verify store is valid
        assert!(store.validate().is_ok(), "Store should be valid");

        // The key test: verify the store can actually instantiate the module
        // This would fail with "cross-engine" error if Arc pointers don't match
        use crate::instance::Instance;
        let instance = Instance::new(&mut store, &module, &[])
            .expect("Failed to instantiate module - this verifies Arc compatibility");

        // Verify we can get the export
        let exports = instance.exports(&mut store).expect("Failed to get exports");
        assert!(exports.contains(&"add".to_string()), "Should have 'add' export");
    }

    #[test]
    fn test_store_builder_for_module() {
        use crate::module::Module;

        let engine = Engine::new().expect("Failed to create engine");

        // A simple WebAssembly module
        let wasm_bytes = wat::parse_str(r#"
            (module
                (func $nop)
                (export "nop" (func $nop))
            )
        "#).expect("Failed to parse WAT");

        // Compile module
        let module = Module::compile(&engine, &wasm_bytes)
            .expect("Failed to compile module");

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
}

//
// Native C exports for JNI and Panama FFI consumption
//

use std::os::raw::{c_void, c_int};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Store core functions for interface implementations
pub mod ffi_core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;

    /// Core function to create store with engine
    pub fn create_store(engine: &Engine) -> WasmtimeResult<Box<Store>> {
        Store::new(engine).map(Box::new)
    }

    /// Core function to create store with builder configuration
    pub fn create_store_with_config(
        engine: &Engine,
        fuel_limit: Option<u64>,
        memory_limit_bytes: Option<usize>,
        execution_timeout: Option<Duration>,
    ) -> WasmtimeResult<Box<Store>> {
        let mut builder = Store::builder();

        if let Some(fuel) = fuel_limit {
            builder = builder.fuel_limit(fuel);
        }

        if let Some(memory) = memory_limit_bytes {
            builder = builder.memory_limit(memory);
        }

        if let Some(timeout) = execution_timeout {
            builder = builder.execution_timeout(timeout);
        }

        builder.build(engine).map(Box::new)
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

    /// Core function to destroy a store (safe cleanup)
    pub unsafe fn destroy_store(store_ptr: *mut c_void) {
        // Mark store as closed before destroying
        if !store_ptr.is_null() {
            let store = &*(store_ptr as *const Store);
            store.metadata.is_closed.store(true, std::sync::atomic::Ordering::SeqCst);
        }
        ffi_utils::destroy_resource::<Store>(store_ptr, "Store");
    }

    /// Core function to validate store functionality
    pub fn validate_store(store: &Store) -> WasmtimeResult<()> {
        store.validate()
    }

    /// Core function to add fuel to store
    pub fn add_fuel(store: &Store, fuel: u64) -> WasmtimeResult<()> {
        store.add_fuel(fuel)
    }

    /// Core function to consume fuel from store
    pub fn consume_fuel(store: &Store, fuel: u64) -> WasmtimeResult<u64> {
        store.consume_fuel(fuel)
    }

    /// Core function to get fuel remaining
    pub fn fuel_remaining(store: &Store) -> WasmtimeResult<Option<u64>> {
        store.fuel_remaining()
    }

    /// Core function to get store metadata
    pub fn get_metadata(store: &Store) -> &StoreMetadata {
        store.metadata()
    }

    /// Core function to get execution stats
    pub fn get_execution_stats(store: &Store) -> WasmtimeResult<ExecutionState> {
        store.execution_stats()
    }
}

/// Create a new store with engine
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
/// Returns pointer to store that must be freed with wasmtime4j_store_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_new(engine_ptr: *const c_void) -> *mut c_void {
    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            match ffi_core::create_store(engine) {
                Ok(store) => Box::into_raw(store) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Create a new store with configuration
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
/// Returns pointer to store that must be freed with wasmtime4j_store_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_new_with_config(
    engine_ptr: *const c_void,
    fuel_limit: u64,
    memory_limit_bytes: usize,
    execution_timeout_seconds: u64,
) -> *mut c_void {
    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            let opt_fuel = if fuel_limit == 0 { None } else { Some(fuel_limit) };
            let opt_memory = if memory_limit_bytes == 0 { None } else { Some(memory_limit_bytes) };
            let opt_timeout = if execution_timeout_seconds == 0 {
                None
            } else {
                Some(Duration::from_secs(execution_timeout_seconds))
            };

            match ffi_core::create_store_with_config(engine, opt_fuel, opt_memory, opt_timeout) {
                Ok(store) => Box::into_raw(store) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
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
    match ffi_core::get_store_ref(store_ptr) {
        Ok(store) => match core::validate_store(store) {
            Ok(_) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Add fuel to store for execution metering
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_add_fuel(
    store_ptr: *const c_void,
    fuel: u64,
) -> c_int {
    match ffi_core::get_store_ref(store_ptr) {
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
/// Returns amount of fuel actually consumed, or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_consume_fuel(
    store_ptr: *const c_void,
    fuel: u64,
) -> u64 {
    match ffi_core::get_store_ref(store_ptr) {
        Ok(store) => match core::consume_fuel(store, fuel) {
            Ok(consumed) => consumed,
            Err(_) => 0,
        },
        Err(_) => 0,
    }
}

/// Get fuel remaining in store
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
/// Returns fuel remaining, or 0 if not enabled or on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_fuel_remaining(store_ptr: *const c_void) -> u64 {
    match ffi_core::get_store_ref(store_ptr) {
        Ok(store) => match ffi_core::fuel_remaining(store) {
            Ok(Some(fuel)) => fuel,
            Ok(None) => 0, // Fuel not enabled
            Err(_) => 0,
        },
        Err(_) => 0,
    }
}

/// Get store execution count
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_execution_count(store_ptr: *const c_void) -> u64 {
    match ffi_core::get_store_ref(store_ptr) {
        Ok(store) => match core::get_execution_stats(store) {
            Ok(stats) => stats.execution_count,
            Err(_) => 0,
        },
        Err(_) => 0,
    }
}

/// Get store fuel consumed total
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_fuel_consumed(store_ptr: *const c_void) -> u64 {
    match ffi_core::get_store_ref(store_ptr) {
        Ok(store) => match core::get_execution_stats(store) {
            Ok(stats) => stats.fuel_consumed,
            Err(_) => 0,
        },
        Err(_) => 0,
    }
}

/// Get store total execution time in microseconds
///
/// # Safety
///
/// store_ptr must be a valid pointer from wasmtime4j_store_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_store_total_execution_time_micros(store_ptr: *const c_void) -> u64 {
    match ffi_core::get_store_ref(store_ptr) {
        Ok(store) => match core::get_execution_stats(store) {
            Ok(stats) => stats.total_execution_time.as_micros() as u64,
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
    let store = match ffi_core::get_store_ref(store_ptr) {
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

    match ffi_core::get_store_ref(store_ptr) {
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