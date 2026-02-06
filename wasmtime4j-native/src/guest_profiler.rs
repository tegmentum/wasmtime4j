//! WebAssembly Guest Profiler Support
//!
//! This module provides FFI functions for working with WebAssembly guest profiling.
//! The GuestProfiler collects basic profiling data for WebAssembly guests in a
//! cross-platform manner, outputting profiles in Firefox processed profile format.
//!
//! The profiler requires the `profiling` feature to be enabled in wasmtime.
//!
//! # Usage
//!
//! 1. Create a profiler with `wasmtime4j_guest_profiler_create()`
//! 2. Call `wasmtime4j_guest_profiler_sample()` at regular intervals during guest execution
//! 3. When finished, call `wasmtime4j_guest_profiler_finish()` to get the profile output
//! 4. Free the profiler with `wasmtime4j_guest_profiler_free()`
//!
//! The output can be visualized at https://profiler.firefox.com/

use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_long, c_void, CStr, CString};
use std::io::Cursor;
use std::ptr;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, RwLock};
use std::time::{Duration, Instant};

use wasmtime::GuestProfiler;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::module::Module;
use crate::store::Store;

// Keep Engine import for FFI pointer type casting validation
#[allow(unused_imports)]
use crate::engine::Engine;

/// Configuration for creating a guest profiler
#[derive(Debug, Clone)]
pub struct ProfilerConfig {
    /// Name for the profiler (used in output)
    pub name: String,
    /// Sample interval hint in microseconds
    pub interval_us: u64,
    /// Module names to include in profiling (only these appear in stack traces)
    pub module_names: Vec<String>,
}

impl Default for ProfilerConfig {
    fn default() -> Self {
        ProfilerConfig {
            name: "wasmtime4j-profile".to_string(),
            interval_us: 1000, // 1ms default
            module_names: Vec::new(),
        }
    }
}

/// Stored profiler with associated metadata
pub struct StoredProfiler {
    /// The underlying wasmtime profiler
    profiler: Option<GuestProfiler>,
    /// Configuration used to create this profiler
    config: ProfilerConfig,
    /// Start time for profiling
    start_time: Instant,
    /// Number of samples collected
    sample_count: u64,
    /// Total CPU time recorded
    total_cpu_time: Duration,
    /// Whether profiling is active
    is_active: bool,
}

impl StoredProfiler {
    /// Create a new stored profiler
    pub fn new(profiler: GuestProfiler, config: ProfilerConfig) -> Self {
        StoredProfiler {
            profiler: Some(profiler),
            config,
            start_time: Instant::now(),
            sample_count: 0,
            total_cpu_time: Duration::ZERO,
            is_active: true,
        }
    }

    /// Get the profiler configuration
    pub fn config(&self) -> &ProfilerConfig {
        &self.config
    }

    /// Get the number of samples collected
    pub fn sample_count(&self) -> u64 {
        self.sample_count
    }

    /// Get total CPU time recorded
    pub fn total_cpu_time(&self) -> Duration {
        self.total_cpu_time
    }

    /// Get elapsed wall clock time since profiling started
    pub fn elapsed(&self) -> Duration {
        self.start_time.elapsed()
    }

    /// Check if profiling is active
    pub fn is_active(&self) -> bool {
        self.is_active
    }

    /// Record a sample
    pub fn record_sample(&mut self, delta: Duration) {
        self.sample_count += 1;
        self.total_cpu_time += delta;
    }

    /// Stop the profiler and get the underlying GuestProfiler for finishing
    pub fn stop(&mut self) -> Option<GuestProfiler> {
        self.is_active = false;
        self.profiler.take()
    }
}

// Global registry for profilers
lazy_static::lazy_static! {
    static ref PROFILER_REGISTRY: RwLock<HashMap<u64, Arc<RwLock<StoredProfiler>>>> = RwLock::new(HashMap::new());
    static ref NEXT_PROFILER_ID: AtomicU64 = AtomicU64::new(1);
}

