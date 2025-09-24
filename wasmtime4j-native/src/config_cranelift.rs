//! Advanced Cranelift compiler configuration for wasmtime4j
//!
//! This module provides comprehensive access to Cranelift optimization flags,
//! platform-specific tuning options, and advanced code generation settings.

use wasmtime::Config;
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;

/// Advanced Cranelift configuration with all optimization flags
#[derive(Debug, Clone)]
pub struct CraneliftAdvancedConfig {
    /// Optimization level settings
    pub optimization: OptimizationSettings,
    /// Instruction scheduling configuration
    pub scheduling: InstructionSchedulingSettings,
    /// Register allocation strategy
    pub register_allocation: RegisterAllocationSettings,
    /// Loop optimization settings
    pub loop_optimization: LoopOptimizationSettings,
    /// Vectorization settings
    pub vectorization: VectorizationSettings,
    /// Memory model configuration
    pub memory_model: MemoryModelSettings,
    /// Platform-specific settings
    pub platform_specific: PlatformSpecificSettings,
    /// Debug and verification settings
    pub debug_verification: DebugVerificationSettings,
    /// Code generation settings
    pub code_generation: CodeGenerationSettings,
}

/// Optimization level and strategy settings
#[derive(Debug, Clone)]
pub struct OptimizationSettings {
    /// Enable instruction combining optimizations
    pub instruction_combining: bool,
    /// Enable constant folding
    pub constant_folding: bool,
    /// Enable dead code elimination
    pub dead_code_elimination: bool,
    /// Enable common subexpression elimination
    pub common_subexpression_elimination: bool,
    /// Enable global value numbering
    pub global_value_numbering: bool,
    /// Enable loop invariant code motion
    pub loop_invariant_code_motion: bool,
    /// Enable tail call optimization
    pub tail_call_optimization: bool,
    /// Enable inlining optimization
    pub inlining: bool,
    /// Maximum inline depth
    pub max_inline_depth: u32,
    /// Enable interprocedural optimization
    pub interprocedural_optimization: bool,
}

/// Instruction scheduling configuration
#[derive(Debug, Clone)]
pub struct InstructionSchedulingSettings {
    /// Enable instruction scheduling
    pub enabled: bool,
    /// Scheduling strategy
    pub strategy: SchedulingStrategy,
    /// Enable pre-register allocation scheduling
    pub pre_regalloc_scheduling: bool,
    /// Enable post-register allocation scheduling
    pub post_regalloc_scheduling: bool,
    /// Enable list scheduling
    pub list_scheduling: bool,
    /// Enable machine instruction scheduling
    pub machine_instruction_scheduling: bool,
    /// Scheduling window size
    pub scheduling_window_size: u32,
    /// Enable pipeline hazard avoidance
    pub pipeline_hazard_avoidance: bool,
}

/// Register allocation strategy settings
#[derive(Debug, Clone)]
pub struct RegisterAllocationSettings {
    /// Register allocation algorithm
    pub algorithm: RegisterAllocationAlgorithm,
    /// Enable register coalescing
    pub register_coalescing: bool,
    /// Enable live range splitting
    pub live_range_splitting: bool,
    /// Enable rematerialization
    pub rematerialization: bool,
    /// Enable register pressure reduction
    pub register_pressure_reduction: bool,
    /// Spill cost heuristic
    pub spill_cost_heuristic: SpillCostHeuristic,
    /// Maximum spill weight
    pub max_spill_weight: f64,
    /// Enable caller-saved register optimization
    pub caller_saved_optimization: bool,
}

/// Loop optimization settings
#[derive(Debug, Clone)]
pub struct LoopOptimizationSettings {
    /// Enable loop unrolling
    pub loop_unrolling: bool,
    /// Maximum unroll factor
    pub max_unroll_factor: u32,
    /// Enable partial loop unrolling
    pub partial_unrolling: bool,
    /// Enable loop peeling
    pub loop_peeling: bool,
    /// Enable loop rotation
    pub loop_rotation: bool,
    /// Enable loop interchange
    pub loop_interchange: bool,
    /// Enable loop vectorization
    pub loop_vectorization: bool,
    /// Enable loop strength reduction
    pub loop_strength_reduction: bool,
    /// Enable induction variable optimization
    pub induction_variable_optimization: bool,
    /// Loop nesting threshold
    pub loop_nesting_threshold: u32,
}

