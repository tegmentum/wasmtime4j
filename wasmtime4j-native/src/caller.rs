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
    ///
    /// Returns true if epoch interruption is enabled on the engine that created
    /// this store, which means an epoch deadline is always active (defaults to 0 ticks).
    pub fn caller_has_epoch_deadline(
        caller: &mut WasmtimeCaller<'_, crate::store::StoreData>,
    ) -> WasmtimeResult<bool> {
        Ok(caller.data().epoch_interruption_enabled)
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

    /// Set fuel async yield interval for the caller's store
    pub fn caller_set_fuel_async_yield_interval<T>(
        caller: &mut WasmtimeCaller<'_, T>,
        interval: u64,
    ) -> WasmtimeResult<()>
    where
        T: Send + 'static,
    {
        let interval_opt = if interval == 0 { None } else { Some(interval) };
        caller
            .as_context_mut()
            .fuel_async_yield_interval(interval_opt)
            .map_err(|e| WasmtimeError::CallerContextError {
                message: format!("Failed to set fuel async yield interval: {}", e),
            })
    }

    /// Snapshot all debug exit frames from the caller.
    ///
    /// Returns None if guest debugging is not enabled, or an empty vec if no frames.
    /// Each frame is represented as [func_index, pc, num_locals, num_stacks].
    pub fn caller_debug_exit_frames(
        caller: &mut WasmtimeCaller<'_, crate::store::StoreData>,
    ) -> WasmtimeResult<Option<Vec<[i32; 4]>>> {
        let frame_handles: Vec<_> = caller.as_context_mut().debug_exit_frames().collect();
        if frame_handles.is_empty() {
            return Ok(None);
        }
        let mut frames = Vec::new();
        for handle in &frame_handles {
            let (func_index, pc) = handle
                .wasm_function_index_and_pc(caller.as_context_mut())
                .ok()
                .flatten()
                .map(|(fi, pc)| (fi.as_u32() as i32, pc as i32))
                .unwrap_or((-1, -1));
            let num_locals = handle
                .num_locals(caller.as_context_mut())
                .unwrap_or(0) as i32;
            let num_stacks = handle
                .num_stacks(caller.as_context_mut())
                .unwrap_or(0) as i32;
            frames.push([func_index, pc, num_locals, num_stacks]);
        }
        Ok(Some(frames))
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
