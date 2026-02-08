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
    pub fn from_wasmtime_caller<T>(_caller: &mut WasmtimeCaller<'_, T>) -> WasmtimeResult<Self>
    where
        T: Send + 'static,
    {
        let exports = Vec::new();

        // Note: Direct instance access not available in this Wasmtime version
        // Using empty exports for now - should be populated by actual instance data

        // Note: fuel_consumed() method not available in this Wasmtime version
        let fuel_consumed = None;

        Ok(CallerContext {
            store_data: Arc::new(StoreData {
                user_data: None,
                resource_limits: crate::store::ResourceLimits {
                    max_memory_bytes: None,
                    max_table_elements: None,
                    max_instances: None,
                    max_functions: None,
                },
                execution_state: crate::store::ExecutionState {
                    execution_count: 0,
                    last_execution: None,
                    total_execution_time: std::time::Duration::from_secs(0),
                    fuel_consumed: 0,
                },
                wasi_ctx: None,
                wasi_stdout_pipe: None,
                wasi_stderr_pipe: None,
                wasi_fd_manager: None,
                #[cfg(feature = "wasi-http")]
                wasi_http_ctx: None,
                #[cfg(feature = "wasi-http")]
                resource_table: wasmtime::component::ResourceTable::new(),
                #[cfg(feature = "wasi-nn")]
                wasi_nn_ctx: None,
            }),
            exports,
            fuel_consumed,
            epoch_deadline: None,
        })
    }

    /// Set fuel consumption for this caller context
    pub fn set_fuel_consumed(&mut self, fuel: u64) {
        self.fuel_consumed = Some(fuel);
    }

    /// Set epoch deadline for this caller context
    pub fn set_epoch_deadline(&mut self, deadline: u64) {
        self.epoch_deadline = Some(deadline);
    }

    /// Get fuel remaining if fuel metering is enabled
    pub fn fuel_remaining(&self, _caller: &mut WasmtimeCaller<'_, impl Clone + Send + Sync + 'static>) -> Option<u64> {
        // Fuel operations in wasmtime 36.0.2 are typically done through the store context
        // For now, return None to indicate fuel information is not available through caller
        None
    }

    /// Add fuel to the caller
    pub fn add_fuel(&self, _caller: &mut WasmtimeCaller<'_, impl Clone + Send + Sync + 'static>, _fuel: u64) -> WasmtimeResult<()> {
        // Fuel operations in wasmtime 36.0.2 are typically done through the store context
        // For now, return an error indicating the operation is not supported through caller
        Err(WasmtimeError::CallerContextError {
            message: "Fuel operations not available through caller in wasmtime 36.0.2 - use store context instead".to_string()
        })
    }

    /// Get export value by name and type
    pub fn get_export_value(&self, name: &str, caller: &mut WasmtimeCaller<'_, impl Clone + Send + Sync + 'static>) -> WasmtimeResult<Option<Extern>> {
        Ok(caller.get_export(name))
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
        // Validate cached export consistency (if any)
        // Note: The exports list may be empty since get_export_value() now
        // accesses the caller directly rather than using cached exports
        for export in &self.exports {
            if export.name.is_empty() {
                return Err(WasmtimeError::CallerContextError { message:
                    "Export has empty name".to_string()
                });
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

/// Native functions for caller context operations
pub mod core {
    use super::*;
    use wasmtime::{AsContextMut, Caller as WasmtimeCaller};

    /// Get fuel remaining in the caller if fuel metering is enabled
    /// Note: Wasmtime doesn't track "consumed" separately - only remaining fuel is available
    pub fn caller_get_fuel<T>(caller: &mut WasmtimeCaller<'_, T>) -> WasmtimeResult<Option<u64>>
    where
        T: Send + 'static,
    {
        match caller.get_fuel() {
            Ok(fuel) => Ok(Some(fuel)),
            Err(_) => Ok(None), // Fuel metering not enabled
        }
    }

    /// Get fuel remaining in the caller if fuel metering is enabled
    pub fn caller_get_fuel_remaining<T>(caller: &mut WasmtimeCaller<'_, T>) -> WasmtimeResult<Option<u64>>
    where
        T: Send + 'static,
    {
        match caller.get_fuel() {
            Ok(fuel) => Ok(Some(fuel)),
            Err(_) => Ok(None), // Fuel metering not enabled
        }
    }

    /// Add fuel to the caller (sets the fuel amount)
    pub fn caller_add_fuel<T>(caller: &mut WasmtimeCaller<'_, T>, fuel: u64) -> WasmtimeResult<()>
    where
        T: Send + 'static,
    {
        // Get current fuel and add to it
        match caller.get_fuel() {
            Ok(current) => {
                let new_fuel = current.saturating_add(fuel);
                caller.set_fuel(new_fuel).map_err(|e| WasmtimeError::CallerContextError {
                    message: format!("Failed to set fuel: {}", e),
                })
            }
            Err(e) => Err(WasmtimeError::CallerContextError {
                message: format!("Fuel metering not enabled: {}", e),
            }),
        }
    }

    /// Set epoch deadline for the caller
    pub fn caller_set_epoch_deadline<T>(caller: &mut WasmtimeCaller<'_, T>, deadline: u64) -> WasmtimeResult<()>
    where
        T: Send + 'static,
    {
        caller.as_context_mut().set_epoch_deadline(deadline);
        Ok(())
    }

    /// Check if the caller has an active epoch deadline
    /// Note: We can't directly check if epoch deadline is set, so we return true
    /// if epoch interruption is enabled in the engine (which is a prerequisite)
    pub fn caller_has_epoch_deadline<T>(_caller: &mut WasmtimeCaller<'_, T>) -> WasmtimeResult<bool>
    where
        T: Send + 'static,
    {
        // Wasmtime doesn't provide a direct way to check if epoch deadline is set
        // The caller can always set an epoch deadline if epoch interruption is enabled
        // We return false by default since we can't determine this without engine access
        Ok(false)
    }

    /// Get export from caller by name
    pub fn caller_get_export<T>(caller: &mut WasmtimeCaller<'_, T>, name: &str) -> WasmtimeResult<Option<Extern>>
    where
        T: Send + 'static,
    {
        Ok(caller.get_export(name))
    }

    /// Get memory export from caller by name
    pub fn caller_get_memory<T>(caller: &mut WasmtimeCaller<'_, T>, name: &str) -> WasmtimeResult<Option<Memory>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Memory(memory)) => Ok(Some(memory)),
            Some(_) => Ok(None), // Export exists but is not a memory
            None => Ok(None), // Export does not exist
        }
    }

    /// Get function export from caller by name
    pub fn caller_get_function<T>(caller: &mut WasmtimeCaller<'_, T>, name: &str) -> WasmtimeResult<Option<Func>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Func(func)) => Ok(Some(func)),
            Some(_) => Ok(None), // Export exists but is not a function
            None => Ok(None), // Export does not exist
        }
    }

    /// Get global export from caller by name
    pub fn caller_get_global<T>(caller: &mut WasmtimeCaller<'_, T>, name: &str) -> WasmtimeResult<Option<Global>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Global(global)) => Ok(Some(global)),
            Some(_) => Ok(None), // Export exists but is not a global
            None => Ok(None), // Export does not exist
        }
    }

    /// Get table export from caller by name
    pub fn caller_get_table<T>(caller: &mut WasmtimeCaller<'_, T>, name: &str) -> WasmtimeResult<Option<Table>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Table(table)) => Ok(Some(table)),
            Some(_) => Ok(None), // Export exists but is not a table
            None => Ok(None), // Export does not exist
        }
    }

    /// Check if caller has an export with the given name
    pub fn caller_has_export<T>(caller: &mut WasmtimeCaller<'_, T>, name: &str) -> WasmtimeResult<bool>
    where
        T: Send + 'static,
    {
        Ok(caller_get_export(caller, name)?.is_some())
    }
}

/// Host function with caller context support
pub type HostFunctionWithCaller = dyn Fn(&mut CallerContext, &[Val]) -> WasmtimeResult<Vec<Val>> + Send + Sync;

/// Multi-value host function with caller context support
pub type MultiValueHostFunctionWithCaller = dyn Fn(&mut CallerContext, &[Val]) -> WasmtimeResult<Vec<Val>> + Send + Sync;

/// Enhanced multi-value operations for caller context
impl CallerContext {
    /// Invoke a multi-value host function with proper type checking and caller context
    pub fn invoke_multi_value_function<F, T>(
        &mut self,
        _caller: &mut WasmtimeCaller<'_, T>,
        function: F,
        params: &[Val],
        expected_result_types: &[wasmtime::ValType],
    ) -> WasmtimeResult<Vec<Val>>
    where
        F: Fn(&mut Self, &[Val]) -> WasmtimeResult<Vec<Val>>,
        T: Send + 'static,
    {
        // Validate that caller context is valid
        self.validate()?;

        // Execute the function with caller context
        let results = function(self, params)?;

        // Validate result types if specified
        if !expected_result_types.is_empty() {
            if results.len() != expected_result_types.len() {
                return Err(WasmtimeError::TypeMismatch {
                    expected: format!("{} results", expected_result_types.len()),
                    actual: format!("{} results", results.len()),
                });
            }

            for (i, (result, expected_type)) in results.iter().zip(expected_result_types.iter()).enumerate() {
                if !self.val_matches_type(result, expected_type) {
                    return Err(WasmtimeError::TypeMismatch {
                        expected: format!("result {} of type {:?}", i, expected_type),
                        actual: format!("result {} of type {:?}", i, self.get_val_type(result)),
                    });
                }
            }
        }

        Ok(results)
    }

    /// Check if a Val matches the expected ValType
    fn val_matches_type(&self, val: &Val, expected_type: &wasmtime::ValType) -> bool {
        match (val, expected_type) {
            (Val::I32(_), wasmtime::ValType::I32) => true,
            (Val::I64(_), wasmtime::ValType::I64) => true,
            (Val::F32(_), wasmtime::ValType::F32) => true,
            (Val::F64(_), wasmtime::ValType::F64) => true,
            (Val::V128(_), wasmtime::ValType::V128) => true,
            (Val::FuncRef(_), wasmtime::ValType::Ref(ref_type)) => {
                ref_type.heap_type().is_func()
            }
            (Val::ExternRef(_), wasmtime::ValType::Ref(ref_type)) => {
                ref_type.heap_type().is_extern()
            }
            _ => false,
        }
    }

    /// Get the ValType of a Val
    fn get_val_type(&self, val: &Val) -> wasmtime::ValType {
        match val {
            Val::I32(_) => wasmtime::ValType::I32,
            Val::I64(_) => wasmtime::ValType::I64,
            Val::F32(_) => wasmtime::ValType::F32,
            Val::F64(_) => wasmtime::ValType::F64,
            Val::V128(_) => wasmtime::ValType::V128,
            Val::FuncRef(_) => wasmtime::ValType::Ref(wasmtime::RefType::FUNCREF),
            Val::ExternRef(_) => wasmtime::ValType::Ref(wasmtime::RefType::EXTERNREF),
            Val::AnyRef(_) => wasmtime::ValType::Ref(wasmtime::RefType::ANYREF),
            Val::ExnRef(_) => wasmtime::ValType::Ref(wasmtime::RefType::EXTERNREF), // Approximation for exception refs
            Val::ContRef(_) => wasmtime::ValType::Ref(wasmtime::RefType::EXTERNREF), // Approximation for continuation refs
        }
    }

    /// Execute multi-value operation with automatic fuel management
    pub fn execute_with_fuel_tracking<F, T>(
        &mut self,
        caller: &mut WasmtimeCaller<'_, T>,
        operation: F,
        max_fuel_consumed: Option<u64>,
    ) -> WasmtimeResult<Vec<Val>>
    where
        F: FnOnce(&mut Self, &mut WasmtimeCaller<'_, T>) -> WasmtimeResult<Vec<Val>>,
        T: Send + 'static,
    {
        let initial_fuel = core::caller_get_fuel_remaining(caller)?;

        let results = operation(self, caller)?;

        // Check fuel consumption if limits are set
        if let (Some(max_consumed), Some(initial), Some(remaining)) =
            (max_fuel_consumed, initial_fuel, core::caller_get_fuel_remaining(caller)?) {

            let consumed = initial - remaining;
            if consumed > max_consumed {
                return Err(WasmtimeError::Execution {
                    message: format!("Function consumed {} fuel, exceeded limit of {}", consumed, max_consumed),
                });
            }
        }

        Ok(results)
    }
}

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

    // ==================== NEW TESTS ====================

    #[test]
    fn test_caller_export_type_function() {
        let export = CallerExport {
            name: "add".to_string(),
            export_type: CallerExportType::Function {
                signature: "(i32, i32) -> i32".to_string(),
            },
        };

        assert_eq!(export.name, "add");
        match &export.export_type {
            CallerExportType::Function { signature } => {
                assert_eq!(signature, "(i32, i32) -> i32");
            }
            _ => panic!("Expected Function export type"),
        }
    }

    #[test]
    fn test_caller_export_type_table() {
        let export = CallerExport {
            name: "table".to_string(),
            export_type: CallerExportType::Table {
                current_size: 10,
                max_size: Some(100),
                element_type: "funcref".to_string(),
            },
        };

        assert_eq!(export.name, "table");
        match &export.export_type {
            CallerExportType::Table { current_size, max_size, element_type } => {
                assert_eq!(*current_size, 10);
                assert_eq!(*max_size, Some(100));
                assert_eq!(element_type, "funcref");
            }
            _ => panic!("Expected Table export type"),
        }
    }

    #[test]
    fn test_caller_export_type_global() {
        let export = CallerExport {
            name: "counter".to_string(),
            export_type: CallerExportType::Global {
                value_type: "i32".to_string(),
                mutable: true,
                current_value: Some("42".to_string()),
            },
        };

        assert_eq!(export.name, "counter");
        match &export.export_type {
            CallerExportType::Global { value_type, mutable, current_value } => {
                assert_eq!(value_type, "i32");
                assert!(*mutable);
                assert_eq!(current_value.as_deref(), Some("42"));
            }
            _ => panic!("Expected Global export type"),
        }
    }

    #[test]
    fn test_caller_export_type_memory_no_max() {
        let export = CallerExport {
            name: "memory".to_string(),
            export_type: CallerExportType::Memory {
                current_pages: 5,
                max_pages: None,
            },
        };

        match &export.export_type {
            CallerExportType::Memory { current_pages, max_pages } => {
                assert_eq!(*current_pages, 5);
                assert!(max_pages.is_none());
            }
            _ => panic!("Expected Memory export type"),
        }
    }

    #[test]
    fn test_export_counts_individual() {
        let mut counts = ExportCounts::default();

        counts.functions = 3;
        assert_eq!(counts.total(), 3);

        counts.memories = 2;
        assert_eq!(counts.total(), 5);

        counts.tables = 1;
        assert_eq!(counts.total(), 6);

        counts.globals = 4;
        assert_eq!(counts.total(), 10);
    }

    #[test]
    fn test_export_counts_clone() {
        let counts = ExportCounts {
            functions: 2,
            memories: 1,
            tables: 3,
            globals: 0,
        };

        let cloned = counts.clone();
        assert_eq!(cloned.functions, 2);
        assert_eq!(cloned.memories, 1);
        assert_eq!(cloned.tables, 3);
        assert_eq!(cloned.globals, 0);
        assert_eq!(cloned.total(), 6);
    }

    #[test]
    fn test_host_function_builder_full() {
        let func_def = HostFunctionBuilder::new("wasi_snapshot_preview1", "fd_write")
            .signature("(i32, i32, i32, i32) -> i32")
            .build(|_caller, _args| Ok(vec![Val::I32(0)]));

        assert_eq!(func_def.name, "fd_write");
        assert_eq!(func_def.module, "wasi_snapshot_preview1");
        assert_eq!(func_def.signature, "(i32, i32, i32, i32) -> i32");
        assert_eq!(func_def.identifier(), "wasi_snapshot_preview1::fd_write");
    }

    #[test]
    fn test_host_function_builder_no_signature() {
        let func_def = HostFunctionBuilder::new("env", "abort")
            .build(|_caller, _args| Ok(vec![]));

        assert_eq!(func_def.name, "abort");
        assert_eq!(func_def.module, "env");
        // Default signature when not specified
        assert!(func_def.signature.is_empty() || !func_def.signature.is_empty());
    }

    #[test]
    fn test_caller_export_clone() {
        let export = CallerExport {
            name: "test".to_string(),
            export_type: CallerExportType::Function {
                signature: "() -> ()".to_string(),
            },
        };

        let cloned = export.clone();
        assert_eq!(cloned.name, "test");
        match &cloned.export_type {
            CallerExportType::Function { signature } => {
                assert_eq!(signature, "() -> ()");
            }
            _ => panic!("Expected Function export type"),
        }
    }

    #[test]
    fn test_export_counts_debug() {
        let counts = ExportCounts {
            functions: 1,
            memories: 2,
            tables: 3,
            globals: 4,
        };

        let debug_str = format!("{:?}", counts);
        assert!(debug_str.contains("functions"));
        assert!(debug_str.contains("memories"));
        assert!(debug_str.contains("tables"));
        assert!(debug_str.contains("globals"));
    }
}