/// Vectorization settings
#[derive(Debug, Clone)]
pub struct VectorizationSettings {
    /// Enable auto-vectorization
    pub auto_vectorization: bool,
    /// Enable SIMD optimization
    pub simd_optimization: bool,
    /// Vectorization factor
    pub vectorization_factor: u32,
    /// Enable loop vectorization
    pub loop_vectorization: bool,
    /// Enable SLP vectorization
    pub slp_vectorization: bool,
    /// Minimum vector width
    pub min_vector_width: u32,
    /// Maximum vector width
    pub max_vector_width: u32,
    /// Enable masked vectorization
    pub masked_vectorization: bool,
    /// Vector cost model
    pub vector_cost_model: VectorCostModel,
}

/// Memory model configuration
#[derive(Debug, Clone)]
pub struct MemoryModelSettings {
    /// Memory ordering model
    pub memory_ordering: MemoryOrdering,
    /// Enable memory coalescing
    pub memory_coalescing: bool,
    /// Enable load/store optimization
    pub load_store_optimization: bool,
    /// Enable memory disambiguation
    pub memory_disambiguation: bool,
    /// Enable address space optimization
    pub address_space_optimization: bool,
    /// Cache line size hint
    pub cache_line_size: u32,
    /// Enable prefetch generation
    pub prefetch_generation: bool,
    /// Prefetch distance
    pub prefetch_distance: u32,
}

/// Platform-specific optimization settings
#[derive(Debug, Clone)]
pub struct PlatformSpecificSettings {
    /// Target architecture settings
    pub architecture: ArchitectureSettings,
    /// CPU feature optimization
    pub cpu_features: CpuFeatureSettings,
    /// Calling convention optimization
    pub calling_convention: CallingConventionSettings,
    /// OS-specific optimizations
    pub os_specific: OsSpecificSettings,
}

/// Debug and verification settings
#[derive(Debug, Clone)]
pub struct DebugVerificationSettings {
    /// Enable Cranelift debug verifier
    pub cranelift_debug_verifier: bool,
    /// Enable post-optimization verification
    pub post_optimization_verification: bool,
    /// Enable type checking
    pub type_checking: bool,
    /// Enable proof-carrying code
    pub proof_carrying_code: bool,
    /// Verification level
    pub verification_level: VerificationLevel,
    /// Enable assertion generation
    pub assertion_generation: bool,
    /// Enable invariant checking
    pub invariant_checking: bool,
}

/// Code generation settings
#[derive(Debug, Clone)]
pub struct CodeGenerationSettings {
    /// Enable machine code optimization
    pub machine_code_optimization: bool,
    /// Enable branch optimization
    pub branch_optimization: bool,
    /// Enable jump threading
    pub jump_threading: bool,
    /// Enable block layout optimization
    pub block_layout_optimization: bool,
    /// Enable constant pool optimization
    pub constant_pool_optimization: bool,
    /// Code alignment settings
    pub code_alignment: CodeAlignmentSettings,
    /// Enable profile-guided optimization
    pub profile_guided_optimization: bool,
}

/// Scheduling strategy enumeration
#[derive(Debug, Clone, Copy)]
pub enum SchedulingStrategy {
    /// No scheduling
    None,
    /// List scheduling
    List,
    /// Critical path scheduling
    CriticalPath,
    /// Resource-constrained scheduling
    ResourceConstrained,
    /// Software pipelining
    SoftwarePipelining,
}

/// Register allocation algorithm
#[derive(Debug, Clone, Copy)]
pub enum RegisterAllocationAlgorithm {
    /// Linear scan register allocation
    LinearScan,
    /// Graph coloring register allocation
    GraphColoring,
    /// Greedy register allocation
    Greedy,
    /// Live range splitting allocation
    LiveRangeSplitting,
    /// Priority-based allocation
    PriorityBased,
}

/// Spill cost heuristic
#[derive(Debug, Clone, Copy)]
pub enum SpillCostHeuristic {
    /// Simple frequency-based cost
    Frequency,
    /// Loop depth weighted cost
    LoopDepth,
    /// Register pressure based cost
    RegisterPressure,
    /// Hybrid cost model
    Hybrid,
}

