//! Store Call Hooks API
//!
//! This module provides call hook functionality for Wasmtime stores.
//! Call hooks allow monitoring and intercepting function calls in WebAssembly
//! execution, enabling profiling, debugging, and custom behavior injection.
//!
//! The call hooks integrate with Wasmtime's `Store::call_hook` and
//! `Store::call_hook_async` APIs.

use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_long, c_void, CStr, CString};
use std::mem::ManuallyDrop;
use std::ptr;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, RwLock};
use once_cell::sync::Lazy;

use serde::{Deserialize, Serialize};

use crate::error::{WasmtimeError, WasmtimeResult};

/// Represents the type of call event
#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[repr(C)]
pub enum CallEventType {
    /// Function call is about to begin
    CallStart = 0,
    /// Function call has completed (returned normally)
    CallEnd = 1,
    /// Function call resulted in a trap
    CallTrap = 2,
    /// Host function call starting
    HostCallStart = 3,
    /// Host function call ending
    HostCallEnd = 4,
}

impl CallEventType {
    fn from_i32(value: i32) -> Option<Self> {
        match value {
            0 => Some(CallEventType::CallStart),
            1 => Some(CallEventType::CallEnd),
            2 => Some(CallEventType::CallTrap),
            3 => Some(CallEventType::HostCallStart),
            4 => Some(CallEventType::HostCallEnd),
            _ => None,
        }
    }
}

/// Represents a call event with associated metadata
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CallEvent {
    /// Type of call event
    pub event_type: CallEventType,
    /// Function index (if available)
    pub func_index: Option<u32>,
    /// Function name (if available)
    pub func_name: Option<String>,
    /// Timestamp in nanoseconds since hook registration
    pub timestamp_ns: u64,
    /// Call depth (nesting level)
    pub call_depth: u32,
    /// Module name (if available)
    pub module_name: Option<String>,
}

/// Configuration for call hooks
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CallHookConfig {
    /// Whether to capture function indices
    pub capture_func_index: bool,
    /// Whether to capture function names (requires debug info)
    pub capture_func_name: bool,
    /// Whether to capture timestamps
    pub capture_timestamps: bool,
    /// Maximum call depth to track (0 = unlimited)
    pub max_call_depth: u32,
    /// Whether to enable async hooks
    pub enable_async: bool,
    /// Event types to capture
    pub event_filter: Vec<CallEventType>,
}

impl Default for CallHookConfig {
    fn default() -> Self {
        CallHookConfig {
            capture_func_index: true,
            capture_func_name: true,
            capture_timestamps: true,
            max_call_depth: 0,
            enable_async: false,
            event_filter: vec![
                CallEventType::CallStart,
                CallEventType::CallEnd,
                CallEventType::CallTrap,
            ],
        }
    }
}

/// Statistics collected by call hooks
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct CallHookStats {
    /// Total number of call events captured
    pub total_events: u64,
    /// Number of CallStart events
    pub call_starts: u64,
    /// Number of CallEnd events
    pub call_ends: u64,
    /// Number of CallTrap events
    pub call_traps: u64,
    /// Number of HostCallStart events
    pub host_call_starts: u64,
    /// Number of HostCallEnd events
    pub host_call_ends: u64,
    /// Maximum call depth observed
    pub max_depth_observed: u32,
    /// Total execution time in nanoseconds (sum of all calls)
    pub total_execution_time_ns: u64,
}

/// A call hook handler that collects events and statistics
pub struct CallHookHandler {
    /// Configuration for this handler
    config: CallHookConfig,
    /// Statistics collected during execution
    stats: RwLock<CallHookStats>,
    /// Recent events (ring buffer of configurable size)
    recent_events: RwLock<Vec<CallEvent>>,
    /// Maximum number of recent events to keep
    max_recent_events: usize,
    /// Start timestamp for timing calculations
    start_time: std::time::Instant,
    /// Current call depth
    current_depth: std::sync::atomic::AtomicU32,
    /// Unique ID for this handler
    id: u64,
    /// Whether the handler is active
    active: std::sync::atomic::AtomicBool,
}

