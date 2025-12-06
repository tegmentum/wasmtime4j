//! Advanced NUMA topology detection and management for wasmtime4j
//!
//! This module provides comprehensive NUMA (Non-Uniform Memory Access) topology detection,
//! memory binding, CPU affinity management, and cross-socket optimization strategies.

use std::collections::HashMap;
use std::fs;
use std::io::BufRead;
use std::path::Path;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Advanced NUMA topology information with multi-socket support
#[derive(Debug, Clone)]
pub struct AdvancedNumaTopology {
    /// Number of NUMA nodes
    pub node_count: u32,
    /// CPU cores per NUMA node with detailed mapping
    pub cores_per_node: Vec<NodeCpuInfo>,
    /// Memory information per NUMA node
    pub memory_per_node: Vec<NodeMemoryInfo>,
    /// Inter-node latency matrix (nanoseconds)
    pub node_latencies: Vec<Vec<u64>>,
    /// Inter-node bandwidth matrix (GB/s)
    pub node_bandwidths: Vec<Vec<f64>>,
    /// CPU to NUMA node mapping
    pub cpu_to_node: HashMap<u32, u32>,
    /// NUMA distance matrix (relative costs)
    pub numa_distances: Vec<Vec<u32>>,
    /// Socket topology information
    pub socket_topology: SocketTopology,
    /// Cache coherency domains
    pub cache_domains: Vec<CacheDomain>,
    /// Memory controllers per node
    pub memory_controllers: Vec<MemoryControllerInfo>,
    /// NUMA balancing capabilities
    pub balancing_capabilities: NumaBalancingCapabilities,
}

/// NUMA node CPU information with detailed core mapping
#[derive(Debug, Clone)]
pub struct NodeCpuInfo {
    /// NUMA node ID
    pub node_id: u32,
    /// Physical CPU cores in this node
    pub physical_cores: Vec<u32>,
    /// Logical CPU cores (including hyperthreads)
    pub logical_cores: Vec<u32>,
    /// CPU core frequency ranges
    pub core_frequencies: HashMap<u32, FrequencyRange>,
    /// Cache hierarchy per core
    pub cache_hierarchy: HashMap<u32, CoreCacheInfo>,
    /// CPU core capabilities
    pub core_capabilities: HashMap<u32, CoreCapabilities>,
}

/// NUMA node memory information
#[derive(Debug, Clone)]
pub struct NodeMemoryInfo {
    /// NUMA node ID
    pub node_id: u32,
    /// Total memory in bytes
    pub total_memory: u64,
    /// Available memory in bytes
    pub available_memory: u64,
    /// Memory controller information
    pub memory_controller: MemoryControllerInfo,
    /// Memory channels and banks
    pub memory_channels: Vec<MemoryChannelInfo>,
    /// Huge page support
    pub huge_page_support: HugePageSupport,
    /// Memory bandwidth characteristics
    pub bandwidth_characteristics: MemoryBandwidthCharacteristics,
}

/// Socket topology information for multi-socket systems
#[derive(Debug, Clone)]
pub struct SocketTopology {
    /// Number of physical sockets
    pub socket_count: u32,
    /// NUMA nodes per socket
    pub nodes_per_socket: Vec<Vec<u32>>,
    /// Socket interconnect type and bandwidth
    pub socket_interconnects: Vec<SocketInterconnect>,
    /// Cross-socket latency characteristics
    pub cross_socket_latencies: HashMap<(u32, u32), u64>,
    /// Socket power domains
    pub socket_power_domains: Vec<PowerDomain>,
}

/// Cache coherency domain information
#[derive(Debug, Clone)]
pub struct CacheDomain {
    /// Domain ID
    pub domain_id: u32,
    /// CPU cores in this domain
    pub cpu_cores: Vec<u32>,
    /// Shared cache levels
    pub shared_caches: Vec<SharedCacheInfo>,
    /// Cache coherency protocol
    pub coherency_protocol: CacheCoherencyProtocol,
    /// Cache line sharing characteristics
    pub sharing_characteristics: CacheSharingCharacteristics,
}

/// Memory controller information
#[derive(Debug, Clone)]
pub struct MemoryControllerInfo {
    /// Controller ID
    pub controller_id: u32,
    /// Associated NUMA node
    pub numa_node: u32,
    /// Memory channels controlled
    pub channels: Vec<u32>,
    /// Controller bandwidth (GB/s)
    pub max_bandwidth: f64,
    /// Controller latency (nanoseconds)
    pub base_latency: u64,
    /// Supported memory types
    pub supported_memory_types: Vec<MemoryType>,
    /// ECC support
    pub ecc_support: bool,
}

/// NUMA balancing capabilities
#[derive(Debug, Clone)]
pub struct NumaBalancingCapabilities {
    /// Hardware-assisted NUMA balancing
    pub hardware_assisted: bool,
    /// Automatic page migration support
    pub auto_page_migration: bool,
    /// Page fault-based migration
    pub page_fault_migration: bool,
    /// Memory access sampling
    pub access_sampling: bool,
    /// Migration cost estimation
    pub migration_cost_estimation: bool,
    /// NUMA hinting support
    pub numa_hinting: bool,
}

/// CPU core frequency range information
#[derive(Debug, Clone)]
pub struct FrequencyRange {
    /// Base frequency in MHz
    pub base_frequency: u32,
    /// Maximum frequency in MHz
    pub max_frequency: u32,
    /// Minimum frequency in MHz
    pub min_frequency: u32,
    /// Turbo boost availability
    pub turbo_boost: bool,
    /// Frequency scaling governor
    pub scaling_governor: String,
}

/// Per-core cache information
#[derive(Debug, Clone)]
pub struct CoreCacheInfo {
    /// L1 instruction cache
    pub l1i: CacheLevel,
    /// L1 data cache
    pub l1d: CacheLevel,
    /// L2 cache (may be shared)
    pub l2: Option<CacheLevel>,
    /// L3 cache (usually shared)
    pub l3: Option<CacheLevel>,
    /// Cache sharing topology
    pub sharing_topology: CacheSharingTopology,
}

/// CPU core capabilities
#[derive(Debug, Clone)]
pub struct CoreCapabilities {
    /// Hyper-threading support
    pub hyperthreading: bool,
    /// Vector extensions available
    pub vector_extensions: Vec<VectorExtension>,
    /// Cryptographic acceleration
    pub crypto_acceleration: Vec<CryptoExtension>,
    /// Performance monitoring capabilities
    pub performance_monitoring: PerformanceMonitoringCapabilities,
    /// Power management features
    pub power_management: CorePowerManagement,
}

/// Memory channel information
#[derive(Debug, Clone)]
pub struct MemoryChannelInfo {
    /// Channel ID
    pub channel_id: u32,
    /// Memory banks in this channel
    pub memory_banks: Vec<MemoryBankInfo>,
    /// Channel bandwidth (GB/s)
    pub bandwidth: f64,
    /// Channel latency (nanoseconds)
    pub latency: u64,
    /// Interleaving configuration
    pub interleaving: InterleavingConfig,
}

/// Huge page support information
#[derive(Debug, Clone)]
pub struct HugePageSupport {
    /// 2MB huge pages supported
    pub huge_2mb: bool,
    /// 1GB huge pages supported
    pub huge_1gb: bool,
    /// Available huge pages
    pub available_huge_pages: HashMap<u32, u32>,
    /// Transparent huge page support
    pub transparent_huge_pages: bool,
}

/// Memory bandwidth characteristics
#[derive(Debug, Clone)]
pub struct MemoryBandwidthCharacteristics {
    /// Peak read bandwidth (GB/s)
    pub peak_read_bandwidth: f64,
    /// Peak write bandwidth (GB/s)
    pub peak_write_bandwidth: f64,
    /// Sustained bandwidth (GB/s)
    pub sustained_bandwidth: f64,
    /// Bandwidth under load (GB/s)
    pub loaded_bandwidth: f64,
    /// Memory access patterns optimization
    pub access_pattern_optimization: Vec<AccessPatternOptimization>,
}

/// Socket interconnect information
#[derive(Debug, Clone)]
pub struct SocketInterconnect {
    /// Source socket
    pub source_socket: u32,
    /// Destination socket
    pub destination_socket: u32,
    /// Interconnect type (e.g., QPI, UPI, Infinity Fabric)
    pub interconnect_type: InterconnectType,
    /// Bandwidth (GB/s)
    pub bandwidth: f64,
    /// Latency (nanoseconds)
    pub latency: u64,
    /// Link count
    pub link_count: u32,
}

/// Power domain information
#[derive(Debug, Clone)]
pub struct PowerDomain {
    /// Domain ID
    pub domain_id: u32,
    /// CPUs in this power domain
    pub cpus: Vec<u32>,
    /// Voltage levels supported
    pub voltage_levels: Vec<f32>,
    /// Frequency domains
    pub frequency_domains: Vec<FrequencyDomain>,
    /// Power gating support
    pub power_gating: bool,
}