/// Vector cost model
#[derive(Debug, Clone, Copy)]
pub enum VectorCostModel {
    /// Basic cost model
    Basic,
    /// Target-specific cost model
    TargetSpecific,
    /// Profile-guided cost model
    ProfileGuided,
    /// Aggressive vectorization
    Aggressive,
}

/// Memory ordering model
#[derive(Debug, Clone, Copy)]
pub enum MemoryOrdering {
    /// Relaxed memory ordering
    Relaxed,
    /// Acquire-release ordering
    AcquireRelease,
    /// Sequential consistency
    SequentialConsistency,
}

/// Architecture-specific settings
#[derive(Debug, Clone)]
pub struct ArchitectureSettings {
    /// x86-specific settings
    pub x86: X86Settings,
    /// ARM-specific settings
    pub arm: ArmSettings,
    /// RISC-V specific settings
    pub riscv: RiscVSettings,
}

/// CPU feature optimization settings
#[derive(Debug, Clone)]
pub struct CpuFeatureSettings {
    /// Enable AVX optimizations
    pub avx_optimization: bool,
    /// Enable SSE optimizations
    pub sse_optimization: bool,
    /// Enable NEON optimizations (ARM)
    pub neon_optimization: bool,
    /// Enable hardware atomic operations
    pub hardware_atomics: bool,
    /// Enable hardware AES
    pub hardware_aes: bool,
    /// Enable hardware floating point
    pub hardware_float: bool,
    /// CPU cache optimization
    pub cache_optimization: bool,
}

/// Calling convention optimization
#[derive(Debug, Clone)]
pub struct CallingConventionSettings {
    /// Enable tail call optimization
    pub tail_call_optimization: bool,
    /// Enable register parameter optimization
    pub register_parameter_optimization: bool,
    /// Enable stack parameter optimization
    pub stack_parameter_optimization: bool,
    /// Enable return value optimization
    pub return_value_optimization: bool,
    /// Use platform-specific calling conventions
    pub platform_specific_conventions: bool,
}

/// OS-specific optimization settings
#[derive(Debug, Clone)]
pub struct OsSpecificSettings {
    /// Enable Linux-specific optimizations
    pub linux_optimizations: bool,
    /// Enable Windows-specific optimizations
    pub windows_optimizations: bool,
    /// Enable macOS-specific optimizations
    pub macos_optimizations: bool,
    /// Enable NUMA awareness
    pub numa_awareness: bool,
    /// Enable huge page support
    pub huge_page_support: bool,
    /// Thread affinity optimization
    pub thread_affinity_optimization: bool,
}

/// x86-specific optimization settings
#[derive(Debug, Clone)]
pub struct X86Settings {
    /// Enable x86-64 optimizations
    pub x86_64_optimizations: bool,
    /// Enable AVX512 support
    pub avx512_support: bool,
    /// Enable BMI instruction set
    pub bmi_instructions: bool,
    /// Enable POPCNT optimization
    pub popcnt_optimization: bool,
    /// Enable LZCNT optimization
    pub lzcnt_optimization: bool,
    /// Enable branch prediction hints
    pub branch_prediction_hints: bool,
}

/// ARM-specific optimization settings
#[derive(Debug, Clone)]
pub struct ArmSettings {
    /// Enable ARM64 optimizations
    pub arm64_optimizations: bool,
    /// Enable NEON SIMD
    pub neon_simd: bool,
    /// Enable crypto extensions
    pub crypto_extensions: bool,
    /// Enable SVE (Scalable Vector Extensions)
    pub sve_support: bool,
    /// ARM-specific scheduling
    pub arm_scheduling: bool,
}

/// RISC-V specific optimization settings
#[derive(Debug, Clone)]
pub struct RiscVSettings {
    /// Enable RV64 optimizations
    pub rv64_optimizations: bool,
    /// Enable vector extensions
    pub vector_extensions: bool,
    /// Enable compressed instructions
    pub compressed_instructions: bool,
    /// Enable atomic extensions
    pub atomic_extensions: bool,
    /// Enable floating point extensions
    pub floating_point_extensions: bool,
}

/// Verification level settings
#[derive(Debug, Clone, Copy)]
pub enum VerificationLevel {
    /// No verification
    None,
    /// Basic verification
    Basic,
    /// Comprehensive verification
    Comprehensive,
    /// Exhaustive verification
    Exhaustive,
}

