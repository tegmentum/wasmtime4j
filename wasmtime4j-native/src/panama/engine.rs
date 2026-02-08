//! Panama FFI bindings for Wasmtime engine operations.
//!
//! This module provides C-compatible functions for creating, configuring,
//! and managing Wasmtime engines through the Panama Foreign Function Interface.

use std::os::raw::{c_char, c_int, c_long, c_void};

use crate::engine::core;
use crate::error::ffi_utils;
use crate::ffi_common::{error_handling, parameter_conversion};

/// Create a new Wasmtime engine with default configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_create() -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| core::create_engine())
}

/// Create a new Wasmtime engine with custom configuration (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_create_with_config(
    strategy: c_int,
    opt_level: c_int,
    debug_info: c_int,
    wasm_threads: c_int,
    wasm_simd: c_int,
    wasm_reference_types: c_int,
    wasm_bulk_memory: c_int,
    wasm_multi_value: c_int,
    fuel_enabled: c_int,
    max_memory_pages: c_int,
    max_stack_size: c_int,
    epoch_interruption: c_int,
    max_instances: c_int,
) -> *mut c_void {


    ffi_utils::ffi_try_ptr(|| {
        let strategy_opt = parameter_conversion::convert_strategy(strategy);
        let opt_level_opt = parameter_conversion::convert_opt_level(opt_level);
        let max_memory_pages_opt = parameter_conversion::convert_int_to_optional_u32(max_memory_pages);
        let max_stack_size_opt = parameter_conversion::convert_int_to_optional_usize(max_stack_size);
        let max_instances_opt = parameter_conversion::convert_int_to_optional_u32(max_instances);

        core::create_engine_with_config(
            strategy_opt,
            opt_level_opt,
            parameter_conversion::convert_int_to_bool(debug_info),
            parameter_conversion::convert_int_to_bool(wasm_threads),
            parameter_conversion::convert_int_to_bool(wasm_simd),
            parameter_conversion::convert_int_to_bool(wasm_reference_types),
            parameter_conversion::convert_int_to_bool(wasm_bulk_memory),
            parameter_conversion::convert_int_to_bool(wasm_multi_value),
            parameter_conversion::convert_int_to_bool(fuel_enabled),
            max_memory_pages_opt,
            max_stack_size_opt,
            parameter_conversion::convert_int_to_bool(epoch_interruption),
            max_instances_opt,
            false,  // async_support - TODO: add Panama parameter
        )
    })
}

/// Destroy a Wasmtime engine (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_destroy(engine_ptr: *mut c_void) {
    unsafe {
        core::destroy_engine(engine_ptr);
    }
}

/// Check if fuel consumption is enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_is_fuel_enabled(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => if engine.fuel_enabled() { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Check if epoch interruption is enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_is_epoch_interruption_enabled(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => if engine.epoch_interruption_enabled() { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Check if coredump generation on trap is enabled (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_is_coredump_on_trap_enabled(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => if engine.coredump_on_trap() { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Get memory limit in pages (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_get_memory_limit(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => engine.memory_limit_pages().map(|limit| limit as c_int).unwrap_or(-1),
        Err(_) => -1,
    }
}

/// Get stack size limit in bytes (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_get_stack_limit(engine_ptr: *mut c_void) -> c_long {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => engine.stack_size_limit().map(|limit| limit as c_long).unwrap_or(-1),
        Err(_) => -1,
    }
}

/// Get maximum instances limit (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_get_max_instances(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => core::get_max_instances(engine).map(|limit| limit as c_int).unwrap_or(-1),
        Err(_) => -1,
    }
}

