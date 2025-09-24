//! Platform-specific optimization and tuning configuration for wasmtime4j
//!
//! This module provides comprehensive platform detection, CPU feature analysis,
//! and architecture-specific optimization settings for maximum performance.

use std::collections::HashMap;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Platform-specific configuration with runtime detection and optimization
#[derive(Debug, Clone)]
pub struct PlatformConfig {
    /// Detected platform information
    pub platform_info: PlatformInfo,
    /// CPU-specific optimizations
    pub cpu_optimization: CpuOptimization,
    /// Memory hierarchy configuration
    pub memory_hierarchy: MemoryHierarchyConfig,
    /// NUMA configuration
    pub numa_config: NumaConfig,
    /// Thread affinity settings
    pub thread_affinity: ThreadAffinityConfig,
    /// Operating system optimizations
    pub os_optimization: OsOptimization,
    /// Architecture-specific settings
    pub architecture_specific: ArchitectureSpecificConfig,
    /// Power management settings
    pub power_management: PowerManagementConfig,
}

/// Platform information detected at runtime
#[derive(Debug, Clone)]
pub struct PlatformInfo {
    /// Operating system type
    pub os_type: OsType,
    /// CPU architecture
    pub architecture: Architecture,
    /// CPU model and vendor
    pub cpu_model: CpuModel,
    /// Number of CPU cores
    pub cpu_cores: u32,
    /// Number of logical processors
    pub logical_processors: u32,
    /// Available memory in bytes
    pub total_memory: u64,
    /// Cache hierarchy information
    pub cache_hierarchy: CacheHierarchy,
    /// Supported CPU features
    pub cpu_features: SupportedCpuFeatures,
    /// NUMA topology
    pub numa_topology: NumaTopology,
}

/// CPU optimization configuration
#[derive(Debug, Clone)]
pub struct CpuOptimization {
    /// Enable CPU-specific instruction selection
    pub cpu_specific_instructions: bool,
    /// Enable branch prediction optimization
    pub branch_prediction_optimization: bool,
    /// Enable cache-friendly code generation
    pub cache_friendly_codegen: bool,
    /// Enable instruction pipeline optimization
    pub pipeline_optimization: bool,
    /// Enable superscalar optimization
    pub superscalar_optimization: bool,
    /// Enable out-of-order execution optimization
    pub out_of_order_optimization: bool,
    /// CPU frequency scaling awareness
    pub frequency_scaling_awareness: bool,
    /// Thermal throttling awareness
    pub thermal_throttling_awareness: bool,
}

/// Memory hierarchy configuration
#[derive(Debug, Clone)]
pub struct MemoryHierarchyConfig {
    /// L1 cache optimization
    pub l1_cache_optimization: L1CacheOptimization,
    /// L2 cache optimization
    pub l2_cache_optimization: L2CacheOptimization,
    /// L3 cache optimization
    pub l3_cache_optimization: L3CacheOptimization,
    /// Main memory optimization
    pub main_memory_optimization: MainMemoryOptimization,
    /// Prefetching configuration
    pub prefetching: PrefetchingConfig,
    /// Memory bandwidth optimization
    pub bandwidth_optimization: BandwidthOptimization,
}

/// NUMA (Non-Uniform Memory Access) configuration
#[derive(Debug, Clone)]
pub struct NumaConfig {
    /// Enable NUMA awareness
    pub numa_awareness: bool,
    /// NUMA node allocation strategy
    pub allocation_strategy: NumaAllocationStrategy,
    /// Inter-node communication optimization
    pub inter_node_optimization: bool,
    /// Memory locality optimization
    pub memory_locality_optimization: bool,
    /// NUMA balancing configuration
    pub numa_balancing: NumaBalancingConfig,
}

/// Thread affinity configuration
#[derive(Debug, Clone)]
pub struct ThreadAffinityConfig {
    /// Enable thread affinity optimization
    pub enabled: bool,
    /// CPU binding strategy
    pub binding_strategy: CpuBindingStrategy,
    /// Core allocation preferences
    pub core_allocation: CoreAllocationPreferences,
    /// Hyper-threading awareness
    pub hyperthread_awareness: bool,
    /// Load balancing configuration
    pub load_balancing: LoadBalancingConfig,
}

/// Operating system specific optimizations
#[derive(Debug, Clone)]
pub struct OsOptimization {
    /// System call optimization
    pub syscall_optimization: SyscallOptimization,
    /// Virtual memory optimization
    pub virtual_memory_optimization: VirtualMemoryOptimization,
    /// I/O optimization
    pub io_optimization: IoOptimization,
    /// Scheduler integration
    pub scheduler_integration: SchedulerIntegration,
    /// Resource management optimization
    pub resource_management: ResourceManagementOptimization,
}

/// Architecture-specific configuration
#[derive(Debug, Clone)]
pub struct ArchitectureSpecificConfig {
    /// x86/x86_64 specific settings
    pub x86_config: X86ArchConfig,
    /// ARM/ARM64 specific settings
    pub arm_config: ArmArchConfig,
    /// RISC-V specific settings
    pub riscv_config: RiscVArchConfig,
    /// Other architecture settings
    pub other_config: OtherArchConfig,
}

/// Power management and thermal configuration
#[derive(Debug, Clone)]
pub struct PowerManagementConfig {
    /// CPU frequency management
    pub frequency_management: FrequencyManagementConfig,
    /// Thermal management
    pub thermal_management: ThermalManagementConfig,
    /// Power efficiency optimization
    pub power_efficiency: PowerEfficiencyConfig,
    /// Energy-aware scheduling
    pub energy_aware_scheduling: bool,
}

/// Operating system types
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum OsType {
    Linux,
    Windows,
    MacOS,
    FreeBSD,
    Other,
}

/// CPU architecture types
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum Architecture {
    X86_64,
    AArch64,
    RiscV64,
    X86,
    AArch32,
    Other,
}

/// CPU model information
#[derive(Debug, Clone)]
pub struct CpuModel {
    /// CPU vendor (Intel, AMD, ARM, etc.)
    pub vendor: String,
    /// CPU family
    pub family: String,
    /// CPU model name
    pub model: String,
    /// CPU stepping/revision
    pub stepping: u32,
    /// Base frequency in MHz
    pub base_frequency: u32,
    /// Maximum frequency in MHz
    pub max_frequency: u32,
    /// Cache sizes and configuration
    pub cache_info: CacheInfo,
}

