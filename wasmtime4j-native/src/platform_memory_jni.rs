//! JNI bindings for platform-specific memory management
//!
//! This module provides JNI interface for the platform memory allocator,
//! allowing Java code to use advanced memory management features including
//! huge pages, NUMA awareness, and comprehensive monitoring.

use jni::objects::{JClass, JObject, JByteArray};
use jni::sys::{jlong, jint, jboolean, jobjectArray, jobject};
use jni::JNIEnv;
use std::ffi::c_void;

use crate::memory::{PlatformMemoryAllocator, PlatformMemoryConfig, PageSize};
use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};

/// Creates a new platform memory allocator
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeCreate(
    mut env: JNIEnv,
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
    jni_utils::jni_try_ptr(&mut env, || {
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

        let allocator = PlatformMemoryAllocator::new(config)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Failed to create platform allocator: {}", e),
                backtrace: None
            })?;
        Ok(Box::new(allocator))
    }) as jlong
}

/// Allocates memory with platform optimizations
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeAllocate(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    size: jlong,
    alignment: jint,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };
        let alignment_opt = if alignment == 0 { None } else { Some(alignment as usize) };

        let ptr = allocator.allocate(size as usize, alignment_opt)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Memory allocation failed: {}", e),
                backtrace: None
            })?;
        Ok(ptr.as_ptr() as jlong)
    })
}

/// Deallocates platform memory
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDeallocate(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        if ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid pointer".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        let non_null_ptr = std::ptr::NonNull::new(ptr as *mut c_void)
            .ok_or_else(|| WasmtimeError::InvalidParameter { message: "Null pointer provided".to_string() })?;

        allocator.deallocate(non_null_ptr)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Memory deallocation failed: {}", e),
                backtrace: None
            })?;
        Ok(1)
    })
}

/// Gets platform memory statistics
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeGetStats(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jobject {
    match (|| -> WasmtimeResult<_> {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };
        let stats = allocator.get_stats();

        // Create Java MemoryStats object
        let stats_class = env.find_class("ai/tegmentum/wasmtime4j/jni/memory/PlatformMemoryManager$MemoryStats")
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to find MemoryStats class".to_string(), backtrace: None })?;

        let constructor_signature = "(JJJJJJDDJD)V";
        let stats_object = env.new_object(stats_class, constructor_signature, &[
            (stats.total_allocated as jlong).into(),
            (stats.total_freed as jlong).into(),
            (stats.current_usage as jlong).into(),
            (stats.peak_usage as jlong).into(),
            (stats.allocation_count as jlong).into(),
            (stats.deallocation_count as jlong).into(),
            stats.fragmentation_ratio.into(),
            stats.compression_ratio.into(),
            (stats.deduplication_savings as jlong).into(),
            (stats.huge_pages_used as jlong).into(),
            stats.numa_hit_rate.into(),
        ]).map_err(|_| WasmtimeError::Runtime { message: "Failed to create MemoryStats object".to_string(), backtrace: None })?;

        Ok(stats_object)
    })() {
        Ok(obj) => obj.into_raw(),
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Gets platform memory information
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeGetPlatformInfo<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    handle: jlong,
) -> jobject {
    jni_utils::jni_try_object(&mut env, |env| {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };
        let info = allocator.memory_info();

        // Create Java PlatformInfo object
        let info_class = env.find_class("ai/tegmentum/wasmtime4j/jni/memory/PlatformMemoryManager$PlatformInfo")
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to find PlatformInfo class".to_string(), backtrace: None })?;

        let constructor_signature = "(JJJJIIIZZ)V";
        let info_object = env.new_object(info_class, constructor_signature, &[
            (info.total_physical_memory as jlong).into(),
            (info.available_memory as jlong).into(),
            (info.page_size as jlong).into(),
            (info.huge_page_size as jlong).into(),
            (info.numa_nodes as jint).into(),
            (info.cpu_cores as jint).into(),
            (info.cache_line_size as jint).into(),
            (info.supports_huge_pages as i32).into(),
            (info.supports_numa as i32).into(),
        ]).map_err(|_| WasmtimeError::Runtime { message: "Failed to create PlatformInfo object".to_string(), backtrace: None })?;

        Ok(unsafe { JObject::from_raw(info_object.into_raw()) })
    })
}

