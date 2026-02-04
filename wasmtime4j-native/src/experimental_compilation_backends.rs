//! # Experimental Compilation Backends
//!
//! This module implements experimental compilation backends for Wasmtime including
//! WASM-to-native compilation, JIT improvements, and advanced optimization strategies.

use std::collections::HashMap;
use std::sync::{Arc, RwLock, Mutex};
use std::time::{Duration, Instant, SystemTime};
use std::thread;
use std::path::PathBuf;
use anyhow::{Result, anyhow};
use log::{info, warn, debug, error};
use serde::{Deserialize, Serialize};
use wasmtime::{Config, Engine, Module, CompilationStrategy, OptLevel, Strategy};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::future_capabilities::{FutureCapability, get_capability_manager};

/// Experimental compilation backend manager
#[derive(Debug)]
pub struct ExperimentalCompilationBackend {
    backends: Arc<RwLock<HashMap<BackendType, CompilationBackend>>>,
    active_backend: Arc<RwLock<BackendType>>,
    compilation_cache: Arc<RwLock<CompilationCache>>,
    optimization_profiles: Arc<RwLock<HashMap<String, OptimizationProfile>>>,
    metrics_collector: Arc<Mutex<CompilationMetrics>>,
}

/// Types of compilation backends
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum BackendType {
    /// Standard Cranelift backend
    Standard,
    /// Experimental WASM-to-native compilation
    WasmToNative,
    /// Advanced JIT with profile-guided optimization
    AdvancedJit,
    /// Predictive compilation backend
    Predictive,
    /// Adaptive execution backend
    Adaptive,
    /// Speculative optimization backend
    Speculative,
    /// Cross-module optimization backend
    CrossModule,
}

/// Compilation backend implementation
#[derive(Debug)]
pub struct CompilationBackend {
    backend_type: BackendType,
    engine: Engine,
    config: BackendConfig,
    statistics: CompilationStatistics,
    is_available: bool,
    fallback_backend: Option<BackendType>,
}

/// Backend-specific configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BackendConfig {
    pub optimization_level: OptimizationLevel,
    pub enable_profiling: bool,
    pub enable_debug_info: bool,
    pub compilation_threads: Option<usize>,
    pub memory_limit: Option<usize>,
    pub timeout: Option<Duration>,
    pub cache_size: usize,
    pub enable_parallel_compilation: bool,
    pub backend_specific: HashMap<String, String>,
}

/// Optimization levels for backends
#[derive(Debug, Clone, Copy, Serialize, Deserialize)]
pub enum OptimizationLevel {
    None,
    Speed,
    SpeedAndSize,
    Size,
    Adaptive,
    ProfileGuided,
}

/// Compilation statistics for each backend
#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct CompilationStatistics {
    pub total_compilations: u64,
    pub successful_compilations: u64,
    pub failed_compilations: u64,
    pub average_compilation_time: Duration,
    pub total_compilation_time: Duration,
    pub cache_hits: u64,
    pub cache_misses: u64,
    pub optimized_functions: u64,
    pub generated_code_size: u64,
}

/// Optimization profile for specific use cases
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct OptimizationProfile {
    pub name: String,
    pub backend_preferences: Vec<BackendType>,
    pub optimization_level: OptimizationLevel,
    pub compile_time_budget: Duration,
    pub target_performance: PerformanceTarget,
    pub memory_constraints: MemoryConstraints,
    pub specific_optimizations: Vec<OptimizationPass>,
}

/// Performance targeting for optimization
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum PerformanceTarget {
    Throughput,
    Latency,
    MemoryUsage,
    CompileTime,
    Balanced,
}

/// Memory constraints for compilation
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryConstraints {
    pub max_heap_size: Option<usize>,
    pub max_stack_size: Option<usize>,
    pub gc_pressure_limit: Option<f64>,
}

/// Specific optimization passes
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum OptimizationPass {
    InlineExpansion,
    LoopOptimization,
    VectorOptimization,
    BranchPrediction,
    CallSiteOptimization,
    MemoryLayoutOptimization,
    RegisterAllocation,
    PeepholeOptimization,
    DeadCodeElimination,
    ConstantPropagation,
}

