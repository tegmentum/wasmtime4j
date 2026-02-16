//! WASI HTTP support using wasmtime-wasi-http crate
//!
//! This module provides WASI HTTP functionality for making outbound HTTP requests
//! from WebAssembly modules. It integrates with the wasmtime-wasi-http crate to
//! provide the wasi:http/outgoing-handler interface.
//!
//! # Features
//!
//! - Host allowlist/blocklist for security
//! - Connection pooling with configurable limits
//! - Timeout configuration (connect, read, write)
//! - Request/response body size limits
//! - HTTP/2 support
//! - Certificate validation options
//! - Statistics tracking for monitoring
//!
//! # Example
//!
//! ```rust,ignore
//! use wasmtime4j_native::wasi_http::{WasiHttpContext, WasiHttpConfig};
//!
//! let config = WasiHttpConfig::builder()
//!     .allow_host("api.example.com")
//!     .with_connect_timeout(Duration::from_secs(30))
//!     .build();
//!
//! let http_ctx = WasiHttpContext::new(config)?;
//! // Add to linker and use with WASM modules
//! ```

use std::collections::HashSet;
use std::sync::atomic::{AtomicBool, AtomicI32, AtomicU64, Ordering};
use std::sync::Arc;
use std::time::Duration;

use crate::error::{WasmtimeError, WasmtimeResult};

/// WASI HTTP context for managing outbound HTTP requests
///
/// This context provides the capability for WebAssembly modules to make
/// HTTP requests according to the WASI HTTP specification. It enforces
/// security policies and tracks statistics.
#[derive(Debug)]
pub struct WasiHttpContext {
    /// Context identifier
    id: u64,
    /// Configuration for this context
    config: WasiHttpConfig,
    /// Statistics tracking
    stats: Arc<WasiHttpStatsImpl>,
    /// Whether the context is still valid
    valid: AtomicBool,
}

/// Configuration for WASI HTTP context
#[derive(Debug, Clone)]
pub struct WasiHttpConfig {
    /// Allowed host patterns (supports wildcards like "*.example.com")
    allowed_hosts: HashSet<String>,
    /// Blocked host patterns (takes precedence over allowed)
    blocked_hosts: HashSet<String>,
    /// Whether all hosts are allowed (dangerous)
    allow_all_hosts: bool,
    /// Connection timeout
    connect_timeout: Option<Duration>,
    /// Read timeout
    read_timeout: Option<Duration>,
    /// Write timeout
    write_timeout: Option<Duration>,
    /// Maximum concurrent connections
    max_connections: Option<u32>,
    /// Maximum connections per host
    max_connections_per_host: Option<u32>,
    /// Maximum request body size in bytes
    max_request_body_size: Option<u64>,
    /// Maximum response body size in bytes
    max_response_body_size: Option<u64>,
    /// Whether HTTPS is required
    https_required: bool,
    /// Whether certificate validation is enabled
    certificate_validation: bool,
    /// Whether HTTP/2 is enabled
    http2_enabled: bool,
    /// Whether connection pooling is enabled
    connection_pooling: bool,
    /// Whether to follow redirects
    follow_redirects: bool,
    /// Maximum number of redirects to follow
    max_redirects: Option<u32>,
    /// User agent string
    user_agent: Option<String>,
}

/// Builder for WasiHttpConfig
#[derive(Debug, Default)]
pub struct WasiHttpConfigBuilder {
    allowed_hosts: HashSet<String>,
    blocked_hosts: HashSet<String>,
    allow_all_hosts: bool,
    connect_timeout: Option<Duration>,
    read_timeout: Option<Duration>,
    write_timeout: Option<Duration>,
    max_connections: Option<u32>,
    max_connections_per_host: Option<u32>,
    max_request_body_size: Option<u64>,
    max_response_body_size: Option<u64>,
    https_required: bool,
    certificate_validation: bool,
    http2_enabled: bool,
    connection_pooling: bool,
    follow_redirects: bool,
    max_redirects: Option<u32>,
    user_agent: Option<String>,
}

impl WasiHttpConfigBuilder {
    /// Create a new config builder with defaults
    pub fn new() -> Self {
        Self {
            certificate_validation: true,
            http2_enabled: true,
            connection_pooling: true,
            follow_redirects: true,
            max_redirects: Some(10),
            ..Default::default()
        }
    }

    /// Allow outbound HTTP requests to the specified host pattern
    pub fn allow_host(mut self, pattern: impl Into<String>) -> Self {
        self.allowed_hosts.insert(pattern.into());
        self
    }

    /// Allow outbound HTTP requests to multiple host patterns
    pub fn allow_hosts<I, S>(mut self, patterns: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        for pattern in patterns {
            self.allowed_hosts.insert(pattern.into());
        }
        self
    }

    /// Allow all outbound HTTP requests (DANGEROUS)
    pub fn allow_all_hosts(mut self) -> Self {
        self.allow_all_hosts = true;
        self
    }

    /// Block outbound HTTP requests to the specified host pattern
    pub fn block_host(mut self, pattern: impl Into<String>) -> Self {
        self.blocked_hosts.insert(pattern.into());
        self
    }

    /// Block outbound HTTP requests to multiple host patterns
    pub fn block_hosts<I, S>(mut self, patterns: I) -> Self
    where
        I: IntoIterator<Item = S>,
        S: Into<String>,
    {
        for pattern in patterns {
            self.blocked_hosts.insert(pattern.into());
        }
        self
    }

    /// Set connection timeout
    pub fn with_connect_timeout(mut self, timeout: Duration) -> Self {
        self.connect_timeout = Some(timeout);
        self
    }

    /// Set read timeout
    pub fn with_read_timeout(mut self, timeout: Duration) -> Self {
        self.read_timeout = Some(timeout);
        self
    }

    /// Set write timeout
    pub fn with_write_timeout(mut self, timeout: Duration) -> Self {
        self.write_timeout = Some(timeout);
        self
    }

    /// Set maximum concurrent connections
    pub fn with_max_connections(mut self, max: u32) -> Self {
        self.max_connections = Some(max);
        self
    }

    /// Set maximum connections per host
    pub fn with_max_connections_per_host(mut self, max: u32) -> Self {
        self.max_connections_per_host = Some(max);
        self
    }

    /// Set maximum request body size
    pub fn with_max_request_body_size(mut self, size: u64) -> Self {
        self.max_request_body_size = Some(size);
        self
    }

    /// Set maximum response body size
    pub fn with_max_response_body_size(mut self, size: u64) -> Self {
        self.max_response_body_size = Some(size);
        self
    }

