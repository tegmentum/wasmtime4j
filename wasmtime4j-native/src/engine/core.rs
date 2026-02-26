//! Shared core functions for engine operations
//!
//! These functions eliminate code duplication and provide consistent behavior
//! across JNI and Panama interface implementations while maintaining defensive
//! programming practices.

use std::os::raw::c_void;

use serde::Deserialize;
use wasmtime::{Engine as WasmtimeEngine, MemoryType, OptLevel, RegallocAlgorithm, SharedMemory as WasmtimeSharedMemory, Strategy};

use super::{Engine, EngineBuilder, EngineConfigSummary, WasmFeature};
use crate::error::{ffi_utils, WasmtimeError, WasmtimeResult};
use crate::memory::Memory;
use crate::memory::core::{ValidatedMemory, create_validated_memory};
use crate::validate_ptr_not_null;

/// JSON-deserializable engine configuration for FFI
///
/// This struct provides a clean alternative to the positional-parameter
/// `create_engine_with_extended_config()`. Java serializes `EngineConfig`
/// to JSON, passes it as `(byte*, len)` through FFI, and Rust deserializes
/// it here. All fields are optional; omitted fields use builder defaults.
#[derive(Debug, Deserialize, Default)]
#[serde(rename_all = "camelCase")]
pub struct EngineConfigFfi {
    // Compilation strategy: "cranelift", "winch", "auto"
    pub strategy: Option<String>,
    // Optimization level: "none", "speed", "speed_and_size"
    pub opt_level: Option<String>,

    // Core settings
    pub debug_info: Option<bool>,
    pub fuel_enabled: Option<bool>,
    pub epoch_interruption: Option<bool>,
    pub async_support: Option<bool>,
    pub concurrency_support: Option<bool>,
    pub coredump_on_trap: Option<bool>,
    pub parallel_compilation: Option<bool>,
    pub native_unwind_info: Option<bool>,

    // Stack configuration
    pub max_stack_size: Option<usize>,
    pub async_stack_size: Option<usize>,
    pub async_stack_zeroing: Option<bool>,

    // Memory configuration
    pub memory_reservation: Option<u64>,
    pub memory_guard_size: Option<u64>,
    pub memory_reservation_for_growth: Option<u64>,
    pub memory_may_move: Option<bool>,
    pub guard_before_linear_memory: Option<bool>,
    pub memory_init_cow: Option<bool>,

    // WASM feature flags
    pub wasm_threads: Option<bool>,
    pub wasm_reference_types: Option<bool>,
    pub wasm_simd: Option<bool>,
    pub wasm_bulk_memory: Option<bool>,
    pub wasm_multi_value: Option<bool>,
    pub wasm_multi_memory: Option<bool>,
    pub wasm_tail_call: Option<bool>,
    pub wasm_relaxed_simd: Option<bool>,
    pub wasm_function_references: Option<bool>,
    pub wasm_gc: Option<bool>,
    pub wasm_exceptions: Option<bool>,
    pub wasm_memory64: Option<bool>,
    pub wasm_extended_const: Option<bool>,
    pub wasm_component_model: Option<bool>,
    pub wasm_custom_page_sizes: Option<bool>,
    pub wasm_wide_arithmetic: Option<bool>,
    pub wasm_stack_switching: Option<bool>,
    pub wasm_shared_everything_threads: Option<bool>,

    // Component model extensions
    pub wasm_component_model_async: Option<bool>,
    pub wasm_component_model_async_builtins: Option<bool>,
    pub wasm_component_model_async_stackful: Option<bool>,
    pub wasm_component_model_error_context: Option<bool>,
    pub wasm_component_model_gc: Option<bool>,
    pub wasm_component_model_threading: Option<bool>,
    pub wasm_component_model_fixed_length_lists: Option<bool>,

    // Features settable only via WasmFeatures bitflags (no individual Config method)
    pub wasm_mutable_global: Option<bool>,
    pub wasm_saturating_float_to_int: Option<bool>,
    pub wasm_sign_extension: Option<bool>,
    pub wasm_floats: Option<bool>,
    pub wasm_memory_control: Option<bool>,
    pub wasm_legacy_exceptions: Option<bool>,
    pub wasm_gc_types: Option<bool>,
    pub wasm_component_model_values: Option<bool>,
    pub wasm_component_model_nested_names: Option<bool>,
    pub wasm_component_model_map: Option<bool>,
    pub wasm_call_indirect_overlong: Option<bool>,
    pub wasm_bulk_memory_opt: Option<bool>,
    pub wasm_custom_descriptors: Option<bool>,
    pub wasm_compact_imports: Option<bool>,

