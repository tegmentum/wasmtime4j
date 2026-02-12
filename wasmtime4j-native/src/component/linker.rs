//! Component Model linker for defining host functions and instantiating components
//!
//! This module provides the ComponentLinker for binding host functions to WIT interfaces
//! and instantiating WebAssembly components.

use super::{Component, ComponentStoreData};
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::Instant;
use wasmtime::{
    component::{Instance as ComponentInstance, Linker, ResourceTable},
    Engine as WasmtimeEngine, Store,
};

/// Component Model value representation for host function communication
#[derive(Debug, Clone)]
pub enum ComponentValue {
    /// Boolean value
    Bool(bool),
    /// 8-bit signed integer
    S8(i8),
    /// 16-bit signed integer
    S16(i16),
    /// 32-bit signed integer
    S32(i32),
    /// 64-bit signed integer
    S64(i64),
    /// 8-bit unsigned integer
    U8(u8),
    /// 16-bit unsigned integer
    U16(u16),
    /// 32-bit unsigned integer
    U32(u32),
    /// 64-bit unsigned integer
    U64(u64),
    /// 32-bit float
    F32(f32),
    /// 64-bit float
    F64(f64),
    /// Unicode character
    Char(char),
    /// String value
    String(String),
    /// List of values
    List(Vec<ComponentValue>),
    /// Record with named fields
    Record(Vec<(String, ComponentValue)>),
    /// Tuple of values
    Tuple(Vec<ComponentValue>),
    /// Variant (tagged union)
    Variant {
        case_name: String,
        payload: Option<Box<ComponentValue>>,
    },
    /// Enum case name
    Enum(String),
    /// Optional value
    Option(Option<Box<ComponentValue>>),
    /// Result type
    Result {
        ok: Option<Box<ComponentValue>>,
        err: Option<Box<ComponentValue>>,
        is_ok: bool,
    },
    /// Flags (set of enabled flag names)
    Flags(Vec<String>),
    /// Resource handle (own)
    Own(u64),
    /// Resource handle (borrow)
    Borrow(u64),
}

/// Trait for Component Model host function callbacks
pub trait ComponentHostCallback: Send + Sync {
    /// Execute the host function with Component Model values
    fn execute(&self, params: &[ComponentValue]) -> WasmtimeResult<Vec<ComponentValue>>;

    /// Clone the callback for use across invocations
    fn clone_callback(&self) -> Box<dyn ComponentHostCallback>;
}

/// Registry for Component Model host function callbacks
pub static COMPONENT_HOST_FUNCTION_REGISTRY: std::sync::OnceLock<
    Mutex<HashMap<u64, Arc<ComponentHostFunctionEntry>>>,
> = std::sync::OnceLock::new();
pub static NEXT_COMPONENT_HOST_FUNCTION_ID: std::sync::atomic::AtomicU64 =
    std::sync::atomic::AtomicU64::new(1);

pub fn get_component_host_function_registry(
) -> &'static Mutex<HashMap<u64, Arc<ComponentHostFunctionEntry>>> {
    COMPONENT_HOST_FUNCTION_REGISTRY.get_or_init(|| Mutex::new(HashMap::new()))
}

/// Entry in the component host function registry
pub struct ComponentHostFunctionEntry {
    /// Unique identifier
    pub id: u64,
    /// Interface namespace (e.g., "wasi:cli")
    pub interface_namespace: String,
    /// Interface name (e.g., "stdout")
    pub interface_name: String,
    /// Function name (e.g., "print")
    pub function_name: String,
    /// Callback implementation
    pub callback: Box<dyn ComponentHostCallback>,
}

impl std::fmt::Debug for ComponentHostFunctionEntry {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("ComponentHostFunctionEntry")
            .field("id", &self.id)
            .field("interface_namespace", &self.interface_namespace)
            .field("interface_name", &self.interface_name)
            .field("function_name", &self.function_name)
            .finish()
    }
}

/// WASI Preview 2 configuration for component model
#[derive(Clone, Default)]
pub struct WasiP2Config {
    /// Command-line arguments
    pub args: Vec<String>,
    /// Environment variables
    pub env: HashMap<String, String>,
    /// Whether to inherit environment from host
    pub inherit_env: bool,
    /// Whether to inherit stdio from host
    pub inherit_stdio: bool,
    /// Preopened directories (host_path, guest_path, read_only)
    pub preopened_dirs: Vec<(String, String, bool)>,
    /// Allow network access
    pub allow_network: bool,
    /// Allow clock access
    pub allow_clock: bool,
    /// Allow random number generation
    pub allow_random: bool,
}

