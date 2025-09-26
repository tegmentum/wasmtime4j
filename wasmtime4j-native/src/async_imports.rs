//! Async imports implementation for wasmtime4j
//!
//! This module provides cutting-edge async import capabilities including:
//! - Non-blocking import resolution
//! - Lazy loading of imports
//! - Progressive import validation
//! - Dynamic import binding
//!
//! WARNING: These features are highly experimental and require Wasmtime's async support.

use wasmtime::{Config, Engine, Store, Module, Instance, Linker, Caller, FuncType, ValType, AsContext, AsContextMut};
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::time::{Duration, Instant};
use std::future::Future;
use std::pin::Pin;
use std::task::{Context, Poll};
use tokio::sync::{mpsc, oneshot};
use tokio::time::timeout;

/// Configuration for async import resolution
#[derive(Debug, Clone)]
pub struct AsyncImportConfig {
    /// Enable async import resolution
    pub enabled: bool,
    /// Timeout for import resolution in milliseconds
    pub resolution_timeout_ms: u64,
    /// Maximum concurrent import resolutions
    pub max_concurrent_resolutions: usize,
    /// Enable lazy loading of imports
    pub lazy_loading: bool,
    /// Progressive validation settings
    pub progressive_validation: ProgressiveValidationConfig,
    /// Dynamic binding configuration
    pub dynamic_binding: DynamicBindingConfig,
    /// Import caching configuration
    pub import_caching: ImportCachingConfig,
}

/// Progressive validation configuration
#[derive(Debug, Clone)]
pub struct ProgressiveValidationConfig {
    /// Enable progressive validation
    pub enabled: bool,
    /// Validation batch size
    pub batch_size: usize,
    /// Maximum validation time per batch in milliseconds
    pub max_batch_time_ms: u64,
    /// Enable parallel validation
    pub parallel_validation: bool,
}

/// Dynamic binding configuration
#[derive(Debug, Clone)]
pub struct DynamicBindingConfig {
    /// Enable dynamic binding
    pub enabled: bool,
    /// Allow late binding of imports
    pub late_binding: bool,
    /// Enable runtime rebinding
    pub runtime_rebinding: bool,
    /// Binding cache size
    pub binding_cache_size: usize,
}

/// Import caching configuration
#[derive(Debug, Clone)]
pub struct ImportCachingConfig {
    /// Enable import caching
    pub enabled: bool,
    /// Cache size limit in entries
    pub max_cache_entries: usize,
    /// Cache TTL in milliseconds
    pub cache_ttl_ms: u64,
    /// Enable LRU eviction
    pub lru_eviction: bool,
}

/// Async import manager
#[derive(Debug)]
pub struct AsyncImportManager {
    config: AsyncImportConfig,
    import_registry: Arc<RwLock<ImportRegistry>>,
    resolution_queue: Arc<Mutex<mpsc::UnboundedSender<ImportResolutionRequest>>>,
    active_resolutions: Arc<AtomicU64>,
    import_cache: Arc<RwLock<ImportCache>>,
    resolution_stats: Arc<Mutex<ResolutionStatistics>>,
}

/// Import registry for tracking imports
#[derive(Debug)]
pub struct ImportRegistry {
    modules: HashMap<String, ModuleImports>,
    functions: HashMap<ImportKey, ImportedFunction>,
    tables: HashMap<ImportKey, ImportedTable>,
    memories: HashMap<ImportKey, ImportedMemory>,
    globals: HashMap<ImportKey, ImportedGlobal>,
}

/// Module imports tracking
#[derive(Debug)]
pub struct ModuleImports {
    module_name: String,
    import_count: usize,
    resolved_count: AtomicU64,
    pending_imports: Vec<ImportKey>,
    resolution_status: ImportResolutionStatus,
    lazy_imports: Vec<LazyImport>,
}

/// Import key for identification
#[derive(Debug, Clone, Hash, Eq, PartialEq)]
pub struct ImportKey {
    pub module_name: String,
    pub import_name: String,
    pub import_type: ImportType,
}

/// Import types
#[derive(Debug, Clone, Hash, Eq, PartialEq)]
pub enum ImportType {
    Function,
    Table,
    Memory,
    Global,
}

/// Import resolution status
#[derive(Debug, Clone)]
pub enum ImportResolutionStatus {
    Pending,
    InProgress,
    Resolved,
    Failed(String),
}

