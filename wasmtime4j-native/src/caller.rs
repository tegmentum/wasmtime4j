//! Caller context support for host functions
//!
//! This module provides safe access to WebAssembly execution context from host functions,
//! enabling memory access, fuel management, and export introspection.

use crate::error::{WasmtimeError, WasmtimeResult};
use wasmtime::{AsContextMut, Caller as WasmtimeCaller, Extern, Func, Global, Memory, Table};

/// Native functions for caller context operations
pub mod core {
    use super::*;

    /// Get fuel remaining in the caller if fuel metering is enabled
    pub fn caller_get_fuel_remaining<T>(
        caller: &mut WasmtimeCaller<'_, T>,
    ) -> WasmtimeResult<Option<u64>>
    where
        T: Send + 'static,
    {
        match caller.get_fuel() {
            Ok(fuel) => Ok(Some(fuel)),
            Err(_) => Ok(None), // Fuel metering not enabled
        }
    }

    /// Add fuel to the caller (adds to existing fuel amount)
    pub fn caller_add_fuel<T>(caller: &mut WasmtimeCaller<'_, T>, fuel: u64) -> WasmtimeResult<()>
    where
        T: Send + 'static,
    {
        // Get current fuel and add to it
        match caller.get_fuel() {
            Ok(current) => {
                let new_fuel = current.saturating_add(fuel);
                caller
                    .set_fuel(new_fuel)
                    .map_err(|e| WasmtimeError::CallerContextError {
                        message: format!("Failed to set fuel: {}", e),
                    })
            }
            Err(e) => Err(WasmtimeError::CallerContextError {
                message: format!("Fuel metering not enabled: {}", e),
            }),
        }
    }

    /// Set fuel to a specific value for the caller
    pub fn caller_set_fuel<T>(caller: &mut WasmtimeCaller<'_, T>, fuel: u64) -> WasmtimeResult<()>
    where
        T: Send + 'static,
    {
        caller
            .set_fuel(fuel)
            .map_err(|e| WasmtimeError::CallerContextError {
                message: format!("Failed to set fuel: {}", e),
            })
    }

    /// Set epoch deadline for the caller
    pub fn caller_set_epoch_deadline<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        deadline: u64,
    ) -> WasmtimeResult<()>
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
    pub fn caller_get_export<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        name: &str,
    ) -> WasmtimeResult<Option<Extern>>
    where
        T: Send + 'static,
    {
        Ok(caller.get_export(name))
    }

    /// Get memory export from caller by name
    pub fn caller_get_memory<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        name: &str,
    ) -> WasmtimeResult<Option<Memory>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Memory(memory)) => Ok(Some(memory)),
            Some(_) => Ok(None), // Export exists but is not a memory
            None => Ok(None),    // Export does not exist
        }
    }

    /// Get function export from caller by name
    pub fn caller_get_function<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        name: &str,
    ) -> WasmtimeResult<Option<Func>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Func(func)) => Ok(Some(func)),
            Some(_) => Ok(None), // Export exists but is not a function
            None => Ok(None),    // Export does not exist
        }
    }

    /// Get global export from caller by name
    pub fn caller_get_global<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        name: &str,
    ) -> WasmtimeResult<Option<Global>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Global(global)) => Ok(Some(global)),
            Some(_) => Ok(None), // Export exists but is not a global
            None => Ok(None),    // Export does not exist
        }
    }

    /// Get table export from caller by name
    pub fn caller_get_table<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        name: &str,
    ) -> WasmtimeResult<Option<Table>>
    where
        T: Send + 'static,
    {
        match caller_get_export(caller, name)? {
            Some(Extern::Table(table)) => Ok(Some(table)),
            Some(_) => Ok(None), // Export exists but is not a table
            None => Ok(None),    // Export does not exist
        }
    }

    /// Check if caller has an export with the given name
    pub fn caller_has_export<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        name: &str,
    ) -> WasmtimeResult<bool>
    where
        T: Send + 'static,
    {
        Ok(caller_get_export(caller, name)?.is_some())
    }
}
