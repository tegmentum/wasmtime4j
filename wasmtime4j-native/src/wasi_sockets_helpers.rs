//! WASI Socket Helper Functions for JNI/Panama FFI
//!
//! This module provides socket operations that can be called from Java via JNI or Panama FFI.
//! These are standalone socket operations for Java-level networking, independent of WebAssembly
//! module execution.
//!
//! For WASI components, socket support is provided automatically by wasmtime-wasi through the
//! WasiCtxBuilder configuration (inherit_network, allow_tcp, allow_udp, allow_ip_name_lookup).
//! See wasi_preview2.rs for component-level socket configuration.
//!
//! The socket helpers here use Rust stdlib sockets to provide direct networking capabilities
//! to Java applications without requiring a WebAssembly component.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::WasiPreview2Context;
use std::collections::HashMap;
use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, TcpListener, TcpStream, ToSocketAddrs, UdpSocket};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use lazy_static::lazy_static;

// Global socket state management for Java-side socket operations
// Uses Rust stdlib sockets for direct networking from Java via JNI/Panama FFI
lazy_static! {
    static ref UDP_SOCKETS: Arc<Mutex<HashMap<u64, Arc<Mutex<UdpSocket>>>>> =
        Arc::new(Mutex::new(HashMap::new()));
    static ref UDP_SOCKET_STATE: Arc<Mutex<HashMap<u64, UdpSocketInfo>>> =
        Arc::new(Mutex::new(HashMap::new()));
    static ref TCP_STREAMS: Arc<Mutex<HashMap<u64, Arc<Mutex<TcpStream>>>>> =
        Arc::new(Mutex::new(HashMap::new()));
    static ref TCP_LISTENERS: Arc<Mutex<HashMap<u64, Arc<Mutex<TcpListener>>>>> =
        Arc::new(Mutex::new(HashMap::new()));
    static ref TCP_SOCKET_STATE: Arc<Mutex<HashMap<u64, TcpSocketInfo>>> =
        Arc::new(Mutex::new(HashMap::new()));
}

// Atomic counter for generating unique socket handles
static NEXT_SOCKET_HANDLE: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(10000);

fn next_handle() -> u64 {
    NEXT_SOCKET_HANDLE.fetch_add(1, std::sync::atomic::Ordering::SeqCst)
}

/// TCP socket state tracking
#[derive(Debug, Clone)]
pub struct TcpSocketInfo {
    pub is_ipv6: bool,
    pub bound_addr: Option<SocketAddr>,
    pub remote_addr: Option<SocketAddr>,
    pub state: TcpSocketStateEnum,
    pub listener_handle: Option<u64>,
    pub stream_handle: Option<u64>,
    pub backlog: u32,
    pub keep_alive_enabled: bool,
    pub keep_alive_idle_time_ns: u64,
    pub keep_alive_interval_ns: u64,
    pub keep_alive_count: u32,
    pub hop_limit: u8,
    pub receive_buffer_size: u64,
    pub send_buffer_size: u64,
}

/// TCP socket state enum
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum TcpSocketStateEnum {
    Created,
    Binding,
    Bound,
    Connecting,
    Connected,
    Listening,
    Closed,
}

impl Default for TcpSocketInfo {
    fn default() -> Self {
        TcpSocketInfo {
            is_ipv6: false,
            bound_addr: None,
            remote_addr: None,
            state: TcpSocketStateEnum::Created,
            listener_handle: None,
            stream_handle: None,
            backlog: 128,
            keep_alive_enabled: false,
            keep_alive_idle_time_ns: 7200_000_000_000, // 2 hours in nanoseconds
            keep_alive_interval_ns: 75_000_000_000,    // 75 seconds in nanoseconds
            keep_alive_count: 9,
            hop_limit: 64,
            receive_buffer_size: 65536,
            send_buffer_size: 65536,
        }
    }
}

/// UDP socket state tracking
#[derive(Debug, Clone)]
pub struct UdpSocketInfo {
    pub is_ipv6: bool,
    pub bound_addr: Option<SocketAddr>,
    pub remote_addr: Option<SocketAddr>,
    pub state: UdpSocketStateEnum,
    pub unicast_hop_limit: u8,
    pub receive_buffer_size: u64,
    pub send_buffer_size: u64,
}

/// UDP socket state enum
#[derive(Debug, Clone, Copy, PartialEq)]
pub enum UdpSocketStateEnum {
    Created,
    Binding,
    Bound,
    Connected,
    Closed,
}

impl Default for UdpSocketInfo {
    fn default() -> Self {
        UdpSocketInfo {
            is_ipv6: false,
            bound_addr: None,
            remote_addr: None,
            state: UdpSocketStateEnum::Created,
            unicast_hop_limit: 64,
            receive_buffer_size: 65536,
            send_buffer_size: 65536,
        }
    }
}

/// IP address variants
#[derive(Debug, Clone)]
pub enum IpAddress {
    V4([u8; 4]),
    V6([u16; 8]),
}

