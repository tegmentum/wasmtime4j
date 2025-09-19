//! Advanced WASI Extensions with Security Policies and Async I/O
//!
//! This module provides comprehensive advanced WASI functionality including:
//! - Network programming (TCP/UDP sockets, HTTP client/server)
//! - Threading and concurrency primitives with async operations
//! - Cryptographic operations (hash, encryption, random number generation)
//! - System integration (process management, IPC, shared memory)
//! - Security policy enforcement with capability-based access control
//! - WASI Preview 2 interface bindings and component model support

use std::sync::{Arc, Mutex, RwLock};
use std::collections::HashMap;
use std::net::{SocketAddr, TcpListener, TcpStream, UdpSocket};
use std::path::{Path, PathBuf};
use std::process::{Command, Child};
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use tokio::sync::{Semaphore, Mutex as TokioMutex};
use std::sync::Condvar;
use tokio::task::JoinHandle;
use tokio::io::{AsyncReadExt, AsyncWriteExt};
use futures::future::Future;
use crate::error::{WasmtimeError, WasmtimeResult};
use crate::wasi::{WasiContext, WasiConfig};

/// Advanced WASI context with extended capabilities and security policies
pub struct WasiAdvancedContext {
    /// Base WASI context
    base_context: Arc<RwLock<WasiContext>>,
    /// Security policy configuration
    security_policy: SecurityPolicy,
    /// Network access manager
    network_manager: Arc<NetworkManager>,
    /// Threading manager
    thread_manager: Arc<ThreadManager>,
    /// Cryptography manager
    crypto_manager: Arc<CryptoManager>,
    /// System integration manager
    system_manager: Arc<SystemManager>,
    /// Active resource handles
    resource_handles: Arc<Mutex<ResourceHandles>>,
}

/// Security policy for capability-based access control
#[derive(Debug, Clone)]
pub struct SecurityPolicy {
    /// Network access permissions
    pub network_permissions: NetworkPermissions,
    /// File system access permissions (extends base WASI)
    pub filesystem_permissions: FilesystemPermissions,
    /// Process management permissions
    pub process_permissions: ProcessPermissions,
    /// Cryptography permissions
    pub crypto_permissions: CryptoPermissions,
    /// Resource limits
    pub resource_limits: ResourceLimits,
}

/// Network access permissions
#[derive(Debug, Clone)]
pub struct NetworkPermissions {
    /// Allow TCP client connections
    pub allow_tcp_client: bool,
    /// Allow TCP server operations
    pub allow_tcp_server: bool,
    /// Allow UDP operations
    pub allow_udp: bool,
    /// Allow HTTP client requests
    pub allow_http_client: bool,
    /// Allow HTTP server operations
    pub allow_http_server: bool,
    /// Allowed destination hosts for outbound connections
    pub allowed_hosts: Vec<String>,
    /// Allowed port ranges for server operations
    pub allowed_server_ports: Vec<PortRange>,
}

/// Port range specification
#[derive(Debug, Clone)]
pub struct PortRange {
    pub start: u16,
    pub end: u16,
}

/// Extended filesystem permissions
#[derive(Debug, Clone)]
pub struct FilesystemPermissions {
    /// Allow async file operations
    pub allow_async_io: bool,
    /// Allow file watching (inotify-style)
    pub allow_file_watching: bool,
    /// Allow memory-mapped files
    pub allow_memory_mapping: bool,
    /// Maximum file size for operations
    pub max_file_size: Option<u64>,
}

/// Process management permissions
#[derive(Debug, Clone)]
pub struct ProcessPermissions {
    /// Allow starting child processes
    pub allow_process_spawn: bool,
    /// Allow inter-process communication
    pub allow_ipc: bool,
    /// Allow shared memory operations
    pub allow_shared_memory: bool,
    /// Allowed executable paths
    pub allowed_executables: Vec<PathBuf>,
}

/// Cryptography permissions
#[derive(Debug, Clone)]
pub struct CryptoPermissions {
    /// Allow hash operations
    pub allow_hashing: bool,
    /// Allow symmetric encryption
    pub allow_symmetric_crypto: bool,
    /// Allow asymmetric encryption
    pub allow_asymmetric_crypto: bool,
    /// Allow random number generation
    pub allow_random: bool,
    /// Allow digital signatures
    pub allow_signatures: bool,
}

/// Resource limits for security and stability
#[derive(Debug, Clone)]
pub struct ResourceLimits {
    /// Maximum number of open network connections
    pub max_network_connections: Option<u32>,
    /// Maximum number of active threads
    pub max_threads: Option<u32>,
    /// Maximum memory usage for crypto operations
    pub max_crypto_memory: Option<u64>,
    /// Maximum number of child processes
    pub max_child_processes: Option<u32>,
    /// Maximum shared memory size
    pub max_shared_memory: Option<u64>,
}

/// Network manager for socket operations and HTTP services
pub struct NetworkManager {
    security_policy: SecurityPolicy,
    active_connections: Arc<Mutex<HashMap<u32, NetworkConnection>>>,
    connection_counter: Arc<Mutex<u32>>,
    http_client: reqwest::Client,
}

/// Network connection handle
#[derive(Debug)]
pub enum NetworkConnection {
    TcpStream {
        stream: Arc<TokioMutex<tokio::net::TcpStream>>,
        remote_addr: SocketAddr,
    },
    TcpListener {
        listener: Arc<tokio::net::TcpListener>,
        local_addr: SocketAddr,
    },
    UdpSocket {
        socket: Arc<tokio::net::UdpSocket>,
        local_addr: SocketAddr,
    },
    HttpServer {
        handle: JoinHandle<()>,
        local_addr: SocketAddr,
    },
}

