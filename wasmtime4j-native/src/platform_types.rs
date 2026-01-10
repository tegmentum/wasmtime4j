//! Common type definitions for platform optimization modules
//!
//! This module provides all the shared type definitions used by:
//! - memory_bandwidth_optimization.rs
//! - cpu_microarchitecture_detection.rs
//! - cpu_cache_management.rs
//! - numa_topology.rs
//! - platform_config.rs

use std::collections::HashMap;

// ============================================================================
// Access Pattern Types
// ============================================================================

/// Access pattern classification for memory optimization
#[derive(Debug, Clone, Default)]
pub struct AccessPattern {
    /// Pattern type identifier
    pub pattern_type: AccessPatternType,
    /// Access frequency
    pub frequency: f64,
    /// Stride length in bytes
    pub stride: usize,
    /// Whether pattern is predictable
    pub predictable: bool,
}

/// Type of memory access pattern
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum AccessPatternType {
    #[default]
    Sequential,
    Strided,
    Random,
    Mixed,
}

/// Clustering of similar access patterns
#[derive(Debug, Clone, Default)]
pub struct AccessPatternClustering {
    /// Clustering algorithm
    pub clustering_algorithm: ClusteringAlgorithm,
    /// Number of clusters
    pub cluster_count: usize,
    /// Similarity threshold for clustering
    pub similarity_threshold: f64,
    /// Dynamic reclustering enabled
    pub dynamic_reclustering: bool,
}

/// Quality metrics for pattern clustering
#[derive(Debug, Clone, Default)]
pub struct ClusterQualityMetrics {
    /// Silhouette score
    pub silhouette_score: f64,
    /// Intra-cluster variance
    pub intra_cluster_variance: f64,
}

// ============================================================================
// Prefetch Types
// ============================================================================

/// Prefetch algorithm configuration
#[derive(Debug, Clone, Default)]
pub struct PrefetchAlgorithm {
    /// Algorithm name
    pub name: String,
    /// Prefetch distance
    pub distance: usize,
    /// Aggressiveness level (0-100)
    pub aggressiveness: u32,
    /// Whether adaptive
    pub adaptive: bool,
}

/// Tracking of prefetch effectiveness
#[derive(Debug, Clone, Default)]
pub struct PrefetchEffectivenessTracking {
    /// Hit rate improvement per pattern
    pub hit_rate_improvement: HashMap<String, f64>,
    /// Bandwidth overhead per pattern
    pub bandwidth_overhead: HashMap<String, f64>,
    /// Accuracy metrics per pattern
    pub accuracy_metrics: HashMap<String, f64>,
}

/// Adaptive prefetch tuning parameters
#[derive(Debug, Clone, Default)]
pub struct AdaptivePrefetchTuning {
    /// Auto-tuning enabled
    pub enabled: bool,
    /// Adaptation algorithm
    pub adaptation_algorithm: AdaptationAlgorithm,
    /// Adaptation rate
    pub adaptation_rate: f64,
    /// Performance feedback configuration
    pub performance_feedback: PerformanceFeedback,
}

/// Detection of prefetch interference
#[derive(Debug, Clone, Default)]
pub struct PrefetchInterferenceDetection {
    /// Detection enabled
    pub detection_enabled: bool,
    /// Interference threshold
    pub interference_threshold: f64,
    /// Mitigation strategies
    pub mitigation_strategies: Vec<InterferenceMitigationStrategy>,
}

/// Coordination across cache levels for prefetching
#[derive(Debug, Clone, Default)]
pub struct MultiLevelPrefetchCoordination {
    /// Coordination enabled
    pub enabled: bool,
    /// Coordination algorithm
    pub coordination_algorithm: CoordinationAlgorithm,
    /// Conflict resolution strategy
    pub conflict_resolution: ConflictResolution,
}

/// Preference settings for a cache level
#[derive(Debug, Clone, Default)]
pub struct CacheLevelPreference {
    /// Preference weight
    pub preference_weight: f64,
    /// Access priority
    pub access_priority: AccessPriority,
    /// Retention priority
    pub retention_priority: RetentionPriority,
    /// Prefetch aggressiveness
    pub prefetch_aggressiveness: PrefetchAggressiveness,
}

/// Prefetching capabilities of the system
#[derive(Debug, Clone, Default)]
pub struct PrefetchingCapabilities {
    /// Hardware prefetching support
    pub hardware_prefetching: bool,
    /// Stride prefetching support
    pub stride_prefetching: bool,
    /// Stream prefetching support
    pub stream_prefetching: bool,
    /// Indirect prefetching support
    pub indirect_prefetching: bool,
    /// Prefetch distance (cache lines ahead)
    pub prefetch_distance: u32,
    /// Prefetch degree (number of cache lines)
    pub prefetch_degree: u32,
}

/// Effectiveness metrics for prefetching
#[derive(Debug, Clone, Default)]
pub struct PrefetchingEffectiveness {
    /// Useful prefetch rate
    pub useful_prefetch_rate: f64,
    /// Late prefetch rate
    pub late_prefetch_rate: f64,
    /// Wasteful prefetch rate
    pub wasteful_prefetch_rate: f64,
    /// Overall effectiveness score
    pub effectiveness_score: f64,
}

// ============================================================================
// Controller Types
// ============================================================================

/// Memory scheduling algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum MemorySchedulingAlgorithm {
    #[default]
    None,
    FirstReadyFirstComeFirstServed,
    FirstReadyFairQueuing,
    FirstReadyRoundRobin,
    PriorityBased,
    DeadlineBased,
    ParallelizedAddressRemapping,
    ThreadCluster,
    Atlas,
    Bliss,
    Custom(u32),
}

/// Memory controller configuration
#[derive(Debug, Clone, Default)]
pub struct ControllerConfiguration {
    /// Scheduling algorithm
    pub scheduling_algorithm: MemorySchedulingAlgorithm,
    /// Queue management policy
    pub queue_management: QueueManagementPolicy,
    /// Power management configuration
    pub power_management: ControllerPowerConfiguration,
    /// Error handling configuration
    pub error_handling: ControllerErrorHandling,
}

/// Scheduling optimization for memory controllers
#[derive(Debug, Clone, Default)]
pub struct SchedulingOptimization {
    /// Enabled flag
    pub enabled: bool,
    /// Optimization algorithm
    pub optimization_algorithm: SchedulingOptimizationAlgorithm,
    /// Adaptation frequency
    pub adaptation_frequency: std::time::Duration,
}

/// Request prioritization for memory controllers
#[derive(Debug, Clone, Default)]
pub struct RequestPrioritization {
    /// Enabled flag
    pub enabled: bool,
    /// Prioritization criteria
    pub prioritization_criteria: Vec<PrioritizationCriterion>,
    /// Dynamic adjustment
    pub dynamic_adjustment: bool,
}

/// Power optimization for controllers
#[derive(Debug, Clone, Default)]
pub struct ControllerPowerOptimization {
    /// Enabled flag
    pub enabled: bool,
    /// Power budget management
    pub power_budget_management: bool,
    /// Thermal aware scheduling
    pub thermal_aware_scheduling: bool,
}

/// Error handling configuration for controllers
#[derive(Debug, Clone, Default)]
pub struct ErrorHandlingConfiguration {
    /// Proactive error detection
    pub proactive_error_detection: bool,
    /// Predictive error correction
    pub predictive_error_correction: bool,
    /// Error rate monitoring
    pub error_rate_monitoring: bool,
}

// ============================================================================
// Bandwidth QoS Types
// ============================================================================

/// Bandwidth priority scheme
#[derive(Debug, Clone, Default)]
pub struct BandwidthPriorityScheme {
    /// Priority levels
    pub levels: Vec<BandwidthPriorityLevel>,
    /// Priority inheritance enabled
    pub priority_inheritance: bool,
    /// Aging enabled
    pub aging_enabled: bool,
    /// Preemption policy
    pub preemption_policy: BandwidthPreemptionPolicy,
}

/// Quality of Service policy for bandwidth
#[derive(Debug, Clone, Default)]
pub struct BandwidthQosPolicy {
    /// Policy identifier
    pub policy_id: u32,
    /// Policy name
    pub name: String,
    /// Minimum guaranteed bandwidth
    pub min_bandwidth: f64,
    /// Maximum allowed bandwidth
    pub max_bandwidth: f64,
    /// Priority level
    pub priority: u32,
}

/// Service Level Agreement for bandwidth
#[derive(Debug, Clone, Default)]
pub struct BandwidthSla {
    /// SLA identifier
    pub sla_id: u32,
    /// Guaranteed minimum bandwidth
    pub guaranteed_bandwidth: f64,
    /// Target latency
    pub target_latency: u64,
    /// Violation penalty
    pub violation_penalty: f64,
}

/// Tracking of resource allocation
#[derive(Debug, Clone, Default)]
pub struct ResourceAllocationTracking {
    /// Tracking enabled
    pub tracking_enabled: bool,
    /// Granularity level
    pub granularity: TrackingGranularity,
    /// Reporting frequency
    pub reporting_frequency: std::time::Duration,
}

/// System for handling QoS violations
#[derive(Debug, Clone, Default)]
pub struct ViolationHandlingSystem {
    /// Detection sensitivity
    pub detection_sensitivity: ViolationSensitivity,
    /// Response strategy
    pub response_strategy: ViolationResponseStrategy,
    /// Escalation policy
    pub escalation_policy: ViolationEscalationPolicy,
}

/// System for enforcing performance guarantees
#[derive(Debug, Clone, Default)]
pub struct GuaranteeEnforcementSystem {
    /// Enforcement mechanism
    pub enforcement_mechanism: EnforcementMechanism,
    /// Enforcement strictness
    pub enforcement_strictness: EnforcementStrictness,
    /// Violation tolerance
    pub violation_tolerance: f64,
}