/// Socket address with IP and port
#[derive(Debug, Clone)]
pub struct IpSocketAddress {
    pub ip: IpAddress,
    pub port: u16,
    pub flow_info: u32,  // IPv6 only
    pub scope_id: u32,   // IPv6 only
}

impl IpSocketAddress {
    pub fn to_socket_addr(&self) -> SocketAddr {
        match &self.ip {
            IpAddress::V4(octets) => {
                let ip = Ipv4Addr::new(octets[0], octets[1], octets[2], octets[3]);
                SocketAddr::new(IpAddr::V4(ip), self.port)
            }
            IpAddress::V6(segments) => {
                let ip = Ipv6Addr::new(
                    segments[0], segments[1], segments[2], segments[3],
                    segments[4], segments[5], segments[6], segments[7],
                );
                SocketAddr::new(IpAddr::V6(ip), self.port)
            }
        }
    }

    pub fn from_socket_addr(addr: SocketAddr) -> Self {
        match addr.ip() {
            IpAddr::V4(ip) => {
                let octets = ip.octets();
                IpSocketAddress {
                    ip: IpAddress::V4(octets),
                    port: addr.port(),
                    flow_info: 0,
                    scope_id: 0,
                }
            }
            IpAddr::V6(ip) => {
                let segments = ip.segments();
                IpSocketAddress {
                    ip: IpAddress::V6(segments),
                    port: addr.port(),
                    flow_info: 0, // TODO: get from IPv6 header
                    scope_id: 0,  // TODO: get scope ID
                }
            }
        }
    }
}

/// TCP socket state
pub struct TcpSocketState {
    pub listener: Option<TcpListener>,
    pub stream: Option<TcpStream>,
    pub bound_addr: Option<SocketAddr>,
}

/// UDP socket state
pub struct UdpSocketState {
    pub socket: Option<UdpSocket>,
    pub bound_addr: Option<SocketAddr>,
}

// TCP Socket Functions

/// Create a new TCP socket
pub fn tcp_socket_create(_context: &WasiPreview2Context, is_ipv6: bool) -> WasmtimeResult<u64> {
    let handle = next_handle();

    // Create socket info
    let mut info = TcpSocketInfo::default();
    info.is_ipv6 = is_ipv6;
    info.state = TcpSocketStateEnum::Created;

    // Store socket state
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    states.insert(handle, info);

    Ok(handle)
}

/// Start binding a TCP socket to an address (non-blocking first phase)
pub fn tcp_socket_start_bind(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    if info.state != TcpSocketStateEnum::Created {
        return Err(WasmtimeError::InvalidState {
            message: format!("Socket not in Created state, current state: {:?}", info.state),
        });
    }

    // Store the bind address for finish_bind
    info.bound_addr = Some(addr.to_socket_addr());
    info.state = TcpSocketStateEnum::Binding;

    Ok(())
}

/// Complete the bind operation
pub fn tcp_socket_finish_bind(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    if info.state != TcpSocketStateEnum::Binding {
        return Err(WasmtimeError::InvalidState {
            message: format!("Socket not in Binding state, current state: {:?}", info.state),
        });
    }

    // The actual bind happens when we start listening or connecting
    // For now, just transition to Bound state
    info.state = TcpSocketStateEnum::Bound;

    Ok(())
}

/// Start a non-blocking connect operation
pub fn tcp_socket_start_connect(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    if info.state != TcpSocketStateEnum::Created && info.state != TcpSocketStateEnum::Bound {
        return Err(WasmtimeError::InvalidState {
            message: format!("Socket not in Created or Bound state, current state: {:?}", info.state),
        });
    }

    info.remote_addr = Some(addr.to_socket_addr());
    info.state = TcpSocketStateEnum::Connecting;

    Ok(())
}

/// Complete the connect operation and return stream handles
pub fn tcp_socket_finish_connect(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<(u64, u64)> {
    // Get the remote address first
    let remote_addr = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        if info.state != TcpSocketStateEnum::Connecting {
            return Err(WasmtimeError::InvalidState {
                message: format!("Socket not in Connecting state, current state: {:?}", info.state),
            });
        }

        info.remote_addr.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "No remote address set for connection".to_string(),
            }
        })?
    };

    // Perform the actual connection
    let stream = TcpStream::connect(remote_addr).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("Failed to connect to {}: {}", remote_addr, e),
            ),
        }
    })?;

    // Generate handles for the stream (input and output share the same underlying stream)
    let stream_handle = next_handle();
    let input_stream_handle = next_handle();
    let output_stream_handle = next_handle();

    // Store the stream
    {
        let mut streams = TCP_STREAMS.lock().unwrap();
        streams.insert(stream_handle, Arc::new(Mutex::new(stream)));
    }

    // Update socket state
    {
        let mut states = TCP_SOCKET_STATE.lock().unwrap();
        if let Some(info) = states.get_mut(&socket_handle) {
            info.state = TcpSocketStateEnum::Connected;
            info.stream_handle = Some(stream_handle);
        }
    }

    Ok((input_stream_handle, output_stream_handle))
}