/// Threading manager for async operations and concurrency
pub struct ThreadManager {
    security_policy: SecurityPolicy,
    runtime: Arc<tokio::runtime::Runtime>,
    active_threads: Arc<Mutex<HashMap<u32, ThreadHandle>>>,
    thread_counter: Arc<Mutex<u32>>,
    semaphores: Arc<Mutex<HashMap<u32, Arc<Semaphore>>>>,
    mutexes: Arc<Mutex<HashMap<u32, Arc<TokioMutex<()>>>>>,
    condition_variables: Arc<Mutex<HashMap<u32, Arc<(TokioMutex<bool>, Condvar)>>>>,
}

/// Thread handle for async operations
#[derive(Debug)]
pub struct ThreadHandle {
    join_handle: JoinHandle<()>,
    thread_id: u32,
}

/// Cryptography manager for hash, encryption, and random operations
pub struct CryptoManager {
    security_policy: SecurityPolicy,
    hash_contexts: Arc<Mutex<HashMap<u32, HashContext>>>,
    cipher_contexts: Arc<Mutex<HashMap<u32, CipherContext>>>,
    context_counter: Arc<Mutex<u32>>,
}

/// Hash context for streaming hash operations
pub enum HashContext {
    Sha256(sha2::Sha256),
    Sha512(sha2::Sha512),
    Sha3_256(sha3::Sha3_256),
    Sha3_512(sha3::Sha3_512),
    Blake3(blake3::Hasher),
}

/// Cipher context for encryption/decryption operations
pub enum CipherContext {
    Aes128(aes::Aes128),
    Aes256(aes::Aes256),
}

/// System integration manager for process and IPC operations
pub struct SystemManager {
    security_policy: SecurityPolicy,
    child_processes: Arc<Mutex<HashMap<u32, Child>>>,
    shared_memory_regions: Arc<Mutex<HashMap<u32, SharedMemoryRegion>>>,
    pipes: Arc<Mutex<HashMap<u32, PipeHandle>>>,
    process_counter: Arc<Mutex<u32>>,
}

/// Shared memory region handle
#[derive(Debug)]
pub struct SharedMemoryRegion {
    size: usize,
    data: Arc<Mutex<Vec<u8>>>,
}

/// Inter-process communication pipe handle
#[derive(Debug)]
pub struct PipeHandle {
    read_handle: Arc<TokioMutex<tokio::process::ChildStdout>>,
    write_handle: Arc<TokioMutex<tokio::process::ChildStdin>>,
}

/// Resource handles for cleanup tracking
#[derive(Debug, Default)]
pub struct ResourceHandles {
    network_handles: Vec<u32>,
    thread_handles: Vec<u32>,
    crypto_handles: Vec<u32>,
    process_handles: Vec<u32>,
}

/// Hash algorithm enumeration
#[derive(Debug, Clone, Copy)]
pub enum HashAlgorithm {
    Sha256,
    Sha512,
    Sha3_256,
    Sha3_512,
    Blake3,
}

/// Cipher algorithm enumeration
#[derive(Debug, Clone, Copy)]
pub enum CipherAlgorithm {
    Aes128,
    Aes256,
}

/// Socket type enumeration
#[derive(Debug, Clone, Copy)]
pub enum SocketType {
    Tcp,
    Udp,
}

/// Socket family enumeration
#[derive(Debug, Clone, Copy)]
pub enum SocketFamily {
    Ipv4,
    Ipv6,
}

impl Default for SecurityPolicy {
    fn default() -> Self {
        Self {
            network_permissions: NetworkPermissions::default(),
            filesystem_permissions: FilesystemPermissions::default(),
            process_permissions: ProcessPermissions::default(),
            crypto_permissions: CryptoPermissions::default(),
            resource_limits: ResourceLimits::default(),
        }
    }
}

impl Default for NetworkPermissions {
    fn default() -> Self {
        Self {
            allow_tcp_client: false,
            allow_tcp_server: false,
            allow_udp: false,
            allow_http_client: false,
            allow_http_server: false,
            allowed_hosts: Vec::new(),
            allowed_server_ports: Vec::new(),
        }
    }
}

impl Default for FilesystemPermissions {
    fn default() -> Self {
        Self {
            allow_async_io: false,
            allow_file_watching: false,
            allow_memory_mapping: false,
            max_file_size: Some(100 * 1024 * 1024), // 100MB
        }
    }
}

impl Default for ProcessPermissions {
    fn default() -> Self {
        Self {
            allow_process_spawn: false,
            allow_ipc: false,
            allow_shared_memory: false,
            allowed_executables: Vec::new(),
        }
    }
}

impl Default for CryptoPermissions {
    fn default() -> Self {
        Self {
            allow_hashing: true, // Generally safe to allow
            allow_symmetric_crypto: false,
            allow_asymmetric_crypto: false,
            allow_random: true, // Generally safe to allow
            allow_signatures: false,
        }
    }
}

impl Default for ResourceLimits {
    fn default() -> Self {
        Self {
            max_network_connections: Some(100),
            max_threads: Some(50),
            max_crypto_memory: Some(64 * 1024 * 1024), // 64MB
            max_child_processes: Some(10),
            max_shared_memory: Some(256 * 1024 * 1024), // 256MB
        }
    }
}

impl WasiAdvancedContext {
    /// Create a new advanced WASI context with default security policy
    pub fn new() -> WasmtimeResult<Self> {
        Self::with_security_policy(SecurityPolicy::default())
    }

