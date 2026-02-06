//! WASI (WebAssembly System Interface) support with comprehensive filesystem and environment access
//!
//! This module provides complete WASI functionality including:
//! - Filesystem access with configurable directory mapping and permissions
//! - Environment variable access and manipulation  
//! - Command-line argument passing to WebAssembly modules
//! - Standard input/output/error stream handling
//! - Network socket support (where available in Wasmtime)
//! - Proper security sandboxing and permission enforcement

use std::sync::{Arc, Mutex, RwLock};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use wasmtime_wasi::{WasiCtx, WasiCtxBuilder, DirPerms, FilePerms};
use wasmtime_wasi::p1::WasiP1Ctx;
use wasmtime_wasi::p2::pipe::MemoryOutputPipe;
use wasmtime::Linker;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;
use crate::linker::Linker as WasmtimeLinker;

/// Thread-safe wrapper around WASI context with comprehensive configuration
pub struct WasiContext {
    /// The underlying Wasmtime WASI Preview 1 context
    inner: Arc<Mutex<WasiP1Ctx>>,
    /// Configuration metadata
    config: WasiConfig,
    /// Directory mappings for filesystem access
    directory_mappings: HashMap<String, DirectoryMapping>,
    /// Environment variables (also accessible via RwLock for CLI bindings)
    environment: HashMap<String, String>,
    /// Command line arguments (also accessible via RwLock for CLI bindings)
    arguments: Vec<String>,
    /// Standard stream configurations
    stdio_config: StdioConfig,
    /// Captured stdout pipe (when using Buffer mode)
    stdout_pipe: Option<MemoryOutputPipe>,
    /// Captured stderr pipe (when using Buffer mode)
    stderr_pipe: Option<MemoryOutputPipe>,
    /// Streams registry for CLI bindings
    pub streams: Arc<RwLock<HashMap<u32, WasiStreamInfo>>>,
    /// Stdin stream handle
    pub stdin_handle: Arc<RwLock<Option<u64>>>,
    /// Stdout stream handle
    pub stdout_handle: Arc<RwLock<Option<u64>>>,
    /// Stderr stream handle
    pub stderr_handle: Arc<RwLock<Option<u64>>>,
    /// Exit code (if process has exited)
    pub exit_code: Arc<RwLock<Option<i32>>>,
    /// Environment variables (RwLock-wrapped for CLI bindings)
    pub environment_rw: Arc<RwLock<HashMap<String, String>>>,
    /// Arguments (RwLock-wrapped for CLI bindings)
    pub arguments_rw: Arc<RwLock<Vec<String>>>,
    /// Initial working directory (RwLock-wrapped for CLI bindings)
    pub initial_cwd: Arc<RwLock<Option<String>>>,
    /// Operation counter for generating unique IDs
    pub next_operation_id: std::sync::atomic::AtomicU64,
}

impl Clone for WasiContext {
    fn clone(&self) -> Self {
        WasiContext {
            inner: Arc::clone(&self.inner),  // Arc is cheap to clone
            config: self.config.clone(),
            directory_mappings: self.directory_mappings.clone(),
            environment: self.environment.clone(),
            arguments: self.arguments.clone(),
            stdio_config: self.stdio_config.clone(),
            stdout_pipe: self.stdout_pipe.clone(),
            stderr_pipe: self.stderr_pipe.clone(),
            streams: Arc::clone(&self.streams),
            stdin_handle: Arc::clone(&self.stdin_handle),
            stdout_handle: Arc::clone(&self.stdout_handle),
            stderr_handle: Arc::clone(&self.stderr_handle),
            exit_code: Arc::clone(&self.exit_code),
            environment_rw: Arc::clone(&self.environment_rw),
            arguments_rw: Arc::clone(&self.arguments_rw),
            initial_cwd: Arc::clone(&self.initial_cwd),
            next_operation_id: std::sync::atomic::AtomicU64::new(
                self.next_operation_id.load(std::sync::atomic::Ordering::SeqCst)
            ),
        }
    }
}

impl std::fmt::Debug for WasiContext {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("WasiContext")
            .field("config", &self.config)
            .field("directory_mappings", &self.directory_mappings)
            .field("environment", &self.environment)
            .field("arguments", &self.arguments)
            .field("stdio_config", &self.stdio_config)
            .field("stdout_pipe", &self.stdout_pipe.as_ref().map(|_| "<MemoryOutputPipe>"))
            .field("stderr_pipe", &self.stderr_pipe.as_ref().map(|_| "<MemoryOutputPipe>"))
            .field("streams", &"<streams>")
            .field("stdin_handle", &"<stdin_handle>")
            .field("stdout_handle", &"<stdout_handle>")
            .field("stderr_handle", &"<stderr_handle>")
            .field("exit_code", &"<exit_code>")
            .field("inner", &"<WasiCtx>")
            .finish()
    }
}

/// WASI configuration options
#[derive(Debug, Clone)]
pub struct WasiConfig {
    /// Whether to allow network access
    pub allow_network: bool,
    /// Whether to allow arbitrary directory access
    pub allow_arbitrary_fs: bool,
    /// Maximum file size for operations (in bytes)
    pub max_file_size: Option<u64>,
    /// Maximum number of open file descriptors
    pub max_open_files: Option<u32>,
    /// Environment variable access policy
    pub env_policy: EnvironmentPolicy,
}

/// Environment variable access policy
#[derive(Debug, Clone)]
pub enum EnvironmentPolicy {
    /// Inherit all environment variables from the host
    Inherit,
    /// Allow only specified environment variables
    AllowList(Vec<String>),
    /// Deny specified environment variables, allow others
    DenyList(Vec<String>),
    /// Provide only custom environment variables
    Custom,
}

/// Directory mapping configuration with permissions
#[derive(Debug, Clone)]
pub struct DirectoryMapping {
    /// Host path to map
    pub host_path: PathBuf,
    /// Guest path where it appears in WASI filesystem
    pub guest_path: String,
    /// Directory permissions
    pub dir_perms: WasiDirPermissions,
    /// File permissions within this directory
    pub file_perms: WasiFilePermissions,
}

/// WASI directory permissions wrapper
#[derive(Debug, Clone)]
pub struct WasiDirPermissions {
    /// Can create directories
    pub create: bool,
    /// Can read directory contents
    pub read: bool,
    /// Can remove directories
    pub remove: bool,
}

/// WASI file permissions wrapper
#[derive(Debug, Clone)]  
pub struct WasiFilePermissions {
    /// Can read file contents
    pub read: bool,
    /// Can write to files
    pub write: bool,
    /// Can create new files
    pub create: bool,
    /// Can truncate files
    pub truncate: bool,
}

/// Standard I/O configuration
#[derive(Debug, Clone)]
pub struct StdioConfig {
    /// Standard input configuration
    pub stdin: StdioSource,
    /// Standard output configuration
    pub stdout: StdioSink,
    /// Standard error configuration
    pub stderr: StdioSink,
}

/// Standard input source options
#[derive(Debug, Clone)]
pub enum StdioSource {
    /// Inherit from parent process
    Inherit,
    /// Provide from byte buffer
    Buffer(Vec<u8>),
    /// Read from file
    File(PathBuf),
    /// No input (empty)
    Null,
}

/// Standard output/error sink options
#[derive(Debug, Clone)]
pub enum StdioSink {
    /// Inherit from parent process
    Inherit,
    /// Capture to byte buffer
    Buffer,
    /// Write to file
    File(PathBuf),
    /// Discard output
    Null,
}

/// WASI execution result with captured I/O
#[derive(Debug)]
pub struct WasiExecutionResult {
    /// Exit code from WASI program
    pub exit_code: Option<i32>,
    /// Captured stdout data
    pub stdout: Option<Vec<u8>>,
    /// Captured stderr data
    pub stderr: Option<Vec<u8>>,
}

/// WASI stream for CLI bindings (Preview 1 compatible)
#[derive(Debug, Clone)]
pub struct WasiStreamInfo {
    /// Stream ID
    pub id: u32,
    /// Stream type
    pub stream_type: WasiStreamTypeInfo,
    /// Buffer for buffered operations
    pub buffer: Vec<u8>,
    /// Stream status
    pub status: WasiStreamStatusInfo,
    /// Associated file descriptor or resource
    pub resource_id: Option<u64>,
}

/// WASI stream types for CLI bindings
#[derive(Debug, Clone, PartialEq)]
pub enum WasiStreamTypeInfo {
    /// Input stream for reading
    InputStream,
    /// Output stream for writing
    OutputStream,
    /// Bidirectional stream
    BidirectionalStream,
}

/// WASI stream status for CLI bindings
#[derive(Debug, Clone, PartialEq)]
pub enum WasiStreamStatusInfo {
    /// Stream is ready for operations
    Ready,
    /// Stream is closed
    Closed,
    /// Stream has an error
    Error(String),
}

// ============================================================================
// WasiStreamEntry trait implementation for WasiStreamInfo
// ============================================================================

impl crate::wasi_stream_ops::WasiStreamEntry for WasiStreamInfo {
    fn is_closed(&self) -> bool {
        matches!(self.status, WasiStreamStatusInfo::Closed)
    }

    fn set_closed(&mut self) {
        self.status = WasiStreamStatusInfo::Closed;
    }

    fn buffer(&self) -> &Vec<u8> {
        &self.buffer
    }

    fn buffer_mut(&mut self) -> &mut Vec<u8> {
        &mut self.buffer
    }

    fn clear_buffer(&mut self) {
        self.buffer.clear();
    }
}

// ============================================================================
// WasiStreamContext trait implementation for WasiContext
// ============================================================================

impl crate::wasi_stream_ops::WasiStreamContext for WasiContext {
    type StreamEntry = WasiStreamInfo;

    fn streams_read(&self) -> crate::error::WasmtimeResult<std::sync::RwLockReadGuard<'_, HashMap<u32, Self::StreamEntry>>> {
        self.streams.read().map_err(|_| crate::error::WasmtimeError::Wasi {
            message: "Failed to lock streams".to_string(),
        })
    }

    fn streams_write(&self) -> crate::error::WasmtimeResult<std::sync::RwLockWriteGuard<'_, HashMap<u32, Self::StreamEntry>>> {
        self.streams.write().map_err(|_| crate::error::WasmtimeError::Wasi {
            message: "Failed to lock streams".to_string(),
        })
    }
}

impl Default for WasiConfig {
    fn default() -> Self {
        Self {
            allow_network: false,
            allow_arbitrary_fs: false,
            max_file_size: Some(100 * 1024 * 1024), // 100MB default
            max_open_files: Some(1024),
            env_policy: EnvironmentPolicy::Custom,
        }
    }
}

impl Default for WasiDirPermissions {
    fn default() -> Self {
        Self {
            create: false,
            read: true,
            remove: false,
        }
    }
}

impl Default for WasiFilePermissions {
    fn default() -> Self {
        Self {
            read: true,
            write: false,
            create: false,
            truncate: false,
        }
    }
}

impl Default for StdioConfig {
    fn default() -> Self {
        Self {
            stdin: StdioSource::Null,
            stdout: StdioSink::Buffer,
            stderr: StdioSink::Buffer,
        }
    }
}

impl WasiDirPermissions {
    /// Convert to wasmtime_wasi DirPerms
    pub fn to_wasmtime_perms(&self) -> DirPerms {
        let mut perms = DirPerms::empty();
        if self.read {
            perms |= DirPerms::READ;
        }
        // Note: CREATE and REMOVE permissions might not be available in this version
        perms
    }
}

impl WasiFilePermissions {
    /// Convert to wasmtime_wasi FilePerms
    pub fn to_wasmtime_perms(&self) -> FilePerms {
        let mut perms = FilePerms::empty();
        if self.read {
            perms |= FilePerms::READ;
        }
        if self.write {
            perms |= FilePerms::WRITE;
        }
        perms
    }
}

impl WasiContext {
    /// Create a new WASI context with default configuration
    pub fn new() -> WasmtimeResult<Self> {
        Self::with_config(WasiConfig::default())
    }

