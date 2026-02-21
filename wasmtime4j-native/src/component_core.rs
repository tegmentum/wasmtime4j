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
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};

use wasmtime::{
    component::{
        types::ComponentItem, Component as WasmtimeComponent,
        Instance as WasmtimeComponentInstance, Linker as ComponentLinker, ResourceTable, Type, Val,
    },
    Engine, Store,
};

#[cfg(feature = "wasi")]
use wasmtime_wasi;

use crate::component::{
    CaseType, Component, ComponentMetadata, ComponentTypeKind, ComponentValueType,
    FieldType, FunctionDefinition, InstanceInfo, InterfaceDefinition, Parameter,
    TypeDefinition,
};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Enhanced component engine with actual Wasmtime component model integration
pub struct EnhancedComponentEngine {
    /// Active component instances with metadata
    pub(crate) instances: Arc<RwLock<HashMap<u64, ComponentInstanceHandle>>>,
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

        Ok(EnhancedComponentEngine {
            instances: Arc::new(RwLock::new(HashMap::new())),
            engine,
            next_instance_id: Arc::new(Mutex::new(1)),
            metrics: Arc::new(RwLock::new(ComponentMetrics::default())),
        })
    }

    /// Get a reference to the underlying Wasmtime engine
    ///
    /// # Returns
    ///
    /// Returns a new `EnhancedComponentEngine` with the provided configuration.
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

    /// Look up a core module exported by a component instance
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance
    /// * `name` - The name of the module export to look up
    ///
    /// # Returns
    ///
    /// Returns the core module if found, or None.
    pub fn get_component_instance_module(
        &self,
        instance_id: u64,
        name: &str,
    ) -> WasmtimeResult<Option<wasmtime::Module>> {
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

        handle.last_accessed = Instant::now();
        Ok(handle.instance.get_module(&mut handle.store, name))
    }

    /// Look up a resource type exported by a component instance
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance
    /// * `name` - The name of the resource export to look up
    ///
    /// # Returns
    ///
    /// Returns 1 if found, 0 if not found. Resource type info is written to out params.
    pub fn get_component_instance_resource(
        &self,
        instance_id: u64,
        name: &str,
    ) -> WasmtimeResult<bool> {
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

        handle.last_accessed = Instant::now();
        Ok(handle
            .instance
            .get_resource(&mut handle.store, name)
            .is_some())
    }

    /// Check if a component instance has a specific function export
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance
    /// * `name` - The name of the function to check
    ///
    /// # Returns
    ///
    /// Returns true if the function exists, false otherwise.
    pub fn has_component_instance_func(
        &self,
        instance_id: u64,
        name: &str,
    ) -> WasmtimeResult<bool> {
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

        handle.last_accessed = Instant::now();
        Ok(handle
            .instance
            .get_func(&mut handle.store, name)
            .is_some())
    }

    /// Check if a component instance has a function at the given export index.
    ///
    /// This uses `ComponentExportIndex` for O(1) lookup instead of string-based O(n) lookup.
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The unique identifier for the component instance
    /// * `export_index` - The pre-computed export index
    ///
    /// # Returns
    ///
    /// Returns true if the function exists at the index, false otherwise.
    pub fn has_component_instance_func_by_index(
        &self,
        instance_id: u64,
        export_index: &wasmtime::component::ComponentExportIndex,
    ) -> WasmtimeResult<bool> {
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

        handle.last_accessed = Instant::now();
        Ok(handle
            .instance
            .get_func(&mut handle.store, export_index)
            .is_some())
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

    /// Create enhanced component engine with full feature set
    pub fn create_enhanced_component_engine() -> WasmtimeResult<Box<EnhancedComponentEngine>> {
        EnhancedComponentEngine::new().map(Box::new)
    }

    /// Get enhanced component engine reference
    pub unsafe fn get_enhanced_component_engine_ref(
        engine_ptr: *const c_void,
    ) -> WasmtimeResult<&'static EnhancedComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "enhanced component engine");
        Ok(&*(engine_ptr as *const EnhancedComponentEngine))
    }

    /// Load component using enhanced engine
    pub fn load_component_from_bytes_enhanced(
        engine: &EnhancedComponentEngine,
        bytes: &[u8],
    ) -> WasmtimeResult<Box<Component>> {
        validate_not_empty!(bytes, "component bytes");
        engine.load_component_from_bytes(bytes).map(Box::new)
    }

    /// Destroy enhanced component engine
    pub unsafe fn destroy_enhanced_component_engine(engine_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<EnhancedComponentEngine>(
            engine_ptr,
            "EnhancedComponentEngine",
        );
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

}
