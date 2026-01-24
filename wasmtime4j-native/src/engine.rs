//! Wasmtime engine management with comprehensive configuration and lifecycle support
//!
//! This module provides defensive, thread-safe wrapper around Wasmtime engines
//! with proper resource management and JVM crash prevention.

use std::sync::Arc;
use wasmtime::{Config, Engine as WasmtimeEngine, OptLevel, RegallocAlgorithm, Strategy};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime engine with defensive programming
#[derive(Debug, Clone)]
pub struct Engine {
    inner: Arc<WasmtimeEngine>,
    config_summary: EngineConfigSummary,
}

/// Summary of engine configuration for debugging and introspection
#[derive(Debug, Clone)]
pub struct EngineConfigSummary {
    /// Compilation strategy used (e.g., "Cranelift")
    pub strategy: String,
    /// Optimization level (e.g., "Speed", "SpeedAndSize", "None")
    pub opt_level: String,
    /// Whether debug information is enabled
    pub debug_info: bool,
    /// Whether WebAssembly threads are supported
    pub wasm_threads: bool,
    /// Whether WebAssembly reference types are enabled
    pub wasm_reference_types: bool,
    /// Whether WebAssembly SIMD instructions are supported
    pub wasm_simd: bool,
    /// Whether WebAssembly bulk memory operations are enabled
    pub wasm_bulk_memory: bool,
    /// Whether WebAssembly multi-value returns are supported
    pub wasm_multi_value: bool,
    /// Whether WebAssembly multi-memory is supported
    pub wasm_multi_memory: bool,
    /// Whether WebAssembly tail call is supported
    pub wasm_tail_call: bool,
    /// Whether WebAssembly relaxed SIMD is supported
    pub wasm_relaxed_simd: bool,
    /// Whether WebAssembly function references are supported
    pub wasm_function_references: bool,
    /// Whether WebAssembly garbage collection is supported
    pub wasm_gc: bool,
    /// Whether WebAssembly exceptions are supported
    pub wasm_exceptions: bool,
    /// Whether WebAssembly 64-bit memory is supported
    pub wasm_memory64: bool,
    /// Whether WebAssembly extended constant expressions are supported
    pub wasm_extended_const: bool,
    /// Whether WebAssembly component model is supported
    pub wasm_component_model: bool,
    /// Whether WebAssembly custom page sizes are supported
    pub wasm_custom_page_sizes: bool,
    /// Whether WebAssembly wide arithmetic is supported
    pub wasm_wide_arithmetic: bool,
    /// Whether WebAssembly stack switching is supported
    pub wasm_stack_switching: bool,
    /// Whether WebAssembly shared-everything-threads is supported
    pub wasm_shared_everything_threads: bool,
    /// Whether WebAssembly component model async is supported
    pub wasm_component_model_async: bool,
    /// Whether WebAssembly component model async builtins is supported
    pub wasm_component_model_async_builtins: bool,
    /// Whether WebAssembly component model async stackful is supported
    pub wasm_component_model_async_stackful: bool,
    /// Whether WebAssembly component model error context is supported
    pub wasm_component_model_error_context: bool,
    /// Whether WebAssembly component model GC is supported
    pub wasm_component_model_gc: bool,
    /// Whether fuel consumption is enabled
    pub fuel_enabled: bool,
    /// Maximum memory pages allowed (64KB per page)
    pub max_memory_pages: Option<u32>,
    /// Maximum stack size in bytes
    pub max_stack_size: Option<usize>,
    /// Whether epoch-based interruption is enabled
    pub epoch_interruption: bool,
    /// Maximum number of WebAssembly instances per engine
    pub max_instances: Option<u32>,
    /// Whether async execution support is enabled
    pub async_support: bool,
    /// Whether coredump generation on trap is enabled
    pub coredump_on_trap: bool,
    /// Memory reservation in bytes (virtual memory pre-allocation)
    pub memory_reservation: Option<u64>,
    /// Memory guard size in bytes (protection region after memory)
    pub memory_guard_size: Option<u64>,
    /// Memory reservation for growth in bytes (extra reservation for growing)
    pub memory_reservation_for_growth: Option<u64>,
    /// Maximum memory size in bytes (hard limit on memory usage)
    pub max_memory_size: Option<usize>,
    /// Whether Cranelift debug verifier is enabled
    pub cranelift_debug_verifier: bool,
    /// Whether Cranelift NaN canonicalization is enabled for determinism
    pub cranelift_nan_canonicalization: bool,
    /// Whether Cranelift proof-carrying code validation is enabled
    pub cranelift_pcc: bool,
    /// Cranelift register allocation algorithm (e.g., "Backtracking", "SinglePass")
    pub cranelift_regalloc_algorithm: String,
    /// Whether wmemcheck (WebAssembly memory checker) is enabled
    /// Only available when compiled with the `wmemcheck` feature
    pub wmemcheck_enabled: bool,
    /// Whether table lazy initialization is enabled
    /// When enabled, tables are initialized lazily for faster instantiation
    /// but slightly slower indirect calls. Defaults to true.
    pub table_lazy_init: bool,
    /// Whether GC support infrastructure is enabled
    /// Required for GC, function references, and exception proposals
    pub gc_support: bool,
    /// The GC collector implementation in use (e.g., "Auto", "DeferredReferenceCounting", "Null")
    pub collector: String,
    /// Whether linear memories may relocate their base pointer at runtime
    pub memory_may_move: bool,
    /// Whether guard regions exist before linear memory allocations
    pub guard_before_linear_memory: bool,
    /// Whether copy-on-write memory-mapped data initialization is enabled
    pub memory_init_cow: bool,
    /// Whether component model threading support is enabled (experimental)
    pub wasm_component_model_threading: bool,
    /// Whether deterministic relaxed SIMD behavior is forced across platforms
    pub relaxed_simd_deterministic: bool,
    /// Whether async stacks are zeroed before (re)use for defense-in-depth
    pub async_stack_zeroing: bool,
    /// Size of async stacks in bytes
    pub async_stack_size: Option<usize>,
    /// Whether multi-threaded module compilation is enabled
    pub parallel_compilation: bool,
    /// Whether Mach ports are used instead of Unix signals on macOS
    pub macos_use_mach_ports: bool,
    /// Module version strategy for serialization compatibility (e.g., "WasmtimeVersion", "None", "Custom")
    pub module_version_strategy: String,
    /// Instance allocation strategy (e.g., "OnDemand", "Pooling")
    pub allocation_strategy: String,
}