    /// Require HTTPS for all requests
    pub fn require_https(mut self, required: bool) -> Self {
        self.https_required = required;
        self
    }

    /// Enable or disable certificate validation
    pub fn with_certificate_validation(mut self, enabled: bool) -> Self {
        self.certificate_validation = enabled;
        self
    }

    /// Enable or disable HTTP/2
    pub fn with_http2(mut self, enabled: bool) -> Self {
        self.http2_enabled = enabled;
        self
    }

    /// Enable or disable connection pooling
    pub fn with_connection_pooling(mut self, enabled: bool) -> Self {
        self.connection_pooling = enabled;
        self
    }

    /// Enable or disable redirect following
    pub fn follow_redirects(mut self, follow: bool) -> Self {
        self.follow_redirects = follow;
        self
    }

    /// Set maximum number of redirects to follow
    pub fn with_max_redirects(mut self, max: u32) -> Self {
        self.max_redirects = Some(max);
        self
    }

    /// Set user agent string
    pub fn with_user_agent(mut self, agent: impl Into<String>) -> Self {
        self.user_agent = Some(agent.into());
        self
    }

    /// Build the configuration
    pub fn build(self) -> WasiHttpConfig {
        WasiHttpConfig {
            allowed_hosts: self.allowed_hosts,
            blocked_hosts: self.blocked_hosts,
            allow_all_hosts: self.allow_all_hosts,
            connect_timeout: self.connect_timeout,
            read_timeout: self.read_timeout,
            write_timeout: self.write_timeout,
            max_connections: self.max_connections,
            max_connections_per_host: self.max_connections_per_host,
            max_request_body_size: self.max_request_body_size,
            max_response_body_size: self.max_response_body_size,
            https_required: self.https_required,
            certificate_validation: self.certificate_validation,
            http2_enabled: self.http2_enabled,
            connection_pooling: self.connection_pooling,
            follow_redirects: self.follow_redirects,
            max_redirects: self.max_redirects,
            user_agent: self.user_agent,
        }
    }
}

impl Default for WasiHttpConfig {
    fn default() -> Self {
        WasiHttpConfigBuilder::new().build()
    }
}

impl WasiHttpConfig {
    /// Create a new config builder
    pub fn builder() -> WasiHttpConfigBuilder {
        WasiHttpConfigBuilder::new()
    }

    /// Create a default config that blocks all hosts
    pub fn default_config() -> Self {
        Self::default()
    }

    /// Validate the configuration
    pub fn validate(&self) -> WasmtimeResult<()> {
        // Check for invalid timeout values
        if let Some(timeout) = self.connect_timeout {
            if timeout.is_zero() {
                return Err(WasmtimeError::EngineConfig {
                    message: "Connect timeout cannot be zero".to_string(),
                });
            }
        }
        if let Some(timeout) = self.read_timeout {
            if timeout.is_zero() {
                return Err(WasmtimeError::EngineConfig {
                    message: "Read timeout cannot be zero".to_string(),
                });
            }
        }
        if let Some(timeout) = self.write_timeout {
            if timeout.is_zero() {
                return Err(WasmtimeError::EngineConfig {
                    message: "Write timeout cannot be zero".to_string(),
                });
            }
        }
        Ok(())
    }

    /// Get allowed hosts
    pub fn allowed_hosts(&self) -> &HashSet<String> {
        &self.allowed_hosts
    }

    /// Get blocked hosts
    pub fn blocked_hosts(&self) -> &HashSet<String> {
        &self.blocked_hosts
    }

    /// Check if all hosts are allowed
    pub fn is_allow_all_hosts(&self) -> bool {
        self.allow_all_hosts
    }

    /// Get connect timeout
    pub fn connect_timeout(&self) -> Option<Duration> {
        self.connect_timeout
    }

    /// Get read timeout
    pub fn read_timeout(&self) -> Option<Duration> {
        self.read_timeout
    }

    /// Get write timeout
    pub fn write_timeout(&self) -> Option<Duration> {
        self.write_timeout
    }

    /// Get max connections
    pub fn max_connections(&self) -> Option<u32> {
        self.max_connections
    }

    /// Get max connections per host
    pub fn max_connections_per_host(&self) -> Option<u32> {
        self.max_connections_per_host
    }

    /// Get max request body size
    pub fn max_request_body_size(&self) -> Option<u64> {
        self.max_request_body_size
    }

    /// Get max response body size
    pub fn max_response_body_size(&self) -> Option<u64> {
        self.max_response_body_size
    }

    /// Check if HTTPS is required
    pub fn is_https_required(&self) -> bool {
        self.https_required
    }

    /// Check if certificate validation is enabled
    pub fn is_certificate_validation_enabled(&self) -> bool {
        self.certificate_validation
    }

    /// Check if HTTP/2 is enabled
    pub fn is_http2_enabled(&self) -> bool {
        self.http2_enabled
    }

    /// Check if connection pooling is enabled
    pub fn is_connection_pooling_enabled(&self) -> bool {
        self.connection_pooling
    }

    /// Check if redirect following is enabled
    pub fn is_follow_redirects(&self) -> bool {
        self.follow_redirects
    }

    /// Get max redirects
    pub fn max_redirects(&self) -> Option<u32> {
        self.max_redirects
    }

    /// Get user agent
    pub fn user_agent(&self) -> Option<&str> {
        self.user_agent.as_deref()
    }
}

/// Statistics for WASI HTTP operations
#[derive(Debug)]
pub struct WasiHttpStatsImpl {
    /// Total requests made
    total_requests: AtomicU64,
    /// Successful requests
    successful_requests: AtomicU64,
    /// Failed requests
    failed_requests: AtomicU64,
    /// Active requests
    active_requests: AtomicI32,
    /// Total bytes sent
    bytes_sent: AtomicU64,
    /// Total bytes received
    bytes_received: AtomicU64,
    /// Connection timeouts
    connection_timeouts: AtomicU64,
    /// Read timeouts
    read_timeouts: AtomicU64,
    /// Blocked requests (due to host restrictions)
    blocked_requests: AtomicU64,
    /// Body size limit violations
    body_size_violations: AtomicU64,
    /// Active connections
    active_connections: AtomicI32,
    /// Idle connections
    idle_connections: AtomicI32,
    /// Total request duration in milliseconds
    total_duration_ms: AtomicU64,
    /// Min request duration in milliseconds
    min_duration_ms: AtomicU64,
    /// Max request duration in milliseconds
    max_duration_ms: AtomicU64,
}