impl CallHookHandler {
    /// Create a new call hook handler with the given configuration
    pub fn new(config: CallHookConfig) -> Self {
        let id = NEXT_HANDLER_ID.fetch_add(1, Ordering::SeqCst);
        CallHookHandler {
            config,
            stats: RwLock::new(CallHookStats::default()),
            recent_events: RwLock::new(Vec::with_capacity(1000)),
            max_recent_events: 1000,
            start_time: std::time::Instant::now(),
            current_depth: std::sync::atomic::AtomicU32::new(0),
            id,
            active: std::sync::atomic::AtomicBool::new(true),
        }
    }

    /// Create a handler with default configuration
    pub fn with_defaults() -> Self {
        Self::new(CallHookConfig::default())
    }

    /// Get the handler ID
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Check if the handler is active
    pub fn is_active(&self) -> bool {
        self.active.load(Ordering::SeqCst)
    }

    /// Activate the handler
    pub fn activate(&self) {
        self.active.store(true, Ordering::SeqCst);
    }

    /// Deactivate the handler
    pub fn deactivate(&self) {
        self.active.store(false, Ordering::SeqCst);
    }

    /// Record a call event
    pub fn record_event(
        &self,
        event_type: CallEventType,
        func_index: Option<u32>,
        func_name: Option<String>,
        module_name: Option<String>,
    ) {
        if !self.is_active() {
            return;
        }

        // Check if this event type should be captured
        if !self.config.event_filter.contains(&event_type) {
            return;
        }

        // Update call depth
        let depth = match event_type {
            CallEventType::CallStart | CallEventType::HostCallStart => {
                let depth = self.current_depth.fetch_add(1, Ordering::SeqCst);
                // Check max depth
                if self.config.max_call_depth > 0 && depth >= self.config.max_call_depth {
                    self.current_depth.fetch_sub(1, Ordering::SeqCst);
                    return;
                }
                depth
            }
            CallEventType::CallEnd | CallEventType::HostCallEnd | CallEventType::CallTrap => {
                self.current_depth
                    .fetch_sub(1, Ordering::SeqCst)
                    .saturating_sub(1)
            }
        };

        let timestamp_ns = if self.config.capture_timestamps {
            self.start_time.elapsed().as_nanos() as u64
        } else {
            0
        };

        let event = CallEvent {
            event_type,
            func_index: if self.config.capture_func_index {
                func_index
            } else {
                None
            },
            func_name: if self.config.capture_func_name {
                func_name
            } else {
                None
            },
            timestamp_ns,
            call_depth: depth,
            module_name,
        };

        // Update statistics
        if let Ok(mut stats) = self.stats.write() {
            stats.total_events += 1;
            match event_type {
                CallEventType::CallStart => stats.call_starts += 1,
                CallEventType::CallEnd => stats.call_ends += 1,
                CallEventType::CallTrap => stats.call_traps += 1,
                CallEventType::HostCallStart => stats.host_call_starts += 1,
                CallEventType::HostCallEnd => stats.host_call_ends += 1,
            }
            if depth > stats.max_depth_observed {
                stats.max_depth_observed = depth;
            }
        }

        // Store recent event
        if let Ok(mut events) = self.recent_events.write() {
            if events.len() >= self.max_recent_events {
                events.remove(0);
            }
            events.push(event);
        }
    }

    /// Get current statistics
    pub fn get_stats(&self) -> WasmtimeResult<CallHookStats> {
        self.stats.read().map(|s| s.clone()).map_err(|_| {
            WasmtimeError::Concurrency {
                message: "Failed to acquire stats lock".to_string(),
            }
        })
    }

    /// Get recent events
    pub fn get_recent_events(&self) -> WasmtimeResult<Vec<CallEvent>> {
        self.recent_events
            .read()
            .map(|e| e.clone())
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire events lock".to_string(),
            })
    }

    /// Get configuration
    pub fn get_config(&self) -> &CallHookConfig {
        &self.config
    }

    /// Reset statistics
    pub fn reset_stats(&self) {
        if let Ok(mut stats) = self.stats.write() {
            *stats = CallHookStats::default();
        }
    }

    /// Clear recent events
    pub fn clear_events(&self) {
        if let Ok(mut events) = self.recent_events.write() {
            events.clear();
        }
    }

    /// Get current call depth
    pub fn current_depth(&self) -> u32 {
        self.current_depth.load(Ordering::SeqCst)
    }
}