    // Cranelift settings
    pub cranelift_debug_verifier: Option<bool>,
    pub cranelift_nan_canonicalization: Option<bool>,
    pub cranelift_pcc: Option<bool>,
    // "backtracking" or "single_pass"
    pub cranelift_regalloc_algorithm: Option<String>,
    // Key-value pairs for arbitrary cranelift flags
    pub cranelift_flags: Option<Vec<CraneliftFlag>>,

    // GC settings: "auto", "yes", "no" (tri-state Enabled)
    pub gc_support: Option<String>,
    // "auto", "deferred_reference_counting", "null"
    pub collector: Option<String>,

    // Table settings
    pub table_lazy_init: Option<bool>,

    // SIMD determinism
    pub relaxed_simd_deterministic: Option<bool>,

    // Allocation strategy: "on_demand", "pooling"
    pub allocation_strategy: Option<String>,

    // Profiling: "none", "jitdump", "vtune", "perfmap"
    pub profiling_strategy: Option<String>,

    // Module version strategy: "wasmtime_version", "none", or custom string
    pub module_version_strategy: Option<String>,
    pub module_version_custom: Option<String>,

    // macOS-specific
    #[cfg(target_os = "macos")]
    pub macos_use_mach_ports: Option<bool>,

    // Backtrace and debugging
    pub wasm_backtrace: Option<bool>,
    // "enable", "disable", "environment"
    pub backtrace_details: Option<String>,
    pub generate_address_map: Option<bool>,

    // Shared memory (independent of wasm threads): "auto", "yes", "no" (tri-state Enabled)
    pub shared_memory: Option<String>,

    // Target triple for cross-compilation
    pub target: Option<String>,

    // Memory guaranteed dense image size
    pub memory_guaranteed_dense_image_size: Option<u64>,

    // Cache configuration
    pub cache_enabled: Option<bool>,
    pub cache_config_path: Option<String>,

    // Wasmtime 41.0.3 config additions
    pub compiler_inlining: Option<bool>,
    pub debug_adapter_modules: Option<bool>,
    pub wmemcheck: Option<bool>,
    pub force_memory_init_memfd: Option<bool>,
    pub cranelift_debug_checks: Option<bool>,
    pub enable_compiler: Option<bool>,

    // Guest debugging instrumentation
    pub guest_debug: Option<bool>,

    // Cranelift IR output directory
    pub emit_clif: Option<String>,

    // Acknowledge x86 float ABI behavior
    pub x86_float_abi_ok: Option<bool>,

    // Signals-based trap handling (always overridden to false for JVM safety)
    pub signals_based_traps: Option<bool>,

    // Record/replay configuration: "none", "recording", "replaying"
    #[cfg(feature = "rr")]
    pub rr_config: Option<String>,

    // Cranelift flag enables (single-flag variant, sets flag to "true")
    pub cranelift_flag_enables: Option<Vec<String>>,
}

/// Key-value pair for Cranelift compiler flags
#[derive(Debug, Deserialize)]
pub struct CraneliftFlag {
    pub name: String,
    pub value: String,
}

/// Create an engine from JSON configuration bytes
///
/// This is the preferred entry point for creating engines from Java.
/// The JSON is deserialized into `EngineConfigFfi` and mapped to `EngineBuilder` calls.
pub fn create_engine_from_json_config(json_bytes: &[u8]) -> WasmtimeResult<Box<Engine>> {
    let config: EngineConfigFfi =
        serde_json::from_slice(json_bytes).map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to parse engine config JSON: {}", e),
        })?;

    build_engine_from_config(config)?.build().map(Box::new)
}