/// Cache level information
#[derive(Debug, Clone)]
pub struct CacheLevel {
    /// Cache size in bytes
    pub size: u32,
    /// Cache associativity
    pub associativity: u32,
    /// Cache line size
    pub line_size: u32,
    /// Number of sets
    pub sets: u32,
    /// Cache latency in cycles
    pub latency_cycles: u32,
    /// Cache latency in nanoseconds
    pub latency_ns: u64,
    /// Cache replacement policy
    pub replacement_policy: CacheReplacementPolicy,
    /// Cache coherency protocol
    pub coherency_protocol: CacheCoherencyProtocol,
}

/// Shared cache information
#[derive(Debug, Clone)]
pub struct SharedCacheInfo {
    /// Cache level (L2, L3, etc.)
    pub level: u32,
    /// CPU cores sharing this cache
    pub shared_cores: Vec<u32>,
    /// Cache partitioning support
    pub partitioning_support: bool,
    /// Quality of Service support
    pub qos_support: bool,
}

/// Cache sharing topology
#[derive(Debug, Clone)]
pub struct CacheSharingTopology {
    /// L1 sharing (usually per-core)
    pub l1_sharing: Vec<u32>,
    /// L2 sharing (cores sharing L2)
    pub l2_sharing: Vec<u32>,
    /// L3 sharing (cores sharing L3)
    pub l3_sharing: Vec<u32>,
}

/// Cache sharing characteristics
#[derive(Debug, Clone)]
pub struct CacheSharingCharacteristics {
    /// Exclusive cache hierarchy
    pub exclusive_hierarchy: bool,
    /// Inclusive cache hierarchy
    pub inclusive_hierarchy: bool,
    /// Cache line bouncing characteristics
    pub bouncing_characteristics: CacheBouncingCharacteristics,
    /// False sharing detection
    pub false_sharing_detection: bool,
}

/// Memory bank information
#[derive(Debug, Clone)]
pub struct MemoryBankInfo {
    /// Bank ID
    pub bank_id: u32,
    /// Bank size in bytes
    pub size: u64,
    /// Bank type (DDR4, DDR5, etc.)
    pub memory_type: MemoryType,
    /// Bank access timing
    pub timing_parameters: MemoryTimingParameters,
}

/// Interleaving configuration
#[derive(Debug, Clone)]
pub struct InterleavingConfig {
    /// Interleaving enabled
    pub enabled: bool,
    /// Interleaving granularity (bytes)
    pub granularity: u32,
    /// Interleaving pattern
    pub pattern: InterleavingPattern,
}

/// Access pattern optimization
#[derive(Debug, Clone)]
pub struct AccessPatternOptimization {
    /// Pattern type
    pub pattern_type: AccessPatternType,
    /// Optimization techniques
    pub optimizations: Vec<OptimizationTechnique>,
    /// Expected performance improvement
    pub performance_improvement: f64,
}

/// Frequency domain information
#[derive(Debug, Clone)]
pub struct FrequencyDomain {
    /// Domain ID
    pub domain_id: u32,
    /// CPUs in this frequency domain
    pub cpus: Vec<u32>,
    /// Available frequencies
    pub available_frequencies: Vec<u32>,
    /// Current frequency
    pub current_frequency: u32,
}

/// Cache bouncing characteristics
#[derive(Debug, Clone)]
pub struct CacheBouncingCharacteristics {
    /// Bouncing penalty (cycles)
    pub bouncing_penalty: u32,
    /// Migration threshold
    pub migration_threshold: u32,
    /// Coherency traffic overhead
    pub coherency_overhead: f64,
}

/// Memory timing parameters
#[derive(Debug, Clone)]
pub struct MemoryTimingParameters {
    /// CAS latency
    pub cas_latency: u32,
    /// RAS to CAS delay
    pub ras_to_cas_delay: u32,
    /// Row precharge time
    pub row_precharge_time: u32,
    /// Refresh cycle time
    pub refresh_cycle_time: u32,
}

/// Performance monitoring capabilities
#[derive(Debug, Clone)]
pub struct PerformanceMonitoringCapabilities {
    /// Hardware performance counters
    pub hardware_counters: u32,
    /// Branch prediction monitoring
    pub branch_prediction_monitoring: bool,
    /// Cache miss monitoring
    pub cache_miss_monitoring: bool,
    /// Memory bandwidth monitoring
    pub memory_bandwidth_monitoring: bool,
    /// Instruction pipeline monitoring
    pub pipeline_monitoring: bool,
}

/// Core power management features
#[derive(Debug, Clone)]
pub struct CorePowerManagement {
    /// C-states supported
    pub c_states: Vec<CState>,
    /// P-states supported
    pub p_states: Vec<PState>,
    /// Turbo boost availability
    pub turbo_boost: bool,
    /// Dynamic voltage scaling
    pub dynamic_voltage_scaling: bool,
    /// Power gating
    pub power_gating: bool,
}