/// Component Model linker for defining host functions and instantiating components
pub struct ComponentLinker {
    /// Wasmtime engine for component compilation
    engine: WasmtimeEngine,
    /// Component linker from Wasmtime
    linker: Linker<ComponentStoreData>,
    /// Registered host functions by WIT path
    host_functions: HashMap<String, u64>,
    /// Defined interfaces
    pub(crate) defined_interfaces: HashMap<String, Vec<String>>,
    /// Whether WASI Preview 2 is enabled
    wasi_p2_enabled: bool,
    /// Whether WASI HTTP is enabled
    wasi_http_enabled: bool,
    /// WASI Preview 2 configuration
    wasi_p2_config: WasiP2Config,
    /// Whether this linker has been disposed
    disposed: bool,
}

impl ComponentLinker {
    /// Create a new component linker for the given engine
    pub fn new(engine: &WasmtimeEngine) -> WasmtimeResult<Self> {
        let linker = Linker::new(engine);

        Ok(ComponentLinker {
            engine: engine.clone(),
            linker,
            host_functions: HashMap::new(),
            defined_interfaces: HashMap::new(),
            wasi_p2_enabled: false,
            wasi_http_enabled: false,
            wasi_p2_config: WasiP2Config::default(),
            disposed: false,
        })
    }

    /// Create a new component linker with an owned engine Arc
    ///
    /// This takes ownership of an Arc<WasmtimeEngine> rather than a reference,
    /// which ensures the engine stays valid for the lifetime of the linker.
    /// This is useful for FFI contexts where reference lifetimes are tricky.
    pub fn new_with_owned_engine(engine_arc: Arc<WasmtimeEngine>) -> WasmtimeResult<Self> {
        // Get a reference from the Arc for creating the linker
        let engine_ref: &WasmtimeEngine = &engine_arc;
        let linker = Linker::new(engine_ref);

        // Clone the engine from the Arc (wasmtime::Engine is Clone via Arc internally)
        let engine = (*engine_arc).clone();

        Ok(ComponentLinker {
            engine,
            linker,
            host_functions: HashMap::new(),
            defined_interfaces: HashMap::new(),
            wasi_p2_enabled: false,
            wasi_http_enabled: false,
            wasi_p2_config: WasiP2Config::default(),
            disposed: false,
        })
    }

    /// Configure WASI Preview 2 args
    pub fn set_wasi_args(&mut self, args: Vec<String>) {
        self.wasi_p2_config.args = args;
    }

    /// Configure WASI Preview 2 environment variables
    pub fn set_wasi_env(&mut self, env: HashMap<String, String>) {
        self.wasi_p2_config.env = env;
    }

    /// Set whether to inherit environment from host
    pub fn set_wasi_inherit_env(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_env = inherit;
    }

    /// Set whether to inherit stdio from host
    pub fn set_wasi_inherit_stdio(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_stdio = inherit;
    }

    /// Add a preopened directory
    pub fn add_wasi_preopen_dir(&mut self, host_path: String, guest_path: String, read_only: bool) {
        self.wasi_p2_config
            .preopened_dirs
            .push((host_path, guest_path, read_only));
    }

    /// Set whether network access is allowed
    pub fn set_wasi_allow_network(&mut self, allow: bool) {
        self.wasi_p2_config.allow_network = allow;
    }

    /// Set whether clock access is allowed
    pub fn set_wasi_allow_clock(&mut self, allow: bool) {
        self.wasi_p2_config.allow_clock = allow;
    }

    /// Set whether random number generation is allowed
    pub fn set_wasi_allow_random(&mut self, allow: bool) {
        self.wasi_p2_config.allow_random = allow;
    }

    /// Get the WASI P2 configuration
    pub fn wasi_p2_config(&self) -> &WasiP2Config {
        &self.wasi_p2_config
    }

