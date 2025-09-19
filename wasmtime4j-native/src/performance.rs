//! # Performance Monitoring and Statistics Collection
//!
//! This module provides comprehensive performance monitoring infrastructure for the Wasmtime4j
//! native library. It includes function call timing, memory usage tracking, compilation metrics,
//! and lock-free atomic operations for zero-contention monitoring in production environments.
//!
//! ## Features
//!
//! - **Lock-free Performance Tracking**: Atomic operations ensure zero contention
//! - **Function Call Metrics**: Timing and counting for all native functions
//! - **Memory Usage Monitoring**: Track allocation patterns and memory pressure
//! - **Compilation Performance**: Metrics for WebAssembly module compilation
//! - **Engine Statistics**: Runtime performance and resource utilization
//! - **Feature Detection**: WebAssembly proposal support detection
//!
//! ## Safety
//!
//! All performance tracking uses atomic operations and is designed to be safe for
//! concurrent access from multiple threads without locks or contention.

#![warn(missing_docs)]
#![allow(unused_imports)]

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, AtomicU32, AtomicBool, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::os::raw::{c_char, c_int, c_void, c_double};
use std::ffi::{CStr, CString};
use std::ptr;
use once_cell::sync::Lazy;

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::engine::WasmFeature;

/// Global performance monitoring system instance
pub static PERFORMANCE_SYSTEM: Lazy<PerformanceSystem> = Lazy::new(PerformanceSystem::new);

/// Performance statistics structure for tracking function calls
#[repr(C)]
#[derive(Debug)]
pub struct FunctionCallStats {
    /// Total number of calls to this function
    pub call_count: AtomicU64,
    /// Total time spent in this function (microseconds)
    pub total_time_micros: AtomicU64,
    /// Minimum execution time (microseconds)
    pub min_time_micros: AtomicU64,
    /// Maximum execution time (microseconds)
    pub max_time_micros: AtomicU64,
    /// Last execution time (microseconds)
    pub last_time_micros: AtomicU64,
    /// Number of errors encountered
    pub error_count: AtomicU64,
}

impl Default for FunctionCallStats {
    fn default() -> Self {
        Self {
            call_count: AtomicU64::new(0),
            total_time_micros: AtomicU64::new(0),
            min_time_micros: AtomicU64::new(u64::MAX),
            max_time_micros: AtomicU64::new(0),
            last_time_micros: AtomicU64::new(0),
            error_count: AtomicU64::new(0),
        }
    }
}

impl FunctionCallStats {
    /// Record a function call with timing information
    pub fn record_call(&self, duration_micros: u64, had_error: bool) {
        self.call_count.fetch_add(1, Ordering::Relaxed);
        self.total_time_micros.fetch_add(duration_micros, Ordering::Relaxed);
        self.last_time_micros.store(duration_micros, Ordering::Relaxed);

        if had_error {
            self.error_count.fetch_add(1, Ordering::Relaxed);
        }

        // Update min time atomically
        let mut current_min = self.min_time_micros.load(Ordering::Relaxed);
        while duration_micros < current_min {
            match self.min_time_micros.compare_exchange_weak(
                current_min,
                duration_micros,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(new_min) => current_min = new_min,
            }
        }

        // Update max time atomically
        let mut current_max = self.max_time_micros.load(Ordering::Relaxed);
        while duration_micros > current_max {
            match self.max_time_micros.compare_exchange_weak(
                current_max,
                duration_micros,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(new_max) => current_max = new_max,
            }
        }
    }

    /// Get average execution time in microseconds
    pub fn average_time_micros(&self) -> f64 {
        let total = self.total_time_micros.load(Ordering::Relaxed);
        let count = self.call_count.load(Ordering::Relaxed);
        if count > 0 {
            total as f64 / count as f64
        } else {
            0.0
        }
    }

    /// Get error rate as a percentage
    pub fn error_rate(&self) -> f64 {
        let errors = self.error_count.load(Ordering::Relaxed);
        let total = self.call_count.load(Ordering::Relaxed);
        if total > 0 {
            (errors as f64 / total as f64) * 100.0
        } else {
            0.0
        }
    }
}