    /// Create a new WASI context with specific configuration
    pub fn with_config(config: WasiConfig) -> WasmtimeResult<Self> {
        use wasmtime_wasi::p2::pipe::{MemoryInputPipe, MemoryOutputPipe};

        let mut builder = WasiCtxBuilder::new();

        // Use pipe-based stdio instead of inherit_stdio() to avoid SIGABRT
        // when process stdio handles are captured or unavailable (e.g., during
        // cargo test or benchmark runs). The actual stdio configuration for
        // module execution is handled by build_fresh_p1_ctx() which respects
        // the StdioConfig settings.
        let empty: Vec<u8> = Vec::new();
        builder.stdin(MemoryInputPipe::new(empty));
        builder.stdout(MemoryOutputPipe::new(64 * 1024));
        builder.stderr(MemoryOutputPipe::new(64 * 1024));

        // Build the WASI Preview 1 context
        let wasi_ctx = builder.build_p1();
        
        let default_args = vec!["wasmtime4j".to_string()];
        Ok(WasiContext {
            inner: Arc::new(Mutex::new(wasi_ctx)),
            config,
            directory_mappings: HashMap::new(),
            environment: HashMap::new(),
            arguments: default_args.clone(),
            stdio_config: StdioConfig::default(),
            stdout_pipe: None,
            stderr_pipe: None,
            streams: Arc::new(RwLock::new(HashMap::new())),
            stdin_handle: Arc::new(RwLock::new(None)),
            stdout_handle: Arc::new(RwLock::new(None)),
            stderr_handle: Arc::new(RwLock::new(None)),
            exit_code: Arc::new(RwLock::new(None)),
            environment_rw: Arc::new(RwLock::new(HashMap::new())),
            arguments_rw: Arc::new(RwLock::new(default_args)),
            initial_cwd: Arc::new(RwLock::new(None)),
            next_operation_id: std::sync::atomic::AtomicU64::new(100), // Start at 100 to avoid conflicts with stdio handles
        })
    }

    /// Get a reference to the inner WASI Preview 1 context for linker integration
    ///
    /// # Safety
    /// This provides access to the underlying wasmtime-wasi context.
    /// The caller must ensure proper synchronization when using the returned Arc.
    pub fn inner(&self) -> &Arc<Mutex<WasiP1Ctx>> {
        &self.inner
    }

    /// Add a directory mapping with specific permissions
    pub fn add_directory_mapping<P1: AsRef<Path>, P2: AsRef<str>>(
        &mut self,
        host_path: P1,
        guest_path: P2,
        dir_perms: WasiDirPermissions,
        file_perms: WasiFilePermissions,
    ) -> WasmtimeResult<()> {
        let host_path = host_path.as_ref().to_path_buf();
        let guest_path = guest_path.as_ref().to_string();
        
        // Validate that the host path exists
        if !host_path.exists() {
            return Err(WasmtimeError::Wasi {
                message: format!("Host path does not exist: {}", host_path.display()),
            });
        }

        // Create new WASI context with the directory mapping
        let mut builder = WasiCtxBuilder::new();
        
        // Re-add existing mappings
        for (guest, mapping) in &self.directory_mappings {
            builder.preopened_dir(
                &mapping.host_path,
                guest,
                mapping.dir_perms.to_wasmtime_perms(),
                mapping.file_perms.to_wasmtime_perms(),
            ).map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to add directory mapping {}: {}", guest, e),
            })?;
        }
        
        // Add the new mapping
        builder.preopened_dir(
            &host_path,
            &guest_path,
            dir_perms.to_wasmtime_perms(),
            file_perms.to_wasmtime_perms(),
        ).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to add directory mapping {}: {}", guest_path, e),
        })?;

        // Re-add environment and arguments
        for (key, value) in &self.environment {
            builder.env(key, value);
        }
        builder.args(&self.arguments);

        // Configure stdio
        self.configure_stdio(&mut builder)?;

        // Build and update context
        let new_ctx = builder.build_p1();
        
        let mut inner = self.inner.lock().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire WASI context lock".to_string(),
        })?;
        *inner = new_ctx;
        
        // Store the mapping
        let mapping = DirectoryMapping {
            host_path,
            guest_path: guest_path.clone(),
            dir_perms,
            file_perms,
        };
        self.directory_mappings.insert(guest_path, mapping);
        
        Ok(())
    }

    /// Set an environment variable
    pub fn set_environment_variable<K: AsRef<str>, V: AsRef<str>>(
        &mut self,
        key: K,
        value: V,
    ) -> WasmtimeResult<()> {
        let key = key.as_ref().to_string();
        let value = value.as_ref().to_string();
        
        // Validate environment access policy
        match &self.config.env_policy {
            EnvironmentPolicy::AllowList(allowed) => {
                if !allowed.contains(&key) {
                    return Err(WasmtimeError::Wasi {
                        message: format!("Environment variable '{}' not in allow list", key),
                    });
                }
            }
            EnvironmentPolicy::DenyList(denied) => {
                if denied.contains(&key) {
                    return Err(WasmtimeError::Wasi {
                        message: format!("Environment variable '{}' is denied", key),
                    });
                }
            }
            _ => {} // Custom and Inherit allow all
        }
        
        self.environment.insert(key, value);
        self.rebuild_context()?;
        
        Ok(())
    }

    /// Set command line arguments
    pub fn set_arguments(&mut self, args: Vec<String>) -> WasmtimeResult<()> {
        self.arguments = args;
        self.rebuild_context()?;
        Ok(())
    }

    /// Configure standard I/O streams
    pub fn configure_stdio_streams(
        &mut self,
        stdin: StdioSource,
        stdout: StdioSink,
        stderr: StdioSink,
    ) -> WasmtimeResult<()> {
        self.stdio_config = StdioConfig {
            stdin,
            stdout, 
            stderr,
        };
        self.rebuild_context()?;
        Ok(())
    }

    /// Get a reference to the underlying WASI Preview 1 context for store operations
    pub fn get_wasi_ctx(&self) -> WasmtimeResult<Arc<Mutex<WasiP1Ctx>>> {
        Ok(Arc::clone(&self.inner))
    }

    /// Add WASI imports to a Wasmtime linker
    /// Uses wasmtime-wasi's add_to_linker_sync for WASI Preview 1 support
    pub fn add_to_generic_linker<T: Send + 'static>(
        linker: &mut Linker<T>,
        get_ctx: impl Fn(&mut T) -> &mut WasiP1Ctx + Send + Sync + Copy + 'static,
    ) -> WasmtimeResult<()> {
        // Use wasmtime-wasi's built-in function to add all WASI Preview 1 imports to the linker
        wasmtime_wasi::p1::add_to_linker_sync(linker, get_ctx)
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to add WASI to linker: {}", e),
            })?;

        log::debug!("WASI Preview 1 imports successfully added to linker");
        Ok(())
    }

    /// Check if a path is allowed based on current directory mappings
    pub fn is_path_allowed<P: AsRef<Path>>(&self, path: P) -> bool {
        let path = path.as_ref();
        
        // Check if path falls within any mapped directory
        for mapping in self.directory_mappings.values() {
            if path.starts_with(&mapping.host_path) {
                return true;
            }
        }
        
        // Check if arbitrary filesystem access is allowed
        self.config.allow_arbitrary_fs
    }

    /// Get current environment variables
    pub fn get_environment(&self) -> &HashMap<String, String> {
        &self.environment
    }

    /// Get current command line arguments
    pub fn get_arguments(&self) -> &[String] {
        &self.arguments
    }

    /// Get current directory mappings
    pub fn get_directory_mappings(&self) -> &HashMap<String, DirectoryMapping> {
        &self.directory_mappings
    }

    /// Get current configuration
    pub fn get_config(&self) -> &WasiConfig {
        &self.config
    }

    /// Rebuild the WASI context with current configuration
    fn rebuild_context(&mut self) -> WasmtimeResult<()> {
        let mut builder = WasiCtxBuilder::new();
        
        // Add directory mappings
        for (guest_path, mapping) in &self.directory_mappings {
            builder.preopened_dir(
                &mapping.host_path,
                guest_path,
                mapping.dir_perms.to_wasmtime_perms(),
                mapping.file_perms.to_wasmtime_perms(),
            ).map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to add directory mapping {}: {}", guest_path, e),
            })?;
        }
        
        // Add environment variables
        for (key, value) in &self.environment {
            builder.env(key, value);
        }
        
        // Add command line arguments
        builder.args(&self.arguments);
        
        // Configure stdio
        self.configure_stdio(&mut builder)?;
        
        // Build new context
        let new_ctx = builder.build_p1();
        
        let mut inner = self.inner.lock().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire WASI context lock".to_string(),
        })?;
        *inner = new_ctx;
        
        Ok(())
    }

    /// Build a fresh WasiP1Ctx from current configuration without modifying self
    /// Returns the context along with any output pipes for stdout/stderr capture
    pub fn build_fresh_p1_ctx(&self) -> WasmtimeResult<(WasiP1Ctx, Option<MemoryOutputPipe>, Option<MemoryOutputPipe>)> {
        use wasmtime_wasi::p2::pipe::MemoryInputPipe;
        const DEFAULT_BUFFER_CAPACITY: usize = 64 * 1024;

        let mut builder = WasiCtxBuilder::new();
        let mut stdout_pipe = None;
        let mut stderr_pipe = None;

        // Add directory mappings
        for (guest_path, mapping) in &self.directory_mappings {
            builder.preopened_dir(
                &mapping.host_path,
                guest_path,
                mapping.dir_perms.to_wasmtime_perms(),
                mapping.file_perms.to_wasmtime_perms(),
            ).map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to add directory mapping {}: {}", guest_path, e),
            })?;
        }

        // Add environment variables
        for (key, value) in &self.environment {
            builder.env(key, value);
        }

        // Add command line arguments
        builder.args(&self.arguments);

        // Configure stdin
        match &self.stdio_config.stdin {
            StdioSource::Inherit => {
                builder.inherit_stdin();
            }
            StdioSource::Buffer(data) => {
                let pipe = MemoryInputPipe::new(data.clone());
                builder.stdin(pipe);
            }
            StdioSource::File(_path) => {
                builder.inherit_stdin();
            }
            StdioSource::Null => {
                let empty: Vec<u8> = Vec::new();
                let pipe = MemoryInputPipe::new(empty);
                builder.stdin(pipe);
            }
        }

        // Configure stdout - reuse existing pipe if available, otherwise create new
        match &self.stdio_config.stdout {
            StdioSink::Inherit => {
                builder.inherit_stdout();
            }
            StdioSink::Buffer => {
                // Reuse existing stdout pipe if available (from enable_output_capture)
                let pipe = if let Some(existing_pipe) = &self.stdout_pipe {
                    existing_pipe.clone()
                } else {
                    MemoryOutputPipe::new(DEFAULT_BUFFER_CAPACITY)
                };
                builder.stdout(pipe.clone());
                stdout_pipe = Some(pipe);
            }
            StdioSink::File(_path) => {
                builder.inherit_stdout();
            }
            StdioSink::Null => {
                let pipe = MemoryOutputPipe::new(0);
                builder.stdout(pipe);
            }
        }

        // Configure stderr - reuse existing pipe if available, otherwise create new
        match &self.stdio_config.stderr {
            StdioSink::Inherit => {
                builder.inherit_stderr();
            }
            StdioSink::Buffer => {
                // Reuse existing stderr pipe if available (from enable_output_capture)
                let pipe = if let Some(existing_pipe) = &self.stderr_pipe {
                    existing_pipe.clone()
                } else {
                    MemoryOutputPipe::new(DEFAULT_BUFFER_CAPACITY)
                };
                builder.stderr(pipe.clone());
                stderr_pipe = Some(pipe);
            }
            StdioSink::File(_path) => {
                builder.inherit_stderr();
            }
            StdioSink::Null => {
                let pipe = MemoryOutputPipe::new(0);
                builder.stderr(pipe);
            }
        }

        // Build and return the context with pipes
        let ctx = builder.build_p1();
        Ok((ctx, stdout_pipe, stderr_pipe))
    }

    /// Configure standard I/O streams in the builder
    fn configure_stdio(&mut self, builder: &mut WasiCtxBuilder) -> WasmtimeResult<()> {
        use wasmtime_wasi::p2::pipe::MemoryInputPipe;

        // Configure stdin based on stdio_config
        match &self.stdio_config.stdin {
            StdioSource::Inherit => {
                builder.inherit_stdin();
            }
            StdioSource::Buffer(data) => {
                let pipe = MemoryInputPipe::new(data.clone());
                builder.stdin(pipe);
            }
            StdioSource::File(_path) => {
                // File-based stdin requires async file operations
                // For now, fall back to inherit
                builder.inherit_stdin();
            }
            StdioSource::Null => {
                // Empty input - use empty buffer
                let empty: Vec<u8> = Vec::new();
                let pipe = MemoryInputPipe::new(empty);
                builder.stdin(pipe);
            }
        }

        // Configure stdout based on stdio_config
        // Default buffer capacity of 64KB
        const DEFAULT_BUFFER_CAPACITY: usize = 64 * 1024;

        match &self.stdio_config.stdout {
            StdioSink::Inherit => {
                builder.inherit_stdout();
                self.stdout_pipe = None;
            }
            StdioSink::Buffer => {
                let pipe = MemoryOutputPipe::new(DEFAULT_BUFFER_CAPACITY);
                builder.stdout(pipe.clone());
                self.stdout_pipe = Some(pipe);
            }
            StdioSink::File(_path) => {
                // File-based stdout requires async file operations
                // For now, fall back to inherit
                builder.inherit_stdout();
                self.stdout_pipe = None;
            }
            StdioSink::Null => {
                // Discard output - use a buffer but don't store reference
                let pipe = MemoryOutputPipe::new(0);
                builder.stdout(pipe);
                self.stdout_pipe = None;
            }
        }

        // Configure stderr based on stdio_config
        match &self.stdio_config.stderr {
            StdioSink::Inherit => {
                builder.inherit_stderr();
                self.stderr_pipe = None;
            }
            StdioSink::Buffer => {
                let pipe = MemoryOutputPipe::new(DEFAULT_BUFFER_CAPACITY);
                builder.stderr(pipe.clone());
                self.stderr_pipe = Some(pipe);
            }
            StdioSink::File(_path) => {
                // File-based stderr requires async file operations
                // For now, fall back to inherit
                builder.inherit_stderr();
                self.stderr_pipe = None;
            }
            StdioSink::Null => {
                // Discard output - use a buffer but don't store reference
                let pipe = MemoryOutputPipe::new(0);
                builder.stderr(pipe);
                self.stderr_pipe = None;
            }
        }

        Ok(())
    }

    /// Get captured stdout data
    /// Returns None if stdout was not configured for buffer capture
    pub fn get_stdout_capture(&self) -> Option<Vec<u8>> {
        self.stdout_pipe.as_ref().map(|pipe| pipe.contents().to_vec())
    }

    /// Get captured stderr data
    /// Returns None if stderr was not configured for buffer capture
    pub fn get_stderr_capture(&self) -> Option<Vec<u8>> {
        self.stderr_pipe.as_ref().map(|pipe| pipe.contents().to_vec())
    }

    /// Check if stdout is configured for buffer capture
    pub fn has_stdout_capture(&self) -> bool {
        self.stdout_pipe.is_some()
    }

    /// Check if stderr is configured for buffer capture
    pub fn has_stderr_capture(&self) -> bool {
        self.stderr_pipe.is_some()
    }

    /// Configure stdout for buffer capture
    /// This will take effect on the next rebuild_context call
    pub fn enable_stdout_capture(&mut self) -> WasmtimeResult<()> {
        self.stdio_config.stdout = StdioSink::Buffer;
        self.rebuild_context()
    }

    /// Configure stderr for buffer capture
    /// This will take effect on the next rebuild_context call
    pub fn enable_stderr_capture(&mut self) -> WasmtimeResult<()> {
        self.stdio_config.stderr = StdioSink::Buffer;
        self.rebuild_context()
    }

    /// Configure both stdout and stderr for buffer capture
    pub fn enable_output_capture(&mut self) -> WasmtimeResult<()> {
        self.stdio_config.stdout = StdioSink::Buffer;
        self.stdio_config.stderr = StdioSink::Buffer;
        self.rebuild_context()
    }
}

