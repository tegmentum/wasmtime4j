//! JNI bindings for ModuleCache functionality
//!
//! This module provides JNI-compatible functions for the JniModuleCache class,
//! wrapping the native ModuleSerializer implementation.

#![allow(unused_variables)]

use jni::objects::{JByteArray, JClass, JString};
use jni::sys::{jboolean, jint, jlong, jstring};
use jni::JNIEnv;
use sha2::{Digest, Sha256};
use std::os::raw::c_void;
use std::path::PathBuf;
use std::sync::atomic::{AtomicU64, Ordering};
use std::time::Duration;

use crate::error::{jni_utils, WasmtimeError, WasmtimeResult};
use crate::module::ModuleMetadata;
use crate::serialization::{ModuleSerializer, SerializationConfig, ValidationLevel};

/// Module cache wrapper for JNI
///
/// This wraps ModuleSerializer with additional statistics tracking for JNI access
pub struct JniModuleCacheWrapper {
    /// The underlying serializer with caching functionality
    serializer: ModuleSerializer,
    /// Associated engine pointer for module compilation
    engine_ptr: *mut c_void,
    /// Cache directory path
    cache_dir: PathBuf,
    /// Hit count
    hits: AtomicU64,
    /// Miss count
    misses: AtomicU64,
    /// Whether the cache is closed
    closed: std::sync::atomic::AtomicBool,
}

// Safety: JniModuleCacheWrapper can be safely sent between threads
// The engine_ptr is only used for reference, not mutated
unsafe impl Send for JniModuleCacheWrapper {}
unsafe impl Sync for JniModuleCacheWrapper {}

impl JniModuleCacheWrapper {
    /// Create a new module cache wrapper
    pub fn new(
        engine_ptr: *mut c_void,
        cache_dir: PathBuf,
        max_cache_size: u64,
        _max_entries: i32,
        compression_enabled: bool,
        compression_level: i32,
    ) -> WasmtimeResult<Self> {
        // Create serialization config
        let config = SerializationConfig {
            max_cache_size: max_cache_size as usize,
            max_cache_age: Duration::from_secs(24 * 60 * 60), // 24 hours
            enable_compression: compression_enabled,
            compression_level: compression_level as u32,
            max_entry_age: Duration::from_secs(24 * 60 * 60),
            cache_directory: Some(cache_dir.clone()),
            enable_cross_process: true,
            validation_level: ValidationLevel::Basic,
        };

        let serializer = ModuleSerializer::with_config(config);

        Ok(Self {
            serializer,
            engine_ptr,
            cache_dir,
            hits: AtomicU64::new(0),
            misses: AtomicU64::new(0),
            closed: std::sync::atomic::AtomicBool::new(false),
        })
    }

    /// Generate a hash for the wasm bytes to use as cache key
    fn hash_wasm_bytes(wasm_bytes: &[u8]) -> String {
        let mut hasher = Sha256::new();
        hasher.update(wasm_bytes);
        format!("{:x}", hasher.finalize())
    }

    /// Get or compile a module from cache
    pub fn get_or_compile(&self, wasm_bytes: &[u8]) -> WasmtimeResult<*mut c_void> {
        if self.closed.load(Ordering::Acquire) {
            return Err(WasmtimeError::InvalidState {
                message: "Module cache is closed".to_string(),
            });
        }

        // Get the engine reference
        let engine = unsafe { crate::engine::core::get_engine_ref(self.engine_ptr)? };

        // Check disk cache first
        let cache_key = Self::hash_wasm_bytes(wasm_bytes);
        let cache_file = self.cache_dir.join(format!("{}.wasm", cache_key));

        if cache_file.exists() {
            // Try to deserialize from file
            match self
                .serializer
                .deserialize_from_file(engine.inner(), &cache_file)
            {
                Ok(wasmtime_module) => {
                    self.hits.fetch_add(1, Ordering::Relaxed);
                    // Wrap the module and return pointer
                    let metadata = ModuleMetadata::empty();
                    let wrapped = crate::module::Module::from_wasmtime_module(
                        wasmtime_module,
                        engine.clone(),
                        metadata,
                    );
                    return Ok(Box::into_raw(Box::new(wrapped)) as *mut c_void);
                }
                Err(_) => {
                    // Cache file corrupted or invalid, will recompile
                    let _ = std::fs::remove_file(&cache_file);
                }
            }
        }

        // Cache miss - compile the module
        self.misses.fetch_add(1, Ordering::Relaxed);
        let module = crate::module::core::compile_module(&engine, wasm_bytes)?;

        // Store in cache (best effort - ignore errors)
        let _ = self
            .serializer
            .serialize_to_file(engine.inner(), module.inner(), &cache_file);

        // Note: compile_module already returns Box<Module>, so don't double-box
        Ok(Box::into_raw(module) as *mut c_void)
    }

