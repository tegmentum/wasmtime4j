//! Experimental WebAssembly features for wasmtime4j
//!
//! This module provides support for cutting-edge WebAssembly proposals that are currently
//! in committee stage. These features are experimental and subject to change.
//!
//! WARNING: These features are unstable and should only be used for testing and development.

use wasmtime::Config;
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::os::raw::{c_void, c_int, c_char};
use std::ffi::CStr;

/// Configuration for experimental WebAssembly features
#[derive(Debug, Clone)]
pub struct ExperimentalFeaturesConfig {
    /// Stack switching proposal configuration
    pub stack_switching: StackSwitchingConfig,
    /// Call/CC (call-with-current-continuation) configuration
    pub call_cc: CallCcConfig,
    /// Extended constant expressions configuration
    pub extended_const_expressions: ExtendedConstExpressionsConfig,
    /// Memory64 extended operations configuration
    pub memory64_extended: Memory64ExtendedConfig,
    /// Custom page sizes configuration
    pub custom_page_sizes: CustomPageSizesConfig,
    /// Shared-everything threads configuration
    pub shared_everything_threads: SharedEverythingThreadsConfig,
    /// Type imports configuration
    pub type_imports: TypeImportsConfig,
    /// String imports configuration
    pub string_imports: StringImportsConfig,
    /// Resource types configuration
    pub resource_types: ResourceTypesConfig,
    /// Interface types configuration
    pub interface_types: InterfaceTypesConfig,
    /// Flexible vectors configuration
    pub flexible_vectors: FlexibleVectorsConfig,
}

/// Stack switching proposal configuration for coroutines and fibers
#[derive(Debug, Clone)]
pub struct StackSwitchingConfig {
    /// Enable stack switching support
    pub enabled: bool,
    /// Stack size for switched stacks (in bytes)
    pub stack_size: u64,
    /// Maximum number of concurrent stacks
    pub max_concurrent_stacks: u32,
    /// Stack switching strategy
    pub switching_strategy: StackSwitchingStrategy,
    /// Coroutine support configuration
    pub coroutine_support: CoroutineSupportConfig,
    /// Fiber support configuration
    pub fiber_support: FiberSupportConfig,
    /// Stack state management
    pub state_management: StackStateManagement,
}

/// Call/CC (call-with-current-continuation) configuration
#[derive(Debug, Clone)]
pub struct CallCcConfig {
    /// Enable call/cc support
    pub enabled: bool,
    /// Continuation capture strategy
    pub capture_strategy: ContinuationCaptureStrategy,
    /// Continuation storage management
    pub storage_management: ContinuationStorageConfig,
    /// Performance optimization settings
    pub optimization: CallCcOptimizationConfig,
    /// Integration with exception handling
    pub exception_integration: bool,
}

/// Extended constant expressions configuration
#[derive(Debug, Clone)]
pub struct ExtendedConstExpressionsConfig {
    /// Enable extended constant expressions
    pub enabled: bool,
    /// Allow import-based constant expressions
    pub import_based_expressions: bool,
    /// Support for global dependencies in constants
    pub global_dependencies: bool,
    /// Compile-time constant folding level
    pub constant_folding_level: ConstantFoldingLevel,
    /// Complex expression evaluation settings
    pub complex_evaluation: ComplexEvaluationConfig,
}

/// Memory64 extended operations configuration
#[derive(Debug, Clone)]
pub struct Memory64ExtendedConfig {
    /// Enable memory64 extended operations
    pub enabled: bool,
    /// Large address space optimizations
    pub large_address_optimizations: bool,
    /// Cross-memory addressing modes
    pub cross_memory_addressing: bool,
    /// Platform-specific 64-bit optimizations
    pub platform_optimizations: PlatformOptimizationsConfig,
    /// Memory mapping strategies
    pub memory_mapping: Memory64MappingConfig,
}

/// Custom page sizes configuration
#[derive(Debug, Clone)]
pub struct CustomPageSizesConfig {
    /// Enable custom page sizes
    pub enabled: bool,
    /// Supported page sizes (in bytes)
    pub supported_page_sizes: Vec<u32>,
    /// Default page size strategy
    pub default_strategy: PageSizeStrategy,
    /// Memory alignment requirements
    pub alignment_requirements: AlignmentRequirements,
}