/// Start listening on the socket (non-blocking first phase)
pub fn tcp_socket_start_listen(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    if info.state != TcpSocketStateEnum::Bound {
        return Err(WasmtimeError::InvalidState {
            message: format!("Socket must be bound before listening, current state: {:?}", info.state),
        });
    }

    // State remains Bound until finish_listen completes
    Ok(())
}

/// Complete the listen setup
pub fn tcp_socket_finish_listen(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    // Get socket info
    let (bound_addr, backlog) = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        let addr = info.bound_addr.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "Socket not bound to an address".to_string(),
            }
        })?;

        (addr, info.backlog)
    };

    // Create the listener
    let listener = TcpListener::bind(bound_addr).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("Failed to bind TCP listener to {}: {}", bound_addr, e),
            ),
        }
    })?;

    // Set non-blocking mode for accept operations
    listener.set_nonblocking(true).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("Failed to set non-blocking mode: {}", e),
            ),
        }
    })?;

    // Get the actual bound address (in case port was 0)
    let actual_addr = listener.local_addr().map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to get local address: {}", e)),
        }
    })?;

    // Store the listener
    let listener_handle = next_handle();
    {
        let mut listeners = TCP_LISTENERS.lock().unwrap();
        listeners.insert(listener_handle, Arc::new(Mutex::new(listener)));
    }

    // Update socket state
    {
        let mut states = TCP_SOCKET_STATE.lock().unwrap();
        if let Some(info) = states.get_mut(&socket_handle) {
            info.state = TcpSocketStateEnum::Listening;
            info.listener_handle = Some(listener_handle);
            info.bound_addr = Some(actual_addr);
        }
    }

    // Note: backlog is set at the OS level and we can't easily change it after bind
    // Most systems will use a default if we don't specify
    let _ = backlog;

    Ok(())
}

/// Accept a new connection from a listening socket
pub fn tcp_socket_accept(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<(u64, u64, u64)> {
    // Get the listener handle
    let (listener_handle, is_ipv6) = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        if info.state != TcpSocketStateEnum::Listening {
            return Err(WasmtimeError::InvalidState {
                message: format!("Socket not in Listening state, current state: {:?}", info.state),
            });
        }

        let lh = info.listener_handle.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "No listener associated with socket".to_string(),
            }
        })?;

        (lh, info.is_ipv6)
    };

    // Get the listener and accept a connection
    let (stream, remote_addr) = {
        let listeners = TCP_LISTENERS.lock().unwrap();
        let listener_arc = listeners.get(&listener_handle).ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "Listener not found".to_string(),
            }
        })?;
        let listener = listener_arc.lock().unwrap();

        listener.accept().map_err(|e| {
            if e.kind() == std::io::ErrorKind::WouldBlock {
                WasmtimeError::WouldBlock {
                    message: "No pending connections".to_string(),
                }
            } else {
                WasmtimeError::Io {
                    source: std::io::Error::new(std::io::ErrorKind::Other, format!("Accept failed: {}", e)),
                }
            }
        })?
    };

    // Get local address of the accepted connection
    let local_addr = stream.local_addr().ok();

    // Create a new socket handle for the accepted connection
    let new_socket_handle = next_handle();
    let stream_handle = next_handle();
    let input_stream_handle = next_handle();
    let output_stream_handle = next_handle();

    // Store the stream
    {
        let mut streams = TCP_STREAMS.lock().unwrap();
        streams.insert(stream_handle, Arc::new(Mutex::new(stream)));
    }

    // Create socket info for the new connection
    {
        let mut states = TCP_SOCKET_STATE.lock().unwrap();
        let mut new_info = TcpSocketInfo::default();
        new_info.is_ipv6 = is_ipv6;
        new_info.state = TcpSocketStateEnum::Connected;
        new_info.bound_addr = local_addr;
        new_info.remote_addr = Some(remote_addr);
        new_info.stream_handle = Some(stream_handle);
        states.insert(new_socket_handle, new_info);
    }

    Ok((new_socket_handle, input_stream_handle, output_stream_handle))
}

/// Get the local address of a socket
pub fn tcp_socket_local_address(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    let states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    let addr = info.bound_addr.ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Socket not bound to an address".to_string(),
        }
    })?;

    Ok(IpSocketAddress::from_socket_addr(addr))
}

/// Get the remote address of a connected socket
pub fn tcp_socket_remote_address(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    let states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    let addr = info.remote_addr.ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Socket not connected to a remote address".to_string(),
        }
    })?;

    Ok(IpSocketAddress::from_socket_addr(addr))
}

/// Get the address family of a socket
pub fn tcp_socket_address_family(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<bool> {
    let states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    Ok(info.is_ipv6)
}

/// Set the listen backlog size
pub fn tcp_socket_set_listen_backlog_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.backlog = value as u32;
    Ok(())
}

