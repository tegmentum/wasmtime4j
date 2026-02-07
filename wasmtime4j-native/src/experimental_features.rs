//! Experimental WebAssembly features for wasmtime4j
//!
//! This module provides support for cutting-edge WebAssembly proposals that are currently
//! in committee stage. These features are experimental and subject to change.
//!
//! WARNING: These features are unstable and should only be used for testing and development.

use wasmtime::Config;
use crate::error::{WasmtimeError, WasmtimeResult};
use std::os::raw::{c_void, c_int};

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
}

/// Call/CC (call-with-current-continuation) configuration
#[derive(Debug, Clone)]
pub struct CallCcConfig {
    /// Enable call/cc support
    pub enabled: bool,
    /// Maximum number of continuations
    pub max_continuations: u32,
    /// Continuation storage strategy
    pub storage_strategy: ContinuationStorageStrategy,
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
}

/// Memory64 extended operations configuration
#[derive(Debug, Clone)]
pub struct Memory64ExtendedConfig {
    /// Enable memory64 extended operations
    pub enabled: bool,
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
    /// Strict alignment enforcement
    pub strict_alignment: bool,
}

/// Shared-everything threads configuration
#[derive(Debug, Clone)]
pub struct SharedEverythingThreadsConfig {
    /// Enable shared-everything threads
    pub enabled: bool,
    /// Minimum threads in pool
    pub min_threads: u32,
    /// Maximum threads in pool
    pub max_threads: u32,
    /// Enable global state sharing
    pub global_state_sharing: bool,
    /// Enable atomic operations
    pub atomic_operations: bool,
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
    /// Enable structural compatibility checking
    pub structural_compatibility: bool,
}

/// String imports configuration
#[derive(Debug, Clone)]
pub struct StringImportsConfig {
    /// Enable string imports
    pub enabled: bool,
    /// String encoding format
    pub encoding_format: StringEncodingFormat,
    /// Enable string interning
    pub string_interning: bool,
    /// Enable lazy decoding
    pub lazy_decoding: bool,
    /// Interop with JavaScript strings
    pub js_interop: bool,
}

/// Resource types configuration
#[derive(Debug, Clone)]
pub struct ResourceTypesConfig {
    /// Enable resource types
    pub enabled: bool,
    /// Enable automatic cleanup
    pub automatic_cleanup: bool,
    /// Enable reference counting
    pub reference_counting: bool,
    /// Resource cleanup strategy
    pub cleanup_strategy: ResourceCleanupStrategy,
}

/// Interface types configuration
#[derive(Debug, Clone)]
pub struct InterfaceTypesConfig {
    /// Enable interface types
    pub enabled: bool,
}

/// Flexible vectors configuration
#[derive(Debug, Clone)]
pub struct FlexibleVectorsConfig {
    /// Enable flexible vectors
    pub enabled: bool,
    /// Dynamic vector sizing
    pub dynamic_sizing: bool,
    /// Auto vectorization optimization
    pub auto_vectorization: bool,
    /// SIMD fallback support
    pub simd_fallback: bool,
}

// Strategy enums

#[derive(Debug, Clone, Copy)]
pub enum ContinuationStorageStrategy {
    Stack,
    Heap,
    Hybrid,
}

#[derive(Debug, Clone, Copy)]
pub enum ConstantFoldingLevel {
    None,
    Basic,
    Aggressive,
    Full,
}

#[derive(Debug, Clone, Copy)]
pub enum PageSizeStrategy {
    System,
    Optimal,
    Custom(u32),
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

#[derive(Debug, Clone, Copy)]
pub enum StringEncodingFormat {
    Utf8,
    Utf16,
    Latin1,
    Custom,
}

#[derive(Debug, Clone, Copy)]
pub enum ResourceCleanupStrategy {
    Manual,
    Automatic,
    Hybrid,
}

// Default implementations

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
        }
    }
}