/// Shared-everything threads configuration
#[derive(Debug, Clone)]
pub struct SharedEverythingThreadsConfig {
    /// Enable shared-everything threads
    pub enabled: bool,
    /// Thread pool configuration
    pub thread_pool: SharedThreadPoolConfig,
    /// Shared state management
    pub shared_state: SharedStateConfig,
    /// Synchronization primitives
    pub synchronization: SynchronizationConfig,
    /// Thread safety mechanisms
    pub thread_safety: ThreadSafetyConfig,
}

/// Type imports configuration
#[derive(Debug, Clone)]
pub struct TypeImportsConfig {
    /// Enable type imports
    pub enabled: bool,
    /// Type validation strategy
    pub validation_strategy: TypeValidationStrategy,
    /// Import resolution mechanism
    pub resolution_mechanism: ImportResolutionMechanism,
    /// Type compatibility checking
    pub compatibility_checking: CompatibilityCheckingConfig,
}

/// String imports configuration
#[derive(Debug, Clone)]
pub struct StringImportsConfig {
    /// Enable string imports
    pub enabled: bool,
    /// String encoding format
    pub encoding_format: StringEncodingFormat,
    /// Import optimization settings
    pub optimization: StringImportOptimizationConfig,
    /// Interop with JavaScript strings
    pub js_interop: bool,
}

/// Resource types configuration
#[derive(Debug, Clone)]
pub struct ResourceTypesConfig {
    /// Enable resource types
    pub enabled: bool,
    /// Resource lifetime management
    pub lifetime_management: ResourceLifetimeConfig,
    /// Ownership tracking
    pub ownership_tracking: OwnershipTrackingConfig,
    /// Resource cleanup strategies
    pub cleanup_strategies: ResourceCleanupConfig,
}

/// Interface types configuration
#[derive(Debug, Clone)]
pub struct InterfaceTypesConfig {
    /// Enable interface types
    pub enabled: bool,
    /// Interface validation level
    pub validation_level: InterfaceValidationLevel,
    /// Type adapter generation
    pub adapter_generation: AdapterGenerationConfig,
    /// Cross-language interop
    pub cross_language_interop: CrossLanguageInteropConfig,
}

/// Flexible vectors configuration
#[derive(Debug, Clone)]
pub struct FlexibleVectorsConfig {
    /// Enable flexible vectors
    pub enabled: bool,
    /// Dynamic vector sizing
    pub dynamic_sizing: bool,
    /// Vector operation optimization
    pub operation_optimization: VectorOptimizationConfig,
    /// SIMD integration
    pub simd_integration: SimdIntegrationConfig,
}

// Strategy enums and configurations

#[derive(Debug, Clone, Copy)]
pub enum StackSwitchingStrategy {
    Cooperative,
    Preemptive,
    Hybrid,
}

#[derive(Debug, Clone)]
pub struct CoroutineSupportConfig {
    pub enabled: bool,
    pub max_coroutines: u32,
    pub default_stack_size: u64,
}

#[derive(Debug, Clone)]
pub struct FiberSupportConfig {
    pub enabled: bool,
    pub fiber_pool_size: u32,
    pub fiber_stack_size: u64,
}

#[derive(Debug, Clone)]
pub struct StackStateManagement {
    pub persistence: bool,
    pub compression: bool,
    pub cleanup_strategy: StackCleanupStrategy,
}

#[derive(Debug, Clone, Copy)]
pub enum StackCleanupStrategy {
    Immediate,
    Lazy,
    Pooled,
}

#[derive(Debug, Clone, Copy)]
pub enum ContinuationCaptureStrategy {
    Full,
    Partial,
    Optimized,
}

