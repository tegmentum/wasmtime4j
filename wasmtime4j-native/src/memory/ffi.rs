//! FFI export functions for JNI and Panama bindings
//!
//! This module provides C-compatible functions that can be called from
//! both JNI and Panama FFI implementations.

// Platform memory FFI functions removed (Phase 12: dead PlatformMemory infrastructure)

/// Clears all handle registries (for testing purposes)
///
/// This function clears both memory and store handle registries to prevent
/// stale handles from interfering with subsequent tests. Should only be
/// called in test teardown after all handles have been properly destroyed.
///
/// # Returns
/// 0 on success, negative error code on failure
#[no_mangle]
pub extern "C" fn wasmtime4j_memory_clear_handle_registries() -> i32 {
    match super::core::clear_handle_registries() {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Clears the destroyed pointers registry (for testing purposes)
///
/// This function clears the HashSet tracking destroyed pointers to prevent
/// unbounded memory growth during large test suite execution.
/// Should only be called in test teardown after all native resources
/// have been properly destroyed.
///
/// # Returns
/// The number of entries cleared from the registry
#[no_mangle]
pub extern "C" fn wasmtime4j_clear_destroyed_pointers() -> u64 {
    crate::error::ffi_utils::clear_destroyed_pointers() as u64
}