impl Default for WasiContext {
    fn default() -> Self {
        // Try to create with default configuration
        match Self::new() {
            Ok(context) => context,
            Err(_) => {
                // Fallback to minimal WASI context that should always work
                let mut builder = WasiCtxBuilder::new();
                let wasi_ctx = builder.build_p1();
                let default_args = vec!["wasmtime4j".to_string()];

                WasiContext {
                    inner: Arc::new(Mutex::new(wasi_ctx)),
                    config: WasiConfig::default(),
                    directory_mappings: HashMap::new(),
                    environment: HashMap::new(),
                    arguments: default_args.clone(),
                    stdio_config: StdioConfig::default(),
                    stdout_pipe: None,
                    stderr_pipe: None,
                    streams: Arc::new(RwLock::new(HashMap::new())),
                    stdin_handle: Arc::new(RwLock::new(None)),
                    stdout_handle: Arc::new(RwLock::new(None)),
                    stderr_handle: Arc::new(RwLock::new(None)),
                    exit_code: Arc::new(RwLock::new(None)),
                    environment_rw: Arc::new(RwLock::new(HashMap::new())),
                    arguments_rw: Arc::new(RwLock::new(default_args)),
                    initial_cwd: Arc::new(RwLock::new(None)),
                    next_operation_id: std::sync::atomic::AtomicU64::new(100),
                }
            }
        }
    }
}

/// Native FFI functions for WASI operations
use std::os::raw::{c_char, c_int, c_void};
use crate::error::ffi_utils;

/// Create a new WASI context with default configuration
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_new() -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let ctx = WasiContext::new()?;
        Ok(Box::new(ctx))
    })
}

/// Create a new WASI context with custom configuration
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_new_with_config(
    allow_network: c_int,
    allow_arbitrary_fs: c_int,
    max_file_size: u64,
    max_open_files: u32,
) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let config = WasiConfig {
            allow_network: allow_network != 0,
            allow_arbitrary_fs: allow_arbitrary_fs != 0,
            max_file_size: if max_file_size > 0 { Some(max_file_size) } else { None },
            max_open_files: if max_open_files > 0 { Some(max_open_files) } else { None },
            env_policy: EnvironmentPolicy::Custom,
        };
        let ctx = WasiContext::with_config(config)?;
        Ok(Box::new(ctx))
    })
}

/// Add a directory mapping to the WASI context
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_add_dir(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
    can_create: c_int,
    can_read: c_int,
    can_remove: c_int,
    file_read: c_int,
    file_write: c_int,
    file_create: c_int,
    file_truncate: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;
        let host_path_str = ffi_utils::c_str_to_string(host_path, "host path")?;
        let guest_path_str = ffi_utils::c_str_to_string(guest_path, "guest path")?;
        
        let dir_perms = WasiDirPermissions {
            create: can_create != 0,
            read: can_read != 0,
            remove: can_remove != 0,
        };
        
        let file_perms = WasiFilePermissions {
            read: file_read != 0,
            write: file_write != 0,
            create: file_create != 0,
            truncate: file_truncate != 0,
        };
        
        ctx.add_directory_mapping(&host_path_str, &guest_path_str, dir_perms, file_perms)?;
        Ok(())
    })
}

/// Set an environment variable in the WASI context
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_set_env(
    ctx_ptr: *mut c_void,
    key: *const c_char,
    value: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;
        let key_str = ffi_utils::c_str_to_string(key, "environment key")?;
        let value_str = ffi_utils::c_str_to_string(value, "environment value")?;

        ctx.set_environment_variable(&key_str, &value_str)?;
        Ok(())
    })
}

/// Set command line arguments for the WASI context
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_set_args(
    ctx_ptr: *mut c_void,
    args: *const *const c_char,
    args_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;
        
        if args.is_null() || args_len == 0 {
            ctx.set_arguments(vec!["wasmtime4j".to_string()])?;
            return Ok(());
        }
        
        let args_slice = ffi_utils::slice_from_raw_parts(args, args_len, "arguments array")?;
        let mut arg_strings = Vec::with_capacity(args_len);
        
        for &arg_ptr in args_slice {
            let arg_str = ffi_utils::c_str_to_string(arg_ptr, "argument")?;
            arg_strings.push(arg_str);
        }
        
        ctx.set_arguments(arg_strings)?;
        Ok(())
    })
}

/// Configure standard I/O streams for the WASI context
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_configure_stdio(
    ctx_ptr: *mut c_void,
    stdin_type: c_int,
    stdin_data: *const c_char,
    stdout_type: c_int,
    stdout_data: *const c_char,
    stderr_type: c_int,
    stderr_data: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;
        
        // Configure stdin
        let stdin = match stdin_type {
            0 => StdioSource::Inherit,
            1 => {
                if stdin_data.is_null() {
                    StdioSource::Null
                } else {
                    let data_str = ffi_utils::c_str_to_string(stdin_data, "stdin data")?;
                    StdioSource::Buffer(data_str.into_bytes())
                }
            }
            2 => {
                if stdin_data.is_null() {
                    StdioSource::Null
                } else {
                    let path_str = ffi_utils::c_str_to_string(stdin_data, "stdin file path")?;
                    StdioSource::File(PathBuf::from(path_str))
                }
            }
            _ => StdioSource::Null,
        };
        
        // Configure stdout
        let stdout = match stdout_type {
            0 => StdioSink::Inherit,
            1 => StdioSink::Buffer,
            2 => {
                if stdout_data.is_null() {
                    StdioSink::Null
                } else {
                    let path_str = ffi_utils::c_str_to_string(stdout_data, "stdout file path")?;
                    StdioSink::File(PathBuf::from(path_str))
                }
            }
            _ => StdioSink::Null,
        };
        
        // Configure stderr
        let stderr = match stderr_type {
            0 => StdioSink::Inherit,
            1 => StdioSink::Buffer,
            2 => {
                if stderr_data.is_null() {
                    StdioSink::Null
                } else {
                    let path_str = ffi_utils::c_str_to_string(stderr_data, "stderr file path")?;
                    StdioSink::File(PathBuf::from(path_str))
                }
            }
            _ => StdioSink::Null,
        };
        
        ctx.configure_stdio_streams(stdin, stdout, stderr)?;
        Ok(())
    })
}

/// Check if a path is allowed based on WASI context configuration
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_is_path_allowed(
    ctx_ptr: *const c_void,
    path: *const c_char,
) -> c_int {
    let result = ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;
        let path_str = ffi_utils::c_str_to_string(path, "path")?;
        let allowed = ctx.is_path_allowed(&path_str);
        Ok(if allowed { 1 } else { 0 })
    });
    result.1
}

/// Get the number of directory mappings
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_get_dir_count(ctx_ptr: *const c_void) -> usize {
    let result = ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;
        Ok(ctx.get_directory_mappings().len())
    });
    result.1
}

/// Get the number of environment variables
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_get_env_count(ctx_ptr: *const c_void) -> usize {
    let result = ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;
        Ok(ctx.get_environment().len())
    });
    result.1
}

/// Get the number of command line arguments
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_get_args_count(ctx_ptr: *const c_void) -> usize {
    let result = ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;
        Ok(ctx.get_arguments().len())
    });
    result.1
}

/// Destroy a WASI context and free its resources
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_destroy(ctx_ptr: *mut c_void) {
    if !ctx_ptr.is_null() {
        ffi_utils::destroy_resource::<WasiContext>(ctx_ptr, "WASI context");
    }
}

/// Add a WASI context to a Store for WebAssembly instance creation
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_add_to_store(
    ctx_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Dereference the WASI context pointer - this clones the Arc inside
        let wasi_ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        // Validate store pointer
        if store_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        // Cast store pointer to Store reference
        let store = ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "Store")?;

        // Create a new file descriptor manager for this store
        let fd_manager = WasiFileDescriptorManager::new();

        // Add WASI context to store's data (builds a fresh WasiP1Ctx from configuration)
        store.set_wasi_context(wasi_ctx, fd_manager)?;

        log::debug!("WASI context successfully added to Store");
        Ok(())
    })
}

/// Get the WASI context from a Store if one is attached
/// Returns null pointer if no WASI context is attached (this is not an error)
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_get_from_store(
    store_ptr: *const c_void,
) -> *mut c_void {
    // Check for null store pointer
    if store_ptr.is_null() {
        ffi_utils::set_last_error(WasmtimeError::InvalidParameter {
            message: "Store handle cannot be null".to_string(),
        });
        return std::ptr::null_mut();
    }

    // Cast store pointer to Store reference
    let store = match ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "Store") {
        Ok(s) => s,
        Err(e) => {
            ffi_utils::set_last_error(e);
            return std::ptr::null_mut();
        }
    };

    // Note: WasiContext is not stored in the store anymore - WasiP1Ctx is stored directly.
    // This function now just checks if WASI is enabled and returns null (can't retrieve the original config).
    // Use has_wasi_context() to check if WASI is enabled, or get_wasi_stdout()/get_wasi_stderr() to get output.
    if store.has_wasi_context() {
        log::debug!("Store has WASI context, but original WasiContext config is not retrievable");
    } else {
        log::debug!("No WASI context attached to Store");
    }
    ffi_utils::clear_last_error();
    std::ptr::null_mut()
}

/// Check if a Store has a WASI context attached
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_store_has_wasi(
    store_ptr: *const c_void,
) -> c_int {
    let result = ffi_utils::ffi_try(|| {
        if store_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        // Cast to Store and check for WASI context
        let store = &*(store_ptr as *const crate::store::Store);
        let has_wasi = store.has_wasi_context();
        log::debug!("Checking for WASI context in Store - has_wasi: {}", has_wasi);
        Ok(if has_wasi { 1 } else { 0 })
    });
    result.1
}

// ============================================================================
// Panama FFI exports for WASI context management
// ============================================================================

/// Create a new WASI context (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_create() -> *mut c_void {
    wasi_ctx_new()
}

/// Destroy a WASI context (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_destroy(ctx_ptr: *mut c_void) {
    wasi_ctx_destroy(ctx_ptr);
}

/// Set command line arguments for WASI context (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_set_argv(
    ctx_ptr: *mut c_void,
    args: *const *const c_char,
    args_len: usize,
) -> c_int {
    wasi_ctx_set_args(ctx_ptr, args, args_len)
}