impl Default for CallCcConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            max_continuations: 1000,
            storage_strategy: ContinuationStorageStrategy::Hybrid,
        }
    }
}

impl Default for ExtendedConstExpressionsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            import_based_expressions: false,
            global_dependencies: false,
            constant_folding_level: ConstantFoldingLevel::Basic,
        }
    }
}

impl Default for Memory64ExtendedConfig {
    fn default() -> Self {
        Self {
            enabled: false,
        }
    }
}

impl Default for CustomPageSizesConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            supported_page_sizes: vec![4096, 8192, 16384, 65536],
            default_strategy: PageSizeStrategy::System,
            strict_alignment: false,
        }
    }
}

impl Default for SharedEverythingThreadsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            min_threads: 1,
            max_threads: std::thread::available_parallelism().map(|p| p.get() as u32).unwrap_or(4),
            global_state_sharing: false,
            atomic_operations: false,
        }
    }
}

impl Default for TypeImportsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            validation_strategy: TypeValidationStrategy::Strict,
            resolution_mechanism: ImportResolutionMechanism::Static,
            structural_compatibility: true,
        }
    }
}

impl Default for StringImportsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            encoding_format: StringEncodingFormat::Utf8,
            string_interning: true,
            lazy_decoding: true,
            js_interop: false,
        }
    }
}

impl Default for ResourceTypesConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            automatic_cleanup: true,
            reference_counting: false,
            cleanup_strategy: ResourceCleanupStrategy::Automatic,
        }
    }
}

impl Default for InterfaceTypesConfig {
    fn default() -> Self {
        Self {
            enabled: false,
        }
    }
}

impl Default for FlexibleVectorsConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            dynamic_sizing: false,
            auto_vectorization: true,
            simd_fallback: true,
        }
    }
}

impl ExperimentalFeaturesConfig {
    /// Apply experimental features to Wasmtime Config
    pub fn apply_to_config(&self, _config: &mut Config) -> WasmtimeResult<()> {
        // Note: Most of these features are not yet supported by Wasmtime
        // This method validates configuration and logs for debugging

        if self.stack_switching.enabled {
            self.validate_stack_switching_config()?;
            log::info!("Stack switching configured: stack_size={}, max_stacks={}",
                      self.stack_switching.stack_size,
                      self.stack_switching.max_concurrent_stacks);
        }

        if self.call_cc.enabled {
            self.validate_call_cc_config()?;
            log::info!("Call/CC configured: max_continuations={}, storage={:?}",
                      self.call_cc.max_continuations,
                      self.call_cc.storage_strategy);
        }

        if self.extended_const_expressions.enabled {
            log::info!("Extended constant expressions configured: import_based={}, global_deps={}, folding={:?}",
                      self.extended_const_expressions.import_based_expressions,
                      self.extended_const_expressions.global_dependencies,
                      self.extended_const_expressions.constant_folding_level);
        }

        if self.memory64_extended.enabled {
            log::info!("Memory64 extended configured");
        }

        if self.custom_page_sizes.enabled {
            self.validate_custom_page_sizes_config()?;
            log::info!("Custom page sizes configured: supported_sizes={:?}, strategy={:?}",
                      self.custom_page_sizes.supported_page_sizes,
                      self.custom_page_sizes.default_strategy);
        }

        if self.shared_everything_threads.enabled {
            self.validate_shared_everything_threads_config()?;
            log::info!("Shared-everything threads configured: min_threads={}, max_threads={}",
                      self.shared_everything_threads.min_threads,
                      self.shared_everything_threads.max_threads);
        }

        if self.type_imports.enabled {
            log::info!("Type imports configured: validation={:?}, resolution={:?}",
                      self.type_imports.validation_strategy,
                      self.type_imports.resolution_mechanism);
        }

        if self.string_imports.enabled {
            log::info!("String imports configured: encoding={:?}, interning={}, js_interop={}",
                      self.string_imports.encoding_format,
                      self.string_imports.string_interning,
                      self.string_imports.js_interop);
        }

        if self.resource_types.enabled {
            log::info!("Resource types configured: auto_cleanup={}, ref_counting={}",
                      self.resource_types.automatic_cleanup,
                      self.resource_types.reference_counting);
        }

        if self.interface_types.enabled {
            log::info!("Interface types configured");
        }

        if self.flexible_vectors.enabled {
            log::info!("Flexible vectors configured: dynamic_sizing={}, auto_vectorization={}",
                      self.flexible_vectors.dynamic_sizing,
                      self.flexible_vectors.auto_vectorization);
        }

        Ok(())
    }

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