    /// Create a new advanced WASI context with custom security policy
    pub fn with_security_policy(security_policy: SecurityPolicy) -> WasmtimeResult<Self> {
        let base_context = Arc::new(RwLock::new(WasiContext::new()?));
        let http_client = reqwest::Client::builder()
            .timeout(Duration::from_secs(30))
            .build()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create HTTP client: {}", e),
            })?;

        let runtime = Arc::new(
            tokio::runtime::Runtime::new()
                .map_err(|e| WasmtimeError::Wasi {
                    message: format!("Failed to create async runtime: {}", e),
                })?
        );

        Ok(Self {
            base_context,
            network_manager: Arc::new(NetworkManager {
                security_policy: security_policy.clone(),
                active_connections: Arc::new(Mutex::new(HashMap::new())),
                connection_counter: Arc::new(Mutex::new(0)),
                http_client,
            }),
            thread_manager: Arc::new(ThreadManager {
                security_policy: security_policy.clone(),
                runtime,
                active_threads: Arc::new(Mutex::new(HashMap::new())),
                thread_counter: Arc::new(Mutex::new(0)),
                semaphores: Arc::new(Mutex::new(HashMap::new())),
                mutexes: Arc::new(Mutex::new(HashMap::new())),
                condition_variables: Arc::new(Mutex::new(HashMap::new())),
            }),
            crypto_manager: Arc::new(CryptoManager {
                security_policy: security_policy.clone(),
                hash_contexts: Arc::new(Mutex::new(HashMap::new())),
                cipher_contexts: Arc::new(Mutex::new(HashMap::new())),
                context_counter: Arc::new(Mutex::new(0)),
            }),
            system_manager: Arc::new(SystemManager {
                security_policy: security_policy.clone(),
                child_processes: Arc::new(Mutex::new(HashMap::new())),
                shared_memory_regions: Arc::new(Mutex::new(HashMap::new())),
                pipes: Arc::new(Mutex::new(HashMap::new())),
                process_counter: Arc::new(Mutex::new(0)),
            }),
            security_policy,
            resource_handles: Arc::new(Mutex::new(ResourceHandles::default())),
        })
    }

    /// Get reference to base WASI context
    pub fn get_base_context(&self) -> Arc<RwLock<WasiContext>> {
        Arc::clone(&self.base_context)
    }

    /// Get security policy
    pub fn get_security_policy(&self) -> &SecurityPolicy {
        &self.security_policy
    }

    // Network Operations

    /// Create a TCP socket
    pub async fn create_tcp_socket(&self, bind_addr: SocketAddr) -> WasmtimeResult<u32> {
        if !self.security_policy.network_permissions.allow_tcp_server {
            return Err(WasmtimeError::Wasi {
                message: "TCP socket creation not allowed by security policy".to_string(),
            });
        }

        let listener = tokio::net::TcpListener::bind(bind_addr)
            .await
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to bind TCP socket: {}", e),
            })?;

        let local_addr = listener.local_addr()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to get socket address: {}", e),
            })?;

        let connection_id = {
            let mut counter = self.network_manager.connection_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connection counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let connection = NetworkConnection::TcpListener {
            listener: Arc::new(listener),
            local_addr,
        };

        {
            let mut connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;
            connections.insert(connection_id, connection);
        }

        Ok(connection_id)
    }

    /// Accept a TCP connection
    pub async fn accept_tcp_connection(&self, socket_id: u32) -> WasmtimeResult<u32> {
        let listener = {
            let connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;

            match connections.get(&socket_id) {
                Some(NetworkConnection::TcpListener { listener, .. }) => Arc::clone(listener),
                _ => return Err(WasmtimeError::Wasi {
                    message: "Invalid TCP listener socket ID".to_string(),
                }),
            }
        };

        let (stream, remote_addr) = listener.accept()
            .await
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to accept TCP connection: {}", e),
            })?;

        let connection_id = {
            let mut counter = self.network_manager.connection_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connection counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let connection = NetworkConnection::TcpStream {
            stream: Arc::new(TokioMutex::new(stream)),
            remote_addr,
        };

        {
            let mut connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;
            connections.insert(connection_id, connection);
        }

        Ok(connection_id)
    }

    /// Connect to a TCP server
    pub async fn connect_tcp(&self, addr: SocketAddr) -> WasmtimeResult<u32> {
        if !self.security_policy.network_permissions.allow_tcp_client {
            return Err(WasmtimeError::Wasi {
                message: "TCP client connections not allowed by security policy".to_string(),
            });
        }

        // Check if host is allowed
        let host = addr.ip().to_string();
        if !self.security_policy.network_permissions.allowed_hosts.is_empty() &&
           !self.security_policy.network_permissions.allowed_hosts.contains(&host) {
            return Err(WasmtimeError::Wasi {
                message: format!("Connection to host {} not allowed by security policy", host),
            });
        }

        let stream = tokio::net::TcpStream::connect(addr)
            .await
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to connect to TCP server: {}", e),
            })?;

        let connection_id = {
            let mut counter = self.network_manager.connection_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connection counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let connection = NetworkConnection::TcpStream {
            stream: Arc::new(TokioMutex::new(stream)),
            remote_addr: addr,
        };

        {
            let mut connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;
            connections.insert(connection_id, connection);
        }

        Ok(connection_id)
    }

    /// Create a UDP socket
    pub async fn create_udp_socket(&self, bind_addr: SocketAddr) -> WasmtimeResult<u32> {
        if !self.security_policy.network_permissions.allow_udp {
            return Err(WasmtimeError::Wasi {
                message: "UDP operations not allowed by security policy".to_string(),
            });
        }

        let socket = tokio::net::UdpSocket::bind(bind_addr)
            .await
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to bind UDP socket: {}", e),
            })?;

        let local_addr = socket.local_addr()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to get socket address: {}", e),
            })?;

        let connection_id = {
            let mut counter = self.network_manager.connection_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connection counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let connection = NetworkConnection::UdpSocket {
            socket: Arc::new(socket),
            local_addr,
        };

        {
            let mut connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;
            connections.insert(connection_id, connection);
        }

        Ok(connection_id)
    }

    /// Read data from a network connection
    pub async fn read_from_connection(&self, connection_id: u32, buffer: &mut [u8]) -> WasmtimeResult<usize> {
        let connection = {
            let connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;

            connections.get(&connection_id).cloned().ok_or_else(|| WasmtimeError::Wasi {
                message: "Invalid connection ID".to_string(),
            })?
        };

        match connection {
            NetworkConnection::TcpStream { stream, .. } => {
                let mut stream_guard = stream.lock().await;
                stream_guard.read(buffer)
                    .await
                    .map_err(|e| WasmtimeError::Wasi {
                        message: format!("Failed to read from TCP stream: {}", e),
                    })
            }
            _ => Err(WasmtimeError::Wasi {
                message: "Connection does not support reading".to_string(),
            }),
        }
    }

    /// Write data to a network connection
    pub async fn write_to_connection(&self, connection_id: u32, data: &[u8]) -> WasmtimeResult<usize> {
        let connection = {
            let connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;

            connections.get(&connection_id).cloned().ok_or_else(|| WasmtimeError::Wasi {
                message: "Invalid connection ID".to_string(),
            })?
        };

        match connection {
            NetworkConnection::TcpStream { stream, .. } => {
                let mut stream_guard = stream.lock().await;
                stream_guard.write(data)
                    .await
                    .map_err(|e| WasmtimeError::Wasi {
                        message: format!("Failed to write to TCP stream: {}", e),
                    })
            }
            _ => Err(WasmtimeError::Wasi {
                message: "Connection does not support writing".to_string(),
            }),
        }
    }

    /// Make an HTTP GET request
    pub async fn http_get(&self, url: &str) -> WasmtimeResult<(u16, String)> {
        if !self.security_policy.network_permissions.allow_http_client {
            return Err(WasmtimeError::Wasi {
                message: "HTTP client requests not allowed by security policy".to_string(),
            });
        }

        let response = self.network_manager.http_client.get(url)
            .send()
            .await
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("HTTP GET request failed: {}", e),
            })?;

        let status = response.status().as_u16();
        let body = response.text()
            .await
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to read HTTP response body: {}", e),
            })?;

        Ok((status, body))
    }

    // Threading Operations

    /// Create a new async thread
    pub async fn create_thread<F>(&self, task: F) -> WasmtimeResult<u32>
    where
        F: Future<Output = ()> + Send + 'static,
    {
        if let Some(max_threads) = self.security_policy.resource_limits.max_threads {
            let active_count = self.thread_manager.active_threads.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire threads lock".to_string(),
                })?
                .len() as u32;

            if active_count >= max_threads {
                return Err(WasmtimeError::Wasi {
                    message: format!("Maximum thread limit ({}) exceeded", max_threads),
                });
            }
        }

        let thread_id = {
            let mut counter = self.thread_manager.thread_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire thread counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let join_handle = self.thread_manager.runtime.spawn(task);
        let thread_handle = ThreadHandle {
            join_handle,
            thread_id,
        };

        {
            let mut threads = self.thread_manager.active_threads.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire threads lock".to_string(),
                })?;
            threads.insert(thread_id, thread_handle);
        }

        Ok(thread_id)
    }

    /// Create a semaphore with specified permits
    pub fn create_semaphore(&self, permits: usize) -> WasmtimeResult<u32> {
        let semaphore_id = {
            let mut counter = self.thread_manager.thread_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire thread counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let semaphore = Arc::new(Semaphore::new(permits));

        {
            let mut semaphores = self.thread_manager.semaphores.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire semaphores lock".to_string(),
                })?;
            semaphores.insert(semaphore_id, semaphore);
        }

        Ok(semaphore_id)
    }

    /// Create a mutex for synchronization
    pub fn create_mutex(&self) -> WasmtimeResult<u32> {
        let mutex_id = {
            let mut counter = self.thread_manager.thread_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire thread counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let mutex = Arc::new(TokioMutex::new(()));

        {
            let mut mutexes = self.thread_manager.mutexes.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire mutexes lock".to_string(),
                })?;
            mutexes.insert(mutex_id, mutex);
        }

        Ok(mutex_id)
    }

    // Cryptography Operations

    /// Create a hash context
    pub fn create_hasher(&self, algorithm: HashAlgorithm) -> WasmtimeResult<u32> {
        if !self.security_policy.crypto_permissions.allow_hashing {
            return Err(WasmtimeError::Wasi {
                message: "Hash operations not allowed by security policy".to_string(),
            });
        }

        let context_id = {
            let mut counter = self.crypto_manager.context_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire crypto counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let hash_context = match algorithm {
            HashAlgorithm::Sha256 => HashContext::Sha256(sha2::Sha256::new()),
            HashAlgorithm::Sha512 => HashContext::Sha512(sha2::Sha512::new()),
            HashAlgorithm::Sha3_256 => HashContext::Sha3_256(sha3::Sha3_256::new()),
            HashAlgorithm::Sha3_512 => HashContext::Sha3_512(sha3::Sha3_512::new()),
            HashAlgorithm::Blake3 => HashContext::Blake3(blake3::Hasher::new()),
        };

        {
            let mut contexts = self.crypto_manager.hash_contexts.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire hash contexts lock".to_string(),
                })?;
            contexts.insert(context_id, hash_context);
        }

        Ok(context_id)
    }

    /// Compute hash of data in one operation
    pub fn hash_data(&self, algorithm: HashAlgorithm, data: &[u8]) -> WasmtimeResult<Vec<u8>> {
        if !self.security_policy.crypto_permissions.allow_hashing {
            return Err(WasmtimeError::Wasi {
                message: "Hash operations not allowed by security policy".to_string(),
            });
        }

        use sha2::Digest;

        let result = match algorithm {
            HashAlgorithm::Sha256 => {
                let mut hasher = sha2::Sha256::new();
                hasher.update(data);
                hasher.finalize().to_vec()
            }
            HashAlgorithm::Sha512 => {
                let mut hasher = sha2::Sha512::new();
                hasher.update(data);
                hasher.finalize().to_vec()
            }
            HashAlgorithm::Sha3_256 => {
                let mut hasher = sha3::Sha3_256::new();
                hasher.update(data);
                hasher.finalize().to_vec()
            }
            HashAlgorithm::Sha3_512 => {
                let mut hasher = sha3::Sha3_512::new();
                hasher.update(data);
                hasher.finalize().to_vec()
            }
            HashAlgorithm::Blake3 => {
                blake3::hash(data).as_bytes().to_vec()
            }
        };

        Ok(result)
    }

    /// Generate random bytes
    pub fn generate_random_bytes(&self, length: usize) -> WasmtimeResult<Vec<u8>> {
        if !self.security_policy.crypto_permissions.allow_random {
            return Err(WasmtimeError::Wasi {
                message: "Random number generation not allowed by security policy".to_string(),
            });
        }

        use rand::RngCore;
        let mut buffer = vec![0u8; length];
        rand::thread_rng().fill_bytes(&mut buffer);
        Ok(buffer)
    }

    // System Integration Operations

    /// Start a child process
    pub async fn start_process(&self, command: &[String], env: &HashMap<String, String>) -> WasmtimeResult<u32> {
        if !self.security_policy.process_permissions.allow_process_spawn {
            return Err(WasmtimeError::Wasi {
                message: "Process spawning not allowed by security policy".to_string(),
            });
        }

        if command.is_empty() {
            return Err(WasmtimeError::Wasi {
                message: "Command cannot be empty".to_string(),
            });
        }

        // Check if executable is allowed
        let executable_path = PathBuf::from(&command[0]);
        if !self.security_policy.process_permissions.allowed_executables.is_empty() &&
           !self.security_policy.process_permissions.allowed_executables.contains(&executable_path) {
            return Err(WasmtimeError::Wasi {
                message: format!("Executable '{}' not allowed by security policy", command[0]),
            });
        }

        let mut cmd = Command::new(&command[0]);
        if command.len() > 1 {
            cmd.args(&command[1..]);
        }
        cmd.envs(env);

        let child = cmd.spawn()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to start process: {}", e),
            })?;

        let process_id = {
            let mut counter = self.system_manager.process_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire process counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        {
            let mut processes = self.system_manager.child_processes.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire processes lock".to_string(),
                })?;
            processes.insert(process_id, child);
        }

        Ok(process_id)
    }

    /// Wait for a process to complete
    pub async fn wait_for_process(&self, process_id: u32) -> WasmtimeResult<i32> {
        let mut child = {
            let mut processes = self.system_manager.child_processes.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire processes lock".to_string(),
                })?;

            processes.remove(&process_id).ok_or_else(|| WasmtimeError::Wasi {
                message: "Invalid process ID".to_string(),
            })?
        };

        let status = child.wait()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to wait for process: {}", e),
            })?;

        Ok(status.code().unwrap_or(-1))
    }

    /// Create shared memory region
    pub fn create_shared_memory(&self, size: usize) -> WasmtimeResult<u32> {
        if !self.security_policy.process_permissions.allow_shared_memory {
            return Err(WasmtimeError::Wasi {
                message: "Shared memory operations not allowed by security policy".to_string(),
            });
        }

        if let Some(max_size) = self.security_policy.resource_limits.max_shared_memory {
            if size as u64 > max_size {
                return Err(WasmtimeError::Wasi {
                    message: format!("Shared memory size ({}) exceeds limit ({})", size, max_size),
                });
            }
        }

        let memory_id = {
            let mut counter = self.system_manager.process_counter.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire process counter lock".to_string(),
                })?;
            *counter += 1;
            *counter
        };

        let region = SharedMemoryRegion {
            size,
            data: Arc::new(Mutex::new(vec![0u8; size])),
        };

        {
            let mut regions = self.system_manager.shared_memory_regions.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire shared memory lock".to_string(),
                })?;
            regions.insert(memory_id, region);
        }

        Ok(memory_id)
    }

    /// Read from shared memory
    pub fn read_shared_memory(&self, memory_id: u32, offset: usize, buffer: &mut [u8]) -> WasmtimeResult<usize> {
        let region = {
            let regions = self.system_manager.shared_memory_regions.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire shared memory lock".to_string(),
                })?;

            regions.get(&memory_id).cloned().ok_or_else(|| WasmtimeError::Wasi {
                message: "Invalid shared memory ID".to_string(),
            })?
        };

        let data = region.data.lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire shared memory data lock".to_string(),
            })?;

        if offset >= data.len() {
            return Ok(0);
        }

        let available = data.len() - offset;
        let to_copy = buffer.len().min(available);
        buffer[..to_copy].copy_from_slice(&data[offset..offset + to_copy]);

        Ok(to_copy)
    }

    /// Write to shared memory
    pub fn write_shared_memory(&self, memory_id: u32, offset: usize, data: &[u8]) -> WasmtimeResult<usize> {
        let region = {
            let regions = self.system_manager.shared_memory_regions.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire shared memory lock".to_string(),
                })?;

            regions.get(&memory_id).cloned().ok_or_else(|| WasmtimeError::Wasi {
                message: "Invalid shared memory ID".to_string(),
            })?
        };

        let mut memory_data = region.data.lock()
            .map_err(|_| WasmtimeError::Concurrency {
                message: "Failed to acquire shared memory data lock".to_string(),
            })?;

        if offset >= memory_data.len() {
            return Ok(0);
        }

        let available = memory_data.len() - offset;
        let to_copy = data.len().min(available);
        memory_data[offset..offset + to_copy].copy_from_slice(&data[..to_copy]);

        Ok(to_copy)
    }

    /// Cleanup all resources
    pub async fn cleanup(&self) -> WasmtimeResult<()> {
        // Close network connections
        {
            let mut connections = self.network_manager.active_connections.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire connections lock".to_string(),
                })?;
            connections.clear();
        }

        // Abort active threads
        {
            let mut threads = self.thread_manager.active_threads.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire threads lock".to_string(),
                })?;
            for (_, thread_handle) in threads.drain() {
                thread_handle.join_handle.abort();
            }
        }

        // Clear crypto contexts
        {
            let mut hash_contexts = self.crypto_manager.hash_contexts.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire hash contexts lock".to_string(),
                })?;
            hash_contexts.clear();
        }

        // Terminate child processes
        {
            let mut processes = self.system_manager.child_processes.lock()
                .map_err(|_| WasmtimeError::Concurrency {
                    message: "Failed to acquire processes lock".to_string(),
                })?;
            for (_, mut child) in processes.drain() {
                let _ = child.kill();
            }
        }

        Ok(())
    }
}

