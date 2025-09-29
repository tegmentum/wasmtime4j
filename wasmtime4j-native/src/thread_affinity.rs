//! Thread affinity management and CPU core binding optimization
//!
//! This module provides advanced thread affinity management for optimal WebAssembly
//! execution performance, including:
//! - CPU core binding with NUMA awareness
//! - Dynamic affinity adjustment based on workload
//! - Cache-optimal thread placement
//! - Hyper-threading optimization
//! - CPU frequency scaling coordination

use std::sync::{Arc, RwLock};
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, Ordering};
use std::collections::{HashMap, HashSet, BTreeMap};
use std::thread;
use std::time::{Duration, Instant};
use parking_lot::{RwLock as ParkingRwLock, Mutex as ParkingMutex};
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::work_stealing::{CpuTopology, NumaNode, CacheHierarchy};

/// Thread affinity manager for optimal CPU core binding
pub struct ThreadAffinityManager {
    /// CPU topology information
    topology: Arc<CpuTopology>,
    /// Affinity configuration
    config: AffinityConfig,
    /// CPU core assignments
    core_assignments: Arc<ParkingRwLock<HashMap<thread::ThreadId, CoreAssignment>>>,
    /// CPU core utilization tracking
    core_utilization: Arc<ParkingRwLock<HashMap<usize, CoreUtilization>>>,
    /// Affinity policies
    policies: Arc<ParkingRwLock<Vec<AffinityPolicy>>>,
    /// Performance counters
    performance_counters: Arc<ParkingRwLock<PerformanceCounters>>,
    /// Dynamic adjustment enabled
    dynamic_adjustment: Arc<AtomicBool>,
    /// Management thread handle
    manager_thread: Option<thread::JoinHandle<WasmtimeResult<()>>>,
    /// Shutdown flag
    shutdown: Arc<AtomicBool>,
}

/// Thread affinity configuration
#[derive(Debug, Clone)]
pub struct AffinityConfig {
    /// Enable automatic CPU core binding
    pub auto_binding_enabled: bool,
    /// Enable NUMA-aware placement
    pub numa_aware_placement: bool,
    /// Enable hyper-threading optimization
    pub hyperthreading_optimization: bool,
    /// Enable cache-optimal placement
    pub cache_optimal_placement: bool,
    /// Enable dynamic affinity adjustment
    pub dynamic_adjustment_enabled: bool,
    /// Minimum core utilization threshold for rebalancing
    pub rebalancing_threshold: f64,
    /// Affinity adjustment interval
    pub adjustment_interval: Duration,
    /// Maximum number of thread migrations per interval
    pub max_migrations_per_interval: usize,
    /// Core assignment strategy
    pub assignment_strategy: CoreAssignmentStrategy,
    /// Hysteresis factor to prevent oscillation
    pub hysteresis_factor: f64,
}

/// Core assignment strategies
#[derive(Debug, Clone, Copy)]
pub enum CoreAssignmentStrategy {
    /// First available core
    FirstAvailable,
    /// Round-robin assignment
    RoundRobin,
    /// Least loaded core
    LeastLoaded,
    /// NUMA-aware assignment
    NumaAware,
    /// Cache-optimal assignment
    CacheOptimal,
    /// Performance-guided assignment
    PerformanceGuided,
    /// Adaptive based on workload
    Adaptive,
}

/// CPU core assignment information
#[derive(Debug, Clone)]
pub struct CoreAssignment {
    /// Assigned CPU core ID
    pub core_id: usize,
    /// NUMA node of the assigned core
    pub numa_node: usize,
    /// Assignment timestamp
    pub assigned_at: Instant,
    /// Assignment reason
    pub assignment_reason: AssignmentReason,
    /// Thread priority
    pub thread_priority: ThreadPriority,
    /// Performance hint
    pub performance_hint: Option<PerformanceHint>,
    /// Binding strength
    pub binding_strength: BindingStrength,
    /// Migration count
    pub migration_count: u32,
    /// Last migration timestamp
    pub last_migration: Option<Instant>,
}

/// Reasons for core assignment
#[derive(Debug, Clone, Copy)]
pub enum AssignmentReason {
    /// Initial assignment
    Initial,
    /// Load balancing
    LoadBalancing,
    /// NUMA optimization
    NumaOptimization,
    /// Cache optimization
    CacheOptimization,
    /// Performance optimization
    PerformanceOptimization,
    /// Thermal management
    ThermalManagement,
    /// Power management
    PowerManagement,
    /// User request
    UserRequest,
}

/// Thread priority levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum ThreadPriority {
    /// Background/low priority
    Background,
    /// Normal priority
    Normal,
    /// High priority
    High,
    /// Real-time priority
    RealTime,
}

/// Performance hints for thread placement
#[derive(Debug, Clone)]
pub struct PerformanceHint {
    /// Expected CPU utilization (0.0 - 1.0)
    pub cpu_utilization: Option<f64>,
    /// Memory access pattern
    pub memory_pattern: Option<MemoryAccessPattern>,
    /// Cache sensitivity level
    pub cache_sensitivity: CacheSensitivity,
    /// NUMA locality preference
    pub numa_preference: NumaPreference,
    /// Frequency requirement
    pub frequency_requirement: FrequencyRequirement,
}