/// Validate engine functionality (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_validate(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => if core::validate_engine(engine).is_ok() { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Check if engine supports WebAssembly feature by name (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_supports_feature(
    engine_ptr: *mut c_void,
    feature_name: *const c_char,
) -> c_int {
    use crate::engine::WasmFeature;

    // Convert C string to Rust string
    let feature_str = match unsafe { std::ffi::CStr::from_ptr(feature_name) }.to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    // Parse feature name to WasmFeature enum
    // Note: Some Java enum names have aliases for compatibility
    let feature = match feature_str {
        "THREADS" => WasmFeature::Threads,
        "REFERENCE_TYPES" => WasmFeature::ReferenceTypes,
        "SIMD" => WasmFeature::Simd,
        "BULK_MEMORY" => WasmFeature::BulkMemory,
        "MULTI_VALUE" => WasmFeature::MultiValue,
        "TAIL_CALL" => WasmFeature::TailCall,
        "MULTI_MEMORY" => WasmFeature::MultiMemory,
        "MEMORY64" => WasmFeature::Memory64,
        "EXCEPTIONS" => WasmFeature::Exceptions,
        "RELAXED_SIMD" => WasmFeature::RelaxedSimd,
        // Accept both Java enum name and native name
        "EXTENDED_CONST" | "EXTENDED_CONST_EXPRESSIONS" => WasmFeature::ExtendedConst,
        "COMPONENT_MODEL" => WasmFeature::ComponentModel,
        // Accept both Java enum name and native name
        "FUNCTION_REFERENCES" | "TYPED_FUNCTION_REFERENCES" => WasmFeature::FunctionReferences,
        "GC" => WasmFeature::Gc,
        "CUSTOM_PAGE_SIZES" => WasmFeature::CustomPageSizes,
        "WIDE_ARITHMETIC" => WasmFeature::WideArithmetic,
        "STACK_SWITCHING" => WasmFeature::StackSwitching,
        "SHARED_EVERYTHING_THREADS" => WasmFeature::SharedEverythingThreads,
        "COMPONENT_MODEL_ASYNC" => WasmFeature::ComponentModelAsync,
        "COMPONENT_MODEL_ASYNC_BUILTINS" => WasmFeature::ComponentModelAsyncBuiltins,
        "COMPONENT_MODEL_ASYNC_STACKFUL" => WasmFeature::ComponentModelAsyncStackful,
        "COMPONENT_MODEL_ERROR_CONTEXT" => WasmFeature::ComponentModelErrorContext,
        "COMPONENT_MODEL_GC" => WasmFeature::ComponentModelGc,
        _ => return -1,
    };

    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => if engine.supports_feature(feature) { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Get engine reference count for debugging (Panama FFI version)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_get_reference_count(engine_ptr: *mut c_void) -> c_int {
    // Demonstrate shared error handling utilities
    match error_handling::validate_void_pointer_as::<crate::engine::Engine>(engine_ptr, "engine") {
        Ok(_) => {
            // Now we can safely use the engine
            match unsafe { core::get_engine_ref(engine_ptr) } {
                Ok(engine) => core::get_reference_count(engine) as c_int,
                Err(e) => {
                    let error_info = error_handling::convert_internal_error(&e);
                    log::error!("Engine access failed: {}", error_info.message);
                    -1
                }
            }
        },
        Err(validation_error) => {
            let error_info = error_handling::validation_error_to_info(validation_error);
            log::error!("Parameter validation failed: {}", error_info.message);
            -1
        }
    }
}

/// Increment the epoch counter (Panama FFI version)
///
/// This function is signal-safe and performs only an atomic increment.
/// The epoch counter is used for epoch-based interruption of WebAssembly execution.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_increment_epoch(engine_ptr: *mut c_void) {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => {
            engine.increment_epoch();
        },
        Err(e) => {
            log::error!("Failed to increment epoch: {:?}", e);
        }
    }
}