/// Cache hierarchy information
#[derive(Debug, Clone)]
pub struct CacheHierarchy {
    /// L1 instruction cache
    pub l1i_cache: CacheLevel,
    /// L1 data cache
    pub l1d_cache: CacheLevel,
    /// L2 cache
    pub l2_cache: CacheLevel,
    /// L3 cache
    pub l3_cache: Option<CacheLevel>,
    /// Cache line size
    pub cache_line_size: u32,
}

/// Individual cache level information
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
    pub latency: u32,
}

/// Supported CPU features
#[derive(Debug, Clone)]
pub struct SupportedCpuFeatures {
    /// x86-specific features
    pub x86_features: X86Features,
    /// ARM-specific features
    pub arm_features: ArmFeatures,
    /// RISC-V specific features
    pub riscv_features: RiscVFeatures,
    /// Generic features
    pub generic_features: GenericFeatures,
}

/// NUMA topology information
#[derive(Debug, Clone)]
pub struct NumaTopology {
    /// Number of NUMA nodes
    pub node_count: u32,
    /// CPU cores per NUMA node
    pub cores_per_node: Vec<u32>,
    /// Memory per NUMA node in bytes
    pub memory_per_node: Vec<u64>,
    /// Inter-node distances
    pub node_distances: Vec<Vec<u32>>,
    /// NUMA node CPU mappings
    pub cpu_node_mapping: HashMap<u32, u32>,
}

/// L1 cache optimization settings
#[derive(Debug, Clone)]
pub struct L1CacheOptimization {
    /// Enable L1 cache-friendly data layout
    pub cache_friendly_layout: bool,
    /// Enable L1 instruction cache optimization
    pub instruction_cache_optimization: bool,
    /// Enable L1 data cache optimization
    pub data_cache_optimization: bool,
    /// L1 cache line alignment
    pub cache_line_alignment: bool,
    /// L1 cache blocking factor
    pub cache_blocking_factor: u32,
}

/// L2 cache optimization settings
#[derive(Debug, Clone)]
pub struct L2CacheOptimization {
    /// Enable L2 cache tiling
    pub cache_tiling: bool,
    /// L2 cache blocking strategy
    pub blocking_strategy: CacheBlockingStrategy,
    /// Enable L2 cache prefetching
    pub prefetching: bool,
    /// L2 cache replacement policy awareness
    pub replacement_policy_awareness: bool,
}

/// L3 cache optimization settings
#[derive(Debug, Clone)]
pub struct L3CacheOptimization {
    /// Enable L3 cache partitioning
    pub cache_partitioning: bool,
    /// L3 cache sharing strategy
    pub sharing_strategy: CacheSharingStrategy,
    /// Enable L3 cache optimization for multi-core
    pub multicore_optimization: bool,
    /// L3 cache victim optimization
    pub victim_optimization: bool,
}

/// Main memory optimization settings
#[derive(Debug, Clone)]
pub struct MainMemoryOptimization {
    /// Memory bandwidth optimization
    pub bandwidth_optimization: bool,
    /// Memory latency hiding
    pub latency_hiding: bool,
    /// Memory controller optimization
    pub controller_optimization: bool,
    /// DRAM timing optimization
    pub timing_optimization: bool,
}

/// Prefetching configuration
#[derive(Debug, Clone)]
pub struct PrefetchingConfig {
    /// Enable hardware prefetching
    pub hardware_prefetching: bool,
    /// Enable software prefetching
    pub software_prefetching: bool,
    /// Prefetch distance
    pub prefetch_distance: u32,
    /// Prefetch stride detection
    pub stride_detection: bool,
    /// Adaptive prefetching
    pub adaptive_prefetching: bool,
}

/// Memory bandwidth optimization
#[derive(Debug, Clone)]
pub struct BandwidthOptimization {
    /// Enable memory interleaving
    pub memory_interleaving: bool,
    /// Enable dual-channel optimization
    pub dual_channel_optimization: bool,
    /// Enable multi-channel optimization
    pub multi_channel_optimization: bool,
    /// Memory access pattern optimization
    pub access_pattern_optimization: bool,
}

/// NUMA allocation strategy
#[derive(Debug, Clone, Copy)]
pub enum NumaAllocationStrategy {
    /// First-touch allocation
    FirstTouch,
    /// Round-robin allocation
    RoundRobin,
    /// Interleaved allocation
    Interleaved,
    /// Local allocation
    Local,
    /// Preferred allocation
    Preferred,
}

/// NUMA balancing configuration
#[derive(Debug, Clone)]
pub struct NumaBalancingConfig {
    /// Enable automatic NUMA balancing
    pub automatic_balancing: bool,
    /// Migration threshold
    pub migration_threshold: f64,
    /// Balancing period in milliseconds
    pub balancing_period: u32,
    /// Migration cost factor
    pub migration_cost_factor: f64,
}

/// CPU binding strategy
#[derive(Debug, Clone, Copy)]
pub enum CpuBindingStrategy {
    /// No binding (free scheduling)
    None,
    /// Bind to specific cores
    SpecificCores,
    /// Bind to core groups
    CoreGroups,
    /// Bind to NUMA nodes
    NumaNodes,
    /// Bind avoiding hyper-threads
    AvoidHyperThreads,
}

/// Core allocation preferences
#[derive(Debug, Clone)]
pub struct CoreAllocationPreferences {
    /// Prefer physical cores over logical cores
    pub prefer_physical_cores: bool,
    /// Prefer local NUMA nodes
    pub prefer_local_numa: bool,
    /// Prefer lower-numbered cores
    pub prefer_lower_cores: bool,
    /// Avoid shared cache interference
    pub avoid_cache_interference: bool,
}

/// Load balancing configuration
#[derive(Debug, Clone)]
pub struct LoadBalancingConfig {
    /// Enable work stealing
    pub work_stealing: bool,
    /// Load balancing threshold
    pub load_threshold: f64,
    /// Migration frequency
    pub migration_frequency: u32,
    /// Balance across NUMA nodes
    pub numa_balancing: bool,
}

/// System call optimization
#[derive(Debug, Clone)]
pub struct SyscallOptimization {
    /// Enable syscall batching
    pub syscall_batching: bool,
    /// Enable VDSO optimization
    pub vdso_optimization: bool,
    /// Enable fast system calls
    pub fast_syscalls: bool,
    /// Syscall caching
    pub syscall_caching: bool,
}

