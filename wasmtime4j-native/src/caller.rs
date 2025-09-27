//! Caller context support for host functions with comprehensive WebAssembly state access
//!
//! This module provides safe access to WebAssembly execution context from within host functions,
//! allowing host code to inspect and manipulate the calling WebAssembly instance's state.

use std::sync::{Arc, Mutex};
use wasmtime::{Caller as WasmtimeCaller, Memory, Global, Table, Store, StoreContextMut, StoreContext};
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::StoreData;
use crate::memory::Memory as WasmMemory;
use crate::global::Global as WasmGlobal;
use crate::table::Table as WasmTable;

/// Safe wrapper around Wasmtime Caller providing access to WebAssembly execution context
pub struct CallerContext<'a> {
    /// The underlying Wasmtime caller
    caller: &'a mut WasmtimeCaller<'a, StoreData>,
}

impl<'a> CallerContext<'a> {
    /// Creates a new CallerContext from a Wasmtime Caller
    ///
    /// # Arguments
    /// * `caller` - The Wasmtime caller to wrap
    ///
    /// # Returns
    /// A new CallerContext instance
    pub fn new(caller: &'a mut WasmtimeCaller<'a, StoreData>) -> Self {
        Self { caller }
    }

    /// Gets a memory export from the calling WebAssembly instance
    ///
    /// # Arguments
    /// * `name` - Name of the memory export to retrieve
    ///
    /// # Returns
    /// The WebAssembly memory if found
    ///
    /// # Errors
    /// Returns WasmtimeError if the memory is not found or cannot be accessed
    pub fn get_memory(&mut self, name: &str) -> WasmtimeResult<WasmMemory> {
        let wasmtime_memory = self.caller
            .get_export(name)
            .and_then(|export| export.into_memory())
            .ok_or_else(|| WasmtimeError::Runtime {
                message: format!("Memory export '{}' not found", name),
                backtrace: None,
            })?;

        WasmMemory::from_wasmtime_memory(wasmtime_memory, self.caller)
    }

    /// Gets a global export from the calling WebAssembly instance
    ///
    /// # Arguments
    /// * `name` - Name of the global export to retrieve
    ///
    /// # Returns
    /// The WebAssembly global if found
    ///
    /// # Errors
    /// Returns WasmtimeError if the global is not found or cannot be accessed
    pub fn get_global(&mut self, name: &str) -> WasmtimeResult<WasmGlobal> {
        let wasmtime_global = self.caller
            .get_export(name)
            .and_then(|export| export.into_global())
            .ok_or_else(|| WasmtimeError::Runtime {
                message: format!("Global export '{}' not found", name),
                backtrace: None,
            })?;

        WasmGlobal::from_wasmtime_global(wasmtime_global, self.caller)
    }

    /// Gets a table export from the calling WebAssembly instance
    ///
    /// # Arguments
    /// * `name` - Name of the table export to retrieve
    ///
    /// # Returns
    /// The WebAssembly table if found
    ///
    /// # Errors
    /// Returns WasmtimeError if the table is not found or cannot be accessed
    pub fn get_table(&mut self, name: &str) -> WasmtimeResult<WasmTable> {
        let wasmtime_table = self.caller
            .get_export(name)
            .and_then(|export| export.into_table())
            .ok_or_else(|| WasmtimeError::Runtime {
                message: format!("Table export '{}' not found", name),
                backtrace: None,
            })?;

        WasmTable::from_wasmtime_table(wasmtime_table, self.caller)
    }

    /// Gets fuel remaining in the calling WebAssembly instance
    ///
    /// # Returns
    /// The amount of fuel remaining, or None if fuel consumption is not enabled
    ///
    /// # Errors
    /// Returns WasmtimeError if fuel cannot be accessed
    pub fn get_fuel(&self) -> WasmtimeResult<Option<u64>> {
        Ok(self.caller.fuel_remaining())
    }

