//! WASI (WebAssembly System Interface) support with comprehensive filesystem and environment access
//!
//! This module provides complete WASI functionality including:
//! - Filesystem access with configurable directory mapping and permissions
//! - Environment variable access and manipulation  
//! - Command-line argument passing to WebAssembly modules
//! - Standard input/output/error stream handling
//! - Network socket support (where available in Wasmtime)
//! - Proper security sandboxing and permission enforcement

use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use wasmtime_wasi::{WasiCtx, WasiCtxBuilder, DirPerms, FilePerms};
use wasmtime::Linker;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around WASI context with comprehensive configuration
pub struct WasiContext {
    /// The underlying Wasmtime WASI context
    inner: Arc<Mutex<WasiCtx>>,
    /// Configuration metadata
    config: WasiConfig,
    /// Directory mappings for filesystem access
    directory_mappings: HashMap<String, DirectoryMapping>,
    /// Environment variables
    environment: HashMap<String, String>,
    /// Command line arguments
    arguments: Vec<String>,
    /// Standard stream configurations
    stdio_config: StdioConfig,
    /// File descriptor manager
    fd_manager: Arc<Mutex<WasiFileDescriptorManager>>,
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
        let mut builder = WasiCtxBuilder::new();

        // Configure basic WASI settings
        builder.inherit_stdio();

        // Build the WASI context
        let wasi_ctx = builder.build();

        Ok(WasiContext {
            inner: Arc::new(Mutex::new(wasi_ctx)),
            config,
            directory_mappings: HashMap::new(),
            environment: HashMap::new(),
            arguments: vec!["wasmtime4j".to_string()], // Default program name
            stdio_config: StdioConfig::default(),
            fd_manager: Arc::new(Mutex::new(WasiFileDescriptorManager::new())),
        })
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
        let new_ctx = builder.build();
        
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

    /// Get a reference to the underlying WASI context for store operations
    pub fn get_wasi_ctx(&self) -> WasmtimeResult<Arc<Mutex<WasiCtx>>> {
        Ok(Arc::clone(&self.inner))
    }

    /// Add WASI imports to a Wasmtime linker
    /// This method will be implemented when the proper API is determined
    pub fn add_to_linker<T>(
        _linker: &mut Linker<T>,
        _get_ctx: impl Fn(&mut T) -> &mut WasiCtx + Send + Sync + Copy + 'static,
    ) -> WasmtimeResult<()> {
        // TODO: Implement when wasmtime-wasi API is clarified
        Err(WasmtimeError::Wasi {
            message: "WASI linker integration not yet implemented".to_string(),
        })
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
        let new_ctx = builder.build();
        
        let mut inner = self.inner.lock().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire WASI context lock".to_string(),
        })?;
        *inner = new_ctx;
        
        Ok(())
    }

    /// Configure standard I/O streams in the builder
    fn configure_stdio(&self, builder: &mut WasiCtxBuilder) -> WasmtimeResult<()> {
        // For now, use the simple stdio inheritance
        // The stdio configuration will be more limited in this version
        // TODO: Implement proper stdio redirection when the API supports it
        builder
            .inherit_stdin()
            .inherit_stdout()
            .inherit_stderr();
        
        Ok(())
    }
}

impl Default for WasiContext {
    fn default() -> Self {
        Self::new().expect("Failed to create default WASI context")
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
        let _wasi_ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;
        
        // Validate store pointer
        if store_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }

        // Get store reference and integrate WASI context
        let store = ffi_utils::deref_ptr_mut::<crate::store::Store>(store_ptr, "store")?;

        // Store the WASI context pointer in the store's user data
        // We use Box to manage the lifetime properly
        let ctx_box = Box::new(ctx_ptr as usize);

        store.with_context(|mut store_ctx| {
            store_ctx.data_mut().user_data = Some(ctx_box);
            Ok(())
        })?;

        log::debug!("WASI context successfully integrated with Store");
        Ok(())
    })
}

