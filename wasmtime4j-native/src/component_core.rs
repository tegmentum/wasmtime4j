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
    AsContextMut, Engine, Store,
};

#[cfg(feature = "wasi")]
use wasmtime_wasi;

use crate::component::{
    CaseType, Component, ComponentMetadata, ComponentTypeKind, ComponentValueType, FieldType,
    FunctionDefinition, InstanceInfo, InterfaceDefinition, Parameter, TypeDefinition,
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
    /// Original component bytes for re-compilation (e.g., with concurrent engine)
    pub component_bytes: Arc<Vec<u8>>,
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
                metrics.avg_instantiation_time = Duration::from_nanos(new_avg.max(0) as u64);
            }
        }

        Ok(Component::new(component, metadata, bytes.to_vec()))
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
            component_bytes: Arc::clone(component.original_bytes()),
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
        Ok(handle.instance.get_func(&mut handle.store, name).is_some())
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
            Type::Char => ComponentValueType::Char,
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

    /// Execute multiple component function calls concurrently using Wasmtime's
    /// native concurrent call support.
    ///
    /// This method re-compiles and re-instantiates the component with a
    /// concurrent-capable engine (async + component-model-async), then uses
    /// `StoreContextMut::run_concurrent` + `Func::call_concurrent` for true
    /// Wasmtime-level concurrency within a single store.
    ///
    /// # Arguments
    ///
    /// * `instance_id` - The instance whose component bytes will be used
    /// * `calls` - List of (function_name, parameters) to execute concurrently
    ///
    /// # Returns
    ///
    /// Returns a vector of result vectors, one per call, in the same order as input.
    ///
    /// # Notes
    ///
    /// The concurrent instance is separate from the sync instance. State
    /// modifications in concurrent calls are NOT visible to the sync instance.
    pub fn run_concurrent_calls(
        &self,
        instance_id: u64,
        calls: Vec<(String, Vec<Val>)>,
    ) -> WasmtimeResult<Vec<Vec<Val>>> {
        if calls.is_empty() {
            return Ok(Vec::new());
        }

        // Get the component bytes from the existing instance
        let component_bytes = {
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
            Arc::clone(&handle.component_bytes)
        };

        // Get or create concurrent engine
        let concurrent_engine = crate::engine::get_shared_concurrent_component_engine();

        // Compile component with concurrent engine
        let component = WasmtimeComponent::new(&concurrent_engine, component_bytes.as_slice())
            .map_err(|e| WasmtimeError::Compilation {
                message: format!(
                    "Failed to compile component for concurrent execution: {}",
                    e
                ),
            })?;

        // Use Tokio runtime for async operations
        let runtime = crate::async_runtime::get_async_runtime();

        runtime.block_on(async {
            // Create store for concurrent execution
            let store_data = ComponentStoreData {
                instance_id: 0,
                resource_table: ResourceTable::new(),
                #[cfg(feature = "wasi")]
                wasi_ctx: wasmtime_wasi::WasiCtxBuilder::new().build(),
                start_time: Instant::now(),
            };

            let mut store = Store::new(&concurrent_engine, store_data);

            // Create linker with async WASI support
            let mut linker: ComponentLinker<ComponentStoreData> =
                ComponentLinker::new(&concurrent_engine);

            #[cfg(feature = "wasi")]
            {
                wasmtime_wasi::p2::add_to_linker_async(&mut linker).map_err(|e| {
                    WasmtimeError::Instance {
                        message: format!("Failed to add WASI to concurrent linker: {}", e),
                    }
                })?;
            }

            // Instantiate asynchronously (required for async engine)
            let instance = linker
                .instantiate_async(&mut store, &component)
                .await
                .map_err(|e| WasmtimeError::Instance {
                    message: format!(
                        "Failed to instantiate component for concurrent execution: {}",
                        e
                    ),
                })?;

            // Look up all functions and their result counts before entering concurrent scope
            let mut funcs = Vec::with_capacity(calls.len());
            let mut result_counts = Vec::with_capacity(calls.len());

            for (name, _) in &calls {
                let func = instance.get_func(&mut store, name).ok_or_else(|| {
                    WasmtimeError::ImportExport {
                        message: format!("Function '{}' not found in concurrent instance", name),
                    }
                })?;
                let count = func.ty(&store).results().len();
                funcs.push(func);
                result_counts.push(count);
            }

            // Run concurrent scope using Wasmtime's native concurrent call support
            let concurrent_results = store
                .as_context_mut()
                .run_concurrent(async |accessor| {
                    let mut futures = Vec::with_capacity(calls.len());

                    for (i, (func, (_, params))) in funcs.iter().zip(calls.iter()).enumerate() {
                        let func = *func;
                        let result_count = result_counts[i];
                        let params = params.as_slice();
                        let accessor = &accessor;

                        futures.push(async move {
                            let mut results = vec![Val::Bool(false); result_count];
                            func.call_concurrent(accessor, params, &mut results)
                                .await
                                .map_err(|e| WasmtimeError::Runtime {
                                    message: format!("Concurrent call failed: {}", e),
                                    backtrace: None,
                                })?;
                            Ok::<Vec<Val>, WasmtimeError>(results)
                        });
                    }

                    futures::future::join_all(futures).await
                })
                .await
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to run concurrent scope: {}", e),
                    backtrace: None,
                })?;

            // Collect results, propagating any errors
            let mut final_results = Vec::with_capacity(concurrent_results.len());
            for result in concurrent_results {
                final_results.push(result?);
            }

            Ok(final_results)
        })
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

