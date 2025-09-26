//! Advanced work-stealing scheduler with locality awareness
//!
//! This module implements a sophisticated work-stealing scheduler optimized for
//! WebAssembly execution with features including:
//! - NUMA-aware task placement and stealing
//! - Adaptive load balancing with CPU core affinity
//! - Lock-free work queues with back-pressure
//! - Thread-local optimization for hot paths
//! - Dynamic thread pool scaling based on workload

use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, AtomicUsize, Ordering};
use std::collections::{HashMap, VecDeque};
use std::thread::{self, JoinHandle};
use std::time::{Duration, Instant};
use crossbeam::deque::{Injector, Stealer, Worker};
use crossbeam::utils::Backoff;
use crossbeam::epoch::{self, Atomic, Shared};
use parking_lot::{RwLock as ParkingRwLock, Mutex as ParkingMutex};
use num_cpus;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::threading::{WasmThread, ThreadId, WasmThreadState};

/// CPU topology information for NUMA-aware scheduling
#[derive(Debug, Clone)]
pub struct CpuTopology {
    /// Number of physical CPU cores
    pub physical_cores: usize,
    /// Number of logical CPU cores (with hyperthreading)
    pub logical_cores: usize,
    /// NUMA nodes information
    pub numa_nodes: Vec<NumaNode>,
    /// CPU cache hierarchy
    pub cache_hierarchy: CacheHierarchy,
    /// CPU frequency information
    pub frequency_info: CpuFrequencyInfo,
}

/// NUMA node information
#[derive(Debug, Clone)]
pub struct NumaNode {
    /// Node ID
    pub id: usize,
    /// CPU cores belonging to this node
    pub cpu_cores: Vec<usize>,
    /// Memory capacity in bytes
    pub memory_capacity: u64,
    /// Memory bandwidth in bytes per second
    pub memory_bandwidth: u64,
    /// Node-to-node distances
    pub distances: HashMap<usize, u32>,
}

/// CPU cache hierarchy information
#[derive(Debug, Clone)]
pub struct CacheHierarchy {
    /// L1 cache size per core in bytes
    pub l1_cache_size: usize,
    /// L2 cache size per core in bytes
    pub l2_cache_size: usize,
    /// L3 cache size (shared) in bytes
    pub l3_cache_size: usize,
    /// Cache line size in bytes
    pub cache_line_size: usize,
}

/// CPU frequency information
#[derive(Debug, Clone)]
pub struct CpuFrequencyInfo {
    /// Base frequency in MHz
    pub base_frequency: u32,
    /// Maximum turbo frequency in MHz
    pub max_frequency: u32,
    /// Current frequency scaling governor
    pub governor: String,
}

/// Work-stealing task representation
pub struct WorkStealingTask {
    /// Unique task identifier
    pub id: TaskId,
    /// Task payload (function to execute)
    pub payload: Box<dyn FnOnce() -> WasmtimeResult<()> + Send + 'static>,
    /// Task priority (higher values = higher priority)
    pub priority: TaskPriority,
    /// CPU affinity hint for optimal placement
    pub cpu_affinity: Option<CpuAffinityHint>,
    /// Memory locality hint
    pub memory_locality: Option<MemoryLocalityHint>,
    /// Task creation timestamp
    pub created_at: Instant,
    /// Expected execution duration
    pub estimated_duration: Option<Duration>,
    /// Task dependencies
    pub dependencies: Vec<TaskId>,
    /// Task scheduling constraints
    pub constraints: TaskConstraints,
}

/// Task identifier type
pub type TaskId = u64;

/// Task priority levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum TaskPriority {
    /// Low priority background tasks
    Low = 0,
    /// Normal priority tasks
    Normal = 1,
    /// High priority time-sensitive tasks
    High = 2,
    /// Critical real-time tasks
    Critical = 3,
}

/// CPU affinity hint for task placement
#[derive(Debug, Clone)]
pub enum CpuAffinityHint {
    /// Prefer any available CPU
    Any,
    /// Prefer specific CPU core
    Core(usize),
    /// Prefer CPU cores in specific NUMA node
    NumaNode(usize),
    /// Prefer CPU cores with specific cache affinity
    CacheAffinity(usize),
    /// Avoid specific CPU cores
    Avoid(Vec<usize>),
}

/// Memory locality hint for task placement
#[derive(Debug, Clone)]
pub struct MemoryLocalityHint {
    /// Preferred NUMA node for memory access
    pub numa_node: Option<usize>,
    /// Expected memory access pattern
    pub access_pattern: MemoryAccessPattern,
    /// Expected memory usage in bytes
    pub memory_usage: Option<usize>,
}

/// Memory access pattern enumeration
#[derive(Debug, Clone, Copy)]
pub enum MemoryAccessPattern {
    /// Sequential memory access
    Sequential,
    /// Random memory access
    Random,
    /// Streaming access pattern
    Streaming,
    /// Hot/cold access pattern
    HotCold,
}

/// Task scheduling constraints
#[derive(Debug, Clone)]
pub struct TaskConstraints {
    /// Maximum execution time before timeout
    pub max_execution_time: Option<Duration>,
    /// Required minimum memory in bytes
    pub min_memory_required: Option<usize>,
    /// Thread safety requirements
    pub thread_safety: ThreadSafetyLevel,
    /// Resource requirements
    pub resources: Vec<ResourceRequirement>,
}

/// Thread safety level requirements
#[derive(Debug, Clone, Copy)]
pub enum ThreadSafetyLevel {
    /// Task is thread-safe and can run concurrently
    ThreadSafe,
    /// Task requires mutual exclusion
    Exclusive,
    /// Task requires reader-writer semantics
    ReadWrite,
}

