//! JNI bindings for ResourceLimiter operations

use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jlong, jint, jstring};

use crate::store_limiter;

/// JNI binding for JniResourceLimiter.nativeCreate
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeCreate(
    _env: JNIEnv,
    _class: JClass,
    max_memory_bytes: jlong,
    max_memory_pages: jlong,
    max_table_elements: jlong,
    max_instances: jint,
    max_tables: jint,
    max_memories: jint,
) -> jlong {
    unsafe {
        store_limiter::wasmtime4j_limiter_create(
            max_memory_bytes,
            max_memory_pages,
            max_table_elements,
            max_instances,
            max_tables,
            max_memories,
        )
    }
}

/// JNI binding for JniResourceLimiter.nativeCreateDefault
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeCreateDefault(
    _env: JNIEnv,
    _class: JClass,
) -> jlong {
    unsafe {
        store_limiter::wasmtime4j_limiter_create_default()
    }
}

/// JNI binding for JniResourceLimiter.nativeFree
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeFree(
    _env: JNIEnv,
    _class: JClass,
    limiter_id: jlong,
) -> jint {
    unsafe {
        store_limiter::wasmtime4j_limiter_free(limiter_id)
    }
}

/// JNI binding for JniResourceLimiter.nativeAllowMemoryGrow
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeAllowMemoryGrow(
    _env: JNIEnv,
    _class: JClass,
    limiter_id: jlong,
    current_pages: jlong,
    requested_pages: jlong,
) -> jint {
    unsafe {
        store_limiter::wasmtime4j_limiter_allow_memory_grow(limiter_id, current_pages, requested_pages)
    }
}

/// JNI binding for JniResourceLimiter.nativeAllowTableGrow
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeAllowTableGrow(
    _env: JNIEnv,
    _class: JClass,
    limiter_id: jlong,
    current_elements: jlong,
    requested_elements: jlong,
) -> jint {
    unsafe {
        store_limiter::wasmtime4j_limiter_allow_table_grow(limiter_id, current_elements, requested_elements)
    }
}

/// JNI binding for JniResourceLimiter.nativeGetStatsJson
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeGetStatsJson(
    mut env: JNIEnv,
    _class: JClass,
    limiter_id: jlong,
) -> jstring {
    unsafe {
        let json_ptr = store_limiter::wasmtime4j_limiter_get_stats_json(limiter_id);
        if json_ptr.is_null() {
            return std::ptr::null_mut();
        }

        let c_str = std::ffi::CStr::from_ptr(json_ptr);
        let result = match c_str.to_str() {
            Ok(s) => env.new_string(s).map(|js| js.into_raw()).unwrap_or(std::ptr::null_mut()),
            Err(_) => std::ptr::null_mut(),
        };

        // Free the native string
        store_limiter::wasmtime4j_limiter_string_free(json_ptr);

        result
    }
}

/// JNI binding for JniResourceLimiter.nativeResetStats
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeResetStats(
    _env: JNIEnv,
    _class: JClass,
    limiter_id: jlong,
) -> jint {
    unsafe {
        store_limiter::wasmtime4j_limiter_reset_stats(limiter_id)
    }
}

/// JNI binding for JniResourceLimiter.nativeGetCount
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_execution_JniResourceLimiter_nativeGetCount(
    _env: JNIEnv,
    _class: JClass,
) -> jint {
    unsafe {
        store_limiter::wasmtime4j_limiter_get_count()
    }
}