#[derive(Debug, Clone)]
pub struct ContinuationStorageConfig {
    pub storage_strategy: ContinuationStorageStrategy,
    pub max_continuations: u32,
    pub compression_enabled: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum ContinuationStorageStrategy {
    Stack,
    Heap,
    Hybrid,
}

#[derive(Debug, Clone)]
pub struct CallCcOptimizationConfig {
    pub inline_continuations: bool,
    pub escape_analysis: bool,
    pub tail_call_optimization: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum ConstantFoldingLevel {
    None,
    Basic,
    Aggressive,
    Full,
}

#[derive(Debug, Clone)]
pub struct ComplexEvaluationConfig {
    pub max_complexity: u32,
    pub timeout_ms: u64,
    pub enable_caching: bool,
}

#[derive(Debug, Clone)]
pub struct PlatformOptimizationsConfig {
    pub x86_64_optimizations: bool,
    pub arm64_optimizations: bool,
    pub custom_optimizations: HashMap<String, bool>,
}

#[derive(Debug, Clone)]
pub struct Memory64MappingConfig {
    pub virtual_mapping: bool,
    pub lazy_allocation: bool,
    pub huge_pages: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum PageSizeStrategy {
    System,
    Optimal,
    Custom(u32),
}

#[derive(Debug, Clone)]
pub struct AlignmentRequirements {
    pub min_alignment: u32,
    pub preferred_alignment: u32,
    pub strict_alignment: bool,
}

#[derive(Debug, Clone)]
pub struct SharedThreadPoolConfig {
    pub min_threads: u32,
    pub max_threads: u32,
    pub thread_affinity: bool,
}

#[derive(Debug, Clone)]
pub struct SharedStateConfig {
    pub global_state_sharing: bool,
    pub memory_sharing: bool,
    pub table_sharing: bool,
}

#[derive(Debug, Clone)]
pub struct SynchronizationConfig {
    pub atomic_operations: bool,
    pub mutex_support: bool,
    pub condition_variables: bool,
}

#[derive(Debug, Clone)]
pub struct ThreadSafetyConfig {
    pub race_detection: bool,
    pub deadlock_detection: bool,
    pub memory_ordering: MemoryOrderingConfig,
}

#[derive(Debug, Clone)]
pub struct MemoryOrderingConfig {
    pub default_ordering: MemoryOrdering,
    pub relaxed_operations: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum MemoryOrdering {
    Relaxed,
    Acquire,
    Release,
    AcquireRelease,
    SequentiallyConsistent,
}

#[derive(Debug, Clone, Copy)]
pub enum TypeValidationStrategy {
    Strict,
    Relaxed,
    Dynamic,
}

#[derive(Debug, Clone, Copy)]
pub enum ImportResolutionMechanism {
    Static,
    Dynamic,
    Lazy,
}

#[derive(Debug, Clone)]
pub struct CompatibilityCheckingConfig {
    pub structural_compatibility: bool,
    pub nominal_compatibility: bool,
    pub version_compatibility: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum StringEncodingFormat {
    Utf8,
    Utf16,
    Latin1,
    Custom,
}

#[derive(Debug, Clone)]
pub struct StringImportOptimizationConfig {
    pub string_interning: bool,
    pub lazy_decoding: bool,
    pub compression: bool,
}

#[derive(Debug, Clone)]
pub struct ResourceLifetimeConfig {
    pub automatic_cleanup: bool,
    pub reference_counting: bool,
    pub weak_references: bool,
}

#[derive(Debug, Clone)]
pub struct OwnershipTrackingConfig {
    pub strict_ownership: bool,
    pub borrow_checking: bool,
    pub lifetime_analysis: bool,
}

#[derive(Debug, Clone)]
pub struct ResourceCleanupConfig {
    pub cleanup_strategy: ResourceCleanupStrategy,
    pub finalizer_support: bool,
    pub gc_integration: bool,
}

#[derive(Debug, Clone, Copy)]
pub enum ResourceCleanupStrategy {
    Manual,
    Automatic,
    Hybrid,
}

#[derive(Debug, Clone, Copy)]
pub enum InterfaceValidationLevel {
    Minimal,
    Standard,
    Strict,
}

#[derive(Debug, Clone)]
pub struct AdapterGenerationConfig {
    pub automatic_generation: bool,
    pub optimization_level: u32,
    pub caching_enabled: bool,
}

#[derive(Debug, Clone)]
pub struct CrossLanguageInteropConfig {
    pub c_interop: bool,
    pub rust_interop: bool,
    pub javascript_interop: bool,
    pub custom_language_support: HashMap<String, bool>,
}

#[derive(Debug, Clone)]
pub struct VectorOptimizationConfig {
    pub auto_vectorization: bool,
    pub unroll_loops: bool,
    pub optimize_for_target: bool,
}

#[derive(Debug, Clone)]
pub struct SimdIntegrationConfig {
    pub simd_fallback: bool,
    pub platform_specific_simd: bool,
    pub dynamic_simd_selection: bool,
}

impl Default for ExperimentalFeaturesConfig {
    fn default() -> Self {
        Self {
            stack_switching: StackSwitchingConfig::default(),
            call_cc: CallCcConfig::default(),
            extended_const_expressions: ExtendedConstExpressionsConfig::default(),
            memory64_extended: Memory64ExtendedConfig::default(),
            custom_page_sizes: CustomPageSizesConfig::default(),
            shared_everything_threads: SharedEverythingThreadsConfig::default(),
            type_imports: TypeImportsConfig::default(),
            string_imports: StringImportsConfig::default(),
            resource_types: ResourceTypesConfig::default(),
            interface_types: InterfaceTypesConfig::default(),
            flexible_vectors: FlexibleVectorsConfig::default(),
        }
    }
}

impl Default for StackSwitchingConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            stack_size: 1024 * 1024, // 1MB
            max_concurrent_stacks: 100,
            switching_strategy: StackSwitchingStrategy::Cooperative,
            coroutine_support: CoroutineSupportConfig::default(),
            fiber_support: FiberSupportConfig::default(),
            state_management: StackStateManagement::default(),
        }
    }
}

impl Default for CoroutineSupportConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            max_coroutines: 1000,
            default_stack_size: 64 * 1024, // 64KB
        }
    }
}

