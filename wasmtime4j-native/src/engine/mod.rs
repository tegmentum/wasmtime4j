//! Wasmtime engine management with comprehensive configuration and lifecycle support
//!
//! This module provides defensive, thread-safe wrapper around Wasmtime engines
//! with proper resource management and JVM crash prevention.

use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::{Arc, RwLock};

use wasmtime::{Config, Engine as WasmtimeEngine, Strategy};

use crate::error::{WasmtimeError, WasmtimeResult};

mod builder;
pub mod core;
mod ffi;
pub mod pool;
#[cfg(test)]
mod tests;

// Re-export public types
pub use builder::{EngineBuilder, EngineConfigSummary};
pub use pool::{
    acquire_pooled_engine, engine_pool_cleanup, engine_pool_max_size, engine_pool_size,
    get_shared_async_wasmtime_engine, get_shared_component_wasmtime_engine, get_shared_engine,
    get_shared_gc_wasmtime_engine, get_shared_wasmtime_engine, release_pooled_engine,
    wasmtime_full_cleanup, ManagedEngine,
};

// Re-export FFI functions
pub use ffi::*;

/// Returns a `wasmtime::Config` with `signals_based_traps(false)` already set.
///
/// Every `Config` / `Engine` created in this crate **must** disable signal-based
/// traps so that Wasmtime's SIGSEGV/SIGBUS handlers do not collide with the
/// JVM's own signal handlers (which would cause SIGABRT / JVM crash).
pub(crate) fn safe_wasmtime_config() -> Config {
    let mut config = Config::new();
    config.signals_based_traps(false);
    config
}

/// Thread-safe wrapper around Wasmtime engine with defensive programming
///
/// This struct includes synchronization primitives to prevent race conditions
/// when multiple threads concurrently compile modules or create stores from
/// the same engine. This is critical for JVM stability when using shared memory
/// and concurrent WebAssembly execution.
#[derive(Clone)]
pub struct Engine {
    inner: Arc<WasmtimeEngine>,
    config_summary: EngineConfigSummary,
    /// Lock for serializing concurrent operations to prevent JVM crashes.
    /// This lock is acquired during module compilation and store creation
    /// to prevent race conditions in native code when using shared memory.
    /// Uses RwLock to allow multiple readers (e.g., epoch increments) while
    /// serializing write operations (compilation, store creation).
    concurrent_ops_lock: Arc<RwLock<()>>,
    /// Flag indicating whether this engine has been closed.
    /// Shared across all clones via Arc to ensure consistent state.
    /// Uses SeqCst ordering for visibility across threads.
    is_closed: Arc<AtomicBool>,
}

impl std::fmt::Debug for Engine {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("Engine")
            .field("inner", &self.inner)
            .field("config_summary", &self.config_summary)
            .field("concurrent_ops_lock", &"<RwLock>")
            .field("is_closed", &self.is_closed.load(Ordering::SeqCst))
            .finish()
    }
}

impl Engine {
    /// Create engine with default configuration optimized for production
    pub fn new() -> WasmtimeResult<Self> {
        Self::builder().build()
    }

    /// Create engine builder for custom configuration
    pub fn builder() -> EngineBuilder {
        EngineBuilder::new()
    }

