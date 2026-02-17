//! Core Component Model operations for Wasmtime
//!
//! This module provides the actual implementation of WebAssembly Component Model
//! operations using Wasmtime's component runtime. It bridges the high-level
//! component interface with Wasmtime's native component model implementation.
//!
//! ## Key Features
//!
//! - **Component Instantiation**: Efficient component loading and instantiation
//! - **Interface Resolution**: Type-safe WIT interface binding and validation
//! - **Resource Management**: Automatic cleanup and lifecycle management
//! - **Error Handling**: Comprehensive error propagation and defensive programming
//! - **Performance**: Optimized for production workloads with minimal overhead

use std::collections::HashMap;
use std::path::Path;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};

use wasmtime::{
    component::{
        types::ComponentItem, Component as WasmtimeComponent,
        Instance as WasmtimeComponentInstance, Linker as ComponentLinker, ResourceTable, Type, Val,
    },
    Config, Engine, Store,
};

#[cfg(feature = "wasi")]
use wasmtime_wasi;

use crate::component::{
    CaseType, Component, ComponentEngine, ComponentMetadata, ComponentTypeKind, ComponentValueType,
    FieldType, FunctionDefinition, InstanceInfo, InterfaceDefinition, Parameter,
    TypeDefinition,
};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Enhanced component engine with actual Wasmtime component model integration
pub struct EnhancedComponentEngine {
    /// Active component instances with metadata
    pub(crate) instances: Arc<RwLock<HashMap<u64, ComponentInstanceHandle>>>,
    /// Component linker for interface resolution
    linker: ComponentLinker<ComponentStoreData>,
    /// Wasmtime engine configured for component model
    engine: Engine,
    /// Next instance ID generator
    next_instance_id: Arc<Mutex<u64>>,
    /// Performance metrics
    metrics: Arc<RwLock<ComponentMetrics>>,
}

/// Extended store data for component instances
pub struct ComponentStoreData {
    /// Instance identifier
    pub instance_id: u64,
    /// Resource table for this store
    pub resource_table: ResourceTable,
    /// WASI context for Preview 2 support
    #[cfg(feature = "wasi")]
    pub wasi_ctx: wasmtime_wasi::WasiCtx,
    /// Start time for performance tracking
    pub start_time: Instant,
}

impl std::fmt::Debug for ComponentStoreData {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ComponentStoreData")
            .field("instance_id", &self.instance_id)
            .field("resource_table", &"<ResourceTable>")
            .field("start_time", &self.start_time)
            .finish()
    }
}

/// Handle for an active component instance with owned Store
///
/// This struct keeps Store and Instance together as required by Wasmtime's ownership model.
/// Store must not be separated from Instance to prevent ownership violations.
#[derive(Debug)]
pub struct ComponentInstanceHandle {
    /// Store owning the instance
    pub store: Store<ComponentStoreData>,
    /// Component instance
    pub instance: WasmtimeComponentInstance,
    /// Component metadata
    pub metadata: ComponentMetadata,
    /// Creation timestamp
    pub created_at: Instant,
    /// Last access timestamp
    pub last_accessed: Instant,
    /// Reference count
    pub ref_count: usize,
}

/// Component performance metrics
#[derive(Default, Debug, Clone)]
pub struct ComponentMetrics {
    /// Total components loaded
    pub components_loaded: u64,
    /// Total instances created
    pub instances_created: u64,
    /// Total instances destroyed
    pub instances_destroyed: u64,
    /// Average instantiation time
    pub avg_instantiation_time: Duration,
    /// Peak memory usage
    pub peak_memory_usage: usize,
    /// Total function calls
    pub function_calls: u64,
    /// Error count
    pub error_count: u64,
}

impl EnhancedComponentEngine {
    /// Create a new enhanced component engine with optimized configuration
    ///
    /// # Returns
    ///
    /// Returns a new `EnhancedComponentEngine` configured for production use
    /// with component model support enabled.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::EngineConfig` if the engine cannot be created.
    pub fn new() -> WasmtimeResult<Self> {
        // Use the shared component engine to avoid GLOBAL_CODE registry accumulation
        let engine = crate::engine::get_shared_component_wasmtime_engine();
        let linker = ComponentLinker::new(&engine);

        Ok(EnhancedComponentEngine {
            instances: Arc::new(RwLock::new(HashMap::new())),
            linker,
            engine,
            next_instance_id: Arc::new(Mutex::new(1)),
            metrics: Arc::new(RwLock::new(ComponentMetrics::default())),
        })
    }