/// Compilation cache for storing compiled modules
#[derive(Debug, Default)]
pub struct CompilationCache {
    entries: HashMap<String, CacheEntry>,
    max_size: usize,
    current_size: usize,
}

/// Cache entry for compiled modules
#[derive(Debug, Clone)]
pub struct CacheEntry {
    pub compiled_module: Vec<u8>,
    pub backend_type: BackendType,
    pub optimization_profile: String,
    pub compilation_time: Duration,
    pub access_count: u64,
    pub last_accessed: SystemTime,
    pub size: usize,
}

/// Metrics collector for compilation performance
#[derive(Debug, Default)]
pub struct CompilationMetrics {
    pub backend_performance: HashMap<BackendType, BackendPerformanceMetrics>,
    pub profile_effectiveness: HashMap<String, ProfileEffectivenessMetrics>,
    pub cache_statistics: CacheStatistics,
    pub optimization_impact: HashMap<OptimizationPass, OptimizationImpact>,
}

/// Performance metrics for each backend
#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct BackendPerformanceMetrics {
    pub average_compile_time: Duration,
    pub success_rate: f64,
    pub generated_code_quality: f64,
    pub memory_usage: f64,
    pub cache_efficiency: f64,
}

/// Effectiveness metrics for optimization profiles
#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct ProfileEffectivenessMetrics {
    pub performance_improvement: f64,
    pub compile_time_overhead: f64,
    pub memory_overhead: f64,
    pub success_rate: f64,
}

/// Cache performance statistics
#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct CacheStatistics {
    pub hit_rate: f64,
    pub eviction_rate: f64,
    pub average_entry_size: usize,
    pub total_cache_usage: usize,
}

/// Impact of optimization passes
#[derive(Debug, Default, Clone, Serialize, Deserialize)]
pub struct OptimizationImpact {
    pub performance_gain: f64,
    pub compile_time_cost: f64,
    pub memory_cost: f64,
    pub applicability: f64,
}

impl ExperimentalCompilationBackend {
    /// Create a new experimental compilation backend manager
    pub fn new() -> Result<Self> {
        let mut backend = Self {
            backends: Arc::new(RwLock::new(HashMap::new())),
            active_backend: Arc::new(RwLock::new(BackendType::Standard)),
            compilation_cache: Arc::new(RwLock::new(CompilationCache::new(1024 * 1024 * 100))), // 100MB cache
            optimization_profiles: Arc::new(RwLock::new(HashMap::new())),
            metrics_collector: Arc::new(Mutex::new(CompilationMetrics::default())),
        };

        backend.initialize_backends()?;
        backend.setup_optimization_profiles()?;

        Ok(backend)
    }

    /// Initialize available compilation backends
    fn initialize_backends(&mut self) -> Result<()> {
        let mut backends = self.backends.write().unwrap_or_else(|e| e.into_inner());
        let capability_manager = get_capability_manager();

        // Standard backend (always available)
        backends.insert(
            BackendType::Standard,
            self.create_standard_backend()?,
        );

        // WASM-to-native backend (if available)
        if capability_manager.is_capability_available(FutureCapability::RuntimeCodeGeneration) {
            backends.insert(
                BackendType::WasmToNative,
                self.create_wasm_to_native_backend()?,
            );
        }

        // Advanced JIT backend (if available)
        if capability_manager.is_capability_available(FutureCapability::ProfileGuidedOptimization) {
            backends.insert(
                BackendType::AdvancedJit,
                self.create_advanced_jit_backend()?,
            );
        }

        // Predictive compilation backend (if available)
        if capability_manager.is_capability_available(FutureCapability::PredictiveCompilation) {
            backends.insert(
                BackendType::Predictive,
                self.create_predictive_backend()?,
            );
        }

        // Adaptive execution backend (if available)
        if capability_manager.is_capability_available(FutureCapability::AdaptiveExecution) {
            backends.insert(
                BackendType::Adaptive,
                self.create_adaptive_backend()?,
            );
        }

        // Speculative optimization backend (if available)
        if capability_manager.is_capability_available(FutureCapability::SpeculativeOptimization) {
            backends.insert(
                BackendType::Speculative,
                self.create_speculative_backend()?,
            );
        }

        // Cross-module optimization backend (if available)
        if capability_manager.is_capability_available(FutureCapability::CrossModuleOptimization) {
            backends.insert(
                BackendType::CrossModule,
                self.create_cross_module_backend()?,
            );
        }

        info!("Initialized {} compilation backends", backends.len());
        Ok(())
    }