/// JSON serialization types for concurrent component calls.
///
/// These types enable simple JSON-based serialization of concurrent call
/// batches across the FFI boundary. This is used instead of the binary
/// WitValueFFI format because the concurrent call path already involves
/// expensive operations (re-compilation, re-instantiation), making JSON
/// overhead negligible.
pub mod concurrent_call_json {
    use serde::{Deserialize, Serialize};
    use wasmtime::component::Val;

    use crate::error::{WasmtimeError, WasmtimeResult};

    /// A single concurrent call in JSON format
    #[derive(Debug, Serialize, Deserialize)]
    pub struct JsonConcurrentCall {
        /// Function name to call
        pub name: String,
        /// Function arguments
        pub args: Vec<JsonVal>,
    }

    /// A component value in JSON-serializable format
    #[derive(Debug, Clone, Serialize, Deserialize)]
    #[serde(tag = "type", content = "value")]
    pub enum JsonVal {
        /// Boolean value
        Bool(bool),
        /// Signed 8-bit integer
        S8(i8),
        /// Signed 16-bit integer
        S16(i16),
        /// Signed 32-bit integer
        S32(i32),
        /// Signed 64-bit integer
        S64(i64),
        /// Unsigned 8-bit integer
        U8(u8),
        /// Unsigned 16-bit integer
        U16(u16),
        /// Unsigned 32-bit integer
        U32(u32),
        /// Unsigned 64-bit integer
        U64(u64),
        /// 32-bit float
        Float32(f32),
        /// 64-bit float
        Float64(f64),
        /// Unicode character
        Char(char),
        /// String value
        String(String),
        /// List of values
        List(Vec<JsonVal>),
        /// Record (named fields in definition order)
        Record(Vec<(String, JsonVal)>),
        /// Tuple
        Tuple(Vec<JsonVal>),
        /// Variant (discriminant name + optional payload)
        Variant {
            discriminant: String,
            value: Option<Box<JsonVal>>,
        },
        /// Enum (discriminant name)
        Enum(String),
        /// Optional value
        Option(Option<Box<JsonVal>>),
        /// Result value (ok or err)
        Result {
            ok: Option<Box<JsonVal>>,
            err: Option<Box<JsonVal>>,
            is_ok: bool,
        },
        /// Bit flags (set of active flag names)
        Flags(Vec<String>),
    }