/// Code alignment settings
#[derive(Debug, Clone)]
pub struct CodeAlignmentSettings {
    /// Function alignment
    pub function_alignment: u32,
    /// Loop alignment
    pub loop_alignment: u32,
    /// Jump target alignment
    pub jump_target_alignment: u32,
    /// Basic block alignment
    pub basic_block_alignment: u32,
}

impl Default for CraneliftAdvancedConfig {
    fn default() -> Self {
        Self {
            optimization: OptimizationSettings::default(),
            scheduling: InstructionSchedulingSettings::default(),
            register_allocation: RegisterAllocationSettings::default(),
            loop_optimization: LoopOptimizationSettings::default(),
            vectorization: VectorizationSettings::default(),
            memory_model: MemoryModelSettings::default(),
            platform_specific: PlatformSpecificSettings::default(),
            debug_verification: DebugVerificationSettings::default(),
            code_generation: CodeGenerationSettings::default(),
        }
    }
}

impl Default for OptimizationSettings {
    fn default() -> Self {
        Self {
            instruction_combining: true,
            constant_folding: true,
            dead_code_elimination: true,
            common_subexpression_elimination: true,
            global_value_numbering: true,
            loop_invariant_code_motion: true,
            tail_call_optimization: true,
            inlining: true,
            max_inline_depth: 4,
            interprocedural_optimization: false,
        }
    }
}

impl Default for InstructionSchedulingSettings {
    fn default() -> Self {
        Self {
            enabled: true,
            strategy: SchedulingStrategy::List,
            pre_regalloc_scheduling: true,
            post_regalloc_scheduling: true,
            list_scheduling: true,
            machine_instruction_scheduling: true,
            scheduling_window_size: 32,
            pipeline_hazard_avoidance: true,
        }
    }
}

impl Default for RegisterAllocationSettings {
    fn default() -> Self {
        Self {
            algorithm: RegisterAllocationAlgorithm::LinearScan,
            register_coalescing: true,
            live_range_splitting: true,
            rematerialization: true,
            register_pressure_reduction: true,
            spill_cost_heuristic: SpillCostHeuristic::Hybrid,
            max_spill_weight: 1000.0,
            caller_saved_optimization: true,
        }
    }
}

impl Default for LoopOptimizationSettings {
    fn default() -> Self {
        Self {
            loop_unrolling: true,
            max_unroll_factor: 8,
            partial_unrolling: true,
            loop_peeling: true,
            loop_rotation: true,
            loop_interchange: false,
            loop_vectorization: true,
            loop_strength_reduction: true,
            induction_variable_optimization: true,
            loop_nesting_threshold: 3,
        }
    }
}

impl Default for VectorizationSettings {
    fn default() -> Self {
        Self {
            auto_vectorization: true,
            simd_optimization: true,
            vectorization_factor: 4,
            loop_vectorization: true,
            slp_vectorization: true,
            min_vector_width: 128,
            max_vector_width: 512,
            masked_vectorization: false,
            vector_cost_model: VectorCostModel::TargetSpecific,
        }
    }
}

impl Default for MemoryModelSettings {
    fn default() -> Self {
        Self {
            memory_ordering: MemoryOrdering::AcquireRelease,
            memory_coalescing: true,
            load_store_optimization: true,
            memory_disambiguation: true,
            address_space_optimization: true,
            cache_line_size: 64,
            prefetch_generation: false,
            prefetch_distance: 32,
        }
    }
}

impl Default for PlatformSpecificSettings {
    fn default() -> Self {
        Self {
            architecture: ArchitectureSettings::default(),
            cpu_features: CpuFeatureSettings::default(),
            calling_convention: CallingConventionSettings::default(),
            os_specific: OsSpecificSettings::default(),
        }
    }
}

impl Default for DebugVerificationSettings {
    fn default() -> Self {
        Self {
            cranelift_debug_verifier: false,
            post_optimization_verification: false,
            type_checking: true,
            proof_carrying_code: false,
            verification_level: VerificationLevel::Basic,
            assertion_generation: false,
            invariant_checking: false,
        }
    }
}

impl Default for CodeGenerationSettings {
    fn default() -> Self {
        Self {
            machine_code_optimization: true,
            branch_optimization: true,
            jump_threading: true,
            block_layout_optimization: true,
            constant_pool_optimization: true,
            code_alignment: CodeAlignmentSettings::default(),
            profile_guided_optimization: false,
        }
    }
}

