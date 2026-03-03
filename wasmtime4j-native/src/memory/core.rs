//! Core FFI-compatible functions for Memory operations
//!
//! This module provides functions that can be safely called from both JNI and Panama FFI
//! implementations. These functions eliminate code duplication and provide consistent behavior
//! across interface implementations while maintaining defensive programming practices.

use std::collections::HashSet;
use std::os::raw::c_void;
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, RwLock};

use crate::error::{ffi_utils, WasmtimeError, WasmtimeResult};
use crate::store::Store;

use super::types::{MemoryError, MemoryVariant};
use super::Memory;

/// Memory handle validation magic number for integrity checking
const MEMORY_MAGIC: u64 = 0xDEADBEEF_CAFEBABE;

/// Thread-safe registry for tracking valid memory handles (using usize for thread safety)
static VALID_MEMORY_HANDLES: std::sync::LazyLock<Arc<RwLock<HashSet<usize>>>> =
    std::sync::LazyLock::new(|| Arc::new(RwLock::new(HashSet::new())));

/// Thread-safe registry for tracking valid store handles (using usize for thread safety)
static VALID_STORE_HANDLES: std::sync::LazyLock<Arc<RwLock<HashSet<usize>>>> =
    std::sync::LazyLock::new(|| Arc::new(RwLock::new(HashSet::new())));

/// Memory access counter for detecting potential race conditions
static MEMORY_ACCESS_COUNTER: AtomicU64 = AtomicU64::new(0);

/// Wrapper for memory with validation metadata
#[repr(C)]
pub struct ValidatedMemory {
    magic: u64,
    pub(crate) memory: Memory,
    access_count: AtomicU64,
    is_destroyed: std::sync::atomic::AtomicBool,
}

impl ValidatedMemory {
    /// Create a new validated memory wrapper
    pub fn new(memory: Memory) -> Self {
        Self {
            magic: MEMORY_MAGIC,
            memory,
            access_count: AtomicU64::new(0),
            is_destroyed: std::sync::atomic::AtomicBool::new(false),
        }
    }

    /// Check if this memory instance is valid
    pub fn is_valid(&self) -> bool {
        self.magic == MEMORY_MAGIC && !self.is_destroyed.load(Ordering::Acquire)
    }

    /// Mark this memory instance as destroyed
    pub fn mark_destroyed(&self) {
        self.is_destroyed.store(true, Ordering::Release);
    }

    /// Increment access counter and return memory reference
    pub fn access_memory(&self) -> WasmtimeResult<&Memory> {
        if !self.is_valid() {
            return Err(WasmtimeError::InvalidParameter {
                message: "Attempt to access destroyed or corrupted memory".to_string(),
            });
        }

        self.access_count.fetch_add(1, Ordering::Relaxed);
        MEMORY_ACCESS_COUNTER.fetch_add(1, Ordering::Relaxed);
        Ok(&self.memory)
    }

    /// Get access statistics
    pub fn get_access_count(&self) -> u64 {
        self.access_count.load(Ordering::Relaxed)
    }
}

/// Register a memory handle as valid
pub fn register_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
    let mut handles = VALID_MEMORY_HANDLES
        .write()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire memory handle registry lock".to_string(),
        })?;

    handles.insert(ptr as usize);
    log::debug!("Registered memory handle: {:p}", ptr);
    Ok(())
}

/// Unregister a memory handle
pub fn unregister_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
    let mut handles = VALID_MEMORY_HANDLES
        .write()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire memory handle registry lock".to_string(),
        })?;

    if handles.remove(&(ptr as usize)) {
        log::debug!("Unregistered memory handle: {:p}", ptr);
        Ok(())
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: format!("Memory handle {:p} was not registered", ptr),
        })
    }
}

/// Register a store handle as valid
pub fn register_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
    let mut handles = VALID_STORE_HANDLES
        .write()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire store handle registry lock".to_string(),
        })?;

    handles.insert(ptr as usize);
    log::debug!("Registered store handle: {:p}", ptr);
    Ok(())
}