/// Memory access patterns for optimization
#[derive(Debug, Clone, Copy)]
pub enum MemoryAccessPattern {
    /// Sequential access pattern
    Sequential,
    /// Random access pattern
    Random,
    /// Streaming pattern
    Streaming,
    /// Sparse access pattern
    Sparse,
    /// Locality-heavy pattern
    LocalityHeavy,
}

/// Cache sensitivity levels
#[derive(Debug, Clone, Copy)]
pub enum CacheSensitivity {
    /// Low cache sensitivity
    Low,
    /// Medium cache sensitivity
    Medium,
    /// High cache sensitivity
    High,
    /// Critical cache sensitivity
    Critical,
}

/// NUMA locality preferences
#[derive(Debug, Clone, Copy)]
pub enum NumaPreference {
    /// No NUMA preference
    None,
    /// Prefer local NUMA node
    Local,
    /// Prefer specific NUMA node
    Specific(usize),
    /// Avoid specific NUMA node
    Avoid(usize),
}

/// CPU frequency requirements
#[derive(Debug, Clone, Copy)]
pub enum FrequencyRequirement {
    /// No specific requirement
    None,
    /// Minimum frequency required (MHz)
    Minimum(u32),
    /// Maximum frequency for power efficiency
    Maximum(u32),
    /// Optimal frequency range
    Range(u32, u32),
}

/// Core binding strength levels
#[derive(Debug, Clone, Copy)]
pub enum BindingStrength {
    /// Soft binding (hint only)
    Soft,
    /// Normal binding
    Normal,
    /// Strong binding (avoid migration)
    Strong,
    /// Pinned (prevent migration)
    Pinned,
}

/// CPU core utilization tracking
#[derive(Debug, Clone)]
pub struct CoreUtilization {
    /// Core ID
    pub core_id: usize,
    /// Current utilization percentage (0.0 - 1.0)
    pub utilization: f64,
    /// Number of threads assigned
    pub thread_count: usize,
    /// Average task execution time
    pub avg_execution_time: Duration,
    /// Cache hit ratio
    pub cache_hit_ratio: f64,
    /// Memory bandwidth utilization
    pub memory_bandwidth_utilization: f64,
    /// Temperature (Celsius)
    pub temperature: Option<f32>,
    /// Current frequency (MHz)
    pub current_frequency: Option<u32>,
    /// Last update timestamp
    pub last_updated: Instant,
    /// Performance score (0.0 - 1.0)
    pub performance_score: f64,
}

/// Affinity policy for automatic management
#[derive(Debug, Clone)]
pub struct AffinityPolicy {
    /// Policy name
    pub name: String,
    /// Policy priority
    pub priority: PolicyPriority,
    /// Matching criteria
    pub criteria: PolicyCriteria,
    /// Assignment action
    pub action: PolicyAction,
    /// Policy conditions
    pub conditions: Vec<PolicyCondition>,
    /// Policy enabled flag
    pub enabled: bool,
    /// Policy statistics
    pub statistics: PolicyStatistics,
}

/// Policy priority levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum PolicyPriority {
    /// Low priority
    Low,
    /// Normal priority
    Normal,
    /// High priority
    High,
    /// Critical priority
    Critical,
}

/// Policy matching criteria
#[derive(Debug, Clone)]
pub struct PolicyCriteria {
    /// Thread name pattern
    pub thread_name_pattern: Option<String>,
    /// Thread priority requirement
    pub thread_priority: Option<ThreadPriority>,
    /// Performance hint requirements
    pub performance_hints: Vec<PerformanceHint>,
    /// CPU utilization range
    pub cpu_utilization_range: Option<(f64, f64)>,
    /// Memory usage range
    pub memory_usage_range: Option<(usize, usize)>,
}

/// Policy actions
#[derive(Debug, Clone)]
pub enum PolicyAction {
    /// Assign to specific core
    AssignToCore(usize),
    /// Assign to NUMA node
    AssignToNumaNode(usize),
    /// Assign using strategy
    AssignUsingStrategy(CoreAssignmentStrategy),
    /// Prevent assignment to cores
    AvoidCores(Vec<usize>),
    /// Set binding strength
    SetBindingStrength(BindingStrength),
    /// Set thread priority
    SetThreadPriority(ThreadPriority),
}

/// Policy conditions
#[derive(Debug, Clone)]
pub struct PolicyCondition {
    /// Condition type
    pub condition_type: ConditionType,
    /// Threshold value
    pub threshold: f64,
    /// Comparison operator
    pub operator: ComparisonOperator,
    /// Condition enabled flag
    pub enabled: bool,
}

/// Policy condition types
#[derive(Debug, Clone, Copy)]
pub enum ConditionType {
    /// CPU utilization condition
    CpuUtilization,
    /// Memory utilization condition
    MemoryUtilization,
    /// Cache hit ratio condition
    CacheHitRatio,
    /// Temperature condition
    Temperature,
    /// Thread count condition
    ThreadCount,
    /// Performance score condition
    PerformanceScore,
}