/// Register a profiler and return its ID
pub fn register_profiler(profiler: StoredProfiler) -> WasmtimeResult<u64> {
    let id = NEXT_PROFILER_ID.fetch_add(1, Ordering::SeqCst);
    let mut registry = PROFILER_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire profiler registry lock".to_string(),
    })?;
    registry.insert(id, Arc::new(RwLock::new(profiler)));
    Ok(id)
}

/// Get a profiler by ID
pub fn get_profiler(id: u64) -> WasmtimeResult<Arc<RwLock<StoredProfiler>>> {
    let registry = PROFILER_REGISTRY.read().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire profiler registry lock".to_string(),
    })?;
    registry.get(&id).cloned().ok_or_else(|| WasmtimeError::GuestProfiler {
        message: format!("Profiler with ID {} not found", id),
    })
}

/// Remove a profiler from the registry
pub fn unregister_profiler(id: u64) -> WasmtimeResult<()> {
    let mut registry = PROFILER_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire profiler registry lock".to_string(),
    })?;
    registry.remove(&id);
    Ok(())
}

// ============================================================================
// Panama FFI Functions
// ============================================================================

/// Create a new guest profiler with default configuration
///
/// # Arguments
/// * `engine_ptr` - Pointer to the Engine
/// * `name` - C string name for the profiler
/// * `interval_us` - Sample interval hint in microseconds
///
/// # Returns
/// * Profiler ID on success, -1 on error
///
/// # Safety
/// This function is unsafe because it dereferences raw pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_create(
    engine_ptr: *mut c_void,
    name: *const c_char,
    interval_us: c_long,
) -> c_long {
    if engine_ptr.is_null() || name.is_null() {
        return -1;
    }

    let name_str = match CStr::from_ptr(name).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -2,
    };

    let engine = &*(engine_ptr as *const Engine);

    let interval = Duration::from_micros(interval_us as u64);

    // Create profiler with no modules initially (can be added via sample calls)
    // Note: wasmtime 40.0.1 requires Engine reference and returns Result
    let profiler = match GuestProfiler::new(
        engine.inner(),
        &name_str,
        interval,
        Vec::<(String, wasmtime::Module)>::new(),
    ) {
        Ok(p) => p,
        Err(_) => return -3,
    };

    let config = ProfilerConfig {
        name: name_str,
        interval_us: interval_us as u64,
        module_names: Vec::new(),
    };

    let stored = StoredProfiler::new(profiler, config);

    match register_profiler(stored) {
        Ok(id) => id as c_long,
        Err(_) => -4,
    }
}

/// Create a guest profiler with a specific module
///
/// # Arguments
/// * `engine_ptr` - Pointer to the Engine
/// * `module_ptr` - Pointer to the Module to profile
/// * `name` - C string name for the profiler
/// * `interval_us` - Sample interval hint in microseconds
///
/// # Returns
/// * Profiler ID on success, -1 on error
///
/// # Safety
/// This function is unsafe because it dereferences raw pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_create_with_module(
    engine_ptr: *mut c_void,
    module_ptr: *mut c_void,
    name: *const c_char,
    interval_us: c_long,
) -> c_long {
    if engine_ptr.is_null() || module_ptr.is_null() || name.is_null() {
        return -1;
    }

    let name_str = match CStr::from_ptr(name).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return -2,
    };

    let engine = &*(engine_ptr as *const Engine);
    let module = &*(module_ptr as *const Module);

    let interval = Duration::from_micros(interval_us as u64);

    // Get module name from metadata
    let module_name = module.metadata.name.clone().unwrap_or_else(|| "unknown".to_string());

    // Create profiler with the module - need to clone the inner wasmtime::Module
    // Note: wasmtime 40.0.1 requires Engine reference and returns Result
    let profiler = match GuestProfiler::new(
        engine.inner(),
        &name_str,
        interval,
        vec![(module_name.clone(), module.inner().clone())],
    ) {
        Ok(p) => p,
        Err(_) => return -3,
    };

    let config = ProfilerConfig {
        name: name_str,
        interval_us: interval_us as u64,
        module_names: vec![module_name],
    };

    let stored = StoredProfiler::new(profiler, config);

    match register_profiler(stored) {
        Ok(id) => id as c_long,
        Err(_) => -4,
    }
}