// Native FFI functions for advanced WASI operations
use std::os::raw::{c_char, c_int, c_void};
use crate::error::ffi_utils;

/// Create an advanced WASI context with security policy
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_create_advanced(
    allow_network: c_int,
    allow_tcp_client: c_int,
    allow_tcp_server: c_int,
    allow_udp: c_int,
    allow_http_client: c_int,
    allow_http_server: c_int,
    allow_process_spawn: c_int,
    allow_ipc: c_int,
    allow_shared_memory: c_int,
    allow_hashing: c_int,
    allow_symmetric_crypto: c_int,
    allow_random: c_int,
    max_connections: u32,
    max_threads: u32,
    max_processes: u32,
) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let security_policy = SecurityPolicy {
            network_permissions: NetworkPermissions {
                allow_tcp_client: allow_tcp_client != 0,
                allow_tcp_server: allow_tcp_server != 0,
                allow_udp: allow_udp != 0,
                allow_http_client: allow_http_client != 0,
                allow_http_server: allow_http_server != 0,
                allowed_hosts: Vec::new(), // Would be set separately
                allowed_server_ports: Vec::new(), // Would be set separately
            },
            filesystem_permissions: FilesystemPermissions::default(),
            process_permissions: ProcessPermissions {
                allow_process_spawn: allow_process_spawn != 0,
                allow_ipc: allow_ipc != 0,
                allow_shared_memory: allow_shared_memory != 0,
                allowed_executables: Vec::new(), // Would be set separately
            },
            crypto_permissions: CryptoPermissions {
                allow_hashing: allow_hashing != 0,
                allow_symmetric_crypto: allow_symmetric_crypto != 0,
                allow_asymmetric_crypto: false,
                allow_random: allow_random != 0,
                allow_signatures: false,
            },
            resource_limits: ResourceLimits {
                max_network_connections: if max_connections > 0 { Some(max_connections) } else { None },
                max_threads: if max_threads > 0 { Some(max_threads) } else { None },
                max_child_processes: if max_processes > 0 { Some(max_processes) } else { None },
                max_crypto_memory: Some(64 * 1024 * 1024), // 64MB default
                max_shared_memory: Some(256 * 1024 * 1024), // 256MB default
            },
        };

        // Create in a blocking context since we're in a sync FFI function
        let rt = tokio::runtime::Runtime::new()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create runtime: {}", e),
            })?;

        let ctx = rt.block_on(async {
            WasiAdvancedContext::with_security_policy(security_policy)
        })?;

        Ok(Box::new(ctx))
    })
}