/// Virtual memory optimization
#[derive(Debug, Clone)]
pub struct VirtualMemoryOptimization {
    /// Enable huge pages
    pub huge_pages: bool,
    /// Transparent huge pages
    pub transparent_huge_pages: bool,
    /// Memory mapping optimization
    pub mmap_optimization: bool,
    /// TLB optimization
    pub tlb_optimization: bool,
}

/// I/O optimization settings
#[derive(Debug, Clone)]
pub struct IoOptimization {
    /// Enable asynchronous I/O
    pub async_io: bool,
    /// I/O scheduler optimization
    pub scheduler_optimization: bool,
    /// Buffer management optimization
    pub buffer_optimization: bool,
    /// Direct I/O when beneficial
    pub direct_io: bool,
}

/// Scheduler integration settings
#[derive(Debug, Clone)]
pub struct SchedulerIntegration {
    /// OS scheduler awareness
    pub scheduler_awareness: bool,
    /// Priority-based scheduling
    pub priority_scheduling: bool,
    /// Real-time scheduling support
    pub realtime_scheduling: bool,
    /// CPU isolation support
    pub cpu_isolation: bool,
}

/// Resource management optimization
#[derive(Debug, Clone)]
pub struct ResourceManagementOptimization {
    /// Memory allocation optimization
    pub memory_allocation: bool,
    /// File descriptor optimization
    pub fd_optimization: bool,
    /// Process/thread optimization
    pub process_optimization: bool,
    /// Resource pooling
    pub resource_pooling: bool,
}

/// x86/x86_64 architecture configuration
#[derive(Debug, Clone)]
pub struct X86ArchConfig {
    /// Enable x86-specific optimizations
    pub x86_optimizations: bool,
    /// Use x86-specific instructions
    pub specific_instructions: X86Instructions,
    /// x86 cache optimization
    pub cache_optimization: X86CacheOptimization,
    /// x86 branch optimization
    pub branch_optimization: X86BranchOptimization,
}

/// ARM/ARM64 architecture configuration
#[derive(Debug, Clone)]
pub struct ArmArchConfig {
    /// Enable ARM-specific optimizations
    pub arm_optimizations: bool,
    /// ARM instruction set optimization
    pub instruction_set: ArmInstructionSet,
    /// ARM cache optimization
    pub cache_optimization: ArmCacheOptimization,
    /// ARM power optimization
    pub power_optimization: ArmPowerOptimization,
}

/// RISC-V architecture configuration
#[derive(Debug, Clone)]
pub struct RiscVArchConfig {
    /// Enable RISC-V optimizations
    pub riscv_optimizations: bool,
    /// RISC-V extension support
    pub extensions: RiscVExtensions,
    /// RISC-V custom instructions
    pub custom_instructions: bool,
    /// RISC-V vector optimization
    pub vector_optimization: bool,
}

/// Other architecture configuration
#[derive(Debug, Clone)]
pub struct OtherArchConfig {
    /// Generic optimizations
    pub generic_optimizations: bool,
    /// Custom architecture support
    pub custom_support: bool,
}

/// Frequency management configuration
#[derive(Debug, Clone)]
pub struct FrequencyManagementConfig {
    /// Enable dynamic frequency scaling
    pub dynamic_scaling: bool,
    /// Performance governor preference
    pub performance_governor: bool,
    /// Turbo boost awareness
    pub turbo_boost_awareness: bool,
    /// Frequency scaling strategy
    pub scaling_strategy: FrequencyScalingStrategy,
}

/// Thermal management configuration
#[derive(Debug, Clone)]
pub struct ThermalManagementConfig {
    /// Enable thermal monitoring
    pub thermal_monitoring: bool,
    /// Thermal throttling awareness
    pub throttling_awareness: bool,
    /// Temperature-based optimization
    pub temperature_optimization: bool,
    /// Cooling strategy
    pub cooling_strategy: CoolingStrategy,
}

/// Power efficiency configuration
#[derive(Debug, Clone)]
pub struct PowerEfficiencyConfig {
    /// Enable power-aware scheduling
    pub power_aware_scheduling: bool,
    /// Sleep state optimization
    pub sleep_state_optimization: bool,
    /// Dynamic voltage scaling
    pub dynamic_voltage_scaling: bool,
    /// Energy efficiency preference
    pub efficiency_preference: EnergyEfficiencyPreference,
}

// Feature enumerations and implementation types
#[derive(Debug, Clone)]
pub struct X86Features {
    pub sse: bool,
    pub sse2: bool,
    pub sse3: bool,
    pub ssse3: bool,
    pub sse4_1: bool,
    pub sse4_2: bool,
    pub avx: bool,
    pub avx2: bool,
    pub avx512: bool,
    pub bmi1: bool,
    pub bmi2: bool,
    pub popcnt: bool,
    pub lzcnt: bool,
    pub tzcnt: bool,
    pub aes: bool,
    pub pclmulqdq: bool,
    pub sha: bool,
}

#[derive(Debug, Clone)]
pub struct ArmFeatures {
    pub neon: bool,
    pub crypto: bool,
    pub sve: bool,
    pub sve2: bool,
    pub dotprod: bool,
    pub fp16: bool,
    pub bf16: bool,
    pub i8mm: bool,
}

#[derive(Debug, Clone)]
pub struct RiscVFeatures {
    pub rv64i: bool,
    pub rv64m: bool,
    pub rv64a: bool,
    pub rv64f: bool,
    pub rv64d: bool,
    pub rv64c: bool,
    pub rv64v: bool,
    pub rv64b: bool,
}

