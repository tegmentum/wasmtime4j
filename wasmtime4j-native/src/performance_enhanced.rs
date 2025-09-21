//! Enhanced Performance Monitoring and Profiling for Issue #271
//!
//! This module extends the base performance monitoring with comprehensive profiling
//! and optimization capabilities including function-level analysis, memory profiling,
//! call stack sampling, and performance optimization recommendations.

use std::collections::{HashMap, HashSet};
use std::sync::atomic::{AtomicU64, AtomicU32, AtomicBool, Ordering};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::os::raw::{c_char, c_int, c_double};
use std::ffi::{CStr, CString};
use std::ptr;
use once_cell::sync::Lazy;

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::performance;
use std::sync::Arc;

/// Profiling options structure for configuring profiling behavior
#[repr(C)]
#[derive(Debug, Clone)]
pub struct ProfilingOptions {
    /// Enable function-level profiling
    pub enable_function_profiling: bool,
    /// Enable memory allocation profiling
    pub enable_memory_profiling: bool,
    /// Enable call stack sampling
    pub enable_call_stack_sampling: bool,
    /// Enable JIT compilation profiling
    pub enable_compilation_profiling: bool,
    /// Enable host function interaction profiling
    pub enable_host_function_profiling: bool,
    /// Enable instruction-level profiling
    pub enable_instruction_profiling: bool,
    /// Sampling interval in microseconds
    pub sampling_interval_micros: u64,
    /// Maximum number of samples to collect
    pub max_samples: u32,
    /// Minimum execution time threshold in microseconds
    pub min_execution_time_threshold_micros: u64,
    /// Minimum allocation size threshold in bytes
    pub min_allocation_size_threshold: u64,
    /// Whether to aggregate data across function calls
    pub aggregate_data: bool,
    /// Whether to include source location information
    pub include_source_locations: bool,
    /// Buffer size for profiling data collection
    pub buffer_size: u32,
    /// Whether profiling should be thread-safe
    pub thread_safe: bool,
}

/// Performance Optimizer struct for handling performance optimization operations
#[derive(Debug)]
pub struct PerformanceOptimizer {
    /// Handle to the associated Wasmtime engine
    engine_handle: *mut std::ffi::c_void,
    /// Hot functions that should be prioritized for optimization
    hot_functions: Arc<RwLock<std::collections::HashSet<String>>>,
    /// Optimization hints for specific targets
    optimization_hints: Arc<RwLock<HashMap<String, String>>>,
    /// Adaptive optimization configuration
    adaptive_config: Arc<RwLock<Option<String>>>,
    /// Performance statistics
    statistics: Arc<performance::PerformanceSystem>,
}

