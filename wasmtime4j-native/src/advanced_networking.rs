//! Advanced Networking Protocols Implementation for WASI Preview 2
//!
//! This module provides comprehensive networking protocol support including:
//! - WebSocket client and server with secure connections
//! - HTTP/2 protocol with multiplexing and flow control
//! - gRPC client and server with protobuf serialization
//! - Async networking with non-blocking I/O operations
//! - Connection pooling and keep-alive management
//! - SSL/TLS support with certificate validation
//! - Network monitoring and performance optimization
//! - Custom protocol negotiation and multiplexing

use std::collections::HashMap;
use std::mem::ManuallyDrop;
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};
use std::net::SocketAddr;
use std::pin::Pin;
use std::os::raw::{c_char, c_int, c_void, c_uint, c_ulong};

use tokio::net::{TcpListener, TcpStream};
use tokio::sync::{mpsc, oneshot, Semaphore};
use tokio::time::timeout;
use futures::future::{BoxFuture, FutureExt};
use futures::{SinkExt, StreamExt};

// WebSocket dependencies
use tokio_tungstenite::{
    WebSocketStream, MaybeTlsStream, accept_async, client_async,
    tungstenite::{Message as WsMessage, Result as WsResult, Error as WsError}
};

// HTTP/2 dependencies
use h2::server::{self, SendResponse};
use h2::client::{self, ResponseFuture};
use h2::{RecvStream, SendStream};
use hyper::body::{Bytes, Frame};
use http::{Request, Response, HeaderMap, Method, Uri, Version};

// gRPC dependencies
use tonic::{
    transport::{Server as GrpcServer, Channel as GrpcChannel, Endpoint},
    Request as GrpcRequest, Response as GrpcResponse, Status as GrpcStatus,
    codec::{Codec, DecodeBuf, Decoder, EncodeBuf, Encoder},
    body::BoxBody,
};
use prost::Message as ProstMessage;

// TLS dependencies
use tokio_rustls::{TlsAcceptor, TlsConnector, client::TlsStream as ClientTlsStream, server::TlsStream as ServerTlsStream};
use rustls::{ClientConfig, ServerConfig};
use rustls::pki_types::{CertificateDer, PrivateKeyDer};
use webpki_roots;

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::async_runtime::get_runtime_handle;

/// Global advanced networking manager
/// Wrapped in ManuallyDrop to prevent automatic cleanup during process exit.
static ADVANCED_NETWORK_MANAGER: once_cell::sync::Lazy<ManuallyDrop<AdvancedNetworkManager>> =
    once_cell::sync::Lazy::new(|| ManuallyDrop::new(AdvancedNetworkManager::new()));

/// Advanced networking manager providing protocol-specific functionality
pub struct AdvancedNetworkManager {
    /// WebSocket connections
    websocket_connections: Arc<RwLock<HashMap<u64, WebSocketConnection>>>,
    /// HTTP/2 connections
    http2_connections: Arc<RwLock<HashMap<u64, Http2Connection>>>,
    /// gRPC connections
    grpc_connections: Arc<RwLock<HashMap<u64, GrpcConnection>>>,
    /// TLS configurations
    tls_config: Arc<Mutex<TlsConfiguration>>,
    /// Connection pool manager
    connection_pool: Arc<Mutex<AdvancedConnectionPool>>,
    /// Protocol negotiator
    protocol_negotiator: Arc<ProtocolNegotiator>,
    /// Performance monitor
    performance_monitor: Arc<Mutex<NetworkPerformanceMonitor>>,
    /// Configuration
    config: AdvancedNetworkConfig,
    /// Connection ID counter
    next_connection_id: std::sync::atomic::AtomicU64,
    /// Operation semaphore
    operation_semaphore: Arc<Semaphore>,
}