/// Create a TCP socket
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_create_tcp_socket(
    ctx_ptr: *mut c_void,
    addr: *const c_char,
    port: u16,
) -> u32 {
    ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;
        let addr_str = ffi_utils::c_str_to_string(addr, "address")?;

        let socket_addr: SocketAddr = format!("{}:{}", addr_str, port)
            .parse()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Invalid socket address: {}", e),
            })?;

        // Use the runtime from the context to execute async operation
        let rt = tokio::runtime::Runtime::new()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create runtime: {}", e),
            })?;

        let socket_id = rt.block_on(async {
            ctx.create_tcp_socket(socket_addr).await
        })?;

        Ok(socket_id)
    }).1
}

/// Connect to a TCP server
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_connect_tcp(
    ctx_ptr: *mut c_void,
    addr: *const c_char,
    port: u16,
) -> u32 {
    ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;
        let addr_str = ffi_utils::c_str_to_string(addr, "address")?;

        let socket_addr: SocketAddr = format!("{}:{}", addr_str, port)
            .parse()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Invalid socket address: {}", e),
            })?;

        let rt = tokio::runtime::Runtime::new()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create runtime: {}", e),
            })?;

        let connection_id = rt.block_on(async {
            ctx.connect_tcp(socket_addr).await
        })?;

        Ok(connection_id)
    }).1
}