/// Comparison operators for conditions
#[derive(Debug, Clone, Copy)]
pub enum ComparisonOperator {
    /// Less than
    LessThan,
    /// Less than or equal
    LessThanOrEqual,
    /// Equal
    Equal,
    /// Greater than or equal
    GreaterThanOrEqual,
    /// Greater than
    GreaterThan,
    /// Not equal
    NotEqual,
}

/// Policy execution statistics
#[derive(Debug, Clone, Default)]
pub struct PolicyStatistics {
    /// Number of times policy was applied
    pub applications: u64,
    /// Number of successful applications
    pub successful_applications: u64,
    /// Average application time
    pub avg_application_time: Duration,
    /// Last application timestamp
    pub last_applied: Option<Instant>,
    /// Policy effectiveness score
    pub effectiveness_score: f64,
}

/// Performance counters for affinity management
#[derive(Debug, Clone, Default)]
pub struct PerformanceCounters {
    /// Total thread assignments
    pub total_assignments: u64,
    /// Total thread migrations
    pub total_migrations: u64,
    /// Successful assignments
    pub successful_assignments: u64,
    /// Failed assignments
    pub failed_assignments: u64,
    /// Average assignment time
    pub avg_assignment_time: Duration,
    /// Average migration time
    pub avg_migration_time: Duration,
    /// Core utilization balance score
    pub utilization_balance_score: f64,
    /// NUMA locality score
    pub numa_locality_score: f64,
    /// Cache affinity score
    pub cache_affinity_score: f64,
    /// Overall efficiency score
    pub overall_efficiency: f64,
}

impl Default for AffinityConfig {
    fn default() -> Self {
        Self {
            auto_binding_enabled: true,
            numa_aware_placement: true,
            hyperthreading_optimization: true,
            cache_optimal_placement: true,
            dynamic_adjustment_enabled: true,
            rebalancing_threshold: 0.15,
            adjustment_interval: Duration::from_millis(500),
            max_migrations_per_interval: 4,
            assignment_strategy: CoreAssignmentStrategy::Adaptive,
            hysteresis_factor: 0.1,
        }
    }
}

impl ThreadAffinityManager {
    /// Create a new thread affinity manager
    pub fn new(
        topology: Arc<CpuTopology>,
        config: AffinityConfig,
    ) -> WasmtimeResult<Self> {
        let core_assignments = Arc::new(ParkingRwLock::new(HashMap::new()));
        let core_utilization = Arc::new(ParkingRwLock::new(HashMap::new()));
        let policies = Arc::new(ParkingRwLock::new(Vec::new()));
        let performance_counters = Arc::new(ParkingRwLock::new(PerformanceCounters::default()));
        let dynamic_adjustment = Arc::new(AtomicBool::new(config.dynamic_adjustment_enabled));
        let shutdown = Arc::new(AtomicBool::new(false));

        // Initialize core utilization tracking
        {
            let mut utilization = core_utilization.write();
            for core_id in 0..topology.logical_cores {
                utilization.insert(core_id, CoreUtilization {
                    core_id,
                    utilization: 0.0,
                    thread_count: 0,
                    avg_execution_time: Duration::from_millis(0),
                    cache_hit_ratio: 0.0,
                    memory_bandwidth_utilization: 0.0,
                    temperature: None,
                    current_frequency: None,
                    last_updated: Instant::now(),
                    performance_score: 1.0,
                });
            }
        }

        let mut manager = Self {
            topology,
            config,
            core_assignments,
            core_utilization,
            policies,
            performance_counters,
            dynamic_adjustment,
            manager_thread: None,
            shutdown,
        };

        // Initialize default policies
        manager.initialize_default_policies()?;

        // Start management thread if dynamic adjustment is enabled
        if manager.config.dynamic_adjustment_enabled {
            manager.start_management_thread()?;
        }

        log::info!("Thread affinity manager initialized");
        Ok(manager)
    }

