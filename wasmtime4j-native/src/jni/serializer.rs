//! JNI bindings for module serialization operations

use jni::objects::{JByteArray, JClass};
use jni::sys::{jboolean, jbyteArray, jdouble, jint, jlong};
use jni::JNIEnv;

use std::os::raw::c_void;
use std::time::Duration;

use crate::error::{jni_utils, WasmtimeError};
use crate::serialization::{core as serialization_core, SerializationConfig, ValidationLevel};

/// Create a new module serializer with default configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateSerializer(
    mut env: JNIEnv,
    _class: JClass,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || Ok(serialization_core::create_serializer())) as jlong
}

/// Create a new module serializer with custom configuration
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniWasmRuntime_nativeCreateSerializerWithConfig(
    mut env: JNIEnv,
    _class: JClass,
    max_cache_size: jlong,
    enable_compression: jboolean,
    compression_level: jint,
) -> jlong {
    jni_utils::jni_try_ptr(&mut env, || {
        let config = SerializationConfig {
            max_cache_size: if max_cache_size == 0 {
                1024 * 1024 * 1024
            } else {
                max_cache_size as usize
            },
            max_cache_age: Duration::from_secs(24 * 60 * 60),
            enable_compression: enable_compression != 0,
            compression_level: if compression_level == 0 {
                6
            } else {
                compression_level as u32
            },
            max_entry_age: Duration::from_secs(24 * 60 * 60),
            cache_directory: None,
            enable_cross_process: false,
            validation_level: ValidationLevel::Basic,
        };

        Ok(serialization_core::create_serializer_with_config(config))
    }) as jlong
}

/// Serialize module bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeSerialize(
    mut env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
    engine_ptr: jlong,
    module_bytes: jbyteArray,
) -> jbyteArray {
    // Convert byte array to Vec<u8>
    let data = match (|| -> Result<Vec<u8>, jni::errors::Error> {
        let byte_array = unsafe { JByteArray::from_raw(module_bytes) };
        let array_elements =
            unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
        let len = env.get_array_length(&byte_array)? as usize;
        let slice =
            unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
        Ok(slice.to_vec())
    })() {
        Ok(data) => data,
        Err(_) => return std::ptr::null_mut(),
    };

    // Perform serialization
    let result: Result<Vec<u8>, WasmtimeError> = (|| {
        if serializer_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Serializer handle cannot be null".to_string(),
            });
        }
        if engine_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Engine handle cannot be null".to_string(),
            });
        }

        let serializer =
            unsafe { serialization_core::get_serializer_mut(serializer_ptr as *mut c_void)? };
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };

        serialization_core::serialize_module(serializer, engine.inner(), &data)
    })();

    match result {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(jarray) => jarray.into_raw(),
            Err(e) => {
                jni_utils::throw_jni_exception(
                    &mut env,
                    &WasmtimeError::Internal {
                        message: format!("Failed to create Java byte array: {}", e),
                    },
                );
                std::ptr::null_mut()
            }
        },
        Err(error) => {
            jni_utils::throw_jni_exception(&mut env, &error);
            std::ptr::null_mut()
        }
    }
}

/// Deserialize module bytes
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeDeserialize(
    mut env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
    engine_ptr: jlong,
    serialized_bytes: jbyteArray,
) -> jlong {
    // Convert byte array to Vec<u8>
    let data = match (|| -> Result<Vec<u8>, jni::errors::Error> {
        let byte_array = unsafe { JByteArray::from_raw(serialized_bytes) };
        let array_elements =
            unsafe { env.get_array_elements(&byte_array, jni::objects::ReleaseMode::NoCopyBack)? };
        let len = env.get_array_length(&byte_array)? as usize;
        let slice =
            unsafe { std::slice::from_raw_parts(array_elements.as_ptr() as *const u8, len) };
        Ok(slice.to_vec())
    })() {
        Ok(data) => data,
        Err(_) => return 0 as jlong,
    };

    // Perform deserialization
    jni_utils::jni_try_ptr(&mut env, || {
        if serializer_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Serializer handle cannot be null".to_string(),
            });
        }
        if engine_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Engine handle cannot be null".to_string(),
            });
        }

        let serializer =
            unsafe { serialization_core::get_serializer_mut(serializer_ptr as *mut c_void)? };
        let engine = unsafe { crate::engine::core::get_engine_ref(engine_ptr as *const c_void)? };

        // Use the new function that handles decompression and creates our wrapper Module
        let module = serialization_core::deserialize_module_to_wrapper(serializer, engine, &data)?;
        // Return boxed module - jni_try_ptr handles Box::into_raw conversion
        Ok(Box::new(module))
    }) as jlong
}

/// Clear serializer cache
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeClearCache(
    mut env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
) -> jboolean {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        if serializer_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Serializer handle cannot be null".to_string(),
            });
        }

        let serializer =
            unsafe { serialization_core::get_serializer_mut(serializer_ptr as *mut c_void)? };
        serialization_core::clear_cache(serializer);
        Ok(1)
    })
}

/// Get cache entry count
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeGetCacheEntryCount(
    _env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
) -> jint {
    if serializer_ptr == 0 {
        return 0;
    }

    match unsafe { serialization_core::get_serializer_ref(serializer_ptr as *const c_void) } {
        Ok(serializer) => serialization_core::get_cache_info(serializer).entry_count as jint,
        Err(_) => 0,
    }
}

/// Get cache total size
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeGetCacheTotalSize(
    _env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
) -> jlong {
    if serializer_ptr == 0 {
        return 0;
    }

    match unsafe { serialization_core::get_serializer_ref(serializer_ptr as *const c_void) } {
        Ok(serializer) => serialization_core::get_cache_info(serializer).total_size as jlong,
        Err(_) => 0,
    }
}

/// Get cache hit rate
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeGetCacheHitRate(
    _env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
) -> jdouble {
    if serializer_ptr == 0 {
        return 0.0;
    }

    match unsafe { serialization_core::get_serializer_ref(serializer_ptr as *const c_void) } {
        Ok(serializer) => serialization_core::get_cache_info(serializer).hit_rate as jdouble,
        Err(_) => 0.0,
    }
}

/// Destroy serializer and free resources
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniSerializer_nativeDestroy(
    _env: JNIEnv,
    _class: JClass,
    serializer_ptr: jlong,
) {
    if serializer_ptr != 0 {
        unsafe {
            serialization_core::destroy_serializer(serializer_ptr as *mut c_void);
        }
    }
}
