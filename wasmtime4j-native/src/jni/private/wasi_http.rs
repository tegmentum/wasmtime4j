//! WASI HTTP JNI bindings
//!
//! This module provides JNI bindings for WASI HTTP functionality,
//! allowing Java code to create and manage WASI HTTP contexts.

use std::time::Duration;

use jni::objects::{JClass, JObject, JObjectArray, JString};
use jni::sys::{jboolean, jint, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;

use crate::error::{jni_utils, WasmtimeError};
use crate::wasi_http::{WasiHttpConfig, WasiHttpConfigBuilder, WasiHttpContext};

/// Helper to extract a Java String from a JNI JObject reference, returning None if null
fn get_java_string(env: &mut JNIEnv, obj: &JObject) -> Result<Option<String>, WasmtimeError> {
    if obj.is_null() {
        return Ok(None);
    }
    let jstr = JString::from(unsafe { JObject::from_raw(obj.as_raw()) });
    let s: String = env
        .get_string(&jstr)
        .map_err(|e| WasmtimeError::Internal {
            message: format!("Failed to get Java string: {}", e),
        })?
        .into();
    Ok(Some(s))
}

/// Helper to iterate a Java String[] array and collect into a Vec<String>
fn get_string_array(env: &mut JNIEnv, array: &JObject) -> Result<Vec<String>, WasmtimeError> {
    if array.is_null() {
        return Ok(Vec::new());
    }
    let jarray = JObjectArray::from(unsafe { JObject::from_raw(array.as_raw()) });
    let len = env
        .get_array_length(&jarray)
        .map_err(|e| WasmtimeError::Internal {
            message: format!("Failed to get array length: {}", e),
        })?;
    let mut result = Vec::with_capacity(len as usize);
    for i in 0..len {
        let elem =
            env.get_object_array_element(&jarray, i)
                .map_err(|e| WasmtimeError::Internal {
                    message: format!("Failed to get array element {}: {}", i, e),
                })?;
        if let Some(s) = get_java_string(env, &elem)? {
            result.push(s);
        }
    }
    Ok(result)
}

/// Build a WasiHttpConfig from individual parameters passed from Java.
///
/// Parameters use sentinel values for "not set":
/// - timeout values: -1 means not set (no timeout)
/// - max values: -1 means not set (no limit)
fn build_config_from_params(
    env: &mut JNIEnv,
    allowed_hosts: &JObject,
    blocked_hosts: &JObject,
    connect_timeout_ms: jlong,
    read_timeout_ms: jlong,
    write_timeout_ms: jlong,
    max_connections: i32,
    max_connections_per_host: i32,
    max_request_body_size: jlong,
    max_response_body_size: jlong,
    https_required: jboolean,
    certificate_validation: jboolean,
    http2_enabled: jboolean,
    connection_pooling: jboolean,
    follow_redirects: jboolean,
    max_redirects: i32,
    user_agent: &JObject,
) -> Result<WasiHttpConfig, WasmtimeError> {
    let mut builder = WasiHttpConfigBuilder::new();

    // Allowed hosts
    let hosts = get_string_array(env, allowed_hosts)?;
    for host in hosts {
        builder = builder.allow_host(host);
    }

    // Blocked hosts
    let hosts = get_string_array(env, blocked_hosts)?;
    for host in hosts {
        builder = builder.block_host(host);
    }

    // Timeouts
    if connect_timeout_ms >= 0 {
        builder = builder.with_connect_timeout(Duration::from_millis(connect_timeout_ms as u64));
    }
    if read_timeout_ms >= 0 {
        builder = builder.with_read_timeout(Duration::from_millis(read_timeout_ms as u64));
    }
    if write_timeout_ms >= 0 {
        builder = builder.with_write_timeout(Duration::from_millis(write_timeout_ms as u64));
    }

    // Connection limits
    if max_connections >= 0 {
        builder = builder.with_max_connections(max_connections as u32);
    }
    if max_connections_per_host >= 0 {
        builder = builder.with_max_connections_per_host(max_connections_per_host as u32);
    }

    // Body size limits
    if max_request_body_size >= 0 {
        builder = builder.with_max_request_body_size(max_request_body_size as u64);
    }
    if max_response_body_size >= 0 {
        builder = builder.with_max_response_body_size(max_response_body_size as u64);
    }

    // Boolean flags
    builder = builder.require_https(https_required != JNI_FALSE);
    builder = builder.with_certificate_validation(certificate_validation != JNI_FALSE);
    builder = builder.with_http2(http2_enabled != JNI_FALSE);
    builder = builder.with_connection_pooling(connection_pooling != JNI_FALSE);
    builder = builder.follow_redirects(follow_redirects != JNI_FALSE);

    // Max redirects
    if max_redirects >= 0 {
        builder = builder.with_max_redirects(max_redirects as u32);
    }

    // User agent
    if let Some(agent) = get_java_string(env, user_agent)? {
        builder = builder.with_user_agent(agent);
    }

    Ok(builder.build())
}

/// Create a new WASI HTTP context with full config support
/// JNI binding for JniWasiHttpContext.nativeCreate
///
/// Accepts individual config parameters to avoid complex JObject field extraction.
/// Sentinel value -1 means "not set" for optional numeric parameters.
#[allow(clippy::too_many_arguments)]
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeCreate<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    allowed_hosts: JObject<'local>,
    blocked_hosts: JObject<'local>,
    connect_timeout_ms: jlong,
    read_timeout_ms: jlong,
    write_timeout_ms: jlong,
    max_connections: jint,
    max_connections_per_host: jint,
    max_request_body_size: jlong,
    max_response_body_size: jlong,
    https_required: jboolean,
    certificate_validation: jboolean,
    http2_enabled: jboolean,
    connection_pooling: jboolean,
    follow_redirects: jboolean,
    max_redirects: jint,
    user_agent: JObject<'local>,
) -> jlong {
    // Build config first (needs env for JNI string extraction)
    let config = match build_config_from_params(
        &mut env,
        &allowed_hosts,
        &blocked_hosts,
        connect_timeout_ms,
        read_timeout_ms,
        write_timeout_ms,
        max_connections,
        max_connections_per_host,
        max_request_body_size,
        max_response_body_size,
        https_required,
        certificate_validation,
        http2_enabled,
        connection_pooling,
        follow_redirects,
        max_redirects,
        &user_agent,
    ) {
        Ok(c) => c,
        Err(e) => {
            jni_utils::throw_jni_exception(&mut env, &e);
            return 0;
        }
    };

    // Create context (doesn't need env)
    jni_utils::jni_try_with_default(&mut env, 0, || {
        let ctx = WasiHttpContext::new(config)?;
        log::info!("Created WASI HTTP context with ID: {}", ctx.id());
        Ok(Box::into_raw(Box::new(ctx)) as jlong)
    })
}

