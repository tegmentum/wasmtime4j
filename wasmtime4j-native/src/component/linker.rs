//! Component Model linker for defining host functions and instantiating components
//!
//! This module provides the ComponentLinker for binding host functions to WIT interfaces
//! and instantiating WebAssembly components.

use super::{Component, ComponentStoreData};
use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use std::time::Instant;
use std::sync::atomic::{AtomicU64, Ordering};
use wasmtime::{
    component::{Instance as ComponentInstance, InstancePre, Linker, ResourceTable, ResourceType, Val},
    Engine as WasmtimeEngine, Store, StoreContextMut,
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
    /// Future handle (component-model-async)
    Future(u64),
    /// Stream handle (component-model-async)
    Stream(u64),
    /// Error context handle (component-model-async)
    ErrorContext(u64),
}

/// Trait for Component Model host function callbacks
pub trait ComponentHostCallback: Send + Sync {
    /// Execute the host function with Component Model values
    fn execute(&self, params: &[ComponentValue]) -> WasmtimeResult<Vec<ComponentValue>>;

    /// Clone the callback for use across invocations
    fn clone_callback(&self) -> Box<dyn ComponentHostCallback>;
}

/// Trait for resource destructor callbacks
pub trait ResourceDestructorCallback: Send + Sync {
    /// Called when the guest drops a resource. The `rep` is the resource representation handle.
    fn destroy(&self, rep: u32) -> WasmtimeResult<()>;
}

// =============================================================================
// Async handle (Future/Stream/ErrorContext) registry
// =============================================================================

use std::sync::Mutex as StdMutex;

/// Registry for async component model handle types (Future, Stream, ErrorContext).
/// These opaque handles require Store context to be useful, so we store the
/// original Val by ID and allow Java to pass them back opaquely.
static ASYNC_VAL_REGISTRY: std::sync::OnceLock<StdMutex<AsyncValRegistry>> =
    std::sync::OnceLock::new();

fn get_async_val_registry() -> &'static StdMutex<AsyncValRegistry> {
    ASYNC_VAL_REGISTRY.get_or_init(|| StdMutex::new(AsyncValRegistry::new()))
}

struct AsyncValRegistry {
    next_id: u64,
    vals: HashMap<u64, Val>,
}

impl AsyncValRegistry {
    fn new() -> Self {
        Self {
            next_id: 1,
            vals: HashMap::new(),
        }
    }

    fn store(&mut self, val: Val) -> u64 {
        let id = self.next_id;
        self.next_id += 1;
        self.vals.insert(id, val);
        id
    }

    fn get(&self, id: u64) -> Option<&Val> {
        self.vals.get(&id)
    }
}

// =============================================================================
// Val <-> ComponentValue conversions
// =============================================================================

/// Convert a wasmtime `Val` to a `ComponentValue`.
pub fn val_to_component_value(val: &Val) -> ComponentValue {
    match val {
        Val::Bool(b) => ComponentValue::Bool(*b),
        Val::S8(v) => ComponentValue::S8(*v),
        Val::S16(v) => ComponentValue::S16(*v),
        Val::S32(v) => ComponentValue::S32(*v),
        Val::S64(v) => ComponentValue::S64(*v),
        Val::U8(v) => ComponentValue::U8(*v),
        Val::U16(v) => ComponentValue::U16(*v),
        Val::U32(v) => ComponentValue::U32(*v),
        Val::U64(v) => ComponentValue::U64(*v),
        Val::Float32(v) => ComponentValue::F32(*v),
        Val::Float64(v) => ComponentValue::F64(*v),
        Val::Char(c) => ComponentValue::Char(*c),
        Val::String(s) => ComponentValue::String(s.to_string()),
        Val::List(items) => {
            ComponentValue::List(items.iter().map(val_to_component_value).collect())
        }
        Val::Record(fields) => ComponentValue::Record(
            fields
                .iter()
                .map(|(name, val)| (name.to_string(), val_to_component_value(val)))
                .collect(),
        ),
        Val::Tuple(elements) => {
            ComponentValue::Tuple(elements.iter().map(val_to_component_value).collect())
        }
        Val::Variant(case_name, payload) => ComponentValue::Variant {
            case_name: case_name.to_string(),
            payload: payload.as_ref().map(|v| Box::new(val_to_component_value(v))),
        },
        Val::Enum(name) => ComponentValue::Enum(name.to_string()),
        Val::Option(opt) => {
            ComponentValue::Option(opt.as_ref().map(|v| Box::new(val_to_component_value(v))))
        }
        Val::Result(result) => match result {
            Ok(ok) => ComponentValue::Result {
                ok: ok.as_ref().map(|v| Box::new(val_to_component_value(v))),
                err: None,
                is_ok: true,
            },
            Err(err) => ComponentValue::Result {
                ok: None,
                err: err.as_ref().map(|v| Box::new(val_to_component_value(v))),
                is_ok: false,
            },
        },
        Val::Flags(flags) => {
            ComponentValue::Flags(flags.iter().map(|f| f.to_string()).collect())
        }
        Val::Resource(resource) => {
            let handle = crate::wit_value_marshal::store_resource(resource.clone());
            ComponentValue::Own(handle)
        }
        Val::Future(_) => {
            let mut registry = get_async_val_registry().lock().unwrap_or_else(|e| e.into_inner());
            let handle = registry.store(val.clone());
            ComponentValue::Future(handle)
        }
        Val::Stream(_) => {
            let mut registry = get_async_val_registry().lock().unwrap_or_else(|e| e.into_inner());
            let handle = registry.store(val.clone());
            ComponentValue::Stream(handle)
        }
        Val::ErrorContext(_) => {
            let mut registry = get_async_val_registry().lock().unwrap_or_else(|e| e.into_inner());
            let handle = registry.store(val.clone());
            ComponentValue::ErrorContext(handle)
        }
    }
}