    /// Create engine with specific configuration
    pub fn with_config(mut config: Config) -> WasmtimeResult<Self> {
        // CRITICAL: Disable signal-based traps to prevent conflicts with JVM signal handlers
        // This is enforced even for external configs to ensure JVM safety
        config.signals_based_traps(false);

        let summary = EngineConfigSummary::from_config(&config);

        let engine = WasmtimeEngine::new(&config).map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to create Wasmtime engine: {}", e),
        })?;

        Ok(Engine {
            inner: Arc::new(engine),
            config_summary: summary,
            concurrent_ops_lock: Arc::new(RwLock::new(())),
            is_closed: Arc::new(AtomicBool::new(false)),
        })
    }

    /// Acquire a write lock for concurrent operations that need serialization.
    ///
    /// This should be used for operations like module compilation and store creation
    /// to prevent race conditions when multiple threads access the same engine.
    ///
    /// # Returns
    /// A RAII guard that releases the lock when dropped.
    ///
    /// # Errors
    /// Returns an error if the engine has been closed.
    pub fn try_acquire_compile_lock(&self) -> WasmtimeResult<std::sync::RwLockWriteGuard<'_, ()>> {
        self.check_not_closed()?;
        Ok(self.concurrent_ops_lock.write().unwrap_or_else(|e| {
            log::warn!("concurrent_ops_lock was poisoned, recovering");
            e.into_inner()
        }))
    }

    /// Acquire a write lock for concurrent operations that need serialization.
    ///
    /// # Panics
    /// Panics if the engine has been closed. Use `try_acquire_compile_lock()` for fallible access.
    pub fn acquire_compile_lock(&self) -> std::sync::RwLockWriteGuard<'_, ()> {
        if self.is_closed.load(Ordering::SeqCst) {
            panic!("Engine has been closed and cannot be used");
        }
        self.concurrent_ops_lock.write().unwrap_or_else(|e| {
            log::warn!("concurrent_ops_lock was poisoned, recovering");
            e.into_inner()
        })
    }

    /// Acquire a read lock for concurrent operations that can run in parallel.
    ///
    /// This should be used for read-only operations that don't need exclusive access.
    ///
    /// # Returns
    /// A RAII guard that releases the lock when dropped.
    ///
    /// # Errors
    /// Returns an error if the engine has been closed.
    pub fn try_acquire_read_lock(&self) -> WasmtimeResult<std::sync::RwLockReadGuard<'_, ()>> {
        self.check_not_closed()?;
        Ok(self.concurrent_ops_lock.read().unwrap_or_else(|e| {
            log::warn!("concurrent_ops_lock was poisoned, recovering");
            e.into_inner()
        }))
    }

    /// Acquire a read lock for concurrent operations that can run in parallel.
    ///
    /// # Panics
    /// Panics if the engine has been closed. Use `try_acquire_read_lock()` for fallible access.
    pub fn acquire_read_lock(&self) -> std::sync::RwLockReadGuard<'_, ()> {
        if self.is_closed.load(Ordering::SeqCst) {
            panic!("Engine has been closed and cannot be used");
        }
        self.concurrent_ops_lock.read().unwrap_or_else(|e| {
            log::warn!("concurrent_ops_lock was poisoned, recovering");
            e.into_inner()
        })
    }

    /// Check if the engine has been closed
    #[inline]
    pub fn is_closed(&self) -> bool {
        self.is_closed.load(Ordering::SeqCst)
    }

    /// Mark this engine as closed.
    /// After calling this, all operations on the engine will fail with an error.
    pub fn mark_closed(&self) {
        self.is_closed.store(true, Ordering::SeqCst);
    }

    /// Check that the engine is not closed, returning an error if it is.
    #[inline]
    fn check_not_closed(&self) -> WasmtimeResult<()> {
        if self.is_closed.load(Ordering::SeqCst) {
            return Err(WasmtimeError::EngineConfig {
                message: "Engine has been closed and cannot be used".to_string(),
            });
        }
        Ok(())
    }

    /// Get reference to inner Wasmtime engine (internal use)
    ///
    /// This explicitly dereferences the Arc to get a reference to the WasmtimeEngine.
    ///
    /// # Panics
    /// Panics if the engine has been closed. Use `try_inner()` for fallible access.
    pub(crate) fn inner(&self) -> &WasmtimeEngine {
        if self.is_closed.load(Ordering::SeqCst) {
            panic!("Engine has been closed and cannot be used");
        }
        // Explicitly dereference Arc to get &WasmtimeEngine
        // This is equivalent to &*self.inner due to Deref coercion,
        // but being explicit prevents any potential optimization issues
        std::ops::Deref::deref(&self.inner)
    }

    /// Get reference to inner Wasmtime engine with error handling.
    ///
    /// Returns an error if the engine has been closed.
    pub(crate) fn try_inner(&self) -> WasmtimeResult<&WasmtimeEngine> {
        self.check_not_closed()?;
        Ok(std::ops::Deref::deref(&self.inner))
    }

    /// Get a clone of the inner Arc (for ownership transfer to component linkers)
    ///
    /// This clones the Arc, incrementing the reference count, which ensures
    /// the WasmtimeEngine stays alive even if the original Engine is dropped.
    pub(crate) fn inner_arc(&self) -> Arc<WasmtimeEngine> {
        Arc::clone(&self.inner)
    }

    /// Get configuration summary
    pub fn config_summary(&self) -> &EngineConfigSummary {
        &self.config_summary
    }

    /// Check if engine supports specific WebAssembly feature
    pub fn supports_feature(&self, feature: WasmFeature) -> bool {
        match feature {
            WasmFeature::Threads => self.config_summary.wasm_threads,
            WasmFeature::ReferenceTypes => self.config_summary.wasm_reference_types,
            WasmFeature::Simd => self.config_summary.wasm_simd,
            WasmFeature::BulkMemory => self.config_summary.wasm_bulk_memory,
            WasmFeature::MultiValue => self.config_summary.wasm_multi_value,
            WasmFeature::MultiMemory => self.config_summary.wasm_multi_memory,
            WasmFeature::TailCall => self.config_summary.wasm_tail_call,
            WasmFeature::RelaxedSimd => self.config_summary.wasm_relaxed_simd,
            WasmFeature::FunctionReferences => self.config_summary.wasm_function_references,
            WasmFeature::Gc => self.config_summary.wasm_gc,
            WasmFeature::Exceptions => self.config_summary.wasm_exceptions,
            WasmFeature::Memory64 => self.config_summary.wasm_memory64,
            WasmFeature::ExtendedConst => self.config_summary.wasm_extended_const,
            WasmFeature::ComponentModel => self.config_summary.wasm_component_model,
            WasmFeature::CustomPageSizes => self.config_summary.wasm_custom_page_sizes,
            WasmFeature::WideArithmetic => self.config_summary.wasm_wide_arithmetic,
            WasmFeature::StackSwitching => self.config_summary.wasm_stack_switching,
            WasmFeature::SharedEverythingThreads => {
                self.config_summary.wasm_shared_everything_threads
            }
            WasmFeature::ComponentModelAsync => self.config_summary.wasm_component_model_async,
            WasmFeature::ComponentModelAsyncBuiltins => {
                self.config_summary.wasm_component_model_async_builtins
            }
            WasmFeature::ComponentModelAsyncStackful => {
                self.config_summary.wasm_component_model_async_stackful
            }
            WasmFeature::ComponentModelErrorContext => {
                self.config_summary.wasm_component_model_error_context
            }
            WasmFeature::ComponentModelGc => self.config_summary.wasm_component_model_gc,
        }
    }

    /// Validate engine is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Check if engine has been closed
        self.check_not_closed()?;

        // Perform minimal validation to ensure engine is still usable
        // This is a defensive programming measure
        if Arc::strong_count(&self.inner) == 0 {
            return Err(WasmtimeError::Internal {
                message: "Engine reference count is invalid".to_string(),
            });
        }
        Ok(())
    }

    /// Get memory limit in pages (64KB per page)
    pub fn memory_limit_pages(&self) -> Option<u32> {
        self.config_summary.max_memory_pages
    }

    /// Get stack size limit in bytes
    pub fn stack_size_limit(&self) -> Option<usize> {
        self.config_summary.max_stack_size
    }

    /// Check if fuel consumption is enabled
    pub fn fuel_enabled(&self) -> bool {
        self.config_summary.fuel_enabled
    }

    /// Check if epoch-based interruption is enabled
    pub fn epoch_interruption_enabled(&self) -> bool {
        self.config_summary.epoch_interruption
    }

    /// Get maximum instances limit
    pub fn max_instances(&self) -> Option<u32> {
        self.config_summary.max_instances
    }

    /// Check if coredump generation on trap is enabled
    pub fn coredump_on_trap(&self) -> bool {
        self.config_summary.coredump_on_trap
    }

    /// Check if async execution support is enabled
    pub fn async_support_enabled(&self) -> bool {
        self.config_summary.async_support
    }

    /// Check engine reference count for debugging
    pub fn reference_count(&self) -> usize {
        Arc::strong_count(&self.inner)
    }

    /// Check if this engine is the same as another engine
    ///
    /// Two engines are considered the same if they share the same underlying
    /// Wasmtime engine (i.e., they were cloned from the same original engine).
    /// This uses Arc pointer equality to determine sameness.
    ///
    /// # Arguments
    /// * `other` - The other engine to compare against
    ///
    /// # Returns
    /// `true` if both engines share the same underlying Wasmtime engine
    pub fn same(&self, other: &Engine) -> bool {
        Arc::ptr_eq(&self.inner, &other.inner)
    }

    /// Detect if bytes are a precompiled WebAssembly module or component
    ///
    /// This inspects the header of the bytes to determine if they look like
    /// a precompiled core wasm module or a precompiled component.
    ///
    /// # Arguments
    /// * `bytes` - The bytes to check
    ///
    /// # Returns
    /// * `Some(0)` - The bytes look like a precompiled core wasm module
    /// * `Some(1)` - The bytes look like a precompiled wasm component
    /// * `None` - The bytes do not appear to be precompiled
    pub fn detect_precompiled(&self, bytes: &[u8]) -> Option<i32> {
        match WasmtimeEngine::detect_precompiled(bytes) {
            Some(wasmtime::Precompiled::Module) => Some(0),
            Some(wasmtime::Precompiled::Component) => Some(1),
            None => None,
        }
    }

    /// Increment the epoch counter
    ///
    /// This method is signal-safe and performs only an atomic increment operation.
    /// The epoch counter is used for epoch-based interruption of WebAssembly execution.
    ///
    /// Stores created from this engine with an epoch deadline will be interrupted
    /// when the epoch counter exceeds their deadline.
    ///
    /// This is typically called from a separate thread or signal handler to
    /// periodically increment the epoch, enabling cooperative timeslicing of
    /// long-running WebAssembly code.
    pub fn increment_epoch(&self) {
        self.inner.increment_epoch();
    }
}