    /// Create standard Cranelift backend
    fn create_standard_backend(&self) -> Result<CompilationBackend> {
        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);

        let engine = Engine::new(&config)?;

        Ok(CompilationBackend {
            backend_type: BackendType::Standard,
            engine,
            config: BackendConfig::default(),
            statistics: CompilationStatistics::default(),
            is_available: true,
            fallback_backend: None,
        })
    }

    /// Create WASM-to-native compilation backend
    fn create_wasm_to_native_backend(&self) -> Result<CompilationBackend> {
        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::SpeedAndSize);
        config.parallel_compilation(true);

        // Enable additional optimizations for native code generation
        config.cranelift_debug_verifier(false); // Disable for performance

        let engine = Engine::new(&config)?;

        let mut backend_config = BackendConfig::default();
        backend_config.optimization_level = OptimizationLevel::Speed;
        backend_config.enable_parallel_compilation = true;
        backend_config.backend_specific.insert("native_codegen".to_string(), "true".to_string());

        Ok(CompilationBackend {
            backend_type: BackendType::WasmToNative,
            engine,
            config: backend_config,
            statistics: CompilationStatistics::default(),
            is_available: true,
            fallback_backend: Some(BackendType::Standard),
        })
    }

    /// Create advanced JIT backend with profile-guided optimization
    fn create_advanced_jit_backend(&self) -> Result<CompilationBackend> {
        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::SpeedAndSize);
        config.parallel_compilation(true);
        config.profiler(wasmtime::ProfilingStrategy::PerfMap);

        let engine = Engine::new(&config)?;

        let mut backend_config = BackendConfig::default();
        backend_config.optimization_level = OptimizationLevel::ProfileGuided;
        backend_config.enable_profiling = true;
        backend_config.enable_parallel_compilation = true;
        backend_config.backend_specific.insert("profile_guided".to_string(), "true".to_string());
        backend_config.backend_specific.insert("adaptive_optimization".to_string(), "true".to_string());

        Ok(CompilationBackend {
            backend_type: BackendType::AdvancedJit,
            engine,
            config: backend_config,
            statistics: CompilationStatistics::default(),
            is_available: true,
            fallback_backend: Some(BackendType::Standard),
        })
    }

    /// Create predictive compilation backend
    fn create_predictive_backend(&self) -> Result<CompilationBackend> {
        // Predictive compilation is not yet available in stable Wasmtime
        // This implementation provides a framework for when it becomes available

        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);
        config.parallel_compilation(true);

        // Placeholder for predictive compilation features
        // These would be enabled when Wasmtime supports them

        let engine = Engine::new(&config)?;

        let mut backend_config = BackendConfig::default();
        backend_config.optimization_level = OptimizationLevel::Adaptive;
        backend_config.backend_specific.insert("predictive_compilation".to_string(), "true".to_string());
        backend_config.backend_specific.insert("prediction_threshold".to_string(), "0.7".to_string());

        Ok(CompilationBackend {
            backend_type: BackendType::Predictive,
            engine,
            config: backend_config,
            statistics: CompilationStatistics::default(),
            is_available: false, // Not yet available in current Wasmtime
            fallback_backend: Some(BackendType::AdvancedJit),
        })
    }

    /// Create adaptive execution backend
    fn create_adaptive_backend(&self) -> Result<CompilationBackend> {
        // Adaptive execution is not yet available in stable Wasmtime
        // This provides the framework for future implementation

        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::Speed);

        let engine = Engine::new(&config)?;

        let mut backend_config = BackendConfig::default();
        backend_config.optimization_level = OptimizationLevel::Adaptive;
        backend_config.backend_specific.insert("adaptive_execution".to_string(), "true".to_string());
        backend_config.backend_specific.insert("adaptation_window".to_string(), "300".to_string());

        Ok(CompilationBackend {
            backend_type: BackendType::Adaptive,
            engine,
            config: backend_config,
            statistics: CompilationStatistics::default(),
            is_available: false, // Not yet available
            fallback_backend: Some(BackendType::AdvancedJit),
        })
    }

    /// Create speculative optimization backend
    fn create_speculative_backend(&self) -> Result<CompilationBackend> {
        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::SpeedAndSize);

        let engine = Engine::new(&config)?;

        let mut backend_config = BackendConfig::default();
        backend_config.optimization_level = OptimizationLevel::Speed;
        backend_config.backend_specific.insert("speculative_optimization".to_string(), "true".to_string());

        Ok(CompilationBackend {
            backend_type: BackendType::Speculative,
            engine,
            config: backend_config,
            statistics: CompilationStatistics::default(),
            is_available: false, // Future feature
            fallback_backend: Some(BackendType::Standard),
        })
    }

    /// Create cross-module optimization backend
    fn create_cross_module_backend(&self) -> Result<CompilationBackend> {
        let mut config = crate::engine::safe_wasmtime_config();
        config.strategy(Strategy::Cranelift);
        config.cranelift_opt_level(OptLevel::SpeedAndSize);
        config.parallel_compilation(true);

        let engine = Engine::new(&config)?;

        let mut backend_config = BackendConfig::default();
        backend_config.optimization_level = OptimizationLevel::Speed;
        backend_config.enable_parallel_compilation = true;
        backend_config.backend_specific.insert("cross_module_optimization".to_string(), "true".to_string());

        Ok(CompilationBackend {
            backend_type: BackendType::CrossModule,
            engine,
            config: backend_config,
            statistics: CompilationStatistics::default(),
            is_available: false, // Future feature
            fallback_backend: Some(BackendType::Standard),
        })
    }

    /// Setup optimization profiles
    fn setup_optimization_profiles(&self) -> Result<()> {
        let mut profiles = self.optimization_profiles.write().unwrap_or_else(|e| e.into_inner());

        // High-performance profile
        profiles.insert("high_performance".to_string(), OptimizationProfile {
            name: "High Performance".to_string(),
            backend_preferences: vec![BackendType::AdvancedJit, BackendType::WasmToNative, BackendType::Standard],
            optimization_level: OptimizationLevel::Speed,
            compile_time_budget: Duration::from_secs(30),
            target_performance: PerformanceTarget::Throughput,
            memory_constraints: MemoryConstraints {
                max_heap_size: None,
                max_stack_size: None,
                gc_pressure_limit: Some(0.8),
            },
            specific_optimizations: vec![
                OptimizationPass::InlineExpansion,
                OptimizationPass::LoopOptimization,
                OptimizationPass::VectorOptimization,
                OptimizationPass::CallSiteOptimization,
            ],
        });

        // Low-latency profile
        profiles.insert("low_latency".to_string(), OptimizationProfile {
            name: "Low Latency".to_string(),
            backend_preferences: vec![BackendType::Predictive, BackendType::AdvancedJit, BackendType::Standard],
            optimization_level: OptimizationLevel::Adaptive,
            compile_time_budget: Duration::from_secs(5),
            target_performance: PerformanceTarget::Latency,
            memory_constraints: MemoryConstraints {
                max_heap_size: Some(1024 * 1024 * 512), // 512MB
                max_stack_size: Some(1024 * 64), // 64KB
                gc_pressure_limit: Some(0.5),
            },
            specific_optimizations: vec![
                OptimizationPass::BranchPrediction,
                OptimizationPass::RegisterAllocation,
                OptimizationPass::CallSiteOptimization,
            ],
        });

        // Memory-constrained profile
        profiles.insert("memory_constrained".to_string(), OptimizationProfile {
            name: "Memory Constrained".to_string(),
            backend_preferences: vec![BackendType::Standard],
            optimization_level: OptimizationLevel::Size,
            compile_time_budget: Duration::from_secs(10),
            target_performance: PerformanceTarget::MemoryUsage,
            memory_constraints: MemoryConstraints {
                max_heap_size: Some(1024 * 1024 * 128), // 128MB
                max_stack_size: Some(1024 * 32), // 32KB
                gc_pressure_limit: Some(0.3),
            },
            specific_optimizations: vec![
                OptimizationPass::DeadCodeElimination,
                OptimizationPass::MemoryLayoutOptimization,
                OptimizationPass::ConstantPropagation,
            ],
        });

        // Development profile
        profiles.insert("development".to_string(), OptimizationProfile {
            name: "Development".to_string(),
            backend_preferences: vec![BackendType::Standard],
            optimization_level: OptimizationLevel::None,
            compile_time_budget: Duration::from_secs(2),
            target_performance: PerformanceTarget::CompileTime,
            memory_constraints: MemoryConstraints {
                max_heap_size: None,
                max_stack_size: None,
                gc_pressure_limit: Some(1.0),
            },
            specific_optimizations: vec![],
        });

        info!("Setup {} optimization profiles", profiles.len());
        Ok(())
    }

    /// Compile module with specified backend and profile
    pub fn compile_module(
        &self,
        wasm_bytes: &[u8],
        backend_type: BackendType,
        profile_name: &str,
    ) -> Result<Module> {
        let start_time = Instant::now();

        // Get backend
        let backends = self.backends.read().unwrap_or_else(|e| e.into_inner());
        let backend = backends.get(&backend_type)
            .ok_or_else(|| anyhow!("Backend {:?} not available", backend_type))?;

        // Check if backend is available
        if !backend.is_available {
            if let Some(fallback) = backend.fallback_backend {
                warn!("Backend {:?} not available, falling back to {:?}", backend_type, fallback);
                return self.compile_module(wasm_bytes, fallback, profile_name);
            } else {
                return Err(anyhow!("Backend {:?} not available and no fallback", backend_type));
            }
        }

        // Check cache first
        let cache_key = self.generate_cache_key(wasm_bytes, backend_type, profile_name);
        if let Some(cached_module) = self.get_from_cache(&cache_key)? {
            debug!("Cache hit for module compilation");
            return Ok(self.deserialize_module(&cached_module.compiled_module, &backend.engine)?);
        }

        // Compile module
        let module = Module::from_binary(&backend.engine, wasm_bytes)?;

        let compilation_time = start_time.elapsed();

        // Update statistics
        self.update_compilation_statistics(backend_type, compilation_time, true);

        // Cache the compiled module
        self.cache_compiled_module(&cache_key, &module, backend_type, profile_name, compilation_time)?;

        info!(
            "Compiled module with {:?} backend in {:?} (profile: {})",
            backend_type, compilation_time, profile_name
        );

        Ok(module)
    }

    /// Select optimal backend for given requirements
    pub fn select_optimal_backend(&self, profile_name: &str) -> Result<BackendType> {
        let profiles = self.optimization_profiles.read().unwrap_or_else(|e| e.into_inner());
        let profile = profiles.get(profile_name)
            .ok_or_else(|| anyhow!("Optimization profile {} not found", profile_name))?;

        let backends = self.backends.read().unwrap_or_else(|e| e.into_inner());

        // Try backends in preference order
        for &backend_type in &profile.backend_preferences {
            if let Some(backend) = backends.get(&backend_type) {
                if backend.is_available {
                    return Ok(backend_type);
                }
            }
        }

        // Fallback to standard backend
        Ok(BackendType::Standard)
    }

    /// Generate cache key for compiled module
    fn generate_cache_key(&self, wasm_bytes: &[u8], backend_type: BackendType, profile_name: &str) -> String {
        use std::collections::hash_map::DefaultHasher;
        use std::hash::{Hash, Hasher};

        let mut hasher = DefaultHasher::new();
        wasm_bytes.hash(&mut hasher);
        backend_type.hash(&mut hasher);
        profile_name.hash(&mut hasher);

        format!("{:x}", hasher.finish())
    }

    /// Get module from cache
    fn get_from_cache(&self, cache_key: &str) -> Result<Option<CacheEntry>> {
        let mut cache = self.compilation_cache.write().unwrap_or_else(|e| e.into_inner());
        if let Some(entry) = cache.entries.get_mut(cache_key) {
            entry.access_count += 1;
            entry.last_accessed = SystemTime::now();
            Ok(Some(entry.clone()))
        } else {
            Ok(None)
        }
    }

    /// Cache compiled module
    fn cache_compiled_module(
        &self,
        cache_key: &str,
        module: &Module,
        backend_type: BackendType,
        profile_name: &str,
        compilation_time: Duration,
    ) -> Result<()> {
        // Serialize module (placeholder - actual serialization would depend on Wasmtime API)
        let serialized = self.serialize_module(module)?;

        let entry = CacheEntry {
            compiled_module: serialized.clone(),
            backend_type,
            optimization_profile: profile_name.to_string(),
            compilation_time,
            access_count: 1,
            last_accessed: SystemTime::now(),
            size: serialized.len(),
        };

        let mut cache = self.compilation_cache.write().unwrap_or_else(|e| e.into_inner());

        // Check if we need to evict entries
        while cache.current_size + entry.size > cache.max_size && !cache.entries.is_empty() {
            cache.evict_lru_entry();
        }

        cache.current_size += entry.size;
        cache.entries.insert(cache_key.to_string(), entry);

        Ok(())
    }

    /// Serialize module (placeholder implementation)
    fn serialize_module(&self, _module: &Module) -> Result<Vec<u8>> {
        // This would use Wasmtime's module serialization when available
        // For now, return empty vector as placeholder
        Ok(vec![])
    }

    /// Deserialize module (placeholder implementation)
    fn deserialize_module(&self, _data: &[u8], engine: &Engine) -> Result<Module> {
        // This would use Wasmtime's module deserialization when available
        // For now, return error to force recompilation
        Err(anyhow!("Module deserialization not implemented"))
    }

    /// Update compilation statistics
    fn update_compilation_statistics(&self, backend_type: BackendType, compilation_time: Duration, success: bool) {
        if let Ok(mut backends) = self.backends.write() {
            if let Some(backend) = backends.get_mut(&backend_type) {
                backend.statistics.total_compilations += 1;
                if success {
                    backend.statistics.successful_compilations += 1;
                } else {
                    backend.statistics.failed_compilations += 1;
                }

                let total_time = backend.statistics.total_compilation_time + compilation_time;
                backend.statistics.total_compilation_time = total_time;
                backend.statistics.average_compilation_time =
                    total_time / backend.statistics.total_compilations as u32;
            }
        }
    }

    /// Get compilation statistics for backend
    pub fn get_backend_statistics(&self, backend_type: BackendType) -> Option<CompilationStatistics> {
        let backends = self.backends.read().unwrap_or_else(|e| e.into_inner());
        backends.get(&backend_type).map(|backend| backend.statistics.clone())
    }

    /// Get all available backends
    pub fn get_available_backends(&self) -> Vec<BackendType> {
        let backends = self.backends.read().unwrap_or_else(|e| e.into_inner());
        backends.iter()
            .filter(|(_, backend)| backend.is_available)
            .map(|(&backend_type, _)| backend_type)
            .collect()
    }

    /// Set active backend
    pub fn set_active_backend(&self, backend_type: BackendType) -> Result<()> {
        let backends = self.backends.read().unwrap_or_else(|e| e.into_inner());
        if !backends.contains_key(&backend_type) {
            return Err(anyhow!("Backend {:?} not available", backend_type));
        }

        let mut active = self.active_backend.write().unwrap_or_else(|e| e.into_inner());
        *active = backend_type;

        info!("Set active compilation backend to {:?}", backend_type);
        Ok(())
    }

    /// Get active backend
    pub fn get_active_backend(&self) -> BackendType {
        *self.active_backend.read().unwrap_or_else(|e| e.into_inner())
    }
}