    fn validate_call_cc_config(&self) -> WasmtimeResult<()> {
        if self.call_cc.max_continuations == 0 {
            return Err(WasmtimeError::EngineConfig {
                message: "Maximum continuations must be greater than zero".to_string(),
            });
        }
        Ok(())
    }

    fn validate_custom_page_sizes_config(&self) -> WasmtimeResult<()> {
        if self.custom_page_sizes.supported_page_sizes.is_empty() {
            return Err(WasmtimeError::EngineConfig {
                message: "Custom page sizes must specify at least one supported size".to_string(),
            });
        }
        Ok(())
    }

    fn validate_shared_everything_threads_config(&self) -> WasmtimeResult<()> {
        if self.shared_everything_threads.min_threads > self.shared_everything_threads.max_threads {
            return Err(WasmtimeError::EngineConfig {
                message: "Minimum thread count cannot exceed maximum thread count".to_string(),
            });
        }
        Ok(())
    }

    /// Create a configuration with all experimental features enabled (for testing)
    pub fn all_experimental_enabled() -> Self {
        Self {
            stack_switching: StackSwitchingConfig { enabled: true, ..Default::default() },
            call_cc: CallCcConfig { enabled: true, ..Default::default() },
            extended_const_expressions: ExtendedConstExpressionsConfig { enabled: true, ..Default::default() },
            memory64_extended: Memory64ExtendedConfig { enabled: true },
            custom_page_sizes: CustomPageSizesConfig { enabled: true, ..Default::default() },
            shared_everything_threads: SharedEverythingThreadsConfig { enabled: true, ..Default::default() },
            type_imports: TypeImportsConfig { enabled: true, ..Default::default() },
            string_imports: StringImportsConfig { enabled: true, ..Default::default() },
            resource_types: ResourceTypesConfig { enabled: true, ..Default::default() },
            interface_types: InterfaceTypesConfig { enabled: true },
            flexible_vectors: FlexibleVectorsConfig { enabled: true, ..Default::default() },
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
        config.call_cc.max_continuations = max_continuations;
        config.call_cc.storage_strategy = match storage_strategy {
            0 => ContinuationStorageStrategy::Stack,
            1 => ContinuationStorageStrategy::Heap,
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
        config.extended_const_expressions.constant_folding_level = match folding_level {
            0 => ConstantFoldingLevel::None,
            1 => ConstantFoldingLevel::Basic,
            2 => ConstantFoldingLevel::Aggressive,
            _ => ConstantFoldingLevel::Full,
        };

        Ok(())
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

    /// Enable flexible vectors in configuration
    pub unsafe fn enable_flexible_vectors(
        config_ptr: *mut c_void,
        dynamic_sizing: c_int,
        auto_vectorization: c_int,
        simd_fallback: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.flexible_vectors.enabled = true;
        config.flexible_vectors.dynamic_sizing = dynamic_sizing != 0;
        config.flexible_vectors.auto_vectorization = auto_vectorization != 0;
        config.flexible_vectors.simd_fallback = simd_fallback != 0;

        Ok(())
    }

    /// Enable string imports in configuration
    pub unsafe fn enable_string_imports(
        config_ptr: *mut c_void,
        encoding_format: c_int,
        string_interning: c_int,
        lazy_decoding: c_int,
        js_interop: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.string_imports.enabled = true;
        config.string_imports.encoding_format = match encoding_format {
            0 => StringEncodingFormat::Utf8,
            1 => StringEncodingFormat::Utf16,
            2 => StringEncodingFormat::Latin1,
            _ => StringEncodingFormat::Custom,
        };
        config.string_imports.string_interning = string_interning != 0;
        config.string_imports.lazy_decoding = lazy_decoding != 0;
        config.string_imports.js_interop = js_interop != 0;

        Ok(())
    }

    /// Enable resource types in configuration
    pub unsafe fn enable_resource_types(
        config_ptr: *mut c_void,
        automatic_cleanup: c_int,
        reference_counting: c_int,
        cleanup_strategy: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.resource_types.enabled = true;
        config.resource_types.automatic_cleanup = automatic_cleanup != 0;
        config.resource_types.reference_counting = reference_counting != 0;
        config.resource_types.cleanup_strategy = match cleanup_strategy {
            0 => ResourceCleanupStrategy::Manual,
            1 => ResourceCleanupStrategy::Automatic,
            _ => ResourceCleanupStrategy::Hybrid,
        };

        Ok(())
    }

    /// Enable type imports in configuration
    pub unsafe fn enable_type_imports(
        config_ptr: *mut c_void,
        validation_strategy: c_int,
        resolution_mechanism: c_int,
        structural_compatibility: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.type_imports.enabled = true;
        config.type_imports.validation_strategy = match validation_strategy {
            0 => TypeValidationStrategy::Strict,
            1 => TypeValidationStrategy::Relaxed,
            _ => TypeValidationStrategy::Dynamic,
        };
        config.type_imports.resolution_mechanism = match resolution_mechanism {
            0 => ImportResolutionMechanism::Static,
            1 => ImportResolutionMechanism::Dynamic,
            _ => ImportResolutionMechanism::Lazy,
        };
        config.type_imports.structural_compatibility = structural_compatibility != 0;

        Ok(())
    }

    /// Enable shared-everything threads in configuration
    pub unsafe fn enable_shared_everything_threads(
        config_ptr: *mut c_void,
        min_threads: u32,
        max_threads: u32,
        global_state_sharing: c_int,
        atomic_operations: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.shared_everything_threads.enabled = true;
        config.shared_everything_threads.min_threads = min_threads;
        config.shared_everything_threads.max_threads = max_threads;
        config.shared_everything_threads.global_state_sharing = global_state_sharing != 0;
        config.shared_everything_threads.atomic_operations = atomic_operations != 0;

        config.validate_shared_everything_threads_config()
    }

    /// Enable custom page sizes in configuration
    pub unsafe fn enable_custom_page_sizes(
        config_ptr: *mut c_void,
        page_size: u32,
        strategy: c_int,
        strict_alignment: c_int,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(config_ptr, "experimental_features_config");
        let config = &mut *(config_ptr as *mut ExperimentalFeaturesConfig);

        config.custom_page_sizes.enabled = true;
        config.custom_page_sizes.supported_page_sizes = vec![page_size];
        config.custom_page_sizes.default_strategy = match strategy {
            0 => PageSizeStrategy::System,
            1 => PageSizeStrategy::Optimal,
            _ => PageSizeStrategy::Custom(page_size),
        };
        config.custom_page_sizes.strict_alignment = strict_alignment != 0;

        config.validate_custom_page_sizes_config()
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
        config.call_cc.max_continuations = 0; // Invalid

        assert!(config.validate_call_cc_config().is_err());

        config.call_cc.max_continuations = 100; // Valid
        assert!(config.validate_call_cc_config().is_ok());
    }

    #[test]
    fn test_wasmtime_config_application() {
        let config = ExperimentalFeaturesConfig::all_experimental_enabled();
        let mut wasmtime_config = crate::engine::safe_wasmtime_config();

        // Should not error even if features aren't supported yet
        assert!(config.apply_to_config(&mut wasmtime_config).is_ok());
    }
}