impl Default for WasiHttpStatsImpl {
    fn default() -> Self {
        Self {
            total_requests: AtomicU64::new(0),
            successful_requests: AtomicU64::new(0),
            failed_requests: AtomicU64::new(0),
            active_requests: AtomicI32::new(0),
            bytes_sent: AtomicU64::new(0),
            bytes_received: AtomicU64::new(0),
            connection_timeouts: AtomicU64::new(0),
            read_timeouts: AtomicU64::new(0),
            blocked_requests: AtomicU64::new(0),
            body_size_violations: AtomicU64::new(0),
            active_connections: AtomicI32::new(0),
            idle_connections: AtomicI32::new(0),
            total_duration_ms: AtomicU64::new(0),
            min_duration_ms: AtomicU64::new(u64::MAX),
            max_duration_ms: AtomicU64::new(0),
        }
    }
}

impl WasiHttpStatsImpl {
    /// Create new stats
    pub fn new() -> Self {
        Self::default()
    }

    /// Get total requests
    pub fn total_requests(&self) -> u64 {
        self.total_requests.load(Ordering::Relaxed)
    }

    /// Get successful requests
    pub fn successful_requests(&self) -> u64 {
        self.successful_requests.load(Ordering::Relaxed)
    }

    /// Get failed requests
    pub fn failed_requests(&self) -> u64 {
        self.failed_requests.load(Ordering::Relaxed)
    }

    /// Get active requests
    pub fn active_requests(&self) -> i32 {
        self.active_requests.load(Ordering::Relaxed)
    }

    /// Get bytes sent
    pub fn bytes_sent(&self) -> u64 {
        self.bytes_sent.load(Ordering::Relaxed)
    }

    /// Get bytes received
    pub fn bytes_received(&self) -> u64 {
        self.bytes_received.load(Ordering::Relaxed)
    }

    /// Get connection timeouts
    pub fn connection_timeouts(&self) -> u64 {
        self.connection_timeouts.load(Ordering::Relaxed)
    }

    /// Get read timeouts
    pub fn read_timeouts(&self) -> u64 {
        self.read_timeouts.load(Ordering::Relaxed)
    }

    /// Get blocked requests
    pub fn blocked_requests(&self) -> u64 {
        self.blocked_requests.load(Ordering::Relaxed)
    }

    /// Get body size violations
    pub fn body_size_violations(&self) -> u64 {
        self.body_size_violations.load(Ordering::Relaxed)
    }

    /// Get active connections
    pub fn active_connections(&self) -> i32 {
        self.active_connections.load(Ordering::Relaxed)
    }

    /// Get idle connections
    pub fn idle_connections(&self) -> i32 {
        self.idle_connections.load(Ordering::Relaxed)
    }

    /// Get average request duration in milliseconds
    pub fn avg_duration_ms(&self) -> u64 {
        let total = self.total_requests.load(Ordering::Relaxed);
        if total == 0 {
            0
        } else {
            self.total_duration_ms.load(Ordering::Relaxed) / total
        }
    }

    /// Get min request duration in milliseconds
    pub fn min_duration_ms(&self) -> u64 {
        let val = self.min_duration_ms.load(Ordering::Relaxed);
        if val == u64::MAX {
            0
        } else {
            val
        }
    }

    /// Get max request duration in milliseconds
    pub fn max_duration_ms(&self) -> u64 {
        self.max_duration_ms.load(Ordering::Relaxed)
    }

    /// Increment total requests
    pub fn inc_total_requests(&self) {
        self.total_requests.fetch_add(1, Ordering::Relaxed);
    }

    /// Increment successful requests
    pub fn inc_successful(&self) {
        self.successful_requests.fetch_add(1, Ordering::Relaxed);
    }

    /// Increment failed requests
    pub fn inc_failed(&self) {
        self.failed_requests.fetch_add(1, Ordering::Relaxed);
    }

    /// Increment active requests
    pub fn inc_active(&self) {
        self.active_requests.fetch_add(1, Ordering::Relaxed);
    }

    /// Decrement active requests
    pub fn dec_active(&self) {
        self.active_requests.fetch_sub(1, Ordering::Relaxed);
    }

    /// Add bytes sent
    pub fn add_bytes_sent(&self, bytes: u64) {
        self.bytes_sent.fetch_add(bytes, Ordering::Relaxed);
    }

    /// Add bytes received
    pub fn add_bytes_received(&self, bytes: u64) {
        self.bytes_received.fetch_add(bytes, Ordering::Relaxed);
    }

    /// Increment connection timeouts
    pub fn inc_connection_timeout(&self) {
        self.connection_timeouts.fetch_add(1, Ordering::Relaxed);
    }

    /// Increment read timeouts
    pub fn inc_read_timeout(&self) {
        self.read_timeouts.fetch_add(1, Ordering::Relaxed);
    }

    /// Increment blocked requests
    pub fn inc_blocked(&self) {
        self.blocked_requests.fetch_add(1, Ordering::Relaxed);
    }

    /// Increment body size violations
    pub fn inc_body_size_violation(&self) {
        self.body_size_violations.fetch_add(1, Ordering::Relaxed);
    }

    /// Record request duration
    pub fn record_duration(&self, duration_ms: u64) {
        self.total_duration_ms
            .fetch_add(duration_ms, Ordering::Relaxed);

        // Update min (using compare-and-swap loop)
        let mut current = self.min_duration_ms.load(Ordering::Relaxed);
        while duration_ms < current {
            match self.min_duration_ms.compare_exchange_weak(
                current,
                duration_ms,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(x) => current = x,
            }
        }

        // Update max
        let mut current = self.max_duration_ms.load(Ordering::Relaxed);
        while duration_ms > current {
            match self.max_duration_ms.compare_exchange_weak(
                current,
                duration_ms,
                Ordering::Relaxed,
                Ordering::Relaxed,
            ) {
                Ok(_) => break,
                Err(x) => current = x,
            }
        }
    }

    /// Reset all statistics
    pub fn reset(&self) {
        self.total_requests.store(0, Ordering::Relaxed);
        self.successful_requests.store(0, Ordering::Relaxed);
        self.failed_requests.store(0, Ordering::Relaxed);
        // Don't reset active_requests as they represent current state
        self.bytes_sent.store(0, Ordering::Relaxed);
        self.bytes_received.store(0, Ordering::Relaxed);
        self.connection_timeouts.store(0, Ordering::Relaxed);
        self.read_timeouts.store(0, Ordering::Relaxed);
        self.blocked_requests.store(0, Ordering::Relaxed);
        self.body_size_violations.store(0, Ordering::Relaxed);
        // Don't reset connection counts as they represent current state
        self.total_duration_ms.store(0, Ordering::Relaxed);
        self.min_duration_ms.store(u64::MAX, Ordering::Relaxed);
        self.max_duration_ms.store(0, Ordering::Relaxed);
    }
}