    /// Create enhanced component engine with custom configuration
    ///
    /// # Arguments
    ///
    /// * `config` - Custom Wasmtime configuration
    ///
    /// # Returns
    ///
    /// Returns a new `EnhancedComponentEngine` with the provided configuration.
    pub fn with_config(mut config: Config) -> WasmtimeResult<Self> {
        // CRITICAL: Disable signal-based traps to prevent conflicts with JVM signal handlers
        config.signals_based_traps(false);
        // Ensure component model is enabled
        config.wasm_component_model(true);
        // Explicitly disable async support to prevent fiber/future crashes
        config.async_support(false);

        let engine = Engine::new(&config).map_err(|e| WasmtimeError::EngineConfig {
            message: format!("Failed to create component engine with config: {}", e),
        })?;

        let linker = ComponentLinker::new(&engine);

        Ok(EnhancedComponentEngine {
            instances: Arc::new(RwLock::new(HashMap::new())),
            linker,
            engine,
            next_instance_id: Arc::new(Mutex::new(1)),
            metrics: Arc::new(RwLock::new(ComponentMetrics::default())),
        })
    }

    /// Get a reference to the underlying Wasmtime engine
    pub fn engine(&self) -> &Engine {
        &self.engine
    }

    /// Load and compile a component from WebAssembly bytes
    ///
    /// # Arguments
    ///
    /// * `bytes` - WebAssembly component bytes
    ///
    /// # Returns
    ///
    /// Returns a compiled `Component` with extracted metadata.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Compilation` if compilation fails.
    pub fn load_component_from_bytes(&self, bytes: &[u8]) -> WasmtimeResult<Component> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Component bytes cannot be empty".to_string(),
            });
        }

        let start_time = Instant::now();

        let component = WasmtimeComponent::new(&self.engine, bytes).map_err(|e| {
            WasmtimeError::Compilation {
                message: format!("Failed to compile component: {}", e),
            }
        })?;

        let metadata = self.extract_component_metadata(&component)?;

        // Update metrics
        if let Ok(mut metrics) = self.metrics.write() {
            metrics.components_loaded += 1;
            let compilation_time = start_time.elapsed();
            let n = metrics.components_loaded;
            if n == 1 {
                metrics.avg_instantiation_time = compilation_time;
            } else {
                let prev_nanos = metrics.avg_instantiation_time.as_nanos() as i128;
                let curr_nanos = compilation_time.as_nanos() as i128;
                let new_avg = prev_nanos + (curr_nanos - prev_nanos) / (n as i128);
                metrics.avg_instantiation_time =
                    Duration::from_nanos(new_avg.max(0) as u64);
            }
        }

        Ok(Component::new(component, metadata))
    }

    /// Load and compile a component from a file
    ///
    /// # Arguments
    ///
    /// * `path` - Path to the WebAssembly component file
    ///
    /// # Returns
    ///
    /// Returns a compiled `Component` loaded from the file.
    pub fn load_component_from_file<P: AsRef<Path>>(&self, path: P) -> WasmtimeResult<Component> {
        let path = path.as_ref();

        if !path.exists() {
            return Err(WasmtimeError::Io {
                source: std::io::Error::new(
                    std::io::ErrorKind::NotFound,
                    format!("Component file not found: {}", path.display()),
                ),
            });
        }

        let bytes = std::fs::read(path).map_err(|e| WasmtimeError::Io { source: e })?;

        self.load_component_from_bytes(&bytes)
    }

    /// Instantiate a component with full interface resolution
    ///
    /// # Arguments
    ///
    /// * `component` - The compiled component to instantiate
    ///
    /// # Returns
    ///
    /// Returns the instance ID. The instance is stored in the engine's HashMap to maintain
    /// proper Wasmtime ownership (Engine must outlive Store, which must outlive Instance).
    pub fn instantiate_component(&self, component: &Component) -> WasmtimeResult<u64> {
        let instance_id = self.get_next_instance_id()?;
        let start_time = Instant::now();

        let store_data = ComponentStoreData {
            instance_id,
            resource_table: ResourceTable::new(),
            #[cfg(feature = "wasi")]
            wasi_ctx: wasmtime_wasi::WasiCtxBuilder::new().build(),
            start_time,
        };

        let mut store = Store::new(&self.engine, store_data);

        // Create a fresh linker for this instantiation
        // This avoids potential issues with shared mutable state in the linker
        let mut linker: ComponentLinker<ComponentStoreData> = ComponentLinker::new(&self.engine);

        // Add WASI Preview 2 imports to the linker
        #[cfg(feature = "wasi")]
        {
            wasmtime_wasi::p2::add_to_linker_sync(&mut linker).map_err(|e| {
                WasmtimeError::Instance {
                    message: format!("Failed to add WASI to linker: {}", e),
                }
            })?;
        }

        let instance = linker
            .instantiate(&mut store, component.wasmtime_component())
            .map_err(|e| WasmtimeError::Instance {
                message: format!("Failed to instantiate component: {}", e),
            })?;

        let handle = ComponentInstanceHandle {
            store,
            instance,
            metadata: component.metadata().clone(),
            created_at: start_time,
            last_accessed: start_time,
            ref_count: 1,
        };

        // Store the instance in the engine's HashMap to maintain Engine/Store/Instance ownership
        // This prevents the ownership violation that causes SIGSEGV
        {
            let mut instances = self
                .instances
                .write()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire instances write lock".to_string(),
                })?;
            instances.insert(instance_id, handle);
        }

        // Update metrics
        if let Ok(mut metrics) = self.metrics.write() {
            metrics.instances_created += 1;
        }

        Ok(instance_id)
    }

    /// Call a component function with typed parameters
    ///
    /// # Arguments
    ///
    /// * `instance_info` - Component instance information
    /// * `function_name` - Name of the function to call
    /// * `params` - Function parameters
    ///
    /// # Returns
    ///
    /// Returns the function result values.
    pub fn call_component_function(
        &self,
        instance_info: &mut ComponentInstanceHandle,
        function_name: &str,
        params: &[Val],
    ) -> WasmtimeResult<Vec<Val>> {
        let start_time = Instant::now();

        // Update last accessed time
        instance_info.last_accessed = start_time;

        // Get the exported function from the component instance
        let func = instance_info
            .instance
            .get_func(&mut instance_info.store, function_name)
            .ok_or_else(|| WasmtimeError::ImportExport {
                message: format!(
                    "Function '{}' not found in component exports",
                    function_name
                ),
            })?;

        // Pre-allocate results vector with correct size
        // Wasmtime requires the results vec to be pre-sized, not just have capacity
        let result_count = func.ty(&instance_info.store).results().len();
        let mut results: Vec<Val> = vec![Val::Bool(false); result_count]; // Initialize with dummy values

        let call_result = func.call(&mut instance_info.store, params, &mut results);

        match call_result {
            Ok(_) => {
                // Call succeeded, now do post_return cleanup
                func.post_return(&mut instance_info.store)
                    .map_err(|e| WasmtimeError::Runtime {
                        message: format!(
                            "Failed to complete post-return cleanup for '{}': {}",
                            function_name, e
                        ),
                        backtrace: None,
                    })?;
            }
            Err(e) => {
                return Err(WasmtimeError::Runtime {
                    message: format!(
                        "Failed to call component function '{}': {}",
                        function_name, e
                    ),
                    backtrace: None,
                });
            }
        }

        // Update metrics
        if let Ok(mut metrics) = self.metrics.write() {
            metrics.function_calls += 1;
        }

        Ok(results)
    }

    /// Get list of exported function names from a component instance
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance
    ///
    /// # Returns
    ///
    /// Returns a vector of function names exported by the component instance.
    pub fn get_exported_function_names(&self, instance_id: u64) -> WasmtimeResult<Vec<String>> {
        let instances = self
            .instances
            .read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances read lock".to_string(),
            })?;

        let handle =
            instances
                .get(&instance_id)
                .ok_or_else(|| WasmtimeError::InvalidParameter {
                    message: format!("Instance {} not found", instance_id),
                })?;

        let mut function_names = Vec::new();
        for export in &handle.metadata.exports {
            for func in &export.functions {
                function_names.push(func.name.clone());
            }
        }

        Ok(function_names)
    }

    /// Invoke a component function by instance ID
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance
    /// * `function_name` - The name of the function to invoke
    /// * `params` - The parameters to pass to the function
    ///
    /// # Returns
    ///
    /// Returns a vector of result values from the function invocation.
    pub fn invoke_component_function(
        &self,
        instance_id: u64,
        function_name: &str,
        params: &[wasmtime::component::Val],
    ) -> WasmtimeResult<Vec<wasmtime::component::Val>> {
        let mut instances = self
            .instances
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances write lock".to_string(),
            })?;

        let handle =
            instances
                .get_mut(&instance_id)
                .ok_or_else(|| WasmtimeError::InvalidParameter {
                    message: format!("Instance {} not found", instance_id),
                })?;

        self.call_component_function(handle, function_name, params)
    }

    /// Remove a component instance by ID
    ///
    /// This properly removes the instance from the engine's internal HashMap,
    /// allowing the Store and Instance to be dropped correctly.
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance to remove
    ///
    /// # Returns
    ///
    /// Returns Ok(()) on success, or an error if the instance was not found.
    pub fn remove_instance(&self, instance_id: u64) -> WasmtimeResult<()> {
        let mut instances = self
            .instances
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances write lock".to_string(),
            })?;

        if let Some(handle) = instances.remove(&instance_id) {
            drop(handle);

            // Update metrics
            if let Ok(mut metrics) = self.metrics.write() {
                if metrics.instances_created > 0 {
                    metrics.instances_created -= 1;
                }
            }
            Ok(())
        } else {
            // Instance already removed or never existed - treat as success for idempotency
            Ok(())
        }
    }

    /// Extract detailed metadata from a compiled component
    ///
    /// # Arguments
    ///
    /// * `component` - The compiled Wasmtime component
    ///
    /// # Returns
    ///
    /// Returns `ComponentMetadata` with comprehensive component information.
    fn extract_component_metadata(
        &self,
        component: &WasmtimeComponent,
    ) -> WasmtimeResult<ComponentMetadata> {
        let mut imports = Vec::new();
        let mut exports = Vec::new();

        // Extract component type information
        let component_type = component.component_type();

        // Process imports
        for (name, import_type) in component_type.imports(&self.engine) {
            let interface_def = self.convert_component_item_to_interface(name, &import_type)?;
            imports.push(interface_def);
        }

        // Process exports
        for (name, export_type) in component_type.exports(&self.engine) {
            let interface_def = self.convert_component_item_to_interface(name, &export_type)?;
            exports.push(interface_def);
        }

        Ok(ComponentMetadata {
            imports,
            exports,
            size_bytes: 0, // Size calculation would require access to the original bytes
        })
    }

    /// Convert Wasmtime ComponentItem to InterfaceDefinition
    fn convert_component_item_to_interface(
        &self,
        name: &str,
        item: &ComponentItem,
    ) -> WasmtimeResult<InterfaceDefinition> {
        let mut functions = Vec::new();
        let mut types = Vec::new();
        let mut resources = Vec::new();

        match item {
            ComponentItem::ComponentFunc(func_type) => {
                let function_def = FunctionDefinition {
                    name: name.to_string(),
                    parameters: func_type
                        .params()
                        .map(|(param_name, param_type)| Parameter {
                            name: param_name.to_string(),
                            value_type: self.convert_wasmtime_type_to_component_type(&param_type),
                        })
                        .collect(),
                    results: func_type
                        .results()
                        .map(|result_type| {
                            self.convert_wasmtime_type_to_component_type(&result_type)
                        })
                        .collect(),
                };
                functions.push(function_def);
            }
            ComponentItem::CoreFunc(_) => {
                // Handle core functions
                let function_def = FunctionDefinition {
                    name: name.to_string(),
                    parameters: Vec::new(), // Would need more detailed analysis
                    results: Vec::new(),
                };
                functions.push(function_def);
            }
            ComponentItem::Component(_) => {
                // Nested component - would need recursive analysis
            }
            ComponentItem::ComponentInstance(_) => {
                // Component instance - extract its interface
            }
            ComponentItem::Type(_type_def) => {
                let type_definition = TypeDefinition {
                    name: name.to_string(),
                    kind: ComponentTypeKind::Alias(ComponentValueType::Type(name.to_string())),
                };
                types.push(type_definition);
            }
            _ => {
                // Handle other component items as needed
            }
        }

        // Parse namespace and version from name if present
        let (namespace, version) = self.parse_interface_name(name);

        Ok(InterfaceDefinition {
            name: name.to_string(),
            namespace,
            version,
            functions,
            types,
            resources,
        })
    }

    /// Convert Wasmtime Type to ComponentValueType
    fn convert_wasmtime_type_to_component_type(&self, wasmtime_type: &Type) -> ComponentValueType {
        match wasmtime_type {
            Type::Bool => ComponentValueType::Bool,
            Type::S8 => ComponentValueType::S8,
            Type::U8 => ComponentValueType::U8,
            Type::S16 => ComponentValueType::S16,
            Type::U16 => ComponentValueType::U16,
            Type::S32 => ComponentValueType::S32,
            Type::U32 => ComponentValueType::U32,
            Type::S64 => ComponentValueType::S64,
            Type::U64 => ComponentValueType::U64,
            Type::Float32 => ComponentValueType::Float32,
            Type::Float64 => ComponentValueType::Float64,
            Type::Char => ComponentValueType::U32, // Unicode scalar value
            Type::String => ComponentValueType::String,
            Type::List(inner) => ComponentValueType::List(Box::new(
                self.convert_wasmtime_type_to_component_type(&inner.ty()),
            )),
            Type::Record(record) => {
                let fields = record
                    .fields()
                    .map(|field| FieldType {
                        name: field.name.to_string(),
                        value_type: self.convert_wasmtime_type_to_component_type(&field.ty),
                    })
                    .collect();
                ComponentValueType::Record(fields)
            }
            Type::Variant(variant) => {
                let cases = variant
                    .cases()
                    .map(|case| CaseType {
                        name: case.name.to_string(),
                        payload: case
                            .ty
                            .as_ref()
                            .map(|ty| self.convert_wasmtime_type_to_component_type(ty)),
                    })
                    .collect();
                ComponentValueType::Variant(cases)
            }
            Type::Enum(enum_type) => {
                let names = enum_type.names().map(|s| s.to_string()).collect();
                ComponentValueType::Enum(names)
            }
            Type::Option(inner) => ComponentValueType::Option(Box::new(
                self.convert_wasmtime_type_to_component_type(&inner.ty()),
            )),
            Type::Result(result) => ComponentValueType::Result {
                ok: result
                    .ok()
                    .as_ref()
                    .map(|ty| Box::new(self.convert_wasmtime_type_to_component_type(ty))),
                err: result
                    .err()
                    .as_ref()
                    .map(|ty| Box::new(self.convert_wasmtime_type_to_component_type(ty))),
            },
            Type::Flags(flags) => {
                let flag_names = flags.names().map(|s| s.to_string()).collect();
                ComponentValueType::Flags(flag_names)
            }
            Type::Own(_resource) => ComponentValueType::Resource("own<resource>".to_string()),
            Type::Borrow(_resource) => ComponentValueType::Resource("borrow<resource>".to_string()),
            Type::Tuple(tuple) => {
                let element_types = tuple
                    .types()
                    .map(|ty| self.convert_wasmtime_type_to_component_type(&ty))
                    .collect();
                ComponentValueType::Tuple(element_types)
            }
            Type::Future(_future_type) => {
                // Future types - represent as generic type
                ComponentValueType::Type("future".to_string())
            }
            Type::Stream(_stream_type) => {
                // Stream types - represent as generic type
                ComponentValueType::Type("stream".to_string())
            }
            Type::ErrorContext => {
                // Error context type
                ComponentValueType::Type("error-context".to_string())
            }
        }
    }

    /// Parse interface name for namespace and version
    fn parse_interface_name(&self, name: &str) -> (Option<String>, Option<String>) {
        // Parse names like "wasi:filesystem@0.2.0" or "mypackage:myinterface@1.0"
        if let Some(at_pos) = name.rfind('@') {
            let (base, version) = name.split_at(at_pos);
            let version = version[1..].to_string(); // Remove '@'

            if let Some(colon_pos) = base.find(':') {
                let (namespace, _) = base.split_at(colon_pos);
                return (Some(namespace.to_string()), Some(version));
            } else {
                return (None, Some(version));
            }
        } else if let Some(colon_pos) = name.find(':') {
            let (namespace, _) = name.split_at(colon_pos);
            return (Some(namespace.to_string()), None);
        }

        (None, None)
    }

    /// Get next available instance ID
    fn get_next_instance_id(&self) -> WasmtimeResult<u64> {
        let mut next_id = self
            .next_instance_id
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instance ID lock".to_string(),
            })?;
        let id = *next_id;
        *next_id += 1;
        Ok(id)
    }

    /// Get current component metrics
    pub fn get_metrics(&self) -> WasmtimeResult<ComponentMetrics> {
        let metrics = self
            .metrics
            .read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire metrics lock".to_string(),
            })?;
        Ok(metrics.clone())
    }

    /// Bind custom interface implementation to the linker
    fn bind_custom_interface(
        &mut self,
        interface_name: &str,
        _implementation: Box<dyn std::any::Any + Send + Sync>,
    ) -> WasmtimeResult<()> {
        // For now, log the binding - actual implementation would depend on the interface
        log::info!("Binding custom interface: {}", interface_name);
        // Future: Use implementation to create actual function bindings
        Ok(())
    }

    /// Cleanup inactive component instances
    pub fn cleanup_instances(&self) -> WasmtimeResult<usize> {
        let mut instances = self
            .instances
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances lock".to_string(),
            })?;

        let initial_count = instances.len();
        let now = Instant::now();

        // Remove instances that haven't been accessed recently and have low reference counts
        instances.retain(|_, info| {
            let time_since_access = now.duration_since(info.last_accessed);
            info.ref_count > 0 && time_since_access < Duration::from_secs(300) // 5 minutes
        });

        let cleaned_count = initial_count - instances.len();

        // Update metrics
        if let Ok(mut metrics) = self.metrics.write() {
            metrics.instances_destroyed += cleaned_count as u64;
        }

        Ok(cleaned_count)
    }

    /// Get information about active instances
    pub fn get_active_instances(&self) -> WasmtimeResult<Vec<InstanceInfo>> {
        let instances = self
            .instances
            .read()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances lock".to_string(),
            })?;

        let active_instances = instances
            .iter()
            .map(|(id, info)| InstanceInfo {
                instance_id: *id,
                strong_references: info.ref_count,
            })
            .collect();

        Ok(active_instances)
    }

    /// Add host interface implementation to the linker
    ///
    /// # Arguments
    ///
    /// * `interface_name` - Name of the interface to implement
    /// * `implementation` - Host implementation of the interface
    ///
    /// # Returns
    ///
    /// Returns `Ok(())` if the interface was successfully added.
    pub fn add_host_interface(
        &mut self,
        interface_name: &str,
        implementation: Box<dyn std::any::Any + Send + Sync>,
    ) -> WasmtimeResult<()> {
        // Implement actual host interface binding using Wasmtime's component model
        log::info!("Adding host interface: {}", interface_name);

        // Add WASI interfaces using wasmtime-wasi for Component Model
        if interface_name.starts_with("wasi:") {
            #[cfg(feature = "wasi")]
            {
                // WASI Preview 2 (p2) component model support using wasmtime-wasi
                // This adds all standard WASI interfaces (filesystem, networking, clocks, random, etc.)
                wasmtime_wasi::p2::add_to_linker_async(&mut self.linker).map_err(|e| {
                    WasmtimeError::EngineConfig {
                        message: format!("Failed to add WASI Preview 2 interfaces: {}", e),
                    }
                })?;
                log::info!("Successfully added WASI Preview 2 component model interfaces");
            }
            #[cfg(not(feature = "wasi"))]
            {
                return Err(WasmtimeError::EngineConfig {
                    message: "WASI support not enabled".to_string(),
                });
            }
        } else {
            // For custom interfaces, bind them using the component linker
            self.bind_custom_interface(interface_name, implementation)?;
        }

        Ok(())
    }
}