// ============================================================================
// Global Registry
// ============================================================================

/// Global handler registry - wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
static HANDLER_REGISTRY: Lazy<ManuallyDrop<RwLock<HashMap<u64, Arc<CallHookHandler>>>>> =
    Lazy::new(|| ManuallyDrop::new(RwLock::new(HashMap::new())));
static NEXT_HANDLER_ID: AtomicU64 = AtomicU64::new(1);

/// Register a call hook handler and return its ID
pub fn register_handler(handler: CallHookHandler) -> WasmtimeResult<u64> {
    let id = handler.id();
    let mut registry = HANDLER_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire handler registry lock".to_string(),
    })?;
    registry.insert(id, Arc::new(handler));
    Ok(id)
}

/// Get a handler by ID
pub fn get_handler(id: u64) -> WasmtimeResult<Arc<CallHookHandler>> {
    let registry = HANDLER_REGISTRY.read().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire handler registry lock".to_string(),
    })?;
    registry.get(&id).cloned().ok_or_else(|| WasmtimeError::CallHook {
        message: format!("Handler with ID {} not found", id),
    })
}

/// Remove a handler from the registry
pub fn unregister_handler(id: u64) -> WasmtimeResult<()> {
    let mut registry = HANDLER_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire handler registry lock".to_string(),
    })?;
    registry.remove(&id);
    Ok(())
}

// ============================================================================
// Panama FFI Functions
// ============================================================================

/// Create a new call hook handler with default configuration
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_create_default() -> c_long {
    let handler = CallHookHandler::with_defaults();
    match register_handler(handler) {
        Ok(id) => id as c_long,
        Err(_) => -1,
    }
}

/// Create a new call hook handler with JSON configuration
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_create(config_json: *const c_char) -> c_long {
    if config_json.is_null() {
        return wasmtime4j_call_hook_create_default();
    }

    let config_str = match CStr::from_ptr(config_json).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let config: CallHookConfig = match serde_json::from_str(config_str) {
        Ok(c) => c,
        Err(_) => return -2,
    };

    let handler = CallHookHandler::new(config);
    match register_handler(handler) {
        Ok(id) => id as c_long,
        Err(_) => -3,
    }
}