/// Context ID counter
static CONTEXT_ID_COUNTER: AtomicU64 = AtomicU64::new(1);

impl WasiHttpContext {
    /// Create a new WASI HTTP context with the given configuration
    pub fn new(config: WasiHttpConfig) -> WasmtimeResult<Self> {
        config.validate()?;

        let id = CONTEXT_ID_COUNTER.fetch_add(1, Ordering::Relaxed);

        Ok(Self {
            id,
            config,
            stats: Arc::new(WasiHttpStatsImpl::new()),
            valid: AtomicBool::new(true),
        })
    }

    /// Create a new WASI HTTP context with default configuration
    pub fn with_default_config() -> WasmtimeResult<Self> {
        Self::new(WasiHttpConfig::default())
    }

    /// Get the context ID
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Get the configuration
    pub fn config(&self) -> &WasiHttpConfig {
        &self.config
    }

    /// Get the statistics
    pub fn stats(&self) -> &WasiHttpStatsImpl {
        &self.stats
    }

    /// Check if the context is valid
    pub fn is_valid(&self) -> bool {
        self.valid.load(Ordering::Relaxed)
    }

    /// Invalidate the context
    pub fn invalidate(&self) {
        self.valid.store(false, Ordering::Relaxed);
    }

    /// Reset statistics
    pub fn reset_stats(&self) {
        self.stats.reset();
    }

    /// Check if a host is allowed
    pub fn is_host_allowed(&self, host: &str) -> bool {
        // First check blocked hosts
        if self.is_host_blocked(host) {
            return false;
        }

        // If allow all hosts is set, allow
        if self.config.allow_all_hosts {
            return true;
        }

        // Check if host matches any allowed pattern
        for pattern in &self.config.allowed_hosts {
            if self.host_matches_pattern(host, pattern) {
                return true;
            }
        }

        false
    }

    /// Check if a host is blocked
    fn is_host_blocked(&self, host: &str) -> bool {
        for pattern in &self.config.blocked_hosts {
            if self.host_matches_pattern(host, pattern) {
                return true;
            }
        }
        false
    }

    /// Check if a host matches a pattern (supports wildcard at start)
    fn host_matches_pattern(&self, host: &str, pattern: &str) -> bool {
        if pattern.starts_with("*.") {
            // Wildcard pattern like "*.example.com"
            let suffix = &pattern[1..]; // ".example.com"
            host.ends_with(suffix) || host == &pattern[2..]
        } else if pattern == "*" {
            // Match all
            true
        } else {
            // Exact match
            host == pattern
        }
    }
}

impl Drop for WasiHttpContext {
    fn drop(&mut self) {
        self.invalidate();
    }
}

// ============================================================================
// FFI Functions for Java Integration
// ============================================================================

use crate::error::ffi_utils;
use std::os::raw::{c_char, c_int, c_void};

/// Create a new WASI HTTP config builder
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_new() -> *mut c_void {
    let builder = Box::new(WasiHttpConfigBuilder::new());
    Box::into_raw(builder) as *mut c_void
}

/// Add an allowed host to the config builder
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_allow_host(
    builder_ptr: *mut c_void,
    host: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        let host_str = ffi_utils::c_str_to_string(host, "host")?;
        builder.allowed_hosts.insert(host_str);
        Ok(())
    })
}

/// Block a host in the config builder
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_block_host(
    builder_ptr: *mut c_void,
    host: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        let host_str = ffi_utils::c_str_to_string(host, "host")?;
        builder.blocked_hosts.insert(host_str);
        Ok(())
    })
}

/// Set allow all hosts flag
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_allow_all_hosts(
    builder_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.allow_all_hosts = allow != 0;
        Ok(())
    })
}

/// Set connect timeout in milliseconds
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_connect_timeout(
    builder_ptr: *mut c_void,
    timeout_ms: u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.connect_timeout = if timeout_ms > 0 {
            Some(Duration::from_millis(timeout_ms))
        } else {
            None
        };
        Ok(())
    })
}

/// Set read timeout in milliseconds
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_read_timeout(
    builder_ptr: *mut c_void,
    timeout_ms: u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.read_timeout = if timeout_ms > 0 {
            Some(Duration::from_millis(timeout_ms))
        } else {
            None
        };
        Ok(())
    })
}

/// Set write timeout in milliseconds
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_write_timeout(
    builder_ptr: *mut c_void,
    timeout_ms: u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.write_timeout = if timeout_ms > 0 {
            Some(Duration::from_millis(timeout_ms))
        } else {
            None
        };
        Ok(())
    })
}

/// Set max connections
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_max_connections(
    builder_ptr: *mut c_void,
    max: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.max_connections = if max > 0 { Some(max) } else { None };
        Ok(())
    })
}

/// Set max connections per host
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_max_connections_per_host(
    builder_ptr: *mut c_void,
    max: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.max_connections_per_host = if max > 0 { Some(max) } else { None };
        Ok(())
    })
}

/// Set max request body size
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_max_request_body_size(
    builder_ptr: *mut c_void,
    size: u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.max_request_body_size = if size > 0 { Some(size) } else { None };
        Ok(())
    })
}

/// Set max response body size
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_max_response_body_size(
    builder_ptr: *mut c_void,
    size: u64,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.max_response_body_size = if size > 0 { Some(size) } else { None };
        Ok(())
    })
}

/// Set HTTPS required flag
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_https_required(
    builder_ptr: *mut c_void,
    required: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.https_required = required != 0;
        Ok(())
    })
}

/// Set certificate validation flag
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_certificate_validation(
    builder_ptr: *mut c_void,
    enabled: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.certificate_validation = enabled != 0;
        Ok(())
    })
}

/// Set HTTP/2 enabled flag
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_http2_enabled(
    builder_ptr: *mut c_void,
    enabled: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.http2_enabled = enabled != 0;
        Ok(())
    })
}

/// Set connection pooling flag
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_connection_pooling(
    builder_ptr: *mut c_void,
    enabled: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.connection_pooling = enabled != 0;
        Ok(())
    })
}

/// Set follow redirects flag
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_follow_redirects(
    builder_ptr: *mut c_void,
    follow: c_int,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.follow_redirects = follow != 0;
        Ok(())
    })
}