// ============================================================================
// Throttling Types
// ============================================================================

/// Throttling policy configuration
#[derive(Debug, Clone, Default)]
pub struct ThrottlingPolicy {
    /// Policy identifier
    pub policy_id: u32,
    /// Policy name
    pub name: String,
    /// Throttle threshold
    pub threshold: f64,
    /// Throttle amount
    pub throttle_amount: f64,
    /// Duration ms
    pub duration_ms: u64,
}

/// System for detecting congestion
#[derive(Debug, Clone, Default)]
pub struct CongestionDetectionSystem {
    /// Detection algorithms
    pub detection_algorithms: Vec<CongestionDetectionAlgorithm>,
    /// Detection thresholds
    pub detection_thresholds: CongestionDetectionThresholds,
    /// Detection window
    pub detection_window: std::time::Duration,
}

/// Throttling algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ThrottlingAlgorithm {
    #[default]
    None,
    LinearThrottling,
    ExponentialBackoff,
    AdaptiveThrottling,
    TokenBucket,
    SlidingWindow,
}

/// Recovery mechanism type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RecoveryMechanism {
    #[default]
    None,
    GradualIncrease,
    StepFunction,
    AdaptiveRecovery,
    Exponential,
    Immediate,
}

/// Fairness enforcement in throttling
#[derive(Debug, Clone, Default)]
pub struct ThrottlingFairnessSystem {
    /// Fairness metric
    pub fairness_metric: ThrottlingFairnessMetric,
    /// Fairness window
    pub fairness_window: std::time::Duration,
    /// Fairness threshold
    pub fairness_threshold: f64,
}

/// Fairness metric type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum FairnessMetric {
    #[default]
    None,
    JainsFairnessIndex,
    MaxMinFairness,
    ProportionalFairness,
    GeneralizedFairness,
}

// ============================================================================
// Performance and Optimization Types
// ============================================================================

/// Optimization priority level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum OptimizationPriority {
    /// Low priority
    #[default]
    Low,
    /// Medium priority
    Medium,
    /// High priority
    High,
    /// Critical priority
    Critical,
}

/// Optimization recommendation
#[derive(Debug, Clone, Default)]
pub struct OptimizationRecommendation {
    /// Recommendation identifier
    pub id: u32,
    /// Description
    pub description: String,
    /// Expected improvement percentage
    pub expected_improvement: f64,
    /// Implementation difficulty (1-10)
    pub difficulty: u32,
    /// Category
    pub category: String,
    /// Priority level
    pub priority: OptimizationPriority,
}

/// Tuning parameters for optimization
#[derive(Debug, Clone, Default)]
pub struct TuningParameters {
    /// Parameter set name
    pub name: String,
    /// Integer parameters
    pub int_params: HashMap<String, i64>,
    /// Float parameters
    pub float_params: HashMap<String, f64>,
    /// Boolean parameters
    pub bool_params: HashMap<String, bool>,
}

/// Objective type for optimization
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ObjectiveType {
    #[default]
    Latency,
    Throughput,
    PowerEfficiency,
    Balanced,
    MaximizeThroughput,
    MinimizeLatency,
    MaximizeEfficiency,
}

/// Adjustment action for dynamic tuning
#[derive(Debug, Clone, Default)]
pub struct AdjustmentAction {
    /// Action type
    pub action_type: String,
    /// Target parameter
    pub target: String,
    /// Adjustment amount
    pub amount: f64,
    /// Cooldown after action
    pub cooldown_ms: u64,
}

/// Trigger for adjustments
#[derive(Debug, Clone, Default)]
pub struct AdjustmentTrigger {
    /// Trigger condition
    pub condition: String,
    /// Threshold value
    pub threshold: f64,
    /// Comparison operator
    pub operator: String,
    /// Sample count required
    pub sample_count: u32,
}

/// Prediction model for optimization
#[derive(Debug, Clone, Default)]
pub struct PredictionModel {
    /// Model type
    pub model_type: String,
    /// Accuracy percentage
    pub accuracy: f64,
    /// Training data size
    pub training_size: usize,
    /// Last updated timestamp
    pub last_updated: u64,
}

/// Real-time analysis algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RealtimeAnalysisAlgorithm {
    #[default]
    None,
    StatisticalAnalysis,
    MachineLearning,
    HeuristicBased,
    RuleEngine,
    Hybrid,
}

// ============================================================================
// Pattern Analysis Types
// ============================================================================

/// Snapshot of pattern evolution
#[derive(Debug, Clone, Default)]
pub struct PatternEvolutionSnapshot {
    /// Timestamp
    pub timestamp: u64,
    /// Pattern counts
    pub pattern_counts: HashMap<String, u32>,
    /// Dominant pattern
    pub dominant_pattern: String,
    /// Transition rate
    pub transition_rate: f64,
}

/// Frequency statistics for patterns
#[derive(Debug, Clone, Default)]
pub struct PatternFrequencyStats {
    /// Total samples
    pub total_samples: u64,
    /// Pattern frequencies
    pub frequencies: HashMap<String, f64>,
    /// Peak frequency
    pub peak_frequency: f64,
    /// Average frequency
    pub average_frequency: f64,
}

/// Optimization for mixed access patterns
#[derive(Debug, Clone, Default)]
pub struct MixedPatternOptimization {
    /// Pattern switching detection
    pub pattern_switching_detection: bool,
    /// Adaptive optimization
    pub adaptive_optimization: bool,
    /// Optimization blending
    pub optimization_blending: OptimizationBlending,
}

/// Optimization for random access patterns
#[derive(Debug, Clone, Default)]
pub struct RandomAccessOptimization {
    /// Cache partitioning
    pub cache_partitioning: bool,
    /// Victim cache utilization
    pub victim_cache_utilization: bool,
    /// Access prediction
    pub access_prediction: AccessPrediction,
}

/// Optimization for sequential access patterns
#[derive(Debug, Clone, Default)]
pub struct SequentialAccessOptimization {
    /// Stream buffer utilization
    pub stream_buffer_utilization: bool,
    /// Prefetch distance adaptation
    pub prefetch_distance_adaptation: bool,
    /// Bandwidth throttling
    pub bandwidth_throttling: BandwidthThrottling,
}

/// Optimization for strided access patterns
#[derive(Debug, Clone, Default)]
pub struct StridedAccessOptimization {
    /// Stride detection
    pub stride_detection: StrideDetection,
    /// Stride prefetching config
    pub stride_prefetching: StridePrefetching,
}

/// Optimization for spatial locality
#[derive(Debug, Clone, Default)]
pub struct SpatialLocalityOptimization {
    /// Block-based allocation
    pub block_based_allocation: bool,
    /// Cache line alignment
    pub cache_line_alignment: bool,
    /// Spatial correlation analysis
    pub spatial_correlation_analysis: bool,
    /// Prefetch coordination
    pub prefetch_coordination: PrefetchCoordination,
}

/// Optimization for temporal locality
#[derive(Debug, Clone, Default)]
pub struct TemporalLocalityOptimization {
    /// LRU optimization
    pub lru_optimization: bool,
    /// Access frequency tracking
    pub access_frequency_tracking: bool,
    /// Temporal correlation analysis
    pub temporal_correlation_analysis: bool,
    /// Hot data identification
    pub hot_data_identification: HotDataIdentification,
}

/// Optimization strategy for locality
#[derive(Debug, Clone, Default)]
pub struct LocalityOptimizationStrategy {
    /// Temporal locality weight
    pub temporal_locality_weight: f64,
    /// Spatial locality weight
    pub spatial_locality_weight: f64,
    /// Access frequency weight
    pub access_frequency_weight: f64,
    /// Reuse distance optimization
    pub reuse_distance_optimization: bool,
}

// ============================================================================
// Cache Types
// ============================================================================

/// Characteristics of a cache level
#[derive(Debug, Clone, Default)]
pub struct CacheLevelCharacteristics {
    /// Cache level (1, 2, 3)
    pub level: u32,
    /// Size in bytes
    pub size: usize,
    /// Line size in bytes
    pub line_size: u32,
    /// Associativity
    pub associativity: u32,
    /// Latency in cycles
    pub latency_cycles: u32,
    /// Write policy
    pub write_policy: String,
}

/// Cache miss penalties
#[derive(Debug, Clone, Default)]
pub struct CacheMissPenalties {
    /// L1 miss penalty cycles
    pub l1_miss: u32,
    /// L2 miss penalty cycles
    pub l2_miss: u32,
    /// L3 miss penalty cycles
    pub l3_miss: u32,
    /// Memory access cycles
    pub memory_access: u32,
}

/// Cache bandwidth characteristics
#[derive(Debug, Clone, Default)]
pub struct CacheBandwidthCharacteristics {
    /// Read bandwidth GB/s
    pub read_bandwidth: f64,
    /// Write bandwidth GB/s
    pub write_bandwidth: f64,
    /// Fill bandwidth GB/s
    pub fill_bandwidth: f64,
    /// Eviction bandwidth GB/s
    pub eviction_bandwidth: f64,
}

/// Cache bypass strategy
#[derive(Debug, Clone, Default)]
pub struct CacheBypassStrategy {
    /// Strategy ID
    pub strategy_id: u32,
    /// Bypass condition
    pub bypass_condition: BypassCondition,
    /// Bypass threshold value
    pub bypass_threshold: f64,
    /// Bypass probability
    pub bypass_probability: f64,
}

/// Cache warming policy
#[derive(Debug, Clone, Default)]
pub struct CacheWarmingPolicy {
    /// Policy ID
    pub policy_id: u32,
    /// Warming trigger
    pub warming_trigger: WarmingTrigger,
    /// Warming strategy
    pub warming_strategy: WarmingStrategy,
    /// Warming intensity
    pub warming_intensity: f64,
}