impl Default for FiberSupportConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            fiber_pool_size: 100,
            fiber_stack_size: 64 * 1024, // 64KB
        }
    }
}

impl Default for StackStateManagement {
    fn default() -> Self {
        Self {
            persistence: false,
            compression: false,
            cleanup_strategy: StackCleanupStrategy::Lazy,
        }
    }
}

impl Default for CallCcConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            capture_strategy: ContinuationCaptureStrategy::Optimized,
            storage_management: ContinuationStorageConfig::default(),
            optimization: CallCcOptimizationConfig::default(),
            exception_integration: false,
        }
    }
}

impl Default for ContinuationStorageConfig {
    fn default() -> Self {
        Self {
            storage_strategy: ContinuationStorageStrategy::Hybrid,
            max_continuations: 1000,
            compression_enabled: false,
        }
    }
}

impl Default for CallCcOptimizationConfig {
    fn default() -> Self {
        Self {
            inline_continuations: true,
            escape_analysis: true,
            tail_call_optimization: true,
        }
    }
}

// Additional default implementations for all other configurations
impl Default for ExtendedConstExpressionsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            import_based_expressions: false,
            global_dependencies: false,
            constant_folding_level: ConstantFoldingLevel::Basic,
            complex_evaluation: ComplexEvaluationConfig::default(),
        }
    }
}

impl Default for ComplexEvaluationConfig {
    fn default() -> Self {
        Self {
            max_complexity: 1000,
            timeout_ms: 5000, // 5 seconds
            enable_caching: true,
        }
    }
}

impl Default for Memory64ExtendedConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            large_address_optimizations: false,
            cross_memory_addressing: false,
            platform_optimizations: PlatformOptimizationsConfig::default(),
            memory_mapping: Memory64MappingConfig::default(),
        }
    }
}

impl Default for PlatformOptimizationsConfig {
    fn default() -> Self {
        Self {
            x86_64_optimizations: false,
            arm64_optimizations: false,
            custom_optimizations: HashMap::new(),
        }
    }
}

impl Default for Memory64MappingConfig {
    fn default() -> Self {
        Self {
            virtual_mapping: false,
            lazy_allocation: true,
            huge_pages: false,
        }
    }
}

impl Default for CustomPageSizesConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            supported_page_sizes: vec![4096, 8192, 16384, 65536],
            default_strategy: PageSizeStrategy::System,
            alignment_requirements: AlignmentRequirements::default(),
        }
    }
}

impl Default for AlignmentRequirements {
    fn default() -> Self {
        Self {
            min_alignment: 1,
            preferred_alignment: 8,
            strict_alignment: false,
        }
    }
}

impl Default for SharedEverythingThreadsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            thread_pool: SharedThreadPoolConfig::default(),
            shared_state: SharedStateConfig::default(),
            synchronization: SynchronizationConfig::default(),
            thread_safety: ThreadSafetyConfig::default(),
        }
    }
}