/// Free a profiler from the registry
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_free(profiler_id: u64) -> c_int {
    match unregister_profiler(profiler_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Record a profiling sample
///
/// This should be called at regular intervals while the guest is executing.
/// The most straightforward way is from an epoch deadline callback.
///
/// # Arguments
/// * `profiler_id` - The profiler ID
/// * `store_ptr` - Pointer to the Store (for stack trace collection)
/// * `delta_us` - CPU time since previous sample in microseconds
///
/// # Returns
/// * 0 on success, negative on error
///
/// # Safety
/// This function is unsafe because it dereferences raw pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_sample(
    profiler_id: u64,
    store_ptr: *mut c_void,
    delta_us: c_long,
) -> c_int {
    if store_ptr.is_null() {
        return -1;
    }

    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -2,
    };

    let mut profiler_guard = match profiler_arc.write() {
        Ok(g) => g,
        Err(_) => return -3,
    };

    if !profiler_guard.is_active() {
        return -4;
    }

    let store = &*(store_ptr as *const Store);
    let delta = Duration::from_micros(delta_us as u64);

    // Record the sample in our tracking
    profiler_guard.record_sample(delta);

    // Call the underlying profiler's sample method
    if let Some(ref mut profiler) = profiler_guard.profiler {
        let store_guard = store.inner.lock();
        profiler.sample(&*store_guard, delta);
    }

    0
}

/// Get profiler statistics as JSON
///
/// # Returns
/// * Newly allocated C string with JSON stats, or null on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_stats_json(profiler_id: u64) -> *mut c_char {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return ptr::null_mut(),
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return ptr::null_mut(),
    };

    let json = serde_json::json!({
        "name": profiler_guard.config.name,
        "interval_us": profiler_guard.config.interval_us,
        "sample_count": profiler_guard.sample_count,
        "total_cpu_time_us": profiler_guard.total_cpu_time.as_micros() as u64,
        "elapsed_time_us": profiler_guard.elapsed().as_micros() as u64,
        "is_active": profiler_guard.is_active,
        "module_names": profiler_guard.config.module_names,
    });

    match CString::new(json.to_string()) {
        Ok(cstr) => cstr.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

/// Finish profiling and write the profile to a buffer
///
/// This consumes the profiler and returns the profile data in Firefox
/// processed profile format (JSON).
///
/// # Arguments
/// * `profiler_id` - The profiler ID
/// * `out_ptr` - Output pointer for the profile data bytes
/// * `out_len` - Output pointer for the length of profile data
///
/// # Returns
/// * 0 on success, negative on error
///
/// # Safety
/// This function is unsafe because it dereferences raw pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_finish(
    profiler_id: u64,
    out_ptr: *mut *mut u8,
    out_len: *mut c_long,
) -> c_int {
    if out_ptr.is_null() || out_len.is_null() {
        return -1;
    }

    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -2,
    };

    let mut profiler_guard = match profiler_arc.write() {
        Ok(g) => g,
        Err(_) => return -3,
    };

    // Take the underlying profiler
    let profiler = match profiler_guard.stop() {
        Some(p) => p,
        None => return -4, // Already finished
    };

    // Write profile to buffer
    let mut buffer = Cursor::new(Vec::new());
    if let Err(e) = profiler.finish(&mut buffer) {
        log::error!("Failed to finish profiler: {}", e);
        return -5;
    }

    let bytes = buffer.into_inner();
    let len = bytes.len();
    let boxed = bytes.into_boxed_slice();
    let ptr = Box::into_raw(boxed) as *mut u8;

    *out_ptr = ptr;
    *out_len = len as c_long;

    // Remove from registry
    let _ = unregister_profiler(profiler_id);

    0
}