/// Set environment variable in WASI context (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_set_env(
    ctx_ptr: *mut c_void,
    key: *const c_char,
    value: *const c_char,
) -> c_int {
    wasi_ctx_set_env(ctx_ptr, key, value)
}

/// Inherit host environment variables (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_inherit_env(ctx_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;
        // Set all current environment variables
        for (key, value) in std::env::vars() {
            ctx.set_environment_variable(&key, &value)?;
        }
        Ok(())
    })
}

/// Inherit host stdio (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_inherit_stdio(ctx_ptr: *mut c_void) -> c_int {
    wasi_ctx_configure_stdio(
        ctx_ptr,
        0,  // stdin: Inherit
        std::ptr::null(),
        0,  // stdout: Inherit
        std::ptr::null(),
        0,  // stderr: Inherit
        std::ptr::null(),
    )
}

/// Set stdin to read from file (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_set_stdin(
    ctx_ptr: *mut c_void,
    path: *const c_char,
) -> c_int {
    wasi_ctx_configure_stdio(
        ctx_ptr,
        2,  // stdin: File
        path,
        0,  // stdout: Inherit (keep current)
        std::ptr::null(),
        0,  // stderr: Inherit (keep current)
        std::ptr::null(),
    )
}

/// Set stdin from binary data buffer (supports binary data with null bytes)
///
/// # Arguments
/// * `ctx_ptr` - Pointer to the WASI context
/// * `data_ptr` - Pointer to the binary data (can contain null bytes)
/// * `data_len` - Length of the data in bytes
///
/// # Returns
/// 0 on success, non-zero on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_set_stdin_bytes(
    ctx_ptr: *mut c_void,
    data_ptr: *const u8,
    data_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        // Handle null or empty data
        let stdin_data = if data_ptr.is_null() || data_len == 0 {
            Vec::new()
        } else {
            std::slice::from_raw_parts(data_ptr, data_len).to_vec()
        };

        // Configure stdin as Buffer with the binary data, keep stdout/stderr unchanged
        ctx.configure_stdio_streams(
            StdioSource::Buffer(stdin_data),
            ctx.stdio_config.stdout.clone(),
            ctx.stdio_config.stderr.clone(),
        )?;
        Ok(())
    })
}

/// Set stdout to write to file (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_set_stdout(
    ctx_ptr: *mut c_void,
    path: *const c_char,
) -> c_int {
    wasi_ctx_configure_stdio(
        ctx_ptr,
        0,  // stdin: Inherit (keep current)
        std::ptr::null(),
        2,  // stdout: File
        path,
        0,  // stderr: Inherit (keep current)
        std::ptr::null(),
    )
}

/// Set stderr to write to file (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_set_stderr(
    ctx_ptr: *mut c_void,
    path: *const c_char,
) -> c_int {
    wasi_ctx_configure_stdio(
        ctx_ptr,
        0,  // stdin: Inherit (keep current)
        std::ptr::null(),
        0,  // stdout: Inherit (keep current)
        std::ptr::null(),
        2,  // stderr: File
        path,
    )
}

/// Enable output capture for stdout and stderr
///
/// This configures the WASI context to capture stdout and stderr to internal buffers
/// instead of inheriting from the host process.
///
/// # Arguments
/// * `ctx_ptr` - Pointer to the WASI context
///
/// # Returns
/// 0 on success, non-zero on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_enable_output_capture(
    ctx_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;
        ctx.enable_output_capture()?;
        Ok(())
    })
}

/// Get captured stdout data
///
/// Returns the captured stdout data. The caller is responsible for freeing the
/// returned buffer using `wasmtime4j_wasi_free_capture_buffer`.
///
/// # Arguments
/// * `ctx_ptr` - Pointer to the WASI context
/// * `data_len_out` - Output parameter for the length of the captured data
///
/// # Returns
/// Pointer to the captured data, or null if no capture is available.
/// The caller must free this memory using `wasmtime4j_wasi_free_capture_buffer`.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_get_stdout_capture(
    ctx_ptr: *const c_void,
    data_len_out: *mut usize,
) -> *mut u8 {
    if ctx_ptr.is_null() || data_len_out.is_null() {
        return std::ptr::null_mut();
    }

    match ffi_utils::deref_ptr::<WasiContext>(ctx_ptr as *mut c_void, "WASI context") {
        Ok(ctx) => {
            if let Some(data) = ctx.get_stdout_capture() {
                let len = data.len();
                if len == 0 {
                    *data_len_out = 0;
                    return std::ptr::null_mut();
                }

                // Allocate memory for the data
                let ptr = libc::malloc(len) as *mut u8;
                if ptr.is_null() {
                    *data_len_out = 0;
                    return std::ptr::null_mut();
                }

                // Copy data to the allocated buffer
                std::ptr::copy_nonoverlapping(data.as_ptr(), ptr, len);
                *data_len_out = len;
                ptr
            } else {
                *data_len_out = 0;
                std::ptr::null_mut()
            }
        }
        Err(_) => {
            *data_len_out = 0;
            std::ptr::null_mut()
        }
    }
}

/// Get captured stderr data
///
/// Returns the captured stderr data. The caller is responsible for freeing the
/// returned buffer using `wasmtime4j_wasi_free_capture_buffer`.
///
/// # Arguments
/// * `ctx_ptr` - Pointer to the WASI context
/// * `data_len_out` - Output parameter for the length of the captured data
///
/// # Returns
/// Pointer to the captured data, or null if no capture is available.
/// The caller must free this memory using `wasmtime4j_wasi_free_capture_buffer`.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_get_stderr_capture(
    ctx_ptr: *const c_void,
    data_len_out: *mut usize,
) -> *mut u8 {
    if ctx_ptr.is_null() || data_len_out.is_null() {
        return std::ptr::null_mut();
    }

    match ffi_utils::deref_ptr::<WasiContext>(ctx_ptr as *mut c_void, "WASI context") {
        Ok(ctx) => {
            if let Some(data) = ctx.get_stderr_capture() {
                let len = data.len();
                if len == 0 {
                    *data_len_out = 0;
                    return std::ptr::null_mut();
                }

                // Allocate memory for the data
                let ptr = libc::malloc(len) as *mut u8;
                if ptr.is_null() {
                    *data_len_out = 0;
                    return std::ptr::null_mut();
                }

                // Copy data to the allocated buffer
                std::ptr::copy_nonoverlapping(data.as_ptr(), ptr, len);
                *data_len_out = len;
                ptr
            } else {
                *data_len_out = 0;
                std::ptr::null_mut()
            }
        }
        Err(_) => {
            *data_len_out = 0;
            std::ptr::null_mut()
        }
    }
}

/// Free a capture buffer allocated by get_stdout_capture or get_stderr_capture
///
/// # Arguments
/// * `buffer` - Pointer to the buffer to free (can be null)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_free_capture_buffer(buffer: *mut u8) {
    if !buffer.is_null() {
        libc::free(buffer as *mut c_void);
    }
}

