//! JNI bindings for Hot Reload operations

use jni::JNIEnv;
use jni::objects::{JClass, JString, JObject};
use jni::sys::{jlong, jint, jboolean, jobject, jfloat, jdouble};

use crate::hot_reload::{HotReloadManager, HotReloadConfig, SwapStrategy, LoadRequest, LoadPriority, ValidationConfig};
// Import SemanticVersion from hot_reload module since it has the implementation
use crate::hot_reload::SemanticVersion;
use crate::error::{jni_utils, WasmtimeError};
use crate::engine::Engine as WasmtimeEngine;
use std::sync::Arc;
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};

/// Create a new hot reload manager
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeCreateHotReloadManager(
    mut env: JNIEnv,
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
    jni_utils::jni_try_ptr(&mut env, || {
        if engine_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid engine pointer".to_string()
            });
        }

        unsafe {
            let engine_ptr = engine_ptr as *const WasmtimeEngine;
            if engine_ptr.is_null() {
                return Err(WasmtimeError::InvalidParameter {
                    message: "Null engine pointer".to_string()
                });
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
                Ok(manager) => Ok(Box::new(manager)),
                Err(e) => Err(e),
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
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeStartHotSwap<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    manager_ptr: jlong,
    component_name: JString<'local>,
    version_string: JString<'local>,
    swap_strategy_type: jint,
    strategy_param1: jlong,
    strategy_param2: jlong,
    strategy_param3: jdouble,
) -> JString<'local> {
    // Extract string conversions before jni_try_with_default to avoid borrowing conflicts
    let name: String = match env.get_string(&component_name) {
        Ok(s) => s.into(),
        Err(_) => return JString::from(JObject::null()),
    };
    let version_str: String = match env.get_string(&version_string) {
        Ok(s) => s.into(),
        Err(_) => return JString::from(JObject::null()),
    };

    match (|| -> Result<String, WasmtimeError> {
        if manager_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid manager pointer".to_string() });
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };

        // Parse semantic version for hot reload
        let version = SemanticVersion::parse(&version_str)
            .map_err(|_| WasmtimeError::InvalidParameter { message: "Invalid semantic version format".to_string() })?;

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
        Ok(operation_id)
    })() {
        Ok(operation_id) => {
            match env.new_string(operation_id) {
                Ok(jstring) => jstring,
                Err(_) => JString::from(JObject::null()),
            }
        },
        Err(_) => JString::from(JObject::null()),
    }
}