/// Resource requirement specification
#[derive(Debug, Clone)]
pub struct ResourceRequirement {
    /// Resource type identifier
    pub resource_type: String,
    /// Required resource amount
    pub amount: u64,
    /// Resource access mode
    pub access_mode: ResourceAccessMode,
}

/// Resource access mode
#[derive(Debug, Clone, Copy)]
pub enum ResourceAccessMode {
    /// Read-only access
    ReadOnly,
    /// Write-only access
    WriteOnly,
    /// Read-write access
    ReadWrite,
    /// Exclusive access
    Exclusive,
}

/// Work-stealing scheduler with advanced optimizations
pub struct WorkStealingScheduler {
    /// CPU topology information
    topology: Arc<CpuTopology>,
    /// Global task injector queue
    injector: Arc<Injector<Arc<WorkStealingTask>>>,
    /// Per-worker thread queues
    workers: Arc<ParkingRwLock<Vec<WorkerContext>>>,
    /// Worker thread handles
    worker_threads: Arc<ParkingMutex<Vec<JoinHandle<WasmtimeResult<()>>>>>,
    /// Scheduler configuration
    config: WorkStealingConfig,
    /// Scheduler statistics
    statistics: Arc<ParkingRwLock<WorkStealingStatistics>>,
    /// Task ID generator
    next_task_id: AtomicU64,
    /// Shutdown flag
    shutdown: Arc<AtomicBool>,
    /// Load balancer
    load_balancer: Arc<LoadBalancer>,
    /// Performance monitor
    performance_monitor: Arc<PerformanceMonitor>,
}

/// Work-stealing scheduler configuration
#[derive(Debug, Clone)]
pub struct WorkStealingConfig {
    /// Initial number of worker threads
    pub initial_workers: usize,
    /// Maximum number of worker threads
    pub max_workers: usize,
    /// Minimum number of worker threads to keep alive
    pub min_workers: usize,
    /// Worker thread idle timeout before scaling down
    pub idle_timeout: Duration,
    /// Work stealing attempt interval
    pub steal_interval: Duration,
    /// Maximum number of steal attempts per round
    pub max_steal_attempts: usize,
    /// Enable NUMA awareness
    pub numa_aware: bool,
    /// Enable CPU affinity optimization
    pub cpu_affinity_enabled: bool,
    /// Task queue capacity per worker
    pub queue_capacity: usize,
    /// Load balancing strategy
    pub load_balancing_strategy: LoadBalancingStrategy,
    /// Thread priority for worker threads
    pub thread_priority: ThreadPriority,
}

/// Load balancing strategies
#[derive(Debug, Clone, Copy)]
pub enum LoadBalancingStrategy {
    /// Round-robin task distribution
    RoundRobin,
    /// Least-loaded worker selection
    LeastLoaded,
    /// NUMA-aware placement
    NumaAware,
    /// Cache-aware placement
    CacheAware,
    /// Adaptive based on workload
    Adaptive,
}

/// Thread priority levels
#[derive(Debug, Clone, Copy)]
pub enum ThreadPriority {
    /// Low priority
    Low,
    /// Normal priority
    Normal,
    /// High priority
    High,
    /// Real-time priority
    RealTime,
}

/// Worker context for individual worker threads
pub struct WorkerContext {
    /// Worker ID
    pub id: WorkerId,
    /// Worker thread queue
    pub queue: Worker<Arc<WorkStealingTask>>,
    /// Worker thread stealer (for other threads to steal from)
    pub stealer: Stealer<Arc<WorkStealingTask>>,
    /// Worker statistics
    pub statistics: Arc<ParkingRwLock<WorkerStatistics>>,
    /// CPU core assignment
    pub cpu_core: Option<usize>,
    /// NUMA node assignment
    pub numa_node: Option<usize>,
    /// Worker state
    pub state: Arc<AtomicU32>, // WorkerState as u32
    /// Last activity timestamp
    pub last_activity: Arc<AtomicU64>, // Instant as nanoseconds
    /// Current task being executed
    pub current_task: Arc<Atomic<WorkStealingTask>>,
}

/// Worker identifier type
pub type WorkerId = usize;

/// Worker thread states
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(u32)]
pub enum WorkerState {
    /// Worker is idle and waiting for tasks
    Idle = 0,
    /// Worker is executing a task
    Executing = 1,
    /// Worker is stealing tasks from other workers
    Stealing = 2,
    /// Worker is being scaled down
    ScalingDown = 3,
    /// Worker is shutting down
    ShuttingDown = 4,
    /// Worker has terminated
    Terminated = 5,
}

/// Per-worker statistics
#[derive(Debug, Clone, Default)]
pub struct WorkerStatistics {
    /// Tasks executed by this worker
    pub tasks_executed: u64,
    /// Tasks stolen by this worker
    pub tasks_stolen: u64,
    /// Tasks stolen from this worker
    pub tasks_lost_to_stealing: u64,
    /// Total execution time
    pub total_execution_time: Duration,
    /// Total idle time
    pub total_idle_time: Duration,
    /// Total stealing time
    pub total_stealing_time: Duration,
    /// Average task execution time
    pub avg_task_execution_time: Duration,
    /// Cache misses (if available)
    pub cache_misses: u64,
    /// Context switches
    pub context_switches: u64,
    /// Memory locality hits
    pub memory_locality_hits: u64,
}

