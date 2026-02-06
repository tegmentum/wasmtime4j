//! JNI bindings for WASI Preview 2 sockets operations
//!
//! This module provides JNI functions for wasi:sockets interfaces including
//! TCP sockets, UDP sockets, and network operations.

use jni::objects::{JByteArray, JClass, JObject};
use jni::sys::{jboolean, jbyte, jbyteArray, jint, jlong, jlongArray};
use jni::JNIEnv;

use crate::error::jni_utils;
use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_sockets_helpers::{self, IpAddress, IpSocketAddress};

// Helper function to extract context from handle
unsafe fn get_context<'a>(context_handle: jlong) -> Option<&'a WasiPreview2Context> {
    if context_handle == 0 {
        return None;
    }
    let ptr = context_handle as *const WasiPreview2Context;
    if ptr.is_null() {
        return None;
    }
    Some(&*ptr)
}

// Helper function to build IpSocketAddress from Java fields
fn build_ip_socket_address(
    env: &mut JNIEnv,
    is_ipv4: jboolean,
    ipv4_octets: JByteArray,
    ipv6_segments: JObject,
    port: jint,
    flow_info: jint,
    scope_id: jint,
) -> Result<IpSocketAddress, String> {
    let ip = if is_ipv4 != 0 {
        // IPv4 address
        let octets = env
            .convert_byte_array(ipv4_octets)
            .map_err(|e| format!("Failed to read IPv4 octets: {}", e))?;
        if octets.len() != 4 {
            return Err("IPv4 address must have 4 octets".to_string());
        }
        let mut arr = [0u8; 4];
        arr.copy_from_slice(&octets);
        IpAddress::V4(arr)
    } else {
        // IPv6 address - segments is a short array
        let segments_array = JByteArray::from(ipv6_segments);
        let segment_bytes = env
            .convert_byte_array(segments_array)
            .map_err(|e| format!("Failed to read IPv6 segments: {}", e))?;
        if segment_bytes.len() != 16 {
            return Err("IPv6 address must have 16 bytes".to_string());
        }
        let mut segments = [0u16; 8];
        for i in 0..8 {
            segments[i] = u16::from_be_bytes([segment_bytes[i * 2], segment_bytes[i * 2 + 1]]);
        }
        IpAddress::V6(segments)
    };

    Ok(IpSocketAddress {
        ip,
        port: port as u16,
        flow_info: flow_info as u32,
        scope_id: scope_id as u32,
    })
}

// =============================================================================
// TCP Socket Functions
// =============================================================================

/// Create a new TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    is_ipv6: jboolean,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::tcp_socket_create(context, is_ipv6 != 0) {
        Ok(handle) => handle as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to create TCP socket: {}", e),
            );
            -1
        }
    }
}

/// Start binding a TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeStartBind(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    is_ipv4: jboolean,
    ipv4_octets: JByteArray,
    ipv6_segments: JObject,
    port: jint,
    flow_info: jint,
    scope_id: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    let addr = match build_ip_socket_address(
        &mut env,
        is_ipv4,
        ipv4_octets,
        ipv6_segments,
        port,
        flow_info,
        scope_id,
    ) {
        Ok(a) => a,
        Err(msg) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", msg);
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_start_bind(
        context,
        socket_handle as u64,
        &addr,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to start bind: {}", e),
        );
    }
}

/// Finish binding a TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeFinishBind(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) =
        wasi_sockets_helpers::tcp_socket_finish_bind(context, socket_handle as u64)
    {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to finish bind: {}", e),
        );
    }
}

/// Start connecting a TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeStartConnect(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    is_ipv4: jboolean,
    ipv4_octets: JByteArray,
    ipv6_segments: JObject,
    port: jint,
    flow_info: jint,
    scope_id: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    let addr = match build_ip_socket_address(
        &mut env,
        is_ipv4,
        ipv4_octets,
        ipv6_segments,
        port,
        flow_info,
        scope_id,
    ) {
        Ok(a) => a,
        Err(msg) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", msg);
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_start_connect(
        context,
        socket_handle as u64,
        &addr,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to start connect: {}", e),
        );
    }
}

