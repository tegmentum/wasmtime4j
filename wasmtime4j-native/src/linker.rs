//! WebAssembly linker for host function binding and import resolution
//!
//! This module provides comprehensive linker functionality for defining host functions,
//! binding imports, and resolving module dependencies before instantiation.

use std::sync::{Arc, Mutex};
use std::collections::{HashMap, HashSet, VecDeque};
use std::time::Instant;
use wasmtime::{
    Linker as WasmtimeLinker,
    FuncType,
    Val,
    Caller,
    ImportType as WasmtimeImportType,
    ExternType,
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

/// Dependency graph for module resolution
#[derive(Debug, Clone)]
pub struct DependencyGraph {
    /// Nodes in the graph (modules)
    pub nodes: Vec<DependencyNode>,
    /// Edges in the graph (dependencies)
    pub edges: Vec<DependencyEdge>,
    /// Whether the graph has been validated
    pub validated: bool,
    /// Topological sort order (if valid)
    pub topological_order: Vec<usize>,
    /// Circular dependency chains detected
    pub circular_chains: Vec<String>,
}

/// Node in the dependency graph representing a module
#[derive(Debug, Clone)]
pub struct DependencyNode {
    /// Index of this node
    pub index: usize,
    /// Module reference
    pub module: Module,
    /// Imports required by this module
    pub imports: Vec<ModuleImport>,
    /// Exports provided by this module
    pub exports: Vec<ModuleExport>,
    /// Whether this node has been visited during traversal
    pub visited: bool,
    /// Whether this node is currently being processed (for cycle detection)
    pub processing: bool,
}

/// Edge in the dependency graph representing a dependency relationship
#[derive(Debug, Clone)]
pub struct DependencyEdge {
    /// Index of the dependent node
    pub from_node: usize,
    /// Index of the dependency node
    pub to_node: usize,
    /// Import module name
    pub import_module: String,
    /// Import name
    pub import_name: String,
    /// Type of dependency
    pub dependency_type: DependencyType,
    /// Whether this dependency has been resolved
    pub resolved: bool,
}

/// Import required by a module
#[derive(Debug, Clone)]
pub struct ModuleImport {
    /// Module name for the import
    pub module_name: String,
    /// Import name
    pub import_name: String,
    /// Expected import type
    pub import_type: WasmtimeImportType,
    /// Whether this import is optional
    pub optional: bool,
}

/// Export provided by a module
#[derive(Debug, Clone)]
pub struct ModuleExport {
    /// Export name
    pub name: String,
    /// Export type
    pub export_type: ExternType,
}

/// Type of dependency between modules
#[derive(Debug, Clone, PartialEq)]
pub enum DependencyType {
    /// Function dependency
    Function,
    /// Memory dependency
    Memory,
    /// Table dependency
    Table,
    /// Global dependency
    Global,
    /// Instance dependency (all exports)
    Instance,
}

/// Issue found during import validation
#[derive(Debug, Clone)]
pub struct ImportValidationIssue {
    /// Severity of the issue
    pub severity: ImportIssueSeverity,
    /// Type of issue
    pub issue_type: ImportIssueType,
    /// Module name where the issue occurred
    pub module_name: String,
    /// Import name that has the issue
    pub import_name: String,
    /// Detailed description of the issue
    pub message: String,
    /// Expected type (if applicable)
    pub expected_type: Option<String>,
    /// Actual type found (if applicable)
    pub actual_type: Option<String>,
}

/// Severity levels for import validation issues
#[derive(Debug, Clone, PartialEq)]
pub enum ImportIssueSeverity {
    /// Informational issue
    Info,
    /// Warning that might cause issues
    Warning,
    /// Error that will prevent instantiation
    Error,
    /// Critical error
    Critical,
}

/// Types of import validation issues
#[derive(Debug, Clone, PartialEq)]
pub enum ImportIssueType {
    /// Import is missing from the linker
    MissingImport,
    /// Import type doesn't match
    TypeMismatch,
    /// Circular dependency detected
    CircularDependency,
    /// Import signature is incompatible
    SignatureMismatch,
    /// Module not found
    ModuleNotFound,
    /// Export not found
    ExportNotFound,
    /// Ambiguous import resolution
    AmbiguousImport,
    /// Validation failed for unknown reasons
    ValidationFailed,
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

    /// Resolves dependencies for a set of modules
    ///
    /// # Arguments
    /// * `modules` - The modules to analyze for dependencies
    ///
    /// # Returns
    /// A dependency graph with resolution information
    ///
    /// # Errors
    /// Returns WasmtimeError if dependency analysis fails
    pub fn resolve_dependencies(&self, modules: &[Module]) -> WasmtimeResult<DependencyGraph> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let start_time = Instant::now();
        log::debug!("Starting dependency resolution for {} modules", modules.len());

        // Build dependency graph
        let mut graph = self.build_dependency_graph(modules)?;

        // Detect circular dependencies
        let circular_chains = self.detect_circular_dependencies(&mut graph)?;
        graph.circular_chains = circular_chains;

        // Compute topological order if no circular dependencies
        if graph.circular_chains.is_empty() {
            graph.topological_order = self.compute_topological_order(&graph)?;
            graph.validated = true;
        }

        let elapsed = start_time.elapsed();
        log::debug!("Dependency resolution completed in {:?}", elapsed);

        Ok(graph)
    }

    /// Validates imports for a set of modules
    ///
    /// # Arguments
    /// * `modules` - The modules to validate imports for
    ///
    /// # Returns
    /// A list of validation issues found
    ///
    /// # Errors
    /// Returns WasmtimeError if validation fails
    pub fn validate_imports(&self, modules: &[Module]) -> WasmtimeResult<Vec<ImportValidationIssue>> {
        if self.metadata.disposed {
            return Err(WasmtimeError::Runtime {
                message: "Linker has been disposed".to_string(),
                backtrace: None
            });
        }

        let start_time = Instant::now();
        log::debug!("Starting import validation for {} modules", modules.len());

        let mut issues = Vec::new();

        // Check each module's imports
        for module in modules {
            let module_issues = self.validate_module_imports(module)?;
            issues.extend(module_issues);
        }

        let elapsed = start_time.elapsed();
        log::debug!("Import validation completed in {:?}, found {} issues", elapsed, issues.len());

        Ok(issues)
    }

    /// Checks if a specific import is defined in this linker
    ///
    /// # Arguments
    /// * `module_name` - The module name for the import
    /// * `import_name` - The import name
    ///
    /// # Returns
    /// True if the import is defined, false otherwise
    pub fn has_import(&self, module_name: &str, import_name: &str) -> bool {
        let key = format!("{}::{}", module_name, import_name);
        self.imports_registry.contains_key(&key)
    }

    /// Builds a dependency graph for the given modules
    fn build_dependency_graph(&self, modules: &[Module]) -> WasmtimeResult<DependencyGraph> {
        let mut graph = DependencyGraph {
            nodes: Vec::new(),
            edges: Vec::new(),
            validated: false,
            topological_order: Vec::new(),
            circular_chains: Vec::new(),
        };

        // Create nodes for each module
        for (index, module) in modules.iter().enumerate() {
            let imports = self.extract_module_imports(module)?;
            let exports = self.extract_module_exports(module)?;

            let node = DependencyNode {
                index,
                module: module.clone(),
                imports,
                exports,
                visited: false,
                processing: false,
            };

            graph.nodes.push(node);
        }

        // Create edges based on import/export relationships
        for (from_idx, from_node) in graph.nodes.iter().enumerate() {
            for import in &from_node.imports {
                // Find which node exports this import
                for (to_idx, to_node) in graph.nodes.iter().enumerate() {
                    if from_idx == to_idx {
                        continue; // Skip self-references
                    }

                    if self.node_exports_import(&to_node, &import.module_name, &import.import_name) {
                        let edge = DependencyEdge {
                            from_node: from_idx,
                            to_node: to_idx,
                            import_module: import.module_name.clone(),
                            import_name: import.import_name.clone(),
                            dependency_type: self.wasmtime_type_to_dependency_type(&import.import_type),
                            resolved: true,
                        };
                        graph.edges.push(edge);
                    }
                }
            }
        }

        Ok(graph)
    }

    /// Detects circular dependencies in the graph
    fn detect_circular_dependencies(&self, graph: &mut DependencyGraph) -> WasmtimeResult<Vec<String>> {
        let mut circular_chains = Vec::new();
        let mut stack = Vec::new();

        // Reset visit state
        for node in &mut graph.nodes {
            node.visited = false;
            node.processing = false;
        }

        // DFS from each unvisited node
        for i in 0..graph.nodes.len() {
            if !graph.nodes[i].visited {
                if let Some(cycle) = self.dfs_detect_cycle(graph, i, &mut stack)? {
                    circular_chains.push(cycle);
                }
            }
        }

        Ok(circular_chains)
    }

    /// Performs DFS to detect cycles
    fn dfs_detect_cycle(&self, graph: &mut DependencyGraph, node_idx: usize, stack: &mut Vec<usize>) -> WasmtimeResult<Option<String>> {
        graph.nodes[node_idx].visited = true;
        graph.nodes[node_idx].processing = true;
        stack.push(node_idx);

        // Check all outgoing edges
        for edge in &graph.edges {
            if edge.from_node == node_idx {
                let next_idx = edge.to_node;

                if graph.nodes[next_idx].processing {
                    // Found a cycle
                    let cycle_start = stack.iter().position(|&x| x == next_idx).unwrap();
                    let cycle_nodes: Vec<String> = stack[cycle_start..]
                        .iter()
                        .map(|&idx| graph.nodes[idx].module.name().unwrap_or_else(|| format!("module_{}", idx)))
                        .collect();

                    // Add the first node again to complete the cycle
                    let mut cycle_description = cycle_nodes.join(" -> ");
                    cycle_description.push_str(" -> ");
                    cycle_description.push_str(&cycle_nodes[0]);

                    stack.pop();
                    graph.nodes[node_idx].processing = false;
                    return Ok(Some(cycle_description));
                }

                if !graph.nodes[next_idx].visited {
                    if let Some(cycle) = self.dfs_detect_cycle(graph, next_idx, stack)? {
                        stack.pop();
                        graph.nodes[node_idx].processing = false;
                        return Ok(Some(cycle));
                    }
                }
            }
        }

        stack.pop();
        graph.nodes[node_idx].processing = false;
        Ok(None)
    }

    /// Computes topological order for the dependency graph
    fn compute_topological_order(&self, graph: &DependencyGraph) -> WasmtimeResult<Vec<usize>> {
        let mut in_degree = vec![0; graph.nodes.len()];

        // Calculate in-degrees
        for edge in &graph.edges {
            in_degree[edge.to_node] += 1;
        }

        // Find nodes with no incoming edges
        let mut queue = VecDeque::new();
        for (i, &degree) in in_degree.iter().enumerate() {
            if degree == 0 {
                queue.push_back(i);
            }
        }

        let mut result = Vec::new();

        // Process nodes in topological order
        while let Some(node_idx) = queue.pop_front() {
            result.push(node_idx);

            // Remove edges from this node
            for edge in &graph.edges {
                if edge.from_node == node_idx {
                    in_degree[edge.to_node] -= 1;
                    if in_degree[edge.to_node] == 0 {
                        queue.push_back(edge.to_node);
                    }
                }
            }
        }

        if result.len() != graph.nodes.len() {
            return Err(WasmtimeError::Runtime {
                message: "Cannot compute topological order: circular dependencies detected".to_string(),
                backtrace: None
            });
        }

        Ok(result)
    }

    /// Extracts imports from a module
    fn extract_module_imports(&self, module: &Module) -> WasmtimeResult<Vec<ModuleImport>> {
        let wasmtime_module = module.inner();
        let mut imports = Vec::new();

        for import in wasmtime_module.imports() {
            let module_import = ModuleImport {
                module_name: import.module().to_string(),
                import_name: import.name().to_string(),
                import_type: import.ty().clone(),
                optional: false, // WebAssembly imports are generally not optional
            };
            imports.push(module_import);
        }

        Ok(imports)
    }

    /// Extracts exports from a module
    fn extract_module_exports(&self, module: &Module) -> WasmtimeResult<Vec<ModuleExport>> {
        let wasmtime_module = module.inner();
        let mut exports = Vec::new();

        for export in wasmtime_module.exports() {
            let module_export = ModuleExport {
                name: export.name().to_string(),
                export_type: export.ty().clone(),
            };
            exports.push(module_export);
        }

        Ok(exports)
    }

    /// Checks if a node exports a specific import
    fn node_exports_import(&self, node: &DependencyNode, module_name: &str, import_name: &str) -> bool {
        // For now, assume the module name matches the node's module name
        // In a more sophisticated implementation, this would handle module aliasing
        if let Some(node_module_name) = node.module.name() {
            if node_module_name == module_name {
                return node.exports.iter().any(|export| export.name == import_name);
            }
        }

        false
    }

    /// Converts Wasmtime import type to dependency type
    fn wasmtime_type_to_dependency_type(&self, import_type: &WasmtimeImportType) -> DependencyType {
        match import_type {
            WasmtimeImportType::Func(_) => DependencyType::Function,
            WasmtimeImportType::Memory(_) => DependencyType::Memory,
            WasmtimeImportType::Table(_) => DependencyType::Table,
            WasmtimeImportType::Global(_) => DependencyType::Global,
        }
    }

    /// Validates imports for a single module
    fn validate_module_imports(&self, module: &Module) -> WasmtimeResult<Vec<ImportValidationIssue>> {
        let mut issues = Vec::new();
        let wasmtime_module = module.inner();

        for import in wasmtime_module.imports() {
            let module_name = import.module();
            let import_name = import.name();

            // Check if the import is available in the linker
            if !self.has_import(module_name, import_name) {
                issues.push(ImportValidationIssue {
                    severity: ImportIssueSeverity::Error,
                    issue_type: ImportIssueType::MissingImport,
                    module_name: module_name.to_string(),
                    import_name: import_name.to_string(),
                    message: format!("Import {}::{} is not available in the linker", module_name, import_name),
                    expected_type: Some(format!("{:?}", import.ty())),
                    actual_type: None,
                });
                continue;
            }

            // Type compatibility check would go here
            // For now, we assume types are compatible if the import exists
        }

        Ok(issues)
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

//
// Native C exports for JNI and Panama FFI consumption
//

use std::os::raw::{c_void, c_char, c_int};
use std::ffi::{CStr, CString};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Linker core functions for interface implementations
pub mod ffi_core {
    use super::*;
    use std::os::raw::c_void;
    use crate::error::ffi_utils;
    use crate::validate_ptr_not_null;

    /// Core function to create linker with engine
    pub fn create_linker(engine: &Engine) -> WasmtimeResult<Box<Linker>> {
        Linker::new(engine).map(Box::new)
    }

    /// Core function to create linker with configuration
    pub fn create_linker_with_config(engine: &Engine, config: LinkerConfig) -> WasmtimeResult<Box<Linker>> {
        Linker::with_config(engine, config).map(Box::new)
    }

    /// Core function to validate linker pointer and get reference
    pub unsafe fn get_linker_ref(linker_ptr: *const c_void) -> WasmtimeResult<&'static Linker> {
        validate_ptr_not_null!(linker_ptr, "linker");
        Ok(&*(linker_ptr as *const Linker))
    }

    /// Core function to validate linker pointer and get mutable reference
    pub unsafe fn get_linker_mut(linker_ptr: *mut c_void) -> WasmtimeResult<&'static mut Linker> {
        validate_ptr_not_null!(linker_ptr, "linker");
        Ok(&mut *(linker_ptr as *mut Linker))
    }

    /// Core function to destroy a linker (safe cleanup)
    pub unsafe fn destroy_linker(linker_ptr: *mut c_void) {
        ffi_utils::destroy_resource::<Linker>(linker_ptr, "Linker");
    }

    /// Core function to instantiate module with linker
    pub fn instantiate_module(
        linker: &Linker,
        store: &mut Store,
        module: &Module,
    ) -> WasmtimeResult<LinkerInstantiationResult> {
        linker.instantiate(store, module)
    }

    /// Core function to get linker metadata
    pub fn get_metadata(linker: &Linker) -> &LinkerMetadata {
        linker.metadata()
    }

    /// Core function to check if linker is valid
    pub fn is_valid(linker: &Linker) -> bool {
        linker.is_valid()
    }

    /// Core function to dispose linker
    pub fn dispose_linker(linker: &mut Linker) {
        linker.dispose()
    }

    /// Core function to get host function count
    pub fn host_function_count(linker: &Linker) -> usize {
        linker.host_functions().len()
    }

    /// Core function to get import count
    pub fn import_count(linker: &Linker) -> usize {
        linker.imports().len()
    }
}