/// Get the status of a hot swap operation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeGetSwapStatus(
    mut env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
    operation_id: JString,
) -> jobject {
    // Extract operation ID before jni_try to avoid borrowing conflict
    let op_id: String = match env.get_string(&operation_id) {
        Ok(s) => s.into(),
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &WasmtimeError::RuntimeError {
                message: format!("Failed to get operation ID: {}", e)
            });
            return JObject::null().into_raw();
        }
    };

    jni_utils::jni_try_object(&mut env, |env| {
        if manager_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid manager pointer".to_string() });
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };

        match manager.get_swap_status(&op_id)? {
            Some(operation) => {
                // Create a Java object representing the swap operation
                let class = env.find_class("ai/tegmentum/wasmtime4j/jni/HotSwapStatus")
                    .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to find class: {}", e) })?;
                let obj = env.alloc_object(class)
                    .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to alloc object: {}", e) })?;

                // Set fields on the Java object
                env.set_field(&obj, "operationId", "Ljava/lang/String;",
                    (&JObject::from(env.new_string(operation.operation_id)
                        .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to create string: {}", e) })?)).into())
                    .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
                env.set_field(&obj, "componentName", "Ljava/lang/String;",
                    (&JObject::from(env.new_string(operation.component_name)
                        .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to create string: {}", e) })?)).into())
                    .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
                env.set_field(&obj, "status", "I",
                    (operation.status as u8 as jint).into())
                    .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
                env.set_field(&obj, "progress", "F",
                    (operation.progress as jfloat).into())
                    .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;

                Ok(unsafe { JObject::from_raw(obj.into_raw()) })
            }
            None => Ok(JObject::null()),
        }
    })
}

/// Cancel a hot swap operation
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeCancelHotSwap(
    mut env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
    operation_id: JString,
) -> jboolean {
    // Extract string conversion before jni_try_with_default to avoid borrowing conflicts
    let op_id: String = match env.get_string(&operation_id) {
        Ok(s) => s.into(),
        Err(_) => return 0,
    };

    jni_utils::jni_try_with_default(&mut env, 0, || {
        if manager_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid manager pointer".to_string() });
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };

        manager.cancel_hot_swap(&op_id)?;
        Ok(1 as jboolean)
    })
}

/// Load a component asynchronously
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeLoadComponentAsync<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    manager_ptr: jlong,
    component_name: JString<'local>,
    component_path: JString<'local>,
    version_string: JString<'local>,
    priority: jint,
    validate_interfaces: jboolean,
    validate_dependencies: jboolean,
    validate_security: jboolean,
    validate_performance: jboolean,
    timeout_secs: jlong,
) -> JString<'local> {
    // Extract string conversions before jni_try_with_default to avoid borrowing conflicts
    let name: String = match env.get_string(&component_name) {
        Ok(s) => s.into(),
        Err(_) => return JString::from(JObject::null()),
    };
    let path: String = match env.get_string(&component_path) {
        Ok(s) => s.into(),
        Err(_) => return JString::from(JObject::null()),
    };
    let version_str: String = match env.get_string(&version_string) {
        Ok(s) => s.into(),
        Err(_) => return JString::from(JObject::null()),
    };

    match (|| -> Result<String, WasmtimeError> {
        if manager_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid manager pointer".to_string() });
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };

        // Parse semantic version for hot reload
        let version = SemanticVersion::parse(&version_str)
            .map_err(|_| WasmtimeError::InvalidParameter { message: "Invalid semantic version format".to_string() })?;

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
        Ok(request_id)
    })() {
        Ok(request_id) => {
            match env.new_string(request_id) {
                Ok(jstring) => jstring,
                Err(_) => JString::from(JObject::null()),
            }
        },
        Err(_) => JString::from(JObject::null()),
    }
}

/// Get hot reload metrics
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_JniHotReloadManager_nativeGetMetrics(
    mut env: JNIEnv,
    _class: JClass,
    manager_ptr: jlong,
) -> jobject {
    // Extract class lookup before jni_try_with_default to avoid borrowing conflicts
    let class = match env.find_class("ai/tegmentum/wasmtime4j/jni/HotReloadMetrics") {
        Ok(c) => c,
        Err(_) => return JObject::null().into_raw(),
    };

    match (|| -> Result<jobject, WasmtimeError> {
        if manager_ptr == 0 {
            return Err(WasmtimeError::InvalidParameter { message: "Invalid manager pointer".to_string() });
        }

        let manager = unsafe { &*(manager_ptr as *const HotReloadManager) };
        let metrics = manager.get_metrics()?;

        // Create a Java object representing the metrics
        let obj = env.alloc_object(class)
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to alloc object: {}", e) })?;

        // Set fields on the Java object
        env.set_field(&obj, "totalSwaps", "J",
            (metrics.total_swaps as jlong).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "successfulSwaps", "J",
            (metrics.successful_swaps as jlong).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "failedSwaps", "J",
            (metrics.failed_swaps as jlong).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "rollbacks", "J",
            (metrics.rollbacks as jlong).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "avgSwapTimeMs", "J",
            (metrics.avg_swap_time.as_millis() as jlong).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "currentActiveSwaps", "I",
            (metrics.current_active_swaps as jint).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "componentsLoaded", "J",
            (metrics.components_loaded as jlong).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;
        env.set_field(&obj, "cacheEfficiency", "F",
            (metrics.cache_efficiency as jfloat).into())
            .map_err(|e| WasmtimeError::RuntimeError { message: format!("Failed to set field: {}", e) })?;

        Ok(obj.into_raw())
    })() {
        Ok(obj) => obj,
        Err(_) => JObject::null().into_raw(),
    }
}

// Helper function to generate UUID-like strings for request IDs
fn generate_uuid() -> String {
    let timestamp = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos();
    format!("hotreload-{}", timestamp)
}