/// Build an EngineBuilder from a deserialized config, applying all settings.
///
/// This is the shared config-application logic used by both `create_engine_from_json_config`
/// and `create_engine_from_json_config_with_cache_store`.
fn build_engine_from_config(config: EngineConfigFfi) -> WasmtimeResult<EngineBuilder> {
    let mut builder = Engine::builder();

    // Compilation strategy
    if let Some(ref strategy) = config.strategy {
        builder = builder.strategy(match strategy.as_str() {
            "cranelift" => Strategy::Cranelift,
            "winch" => Strategy::Winch,
            _ => Strategy::Auto,
        });
    }

    // Optimization level
    if let Some(ref opt) = config.opt_level {
        builder = builder.opt_level(match opt.as_str() {
            "none" => OptLevel::None,
            "speed" => OptLevel::Speed,
            "speed_and_size" => OptLevel::SpeedAndSize,
            _ => OptLevel::Speed,
        });
    }

    // Core settings
    if let Some(v) = config.debug_info { builder = builder.debug_info(v); }
    if let Some(v) = config.fuel_enabled { builder = builder.fuel_enabled(v); }
    if let Some(v) = config.epoch_interruption { builder = builder.epoch_interruption(v); }
    if let Some(v) = config.async_support { builder = builder.async_support(v); }
    if let Some(v) = config.concurrency_support { builder = builder.concurrency_support(v); }
    if let Some(v) = config.coredump_on_trap { builder = builder.coredump_on_trap(v); }
    if let Some(v) = config.parallel_compilation { builder = builder.parallel_compilation(v); }
    if let Some(v) = config.native_unwind_info { builder = builder.native_unwind_info(v); }

    // Stack
    if let Some(v) = config.max_stack_size { builder = builder.max_stack_size(v); }
    if let Some(v) = config.async_stack_size { builder = builder.async_stack_size(v); }
    if let Some(v) = config.async_stack_zeroing { builder = builder.async_stack_zeroing(v); }

    // Memory
    if let Some(v) = config.memory_reservation { builder = builder.memory_reservation(v); }
    if let Some(v) = config.memory_guard_size { builder = builder.memory_guard_size(v); }
    if let Some(v) = config.memory_reservation_for_growth {
        builder = builder.memory_reservation_for_growth(v);
    }
    if let Some(v) = config.memory_may_move { builder = builder.memory_may_move(v); }
    if let Some(v) = config.guard_before_linear_memory {
        builder = builder.guard_before_linear_memory(v);
    }
    if let Some(v) = config.memory_init_cow { builder = builder.memory_init_cow(v); }

    // WASM features
    if let Some(v) = config.wasm_threads { builder = builder.wasm_threads(v); }
    if let Some(v) = config.wasm_reference_types { builder = builder.wasm_reference_types(v); }
    if let Some(v) = config.wasm_simd { builder = builder.wasm_simd(v); }
    if let Some(v) = config.wasm_bulk_memory { builder = builder.wasm_bulk_memory(v); }
    if let Some(v) = config.wasm_multi_value { builder = builder.wasm_multi_value(v); }
    if let Some(v) = config.wasm_multi_memory { builder = builder.wasm_multi_memory(v); }
    if let Some(v) = config.wasm_tail_call { builder = builder.wasm_tail_call(v); }
    if let Some(v) = config.wasm_relaxed_simd { builder = builder.wasm_relaxed_simd(v); }
    if let Some(v) = config.wasm_function_references {
        builder = builder.wasm_function_references(v);
    }
    if let Some(v) = config.wasm_gc { builder = builder.wasm_gc(v); }
    if let Some(v) = config.wasm_exceptions { builder = builder.wasm_exceptions(v); }
    if let Some(v) = config.wasm_memory64 { builder = builder.wasm_memory64(v); }
    if let Some(v) = config.wasm_extended_const { builder = builder.wasm_extended_const(v); }
    if let Some(v) = config.wasm_component_model { builder = builder.wasm_component_model(v); }
    if let Some(v) = config.wasm_custom_page_sizes { builder = builder.wasm_custom_page_sizes(v); }
    if let Some(v) = config.wasm_wide_arithmetic { builder = builder.wasm_wide_arithmetic(v); }
    if let Some(v) = config.wasm_stack_switching { builder = builder.wasm_stack_switching(v); }
    if let Some(v) = config.wasm_shared_everything_threads {
        builder = builder.wasm_shared_everything_threads(v);
    }

    // Component model extensions
    if let Some(v) = config.wasm_component_model_async {
        builder = builder.wasm_component_model_async(v);
    }
    if let Some(v) = config.wasm_component_model_async_builtins {
        builder = builder.wasm_component_model_async_builtins(v);
    }
    if let Some(v) = config.wasm_component_model_async_stackful {
        builder = builder.wasm_component_model_async_stackful(v);
    }
    if let Some(v) = config.wasm_component_model_error_context {
        builder = builder.wasm_component_model_error_context(v);
    }
    if let Some(v) = config.wasm_component_model_gc {
        builder = builder.wasm_component_model_gc(v);
    }
    if let Some(v) = config.wasm_component_model_threading {
        builder = builder.wasm_component_model_threading(v);
    }
    if let Some(v) = config.wasm_component_model_fixed_length_lists {
        builder = builder.wasm_component_model_fixed_length_lists(v);
    }

    // WasmFeatures-only flags
    if let Some(v) = config.wasm_mutable_global { builder = builder.wasm_mutable_global(v); }
    if let Some(v) = config.wasm_saturating_float_to_int {
        builder = builder.wasm_saturating_float_to_int(v);
    }
    if let Some(v) = config.wasm_sign_extension { builder = builder.wasm_sign_extension(v); }
    if let Some(v) = config.wasm_floats { builder = builder.wasm_floats(v); }
    if let Some(v) = config.wasm_memory_control { builder = builder.wasm_memory_control(v); }
    if let Some(v) = config.wasm_legacy_exceptions { builder = builder.wasm_legacy_exceptions(v); }
    if let Some(v) = config.wasm_gc_types { builder = builder.wasm_gc_types(v); }
    if let Some(v) = config.wasm_component_model_values {
        builder = builder.wasm_component_model_values(v);
    }
    if let Some(v) = config.wasm_component_model_nested_names {
        builder = builder.wasm_component_model_nested_names(v);
    }
    if let Some(v) = config.wasm_component_model_map {
        builder = builder.wasm_component_model_map(v);
    }
    if let Some(v) = config.wasm_call_indirect_overlong {
        builder = builder.wasm_call_indirect_overlong(v);
    }
    if let Some(v) = config.wasm_bulk_memory_opt { builder = builder.wasm_bulk_memory_opt(v); }
    if let Some(v) = config.wasm_custom_descriptors {
        builder = builder.wasm_custom_descriptors(v);
    }
    if let Some(v) = config.wasm_compact_imports { builder = builder.wasm_compact_imports(v); }

    // Cranelift settings
    if let Some(v) = config.cranelift_debug_verifier {
        builder = builder.cranelift_debug_verifier(v);
    }
    if let Some(v) = config.cranelift_nan_canonicalization {
        builder = builder.cranelift_nan_canonicalization(v);
    }
    if let Some(v) = config.cranelift_pcc { builder = builder.cranelift_pcc(v); }
    if let Some(ref alg) = config.cranelift_regalloc_algorithm {
        builder = builder.cranelift_regalloc_algorithm(match alg.as_str() {
            "single_pass" => RegallocAlgorithm::SinglePass,
            _ => RegallocAlgorithm::Backtracking,
        });
    }
    if let Some(ref flags) = config.cranelift_flags {
        for flag in flags {
            builder = builder.cranelift_flag_set(&flag.name, &flag.value);
        }
    }

    // GC settings
    if let Some(ref v) = config.gc_support {
        builder = builder.gc_support(match v.as_str() {
            "yes" | "true" => true,
            "no" | "false" => false,
            _ => true, // auto defaults to enabled
        });
    }
    if let Some(ref c) = config.collector {
        builder = builder.collector(match c.as_str() {
            "deferred_reference_counting" => wasmtime::Collector::DeferredReferenceCounting,
            "null" => wasmtime::Collector::Null,
            _ => wasmtime::Collector::Auto,
        });
    }

    // Table
    if let Some(v) = config.table_lazy_init { builder = builder.table_lazy_init(v); }

    // SIMD determinism
    if let Some(v) = config.relaxed_simd_deterministic {
        builder = builder.relaxed_simd_deterministic(v);
    }

    // Allocation strategy
    if let Some(ref strat) = config.allocation_strategy {
        if strat == "pooling" {
            builder = builder.allocation_strategy(
                wasmtime::InstanceAllocationStrategy::Pooling(Default::default()),
            );
        }
    }

    // Profiling
    if let Some(ref p) = config.profiling_strategy {
        builder = builder.profiling_strategy(match p.as_str() {
            "jitdump" => wasmtime::ProfilingStrategy::JitDump,
            "vtune" => wasmtime::ProfilingStrategy::VTune,
            "perfmap" => wasmtime::ProfilingStrategy::PerfMap,
            "pulley" => wasmtime::ProfilingStrategy::Pulley,
            _ => wasmtime::ProfilingStrategy::None,
        });
    }

    // Module version strategy
    if let Some(ref mvs) = config.module_version_strategy {
        builder = builder.module_version_strategy(match mvs.as_str() {
            "none" => wasmtime::ModuleVersionStrategy::None,
            "custom" => {
                let custom = config
                    .module_version_custom
                    .as_deref()
                    .unwrap_or("unknown");
                wasmtime::ModuleVersionStrategy::Custom(custom.to_string())
            }
            _ => wasmtime::ModuleVersionStrategy::WasmtimeVersion,
        });
    }

    // macOS-specific
    #[cfg(target_os = "macos")]
    if let Some(v) = config.macos_use_mach_ports {
        builder = builder.macos_use_mach_ports(v);
    }

    // Backtrace and debugging
    if let Some(v) = config.wasm_backtrace {
        builder = builder.wasm_backtrace(v);
    }
    if let Some(ref details) = config.backtrace_details {
        let bt_details = match details.as_str() {
            "enable" => wasmtime::WasmBacktraceDetails::Enable,
            "disable" => wasmtime::WasmBacktraceDetails::Disable,
            "environment" => wasmtime::WasmBacktraceDetails::Environment,
            _ => wasmtime::WasmBacktraceDetails::Enable,
        };
        builder = builder.wasm_backtrace_details(bt_details);
    }
    if let Some(v) = config.generate_address_map {
        builder = builder.generate_address_map(v);
    }

    // Shared memory (independent of wasm threads)
    if let Some(ref v) = config.shared_memory {
        builder = builder.shared_memory(match v.as_str() {
            "yes" | "true" => true,
            "no" | "false" => false,
            _ => false, // auto defaults to disabled
        });
    }

    // Target triple for cross-compilation
    if let Some(ref target) = config.target {
        builder = builder.target(target);
    }

    // Memory guaranteed dense image size
    if let Some(v) = config.memory_guaranteed_dense_image_size {
        builder = builder.memory_guaranteed_dense_image_size(v);
    }

    // Cache configuration
    if let Some(true) = config.cache_enabled {
        if let Some(ref path) = config.cache_config_path {
            builder = builder.cache_config_load(path)?;
        } else {
            builder = builder.cache_config_load_default()?;
        }
    }

    // Wasmtime 41.0.3 config additions
    if let Some(v) = config.compiler_inlining {
        builder = builder.compiler_inlining(v);
    }
    if let Some(v) = config.debug_adapter_modules {
        builder = builder.debug_adapter_modules(v);
    }
    #[cfg(feature = "wmemcheck")]
    if let Some(v) = config.wmemcheck {
        builder = builder.wmemcheck(v);
    }
    if let Some(v) = config.force_memory_init_memfd {
        builder = builder.force_memory_init_memfd(v);
    }
    if let Some(v) = config.cranelift_debug_checks {
        builder = builder.cranelift_debug_checks(v);
    }
    if let Some(v) = config.enable_compiler {
        builder = builder.enable_compiler(v);
    }

    // Guest debugging instrumentation
    if let Some(v) = config.guest_debug {
        builder = builder.guest_debug(v);
    }

    // Cranelift IR output directory
    if let Some(ref path) = config.emit_clif {
        builder = builder.emit_clif(path);
    }

    // Cranelift flag enables (single-flag variant)
    if let Some(ref enables) = config.cranelift_flag_enables {
        for flag_name in enables {
            builder = builder.cranelift_flag_enable(flag_name);
        }
    }

    // x86 float ABI acknowledgment
    if let Some(v) = config.x86_float_abi_ok {
        builder = builder.x86_float_abi_ok(v);
    }

    // signals_based_traps: always override to false for JVM safety
    if let Some(v) = config.signals_based_traps {
        if v {
            log::warn!(
                "signals_based_traps(true) requested but overridden to false for JVM safety"
            );
        }
        // Always set to false regardless of the requested value
        builder = builder.signals_based_traps(false);
    }

    // Record/replay configuration
    #[cfg(feature = "rr")]
    if let Some(ref rr) = config.rr_config {
        let rr_value = match rr.as_str() {
            "recording" => 1,
            "replaying" => 2,
            _ => 0,
        };
        builder = builder.rr(rr_value);
    }

    Ok(builder)
}