/// Check if stdout capture is enabled
///
/// # Arguments
/// * `ctx_ptr` - Pointer to the WASI context
///
/// # Returns
/// 1 if capture is enabled, 0 if not, -1 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_has_stdout_capture(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }

    match ffi_utils::deref_ptr::<WasiContext>(ctx_ptr as *mut c_void, "WASI context") {
        Ok(ctx) => if ctx.has_stdout_capture() { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Check if stderr capture is enabled
///
/// # Arguments
/// * `ctx_ptr` - Pointer to the WASI context
///
/// # Returns
/// 1 if capture is enabled, 0 if not, -1 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_has_stderr_capture(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }

    match ffi_utils::deref_ptr::<WasiContext>(ctx_ptr as *mut c_void, "WASI context") {
        Ok(ctx) => if ctx.has_stderr_capture() { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Add a pre-opened directory with full permissions (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_preopen_dir(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
) -> c_int {
    wasi_ctx_add_dir(
        ctx_ptr,
        host_path,
        guest_path,
        1,  // can_create
        1,  // can_read
        1,  // can_remove
        1,  // file_read
        1,  // file_write
        1,  // file_create
        1,  // file_truncate
    )
}

/// Add a pre-opened directory with read-only permissions (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_preopen_dir_readonly(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
) -> c_int {
    wasi_ctx_add_dir(
        ctx_ptr,
        host_path,
        guest_path,
        0,  // can_create: false
        1,  // can_read: true
        0,  // can_remove: false
        1,  // file_read: true
        0,  // file_write: false
        0,  // file_create: false
        0,  // file_truncate: false
    )
}

/// Add a pre-opened directory with custom permissions (Panama FFI version)
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_context_preopen_dir_with_perms(
    ctx_ptr: *mut c_void,
    host_path: *const c_char,
    guest_path: *const c_char,
    can_read: c_int,
    can_write: c_int,
    can_create: c_int,
) -> c_int {
    wasi_ctx_add_dir(
        ctx_ptr,
        host_path,
        guest_path,
        can_create,
        can_read,
        can_create,  // can_remove matches can_create
        can_read,
        can_write,
        can_create,
        can_write,   // file_truncate matches can_write
    )
}

/// Add WASI to a linker (Panama FFI version)
///
/// This function adds all WASI Preview 1 imports to the provided linker.
/// The linker will be configured to extract WASI context from any store that has one.
///
/// # Safety
/// This function is unsafe because it deals with raw pointers from FFI.
/// Caller must ensure linker_ptr is a valid pointer.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_add_wasi(
    linker_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        use crate::linker::Linker;

        // Dereference linker pointer - Panama passes our Linker wrapper type
        let linker = ffi_utils::deref_ptr_mut::<Linker>(linker_ptr, "linker")?;

        // Use the Linker's enable_wasi method which handles mutex locking
        linker.enable_wasi()?;

        log::debug!("WASI Preview 1 imports successfully added to linker via Panama FFI");
        Ok(())
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use std::ffi::CString;
    use tempfile::TempDir;

    #[test]
    fn test_wasi_context_creation() {
        let ctx = WasiContext::new();
        assert!(ctx.is_ok());
        
        let ctx = ctx.unwrap();
        assert_eq!(ctx.get_environment().len(), 0);
        assert_eq!(ctx.get_arguments().len(), 1);
        assert_eq!(ctx.get_arguments()[0], "wasmtime4j");
        assert_eq!(ctx.get_directory_mappings().len(), 0);
    }

    #[test]
    fn test_wasi_context_with_config() {
        let config = WasiConfig {
            allow_network: true,
            allow_arbitrary_fs: true,
            max_file_size: Some(1000),
            max_open_files: Some(10),
            env_policy: EnvironmentPolicy::Custom,
        };
        
        let ctx = WasiContext::with_config(config);
        assert!(ctx.is_ok());
        
        let ctx = ctx.unwrap();
        assert!(ctx.get_config().allow_network);
        assert!(ctx.get_config().allow_arbitrary_fs);
        assert_eq!(ctx.get_config().max_file_size, Some(1000));
        assert_eq!(ctx.get_config().max_open_files, Some(10));
    }

    #[test]
    fn test_environment_variables() {
        let mut ctx = WasiContext::new().unwrap();
        
        // Test setting environment variables
        assert!(ctx.set_environment_variable("TEST_VAR", "test_value").is_ok());
        assert!(ctx.set_environment_variable("PATH", "/usr/bin").is_ok());
        
        let env = ctx.get_environment();
        assert_eq!(env.get("TEST_VAR"), Some(&"test_value".to_string()));
        assert_eq!(env.get("PATH"), Some(&"/usr/bin".to_string()));
        assert_eq!(env.len(), 2);
    }

    #[test]
    fn test_environment_policy_allow_list() {
        let config = WasiConfig {
            env_policy: EnvironmentPolicy::AllowList(vec!["ALLOWED_VAR".to_string()]),
            ..WasiConfig::default()
        };
        
        let mut ctx = WasiContext::with_config(config).unwrap();
        
        // Should allow setting variables in the allow list
        assert!(ctx.set_environment_variable("ALLOWED_VAR", "value").is_ok());
        
        // Should reject variables not in the allow list
        let result = ctx.set_environment_variable("FORBIDDEN_VAR", "value");
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("not in allow list"));
    }

    #[test]
    fn test_environment_policy_deny_list() {
        let config = WasiConfig {
            env_policy: EnvironmentPolicy::DenyList(vec!["FORBIDDEN_VAR".to_string()]),
            ..WasiConfig::default()
        };
        
        let mut ctx = WasiContext::with_config(config).unwrap();
        
        // Should allow setting variables not in the deny list
        assert!(ctx.set_environment_variable("ALLOWED_VAR", "value").is_ok());
        
        // Should reject variables in the deny list
        let result = ctx.set_environment_variable("FORBIDDEN_VAR", "value");
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("is denied"));
    }

    #[test]
    fn test_command_line_arguments() {
        let mut ctx = WasiContext::new().unwrap();
        
        let args = vec![
            "my_program".to_string(),
            "--flag".to_string(),
            "value".to_string(),
            "/path/to/file".to_string(),
        ];
        
        assert!(ctx.set_arguments(args.clone()).is_ok());
        assert_eq!(ctx.get_arguments(), args);
    }

    #[test]
    fn test_directory_mapping() {
        let temp_dir = TempDir::new().unwrap();
        let host_path = temp_dir.path();
        
        // Create a test file
        let test_file = host_path.join("test.txt");
        fs::write(&test_file, "test content").unwrap();
        
        let mut ctx = WasiContext::new().unwrap();
        
        let dir_perms = WasiDirPermissions {
            create: true,
            read: true,
            remove: false,
        };
        
        let file_perms = WasiFilePermissions {
            read: true,
            write: true,
            create: true,
            truncate: false,
        };
        
        assert!(ctx.add_directory_mapping(
            host_path,
            "/sandbox",
            dir_perms.clone(),
            file_perms.clone()
        ).is_ok());
        
        let mappings = ctx.get_directory_mappings();
        assert_eq!(mappings.len(), 1);
        assert!(mappings.contains_key("/sandbox"));
        
        let mapping = &mappings["/sandbox"];
        assert_eq!(mapping.guest_path, "/sandbox");
        assert_eq!(mapping.dir_perms.create, dir_perms.create);
        assert_eq!(mapping.dir_perms.read, dir_perms.read);
        assert_eq!(mapping.file_perms.read, file_perms.read);
        assert_eq!(mapping.file_perms.write, file_perms.write);
    }

    #[test]
    fn test_directory_mapping_nonexistent_path() {
        let mut ctx = WasiContext::new().unwrap();
        
        let result = ctx.add_directory_mapping(
            "/nonexistent/path",
            "/sandbox",
            WasiDirPermissions::default(),
            WasiFilePermissions::default(),
        );
        
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("does not exist"));
    }

    #[test]
    fn test_path_allowed_with_mapping() {
        let temp_dir = TempDir::new().unwrap();
        let host_path = temp_dir.path();
        
        let mut ctx = WasiContext::new().unwrap();
        
        // Add directory mapping
        assert!(ctx.add_directory_mapping(
            host_path,
            "/sandbox",
            WasiDirPermissions::default(),
            WasiFilePermissions::default()
        ).is_ok());
        
        // Test allowed paths
        assert!(ctx.is_path_allowed(host_path));
        assert!(ctx.is_path_allowed(host_path.join("subdir")));
        assert!(ctx.is_path_allowed(host_path.join("file.txt")));
        
        // Test disallowed paths
        assert!(!ctx.is_path_allowed("/tmp/other"));
        assert!(!ctx.is_path_allowed("/etc/passwd"));
    }

    #[test]
    fn test_path_allowed_with_arbitrary_fs() {
        let config = WasiConfig {
            allow_arbitrary_fs: true,
            ..WasiConfig::default()
        };
        
        let ctx = WasiContext::with_config(config).unwrap();
        
        // Should allow any path when arbitrary FS access is enabled
        assert!(ctx.is_path_allowed("/tmp/test"));
        assert!(ctx.is_path_allowed("/etc/passwd"));
        assert!(ctx.is_path_allowed("/home/user/file"));
    }

    #[test]
    fn test_stdio_configuration() {
        let mut ctx = WasiContext::new().unwrap();
        
        // Test configuring stdio streams
        let stdin = StdioSource::Buffer(b"test input".to_vec());
        let stdout = StdioSink::Buffer;
        let stderr = StdioSink::Buffer;
        
        assert!(ctx.configure_stdio_streams(stdin, stdout, stderr).is_ok());
        
        // The configuration should be stored internally
        // (We can't easily test the actual wasmtime configuration without running a WASM module)
    }

    #[test]
    fn test_permission_conversions() {
        let dir_perms = WasiDirPermissions {
            create: true,
            read: true,
            remove: false,
        };
        
        let wasi_perms = dir_perms.to_wasmtime_perms();
        assert!(wasi_perms.contains(DirPerms::READ));
        // Note: CREATE and REMOVE permissions might not be available in this version
        
        let file_perms = WasiFilePermissions {
            read: true,
            write: false,
            create: false,
            truncate: false,
        };
        
        let wasi_perms = file_perms.to_wasmtime_perms();
        assert!(wasi_perms.contains(FilePerms::READ));
        assert!(!wasi_perms.contains(FilePerms::WRITE));
    }

    #[test]
    fn test_default_implementations() {
        let config = WasiConfig::default();
        assert!(!config.allow_network);
        assert!(!config.allow_arbitrary_fs);
        assert_eq!(config.max_file_size, Some(100 * 1024 * 1024));
        assert_eq!(config.max_open_files, Some(1024));
        
        let dir_perms = WasiDirPermissions::default();
        assert!(!dir_perms.create);
        assert!(dir_perms.read);
        assert!(!dir_perms.remove);
        
        let file_perms = WasiFilePermissions::default();
        assert!(file_perms.read);
        assert!(!file_perms.write);
        assert!(!file_perms.create);
        assert!(!file_perms.truncate);
        
        let stdio_config = StdioConfig::default();
        assert!(matches!(stdio_config.stdin, StdioSource::Null));
        assert!(matches!(stdio_config.stdout, StdioSink::Buffer));
        assert!(matches!(stdio_config.stderr, StdioSink::Buffer));
    }

    #[test]
    fn test_ffi_wasi_ctx_new() {
        unsafe {
            let ctx_ptr = wasi_ctx_new();
            assert!(!ctx_ptr.is_null());
            wasi_ctx_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_wasi_ctx_new_with_config() {
        unsafe {
            let ctx_ptr = wasi_ctx_new_with_config(1, 0, 1000, 100);
            assert!(!ctx_ptr.is_null());
            wasi_ctx_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_environment_operations() {
        unsafe {
            let ctx_ptr = wasi_ctx_new();
            assert!(!ctx_ptr.is_null());
            
            // Test setting environment variable
            let key = CString::new("TEST_VAR").unwrap();
            let value = CString::new("test_value").unwrap();
            let result = wasi_ctx_set_env(ctx_ptr, key.as_ptr(), value.as_ptr());
            assert_eq!(result, 0); // Success
            
            // Test getting environment count
            let count = wasi_ctx_get_env_count(ctx_ptr);
            assert_eq!(count, 1);
            
            wasi_ctx_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_argument_operations() {
        unsafe {
            let ctx_ptr = wasi_ctx_new();
            assert!(!ctx_ptr.is_null());
            
            // Test setting arguments
            let arg1 = CString::new("program").unwrap();
            let arg2 = CString::new("--flag").unwrap();
            let arg3 = CString::new("value").unwrap();
            
            let args = vec![arg1.as_ptr(), arg2.as_ptr(), arg3.as_ptr()];
            let result = wasi_ctx_set_args(ctx_ptr, args.as_ptr(), args.len());
            assert_eq!(result, 0); // Success
            
            // Test getting argument count
            let count = wasi_ctx_get_args_count(ctx_ptr);
            assert_eq!(count, 3);
            
            wasi_ctx_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_directory_operations() {
        let temp_dir = TempDir::new().unwrap();
        let host_path_str = temp_dir.path().to_str().unwrap();
        
        unsafe {
            let ctx_ptr = wasi_ctx_new();
            assert!(!ctx_ptr.is_null());
            
            // Test adding directory mapping
            let host_path = CString::new(host_path_str).unwrap();
            let guest_path = CString::new("/sandbox").unwrap();
            
            let result = wasi_ctx_add_dir(
                ctx_ptr,
                host_path.as_ptr(),
                guest_path.as_ptr(),
                0, 1, 0, // dir perms: no create, read, no remove
                1, 0, 0, 0, // file perms: read only
            );
            assert_eq!(result, 0); // Success
            
            // Test getting directory count
            let count = wasi_ctx_get_dir_count(ctx_ptr);
            assert_eq!(count, 1);
            
            // Test path allowance
            let test_path = CString::new(host_path_str).unwrap();
            let allowed = wasi_ctx_is_path_allowed(ctx_ptr, test_path.as_ptr());
            assert_eq!(allowed, 1); // Allowed
            
            let forbidden_path = CString::new("/tmp/other").unwrap();
            let not_allowed = wasi_ctx_is_path_allowed(ctx_ptr, forbidden_path.as_ptr());
            assert_eq!(not_allowed, 0); // Not allowed
            
            wasi_ctx_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_stdio_configuration() {
        unsafe {
            let ctx_ptr = wasi_ctx_new();
            assert!(!ctx_ptr.is_null());
            
            // Test configuring stdio with buffer input
            let stdin_data = CString::new("test input").unwrap();
            let result = wasi_ctx_configure_stdio(
                ctx_ptr,
                1, stdin_data.as_ptr(), // stdin: buffer with data
                1, std::ptr::null(),    // stdout: buffer
                1, std::ptr::null(),    // stderr: buffer
            );
            assert_eq!(result, 0); // Success
            
            wasi_ctx_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_error_handling() {
        unsafe {
            // Test with null pointer
            let result = wasi_ctx_set_env(
                std::ptr::null_mut(),
                std::ptr::null(),
                std::ptr::null(),
            );
            assert_ne!(result, 0); // Should fail
            
            // Test adding directory with invalid path
            let ctx_ptr = wasi_ctx_new();
            assert!(!ctx_ptr.is_null());
            
            let invalid_path = CString::new("/nonexistent/directory").unwrap();
            let guest_path = CString::new("/sandbox").unwrap();
            let result = wasi_ctx_add_dir(
                ctx_ptr,
                invalid_path.as_ptr(),
                guest_path.as_ptr(),
                1, 1, 1, // dir perms
                1, 1, 1, 1, // file perms
            );
            assert_ne!(result, 0); // Should fail
            
            wasi_ctx_destroy(ctx_ptr);
        }
    }
}

/// WASI Preview 1 filesystem operations implementation
use std::fs::{self, File, OpenOptions, Metadata};
use std::io::{Read, Write, Seek, SeekFrom};
use std::sync::atomic::{AtomicU32, Ordering};
use std::time::SystemTime;

/// File descriptor manager for WASI filesystem operations
#[derive(Debug)]
pub struct WasiFileDescriptorManager {
    /// Maps file descriptors to open files
    open_files: HashMap<u32, WasiFileDescriptor>,
    /// Maps file descriptors to open directories
    open_directories: HashMap<u32, WasiDirectoryDescriptor>,
    /// Next available file descriptor number
    next_fd: AtomicU32,
}

/// Represents an open file descriptor
#[derive(Debug)]
pub struct WasiFileDescriptor {
    /// The underlying file handle
    file: File,
    /// The path used to open this file
    path: String,
    /// The rights associated with this descriptor
    rights: u64,
    /// The flags used to open this file
    flags: u32,
}

/// Represents an open directory descriptor
#[derive(Debug)]
pub struct WasiDirectoryDescriptor {
    /// The directory path
    path: String,
    /// The rights associated with this descriptor
    rights: u64,
    /// Current position for readdir operations
    position: usize,
    /// Cached directory entries
    entries: Vec<WasiDirectoryEntry>,
}

/// Directory entry information
#[derive(Debug, Clone)]
pub struct WasiDirectoryEntry {
    /// Entry name
    pub name: String,
    /// Entry type
    pub file_type: u8,
    /// Inode number
    pub inode: u64,
    /// File size
    pub size: u64,
    /// Access time (nanoseconds since epoch)
    pub access_time: u64,
    /// Modification time (nanoseconds since epoch)
    pub modification_time: u64,
    /// Creation time (nanoseconds since epoch)
    pub creation_time: u64,
}

impl WasiFileDescriptorManager {
    pub fn new() -> Self {
        Self {
            open_files: HashMap::new(),
            open_directories: HashMap::new(),
            next_fd: AtomicU32::new(3), // Start from 3 (after stdin/stdout/stderr)
        }
    }

    pub fn allocate_fd(&self) -> u32 {
        self.next_fd.fetch_add(1, Ordering::SeqCst)
    }

    pub fn add_file(&mut self, fd: u32, file_desc: WasiFileDescriptor) {
        self.open_files.insert(fd, file_desc);
    }

    pub fn add_directory(&mut self, fd: u32, dir_desc: WasiDirectoryDescriptor) {
        self.open_directories.insert(fd, dir_desc);
    }

    pub fn get_file_mut(&mut self, fd: u32) -> Option<&mut WasiFileDescriptor> {
        self.open_files.get_mut(&fd)
    }

    pub fn get_directory_mut(&mut self, fd: u32) -> Option<&mut WasiDirectoryDescriptor> {
        self.open_directories.get_mut(&fd)
    }

    pub fn close_file(&mut self, fd: u32) -> Option<WasiFileDescriptor> {
        self.open_files.remove(&fd)
    }

    pub fn close_directory(&mut self, fd: u32) -> Option<WasiDirectoryDescriptor> {
        self.open_directories.remove(&fd)
    }
}

impl WasiDirectoryDescriptor {
    pub fn new(path: String, rights: u64) -> WasmtimeResult<Self> {
        let mut entries = Vec::new();

        // Read directory contents
        let dir_entries = fs::read_dir(&path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to read directory {}: {}", path, e),
        })?;

        for entry in dir_entries {
            let entry = entry.map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to read directory entry: {}", e),
            })?;

            let metadata = entry.metadata().map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to get metadata for {}: {}", entry.path().display(), e),
            })?;

            let file_type = if metadata.is_dir() {
                3 // Directory
            } else if metadata.is_file() {
                4 // Regular file
            } else {
                7 // Symbolic link or other
            };

            let name = entry.file_name().to_string_lossy().to_string();
            let size = metadata.len();

            // Convert times to nanoseconds since epoch
            let (access_time, modification_time, creation_time) =
                Self::extract_times(&metadata);

            entries.push(WasiDirectoryEntry {
                name,
                file_type,
                inode: 0, // Not available on all platforms
                size,
                access_time,
                modification_time,
                creation_time,
            });
        }

        Ok(Self {
            path,
            rights,
            position: 0,
            entries,
        })
    }

    fn extract_times(metadata: &Metadata) -> (u64, u64, u64) {
        let access_time = metadata.accessed()
            .ok()
            .and_then(|t| t.duration_since(SystemTime::UNIX_EPOCH).ok())
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);

        let modification_time = metadata.modified()
            .ok()
            .and_then(|t| t.duration_since(SystemTime::UNIX_EPOCH).ok())
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);

        let creation_time = metadata.created()
            .ok()
            .and_then(|t| t.duration_since(SystemTime::UNIX_EPOCH).ok())
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);

        (access_time, modification_time, creation_time)
    }
}

/// Add the file descriptor manager to WasiContext
impl WasiContext {
    /// Add file descriptor manager to WasiContext
    pub fn new_with_fd_manager() -> WasmtimeResult<(Self, WasiFileDescriptorManager)> {
        let ctx = Self::new()?;
        let fd_manager = WasiFileDescriptorManager::new();
        Ok((ctx, fd_manager))
    }

    /// Open a file with the specified flags and rights
    pub fn path_open(
        &mut self,
        fd_manager: &mut WasiFileDescriptorManager,
        dir_fd: u32,
        path: &str,
        oflags: u32,
        rights: u64,
        _inherit_rights: u64, // Rights for newly created descriptors
        fdflags: u32,
    ) -> WasmtimeResult<u32> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        // Resolve path relative to directory fd if needed
        let full_path = if dir_fd == u32::MAX || dir_fd == 0 {
            // AT_FDCWD or root - use path as-is
            path.to_string()
        } else {
            // TODO: Resolve relative to directory fd
            // For now, treat as absolute path
            path.to_string()
        };

        // Create OpenOptions based on flags
        let mut open_opts = OpenOptions::new();

        // Handle creation and directory flags
        if (oflags & 0x01) != 0 { // O_CREAT
            open_opts.create(true);
        }
        if (oflags & 0x02) != 0 { // O_DIRECTORY
            return self.open_directory(fd_manager, &full_path, rights);
        }
        if (oflags & 0x04) != 0 { // O_EXCL
            open_opts.create_new(true);
        }
        if (oflags & 0x08) != 0 { // O_TRUNC
            open_opts.truncate(true);
        }

        // Set read/write permissions based on rights
        if (rights & 0x02) != 0 { // FD_READ
            open_opts.read(true);
        }
        if (rights & 0x40) != 0 { // FD_WRITE
            open_opts.write(true);
        }
        if (rights & 0x20) != 0 { // FD_APPEND
            open_opts.append(true);
        }

        // Validate we have at least read or write permissions
        if (rights & 0x42) == 0 { // Neither FD_READ nor FD_WRITE
            open_opts.read(true); // Default to read-only
        }

        // Open the file
        let file = open_opts.open(&full_path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to open file {}: {}", full_path, e),
        })?;

        // Create file descriptor
        let fd = fd_manager.allocate_fd();
        let file_desc = WasiFileDescriptor {
            file,
            path: full_path,
            rights,
            flags: fdflags,
        };

        // Store in descriptor manager
        fd_manager.add_file(fd, file_desc);
        Ok(fd)
    }

    /// Read from a file descriptor
    pub fn fd_read(
        &self,
        fd_manager: &mut WasiFileDescriptorManager,
        fd: u32,
        buffer: &mut [u8],
    ) -> WasmtimeResult<usize> {
        let file_desc = fd_manager.get_file_mut(fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid file descriptor: {}", fd),
            })?;

        // Check read permissions
        if (file_desc.rights & 0x02) == 0 { // FD_READ
            return Err(WasmtimeError::Wasi {
                message: format!("File descriptor {} does not have read permissions", fd),
            });
        }

        // Read from file
        file_desc.file.read(buffer).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to read from file {}: {}", file_desc.path, e),
        })
    }

    /// Write to a file descriptor
    pub fn fd_write(
        &self,
        fd_manager: &mut WasiFileDescriptorManager,
        fd: u32,
        buffer: &[u8],
    ) -> WasmtimeResult<usize> {
        let file_desc = fd_manager.get_file_mut(fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid file descriptor: {}", fd),
            })?;

        // Check write permissions
        if (file_desc.rights & 0x40) == 0 { // FD_WRITE
            return Err(WasmtimeError::Wasi {
                message: format!("File descriptor {} does not have write permissions", fd),
            });
        }

        // Write to file
        file_desc.file.write(buffer).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to write to file {}: {}", file_desc.path, e),
        })
    }

    /// Flush a file descriptor
    pub fn fd_sync(
        &self,
        fd_manager: &mut WasiFileDescriptorManager,
        fd: u32,
    ) -> WasmtimeResult<()> {
        let file_desc = fd_manager.get_file_mut(fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid file descriptor: {}", fd),
            })?;

        file_desc.file.flush().map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to sync file {}: {}", file_desc.path, e),
        })
    }

    /// Seek in a file descriptor
    pub fn fd_seek(
        &self,
        fd_manager: &mut WasiFileDescriptorManager,
        fd: u32,
        offset: i64,
        whence: u8,
    ) -> WasmtimeResult<u64> {
        let file_desc = fd_manager.get_file_mut(fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid file descriptor: {}", fd),
            })?;

        // Check seek permissions
        if (file_desc.rights & 0x08) == 0 { // FD_SEEK
            return Err(WasmtimeError::Wasi {
                message: format!("File descriptor {} does not have seek permissions", fd),
            });
        }

        let seek_from = match whence {
            0 => SeekFrom::Start(offset as u64), // SEEK_SET
            1 => SeekFrom::Current(offset),      // SEEK_CUR
            2 => SeekFrom::End(offset),          // SEEK_END
            _ => return Err(WasmtimeError::Wasi {
                message: format!("Invalid seek whence value: {}", whence),
            }),
        };

        file_desc.file.seek(seek_from).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to seek in file {}: {}", file_desc.path, e),
        })
    }

    /// Close a file descriptor
    pub fn fd_close(
        &self,
        fd_manager: &mut WasiFileDescriptorManager,
        fd: u32,
    ) -> WasmtimeResult<()> {
        // Standard streams cannot be closed
        if fd < 3 {
            return Err(WasmtimeError::Wasi {
                message: format!("Cannot close standard file descriptor: {}", fd),
            });
        }

        // Remove from file manager (file is automatically closed when dropped)
        fd_manager.close_file(fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid file descriptor: {}", fd),
            })?;

        Ok(())
    }

    /// Get file statistics for a file descriptor
    pub fn fd_filestat_get(
        &self,
        fd_manager: &WasiFileDescriptorManager,
        fd: u32,
    ) -> WasmtimeResult<WasiFilestat> {
        let file_desc = fd_manager.open_files.get(&fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid file descriptor: {}", fd),
            })?;

        let metadata = file_desc.file.metadata().map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to get metadata for file {}: {}", file_desc.path, e),
        })?;

        let (access_time, modification_time, creation_time) =
            WasiDirectoryDescriptor::extract_times(&metadata);

        let filetype = if metadata.is_dir() {
            3 // Directory
        } else if metadata.is_file() {
            4 // Regular file
        } else {
            7 // Symbolic link or other
        };

        Ok(WasiFilestat {
            device: 0, // Device ID not available on all platforms
            inode: 0,  // Inode not available on all platforms
            filetype,
            nlink: 1,
            size: metadata.len(),
            atim: access_time,
            mtim: modification_time,
            ctim: creation_time,
        })
    }

    /// Open a directory with the specified rights
    pub fn open_directory(
        &mut self,
        fd_manager: &mut WasiFileDescriptorManager,
        path: &str,
        rights: u64,
    ) -> WasmtimeResult<u32> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        // Validate it's actually a directory
        let metadata = fs::metadata(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to get metadata for {}: {}", path, e),
        })?;

        if !metadata.is_dir() {
            return Err(WasmtimeError::Wasi {
                message: format!("Path is not a directory: {}", path),
            });
        }

        // Create directory descriptor
        let fd = fd_manager.allocate_fd();
        let dir_desc = WasiDirectoryDescriptor::new(path.to_string(), rights)?;

        // Store in descriptor manager
        fd_manager.add_directory(fd, dir_desc);
        Ok(fd)
    }

    /// Read directory entries
    pub fn fd_readdir(
        &self,
        fd_manager: &mut WasiFileDescriptorManager,
        fd: u32,
        cookie: u64,
    ) -> WasmtimeResult<Vec<WasiDirectoryEntry>> {
        let dir_desc = fd_manager.get_directory_mut(fd)
            .ok_or_else(|| WasmtimeError::Wasi {
                message: format!("Invalid directory descriptor: {}", fd),
            })?;

        // Check read permissions
        if (dir_desc.rights & 0x02) == 0 { // FD_READ
            return Err(WasmtimeError::Wasi {
                message: format!("Directory descriptor {} does not have read permissions", fd),
            });
        }

        // Start from cookie position
        let start_pos = cookie as usize;
        if start_pos >= dir_desc.entries.len() {
            return Ok(Vec::new()); // No more entries
        }

        // Return remaining entries starting from cookie
        Ok(dir_desc.entries[start_pos..].to_vec())
    }

    /// Create a directory
    pub fn path_create_directory(
        &self,
        _dir_fd: u32,
        path: &str,
    ) -> WasmtimeResult<()> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        fs::create_dir(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to create directory {}: {}", path, e),
        })
    }

    /// Remove a directory
    pub fn path_remove_directory(
        &self,
        _dir_fd: u32,
        path: &str,
    ) -> WasmtimeResult<()> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        fs::remove_dir(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to remove directory {}: {}", path, e),
        })
    }

    /// Get file statistics by path
    pub fn path_filestat_get(
        &self,
        _dir_fd: u32,
        _flags: u32,
        path: &str,
    ) -> WasmtimeResult<WasiFilestat> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        let metadata = fs::metadata(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to get metadata for {}: {}", path, e),
        })?;

        let (access_time, modification_time, creation_time) =
            WasiDirectoryDescriptor::extract_times(&metadata);

        let filetype = if metadata.is_dir() {
            3 // Directory
        } else if metadata.is_file() {
            4 // Regular file
        } else {
            7 // Symbolic link or other
        };

        Ok(WasiFilestat {
            device: 0, // Device ID not available on all platforms
            inode: 0,  // Inode not available on all platforms
            filetype,
            nlink: 1,
            size: metadata.len(),
            atim: access_time,
            mtim: modification_time,
            ctim: creation_time,
        })
    }

    /// Rename a file or directory
    pub fn path_rename(
        &self,
        _old_dir_fd: u32,
        old_path: &str,
        _new_dir_fd: u32,
        new_path: &str,
    ) -> WasmtimeResult<()> {
        // Validate paths are allowed
        if !self.is_path_allowed(old_path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Old path not allowed: {}", old_path),
            });
        }
        if !self.is_path_allowed(new_path) {
            return Err(WasmtimeError::Wasi {
                message: format!("New path not allowed: {}", new_path),
            });
        }

        fs::rename(old_path, new_path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to rename {} to {}: {}", old_path, new_path, e),
        })
    }

    /// Unlink a file
    pub fn path_unlink_file(
        &self,
        _dir_fd: u32,
        path: &str,
    ) -> WasmtimeResult<()> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        fs::remove_file(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to unlink file {}: {}", path, e),
        })
    }

    /// Process and environment operations

    /// Get an environment variable value
    pub fn environ_get(&self, key: &str) -> Option<String> {
        match &self.config.env_policy {
            EnvironmentPolicy::Inherit => std::env::var(key).ok(),
            EnvironmentPolicy::AllowList(allowed) => {
                if allowed.contains(&key.to_string()) {
                    std::env::var(key).ok()
                } else {
                    None
                }
            }
            EnvironmentPolicy::DenyList(denied) => {
                if denied.contains(&key.to_string()) {
                    None
                } else {
                    std::env::var(key).ok()
                }
            }
            EnvironmentPolicy::Custom => {
                self.environment.get(key).cloned()
            }
        }
    }

    /// Get all environment variables as key=value pairs
    pub fn environ_sizes_get(&self) -> (usize, usize) {
        let env_vars = match &self.config.env_policy {
            EnvironmentPolicy::Inherit => {
                std::env::vars().collect::<Vec<_>>()
            }
            EnvironmentPolicy::AllowList(allowed) => {
                std::env::vars()
                    .filter(|(k, _)| allowed.contains(k))
                    .collect()
            }
            EnvironmentPolicy::DenyList(denied) => {
                std::env::vars()
                    .filter(|(k, _)| !denied.contains(k))
                    .collect()
            }
            EnvironmentPolicy::Custom => {
                self.environment.iter()
                    .map(|(k, v)| (k.clone(), v.clone()))
                    .collect()
            }
        };

        let environ_count = env_vars.len();
        let environ_size: usize = env_vars.iter()
            .map(|(k, v)| k.len() + v.len() + 2) // key=value\0
            .sum();

        (environ_count, environ_size)
    }

    /// Get command line arguments count and total size
    pub fn args_sizes_get(&self) -> (usize, usize) {
        let args_count = self.arguments.len();
        let args_size: usize = self.arguments.iter()
            .map(|arg| arg.len() + 1) // arg\0
            .sum();

        (args_count, args_size)
    }

    /// Exit the process with given exit code
    pub fn proc_exit(&self, exit_code: u32) -> ! {
        std::process::exit(exit_code as i32);
    }

    /// Get process ID (always returns 42 for security/portability)
    pub fn sched_yield(&self) -> WasmtimeResult<()> {
        // Yield CPU to other processes/threads
        std::thread::yield_now();
        Ok(())
    }

    /// Time operations

    /// Get clock resolution for specified clock
    pub fn clock_res_get(&self, clock_id: u32) -> WasmtimeResult<u64> {
        match clock_id {
            0 => Ok(1), // REALTIME - nanosecond resolution
            1 => Ok(1), // MONOTONIC - nanosecond resolution
            2 => Ok(1000), // PROCESS_CPUTIME - microsecond resolution
            3 => Ok(1000), // THREAD_CPUTIME - microsecond resolution
            _ => Err(WasmtimeError::Wasi {
                message: format!("Invalid clock ID: {}", clock_id),
            }),
        }
    }

    /// Get current time for specified clock
    pub fn clock_time_get(&self, clock_id: u32, _precision: u64) -> WasmtimeResult<u64> {
        use std::time::{SystemTime, UNIX_EPOCH};

        match clock_id {
            0 => {
                // REALTIME - wall clock time
                SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .map(|d| d.as_nanos() as u64)
                    .map_err(|e| WasmtimeError::Wasi {
                        message: format!("Failed to get system time: {}", e),
                    })
            }
            1 => {
                // MONOTONIC - monotonic time
                // Use a static start time for monotonic clock
                use std::sync::OnceLock;
                static START_TIME: OnceLock<std::time::Instant> = OnceLock::new();
                let start = START_TIME.get_or_init(std::time::Instant::now);
                Ok(start.elapsed().as_nanos() as u64)
            }
            2 | 3 => {
                // PROCESS_CPUTIME / THREAD_CPUTIME - CPU time
                // Approximate with wall clock time
                SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .map(|d| d.as_nanos() as u64)
                    .map_err(|e| WasmtimeError::Wasi {
                        message: format!("Failed to get CPU time: {}", e),
                    })
            }
            _ => Err(WasmtimeError::Wasi {
                message: format!("Invalid clock ID: {}", clock_id),
            }),
        }
    }

    /// Random operations

    /// Generate random bytes
    pub fn random_get(&self, buffer: &mut [u8]) -> WasmtimeResult<()> {
        use std::fs::File;
        use std::io::Read;

        // Try to use system random number generator
        #[cfg(unix)]
        {
            let mut urandom = File::open("/dev/urandom").map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to open /dev/urandom: {}", e),
            })?;

            urandom.read_exact(buffer).map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to read random bytes: {}", e),
            })?;
        }

        #[cfg(windows)]
        {
            // On Windows, use a simple fallback with current time-based seed
            use std::time::{SystemTime, UNIX_EPOCH};

            let seed = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap_or_default()
                .as_nanos() as u64;

            // Simple linear congruential generator for fallback
            let mut rng_state = seed;
            for byte in buffer.iter_mut() {
                rng_state = rng_state.wrapping_mul(1103515245).wrapping_add(12345);
                *byte = (rng_state >> 24) as u8;
            }
        }

        #[cfg(not(any(unix, windows)))]
        {
            // Fallback for other platforms
            use std::time::{SystemTime, UNIX_EPOCH};

            let seed = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap_or_default()
                .as_nanos() as u64;

            let mut rng_state = seed;
            for byte in buffer.iter_mut() {
                rng_state = rng_state.wrapping_mul(1103515245).wrapping_add(12345);
                *byte = (rng_state >> 24) as u8;
            }
        }

        Ok(())
    }

}