impl Default for SharedThreadPoolConfig {
    fn default() -> Self {
        Self {
            min_threads: 1,
            max_threads: std::thread::available_parallelism().map(|p| p.get() as u32).unwrap_or(4),
            thread_affinity: false,
        }
    }
}

impl Default for SharedStateConfig {
    fn default() -> Self {
        Self {
            global_state_sharing: false,
            memory_sharing: false,
            table_sharing: false,
        }
    }
}

impl Default for SynchronizationConfig {
    fn default() -> Self {
        Self {
            atomic_operations: false,
            mutex_support: false,
            condition_variables: false,
        }
    }
}

impl Default for ThreadSafetyConfig {
    fn default() -> Self {
        Self {
            race_detection: false,
            deadlock_detection: false,
            memory_ordering: MemoryOrderingConfig::default(),
        }
    }
}

impl Default for MemoryOrderingConfig {
    fn default() -> Self {
        Self {
            default_ordering: MemoryOrdering::SequentiallyConsistent,
            relaxed_operations: false,
        }
    }
}

impl Default for TypeImportsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            validation_strategy: TypeValidationStrategy::Strict,
            resolution_mechanism: ImportResolutionMechanism::Static,
            compatibility_checking: CompatibilityCheckingConfig::default(),
        }
    }
}

impl Default for CompatibilityCheckingConfig {
    fn default() -> Self {
        Self {
            structural_compatibility: true,
            nominal_compatibility: false,
            version_compatibility: true,
        }
    }
}

impl Default for StringImportsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            encoding_format: StringEncodingFormat::Utf8,
            optimization: StringImportOptimizationConfig::default(),
            js_interop: false,
        }
    }
}

impl Default for StringImportOptimizationConfig {
    fn default() -> Self {
        Self {
            string_interning: true,
            lazy_decoding: true,
            compression: false,
        }
    }
}

impl Default for ResourceTypesConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            lifetime_management: ResourceLifetimeConfig::default(),
            ownership_tracking: OwnershipTrackingConfig::default(),
            cleanup_strategies: ResourceCleanupConfig::default(),
        }
    }
}

impl Default for ResourceLifetimeConfig {
    fn default() -> Self {
        Self {
            automatic_cleanup: true,
            reference_counting: false,
            weak_references: false,
        }
    }
}

impl Default for OwnershipTrackingConfig {
    fn default() -> Self {
        Self {
            strict_ownership: false,
            borrow_checking: false,
            lifetime_analysis: false,
        }
    }
}

impl Default for ResourceCleanupConfig {
    fn default() -> Self {
        Self {
            cleanup_strategy: ResourceCleanupStrategy::Automatic,
            finalizer_support: false,
            gc_integration: false,
        }
    }
}

impl Default for InterfaceTypesConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            validation_level: InterfaceValidationLevel::Standard,
            adapter_generation: AdapterGenerationConfig::default(),
            cross_language_interop: CrossLanguageInteropConfig::default(),
        }
    }
}

impl Default for AdapterGenerationConfig {
    fn default() -> Self {
        Self {
            automatic_generation: true,
            optimization_level: 2,
            caching_enabled: true,
        }
    }
}

impl Default for CrossLanguageInteropConfig {
    fn default() -> Self {
        Self {
            c_interop: false,
            rust_interop: false,
            javascript_interop: false,
            custom_language_support: HashMap::new(),
        }
    }
}

impl Default for FlexibleVectorsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            dynamic_sizing: false,
            operation_optimization: VectorOptimizationConfig::default(),
            simd_integration: SimdIntegrationConfig::default(),
        }
    }
}

impl Default for VectorOptimizationConfig {
    fn default() -> Self {
        Self {
            auto_vectorization: true,
            unroll_loops: true,
            optimize_for_target: true,
        }
    }
}

impl Default for SimdIntegrationConfig {
    fn default() -> Self {
        Self {
            simd_fallback: true,
            platform_specific_simd: true,
            dynamic_simd_selection: false,
        }
    }
}