/// WebAssembly features that can be queried
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum WasmFeature {
    /// WebAssembly threads proposal support
    Threads,
    /// WebAssembly reference types proposal support
    ReferenceTypes,
    /// WebAssembly SIMD (Single Instruction, Multiple Data) support
    Simd,
    /// WebAssembly bulk memory operations proposal support
    BulkMemory,
    /// WebAssembly multi-value proposal support (multiple return values)
    MultiValue,
    /// WebAssembly multi-memory proposal support (multiple memory instances)
    MultiMemory,
    /// WebAssembly tail call proposal support (tail call optimization)
    TailCall,
    /// WebAssembly relaxed SIMD proposal support (performance optimization)
    RelaxedSimd,
    /// WebAssembly function references proposal support
    FunctionReferences,
    /// WebAssembly garbage collection proposal support
    Gc,
    /// WebAssembly exceptions proposal support
    Exceptions,
    /// WebAssembly 64-bit memory support
    Memory64,
    /// WebAssembly extended constant expressions support
    ExtendedConst,
    /// WebAssembly component model support
    ComponentModel,
    /// WebAssembly custom page sizes support
    CustomPageSizes,
    /// WebAssembly wide arithmetic support
    WideArithmetic,
    /// WebAssembly stack switching support
    StackSwitching,
    /// WebAssembly shared-everything-threads support
    SharedEverythingThreads,
    /// WebAssembly component model async support
    ComponentModelAsync,
    /// WebAssembly component model async builtins support
    ComponentModelAsyncBuiltins,
    /// WebAssembly component model async stackful support
    ComponentModelAsyncStackful,
    /// WebAssembly component model error context support
    ComponentModelErrorContext,
    /// WebAssembly component model GC support
    ComponentModelGc,
}