    /// Initialize default affinity policies
    fn initialize_default_policies(&mut self) -> WasmtimeResult<()> {
        let mut policies = self.policies.write();

        // High-priority thread policy
        policies.push(AffinityPolicy {
            name: "HighPriorityThreads".to_string(),
            priority: PolicyPriority::High,
            criteria: PolicyCriteria {
                thread_name_pattern: Some("high-priority-*".to_string()),
                thread_priority: Some(ThreadPriority::High),
                performance_hints: vec![],
                cpu_utilization_range: None,
                memory_usage_range: None,
            },
            action: PolicyAction::AssignUsingStrategy(CoreAssignmentStrategy::PerformanceGuided),
            conditions: vec![
                PolicyCondition {
                    condition_type: ConditionType::CpuUtilization,
                    threshold: 0.8,
                    operator: ComparisonOperator::LessThan,
                    enabled: true,
                },
            ],
            enabled: true,
            statistics: PolicyStatistics::default(),
        });

        // NUMA-aware policy for memory-intensive tasks
        policies.push(AffinityPolicy {
            name: "NumaAwareMemoryIntensive".to_string(),
            priority: PolicyPriority::Normal,
            criteria: PolicyCriteria {
                thread_name_pattern: Some("memory-intensive-*".to_string()),
                thread_priority: None,
                performance_hints: vec![
                    PerformanceHint {
                        cpu_utilization: None,
                        memory_pattern: Some(MemoryAccessPattern::Sequential),
                        cache_sensitivity: CacheSensitivity::High,
                        numa_preference: NumaPreference::Local,
                        frequency_requirement: FrequencyRequirement::None,
                    },
                ],
                cpu_utilization_range: None,
                memory_usage_range: Some((100 * 1024 * 1024, usize::MAX)), // > 100MB
            },
            action: PolicyAction::AssignUsingStrategy(CoreAssignmentStrategy::NumaAware),
            conditions: vec![],
            enabled: true,
            statistics: PolicyStatistics::default(),
        });

        // Load balancing policy
        policies.push(AffinityPolicy {
            name: "LoadBalancing".to_string(),
            priority: PolicyPriority::Normal,
            criteria: PolicyCriteria {
                thread_name_pattern: None,
                thread_priority: Some(ThreadPriority::Normal),
                performance_hints: vec![],
                cpu_utilization_range: None,
                memory_usage_range: None,
            },
            action: PolicyAction::AssignUsingStrategy(CoreAssignmentStrategy::LeastLoaded),
            conditions: vec![
                PolicyCondition {
                    condition_type: ConditionType::CpuUtilization,
                    threshold: self.config.rebalancing_threshold,
                    operator: ComparisonOperator::GreaterThan,
                    enabled: true,
                },
            ],
            enabled: true,
            statistics: PolicyStatistics::default(),
        });

        log::debug!("Initialized {} default affinity policies", policies.len());
        Ok(())
    }

    /// Start the management thread for dynamic affinity adjustment
    fn start_management_thread(&mut self) -> WasmtimeResult<()> {
        let core_assignments = self.core_assignments.clone();
        let core_utilization = self.core_utilization.clone();
        let policies = self.policies.clone();
        let performance_counters = self.performance_counters.clone();
        let dynamic_adjustment = self.dynamic_adjustment.clone();
        let shutdown = self.shutdown.clone();
        let config = self.config.clone();
        let topology = self.topology.clone();

        let handle = thread::Builder::new()
            .name("affinity-manager".to_string())
            .spawn(move || {
                Self::management_thread_main(
                    core_assignments,
                    core_utilization,
                    policies,
                    performance_counters,
                    dynamic_adjustment,
                    shutdown,
                    config,
                    topology,
                )
            })
            .map_err(|e| WasmtimeError::Concurrency {
                message: format!("Failed to start affinity management thread: {}", e),
            })?;

        self.manager_thread = Some(handle);
        log::debug!("Started affinity management thread");
        Ok(())
    }

    /// Management thread main loop
    fn management_thread_main(
        core_assignments: Arc<ParkingRwLock<HashMap<thread::ThreadId, CoreAssignment>>>,
        core_utilization: Arc<ParkingRwLock<HashMap<usize, CoreUtilization>>>,
        policies: Arc<ParkingRwLock<Vec<AffinityPolicy>>>,
        performance_counters: Arc<ParkingRwLock<PerformanceCounters>>,
        dynamic_adjustment: Arc<AtomicBool>,
        shutdown: Arc<AtomicBool>,
        config: AffinityConfig,
        topology: Arc<CpuTopology>,
    ) -> WasmtimeResult<()> {
        log::debug!("Affinity management thread started");

        while !shutdown.load(Ordering::Acquire) {
            if dynamic_adjustment.load(Ordering::Acquire) {
                // Update core utilization
                if let Err(e) = Self::update_core_utilization(&core_utilization, &topology) {
                    log::warn!("Failed to update core utilization: {}", e);
                }

                // Apply affinity policies
                if let Err(e) = Self::apply_affinity_policies(
                    &core_assignments,
                    &core_utilization,
                    &policies,
                    &performance_counters,
                    &config,
                    &topology,
                ) {
                    log::warn!("Failed to apply affinity policies: {}", e);
                }

                // Perform load balancing if needed
                if let Err(e) = Self::perform_load_balancing(
                    &core_assignments,
                    &core_utilization,
                    &performance_counters,
                    &config,
                    &topology,
                ) {
                    log::warn!("Failed to perform load balancing: {}", e);
                }

                // Update performance counters
                if let Err(e) = Self::update_performance_counters(
                    &core_assignments,
                    &core_utilization,
                    &performance_counters,
                ) {
                    log::warn!("Failed to update performance counters: {}", e);
                }
            }

            thread::sleep(config.adjustment_interval);
        }

        log::debug!("Affinity management thread terminated");
        Ok(())
    }

    /// Update core utilization metrics
    fn update_core_utilization(
        core_utilization: &Arc<ParkingRwLock<HashMap<usize, CoreUtilization>>>,
        topology: &Arc<CpuTopology>,
    ) -> WasmtimeResult<()> {
        let mut utilization = core_utilization.write();

        for core_id in 0..topology.logical_cores {
            if let Some(core_util) = utilization.get_mut(&core_id) {
                // Update utilization metrics (simplified implementation)
                core_util.utilization = Self::measure_core_utilization(core_id)?;
                core_util.cache_hit_ratio = Self::measure_cache_hit_ratio(core_id)?;
                core_util.memory_bandwidth_utilization = Self::measure_memory_bandwidth(core_id)?;
                core_util.temperature = Self::measure_core_temperature(core_id)?;
                core_util.current_frequency = Self::measure_core_frequency(core_id)?;
                core_util.last_updated = Instant::now();

                // Calculate performance score
                core_util.performance_score = Self::calculate_performance_score(core_util);
            }
        }

        Ok(())
    }