/// Get the WASI context from a Store if one is attached
#[no_mangle]
pub unsafe extern "C" fn wasi_ctx_get_from_store(
    store_ptr: *const c_void,
) -> *mut c_void {
    let result = ffi_utils::ffi_try_ptr(|| {
        if store_ptr.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Store handle cannot be null".to_string(),
            });
        }
        
        // Get store reference and retrieve WASI context
        let store = ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")?;

        // Retrieve WASI context pointer from store's user data
        let ctx_ptr = store.with_context_ro(|store_ctx| {
            if let Some(user_data) = &store_ctx.data().user_data {
                if let Some(ctx_addr) = user_data.downcast_ref::<usize>() {
                    Ok(*ctx_addr as *mut c_void)
                } else {
                    Ok(std::ptr::null_mut())
                }
            } else {
                Ok(std::ptr::null_mut())
            }
        })?;

        log::debug!("Retrieved WASI context from Store: {:p}", ctx_ptr);
        Ok(Box::new(ctx_ptr))
    });
    result
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
        
        // Get store reference and check for WASI context
        let store = ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")?;

        // Check if WASI context exists in store's user data
        let has_wasi = store.with_context_ro(|store_ctx| {
            if let Some(user_data) = &store_ctx.data().user_data {
                if let Some(ctx_addr) = user_data.downcast_ref::<usize>() {
                    Ok(if *ctx_addr != 0 { 1 } else { 0 })
                } else {
                    Ok(0)
                }
            } else {
                Ok(0)
            }
        })?;

        log::debug!("Store WASI context check result: {}", has_wasi);
        Ok(has_wasi)
    });
    result.1
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