/// Create an engine from JSON configuration bytes with an incremental compilation cache store
///
/// This is the same as `create_engine_from_json_config` but additionally enables
/// incremental compilation with the provided cache store.
pub fn create_engine_from_json_config_with_cache_store(
    json_bytes: &[u8],
    cache_store: std::sync::Arc<dyn wasmtime::CacheStore>,
) -> WasmtimeResult<Box<Engine>> {
    let config: EngineConfigFfi =
        serde_json::from_slice(json_bytes).map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to parse engine config JSON: {}", e),
        })?;

    // Build the engine using the same config parsing as create_engine_from_json_config
    // but add the cache store before building
    let mut builder = build_engine_from_config(config)?;
    builder = builder.enable_incremental_compilation(cache_store)?;
    builder.build().map(Box::new)
}

/// Create an engine from JSON configuration bytes with extension traits.
///
/// This is the general version that can accept any combination of extension traits:
/// - cache_store: Incremental compilation cache
/// - memory_creator: Custom linear memory allocation
/// - stack_creator: Custom async fiber stack allocation
/// - code_memory: Custom JIT code memory management
pub fn create_engine_from_json_config_with_extensions(
    json_bytes: &[u8],
    cache_store: Option<std::sync::Arc<dyn wasmtime::CacheStore>>,
    memory_creator: Option<std::sync::Arc<dyn wasmtime::MemoryCreator>>,
    stack_creator: Option<std::sync::Arc<dyn wasmtime::StackCreator>>,
    code_memory: Option<std::sync::Arc<dyn wasmtime::CustomCodeMemory>>,
) -> WasmtimeResult<Box<Engine>> {
    let config: EngineConfigFfi =
        serde_json::from_slice(json_bytes).map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to parse engine config JSON: {}", e),
        })?;

    let mut builder = build_engine_from_config(config)?;

    if let Some(cs) = cache_store {
        builder = builder.enable_incremental_compilation(cs)?;
    }
    if let Some(mc) = memory_creator {
        builder = builder.with_host_memory(mc);
    }
    if let Some(sc) = stack_creator {
        builder = builder.with_host_stack(sc);
    }
    if let Some(cm) = code_memory {
        builder = builder.with_custom_code_memory(cm);
    }

    builder.build().map(Box::new)
}