/// Enable or disable TCP keep-alive
pub fn tcp_socket_set_keep_alive_enabled(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    enabled: bool,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.keep_alive_enabled = enabled;
    Ok(())
}

/// Set the TCP keep-alive idle time
pub fn tcp_socket_set_keep_alive_idle_time(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    duration_nanos: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.keep_alive_idle_time_ns = duration_nanos;
    Ok(())
}

/// Set the TCP keep-alive interval
pub fn tcp_socket_set_keep_alive_interval(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    duration_nanos: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.keep_alive_interval_ns = duration_nanos;
    Ok(())
}

/// Set the TCP keep-alive probe count
pub fn tcp_socket_set_keep_alive_count(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    count: u32,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.keep_alive_count = count;
    Ok(())
}

/// Set the hop limit (TTL) for the socket
pub fn tcp_socket_set_hop_limit(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u8,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.hop_limit = value;
    Ok(())
}

/// Get the receive buffer size
pub fn tcp_socket_receive_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<u64> {
    let states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    Ok(info.receive_buffer_size)
}

/// Set the receive buffer size
pub fn tcp_socket_set_receive_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.receive_buffer_size = value;
    Ok(())
}

/// Get the send buffer size
pub fn tcp_socket_send_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<u64> {
    let states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    Ok(info.send_buffer_size)
}

/// Set the send buffer size
pub fn tcp_socket_set_send_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u64,
) -> WasmtimeResult<()> {
    let mut states = TCP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        }
    })?;

    info.send_buffer_size = value;
    Ok(())
}

/// Subscribe to socket events (returns a pollable handle)
pub fn tcp_socket_subscribe(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<u64> {
    // Verify socket exists
    let states = TCP_SOCKET_STATE.lock().unwrap();
    if !states.contains_key(&socket_handle) {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid TCP socket handle: {}", socket_handle),
        });
    }

    // Return a pollable handle (for now, just a unique ID)
    // Full async/poll implementation would require integration with tokio runtime
    Ok(next_handle())
}

/// Shutdown the socket for reading, writing, or both
pub fn tcp_socket_shutdown(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    shutdown_type: u8, // 0=receive, 1=send, 2=both
) -> WasmtimeResult<()> {
    // Get the stream handle
    let stream_handle = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        info.stream_handle.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "Socket not connected".to_string(),
            }
        })?
    };

    // Get the stream and shutdown
    let streams = TCP_STREAMS.lock().unwrap();
    let stream_arc = streams.get(&stream_handle).ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Stream not found".to_string(),
        }
    })?;
    let stream = stream_arc.lock().unwrap();

    let shutdown = match shutdown_type {
        0 => std::net::Shutdown::Read,
        1 => std::net::Shutdown::Write,
        _ => std::net::Shutdown::Both,
    };

    stream.shutdown(shutdown).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Shutdown failed: {}", e)),
        }
    })?;

    Ok(())
}

/// Close a TCP socket and release all resources
pub fn tcp_socket_close(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    // Remove socket state and get associated handles
    let (listener_handle, stream_handle) = {
        let mut states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.remove(&socket_handle);
        match info {
            Some(i) => (i.listener_handle, i.stream_handle),
            None => (None, None),
        }
    };

    // Remove listener if present
    if let Some(lh) = listener_handle {
        let mut listeners = TCP_LISTENERS.lock().unwrap();
        listeners.remove(&lh);
    }

    // Remove stream if present
    if let Some(sh) = stream_handle {
        let mut streams = TCP_STREAMS.lock().unwrap();
        streams.remove(&sh);
    }

    Ok(())
}

/// Read data from a TCP stream
pub fn tcp_stream_read(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    max_bytes: usize,
) -> WasmtimeResult<Vec<u8>> {
    // Get the stream handle
    let stream_handle = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        info.stream_handle.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "Socket not connected".to_string(),
            }
        })?
    };

    // Read from the stream
    let streams = TCP_STREAMS.lock().unwrap();
    let stream_arc = streams.get(&stream_handle).ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Stream not found".to_string(),
        }
    })?;
    let mut stream = stream_arc.lock().unwrap();

    // Set non-blocking for this read
    stream.set_nonblocking(true).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to set non-blocking: {}", e)),
        }
    })?;

    let mut buffer = vec![0u8; max_bytes];
    use std::io::Read;
    match stream.read(&mut buffer) {
        Ok(0) => {
            // EOF
            Ok(Vec::new())
        }
        Ok(n) => {
            buffer.truncate(n);
            Ok(buffer)
        }
        Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => {
            // No data available
            Err(WasmtimeError::WouldBlock {
                message: "No data available".to_string(),
            })
        }
        Err(e) => Err(WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Read failed: {}", e)),
        }),
    }
}