/// Precompile a WebAssembly module for AOT (ahead-of-time) usage (Panama FFI version)
///
/// Takes raw WebAssembly bytes and compiles them to a serialized form that can
/// be loaded later via Module::deserialize without needing to recompile.
///
/// Returns 0 on success, non-zero on failure.
/// The caller is responsible for freeing the output data with wasmtime4j_panama_free_bytes.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_precompile_module(
    engine_ptr: *mut c_void,
    wasm_bytes: *const u8,
    wasm_len: usize,
    out_data: *mut *mut u8,
    out_len: *mut usize,
) -> c_int {
    if engine_ptr.is_null() || wasm_bytes.is_null() || out_data.is_null() || out_len.is_null() {
        return -1;
    }

    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => {
            let bytes = unsafe { std::slice::from_raw_parts(wasm_bytes, wasm_len) };
            match core::precompile_module(engine, bytes) {
                Ok(precompiled) => {
                    let len = precompiled.len();
                    let data = Box::into_raw(precompiled.into_boxed_slice()) as *mut u8;
                    unsafe {
                        *out_data = data;
                        *out_len = len;
                    }
                    0  // Success
                }
                Err(e) => {
                    log::error!("Failed to precompile module: {:?}", e);
                    -1
                }
            }
        }
        Err(e) => {
            log::error!("Invalid engine pointer: {:?}", e);
            -1
        }
    }
}

/// Check if the engine is using Pulley interpreter (Panama FFI version)
///
/// Returns 1 if using Pulley, 0 if not, -1 on error.
/// Note: Pulley is only available in wasmtime >= 40.0.0. In 39.0.1, always returns 0.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_is_pulley(engine_ptr: *mut c_void) -> c_int {
    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(_engine) => {
            // Pulley is not available in wasmtime 39.0.1
            // Return 0 (not using Pulley) - this is the correct behavior for pre-Pulley versions
            0
        }
        Err(_) => -1,
    }
}

/// Get the precompile compatibility hash for the engine (Panama FFI version)
///
/// Writes a u64 hash to out_hash buffer (must be at least 8 bytes).
/// Uses wasmtime 41.0.1 Engine::precompile_compatibility_hash() API.
/// Returns 0 on success, -1 on error.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_precompile_compatibility_hash(
    engine_ptr: *mut c_void,
    out_hash: *mut u64,
) -> c_int {
    use std::hash::{Hash, Hasher};
    use std::collections::hash_map::DefaultHasher;

    match unsafe { core::get_engine_ref(engine_ptr) } {
        Ok(engine) => {
            let mut hasher = DefaultHasher::new();
            engine.inner().precompile_compatibility_hash().hash(&mut hasher);
            let hash = hasher.finish();

            unsafe {
                if !out_hash.is_null() {
                    *out_hash = hash;
                }
            }
            0  // Success
        }
        Err(e) => {
            log::error!("Invalid engine pointer: {:?}", e);
            -1
        }
    }
}

