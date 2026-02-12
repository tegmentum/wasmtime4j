//! Epoch deadline callback handling for WebAssembly execution control
//!
//! This module provides callback infrastructure for handling epoch deadline events,
//! allowing Java code to be notified when WebAssembly execution exceeds its epoch deadline
//! and optionally extend the deadline to continue execution.

use crate::error::{WasmtimeError, WasmtimeResult};
use once_cell::sync::Lazy;
use std::collections::HashMap;
use std::os::raw::c_void;
use std::sync::{Arc, Mutex, RwLock};

/// Epoch deadline exceeded action types
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
#[repr(i32)]
pub enum EpochDeadlineAction {
    /// Continue execution with updated deadline delta
    Continue = 0,
    /// Trap execution (halt with error)
    Trap = 1,
    /// Yield execution (for async scenarios)
    Yield = 2,
}

/// Epoch deadline callback context
#[derive(Debug, Clone)]
pub struct EpochDeadlineContext {
    /// Store ID where deadline was exceeded
    pub store_id: u64,
    /// Current epoch value
    pub current_epoch: u64,
    /// Number of times deadline has been exceeded in this execution
    pub deadline_exceeded_count: u32,
}

/// Result from epoch deadline callback
#[derive(Debug, Clone)]
pub struct EpochDeadlineResult {
    /// Action to take
    pub action: EpochDeadlineAction,
    /// New deadline delta if action is Continue (ticks until next deadline)
    pub delta: u64,
}

impl Default for EpochDeadlineResult {
    fn default() -> Self {
        Self {
            action: EpochDeadlineAction::Trap,
            delta: 0,
        }
    }
}

/// Callback function type for epoch deadline events
pub type EpochDeadlineCallback =
    Box<dyn Fn(&EpochDeadlineContext) -> EpochDeadlineResult + Send + Sync>;

/// Epoch deadline callback handler with statistics tracking
pub struct EpochCallbackHandler {
    /// Unique handler ID
    id: u64,
    /// Store ID this handler is associated with
    store_id: u64,
    /// The callback function
    callback: Arc<EpochDeadlineCallback>,
    /// Configuration for this handler
    config: EpochCallbackConfig,
    /// Statistics about epoch deadline events
    stats: Mutex<EpochCallbackStats>,
}

/// Configuration for epoch callback handling
#[derive(Debug, Clone)]
pub struct EpochCallbackConfig {
    /// Maximum number of times to extend deadline per execution
    pub max_deadline_extensions: Option<u32>,
    /// Maximum total delta that can be added
    pub max_total_delta: Option<u64>,
    /// Whether to automatically extend deadline
    pub auto_extend: bool,
    /// Auto-extend delta amount when enabled
    pub auto_extend_delta: u64,
}

impl Default for EpochCallbackConfig {
    fn default() -> Self {
        Self {
            max_deadline_extensions: None,
            max_total_delta: None,
            auto_extend: false,
            auto_extend_delta: 1,
        }
    }
}

/// Statistics for epoch callback handler
#[derive(Debug, Clone, Default)]
pub struct EpochCallbackStats {
    /// Total deadline exceeded events
    pub deadline_exceeded_events: u64,
    /// Total delta added via callbacks
    pub total_delta_added: u64,
    /// Number of times execution continued
    pub continued_count: u64,
    /// Number of times execution was trapped
    pub trapped_count: u64,
    /// Number of times execution yielded
    pub yield_count: u64,
}

/// ID counter for callback handlers
static CALLBACK_ID_COUNTER: Lazy<Mutex<u64>> = Lazy::new(|| Mutex::new(1));

/// Registry of epoch callback handlers by ID
static EPOCH_CALLBACK_REGISTRY: Lazy<RwLock<HashMap<u64, Arc<EpochCallbackHandler>>>> =
    Lazy::new(|| RwLock::new(HashMap::new()));

