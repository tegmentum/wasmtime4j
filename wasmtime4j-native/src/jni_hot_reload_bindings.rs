//! JNI bindings for Hot Reload operations

use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jlong, jint, jboolean, jobject, jfloat, jdouble};

use crate::hot_reload::{HotReloadManager, HotReloadConfig, SwapStrategy, LoadRequest, LoadPriority, ValidationConfig};
use crate::component_orchestration::dependency_resolution::SemanticVersion;
use crate::error::jni_utils;
use crate::component_core::EnhancedComponentEngine;
use std::sync::Arc;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use std::os::raw::c_void;

/// Create a new hot reload manager
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeCreateHotReloadManager(
    env: JNIEnv,
    _class: JClass,
    engine_ptr: jlong,
    validation_enabled: jboolean,
    state_preservation_enabled: jboolean,
    debounce_delay_ms: jlong,
    precompilation_enabled: jboolean,
    max_reload_attempts: jint,
    health_check_interval_secs: jlong,
    loader_thread_count: jint,
    cache_size: jint,
) -> jlong {
    jni_utils::jni_try_ptr(env, || {
        if engine_ptr == 0 {
            return std::ptr::null_mut();
        }

        unsafe {
            let engine_ptr = engine_ptr as *const EnhancedComponentEngine;
            if engine_ptr.is_null() {
                return std::ptr::null_mut();
            }

            let engine = Arc::new(std::ptr::read(engine_ptr));

            let config = HotReloadConfig {
                validation_enabled: validation_enabled != 0,
                state_preservation_enabled: state_preservation_enabled != 0,
                debounce_delay_ms: debounce_delay_ms as u64,
                precompilation_enabled: precompilation_enabled != 0,
                max_reload_attempts: max_reload_attempts as u32,
                health_check_interval: Duration::from_secs(health_check_interval_secs as u64),
                default_swap_strategy: SwapStrategy::Canary {
                    initial_percentage: 10.0,
                    increment_percentage: 25.0,
                    increment_interval: Duration::from_secs(60),
                    success_threshold: 0.99,
                },
                loader_thread_count: loader_thread_count as usize,
                cache_size: cache_size as usize,
            };

            match HotReloadManager::new(engine, config) {
                Ok(manager) => Box::into_raw(Box::new(manager)) as *mut c_void,
                Err(_) => std::ptr::null_mut(),
            }
        }
    }) as jlong
}

/// Destroy a hot reload manager
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeDestroyHotReloadManager(
    _env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
) {
    if manager_ptr != 0 {
        unsafe {
            let _ = Box::from_raw(manager_ptr as *mut HotReloadManager);
        }
    }
}

/// Start a hot swap operation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeStartHotSwap(
    env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
    component_name: JString,
    version_string: JString,
    swap_strategy_type: jint,
    strategy_param1: jlong,
    strategy_param2: jlong,
    strategy_param3: jdouble,
) -> JString {
    jni_utils::jni_try_result(env, || {
        if manager_ptr == 0 {
            return Err("Invalid manager pointer".into());
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };

        let name: String = env.get_string(component_name)?.into();
        let version_str: String = env.get_string(version_string)?.into();

        let version = SemanticVersion::parse(&version_str)
            .map_err(|_| "Invalid semantic version".to_string())?;

        // Convert swap strategy based on type
        let strategy = match swap_strategy_type {
            0 => Some(SwapStrategy::Immediate),
            1 => Some(SwapStrategy::Canary {
                initial_percentage: strategy_param1 as f32 / 100.0,
                increment_percentage: strategy_param2 as f32 / 100.0,
                increment_interval: Duration::from_secs(60),
                success_threshold: strategy_param3 as f32,
            }),
            2 => Some(SwapStrategy::BlueGreen),
            3 => Some(SwapStrategy::RollingUpdate {
                batch_size: strategy_param1 as usize,
                batch_interval: Duration::from_secs(strategy_param2 as u64),
            }),
            4 => Some(SwapStrategy::ABTest {
                test_percentage: strategy_param1 as f32 / 100.0,
                test_duration: Duration::from_secs(strategy_param2 as u64),
                success_metrics: vec!["response_time".to_string(), "error_rate".to_string()],
            }),
            _ => None,
        };

        let operation_id = manager.start_hot_swap(name, version, strategy)?;
        Ok(env.new_string(operation_id)?)
    }).unwrap_or_else(|_| JString::from(JObject::null()))
}

