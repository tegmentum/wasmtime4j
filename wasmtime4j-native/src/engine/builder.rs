//! Engine builder for creating configured engines
//!
//! This module provides the EngineBuilder and EngineConfigSummary types
//! for creating and configuring Wasmtime engines with various options.

use std::sync::atomic::AtomicBool;
use std::sync::{Arc, RwLock};

use wasmtime::{Config, Engine as WasmtimeEngine, OptLevel, RegallocAlgorithm, Strategy};

use super::{safe_wasmtime_config, Engine};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Summary of engine configuration for runtime introspection and FFI queries.
///
/// Only contains fields that are read after engine construction:
/// - Runtime-essential fields used by store/instance creation
/// - Feature flags exposed to Java via `supports_feature()` and FFI getters
///
/// Construction-only settings (strategy, opt_level, debug_info, etc.) are
/// applied to `wasmtime::Config` during `EngineBuilder::build()` and not stored.
#[derive(Debug, Clone)]
pub struct EngineConfigSummary {
    // ===== Wasm feature flags (exposed via supports_feature()) =====
    pub wasm_threads: bool,
    pub wasm_reference_types: bool,
    pub wasm_simd: bool,
    pub wasm_bulk_memory: bool,
    pub wasm_multi_value: bool,
    pub wasm_multi_memory: bool,
    pub wasm_tail_call: bool,
    pub wasm_relaxed_simd: bool,
    pub wasm_function_references: bool,
    pub wasm_gc: bool,
    pub wasm_exceptions: bool,
    pub wasm_memory64: bool,
    pub wasm_extended_const: bool,
    pub wasm_component_model: bool,
    pub wasm_custom_page_sizes: bool,
    pub wasm_wide_arithmetic: bool,
    pub wasm_stack_switching: bool,
    pub wasm_shared_everything_threads: bool,
    pub wasm_component_model_async: bool,
    pub wasm_component_model_async_builtins: bool,
    pub wasm_component_model_async_stackful: bool,
    pub wasm_component_model_error_context: bool,
    pub wasm_component_model_gc: bool,
    pub wasm_component_model_threading: bool,

    // ===== Runtime-essential fields =====
    /// Whether fuel consumption is enabled (drives store fuel initialization)
    pub fuel_enabled: bool,
    /// Whether epoch-based interruption is enabled (drives store epoch setup)
    pub epoch_interruption: bool,

    // ===== Introspection mirrors (Engine accessor methods) =====
    pub max_stack_size: Option<usize>,
    pub async_support: bool,
    pub coredump_on_trap: bool,

    // ===== FFI getter fields =====
    pub memory_reservation: Option<u64>,
    pub memory_guard_size: Option<u64>,
    pub memory_reservation_for_growth: Option<u64>,
    pub wmemcheck_enabled: bool,
    pub table_lazy_init: bool,
}

impl Default for EngineConfigSummary {
    fn default() -> Self {
        Self {
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
            wasm_component_model_threading: false,
            fuel_enabled: false,
            epoch_interruption: false,
            max_stack_size: None,
            async_support: false,
            coredump_on_trap: false,
            memory_reservation: None,
            memory_guard_size: None,
            memory_reservation_for_growth: None,
            wmemcheck_enabled: false,
            table_lazy_init: true,
        }
    }
}

impl EngineConfigSummary {
    /// Returns best-guess Wasmtime defaults for when Engine is created with
    /// a raw `wasmtime::Config` (no EngineBuilder). Wasmtime's Config doesn't
    /// expose its settings for introspection, so these are hardcoded.
    pub(crate) fn default_assumptions() -> Self {
        EngineConfigSummary {
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
            wasm_memory64: true,
            wasm_extended_const: true,
            wasm_component_model: true,
            wasm_custom_page_sizes: false,
            wasm_wide_arithmetic: false,
            wasm_stack_switching: false,
            wasm_shared_everything_threads: false,
            wasm_component_model_async: false,
            wasm_component_model_async_builtins: false,
            wasm_component_model_async_stackful: false,
            wasm_component_model_error_context: false,
            wasm_component_model_gc: false,
            wasm_component_model_threading: false,
            fuel_enabled: false, // Wasmtime default: consume_fuel is off
            epoch_interruption: false,
            max_stack_size: None,
            async_support: false,
            coredump_on_trap: false,
            memory_reservation: None,
            memory_guard_size: None,
            memory_reservation_for_growth: None,
            wmemcheck_enabled: false,
            table_lazy_init: true,
        }
    }