impl EpochCallbackHandler {
    /// Create a new epoch callback handler
    pub fn new(
        store_id: u64,
        callback: EpochDeadlineCallback,
        config: EpochCallbackConfig,
    ) -> WasmtimeResult<Self> {
        let id = {
            let mut counter = CALLBACK_ID_COUNTER.lock().map_err(|_| {
                WasmtimeError::resource_error("Failed to acquire callback ID counter".to_string())
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
            stats: Mutex::new(EpochCallbackStats::default()),
        })
    }

    /// Get the handler ID
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Get the store ID this handler is associated with
    pub fn store_id(&self) -> u64 {
        self.store_id
    }

    /// Handle an epoch deadline exceeded event
    pub fn handle_deadline_exceeded(&self, context: &EpochDeadlineContext) -> EpochDeadlineResult {
        // Update statistics
        if let Ok(mut stats) = self.stats.lock() {
            stats.deadline_exceeded_events += 1;
        }

        // Check if we've exceeded max extensions
        if let Some(max) = self.config.max_deadline_extensions {
            if context.deadline_exceeded_count >= max {
                if let Ok(mut stats) = self.stats.lock() {
                    stats.trapped_count += 1;
                }
                return EpochDeadlineResult {
                    action: EpochDeadlineAction::Trap,
                    delta: 0,
                };
            }
        }

        // Use auto-extend if enabled
        if self.config.auto_extend {
            if let Ok(mut stats) = self.stats.lock() {
                stats.total_delta_added += self.config.auto_extend_delta;
                stats.continued_count += 1;
            }
            return EpochDeadlineResult {
                action: EpochDeadlineAction::Continue,
                delta: self.config.auto_extend_delta,
            };
        }

        // Call the user callback
        let result = (self.callback)(context);

        // Update statistics based on result
        if let Ok(mut stats) = self.stats.lock() {
            match result.action {
                EpochDeadlineAction::Continue => {
                    stats.total_delta_added += result.delta;
                    stats.continued_count += 1;
                }
                EpochDeadlineAction::Trap => {
                    stats.trapped_count += 1;
                }
                EpochDeadlineAction::Yield => {
                    stats.yield_count += 1;
                }
            }
        }

        result
    }

    /// Get current statistics
    pub fn stats(&self) -> EpochCallbackStats {
        self.stats.lock().map(|s| s.clone()).unwrap_or_default()
    }

    /// Reset statistics
    pub fn reset_stats(&self) {
        if let Ok(mut stats) = self.stats.lock() {
            *stats = EpochCallbackStats::default();
        }
    }

    /// Register a handler in the global registry
    pub fn register_handler(handler: EpochCallbackHandler) -> WasmtimeResult<u64> {
        let id = handler.id;
        let handler = Arc::new(handler);

        let mut registry = EPOCH_CALLBACK_REGISTRY.write().map_err(|_| {
            WasmtimeError::resource_error("Failed to acquire epoch callback registry".to_string())
        })?;
        registry.insert(id, handler);

        Ok(id)
    }

    /// Get a handler by ID
    pub fn get_handler(handler_id: u64) -> WasmtimeResult<Arc<EpochCallbackHandler>> {
        let registry = EPOCH_CALLBACK_REGISTRY.read().map_err(|_| {
            WasmtimeError::resource_error("Failed to acquire epoch callback registry".to_string())
        })?;

        registry.get(&handler_id).cloned().ok_or_else(|| {
            WasmtimeError::resource_error(format!(
                "Epoch callback handler {} not found",
                handler_id
            ))
        })
    }

    /// Remove a handler from the registry
    pub fn unregister_handler(handler_id: u64) -> WasmtimeResult<()> {
        let mut registry = EPOCH_CALLBACK_REGISTRY.write().map_err(|_| {
            WasmtimeError::resource_error("Failed to acquire epoch callback registry".to_string())
        })?;

        registry.remove(&handler_id);
        Ok(())
    }
}

/// Create an epoch deadline callback handler for a store
///
/// # Arguments
/// * `store_id` - The store ID to associate with this handler
/// * `callback` - The callback function to invoke on deadline exceeded
/// * `config` - Configuration for the handler
///
/// # Returns
/// Handler ID on success
pub fn create_epoch_callback_handler(
    store_id: u64,
    callback: EpochDeadlineCallback,
    config: EpochCallbackConfig,
) -> WasmtimeResult<u64> {
    let handler = EpochCallbackHandler::new(store_id, callback, config)?;
    EpochCallbackHandler::register_handler(handler)
}

/// Invoke epoch deadline callback for a given handler
pub fn invoke_epoch_callback(
    handler_id: u64,
    context: &EpochDeadlineContext,
) -> WasmtimeResult<EpochDeadlineResult> {
    let handler = EpochCallbackHandler::get_handler(handler_id)?;
    Ok(handler.handle_deadline_exceeded(context))
}

/// Get statistics for an epoch callback handler
pub fn get_handler_stats(handler_id: u64) -> WasmtimeResult<EpochCallbackStats> {
    let handler = EpochCallbackHandler::get_handler(handler_id)?;
    Ok(handler.stats())
}

/// Destroy an epoch callback handler
pub fn destroy_epoch_callback_handler(handler_id: u64) -> WasmtimeResult<()> {
    EpochCallbackHandler::unregister_handler(handler_id)
}

// =============================================================================
// FFI Functions for Panama/JNI
// =============================================================================

use crate::shared_ffi::{FFI_ERROR, FFI_SUCCESS};
use std::os::raw::c_int;

/// Create an epoch deadline callback handler with a function pointer
///
/// # Safety
///
/// callback_ptr must be a valid function pointer with signature:
/// int32_t callback(uint64_t store_id, uint64_t current_epoch, uint32_t exceeded_count, uint64_t* delta_out)
/// Returns: 0=Continue, 1=Trap, 2=Yield
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_epoch_callback_create(
    store_id: u64,
    callback_ptr: *const c_void,
    max_extensions: i32,
    max_total_delta: i64,
) -> u64 {
    if callback_ptr.is_null() {
        return 0;
    }

    // SAFETY: The caller must guarantee that `callback_ptr` is a valid function pointer
    // with the following signature: extern "C" fn(u64, u64, u32, *mut u64) -> i32
    // Parameters are: store_id, current_epoch, deadline_exceeded_count, out_delta
    // Return value: 0 = Continue, 2 = Yield, other = Trap
    // The function must use the C calling convention and remain valid for the lifetime
    // of the epoch callback handler.
    type CallbackFn = extern "C" fn(u64, u64, u32, *mut u64) -> i32;
    let callback_fn: CallbackFn = std::mem::transmute(callback_ptr);

    let config = EpochCallbackConfig {
        max_deadline_extensions: if max_extensions < 0 {
            None
        } else {
            Some(max_extensions as u32)
        },
        max_total_delta: if max_total_delta < 0 {
            None
        } else {
            Some(max_total_delta as u64)
        },
        auto_extend: false,
        auto_extend_delta: 0,
    };

    let callback: EpochDeadlineCallback = Box::new(move |ctx| {
        let mut delta: u64 = 0;
        let result = callback_fn(
            ctx.store_id,
            ctx.current_epoch,
            ctx.deadline_exceeded_count,
            &mut delta,
        );

        let action = match result {
            0 => EpochDeadlineAction::Continue,
            2 => EpochDeadlineAction::Yield,
            _ => EpochDeadlineAction::Trap,
        };

        EpochDeadlineResult { action, delta }
    });

    match create_epoch_callback_handler(store_id, callback, config) {
        Ok(id) => id,
        Err(e) => {
            log::error!("Failed to create epoch callback handler: {}", e);
            0
        }
    }
}

/// Create an auto-extending epoch callback handler
#[no_mangle]
pub extern "C" fn wasmtime4j_epoch_callback_create_auto(
    store_id: u64,
    auto_extend_delta: u64,
    max_extensions: i32,
) -> u64 {
    let config = EpochCallbackConfig {
        max_deadline_extensions: if max_extensions < 0 {
            None
        } else {
            Some(max_extensions as u32)
        },
        max_total_delta: None,
        auto_extend: true,
        auto_extend_delta,
    };

    let callback: EpochDeadlineCallback = Box::new(|_| EpochDeadlineResult::default());

    match create_epoch_callback_handler(store_id, callback, config) {
        Ok(id) => id,
        Err(e) => {
            log::error!("Failed to create auto epoch callback handler: {}", e);
            0
        }
    }
}

/// Handle an epoch deadline exceeded event and get action/delta
///
/// # Safety
///
/// action_out and delta_out must be valid pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_epoch_callback_handle_deadline(
    handler_id: u64,
    store_id: u64,
    current_epoch: u64,
    exceeded_count: u32,
    action_out: *mut i32,
    delta_out: *mut u64,
) -> c_int {
    if action_out.is_null() || delta_out.is_null() {
        return FFI_ERROR;
    }

    let context = EpochDeadlineContext {
        store_id,
        current_epoch,
        deadline_exceeded_count: exceeded_count,
    };

    match invoke_epoch_callback(handler_id, &context) {
        Ok(result) => {
            *action_out = result.action as i32;
            *delta_out = result.delta;
            FFI_SUCCESS
        }
        Err(e) => {
            log::error!("Failed to handle epoch deadline: {}", e);
            *action_out = EpochDeadlineAction::Trap as i32;
            *delta_out = 0;
            FFI_ERROR
        }
    }
}

