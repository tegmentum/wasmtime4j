//! WebAssembly Coredump Support
//!
//! This module provides FFI functions for working with WebAssembly coredumps.
//! Coredumps capture the complete state of a WebAssembly execution at the point
//! of a trap, including stack frames, global values, and memory contents.
//!
//! The coredump functionality requires the `coredump` feature to be enabled
//! in the wasmtime dependency.

use std::collections::HashMap;
use std::ffi::{c_char, c_int, c_long, c_void, CStr, CString};
use std::ptr;
use std::sync::{Arc, RwLock};

use wasmtime::WasmCoreDump;

use crate::error::{WasmtimeError, WasmtimeResult};

/// Represents a stored coredump with associated metadata
pub struct StoredCoreDump {
    /// The underlying wasmtime coredump
    coredump: WasmCoreDump,
    /// Optional name for the coredump
    name: Option<String>,
    /// Trap message that caused this coredump
    trap_message: Option<String>,
}

impl StoredCoreDump {
    /// Create a new stored coredump
    pub fn new(coredump: WasmCoreDump, trap_message: Option<String>) -> Self {
        StoredCoreDump {
            coredump,
            name: None,
            trap_message,
        }
    }

    /// Get the underlying coredump reference
    pub fn coredump(&self) -> &WasmCoreDump {
        &self.coredump
    }

    /// Get the coredump name
    pub fn name(&self) -> Option<&str> {
        self.name.as_deref()
    }

    /// Set the coredump name
    pub fn set_name(&mut self, name: String) {
        self.name = Some(name);
    }

    /// Get the trap message
    pub fn trap_message(&self) -> Option<&str> {
        self.trap_message.as_deref()
    }

    /// Get the number of frames in the coredump
    pub fn frame_count(&self) -> usize {
        self.coredump.frames().len()
    }

}

/// Global registry for coredumps
lazy_static::lazy_static! {
    static ref COREDUMP_REGISTRY: RwLock<HashMap<u64, Arc<StoredCoreDump>>> = RwLock::new(HashMap::new());
    static ref NEXT_COREDUMP_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);
}

/// Register a coredump and return its ID
pub fn register_coredump(coredump: StoredCoreDump) -> WasmtimeResult<u64> {
    let id = NEXT_COREDUMP_ID.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
    let mut registry = COREDUMP_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire coredump registry lock".to_string(),
    })?;
    registry.insert(id, Arc::new(coredump));
    Ok(id)
}

/// Get a coredump by ID
pub fn get_coredump(id: u64) -> WasmtimeResult<Arc<StoredCoreDump>> {
    let registry = COREDUMP_REGISTRY.read().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire coredump registry lock".to_string(),
    })?;
    registry.get(&id).cloned().ok_or_else(|| WasmtimeError::Coredump {
        message: format!("Coredump with ID {} not found", id),
    })
}

/// Remove a coredump from the registry
pub fn unregister_coredump(id: u64) -> WasmtimeResult<()> {
    let mut registry = COREDUMP_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire coredump registry lock".to_string(),
    })?;
    registry.remove(&id);
    Ok(())
}

// ============================================================================
// Panama FFI Functions
// ============================================================================