/// Builder for creating configured engines
pub struct EngineBuilder {
    config: Config,
    strategy: Option<Strategy>,
    opt_level: Option<OptLevel>,
    debug_info: bool,
    fuel_enabled: bool,
    max_memory_pages: Option<u32>,
    max_stack_size: Option<usize>,
    epoch_interruption: bool,
    max_instances: Option<u32>,
    // Track wasm features separately for proper config summary
    wasm_threads: bool,
    wasm_reference_types: bool,
    wasm_simd: bool,
    wasm_bulk_memory: bool,
    wasm_multi_value: bool,
    wasm_multi_memory: bool,
    wasm_tail_call: bool,
    wasm_relaxed_simd: bool,
    wasm_function_references: bool,
    wasm_gc: bool,
    wasm_exceptions: bool,
    wasm_memory64: bool,
    wasm_extended_const: bool,
    wasm_component_model: bool,
    wasm_custom_page_sizes: bool,
    wasm_wide_arithmetic: bool,
    wasm_stack_switching: bool,
    wasm_shared_everything_threads: bool,
    wasm_component_model_async: bool,
    wasm_component_model_async_builtins: bool,
    wasm_component_model_async_stackful: bool,
    wasm_component_model_error_context: bool,
    wasm_component_model_gc: bool,
    async_support: bool,
    coredump_on_trap: bool,
    // Memory configuration options
    memory_reservation: Option<u64>,
    memory_guard_size: Option<u64>,
    memory_reservation_for_growth: Option<u64>,
    max_memory_size: Option<usize>,
    // Cranelift configuration options
    cranelift_debug_verifier: bool,
    cranelift_nan_canonicalization: bool,
    cranelift_pcc: bool,
    cranelift_regalloc_algorithm: Option<RegallocAlgorithm>,
    // wmemcheck - only available when compiled with wmemcheck feature
    #[cfg(feature = "wmemcheck")]
    wmemcheck_enabled: bool,
    // Table lazy initialization - enabled by default for faster instantiation
    table_lazy_init: bool,
    // GC support infrastructure - required for GC, function references, exceptions
    gc_support: bool,
    // GC collector implementation choice
    collector: Option<wasmtime::Collector>,
    // Memory may relocate at runtime
    memory_may_move: bool,
    // Guard regions before linear memory
    guard_before_linear_memory: bool,
    // Copy-on-write memory initialization
    memory_init_cow: bool,
    // Component model threading support (experimental)
    wasm_component_model_threading: bool,
    // Deterministic relaxed SIMD behavior
    relaxed_simd_deterministic: bool,
    // Zero async stacks before reuse
    async_stack_zeroing: bool,
    // Async stack size
    async_stack_size: Option<usize>,
    // Multi-threaded compilation
    parallel_compilation: bool,
    // Use Mach ports on macOS
    macos_use_mach_ports: bool,
    // Module version strategy
    module_version_strategy: Option<wasmtime::ModuleVersionStrategy>,
    // Instance allocation strategy
    allocation_strategy: Option<wasmtime::InstanceAllocationStrategy>,
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
    pub fn with_config(config: Config) -> WasmtimeResult<Self> {
        let summary = EngineConfigSummary::from_config(&config);
        
        let engine = WasmtimeEngine::new(&config)
            .map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to create Wasmtime engine: {}", e),
            })?;

        Ok(Engine {
            inner: Arc::new(engine),
            config_summary: summary,
        })
    }

    /// Get reference to inner Wasmtime engine (internal use)
    ///
    /// This explicitly dereferences the Arc to get a reference to the WasmtimeEngine.
    pub(crate) fn inner(&self) -> &WasmtimeEngine {
        // Explicitly dereference Arc to get &WasmtimeEngine
        // This is equivalent to &*self.inner due to Deref coercion,
        // but being explicit prevents any potential optimization issues
        std::ops::Deref::deref(&self.inner)
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
            WasmFeature::SharedEverythingThreads => self.config_summary.wasm_shared_everything_threads,
            WasmFeature::ComponentModelAsync => self.config_summary.wasm_component_model_async,
            WasmFeature::ComponentModelAsyncBuiltins => self.config_summary.wasm_component_model_async_builtins,
            WasmFeature::ComponentModelAsyncStackful => self.config_summary.wasm_component_model_async_stackful,
            WasmFeature::ComponentModelErrorContext => self.config_summary.wasm_component_model_error_context,
            WasmFeature::ComponentModelGc => self.config_summary.wasm_component_model_gc,
        }
    }

    /// Validate engine is still functional (defensive check)
    pub fn validate(&self) -> WasmtimeResult<()> {
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

/// Shared core functions for engine operations used by both JNI and Panama interfaces
/// 
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;
    
    /// Core function to create a new engine with default configuration
    /// Uses EngineBuilder to ensure proper defaults including component model support
    pub fn create_engine() -> WasmtimeResult<Box<Engine>> {
        EngineBuilder::new().build().map(Box::new)
    }
    
    /// Core function to create an engine with custom configuration
    pub fn create_engine_with_config(
        strategy: Option<Strategy>,
        opt_level: Option<OptLevel>,
        debug_info: bool,
        wasm_threads: bool,
        wasm_simd: bool,
        wasm_reference_types: bool,
        wasm_bulk_memory: bool,
        wasm_multi_value: bool,
        fuel_enabled: bool,
        max_memory_pages: Option<u32>,
        max_stack_size: Option<usize>,
        epoch_interruption: bool,
        max_instances: Option<u32>,
        async_support: bool,
    ) -> WasmtimeResult<Box<Engine>> {
        let mut builder = Engine::builder();

        if let Some(strategy) = strategy {
            builder = builder.strategy(strategy);
        }

        if let Some(opt_level) = opt_level {
            builder = builder.opt_level(opt_level);
        }

        builder = builder
            .debug_info(debug_info)
            .wasm_threads(wasm_threads)
            .wasm_simd(wasm_simd)
            .wasm_reference_types(wasm_reference_types)
            .wasm_bulk_memory(wasm_bulk_memory)
            .wasm_multi_value(wasm_multi_value)
            .fuel_enabled(fuel_enabled)
            .epoch_interruption(epoch_interruption)
            .async_support(async_support);

        if let Some(pages) = max_memory_pages {
            builder = builder.max_memory_pages(pages);
        }

        if let Some(stack_size) = max_stack_size {
            builder = builder.max_stack_size(stack_size);
        }

        if let Some(instances) = max_instances {
            builder = builder.max_instances(instances);
        }

        builder.build().map(Box::new)
    }

    /// Core function to create an engine with extended configuration including GC and memory options
    pub fn create_engine_with_extended_config(
        strategy: Option<Strategy>,
        opt_level: Option<OptLevel>,
        debug_info: bool,
        wasm_threads: bool,
        wasm_simd: bool,
        wasm_reference_types: bool,
        wasm_bulk_memory: bool,
        wasm_multi_value: bool,
        fuel_enabled: bool,
        max_memory_pages: Option<u32>,
        max_stack_size: Option<usize>,
        epoch_interruption: bool,
        max_instances: Option<u32>,
        async_support: bool,
        // GC configuration
        wasm_gc: bool,
        wasm_function_references: bool,
        wasm_exceptions: bool,
        // Memory configuration
        memory_reservation: Option<u64>,
        memory_guard_size: Option<u64>,
        memory_reservation_for_growth: Option<u64>,
        // Additional features
        wasm_tail_call: bool,
        wasm_relaxed_simd: bool,
        wasm_multi_memory: bool,
        wasm_memory64: bool,
        wasm_extended_const: bool,
        wasm_component_model: bool,
        coredump_on_trap: bool,
        cranelift_nan_canonicalization: bool,
        // Experimental features
        wasm_custom_page_sizes: bool,
        wasm_wide_arithmetic: bool,
    ) -> WasmtimeResult<Box<Engine>> {
        let mut builder = Engine::builder();

        if let Some(strategy) = strategy {
            builder = builder.strategy(strategy);
        }

        if let Some(opt_level) = opt_level {
            builder = builder.opt_level(opt_level);
        }

        builder = builder
            .debug_info(debug_info)
            .wasm_threads(wasm_threads)
            .wasm_simd(wasm_simd)
            .wasm_reference_types(wasm_reference_types)
            .wasm_bulk_memory(wasm_bulk_memory)
            .wasm_multi_value(wasm_multi_value)
            .fuel_enabled(fuel_enabled)
            .epoch_interruption(epoch_interruption)
            .async_support(async_support)
            // GC configuration
            .wasm_gc(wasm_gc)
            .wasm_function_references(wasm_function_references)
            .wasm_exceptions(wasm_exceptions)
            // Additional features
            .wasm_tail_call(wasm_tail_call)
            .wasm_relaxed_simd(wasm_relaxed_simd)
            .wasm_multi_memory(wasm_multi_memory)
            .wasm_memory64(wasm_memory64)
            .wasm_extended_const(wasm_extended_const)
            .wasm_component_model(wasm_component_model)
            .coredump_on_trap(coredump_on_trap)
            .cranelift_nan_canonicalization(cranelift_nan_canonicalization)
            // Experimental features
            .wasm_custom_page_sizes(wasm_custom_page_sizes)
            .wasm_wide_arithmetic(wasm_wide_arithmetic);

        if let Some(pages) = max_memory_pages {
            builder = builder.max_memory_pages(pages);
        }

        if let Some(stack_size) = max_stack_size {
            builder = builder.max_stack_size(stack_size);
        }

        if let Some(instances) = max_instances {
            builder = builder.max_instances(instances);
        }

        // Memory configuration
        if let Some(reservation) = memory_reservation {
            builder = builder.memory_reservation(reservation);
        }

        if let Some(guard_size) = memory_guard_size {
            builder = builder.memory_guard_size(guard_size);
        }

        if let Some(growth_reservation) = memory_reservation_for_growth {
            builder = builder.memory_reservation_for_growth(growth_reservation);
        }

        builder.build().map(Box::new)
    }

    /// Core function to detect if a host CPU feature is available
    ///
    /// Note: In wasmtime 39, the Config::detect_host_feature method is used
    /// to override feature detection (for testing), not to query features.
    /// The actual feature detection happens automatically during compilation.
    /// This function uses Rust's std_detect crate features where available,
    /// or returns false for unknown features.
    pub fn detect_host_feature(feature_name: &str) -> bool {
        // Wasmtime 39's detect_host_feature is for overriding detection, not querying.
        // We use target_feature detection at compile time and cfg checks for runtime.
        // For common CPU features, we check using Rust's built-in capabilities.
        match feature_name {
            // x86_64 features
            "sse" => cfg!(target_feature = "sse"),
            "sse2" => cfg!(target_feature = "sse2"),
            "sse3" => cfg!(target_feature = "sse3"),
            "ssse3" => cfg!(target_feature = "ssse3"),
            "sse4.1" | "sse41" => cfg!(target_feature = "sse4.1"),
            "sse4.2" | "sse42" => cfg!(target_feature = "sse4.2"),
            "avx" => cfg!(target_feature = "avx"),
            "avx2" => cfg!(target_feature = "avx2"),
            "avx512f" => cfg!(target_feature = "avx512f"),
            "bmi1" => cfg!(target_feature = "bmi1"),
            "bmi2" => cfg!(target_feature = "bmi2"),
            "lzcnt" => cfg!(target_feature = "lzcnt"),
            "popcnt" => cfg!(target_feature = "popcnt"),
            "fma" => cfg!(target_feature = "fma"),
            // ARM features
            "neon" => cfg!(target_feature = "neon"),
            // Unknown features
            _ => false,
        }
    }
    
    /// Core function to validate engine pointer and get reference
    pub unsafe fn get_engine_ref(engine_ptr: *const c_void) -> WasmtimeResult<&'static Engine> {
        validate_ptr_not_null!(engine_ptr, "engine");
        Ok(&*(engine_ptr as *const Engine))
    }
    
    /// Core function to validate engine pointer and get mutable reference
    pub unsafe fn get_engine_mut(engine_ptr: *mut c_void) -> WasmtimeResult<&'static mut Engine> {
        validate_ptr_not_null!(engine_ptr, "engine");
        Ok(&mut *(engine_ptr as *mut Engine))
    }
    
    /// Core function to check if engine supports a specific feature
    pub fn check_feature_support(engine: &Engine, feature: WasmFeature) -> bool {
        engine.supports_feature(feature)
    }
    
    /// Core function to get engine configuration summary
    pub fn get_config_summary(engine: &Engine) -> &EngineConfigSummary {
        engine.config_summary()
    }
    
    /// Core function to destroy an engine (safe cleanup)
    pub unsafe fn destroy_engine(engine_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Engine>(engine_ptr, "Engine");
    }
    
    /// Core function to validate engine functionality
    pub fn validate_engine(engine: &Engine) -> WasmtimeResult<()> {
        engine.validate()
    }

    /// Core function to check memory limits
    pub fn get_memory_limit(engine: &Engine) -> Option<u32> {
        engine.memory_limit_pages()
    }

    /// Core function to check stack size limits
    pub fn get_stack_limit(engine: &Engine) -> Option<usize> {
        engine.stack_size_limit()
    }

    /// Core function to check if fuel is enabled
    pub fn is_fuel_enabled(engine: &Engine) -> bool {
        engine.fuel_enabled()
    }

    /// Core function to check if epoch interruption is enabled
    pub fn is_epoch_interruption_enabled(engine: &Engine) -> bool {
        engine.epoch_interruption_enabled()
    }

    /// Core function to check if coredump on trap is enabled
    pub fn is_coredump_on_trap_enabled(engine: &Engine) -> bool {
        engine.coredump_on_trap()
    }

    /// Core function to get maximum instances limit
    pub fn get_max_instances(engine: &Engine) -> Option<u32> {
        engine.max_instances()
    }

    /// Core function to get engine reference count
    pub fn get_reference_count(engine: &Engine) -> usize {
        engine.reference_count()
    }

    /// Core function to precompile a WebAssembly module for AOT (ahead-of-time) usage
    ///
    /// This compiles the WebAssembly binary into a serialized form that can be
    /// later loaded via `Module::deserialize` without needing to recompile.
    /// This is useful for caching compiled modules to disk.
    pub fn precompile_module(engine: &Engine, wasm_bytes: &[u8]) -> WasmtimeResult<Vec<u8>> {
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::ValidationError {
                message: "WASM bytes cannot be empty".to_string(),
            });
        }
        engine.inner.precompile_module(wasm_bytes).map_err(|e| {
            WasmtimeError::CompilationError {
                message: format!("Failed to precompile module: {}", e),
            }
        })
    }

    /// Core function to precompile a WebAssembly component for AOT usage
    #[cfg(feature = "component-model")]
    pub fn precompile_component(engine: &Engine, wasm_bytes: &[u8]) -> WasmtimeResult<Vec<u8>> {
        if wasm_bytes.is_empty() {
            return Err(WasmtimeError::ValidationError {
                message: "WASM bytes cannot be empty".to_string(),
            });
        }
        engine.inner.precompile_component(wasm_bytes).map_err(|e| {
            WasmtimeError::CompilationError {
                message: format!("Failed to precompile component: {}", e),
            }
        })
    }
}