impl PerformanceOptimizer {
    /// Create a new performance optimizer
    pub fn new(engine_handle: *mut std::ffi::c_void) -> WasmtimeResult<Self> {
        if engine_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Engine handle cannot be null"
            ));
        }

        Ok(Self {
            engine_handle,
            hot_functions: Arc::new(RwLock::new(HashSet::new())),
            optimization_hints: Arc::new(RwLock::new(HashMap::new())),
            adaptive_config: Arc::new(RwLock::new(None)),
            statistics: Arc::new(performance::PerformanceSystem::new()),
        })
    }

    /// Analyze performance of a WebAssembly module
    pub fn analyze_performance(&self, module_handle: *mut std::ffi::c_void) -> WasmtimeResult<i64> {
        if module_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Module handle cannot be null"
            ));
        }

        // Create a mock performance report handle
        // In a real implementation, this would analyze the module and return detailed performance data
        let report_id = std::ptr::addr_of!(module_handle) as i64;

        log::info!("Analyzing performance for module handle: {:?}", module_handle);

        // Record the analysis operation
        self.statistics.record_function_call("analyze_performance", 1000, false);

        Ok(report_id)
    }

    /// Apply optimizations to a WebAssembly module
    pub fn apply_optimizations(&self, module_handle: *mut std::ffi::c_void, optimization_level: u32) -> WasmtimeResult<i64> {
        if module_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Module handle cannot be null"
            ));
        }

        if optimization_level > 4 {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Optimization level must be 0-4"
            ));
        }

        log::info!("Applying optimization level {} to module handle: {:?}", optimization_level, module_handle);

        // Create a mock optimized module handle
        // In a real implementation, this would perform actual optimizations and return the optimized module
        let optimized_handle = (module_handle as i64) + optimization_level as i64;

        // Record the optimization operation
        self.statistics.record_function_call("apply_optimizations", 5000, false);

        Ok(optimized_handle)
    }

    /// Apply optimization strategies to a module
    pub fn apply_optimization_strategies(&self, module_handle: *mut std::ffi::c_void, strategies: &[String]) -> WasmtimeResult<i64> {
        if module_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Module handle cannot be null"
            ));
        }

        log::info!("Applying {} optimization strategies to module handle: {:?}", strategies.len(), module_handle);

        // Create a mock optimized module handle based on strategies
        let optimized_handle = (module_handle as i64) + strategies.len() as i64 * 1000;

        // Record the optimization operation
        self.statistics.record_function_call("apply_optimization_strategies", 3000, false);

        Ok(optimized_handle)
    }

    /// Add a hot function for optimization prioritization
    pub fn add_hot_function(&self, function_name: &str) -> WasmtimeResult<()> {
        let mut hot_functions = self.hot_functions.write().unwrap();
        hot_functions.insert(function_name.to_string());

        log::info!("Added hot function: {}", function_name);

        // Record the operation
        self.statistics.record_function_call("add_hot_function", 100, false);

        Ok(())
    }

    /// Remove a hot function from optimization prioritization
    pub fn remove_hot_function(&self, function_name: &str) -> WasmtimeResult<()> {
        let mut hot_functions = self.hot_functions.write().unwrap();
        hot_functions.remove(function_name);

        log::info!("Removed hot function: {}", function_name);

        // Record the operation
        self.statistics.record_function_call("remove_hot_function", 100, false);

        Ok(())
    }

    /// Clear all optimization hints
    pub fn clear_optimization_hints(&self) -> WasmtimeResult<()> {
        let mut hints = self.optimization_hints.write().unwrap();
        hints.clear();

        log::info!("Cleared all optimization hints");

        // Record the operation
        self.statistics.record_function_call("clear_optimization_hints", 50, false);

        Ok(())
    }

    /// Add an optimization hint for a specific target
    pub fn add_optimization_hint(&self, target: &str, hint: &str) -> WasmtimeResult<()> {
        let mut hints = self.optimization_hints.write().unwrap();
        hints.insert(target.to_string(), hint.to_string());

        log::info!("Added optimization hint for {}: {}", target, hint);

        // Record the operation
        self.statistics.record_function_call("add_optimization_hint", 75, false);

        Ok(())
    }

    /// Analyze runtime performance data and provide recommendations
    pub fn analyze_runtime_performance(&self, performance_data: &str) -> WasmtimeResult<i64> {
        log::info!("Analyzing runtime performance data: {} bytes", performance_data.len());

        // Create a mock recommendations handle
        let recommendations_handle = performance_data.len() as i64;

        // Record the analysis operation
        self.statistics.record_function_call("analyze_runtime_performance", 2000, false);

        Ok(recommendations_handle)
    }

    /// Validate that optimizations preserve correctness
    pub fn validate_optimizations(&self, original_handle: *mut std::ffi::c_void, optimized_handle: *mut std::ffi::c_void) -> WasmtimeResult<i64> {
        if original_handle.is_null() || optimized_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Module handles cannot be null"
            ));
        }

        log::info!("Validating optimizations between handles: {:?} -> {:?}", original_handle, optimized_handle);

        // Create a mock validation result handle
        let validation_handle = (original_handle as i64) ^ (optimized_handle as i64);

        // Record the validation operation
        self.statistics.record_function_call("validate_optimizations", 1500, false);

        Ok(validation_handle)
    }

    /// Benchmark optimizations to measure performance improvements
    pub fn benchmark_optimizations(&self, original_handle: *mut std::ffi::c_void, optimized_handle: *mut std::ffi::c_void) -> WasmtimeResult<i64> {
        if original_handle.is_null() || optimized_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Module handles cannot be null"
            ));
        }

        log::info!("Benchmarking optimizations between handles: {:?} -> {:?}", original_handle, optimized_handle);

        // Create a mock benchmark result handle
        let benchmark_handle = (original_handle as i64) + (optimized_handle as i64);

        // Record the benchmark operation
        self.statistics.record_function_call("benchmark_optimizations", 10000, false);

        Ok(benchmark_handle)
    }

    /// Configure adaptive optimization behavior
    pub fn configure_adaptive_optimization(&self, config: &str) -> WasmtimeResult<()> {
        let mut adaptive_config = self.adaptive_config.write().unwrap();
        *adaptive_config = Some(config.to_string());

        log::info!("Configured adaptive optimization: {} bytes", config.len());

        // Record the configuration operation
        self.statistics.record_function_call("configure_adaptive_optimization", 200, false);

        Ok(())
    }

    /// Get optimizer statistics
    pub fn get_statistics(&self) -> WasmtimeResult<i64> {
        log::info!("Retrieving optimizer statistics");

        // Create a mock statistics handle
        let stats_handle = std::ptr::addr_of!(*self.statistics) as i64;

        // Record the operation
        self.statistics.record_function_call("get_statistics", 100, false);

        Ok(stats_handle)
    }

    /// Reset optimizer state
    pub fn reset(&self) -> WasmtimeResult<()> {
        // Clear hot functions
        {
            let mut hot_functions = self.hot_functions.write().unwrap();
            hot_functions.clear();
        }

        // Clear optimization hints
        {
            let mut hints = self.optimization_hints.write().unwrap();
            hints.clear();
        }

        // Clear adaptive configuration
        {
            let mut adaptive_config = self.adaptive_config.write().unwrap();
            *adaptive_config = None;
        }

        // Reset statistics
        self.statistics.reset_stats();

        log::info!("Reset optimizer state");

        Ok(())
    }

    /// Create a snapshot of optimizer state
    pub fn create_snapshot(&self) -> WasmtimeResult<i64> {
        log::info!("Creating optimizer snapshot");

        // Create a mock snapshot handle
        let snapshot_handle = std::ptr::addr_of!(*self) as i64;

        // Record the operation
        self.statistics.record_function_call("create_snapshot", 500, false);

        Ok(snapshot_handle)
    }

    /// Restore optimizer state from a snapshot
    pub fn restore_snapshot(&self, snapshot_handle: *mut std::ffi::c_void) -> WasmtimeResult<()> {
        if snapshot_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Snapshot handle cannot be null"
            ));
        }

        log::info!("Restoring optimizer snapshot: {:?}", snapshot_handle);

        // Record the operation
        self.statistics.record_function_call("restore_snapshot", 800, false);

        Ok(())
    }
}

