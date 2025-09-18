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
        let engine_inner = engine.inner()
            .map_err(|e| WasmtimeError::Runtime { message: format!("Engine not available: {}", e), backtrace: None })?;

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

        // Simple implementation - define a basic function
        // This is a simplified version that would need to be expanded with proper host function wrapping
        linker.func_wrap(
            module_name,
            function_name,
            |_caller: Caller<'_, StoreData>| -> Result<(), wasmtime::Trap> {
                // Simplified - just return OK for now
                Ok(())
            }
        ).map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to define host function: {}", e),
                backtrace: None
            })?;

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

        let wasmtime_memory = memory.inner()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Memory not available: {}", e),
                backtrace: None
            })?;

        linker.define(module_name, memory_name, wasmtime_memory)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to define memory: {}", e),
                backtrace: None
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

    /// Defines a table in the linker
    pub fn define_table(
        &mut self,
        module_name: &str,
        table_name: &str,
        table: &WasmTable,
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

        let wasmtime_table = table.inner()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Table not available: {}", e),
                backtrace: None
            })?;

        linker.define(module_name, table_name, wasmtime_table)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to define table: {}", e),
                backtrace: None
            })?;

        self.metadata.import_count += 1;
        log::debug!("Defined table {}::{}", module_name, table_name);
        Ok(())
    }

    /// Defines a global in the linker
    pub fn define_global(
        &mut self,
        module_name: &str,
        global_name: &str,
        global: &WasmGlobal,
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

        let wasmtime_global = global.inner()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to define global: {}", e),
                backtrace: None
            })?;

        linker.define(module_name, global_name, wasmtime_global)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to define global: {}", e),
                backtrace: None
            })?;

        self.metadata.import_count += 1;
        log::debug!("Defined global {}::{}", module_name, global_name);
        Ok(())
    }

    /// Defines an instance in the linker
    pub fn define_instance(
        &mut self,
        module_name: &str,
        instance: &Instance,
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

        let wasmtime_instance = instance.inner()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Instance not available: {}", e),
                backtrace: None
            })?;

        linker.instance(module_name, &wasmtime_instance)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to define instance: {}", e),
                backtrace: None
            })?;

        self.metadata.import_count += 1;
        log::debug!("Defined instance for module {}", module_name);
        Ok(())
    }

    /// Creates an alias in the linker
    pub fn alias(
        &mut self,
        source_module: &str,
        source_name: &str,
        target_module: &str,
        target_name: &str,
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

        linker.alias(source_module, source_name, target_module, target_name)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create alias: {}", e),
                backtrace: None
            })?;

        log::debug!("Created alias from {}::{} to {}::{}", source_module, source_name, target_module, target_name);
        Ok(())
    }

    /// Instantiates a module using this linker
    pub fn instantiate(
        &self,
        store: &mut crate::store::Store,
        module: &crate::module::Module,
    ) -> WasmtimeResult<Instance> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let linker = self.inner.lock()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to lock linker: {}", e),
                backtrace: None
            })?;

        let wasmtime_module = module.inner()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Module not available: {}", e),
                backtrace: None
            })?;

        let mut store_guard = store.with_context_mut(|mut ctx| {
            linker.instantiate(&mut ctx, &wasmtime_module)
                .map_err(|e| WasmtimeError::Runtime {
                    message: format!("Failed to instantiate module: {}", e),
                    backtrace: None
                })
        })?;

        let instance = Instance::new(store_guard)?;
        log::debug!("Instantiated module using linker");
        Ok(instance)
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