    /// Precompile a module and store in cache
    pub fn precompile(&self, wasm_bytes: &[u8]) -> WasmtimeResult<String> {
        if self.closed.load(Ordering::Acquire) {
            return Err(WasmtimeError::InvalidState {
                message: "Module cache is closed".to_string(),
            });
        }

        let engine = unsafe { crate::engine::core::get_engine_ref(self.engine_ptr)? };

        // Compile the module
        let module = crate::module::core::compile_module(&engine, wasm_bytes)?;

        // Generate cache key
        let cache_key = Self::hash_wasm_bytes(wasm_bytes);
        let cache_file = self.cache_dir.join(format!("{}.wasm", cache_key));

        // Serialize and store
        self.serializer
            .serialize_to_file(engine.inner(), module.inner(), &cache_file)?;

        Ok(cache_key)
    }

    /// Clear the cache
    pub fn clear(&self) -> WasmtimeResult<bool> {
        if self.closed.load(Ordering::Acquire) {
            return Ok(false);
        }
        self.serializer.clear_cache()?;

        // Also clear disk cache
        if self.cache_dir.exists() {
            for entry in std::fs::read_dir(&self.cache_dir).into_iter().flatten() {
                if let Ok(entry) = entry {
                    let path = entry.path();
                    if path.extension().map_or(false, |ext| ext == "wasm") {
                        let _ = std::fs::remove_file(path);
                    }
                }
            }
        }

        self.hits.store(0, Ordering::Relaxed);
        self.misses.store(0, Ordering::Relaxed);
        Ok(true)
    }

    /// Perform maintenance (no-op currently - LRU eviction happens automatically)
    pub fn perform_maintenance(&self) -> WasmtimeResult<bool> {
        if self.closed.load(Ordering::Acquire) {
            return Ok(false);
        }
        // LRU eviction happens automatically in the serializer
        // This is a no-op for now but could be extended for disk cleanup
        Ok(true)
    }

    /// Get entry count
    pub fn entry_count(&self) -> u64 {
        if self.closed.load(Ordering::Acquire) {
            return 0;
        }
        self.serializer.get_cache_info().entry_count as u64
    }

    /// Get storage bytes used
    pub fn storage_bytes(&self) -> u64 {
        if self.closed.load(Ordering::Acquire) {
            return 0;
        }
        self.serializer.get_cache_info().total_size as u64
    }

    /// Get hit count
    pub fn hit_count(&self) -> u64 {
        self.hits.load(Ordering::Relaxed)
    }

    /// Get miss count
    pub fn miss_count(&self) -> u64 {
        self.misses.load(Ordering::Relaxed)
    }

    /// Close the cache
    pub fn close(&self) {
        self.closed.store(true, Ordering::Release);
    }
}

// =============================================================================
// JNI Bindings
// =============================================================================