impl Default for ArchitectureSettings {
    fn default() -> Self {
        Self {
            x86: X86Settings::default(),
            arm: ArmSettings::default(),
            riscv: RiscVSettings::default(),
        }
    }
}

impl Default for CpuFeatureSettings {
    fn default() -> Self {
        Self {
            avx_optimization: true,
            sse_optimization: true,
            neon_optimization: true,
            hardware_atomics: true,
            hardware_aes: false,
            hardware_float: true,
            cache_optimization: true,
        }
    }
}

impl Default for CallingConventionSettings {
    fn default() -> Self {
        Self {
            tail_call_optimization: true,
            register_parameter_optimization: true,
            stack_parameter_optimization: true,
            return_value_optimization: true,
            platform_specific_conventions: true,
        }
    }
}

impl Default for OsSpecificSettings {
    fn default() -> Self {
        Self {
            linux_optimizations: cfg!(target_os = "linux"),
            windows_optimizations: cfg!(target_os = "windows"),
            macos_optimizations: cfg!(target_os = "macos"),
            numa_awareness: false,
            huge_page_support: false,
            thread_affinity_optimization: false,
        }
    }
}

impl Default for X86Settings {
    fn default() -> Self {
        Self {
            x86_64_optimizations: cfg!(target_arch = "x86_64"),
            avx512_support: false,
            bmi_instructions: true,
            popcnt_optimization: true,
            lzcnt_optimization: true,
            branch_prediction_hints: true,
        }
    }
}

impl Default for ArmSettings {
    fn default() -> Self {
        Self {
            arm64_optimizations: cfg!(target_arch = "aarch64"),
            neon_simd: true,
            crypto_extensions: false,
            sve_support: false,
            arm_scheduling: true,
        }
    }
}

impl Default for RiscVSettings {
    fn default() -> Self {
        Self {
            rv64_optimizations: cfg!(target_arch = "riscv64"),
            vector_extensions: false,
            compressed_instructions: true,
            atomic_extensions: true,
            floating_point_extensions: true,
        }
    }
}

impl Default for CodeAlignmentSettings {
    fn default() -> Self {
        Self {
            function_alignment: 16,
            loop_alignment: 16,
            jump_target_alignment: 16,
            basic_block_alignment: 8,
        }
    }
}

impl CraneliftAdvancedConfig {
    /// Create new configuration with performance-optimized defaults
    pub fn performance_optimized() -> Self {
        let mut config = Self::default();

        // Enable aggressive optimizations
        config.optimization.interprocedural_optimization = true;
        config.optimization.max_inline_depth = 8;

        // Enable advanced scheduling
        config.scheduling.strategy = SchedulingStrategy::CriticalPath;
        config.scheduling.scheduling_window_size = 64;

        // Use advanced register allocation
        config.register_allocation.algorithm = RegisterAllocationAlgorithm::GraphColoring;

        // Aggressive loop optimizations
        config.loop_optimization.max_unroll_factor = 16;
        config.loop_optimization.loop_interchange = true;

        // Enable vectorization
        config.vectorization.max_vector_width = 512;
        config.vectorization.masked_vectorization = true;
        config.vectorization.vector_cost_model = VectorCostModel::Aggressive;

        // Enable prefetching
        config.memory_model.prefetch_generation = true;
        config.memory_model.prefetch_distance = 64;

        // Enable profile-guided optimization
        config.code_generation.profile_guided_optimization = true;

        config
    }

    /// Create new configuration optimized for code size
    pub fn size_optimized() -> Self {
        let mut config = Self::default();

        // Disable aggressive optimizations that increase code size
        config.optimization.inlining = false;
        config.optimization.interprocedural_optimization = false;

        // Conservative scheduling
        config.scheduling.scheduling_window_size = 8;

        // Reduce loop unrolling
        config.loop_optimization.loop_unrolling = false;
        config.loop_optimization.partial_unrolling = false;
        config.loop_optimization.loop_peeling = false;

        // Conservative vectorization
        config.vectorization.auto_vectorization = false;
        config.vectorization.loop_vectorization = false;
        config.vectorization.slp_vectorization = false;

        // Tighter code alignment
        config.code_generation.code_alignment.function_alignment = 8;
        config.code_generation.code_alignment.loop_alignment = 8;
        config.code_generation.code_alignment.jump_target_alignment = 8;
        config.code_generation.code_alignment.basic_block_alignment = 4;

        config
    }

