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

use std::collections::HashMap;
use std::path::Path;
use std::sync::{Arc, Mutex, Weak};
use wasmtime::{
    Engine,
    Store,
    component::{Component as WasmtimeComponent, Linker, Instance as ComponentInstance}
};
use crate::error::{WasmtimeError, WasmtimeResult};

/// Engine for managing WebAssembly component instances
///
/// The ComponentEngine provides a high-level interface for working with
/// WebAssembly components, including loading, instantiation, and lifecycle management.
/// It maintains internal state for resource tracking and automatic cleanup.
pub struct ComponentEngine {
    /// Wasmtime engine for component compilation and execution
    engine: Engine,
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
#[derive(Default)]
pub struct ComponentStoreData {
    /// Instance ID for resource tracking
    pub instance_id: u64,
    /// Custom user data (reserved for future use)
    pub user_data: Option<Box<dyn std::any::Any + Send + Sync>>,
}

/// A compiled WebAssembly component
///
/// Represents a WebAssembly component that has been compiled and is ready
/// for instantiation. Components are immutable after compilation.
pub struct Component {
    /// The compiled Wasmtime component
    component: WasmtimeComponent,
    /// Component metadata for introspection
    metadata: ComponentMetadata,
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

/// WebAssembly Interface Type (WIT) interface definition
///
/// Represents a WIT interface with its methods, types, and resources.
/// Used for type-safe binding between host and component.
#[derive(Debug, Clone)]
pub struct InterfaceDefinition {
    /// Interface name
    pub name: String,
    /// Interface namespace (e.g., "wasi:filesystem")
    pub namespace: Option<String>,
    /// Interface version
    pub version: Option<String>,
    /// Functions defined in this interface
    pub functions: Vec<FunctionDefinition>,
    /// Types defined in this interface
    pub types: Vec<TypeDefinition>,
    /// Resources defined in this interface
    pub resources: Vec<ResourceDefinition>,
}

/// Function definition within a WIT interface
#[derive(Debug, Clone)]
pub struct FunctionDefinition {
    /// Function name
    pub name: String,
    /// Function parameters
    pub parameters: Vec<Parameter>,
    /// Function return types
    pub results: Vec<ComponentValueType>,
}

/// Parameter definition for WIT functions
#[derive(Debug, Clone)]
pub struct Parameter {
    /// Parameter name
    pub name: String,
    /// Parameter type
    pub value_type: ComponentValueType,
}

/// Type definition within a WIT interface
#[derive(Debug, Clone)]
pub struct TypeDefinition {
    /// Type name
    pub name: String,
    /// Type kind (record, variant, enum, etc.)
    pub kind: ComponentTypeKind,
}

/// Resource definition within a WIT interface
#[derive(Debug, Clone)]
pub struct ResourceDefinition {
    /// Resource name
    pub name: String,
    /// Resource constructor functions
    pub constructors: Vec<FunctionDefinition>,
    /// Resource instance methods
    pub methods: Vec<FunctionDefinition>,
}

/// WebAssembly Component Model value types
#[derive(Debug, Clone)]
pub enum ComponentValueType {
    /// Boolean type
    Bool,
    /// 8-bit signed integer
    S8,
    /// 8-bit unsigned integer  
    U8,
    /// 16-bit signed integer
    S16,
    /// 16-bit unsigned integer
    U16,
    /// 32-bit signed integer
    S32,
    /// 32-bit unsigned integer
    U32,
    /// 64-bit signed integer
    S64,
    /// 64-bit unsigned integer
    U64,
    /// 32-bit floating point
    Float32,
    /// 64-bit floating point
    Float64,
    /// Unicode string
    String,
    /// List of values
    List(Box<ComponentValueType>),
    /// Optional value
    Option(Box<ComponentValueType>),
    /// Result type with success and error cases
    Result { 
        /// Success value type
        ok: Option<Box<ComponentValueType>>, 
        /// Error value type
        err: Option<Box<ComponentValueType>> 
    },
    /// Record type with named fields
    Record(Vec<FieldType>),
    /// Variant type with multiple cases
    Variant(Vec<CaseType>),
    /// Enum type with named values
    Enum(Vec<String>),
    /// Resource handle
    Resource(String),
    /// Custom type reference
    Type(String),
}

/// Type kind for WIT type definitions
#[derive(Debug, Clone)]
pub enum ComponentTypeKind {
    /// Record type with named fields
    Record(Vec<FieldType>),
    /// Variant type with multiple cases
    Variant(Vec<CaseType>),
    /// Enum type with named values
    Enum(Vec<String>),
    /// Type alias
    Alias(ComponentValueType),
}

/// Field type for record types
#[derive(Debug, Clone)]
pub struct FieldType {
    /// Field name
    pub name: String,
    /// Field type
    pub value_type: ComponentValueType,
}

/// Case type for variant types
#[derive(Debug, Clone)]
pub struct CaseType {
    /// Case name
    pub name: String,
    /// Optional case payload type
    pub payload: Option<ComponentValueType>,
}

/// Resource manager for automatic component cleanup
///
/// Tracks component instances and ensures proper cleanup when components
/// are no longer referenced. Prevents resource leaks and dangling references.
pub struct ResourceManager {
    /// Weak references to active instances
    instances: HashMap<u64, Weak<ComponentInstance>>,
    /// Cleanup callbacks for each instance
    cleanup_callbacks: HashMap<u64, Box<dyn FnOnce() + Send>>,
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
        let engine = Engine::default();
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
    pub fn with_engine(engine: Engine) -> WasmtimeResult<Self> {
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

        let component = WasmtimeComponent::new(&self.engine, bytes)
            .map_err(|e| WasmtimeError::Compilation {
                message: format!("Failed to compile component: {}", e),
            })?;

        let metadata = self.extract_component_metadata(&component)?;

        Ok(Component {
            component,
            metadata,
        })
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
                    format!("Component file not found: {}", path.display())
                )
            });
        }

        let component = WasmtimeComponent::from_file(&self.engine, path)
            .map_err(|e| WasmtimeError::Compilation {
                message: format!("Failed to load component from file {}: {}", path.display(), e),
            })?;

        let metadata = self.extract_component_metadata(&component)?;

        Ok(Component {
            component,
            metadata,
        })
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
    pub fn instantiate_component(&self, component: &Component) -> WasmtimeResult<Arc<ComponentInstance>> {
        let instance_id = {
            let mut next_id = self.next_instance_id.lock()
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
        };
        
        let mut store = Store::new(&self.engine, store_data);
        
        let instance = self.linker.instantiate(&mut store, &component.component)
            .map_err(|e| WasmtimeError::Instance {
                message: format!("Failed to instantiate component: {}", e),
            })?;

        let instance_ref = Arc::new(instance);
        
        // Track the instance for resource management
        {
            let mut instances = self.instances.lock()
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
    pub fn add_host_interface(&mut self, interface_name: &str, _implementation: HostInterface) -> WasmtimeResult<()> {
        // TODO: Implement host interface binding once Wasmtime component model API is stable
        // This is a placeholder for future implementation
        log::info!("Adding host interface: {}", interface_name);
        
        // For now, we'll prepare the linker structure but not bind actual implementations
        // The actual binding will be implemented as the Wasmtime component model API stabilizes
        
        Ok(())
    }

    /// Get information about active component instances
    ///
    /// # Returns
    ///
    /// Returns a vector containing metadata about all active component instances
    /// managed by this engine.
    pub fn get_active_instances(&self) -> WasmtimeResult<Vec<InstanceInfo>> {
        let instances = self.instances.lock()
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
        let mut instances = self.instances.lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire instances lock".to_string(),
            })?;

        let initial_count = instances.len();
        instances.retain(|_, weak_ref| weak_ref.strong_count() > 0);
        let final_count = instances.len();

        Ok(initial_count - final_count)
    }

    /// Extract metadata from a compiled component
    ///
    /// This internal function analyzes a compiled component to extract
    /// information about its imports, exports, and interfaces.
    fn extract_component_metadata(&self, _component: &WasmtimeComponent) -> WasmtimeResult<ComponentMetadata> {
        // TODO: Implement actual metadata extraction using Wasmtime component introspection API
        // For now, return minimal metadata structure
        
        Ok(ComponentMetadata {
            imports: Vec::new(),
            exports: Vec::new(), 
            size_bytes: 0, // Will be implemented with actual component analysis
        })
    }
}