/// Cache coherency capabilities
#[derive(Debug, Clone, Default)]
pub struct CacheCoherencyCapabilities {
    /// Hardware coherency support
    pub hardware_coherency: bool,
    /// Coherency protocol type
    pub coherency_protocol: CoherencyProtocol,
    /// Multiprocessor coherency support
    pub multiprocessor_coherency: bool,
    /// Cache maintenance operations support
    pub cache_maintenance_operations: bool,
}

/// Inter-cache optimization
#[derive(Debug, Clone, Default)]
pub struct InterCacheOptimization {
    /// Victim cache utilization
    pub victim_cache_utilization: bool,
    /// Cross-level prefetching
    pub cross_level_prefetching: bool,
    /// Cache bypass optimization
    pub cache_bypass_optimization: bool,
    /// Coherency optimization level
    pub coherency_optimization: CoherencyOptimizationLevel,
}

/// Partitioning for pollution control
#[derive(Debug, Clone, Default)]
pub struct PartitioningPollutionControl {
    /// Dynamic partitioning
    pub dynamic_partitioning: bool,
    /// Pollution aware allocation
    pub pollution_aware_allocation: bool,
    /// Isolation enforcement
    pub isolation_enforcement: IsolationEnforcement,
}

/// Pollution prevention strategy type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum PollutionPreventionStrategy {
    #[default]
    None,
    CacheBypass,
    AccessPrioritization,
    VictimCacheUtilization,
    PartitionIsolation,
    SetAssociativity,
}

/// Identification of pollution sources
#[derive(Debug, Clone, Default)]
pub struct PollutionSourceIdentification {
    /// Streaming detection
    pub streaming_detection: bool,
    /// Thrashing detection
    pub thrashing_detection: bool,
    /// Interference detection
    pub interference_detection: InterferenceDetection,
}

// ============================================================================
// Memory Controller and Bandwidth Types
// ============================================================================

/// Memory controller characteristics
#[derive(Debug, Clone, Default)]
pub struct MemoryControllerCharacteristics {
    /// Controller count
    pub controller_count: u32,
    /// Channels per controller
    pub channels_per_controller: u32,
    /// Maximum bandwidth GB/s
    pub max_bandwidth: f64,
    /// Minimum latency ns
    pub min_latency: u64,
}

/// Memory bandwidth characteristics
#[derive(Debug, Clone, Default)]
pub struct MemoryBandwidthCharacteristics {
    /// Peak bandwidth GB/s
    pub peak_bandwidth: f64,
    /// Sustained bandwidth GB/s
    pub sustained_bandwidth: f64,
    /// Read bandwidth GB/s
    pub read_bandwidth: f64,
    /// Write bandwidth GB/s
    pub write_bandwidth: f64,
}

/// Memory latency characteristics
#[derive(Debug, Clone, Default)]
pub struct MemoryLatencyCharacteristics {
    /// Minimum latency ns
    pub min_latency: u64,
    /// Average latency ns
    pub avg_latency: u64,
    /// Maximum latency ns
    pub max_latency: u64,
    /// Latency variance
    pub variance: f64,
}

/// Channel bandwidth measurement
#[derive(Debug, Clone, Default)]
pub struct ChannelBandwidthMeasurement {
    /// Channel ID
    pub channel_id: u32,
    /// Current bandwidth GB/s
    pub current_bandwidth: f64,
    /// Peak bandwidth GB/s
    pub peak_bandwidth: f64,
    /// Utilization percentage
    pub utilization: f64,
}

/// Data replication strategy
#[derive(Debug, Clone, Default)]
pub struct DataReplicationStrategy {
    /// Strategy ID
    pub strategy_id: u32,
    /// Replication trigger
    pub replication_trigger: ReplicationTrigger,
    /// Replication factor
    pub replication_factor: u32,
    /// Consistency model
    pub consistency_model: ConsistencyModel,
}

/// Memory migration policy
#[derive(Debug, Clone, Default)]
pub struct MemoryMigrationPolicy {
    /// Policy identifier
    pub policy_id: u32,
    /// Trigger conditions
    pub trigger_conditions: Vec<MigrationTriggerCondition>,
    /// Migration algorithm
    pub migration_algorithm: MigrationAlgorithm,
    /// Migration batch size
    pub migration_batch_size: usize,
}

/// Load balancing strategy for memory
#[derive(Debug, Clone, Default)]
pub struct LoadBalancingStrategy {
    /// Enabled flag
    pub enabled: bool,
    /// Balancing algorithm
    pub balancing_algorithm: LoadBalancingAlgorithm,
    /// Load threshold
    pub load_threshold: f64,
    /// Migration cost threshold
    pub migration_cost_threshold: f64,
}

/// Placement algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum PlacementAlgorithm {
    #[default]
    FirstFit,
    BestFit,
    WorstFit,
    NextFit,
    LocalityAware,
    RoundRobin,
}

// ============================================================================
// NUMA Types
// ============================================================================

/// NUMA performance characteristics
#[derive(Debug, Clone, Default)]
pub struct NumaPerformanceCharacteristics {
    /// Node count
    pub node_count: u32,
    /// Local access latency ns
    pub local_latency: u64,
    /// Remote access latency ns
    pub remote_latency: u64,
    /// Bandwidth ratio local/remote
    pub bandwidth_ratio: f64,
}

// ============================================================================
// Bandwidth Performance Types
// ============================================================================

/// Performance bandwidth indicators
#[derive(Debug, Clone, Default)]
pub struct PerformanceBandwidthIndicators {
    /// Current utilization percentage
    pub utilization: f64,
    /// Throughput GB/s
    pub throughput: f64,
    /// Latency ns
    pub latency: u64,
    /// Queue depth
    pub queue_depth: u32,
}

// ============================================================================
// CPU Architecture Types - x86
// ============================================================================

/// Basic x86 CPU features
#[derive(Debug, Clone, Default)]
pub struct X86BasicFeatures {
    /// x87 FPU support
    pub x87_fpu: bool,
    /// MMX support
    pub mmx: bool,
    /// SSE support
    pub sse: bool,
    /// SSE2 support
    pub sse2: bool,
    /// SSE3 support
    pub sse3: bool,
    /// SSSE3 support
    pub ssse3: bool,
    /// SSE4.1 support
    pub sse4_1: bool,
    /// SSE4.2 support
    pub sse4_2: bool,
    /// SSE4a support (AMD-specific)
    pub sse4a: bool,
}

/// x86 SIMD extensions
#[derive(Debug, Clone, Default)]
pub struct X86SimdExtensions {
    /// AVX support
    pub avx: bool,
    /// AVX2 support
    pub avx2: bool,
    /// FMA support
    pub fma: bool,
    /// FMA4 support (AMD-specific)
    pub fma4: bool,
    /// XOP support (AMD-specific)
    pub xop: bool,
}

/// AVX-512 extension details
#[derive(Debug, Clone, Default)]
pub struct AvxExtensions {
    /// AVX-512 Foundation
    pub avx512f: bool,
    /// AVX-512 Doubleword and Quadword
    pub avx512dq: bool,
    /// AVX-512 Conflict Detection
    pub avx512cd: bool,
    /// AVX-512 Byte and Word
    pub avx512bw: bool,
    /// AVX-512 Vector Length
    pub avx512vl: bool,
    /// AVX-512 Integer Fused Multiply-Add
    pub avx512ifma: bool,
    /// AVX-512 Vector Byte Manipulation Instructions
    pub avx512vbmi: bool,
    /// AVX-512 4-register Neural Network Instructions Word
    pub avx512_4vnniw: bool,
    /// AVX-512 4-register Fused Multiply-Accumulation Packed Single
    pub avx512_4fmaps: bool,
    /// AVX-512 Vector Population Count Doubleword and Quadword
    pub avx512_vpopcntdq: bool,
    /// AVX-512 Vector Neural Network Instructions
    pub avx512_vnni: bool,
}

/// Intel-specific CPU features
#[derive(Debug, Clone, Default)]
pub struct IntelSpecificFeatures {
    /// TSX (Transactional Synchronization Extensions) support
    pub tsx: bool,
    /// MPX (Memory Protection Extensions) support - deprecated
    pub mpx: bool,
    /// CET (Control-flow Enforcement Technology) support
    pub cet: bool,
    /// SHA extensions support
    pub sha: bool,
    /// SGX (Software Guard Extensions) support
    pub sgx: bool,
    /// TME (Total Memory Encryption) support
    pub tme: bool,
}

/// AMD-specific CPU features
#[derive(Debug, Clone, Default)]
pub struct AmdSpecificFeatures {
    /// 3DNow! support (legacy)
    pub three_dnow: bool,
    /// Enhanced 3DNow! support
    pub enhanced_3dnow: bool,
    /// SSE4a support (AMD-specific)
    pub sse4a: bool,
    /// FMA4 support (AMD-specific)
    pub fma4: bool,
    /// XOP (Extended Operations) support
    pub xop: bool,
    /// TBM (Trailing Bit Manipulation) support
    pub tbm: bool,
    /// SVM (Secure Virtual Machine) support
    pub svm: bool,
}

/// x86 cryptographic extensions
#[derive(Debug, Clone, Default)]
pub struct X86CryptoExtensions {
    /// AES-NI support
    pub aes: bool,
    /// PCLMULQDQ (carry-less multiplication) support
    pub pclmulqdq: bool,
    /// SHA extensions support
    pub sha: bool,
    /// SHA-512 extensions support
    pub sha512: bool,
    /// VAES (AVX-512 AES) support
    pub vaes: bool,
    /// VPCLMULQDQ (AVX-512 PCLMUL) support
    pub vpclmulqdq: bool,
}

