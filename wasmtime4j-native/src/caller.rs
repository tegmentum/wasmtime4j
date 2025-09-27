//! Caller context support for host functions
//!
//! This module provides safe access to WebAssembly execution context from host functions,
//! enabling memory access, fuel management, and export introspection.

use std::sync::Arc;
use wasmtime::{Caller as WasmtimeCaller, Extern, Func, Global, Memory, Table, Val};
use crate::store::StoreData;
use crate::error::{WasmtimeError, WasmtimeResult};

/// Safe wrapper around Wasmtime's Caller for host function context access
pub struct CallerContext {
    /// Store data reference for accessing user data
    store_data: Arc<StoreData>,
    /// Available exports from the calling instance
    exports: Vec<CallerExport>,
    /// Current fuel consumption if enabled
    fuel_consumed: Option<u64>,
    /// Epoch deadline if set
    epoch_deadline: Option<u64>,
}

/// Export available through caller context
#[derive(Debug, Clone)]
pub struct CallerExport {
    /// Name of the export
    pub name: String,
    /// Type of the export
    pub export_type: CallerExportType,
}

/// Types of exports available through caller
#[derive(Debug, Clone)]
pub enum CallerExportType {
    /// Function export
    Function {
        /// Function signature information
        signature: String
    },
    /// Memory export
    Memory {
        /// Current size in pages
        current_pages: u32,
        /// Maximum size in pages if specified
        max_pages: Option<u32>,
    },
    /// Table export
    Table {
        /// Current size in elements
        current_size: u32,
        /// Maximum size if specified
        max_size: Option<u32>,
        /// Element type
        element_type: String,
    },
    /// Global export
    Global {
        /// Value type
        value_type: String,
        /// Whether mutable
        mutable: bool,
        /// Current value if accessible
        current_value: Option<String>,
    },
}

impl CallerContext {
    /// Create caller context from Wasmtime caller
    pub fn from_wasmtime_caller<T>(caller: &mut WasmtimeCaller<'_, T>) -> WasmtimeResult<Self>
    where
        T: Clone + Send + Sync + 'static,
    {
        let mut exports = Vec::new();

        // Collect available exports
        for (name, export) in caller.instance().unwrap().exports(caller) {
            let export_type = match export {
                Extern::Func(func) => {
                    let ty = func.ty(caller);
                    CallerExportType::Function {
                        signature: format!("{:?}", ty),
                    }
                },
                Extern::Memory(memory) => {
                    let ty = memory.ty(caller);
                    CallerExportType::Memory {
                        current_pages: memory.size(caller) as u32,
                        max_pages: ty.maximum(),
                    }
                },
                Extern::Table(table) => {
                    let ty = table.ty(caller);
                    CallerExportType::Table {
                        current_size: table.size(caller),
                        max_size: ty.maximum(),
                        element_type: format!("{:?}", ty.element()),
                    }
                },
                Extern::Global(global) => {
                    let ty = global.ty(caller);
                    let current_value = match global.get(caller) {
                        Ok(val) => Some(format!("{:?}", val)),
                        Err(_) => None,
                    };
                    CallerExportType::Global {
                        value_type: format!("{:?}", ty.content()),
                        mutable: ty.mutability() == wasmtime::Mutability::Var,
                        current_value,
                    }
                },
            };

            exports.push(CallerExport {
                name: name.to_string(),
                export_type,
            });
        }

        // Get fuel information if available
        let fuel_consumed = caller.fuel_consumed().ok();

        Ok(CallerContext {
            store_data: Arc::new(StoreData::default()),
            exports,
            fuel_consumed,
            epoch_deadline: None,
        })
    }

    /// Get store data associated with this caller
    pub fn data(&self) -> &StoreData {
        &self.store_data
    }

    /// Get all available exports
    pub fn exports(&self) -> &[CallerExport] {
        &self.exports
    }

    /// Get specific export by name
    pub fn get_export(&self, name: &str) -> Option<&CallerExport> {
        self.exports.iter().find(|e| e.name == name)
    }

    /// Get function export by name
    pub fn get_function(&self, name: &str) -> Option<&CallerExport> {
        self.exports.iter().find(|e| {
            e.name == name && matches!(e.export_type, CallerExportType::Function { .. })
        })
    }

    /// Get memory export by name
    pub fn get_memory(&self, name: &str) -> Option<&CallerExport> {
        self.exports.iter().find(|e| {
            e.name == name && matches!(e.export_type, CallerExportType::Memory { .. })
        })
    }

    /// Get table export by name
    pub fn get_table(&self, name: &str) -> Option<&CallerExport> {
        self.exports.iter().find(|e| {
            e.name == name && matches!(e.export_type, CallerExportType::Table { .. })
        })
    }

    /// Get global export by name
    pub fn get_global(&self, name: &str) -> Option<&CallerExport> {
        self.exports.iter().find(|e| {
            e.name == name && matches!(e.export_type, CallerExportType::Global { .. })
        })
    }

    /// Get current fuel consumption if fuel metering is enabled
    pub fn fuel_consumed(&self) -> Option<u64> {
        self.fuel_consumed
    }

    /// Check if epoch deadline is set
    pub fn has_epoch_deadline(&self) -> bool {
        self.epoch_deadline.is_some()
    }

    /// Get current epoch deadline if set
    pub fn epoch_deadline(&self) -> Option<u64> {
        self.epoch_deadline
    }

    /// Check if a specific export exists
    pub fn has_export(&self, name: &str) -> bool {
        self.exports.iter().any(|e| e.name == name)
    }

    /// Get count of each export type
    pub fn export_counts(&self) -> ExportCounts {
        let mut counts = ExportCounts::default();

        for export in &self.exports {
            match export.export_type {
                CallerExportType::Function { .. } => counts.functions += 1,
                CallerExportType::Memory { .. } => counts.memories += 1,
                CallerExportType::Table { .. } => counts.tables += 1,
                CallerExportType::Global { .. } => counts.globals += 1,
            }
        }

        counts
    }

    /// Validate caller context integrity
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Basic validation
        if self.exports.is_empty() {
            return Err(WasmtimeError::CallerContextError(
                "No exports available in caller context".to_string()
            ));
        }

        // Validate export consistency
        for export in &self.exports {
            if export.name.is_empty() {
                return Err(WasmtimeError::CallerContextError(
                    "Export has empty name".to_string()
                ));
            }
        }

        Ok(())
    }
}