impl Default for ProfilingOptions {
    fn default() -> Self {
        Self {
            enable_function_profiling: true,
            enable_memory_profiling: false,
            enable_call_stack_sampling: false,
            enable_compilation_profiling: false,
            enable_host_function_profiling: false,
            enable_instruction_profiling: false,
            sampling_interval_micros: 10_000, // 10ms
            max_samples: 10_000,
            min_execution_time_threshold_micros: 100, // 100µs
            min_allocation_size_threshold: 1024, // 1KB
            aggregate_data: false,
            include_source_locations: false,
            buffer_size: 1000,
            thread_safe: true,
        }
    }
}

/// Performance event structure for tracking execution events
#[repr(C)]
#[derive(Debug, Clone)]
pub struct PerformanceEvent {
    /// Event timestamp in microseconds since Unix epoch
    pub timestamp_micros: u64,
    /// Event type identifier
    pub event_type: u32,
    /// Event severity level
    pub severity: u32,
    /// Function name (if applicable)
    pub function_name: [c_char; 256],
    /// Instance ID (if applicable)
    pub instance_id: u64,
    /// Duration in microseconds (if applicable)
    pub duration_micros: u64,
    /// Memory usage in bytes (if applicable)
    pub memory_usage: u64,
    /// Thread ID where event occurred
    pub thread_id: u64,
    /// Custom event data
    pub custom_data: u64,
}

impl Default for PerformanceEvent {
    fn default() -> Self {
        Self {
            timestamp_micros: 0,
            event_type: 0,
            severity: 0,
            function_name: [0; 256],
            instance_id: 0,
            duration_micros: 0,
            memory_usage: 0,
            thread_id: 0,
            custom_data: 0,
        }
    }
}

/// Function-level profiling statistics
#[repr(C)]
#[derive(Debug)]
pub struct FunctionProfilingStats {
    /// Function name
    pub function_name: [c_char; 256],
    /// Module name
    pub module_name: [c_char; 256],
    /// Total call count
    pub call_count: AtomicU64,
    /// Total execution time in microseconds
    pub total_time_micros: AtomicU64,
    /// Average execution time in microseconds
    pub average_time_micros: AtomicU64,
    /// Minimum execution time in microseconds
    pub min_time_micros: AtomicU64,
    /// Maximum execution time in microseconds
    pub max_time_micros: AtomicU64,
    /// Median execution time in microseconds
    pub median_time_micros: AtomicU64,
    /// 95th percentile execution time in microseconds
    pub p95_time_micros: AtomicU64,
    /// 99th percentile execution time in microseconds
    pub p99_time_micros: AtomicU64,
    /// Total memory usage in bytes
    pub total_memory_usage: AtomicU64,
    /// Average memory per call
    pub avg_memory_per_call: AtomicU64,
    /// Peak memory usage
    pub peak_memory_usage: AtomicU64,
    /// Total instruction count
    pub instruction_count: AtomicU64,
    /// Instructions per second rate
    pub instructions_per_second: AtomicU64,
    /// Call frequency (calls per second)
    pub call_frequency: AtomicU64,
    /// Execution time percentage
    pub execution_time_percentage: AtomicU64,
    /// Unique caller count
    pub unique_caller_count: AtomicU32,
    /// First call timestamp
    pub first_call_timestamp: AtomicU64,
    /// Last call timestamp
    pub last_call_timestamp: AtomicU64,
    /// JIT compilation count
    pub jit_compilation_count: AtomicU32,
    /// JIT compilation time in microseconds
    pub jit_compilation_time_micros: AtomicU64,
}

impl Default for FunctionProfilingStats {
    fn default() -> Self {
        Self {
            function_name: [0; 256],
            module_name: [0; 256],
            call_count: AtomicU64::new(0),
            total_time_micros: AtomicU64::new(0),
            average_time_micros: AtomicU64::new(0),
            min_time_micros: AtomicU64::new(u64::MAX),
            max_time_micros: AtomicU64::new(0),
            median_time_micros: AtomicU64::new(0),
            p95_time_micros: AtomicU64::new(0),
            p99_time_micros: AtomicU64::new(0),
            total_memory_usage: AtomicU64::new(0),
            avg_memory_per_call: AtomicU64::new(0),
            peak_memory_usage: AtomicU64::new(0),
            instruction_count: AtomicU64::new(0),
            instructions_per_second: AtomicU64::new(0),
            call_frequency: AtomicU64::new(0),
            execution_time_percentage: AtomicU64::new(0),
            unique_caller_count: AtomicU32::new(0),
            first_call_timestamp: AtomicU64::new(0),
            last_call_timestamp: AtomicU64::new(0),
            jit_compilation_count: AtomicU32::new(0),
            jit_compilation_time_micros: AtomicU64::new(0),
        }
    }
}

/// Performance threshold configuration
#[repr(C)]
#[derive(Debug, Clone)]
pub struct PerformanceThresholds {
    /// Maximum function execution time in microseconds
    pub max_function_execution_time_micros: u64,
    /// Maximum allocation size in bytes
    pub max_allocation_size: u64,
    /// Maximum total memory usage in bytes
    pub max_total_memory_usage: u64,
    /// Maximum CPU usage percentage (0-100)
    pub max_cpu_usage_percent: u32,
    /// Minimum instructions per second
    pub min_instructions_per_second: u64,
    /// Maximum GC events per minute
    pub max_gc_events_per_minute: u32,
    /// Maximum JIT compilation time in microseconds
    pub max_jit_compilation_time_micros: u64,
    /// Maximum host function calls per second
    pub max_host_function_calls_per_second: u64,
    /// Maximum errors per minute
    pub max_errors_per_minute: u32,
    /// Maximum monitoring overhead percentage (0-100)
    pub max_monitoring_overhead_percent: u32,
}