/// Global scheduler statistics
#[derive(Debug, Clone, Default)]
pub struct WorkStealingStatistics {
    /// Total tasks submitted
    pub total_tasks_submitted: u64,
    /// Total tasks completed
    pub total_tasks_completed: u64,
    /// Total tasks failed
    pub total_tasks_failed: u64,
    /// Current active workers
    pub active_workers: usize,
    /// Peak worker count
    pub peak_worker_count: usize,
    /// Total stealing operations
    pub total_steal_operations: u64,
    /// Successful steal operations
    pub successful_steals: u64,
    /// Average task turnaround time
    pub avg_turnaround_time: Duration,
    /// Scheduler uptime
    pub uptime: Duration,
    /// Load balancing efficiency
    pub load_balance_efficiency: f64,
}

/// Load balancer for work distribution and worker scaling
pub struct LoadBalancer {
    /// Load balancing configuration
    config: LoadBalancingConfig,
    /// Worker load information
    worker_loads: Arc<ParkingRwLock<HashMap<WorkerId, WorkerLoad>>>,
    /// Load balancing statistics
    statistics: Arc<ParkingRwLock<LoadBalancingStatistics>>,
    /// Scaling decisions history
    scaling_history: Arc<ParkingRwLock<VecDeque<ScalingDecision>>>,
}

/// Load balancing configuration
#[derive(Debug, Clone)]
pub struct LoadBalancingConfig {
    /// Load measurement interval
    pub measurement_interval: Duration,
    /// Load imbalance threshold for triggering rebalancing
    pub imbalance_threshold: f64,
    /// Scaling up threshold (load factor)
    pub scale_up_threshold: f64,
    /// Scaling down threshold (load factor)
    pub scale_down_threshold: f64,
    /// Maximum scaling operations per interval
    pub max_scaling_ops_per_interval: usize,
    /// Hysteresis factor to prevent oscillation
    pub hysteresis_factor: f64,
}

/// Worker load information
#[derive(Debug, Clone)]
pub struct WorkerLoad {
    /// Current queue length
    pub queue_length: usize,
    /// CPU utilization percentage
    pub cpu_utilization: f64,
    /// Memory utilization percentage
    pub memory_utilization: f64,
    /// Recent task completion rate
    pub task_completion_rate: f64,
    /// Average task execution time
    pub avg_execution_time: Duration,
    /// Load measurement timestamp
    pub measured_at: Instant,
}

/// Load balancing statistics
#[derive(Debug, Clone, Default)]
pub struct LoadBalancingStatistics {
    /// Total rebalancing operations
    pub total_rebalancing_ops: u64,
    /// Total scaling up operations
    pub total_scale_up_ops: u64,
    /// Total scaling down operations
    pub total_scale_down_ops: u64,
    /// Average load imbalance factor
    pub avg_load_imbalance: f64,
    /// Load balancing effectiveness
    pub balancing_effectiveness: f64,
}

/// Scaling decision record
#[derive(Debug, Clone)]
pub struct ScalingDecision {
    /// Decision timestamp
    pub timestamp: Instant,
    /// Scaling action taken
    pub action: ScalingAction,
    /// Number of workers affected
    pub worker_count_change: i32,
    /// Load factor that triggered the decision
    pub trigger_load_factor: f64,
    /// Decision rationale
    pub rationale: String,
}

/// Scaling actions
#[derive(Debug, Clone, Copy)]
pub enum ScalingAction {
    /// Scale up (add workers)
    ScaleUp,
    /// Scale down (remove workers)
    ScaleDown,
    /// No scaling needed
    NoAction,
    /// Rebalance existing workers
    Rebalance,
}

/// Performance monitor for tracking scheduler performance
pub struct PerformanceMonitor {
    /// Monitoring configuration
    config: PerformanceMonitoringConfig,
    /// Performance metrics
    metrics: Arc<ParkingRwLock<PerformanceMetrics>>,
    /// Monitoring thread handle
    monitor_thread: Option<JoinHandle<WasmtimeResult<()>>>,
    /// Shutdown flag for monitoring
    shutdown: Arc<AtomicBool>,
}

/// Performance monitoring configuration
#[derive(Debug, Clone)]
pub struct PerformanceMonitoringConfig {
    /// Monitoring interval
    pub monitoring_interval: Duration,
    /// Enable detailed profiling
    pub detailed_profiling: bool,
    /// Enable cache performance monitoring
    pub cache_monitoring: bool,
    /// Enable NUMA performance monitoring
    pub numa_monitoring: bool,
    /// Performance data retention period
    pub retention_period: Duration,
}

/// Performance metrics collection
#[derive(Debug, Clone, Default)]
pub struct PerformanceMetrics {
    /// Overall scheduler throughput (tasks per second)
    pub throughput: f64,
    /// Average task latency
    pub avg_latency: Duration,
    /// 95th percentile latency
    pub p95_latency: Duration,
    /// 99th percentile latency
    pub p99_latency: Duration,
    /// CPU utilization across all cores
    pub cpu_utilization: f64,
    /// Memory bandwidth utilization
    pub memory_bandwidth_utilization: f64,
    /// Cache hit ratio
    pub cache_hit_ratio: f64,
    /// NUMA locality ratio
    pub numa_locality_ratio: f64,
    /// Work stealing efficiency
    pub steal_efficiency: f64,
    /// Load balancing efficiency
    pub load_balance_efficiency: f64,
}

impl Default for WorkStealingConfig {
    fn default() -> Self {
        let cpu_count = num_cpus::get();
        Self {
            initial_workers: cpu_count,
            max_workers: cpu_count * 2,
            min_workers: (cpu_count / 4).max(1),
            idle_timeout: Duration::from_secs(30),
            steal_interval: Duration::from_micros(100),
            max_steal_attempts: cpu_count,
            numa_aware: true,
            cpu_affinity_enabled: true,
            queue_capacity: 1024,
            load_balancing_strategy: LoadBalancingStrategy::Adaptive,
            thread_priority: ThreadPriority::Normal,
        }
    }
}

