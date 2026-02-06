//! Panama Foreign Function Interface bindings for experimental WebAssembly features
//!
//! This module provides Panama FFI bindings that bridge Java experimental feature configurations
//! with native Rust implementations of experimental WebAssembly proposals and advanced
//! Wasmtime capabilities.
//!
//! WARNING: These bindings expose highly experimental features that are unstable
//! and subject to significant change.

use std::ffi::{CString, CStr, c_char, c_void};
use std::ptr;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::experimental_features::core as exp_core;
use crate::advanced_experimental::core as adv_core;
use crate::validate_ptr_not_null;

/// Create experimental features configuration
#[no_mangle]
pub extern "C" fn wasmtime4j_create_experimental_features() -> *mut c_void {
    match create_experimental_features_impl() {
        Ok(ptr) => ptr,
        Err(e) => {
            log::error!("Failed to create experimental features: {:?}", e);
            ptr::null_mut()
        }
    }
}

fn create_experimental_features_impl() -> WasmtimeResult<*mut c_void> {
    // Create experimental features config - this is what exp_core functions expect
    let exp_config = exp_core::create_experimental_features_config()?;

    let ptr = Box::into_raw(exp_config) as *mut c_void;

    log::info!("Created native experimental features instance: ptr={:p}", ptr);
    Ok(ptr)
}

/// Enable experimental feature
#[no_mangle]
pub extern "C" fn wasmtime4j_enable_experimental_feature(
    handle: *mut c_void,
    feature_key: *const c_char,
) {
    match enable_experimental_feature_impl(handle, feature_key) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to enable experimental feature: {:?}", e);
        }
    }
}

fn enable_experimental_feature_impl(
    handle: *mut c_void,
    feature_key: *const c_char,
) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");
    validate_ptr_not_null!(feature_key, "feature_key");

    let feature_key_str = unsafe { CStr::from_ptr(feature_key) }
        .to_str()
        .map_err(|e| WasmtimeError::InvalidParameter {
            message: format!("Invalid feature key string: {}", e),
        })?;

    log::info!("Enabling experimental feature: {} (handle: {:p})", feature_key_str, handle);

    // For now, we just log the feature enablement
    // In a full implementation, this would configure the actual feature
    match feature_key_str {
        "stack-switching" => {
            log::info!("Stack switching would be enabled here");
        },
        "call-cc" => {
            log::info!("Call/CC would be enabled here");
        },
        "extended-const-expressions" => {
            log::info!("Extended constant expressions would be enabled here");
        },
        "memory64-extended" => {
            log::info!("Memory64 extended would be enabled here");
        },
        "advanced-jit" => {
            log::info!("Advanced JIT optimizations would be enabled here");
        },
        "advanced-sandbox" => {
            log::info!("Advanced sandboxing would be enabled here");
        },
        _ => {
            log::warn!("Unknown experimental feature: {}", feature_key_str);
        }
    }

    Ok(())
}

/// Disable experimental feature
#[no_mangle]
pub extern "C" fn wasmtime4j_disable_experimental_feature(
    handle: *mut c_void,
    feature_key: *const c_char,
) {
    match disable_experimental_feature_impl(handle, feature_key) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to disable experimental feature: {:?}", e);
        }
    }
}

fn disable_experimental_feature_impl(
    handle: *mut c_void,
    feature_key: *const c_char,
) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");
    validate_ptr_not_null!(feature_key, "feature_key");

    let feature_key_str = unsafe { CStr::from_ptr(feature_key) }
        .to_str()
        .map_err(|e| WasmtimeError::InvalidParameter {
            message: format!("Invalid feature key string: {}", e),
        })?;

    log::info!("Disabling experimental feature: {} (handle: {:p})", feature_key_str, handle);

    // For now, we just log the feature disablement
    Ok(())
}

/// Configure stack switching
#[no_mangle]
pub extern "C" fn wasmtime4j_configure_stack_switching(
    handle: *mut c_void,
    stack_size: u64,
    max_stacks: u32,
    strategy: i32,
) {
    match configure_stack_switching_impl(handle, stack_size, max_stacks, strategy) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to configure stack switching: {:?}", e);
        }
    }
}

fn configure_stack_switching_impl(
    handle: *mut c_void,
    stack_size: u64,
    max_stacks: u32,
    _strategy: i32,
) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    unsafe {
        exp_core::enable_stack_switching(handle, stack_size, max_stacks)
    }
}

/// Configure call/cc
#[no_mangle]
pub extern "C" fn wasmtime4j_configure_call_cc(
    handle: *mut c_void,
    max_continuations: u32,
    storage_strategy: i32,
    compression_enabled: i32,
) {
    match configure_call_cc_impl(handle, max_continuations, storage_strategy, compression_enabled) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to configure call/cc: {:?}", e);
        }
    }
}

fn configure_call_cc_impl(
    handle: *mut c_void,
    max_continuations: u32,
    storage_strategy: i32,
    _compression_enabled: i32,
) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    unsafe {
        exp_core::enable_call_cc(handle, max_continuations, storage_strategy)
    }
}