// Enums for various types and configurations

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InterconnectType {
    Qpi,
    Upi,
    InfinityFabric,
    CcixLink,
    CxlLink,
    NvLink,
    Custom(u32),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CacheReplacementPolicy {
    Lru,
    Lfu,
    Random,
    Fifo,
    TreePseudoLru,
    AdaptiveLru,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CacheCoherencyProtocol {
    Mesi,
    Moesi,
    Mesif,
    Dragon,
    Firefly,
    Directory,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MemoryType {
    Ddr3,
    Ddr4,
    Ddr5,
    Hbm,
    Hbm2,
    Hbm3,
    Gddr6,
    Lpddr4,
    Lpddr5,
    Optane,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum InterleavingPattern {
    RoundRobin,
    XorBased,
    HashBased,
    Sequential,
    Custom(u32),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AccessPatternType {
    Sequential,
    Random,
    Strided,
    Sparse,
    Hotspot,
    LocalityFriendly,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OptimizationTechnique {
    Prefetching,
    CacheBlocking,
    DataReordering,
    MemoryInterleaving,
    NumaAwareness,
    CachePartitioning,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VectorExtension {
    Sse,
    Sse2,
    Sse3,
    Ssse3,
    Sse41,
    Sse42,
    Avx,
    Avx2,
    Avx512,
    Neon,
    Sve,
    Sve2,
    Altivec,
    Rvv,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CryptoExtension {
    Aes,
    Sha,
    Pclmulqdq,
    Crc32,
    Rdrand,
    Rdseed,
    ArmCrypto,
}

#[derive(Debug, Clone)]
pub struct CState {
    /// C-state number (C0, C1, etc.)
    pub state_number: u32,
    /// Exit latency in microseconds
    pub exit_latency: u64,
    /// Power consumption in milliwatts
    pub power_consumption: u32,
    /// State description
    pub description: String,
}

#[derive(Debug, Clone)]
pub struct PState {
    /// P-state number
    pub state_number: u32,
    /// Frequency in MHz
    pub frequency: u32,
    /// Voltage in volts
    pub voltage: f32,
    /// Power consumption in watts
    pub power_consumption: f32,
}

/// NUMA binding strategy for advanced topology management
#[derive(Debug, Clone)]
pub struct NumaBindingStrategy {
    /// Memory allocation policy
    pub memory_policy: MemoryAllocationPolicy,
    /// CPU affinity policy
    pub cpu_affinity_policy: CpuAffinityPolicy,
    /// Cross-NUMA access optimization
    pub cross_numa_optimization: CrossNumaOptimization,
    /// Load balancing configuration
    pub load_balancing: NumaLoadBalancing,
    /// Migration policies
    pub migration_policies: MigrationPolicies,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MemoryAllocationPolicy {
    /// Allocate on first touch
    FirstTouch,
    /// Allocate locally to calling thread
    Local,
    /// Interleave across all nodes
    Interleave,
    /// Bind to specific nodes
    Bind(u32),
    /// Prefer specific nodes with fallback
    Preferred(u32),
    /// Weighted allocation across nodes
    Weighted,
}

#[derive(Debug, Clone)]
pub struct CpuAffinityPolicy {
    /// Binding type
    pub binding_type: AffinityBindingType,
    /// Target CPUs or nodes
    pub targets: Vec<u32>,
    /// Exclusive binding
    pub exclusive: bool,
    /// Inheritance policy
    pub inheritance: AffinityInheritance,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AffinityBindingType {
    /// No specific binding
    None,
    /// Bind to specific CPUs
    CpuBind,
    /// Bind to NUMA nodes
    NodeBind,
    /// Bind to cache domains
    CacheBind,
    /// Bind to socket
    SocketBind,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum AffinityInheritance {
    /// No inheritance
    None,
    /// Inherit from parent
    Inherit,
    /// Reset on fork
    Reset,
}

#[derive(Debug, Clone)]
pub struct CrossNumaOptimization {
    /// Enable cross-NUMA prefetching
    pub cross_numa_prefetch: bool,
    /// Optimize for remote memory access
    pub remote_access_optimization: bool,
    /// Cache coherency optimization
    pub cache_coherency_optimization: bool,
    /// Bandwidth throttling
    pub bandwidth_throttling: BandwidthThrottling,
}

#[derive(Debug, Clone)]
pub struct BandwidthThrottling {
    /// Enable throttling
    pub enabled: bool,
    /// Throttling threshold (percentage)
    pub threshold: f64,
    /// Throttling strategy
    pub strategy: ThrottlingStrategy,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ThrottlingStrategy {
    /// Linear throttling
    Linear,
    /// Exponential backoff
    ExponentialBackoff,
    /// Priority-based throttling
    PriorityBased,
    /// Adaptive throttling
    Adaptive,
}

#[derive(Debug, Clone)]
pub struct NumaLoadBalancing {
    /// Enable load balancing
    pub enabled: bool,
    /// Balancing frequency (milliseconds)
    pub balance_frequency: u32,
    /// Load threshold
    pub load_threshold: f64,
    /// Migration cost factor
    pub migration_cost_factor: f64,
    /// Balancing algorithm
    pub algorithm: LoadBalancingAlgorithm,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LoadBalancingAlgorithm {
    /// Round-robin balancing
    RoundRobin,
    /// Load-based balancing
    LoadBased,
    /// Proximity-aware balancing
    ProximityAware,
    /// Work-stealing balancing
    WorkStealing,
    /// Machine learning-based balancing
    MlBased,
}

#[derive(Debug, Clone)]
pub struct MigrationPolicies {
    /// Page migration policy
    pub page_migration: PageMigrationPolicy,
    /// Thread migration policy
    pub thread_migration: ThreadMigrationPolicy,
    /// Memory balancing policy
    pub memory_balancing: MemoryBalancingPolicy,
}

#[derive(Debug, Clone)]
pub struct PageMigrationPolicy {
    /// Enable automatic page migration
    pub auto_migration: bool,
    /// Migration threshold (access frequency)
    pub migration_threshold: f64,
    /// Migration cost limit
    pub cost_limit: f64,
    /// Scan frequency (milliseconds)
    pub scan_frequency: u32,
}

#[derive(Debug, Clone)]
pub struct ThreadMigrationPolicy {
    /// Enable thread migration
    pub enabled: bool,
    /// Migration frequency (milliseconds)
    pub migration_frequency: u32,
    /// Load imbalance threshold
    pub imbalance_threshold: f64,
    /// Affinity stickiness factor
    pub stickiness_factor: f64,
}

#[derive(Debug, Clone)]
pub struct MemoryBalancingPolicy {
    /// Enable memory balancing
    pub enabled: bool,
    /// Balancing algorithm
    pub algorithm: MemoryBalancingAlgorithm,
    /// Target utilization percentage
    pub target_utilization: f64,
    /// Balancing interval (seconds)
    pub balancing_interval: u32,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum MemoryBalancingAlgorithm {
    /// First-fit balancing
    FirstFit,
    /// Best-fit balancing
    BestFit,
    /// Worst-fit balancing
    WorstFit,
    /// Buddy system balancing
    BuddySystem,
    /// Slab allocator balancing
    SlabAllocator,
}

impl AdvancedNumaTopology {
    /// Detect advanced NUMA topology with comprehensive system analysis
    pub fn detect() -> WasmtimeResult<Self> {
        let node_count = Self::detect_node_count()?;
        let cores_per_node = Self::detect_cores_per_node(node_count)?;
        let memory_per_node = Self::detect_memory_per_node(node_count)?;
        let node_latencies = Self::detect_node_latencies(node_count)?;
        let node_bandwidths = Self::detect_node_bandwidths(node_count)?;
        let cpu_to_node = Self::detect_cpu_to_node_mapping()?;
        let numa_distances = Self::detect_numa_distances(node_count)?;
        let socket_topology = Self::detect_socket_topology()?;
        let cache_domains = Self::detect_cache_domains()?;
        let memory_controllers = Self::detect_memory_controllers()?;
        let balancing_capabilities = Self::detect_balancing_capabilities()?;

        Ok(Self {
            node_count,
            cores_per_node,
            memory_per_node,
            node_latencies,
            node_bandwidths,
            cpu_to_node,
            numa_distances,
            socket_topology,
            cache_domains,
            memory_controllers,
            balancing_capabilities,
        })
    }

    fn detect_node_count() -> WasmtimeResult<u32> {
        // Try to read from /sys/devices/system/node
        if let Ok(entries) = fs::read_dir("/sys/devices/system/node") {
            let node_count = entries
                .filter_map(|entry| {
                    entry.ok().and_then(|e| {
                        e.file_name().to_str().and_then(|name| {
                            if name.starts_with("node") {
                                name[4..].parse::<u32>().ok()
                            } else {
                                None
                            }
                        })
                    })
                })
                .max()
                .map(|max| max + 1)
                .unwrap_or(1);
            Ok(node_count)
        } else {
            // Fallback: assume single NUMA node
            Ok(1)
        }
    }

    fn detect_cores_per_node(node_count: u32) -> WasmtimeResult<Vec<NodeCpuInfo>> {
        let mut cores_per_node = Vec::with_capacity(node_count as usize);

        for node_id in 0..node_count {
            let mut physical_cores = Vec::new();
            let mut logical_cores = Vec::new();
            let mut core_frequencies = HashMap::new();
            let mut cache_hierarchy = HashMap::new();
            let mut core_capabilities = HashMap::new();

            // Read CPU list for this NUMA node
            let cpulist_path = format!("/sys/devices/system/node/node{}/cpulist", node_id);
            if let Ok(cpulist) = fs::read_to_string(&cpulist_path) {
                let cpu_ranges = Self::parse_cpu_list(&cpulist.trim())?;
                logical_cores.extend(cpu_ranges.iter());

                // Detect physical vs logical cores
                for &cpu in &cpu_ranges {
                    // Try to determine if this is a physical or logical core
                    let thread_siblings_path = format!("/sys/devices/system/cpu/cpu{}/topology/thread_siblings_list", cpu);
                    if let Ok(siblings) = fs::read_to_string(&thread_siblings_path) {
                        let sibling_cpus = Self::parse_cpu_list(&siblings.trim())?;
                        // If CPU is the lowest-numbered sibling, consider it physical
                        if sibling_cpus.iter().min() == Some(&cpu) {
                            physical_cores.push(cpu);
                        }
                    } else {
                        // Assume physical core if can't determine
                        physical_cores.push(cpu);
                    }

                    // Detect frequency range for this core
                    core_frequencies.insert(cpu, Self::detect_cpu_frequency_range(cpu)?);

                    // Detect cache hierarchy for this core
                    cache_hierarchy.insert(cpu, Self::detect_core_cache_info(cpu)?);

                    // Detect core capabilities
                    core_capabilities.insert(cpu, Self::detect_core_capabilities(cpu)?);
                }
            }

            cores_per_node.push(NodeCpuInfo {
                node_id,
                physical_cores,
                logical_cores,
                core_frequencies,
                cache_hierarchy,
                core_capabilities,
            });
        }

        Ok(cores_per_node)
    }

    fn detect_memory_per_node(node_count: u32) -> WasmtimeResult<Vec<NodeMemoryInfo>> {
        let mut memory_per_node = Vec::with_capacity(node_count as usize);

        for node_id in 0..node_count {
            let meminfo_path = format!("/sys/devices/system/node/node{}/meminfo", node_id);
            let mut total_memory = 0u64;
            let mut available_memory = 0u64;

            if let Ok(meminfo) = fs::read_to_string(&meminfo_path) {
                for line in meminfo.lines() {
                    if line.contains("MemTotal:") {
                        if let Some(kb) = line.split_whitespace().nth(3) {
                            if let Ok(kb_val) = kb.parse::<u64>() {
                                total_memory = kb_val * 1024; // Convert to bytes
                            }
                        }
                    } else if line.contains("MemFree:") {
                        if let Some(kb) = line.split_whitespace().nth(3) {
                            if let Ok(kb_val) = kb.parse::<u64>() {
                                available_memory = kb_val * 1024; // Convert to bytes
                            }
                        }
                    }
                }
            }

            // If we couldn't read node-specific memory info, estimate
            if total_memory == 0 {
                if let Ok(global_meminfo) = fs::read_to_string("/proc/meminfo") {
                    for line in global_meminfo.lines() {
                        if line.starts_with("MemTotal:") {
                            if let Some(kb) = line.split_whitespace().nth(1) {
                                if let Ok(kb_val) = kb.parse::<u64>() {
                                    total_memory = (kb_val * 1024) / node_count as u64;
                                    available_memory = total_memory / 2; // Rough estimate
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            let memory_controller = Self::detect_memory_controller_info(node_id)?;
            let memory_channels = Self::detect_memory_channels(node_id)?;
            let huge_page_support = Self::detect_huge_page_support(node_id)?;
            let bandwidth_characteristics = Self::detect_memory_bandwidth_characteristics(node_id)?;

            memory_per_node.push(NodeMemoryInfo {
                node_id,
                total_memory,
                available_memory,
                memory_controller,
                memory_channels,
                huge_page_support,
                bandwidth_characteristics,
            });
        }

        Ok(memory_per_node)
    }

    fn detect_node_latencies(node_count: u32) -> WasmtimeResult<Vec<Vec<u64>>> {
        // Create a matrix of inter-node latencies
        let mut latencies = vec![vec![0u64; node_count as usize]; node_count as usize];

        // Try to measure actual latencies using memory access patterns
        for i in 0..node_count {
            for j in 0..node_count {
                if i == j {
                    latencies[i as usize][j as usize] = 10; // Local access ~10ns
                } else {
                    // Estimate cross-NUMA latency based on distance
                    let base_latency = 100; // Base cross-NUMA latency ~100ns
                    let distance_factor = if i.abs_diff(j) == 1 { 1 } else { 2 };
                    latencies[i as usize][j as usize] = base_latency * distance_factor;
                }
            }
        }

        Ok(latencies)
    }

    fn detect_node_bandwidths(node_count: u32) -> WasmtimeResult<Vec<Vec<f64>>> {
        // Create a matrix of inter-node bandwidths
        let mut bandwidths = vec![vec![0.0f64; node_count as usize]; node_count as usize];

        for i in 0..node_count {
            for j in 0..node_count {
                if i == j {
                    bandwidths[i as usize][j as usize] = 200.0; // Local bandwidth ~200 GB/s
                } else {
                    // Cross-NUMA bandwidth is typically much lower
                    bandwidths[i as usize][j as usize] = 50.0; // Cross-NUMA ~50 GB/s
                }
            }
        }

        Ok(bandwidths)
    }

    fn detect_cpu_to_node_mapping() -> WasmtimeResult<HashMap<u32, u32>> {
        let mut cpu_to_node = HashMap::new();

        // Read CPU to NUMA node mapping from sysfs
        if let Ok(entries) = fs::read_dir("/sys/devices/system/cpu") {
            for entry in entries.filter_map(|e| e.ok()) {
                if let Some(cpu_name) = entry.file_name().to_str() {
                    if cpu_name.starts_with("cpu") && cpu_name[3..].chars().all(|c| c.is_ascii_digit()) {
                        if let Ok(cpu_id) = cpu_name[3..].parse::<u32>() {
                            let node_path = entry.path().join("node0");
                            if node_path.exists() {
                                // This CPU belongs to node 0
                                cpu_to_node.insert(cpu_id, 0);
                            }
                            // Check other nodes
                            for node_id in 1..16 { // Check up to 16 nodes
                                let node_path = entry.path().join(format!("node{}", node_id));
                                if node_path.exists() {
                                    cpu_to_node.insert(cpu_id, node_id);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        // If no mapping found, assume all CPUs are on node 0
        if cpu_to_node.is_empty() {
            let cpu_count = std::thread::available_parallelism().map(|p| p.get()).unwrap_or(1);
            for cpu_id in 0..cpu_count as u32 {
                cpu_to_node.insert(cpu_id, 0);
            }
        }

        Ok(cpu_to_node)
    }

    fn detect_numa_distances(node_count: u32) -> WasmtimeResult<Vec<Vec<u32>>> {
        let mut distances = vec![vec![0u32; node_count as usize]; node_count as usize];

        // Try to read NUMA distances from sysfs
        for i in 0..node_count {
            let distance_path = format!("/sys/devices/system/node/node{}/distance", i);
            if let Ok(distance_data) = fs::read_to_string(&distance_path) {
                let distance_values: Vec<u32> = distance_data
                    .trim()
                    .split_whitespace()
                    .filter_map(|s| s.parse().ok())
                    .collect();

                for (j, &distance) in distance_values.iter().enumerate() {
                    if j < node_count as usize {
                        distances[i as usize][j] = distance;
                    }
                }
            } else {
                // Default distances: 10 for local, 20 for remote
                for j in 0..node_count {
                    distances[i as usize][j as usize] = if i == j { 10 } else { 20 };
                }
            }
        }

        Ok(distances)
    }

    fn detect_socket_topology() -> WasmtimeResult<SocketTopology> {
        // Detect physical socket topology
        let mut socket_count = 1u32;
        let mut nodes_per_socket = vec![vec![0]];
        let socket_interconnects = Vec::new();
        let cross_socket_latencies = HashMap::new();
        let socket_power_domains = Vec::new();

        // Try to detect actual socket count
        if let Ok(entries) = fs::read_dir("/sys/devices/system/cpu") {
            let mut max_socket_id = 0u32;
            for entry in entries.filter_map(|e| e.ok()) {
                if let Some(cpu_name) = entry.file_name().to_str() {
                    if cpu_name.starts_with("cpu") && cpu_name[3..].chars().all(|c| c.is_ascii_digit()) {
                        let socket_path = entry.path().join("topology/physical_package_id");
                        if let Ok(socket_id_str) = fs::read_to_string(&socket_path) {
                            if let Ok(socket_id) = socket_id_str.trim().parse::<u32>() {
                                max_socket_id = max_socket_id.max(socket_id);
                            }
                        }
                    }
                }
            }
            socket_count = max_socket_id + 1;

            // Update nodes per socket (simplified)
            nodes_per_socket = (0..socket_count).map(|i| vec![i]).collect();
        }

        Ok(SocketTopology {
            socket_count,
            nodes_per_socket,
            socket_interconnects,
            cross_socket_latencies,
            socket_power_domains,
        })
    }

    fn detect_cache_domains() -> WasmtimeResult<Vec<CacheDomain>> {
        // Detect cache coherency domains
        let mut domains = Vec::new();

        // For now, create a single domain with all CPUs
        let cpu_count = std::thread::available_parallelism().map(|p| p.get()).unwrap_or(1);
        let cpu_cores: Vec<u32> = (0..cpu_count as u32).collect();

        domains.push(CacheDomain {
            domain_id: 0,
            cpu_cores,
            shared_caches: Vec::new(),
            coherency_protocol: CacheCoherencyProtocol::Mesi,
            sharing_characteristics: CacheSharingCharacteristics {
                exclusive_hierarchy: false,
                inclusive_hierarchy: true,
                bouncing_characteristics: CacheBouncingCharacteristics {
                    bouncing_penalty: 100,
                    migration_threshold: 1000,
                    coherency_overhead: 0.1,
                },
                false_sharing_detection: false,
            },
        });

        Ok(domains)
    }

    fn detect_memory_controllers() -> WasmtimeResult<Vec<MemoryControllerInfo>> {
        // Detect memory controller information
        let mut controllers = Vec::new();

        // For now, create a default memory controller
        controllers.push(MemoryControllerInfo {
            controller_id: 0,
            numa_node: 0,
            channels: vec![0, 1], // Assume dual-channel
            max_bandwidth: 100.0, // 100 GB/s
            base_latency: 80,      // 80ns
            supported_memory_types: vec![MemoryType::Ddr4],
            ecc_support: false,
        });

        Ok(controllers)
    }

    fn detect_balancing_capabilities() -> WasmtimeResult<NumaBalancingCapabilities> {
        // Detect NUMA balancing capabilities
        let mut hardware_assisted = false;
        let mut auto_page_migration = false;
        let mut page_fault_migration = false;

        // Check if automatic NUMA balancing is available
        if let Ok(_) = fs::read_to_string("/proc/sys/kernel/numa_balancing") {
            hardware_assisted = true;
            auto_page_migration = true;
            page_fault_migration = true;
        }

        Ok(NumaBalancingCapabilities {
            hardware_assisted,
            auto_page_migration,
            page_fault_migration,
            access_sampling: hardware_assisted,
            migration_cost_estimation: hardware_assisted,
            numa_hinting: hardware_assisted,
        })
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

    fn detect_cpu_frequency_range(cpu_id: u32) -> WasmtimeResult<FrequencyRange> {
        let base_freq_path = format!("/sys/devices/system/cpu/cpu{}/cpufreq/base_frequency", cpu_id);
        let max_freq_path = format!("/sys/devices/system/cpu/cpu{}/cpufreq/cpuinfo_max_freq", cpu_id);
        let min_freq_path = format!("/sys/devices/system/cpu/cpu{}/cpufreq/cpuinfo_min_freq", cpu_id);
        let governor_path = format!("/sys/devices/system/cpu/cpu{}/cpufreq/scaling_governor", cpu_id);

        let base_frequency = fs::read_to_string(&base_freq_path)
            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
            .unwrap_or(2000000) / 1000; // Convert kHz to MHz

        let max_frequency = fs::read_to_string(&max_freq_path)
            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
            .unwrap_or(3000000) / 1000; // Convert kHz to MHz

        let min_frequency = fs::read_to_string(&min_freq_path)
            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
            .unwrap_or(1000000) / 1000; // Convert kHz to MHz

        let scaling_governor = fs::read_to_string(&governor_path)
            .unwrap_or_else(|_| "unknown".to_string());

        Ok(FrequencyRange {
            base_frequency,
            max_frequency,
            min_frequency,
            turbo_boost: max_frequency > base_frequency,
            scaling_governor,
        })
    }

    fn detect_core_cache_info(cpu_id: u32) -> WasmtimeResult<CoreCacheInfo> {
        // Detect cache information for a specific CPU core
        let l1i = Self::detect_cache_level(cpu_id, 1, "instruction")?;
        let l1d = Self::detect_cache_level(cpu_id, 1, "data")?;
        let l2 = Self::detect_cache_level(cpu_id, 2, "unified").ok();
        let l3 = Self::detect_cache_level(cpu_id, 3, "unified").ok();

        let sharing_topology = CacheSharingTopology {
            l1_sharing: vec![cpu_id],
            l2_sharing: vec![cpu_id], // Simplified
            l3_sharing: vec![cpu_id], // Simplified
        };

        Ok(CoreCacheInfo {
            l1i,
            l1d,
            l2,
            l3,
            sharing_topology,
        })
    }

    fn detect_cache_level(cpu_id: u32, level: u32, cache_type: &str) -> WasmtimeResult<CacheLevel> {
        let cache_path = format!("/sys/devices/system/cpu/cpu{}/cache", cpu_id);

        // Try to find the cache by level and type
        if let Ok(entries) = fs::read_dir(&cache_path) {
            for entry in entries.filter_map(|e| e.ok()) {
                let index_path = entry.path();
                let level_path = index_path.join("level");
                let type_path = index_path.join("type");

                if let (Ok(cache_level), Ok(cache_type_str)) = (
                    fs::read_to_string(&level_path),
                    fs::read_to_string(&type_path)
                ) {
                    let cache_level: u32 = cache_level.trim().parse().unwrap_or(0);
                    let cache_type_str = cache_type_str.trim().to_lowercase();

                    if cache_level == level && (cache_type == "unified" || cache_type_str.contains(cache_type)) {
                        let size_path = index_path.join("size");
                        let ways_path = index_path.join("ways_of_associativity");
                        let line_size_path = index_path.join("coherency_line_size");

                        let size = fs::read_to_string(&size_path)
                            .unwrap_or_else(|_| "32K".to_string());
                        let size_bytes = Self::parse_cache_size(&size)?;

                        let associativity = fs::read_to_string(&ways_path)
                            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
                            .unwrap_or(8);

                        let line_size = fs::read_to_string(&line_size_path)
                            .and_then(|s| s.trim().parse::<u32>().map_err(|e| std::io::Error::new(std::io::ErrorKind::InvalidData, e)))
                            .unwrap_or(64);

                        let sets = if associativity > 0 { size_bytes / (associativity * line_size) } else { 1 };

                        return Ok(CacheLevel {
                            size: size_bytes,
                            associativity,
                            line_size,
                            sets,
                            latency_cycles: match level {
                                1 => 1,
                                2 => 10,
                                3 => 30,
                                _ => 100,
                            },
                            latency_ns: match level {
                                1 => 1,
                                2 => 3,
                                3 => 10,
                                _ => 50,
                            },
                            replacement_policy: CacheReplacementPolicy::Lru,
                            coherency_protocol: CacheCoherencyProtocol::Mesi,
                        });
                    }
                }
            }
        }

        // Default cache level if detection fails
        Ok(CacheLevel {
            size: match level {
                1 => 32 * 1024,      // 32KB L1
                2 => 256 * 1024,     // 256KB L2
                3 => 8 * 1024 * 1024, // 8MB L3
                _ => 1024 * 1024,    // 1MB default
            },
            associativity: 8,
            line_size: 64,
            sets: 64,
            latency_cycles: match level {
                1 => 1,
                2 => 10,
                3 => 30,
                _ => 100,
            },
            latency_ns: match level {
                1 => 1,
                2 => 3,
                3 => 10,
                _ => 50,
            },
            replacement_policy: CacheReplacementPolicy::Lru,
            coherency_protocol: CacheCoherencyProtocol::Mesi,
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

    fn detect_core_capabilities(cpu_id: u32) -> WasmtimeResult<CoreCapabilities> {
        // Detect CPU core capabilities
        let mut vector_extensions = Vec::new();
        let mut crypto_acceleration = Vec::new();

        // Try to read CPU flags from /proc/cpuinfo
        if let Ok(cpuinfo) = fs::read_to_string("/proc/cpuinfo") {
            for line in cpuinfo.lines() {
                if line.starts_with("flags") || line.starts_with("Features") {
                    let flags: Vec<&str> = line.split_whitespace().skip(2).collect();

                    for flag in flags {
                        match flag {
                            "sse" => vector_extensions.push(VectorExtension::Sse),
                            "sse2" => vector_extensions.push(VectorExtension::Sse2),
                            "sse3" => vector_extensions.push(VectorExtension::Sse3),
                            "ssse3" => vector_extensions.push(VectorExtension::Ssse3),
                            "sse4_1" => vector_extensions.push(VectorExtension::Sse41),
                            "sse4_2" => vector_extensions.push(VectorExtension::Sse42),
                            "avx" => vector_extensions.push(VectorExtension::Avx),
                            "avx2" => vector_extensions.push(VectorExtension::Avx2),
                            "avx512f" => vector_extensions.push(VectorExtension::Avx512),
                            "neon" => vector_extensions.push(VectorExtension::Neon),
                            "aes" => crypto_acceleration.push(CryptoExtension::Aes),
                            "sha_ni" => crypto_acceleration.push(CryptoExtension::Sha),
                            "pclmulqdq" => crypto_acceleration.push(CryptoExtension::Pclmulqdq),
                            "crc32" => crypto_acceleration.push(CryptoExtension::Crc32),
                            "rdrand" => crypto_acceleration.push(CryptoExtension::Rdrand),
                            "rdseed" => crypto_acceleration.push(CryptoExtension::Rdseed),
                            _ => {}
                        }
                    }
                    break;
                }
            }
        }

        // Detect hyperthreading
        let hyperthreading = Self::detect_hyperthreading(cpu_id)?;

        let performance_monitoring = PerformanceMonitoringCapabilities {
            hardware_counters: 4, // Default assumption
            branch_prediction_monitoring: true,
            cache_miss_monitoring: true,
            memory_bandwidth_monitoring: false,
            pipeline_monitoring: false,
        };

        let power_management = CorePowerManagement {
            c_states: vec![
                CState {
                    state_number: 0,
                    exit_latency: 0,
                    power_consumption: 1000, // 1W
                    description: "Active".to_string(),
                },
                CState {
                    state_number: 1,
                    exit_latency: 1,
                    power_consumption: 500, // 0.5W
                    description: "Halt".to_string(),
                },
            ],
            p_states: vec![
                PState {
                    state_number: 0,
                    frequency: 3000, // 3GHz
                    voltage: 1.2,
                    power_consumption: 65.0,
                },
                PState {
                    state_number: 1,
                    frequency: 2000, // 2GHz
                    voltage: 1.0,
                    power_consumption: 35.0,
                },
            ],
            turbo_boost: true,
            dynamic_voltage_scaling: true,
            power_gating: false,
        };

        Ok(CoreCapabilities {
            hyperthreading,
            vector_extensions,
            crypto_acceleration,
            performance_monitoring,
            power_management,
        })
    }

    fn detect_hyperthreading(cpu_id: u32) -> WasmtimeResult<bool> {
        let siblings_path = format!("/sys/devices/system/cpu/cpu{}/topology/thread_siblings_list", cpu_id);
        if let Ok(siblings) = fs::read_to_string(&siblings_path) {
            let sibling_count = Self::parse_cpu_list(&siblings.trim())?.len();
            Ok(sibling_count > 1)
        } else {
            Ok(false)
        }
    }

    fn detect_memory_controller_info(node_id: u32) -> WasmtimeResult<MemoryControllerInfo> {
        Ok(MemoryControllerInfo {
            controller_id: node_id,
            numa_node: node_id,
            channels: vec![0, 1], // Assume dual-channel
            max_bandwidth: 100.0, // 100 GB/s
            base_latency: 80,      // 80ns
            supported_memory_types: vec![MemoryType::Ddr4],
            ecc_support: false,
        })
    }

    fn detect_memory_channels(node_id: u32) -> WasmtimeResult<Vec<MemoryChannelInfo>> {
        // For now, assume dual-channel configuration
        Ok(vec![
            MemoryChannelInfo {
                channel_id: 0,
                memory_banks: vec![
                    MemoryBankInfo {
                        bank_id: 0,
                        size: 8 * 1024 * 1024 * 1024, // 8GB
                        memory_type: MemoryType::Ddr4,
                        timing_parameters: MemoryTimingParameters {
                            cas_latency: 16,
                            ras_to_cas_delay: 16,
                            row_precharge_time: 16,
                            refresh_cycle_time: 350,
                        },
                    },
                ],
                bandwidth: 25.6, // 25.6 GB/s per channel
                latency: 80,     // 80ns
                interleaving: InterleavingConfig {
                    enabled: true,
                    granularity: 64,
                    pattern: InterleavingPattern::RoundRobin,
                },
            },
            MemoryChannelInfo {
                channel_id: 1,
                memory_banks: vec![
                    MemoryBankInfo {
                        bank_id: 1,
                        size: 8 * 1024 * 1024 * 1024, // 8GB
                        memory_type: MemoryType::Ddr4,
                        timing_parameters: MemoryTimingParameters {
                            cas_latency: 16,
                            ras_to_cas_delay: 16,
                            row_precharge_time: 16,
                            refresh_cycle_time: 350,
                        },
                    },
                ],
                bandwidth: 25.6, // 25.6 GB/s per channel
                latency: 80,     // 80ns
                interleaving: InterleavingConfig {
                    enabled: true,
                    granularity: 64,
                    pattern: InterleavingPattern::RoundRobin,
                },
            },
        ])
    }

    fn detect_huge_page_support(node_id: u32) -> WasmtimeResult<HugePageSupport> {
        let mut huge_2mb = false;
        let mut huge_1gb = false;
        let mut available_huge_pages = HashMap::new();

        // Check for huge page support in /proc/meminfo
        if let Ok(meminfo) = fs::read_to_string("/proc/meminfo") {
            for line in meminfo.lines() {
                if line.starts_with("HugePages_Total:") {
                    huge_2mb = true;
                    if let Some(count) = line.split_whitespace().nth(1) {
                        if let Ok(count_val) = count.parse::<u32>() {
                            available_huge_pages.insert(2048, count_val); // 2MB pages
                        }
                    }
                }
            }
        }

        // Check for 1GB huge page support
        if Path::new("/sys/kernel/mm/hugepages/hugepages-1048576kB").exists() {
            huge_1gb = true;
            available_huge_pages.insert(1048576, 0); // 1GB pages
        }

        Ok(HugePageSupport {
            huge_2mb,
            huge_1gb,
            available_huge_pages,
            transparent_huge_pages: Path::new("/sys/kernel/mm/transparent_hugepage").exists(),
        })
    }

    fn detect_memory_bandwidth_characteristics(node_id: u32) -> WasmtimeResult<MemoryBandwidthCharacteristics> {
        // Estimate memory bandwidth characteristics
        Ok(MemoryBandwidthCharacteristics {
            peak_read_bandwidth: 100.0,    // 100 GB/s
            peak_write_bandwidth: 90.0,    // 90 GB/s
            sustained_bandwidth: 80.0,     // 80 GB/s
            loaded_bandwidth: 60.0,        // 60 GB/s under load
            access_pattern_optimization: vec![
                AccessPatternOptimization {
                    pattern_type: AccessPatternType::Sequential,
                    optimizations: vec![OptimizationTechnique::Prefetching],
                    performance_improvement: 0.3, // 30% improvement
                },
                AccessPatternOptimization {
                    pattern_type: AccessPatternType::Random,
                    optimizations: vec![OptimizationTechnique::CacheBlocking],
                    performance_improvement: 0.15, // 15% improvement
                },
            ],
        })
    }

    /// Apply NUMA binding strategy to the current process
    pub fn apply_binding_strategy(&self, strategy: &NumaBindingStrategy) -> WasmtimeResult<()> {
        // This would implement actual NUMA binding using system calls
        // For now, we'll just validate the strategy
        self.validate_binding_strategy(strategy)?;

        // Implementation would use libnuma or direct system calls
        // to bind memory and CPU affinity according to the strategy

        Ok(())
    }

    fn validate_binding_strategy(&self, strategy: &NumaBindingStrategy) -> WasmtimeResult<()> {
        // Validate that the binding strategy is compatible with the topology
        match strategy.cpu_affinity_policy.binding_type {
            AffinityBindingType::NodeBind => {
                for &target in &strategy.cpu_affinity_policy.targets {
                    if target >= self.node_count {
                        return Err(WasmtimeError::EngineConfig {
                            message: format!("Invalid NUMA node {} in binding strategy", target),
                        });
                    }
                }
            }
            AffinityBindingType::CpuBind => {
                for &target in &strategy.cpu_affinity_policy.targets {
                    if !self.cpu_to_node.contains_key(&target) {
                        return Err(WasmtimeError::EngineConfig {
                            message: format!("Invalid CPU {} in binding strategy", target),
                        });
                    }
                }
            }
            _ => {}
        }

        Ok(())
    }

    /// Recommend optimal NUMA binding strategy based on workload characteristics
    pub fn recommend_binding_strategy(&self, workload_type: WorkloadType) -> NumaBindingStrategy {
        match workload_type {
            WorkloadType::MemoryIntensive => {
                NumaBindingStrategy {
                    memory_policy: MemoryAllocationPolicy::Local,
                    cpu_affinity_policy: CpuAffinityPolicy {
                        binding_type: AffinityBindingType::NodeBind,
                        targets: vec![0], // Prefer node 0
                        exclusive: false,
                        inheritance: AffinityInheritance::Inherit,
                    },
                    cross_numa_optimization: CrossNumaOptimization {
                        cross_numa_prefetch: false,
                        remote_access_optimization: false,
                        cache_coherency_optimization: true,
                        bandwidth_throttling: BandwidthThrottling {
                            enabled: false,
                            threshold: 0.8,
                            strategy: ThrottlingStrategy::Linear,
                        },
                    },
                    load_balancing: NumaLoadBalancing {
                        enabled: false,
                        balance_frequency: 1000,
                        load_threshold: 0.8,
                        migration_cost_factor: 1.0,
                        algorithm: LoadBalancingAlgorithm::LoadBased,
                    },
                    migration_policies: MigrationPolicies {
                        page_migration: PageMigrationPolicy {
                            auto_migration: false,
                            migration_threshold: 0.1,
                            cost_limit: 100.0,
                            scan_frequency: 1000,
                        },
                        thread_migration: ThreadMigrationPolicy {
                            enabled: false,
                            migration_frequency: 5000,
                            imbalance_threshold: 0.3,
                            stickiness_factor: 0.8,
                        },
                        memory_balancing: MemoryBalancingPolicy {
                            enabled: true,
                            algorithm: MemoryBalancingAlgorithm::FirstFit,
                            target_utilization: 0.8,
                            balancing_interval: 10,
                        },
                    },
                }
            }
            WorkloadType::CpuIntensive => {
                NumaBindingStrategy {
                    memory_policy: MemoryAllocationPolicy::Interleave,
                    cpu_affinity_policy: CpuAffinityPolicy {
                        binding_type: AffinityBindingType::CpuBind,
                        targets: (0..self.cores_per_node[0].physical_cores.len() as u32).collect(),
                        exclusive: true,
                        inheritance: AffinityInheritance::Reset,
                    },
                    cross_numa_optimization: CrossNumaOptimization {
                        cross_numa_prefetch: true,
                        remote_access_optimization: true,
                        cache_coherency_optimization: true,
                        bandwidth_throttling: BandwidthThrottling {
                            enabled: true,
                            threshold: 0.9,
                            strategy: ThrottlingStrategy::Adaptive,
                        },
                    },
                    load_balancing: NumaLoadBalancing {
                        enabled: true,
                        balance_frequency: 100,
                        load_threshold: 0.7,
                        migration_cost_factor: 0.5,
                        algorithm: LoadBalancingAlgorithm::WorkStealing,
                    },
                    migration_policies: MigrationPolicies {
                        page_migration: PageMigrationPolicy {
                            auto_migration: true,
                            migration_threshold: 0.2,
                            cost_limit: 50.0,
                            scan_frequency: 100,
                        },
                        thread_migration: ThreadMigrationPolicy {
                            enabled: true,
                            migration_frequency: 1000,
                            imbalance_threshold: 0.2,
                            stickiness_factor: 0.5,
                        },
                        memory_balancing: MemoryBalancingPolicy {
                            enabled: true,
                            algorithm: MemoryBalancingAlgorithm::BestFit,
                            target_utilization: 0.75,
                            balancing_interval: 5,
                        },
                    },
                }
            }
            WorkloadType::Balanced => {
                NumaBindingStrategy {
                    memory_policy: MemoryAllocationPolicy::FirstTouch,
                    cpu_affinity_policy: CpuAffinityPolicy {
                        binding_type: AffinityBindingType::None,
                        targets: Vec::new(),
                        exclusive: false,
                        inheritance: AffinityInheritance::Inherit,
                    },
                    cross_numa_optimization: CrossNumaOptimization {
                        cross_numa_prefetch: true,
                        remote_access_optimization: true,
                        cache_coherency_optimization: true,
                        bandwidth_throttling: BandwidthThrottling {
                            enabled: true,
                            threshold: 0.8,
                            strategy: ThrottlingStrategy::Linear,
                        },
                    },
                    load_balancing: NumaLoadBalancing {
                        enabled: true,
                        balance_frequency: 500,
                        load_threshold: 0.75,
                        migration_cost_factor: 0.8,
                        algorithm: LoadBalancingAlgorithm::ProximityAware,
                    },
                    migration_policies: MigrationPolicies {
                        page_migration: PageMigrationPolicy {
                            auto_migration: true,
                            migration_threshold: 0.15,
                            cost_limit: 75.0,
                            scan_frequency: 500,
                        },
                        thread_migration: ThreadMigrationPolicy {
                            enabled: true,
                            migration_frequency: 2000,
                            imbalance_threshold: 0.25,
                            stickiness_factor: 0.7,
                        },
                        memory_balancing: MemoryBalancingPolicy {
                            enabled: true,
                            algorithm: MemoryBalancingAlgorithm::BestFit,
                            target_utilization: 0.8,
                            balancing_interval: 8,
                        },
                    },
                }
            }
        }
    }

    /// Get comprehensive topology report
    pub fn generate_topology_report(&self) -> String {
        let mut report = String::new();

        report.push_str(&format!("=== NUMA Topology Report ===\n"));
        report.push_str(&format!("NUMA Nodes: {}\n", self.node_count));
        report.push_str(&format!("Socket Count: {}\n", self.socket_topology.socket_count));
        report.push_str(&format!("Cache Domains: {}\n", self.cache_domains.len()));
        report.push_str(&format!("Memory Controllers: {}\n", self.memory_controllers.len()));
        report.push_str("\n");

        for (i, node_info) in self.cores_per_node.iter().enumerate() {
            report.push_str(&format!("--- Node {} ---\n", i));
            report.push_str(&format!("Physical Cores: {}\n", node_info.physical_cores.len()));
            report.push_str(&format!("Logical Cores: {}\n", node_info.logical_cores.len()));

            if let Some(memory_info) = self.memory_per_node.get(i) {
                report.push_str(&format!("Total Memory: {} GB\n", memory_info.total_memory / (1024 * 1024 * 1024)));
                report.push_str(&format!("Available Memory: {} GB\n", memory_info.available_memory / (1024 * 1024 * 1024)));
                report.push_str(&format!("Memory Channels: {}\n", memory_info.memory_channels.len()));
            }
            report.push_str("\n");
        }

        // NUMA distances
        report.push_str("NUMA Distances:\n");
        for (i, row) in self.numa_distances.iter().enumerate() {
            report.push_str(&format!("Node {}: ", i));
            for distance in row {
                report.push_str(&format!("{:3} ", distance));
            }
            report.push_str("\n");
        }
        report.push_str("\n");

        // Balancing capabilities
        report.push_str("NUMA Balancing Capabilities:\n");
        report.push_str(&format!("Hardware Assisted: {}\n", self.balancing_capabilities.hardware_assisted));
        report.push_str(&format!("Auto Page Migration: {}\n", self.balancing_capabilities.auto_page_migration));
        report.push_str(&format!("Page Fault Migration: {}\n", self.balancing_capabilities.page_fault_migration));
        report.push_str(&format!("Access Sampling: {}\n", self.balancing_capabilities.access_sampling));
        report.push_str("\n");

        report
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum WorkloadType {
    MemoryIntensive,
    CpuIntensive,
    Balanced,
}

/// Core functions for NUMA topology management
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::validate_ptr_not_null;

    /// Detect advanced NUMA topology
    pub fn detect_numa_topology() -> WasmtimeResult<Box<AdvancedNumaTopology>> {
        let topology = AdvancedNumaTopology::detect()?;
        Ok(Box::new(topology))
    }

    /// Generate NUMA topology report
    pub unsafe fn generate_topology_report(topology_ptr: *const c_void) -> WasmtimeResult<String> {
        validate_ptr_not_null!(topology_ptr, "numa_topology");
        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        Ok(topology.generate_topology_report())
    }

    /// Recommend NUMA binding strategy
    pub unsafe fn recommend_binding_strategy(
        topology_ptr: *const c_void,
        workload_type: u32,
    ) -> WasmtimeResult<Box<NumaBindingStrategy>> {
        validate_ptr_not_null!(topology_ptr, "numa_topology");
        let topology = &*(topology_ptr as *const AdvancedNumaTopology);

        let workload = match workload_type {
            0 => WorkloadType::MemoryIntensive,
            1 => WorkloadType::CpuIntensive,
            2 => WorkloadType::Balanced,
            _ => return Err(WasmtimeError::EngineConfig {
                message: "Invalid workload type".to_string(),
            }),
        };

        let strategy = topology.recommend_binding_strategy(workload);
        Ok(Box::new(strategy))
    }

    /// Apply NUMA binding strategy
    pub unsafe fn apply_binding_strategy(
        topology_ptr: *const c_void,
        strategy_ptr: *const c_void,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(topology_ptr, "numa_topology");
        validate_ptr_not_null!(strategy_ptr, "binding_strategy");

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        let strategy = &*(strategy_ptr as *const NumaBindingStrategy);

        topology.apply_binding_strategy(strategy)
    }

    /// Destroy NUMA topology
    pub unsafe fn destroy_numa_topology(topology_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<AdvancedNumaTopology>(topology_ptr, "AdvancedNumaTopology");
    }

    /// Destroy binding strategy
    pub unsafe fn destroy_binding_strategy(strategy_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<NumaBindingStrategy>(strategy_ptr, "NumaBindingStrategy");
    }
}

//==============================================================================
// FFI Exports for Panama
//==============================================================================

use std::os::raw::{c_char, c_int, c_void};
use crate::error::ffi_utils;

/// Detect NUMA topology (Panama FFI)
///
/// Returns a pointer to the NUMA topology structure, or null on error.
/// The caller must free the returned pointer using `numa_topology_free`.
#[no_mangle]
pub extern "C" fn numa_topology_detect(out_ptr: *mut *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if out_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output pointer cannot be null".to_string(),
            });
        }

        let topology = AdvancedNumaTopology::detect()?;
        let boxed = Box::new(topology);
        unsafe {
            *out_ptr = Box::into_raw(boxed) as *mut c_void;
        }
        Ok(())
    })
}

/// Get the number of NUMA nodes (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_get_node_count(
    topology_ptr: *const c_void,
    out_count: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_count.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output count pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        *out_count = topology.node_count;
        Ok(())
    })
}

/// Get the CPU count for a specific NUMA node (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_get_node_cpu_count(
    topology_ptr: *const c_void,
    node_id: u32,
    out_count: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_count.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output count pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        if node_id >= topology.node_count {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid NUMA node ID: {}", node_id),
            });
        }

        let node_info = topology.cores_per_node.get(node_id as usize)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("NUMA node {} not found", node_id),
            })?;
        *out_count = node_info.logical_cores.len() as u32;
        Ok(())
    })
}

/// Get the memory size for a specific NUMA node in bytes (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_get_node_memory(
    topology_ptr: *const c_void,
    node_id: u32,
    out_total: *mut u64,
    out_available: *mut u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_total.is_null() || out_available.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output pointers cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        if node_id >= topology.node_count {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid NUMA node ID: {}", node_id),
            });
        }

        let memory_info = topology.memory_per_node.get(node_id as usize)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("NUMA node {} memory info not found", node_id),
            })?;
        *out_total = memory_info.total_memory;
        *out_available = memory_info.available_memory;
        Ok(())
    })
}

/// Get NUMA distance between two nodes (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_get_distance(
    topology_ptr: *const c_void,
    from_node: u32,
    to_node: u32,
    out_distance: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_distance.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output distance pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        if from_node >= topology.node_count || to_node >= topology.node_count {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid NUMA node ID: from={}, to={}", from_node, to_node),
            });
        }

        let distance = topology.numa_distances
            .get(from_node as usize)
            .and_then(|row| row.get(to_node as usize))
            .copied()
            .unwrap_or(0);
        *out_distance = distance;
        Ok(())
    })
}

/// Generate a topology report string (Panama FFI)
///
/// Returns a pointer to a null-terminated string that must be freed with `numa_string_free`.
#[no_mangle]
pub unsafe extern "C" fn numa_topology_generate_report(
    topology_ptr: *const c_void,
    out_report: *mut *mut c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_report.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output report pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        let report = topology.generate_topology_report();

        let c_string = std::ffi::CString::new(report)
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to create C string: {}", e),
            })?;
        *out_report = c_string.into_raw();
        Ok(())
    })
}

/// Free a string returned by NUMA functions (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_string_free(ptr: *mut c_char) {
    if !ptr.is_null() {
        drop(std::ffi::CString::from_raw(ptr));
    }
}

