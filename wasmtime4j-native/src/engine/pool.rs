//! Engine pooling and singleton management
//!
//! This module implements engine pooling to mitigate wasmtime's GLOBAL_CODE
//! registry scalability issue. When many engines are created and destroyed,
//! wasmtime's internal registry can accumulate stale entries that eventually
//! cause SIGABRT crashes.
//!
//! By reusing engines instead of creating new ones, we dramatically reduce
//! the number of GLOBAL_CODE registrations and avoid the crash.

use std::any::Any;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, Mutex, OnceLock};

use once_cell::sync::Lazy;
use wasmtime::Engine as WasmtimeEngine;

use super::{safe_wasmtime_config, Engine};
use crate::error::WasmtimeResult;

/// Maximum number of engines to keep in the pool.
/// Beyond this limit, released engines are dropped normally.
pub(crate) const ENGINE_POOL_MAX_SIZE: usize = 16;

/// The shared singleton engine for default use cases.
/// This engine is lazily initialized on first access and reused for all
/// callers who don't need custom configuration.
///
/// Uses OnceLock with Result to prevent panics on initialization failure.
/// If initialization fails, the error is stored and returned on each access.
static SHARED_ENGINE: OnceLock<Result<Engine, String>> = OnceLock::new();

/// Internal helper to initialize the shared engine safely.
fn init_shared_engine() -> Result<Engine, String> {
    Engine::new().map_err(|e| format!("Failed to create shared singleton engine: {}", e))
}

/// Shared wasmtime::Engine with component model enabled.
/// Used by ComponentEngine and EnhancedComponentEngine.
///
/// Uses OnceLock with Result to prevent panics on initialization failure.
pub(crate) static SHARED_COMPONENT_WASMTIME_ENGINE: OnceLock<Result<WasmtimeEngine, String>> =
    OnceLock::new();

/// Internal helper to initialize the shared component wasmtime engine safely.
pub(crate) fn init_shared_component_wasmtime_engine() -> Result<WasmtimeEngine, String> {
    let mut config = safe_wasmtime_config();
    config.wasm_component_model(true);
    config.wasm_simd(true);
    config.wasm_bulk_memory(true);
    config.wasm_multi_value(true);
    config.wasm_reference_types(true);
    WasmtimeEngine::new(&config)
        .map_err(|e| format!("Failed to create shared component wasmtime engine: {}", e))
}

/// Shared wasmtime::Engine with GC enabled.
/// Used by GC operations.
///
/// Uses OnceLock with Result to prevent panics on initialization failure.
pub(crate) static SHARED_GC_WASMTIME_ENGINE: OnceLock<Result<WasmtimeEngine, String>> =
    OnceLock::new();

/// Internal helper to initialize the shared GC wasmtime engine safely.
pub(crate) fn init_shared_gc_wasmtime_engine() -> Result<WasmtimeEngine, String> {
    let mut config = safe_wasmtime_config();
    config.wasm_gc(true);
    config.wasm_reference_types(true);
    config.wasm_function_references(true);
    WasmtimeEngine::new(&config)
        .map_err(|e| format!("Failed to create shared GC wasmtime engine: {}", e))
}

/// Shared wasmtime::Engine with async support enabled.
/// Used by WASI Preview 2 and other async operations in tests.
///
/// Uses OnceLock with Result to prevent panics on initialization failure.
pub(crate) static SHARED_ASYNC_WASMTIME_ENGINE: OnceLock<Result<WasmtimeEngine, String>> =
    OnceLock::new();

/// Internal helper to initialize the shared async wasmtime engine safely.
pub(crate) fn init_shared_async_wasmtime_engine() -> Result<WasmtimeEngine, String> {
    let mut config = safe_wasmtime_config();
    config.wasm_component_model(true);
    WasmtimeEngine::new(&config)
        .map_err(|e| format!("Failed to create shared async wasmtime engine: {}", e))
}