/// Unregister a store handle
pub fn unregister_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
    let mut handles = VALID_STORE_HANDLES
        .write()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire store handle registry lock".to_string(),
        })?;

    if handles.remove(&(ptr as usize)) {
        log::debug!("Unregistered store handle: {:p}", ptr);
        Ok(())
    } else {
        Err(WasmtimeError::InvalidParameter {
            message: format!("Store handle {:p} was not registered", ptr),
        })
    }
}

/// Clear all handle registries (for testing purposes)
///
/// This function clears both memory and store handle registries.
/// It should only be used in test teardown to prevent stale handles
/// from interfering with subsequent tests.
///
/// # Safety
/// Calling this function while handles are still in use will cause
/// validation errors when those handles are accessed.
pub fn clear_handle_registries() -> WasmtimeResult<()> {
    // Clear memory handles
    let mut memory_handles =
        VALID_MEMORY_HANDLES
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire memory handle registry lock".to_string(),
            })?;
    let memory_count = memory_handles.len();
    memory_handles.clear();
    drop(memory_handles);

    // Clear store handles
    let mut store_handles =
        VALID_STORE_HANDLES
            .write()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire store handle registry lock".to_string(),
            })?;
    let store_count = store_handles.len();
    store_handles.clear();
    drop(store_handles);

    log::debug!(
        "Cleared handle registries: {} memory handles, {} store handles",
        memory_count,
        store_count
    );
    Ok(())
}

/// Validate memory handle with comprehensive checks
pub unsafe fn validate_memory_handle(ptr: *const c_void) -> WasmtimeResult<()> {
    // Basic null check
    if ptr.is_null() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Memory pointer cannot be null".to_string(),
        });
    }

    // Check if handle is registered
    let handles = VALID_MEMORY_HANDLES
        .read()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire memory handle registry lock".to_string(),
        })?;

    if !handles.contains(&(ptr as usize)) {
        return Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Memory handle {:p} is not registered or has been freed",
                ptr
            ),
        });
    }

    // Check magic number and validity if using ValidatedMemory
    let validated_memory = &*(ptr as *const ValidatedMemory);
    if validated_memory.magic != MEMORY_MAGIC {
        return Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Memory handle {:p} has invalid magic number (corrupted)",
                ptr
            ),
        });
    }

    if validated_memory.is_destroyed.load(Ordering::Acquire) {
        return Err(WasmtimeError::InvalidParameter {
            message: format!(
                "Memory handle {:p} has been destroyed (use-after-free attempt)",
                ptr
            ),
        });
    }

    Ok(())
}

/// Validate store handle with comprehensive checks
pub unsafe fn validate_store_handle(ptr: *const c_void) -> WasmtimeResult<()> {
    // Basic null check
    if ptr.is_null() {
        return Err(WasmtimeError::InvalidParameter {
            message: "Store pointer cannot be null".to_string(),
        });
    }

    // Check if handle is registered
    let handles = VALID_STORE_HANDLES
        .read()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire store handle registry lock".to_string(),
        })?;

    if !handles.contains(&(ptr as usize)) {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Store handle {:p} is not registered or has been freed", ptr),
        });
    }

    Ok(())
}

/// Get memory from pointer with comprehensive validation
pub unsafe fn get_memory_ref(ptr: *const c_void) -> WasmtimeResult<&'static Memory> {
    validate_memory_handle(ptr)?;

    let validated_memory = &*(ptr as *const ValidatedMemory);
    validated_memory.access_memory()
}

/// Get mutable memory from pointer with comprehensive validation
pub unsafe fn get_memory_mut(ptr: *mut c_void) -> WasmtimeResult<&'static mut Memory> {
    validate_memory_handle(ptr)?;

    let validated_memory = &mut *(ptr as *mut ValidatedMemory);
    let _memory_ref = validated_memory.access_memory()?; // Validate first
    Ok(&mut validated_memory.memory)
}