/// Write data to a TCP stream
pub fn tcp_stream_write(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    data: &[u8],
) -> WasmtimeResult<usize> {
    // Get the stream handle
    let stream_handle = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        info.stream_handle.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "Socket not connected".to_string(),
            }
        })?
    };

    // Write to the stream
    let streams = TCP_STREAMS.lock().unwrap();
    let stream_arc = streams.get(&stream_handle).ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Stream not found".to_string(),
        }
    })?;
    let mut stream = stream_arc.lock().unwrap();

    // Set non-blocking for this write
    stream.set_nonblocking(true).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to set non-blocking: {}", e)),
        }
    })?;

    use std::io::Write;
    match stream.write(data) {
        Ok(n) => Ok(n),
        Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => {
            Err(WasmtimeError::WouldBlock {
                message: "Write would block".to_string(),
            })
        }
        Err(e) => Err(WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Write failed: {}", e)),
        }),
    }
}

/// Flush a TCP stream
pub fn tcp_stream_flush(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    // Get the stream handle
    let stream_handle = {
        let states = TCP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid TCP socket handle: {}", socket_handle),
            }
        })?;

        info.stream_handle.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "Socket not connected".to_string(),
            }
        })?
    };

    // Flush the stream
    let streams = TCP_STREAMS.lock().unwrap();
    let stream_arc = streams.get(&stream_handle).ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Stream not found".to_string(),
        }
    })?;
    let mut stream = stream_arc.lock().unwrap();

    use std::io::Write;
    stream.flush().map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Flush failed: {}", e)),
        }
    })?;

    Ok(())
}

// UDP Socket Functions

/// Create a new UDP socket
pub fn udp_socket_create(_context: &WasiPreview2Context, is_ipv6: bool) -> WasmtimeResult<u64> {
    let handle = next_handle();

    // Create socket info
    let mut info = UdpSocketInfo::default();
    info.is_ipv6 = is_ipv6;
    info.state = UdpSocketStateEnum::Created;

    // Store socket state (socket itself will be created during bind)
    let mut states = UDP_SOCKET_STATE.lock().unwrap();
    states.insert(handle, info);

    Ok(handle)
}

/// Start binding a UDP socket to an address (non-blocking first phase)
pub fn udp_socket_start_bind(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    let mut states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    if info.state != UdpSocketStateEnum::Created {
        return Err(WasmtimeError::InvalidState {
            message: format!("Socket not in Created state, current state: {:?}", info.state),
        });
    }

    // Store the bind address for finish_bind
    info.bound_addr = Some(addr.to_socket_addr());
    info.state = UdpSocketStateEnum::Binding;

    Ok(())
}

/// Complete the bind operation
pub fn udp_socket_finish_bind(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    // Get the bind address
    let bind_addr = {
        let states = UDP_SOCKET_STATE.lock().unwrap();
        let info = states.get(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid UDP socket handle: {}", socket_handle),
            }
        })?;

        if info.state != UdpSocketStateEnum::Binding {
            return Err(WasmtimeError::InvalidState {
                message: format!("Socket not in Binding state, current state: {:?}", info.state),
            });
        }

        info.bound_addr.ok_or_else(|| {
            WasmtimeError::InvalidState {
                message: "No bind address set".to_string(),
            }
        })?
    };

    // Create and bind the actual UDP socket
    let socket = UdpSocket::bind(bind_addr).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(
                std::io::ErrorKind::Other,
                format!("Failed to bind UDP socket to {}: {}", bind_addr, e),
            ),
        }
    })?;

    // Get the actual bound address (in case port was 0)
    let actual_addr = socket.local_addr().map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to get local address: {}", e)),
        }
    })?;

    // Store the socket
    {
        let mut sockets = UDP_SOCKETS.lock().unwrap();
        sockets.insert(socket_handle, Arc::new(Mutex::new(socket)));
    }

    // Update socket state
    {
        let mut states = UDP_SOCKET_STATE.lock().unwrap();
        if let Some(info) = states.get_mut(&socket_handle) {
            info.state = UdpSocketStateEnum::Bound;
            info.bound_addr = Some(actual_addr);
        }
    }

    Ok(())
}

/// Connect a UDP socket to a remote address (connected UDP mode)
pub fn udp_socket_stream(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    remote_addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    // Update state first
    {
        let mut states = UDP_SOCKET_STATE.lock().unwrap();
        let info = states.get_mut(&socket_handle).ok_or_else(|| {
            WasmtimeError::InvalidParameter {
                message: format!("Invalid UDP socket handle: {}", socket_handle),
            }
        })?;

        if info.state != UdpSocketStateEnum::Bound {
            return Err(WasmtimeError::InvalidState {
                message: format!("Socket must be bound before connecting, current state: {:?}", info.state),
            });
        }

        info.remote_addr = Some(remote_addr.to_socket_addr());
        info.state = UdpSocketStateEnum::Connected;
    }

    // Connect the socket
    let dest = remote_addr.to_socket_addr();
    let sockets = UDP_SOCKETS.lock().unwrap();
    let socket_arc = sockets.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Socket not found".to_string(),
        }
    })?;
    let socket = socket_arc.lock().unwrap();

    socket.connect(dest).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to connect UDP socket: {}", e)),
        }
    })?;

    Ok(())
}