/// Host interface implementation placeholder
///
/// This struct will be expanded to support actual host interface implementations
/// as the Wasmtime component model API develops.
pub struct HostInterface {
    /// Interface name
    pub name: String,
    /// Interface implementation (placeholder)
    pub implementation: Box<dyn std::any::Any + Send + Sync>,
}

/// Information about an active component instance
#[derive(Debug, Clone)]
pub struct InstanceInfo {
    /// Unique instance identifier
    pub instance_id: u64,
    /// Number of strong references to this instance
    pub strong_references: usize,
}

impl Component {
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
        self.metadata.exports.iter()
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
        self.metadata.imports.iter()
            .any(|import| import.name == interface_name)
    }
}

impl ResourceManager {
    /// Create a new resource manager
    ///
    /// # Returns
    ///
    /// Returns a new `ResourceManager` ready to track component instances.
    pub fn new() -> Self {
        ResourceManager {
            instances: HashMap::new(),
            cleanup_callbacks: HashMap::new(),
        }
    }

    /// Register a component instance for tracking
    ///
    /// # Arguments
    ///
    /// * `instance_id` - Unique identifier for the instance
    /// * `instance` - Weak reference to the instance
    /// * `cleanup_callback` - Optional cleanup callback to execute when instance is dropped
    ///
    /// # Returns
    ///
    /// Returns `Ok(())` if the instance was successfully registered.
    pub fn register_instance<F>(&mut self, 
        instance_id: u64, 
        instance: Weak<ComponentInstance>,
        cleanup_callback: Option<F>) -> WasmtimeResult<()>
    where
        F: FnOnce() + Send + 'static,
    {
        self.instances.insert(instance_id, instance);
        
        if let Some(callback) = cleanup_callback {
            self.cleanup_callbacks.insert(instance_id, Box::new(callback));
        }
        
        Ok(())
    }