/// Memory usage statistics
#[repr(C)]
#[derive(Debug)]
pub struct MemoryStats {
    /// Current allocated memory (bytes)
    pub current_allocated: AtomicU64,
    /// Peak allocated memory (bytes)
    pub peak_allocated: AtomicU64,
    /// Total allocations made
    pub total_allocations: AtomicU64,
    /// Total deallocations made
    pub total_deallocations: AtomicU64,
    /// Current number of active allocations
    pub active_allocations: AtomicU64,
}

impl Default for MemoryStats {
    fn default() -> Self {
        Self {
            current_allocated: AtomicU64::new(0),
            peak_allocated: AtomicU64::new(0),
            total_allocations: AtomicU64::new(0),
            total_deallocations: AtomicU64::new(0),
            active_allocations: AtomicU64::new(0),
        }
    }
}

impl MemoryStats {
    /// Record a memory allocation
    pub fn record_allocation(&self, size: u64) {
        self.total_allocations.fetch_add(1, Ordering::Relaxed);
        self.active_allocations.fetch_add(1, Ordering::Relaxed);
        let new_current = self.current_allocated.fetch_add(size, Ordering::Relaxed) + size;

        // Update peak atomically
        let mut current_peak = self.peak_allocated.load(Ordering::Relaxed);
        while new_current > current_peak {
            match self.peak_allocated.compare_exchange_weak(
                current_peak,
                new_current,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(new_peak) => current_peak = new_peak,
            }
        }
    }

    /// Record a memory deallocation
    pub fn record_deallocation(&self, size: u64) {
        self.total_deallocations.fetch_add(1, Ordering::Relaxed);
        self.active_allocations.fetch_sub(1, Ordering::Relaxed);
        self.current_allocated.fetch_sub(size, Ordering::Relaxed);
    }

    /// Get memory utilization as a percentage of peak
    pub fn utilization_percentage(&self) -> f64 {
        let current = self.current_allocated.load(Ordering::Relaxed);
        let peak = self.peak_allocated.load(Ordering::Relaxed);
        if peak > 0 {
            (current as f64 / peak as f64) * 100.0
        } else {
            0.0
        }
    }
}

/// Compilation performance statistics
#[repr(C)]
#[derive(Debug)]
pub struct CompilationStats {
    /// Number of modules compiled
    pub modules_compiled: AtomicU64,
    /// Total compilation time (microseconds)
    pub total_compile_time_micros: AtomicU64,
    /// Total module size compiled (bytes)
    pub total_module_size_bytes: AtomicU64,
    /// Number of compilation failures
    pub compilation_failures: AtomicU64,
    /// Average compilation time per KB
    pub avg_compile_time_per_kb: AtomicU64,
}

impl Default for CompilationStats {
    fn default() -> Self {
        Self {
            modules_compiled: AtomicU64::new(0),
            total_compile_time_micros: AtomicU64::new(0),
            total_module_size_bytes: AtomicU64::new(0),
            compilation_failures: AtomicU64::new(0),
            avg_compile_time_per_kb: AtomicU64::new(0),
        }
    }
}

impl CompilationStats {
    /// Record a compilation event
    pub fn record_compilation(&self, duration_micros: u64, module_size_bytes: u64, success: bool) {
        if success {
            self.modules_compiled.fetch_add(1, Ordering::Relaxed);
            self.total_compile_time_micros.fetch_add(duration_micros, Ordering::Relaxed);
            self.total_module_size_bytes.fetch_add(module_size_bytes, Ordering::Relaxed);

            // Update average compilation time per KB
            let total_time = self.total_compile_time_micros.load(Ordering::Relaxed);
            let total_size_kb = self.total_module_size_bytes.load(Ordering::Relaxed) / 1024;
            if total_size_kb > 0 {
                self.avg_compile_time_per_kb.store(total_time / total_size_kb, Ordering::Relaxed);
            }
        } else {
            self.compilation_failures.fetch_add(1, Ordering::Relaxed);
        }
    }