/// Imported function information
#[derive(Debug)]
pub struct ImportedFunction {
    key: ImportKey,
    func_type: FuncType,
    resolution_strategy: FunctionResolutionStrategy,
    binding_time: BindingTime,
    resolution_status: ImportResolutionStatus,
}

/// Function resolution strategies
#[derive(Debug, Clone)]
pub enum FunctionResolutionStrategy {
    Eager,
    Lazy,
    OnDemand,
    Cached,
}

/// Binding time options
#[derive(Debug, Clone)]
pub enum BindingTime {
    CompileTime,
    LinkTime,
    Runtime,
    FirstUse,
}

/// Imported table information
#[derive(Debug)]
pub struct ImportedTable {
    key: ImportKey,
    element_type: ValType,
    limits: (u32, Option<u32>),
    resolution_status: ImportResolutionStatus,
}

/// Imported memory information
#[derive(Debug)]
pub struct ImportedMemory {
    key: ImportKey,
    limits: (u32, Option<u32>),
    shared: bool,
    resolution_status: ImportResolutionStatus,
}

/// Imported global information
#[derive(Debug)]
pub struct ImportedGlobal {
    key: ImportKey,
    global_type: ValType,
    mutable: bool,
    resolution_status: ImportResolutionStatus,
}

/// Lazy import definition
#[derive(Debug)]
pub struct LazyImport {
    key: ImportKey,
    resolution_trigger: ResolutionTrigger,
    resolver: Box<dyn ImportResolver>,
}

/// Resolution triggers for lazy imports
#[derive(Debug)]
pub enum ResolutionTrigger {
    FirstAccess,
    ModuleInstantiation,
    ExplicitRequest,
    TimeoutBased(Duration),
}

/// Import resolver trait
pub trait ImportResolver: Send + Sync + std::fmt::Debug {
    fn resolve_import(&self, key: &ImportKey) -> Pin<Box<dyn Future<Output = WasmtimeResult<ImportValue>> + Send>>;
    fn can_resolve(&self, key: &ImportKey) -> bool;
    fn resolution_priority(&self) -> u8;
}

/// Import resolution request
#[derive(Debug)]
pub struct ImportResolutionRequest {
    key: ImportKey,
    priority: ResolutionPriority,
    timeout: Option<Duration>,
    response_channel: oneshot::Sender<WasmtimeResult<ImportValue>>,
}

/// Resolution priority levels
#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord)]
pub enum ResolutionPriority {
    Low = 0,
    Normal = 1,
    High = 2,
    Critical = 3,
}

/// Import value types
#[derive(Debug)]
pub enum ImportValue {
    Function(Box<dyn Fn(&mut Caller<'_, ()>, &[wasmtime::Val]) -> wasmtime::Result<Vec<wasmtime::Val>> + Send + Sync>),
    Table(wasmtime::Table),
    Memory(wasmtime::Memory),
    Global(wasmtime::Global),
}

/// Import cache for resolved imports
#[derive(Debug)]
pub struct ImportCache {
    entries: HashMap<ImportKey, CachedImport>,
    max_size: usize,
    access_order: Vec<ImportKey>,
    total_hits: u64,
    total_misses: u64,
}

/// Cached import entry
#[derive(Debug)]
pub struct CachedImport {
    value: ImportValue,
    created_at: Instant,
    last_accessed: Instant,
    access_count: u64,
    ttl: Duration,
}

/// Resolution statistics
#[derive(Debug)]
pub struct ResolutionStatistics {
    total_imports: u64,
    resolved_imports: u64,
    failed_resolutions: u64,
    lazy_resolutions: u64,
    cached_resolutions: u64,
    average_resolution_time_ms: f64,
    peak_concurrent_resolutions: u64,
}

impl Default for AsyncImportConfig {
    fn default() -> Self {
        Self {
            enabled: false,
            resolution_timeout_ms: 5000, // 5 seconds
            max_concurrent_resolutions: 100,
            lazy_loading: true,
            progressive_validation: ProgressiveValidationConfig::default(),
            dynamic_binding: DynamicBindingConfig::default(),
            import_caching: ImportCachingConfig::default(),
        }
    }
}

impl Default for ProgressiveValidationConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            batch_size: 10,
            max_batch_time_ms: 1000, // 1 second per batch
            parallel_validation: true,
        }
    }
}