/// Control flow extensions
#[derive(Debug, Clone, Default)]
pub struct ControlFlowExtensions {
    /// CET-IBT (Indirect Branch Tracking) support
    pub cet_ibt: bool,
    /// CET-SS (Shadow Stack) support
    pub cet_ss: bool,
    /// CFI (Control Flow Integrity) support
    pub cfi: bool,
}

/// Memory protection extensions
#[derive(Debug, Clone, Default)]
pub struct MemoryProtectionExtensions {
    /// SMEP (Supervisor Mode Execution Prevention) support
    pub smep: bool,
    /// SMAP (Supervisor Mode Access Prevention) support
    pub smap: bool,
    /// PKU (Protection Keys for Userspace) support
    pub pku: bool,
    /// LA57 (57-bit Linear Addresses) support
    pub la57: bool,
}

// ============================================================================
// CPU Architecture Types - ARM
// ============================================================================

/// ARM base CPU features
#[derive(Debug, Clone, Default)]
pub struct ArmBaseFeatures {
    /// Thumb instruction set support
    pub thumb: bool,
    /// Jazelle DBX support
    pub jazelle: bool,
    /// ThumbEE support
    pub thumbee: bool,
    /// TrustZone Security Extensions support
    pub security_extensions: bool,
    /// Virtualization Extensions support
    pub virtualization_extensions: bool,
    /// Generic Timer support
    pub generic_timer: bool,
    /// Large Physical Address Extension (LPAE) support
    pub large_physical_address_extension: bool,
}

/// ARM NEON features
#[derive(Debug, Clone, Default)]
pub struct NeonFeatures {
    /// NEON support available
    pub neon_available: bool,
    /// NEON FP16 (half-precision) support
    pub neon_fp16: bool,
    /// NEON BF16 (bfloat16) support
    pub neon_bf16: bool,
    /// NEON I8MM (8-bit integer matrix multiply) support
    pub neon_i8mm: bool,
    /// NEON dot product support
    pub neon_dotprod: bool,
}

/// ARM SVE features
#[derive(Debug, Clone, Default)]
pub struct SveFeatures {
    /// SVE (Scalable Vector Extensions) available
    pub sve_available: bool,
    /// SVE vector length (in bits)
    pub sve_vector_length: Option<u32>,
    /// SVE2 available
    pub sve2_available: bool,
    /// SVE2 AES instructions
    pub sve2_aes: bool,
    /// SVE2 SHA3 instructions
    pub sve2_sha3: bool,
    /// SVE2 SM4 instructions
    pub sve2_sm4: bool,
    /// SVE2 bit permutation instructions
    pub sve2_bitperm: bool,
}

/// ARM cryptographic features
#[derive(Debug, Clone, Default)]
pub struct ArmCryptoFeatures {
    /// AES support
    pub aes: bool,
    /// SHA1 support
    pub sha1: bool,
    /// SHA-256 support
    pub sha256: bool,
    /// SHA-512 support
    pub sha512: bool,
    /// SHA3 support
    pub sha3: bool,
    /// SM3 (Chinese hash) support
    pub sm3: bool,
    /// SM4 (Chinese cipher) support
    pub sm4: bool,
}

/// ARM TrustZone features
#[derive(Debug, Clone, Default)]
pub struct TrustZoneFeatures {
    /// TrustZone available
    pub trustzone_available: bool,
    /// Secure world support
    pub secure_world_support: bool,
    /// Secure Monitor Calls (SMC) support
    pub secure_monitor_calls: bool,
    /// Secure interrupts support
    pub secure_interrupts: bool,
}

/// ARM performance features
#[derive(Debug, Clone, Default)]
pub struct ArmPerformanceFeatures {
    /// big.LITTLE heterogeneous computing support
    pub big_little: bool,
    /// DynamIQ Shared Unit (DSU) support
    pub dsu: bool,
    /// Cache Coherent Network (CCN) support
    pub ccn: bool,
    /// Cache Coherent Interconnect (CCI) support
    pub cci: bool,
}

/// Mali GPU integration features
#[derive(Debug, Clone, Default)]
pub struct MaliIntegrationFeatures {
    /// Mali model
    pub model: String,
    /// Shader cores
    pub shader_cores: u32,
    /// GPU frequency MHz
    pub frequency_mhz: u32,
    /// Unified memory
    pub unified_memory: bool,
}

// ============================================================================
// CPU Architecture Types - RISC-V
// ============================================================================

/// RISC-V base ISA
#[derive(Debug, Clone, Default)]
pub struct RiscVBaseIsa {
    /// RV32I base integer instruction set
    pub rv32i: bool,
    /// RV64I base integer instruction set
    pub rv64i: bool,
    /// RV128I base integer instruction set
    pub rv128i: bool,
}

/// RISC-V standard extensions
#[derive(Debug, Clone, Default)]
pub struct RiscVStandardExtensions {
    /// M extension (Integer Multiply/Divide)
    pub m_extension: bool,
    /// A extension (Atomic Instructions)
    pub a_extension: bool,
    /// F extension (Single-Precision Floating-Point)
    pub f_extension: bool,
    /// D extension (Double-Precision Floating-Point)
    pub d_extension: bool,
    /// Q extension (Quad-Precision Floating-Point)
    pub q_extension: bool,
    /// C extension (Compressed Instructions)
    pub c_extension: bool,
    /// B extension (Bit Manipulation)
    pub b_extension: bool,
    /// P extension (Packed-SIMD Instructions)
    pub p_extension: bool,
    /// V extension (Vector Instructions)
    pub v_extension: bool,
    /// N extension (User-Level Interrupts)
    pub n_extension: bool,
}

/// RISC-V custom extension
#[derive(Debug, Clone, Default)]
pub struct RiscVCustomExtension {
    /// Extension name
    pub name: String,
    /// Version
    pub version: String,
    /// Description
    pub description: String,
}

/// RISC-V vector extension
#[derive(Debug, Clone, Default)]
pub struct RiscVVectorExtension {
    /// V extension support
    pub v_extension: bool,
    /// VLEN
    pub vlen: u32,
    /// ELEN
    pub elen: u32,
}

/// RISC-V supervisor features
#[derive(Debug, Clone, Default)]
pub struct RiscVSupervisorFeatures {
    /// Supervisor mode support
    pub supervisor_mode: bool,
    /// Virtual memory support
    pub virtual_memory: bool,
    /// Page-based virtual memory support
    pub page_based_virtual_memory: bool,
    /// Hypervisor extension support
    pub hypervisor_extension: bool,
}

/// RISC-V hypervisor features
#[derive(Debug, Clone, Default)]
pub struct RiscVHypervisorFeatures {
    /// H extension support
    pub h_extension: bool,
    /// Two-stage translation
    pub two_stage_translation: bool,
    /// Virtual interrupt
    pub virtual_interrupt: bool,
}

// ============================================================================
// CPU Architecture Types - Other Architectures
// ============================================================================

/// PowerPC configuration
#[derive(Debug, Clone, Default)]
pub struct PowerPcConfig {
    /// Architecture version
    pub version: String,
    /// VSX support
    pub vsx: bool,
    /// AltiVec support
    pub altivec: bool,
}

/// MIPS configuration
#[derive(Debug, Clone, Default)]
pub struct MipsConfig {
    /// Architecture revision
    pub revision: u32,
    /// MSA support
    pub msa: bool,
    /// DSP support
    pub dsp: bool,
}

/// SPARC configuration
#[derive(Debug, Clone, Default)]
pub struct SparcConfig {
    /// Architecture version
    pub version: String,
    /// VIS version
    pub vis_version: u32,
}

/// LoongArch configuration
#[derive(Debug, Clone, Default)]
pub struct LoongArchConfig {
    /// Architecture version
    pub version: String,
    /// LSX support
    pub lsx: bool,
    /// LASX support
    pub lasx: bool,
}

/// Exotic architecture features
#[derive(Debug, Clone, Default)]
pub struct ExoticArchitectureFeatures {
    /// Architecture name
    pub name: String,
    /// Custom extensions
    pub extensions: Vec<String>,
    /// Special capabilities
    pub capabilities: Vec<String>,
}

// ============================================================================
// CPU Execution and Pipeline Types
// ============================================================================

/// Execution unit configuration
#[derive(Debug, Clone, Default)]
pub struct ExecutionUnit {
    /// Unit ID
    pub unit_id: u32,
    /// Unit type
    pub unit_type: ExecutionUnitType,
    /// Latency cycles
    pub latency: u32,
    /// Throughput ops/cycle
    pub throughput: f64,
}

/// Execution unit type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ExecutionUnitType {
    /// General integer ALU
    #[default]
    Integer,
    /// Integer ALU
    IntegerAlu,
    /// Floating-point ALU
    FloatingPoint,
    /// Floating-point ALU (alias)
    FloatingPointAlu,
    /// Floating-point multiplier
    FloatingPointMultiplier,
    /// Vector unit
    Vector,
    /// Load/store unit
    LoadStore,
    /// Branch unit
    Branch,
}

/// Vector execution unit
#[derive(Debug, Clone, Default)]
pub struct VectorExecutionUnit {
    /// Unit ID
    pub unit_id: u32,
    /// Vector width in bits
    pub vector_width: u32,
    /// Supported operations
    pub supported_operations: Vec<VectorOperation>,
    /// Latency cycles
    pub latency: u32,
    /// Throughput ops/cycle
    pub throughput: f64,
}

/// Vector operation type
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VectorOperation {
    /// Vector add
    Add,
    /// Vector subtract
    Subtract,
    /// Vector multiply
    Multiply,
    /// Fused multiply-add
    Fma,
    /// Vector divide
    Divide,
    /// Vector compare
    Compare,
    /// Vector shuffle
    Shuffle,
}

