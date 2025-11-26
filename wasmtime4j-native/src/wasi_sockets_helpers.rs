use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi_preview2::WasiPreview2Context;
use std::collections::HashMap;
use std::net::{IpAddr, Ipv4Addr, Ipv6Addr, SocketAddr, TcpListener, TcpStream, UdpSocket};
use std::sync::{Arc, Mutex};
use std::time::Duration;
use lazy_static::lazy_static;

// Global socket state management
// TODO: Replace with actual Wasmtime WASI socket integration
// For now, using Rust stdlib sockets as MVP implementation (similar to random API)
lazy_static! {
    static ref UDP_SOCKETS: Arc<Mutex<HashMap<u64, Arc<Mutex<UdpSocket>>>>> =
        Arc::new(Mutex::new(HashMap::new()));
    static ref TCP_SOCKETS: Arc<Mutex<HashMap<u64, Arc<Mutex<TcpStream>>>>> =
        Arc::new(Mutex::new(HashMap::new()));
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

pub fn tcp_socket_create(_context: &WasiPreview2Context, is_ipv6: bool) -> WasmtimeResult<u64> {
    // MVP: Create socket handle (placeholder ID)
    // In production, this would interact with Wasmtime's WASI implementation
    Ok(if is_ipv6 { 2 } else { 1 })
}

pub fn tcp_socket_start_bind(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    // MVP: Bind operation would be staged here
    Ok(())
}

pub fn tcp_socket_finish_bind(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<()> {
    // MVP: Complete bind operation
    Ok(())
}

pub fn tcp_socket_start_connect(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    // MVP: Start connection
    Ok(())
}

pub fn tcp_socket_finish_connect(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<(u64, u64)> {
    // MVP: Return (input_stream_handle, output_stream_handle)
    Ok((100, 101))
}

pub fn tcp_socket_start_listen(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<()> {
    // MVP: Start listening
    Ok(())
}

pub fn tcp_socket_finish_listen(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<()> {
    // MVP: Complete listen setup
    Ok(())
}

pub fn tcp_socket_accept(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<(u64, u64, u64)> {
    // MVP: Return (new_socket_handle, input_stream_handle, output_stream_handle)
    Ok((200, 201, 202))
}

pub fn tcp_socket_local_address(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    // MVP: Return placeholder address
    Ok(IpSocketAddress {
        ip: IpAddress::V4([127, 0, 0, 1]),
        port: 8080,
        flow_info: 0,
        scope_id: 0,
    })
}

pub fn tcp_socket_remote_address(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    // MVP: Return placeholder address
    Ok(IpSocketAddress {
        ip: IpAddress::V4([127, 0, 0, 1]),
        port: 9090,
        flow_info: 0,
        scope_id: 0,
    })
}

pub fn tcp_socket_address_family(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<bool> {
    // MVP: Return is_ipv6 based on handle
    Ok(socket_handle == 2)
}

pub fn tcp_socket_set_listen_backlog_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_set_keep_alive_enabled(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _enabled: bool,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_set_keep_alive_idle_time(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _duration_nanos: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_set_keep_alive_interval(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _duration_nanos: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_set_keep_alive_count(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _count: u32,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_set_hop_limit(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u8,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_receive_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<u64> {
    Ok(65536) // Default buffer size
}

pub fn tcp_socket_set_receive_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_send_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<u64> {
    Ok(65536) // Default buffer size
}

pub fn tcp_socket_set_send_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_subscribe(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<u64> {
    // MVP: Return pollable handle
    Ok(1000)
}

pub fn tcp_socket_shutdown(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _shutdown_type: u8, // 0=receive, 1=send, 2=both
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn tcp_socket_close(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

// UDP Socket Functions

pub fn udp_socket_create(_context: &WasiPreview2Context, is_ipv6: bool) -> WasmtimeResult<u64> {
    // MVP: Create UDP socket using Rust stdlib
    // TODO: Replace with actual Wasmtime WASI socket integration

    // Create an unbound socket (will bind later with start_bind/finish_bind)
    let bind_addr = if is_ipv6 {
        ":::0"  // IPv6 any address
    } else {
        "0.0.0.0:0"  // IPv4 any address
    };

    let socket = UdpSocket::bind(bind_addr).map_err(|e| {
        WasmtimeError::Io {
            source: std::io::Error::new(std::io::ErrorKind::Other, format!("Failed to create UDP socket: {}", e)),
        }
    })?;

    // Generate a unique handle ID
    static NEXT_HANDLE: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1000);
    let handle = NEXT_HANDLE.fetch_add(1, std::sync::atomic::Ordering::SeqCst);

    // Store the socket
    let mut sockets = UDP_SOCKETS.lock().unwrap();
    sockets.insert(handle, Arc::new(Mutex::new(socket)));

    Ok(handle)
}

pub fn udp_socket_start_bind(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn udp_socket_finish_bind(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn udp_socket_stream(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _remote_addr: &IpSocketAddress,
) -> WasmtimeResult<()> {
    // MVP: Set remote address for connected UDP
    Ok(())
}

pub fn udp_socket_local_address(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    Ok(IpSocketAddress {
        ip: IpAddress::V4([0, 0, 0, 0]),
        port: 0,
        flow_info: 0,
        scope_id: 0,
    })
}

pub fn udp_socket_remote_address(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<IpSocketAddress> {
    Ok(IpSocketAddress {
        ip: IpAddress::V4([127, 0, 0, 1]),
        port: 8080,
        flow_info: 0,
        scope_id: 0,
    })
}

pub fn udp_socket_address_family(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<bool> {
    Ok(socket_handle == 4)
}

pub fn udp_socket_set_unicast_hop_limit(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u8,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn udp_socket_receive_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<u64> {
    Ok(65536)
}

pub fn udp_socket_set_receive_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn udp_socket_send_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<u64> {
    Ok(65536)
}

pub fn udp_socket_set_send_buffer_size(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
    _value: u64,
) -> WasmtimeResult<()> {
    Ok(())
}

pub fn udp_socket_subscribe(
    _context: &WasiPreview2Context,
    _socket_handle: u64,
) -> WasmtimeResult<u64> {
    Ok(2000)
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

pub fn udp_socket_close(
    _context: &WasiPreview2Context,
    socket_handle: u64,
) -> WasmtimeResult<()> {
    // MVP: Remove socket from global registry
    // TODO: Replace with actual Wasmtime WASI socket integration

    let mut sockets = UDP_SOCKETS.lock().unwrap();
    sockets.remove(&socket_handle);
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
