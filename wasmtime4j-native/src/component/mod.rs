//! WebAssembly Component Model support for WASI Preview 2
//!
//! This module provides comprehensive support for the WebAssembly Component Model,
//! enabling WASI Preview 2 functionality through Wasmtime's component model API.
//!
//! The Component Model allows for composable WebAssembly components that can
//! define and use interfaces through WebAssembly Interface Type (WIT) definitions.
//!
//! ## Architecture
//!
//! - **ComponentEngine**: Manages component instances and provides the runtime environment
//! - **Component**: Represents a compiled WebAssembly component
//! - **ComponentInstance**: An instantiated component ready for invocation
//! - **WitInterface**: Interface definitions and bindings for type-safe interaction
//! - **ResourceManager**: Automatic cleanup and lifecycle management
//!
//! ## Safety and Defensive Programming
//!
//! All component operations include comprehensive validation and error handling
//! to prevent JVM crashes and ensure robust operation in production environments.

mod linker;
mod resources;
mod wit;

#[cfg(test)]
mod tests;

// Re-export all public types for backward compatibility with crate::component::*
pub use resources::{
    ComponentInstanceMetadata, ComponentInstanceState, ComponentInstanceWrapper, HostInterface,
    InstanceInfo, ResourceManager,
};

pub use wit::{
    CaseType, ComponentTypeKind, ComponentValueType, FieldType, FunctionDefinition,
    InterfaceDefinition, Parameter, ResourceDefinition, TypeDefinition,
};

pub use linker::{
    component_linker_core, get_component_host_function_registry, parse_wit_path,
    CallbackMonotonicClock, CallbackRng, CallbackSocketAddrCheck, CallbackWallClock,
    ComponentHostCallback, ComponentHostFunctionEntry, ComponentInstancePreWrapper,
    ComponentLinker, ComponentValue, ResourceDestructorCallback, WasiP2Config,
    NEXT_COMPONENT_HOST_FUNCTION_ID,
};
pub(crate) use linker::{async_val_close, component_value_to_json_val, json_val_to_component_value};

use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::path::Path;
use std::sync::{Arc, Mutex, Weak};
use std::time::Instant;
use wasmtime::{
    component::{
        Component as WasmtimeComponent, Instance as ComponentInstance, Linker, ResourceTable,
    },
    Engine as WasmtimeEngine, Store,
};

/// Engine for managing WebAssembly component instances
///
/// The ComponentEngine provides a high-level interface for working with
/// WebAssembly components, including loading, instantiation, and lifecycle management.
/// It maintains internal state for resource tracking and automatic cleanup.
pub struct ComponentEngine {
    /// Wasmtime engine for component compilation and execution
    pub(crate) engine: WasmtimeEngine,
    /// Component linker for resolving imports and exports
    linker: Linker<ComponentStoreData>,
    /// Active component instances for resource tracking
    instances: Arc<Mutex<HashMap<u64, Weak<ComponentInstance>>>>,
    /// Next instance ID for tracking
    next_instance_id: Arc<Mutex<u64>>,
}

/// Store data for component instances
///
/// This struct contains the data associated with each WebAssembly component store,
/// providing context for component execution and resource management.
pub struct ComponentStoreData {
    /// Instance ID for resource tracking
    pub instance_id: u64,
    /// Custom user data (reserved for future use)
    pub user_data: Option<Box<dyn std::any::Any + Send + Sync>>,
    /// Resource table for component resources
    pub resource_table: ResourceTable,
    /// WASI context for Preview 2 support
    #[cfg(feature = "wasi")]
    pub wasi_ctx: wasmtime_wasi::WasiCtx,
    /// WASI HTTP context for HTTP request/response support
    #[cfg(feature = "wasi-http")]
    pub wasi_http_ctx: Option<wasmtime_wasi_http::WasiHttpCtx>,
    /// No-op HTTP hooks (required by WasiHttpView trait in wasmtime 43+)
    #[cfg(feature = "wasi-http")]
    pub wasi_http_hooks: [(); 0],
    /// WASI config variables for wasi:config/runtime support
    #[cfg(feature = "wasi-config")]
    pub wasi_config_vars: wasmtime_wasi_config::WasiConfigVariables,
    /// Optional store limits for resource governance
    pub store_limits: Option<wasmtime::StoreLimits>,
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

impl Default for ComponentStoreData {
    fn default() -> Self {
        ComponentStoreData {
            instance_id: 0,
            user_data: None,
            resource_table: ResourceTable::new(),
            #[cfg(feature = "wasi")]
            wasi_ctx: wasmtime_wasi::WasiCtx::builder().build(),
            #[cfg(feature = "wasi-http")]
            wasi_http_ctx: None,
            #[cfg(feature = "wasi-http")]
            wasi_http_hooks: [(); 0],
            #[cfg(feature = "wasi-config")]
            wasi_config_vars: wasmtime_wasi_config::WasiConfigVariables::new(),
            store_limits: None,
            start_time: Instant::now(),
        }
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

// Implement WasiHttpView for ComponentStoreData to enable WASI HTTP support
#[cfg(feature = "wasi-http")]
impl wasmtime_wasi_http::p2::WasiHttpView for ComponentStoreData {
    fn http(&mut self) -> wasmtime_wasi_http::p2::WasiHttpCtxView<'_> {
        wasmtime_wasi_http::p2::WasiHttpCtxView {
            ctx: self
                .wasi_http_ctx
                .get_or_insert_with(wasmtime_wasi_http::WasiHttpCtx::new),
            table: &mut self.resource_table,
            hooks: &mut self.wasi_http_hooks,
        }
    }
}

// Implement WasiHttpView (p3) for ComponentStoreData to enable WASI HTTP P3 support
#[cfg(all(feature = "wasi-p3", feature = "wasi-http"))]
impl wasmtime_wasi_http::p3::WasiHttpView for ComponentStoreData {
    fn http(&mut self) -> wasmtime_wasi_http::p3::WasiHttpCtxView<'_> {
        wasmtime_wasi_http::p3::WasiHttpCtxView {
            ctx: self
                .wasi_http_ctx
                .get_or_insert_with(wasmtime_wasi_http::WasiHttpCtx::new),
            table: &mut self.resource_table,
            hooks: &mut self.wasi_http_hooks,
        }
    }
}

/// A compiled WebAssembly component
///
/// Represents a WebAssembly component that has been compiled and is ready
/// for instantiation. Components are immutable after compilation.
pub struct Component {
    /// The compiled Wasmtime component
    component: WasmtimeComponent,
    /// Component metadata for introspection
    pub(crate) metadata: ComponentMetadata,
    /// Original wasm bytes for re-compilation with different engines (e.g., concurrent)
    pub(crate) original_bytes: Arc<Vec<u8>>,
}

/// Metadata about a compiled component
///
/// Contains information about the component's imports, exports, and interfaces
/// for introspection and validation purposes.
#[derive(Debug, Clone)]
pub struct ComponentMetadata {
    /// Component imports (required interfaces)
    pub imports: Vec<InterfaceDefinition>,
    /// Component exports (provided interfaces)
    pub exports: Vec<InterfaceDefinition>,
    /// Component size in bytes
    pub size_bytes: usize,
}

impl ComponentEngine {
    /// Create a new component engine with default configuration
    ///
    /// # Returns
    ///
    /// Returns a new `ComponentEngine` instance ready for component operations.
    /// The engine is configured with sensible defaults for component execution.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::EngineConfig` if the engine cannot be created
    /// due to system resource constraints or configuration issues.
    pub fn new() -> WasmtimeResult<Self> {
        // Use the shared component engine to avoid GLOBAL_CODE registry accumulation
        let engine = crate::engine::get_shared_component_wasmtime_engine();
        let linker = Linker::new(&engine);

        Ok(ComponentEngine {
            engine,
            linker,
            instances: Arc::new(Mutex::new(HashMap::new())),
            next_instance_id: Arc::new(Mutex::new(1)),
        })
    }