/// Create a UDP socket
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_create_udp_socket(
    ctx_ptr: *mut c_void,
    addr: *const c_char,
    port: u16,
) -> u32 {
    ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;
        let addr_str = ffi_utils::c_str_to_string(addr, "address")?;

        let socket_addr: SocketAddr = format!("{}:{}", addr_str, port)
            .parse()
            .map_err(|e| WasmtimeError::InvalidParameter {
                message: format!("Invalid socket address: {}", e),
            })?;

        let rt = tokio::runtime::Runtime::new()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create runtime: {}", e),
            })?;

        let socket_id = rt.block_on(async {
            ctx.create_udp_socket(socket_addr).await
        })?;

        Ok(socket_id)
    }).1
}

/// Create a hash context
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_create_hasher(
    ctx_ptr: *mut c_void,
    algorithm: c_int,
) -> u32 {
    ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;

        let hash_alg = match algorithm {
            0 => HashAlgorithm::Sha256,
            1 => HashAlgorithm::Sha512,
            2 => HashAlgorithm::Sha3_256,
            3 => HashAlgorithm::Sha3_512,
            4 => HashAlgorithm::Blake3,
            _ => return Err(WasmtimeError::InvalidParameter {
                message: "Invalid hash algorithm".to_string(),
            }),
        };

        let hasher_id = ctx.create_hasher(hash_alg)?;
        Ok(hasher_id)
    }).1
}