/// Convert a `ComponentValue` to a wasmtime `Val`.
pub fn component_value_to_val(cv: &ComponentValue) -> Val {
    match cv {
        ComponentValue::Bool(b) => Val::Bool(*b),
        ComponentValue::S8(v) => Val::S8(*v),
        ComponentValue::S16(v) => Val::S16(*v),
        ComponentValue::S32(v) => Val::S32(*v),
        ComponentValue::S64(v) => Val::S64(*v),
        ComponentValue::U8(v) => Val::U8(*v),
        ComponentValue::U16(v) => Val::U16(*v),
        ComponentValue::U32(v) => Val::U32(*v),
        ComponentValue::U64(v) => Val::U64(*v),
        ComponentValue::F32(v) => Val::Float32(*v),
        ComponentValue::F64(v) => Val::Float64(*v),
        ComponentValue::Char(c) => Val::Char(*c),
        ComponentValue::String(s) => Val::String(s.clone().into()),
        ComponentValue::List(items) => {
            Val::List(items.iter().map(component_value_to_val).collect())
        }
        ComponentValue::Record(fields) => Val::Record(
            fields
                .iter()
                .map(|(name, val)| (name.clone().into(), component_value_to_val(val)))
                .collect(),
        ),
        ComponentValue::Tuple(elements) => {
            Val::Tuple(elements.iter().map(component_value_to_val).collect())
        }
        ComponentValue::Variant {
            case_name,
            payload,
        } => Val::Variant(
            case_name.clone().into(),
            payload.as_ref().map(|v| Box::new(component_value_to_val(v))),
        ),
        ComponentValue::Enum(name) => Val::Enum(name.clone().into()),
        ComponentValue::Option(opt) => {
            Val::Option(opt.as_ref().map(|v| Box::new(component_value_to_val(v))))
        }
        ComponentValue::Result { ok, err, is_ok } => {
            if *is_ok {
                Val::Result(Ok(
                    ok.as_ref().map(|v| Box::new(component_value_to_val(v)))
                ))
            } else {
                Val::Result(Err(
                    err.as_ref().map(|v| Box::new(component_value_to_val(v)))
                ))
            }
        }
        ComponentValue::Flags(flags) => {
            Val::Flags(flags.iter().map(|f| f.clone().into()).collect())
        }
        ComponentValue::Own(handle) | ComponentValue::Borrow(handle) => {
            if let Some(resource) = crate::wit_value_marshal::get_resource(*handle) {
                Val::Resource(resource)
            } else {
                log::warn!("Resource handle {} not found in registry", handle);
                Val::Bool(false)
            }
        }
        ComponentValue::Future(handle)
        | ComponentValue::Stream(handle)
        | ComponentValue::ErrorContext(handle) => {
            let registry = get_async_val_registry().lock().unwrap_or_else(|e| e.into_inner());
            if let Some(val) = registry.get(*handle) {
                val.clone()
            } else {
                log::warn!("Async handle {} not found in registry", handle);
                Val::Bool(false)
            }
        }
    }
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

/// Callback-based wall clock implementation for Java FFI.
///
/// Uses extern "C" function pointers to delegate clock operations to Java code.
/// The `now_fn` returns wall time as (seconds, nanoseconds) since Unix epoch.
/// The `resolution_fn` returns clock resolution as (seconds, nanoseconds).
#[derive(Debug, Clone, Copy)]
pub struct CallbackWallClock {
    /// Callback for `now()`: (callback_id) -> writes to (seconds_out, nanos_out)
    pub now_fn: extern "C" fn(i64, *mut i64, *mut u32),
    /// Callback for `resolution()`: (callback_id) -> writes to (seconds_out, nanos_out)
    pub resolution_fn: extern "C" fn(i64, *mut i64, *mut u32),
    /// Identifier for the Java-side clock implementation
    pub callback_id: i64,
}

// SAFETY: The function pointers are thread-safe as they synchronously call back to Java.
unsafe impl Send for CallbackWallClock {}

#[cfg(feature = "wasi")]
impl wasmtime_wasi::HostWallClock for CallbackWallClock {
    fn resolution(&self) -> std::time::Duration {
        let mut seconds: i64 = 0;
        let mut nanos: u32 = 0;
        (self.resolution_fn)(self.callback_id, &mut seconds, &mut nanos);
        std::time::Duration::new(seconds as u64, nanos)
    }

    fn now(&self) -> std::time::Duration {
        let mut seconds: i64 = 0;
        let mut nanos: u32 = 0;
        (self.now_fn)(self.callback_id, &mut seconds, &mut nanos);
        std::time::Duration::new(seconds as u64, nanos)
    }
}

/// Callback-based monotonic clock implementation for Java FFI.
///
/// Uses extern "C" function pointers to delegate clock operations to Java code.
/// The `now_fn` returns monotonic time in nanoseconds.
/// The `resolution_fn` returns clock resolution in nanoseconds.
#[derive(Debug, Clone, Copy)]
pub struct CallbackMonotonicClock {
    /// Callback for `now()`: (callback_id) -> nanoseconds u64
    pub now_fn: extern "C" fn(i64) -> u64,
    /// Callback for `resolution()`: (callback_id) -> nanoseconds u64
    pub resolution_fn: extern "C" fn(i64) -> u64,
    /// Identifier for the Java-side clock implementation
    pub callback_id: i64,
}

// SAFETY: The function pointers are thread-safe as they synchronously call back to Java.
unsafe impl Send for CallbackMonotonicClock {}

#[cfg(feature = "wasi")]
impl wasmtime_wasi::HostMonotonicClock for CallbackMonotonicClock {
    fn resolution(&self) -> u64 {
        (self.resolution_fn)(self.callback_id)
    }

    fn now(&self) -> u64 {
        (self.now_fn)(self.callback_id)
    }
}

/// Callback-based RNG implementation for Java FFI.
///
/// Uses extern "C" function pointers to delegate random number generation to Java code.
/// The `fill_bytes_fn` fills a buffer with random bytes.
#[derive(Debug, Clone, Copy)]
pub struct CallbackRng {
    /// Callback for `fill_bytes()`: (callback_id, buf_ptr, buf_len)
    pub fill_bytes_fn: extern "C" fn(i64, *mut u8, usize),
    /// Identifier for the Java-side RNG implementation
    pub callback_id: i64,
}

// SAFETY: The function pointers are thread-safe as they synchronously call back to Java.
unsafe impl Send for CallbackRng {}

#[cfg(feature = "wasi")]
impl rand_core::RngCore for CallbackRng {
    fn next_u32(&mut self) -> u32 {
        let mut buf = [0u8; 4];
        self.fill_bytes(&mut buf);
        u32::from_le_bytes(buf)
    }

    fn next_u64(&mut self) -> u64 {
        let mut buf = [0u8; 8];
        self.fill_bytes(&mut buf);
        u64::from_le_bytes(buf)
    }

    fn fill_bytes(&mut self, dest: &mut [u8]) {
        (self.fill_bytes_fn)(self.callback_id, dest.as_mut_ptr(), dest.len());
    }

    fn try_fill_bytes(&mut self, dest: &mut [u8]) -> Result<(), rand_core::Error> {
        self.fill_bytes(dest);
        Ok(())
    }
}

/// Callback-based socket address check for Java FFI.
///
/// Uses an extern "C" function pointer to delegate socket address validation to Java code.
/// The callback receives IP address bytes, port, and use type, returning non-zero to allow.
#[derive(Debug, Clone, Copy)]
pub struct CallbackSocketAddrCheck {
    /// Callback: (callback_id, ip_v4_or_v6: i32, ip_bytes_ptr, ip_bytes_len, port: u16, use_type: i32) -> i32
    /// Returns non-zero to allow, zero to deny.
    /// ip_v4_or_v6: 4 for IPv4, 6 for IPv6
    /// use_type: 0=TcpBind, 1=TcpConnect, 2=UdpBind, 3=UdpConnect, 4=UdpOutgoingDatagram
    pub check_fn: extern "C" fn(i64, i32, *const u8, usize, u16, i32) -> i32,
    /// Identifier for the Java-side check implementation
    pub callback_id: i64,
}

// SAFETY: The function pointers are thread-safe as they synchronously call back to Java.
unsafe impl Send for CallbackSocketAddrCheck {}
unsafe impl Sync for CallbackSocketAddrCheck {}

/// WASI Preview 2 configuration for component model
#[derive(Clone)]
pub struct WasiP2Config {
    /// Command-line arguments
    pub args: Vec<String>,
    /// Whether to inherit arguments from host
    pub inherit_args: bool,
    /// Environment variables
    pub env: HashMap<String, String>,
    /// Whether to inherit environment from host
    pub inherit_env: bool,
    /// Whether to inherit stdio from host
    pub inherit_stdio: bool,
    /// Whether to inherit stdin from host individually
    pub inherit_stdin: bool,
    /// Whether to inherit stdout from host individually
    pub inherit_stdout: bool,
    /// Whether to inherit stderr from host individually
    pub inherit_stderr: bool,
    /// Preopened directories (host_path, guest_path, dir_perms_bits, file_perms_bits)
    pub preopened_dirs: Vec<(String, String, u32, u32)>,
    /// Allow network access
    pub allow_network: bool,
    /// Allow TCP sockets
    pub allow_tcp: bool,
    /// Allow UDP sockets
    pub allow_udp: bool,
    /// Allow IP name lookup (DNS)
    pub allow_ip_name_lookup: bool,
    /// Allow clock access (reserved for future use — WasiCtxBuilder enables clocks by default)
    pub allow_clock: bool,
    /// Allow random number generation (reserved for future use — WasiCtxBuilder enables RNG by default)
    pub allow_random: bool,
    /// Allow blocking the current thread
    pub allow_blocking_current_thread: bool,
    /// Insecure random seed (if set)
    pub insecure_random_seed: Option<u64>,
    /// Custom wall clock callback (if set)
    pub wall_clock: Option<CallbackWallClock>,
    /// Custom monotonic clock callback (if set)
    pub monotonic_clock: Option<CallbackMonotonicClock>,
    /// Custom secure random callback (if set)
    pub secure_random: Option<CallbackRng>,
    /// Custom insecure random callback (if set)
    pub insecure_random: Option<CallbackRng>,
    /// Custom socket address check callback (if set)
    pub socket_addr_check: Option<CallbackSocketAddrCheck>,
}

impl Default for WasiP2Config {
    fn default() -> Self {
        Self {
            args: Vec::new(),
            inherit_args: false,
            env: HashMap::new(),
            inherit_env: false,
            inherit_stdio: false,
            inherit_stdin: false,
            inherit_stdout: false,
            inherit_stderr: false,
            preopened_dirs: Vec::new(),
            allow_network: false,
            allow_tcp: true,
            allow_udp: true,
            allow_ip_name_lookup: true,
            allow_clock: false,
            allow_random: false,
            allow_blocking_current_thread: false,
            insecure_random_seed: None,
            wall_clock: None,
            monotonic_clock: None,
            secure_random: None,
            insecure_random: None,
            socket_addr_check: None,
        }
    }
}

impl WasiP2Config {
    /// Build a WasiCtx from the stored configuration
    #[cfg(feature = "wasi")]
    pub fn build_wasi_ctx(&self) -> wasmtime_wasi::WasiCtx {
        use wasmtime_wasi::WasiCtxBuilder;

        let mut builder = WasiCtxBuilder::new();

        // Set args
        if self.inherit_args {
            builder.inherit_args();
        } else if !self.args.is_empty() {
            let args_refs: Vec<&str> = self.args.iter().map(|s| s.as_str()).collect();
            builder.args(&args_refs);
        }

        // Set environment
        if self.inherit_env {
            builder.inherit_env();
        } else if !self.env.is_empty() {
            let env_refs: Vec<(&str, &str)> = self
                .env
                .iter()
                .map(|(k, v)| (k.as_str(), v.as_str()))
                .collect();
            builder.envs(&env_refs);
        }

        // Set stdio
        if self.inherit_stdio {
            builder.inherit_stdio();
        } else {
            if self.inherit_stdin {
                builder.inherit_stdin();
            }
            if self.inherit_stdout {
                builder.inherit_stdout();
            }
            if self.inherit_stderr {
                builder.inherit_stderr();
            }
        }

        // Network controls
        crate::wasi_common_config::apply_network_config(
            &mut builder,
            self.allow_network,
            self.allow_tcp,
            self.allow_udp,
            self.allow_ip_name_lookup,
        );

        // Allow blocking current thread
        builder.allow_blocking_current_thread(self.allow_blocking_current_thread);

        // Custom clocks and RNG
        crate::wasi_common_config::apply_clock_and_rng_config(
            &mut builder,
            self.insecure_random_seed,
            self.wall_clock,
            self.monotonic_clock,
            self.secure_random,
            self.insecure_random,
        );

        // Socket address check callback
        crate::wasi_common_config::apply_socket_addr_check(
            &mut builder,
            self.socket_addr_check,
        );

        // Preopened directories with granular permissions
        for (host_path, guest_path, dir_bits, file_bits) in &self.preopened_dirs {
            let path = std::path::Path::new(host_path);
            if path.exists() && path.is_dir() {
                let (dir_perms, file_perms) =
                    crate::wasi_common_config::decode_permissions(*dir_bits, *file_bits);
                if let Err(e) = builder.preopened_dir(path, guest_path, dir_perms, file_perms) {
                    log::warn!("Failed to preopen directory {}: {}", host_path, e);
                }
            }
        }

        builder.build()
    }
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
    /// Whether the engine supports async operations
    async_support: bool,
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
            async_support: false,
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
            async_support: false,
        })
    }

    /// Set whether async support is enabled for this linker.
    ///
    /// This must match the engine's `async_support` configuration.
    /// When enabled, `define_function_async` can be used to register
    /// async host functions via `func_new_async`.
    pub fn set_async_support(&mut self, enabled: bool) {
        self.async_support = enabled;
    }

    /// Check if async support is enabled for this linker.
    pub fn async_support_enabled(&self) -> bool {
        self.async_support
    }

    /// Configure WASI Preview 2 args
    pub fn set_wasi_args(&mut self, args: Vec<String>) {
        self.wasi_p2_config.args = args;
    }

    /// Configure WASI Preview 2 environment variables
    pub fn set_wasi_env(&mut self, env: HashMap<String, String>) {
        self.wasi_p2_config.env = env;
    }

    /// Set whether to inherit arguments from host
    pub fn set_wasi_inherit_args(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_args = inherit;
    }

    /// Set whether to inherit environment from host
    pub fn set_wasi_inherit_env(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_env = inherit;
    }

    /// Set whether to inherit stdio from host
    pub fn set_wasi_inherit_stdio(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_stdio = inherit;
    }

    /// Add a preopened directory with permission bits
    pub fn add_wasi_preopen_dir(
        &mut self,
        host_path: String,
        guest_path: String,
        dir_perms_bits: u32,
        file_perms_bits: u32,
    ) {
        self.wasi_p2_config
            .preopened_dirs
            .push((host_path, guest_path, dir_perms_bits, file_perms_bits));
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

    /// Set whether to inherit stdin individually
    pub fn set_wasi_inherit_stdin(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_stdin = inherit;
    }

    /// Set whether to inherit stdout individually
    pub fn set_wasi_inherit_stdout(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_stdout = inherit;
    }

    /// Set whether to inherit stderr individually
    pub fn set_wasi_inherit_stderr(&mut self, inherit: bool) {
        self.wasi_p2_config.inherit_stderr = inherit;
    }

    /// Set whether TCP sockets are allowed
    pub fn set_wasi_allow_tcp(&mut self, allow: bool) {
        self.wasi_p2_config.allow_tcp = allow;
    }

    /// Set whether UDP sockets are allowed
    pub fn set_wasi_allow_udp(&mut self, allow: bool) {
        self.wasi_p2_config.allow_udp = allow;
    }

    /// Set whether IP name lookup is allowed
    pub fn set_wasi_allow_ip_name_lookup(&mut self, allow: bool) {
        self.wasi_p2_config.allow_ip_name_lookup = allow;
    }

    /// Set whether blocking the current thread is allowed
    pub fn set_wasi_allow_blocking_current_thread(&mut self, allow: bool) {
        self.wasi_p2_config.allow_blocking_current_thread = allow;
    }

    /// Set insecure random seed
    pub fn set_wasi_insecure_random_seed(&mut self, seed: u64) {
        self.wasi_p2_config.insecure_random_seed = Some(seed);
    }

    /// Set custom wall clock callback
    pub fn set_wasi_wall_clock(&mut self, clock: CallbackWallClock) {
        self.wasi_p2_config.wall_clock = Some(clock);
    }

    /// Set custom monotonic clock callback
    pub fn set_wasi_monotonic_clock(&mut self, clock: CallbackMonotonicClock) {
        self.wasi_p2_config.monotonic_clock = Some(clock);
    }

    /// Set custom secure random callback
    pub fn set_wasi_secure_random(&mut self, rng: CallbackRng) {
        self.wasi_p2_config.secure_random = Some(rng);
    }

    /// Set custom insecure random callback
    pub fn set_wasi_insecure_random(&mut self, rng: CallbackRng) {
        self.wasi_p2_config.insecure_random = Some(rng);
    }

    /// Set socket address check callback
    pub fn set_wasi_socket_addr_check(&mut self, check: CallbackSocketAddrCheck) {
        self.wasi_p2_config.socket_addr_check = Some(check);
    }

    /// Get the WASI P2 configuration
    pub fn wasi_p2_config(&self) -> &WasiP2Config {
        &self.wasi_p2_config
    }

    /// Build a WasiCtx from the stored configuration
    #[cfg(feature = "wasi")]
    pub fn build_wasi_ctx(&self) -> wasmtime_wasi::WasiCtx {
        self.wasi_p2_config.build_wasi_ctx()
    }

    /// Define a host function for a WIT interface.
    ///
    /// The `interface_namespace` is the full package prefix (e.g. "wasi:cli")
    /// and `interface_name` is the interface within that package (e.g. "stdout").
    /// The function is registered on wasmtime's component `Linker` at path
    /// `{interface_namespace}/{interface_name}`.
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
            registry.insert(id, entry.clone());
        }

        // Wire the host function into wasmtime's component Linker
        let interface_path = format!("{}/{}", interface_namespace, interface_name);
        self.register_func_on_linker(&interface_path, function_name, entry.clone())?;

        // Build WIT path key for internal tracking
        let wit_path = format!(
            "{}/{}#{}",
            interface_namespace, interface_name, function_name
        );
        self.host_functions.insert(wit_path.clone(), id);

        // Track in defined interfaces
        let interface_key = format!("{}/{}", interface_namespace, interface_name);
        self.defined_interfaces
            .entry(interface_key)
            .or_insert_with(Vec::new)
            .push(function_name.to_string());

        log::debug!("Defined component host function: {} (id={})", wit_path, id);

        Ok(id)
    }

    /// Define a host function using full WIT path.
    ///
    /// The WIT path format is `"namespace:package/interface#function"` or
    /// `"namespace:package/interface@version#function"`.
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
            registry.insert(id, entry.clone());
        }

        // Wire the host function into wasmtime's component Linker.
        // Use the interface part of the WIT path (everything before '#') as the instance path.
        let parts: Vec<&str> = wit_path.split('#').collect();
        let interface_path = parts[0];
        self.register_func_on_linker(interface_path, &function, entry.clone())?;

        self.host_functions.insert(wit_path.to_string(), id);

        // Track in defined interfaces
        let interface_key = interface_path.to_string();
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

    /// Register a host function on wasmtime's component Linker.
    ///
    /// This creates a `func_new` entry on the appropriate `LinkerInstance`
    /// that bridges between wasmtime's `Val` type and our `ComponentValue` type.
    fn register_func_on_linker(
        &mut self,
        interface_path: &str,
        function_name: &str,
        entry: Arc<ComponentHostFunctionEntry>,
    ) -> WasmtimeResult<()> {
        let func_name = function_name.to_string();
        self.linker
            .root()
            .instance(interface_path)
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to get linker instance for '{}': {}",
                    interface_path, e
                ),
            })?
            .func_new(&func_name, move |_store_ctx: StoreContextMut<'_, ComponentStoreData>,
                                         _func_type,
                                         params: &[Val],
                                         results: &mut [Val]| {
                // Convert wasmtime Val params to ComponentValue
                let cv_params: Vec<ComponentValue> =
                    params.iter().map(val_to_component_value).collect();

                // Invoke the host callback
                let cv_results = entry.callback.execute(&cv_params).map_err(|e| {
                    anyhow::anyhow!("Host function callback failed: {}", e)
                })?;

                // Convert ComponentValue results back to wasmtime Val
                for (i, cv) in cv_results.iter().enumerate() {
                    if i < results.len() {
                        results[i] = component_value_to_val(cv);
                    }
                }

                Ok(())
            })
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to register function '{}' on '{}': {}",
                    function_name, interface_path, e
                ),
            })?;

        Ok(())
    }

    /// Register an async host function on wasmtime's component Linker.
    ///
    /// This creates a `func_new_async` entry on the appropriate `LinkerInstance`
    /// that bridges between wasmtime's `Val` type and our `ComponentValue` type.
    /// Requires the engine to have been created with `async_support(true)`.
    fn register_func_on_linker_async(
        &mut self,
        interface_path: &str,
        function_name: &str,
        entry: Arc<ComponentHostFunctionEntry>,
    ) -> WasmtimeResult<()> {
        let func_name = function_name.to_string();
        self.linker
            .root()
            .instance(interface_path)
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to get linker instance for '{}': {}",
                    interface_path, e
                ),
            })?
            .func_new_async(&func_name, move |_store_ctx: StoreContextMut<'_, ComponentStoreData>,
                                               _func_type,
                                               params: &[Val],
                                               results: &mut [Val]| {
                let entry = entry.clone();
                Box::new(async move {
                    // Convert wasmtime Val params to ComponentValue
                    let cv_params: Vec<ComponentValue> =
                        params.iter().map(val_to_component_value).collect();

                    // Invoke the host callback
                    let cv_results = entry.callback.execute(&cv_params).map_err(|e| {
                        anyhow::anyhow!("Host function callback failed: {}", e)
                    })?;

                    // Convert ComponentValue results back to wasmtime Val
                    for (i, cv) in cv_results.iter().enumerate() {
                        if i < results.len() {
                            results[i] = component_value_to_val(cv);
                        }
                    }

                    Ok(())
                })
            })
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to register async function '{}' on '{}': {}",
                    function_name, interface_path, e
                ),
            })?;

        Ok(())
    }

    /// Define an async host function for a WIT interface.
    ///
    /// This is the async variant of [`define_function`]. It uses `func_new_async`
    /// instead of `func_new`, which allows the host function to be called from
    /// async contexts (e.g., when using `call_async` on component functions).
    ///
    /// Requires the engine to have been created with `async_support(true)`.
    pub fn define_function_async(
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

        if !self.async_support {
            return Err(WasmtimeError::Runtime {
                message: "Cannot define async function: engine was not created with async_support(true)".to_string(),
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
            registry.insert(id, entry.clone());
        }

        // Wire the host function into wasmtime's component Linker using async variant
        let interface_path = format!("{}/{}", interface_namespace, interface_name);
        self.register_func_on_linker_async(&interface_path, function_name, entry.clone())?;

        // Build WIT path key for internal tracking
        let wit_path = format!(
            "{}/{}#{}",
            interface_namespace, interface_name, function_name
        );
        self.host_functions.insert(wit_path.clone(), id);

        // Track in defined interfaces
        let interface_key = format!("{}/{}", interface_namespace, interface_name);
        self.defined_interfaces
            .entry(interface_key)
            .or_insert_with(Vec::new)
            .push(function_name.to_string());

        log::debug!("Defined async component host function: {} (id={})", wit_path, id);

        Ok(id)
    }

    /// Define an async host function using full WIT path.
    ///
    /// This is the async variant of [`define_function_by_path`]. It uses
    /// `func_new_async` instead of `func_new`.
    ///
    /// Requires the engine to have been created with `async_support(true)`.
    pub fn define_function_by_path_async(
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

        if !self.async_support {
            return Err(WasmtimeError::Runtime {
                message: "Cannot define async function: engine was not created with async_support(true)".to_string(),
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
            registry.insert(id, entry.clone());
        }

        // Wire the host function into wasmtime's component Linker using async variant.
        let parts: Vec<&str> = wit_path.split('#').collect();
        let interface_path = parts[0];
        self.register_func_on_linker_async(interface_path, &function, entry.clone())?;

        self.host_functions.insert(wit_path.to_string(), id);

        // Track in defined interfaces
        let interface_key = interface_path.to_string();
        self.defined_interfaces
            .entry(interface_key)
            .or_insert_with(Vec::new)
            .push(function);

        log::debug!(
            "Defined async component host function by path: {} (id={})",
            wit_path,
            id
        );

        Ok(id)
    }

    /// Define a host resource type on the linker.
    ///
    /// This registers a resource type with the given name under the specified interface
    /// path. The `resource_id` is used as a dynamic payload for the `ResourceType` and
    /// must be unique per resource definition. The destructor callback is invoked when
    /// the guest drops an owned handle to this resource.
    pub fn define_resource(
        &mut self,
        interface_path: &str,
        resource_name: &str,
        resource_id: u32,
        destructor: Arc<dyn ResourceDestructorCallback>,
    ) -> WasmtimeResult<()> {
        let resource_type = ResourceType::host_dynamic(resource_id);

        self.linker
            .root()
            .instance(interface_path)
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to get linker instance for '{}': {}",
                    interface_path, e
                ),
            })?
            .resource(resource_name, resource_type, move |_store_ctx, rep| {
                destructor.destroy(rep).map_err(|e| {
                    anyhow::anyhow!("Resource destructor failed: {}", e)
                })
            })
            .map_err(|e| WasmtimeError::Linker {
                message: format!(
                    "Failed to define resource '{}' on '{}': {}",
                    resource_name, interface_path, e
                ),
            })?;

        // Track the resource in the defined interfaces
        let interface_key = interface_path.to_string();
        let resource_entry = format!("[resource]{}", resource_name);
        self.defined_interfaces
            .entry(interface_key)
            .or_default()
            .push(resource_entry);

        Ok(())
    }

    /// Check if a specific interface is defined
    pub fn has_interface(&self, interface_namespace: &str, interface_name: &str) -> bool {
        let key = format!("{}/{}", interface_namespace, interface_name);
        self.defined_interfaces.contains_key(&key)
    }

    /// Check if a specific function is defined
    pub fn has_function(
        &self,
        interface_namespace: &str,
        interface_name: &str,
        function_name: &str,
    ) -> bool {
        let key = format!("{}/{}", interface_namespace, interface_name);
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
        let key = format!("{}/{}", interface_namespace, interface_name);
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

    /// Pre-instantiate a component for fast repeated instantiation.
    ///
    /// This performs the expensive type-checking and import resolution work once,
    /// allowing subsequent instantiations to be significantly faster.
    pub fn instantiate_pre(
        &self,
        component: &Component,
    ) -> WasmtimeResult<ComponentInstancePreWrapper> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        let start = Instant::now();

        let instance_pre = self
            .linker
            .instantiate_pre(component.wasmtime_component())
            .map_err(|e| WasmtimeError::Linker {
                message: format!("Failed to pre-instantiate component: {}", e),
            })?;

        let preparation_time_ns = start.elapsed().as_nanos() as u64;

        Ok(ComponentInstancePreWrapper {
            inner: instance_pre,
            engine: self.engine.clone(),
            wasi_p2_enabled: self.wasi_p2_enabled,
            wasi_http_enabled: self.wasi_http_enabled,
            wasi_p2_config: self.wasi_p2_config.clone(),
            preparation_time_ns,
            instance_count: AtomicU64::new(0),
            total_instantiation_time_ns: AtomicU64::new(0),
        })
    }

    /// Check if WASI Preview 2 is enabled
    pub fn is_wasi_p2_enabled(&self) -> bool {
        self.wasi_p2_enabled
    }

    /// Check if the linker is valid
    pub fn is_valid(&self) -> bool {
        !self.disposed
    }

    /// Allow or disallow shadowing of previously defined names.
    ///
    /// When enabled, new definitions can override existing ones without error.
    pub fn allow_shadowing(&mut self, allow: bool) {
        self.linker.allow_shadowing(allow);
    }

    /// Define all unknown imports of the given component as traps.
    ///
    /// This is useful for partially-defined components where some imports
    /// should trap at runtime rather than failing at instantiation time.
    pub fn define_unknown_imports_as_traps(
        &mut self,
        component: &Component,
    ) -> WasmtimeResult<()> {
        if self.disposed {
            return Err(WasmtimeError::Runtime {
                message: "ComponentLinker has been disposed".to_string(),
                backtrace: None,
            });
        }

        self.linker
            .define_unknown_imports_as_traps(component.wasmtime_component())
            .map_err(|e| WasmtimeError::Linker {
                message: format!("Failed to define unknown imports as traps: {}", e),
            })
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

// ============================================================================
// ComponentInstancePre - Pre-instantiated component for fast repeated instantiation
// ============================================================================

/// Pre-instantiated component wrapper with statistics tracking.
///
/// Wraps `wasmtime::component::InstancePre<ComponentStoreData>` to enable fast repeated
/// instantiation of the same component. The expensive work of type-checking and
/// import resolution is done once, and each subsequent instantiation is faster.
pub struct ComponentInstancePreWrapper {
    /// The wasmtime InstancePre handle
    inner: InstancePre<ComponentStoreData>,
    /// Engine for creating stores during instantiation
    engine: WasmtimeEngine,
    /// Whether WASI P2 is enabled (needed to build store data)
    wasi_p2_enabled: bool,
    /// Whether WASI HTTP is enabled
    wasi_http_enabled: bool,
    /// WASI P2 configuration (cloned from linker at creation time)
    wasi_p2_config: WasiP2Config,
    /// Preparation time in nanoseconds
    preparation_time_ns: u64,
    /// Number of instances created from this InstancePre
    instance_count: AtomicU64,
    /// Total instantiation time in nanoseconds
    total_instantiation_time_ns: AtomicU64,
}

impl ComponentInstancePreWrapper {
    /// Instantiate the component, creating a fresh store with the configured WASI context.
    pub fn instantiate(&self) -> WasmtimeResult<Arc<ComponentInstance>> {
        let start = Instant::now();

        // Build store data with configured WASI context if WASI P2 is enabled
        #[cfg(feature = "wasi")]
        let store_data = if self.wasi_p2_enabled {
            ComponentStoreData {
                instance_id: 0,
                user_data: None,
                resource_table: ResourceTable::new(),
                wasi_ctx: self.wasi_p2_config.build_wasi_ctx(),
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
            .inner
            .instantiate(&mut store)
            .map_err(|e| WasmtimeError::Instance {
                message: format!("Failed to instantiate from ComponentInstancePre: {}", e),
            })?;

        let duration = start.elapsed().as_nanos() as u64;
        self.instance_count.fetch_add(1, Ordering::Relaxed);
        self.total_instantiation_time_ns
            .fetch_add(duration, Ordering::Relaxed);

        Ok(Arc::new(instance))
    }

    /// Check if valid
    pub fn is_valid(&self) -> bool {
        true // InstancePre is always valid once created
    }

    /// Get instance count
    pub fn instance_count(&self) -> u64 {
        self.instance_count.load(Ordering::Relaxed)
    }

    /// Get preparation time in nanoseconds
    pub fn preparation_time_ns(&self) -> u64 {
        self.preparation_time_ns
    }

    /// Get average instantiation time in nanoseconds
    pub fn average_instantiation_time_ns(&self) -> u64 {
        let count = self.instance_count.load(Ordering::Relaxed);
        if count == 0 {
            0
        } else {
            self.total_instantiation_time_ns.load(Ordering::Relaxed) / count
        }
    }

    /// Get the engine reference
    pub fn engine(&self) -> &WasmtimeEngine {
        &self.engine
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

    /// Define an async host function by WIT path
    pub fn define_host_function_by_path_async(
        linker: &mut ComponentLinker,
        wit_path: &str,
        callback: Box<dyn ComponentHostCallback>,
    ) -> WasmtimeResult<u64> {
        linker.define_function_by_path_async(wit_path, callback)
    }

    /// Set async support on a component linker
    pub fn set_async_support(linker: &mut ComponentLinker, enabled: bool) {
        linker.set_async_support(enabled);
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

    /// Allow shadowing of definitions
    pub fn allow_shadowing(linker: &mut ComponentLinker, allow: bool) {
        linker.allow_shadowing(allow);
    }

    /// Define unknown imports as traps
    pub fn define_unknown_imports_as_traps(
        linker: &mut ComponentLinker,
        component: &Component,
    ) -> WasmtimeResult<()> {
        linker.define_unknown_imports_as_traps(component)
    }

    /// Pre-instantiate a component
    pub fn instantiate_pre(
        linker: &ComponentLinker,
        component: &Component,
    ) -> WasmtimeResult<Box<ComponentInstancePreWrapper>> {
        linker.instantiate_pre(component).map(Box::new)
    }

    /// Get component instance pre reference from pointer
    pub unsafe fn get_component_instance_pre_ref(
        pre_ptr: *const c_void,
    ) -> WasmtimeResult<&'static ComponentInstancePreWrapper> {
        validate_ptr_not_null!(pre_ptr, "component instance pre");
        Ok(&*(pre_ptr as *const ComponentInstancePreWrapper))
    }

    /// Destroy component instance pre
    pub unsafe fn destroy_component_instance_pre(pre_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<ComponentInstancePreWrapper>(
            pre_ptr,
            "ComponentInstancePreWrapper",
        );
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
            async_support: false,
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

/// Set whether to inherit arguments from host
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_inherit_args(
    linker_ptr: *mut c_void,
    inherit: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_inherit_args(inherit != 0);
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

/// Add a preopened directory with permission bits
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_add_wasi_preopen_dir(
    linker_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
    dir_perms_bits: c_int,
    file_perms_bits: c_int,
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

    linker.add_wasi_preopen_dir(
        host_str,
        guest_str,
        dir_perms_bits as u32,
        file_perms_bits as u32,
    );
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

/// Set whether to inherit stdin individually
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_inherit_stdin(
    linker_ptr: *mut c_void,
    inherit: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_inherit_stdin(inherit != 0);
    FFI_SUCCESS
}

/// Set whether to inherit stdout individually
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_inherit_stdout(
    linker_ptr: *mut c_void,
    inherit: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_inherit_stdout(inherit != 0);
    FFI_SUCCESS
}

/// Set whether to inherit stderr individually
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_inherit_stderr(
    linker_ptr: *mut c_void,
    inherit: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_inherit_stderr(inherit != 0);
    FFI_SUCCESS
}

/// Set whether TCP sockets are allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_tcp(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_tcp(allow != 0);
    FFI_SUCCESS
}

/// Set whether UDP sockets are allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_udp(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_udp(allow != 0);
    FFI_SUCCESS
}

/// Set whether IP name lookup is allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_ip_name_lookup(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_ip_name_lookup(allow != 0);
    FFI_SUCCESS
}

/// Set whether blocking the current thread is allowed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_allow_blocking_current_thread(
    linker_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_allow_blocking_current_thread(allow != 0);
    FFI_SUCCESS
}

/// Set insecure random seed
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_insecure_random_seed(
    linker_ptr: *mut c_void,
    seed: u64,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_insecure_random_seed(seed);
    FFI_SUCCESS
}

/// Set custom wall clock callback
///
/// The `now_fn` and `resolution_fn` are called with `callback_id` and pointers to
/// write (seconds: i64, nanoseconds: u32) results.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_wall_clock(
    linker_ptr: *mut c_void,
    now_fn: extern "C" fn(i64, *mut i64, *mut u32),
    resolution_fn: extern "C" fn(i64, *mut i64, *mut u32),
    callback_id: i64,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_wall_clock(CallbackWallClock {
        now_fn,
        resolution_fn,
        callback_id,
    });
    FFI_SUCCESS
}

/// Set custom monotonic clock callback
///
/// The `now_fn` returns nanoseconds as u64. The `resolution_fn` returns resolution in nanoseconds.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_monotonic_clock(
    linker_ptr: *mut c_void,
    now_fn: extern "C" fn(i64) -> u64,
    resolution_fn: extern "C" fn(i64) -> u64,
    callback_id: i64,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_monotonic_clock(CallbackMonotonicClock {
        now_fn,
        resolution_fn,
        callback_id,
    });
    FFI_SUCCESS
}

/// Set custom secure random callback
///
/// The `fill_bytes_fn` is called with (callback_id, buffer_ptr, buffer_len)
/// and must fill the buffer with cryptographically secure random bytes.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_secure_random(
    linker_ptr: *mut c_void,
    fill_bytes_fn: extern "C" fn(i64, *mut u8, usize),
    callback_id: i64,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_secure_random(CallbackRng {
        fill_bytes_fn,
        callback_id,
    });
    FFI_SUCCESS
}

/// Set custom insecure random callback
///
/// The `fill_bytes_fn` is called with (callback_id, buffer_ptr, buffer_len)
/// and must fill the buffer with random bytes (not necessarily secure).
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_insecure_random(
    linker_ptr: *mut c_void,
    fill_bytes_fn: extern "C" fn(i64, *mut u8, usize),
    callback_id: i64,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_insecure_random(CallbackRng {
        fill_bytes_fn,
        callback_id,
    });
    FFI_SUCCESS
}

/// Set socket address check callback
///
/// The `check_fn` is called with (callback_id, ip_version, ip_bytes_ptr, ip_bytes_len, port, use_type)
/// where ip_version is 4 or 6, use_type is 0-4 (TcpBind, TcpConnect, UdpBind, UdpConnect, UdpOutgoingDatagram).
/// Returns non-zero to allow the connection, zero to deny.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_set_wasi_socket_addr_check(
    linker_ptr: *mut c_void,
    check_fn: extern "C" fn(i64, i32, *const u8, usize, u16, i32) -> i32,
    callback_id: i64,
) -> c_int {
    if linker_ptr.is_null() {
        return FFI_ERROR;
    }

    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.set_wasi_socket_addr_check(CallbackSocketAddrCheck {
        check_fn,
        callback_id,
    });
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

/// Allow shadowing of previously defined names
///
/// # Safety
///
/// linker_ptr must be a valid mutable pointer to a ComponentLinker
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_allow_shadowing(
    linker_ptr: *mut c_void,
    allow: c_int,
) {
    if linker_ptr.is_null() {
        return;
    }
    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    linker.allow_shadowing(allow != 0);
}

/// Define all unknown imports of the given component as traps
///
/// # Safety
///
/// linker_ptr and component_ptr must be valid pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_define_unknown_imports_as_traps(
    linker_ptr: *mut c_void,
    component_ptr: *const c_void,
) -> c_int {
    if linker_ptr.is_null() || component_ptr.is_null() {
        return FFI_ERROR;
    }
    let linker = &mut *(linker_ptr as *mut ComponentLinker);
    let component = &*(component_ptr as *const Component);

    match linker.define_unknown_imports_as_traps(component) {
        Ok(()) => FFI_SUCCESS,
        Err(e) => {
            log::error!("Failed to define unknown imports as traps: {}", e);
            FFI_ERROR
        }
    }
}

// ============================================================================
// ComponentInstancePre FFI Functions
// ============================================================================

/// Pre-instantiate a component using the linker
///
/// # Safety
///
/// linker_ptr and component_ptr must be valid pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_instantiate_pre(
    linker_ptr: *const c_void,
    component_ptr: *const c_void,
) -> *mut c_void {
    if linker_ptr.is_null() || component_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let linker = &*(linker_ptr as *const ComponentLinker);
    let component = &*(component_ptr as *const Component);

    match linker.instantiate_pre(component) {
        Ok(wrapper) => Box::into_raw(Box::new(wrapper)) as *mut c_void,
        Err(e) => {
            log::error!("Failed to pre-instantiate component: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Instantiate from a ComponentInstancePre
///
/// # Safety
///
/// pre_ptr and instance_out must be valid pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_pre_instantiate(
    pre_ptr: *const c_void,
    instance_out: *mut *mut c_void,
) -> c_int {
    if pre_ptr.is_null() || instance_out.is_null() {
        return FFI_ERROR;
    }

    let wrapper = &*(pre_ptr as *const ComponentInstancePreWrapper);

    match wrapper.instantiate() {
        Ok(instance) => {
            *instance_out = Box::into_raw(Box::new(instance)) as *mut c_void;
            FFI_SUCCESS
        }
        Err(e) => {
            log::error!("Failed to instantiate from ComponentInstancePre: {}", e);
            FFI_ERROR
        }
    }
}

/// Check if ComponentInstancePre is valid
///
/// # Safety
///
/// pre_ptr must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_pre_is_valid(
    pre_ptr: *const c_void,
) -> c_int {
    if pre_ptr.is_null() {
        return 0;
    }
    let wrapper = &*(pre_ptr as *const ComponentInstancePreWrapper);
    if wrapper.is_valid() {
        1
    } else {
        0
    }
}

/// Get instance count from ComponentInstancePre
///
/// # Safety
///
/// pre_ptr must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_pre_instance_count(
    pre_ptr: *const c_void,
) -> u64 {
    if pre_ptr.is_null() {
        return 0;
    }
    let wrapper = &*(pre_ptr as *const ComponentInstancePreWrapper);
    wrapper.instance_count()
}

/// Get preparation time in nanoseconds from ComponentInstancePre
///
/// # Safety
///
/// pre_ptr must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_pre_preparation_time_ns(
    pre_ptr: *const c_void,
) -> u64 {
    if pre_ptr.is_null() {
        return 0;
    }
    let wrapper = &*(pre_ptr as *const ComponentInstancePreWrapper);
    wrapper.preparation_time_ns()
}

