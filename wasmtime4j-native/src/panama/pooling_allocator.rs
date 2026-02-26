//! Pooling allocator FFI module for Panama
//!
//! This module provides Panama FFI bindings for the pooling allocator, which enables
//! efficient reuse of WebAssembly instance slots to reduce allocation overhead.

use crate::jni_pooling_allocator_bindings::JniPoolingAllocatorWrapper;
use std::os::raw::{c_int, c_long, c_void};

/// Create a pooling allocator with default configuration (Panama FFI)
///
/// # Safety
/// - Caller must free the returned pointer using wasmtime4j_pooling_allocator_destroy
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_create() -> *mut c_void {
    match JniPoolingAllocatorWrapper::new(
        100,     // instance_pool_size
        1 << 26, // max_memory_per_instance (64MB)
        1 << 16, // stack_size (64KB)
        100,     // max_stacks
        10,      // max_tables_per_instance
        10,      // max_tables_per_component
        1000,    // max_tables
        true,    // memory_decommit_enabled
        false,   // pool_warming_enabled
        0.0,     // pool_warming_percentage
    ) {
        Ok(wrapper) => Box::into_raw(Box::new(wrapper)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Create a pooling allocator with custom configuration (Panama FFI)
///
/// # Safety
/// - Caller must free the returned pointer using wasmtime4j_pooling_allocator_destroy
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_create_with_config(
    instance_pool_size: c_int,
    max_memory_per_instance: c_long,
    stack_size: c_int,
    max_stacks: c_int,
    max_tables_per_instance: c_int,
    max_tables_per_component: c_int,
    max_tables: c_int,
    memory_decommit_enabled: c_int,
    pool_warming_enabled: c_int,
    pool_warming_percentage: f32,
) -> *mut c_void {
    match JniPoolingAllocatorWrapper::new(
        instance_pool_size as u32,
        max_memory_per_instance as usize,
        stack_size as usize,
        max_stacks as u32,
        max_tables_per_instance as u32,
        max_tables_per_component as u32,
        max_tables as u32,
        memory_decommit_enabled != 0,
        pool_warming_enabled != 0,
        pool_warming_percentage,
    ) {
        Ok(wrapper) => Box::into_raw(Box::new(wrapper)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Allocate an instance from the pool (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
/// - instance_id_out must be a valid pointer to a c_long
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_allocate_instance(
    allocator_ptr: *mut c_void,
    instance_id_out: *mut c_long,
) -> c_int {
    if allocator_ptr.is_null() || instance_id_out.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.allocate_instance() {
        Ok(id) => {
            unsafe { *instance_id_out = id as c_long };
            1
        }
        Err(_) => 0,
    }
}

/// Reuse an existing instance (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_reuse_instance(
    allocator_ptr: *mut c_void,
    instance_id: c_long,
) -> c_int {
    if allocator_ptr.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.reuse_instance(instance_id as u64) {
        Ok(true) => 1,
        _ => 0,
    }
}

/// Release an instance back to the pool (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_release_instance(
    allocator_ptr: *mut c_void,
    instance_id: c_long,
) -> c_int {
    if allocator_ptr.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.release_instance(instance_id as u64) {
        Ok(true) => 1,
        _ => 0,
    }
}

/// Get statistics from the allocator (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
/// - stats_out must be a valid pointer to a memory segment with at least 112 bytes
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_get_statistics(
    allocator_ptr: *mut c_void,
    stats_out: *mut c_long,
) -> c_int {
    if allocator_ptr.is_null() || stats_out.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.get_statistics() {
        Ok(stats) => {
            unsafe {
                for (i, &value) in stats.iter().enumerate() {
                    *stats_out.add(i) = value;
                }
            }
            1
        }
        Err(_) => 0,
    }
}

/// Reset statistics (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_reset_statistics(
    allocator_ptr: *mut c_void,
) -> c_int {
    if allocator_ptr.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.reset_statistics() {
        Ok(()) => 1,
        Err(_) => 0,
    }
}

/// Warm the pools (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_warm_pools(allocator_ptr: *mut c_void) -> c_int {
    if allocator_ptr.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.warm_pools() {
        Ok(()) => 1,
        Err(_) => 0,
    }
}

/// Perform maintenance on the pool (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_perform_maintenance(
    allocator_ptr: *mut c_void,
) -> c_int {
    if allocator_ptr.is_null() {
        return 0;
    }

    let wrapper = unsafe { &*(allocator_ptr as *const JniPoolingAllocatorWrapper) };

    match wrapper.perform_maintenance() {
        Ok(()) => 1,
        Err(_) => 0,
    }
}

/// Destroy the pooling allocator (Panama FFI)
///
/// # Safety
/// - allocator_ptr must be a valid pointer to a pooling allocator created by this module
#[no_mangle]
pub extern "C" fn wasmtime4j_pooling_allocator_destroy(allocator_ptr: *mut c_void) {
    if allocator_ptr.is_null() {
        return;
    }

    let wrapper = unsafe { Box::from_raw(allocator_ptr as *mut JniPoolingAllocatorWrapper) };
    wrapper.close();
    drop(wrapper);
}