/// Returns a clone of the shared component wasmtime::Engine.
///
/// # Panics
/// Logs an error and returns a fallback engine if initialization failed.
/// This maintains API compatibility while preventing silent failures.
pub fn get_shared_component_wasmtime_engine() -> WasmtimeEngine {
    let result =
        SHARED_COMPONENT_WASMTIME_ENGINE.get_or_init(init_shared_component_wasmtime_engine);
    match result {
        Ok(engine) => engine.clone(),
        Err(e) => {
            log::error!(
                "Shared component engine initialization failed: {}. Creating fallback.",
                e
            );
            // Create a minimal fallback engine - this is a last resort
            let mut config = safe_wasmtime_config();
            config.wasm_component_model(true);
            WasmtimeEngine::new(&config).unwrap_or_else(|_| {
                // Ultimate fallback: basic config
                WasmtimeEngine::new(&safe_wasmtime_config())
                    .expect("Failed to create even basic fallback engine")
            })
        }
    }
}

/// Returns a clone of the shared GC wasmtime::Engine.
///
/// # Panics
/// Logs an error and returns a fallback engine if initialization failed.
pub fn get_shared_gc_wasmtime_engine() -> WasmtimeEngine {
    let result = SHARED_GC_WASMTIME_ENGINE.get_or_init(init_shared_gc_wasmtime_engine);
    match result {
        Ok(engine) => engine.clone(),
        Err(e) => {
            log::error!(
                "Shared GC wasmtime engine initialization failed: {}. Creating fallback.",
                e
            );
            let mut config = safe_wasmtime_config();
            config.wasm_gc(true);
            config.wasm_reference_types(true);
            config.wasm_function_references(true);
            WasmtimeEngine::new(&config).unwrap_or_else(|_| {
                WasmtimeEngine::new(&safe_wasmtime_config())
                    .expect("Failed to create even basic fallback engine")
            })
        }
    }
}

/// Returns a clone of the shared async wasmtime::Engine.
/// Used by WASI Preview 2 and other async operations in tests.
///
/// # Panics
/// Logs an error and returns a fallback engine if initialization failed.
pub fn get_shared_async_wasmtime_engine() -> WasmtimeEngine {
    let result = SHARED_ASYNC_WASMTIME_ENGINE.get_or_init(init_shared_async_wasmtime_engine);
    match result {
        Ok(engine) => engine.clone(),
        Err(e) => {
            log::error!(
                "Shared async wasmtime engine initialization failed: {}. Creating fallback.",
                e
            );
            let mut config = safe_wasmtime_config();
            config.wasm_component_model(true);
            WasmtimeEngine::new(&config).unwrap_or_else(|_| {
                WasmtimeEngine::new(&safe_wasmtime_config())
                    .expect("Failed to create even basic fallback engine")
            })
        }
    }
}

/// Shared wasmtime::Engine with async + component model async support.
/// Used for concurrent component function calls via `call_concurrent`.
///
/// Uses OnceLock with Result to prevent panics on initialization failure.
pub(crate) static SHARED_CONCURRENT_COMPONENT_ENGINE: OnceLock<Result<WasmtimeEngine, String>> =
    OnceLock::new();

/// Internal helper to initialize the shared concurrent component engine safely.
pub(crate) fn init_shared_concurrent_component_engine() -> Result<WasmtimeEngine, String> {
    let mut config = safe_wasmtime_config();
    config.wasm_component_model(true);
    config.wasm_component_model_async(true);
    config.concurrency_support(true);
    config.wasm_simd(true);
    config.wasm_bulk_memory(true);
    config.wasm_multi_value(true);
    config.wasm_reference_types(true);
    WasmtimeEngine::new(&config)
        .map_err(|e| format!("Failed to create concurrent component engine: {}", e))
}

/// Returns a clone of the shared concurrent component wasmtime::Engine.
/// Used for concurrent component function calls.
///
/// # Panics
/// Logs an error and returns a fallback engine if initialization failed.
pub fn get_shared_concurrent_component_engine() -> WasmtimeEngine {
    let result = SHARED_CONCURRENT_COMPONENT_ENGINE
        .get_or_init(init_shared_concurrent_component_engine);
    match result {
        Ok(engine) => engine.clone(),
        Err(e) => {
            log::error!(
                "Concurrent component engine initialization failed: {}. Creating fallback.",
                e
            );
            let mut config = safe_wasmtime_config();
            config.wasm_component_model(true);
            config.wasm_component_model_async(true);
            config.concurrency_support(true);
            WasmtimeEngine::new(&config).unwrap_or_else(|_| {
                WasmtimeEngine::new(&safe_wasmtime_config())
                    .expect("Failed to create even basic fallback engine")
            })
        }
    }
}

