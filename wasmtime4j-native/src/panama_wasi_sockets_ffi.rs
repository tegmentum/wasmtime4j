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