#[derive(Debug, Clone)]
pub struct GenericFeatures {
    pub atomic_operations: bool,
    pub floating_point: bool,
    pub vector_operations: bool,
    pub branch_prediction: bool,
    pub out_of_order_execution: bool,
    pub superscalar: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum CacheBlockingStrategy {
    NoBlocking,
    SimpleBlocking,
    HierarchicalBlocking,
    AdaptiveBlocking,
}

#[derive(Debug, Clone, Copy)]
pub enum CacheSharingStrategy {
    Exclusive,
    Shared,
    Partitioned,
    Adaptive,
}

#[derive(Debug, Clone)]
pub struct X86Instructions {
    pub use_sse: bool,
    pub use_avx: bool,
    pub use_bmi: bool,
    pub use_popcnt: bool,
    pub use_lzcnt: bool,
}

#[derive(Debug, Clone)]
pub struct X86CacheOptimization {
    pub cache_line_prefetch: bool,
    pub store_to_load_forwarding: bool,
    pub cache_blocking: bool,
}

#[derive(Debug, Clone)]
pub struct X86BranchOptimization {
    pub branch_prediction_hints: bool,
    pub indirect_branch_optimization: bool,
    pub return_stack_optimization: bool,
}

#[derive(Debug, Clone)]
pub struct ArmInstructionSet {
    pub use_neon: bool,
    pub use_crypto: bool,
    pub use_sve: bool,
    pub use_dotprod: bool,
}

#[derive(Debug, Clone)]
pub struct ArmCacheOptimization {
    pub cache_line_alignment: bool,
    pub prefetch_optimization: bool,
    pub cache_coloring: bool,
}

#[derive(Debug, Clone)]
pub struct ArmPowerOptimization {
    pub big_little_scheduling: bool,
    pub dynamic_voltage_scaling: bool,
    pub power_gating: bool,
}

#[derive(Debug, Clone)]
pub struct RiscVExtensions {
    pub vector_extension: bool,
    pub bit_manipulation: bool,
    pub compressed_instructions: bool,
    pub atomic_operations: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum FrequencyScalingStrategy {
    Performance,
    Powersave,
    Ondemand,
    Conservative,
    Userspace,
}

#[derive(Debug, Clone, Copy)]
pub enum CoolingStrategy {
    Passive,
    Active,
    Adaptive,
    Aggressive,
}

#[derive(Debug, Clone, Copy)]
pub enum EnergyEfficiencyPreference {
    Performance,
    Balance,
    PowerSave,
    MaxEfficiency,
}

#[derive(Debug, Clone)]
pub struct CacheInfo {
    pub l1i_size: u32,
    pub l1d_size: u32,
    pub l2_size: u32,
    pub l3_size: u32,
    pub cache_line_size: u32,
}

impl Default for PlatformConfig {
    fn default() -> Self {
        Self {
            platform_info: PlatformInfo::detect(),
            cpu_optimization: CpuOptimization::default(),
            memory_hierarchy: MemoryHierarchyConfig::default(),
            numa_config: NumaConfig::default(),
            thread_affinity: ThreadAffinityConfig::default(),
            os_optimization: OsOptimization::default(),
            architecture_specific: ArchitectureSpecificConfig::default(),
            power_management: PowerManagementConfig::default(),
        }
    }
}

impl PlatformInfo {
    /// Detect platform information at runtime
    pub fn detect() -> Self {
        Self {
            os_type: Self::detect_os(),
            architecture: Self::detect_architecture(),
            cpu_model: Self::detect_cpu_model(),
            cpu_cores: Self::detect_cpu_cores(),
            logical_processors: Self::detect_logical_processors(),
            total_memory: Self::detect_total_memory(),
            cache_hierarchy: Self::detect_cache_hierarchy(),
            cpu_features: Self::detect_cpu_features(),
            numa_topology: Self::detect_numa_topology(),
        }
    }

    fn detect_os() -> OsType {
        if cfg!(target_os = "linux") {
            OsType::Linux
        } else if cfg!(target_os = "windows") {
            OsType::Windows
        } else if cfg!(target_os = "macos") {
            OsType::MacOS
        } else if cfg!(target_os = "freebsd") {
            OsType::FreeBSD
        } else {
            OsType::Other
        }
    }

    fn detect_architecture() -> Architecture {
        if cfg!(target_arch = "x86_64") {
            Architecture::X86_64
        } else if cfg!(target_arch = "aarch64") {
            Architecture::AArch64
        } else if cfg!(target_arch = "riscv64") {
            Architecture::RiscV64
        } else if cfg!(target_arch = "x86") {
            Architecture::X86
        } else if cfg!(target_arch = "arm") {
            Architecture::AArch32
        } else {
            Architecture::Other
        }
    }

    fn detect_cpu_model() -> CpuModel {
        // This would be implemented using platform-specific APIs
        // For now, return defaults
        CpuModel {
            vendor: "Unknown".to_string(),
            family: "Unknown".to_string(),
            model: "Unknown".to_string(),
            stepping: 0,
            base_frequency: 2000, // 2 GHz default
            max_frequency: 3000,  // 3 GHz default
            cache_info: CacheInfo {
                l1i_size: 32 * 1024,   // 32KB
                l1d_size: 32 * 1024,   // 32KB
                l2_size: 256 * 1024,   // 256KB
                l3_size: 8 * 1024 * 1024, // 8MB
                cache_line_size: 64,
            },
        }
    }

    fn detect_cpu_cores() -> u32 {
        std::thread::available_parallelism()
            .map(|p| p.get() as u32)
            .unwrap_or(1)
    }

    fn detect_logical_processors() -> u32 {
        Self::detect_cpu_cores() // Simplified for now
    }

    fn detect_total_memory() -> u64 {
        // This would be implemented using platform-specific APIs
        8 * 1024 * 1024 * 1024 // 8GB default
    }

    fn detect_cache_hierarchy() -> CacheHierarchy {
        CacheHierarchy {
            l1i_cache: CacheLevel {
                size: 32 * 1024,
                associativity: 8,
                line_size: 64,
                sets: 64,
                latency: 1,
            },
            l1d_cache: CacheLevel {
                size: 32 * 1024,
                associativity: 8,
                line_size: 64,
                sets: 64,
                latency: 1,
            },
            l2_cache: CacheLevel {
                size: 256 * 1024,
                associativity: 8,
                line_size: 64,
                sets: 512,
                latency: 10,
            },
            l3_cache: Some(CacheLevel {
                size: 8 * 1024 * 1024,
                associativity: 16,
                line_size: 64,
                sets: 8192,
                latency: 30,
            }),
            cache_line_size: 64,
        }
    }

    fn detect_cpu_features() -> SupportedCpuFeatures {
        SupportedCpuFeatures {
            x86_features: X86Features::detect(),
            arm_features: ArmFeatures::detect(),
            riscv_features: RiscVFeatures::detect(),
            generic_features: GenericFeatures::detect(),
        }
    }

