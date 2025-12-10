//! Panama FFI bindings for WASI Preview 2 sockets operations
//!
//! This module provides C-compatible FFI functions for use by the wasmtime4j-panama module
//! to access WASI Preview 2 sockets operations (wasi:sockets).
//!
//! All functions use C calling conventions and handle memory management appropriately.

use std::os::raw::{c_int, c_longlong, c_uchar, c_uint, c_ushort, c_void};

use crate::wasi_preview2::WasiPreview2Context;
use crate::wasi_sockets_helpers::{self, IpAddress, IpSocketAddress};

// Helper function to get context from handle
unsafe fn get_context<'a>(context_handle: *mut c_void) -> Option<&'a WasiPreview2Context> {
    if context_handle.is_null() {
        return None;
    }
    let ptr = context_handle as *const WasiPreview2Context;
    if ptr.is_null() {
        return None;
    }
    Some(&*ptr)
}

// Helper function to build IpSocketAddress from C parameters
unsafe fn build_ip_socket_address_from_c(
    is_ipv4: c_int,
    ipv4_octets: *const c_uchar,
    ipv6_segments: *const c_ushort,
    port: c_ushort,
    flow_info: c_uint,
    scope_id: c_uint,
) -> Option<IpSocketAddress> {
    let ip = if is_ipv4 != 0 {
        if ipv4_octets.is_null() {
            return None;
        }
        let mut octets = [0u8; 4];
        for i in 0..4 {
            octets[i] = *ipv4_octets.add(i);
        }
        IpAddress::V4(octets)
    } else {
        if ipv6_segments.is_null() {
            return None;
        }
        let mut segments = [0u16; 8];
        for i in 0..8 {
            segments[i] = *ipv6_segments.add(i);
        }
        IpAddress::V6(segments)
    };

    Some(IpSocketAddress {
        ip,
        port,
        flow_info,
        scope_id,
    })
}

// Helper function to encode IpSocketAddress to C parameters
unsafe fn encode_ip_socket_address_to_c(
    addr: &IpSocketAddress,
    out_is_ipv4: *mut c_int,
    out_ipv4_octets: *mut c_uchar,
    out_ipv6_segments: *mut c_ushort,
    out_port: *mut c_ushort,
    out_flow_info: *mut c_uint,
    out_scope_id: *mut c_uint,
) {
    match &addr.ip {
        IpAddress::V4(octets) => {
            *out_is_ipv4 = 1;
            for i in 0..4 {
                *out_ipv4_octets.add(i) = octets[i];
            }
        }
        IpAddress::V6(segments) => {
            *out_is_ipv4 = 0;
            for i in 0..8 {
                *out_ipv6_segments.add(i) = segments[i];
            }
        }
    }
    *out_port = addr.port;
    *out_flow_info = addr.flow_info;
    *out_scope_id = addr.scope_id;
}

// =============================================================================
// TCP Socket Functions
// =============================================================================