/// Get store from pointer with validation (read-only)
pub unsafe fn get_store_ref(ptr: *const c_void) -> WasmtimeResult<&'static Store> {
    validate_store_handle(ptr)?;
    ffi_utils::deref_ptr::<Store>(ptr, "store")
}

/// Get mutable store from pointer with validation
pub unsafe fn get_store_mut(ptr: *mut c_void) -> WasmtimeResult<&'static mut Store> {
    validate_store_handle(ptr as *const c_void)?;
    ffi_utils::deref_ptr_mut::<Store>(ptr, "store")
}

/// Core function to get memory size in bytes
pub fn get_memory_size(memory: &Memory, store: &Store) -> WasmtimeResult<usize> {
    memory.size_bytes(store)
}

/// Core function to grow memory by pages
pub fn grow_memory(memory: &Memory, store: &mut Store, pages: u64) -> WasmtimeResult<u64> {
    memory.grow(store, pages)
}

/// Core function to grow memory asynchronously
///
/// Requires engine with `async_support(true)`. Uses the tokio runtime to bridge
/// the sync FFI call to wasmtime's async memory growth which goes through the
/// async resource limiter.
#[cfg(feature = "async")]
pub fn grow_memory_async(memory: &Memory, store: &mut Store, pages: u64) -> WasmtimeResult<u64> {
    let wasmtime_mem = match &memory.inner {
        super::types::MemoryVariant::Regular(mem) => mem,
        super::types::MemoryVariant::Shared(_) => {
            return Err(WasmtimeError::InvalidParameter {
                message: "Async growth not supported for shared memory".to_string(),
            });
        }
    };
    let mem = *wasmtime_mem;
    let handle = crate::async_runtime::get_runtime_handle();
    let mut store_guard = store.try_lock_store()?;
    handle
        .block_on(async { mem.grow_async(&mut *store_guard, pages).await })
        .map_err(|e| WasmtimeError::Memory {
            message: format!("Async memory growth failed: {}", e),
        })
}

/// Core function to get memory page count
pub fn get_memory_page_count(memory: &Memory, store: &Store) -> WasmtimeResult<u64> {
    memory.size_pages(store)
}

/// Core function to get memory page size in bytes
///
/// Returns the page size for this memory, which is normally 65536 but can differ
/// when the `wasm_custom_page_sizes` engine feature is enabled.
pub fn get_memory_page_size(memory: &Memory) -> u64 {
    memory.memory_type.page_size()
}

/// Core function to get memory page size as log2
///
/// Returns the log2 of the page size (normally 16 for 65536 byte pages).
pub fn get_memory_page_size_log2(memory: &Memory) -> u32 {
    memory.memory_type.page_size_log2() as u32
}

/// Core function to get memory data size in bytes
///
/// Returns the current data size of the memory (pages * page_size).
pub fn get_memory_data_size(memory: &Memory, store: &Store) -> WasmtimeResult<usize> {
    memory.data_size(store)
}

/// Core function to read bytes from memory
pub fn read_memory_bytes(
    memory: &Memory,
    store: &Store,
    offset: usize,
    length: usize,
) -> WasmtimeResult<Vec<u8>> {
    memory.read_bytes(store, offset, length)
}

/// Core function to write bytes to memory
pub fn write_memory_bytes(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    data: &[u8],
) -> WasmtimeResult<()> {
    memory.write_bytes(store, offset, data)
}