/// Finish connecting a TCP socket
///
/// Returns a long array: [input_stream_handle, output_stream_handle]
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeFinishConnect(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::tcp_socket_finish_connect(context, socket_handle as u64) {
        Ok((input_handle, output_handle)) => {
            let result = env.new_long_array(2);
            if let Ok(arr) = result {
                let data = [input_handle as jlong, output_handle as jlong];
                let _ = env.set_long_array_region(&arr, 0, &data);
                arr.into_raw()
            } else {
                let _ = env.throw_new(
                    "java/lang/OutOfMemoryError",
                    "Failed to allocate result array",
                );
                JObject::null().into_raw()
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to finish connect: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// Start listening on a TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeStartListen(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) =
        wasi_sockets_helpers::tcp_socket_start_listen(context, socket_handle as u64)
    {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to start listen: {}", e),
        );
    }
}

/// Finish listening on a TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeFinishListen(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) =
        wasi_sockets_helpers::tcp_socket_finish_listen(context, socket_handle as u64)
    {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to finish listen: {}", e),
        );
    }
}

/// Accept a connection on a TCP socket
///
/// Returns a long array: [new_socket_handle, input_stream_handle, output_stream_handle]
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeAccept(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::tcp_socket_accept(context, socket_handle as u64) {
        Ok((new_socket, input_handle, output_handle)) => {
            let result = env.new_long_array(3);
            if let Ok(arr) = result {
                let data = [
                    new_socket as jlong,
                    input_handle as jlong,
                    output_handle as jlong,
                ];
                let _ = env.set_long_array_region(&arr, 0, &data);
                arr.into_raw()
            } else {
                let _ = env.throw_new(
                    "java/lang/OutOfMemoryError",
                    "Failed to allocate result array",
                );
                JObject::null().into_raw()
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to accept connection: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// Get local address of a TCP socket
///
/// Returns a long array encoding the address
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeLocalAddress(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::tcp_socket_local_address(context, socket_handle as u64) {
        Ok(addr) => encode_ip_socket_address(&mut env, &addr),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get local address: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// Get remote address of a TCP socket
///
/// Returns a long array encoding the address
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeRemoteAddress(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::tcp_socket_remote_address(context, socket_handle as u64) {
        Ok(addr) => encode_ip_socket_address(&mut env, &addr),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get remote address: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// Get address family of a TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeAddressFamily(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jboolean {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return 0;
        }
    };

    match wasi_sockets_helpers::tcp_socket_address_family(context, socket_handle as u64) {
        Ok(is_ipv6) => {
            if is_ipv6 {
                1
            } else {
                0
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get address family: {}", e),
            );
            0
        }
    }
}

/// Set listen backlog size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetListenBacklogSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_listen_backlog_size(
        context,
        socket_handle as u64,
        value as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set listen backlog size: {}", e),
        );
    }
}

/// Set keep-alive enabled
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetKeepAliveEnabled(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    enabled: jboolean,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_enabled(
        context,
        socket_handle as u64,
        enabled != 0,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set keep-alive enabled: {}", e),
        );
    }
}

/// Set keep-alive idle time
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetKeepAliveIdleTime(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    duration_nanos: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_idle_time(
        context,
        socket_handle as u64,
        duration_nanos as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set keep-alive idle time: {}", e),
        );
    }
}

/// Set keep-alive interval
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetKeepAliveInterval(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    duration_nanos: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_interval(
        context,
        socket_handle as u64,
        duration_nanos as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set keep-alive interval: {}", e),
        );
    }
}

/// Set keep-alive count
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetKeepAliveCount(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    count: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_count(
        context,
        socket_handle as u64,
        count as u32,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set keep-alive count: {}", e),
        );
    }
}

/// Set hop limit
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetHopLimit(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_hop_limit(
        context,
        socket_handle as u64,
        value as u8,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set hop limit: {}", e),
        );
    }
}

/// Get receive buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeReceiveBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::tcp_socket_receive_buffer_size(
        context,
        socket_handle as u64,
    ) {
        Ok(size) => size as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get receive buffer size: {}", e),
            );
            -1
        }
    }
}

/// Set receive buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetReceiveBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_receive_buffer_size(
        context,
        socket_handle as u64,
        value as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set receive buffer size: {}", e),
        );
    }
}

/// Get send buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSendBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::tcp_socket_send_buffer_size(context, socket_handle as u64)
    {
        Ok(size) => size as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get send buffer size: {}", e),
            );
            -1
        }
    }
}

