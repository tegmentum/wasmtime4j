//! JNI bindings for platform-specific memory management
//!
//! This module provides JNI interface for the platform memory allocator,
//! allowing Java code to use advanced memory management features including
//! huge pages, NUMA awareness, and comprehensive monitoring.

use jni::objects::{JClass, JObject, JString, JByteArray};
use jni::sys::{jlong, jint, jboolean, jdoubleArray, jobjectArray};
use jni::JNIEnv;
use std::ptr;
use std::ffi::c_void;

use crate::memory::{PlatformMemoryAllocator, PlatformMemoryConfig, PageSize, PlatformMemoryPoolStats, PlatformMemoryInfo, PlatformMemoryLeak};
use crate::error::jni_utils;

/// Creates a new platform memory allocator
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeCreate(
    env: JNIEnv,
    _class: JClass,
    enable_huge_pages: jboolean,
    numa_node: jint,
    initial_pool_size: jlong,
    max_pool_size: jlong,
    enable_compression: jboolean,
    enable_deduplication: jboolean,
    prefetch_buffer_size: jlong,
    enable_leak_detection: jboolean,
    alignment: jint,
    page_size: jint,
) -> jlong {
    jni_utils::jni_call(&env, || {
        let config = PlatformMemoryConfig {
            enable_huge_pages: enable_huge_pages != 0,
            numa_node,
            initial_pool_size: initial_pool_size as usize,
            max_pool_size: max_pool_size as usize,
            enable_compression: enable_compression != 0,
            enable_deduplication: enable_deduplication != 0,
            prefetch_buffer_size: prefetch_buffer_size as usize,
            enable_leak_detection: enable_leak_detection != 0,
            alignment: alignment as usize,
            page_size: match page_size {
                0 => PageSize::Default,
                1 => PageSize::Small,
                2 => PageSize::Large,
                3 => PageSize::Huge,
                _ => PageSize::Default,
            },
        };

        match PlatformMemoryAllocator::new(config) {
            Ok(allocator) => {
                let boxed = Box::new(allocator);
                Box::into_raw(boxed) as jlong
            }
            Err(e) => {
                jni_utils::throw_runtime_exception(&env, &format!("Failed to create platform allocator: {}", e));
                0
            }
        }
    }).unwrap_or(0)
}

/// Allocates memory with platform optimizations
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeAllocate(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    size: jlong,
    alignment: jint,
) -> jlong {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return 0;
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };
        let alignment_opt = if alignment == 0 { None } else { Some(alignment as usize) };

        match allocator.allocate(size as usize, alignment_opt) {
            Ok(ptr) => ptr.as_ptr() as jlong,
            Err(e) => {
                jni_utils::throw_runtime_exception(&env, &format!("Memory allocation failed: {}", e));
                0
            }
        }
    }).unwrap_or(0)
}

/// Deallocates platform memory
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDeallocate(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    ptr: jlong,
) -> jboolean {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return 0;
        }

        if ptr == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid pointer");
            return 0;
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        if let Some(non_null_ptr) = std::ptr::NonNull::new(ptr as *mut c_void) {
            match allocator.deallocate(non_null_ptr) {
                Ok(_) => 1,
                Err(e) => {
                    jni_utils::throw_runtime_exception(&env, &format!("Memory deallocation failed: {}", e));
                    0
                }
            }
        } else {
            jni_utils::throw_illegal_argument(&env, "Null pointer provided");
            0
        }
    }).unwrap_or(0)
}

/// Gets platform memory statistics
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeGetStats(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> JObject {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return JObject::null();
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };
        let stats = allocator.get_stats();

        // Create Java MemoryStats object
        let stats_class = env.find_class("ai/tegmentum/wasmtime4j/jni/memory/PlatformMemoryManager$MemoryStats")
            .expect("Failed to find MemoryStats class");

        let constructor_signature = "(JJJJJJDDJD)V";
        let stats_object = env.new_object(stats_class, constructor_signature, &[
            stats.total_allocated.into(),
            stats.total_freed.into(),
            stats.current_usage.into(),
            stats.peak_usage.into(),
            stats.allocation_count.into(),
            stats.deallocation_count.into(),
            stats.fragmentation_ratio.into(),
            stats.compression_ratio.into(),
            stats.deduplication_savings.into(),
            stats.huge_pages_used.into(),
            stats.numa_hit_rate.into(),
        ]).expect("Failed to create MemoryStats object");

        stats_object
    }).unwrap_or(JObject::null())
}