impl EngineBuilder {
    /// Create new engine builder with safe defaults
    fn new() -> Self {
        let mut config = Config::new();

        // Set production-optimized defaults
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);

        // Enable commonly used WebAssembly features
        config.wasm_reference_types(true);
        config.wasm_bulk_memory(true);
        config.wasm_multi_value(true);
        config.wasm_simd(true);

        // Enable Component Model support (Tier 1 feature)
        config.wasm_component_model(true);

        // Configure stack size - increase from default 512 KiB to 2 MiB for JNI safety
        config.max_wasm_stack(2 * 1024 * 1024);

        // Enable debug info for better backtraces during development
        config.debug_info(true);

        // Enable WASM backtraces for better error diagnostics
        config.wasm_backtrace(true);
        config.wasm_backtrace_details(wasmtime::WasmBacktraceDetails::Enable);

        // CRITICAL: Disable signal-based traps to avoid conflict with JVM signal handlers
        // JVM and Wasmtime both install SIGSEGV handlers which conflict
        // This forces explicit bounds checks instead of using signals
        config.signals_based_traps(false);

        // Note: Fuel consumption is opt-in via StoreBuilder.fuel_limit()
        // config.consume_fuel(true);

        EngineBuilder {
            config,
            strategy: Some(Strategy::Cranelift),
            opt_level: Some(OptLevel::Speed),
            debug_info: true,
            fuel_enabled: false,
            max_memory_pages: None,
            max_stack_size: None,
            epoch_interruption: false,
            max_instances: None,
            wasm_threads: true,
            wasm_reference_types: true,
            wasm_simd: true,
            wasm_bulk_memory: true,
            wasm_multi_value: true,
            wasm_multi_memory: false,
            wasm_tail_call: false,
            wasm_relaxed_simd: false,
            wasm_function_references: false,
            wasm_gc: false,
            wasm_exceptions: false,
            wasm_memory64: true,              // Tier 1 - on by default
            wasm_extended_const: true,        // Tier 1 - on by default
            wasm_component_model: true,       // Tier 1 - on by default
            wasm_custom_page_sizes: false,    // Tier 3 - off by default
            wasm_wide_arithmetic: false,      // Tier 3 - off by default
            wasm_stack_switching: false,      // Tier 3 - off by default
            wasm_shared_everything_threads: false,  // Tier 3 - off by default
            wasm_component_model_async: false,  // Component model extension - off by default
            wasm_component_model_async_builtins: false,  // Component model extension - off by default
            wasm_component_model_async_stackful: false,  // Component model extension - off by default
            wasm_component_model_error_context: false,  // Component model extension - off by default
            wasm_component_model_gc: false,  // Component model extension - off by default
            async_support: false,  // Async execution support - off by default
            coredump_on_trap: false,  // Coredump on trap - off by default
            memory_reservation: None,  // Memory reservation - use Wasmtime default
            memory_guard_size: None,  // Memory guard size - use Wasmtime default
            memory_reservation_for_growth: None,  // Memory reservation for growth - use Wasmtime default
            max_memory_size: None,  // Max memory size - use Wasmtime default
            cranelift_debug_verifier: false,  // Cranelift debug verifier - off by default
            cranelift_nan_canonicalization: false,  // Cranelift NaN canonicalization - off by default
            cranelift_pcc: false,  // Cranelift proof-carrying code - off by default
            cranelift_regalloc_algorithm: None,  // Cranelift regalloc algorithm - use default
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: false,  // wmemcheck - off by default
            table_lazy_init: true,  // Table lazy init - on by default (wasmtime default)
            // New config options with wasmtime defaults
            gc_support: true,  // GC support - on by default (enables GC infrastructure)
            collector: None,  // Collector - use wasmtime default (Auto)
            memory_may_move: true,  // Memory may move - on by default
            guard_before_linear_memory: true,  // Guard before memory - on by default
            memory_init_cow: true,  // CoW memory init - on by default
            wasm_component_model_threading: false,  // Component threading - off (experimental)
            relaxed_simd_deterministic: false,  // Relaxed SIMD deterministic - off by default
            async_stack_zeroing: false,  // Async stack zeroing - off by default
            async_stack_size: None,  // Async stack size - use wasmtime default
            parallel_compilation: true,  // Parallel compilation - on by default
            macos_use_mach_ports: true,  // Mach ports on macOS - on by default
            module_version_strategy: None,  // Module version - use wasmtime default
            allocation_strategy: None,  // Allocation strategy - use wasmtime default (OnDemand)
        }
    }

    /// Set compilation strategy
    pub fn strategy(mut self, strategy: Strategy) -> Self {
        self.config.strategy(strategy.clone());
        self.strategy = Some(strategy);
        self
    }

    /// Set optimization level
    pub fn opt_level(mut self, level: OptLevel) -> Self {
        self.config.cranelift_opt_level(level.clone());
        self.opt_level = Some(level);
        self
    }

    /// Enable or disable debug information
    pub fn debug_info(mut self, enable: bool) -> Self {
        self.config.debug_info(enable);
        self.debug_info = enable;
        self
    }

    /// Configure WebAssembly threads support
    /// Also enables shared memory support when threads are enabled
    pub fn wasm_threads(mut self, enable: bool) -> Self {
        self.config.wasm_threads(enable);
        // Shared memory is required for the threads proposal to work with shared memories
        self.config.shared_memory(enable);
        self.wasm_threads = enable;
        self
    }

    /// Configure WebAssembly reference types support
    pub fn wasm_reference_types(mut self, enable: bool) -> Self {
        self.config.wasm_reference_types(enable);
        self.wasm_reference_types = enable;
        self
    }

    /// Configure WebAssembly SIMD support
    pub fn wasm_simd(mut self, enable: bool) -> Self {
        self.config.wasm_simd(enable);
        self.wasm_simd = enable;
        self
    }

    /// Configure WebAssembly bulk memory support
    pub fn wasm_bulk_memory(mut self, enable: bool) -> Self {
        self.config.wasm_bulk_memory(enable);
        self.wasm_bulk_memory = enable;
        self
    }

    /// Configure WebAssembly multi-value support
    pub fn wasm_multi_value(mut self, enable: bool) -> Self {
        self.config.wasm_multi_value(enable);
        self.wasm_multi_value = enable;
        self
    }

    /// Configure WebAssembly multi-memory support
    pub fn wasm_multi_memory(mut self, enable: bool) -> Self {
        self.config.wasm_multi_memory(enable);
        self.wasm_multi_memory = enable;
        self
    }

    /// Configure WebAssembly tail call support
    pub fn wasm_tail_call(mut self, enable: bool) -> Self {
        self.config.wasm_tail_call(enable);
        self.wasm_tail_call = enable;
        self
    }

    /// Configure WebAssembly relaxed SIMD support
    pub fn wasm_relaxed_simd(mut self, enable: bool) -> Self {
        self.config.wasm_relaxed_simd(enable);
        self.wasm_relaxed_simd = enable;
        self
    }

    /// Configure WebAssembly function references support
    pub fn wasm_function_references(mut self, enable: bool) -> Self {
        self.config.wasm_function_references(enable);
        self.wasm_function_references = enable;
        self
    }

    /// Configure WebAssembly garbage collection support
    pub fn wasm_gc(mut self, enable: bool) -> Self {
        self.config.wasm_gc(enable);
        self.wasm_gc = enable;
        self
    }

    /// Configure WebAssembly exceptions support
    pub fn wasm_exceptions(mut self, enable: bool) -> Self {
        self.config.wasm_exceptions(enable);
        self.wasm_exceptions = enable;
        self
    }

    /// Configure WebAssembly 64-bit memory support
    pub fn wasm_memory64(mut self, enable: bool) -> Self {
        self.config.wasm_memory64(enable);
        self.wasm_memory64 = enable;
        self
    }

    /// Configure WebAssembly extended constant expressions support
    pub fn wasm_extended_const(mut self, enable: bool) -> Self {
        self.config.wasm_extended_const(enable);
        self.wasm_extended_const = enable;
        self
    }

    /// Configure WebAssembly component model support
    pub fn wasm_component_model(mut self, enable: bool) -> Self {
        self.config.wasm_component_model(enable);
        self.wasm_component_model = enable;
        self
    }

    /// Configure WebAssembly custom page sizes support
    pub fn wasm_custom_page_sizes(mut self, enable: bool) -> Self {
        self.config.wasm_custom_page_sizes(enable);
        self.wasm_custom_page_sizes = enable;
        self
    }

    /// Configure WebAssembly wide arithmetic support
    pub fn wasm_wide_arithmetic(mut self, enable: bool) -> Self {
        self.config.wasm_wide_arithmetic(enable);
        self.wasm_wide_arithmetic = enable;
        self
    }

    /// Configure WebAssembly stack switching support
    pub fn wasm_stack_switching(mut self, enable: bool) -> Self {
        self.config.wasm_stack_switching(enable);
        self.wasm_stack_switching = enable;
        self
    }

    /// Configure WebAssembly shared-everything-threads support
    pub fn wasm_shared_everything_threads(mut self, enable: bool) -> Self {
        self.config.wasm_shared_everything_threads(enable);
        self.wasm_shared_everything_threads = enable;
        self
    }

    /// Configure WebAssembly component model async support
    pub fn wasm_component_model_async(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_async(enable);
        self.wasm_component_model_async = enable;
        self
    }

    /// Configure WebAssembly component model async builtins support
    pub fn wasm_component_model_async_builtins(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_async_builtins(enable);
        self.wasm_component_model_async_builtins = enable;
        self
    }

    /// Configure WebAssembly component model async stackful support
    pub fn wasm_component_model_async_stackful(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_async_stackful(enable);
        self.wasm_component_model_async_stackful = enable;
        self
    }

    /// Configure WebAssembly component model error context support
    pub fn wasm_component_model_error_context(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_error_context(enable);
        self.wasm_component_model_error_context = enable;
        self
    }

    /// Configure WebAssembly component model GC support
    pub fn wasm_component_model_gc(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_gc(enable);
        self.wasm_component_model_gc = enable;
        self
    }

    /// Enable or disable fuel consumption tracking
    pub fn fuel_enabled(mut self, enable: bool) -> Self {
        if enable {
            self.config.consume_fuel(true);
        }
        self.fuel_enabled = enable;
        self
    }

    /// Set maximum memory pages limit (64KB per page)
    pub fn max_memory_pages(mut self, pages: u32) -> Self {
        // Note: Wasmtime doesn't have direct config for max memory pages
        // This is tracked for validation in the wrapper layer
        self.max_memory_pages = Some(pages);
        self
    }

    /// Set maximum stack size in bytes
    pub fn max_stack_size(mut self, size: usize) -> Self {
        // Note: Wasmtime doesn't expose direct stack size configuration
        // This is tracked for validation in the wrapper layer
        self.max_stack_size = Some(size);
        self
    }

    /// Enable or disable epoch-based interruption
    pub fn epoch_interruption(mut self, enable: bool) -> Self {
        if enable {
            self.config.epoch_interruption(true);
        }
        self.epoch_interruption = enable;
        self
    }

    /// Set maximum number of instances per engine
    pub fn max_instances(mut self, instances: u32) -> Self {
        // This is tracked for validation in the wrapper layer
        self.max_instances = Some(instances);
        self
    }

    /// Enable or disable async execution support
    pub fn async_support(mut self, enable: bool) -> Self {
        self.config.async_support(enable);
        self.async_support = enable;
        self
    }

    /// Enable or disable coredump generation on trap
    ///
    /// When enabled, traps will generate a coredump that can be used for
    /// post-mortem debugging of WebAssembly execution failures.
    pub fn coredump_on_trap(mut self, enable: bool) -> Self {
        self.config.coredump_on_trap(enable);
        self.coredump_on_trap = enable;
        self
    }

    /// Set memory reservation size in bytes
    ///
    /// This configures the size of the virtual memory reservation for linear
    /// memories. The default is 4GB when 64-bit memories are enabled, or
    /// whatever the default is in wasmtime.
    ///
    /// # Arguments
    /// * `size` - The reservation size in bytes
    pub fn memory_reservation(mut self, size: u64) -> Self {
        self.config.memory_reservation(size);
        self.memory_reservation = Some(size);
        self
    }

    /// Set memory guard size in bytes
    ///
    /// Guard pages are unmapped pages placed at the end of the memory to
    /// catch out-of-bounds accesses without explicit bounds checks.
    ///
    /// # Arguments
    /// * `size` - The guard size in bytes
    pub fn memory_guard_size(mut self, size: u64) -> Self {
        self.config.memory_guard_size(size);
        self.memory_guard_size = Some(size);
        self
    }

    /// Set memory reservation for growth in bytes
    ///
    /// This configures how much extra virtual memory is reserved after the
    /// initial memory size to allow for memory growth without needing to
    /// move the memory.
    ///
    /// # Arguments
    /// * `size` - The reservation for growth in bytes
    pub fn memory_reservation_for_growth(mut self, size: u64) -> Self {
        self.config.memory_reservation_for_growth(size);
        self.memory_reservation_for_growth = Some(size);
        self
    }

    /// Set maximum memory size in bytes
    ///
    /// This limits the maximum size that any linear memory can grow to.
    /// This is a wrapper-level limit tracked for validation.
    ///
    /// # Arguments
    /// * `size` - The maximum size in bytes
    pub fn max_memory_size(mut self, size: usize) -> Self {
        // Note: Wasmtime doesn't have direct config for max memory size
        // This is tracked for validation in the wrapper layer
        self.max_memory_size = Some(size);
        self
    }

    /// Enable or disable Cranelift debug verifier
    ///
    /// When enabled, Cranelift will perform additional verification checks
    /// on the generated machine code. This is useful for debugging compiler
    /// issues but adds overhead to compilation.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable the debug verifier
    pub fn cranelift_debug_verifier(mut self, enable: bool) -> Self {
        self.config.cranelift_debug_verifier(enable);
        self.cranelift_debug_verifier = enable;
        self
    }

    /// Enable or disable Cranelift NaN canonicalization
    ///
    /// When enabled, floating-point NaN values are canonicalized to a single
    /// representation. This ensures deterministic behavior across different
    /// platforms and is important for reproducible WebAssembly execution.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable NaN canonicalization
    pub fn cranelift_nan_canonicalization(mut self, enable: bool) -> Self {
        self.config.cranelift_nan_canonicalization(enable);
        self.cranelift_nan_canonicalization = enable;
        self
    }

    /// Enable or disable Cranelift proof-carrying code validation
    ///
    /// Proof-carrying code (PCC) is an advanced feature that enables runtime
    /// verification of safety properties of the generated machine code.
    /// This can be used to eliminate some runtime checks but adds compilation overhead.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable PCC validation
    pub fn cranelift_pcc(mut self, enable: bool) -> Self {
        self.config.cranelift_pcc(enable);
        self.cranelift_pcc = enable;
        self
    }

    /// Set the Cranelift register allocation algorithm
    ///
    /// This configures which register allocation algorithm Cranelift uses
    /// during code generation. Different algorithms have different trade-offs
    /// between compilation speed and generated code quality.
    ///
    /// # Arguments
    /// * `algorithm` - The register allocation algorithm to use
    pub fn cranelift_regalloc_algorithm(mut self, algorithm: RegallocAlgorithm) -> Self {
        self.config.cranelift_regalloc_algorithm(algorithm.clone());
        self.cranelift_regalloc_algorithm = Some(algorithm);
        self
    }

    /// Enable or disable wmemcheck (WebAssembly memory checker)
    ///
    /// wmemcheck is a memory debugging tool similar to Valgrind's memcheck that
    /// detects memory errors in WebAssembly programs at runtime. It can detect:
    /// - Use of uninitialized memory
    /// - Use-after-free (for tables)
    /// - Double-free errors (for tables)
    ///
    /// **Note**: This is only available when the `wmemcheck` Cargo feature is enabled.
    /// It adds significant runtime overhead and should only be used for debugging.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable wmemcheck memory checking
    #[cfg(feature = "wmemcheck")]
    pub fn wmemcheck(mut self, enable: bool) -> Self {
        self.config.wmemcheck(enable);
        self.wmemcheck_enabled = enable;
        self
    }

    /// Enable or disable table lazy initialization
    ///
    /// When enabled (the default), tables are initialized lazily which results in
    /// faster instantiation but slightly slower indirect calls. When disabled,
    /// tables are initialized eagerly during instantiation.
    ///
    /// This trade-off is typically worth it for modules that don't use all their
    /// table entries immediately after instantiation.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable lazy table initialization
    pub fn table_lazy_init(mut self, enable: bool) -> Self {
        self.config.table_lazy_init(enable);
        self.table_lazy_init = enable;
        self
    }

    /// Enable or disable GC support infrastructure
    ///
    /// GC support is required for the WebAssembly GC proposal, function references,
    /// and exception handling proposals. When disabled, these proposals cannot be used.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable GC support
    pub fn gc_support(mut self, enable: bool) -> Self {
        self.config.gc_support(enable);
        self.gc_support = enable;
        self
    }

    /// Configure which garbage collector implementation is used
    ///
    /// This selects the GC implementation for managing WebAssembly GC types.
    /// The default is `Collector::Auto` which selects an appropriate collector.
    ///
    /// # Arguments
    /// * `collector` - The collector implementation to use
    pub fn collector(mut self, collector: wasmtime::Collector) -> Self {
        self.config.collector(collector.clone());
        self.collector = Some(collector);
        self
    }

    /// Configure whether linear memories may relocate at runtime
    ///
    /// When enabled, Wasmtime may relocate the base pointer of a linear memory
    /// during memory growth operations. This allows for more efficient memory
    /// management but requires that host code never caches linear memory pointers.
    ///
    /// When disabled, the memory base pointer is guaranteed to remain stable,
    /// which is safer but may be less efficient.
    ///
    /// # Arguments
    /// * `enable` - Whether to allow memory relocation (default: true)
    pub fn memory_may_move(mut self, enable: bool) -> Self {
        self.config.memory_may_move(enable);
        self.memory_may_move = enable;
        self
    }

    /// Configure whether guard regions exist before linear memory
    ///
    /// Guard regions are unmapped pages that provide extra protection against
    /// buffer underflows. This is a defense-in-depth measure.
    ///
    /// # Arguments
    /// * `enable` - Whether to add guard regions before memory (default: true)
    pub fn guard_before_linear_memory(mut self, enable: bool) -> Self {
        self.config.guard_before_linear_memory(enable);
        self.guard_before_linear_memory = enable;
        self
    }

    /// Enable or disable copy-on-write memory initialization
    ///
    /// When enabled, Wasmtime uses memory-mapped files for data segment
    /// initialization, which can significantly speed up module instantiation
    /// for modules with large data sections.
    ///
    /// # Arguments
    /// * `enable` - Whether to use CoW memory initialization (default: true)
    pub fn memory_init_cow(mut self, enable: bool) -> Self {
        self.config.memory_init_cow(enable);
        self.memory_init_cow = enable;
        self
    }

    /// Configure WebAssembly component model threading support (experimental)
    ///
    /// This enables threading support in the component model, corresponding to
    /// the shared-everything-threads proposal. This feature is highly experimental.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable component model threading
    pub fn wasm_component_model_threading(mut self, enable: bool) -> Self {
        self.config.wasm_component_model_threading(enable);
        self.wasm_component_model_threading = enable;
        self
    }

    /// Configure deterministic relaxed SIMD behavior
    ///
    /// When enabled, relaxed SIMD instructions produce deterministic results
    /// across all platforms. This is useful for reproducible execution but
    /// may reduce performance on some platforms.
    ///
    /// # Arguments
    /// * `enable` - Whether to force deterministic relaxed SIMD (default: false)
    pub fn relaxed_simd_deterministic(mut self, enable: bool) -> Self {
        self.config.relaxed_simd_deterministic(enable);
        self.relaxed_simd_deterministic = enable;
        self
    }

    /// Configure async stack zeroing for defense-in-depth
    ///
    /// When enabled, async stacks are zeroed before (re)use. This provides
    /// defense-in-depth against information leakage but adds overhead.
    ///
    /// # Arguments
    /// * `enable` - Whether to zero async stacks (default: false)
    pub fn async_stack_zeroing(mut self, enable: bool) -> Self {
        self.config.async_stack_zeroing(enable);
        self.async_stack_zeroing = enable;
        self
    }

    /// Configure async stack size
    ///
    /// Sets the size of the stack used for async execution. This is separate
    /// from the WebAssembly linear memory stack and is used by the async
    /// runtime for suspending and resuming execution.
    ///
    /// # Arguments
    /// * `size` - The async stack size in bytes
    pub fn async_stack_size(mut self, size: usize) -> Self {
        self.config.async_stack_size(size);
        self.async_stack_size = Some(size);
        self
    }

    /// Configure parallel compilation
    ///
    /// When enabled (the default), module compilation uses multiple threads.
    /// Disabling this can be useful for debugging or in resource-constrained
    /// environments.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable parallel compilation (default: true)
    pub fn parallel_compilation(mut self, enable: bool) -> Self {
        self.config.parallel_compilation(enable);
        self.parallel_compilation = enable;
        self
    }

    /// Configure Mach port usage on macOS
    ///
    /// When enabled (the default on macOS), Wasmtime uses Mach ports instead
    /// of Unix signals for exception handling. This is more reliable but
    /// requires macOS-specific code paths.
    ///
    /// # Arguments
    /// * `enable` - Whether to use Mach ports on macOS (default: true)
    #[cfg(target_os = "macos")]
    pub fn macos_use_mach_ports(mut self, enable: bool) -> Self {
        self.config.macos_use_mach_ports(enable);
        self.macos_use_mach_ports = enable;
        self
    }

    /// Configure module version strategy for serialization
    ///
    /// This controls how module version compatibility is checked when
    /// deserializing precompiled modules. The default uses Wasmtime's version.
    ///
    /// # Arguments
    /// * `strategy` - The version strategy to use
    pub fn module_version_strategy(mut self, strategy: wasmtime::ModuleVersionStrategy) -> Self {
        let _ = self.config.module_version(strategy.clone());
        self.module_version_strategy = Some(strategy);
        self
    }

    /// Configure instance allocation strategy
    ///
    /// This configures how WebAssembly instances are allocated. The default
    /// is on-demand allocation. Pooling allocation can improve instantiation
    /// performance for high-throughput scenarios.
    ///
    /// # Arguments
    /// * `strategy` - The allocation strategy to use
    pub fn allocation_strategy(mut self, strategy: wasmtime::InstanceAllocationStrategy) -> Self {
        self.config.allocation_strategy(strategy.clone());
        self.allocation_strategy = Some(strategy);
        self
    }

    /// Build engine with current configuration
    pub fn build(self) -> WasmtimeResult<Engine> {
        let summary = EngineConfigSummary::from_builder(&self);
        
        let engine = WasmtimeEngine::new(&self.config)
            .map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to create Wasmtime engine: {}", e),
            })?;

        Ok(Engine {
            inner: Arc::new(engine),
            config_summary: summary,
        })
    }
}

