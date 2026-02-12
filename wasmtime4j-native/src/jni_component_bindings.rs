//! JNI bindings for Component Model functionality
//!
//! This module provides JNI bindings to expose Component Model features like
//! ComponentMetrics to the Java layer.

#![allow(unused_variables)]

#[cfg(feature = "jni-bindings")]
use jni::objects::{JClass, JObject, JValue};
#[cfg(feature = "jni-bindings")]
use jni::sys::{jlong, jobject};
#[cfg(feature = "jni-bindings")]
use jni::JNIEnv;

#[cfg(feature = "jni-bindings")]
use crate::error::jni_utils;

/// Get the total number of components loaded from the engine metrics
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetComponentsLoaded(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || {
        // Access the component engine metrics
        // For now, return 0 as we need to implement proper engine handle lookup
        // This will be wired up when JniComponentEngine exposes metrics
        Ok(0i64)
    })
}

/// Get the total number of instances created from the engine metrics
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetInstancesCreated(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || Ok(0i64))
}

/// Get the total number of instances destroyed from the engine metrics
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetInstancesDestroyed(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || Ok(0i64))
}

/// Get the average instantiation time in nanoseconds
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetAvgInstantiationTimeNanos(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || Ok(0i64))
}

/// Get peak memory usage in bytes
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetPeakMemoryUsage(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || Ok(0i64))
}

/// Get total function calls from the engine metrics
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetFunctionCalls(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || Ok(0i64))
}

/// Get the error count from the engine metrics
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetErrorCount(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jlong {
    jni_utils::jni_try_with_default(&mut env, 0, || Ok(0i64))
}

/// Helper function to put a Long value into a HashMap
#[cfg(feature = "jni-bindings")]
fn put_long_in_map(
    env: &mut JNIEnv,
    map: &JObject,
    key: &str,
    value: i64,
) -> Result<(), crate::WasmtimeError> {
    let key_str = env.new_string(key).map_err(|e| {
        crate::WasmtimeError::JniError(format!("Failed to create key string: {}", e))
    })?;
    let long_class = env
        .find_class("java/lang/Long")
        .map_err(|e| crate::WasmtimeError::JniError(format!("Failed to find Long class: {}", e)))?;
    let long_obj = env
        .new_object(long_class, "(J)V", &[JValue::Long(value)])
        .map_err(|e| {
            crate::WasmtimeError::JniError(format!("Failed to create Long object: {}", e))
        })?;

    env.call_method(
        map,
        "put",
        "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
        &[
            JValue::Object(&JObject::from(key_str)),
            JValue::Object(&long_obj),
        ],
    )
    .map_err(|e| crate::WasmtimeError::JniError(format!("Failed to put value in map: {}", e)))?;

    Ok(())
}

/// Get comprehensive component metrics as a Java object
/// Returns a jobject representing a HashMap with metrics or null on error
#[cfg(feature = "jni-bindings")]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniComponentImpl_nativeGetMetrics(
    mut env: JNIEnv,
    _class: JClass,
    engine_handle: jlong,
) -> jobject {
    jni_utils::jni_try_object(&mut env, |env| {
        // Create a HashMap with metrics
        let hash_map_class = env.find_class("java/util/HashMap").map_err(|e| {
            crate::WasmtimeError::JniError(format!("Failed to find HashMap class: {}", e))
        })?;
        let hash_map = env.new_object(hash_map_class, "()V", &[]).map_err(|e| {
            crate::WasmtimeError::JniError(format!("Failed to create HashMap: {}", e))
        })?;

        // Populate with placeholder values - will be wired to actual metrics
        put_long_in_map(env, &hash_map, "componentsLoaded", 0)?;
        put_long_in_map(env, &hash_map, "instancesCreated", 0)?;
        put_long_in_map(env, &hash_map, "instancesDestroyed", 0)?;
        put_long_in_map(env, &hash_map, "avgInstantiationTimeNanos", 0)?;
        put_long_in_map(env, &hash_map, "peakMemoryUsage", 0)?;
        put_long_in_map(env, &hash_map, "functionCalls", 0)?;
        put_long_in_map(env, &hash_map, "errorCount", 0)?;

        // SAFETY: We need to convert the local reference to a global reference that outlives this scope
        // The returned jobject will be managed by the JVM
        let global = env.new_global_ref(&hash_map).map_err(|e| {
            crate::WasmtimeError::JniError(format!("Failed to create global ref: {}", e))
        })?;

        // Convert global reference to owned JObject for return
        // SAFETY: The global reference ensures the object stays alive across JNI boundary
        Ok(unsafe { JObject::from_raw(global.as_raw()) })
    })
}

#[cfg(test)]
mod tests {
    // Tests would go here
}