/// Free bytes allocated by wasmtime4j_profiler_finish
///
/// # Safety
/// This function is unsafe because it dereferences the pointer
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_bytes_free(ptr: *mut u8, len: c_long) {
    if !ptr.is_null() && len > 0 {
        let slice = std::slice::from_raw_parts_mut(ptr, len as usize);
        drop(Box::from_raw(slice as *mut [u8]));
    }
}

/// Free a C string allocated by profiler functions
///
/// # Safety
/// This function is unsafe because it dereferences the pointer
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_string_free(s: *mut c_char) {
    if !s.is_null() {
        drop(CString::from_raw(s));
    }
}

/// Check if a profiler is active
///
/// # Returns
/// * 1 if active, 0 if not active, -1 on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_is_active(profiler_id: u64) -> c_int {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    if profiler_guard.is_active() { 1 } else { 0 }
}

/// Get the sample count for a profiler
///
/// # Returns
/// * Sample count on success, -1 on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_sample_count(profiler_id: u64) -> c_long {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    profiler_guard.sample_count() as c_long
}

/// Get elapsed time since profiler creation in microseconds
///
/// # Returns
/// * Elapsed time in microseconds on success, -1 on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_elapsed_us(profiler_id: u64) -> c_long {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    profiler_guard.elapsed().as_micros() as c_long
}

/// Get total CPU time recorded in microseconds
///
/// # Returns
/// * Total CPU time in microseconds on success, -1 on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_cpu_time_us(profiler_id: u64) -> c_long {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    profiler_guard.total_cpu_time().as_micros() as c_long
}

/// Get the number of registered profilers
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_count() -> c_int {
    match PROFILER_REGISTRY.read() {
        Ok(registry) => registry.len() as c_int,
        Err(_) => -1,
    }
}

/// Get all registered profiler IDs as a JSON array
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_all_ids() -> *mut c_char {
    match PROFILER_REGISTRY.read() {
        Ok(registry) => {
            let ids: Vec<u64> = registry.keys().cloned().collect();
            match CString::new(serde_json::to_string(&ids).unwrap_or_default()) {
                Ok(cstr) => cstr.into_raw(),
                Err(_) => ptr::null_mut(),
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Clear all registered profilers
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_clear_all() -> c_int {
    match PROFILER_REGISTRY.write() {
        Ok(mut registry) => {
            registry.clear();
            0
        }
        Err(_) => -1,
    }
}

/// Stop profiling without getting the output
///
/// This stops the profiler but doesn't write the profile.
/// The profiler can then be freed normally.
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_stop(profiler_id: u64) -> c_int {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    let mut profiler_guard = match profiler_arc.write() {
        Ok(g) => g,
        Err(_) => return -2,
    };

    profiler_guard.stop();
    0
}

/// Get the profiler name
///
/// # Returns
/// * Newly allocated C string with the name, or null on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_name(profiler_id: u64) -> *mut c_char {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return ptr::null_mut(),
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return ptr::null_mut(),
    };

    match CString::new(profiler_guard.config.name.clone()) {
        Ok(cstr) => cstr.into_raw(),
        Err(_) => ptr::null_mut(),
    }
}

/// Get the configured sample interval in microseconds
///
/// # Returns
/// * Sample interval in microseconds on success, -1 on error
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_guest_profiler_get_interval_us(profiler_id: u64) -> c_long {
    let profiler_arc = match get_profiler(profiler_id) {
        Ok(p) => p,
        Err(_) => return -1,
    };

    let profiler_guard = match profiler_arc.read() {
        Ok(g) => g,
        Err(_) => return -1,
    };

    profiler_guard.config.interval_us as c_long
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_profiler_config_default() {
        let config = ProfilerConfig::default();
        assert_eq!(config.name, "wasmtime4j-profile");
        assert_eq!(config.interval_us, 1000);
        assert!(config.module_names.is_empty());
    }

    #[test]
    fn test_profiler_registry() {
        // Test that registry starts or clears properly
        unsafe {
            wasmtime4j_guest_profiler_clear_all();
            let count = wasmtime4j_guest_profiler_get_count();
            assert_eq!(count, 0);
        }
    }
}