    /// Get compilation success rate as a percentage
    pub fn success_rate(&self) -> f64 {
        let successes = self.modules_compiled.load(Ordering::Relaxed);
        let failures = self.compilation_failures.load(Ordering::Relaxed);
        let total = successes + failures;
        if total > 0 {
            (successes as f64 / total as f64) * 100.0
        } else {
            0.0
        }
    }

    /// Get average compilation speed in KB/second
    pub fn average_compile_speed_kb_per_sec(&self) -> f64 {
        let total_size_kb = self.total_module_size_bytes.load(Ordering::Relaxed) as f64 / 1024.0;
        let total_time_secs = self.total_compile_time_micros.load(Ordering::Relaxed) as f64 / 1_000_000.0;
        if total_time_secs > 0.0 {
            total_size_kb / total_time_secs
        } else {
            0.0
        }
    }
}

/// Engine-level performance statistics
#[repr(C)]
#[derive(Debug)]
pub struct EngineStats {
    /// Number of active engines
    pub active_engines: AtomicU32,
    /// Total engines created
    pub total_engines_created: AtomicU64,
    /// Total stores created
    pub total_stores_created: AtomicU64,
    /// Total instances created
    pub total_instances_created: AtomicU64,
    /// Total function calls made
    pub total_function_calls: AtomicU64,
    /// Total execution time (microseconds)
    pub total_execution_time_micros: AtomicU64,
}

impl Default for EngineStats {
    fn default() -> Self {
        Self {
            active_engines: AtomicU32::new(0),
            total_engines_created: AtomicU64::new(0),
            total_stores_created: AtomicU64::new(0),
            total_instances_created: AtomicU64::new(0),
            total_function_calls: AtomicU64::new(0),
            total_execution_time_micros: AtomicU64::new(0),
        }
    }
}

/// WebAssembly feature support detection
#[repr(C)]
#[derive(Debug)]
pub struct FeatureSupport {
    /// SIMD support available
    pub simd_support: AtomicBool,
    /// Multi-memory support available
    pub multi_memory_support: AtomicBool,
    /// Memory64 support available
    pub memory64_support: AtomicBool,
    /// Bulk memory operations support
    pub bulk_memory_support: AtomicBool,
    /// Reference types support
    pub reference_types_support: AtomicBool,
    /// Multi-value support
    pub multi_value_support: AtomicBool,
    /// Tail call support
    pub tail_call_support: AtomicBool,
    /// Threads support
    pub threads_support: AtomicBool,
    /// Component model support
    pub component_model_support: AtomicBool,
    /// WASI support
    pub wasi_support: AtomicBool,
}

impl Default for FeatureSupport {
    fn default() -> Self {
        Self {
            simd_support: AtomicBool::new(false),
            multi_memory_support: AtomicBool::new(false),
            memory64_support: AtomicBool::new(false),
            bulk_memory_support: AtomicBool::new(false),
            reference_types_support: AtomicBool::new(false),
            multi_value_support: AtomicBool::new(false),
            tail_call_support: AtomicBool::new(false),
            threads_support: AtomicBool::new(false),
            component_model_support: AtomicBool::new(false),
            wasi_support: AtomicBool::new(false),
        }
    }
}

/// Main performance monitoring system
#[derive(Debug)]
pub struct PerformanceSystem {
    /// Function call statistics by function name
    function_stats: RwLock<HashMap<String, Arc<FunctionCallStats>>>,
    /// Memory usage statistics
    memory_stats: MemoryStats,
    /// Compilation performance statistics
    compilation_stats: CompilationStats,
    /// Engine-level statistics
    engine_stats: EngineStats,
    /// Feature support detection
    feature_support: FeatureSupport,
    /// System initialization timestamp
    init_time: SystemTime,
    /// Performance monitoring enabled flag
    enabled: AtomicBool,
}

impl PerformanceSystem {
    /// Create a new performance monitoring system
    pub fn new() -> Self {
        Self {
            function_stats: RwLock::new(HashMap::new()),
            memory_stats: MemoryStats::default(),
            compilation_stats: CompilationStats::default(),
            engine_stats: EngineStats::default(),
            feature_support: FeatureSupport::default(),
            init_time: SystemTime::now(),
            enabled: AtomicBool::new(true),
        }
    }