impl EngineConfigSummary {
    /// Compute a deterministic hash string representing this engine configuration.
    /// This is used for module serialization compatibility checking.
    pub fn to_hash_string(&self) -> String {
        use sha2::{Sha256, Digest};
        let mut hasher = Sha256::new();

        // Include all configuration fields that affect module compatibility
        hasher.update(self.strategy.as_bytes());
        hasher.update(self.opt_level.as_bytes());
        hasher.update(if self.debug_info { b"1" } else { b"0" });
        hasher.update(if self.wasm_threads { b"1" } else { b"0" });
        hasher.update(if self.wasm_reference_types { b"1" } else { b"0" });
        hasher.update(if self.wasm_simd { b"1" } else { b"0" });
        hasher.update(if self.wasm_bulk_memory { b"1" } else { b"0" });
        hasher.update(if self.wasm_multi_value { b"1" } else { b"0" });
        hasher.update(if self.wasm_multi_memory { b"1" } else { b"0" });
        hasher.update(if self.wasm_tail_call { b"1" } else { b"0" });
        hasher.update(if self.wasm_relaxed_simd { b"1" } else { b"0" });
        hasher.update(if self.wasm_function_references { b"1" } else { b"0" });
        hasher.update(if self.wasm_gc { b"1" } else { b"0" });
        hasher.update(if self.wasm_exceptions { b"1" } else { b"0" });
        hasher.update(if self.wasm_memory64 { b"1" } else { b"0" });
        hasher.update(if self.wasm_extended_const { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model { b"1" } else { b"0" });
        hasher.update(if self.wasm_custom_page_sizes { b"1" } else { b"0" });
        hasher.update(if self.wasm_wide_arithmetic { b"1" } else { b"0" });
        hasher.update(if self.wasm_stack_switching { b"1" } else { b"0" });
        hasher.update(if self.wasm_shared_everything_threads { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model_async { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model_async_builtins { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model_async_stackful { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model_error_context { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model_gc { b"1" } else { b"0" });
        hasher.update(if self.fuel_enabled { b"1" } else { b"0" });
        hasher.update(if self.epoch_interruption { b"1" } else { b"0" });
        hasher.update(if self.async_support { b"1" } else { b"0" });
        hasher.update(if self.coredump_on_trap { b"1" } else { b"0" });
        hasher.update(if self.cranelift_debug_verifier { b"1" } else { b"0" });
        hasher.update(if self.cranelift_nan_canonicalization { b"1" } else { b"0" });
        hasher.update(if self.cranelift_pcc { b"1" } else { b"0" });
        hasher.update(self.cranelift_regalloc_algorithm.as_bytes());
        hasher.update(if self.wmemcheck_enabled { b"1" } else { b"0" });
        hasher.update(if self.table_lazy_init { b"1" } else { b"0" });
        // New config options
        hasher.update(if self.gc_support { b"1" } else { b"0" });
        hasher.update(self.collector.as_bytes());
        hasher.update(if self.memory_may_move { b"1" } else { b"0" });
        hasher.update(if self.guard_before_linear_memory { b"1" } else { b"0" });
        hasher.update(if self.memory_init_cow { b"1" } else { b"0" });
        hasher.update(if self.wasm_component_model_threading { b"1" } else { b"0" });
        hasher.update(if self.relaxed_simd_deterministic { b"1" } else { b"0" });
        hasher.update(if self.async_stack_zeroing { b"1" } else { b"0" });
        hasher.update(if self.parallel_compilation { b"1" } else { b"0" });
        hasher.update(if self.macos_use_mach_ports { b"1" } else { b"0" });
        hasher.update(self.module_version_strategy.as_bytes());
        hasher.update(self.allocation_strategy.as_bytes());

        // Include target architecture and OS for cross-platform compatibility
        hasher.update(std::env::consts::ARCH.as_bytes());
        hasher.update(std::env::consts::OS.as_bytes());

        // Include Wasmtime version for ABI compatibility
        hasher.update(env!("CARGO_PKG_VERSION").as_bytes());

        format!("{:x}", hasher.finalize())
    }

    fn from_config(_config: &Config) -> Self {
        // Note: Wasmtime Config doesn't expose all settings for introspection
        // We track what we can and make reasonable assumptions
        EngineConfigSummary {
            strategy: "Cranelift".to_string(), // Default assumption
            opt_level: "Speed".to_string(),    // Default assumption
            debug_info: false,                 // Default assumption
            wasm_threads: true,                // Common default
            wasm_reference_types: true,        // Commonly enabled
            wasm_simd: true,                   // Commonly enabled
            wasm_bulk_memory: true,            // Commonly enabled
            wasm_multi_value: true,            // Commonly enabled
            wasm_multi_memory: false,          // Tier 3 feature - off by default
            wasm_tail_call: false,             // Tier 3 feature - off by default
            wasm_relaxed_simd: false,          // Tier 3 feature - off by default
            wasm_function_references: false,   // Tier 3 feature - off by default
            wasm_gc: false,                    // Tier 3 feature - off by default
            wasm_exceptions: false,            // Tier 3 feature - off by default
            wasm_memory64: true,               // Tier 1 feature - on by default
            wasm_extended_const: true,         // Tier 1 feature - on by default
            wasm_component_model: true,        // Tier 1 feature - on by default
            wasm_custom_page_sizes: false,     // Tier 3 feature - off by default
            wasm_wide_arithmetic: false,       // Tier 3 feature - off by default
            wasm_stack_switching: false,       // Tier 3 feature - off by default
            wasm_shared_everything_threads: false,  // Tier 3 feature - off by default
            wasm_component_model_async: false,  // Component model extension - off by default
            wasm_component_model_async_builtins: false,  // Component model extension - off by default
            wasm_component_model_async_stackful: false,  // Component model extension - off by default
            wasm_component_model_error_context: false,  // Component model extension - off by default
            wasm_component_model_gc: false,  // Component model extension - off by default
            fuel_enabled: true,                // Default assumption (enabled for Store operations)
            max_memory_pages: None,            // No limit by default
            max_stack_size: None,              // No limit by default
            epoch_interruption: false,         // Default assumption
            max_instances: None,               // No limit by default
            async_support: false,              // Off by default
            coredump_on_trap: false,           // Off by default
            memory_reservation: None,          // No custom reservation by default
            memory_guard_size: None,           // No custom guard size by default
            memory_reservation_for_growth: None, // No custom growth reservation by default
            max_memory_size: None,             // No custom max memory by default
            cranelift_debug_verifier: false,   // Off by default
            cranelift_nan_canonicalization: false, // Off by default
            cranelift_pcc: false,              // Off by default
            cranelift_regalloc_algorithm: "Backtracking".to_string(), // Default algorithm
            wmemcheck_enabled: false,          // Off by default (requires wmemcheck feature)
            table_lazy_init: true,             // On by default (wasmtime default)
            // New config options with wasmtime defaults
            gc_support: true,                  // GC support - on by default
            collector: "Auto".to_string(),     // Collector - Auto by default
            memory_may_move: true,             // Memory may move - on by default
            guard_before_linear_memory: true,  // Guard before memory - on by default
            memory_init_cow: true,             // CoW memory init - on by default
            wasm_component_model_threading: false, // Component threading - off (experimental)
            relaxed_simd_deterministic: false, // Relaxed SIMD deterministic - off by default
            async_stack_zeroing: false,        // Async stack zeroing - off by default
            async_stack_size: None,            // Async stack size - use default
            parallel_compilation: true,        // Parallel compilation - on by default
            macos_use_mach_ports: true,        // Mach ports on macOS - on by default
            module_version_strategy: "WasmtimeVersion".to_string(), // Use wasmtime version
            allocation_strategy: "OnDemand".to_string(), // On-demand allocation by default
        }
    }

    fn from_builder(builder: &EngineBuilder) -> Self {
        EngineConfigSummary {
            strategy: builder.strategy.as_ref().map(|s| match s {
                Strategy::Cranelift => "Cranelift".to_string(),
                _ => "Auto".to_string(),
            }).unwrap_or_else(|| "Auto".to_string()),
            opt_level: builder.opt_level.as_ref().map(|o| match o {
                OptLevel::None => "None".to_string(),
                OptLevel::Speed => "Speed".to_string(),
                OptLevel::SpeedAndSize => "SpeedAndSize".to_string(),
                _ => "Auto".to_string(),
            }).unwrap_or_else(|| "Auto".to_string()),
            debug_info: builder.debug_info,
            wasm_threads: builder.wasm_threads,
            wasm_reference_types: builder.wasm_reference_types,
            wasm_simd: builder.wasm_simd,
            wasm_bulk_memory: builder.wasm_bulk_memory,
            wasm_multi_value: builder.wasm_multi_value,
            wasm_multi_memory: builder.wasm_multi_memory,
            wasm_tail_call: builder.wasm_tail_call,
            wasm_relaxed_simd: builder.wasm_relaxed_simd,
            wasm_function_references: builder.wasm_function_references,
            wasm_gc: builder.wasm_gc,
            wasm_exceptions: builder.wasm_exceptions,
            wasm_memory64: builder.wasm_memory64,
            wasm_extended_const: builder.wasm_extended_const,
            wasm_component_model: builder.wasm_component_model,
            wasm_custom_page_sizes: builder.wasm_custom_page_sizes,
            wasm_wide_arithmetic: builder.wasm_wide_arithmetic,
            wasm_stack_switching: builder.wasm_stack_switching,
            wasm_shared_everything_threads: builder.wasm_shared_everything_threads,
            wasm_component_model_async: builder.wasm_component_model_async,
            wasm_component_model_async_builtins: builder.wasm_component_model_async_builtins,
            wasm_component_model_async_stackful: builder.wasm_component_model_async_stackful,
            wasm_component_model_error_context: builder.wasm_component_model_error_context,
            wasm_component_model_gc: builder.wasm_component_model_gc,
            fuel_enabled: builder.fuel_enabled,
            max_memory_pages: builder.max_memory_pages,
            max_stack_size: builder.max_stack_size,
            epoch_interruption: builder.epoch_interruption,
            max_instances: builder.max_instances,
            async_support: builder.async_support,
            coredump_on_trap: builder.coredump_on_trap,
            memory_reservation: builder.memory_reservation,
            memory_guard_size: builder.memory_guard_size,
            memory_reservation_for_growth: builder.memory_reservation_for_growth,
            max_memory_size: builder.max_memory_size,
            cranelift_debug_verifier: builder.cranelift_debug_verifier,
            cranelift_nan_canonicalization: builder.cranelift_nan_canonicalization,
            cranelift_pcc: builder.cranelift_pcc,
            cranelift_regalloc_algorithm: builder.cranelift_regalloc_algorithm.as_ref().map(|a| match a {
                RegallocAlgorithm::Backtracking => "Backtracking".to_string(),
                RegallocAlgorithm::SinglePass => "SinglePass".to_string(),
                _ => "Unknown".to_string(),
            }).unwrap_or_else(|| "Backtracking".to_string()),
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: builder.wmemcheck_enabled,
            #[cfg(not(feature = "wmemcheck"))]
            wmemcheck_enabled: false,
            table_lazy_init: builder.table_lazy_init,
            // New config options
            gc_support: builder.gc_support,
            collector: builder.collector.as_ref().map(|c| match c {
                wasmtime::Collector::Auto => "Auto".to_string(),
                wasmtime::Collector::DeferredReferenceCounting => "DeferredReferenceCounting".to_string(),
                wasmtime::Collector::Null => "Null".to_string(),
                _ => "Unknown".to_string(),
            }).unwrap_or_else(|| "Auto".to_string()),
            memory_may_move: builder.memory_may_move,
            guard_before_linear_memory: builder.guard_before_linear_memory,
            memory_init_cow: builder.memory_init_cow,
            wasm_component_model_threading: builder.wasm_component_model_threading,
            relaxed_simd_deterministic: builder.relaxed_simd_deterministic,
            async_stack_zeroing: builder.async_stack_zeroing,
            async_stack_size: builder.async_stack_size,
            parallel_compilation: builder.parallel_compilation,
            macos_use_mach_ports: builder.macos_use_mach_ports,
            module_version_strategy: builder.module_version_strategy.as_ref().map(|s| match s {
                wasmtime::ModuleVersionStrategy::WasmtimeVersion => "WasmtimeVersion".to_string(),
                wasmtime::ModuleVersionStrategy::None => "None".to_string(),
                wasmtime::ModuleVersionStrategy::Custom(_) => "Custom".to_string(),
            }).unwrap_or_else(|| "WasmtimeVersion".to_string()),
            allocation_strategy: builder.allocation_strategy.as_ref().map(|s| match s {
                wasmtime::InstanceAllocationStrategy::OnDemand => "OnDemand".to_string(),
                wasmtime::InstanceAllocationStrategy::Pooling(_) => "Pooling".to_string(),
                _ => "Unknown".to_string(),
            }).unwrap_or_else(|| "OnDemand".to_string()),
        }
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
                let mut config = Config::new();
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
                    },
                    Err(_) => {
                        // Last resort: create engine with completely default wasmtime config
                        // This should virtually never fail unless the system is severely broken
                        let default_config = Config::default();
                        Engine {
                            inner: Arc::new(WasmtimeEngine::new(&default_config)
                                .unwrap_or_else(|_| panic!("Critical: Cannot create fallback engine - system unusable"))),
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_engine_creation() {
        let engine = Engine::new().expect("Failed to create engine");
        assert!(engine.validate().is_ok());
    }

    #[test]
    fn test_engine_builder() {
        let engine = Engine::builder()
            .opt_level(OptLevel::None)
            .debug_info(true)
            .wasm_threads(false)
            .build()
            .expect("Failed to build engine");

        assert!(engine.validate().is_ok());
        assert!(!engine.supports_feature(WasmFeature::Threads));
    }

    #[test]
    fn test_engine_clone() {
        let engine1 = Engine::new().expect("Failed to create engine");
        let engine2 = engine1.clone();
        
        assert!(engine1.validate().is_ok());
        assert!(engine2.validate().is_ok());
        
        // Should share the same underlying engine
        assert!(Arc::ptr_eq(&engine1.inner, &engine2.inner));
    }

    #[test]
    fn test_feature_support() {
        let engine = Engine::builder()
            .wasm_simd(true)
            .wasm_reference_types(true)
            .build()
            .expect("Failed to build engine");

        assert!(engine.supports_feature(WasmFeature::Simd));
        assert!(engine.supports_feature(WasmFeature::ReferenceTypes));
    }

    #[test]
    fn test_fuel_configuration() {
        let engine = Engine::builder()
            .fuel_enabled(true)
            .build()
            .expect("Failed to build engine with fuel");

        assert!(engine.fuel_enabled());
    }

    #[test]
    fn test_memory_limits() {
        let engine = Engine::builder()
            .max_memory_pages(1024) // 64MB limit (1024 * 64KB)
            .build()
            .expect("Failed to build engine with memory limits");

        assert_eq!(engine.memory_limit_pages(), Some(1024));
    }

    #[test]
    fn test_stack_limits() {
        let engine = Engine::builder()
            .max_stack_size(2 * 1024 * 1024) // 2MB stack
            .build()
            .expect("Failed to build engine with stack limits");

        assert_eq!(engine.stack_size_limit(), Some(2 * 1024 * 1024));
    }

    #[test]
    fn test_epoch_interruption() {
        let engine = Engine::builder()
            .epoch_interruption(true)
            .build()
            .expect("Failed to build engine with epoch interruption");

        assert!(engine.epoch_interruption_enabled());
    }

    #[test]
    fn test_instance_limits() {
        let engine = Engine::builder()
            .max_instances(100)
            .build()
            .expect("Failed to build engine with instance limits");

        assert_eq!(engine.max_instances(), Some(100));
    }

    #[test]
    fn test_comprehensive_configuration() {
        let engine = Engine::builder()
            .strategy(Strategy::Cranelift)
            .opt_level(OptLevel::SpeedAndSize)
            .debug_info(true)
            .fuel_enabled(true)
            .max_memory_pages(512)
            .max_stack_size(1024 * 1024)
            .epoch_interruption(true)
            .max_instances(50)
            .wasm_threads(true)
            .wasm_simd(true)
            .build()
            .expect("Failed to build comprehensive engine configuration");

        assert!(engine.validate().is_ok());
        assert!(engine.fuel_enabled());
        assert!(engine.epoch_interruption_enabled());
        assert_eq!(engine.memory_limit_pages(), Some(512));
        assert_eq!(engine.stack_size_limit(), Some(1024 * 1024));
        assert_eq!(engine.max_instances(), Some(50));
        
        let config = engine.config_summary();
        assert_eq!(config.strategy, "Cranelift");
        assert_eq!(config.opt_level, "SpeedAndSize");
        assert!(config.debug_info);
    }

    #[test]
    fn test_async_support() {
        let engine = Engine::builder()
            .async_support(true)
            .build()
            .expect("Failed to build engine with async support");

        assert!(engine.validate().is_ok());
        assert!(engine.async_support_enabled());

        let config = engine.config_summary();
        assert!(config.async_support);
    }

    #[test]
    #[cfg(feature = "async")]
    fn test_async_function_execution() {
        use wasmtime::{Func, FuncType, Store, ValType, Val};
        use crate::store::StoreData;

        // Create async-enabled engine
        let engine = Engine::builder()
            .async_support(true)
            .build()
            .expect("Failed to build async engine");

        assert!(engine.async_support_enabled());

        // Create store with async engine
        let mut store = Store::new(engine.inner.as_ref(), StoreData::default());

        // Create a simple async host function that adds two i32 values
        let func_type = FuncType::new(
            vec![ValType::I32, ValType::I32],
            vec![ValType::I32]
        );

        let func = Func::new_async(
            &mut store,
            func_type,
            |_caller, params: &[Val], results: &mut [Val]| {
                Box::new(async move {
                    if let (Some(Val::I32(a)), Some(Val::I32(b))) = (params.get(0), params.get(1)) {
                        results[0] = Val::I32(a + b);
                        Ok(())
                    } else {
                        Err(anyhow::anyhow!("Invalid parameters"))
                    }
                })
            }
        );

        // Call the async function using the runtime
        let runtime = crate::async_runtime::get_async_runtime();
        let params = vec![Val::I32(10), Val::I32(32)];
        let mut results = vec![Val::I32(0)];

        runtime.block_on(async {
            func.call_async(&mut store, &params, &mut results)
                .await
                .expect("Async function call failed");
        });

        // Verify result
        if let Val::I32(result) = results[0] {
            assert_eq!(result, 42, "Async function should return 10 + 32 = 42");
        } else {
            panic!("Expected I32 result");
        }
    }
}

//
// Native C exports for JNI and Panama FFI consumption
//

use std::os::raw::{c_void, c_int};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Create a new engine with default configuration
///
/// # Safety
///
/// Returns pointer to engine that must be freed with wasmtime4j_engine_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_new() -> *mut c_void {
    match core::create_engine() {
        Ok(engine) => Box::into_raw(engine) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Alias for wasmtime4j_engine_new (Panama FFI compatibility)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_create() -> *mut c_void {
    wasmtime4j_engine_new()
}

/// Create a new engine with custom configuration
///
/// # Safety
///
/// Returns pointer to engine that must be freed with wasmtime4j_engine_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_new_with_config(
    debug_info: c_int,
    wasm_threads: c_int,
    wasm_simd: c_int,
    wasm_reference_types: c_int,
    wasm_bulk_memory: c_int,
    wasm_multi_value: c_int,
    fuel_enabled: c_int,
    max_memory_pages: u32,
    max_stack_size: usize,
    epoch_interruption: c_int,
    max_instances: u32,
) -> *mut c_void {
    let opt_max_memory_pages = if max_memory_pages == 0 { None } else { Some(max_memory_pages) };
    let opt_max_stack_size = if max_stack_size == 0 { None } else { Some(max_stack_size) };
    let opt_max_instances = if max_instances == 0 { None } else { Some(max_instances) };

    match core::create_engine_with_config(
        Some(Strategy::Cranelift),
        Some(OptLevel::Speed),
        debug_info != 0,
        wasm_threads != 0,
        wasm_simd != 0,
        wasm_reference_types != 0,
        wasm_bulk_memory != 0,
        wasm_multi_value != 0,
        fuel_enabled != 0,
        opt_max_memory_pages,
        opt_max_stack_size,
        epoch_interruption != 0,
        opt_max_instances,
        false,  // async_support - TODO: add parameter
    ) {
        Ok(engine) => Box::into_raw(engine) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Destroy engine and free resources
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_destroy(engine_ptr: *mut c_void) {
    if !engine_ptr.is_null() {
        core::destroy_engine(engine_ptr);
    }
}

/// Validate engine is still functional
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_validate(engine_ptr: *const c_void) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => match core::validate_engine(engine) {
            Ok(_) => FFI_SUCCESS,
            Err(_) => FFI_ERROR,
        },
        Err(_) => FFI_ERROR,
    }
}

/// Check if engine supports specific WebAssembly feature
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_supports_feature(
    engine_ptr: *const c_void,
    feature: c_int,
) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            let wasm_feature = match feature {
                0 => WasmFeature::Threads,
                1 => WasmFeature::ReferenceTypes,
                2 => WasmFeature::Simd,
                3 => WasmFeature::BulkMemory,
                4 => WasmFeature::MultiValue,
                _ => return FFI_ERROR,
            };
            if core::check_feature_support(engine, wasm_feature) { 1 } else { 0 }
        },
        Err(_) => FFI_ERROR,
    }
}

/// Get memory limit in pages (64KB per page)
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_memory_limit_pages(engine_ptr: *const c_void) -> u32 {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => core::get_memory_limit(engine).unwrap_or(0),
        Err(_) => 0,
    }
}

/// Get stack size limit in bytes
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_stack_size_limit(engine_ptr: *const c_void) -> usize {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => core::get_stack_limit(engine).unwrap_or(0),
        Err(_) => 0,
    }
}

/// Check if fuel consumption is enabled
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_fuel_enabled(engine_ptr: *const c_void) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => if core::is_fuel_enabled(engine) { 1 } else { 0 },
        Err(_) => FFI_ERROR,
    }
}

/// Check if epoch-based interruption is enabled
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_epoch_interruption_enabled(engine_ptr: *const c_void) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => if core::is_epoch_interruption_enabled(engine) { 1 } else { 0 },
        Err(_) => FFI_ERROR,
    }
}