    /// Create new configuration for debugging
    pub fn debug_optimized() -> Self {
        let mut config = Self::default();

        // Disable most optimizations for debugging
        config.optimization.instruction_combining = false;
        config.optimization.constant_folding = false;
        config.optimization.dead_code_elimination = false;
        config.optimization.common_subexpression_elimination = false;
        config.optimization.global_value_numbering = false;
        config.optimization.loop_invariant_code_motion = false;
        config.optimization.tail_call_optimization = false;
        config.optimization.inlining = false;

        // Disable scheduling for predictable debugging
        config.scheduling.enabled = false;

        // Disable loop optimizations
        config.loop_optimization.loop_unrolling = false;
        config.loop_optimization.loop_vectorization = false;

        // Disable vectorization
        config.vectorization.auto_vectorization = false;
        config.vectorization.simd_optimization = false;

        // Enable debug verification
        config.debug_verification.cranelift_debug_verifier = true;
        config.debug_verification.post_optimization_verification = true;
        config.debug_verification.verification_level = VerificationLevel::Comprehensive;
        config.debug_verification.assertion_generation = true;
        config.debug_verification.invariant_checking = true;

        config
    }

    /// Apply configuration to Wasmtime Config
    pub fn apply_to_config(&self, config: &mut Config) -> WasmtimeResult<()> {
        // Apply Cranelift settings through string settings
        let mut settings = HashMap::new();

        // Convert our settings to Cranelift string settings
        self.to_cranelift_settings(&mut settings)?;

        // Apply settings to config
        for (key, value) in settings {
            config.cranelift_flag_set(&key, &value)
                .map_err(|e| WasmtimeError::EngineConfig {
                    message: format!("Failed to set Cranelift flag {}: {}", key, e),
                })?;
        }

        Ok(())
    }

    /// Convert to Cranelift string settings
    fn to_cranelift_settings(&self, settings: &mut HashMap<String, String>) -> WasmtimeResult<()> {
        // Optimization settings
        settings.insert("opt_level".to_string(), if self.optimization.interprocedural_optimization { "speed_and_size" } else { "speed" }.to_string());
        settings.insert("enable_verifier".to_string(), self.debug_verification.cranelift_debug_verifier.to_string());

        // Instruction scheduling
        if !self.scheduling.enabled {
            settings.insert("enable_instruction_scheduling".to_string(), "false".to_string());
        }

        // Register allocation
        match self.register_allocation.algorithm {
            RegisterAllocationAlgorithm::LinearScan => {
                settings.insert("regalloc".to_string(), "linear_scan".to_string());
            }
            RegisterAllocationAlgorithm::GraphColoring => {
                settings.insert("regalloc".to_string(), "graph_coloring".to_string());
            }
            _ => {
                // Use default for other algorithms
            }
        }

        // Loop optimizations
        settings.insert("enable_loop_unrolling".to_string(), self.loop_optimization.loop_unrolling.to_string());

        // Memory model
        match self.memory_model.memory_ordering {
            MemoryOrdering::Relaxed => {
                settings.insert("memory_model".to_string(), "relaxed".to_string());
            }
            MemoryOrdering::AcquireRelease => {
                settings.insert("memory_model".to_string(), "acquire_release".to_string());
            }
            MemoryOrdering::SequentialConsistency => {
                settings.insert("memory_model".to_string(), "sequential".to_string());
            }
        }

        // Platform-specific settings
        if self.platform_specific.cpu_features.avx_optimization {
            settings.insert("enable_avx".to_string(), "true".to_string());
        }

        if self.platform_specific.cpu_features.sse_optimization {
            settings.insert("enable_sse".to_string(), "true".to_string());
        }

        // Code generation
        settings.insert("enable_branch_optimization".to_string(), self.code_generation.branch_optimization.to_string());
        settings.insert("enable_jump_threading".to_string(), self.code_generation.jump_threading.to_string());

        Ok(())
    }