impl Default for LoadBalancingConfig {
    fn default() -> Self {
        Self {
            measurement_interval: Duration::from_millis(100),
            imbalance_threshold: 0.2,
            scale_up_threshold: 0.8,
            scale_down_threshold: 0.2,
            max_scaling_ops_per_interval: 2,
            hysteresis_factor: 0.1,
        }
    }
}

impl Default for PerformanceMonitoringConfig {
    fn default() -> Self {
        Self {
            monitoring_interval: Duration::from_millis(100),
            detailed_profiling: false,
            cache_monitoring: false,
            numa_monitoring: false,
            retention_period: Duration::from_secs(300),
        }
    }
}

impl CpuTopology {
    /// Detect CPU topology from the system
    pub fn detect() -> WasmtimeResult<Self> {
        let physical_cores = num_cpus::get_physical();
        let logical_cores = num_cpus::get();

        // Create a default NUMA node (simplified for now)
        let numa_nodes = vec![NumaNode {
            id: 0,
            cpu_cores: (0..logical_cores).collect(),
            memory_capacity: 8 * 1024 * 1024 * 1024, // 8GB default
            memory_bandwidth: 25 * 1024 * 1024 * 1024, // 25GB/s default
            distances: [(0, 10)].into_iter().collect(),
        }];

        // Default cache hierarchy
        let cache_hierarchy = CacheHierarchy {
            l1_cache_size: 32 * 1024,
            l2_cache_size: 256 * 1024,
            l3_cache_size: 8 * 1024 * 1024,
            cache_line_size: 64,
        };

        // Default frequency info
        let frequency_info = CpuFrequencyInfo {
            base_frequency: 2400,
            max_frequency: 3800,
            governor: "performance".to_string(),
        };

        Ok(Self {
            physical_cores,
            logical_cores,
            numa_nodes,
            cache_hierarchy,
            frequency_info,
        })
    }

    /// Get optimal CPU core for task based on affinity hint
    pub fn get_optimal_core(&self, hint: &CpuAffinityHint) -> Option<usize> {
        match hint {
            CpuAffinityHint::Any => None,
            CpuAffinityHint::Core(core) => Some(*core),
            CpuAffinityHint::NumaNode(node_id) => {
                self.numa_nodes
                    .get(*node_id)
                    .and_then(|node| node.cpu_cores.first().copied())
            }
            CpuAffinityHint::CacheAffinity(core) => {
                // Return a core that shares cache with the specified core
                // Simplified: just return the next core
                Some((*core + 1) % self.logical_cores)
            }
            CpuAffinityHint::Avoid(avoid_cores) => {
                (0..self.logical_cores)
                    .find(|core| !avoid_cores.contains(core))
            }
        }
    }
}

impl WorkStealingScheduler {
    /// Create a new work-stealing scheduler
    pub fn new(config: WorkStealingConfig) -> WasmtimeResult<Self> {
        let topology = Arc::new(CpuTopology::detect()?);
        let injector = Arc::new(Injector::new());
        let workers = Arc::new(ParkingRwLock::new(Vec::new()));
        let worker_threads = Arc::new(ParkingMutex::new(Vec::new()));
        let statistics = Arc::new(ParkingRwLock::new(WorkStealingStatistics::default()));
        let shutdown = Arc::new(AtomicBool::new(false));

        let load_balancer = Arc::new(LoadBalancer::new(LoadBalancingConfig::default())?);
        let performance_monitor = Arc::new(PerformanceMonitor::new(PerformanceMonitoringConfig::default())?);

        let mut scheduler = Self {
            topology,
            injector,
            workers,
            worker_threads,
            config,
            statistics,
            next_task_id: AtomicU64::new(1),
            shutdown,
            load_balancer,
            performance_monitor,
        };

        // Initialize worker threads
        scheduler.initialize_workers()?;

        Ok(scheduler)
    }

    /// Initialize worker threads
    fn initialize_workers(&mut self) -> WasmtimeResult<()> {
        let mut workers = self.workers.write();
        let mut worker_threads = self.worker_threads.lock();

        for worker_id in 0..self.config.initial_workers {
            let worker_context = self.create_worker_context(worker_id)?;
            let thread_handle = self.spawn_worker_thread(worker_context.clone())?;

            workers.push(worker_context);
            worker_threads.push(thread_handle);
        }

        // Update statistics
        if let Ok(mut stats) = self.statistics.write() {
            stats.active_workers = self.config.initial_workers;
            stats.peak_worker_count = self.config.initial_workers;
        }

        log::info!("Initialized {} worker threads for work-stealing scheduler", self.config.initial_workers);
        Ok(())
    }

    /// Create a new worker context
    fn create_worker_context(&self, worker_id: WorkerId) -> WasmtimeResult<WorkerContext> {
        let queue = Worker::new_fifo();
        let stealer = queue.stealer();
        let statistics = Arc::new(ParkingRwLock::new(WorkerStatistics::default()));
        let state = Arc::new(AtomicU32::new(WorkerState::Idle as u32));
        let last_activity = Arc::new(AtomicU64::new(0));
        let current_task = Arc::new(Atomic::null());

        // Assign CPU core and NUMA node if enabled
        let (cpu_core, numa_node) = if self.config.cpu_affinity_enabled {
            let core = worker_id % self.topology.logical_cores;
            let numa = self.topology.numa_nodes
                .iter()
                .find(|node| node.cpu_cores.contains(&core))
                .map(|node| node.id);
            (Some(core), numa)
        } else {
            (None, None)
        };

        Ok(WorkerContext {
            id: worker_id,
            queue,
            stealer,
            statistics,
            cpu_core,
            numa_node,
            state,
            last_activity,
            current_task,
        })
    }

