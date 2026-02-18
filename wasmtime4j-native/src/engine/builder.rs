//! Engine builder for creating configured engines
//!
//! This module provides the EngineBuilder and EngineConfigSummary types
//! for creating and configuring Wasmtime engines with various options.

use std::sync::atomic::AtomicBool;
use std::sync::{Arc, RwLock};

use wasmtime::{Config, Engine as WasmtimeEngine, OptLevel, RegallocAlgorithm, Strategy};

use super::{safe_wasmtime_config, Engine};
use crate::error::{WasmtimeError, WasmtimeResult};

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

impl Default for EngineConfigSummary {
    fn default() -> Self {
        Self {
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
        }
    }
}

impl EngineConfigSummary {
    pub(crate) fn from_config(_config: &Config) -> Self {
        // Note: Wasmtime Config doesn't expose all settings for introspection
        // We track what we can and make reasonable assumptions
        EngineConfigSummary {
            strategy: "Cranelift".to_string(),          // Default assumption
            opt_level: "Speed".to_string(),             // Default assumption
            debug_info: false,                          // Default assumption
            wasm_threads: true,                         // Common default
            wasm_reference_types: true,                 // Commonly enabled
            wasm_simd: true,                            // Commonly enabled
            wasm_bulk_memory: true,                     // Commonly enabled
            wasm_multi_value: true,                     // Commonly enabled
            wasm_multi_memory: false,                   // Tier 3 feature - off by default
            wasm_tail_call: false,                      // Tier 3 feature - off by default
            wasm_relaxed_simd: false,                   // Tier 3 feature - off by default
            wasm_function_references: false,            // Tier 3 feature - off by default
            wasm_gc: false,                             // Tier 3 feature - off by default
            wasm_exceptions: false,                     // Tier 3 feature - off by default
            wasm_memory64: true,                        // Tier 1 feature - on by default
            wasm_extended_const: true,                  // Tier 1 feature - on by default
            wasm_component_model: true,                 // Tier 1 feature - on by default
            wasm_custom_page_sizes: false,              // Tier 3 feature - off by default
            wasm_wide_arithmetic: false,                // Tier 3 feature - off by default
            wasm_stack_switching: false,                // Tier 3 feature - off by default
            wasm_shared_everything_threads: false,      // Tier 3 feature - off by default
            wasm_component_model_async: false, // Component model extension - off by default
            wasm_component_model_async_builtins: false, // Component model extension - off by default
            wasm_component_model_async_stackful: false, // Component model extension - off by default
            wasm_component_model_error_context: false, // Component model extension - off by default
            wasm_component_model_gc: false,            // Component model extension - off by default
            fuel_enabled: true, // Default assumption (enabled for Store operations)
            max_memory_pages: None, // No limit by default
            max_stack_size: None, // No limit by default
            epoch_interruption: false, // Default assumption
            max_instances: None, // No limit by default
            async_support: false, // Off by default
            coredump_on_trap: false, // Off by default
            memory_reservation: None, // No custom reservation by default
            memory_guard_size: None, // No custom guard size by default
            memory_reservation_for_growth: None, // No custom growth reservation by default
            max_memory_size: None, // No custom max memory by default
            cranelift_debug_verifier: false, // Off by default
            cranelift_nan_canonicalization: false, // Off by default
            cranelift_pcc: false, // Off by default
            cranelift_regalloc_algorithm: "Backtracking".to_string(), // Default algorithm
            wmemcheck_enabled: false, // Off by default (requires wmemcheck feature)
            table_lazy_init: true, // On by default (wasmtime default)
            // New config options with wasmtime defaults
            gc_support: true,                      // GC support - on by default
            collector: "Auto".to_string(),         // Collector - Auto by default
            memory_may_move: true,                 // Memory may move - on by default
            guard_before_linear_memory: true,      // Guard before memory - on by default
            memory_init_cow: true,                 // CoW memory init - on by default
            wasm_component_model_threading: false, // Component threading - off (experimental)
            relaxed_simd_deterministic: false,     // Relaxed SIMD deterministic - off by default
            async_stack_zeroing: false,            // Async stack zeroing - off by default
            async_stack_size: None,                // Async stack size - use default
            parallel_compilation: true,            // Parallel compilation - on by default
            macos_use_mach_ports: true,            // Mach ports on macOS - on by default
            module_version_strategy: "WasmtimeVersion".to_string(), // Use wasmtime version
            allocation_strategy: "OnDemand".to_string(), // On-demand allocation by default
        }
    }