/// Returns a clone of the shared wasmtime::Engine.
/// Defaults to the component engine which has commonly needed features.
pub fn get_shared_wasmtime_engine() -> WasmtimeEngine {
    get_shared_component_wasmtime_engine()
}

/// Pool of reusable engines with default configuration.
/// Engines returned to the pool can be reused by subsequent callers,
/// reducing the total number of engine creations over time.
static ENGINE_POOL: Lazy<Mutex<Vec<Engine>>> =
    Lazy::new(|| Mutex::new(Vec::with_capacity(ENGINE_POOL_MAX_SIZE)));

/// Returns a clone of the shared singleton engine.
///
/// This is the recommended way to get an engine for most use cases.
/// The singleton is created once and reused, avoiding repeated engine
/// creation that can trigger wasmtime's GLOBAL_CODE registry issues.
///
/// # Example
/// ```
/// use wasmtime4j::engine::get_shared_engine;
///
/// let engine = get_shared_engine();
/// // Use engine for module compilation, store creation, etc.
/// ```
///
/// # Thread Safety
/// The returned engine is safe to use from multiple threads concurrently.
/// The underlying wasmtime engine is thread-safe.
///
/// # Panics
/// Logs an error and creates a fallback engine if initialization failed.
pub fn get_shared_engine() -> Engine {
    let result = SHARED_ENGINE.get_or_init(init_shared_engine);
    match result {
        Ok(engine) => engine.clone(),
        Err(e) => {
            log::error!(
                "Shared singleton engine initialization failed: {}. Creating fallback.",
                e
            );
            // Try to create a new engine as fallback
            Engine::new().unwrap_or_else(|_| {
                // Ultimate fallback with minimal config
                Engine::with_config(safe_wasmtime_config())
                    .expect("Failed to create even basic fallback engine")
            })
        }
    }
}

/// Acquires an engine from the pool, or creates a new one if the pool is empty.
///
/// Use this when you need an engine with default configuration but want
/// explicit control over its lifecycle. Call `release_pooled_engine()` when
/// done to return it to the pool for reuse.
///
/// # Example
/// ```
/// use wasmtime4j::engine::{acquire_pooled_engine, release_pooled_engine};
///
/// let engine = acquire_pooled_engine();
/// // Use engine...
/// release_pooled_engine(engine); // Return to pool for reuse
/// ```
///
/// # Returns
/// An engine with default configuration, either from the pool or newly created.
pub fn acquire_pooled_engine() -> Engine {
    let mut pool = ENGINE_POOL.lock().unwrap_or_else(|e| {
        log::warn!("Engine pool mutex was poisoned, recovering");
        e.into_inner()
    });

    pool.pop().unwrap_or_else(|| {
        // Pool is empty, create a new engine
        Engine::new().unwrap_or_else(|e| {
            log::error!("Failed to create pooled engine: {:?}", e);
            // Fall back to shared engine clone as last resort
            get_shared_engine()
        })
    })
}

/// Returns an engine to the pool for reuse.
///
/// If the pool is at capacity (`ENGINE_POOL_MAX_SIZE`), the engine is
/// dropped normally instead of being pooled.
///
/// # Arguments
/// * `engine` - The engine to return to the pool
///
/// # Example
/// ```
/// use wasmtime4j::engine::{acquire_pooled_engine, release_pooled_engine};
///
/// let engine = acquire_pooled_engine();
/// // Use engine...
/// release_pooled_engine(engine);
/// ```
pub fn release_pooled_engine(engine: Engine) {
    let mut pool = ENGINE_POOL.lock().unwrap_or_else(|e| {
        log::warn!("Engine pool mutex was poisoned, recovering");
        e.into_inner()
    });

    if pool.len() < ENGINE_POOL_MAX_SIZE {
        pool.push(engine);
    }
    // else: pool is full, let the engine drop naturally
}

