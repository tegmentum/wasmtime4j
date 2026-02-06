//! JNI bindings for WASI Preview 2 clocks operations
//!
//! This module provides JNI functions for wasi:clocks interfaces including
//! monotonic clock, wall clock, and timezone operations.

use jni::objects::{JClass, JObject};
use jni::sys::{jint, jlong, jlongArray};
use jni::JNIEnv;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_clocks_helpers;
use crate::{jni_validate_handle, jni_deref_ptr};

/// Get the current monotonic clock instant in nanoseconds
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiMonotonicClock_nativeNow(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", -1);
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", -1);

    // Call helper function
    match wasi_clocks_helpers::monotonic_now(context) {
        Ok(instant) => instant as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get monotonic clock: {}", e),
            );
            -1
        }
    }
}

/// Get the monotonic clock resolution in nanoseconds
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiMonotonicClock_nativeResolution(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", -1);
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", -1);

    // Call helper function
    match wasi_clocks_helpers::monotonic_resolution(context) {
        Ok(resolution) => resolution as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get monotonic clock resolution: {}", e),
            );
            -1
        }
    }
}

/// Subscribe to monotonic clock at a specific instant
///
/// Returns a pollable ID that becomes ready when the clock reaches the specified instant.
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiMonotonicClock_nativeSubscribeInstant(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    when: jlong,
) -> jlong {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", -1);
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", -1);

    // Call helper function
    match wasi_clocks_helpers::monotonic_subscribe_instant(context, when as u64) {
        Ok(pollable_id) => pollable_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to subscribe to instant: {}", e),
            );
            -1
        }
    }
}

/// Subscribe to monotonic clock for a duration
///
/// Returns a pollable ID that becomes ready after the specified duration elapses.
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiMonotonicClock_nativeSubscribeDuration(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    duration: jlong,
) -> jlong {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", -1);
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", -1);

    // Call helper function
    match wasi_clocks_helpers::monotonic_subscribe_duration(context, duration as u64) {
        Ok(pollable_id) => pollable_id as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to subscribe to duration: {}", e),
            );
            -1
        }
    }
}

/// Get the current wall clock time
///
/// Returns an array of [seconds, nanoseconds] since Unix epoch.
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiWallClock_nativeNow(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlongArray {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null().into_raw());
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", JObject::null().into_raw());

    // Call helper function
    let datetime = match wasi_clocks_helpers::wall_clock_now(context) {
        Ok(dt) => dt,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get wall clock time: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Create long array [seconds, nanoseconds]
    let array = match env.new_long_array(2) {
        Ok(arr) => arr,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create array: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    let values = [datetime.seconds as jlong, datetime.nanoseconds as jlong];
    if let Err(e) = env.set_long_array_region(&array, 0, &values) {
        let _ = env.throw_new(
            "java/lang/RuntimeException",
            format!("Failed to set array values: {}", e),
        );
        return JObject::null().into_raw();
    }

    array.into_raw()
}

/// Get the wall clock resolution
///
/// Returns an array of [seconds, nanoseconds] representing the clock resolution.
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiWallClock_nativeResolution(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlongArray {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null().into_raw());
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", JObject::null().into_raw());

    // Call helper function
    let datetime = match wasi_clocks_helpers::wall_clock_resolution(context) {
        Ok(dt) => dt,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get wall clock resolution: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    // Create long array [seconds, nanoseconds]
    let array = match env.new_long_array(2) {
        Ok(arr) => arr,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create array: {}", e),
            );
            return JObject::null().into_raw();
        }
    };

    let values = [datetime.seconds as jlong, datetime.nanoseconds as jlong];
    if let Err(e) = env.set_long_array_region(&array, 0, &values) {
        let _ = env.throw_new(
            "java/lang/RuntimeException",
            format!("Failed to set array values: {}", e),
        );
        return JObject::null().into_raw();
    }

    array.into_raw()
}

/// Get timezone display information for a specific datetime
///
/// Returns a TimezoneDisplay object containing UTC offset, name, and DST status.
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiTimezone_nativeDisplay<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    context_handle: jlong,
    seconds: jlong,
    nanoseconds: jint,
) -> JObject<'local> {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", JObject::null());
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", JObject::null());

    // Create DateTime
    let datetime = wasi_clocks_helpers::DateTime {
        seconds: seconds as u64,
        nanoseconds: nanoseconds as u32,
    };

    // Call helper function
    let display = match wasi_clocks_helpers::timezone_display(context, datetime) {
        Ok(d) => d,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get timezone display: {}", e),
            );
            return JObject::null();
        }
    };

    // Create TimezoneDisplay Java object
    let class = match env.find_class("ai/tegmentum/wasmtime4j/wasi/clocks/TimezoneDisplay") {
        Ok(c) => c,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to find TimezoneDisplay class: {}", e),
            );
            return JObject::null();
        }
    };

    let name_jstring = match env.new_string(&display.name) {
        Ok(s) => s,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create timezone name string: {}", e),
            );
            return JObject::null();
        }
    };

    let result = match env.new_object(
        &class,
        "(ILjava/lang/String;Z)V",
        &[
            jni::objects::JValue::from(display.utc_offset_seconds),
            jni::objects::JValue::from(&name_jstring),
            jni::objects::JValue::from(if display.in_daylight_saving_time { 1u8 } else { 0u8 }),
        ],
    ) {
        Ok(obj) => obj,
        Err(e) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("Failed to create TimezoneDisplay object: {}", e),
            );
            return JObject::null();
        }
    };

    result
}

/// Get timezone UTC offset in seconds for a specific datetime
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_clocks_JniWasiTimezone_nativeUtcOffset(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    seconds: jlong,
    nanoseconds: jint,
) -> jint {
    // Validate and get context using macros
    jni_validate_handle!(env, context_handle, "context", 0);
    let context = jni_deref_ptr!(env, context_handle, WasiPreview2Context, "Context", 0);

    // Create DateTime
    let datetime = wasi_clocks_helpers::DateTime {
        seconds: seconds as u64,
        nanoseconds: nanoseconds as u32,
    };

    // Call helper function
    match wasi_clocks_helpers::timezone_utc_offset(context, datetime) {
        Ok(offset) => offset,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get UTC offset: {}", e),
            );
            0
        }
    }
}