/// Core function to eagerly initialize Wasmtime's thread-local state
///
/// This pre-initializes Wasmtime's thread-local storage for the calling thread,
/// avoiding the latency spike on the first WebAssembly operation.
pub fn tls_eager_initialize() -> WasmtimeResult<()> {
    WasmtimeEngine::tls_eager_initialize();
    Ok(())
}

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
    max_stack_size: Option<usize>,
    epoch_interruption: bool,
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

    if let Some(stack_size) = max_stack_size {
        builder = builder.max_stack_size(stack_size);
    }

    builder.build().map(Box::new)
}

/// Core function to create an engine with extended configuration including GC and memory options
#[allow(clippy::too_many_arguments)]
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
    max_stack_size: Option<usize>,
    epoch_interruption: bool,
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
    // Profiling and debug
    profiling_strategy: Option<wasmtime::ProfilingStrategy>,
    native_unwind_info: bool,
    // Extended config options
    cranelift_debug_verifier: bool,
    async_stack_size: Option<usize>,
    memory_may_move: bool,
    guard_before_linear_memory: bool,
    parallel_compilation: bool,
    pooling_allocator: bool,
    table_lazy_init: bool,
    relaxed_simd_deterministic: bool,
    memory_init_cow: bool,
    async_stack_zeroing: bool,
    gc_support: bool,
    // Cranelift flags (key=value pairs)
    cranelift_flags: &[(String, String)],
    // Module version strategy: 0=WasmtimeVersion, 1=None, 2=Custom
    module_version_strategy: i32,
    module_version_custom: Option<&str>,
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
        .wasm_wide_arithmetic(wasm_wide_arithmetic)
        // Extended config
        .cranelift_debug_verifier(cranelift_debug_verifier)
        .memory_may_move(memory_may_move)
        .guard_before_linear_memory(guard_before_linear_memory)
        .parallel_compilation(parallel_compilation)
        .table_lazy_init(table_lazy_init)
        .relaxed_simd_deterministic(relaxed_simd_deterministic)
        .memory_init_cow(memory_init_cow)
        .async_stack_zeroing(async_stack_zeroing)
        .gc_support(gc_support);

    if let Some(stack_size) = max_stack_size {
        builder = builder.max_stack_size(stack_size);
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

    // Profiling and debug
    if let Some(profiling) = profiling_strategy {
        builder = builder.profiling_strategy(profiling);
    }

    builder = builder.native_unwind_info(native_unwind_info);

    // Async stack size
    if let Some(async_size) = async_stack_size {
        builder = builder.async_stack_size(async_size);
    }

    // Pooling allocator
    if pooling_allocator {
        builder = builder.allocation_strategy(
            wasmtime::InstanceAllocationStrategy::Pooling(Default::default()),
        );
    }

    // Cranelift flags
    for (key, value) in cranelift_flags {
        builder = builder.cranelift_flag_set(key, value);
    }

    // Module version strategy
    match module_version_strategy {
        1 => {
            builder = builder
                .module_version_strategy(wasmtime::ModuleVersionStrategy::None);
        }
        2 => {
            if let Some(custom) = module_version_custom {
                builder = builder.module_version_strategy(
                    wasmtime::ModuleVersionStrategy::Custom(custom.to_string()),
                );
            }
        }
        _ => {
            // 0 or default: WasmtimeVersion (the default, no action needed)
        }
    }

    builder.build().map(Box::new)
}

