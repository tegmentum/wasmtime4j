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
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_read_timeout(builder_ptr, timeout_ms)
    }
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
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_write_timeout(builder_ptr, timeout_ms)
    }
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
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_https_required(builder_ptr, required)
    }
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
        crate::wasi_http::wasi_http_config_builder_set_certificate_validation(
            builder_ptr,
            enabled,
        )
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
    unsafe {
        crate::wasi_http::wasi_http_config_builder_set_follow_redirects(builder_ptr, follow)
    }
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
pub extern "C" fn wasmtime4j_panama_wasi_http_ctx_stats_bytes_sent(
    ctx_ptr: *const c_void,
) -> u64 {
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
// Linker Integration Functions
// ============================================================================

/// Add WASI HTTP to a linker (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_add_to_linker(
    linker_ptr: *mut c_void,
    store_ptr: *mut c_void,
    http_ctx_ptr: *mut c_void,
) -> c_int {
    if linker_ptr.is_null() || store_ptr.is_null() || http_ctx_ptr.is_null() {
        return -1;
    }
    unsafe { crate::wasi_http::wasi_http_add_to_linker(linker_ptr, store_ptr, http_ctx_ptr) }
}

/// Check if WASI HTTP support is available (Panama FFI)
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_wasi_http_is_available() -> c_int {
    crate::wasi_http::wasi_http_is_available()
}

// ============================================================================
// Exception Handling Functions
// ============================================================================