/// Core function to read a single byte from memory
pub fn read_memory_byte(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<u8> {
    let bytes = memory.read_bytes(store, offset, 1)?;
    Ok(bytes[0])
}

/// Core function to write a single byte to memory
pub fn write_memory_byte(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: u8,
) -> WasmtimeResult<()> {
    memory.write_bytes(store, offset, &[value])
}

/// Core function to get direct memory buffer access
pub fn get_memory_buffer(memory: &Memory, store: &Store) -> WasmtimeResult<(*mut u8, usize)> {
    match memory.variant() {
        MemoryVariant::Regular(mem) => store.with_context_ro(|ctx| {
            let data = mem.data(ctx);
            let ptr = data.as_ptr() as *mut u8;
            let len = data.len();
            Ok((ptr, len))
        }),
        MemoryVariant::Shared(mem) => {
            let data = mem.data();
            let ptr = data.as_ptr() as *mut u8;
            let len = data.len();
            Ok((ptr, len))
        }
    }
}

/// Create a validated memory instance with handle registration
pub fn create_validated_memory(memory: Memory) -> WasmtimeResult<*mut ValidatedMemory> {
    let validated = ValidatedMemory::new(memory);
    let boxed = Box::new(validated);
    let ptr = Box::into_raw(boxed);

    // Register the handle
    register_memory_handle(ptr as *const c_void)?;

    log::debug!("Created validated memory handle: {:p}", ptr);
    Ok(ptr)
}

/// Core function to destroy memory with proper cleanup and validation
pub unsafe fn destroy_memory(ptr: *mut c_void) {
    if ptr.is_null() {
        log::warn!("Attempted to destroy null memory pointer");
        return;
    }

    // First try to validate the handle (this might fail if already destroyed)
    match validate_memory_handle(ptr) {
        Ok(_) => {
            // Mark as destroyed first to prevent further access
            let validated_memory = &*(ptr as *const ValidatedMemory);
            validated_memory.mark_destroyed();

            // Unregister the handle
            if let Err(e) = unregister_memory_handle(ptr) {
                log::error!("Failed to unregister memory handle {:p}: {}", ptr, e);
            }

            // Finally, deallocate the memory
            let _ = Box::from_raw(ptr as *mut ValidatedMemory);
            log::debug!("Destroyed validated memory handle: {:p}", ptr);
        }
        Err(e) => {
            log::warn!(
                "Attempted to destroy invalid memory handle {:p}: {}",
                ptr,
                e
            );
            // Still attempt cleanup in case it's a partially corrupted handle
            match unregister_memory_handle(ptr) {
                Ok(_) => {
                    let _ = Box::from_raw(ptr as *mut ValidatedMemory);
                    log::debug!(
                        "Destroyed corrupted but registered memory handle: {:p}",
                        ptr
                    );
                }
                Err(unregister_err) => {
                    log::debug!(
                        "Handle was not registered (expected for corrupted handle): {}",
                        unregister_err
                    );
                }
            }
        }
    }
}

// Shared Memory Operations

/// Core function to check if memory is shared
pub fn memory_is_shared(memory: &Memory, store: &Store) -> WasmtimeResult<bool> {
    match memory.variant() {
        MemoryVariant::Regular(mem) => store.with_context_ro(|ctx| {
            let memory_type = mem.ty(ctx);
            Ok(memory_type.is_shared())
        }),
        MemoryVariant::Shared(_) => Ok(true),
    }
}

/// Validates shared memory, alignment, and bounds for atomic operations.
fn validate_atomic_access(
    memory: &Memory,
    store: &Store,
    offset: usize,
    size: usize,
) -> WasmtimeResult<()> {
    if !memory_is_shared(memory, store)? {
        return Err(WasmtimeError::Memory {
            message: "Atomic operations require shared memory".to_string(),
        });
    }
    if offset % size != 0 {
        return Err(WasmtimeError::Memory {
            message: format!("Offset {} is not aligned to {}-byte boundary", offset, size),
        });
    }
    let memory_size = memory.size_bytes(store)?;
    if offset + size > memory_size {
        return Err(MemoryError::BoundsViolation {
            offset,
            length: size,
            memory_size,
        }
        .into());
    }
    Ok(())
}

/// Core function for atomic compare-and-swap on 32-bit value
pub fn atomic_compare_and_swap_i32(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    expected: i32,
    new_value: i32,
) -> WasmtimeResult<i32> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe {
            let result = (*atomic_ptr).compare_exchange(
                expected,
                new_value,
                std::sync::atomic::Ordering::SeqCst,
                std::sync::atomic::Ordering::SeqCst,
            );
            Ok(result.unwrap_or_else(|v| v))
        }
    })
}