/// Create a new TCP socket
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `is_ipv6`: 1 for IPv6, 0 for IPv4
/// - `out_handle`: Pointer to write the socket handle
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_create(
    context_handle: *mut c_void,
    is_ipv6: c_int,
    out_handle: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_handle.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_create(context.unwrap(), is_ipv6 != 0) {
        Ok(handle) => {
            unsafe {
                *out_handle = handle as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Start binding a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_start_bind(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    is_ipv4: c_int,
    ipv4_octets: *const c_uchar,
    ipv6_segments: *const c_ushort,
    port: c_ushort,
    flow_info: c_uint,
    scope_id: c_uint,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    let addr = unsafe {
        build_ip_socket_address_from_c(is_ipv4, ipv4_octets, ipv6_segments, port, flow_info, scope_id)
    };
    if addr.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_start_bind(
        context.unwrap(),
        socket_handle as u64,
        &addr.unwrap(),
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Finish binding a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_finish_bind(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_finish_bind(context.unwrap(), socket_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Start connecting a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_start_connect(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    is_ipv4: c_int,
    ipv4_octets: *const c_uchar,
    ipv6_segments: *const c_ushort,
    port: c_ushort,
    flow_info: c_uint,
    scope_id: c_uint,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    let addr = unsafe {
        build_ip_socket_address_from_c(is_ipv4, ipv4_octets, ipv6_segments, port, flow_info, scope_id)
    };
    if addr.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_start_connect(
        context.unwrap(),
        socket_handle as u64,
        &addr.unwrap(),
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Finish connecting a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_finish_connect(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_input_stream: *mut c_longlong,
    out_output_stream: *mut c_longlong,
) -> c_int {
    if out_input_stream.is_null() || out_output_stream.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_finish_connect(context.unwrap(), socket_handle as u64) {
        Ok((input, output)) => {
            unsafe {
                *out_input_stream = input as c_longlong;
                *out_output_stream = output as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Start listening on a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_start_listen(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_start_listen(context.unwrap(), socket_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Finish listening on a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_finish_listen(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_finish_listen(context.unwrap(), socket_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Accept a connection on a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_accept(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_new_socket: *mut c_longlong,
    out_input_stream: *mut c_longlong,
    out_output_stream: *mut c_longlong,
) -> c_int {
    if out_new_socket.is_null() || out_input_stream.is_null() || out_output_stream.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_accept(context.unwrap(), socket_handle as u64) {
        Ok((new_socket, input, output)) => {
            unsafe {
                *out_new_socket = new_socket as c_longlong;
                *out_input_stream = input as c_longlong;
                *out_output_stream = output as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get local address of a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_local_address(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_is_ipv4: *mut c_int,
    out_ipv4_octets: *mut c_uchar,
    out_ipv6_segments: *mut c_ushort,
    out_port: *mut c_ushort,
    out_flow_info: *mut c_uint,
    out_scope_id: *mut c_uint,
) -> c_int {
    if out_is_ipv4.is_null() || out_port.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_local_address(context.unwrap(), socket_handle as u64) {
        Ok(addr) => {
            unsafe {
                encode_ip_socket_address_to_c(
                    &addr,
                    out_is_ipv4,
                    out_ipv4_octets,
                    out_ipv6_segments,
                    out_port,
                    out_flow_info,
                    out_scope_id,
                );
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get remote address of a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_remote_address(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_is_ipv4: *mut c_int,
    out_ipv4_octets: *mut c_uchar,
    out_ipv6_segments: *mut c_ushort,
    out_port: *mut c_ushort,
    out_flow_info: *mut c_uint,
    out_scope_id: *mut c_uint,
) -> c_int {
    if out_is_ipv4.is_null() || out_port.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_remote_address(context.unwrap(), socket_handle as u64) {
        Ok(addr) => {
            unsafe {
                encode_ip_socket_address_to_c(
                    &addr,
                    out_is_ipv4,
                    out_ipv4_octets,
                    out_ipv6_segments,
                    out_port,
                    out_flow_info,
                    out_scope_id,
                );
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get address family of a TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_address_family(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_is_ipv6: *mut c_int,
) -> c_int {
    if out_is_ipv6.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_address_family(context.unwrap(), socket_handle as u64) {
        Ok(is_ipv6) => {
            unsafe {
                *out_is_ipv6 = if is_ipv6 { 1 } else { 0 };
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set listen backlog size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_listen_backlog_size(
        context.unwrap(),
        socket_handle as u64,
        value as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Set keep-alive enabled
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    enabled: c_int,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_keep_alive_enabled(
        context.unwrap(),
        socket_handle as u64,
        enabled != 0,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Set keep-alive idle time
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    duration_nanos: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_keep_alive_idle_time(
        context.unwrap(),
        socket_handle as u64,
        duration_nanos as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Set keep-alive interval
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    duration_nanos: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_keep_alive_interval(
        context.unwrap(),
        socket_handle as u64,
        duration_nanos as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Set keep-alive count
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    count: c_uint,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_keep_alive_count(
        context.unwrap(),
        socket_handle as u64,
        count,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Set hop limit
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_hop_limit(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_uchar,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_hop_limit(
        context.unwrap(),
        socket_handle as u64,
        value,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get receive buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_size: *mut c_longlong,
) -> c_int {
    if out_size.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_receive_buffer_size(
        context.unwrap(),
        socket_handle as u64,
    ) {
        Ok(size) => {
            unsafe {
                *out_size = size as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set receive buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_receive_buffer_size(
        context.unwrap(),
        socket_handle as u64,
        value as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get send buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_send_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_size: *mut c_longlong,
) -> c_int {
    if out_size.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_send_buffer_size(context.unwrap(), socket_handle as u64)
    {
        Ok(size) => {
            unsafe {
                *out_size = size as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set send buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_set_send_buffer_size(
        context.unwrap(),
        socket_handle as u64,
        value as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Subscribe to TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_subscribe(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_pollable: *mut c_longlong,
) -> c_int {
    if out_pollable.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_subscribe(context.unwrap(), socket_handle as u64) {
        Ok(handle) => {
            unsafe {
                *out_pollable = handle as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Shutdown TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_shutdown(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    shutdown_type: c_uchar,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_shutdown(
        context.unwrap(),
        socket_handle as u64,
        shutdown_type,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Close TCP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_tcp_socket_close(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::tcp_socket_close(context.unwrap(), socket_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

// =============================================================================
// UDP Socket Functions
// =============================================================================

/// Create a new UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_create(
    context_handle: *mut c_void,
    is_ipv6: c_int,
    out_handle: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_handle.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_create(context.unwrap(), is_ipv6 != 0) {
        Ok(handle) => {
            unsafe {
                *out_handle = handle as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Start binding a UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_start_bind(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    is_ipv4: c_int,
    ipv4_octets: *const c_uchar,
    ipv6_segments: *const c_ushort,
    port: c_ushort,
    flow_info: c_uint,
    scope_id: c_uint,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    let addr = unsafe {
        build_ip_socket_address_from_c(is_ipv4, ipv4_octets, ipv6_segments, port, flow_info, scope_id)
    };
    if addr.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_start_bind(
        context.unwrap(),
        socket_handle as u64,
        &addr.unwrap(),
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Finish binding a UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_finish_bind(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_finish_bind(context.unwrap(), socket_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Set remote address for UDP socket (stream mode)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_stream(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    is_ipv4: c_int,
    ipv4_octets: *const c_uchar,
    ipv6_segments: *const c_ushort,
    port: c_ushort,
    flow_info: c_uint,
    scope_id: c_uint,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    let addr = unsafe {
        build_ip_socket_address_from_c(is_ipv4, ipv4_octets, ipv6_segments, port, flow_info, scope_id)
    };
    if addr.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_stream(
        context.unwrap(),
        socket_handle as u64,
        &addr.unwrap(),
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get local address of a UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_local_address(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_is_ipv4: *mut c_int,
    out_ipv4_octets: *mut c_uchar,
    out_ipv6_segments: *mut c_ushort,
    out_port: *mut c_ushort,
    out_flow_info: *mut c_uint,
    out_scope_id: *mut c_uint,
) -> c_int {
    if out_is_ipv4.is_null() || out_port.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_local_address(context.unwrap(), socket_handle as u64) {
        Ok(addr) => {
            unsafe {
                encode_ip_socket_address_to_c(
                    &addr,
                    out_is_ipv4,
                    out_ipv4_octets,
                    out_ipv6_segments,
                    out_port,
                    out_flow_info,
                    out_scope_id,
                );
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get remote address of a UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_remote_address(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_is_ipv4: *mut c_int,
    out_ipv4_octets: *mut c_uchar,
    out_ipv6_segments: *mut c_ushort,
    out_port: *mut c_ushort,
    out_flow_info: *mut c_uint,
    out_scope_id: *mut c_uint,
) -> c_int {
    if out_is_ipv4.is_null() || out_port.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_remote_address(context.unwrap(), socket_handle as u64) {
        Ok(addr) => {
            unsafe {
                encode_ip_socket_address_to_c(
                    &addr,
                    out_is_ipv4,
                    out_ipv4_octets,
                    out_ipv6_segments,
                    out_port,
                    out_flow_info,
                    out_scope_id,
                );
            }
            0
        }
        Err(_) => -1,
    }
}

/// Get address family of a UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_address_family(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_is_ipv6: *mut c_int,
) -> c_int {
    if out_is_ipv6.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_address_family(context.unwrap(), socket_handle as u64) {
        Ok(is_ipv6) => {
            unsafe {
                *out_is_ipv6 = if is_ipv6 { 1 } else { 0 };
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set unicast hop limit
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_uchar,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_set_unicast_hop_limit(
        context.unwrap(),
        socket_handle as u64,
        value,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get receive buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_receive_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_size: *mut c_longlong,
) -> c_int {
    if out_size.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_receive_buffer_size(
        context.unwrap(),
        socket_handle as u64,
    ) {
        Ok(size) => {
            unsafe {
                *out_size = size as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set receive buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_set_receive_buffer_size(
        context.unwrap(),
        socket_handle as u64,
        value as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Get send buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_send_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_size: *mut c_longlong,
) -> c_int {
    if out_size.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_send_buffer_size(context.unwrap(), socket_handle as u64)
    {
        Ok(size) => {
            unsafe {
                *out_size = size as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Set send buffer size
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    value: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_set_send_buffer_size(
        context.unwrap(),
        socket_handle as u64,
        value as u64,
    ) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Subscribe to UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_subscribe(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    out_pollable: *mut c_longlong,
) -> c_int {
    if out_pollable.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_subscribe(context.unwrap(), socket_handle as u64) {
        Ok(handle) => {
            unsafe {
                *out_pollable = handle as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Close UDP socket
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_close(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_close(context.unwrap(), socket_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Receive UDP datagrams
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `socket_handle`: The UDP socket handle
/// - `max_results`: Maximum number of datagrams to receive
/// - `out_count`: Pointer to write the number of datagrams received
/// - `out_datagrams_data`: Array of pointers to write datagram data (caller allocates)
/// - `out_datagrams_len`: Array to write datagram data lengths
/// - `out_is_ipv4`: Array to write address family flags (1=IPv4, 0=IPv6)
/// - `out_ipv4_octets`: Array to write IPv4 octets (4 bytes per datagram)
/// - `out_ipv6_segments`: Array to write IPv6 segments (16 bytes per datagram)
/// - `out_ports`: Array to write port numbers
/// - `out_flow_info`: Array to write flow info (IPv6 only)
/// - `out_scope_id`: Array to write scope IDs (IPv6 only)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_receive(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    max_results: c_longlong,
    out_count: *mut c_longlong,
    out_datagrams_data: *mut *mut c_uchar,
    out_datagrams_len: *mut c_longlong,
    out_is_ipv4: *mut c_int,
    out_ipv4_octets: *mut c_uchar,
    out_ipv6_segments: *mut c_ushort,
    out_ports: *mut c_ushort,
    out_flow_info: *mut c_uint,
    out_scope_id: *mut c_uint,
) -> c_int {
    if out_count.is_null() || out_datagrams_data.is_null() || out_datagrams_len.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::udp_socket_receive(
        context.unwrap(),
        socket_handle as u64,
        max_results as u64,
    ) {
        Ok(datagrams) => {
            let count = datagrams.len();
            unsafe {
                *out_count = count as c_longlong;

                for (i, (data, addr)) in datagrams.iter().enumerate() {
                    // Write datagram data length
                    *out_datagrams_len.add(i) = data.len() as c_longlong;

                    // Copy datagram data to caller-allocated buffer
                    let data_ptr = *out_datagrams_data.add(i);
                    if !data_ptr.is_null() {
                        std::ptr::copy_nonoverlapping(data.as_ptr(), data_ptr, data.len());
                    }

                    // Encode address
                    encode_ip_socket_address_to_c(
                        addr,
                        out_is_ipv4.add(i),
                        out_ipv4_octets.add(i * 4),
                        out_ipv6_segments.add(i * 8),
                        out_ports.add(i),
                        out_flow_info.add(i),
                        out_scope_id.add(i),
                    );
                }
            }
            0
        }
        Err(_) => -1,
    }
}

/// Send UDP datagrams
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `socket_handle`: The UDP socket handle
/// - `datagram_count`: Number of datagrams to send
/// - `datagram_data`: Array of pointers to datagram data
/// - `datagram_lengths`: Array of datagram data lengths
/// - `has_remote_address`: Array of flags indicating if remote address is provided
/// - `is_ipv4`: Array of address family flags (1=IPv4, 0=IPv6)
/// - `ipv4_octets`: Array of IPv4 octets (4 bytes per datagram)
/// - `ipv6_segments`: Array of IPv6 segments (16 bytes per datagram)
/// - `ports`: Array of port numbers
/// - `flow_info`: Array of flow info (IPv6 only)
/// - `scope_id`: Array of scope IDs (IPv6 only)
/// - `out_sent_count`: Pointer to write the number of datagrams sent
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_udp_socket_send(
    context_handle: *mut c_void,
    socket_handle: c_longlong,
    datagram_count: c_longlong,
    datagram_data: *const *const c_uchar,
    datagram_lengths: *const c_longlong,
    has_remote_address: *const c_int,
    is_ipv4: *const c_int,
    ipv4_octets: *const c_uchar,
    ipv6_segments: *const c_ushort,
    ports: *const c_ushort,
    flow_info: *const c_uint,
    scope_id: *const c_uint,
    out_sent_count: *mut c_longlong,
) -> c_int {
    if out_sent_count.is_null()
        || datagram_data.is_null()
        || datagram_lengths.is_null()
        || has_remote_address.is_null()
    {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    // Build datagrams vector
    let mut datagrams = Vec::new();
    unsafe {
        for i in 0..datagram_count as usize {
            // Get data
            let data_ptr = *datagram_data.add(i);
            let data_len = *datagram_lengths.add(i) as usize;
            if data_ptr.is_null() {
                return -1;
            }
            let data = std::slice::from_raw_parts(data_ptr, data_len).to_vec();

            // Get optional remote address
            let addr = if *has_remote_address.add(i) != 0 {
                build_ip_socket_address_from_c(
                    *is_ipv4.add(i),
                    ipv4_octets.add(i * 4),
                    ipv6_segments.add(i * 8),
                    *ports.add(i),
                    *flow_info.add(i),
                    *scope_id.add(i),
                )
            } else {
                None
            };

            datagrams.push((data, addr));
        }
    }

    match wasi_sockets_helpers::udp_socket_send(context.unwrap(), socket_handle as u64, &datagrams)
    {
        Ok(count) => {
            unsafe {
                *out_sent_count = count as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

// =============================================================================
// Network Functions
// =============================================================================

/// Create a new network resource
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_network_create(
    context_handle: *mut c_void,
    out_handle: *mut c_longlong,
) -> c_int {
    if context_handle.is_null() || out_handle.is_null() {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::network_create(context.unwrap()) {
        Ok(handle) => {
            unsafe {
                *out_handle = handle as c_longlong;
            }
            0
        }
        Err(_) => -1,
    }
}

/// Close a network resource
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_network_close(
    context_handle: *mut c_void,
    network_handle: c_longlong,
) -> c_int {
    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::network_close(context.unwrap(), network_handle as u64) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

// =============================================================================
// IP Name Lookup Functions
// =============================================================================

/// Resolve hostname to IP addresses
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `network_handle`: The network handle (validated but not used)
/// - `hostname`: Pointer to the hostname string
/// - `hostname_len`: Length of the hostname string
/// - `address_family`: Address family filter: 0 = all, 4 = IPv4 only, 6 = IPv6 only
///
/// # Returns
/// Stream handle on success (> 0), 0 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_resolve_addresses(
    context_handle: *mut c_void,
    network_handle: c_longlong,
    hostname: *const c_uchar,
    hostname_len: c_longlong,
    address_family: c_uchar,
) -> c_longlong {
    if context_handle.is_null() || hostname.is_null() || hostname_len <= 0 {
        return 0;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return 0;
    }

    // Convert hostname to string
    let hostname_str = unsafe {
        let slice = std::slice::from_raw_parts(hostname, hostname_len as usize);
        match std::str::from_utf8(slice) {
            Ok(s) => s,
            Err(_) => return 0,
        }
    };

    match wasi_sockets_helpers::ip_name_lookup_resolve_addresses(
        context.unwrap(),
        network_handle as u64,
        hostname_str,
        address_family,
    ) {
        Ok(handle) => handle as c_longlong,
        Err(_) => 0,
    }
}

/// Get the next address from a resolve address stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: The stream handle from resolve_addresses
/// - `out_result`: Pointer to int[14] array to receive:
///   [0] = has_address (1 if address returned, 0 if exhausted)
///   [1] = is_ipv4 (1 for IPv4, 0 for IPv6)
///   [2-5] = IPv4 bytes (if is_ipv4)
///   [6-13] = IPv6 segments (if !is_ipv4)
///
/// # Returns
/// 0 on success, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_resolve_stream_next(
    context_handle: *mut c_void,
    stream_handle: c_longlong,
    out_result: *mut c_int,
) -> c_int {
    if context_handle.is_null() || out_result.is_null() || stream_handle <= 0 {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::resolve_address_stream_next(context.unwrap(), stream_handle as u64)
    {
        Ok((has_address, is_ipv4, ipv4_bytes, ipv6_segments)) => {
            unsafe {
                // [0] = has_address
                *out_result = if has_address { 1 } else { 0 };
                // [1] = is_ipv4
                *out_result.add(1) = if is_ipv4 { 1 } else { 0 };

                // [2-5] = IPv4 bytes
                for i in 0..4 {
                    *out_result.add(2 + i) = ipv4_bytes[i] as c_int;
                }

                // [6-13] = IPv6 segments
                for i in 0..8 {
                    *out_result.add(6 + i) = ipv6_segments[i] as c_int;
                }
            }
            0
        }
        Err(_) => -1,
    }
}

/// Subscribe to a resolve address stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: The stream handle
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_resolve_stream_subscribe(
    context_handle: *mut c_void,
    stream_handle: c_longlong,
) {
    if context_handle.is_null() || stream_handle <= 0 {
        return;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return;
    }

    let _ = wasi_sockets_helpers::resolve_address_stream_subscribe(
        context.unwrap(),
        stream_handle as u64,
    );
}

/// Check if a resolve address stream is closed
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: The stream handle
///
/// # Returns
/// 1 if closed, 0 if open, -1 on error
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_resolve_stream_is_closed(
    context_handle: *mut c_void,
    stream_handle: c_longlong,
) -> c_int {
    if context_handle.is_null() || stream_handle <= 0 {
        return -1;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return -1;
    }

    match wasi_sockets_helpers::resolve_address_stream_is_closed(
        context.unwrap(),
        stream_handle as u64,
    ) {
        Ok(closed) => {
            if closed {
                1
            } else {
                0
            }
        }
        Err(_) => -1,
    }
}

/// Close a resolve address stream
///
/// # Parameters
/// - `context_handle`: Pointer to the WASI context
/// - `stream_handle`: The stream handle to close
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_resolve_stream_close(
    context_handle: *mut c_void,
    stream_handle: c_longlong,
) {
    if context_handle.is_null() || stream_handle <= 0 {
        return;
    }

    let context = unsafe { get_context(context_handle) };
    if context.is_none() {
        return;
    }

    let _ =
        wasi_sockets_helpers::resolve_address_stream_close(context.unwrap(), stream_handle as u64);
}