/// Get average instantiation time in nanoseconds from ComponentInstancePre
///
/// # Safety
///
/// pre_ptr must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_pre_avg_instantiation_time_ns(
    pre_ptr: *const c_void,
) -> u64 {
    if pre_ptr.is_null() {
        return 0;
    }
    let wrapper = &*(pre_ptr as *const ComponentInstancePreWrapper);
    wrapper.average_instantiation_time_ns()
}

/// Destroy a ComponentInstancePre
///
/// # Safety
///
/// pre_ptr must be valid and not used after this call
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_instance_pre_destroy(pre_ptr: *mut c_void) {
    if !pre_ptr.is_null() {
        let _ = Box::from_raw(pre_ptr as *mut ComponentInstancePreWrapper);
    }
}

// =============================================================================
// Resource Definition FFI
// =============================================================================

/// FFI destructor callback wrapper for Panama.
///
/// Stores a function pointer and callback ID. When the guest drops a resource,
/// the function pointer is called with the callback ID and the resource rep.
struct FfiResourceDestructorCallback {
    callback_fn: Option<unsafe extern "C" fn(u64, u32) -> c_int>,
    callback_id: u64,
}

// Safety: The callback function pointer is provided by the host (Java via Panama)
// and must be valid for the lifetime of the linker.
unsafe impl Send for FfiResourceDestructorCallback {}
unsafe impl Sync for FfiResourceDestructorCallback {}