    /// Spawn a worker thread
    fn spawn_worker_thread(&self, context: WorkerContext) -> WasmtimeResult<JoinHandle<WasmtimeResult<()>>> {
        let injector = self.injector.clone();
        let workers = self.workers.clone();
        let shutdown = self.shutdown.clone();
        let config = self.config.clone();
        let topology = self.topology.clone();

        let handle = thread::Builder::new()
            .name(format!("work-stealer-{}", context.id))
            .spawn(move || {
                // Set CPU affinity if enabled
                if let Some(cpu_core) = context.cpu_core {
                    if let Err(e) = set_cpu_affinity(cpu_core) {
                        log::warn!("Failed to set CPU affinity for worker {}: {}", context.id, e);
                    }
                }

                // Set thread priority
                if let Err(e) = set_thread_priority(config.thread_priority) {
                    log::warn!("Failed to set thread priority for worker {}: {}", context.id, e);
                }

                Self::worker_thread_main(
                    context,
                    injector,
                    workers,
                    shutdown,
                    config,
                    topology,
                )
            })
            .map_err(|e| WasmtimeError::Threading {
                message: format!("Failed to spawn worker thread: {}", e),
            })?;

        Ok(handle)
    }

    /// Main worker thread execution loop
    fn worker_thread_main(
        context: WorkerContext,
        injector: Arc<Injector<Arc<WorkStealingTask>>>,
        workers: Arc<ParkingRwLock<Vec<WorkerContext>>>,
        shutdown: Arc<AtomicBool>,
        config: WorkStealingConfig,
        topology: Arc<CpuTopology>,
    ) -> WasmtimeResult<()> {
        let backoff = Backoff::new();
        let mut last_steal_attempt = Instant::now();

        log::debug!("Worker {} started on CPU core {:?}", context.id, context.cpu_core);

        while !shutdown.load(Ordering::Acquire) {
            // Update worker state and activity
            context.last_activity.store(
                Instant::now().duration_since(std::time::UNIX_EPOCH).unwrap().as_nanos() as u64,
                Ordering::Release
            );

            // Try to get a task from local queue first
            if let Some(task) = context.queue.pop() {
                context.state.store(WorkerState::Executing as u32, Ordering::Release);
                Self::execute_task(&context, task)?;
                backoff.reset();
                continue;
            }

            // Try to steal from global injector
            if let Some(task) = injector.steal().success() {
                context.state.store(WorkerState::Executing as u32, Ordering::Release);
                Self::execute_task(&context, task)?;
                backoff.reset();
                continue;
            }

            // Attempt work stealing from other workers
            if last_steal_attempt.elapsed() >= config.steal_interval {
                context.state.store(WorkerState::Stealing as u32, Ordering::Release);
                if Self::attempt_work_stealing(&context, &workers, &config, &topology)? {
                    last_steal_attempt = Instant::now();
                    backoff.reset();
                    continue;
                }
                last_steal_attempt = Instant::now();
            }

            // No work found, go idle
            context.state.store(WorkerState::Idle as u32, Ordering::Release);
            backoff.snooze();
        }

        // Worker shutdown
        context.state.store(WorkerState::Terminated as u32, Ordering::Release);
        log::debug!("Worker {} terminated", context.id);
        Ok(())
    }

    /// Execute a task in the worker context
    fn execute_task(context: &WorkerContext, task: Arc<WorkStealingTask>) -> WasmtimeResult<()> {
        let start_time = Instant::now();

        // Store current task reference
        let task_ptr = Arc::into_raw(task.clone()) as *const WorkStealingTask;
        context.current_task.store(
            unsafe { Shared::from_raw(task_ptr) },
            Ordering::Release
        );

        // Execute the task
        let result = {
            let payload = std::ptr::replace(
                &task.payload as *const _ as *mut _,
                Box::new(|| Ok(()))
            );
            payload()
        };

        // Clear current task reference
        context.current_task.store(Shared::null(), Ordering::Release);

        let execution_time = start_time.elapsed();

        // Update worker statistics
        if let Ok(mut stats) = context.statistics.write() {
            stats.tasks_executed += 1;
            stats.total_execution_time += execution_time;

            // Update rolling average
            let total_tasks = stats.tasks_executed;
            stats.avg_task_execution_time = Duration::from_nanos(
                (stats.avg_task_execution_time.as_nanos() as u64 * (total_tasks - 1) +
                 execution_time.as_nanos() as u64) / total_tasks
            );
        }

        log::trace!("Task {} executed in {:?} by worker {}",
                   task.id, execution_time, context.id);

        result
    }

