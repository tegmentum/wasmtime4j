//! Fuel exhaustion callback handling for WebAssembly execution metering
//!
//! This module provides callback infrastructure for handling fuel exhaustion events,
//! allowing Java code to be notified when WebAssembly execution runs out of fuel
//! and optionally add more fuel to continue execution.

use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use std::os::raw::{c_void, c_int};
use once_cell::sync::Lazy;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::shared_ffi::{FFI_SUCCESS, FFI_ERROR};

/// Fuel exhaustion event types
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i32)]
pub enum FuelExhaustionAction {
    /// Continue execution (callback provided more fuel)
    Continue = 0,
    /// Halt execution with a trap
    Trap = 1,
    /// Pause execution (for async scenarios)
    Pause = 2,
}

/// Fuel exhaustion callback context
#[derive(Debug, Clone)]
pub struct FuelExhaustionContext {
    /// Store ID where fuel was exhausted
    pub store_id: u64,
    /// Amount of fuel consumed before exhaustion
    pub fuel_consumed: u64,
    /// Initial fuel amount when execution started
    pub initial_fuel: u64,
    /// Number of times fuel has been exhausted in this execution
    pub exhaustion_count: u32,
    /// Optional function name that was executing when fuel ran out
    pub function_name: Option<String>,
}

/// Result from fuel exhaustion callback
#[derive(Debug, Clone)]
pub struct FuelExhaustionResult {
    /// Action to take
    pub action: FuelExhaustionAction,
    /// Amount of fuel to add if action is Continue
    pub additional_fuel: u64,
}

impl Default for FuelExhaustionResult {
    fn default() -> Self {
        Self {
            action: FuelExhaustionAction::Trap,
            additional_fuel: 0,
        }
    }
}

/// Callback function type for fuel exhaustion events
pub type FuelExhaustionCallback = Box<dyn Fn(&FuelExhaustionContext) -> FuelExhaustionResult + Send + Sync>;

/// Fuel callback handler with statistics tracking
pub struct FuelCallbackHandler {
    /// Unique handler ID
    id: u64,
    /// Store ID this handler is associated with
    store_id: u64,
    /// The callback function
    callback: Arc<FuelExhaustionCallback>,
    /// Configuration for this handler
    config: FuelCallbackConfig,
    /// Statistics about fuel exhaustion events
    stats: Mutex<FuelCallbackStats>,
}

/// Configuration for fuel callback handling
#[derive(Debug, Clone)]
pub struct FuelCallbackConfig {
    /// Maximum number of times to allow fuel refill per execution
    pub max_refill_count: Option<u32>,
    /// Maximum total fuel that can be added via callbacks
    pub max_total_fuel: Option<u64>,
    /// Whether to automatically add fuel up to a threshold
    pub auto_refill: bool,
    /// Auto-refill amount when enabled
    pub auto_refill_amount: u64,
    /// Threshold below which auto-refill kicks in
    pub auto_refill_threshold: u64,
}

impl Default for FuelCallbackConfig {
    fn default() -> Self {
        Self {
            max_refill_count: None,
            max_total_fuel: None,
            auto_refill: false,
            auto_refill_amount: 1_000_000,
            auto_refill_threshold: 1_000,
        }
    }
}

/// Statistics for fuel callback handler
#[derive(Debug, Clone, Default)]
pub struct FuelCallbackStats {
    /// Total fuel exhaustion events
    pub exhaustion_events: u64,
    /// Total fuel added via callbacks
    pub total_fuel_added: u64,
    /// Number of times execution continued after exhaustion
    pub continued_count: u64,
    /// Number of times execution was trapped
    pub trapped_count: u64,
    /// Number of times execution was paused
    pub paused_count: u64,
}

/// ID counter for callback handlers
static CALLBACK_ID_COUNTER: Lazy<Mutex<u64>> = Lazy::new(|| Mutex::new(1));