    /// Consumes fuel from the calling WebAssembly instance
    ///
    /// # Arguments
    /// * `fuel` - Amount of fuel to consume
    ///
    /// # Returns
    /// The amount of fuel remaining after consumption
    ///
    /// # Errors
    /// Returns WasmtimeError if fuel consumption fails or fuel is not enabled
    pub fn consume_fuel(&mut self, fuel: u64) -> WasmtimeResult<u64> {
        self.caller.consume_fuel(fuel)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to consume fuel: {}", e),
                backtrace: None,
            })
    }

    /// Sets an epoch deadline for the calling WebAssembly instance
    ///
    /// # Arguments
    /// * `ticks_beyond_current` - Number of ticks beyond the current epoch
    ///
    /// # Errors
    /// Returns WasmtimeError if epoch deadline cannot be set
    pub fn set_epoch_deadline(&mut self, ticks_beyond_current: u64) -> WasmtimeResult<()> {
        self.caller.set_epoch_deadline(ticks_beyond_current);
        Ok(())
    }

    /// Gets the store data associated with the calling WebAssembly instance
    ///
    /// # Returns
    /// A reference to the store data
    pub fn data(&self) -> &StoreData {
        self.caller.data()
    }

    /// Gets a mutable reference to the store data associated with the calling WebAssembly instance
    ///
    /// # Returns
    /// A mutable reference to the store data
    pub fn data_mut(&mut self) -> &mut StoreData {
        self.caller.data_mut()
    }

    /// Lists all exports available from the calling WebAssembly instance
    ///
    /// # Returns
    /// A vector of export names
    pub fn list_exports(&self) -> Vec<String> {
        self.caller
            .exported_names()
            .map(|name| name.to_string())
            .collect()
    }

    /// Checks if a specific export exists in the calling WebAssembly instance
    ///
    /// # Arguments
    /// * `name` - Name of the export to check
    ///
    /// # Returns
    /// True if the export exists, false otherwise
    pub fn has_export(&self, name: &str) -> bool {
        self.caller.get_export(name).is_some()
    }

    /// Gets the raw Wasmtime caller for advanced operations
    ///
    /// # Safety
    /// This provides direct access to the underlying Wasmtime caller.
    /// Use with caution as it bypasses safety checks.
    pub fn raw_caller(&mut self) -> &mut WasmtimeCaller<'a, StoreData> {
        self.caller
    }
}

/// Trait for types that can be called with a CallerContext
pub trait CallableWithContext<Args, Return> {
    /// Calls the function with the provided caller context and arguments
    fn call_with_context(&self, caller: &mut CallerContext, args: Args) -> WasmtimeResult<Return>;
}

/// Host function callback that receives caller context
pub type CallerContextCallback<T> = Box<dyn Fn(&mut CallerContext, T) -> WasmtimeResult<()> + Send + Sync>;

/// Creates a host function wrapper that provides caller context to the callback
///
/// # Arguments
/// * `callback` - The callback function to wrap
///
/// # Returns
/// A Wasmtime-compatible function that provides caller context
pub fn create_host_function_with_context<T>(
    callback: CallerContextCallback<T>,
) -> impl Fn(&mut WasmtimeCaller<StoreData>, T) -> wasmtime::Result<()>
where
    T: 'static,
{
    move |caller: &mut WasmtimeCaller<StoreData>, args: T| {
        let mut caller_context = CallerContext::new(caller);
        callback(&mut caller_context, args)
            .map_err(|e| wasmtime::Error::msg(e.to_string()))
    }
}

/// Utilities for working with caller contexts
pub mod utils {
    use super::*;

    /// Safely executes a function with caller context, handling errors appropriately
    ///
    /// # Arguments
    /// * `caller` - The caller context to use
    /// * `operation` - The operation to execute
    ///
    /// # Returns
    /// The result of the operation, or an error if it fails
    pub fn with_context<F, R>(
        caller: &mut CallerContext,
        operation: F,
    ) -> WasmtimeResult<R>
    where
        F: FnOnce(&mut CallerContext) -> WasmtimeResult<R>,
    {
        operation(caller)
    }

    /// Validates that a caller context has the required exports
    ///
    /// # Arguments
    /// * `caller` - The caller context to validate
    /// * `required_exports` - List of export names that must be present
    ///
    /// # Returns
    /// Ok if all required exports are present, error otherwise
    pub fn validate_required_exports(
        caller: &CallerContext,
        required_exports: &[&str],
    ) -> WasmtimeResult<()> {
        for export_name in required_exports {
            if !caller.has_export(export_name) {
                return Err(WasmtimeError::Runtime {
                    message: format!("Required export '{}' not found", export_name),
                    backtrace: None,
                });
            }
        }
        Ok(())
    }

    /// Gets the default memory export from a caller context
    ///
    /// # Arguments
    /// * `caller` - The caller context to get memory from
    ///
    /// # Returns
    /// The default memory export if found
    pub fn get_default_memory(caller: &mut CallerContext) -> WasmtimeResult<WasmMemory> {
        caller.get_memory("memory")
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_caller_context_exports() {
        // Test will be implemented when we have a working WebAssembly instance
        // For now, just verify the module compiles
        assert!(true);
    }

    #[test]
    fn test_required_exports_validation() {
        // Test will be implemented when we have a working WebAssembly instance
        assert!(true);
    }
}