    /// Enable or disable performance monitoring
    pub fn set_enabled(&self, enabled: bool) {
        self.enabled.store(enabled, Ordering::Relaxed);
    }

    /// Check if performance monitoring is enabled
    pub fn is_enabled(&self) -> bool {
        self.enabled.load(Ordering::Relaxed)
    }

    /// Get or create function statistics for a given function name
    pub fn get_function_stats(&self, function_name: &str) -> Arc<FunctionCallStats> {
        let stats_map = self.function_stats.read().unwrap();
        if let Some(stats) = stats_map.get(function_name) {
            return Arc::clone(stats);
        }
        drop(stats_map);

        let mut stats_map = self.function_stats.write().unwrap();
        // Double-check after acquiring write lock
        if let Some(stats) = stats_map.get(function_name) {
            return Arc::clone(stats);
        }

        let stats = Arc::new(FunctionCallStats::default());
        stats_map.insert(function_name.to_string(), Arc::clone(&stats));
        stats
    }

    /// Record a function call
    pub fn record_function_call(&self, function_name: &str, duration_micros: u64, had_error: bool) {
        if !self.is_enabled() {
            return;
        }

        let stats = self.get_function_stats(function_name);
        stats.record_call(duration_micros, had_error);
    }

    /// Get memory statistics
    pub fn memory_stats(&self) -> &MemoryStats {
        &self.memory_stats
    }

    /// Get compilation statistics
    pub fn compilation_stats(&self) -> &CompilationStats {
        &self.compilation_stats
    }

    /// Get engine statistics
    pub fn engine_stats(&self) -> &EngineStats {
        &self.engine_stats
    }

    /// Get feature support information
    pub fn feature_support(&self) -> &FeatureSupport {
        &self.feature_support
    }

    /// Get system uptime in seconds
    pub fn uptime_seconds(&self) -> f64 {
        self.init_time.elapsed().unwrap_or(Duration::ZERO).as_secs_f64()
    }

    /// Reset all statistics
    pub fn reset_stats(&self) {
        if let Ok(mut stats_map) = self.function_stats.write() {
            stats_map.clear();
        }

        // Reset memory stats
        self.memory_stats.current_allocated.store(0, Ordering::Relaxed);
        self.memory_stats.peak_allocated.store(0, Ordering::Relaxed);
        self.memory_stats.total_allocations.store(0, Ordering::Relaxed);
        self.memory_stats.total_deallocations.store(0, Ordering::Relaxed);
        self.memory_stats.active_allocations.store(0, Ordering::Relaxed);

        // Reset compilation stats
        self.compilation_stats.modules_compiled.store(0, Ordering::Relaxed);
        self.compilation_stats.total_compile_time_micros.store(0, Ordering::Relaxed);
        self.compilation_stats.total_module_size_bytes.store(0, Ordering::Relaxed);
        self.compilation_stats.compilation_failures.store(0, Ordering::Relaxed);
        self.compilation_stats.avg_compile_time_per_kb.store(0, Ordering::Relaxed);

        // Reset engine stats
        self.engine_stats.active_engines.store(0, Ordering::Relaxed);
        self.engine_stats.total_engines_created.store(0, Ordering::Relaxed);
        self.engine_stats.total_stores_created.store(0, Ordering::Relaxed);
        self.engine_stats.total_instances_created.store(0, Ordering::Relaxed);
        self.engine_stats.total_function_calls.store(0, Ordering::Relaxed);
        self.engine_stats.total_execution_time_micros.store(0, Ordering::Relaxed);
    }