    /// Assign thread to optimal CPU core
    pub fn assign_thread(
        &self,
        thread_id: thread::ThreadId,
        priority: ThreadPriority,
        performance_hint: Option<PerformanceHint>,
    ) -> WasmtimeResult<CoreAssignment> {
        let start_time = Instant::now();

        // Determine optimal core using configured strategy
        let core_id = match self.config.assignment_strategy {
            CoreAssignmentStrategy::FirstAvailable => {
                self.find_first_available_core()?
            }
            CoreAssignmentStrategy::RoundRobin => {
                self.find_round_robin_core()?
            }
            CoreAssignmentStrategy::LeastLoaded => {
                self.find_least_loaded_core()?
            }
            CoreAssignmentStrategy::NumaAware => {
                self.find_numa_aware_core(&performance_hint)?
            }
            CoreAssignmentStrategy::CacheOptimal => {
                self.find_cache_optimal_core(&performance_hint)?
            }
            CoreAssignmentStrategy::PerformanceGuided => {
                self.find_performance_guided_core(&performance_hint)?
            }
            CoreAssignmentStrategy::Adaptive => {
                self.find_adaptive_core(priority, &performance_hint)?
            }
        };

        // Create core assignment
        let numa_node = self.topology.numa_nodes
            .iter()
            .find(|node| node.cpu_cores.contains(&core_id))
            .map(|node| node.id)
            .unwrap_or(0);

        let assignment = CoreAssignment {
            core_id,
            numa_node,
            assigned_at: Instant::now(),
            assignment_reason: AssignmentReason::Initial,
            thread_priority: priority,
            performance_hint: performance_hint.clone(),
            binding_strength: self.determine_binding_strength(priority, &performance_hint),
            migration_count: 0,
            last_migration: None,
        };

        // Apply CPU affinity
        if self.config.auto_binding_enabled {
            self.apply_cpu_affinity(thread_id, core_id)?;
        }

        // Update assignment tracking
        {
            let mut assignments = self.core_assignments.write();
            assignments.insert(thread_id, assignment.clone());
        }

        // Update core utilization
        {
            let mut utilization = self.core_utilization.write();
            if let Some(core_util) = utilization.get_mut(&core_id) {
                core_util.thread_count += 1;
            }
        }

        // Update performance counters
        {
            let mut counters = self.performance_counters.write();
            counters.total_assignments += 1;
            counters.successful_assignments += 1;
            counters.avg_assignment_time = Self::update_average_duration(
                counters.avg_assignment_time,
                start_time.elapsed(),
                counters.total_assignments,
            );
        }

        log::debug!("Assigned thread {:?} to core {} (NUMA node {})",
                   thread_id, core_id, numa_node);

        Ok(assignment)
    }

    /// Remove thread assignment
    pub fn remove_thread(&self, thread_id: thread::ThreadId) -> WasmtimeResult<()> {
        let assignment = {
            let mut assignments = self.core_assignments.write();
            assignments.remove(&thread_id)
        };

        if let Some(assignment) = assignment {
            // Update core utilization
            let mut utilization = self.core_utilization.write();
            if let Some(core_util) = utilization.get_mut(&assignment.core_id) {
                if core_util.thread_count > 0 {
                    core_util.thread_count -= 1;
                }
            }

            log::debug!("Removed thread {:?} from core {}", thread_id, assignment.core_id);
        }

        Ok(())
    }

    /// Get thread assignment information
    pub fn get_assignment(&self, thread_id: thread::ThreadId) -> Option<CoreAssignment> {
        let assignments = self.core_assignments.read();
        assignments.get(&thread_id).cloned()
    }

    /// Get core utilization information
    pub fn get_core_utilization(&self, core_id: usize) -> Option<CoreUtilization> {
        let utilization = self.core_utilization.read();
        utilization.get(&core_id).cloned()
    }

    /// Add custom affinity policy
    pub fn add_policy(&self, policy: AffinityPolicy) -> WasmtimeResult<()> {
        let mut policies = self.policies.write();
        policies.push(policy);
        policies.sort_by_key(|p| std::cmp::Reverse(p.priority));
        Ok(())
    }

    /// Remove affinity policy
    pub fn remove_policy(&self, policy_name: &str) -> WasmtimeResult<bool> {
        let mut policies = self.policies.write();
        let initial_len = policies.len();
        policies.retain(|p| p.name != policy_name);
        Ok(policies.len() != initial_len)
    }

    /// Get performance counters
    pub fn get_performance_counters(&self) -> PerformanceCounters {
        let counters = self.performance_counters.read();
        counters.clone()
    }

    /// Enable or disable dynamic adjustment
    pub fn set_dynamic_adjustment(&self, enabled: bool) {
        self.dynamic_adjustment.store(enabled, Ordering::Release);
        log::info!("Dynamic affinity adjustment {}", if enabled { "enabled" } else { "disabled" });
    }