/// Load/store unit
#[derive(Debug, Clone, Default)]
pub struct LoadStoreUnit {
    /// Unit ID
    pub unit_id: u32,
    /// Load latency cycles
    pub load_latency: u32,
    /// Store latency cycles
    pub store_latency: u32,
    /// Throughput ops/cycle
    pub throughput: f64,
    /// Address generation units
    pub address_generation_units: u32,
}

/// Branch execution unit
#[derive(Debug, Clone, Default)]
pub struct BranchUnit {
    /// Unit ID
    pub unit_id: u32,
    /// Branch latency cycles
    pub branch_latency: u32,
    /// Misprediction penalty cycles
    pub misprediction_penalty: u32,
    /// Throughput ops/cycle
    pub throughput: f64,
}

/// Scheduler configuration
#[derive(Debug, Clone, Default)]
pub struct SchedulerConfiguration {
    /// Scheduler type
    pub scheduler_type: SchedulerType,
    /// Instruction window size
    pub instruction_window_size: u32,
    /// Issue width
    pub issue_width: u32,
    /// Dispatch width
    pub dispatch_width: u32,
}

/// Pipeline stage information
#[derive(Debug, Clone, Default)]
pub struct PipelineStage {
    /// Stage name
    pub name: String,
    /// Stage number
    pub number: u32,
    /// Latency cycles
    pub latency: u32,
}

/// Pipeline efficiency metrics
#[derive(Debug, Clone, Default)]
pub struct PipelineEfficiencyMetrics {
    /// IPC (Instructions Per Cycle)
    pub ipc: f64,
    /// Stall rate
    pub stall_rate: f64,
    /// Bubble rate
    pub bubble_rate: f64,
    /// Utilization
    pub utilization: f64,
}

/// Hazard handling capabilities
#[derive(Debug, Clone, Default)]
pub struct HazardHandlingCapabilities {
    /// Data forwarding
    pub data_forwarding: bool,
    /// Branch prediction
    pub branch_prediction: bool,
    /// Speculation
    pub speculation: bool,
    /// Pipeline flush cost
    pub flush_cost: u32,
}

// ============================================================================
// Branch Prediction Types
// ============================================================================

/// Branch prediction algorithm
#[derive(Debug, Clone, Default)]
pub struct BranchPredictionAlgorithm {
    /// Algorithm name
    pub name: String,
    /// Predictor type
    pub predictor_type: String,
    /// History bits
    pub history_bits: u32,
}

/// Branch target buffer characteristics
#[derive(Debug, Clone, Default)]
pub struct BtbCharacteristics {
    /// Entry count
    pub entries: u32,
    /// Associativity
    pub associativity: u32,
    /// Hit latency
    pub hit_latency: u32,
}

/// Return stack buffer information
#[derive(Debug, Clone, Default)]
pub struct ReturnStackBufferInfo {
    /// Entry count
    pub entries: u32,
    /// Accuracy
    pub accuracy: f64,
}

/// Indirect branch prediction
#[derive(Debug, Clone, Default)]
pub struct IndirectBranchPrediction {
    /// Predictor type
    pub predictor_type: String,
    /// Target table entries
    pub entries: u32,
    /// Accuracy
    pub accuracy: f64,
}

/// Branch prediction accuracy metrics
#[derive(Debug, Clone, Default)]
pub struct BranchPredictionAccuracy {
    /// Overall accuracy
    pub overall: f64,
    /// Conditional accuracy
    pub conditional: f64,
    /// Indirect accuracy
    pub indirect: f64,
    /// Return accuracy
    pub return_accuracy: f64,
}

/// Advanced branch prediction features
#[derive(Debug, Clone, Default)]
pub struct AdvancedBranchPrediction {
    /// Neural branch prediction
    pub neural_branch_prediction: bool,
    /// Perceptron predictor
    pub perceptron_predictor: bool,
    /// Two-level adaptive predictor
    pub two_level_adaptive: bool,
    /// Tournament predictor
    pub tournament_predictor: bool,
    /// Prediction accuracy (0.0 - 1.0)
    pub prediction_accuracy: f64,
}

// ============================================================================
// Floating Point Types
// ============================================================================

/// Floating point unit characteristics
#[derive(Debug, Clone, Default)]
pub struct FloatingPointCharacteristics {
    /// Precision bits
    pub precision: u32,
    /// Unit count
    pub unit_count: u32,
    /// Add latency
    pub add_latency: u32,
    /// Multiply latency
    pub mul_latency: u32,
    /// Division latency
    pub div_latency: u32,
}

/// Special function performance
#[derive(Debug, Clone, Default)]
pub struct SpecialFunctionPerformance {
    /// Square root latency
    pub sqrt_latency: u32,
    /// Reciprocal latency
    pub reciprocal_latency: u32,
    /// Transcendental support
    pub transcendental: bool,
}

/// Denormal handling characteristics
#[derive(Debug, Clone, Default)]
pub struct DenormalHandlingCharacteristics {
    /// Flush to zero support
    pub flush_to_zero: bool,
    /// Denormals as zero
    pub denormals_as_zero: bool,
    /// Performance penalty
    pub performance_penalty: bool,
}

// ============================================================================
// Vector Processing Types
// ============================================================================

/// Vector register configuration
#[derive(Debug, Clone, Default)]
pub struct VectorRegisterConfiguration {
    /// Register count
    pub register_count: u32,
    /// Register width bits
    pub register_width: u32,
    /// Mask registers
    pub mask_registers: u32,
}

/// Vector length support
#[derive(Debug, Clone, Default)]
pub struct VectorLengthSupport {
    /// Minimum vector length bits
    pub min_vlen: u32,
    /// Maximum vector length bits
    pub max_vlen: u32,
    /// Scalable vector length
    pub scalable: bool,
}

/// Vector throughput characteristics
#[derive(Debug, Clone, Default)]
pub struct VectorThroughputCharacteristics {
    /// Integer throughput ops/cycle
    pub integer_throughput: f64,
    /// Float throughput ops/cycle
    pub float_throughput: f64,
    /// Memory throughput GB/s
    pub memory_throughput: f64,
}

/// Vector memory access patterns
#[derive(Debug, Clone, Default)]
pub struct VectorMemoryAccessPatterns {
    /// Gather support
    pub gather: bool,
    /// Scatter support
    pub scatter: bool,
    /// Stride support
    pub strided: bool,
    /// Unit stride optimal
    pub unit_stride_optimal: bool,
}

/// Instruction throughput characteristics
#[derive(Debug, Clone, Default)]
pub struct InstructionThroughputCharacteristics {
    /// Integer IPC
    pub integer_ipc: f64,
    /// Float IPC
    pub float_ipc: f64,
    /// Branch IPC
    pub branch_ipc: f64,
    /// Memory IPC
    pub memory_ipc: f64,
}

// ============================================================================
// ISA Analysis Types
// ============================================================================

/// ISA analysis results
#[derive(Debug, Clone, Default)]
pub struct IsaAnalysis {
    /// Instruction count
    pub instruction_count: u32,
    /// Instruction categories
    pub categories: Vec<String>,
    /// Extension analysis
    pub extensions: Vec<String>,
    /// Optimization opportunities
    pub optimization_opportunities: Vec<String>,
}

/// Microcode capabilities
#[derive(Debug, Clone, Default)]
pub struct MicrocodeCapabilities {
    /// Microcode version
    pub version: String,
    /// Updateable
    pub updateable: bool,
    /// Size bytes
    pub size: usize,
    /// Patches applied
    pub patches: u32,
}

/// Hardware acceleration features
#[derive(Debug, Clone, Default)]
pub struct HardwareAccelerationFeatures {
    /// Crypto acceleration
    pub crypto: bool,
    /// AI/ML acceleration
    pub ai_ml: bool,
    /// Compression acceleration
    pub compression: bool,
    /// Network acceleration
    pub network: bool,
}

/// Virtualization features
#[derive(Debug, Clone, Default)]
pub struct VirtualizationFeatures {
    /// Hardware virtualization
    pub hardware_virtualization: bool,
    /// Nested virtualization
    pub nested: bool,
    /// IO virtualization
    pub io_virtualization: bool,
    /// Memory virtualization
    pub memory_virtualization: bool,
}

/// Debug and profiling features
#[derive(Debug, Clone, Default)]
pub struct DebugAndProfilingFeatures {
    /// Hardware breakpoints
    pub hardware_breakpoints: u32,
    /// Watchpoints
    pub watchpoints: u32,
    /// Performance counters
    pub performance_counters: u32,
    /// Branch tracing
    pub branch_tracing: bool,
}

/// Reliability features
#[derive(Debug, Clone, Default)]
pub struct ReliabilityFeatures {
    /// ECC support
    pub ecc: bool,
    /// Parity checking
    pub parity: bool,
    /// Error injection
    pub error_injection: bool,
    /// Machine check
    pub machine_check: bool,
}

// ============================================================================
// Security Types
// ============================================================================

/// Control flow integrity features
#[derive(Debug, Clone, Default)]
pub struct ControlFlowIntegrityFeatures {
    /// Hardware CFI support
    pub hardware_cfi: bool,
    /// Indirect branch tracking
    pub indirect_branch_tracking: bool,
    /// Return address signing (ARM pointer authentication)
    pub return_address_signing: bool,
    /// Shadow stack support
    pub shadow_stack: bool,
}

/// Memory tagging features
#[derive(Debug, Clone, Default)]
pub struct MemoryTaggingFeatures {
    /// Memory tagging extensions support (ARM MTE)
    pub memory_tagging_extensions: bool,
    /// Tag-based AddressSanitizer
    pub tag_based_asan: bool,
    /// Hardware tag checking
    pub hardware_tag_checking: bool,
    /// Tag granularity (bytes)
    pub tag_granularity: Option<u32>,
}