/// Create a new linker with engine
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
/// Returns pointer to linker that must be freed with wasmtime4j_linker_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_new(engine_ptr: *const c_void) -> *mut c_void {
    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            match ffi_core::create_linker(engine) {
                Ok(linker) => Box::into_raw(linker) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Create a new linker with configuration
///
/// # Safety
///
/// engine_ptr must be a valid pointer from wasmtime4j_engine_new
/// Returns pointer to linker that must be freed with wasmtime4j_linker_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_new_with_config(
    engine_ptr: *const c_void,
    allow_unknown_exports: c_int,
    allow_shadowing: c_int,
) -> *mut c_void {
    match crate::engine::core::get_engine_ref(engine_ptr) {
        Ok(engine) => {
            let config = LinkerConfig {
                allow_unknown_exports: allow_unknown_exports != 0,
                allow_shadowing: allow_shadowing != 0,
            };
            match ffi_core::create_linker_with_config(engine, config) {
                Ok(linker) => Box::into_raw(linker) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Destroy linker and free resources
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_destroy(linker_ptr: *mut c_void) {
    if !linker_ptr.is_null() {
        core::destroy_linker(linker_ptr);
    }
}

/// Instantiate module with linker
///
/// # Safety
///
/// All pointers must be valid
/// Returns pointer to instance that must be freed with wasmtime4j_instance_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_instantiate(
    linker_ptr: *const c_void,
    store_ptr: *mut c_void,
    module_ptr: *const c_void,
) -> *mut c_void {
    if linker_ptr.is_null() || store_ptr.is_null() || module_ptr.is_null() {
        return std::ptr::null_mut();
    }

    match (
        core::get_linker_ref(linker_ptr),
        crate::store::core::get_store_mut(store_ptr),
        crate::module::core::get_module_ref(module_ptr)
    ) {
        (Ok(linker), Ok(store), Ok(module)) => {
            match ffi_core::instantiate_module(linker, store, module) {
                Ok(result) => Box::into_raw(Box::new(result.instance)) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        },
        _ => std::ptr::null_mut(),
    }
}

/// Check if linker is valid
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_is_valid(linker_ptr: *const c_void) -> c_int {
    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => if core::is_valid(linker) { 1 } else { 0 },
        Err(_) => FFI_ERROR,
    }
}

/// Dispose linker resources
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_dispose(linker_ptr: *mut c_void) -> c_int {
    match ffi_core::get_linker_mut(linker_ptr) {
        Ok(linker) => {
            core::dispose_linker(linker);
            FFI_SUCCESS
        },
        Err(_) => FFI_ERROR,
    }
}

/// Get number of host functions in linker
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_host_function_count(linker_ptr: *const c_void) -> usize {
    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => core::host_function_count(linker),
        Err(_) => 0,
    }
}

/// Get number of imports in linker
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_import_count(linker_ptr: *const c_void) -> usize {
    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => core::import_count(linker),
        Err(_) => 0,
    }
}

/// Get linker instantiation count
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_instantiation_count(linker_ptr: *const c_void) -> u64 {
    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => core::get_metadata(linker).instantiation_count,
        Err(_) => 0,
    }
}