    /// Core finding strategies implementation
    fn find_first_available_core(&self) -> WasmtimeResult<usize> {
        let utilization = self.core_utilization.read();

        for core_id in 0..self.topology.logical_cores {
            if let Some(core_util) = utilization.get(&core_id) {
                if core_util.utilization < 0.8 && core_util.thread_count < 4 {
                    return Ok(core_id);
                }
            }
        }

        // Fallback to core 0 if none available
        Ok(0)
    }

    fn find_round_robin_core(&self) -> WasmtimeResult<usize> {
        static ROUND_ROBIN_COUNTER: std::sync::atomic::AtomicUsize =
            std::sync::atomic::AtomicUsize::new(0);

        let core_id = ROUND_ROBIN_COUNTER.fetch_add(1, Ordering::SeqCst)
            % self.topology.logical_cores;
        Ok(core_id)
    }

    fn find_least_loaded_core(&self) -> WasmtimeResult<usize> {
        let utilization = self.core_utilization.read();

        let (best_core, _) = utilization
            .iter()
            .min_by(|(_, a), (_, b)| {
                a.utilization.partial_cmp(&b.utilization).unwrap_or(std::cmp::Ordering::Equal)
            })
            .map(|(core_id, util)| (*core_id, util.utilization))
            .unwrap_or((0, 0.0));

        Ok(best_core)
    }

    fn find_numa_aware_core(&self, hint: &Option<PerformanceHint>) -> WasmtimeResult<usize> {
        if let Some(hint) = hint {
            if let NumaPreference::Specific(numa_node) = hint.numa_preference {
                if let Some(node) = self.topology.numa_nodes.get(numa_node) {
                    // Find least loaded core in the preferred NUMA node
                    let utilization = self.core_utilization.read();
                    let (best_core, _) = node.cpu_cores
                        .iter()
                        .filter_map(|&core_id| {
                            utilization.get(&core_id).map(|util| (core_id, util.utilization))
                        })
                        .min_by(|(_, a), (_, b)| {
                            a.partial_cmp(b).unwrap_or(std::cmp::Ordering::Equal)
                        })
                        .unwrap_or((node.cpu_cores[0], 0.0));

                    return Ok(best_core);
                }
            }
        }

        // Fallback to least loaded strategy
        self.find_least_loaded_core()
    }

    fn find_cache_optimal_core(&self, _hint: &Option<PerformanceHint>) -> WasmtimeResult<usize> {
        // Simplified cache-optimal placement
        // In a real implementation, this would consider cache topology
        let utilization = self.core_utilization.read();

        let (best_core, _) = utilization
            .iter()
            .max_by(|(_, a), (_, b)| {
                a.cache_hit_ratio.partial_cmp(&b.cache_hit_ratio)
                    .unwrap_or(std::cmp::Ordering::Equal)
            })
            .map(|(core_id, util)| (*core_id, util.cache_hit_ratio))
            .unwrap_or((0, 0.0));

        Ok(best_core)
    }

    fn find_performance_guided_core(&self, _hint: &Option<PerformanceHint>) -> WasmtimeResult<usize> {
        let utilization = self.core_utilization.read();

        let (best_core, _) = utilization
            .iter()
            .max_by(|(_, a), (_, b)| {
                a.performance_score.partial_cmp(&b.performance_score)
                    .unwrap_or(std::cmp::Ordering::Equal)
            })
            .map(|(core_id, util)| (*core_id, util.performance_score))
            .unwrap_or((0, 0.0));

        Ok(best_core)
    }

    fn find_adaptive_core(
        &self,
        priority: ThreadPriority,
        hint: &Option<PerformanceHint>,
    ) -> WasmtimeResult<usize> {
        match priority {
            ThreadPriority::RealTime | ThreadPriority::High => {
                self.find_performance_guided_core(hint)
            }
            ThreadPriority::Normal => {
                if hint.as_ref().map_or(false, |h| matches!(h.numa_preference, NumaPreference::Local | NumaPreference::Specific(_))) {
                    self.find_numa_aware_core(hint)
                } else {
                    self.find_least_loaded_core()
                }
            }
            ThreadPriority::Background => {
                self.find_least_loaded_core()
            }
        }
    }

    /// Determine appropriate binding strength
    fn determine_binding_strength(
        &self,
        priority: ThreadPriority,
        hint: &Option<PerformanceHint>,
    ) -> BindingStrength {
        match priority {
            ThreadPriority::RealTime => BindingStrength::Pinned,
            ThreadPriority::High => {
                if hint.as_ref().map_or(false, |h| matches!(h.cache_sensitivity, CacheSensitivity::Critical)) {
                    BindingStrength::Strong
                } else {
                    BindingStrength::Normal
                }
            }
            ThreadPriority::Normal => BindingStrength::Normal,
            ThreadPriority::Background => BindingStrength::Soft,
        }
    }