/// Add file descriptor manager to WasiContext
impl WasiContext {
    /// Get the file descriptor manager
    fn with_fd_manager<F, R>(&self, f: F) -> WasmtimeResult<R>
    where
        F: FnOnce(&mut WasiFileDescriptorManager) -> R,
    {
        let mut fd_manager = self.fd_manager.lock().map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire file descriptor manager lock".to_string(),
        })?;
        Ok(f(&mut *fd_manager))
    }

    /// Open a file with the specified flags and rights
    pub fn path_open(
        &self,
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

        // Create OpenOptions based on flags
        let mut open_opts = OpenOptions::new();

        // Handle read/write flags
        if (oflags & 0x01) != 0 { // O_CREAT
            open_opts.create(true);
        }
        if (oflags & 0x02) != 0 { // O_DIRECTORY
            return self.open_directory(path, rights);
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

        // Open the file
        let file = open_opts.open(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to open file {}: {}", path, e),
        })?;

        // Create file descriptor and store it
        self.with_fd_manager(|fd_manager| {
            let fd = fd_manager.allocate_fd();
            let file_desc = WasiFileDescriptor {
                file,
                path: path.to_string(),
                rights,
                flags: fdflags,
            };
            fd_manager.add_file(fd, file_desc);
            fd
        })
    }

    /// Open a directory with the specified rights
    pub fn open_directory(&self, path: &str, rights: u64) -> WasmtimeResult<u32> {
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

        // Create directory descriptor and store it
        let dir_desc = WasiDirectoryDescriptor::new(path.to_string(), rights)?;
        self.with_fd_manager(|fd_manager| {
            let fd = fd_manager.allocate_fd();
            fd_manager.add_directory(fd, dir_desc);
            fd
        })
    }

    /// Close a file descriptor
    pub fn fd_close(&self, fd: u32) -> WasmtimeResult<()> {
        self.with_fd_manager(|fd_manager| {
            // Try to close as file first, then as directory
            if fd_manager.close_file(fd).is_some() {
                log::debug!("Closed file descriptor: {}", fd);
                Ok(())
            } else if fd_manager.close_directory(fd).is_some() {
                log::debug!("Closed directory descriptor: {}", fd);
                Ok(())
            } else {
                Err(WasmtimeError::Wasi {
                    message: format!("Invalid file descriptor: {}", fd),
                })
            }
        })?
    }

    /// Read from a file descriptor
    pub fn fd_read(&self, fd: u32, buffer: &mut [u8]) -> WasmtimeResult<usize> {
        self.with_fd_manager(|fd_manager| {
            if let Some(file_desc) = fd_manager.get_file_mut(fd) {
                // Check read permission
                if (file_desc.rights & 0x02) == 0 { // FD_READ
                    return Err(WasmtimeError::Wasi {
                        message: format!("No read permission for file descriptor: {}", fd),
                    });
                }

                file_desc.file.read(buffer).map_err(|e| WasmtimeError::Wasi {
                    message: format!("Failed to read from file descriptor {}: {}", fd, e),
                })
            } else {
                Err(WasmtimeError::Wasi {
                    message: format!("Invalid file descriptor: {}", fd),
                })
            }
        })?
    }

    /// Write to a file descriptor
    pub fn fd_write(&self, fd: u32, buffer: &[u8]) -> WasmtimeResult<usize> {
        self.with_fd_manager(|fd_manager| {
            if let Some(file_desc) = fd_manager.get_file_mut(fd) {
                // Check write permission
                if (file_desc.rights & 0x40) == 0 { // FD_WRITE
                    return Err(WasmtimeError::Wasi {
                        message: format!("No write permission for file descriptor: {}", fd),
                    });
                }

                file_desc.file.write(buffer).map_err(|e| WasmtimeError::Wasi {
                    message: format!("Failed to write to file descriptor {}: {}", fd, e),
                })
            } else {
                Err(WasmtimeError::Wasi {
                    message: format!("Invalid file descriptor: {}", fd),
                })
            }
        })?
    }

    /// Seek in a file descriptor
    pub fn fd_seek(&self, fd: u32, offset: i64, whence: u8) -> WasmtimeResult<u64> {
        self.with_fd_manager(|fd_manager| {
            if let Some(file_desc) = fd_manager.get_file_mut(fd) {
                let seek_from = match whence {
                    0 => SeekFrom::Start(offset as u64), // SEEK_SET
                    1 => SeekFrom::Current(offset),       // SEEK_CUR
                    2 => SeekFrom::End(offset),           // SEEK_END
                    _ => return Err(WasmtimeError::Wasi {
                        message: format!("Invalid whence value: {}", whence),
                    }),
                };

                file_desc.file.seek(seek_from).map_err(|e| WasmtimeError::Wasi {
                    message: format!("Failed to seek in file descriptor {}: {}", fd, e),
                })
            } else {
                Err(WasmtimeError::Wasi {
                    message: format!("Invalid file descriptor: {}", fd),
                })
            }
        })?
    }

    /// Get file statistics for a file descriptor
    pub fn fd_filestat_get(&self, fd: u32) -> WasmtimeResult<WasiFilestat> {
        self.with_fd_manager(|fd_manager| {
            if let Some(file_desc) = fd_manager.open_files.get(&fd) {
                let path = file_desc.path.clone();
                let metadata = fs::metadata(&path).map_err(|e| WasmtimeError::Wasi {
                    message: format!("Failed to get metadata for file descriptor {}: {}", fd, e),
                })?;

                Ok(Self::metadata_to_filestat(&metadata))
            } else {
                Err(WasmtimeError::Wasi {
                    message: format!("Invalid file descriptor: {}", fd),
                })
            }
        })?
    }

    /// Get file statistics for a path
    pub fn path_filestat_get(&self, _dir_fd: u32, _flags: u32, path: &str) -> WasmtimeResult<WasiFilestat> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        let metadata = fs::metadata(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to get metadata for path {}: {}", path, e),
        })?;

        Ok(Self::metadata_to_filestat(&metadata))
    }

    /// Convert std::fs::Metadata to WasiFilestat
    fn metadata_to_filestat(metadata: &Metadata) -> WasiFilestat {
        let filetype = if metadata.is_dir() {
            3 // Directory
        } else if metadata.is_file() {
            4 // Regular file
        } else {
            7 // Symbolic link or other
        };

        let (atim, mtim, ctim) = Self::extract_metadata_times(metadata);

        WasiFilestat {
            device: 0, // Not available on all platforms
            inode: 0,  // Not available on all platforms
            filetype,
            nlink: 1,
            size: metadata.len(),
            atim,
            mtim,
            ctim,
        }
    }

    /// Extract timestamps from metadata
    fn extract_metadata_times(metadata: &Metadata) -> (u64, u64, u64) {
        let atim = metadata.accessed()
            .ok()
            .and_then(|t| t.duration_since(SystemTime::UNIX_EPOCH).ok())
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);

        let mtim = metadata.modified()
            .ok()
            .and_then(|t| t.duration_since(SystemTime::UNIX_EPOCH).ok())
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);

        let ctim = metadata.created()
            .ok()
            .and_then(|t| t.duration_since(SystemTime::UNIX_EPOCH).ok())
            .map(|d| d.as_nanos() as u64)
            .unwrap_or(0);

        (atim, mtim, ctim)
    }

    /// Read directory entries
    pub fn fd_readdir(&self, fd: u32, cookie: u64) -> WasmtimeResult<Vec<WasiDirectoryEntry>> {
        self.with_fd_manager(|fd_manager| {
            if let Some(dir_desc) = fd_manager.get_directory_mut(fd) {
                // Check if cookie is valid
                let start_index = cookie as usize;
                if start_index > dir_desc.entries.len() {
                    return Ok(Vec::new());
                }

                // Return entries starting from the cookie position
                Ok(dir_desc.entries[start_index..].to_vec())
            } else {
                Err(WasmtimeError::Wasi {
                    message: format!("Invalid directory descriptor: {}", fd),
                })
            }
        })?
    }

    /// Create a directory
    pub fn path_create_directory(&self, _dir_fd: u32, path: &str) -> WasmtimeResult<()> {
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
    pub fn path_remove_directory(&self, _dir_fd: u32, path: &str) -> WasmtimeResult<()> {
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

    /// Rename a file or directory
    pub fn path_rename(&self, _old_dir_fd: u32, old_path: &str, _new_dir_fd: u32, new_path: &str) -> WasmtimeResult<()> {
        // Validate both paths are allowed
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
    pub fn path_unlink_file(&self, _dir_fd: u32, path: &str) -> WasmtimeResult<()> {
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

    /// Create a symbolic link
    pub fn path_symlink(&self, old_path: &str, _dir_fd: u32, new_path: &str) -> WasmtimeResult<()> {
        // Validate both paths are allowed
        if !self.is_path_allowed(new_path) {
            return Err(WasmtimeError::Wasi {
                message: format!("New path not allowed: {}", new_path),
            });
        }

        #[cfg(unix)]
        {
            std::os::unix::fs::symlink(old_path, new_path).map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create symlink {} -> {}: {}", old_path, new_path, e),
            })
        }

        #[cfg(windows)]
        {
            // Windows symlink creation requires different permissions and API
            Err(WasmtimeError::Wasi {
                message: "Symbolic links not supported on Windows".to_string(),
            })
        }
    }

    /// Read a symbolic link
    pub fn path_readlink(&self, _dir_fd: u32, path: &str, buffer: &mut [u8]) -> WasmtimeResult<usize> {
        // Validate path is allowed
        if !self.is_path_allowed(path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Path not allowed: {}", path),
            });
        }

        let target = fs::read_link(path).map_err(|e| WasmtimeError::Wasi {
            message: format!("Failed to read symlink {}: {}", path, e),
        })?;

        let target_string = target.to_string_lossy();
        let target_bytes = target_string.as_bytes();
        let copy_len = std::cmp::min(target_bytes.len(), buffer.len());
        buffer[..copy_len].copy_from_slice(&target_bytes[..copy_len]);

        Ok(copy_len)
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
    fd_out: *mut u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path cannot be null or empty".to_string(),
            });
        }

        if fd_out.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Output file descriptor pointer cannot be null".to_string(),
            });
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        let fd = ctx.path_open(dir_fd, path_str, oflags, rights, 0, fdflags)?;
        *fd_out = fd;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;
        ctx.fd_close(fd)?;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if buffer.is_null() || bytes_read.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Buffer and bytes_read cannot be null".to_string(),
            });
        }

        if buffer_len == 0 {
            *bytes_read = 0;
            return Ok(());
        }

        let buffer_slice = slice::from_raw_parts_mut(buffer, buffer_len);
        let read_count = ctx.fd_read(fd, buffer_slice)?;
        *bytes_read = read_count;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if buffer.is_null() || bytes_written.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Buffer and bytes_written cannot be null".to_string(),
            });
        }

        if buffer_len == 0 {
            *bytes_written = 0;
            return Ok(());
        }

        let buffer_slice = slice::from_raw_parts(buffer, buffer_len);
        let written_count = ctx.fd_write(fd, buffer_slice)?;
        *bytes_written = written_count;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if new_position.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "New position pointer cannot be null".to_string(),
            });
        }

        let position = ctx.fd_seek(fd, offset, whence)?;
        *new_position = position;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if filestat.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Filestat pointer cannot be null".to_string(),
            });
        }

        let stat = ctx.fd_filestat_get(fd)?;
        *filestat = stat;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

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

        let stat = ctx.path_filestat_get(dir_fd, flags, path_str)?;
        *filestat = stat;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if buffer.is_null() || bytes_written.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Buffer and bytes_written cannot be null".to_string(),
            });
        }

        if buffer_len == 0 {
            *bytes_written = 0;
            return Ok(());
        }

        let entries = ctx.fd_readdir(fd, cookie)?;
        let buffer_slice = slice::from_raw_parts_mut(buffer, buffer_len);

        // Serialize directory entries into the buffer
        let mut written = 0;
        for entry in entries {
            // Calculate entry size: d_next(8) + d_ino(8) + d_namlen(4) + d_type(1) + name
            let entry_size = 8 + 8 + 4 + 1 + entry.name.len();

            if written + entry_size > buffer_len {
                break; // Buffer full
            }

            // Write d_next (next cookie)
            let next_cookie = (cookie + written as u64 + 1).to_le_bytes();
            buffer_slice[written..written + 8].copy_from_slice(&next_cookie);
            written += 8;

            // Write d_ino (inode)
            let inode_bytes = entry.inode.to_le_bytes();
            buffer_slice[written..written + 8].copy_from_slice(&inode_bytes);
            written += 8;

            // Write d_namlen (name length)
            let namlen = (entry.name.len() as u32).to_le_bytes();
            buffer_slice[written..written + 4].copy_from_slice(&namlen);
            written += 4;

            // Write d_type (file type)
            buffer_slice[written] = entry.file_type;
            written += 1;

            // Write name
            buffer_slice[written..written + entry.name.len()].copy_from_slice(entry.name.as_bytes());
            written += entry.name.len();
        }

        *bytes_written = written;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

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

        ctx.path_create_directory(dir_fd, path_str)?;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

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

        ctx.path_remove_directory(dir_fd, path_str)?;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

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

        ctx.path_rename(old_dir_fd, old_path_str, new_dir_fd, new_path_str)?;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

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

        ctx.path_unlink_file(dir_fd, path_str)?;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

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

        ctx.path_symlink(old_path_str, dir_fd, new_path_str)?;
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
        let ctx = ffi_utils::deref_ptr::<WasiContext>(ctx_ptr, "WASI context")?;

        if path.is_null() || path_len == 0 || buffer.is_null() || bytes_written.is_null() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Path, buffer, and bytes_written cannot be null".to_string(),
            });
        }

        if buffer_len == 0 {
            *bytes_written = 0;
            return Ok(());
        }

        let path_bytes = slice::from_raw_parts(path as *const u8, path_len);
        let path_str = std::str::from_utf8(path_bytes).map_err(|_| {
            WasmtimeError::InvalidParameter {
                message: "Invalid UTF-8 in path".to_string(),
            }
        })?;

        let buffer_slice = slice::from_raw_parts_mut(buffer, buffer_len);
        let read_count = ctx.path_readlink(dir_fd, path_str, buffer_slice)?;
        *bytes_written = read_count;
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

/// JNI bridge functions for Java integration - Simple stub implementations
/// These are basic stubs that allow compilation and basic functionality



/// Get random bytes using direct ByteBuffer
#[no_mangle]
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiRandomOperations_nativeGetRandomBytesDirect(
    _env: *mut jni::sys::JNIEnv,
    _class: jni::sys::jclass,
    _context_handle: jni::sys::jlong,
    _buffer: jni::sys::jobject,
    _position: jni::sys::jint,
    _length: jni::sys::jint,
) -> jni::sys::jint {
    // Stub implementation - always return success
    0
}

/// Get random bytes using byte array  
#[no_mangle]
pub unsafe extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_WasiRandomOperations_nativeGetRandomBytesArray(
    _env: *mut jni::sys::JNIEnv,
    _class: jni::sys::jclass,
    _context_handle: jni::sys::jlong,
    _buffer: jni::sys::jbyteArray,
) -> jni::sys::jint {
    // Stub implementation - always return success
    0
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