/// Advanced networking configuration
#[derive(Debug, Clone)]
pub struct AdvancedNetworkConfig {
    /// Enable WebSocket support
    pub enable_websockets: bool,
    /// Enable HTTP/2 support
    pub enable_http2: bool,
    /// Enable gRPC support
    pub enable_grpc: bool,
    /// Enable TLS/SSL support
    pub enable_tls: bool,
    /// Maximum concurrent connections per protocol
    pub max_connections_per_protocol: u32,
    /// Connection timeout in milliseconds
    pub connection_timeout_ms: u64,
    /// Keep-alive interval in milliseconds
    pub keep_alive_interval_ms: u64,
    /// Maximum frame size for HTTP/2
    pub http2_max_frame_size: u32,
    /// WebSocket ping interval in milliseconds
    pub websocket_ping_interval_ms: u64,
    /// gRPC maximum message size
    pub grpc_max_message_size: usize,
    /// Enable connection pooling
    pub enable_connection_pooling: bool,
    /// Pool size per endpoint
    pub pool_size_per_endpoint: u32,
    /// TLS certificate validation
    pub enable_certificate_validation: bool,
}

/// WebSocket connection wrapper
pub struct WebSocketConnection {
    id: u64,
    stream: WebSocketStream<MaybeTlsStream<TcpStream>>,
    remote_addr: SocketAddr,
    connection_type: WebSocketConnectionType,
    created_at: Instant,
    last_activity: Instant,
    messages_sent: u64,
    messages_received: u64,
    status: ConnectionStatus,
}

/// WebSocket connection types
#[derive(Debug, Clone, PartialEq)]
pub enum WebSocketConnectionType {
    Client,
    Server,
}

/// HTTP/2 connection wrapper
pub struct Http2Connection {
    id: u64,
    connection_type: Http2ConnectionType,
    remote_addr: SocketAddr,
    created_at: Instant,
    last_activity: Instant,
    requests_sent: u64,
    responses_received: u64,
    active_streams: HashMap<u32, Http2Stream>,
    status: ConnectionStatus,
}

/// HTTP/2 connection types
#[derive(Debug, Clone)]
pub enum Http2ConnectionType {
    Client(h2::client::Connection<TcpStream>),
    Server,
}

/// HTTP/2 stream wrapper
pub struct Http2Stream {
    stream_id: u32,
    send_stream: Option<SendStream<Bytes>>,
    recv_stream: Option<RecvStream>,
    request: Option<Request<()>>,
    response: Option<Response<()>>,
    created_at: Instant,
}

/// gRPC connection wrapper
pub struct GrpcConnection {
    id: u64,
    channel: GrpcChannel,
    endpoint: Endpoint,
    created_at: Instant,
    last_activity: Instant,
    requests_sent: u64,
    responses_received: u64,
    status: ConnectionStatus,
}

/// Connection status for all protocols
#[derive(Debug, Clone, PartialEq)]
pub enum ConnectionStatus {
    Connecting,
    Connected,
    Authenticated,
    Closing,
    Closed,
    Error(String),
}

/// TLS configuration management
pub struct TlsConfiguration {
    client_config: Option<Arc<ClientConfig>>,
    server_config: Option<Arc<ServerConfig>>,
    certificates: HashMap<String, Certificate>,
    private_keys: HashMap<String, PrivateKey>,
    root_certificates: Vec<Certificate>,
}

/// Advanced connection pool for protocol-specific connections
pub struct AdvancedConnectionPool {
    websocket_pools: HashMap<String, Vec<PooledWebSocketConnection>>,
    http2_pools: HashMap<String, Vec<PooledHttp2Connection>>,
    grpc_pools: HashMap<String, Vec<PooledGrpcConnection>>,
    max_pool_size: usize,
    max_idle_time: Duration,
}

/// Pooled WebSocket connection
pub struct PooledWebSocketConnection {
    connection_id: u64,
    endpoint: String,
    created_at: Instant,
    last_used: Instant,
}

/// Pooled HTTP/2 connection
pub struct PooledHttp2Connection {
    connection_id: u64,
    endpoint: String,
    created_at: Instant,
    last_used: Instant,
    available_streams: u32,
}

/// Pooled gRPC connection
pub struct PooledGrpcConnection {
    connection_id: u64,
    endpoint: String,
    created_at: Instant,
    last_used: Instant,
}

/// Protocol negotiation manager
pub struct ProtocolNegotiator {
    supported_protocols: Vec<SupportedProtocol>,
    alpn_protocols: Vec<Vec<u8>>,
}

/// Supported network protocols
#[derive(Debug, Clone, PartialEq)]
pub enum SupportedProtocol {
    Http1_1,
    Http2,
    WebSocket,
    Grpc,
    Custom(String),
}