impl Default for DynamicBindingConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            late_binding: true,
            runtime_rebinding: false,
            binding_cache_size: 1000,
        }
    }
}

impl Default for ImportCachingConfig {
    fn default() -> Self {
        Self {
            enabled: true,
            max_cache_entries: 10000,
            cache_ttl_ms: 300000, // 5 minutes
            lru_eviction: true,
        }
    }
}

impl AsyncImportManager {
    /// Create new async import manager
    pub fn new(config: AsyncImportConfig) -> WasmtimeResult<Self> {
        let (tx, mut rx) = mpsc::unbounded_channel();

        let manager = Self {
            config: config.clone(),
            import_registry: Arc::new(RwLock::new(ImportRegistry::new())),
            resolution_queue: Arc::new(Mutex::new(tx)),
            active_resolutions: Arc::new(AtomicU64::new(0)),
            import_cache: Arc::new(RwLock::new(ImportCache::new(config.import_caching.max_cache_entries))),
            resolution_stats: Arc::new(Mutex::new(ResolutionStatistics::new())),
        };

        // Start resolution worker if async imports are enabled
        if config.enabled {
            manager.start_resolution_worker(rx)?;
        }

        Ok(manager)
    }

    /// Register module for async import resolution
    pub async fn register_module(&self, module: &Module, module_name: String) -> WasmtimeResult<()> {
        let mut registry = self.import_registry.write().map_err(|_| WasmtimeError::Internal {
            message: "Failed to acquire registry write lock".to_string(),
        })?;

        let mut imports = ModuleImports {
            module_name: module_name.clone(),
            import_count: 0,
            resolved_count: AtomicU64::new(0),
            pending_imports: Vec::new(),
            resolution_status: ImportResolutionStatus::Pending,
            lazy_imports: Vec::new(),
        };

        // Analyze module imports
        for import in module.imports() {
            let import_key = ImportKey {
                module_name: import.module().to_string(),
                import_name: import.name().to_string(),
                import_type: match import.ty() {
                    wasmtime::ExternType::Func(_) => ImportType::Function,
                    wasmtime::ExternType::Table(_) => ImportType::Table,
                    wasmtime::ExternType::Memory(_) => ImportType::Memory,
                    wasmtime::ExternType::Global(_) => ImportType::Global,
                },
            };

            imports.import_count += 1;
            imports.pending_imports.push(import_key.clone());

            // Register specific import type
            match import.ty() {
                wasmtime::ExternType::Func(func_type) => {
                    let imported_func = ImportedFunction {
                        key: import_key.clone(),
                        func_type: func_type.clone(),
                        resolution_strategy: if self.config.lazy_loading {
                            FunctionResolutionStrategy::Lazy
                        } else {
                            FunctionResolutionStrategy::Eager
                        },
                        binding_time: if self.config.dynamic_binding.late_binding {
                            BindingTime::Runtime
                        } else {
                            BindingTime::LinkTime
                        },
                        resolution_status: ImportResolutionStatus::Pending,
                    };
                    registry.functions.insert(import_key, imported_func);
                }
                wasmtime::ExternType::Table(table_type) => {
                    let imported_table = ImportedTable {
                        key: import_key.clone(),
                        element_type: table_type.element().clone(),
                        limits: (table_type.minimum(), table_type.maximum()),
                        resolution_status: ImportResolutionStatus::Pending,
                    };
                    registry.tables.insert(import_key, imported_table);
                }
                wasmtime::ExternType::Memory(memory_type) => {
                    let imported_memory = ImportedMemory {
                        key: import_key.clone(),
                        limits: (memory_type.minimum(), memory_type.maximum()),
                        shared: memory_type.is_shared(),
                        resolution_status: ImportResolutionStatus::Pending,
                    };
                    registry.memories.insert(import_key, imported_memory);
                }
                wasmtime::ExternType::Global(global_type) => {
                    let imported_global = ImportedGlobal {
                        key: import_key.clone(),
                        global_type: global_type.content().clone(),
                        mutable: global_type.mutability() == wasmtime::Mutability::Var,
                        resolution_status: ImportResolutionStatus::Pending,
                    };
                    registry.globals.insert(import_key, imported_global);
                }
            }
        }

        registry.modules.insert(module_name, imports);
        Ok(())
    }

