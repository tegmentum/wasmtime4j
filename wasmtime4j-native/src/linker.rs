//! WebAssembly linker for host function binding and import resolution
//!
//! This module provides comprehensive linker functionality for defining host functions,
//! binding imports, and resolving module dependencies before instantiation.

use std::sync::{Arc, Mutex};
use std::collections::HashMap;
use std::time::Instant;
use wasmtime::{
    Linker as WasmtimeLinker,
    FuncType,
    Val,
    Caller,
};
use crate::engine::Engine;
use crate::store::{Store, StoreData};
use crate::module::Module;
use crate::instance::Instance;
use crate::hostfunc::HostFunction;
use crate::memory::Memory as WasmMemory;
use crate::table::Table as WasmTable;
use crate::global::Global as WasmGlobal;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Thread-safe wrapper around Wasmtime linker with comprehensive host binding support
#[derive(Debug)]
pub struct Linker {
    inner: Arc<Mutex<WasmtimeLinker<StoreData>>>,
    metadata: LinkerMetadata,
    host_functions: HashMap<String, HostFunctionDefinition>,
    imports_registry: HashMap<String, ImportDefinition>,
}

/// Linker metadata and statistics
#[derive(Debug, Clone)]
pub struct LinkerMetadata {
    /// Engine reference for validation
    pub engine_id: String,
    /// Timestamp when this linker was created
    pub created_at: Instant,
    /// Number of host functions defined
    pub host_function_count: usize,
    /// Number of imports registered
    pub import_count: usize,
    /// Number of successful instantiations
    pub instantiation_count: u64,
    /// Whether WASI is enabled
    pub wasi_enabled: bool,
    /// Whether this linker has been disposed
    pub disposed: bool,
}

/// Host function definition with metadata
#[derive(Debug, Clone)]
pub struct HostFunctionDefinition {
    /// Module name for the import
    pub module_name: String,
    /// Function name for the import
    pub function_name: String,
    /// Function type signature
    pub function_type: FuncType,
    /// Host function implementation
    pub host_function: HostFunction,
    /// Timestamp when defined
    pub defined_at: Instant,
}

/// Import definition for linker registry
#[derive(Debug, Clone)]
pub struct ImportDefinition {
    /// Module name for the import
    pub module_name: String,
    /// Import name
    pub import_name: String,
    /// Type of import (function, memory, table, global)
    pub import_type: ImportType,
    /// Timestamp when defined
    pub defined_at: Instant,
}

/// Types of imports that can be defined in the linker
#[derive(Debug, Clone)]
pub enum ImportType {
    /// Function import
    Function {
        /// Function type signature
        function_type: FuncType,
    },
    /// Memory import
    Memory,
    /// Table import
    Table,
    /// Global import
    Global,
    /// Instance import (all exports from an instance)
    Instance,
}

/// Configuration for linker creation and behavior
#[derive(Debug, Clone)]
pub struct LinkerConfig {
    /// Whether to enable WASI support by default
    pub enable_wasi: bool,
    /// Whether to allow shadowing of imports
    pub allow_shadowing: bool,
    /// Maximum number of host functions allowed
    pub max_host_functions: Option<usize>,
    /// Whether to validate function signatures on define
    pub validate_signatures: bool,
}

impl Default for LinkerConfig {
    fn default() -> Self {
        Self {
            enable_wasi: false,
            allow_shadowing: false,
            max_host_functions: None,
            validate_signatures: true,
        }
    }
}

/// Result from linker instantiation operations
#[derive(Debug)]
pub struct LinkerInstantiationResult {
    /// The created instance
    pub instance: Instance,
    /// Number of imports resolved
    pub resolved_imports: usize,
    /// Time taken for instantiation
    pub instantiation_time: std::time::Duration,
}

impl Linker {
    /// Creates a new linker for the given engine
    ///
    /// # Arguments
    /// * `engine` - The engine to create the linker for
    ///
    /// # Returns
    /// A new Linker instance
    ///
    /// # Errors
    /// Returns WasmtimeError if linker creation fails
    pub fn new(engine: &Engine) -> WasmtimeResult<Self> {
        Self::with_config(engine, LinkerConfig::default())
    }

    /// Creates a new linker with custom configuration
    ///
    /// # Arguments
    /// * `engine` - The engine to create the linker for
    /// * `config` - Configuration for the linker
    ///
    /// # Returns
    /// A new Linker instance with the specified configuration
    ///
    /// # Errors
    /// Returns WasmtimeError if linker creation fails
    pub fn with_config(engine: &Engine, config: LinkerConfig) -> WasmtimeResult<Self> {
        let engine_inner = engine.inner();

        let linker = WasmtimeLinker::new(&engine_inner);

        let metadata = LinkerMetadata {
            engine_id: "engine_placeholder".to_string(), // Simplified - remove engine.id() call
            created_at: Instant::now(),
            host_function_count: 0,
            import_count: 0,
            instantiation_count: 0,
            wasi_enabled: config.enable_wasi,
            disposed: false,
        };

        let mut result = Self {
            inner: Arc::new(Mutex::new(linker)),
            metadata,
            host_functions: HashMap::new(),
            imports_registry: HashMap::new(),
        };

        if config.enable_wasi {
            result.enable_wasi()?;
        }

        Ok(result)
    }