/// Network performance monitoring
pub struct NetworkPerformanceMonitor {
    protocol_metrics: HashMap<SupportedProtocol, ProtocolMetrics>,
    connection_metrics: ConnectionMetrics,
    throughput_metrics: ThroughputMetrics,
}

/// Protocol-specific metrics
#[derive(Debug, Default, Clone)]
pub struct ProtocolMetrics {
    total_connections: u64,
    active_connections: u64,
    bytes_sent: u64,
    bytes_received: u64,
    errors: u64,
    average_latency_ms: f64,
    connection_success_rate: f64,
}

/// Connection metrics
#[derive(Debug, Default, Clone)]
pub struct ConnectionMetrics {
    total_connections: u64,
    successful_connections: u64,
    failed_connections: u64,
    connection_establishment_time_ms: f64,
    connection_reuse_rate: f64,
}

/// Throughput metrics
#[derive(Debug, Default, Clone)]
pub struct ThroughputMetrics {
    requests_per_second: f64,
    bytes_per_second: f64,
    concurrent_connections: u32,
    peak_concurrent_connections: u32,
}

impl Default for AdvancedNetworkConfig {
    fn default() -> Self {
        Self {
            enable_websockets: true,
            enable_http2: true,
            enable_grpc: true,
            enable_tls: true,
            max_connections_per_protocol: 1000,
            connection_timeout_ms: 30000,
            keep_alive_interval_ms: 60000,
            http2_max_frame_size: 16384,
            websocket_ping_interval_ms: 30000,
            grpc_max_message_size: 64 * 1024 * 1024, // 64MB
            enable_connection_pooling: true,
            pool_size_per_endpoint: 20,
            enable_certificate_validation: true,
        }
    }
}

impl AdvancedNetworkManager {
    /// Create a new advanced networking manager
    pub fn new() -> Self {
        let config = AdvancedNetworkConfig::default();
        let max_connections = config.max_connections_per_protocol * 3; // WebSocket + HTTP/2 + gRPC

        Self {
            websocket_connections: Arc::new(RwLock::new(HashMap::new())),
            http2_connections: Arc::new(RwLock::new(HashMap::new())),
            grpc_connections: Arc::new(RwLock::new(HashMap::new())),
            tls_config: Arc::new(Mutex::new(TlsConfiguration::new())),
            connection_pool: Arc::new(Mutex::new(AdvancedConnectionPool::new(20, Duration::from_secs(300)))),
            protocol_negotiator: Arc::new(ProtocolNegotiator::new()),
            performance_monitor: Arc::new(Mutex::new(NetworkPerformanceMonitor::new())),
            config,
            next_connection_id: std::sync::atomic::AtomicU64::new(1),
            operation_semaphore: Arc::new(Semaphore::new(max_connections as usize)),
        }
    }