/// Get maximum instances limit
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_max_instances(engine_ptr: *const c_void) -> u32 {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => core::get_max_instances(engine).unwrap_or(0),
        Err(_) => 0,
    }
}

/// Get engine reference count for debugging
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_reference_count(engine_ptr: *const c_void) -> usize {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => core::get_reference_count(engine),
        Err(_) => 0,
    }
}

/// Check if coredump generation on trap is enabled
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_coredump_on_trap_enabled(engine_ptr: *const c_void) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => if core::is_coredump_on_trap_enabled(engine) { 1 } else { 0 },
        Err(_) => FFI_ERROR,
    }
}

/// Precompile a WebAssembly module for AOT (ahead-of-time) usage
///
/// Takes raw WebAssembly bytes and compiles them to a serialized form that can
/// be loaded later via Module::deserialize without needing to recompile.
///
/// # Safety
///
/// - engine_ptr must be a valid pointer from wasmtime4j_engine_new
/// - wasm_bytes must be a valid pointer to a byte array of length wasm_len
/// - out_data must be a valid pointer to receive the output byte array pointer
/// - out_len must be a valid pointer to receive the output byte array length
///
/// Returns 0 on success, non-zero on failure.
/// The caller is responsible for freeing the output data with wasmtime4j_free_bytes.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_precompile_module(
    engine_ptr: *const c_void,
    wasm_bytes: *const u8,
    wasm_len: usize,
    out_data: *mut *mut u8,
    out_len: *mut usize,
) -> c_int {
    if engine_ptr.is_null() || wasm_bytes.is_null() || out_data.is_null() || out_len.is_null() {
        return FFI_ERROR;
    }

    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            let bytes = std::slice::from_raw_parts(wasm_bytes, wasm_len);
            match core::precompile_module(engine, bytes) {
                Ok(precompiled) => {
                    let len = precompiled.len();
                    let data = Box::into_raw(precompiled.into_boxed_slice()) as *mut u8;
                    *out_data = data;
                    *out_len = len;
                    0  // Success
                }
                Err(_) => FFI_ERROR,
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Create a new engine with extended configuration including memory settings
///
/// This extends wasmtime4j_engine_new_with_config with additional memory configuration options.
///
/// # Safety
///
/// This function is unsafe because it's called from FFI
///
/// Returns pointer to engine that must be freed with wasmtime4j_engine_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_new_with_memory_config(
    debug_info: c_int,
    wasm_threads: c_int,
    wasm_simd: c_int,
    wasm_reference_types: c_int,
    wasm_bulk_memory: c_int,
    wasm_multi_value: c_int,
    fuel_enabled: c_int,
    max_memory_pages: u32,
    max_stack_size: usize,
    epoch_interruption: c_int,
    max_instances: u32,
    memory_reservation: u64,
    memory_guard_size: u64,
    memory_reservation_for_growth: u64,
) -> *mut c_void {
    let mut builder = Engine::builder()
        .strategy(Strategy::Cranelift)
        .opt_level(OptLevel::Speed)
        .debug_info(debug_info != 0)
        .wasm_threads(wasm_threads != 0)
        .wasm_simd(wasm_simd != 0)
        .wasm_reference_types(wasm_reference_types != 0)
        .wasm_bulk_memory(wasm_bulk_memory != 0)
        .wasm_multi_value(wasm_multi_value != 0)
        .fuel_enabled(fuel_enabled != 0)
        .epoch_interruption(epoch_interruption != 0);

    if max_memory_pages > 0 {
        builder = builder.max_memory_pages(max_memory_pages);
    }
    if max_stack_size > 0 {
        builder = builder.max_stack_size(max_stack_size);
    }
    if max_instances > 0 {
        builder = builder.max_instances(max_instances);
    }
    if memory_reservation > 0 {
        builder = builder.memory_reservation(memory_reservation);
    }
    if memory_guard_size > 0 {
        builder = builder.memory_guard_size(memory_guard_size);
    }
    if memory_reservation_for_growth > 0 {
        builder = builder.memory_reservation_for_growth(memory_reservation_for_growth);
    }

    match builder.build() {
        Ok(engine) => Box::into_raw(Box::new(engine)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Get the configured memory reservation size for an engine
///
/// Returns the memory reservation size in bytes, or 0 if not configured or on error.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_memory_reservation(engine_ptr: *const c_void) -> u64 {
    if engine_ptr.is_null() {
        return 0;
    }
    let engine = &*(engine_ptr as *const Engine);
    engine.config_summary.memory_reservation.unwrap_or(0)
}

/// Get the configured memory guard size for an engine
///
/// Returns the memory guard size in bytes, or 0 if not configured or on error.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_memory_guard_size(engine_ptr: *const c_void) -> u64 {
    if engine_ptr.is_null() {
        return 0;
    }
    let engine = &*(engine_ptr as *const Engine);
    engine.config_summary.memory_guard_size.unwrap_or(0)
}

/// Get the configured memory reservation for growth for an engine
///
/// Returns the memory reservation for growth in bytes, or 0 if not configured or on error.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_memory_reservation_for_growth(engine_ptr: *const c_void) -> u64 {
    if engine_ptr.is_null() {
        return 0;
    }
    let engine = &*(engine_ptr as *const Engine);
    engine.config_summary.memory_reservation_for_growth.unwrap_or(0)
}

/// Get the configured max memory size for an engine
///
/// Returns the max memory size in bytes, or 0 if not configured or on error.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_max_memory_size(engine_ptr: *const c_void) -> usize {
    if engine_ptr.is_null() {
        return 0;
    }
    let engine = &*(engine_ptr as *const Engine);
    engine.config_summary.max_memory_size.unwrap_or(0)
}