    pub(crate) fn from_builder(builder: &EngineBuilder) -> Self {
        EngineConfigSummary {
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
            wasm_component_model_threading: builder.wasm_component_model_threading,
            fuel_enabled: builder.fuel_enabled,
            epoch_interruption: builder.epoch_interruption,
            max_stack_size: builder.max_stack_size,
            async_support: builder.async_support,
            coredump_on_trap: builder.coredump_on_trap,
            memory_reservation: builder.memory_reservation,
            memory_guard_size: builder.memory_guard_size,
            memory_reservation_for_growth: builder.memory_reservation_for_growth,
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: builder.wmemcheck_enabled,
            #[cfg(not(feature = "wmemcheck"))]
            wmemcheck_enabled: false,
            table_lazy_init: builder.table_lazy_init,
        }
    }
}

/// Builder for creating configured engines
pub struct EngineBuilder {
    pub(crate) config: Config,
    pub(crate) strategy: Option<Strategy>,
    pub(crate) opt_level: Option<OptLevel>,
    pub(crate) debug_info: bool,
    pub(crate) fuel_enabled: bool,
    pub(crate) max_stack_size: Option<usize>,
    pub(crate) epoch_interruption: bool,
    // Track wasm features separately for proper config summary
    pub(crate) wasm_threads: bool,
    pub(crate) wasm_reference_types: bool,
    pub(crate) wasm_simd: bool,
    pub(crate) wasm_bulk_memory: bool,
    pub(crate) wasm_multi_value: bool,
    pub(crate) wasm_multi_memory: bool,
    pub(crate) wasm_tail_call: bool,
    pub(crate) wasm_relaxed_simd: bool,
    pub(crate) wasm_function_references: bool,
    pub(crate) wasm_gc: bool,
    pub(crate) wasm_exceptions: bool,
    pub(crate) wasm_memory64: bool,
    pub(crate) wasm_extended_const: bool,
    pub(crate) wasm_component_model: bool,
    pub(crate) wasm_custom_page_sizes: bool,
    pub(crate) wasm_wide_arithmetic: bool,
    pub(crate) wasm_stack_switching: bool,
    pub(crate) wasm_shared_everything_threads: bool,
    pub(crate) wasm_component_model_async: bool,
    pub(crate) wasm_component_model_async_builtins: bool,
    pub(crate) wasm_component_model_async_stackful: bool,
    pub(crate) wasm_component_model_error_context: bool,
    pub(crate) wasm_component_model_gc: bool,
    pub(crate) async_support: bool,
    pub(crate) coredump_on_trap: bool,
    // Memory configuration options
    pub(crate) memory_reservation: Option<u64>,
    pub(crate) memory_guard_size: Option<u64>,
    pub(crate) memory_reservation_for_growth: Option<u64>,
    // Cranelift configuration options
    pub(crate) cranelift_debug_verifier: bool,
    pub(crate) cranelift_nan_canonicalization: bool,
    pub(crate) cranelift_pcc: bool,
    pub(crate) cranelift_regalloc_algorithm: Option<RegallocAlgorithm>,
    // wmemcheck - only available when compiled with wmemcheck feature
    #[cfg(feature = "wmemcheck")]
    pub(crate) wmemcheck_enabled: bool,
    // Table lazy initialization - enabled by default for faster instantiation
    pub(crate) table_lazy_init: bool,
    // GC support infrastructure - required for GC, function references, exceptions
    pub(crate) gc_support: bool,
    // GC collector implementation choice
    pub(crate) collector: Option<wasmtime::Collector>,
    // Memory may relocate at runtime
    pub(crate) memory_may_move: bool,
    // Guard regions before linear memory
    pub(crate) guard_before_linear_memory: bool,
    // Copy-on-write memory initialization
    pub(crate) memory_init_cow: bool,
    // Component model threading support (experimental)
    pub(crate) wasm_component_model_threading: bool,
    // Deterministic relaxed SIMD behavior
    pub(crate) relaxed_simd_deterministic: bool,
    // Zero async stacks before reuse
    pub(crate) async_stack_zeroing: bool,
    // Async stack size
    pub(crate) async_stack_size: Option<usize>,
    // Multi-threaded compilation
    pub(crate) parallel_compilation: bool,
    // Use Mach ports on macOS
    pub(crate) macos_use_mach_ports: bool,
    // WebAssembly backtrace collection
    pub(crate) wasm_backtrace: bool,
    // Address map generation for debugging
    pub(crate) generate_address_map: bool,
    // Shared memory support (independent of wasm threads)
    pub(crate) shared_memory: bool,
    // Module version strategy
    pub(crate) module_version_strategy: Option<wasmtime::ModuleVersionStrategy>,
    // Instance allocation strategy
    pub(crate) allocation_strategy: Option<wasmtime::InstanceAllocationStrategy>,
    // Profiling strategy
    pub(crate) profiling_strategy: wasmtime::ProfilingStrategy,
    // Native unwind info
    pub(crate) native_unwind_info: bool,
    // Cranelift compiler inlining
    pub(crate) compiler_inlining: bool,
    // Debug adapter modules support
    pub(crate) debug_adapter_modules: bool,
    // Force memfd for memory initialization (Linux optimization)
    pub(crate) force_memory_init_memfd: bool,
    // Cranelift Wasmtime debug checks
    pub(crate) cranelift_debug_checks: bool,
    // Enable or disable the compiler (allows runtime-only engines)
    pub(crate) enable_compiler: bool,
}