/// Global registry of fuel callback handlers
static FUEL_CALLBACK_REGISTRY: Lazy<RwLock<HashMap<u64, Arc<FuelCallbackHandler>>>> =
    Lazy::new(|| RwLock::new(HashMap::new()));

impl FuelCallbackHandler {
    /// Create a new fuel callback handler
    pub fn new(
        store_id: u64,
        callback: FuelExhaustionCallback,
        config: FuelCallbackConfig,
    ) -> WasmtimeResult<Self> {
        let id = {
            let mut counter = CALLBACK_ID_COUNTER.lock().map_err(|e| {
                WasmtimeError::Concurrency {
                    message: format!("Failed to acquire ID counter: {}", e),
                }
            })?;
            let id = *counter;
            *counter += 1;
            id
        };

        Ok(Self {
            id,
            store_id,
            callback: Arc::new(callback),
            config,
            stats: Mutex::new(FuelCallbackStats::default()),
        })
    }

    /// Get the handler ID
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Get the store ID
    pub fn store_id(&self) -> u64 {
        self.store_id
    }

    /// Handle a fuel exhaustion event
    pub fn handle_exhaustion(&self, context: &FuelExhaustionContext) -> FuelExhaustionResult {
        let mut stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
        stats.exhaustion_events += 1;

        // Check refill limits
        if let Some(max_count) = self.config.max_refill_count {
            if context.exhaustion_count >= max_count {
                stats.trapped_count += 1;
                return FuelExhaustionResult {
                    action: FuelExhaustionAction::Trap,
                    additional_fuel: 0,
                };
            }
        }

        // Try auto-refill first if enabled
        if self.config.auto_refill {
            let fuel_to_add = self.config.auto_refill_amount;

            // Check total fuel limit
            if let Some(max_total) = self.config.max_total_fuel {
                if stats.total_fuel_added + fuel_to_add > max_total {
                    stats.trapped_count += 1;
                    return FuelExhaustionResult {
                        action: FuelExhaustionAction::Trap,
                        additional_fuel: 0,
                    };
                }
            }

            stats.total_fuel_added += fuel_to_add;
            stats.continued_count += 1;
            return FuelExhaustionResult {
                action: FuelExhaustionAction::Continue,
                additional_fuel: fuel_to_add,
            };
        }

        // Otherwise, invoke the callback
        let result = (self.callback)(context);

        // Update statistics based on result
        match result.action {
            FuelExhaustionAction::Continue => {
                // Check total fuel limit
                if let Some(max_total) = self.config.max_total_fuel {
                    if stats.total_fuel_added + result.additional_fuel > max_total {
                        stats.trapped_count += 1;
                        return FuelExhaustionResult {
                            action: FuelExhaustionAction::Trap,
                            additional_fuel: 0,
                        };
                    }
                }
                stats.total_fuel_added += result.additional_fuel;
                stats.continued_count += 1;
            }
            FuelExhaustionAction::Trap => {
                stats.trapped_count += 1;
            }
            FuelExhaustionAction::Pause => {
                stats.paused_count += 1;
            }
        }

        result
    }

    /// Get statistics
    pub fn stats(&self) -> FuelCallbackStats {
        self.stats.lock().unwrap_or_else(|e| e.into_inner()).clone()
    }

    /// Reset statistics
    pub fn reset_stats(&self) {
        let mut stats = self.stats.lock().unwrap_or_else(|e| e.into_inner());
        *stats = FuelCallbackStats::default();
    }
}

/// Core functions for fuel callback handling
pub mod core {
    use super::*;