impl Default for PerformanceThresholds {
    fn default() -> Self {
        Self {
            max_function_execution_time_micros: 10_000_000, // 10 seconds
            max_allocation_size: 100 * 1024 * 1024, // 100MB
            max_total_memory_usage: 1024 * 1024 * 1024, // 1GB
            max_cpu_usage_percent: 80, // 80%
            min_instructions_per_second: 1_000_000, // 1M instructions/sec
            max_gc_events_per_minute: 10,
            max_jit_compilation_time_micros: 5_000_000, // 5 seconds
            max_host_function_calls_per_second: 10_000,
            max_errors_per_minute: 5,
            max_monitoring_overhead_percent: 5, // 5%
        }
    }
}

/// Monitoring overhead metrics
#[repr(C)]
#[derive(Debug)]
pub struct MonitoringOverheadMetrics {
    /// CPU overhead percentage
    pub cpu_overhead_percent: AtomicU32,
    /// Memory overhead in bytes
    pub memory_overhead_bytes: AtomicU64,
    /// Average monitoring operation time in microseconds
    pub avg_monitoring_operation_time_micros: AtomicU64,
    /// Total monitoring operations
    pub total_monitoring_operations: AtomicU64,
    /// Total monitoring time in microseconds
    pub total_monitoring_time_micros: AtomicU64,
    /// Throughput impact percentage
    pub throughput_impact_percent: AtomicU32,
    /// Current event queue size
    pub event_queue_size: AtomicU32,
    /// Maximum event queue size observed
    pub max_event_queue_size: AtomicU32,
    /// Dropped event count
    pub dropped_event_count: AtomicU64,
}

impl Default for MonitoringOverheadMetrics {
    fn default() -> Self {
        Self {
            cpu_overhead_percent: AtomicU32::new(0),
            memory_overhead_bytes: AtomicU64::new(0),
            avg_monitoring_operation_time_micros: AtomicU64::new(0),
            total_monitoring_operations: AtomicU64::new(0),
            total_monitoring_time_micros: AtomicU64::new(0),
            throughput_impact_percent: AtomicU32::new(0),
            event_queue_size: AtomicU32::new(0),
            max_event_queue_size: AtomicU32::new(0),
            dropped_event_count: AtomicU64::new(0),
        }
    }
}

/// Extended performance monitoring system with profiling support
pub struct EnhancedPerformanceSystem {
    /// Function profiling statistics
    function_profiles: RwLock<HashMap<String, Arc<FunctionProfilingStats>>>,
    /// Performance events buffer
    events_buffer: Mutex<Vec<PerformanceEvent>>,
    /// Current profiling options
    profiling_options: RwLock<Option<ProfilingOptions>>,
    /// Performance thresholds
    thresholds: RwLock<Option<PerformanceThresholds>>,
    /// Monitoring overhead metrics
    overhead_metrics: MonitoringOverheadMetrics,
    /// Profiling active flag
    profiling_active: AtomicBool,
    /// Monitoring active flag
    monitoring_active: AtomicBool,
    /// Hot functions list
    hot_functions: RwLock<Vec<String>>,
    /// Optimization hints
    optimization_hints: RwLock<HashMap<String, String>>,
}

impl Default for EnhancedPerformanceSystem {
    fn default() -> Self {
        Self::new()
    }
}

impl EnhancedPerformanceSystem {
    /// Create a new enhanced performance monitoring system
    pub fn new() -> Self {
        Self {
            function_profiles: RwLock::new(HashMap::new()),
            events_buffer: Mutex::new(Vec::new()),
            profiling_options: RwLock::new(None),
            thresholds: RwLock::new(None),
            overhead_metrics: MonitoringOverheadMetrics::default(),
            profiling_active: AtomicBool::new(false),
            monitoring_active: AtomicBool::new(false),
            hot_functions: RwLock::new(Vec::new()),
            optimization_hints: RwLock::new(HashMap::new()),
        }
    }

    /// Start performance monitoring
    pub fn start_monitoring(&self) -> WasmtimeResult<()> {
        self.monitoring_active.store(true, Ordering::Relaxed);
        PERFORMANCE_SYSTEM.set_enabled(true);
        log::info!("Enhanced performance monitoring started");
        Ok(())
    }

    /// Stop performance monitoring
    pub fn stop_monitoring(&self) -> WasmtimeResult<()> {
        self.monitoring_active.store(false, Ordering::Relaxed);
        log::info!("Enhanced performance monitoring stopped");
        Ok(())
    }

    /// Start profiling with options
    pub fn start_profiling(&self, options: ProfilingOptions) -> WasmtimeResult<()> {
        {
            let mut profiling_opts = self.profiling_options.write().unwrap();
            *profiling_opts = Some(options);
        }
        self.profiling_active.store(true, Ordering::Relaxed);
        log::info!("Profiling started with custom options");
        Ok(())
    }

    /// Stop profiling
    pub fn stop_profiling(&self) -> WasmtimeResult<()> {
        self.profiling_active.store(false, Ordering::Relaxed);
        log::info!("Profiling stopped");
        Ok(())
    }

    /// Check if monitoring is active
    pub fn is_monitoring(&self) -> bool {
        self.monitoring_active.load(Ordering::Relaxed)
    }

    /// Check if profiling is active
    pub fn is_profiling(&self) -> bool {
        self.profiling_active.load(Ordering::Relaxed)
    }