/// Check if WASI is enabled in linker
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_wasi_enabled(linker_ptr: *const c_void) -> c_int {
    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => if core::get_metadata(linker).wasi_enabled { 1 } else { 0 },
        Err(_) => FFI_ERROR,
    }
}

/// Get linker creation timestamp in microseconds since epoch
///
/// # Safety
///
/// linker_ptr must be a valid pointer from wasmtime4j_linker_new
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_created_at_micros(linker_ptr: *const c_void) -> u64 {
    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => {
            let metadata = core::get_metadata(linker);
            metadata.created_at.duration_since(std::time::UNIX_EPOCH)
                .unwrap_or_default()
                .as_micros() as u64
        },
        Err(_) => 0,
    }
}
/// Resolve dependencies for a set of modules
///
/// # Safety
///
/// All pointers must be valid
/// Returns pointer to dependency graph that must be freed with wasmtime4j_dependency_graph_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_resolve_dependencies(
    linker_ptr: *const c_void,
    module_ptrs: *const *const c_void,
    module_count: usize,
) -> *mut c_void {
    if linker_ptr.is_null() || module_ptrs.is_null() || module_count == 0 {
        return std::ptr::null_mut();
    }

    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => {
            // Convert module pointers to Module references
            let mut modules = Vec::new();
            for i in 0..module_count {
                let module_ptr = *module_ptrs.add(i);
                if let Ok(module) = crate::module::ffi_core::get_module_ref(module_ptr) {
                    modules.push(module.clone());
                }
            }

            match linker.resolve_dependencies(&modules) {
                Ok(graph) => Box::into_raw(Box::new(graph)) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Validate imports for a set of modules
///
/// # Safety
///
/// All pointers must be valid
/// Returns pointer to validation issues array that must be freed with wasmtime4j_validation_issues_destroy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_validate_imports(
    linker_ptr: *const c_void,
    module_ptrs: *const *const c_void,
    module_count: usize,
    issue_count_out: *mut usize,
) -> *mut c_void {
    if linker_ptr.is_null() || module_ptrs.is_null() || module_count == 0 {
        if !issue_count_out.is_null() {
            *issue_count_out = 0;
        }
        return std::ptr::null_mut();
    }

    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => {
            // Convert module pointers to Module references
            let mut modules = Vec::new();
            for i in 0..module_count {
                let module_ptr = *module_ptrs.add(i);
                if let Ok(module) = crate::module::ffi_core::get_module_ref(module_ptr) {
                    modules.push(module.clone());
                }
            }

            match linker.validate_imports(&modules) {
                Ok(issues) => {
                    if !issue_count_out.is_null() {
                        *issue_count_out = issues.len();
                    }
                    Box::into_raw(Box::new(issues)) as *mut c_void
                },
                Err(_) => {
                    if !issue_count_out.is_null() {
                        *issue_count_out = 0;
                    }
                    std::ptr::null_mut()
                }
            }
        },
        Err(_) => {
            if !issue_count_out.is_null() {
                *issue_count_out = 0;
            }
            std::ptr::null_mut()
        }
    }
}

/// Check if linker has a specific import
///
/// # Safety
///
/// linker_ptr must be valid, module_name and import_name must be valid C strings
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_linker_has_import(
    linker_ptr: *const c_void,
    module_name: *const c_char,
    import_name: *const c_char,
) -> c_int {
    if linker_ptr.is_null() || module_name.is_null() || import_name.is_null() {
        return FFI_ERROR;
    }

    match ffi_core::get_linker_ref(linker_ptr) {
        Ok(linker) => {
            if let (Ok(mod_name), Ok(imp_name)) = (
                CStr::from_ptr(module_name).to_str(),
                CStr::from_ptr(import_name).to_str()
            ) {
                if linker.has_import(mod_name, imp_name) { 1 } else { 0 }
            } else {
                FFI_ERROR
            }
        },
        Err(_) => FFI_ERROR,
    }
}

/// Destroy dependency graph
///
/// # Safety
///
/// graph_ptr must be a valid pointer from wasmtime4j_linker_resolve_dependencies
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_dependency_graph_destroy(graph_ptr: *mut c_void) {
    if !graph_ptr.is_null() {
        let _ = Box::from_raw(graph_ptr as *mut DependencyGraph);
    }
}

/// Destroy validation issues array
///
/// # Safety
///
/// issues_ptr must be a valid pointer from wasmtime4j_linker_validate_imports
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_validation_issues_destroy(issues_ptr: *mut c_void) {
    if !issues_ptr.is_null() {
        let _ = Box::from_raw(issues_ptr as *mut Vec<ImportValidationIssue>);
    }
}