    /// Register a fuel callback handler
    pub fn register_handler(handler: FuelCallbackHandler) -> WasmtimeResult<u64> {
        let id = handler.id();
        let handler_arc = Arc::new(handler);

        let mut registry = FUEL_CALLBACK_REGISTRY.write().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to acquire callback registry: {}", e),
            }
        })?;

        registry.insert(id, handler_arc);
        Ok(id)
    }

    /// Get a handler by ID
    pub fn get_handler(handler_id: u64) -> WasmtimeResult<Arc<FuelCallbackHandler>> {
        let registry = FUEL_CALLBACK_REGISTRY.read().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to acquire callback registry: {}", e),
            }
        })?;

        registry.get(&handler_id).cloned().ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Fuel callback handler {} not found", handler_id),
            }
        })
    }

    /// Unregister a handler
    pub fn unregister_handler(handler_id: u64) -> WasmtimeResult<()> {
        let mut registry = FUEL_CALLBACK_REGISTRY.write().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to acquire callback registry: {}", e),
            }
        })?;

        registry.remove(&handler_id);
        Ok(())
    }

    /// Handle fuel exhaustion for a store
    pub fn handle_fuel_exhaustion(
        store_id: u64,
        fuel_consumed: u64,
        initial_fuel: u64,
        exhaustion_count: u32,
    ) -> WasmtimeResult<FuelExhaustionResult> {
        // Find handler for this store
        let registry = FUEL_CALLBACK_REGISTRY.read().map_err(|e| {
            WasmtimeError::Concurrency {
                message: format!("Failed to acquire callback registry: {}", e),
            }
        })?;

        for handler in registry.values() {
            if handler.store_id() == store_id {
                let context = FuelExhaustionContext {
                    store_id,
                    fuel_consumed,
                    initial_fuel,
                    exhaustion_count,
                    function_name: None,
                };
                return Ok(handler.handle_exhaustion(&context));
            }
        }

        // No handler found - default to trap
        Ok(FuelExhaustionResult::default())
    }

    /// Create a simple callback handler with auto-refill
    pub fn create_auto_refill_handler(
        store_id: u64,
        refill_amount: u64,
        max_refills: Option<u32>,
    ) -> WasmtimeResult<u64> {
        let config = FuelCallbackConfig {
            max_refill_count: max_refills,
            max_total_fuel: None,
            auto_refill: true,
            auto_refill_amount: refill_amount,
            auto_refill_threshold: 0,
        };

        let callback: FuelExhaustionCallback = Box::new(|_ctx| FuelExhaustionResult::default());
        let handler = FuelCallbackHandler::new(store_id, callback, config)?;

        register_handler(handler)
    }

    /// Get handler statistics
    pub fn get_handler_stats(handler_id: u64) -> WasmtimeResult<FuelCallbackStats> {
        let handler = get_handler(handler_id)?;
        Ok(handler.stats())
    }
}

//
// Native C exports for JNI and Panama FFI consumption
//

/// Create a fuel callback handler with auto-refill configuration
///
/// # Safety
///
/// Returns handler ID, or 0 on error
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_fuel_callback_create_auto_refill(
    store_id: u64,
    refill_amount: u64,
    max_refills: i32, // -1 for unlimited
) -> u64 {
    let max_refills_opt = if max_refills < 0 {
        None
    } else {
        Some(max_refills as u32)
    };

    match core::create_auto_refill_handler(store_id, refill_amount, max_refills_opt) {
        Ok(id) => id,
        Err(e) => {
            log::error!("Failed to create fuel callback handler: {}", e);
            0
        }
    }
}