    /// Resolve imports asynchronously
    pub async fn resolve_imports(&self, module_name: &str) -> WasmtimeResult<HashMap<ImportKey, ImportValue>> {
        if !self.config.enabled {
            return Err(WasmtimeError::EngineConfig {
                message: "Async imports are not enabled".to_string(),
            });
        }

        let pending_imports = {
            let registry = self.import_registry.read().map_err(|_| WasmtimeError::Internal {
                message: "Failed to acquire registry read lock".to_string(),
            })?;

            registry.modules.get(module_name)
                .map(|module| module.pending_imports.clone())
                .unwrap_or_default()
        };

        let mut resolved_imports = HashMap::new();
        let mut resolution_futures = Vec::new();

        // Check cache first
        if self.config.import_caching.enabled {
            let mut cache = self.import_cache.write().map_err(|_| WasmtimeError::Internal {
                message: "Failed to acquire cache write lock".to_string(),
            })?;

            for import_key in &pending_imports {
                if let Some(cached_import) = cache.get_cached_import(import_key) {
                    // Note: In a real implementation, we'd need to clone the ImportValue
                    // For now, we'll skip cache retrieval and focus on the resolution logic
                    continue;
                }
            }
        }

        // Create resolution futures for uncached imports
        for import_key in pending_imports {
            if resolved_imports.contains_key(&import_key) {
                continue; // Already resolved from cache
            }

            let resolution_future = self.resolve_single_import(import_key.clone());
            resolution_futures.push(async move {
                (import_key, resolution_future.await)
            });
        }

        // Execute resolutions with progressive validation
        if self.config.progressive_validation.enabled && self.config.progressive_validation.parallel_validation {
            let batch_size = self.config.progressive_validation.batch_size;
            let batch_timeout = Duration::from_millis(self.config.progressive_validation.max_batch_time_ms);

            for batch in resolution_futures.chunks(batch_size) {
                let batch_results = timeout(
                    batch_timeout,
                    futures::future::join_all(batch)
                ).await.map_err(|_| WasmtimeError::Runtime {
                    message: "Resolution batch timeout exceeded".to_string(),
                })?;

                for (import_key, result) in batch_results {
                    match result {
                        Ok(import_value) => {
                            resolved_imports.insert(import_key, import_value);
                        }
                        Err(error) => {
                            log::warn!("Failed to resolve import {:?}: {:?}", import_key, error);
                            return Err(error);
                        }
                    }
                }
            }
        } else {
            // Sequential resolution
            for future in resolution_futures {
                let (import_key, result) = future.await;
                match result {
                    Ok(import_value) => {
                        resolved_imports.insert(import_key, import_value);
                    }
                    Err(error) => {
                        log::warn!("Failed to resolve import {:?}: {:?}", import_key, error);
                        return Err(error);
                    }
                }
            }
        }

        // Update resolution statistics
        {
            let mut stats = self.resolution_stats.lock().map_err(|_| WasmtimeError::Internal {
                message: "Failed to acquire stats lock".to_string(),
            })?;
            stats.resolved_imports += resolved_imports.len() as u64;
        }

        Ok(resolved_imports)
    }

    /// Resolve a single import
    async fn resolve_single_import(&self, import_key: ImportKey) -> WasmtimeResult<ImportValue> {
        let start_time = Instant::now();
        self.active_resolutions.fetch_add(1, Ordering::Relaxed);

        // Create resolution request
        let (response_tx, response_rx) = oneshot::channel();
        let request = ImportResolutionRequest {
            key: import_key.clone(),
            priority: ResolutionPriority::Normal,
            timeout: Some(Duration::from_millis(self.config.resolution_timeout_ms)),
            response_channel: response_tx,
        };

        // Queue resolution request
        {
            let queue = self.resolution_queue.lock().map_err(|_| WasmtimeError::Internal {
                message: "Failed to acquire resolution queue lock".to_string(),
            })?;
            queue.send(request).map_err(|_| WasmtimeError::Internal {
                message: "Failed to send resolution request".to_string(),
            })?;
        }

        // Wait for resolution
        let result = response_rx.await.map_err(|_| WasmtimeError::Internal {
            message: "Resolution request cancelled".to_string(),
        })?;

        self.active_resolutions.fetch_sub(1, Ordering::Relaxed);

        // Update statistics
        let resolution_time = start_time.elapsed();
        {
            let mut stats = self.resolution_stats.lock().map_err(|_| WasmtimeError::Internal {
                message: "Failed to acquire stats lock".to_string(),
            })?;
            stats.update_resolution_time(resolution_time.as_millis() as f64);
        }

        result
    }