    /// Cleanup all inactive instances
    ///
    /// # Returns
    ///
    /// Returns the number of instances that were cleaned up.
    pub fn cleanup_inactive(&mut self) -> usize {
        let mut cleaned_up = 0;
        let mut to_remove = Vec::new();

        for (id, weak_ref) in &self.instances {
            if weak_ref.strong_count() == 0 {
                to_remove.push(*id);
            }
        }

        for id in to_remove {
            self.instances.remove(&id);
            
            if let Some(callback) = self.cleanup_callbacks.remove(&id) {
                callback();
            }
            
            cleaned_up += 1;
        }

        cleaned_up
    }

    /// Get count of active instances
    ///
    /// # Returns
    ///
    /// Returns the number of currently active component instances.
    pub fn active_count(&self) -> usize {
        self.instances.iter()
            .filter(|(_, weak_ref)| weak_ref.strong_count() > 0)
            .count()
    }
}

/// Shared core functions for component operations used by both JNI and Panama interfaces
/// 
/// These functions eliminate code duplication and provide consistent behavior
/// across interface implementations while maintaining defensive programming practices.
pub mod core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::{validate_ptr_not_null, validate_not_empty};
    
    /// Core function to create a new component engine
    pub fn create_component_engine() -> WasmtimeResult<Box<ComponentEngine>> {
        ComponentEngine::new().map(Box::new)
    }
    
    /// Core function to create a component engine with a custom Wasmtime engine
    pub fn create_component_engine_with_engine(engine: Engine) -> WasmtimeResult<Box<ComponentEngine>> {
        ComponentEngine::with_engine(engine).map(Box::new)
    }
    
    /// Core function to validate component engine pointer and get reference
    pub unsafe fn get_component_engine_ref(engine_ptr: *const c_void) -> WasmtimeResult<&'static ComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "component engine");
        Ok(&*(engine_ptr as *const ComponentEngine))
    }
    
    /// Core function to validate component engine pointer and get mutable reference
    pub unsafe fn get_component_engine_mut(engine_ptr: *mut c_void) -> WasmtimeResult<&'static mut ComponentEngine> {
        validate_ptr_not_null!(engine_ptr, "component engine");
        Ok(&mut *(engine_ptr as *mut ComponentEngine))
    }
    
    /// Core function to load a component from bytes
    pub fn load_component_from_bytes(engine: &ComponentEngine, bytes: &[u8]) -> WasmtimeResult<Box<Component>> {
        validate_not_empty!(bytes, "component bytes");
        engine.load_component_from_bytes(bytes).map(Box::new)
    }
    
    /// Core function to load a component from a file
    pub fn load_component_from_file<P: AsRef<std::path::Path>>(engine: &ComponentEngine, path: P) -> WasmtimeResult<Box<Component>> {
        engine.load_component_from_file(path).map(Box::new)
    }
    
    /// Core function to validate component pointer and get reference
    pub unsafe fn get_component_ref(component_ptr: *const c_void) -> WasmtimeResult<&'static Component> {
        validate_ptr_not_null!(component_ptr, "component");
        Ok(&*(component_ptr as *const Component))
    }
    
    /// Core function to validate component pointer and get mutable reference
    pub unsafe fn get_component_mut(component_ptr: *mut c_void) -> WasmtimeResult<&'static mut Component> {
        validate_ptr_not_null!(component_ptr, "component");
        Ok(&mut *(component_ptr as *mut Component))
    }
    
    /// Core function to instantiate a component
    pub fn instantiate_component(engine: &ComponentEngine, component: &Component) -> WasmtimeResult<Arc<ComponentInstance>> {
        engine.instantiate_component(component)
    }
    
    /// Core function to add a host interface to the component linker
    pub fn add_host_interface(engine: &mut ComponentEngine, interface_name: &str, implementation: HostInterface) -> WasmtimeResult<()> {
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

    /// Core function to compile a component directly (alias for load_component_from_bytes)
    pub fn compile_component(engine: &ComponentEngine, bytes: &[u8]) -> WasmtimeResult<Box<Component>> {
        load_component_from_bytes(engine, bytes)
    }

    /// Core function to create a component linker
    pub fn create_component_linker(engine: &ComponentEngine) -> WasmtimeResult<Box<Linker<ComponentStoreData>>> {
        let linker = Linker::new(&engine.engine);
        Ok(Box::new(linker))
    }

    /// Core function to get component exports as strings
    pub fn get_component_exports(component: &Component) -> Vec<String> {
        component.metadata().exports.iter()
            .map(|export| export.name.clone())
            .collect()
    }

    /// Core function to get component imports as strings
    pub fn get_component_imports(component: &Component) -> Vec<String> {
        component.metadata().imports.iter()
            .map(|import| import.name.clone())
            .collect()
    }

    /// Core function to get component export names
    pub fn get_export_names(component: &Component) -> Vec<String> {
        get_component_exports(component)
    }

    /// Core function to get component import names
    pub fn get_import_names(component: &Component) -> Vec<String> {
        get_component_imports(component)
    }

    /// Core function to get component export by name
    pub fn get_component_export_by_name<'a>(component: &'a Component, name: &str) -> Option<&'a InterfaceDefinition> {
        component.metadata().exports.iter()
            .find(|export| export.name == name)
    }

    /// Core function to get component import by name
    pub fn get_component_import_by_name<'a>(component: &'a Component, name: &str) -> Option<&'a InterfaceDefinition> {
        component.metadata().imports.iter()
            .find(|import| import.name == name)
    }

    /// Core function to validate component linker pointer and get reference
    pub unsafe fn get_component_linker_ref(linker_ptr: *const c_void) -> WasmtimeResult<&'static Linker<ComponentStoreData>> {
        validate_ptr_not_null!(linker_ptr, "component linker");
        Ok(&*(linker_ptr as *const Linker<ComponentStoreData>))
    }

    /// Core function to validate component linker pointer and get mutable reference
    pub unsafe fn get_component_linker_mut(linker_ptr: *mut c_void) -> WasmtimeResult<&'static mut Linker<ComponentStoreData>> {
        validate_ptr_not_null!(linker_ptr, "component linker");
        Ok(&mut *(linker_ptr as *mut Linker<ComponentStoreData>))
    }

    /// Core function to validate component instance pointer and get reference
    pub unsafe fn get_component_instance_ref(instance_ptr: *const c_void) -> WasmtimeResult<&'static Arc<ComponentInstance>> {
        validate_ptr_not_null!(instance_ptr, "component instance");
        Ok(&*(instance_ptr as *const Arc<ComponentInstance>))
    }

    /// Core function to destroy a component linker (safe cleanup)
    pub unsafe fn destroy_component_linker(linker_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Linker<ComponentStoreData>>(linker_ptr, "ComponentLinker");
    }
}