/// Create a fuel callback handler with custom callback pointer
///
/// # Safety
///
/// callback_ptr must be a valid function pointer with signature:
/// int32_t callback(uint64_t store_id, uint64_t fuel_consumed, uint64_t initial_fuel,
///                  uint32_t exhaustion_count, uint64_t* additional_fuel_out)
/// Returns: 0=Continue, 1=Trap, 2=Pause
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_fuel_callback_create_custom(
    store_id: u64,
    callback_ptr: *const c_void,
    max_refill_count: i32,
    max_total_fuel: i64,
) -> u64 {
    if callback_ptr.is_null() {
        return 0;
    }

    type CallbackFn = extern "C" fn(u64, u64, u64, u32, *mut u64) -> i32;
    let callback_fn: CallbackFn = std::mem::transmute(callback_ptr);

    let config = FuelCallbackConfig {
        max_refill_count: if max_refill_count < 0 {
            None
        } else {
            Some(max_refill_count as u32)
        },
        max_total_fuel: if max_total_fuel < 0 {
            None
        } else {
            Some(max_total_fuel as u64)
        },
        auto_refill: false,
        auto_refill_amount: 0,
        auto_refill_threshold: 0,
    };

    let callback: FuelExhaustionCallback = Box::new(move |ctx| {
        let mut additional_fuel: u64 = 0;
        let result = callback_fn(
            ctx.store_id,
            ctx.fuel_consumed,
            ctx.initial_fuel,
            ctx.exhaustion_count,
            &mut additional_fuel,
        );

        let action = match result {
            0 => FuelExhaustionAction::Continue,
            2 => FuelExhaustionAction::Pause,
            _ => FuelExhaustionAction::Trap,
        };

        FuelExhaustionResult {
            action,
            additional_fuel,
        }
    });

    match FuelCallbackHandler::new(store_id, callback, config) {
        Ok(handler) => match core::register_handler(handler) {
            Ok(id) => id,
            Err(e) => {
                log::error!("Failed to register fuel callback handler: {}", e);
                0
            }
        },
        Err(e) => {
            log::error!("Failed to create fuel callback handler: {}", e);
            0
        }
    }
}

/// Handle fuel exhaustion event and get action/fuel to add
///
/// # Safety
///
/// action_out and additional_fuel_out must be valid pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_fuel_callback_handle_exhaustion(
    store_id: u64,
    fuel_consumed: u64,
    initial_fuel: u64,
    exhaustion_count: u32,
    action_out: *mut i32,
    additional_fuel_out: *mut u64,
) -> c_int {
    if action_out.is_null() || additional_fuel_out.is_null() {
        return FFI_ERROR;
    }

    match core::handle_fuel_exhaustion(store_id, fuel_consumed, initial_fuel, exhaustion_count) {
        Ok(result) => {
            *action_out = result.action as i32;
            *additional_fuel_out = result.additional_fuel;
            FFI_SUCCESS
        }
        Err(e) => {
            log::error!("Failed to handle fuel exhaustion: {}", e);
            *action_out = FuelExhaustionAction::Trap as i32;
            *additional_fuel_out = 0;
            FFI_ERROR
        }
    }
}

/// Destroy a fuel callback handler
///
/// # Safety
///
/// handler_id must be a valid handler ID
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_fuel_callback_destroy(handler_id: u64) -> c_int {
    match core::unregister_handler(handler_id) {
        Ok(()) => FFI_SUCCESS,
        Err(_) => FFI_ERROR,
    }
}

