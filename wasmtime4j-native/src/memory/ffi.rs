//! FFI export functions for JNI and Panama bindings
//!
//! This module provides C-compatible functions that can be called from
//! both JNI and Panama FFI implementations.

use std::ffi::c_void;
use std::io::Write;
use std::ptr::NonNull;

use flate2::write::GzEncoder;
use flate2::Compression;
use log::error;

use super::platform::PlatformMemoryAllocator;
use super::types::{
    MemoryConfig, PlatformMemoryConfig, PlatformMemoryInfo, PlatformMemoryLeak,
    PlatformMemoryPoolStats,
};
use super::Memory;

/// Creates a new platform memory allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_allocator_create(
    config: *const PlatformMemoryConfig,
) -> *mut PlatformMemoryAllocator {
    let config = if config.is_null() {
        PlatformMemoryConfig::default()
    } else {
        unsafe { (*config).clone() }
    };

    match PlatformMemoryAllocator::new(config) {
        Ok(allocator) => Box::into_raw(Box::new(allocator)),
        Err(e) => {
            error!("Failed to create platform memory allocator: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Allocates memory with platform optimizations
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_allocate(
    allocator: *mut PlatformMemoryAllocator,
    size: usize,
    alignment: usize,
) -> *mut c_void {
    if allocator.is_null() || size == 0 {
        return std::ptr::null_mut();
    }

    let allocator = unsafe { &*allocator };
    let alignment = if alignment == 0 {
        None
    } else {
        Some(alignment)
    };

    match allocator.allocate(size, alignment) {
        Ok(ptr) => ptr.as_ptr(),
        Err(e) => {
            error!("Platform memory allocation failed: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Deallocates platform memory
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_deallocate(
    allocator: *mut PlatformMemoryAllocator,
    ptr: *mut c_void,
) -> bool {
    if allocator.is_null() || ptr.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };

    if let Some(non_null_ptr) = NonNull::new(ptr) {
        allocator.deallocate(non_null_ptr).is_ok()
    } else {
        false
    }
}

/// Gets platform memory statistics
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_get_stats(
    allocator: *mut PlatformMemoryAllocator,
    stats_out: *mut PlatformMemoryPoolStats,
) -> bool {
    if allocator.is_null() || stats_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    let stats = allocator.get_stats();
    unsafe { *stats_out = stats };
    true
}

/// Detects platform memory leaks
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_detect_leaks(
    allocator: *mut PlatformMemoryAllocator,
    leaks_out: *mut *mut PlatformMemoryLeak,
    leak_count_out: *mut usize,
) -> bool {
    if allocator.is_null() || leaks_out.is_null() || leak_count_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };

    match allocator.detect_leaks() {
        Ok(leaks) => {
            let leak_count = leaks.len();
            let leaks_box = Box::new(leaks);
            unsafe {
                *leaks_out = Box::into_raw(leaks_box) as *mut PlatformMemoryLeak;
                *leak_count_out = leak_count;
            }
            true
        }
        Err(_) => false,
    }
}

/// Prefetches platform memory for cache optimization
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_prefetch(
    allocator: *mut PlatformMemoryAllocator,
    ptr: *const c_void,
    size: usize,
) -> bool {
    if allocator.is_null() || ptr.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    allocator.prefetch_memory(ptr, size).is_ok()
}

/// Gets platform memory information
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_get_info(
    allocator: *mut PlatformMemoryAllocator,
    info_out: *mut PlatformMemoryInfo,
) -> bool {
    if allocator.is_null() || info_out.is_null() {
        return false;
    }

    let allocator = unsafe { &*allocator };
    unsafe { *info_out = allocator.memory_info.clone() };
    true
}

/// Destroys a platform memory allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_allocator_destroy(
    allocator: *mut PlatformMemoryAllocator,
) {
    if !allocator.is_null() {
        unsafe { drop(Box::from_raw(allocator)) };
    }
}

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

/// Creates a WebAssembly memory with platform optimization
#[no_mangle]
pub extern "C" fn wasmtime4j_memory_create_with_platform_config(
    store: *mut crate::store::Store,
    initial_pages: u64,
    maximum_pages: i64, // -1 for None
    platform_config: *const PlatformMemoryConfig,
) -> *mut Memory {
    if store.is_null() {
        error!("Store pointer cannot be null");
        return std::ptr::null_mut();
    }

    let store = unsafe { &mut *store };

    let memory_config = MemoryConfig {
        initial_pages,
        maximum_pages: if maximum_pages < 0 {
            None
        } else {
            Some(maximum_pages as u64)
        },
        is_shared: false,
        is_64: false,
        memory_index: 0,
        name: None,
    };

    let result = if platform_config.is_null() {
        Memory::new_with_config(store, memory_config)
    } else {
        let platform_config = unsafe { (*platform_config).clone() };
        Memory::new_with_platform_config(store, memory_config, platform_config)
    };

    match result {
        Ok(memory) => Box::into_raw(Box::new(memory)),
        Err(e) => {
            error!("Failed to create memory with platform config: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Gets platform allocator from WebAssembly memory (if available)
#[no_mangle]
pub extern "C" fn wasmtime4j_memory_get_platform_allocator(
    memory: *const Memory,
) -> *const PlatformMemoryAllocator {
    if memory.is_null() {
        return std::ptr::null();
    }

    let memory = unsafe { &*memory };

    if let Some(ref allocator) = memory.platform_allocator {
        allocator.as_ref() as *const PlatformMemoryAllocator
    } else {
        std::ptr::null()
    }
}

/// Performs memory compression on data
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_compress(
    allocator: *mut PlatformMemoryAllocator,
    data: *const u8,
    data_len: usize,
    compressed_out: *mut *mut u8,
    compressed_len_out: *mut usize,
) -> bool {
    if allocator.is_null()
        || data.is_null()
        || compressed_out.is_null()
        || compressed_len_out.is_null()
    {
        return false;
    }

    let _allocator = unsafe { &*allocator };
    let input_data = unsafe { std::slice::from_raw_parts(data, data_len) };

    // Simple compression using flate2
    let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
    if encoder.write_all(input_data).is_err() {
        return false;
    }

    match encoder.finish() {
        Ok(compressed_data) => {
            let compressed_len = compressed_data.len();
            let compressed_ptr = compressed_data.as_ptr() as *mut u8;

            // Leak the memory so it can be used by caller (caller must free)
            std::mem::forget(compressed_data);

            unsafe {
                *compressed_out = compressed_ptr;
                *compressed_len_out = compressed_len;
            }
            true
        }
        Err(_) => false,
    }
}

/// Performs memory deduplication
#[no_mangle]
pub extern "C" fn wasmtime4j_platform_memory_deduplicate(
    allocator: *mut PlatformMemoryAllocator,
    data: *const u8,
    data_len: usize,
) -> *mut c_void {
    if allocator.is_null() || data.is_null() {
        return std::ptr::null_mut();
    }

    let allocator = unsafe { &*allocator };
    let input_data = unsafe { std::slice::from_raw_parts(data, data_len) };

    match allocator.deduplicate_memory(input_data) {
        Ok(ptr) => ptr,
        Err(_) => std::ptr::null_mut(),
    }
}