    fn detect_numa_topology() -> NumaTopology {
        NumaTopology {
            node_count: 1,
            cores_per_node: vec![Self::detect_cpu_cores()],
            memory_per_node: vec![Self::detect_total_memory()],
            node_distances: vec![vec![0]],
            cpu_node_mapping: HashMap::new(),
        }
    }
}

impl X86Features {
    fn detect() -> Self {
        // This would use CPUID instructions to detect features
        Self {
            sse: cfg!(target_feature = "sse"),
            sse2: cfg!(target_feature = "sse2"),
            sse3: cfg!(target_feature = "sse3"),
            ssse3: cfg!(target_feature = "ssse3"),
            sse4_1: cfg!(target_feature = "sse4.1"),
            sse4_2: cfg!(target_feature = "sse4.2"),
            avx: cfg!(target_feature = "avx"),
            avx2: cfg!(target_feature = "avx2"),
            avx512: false, // Not commonly available
            bmi1: cfg!(target_feature = "bmi1"),
            bmi2: cfg!(target_feature = "bmi2"),
            popcnt: cfg!(target_feature = "popcnt"),
            lzcnt: cfg!(target_feature = "lzcnt"),
            tzcnt: false,
            aes: cfg!(target_feature = "aes"),
            pclmulqdq: cfg!(target_feature = "pclmulqdq"),
            sha: false,
        }
    }
}

impl ArmFeatures {
    fn detect() -> Self {
        Self {
            neon: cfg!(target_feature = "neon"),
            crypto: false,
            sve: false,
            sve2: false,
            dotprod: false,
            fp16: false,
            bf16: false,
            i8mm: false,
        }
    }
}

impl RiscVFeatures {
    fn detect() -> Self {
        Self {
            rv64i: cfg!(target_arch = "riscv64"),
            rv64m: false,
            rv64a: false,
            rv64f: false,
            rv64d: false,
            rv64c: false,
            rv64v: false,
            rv64b: false,
        }
    }
}

impl GenericFeatures {
    fn detect() -> Self {
        Self {
            atomic_operations: true,
            floating_point: true,
            vector_operations: cfg!(any(target_feature = "sse", target_feature = "neon")),
            branch_prediction: true,
            out_of_order_execution: true,
            superscalar: true,
        }
    }
}

impl Default for CpuOptimization {
    fn default() -> Self {
        Self {
            cpu_specific_instructions: true,
            branch_prediction_optimization: true,
            cache_friendly_codegen: true,
            pipeline_optimization: true,
            superscalar_optimization: true,
            out_of_order_optimization: true,
            frequency_scaling_awareness: false,
            thermal_throttling_awareness: false,
        }
    }
}

impl Default for MemoryHierarchyConfig {
    fn default() -> Self {
        Self {
            l1_cache_optimization: L1CacheOptimization::default(),
            l2_cache_optimization: L2CacheOptimization::default(),
            l3_cache_optimization: L3CacheOptimization::default(),
            main_memory_optimization: MainMemoryOptimization::default(),
            prefetching: PrefetchingConfig::default(),
            bandwidth_optimization: BandwidthOptimization::default(),
        }
    }
}

impl Default for L1CacheOptimization {
    fn default() -> Self {
        Self {
            cache_friendly_layout: true,
            instruction_cache_optimization: true,
            data_cache_optimization: true,
            cache_line_alignment: true,
            cache_blocking_factor: 4,
        }
    }
}

impl Default for L2CacheOptimization {
    fn default() -> Self {
        Self {
            cache_tiling: true,
            blocking_strategy: CacheBlockingStrategy::HierarchicalBlocking,
            prefetching: true,
            replacement_policy_awareness: true,
        }
    }
}

impl Default for L3CacheOptimization {
    fn default() -> Self {
        Self {
            cache_partitioning: false,
            sharing_strategy: CacheSharingStrategy::Shared,
            multicore_optimization: true,
            victim_optimization: true,
        }
    }
}

impl Default for MainMemoryOptimization {
    fn default() -> Self {
        Self {
            bandwidth_optimization: true,
            latency_hiding: true,
            controller_optimization: true,
            timing_optimization: false,
        }
    }
}

impl Default for PrefetchingConfig {
    fn default() -> Self {
        Self {
            hardware_prefetching: true,
            software_prefetching: false,
            prefetch_distance: 32,
            stride_detection: true,
            adaptive_prefetching: false,
        }
    }
}

impl Default for BandwidthOptimization {
    fn default() -> Self {
        Self {
            memory_interleaving: true,
            dual_channel_optimization: true,
            multi_channel_optimization: true,
            access_pattern_optimization: true,
        }
    }
}

impl Default for NumaConfig {
    fn default() -> Self {
        Self {
            numa_awareness: false,
            allocation_strategy: NumaAllocationStrategy::FirstTouch,
            inter_node_optimization: false,
            memory_locality_optimization: true,
            numa_balancing: NumaBalancingConfig::default(),
        }
    }
}

impl Default for NumaBalancingConfig {
    fn default() -> Self {
        Self {
            automatic_balancing: false,
            migration_threshold: 0.1,
            balancing_period: 1000,
            migration_cost_factor: 1.0,
        }
    }
}

impl Default for ThreadAffinityConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            binding_strategy: CpuBindingStrategy::None,
            core_allocation: CoreAllocationPreferences::default(),
            hyperthread_awareness: true,
            load_balancing: LoadBalancingConfig::default(),
        }
    }
}

impl Default for CoreAllocationPreferences {
    fn default() -> Self {
        Self {
            prefer_physical_cores: true,
            prefer_local_numa: true,
            prefer_lower_cores: false,
            avoid_cache_interference: true,
        }
    }
}

impl Default for LoadBalancingConfig {
    fn default() -> Self {
        Self {
            work_stealing: true,
            load_threshold: 0.8,
            migration_frequency: 100,
            numa_balancing: false,
        }
    }
}

impl Default for OsOptimization {
    fn default() -> Self {
        Self {
            syscall_optimization: SyscallOptimization::default(),
            virtual_memory_optimization: VirtualMemoryOptimization::default(),
            io_optimization: IoOptimization::default(),
            scheduler_integration: SchedulerIntegration::default(),
            resource_management: ResourceManagementOptimization::default(),
        }
    }
}