/// Creates a new WebAssembly tag for exception handling.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
/// - param_types and return_types must be valid pointers to int arrays
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_create(
    store_ptr: *mut c_void,
    param_types: *const c_int,
    param_count: c_int,
    _return_types: *const c_int,
    _return_count: c_int,
) -> *mut c_void {
    use wasmtime::{FuncType, Tag, TagType, ValType};

    if store_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let store = unsafe { &*(store_ptr as *const crate::store::Store) };

    // Convert param type codes to ValTypes
    let params: Vec<ValType> = if param_count > 0 && !param_types.is_null() {
        let param_slice =
            unsafe { std::slice::from_raw_parts(param_types, param_count as usize) };
        param_slice
            .iter()
            .filter_map(|&code| match code {
                0 => Some(ValType::I32),
                1 => Some(ValType::I64),
                2 => Some(ValType::F32),
                3 => Some(ValType::F64),
                4 => Some(ValType::V128),
                5 => Some(ValType::FUNCREF),
                6 => Some(ValType::EXTERNREF),
                _ => None,
            })
            .collect()
    } else {
        Vec::new()
    };

    // Create FuncType for the tag (tags use params only, empty results)
    let func_type = FuncType::new(store.engine().inner(), params.iter().cloned(), []);

    // Create TagType from FuncType
    let tag_type = TagType::new(func_type);

    // Lock the store and create the Tag
    let mut store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => return std::ptr::null_mut(),
    };
    match Tag::new(&mut *store_guard, &tag_type) {
        Ok(tag) => Box::into_raw(Box::new(tag)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Gets the parameter types of a tag.
///
/// # Safety
/// - tag_ptr must be a valid pointer to a Tag
/// - store_ptr must be a valid pointer to a Store
/// - out_count must be a valid pointer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_get_param_types(
    tag_ptr: *const c_void,
    store_ptr: *mut c_void,
    out_count: *mut c_int,
) -> *mut c_int {
    use wasmtime::{Tag, ValType};

    if tag_ptr.is_null() || store_ptr.is_null() || out_count.is_null() {
        if !out_count.is_null() {
            unsafe {
                *out_count = 0;
            }
        }
        return std::ptr::null_mut();
    }

    let tag = unsafe { &*(tag_ptr as *const Tag) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => {
            unsafe { *out_count = 0; }
            return std::ptr::null_mut();
        }
    };

    let tag_type = tag.ty(&*store_guard);
    let func_type = tag_type.ty();
    let params: Vec<c_int> = func_type
        .params()
        .map(|vt| match vt {
            ValType::I32 => 0,
            ValType::I64 => 1,
            ValType::F32 => 2,
            ValType::F64 => 3,
            ValType::V128 => 4,
            ValType::Ref(r) => match r.heap_type() {
                wasmtime::HeapType::Func => 5,
                _ => 6, // EXTERNREF or other ref types
            },
        })
        .collect();

    let count = params.len();
    unsafe {
        *out_count = count as c_int;
    }

    if count == 0 {
        return std::ptr::null_mut();
    }

    let boxed = params.into_boxed_slice();
    Box::into_raw(boxed) as *mut c_int
}

/// Gets the return types of a tag.
///
/// # Safety
/// - tag_ptr must be a valid pointer to a Tag
/// - store_ptr must be a valid pointer to a Store
/// - out_count must be a valid pointer
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_get_return_types(
    tag_ptr: *const c_void,
    store_ptr: *mut c_void,
    out_count: *mut c_int,
) -> *mut c_int {
    use wasmtime::{Tag, ValType};

    if tag_ptr.is_null() || store_ptr.is_null() || out_count.is_null() {
        if !out_count.is_null() {
            unsafe {
                *out_count = 0;
            }
        }
        return std::ptr::null_mut();
    }

    let tag = unsafe { &*(tag_ptr as *const Tag) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => {
            unsafe { *out_count = 0; }
            return std::ptr::null_mut();
        }
    };

    let tag_type = tag.ty(&*store_guard);
    let func_type = tag_type.ty();
    let results: Vec<c_int> = func_type
        .results()
        .map(|vt| match vt {
            ValType::I32 => 0,
            ValType::I64 => 1,
            ValType::F32 => 2,
            ValType::F64 => 3,
            ValType::V128 => 4,
            ValType::Ref(r) => match r.heap_type() {
                wasmtime::HeapType::Func => 5,
                _ => 6, // EXTERNREF or other ref types
            },
        })
        .collect();

    let count = results.len();
    unsafe {
        *out_count = count as c_int;
    }

    if count == 0 {
        return std::ptr::null_mut();
    }

    let boxed = results.into_boxed_slice();
    Box::into_raw(boxed) as *mut c_int
}

/// Frees a tag types array.
///
/// # Safety
/// - types_ptr must be a valid pointer allocated by tag_get_param/return_types
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_types_free(types_ptr: *mut c_int, count: c_int) {
    if !types_ptr.is_null() && count > 0 {
        unsafe {
            let _ = Vec::from_raw_parts(types_ptr, count as usize, count as usize);
        }
    }
}

/// Checks if two tags are equal.
///
/// # Safety
/// - tag1_ptr and tag2_ptr must be valid pointers to Tags
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_equals(
    tag1_ptr: *const c_void,
    tag2_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> c_int {
    use wasmtime::Tag;

    if tag1_ptr.is_null() || tag2_ptr.is_null() || store_ptr.is_null() {
        return 0;
    }

    let tag1 = unsafe { &*(tag1_ptr as *const Tag) };
    let tag2 = unsafe { &*(tag2_ptr as *const Tag) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => return 0, // Return false if store is closed
    };

    // Compare tags by checking if they have the same type
    let ty1 = tag1.ty(&*store_guard);
    let ty2 = tag2.ty(&*store_guard);

    // Tags are equal if their func types match
    let ft1 = ty1.ty();
    let ft2 = ty2.ty();

    // Compare params and results count and types using matches()
    let params1: Vec<_> = ft1.params().collect();
    let params2: Vec<_> = ft2.params().collect();
    let results1: Vec<_> = ft1.results().collect();
    let results2: Vec<_> = ft2.results().collect();

    if params1.len() != params2.len() || results1.len() != results2.len() {
        return 0;
    }

    // Check each param type matches
    for (p1, p2) in params1.iter().zip(params2.iter()) {
        if !p1.matches(p2) {
            return 0;
        }
    }

    // Check each result type matches
    for (r1, r2) in results1.iter().zip(results2.iter()) {
        if !r1.matches(r2) {
            return 0;
        }
    }

    1
}

/// Destroys a tag and frees its native resources.
///
/// # Safety
/// - tag_ptr must be a valid pointer to a Tag
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_tag_destroy(tag_ptr: *mut c_void) {
    use wasmtime::Tag;

    if !tag_ptr.is_null() {
        unsafe {
            let _ = Box::from_raw(tag_ptr as *mut Tag);
        }
    }
}

/// Gets the tag from an exception reference.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an ExnRef
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_get_tag(
    exnref_ptr: *const c_void,
    store_ptr: *mut c_void,
) -> *mut c_void {
    use wasmtime::{ExnRef, OwnedRooted, RootScope};

    if exnref_ptr.is_null() || store_ptr.is_null() {
        return std::ptr::null_mut();
    }

    let owned_exnref = unsafe { &*(exnref_ptr as *const OwnedRooted<ExnRef>) };
    let store = unsafe { &*(store_ptr as *const crate::store::Store) };
    let mut store_guard = match store.try_lock_store() {
        Ok(guard) => guard,
        Err(_) => return std::ptr::null_mut(),
    };

    // Create a root scope and get the tag
    let mut scope = RootScope::new(&mut *store_guard);
    let exnref = owned_exnref.to_rooted(&mut scope);
    match exnref.tag(&mut scope) {
        Ok(tag) => Box::into_raw(Box::new(tag)) as *mut c_void,
        Err(_) => std::ptr::null_mut(),
    }
}

/// Checks if an exception reference is valid.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an ExnRef
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_is_valid(
    _exnref_ptr: *const c_void,
    _store_ptr: *mut c_void,
) -> c_int {
    // ExnRef.isValid is not yet implemented
    0
}

/// Destroys an exception reference and frees its native resources.
///
/// # Safety
/// - exnref_ptr must be a valid pointer to an OwnedRooted<ExnRef>
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_exnref_destroy(exnref_ptr: *mut c_void) -> c_int {
    if exnref_ptr.is_null() {
        return 0; // FFI_SUCCESS
    }
    unsafe {
        let _boxed = Box::from_raw(exnref_ptr as *mut wasmtime::OwnedRooted<wasmtime::ExnRef>);
        // Dropping the Box cleans up the OwnedRooted
    }
    0 // FFI_SUCCESS
}

/// Checks if the store has a pending exception.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_has_pending_exception(store_ptr: *mut c_void) -> c_int {
    if store_ptr.is_null() {
        return 0;
    }
    unsafe {
        match crate::store::core::get_store_ref(store_ptr) {
            Ok(store) => {
                if store.has_pending_exception() {
                    1
                } else {
                    0
                }
            }
            Err(_) => 0,
        }
    }
}

/// Takes and removes the pending exception from the store.
/// Returns a handle to the ExnRef, or null if no exception is pending.
///
/// # Safety
/// - store_ptr must be a valid pointer to a Store
#[no_mangle]
pub extern "C" fn wasmtime4j_panama_store_take_pending_exception(
    store_ptr: *mut c_void,
) -> *mut c_void {
    if store_ptr.is_null() {
        return std::ptr::null_mut();
    }
    unsafe {
        match crate::store::core::get_store_ref(store_ptr) {
            Ok(store) => {
                match store.take_pending_exception() {
                    Some(exn_ref) => {
                        // Box the OwnedRooted<ExnRef> and return as handle
                        Box::into_raw(Box::new(exn_ref)) as *mut c_void
                    }
                    None => std::ptr::null_mut(),
                }
            }
            Err(_) => std::ptr::null_mut(),
        }
    }
}