    /// Apply CPU affinity to thread
    fn apply_cpu_affinity(&self, thread_id: thread::ThreadId, core_id: usize) -> WasmtimeResult<()> {
        // Platform-specific CPU affinity implementation
        #[cfg(target_os = "linux")]
        {
            use libc::{cpu_set_t, pthread_t, CPU_SET, CPU_ZERO, pthread_setaffinity_np};
            use std::mem;

            // Convert ThreadId to pthread_t (this is simplified)
            let pthread_id = unsafe {
                mem::transmute::<thread::ThreadId, pthread_t>(thread_id)
            };

            unsafe {
                let mut cpu_set: cpu_set_t = mem::zeroed();
                CPU_ZERO(&mut cpu_set);
                CPU_SET(core_id, &mut cpu_set);

                if pthread_setaffinity_np(pthread_id, mem::size_of::<cpu_set_t>(), &cpu_set) != 0 {
                    return Err(WasmtimeError::Concurrency {
                        message: format!("Failed to set CPU affinity for core {}", core_id),
                    });
                }
            }
        }

        #[cfg(not(target_os = "linux"))]
        {
            log::warn!("CPU affinity setting not implemented for this platform");
        }

        Ok(())
    }

    /// Helper functions for metrics collection
    fn measure_core_utilization(core_id: usize) -> WasmtimeResult<f64> {
        // Simplified utilization measurement
        // Real implementation would use system APIs
        Ok(0.5 + (core_id as f64 * 0.1) % 0.4)
    }

    fn measure_cache_hit_ratio(_core_id: usize) -> WasmtimeResult<f64> {
        // Simplified cache hit ratio measurement
        Ok(0.85)
    }

    fn measure_memory_bandwidth(_core_id: usize) -> WasmtimeResult<f64> {
        // Simplified memory bandwidth measurement
        Ok(0.6)
    }

    fn measure_core_temperature(_core_id: usize) -> WasmtimeResult<Option<f32>> {
        // Simplified temperature measurement
        Ok(Some(65.0))
    }

    fn measure_core_frequency(_core_id: usize) -> WasmtimeResult<Option<u32>> {
        // Simplified frequency measurement
        Ok(Some(2400))
    }

    fn calculate_performance_score(core_util: &CoreUtilization) -> f64 {
        let utilization_score = (1.0 - core_util.utilization).max(0.0);
        let cache_score = core_util.cache_hit_ratio;
        let memory_score = (1.0 - core_util.memory_bandwidth_utilization).max(0.0);
        let thread_score = (1.0 - (core_util.thread_count as f64 / 8.0)).max(0.0);

        (utilization_score * 0.3 + cache_score * 0.25 + memory_score * 0.25 + thread_score * 0.2)
    }

    fn update_average_duration(current_avg: Duration, new_duration: Duration, count: u64) -> Duration {
        if count == 0 {
            new_duration
        } else {
            Duration::from_nanos(
                (current_avg.as_nanos() as u64 * (count - 1) + new_duration.as_nanos() as u64) / count
            )
        }
    }

    /// Apply affinity policies (stub implementation)
    fn apply_affinity_policies(
        _core_assignments: &Arc<ParkingRwLock<HashMap<thread::ThreadId, CoreAssignment>>>,
        _core_utilization: &Arc<ParkingRwLock<HashMap<usize, CoreUtilization>>>,
        _policies: &Arc<ParkingRwLock<Vec<AffinityPolicy>>>,
        _performance_counters: &Arc<ParkingRwLock<PerformanceCounters>>,
        _config: &AffinityConfig,
        _topology: &Arc<CpuTopology>,
    ) -> WasmtimeResult<()> {
        // Implementation would evaluate and apply policies
        Ok(())
    }

    /// Perform load balancing (stub implementation)
    fn perform_load_balancing(
        _core_assignments: &Arc<ParkingRwLock<HashMap<thread::ThreadId, CoreAssignment>>>,
        _core_utilization: &Arc<ParkingRwLock<HashMap<usize, CoreUtilization>>>,
        _performance_counters: &Arc<ParkingRwLock<PerformanceCounters>>,
        _config: &AffinityConfig,
        _topology: &Arc<CpuTopology>,
    ) -> WasmtimeResult<()> {
        // Implementation would perform load balancing
        Ok(())
    }

    /// Update performance counters (stub implementation)
    fn update_performance_counters(
        _core_assignments: &Arc<ParkingRwLock<HashMap<thread::ThreadId, CoreAssignment>>>,
        _core_utilization: &Arc<ParkingRwLock<HashMap<usize, CoreUtilization>>>,
        _performance_counters: &Arc<ParkingRwLock<PerformanceCounters>>,
    ) -> WasmtimeResult<()> {
        // Implementation would update performance counters
        Ok(())
    }

    /// Shutdown the affinity manager
    pub fn shutdown(&mut self) -> WasmtimeResult<()> {
        log::info!("Shutting down thread affinity manager");

        self.shutdown.store(true, Ordering::Release);

        if let Some(handle) = self.manager_thread.take() {
            if let Err(e) = handle.join() {
                log::warn!("Management thread join failed: {:?}", e);
            }
        }

        log::info!("Thread affinity manager shutdown complete");
        Ok(())
    }
}