/// Configure advanced security
#[no_mangle]
pub extern "C" fn wasmtime4j_configure_advanced_security(
    handle: *mut c_void,
    security_level: i32,
    enable_sandboxing: i32,
    enable_resource_limits: i32,
    max_memory_mb: i32,
) {
    match configure_advanced_security_impl(handle, security_level, enable_sandboxing,
                                          enable_resource_limits, max_memory_mb) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to configure advanced security: {:?}", e);
        }
    }
}

fn configure_advanced_security_impl(
    handle: *mut c_void,
    security_level: i32,
    enable_sandboxing: i32,
    enable_resource_limits: i32,
    max_memory_mb: i32,
) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    unsafe {
        adv_core::configure_advanced_security(
            handle,
            security_level,
            enable_sandboxing,
            enable_resource_limits,
            max_memory_mb as u64,
        )
    }
}

/// Configure advanced profiling
#[no_mangle]
pub extern "C" fn wasmtime4j_configure_advanced_profiling(
    handle: *mut c_void,
    enable_perf_counters: i32,
    enable_tracing: i32,
    granularity: i32,
    sampling_interval: u64,
) {
    match configure_advanced_profiling_impl(handle, enable_perf_counters, enable_tracing,
                                           granularity, sampling_interval) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to configure advanced profiling: {:?}", e);
        }
    }
}

fn configure_advanced_profiling_impl(
    handle: *mut c_void,
    enable_perf_counters: i32,
    enable_tracing: i32,
    granularity: i32,
    sampling_interval: u64,
) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    unsafe {
        adv_core::configure_advanced_profiling(
            handle,
            enable_perf_counters,
            enable_tracing,
            granularity,
            sampling_interval,
        )
    }
}

/// Start profiling session
#[no_mangle]
pub extern "C" fn wasmtime4j_start_profiling(handle: *const c_void) {
    match start_profiling_impl(handle) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to start profiling: {:?}", e);
        }
    }
}

fn start_profiling_impl(handle: *const c_void) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    unsafe {
        adv_core::start_advanced_profiling(handle)
    }
}

/// Stop profiling session
#[no_mangle]
pub extern "C" fn wasmtime4j_stop_profiling(handle: *const c_void) {
    match stop_profiling_impl(handle) {
        Ok(()) => {}
        Err(e) => {
            log::error!("Failed to stop profiling: {:?}", e);
        }
    }
}

fn stop_profiling_impl(handle: *const c_void) -> WasmtimeResult<()> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    unsafe {
        adv_core::stop_advanced_profiling(handle)
    }
}

/// Get profiling results
#[no_mangle]
pub extern "C" fn wasmtime4j_get_profiling_results(handle: *const c_void) -> *const c_char {
    match get_profiling_results_impl(handle) {
        Ok(ptr) => ptr,
        Err(e) => {
            log::error!("Failed to get profiling results: {:?}", e);
            ptr::null()
        }
    }
}

fn get_profiling_results_impl(handle: *const c_void) -> WasmtimeResult<*const c_char> {
    validate_ptr_not_null!(handle, "experimental_features_handle");

    // For now, return a placeholder profiling results string
    let results = format!(
        "{{\"cpu_cycles\": 1234567, \"instructions_retired\": 987654, \"cache_misses\": 1024, \"session_id\": {:p}}}",
        handle
    );

    let c_results = CString::new(results).map_err(|e| WasmtimeError::InvalidParameter {
        message: format!("Failed to create result string: {}", e),
    })?;

    // Leak the string so it remains valid for the caller
    // The caller is responsible for freeing this memory
    Ok(c_results.into_raw())
}

/// Destroy experimental features
#[no_mangle]
pub extern "C" fn wasmtime4j_destroy_experimental_features(handle: *mut c_void) {
    if handle.is_null() {
        return;
    }

    unsafe {
        exp_core::destroy_experimental_features_config(handle);
    }

    log::info!("Destroyed experimental features instance: {:p}", handle);
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_experimental_features_lifecycle() {
        // Basic lifecycle test
        let handle = wasmtime4j_create_experimental_features();
        assert!(!handle.is_null());

        // Test feature enablement
        let feature_key = CString::new("stack-switching").unwrap();
        wasmtime4j_enable_experimental_feature(handle, feature_key.as_ptr());

        // Test configuration
        wasmtime4j_configure_stack_switching(handle, 1024 * 1024, 100, 0);

        // Test profiling
        wasmtime4j_start_profiling(handle);
        let results = wasmtime4j_get_profiling_results(handle);
        assert!(!results.is_null());

        // Free the results string
        unsafe {
            let _ = CString::from_raw(results as *mut c_char);
        }

        wasmtime4j_stop_profiling(handle);

        // Clean up
        wasmtime4j_destroy_experimental_features(handle);
    }

    #[test]
    fn test_null_handle_safety() {
        // Test that null handles are handled safely
        wasmtime4j_enable_experimental_feature(ptr::null_mut(), ptr::null());
        wasmtime4j_disable_experimental_feature(ptr::null_mut(), ptr::null());
        wasmtime4j_configure_stack_switching(ptr::null_mut(), 1024, 100, 0);
        wasmtime4j_start_profiling(ptr::null());
        wasmtime4j_stop_profiling(ptr::null());

        let results = wasmtime4j_get_profiling_results(ptr::null());
        assert!(results.is_null());

        wasmtime4j_destroy_experimental_features(ptr::null_mut());
    }
}