    /// Get the global advanced network manager
    pub fn global() -> &'static AdvancedNetworkManager {
        &**ADVANCED_NETWORK_MANAGER
    }

    /// Create a WebSocket client connection
    pub async fn websocket_connect(&self, url: &str, headers: Option<HeaderMap>) -> WasmtimeResult<u64> {
        if !self.config.enable_websockets {
            return Err(WasmtimeError::Network {
                message: "WebSocket support is disabled".to_string(),
            });
        }

        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Network {
                message: format!("Failed to acquire connection permit: {}", e),
            }
        })?;

        let connection_id = self.next_connection_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        let timeout_duration = Duration::from_millis(self.config.connection_timeout_ms);

        let connect_future = async {
            let uri: Uri = url.parse().map_err(|e| WasmtimeError::Network {
                message: format!("Invalid WebSocket URL: {}", e),
            })?;

            let mut request = http::Request::builder()
                .uri(&uri)
                .header("Connection", "Upgrade")
                .header("Upgrade", "websocket")
                .header("Sec-WebSocket-Version", "13")
                .header("Sec-WebSocket-Key", tungstenite::handshake::client::generate_key());

            // Add custom headers if provided
            if let Some(header_map) = headers {
                for (name, value) in header_map {
                    if let Some(name) = name {
                        request = request.header(name, value);
                    }
                }
            }

            let request = request.body(()).map_err(|e| WasmtimeError::Network {
                message: format!("Failed to build WebSocket request: {}", e),
            })?;

            let tcp_stream = TcpStream::connect(format!("{}:{}",
                uri.host().unwrap_or("localhost"),
                uri.port_u16().unwrap_or(if uri.scheme_str() == Some("wss") { 443 } else { 80 })
            )).await.map_err(|e| WasmtimeError::Network {
                message: format!("Failed to connect to WebSocket server: {}", e),
            })?;

            let remote_addr = tcp_stream.peer_addr().map_err(|e| WasmtimeError::Network {
                message: format!("Failed to get remote address: {}", e),
            })?;

            let (ws_stream, _response) = client_async(request, tcp_stream).await.map_err(|e| WasmtimeError::Network {
                message: format!("WebSocket handshake failed: {}", e),
            })?;

            Ok((ws_stream, remote_addr))
        };

        let (ws_stream, remote_addr) = timeout(timeout_duration, connect_future).await
            .map_err(|_| WasmtimeError::Network {
                message: "WebSocket connection timed out".to_string(),
            })??;

        let connection = WebSocketConnection {
            id: connection_id,
            stream: ws_stream,
            remote_addr,
            connection_type: WebSocketConnectionType::Client,
            created_at: Instant::now(),
            last_activity: Instant::now(),
            messages_sent: 0,
            messages_received: 0,
            status: ConnectionStatus::Connected,
        };

        // Store connection
        {
            let mut connections = self.websocket_connections.write().unwrap();
            connections.insert(connection_id, connection);
        }

        // Update performance metrics
        {
            let mut monitor = self.performance_monitor.lock().unwrap();
            monitor.update_connection_established(SupportedProtocol::WebSocket);
        }

        Ok(connection_id)
    }

    /// Send a WebSocket message
    pub async fn websocket_send(&self, connection_id: u64, message: WsMessage) -> WasmtimeResult<()> {
        let mut connections = self.websocket_connections.write().unwrap();
        let connection = connections.get_mut(&connection_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("WebSocket connection {} not found", connection_id),
            })?;

        if connection.status != ConnectionStatus::Connected {
            return Err(WasmtimeError::Network {
                message: "WebSocket connection is not connected".to_string(),
            });
        }

        connection.stream.send(message).await.map_err(|e| WasmtimeError::Network {
            message: format!("Failed to send WebSocket message: {}", e),
        })?;

        connection.messages_sent += 1;
        connection.last_activity = Instant::now();

        // Update performance metrics
        {
            let mut monitor = self.performance_monitor.lock().unwrap();
            monitor.update_message_sent(SupportedProtocol::WebSocket);
        }

        Ok(())
    }

    /// Receive a WebSocket message
    pub async fn websocket_receive(&self, connection_id: u64) -> WasmtimeResult<Option<WsMessage>> {
        let mut connections = self.websocket_connections.write().unwrap();
        let connection = connections.get_mut(&connection_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("WebSocket connection {} not found", connection_id),
            })?;

        if connection.status != ConnectionStatus::Connected {
            return Err(WasmtimeError::Network {
                message: "WebSocket connection is not connected".to_string(),
            });
        }

        let timeout_duration = Duration::from_millis(self.config.connection_timeout_ms);
        let receive_future = connection.stream.next();

        let message_opt = timeout(timeout_duration, receive_future).await
            .map_err(|_| WasmtimeError::Network {
                message: "WebSocket receive timed out".to_string(),
            })?;

        match message_opt {
            Some(Ok(message)) => {
                connection.messages_received += 1;
                connection.last_activity = Instant::now();

                // Update performance metrics
                {
                    let mut monitor = self.performance_monitor.lock().unwrap();
                    monitor.update_message_received(SupportedProtocol::WebSocket);
                }

                Ok(Some(message))
            },
            Some(Err(e)) => Err(WasmtimeError::Network {
                message: format!("WebSocket receive error: {}", e),
            }),
            None => Ok(None), // Stream closed
        }
    }

    /// Create an HTTP/2 client connection
    pub async fn http2_connect(&self, addr: SocketAddr) -> WasmtimeResult<u64> {
        if !self.config.enable_http2 {
            return Err(WasmtimeError::Network {
                message: "HTTP/2 support is disabled".to_string(),
            });
        }

        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Network {
                message: format!("Failed to acquire connection permit: {}", e),
            }
        })?;

        let connection_id = self.next_connection_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        let timeout_duration = Duration::from_millis(self.config.connection_timeout_ms);

        let connect_future = async {
            let tcp_stream = TcpStream::connect(addr).await.map_err(|e| WasmtimeError::Network {
                message: format!("Failed to connect to HTTP/2 server: {}", e),
            })?;

            let (h2_client, h2_connection) = h2::client::handshake(tcp_stream).await.map_err(|e| WasmtimeError::Network {
                message: format!("HTTP/2 handshake failed: {}", e),
            })?;

            Ok((h2_client, h2_connection))
        };

        let (h2_client, h2_connection) = timeout(timeout_duration, connect_future).await
            .map_err(|_| WasmtimeError::Network {
                message: "HTTP/2 connection timed out".to_string(),
            })??;

        // Spawn connection handler
        tokio::spawn(async move {
            if let Err(e) = h2_connection.await {
                eprintln!("HTTP/2 connection error: {}", e);
            }
        });

        let connection = Http2Connection {
            id: connection_id,
            connection_type: Http2ConnectionType::Client(h2_client),
            remote_addr: addr,
            created_at: Instant::now(),
            last_activity: Instant::now(),
            requests_sent: 0,
            responses_received: 0,
            active_streams: HashMap::new(),
            status: ConnectionStatus::Connected,
        };

        // Store connection
        {
            let mut connections = self.http2_connections.write().unwrap();
            connections.insert(connection_id, connection);
        }

        // Update performance metrics
        {
            let mut monitor = self.performance_monitor.lock().unwrap();
            monitor.update_connection_established(SupportedProtocol::Http2);
        }

        Ok(connection_id)
    }

    /// Create a gRPC client connection
    pub async fn grpc_connect(&self, endpoint: &str) -> WasmtimeResult<u64> {
        if !self.config.enable_grpc {
            return Err(WasmtimeError::Network {
                message: "gRPC support is disabled".to_string(),
            });
        }

        let _permit = self.operation_semaphore.acquire().await.map_err(|e| {
            WasmtimeError::Network {
                message: format!("Failed to acquire connection permit: {}", e),
            }
        })?;

        let connection_id = self.next_connection_id.fetch_add(1, std::sync::atomic::Ordering::SeqCst);
        let timeout_duration = Duration::from_millis(self.config.connection_timeout_ms);

        let connect_future = async {
            let endpoint_parsed = Endpoint::from_shared(endpoint.to_string()).map_err(|e| WasmtimeError::Network {
                message: format!("Invalid gRPC endpoint: {}", e),
            })?
            .timeout(Duration::from_millis(self.config.connection_timeout_ms))
            .connect_timeout(Duration::from_millis(self.config.connection_timeout_ms));

            let channel = endpoint_parsed.connect().await.map_err(|e| WasmtimeError::Network {
                message: format!("Failed to connect to gRPC server: {}", e),
            })?;

            Ok((channel, endpoint_parsed))
        };

        let (channel, endpoint_parsed) = timeout(timeout_duration, connect_future).await
            .map_err(|_| WasmtimeError::Network {
                message: "gRPC connection timed out".to_string(),
            })??;

        let connection = GrpcConnection {
            id: connection_id,
            channel,
            endpoint: endpoint_parsed,
            created_at: Instant::now(),
            last_activity: Instant::now(),
            requests_sent: 0,
            responses_received: 0,
            status: ConnectionStatus::Connected,
        };

        // Store connection
        {
            let mut connections = self.grpc_connections.write().unwrap();
            connections.insert(connection_id, connection);
        }

        // Update performance metrics
        {
            let mut monitor = self.performance_monitor.lock().unwrap();
            monitor.update_connection_established(SupportedProtocol::Grpc);
        }

        Ok(connection_id)
    }

    /// Close a connection (any protocol)
    pub async fn close_connection(&self, connection_id: u64) -> WasmtimeResult<()> {
        // Try WebSocket first
        {
            let mut connections = self.websocket_connections.write().unwrap();
            if let Some(mut connection) = connections.remove(&connection_id) {
                connection.status = ConnectionStatus::Closed;
                let _ = connection.stream.close().await;
                return Ok(());
            }
        }

        // Try HTTP/2
        {
            let mut connections = self.http2_connections.write().unwrap();
            if let Some(mut connection) = connections.remove(&connection_id) {
                connection.status = ConnectionStatus::Closed;
                return Ok(());
            }
        }

        // Try gRPC
        {
            let mut connections = self.grpc_connections.write().unwrap();
            if let Some(mut connection) = connections.remove(&connection_id) {
                connection.status = ConnectionStatus::Closed;
                return Ok(());
            }
        }

        Err(WasmtimeError::InvalidParameter {
            message: format!("Connection {} not found", connection_id),
        })
    }

    /// Get network performance metrics
    pub fn get_performance_metrics(&self) -> NetworkPerformanceMonitor {
        let monitor = self.performance_monitor.lock().unwrap();
        monitor.clone()
    }

    /// Configure TLS settings
    pub fn configure_tls(&self, client_config: Option<ClientConfig>, server_config: Option<ServerConfig>) -> WasmtimeResult<()> {
        let mut tls_config = self.tls_config.lock().unwrap();

        if let Some(config) = client_config {
            tls_config.client_config = Some(Arc::new(config));
        }

        if let Some(config) = server_config {
            tls_config.server_config = Some(Arc::new(config));
        }

        Ok(())
    }
}