/// Get the local address of a UDP socket
pub fn udp_socket_local_address(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    // First check if we have a bound socket
    let sockets = UDP_SOCKETS.lock().unwrap();
    if let Some(socket_arc) = sockets.get(&socket_handle) {
        let socket = socket_arc.lock().unwrap();
        let addr = socket.local_addr().map_err(|e| {
            WasmtimeError::Io {
                source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to get local address: {}", e)),
            }
        })?;
        return Ok(IpSocketAddress::from_socket_addr(addr));
    }

    // Fall back to state info for unbound sockets
    let states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    if let Some(addr) = info.bound_addr {
        Ok(IpSocketAddress::from_socket_addr(addr))
    } else {
        Err(WasmtimeError::InvalidState {
            message: "Socket not bound to an address".to_string(),
        })
    }
}

/// Get the remote address of a connected UDP socket
pub fn udp_socket_remote_address(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    let states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    if info.state != UdpSocketStateEnum::Connected {
        return Err(WasmtimeError::InvalidState {
            message: format!("Socket not connected, current state: {:?}", info.state),
        });
    }

    let addr = info.remote_addr.ok_or_else(|| {
        WasmtimeError::InvalidState {
            message: "Socket not connected to a remote address".to_string(),
        }
    })?;

    Ok(IpSocketAddress::from_socket_addr(addr))
}

/// Get the address family of a UDP socket (true = IPv6, false = IPv4)
pub fn udp_socket_address_family(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<bool> {
    let states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    Ok(info.is_ipv6)
}

/// Set the unicast hop limit (TTL) for a UDP socket
pub fn udp_socket_set_unicast_hop_limit(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u8,
) -> WasmtimeResult<()> {
    let mut states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    info.unicast_hop_limit = value;

    // If socket is already bound, try to set the TTL on the actual socket
    drop(states);
    let sockets = UDP_SOCKETS.lock().unwrap();
    if let Some(socket_arc) = sockets.get(&socket_handle) {
        let socket = socket_arc.lock().unwrap();
        // Note: set_ttl may not be supported on all platforms
        let _ = socket.set_ttl(value as u32);
    }

    Ok(())
}

/// Get the receive buffer size for a UDP socket
pub fn udp_socket_receive_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<u64> {
    let states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    Ok(info.receive_buffer_size)
}

/// Set the receive buffer size for a UDP socket
pub fn udp_socket_set_receive_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u64,
) -> WasmtimeResult<()> {
    let mut states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    info.receive_buffer_size = value;
    Ok(())
}

/// Get the send buffer size for a UDP socket
pub fn udp_socket_send_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<u64> {
    let states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    Ok(info.send_buffer_size)
}

/// Set the send buffer size for a UDP socket
pub fn udp_socket_set_send_buffer_size(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    value: u64,
) -> WasmtimeResult<()> {
    let mut states = UDP_SOCKET_STATE.lock().unwrap();
    let info = states.get_mut(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;

    info.send_buffer_size = value;
    Ok(())
}

/// Subscribe to UDP socket events (returns a pollable handle)
pub fn udp_socket_subscribe(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<u64> {
    // Verify socket exists
    let states = UDP_SOCKET_STATE.lock().unwrap();
    if !states.contains_key(&socket_handle) {
        return Err(WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        });
    }

    // Return a pollable handle (for now, just a unique ID)
    // Full async/poll implementation would require integration with tokio runtime
    Ok(next_handle())
}

pub fn udp_socket_receive(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    max_results: u64,
) -> WasmtimeResult<Vec<(Vec<u8>, IpSocketAddress)>> {
    // MVP: Use Rust stdlib UDP socket from global registry
    // TODO: Replace with actual Wasmtime WASI socket integration

    // Get the socket from the registry
    let sockets = UDP_SOCKETS.lock().unwrap();
    let socket_arc = sockets.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;
    let socket = socket_arc.lock().unwrap();

    // Set non-blocking mode for receive
    socket.set_nonblocking(true).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to set non-blocking mode: {}", e)),
        }
    })?;

    let mut datagrams = Vec::new();
    let mut buffer = vec![0u8; 65536]; // Maximum UDP datagram size

    // Try to receive up to max_results datagrams
    for _ in 0..max_results {
        match socket.recv_from(&mut buffer) {
            Ok((len, addr)) => {
                let data = buffer[..len].to_vec();
                let ip_addr = IpSocketAddress::from_socket_addr(addr);
                datagrams.push((data, ip_addr));
            }
            Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                // No more data available - this is normal for non-blocking I/O
                break;
            }
            Err(e) => {
                return Err(WasmtimeError::Io {
                    source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to receive datagram: {}", e)),
                });
            }
        }
    }

    Ok(datagrams)
}