    /// Start the resolution worker
    fn start_resolution_worker(&self, mut rx: mpsc::UnboundedReceiver<ImportResolutionRequest>) -> WasmtimeResult<()> {
        let config = self.config.clone();
        let stats = self.resolution_stats.clone();

        tokio::spawn(async move {
            while let Some(request) = rx.recv().await {
                let start_time = Instant::now();

                // Simulate import resolution (in a real implementation, this would involve actual resolution logic)
                let result = Self::perform_import_resolution(&request.key).await;

                // Send response
                if let Err(_) = request.response_channel.send(result) {
                    log::warn!("Failed to send resolution response for {:?}", request.key);
                }

                // Update statistics
                if let Ok(mut stats) = stats.lock() {
                    stats.total_imports += 1;
                    if result.is_ok() {
                        stats.resolved_imports += 1;
                    } else {
                        stats.failed_resolutions += 1;
                    }
                }
            }
        });

        Ok(())
    }

    /// Perform actual import resolution (placeholder implementation)
    async fn perform_import_resolution(import_key: &ImportKey) -> WasmtimeResult<ImportValue> {
        // This is a placeholder implementation
        // In a real implementation, this would:
        // 1. Look up resolvers for the import
        // 2. Try each resolver in priority order
        // 3. Return the resolved import value

        match import_key.import_type {
            ImportType::Function => {
                // Create a dummy function
                let func = Box::new(|_caller: &mut Caller<'_, ()>, _args: &[wasmtime::Val]| {
                    Ok(vec![])
                });
                Ok(ImportValue::Function(func))
            }
            _ => Err(WasmtimeError::Runtime {
                message: format!("Import resolution not implemented for {:?}", import_key.import_type),
            }),
        }
    }

    /// Enable lazy loading for specific imports
    pub fn enable_lazy_loading(&self, import_keys: Vec<ImportKey>) -> WasmtimeResult<()> {
        let mut registry = self.import_registry.write().map_err(|_| WasmtimeError::Internal {
            message: "Failed to acquire registry write lock".to_string(),
        })?;

        for import_key in import_keys {
            if let Some(func) = registry.functions.get_mut(&import_key) {
                func.resolution_strategy = FunctionResolutionStrategy::Lazy;
                func.binding_time = BindingTime::FirstUse;
            }
        }

        Ok(())
    }

    /// Get resolution statistics
    pub fn get_resolution_statistics(&self) -> WasmtimeResult<ResolutionStatistics> {
        let stats = self.resolution_stats.lock().map_err(|_| WasmtimeError::Internal {
            message: "Failed to acquire stats lock".to_string(),
        })?;
        Ok(stats.clone())
    }
}

impl ImportRegistry {
    fn new() -> Self {
        Self {
            modules: HashMap::new(),
            functions: HashMap::new(),
            tables: HashMap::new(),
            memories: HashMap::new(),
            globals: HashMap::new(),
        }
    }
}

impl ImportCache {
    fn new(max_size: usize) -> Self {
        Self {
            entries: HashMap::new(),
            max_size,
            access_order: Vec::new(),
            total_hits: 0,
            total_misses: 0,
        }
    }

    fn get_cached_import(&mut self, key: &ImportKey) -> Option<&ImportValue> {
        if let Some(entry) = self.entries.get_mut(key) {
            entry.last_accessed = Instant::now();
            entry.access_count += 1;
            self.total_hits += 1;

            // Update access order for LRU
            if let Some(pos) = self.access_order.iter().position(|k| k == key) {
                self.access_order.remove(pos);
            }
            self.access_order.push(key.clone());

            Some(&entry.value)
        } else {
            self.total_misses += 1;
            None
        }
    }
}

impl ResolutionStatistics {
    fn new() -> Self {
        Self {
            total_imports: 0,
            resolved_imports: 0,
            failed_resolutions: 0,
            lazy_resolutions: 0,
            cached_resolutions: 0,
            average_resolution_time_ms: 0.0,
            peak_concurrent_resolutions: 0,
        }
    }