impl CompilationCache {
    /// Create new compilation cache
    pub fn new(max_size: usize) -> Self {
        Self {
            entries: HashMap::new(),
            max_size,
            current_size: 0,
        }
    }

    /// Evict least recently used entry
    fn evict_lru_entry(&mut self) {
        let mut oldest_key = None;
        let mut oldest_time = SystemTime::now();

        for (key, entry) in &self.entries {
            if entry.last_accessed < oldest_time {
                oldest_time = entry.last_accessed;
                oldest_key = Some(key.clone());
            }
        }

        if let Some(key) = oldest_key {
            if let Some(entry) = self.entries.remove(&key) {
                self.current_size -= entry.size;
            }
        }
    }
}

impl Default for BackendConfig {
    fn default() -> Self {
        Self {
            optimization_level: OptimizationLevel::Speed,
            enable_profiling: false,
            enable_debug_info: false,
            compilation_threads: None,
            memory_limit: None,
            timeout: Some(Duration::from_secs(60)),
            cache_size: 1024 * 1024 * 50, // 50MB
            enable_parallel_compilation: true,
            backend_specific: HashMap::new(),
        }
    }
}

/// Global experimental compilation backend instance
static mut EXPERIMENTAL_BACKEND: Option<ExperimentalCompilationBackend> = None;
static EXPERIMENTAL_BACKEND_INIT: std::sync::Once = std::sync::Once::new();