impl ExperimentalFeaturesConfig {
    /// Apply experimental features to Wasmtime Config
    pub fn apply_to_config(&self, config: &mut Config) -> WasmtimeResult<()> {
        // Note: Most of these features are not yet supported by Wasmtime
        // This method prepares the configuration structure for future support

        // Apply stack switching configuration
        if self.stack_switching.enabled {
            // Enable stack switching when Wasmtime supports it
            // For now, we just validate the configuration
            self.validate_stack_switching_config()?;
        }

        // Apply call/cc configuration
        if self.call_cc.enabled {
            // Enable call/cc when Wasmtime supports it
            self.validate_call_cc_config()?;
        }

        // Apply extended constant expressions
        if self.extended_const_expressions.enabled {
            // Extended constant expressions may be partially supported
            self.validate_extended_const_expressions_config()?;
        }

        // Apply memory64 extended features
        if self.memory64_extended.enabled {
            // Some memory64 extensions may be available
            self.validate_memory64_extended_config()?;
        }

        // Continue with other experimental features...

        Ok(())
    }

    /// Validate stack switching configuration
    fn validate_stack_switching_config(&self) -> WasmtimeResult<()> {
        if self.stack_switching.stack_size < 4096 {
            return Err(WasmtimeError::EngineConfig {
                message: "Stack size must be at least 4KB".to_string(),
            });
        }

        if self.stack_switching.max_concurrent_stacks == 0 {
            return Err(WasmtimeError::EngineConfig {
                message: "Maximum concurrent stacks must be greater than zero".to_string(),
            });
        }

        Ok(())
    }

    /// Validate call/cc configuration
    fn validate_call_cc_config(&self) -> WasmtimeResult<()> {
        if self.call_cc.storage_management.max_continuations == 0 {
            return Err(WasmtimeError::EngineConfig {
                message: "Maximum continuations must be greater than zero".to_string(),
            });
        }

        Ok(())
    }

    /// Validate extended constant expressions configuration
    fn validate_extended_const_expressions_config(&self) -> WasmtimeResult<()> {
        if self.extended_const_expressions.complex_evaluation.max_complexity == 0 {
            return Err(WasmtimeError::EngineConfig {
                message: "Maximum complexity must be greater than zero".to_string(),
            });
        }

        Ok(())
    }

    /// Validate memory64 extended configuration
    fn validate_memory64_extended_config(&self) -> WasmtimeResult<()> {
        // Add validation logic for memory64 extended features
        Ok(())
    }

    /// Create a configuration with all experimental features enabled (for testing)
    pub fn all_experimental_enabled() -> Self {
        Self {
            stack_switching: StackSwitchingConfig {
                enabled: true,
                ..StackSwitchingConfig::default()
            },
            call_cc: CallCcConfig {
                enabled: true,
                ..CallCcConfig::default()
            },
            extended_const_expressions: ExtendedConstExpressionsConfig {
                enabled: true,
                ..ExtendedConstExpressionsConfig::default()
            },
            memory64_extended: Memory64ExtendedConfig {
                enabled: true,
                ..Memory64ExtendedConfig::default()
            },
            custom_page_sizes: CustomPageSizesConfig {
                enabled: true,
                ..CustomPageSizesConfig::default()
            },
            shared_everything_threads: SharedEverythingThreadsConfig {
                enabled: true,
                ..SharedEverythingThreadsConfig::default()
            },
            type_imports: TypeImportsConfig {
                enabled: true,
                ..TypeImportsConfig::default()
            },
            string_imports: StringImportsConfig {
                enabled: true,
                ..StringImportsConfig::default()
            },
            resource_types: ResourceTypesConfig {
                enabled: true,
                ..ResourceTypesConfig::default()
            },
            interface_types: InterfaceTypesConfig {
                enabled: true,
                ..InterfaceTypesConfig::default()
            },
            flexible_vectors: FlexibleVectorsConfig {
                enabled: true,
                ..FlexibleVectorsConfig::default()
            },
        }
    }
}

/// Core functions for experimental features configuration
pub mod core {
    use super::*;
    use crate::validate_ptr_not_null;

    /// Create default experimental features configuration
    pub fn create_experimental_features_config() -> WasmtimeResult<Box<ExperimentalFeaturesConfig>> {
        Ok(Box::new(ExperimentalFeaturesConfig::default()))
    }