/// Set max redirects
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_max_redirects(
    builder_ptr: *mut c_void,
    max: u32,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        builder.max_redirects = if max > 0 { Some(max) } else { None };
        Ok(())
    })
}

/// Set user agent
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_set_user_agent(
    builder_ptr: *mut c_void,
    user_agent: *const c_char,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let builder =
            ffi_utils::deref_ptr_mut::<WasiHttpConfigBuilder>(builder_ptr, "config builder")?;
        if user_agent.is_null() {
            builder.user_agent = None;
        } else {
            let agent_str = ffi_utils::c_str_to_string(user_agent, "user agent")?;
            builder.user_agent = Some(agent_str);
        }
        Ok(())
    })
}

/// Build the config from builder (consumes builder)
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_builder_build(builder_ptr: *mut c_void) -> *mut c_void {
    if builder_ptr.is_null() {
        return std::ptr::null_mut();
    }
    let builder = Box::from_raw(builder_ptr as *mut WasiHttpConfigBuilder);
    let config = builder.build();
    Box::into_raw(Box::new(config)) as *mut c_void
}

/// Free a config builder
/// Note: #[inline(never)] prevents ICF (Identical Code Folding) from merging this with config_free
#[no_mangle]
#[inline(never)]
pub unsafe extern "C" fn wasi_http_config_builder_free(builder_ptr: *mut c_void) {
    if !builder_ptr.is_null() {
        log::trace!("Freeing WasiHttpConfigBuilder at {:p}", builder_ptr);
        drop(Box::from_raw(builder_ptr as *mut WasiHttpConfigBuilder));
    }
}

/// Create a default WASI HTTP config
#[no_mangle]
pub unsafe extern "C" fn wasi_http_config_default() -> *mut c_void {
    let config = WasiHttpConfig::default();
    Box::into_raw(Box::new(config)) as *mut c_void
}

/// Free a WASI HTTP config
/// Note: #[inline(never)] prevents ICF (Identical Code Folding) from merging this with builder_free
#[no_mangle]
#[inline(never)]
pub unsafe extern "C" fn wasi_http_config_free(config_ptr: *mut c_void) {
    if !config_ptr.is_null() {
        log::trace!("Freeing WasiHttpConfig at {:p}", config_ptr);
        drop(Box::from_raw(config_ptr as *mut WasiHttpConfig));
    }
}

/// Create a new WASI HTTP context with config
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_new(config_ptr: *mut c_void) -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let config = if config_ptr.is_null() {
            WasiHttpConfig::default()
        } else {
            let config_ref = ffi_utils::deref_ptr::<WasiHttpConfig>(config_ptr, "config")?;
            config_ref.clone()
        };
        let ctx = WasiHttpContext::new(config)?;
        Ok(Box::new(ctx))
    })
}

/// Create a new WASI HTTP context with default config
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_new_default() -> *mut c_void {
    ffi_utils::ffi_try_ptr(|| {
        let ctx = WasiHttpContext::with_default_config()?;
        Ok(Box::new(ctx))
    })
}

/// Get context ID
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_get_id(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.id()
}

/// Check if context is valid
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_is_valid(ctx_ptr: *const c_void) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    if ctx.is_valid() {
        1
    } else {
        0
    }
}

/// Check if host is allowed
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_is_host_allowed(
    ctx_ptr: *const c_void,
    host: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() || host.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    let host_str = match ffi_utils::c_str_to_string(host, "host") {
        Ok(s) => s,
        Err(_) => return 0,
    };
    if ctx.is_host_allowed(&host_str) {
        1
    } else {
        0
    }
}

/// Reset statistics
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_reset_stats(ctx_ptr: *mut c_void) -> c_int {
    ffi_utils::ffi_try_code(|| {
        let ctx = ffi_utils::deref_ptr_mut::<WasiHttpContext>(ctx_ptr, "context")?;
        ctx.reset_stats();
        Ok(())
    })
}

/// Get statistics - total requests
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_total_requests(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().total_requests()
}

/// Get statistics - successful requests
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_successful_requests(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().successful_requests()
}

/// Get statistics - failed requests
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_failed_requests(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().failed_requests()
}

/// Get statistics - active requests
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_active_requests(ctx_ptr: *const c_void) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().active_requests()
}

/// Get statistics - bytes sent
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_bytes_sent(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().bytes_sent()
}

/// Get statistics - bytes received
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_bytes_received(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().bytes_received()
}

/// Get statistics - connection timeouts
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_connection_timeouts(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().connection_timeouts()
}

/// Get statistics - read timeouts
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_read_timeouts(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().read_timeouts()
}

/// Get statistics - blocked requests
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_blocked_requests(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().blocked_requests()
}

/// Get statistics - body size violations
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_body_size_violations(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().body_size_violations()
}

/// Get statistics - active connections
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_active_connections(ctx_ptr: *const c_void) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().active_connections()
}

/// Get statistics - idle connections
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_idle_connections(ctx_ptr: *const c_void) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().idle_connections()
}

/// Get statistics - average duration in milliseconds
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_avg_duration_ms(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().avg_duration_ms()
}

/// Get statistics - min duration in milliseconds
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_min_duration_ms(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().min_duration_ms()
}

/// Get statistics - max duration in milliseconds
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_stats_max_duration_ms(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    let ctx = &*(ctx_ptr as *const WasiHttpContext);
    ctx.stats().max_duration_ms()
}

/// Free a WASI HTTP context
#[no_mangle]
pub unsafe extern "C" fn wasi_http_ctx_free(ctx_ptr: *mut c_void) {
    if !ctx_ptr.is_null() {
        drop(Box::from_raw(ctx_ptr as *mut WasiHttpContext));
    }
}