impl Default for Engine {
    fn default() -> Self {
        // Create a minimal, guaranteed-to-succeed engine configuration
        match Self::new() {
            Ok(engine) => engine,
            Err(_) => {
                // Fallback to absolute minimal configuration that should always work
                let mut config = safe_wasmtime_config();
                config.strategy(Strategy::Cranelift);

                match WasmtimeEngine::new(&config) {
                    Ok(wasmtime_engine) => Engine {
                        inner: Arc::new(wasmtime_engine),
                        config_summary: EngineConfigSummary {
                            strategy: "Cranelift".to_string(),
                            opt_level: "None".to_string(),
                            debug_info: false,
                            wasm_threads: false,
                            wasm_reference_types: false,
                            wasm_simd: false,
                            wasm_bulk_memory: false,
                            wasm_multi_value: false,
                            wasm_multi_memory: false,
                            wasm_tail_call: false,
                            wasm_relaxed_simd: false,
                            wasm_function_references: false,
                            wasm_gc: false,
                            wasm_exceptions: false,
                            wasm_memory64: false,
                            wasm_extended_const: false,
                            wasm_component_model: false,
                            wasm_custom_page_sizes: false,
                            wasm_wide_arithmetic: false,
                            wasm_stack_switching: false,
                            wasm_shared_everything_threads: false,
                            wasm_component_model_async: false,
                            wasm_component_model_async_builtins: false,
                            wasm_component_model_async_stackful: false,
                            wasm_component_model_error_context: false,
                            wasm_component_model_gc: false,
                            fuel_enabled: false,
                            max_memory_pages: None,
                            max_stack_size: None,
                            epoch_interruption: false,
                            max_instances: None,
                            async_support: false,
                            coredump_on_trap: false,
                            memory_reservation: None,
                            memory_guard_size: None,
                            memory_reservation_for_growth: None,
                            max_memory_size: None,
                            cranelift_debug_verifier: false,
                            cranelift_nan_canonicalization: false,
                            cranelift_pcc: false,
                            cranelift_regalloc_algorithm: "Backtracking".to_string(),
                            wmemcheck_enabled: false,
                            table_lazy_init: true,
                            // New config options with minimal defaults
                            gc_support: false,
                            collector: "Auto".to_string(),
                            memory_may_move: true,
                            guard_before_linear_memory: true,
                            memory_init_cow: true,
                            wasm_component_model_threading: false,
                            relaxed_simd_deterministic: false,
                            async_stack_zeroing: false,
                            async_stack_size: None,
                            parallel_compilation: true,
                            macos_use_mach_ports: true,
                            module_version_strategy: "WasmtimeVersion".to_string(),
                            allocation_strategy: "OnDemand".to_string(),
                        },
                        concurrent_ops_lock: Arc::new(RwLock::new(())),
                        is_closed: Arc::new(AtomicBool::new(false)),
                    },
                    Err(_) => {
                        // Last resort: create engine with safe wasmtime config
                        // This should virtually never fail unless the system is severely broken
                        let default_config = safe_wasmtime_config();
                        Engine {
                            inner: Arc::new(WasmtimeEngine::new(&default_config).unwrap_or_else(
                                |_| {
                                    panic!(
                                        "Critical: Cannot create fallback engine - system unusable"
                                    )
                                },
                            )),
                            config_summary: EngineConfigSummary {
                                strategy: "Default".to_string(),
                                opt_level: "None".to_string(),
                                debug_info: false,
                                wasm_threads: false,
                                wasm_reference_types: false,
                                wasm_simd: false,
                                wasm_bulk_memory: false,
                                wasm_multi_value: false,
                                wasm_multi_memory: false,
                                wasm_tail_call: false,
                                wasm_relaxed_simd: false,
                                wasm_function_references: false,
                                wasm_gc: false,
                                wasm_exceptions: false,
                                wasm_memory64: false,
                                wasm_extended_const: false,
                                wasm_component_model: false,
                                wasm_custom_page_sizes: false,
                                wasm_wide_arithmetic: false,
                                wasm_stack_switching: false,
                                wasm_shared_everything_threads: false,
                                wasm_component_model_async: false,
                                wasm_component_model_async_builtins: false,
                                wasm_component_model_async_stackful: false,
                                wasm_component_model_error_context: false,
                                wasm_component_model_gc: false,
                                fuel_enabled: false,
                                max_memory_pages: None,
                                max_stack_size: None,
                                epoch_interruption: false,
                                max_instances: None,
                                async_support: false,
                                coredump_on_trap: false,
                                memory_reservation: None,
                                memory_guard_size: None,
                                memory_reservation_for_growth: None,
                                max_memory_size: None,
                                cranelift_debug_verifier: false,
                                cranelift_nan_canonicalization: false,
                                cranelift_pcc: false,
                                cranelift_regalloc_algorithm: "Backtracking".to_string(),
                                wmemcheck_enabled: false,
                                table_lazy_init: true,
                                // New config options with minimal defaults
                                gc_support: false,
                                collector: "Auto".to_string(),
                                memory_may_move: true,
                                guard_before_linear_memory: true,
                                memory_init_cow: true,
                                wasm_component_model_threading: false,
                                relaxed_simd_deterministic: false,
                                async_stack_zeroing: false,
                                async_stack_size: None,
                                parallel_compilation: true,
                                macos_use_mach_ports: true,
                                module_version_strategy: "WasmtimeVersion".to_string(),
                                allocation_strategy: "OnDemand".to_string(),
                            },
                            concurrent_ops_lock: Arc::new(RwLock::new(())),
                            is_closed: Arc::new(AtomicBool::new(false)),
                        }
                    }
                }
            }
        }
    }
}

// Thread safety: Engine wraps Arc<WasmtimeEngine> which is thread-safe
unsafe impl Send for Engine {}
unsafe impl Sync for Engine {}