/// Set send buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSetSendBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_send_buffer_size(
        context,
        socket_handle as u64,
        value as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set send buffer size: {}", e),
        );
    }
}

/// Subscribe to TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeSubscribe(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::tcp_socket_subscribe(context, socket_handle as u64) {
        Ok(handle) => handle as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to subscribe: {}", e),
            );
            -1
        }
    }
}

/// Shutdown TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeShutdown(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    shutdown_type: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_shutdown(
        context,
        socket_handle as u64,
        shutdown_type as u8,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to shutdown socket: {}", e),
        );
    }
}

/// Close TCP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiTcpSocket_nativeClose(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::tcp_socket_close(context, socket_handle as u64) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to close socket: {}", e),
        );
    }
}

// =============================================================================
// UDP Socket Functions
// =============================================================================

/// Create a new UDP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    is_ipv6: jboolean,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::udp_socket_create(context, is_ipv6 != 0) {
        Ok(handle) => handle as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to create UDP socket: {}", e),
            );
            -1
        }
    }
}

/// Start binding a UDP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeStartBind(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    is_ipv4: jboolean,
    ipv4_octets: JByteArray,
    ipv6_segments: JObject,
    port: jint,
    flow_info: jint,
    scope_id: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    let addr = match build_ip_socket_address(
        &mut env,
        is_ipv4,
        ipv4_octets,
        ipv6_segments,
        port,
        flow_info,
        scope_id,
    ) {
        Ok(a) => a,
        Err(msg) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", msg);
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::udp_socket_start_bind(
        context,
        socket_handle as u64,
        &addr,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to start bind: {}", e),
        );
    }
}

/// Finish binding a UDP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeFinishBind(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) =
        wasi_sockets_helpers::udp_socket_finish_bind(context, socket_handle as u64)
    {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to finish bind: {}", e),
        );
    }
}

/// Set remote address for UDP socket (stream mode)
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeStream(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    is_ipv4: jboolean,
    ipv4_octets: JByteArray,
    ipv6_segments: JObject,
    port: jint,
    flow_info: jint,
    scope_id: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    let addr = match build_ip_socket_address(
        &mut env,
        is_ipv4,
        ipv4_octets,
        ipv6_segments,
        port,
        flow_info,
        scope_id,
    ) {
        Ok(a) => a,
        Err(msg) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", msg);
            return;
        }
    };

    if let Err(e) =
        wasi_sockets_helpers::udp_socket_stream(context, socket_handle as u64, &addr)
    {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set remote address: {}", e),
        );
    }
}

/// Get local address of a UDP socket
///
/// Returns a long array encoding the address
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeLocalAddress(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::udp_socket_local_address(context, socket_handle as u64) {
        Ok(addr) => encode_ip_socket_address(&mut env, &addr),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get local address: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// Get remote address of a UDP socket
///
/// Returns a long array encoding the address
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeRemoteAddress(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::udp_socket_remote_address(context, socket_handle as u64) {
        Ok(addr) => encode_ip_socket_address(&mut env, &addr),
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get remote address: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// Get address family of a UDP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeAddressFamily(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jboolean {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return 0;
        }
    };

    match wasi_sockets_helpers::udp_socket_address_family(context, socket_handle as u64) {
        Ok(is_ipv6) => {
            if is_ipv6 {
                1
            } else {
                0
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get address family: {}", e),
            );
            0
        }
    }
}

/// Set unicast hop limit
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSetUnicastHopLimit(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jint,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::udp_socket_set_unicast_hop_limit(
        context,
        socket_handle as u64,
        value as u8,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set unicast hop limit: {}", e),
        );
    }
}

/// Get receive buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeReceiveBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::udp_socket_receive_buffer_size(
        context,
        socket_handle as u64,
    ) {
        Ok(size) => size as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get receive buffer size: {}", e),
            );
            -1
        }
    }
}

/// Set receive buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSetReceiveBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::udp_socket_set_receive_buffer_size(
        context,
        socket_handle as u64,
        value as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set receive buffer size: {}", e),
        );
    }
}

/// Get send buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSendBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::udp_socket_send_buffer_size(context, socket_handle as u64)
    {
        Ok(size) => size as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get send buffer size: {}", e),
            );
            -1
        }
    }
}