// Implement WasiView for ComponentStoreData to enable WASI Preview 2 component model
#[cfg(feature = "wasi")]
impl wasmtime_wasi::WasiView for ComponentStoreData {
    fn ctx(&mut self) -> wasmtime_wasi::WasiCtxView<'_> {
        wasmtime_wasi::WasiCtxView {
            ctx: &mut self.wasi_ctx,
            table: &mut self.resource_table,
        }
    }
}

impl Default for ComponentStoreData {
    fn default() -> Self {
        ComponentStoreData {
            instance_id: 0,
            resource_table: ResourceTable::new(),
            #[cfg(feature = "wasi")]
            wasi_ctx: wasmtime_wasi::WasiCtx::builder().build(),
            start_time: Instant::now(),
        }
    }
}

/// Core functions compatible with existing JNI/Panama integration
pub mod core {
    use super::*;
    use crate::error::ffi_utils;
    use crate::{validate_not_empty, validate_ptr_not_null};
    use std::os::raw::c_void;

    /// Core function to create a new enhanced component engine
    pub fn create_component_engine() -> WasmtimeResult<Box<ComponentEngine>> {
        // For backward compatibility, we create the original ComponentEngine
        // but internally use EnhancedComponentEngine for actual operations
        ComponentEngine::new().map(Box::new)
    }

