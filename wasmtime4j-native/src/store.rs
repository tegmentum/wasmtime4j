//! WebAssembly store management with resource tracking and isolation
//!
//! The Store provides execution context and resource management for WebAssembly instances.
//! Each store maintains its own isolated execution environment with resource limits,
//! fuel tracking, and comprehensive cleanup capabilities.

use std::sync::{Arc, Mutex};
use std::time::{Duration, Instant};
use wasmtime::{Store as WasmtimeStore, Caller, StoreContext, StoreContextMut, AsContext, AsContextMut};
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
    pub created_at: Instant,
    pub fuel_limit: Option<u64>,
    pub memory_limit_bytes: Option<usize>,
    pub execution_timeout: Option<Duration>,
    pub instance_count: usize,
}

/// Store-specific data for host function context
#[derive(Debug, Clone)]
pub struct StoreData {
    pub user_data: Option<Box<dyn std::any::Any + Send + Sync>>,
    pub resource_limits: ResourceLimits,
    pub execution_state: ExecutionState,
}

/// Resource limits and quotas
#[derive(Debug, Clone)]
pub struct ResourceLimits {
    pub max_memory_bytes: Option<usize>,
    pub max_table_elements: Option<u32>,
    pub max_instances: Option<usize>,
    pub max_functions: Option<usize>,
}

/// Execution state tracking
#[derive(Debug, Clone)]
pub struct ExecutionState {
    pub execution_count: u64,
    pub last_execution: Option<Instant>,
    pub total_execution_time: Duration,
    pub fuel_consumed: u64,
}

/// Builder for creating configured stores
#[derive(Debug)]
pub struct StoreBuilder {
    fuel_limit: Option<u64>,
    memory_limit_bytes: Option<usize>,
    execution_timeout: Option<Duration>,
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

        store.add_fuel(fuel).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to add fuel: {}", e),
            backtrace: None,
        })?;

        Ok(())
    }

    /// Get remaining fuel
    pub fn fuel_remaining(&self) -> WasmtimeResult<Option<u64>> {
        let store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        Ok(store.fuel_remaining())
    }

    /// Consume fuel from the store
    pub fn consume_fuel(&self, fuel: u64) -> WasmtimeResult<u64> {
        let mut store = self.inner.lock().map_err(|e| WasmtimeError::Concurrency {
            message: format!("Failed to acquire store lock: {}", e),
        })?;

        store.consume_fuel(fuel).map_err(|e| WasmtimeError::Runtime {
            message: format!("Failed to consume fuel: {}", e),
            backtrace: None,
        })
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

        store.gc();
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
            wasmtime_store.add_fuel(fuel_limit).map_err(|e| WasmtimeError::Store {
                message: format!("Failed to add initial fuel: {}", e),
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