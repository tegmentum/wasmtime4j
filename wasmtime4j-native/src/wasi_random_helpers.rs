//! Shared helper functions for WASI random operations
//!
//! This module provides common functionality used by both JNI and Panama FFI bindings
//! for WASI Preview 2 random operations.

use crate::error::WasmtimeResult;
use crate::wasi_preview2::WasiPreview2Context;
use rand::RngCore;

/// Get cryptographically-secure random bytes
///
/// Returns a vector of random bytes of the specified length.
/// This function uses a cryptographically-secure random number generator
/// and never blocks.
///
/// # Parameters
/// - `context`: The WASI Preview 2 context
/// - `len`: Number of random bytes to generate
///
/// # Returns
/// Vector of random bytes
pub fn get_random_bytes(_context: &WasiPreview2Context, len: u64) -> WasmtimeResult<Vec<u8>> {
    // MVP: Use system CSPRNG
    // TODO: Replace with actual Wasmtime random API
    let mut bytes = vec![0u8; len as usize];
    let mut rng = rand::thread_rng();
    rng.fill_bytes(&mut bytes);
    Ok(bytes)
}

/// Get a cryptographically-secure random u64 value
///
/// Returns a random unsigned 64-bit integer using the same
/// cryptographically-secure RNG as get_random_bytes.
///
/// # Parameters
/// - `context`: The WASI Preview 2 context
///
/// # Returns
/// Random u64 value
pub fn get_random_u64(_context: &WasiPreview2Context) -> WasmtimeResult<u64> {
    // MVP: Use system CSPRNG
    // TODO: Replace with actual Wasmtime random API
    let mut rng = rand::thread_rng();
    Ok(rng.next_u64())
}