/// Set send buffer size
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSetSendBufferSize(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    value: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::udp_socket_set_send_buffer_size(
        context,
        socket_handle as u64,
        value as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to set send buffer size: {}", e),
        );
    }
}

/// Subscribe to UDP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSubscribe(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::udp_socket_subscribe(context, socket_handle as u64) {
        Ok(handle) => handle as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to subscribe: {}", e),
            );
            -1
        }
    }
}

/// Close UDP socket
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeClose(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::udp_socket_close(context, socket_handle as u64) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to close socket: {}", e),
        );
    }
}

/// Receive datagrams from UDP socket
///
/// Returns an encoded byte array:
/// - First 4 bytes: count (big-endian int)
/// - For each datagram:
///   - 4 bytes: data length
///   - N bytes: data
///   - 1 byte: isIpv4 (1 or 0)
///   - 2 bytes: port (big-endian)
///   - For IPv4: 4 bytes octets
///   - For IPv6: 4 bytes flowInfo + 4 bytes scopeId + 16 bytes (8 shorts)
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeReceive(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    max_results: jlong,
) -> jbyteArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return std::ptr::null_mut();
        }
    };

    match wasi_sockets_helpers::udp_socket_receive(
        context,
        socket_handle as u64,
        max_results as u64,
    ) {
        Ok(datagrams) => {
            // Calculate total size needed
            let count = datagrams.len();
            let mut total_size = 4; // count
            for (data, addr) in &datagrams {
                total_size += 4; // data length
                total_size += data.len(); // data
                total_size += 1; // isIpv4
                total_size += 2; // port
                match &addr.ip {
                    IpAddress::V4(_) => total_size += 4, // IPv4 octets
                    IpAddress::V6(_) => total_size += 4 + 4 + 16, // flowInfo + scopeId + segments
                }
            }

            // Create output buffer
            let mut buffer = vec![0u8; total_size];
            let mut offset = 0;

            // Write count
            buffer[offset] = ((count >> 24) & 0xFF) as u8;
            buffer[offset + 1] = ((count >> 16) & 0xFF) as u8;
            buffer[offset + 2] = ((count >> 8) & 0xFF) as u8;
            buffer[offset + 3] = (count & 0xFF) as u8;
            offset += 4;

            // Write each datagram
            for (data, addr) in &datagrams {
                // Write data length
                let data_len = data.len();
                buffer[offset] = ((data_len >> 24) & 0xFF) as u8;
                buffer[offset + 1] = ((data_len >> 16) & 0xFF) as u8;
                buffer[offset + 2] = ((data_len >> 8) & 0xFF) as u8;
                buffer[offset + 3] = (data_len & 0xFF) as u8;
                offset += 4;

                // Write data
                buffer[offset..offset + data_len].copy_from_slice(data);
                offset += data_len;

                // Write address info
                match &addr.ip {
                    IpAddress::V4(octets) => {
                        buffer[offset] = 1; // isIpv4
                        offset += 1;
                        buffer[offset] = ((addr.port >> 8) & 0xFF) as u8;
                        buffer[offset + 1] = (addr.port & 0xFF) as u8;
                        offset += 2;
                        buffer[offset..offset + 4].copy_from_slice(octets);
                        offset += 4;
                    }
                    IpAddress::V6(segments) => {
                        buffer[offset] = 0; // isIpv4
                        offset += 1;
                        buffer[offset] = ((addr.port >> 8) & 0xFF) as u8;
                        buffer[offset + 1] = (addr.port & 0xFF) as u8;
                        offset += 2;
                        // flowInfo
                        buffer[offset] = ((addr.flow_info >> 24) & 0xFF) as u8;
                        buffer[offset + 1] = ((addr.flow_info >> 16) & 0xFF) as u8;
                        buffer[offset + 2] = ((addr.flow_info >> 8) & 0xFF) as u8;
                        buffer[offset + 3] = (addr.flow_info & 0xFF) as u8;
                        offset += 4;
                        // scopeId
                        buffer[offset] = ((addr.scope_id >> 24) & 0xFF) as u8;
                        buffer[offset + 1] = ((addr.scope_id >> 16) & 0xFF) as u8;
                        buffer[offset + 2] = ((addr.scope_id >> 8) & 0xFF) as u8;
                        buffer[offset + 3] = (addr.scope_id & 0xFF) as u8;
                        offset += 4;
                        // segments
                        for seg in segments {
                            buffer[offset] = ((seg >> 8) & 0xFF) as u8;
                            buffer[offset + 1] = (seg & 0xFF) as u8;
                            offset += 2;
                        }
                    }
                }
            }

            // Create Java byte array
            match env.new_byte_array(buffer.len() as i32) {
                Ok(arr) => {
                    // Convert u8 to i8 for JNI
                    let i8_buffer: Vec<i8> = buffer.into_iter().map(|b| b as i8).collect();
                    let _ = env.set_byte_array_region(&arr, 0, &i8_buffer);
                    arr.into_raw()
                }
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to receive datagrams: {}", e),
            );
            std::ptr::null_mut()
        }
    }
}