    impl JsonVal {
        /// Convert a Wasmtime `Val` to a `JsonVal`
        pub fn from_val(val: &Val) -> WasmtimeResult<Self> {
            match val {
                Val::Bool(v) => Ok(JsonVal::Bool(*v)),
                Val::S8(v) => Ok(JsonVal::S8(*v)),
                Val::S16(v) => Ok(JsonVal::S16(*v)),
                Val::S32(v) => Ok(JsonVal::S32(*v)),
                Val::S64(v) => Ok(JsonVal::S64(*v)),
                Val::U8(v) => Ok(JsonVal::U8(*v)),
                Val::U16(v) => Ok(JsonVal::U16(*v)),
                Val::U32(v) => Ok(JsonVal::U32(*v)),
                Val::U64(v) => Ok(JsonVal::U64(*v)),
                Val::Float32(v) => Ok(JsonVal::Float32(*v)),
                Val::Float64(v) => Ok(JsonVal::Float64(*v)),
                Val::Char(v) => Ok(JsonVal::Char(*v)),
                Val::String(v) => Ok(JsonVal::String(v.to_string())),
                Val::List(items) => {
                    let json_items: WasmtimeResult<Vec<JsonVal>> =
                        items.iter().map(JsonVal::from_val).collect();
                    Ok(JsonVal::List(json_items?))
                }
                Val::Record(fields) => {
                    let json_fields: WasmtimeResult<Vec<(String, JsonVal)>> = fields
                        .iter()
                        .map(|(name, val)| {
                            JsonVal::from_val(val).map(|jv| (name.clone(), jv))
                        })
                        .collect();
                    Ok(JsonVal::Record(json_fields?))
                }
                Val::Tuple(elements) => {
                    let json_elements: WasmtimeResult<Vec<JsonVal>> =
                        elements.iter().map(JsonVal::from_val).collect();
                    Ok(JsonVal::Tuple(json_elements?))
                }
                Val::Variant(discriminant, payload) => Ok(JsonVal::Variant {
                    discriminant: discriminant.clone(),
                    value: payload
                        .as_ref()
                        .map(|v| JsonVal::from_val(v).map(Box::new))
                        .transpose()?,
                }),
                Val::Enum(discriminant) => Ok(JsonVal::Enum(discriminant.clone())),
                Val::Option(opt) => Ok(JsonVal::Option(
                    opt.as_ref()
                        .map(|v| JsonVal::from_val(v).map(Box::new))
                        .transpose()?,
                )),
                Val::Result(res) => match res {
                    Ok(ok_val) => Ok(JsonVal::Result {
                        ok: ok_val
                            .as_ref()
                            .map(|v| JsonVal::from_val(v).map(Box::new))
                            .transpose()?,
                        err: None,
                        is_ok: true,
                    }),
                    Err(err_val) => Ok(JsonVal::Result {
                        ok: None,
                        err: err_val
                            .as_ref()
                            .map(|v| JsonVal::from_val(v).map(Box::new))
                            .transpose()?,
                        is_ok: false,
                    }),
                },
                Val::Flags(v) => Ok(JsonVal::Flags(v.clone())),
                Val::Resource(_) => Err(WasmtimeError::InvalidParameter {
                    message: "Resource values cannot be serialized for concurrent calls"
                        .to_string(),
                }),
                Val::Future(_) | Val::Stream(_) | Val::ErrorContext(_) => {
                    Err(WasmtimeError::InvalidParameter {
                        message: "Async values (future/stream/error-context) cannot be serialized for concurrent calls"
                            .to_string(),
                    })
                }
            }
        }