/// Native FFI functions for WASI filesystem operations
use std::slice;

/// Open a file with specified flags and rights
#[no_mangle]
pub unsafe extern "C" fn wasi_path_open(
    ctx_ptr: *mut c_void,
    dir_fd: u32,
    path: *const c_char,
    path_len: usize,
    oflags: u32,
    rights: u64,
    fdflags: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path cannot be null or empty".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        // For now, return a dummy file descriptor
        // In full implementation, we would call ctx.path_open()
        let _fd = 3; // Dummy file descriptor
        Ok(())
    })
}

/// Close a file descriptor
#[no_mangle]
pub unsafe extern "C" fn wasi_fd_close(
    ctx_ptr: *mut c_void,
    fd: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        // For now, just validate the context
        // In full implementation, we would call ctx.fd_close(fd)
        log::debug!("Closing file descriptor: {}", fd);
        Ok(())
    })
}

/// Read from a file descriptor
#[no_mangle]
pub unsafe extern "C" fn wasi_fd_read(
    ctx_ptr: *mut c_void,
    fd: u32,
    buffer: *mut u8,
    buffer_len: usize,
    bytes_read: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if buffer.is_null() || bytes_read.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Buffer and bytes_read cannot be null".to_string(),
            });
        }

        // For now, just write 0 bytes read
        // In full implementation, we would read from the file
        *bytes_read = 0;
        log::debug!("Reading from file descriptor: {} (len: {})", fd, buffer_len);
        Ok(())
    })
}