    pub(crate) fn from_builder(builder: &EngineBuilder) -> Self {
        EngineConfigSummary {
            strategy: builder
                .strategy
                .as_ref()
                .map(|s| match s {
                    Strategy::Cranelift => "Cranelift".to_string(),
                    _ => "Auto".to_string(),
                })
                .unwrap_or_else(|| "Auto".to_string()),
            opt_level: builder
                .opt_level
                .as_ref()
                .map(|o| match o {
                    OptLevel::None => "None".to_string(),
                    OptLevel::Speed => "Speed".to_string(),
                    OptLevel::SpeedAndSize => "SpeedAndSize".to_string(),
                    _ => "Auto".to_string(),
                })
                .unwrap_or_else(|| "Auto".to_string()),
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
            cranelift_regalloc_algorithm: builder
                .cranelift_regalloc_algorithm
                .as_ref()
                .map(|a| match a {
                    RegallocAlgorithm::Backtracking => "Backtracking".to_string(),
                    RegallocAlgorithm::SinglePass => "SinglePass".to_string(),
                    _ => "Unknown".to_string(),
                })
                .unwrap_or_else(|| "Backtracking".to_string()),
            #[cfg(feature = "wmemcheck")]
            wmemcheck_enabled: builder.wmemcheck_enabled,
            #[cfg(not(feature = "wmemcheck"))]
            wmemcheck_enabled: false,
            table_lazy_init: builder.table_lazy_init,
            // New config options
            gc_support: builder.gc_support,
            collector: builder
                .collector
                .as_ref()
                .map(|c| match c {
                    wasmtime::Collector::Auto => "Auto".to_string(),
                    wasmtime::Collector::DeferredReferenceCounting => {
                        "DeferredReferenceCounting".to_string()
                    }
                    wasmtime::Collector::Null => "Null".to_string(),
                    _ => "Unknown".to_string(),
                })
                .unwrap_or_else(|| "Auto".to_string()),
            memory_may_move: builder.memory_may_move,
            guard_before_linear_memory: builder.guard_before_linear_memory,
            memory_init_cow: builder.memory_init_cow,
            wasm_component_model_threading: builder.wasm_component_model_threading,
            relaxed_simd_deterministic: builder.relaxed_simd_deterministic,
            async_stack_zeroing: builder.async_stack_zeroing,
            async_stack_size: builder.async_stack_size,
            parallel_compilation: builder.parallel_compilation,
            macos_use_mach_ports: builder.macos_use_mach_ports,
            module_version_strategy: builder
                .module_version_strategy
                .as_ref()
                .map(|s| match s {
                    wasmtime::ModuleVersionStrategy::WasmtimeVersion => {
                        "WasmtimeVersion".to_string()
                    }
                    wasmtime::ModuleVersionStrategy::None => "None".to_string(),
                    wasmtime::ModuleVersionStrategy::Custom(_) => "Custom".to_string(),
                })
                .unwrap_or_else(|| "WasmtimeVersion".to_string()),
            allocation_strategy: builder
                .allocation_strategy
                .as_ref()
                .map(|s| match s {
                    wasmtime::InstanceAllocationStrategy::OnDemand => "OnDemand".to_string(),
                    wasmtime::InstanceAllocationStrategy::Pooling(_) => "Pooling".to_string(),
                    _ => "Unknown".to_string(),
                })
                .unwrap_or_else(|| "OnDemand".to_string()),
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
    pub(crate) max_memory_pages: Option<u32>,
    pub(crate) max_stack_size: Option<usize>,
    pub(crate) epoch_interruption: bool,
    pub(crate) max_instances: Option<u32>,
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
    pub(crate) max_memory_size: Option<usize>,
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
    // Module version strategy
    pub(crate) module_version_strategy: Option<wasmtime::ModuleVersionStrategy>,
    // Instance allocation strategy
    pub(crate) allocation_strategy: Option<wasmtime::InstanceAllocationStrategy>,
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
            max_memory_size: None,               // Max memory size - use Wasmtime default
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
            module_version_strategy: None, // Module version - use wasmtime default
            allocation_strategy: None, // Allocation strategy - use wasmtime default (OnDemand)
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