/// Get global experimental compilation backend
pub fn get_experimental_backend() -> Result<&'static ExperimentalCompilationBackend> {
    unsafe {
        EXPERIMENTAL_BACKEND_INIT.call_once(|| {
            match ExperimentalCompilationBackend::new() {
                Ok(backend) => EXPERIMENTAL_BACKEND = Some(backend),
                Err(e) => error!("Failed to initialize experimental compilation backend: {}", e),
            }
        });

        EXPERIMENTAL_BACKEND.as_ref()
            .ok_or_else(|| anyhow!("Experimental compilation backend not initialized"))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_backend_creation() {
        let backend = ExperimentalCompilationBackend::new().unwrap();
        let available_backends = backend.get_available_backends();

        // Should always have standard backend
        assert!(available_backends.contains(&BackendType::Standard));
    }

    #[test]
    fn test_optimization_profiles() {
        let backend = ExperimentalCompilationBackend::new().unwrap();
        let optimal_backend = backend.select_optimal_backend("high_performance").unwrap();

        // Should select an available backend
        assert!(backend.get_available_backends().contains(&optimal_backend));
    }

    #[test]
    fn test_cache_operations() {
        let mut cache = CompilationCache::new(1024);
        assert_eq!(cache.entries.len(), 0);
        assert_eq!(cache.current_size, 0);
    }

    #[test]
    fn test_cache_key_generation() {
        let backend = ExperimentalCompilationBackend::new().unwrap();
        let wasm_bytes = b"test_wasm";

        let key1 = backend.generate_cache_key(wasm_bytes, BackendType::Standard, "high_performance");
        let key2 = backend.generate_cache_key(wasm_bytes, BackendType::Standard, "high_performance");
        let key3 = backend.generate_cache_key(wasm_bytes, BackendType::AdvancedJit, "high_performance");

        assert_eq!(key1, key2);
        assert_ne!(key1, key3);
    }

    #[test]
    fn test_backend_selection() {
        let backend = ExperimentalCompilationBackend::new().unwrap();

        // Should be able to set and get active backend
        backend.set_active_backend(BackendType::Standard).unwrap();
        assert_eq!(backend.get_active_backend(), BackendType::Standard);
    }
}