        /// Convert a `JsonVal` to a Wasmtime `Val`
        pub fn to_val(&self) -> WasmtimeResult<Val> {
            match self {
                JsonVal::Bool(v) => Ok(Val::Bool(*v)),
                JsonVal::S8(v) => Ok(Val::S8(*v)),
                JsonVal::S16(v) => Ok(Val::S16(*v)),
                JsonVal::S32(v) => Ok(Val::S32(*v)),
                JsonVal::S64(v) => Ok(Val::S64(*v)),
                JsonVal::U8(v) => Ok(Val::U8(*v)),
                JsonVal::U16(v) => Ok(Val::U16(*v)),
                JsonVal::U32(v) => Ok(Val::U32(*v)),
                JsonVal::U64(v) => Ok(Val::U64(*v)),
                JsonVal::Float32(v) => Ok(Val::Float32(*v)),
                JsonVal::Float64(v) => Ok(Val::Float64(*v)),
                JsonVal::Char(v) => Ok(Val::Char(*v)),
                JsonVal::String(v) => Ok(Val::String(v.clone().into())),
                JsonVal::List(items) => {
                    let vals: WasmtimeResult<Vec<Val>> =
                        items.iter().map(JsonVal::to_val).collect();
                    Ok(Val::List(vals?.into()))
                }
                JsonVal::Record(fields) => {
                    let vals: WasmtimeResult<Vec<(String, Val)>> = fields
                        .iter()
                        .map(|(name, jv)| jv.to_val().map(|v| (name.clone(), v)))
                        .collect();
                    Ok(Val::Record(vals?))
                }
                JsonVal::Tuple(elements) => {
                    let vals: WasmtimeResult<Vec<Val>> =
                        elements.iter().map(JsonVal::to_val).collect();
                    Ok(Val::Tuple(vals?.into()))
                }
                JsonVal::Variant {
                    discriminant,
                    value,
                } => Ok(Val::Variant(
                    discriminant.clone(),
                    value
                        .as_ref()
                        .map(|v| v.to_val().map(Box::new))
                        .transpose()?,
                )),
                JsonVal::Enum(v) => Ok(Val::Enum(v.clone())),
                JsonVal::Option(opt) => Ok(Val::Option(
                    opt.as_ref().map(|v| v.to_val().map(Box::new)).transpose()?,
                )),
                JsonVal::Result { ok, err, is_ok } => {
                    if *is_ok {
                        Ok(Val::Result(Ok(ok
                            .as_ref()
                            .map(|v| v.to_val().map(Box::new))
                            .transpose()?)))
                    } else {
                        Ok(Val::Result(Err(err
                            .as_ref()
                            .map(|v| v.to_val().map(Box::new))
                            .transpose()?)))
                    }
                }
                JsonVal::Flags(v) => Ok(Val::Flags(v.clone())),
            }
        }
    }

    /// Deserialize a JSON batch of concurrent calls into the format expected by
    /// `EnhancedComponentEngine::run_concurrent_calls`.
    pub fn deserialize_calls(json: &str) -> WasmtimeResult<Vec<(String, Vec<Val>)>> {
        let calls: Vec<JsonConcurrentCall> =
            serde_json::from_str(json).map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Failed to deserialize concurrent calls JSON: {}", e),
            })?;

        let mut result = Vec::with_capacity(calls.len());
        for call in calls {
            let params: WasmtimeResult<Vec<Val>> = call.args.iter().map(JsonVal::to_val).collect();
            result.push((call.name, params?));
        }

        Ok(result)
    }

    /// Serialize concurrent call results to JSON.
    pub fn serialize_results(results: &[Vec<Val>]) -> WasmtimeResult<String> {
        let json_results: WasmtimeResult<Vec<Vec<JsonVal>>> = results
            .iter()
            .map(|result_vals| result_vals.iter().map(JsonVal::from_val).collect())
            .collect();

        serde_json::to_string(&json_results?).map_err(|e| WasmtimeError::Internal {
            message: format!("Failed to serialize concurrent call results: {}", e),
        })
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