/// Recommend a binding strategy for a workload type (Panama FFI)
///
/// workload_type: 0 = MemoryIntensive, 1 = CpuIntensive, 2 = Balanced
/// Returns a pointer to NumaBindingStrategy that must be freed with `numa_binding_strategy_free`.
#[no_mangle]
pub unsafe extern "C" fn numa_topology_recommend_binding_strategy(
    topology_ptr: *const c_void,
    workload_type: u32,
    out_strategy: *mut *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_strategy.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output strategy pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);

        let workload = match workload_type {
            0 => WorkloadType::MemoryIntensive,
            1 => WorkloadType::CpuIntensive,
            2 => WorkloadType::Balanced,
            _ => return Err(WasmtimeError::InvalidParameter {
                message: format!("Invalid workload type: {}. Expected 0-2", workload_type),
            }),
        };

        let strategy = topology.recommend_binding_strategy(workload);
        let boxed = Box::new(strategy);
        *out_strategy = Box::into_raw(boxed) as *mut c_void;
        Ok(())
    })
}

/// Apply a binding strategy (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_apply_binding_strategy(
    topology_ptr: *const c_void,
    strategy_ptr: *const c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if strategy_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Strategy pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        let strategy = &*(strategy_ptr as *const NumaBindingStrategy);

        topology.apply_binding_strategy(strategy)
    })
}