impl ResourceDestructorCallback for FfiResourceDestructorCallback {
    fn destroy(&self, rep: u32) -> WasmtimeResult<()> {
        if let Some(callback_fn) = self.callback_fn {
            let result = unsafe { callback_fn(self.callback_id, rep) };
            if result != 0 {
                return Err(WasmtimeError::Runtime {
                    message: format!(
                        "Resource destructor callback failed with code {} (callback_id={}, rep={})",
                        result, self.callback_id, rep
                    ),
                    backtrace: None,
                });
            }
        }
        Ok(())
    }
}

/// Define a host resource type on the component linker (shared FFI).
///
/// # Arguments
/// * `linker_ptr` - Pointer to the ComponentLinker
/// * `interface_path_ptr` - UTF-8 interface path (e.g., "wasi:io/streams")
/// * `interface_path_len` - Length of the interface path
/// * `resource_name_ptr` - UTF-8 resource name
/// * `resource_name_len` - Length of the resource name
/// * `resource_id` - Unique resource ID for this definition
/// * `destructor_fn` - Function pointer for the destructor callback (nullable)
/// * `destructor_callback_id` - Callback ID passed to the destructor function
///
/// # Returns
/// 0 on success, non-zero error code on failure
///
/// # Safety
///
/// All pointers must be valid. String pointers must point to valid UTF-8 data.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_component_linker_define_resource(
    linker_ptr: *mut c_void,
    interface_path_ptr: *const u8,
    interface_path_len: usize,
    resource_name_ptr: *const u8,
    resource_name_len: usize,
    resource_id: u32,
    destructor_fn: Option<unsafe extern "C" fn(u64, u32) -> c_int>,
    destructor_callback_id: u64,
) -> c_int {
    crate::error::ffi_utils::ffi_try_code(|| {
        if linker_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Linker pointer is null".to_string(),
            });
        }

        let interface_path = if interface_path_ptr.is_null() || interface_path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Interface path is null or empty".to_string(),
            });
        } else {
            let bytes = std::slice::from_raw_parts(interface_path_ptr, interface_path_len);
            std::str::from_utf8(bytes).map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Interface path is not valid UTF-8: {}", e),
            })?
        };

        let resource_name = if resource_name_ptr.is_null() || resource_name_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Resource name is null or empty".to_string(),
            });
        } else {
            let bytes = std::slice::from_raw_parts(resource_name_ptr, resource_name_len);
            std::str::from_utf8(bytes).map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Resource name is not valid UTF-8: {}", e),
            })?
        };

        let linker = &mut *(linker_ptr as *mut ComponentLinker);

        let destructor = Arc::new(FfiResourceDestructorCallback {
            callback_fn: destructor_fn,
            callback_id: destructor_callback_id,
        });

        linker.define_resource(interface_path, resource_name, resource_id, destructor)
    })
}