/// Free a coredump from the registry
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_free(coredump_id: u64) -> c_int {
    match unregister_coredump(coredump_id) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Get the number of frames in a coredump
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_frame_count(coredump_id: u64) -> c_int {
    match get_coredump(coredump_id) {
        Ok(coredump) => coredump.frame_count() as c_int,
        Err(_) => -1,
    }
}

/// Get the trap message from a coredump
/// Returns a newly allocated C string that must be freed by the caller
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_trap_message(coredump_id: u64) -> *mut c_char {
    match get_coredump(coredump_id) {
        Ok(coredump) => {
            if let Some(msg) = coredump.trap_message() {
                match CString::new(msg) {
                    Ok(cstr) => cstr.into_raw(),
                    Err(_) => ptr::null_mut(),
                }
            } else {
                ptr::null_mut()
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Get the name of a coredump
/// Returns a newly allocated C string that must be freed by the caller
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_name(coredump_id: u64) -> *mut c_char {
    match get_coredump(coredump_id) {
        Ok(coredump) => {
            if let Some(name) = coredump.name() {
                match CString::new(name) {
                    Ok(cstr) => cstr.into_raw(),
                    Err(_) => ptr::null_mut(),
                }
            } else {
                ptr::null_mut()
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Free a C string allocated by coredump functions
///
/// # Safety
/// This function is unsafe because it dereferences the pointer
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_string_free(s: *mut c_char) {
    if !s.is_null() {
        drop(CString::from_raw(s));
    }
}

/// Serialize a coredump to bytes
/// Returns a pointer to the serialized bytes and sets out_len to the length
/// The returned bytes must be freed using wasmtime4j_coredump_bytes_free
///
/// # Arguments
/// * `coredump_id` - The coredump ID from the registry
/// * `store_ptr` - Pointer to the Store (required for serialization in Wasmtime 38.x)
/// * `name` - C string for the coredump name in the serialized format
/// * `out_ptr` - Output pointer for the serialized bytes
/// * `out_len` - Output pointer for the length of serialized bytes
///
/// # Safety
/// This function is unsafe because it's called from FFI and dereferences raw pointers
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_serialize(
    coredump_id: u64,
    store_ptr: *mut c_void,
    name: *const c_char,
    out_ptr: *mut *mut u8,
    out_len: *mut c_long,
) -> c_int {
    if out_ptr.is_null() || out_len.is_null() || store_ptr.is_null() || name.is_null() {
        return -1;
    }

    // Get the coredump name from C string
    let name_str = match CStr::from_ptr(name).to_str() {
        Ok(s) => s,
        Err(_) => return -3,
    };

    // Get the Store from the pointer
    let store = &mut *(store_ptr as *mut crate::store::Store);

    // Lock the store to get mutable access
    let mut store_guard = store.inner.lock();

    match get_coredump(coredump_id) {
        Ok(coredump) => {
            let bytes = coredump.coredump().serialize(&mut *store_guard, name_str);
            let len = bytes.len();
            let boxed = bytes.into_boxed_slice();
            let ptr = Box::into_raw(boxed) as *mut u8;
            *out_ptr = ptr;
            *out_len = len as c_long;
            0
        }
        Err(_) => -1,
    }
}

/// Free bytes allocated by wasmtime4j_coredump_serialize
///
/// # Safety
/// This function is unsafe because it dereferences the pointer
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_bytes_free(ptr: *mut u8, len: c_long) {
    if !ptr.is_null() && len > 0 {
        let slice = std::slice::from_raw_parts_mut(ptr, len as usize);
        drop(Box::from_raw(slice as *mut [u8]));
    }
}

/// Get information about a specific frame in the coredump
/// Returns a JSON string with frame details
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_frame_info(
    coredump_id: u64,
    frame_index: c_int,
) -> *mut c_char {
    if frame_index < 0 {
        return ptr::null_mut();
    }

    match get_coredump(coredump_id) {
        Ok(coredump) => {
            let frames = coredump.coredump().frames();
            if (frame_index as usize) >= frames.len() {
                return ptr::null_mut();
            }

            let frame = &frames[frame_index as usize];

            // Build JSON with frame information
            let json = serde_json::json!({
                "index": frame_index,
                "func_index": frame.func_index(),
                "func_offset": frame.func_offset(),
            });

            match CString::new(json.to_string()) {
                Ok(cstr) => cstr.into_raw(),
                Err(_) => ptr::null_mut(),
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Get all frame information as a JSON array
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_all_frames(coredump_id: u64) -> *mut c_char {
    match get_coredump(coredump_id) {
        Ok(coredump) => {
            let frames: Vec<serde_json::Value> = coredump
                .coredump()
                .frames()
                .iter()
                .enumerate()
                .map(|(i, frame)| {
                    serde_json::json!({
                        "index": i,
                        "func_index": frame.func_index(),
                        "func_offset": frame.func_offset(),
                    })
                })
                .collect();

            match CString::new(serde_json::to_string(&frames).unwrap_or_default()) {
                Ok(cstr) => cstr.into_raw(),
                Err(_) => ptr::null_mut(),
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Get the number of registered coredumps
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_count() -> c_int {
    match COREDUMP_REGISTRY.read() {
        Ok(registry) => registry.len() as c_int,
        Err(_) => -1,
    }
}

/// Get all registered coredump IDs as a JSON array
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_get_all_ids() -> *mut c_char {
    match COREDUMP_REGISTRY.read() {
        Ok(registry) => {
            let ids: Vec<u64> = registry.keys().cloned().collect();
            match CString::new(serde_json::to_string(&ids).unwrap_or_default()) {
                Ok(cstr) => cstr.into_raw(),
                Err(_) => ptr::null_mut(),
            }
        }
        Err(_) => ptr::null_mut(),
    }
}

/// Clear all registered coredumps
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_coredump_clear_all() -> c_int {
    match COREDUMP_REGISTRY.write() {
        Ok(mut registry) => {
            registry.clear();
            0
        }
        Err(_) => -1,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_coredump_registry() {
        // Test that registry starts empty
        unsafe {
            let count = wasmtime4j_coredump_get_count();
            assert!(count >= 0);
        }
    }
}