    /// Build a WasiCtx from the stored configuration
    #[cfg(feature = "wasi")]
    pub fn build_wasi_ctx(&self) -> wasmtime_wasi::WasiCtx {
        use wasmtime_wasi::{DirPerms, FilePerms, WasiCtxBuilder};

        let mut builder = WasiCtxBuilder::new();

        // Set args
        if !self.wasi_p2_config.args.is_empty() {
            let args_refs: Vec<&str> = self
                .wasi_p2_config
                .args
                .iter()
                .map(|s| s.as_str())
                .collect();
            builder.args(&args_refs);
        }

        // Set environment
        if self.wasi_p2_config.inherit_env {
            builder.inherit_env();
        } else if !self.wasi_p2_config.env.is_empty() {
            let env_refs: Vec<(&str, &str)> = self
                .wasi_p2_config
                .env
                .iter()
                .map(|(k, v)| (k.as_str(), v.as_str()))
                .collect();
            builder.envs(&env_refs);
        }

        // Set stdio
        if self.wasi_p2_config.inherit_stdio {
            builder.inherit_stdio();
        }

        // Preopened directories
        for (host_path, guest_path, read_only) in &self.wasi_p2_config.preopened_dirs {
            let path = std::path::Path::new(host_path);
            if path.exists() && path.is_dir() {
                let (dir_perms, file_perms) = if *read_only {
                    (DirPerms::READ, FilePerms::READ)
                } else {
                    (DirPerms::all(), FilePerms::all())
                };

                if let Err(e) = builder.preopened_dir(path, guest_path, dir_perms, file_perms) {
                    log::warn!("Failed to preopen directory {}: {}", host_path, e);
                }
            }
        }

        builder.build()
    }

    /// Define a host function for a WIT interface
    pub fn define_function(
        &mut self,
        interface_namespace: &str,
        interface_name: &str,
        function_name: &str,
        callback: Box<dyn ComponentHostCallback>,
    ) -> WasmtimeResult<u64> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        let id = NEXT_COMPONENT_HOST_FUNCTION_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        let entry = Arc::new(ComponentHostFunctionEntry {
            id,
            interface_namespace: interface_namespace.to_string(),
            interface_name: interface_name.to_string(),
            function_name: function_name.to_string(),
            callback,
        });

        // Register in global registry
        {
            let mut registry = get_component_host_function_registry().lock().map_err(|e| {
                WasmtimeError::Concurrency {
                    message: format!("Failed to lock component host function registry: {}", e),
                }
            })?;
            registry.insert(id, entry);
        }

        // Build WIT path key
        let wit_path = format!(
            "{}:{}/{}#{}",
            interface_namespace, interface_name, interface_name, function_name
        );
        self.host_functions.insert(wit_path.clone(), id);

        // Track in defined interfaces
        let interface_key = format!(
            "{}:{}/{}",
            interface_namespace, interface_name, interface_name
        );
        self.defined_interfaces
            .entry(interface_key)
            .or_insert_with(Vec::new)
            .push(function_name.to_string());

        log::debug!("Defined component host function: {} (id={})", wit_path, id);

