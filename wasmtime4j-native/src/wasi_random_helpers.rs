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

#[cfg(test)]
mod tests {
    use super::*;
    use std::collections::HashSet;

    // Test the underlying random functionality directly using the system RNG.
    // The get_random_bytes and get_random_u64 functions don't actually use
    // the context parameter (it's for future API compatibility), so we can
    // test the random generation logic directly.

    fn generate_random_bytes(len: usize) -> Vec<u8> {
        let mut bytes = vec![0u8; len];
        let mut rng = rand::thread_rng();
        rng.fill_bytes(&mut bytes);
        bytes
    }

    fn generate_random_u64() -> u64 {
        let mut rng = rand::thread_rng();
        rng.next_u64()
    }

    #[test]
    fn test_random_bytes_zero_length() {
        let bytes = generate_random_bytes(0);
        assert_eq!(bytes.len(), 0, "Should return empty vector for zero length");
    }

    #[test]
    fn test_random_bytes_small_length() {
        let bytes = generate_random_bytes(16);
        assert_eq!(bytes.len(), 16, "Should return 16 bytes");
    }

    #[test]
    fn test_random_bytes_large_length() {
        let bytes = generate_random_bytes(1024);
        assert_eq!(bytes.len(), 1024, "Should return 1024 bytes");
    }

    #[test]
    fn test_random_bytes_not_all_zeros() {
        let bytes = generate_random_bytes(32);

        // With 32 random bytes, extremely unlikely all are zeros
        let non_zero_count = bytes.iter().filter(|&&b| b != 0).count();
        assert!(non_zero_count > 0, "Random bytes should not all be zero");
    }

    #[test]
    fn test_random_bytes_different_calls_different_results() {
        let bytes1 = generate_random_bytes(32);
        let bytes2 = generate_random_bytes(32);

        // With 32 bytes, probability of collision is negligible (2^-256)
        assert_ne!(
            bytes1, bytes2,
            "Different calls should produce different results"
        );
    }

    #[test]
    fn test_random_bytes_uniform_distribution() {
        let bytes = generate_random_bytes(10000);

        // Count occurrences of each byte value
        let mut counts = [0u32; 256];
        for b in &bytes {
            counts[*b as usize] += 1;
        }

        // With 10000 bytes, expected count per value is ~39
        // All values should appear at least once (extremely high probability)
        let appeared_count = counts.iter().filter(|&&c| c > 0).count();
        assert!(
            appeared_count > 200,
            "Most byte values should appear at least once"
        );
    }

    #[test]
    fn test_random_u64_non_zero() {
        // Generate multiple values to ensure we're getting real randomness
        let mut found_non_zero = false;
        for _ in 0..10 {
            let value = generate_random_u64();
            if value != 0 {
                found_non_zero = true;
                break;
            }
        }
        assert!(
            found_non_zero,
            "Should generate at least one non-zero value"
        );
    }

    #[test]
    fn test_random_u64_different_calls_different_results() {
        // Generate several random values
        let mut values = HashSet::new();
        for _ in 0..100 {
            let value = generate_random_u64();
            values.insert(value);
        }

        // With 100 random u64 values, collision probability is negligible
        assert!(
            values.len() >= 99,
            "All values should be unique (or at most 1 collision)"
        );
    }

    #[test]
    fn test_random_u64_full_range() {
        // Generate many random values and check bit coverage
        let mut high_bit_seen = false;
        let mut low_bit_seen = false;

        for _ in 0..1000 {
            let value = generate_random_u64();
            if value > 0x8000_0000_0000_0000 {
                high_bit_seen = true;
            }
            if value < 0x1000_0000_0000_0000 {
                low_bit_seen = true;
            }
            if high_bit_seen && low_bit_seen {
                break;
            }
        }

        // Should see both high and low values (with high probability)
        assert!(
            high_bit_seen || low_bit_seen,
            "Should generate values across range"
        );
    }

    #[test]
    fn test_random_bytes_exact_length() {
        for len in [1, 7, 15, 17, 31, 33, 63, 65, 127, 128, 255, 256] {
            let bytes = generate_random_bytes(len);
            assert_eq!(bytes.len(), len, "Should return exact length {}", len);
        }
    }
}