/// Get epoch callback handler statistics
///
/// # Safety
///
/// All output pointers must be valid
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_epoch_callback_get_stats(
    handler_id: u64,
    exceeded_events_out: *mut u64,
    total_delta_out: *mut u64,
    continued_count_out: *mut u64,
    trapped_count_out: *mut u64,
    yield_count_out: *mut u64,
) -> c_int {
    match get_handler_stats(handler_id) {
        Ok(stats) => {
            if !exceeded_events_out.is_null() {
                *exceeded_events_out = stats.deadline_exceeded_events;
            }
            if !total_delta_out.is_null() {
                *total_delta_out = stats.total_delta_added;
            }
            if !continued_count_out.is_null() {
                *continued_count_out = stats.continued_count;
            }
            if !trapped_count_out.is_null() {
                *trapped_count_out = stats.trapped_count;
            }
            if !yield_count_out.is_null() {
                *yield_count_out = stats.yield_count;
            }
            FFI_SUCCESS
        }
        Err(e) => {
            log::error!("Failed to get epoch callback stats: {}", e);
            FFI_ERROR
        }
    }
}

/// Destroy an epoch callback handler by ID
#[no_mangle]
pub extern "C" fn wasmtime4j_epoch_callback_destroy(handler_id: u64) -> c_int {
    match destroy_epoch_callback_handler(handler_id) {
        Ok(_) => FFI_SUCCESS,
        Err(e) => {
            log::error!("Failed to destroy epoch callback handler: {}", e);
            FFI_ERROR
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_epoch_callback_handler_creation() {
        let callback: EpochDeadlineCallback = Box::new(|_ctx| EpochDeadlineResult {
            action: EpochDeadlineAction::Continue,
            delta: 10,
        });

        let config = EpochCallbackConfig::default();
        let handler = EpochCallbackHandler::new(1, callback, config).unwrap();
        assert!(handler.id() > 0);
        assert_eq!(handler.store_id(), 1);
    }

    #[test]
    fn test_epoch_callback_invocation() {
        let callback: EpochDeadlineCallback = Box::new(|ctx| EpochDeadlineResult {
            action: EpochDeadlineAction::Continue,
            delta: ctx.current_epoch + 5,
        });

        let config = EpochCallbackConfig::default();
        let handler = EpochCallbackHandler::new(1, callback, config).unwrap();

        let context = EpochDeadlineContext {
            store_id: 1,
            current_epoch: 100,
            deadline_exceeded_count: 1,
        };

        let result = handler.handle_deadline_exceeded(&context);
        assert_eq!(result.action, EpochDeadlineAction::Continue);
        assert_eq!(result.delta, 105);
    }

    #[test]
    fn test_auto_extend() {
        let callback: EpochDeadlineCallback = Box::new(|_| EpochDeadlineResult::default());

        let config = EpochCallbackConfig {
            auto_extend: true,
            auto_extend_delta: 50,
            ..Default::default()
        };

        let handler = EpochCallbackHandler::new(1, callback, config).unwrap();

        let context = EpochDeadlineContext {
            store_id: 1,
            current_epoch: 100,
            deadline_exceeded_count: 1,
        };

        let result = handler.handle_deadline_exceeded(&context);
        assert_eq!(result.action, EpochDeadlineAction::Continue);
        assert_eq!(result.delta, 50);
    }

    #[test]
    fn test_max_extensions_limit() {
        let callback: EpochDeadlineCallback = Box::new(|_| EpochDeadlineResult {
            action: EpochDeadlineAction::Continue,
            delta: 10,
        });

        let config = EpochCallbackConfig {
            max_deadline_extensions: Some(3),
            ..Default::default()
        };

        let handler = EpochCallbackHandler::new(1, callback, config).unwrap();

        // Should trap when exceeded count reaches max
        let context = EpochDeadlineContext {
            store_id: 1,
            current_epoch: 100,
            deadline_exceeded_count: 3,
        };

        let result = handler.handle_deadline_exceeded(&context);
        assert_eq!(result.action, EpochDeadlineAction::Trap);
    }
}
