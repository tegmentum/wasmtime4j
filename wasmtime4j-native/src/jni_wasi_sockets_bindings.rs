//! JNI bindings for WASI Preview 2 sockets operations
//!
//! This module provides JNI functions for wasi:sockets interfaces including
//! TCP sockets, UDP sockets, and network operations.

use jni::objects::{JByteArray, JClass, JObject, JObjectArray};
use jni::sys::{jboolean, jbyte, jint, jlong, jlongArray, jshort};
use jni::JNIEnv;

use crate::error::{WasmtimeError, WasmtimeResult};
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_create(context.unwrap(), is_ipv6 != 0) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

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
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) =
        wasi_sockets_helpers::tcp_socket_finish_bind(context.unwrap(), socket_handle as u64)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

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
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    match wasi_sockets_helpers::tcp_socket_finish_connect(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) =
        wasi_sockets_helpers::tcp_socket_start_listen(context.unwrap(), socket_handle as u64)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) =
        wasi_sockets_helpers::tcp_socket_finish_listen(context.unwrap(), socket_handle as u64)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    match wasi_sockets_helpers::tcp_socket_accept(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    match wasi_sockets_helpers::tcp_socket_local_address(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    match wasi_sockets_helpers::tcp_socket_remote_address(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return 0;
    }

    match wasi_sockets_helpers::tcp_socket_address_family(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_listen_backlog_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_enabled(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_idle_time(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_interval(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_keep_alive_count(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_hop_limit(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_receive_buffer_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_receive_buffer_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_send_buffer_size(context.unwrap(), socket_handle as u64)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_set_send_buffer_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_subscribe(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_shutdown(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::tcp_socket_close(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_create(context.unwrap(), is_ipv6 != 0) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

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
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) =
        wasi_sockets_helpers::udp_socket_finish_bind(context.unwrap(), socket_handle as u64)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

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
        wasi_sockets_helpers::udp_socket_stream(context.unwrap(), socket_handle as u64, &addr)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    match wasi_sockets_helpers::udp_socket_local_address(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return JObject::null().into_raw();
    }

    match wasi_sockets_helpers::udp_socket_remote_address(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return 0;
    }

    match wasi_sockets_helpers::udp_socket_address_family(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::udp_socket_set_unicast_hop_limit(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_receive_buffer_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::udp_socket_set_receive_buffer_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_send_buffer_size(context.unwrap(), socket_handle as u64)
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::udp_socket_set_send_buffer_size(
        context.unwrap(),
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_subscribe(context.unwrap(), socket_handle as u64) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) = wasi_sockets_helpers::udp_socket_close(context.unwrap(), socket_handle as u64) {
        let _ = env.throw_new(
            "ai/tegmentum/wasmtime4j/exception/WasmException",
            format!("Failed to close socket: {}", e),
        );
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return -1;
    }

    match wasi_sockets_helpers::network_create(context.unwrap()) {
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
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        let _ = env.throw_new("java/lang/IllegalArgumentException", "Invalid context handle");
        return;
    }

    if let Err(e) =
        wasi_sockets_helpers::network_close(context.unwrap(), network_handle as u64)
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