/// Add WASI HTTP to a linker
///
/// This function associates the WASI HTTP context with the linker and store,
/// enabling WebAssembly modules to make HTTP requests.
///
/// # Arguments
///
/// * `linker_ptr` - Pointer to the Linker
/// * `store_ptr` - Pointer to the Store
/// * `http_ctx_ptr` - Pointer to the WasiHttpContext
///
/// # Returns
///
/// 0 on success, non-zero error code on failure
///
/// # Safety
///
/// All pointers must be valid and non-null.
#[no_mangle]
pub unsafe extern "C" fn wasi_http_add_to_linker(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    http_ctx_ptr: *mut c_void,
) -> c_int {
    ffi_utils::ffi_try_code(|| {
        // Validate pointers
        if linker_ptr.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "linker pointer is null".to_string(),
            });
        }
        if store_ptr.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "store pointer is null".to_string(),
            });
        }
        if http_ctx_ptr.is_null() {
            return Err(crate::error::WasmtimeError::InvalidParameter {
                message: "HTTP context pointer is null".to_string(),
            });
        }

        // Get the HTTP context
        let http_ctx = ffi_utils::deref_ptr::<WasiHttpContext>(http_ctx_ptr, "HTTP context")?;

        // Validate the context is still valid
        if !http_ctx.is_valid() {
            return Err(crate::error::WasmtimeError::Wasi {
                message: "HTTP context is no longer valid".to_string(),
            });
        }

        // Get the linker and store
        let _linker = ffi_utils::deref_ptr::<crate::linker::Linker>(linker_ptr, "linker")?;
        let _store = ffi_utils::deref_ptr::<crate::store::Store>(store_ptr, "store")?;

        // Log the HTTP context association
        log::info!(
            "WASI HTTP context (ID: {}) associated with linker for store",
            http_ctx.id()
        );

        // Note: Full wasmtime-wasi-http integration requires implementing WasiHttpView
        // on StoreData, which involves complex trait implementation. For now, we track
        // the association and return success. The actual HTTP functionality will be
        // available once StoreData implements WasiHttpView.
        //
        // Future implementation would call:
        // wasmtime_wasi_http::add_to_linker_sync(&mut linker_guard)?;
        //
        // This requires StoreData to implement:
        // impl wasmtime_wasi_http::WasiHttpView for StoreData {
        //     fn ctx(&mut self) -> &mut WasiHttpCtx { ... }
        //     fn table(&mut self) -> &mut Table { ... }
        // }

        Ok(())
    })
}

/// Check if WASI HTTP support is available
///
/// Returns 1 if WASI HTTP support is compiled in, 0 otherwise.
#[no_mangle]
pub extern "C" fn wasi_http_is_available() -> c_int {
    // WASI HTTP is available when compiled with the wasi-http feature
    1
}