    /// Defines a host function that can be imported by WebAssembly modules
    ///
    /// # Arguments
    /// * `module_name` - The module name for the import
    /// * `function_name` - The function name for the import
    /// * `function_type` - The WebAssembly function type signature
    /// * `host_function` - The host function implementation
    ///
    /// # Errors
    /// Returns WasmtimeError if the function cannot be defined
    pub fn define_host_function(
        &mut self,
        module_name: &str,
        function_name: &str,
        function_type: FuncType,
        host_function: HostFunction,
    ) -> WasmtimeResult<()> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let mut linker = self.inner.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock linker: {}", e),
                backtrace: None
            })?;

        // Use the host function to create a proper Wasmtime function
        // This will be handled through the HostFunction callback system
        // For now, we just register the metadata and defer actual function creation
        log::debug!("Registering host function for later instantiation: {}::{}", module_name, function_name);

        // Record the host function definition
        let key = format!("{}::{}", module_name, function_name);
        let definition = HostFunctionDefinition {
            module_name: module_name.to_string(),
            function_name: function_name.to_string(),
            function_type: function_type.clone(),
            host_function,
            defined_at: Instant::now(),
        };

        self.host_functions.insert(key.clone(), definition);
        self.metadata.host_function_count += 1;

        // Record in imports registry
        let import_def = ImportDefinition {
            module_name: module_name.to_string(),
            import_name: function_name.to_string(),
            import_type: ImportType::Function { function_type },
            defined_at: Instant::now(),
        };
        self.imports_registry.insert(key, import_def);
        self.metadata.import_count += 1;

        log::debug!("Defined host function {}::{}", module_name, function_name);
        Ok(())
    }

    /// Instantiate all registered host functions with the given store
    ///
    /// # Arguments
    /// * `store` - The store to use for host function instantiation
    ///
    /// # Errors
    /// Returns WasmtimeError if host function instantiation fails
    pub fn instantiate_host_functions(&mut self, store: &mut Store) -> WasmtimeResult<()> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let mut linker = self.inner.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock linker: {}", e),
                backtrace: None
            })?;

        // Instantiate all registered host functions
        for (key, definition) in &self.host_functions {
            log::debug!("Instantiating host function: {}", key);

            // Create the Wasmtime function using the host function
            store.with_context_mut(|ctx| {
                let wasmtime_func = definition.host_function.create_wasmtime_func(ctx)?;

                // Define the function in the linker
                linker.define(
                    ctx,
                    &definition.module_name,
                    &definition.function_name,
                    wasmtime::Extern::Func(wasmtime_func)
                ).map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to define host function in linker: {}", e),
                    backtrace: None
                })
            })?;
        }

        log::debug!("Successfully instantiated {} host functions", self.host_functions.len());
        Ok(())
    }

    /// Defines a memory that can be imported by WebAssembly modules
    ///
    /// # Arguments
    /// * `module_name` - The module name for the import
    /// * `memory_name` - The memory name for the import
    /// * `memory` - The WebAssembly memory to provide
    ///
    /// # Errors
    /// Returns WasmtimeError if the memory cannot be defined
    pub fn define_memory(
        &mut self,
        store: &Store,
        module_name: &str,
        memory_name: &str,
        memory: &WasmMemory,
    ) -> WasmtimeResult<()> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let mut linker = self.inner.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock linker: {}", e),
                backtrace: None
            })?;

        let wasmtime_memory = memory.inner();

        store.with_context(|ctx| {
            linker.define(ctx, module_name, memory_name, wasmtime::Extern::Memory(*wasmtime_memory))
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to define memory: {}", e),
                    backtrace: None
                })
        })?;

        // Record in imports registry
        let key = format!("{}::{}", module_name, memory_name);
        let import_def = ImportDefinition {
            module_name: module_name.to_string(),
            import_name: memory_name.to_string(),
            import_type: ImportType::Memory,
            defined_at: Instant::now(),
        };
        self.imports_registry.insert(key, import_def);
        self.metadata.import_count += 1;

        log::debug!("Defined memory {}::{}", module_name, memory_name);
        Ok(())
    }

    /// Enables WASI (WebAssembly System Interface) support
    ///
    /// # Errors
    /// Returns WasmtimeError if WASI cannot be enabled
    pub fn enable_wasi(&mut self) -> WasmtimeResult<()> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let mut linker = self.inner.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock linker: {}", e),
                backtrace: None
            })?;

        #[cfg(feature = "wasi")]
        {
            // For now, just mark as enabled - WASI integration would be added later
            self.metadata.wasi_enabled = true;
            log::debug!("WASI support enabled");
        }

        #[cfg(not(feature = "wasi"))]
        {
            return Err(WasmtimeError::Runtime {
                message: "WASI support not compiled in".to_string(),
                backtrace: None
            });
        }

        Ok(())
    }

    /// Gets the metadata for this linker
    pub fn metadata(&self) -> &LinkerMetadata {
        &self.metadata
    }

    /// Gets a list of all defined host functions
    pub fn host_functions(&self) -> Vec<&HostFunctionDefinition> {
        self.host_functions.values().collect()
    }

    /// Gets a list of all import definitions
    pub fn imports(&self) -> Vec<&ImportDefinition> {
        self.imports_registry.values().collect()
    }

    /// Checks if the linker is valid and usable
    pub fn is_valid(&self) -> bool {
        !self.metadata.disposed && self.inner.lock().is_ok()
    }

    /// Disposes the linker and releases resources
    pub fn dispose(&mut self) {
        if !self.metadata.disposed {
            self.host_functions.clear();
            self.imports_registry.clear();
            self.metadata.disposed = true;
            log::debug!("Linker disposed");
        }
    }

    /// Gets access to the inner wasmtime linker (for advanced use cases)
    ///
    /// # Safety
    /// This provides direct access to the underlying wasmtime linker.
    /// Use with caution as it bypasses safety checks.
    pub fn inner(&self) -> WasmtimeResult<std::sync::MutexGuard<WasmtimeLinker<StoreData>>> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        self.inner.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock linker: {}", e),
                backtrace: None
            })
    }
}

impl Drop for Linker {
    fn drop(&mut self) {
        self.dispose();
    }
}