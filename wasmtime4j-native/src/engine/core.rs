//! Shared core functions for engine operations
//!
//! These functions eliminate code duplication and provide consistent behavior
//! across JNI and Panama interface implementations while maintaining defensive
//! programming practices.

use std::os::raw::c_void;

use wasmtime::{OptLevel, Strategy};

use super::{Engine, EngineBuilder, EngineConfigSummary, WasmFeature};
use crate::error::{ffi_utils, WasmtimeError, WasmtimeResult};
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