        Ok(id)
    }

    /// Define a host function using full WIT path
    pub fn define_function_by_path(
        &mut self,
        wit_path: &str,
        callback: Box<dyn ComponentHostCallback>,
    ) -> WasmtimeResult<u64> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        let id = NEXT_COMPONENT_HOST_FUNCTION_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

        // Parse WIT path to extract components
        let (namespace, interface, function) = parse_wit_path(wit_path)?;

        let entry = Arc::new(ComponentHostFunctionEntry {
            id,
            interface_namespace: namespace.clone(),
            interface_name: interface.clone(),
            function_name: function.clone(),
            callback,
        });

        // Register in global registry
        {
            let mut registry = get_component_host_function_registry().lock().map_err(|e| {
                WasmtimeError::Concurrency {
                    message: format!("Failed to lock component host function registry: {}", e),
                }
            })?;
            registry.insert(id, entry);
        }

        self.host_functions.insert(wit_path.to_string(), id);

        // Track in defined interfaces
        let interface_key = format!("{}:{}/{}", namespace, interface, interface);
        self.defined_interfaces
            .entry(interface_key)
            .or_insert_with(Vec::new)
            .push(function);

        log::debug!(
            "Defined component host function by path: {} (id={})",
            wit_path,
            id
        );

        Ok(id)
    }

    /// Check if a specific interface is defined
    pub fn has_interface(&self, interface_namespace: &str, interface_name: &str) -> bool {
        let key = format!(
            "{}:{}/{}",
            interface_namespace, interface_name, interface_name
        );
        self.defined_interfaces.contains_key(&key)
    }

    /// Check if a specific function is defined
    pub fn has_function(
        &self,
        interface_namespace: &str,
        interface_name: &str,
        function_name: &str,
    ) -> bool {
        let key = format!(
            "{}:{}/{}",
            interface_namespace, interface_name, interface_name
        );
        if let Some(functions) = self.defined_interfaces.get(&key) {
            functions.contains(&function_name.to_string())
        } else {
            false
        }
    }

    /// Get all defined interface paths
    pub fn get_defined_interfaces(&self) -> Vec<String> {
        self.defined_interfaces.keys().cloned().collect()
    }

    /// Get all functions defined for an interface
    pub fn get_defined_functions(
        &self,
        interface_namespace: &str,
        interface_name: &str,
    ) -> Vec<String> {
        let key = format!(
            "{}:{}/{}",
            interface_namespace, interface_name, interface_name
        );
        self.defined_interfaces
            .get(&key)
            .cloned()
            .unwrap_or_default()
    }

    /// Enable WASI Preview 2 support
    #[cfg(feature = "wasi")]
    pub fn enable_wasi_preview2(&mut self) -> WasmtimeResult<()> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        if self.wasi_p2_enabled {
            return Ok(()); // Already enabled
        }

        // Add WASI Preview 2 to the linker using p2 module
        // The closure extracts the WasiCtx from ComponentStoreData
        wasmtime_wasi::p2::add_to_linker_sync(&mut self.linker).map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to enable WASI Preview 2: {}", e),
            }
        })?;

        self.wasi_p2_enabled = true;
        log::debug!("WASI Preview 2 enabled in component linker");

        Ok(())
    }

    #[cfg(not(feature = "wasi"))]
    pub fn enable_wasi_preview2(&mut self) -> WasmtimeResult<()> {
        Err(WasmtimeError::Runtime {
            message: "WASI support not compiled in".to_string(),
            backtrace: None,
        })
    }

    /// Enable WASI HTTP support
    ///
    /// This enables HTTP request/response functionality in WebAssembly components.
    /// WASI Preview 2 must be enabled first for this to work.
    #[cfg(feature = "wasi-http")]
    pub fn enable_wasi_http(&mut self) -> WasmtimeResult<()> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        if self.wasi_http_enabled {
            return Ok(()); // Already enabled
        }

        // WASI HTTP requires WASI Preview 2 to be enabled first
        if !self.wasi_p2_enabled {
            return Err(WasmtimeError::Runtime {
                message: "WASI Preview 2 must be enabled before WASI HTTP".to_string(),
                backtrace: None,
            });
        }

        // Add WASI HTTP to the linker - use add_only_http_to_linker_sync
        // since WASI P2 is already added
        wasmtime_wasi_http::add_only_http_to_linker_sync(&mut self.linker).map_err(|e| {
            WasmtimeError::Wasi {
                message: format!("Failed to enable WASI HTTP: {}", e),
            }
        })?;

        self.wasi_http_enabled = true;
        log::debug!("WASI HTTP enabled in component linker");

        Ok(())
    }

    #[cfg(not(feature = "wasi-http"))]
    pub fn enable_wasi_http(&mut self) -> WasmtimeResult<()> {
        Err(WasmtimeError::Runtime {
            message: "WASI HTTP support not compiled in".to_string(),
            backtrace: None,
        })
    }

    /// Check if WASI HTTP is enabled
    pub fn is_wasi_http_enabled(&self) -> bool {
        self.wasi_http_enabled
    }

    /// Instantiate a component using this linker
    pub fn instantiate(&self, component: &Component) -> WasmtimeResult<Arc<ComponentInstance>> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        // Build store data with configured WASI context if WASI P2 is enabled
        #[cfg(feature = "wasi")]
        let store_data = if self.wasi_p2_enabled {
            ComponentStoreData {
                instance_id: 0,
                user_data: None,
                resource_table: ResourceTable::new(),
                wasi_ctx: self.build_wasi_ctx(),
                #[cfg(feature = "wasi-http")]
                wasi_http_ctx: if self.wasi_http_enabled {
                    Some(wasmtime_wasi_http::WasiHttpCtx::new())
                } else {
                    None
                },
                start_time: Instant::now(),
            }
        } else {
            ComponentStoreData {
                instance_id: 0,
                user_data: None,
                ..Default::default()
            }
        };

        #[cfg(not(feature = "wasi"))]
        let store_data = ComponentStoreData {
            instance_id: 0,
            user_data: None,
            ..Default::default()
        };

        let mut store = Store::new(&self.engine, store_data);

        let instance = self
            .linker
            .instantiate(&mut store, component.wasmtime_component())
            .map_err(|e| WasmtimeError::Instance {
                message: format!("Failed to instantiate component: {}", e),
            })?;

        Ok(Arc::new(instance))
    }

    /// Check if WASI Preview 2 is enabled
    pub fn is_wasi_p2_enabled(&self) -> bool {
        self.wasi_p2_enabled
    }

    /// Check if the linker is valid
    pub fn is_valid(&self) -> bool {
        !self.disposed
    }

    /// Dispose the linker
    pub fn dispose(&mut self) {
        if !self.disposed {
            self.host_functions.clear();
            self.defined_interfaces.clear();
            self.disposed = true;
            log::debug!("ComponentLinker disposed");
        }
    }

    /// Get the engine
    pub fn engine(&self) -> &WasmtimeEngine {
        &self.engine
    }

    /// Get number of defined host functions
    pub fn host_function_count(&self) -> usize {
        self.host_functions.len()
    }
}