/// Core function to detect if a host CPU feature is available at **runtime**.
///
/// On x86/x86_64 this uses `std::arch::is_x86_feature_detected!()` which
/// queries CPUID at runtime, giving correct results even when the binary was
/// cross-compiled or built with a different `-C target-cpu`.
///
/// On AArch64, NEON is architecturally mandatory so a static `true` suffices.
pub fn detect_host_feature(feature_name: &str) -> bool {
    #[cfg(any(target_arch = "x86", target_arch = "x86_64"))]
    match feature_name {
        "sse" => return std::arch::is_x86_feature_detected!("sse"),
        "sse2" => return std::arch::is_x86_feature_detected!("sse2"),
        "sse3" => return std::arch::is_x86_feature_detected!("sse3"),
        "ssse3" => return std::arch::is_x86_feature_detected!("ssse3"),
        "sse4.1" | "sse41" => return std::arch::is_x86_feature_detected!("sse4.1"),
        "sse4.2" | "sse42" => return std::arch::is_x86_feature_detected!("sse4.2"),
        "avx" => return std::arch::is_x86_feature_detected!("avx"),
        "avx2" => return std::arch::is_x86_feature_detected!("avx2"),
        "avx512f" => return std::arch::is_x86_feature_detected!("avx512f"),
        "bmi1" => return std::arch::is_x86_feature_detected!("bmi1"),
        "bmi2" => return std::arch::is_x86_feature_detected!("bmi2"),
        "lzcnt" => return std::arch::is_x86_feature_detected!("lzcnt"),
        "popcnt" => return std::arch::is_x86_feature_detected!("popcnt"),
        "fma" => return std::arch::is_x86_feature_detected!("fma"),
        _ => {}
    }

    #[cfg(target_arch = "aarch64")]
    if feature_name == "neon" {
        return true;
    }

    false
}