/// Secure boot capabilities
#[derive(Debug, Clone, Default)]
pub struct SecureBootCapabilities {
    /// Secure boot support
    pub secure_boot_support: bool,
    /// Measured boot
    pub measured_boot: bool,
    /// Attestation support
    pub attestation_support: bool,
    /// Root of trust
    pub root_of_trust: bool,
}

/// Hardware RNG features
#[derive(Debug, Clone, Default)]
pub struct HardwareRngFeatures {
    /// Hardware RNG available
    pub hardware_rng_available: bool,
    /// RNG entropy source type
    pub rng_entropy_source: RngEntropySource,
    /// Throughput MB/s
    pub rng_throughput: u32,
}

/// RNG entropy source type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RngEntropySource {
    /// Unknown source
    #[default]
    Unknown,
    /// True random (hardware-based)
    TrueRandom,
    /// Pseudo-random
    PseudoRandom,
    /// Mixed source
    Mixed,
}

/// Side-channel mitigation features
#[derive(Debug, Clone, Default)]
pub struct SideChannelMitigationFeatures {
    /// Spectre V1 mitigation
    pub spectre_v1_mitigation: bool,
    /// Spectre V2 mitigation
    pub spectre_v2_mitigation: bool,
    /// Meltdown mitigation
    pub meltdown_mitigation: bool,
    /// L1 Terminal Fault mitigation
    pub l1tf_mitigation: bool,
    /// Microarchitectural Data Sampling mitigation
    pub mds_mitigation: bool,
}

// ============================================================================
// Power Management Types
// ============================================================================

/// DVFS capabilities
#[derive(Debug, Clone, Default)]
pub struct DvfsCapabilities {
    /// Dynamic voltage scaling support
    pub dynamic_voltage_scaling: bool,
    /// Dynamic frequency scaling support
    pub dynamic_frequency_scaling: bool,
    /// Per-core DVFS support
    pub per_core_dvfs: bool,
    /// Boost frequencies support
    pub boost_frequencies: bool,
    /// Turbo boost support
    pub turbo_boost: bool,
}

/// Power gating capabilities
#[derive(Debug, Clone, Default)]
pub struct PowerGatingCapabilities {
    /// Core power gating support
    pub core_power_gating: bool,
    /// Cluster power gating support
    pub cluster_power_gating: bool,
    /// Partial power gating support
    pub partial_power_gating: bool,
    /// Retention states support
    pub retention_states: bool,
}

/// Idle state management
#[derive(Debug, Clone, Default)]
pub struct IdleStateManagement {
    /// Available C-states
    pub c_states: Vec<IdleState>,
    /// Package C-states support
    pub package_c_states: bool,
    /// Adaptive idle support
    pub adaptive_idle: bool,
}

/// Thermal management capabilities
#[derive(Debug, Clone, Default)]
pub struct ThermalManagementCapabilities {
    /// Thermal monitoring support
    pub thermal_monitoring: bool,
    /// Thermal throttling support
    pub thermal_throttling: bool,
    /// Temperature sensors available
    pub temperature_sensors: bool,
    /// Fan control support
    pub fan_control: bool,
    /// Thermal design power in watts
    pub thermal_design_power: u32,
}

/// Power monitoring capabilities
#[derive(Debug, Clone, Default)]
pub struct PowerMonitoringCapabilities {
    /// Power measurement support
    pub power_measurement: bool,
    /// Energy counters support
    pub energy_counters: bool,
    /// Per-core power monitoring support
    pub per_core_power_monitoring: bool,
    /// Power capping support
    pub power_capping: bool,
}

// ============================================================================
// Topology Types
// ============================================================================

/// SMT configuration
#[derive(Debug, Clone, Default)]
pub struct SmtConfiguration {
    /// SMT enabled
    pub enabled: bool,
    /// Threads per core
    pub threads_per_core: u32,
    /// SMT type
    pub smt_type: SmtType,
}

/// Core cluster information
#[derive(Debug, Clone, Default)]
pub struct CoreCluster {
    /// Cluster ID
    pub cluster_id: u32,
    /// Cluster type
    pub cluster_type: CoreClusterType,
    /// Physical cores
    pub physical_cores: Vec<u32>,
    /// Logical cores
    pub logical_cores: Vec<u32>,
    /// Cache sharing level
    pub cache_sharing: CacheSharingLevel,
    /// Interconnect type
    pub interconnect_type: CoreInterconnectType,
}

/// Uncore components
#[derive(Debug, Clone, Default)]
pub struct UncoreComponents {
    /// Memory controller
    pub memory_controller: bool,
    /// PCIe controller
    pub pcie_controller: bool,
    /// Integrated graphics
    pub integrated_graphics: bool,
    /// System agent
    pub system_agent: bool,
    /// Power control unit
    pub power_control_unit: bool,
}

/// Thermal design power
#[derive(Debug, Clone, Default)]
pub struct ThermalDesignPower {
    /// Base TDP watts
    pub base_tdp: f64,
    /// Max turbo power watts
    pub max_turbo_power: f64,
    /// Power limit 1 watts
    pub power_limit_1: f64,
    /// Power limit 2 watts
    pub power_limit_2: f64,
    /// Power limit 4 watts
    pub power_limit_4: f64,
}

/// Process technology
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ProcessTechnology {
    #[default]
    Unknown,
    Planar,
    FinFet5nm,
    FinFet7nm,
    FinFet10nm,
    FinFet14nm,
    FinFet22nm,
}

/// Power efficiency characteristics
#[derive(Debug, Clone, Default)]
pub struct PowerEfficiencyCharacteristics {
    /// Performance per watt
    pub performance_per_watt: f64,
    /// Idle power watts
    pub idle_power: f64,
    /// Dynamic power scaling
    pub dynamic_power_scaling: bool,
}

/// Generation information
#[derive(Debug, Clone, Default)]
pub struct GenerationInfo {
    /// Generation name
    pub generation_name: String,
    /// Release year
    pub release_year: u32,
    /// Predecessor generation
    pub predecessor: Option<String>,
    /// Successor generation
    pub successor: Option<String>,
    /// Generation improvements
    pub generation_improvements: Vec<GenerationImprovement>,
}

// ============================================================================
// Out-of-Order and Speculation Types
// ============================================================================

/// Out-of-order execution capabilities
#[derive(Debug, Clone, Default)]
pub struct OutOfOrderCapabilities {
    /// Out-of-order execution support
    pub out_of_order_execution: bool,
    /// Instruction window size
    pub instruction_window_size: u32,
    /// Reorder buffer size
    pub reorder_buffer_size: u32,
    /// Register renaming support
    pub register_renaming: bool,
    /// Physical register file size
    pub register_file_size: u32,
}

/// Speculative execution features
#[derive(Debug, Clone, Default)]
pub struct SpeculativeExecutionFeatures {
    /// Speculative execution support
    pub speculative_execution: bool,
    /// Branch prediction speculation
    pub branch_prediction_speculation: bool,
    /// Memory speculation
    pub memory_speculation: bool,
    /// Speculation depth
    pub speculation_depth: u32,
    /// Misspeculation recovery support
    pub misspeculation_recovery: bool,
}

/// Dynamic optimization features
#[derive(Debug, Clone, Default)]
pub struct DynamicOptimizationFeatures {
    /// Micro-op fusion
    pub micro_op_fusion: bool,
    /// Macro-op fusion
    pub macro_op_fusion: bool,
    /// Loop stream detection
    pub loop_stream_detection: bool,
    /// Zero latency moves
    pub zero_latency_moves: bool,
    /// Elimination optimizations
    pub elimination_optimizations: bool,
}

// ============================================================================
// Atomic and Memory Ordering Types
// ============================================================================

/// Atomic operation support
#[derive(Debug, Clone, Default)]
pub struct AtomicOperationSupport {
    /// Basic atomic operations support
    pub basic_atomics: bool,
    /// Compare-and-swap support
    pub compare_and_swap: bool,
    /// Load-linked/store-conditional support
    pub load_linked_store_conditional: bool,
    /// Memory barriers support
    pub memory_barriers: bool,
    /// Acquire/release semantics support
    pub acquire_release_semantics: bool,
}

/// Memory ordering model
#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub enum MemoryOrderingModel {
    /// Strong ordering (x86-style TSO)
    StrongOrdering,
    /// Weak ordering (ARM-style)
    #[default]
    WeakOrdering,
    /// Relaxed ordering
    RelaxedOrdering,
    /// Sequential consistency
    SequentialConsistency,
}

/// Exception handling capabilities
#[derive(Debug, Clone, Default)]
pub struct ExceptionHandlingCapabilities {
    /// Precise exceptions support
    pub precise_exceptions: bool,
    /// Imprecise exceptions support
    pub imprecise_exceptions: bool,
    /// Exception vectors support
    pub exception_vectors: bool,
    /// Nested exceptions support
    pub nested_exceptions: bool,
}

/// Interrupt handling capabilities
#[derive(Debug, Clone, Default)]
pub struct InterruptHandlingCapabilities {
    /// Vectored interrupts support
    pub vectored_interrupts: bool,
    /// Interrupt priorities support
    pub interrupt_priorities: bool,
    /// Nested interrupts support
    pub nested_interrupts: bool,
    /// Fast interrupt response support
    pub fast_interrupt_response: bool,
}

// ============================================================================
// ARM-Specific Detailed Types
// ============================================================================

/// Cortex-A cache optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexACacheOptimizations {
    /// Cache prefetch
    pub prefetch: bool,
    /// Cache clean
    pub clean: bool,
    /// Cache invalidate
    pub invalidate: bool,
}