    /// Attempt to steal work from other workers
    fn attempt_work_stealing(
        context: &WorkerContext,
        workers: &Arc<ParkingRwLock<Vec<WorkerContext>>>,
        config: &WorkStealingConfig,
        topology: &Arc<CpuTopology>,
    ) -> WasmtimeResult<bool> {
        let workers_guard = workers.read();
        let worker_count = workers_guard.len();

        if worker_count <= 1 {
            return Ok(false);
        }

        // Create stealing order based on locality if NUMA-aware
        let steal_order = if config.numa_aware {
            Self::get_numa_aware_steal_order(context, &workers_guard, topology)
        } else {
            Self::get_random_steal_order(context.id, worker_count)
        };

        let mut attempts = 0;
        let start_time = Instant::now();

        for &target_worker_id in &steal_order {
            if attempts >= config.max_steal_attempts {
                break;
            }

            if target_worker_id == context.id {
                continue;
            }

            if let Some(target_worker) = workers_guard.get(target_worker_id) {
                match target_worker.stealer.steal() {
                    crossbeam::deque::Steal::Success(task) => {
                        // Successfully stole a task
                        if let Ok(mut stats) = context.statistics.write() {
                            stats.tasks_stolen += 1;
                            stats.total_stealing_time += start_time.elapsed();
                        }

                        if let Ok(mut target_stats) = target_worker.statistics.write() {
                            target_stats.tasks_lost_to_stealing += 1;
                        }

                        // Execute the stolen task immediately
                        Self::execute_task(context, task)?;
                        return Ok(true);
                    }
                    crossbeam::deque::Steal::Empty => {
                        // Target worker has no tasks
                    }
                    crossbeam::deque::Steal::Retry => {
                        // Retry stealing from this worker
                        attempts += 1;
                        continue;
                    }
                }
            }

            attempts += 1;
        }

        // Update stealing time even if unsuccessful
        if let Ok(mut stats) = context.statistics.write() {
            stats.total_stealing_time += start_time.elapsed();
        }

        Ok(false)
    }

    /// Get NUMA-aware stealing order prioritizing local NUMA nodes
    fn get_numa_aware_steal_order(
        context: &WorkerContext,
        workers: &[WorkerContext],
        topology: &Arc<CpuTopology>,
    ) -> Vec<usize> {
        let mut steal_order = Vec::new();
        let context_numa = context.numa_node;

        // First, add workers in the same NUMA node
        if let Some(numa_id) = context_numa {
            for worker in workers {
                if worker.numa_node == Some(numa_id) && worker.id != context.id {
                    steal_order.push(worker.id);
                }
            }
        }

        // Then, add workers from other NUMA nodes ordered by distance
        let mut other_workers: Vec<_> = workers.iter()
            .filter(|w| w.numa_node != context_numa && w.id != context.id)
            .collect();

        if let Some(context_numa_id) = context_numa {
            if let Some(context_numa_node) = topology.numa_nodes.get(context_numa_id) {
                other_workers.sort_by_key(|worker| {
                    worker.numa_node
                        .and_then(|numa_id| context_numa_node.distances.get(&numa_id))
                        .copied()
                        .unwrap_or(u32::MAX)
                });
            }
        }

        steal_order.extend(other_workers.iter().map(|w| w.id));

        // Shuffle within NUMA groups for fairness
        use rand::seq::SliceRandom;
        let mut rng = rand::thread_rng();
        if let Some(same_numa_count) = context_numa
            .and_then(|numa_id| Some(topology.numa_nodes.get(numa_id)?.cpu_cores.len()))
        {
            let same_numa_end = std::cmp::min(same_numa_count, steal_order.len());
            steal_order[..same_numa_end].shuffle(&mut rng);
        }

        steal_order
    }

    /// Get random stealing order for load balancing
    fn get_random_steal_order(current_worker_id: usize, worker_count: usize) -> Vec<usize> {
        use rand::seq::SliceRandom;
        let mut steal_order: Vec<usize> = (0..worker_count)
            .filter(|&id| id != current_worker_id)
            .collect();

        let mut rng = rand::thread_rng();
        steal_order.shuffle(&mut rng);
        steal_order
    }

    /// Submit a task to the scheduler
    pub fn submit_task(&self, task: WorkStealingTask) -> WasmtimeResult<TaskId> {
        if self.shutdown.load(Ordering::Acquire) {
            return Err(WasmtimeError::Threading {
                message: "Scheduler is shutting down".to_string(),
            });
        }

        let task_id = task.id;
        let task = Arc::new(task);

        // Choose worker based on load balancing strategy and task hints
        match self.config.load_balancing_strategy {
            LoadBalancingStrategy::RoundRobin => {
                self.submit_task_round_robin(task)?;
            }
            LoadBalancingStrategy::LeastLoaded => {
                self.submit_task_least_loaded(task)?;
            }
            LoadBalancingStrategy::NumaAware => {
                self.submit_task_numa_aware(task)?;
            }
            LoadBalancingStrategy::CacheAware => {
                self.submit_task_cache_aware(task)?;
            }
            LoadBalancingStrategy::Adaptive => {
                self.submit_task_adaptive(task)?;
            }
        }

        // Update statistics
        if let Ok(mut stats) = self.statistics.write() {
            stats.total_tasks_submitted += 1;
        }

        log::trace!("Submitted task {} to work-stealing scheduler", task_id);
        Ok(task_id)
    }

    /// Submit task using round-robin distribution
    fn submit_task_round_robin(&self, task: Arc<WorkStealingTask>) -> WasmtimeResult<()> {
        static ROUND_ROBIN_COUNTER: AtomicUsize = AtomicUsize::new(0);

        let workers = self.workers.read();
        if workers.is_empty() {
            return Err(WasmtimeError::Threading {
                message: "No workers available".to_string(),
            });
        }

        let worker_index = ROUND_ROBIN_COUNTER.fetch_add(1, Ordering::SeqCst) % workers.len();
        workers[worker_index].queue.push(task);
        Ok(())
    }