    /// Core function to create component engine with custom Wasmtime engine
    pub fn create_component_engine_with_engine(
        engine: wasmtime::Engine,
    ) -> WasmtimeResult<Box<ComponentEngine>> {
        ComponentEngine::with_engine(engine).map(Box::new)
    }

    /// Create enhanced component engine with full feature set
    pub fn create_enhanced_component_engine() -> WasmtimeResult<Box<EnhancedComponentEngine>> {
        EnhancedComponentEngine::new().map(Box::new)
    }

    /// Core function to validate component engine pointer and get reference
    pub unsafe fn get_component_engine_ref(
        engine_ptr: *const c_void,
    ) -> WasmtimeResult<&'static ComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "component engine");
        Ok(&*(engine_ptr as *const ComponentEngine))
    }

    /// Core function to validate component engine pointer and get mutable reference
    pub unsafe fn get_component_engine_mut(
        engine_ptr: *mut c_void,
    ) -> WasmtimeResult<&'static mut ComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "component engine");
        Ok(&mut *(engine_ptr as *mut ComponentEngine))
    }

    /// Get enhanced component engine reference
    pub unsafe fn get_enhanced_component_engine_ref(
        engine_ptr: *const c_void,
    ) -> WasmtimeResult<&'static EnhancedComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "enhanced component engine");
        Ok(&*(engine_ptr as *const EnhancedComponentEngine))
    }

    /// Get enhanced component engine mutable reference
    pub unsafe fn get_enhanced_component_engine_mut(
        engine_ptr: *mut c_void,
    ) -> WasmtimeResult<&'static mut EnhancedComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "enhanced component engine");
        Ok(&mut *(engine_ptr as *mut EnhancedComponentEngine))
    }

    /// Core function to load a component from bytes (backward compatible)
    pub fn load_component_from_bytes(
        engine: &ComponentEngine,
        bytes: &[u8],
    ) -> WasmtimeResult<Box<Component>> {
        validate_not_empty!(bytes, "component bytes");
        engine.load_component_from_bytes(bytes).map(Box::new)
    }

    /// Load component using enhanced engine
    pub fn load_component_from_bytes_enhanced(
        engine: &EnhancedComponentEngine,
        bytes: &[u8],
    ) -> WasmtimeResult<Box<Component>> {
        validate_not_empty!(bytes, "component bytes");
        engine.load_component_from_bytes(bytes).map(Box::new)
    }

    /// Core function to load a component from a file (backward compatible)
    pub fn load_component_from_file<P: AsRef<std::path::Path>>(
        engine: &ComponentEngine,
        path: P,
    ) -> WasmtimeResult<Box<Component>> {
        engine.load_component_from_file(path).map(Box::new)
    }

    /// Load component from file using enhanced engine
    pub fn load_component_from_file_enhanced<P: AsRef<std::path::Path>>(
        engine: &EnhancedComponentEngine,
        path: P,
    ) -> WasmtimeResult<Box<Component>> {
        engine.load_component_from_file(path).map(Box::new)
    }

    /// Core function to validate component pointer and get reference
    pub unsafe fn get_component_ref(
        component_ptr: *const c_void,
    ) -> WasmtimeResult<&'static Component> {
        validate_ptr_not_null!(component_ptr, "component");
        Ok(&*(component_ptr as *const Component))
    }

    /// Core function to validate component pointer and get mutable reference
    pub unsafe fn get_component_mut(
        component_ptr: *mut c_void,
    ) -> WasmtimeResult<&'static mut Component> {
        validate_ptr_not_null!(component_ptr, "component");
        Ok(&mut *(component_ptr as *mut Component))
    }

    /// Core function to instantiate a component (backward compatible)
    pub fn instantiate_component(
        engine: &ComponentEngine,
        component: &Component,
    ) -> WasmtimeResult<Arc<wasmtime::component::Instance>> {
        engine.instantiate_component(component)
    }

    /// Instantiate component using enhanced engine (returns instance ID)
    pub fn instantiate_component_enhanced(
        engine: &EnhancedComponentEngine,
        component: &Component,
    ) -> WasmtimeResult<u64> {
        engine.instantiate_component(component)
    }

    /// Core function to add a host interface to the component linker
    pub fn add_host_interface(
        engine: &mut ComponentEngine,
        interface_name: &str,
        implementation: crate::component::HostInterface,
    ) -> WasmtimeResult<()> {
        engine.add_host_interface(interface_name, implementation)
    }

    /// Add host interface to enhanced engine
    pub fn add_host_interface_enhanced(
        engine: &mut EnhancedComponentEngine,
        interface_name: &str,
        implementation: Box<dyn std::any::Any + Send + Sync>,
    ) -> WasmtimeResult<()> {
        engine.add_host_interface(interface_name, implementation)
    }

    /// Core function to get active component instances
    pub fn get_active_instances(engine: &ComponentEngine) -> WasmtimeResult<Vec<InstanceInfo>> {
        engine.get_active_instances()
    }

    /// Get active instances from enhanced engine
    pub fn get_active_instances_enhanced(
        engine: &EnhancedComponentEngine,
    ) -> WasmtimeResult<Vec<InstanceInfo>> {
        engine.get_active_instances()
    }

    /// Core function to cleanup inactive component instances
    pub fn cleanup_instances(engine: &ComponentEngine) -> WasmtimeResult<usize> {
        engine.cleanup_instances()
    }

    /// Cleanup instances using enhanced engine
    pub fn cleanup_instances_enhanced(engine: &EnhancedComponentEngine) -> WasmtimeResult<usize> {
        engine.cleanup_instances()
    }

    /// Core function to get component metadata
    pub fn get_component_metadata(component: &Component) -> &ComponentMetadata {
        component.metadata()
    }

    /// Core function to get component size in bytes
    pub fn get_component_size(component: &Component) -> usize {
        component.size_bytes()
    }

    /// Core function to check if component exports an interface
    pub fn exports_interface(component: &Component, interface_name: &str) -> bool {
        component.exports_interface(interface_name)
    }

    /// Core function to check if component imports an interface
    pub fn imports_interface(component: &Component, interface_name: &str) -> bool {
        component.imports_interface(interface_name)
    }

    /// Core function to destroy a component engine (safe cleanup)
    pub unsafe fn destroy_component_engine(engine_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<ComponentEngine>(engine_ptr, "ComponentEngine");
    }

    /// Destroy enhanced component engine
    pub unsafe fn destroy_enhanced_component_engine(engine_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<EnhancedComponentEngine>(
            engine_ptr,
            "EnhancedComponentEngine",
        );
    }

    /// Core function to destroy a component (safe cleanup)
    pub unsafe fn destroy_component(component_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Component>(component_ptr, "Component");
    }

    /// Core function to destroy a component instance (safe cleanup)
    pub unsafe fn destroy_component_instance(instance_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Arc<wasmtime::component::Instance>>(
            instance_ptr,
            "ComponentInstance",
        );
    }

    /// Core function to get number of exports in component
    pub fn get_export_count(component: &Component) -> usize {
        component.metadata().exports.len()
    }

    /// Core function to get number of imports in component
    pub fn get_import_count(component: &Component) -> usize {
        component.metadata().imports.len()
    }

    /// Get component metrics from enhanced engine
    pub fn get_component_metrics(
        engine: &EnhancedComponentEngine,
    ) -> WasmtimeResult<ComponentMetrics> {
        engine.get_metrics()
    }

    /// Call component function with enhanced engine
    pub fn call_component_function(
        instance_info: &mut ComponentInstanceHandle,
        function_name: &str,
        params: &[wasmtime::component::Val],
        engine: &EnhancedComponentEngine,
    ) -> WasmtimeResult<Vec<wasmtime::component::Val>> {
        engine.call_component_function(instance_info, function_name, params)
    }
}