/// Core function for atomic compare-and-swap on 64-bit value
pub fn atomic_compare_and_swap_i64(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    expected: i64,
    new_value: i64,
) -> WasmtimeResult<i64> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe {
            let result = (*atomic_ptr).compare_exchange(
                expected,
                new_value,
                std::sync::atomic::Ordering::SeqCst,
                std::sync::atomic::Ordering::SeqCst,
            );
            Ok(result.unwrap_or_else(|v| v))
        }
    })
}

/// Core function for atomic load on 32-bit value
pub fn atomic_load_i32(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<i32> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context_ro(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_readonly(&*ctx);
        let memory_data = unsafe { std::slice::from_raw_parts(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe { Ok((*atomic_ptr).load(std::sync::atomic::Ordering::Acquire)) }
    })
}

/// Core function for atomic load on 64-bit value
pub fn atomic_load_i64(memory: &Memory, store: &Store, offset: usize) -> WasmtimeResult<i64> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context_ro(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_readonly(&*ctx);
        let memory_data = unsafe { std::slice::from_raw_parts(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe { Ok((*atomic_ptr).load(std::sync::atomic::Ordering::Acquire)) }
    })
}

/// Core function for atomic store on 32-bit value
pub fn atomic_store_i32(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i32,
) -> WasmtimeResult<()> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe { (*atomic_ptr).store(value, std::sync::atomic::Ordering::Release) };
        Ok(())
    })
}

/// Core function for atomic store on 64-bit value
pub fn atomic_store_i64(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i64,
) -> WasmtimeResult<()> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe { (*atomic_ptr).store(value, std::sync::atomic::Ordering::Release) };
        Ok(())
    })
}