impl Drop for ComponentLinker {
    fn drop(&mut self) {
        // Remove host functions from registry
        for (_, id) in &self.host_functions {
            if let Ok(mut registry) = get_component_host_function_registry().lock() {
                registry.remove(id);
            }
        }
        log::debug!("ComponentLinker dropped");
    }
}

/// Parse a WIT path into namespace, interface, and function components
pub fn parse_wit_path(wit_path: &str) -> WasmtimeResult<(String, String, String)> {
    // Expected formats:
    // - "namespace:package/interface#function"
    // - "namespace:package/interface@version#function"

    let parts: Vec<&str> = wit_path.split('#').collect();
    if parts.len() != 2 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid WIT path format: {}", wit_path),
        });
    }

    let function_name = parts[1].to_string();
    let interface_part = parts[0];

    // Remove version if present
    let interface_part = interface_part.split('@').next().unwrap_or(interface_part);

    let namespace_parts: Vec<&str> = interface_part.split('/').collect();
    if namespace_parts.len() != 2 {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid WIT interface path: {}", interface_part),
        });
    }

    let namespace = namespace_parts[0].to_string();
    let interface = namespace_parts[1].to_string();

    Ok((namespace, interface, function_name))
}

//
// Component Linker Core Module (shared between JNI and Panama)
//

/// Core functions for component linker operations
pub mod component_linker_core {
    use super::*;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;
    use std::os::raw::c_void;

    /// Create a new component linker
    pub fn create_component_linker(
        engine: &WasmtimeEngine,
    ) -> WasmtimeResult<Box<ComponentLinker>> {
        ComponentLinker::new(engine).map(Box::new)
    }