pub fn udp_socket_send(
    _context: &WasiPreview2Context,
    socket_handle: u64,
    datagrams: &[(Vec<u8>, Option<IpSocketAddress>)],
) -> WasmtimeResult<u64> {
    // MVP: Use Rust stdlib UDP socket from global registry
    // TODO: Replace with actual Wasmtime WASI socket integration

    // Get the socket from the registry
    let sockets = UDP_SOCKETS.lock().unwrap();
    let socket_arc = sockets.get(&socket_handle).ok_or_else(|| {
        WasmtimeError::InvalidParameter {
            message: format!("Invalid UDP socket handle: {}", socket_handle),
        }
    })?;
    let socket = socket_arc.lock().unwrap();

    // Set non-blocking mode for send
    socket.set_nonblocking(true).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to set non-blocking mode: {}", e)),
        }
    })?;

    let mut sent_count = 0u64;

    // Send each datagram
    for (data, addr_opt) in datagrams {
        // Determine destination address
        let dest_addr = if let Some(addr) = addr_opt {
            addr.to_socket_addr()
        } else {
            // If no address provided, the socket must have been connected via stream()
            // For MVP, we'll return an error
            return Err(WasmtimeError::InvalidState {
                message: "Cannot send without address on unconnected UDP socket".to_string(),
            });
        };

        // Send the datagram
        match socket.send_to(data, dest_addr) {
            Ok(_) => {
                sent_count += 1;
            }
            Err(e) if e.kind() == std::io::ErrorKind::WouldBlock => {
                // Socket buffer is full, stop sending
                break;
            }
            Err(e) => {
                // Return the count of successfully sent datagrams before the error
                // This matches WASI semantics of partial success
                if sent_count > 0 {
                    return Ok(sent_count);
                }
                return Err(WasmtimeError::Io {
                    source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to send datagram: {}", e)),
                });
            }
        }
    }

    Ok(sent_count)
}

/// Close a UDP socket and release all resources
pub fn udp_socket_close(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    // Remove socket state
    {
        let mut states = UDP_SOCKET_STATE.lock().unwrap();
        if let Some(info) = states.get_mut(&socket_handle) {
            info.state = UdpSocketStateEnum::Closed;
        }
        states.remove(&socket_handle);
    }

    // Remove actual socket
    {
        let mut sockets = UDP_SOCKETS.lock().unwrap();
        sockets.remove(&socket_handle);
    }

    Ok(())
}

// Network Functions

pub fn network_create(_context: &WasiPreview2Context) -> WasmtimeResult<u64> {
    // MVP: Return network handle
    Ok(5000)
}