/// Clears the engine pool, dropping all pooled engines.
///
/// This can be called to force cleanup of pooled engines, which may help
/// in scenarios where wasmtime's GLOBAL_CODE registry needs to be cleared.
/// After calling this, subsequent `acquire_pooled_engine()` calls will
/// create new engines.
///
/// Note: This does NOT affect the shared singleton engine.
///
/// # Example
/// ```
/// use wasmtime4j::engine::engine_pool_cleanup;
///
/// // After running many tests...
/// engine_pool_cleanup();
/// ```
pub fn engine_pool_cleanup() {
    let mut pool = ENGINE_POOL.lock().unwrap_or_else(|e| {
        log::warn!("Engine pool mutex was poisoned, recovering");
        e.into_inner()
    });

    let count = pool.len();
    pool.clear();

    if count > 0 {
        log::debug!("Cleared {} engines from pool", count);
    }
}

/// Returns the current number of engines in the pool.
///
/// This is primarily useful for testing and monitoring.
pub fn engine_pool_size() -> usize {
    ENGINE_POOL.lock().unwrap_or_else(|e| e.into_inner()).len()
}

/// Returns the maximum capacity of the engine pool.
pub fn engine_pool_max_size() -> usize {
    ENGINE_POOL_MAX_SIZE
}

/// Performs a full cleanup of wasmtime resources.
///
/// This function should be called periodically during long-running processes
/// or after running many tests to help mitigate wasmtime's GLOBAL_CODE
/// registry accumulation issue.
///
/// What this does:
/// 1. Clears the engine pool, dropping all pooled engines
/// 2. Yields to allow pending deallocations to complete
/// 3. Optionally triggers a garbage collection hint (on supported platforms)
///
/// Note: This does NOT affect the shared singleton engine, as that would
/// break existing references. If you need to reset absolutely everything,
/// the process must be restarted.
///
/// # Example
/// ```
/// use wasmtime4j::engine::wasmtime_full_cleanup;
///
/// // After running a batch of tests...
/// wasmtime_full_cleanup();
/// ```
pub fn wasmtime_full_cleanup() {
    // Step 1: Clear the engine pool
    engine_pool_cleanup();

    // Step 2: Yield to allow pending Arc deallocations to complete
    // This gives Rust's allocator a chance to process drops
    std::thread::yield_now();

    // Step 3: Small sleep to allow OS to reclaim memory mappings
    // This helps ensure mmap address space is freed before new allocations
    std::thread::sleep(std::time::Duration::from_millis(1));

    log::debug!("wasmtime_full_cleanup completed");
}

// =============================================================================
// Managed Engine with Proper Drop Order
// =============================================================================
//
// The ManagedEngine ensures that all resources created from an engine
// (modules, stores, instances) are dropped BEFORE the engine itself.
// This prevents issues where lingering Arc references keep wasmtime's
// GLOBAL_CODE registry entries alive.

/// Counter for generating unique managed engine IDs
static MANAGED_ENGINE_COUNTER: AtomicU64 = AtomicU64::new(0);

/// A managed engine wrapper that enforces proper resource cleanup order.
///
/// When dropped, a `ManagedEngine` ensures all associated resources
/// (modules, stores, instances) are dropped BEFORE the underlying engine.
/// This prevents wasmtime's GLOBAL_CODE registry issues caused by
/// out-of-order deallocation.
///
/// # Example
/// ```text
/// use wasmtime4j::engine::ManagedEngine;
///
/// let managed = ManagedEngine::new()?;
/// let engine = managed.engine();
/// // Create modules, stores, etc. from engine...
/// // When managed is dropped, all tracked resources are cleaned up first
/// ```
pub struct ManagedEngine {
    /// Unique ID for this managed engine
    id: u64,
    /// The underlying engine
    engine: Engine,
    /// Tracked resources that must be dropped before the engine
    /// Uses Box<dyn Any> to allow storing different resource types
    resources: Mutex<Vec<Box<dyn Any + Send>>>,
}