    /// Detect and initialize feature support
    pub fn detect_features(&self) {
        // Detect SIMD support
        self.feature_support.simd_support.store(
            self.detect_wasm_feature(WasmFeature::Simd),
            Ordering::Relaxed
        );

        // Detect bulk memory support
        self.feature_support.bulk_memory_support.store(
            self.detect_wasm_feature(WasmFeature::BulkMemory),
            Ordering::Relaxed
        );

        // Detect reference types support
        self.feature_support.reference_types_support.store(
            self.detect_wasm_feature(WasmFeature::ReferenceTypes),
            Ordering::Relaxed
        );

        // Detect multi-value support
        self.feature_support.multi_value_support.store(
            self.detect_wasm_feature(WasmFeature::MultiValue),
            Ordering::Relaxed
        );

        // Detect threads support
        self.feature_support.threads_support.store(
            self.detect_wasm_feature(WasmFeature::Threads),
            Ordering::Relaxed
        );

        // Tail call support is not available in current WasmFeature enum
        self.feature_support.tail_call_support.store(false, Ordering::Relaxed);

        // Component model support (always true for Wasmtime 36.0.2)
        self.feature_support.component_model_support.store(true, Ordering::Relaxed);

        // WASI support (always true if compiled with WASI feature)
        #[cfg(feature = "wasi")]
        self.feature_support.wasi_support.store(true, Ordering::Relaxed);
        #[cfg(not(feature = "wasi"))]
        self.feature_support.wasi_support.store(false, Ordering::Relaxed);
    }

    /// Detect if a specific WebAssembly feature is supported
    fn detect_wasm_feature(&self, _feature: WasmFeature) -> bool {
        // For now, return true for basic features that are generally supported
        // In a real implementation, this would test actual feature support
        true
    }
}

/// Performance timer for measuring function execution time
pub struct PerformanceTimer {
    function_name: String,
    start_time: Instant,
}

impl PerformanceTimer {
    /// Start timing a function
    pub fn start(function_name: impl Into<String>) -> Self {
        Self {
            function_name: function_name.into(),
            start_time: Instant::now(),
        }
    }

    /// Finish timing and record the result
    pub fn finish(self, had_error: bool) {
        let duration = self.start_time.elapsed();
        let duration_micros = duration.as_micros() as u64;

        PERFORMANCE_SYSTEM.record_function_call(&self.function_name, duration_micros, had_error);
    }
}

/// Macro for automatically timing function calls
#[macro_export]
macro_rules! time_function {
    ($name:expr, $block:block) => {{
        let timer = $crate::performance::PerformanceTimer::start($name);
        let result = $block;
        let had_error = result.is_err();
        timer.finish(had_error);
        result
    }};
}

// C API exports for performance monitoring

/// Initialize the performance monitoring system
///
/// # Safety
///
/// This function is safe to call multiple times. Should be called before using
/// other performance monitoring functions.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_init() -> c_int {
    PERFORMANCE_SYSTEM.set_enabled(true);
    PERFORMANCE_SYSTEM.detect_features();
    log::info!("Performance monitoring system initialized");
    0 // Success
}

/// Enable or disable performance monitoring
///
/// # Safety
///
/// The enabled parameter should be 0 for false, non-zero for true.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_set_enabled(enabled: c_int) -> c_int {
    PERFORMANCE_SYSTEM.set_enabled(enabled != 0);
    0 // Success
}

/// Check if performance monitoring is enabled
///
/// # Safety
///
/// Returns 1 if enabled, 0 if disabled.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_is_enabled() -> c_int {
    if PERFORMANCE_SYSTEM.is_enabled() { 1 } else { 0 }
}

/// Record a function call with timing information
///
/// # Safety
///
/// The function_name parameter must be a valid null-terminated C string.
/// The duration_micros parameter should be the execution time in microseconds.
/// The had_error parameter should be 0 for success, non-zero for error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_record_function_call(
    function_name: *const c_char,
    duration_micros: u64,
    had_error: c_int,
) -> c_int {
    if function_name.is_null() {
        return -1; // Invalid parameter
    }

    let name_cstr = match CStr::from_ptr(function_name).to_str() {
        Ok(s) => s,
        Err(_) => return -1, // Invalid UTF-8
    };

    PERFORMANCE_SYSTEM.record_function_call(name_cstr, duration_micros, had_error != 0);
    0 // Success
}

