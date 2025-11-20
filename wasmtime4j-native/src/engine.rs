//! Wasmtime engine management with comprehensive configuration and lifecycle support
//!
//! This module provides defensive, thread-safe wrapper around Wasmtime engines
//! with proper resource management and JVM crash prevention.

use std::sync::Arc;
use wasmtime::{Config, Engine as WasmtimeEngine, OptLevel, Strategy};
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
}

/// Builder for creating configured engines
#[derive(Debug)]
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
    pub(crate) fn inner(&self) -> &WasmtimeEngine {
        &self.inner
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

    /// Check if async execution support is enabled
    pub fn async_support_enabled(&self) -> bool {
        self.config_summary.async_support
    }

    /// Check engine reference count for debugging
    pub fn reference_count(&self) -> usize {
        Arc::strong_count(&self.inner)
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
    pub fn create_engine() -> WasmtimeResult<Box<Engine>> {
        Engine::new().map(Box::new)
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

    /// Core function to get maximum instances limit
    pub fn get_max_instances(engine: &Engine) -> Option<u32> {
        engine.max_instances()
    }

    /// Core function to get engine reference count
    pub fn get_reference_count(engine: &Engine) -> usize {
        engine.reference_count()
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
    pub fn wasm_threads(mut self, enable: bool) -> Self {
        self.config.wasm_threads(enable);
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