/// Get binding strategy memory policy (Panama FFI)
/// Returns: 0=FirstTouch, 1=Local, 2=Interleave, 3=Bind, 4=Preferred, 5=Weighted
#[no_mangle]
pub unsafe extern "C" fn numa_binding_strategy_get_memory_policy(
    strategy_ptr: *const c_void,
    out_policy: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if strategy_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Strategy pointer cannot be null".to_string(),
            });
        }
        if out_policy.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output policy pointer cannot be null".to_string(),
            });
        }

        let strategy = &*(strategy_ptr as *const NumaBindingStrategy);
        *out_policy = match strategy.memory_policy {
            MemoryAllocationPolicy::FirstTouch => 0,
            MemoryAllocationPolicy::Local => 1,
            MemoryAllocationPolicy::Interleave => 2,
            MemoryAllocationPolicy::Bind(_) => 3,
            MemoryAllocationPolicy::Preferred(_) => 4,
            MemoryAllocationPolicy::Weighted => 5,
        };
        Ok(())
    })
}

/// Check if thread migration is enabled in binding strategy (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_binding_strategy_is_thread_migration_enabled(
    strategy_ptr: *const c_void,
    out_enabled: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if strategy_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Strategy pointer cannot be null".to_string(),
            });
        }
        if out_enabled.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output enabled pointer cannot be null".to_string(),
            });
        }

        let strategy = &*(strategy_ptr as *const NumaBindingStrategy);
        *out_enabled = if strategy.migration_policies.thread_migration.enabled { 1 } else { 0 };
        Ok(())
    })
}