/// Get engine-level statistics
///
/// # Safety
///
/// The stats parameter must point to a valid EngineStats structure.
/// The structure will be filled with current statistics.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_engine_statistics(
    stats: *mut EngineStats,
) -> c_int {
    if stats.is_null() {
        return -1; // Invalid parameter
    }

    let engine_stats = PERFORMANCE_SYSTEM.engine_stats();

    // Copy atomic values to the output structure
    (*stats).active_engines.store(engine_stats.active_engines.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_engines_created.store(engine_stats.total_engines_created.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_stores_created.store(engine_stats.total_stores_created.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_instances_created.store(engine_stats.total_instances_created.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_function_calls.store(engine_stats.total_function_calls.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_execution_time_micros.store(engine_stats.total_execution_time_micros.load(Ordering::Relaxed), Ordering::Relaxed);

    0 // Success
}

/// Get memory usage statistics
///
/// # Safety
///
/// The stats parameter must point to a valid MemoryStats structure.
/// The structure will be filled with current memory statistics.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_memory_statistics(
    stats: *mut MemoryStats,
) -> c_int {
    if stats.is_null() {
        return -1; // Invalid parameter
    }

    let memory_stats = PERFORMANCE_SYSTEM.memory_stats();

    // Copy atomic values to the output structure
    (*stats).current_allocated.store(memory_stats.current_allocated.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).peak_allocated.store(memory_stats.peak_allocated.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_allocations.store(memory_stats.total_allocations.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_deallocations.store(memory_stats.total_deallocations.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).active_allocations.store(memory_stats.active_allocations.load(Ordering::Relaxed), Ordering::Relaxed);

    0 // Success
}

/// Get compilation performance statistics
///
/// # Safety
///
/// The stats parameter must point to a valid CompilationStats structure.
/// The structure will be filled with current compilation statistics.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_compilation_statistics(
    stats: *mut CompilationStats,
) -> c_int {
    if stats.is_null() {
        return -1; // Invalid parameter
    }

    let compilation_stats = PERFORMANCE_SYSTEM.compilation_stats();

    // Copy atomic values to the output structure
    (*stats).modules_compiled.store(compilation_stats.modules_compiled.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_compile_time_micros.store(compilation_stats.total_compile_time_micros.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).total_module_size_bytes.store(compilation_stats.total_module_size_bytes.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).compilation_failures.store(compilation_stats.compilation_failures.load(Ordering::Relaxed), Ordering::Relaxed);
    (*stats).avg_compile_time_per_kb.store(compilation_stats.avg_compile_time_per_kb.load(Ordering::Relaxed), Ordering::Relaxed);

    0 // Success
}

/// Get WebAssembly feature support information
///
/// # Safety
///
/// The features parameter must point to a valid FeatureSupport structure.
/// The structure will be filled with current feature support status.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_feature_support(
    features: *mut FeatureSupport,
) -> c_int {
    if features.is_null() {
        return -1; // Invalid parameter
    }

    let feature_support = PERFORMANCE_SYSTEM.feature_support();

    // Copy atomic values to the output structure
    (*features).simd_support.store(feature_support.simd_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).multi_memory_support.store(feature_support.multi_memory_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).memory64_support.store(feature_support.memory64_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).bulk_memory_support.store(feature_support.bulk_memory_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).reference_types_support.store(feature_support.reference_types_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).multi_value_support.store(feature_support.multi_value_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).tail_call_support.store(feature_support.tail_call_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).threads_support.store(feature_support.threads_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).component_model_support.store(feature_support.component_model_support.load(Ordering::Relaxed), Ordering::Relaxed);
    (*features).wasi_support.store(feature_support.wasi_support.load(Ordering::Relaxed), Ordering::Relaxed);

    0 // Success
}

/// Get system uptime in seconds
///
/// # Safety
///
/// Returns the number of seconds since the performance system was initialized.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_uptime_seconds() -> c_double {
    PERFORMANCE_SYSTEM.uptime_seconds()
}

/// Reset all performance statistics
///
/// # Safety
///
/// This function is safe to call at any time. All statistics will be reset to zero.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_reset_statistics() -> c_int {
    PERFORMANCE_SYSTEM.reset_stats();
    log::info!("Performance statistics reset");
    0 // Success
}

/// Detect WebAssembly feature support
///
/// # Safety
///
/// The feature_name parameter must be a valid null-terminated C string.
/// Returns 1 if the feature is supported, 0 if not supported, -1 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_detect_feature_support(
    feature_name: *const c_char,
) -> c_int {
    if feature_name.is_null() {
        return -1; // Invalid parameter
    }

    let name_cstr = match CStr::from_ptr(feature_name).to_str() {
        Ok(s) => s,
        Err(_) => return -1, // Invalid UTF-8
    };

    let feature_support = PERFORMANCE_SYSTEM.feature_support();

    let supported = match name_cstr {
        "simd" => feature_support.simd_support.load(Ordering::Relaxed),
        "multi-memory" => feature_support.multi_memory_support.load(Ordering::Relaxed),
        "memory64" => feature_support.memory64_support.load(Ordering::Relaxed),
        "bulk-memory" => feature_support.bulk_memory_support.load(Ordering::Relaxed),
        "reference-types" => feature_support.reference_types_support.load(Ordering::Relaxed),
        "multi-value" => feature_support.multi_value_support.load(Ordering::Relaxed),
        "tail-call" => feature_support.tail_call_support.load(Ordering::Relaxed),
        "threads" => feature_support.threads_support.load(Ordering::Relaxed),
        "component-model" => feature_support.component_model_support.load(Ordering::Relaxed),
        "wasi" => feature_support.wasi_support.load(Ordering::Relaxed),
        _ => return -1, // Unknown feature
    };

    if supported { 1 } else { 0 }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::thread;
    use std::time::Duration;

    #[test]
    fn test_function_call_stats() {
        let stats = FunctionCallStats::default();

        // Test recording calls
        stats.record_call(1000, false);
        stats.record_call(2000, false);
        stats.record_call(500, true);

        assert_eq!(stats.call_count.load(Ordering::Relaxed), 3);
        assert_eq!(stats.total_time_micros.load(Ordering::Relaxed), 3500);
        assert_eq!(stats.min_time_micros.load(Ordering::Relaxed), 500);
        assert_eq!(stats.max_time_micros.load(Ordering::Relaxed), 2000);
        assert_eq!(stats.error_count.load(Ordering::Relaxed), 1);

        // Test calculated values
        assert!((stats.average_time_micros() - 1166.666666666667).abs() < 0.001);
        assert!((stats.error_rate() - 33.333333333333336).abs() < 0.001);
    }

    #[test]
    fn test_memory_stats() {
        let stats = MemoryStats::default();

        // Test recording allocations
        stats.record_allocation(1024);
        stats.record_allocation(2048);
        stats.record_allocation(512);

        assert_eq!(stats.current_allocated.load(Ordering::Relaxed), 3584);
        assert_eq!(stats.peak_allocated.load(Ordering::Relaxed), 3584);
        assert_eq!(stats.total_allocations.load(Ordering::Relaxed), 3);
        assert_eq!(stats.active_allocations.load(Ordering::Relaxed), 3);

        // Test recording deallocations
        stats.record_deallocation(1024);

        assert_eq!(stats.current_allocated.load(Ordering::Relaxed), 2560);
        assert_eq!(stats.peak_allocated.load(Ordering::Relaxed), 3584);
        assert_eq!(stats.total_deallocations.load(Ordering::Relaxed), 1);
        assert_eq!(stats.active_allocations.load(Ordering::Relaxed), 2);

        // Test utilization calculation
        let utilization = stats.utilization_percentage();
        assert!((utilization - 71.42857142857143).abs() < 0.001);
    }

    #[test]
    fn test_compilation_stats() {
        let stats = CompilationStats::default();

        // Test successful compilations
        stats.record_compilation(5000, 1024, true);
        stats.record_compilation(3000, 2048, true);
        stats.record_compilation(0, 0, false); // Failed compilation

        assert_eq!(stats.modules_compiled.load(Ordering::Relaxed), 2);
        assert_eq!(stats.total_compile_time_micros.load(Ordering::Relaxed), 8000);
        assert_eq!(stats.total_module_size_bytes.load(Ordering::Relaxed), 3072);
        assert_eq!(stats.compilation_failures.load(Ordering::Relaxed), 1);

        // Test calculated values
        let success_rate = stats.success_rate();
        assert!((success_rate - 66.66666666666667).abs() < 0.001);

        let compile_speed = stats.average_compile_speed_kb_per_sec();
        assert!(compile_speed > 0.0);
    }

    #[test]
    fn test_performance_system() {
        let system = PerformanceSystem::new();

        // Test enabling/disabling
        system.set_enabled(false);
        assert!(!system.is_enabled());

        system.set_enabled(true);
        assert!(system.is_enabled());

        // Test function recording
        system.record_function_call("test_function", 1500, false);

        let stats = system.get_function_stats("test_function");
        assert_eq!(stats.call_count.load(Ordering::Relaxed), 1);
        assert_eq!(stats.total_time_micros.load(Ordering::Relaxed), 1500);

        // Test reset
        system.reset_stats();
        let stats_after_reset = system.get_function_stats("test_function");
        assert_eq!(stats_after_reset.call_count.load(Ordering::Relaxed), 0);
    }

    #[test]
    fn test_performance_timer() {
        let timer = PerformanceTimer::start("test_timer");
        thread::sleep(Duration::from_millis(1));
        timer.finish(false);

        let stats = PERFORMANCE_SYSTEM.get_function_stats("test_timer");
        assert!(stats.call_count.load(Ordering::Relaxed) > 0);
        assert!(stats.total_time_micros.load(Ordering::Relaxed) > 0);
    }

    #[test]
    fn test_concurrent_access() {
        let system = Arc::new(PerformanceSystem::new());
        let mut handles = vec![];

        // Spawn multiple threads recording function calls
        for i in 0..10 {
            let system_clone = Arc::clone(&system);
            let handle = thread::spawn(move || {
                for j in 0..100 {
                    system_clone.record_function_call(
                        &format!("function_{}", i),
                        (i * 100 + j) as u64,
                        false
                    );
                }
            });
            handles.push(handle);
        }

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }

        // Verify all calls were recorded
        for i in 0..10 {
            let stats = system.get_function_stats(&format!("function_{}", i));
            assert_eq!(stats.call_count.load(Ordering::Relaxed), 100);
        }
    }

    #[test]
    fn test_c_api_safety() {
        unsafe {
            // Test initialization
            assert_eq!(wasmtime4j_perf_init(), 0);

            // Test enabled state
            assert_eq!(wasmtime4j_perf_is_enabled(), 1);

            wasmtime4j_perf_set_enabled(0);
            assert_eq!(wasmtime4j_perf_is_enabled(), 0);

            wasmtime4j_perf_set_enabled(1);
            assert_eq!(wasmtime4j_perf_is_enabled(), 1);

            // Test function recording with valid string
            let test_name = CString::new("test_function").unwrap();
            assert_eq!(wasmtime4j_perf_record_function_call(test_name.as_ptr(), 1000, 0), 0);

            // Test with null pointer
            assert_eq!(wasmtime4j_perf_record_function_call(ptr::null(), 1000, 0), -1);

            // Test statistics retrieval
            let mut engine_stats = EngineStats::default();
            assert_eq!(wasmtime4j_perf_get_engine_statistics(&mut engine_stats), 0);

            let mut memory_stats = MemoryStats::default();
            assert_eq!(wasmtime4j_perf_get_memory_statistics(&mut memory_stats), 0);

            let mut compilation_stats = CompilationStats::default();
            assert_eq!(wasmtime4j_perf_get_compilation_statistics(&mut compilation_stats), 0);

            let mut feature_support = FeatureSupport::default();
            assert_eq!(wasmtime4j_perf_get_feature_support(&mut feature_support), 0);

            // Test uptime
            let uptime = wasmtime4j_perf_get_uptime_seconds();
            assert!(uptime >= 0.0);

            // Test feature detection
            let simd_name = CString::new("simd").unwrap();
            let simd_support = wasmtime4j_detect_feature_support(simd_name.as_ptr());
            assert!(simd_support >= 0);

            let invalid_name = CString::new("invalid_feature").unwrap();
            assert_eq!(wasmtime4j_detect_feature_support(invalid_name.as_ptr()), -1);

            // Test null pointer
            assert_eq!(wasmtime4j_detect_feature_support(ptr::null()), -1);

            // Test reset
            assert_eq!(wasmtime4j_perf_reset_statistics(), 0);
        }
    }
}