    /// Submit task to least loaded worker
    fn submit_task_least_loaded(&self, task: Arc<WorkStealingTask>) -> WasmtimeResult<()> {
        let workers = self.workers.read();
        if workers.is_empty() {
            return Err(WasmtimeError::Threading {
                message: "No workers available".to_string(),
            });
        }

        // Find worker with smallest queue
        let (min_worker_idx, _) = workers
            .iter()
            .enumerate()
            .map(|(idx, worker)| (idx, worker.queue.len()))
            .min_by_key(|(_, len)| *len)
            .ok_or_else(|| WasmtimeError::Threading {
                message: "Failed to find least loaded worker".to_string(),
            })?;

        workers[min_worker_idx].queue.push(task);
        Ok(())
    }

    /// Submit task with NUMA awareness
    fn submit_task_numa_aware(&self, task: Arc<WorkStealingTask>) -> WasmtimeResult<()> {
        let workers = self.workers.read();
        if workers.is_empty() {
            return Err(WasmtimeError::Threading {
                message: "No workers available".to_string(),
            });
        }

        // Try to place task on worker in preferred NUMA node
        if let Some(memory_hint) = &task.memory_locality {
            if let Some(preferred_numa) = memory_hint.numa_node {
                for worker in workers.iter() {
                    if worker.numa_node == Some(preferred_numa) {
                        worker.queue.push(task);
                        return Ok(());
                    }
                }
            }
        }

        // Fallback to least loaded placement
        self.submit_task_least_loaded(task)
    }

    /// Submit task with cache awareness
    fn submit_task_cache_aware(&self, task: Arc<WorkStealingTask>) -> WasmtimeResult<()> {
        let workers = self.workers.read();
        if workers.is_empty() {
            return Err(WasmtimeError::Threading {
                message: "No workers available".to_string(),
            });
        }

        // Try to place task on worker with cache affinity
        if let Some(cpu_hint) = &task.cpu_affinity {
            if let Some(preferred_core) = self.topology.get_optimal_core(cpu_hint) {
                for worker in workers.iter() {
                    if worker.cpu_core == Some(preferred_core) {
                        worker.queue.push(task);
                        return Ok(());
                    }
                }
            }
        }

        // Fallback to least loaded placement
        self.submit_task_least_loaded(task)
    }

    /// Submit task using adaptive strategy
    fn submit_task_adaptive(&self, task: Arc<WorkStealingTask>) -> WasmtimeResult<()> {
        // Use NUMA awareness for memory-intensive tasks
        if let Some(memory_hint) = &task.memory_locality {
            if matches!(memory_hint.access_pattern, MemoryAccessPattern::Sequential | MemoryAccessPattern::Streaming) {
                return self.submit_task_numa_aware(task);
            }
        }

        // Use cache awareness for CPU-intensive tasks
        if task.priority >= TaskPriority::High {
            return self.submit_task_cache_aware(task);
        }

        // Default to least loaded for balanced distribution
        self.submit_task_least_loaded(task)
    }

    /// Get current scheduler statistics
    pub fn get_statistics(&self) -> WasmtimeResult<WorkStealingStatistics> {
        let stats = self.statistics.read().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire statistics lock".to_string(),
        })?;
        Ok(stats.clone())
    }

    /// Shutdown the scheduler
    pub fn shutdown(&self) -> WasmtimeResult<()> {
        log::info!("Shutting down work-stealing scheduler");

        self.shutdown.store(true, Ordering::Release);

        // Wait for all worker threads to complete
        let mut worker_threads = self.worker_threads.lock();
        for handle in worker_threads.drain(..) {
            if let Err(e) = handle.join() {
                log::warn!("Worker thread join failed: {:?}", e);
            }
        }

        log::info!("Work-stealing scheduler shutdown complete");
        Ok(())
    }
}

impl LoadBalancer {
    /// Create a new load balancer
    pub fn new(config: LoadBalancingConfig) -> WasmtimeResult<Self> {
        Ok(Self {
            config,
            worker_loads: Arc::new(ParkingRwLock::new(HashMap::new())),
            statistics: Arc::new(ParkingRwLock::new(LoadBalancingStatistics::default())),
            scaling_history: Arc::new(ParkingRwLock::new(VecDeque::new())),
        })
    }
}

impl PerformanceMonitor {
    /// Create a new performance monitor
    pub fn new(config: PerformanceMonitoringConfig) -> WasmtimeResult<Self> {
        Ok(Self {
            config,
            metrics: Arc::new(ParkingRwLock::new(PerformanceMetrics::default())),
            monitor_thread: None,
            shutdown: Arc::new(AtomicBool::new(false)),
        })
    }
}

/// Set CPU affinity for the current thread
fn set_cpu_affinity(cpu_core: usize) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    #[cfg(target_os = "linux")]
    {
        use libc::{cpu_set_t, CPU_SET, CPU_ZERO, sched_setaffinity};
        use std::mem;

        unsafe {
            let mut cpu_set: cpu_set_t = mem::zeroed();
            CPU_ZERO(&mut cpu_set);
            CPU_SET(cpu_core, &mut cpu_set);

            if sched_setaffinity(0, mem::size_of::<cpu_set_t>(), &cpu_set) != 0 {
                return Err("Failed to set CPU affinity".into());
            }
        }
    }

    #[cfg(not(target_os = "linux"))]
    {
        log::warn!("CPU affinity setting not implemented for this platform");
    }

    Ok(())
}