/// Send datagrams on UDP socket
///
/// Takes an encoded byte array:
/// - First 4 bytes: count (big-endian int)
/// - For each datagram:
///   - 4 bytes: data length
///   - N bytes: data
///   - 1 byte: hasRemoteAddr
///   - If hasRemoteAddr:
///     - 1 byte: isIpv4
///     - 2 bytes: port (big-endian)
///     - For IPv4: 4 bytes octets
///     - For IPv6: 4 bytes flowInfo + 4 bytes scopeId + 16 bytes (8 shorts)
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiUdpSocket_nativeSend(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    socket_handle: jlong,
    encoded_datagrams: jbyteArray,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    // Get byte array
    let arr = unsafe { JByteArray::from_raw(encoded_datagrams) };
    let len = match env.get_array_length(&arr) {
        Ok(l) => l as usize,
        Err(_) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid byte array");
            return -1;
        }
    };

    let mut buffer = vec![0i8; len];
    if env.get_byte_array_region(&arr, 0, &mut buffer).is_err() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Failed to read byte array");
        return -1;
    }

    let buffer: Vec<u8> = buffer.into_iter().map(|b| b as u8).collect();
    let mut offset = 0;

    // Read count
    if buffer.len() < 4 {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer too small");
        return -1;
    }
    let count = ((buffer[0] as u32) << 24)
        | ((buffer[1] as u32) << 16)
        | ((buffer[2] as u32) << 8)
        | (buffer[3] as u32);
    offset += 4;

    // Parse datagrams
    let mut datagrams = Vec::new();
    for _ in 0..count {
        if offset + 4 > buffer.len() {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
            return -1;
        }

        // Read data length
        let data_len = ((buffer[offset] as u32) << 24)
            | ((buffer[offset + 1] as u32) << 16)
            | ((buffer[offset + 2] as u32) << 8)
            | (buffer[offset + 3] as u32);
        offset += 4;

        if offset + data_len as usize > buffer.len() {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
            return -1;
        }

        // Read data
        let data = buffer[offset..offset + data_len as usize].to_vec();
        offset += data_len as usize;

        // Read hasRemoteAddr
        if offset >= buffer.len() {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
            return -1;
        }
        let has_remote_addr = buffer[offset] != 0;
        offset += 1;

        let addr = if has_remote_addr {
            if offset >= buffer.len() {
                let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
                return -1;
            }
            let is_ipv4 = buffer[offset] != 0;
            offset += 1;

            if offset + 2 > buffer.len() {
                let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
                return -1;
            }
            let port = ((buffer[offset] as u16) << 8) | (buffer[offset + 1] as u16);
            offset += 2;

            if is_ipv4 {
                if offset + 4 > buffer.len() {
                    let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
                    return -1;
                }
                // The bounds check above guarantees this slice is exactly 4 bytes
                let octets: [u8; 4] = buffer[offset..offset + 4]
                    .try_into()
                    .expect("bounds check guarantees exactly 4 bytes");
                offset += 4;
                Some(IpSocketAddress {
                    ip: IpAddress::V4(octets),
                    port,
                    flow_info: 0,
                    scope_id: 0,
                })
            } else {
                if offset + 4 + 4 + 16 > buffer.len() {
                    let _ = env.throw_new("java/lang/IllegalArgumentException", "Buffer underflow");
                    return -1;
                }
                let flow_info = ((buffer[offset] as u32) << 24)
                    | ((buffer[offset + 1] as u32) << 16)
                    | ((buffer[offset + 2] as u32) << 8)
                    | (buffer[offset + 3] as u32);
                offset += 4;
                let scope_id = ((buffer[offset] as u32) << 24)
                    | ((buffer[offset + 1] as u32) << 16)
                    | ((buffer[offset + 2] as u32) << 8)
                    | (buffer[offset + 3] as u32);
                offset += 4;
                let mut segments = [0u16; 8];
                for seg in &mut segments {
                    *seg = ((buffer[offset] as u16) << 8) | (buffer[offset + 1] as u16);
                    offset += 2;
                }
                Some(IpSocketAddress {
                    ip: IpAddress::V6(segments),
                    port,
                    flow_info,
                    scope_id,
                })
            }
        } else {
            None
        };

        datagrams.push((data, addr));
    }

    match wasi_sockets_helpers::udp_socket_send(context, socket_handle as u64, &datagrams) {
        Ok(sent) => sent as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to send datagrams: {}", e),
            );
            -1
        }
    }
}