    /// Validate configuration for compatibility
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Check for conflicting settings
        if !self.optimization.inlining && self.optimization.interprocedural_optimization {
            return Err(WasmtimeError::EngineConfig {
                message: "Interprocedural optimization requires inlining to be enabled".to_string(),
            });
        }

        if self.vectorization.max_vector_width < self.vectorization.min_vector_width {
            return Err(WasmtimeError::EngineConfig {
                message: "Maximum vector width cannot be less than minimum vector width".to_string(),
            });
        }

        if self.loop_optimization.max_unroll_factor == 0 {
            return Err(WasmtimeError::EngineConfig {
                message: "Maximum unroll factor must be greater than zero".to_string(),
            });
        }

        // Check platform compatibility
        #[cfg(not(target_arch = "x86_64"))]
        if self.platform_specific.cpu_features.avx_optimization {
            return Err(WasmtimeError::EngineConfig {
                message: "AVX optimization is only available on x86_64 platforms".to_string(),
            });
        }

        #[cfg(not(target_arch = "aarch64"))]
        if self.platform_specific.cpu_features.neon_optimization {
            return Err(WasmtimeError::EngineConfig {
                message: "NEON optimization is only available on ARM64 platforms".to_string(),
            });
        }

        Ok(())
    }

    /// Get configuration performance score (0-100)
    pub fn performance_score(&self) -> u32 {
        let mut score = 50; // Base score

        // Add points for optimizations
        if self.optimization.interprocedural_optimization { score += 10; }
        if self.optimization.inlining { score += 5; }
        if self.scheduling.enabled { score += 5; }
        if self.loop_optimization.loop_unrolling { score += 5; }
        if self.vectorization.auto_vectorization { score += 10; }
        if self.memory_model.prefetch_generation { score += 5; }
        if self.code_generation.profile_guided_optimization { score += 10; }

        // Subtract points for debug features
        if self.debug_verification.cranelift_debug_verifier { score -= 10; }
        if self.debug_verification.post_optimization_verification { score -= 5; }

        score.min(100)
    }

    /// Get configuration code size impact (0-100, lower is better)
    pub fn code_size_impact(&self) -> u32 {
        let mut impact = 30; // Base impact

        // Add impact for size-increasing optimizations
        if self.optimization.inlining {
            impact += 10 + (self.optimization.max_inline_depth * 2);
        }
        if self.optimization.interprocedural_optimization { impact += 15; }
        if self.loop_optimization.loop_unrolling {
            impact += 5 + (self.loop_optimization.max_unroll_factor / 2);
        }
        if self.vectorization.auto_vectorization { impact += 10; }

        impact.min(100)
    }
}

