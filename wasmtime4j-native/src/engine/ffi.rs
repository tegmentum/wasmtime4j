//! Native C exports for JNI and Panama FFI consumption
//!
//! This module provides FFI-safe C functions for engine management.

use std::os::raw::{c_int, c_void};

use super::pool::{
    acquire_pooled_engine, engine_pool_cleanup, engine_pool_max_size, engine_pool_size,
    get_shared_engine, release_pooled_engine, wasmtime_full_cleanup,
};
use super::{core, Engine};
use crate::shared_ffi::{FfiWasmFeature, FFI_ERROR, FFI_SUCCESS};

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
            let wasm_feature = match FfiWasmFeature::from_ffi(feature) {
                Ok(f) => f,
                Err(_) => return FFI_ERROR,
            };
            if core::check_feature_support(engine, wasm_feature) {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
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
        Ok(engine) => {
            if core::is_fuel_enabled(engine) {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Check if epoch-based interruption is enabled
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_epoch_interruption_enabled(
    engine_ptr: *const c_void,
) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            if core::is_epoch_interruption_enabled(engine) {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
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
pub unsafe extern "C" fn wasmtime4j_engine_coredump_on_trap_enabled(
    engine_ptr: *const c_void,
) -> c_int {
    match core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            if core::is_coredump_on_trap_enabled(engine) {
                1
            } else {
                0
            }
        }
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
                    0 // Success
                }
                Err(_) => FFI_ERROR,
            }
        }
        Err(_) => FFI_ERROR,
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
    engine.config_summary().memory_reservation.unwrap_or(0)
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
    engine.config_summary().memory_guard_size.unwrap_or(0)
}

/// Get the configured memory reservation for growth for an engine
///
/// Returns the memory reservation for growth in bytes, or 0 if not configured or on error.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_memory_reservation_for_growth(
    engine_ptr: *const c_void,
) -> u64 {
    if engine_ptr.is_null() {
        return 0;
    }
    let engine = &*(engine_ptr as *const Engine);
    engine
        .config_summary()
        .memory_reservation_for_growth
        .unwrap_or(0)
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
    if engine.config_summary().wmemcheck_enabled {
        1
    } else {
        0
    }
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
pub unsafe extern "C" fn wasmtime4j_engine_table_lazy_init_enabled(
    engine_ptr: *const c_void,
) -> c_int {
    if engine_ptr.is_null() {
        return FFI_ERROR;
    }
    let engine = &*(engine_ptr as *const Engine);
    if engine.config_summary().table_lazy_init {
        1
    } else {
        0
    }
}

// =============================================================================
// Engine Pooling and Cleanup FFI Exports
// =============================================================================

/// Get the shared singleton engine.
///
/// Returns a pointer to the shared engine that is reused across all calls.
/// This is the recommended way to get an engine for most use cases, as it
/// avoids creating new engines which can trigger wasmtime's GLOBAL_CODE
/// registry issues.
///
/// The returned engine is managed by the library and should NOT be freed
/// with wasmtime4j_engine_destroy. It remains valid for the lifetime of
/// the process.
///
/// # Safety
///
/// The returned pointer is valid for the lifetime of the process.
/// Wrapper to allow *const c_void in OnceLock (raw pointers are not Send+Sync)
struct SharedEnginePtr(*const c_void);
unsafe impl Send for SharedEnginePtr {}
unsafe impl Sync for SharedEnginePtr {}

#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_get_shared() -> *const c_void {
    static SHARED_ENGINE_PTR: std::sync::OnceLock<SharedEnginePtr> = std::sync::OnceLock::new();
    SHARED_ENGINE_PTR
        .get_or_init(|| {
            let engine = get_shared_engine();
            SharedEnginePtr(Box::into_raw(Box::new(engine)) as *const c_void)
        })
        .0
}

/// Acquire an engine from the pool.
///
/// Returns an engine from the pool if available, or creates a new one.
/// The returned engine should be released with wasmtime4j_engine_release_pooled
/// when no longer needed, NOT with wasmtime4j_engine_destroy.
///
/// # Safety
///
/// Returns pointer to engine that must be released with wasmtime4j_engine_release_pooled
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_acquire_pooled() -> *mut c_void {
    let engine = acquire_pooled_engine();
    Box::into_raw(Box::new(engine)) as *mut c_void
}

/// Release an engine back to the pool.
///
/// Returns the engine to the pool for reuse by other callers.
/// If the pool is full, the engine is dropped.
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_acquire_pooled
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_release_pooled(engine_ptr: *mut c_void) {
    if engine_ptr.is_null() {
        return;
    }
    let engine = Box::from_raw(engine_ptr as *mut Engine);
    release_pooled_engine(*engine);
}

/// Get the current number of engines in the pool.
///
/// # Safety
///
/// This function is always safe to call.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_pool_size() -> usize {
    engine_pool_size()
}

/// Get the maximum capacity of the engine pool.
///
/// # Safety
///
/// This function is always safe to call.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_pool_max_size() -> usize {
    engine_pool_max_size()
}

/// Clear the engine pool, dropping all pooled engines.
///
/// This can be called to force cleanup of pooled engines.
/// Note: This does NOT affect the shared singleton engine.
///
/// # Safety
///
/// This function is always safe to call.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_engine_pool_cleanup() {
    engine_pool_cleanup();
}

/// Perform a full wasmtime cleanup.
///
/// This function:
/// 1. Clears the engine pool
/// 2. Yields to allow pending deallocations
/// 3. Sleeps briefly to allow OS to reclaim memory
///
/// Call this periodically during long-running processes or after running
/// many operations to help mitigate wasmtime's GLOBAL_CODE registry issues.
///
/// Note: This does NOT affect the shared singleton engine.
///
/// # Safety
///
/// This function is always safe to call.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_full_cleanup() {
    wasmtime_full_cleanup();
}