impl Default for SyscallOptimization {
    fn default() -> Self {
        Self {
            syscall_batching: false,
            vdso_optimization: true,
            fast_syscalls: true,
            syscall_caching: false,
        }
    }
}

impl Default for VirtualMemoryOptimization {
    fn default() -> Self {
        Self {
            huge_pages: false,
            transparent_huge_pages: false,
            mmap_optimization: true,
            tlb_optimization: true,
        }
    }
}

impl Default for IoOptimization {
    fn default() -> Self {
        Self {
            async_io: true,
            scheduler_optimization: true,
            buffer_optimization: true,
            direct_io: false,
        }
    }
}

impl Default for SchedulerIntegration {
    fn default() -> Self {
        Self {
            scheduler_awareness: false,
            priority_scheduling: false,
            realtime_scheduling: false,
            cpu_isolation: false,
        }
    }
}

impl Default for ResourceManagementOptimization {
    fn default() -> Self {
        Self {
            memory_allocation: true,
            fd_optimization: true,
            process_optimization: true,
            resource_pooling: true,
        }
    }
}

impl Default for ArchitectureSpecificConfig {
    fn default() -> Self {
        Self {
            x86_config: X86ArchConfig::default(),
            arm_config: ArmArchConfig::default(),
            riscv_config: RiscVArchConfig::default(),
            other_config: OtherArchConfig::default(),
        }
    }
}

impl Default for X86ArchConfig {
    fn default() -> Self {
        Self {
            x86_optimizations: cfg!(any(target_arch = "x86", target_arch = "x86_64")),
            specific_instructions: X86Instructions::default(),
            cache_optimization: X86CacheOptimization::default(),
            branch_optimization: X86BranchOptimization::default(),
        }
    }
}

impl Default for X86Instructions {
    fn default() -> Self {
        Self {
            use_sse: cfg!(target_feature = "sse"),
            use_avx: cfg!(target_feature = "avx"),
            use_bmi: cfg!(target_feature = "bmi1"),
            use_popcnt: cfg!(target_feature = "popcnt"),
            use_lzcnt: cfg!(target_feature = "lzcnt"),
        }
    }
}

impl Default for X86CacheOptimization {
    fn default() -> Self {
        Self {
            cache_line_prefetch: true,
            store_to_load_forwarding: true,
            cache_blocking: true,
        }
    }
}

impl Default for X86BranchOptimization {
    fn default() -> Self {
        Self {
            branch_prediction_hints: true,
            indirect_branch_optimization: true,
            return_stack_optimization: true,
        }
    }
}

impl Default for ArmArchConfig {
    fn default() -> Self {
        Self {
            arm_optimizations: cfg!(any(target_arch = "arm", target_arch = "aarch64")),
            instruction_set: ArmInstructionSet::default(),
            cache_optimization: ArmCacheOptimization::default(),
            power_optimization: ArmPowerOptimization::default(),
        }
    }
}

impl Default for ArmInstructionSet {
    fn default() -> Self {
        Self {
            use_neon: cfg!(target_feature = "neon"),
            use_crypto: false,
            use_sve: false,
            use_dotprod: false,
        }
    }
}

impl Default for ArmCacheOptimization {
    fn default() -> Self {
        Self {
            cache_line_alignment: true,
            prefetch_optimization: true,
            cache_coloring: false,
        }
    }
}

impl Default for ArmPowerOptimization {
    fn default() -> Self {
        Self {
            big_little_scheduling: false,
            dynamic_voltage_scaling: false,
            power_gating: false,
        }
    }
}

impl Default for RiscVArchConfig {
    fn default() -> Self {
        Self {
            riscv_optimizations: cfg!(target_arch = "riscv64"),
            extensions: RiscVExtensions::default(),
            custom_instructions: false,
            vector_optimization: false,
        }
    }
}

impl Default for RiscVExtensions {
    fn default() -> Self {
        Self {
            vector_extension: false,
            bit_manipulation: false,
            compressed_instructions: true,
            atomic_operations: true,
        }
    }
}

impl Default for OtherArchConfig {
    fn default() -> Self {
        Self {
            generic_optimizations: true,
            custom_support: false,
        }
    }
}

impl Default for PowerManagementConfig {
    fn default() -> Self {
        Self {
            frequency_management: FrequencyManagementConfig::default(),
            thermal_management: ThermalManagementConfig::default(),
            power_efficiency: PowerEfficiencyConfig::default(),
            energy_aware_scheduling: false,
        }
    }
}

impl Default for FrequencyManagementConfig {
    fn default() -> Self {
        Self {
            dynamic_scaling: false,
            performance_governor: true,
            turbo_boost_awareness: false,
            scaling_strategy: FrequencyScalingStrategy::Performance,
        }
    }
}

impl Default for ThermalManagementConfig {
    fn default() -> Self {
        Self {
            thermal_monitoring: false,
            throttling_awareness: false,
            temperature_optimization: false,
            cooling_strategy: CoolingStrategy::Passive,
        }
    }
}

impl Default for PowerEfficiencyConfig {
    fn default() -> Self {
        Self {
            power_aware_scheduling: false,
            sleep_state_optimization: false,
            dynamic_voltage_scaling: false,
            efficiency_preference: EnergyEfficiencyPreference::Performance,
        }
    }
}

impl PlatformConfig {
    /// Create platform configuration optimized for the current system
    pub fn auto_detect() -> Self {
        let mut config = Self::default();

        // Enable platform-specific optimizations based on detected hardware
        match config.platform_info.architecture {
            Architecture::X86_64 => {
                config.architecture_specific.x86_config.x86_optimizations = true;
                if config.platform_info.cpu_features.x86_features.avx {
                    config.architecture_specific.x86_config.specific_instructions.use_avx = true;
                }
            }
            Architecture::AArch64 => {
                config.architecture_specific.arm_config.arm_optimizations = true;
                if config.platform_info.cpu_features.arm_features.neon {
                    config.architecture_specific.arm_config.instruction_set.use_neon = true;
                }
            }
            Architecture::RiscV64 => {
                config.architecture_specific.riscv_config.riscv_optimizations = true;
            }
            _ => {}
        }

        // Enable NUMA awareness if multiple nodes detected
        if config.platform_info.numa_topology.node_count > 1 {
            config.numa_config.numa_awareness = true;
            config.thread_affinity.enabled = true;
            config.thread_affinity.binding_strategy = CpuBindingStrategy::NumaNodes;
        }

        // Enable power management for mobile platforms
        match config.platform_info.os_type {
            OsType::MacOS => {
                config.power_management.power_efficiency.efficiency_preference =
                    EnergyEfficiencyPreference::Balance;
            }
            _ => {}
        }

        config
    }