/// Get fuel callback handler statistics
///
/// # Safety
///
/// All output pointers must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_fuel_callback_get_stats(
    handler_id: u64,
    exhaustion_events_out: *mut u64,
    total_fuel_added_out: *mut u64,
    continued_count_out: *mut u64,
    trapped_count_out: *mut u64,
    paused_count_out: *mut u64,
) -> c_int {
    if exhaustion_events_out.is_null()
        || total_fuel_added_out.is_null()
        || continued_count_out.is_null()
        || trapped_count_out.is_null()
        || paused_count_out.is_null()
    {
        return FFI_ERROR;
    }

    match core::get_handler_stats(handler_id) {
        Ok(stats) => {
            *exhaustion_events_out = stats.exhaustion_events;
            *total_fuel_added_out = stats.total_fuel_added;
            *continued_count_out = stats.continued_count;
            *trapped_count_out = stats.trapped_count;
            *paused_count_out = stats.paused_count;
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

/// Reset fuel callback handler statistics
///
/// # Safety
///
/// handler_id must be a valid handler ID
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_fuel_callback_reset_stats(handler_id: u64) -> c_int {
    match core::get_handler(handler_id) {
        Ok(handler) => {
            handler.reset_stats();
            FFI_SUCCESS
        }
        Err(_) => FFI_ERROR,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_fuel_callback_creation() {
        let config = FuelCallbackConfig::default();
        let callback: FuelExhaustionCallback = Box::new(|_| FuelExhaustionResult::default());
        let handler = FuelCallbackHandler::new(1, callback, config).expect("Failed to create handler");
        assert!(handler.id() > 0);
        assert_eq!(handler.store_id(), 1);
    }

    #[test]
    fn test_auto_refill() {
        let config = FuelCallbackConfig {
            auto_refill: true,
            auto_refill_amount: 1000,
            ..Default::default()
        };
        let callback: FuelExhaustionCallback = Box::new(|_| FuelExhaustionResult::default());
        let handler = FuelCallbackHandler::new(1, callback, config).expect("Failed to create handler");

        let context = FuelExhaustionContext {
            store_id: 1,
            fuel_consumed: 10000,
            initial_fuel: 10000,
            exhaustion_count: 0,
            function_name: None,
        };

        let result = handler.handle_exhaustion(&context);
        assert_eq!(result.action, FuelExhaustionAction::Continue);
        assert_eq!(result.additional_fuel, 1000);

        let stats = handler.stats();
        assert_eq!(stats.exhaustion_events, 1);
        assert_eq!(stats.continued_count, 1);
        assert_eq!(stats.total_fuel_added, 1000);
    }

    #[test]
    fn test_max_refill_count() {
        let config = FuelCallbackConfig {
            max_refill_count: Some(2),
            auto_refill: true,
            auto_refill_amount: 1000,
            ..Default::default()
        };
        let callback: FuelExhaustionCallback = Box::new(|_| FuelExhaustionResult::default());
        let handler = FuelCallbackHandler::new(1, callback, config).expect("Failed to create handler");

        // Third exhaustion (count=2) should trap
        let context = FuelExhaustionContext {
            store_id: 1,
            fuel_consumed: 30000,
            initial_fuel: 10000,
            exhaustion_count: 2,
            function_name: None,
        };

        let result = handler.handle_exhaustion(&context);
        assert_eq!(result.action, FuelExhaustionAction::Trap);
    }

    #[test]
    fn test_callback_invocation() {
        let config = FuelCallbackConfig::default();
        let callback: FuelExhaustionCallback = Box::new(|ctx| {
            if ctx.fuel_consumed < 50000 {
                FuelExhaustionResult {
                    action: FuelExhaustionAction::Continue,
                    additional_fuel: 5000,
                }
            } else {
                FuelExhaustionResult {
                    action: FuelExhaustionAction::Trap,
                    additional_fuel: 0,
                }
            }
        });
        let handler = FuelCallbackHandler::new(1, callback, config).expect("Failed to create handler");

        // Should continue
        let context1 = FuelExhaustionContext {
            store_id: 1,
            fuel_consumed: 10000,
            initial_fuel: 10000,
            exhaustion_count: 0,
            function_name: None,
        };
        let result1 = handler.handle_exhaustion(&context1);
        assert_eq!(result1.action, FuelExhaustionAction::Continue);
        assert_eq!(result1.additional_fuel, 5000);

        // Should trap
        let context2 = FuelExhaustionContext {
            store_id: 1,
            fuel_consumed: 60000,
            initial_fuel: 10000,
            exhaustion_count: 5,
            function_name: None,
        };
        let result2 = handler.handle_exhaustion(&context2);
        assert_eq!(result2.action, FuelExhaustionAction::Trap);
    }

    #[test]
    fn test_handler_registration() {
        let config = FuelCallbackConfig::default();
        let callback: FuelExhaustionCallback = Box::new(|_| FuelExhaustionResult::default());
        let handler = FuelCallbackHandler::new(42, callback, config).expect("Failed to create handler");

        let handler_id = core::register_handler(handler).expect("Failed to register handler");
        assert!(handler_id > 0);

        let retrieved = core::get_handler(handler_id).expect("Failed to get handler");
        assert_eq!(retrieved.store_id(), 42);

        core::unregister_handler(handler_id).expect("Failed to unregister handler");
        assert!(core::get_handler(handler_id).is_err());
    }
}