/// Core function to validate engine pointer and get reference
///
/// # Safety
///
/// The caller must ensure engine_ptr is a valid pointer to an Engine.
pub unsafe fn get_engine_ref(engine_ptr: *const c_void) -> WasmtimeResult<&'static Engine> {
    validate_ptr_not_null!(engine_ptr, "engine");
    Ok(&*(engine_ptr as *const Engine))
}

/// Core function to validate engine pointer and get mutable reference
///
/// # Safety
///
/// The caller must ensure engine_ptr is a valid pointer to an Engine.
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
///
/// This function marks the engine as closed before destroying it, ensuring
/// that any other threads using the engine will get a clean error rather
/// than a use-after-free crash.
///
/// # Safety
///
/// The caller must ensure engine_ptr is a valid pointer to an Engine.
pub unsafe fn destroy_engine(engine_ptr: *mut c_void) {
    if engine_ptr.is_null() {
        return;
    }

    // Mark the engine as closed before destroying it
    // This ensures other threads will see the closed state
    let engine = &*(engine_ptr as *const Engine);
    engine.mark_closed();

    // Now safe to destroy
    ffi_utils::destroy_resource::<Engine>(engine_ptr, "Engine");
}

/// Core function to create a weak reference to an engine
///
/// Returns a boxed WeakEngine pointer that the caller must free with `destroy_weak_engine`.
///
/// # Safety
///
/// The caller must ensure engine_ptr is a valid pointer to an Engine.
pub unsafe fn create_weak_engine(engine_ptr: *const c_void) -> WasmtimeResult<Box<super::WeakEngine>> {
    validate_ptr_not_null!(engine_ptr, "engine");
    let engine = &*(engine_ptr as *const Engine);
    Ok(Box::new(engine.weak()))
}

/// Core function to upgrade a weak engine reference to a strong engine
///
/// Returns `Some(Box<Engine>)` if the engine is still alive, `None` otherwise.
///
/// # Safety
///
/// The caller must ensure weak_ptr is a valid pointer to a WeakEngine.
pub unsafe fn upgrade_weak_engine(weak_ptr: *const c_void) -> WasmtimeResult<Option<Box<Engine>>> {
    validate_ptr_not_null!(weak_ptr, "weak_engine");
    let weak = &*(weak_ptr as *const super::WeakEngine);
    Ok(weak.upgrade().map(Box::new))
}

/// Core function to destroy a weak engine reference
///
/// # Safety
///
/// The caller must ensure weak_ptr is a valid pointer to a WeakEngine.
pub unsafe fn destroy_weak_engine(weak_ptr: *mut c_void) {
    if weak_ptr.is_null() {
        return;
    }
    ffi_utils::destroy_resource::<super::WeakEngine>(weak_ptr, "WeakEngine");
}

/// Core function to validate engine functionality
pub fn validate_engine(engine: &Engine) -> WasmtimeResult<()> {
    engine.validate()
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
        return Err(WasmtimeError::Validation {
            message: "WASM bytes cannot be empty".to_string(),
        });
    }
    engine
        .inner()
        .precompile_module(wasm_bytes)
        .map_err(|e| WasmtimeError::Compilation {
            message: format!("Failed to precompile module: {}", e),
        })
}

/// Core function to precompile a WebAssembly component for AOT usage
#[cfg(feature = "component-model")]
pub fn precompile_component(engine: &Engine, wasm_bytes: &[u8]) -> WasmtimeResult<Vec<u8>> {
    if wasm_bytes.is_empty() {
        return Err(WasmtimeError::Validation {
            message: "WASM bytes cannot be empty".to_string(),
        });
    }
    engine
        .inner()
        .precompile_component(wasm_bytes)
        .map_err(|e| WasmtimeError::Compilation {
            message: format!("Failed to precompile component: {}", e),
        })
}