    /// Create performance-optimized configuration
    pub fn performance_optimized() -> Self {
        let mut config = Self::auto_detect();

        // Enable all performance optimizations
        config.cpu_optimization.cpu_specific_instructions = true;
        config.cpu_optimization.branch_prediction_optimization = true;
        config.cpu_optimization.cache_friendly_codegen = true;
        config.cpu_optimization.pipeline_optimization = true;
        config.cpu_optimization.superscalar_optimization = true;
        config.cpu_optimization.out_of_order_optimization = true;

        // Aggressive memory hierarchy optimization
        config.memory_hierarchy.l1_cache_optimization.cache_blocking_factor = 8;
        config.memory_hierarchy.l2_cache_optimization.blocking_strategy =
            CacheBlockingStrategy::AdaptiveBlocking;
        config.memory_hierarchy.prefetching.software_prefetching = true;
        config.memory_hierarchy.prefetching.adaptive_prefetching = true;

        // Enable NUMA optimizations
        if config.platform_info.numa_topology.node_count > 1 {
            config.numa_config.numa_awareness = true;
            config.numa_config.memory_locality_optimization = true;
            config.numa_config.inter_node_optimization = true;
        }

        // Aggressive thread affinity
        config.thread_affinity.enabled = true;
        config.thread_affinity.binding_strategy = CpuBindingStrategy::SpecificCores;
        config.thread_affinity.core_allocation.prefer_physical_cores = true;
        config.thread_affinity.core_allocation.avoid_cache_interference = true;

        // Performance-oriented power management
        config.power_management.frequency_management.performance_governor = true;
        config.power_management.frequency_management.turbo_boost_awareness = true;
        config.power_management.power_efficiency.efficiency_preference =
            EnergyEfficiencyPreference::Performance;

        config
    }

    /// Create power-efficient configuration
    pub fn power_efficient() -> Self {
        let mut config = Self::auto_detect();

        // Conservative CPU optimizations
        config.cpu_optimization.frequency_scaling_awareness = true;
        config.cpu_optimization.thermal_throttling_awareness = true;

        // Conservative memory optimizations
        config.memory_hierarchy.prefetching.software_prefetching = false;
        config.memory_hierarchy.prefetching.hardware_prefetching = false;

        // Power-aware thread management
        config.thread_affinity.enabled = false; // Allow OS scheduling
        config.thread_affinity.load_balancing.work_stealing = false;

        // Power management optimizations
        config.power_management.frequency_management.dynamic_scaling = true;
        config.power_management.frequency_management.performance_governor = false;
        config.power_management.frequency_management.scaling_strategy =
            FrequencyScalingStrategy::Ondemand;

        config.power_management.thermal_management.thermal_monitoring = true;
        config.power_management.thermal_management.throttling_awareness = true;

        config.power_management.power_efficiency.power_aware_scheduling = true;
        config.power_management.power_efficiency.sleep_state_optimization = true;
        config.power_management.power_efficiency.dynamic_voltage_scaling = true;
        config.power_management.power_efficiency.efficiency_preference =
            EnergyEfficiencyPreference::MaxEfficiency;

        config.power_management.energy_aware_scheduling = true;

        config
    }

    /// Validate platform configuration
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Check architecture-specific settings
        match self.platform_info.architecture {
            Architecture::X86_64 | Architecture::X86 => {
                if !self.architecture_specific.x86_config.x86_optimizations
                   && (self.architecture_specific.x86_config.specific_instructions.use_avx
                       || self.architecture_specific.x86_config.specific_instructions.use_sse) {
                    return Err(WasmtimeError::EngineConfig {
                        message: "x86-specific instructions require x86 optimizations to be enabled".to_string(),
                    });
                }
            }
            Architecture::AArch64 | Architecture::AArch32 => {
                if !self.architecture_specific.arm_config.arm_optimizations
                   && self.architecture_specific.arm_config.instruction_set.use_neon {
                    return Err(WasmtimeError::EngineConfig {
                        message: "ARM-specific instructions require ARM optimizations to be enabled".to_string(),
                    });
                }
            }
            _ => {}
        }

        // Check NUMA configuration
        if self.numa_config.numa_awareness && self.platform_info.numa_topology.node_count <= 1 {
            return Err(WasmtimeError::EngineConfig {
                message: "NUMA awareness requires multiple NUMA nodes".to_string(),
            });
        }

        // Check thread affinity configuration
        if self.thread_affinity.enabled {
            match self.thread_affinity.binding_strategy {
                CpuBindingStrategy::NumaNodes => {
                    if self.platform_info.numa_topology.node_count <= 1 {
                        return Err(WasmtimeError::EngineConfig {
                            message: "NUMA node binding requires multiple NUMA nodes".to_string(),
                        });
                    }
                }
                CpuBindingStrategy::SpecificCores => {
                    if self.platform_info.cpu_cores == 0 {
                        return Err(WasmtimeError::EngineConfig {
                            message: "Specific core binding requires valid CPU core count".to_string(),
                        });
                    }
                }
                _ => {}
            }
        }