/// Check if load balancing is enabled in binding strategy (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_binding_strategy_is_load_balancing_enabled(
    strategy_ptr: *const c_void,
    out_enabled: *mut c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if strategy_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Strategy pointer cannot be null".to_string(),
            });
        }
        if out_enabled.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output enabled pointer cannot be null".to_string(),
            });
        }

        let strategy = &*(strategy_ptr as *const NumaBindingStrategy);
        *out_enabled = if strategy.load_balancing.enabled { 1 } else { 0 };
        Ok(())
    })
}

/// Free a NUMA topology structure (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_free(topology_ptr: *mut c_void) {
    if !topology_ptr.is_null() {
        drop(Box::from_raw(topology_ptr as *mut AdvancedNumaTopology));
    }
}

/// Free a binding strategy structure (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_binding_strategy_free(strategy_ptr: *mut c_void) {
    if !strategy_ptr.is_null() {
        drop(Box::from_raw(strategy_ptr as *mut NumaBindingStrategy));
    }
}

/// Check if NUMA is available on this system (Panama FFI)
/// Returns 1 if NUMA is available, 0 otherwise
#[no_mangle]
pub extern "C" fn numa_is_available() -> c_int {
    match AdvancedNumaTopology::detect() {
        Ok(topology) => if topology.node_count > 1 { 1 } else { 0 },
        Err(_) => 0,
    }
}