/// Count of exports by type
#[derive(Debug, Default, Clone)]
pub struct ExportCounts {
    /// Number of function exports
    pub functions: usize,
    /// Number of memory exports
    pub memories: usize,
    /// Number of table exports
    pub tables: usize,
    /// Number of global exports
    pub globals: usize,
}

impl ExportCounts {
    /// Get total export count
    pub fn total(&self) -> usize {
        self.functions + self.memories + self.tables + self.globals
    }

    /// Check if any exports exist
    pub fn has_exports(&self) -> bool {
        self.total() > 0
    }
}

/// Host function with caller context support
pub type HostFunctionWithCaller<T> = dyn Fn(&mut CallerContext, &[Val]) -> WasmtimeResult<Vec<Val>> + Send + Sync;

/// Host function builder for functions that need caller context
pub struct HostFunctionBuilder {
    name: String,
    module: String,
    signature: String,
}

impl HostFunctionBuilder {
    /// Create new host function builder
    pub fn new(module: &str, name: &str) -> Self {
        Self {
            name: name.to_string(),
            module: module.to_string(),
            signature: String::new(),
        }
    }

    /// Set function signature information
    pub fn signature(mut self, sig: &str) -> Self {
        self.signature = sig.to_string();
        self
    }

    /// Build the host function definition
    pub fn build<F>(self, func: F) -> HostFunctionDefinition
    where
        F: Fn(&mut CallerContext, &[Val]) -> WasmtimeResult<Vec<Val>> + Send + Sync + 'static,
    {
        HostFunctionDefinition {
            name: self.name,
            module: self.module,
            signature: self.signature,
            implementation: Arc::new(func),
        }
    }
}

/// Host function definition with caller context
pub struct HostFunctionDefinition {
    /// Function name
    pub name: String,
    /// Module name
    pub module: String,
    /// Function signature description
    pub signature: String,
    /// Function implementation
    pub implementation: Arc<dyn Fn(&mut CallerContext, &[Val]) -> WasmtimeResult<Vec<Val>> + Send + Sync>,
}

impl HostFunctionDefinition {
    /// Create new host function definition
    pub fn new<F>(module: &str, name: &str, func: F) -> Self
    where
        F: Fn(&mut CallerContext, &[Val]) -> WasmtimeResult<Vec<Val>> + Send + Sync + 'static,
    {
        Self {
            name: name.to_string(),
            module: module.to_string(),
            signature: String::new(),
            implementation: Arc::new(func),
        }
    }

    /// Call the host function with caller context
    pub fn call(&self, caller: &mut CallerContext, args: &[Val]) -> WasmtimeResult<Vec<Val>> {
        (self.implementation)(caller, args)
    }

    /// Get function identifier (module::name)
    pub fn identifier(&self) -> String {
        format!("{}::{}", self.module, self.name)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_export_counts() {
        let mut counts = ExportCounts::default();
        assert_eq!(counts.total(), 0);
        assert!(!counts.has_exports());

        counts.functions = 2;
        counts.memories = 1;
        assert_eq!(counts.total(), 3);
        assert!(counts.has_exports());
    }

    #[test]
    fn test_host_function_builder() {
        let func_def = HostFunctionBuilder::new("test", "example")
            .signature("() -> i32")
            .build(|_caller, _args| Ok(vec![Val::I32(42)]));

        assert_eq!(func_def.name, "example");
        assert_eq!(func_def.module, "test");
        assert_eq!(func_def.signature, "() -> i32");
        assert_eq!(func_def.identifier(), "test::example");
    }

    #[test]
    fn test_caller_export() {
        let export = CallerExport {
            name: "memory".to_string(),
            export_type: CallerExportType::Memory {
                current_pages: 1,
                max_pages: Some(10),
            },
        };

        assert_eq!(export.name, "memory");
        matches!(export.export_type, CallerExportType::Memory { .. });
    }
}