        Ok(())
    }

    /// Get configuration performance impact score (0-100)
    pub fn performance_impact(&self) -> u32 {
        let mut score = 50; // Base score

        if self.cpu_optimization.cpu_specific_instructions { score += 10; }
        if self.cpu_optimization.branch_prediction_optimization { score += 5; }
        if self.cpu_optimization.cache_friendly_codegen { score += 8; }
        if self.cpu_optimization.pipeline_optimization { score += 7; }
        if self.cpu_optimization.superscalar_optimization { score += 6; }
        if self.cpu_optimization.out_of_order_optimization { score += 5; }

        if self.memory_hierarchy.prefetching.software_prefetching { score += 5; }
        if self.memory_hierarchy.prefetching.adaptive_prefetching { score += 3; }

        if self.numa_config.numa_awareness { score += 10; }
        if self.thread_affinity.enabled { score += 8; }

        if self.power_management.frequency_management.performance_governor { score += 5; }

        score.min(100)
    }

    /// Get configuration power consumption estimate (0-100, lower is better)
    pub fn power_consumption_estimate(&self) -> u32 {
        let mut consumption = 50; // Base consumption

        if self.cpu_optimization.frequency_scaling_awareness { consumption -= 10; }
        if self.cpu_optimization.thermal_throttling_awareness { consumption -= 8; }

        if self.power_management.frequency_management.dynamic_scaling { consumption -= 15; }
        if self.power_management.thermal_management.thermal_monitoring { consumption -= 5; }
        if self.power_management.power_efficiency.power_aware_scheduling { consumption -= 10; }
        if self.power_management.power_efficiency.sleep_state_optimization { consumption -= 8; }
        if self.power_management.power_efficiency.dynamic_voltage_scaling { consumption -= 12; }

        match self.power_management.power_efficiency.efficiency_preference {
            EnergyEfficiencyPreference::MaxEfficiency => consumption -= 20,
            EnergyEfficiencyPreference::PowerSave => consumption -= 15,
            EnergyEfficiencyPreference::Balance => consumption -= 5,
            EnergyEfficiencyPreference::Performance => consumption += 10,
        }

        if self.power_management.energy_aware_scheduling { consumption -= 8; }

        consumption.max(0).min(100)
    }
}

/// Core functions for platform configuration
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::validate_ptr_not_null;

    /// Create auto-detected platform configuration
    pub fn create_platform_config() -> WasmtimeResult<Box<PlatformConfig>> {
        Ok(Box::new(PlatformConfig::auto_detect()))
    }

    /// Create performance-optimized platform configuration
    pub fn create_performance_platform_config() -> WasmtimeResult<Box<PlatformConfig>> {
        Ok(Box::new(PlatformConfig::performance_optimized()))
    }

    /// Create power-efficient platform configuration
    pub fn create_power_efficient_platform_config() -> WasmtimeResult<Box<PlatformConfig>> {
        Ok(Box::new(PlatformConfig::power_efficient()))
    }

    /// Validate platform configuration
    pub unsafe fn validate_platform_config(config_ptr: *const c_void) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "platform_config");
        let config = &*(config_ptr as *const PlatformConfig);
        config.validate()
    }

    /// Get performance impact score
    pub unsafe fn get_performance_impact(config_ptr: *const c_void) -> WasmtimeResult<u32> {
        validate_ptr_not_null!(config_ptr, "platform_config");
        let config = &*(config_ptr as *const PlatformConfig);
        Ok(config.performance_impact())
    }

    /// Get power consumption estimate
    pub unsafe fn get_power_consumption_estimate(config_ptr: *const c_void) -> WasmtimeResult<u32> {
        validate_ptr_not_null!(config_ptr, "platform_config");
        let config = &*(config_ptr as *const PlatformConfig);
        Ok(config.power_consumption_estimate())
    }

    /// Destroy platform configuration
    pub unsafe fn destroy_platform_config(config_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<PlatformConfig>(config_ptr, "PlatformConfig");
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_platform_detection() {
        let platform_info = PlatformInfo::detect();
        assert_ne!(platform_info.os_type, OsType::Other); // Should detect actual OS
        assert_ne!(platform_info.architecture, Architecture::Other); // Should detect actual arch
        assert!(platform_info.cpu_cores > 0);
        assert!(platform_info.total_memory > 0);
    }

    #[test]
    fn test_auto_detect_config() {
        let config = PlatformConfig::auto_detect();
        assert!(config.validate().is_ok());
    }

    #[test]
    fn test_performance_optimized_config() {
        let config = PlatformConfig::performance_optimized();
        assert!(config.validate().is_ok());

        assert!(config.cpu_optimization.cpu_specific_instructions);
        assert!(config.cpu_optimization.branch_prediction_optimization);
        assert!(config.memory_hierarchy.prefetching.software_prefetching);

        let performance_score = config.performance_impact();
        assert!(performance_score > 70); // Should be high performance
    }

    #[test]
    fn test_power_efficient_config() {
        let config = PlatformConfig::power_efficient();
        assert!(config.validate().is_ok());

        assert!(config.cpu_optimization.frequency_scaling_awareness);
        assert!(config.power_management.power_efficiency.power_aware_scheduling);
        assert_eq!(config.power_management.power_efficiency.efficiency_preference,
                   EnergyEfficiencyPreference::MaxEfficiency);

        let power_consumption = config.power_consumption_estimate();
        assert!(power_consumption < 30); // Should be low power consumption
    }

    #[test]
    fn test_cpu_feature_detection() {
        let features = SupportedCpuFeatures {
            x86_features: X86Features::detect(),
            arm_features: ArmFeatures::detect(),
            riscv_features: RiscVFeatures::detect(),
            generic_features: GenericFeatures::detect(),
        };

        // Should detect some generic features
        assert!(features.generic_features.atomic_operations);
        assert!(features.generic_features.floating_point);
    }

    #[test]
    fn test_configuration_validation() {
        let mut config = PlatformConfig::default();
        assert!(config.validate().is_ok());

        // Test invalid NUMA configuration
        config.numa_config.numa_awareness = true;
        config.platform_info.numa_topology.node_count = 1;
        assert!(config.validate().is_err());

        // Reset and test invalid thread affinity
        config = PlatformConfig::default();
        config.thread_affinity.enabled = true;
        config.thread_affinity.binding_strategy = CpuBindingStrategy::NumaNodes;
        config.platform_info.numa_topology.node_count = 1;
        assert!(config.validate().is_err());
    }

    #[test]
    fn test_performance_scoring() {
        let default_config = PlatformConfig::default();
        let performance_config = PlatformConfig::performance_optimized();
        let power_config = PlatformConfig::power_efficient();

        let default_score = default_config.performance_impact();
        let performance_score = performance_config.performance_impact();
        let power_score = power_config.performance_impact();

        assert!(performance_score > default_score);
        assert!(default_score >= power_score);
    }

    #[test]
    fn test_power_consumption_estimation() {
        let default_config = PlatformConfig::default();
        let performance_config = PlatformConfig::performance_optimized();
        let power_config = PlatformConfig::power_efficient();

        let default_consumption = default_config.power_consumption_estimate();
        let performance_consumption = performance_config.power_consumption_estimate();
        let power_consumption = power_config.power_consumption_estimate();

        assert!(power_consumption < default_consumption);
        assert!(default_consumption <= performance_consumption);
    }
}