/// Cortex-M DSP optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexMDspOptimizations {
    /// DSP instructions
    pub dsp_instructions: bool,
    /// Saturating arithmetic
    pub saturating_arithmetic: bool,
    /// SIMD operations
    pub simd: bool,
}

/// Cortex-M low power optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexMLowPowerOptimizations {
    /// Sleep modes
    pub sleep_modes: Vec<String>,
    /// Wake-up latency
    pub wake_latency_us: u32,
    /// Retention support
    pub retention: bool,
}

/// Cortex-M MPU configuration
#[derive(Debug, Clone, Default)]
pub struct CortexMMpuConfig {
    /// MPU regions
    pub regions: u32,
    /// Subregions
    pub subregions: bool,
    /// Background region
    pub background_region: bool,
}

/// Cortex-M realtime optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexMRealtimeOptimizations {
    /// Tail chaining
    pub tail_chaining: bool,
    /// Late arrival
    pub late_arrival: bool,
    /// Vector table offset
    pub vtor: bool,
}

/// Cortex-R error correction
#[derive(Debug, Clone, Default)]
pub struct CortexRErrorCorrection {
    /// ECC support
    pub ecc: bool,
    /// Parity support
    pub parity: bool,
    /// Lockstep mode
    pub lockstep: bool,
}

/// Cortex-R realtime optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexRRealtimeOptimizations {
    /// Low-latency interrupts
    pub low_latency_interrupts: bool,
    /// Deterministic execution
    pub deterministic: bool,
    /// Flash patch
    pub flash_patch: bool,
}

/// Cortex-R TCM configuration
#[derive(Debug, Clone, Default)]
pub struct CortexRTcmConfig {
    /// ITCM size
    pub itcm_size: usize,
    /// DTCM size
    pub dtcm_size: usize,
    /// TCM wait states
    pub wait_states: u32,
}

/// Cortex-X high performance optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexXHighPerformanceOptimizations {
    /// Wide issue
    pub wide_issue: bool,
    /// Large OoO window
    pub large_ooo_window: bool,
    /// Aggressive speculation
    pub aggressive_speculation: bool,
}

/// Cortex-X advanced SIMD optimizations
#[derive(Debug, Clone, Default)]
pub struct CortexXAdvancedSimdOptimizations {
    /// Full SVE2
    pub full_sve2: bool,
    /// Vector length
    pub vector_length: u32,
    /// SIMD throughput
    pub simd_throughput: f64,
}

/// Cortex-X ML acceleration
#[derive(Debug, Clone, Default)]
pub struct CortexXMlAcceleration {
    /// BFloat16 support
    pub bfloat16: bool,
    /// Int8 matrix
    pub int8_matrix: bool,
    /// ML ops throughput
    pub ml_throughput: f64,
}

/// Neoverse coherency optimizations
#[derive(Debug, Clone, Default)]
pub struct NeoverseCoherencyOptimizations {
    /// CMN support
    pub cmn: bool,
    /// SLC support
    pub slc: bool,
    /// Coherency protocol
    pub protocol: String,
}

/// Neoverse mesh optimizations
#[derive(Debug, Clone, Default)]
pub struct NeoverseMeshOptimizations {
    /// Mesh topology
    pub mesh_topology: bool,
    /// Mesh dimensions
    pub dimensions: (u32, u32),
    /// Ring buffer
    pub ring_buffer: bool,
}

/// Neoverse server optimizations
#[derive(Debug, Clone, Default)]
pub struct NeoverseServerOptimizations {
    /// High bandwidth memory
    pub high_bandwidth: bool,
    /// Large cache
    pub large_cache: bool,
    /// RAS features
    pub ras: bool,
}

/// big.LITTLE configuration
#[derive(Debug, Clone, Default)]
pub struct BigLittleConfig {
    /// Big cores count
    pub big_cores: u32,
    /// Little cores count
    pub little_cores: u32,
    /// Heterogeneous scheduling
    pub heterogeneous_scheduling: bool,
}

/// DynamIQ configuration
#[derive(Debug, Clone, Default)]
pub struct DynamiqConfig {
    /// Cluster count
    pub cluster_count: u32,
    /// DSU support
    pub dsu: bool,
    /// Flexible clustering
    pub flexible_clustering: bool,
}

/// Custom silicon configuration
#[derive(Debug, Clone, Default)]
pub struct CustomSiliconConfig {
    /// Vendor name
    pub vendor: String,
    /// Custom features
    pub features: Vec<String>,
    /// Performance tier
    pub performance_tier: u32,
}

/// Apple cache configuration
#[derive(Debug, Clone, Default)]
pub struct AppleCacheConfig {
    /// SLC size
    pub slc_size: usize,
    /// L1 size
    pub l1_size: usize,
    /// L2 size
    pub l2_size: usize,
}

/// Apple execution unit configuration
#[derive(Debug, Clone, Default)]
pub struct AppleExecutionUnitConfig {
    /// Wide decode
    pub wide_decode: bool,
    /// Dispatch width
    pub dispatch_width: u32,
    /// ROB size
    pub rob_size: u32,
}

/// Apple hardware accelerator
#[derive(Debug, Clone, Default)]
pub struct AppleHardwareAccelerator {
    /// Neural engine
    pub neural_engine: bool,
    /// TOPS
    pub tops: f64,
    /// GPU cores
    pub gpu_cores: u32,
}

/// Apple power optimizations
#[derive(Debug, Clone, Default)]
pub struct ApplePowerOptimizations {
    /// Efficiency cores
    pub efficiency_cores: bool,
    /// Power gating
    pub power_gating: bool,
    /// DVFS support
    pub dvfs: bool,
}

/// AMX precision support
#[derive(Debug, Clone, Default)]
pub struct AmxPrecisionSupport {
    /// BFloat16
    pub bfloat16: bool,
    /// Int8
    pub int8: bool,
    /// FP16
    pub fp16: bool,
}

// ============================================================================
// Additional Missing Types - Phase 6.1 Completion
// ============================================================================

/// Access prediction for memory optimization
#[derive(Debug, Clone, Default)]
pub struct AccessPrediction {
    /// Prediction algorithm type
    pub prediction_algorithm: PredictionAlgorithm,
    /// Prediction window
    pub prediction_window: std::time::Duration,
    /// Confidence threshold
    pub confidence_threshold: f64,
}

/// Access priority level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum AccessPriority {
    #[default]
    Normal,
    High,
    Critical,
    RealTime,
}

/// Adaptation algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum AdaptationAlgorithm {
    #[default]
    None,
    ReinforcementLearning,
    GradientDescent,
    GeneticAlgorithm,
    SimulatedAnnealing,
    BayesianOptimization,
}

/// Bandwidth preemption policy
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum BandwidthPreemptionPolicy {
    #[default]
    NoPreemption,
    CooperativePreemption,
    ForcedPreemption,
    PriorityBasedPreemption,
}

/// Bandwidth priority level
#[derive(Debug, Clone, Default)]
pub struct BandwidthPriorityLevel {
    /// Level identifier
    pub level_id: u32,
    /// Level name
    pub name: String,
    /// Bandwidth share percentage
    pub bandwidth_share: f64,
    /// Minimum bandwidth guarantee
    pub minimum_guarantee: f64,
    /// Maximum bandwidth limit
    pub maximum_limit: f64,
}

/// Bandwidth throttling configuration
#[derive(Debug, Clone, Default)]
pub struct BandwidthThrottling {
    /// Throttling enabled
    pub enabled: bool,
    /// Throttle threshold
    pub throttle_threshold: f64,
    /// Throttle factor
    pub throttle_factor: f64,
}

/// Blending algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum BlendingAlgorithm {
    #[default]
    None,
    WeightedAverage,
    LinearInterpolation,
    ExponentialSmoothing,
    MovingAverage,
    Kalman,
}

/// Bypass condition type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum BypassCondition {
    #[default]
    None,
    LargeSequentialAccess,
    LowReuse,
    HighConflict,
    StreamingData,
    TemporaryData,
}

/// Cache sharing level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CacheSharingLevel {
    #[default]
    Private,
    L2,
    L3,
    SharedL2,
    SharedL3,
    FullyShared,
}

/// Clustering algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ClusteringAlgorithm {
    #[default]
    None,
    KMeans,
    DBSCAN,
    Hierarchical,
    SpectralClustering,
    GaussianMixture,
}

/// Coherency optimization level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CoherencyOptimizationLevel {
    #[default]
    None,
    Basic,
    Moderate,
    Aggressive,
}

/// Coherency protocol type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CoherencyProtocol {
    /// No coherency protocol
    #[default]
    None,
    /// MESI protocol (Modified, Exclusive, Shared, Invalid)
    Mesi,
    /// MOESI protocol (Modified, Owned, Exclusive, Shared, Invalid)
    Moesi,
    /// MESIF protocol (MESI + Forward)
    Mesif,
    /// Directory-based protocol
    Directory,
}

/// Conflict resolution type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ConflictResolution {
    #[default]
    None,
    PriorityBased,
    TimestampOrdering,
    RoundRobin,
    LeastRecentlyUsed,
    Random,
}

/// Congestion detection algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CongestionDetectionAlgorithm {
    #[default]
    None,
    QueueDepthBased,
    LatencyBased,
    BandwidthUtilization,
    PacketLoss,
    Combined,
}

/// Congestion detection thresholds
#[derive(Debug, Clone, Default)]
pub struct CongestionDetectionThresholds {
    /// Queue depth threshold
    pub queue_depth_threshold: f64,
    /// Latency threshold multiplier
    pub latency_threshold_multiplier: f64,
    /// Bandwidth threshold
    pub bandwidth_threshold: f64,
}