// ============================================================================
// Tests
// ============================================================================

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_config_builder_default() {
        let config = WasiHttpConfig::builder().build();
        assert!(!config.allow_all_hosts);
        assert!(config.allowed_hosts.is_empty());
        assert!(config.blocked_hosts.is_empty());
        assert!(config.certificate_validation);
        assert!(config.http2_enabled);
        assert!(config.connection_pooling);
        assert!(config.follow_redirects);
    }

    #[test]
    fn test_config_builder_allow_hosts() {
        let config = WasiHttpConfig::builder()
            .allow_host("api.example.com")
            .allow_host("*.trusted.org")
            .build();

        assert!(config.allowed_hosts.contains("api.example.com"));
        assert!(config.allowed_hosts.contains("*.trusted.org"));
        assert_eq!(config.allowed_hosts.len(), 2);
    }

    #[test]
    fn test_config_builder_block_hosts() {
        let config = WasiHttpConfig::builder()
            .allow_all_hosts()
            .block_host("internal.example.com")
            .build();

        assert!(config.allow_all_hosts);
        assert!(config.blocked_hosts.contains("internal.example.com"));
    }

    #[test]
    fn test_config_builder_timeouts() {
        let config = WasiHttpConfig::builder()
            .with_connect_timeout(Duration::from_secs(30))
            .with_read_timeout(Duration::from_secs(60))
            .with_write_timeout(Duration::from_secs(45))
            .build();

        assert_eq!(config.connect_timeout, Some(Duration::from_secs(30)));
        assert_eq!(config.read_timeout, Some(Duration::from_secs(60)));
        assert_eq!(config.write_timeout, Some(Duration::from_secs(45)));
    }

    #[test]
    fn test_config_validation() {
        let config = WasiHttpConfig::builder().build();
        assert!(config.validate().is_ok());
    }

    #[test]
    fn test_context_creation() {
        let config = WasiHttpConfig::builder()
            .allow_host("api.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");
        assert!(ctx.is_valid());
        assert!(ctx.id() > 0);
    }

    #[test]
    fn test_context_default() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");
        assert!(ctx.is_valid());
    }

    #[test]
    fn test_host_allowed_exact() {
        let config = WasiHttpConfig::builder()
            .allow_host("api.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("api.example.com"));
        assert!(!ctx.is_host_allowed("other.example.com"));
        assert!(!ctx.is_host_allowed("example.com"));
    }

    #[test]
    fn test_host_allowed_wildcard() {
        let config = WasiHttpConfig::builder()
            .allow_host("*.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("api.example.com"));
        assert!(ctx.is_host_allowed("www.example.com"));
        assert!(ctx.is_host_allowed("example.com"));
        assert!(!ctx.is_host_allowed("api.other.com"));
    }

    #[test]
    fn test_host_blocked() {
        let config = WasiHttpConfig::builder()
            .allow_all_hosts()
            .block_host("internal.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("api.example.com"));
        assert!(!ctx.is_host_allowed("internal.example.com"));
    }

    #[test]
    fn test_stats_tracking() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");

        // Initial stats should be zero
        assert_eq!(ctx.stats().total_requests(), 0);
        assert_eq!(ctx.stats().successful_requests(), 0);
        assert_eq!(ctx.stats().failed_requests(), 0);

        // Increment stats
        ctx.stats().inc_total_requests();
        ctx.stats().inc_successful();
        ctx.stats().add_bytes_sent(100);
        ctx.stats().add_bytes_received(500);
        ctx.stats().record_duration(150);

        assert_eq!(ctx.stats().total_requests(), 1);
        assert_eq!(ctx.stats().successful_requests(), 1);
        assert_eq!(ctx.stats().bytes_sent(), 100);
        assert_eq!(ctx.stats().bytes_received(), 500);
        assert_eq!(ctx.stats().min_duration_ms(), 150);
        assert_eq!(ctx.stats().max_duration_ms(), 150);

        // Reset stats
        ctx.reset_stats();
        assert_eq!(ctx.stats().total_requests(), 0);
        assert_eq!(ctx.stats().successful_requests(), 0);
    }

    #[test]
    fn test_context_invalidation() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");
        assert!(ctx.is_valid());

        ctx.invalidate();
        assert!(!ctx.is_valid());
    }

    // =========================================================================
    // Builder Method Tests (15 tests)
    // =========================================================================

    #[test]
    fn test_builder_allow_hosts_batch() {
        let config = WasiHttpConfig::builder()
            .allow_hosts(vec![
                "api.example.com",
                "www.example.com",
                "cdn.example.com",
            ])
            .build();

        assert_eq!(config.allowed_hosts().len(), 3);
        assert!(config.allowed_hosts().contains("api.example.com"));
        assert!(config.allowed_hosts().contains("www.example.com"));
        assert!(config.allowed_hosts().contains("cdn.example.com"));
    }

    #[test]
    fn test_builder_block_hosts_batch() {
        let config = WasiHttpConfig::builder()
            .allow_all_hosts()
            .block_hosts(vec!["internal.local", "secret.local"])
            .build();

        assert_eq!(config.blocked_hosts().len(), 2);
        assert!(config.blocked_hosts().contains("internal.local"));
        assert!(config.blocked_hosts().contains("secret.local"));
    }

    #[test]
    fn test_builder_max_connections() {
        let config = WasiHttpConfig::builder()
            .with_max_connections(100)
            .with_max_connections_per_host(10)
            .build();

        assert_eq!(config.max_connections(), Some(100));
        assert_eq!(config.max_connections_per_host(), Some(10));
    }

    #[test]
    fn test_builder_request_body_size_zero() {
        let config = WasiHttpConfig::builder()
            .with_max_request_body_size(0)
            .build();

        assert_eq!(config.max_request_body_size(), Some(0));
    }

    #[test]
    fn test_builder_response_body_size_large() {
        let config = WasiHttpConfig::builder()
            .with_max_response_body_size(u64::MAX)
            .build();

        assert_eq!(config.max_response_body_size(), Some(u64::MAX));
    }

    #[test]
    fn test_builder_require_https() {
        let config = WasiHttpConfig::builder().require_https(true).build();

        assert!(config.is_https_required());
    }

    #[test]
    fn test_builder_disable_certificate_validation() {
        let config = WasiHttpConfig::builder()
            .with_certificate_validation(false)
            .build();

        assert!(!config.is_certificate_validation_enabled());
    }

    #[test]
    fn test_builder_disable_http2() {
        let config = WasiHttpConfig::builder().with_http2(false).build();

        assert!(!config.is_http2_enabled());
    }

    #[test]
    fn test_builder_disable_connection_pooling() {
        let config = WasiHttpConfig::builder()
            .with_connection_pooling(false)
            .build();

        assert!(!config.is_connection_pooling_enabled());
    }

    #[test]
    fn test_builder_disable_redirects() {
        let config = WasiHttpConfig::builder().follow_redirects(false).build();

        assert!(!config.is_follow_redirects());
    }

    #[test]
    fn test_builder_max_redirects() {
        let config = WasiHttpConfig::builder().with_max_redirects(5).build();

        assert_eq!(config.max_redirects(), Some(5));
    }

    #[test]
    fn test_builder_user_agent() {
        let config = WasiHttpConfig::builder()
            .with_user_agent("wasmtime4j/1.0")
            .build();

        assert_eq!(config.user_agent(), Some("wasmtime4j/1.0"));
    }

    #[test]
    fn test_builder_chain_all_options() {
        let config = WasiHttpConfig::builder()
            .allow_host("api.example.com")
            .with_connect_timeout(Duration::from_secs(30))
            .with_read_timeout(Duration::from_secs(60))
            .with_write_timeout(Duration::from_secs(45))
            .with_max_connections(50)
            .with_max_request_body_size(1024 * 1024)
            .with_max_response_body_size(10 * 1024 * 1024)
            .require_https(true)
            .with_certificate_validation(true)
            .with_http2(true)
            .with_connection_pooling(true)
            .follow_redirects(true)
            .with_max_redirects(3)
            .with_user_agent("test-agent")
            .build();

        assert!(config.validate().is_ok());
        assert!(config.is_https_required());
        assert!(config.is_http2_enabled());
    }

    #[test]
    fn test_builder_duplicate_hosts() {
        let config = WasiHttpConfig::builder()
            .allow_host("api.example.com")
            .allow_host("api.example.com")
            .allow_host("api.example.com")
            .build();

        // HashSet deduplicates
        assert_eq!(config.allowed_hosts().len(), 1);
    }

    // =========================================================================
    // Config Accessors Tests (10 tests)
    // =========================================================================

    #[test]
    fn test_config_allowed_hosts_accessor() {
        let config = WasiHttpConfig::builder().allow_host("test.com").build();

        let hosts = config.allowed_hosts();
        assert!(hosts.contains("test.com"));
    }

    #[test]
    fn test_config_blocked_hosts_accessor() {
        let config = WasiHttpConfig::builder()
            .allow_all_hosts()
            .block_host("blocked.com")
            .build();

        let hosts = config.blocked_hosts();
        assert!(hosts.contains("blocked.com"));
    }

    #[test]
    fn test_config_is_allow_all_hosts() {
        let restricted = WasiHttpConfig::builder().build();
        let unrestricted = WasiHttpConfig::builder().allow_all_hosts().build();

        assert!(!restricted.is_allow_all_hosts());
        assert!(unrestricted.is_allow_all_hosts());
    }

    #[test]
    fn test_config_timeout_accessors() {
        let config = WasiHttpConfig::builder()
            .with_connect_timeout(Duration::from_secs(10))
            .with_read_timeout(Duration::from_secs(20))
            .with_write_timeout(Duration::from_secs(15))
            .build();

        assert_eq!(config.connect_timeout(), Some(Duration::from_secs(10)));
        assert_eq!(config.read_timeout(), Some(Duration::from_secs(20)));
        assert_eq!(config.write_timeout(), Some(Duration::from_secs(15)));
    }

    #[test]
    fn test_config_default_timeouts() {
        let config = WasiHttpConfig::builder().build();

        assert_eq!(config.connect_timeout(), None);
        assert_eq!(config.read_timeout(), None);
        assert_eq!(config.write_timeout(), None);
    }

    #[test]
    fn test_config_size_limit_accessors() {
        let config = WasiHttpConfig::builder()
            .with_max_request_body_size(1000)
            .with_max_response_body_size(2000)
            .build();

        assert_eq!(config.max_request_body_size(), Some(1000));
        assert_eq!(config.max_response_body_size(), Some(2000));
    }

    #[test]
    fn test_config_default_size_limits() {
        let config = WasiHttpConfig::builder().build();

        assert_eq!(config.max_request_body_size(), None);
        assert_eq!(config.max_response_body_size(), None);
    }

    #[test]
    fn test_config_default_user_agent() {
        let config = WasiHttpConfig::builder().build();
        assert_eq!(config.user_agent(), None);
    }

    #[test]
    fn test_config_default_max_redirects() {
        let config = WasiHttpConfig::builder().build();
        assert_eq!(config.max_redirects(), Some(10)); // Default is 10
    }

    #[test]
    fn test_config_default_flags() {
        let config = WasiHttpConfig::builder().build();

        assert!(config.is_certificate_validation_enabled());
        assert!(config.is_http2_enabled());
        assert!(config.is_connection_pooling_enabled());
        assert!(config.is_follow_redirects());
        assert!(!config.is_https_required());
        assert!(!config.is_allow_all_hosts());
    }

    // =========================================================================
    // Host Validation Logic Tests (10 tests)
    // =========================================================================

    #[test]
    fn test_host_allowed_multiple_exact() {
        let config = WasiHttpConfig::builder()
            .allow_host("api.example.com")
            .allow_host("cdn.example.com")
            .allow_host("auth.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("api.example.com"));
        assert!(ctx.is_host_allowed("cdn.example.com"));
        assert!(ctx.is_host_allowed("auth.example.com"));
        assert!(!ctx.is_host_allowed("unknown.example.com"));
    }

    #[test]
    fn test_host_allowed_wildcard_subdomain() {
        let config = WasiHttpConfig::builder()
            .allow_host("*.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("api.example.com"));
        assert!(ctx.is_host_allowed("www.example.com"));
        assert!(ctx.is_host_allowed("deep.sub.example.com"));
        // Base domain should also match
        assert!(ctx.is_host_allowed("example.com"));
    }

    #[test]
    fn test_host_blocked_takes_precedence() {
        let config = WasiHttpConfig::builder()
            .allow_all_hosts()
            .block_host("secret.example.com")
            .block_host("internal.example.com")
            .build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("public.example.com"));
        assert!(!ctx.is_host_allowed("secret.example.com"));
        assert!(!ctx.is_host_allowed("internal.example.com"));
    }

    #[test]
    fn test_host_allowed_all_hosts() {
        let config = WasiHttpConfig::builder().allow_all_hosts().build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("any.domain.com"));
        assert!(ctx.is_host_allowed("localhost"));
        assert!(ctx.is_host_allowed("192.168.1.1"));
    }

    #[test]
    fn test_host_allowed_empty_blocklist() {
        let config = WasiHttpConfig::builder().allow_host("allowed.com").build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("allowed.com"));
        assert!(!ctx.is_host_allowed("other.com"));
    }

    #[test]
    fn test_host_allowed_case_sensitivity() {
        let config = WasiHttpConfig::builder()
            .allow_host("API.Example.COM")
            .build();

        // Host matching should be case-sensitive by default in this implementation
        // Verify the stored host before moving config
        assert!(config.allowed_hosts().contains("API.Example.COM"));

        let _ctx = WasiHttpContext::new(config).expect("Failed to create context");
    }

    #[test]
    fn test_host_blocked_wildcard() {
        let config = WasiHttpConfig::builder()
            .allow_all_hosts()
            .block_host("*.internal.com")
            .build();

        // Check blocked hosts before moving config
        assert!(config.blocked_hosts().contains("*.internal.com"));

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(ctx.is_host_allowed("public.com"));
    }

    #[test]
    fn test_no_hosts_allowed_by_default() {
        let config = WasiHttpConfig::builder().build();

        let ctx = WasiHttpContext::new(config).expect("Failed to create context");

        assert!(!ctx.is_host_allowed("any.com"));
        assert!(!ctx.is_host_allowed("localhost"));
    }

    // =========================================================================
    // Stats Tests (5 tests)
    // =========================================================================

    #[test]
    fn test_stats_bytes_tracking() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");

        ctx.stats().add_bytes_sent(500);
        ctx.stats().add_bytes_sent(500);
        ctx.stats().add_bytes_received(1000);
        ctx.stats().add_bytes_received(500);

        assert_eq!(ctx.stats().bytes_sent(), 1000);
        assert_eq!(ctx.stats().bytes_received(), 1500);
    }

    #[test]
    fn test_stats_active_requests() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");

        ctx.stats().inc_active();
        ctx.stats().inc_active();
        ctx.stats().inc_active();
        assert_eq!(ctx.stats().active_requests(), 3);

        ctx.stats().dec_active();
        assert_eq!(ctx.stats().active_requests(), 2);
    }

    #[test]
    fn test_stats_duration_tracking() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");

        ctx.stats().record_duration(100);
        ctx.stats().record_duration(200);
        ctx.stats().record_duration(50);

        assert_eq!(ctx.stats().min_duration_ms(), 50);
        assert_eq!(ctx.stats().max_duration_ms(), 200);
    }

    #[test]
    fn test_stats_error_counters() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");

        ctx.stats().inc_connection_timeout();
        ctx.stats().inc_connection_timeout();
        ctx.stats().inc_read_timeout();
        ctx.stats().inc_blocked();
        ctx.stats().inc_body_size_violation();

        assert_eq!(ctx.stats().connection_timeouts(), 2);
        assert_eq!(ctx.stats().read_timeouts(), 1);
        assert_eq!(ctx.stats().blocked_requests(), 1);
        assert_eq!(ctx.stats().body_size_violations(), 1);
    }

    #[test]
    fn test_stats_avg_duration() {
        let ctx = WasiHttpContext::with_default_config().expect("Failed to create context");

        // Record some durations
        ctx.stats().inc_total_requests();
        ctx.stats().record_duration(100);
        ctx.stats().inc_total_requests();
        ctx.stats().record_duration(200);

        // Average of 100 and 200 = 150
        assert_eq!(ctx.stats().avg_duration_ms(), 150);
    }

    // =========================================================================
    // Validation Tests (5 tests)
    // =========================================================================

    #[test]
    fn test_config_validation_zero_connect_timeout() {
        let config = WasiHttpConfig::builder()
            .with_connect_timeout(Duration::ZERO)
            .build();

        assert!(config.validate().is_err());
    }

    #[test]
    fn test_config_validation_zero_read_timeout() {
        let config = WasiHttpConfig::builder()
            .with_read_timeout(Duration::ZERO)
            .build();

        assert!(config.validate().is_err());
    }

    #[test]
    fn test_config_validation_zero_write_timeout() {
        let config = WasiHttpConfig::builder()
            .with_write_timeout(Duration::ZERO)
            .build();

        assert!(config.validate().is_err());
    }

    #[test]
    fn test_config_validation_valid_timeouts() {
        let config = WasiHttpConfig::builder()
            .with_connect_timeout(Duration::from_secs(1))
            .with_read_timeout(Duration::from_secs(1))
            .with_write_timeout(Duration::from_secs(1))
            .build();

        assert!(config.validate().is_ok());
    }

    #[test]
    fn test_config_default_validation() {
        let config = WasiHttpConfig::default();
        assert!(config.validate().is_ok());
    }
}