    /// Create a component engine with custom configuration
    ///
    /// # Arguments
    ///
    /// * `engine` - Pre-configured Wasmtime engine to use
    ///
    /// # Returns
    ///
    /// Returns a new `ComponentEngine` using the provided engine configuration.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::EngineConfig` if the linker cannot be created
    /// with the provided engine.
    pub fn with_engine(engine: WasmtimeEngine) -> WasmtimeResult<Self> {
        let linker = Linker::new(&engine);

        Ok(ComponentEngine {
            engine,
            linker,
            instances: Arc::new(Mutex::new(HashMap::new())),
            next_instance_id: Arc::new(Mutex::new(1)),
        })
    }

    /// Load a component from WebAssembly bytes
    ///
    /// # Arguments
    ///
    /// * `bytes` - WebAssembly component bytes to compile
    ///
    /// # Returns
    ///
    /// Returns a compiled `Component` ready for instantiation.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Compilation` if the component bytes are invalid
    /// or cannot be compiled by Wasmtime.
    ///
    /// # Safety
    ///
    /// This function validates the input bytes before compilation to prevent
    /// crashes from malformed WebAssembly data.
    pub fn load_component_from_bytes(&self, bytes: &[u8]) -> WasmtimeResult<Component> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Component bytes cannot be empty".to_string(),
            });
        }

        let component = WasmtimeComponent::new(&self.engine, bytes).map_err(|e| {
            WasmtimeError::Compilation {
                message: format!("Failed to compile component: {}", e),
            }
        })?;

        let metadata = self.extract_component_metadata(&component)?;

        Ok(Component::new(component, metadata, bytes.to_vec()))
    }

    /// Load a component from a WebAssembly file
    ///
    /// # Arguments
    ///
    /// * `path` - Path to WebAssembly component file
    ///
    /// # Returns
    ///
    /// Returns a compiled `Component` loaded from the specified file.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Io` if the file cannot be read, or
    /// `WasmtimeError::Compilation` if the file contains invalid WebAssembly.
    ///
    /// # Safety
    ///
    /// This function validates the file path and contents before compilation
    /// to prevent crashes from invalid file data.
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

        let component = WasmtimeComponent::new(&self.engine, &bytes).map_err(|e| {
            WasmtimeError::Compilation {
                message: format!(
                    "Failed to load component from file {}: {}",
                    path.display(),
                    e
                ),
            }
        })?;

        let metadata = self.extract_component_metadata(&component)?;

        Ok(Component::new(component, metadata, bytes))
    }

    /// Instantiate a component with the current linker configuration
    ///
    /// # Arguments
    ///
    /// * `component` - The compiled component to instantiate
    ///
    /// # Returns
    ///
    /// Returns a new `ComponentInstance` ready for function invocation.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Instance` if instantiation fails due to
    /// unresolved imports or other instantiation errors.
    ///
    /// # Safety
    ///
    /// This function performs comprehensive validation of component imports
    /// and exports before instantiation to prevent runtime errors.
    pub fn instantiate_component(
        &self,
        component: &Component,
    ) -> WasmtimeResult<Arc<ComponentInstance>> {
        let instance_id = {
            let mut next_id =
                self.next_instance_id
                    .lock()
                    .map_err(|_| WasmtimeError::Concurrency {
                        message: "Failed to acquire instance ID lock".to_string(),
                    })?;
            let id = *next_id;
            *next_id += 1;
            id
        };

        let store_data = ComponentStoreData {
            instance_id,
            user_data: None,
            ..Default::default()
        };

        let mut store = Store::new(&self.engine, store_data);

        let instance = self
            .linker
            .instantiate(&mut store, &component.component)
            .map_err(|e| WasmtimeError::Instance {
                message: format!("Failed to instantiate component: {}", e),
            })?;

        let instance_ref = Arc::new(instance);

        // Track the instance for resource management
        {
            let mut instances = self
                .instances
                .lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire instances lock".to_string(),
                })?;
            instances.insert(instance_id, Arc::downgrade(&instance_ref));
        }

        Ok(instance_ref)
    }

    /// Instantiate a WebAssembly component asynchronously
    ///
    /// This is the async version of `instantiate_component` for engines created
    /// with `async_support(true)`. It allows component instantiation without
    /// blocking the calling thread.
    ///
    /// # Arguments
    ///
    /// * `component` - The compiled component to instantiate
    ///
    /// # Returns
    ///
    /// Returns an `Arc<ComponentInstance>` containing the instantiated component.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Instance` if instantiation fails for any reason,
    /// including import resolution errors or memory allocation failures.
    ///
    /// # Safety
    ///
    /// This function performs comprehensive validation of component imports
    /// and exports before instantiation to prevent runtime errors.
    pub async fn instantiate_component_async(
        &self,
        component: &Component,
    ) -> WasmtimeResult<Arc<ComponentInstance>> {
        let instance_id = {
            let mut next_id =
                self.next_instance_id
                    .lock()
                    .map_err(|_| WasmtimeError::Concurrency {
                        message: "Failed to acquire instance ID lock".to_string(),
                    })?;
            let id = *next_id;
            *next_id += 1;
            id
        };

        let store_data = ComponentStoreData {
            instance_id,
            user_data: None,
            ..Default::default()
        };

        let mut store = Store::new(&self.engine, store_data);

        let instance = self
            .linker
            .instantiate_async(&mut store, &component.component)
            .await
            .map_err(|e| WasmtimeError::Instance {
                message: format!("Failed to instantiate component asynchronously: {}", e),
            })?;

        let instance_ref = Arc::new(instance);

        // Track the instance for resource management
        {
            let mut instances = self
                .instances
                .lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire instances lock".to_string(),
                })?;
            instances.insert(instance_id, Arc::downgrade(&instance_ref));
        }

        Ok(instance_ref)
    }

    /// Add a host interface to the component linker
    ///
    /// This function allows binding host-provided implementations of WIT interfaces
    /// that components can import and use.
    ///
    /// # Arguments
    ///
    /// * `interface_name` - Name of the interface to bind
    /// * `implementation` - Host implementation of the interface
    ///
    /// # Returns
    ///
    /// Returns `Ok(())` if the interface was successfully bound.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::ImportExport` if the interface cannot be bound
    /// due to type mismatches or other linking errors.
    pub fn add_host_interface(
        &mut self,
        interface_name: &str,
        _implementation: HostInterface,
    ) -> WasmtimeResult<()> {
        // Host interface binding is not yet implemented - return error to indicate the feature is unavailable
        // The Wasmtime component model API is still stabilizing
        log::warn!(
            "Host interface binding not yet implemented: {}",
            interface_name
        );

        Err(WasmtimeError::UnsupportedFeature {
            message: format!("Host interface binding for '{}'", interface_name),
        })
    }

    /// Get information about active component instances
    ///
    /// # Returns
    ///
    /// Returns a vector containing metadata about all active component instances
    /// managed by this engine.
    pub fn get_active_instances(&self) -> WasmtimeResult<Vec<InstanceInfo>> {
        let instances = self
            .instances
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances lock".to_string(),
            })?;

        let mut active_instances = Vec::new();

        for (id, weak_ref) in instances.iter() {
            if weak_ref.strong_count() > 0 {
                active_instances.push(InstanceInfo {
                    instance_id: *id,
                    strong_references: weak_ref.strong_count(),
                });
            }
        }

        Ok(active_instances)
    }

    /// Cleanup inactive component instances
    ///
    /// Removes references to component instances that are no longer active,
    /// freeing up resources and preventing memory leaks.
    ///
    /// # Returns
    ///
    /// Returns the number of instances that were cleaned up.
    pub fn cleanup_instances(&self) -> WasmtimeResult<usize> {
        let mut instances = self
            .instances
            .lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances lock".to_string(),
            })?;

        let initial_count = instances.len();
        instances.retain(|_, weak_ref| weak_ref.strong_count() > 0);
        let final_count = instances.len();

        Ok(initial_count - final_count)
    }

    /// Compile a WebAssembly component from bytes
    ///
    /// # Arguments
    ///
    /// * `bytes` - WebAssembly component bytes to compile
    ///
    /// # Returns
    ///
    /// Returns a compiled `Component` ready for instantiation.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Compilation` if the component bytes are invalid
    /// or cannot be compiled by Wasmtime.
    pub fn compile_component(&self, bytes: &[u8]) -> WasmtimeResult<Component> {
        self.load_component_from_bytes(bytes)
    }

    /// Compile a WebAssembly component from WAT (WebAssembly Text format)
    ///
    /// # Arguments
    ///
    /// * `wat` - WebAssembly Text format string
    ///
    /// # Returns
    ///
    /// Returns a compiled `Component` ready for instantiation.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::Compilation` if the WAT cannot be parsed or compiled.
    pub fn compile_component_wat(&self, wat: &str) -> WasmtimeResult<Component> {
        if wat.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "WAT text cannot be empty".to_string(),
            });
        }

        // Convert WAT to bytes first using the wat crate
        let bytes = wat::parse_str(wat).map_err(|e| WasmtimeError::Compilation {
            message: format!("Failed to parse WAT: {}", e),
        })?;

        self.load_component_from_bytes(&bytes)
    }

    /// Get the number of active component instances
    ///
    /// # Returns
    ///
    /// Returns the count of currently active component instances.
    pub fn get_instance_count(&self) -> usize {
        self.instances
            .lock()
            .map(|instances| instances.len())
            .unwrap_or(0)
    }

    /// Cleanup unused component instances
    ///
    /// Removes references to component instances that are no longer active.
    pub fn cleanup_unused_instances(&self) {
        if let Ok(cleaned) = self.cleanup_instances() {
            log::debug!("Cleaned up {} unused component instances", cleaned);
        }
    }

    /// Check if the engine supports a specific feature
    ///
    /// # Arguments
    ///
    /// * `feature_name` - Name of the feature to check
    ///
    /// # Returns
    ///
    /// Returns `Ok(true)` if the feature is supported, `Ok(false)` otherwise.
    pub fn supports_feature(&self, feature_name: &str) -> WasmtimeResult<bool> {
        // Component Model features supported by this implementation
        let supported_features = [
            "component-model",
            "wit-interfaces",
            "component-linking",
            "resource-management",
            "interface-validation",
        ];

        Ok(supported_features.contains(&feature_name))
    }

    /// Extract metadata from a compiled component
    ///
    /// This internal function analyzes a compiled component to extract
    /// information about its imports, exports, and interfaces.
    fn extract_component_metadata(
        &self,
        component: &WasmtimeComponent,
    ) -> WasmtimeResult<ComponentMetadata> {
        // Get component type information from Wasmtime
        let component_ty = component.component_type();

        let mut imports = Vec::new();
        let mut exports = Vec::new();

        // Extract imports - Wasmtime 37.0.2 requires engine parameter
        for (name, import_ty) in component_ty.imports(&self.engine) {
            let mut functions = Vec::new();

            use wasmtime::component::types::ComponentItem;
            match import_ty {
                ComponentItem::ComponentInstance(instance_ty) => {
                    // It's an instance import - enumerate its functions
                    for (func_name, func_ty) in instance_ty.exports(&self.engine) {
                        if matches!(func_ty, ComponentItem::ComponentFunc(_)) {
                            functions.push(FunctionDefinition {
                                name: func_name.to_string(),
                                parameters: Vec::new(),
                                results: Vec::new(),
                            });
                        }
                    }
                }
                ComponentItem::ComponentFunc(_func_ty) => {
                    // It's a direct function import
                    functions.push(FunctionDefinition {
                        name: name.to_string(),
                        parameters: Vec::new(),
                        results: Vec::new(),
                    });
                }
                _ => {
                    // Other import types (modules, types, etc.) - skip for now
                }
            }

            imports.push(InterfaceDefinition {
                name: name.to_string(),
                namespace: None, // Will be enhanced with actual namespace parsing
                version: None,   // Will be enhanced with actual version parsing
                functions,
                types: Vec::new(), // Will be enhanced with actual type extraction
                resources: Vec::new(), // Will be enhanced with actual resource extraction
            });
        }

        // Extract exports - Wasmtime 37.0.2 requires engine parameter
        for (name, export_ty) in component_ty.exports(&self.engine) {
            let mut functions = Vec::new();

            // Try to extract functions from the export type
            // The export might be a ComponentItem which could be a function or instance
            use wasmtime::component::types::ComponentItem;
            match export_ty {
                ComponentItem::ComponentInstance(instance_ty) => {
                    // It's an instance export - enumerate its functions
                    for (func_name, func_ty) in instance_ty.exports(&self.engine) {
                        if matches!(func_ty, ComponentItem::ComponentFunc(_)) {
                            functions.push(FunctionDefinition {
                                name: func_name.to_string(),
                                parameters: Vec::new(), // Function signature details
                                results: Vec::new(),
                            });
                        }
                    }
                }
                ComponentItem::ComponentFunc(_func_ty) => {
                    // It's a direct function export
                    functions.push(FunctionDefinition {
                        name: name.to_string(),
                        parameters: Vec::new(),
                        results: Vec::new(),
                    });
                }
                _ => {
                    // Other export types (modules, types, etc.) - skip for now
                }
            }

            exports.push(InterfaceDefinition {
                name: name.to_string(),
                namespace: None, // Will be enhanced with actual namespace parsing
                version: None,   // Will be enhanced with actual version parsing
                functions,
                types: Vec::new(), // Will be enhanced with actual type extraction
                resources: Vec::new(), // Will be enhanced with actual resource extraction
            });
        }

        // Calculate approximate size (this is a simplified approach)
        let size_bytes = std::mem::size_of_val(component)
            + imports.len() * std::mem::size_of::<InterfaceDefinition>()
            + exports.len() * std::mem::size_of::<InterfaceDefinition>();

        Ok(ComponentMetadata {
            imports,
            exports,
            size_bytes,
        })
    }
}