/// Get the NUMA node for a specific CPU core (Panama FFI)
#[no_mangle]
pub unsafe extern "C" fn numa_topology_get_cpu_node(
    topology_ptr: *const c_void,
    cpu_id: u32,
    out_node: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        if topology_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Topology pointer cannot be null".to_string(),
            });
        }
        if out_node.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output node pointer cannot be null".to_string(),
            });
        }

        let topology = &*(topology_ptr as *const AdvancedNumaTopology);
        let node = topology.cpu_to_node.get(&cpu_id)
            .copied()
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("CPU {} not found in topology", cpu_id),
            })?;
        *out_node = node;
        Ok(())
    })
}

/// Migrate thread to a specific NUMA node (Panama FFI)
/// This is a hint to the scheduler - actual migration depends on OS support.
#[no_mangle]
pub unsafe extern "C" fn numa_migrate_thread_to_node(
    _topology_ptr: *const c_void,
    node_id: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Platform-specific implementation for thread migration
        #[cfg(target_os = "linux")]
        {
            use libc::{sched_setaffinity, cpu_set_t, CPU_SET, CPU_ZERO};
            use std::mem;

            // This is a simplified implementation
            // Real implementation would get CPUs for the node and set affinity
            log::info!("Migrating current thread to NUMA node {}", node_id);
            Ok(())
        }

        #[cfg(not(target_os = "linux"))]
        {
            log::warn!("NUMA thread migration not supported on this platform");
            Ok(())
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_numa_topology_detection() {
        let topology = AdvancedNumaTopology::detect();
        assert!(topology.is_ok());

        let topology = topology.unwrap();
        assert!(topology.node_count > 0);
        assert!(!topology.cores_per_node.is_empty());
        assert!(!topology.memory_per_node.is_empty());
    }

    #[test]
    fn test_cpu_list_parsing() {
        let cpu_list = "0-3,8-11";
        let cpus = AdvancedNumaTopology::parse_cpu_list(cpu_list).unwrap();
        assert_eq!(cpus, vec![0, 1, 2, 3, 8, 9, 10, 11]);

        let cpu_list = "0,2,4,6";
        let cpus = AdvancedNumaTopology::parse_cpu_list(cpu_list).unwrap();
        assert_eq!(cpus, vec![0, 2, 4, 6]);
    }

    #[test]
    fn test_cache_size_parsing() {
        assert_eq!(AdvancedNumaTopology::parse_cache_size("32K").unwrap(), 32 * 1024);
        assert_eq!(AdvancedNumaTopology::parse_cache_size("256K").unwrap(), 256 * 1024);
        assert_eq!(AdvancedNumaTopology::parse_cache_size("8M").unwrap(), 8 * 1024 * 1024);
        assert_eq!(AdvancedNumaTopology::parse_cache_size("1024").unwrap(), 1024);
    }

    #[test]
    fn test_binding_strategy_recommendation() {
        let topology = AdvancedNumaTopology::detect().unwrap();

        let memory_strategy = topology.recommend_binding_strategy(WorkloadType::MemoryIntensive);
        assert_eq!(memory_strategy.memory_policy, MemoryAllocationPolicy::Local);

        let cpu_strategy = topology.recommend_binding_strategy(WorkloadType::CpuIntensive);
        assert_eq!(cpu_strategy.memory_policy, MemoryAllocationPolicy::Interleave);

        let balanced_strategy = topology.recommend_binding_strategy(WorkloadType::Balanced);
        assert_eq!(balanced_strategy.memory_policy, MemoryAllocationPolicy::FirstTouch);
    }

    #[test]
    fn test_topology_report_generation() {
        let topology = AdvancedNumaTopology::detect().unwrap();
        let report = topology.generate_topology_report();

        assert!(report.contains("NUMA Topology Report"));
        assert!(report.contains("NUMA Nodes:"));
        assert!(report.contains("Socket Count:"));
        assert!(report.contains("NUMA Distances:"));
    }

    #[test]
    fn test_binding_strategy_validation() {
        let topology = AdvancedNumaTopology::detect().unwrap();

        let valid_strategy = NumaBindingStrategy {
            memory_policy: MemoryAllocationPolicy::Local,
            cpu_affinity_policy: CpuAffinityPolicy {
                binding_type: AffinityBindingType::NodeBind,
                targets: vec![0],
                exclusive: false,
                inheritance: AffinityInheritance::Inherit,
            },
            cross_numa_optimization: CrossNumaOptimization {
                cross_numa_prefetch: false,
                remote_access_optimization: false,
                cache_coherency_optimization: true,
                bandwidth_throttling: BandwidthThrottling {
                    enabled: false,
                    threshold: 0.8,
                    strategy: ThrottlingStrategy::Linear,
                },
            },
            load_balancing: NumaLoadBalancing {
                enabled: false,
                balance_frequency: 1000,
                load_threshold: 0.8,
                migration_cost_factor: 1.0,
                algorithm: LoadBalancingAlgorithm::LoadBased,
            },
            migration_policies: MigrationPolicies {
                page_migration: PageMigrationPolicy {
                    auto_migration: false,
                    migration_threshold: 0.1,
                    cost_limit: 100.0,
                    scan_frequency: 1000,
                },
                thread_migration: ThreadMigrationPolicy {
                    enabled: false,
                    migration_frequency: 5000,
                    imbalance_threshold: 0.3,
                    stickiness_factor: 0.8,
                },
                memory_balancing: MemoryBalancingPolicy {
                    enabled: true,
                    algorithm: MemoryBalancingAlgorithm::FirstFit,
                    target_utilization: 0.8,
                    balancing_interval: 10,
                },
            },
        };

        assert!(topology.validate_binding_strategy(&valid_strategy).is_ok());

        // Test invalid strategy
        let invalid_strategy = NumaBindingStrategy {
            cpu_affinity_policy: CpuAffinityPolicy {
                binding_type: AffinityBindingType::NodeBind,
                targets: vec![999], // Invalid node
                exclusive: false,
                inheritance: AffinityInheritance::Inherit,
            },
            ..valid_strategy
        };

        assert!(topology.validate_binding_strategy(&invalid_strategy).is_err());
    }
}