    /// Get component linker reference from pointer
    pub unsafe fn get_component_linker_ref(
        linker_ptr: *const c_void,
    ) -> WasmtimeResult<&'static ComponentLinker> {
        validate_ptr_not_null!(linker_ptr, "component linker");
        Ok(&*(linker_ptr as *const ComponentLinker))
    }

    /// Get component linker mutable reference from pointer
    pub unsafe fn get_component_linker_mut(
        linker_ptr: *mut c_void,
    ) -> WasmtimeResult<&'static mut ComponentLinker> {
        validate_ptr_not_null!(linker_ptr, "component linker");
        Ok(&mut *(linker_ptr as *mut ComponentLinker))
    }

    /// Destroy component linker
    pub unsafe fn destroy_component_linker(linker_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<ComponentLinker>(linker_ptr, "ComponentLinker");
    }

    /// Define a host function
    pub fn define_host_function(
        linker: &mut ComponentLinker,
        interface_namespace: &str,
        interface_name: &str,
        function_name: &str,
        callback: Box<dyn ComponentHostCallback>,
    ) -> WasmtimeResult<u64> {
        linker.define_function(interface_namespace, interface_name, function_name, callback)
    }

    /// Define a host function by WIT path
    pub fn define_host_function_by_path(
        linker: &mut ComponentLinker,
        wit_path: &str,
        callback: Box<dyn ComponentHostCallback>,
    ) -> WasmtimeResult<u64> {
        linker.define_function_by_path(wit_path, callback)
    }

    /// Enable WASI Preview 2
    pub fn enable_wasi_p2(linker: &mut ComponentLinker) -> WasmtimeResult<()> {
        linker.enable_wasi_preview2()
    }

    /// Instantiate component
    pub fn instantiate_component(
        linker: &ComponentLinker,
        component: &Component,
    ) -> WasmtimeResult<Arc<ComponentInstance>> {
        linker.instantiate(component)
    }

    /// Get host function from registry
    pub fn get_host_function(id: u64) -> WasmtimeResult<Arc<ComponentHostFunctionEntry>> {
        let registry = get_component_host_function_registry().lock().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to lock component host function registry: {}", e),
            }
        })?;

        registry
            .get(&id)
            .cloned()
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Component host function not found: {}", id),
            })
    }

    /// Remove host function from registry
    pub fn remove_host_function(id: u64) -> WasmtimeResult<()> {
        let mut registry = get_component_host_function_registry().lock().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to lock component host function registry: {}", e),
            }
        })?;

        registry
            .remove(&id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Component host function not found for removal: {}", id),
            })?;

        Ok(())
    }

    /// Get registry statistics
    pub fn get_registry_stats() -> WasmtimeResult<(usize, u64)> {
        let registry = get_component_host_function_registry().lock().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to lock component host function registry: {}", e),
            }
        })?;

        let count = registry.len();
        let next_id = NEXT_COMPONENT_HOST_FUNCTION_ID.load(std::sync::atomic::Ordering::SeqCst);

        Ok((count, next_id))
    }
}

//
// Component Linker FFI Functions
//

use super::ComponentEngine;
use std::ffi::{CStr, CString};
use std::os::raw::{c_char, c_int, c_void};

const FFI_SUCCESS: c_int = 0;
const FFI_ERROR: c_int = -1;