impl ManagedEngine {
    /// Creates a new managed engine with default configuration.
    pub fn new() -> WasmtimeResult<Self> {
        Ok(Self {
            id: MANAGED_ENGINE_COUNTER.fetch_add(1, Ordering::SeqCst),
            engine: Engine::new()?,
            resources: Mutex::new(Vec::new()),
        })
    }

    /// Creates a new managed engine from the shared singleton.
    ///
    /// Note: The singleton engine itself is not managed and will outlive
    /// this ManagedEngine. Only the tracked resources are cleaned up.
    pub fn from_shared() -> Self {
        Self {
            id: MANAGED_ENGINE_COUNTER.fetch_add(1, Ordering::SeqCst),
            engine: get_shared_engine(),
            resources: Mutex::new(Vec::new()),
        }
    }

    /// Creates a new managed engine from an existing engine.
    pub fn from_engine(engine: Engine) -> Self {
        Self {
            id: MANAGED_ENGINE_COUNTER.fetch_add(1, Ordering::SeqCst),
            engine,
            resources: Mutex::new(Vec::new()),
        }
    }

    /// Returns a reference to the underlying engine.
    pub fn engine(&self) -> &Engine {
        &self.engine
    }

    /// Returns a clone of the underlying engine.
    pub fn engine_clone(&self) -> Engine {
        self.engine.clone()
    }

    /// Returns the unique ID of this managed engine.
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Tracks a resource for cleanup when this managed engine is dropped.
    ///
    /// Resources are dropped in reverse order of registration (LIFO).
    ///
    /// # Arguments
    /// * `resource` - Any Send resource that should be cleaned up with this engine
    pub fn track_resource<T: Any + Send + 'static>(&self, resource: T) {
        let mut resources = self.resources.lock().unwrap_or_else(|e| {
            log::warn!("ManagedEngine resources mutex was poisoned, recovering");
            e.into_inner()
        });
        resources.push(Box::new(resource));
    }

    /// Tracks a resource and returns a clone/reference for use.
    ///
    /// This is a convenience method for tracking Arc-wrapped resources.
    ///
    /// # Arguments
    /// * `resource` - An Arc-wrapped resource to track
    ///
    /// # Returns
    /// A clone of the Arc for the caller to use
    pub fn track_arc<T: Any + Send + Sync + 'static>(&self, resource: Arc<T>) -> Arc<T> {
        let clone = Arc::clone(&resource);
        self.track_resource(resource);
        clone
    }

    /// Returns the number of tracked resources.
    pub fn resource_count(&self) -> usize {
        self.resources
            .lock()
            .unwrap_or_else(|e| e.into_inner())
            .len()
    }

    /// Explicitly clears all tracked resources.
    ///
    /// This can be called to force early cleanup without dropping the engine.
    pub fn clear_resources(&self) {
        let mut resources = self.resources.lock().unwrap_or_else(|e| {
            log::warn!("ManagedEngine resources mutex was poisoned, recovering");
            e.into_inner()
        });

        let count = resources.len();

        // Drop in reverse order (LIFO)
        while resources.pop().is_some() {}

        if count > 0 {
            log::debug!("ManagedEngine {} cleared {} resources", self.id, count);
        }
    }
}

impl Default for ManagedEngine {
    fn default() -> Self {
        // Use from_shared() which has fallback protection, avoiding panic
        Self::from_shared()
    }
}

impl Drop for ManagedEngine {
    fn drop(&mut self) {
        // First, clear all tracked resources (in reverse order)
        self.clear_resources();

        // Then yield to allow pending Arc deallocations
        std::thread::yield_now();

        log::debug!("ManagedEngine {} dropped", self.id);

        // The engine will now drop naturally after this
    }
}

impl std::fmt::Debug for ManagedEngine {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ManagedEngine")
            .field("id", &self.id)
            .field("engine", &self.engine)
            .field("resource_count", &self.resource_count())
            .finish()
    }
}