impl EngineBuilder {
    /// Create new engine builder with safe defaults
    pub(crate) fn new() -> Self {
        let mut config = safe_wasmtime_config();

        // Set production-optimized defaults
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);

        // Enable commonly used WebAssembly features
        config.wasm_reference_types(true);
        config.wasm_bulk_memory(true);
        config.wasm_multi_value(true);
        config.wasm_simd(true);
        config.wasm_function_references(true); // Tier 2 - enable for ref.func support

        // Enable Component Model support (Tier 1 feature)
        config.wasm_component_model(true);

        // Configure stack size - increase from default 512 KiB to 2 MiB for JNI safety
        config.max_wasm_stack(2 * 1024 * 1024);

        // Enable debug info for better backtraces during development
        config.debug_info(true);

        // Enable WASM backtraces for better error diagnostics
        config.wasm_backtrace(true);
        config.wasm_backtrace_details(wasmtime::WasmBacktraceDetails::Enable);

        // Note: Fuel consumption is opt-in via StoreBuilder.fuel_limit()
        // config.consume_fuel(true);

        EngineBuilder {
            config,
            strategy: Some(Strategy::Cranelift),
            opt_level: Some(OptLevel::Speed),
            debug_info: true,
            fuel_enabled: false,
            max_stack_size: None,
            epoch_interruption: false,
            wasm_threads: true,
            wasm_reference_types: true,
            wasm_simd: true,
            wasm_bulk_memory: true,
            wasm_multi_value: true,
            wasm_multi_memory: false,
            wasm_tail_call: false,
            wasm_relaxed_simd: false,
            wasm_function_references: true, // Tier 2 - enable for ref.func support
            wasm_gc: false,
            wasm_exceptions: false,
            wasm_memory64: true,                        // Tier 1 - on by default
            wasm_extended_const: true,                  // Tier 1 - on by default
            wasm_component_model: true,                 // Tier 1 - on by default
            wasm_custom_page_sizes: false,              // Tier 3 - off by default
            wasm_wide_arithmetic: false,                // Tier 3 - off by default
            wasm_stack_switching: false,                // Tier 3 - off by default
            wasm_shared_everything_threads: false,      // Tier 3 - off by default
            wasm_component_model_async: false, // Component model extension - off by default
            wasm_component_model_async_builtins: false, // Component model extension - off by default
            wasm_component_model_async_stackful: false, // Component model extension - off by default
            wasm_component_model_error_context: false, // Component model extension - off by default
            wasm_component_model_gc: false,            // Component model extension - off by default
            async_support: false,                      // Async execution support - off by default
            coredump_on_trap: false,                   // Coredump on trap - off by default
            memory_reservation: None,                  // Memory reservation - use Wasmtime default
            memory_guard_size: None,                   // Memory guard size - use Wasmtime default
            memory_reservation_for_growth: None, // Memory reservation for growth - use Wasmtime default
            cranelift_debug_verifier: false,     // Cranelift debug verifier - off by default
            cranelift_nan_canonicalization: false, // Cranelift NaN canonicalization - off by default
            cranelift_pcc: false,                  // Cranelift proof-carrying code - off by default
            cranelift_regalloc_algorithm: None,    // Cranelift regalloc algorithm - use default
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: false, // wmemcheck - off by default
            table_lazy_init: true, // Table lazy init - on by default (wasmtime default)
            // New config options with wasmtime defaults
            gc_support: true, // GC support - on by default (enables GC infrastructure)
            collector: None,  // Collector - use wasmtime default (Auto)
            memory_may_move: true, // Memory may move - on by default
            guard_before_linear_memory: true, // Guard before memory - on by default
            memory_init_cow: true, // CoW memory init - on by default
            wasm_component_model_threading: false, // Component threading - off (experimental)
            relaxed_simd_deterministic: false, // Relaxed SIMD deterministic - off by default
            async_stack_zeroing: false, // Async stack zeroing - off by default
            async_stack_size: None, // Async stack size - use wasmtime default
            parallel_compilation: true, // Parallel compilation - on by default
            macos_use_mach_ports: true, // Mach ports on macOS - on by default
            wasm_backtrace: true, // Backtrace collection - on by default
            generate_address_map: true, // Address map generation - on by default
            shared_memory: false, // Shared memory - off by default
            module_version_strategy: None, // Module version - use wasmtime default
            allocation_strategy: None, // Allocation strategy - use wasmtime default (OnDemand)
            profiling_strategy: wasmtime::ProfilingStrategy::None, // No profiling by default
            native_unwind_info: true, // Native unwind info - on by default (wasmtime default)
            compiler_inlining: true, // Compiler inlining - on by default (wasmtime default)
            debug_adapter_modules: false, // Debug adapter modules - off by default
            force_memory_init_memfd: false, // Force memfd - off by default (Linux-specific)
            cranelift_debug_checks: false, // Cranelift debug checks - off by default
            enable_compiler: true, // Compiler enabled - on by default
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
        self.config.consume_fuel(enable);
        self.fuel_enabled = enable;
        self
    }