/// Get the status of a hot swap operation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeGetSwapStatus(
    env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
    operation_id: JString,
) -> jobject {
    jni_utils::jni_try_result(env, || {
        if manager_ptr == 0 {
            return Err("Invalid manager pointer".into());
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };
        let op_id: String = env.get_string(operation_id)?.into();

        match manager.get_swap_status(&op_id)? {
            Some(operation) => {
                // Create a Java object representing the swap operation
                let class = env.find_class("ai/tegmentum/wasmtime4j/jni/HotSwapStatus")?;
                let obj = env.alloc_object(class)?;

                // Set fields on the Java object
                env.set_field(obj, "operationId", "Ljava/lang/String;",
                    env.new_string(operation.operation_id)?.into())?;
                env.set_field(obj, "componentName", "Ljava/lang/String;",
                    env.new_string(operation.component_name)?.into())?;
                env.set_field(obj, "status", "I",
                    (operation.status as u8 as jint).into())?;
                env.set_field(obj, "progress", "F",
                    (operation.progress as jfloat).into())?;

                Ok(obj.into_raw())
            }
            None => Ok(JObject::null().into_raw()),
        }
    }).unwrap_or_else(|_| JObject::null().into_raw())
}

/// Cancel a hot swap operation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeCancelHotSwap(
    env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
    operation_id: JString,
) -> jboolean {
    jni_utils::jni_try_result(env, || {
        if manager_ptr == 0 {
            return Err("Invalid manager pointer".into());
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };
        let op_id: String = env.get_string(operation_id)?.into();

        manager.cancel_hot_swap(&op_id)?;
        Ok(1 as jboolean)
    }).unwrap_or(0)
}

/// Load a component asynchronously
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeLoadComponentAsync(
    env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
    component_name: JString,
    component_path: JString,
    version_string: JString,
    priority: jint,
    validate_interfaces: jboolean,
    validate_dependencies: jboolean,
    validate_security: jboolean,
    validate_performance: jboolean,
    timeout_secs: jlong,
) -> JString {
    jni_utils::jni_try_result(env, || {
        if manager_ptr == 0 {
            return Err("Invalid manager pointer".into());
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };

        let name: String = env.get_string(component_name)?.into();
        let path: String = env.get_string(component_path)?.into();
        let version_str: String = env.get_string(version_string)?.into();

        let version = SemanticVersion::parse(&version_str)
            .map_err(|_| "Invalid semantic version".to_string())?;

        let load_priority = match priority {
            0 => LoadPriority::Low,
            1 => LoadPriority::Normal,
            2 => LoadPriority::High,
            3 => LoadPriority::Critical,
            _ => LoadPriority::Normal,
        };

        let validation_config = ValidationConfig {
            validate_interfaces: validate_interfaces != 0,
            validate_dependencies: validate_dependencies != 0,
            validate_security: validate_security != 0,
            validate_performance: validate_performance != 0,
            timeout: Duration::from_secs(timeout_secs as u64),
        };

        let request = LoadRequest {
            request_id: generate_uuid(),
            component_name: name,
            component_path: path,
            version,
            priority: load_priority,
            validation_config,
            requested_at: Instant::now(),
        };

        let request_id = manager.load_component_async(request)?;
        Ok(env.new_string(request_id)?)
    }).unwrap_or_else(|_| JString::from(JObject::null()))
}

/// Get hot reload metrics
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeGetMetrics(
    env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
) -> jobject {
    jni_utils::jni_try_result(env, || {
        if manager_ptr == 0 {
            return Err("Invalid manager pointer".into());
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };
        let metrics = manager.get_metrics()?;

        // Create a Java object representing the metrics
        let class = env.find_class("ai/tegmentum/wasmtime4j/jni/HotReloadMetrics")?;
        let obj = env.alloc_object(class)?;

        // Set fields on the Java object
        env.set_field(obj, "totalSwaps", "J",
            (metrics.total_swaps as jlong).into())?;
        env.set_field(obj, "successfulSwaps", "J",
            (metrics.successful_swaps as jlong).into())?;
        env.set_field(obj, "failedSwaps", "J",
            (metrics.failed_swaps as jlong).into())?;
        env.set_field(obj, "rollbacks", "J",
            (metrics.rollbacks as jlong).into())?;
        env.set_field(obj, "avgSwapTimeMs", "J",
            (metrics.avg_swap_time.as_millis() as jlong).into())?;
        env.set_field(obj, "currentActiveSwaps", "I",
            (metrics.current_active_swaps as jint).into())?;
        env.set_field(obj, "componentsLoaded", "J",
            (metrics.components_loaded as jlong).into())?;
        env.set_field(obj, "cacheEfficiency", "F",
            (metrics.cache_efficiency as jfloat).into())?;

        Ok(obj.into_raw())
    }).unwrap_or_else(|_| JObject::null().into_raw())
}

// Helper function to generate UUID-like strings for request IDs
fn generate_uuid() -> String {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    format!("hotreload-{}", timestamp)
}