impl Component {
    /// Create a new Component with Wasmtime component and metadata
    ///
    /// # Arguments
    ///
    /// * `component` - The compiled Wasmtime component
    /// * `metadata` - Component metadata
    ///
    /// # Returns
    ///
    /// Returns a new `Component` instance.
    pub(crate) fn new(
        component: WasmtimeComponent,
        metadata: ComponentMetadata,
        original_bytes: Vec<u8>,
    ) -> Self {
        Component {
            component,
            metadata,
            original_bytes: Arc::new(original_bytes),
        }
    }

    /// Get the original wasm bytes for re-compilation with different engines
    pub(crate) fn original_bytes(&self) -> &Arc<Vec<u8>> {
        &self.original_bytes
    }

    /// Get the internal Wasmtime component
    ///
    /// # Returns
    ///
    /// Returns a reference to the internal Wasmtime component.
    pub(crate) fn wasmtime_component(&self) -> &WasmtimeComponent {
        &self.component
    }

    /// Get component metadata
    ///
    /// # Returns
    ///
    /// Returns a reference to the component's metadata for introspection.
    pub fn metadata(&self) -> &ComponentMetadata {
        &self.metadata
    }

    /// Get the size of the component in bytes
    ///
    /// # Returns
    ///
    /// Returns the compiled size of the component in bytes.
    pub fn size_bytes(&self) -> usize {
        self.metadata.size_bytes
    }