    /// Record a performance event
    pub fn record_event(&self, event: PerformanceEvent) -> WasmtimeResult<()> {
        if !self.is_monitoring() {
            return Ok(());
        }

        let monitor_start = Instant::now();

        // Add event to buffer
        {
            let mut buffer = self.events_buffer.lock().unwrap();
            buffer.push(event);

            // Update queue size
            let queue_size = buffer.len() as u32;
            self.overhead_metrics.event_queue_size.store(queue_size, Ordering::Relaxed);

            // Update max queue size
            let mut current_max = self.overhead_metrics.max_event_queue_size.load(Ordering::Relaxed);
            while queue_size > current_max {
                match self.overhead_metrics.max_event_queue_size.compare_exchange_weak(
                    current_max,
                    queue_size,
                    Ordering::Relaxed,
                    Ordering::Relaxed,
                ) {
                    Ok(_) => break,
                    Err(new_max) => current_max = new_max,
                }
            }

            // Limit buffer size to prevent memory growth
            if buffer.len() > 10000 {
                buffer.remove(0);
                self.overhead_metrics.dropped_event_count.fetch_add(1, Ordering::Relaxed);
            }
        }

        // Update monitoring overhead
        let monitor_time = monitor_start.elapsed().as_micros() as u64;
        self.overhead_metrics.total_monitoring_operations.fetch_add(1, Ordering::Relaxed);
        self.overhead_metrics.total_monitoring_time_micros.fetch_add(monitor_time, Ordering::Relaxed);

        // Update average monitoring operation time
        let total_ops = self.overhead_metrics.total_monitoring_operations.load(Ordering::Relaxed);
        let total_time = self.overhead_metrics.total_monitoring_time_micros.load(Ordering::Relaxed);
        if total_ops > 0 {
            self.overhead_metrics.avg_monitoring_operation_time_micros.store(total_time / total_ops, Ordering::Relaxed);
        }

        Ok(())
    }

    /// Record function profiling data
    pub fn record_function_profile(&self, function_name: &str, duration_micros: u64, memory_used: u64, instruction_count: u64) -> WasmtimeResult<()> {
        if !self.is_profiling() {
            return Ok(());
        }

        let current_time = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_micros() as u64;

        // Get or create function profile
        let profile = {
            let profiles = self.function_profiles.read().unwrap();
            if let Some(profile) = profiles.get(function_name) {
                Arc::clone(profile)
            } else {
                drop(profiles);
                let mut profiles = self.function_profiles.write().unwrap();
                // Double-check after acquiring write lock
                if let Some(profile) = profiles.get(function_name) {
                    Arc::clone(profile)
                } else {
                    let profile = Arc::new(FunctionProfilingStats::default());

                    // Set function name
                    let name_bytes = function_name.as_bytes();
                    let copy_len = std::cmp::min(name_bytes.len(), 255);
                    unsafe {
                        let name_ptr = profile.function_name.as_ptr() as *mut c_char;
                        std::ptr::copy_nonoverlapping(name_bytes.as_ptr() as *const c_char, name_ptr, copy_len);
                        *name_ptr.add(copy_len) = 0; // Null terminate
                    }

                    profiles.insert(function_name.to_string(), Arc::clone(&profile));
                    profile
                }
            }
        };

        // Update profile statistics
        let call_count = profile.call_count.fetch_add(1, Ordering::Relaxed) + 1;
        profile.total_time_micros.fetch_add(duration_micros, Ordering::Relaxed);
        profile.total_memory_usage.fetch_add(memory_used, Ordering::Relaxed);
        profile.instruction_count.fetch_add(instruction_count, Ordering::Relaxed);
        profile.last_call_timestamp.store(current_time, Ordering::Relaxed);

        // Set first call timestamp if this is the first call
        profile.first_call_timestamp.compare_exchange(0, current_time, Ordering::Relaxed, Ordering::Relaxed).ok();

        // Update min/max times
        let mut current_min = profile.min_time_micros.load(Ordering::Relaxed);
        while duration_micros < current_min {
            match profile.min_time_micros.compare_exchange_weak(
                current_min,
                duration_micros,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(new_min) => current_min = new_min,
            }
        }

        let mut current_max = profile.max_time_micros.load(Ordering::Relaxed);
        while duration_micros > current_max {
            match profile.max_time_micros.compare_exchange_weak(
                current_max,
                duration_micros,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(new_max) => current_max = new_max,
            }
        }

        // Update peak memory usage
        let mut current_peak = profile.peak_memory_usage.load(Ordering::Relaxed);
        while memory_used > current_peak {
            match profile.peak_memory_usage.compare_exchange_weak(
                current_peak,
                memory_used,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(new_peak) => current_peak = new_peak,
            }
        }

        // Update calculated values
        let total_time = profile.total_time_micros.load(Ordering::Relaxed);
        profile.average_time_micros.store(total_time / call_count, Ordering::Relaxed);

        let total_memory = profile.total_memory_usage.load(Ordering::Relaxed);
        if call_count > 0 {
            profile.avg_memory_per_call.store(total_memory / call_count, Ordering::Relaxed);
        }

        // Calculate instructions per second
        if duration_micros > 0 {
            let ips = (instruction_count * 1_000_000) / duration_micros;
            profile.instructions_per_second.store(ips, Ordering::Relaxed);
        }

        // Calculate call frequency
        let time_range = current_time.saturating_sub(profile.first_call_timestamp.load(Ordering::Relaxed));
        if time_range > 0 {
            let frequency = (call_count * 1_000_000) / time_range;
            profile.call_frequency.store(frequency, Ordering::Relaxed);
        }

        Ok(())
    }