/// Create a new component linker
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_new(engine_ptr: *const c_void) -> *mut c_void {
    if engine_ptr.is_null() {
        return std::ptr::null_mut();
    }

    // Get the component engine and use its engine
    let component_engine = &*(engine_ptr as *const ComponentEngine);

    match ComponentLinker::new(&component_engine.engine) {
        Ok(linker) => Box::into_raw(Box::new(linker)) as *mut c_void,
        Err(e) => {
            log::error!("Failed to create component linker: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Create a component linker from a raw Wasmtime engine
/// Note: engine_ptr should point to an EngineWrapper (from crate::engine::Engine)
///
/// # Safety
///
/// This function uses defensive programming to prevent JVM crashes.
/// All pointer operations are validated before use.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_new_with_engine(
    engine_ptr: *const c_void,
) -> *mut c_void {
    if engine_ptr.is_null() {
        log::error!("wasmtime4j_component_linker_new_with_engine: engine_ptr is null");
        return std::ptr::null_mut();
    }

    // Use catch_unwind to prevent panics from crashing the JVM
    let result = std::panic::catch_unwind(std::panic::AssertUnwindSafe(|| {
        // Create a fresh engine with component model support enabled
        // Note: This creates a new engine per call, but is necessary for FFI safety
        // as the ComponentLinker stores ownership of the engine
        let mut config = crate::engine::safe_wasmtime_config();
        config.wasm_component_model(true);
        config.wasm_bulk_memory(true);
        config.wasm_multi_value(true);
        config.wasm_simd(true);
        config.wasm_reference_types(true);

        let fresh_engine = match WasmtimeEngine::new(&config) {
            Ok(e) => e,
            Err(err) => {
                log::error!(
                    "Failed to create fresh engine for component linker: {}",
                    err
                );
                return std::ptr::null_mut();
            }
        };

        // Create component linker with the fresh engine
        let linker = Linker::new(&fresh_engine);

        let component_linker = ComponentLinker {
            engine: fresh_engine,
            linker,
            host_functions: HashMap::new(),
            defined_interfaces: HashMap::new(),
            wasi_p2_enabled: false,
            wasi_http_enabled: false,
            wasi_p2_config: WasiP2Config::default(),
            disposed: false,
        };

        Box::into_raw(Box::new(component_linker)) as *mut c_void
    }));

    match result {
        Ok(ptr) => ptr,
        Err(e) => {
            log::error!(
                "wasmtime4j_component_linker_new_with_engine: panic caught: {:?}",
                e.downcast_ref::<&str>().unwrap_or(&"unknown panic")
            );
            std::ptr::null_mut()
        }
    }
}

/// Destroy a component linker
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_destroy(linker_ptr: *mut c_void) {
    if !linker_ptr.is_null() {
        let _ = Box::from_raw(linker_ptr as *mut ComponentLinker);
    }
}

/// Check if component linker is valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_is_valid(linker_ptr: *const c_void) -> c_int {
    if linker_ptr.is_null() {
        return 0;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    if linker.is_valid() {
        1
    } else {
        0
    }
}

/// Dispose component linker resources
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_dispose(linker_ptr: *mut c_void) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.dispose();
    FFI_SUCCESS
}

/// Check if an interface is defined in the linker
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_has_interface(
    linker_ptr: *const c_void,
    namespace: *const c_char,
    interface_name: *const c_char,
) -> c_int {
    if linker_ptr.is_null() || namespace.is_null() || interface_name.is_null() {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);

    let ns_str = match CStr::from_ptr(namespace).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let iface_str = match CStr::from_ptr(interface_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    if linker.has_interface(ns_str, iface_str) {
        1
    } else {
        0
    }
}

/// Check if a function is defined in the linker
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_has_function(
    linker_ptr: *const c_void,
    namespace: *const c_char,
    interface_name: *const c_char,
    function_name: *const c_char,
) -> c_int {
    if linker_ptr.is_null()
        || namespace.is_null()
        || interface_name.is_null()
        || function_name.is_null()
    {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);

    let ns_str = match CStr::from_ptr(namespace).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let iface_str = match CStr::from_ptr(interface_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let func_str = match CStr::from_ptr(function_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    if linker.has_function(ns_str, iface_str, func_str) {
        1
    } else {
        0
    }
}

/// Get number of defined host functions
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_host_function_count(
    linker_ptr: *const c_void,
) -> usize {
    if linker_ptr.is_null() {
        return 0;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    linker.host_function_count()
}

/// Get number of defined interfaces
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_interface_count(
    linker_ptr: *const c_void,
) -> usize {
    if linker_ptr.is_null() {
        return 0;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    linker.defined_interfaces.len()
}

/// Check if WASI Preview 2 is enabled
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_wasi_p2_enabled(
    linker_ptr: *const c_void,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    if linker.is_wasi_p2_enabled() {
        1
    } else {
        0
    }
}

/// Enable WASI Preview 2
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_enable_wasi_p2(
    linker_ptr: *mut c_void,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);

    match linker.enable_wasi_preview2() {
        Ok(()) => FFI_SUCCESS,
        Err(e) => {
            log::error!("Failed to enable WASI Preview 2: {}", e);
            FFI_ERROR
        }
    }
}

/// Set WASI Preview 2 arguments (JSON array of strings)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_args(
    linker_ptr: *mut c_void,
    args_json: *const c_char,
) -> c_int {
    if linker_ptr.is_null() || args_json.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);

    let json_str = match CStr::from_ptr(args_json).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    // Parse JSON array of strings
    let args: Vec<String> = match serde_json::from_str(json_str) {
        Ok(a) => a,
        Err(e) => {
            log::error!("Failed to parse WASI args JSON: {}", e);
            return FFI_ERROR;
        }
    };

    linker.set_wasi_args(args);
    FFI_SUCCESS
}

/// Add a WASI Preview 2 environment variable
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_add_wasi_env(
    linker_ptr: *mut c_void,
    key: *const c_char,
    value: *const c_char,
) -> c_int {
    if linker_ptr.is_null() || key.is_null() || value.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);

    let key_str = match CStr::from_ptr(key).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return FFI_ERROR,
    };

    let value_str = match CStr::from_ptr(value).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return FFI_ERROR,
    };

    linker.wasi_p2_config.env.insert(key_str, value_str);
    FFI_SUCCESS
}

/// Set whether to inherit environment from host
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_inherit_env(
    linker_ptr: *mut c_void,
    inherit: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_inherit_env(inherit != 0);
    FFI_SUCCESS
}

/// Set whether to inherit stdio from host
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_inherit_stdio(
    linker_ptr: *mut c_void,
    inherit: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_inherit_stdio(inherit != 0);
    FFI_SUCCESS
}

/// Add a preopened directory
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_add_wasi_preopen_dir(
    linker_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
    read_only: c_int,
) -> c_int {
    if linker_ptr.is_null() || host_path.is_null() || guest_path.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);

    let host_str = match CStr::from_ptr(host_path).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return FFI_ERROR,
    };

    let guest_str = match CStr::from_ptr(guest_path).to_str() {
        Ok(s) => s.to_string(),
        Err(_) => return FFI_ERROR,
    };

    linker.add_wasi_preopen_dir(host_str, guest_str, read_only != 0);
    FFI_SUCCESS
}