/// Check if wmemcheck (WebAssembly memory checker) is enabled for an engine
///
/// wmemcheck is a memory debugging tool similar to Valgrind's memcheck that
/// detects memory errors in WebAssembly programs at runtime. It can detect:
/// - Use of uninitialized memory
/// - Use-after-free (for tables)
/// - Double-free errors (for tables)
///
/// Returns 1 if wmemcheck is enabled, 0 if disabled or not supported, -1 on error.
///
/// **Note**: wmemcheck adds significant runtime overhead and is only available
/// when the `wmemcheck` Cargo feature is enabled during compilation.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_wmemcheck_enabled(engine_ptr: *const c_void) -> c_int {
    if engine_ptr.is_null() {
        return FFI_ERROR;
    }
    let engine = &*(engine_ptr as *const Engine);
    if engine.config_summary.wmemcheck_enabled { 1 } else { 0 }
}

/// Check if wmemcheck feature is available in this build
///
/// Returns 1 if the wmemcheck feature was compiled in, 0 otherwise.
/// This allows Java code to check if wmemcheck can be enabled before
/// attempting to create an engine with wmemcheck enabled.
///
/// # Safety
///
/// This function is always safe to call.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wmemcheck_available() -> c_int {
    #[cfg(feature = "wmemcheck")]
    {
        1
    }
    #[cfg(not(feature = "wmemcheck"))]
    {
        0
    }
}

/// Check if table lazy initialization is enabled for an engine
///
/// Table lazy initialization is a performance optimization that defers table
/// initialization until entries are actually used. This results in:
/// - Faster instantiation (tables don't need to be fully populated upfront)
/// - Slightly slower indirect calls (first access may need to initialize the entry)
///
/// Returns 1 if table lazy initialization is enabled, 0 if disabled, -1 on error.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_table_lazy_init_enabled(engine_ptr: *const c_void) -> c_int {
    if engine_ptr.is_null() {
        return FFI_ERROR;
    }
    let engine = &*(engine_ptr as *const Engine);
    if engine.config_summary.table_lazy_init { 1 } else { 0 }
}