/// Consistency model for memory
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ConsistencyModel {
    #[default]
    Strong,
    EventualConsistency,
    CausalConsistency,
    SequentialConsistency,
}

/// Controller error handling
#[derive(Debug, Clone, Default)]
pub struct ControllerErrorHandling {
    /// Error correction enabled
    pub error_correction_enabled: bool,
    /// Error reporting level
    pub error_reporting: ErrorReportingLevel,
    /// Recovery strategy
    pub recovery_strategy: ErrorRecoveryStrategy,
}

/// Controller power configuration
#[derive(Debug, Clone, Default)]
pub struct ControllerPowerConfiguration {
    /// Dynamic frequency scaling
    pub dynamic_frequency_scaling: bool,
    /// Power gating enabled
    pub power_gating_enabled: bool,
    /// Idle detection threshold
    pub idle_detection_threshold: std::time::Duration,
}

/// Coordination algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CoordinationAlgorithm {
    #[default]
    None,
    HierarchicalCoordination,
    PeerToPeer,
    Centralized,
    Distributed,
    Consensus,
}

/// Core cluster type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CoreClusterType {
    #[default]
    Homogeneous,
    Heterogeneous,
    BigLittle,
    DynamIQ,
}

/// Core interconnect type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum CoreInterconnectType {
    #[default]
    Bus,
    Ring,
    RingBus,
    Mesh,
    Crossbar,
}

/// Enforcement mechanism type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum EnforcementMechanism {
    #[default]
    None,
    ResourceReservation,
    Throttling,
    Isolation,
    Preemption,
    QoS,
}

/// Enforcement strictness level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum EnforcementStrictness {
    #[default]
    Relaxed,
    Moderate,
    Strict,
    Absolute,
    Soft,
}

/// Error recovery strategy type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ErrorRecoveryStrategy {
    #[default]
    None,
    Retry,
    Fallback,
    Abort,
    Checkpoint,
    Redirect,
}

/// Error reporting level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ErrorReportingLevel {
    #[default]
    None,
    Warning,
    Error,
    Critical,
    All,
    Detailed,
}

/// Generation improvement type
#[derive(Debug, Clone, Default)]
pub struct GenerationImprovement {
    /// Improvement type
    pub improvement_type: ImprovementType,
    /// Description
    pub description: String,
    /// Quantitative benefit
    pub quantitative_benefit: f64,
}

/// Hot data identification
#[derive(Debug, Clone, Default)]
pub struct HotDataIdentification {
    /// Threshold accesses per second
    pub threshold_accesses_per_second: u64,
    /// Time window
    pub time_window: std::time::Duration,
    /// Promotion strategy
    pub promotion_strategy: PromotionStrategy,
}

/// Idle state configuration
#[derive(Debug, Clone, Default)]
pub struct IdleState {
    /// State name (e.g., "C1", "C1E", "C3", "C6")
    pub state_name: String,
    /// Exit latency in microseconds
    pub exit_latency_us: u32,
    /// Power consumption in milliwatts
    pub power_consumption_mw: u32,
}

/// Improvement type for optimization
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ImprovementType {
    #[default]
    Performance,
    PerformanceImprovement,
    Power,
    PowerEfficiency,
    Latency,
    Throughput,
}

/// Interference detection configuration
#[derive(Debug, Clone, Default)]
pub struct InterferenceDetection {
    /// Miss rate correlation
    pub miss_rate_correlation: bool,
    /// Bandwidth correlation
    pub bandwidth_correlation: bool,
    /// Latency correlation
    pub latency_correlation: bool,
}

/// Interference mitigation strategy type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum InterferenceMitigationStrategy {
    #[default]
    None,
    ReducePrefetchAggression,
    SelectivePrefetching,
    PartitionIsolation,
    BandwidthThrottling,
    QueuePrioritization,
}

/// Isolation enforcement type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum IsolationEnforcement {
    #[default]
    None,
    Soft,
    Hard,
    Complete,
    Partitioned,
    Virtualized,
}

/// Load balancing algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum LoadBalancingAlgorithm {
    #[default]
    RoundRobin,
    ProportionalShare,
    LeastLoaded,
    WeightedFair,
}

/// Migration algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum MigrationAlgorithm {
    #[default]
    None,
    GradualMigration,
    ImmediateMigration,
    BatchMigration,
    HotPageMigration,
    AccessBasedMigration,
}

/// Migration trigger condition type
#[derive(Debug, Clone, Copy, PartialEq, Default)]
pub enum MigrationTriggerCondition {
    #[default]
    None,
    AccessPatternChange,
    UtilizationImbalance(f64),
    LocalityDegradation,
    ThermalPressure,
    MemoryPressure,
}

/// Optimization blending configuration
#[derive(Debug, Clone, Default)]
pub struct OptimizationBlending {
    /// Blending algorithm
    pub blending_algorithm: BlendingAlgorithm,
    /// Weight adaptation rate
    pub weight_adaptation_rate: f64,
}

/// Performance feedback mechanism
#[derive(Debug, Clone, Default)]
pub struct PerformanceFeedback {
    /// Latency feedback weight
    pub latency_feedback_weight: f64,
    /// Bandwidth feedback weight
    pub bandwidth_feedback_weight: f64,
    /// Accuracy feedback weight
    pub accuracy_feedback_weight: f64,
}

/// Prediction algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum PredictionAlgorithm {
    #[default]
    None,
    MarkovChain,
    NeuralNetwork,
    DecisionTree,
    LinearRegression,
    TimeSeriesAnalysis,
}

/// Prefetch aggressiveness level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum PrefetchAggressiveness {
    #[default]
    Conservative,
    Moderate,
    Aggressive,
    VeryAggressive,
}

/// Prefetch coordination configuration
#[derive(Debug, Clone, Default)]
pub struct PrefetchCoordination {
    /// Inter-level coordination
    pub inter_level_coordination: bool,
    /// Bandwidth aware prefetch
    pub bandwidth_aware_prefetch: bool,
    /// Adaptive prefetch distance
    pub adaptive_prefetch_distance: bool,
}

/// Prioritization criterion type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum PrioritizationCriterion {
    #[default]
    None,
    AccessPattern,
    RequestType,
    SourceCore,
    LatencyClass,
    ThroughputClass,
}

/// Promotion strategy type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum PromotionStrategy {
    #[default]
    None,
    Immediate,
    Deferred,
    FrequencyBased,
    RecencyBased,
    Adaptive,
}

/// Queue management policy
#[derive(Debug, Clone, Default)]
pub struct QueueManagementPolicy {
    /// Max queue depth
    pub max_queue_depth: u32,
    /// Prioritization enabled
    pub prioritization_enabled: bool,
    /// Queue partitioning
    pub queue_partitioning: QueuePartitioning,
}

/// Queue partitioning type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum QueuePartitioning {
    #[default]
    None,
    PriorityBased,
    SourceBased,
    TypeBased,
    Dynamic,
    Static,
}

/// Replication trigger type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ReplicationTrigger {
    #[default]
    None,
    HighAccessFrequency,
    LatencyThreshold,
    BandwidthSaturation,
    LocalityImbalance,
    Manual,
}

/// Retention priority for cache
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum RetentionPriority {
    #[default]
    Low,
    Medium,
    High,
    Critical,
}

/// Scheduler type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum SchedulerType {
    #[default]
    FIFO,
    RoundRobin,
    Priority,
    CFS,
    Deadline,
    Unified,
}

/// Scheduling optimization algorithm type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum SchedulingOptimizationAlgorithm {
    #[default]
    None,
    MachineLearningBased,
    HeuristicBased,
    GreedyOptimization,
    DynamicProgramming,
    GeneticAlgorithm,
}

/// SMT (Simultaneous Multi-Threading) type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum SmtType {
    #[default]
    None,
    Hyperthreading,
    SMT2,
    SMT4,
    SMT8,
}

/// Stride detection configuration
#[derive(Debug, Clone, Default)]
pub struct StrideDetection {
    /// Detection window
    pub detection_window: usize,
    /// Confidence threshold
    pub confidence_threshold: f64,
    /// Multi stride support
    pub multi_stride_support: bool,
}

/// Stride prefetching configuration
#[derive(Debug, Clone, Default)]
pub struct StridePrefetching {
    /// Prefetch degree
    pub prefetch_degree: u32,
    /// Adaptive degree
    pub adaptive_degree: bool,
    /// Stride history depth
    pub stride_history_depth: usize,
}

/// Throttling fairness metric type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ThrottlingFairnessMetric {
    #[default]
    None,
    ProportionalFair,
    MaxMinFair,
    WeightedFair,
    JainIndex,
    GeneralizedFairness,
}

/// Tracking granularity
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum TrackingGranularity {
    #[default]
    Coarse,
    Medium,
    Fine,
    PerElement,
    PerCore,
}

/// Violation escalation policy
#[derive(Debug, Clone, Default)]
pub struct ViolationEscalationPolicy {
    /// Escalation levels
    pub escalation_levels: u32,
    /// Escalation delay
    pub escalation_delay: std::time::Duration,
}

/// Violation response strategy type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ViolationResponseStrategy {
    #[default]
    None,
    GradualEnforcement,
    ImmediateEnforcement,
    Warning,
    Throttle,
    Block,
}

/// Violation sensitivity level
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum ViolationSensitivity {
    #[default]
    Low,
    Medium,
    High,
    Critical,
}

/// Warming strategy for cache
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum WarmingStrategy {
    #[default]
    None,
    Sequential,
    Random,
    PriorityBased,
    Adaptive,
}

/// Warming trigger type
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum WarmingTrigger {
    #[default]
    None,
    PredictedAccess,
    SystemStartup,
    ContextSwitch,
    PhaseTrigger,
    Manual,
}