/// Gets platform memory information
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeGetPlatformInfo(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> JObject {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return JObject::null();
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };
        let info = &allocator.memory_info;

        // Create Java PlatformInfo object
        let info_class = env.find_class("ai/tegmentum/wasmtime4j/jni/memory/PlatformMemoryManager$PlatformInfo")
            .expect("Failed to find PlatformInfo class");

        let constructor_signature = "(JJJJIIIZZ)V";
        let info_object = env.new_object(info_class, constructor_signature, &[
            info.total_physical_memory.into(),
            info.available_memory.into(),
            info.page_size.into(),
            info.huge_page_size.into(),
            info.numa_nodes.into(),
            info.cpu_cores.into(),
            info.cache_line_size.into(),
            (info.supports_huge_pages as i32).into(),
            (info.supports_numa as i32).into(),
        ]).expect("Failed to create PlatformInfo object");

        info_object
    }).unwrap_or(JObject::null())
}

/// Detects platform memory leaks
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDetectLeaks(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jobjectArray {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return std::ptr::null_mut();
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        match allocator.detect_leaks() {
            Ok(leaks) => {
                // Create Java array of MemoryLeak objects
                let leak_class = env.find_class("ai/tegmentum/wasmtime4j/jni/memory/PlatformMemoryManager$MemoryLeak")
                    .expect("Failed to find MemoryLeak class");

                let leak_array = env.new_object_array(leaks.len() as i32, leak_class, JObject::null())
                    .expect("Failed to create leak array");

                // Note: Full implementation would create individual MemoryLeak objects
                // This is a simplified version that returns empty array
                leak_array.into_raw()
            }
            Err(e) => {
                jni_utils::throw_runtime_exception(&env, &format!("Leak detection failed: {}", e));
                std::ptr::null_mut()
            }
        }
    }).unwrap_or(std::ptr::null_mut())
}

/// Prefetches platform memory for cache optimization
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativePrefetch(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    ptr: jlong,
    size: jlong,
) -> jboolean {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return 0;
        }

        if ptr == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid pointer");
            return 0;
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        match allocator.prefetch_memory(ptr as *const c_void, size as usize) {
            Ok(_) => 1,
            Err(_) => 0, // Prefetch failure is not critical
        }
    }).unwrap_or(0)
}

/// Compresses data using platform-specific compression
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeCompress(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    data: JByteArray,
) -> JByteArray {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return JByteArray::from(JObject::null());
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        // Convert Java byte array to Rust slice
        let data_len = env.get_array_length(data).expect("Failed to get array length") as usize;
        let mut data_bytes = vec![0u8; data_len];
        env.get_byte_array_region(data, 0, &mut data_bytes).expect("Failed to read byte array");

        match allocator.compress_memory(&data_bytes) {
            Ok(compressed) => {
                // Create Java byte array for compressed data
                let compressed_array = env.new_byte_array(compressed.len() as i32)
                    .expect("Failed to create compressed array");
                env.set_byte_array_region(compressed_array, 0, &compressed)
                    .expect("Failed to set compressed array");
                compressed_array
            }
            Err(e) => {
                jni_utils::throw_runtime_exception(&env, &format!("Compression failed: {}", e));
                JByteArray::from(JObject::null())
            }
        }
    }).unwrap_or(JByteArray::from(JObject::null()))
}

/// Performs memory deduplication
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDeduplicate(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
    data: JByteArray,
) -> jlong {
    jni_utils::jni_call(&env, || {
        if handle == 0 {
            jni_utils::throw_illegal_argument(&env, "Invalid allocator handle");
            return 0;
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        // Convert Java byte array to Rust slice
        let data_len = env.get_array_length(data).expect("Failed to get array length") as usize;
        let mut data_bytes = vec![0u8; data_len];
        env.get_byte_array_region(data, 0, &mut data_bytes).expect("Failed to read byte array");

        match allocator.deduplicate_memory(&data_bytes) {
            Ok(ptr) => ptr as jlong,
            Err(e) => {
                jni_utils::throw_runtime_exception(&env, &format!("Deduplication failed: {}", e));
                0
            }
        }
    }).unwrap_or(0)
}

/// Destroys a platform memory allocator
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDestroy(
    env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    jni_utils::jni_call(&env, || {
        if handle != 0 {
            unsafe {
                let _ = Box::from_raw(handle as *mut PlatformMemoryAllocator);
            }
        }
    });
}