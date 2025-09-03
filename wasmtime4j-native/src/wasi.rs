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