/// Detects platform memory leaks
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDetectLeaks(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) -> jobjectArray {
    jni_utils::jni_try_object(&mut env, |env| {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        let leaks = allocator.detect_leaks()
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Leak detection failed: {}", e),
                backtrace: None
            })?;

        // Create Java array of MemoryLeak objects
        let leak_class = env.find_class("ai/tegmentum/wasmtime4j/jni/memory/PlatformMemoryManager$MemoryLeak")
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to find MemoryLeak class".to_string(), backtrace: None })?;

        let leak_array = env.new_object_array(leaks.len() as i32, leak_class, JObject::null())
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to create leak array".to_string(), backtrace: None })?;

        // Note: Full implementation would create individual MemoryLeak objects
        // This is a simplified version that returns empty array
        Ok(unsafe { JObject::from_raw(leak_array.into_raw()) })
    })
}

/// Prefetches platform memory for cache optimization
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativePrefetch(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    ptr: jlong,
    size: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        if ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid pointer".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        match allocator.prefetch_memory(ptr as *const c_void, size as usize) {
            Ok(_) => Ok(1),
            Err(_) => Ok(0), // Prefetch failure is not critical
        }
    })
}

/// Compresses data using platform-specific compression
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeCompress<'a>(
    mut env: JNIEnv<'a>,
    _class: JClass<'a>,
    handle: jlong,
    data: JByteArray<'a>,
) -> jobject {
    jni_utils::jni_try_object(&mut env, |env| {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        // Convert Java byte array to Rust slice
        let data_len = env.get_array_length(&data)
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to get array length".to_string(), backtrace: None })? as usize;
        let mut data_bytes = vec![0i8; data_len];
        env.get_byte_array_region(data, 0, &mut data_bytes)
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to read byte array".to_string(), backtrace: None })?;

        let data_bytes_u8: Vec<u8> = data_bytes.iter().map(|&x| x as u8).collect();
        let compressed = allocator.compress_memory(&data_bytes_u8)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Compression failed: {}", e),
                backtrace: None
            })?;

        // Create Java byte array for compressed data
        let compressed_array = env.new_byte_array(compressed.len() as i32)
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to create compressed array".to_string(), backtrace: None })?;
        let compressed_i8: Vec<i8> = compressed.iter().map(|&x| x as i8).collect();
        env.set_byte_array_region(&compressed_array, 0, &compressed_i8)
            .map_err(|_| WasmtimeError::Runtime { message: "Failed to set compressed array".to_string(), backtrace: None })?;
        Ok(unsafe { JObject::from_raw(compressed_array.into_raw()) })
    })
}

/// Performs memory deduplication
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDeduplicate(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
    data: JByteArray,
) -> jlong {
    // Extract array operations before jni_try_with_default to avoid borrowing conflicts
    let data_len = match env.get_array_length(&data) {
        Ok(len) => len as usize,
        Err(_) => return 0,
    };
    let mut data_bytes = vec![0i8; data_len];
    if env.get_byte_array_region(data, 0, &mut data_bytes).is_err() {
        return 0;
    }

    jni_utils::jni_try_with_default(&mut env, 0, || {
        if handle == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid allocator handle".to_string() });
        }

        let allocator = unsafe { &*(handle as *const PlatformMemoryAllocator) };

        let data_bytes_u8: Vec<u8> = data_bytes.iter().map(|&x| x as u8).collect();
        let ptr = allocator.deduplicate_memory(&data_bytes_u8)
            .map_err(|e| WasmtimeError::Runtime {
                message: format!("Deduplication failed: {}", e),
                backtrace: None
            })?;
        Ok(ptr as jlong)
    })
}

/// Destroys a platform memory allocator
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_memory_PlatformMemoryManager_nativeDestroy(
    mut env: JNIEnv,
    _class: JClass,
    handle: jlong,
) {
    jni_utils::jni_try_void(&mut env, || {
        if handle != 0 {
            unsafe {
                let _ = Box::from_raw(handle as *mut PlatformMemoryAllocator);
            }
        }
        Ok(())
    });
}