// =============================================================================
// Network Functions
// =============================================================================

/// Create a new network resource
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiNetwork_nativeCreate(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return -1;
        }
    };

    match wasi_sockets_helpers::network_create(context) {
        Ok(handle) => handle as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to create network: {}", e),
            );
            -1
        }
    }
}

/// Close a network resource
///
/// # Safety
/// This function is called from Java via JNI and must handle all edge cases safely.
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiNetwork_nativeClose(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    network_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
            return;
        }
    };

    if let Err(e) =
        wasi_sockets_helpers::network_close(context, network_handle as u64)
    {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to close network: {}", e),
        );
    }
}

// =============================================================================
// Helper Functions
// =============================================================================

/// Encode an IpSocketAddress as a long array for return to Java
///
/// Encoding format:
/// - IPv4: [is_ipv4=1, octet0, octet1, octet2, octet3, port, flow_info, scope_id]
/// - IPv6: [is_ipv4=0, seg0, seg1, ..., seg7, port, flow_info, scope_id] (length 12)
fn encode_ip_socket_address(env: &mut JNIEnv, addr: &IpSocketAddress) -> jlongArray {
    match &addr.ip {
        IpAddress::V4(octets) => {
            let result = env.new_long_array(8);
            if let Ok(arr) = result {
                let data = [
                    1, // is_ipv4
                    octets[0] as jlong,
                    octets[1] as jlong,
                    octets[2] as jlong,
                    octets[3] as jlong,
                    addr.port as jlong,
                    addr.flow_info as jlong,
                    addr.scope_id as jlong,
                ];
                let _ = env.set_long_array_region(&arr, 0, &data);
                arr.into_raw()
            } else {
                let _ = env.throw_new(
                    "java/lang/OutOfMemoryError",
                    "Failed to allocate address array",
                );
                JObject::null().into_raw()
            }
        }
        IpAddress::V6(segments) => {
            let result = env.new_long_array(12);
            if let Ok(arr) = result {
                let mut data = [0 as jlong; 12];
                data[0] = 0; // is_ipv4 = false
                for i in 0..8 {
                    data[i + 1] = segments[i] as jlong;
                }
                data[9] = addr.port as jlong;
                data[10] = addr.flow_info as jlong;
                data[11] = addr.scope_id as jlong;
                let _ = env.set_long_array_region(&arr, 0, &data);
                arr.into_raw()
            } else {
                let _ = env.throw_new(
                    "java/lang/OutOfMemoryError",
                    "Failed to allocate address array",
                );
                JObject::null().into_raw()
            }
        }
    }
}

// =============================================================================
// IP Name Lookup Functions
// =============================================================================

/// JNI: Resolve hostname to IP addresses
///
/// # Arguments
/// * `context_handle` - The WASI context handle
/// * `network_handle` - The network handle (validated but not used)
/// * `hostname` - The hostname to resolve
/// * `address_family` - Address family filter: 0 = all, 4 = IPv4 only, 6 = IPv6 only
///
/// # Returns
/// Stream handle on success (> 0), 0 on error
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniWasiIpNameLookup_nativeResolveAddresses(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    network_handle: jlong,
    hostname: JObject,
    address_family: jbyte,
) -> jlong {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new(
                "java/lang/IllegalStateException",
                "Invalid context handle",
            );
            return 0;
        }
    };

    // Convert Java string to Rust string
    let hostname_str = match env.get_string(&hostname.into()) {
        Ok(s) => s.to_string_lossy().into_owned(),
        Err(_) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                "Invalid hostname string",
            );
            return 0;
        }
    };

    match wasi_sockets_helpers::ip_name_lookup_resolve_addresses(
        context,
        network_handle as u64,
        &hostname_str,
        address_family as u8,
    ) {
        Ok(handle) => handle as jlong,
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to resolve addresses: {}", e),
            );
            0
        }
    }
}