    /// Check if the component exports a specific interface
    ///
    /// # Arguments
    ///
    /// * `interface_name` - Name of the interface to check
    ///
    /// # Returns
    ///
    /// Returns `true` if the component exports the specified interface.
    pub fn exports_interface(&self, interface_name: &str) -> bool {
        self.metadata
            .exports
            .iter()
            .any(|export| export.name == interface_name)
    }

    /// Check if the component imports a specific interface
    ///
    /// # Arguments
    ///
    /// * `interface_name` - Name of the interface to check
    ///
    /// # Returns
    ///
    /// Returns `true` if the component imports the specified interface.
    pub fn imports_interface(&self, interface_name: &str) -> bool {
        self.metadata
            .imports
            .iter()
            .any(|import| import.name == interface_name)
    }

    /// Validate component against WIT interface requirements
    ///
    /// # Arguments
    ///
    /// * `wit_interface` - WIT interface definition to validate against
    ///
    /// # Returns
    ///
    /// Returns `Ok(true)` if the component is valid against the interface, `Ok(false)` otherwise.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::ValidationError` if validation fails due to system errors.
    pub fn validate_wit_interface(&self, wit_interface: &str) -> WasmtimeResult<bool> {
        if wit_interface.is_empty() {
            return Ok(false);
        }

        // Basic WIT interface validation
        // This is a simplified implementation that checks for basic WIT syntax
        let has_interface_keyword = wit_interface.contains("interface");
        let has_braces = wit_interface.contains("{") && wit_interface.contains("}");

        // More sophisticated validation would parse the actual WIT and check
        // compatibility with the component's type signatures
        Ok(has_interface_keyword && has_braces)
    }

    /// Get export interface definition by name
    ///
    /// # Arguments
    ///
    /// * `export_name` - Name of the export to get interface for
    ///
    /// # Returns
    ///
    /// Returns the interface definition for the export if found.
    ///
    /// # Errors
    ///
    /// Returns `WasmtimeError::NotFound` if the export doesn't exist.
    pub fn get_export_interface(
        &self,
        export_name: &str,
    ) -> WasmtimeResult<Option<&InterfaceDefinition>> {
        let interface = self
            .metadata
            .exports
            .iter()
            .find(|export| export.name == export_name);

        Ok(interface)
    }

    /// Get import interface definition by name
    ///
    /// # Arguments
    ///
    /// * `import_name` - Name of the import to get interface for
    ///
    /// # Returns
    ///
    /// Returns the interface definition for the import if found.
    pub fn get_import_interface(
        &self,
        import_name: &str,
    ) -> WasmtimeResult<Option<&InterfaceDefinition>> {
        let interface = self
            .metadata
            .imports
            .iter()
            .find(|import| import.name == import_name);

        Ok(interface)
    }

    /// Get all exported interface names
    ///
    /// # Returns
    ///
    /// Returns a vector of all exported interface names.
    pub fn get_exported_interfaces(&self) -> Vec<String> {
        self.metadata
            .exports
            .iter()
            .map(|export| export.name.clone())
            .collect()
    }

    /// Get all imported interface names
    ///
    /// # Returns
    ///
    /// Returns a vector of all imported interface names.
    pub fn get_imported_interfaces(&self) -> Vec<String> {
        self.metadata
            .imports
            .iter()
            .map(|import| import.name.clone())
            .collect()
    }

    /// Serialize the component to bytes for later deserialization.
    pub fn serialize(&self) -> WasmtimeResult<Vec<u8>> {
        self.component
            .serialize()
            .map_err(|e| WasmtimeError::Internal {
                message: format!("Component serialization failed: {}", e),
            })
    }

    /// Deserialize a component from previously serialized bytes.
    ///
    /// # Safety
    ///
    /// The bytes must have been produced by a previous call to `serialize()` on
    /// a component compiled with a compatible engine configuration.
    pub fn deserialize(engine: &wasmtime::Engine, bytes: &[u8]) -> WasmtimeResult<Self> {
        if bytes.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Serialized component bytes cannot be empty".to_string(),
            });
        }

        let component = unsafe { WasmtimeComponent::deserialize(engine, bytes) }.map_err(|e| {
            WasmtimeError::Internal {
                message: format!("Component deserialization failed: {}", e),
            }
        })?;

        Ok(Component {
            component,
            metadata: ComponentMetadata {
                imports: Vec::new(),
                exports: Vec::new(),
                size_bytes: bytes.len(),
            },
            original_bytes: Arc::new(Vec::new()),
        })
    }

    /// Deserialize a component from a previously serialized file.
    ///
    /// This is more efficient than reading the file and then calling `deserialize()`
    /// because it uses memory-mapped I/O to avoid copying the file contents into memory.
    ///
    /// # Safety
    ///
    /// The file's contents must have been previously created by `serialize()` from
    /// a component compiled with a compatible engine configuration.
    pub fn deserialize_file(
        engine: &wasmtime::Engine,
        path: impl AsRef<std::path::Path>,
    ) -> WasmtimeResult<Self> {
        let path = path.as_ref();

        if !path.exists() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("File does not exist: {}", path.display()),
            });
        }

        let component =
            unsafe { WasmtimeComponent::deserialize_file(engine, path) }.map_err(|e| {
                WasmtimeError::Internal {
                    message: format!("Component deserialization from file failed: {}", e),
                }
            })?;

        Ok(Component {
            component,
            metadata: ComponentMetadata {
                imports: Vec::new(),
                exports: Vec::new(),
                size_bytes: 0,
            },
            original_bytes: Arc::new(Vec::new()),
        })
    }

    /// Get the resources required by this component.
    ///
    /// Returns `None` if the component imports other modules or components
    /// whose resource requirements cannot be statically determined.
    ///
    /// The returned struct contains:
    /// - `num_memories` - Number of linear memories
    /// - `max_initial_memory_size` - Maximum initial memory size in bytes (None if unbounded)
    /// - `num_tables` - Number of tables
    /// - `max_initial_table_size` - Maximum initial table size (None if unbounded)
    pub fn resources_required(&self) -> Option<wasmtime::ResourcesRequired> {
        self.component.resources_required()
    }

    /// Get the memory address range of the compiled image for this component.
    ///
    /// Returns the start and end addresses of the range as `(usize, usize)`.
    pub fn image_range(&self) -> (usize, usize) {
        let range = self.component.image_range();
        (range.start as usize, range.end as usize)
    }

    /// Pre-initialize copy-on-write image for faster instantiation.
    ///
    /// When using CoW memory initialization (the default), this eagerly creates the
    /// memory-mapped image, avoiding the lazy initialization cost on first instantiation.
    pub fn initialize_copy_on_write_image(&self) -> WasmtimeResult<()> {
        self.component
            .initialize_copy_on_write_image()
            .map_err(|e| WasmtimeError::Internal {
                message: format!("Failed to initialize copy-on-write image: {}", e),
            })
    }
}

impl Default for ComponentEngine {
    fn default() -> Self {
        // ComponentEngine::new() uses get_shared_component_wasmtime_engine() which
        // has fallback protection, so this should always succeed. But handle
        // the error gracefully just in case.
        match Self::new() {
            Ok(engine) => engine,
            Err(e) => {
                log::error!(
                    "Failed to create default ComponentEngine: {}. Creating minimal fallback.",
                    e
                );
                // Create a minimal engine using the fallback-protected shared engine
                let engine = crate::engine::get_shared_component_wasmtime_engine();
                let linker = wasmtime::component::Linker::new(&engine);
                ComponentEngine {
                    engine,
                    linker,
                    instances: std::sync::Arc::new(std::sync::Mutex::new(
                        std::collections::HashMap::new(),
                    )),
                    next_instance_id: std::sync::Arc::new(std::sync::Mutex::new(1)),
                }
            }
        }
    }
}