/// Shared core functions for Cranelift configuration
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::validate_ptr_not_null;

    /// Create default advanced Cranelift configuration
    pub fn create_cranelift_config() -> WasmtimeResult<Box<CraneliftAdvancedConfig>> {
        Ok(Box::new(CraneliftAdvancedConfig::default()))
    }

    /// Create performance-optimized configuration
    pub fn create_performance_config() -> WasmtimeResult<Box<CraneliftAdvancedConfig>> {
        Ok(Box::new(CraneliftAdvancedConfig::performance_optimized()))
    }

    /// Create size-optimized configuration
    pub fn create_size_config() -> WasmtimeResult<Box<CraneliftAdvancedConfig>> {
        Ok(Box::new(CraneliftAdvancedConfig::size_optimized()))
    }

    /// Create debug-optimized configuration
    pub fn create_debug_config() -> WasmtimeResult<Box<CraneliftAdvancedConfig>> {
        Ok(Box::new(CraneliftAdvancedConfig::debug_optimized()))
    }

    /// Apply configuration to Wasmtime Config
    pub unsafe fn apply_cranelift_config(
        config_ptr: *const c_void,
        wasmtime_config: &mut Config,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "cranelift_config");
        let config = &*(config_ptr as *const CraneliftAdvancedConfig);
        config.apply_to_config(wasmtime_config)
    }

    /// Validate Cranelift configuration
    pub unsafe fn validate_cranelift_config(config_ptr: *const c_void) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "cranelift_config");
        let config = &*(config_ptr as *const CraneliftAdvancedConfig);
        config.validate()
    }

    /// Get performance score for configuration
    pub unsafe fn get_performance_score(config_ptr: *const c_void) -> WasmtimeResult<u32> {
        validate_ptr_not_null!(config_ptr, "cranelift_config");
        let config = &*(config_ptr as *const CraneliftAdvancedConfig);
        Ok(config.performance_score())
    }

    /// Get code size impact for configuration
    pub unsafe fn get_code_size_impact(config_ptr: *const c_void) -> WasmtimeResult<u32> {
        validate_ptr_not_null!(config_ptr, "cranelift_config");
        let config = &*(config_ptr as *const CraneliftAdvancedConfig);
        Ok(config.code_size_impact())
    }

    /// Destroy Cranelift configuration
    pub unsafe fn destroy_cranelift_config(config_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<CraneliftAdvancedConfig>(config_ptr, "CraneliftAdvancedConfig");
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_config_creation() {
        let config = CraneliftAdvancedConfig::default();
        assert!(config.optimization.instruction_combining);
        assert!(config.scheduling.enabled);
        assert_eq!(config.register_allocation.algorithm, RegisterAllocationAlgorithm::LinearScan);
    }

    #[test]
    fn test_performance_optimized_config() {
        let config = CraneliftAdvancedConfig::performance_optimized();
        assert!(config.optimization.interprocedural_optimization);
        assert_eq!(config.scheduling.strategy, SchedulingStrategy::CriticalPath);
        assert_eq!(config.register_allocation.algorithm, RegisterAllocationAlgorithm::GraphColoring);
        assert_eq!(config.loop_optimization.max_unroll_factor, 16);
    }

    #[test]
    fn test_size_optimized_config() {
        let config = CraneliftAdvancedConfig::size_optimized();
        assert!(!config.optimization.inlining);
        assert!(!config.loop_optimization.loop_unrolling);
        assert!(!config.vectorization.auto_vectorization);
        assert_eq!(config.code_generation.code_alignment.function_alignment, 8);
    }

    #[test]
    fn test_debug_optimized_config() {
        let config = CraneliftAdvancedConfig::debug_optimized();
        assert!(!config.optimization.instruction_combining);
        assert!(!config.scheduling.enabled);
        assert!(config.debug_verification.cranelift_debug_verifier);
        assert_eq!(config.debug_verification.verification_level, VerificationLevel::Comprehensive);
    }

    #[test]
    fn test_configuration_validation() {
        let mut config = CraneliftAdvancedConfig::default();
        assert!(config.validate().is_ok());

        // Test invalid configuration
        config.optimization.inlining = false;
        config.optimization.interprocedural_optimization = true;
        assert!(config.validate().is_err());

        // Test invalid vector widths
        config = CraneliftAdvancedConfig::default();
        config.vectorization.max_vector_width = 64;
        config.vectorization.min_vector_width = 128;
        assert!(config.validate().is_err());
    }

    #[test]
    fn test_performance_scoring() {
        let default_config = CraneliftAdvancedConfig::default();
        let performance_config = CraneliftAdvancedConfig::performance_optimized();
        let debug_config = CraneliftAdvancedConfig::debug_optimized();

        let default_score = default_config.performance_score();
        let performance_score = performance_config.performance_score();
        let debug_score = debug_config.performance_score();

        assert!(performance_score > default_score);
        assert!(default_score > debug_score);
        assert!(performance_score <= 100);
        assert!(debug_score >= 0);
    }

    #[test]
    fn test_code_size_impact() {
        let size_config = CraneliftAdvancedConfig::size_optimized();
        let performance_config = CraneliftAdvancedConfig::performance_optimized();

        let size_impact = size_config.code_size_impact();
        let performance_impact = performance_config.code_size_impact();

        assert!(size_impact < performance_impact);
        assert!(size_impact <= 100);
        assert!(performance_impact <= 100);
    }

    #[test]
    fn test_cranelift_settings_conversion() {
        let config = CraneliftAdvancedConfig::performance_optimized();
        let mut settings = HashMap::new();

        assert!(config.to_cranelift_settings(&mut settings).is_ok());
        assert!(!settings.is_empty());
        assert!(settings.contains_key("opt_level"));
        assert!(settings.contains_key("enable_verifier"));
    }

    #[test]
    fn test_wasmtime_config_application() {
        let config = CraneliftAdvancedConfig::default();
        let mut wasmtime_config = Config::new();

        assert!(config.apply_to_config(&mut wasmtime_config).is_ok());
    }
}