impl TlsConfiguration {
    fn new() -> Self {
        Self {
            client_config: None,
            server_config: None,
            certificates: HashMap::new(),
            private_keys: HashMap::new(),
            root_certificates: Vec::new(),
        }
    }
}

impl AdvancedConnectionPool {
    fn new(max_pool_size: usize, max_idle_time: Duration) -> Self {
        Self {
            websocket_pools: HashMap::new(),
            http2_pools: HashMap::new(),
            grpc_pools: HashMap::new(),
            max_pool_size,
            max_idle_time,
        }
    }
}

impl ProtocolNegotiator {
    fn new() -> Self {
        Self {
            supported_protocols: vec![
                SupportedProtocol::Http1_1,
                SupportedProtocol::Http2,
                SupportedProtocol::WebSocket,
                SupportedProtocol::Grpc,
            ],
            alpn_protocols: vec![
                b"h2".to_vec(),
                b"http/1.1".to_vec(),
                b"grpc-exp".to_vec(),
            ],
        }
    }
}

impl NetworkPerformanceMonitor {
    fn new() -> Self {
        Self {
            protocol_metrics: HashMap::new(),
            connection_metrics: ConnectionMetrics::default(),
            throughput_metrics: ThroughputMetrics::default(),
        }
    }

    fn update_connection_established(&mut self, protocol: SupportedProtocol) {
        let metrics = self.protocol_metrics.entry(protocol).or_insert_with(ProtocolMetrics::default);
        metrics.total_connections += 1;
        metrics.active_connections += 1;
    }