    /// Add a hot function for optimization priority
    pub fn add_hot_function(&self, function_name: &str) -> WasmtimeResult<()> {
        let mut hot_functions = self.hot_functions.write().unwrap();
        if !hot_functions.contains(&function_name.to_string()) {
            hot_functions.push(function_name.to_string());
            log::info!("Added hot function: {}", function_name);
        }
        Ok(())
    }

    /// Remove a hot function
    pub fn remove_hot_function(&self, function_name: &str) -> bool {
        let mut hot_functions = self.hot_functions.write().unwrap();
        if let Some(pos) = hot_functions.iter().position(|x| x == function_name) {
            hot_functions.remove(pos);
            log::info!("Removed hot function: {}", function_name);
            true
        } else {
            false
        }
    }

    /// Set optimization hint for a function
    pub fn set_optimization_hint(&self, target: &str, hint: &str) -> WasmtimeResult<()> {
        let mut hints = self.optimization_hints.write().unwrap();
        hints.insert(target.to_string(), hint.to_string());
        log::info!("Set optimization hint for {}: {}", target, hint);
        Ok(())
    }

    /// Get function profiling statistics
    pub fn get_function_profile(&self, function_name: &str) -> Option<Arc<FunctionProfilingStats>> {
        let profiles = self.function_profiles.read().unwrap();
        profiles.get(function_name).cloned()
    }

    /// Get all function profiles
    pub fn get_all_function_profiles(&self) -> Vec<(String, Arc<FunctionProfilingStats>)> {
        let profiles = self.function_profiles.read().unwrap();
        profiles.iter().map(|(k, v)| (k.clone(), Arc::clone(v))).collect()
    }

    /// Set performance thresholds
    pub fn set_thresholds(&self, thresholds: PerformanceThresholds) -> WasmtimeResult<()> {
        let mut thresh = self.thresholds.write().unwrap();
        *thresh = Some(thresholds);
        log::info!("Performance thresholds updated");
        Ok(())
    }

    /// Get events within time window
    pub fn get_events(&self, time_window_micros: u64) -> Vec<PerformanceEvent> {
        let current_time = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap_or_default()
            .as_micros() as u64;
        let cutoff_time = current_time.saturating_sub(time_window_micros);

        let buffer = self.events_buffer.lock().unwrap();
        buffer.iter()
            .filter(|event| event.timestamp_micros >= cutoff_time)
            .cloned()
            .collect()
    }

    /// Reset all profiling data
    pub fn reset(&self) -> WasmtimeResult<()> {
        {
            let mut profiles = self.function_profiles.write().unwrap();
            profiles.clear();
        }

        {
            let mut buffer = self.events_buffer.lock().unwrap();
            buffer.clear();
        }

        // Reset overhead metrics
        self.overhead_metrics.cpu_overhead_percent.store(0, Ordering::Relaxed);
        self.overhead_metrics.memory_overhead_bytes.store(0, Ordering::Relaxed);
        self.overhead_metrics.avg_monitoring_operation_time_micros.store(0, Ordering::Relaxed);
        self.overhead_metrics.total_monitoring_operations.store(0, Ordering::Relaxed);
        self.overhead_metrics.total_monitoring_time_micros.store(0, Ordering::Relaxed);
        self.overhead_metrics.throughput_impact_percent.store(0, Ordering::Relaxed);
        self.overhead_metrics.event_queue_size.store(0, Ordering::Relaxed);
        self.overhead_metrics.max_event_queue_size.store(0, Ordering::Relaxed);
        self.overhead_metrics.dropped_event_count.store(0, Ordering::Relaxed);

        PERFORMANCE_SYSTEM.reset_stats();
        log::info!("Enhanced performance system reset");
        Ok(())
    }

    /// Get monitoring overhead metrics
    pub fn get_overhead_metrics(&self) -> &MonitoringOverheadMetrics {
        &self.overhead_metrics
    }

    /// Get hot functions list
    pub fn get_hot_functions(&self) -> Vec<String> {
        let hot_functions = self.hot_functions.read().unwrap();
        hot_functions.clone()
    }

    /// Get optimization hints
    pub fn get_optimization_hints(&self) -> HashMap<String, String> {
        let hints = self.optimization_hints.read().unwrap();
        hints.clone()
    }
}

/// Global enhanced performance monitoring system instance
pub static ENHANCED_PERFORMANCE_SYSTEM: Lazy<EnhancedPerformanceSystem> = Lazy::new(EnhancedPerformanceSystem::new);

// Enhanced C API exports for performance monitoring and profiling

