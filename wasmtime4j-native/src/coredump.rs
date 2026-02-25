//! Coredump registry for storing WasmCoreDump data extracted from trap errors.
//!
//! When `coredump_on_trap(true)` is configured on the engine, Wasmtime attaches
//! a `WasmCoreDump` to the error chain when a trap occurs. This module provides
//! a thread-safe global registry to store those errors (which contain the coredump)
//! so they can be queried from Java via FFI.

use std::collections::HashMap;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::RwLock;

use once_cell::sync::Lazy;

/// Global coredump registry storing wasmtime::Error objects that contain WasmCoreDump.
static REGISTRY: Lazy<RwLock<HashMap<u64, CoredumpEntry>>> =
    Lazy::new(|| RwLock::new(HashMap::new()));

/// Monotonically increasing counter for coredump IDs.
static COUNTER: AtomicU64 = AtomicU64::new(1);

/// Entry in the coredump registry.
struct CoredumpEntry {
    /// The original wasmtime::Error that contains the WasmCoreDump in its error chain.
    /// We keep the full error to allow accessing the WasmCoreDump via downcast_ref.
    error: wasmtime::Error,
    /// Cached trap message for quick access without locking the error.
    trap_message: String,
}

/// Register a wasmtime::Error that contains a WasmCoreDump in its error chain.
///
/// Returns the registry ID for later retrieval, or None if the error doesn't
/// contain a WasmCoreDump.
pub fn register_error(error: wasmtime::Error, trap_message: String) -> u64 {
    let id = COUNTER.fetch_add(1, Ordering::Relaxed);
    let entry = CoredumpEntry {
        error,
        trap_message,
    };
    let mut registry = REGISTRY.write().unwrap_or_else(|e| e.into_inner());
    registry.insert(id, entry);
    id
}

/// Execute a closure with a borrowed reference to the WasmCoreDump.
///
/// Returns None if the ID is not found or the error no longer contains a WasmCoreDump.
pub fn with_coredump<F, R>(id: u64, f: F) -> Option<R>
where
    F: FnOnce(&wasmtime::WasmCoreDump) -> R,
{
    let registry = REGISTRY.read().unwrap_or_else(|e| e.into_inner());
    let entry = registry.get(&id)?;
    let coredump = entry.error.downcast_ref::<wasmtime::WasmCoreDump>()?;
    Some(f(coredump))
}

/// Get the cached trap message for a coredump entry.
pub fn get_trap_message(id: u64) -> Option<String> {
    let registry = REGISTRY.read().unwrap_or_else(|e| e.into_inner());
    registry.get(&id).map(|e| e.trap_message.clone())
}

/// Remove a coredump entry from the registry, freeing its resources.
///
/// Returns true if the entry was found and removed.
pub fn remove(id: u64) -> bool {
    let mut registry = REGISTRY.write().unwrap_or_else(|e| e.into_inner());
    registry.remove(&id).is_some()
}

/// Remove all coredump entries from the registry.
pub fn clear_all() {
    let mut registry = REGISTRY.write().unwrap_or_else(|e| e.into_inner());
    registry.clear();
}

/// Get the number of coredump entries currently in the registry.
pub fn count() -> usize {
    let registry = REGISTRY.read().unwrap_or_else(|e| e.into_inner());
    registry.len()
}

/// Get all coredump IDs currently in the registry.
pub fn all_ids() -> Vec<u64> {
    let registry = REGISTRY.read().unwrap_or_else(|e| e.into_inner());
    registry.keys().copied().collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_register_and_retrieve() {
        // Create a simple error (without actual WasmCoreDump)
        let error = wasmtime::Error::msg("test trap error");
        let id = register_error(error, "test trap".to_string());

        assert!(id > 0);
        assert_eq!(get_trap_message(id), Some("test trap".to_string()));

        // with_coredump returns None because there's no WasmCoreDump in the chain
        let result: Option<usize> = with_coredump(id, |_cd| 0);
        assert!(result.is_none());

        // Cleanup
        assert!(remove(id));
        assert!(!remove(id)); // Already removed
    }

    #[test]
    fn test_clear_all() {
        let e1 = wasmtime::Error::msg("error 1");
        let e2 = wasmtime::Error::msg("error 2");
        let id1 = register_error(e1, "trap 1".to_string());
        let id2 = register_error(e2, "trap 2".to_string());

        assert!(count() >= 2);
        clear_all();
        assert_eq!(get_trap_message(id1), None);
        assert_eq!(get_trap_message(id2), None);
    }

    #[test]
    fn test_all_ids() {
        clear_all();
        let e1 = wasmtime::Error::msg("a");
        let e2 = wasmtime::Error::msg("b");
        let id1 = register_error(e1, "a".to_string());
        let id2 = register_error(e2, "b".to_string());

        let ids = all_ids();
        assert!(ids.contains(&id1));
        assert!(ids.contains(&id2));

        clear_all();
    }
}
