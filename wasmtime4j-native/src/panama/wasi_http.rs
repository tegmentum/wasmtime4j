//! WASI HTTP FFI module for Panama
//!
//! This module provides Panama FFI bindings for WASI HTTP functionality,
//! including HTTP configuration, context management, statistics, and linker integration.

use std::os::raw::{c_char, c_int, c_void};

// ============================================================================
// Config Builder Functions
// ============================================================================

/// Create a new WASI HTTP config builder (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_new() -> *mut c_void {
    unsafe { crate::wasi_http::wasi_http_config_builder_new() }
}

/// Add an allowed host to the config builder (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_allow_host(
    builder_ptr: *mut c_void,
    host: *const c_char,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_allow_host(builder_ptr, host) }
}

/// Block a host in the config builder (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_block_host(
    builder_ptr: *mut c_void,
    host: *const c_char,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_block_host(builder_ptr, host) }
}

/// Set allow all hosts flag (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_allow_all_hosts(
    builder_ptr: *mut c_void,
    allow: c_int,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_allow_all_hosts(builder_ptr, allow) }
}

/// Set connect timeout in milliseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_connect_timeout(
    builder_ptr: *mut c_void,
    timeout_ms: u64,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_connect_timeout(builder_ptr, timeout_ms)
    }
}

/// Set read timeout in milliseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_read_timeout(
    builder_ptr: *mut c_void,
    timeout_ms: u64,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_read_timeout(builder_ptr, timeout_ms) }
}

/// Set write timeout in milliseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_write_timeout(
    builder_ptr: *mut c_void,
    timeout_ms: u64,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_write_timeout(builder_ptr, timeout_ms) }
}

/// Set max connections (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_max_connections(
    builder_ptr: *mut c_void,
    max: u32,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_max_connections(builder_ptr, max) }
}

/// Set max connections per host (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_max_connections_per_host(
    builder_ptr: *mut c_void,
    max: u32,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_max_connections_per_host(builder_ptr, max)
    }
}

/// Set max request body size (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_max_request_body_size(
    builder_ptr: *mut c_void,
    size: u64,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_max_request_body_size(builder_ptr, size)
    }
}

/// Set max response body size (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_max_response_body_size(
    builder_ptr: *mut c_void,
    size: u64,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_max_response_body_size(builder_ptr, size)
    }
}

/// Set HTTPS required flag (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_https_required(
    builder_ptr: *mut c_void,
    required: c_int,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_https_required(builder_ptr, required) }
}

/// Set certificate validation flag (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_certificate_validation(
    builder_ptr: *mut c_void,
    enabled: c_int,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_certificate_validation(builder_ptr, enabled)
    }
}

/// Set HTTP/2 enabled flag (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_http2_enabled(
    builder_ptr: *mut c_void,
    enabled: c_int,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_http2_enabled(builder_ptr, enabled) }
}

/// Set connection pooling flag (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_connection_pooling(
    builder_ptr: *mut c_void,
    enabled: c_int,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_connection_pooling(builder_ptr, enabled)
    }
}

/// Set follow redirects flag (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_follow_redirects(
    builder_ptr: *mut c_void,
    follow: c_int,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_follow_redirects(builder_ptr, follow) }
}

/// Set max redirects (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_max_redirects(
    builder_ptr: *mut c_void,
    max: u32,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_max_redirects(builder_ptr, max) }
}

/// Set user agent (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_set_user_agent(
    builder_ptr: *mut c_void,
    user_agent: *const c_char,
) -> c_int {
    if builder_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_set_user_agent(builder_ptr, user_agent) }
}

/// Build the config from builder (consumes builder) (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_build(
    builder_ptr: *mut c_void,
) -> *mut c_void {
    if builder_ptr.is_null() {
        return std::ptr::null_mut();
    }
    unsafe { crate::wasi_http::wasi_http_config_builder_build(builder_ptr) }
}