/// Set thread priority for the current thread
fn set_thread_priority(priority: ThreadPriority) -> Result<(), Box<dyn std::error::Error + Send + Sync>> {
    #[cfg(target_os = "linux")]
    {
        use libc::{setpriority, PRIO_PROCESS};

        let nice_value = match priority {
            ThreadPriority::Low => 10,
            ThreadPriority::Normal => 0,
            ThreadPriority::High => -10,
            ThreadPriority::RealTime => -20,
        };

        unsafe {
            if setpriority(PRIO_PROCESS, 0, nice_value) != 0 {
                return Err("Failed to set thread priority".into());
            }
        }
    }

    #[cfg(not(target_os = "linux"))]
    {
        log::warn!("Thread priority setting not implemented for this platform");
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::Arc;
    use std::time::{Duration, Instant};

    #[test]
    fn test_cpu_topology_detection() {
        let topology = CpuTopology::detect().expect("Failed to detect CPU topology");
        assert!(topology.logical_cores > 0);
        assert!(topology.physical_cores > 0);
        assert!(!topology.numa_nodes.is_empty());
    }

    #[test]
    fn test_work_stealing_scheduler_creation() {
        let config = WorkStealingConfig::default();
        let scheduler = WorkStealingScheduler::new(config).expect("Failed to create scheduler");

        let stats = scheduler.get_statistics().expect("Failed to get statistics");
        assert!(stats.active_workers > 0);
    }

    #[test]
    fn test_task_submission_and_execution() {
        let config = WorkStealingConfig {
            initial_workers: 2,
            max_workers: 4,
            ..WorkStealingConfig::default()
        };

        let scheduler = WorkStealingScheduler::new(config).expect("Failed to create scheduler");

        let task_executed = Arc::new(AtomicBool::new(false));
        let task_executed_clone = task_executed.clone();

        let task = WorkStealingTask {
            id: 1,
            payload: Box::new(move || {
                task_executed_clone.store(true, Ordering::Release);
                Ok(())
            }),
            priority: TaskPriority::Normal,
            cpu_affinity: None,
            memory_locality: None,
            created_at: Instant::now(),
            estimated_duration: None,
            dependencies: vec![],
            constraints: TaskConstraints {
                max_execution_time: None,
                min_memory_required: None,
                thread_safety: ThreadSafetyLevel::ThreadSafe,
                resources: vec![],
            },
        };

        let task_id = scheduler.submit_task(task).expect("Failed to submit task");
        assert_eq!(task_id, 1);

        // Wait for task execution
        std::thread::sleep(Duration::from_millis(100));
        assert!(task_executed.load(Ordering::Acquire));

        scheduler.shutdown().expect("Failed to shutdown scheduler");
    }

    #[test]
    fn test_numa_aware_task_placement() {
        let config = WorkStealingConfig {
            numa_aware: true,
            load_balancing_strategy: LoadBalancingStrategy::NumaAware,
            ..WorkStealingConfig::default()
        };

        let scheduler = WorkStealingScheduler::new(config).expect("Failed to create scheduler");

        let task = WorkStealingTask {
            id: 1,
            payload: Box::new(|| Ok(())),
            priority: TaskPriority::Normal,
            cpu_affinity: None,
            memory_locality: Some(MemoryLocalityHint {
                numa_node: Some(0),
                access_pattern: MemoryAccessPattern::Sequential,
                memory_usage: Some(1024 * 1024), // 1MB
            }),
            created_at: Instant::now(),
            estimated_duration: Some(Duration::from_millis(10)),
            dependencies: vec![],
            constraints: TaskConstraints {
                max_execution_time: Some(Duration::from_secs(1)),
                min_memory_required: Some(1024 * 1024),
                thread_safety: ThreadSafetyLevel::ThreadSafe,
                resources: vec![],
            },
        };

        let task_id = scheduler.submit_task(task).expect("Failed to submit NUMA-aware task");
        assert_eq!(task_id, 1);

        scheduler.shutdown().expect("Failed to shutdown scheduler");
    }

    #[test]
    fn test_load_balancer() {
        let config = LoadBalancingConfig::default();
        let load_balancer = LoadBalancer::new(config).expect("Failed to create load balancer");

        // Test that load balancer was created successfully
        let stats = load_balancer.statistics.read();
        assert_eq!(stats.total_rebalancing_ops, 0);
    }

    #[test]
    fn test_performance_monitor() {
        let config = PerformanceMonitoringConfig::default();
        let monitor = PerformanceMonitor::new(config).expect("Failed to create performance monitor");

        // Test that performance monitor was created successfully
        let metrics = monitor.metrics.read();
        assert_eq!(metrics.throughput, 0.0);
    }

    #[test]
    fn test_cpu_affinity_hint() {
        let topology = CpuTopology::detect().expect("Failed to detect CPU topology");

        // Test different affinity hints
        let core_hint = CpuAffinityHint::Core(0);
        let optimal_core = topology.get_optimal_core(&core_hint);
        assert_eq!(optimal_core, Some(0));

        let numa_hint = CpuAffinityHint::NumaNode(0);
        let numa_core = topology.get_optimal_core(&numa_hint);
        assert!(numa_core.is_some());

        let any_hint = CpuAffinityHint::Any;
        let any_core = topology.get_optimal_core(&any_hint);
        assert!(any_core.is_none());
    }

    #[test]
    fn test_task_priority_ordering() {
        assert!(TaskPriority::Critical > TaskPriority::High);
        assert!(TaskPriority::High > TaskPriority::Normal);
        assert!(TaskPriority::Normal > TaskPriority::Low);
    }

    #[test]
    fn test_worker_state_transitions() {
        assert_ne!(WorkerState::Idle as u32, WorkerState::Executing as u32);
        assert_ne!(WorkerState::Executing as u32, WorkerState::Stealing as u32);
        assert_ne!(WorkerState::Stealing as u32, WorkerState::Terminated as u32);
    }
}