/// Free a call hook handler
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_free(handler_id: u64) -> c_int {
    match unregister_handler(handler_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Record a call event
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_record_event(
    handler_id: u64,
    event_type: c_int,
    func_index: c_int,
    func_name: *const c_char,
    module_name: *const c_char,
) -> c_int {
    let event_type = match CallEventType::from_i32(event_type) {
        Some(t) => t,
        None => return -1,
    };

    let func_index = if func_index >= 0 {
        Some(func_index as u32)
    } else {
        None
    };

    let func_name = if !func_name.is_null() {
        CStr::from_ptr(func_name).to_str().ok().map(|s| s.to_string())
    } else {
        None
    };

    let module_name = if !module_name.is_null() {
        CStr::from_ptr(module_name)
            .to_str()
            .ok()
            .map(|s| s.to_string())
    } else {
        None
    };

    match get_handler(handler_id) {
        Ok(handler) => {
            handler.record_event(event_type, func_index, func_name, module_name);
            0
        }
        Err(_) => -2,
    }
}

/// Get statistics as JSON
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_get_stats_json(handler_id: u64) -> *mut c_char {
    match get_handler(handler_id) {
        Ok(handler) => match handler.get_stats() {
            Ok(stats) => match serde_json::to_string(&stats) {
                Ok(json) => match CString::new(json) {
                    Ok(cstr) => cstr.into_raw(),
                    Err(_) => ptr::null_mut(),
                },
                Err(_) => ptr::null_mut(),
            },
            Err(_) => ptr::null_mut(),
        },
        Err(_) => ptr::null_mut(),
    }
}

/// Get recent events as JSON
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_get_events_json(handler_id: u64) -> *mut c_char {
    match get_handler(handler_id) {
        Ok(handler) => match handler.get_recent_events() {
            Ok(events) => match serde_json::to_string(&events) {
                Ok(json) => match CString::new(json) {
                    Ok(cstr) => cstr.into_raw(),
                    Err(_) => ptr::null_mut(),
                },
                Err(_) => ptr::null_mut(),
            },
            Err(_) => ptr::null_mut(),
        },
        Err(_) => ptr::null_mut(),
    }
}

/// Get configuration as JSON
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_get_config_json(handler_id: u64) -> *mut c_char {
    match get_handler(handler_id) {
        Ok(handler) => {
            let config = handler.get_config();
            match serde_json::to_string(config) {
                Ok(json) => match CString::new(json) {
                    Ok(cstr) => cstr.into_raw(),
                    Err(_) => ptr::null_mut(),
                },
                Err(_) => ptr::null_mut(),
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Reset statistics
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_reset_stats(handler_id: u64) -> c_int {
    match get_handler(handler_id) {
        Ok(handler) => {
            handler.reset_stats();
            0
        }
        Err(_) => -1,
    }
}

/// Clear recent events
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_clear_events(handler_id: u64) -> c_int {
    match get_handler(handler_id) {
        Ok(handler) => {
            handler.clear_events();
            0
        }
        Err(_) => -1,
    }
}

/// Activate a handler
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_activate(handler_id: u64) -> c_int {
    match get_handler(handler_id) {
        Ok(handler) => {
            handler.activate();
            0
        }
        Err(_) => -1,
    }
}

/// Deactivate a handler
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_deactivate(handler_id: u64) -> c_int {
    match get_handler(handler_id) {
        Ok(handler) => {
            handler.deactivate();
            0
        }
        Err(_) => -1,
    }
}

/// Check if a handler is active
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_is_active(handler_id: u64) -> c_int {
    match get_handler(handler_id) {
        Ok(handler) => {
            if handler.is_active() {
                1
            } else {
                0
            }
        }
        Err(_) => -1,
    }
}

/// Get current call depth
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_get_depth(handler_id: u64) -> c_int {
    match get_handler(handler_id) {
        Ok(handler) => handler.current_depth() as c_int,
        Err(_) => -1,
    }
}

/// Get the number of registered handlers
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_get_count() -> c_int {
    match HANDLER_REGISTRY.read() {
        Ok(registry) => registry.len() as c_int,
        Err(_) => -1,
    }
}

/// Free a string allocated by call hook functions
///
/// # Safety
/// This function is unsafe because it dereferences the pointer
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_call_hook_string_free(s: *mut c_char) {
    if !s.is_null() {
        drop(CString::from_raw(s));
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_call_hook_handler_creation() {
        let handler = CallHookHandler::with_defaults();
        assert!(handler.is_active());
        assert_eq!(handler.current_depth(), 0);
    }

    #[test]
    fn test_call_hook_record_event() {
        let handler = CallHookHandler::with_defaults();
        handler.record_event(CallEventType::CallStart, Some(0), Some("test".to_string()), None);
        handler.record_event(CallEventType::CallEnd, Some(0), Some("test".to_string()), None);

        let stats = handler.get_stats().unwrap();
        assert_eq!(stats.call_starts, 1);
        assert_eq!(stats.call_ends, 1);
        assert_eq!(stats.total_events, 2);
    }

    #[test]
    fn test_call_hook_depth_tracking() {
        let handler = CallHookHandler::with_defaults();

        handler.record_event(CallEventType::CallStart, Some(0), None, None);
        assert_eq!(handler.current_depth(), 1);

        handler.record_event(CallEventType::CallStart, Some(1), None, None);
        assert_eq!(handler.current_depth(), 2);

        handler.record_event(CallEventType::CallEnd, Some(1), None, None);
        assert_eq!(handler.current_depth(), 1);

        handler.record_event(CallEventType::CallEnd, Some(0), None, None);
        assert_eq!(handler.current_depth(), 0);
    }

    #[test]
    fn test_call_hook_activation() {
        let handler = CallHookHandler::with_defaults();
        assert!(handler.is_active());

        handler.deactivate();
        assert!(!handler.is_active());

        // Events should not be recorded when inactive
        handler.record_event(CallEventType::CallStart, Some(0), None, None);
        let stats = handler.get_stats().unwrap();
        assert_eq!(stats.total_events, 0);

        handler.activate();
        handler.record_event(CallEventType::CallStart, Some(0), None, None);
        let stats = handler.get_stats().unwrap();
        assert_eq!(stats.total_events, 1);
    }
}