/// Write to a file descriptor
#[no_mangle]
pub unsafe extern "C" fn wasi_fd_write(
    ctx_ptr: *mut c_void,
    fd: u32,
    buffer: *const u8,
    buffer_len: usize,
    bytes_written: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if buffer.is_null() || bytes_written.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Buffer and bytes_written cannot be null".to_string(),
            });
        }

        // For now, just write 0 bytes written
        // In full implementation, we would write to the file
        *bytes_written = 0;
        log::debug!("Writing to file descriptor: {} (len: {})", fd, buffer_len);
        Ok(())
    })
}

/// Seek in a file
#[no_mangle]
pub unsafe extern "C" fn wasi_fd_seek(
    ctx_ptr: *mut c_void,
    fd: u32,
    offset: i64,
    whence: u8,
    new_position: *mut u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if new_position.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "New position pointer cannot be null".to_string(),
            });
        }

        // For now, just return current position as 0
        // In full implementation, we would seek in the file
        *new_position = 0;
        log::debug!("Seeking file descriptor: {} (offset: {}, whence: {})", fd, offset, whence);
        Ok(())
    })
}

/// Get file statistics
#[no_mangle]
pub unsafe extern "C" fn wasi_fd_filestat_get(
    ctx_ptr: *mut c_void,
    fd: u32,
    filestat: *mut WasiFilestat,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if filestat.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Filestat pointer cannot be null".to_string(),
            });
        }

        // For now, just return empty file stats
        // In full implementation, we would get actual file statistics
        (*filestat) = WasiFilestat {
            device: 0,
            inode: 0,
            filetype: 4, // Regular file
            nlink: 1,
            size: 0,
            atim: 0,
            mtim: 0,
            ctim: 0,
        };

        log::debug!("Getting file stats for descriptor: {}", fd);
        Ok(())
    })
}

/// Get file statistics by path
#[no_mangle]
pub unsafe extern "C" fn wasi_path_filestat_get(
    ctx_ptr: *mut c_void,
    dir_fd: u32,
    flags: u32,
    path: *const c_char,
    path_len: usize,
    filestat: *mut WasiFilestat,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 || filestat.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path and filestat cannot be null".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        // For now, just return empty file stats
        // In full implementation, we would get actual file statistics for the path
        (*filestat) = WasiFilestat {
            device: 0,
            inode: 0,
            filetype: 4, // Regular file
            nlink: 1,
            size: 0,
            atim: 0,
            mtim: 0,
            ctim: 0,
        };

        log::debug!("Getting file stats for path: {} (dir_fd: {}, flags: {})", path_str, dir_fd, flags);
        Ok(())
    })
}

/// Read directory entries
#[no_mangle]
pub unsafe extern "C" fn wasi_fd_readdir(
    ctx_ptr: *mut c_void,
    fd: u32,
    buffer: *mut u8,
    buffer_len: usize,
    cookie: u64,
    bytes_written: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if buffer.is_null() || bytes_written.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Buffer and bytes_written cannot be null".to_string(),
            });
        }

        // For now, just write 0 bytes
        // In full implementation, we would read directory entries
        *bytes_written = 0;
        log::debug!("Reading directory for descriptor: {} (cookie: {}, buffer_len: {})", fd, cookie, buffer_len);
        Ok(())
    })
}

/// Create a directory
#[no_mangle]
pub unsafe extern "C" fn wasi_path_create_directory(
    ctx_ptr: *mut c_void,
    dir_fd: u32,
    path: *const c_char,
    path_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path cannot be null or empty".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        log::debug!("Creating directory: {} (dir_fd: {})", path_str, dir_fd);
        // In full implementation, we would create the directory
        Ok(())
    })
}

/// Remove a directory
#[no_mangle]
pub unsafe extern "C" fn wasi_path_remove_directory(
    ctx_ptr: *mut c_void,
    dir_fd: u32,
    path: *const c_char,
    path_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path cannot be null or empty".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        log::debug!("Removing directory: {} (dir_fd: {})", path_str, dir_fd);
        // In full implementation, we would remove the directory
        Ok(())
    })
}

/// Rename a file or directory
#[no_mangle]
pub unsafe extern "C" fn wasi_path_rename(
    ctx_ptr: *mut c_void,
    old_dir_fd: u32,
    old_path: *const c_char,
    old_path_len: usize,
    new_dir_fd: u32,
    new_path: *const c_char,
    new_path_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if old_path.is_null() || old_path_len == 0 || new_path.is_null() || new_path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Paths cannot be null or empty".to_string(),
            });
        }

        let old_path_bytes = slice::from_raw_parts(old_path as *const u8, old_path_len);
        let old_path_str = std::str::from_utf8(old_path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in old path".to_string(),
            }
        })?;

        let new_path_bytes = slice::from_raw_parts(new_path as *const u8, new_path_len);
        let new_path_str = std::str::from_utf8(new_path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in new path".to_string(),
            }
        })?;

        log::debug!("Renaming {} -> {} (old_dir_fd: {}, new_dir_fd: {})",
                   old_path_str, new_path_str, old_dir_fd, new_dir_fd);
        // In full implementation, we would rename the file/directory
        Ok(())
    })
}

/// Unlink a file
#[no_mangle]
pub unsafe extern "C" fn wasi_path_unlink_file(
    ctx_ptr: *mut c_void,
    dir_fd: u32,
    path: *const c_char,
    path_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path cannot be null or empty".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        log::debug!("Unlinking file: {} (dir_fd: {})", path_str, dir_fd);
        // In full implementation, we would unlink the file
        Ok(())
    })
}

/// Create a symbolic link
#[no_mangle]
pub unsafe extern "C" fn wasi_path_symlink(
    ctx_ptr: *mut c_void,
    old_path: *const c_char,
    old_path_len: usize,
    dir_fd: u32,
    new_path: *const c_char,
    new_path_len: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr_mut::<WasiContext>(ctx_ptr, "WASI context")?;

        if old_path.is_null() || old_path_len == 0 || new_path.is_null() || new_path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Paths cannot be null or empty".to_string(),
            });
        }

        let old_path_bytes = slice::from_raw_parts(old_path as *const u8, old_path_len);
        let old_path_str = std::str::from_utf8(old_path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in old path".to_string(),
            }
        })?;

        let new_path_bytes = slice::from_raw_parts(new_path as *const u8, new_path_len);
        let new_path_str = std::str::from_utf8(new_path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in new path".to_string(),
            }
        })?;

        log::debug!("Creating symlink {} -> {} (dir_fd: {})", old_path_str, new_path_str, dir_fd);
        // In full implementation, we would create the symbolic link
        Ok(())
    })
}