    /// Set maximum stack size in bytes
    pub fn max_stack_size(mut self, size: usize) -> Self {
        self.config.max_wasm_stack(size);
        self.max_stack_size = Some(size);
        self
    }

    /// Enable or disable epoch-based interruption
    pub fn epoch_interruption(mut self, enable: bool) -> Self {
        self.config.epoch_interruption(enable);
        self.epoch_interruption = enable;
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

    /// Set an arbitrary Cranelift compiler flag
    ///
    /// This allows setting any Cranelift flag by name and value string.
    /// For example: `cranelift_flag_set("opt_level", "speed_and_size")`.
    ///
    /// # Arguments
    /// * `name` - The flag name
    /// * `value` - The flag value
    pub fn cranelift_flag_set(mut self, name: &str, value: &str) -> Self {
        // Safety: name and value are valid string references
        unsafe {
            self.config.cranelift_flag_set(name, value);
        }
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

    /// Configure WebAssembly backtrace collection on traps
    ///
    /// When enabled, Wasmtime collects stack trace information when a trap occurs.
    ///
    /// # Arguments
    /// * `enable` - Whether to collect backtraces (default: true)
    pub fn wasm_backtrace(mut self, enable: bool) -> Self {
        self.config.wasm_backtrace(enable);
        self.wasm_backtrace = enable;
        self
    }

    /// Configure the level of detail for WebAssembly backtrace information
    ///
    /// This controls whether symbolic names are included in backtraces.
    ///
    /// # Arguments
    /// * `details` - The backtrace detail level
    pub fn wasm_backtrace_details(mut self, details: wasmtime::WasmBacktraceDetails) -> Self {
        self.config.wasm_backtrace_details(details);
        self
    }

    /// Configure address map generation for compiled code
    ///
    /// Address maps provide mapping from native code addresses back to WebAssembly
    /// bytecode offsets, useful for debugging and profiling.
    ///
    /// # Arguments
    /// * `enable` - Whether to generate address maps (default: true)
    pub fn generate_address_map(mut self, enable: bool) -> Self {
        self.config.generate_address_map(enable);
        self.generate_address_map = enable;
        self
    }

    /// Configure shared memory support (independent of wasm threads)
    ///
    /// When enabled, the `shared` attribute is allowed on memory definitions,
    /// enabling atomic operations on shared memory.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable shared memory (default: false)
    pub fn shared_memory(mut self, enable: bool) -> Self {
        self.config.shared_memory(enable);
        self.shared_memory = enable;
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

    /// Set the profiling strategy for the engine.
    ///
    /// This configures how profiling information is collected during
    /// WebAssembly execution.
    ///
    /// # Arguments
    /// * `strategy` - The profiling strategy to use
    pub fn profiling_strategy(mut self, strategy: wasmtime::ProfilingStrategy) -> Self {
        self.config.profiler(strategy.clone());
        self.profiling_strategy = strategy;
        self
    }

    /// Enable or disable native unwind information.
    ///
    /// This controls whether the engine generates native unwind tables,
    /// which are needed for proper stack unwinding on some platforms.
    ///
    /// # Arguments
    /// * `enable` - Whether to generate native unwind info
    pub fn native_unwind_info(mut self, enable: bool) -> Self {
        self.config.native_unwind_info(enable);
        self.native_unwind_info = enable;
        self
    }

    /// Enable or disable Cranelift function inlining
    ///
    /// When enabled (the default), Cranelift may inline functions during compilation
    /// for better runtime performance.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable compiler inlining
    pub fn compiler_inlining(mut self, enable: bool) -> Self {
        self.config.compiler_inlining(enable);
        self.compiler_inlining = enable;
        self
    }

    /// Enable or disable debug adapter modules
    ///
    /// When enabled, debug adapter modules are allowed for DAP-based debugging.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable debug adapter modules
    pub fn debug_adapter_modules(mut self, enable: bool) -> Self {
        self.config.debug_adapter_modules(enable);
        self.debug_adapter_modules = enable;
        self
    }

    /// Enable or disable forcing memfd for memory initialization (Linux)
    ///
    /// When enabled, Wasmtime will always use memfd for memory initialization,
    /// even when copy-on-write initialization might not otherwise use it.
    ///
    /// # Arguments
    /// * `enable` - Whether to force memfd memory initialization
    pub fn force_memory_init_memfd(mut self, enable: bool) -> Self {
        self.config.force_memory_init_memfd(enable);
        self.force_memory_init_memfd = enable;
        self
    }

    /// Enable or disable Cranelift Wasmtime-specific debug checks
    ///
    /// When enabled, additional runtime assertions are inserted into compiled code
    /// to verify Wasmtime invariants. Primarily for Wasmtime development.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable Cranelift debug checks
    pub fn cranelift_debug_checks(mut self, enable: bool) -> Self {
        self.config.cranelift_wasmtime_debug_checks(enable);
        self.cranelift_debug_checks = enable;
        self
    }

    /// Enable or disable the compiler
    ///
    /// When disabled, the engine can only execute pre-compiled modules and cannot
    /// compile new WebAssembly modules. This is useful for runtime-only deployments
    /// where compilation is done ahead of time.
    ///
    /// # Arguments
    /// * `enable` - Whether to enable the compiler (default: true)
    pub fn enable_compiler(mut self, enable: bool) -> Self {
        self.config.enable_compiler(enable);
        self.enable_compiler = enable;
        self
    }

    /// Set the compilation target triple for cross-compilation
    ///
    /// This configures the target architecture for code generation, allowing
    /// cross-compilation of WebAssembly modules for different platforms.
    ///
    /// # Arguments
    /// * `triple` - The target triple string (e.g., "x86_64-unknown-linux-gnu")
    pub fn target(mut self, triple: &str) -> Self {
        let _ = self.config.target(triple);
        self
    }

    /// Set the guaranteed dense image size for linear memories
    ///
    /// This configures the size of the dense image used for memory initialization.
    /// Modules with data segments that fit within this size will use a dense
    /// memory initialization strategy, which is faster.
    ///
    /// # Arguments
    /// * `size` - The guaranteed dense image size in bytes
    pub fn memory_guaranteed_dense_image_size(mut self, size: u64) -> Self {
        self.config.memory_guaranteed_dense_image_size(size);
        self
    }

    /// Load default cache configuration
    ///
    /// Enables compilation caching using the default cache directory. This
    /// avoids recompiling modules that have already been compiled by a
    /// previous engine instance, significantly improving startup time.
    ///
    /// The default cache location is platform-specific and managed by Wasmtime.
    pub fn cache_config_load_default(mut self) -> WasmtimeResult<Self> {
        let cache =
            wasmtime::Cache::from_file(None).map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to load default cache config: {}", e),
            })?;
        self.config.cache(Some(cache));
        Ok(self)
    }

    /// Load cache configuration from a file
    ///
    /// Enables compilation caching using the specified configuration file.
    /// The configuration file specifies the cache directory, eviction policy,
    /// and other caching parameters.
    ///
    /// # Arguments
    /// * `path` - Path to the cache configuration TOML file
    pub fn cache_config_load(mut self, path: &str) -> WasmtimeResult<Self> {
        let cache = wasmtime::Cache::from_file(Some(std::path::Path::new(path))).map_err(
            |e| WasmtimeError::EngineConfig {
                message: format!("Failed to load cache config from '{}': {}", path, e),
            },
        )?;
        self.config.cache(Some(cache));
        Ok(self)
    }

    /// Build engine with current configuration
    pub fn build(self) -> WasmtimeResult<Engine> {
        let summary = EngineConfigSummary::from_builder(&self);

        let engine =
            WasmtimeEngine::new(&self.config).map_err(|e| WasmtimeError::EngineConfig {
                message: format!("Failed to create Wasmtime engine: {}", e),
            })?;

        Ok(Engine {
            inner: Arc::new(engine),
            config_summary: summary,
            concurrent_ops_lock: Arc::new(RwLock::new(())),
            is_closed: Arc::new(AtomicBool::new(false)),
        })
    }
}