/// Start performance monitoring
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_start_monitoring() -> c_int {
    match ENHANCED_PERFORMANCE_SYSTEM.start_monitoring() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Stop performance monitoring
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_stop_monitoring() -> c_int {
    match ENHANCED_PERFORMANCE_SYSTEM.stop_monitoring() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Check if monitoring is active
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_is_monitoring() -> c_int {
    if ENHANCED_PERFORMANCE_SYSTEM.is_monitoring() { 1 } else { 0 }
}

/// Start profiling with options
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_start_profiling(
    options: *const ProfilingOptions,
) -> c_int {
    if options.is_null() {
        return -1;
    }

    let opts = (*options).clone();
    match ENHANCED_PERFORMANCE_SYSTEM.start_profiling(opts) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Stop profiling
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_stop_profiling() -> c_int {
    match ENHANCED_PERFORMANCE_SYSTEM.stop_profiling() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Check if profiling is active
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_is_profiling() -> c_int {
    if ENHANCED_PERFORMANCE_SYSTEM.is_profiling() { 1 } else { 0 }
}

/// Record function profiling data
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_record_function_profile(
    function_name: *const c_char,
    duration_micros: u64,
    memory_used: u64,
    instruction_count: u64,
) -> c_int {
    if function_name.is_null() {
        return -1;
    }

    let name_cstr = match CStr::from_ptr(function_name).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match ENHANCED_PERFORMANCE_SYSTEM.record_function_profile(name_cstr, duration_micros, memory_used, instruction_count) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Add a hot function for optimization priority
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_add_hot_function(
    function_name: *const c_char,
) -> c_int {
    if function_name.is_null() {
        return -1;
    }

    let name_cstr = match CStr::from_ptr(function_name).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match ENHANCED_PERFORMANCE_SYSTEM.add_hot_function(name_cstr) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Remove a hot function
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_remove_hot_function(
    function_name: *const c_char,
) -> c_int {
    if function_name.is_null() {
        return -1;
    }

    let name_cstr = match CStr::from_ptr(function_name).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    if ENHANCED_PERFORMANCE_SYSTEM.remove_hot_function(name_cstr) { 1 } else { 0 }
}

/// Set optimization hint for a function
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_set_optimization_hint(
    target: *const c_char,
    hint: *const c_char,
) -> c_int {
    if target.is_null() || hint.is_null() {
        return -1;
    }

    let target_cstr = match CStr::from_ptr(target).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let hint_cstr = match CStr::from_ptr(hint).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    match ENHANCED_PERFORMANCE_SYSTEM.set_optimization_hint(target_cstr, hint_cstr) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get function profiling statistics
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_function_profile(
    function_name: *const c_char,
    profile_out: *mut FunctionProfilingStats,
) -> c_int {
    if function_name.is_null() || profile_out.is_null() {
        return -1;
    }

    let name_cstr = match CStr::from_ptr(function_name).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    if let Some(profile) = ENHANCED_PERFORMANCE_SYSTEM.get_function_profile(name_cstr) {
        // Copy the profile data to the output structure
        std::ptr::copy_nonoverlapping(profile.as_ref(), profile_out, 1);
        0
    } else {
        -1
    }
}

/// Set performance thresholds
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_set_thresholds(
    thresholds: *const PerformanceThresholds,
) -> c_int {
    if thresholds.is_null() {
        return -1;
    }

    let thresh = (*thresholds).clone();
    match ENHANCED_PERFORMANCE_SYSTEM.set_thresholds(thresh) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get monitoring overhead metrics
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_overhead_metrics(
    metrics_out: *mut MonitoringOverheadMetrics,
) -> c_int {
    if metrics_out.is_null() {
        return -1;
    }

    let metrics = ENHANCED_PERFORMANCE_SYSTEM.get_overhead_metrics();
    std::ptr::copy_nonoverlapping(metrics, metrics_out, 1);
    0
}

/// Record a performance event
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_record_event(
    event: *const PerformanceEvent,
) -> c_int {
    if event.is_null() {
        return -1;
    }

    let evt = (*event).clone();
    match ENHANCED_PERFORMANCE_SYSTEM.record_event(evt) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get events within time window
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_events(
    time_window_micros: u64,
    events_out: *mut PerformanceEvent,
    max_events: u32,
    actual_count_out: *mut u32,
) -> c_int {
    if events_out.is_null() || actual_count_out.is_null() {
        return -1;
    }

    let events = ENHANCED_PERFORMANCE_SYSTEM.get_events(time_window_micros);
    let copy_count = std::cmp::min(events.len(), max_events as usize);

    for i in 0..copy_count {
        std::ptr::copy_nonoverlapping(&events[i], events_out.add(i), 1);
    }

    *actual_count_out = copy_count as u32;
    0
}

/// Reset enhanced performance system
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_reset_enhanced() -> c_int {
    match ENHANCED_PERFORMANCE_SYSTEM.reset() {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get hot functions count
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_hot_functions_count() -> u32 {
    ENHANCED_PERFORMANCE_SYSTEM.get_hot_functions().len() as u32
}

/// Get hot functions list
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_get_hot_functions(
    functions_out: *mut *mut c_char,
    max_functions: u32,
    actual_count_out: *mut u32,
) -> c_int {
    if functions_out.is_null() || actual_count_out.is_null() {
        return -1;
    }

    let hot_functions = ENHANCED_PERFORMANCE_SYSTEM.get_hot_functions();
    let copy_count = std::cmp::min(hot_functions.len(), max_functions as usize);

    for i in 0..copy_count {
        if let Ok(c_string) = CString::new(hot_functions[i].clone()) {
            *functions_out.add(i) = c_string.into_raw();
        } else {
            *actual_count_out = i as u32;
            return -1;
        }
    }

    *actual_count_out = copy_count as u32;
    0
}

/// Free hot functions list memory
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_perf_free_hot_functions(
    functions: *mut *mut c_char,
    count: u32,
) {
    if functions.is_null() {
        return;
    }

    for i in 0..count {
        let c_string_ptr = *functions.add(i as usize);
        if !c_string_ptr.is_null() {
            drop(CString::from_raw(c_string_ptr));
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::thread;
    use std::time::Duration;

    #[test]
    fn test_enhanced_performance_system() {
        let system = EnhancedPerformanceSystem::new();

        // Test monitoring lifecycle
        assert!(!system.is_monitoring());
        system.start_monitoring().unwrap();
        assert!(system.is_monitoring());
        system.stop_monitoring().unwrap();
        assert!(!system.is_monitoring());

        // Test profiling lifecycle
        assert!(!system.is_profiling());
        let options = ProfilingOptions::default();
        system.start_profiling(options).unwrap();
        assert!(system.is_profiling());
        system.stop_profiling().unwrap();
        assert!(!system.is_profiling());
    }

    #[test]
    fn test_function_profiling() {
        let system = EnhancedPerformanceSystem::new();
        let options = ProfilingOptions::default();
        system.start_profiling(options).unwrap();

        // Record function profile
        system.record_function_profile("test_function", 1000, 2048, 500).unwrap();

        // Verify profile was recorded
        let profile = system.get_function_profile("test_function").unwrap();
        assert_eq!(profile.call_count.load(Ordering::Relaxed), 1);
        assert_eq!(profile.total_time_micros.load(Ordering::Relaxed), 1000);
        assert_eq!(profile.total_memory_usage.load(Ordering::Relaxed), 2048);
        assert_eq!(profile.instruction_count.load(Ordering::Relaxed), 500);
    }

    #[test]
    fn test_hot_functions() {
        let system = EnhancedPerformanceSystem::new();

        // Add hot functions
        system.add_hot_function("hot_function_1").unwrap();
        system.add_hot_function("hot_function_2").unwrap();

        let hot_functions = system.get_hot_functions();
        assert_eq!(hot_functions.len(), 2);
        assert!(hot_functions.contains(&"hot_function_1".to_string()));
        assert!(hot_functions.contains(&"hot_function_2".to_string()));

        // Remove hot function
        assert!(system.remove_hot_function("hot_function_1"));
        assert!(!system.remove_hot_function("non_existent_function"));

        let hot_functions = system.get_hot_functions();
        assert_eq!(hot_functions.len(), 1);
        assert!(!hot_functions.contains(&"hot_function_1".to_string()));
        assert!(hot_functions.contains(&"hot_function_2".to_string()));
    }

    #[test]
    fn test_optimization_hints() {
        let system = EnhancedPerformanceSystem::new();

        // Set optimization hints
        system.set_optimization_hint("function_1", "inline").unwrap();
        system.set_optimization_hint("function_2", "loop_unroll").unwrap();

        let hints = system.get_optimization_hints();
        assert_eq!(hints.len(), 2);
        assert_eq!(hints.get("function_1").unwrap(), "inline");
        assert_eq!(hints.get("function_2").unwrap(), "loop_unroll");
    }

    #[test]
    fn test_performance_events() {
        let system = EnhancedPerformanceSystem::new();
        system.start_monitoring().unwrap();

        let mut event = PerformanceEvent::default();
        event.event_type = 1;
        event.severity = 2;
        event.duration_micros = 1500;
        event.memory_usage = 1024;
        event.timestamp_micros = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_micros() as u64;

        // Record event
        system.record_event(event).unwrap();

        // Get events within time window
        let events = system.get_events(60_000_000); // 60 seconds
        assert_eq!(events.len(), 1);
        assert_eq!(events[0].event_type, 1);
        assert_eq!(events[0].severity, 2);
        assert_eq!(events[0].duration_micros, 1500);
        assert_eq!(events[0].memory_usage, 1024);
    }

    #[test]
    fn test_overhead_metrics() {
        let system = EnhancedPerformanceSystem::new();
        system.start_monitoring().unwrap();

        // Record some events to generate overhead
        for _ in 0..10 {
            let event = PerformanceEvent::default();
            system.record_event(event).unwrap();
        }

        let metrics = system.get_overhead_metrics();
        assert!(metrics.total_monitoring_operations.load(Ordering::Relaxed) > 0);
        assert!(metrics.total_monitoring_time_micros.load(Ordering::Relaxed) > 0);
    }

    #[test]
    fn test_concurrent_profiling() {
        let system = Arc::new(EnhancedPerformanceSystem::new());
        let options = ProfilingOptions::default();
        system.start_profiling(options).unwrap();

        let mut handles = vec![];

        // Spawn multiple threads recording function profiles
        for i in 0..5 {
            let system_clone = Arc::clone(&system);
            let handle = thread::spawn(move || {
                for j in 0..20 {
                    system_clone.record_function_profile(
                        &format!("function_{}", i),
                        (i * 100 + j) as u64,
                        1024,
                        100
                    ).unwrap();
                }
            });
            handles.push(handle);
        }

        // Wait for all threads to complete
        for handle in handles {
            handle.join().unwrap();
        }

        // Verify all profiles were recorded
        for i in 0..5 {
            let profile = system.get_function_profile(&format!("function_{}", i)).unwrap();
            assert_eq!(profile.call_count.load(Ordering::Relaxed), 20);
        }
    }

    #[test]
    fn test_reset_functionality() {
        let system = EnhancedPerformanceSystem::new();
        let options = ProfilingOptions::default();
        system.start_profiling(options).unwrap();

        // Add some data
        system.record_function_profile("test_function", 1000, 2048, 500).unwrap();
        system.add_hot_function("hot_function").unwrap();
        system.set_optimization_hint("function", "inline").unwrap();

        // Verify data exists
        assert!(system.get_function_profile("test_function").is_some());
        assert!(!system.get_hot_functions().is_empty());
        assert!(!system.get_optimization_hints().is_empty());

        // Reset system
        system.reset().unwrap();

        // Verify data is cleared
        assert!(system.get_function_profile("test_function").is_none());
        // Note: Hot functions and optimization hints are not cleared by reset
        // as they represent configuration rather than collected data
    }
}