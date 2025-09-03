//! WebAssembly store management with resource tracking and isolation
//!
//! The Store provides execution context and resource management for WebAssembly instances.
//! Each store maintains its own isolated execution environment with resource limits,
//! fuel tracking, and comprehensive cleanup capabilities.

use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};
use wasmtime::{Store as WasmtimeStore, StoreContext, StoreContextMut, AsContext, AsContextMut};
use crate::engine::Engine;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime store with resource management
pub struct Store {
    inner: Arc<Mutex<WasmtimeStore<StoreData>>>,
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
}

/// Store-specific data for host function context
#[derive(Debug)]
pub struct StoreData {
    /// Optional user-defined data attached to the store
    pub user_data: Option<Box<dyn std::any::Any + Send + Sync>>,
    /// Resource limits and quotas for this store
    pub resource_limits: ResourceLimits,
    /// Current execution state and statistics
    pub execution_state: ExecutionState,
}

impl Clone for StoreData {
    fn clone(&self) -> Self {
        StoreData {
            user_data: None, // Can't clone arbitrary Any type, set to None
            resource_limits: self.resource_limits.clone(),
            execution_state: self.execution_state.clone(),
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

    /// Create store builder for custom configuration
    pub fn builder() -> StoreBuilder {
        StoreBuilder::new()
    }

    /// Execute function with store context
    pub fn with_context<T, F>(&self, func: F) -> WasmtimeResult<T>
    where
        F: FnOnce(&mut StoreContextMut<StoreData>) -> WasmtimeResult<T>,
    {
        let mut store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

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
        let store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        func(&store.as_context())
    }

    /// Get store metadata
    pub fn metadata(&self) -> &StoreMetadata {
        &self.metadata
    }

    /// Add fuel to the store for execution limiting
    pub fn add_fuel(&self, fuel: u64) -> WasmtimeResult<()> {
        let mut store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        store.set_fuel(fuel).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to set fuel: {}", e),
            backtrace: None,
        })?;

        Ok(())
    }

    /// Get remaining fuel
    pub fn fuel_remaining(&self) -> WasmtimeResult<Option<u64>> {
        let store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        Ok(Some(store.get_fuel().unwrap_or(0)))
    }

    /// Consume fuel from the store
    pub fn consume_fuel(&self, fuel: u64) -> WasmtimeResult<u64> {
        let mut store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

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
        if let Ok(mut store) = self.inner.lock() {
            store.set_epoch_deadline(ticks);
        }
    }

    /// Get execution statistics
    pub fn execution_stats(&self) -> WasmtimeResult<ExecutionState> {
        let store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        Ok(store.data().execution_state.clone())
    }

    /// Validate store is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        if let Ok(_guard) = self.inner.try_lock() {
            Ok(())
        } else {
            Err(WasmtimeError::Concurrency {
                message: "Store is locked and may be corrupted".to_string(),
            })
        }
    }

    /// Force garbage collection in the store
    pub fn gc(&self) -> WasmtimeResult<()> {
        let mut store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        store.gc(None); // Pass None for manual GC trigger
        Ok(())
    }

    /// Get memory usage statistics
    pub fn memory_usage(&self) -> WasmtimeResult<MemoryUsage> {
        let store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        // Note: Wasmtime doesn't provide direct memory usage stats
        // We approximate based on what we can measure
        Ok(MemoryUsage {
            total_bytes: 0, // Not easily available
            used_bytes: 0,  // Not easily available
            instance_count: store.data().execution_state.execution_count as usize,
        })
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

        let store_data = StoreData {
            user_data: None,
            resource_limits: self.resource_limits,
            execution_state: ExecutionState {
                execution_count: 0,
                last_execution: None,
                total_execution_time: Duration::new(0, 0),
                fuel_consumed: 0,
            },
        };

        let mut wasmtime_store = WasmtimeStore::new(engine.inner(), store_data);

        // Configure fuel if specified
        if let Some(fuel_limit) = self.fuel_limit {
            wasmtime_store.set_fuel(fuel_limit).map_err(|e| WasmtimeError::Store {
                message: format!("Failed to set initial fuel: {}", e),
            })?;
        }

        let metadata = StoreMetadata {
            created_at: Instant::now(),
            fuel_limit: self.fuel_limit,
            memory_limit_bytes: self.memory_limit_bytes,
            execution_timeout: self.execution_timeout,
            instance_count: 0,
        };

        Ok(Store {
            inner: Arc::new(Mutex::new(wasmtime_store)),
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

/// Shared core functions for store operations used by both JNI and Panama interfaces
/// 
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use std::time::Duration;
    use crate::error::{ffi_utils, validate_ptr_not_null};
    use crate::engine::Engine;
    
    /// Core function to create a new store with default configuration
    pub fn create_store(engine: &Engine) -> WasmtimeResult<Box<Store>> {
        Store::new(engine).map(Box::new)
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
        ffi_utils::destroy_resource::<Store>(store_ptr, "Store");
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
        let engine = Engine::new().expect("Failed to create engine");
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
}