    /// Create experimental features configuration with all features enabled
    pub fn create_all_experimental_config() -> WasmtimeResult<Box<ExperimentalFeaturesConfig>> {
        Ok(Box::new(ExperimentalFeaturesConfig::all_experimental_enabled()))
    }

    /// Enable stack switching in configuration
    pub unsafe fn enable_stack_switching(
        config_ptr: *mut c_void,
        stack_size: u64,
        max_stacks: u32,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.stack_switching.enabled = true;
        config.stack_switching.stack_size = stack_size;
        config.stack_switching.max_concurrent_stacks = max_stacks;

        config.validate_stack_switching_config()
    }

    /// Enable call/cc in configuration
    pub unsafe fn enable_call_cc(
        config_ptr: *mut c_void,
        max_continuations: u32,
        storage_strategy: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.call_cc.enabled = true;
        config.call_cc.storage_management.max_continuations = max_continuations;

        // Convert storage strategy from C int
        config.call_cc.storage_management.storage_strategy = match storage_strategy {
            0 => ContinuationStorageStrategy::Stack,
            1 => ContinuationStorageStrategy::Heap,
            2 => ContinuationStorageStrategy::Hybrid,
            _ => ContinuationStorageStrategy::Hybrid,
        };

        config.validate_call_cc_config()
    }

    /// Enable extended constant expressions in configuration
    pub unsafe fn enable_extended_const_expressions(
        config_ptr: *mut c_void,
        import_based: c_int,
        global_deps: c_int,
        folding_level: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.extended_const_expressions.enabled = true;
        config.extended_const_expressions.import_based_expressions = import_based != 0;
        config.extended_const_expressions.global_dependencies = global_deps != 0;

        // Convert folding level from C int
        config.extended_const_expressions.constant_folding_level = match folding_level {
            0 => ConstantFoldingLevel::None,
            1 => ConstantFoldingLevel::Basic,
            2 => ConstantFoldingLevel::Aggressive,
            3 => ConstantFoldingLevel::Full,
            _ => ConstantFoldingLevel::Basic,
        };

        config.validate_extended_const_expressions_config()
    }

    /// Apply experimental features to Wasmtime Config
    pub unsafe fn apply_experimental_features(
        config_ptr: *const c_void,
        wasmtime_config: &mut Config,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &*(config_ptr as *const ExperimentalFeaturesConfig);
        config.apply_to_config(wasmtime_config)
    }

    /// Destroy experimental features configuration
    pub unsafe fn destroy_experimental_features_config(config_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<ExperimentalFeaturesConfig>(
            config_ptr,
            "ExperimentalFeaturesConfig"
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_default_experimental_config() {
        let config = ExperimentalFeaturesConfig::default();
        assert!(!config.stack_switching.enabled);
        assert!(!config.call_cc.enabled);
        assert!(!config.extended_const_expressions.enabled);
    }

    #[test]
    fn test_all_experimental_enabled() {
        let config = ExperimentalFeaturesConfig::all_experimental_enabled();
        assert!(config.stack_switching.enabled);
        assert!(config.call_cc.enabled);
        assert!(config.extended_const_expressions.enabled);
        assert!(config.memory64_extended.enabled);
        assert!(config.flexible_vectors.enabled);
    }

    #[test]
    fn test_stack_switching_validation() {
        let mut config = ExperimentalFeaturesConfig::default();
        config.stack_switching.enabled = true;
        config.stack_switching.stack_size = 2048; // Too small

        assert!(config.validate_stack_switching_config().is_err());

        config.stack_switching.stack_size = 8192; // Valid size
        assert!(config.validate_stack_switching_config().is_ok());
    }

    #[test]
    fn test_call_cc_validation() {
        let mut config = ExperimentalFeaturesConfig::default();
        config.call_cc.enabled = true;
        config.call_cc.storage_management.max_continuations = 0; // Invalid

        assert!(config.validate_call_cc_config().is_err());

        config.call_cc.storage_management.max_continuations = 100; // Valid
        assert!(config.validate_call_cc_config().is_ok());
    }

    #[test]
    fn test_wasmtime_config_application() {
        let config = ExperimentalFeaturesConfig::all_experimental_enabled();
        let mut wasmtime_config = Config::new();

        // Should not error even if features aren't supported yet
        assert!(config.apply_to_config(&mut wasmtime_config).is_ok());
    }
}