/// Set whether network access is allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_network(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_network(allow != 0);
    FFI_SUCCESS
}

/// Set whether clock access is allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_clock(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_clock(allow != 0);
    FFI_SUCCESS
}

/// Set whether random number generation is allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_random(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_random(allow != 0);
    FFI_SUCCESS
}

/// Enable WASI HTTP support
///
/// This enables HTTP request/response functionality in WebAssembly components.
/// WASI Preview 2 must be enabled first for this to work.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_enable_wasi_http(
    linker_ptr: *mut c_void,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);

    match linker.enable_wasi_http() {
        Ok(()) => FFI_SUCCESS,
        Err(e) => {
            log::error!("Failed to enable WASI HTTP: {}", e);
            FFI_ERROR
        }
    }
}

/// Check if WASI HTTP is enabled
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_wasi_http_enabled(
    linker_ptr: *const c_void,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    if linker.is_wasi_http_enabled() {
        1
    } else {
        0
    }
}

/// Instantiate a component using the linker
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_instantiate(
    linker_ptr: *const c_void,
    component_ptr: *const c_void,
    instance_out: *mut *mut c_void,
) -> c_int {
    if linker_ptr.is_null() || component_ptr.is_null() || instance_out.is_null() {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    let component = &*(component_ptr as *const Component);

    match linker.instantiate(component) {
        Ok(instance) => {
            *instance_out = Box::into_raw(Box::new(instance)) as *mut c_void;
            FFI_SUCCESS
        }
        Err(e) => {
            log::error!("Failed to instantiate component: {}", e);
            FFI_ERROR
        }
    }
}

/// Get all defined interface names (returns JSON array)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_get_interfaces(
    linker_ptr: *const c_void,
    json_out: *mut *mut c_char,
) -> c_int {
    if linker_ptr.is_null() || json_out.is_null() {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    let interfaces = linker.get_defined_interfaces();

    // Build JSON array
    let json_str = format!(
        "[{}]",
        interfaces
            .iter()
            .map(|s| format!("\"{}\"", s))
            .collect::<Vec<_>>()
            .join(",")
    );

    match CString::new(json_str) {
        Ok(c_string) => {
            *json_out = c_string.into_raw();
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Get functions for a specific interface (returns JSON array)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_get_functions(
    linker_ptr: *const c_void,
    namespace: *const c_char,
    interface_name: *const c_char,
    json_out: *mut *mut c_char,
) -> c_int {
    if linker_ptr.is_null() || namespace.is_null() || interface_name.is_null() || json_out.is_null()
    {
        return FFI_ERROR;
    }

    let linker = &*(linker_ptr as *const ComponentLinker);

    let ns_str = match CStr::from_ptr(namespace).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let iface_str = match CStr::from_ptr(interface_name).to_str() {
        Ok(s) => s,
        Err(_) => return FFI_ERROR,
    };

    let functions = linker.get_defined_functions(ns_str, iface_str);

    // Build JSON array
    let json_str = format!(
        "[{}]",
        functions
            .iter()
            .map(|s| format!("\"{}\"", s))
            .collect::<Vec<_>>()
            .join(",")
    );

    match CString::new(json_str) {
        Ok(c_string) => {
            *json_out = c_string.into_raw();
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}
