//! JNI bindings for pooling allocator operations
//!
//! This module provides JNI wrappers for the pooling allocator functionality,
//! enabling Java applications to access high-performance instance pooling for
//! WebAssembly execution.

use jni::objects::JClass;
use jni::sys::{jboolean, jfloat, jint, jlong, jlongArray, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

use crate::pooling_allocator::{PoolingAllocator, PoolingAllocatorConfig};

/// Creates a new pooling allocator with custom configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeCreateWithConfig(
    _env: JNIEnv,
    _class: JClass,
    instance_pool_size: jint,
    max_memory_per_instance: jlong,
    stack_size: jint,
    max_stacks: jint,
    max_tables_per_instance: jint,
    max_tables: jint,
    memory_decommit_enabled: jboolean,
    pool_warming_enabled: jboolean,
    pool_warming_percentage: jfloat,
) -> jlong {
    let config = PoolingAllocatorConfig {
        instance_pool_size: instance_pool_size as usize,
        max_memory_per_instance: max_memory_per_instance as u64,
        stack_size: stack_size as u32,
        max_stacks: max_stacks as usize,
        max_tables_per_instance: max_tables_per_instance as u32,
        max_tables: max_tables as usize,
        memory_decommit_enabled: memory_decommit_enabled != JNI_FALSE,
        pool_warming_enabled: pool_warming_enabled != JNI_FALSE,
        pool_warming_percentage,
    };

    match PoolingAllocator::new(config) {
        Ok(allocator) => Box::into_raw(Box::new(allocator)) as jlong,
        Err(e) => {
            log::error!("Failed to create pooling allocator: {}", e);
            0
        }
    }
}

/// Allocates an instance from the pool
/// Returns the instance ID or -1 on failure
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeAllocateInstance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jlong {
    if allocator_handle == 0 {
        return -1;
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.allocate_instance() {
        Ok(instance_id) => instance_id as jlong,
        Err(e) => {
            log::warn!("Failed to allocate instance: {}", e);
            -1
        }
    }
}

/// Reuses an existing instance from the pool
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeReuseInstance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
    instance_id: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.reuse_instance(instance_id as u64) {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            log::warn!("Failed to reuse instance {}: {}", instance_id, e);
            JNI_FALSE
        }
    }
}

/// Releases an instance back to the pool
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeReleaseInstance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
    instance_id: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.release_instance(instance_id as u64) {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            log::warn!("Failed to release instance {}: {}", instance_id, e);
            JNI_FALSE
        }
    }
}

/// Gets pool statistics as a long array
/// Returns array of 14 longs: [instancesAllocated, instancesReused, instancesCreated,
/// memoryPoolsAllocated, memoryPoolsReused, stackPoolsAllocated, stackPoolsReused,
/// tablePoolsAllocated, tablePoolsReused, peakMemoryUsage, currentMemoryUsage,
/// allocationFailures, poolWarmingTimeNanos, averageAllocationTimeNanos]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeGetStatistics(
    env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jlongArray {
    if allocator_handle == 0 {
        return std::ptr::null_mut();
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.get_statistics() {
        Ok(stats) => {
            let stats_array: [jlong; 14] = [
                stats.instances_allocated as jlong,
                stats.instances_reused as jlong,
                stats.instances_created as jlong,
                stats.memory_pools_allocated as jlong,
                stats.memory_pools_reused as jlong,
                stats.stack_pools_allocated as jlong,
                stats.stack_pools_reused as jlong,
                stats.table_pools_allocated as jlong,
                stats.table_pools_reused as jlong,
                stats.peak_memory_usage as jlong,
                stats.current_memory_usage as jlong,
                stats.allocation_failures as jlong,
                stats.pool_warming_time.as_nanos() as jlong,
                stats.average_allocation_time.as_nanos() as jlong,
            ];

            match env.new_long_array(14) {
                Ok(result) => {
                    if env.set_long_array_region(&result, 0, &stats_array).is_err() {
                        log::error!("Failed to set statistics array region");
                        return std::ptr::null_mut();
                    }
                    result.into_raw()
                }
                Err(e) => {
                    log::error!("Failed to create statistics array: {}", e);
                    std::ptr::null_mut()
                }
            }
        }
        Err(e) => {
            log::warn!("Failed to get statistics: {}", e);
            std::ptr::null_mut()
        }
    }
}

/// Resets pool statistics
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeResetStatistics(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.reset_statistics() {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            log::warn!("Failed to reset statistics: {}", e);
            JNI_FALSE
        }
    }
}

/// Warms up the pools by pre-allocating resources
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeWarmPools(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.warm_pools() {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            log::warn!("Failed to warm pools: {}", e);
            JNI_FALSE
        }
    }
}

/// Performs maintenance operations on the pools
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativePerformMaintenance(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) -> jboolean {
    if allocator_handle == 0 {
        return JNI_FALSE;
    }

    let allocator = unsafe { &*(allocator_handle as *const PoolingAllocator) };
    match allocator.perform_maintenance() {
        Ok(()) => JNI_TRUE,
        Err(e) => {
            log::warn!("Failed to perform maintenance: {}", e);
            JNI_FALSE
        }
    }
}

/// Destroys a pooling allocator
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_pool_JniPoolingAllocator_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    allocator_handle: jlong,
) {
    if allocator_handle != 0 {
        unsafe {
            drop(Box::from_raw(allocator_handle as *mut PoolingAllocator));
        }
        log::debug!("Destroyed pooling allocator");
    }
}