/// JNI: Get the next address from a resolve address stream
///
/// # Arguments
/// * `context_handle` - The WASI context handle
/// * `stream_handle` - The stream handle from resolve_addresses
///
/// # Returns
/// int[14] array: [hasAddress, isIpv4, ipv4_b0-b3, ipv6_s0-s7] or null on error
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniResolveAddressStream_nativeGetNextAddress(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    stream_handle: jlong,
) -> jlongArray {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new(
                "java/lang/IllegalStateException",
                "Invalid context handle",
            );
            return JObject::null().into_raw();
        }
    };

    match wasi_sockets_helpers::resolve_address_stream_next(context, stream_handle as u64)
    {
        Ok((has_address, is_ipv4, ipv4_bytes, ipv6_segments)) => {
            // Create int array: [hasAddress, isIpv4, ipv4_b0-b3, ipv6_s0-s7]
            let result = env.new_int_array(14);
            match result {
                Ok(arr) => {
                    let mut data = [0i32; 14];
                    data[0] = if has_address { 1 } else { 0 };
                    data[1] = if is_ipv4 { 1 } else { 0 };
                    // IPv4 bytes
                    for i in 0..4 {
                        data[2 + i] = ipv4_bytes[i] as i32;
                    }
                    // IPv6 segments
                    for i in 0..8 {
                        data[6 + i] = ipv6_segments[i] as i32;
                    }
                    let _ = env.set_int_array_region(&arr, 0, &data);
                    arr.into_raw()
                }
                Err(_) => {
                    let _ = env.throw_new(
                        "java/lang/OutOfMemoryError",
                        "Failed to allocate result array",
                    );
                    JObject::null().into_raw()
                }
            }
        }
        Err(e) => {
            let _ = env.throw_new(
                "ai/tegmentum/wasmtime4j/exception/WasmException",
                format!("Failed to get next address: {}", e),
            );
            JObject::null().into_raw()
        }
    }
}

/// JNI: Subscribe to a resolve address stream
///
/// # Arguments
/// * `context_handle` - The WASI context handle
/// * `stream_handle` - The stream handle
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniResolveAddressStream_nativeSubscribe(
    mut env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    stream_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            let _ = env.throw_new(
                "java/lang/IllegalStateException",
                "Invalid context handle",
            );
            return;
        }
    };

    if let Err(e) = wasi_sockets_helpers::resolve_address_stream_subscribe(
        context,
        stream_handle as u64,
    ) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to subscribe: {}", e),
        );
    }
}

/// JNI: Check if a resolve address stream is closed
///
/// # Arguments
/// * `context_handle` - The WASI context handle
/// * `stream_handle` - The stream handle
///
/// # Returns
/// true if closed, false if open
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniResolveAddressStream_nativeIsClosed(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    stream_handle: jlong,
) -> jboolean {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => {
            // Return true (closed) if context is invalid
            return 1;
        }
    };

    match wasi_sockets_helpers::resolve_address_stream_is_closed(
        context,
        stream_handle as u64,
    ) {
        Ok(closed) => {
            if closed {
                1
            } else {
                0
            }
        }
        Err(_) => 1, // Error means closed
    }
}

/// JNI: Close a resolve address stream
///
/// # Arguments
/// * `context_handle` - The WASI context handle
/// * `stream_handle` - The stream handle to close
#[no_mangle]
pub extern "system" fn Java_ai_tegmentum_wasmtime4j_jni_wasi_sockets_JniResolveAddressStream_nativeClose(
    _env: JNIEnv,
    _class: JClass,
    context_handle: jlong,
    stream_handle: jlong,
) {
    let context = match unsafe { get_context(context_handle) } {
        Some(ctx) => ctx,
        None => return,
    };

    let _ =
        wasi_sockets_helpers::resolve_address_stream_close(context, stream_handle as u64);
}
