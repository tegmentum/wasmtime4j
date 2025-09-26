//! Advanced CPU cache partitioning and affinity management for wasmtime4j
//!
//! This module provides comprehensive CPU cache partitioning, cache affinity optimization,
//! and intelligent cache resource allocation strategies for maximum performance.

use std::collections::{HashMap, HashSet, VecDeque};
use std::fs;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Advanced CPU cache management system
#[derive(Debug, Clone)]
pub struct CachePartitioningManager {
    /// Cache topology information
    pub cache_topology: CacheTopology,
    /// Active cache partitions
    pub partitions: Vec<CachePartition>,
    /// Cache affinity policies
    pub affinity_policies: Vec<CacheAffinityPolicy>,
    /// Cache allocation strategies
    pub allocation_strategies: HashMap<u32, CacheAllocationStrategy>,
    /// Performance monitoring data
    pub performance_data: Arc<RwLock<CachePerformanceData>>,
    /// Cache interference detection
    pub interference_detector: CacheInterferenceDetector,
    /// Quality of Service management
    pub qos_manager: CacheQosManager,
    /// Cache warming strategies
    pub warming_strategies: Vec<CacheWarmingStrategy>,
}

/// Comprehensive cache topology information
#[derive(Debug, Clone)]
pub struct CacheTopology {
    /// Cache hierarchy levels
    pub levels: Vec<CacheTopologyLevel>,
    /// Cache sharing domains
    pub sharing_domains: Vec<CacheSharingDomain>,
    /// Cache interconnect topology
    pub interconnect_topology: CacheInterconnectTopology,
    /// Cache replacement policies per level
    pub replacement_policies: HashMap<u32, CacheReplacementPolicy>,
    /// Cache coherency information
    pub coherency_info: CacheCoherencyInfo,
    /// Cache bandwidth characteristics
    pub bandwidth_characteristics: Vec<CacheBandwidthInfo>,
    /// Cache latency matrix
    pub latency_matrix: Vec<Vec<u64>>,
}

/// Cache topology level information
#[derive(Debug, Clone)]
pub struct CacheTopologyLevel {
    /// Cache level (L1, L2, L3, etc.)
    pub level: u32,
    /// Cache instances at this level
    pub instances: Vec<CacheInstance>,
    /// Sharing characteristics
    pub sharing_characteristics: CacheSharingCharacteristics,
    /// Partitioning capabilities
    pub partitioning_capabilities: CachePartitioningCapabilities,
    /// Performance characteristics
    pub performance_characteristics: CachePerformanceCharacteristics,
}

/// Individual cache instance
#[derive(Debug, Clone)]
pub struct CacheInstance {
    /// Instance ID
    pub instance_id: u32,
    /// Cache level
    pub level: u32,
    /// Physical cache ID
    pub physical_id: u32,
    /// Cache size in bytes
    pub size: u32,
    /// Cache associativity
    pub associativity: u32,
    /// Cache line size
    pub line_size: u32,
    /// Number of sets
    pub sets: u32,
    /// CPU cores associated with this cache
    pub associated_cores: Vec<u32>,
    /// Cache type (unified, instruction, data)
    pub cache_type: CacheType,
    /// Inclusive/exclusive relationship
    pub inclusion_policy: CacheInclusionPolicy,
    /// Partitioning support
    pub partitioning_support: PartitioningSupportLevel,
    /// Current partitions
    pub current_partitions: Vec<u32>,
}

/// Cache sharing domain
#[derive(Debug, Clone)]
pub struct CacheSharingDomain {
    /// Domain ID
    pub domain_id: u32,
    /// CPU cores in this domain
    pub cores: Vec<u32>,
    /// Shared cache levels
    pub shared_levels: Vec<u32>,
    /// Sharing policy
    pub sharing_policy: CacheSharingPolicy,
    /// Interference characteristics
    pub interference_characteristics: InterferenceCharacteristics,
    /// Bandwidth sharing
    pub bandwidth_sharing: BandwidthSharingInfo,
}

/// Cache interconnect topology
#[derive(Debug, Clone)]
pub struct CacheInterconnectTopology {
    /// Ring topology information
    pub ring_topology: Option<RingTopologyInfo>,
    /// Mesh topology information
    pub mesh_topology: Option<MeshTopologyInfo>,
    /// Tree topology information
    pub tree_topology: Option<TreeTopologyInfo>,
    /// Custom topology information
    pub custom_topology: Option<CustomTopologyInfo>,
    /// Inter-cache communication latencies
    pub communication_latencies: HashMap<(u32, u32), u64>,
    /// Bandwidth between cache levels
    pub inter_level_bandwidth: HashMap<(u32, u32), f64>,
}

/// Cache coherency information
#[derive(Debug, Clone)]
pub struct CacheCoherencyInfo {
    /// Coherency protocol
    pub protocol: CacheCoherencyProtocol,
    /// Coherency domains
    pub coherency_domains: Vec<CoherencyDomain>,
    /// Snooping characteristics
    pub snooping_characteristics: SnoopingCharacteristics,
    /// Directory information (if applicable)
    pub directory_info: Option<DirectoryInfo>,
    /// Coherency traffic overhead
    pub coherency_overhead: CoherencyOverhead,
}

/// Cache partition definition
#[derive(Debug, Clone)]
pub struct CachePartition {
    /// Partition ID
    pub partition_id: u32,
    /// Cache level
    pub cache_level: u32,
    /// Cache instance ID
    pub cache_instance_id: u32,
    /// Partition name
    pub name: String,
    /// Allocated cache ways/sets
    pub allocated_ways: Vec<u32>,
    /// Allocated cache size
    pub allocated_size: u32,
    /// Associated CPU cores
    pub cpu_cores: Vec<u32>,
    /// Partition priority
    pub priority: u32,
    /// Partition type
    pub partition_type: PartitionType,
    /// Quality of Service settings
    pub qos_settings: QosSettings,
    /// Performance guarantees
    pub performance_guarantees: PerformanceGuarantees,
    /// Isolation level
    pub isolation_level: IsolationLevel,
}

/// Cache affinity policy
#[derive(Debug, Clone)]
pub struct CacheAffinityPolicy {
    /// Policy ID
    pub policy_id: u32,
    /// Policy name
    pub name: String,
    /// Target CPU cores
    pub target_cores: Vec<u32>,
    /// Cache levels to optimize
    pub cache_levels: Vec<u32>,
    /// Affinity strategy
    pub strategy: AffinityStrategy,
    /// Data placement hints
    pub placement_hints: Vec<DataPlacementHint>,
    /// Migration policies
    pub migration_policies: MigrationPolicy,
    /// Prefetching configuration
    pub prefetching_config: PrefetchingConfiguration,
}

/// Cache allocation strategy
#[derive(Debug, Clone)]
pub struct CacheAllocationStrategy {
    /// Strategy ID
    pub strategy_id: u32,
    /// Strategy name
    pub name: String,
    /// Allocation algorithm
    pub algorithm: AllocationAlgorithm,
    /// Priority scheme
    pub priority_scheme: PriorityScheme,
    /// Dynamic adjustment
    pub dynamic_adjustment: DynamicAdjustmentConfig,
    /// Fairness policy
    pub fairness_policy: FairnessPolicy,
    /// Eviction policy
    pub eviction_policy: EvictionPolicy,
    /// Replacement hints
    pub replacement_hints: ReplacementHints,
}

/// Cache performance monitoring data
#[derive(Debug)]
pub struct CachePerformanceData {
    /// Per-cache hit rates
    pub hit_rates: HashMap<u32, f64>,
    /// Per-cache miss rates
    pub miss_rates: HashMap<u32, f64>,
    /// Cache utilization rates
    pub utilization_rates: HashMap<u32, f64>,
    /// Bandwidth utilization
    pub bandwidth_utilization: HashMap<u32, f64>,
    /// Latency distributions
    pub latency_distributions: HashMap<u32, LatencyDistribution>,
    /// Interference metrics
    pub interference_metrics: HashMap<u32, InterferenceMetrics>,
    /// Power consumption per cache
    pub power_consumption: HashMap<u32, f64>,
    /// Thermal characteristics
    pub thermal_characteristics: HashMap<u32, ThermalCharacteristics>,
    /// Performance counters
    pub performance_counters: HashMap<u32, PerformanceCounters>,
    /// Time series data
    pub time_series: VecDeque<CachePerformanceSnapshot>,
}

/// Cache interference detection system
#[derive(Debug, Clone)]
pub struct CacheInterferenceDetector {
    /// Detection algorithms
    pub detection_algorithms: Vec<InterferenceDetectionAlgorithm>,
    /// Interference patterns
    pub interference_patterns: Vec<InterferencePattern>,
    /// Detection thresholds
    pub detection_thresholds: InterferenceThresholds,
    /// Mitigation strategies
    pub mitigation_strategies: Vec<InterferenceMitigationStrategy>,
    /// Real-time monitoring
    pub realtime_monitoring: RealtimeMonitoringConfig,
}