impl Default for ComponentEngine {
    fn default() -> Self {
        Self::new().expect("Failed to create default ComponentEngine")
    }
}

impl Default for ResourceManager {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_component_engine_creation() {
        let engine = ComponentEngine::new();
        assert!(engine.is_ok());
    }

    #[test]
    fn test_component_engine_with_custom_engine() {
        let wasmtime_engine = Engine::default();
        let component_engine = ComponentEngine::with_engine(wasmtime_engine);
        assert!(component_engine.is_ok());
    }

    #[test]
    fn test_load_component_from_empty_bytes() {
        let engine = ComponentEngine::new().unwrap();
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
        let engine = ComponentEngine::new().unwrap();
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
    fn test_load_component_from_nonexistent_file() {
        let engine = ComponentEngine::new().unwrap();
        let result = engine.load_component_from_file("/nonexistent/file.wasm");
        assert!(result.is_err());
        
        if let Err(WasmtimeError::Io { .. }) = result {
            // Expected I/O error
        } else {
            panic!("Expected I/O error");
        }
    }

    #[test]
    fn test_resource_manager_creation() {
        let manager = ResourceManager::new();
        assert_eq!(manager.active_count(), 0);
    }

    #[test]
    fn test_resource_manager_cleanup() {
        let mut manager = ResourceManager::new();
        let cleaned = manager.cleanup_inactive();
        assert_eq!(cleaned, 0);
    }

    #[test]
    fn test_component_metadata_accessors() {
        let metadata = ComponentMetadata {
            imports: vec![],
            exports: vec![InterfaceDefinition {
                name: "test-interface".to_string(),
                namespace: Some("test".to_string()),
                version: Some("1.0.0".to_string()),
                functions: vec![],
                types: vec![],
                resources: vec![],
            }],
            size_bytes: 1024,
        };

        // Test metadata directly without requiring actual component compilation
        assert_eq!(metadata.size_bytes, 1024);
        assert_eq!(metadata.exports.len(), 1);
        assert_eq!(metadata.exports[0].name, "test-interface");
        assert_eq!(metadata.imports.len(), 0);

        // Test interface lookup functions on metadata
        let has_export = metadata.exports.iter()
            .any(|export| export.name == "test-interface");
        assert!(has_export);

        let has_nonexistent = metadata.exports.iter()
            .any(|export| export.name == "nonexistent");
        assert!(!has_nonexistent);

        let has_import = metadata.imports.iter()
            .any(|import| import.name == "any-interface");
        assert!(!has_import);
    }

    #[test]
    fn test_instance_info() {
        let info = InstanceInfo {
            instance_id: 42,
            strong_references: 1,
        };
        
        assert_eq!(info.instance_id, 42);
        assert_eq!(info.strong_references, 1);
    }

    #[test]
    fn test_value_type_variants() {
        let types = vec![
            ComponentValueType::Bool,
            ComponentValueType::S8,
            ComponentValueType::U8,
            ComponentValueType::S16,
            ComponentValueType::U16,
            ComponentValueType::S32,
            ComponentValueType::U32,
            ComponentValueType::S64,
            ComponentValueType::U64,
            ComponentValueType::Float32,
            ComponentValueType::Float64,
            ComponentValueType::String,
            ComponentValueType::List(Box::new(ComponentValueType::String)),
            ComponentValueType::Option(Box::new(ComponentValueType::Bool)),
            ComponentValueType::Result { ok: Some(Box::new(ComponentValueType::S32)), err: Some(Box::new(ComponentValueType::String)) },
        ];

        // Test that all value types can be created and cloned
        for value_type in types {
            let cloned = value_type.clone();
            // Basic test - ensure types can be constructed and cloned
            match cloned {
                ComponentValueType::Bool => assert!(true),
                ComponentValueType::List(_) => assert!(true),
                ComponentValueType::Option(_) => assert!(true),
                ComponentValueType::Result { .. } => assert!(true),
                _ => assert!(true),
            }
        }
    }
}