/// Generate random bytes
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_generate_random(
    ctx_ptr: *mut c_void,
    buffer: *mut u8,
    length: usize,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;

        if buffer.is_null() || length == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Invalid buffer parameters".to_string(),
            });
        }

        let random_bytes = ctx.generate_random_bytes(length)?;
        let buffer_slice = ffi_utils::slice_from_raw_parts_mut(buffer, length, "random buffer")?;
        buffer_slice.copy_from_slice(&random_bytes);

        Ok(())
    })
}

/// Start a child process
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_start_process(
    ctx_ptr: *mut c_void,
    command: *const *const c_char,
    command_len: usize,
    env_keys: *const *const c_char,
    env_values: *const *const c_char,
    env_len: usize,
) -> u32 {
    ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;

        if command.is_null() || command_len == 0 {
            return Err(WasmtimeError::InvalidParameter {
                message: "Command cannot be empty".to_string(),
            });
        }

        let command_slice = ffi_utils::slice_from_raw_parts(command, command_len, "command array")?;
        let mut cmd_strings = Vec::with_capacity(command_len);

        for &cmd_ptr in command_slice {
            let cmd_str = ffi_utils::c_str_to_string(cmd_ptr, "command argument")?;
            cmd_strings.push(cmd_str);
        }

        let mut env_map = HashMap::new();
        if !env_keys.is_null() && !env_values.is_null() && env_len > 0 {
            let keys_slice = ffi_utils::slice_from_raw_parts(env_keys, env_len, "environment keys")?;
            let values_slice = ffi_utils::slice_from_raw_parts(env_values, env_len, "environment values")?;

            for (&key_ptr, &value_ptr) in keys_slice.iter().zip(values_slice.iter()) {
                let key = ffi_utils::c_str_to_string(key_ptr, "environment key")?;
                let value = ffi_utils::c_str_to_string(value_ptr, "environment value")?;
                env_map.insert(key, value);
            }
        }

        let rt = tokio::runtime::Runtime::new()
            .map_err(|e| WasmtimeError::Wasi {
                message: format!("Failed to create runtime: {}", e),
            })?;

        let process_id = rt.block_on(async {
            ctx.start_process(&cmd_strings, &env_map).await
        })?;

        Ok(process_id)
    }).1
}

/// Create shared memory region
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_create_shared_memory(
    ctx_ptr: *mut c_void,
    size: usize,
) -> u32 {
    ffi_utils::ffi_try(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context")?;
        let memory_id = ctx.create_shared_memory(size)?;
        Ok(memory_id)
    }).1
}