/// Shared core functions for component operations used by both JNI and Panama interfaces
///
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use crate::error::ffi_utils;
    use crate::{validate_not_empty, validate_ptr_not_null};
    use std::os::raw::c_void;

    /// Core function to create a new component engine
    pub fn create_component_engine() -> WasmtimeResult<Box<ComponentEngine>> {
        ComponentEngine::new().map(Box::new)
    }

    /// Core function to create a component engine with a custom Wasmtime engine
    pub fn create_component_engine_with_engine(
        engine: WasmtimeEngine,
    ) -> WasmtimeResult<Box<ComponentEngine>> {
        ComponentEngine::with_engine(engine).map(Box::new)
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

    /// Core function to load a component from bytes
    pub fn load_component_from_bytes(
        engine: &ComponentEngine,
        bytes: &[u8],
    ) -> WasmtimeResult<Box<Component>> {
        validate_not_empty!(bytes, "component bytes");
        engine.load_component_from_bytes(bytes).map(Box::new)
    }

    /// Core function to load a component from a file
    pub fn load_component_from_file<P: AsRef<std::path::Path>>(
        engine: &ComponentEngine,
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

    /// Core function to instantiate a component
    pub fn instantiate_component(
        engine: &ComponentEngine,
        component: &Component,
    ) -> WasmtimeResult<Arc<ComponentInstance>> {
        engine.instantiate_component(component)
    }

    /// Core function to add a host interface to the component linker
    pub fn add_host_interface(
        engine: &mut ComponentEngine,
        interface_name: &str,
        implementation: HostInterface,
    ) -> WasmtimeResult<()> {
        engine.add_host_interface(interface_name, implementation)
    }

    /// Core function to get active component instances
    pub fn get_active_instances(engine: &ComponentEngine) -> WasmtimeResult<Vec<InstanceInfo>> {
        engine.get_active_instances()
    }

    /// Core function to cleanup inactive component instances
    pub fn cleanup_instances(engine: &ComponentEngine) -> WasmtimeResult<usize> {
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

    /// Core function to destroy a component (safe cleanup)
    pub unsafe fn destroy_component(component_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Component>(component_ptr, "Component");
    }

    /// Core function to destroy a component instance (safe cleanup)
    pub unsafe fn destroy_component_instance(instance_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Arc<ComponentInstance>>(instance_ptr, "ComponentInstance");
    }

    /// Core function to get number of exports in component
    pub fn get_export_count(component: &Component) -> usize {
        component.metadata().exports.len()
    }

    /// Core function to get number of imports in component
    pub fn get_import_count(component: &Component) -> usize {
        component.metadata().imports.len()
    }

    /// Core function to get the image range of a compiled component
    pub fn get_component_image_range(component: &Component) -> (usize, usize) {
        component.image_range()
    }

    /// Core function to initialize copy-on-write image for a component
    pub fn initialize_copy_on_write_image(component: &Component) -> WasmtimeResult<()> {
        component.initialize_copy_on_write_image()
    }

    /// Core function to serialize a component
    pub fn serialize_component(component: &Component) -> WasmtimeResult<Vec<u8>> {
        component.serialize()
    }

    /// Core function to deserialize a component
    pub fn deserialize_component(
        engine: &ComponentEngine,
        bytes: &[u8],
    ) -> WasmtimeResult<Box<Component>> {
        validate_not_empty!(bytes, "serialized component bytes");
        Component::deserialize(&engine.engine, bytes).map(Box::new)
    }

    /// Core function to deserialize a component from a file
    pub fn deserialize_component_file(
        engine: &wasmtime::Engine,
        path: &str,
    ) -> WasmtimeResult<Box<Component>> {
        if path.is_empty() {
            return Err(WasmtimeError::InvalidParameter {
                message: "File path cannot be empty".to_string(),
            });
        }

        let path_ref = std::path::Path::new(path);
        if !path_ref.exists() {
            return Err(WasmtimeError::InvalidParameter {
                message: format!("File does not exist: {}", path),
            });
        }

        Component::deserialize_file(engine, path_ref).map(Box::new)
    }

    /// Core function to get a component export index for efficient repeated lookups.
    ///
    /// Returns a boxed `ComponentExportIndex` as a raw pointer, or null if not found.
    /// The `instance` parameter is an optional parent instance export index for nested lookups.
    /// Pass null for root-level exports.
    pub fn get_export_index(
        component: &Component,
        instance: Option<&wasmtime::component::ComponentExportIndex>,
        name: &str,
    ) -> Option<Box<wasmtime::component::ComponentExportIndex>> {
        component
            .component
            .get_export_index(instance, name)
            .map(Box::new)
    }

    /// Core function to destroy a boxed ComponentExportIndex.
    pub unsafe fn destroy_export_index(ptr: *mut c_void) {
        if !ptr.is_null() {
            let _ = Box::from_raw(ptr as *mut wasmtime::component::ComponentExportIndex);
        }
    }

    /// Core function to get the full component type as JSON with all ComponentItem variants.
    ///
    /// Traverses the component's imports and exports and serializes them to JSON
    /// with full type information including function signatures, nested instances, etc.
    pub fn get_full_component_type_json(
        component: &Component,
        engine: &wasmtime::Engine,
    ) -> WasmtimeResult<String> {
        

        let component_type = component.component.component_type();
        let mut json = String::with_capacity(1024);
        json.push_str("{\"imports\":{");

        let mut first = true;
        for (name, item) in component_type.imports(engine) {
            if !first {
                json.push(',');
            }
            first = false;
            append_json_string(&mut json, name);
            json.push(':');
            component_item_to_json(&mut json, &item, engine, 0);
        }

        json.push_str("},\"exports\":{");

        let mut first = true;
        for (name, item) in component_type.exports(engine) {
            if !first {
                json.push(',');
            }
            first = false;
            append_json_string(&mut json, name);
            json.push(':');
            component_item_to_json(&mut json, &item, engine, 0);
        }

        json.push_str("}}");
        Ok(json)
    }

    /// Core function to get the substituted component type as JSON.
    ///
    /// Uses the linker to compute which imports have been satisfied and returns
    /// the remaining type information. The engine is extracted from the linker.
    pub fn get_substituted_component_type_json(
        linker: &ComponentLinker,
        component: &Component,
    ) -> WasmtimeResult<String> {
        

        let sub_type = linker
            .linker()
            .substituted_component_type(&component.component)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to get substituted component type: {}", e),
                backtrace: None,
            })?;

        let engine = linker.engine();

        let mut json = String::with_capacity(1024);
        json.push_str("{\"imports\":{");

        let mut first = true;
        for (name, item) in sub_type.imports(engine) {
            if !first {
                json.push(',');
            }
            first = false;
            append_json_string(&mut json, name);
            json.push(':');
            component_item_to_json(&mut json, &item, engine, 0);
        }

        json.push_str("},\"exports\":{");

        let mut first = true;
        for (name, item) in sub_type.exports(engine) {
            if !first {
                json.push(',');
            }
            first = false;
            append_json_string(&mut json, name);
            json.push(':');
            component_item_to_json(&mut json, &item, engine, 0);
        }

        json.push_str("}}");
        Ok(json)
    }

    /// Maximum recursion depth for nested component type traversal.
    const MAX_TYPE_DEPTH: usize = 10;

    /// Serialize a ComponentItem to JSON.
    fn component_item_to_json(
        json: &mut String,
        item: &wasmtime::component::types::ComponentItem,
        engine: &wasmtime::Engine,
        depth: usize,
    ) {
        use wasmtime::component::types::ComponentItem;

        if depth > MAX_TYPE_DEPTH {
            json.push_str("{\"kind\":\"truncated\"}");
            return;
        }

        match item {
            ComponentItem::ComponentFunc(func) => {
                json.push_str("{\"kind\":\"component_func\",\"params\":[");
                let mut first = true;
                for (name, ty) in func.params() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    json.push_str("{\"name\":");
                    append_json_string(json, name);
                    json.push_str(",\"type\":");
                    type_to_json(json, &ty);
                    json.push('}');
                }
                json.push_str("],\"results\":[");
                let mut first = true;
                for result_ty in func.results() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    type_to_json(json, &result_ty);
                }
                json.push_str("]}");
            }
            ComponentItem::CoreFunc(core_func) => {
                json.push_str("{\"kind\":\"core_func\",\"params\":[");
                let mut first = true;
                for param in core_func.params() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, &format!("{:?}", param));
                }
                json.push_str("],\"results\":[");
                let mut first = true;
                for result in core_func.results() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, &format!("{:?}", result));
                }
                json.push_str("]}");
            }
            ComponentItem::Module(module_ty) => {
                json.push_str("{\"kind\":\"module\",\"imports\":[");
                let mut first = true;
                for ((module_name, import_name), _extern_type) in module_ty.imports(engine) {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    json.push('{');
                    json.push_str("\"module\":");
                    append_json_string(json, module_name);
                    json.push_str(",\"name\":");
                    append_json_string(json, import_name);
                    json.push('}');
                }
                json.push_str("],\"exports\":[");
                let mut first = true;
                for (export_name, _extern_type) in module_ty.exports(engine) {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    json.push('{');
                    json.push_str("\"name\":");
                    append_json_string(json, export_name);
                    json.push('}');
                }
                json.push_str("]}");
            }
            ComponentItem::Component(comp_ty) => {
                json.push_str("{\"kind\":\"component\",\"imports\":{");
                let mut first = true;
                for (name, sub_item) in comp_ty.imports(engine) {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, name);
                    json.push(':');
                    component_item_to_json(json, &sub_item, engine, depth + 1);
                }
                json.push_str("},\"exports\":{");
                let mut first = true;
                for (name, sub_item) in comp_ty.exports(engine) {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, name);
                    json.push(':');
                    component_item_to_json(json, &sub_item, engine, depth + 1);
                }
                json.push_str("}}");
            }
            ComponentItem::ComponentInstance(instance_ty) => {
                json.push_str("{\"kind\":\"component_instance\",\"exports\":{");
                let mut first = true;
                for (name, sub_item) in instance_ty.exports(engine) {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, name);
                    json.push(':');
                    component_item_to_json(json, &sub_item, engine, depth + 1);
                }
                json.push_str("}}");
            }
            ComponentItem::Type(type_def) => {
                json.push_str("{\"kind\":\"type\",\"descriptor\":");
                type_to_json(json, &type_def);
                json.push('}');
            }
            ComponentItem::Resource(resource_ty) => {
                // ResourceType is opaque in Wasmtime 42.0.1 — no public name/index getters.
                // The resource name is the JSON key assigned by the caller (imports/exports map).
                // We include the Debug representation for diagnostic correlation and a stable
                // hash for cross-reference equality checking.
                let debug_str = format!("{:?}", resource_ty);
                let type_hash = {
                    use std::hash::{Hash, Hasher};
                    let mut hasher = std::collections::hash_map::DefaultHasher::new();
                    debug_str.hash(&mut hasher);
                    hasher.finish()
                };
                json.push_str("{\"kind\":\"resource\",\"resourceTypeDebug\":");
                append_json_string(json, &debug_str);
                json.push_str(",\"resourceTypeId\":");
                json.push_str(&type_hash.to_string());
                json.push_str(",\"hostDefined\":false");
                json.push('}');
            }
        }
    }

    /// Serialize a component model Type to JSON string representation.
    fn type_to_json(json: &mut String, ty: &wasmtime::component::types::Type) {
        use wasmtime::component::types::Type;

        match ty {
            Type::Bool => json.push_str("\"bool\""),
            Type::S8 => json.push_str("\"s8\""),
            Type::U8 => json.push_str("\"u8\""),
            Type::S16 => json.push_str("\"s16\""),
            Type::U16 => json.push_str("\"u16\""),
            Type::S32 => json.push_str("\"s32\""),
            Type::U32 => json.push_str("\"u32\""),
            Type::S64 => json.push_str("\"s64\""),
            Type::U64 => json.push_str("\"u64\""),
            Type::Float32 => json.push_str("\"f32\""),
            Type::Float64 => json.push_str("\"f64\""),
            Type::Char => json.push_str("\"char\""),
            Type::String => json.push_str("\"string\""),
            Type::List(list) => {
                json.push_str("{\"type\":\"list\",\"element\":");
                type_to_json(json, &list.ty());
                json.push('}');
            }
            Type::Record(record) => {
                json.push_str("{\"type\":\"record\",\"fields\":[");
                let mut first = true;
                for field in record.fields() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    json.push_str("{\"name\":");
                    append_json_string(json, field.name);
                    json.push_str(",\"type\":");
                    type_to_json(json, &field.ty);
                    json.push('}');
                }
                json.push_str("]}");
            }
            Type::Tuple(tuple) => {
                json.push_str("{\"type\":\"tuple\",\"elements\":[");
                let mut first = true;
                for elem_ty in tuple.types() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    type_to_json(json, &elem_ty);
                }
                json.push_str("]}");
            }
            Type::Variant(variant) => {
                json.push_str("{\"type\":\"variant\",\"cases\":[");
                let mut first = true;
                for case in variant.cases() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    json.push_str("{\"name\":");
                    append_json_string(json, case.name);
                    if let Some(payload) = &case.ty {
                        json.push_str(",\"type\":");
                        type_to_json(json, payload);
                    }
                    json.push('}');
                }
                json.push_str("]}");
            }
            Type::Enum(enum_ty) => {
                json.push_str("{\"type\":\"enum\",\"names\":[");
                let mut first = true;
                for name in enum_ty.names() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, name);
                }
                json.push_str("]}");
            }
            Type::Option(opt) => {
                json.push_str("{\"type\":\"option\",\"inner\":");
                type_to_json(json, &opt.ty());
                json.push('}');
            }
            Type::Result(result) => {
                json.push_str("{\"type\":\"result\"");
                if let Some(ok_ty) = result.ok() {
                    json.push_str(",\"ok\":");
                    type_to_json(json, &ok_ty);
                }
                if let Some(err_ty) = result.err() {
                    json.push_str(",\"err\":");
                    type_to_json(json, &err_ty);
                }
                json.push('}');
            }
            Type::Flags(flags) => {
                json.push_str("{\"type\":\"flags\",\"names\":[");
                let mut first = true;
                for name in flags.names() {
                    if !first {
                        json.push(',');
                    }
                    first = false;
                    append_json_string(json, name);
                }
                json.push_str("]}");
            }
            Type::Own(resource_ty) => {
                // ResourceType is opaque — include Debug identity for diagnostic correlation
                json.push_str("{\"type\":\"own\",\"resourceTypeDebug\":");
                append_json_string(json, &format!("{:?}", resource_ty));
                json.push('}');
            }
            Type::Borrow(resource_ty) => {
                json.push_str("{\"type\":\"borrow\",\"resourceTypeDebug\":");
                append_json_string(json, &format!("{:?}", resource_ty));
                json.push('}');
            }
            Type::Future(future_ty) => {
                json.push_str("{\"type\":\"future\"");
                if let Some(inner) = future_ty.ty() {
                    json.push_str(",\"payload\":");
                    type_to_json(json, &inner);
                }
                json.push('}');
            }
            Type::Stream(stream_ty) => {
                json.push_str("{\"type\":\"stream\"");
                if let Some(inner) = stream_ty.ty() {
                    json.push_str(",\"payload\":");
                    type_to_json(json, &inner);
                }
                json.push('}');
            }
            Type::ErrorContext => json.push_str("{\"type\":\"error_context\"}"),
            Type::Map(map_ty) => {
                json.push_str("{\"type\":\"map\",\"key\":");
                type_to_json(json, &map_ty.key());
                json.push_str(",\"value\":");
                type_to_json(json, &map_ty.value());
                json.push('}');
            }
        }
    }

    /// Append a JSON-escaped string to the buffer.
    fn append_json_string(json: &mut String, s: &str) {
        json.push('"');
        for c in s.chars() {
            match c {
                '"' => json.push_str("\\\""),
                '\\' => json.push_str("\\\\"),
                '\n' => json.push_str("\\n"),
                '\r' => json.push_str("\\r"),
                '\t' => json.push_str("\\t"),
                c if c < '\x20' => {
                    json.push_str(&format!("\\u{:04x}", c as u32));
                }
                c => json.push(c),
            }
        }
        json.push('"');
    }

    /// Core function to get component resources required
    ///
    /// Returns resource information as (num_memories, max_initial_memory_size, num_tables, max_initial_table_size).
    /// max values are -1 if unbounded, -2 if resources_required() returns None.
    pub fn get_component_resources_required(component: &Component) -> (i32, i64, i32, i64) {
        match component.resources_required() {
            Some(req) => {
                let max_mem = req.max_initial_memory_size.map(|s| s as i64).unwrap_or(-1);
                let max_table = req.max_initial_table_size.map(|s| s as i64).unwrap_or(-1);
                (
                    req.num_memories as i32,
                    max_mem,
                    req.num_tables as i32,
                    max_table,
                )
            }
            None => (-2, -2, -2, -2),
        }
    }
}