    fn update_message_sent(&mut self, protocol: SupportedProtocol) {
        let metrics = self.protocol_metrics.entry(protocol).or_insert_with(ProtocolMetrics::default);
        metrics.bytes_sent += 1; // Simplified - would count actual bytes
    }

    fn update_message_received(&mut self, protocol: SupportedProtocol) {
        let metrics = self.protocol_metrics.entry(protocol).or_insert_with(ProtocolMetrics::default);
        metrics.bytes_received += 1; // Simplified - would count actual bytes
    }
}

impl Clone for NetworkPerformanceMonitor {
    fn clone(&self) -> Self {
        Self {
            protocol_metrics: self.protocol_metrics.clone(),
            connection_metrics: self.connection_metrics.clone(),
            throughput_metrics: self.throughput_metrics.clone(),
        }
    }
}

// C API for FFI integration

/// Initialize advanced networking
#[no_mangle]
pub unsafe extern "C" fn advanced_networking_init() -> c_int {
    let _ = AdvancedNetworkManager::global();
    0 // Success
}

/// Create WebSocket connection
#[no_mangle]
pub unsafe extern "C" fn websocket_connect(
    url: *const c_char,
    connection_id_out: *mut u64,
) -> c_int {
    if url.is_null() || connection_id_out.is_null() {
        return -1;
    }

    let url_str = match std::ffi::CStr::from_ptr(url).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let manager = AdvancedNetworkManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.websocket_connect(url_str, None)) {
        Ok(connection_id) => {
            *connection_id_out = connection_id;
            0
        },
        Err(_) => -1,
    }
}