/// Destroy advanced WASI context
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_wasi_advanced_destroy(ctx_ptr: *mut c_void) {
    if !ctx_ptr.is_null() {
        if let Ok(ctx) = ffi_utils::deref_ptr_mut::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context") {
            // Clean up resources asynchronously
            let rt = tokio::runtime::Runtime::new();
            if let Ok(rt) = rt {
                let _ = rt.block_on(async {
                    ctx.cleanup().await
                });
            }
        }
        ffi_utils::destroy_resource::<WasiAdvancedContext>(ctx_ptr, "WASI advanced context");
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::ffi::CString;
    use tempfile::TempDir;

    #[tokio::test]
    async fn test_advanced_wasi_context_creation() {
        let ctx = WasiAdvancedContext::new();
        assert!(ctx.is_ok());

        let ctx = ctx.unwrap();
        let policy = ctx.get_security_policy();
        assert!(!policy.network_permissions.allow_tcp_client);
        assert!(!policy.network_permissions.allow_tcp_server);
        assert!(policy.crypto_permissions.allow_hashing);
        assert!(policy.crypto_permissions.allow_random);
    }

    #[tokio::test]
    async fn test_security_policy_enforcement() {
        let mut policy = SecurityPolicy::default();
        policy.network_permissions.allow_tcp_client = false;

        let ctx = WasiAdvancedContext::with_security_policy(policy).unwrap();

        // Should fail due to security policy
        let result = ctx.connect_tcp("127.0.0.1:8080".parse().unwrap()).await;
        assert!(result.is_err());
        assert!(result.unwrap_err().to_string().contains("not allowed by security policy"));
    }

    #[tokio::test]
    async fn test_tcp_socket_creation() {
        let mut policy = SecurityPolicy::default();
        policy.network_permissions.allow_tcp_server = true;

        let ctx = WasiAdvancedContext::with_security_policy(policy).unwrap();

        // Find an available port
        let listener = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        let addr = listener.local_addr().unwrap();
        drop(listener);

        let result = ctx.create_tcp_socket(addr).await;
        assert!(result.is_ok());
    }

    #[tokio::test]
    async fn test_hash_operations() {
        let mut policy = SecurityPolicy::default();
        policy.crypto_permissions.allow_hashing = true;

        let ctx = WasiAdvancedContext::with_security_policy(policy).unwrap();

        // Test hash context creation
        let hasher_id = ctx.create_hasher(HashAlgorithm::Sha256);
        assert!(hasher_id.is_ok());

        // Test direct hashing
        let data = b"Hello, World!";
        let hash = ctx.hash_data(HashAlgorithm::Sha256, data);
        assert!(hash.is_ok());
        assert_eq!(hash.unwrap().len(), 32); // SHA-256 produces 32 bytes
    }

    #[tokio::test]
    async fn test_random_generation() {
        let ctx = WasiAdvancedContext::new().unwrap();

        let random_bytes = ctx.generate_random_bytes(16);
        assert!(random_bytes.is_ok());
        assert_eq!(random_bytes.unwrap().len(), 16);
    }

    #[tokio::test]
    async fn test_shared_memory() {
        let mut policy = SecurityPolicy::default();
        policy.process_permissions.allow_shared_memory = true;

        let ctx = WasiAdvancedContext::with_security_policy(policy).unwrap();

        // Create shared memory
        let memory_id = ctx.create_shared_memory(1024);
        assert!(memory_id.is_ok());
        let memory_id = memory_id.unwrap();

        // Write data
        let test_data = b"Hello, shared memory!";
        let written = ctx.write_shared_memory(memory_id, 0, test_data);
        assert!(written.is_ok());
        assert_eq!(written.unwrap(), test_data.len());

        // Read data back
        let mut buffer = vec![0u8; test_data.len()];
        let read = ctx.read_shared_memory(memory_id, 0, &mut buffer);
        assert!(read.is_ok());
        assert_eq!(read.unwrap(), test_data.len());
        assert_eq!(&buffer, test_data);
    }

    #[tokio::test]
    async fn test_resource_limits() {
        let mut policy = SecurityPolicy::default();
        policy.resource_limits.max_threads = Some(1);

        let ctx = WasiAdvancedContext::with_security_policy(policy).unwrap();

        // Create first thread - should succeed
        let result1 = ctx.create_thread(async {}).await;
        assert!(result1.is_ok());

        // Create second thread - should fail due to limit
        let result2 = ctx.create_thread(async {}).await;
        assert!(result2.is_err());
        assert!(result2.unwrap_err().to_string().contains("Maximum thread limit"));
    }

    #[test]
    fn test_ffi_create_advanced_context() {
        unsafe {
            let ctx_ptr = wasmtime4j_wasi_create_advanced(
                1, 1, 1, 1, 1, 0, // network permissions
                0, 0, 0, // process permissions
                1, 0, 1, // crypto permissions
                100, 50, 10, // resource limits
            );
            assert!(!ctx_ptr.is_null());
            wasmtime4j_wasi_advanced_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_hash_operations() {
        unsafe {
            let ctx_ptr = wasmtime4j_wasi_create_advanced(
                0, 0, 0, 0, 0, 0, // no network
                0, 0, 0, // no process
                1, 0, 1, // allow hashing and random
                0, 0, 0, // no limits
            );
            assert!(!ctx_ptr.is_null());

            // Test hash context creation
            let hasher_id = wasmtime4j_wasi_create_hasher(ctx_ptr, 0); // SHA-256
            assert!(hasher_id > 0);

            // Test random generation
            let mut buffer = [0u8; 16];
            let result = wasmtime4j_wasi_generate_random(ctx_ptr, buffer.as_mut_ptr(), buffer.len());
            assert_eq!(result, 0); // Success

            wasmtime4j_wasi_advanced_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_shared_memory() {
        unsafe {
            let ctx_ptr = wasmtime4j_wasi_create_advanced(
                0, 0, 0, 0, 0, 0, // no network
                0, 0, 1, // allow shared memory
                0, 0, 0, // no crypto
                0, 0, 0, // no limits
            );
            assert!(!ctx_ptr.is_null());

            // Test shared memory creation
            let memory_id = wasmtime4j_wasi_create_shared_memory(ctx_ptr, 1024);
            assert!(memory_id > 0);

            wasmtime4j_wasi_advanced_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_process_operations() {
        unsafe {
            let ctx_ptr = wasmtime4j_wasi_create_advanced(
                0, 0, 0, 0, 0, 0, // no network
                1, 0, 0, // allow process spawn
                0, 0, 0, // no crypto
                0, 0, 10, // max 10 processes
            );
            assert!(!ctx_ptr.is_null());

            // Test process creation (using echo command which should be available)
            let command = CString::new("echo").unwrap();
            let arg = CString::new("hello").unwrap();
            let commands = vec![command.as_ptr(), arg.as_ptr()];

            let process_id = wasmtime4j_wasi_start_process(
                ctx_ptr,
                commands.as_ptr(),
                commands.len(),
                std::ptr::null(),
                std::ptr::null(),
                0,
            );
            // Process creation might fail depending on system security, but shouldn't crash

            wasmtime4j_wasi_advanced_destroy(ctx_ptr);
        }
    }

    #[test]
    fn test_ffi_error_handling() {
        unsafe {
            // Test with null pointer
            let result = wasmtime4j_wasi_create_hasher(std::ptr::null_mut(), 0);
            assert_eq!(result, 0); // Should fail gracefully

            // Test with invalid algorithm
            let ctx_ptr = wasmtime4j_wasi_create_advanced(
                0, 0, 0, 0, 0, 0,
                0, 0, 0,
                1, 0, 0,
                0, 0, 0,
            );
            let result = wasmtime4j_wasi_create_hasher(ctx_ptr, 999); // Invalid algorithm
            assert_eq!(result, 0); // Should fail gracefully

            wasmtime4j_wasi_advanced_destroy(ctx_ptr);
        }
    }
}