/// Create an engine with extended configuration including GC and memory options (Panama FFI version)
///
/// Returns a pointer to the engine or null on error.
/// All boolean parameters: 0 = false, 1 = true
/// Memory parameters: 0 = use default
#[no_mangle]
#[allow(clippy::too_many_arguments)]
pub extern "C" fn wasmtime4j_panama_engine_create_with_extended_config(
    strategy: c_int,
    opt_level: c_int,
    debug_info: c_int,
    wasm_threads: c_int,
    wasm_simd: c_int,
    wasm_reference_types: c_int,
    wasm_bulk_memory: c_int,
    wasm_multi_value: c_int,
    fuel_enabled: c_int,
    max_memory_pages: c_int,
    max_stack_size: c_int,
    epoch_interruption: c_int,
    max_instances: c_int,
    async_support: c_int,
    // GC configuration
    wasm_gc: c_int,
    wasm_function_references: c_int,
    wasm_exceptions: c_int,
    // Memory configuration (0 = use default)
    memory_reservation: i64,
    memory_guard_size: i64,
    memory_reservation_for_growth: i64,
    // Additional features
    wasm_tail_call: c_int,
    wasm_relaxed_simd: c_int,
    wasm_multi_memory: c_int,
    wasm_memory64: c_int,
    wasm_extended_const: c_int,
    wasm_component_model: c_int,
    coredump_on_trap: c_int,
    cranelift_nan_canonicalization: c_int,
    // Experimental features
    wasm_custom_page_sizes: c_int,
    wasm_wide_arithmetic: c_int,
) -> *mut c_void {
    let strategy_opt = parameter_conversion::convert_strategy(strategy);
    let opt_level_opt = parameter_conversion::convert_opt_level(opt_level);
    let max_memory_pages_opt = parameter_conversion::convert_int_to_optional_u32(max_memory_pages);
    let max_stack_size_opt = parameter_conversion::convert_int_to_optional_usize(max_stack_size);
    let max_instances_opt = parameter_conversion::convert_int_to_optional_u32(max_instances);

    // Memory config: 0 means use default
    let memory_reservation_opt = if memory_reservation > 0 { Some(memory_reservation as u64) } else { None };
    let memory_guard_size_opt = if memory_guard_size > 0 { Some(memory_guard_size as u64) } else { None };
    let memory_reservation_for_growth_opt = if memory_reservation_for_growth > 0 {
        Some(memory_reservation_for_growth as u64)
    } else {
        None
    };

    match core::create_engine_with_extended_config(
        strategy_opt,
        opt_level_opt,
        parameter_conversion::convert_int_to_bool(debug_info),
        parameter_conversion::convert_int_to_bool(wasm_threads),
        parameter_conversion::convert_int_to_bool(wasm_simd),
        parameter_conversion::convert_int_to_bool(wasm_reference_types),
        parameter_conversion::convert_int_to_bool(wasm_bulk_memory),
        parameter_conversion::convert_int_to_bool(wasm_multi_value),
        parameter_conversion::convert_int_to_bool(fuel_enabled),
        max_memory_pages_opt,
        max_stack_size_opt,
        parameter_conversion::convert_int_to_bool(epoch_interruption),
        max_instances_opt,
        parameter_conversion::convert_int_to_bool(async_support),
        // GC configuration
        parameter_conversion::convert_int_to_bool(wasm_gc),
        parameter_conversion::convert_int_to_bool(wasm_function_references),
        parameter_conversion::convert_int_to_bool(wasm_exceptions),
        // Memory configuration
        memory_reservation_opt,
        memory_guard_size_opt,
        memory_reservation_for_growth_opt,
        // Additional features
        parameter_conversion::convert_int_to_bool(wasm_tail_call),
        parameter_conversion::convert_int_to_bool(wasm_relaxed_simd),
        parameter_conversion::convert_int_to_bool(wasm_multi_memory),
        parameter_conversion::convert_int_to_bool(wasm_memory64),
        parameter_conversion::convert_int_to_bool(wasm_extended_const),
        parameter_conversion::convert_int_to_bool(wasm_component_model),
        parameter_conversion::convert_int_to_bool(coredump_on_trap),
        parameter_conversion::convert_int_to_bool(cranelift_nan_canonicalization),
        // Experimental features
        parameter_conversion::convert_int_to_bool(wasm_custom_page_sizes),
        parameter_conversion::convert_int_to_bool(wasm_wide_arithmetic),
    ) {
        Ok(engine) => Box::into_raw(engine) as *mut c_void,
        Err(e) => {
            log::error!("Failed to create engine with extended config: {:?}", e);
            std::ptr::null_mut()
        }
    }
}

/// Detect if a host CPU feature is available (Panama FFI version)
///
/// feature_name: Null-terminated C string of the feature name (e.g., "sse4.2", "avx2")
/// Returns 1 if feature is available, 0 if not.
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_engine_detect_host_feature(
    feature_name: *const c_char,
) -> c_int {
    if feature_name.is_null() {
        return 0;
    }

    let feature_str = unsafe {
        match std::ffi::CStr::from_ptr(feature_name).to_str() {
            Ok(s) => s,
            Err(_) => return 0,
        }
    };

    if core::detect_host_feature(feature_str) { 1 } else { 0 }
}