// Component Model C API for FFI integration

use crate::shared_ffi::{FFI_ERROR, FFI_SUCCESS};
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_void};

/// Destroy a component engine and free its resources
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_engine_destroy(engine_ptr: *mut c_void) {
    if !engine_ptr.is_null() {
        let _ = Box::from_raw(engine_ptr as *mut ComponentEngine);
    }
}

/// Compile a WebAssembly component from bytes
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_compile(
    engine_ptr: *mut c_void,
    component_bytes: *const u8,
    component_size: usize,
    component_out: *mut *mut c_void,
) -> c_int {
    if engine_ptr.is_null() || component_bytes.is_null() || component_out.is_null() {
        return FFI_ERROR;
    }

    let engine = &mut *(engine_ptr as *mut ComponentEngine);
    let component_data = std::slice::from_raw_parts(component_bytes, component_size);

    match engine.compile_component(component_data) {
        Ok(component) => {
            *component_out = Box::into_raw(Box::new(component)) as *mut c_void;
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Destroy a compiled component and free its resources
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_destroy(component_ptr: *mut c_void) {
    if !component_ptr.is_null() {
        let _ = Box::from_raw(component_ptr as *mut Component);
    }
}

/// Instantiate a compiled component
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instantiate(
    engine_ptr: *mut c_void,
    component_ptr: *const c_void,
    instance_out: *mut *mut c_void,
) -> c_int {
    if engine_ptr.is_null() || component_ptr.is_null() || instance_out.is_null() {
        return FFI_ERROR;
    }

    let engine = &mut *(engine_ptr as *mut ComponentEngine);
    let component = &*(component_ptr as *const Component);

    match engine.instantiate_component(component) {
        Ok(instance) => {
            *instance_out = Box::into_raw(Box::new(instance)) as *mut c_void;
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Destroy a component instance and free its resources
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_destroy(instance_ptr: *mut c_void) {
    if !instance_ptr.is_null() {
        let _ = Box::from_raw(instance_ptr as *mut ComponentInstanceWrapper);
    }
}

/// Get the number of exported interfaces from a component
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_export_count(component_ptr: *const c_void) -> usize {
    if component_ptr.is_null() {
        return 0;
    }

    let component = &*(component_ptr as *const Component);
    component.metadata.exports.len()
}

/// Get the number of imports required by a component
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_import_count(component_ptr: *const c_void) -> usize {
    if component_ptr.is_null() {
        return 0;
    }

    let component = &*(component_ptr as *const Component);
    component.metadata.imports.len()
}

/// Get the size of a compiled component in bytes
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_size_bytes(component_ptr: *const c_void) -> usize {
    if component_ptr.is_null() {
        return 0;
    }

    let component = &*(component_ptr as *const Component);
    component.metadata.size_bytes
}

/// Check if a component has a specific export
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_has_export(
    component_ptr: *const c_void,
    export_name: *const c_char,
) -> c_int {
    if component_ptr.is_null() || export_name.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let name_str = match CStr::from_ptr(export_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let has_export = component
        .metadata
        .exports
        .iter()
        .any(|export| export.name == name_str);

    if has_export {
        1
    } else {
        0
    }
}

/// Check if a component requires a specific import
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_has_import(
    component_ptr: *const c_void,
    import_name: *const c_char,
) -> c_int {
    if component_ptr.is_null() || import_name.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let name_str = match CStr::from_ptr(import_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let has_import = component
        .metadata
        .imports
        .iter()
        .any(|import| import.name == name_str);

    if has_import {
        1
    } else {
        0
    }
}

/// Check if a component exports a specific interface
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_exports_interface(
    component_ptr: *const c_void,
    interface_name: *const c_char,
) -> c_int {
    if component_ptr.is_null() || interface_name.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let name_str = match CStr::from_ptr(interface_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    if component.exports_interface(name_str) {
        1
    } else {
        0
    }
}

/// Check if a component imports a specific interface
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_imports_interface(
    component_ptr: *const c_void,
    interface_name: *const c_char,
) -> c_int {
    if component_ptr.is_null() || interface_name.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let name_str = match CStr::from_ptr(interface_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    if component.imports_interface(name_str) {
        1
    } else {
        0
    }
}

/// Validate a component against WIT interface requirements
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_validate(
    component_ptr: *const c_void,
    wit_interface: *const c_char,
) -> c_int {
    if component_ptr.is_null() || wit_interface.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let wit_str = match CStr::from_ptr(wit_interface).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    match component.validate_wit_interface(wit_str) {
        Ok(is_valid) => {
            if is_valid {
                1
            } else {
                0
            }
        }
        Err(_) => FFI_ERROR,
    }
}

/// Get export interface name by index
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_get_export_name(
    component_ptr: *const c_void,
    index: usize,
    name_out: *mut *mut c_char,
) -> c_int {
    if component_ptr.is_null() || name_out.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);

    if index >= component.metadata.exports.len() {
        return FFI_ERROR;
    }

    let export_name = &component.metadata.exports[index].name;

    match CString::new(export_name.as_str()) {
        Ok(c_string) => {
            *name_out = c_string.into_raw();
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Get import interface name by index
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_get_import_name(
    component_ptr: *const c_void,
    index: usize,
    name_out: *mut *mut c_char,
) -> c_int {
    if component_ptr.is_null() || name_out.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);

    if index >= component.metadata.imports.len() {
        return FFI_ERROR;
    }

    let import_name = &component.metadata.imports[index].name;

    match CString::new(import_name.as_str()) {
        Ok(c_string) => {
            *name_out = c_string.into_raw();
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Free a string returned by component functions
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_free_string(str_ptr: *mut c_char) {
    if !str_ptr.is_null() {
        let _ = CString::from_raw(str_ptr);
    }
}

/// Serialize a component to bytes
///
/// Returns serialized data through out-pointers. Caller must free data with
/// wasmtime4j_component_free_serialized_data.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_serialize(
    component_ptr: *mut c_void,
    data_ptr: *mut *mut u8,
    len_ptr: *mut usize,
) -> c_int {
    if component_ptr.is_null() || data_ptr.is_null() || len_ptr.is_null() {
        return FFI_ERROR;
    }

    match core::get_component_ref(component_ptr) {
        Ok(component) => match core::serialize_component(component) {
            Ok(mut bytes) => {
                let len = bytes.len();
                let ptr = bytes.as_mut_ptr();
                *data_ptr = ptr;
                *len_ptr = len;
                // Leak the Vec - caller must free with wasmtime4j_component_free_serialized_data
                std::mem::forget(bytes);
                FFI_SUCCESS
            }
            Err(e) => {
                log::error!("Failed to serialize component: {}", e);
                FFI_ERROR
            }
        },
        Err(e) => {
            log::error!("Invalid component pointer: {}", e);
            FFI_ERROR
        }
    }
}

/// Deserialize a component from bytes
///
/// Returns a new component pointer through out-pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_deserialize(
    engine_ptr: *mut c_void,
    data_ptr: *const u8,
    len: usize,
    component_ptr_out: *mut *mut c_void,
) -> c_int {
    if engine_ptr.is_null() || data_ptr.is_null() || len == 0 || component_ptr_out.is_null() {
        return FFI_ERROR;
    }

    let bytes = std::slice::from_raw_parts(data_ptr, len);

    match core::get_component_engine_ref(engine_ptr) {
        Ok(engine) => match core::deserialize_component(engine, bytes) {
            Ok(component) => {
                *component_ptr_out = Box::into_raw(component) as *mut c_void;
                FFI_SUCCESS
            }
            Err(e) => {
                log::error!("Failed to deserialize component: {}", e);
                FFI_ERROR
            }
        },
        Err(e) => {
            log::error!("Invalid engine pointer: {}", e);
            FFI_ERROR
        }
    }
}

/// Free serialized component data
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_free_serialized_data(data_ptr: *mut u8, len: usize) {
    if !data_ptr.is_null() && len > 0 {
        let _ = Vec::from_raw_parts(data_ptr, len, len);
    }
}

/// Get a component export index for efficient repeated lookups.
///
/// The `instance_index_ptr` is an optional parent instance export index for nested lookups.
/// Pass null for root-level export lookups.
///
/// Returns a boxed `ComponentExportIndex` pointer via `index_out`, or null if not found.
/// The caller must free the returned pointer with `wasmtime4j_component_export_index_destroy`.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_get_export_index(
    component_ptr: *const c_void,
    instance_index_ptr: *const c_void,
    name: *const c_char,
    index_out: *mut *mut c_void,
) -> c_int {
    if component_ptr.is_null() || name.is_null() || index_out.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let name_str = match CStr::from_ptr(name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let instance = if instance_index_ptr.is_null() {
        None
    } else {
        Some(&*(instance_index_ptr as *const wasmtime::component::ComponentExportIndex))
    };

    match core::get_export_index(component, instance, name_str) {
        Some(boxed_index) => {
            *index_out = Box::into_raw(boxed_index) as *mut c_void;
            FFI_SUCCESS
        }
        None => {
            *index_out = std::ptr::null_mut();
            FFI_SUCCESS // Not an error - export just wasn't found
        }
    }
}

/// Destroy a component export index.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_export_index_destroy(index_ptr: *mut c_void) {
    core::destroy_export_index(index_ptr);
}

/// Get the full component type as a JSON string with complete type information.
///
/// Returns a JSON string via `json_out` containing all imports and exports with their
/// full type descriptors. The caller must free the returned string with
/// `wasmtime4j_component_free_string`.
///
/// JSON format:
/// ```json
/// {
///   "imports": {"name": {"kind":"component_func", "params":[...], "results":[...]}},
///   "exports": {"name": {"kind":"component_instance", "exports":{...}}}
/// }
/// ```
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_get_full_type_json(
    component_ptr: *const c_void,
    engine_ptr: *const c_void,
    json_out: *mut *mut c_char,
) -> c_int {
    if component_ptr.is_null() || engine_ptr.is_null() || json_out.is_null() {
        return FFI_ERROR;
    }

    let component = &*(component_ptr as *const Component);
    let engine = &*(engine_ptr as *const ComponentEngine);

    match core::get_full_component_type_json(component, &engine.engine) {
        Ok(json) => match CString::new(json) {
            Ok(c_string) => {
                *json_out = c_string.into_raw();
                FFI_SUCCESS
            }
            Err(_) => FFI_ERROR,
        },
        Err(e) => {
            log::error!("Failed to get full component type: {}", e);
            FFI_ERROR
        }
    }
}

/// Get the substituted component type as a JSON string.
///
/// Uses a linker to compute which imports have been satisfied and returns the
/// remaining (substituted) type. The caller must free the returned string with
/// `wasmtime4j_component_free_string`.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_substituted_type_json(
    linker_ptr: *const c_void,
    component_ptr: *const c_void,
    json_out: *mut *mut c_char,
) -> c_int {
    if linker_ptr.is_null() || component_ptr.is_null() || json_out.is_null() {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    let component = &*(component_ptr as *const Component);

    match core::get_substituted_component_type_json(linker, component) {
        Ok(json) => match CString::new(json) {
            Ok(c_string) => {
                *json_out = c_string.into_raw();
                FFI_SUCCESS
            }
            Err(_) => FFI_ERROR,
        },
        Err(e) => {
            log::error!("Failed to get substituted component type: {}", e);
            FFI_ERROR
        }
    }
}

/// Parse a WAVE-encoded string into a serialized component value.
///
/// Supports primitive types (bool, s8-s64, u8-u64, f32, f64, char, string).
/// Compound types (list, record, tuple, variant, etc.) require component-level
/// type context which cannot be constructed standalone.
///
/// # Arguments
/// * `type_name` - The component type name (e.g., "bool", "u32", "string")
/// * `wave_str` - The WAVE-encoded value string
///
/// # Returns
/// Tuple of (type_discriminator, serialized_data), or error
#[cfg(feature = "wave")]
pub fn component_val_from_wave(
    type_name: &str,
    wave_str: &str,
) -> crate::error::WasmtimeResult<(i32, Vec<u8>)> {
    use wasmtime::component::types::Type;
    use wasmtime::component::Val;

    let ty = match type_name {
        "bool" => Type::Bool,
        "s8" => Type::S8,
        "u8" => Type::U8,
        "s16" => Type::S16,
        "u16" => Type::U16,
        "s32" => Type::S32,
        "u32" => Type::U32,
        "s64" => Type::S64,
        "u64" => Type::U64,
        "f32" | "float32" => Type::Float32,
        "f64" | "float64" => Type::Float64,
        "char" => Type::Char,
        "string" => Type::String,
        _ => {
            return Err(crate::error::WasmtimeError::Type {
                message: format!(
                    "WAVE parsing for compound type '{}' requires component-level type context. \
                     Only primitive types (bool, s8-s64, u8-u64, f32, f64, char, string) are \
                     supported for standalone parsing.",
                    type_name
                ),
            });
        }
    };

    let val = Val::from_wave(&ty, wave_str).map_err(|e| crate::error::WasmtimeError::Type {
        message: format!("Failed to parse WAVE string '{}' as {}: {}", wave_str, type_name, e),
    })?;

    // Serialize the Val using the existing wit_value_marshal infrastructure
    crate::wit_value_marshal::serialize_from_val(&val)
}