/// Create a new module cache with configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeCreateWithConfig(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
    cache_dir: JString,
    max_cache_size: jlong,
    max_entries: jint,
    compression_enabled: jboolean,
    compression_level: jint,
) -> jlong {
    // Extract the string before entering the closure to avoid borrow issues
    let cache_dir_result: Result<String, _> = env.get_string(&cache_dir).map(|s| s.into());

    jni_utils::jni_try_with_default(&mut env, 0, || {
        let cache_dir_str = cache_dir_result.map_err(|e| WasmtimeError::InvalidOperation {
            message: format!("Failed to get cache_dir string: {:?}", e),
        })?;

        let cache_path = PathBuf::from(cache_dir_str);

        // Create the cache wrapper
        let wrapper = JniModuleCacheWrapper::new(
            engine_handle as *mut c_void,
            cache_path,
            max_cache_size as u64,
            max_entries,
            compression_enabled != 0,
            compression_level,
        )?;

        Ok(Box::into_raw(Box::new(wrapper)) as jlong)
    })
}

/// Get or compile a module from cache
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeGetOrCompile(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
    wasm_bytes: JByteArray,
) -> jlong {
    // Extract bytes before entering the closure
    let bytes_result: Result<Vec<u8>, _> = env.convert_byte_array(&wasm_bytes);

    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Err(WasmtimeError::InvalidOperation {
                message: "cache_handle is null".to_string(),
            });
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };

        let bytes = bytes_result.map_err(|e| WasmtimeError::InvalidOperation {
            message: format!("Failed to convert wasm bytes: {:?}", e),
        })?;

        let module_ptr = wrapper.get_or_compile(&bytes)?;
        Ok(module_ptr as jlong)
    })
}

/// Precompile a module and store in cache
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativePrecompile(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
    wasm_bytes: JByteArray,
) -> jstring {
    // Extract bytes before any closures
    let bytes_result: Result<Vec<u8>, _> = env.convert_byte_array(&wasm_bytes);

    // First, compute the result
    let result: WasmtimeResult<String> = (|| {
        if cache_handle == 0 {
            return Err(WasmtimeError::InvalidOperation {
                message: "cache_handle is null".to_string(),
            });
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };

        let bytes = bytes_result.map_err(|e| WasmtimeError::InvalidOperation {
            message: format!("Failed to convert wasm bytes: {:?}", e),
        })?;

        wrapper.precompile(&bytes)
    })();

    // Then convert to Java string
    match result {
        Ok(hash) => match env.new_string(&hash) {
            Ok(jstr) => jstr.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            std::ptr::null_mut()
        }
    }
}

/// Clear the cache
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeClear(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Ok(0);
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };
        let success = wrapper.clear()?;
        Ok(if success { 1 } else { 0 })
    })
}

/// Perform cache maintenance
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativePerformMaintenance(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Ok(0);
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };
        let success = wrapper.perform_maintenance()?;
        Ok(if success { 1 } else { 0 })
    })
}

/// Get entry count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeEntryCount(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Ok(0);
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };
        Ok(wrapper.entry_count() as jlong)
    })
}

/// Get storage bytes used
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeStorageBytes(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Ok(0);
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };
        Ok(wrapper.storage_bytes() as jlong)
    })
}

/// Get hit count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeHitCount(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Ok(0);
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };
        Ok(wrapper.hit_count() as jlong)
    })
}

/// Get miss count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeMissCount(
    mut env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if cache_handle == 0 {
            return Ok(0);
        }

        let wrapper = unsafe { &*(cache_handle as *const JniModuleCacheWrapper) };
        Ok(wrapper.miss_count() as jlong)
    })
}

/// Destroy the cache
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniModuleCache_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    cache_handle: jlong,
) {
    if cache_handle != 0 {
        unsafe {
            let wrapper = Box::from_raw(cache_handle as *mut JniModuleCacheWrapper);
            wrapper.close();
            // wrapper is dropped here
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_hash_wasm_bytes() {
        let wasm = b"\x00asm\x01\x00\x00\x00";
        let hash = JniModuleCacheWrapper::hash_wasm_bytes(wasm);
        assert!(!hash.is_empty());
        assert_eq!(hash.len(), 64); // SHA256 produces 64 hex chars
    }
}