impl Drop for ThreadAffinityManager {
    fn drop(&mut self) {
        let _ = self.shutdown();
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::time::Duration;

    #[test]
    fn test_affinity_manager_creation() {
        let topology = Arc::new(CpuTopology::detect().expect("Failed to detect topology"));
        let config = AffinityConfig::default();
        let manager = ThreadAffinityManager::new(topology, config).expect("Failed to create manager");

        let counters = manager.get_performance_counters();
        assert_eq!(counters.total_assignments, 0);
    }

    #[test]
    fn test_thread_assignment() {
        let topology = Arc::new(CpuTopology::detect().expect("Failed to detect topology"));
        let mut config = AffinityConfig::default();
        config.auto_binding_enabled = false; // Disable actual CPU binding for tests

        let manager = ThreadAffinityManager::new(topology, config).expect("Failed to create manager");

        let thread_id = thread::current().id();
        let assignment = manager.assign_thread(
            thread_id,
            ThreadPriority::Normal,
            None,
        ).expect("Failed to assign thread");

        assert!(assignment.core_id < manager.topology.logical_cores);
        assert_eq!(assignment.thread_priority, ThreadPriority::Normal);

        let retrieved = manager.get_assignment(thread_id);
        assert!(retrieved.is_some());
        assert_eq!(retrieved.unwrap().core_id, assignment.core_id);

        manager.remove_thread(thread_id).expect("Failed to remove thread");
        assert!(manager.get_assignment(thread_id).is_none());
    }

    #[test]
    fn test_performance_hints() {
        let hint = PerformanceHint {
            cpu_utilization: Some(0.8),
            memory_pattern: Some(MemoryAccessPattern::Sequential),
            cache_sensitivity: CacheSensitivity::High,
            numa_preference: NumaPreference::Local,
            frequency_requirement: FrequencyRequirement::Minimum(2000),
        };

        assert!(matches!(hint.cache_sensitivity, CacheSensitivity::High));
        assert!(matches!(hint.numa_preference, NumaPreference::Local));
    }

    #[test]
    fn test_binding_strength_determination() {
        let topology = Arc::new(CpuTopology::detect().expect("Failed to detect topology"));
        let config = AffinityConfig::default();
        let manager = ThreadAffinityManager::new(topology, config).expect("Failed to create manager");

        let high_priority_hint = Some(PerformanceHint {
            cpu_utilization: None,
            memory_pattern: None,
            cache_sensitivity: CacheSensitivity::Critical,
            numa_preference: NumaPreference::None,
            frequency_requirement: FrequencyRequirement::None,
        });

        let strength = manager.determine_binding_strength(ThreadPriority::High, &high_priority_hint);
        assert!(matches!(strength, BindingStrength::Strong));

        let strength = manager.determine_binding_strength(ThreadPriority::RealTime, &None);
        assert!(matches!(strength, BindingStrength::Pinned));

        let strength = manager.determine_binding_strength(ThreadPriority::Background, &None);
        assert!(matches!(strength, BindingStrength::Soft));
    }

    #[test]
    fn test_core_assignment_strategies() {
        let topology = Arc::new(CpuTopology::detect().expect("Failed to detect topology"));
        let mut config = AffinityConfig::default();
        config.auto_binding_enabled = false;

        // Test different assignment strategies
        let strategies = [
            CoreAssignmentStrategy::FirstAvailable,
            CoreAssignmentStrategy::RoundRobin,
            CoreAssignmentStrategy::LeastLoaded,
            CoreAssignmentStrategy::Adaptive,
        ];

        for strategy in &strategies {
            config.assignment_strategy = *strategy;
            let manager = ThreadAffinityManager::new(topology.clone(), config.clone())
                .expect("Failed to create manager");

            let thread_id = thread::current().id();
            let assignment = manager.assign_thread(thread_id, ThreadPriority::Normal, None)
                .expect("Failed to assign thread");

            assert!(assignment.core_id < topology.logical_cores);
            manager.remove_thread(thread_id).expect("Failed to remove thread");
        }
    }

    #[test]
    fn test_policy_creation() {
        let policy = AffinityPolicy {
            name: "TestPolicy".to_string(),
            priority: PolicyPriority::High,
            criteria: PolicyCriteria {
                thread_name_pattern: Some("test-*".to_string()),
                thread_priority: Some(ThreadPriority::High),
                performance_hints: vec![],
                cpu_utilization_range: Some((0.0, 0.8)),
                memory_usage_range: None,
            },
            action: PolicyAction::AssignToCore(0),
            conditions: vec![],
            enabled: true,
            statistics: PolicyStatistics::default(),
        };

        assert_eq!(policy.name, "TestPolicy");
        assert!(policy.enabled);
        assert!(matches!(policy.action, PolicyAction::AssignToCore(0)));
    }

    #[test]
    fn test_core_utilization_measurement() {
        let utilization = ThreadAffinityManager::measure_core_utilization(0)
            .expect("Failed to measure utilization");
        assert!(utilization >= 0.0 && utilization <= 1.0);

        let cache_ratio = ThreadAffinityManager::measure_cache_hit_ratio(0)
            .expect("Failed to measure cache ratio");
        assert!(cache_ratio >= 0.0 && cache_ratio <= 1.0);
    }
}