    fn update_resolution_time(&mut self, time_ms: f64) {
        let total_samples = self.resolved_imports + self.failed_resolutions;
        if total_samples > 0 {
            self.average_resolution_time_ms =
                (self.average_resolution_time_ms * (total_samples - 1) as f64 + time_ms) / total_samples as f64;
        } else {
            self.average_resolution_time_ms = time_ms;
        }
    }
}

impl Clone for ResolutionStatistics {
    fn clone(&self) -> Self {
        Self {
            total_imports: self.total_imports,
            resolved_imports: self.resolved_imports,
            failed_resolutions: self.failed_resolutions,
            lazy_resolutions: self.lazy_resolutions,
            cached_resolutions: self.cached_resolutions,
            average_resolution_time_ms: self.average_resolution_time_ms,
            peak_concurrent_resolutions: self.peak_concurrent_resolutions,
        }
    }
}

/// Core functions for async imports
pub mod core {
    use super::*;
    use crate::validate_ptr_not_null;
    use std::os::raw::{c_void, c_int};

    /// Create async import manager
    pub fn create_async_import_manager(config: AsyncImportConfig) -> WasmtimeResult<Box<AsyncImportManager>> {
        Ok(Box::new(AsyncImportManager::new(config)?))
    }

    /// Register module for async import resolution
    pub unsafe fn register_module_async_imports(
        manager_ptr: *mut c_void,
        module_ptr: *const c_void,
        module_name: *const std::os::raw::c_char,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(manager_ptr, "async_import_manager");
        validate_ptr_not_null!(module_ptr, "module");
        validate_ptr_not_null!(module_name, "module_name");

        let manager = &*(manager_ptr as *const AsyncImportManager);
        let module = &*(module_ptr as *const Module);

        let module_name_str = std::ffi::CStr::from_ptr(module_name)
            .to_str()
            .map_err(|_| WasmtimeError::InvalidArgument {
                message: "Invalid module name".to_string(),
            })?
            .to_string();

        // Use a runtime for async operations
        let rt = tokio::runtime::Runtime::new().map_err(|_| WasmtimeError::Internal {
            message: "Failed to create async runtime".to_string(),
        })?;

        rt.block_on(manager.register_module(module, module_name_str))
    }

    /// Enable lazy loading for imports
    pub unsafe fn enable_lazy_loading_imports(
        manager_ptr: *mut c_void,
        import_keys_ptr: *const c_void,
        count: usize,
    ) -> WasmtimeResult<()> {
        validate_ptr_not_null!(manager_ptr, "async_import_manager");
        validate_ptr_not_null!(import_keys_ptr, "import_keys");

        let manager = &*(manager_ptr as *const AsyncImportManager);

        // In a real implementation, we'd properly deserialize the import keys
        // For now, we'll create placeholder keys
        let import_keys = vec![]; // Placeholder

        manager.enable_lazy_loading(import_keys)
    }

    /// Destroy async import manager
    pub unsafe fn destroy_async_import_manager(manager_ptr: *mut c_void) {
        crate::error::ffi_utils::destroy_resource::<AsyncImportManager>(
            manager_ptr,
            "AsyncImportManager"
        );
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_async_import_config_default() {
        let config = AsyncImportConfig::default();
        assert!(!config.enabled);
        assert_eq!(config.resolution_timeout_ms, 5000);
        assert_eq!(config.max_concurrent_resolutions, 100);
        assert!(config.lazy_loading);
    }

    #[test]
    fn test_import_key_creation() {
        let key = ImportKey {
            module_name: "env".to_string(),
            import_name: "memory".to_string(),
            import_type: ImportType::Memory,
        };

        assert_eq!(key.module_name, "env");
        assert_eq!(key.import_name, "memory");
        assert!(matches!(key.import_type, ImportType::Memory));
    }

    #[test]
    fn test_resolution_statistics() {
        let mut stats = ResolutionStatistics::new();
        stats.update_resolution_time(100.0);
        assert_eq!(stats.average_resolution_time_ms, 100.0);

        stats.resolved_imports = 1;
        stats.update_resolution_time(200.0);
        assert_eq!(stats.average_resolution_time_ms, 150.0);
    }

    #[tokio::test]
    async fn test_async_import_manager_creation() {
        let config = AsyncImportConfig {
            enabled: true,
            ..Default::default()
        };

        let result = AsyncImportManager::new(config);
        assert!(result.is_ok());
    }
}