/// Core function for atomic add on 32-bit value
pub fn atomic_add_i32(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i32,
) -> WasmtimeResult<i32> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe { Ok((*atomic_ptr).fetch_add(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic add on 64-bit value
pub fn atomic_add_i64(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i64,
) -> WasmtimeResult<i64> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe { Ok((*atomic_ptr).fetch_add(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic bitwise AND on 32-bit value
pub fn atomic_and_i32(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i32,
) -> WasmtimeResult<i32> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe { Ok((*atomic_ptr).fetch_and(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic bitwise OR on 32-bit value
pub fn atomic_or_i32(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i32,
) -> WasmtimeResult<i32> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe { Ok((*atomic_ptr).fetch_or(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic bitwise XOR on 32-bit value
pub fn atomic_xor_i32(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i32,
) -> WasmtimeResult<i32> {
    validate_atomic_access(memory, store, offset, 4)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI32;
        unsafe { Ok((*atomic_ptr).fetch_xor(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic bitwise AND on 64-bit value
pub fn atomic_and_i64(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i64,
) -> WasmtimeResult<i64> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe { Ok((*atomic_ptr).fetch_and(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic bitwise OR on 64-bit value
pub fn atomic_or_i64(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i64,
) -> WasmtimeResult<i64> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe { Ok((*atomic_ptr).fetch_or(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic bitwise XOR on 64-bit value
pub fn atomic_xor_i64(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: i64,
) -> WasmtimeResult<i64> {
    validate_atomic_access(memory, store, offset, 8)?;
    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let atomic_ptr = memory_data[offset..].as_ptr() as *const std::sync::atomic::AtomicI64;
        unsafe { Ok((*atomic_ptr).fetch_xor(value, std::sync::atomic::Ordering::SeqCst)) }
    })
}

/// Core function for atomic memory fence
pub fn atomic_fence(_memory: &Memory, store: &Store) -> WasmtimeResult<()> {
    if !memory_is_shared(_memory, store)? {
        return Err(WasmtimeError::Memory {
            message: "Atomic operations require shared memory".to_string(),
        });
    }
    std::sync::atomic::fence(std::sync::atomic::Ordering::SeqCst);
    Ok(())
}

/// Core function for atomic notify (wake threads waiting on a memory location)
pub fn atomic_notify(
    memory: &Memory,
    store: &Store,
    offset: usize,
    count: i32,
) -> WasmtimeResult<i32> {
    let shared_mem = memory.inner_shared().ok_or_else(|| WasmtimeError::Memory {
        message: "Atomic operations require shared memory".to_string(),
    })?;
    validate_atomic_access(memory, store, offset, 4)?;
    shared_mem
        .atomic_notify(offset as u64, count as u32)
        .map(|n| n as i32)
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("atomic_notify failed: {}", e),
            backtrace: None,
        })
}

/// Core function for atomic wait on 32-bit value
pub fn atomic_wait32(
    memory: &Memory,
    store: &Store,
    offset: usize,
    expected: i32,
    timeout_nanos: i64,
) -> WasmtimeResult<i32> {
    let shared_mem = memory.inner_shared().ok_or_else(|| WasmtimeError::Memory {
        message: "Atomic operations require shared memory".to_string(),
    })?;
    validate_atomic_access(memory, store, offset, 4)?;
    let timeout = if timeout_nanos < 0 {
        None
    } else {
        Some(std::time::Duration::from_nanos(timeout_nanos as u64))
    };
    shared_mem
        .atomic_wait32(offset as u64, expected as u32, timeout)
        .map(|r| r as i32)
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("atomic_wait32 failed: {}", e),
            backtrace: None,
        })
}

/// Core function for atomic wait on 64-bit value
pub fn atomic_wait64(
    memory: &Memory,
    store: &Store,
    offset: usize,
    expected: i64,
    timeout_nanos: i64,
) -> WasmtimeResult<i32> {
    let shared_mem = memory.inner_shared().ok_or_else(|| WasmtimeError::Memory {
        message: "Atomic operations require shared memory".to_string(),
    })?;
    validate_atomic_access(memory, store, offset, 8)?;
    let timeout = if timeout_nanos < 0 {
        None
    } else {
        Some(std::time::Duration::from_nanos(timeout_nanos as u64))
    };
    shared_mem
        .atomic_wait64(offset as u64, expected as u64, timeout)
        .map(|r| r as i32)
        .map_err(|e| WasmtimeError::Runtime {
            message: format!("atomic_wait64 failed: {}", e),
            backtrace: None,
        })
}

// Bulk Memory Operations

/// Core function to copy memory within the same memory instance
pub fn memory_copy(
    memory: &Memory,
    store: &mut Store,
    dest_offset: usize,
    src_offset: usize,
    len: usize,
) -> WasmtimeResult<()> {
    let memory_size = memory.size_bytes(store)?;
    if dest_offset.saturating_add(len) > memory_size || src_offset.saturating_add(len) > memory_size
    {
        return Err(MemoryError::BoundsViolation {
            offset: std::cmp::max(dest_offset, src_offset),
            length: len,
            memory_size,
        }
        .into());
    }

    match &memory.inner {
        MemoryVariant::Regular(mem) => store.with_context(|ctx| {
            let memory_data = mem.data_mut(ctx);

            if src_offset < dest_offset && src_offset + len > dest_offset {
                // dest > src with overlap: iterate backward to avoid overwriting source
                for i in (0..len).rev() {
                    memory_data[dest_offset + i] = memory_data[src_offset + i];
                }
            } else if dest_offset < src_offset && dest_offset + len > src_offset {
                // dest < src with overlap: iterate forward
                for i in 0..len {
                    memory_data[dest_offset + i] = memory_data[src_offset + i];
                }
            } else {
                memory_data.copy_within(src_offset..src_offset + len, dest_offset);
            }
            Ok(())
        }),
        MemoryVariant::Shared(mem) => {
            let mem_data = mem.data();

            if src_offset < dest_offset && src_offset + len > dest_offset {
                // dest > src with overlap: iterate backward to avoid overwriting source
                for i in (0..len).rev() {
                    unsafe {
                        let val = *mem_data[src_offset + i].get();
                        *mem_data[dest_offset + i].get() = val;
                    }
                }
            } else if dest_offset < src_offset && dest_offset + len > src_offset {
                // dest < src with overlap: iterate forward
                for i in 0..len {
                    unsafe {
                        let val = *mem_data[src_offset + i].get();
                        *mem_data[dest_offset + i].get() = val;
                    }
                }
            } else {
                for i in 0..len {
                    unsafe {
                        let val = *mem_data[src_offset + i].get();
                        *mem_data[dest_offset + i].get() = val;
                    }
                }
            }
            Ok(())
        }
    }
}

/// Core function to fill memory with a specific byte value
pub fn memory_fill(
    memory: &Memory,
    store: &mut Store,
    offset: usize,
    value: u8,
    len: usize,
) -> WasmtimeResult<()> {
    let memory_size = memory.size_bytes(store)?;
    if offset.saturating_add(len) > memory_size {
        return Err(MemoryError::BoundsViolation {
            offset,
            length: len,
            memory_size,
        }
        .into());
    }

    match &memory.inner {
        MemoryVariant::Regular(mem) => store.with_context(|ctx| {
            let memory_data = mem.data_mut(ctx);
            memory_data[offset..offset + len].fill(value);
            Ok(())
        }),
        MemoryVariant::Shared(mem) => {
            let mem_data = mem.data();
            for i in 0..len {
                unsafe { *mem_data[offset + i].get() = value };
            }
            Ok(())
        }
    }
}

/// Initialize memory from a data segment
pub fn memory_init(
    memory: &Memory,
    store: &Store,
    instance: &crate::instance::Instance,
    dest_offset: u32,
    data_segment_index: u32,
    src_offset: u32,
    len: u32,
) -> WasmtimeResult<()> {
    let segment_manager = instance.get_data_segment_manager();

    let memory_size = store.with_context_ro(|ctx| Ok(memory.variant().data_size(&ctx)))?;

    if (dest_offset as u64).saturating_add(len as u64) > memory_size as u64 {
        return Err(WasmtimeError::Runtime {
            message: format!(
                "memory.init destination would exceed bounds: dest={}, len={}, memory_size={}",
                dest_offset, len, memory_size
            ),
            backtrace: None,
        });
    }

    let data = segment_manager.get_data(data_segment_index, src_offset, len)?;

    store.with_context(|ctx| {
        let (data_ptr, data_len) = memory.variant().data_ptr_for_atomics(&mut *ctx);
        let memory_data = unsafe { std::slice::from_raw_parts_mut(data_ptr, data_len) };
        let dest_start = dest_offset as usize;
        let dest_end = dest_start + len as usize;
        memory_data[dest_start..dest_end].copy_from_slice(&data);
        Ok(())
    })
}

/// Drop a data segment
pub fn data_drop(
    instance: &crate::instance::Instance,
    data_segment_index: u32,
) -> WasmtimeResult<()> {
    let segment_manager = instance.get_data_segment_manager();
    segment_manager.drop_segment(data_segment_index)
}

/// Get diagnostic information about memory handle validation
pub fn get_memory_handle_diagnostics() -> WasmtimeResult<(usize, u64)> {
    let handles = VALID_MEMORY_HANDLES
        .read()
        .map_err(|_| WasmtimeError::Concurrency {
            message: "Failed to acquire memory handle registry lock".to_string(),
        })?;

    let handle_count = handles.len();
    let total_accesses = MEMORY_ACCESS_COUNTER.load(Ordering::Relaxed);

    Ok((handle_count, total_accesses))
}