/// Check if WASI HTTP context is valid
/// JNI binding for JniWasiHttpContext.nativeIsValid
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeIsValid(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jboolean {
    if ctx_handle == 0 {
        return JNI_FALSE;
    }

    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    if ctx.is_valid() {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Check if a host is allowed by the context's config
/// JNI binding for JniWasiHttpContext.nativeIsHostAllowed
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeIsHostAllowed<
    'local,
>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    ctx_handle: jlong,
    host: JObject<'local>,
) -> jboolean {
    if ctx_handle == 0 {
        return JNI_FALSE;
    }

    let host_str = match get_java_string(&mut env, &host) {
        Ok(Some(s)) => s,
        _ => return JNI_FALSE,
    };

    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    if ctx.is_host_allowed(&host_str) {
        JNI_TRUE
    } else {
        JNI_FALSE
    }
}

/// Get context ID
/// JNI binding for JniWasiHttpContext.nativeGetContextId
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeGetContextId(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }

    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.id() as jlong
}

/// Reset WASI HTTP context statistics
/// JNI binding for JniWasiHttpContext.nativeResetStats
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeResetStats(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) {
    if ctx_handle == 0 {
        return;
    }

    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.reset_stats();
}

/// Get stats - total requests
/// JNI binding for JniWasiHttpContext.nativeStatsTotalRequests
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsTotalRequests(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().total_requests() as jlong
}

/// Get stats - successful requests
/// JNI binding for JniWasiHttpContext.nativeStatsSuccessfulRequests
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsSuccessfulRequests(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().successful_requests() as jlong
}

/// Get stats - failed requests
/// JNI binding for JniWasiHttpContext.nativeStatsFailedRequests
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsFailedRequests(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().failed_requests() as jlong
}

/// Get stats - active requests
/// JNI binding for JniWasiHttpContext.nativeStatsActiveRequests
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsActiveRequests(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jint {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().active_requests() as jint
}

/// Get stats - bytes sent
/// JNI binding for JniWasiHttpContext.nativeStatsBytesSent
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsBytesSent(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().bytes_sent() as jlong
}

/// Get stats - bytes received
/// JNI binding for JniWasiHttpContext.nativeStatsBytesReceived
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsBytesReceived(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().bytes_received() as jlong
}

/// Get stats - connection timeouts
/// JNI binding for JniWasiHttpContext.nativeStatsConnectionTimeouts
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsConnectionTimeouts(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().connection_timeouts() as jlong
}

/// Get stats - read timeouts
/// JNI binding for JniWasiHttpContext.nativeStatsReadTimeouts
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsReadTimeouts(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().read_timeouts() as jlong
}

/// Get stats - blocked requests
/// JNI binding for JniWasiHttpContext.nativeStatsBlockedRequests
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsBlockedRequests(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().blocked_requests() as jlong
}

/// Get stats - body size violations
/// JNI binding for JniWasiHttpContext.nativeStatsBodySizeViolations
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsBodySizeViolations(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().body_size_violations() as jlong
}

/// Get stats - active connections
/// JNI binding for JniWasiHttpContext.nativeStatsActiveConnections
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsActiveConnections(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jint {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().active_connections() as jint
}

/// Get stats - idle connections
/// JNI binding for JniWasiHttpContext.nativeStatsIdleConnections
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsIdleConnections(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jint {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().idle_connections() as jint
}

/// Get stats - average duration in milliseconds
/// JNI binding for JniWasiHttpContext.nativeStatsAvgDurationMs
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsAvgDurationMs(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().avg_duration_ms() as jlong
}

/// Get stats - min duration in milliseconds
/// JNI binding for JniWasiHttpContext.nativeStatsMinDurationMs
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsMinDurationMs(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().min_duration_ms() as jlong
}

/// Get stats - max duration in milliseconds
/// JNI binding for JniWasiHttpContext.nativeStatsMaxDurationMs
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeStatsMaxDurationMs(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) -> jlong {
    if ctx_handle == 0 {
        return 0;
    }
    let ctx = unsafe { &*(ctx_handle as *const WasiHttpContext) };
    ctx.stats().max_duration_ms() as jlong
}

/// Free WASI HTTP context
/// JNI binding for JniWasiHttpContext.nativeFree
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_http_JniWasiHttpContext_nativeFree(
    _env: JNIEnv,
    _class: JClass,
    ctx_handle: jlong,
) {
    if ctx_handle == 0 {
        return;
    }

    unsafe {
        let _ = Box::from_raw(ctx_handle as *mut WasiHttpContext);
    }
    log::debug!("Freed WASI HTTP context");
}