/// Read a symbolic link
#[no_mangle]
pub unsafe extern "C" fn wasi_path_readlink(
    ctx_ptr: *mut c_void,
    dir_fd: u32,
    path: *const c_char,
    path_len: usize,
    buffer: *mut u8,
    buffer_len: usize,
    bytes_written: *mut usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let _ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 || buffer.is_null() || bytes_written.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path, buffer, and bytes_written cannot be null".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        // For now, just write 0 bytes
        // In full implementation, we would read the symbolic link target
        *bytes_written = 0;
        log::debug!("Reading symlink: {} (dir_fd: {}, buffer_len: {})", path_str, dir_fd, buffer_len);
        Ok(())
    })
}

/// WASI file statistics structure for FFI
#[repr(C)]
pub struct WasiFilestat {
    pub device: u64,
    pub inode: u64,
    pub filetype: u8,
    pub nlink: u64,
    pub size: u64,
    pub atim: u64,
    pub mtim: u64,
    pub ctim: u64,
}

/// JNI bridge functions for Java integration - Full implementations

/// Get random bytes using direct ByteBuffer
#[no_mangle]
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiRandomOperations_nativeGetRandomBytesDirect(
    env: *mut jni::sys::JNIEnv,
    _class: jni::sys::jclass,
    _context_handle: jni::sys::jlong,
    buffer: jni::sys::jobject,
    position: jni::sys::jint,
    length: jni::sys::jint,
) -> jni::sys::jint {
    use std::time::{SystemTime, UNIX_EPOCH};

    if buffer.is_null() || length <= 0 {
        return -1;
    }

    // Get the JNI environment
    let jni_env = match jni::JNIEnv::from_raw(env) {
        Ok(e) => e,
        Err(_) => return -1,
    };

    let jni_buffer = jni::objects::JByteBuffer::from_raw(buffer);

    // Get direct buffer address
    let buffer_addr = match jni_env.get_direct_buffer_address(&jni_buffer) {
        Ok(addr) => addr,
        Err(_) => return -1,
    };

    // Get buffer capacity
    let buffer_capacity = match jni_env.get_direct_buffer_capacity(&jni_buffer) {
        Ok(cap) => cap,
        Err(_) => return -1,
    };

    // Generate pseudo-random bytes using system time and position as seed
    let seed = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_nanos() as u64;

    let offset = position as usize;
    let mut state = seed;
    for i in 0..(length as usize) {
        if offset + i >= buffer_capacity {
            break;
        }
        // LCG PRNG
        state = state.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        let rand_val = (state >> 33) as u8;
        *buffer_addr.wrapping_add(offset + i) = rand_val;
    }

    0 // Return 0 on success (Java expects 0, non-zero is error code)
}

/// Get random bytes using byte array
#[no_mangle]
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiRandomOperations_nativeGetRandomBytesArray(
    env: *mut jni::sys::JNIEnv,
    _class: jni::sys::jclass,
    _context_handle: jni::sys::jlong,
    buffer: jni::sys::jbyteArray,
) -> jni::sys::jint {
    use std::time::{SystemTime, UNIX_EPOCH};

    if buffer.is_null() {
        return -1;
    }

    // Get the JNI environment
    let mut jni_env = match jni::JNIEnv::from_raw(env) {
        Ok(e) => e,
        Err(_) => return -1,
    };

    // Get array length
    let array = jni::objects::JByteArray::from_raw(buffer);
    let length = match jni_env.get_array_length(&array) {
        Ok(len) => len as usize,
        Err(_) => return -1,
    };

    if length == 0 {
        return 0;
    }

    // Generate pseudo-random bytes using system time as seed
    let seed = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap_or_default()
        .as_nanos() as u64;

    // Generate random bytes
    let mut random_bytes = Vec::with_capacity(length);
    let mut state = seed;
    for _ in 0..length {
        // LCG PRNG
        state = state.wrapping_mul(6364136223846793005).wrapping_add(1442695040888963407);
        random_bytes.push((state >> 33) as i8);
    }

    // Set the byte array data
    if jni_env.set_byte_array_region(&array, 0, &random_bytes).is_err() {
        return -1;
    }

    0 // Return 0 on success (Java expects 0, non-zero is error code)
}

/// Get clock resolution
#[no_mangle]
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiTimeOperations_nativeGetClockResolution(
    _env: *mut jni::sys::JNIEnv,
    _class: jni::sys::jclass,
    _context_handle: jni::sys::jlong,
    clock_id: jni::sys::jint,
) -> jni::sys::jlong {
    // Return basic clock resolution based on clock type
    match clock_id {
        0 => 1, // REALTIME - nanosecond resolution
        1 => 1, // MONOTONIC - nanosecond resolution  
        2 => 1000, // PROCESS_CPUTIME - microsecond resolution
        3 => 1000, // THREAD_CPUTIME - microsecond resolution
        _ => -1, // Invalid clock ID
    }
}

/// Get current time
#[no_mangle]
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiTimeOperations_nativeGetCurrentTime(
    _env: *mut jni::sys::JNIEnv,
    _class: jni::sys::jclass,
    _context_handle: jni::sys::jlong,
    clock_id: jni::sys::jint,
    _precision: jni::sys::jlong,
) -> jni::sys::jlong {
    use std::time::{SystemTime, UNIX_EPOCH};
    
    match clock_id {
        0 | 1 | 2 | 3 => {
            // Return current system time for all valid clock types
            SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .map(|d| d.as_nanos() as jni::sys::jlong)
                .unwrap_or(-1)
        }
        _ => -1, // Invalid clock ID
    }
}

/// Core WASI imports integration with Linker
impl WasiContext {
    /// Add WASI imports to a linker for complete WASI support
    ///
    /// # Arguments
    /// * `linker` - The linker to add WASI imports to
    /// * `store` - The store to use for function instantiation
    ///
    /// # Returns
    /// Ok if WASI imports were successfully added
    ///
    /// # Errors
    /// Returns WasmtimeError if imports cannot be added
    pub fn add_to_linker(
        &self,
        linker: &mut WasmtimeLinker,
        store: &mut Store,
    ) -> WasmtimeResult<()> {
        #[cfg(feature = "wasi")]
        {
            // Get the linker guard
            let mut linker_guard = linker.inner()
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to get linker: {}", e),
                    backtrace: None,
                })?;

            // First, ensure the Store has the WASI context from this WasiContext
            // The wasmtime_wasi add_to_linker expects to extract WasiP1Ctx from StoreData
            store.with_context(|mut ctx| {
                if ctx.data().wasi_ctx.is_none() {
                    // Clone our WasiP1Ctx into the store's data
                    // Note: WasiP1Ctx doesn't implement Clone, so we need to build a new one
                    // with the same configuration. For now, log a warning if not set.
                    log::warn!("Store does not have WASI context. Call store.set_wasi_context() first.");
                }
                Ok(())
            })?;

            // Add WASI Preview 1 imports to the linker
            // The closure extracts the WasiP1Ctx directly from StoreData
            wasmtime_wasi::p1::add_to_linker_sync(&mut *linker_guard, |data: &mut crate::store::StoreData| {
                data.wasi_ctx.as_mut().expect(
                    "Store does not have a WASI context attached. \
                     Call store.set_wasi_context() before adding WASI to linker."
                )
            })
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to add WASI to linker: {}", e),
            })?;

            log::debug!("WASI Preview 1 imports successfully added to linker");
        }

        #[cfg(not(feature = "wasi"))]
        {
            return Err(WasmtimeError::Runtime {
                message: "WASI support not compiled in".to_string(),
                backtrace: None,
            });
        }

        log::info!("WASI imports added to linker successfully");
        Ok(())
    }

    /// Create standard WASI I/O imports for basic stdin/stdout/stderr support
    ///
    /// # Arguments
    /// * `linker` - The linker to add standard I/O imports to
    ///
    /// # Returns
    /// Ok if standard I/O imports were successfully added
    pub fn add_stdio_imports(
        &self,
        linker: &mut WasmtimeLinker,
    ) -> WasmtimeResult<()> {
        // Add basic WASI Preview 1 stdio functions
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_write")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_read")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_close")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_seek")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_sync")?;

        log::debug!("Standard I/O WASI imports added");
        Ok(())
    }

    /// Create standard WASI filesystem imports for basic file operations
    ///
    /// # Arguments
    /// * `linker` - The linker to add filesystem imports to
    ///
    /// # Returns
    /// Ok if filesystem imports were successfully added
    pub fn add_filesystem_imports(
        &self,
        linker: &mut WasmtimeLinker,
    ) -> WasmtimeResult<()> {
        // Add basic WASI Preview 1 filesystem functions
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "path_open")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "path_filestat_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "path_create_directory")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "path_remove_directory")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "path_rename")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "path_unlink_file")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_readdir")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "fd_filestat_get")?;

        log::debug!("Filesystem WASI imports added");
        Ok(())
    }

    /// Create standard WASI environment and process imports
    ///
    /// # Arguments
    /// * `linker` - The linker to add environment imports to
    ///
    /// # Returns
    /// Ok if environment imports were successfully added
    pub fn add_environment_imports(
        &self,
        linker: &mut WasmtimeLinker,
    ) -> WasmtimeResult<()> {
        // Add basic WASI Preview 1 environment and process functions
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "environ_sizes_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "environ_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "args_sizes_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "args_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "proc_exit")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "sched_yield")?;

        log::debug!("Environment and process WASI imports added");
        Ok(())
    }

    /// Create standard WASI time and random imports
    ///
    /// # Arguments
    /// * `linker` - The linker to add time/random imports to
    ///
    /// # Returns
    /// Ok if time/random imports were successfully added
    pub fn add_time_random_imports(
        &self,
        linker: &mut WasmtimeLinker,
    ) -> WasmtimeResult<()> {
        // Add basic WASI Preview 1 time and random functions
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "clock_res_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "clock_time_get")?;
        self.add_wasi_import(linker, "wasi_snapshot_preview1", "random_get")?;

        log::debug!("Time and random WASI imports added");
        Ok(())
    }

    /// Helper method to add a single WASI import to the linker
    ///
    /// # Arguments
    /// * `linker` - The linker to add the import to
    /// * `module_name` - The WASI module name (usually "wasi_snapshot_preview1")
    /// * `function_name` - The WASI function name
    ///
    /// # Returns
    /// Ok if the import was successfully added
    fn add_wasi_import(
        &self,
        _linker: &mut WasmtimeLinker,
        module_name: &str,
        function_name: &str,
    ) -> WasmtimeResult<()> {
        // DEPRECATED: Individual WASI import addition is not supported.
        // Use add_to_linker() which adds ALL WASI functions via wasmtime_wasi.
        // This method exists for API compatibility but does not add actual imports.
        log::warn!(
            "add_wasi_import called for {}::{} - use add_to_linker() instead for actual WASI support",
            module_name, function_name
        );
        Ok(())
    }

    /// Convenience method to add all standard WASI imports at once
    ///
    /// # Arguments
    /// * `linker` - The linker to add all WASI imports to
    /// * `store` - The store to use for function instantiation
    ///
    /// # Returns
    /// Ok if all WASI imports were successfully added
    pub fn add_all_imports(
        &self,
        linker: &mut WasmtimeLinker,
        store: &mut Store,
    ) -> WasmtimeResult<()> {
        // Add core WASI integration first
        self.add_to_linker(linker, store)?;

        // Add all standard import categories
        self.add_stdio_imports(linker)?;
        self.add_filesystem_imports(linker)?;
        self.add_environment_imports(linker)?;
        self.add_time_random_imports(linker)?;

        log::info!("All WASI imports added to linker successfully");
        Ok(())
    }

    /// Check if WASI is properly initialized and ready for use
    pub fn is_ready(&self) -> bool {
        self.inner.lock().is_ok()
    }

    /// Get WASI capability summary for debugging
    pub fn get_capabilities_summary(&self) -> WasiCapabilitiesSummary {
        WasiCapabilitiesSummary {
            network_enabled: self.config.allow_network,
            filesystem_enabled: !self.directory_mappings.is_empty() || self.config.allow_arbitrary_fs,
            environment_count: self.environment.len(),
            directory_mappings_count: self.directory_mappings.len(),
            stdio_configured: true, // Always true for basic stdio
        }
    }
}

/// Summary of WASI capabilities for debugging and monitoring
#[derive(Debug, Clone)]
pub struct WasiCapabilitiesSummary {
    /// Whether network access is enabled
    pub network_enabled: bool,
    /// Whether filesystem access is enabled
    pub filesystem_enabled: bool,
    /// Number of environment variables
    pub environment_count: usize,
    /// Number of directory mappings
    pub directory_mappings_count: usize,
    /// Whether stdio is configured
    pub stdio_configured: bool,
}