pub fn network_close(
    _context: &WasiPreview2Context,
    _network_handle: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

// =============================================================================
// IP Name Lookup (DNS Resolution) Functions
// =============================================================================

/// State for an address resolution stream
struct ResolveAddressStreamState {
    /// Resolved addresses (IPv4 and IPv6)
    addresses: Vec<IpAddress>,
    /// Current index in the addresses vector
    current_index: usize,
    /// Whether the stream has been closed
    closed: bool,
    /// Optional address family filter (0 = none, 4 = IPv4, 6 = IPv6)
    #[allow(dead_code)]
    address_family_filter: u8,
}

lazy_static::lazy_static! {
    /// Global storage for resolve address streams
    static ref RESOLVE_STREAMS: std::sync::Mutex<std::collections::HashMap<u64, ResolveAddressStreamState>> =
        std::sync::Mutex::new(std::collections::HashMap::new());
}

/// Counter for generating unique stream handles
static STREAM_HANDLE_COUNTER: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

/// Resolves a hostname to IP addresses and returns a stream handle.
///
/// # Arguments
/// * `_context` - The WASI preview 2 context
/// * `_network_handle` - The network handle (validated but not used for resolution)
/// * `hostname` - The hostname to resolve (e.g., "example.com")
/// * `address_family` - Address family filter: 0 = all, 4 = IPv4 only, 6 = IPv6 only
///
/// # Returns
/// A stream handle that can be used with `resolve_address_stream_next` and
/// `resolve_address_stream_close`.
pub fn ip_name_lookup_resolve_addresses(
    _context: &WasiPreview2Context,
    _network_handle: u64,
    hostname: &str,
    address_family: u8,
) -> WasmtimeResult<u64> {
    // Validate hostname is not empty
    if hostname.is_empty() {
        return Err(WasmtimeError::Runtime {
            message: "invalid-argument: hostname cannot be empty".to_string(),
            backtrace: None,
        });
    }

    // Try to resolve the hostname using Rust's standard library
    // We append ":0" to make it a valid socket address format
    let socket_addr_str = format!("{}:0", hostname);

    let resolved_addrs: Vec<IpAddress> = match socket_addr_str.to_socket_addrs() {
        Ok(addrs) => {
            addrs
                .filter_map(|addr| {
                    match addr {
                        std::net::SocketAddr::V4(v4) => {
                            if address_family == 0 || address_family == 4 {
                                Some(IpAddress::V4(v4.ip().octets()))
                            } else {
                                None
                            }
                        }
                        std::net::SocketAddr::V6(v6) => {
                            if address_family == 0 || address_family == 6 {
                                // Convert Ipv6Addr segments (u16[8]) to the expected format
                                Some(IpAddress::V6(v6.ip().segments()))
                            } else {
                                None
                            }
                        }
                    }
                })
                .collect()
        }
        Err(e) => {
            return Err(WasmtimeError::from(e));
        }
    };

    // Generate a unique stream handle
    let stream_handle = STREAM_HANDLE_COUNTER.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

    // Store the stream state
    let stream_state = ResolveAddressStreamState {
        addresses: resolved_addrs,
        current_index: 0,
        closed: false,
        address_family_filter: address_family,
    };

    {
        let mut streams = RESOLVE_STREAMS.lock().unwrap();
        streams.insert(stream_handle, stream_state);
    }

    Ok(stream_handle)
}

/// Gets the next resolved IP address from the stream.
///
/// # Arguments
/// * `_context` - The WASI preview 2 context
/// * `stream_handle` - The stream handle returned by `ip_name_lookup_resolve_addresses`
///
/// # Returns
/// A tuple of (has_address, is_ipv4, ipv4_bytes, ipv6_segments):
/// - has_address: true if an address was returned, false if stream is exhausted
/// - is_ipv4: true for IPv4, false for IPv6
/// - ipv4_bytes: 4 bytes for IPv4 address (zeroed if IPv6)
/// - ipv6_segments: 8 u16 segments for IPv6 address (zeroed if IPv4)
pub fn resolve_address_stream_next(
    _context: &WasiPreview2Context,
    stream_handle: u64,
) -> WasmtimeResult<(bool, bool, [u8; 4], [u16; 8])> {
    let mut streams = RESOLVE_STREAMS.lock().unwrap();

    let stream_state = streams.get_mut(&stream_handle).ok_or_else(|| {
        WasmtimeError::Runtime {
            message: format!("invalid-state: stream handle {} not found", stream_handle),
            backtrace: None,
        }
    })?;

    if stream_state.closed {
        return Err(WasmtimeError::Runtime {
            message: "invalid-state: stream has been closed".to_string(),
            backtrace: None,
        });
    }

    if stream_state.current_index >= stream_state.addresses.len() {
        // Stream exhausted
        return Ok((false, false, [0u8; 4], [0u16; 8]));
    }

    let address = &stream_state.addresses[stream_state.current_index];
    stream_state.current_index += 1;

    let (is_ipv4, ipv4_bytes, ipv6_segments) = match address {
        IpAddress::V4(bytes) => {
            (true, *bytes, [0u16; 8])
        }
        IpAddress::V6(segments) => {
            (false, [0u8; 4], *segments)
        }
    };

    Ok((true, is_ipv4, ipv4_bytes, ipv6_segments))
}

/// Subscribes to the stream for async notification.
/// In synchronous mode, this is a no-op.
///
/// # Arguments
/// * `_context` - The WASI preview 2 context
/// * `stream_handle` - The stream handle
pub fn resolve_address_stream_subscribe(
    _context: &WasiPreview2Context,
    stream_handle: u64,
) -> WasmtimeResult<()> {
    let streams = RESOLVE_STREAMS.lock().unwrap();

    let stream_state = streams.get(&stream_handle).ok_or_else(|| {
        WasmtimeError::Runtime {
            message: format!("invalid-state: stream handle {} not found", stream_handle),
            backtrace: None,
        }
    })?;

    if stream_state.closed {
        return Err(WasmtimeError::Runtime {
            message: "invalid-state: stream has been closed".to_string(),
            backtrace: None,
        });
    }

    // In synchronous mode, this is a no-op since all addresses are already resolved
    Ok(())
}

/// Checks if the stream has been closed.
///
/// # Arguments
/// * `_context` - The WASI preview 2 context
/// * `stream_handle` - The stream handle
///
/// # Returns
/// true if the stream is closed, false otherwise
pub fn resolve_address_stream_is_closed(
    _context: &WasiPreview2Context,
    stream_handle: u64,
) -> WasmtimeResult<bool> {
    let streams = RESOLVE_STREAMS.lock().unwrap();

    match streams.get(&stream_handle) {
        Some(state) => Ok(state.closed),
        None => Ok(true), // Non-existent streams are considered closed
    }
}

/// Closes the address resolution stream and releases resources.
///
/// # Arguments
/// * `_context` - The WASI preview 2 context
/// * `stream_handle` - The stream handle to close
pub fn resolve_address_stream_close(
    _context: &WasiPreview2Context,
    stream_handle: u64,
) -> WasmtimeResult<()> {
    let mut streams = RESOLVE_STREAMS.lock().unwrap();

    // Mark as closed if it exists, then remove
    if let Some(state) = streams.get_mut(&stream_handle) {
        state.closed = true;
    }
    streams.remove(&stream_handle);

    Ok(())
}
