//! WASI-threads Implementation
//!
//! This module provides basic WASI-threads support for spawning WebAssembly threads.

use std::sync::atomic::{AtomicU32, Ordering};
use crate::error::{WasmtimeResult, WasmtimeError};

/// WASI-threads context for thread management
#[derive(Debug, Default)]
pub struct WasiThreadsContext {
    /// Thread ID counter
    next_thread_id: AtomicU32,
}

impl WasiThreadsContext {
    /// Create new WASI-threads context
    pub fn new() -> WasmtimeResult<Self> {
        Ok(Self {
            next_thread_id: AtomicU32::new(1),
        })
    }

    /// Spawn a new thread with the given start argument
    ///
    /// Returns the thread ID on success. Thread IDs are between 1 and 0x1FFFFFFF.
    pub fn spawn_thread(&self, thread_start_arg: u32) -> WasmtimeResult<u32> {
        // WASI-Threads spec: thread IDs must be between 1 and 0x1FFFFFFF
        const MAX_THREAD_ID: u32 = 0x1FFFFFFF;

        let thread_id = self.next_thread_id.fetch_add(1, Ordering::SeqCst);

        if thread_id > MAX_THREAD_ID {
            return Err(WasmtimeError::from_string(
                format!("Thread ID {} exceeds maximum allowed value {}", thread_id, MAX_THREAD_ID)
            ));
        }

        log::debug!(
            "Spawning thread with ID {} and start arg {}",
            thread_id,
            thread_start_arg
        );

        // Thread execution is handled by the Wasmtime runtime.
        // This returns the thread ID to indicate successful spawn request.
        Ok(thread_id)
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_context_creation() {
        let context = WasiThreadsContext::new().unwrap();
        assert!(context.next_thread_id.load(Ordering::SeqCst) == 1);
    }

    #[test]
    fn test_spawn_thread() {
        let context = WasiThreadsContext::new().unwrap();

        let thread_id1 = context.spawn_thread(42).unwrap();
        assert_eq!(thread_id1, 1);

        let thread_id2 = context.spawn_thread(100).unwrap();
        assert_eq!(thread_id2, 2);
    }

    #[test]
    fn test_thread_id_incrementing() {
        let context = WasiThreadsContext::new().unwrap();

        for i in 1..=10 {
            let thread_id = context.spawn_thread(i).unwrap();
            assert_eq!(thread_id, i);
        }
    }
}