/// Pooling allocator metrics data returned to Java
///
/// Each field corresponds to a method on wasmtime::PoolingAllocatorMetrics.
/// Returned as a flat array of 12 i64 values for FFI simplicity.
/// Index mapping:
///   0: core_instances (u64)
///   1: component_instances (u64)
///   2: memories (usize)
///   3: tables (usize)
///   4: stacks (usize)
///   5: gc_heaps (usize)
///   6: unused_warm_memories (u32)
///   7: unused_memory_bytes_resident (usize)
///   8: unused_warm_tables (u32)
///   9: unused_table_bytes_resident (usize)
///  10: unused_warm_stacks (u32)
///  11: unused_stack_bytes_resident (isize, -1 if unavailable)
pub fn pooling_allocator_metrics(engine: &Engine) -> Option<[i64; 12]> {
    let metrics = engine.inner().pooling_allocator_metrics()?;
    Some([
        metrics.core_instances() as i64,
        metrics.component_instances() as i64,
        metrics.memories() as i64,
        metrics.tables() as i64,
        metrics.stacks() as i64,
        metrics.gc_heaps() as i64,
        metrics.unused_warm_memories() as i64,
        metrics.unused_memory_bytes_resident() as i64,
        metrics.unused_warm_tables() as i64,
        metrics.unused_table_bytes_resident() as i64,
        metrics.unused_warm_stacks() as i64,
        metrics
            .unused_stack_bytes_resident()
            .map(|v| v as i64)
            .unwrap_or(-1),
    ])
}

/// Core function to create a standalone shared memory from an engine.
///
/// Shared memory does not require a Store - it can be created from just an Engine.
/// The engine must be configured with threads support enabled.
///
/// # Arguments
/// * `engine` - Reference to the Wasmtime engine
/// * `initial_pages` - Initial number of 64KB pages
/// * `max_pages` - Maximum number of 64KB pages (required for shared memory)
///
/// # Returns
/// A pointer to a ValidatedMemory wrapping the SharedMemory
pub fn create_shared_memory(
    engine: &Engine,
    initial_pages: u64,
    max_pages: u64,
) -> WasmtimeResult<*mut ValidatedMemory> {
    if max_pages < initial_pages {
        return Err(WasmtimeError::InvalidParameter {
            message: format!(
                "max_pages ({}) cannot be less than initial_pages ({})",
                max_pages, initial_pages
            ),
        });
    }

    let memory_type = MemoryType::shared(initial_pages as u32, max_pages as u32);
    let shared_memory =
        WasmtimeSharedMemory::new(engine.inner(), memory_type).map_err(|e| {
            WasmtimeError::Memory {
                message: format!("Failed to create shared memory: {}", e),
            }
        })?;

    let memory = Memory::from_shared_memory(shared_memory);
    create_validated_memory(memory)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_create_engine_from_empty_json() {
        let json = b"{}";
        let result = create_engine_from_json_config(json);
        assert!(
            result.is_ok(),
            "Empty JSON should create engine with defaults: {:?}",
            result.err()
        );
    }

    #[test]
    fn test_create_engine_from_json_with_defaults() {
        let json = br#"{"optLevel":"speed","debugInfo":false,"fuelEnabled":false,"epochInterruption":false,"asyncSupport":false,"coredumpOnTrap":false,"parallelCompilation":true,"nativeUnwindInfo":true}"#;
        let result = create_engine_from_json_config(json);
        assert!(
            result.is_ok(),
            "Default config JSON should create engine: {:?}",
            result.err()
        );
    }

    #[test]
    fn test_create_engine_from_json_with_all_wasm_features() {
        let json = br#"{"optLevel":"speed","debugInfo":false,"fuelEnabled":false,"epochInterruption":false,"asyncSupport":false,"coredumpOnTrap":false,"parallelCompilation":true,"nativeUnwindInfo":true,"asyncStackZeroing":true,"memoryMayMove":true,"guardBeforeLinearMemory":true,"memoryInitCow":true,"wasmThreads":false,"wasmReferenceTypes":true,"wasmSimd":true,"wasmBulkMemory":true,"wasmMultiValue":true,"wasmMultiMemory":false,"wasmTailCall":true,"wasmRelaxedSimd":true,"wasmFunctionReferences":true,"wasmGc":true,"wasmExceptions":true,"wasmMemory64":false,"wasmExtendedConst":true,"wasmCustomPageSizes":false,"wasmWideArithmetic":false,"wasmStackSwitching":false,"wasmSharedEverythingThreads":false,"wasmComponentModelAsync":false,"wasmComponentModelAsyncBuiltins":false,"wasmComponentModelAsyncStackful":false,"wasmComponentModelErrorContext":false,"wasmComponentModelGc":false,"wasmComponentModelThreading":false,"craneliftDebugVerifier":false,"craneliftNanCanonicalization":false,"craneliftPcc":false}"#;
        let result = create_engine_from_json_config(json);
        assert!(
            result.is_ok(),
            "Full config JSON should create engine: {:?}",
            result.err()
        );
    }
}