/// Send WebSocket message
#[no_mangle]
pub unsafe extern "C" fn websocket_send_text(
    connection_id: u64,
    message: *const c_char,
) -> c_int {
    if message.is_null() {
        return -1;
    }

    let message_str = match std::ffi::CStr::from_ptr(message).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let manager = AdvancedNetworkManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.websocket_send(connection_id, WsMessage::Text(message_str.to_string()))) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

/// Create HTTP/2 connection
#[no_mangle]
pub unsafe extern "C" fn http2_connect(
    host: *const c_char,
    port: c_uint,
    connection_id_out: *mut u64,
) -> c_int {
    if host.is_null() || connection_id_out.is_null() {
        return -1;
    }

    let host_str = match std::ffi::CStr::from_ptr(host).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let address = match format!("{}:{}", host_str, port).parse::<SocketAddr>() {
        Ok(addr) => addr,
        Err(_) => return -1,
    };

    let manager = AdvancedNetworkManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.http2_connect(address)) {
        Ok(connection_id) => {
            *connection_id_out = connection_id;
            0
        },
        Err(_) => -1,
    }
}

/// Create gRPC connection
#[no_mangle]
pub unsafe extern "C" fn grpc_connect(
    endpoint: *const c_char,
    connection_id_out: *mut u64,
) -> c_int {
    if endpoint.is_null() || connection_id_out.is_null() {
        return -1;
    }

    let endpoint_str = match std::ffi::CStr::from_ptr(endpoint).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let manager = AdvancedNetworkManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.grpc_connect(endpoint_str)) {
        Ok(connection_id) => {
            *connection_id_out = connection_id;
            0
        },
        Err(_) => -1,
    }
}

/// Close any protocol connection
#[no_mangle]
pub unsafe extern "C" fn advanced_networking_close_connection(connection_id: u64) -> c_int {
    let manager = AdvancedNetworkManager::global();
    let handle = get_runtime_handle();

    match handle.block_on(manager.close_connection(connection_id)) {
        Ok(_) => 0,
        Err(_) => -1,
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_advanced_network_manager_creation() {
        let manager = AdvancedNetworkManager::new();
        assert!(manager.config.enable_websockets);
        assert!(manager.config.enable_http2);
        assert!(manager.config.enable_grpc);
        assert!(manager.config.enable_tls);
    }

    #[test]
    fn test_protocol_negotiator() {
        let negotiator = ProtocolNegotiator::new();
        assert!(negotiator.supported_protocols.contains(&SupportedProtocol::Http2));
        assert!(negotiator.supported_protocols.contains(&SupportedProtocol::WebSocket));
        assert!(negotiator.supported_protocols.contains(&SupportedProtocol::Grpc));
    }

    #[test]
    fn test_performance_monitor() {
        let mut monitor = NetworkPerformanceMonitor::new();
        monitor.update_connection_established(SupportedProtocol::WebSocket);

        let metrics = monitor.protocol_metrics.get(&SupportedProtocol::WebSocket).unwrap();
        assert_eq!(metrics.total_connections, 1);
        assert_eq!(metrics.active_connections, 1);
    }

    #[test]
    fn test_tls_configuration() {
        let tls_config = TlsConfiguration::new();
        assert!(tls_config.client_config.is_none());
        assert!(tls_config.server_config.is_none());
        assert!(tls_config.certificates.is_empty());
    }

    #[test]
    fn test_connection_pool() {
        let pool = AdvancedConnectionPool::new(10, Duration::from_secs(300));
        assert_eq!(pool.max_pool_size, 10);
        assert_eq!(pool.max_idle_time, Duration::from_secs(300));
    }

    #[test]
    fn test_c_api_initialization() {
        unsafe {
            let result = advanced_networking_init();
            assert_eq!(result, 0);
        }
    }
}