// =============================================================================
// C FFI Exports for EnhancedComponentEngine
// =============================================================================

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_enhanced_component_engine_creation() {
        let engine = EnhancedComponentEngine::new();
        assert!(engine.is_ok());
    }

    #[test]
    fn test_enhanced_component_engine_with_config() {
        let mut config = crate::engine::safe_wasmtime_config();
        config.wasm_component_model(true);
        let engine = EnhancedComponentEngine::with_config(config);
        assert!(engine.is_ok());
    }

    #[test]
    fn test_component_metrics_default() {
        let metrics = ComponentMetrics::default();
        assert_eq!(metrics.components_loaded, 0);
        assert_eq!(metrics.instances_created, 0);
        assert_eq!(metrics.instances_destroyed, 0);
        assert_eq!(metrics.function_calls, 0);
        assert_eq!(metrics.error_count, 0);
    }

    #[test]
    fn test_store_data_default() {
        let store_data = ComponentStoreData::default();
        assert_eq!(store_data.instance_id, 0);
    }

    #[test]
    fn test_parse_interface_name() {
        let engine = EnhancedComponentEngine::new().unwrap();

        // Test with namespace and version
        let (ns, ver) = engine.parse_interface_name("wasi:filesystem@0.2.0");
        assert_eq!(ns, Some("wasi".to_string()));
        assert_eq!(ver, Some("0.2.0".to_string()));

        // Test with only namespace
        let (ns, ver) = engine.parse_interface_name("wasi:filesystem");
        assert_eq!(ns, Some("wasi".to_string()));
        assert_eq!(ver, None);

        // Test with only version
        let (ns, ver) = engine.parse_interface_name("filesystem@0.2.0");
        assert_eq!(ns, None);
        assert_eq!(ver, Some("0.2.0".to_string()));

        // Test with simple name
        let (ns, ver) = engine.parse_interface_name("filesystem");
        assert_eq!(ns, None);
        assert_eq!(ver, None);
    }

    #[test]
    fn test_component_value_type_conversion() {
        let engine = EnhancedComponentEngine::new().unwrap();

        // Test basic type conversions
        assert!(matches!(
            engine.convert_wasmtime_type_to_component_type(&Type::Bool),
            ComponentValueType::Bool
        ));

        assert!(matches!(
            engine.convert_wasmtime_type_to_component_type(&Type::String),
            ComponentValueType::String
        ));

        assert!(matches!(
            engine.convert_wasmtime_type_to_component_type(&Type::S32),
            ComponentValueType::S32
        ));
    }

    #[test]
    fn test_load_component_from_empty_bytes() {
        let engine = EnhancedComponentEngine::new().unwrap();
        let result = engine.load_component_from_bytes(&[]);
        assert!(result.is_err());

        if let Err(WasmtimeError::InvalidParameter { message }) = result {
            assert!(message.contains("empty"));
        } else {
            panic!("Expected InvalidParameter error");
        }
    }

    #[test]
    fn test_load_component_from_invalid_bytes() {
        let engine = EnhancedComponentEngine::new().unwrap();
        let invalid_bytes = vec![0u8; 10]; // Invalid WebAssembly
        let result = engine.load_component_from_bytes(&invalid_bytes);
        assert!(result.is_err());

        if let Err(WasmtimeError::Compilation { .. }) = result {
            // Expected compilation error
        } else {
            panic!("Expected Compilation error");
        }
    }

    #[test]
    fn test_cleanup_instances() {
        let engine = EnhancedComponentEngine::new().unwrap();
        let result = engine.cleanup_instances();
        assert!(result.is_ok());
        assert_eq!(result.unwrap(), 0); // No instances to clean up
    }

    #[test]
    fn test_get_active_instances() {
        let engine = EnhancedComponentEngine::new().unwrap();
        let result = engine.get_active_instances();
        assert!(result.is_ok());
        assert_eq!(result.unwrap().len(), 0); // No active instances
    }

    #[test]
    fn test_get_metrics() {
        let engine = EnhancedComponentEngine::new().unwrap();
        let result = engine.get_metrics();
        assert!(result.is_ok());
        let metrics = result.unwrap();
        assert_eq!(metrics.components_loaded, 0);
        assert_eq!(metrics.instances_created, 0);
    }
}