/// Cache Quality of Service manager
#[derive(Debug, Clone)]
pub struct CacheQosManager {
    /// QoS policies
    pub qos_policies: Vec<CacheQosPolicy>,
    /// Resource reservations
    pub resource_reservations: Vec<ResourceReservation>,
    /// Service level agreements
    pub service_agreements: Vec<ServiceLevelAgreement>,
    /// Enforcement mechanisms
    pub enforcement_mechanisms: Vec<EnforcementMechanism>,
    /// Monitoring and reporting
    pub monitoring_config: QosMonitoringConfig,
}

/// Cache warming strategy
#[derive(Debug, Clone)]
pub struct CacheWarmingStrategy {
    /// Strategy ID
    pub strategy_id: u32,
    /// Strategy name
    pub name: String,
    /// Warming algorithm
    pub algorithm: WarmingAlgorithm,
    /// Target cache levels
    pub target_levels: Vec<u32>,
    /// Data patterns to warm
    pub data_patterns: Vec<DataPattern>,
    /// Timing configuration
    pub timing_config: WarmingTimingConfig,
    /// Effectiveness metrics
    pub effectiveness_metrics: WarmingEffectivenessMetrics,
}

// Additional supporting structures and enums

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CacheType {
    Unified,
    Instruction,
    Data,
    Translation,
    Trace,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CacheInclusionPolicy {
    Inclusive,
    Exclusive,
    NonInclusive,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PartitioningSupportLevel {
    None,
    WayBased,
    SetBased,
    AddressBased,
    ContentBased,
    HybridBased,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CacheSharingPolicy {
    Exclusive,
    Shared,
    PartitionedShared,
    DynamicShared,
    PriorityBased,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CacheCoherencyProtocol {
    Mesi,
    Moesi,
    Mesif,
    Dragon,
    Firefly,
    DirectoryBased,
    TokenBased,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PartitionType {
    Static,
    Dynamic,
    Adaptive,
    Hierarchical,
    Temporal,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum IsolationLevel {
    None,
    Soft,
    Hard,
    Complete,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AffinityStrategy {
    LocalityAware,
    LoadBalanced,
    PerformanceOptimized,
    PowerEfficient,
    LatencyOptimized,
    BandwidthOptimized,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AllocationAlgorithm {
    FirstFit,
    BestFit,
    WorstFit,
    NextFit,
    BuddySystem,
    SlabAllocator,
    ColoringBased,
    UtilityBased,
}

#[derive(Debug, Clone)]
pub struct CacheSharingCharacteristics {
    /// Sharing granularity
    pub granularity: u32,
    /// Sharing bandwidth
    pub bandwidth: f64,
    /// Contention characteristics
    pub contention_characteristics: ContentionCharacteristics,
    /// False sharing probability
    pub false_sharing_probability: f64,
}

#[derive(Debug, Clone)]
pub struct CachePartitioningCapabilities {
    /// Way-based partitioning
    pub way_based: bool,
    /// Set-based partitioning
    pub set_based: bool,
    /// Address-based partitioning
    pub address_based: bool,
    /// Dynamic partitioning
    pub dynamic_partitioning: bool,
    /// Minimum partition size
    pub min_partition_size: u32,
    /// Maximum partitions
    pub max_partitions: u32,
    /// Partitioning overhead
    pub partitioning_overhead: f64,
}

#[derive(Debug, Clone)]
pub struct CachePerformanceCharacteristics {
    /// Access latency distribution
    pub access_latency: LatencyDistribution,
    /// Bandwidth characteristics
    pub bandwidth_characteristics: BandwidthCharacteristics,
    /// Power characteristics
    pub power_characteristics: PowerCharacteristics,
    /// Thermal characteristics
    pub thermal_characteristics: ThermalCharacteristics,
    /// Reliability characteristics
    pub reliability_characteristics: ReliabilityCharacteristics,
}

#[derive(Debug, Clone)]
pub struct InterferenceCharacteristics {
    /// Interference sensitivity
    pub sensitivity: f64,
    /// Interference generation
    pub generation: f64,
    /// Mitigation effectiveness
    pub mitigation_effectiveness: f64,
    /// Recovery time
    pub recovery_time: Duration,
}

#[derive(Debug, Clone)]
pub struct BandwidthSharingInfo {
    /// Total bandwidth
    pub total_bandwidth: f64,
    /// Per-core allocation
    pub per_core_allocation: HashMap<u32, f64>,
    /// Sharing fairness
    pub sharing_fairness: f64,
    /// Dynamic allocation
    pub dynamic_allocation: bool,
}

#[derive(Debug, Clone)]
pub struct RingTopologyInfo {
    /// Ring nodes
    pub nodes: Vec<u32>,
    /// Ring direction
    pub direction: RingDirection,
    /// Hop latencies
    pub hop_latencies: Vec<u64>,
    /// Bandwidth per hop
    pub hop_bandwidths: Vec<f64>,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum RingDirection {
    Clockwise,
    CounterClockwise,
    Bidirectional,
}

#[derive(Debug, Clone)]
pub struct MeshTopologyInfo {
    /// Mesh dimensions
    pub dimensions: (u32, u32),
    /// Node positions
    pub node_positions: HashMap<u32, (u32, u32)>,
    /// Link latencies
    pub link_latencies: HashMap<(u32, u32), u64>,
    /// Link bandwidths
    pub link_bandwidths: HashMap<(u32, u32), f64>,
}

#[derive(Debug, Clone)]
pub struct TreeTopologyInfo {
    /// Tree root
    pub root: u32,
    /// Parent-child relationships
    pub parent_child: HashMap<u32, Vec<u32>>,
    /// Level latencies
    pub level_latencies: Vec<u64>,
    /// Level bandwidths
    pub level_bandwidths: Vec<f64>,
}

#[derive(Debug, Clone)]
pub struct CustomTopologyInfo {
    /// Adjacency matrix
    pub adjacency_matrix: Vec<Vec<bool>>,
    /// Node properties
    pub node_properties: HashMap<u32, HashMap<String, String>>,
    /// Edge properties
    pub edge_properties: HashMap<(u32, u32), HashMap<String, String>>,
}

#[derive(Debug, Clone)]
pub struct CoherencyDomain {
    /// Domain ID
    pub domain_id: u32,
    /// Participating caches
    pub caches: Vec<u32>,
    /// Coherency protocol
    pub protocol: CacheCoherencyProtocol,
    /// Snooping configuration
    pub snooping_config: SnoopingConfiguration,
}

#[derive(Debug, Clone)]
pub struct SnoopingCharacteristics {
    /// Snoop latency
    pub snoop_latency: u64,
    /// Snoop bandwidth
    pub snoop_bandwidth: f64,
    /// Snoop filtering
    pub snoop_filtering: bool,
    /// Snoop response time
    pub response_time: u64,
}

#[derive(Debug, Clone)]
pub struct DirectoryInfo {
    /// Directory location
    pub location: DirectoryLocation,
    /// Directory size
    pub size: u32,
    /// Directory associativity
    pub associativity: u32,
    /// Directory latency
    pub latency: u64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum DirectoryLocation {
    Centralized,
    Distributed,
    Hierarchical,
}

#[derive(Debug, Clone)]
pub struct CoherencyOverhead {
    /// Traffic overhead percentage
    pub traffic_overhead: f64,
    /// Latency overhead
    pub latency_overhead: u64,
    /// Power overhead
    pub power_overhead: f64,
    /// Bandwidth overhead
    pub bandwidth_overhead: f64,
}

#[derive(Debug, Clone)]
pub struct QosSettings {
    /// Minimum bandwidth guarantee
    pub min_bandwidth: f64,
    /// Maximum bandwidth limit
    pub max_bandwidth: f64,
    /// Latency guarantee
    pub latency_guarantee: u64,
    /// Priority level
    pub priority_level: u32,
    /// Preemption policy
    pub preemption_policy: PreemptionPolicy,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PreemptionPolicy {
    NonPreemptive,
    CooperativePreemptive,
    ForcedPreemptive,
}

#[derive(Debug, Clone)]
pub struct PerformanceGuarantees {
    /// Minimum hit rate
    pub min_hit_rate: f64,
    /// Maximum miss penalty
    pub max_miss_penalty: u64,
    /// Bandwidth guarantee
    pub bandwidth_guarantee: f64,
    /// Latency bound
    pub latency_bound: u64,
}

#[derive(Debug, Clone)]
pub struct DataPlacementHint {
    /// Hint type
    pub hint_type: HintType,
    /// Target cache level
    pub target_level: u32,
    /// Data address range
    pub address_range: (u64, u64),
    /// Access pattern
    pub access_pattern: AccessPattern,
    /// Lifetime hint
    pub lifetime_hint: LifetimeHint,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum HintType {
    Prefetch,
    Pin,
    Evict,
    Migrate,
    Replicate,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AccessPattern {
    Sequential,
    Random,
    Strided,
    Temporal,
    Spatial,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LifetimeHint {
    Transient,
    ShortTerm,
    MediumTerm,
    LongTerm,
    Permanent,
}

#[derive(Debug, Clone)]
pub struct MigrationPolicy {
    /// Migration triggers
    pub triggers: Vec<MigrationTrigger>,
    /// Migration cost model
    pub cost_model: MigrationCostModel,
    /// Migration frequency limit
    pub frequency_limit: f64,
    /// Migration batch size
    pub batch_size: u32,
}

#[derive(Debug, Clone)]
pub struct MigrationTrigger {
    /// Trigger type
    pub trigger_type: TriggerType,
    /// Threshold value
    pub threshold: f64,
    /// Evaluation window
    pub evaluation_window: Duration,
    /// Hysteresis factor
    pub hysteresis: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum TriggerType {
    UtilizationBased,
    LatencyBased,
    BandwidthBased,
    InterferenceBased,
    PowerBased,
}

#[derive(Debug, Clone)]
pub struct MigrationCostModel {
    /// Base migration cost
    pub base_cost: f64,
    /// Distance cost factor
    pub distance_factor: f64,
    /// Size cost factor
    pub size_factor: f64,
    /// Contention cost factor
    pub contention_factor: f64,
}

#[derive(Debug, Clone)]
pub struct PrefetchingConfiguration {
    /// Prefetch algorithms
    pub algorithms: Vec<PrefetchAlgorithm>,
    /// Prefetch distance
    pub distance: u32,
    /// Prefetch degree
    pub degree: u32,
    /// Confidence threshold
    pub confidence_threshold: f64,
    /// Prefetch filtering
    pub filtering_enabled: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum PrefetchAlgorithm {
    NextLine,
    Stride,
    StreamBuffer,
    CorrelationBased,
    MachineLearningBased,
}

#[derive(Debug, Clone)]
pub struct PriorityScheme {
    /// Priority levels
    pub levels: Vec<PriorityLevel>,
    /// Priority inheritance
    pub inheritance: bool,
    /// Priority inversion handling
    pub inversion_handling: InversionHandling,
    /// Dynamic priority adjustment
    pub dynamic_adjustment: bool,
}

#[derive(Debug, Clone)]
pub struct PriorityLevel {
    /// Level ID
    pub level_id: u32,
    /// Level name
    pub name: String,
    /// Resource share
    pub resource_share: f64,
    /// Preemption rights
    pub preemption_rights: bool,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InversionHandling {
    None,
    PriorityInheritance,
    PriorityCeiling,
    Avoid,
}

#[derive(Debug, Clone)]
pub struct DynamicAdjustmentConfig {
    /// Adjustment frequency
    pub frequency: Duration,
    /// Adjustment algorithm
    pub algorithm: AdjustmentAlgorithm,
    /// Stability threshold
    pub stability_threshold: f64,
    /// Maximum adjustment rate
    pub max_adjustment_rate: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AdjustmentAlgorithm {
    ProportionalIntegralDerivative,
    ExponentialWeightedMovingAverage,
    FeedbackControl,
    ReinforcementLearning,
}

#[derive(Debug, Clone)]
pub struct FairnessPolicy {
    /// Fairness metric
    pub metric: FairnessMetric,
    /// Fairness target
    pub target: f64,
    /// Enforcement mechanism
    pub enforcement: FairnessEnforcement,
    /// Time window
    pub time_window: Duration,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FairnessMetric {
    ProportionalShare,
    MaxMin,
    WeightedFair,
    Jains,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum FairnessEnforcement {
    SoftLimit,
    HardLimit,
    Throttling,
    Preemption,
}

#[derive(Debug, Clone)]
pub struct EvictionPolicy {
    /// Eviction algorithm
    pub algorithm: EvictionAlgorithm,
    /// Victim selection criteria
    pub victim_selection: VictimSelection,
    /// Write-back policy
    pub writeback_policy: WritebackPolicy,
    /// Eviction batching
    pub batching: EvictionBatching,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EvictionAlgorithm {
    Lru,
    Lfu,
    Clock,
    Adaptive,
    DeadBlockPrediction,
}

#[derive(Debug, Clone)]
pub struct VictimSelection {
    /// Selection criteria
    pub criteria: Vec<SelectionCriterion>,
    /// Weighting factors
    pub weights: HashMap<SelectionCriterion, f64>,
    /// Selection randomization
    pub randomization: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum SelectionCriterion {
    Age,
    Frequency,
    Recency,
    Size,
    Priority,
    Locality,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WritebackPolicy {
    WriteThrough,
    WriteBack,
    WriteBehind,
    Hybrid,
}

#[derive(Debug, Clone)]
pub struct EvictionBatching {
    /// Batch size
    pub batch_size: u32,
    /// Batching trigger
    pub trigger: BatchingTrigger,
    /// Batch processing delay
    pub processing_delay: Duration,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BatchingTrigger {
    Threshold,
    TimeBasedBased,
    HybridBased,
}

#[derive(Debug, Clone)]
pub struct ReplacementHints {
    /// Replacement probability hints
    pub probability_hints: HashMap<u64, f64>,
    /// Cache line death predictions
    pub death_predictions: HashMap<u64, Instant>,
    /// Reuse distance predictions
    pub reuse_predictions: HashMap<u64, u32>,
    /// Utility scores
    pub utility_scores: HashMap<u64, f64>,
}

#[derive(Debug, Clone)]
pub struct LatencyDistribution {
    /// Mean latency
    pub mean: f64,
    /// Standard deviation
    pub std_dev: f64,
    /// Percentiles
    pub percentiles: HashMap<u32, u64>,
    /// Histogram buckets
    pub histogram: Vec<(u64, u32)>,
}

#[derive(Debug, Clone)]
pub struct InterferenceMetrics {
    /// Interference level
    pub level: f64,
    /// Sources of interference
    pub sources: Vec<u32>,
    /// Impact on performance
    pub performance_impact: f64,
    /// Mitigation effectiveness
    pub mitigation_effectiveness: f64,
}

#[derive(Debug, Clone)]
pub struct ThermalCharacteristics {
    /// Current temperature
    pub temperature: f64,
    /// Temperature gradient
    pub gradient: f64,
    /// Thermal capacity
    pub capacity: f64,
    /// Cooling efficiency
    pub cooling_efficiency: f64,
}

#[derive(Debug, Clone)]
pub struct PerformanceCounters {
    /// Cache hits
    pub hits: u64,
    /// Cache misses
    pub misses: u64,
    /// Evictions
    pub evictions: u64,
    /// Writebacks
    pub writebacks: u64,
    /// Coherency events
    pub coherency_events: u64,
    /// Prefetch events
    pub prefetch_events: u64,
}

#[derive(Debug, Clone)]
pub struct CachePerformanceSnapshot {
    /// Timestamp
    pub timestamp: Instant,
    /// Per-cache metrics
    pub cache_metrics: HashMap<u32, CacheMetrics>,
    /// System-wide metrics
    pub system_metrics: SystemMetrics,
}

#[derive(Debug, Clone)]
pub struct CacheMetrics {
    /// Hit rate
    pub hit_rate: f64,
    /// Miss rate
    pub miss_rate: f64,
    /// Utilization
    pub utilization: f64,
    /// Bandwidth usage
    pub bandwidth_usage: f64,
    /// Average latency
    pub avg_latency: u64,
}

#[derive(Debug, Clone)]
pub struct SystemMetrics {
    /// Overall hit rate
    pub overall_hit_rate: f64,
    /// Memory bandwidth utilization
    pub memory_bandwidth_utilization: f64,
    /// Cache coherency overhead
    pub coherency_overhead: f64,
    /// Power consumption
    pub power_consumption: f64,
}

impl CachePartitioningManager {
    /// Create a new cache partitioning manager
    pub fn new() -> WasmtimeResult<Self> {
        let cache_topology = Self::detect_cache_topology()?;
        let performance_data = Arc::new(RwLock::new(CachePerformanceData::new()));
        let interference_detector = CacheInterferenceDetector::new(&cache_topology)?;
        let qos_manager = CacheQosManager::new()?;

        Ok(Self {
            cache_topology,
            partitions: Vec::new(),
            affinity_policies: Vec::new(),
            allocation_strategies: HashMap::new(),
            performance_data,
            interference_detector,
            qos_manager,
            warming_strategies: Vec::new(),
        })
    }

    /// Detect cache topology from system information
    fn detect_cache_topology() -> WasmtimeResult<CacheTopology> {
        let mut levels = Vec::new();
        let mut sharing_domains = Vec::new();

        // Detect cache levels
        for level in 1..=4 {
            if let Ok(level_info) = Self::detect_cache_level(level) {
                levels.push(level_info);
            }
        }

        // Detect sharing domains
        sharing_domains = Self::detect_sharing_domains(&levels)?;

        // Detect interconnect topology
        let interconnect_topology = Self::detect_interconnect_topology()?;

        // Initialize other topology information
        let replacement_policies = Self::detect_replacement_policies(&levels)?;
        let coherency_info = Self::detect_coherency_info()?;
        let bandwidth_characteristics = Self::detect_bandwidth_characteristics(&levels)?;
        let latency_matrix = Self::detect_latency_matrix(&levels)?;

        Ok(CacheTopology {
            levels,
            sharing_domains,
            interconnect_topology,
            replacement_policies,
            coherency_info,
            bandwidth_characteristics,
            latency_matrix,
        })
    }

    fn detect_cache_level(level: u32) -> WasmtimeResult<CacheTopologyLevel> {
        let mut instances = Vec::new();

        // Try to detect cache instances from /sys filesystem
        if let Ok(entries) = fs::read_dir("/sys/devices/system/cpu") {
            let mut cpu_caches = HashMap::new();

            for entry in entries.filter_map(|e| e.ok()) {
                if let Some(cpu_name) = entry.file_name().to_str() {
                    if cpu_name.starts_with("cpu") && cpu_name[3..].chars().all(|c| c.is_ascii_digit()) {
                        if let Ok(cpu_id) = cpu_name[3..].parse::<u32>() {
                            let cache_path = entry.path().join("cache");
                            if let Ok(cache_entries) = fs::read_dir(&cache_path) {
                                for cache_entry in cache_entries.filter_map(|e| e.ok()) {
                                    if let Some(index_name) = cache_entry.file_name().to_str() {
                                        if index_name.starts_with("index") {
                                            let level_path = cache_entry.path().join("level");
                                            if let Ok(cache_level_str) = fs::read_to_string(&level_path) {
                                                if let Ok(cache_level) = cache_level_str.trim().parse::<u32>() {
                                                    if cache_level == level {
                                                        if let Ok(cache_info) = Self::read_cache_info(&cache_entry.path(), cpu_id) {
                                                            cpu_caches.insert(cpu_id, cache_info);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Group caches by physical cache instance
            let mut physical_caches: HashMap<u32, Vec<u32>> = HashMap::new();
            for (cpu_id, cache_info) in &cpu_caches {
                physical_caches.entry(cache_info.physical_id).or_insert_with(Vec::new).push(*cpu_id);
            }

            // Create cache instances
            for (physical_id, cpu_list) in physical_caches {
                if let Some((_, cache_info)) = cpu_caches.iter().find(|(cpu, _)| cpu_list.contains(cpu)) {
                    instances.push(CacheInstance {
                        instance_id: instances.len() as u32,
                        level,
                        physical_id,
                        size: cache_info.size,
                        associativity: cache_info.associativity,
                        line_size: cache_info.line_size,
                        sets: cache_info.sets,
                        associated_cores: cpu_list,
                        cache_type: cache_info.cache_type,
                        inclusion_policy: cache_info.inclusion_policy,
                        partitioning_support: Self::detect_partitioning_support(level, physical_id),
                        current_partitions: Vec::new(),
                    });
                }
            }
        }

        // If no instances detected, create a default one
        if instances.is_empty() {
            instances.push(CacheInstance {
                instance_id: 0,
                level,
                physical_id: 0,
                size: match level {
                    1 => 32 * 1024,
                    2 => 256 * 1024,
                    3 => 8 * 1024 * 1024,
                    _ => 1024 * 1024,
                },
                associativity: 8,
                line_size: 64,
                sets: 64,
                associated_cores: vec![0],
                cache_type: CacheType::Unified,
                inclusion_policy: CacheInclusionPolicy::Inclusive,
                partitioning_support: PartitioningSupportLevel::None,
                current_partitions: Vec::new(),
            });
        }

        let sharing_characteristics = CacheSharingCharacteristics {
            granularity: 64,
            bandwidth: 100.0,
            contention_characteristics: ContentionCharacteristics {
                max_contention: 1.0,
                contention_threshold: 0.8,
                resolution_time: Duration::from_micros(100),
            },
            false_sharing_probability: 0.1,
        };

        let partitioning_capabilities = CachePartitioningCapabilities {
            way_based: level >= 3,
            set_based: false,
            address_based: false,
            dynamic_partitioning: level >= 3,
            min_partition_size: if level >= 3 { instances[0].size / 16 } else { instances[0].size },
            max_partitions: if level >= 3 { 16 } else { 1 },
            partitioning_overhead: 0.05,
        };

        let performance_characteristics = CachePerformanceCharacteristics {
            access_latency: LatencyDistribution {
                mean: match level {
                    1 => 1.0,
                    2 => 3.0,
                    3 => 10.0,
                    _ => 50.0,
                },
                std_dev: 0.5,
                percentiles: HashMap::new(),
                histogram: Vec::new(),
            },
            bandwidth_characteristics: BandwidthCharacteristics {
                peak_bandwidth: match level {
                    1 => 1000.0,
                    2 => 500.0,
                    3 => 200.0,
                    _ => 100.0,
                },
                sustained_bandwidth: match level {
                    1 => 800.0,
                    2 => 400.0,
                    3 => 150.0,
                    _ => 80.0,
                },
                bandwidth_efficiency: 0.8,
            },
            power_characteristics: PowerCharacteristics {
                static_power: match level {
                    1 => 0.1,
                    2 => 0.5,
                    3 => 2.0,
                    _ => 1.0,
                },
                dynamic_power_per_access: match level {
                    1 => 0.001,
                    2 => 0.01,
                    3 => 0.1,
                    _ => 0.05,
                },
                leakage_power: match level {
                    1 => 0.01,
                    2 => 0.05,
                    3 => 0.2,
                    _ => 0.1,
                },
            },
            thermal_characteristics: ThermalCharacteristics {
                temperature: 45.0,
                gradient: 0.1,
                capacity: 100.0,
                cooling_efficiency: 0.9,
            },
            reliability_characteristics: ReliabilityCharacteristics {
                error_rate: 1e-12,
                mtbf: Duration::from_secs(365 * 24 * 3600 * 10), // 10 years
                correction_capability: ErrorCorrectionCapability::Single,
                detection_capability: ErrorDetectionCapability::Double,
            },
        };

        Ok(CacheTopologyLevel {
            level,
            instances,
            sharing_characteristics,
            partitioning_capabilities,
            performance_characteristics,
        })
    }

    fn read_cache_info(cache_path: &std::path::Path, cpu_id: u32) -> WasmtimeResult<CacheInstanceInfo> {
        let size_path = cache_path.join("size");
        let ways_path = cache_path.join("ways_of_associativity");
        let line_size_path = cache_path.join("coherency_line_size");
        let type_path = cache_path.join("type");
        let shared_cpu_list_path = cache_path.join("shared_cpu_list");

        let size_str = fs::read_to_string(&size_path).unwrap_or_else(|_| "32K".to_string());
        let size = Self::parse_cache_size(&size_str)?;

        let associativity = fs::read_to_string(&ways_path)
            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
            .unwrap_or(8);

        let line_size = fs::read_to_string(&line_size_path)
            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
            .unwrap_or(64);

        let cache_type_str = fs::read_to_string(&type_path).unwrap_or_else(|_| "Unified".to_string());
        let cache_type = match cache_type_str.trim().to_lowercase().as_str() {
            "data" => CacheType::Data,
            "instruction" => CacheType::Instruction,
            "unified" => CacheType::Unified,
            _ => CacheType::Unified,
        };

        let sets = if associativity > 0 { size / (associativity * line_size) } else { 1 };

        // Try to determine physical cache ID from shared CPU list
        let shared_cpus = if let Ok(shared_list) = fs::read_to_string(&shared_cpu_list_path) {
            Self::parse_cpu_list(&shared_list.trim()).unwrap_or_else(|_| vec![cpu_id])
        } else {
            vec![cpu_id]
        };

        let physical_id = shared_cpus.iter().min().copied().unwrap_or(cpu_id);

        Ok(CacheInstanceInfo {
            size,
            associativity,
            line_size,
            sets,
            cache_type,
            physical_id,
            inclusion_policy: CacheInclusionPolicy::Inclusive, // Default assumption
        })
    }

    fn parse_cache_size(size_str: &str) -> WasmtimeResult<u32> {
        let size_str = size_str.trim().to_uppercase();
        let (num_part, suffix) = if size_str.ends_with('K') {
            (&size_str[..size_str.len()-1], 1024)
        } else if size_str.ends_with('M') {
            (&size_str[..size_str.len()-1], 1024 * 1024)
        } else if size_str.ends_with('G') {
            (&size_str[..size_str.len()-1], 1024 * 1024 * 1024)
        } else {
            (size_str.as_str(), 1)
        };

        let num: u32 = num_part.parse().map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to parse cache size: {}", e),
        })?;

        Ok(num * suffix)
    }

    fn parse_cpu_list(cpu_list: &str) -> WasmtimeResult<Vec<u32>> {
        let mut cpus = Vec::new();

        for part in cpu_list.split(',') {
            let part = part.trim();
            if part.contains('-') {
                let range_parts: Vec<&str> = part.split('-').collect();
                if range_parts.len() == 2 {
                    if let (Ok(start), Ok(end)) = (range_parts[0].parse::<u32>(), range_parts[1].parse::<u32>()) {
                        for cpu in start..=end {
                            cpus.push(cpu);
                        }
                    }
                }
            } else if let Ok(cpu) = part.parse::<u32>() {
                cpus.push(cpu);
            }
        }

        Ok(cpus)
    }

    fn detect_partitioning_support(level: u32, physical_id: u32) -> PartitioningSupportLevel {
        // Check for Intel Cache Allocation Technology (CAT)
        if level == 3 {
            if let Ok(cat_info) = fs::read_to_string("/sys/fs/resctrl/info/L3/cbm_mask") {
                if !cat_info.trim().is_empty() {
                    return PartitioningSupportLevel::WayBased;
                }
            }
        }

        // Check for Intel Memory Bandwidth Allocation (MBA)
        if level == 3 {
            if let Ok(_mba_info) = fs::read_to_string("/sys/fs/resctrl/info/MB/bandwidth_gran") {
                return PartitioningSupportLevel::WayBased;
            }
        }

        // Default: no partitioning support
        match level {
            3 => PartitioningSupportLevel::WayBased, // Assume L3 supports way-based partitioning
            _ => PartitioningSupportLevel::None,
        }
    }

    fn detect_sharing_domains(levels: &[CacheTopologyLevel]) -> WasmtimeResult<Vec<CacheSharingDomain>> {
        let mut domains = Vec::new();

        for level_info in levels {
            for instance in &level_info.instances {
                let domain = CacheSharingDomain {
                    domain_id: domains.len() as u32,
                    cores: instance.associated_cores.clone(),
                    shared_levels: vec![level_info.level],
                    sharing_policy: CacheSharingPolicy::Shared,
                    interference_characteristics: InterferenceCharacteristics {
                        sensitivity: 0.5,
                        generation: 0.3,
                        mitigation_effectiveness: 0.7,
                        recovery_time: Duration::from_millis(100),
                    },
                    bandwidth_sharing: BandwidthSharingInfo {
                        total_bandwidth: 100.0,
                        per_core_allocation: instance.associated_cores.iter()
                            .map(|&core| (core, 100.0 / instance.associated_cores.len() as f64))
                            .collect(),
                        sharing_fairness: 0.9,
                        dynamic_allocation: true,
                    },
                };
                domains.push(domain);
            }
        }

        Ok(domains)
    }

    fn detect_interconnect_topology() -> WasmtimeResult<CacheInterconnectTopology> {
        // For now, assume a simple ring topology
        Ok(CacheInterconnectTopology {
            ring_topology: Some(RingTopologyInfo {
                nodes: vec![0, 1, 2, 3],
                direction: RingDirection::Bidirectional,
                hop_latencies: vec![10, 20, 30],
                hop_bandwidths: vec![100.0, 80.0, 60.0],
            }),
            mesh_topology: None,
            tree_topology: None,
            custom_topology: None,
            communication_latencies: HashMap::new(),
            inter_level_bandwidth: HashMap::new(),
        })
    }

    fn detect_replacement_policies(levels: &[CacheTopologyLevel]) -> WasmtimeResult<HashMap<u32, CacheReplacementPolicy>> {
        let mut policies = HashMap::new();

        for level_info in levels {
            // Most modern caches use LRU or pseudo-LRU
            let policy = match level_info.level {
                1 => CacheReplacementPolicy::Lru,
                2 => CacheReplacementPolicy::Lru,
                3 => CacheReplacementPolicy::TreePseudoLru,
                _ => CacheReplacementPolicy::Lru,
            };
            policies.insert(level_info.level, policy);
        }

        Ok(policies)
    }

    fn detect_coherency_info() -> WasmtimeResult<CacheCoherencyInfo> {
        Ok(CacheCoherencyInfo {
            protocol: CacheCoherencyProtocol::Mesi,
            coherency_domains: vec![
                CoherencyDomain {
                    domain_id: 0,
                    caches: vec![0, 1, 2],
                    protocol: CacheCoherencyProtocol::Mesi,
                    snooping_config: SnoopingConfiguration {
                        snoop_filter_enabled: true,
                        broadcast_snooping: false,
                        directory_based: false,
                    },
                },
            ],
            snooping_characteristics: SnoopingCharacteristics {
                snoop_latency: 10,
                snoop_bandwidth: 50.0,
                snoop_filtering: true,
                response_time: 5,
            },
            directory_info: None,
            coherency_overhead: CoherencyOverhead {
                traffic_overhead: 0.15,
                latency_overhead: 5,
                power_overhead: 0.1,
                bandwidth_overhead: 0.2,
            },
        })
    }

    fn detect_bandwidth_characteristics(levels: &[CacheTopologyLevel]) -> WasmtimeResult<Vec<CacheBandwidthInfo>> {
        let mut characteristics = Vec::new();

        for level_info in levels {
            characteristics.push(CacheBandwidthInfo {
                cache_level: level_info.level,
                peak_read_bandwidth: level_info.performance_characteristics.bandwidth_characteristics.peak_bandwidth,
                peak_write_bandwidth: level_info.performance_characteristics.bandwidth_characteristics.peak_bandwidth * 0.9,
                sustained_bandwidth: level_info.performance_characteristics.bandwidth_characteristics.sustained_bandwidth,
                bandwidth_efficiency: level_info.performance_characteristics.bandwidth_characteristics.bandwidth_efficiency,
                contention_model: ContentionModel {
                    max_parallel_requests: 8,
                    contention_factor: 0.8,
                    serialization_penalty: 0.2,
                },
            });
        }

        Ok(characteristics)
    }

    fn detect_latency_matrix(levels: &[CacheTopologyLevel]) -> WasmtimeResult<Vec<Vec<u64>>> {
        let level_count = levels.len();
        let mut matrix = vec![vec![0u64; level_count]; level_count];

        for (i, level_i) in levels.iter().enumerate() {
            for (j, level_j) in levels.iter().enumerate() {
                if i == j {
                    matrix[i][j] = level_i.performance_characteristics.access_latency.mean as u64;
                } else if i < j {
                    // Higher level to lower level (miss penalty)
                    matrix[i][j] = level_j.performance_characteristics.access_latency.mean as u64 * 2;
                } else {
                    // Lower level to higher level (impossible, but set high penalty)
                    matrix[i][j] = 1000;
                }
            }
        }

        Ok(matrix)
    }

    /// Create a new cache partition
    pub fn create_partition(
        &mut self,
        name: String,
        cache_level: u32,
        cache_instance_id: u32,
        cpu_cores: Vec<u32>,
        partition_type: PartitionType,
        priority: u32,
    ) -> WasmtimeResult<u32> {
        // Validate the partition request
        self.validate_partition_request(cache_level, cache_instance_id, &cpu_cores)?;

        let partition_id = self.partitions.len() as u32;

        // Determine allocation based on partition type and available resources
        let allocated_ways = self.allocate_cache_ways(cache_level, cache_instance_id, &cpu_cores, partition_type)?;
        let allocated_size = self.calculate_allocated_size(cache_level, cache_instance_id, &allocated_ways)?;

        let partition = CachePartition {
            partition_id,
            cache_level,
            cache_instance_id,
            name,
            allocated_ways,
            allocated_size,
            cpu_cores,
            priority,
            partition_type,
            qos_settings: QosSettings {
                min_bandwidth: 10.0,
                max_bandwidth: 100.0,
                latency_guarantee: 50,
                priority_level: priority,
                preemption_policy: PreemptionPolicy::CooperativePreemptive,
            },
            performance_guarantees: PerformanceGuarantees {
                min_hit_rate: 0.8,
                max_miss_penalty: 100,
                bandwidth_guarantee: 50.0,
                latency_bound: 100,
            },
            isolation_level: match partition_type {
                PartitionType::Static => IsolationLevel::Hard,
                PartitionType::Dynamic => IsolationLevel::Soft,
                _ => IsolationLevel::Soft,
            },
        };

        // Apply the partition to the cache
        self.apply_partition(&partition)?;

        self.partitions.push(partition);
        Ok(partition_id)
    }

    fn validate_partition_request(
        &self,
        cache_level: u32,
        cache_instance_id: u32,
        cpu_cores: &[u32],
    ) -> WasmtimeResult<()> {
        // Find the cache level
        let level_info = self.cache_topology.levels.iter()
            .find(|l| l.level == cache_level)
            .ok_or_else(|| WasmtimeError::EngineConfig {
                message: format!("Cache level {} not found", cache_level),
            })?;

        // Find the cache instance
        let instance = level_info.instances.iter()
            .find(|i| i.instance_id == cache_instance_id)
            .ok_or_else(|| WasmtimeError::EngineConfig {
                message: format!("Cache instance {} not found", cache_instance_id),
            })?;

        // Check if partitioning is supported
        if instance.partitioning_support == PartitioningSupportLevel::None {
            return Err(WasmtimeError::EngineConfig {
                message: format!("Partitioning not supported for cache level {}", cache_level),
            });
        }

        // Validate CPU cores
        for &cpu_core in cpu_cores {
            if !instance.associated_cores.contains(&cpu_core) {
                return Err(WasmtimeError::EngineConfig {
                    message: format!("CPU core {} not associated with cache instance {}", cpu_core, cache_instance_id),
                });
            }
        }

        Ok(())
    }

    fn allocate_cache_ways(
        &self,
        cache_level: u32,
        cache_instance_id: u32,
        cpu_cores: &[u32],
        partition_type: PartitionType,
    ) -> WasmtimeResult<Vec<u32>> {
        let level_info = self.cache_topology.levels.iter()
            .find(|l| l.level == cache_level)
            .unwrap();
        let instance = level_info.instances.iter()
            .find(|i| i.instance_id == cache_instance_id)
            .unwrap();

        // Calculate how many ways to allocate based on CPU core count and partition type
        let total_ways = instance.associativity;
        let requested_ways = match partition_type {
            PartitionType::Static => {
                // Allocate proportional to number of CPU cores
                std::cmp::max(1, (total_ways * cpu_cores.len() as u32) / instance.associated_cores.len() as u32)
            },
            PartitionType::Dynamic => {
                // Start with minimum allocation
                std::cmp::max(1, total_ways / 4)
            },
            _ => std::cmp::max(1, total_ways / 8),
        };

        // Find available ways (not already allocated)
        let mut allocated_ways = HashSet::new();
        for partition in &self.partitions {
            if partition.cache_level == cache_level && partition.cache_instance_id == cache_instance_id {
                allocated_ways.extend(&partition.allocated_ways);
            }
        }

        let mut available_ways = Vec::new();
        for way in 0..total_ways {
            if !allocated_ways.contains(&way) {
                available_ways.push(way);
            }
        }

        // Allocate the requested number of ways from available ways
        let allocation_count = std::cmp::min(requested_ways, available_ways.len() as u32);
        Ok(available_ways.into_iter().take(allocation_count as usize).collect())
    }

    fn calculate_allocated_size(
        &self,
        cache_level: u32,
        cache_instance_id: u32,
        allocated_ways: &[u32],
    ) -> WasmtimeResult<u32> {
        let level_info = self.cache_topology.levels.iter()
            .find(|l| l.level == cache_level)
            .unwrap();
        let instance = level_info.instances.iter()
            .find(|i| i.instance_id == cache_instance_id)
            .unwrap();

        let way_size = instance.size / instance.associativity;
        Ok(way_size * allocated_ways.len() as u32)
    }

    fn apply_partition(&self, partition: &CachePartition) -> WasmtimeResult<()> {
        // This would apply the actual partition using system-specific mechanisms
        // For Intel CAT (Cache Allocation Technology):
        if partition.cache_level == 3 {
            // Create cache bit mask (CBM) for the allocated ways
            let cbm = self.create_cache_bit_mask(&partition.allocated_ways)?;

            // Apply the CBM to the CPU cores
            for &cpu_core in &partition.cpu_cores {
                if let Err(e) = self.apply_cache_bit_mask(cpu_core, cbm) {
                    eprintln!("Warning: Failed to apply cache bit mask to CPU {}: {:?}", cpu_core, e);
                }
            }
        }

        Ok(())
    }

    fn create_cache_bit_mask(&self, allocated_ways: &[u32]) -> WasmtimeResult<u32> {
        let mut mask = 0u32;
        for &way in allocated_ways {
            mask |= 1 << way;
        }
        Ok(mask)
    }

    fn apply_cache_bit_mask(&self, cpu_core: u32, mask: u32) -> WasmtimeResult<()> {
        // This would use the resctrl filesystem or direct MSR writes
        // For now, we'll just simulate the operation
        let resctrl_path = format!("/sys/fs/resctrl/partition_{}/cpus", cpu_core);

        // In a real implementation, this would:
        // 1. Create or update the resctrl group
        // 2. Set the cache bit mask
        // 3. Assign the CPU core to the group

        // For now, just validate that resctrl is available
        if std::path::Path::new("/sys/fs/resctrl").exists() {
            // Simulation: cache partitioning applied successfully
            Ok(())
        } else {
            Err(WasmtimeError::EngineConfig {
                message: "Cache partitioning not supported (resctrl not available)".to_string(),
            })
        }
    }

    /// Create cache affinity policy
    pub fn create_affinity_policy(
        &mut self,
        name: String,
        target_cores: Vec<u32>,
        cache_levels: Vec<u32>,
        strategy: AffinityStrategy,
    ) -> WasmtimeResult<u32> {
        let policy_id = self.affinity_policies.len() as u32;

        let placement_hints = self.generate_placement_hints(&target_cores, &cache_levels, strategy)?;
        let migration_policies = self.create_migration_policies(strategy)?;
        let prefetching_config = self.create_prefetching_config(strategy)?;

        let policy = CacheAffinityPolicy {
            policy_id,
            name,
            target_cores,
            cache_levels,
            strategy,
            placement_hints,
            migration_policies,
            prefetching_config,
        };

        self.apply_affinity_policy(&policy)?;
        self.affinity_policies.push(policy);

        Ok(policy_id)
    }

    fn generate_placement_hints(
        &self,
        target_cores: &[u32],
        cache_levels: &[u32],
        strategy: AffinityStrategy,
    ) -> WasmtimeResult<Vec<DataPlacementHint>> {
        let mut hints = Vec::new();

        for &level in cache_levels {
            let hint_type = match strategy {
                AffinityStrategy::LocalityAware => HintType::Prefetch,
                AffinityStrategy::PerformanceOptimized => HintType::Pin,
                AffinityStrategy::LatencyOptimized => HintType::Pin,
                _ => HintType::Prefetch,
            };

            hints.push(DataPlacementHint {
                hint_type,
                target_level: level,
                address_range: (0, u64::MAX), // Full address space
                access_pattern: AccessPattern::Sequential,
                lifetime_hint: match strategy {
                    AffinityStrategy::PowerEfficient => LifetimeHint::ShortTerm,
                    AffinityStrategy::PerformanceOptimized => LifetimeHint::LongTerm,
                    _ => LifetimeHint::MediumTerm,
                },
            });
        }

        Ok(hints)
    }

    fn create_migration_policies(&self, strategy: AffinityStrategy) -> WasmtimeResult<MigrationPolicy> {
        let triggers = match strategy {
            AffinityStrategy::LoadBalanced => vec![
                MigrationTrigger {
                    trigger_type: TriggerType::UtilizationBased,
                    threshold: 0.8,
                    evaluation_window: Duration::from_millis(100),
                    hysteresis: 0.1,
                },
            ],
            AffinityStrategy::LatencyOptimized => vec![
                MigrationTrigger {
                    trigger_type: TriggerType::LatencyBased,
                    threshold: 100.0, // 100ns threshold
                    evaluation_window: Duration::from_millis(10),
                    hysteresis: 0.05,
                },
            ],
            _ => vec![],
        };

        Ok(MigrationPolicy {
            triggers,
            cost_model: MigrationCostModel {
                base_cost: 10.0,
                distance_factor: 1.5,
                size_factor: 0.001,
                contention_factor: 2.0,
            },
            frequency_limit: 10.0, // Max 10 migrations per second
            batch_size: 64,
        })
    }

    fn create_prefetching_config(&self, strategy: AffinityStrategy) -> WasmtimeResult<PrefetchingConfiguration> {
        let algorithms = match strategy {
            AffinityStrategy::PerformanceOptimized => vec![
                PrefetchAlgorithm::NextLine,
                PrefetchAlgorithm::Stride,
                PrefetchAlgorithm::StreamBuffer,
            ],
            AffinityStrategy::BandwidthOptimized => vec![
                PrefetchAlgorithm::StreamBuffer,
                PrefetchAlgorithm::CorrelationBased,
            ],
            AffinityStrategy::PowerEfficient => vec![
                PrefetchAlgorithm::NextLine,
            ],
            _ => vec![PrefetchAlgorithm::NextLine],
        };

        Ok(PrefetchingConfiguration {
            algorithms,
            distance: match strategy {
                AffinityStrategy::PerformanceOptimized => 16,
                AffinityStrategy::BandwidthOptimized => 8,
                _ => 4,
            },
            degree: match strategy {
                AffinityStrategy::PerformanceOptimized => 4,
                _ => 2,
            },
            confidence_threshold: 0.7,
            filtering_enabled: strategy != AffinityStrategy::PerformanceOptimized,
        })
    }

    fn apply_affinity_policy(&self, policy: &CacheAffinityPolicy) -> WasmtimeResult<()> {
        // Apply CPU affinity for the target cores
        for &cpu_core in &policy.target_cores {
            self.apply_cpu_affinity(cpu_core, policy)?;
        }

        // Configure prefetching
        self.configure_prefetching(&policy.prefetching_config)?;

        Ok(())
    }

    fn apply_cpu_affinity(&self, cpu_core: u32, policy: &CacheAffinityPolicy) -> WasmtimeResult<()> {
        // This would use system calls to set CPU affinity
        // For now, simulate the operation
        match policy.strategy {
            AffinityStrategy::LocalityAware => {
                // Prefer local cache hierarchy
            },
            AffinityStrategy::LoadBalanced => {
                // Allow migration across cores with shared caches
            },
            AffinityStrategy::PerformanceOptimized => {
                // Pin to specific cores for consistency
            },
            _ => {}
        }

        Ok(())
    }

    fn configure_prefetching(&self, config: &PrefetchingConfiguration) -> WasmtimeResult<()> {
        // Configure hardware prefetchers if possible
        for algorithm in &config.algorithms {
            match algorithm {
                PrefetchAlgorithm::NextLine => {
                    // Enable next-line prefetching
                },
                PrefetchAlgorithm::Stride => {
                    // Configure stride prefetcher
                },
                PrefetchAlgorithm::StreamBuffer => {
                    // Configure stream buffer prefetcher
                },
                _ => {}
            }
        }

        Ok(())
    }

    /// Start performance monitoring
    pub fn start_performance_monitoring(&mut self) -> WasmtimeResult<()> {
        // Initialize performance monitoring for all cache instances
        let mut performance_data = self.performance_data.write().unwrap();

        for level_info in &self.cache_topology.levels {
            for instance in &level_info.instances {
                performance_data.hit_rates.insert(instance.instance_id, 0.0);
                performance_data.miss_rates.insert(instance.instance_id, 0.0);
                performance_data.utilization_rates.insert(instance.instance_id, 0.0);
                performance_data.bandwidth_utilization.insert(instance.instance_id, 0.0);
                performance_data.power_consumption.insert(instance.instance_id, 0.0);

                // Initialize performance counters
                performance_data.performance_counters.insert(
                    instance.instance_id,
                    PerformanceCounters {
                        hits: 0,
                        misses: 0,
                        evictions: 0,
                        writebacks: 0,
                        coherency_events: 0,
                        prefetch_events: 0,
                    }
                );
            }
        }

        Ok(())
    }

    /// Generate cache management report
    pub fn generate_report(&self) -> String {
        let mut report = String::new();

        report.push_str("=== Cache Partitioning Management Report ===\n\n");

        // Cache topology summary
        report.push_str("Cache Topology:\n");
        for level_info in &self.cache_topology.levels {
            report.push_str(&format!("  L{} Cache: {} instances\n", level_info.level, level_info.instances.len()));
            for instance in &level_info.instances {
                report.push_str(&format!("    Instance {}: {}KB, {}-way, {} cores\n",
                    instance.instance_id,
                    instance.size / 1024,
                    instance.associativity,
                    instance.associated_cores.len()));
            }
        }
        report.push_str("\n");

        // Active partitions
        report.push_str("Active Partitions:\n");
        for partition in &self.partitions {
            report.push_str(&format!("  Partition '{}' (ID: {})\n", partition.name, partition.partition_id));
            report.push_str(&format!("    Cache: L{} Instance {}\n", partition.cache_level, partition.cache_instance_id));
            report.push_str(&format!("    Size: {}KB ({} ways)\n", partition.allocated_size / 1024, partition.allocated_ways.len()));
            report.push_str(&format!("    CPUs: {:?}\n", partition.cpu_cores));
            report.push_str(&format!("    Priority: {}\n", partition.priority));
        }
        report.push_str("\n");

        // Affinity policies
        report.push_str("Affinity Policies:\n");
        for policy in &self.affinity_policies {
            report.push_str(&format!("  Policy '{}' (ID: {})\n", policy.name, policy.policy_id));
            report.push_str(&format!("    Strategy: {:?}\n", policy.strategy));
            report.push_str(&format!("    Target CPUs: {:?}\n", policy.target_cores));
            report.push_str(&format!("    Cache Levels: {:?}\n", policy.cache_levels));
        }

        report
    }
}

// Additional supporting structures
#[derive(Debug, Clone)]
struct CacheInstanceInfo {
    size: u32,
    associativity: u32,
    line_size: u32,
    sets: u32,
    cache_type: CacheType,
    physical_id: u32,
    inclusion_policy: CacheInclusionPolicy,
}

#[derive(Debug, Clone)]
struct ContentionCharacteristics {
    max_contention: f64,
    contention_threshold: f64,
    resolution_time: Duration,
}

#[derive(Debug, Clone)]
struct BandwidthCharacteristics {
    peak_bandwidth: f64,
    sustained_bandwidth: f64,
    bandwidth_efficiency: f64,
}

#[derive(Debug, Clone)]
struct PowerCharacteristics {
    static_power: f64,
    dynamic_power_per_access: f64,
    leakage_power: f64,
}

#[derive(Debug, Clone)]
struct ReliabilityCharacteristics {
    error_rate: f64,
    mtbf: Duration,
    correction_capability: ErrorCorrectionCapability,
    detection_capability: ErrorDetectionCapability,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ErrorCorrectionCapability {
    None,
    Single,
    Double,
    Adaptive,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ErrorDetectionCapability {
    None,
    Single,
    Double,
    Triple,
}

#[derive(Debug, Clone)]
struct SnoopingConfiguration {
    snoop_filter_enabled: bool,
    broadcast_snooping: bool,
    directory_based: bool,
}

#[derive(Debug, Clone)]
struct CacheBandwidthInfo {
    cache_level: u32,
    peak_read_bandwidth: f64,
    peak_write_bandwidth: f64,
    sustained_bandwidth: f64,
    bandwidth_efficiency: f64,
    contention_model: ContentionModel,
}

#[derive(Debug, Clone)]
struct ContentionModel {
    max_parallel_requests: u32,
    contention_factor: f64,
    serialization_penalty: f64,
}

impl CachePerformanceData {
    fn new() -> Self {
        Self {
            hit_rates: HashMap::new(),
            miss_rates: HashMap::new(),
            utilization_rates: HashMap::new(),
            bandwidth_utilization: HashMap::new(),
            latency_distributions: HashMap::new(),
            interference_metrics: HashMap::new(),
            power_consumption: HashMap::new(),
            thermal_characteristics: HashMap::new(),
            performance_counters: HashMap::new(),
            time_series: VecDeque::new(),
        }
    }
}

impl CacheInterferenceDetector {
    fn new(topology: &CacheTopology) -> WasmtimeResult<Self> {
        Ok(Self {
            detection_algorithms: vec![
                InterferenceDetectionAlgorithm::StatisticalAnalysis,
                InterferenceDetectionAlgorithm::PerformanceCounters,
            ],
            interference_patterns: Vec::new(),
            detection_thresholds: InterferenceThresholds {
                hit_rate_degradation: 0.1,
                latency_increase: 0.2,
                bandwidth_reduction: 0.15,
            },
            mitigation_strategies: vec![
                InterferenceMitigationStrategy::Partitioning,
                InterferenceMitigationStrategy::Throttling,
            ],
            realtime_monitoring: RealtimeMonitoringConfig {
                enabled: true,
                sampling_frequency: Duration::from_millis(10),
                alert_threshold: 0.2,
            },
        })
    }
}

impl CacheQosManager {
    fn new() -> WasmtimeResult<Self> {
        Ok(Self {
            qos_policies: Vec::new(),
            resource_reservations: Vec::new(),
            service_agreements: Vec::new(),
            enforcement_mechanisms: Vec::new(),
            monitoring_config: QosMonitoringConfig {
                enabled: true,
                reporting_frequency: Duration::from_secs(1),
                violation_alert_threshold: 0.05,
            },
        })
    }
}

// Supporting enums and structures for the additional types
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum InterferenceDetectionAlgorithm {
    StatisticalAnalysis,
    PerformanceCounters,
    MachineLearningBased,
}

#[derive(Debug, Clone)]
struct InterferencePattern {
    pattern_id: u32,
    source_cores: Vec<u32>,
    target_caches: Vec<u32>,
    interference_type: InterferenceType,
    severity: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum InterferenceType {
    CapacityInterference,
    ConflictInterference,
    CoherencyInterference,
    BandwidthInterference,
}

#[derive(Debug, Clone)]
struct InterferenceThresholds {
    hit_rate_degradation: f64,
    latency_increase: f64,
    bandwidth_reduction: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum InterferenceMitigationStrategy {
    Partitioning,
    Throttling,
    Scheduling,
    Prefetching,
}

#[derive(Debug, Clone)]
struct RealtimeMonitoringConfig {
    enabled: bool,
    sampling_frequency: Duration,
    alert_threshold: f64,
}

#[derive(Debug, Clone)]
struct CacheQosPolicy {
    policy_id: u32,
    name: String,
    target_applications: Vec<String>,
    resource_guarantees: Vec<ResourceGuarantee>,
}

#[derive(Debug, Clone)]
struct ResourceReservation {
    reservation_id: u32,
    cache_level: u32,
    reserved_ways: Vec<u32>,
    reserved_bandwidth: f64,
}

#[derive(Debug, Clone)]
struct ServiceLevelAgreement {
    sla_id: u32,
    application_id: String,
    performance_targets: Vec<PerformanceTarget>,
    penalty_clauses: Vec<PenaltyClause>,
}

#[derive(Debug, Clone)]
struct EnforcementMechanism {
    mechanism_id: u32,
    mechanism_type: EnforcementType,
    enforcement_policy: EnforcementPolicy,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum EnforcementType {
    HardLimit,
    SoftLimit,
    AdaptiveLimit,
}

#[derive(Debug, Clone)]
struct EnforcementPolicy {
    violation_response: ViolationResponse,
    grace_period: Duration,
    escalation_policy: EscalationPolicy,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ViolationResponse {
    Warning,
    Throttling,
    Suspension,
    Termination,
}

#[derive(Debug, Clone)]
struct EscalationPolicy {
    escalation_levels: Vec<EscalationLevel>,
    escalation_delay: Duration,
}

#[derive(Debug, Clone)]
struct EscalationLevel {
    level: u32,
    response: ViolationResponse,
    threshold: f64,
}

#[derive(Debug, Clone)]
struct QosMonitoringConfig {
    enabled: bool,
    reporting_frequency: Duration,
    violation_alert_threshold: f64,
}

#[derive(Debug, Clone)]
struct ResourceGuarantee {
    resource_type: ResourceType,
    minimum_allocation: f64,
    maximum_allocation: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ResourceType {
    CacheCapacity,
    CacheBandwidth,
    MemoryBandwidth,
    ComputeTime,
}

#[derive(Debug, Clone)]
struct PerformanceTarget {
    metric: PerformanceMetric,
    target_value: f64,
    tolerance: f64,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum PerformanceMetric {
    Latency,
    Throughput,
    HitRate,
    Bandwidth,
}

#[derive(Debug, Clone)]
struct PenaltyClause {
    violation_type: ViolationType,
    penalty_amount: f64,
    penalty_type: PenaltyType,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum ViolationType {
    LatencyViolation,
    ThroughputViolation,
    AvailabilityViolation,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum PenaltyType {
    Fixed,
    Proportional,
    Progressive,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum WarmingAlgorithm {
    SequentialWarmup,
    PriorityBasedWarmup,
    PredictiveWarmup,
    AdaptiveWarmup,
}

#[derive(Debug, Clone)]
struct DataPattern {
    pattern_id: u32,
    access_sequence: Vec<u64>,
    frequency: f64,
    importance: f64,
}

#[derive(Debug, Clone)]
struct WarmingTimingConfig {
    warmup_duration: Duration,
    warmup_intensity: f64,
    cooldown_period: Duration,
}

#[derive(Debug, Clone)]
struct WarmingEffectivenessMetrics {
    hit_rate_improvement: f64,
    latency_reduction: f64,
    warmup_overhead: f64,
}

/// Core functions for cache partitioning management
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::validate_ptr_not_null;

    /// Create cache partitioning manager
    pub fn create_cache_manager() -> WasmtimeResult<Box<CachePartitioningManager>> {
        let manager = CachePartitioningManager::new()?;
        Ok(Box::new(manager))
    }

    /// Create cache partition
    pub unsafe fn create_partition(
        manager_ptr: *mut c_void,
        name_ptr: *const std::os::raw::c_char,
        cache_level: u32,
        cache_instance_id: u32,
        cpu_cores_ptr: *const u32,
        cpu_cores_len: u32,
        partition_type: u32,
        priority: u32,
    ) -> WasmtimeResult<u32> {
        validate_ptr_not_null!(manager_ptr, "cache_manager");
        validate_ptr_not_null!(name_ptr, "partition_name");
        validate_ptr_not_null!(cpu_cores_ptr, "cpu_cores");

        let manager = &mut *(manager_ptr as *mut CachePartitioningManager);
        let name_cstr = CStr::from_ptr(name_ptr);
        let name = name_cstr.to_str().map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Invalid partition name: {}", e),
        })?.to_string();

        let cpu_cores = std::slice::from_raw_parts(cpu_cores_ptr, cpu_cores_len as usize).to_vec();

        let partition_type = match partition_type {
            0 => PartitionType::Static,
            1 => PartitionType::Dynamic,
            2 => PartitionType::Adaptive,
            3 => PartitionType::Hierarchical,
            4 => PartitionType::Temporal,
            _ => return Err(WasmtimeError::EngineConfig {
                message: "Invalid partition type".to_string(),
            }),
        };

        manager.create_partition(name, cache_level, cache_instance_id, cpu_cores, partition_type, priority)
    }

    /// Create cache affinity policy
    pub unsafe fn create_affinity_policy(
        manager_ptr: *mut c_void,
        name_ptr: *const std::os::raw::c_char,
        target_cores_ptr: *const u32,
        target_cores_len: u32,
        cache_levels_ptr: *const u32,
        cache_levels_len: u32,
        strategy: u32,
    ) -> WasmtimeResult<u32> {
        validate_ptr_not_null!(manager_ptr, "cache_manager");
        validate_ptr_not_null!(name_ptr, "policy_name");
        validate_ptr_not_null!(target_cores_ptr, "target_cores");
        validate_ptr_not_null!(cache_levels_ptr, "cache_levels");

        let manager = &mut *(manager_ptr as *mut CachePartitioningManager);
        let name_cstr = CStr::from_ptr(name_ptr);
        let name = name_cstr.to_str().map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Invalid policy name: {}", e),
        })?.to_string();

        let target_cores = std::slice::from_raw_parts(target_cores_ptr, target_cores_len as usize).to_vec();
        let cache_levels = std::slice::from_raw_parts(cache_levels_ptr, cache_levels_len as usize).to_vec();

        let strategy = match strategy {
            0 => AffinityStrategy::LocalityAware,
            1 => AffinityStrategy::LoadBalanced,
            2 => AffinityStrategy::PerformanceOptimized,
            3 => AffinityStrategy::PowerEfficient,
            4 => AffinityStrategy::LatencyOptimized,
            5 => AffinityStrategy::BandwidthOptimized,
            _ => return Err(WasmtimeError::EngineConfig {
                message: "Invalid affinity strategy".to_string(),
            }),
        };

        manager.create_affinity_policy(name, target_cores, cache_levels, strategy)
    }

    /// Start performance monitoring
    pub unsafe fn start_monitoring(manager_ptr: *mut c_void) -> WasmtimeResult<()> {
        validate_ptr_not_null!(manager_ptr, "cache_manager");
        let manager = &mut *(manager_ptr as *mut CachePartitioningManager);
        manager.start_performance_monitoring()
    }

    /// Generate management report
    pub unsafe fn generate_report(manager_ptr: *const c_void) -> WasmtimeResult<String> {
        validate_ptr_not_null!(manager_ptr, "cache_manager");
        let manager = &*(manager_ptr as *const CachePartitioningManager);
        Ok(manager.generate_report())
    }

    /// Destroy cache manager
    pub unsafe fn destroy_cache_manager(manager_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<CachePartitioningManager>(manager_ptr, "CachePartitioningManager");
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_cache_manager_creation() {
        let manager = CachePartitioningManager::new();
        assert!(manager.is_ok());

        let manager = manager.unwrap();
        assert!(!manager.cache_topology.levels.is_empty());
    }

    #[test]
    fn test_cache_size_parsing() {
        assert_eq!(CachePartitioningManager::parse_cache_size("32K").unwrap(), 32 * 1024);
        assert_eq!(CachePartitioningManager::parse_cache_size("1M").unwrap(), 1024 * 1024);
        assert_eq!(CachePartitioningManager::parse_cache_size("1024").unwrap(), 1024);
    }

    #[test]
    fn test_cpu_list_parsing() {
        let cpus = CachePartitioningManager::parse_cpu_list("0-3,8-11").unwrap();
        assert_eq!(cpus, vec![0, 1, 2, 3, 8, 9, 10, 11]);
    }

    #[test]
    fn test_partition_creation() {
        let mut manager = CachePartitioningManager::new().unwrap();

        // Find a cache level that supports partitioning
        let mut suitable_level = None;
        let mut suitable_instance = None;

        for level_info in &manager.cache_topology.levels {
            for instance in &level_info.instances {
                if instance.partitioning_support != PartitioningSupportLevel::None {
                    suitable_level = Some(level_info.level);
                    suitable_instance = Some(instance.instance_id);
                    break;
                }
            }
            if suitable_level.is_some() {
                break;
            }
        }

        if let (Some(level), Some(instance_id)) = (suitable_level, suitable_instance) {
            let result = manager.create_partition(
                "test_partition".to_string(),
                level,
                instance_id,
                vec![0],
                PartitionType::Static,
                1,
            );
            assert!(result.is_ok());

            let partition_id = result.unwrap();
            assert!(partition_id < manager.partitions.len() as u32);
        }
    }

    #[test]
    fn test_affinity_policy_creation() {
        let mut manager = CachePartitioningManager::new().unwrap();

        let result = manager.create_affinity_policy(
            "test_policy".to_string(),
            vec![0, 1],
            vec![1, 2, 3],
            AffinityStrategy::PerformanceOptimized,
        );
        assert!(result.is_ok());

        let policy_id = result.unwrap();
        assert!(policy_id < manager.affinity_policies.len() as u32);
    }

    #[test]
    fn test_performance_monitoring() {
        let mut manager = CachePartitioningManager::new().unwrap();
        let result = manager.start_performance_monitoring();
        assert!(result.is_ok());

        let performance_data = manager.performance_data.read().unwrap();
        assert!(!performance_data.hit_rates.is_empty());
    }

    #[test]
    fn test_report_generation() {
        let manager = CachePartitioningManager::new().unwrap();
        let report = manager.generate_report();

        assert!(report.contains("Cache Partitioning Management Report"));
        assert!(report.contains("Cache Topology:"));
    }

    #[test]
    fn test_cache_bit_mask_creation() {
        let manager = CachePartitioningManager::new().unwrap();
        let allocated_ways = vec![0, 2, 4, 6];
        let mask = manager.create_cache_bit_mask(&allocated_ways).unwrap();

        // Check that the correct bits are set
        assert_eq!(mask, 0b01010101); // bits 0, 2, 4, 6 set
    }
}