/// Free a config builder (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_builder_free(builder_ptr: *mut c_void) {
    if !builder_ptr.is_null() {
        unsafe { crate::wasi_http::wasi_http_config_builder_free(builder_ptr) }
    }
}

// ============================================================================
// Config Functions
// ============================================================================

/// Create a default WASI HTTP config (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_default() -> *mut c_void {
    unsafe { crate::wasi_http::wasi_http_config_default() }
}

/// Free a WASI HTTP config (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_config_free(config_ptr: *mut c_void) {
    if !config_ptr.is_null() {
        unsafe { crate::wasi_http::wasi_http_config_free(config_ptr) }
    }
}

// ============================================================================
// Context Functions
// ============================================================================

/// Create a new WASI HTTP context with config (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_new(config_ptr: *mut c_void) -> *mut c_void {
    unsafe { crate::wasi_http::wasi_http_ctx_new(config_ptr) }
}

/// Create a new WASI HTTP context with default config (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_new_default() -> *mut c_void {
    unsafe { crate::wasi_http::wasi_http_ctx_new_default() }
}

/// Get context ID (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_get_id(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_get_id(ctx_ptr) }
}

/// Check if context is valid (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_is_valid(ctx_ptr: *const c_void) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_is_valid(ctx_ptr) }
}

/// Check if host is allowed (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_is_host_allowed(
    ctx_ptr: *const c_void,
    host: *const c_char,
) -> c_int {
    if ctx_ptr.is_null() || host.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_is_host_allowed(ctx_ptr, host) }
}

/// Reset statistics (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_reset_stats(ctx_ptr: *mut c_void) -> c_int {
    if ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_reset_stats(ctx_ptr) }
}

// ============================================================================
// Statistics Functions
// ============================================================================

/// Get statistics - total requests (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_total_requests(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_total_requests(ctx_ptr) }
}

/// Get statistics - successful requests (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_successful_requests(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_successful_requests(ctx_ptr) }
}

/// Get statistics - failed requests (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_failed_requests(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_failed_requests(ctx_ptr) }
}

/// Get statistics - active requests (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_active_requests(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_active_requests(ctx_ptr) }
}

/// Get statistics - bytes sent (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_bytes_sent(ctx_ptr: *const c_void) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_bytes_sent(ctx_ptr) }
}

/// Get statistics - bytes received (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_bytes_received(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_bytes_received(ctx_ptr) }
}

/// Get statistics - connection timeouts (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_connection_timeouts(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_connection_timeouts(ctx_ptr) }
}

/// Get statistics - read timeouts (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_read_timeouts(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_read_timeouts(ctx_ptr) }
}

/// Get statistics - blocked requests (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_blocked_requests(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_blocked_requests(ctx_ptr) }
}

/// Get statistics - body size violations (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_body_size_violations(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_body_size_violations(ctx_ptr) }
}

/// Get statistics - active connections (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_active_connections(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_active_connections(ctx_ptr) }
}

/// Get statistics - idle connections (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_idle_connections(
    ctx_ptr: *const c_void,
) -> c_int {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_idle_connections(ctx_ptr) }
}

/// Get statistics - average duration in milliseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_avg_duration_ms(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_avg_duration_ms(ctx_ptr) }
}

/// Get statistics - min duration in milliseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_min_duration_ms(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_min_duration_ms(ctx_ptr) }
}

/// Get statistics - max duration in milliseconds (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_max_duration_ms(
    ctx_ptr: *const c_void,
) -> u64 {
    if ctx_ptr.is_null() {
        return 0;
    }
    unsafe { crate::wasi_http::wasi_http_ctx_stats_max_duration_ms(ctx_ptr) }
}

/// Free a WASI HTTP context (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_free(ctx_ptr: *mut c_void) {
    if !ctx_ptr.is_null() {
        unsafe { crate::wasi_http::wasi_http_ctx_free(ctx_ptr) }
    }
}

// ============================================================================
// Availability Functions
// ============================================================================

/// Check if WASI HTTP support is available (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_is_available() -> c_int {
    crate::wasi_http::wasi_http_is_available()
}
