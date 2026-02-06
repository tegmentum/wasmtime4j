//! Store Resource Limiter API
//!
//! This module provides resource limiting functionality for Wasmtime stores.
//! Resource limiters allow controlling memory growth, table growth, and instance creation
//! to prevent runaway resource consumption.
//!
//! The Wasmtime 38.x API uses a trait-based approach where `ResourceLimiter` is implemented
//! to provide callbacks for memory/table growth decisions.

use std::collections::HashMap;
use std::ffi::{c_int, c_longlong, c_void};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::{Arc, RwLock};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::store::Store;

/// Configuration for resource limits
#[derive(Debug, Clone)]
pub struct ResourceLimiterConfig {
    /// Maximum total memory in bytes across all memories
    pub max_memory_bytes: Option<u64>,
    /// Maximum memory pages per memory (64KB per page)
    pub max_memory_pages: Option<u64>,
    /// Maximum table elements per table
    pub max_table_elements: Option<u64>,
    /// Maximum number of instances
    pub max_instances: Option<u32>,
    /// Maximum number of tables
    pub max_tables: Option<u32>,
    /// Maximum number of memories
    pub max_memories: Option<u32>,
}

impl Default for ResourceLimiterConfig {
    fn default() -> Self {
        ResourceLimiterConfig {
            max_memory_bytes: None,
            max_memory_pages: None,
            max_table_elements: None,
            max_instances: None,
            max_tables: None,
            max_memories: None,
        }
    }
}

/// Statistics tracked by the resource limiter
#[derive(Debug, Clone, Default)]
pub struct ResourceLimiterStats {
    /// Total memory allocated in bytes
    pub total_memory_bytes: u64,
    /// Total table elements allocated
    pub total_table_elements: u64,
    /// Number of memory grow requests
    pub memory_grow_requests: u64,
    /// Number of memory grow denials
    pub memory_grow_denials: u64,
    /// Number of table grow requests
    pub table_grow_requests: u64,
    /// Number of table grow denials
    pub table_grow_denials: u64,
}

/// A resource limiter that tracks and enforces resource limits
#[derive(Debug)]
pub struct StoreLimiter {
    /// Configuration for limits
    config: ResourceLimiterConfig,
    /// Current statistics
    stats: RwLock<ResourceLimiterStats>,
    /// Unique identifier
    id: u64,
}

/// Global counter for limiter IDs
static LIMITER_ID_COUNTER: AtomicU64 = AtomicU64::new(1);

/// Registry for active resource limiters
lazy_static::lazy_static! {
    static ref LIMITER_REGISTRY: RwLock<HashMap<u64, Arc<StoreLimiter>>> = RwLock::new(HashMap::new());
}

impl StoreLimiter {
    /// Create a new resource limiter with the given configuration
    pub fn new(config: ResourceLimiterConfig) -> Self {
        let id = LIMITER_ID_COUNTER.fetch_add(1, Ordering::SeqCst);
        StoreLimiter {
            config,
            stats: RwLock::new(ResourceLimiterStats::default()),
            id,
        }
    }

    /// Get the limiter ID
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Get the configuration
    pub fn config(&self) -> &ResourceLimiterConfig {
        &self.config
    }

    /// Get current statistics
    pub fn stats(&self) -> ResourceLimiterStats {
        self.stats.read().unwrap_or_else(|e| e.into_inner()).clone()
    }

    /// Check if memory growth should be allowed
    ///
    /// # Arguments
    /// * `current_pages` - Current memory size in pages
    /// * `requested_pages` - Number of pages being requested
    ///
    /// # Returns
    /// `true` if growth should be allowed, `false` otherwise
    pub fn allow_memory_grow(&self, current_pages: u64, requested_pages: u64) -> bool {
        let mut stats = self.stats.write().unwrap_or_else(|e| e.into_inner());
        stats.memory_grow_requests += 1;

        let new_size_pages = current_pages.saturating_add(requested_pages);
        let _new_size_bytes = new_size_pages.saturating_mul(65536); // 64KB per page

        // Check page limit
        if let Some(max_pages) = self.config.max_memory_pages {
            if new_size_pages > max_pages {
                stats.memory_grow_denials += 1;
                return false;
            }
        }

        // Check byte limit
        if let Some(max_bytes) = self.config.max_memory_bytes {
            let potential_total = stats.total_memory_bytes.saturating_add(requested_pages * 65536);
            if potential_total > max_bytes {
                stats.memory_grow_denials += 1;
                return false;
            }
        }

        // Update stats
        stats.total_memory_bytes = stats.total_memory_bytes.saturating_add(requested_pages * 65536);
        true
    }

    /// Check if table growth should be allowed
    ///
    /// # Arguments
    /// * `current_elements` - Current table size in elements
    /// * `requested_elements` - Number of elements being requested
    ///
    /// # Returns
    /// `true` if growth should be allowed, `false` otherwise
    pub fn allow_table_grow(&self, current_elements: u64, requested_elements: u64) -> bool {
        let mut stats = self.stats.write().unwrap_or_else(|e| e.into_inner());
        stats.table_grow_requests += 1;

        let new_size = current_elements.saturating_add(requested_elements);

        // Check element limit
        if let Some(max_elements) = self.config.max_table_elements {
            if new_size > max_elements {
                stats.table_grow_denials += 1;
                return false;
            }
        }

        // Update stats
        stats.total_table_elements = stats.total_table_elements.saturating_add(requested_elements);
        true
    }

    /// Reset statistics
    pub fn reset_stats(&self) {
        let mut stats = self.stats.write().unwrap_or_else(|e| e.into_inner());
        *stats = ResourceLimiterStats::default();
    }
}

/// Register a limiter in the global registry
pub fn register_limiter(limiter: StoreLimiter) -> WasmtimeResult<u64> {
    let id = limiter.id();
    let mut registry = LIMITER_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire limiter registry lock".to_string(),
    })?;
    registry.insert(id, Arc::new(limiter));
    Ok(id)
}

/// Get a limiter from the registry
pub fn get_limiter(id: u64) -> WasmtimeResult<Arc<StoreLimiter>> {
    let registry = LIMITER_REGISTRY.read().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire limiter registry lock".to_string(),
    })?;
    registry.get(&id).cloned().ok_or_else(|| WasmtimeError::InvalidParameter {
        message: format!("Limiter with ID {} not found", id),
    })
}

/// Remove a limiter from the registry
pub fn unregister_limiter(id: u64) -> WasmtimeResult<()> {
    let mut registry = LIMITER_REGISTRY.write().map_err(|_| WasmtimeError::Concurrency {
        message: "Failed to acquire limiter registry lock".to_string(),
    })?;
    registry.remove(&id);
    Ok(())
}

// ============================================================================
// FFI Functions for Panama and JNI
// ============================================================================

/// Create a new resource limiter with configuration
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_create(
    max_memory_bytes: c_longlong,
    max_memory_pages: c_longlong,
    max_table_elements: c_longlong,
    max_instances: c_int,
    max_tables: c_int,
    max_memories: c_int,
) -> c_longlong {
    let config = ResourceLimiterConfig {
        max_memory_bytes: if max_memory_bytes < 0 { None } else { Some(max_memory_bytes as u64) },
        max_memory_pages: if max_memory_pages < 0 { None } else { Some(max_memory_pages as u64) },
        max_table_elements: if max_table_elements < 0 { None } else { Some(max_table_elements as u64) },
        max_instances: if max_instances < 0 { None } else { Some(max_instances as u32) },
        max_tables: if max_tables < 0 { None } else { Some(max_tables as u32) },
        max_memories: if max_memories < 0 { None } else { Some(max_memories as u32) },
    };

    let limiter = StoreLimiter::new(config);
    match register_limiter(limiter) {
        Ok(id) => id as c_longlong,
        Err(_) => -1,
    }
}

/// Create a default resource limiter
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_create_default() -> c_longlong {
    let limiter = StoreLimiter::new(ResourceLimiterConfig::default());
    match register_limiter(limiter) {
        Ok(id) => id as c_longlong,
        Err(_) => -1,
    }
}

/// Free a resource limiter
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_free(limiter_id: c_longlong) -> c_int {
    if limiter_id < 0 {
        return -1;
    }
    match unregister_limiter(limiter_id as u64) {
        Ok(()) => 0,
        Err(_) => -1,
    }
}

/// Check if memory growth should be allowed
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_allow_memory_grow(
    limiter_id: c_longlong,
    current_pages: c_longlong,
    requested_pages: c_longlong,
) -> c_int {
    if limiter_id < 0 || current_pages < 0 || requested_pages < 0 {
        return 0; // Deny on invalid input
    }

    match get_limiter(limiter_id as u64) {
        Ok(limiter) => {
            if limiter.allow_memory_grow(current_pages as u64, requested_pages as u64) {
                1
            } else {
                0
            }
        }
        Err(_) => 0, // Deny if limiter not found
    }
}

/// Check if table growth should be allowed
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_allow_table_grow(
    limiter_id: c_longlong,
    current_elements: c_longlong,
    requested_elements: c_longlong,
) -> c_int {
    if limiter_id < 0 || current_elements < 0 || requested_elements < 0 {
        return 0; // Deny on invalid input
    }

    match get_limiter(limiter_id as u64) {
        Ok(limiter) => {
            if limiter.allow_table_grow(current_elements as u64, requested_elements as u64) {
                1
            } else {
                0
            }
        }
        Err(_) => 0, // Deny if limiter not found
    }
}

/// Get limiter statistics as JSON
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_get_stats_json(limiter_id: c_longlong) -> *mut std::ffi::c_char {
    if limiter_id < 0 {
        return std::ptr::null_mut();
    }

    match get_limiter(limiter_id as u64) {
        Ok(limiter) => {
            let stats = limiter.stats();
            let json = serde_json::json!({
                "total_memory_bytes": stats.total_memory_bytes,
                "total_table_elements": stats.total_table_elements,
                "memory_grow_requests": stats.memory_grow_requests,
                "memory_grow_denials": stats.memory_grow_denials,
                "table_grow_requests": stats.table_grow_requests,
                "table_grow_denials": stats.table_grow_denials,
            });
            match std::ffi::CString::new(json.to_string()) {
                Ok(cstr) => cstr.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

/// Free a string returned by limiter functions
///
/// # Safety
/// This function is unsafe because it dereferences the pointer
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_string_free(s: *mut std::ffi::c_char) {
    if !s.is_null() {
        drop(std::ffi::CString::from_raw(s));
    }
}

/// Reset limiter statistics
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_reset_stats(limiter_id: c_longlong) -> c_int {
    if limiter_id < 0 {
        return -1;
    }

    match get_limiter(limiter_id as u64) {
        Ok(limiter) => {
            limiter.reset_stats();
            0
        }
        Err(_) => -1,
    }
}

/// Get the number of registered limiters
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_get_count() -> c_int {
    match LIMITER_REGISTRY.read() {
        Ok(registry) => registry.len() as c_int,
        Err(_) => -1,
    }
}

/// Get limiter configuration as JSON
///
/// # Safety
/// This function is unsafe because it's called from FFI
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_limiter_get_config_json(limiter_id: c_longlong) -> *mut std::ffi::c_char {
    if limiter_id < 0 {
        return std::ptr::null_mut();
    }

    match get_limiter(limiter_id as u64) {
        Ok(limiter) => {
            let config = limiter.config();
            let json = serde_json::json!({
                "max_memory_bytes": config.max_memory_bytes,
                "max_memory_pages": config.max_memory_pages,
                "max_table_elements": config.max_table_elements,
                "max_instances": config.max_instances,
                "max_tables": config.max_tables,
                "max_memories": config.max_memories,
            });
            match std::ffi::CString::new(json.to_string()) {
                Ok(cstr) => cstr.into_raw(),
                Err(_) => std::ptr::null_mut(),
            }
        }
        Err(_) => std::ptr::null_mut(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_limiter_creation() {
        let config = ResourceLimiterConfig {
            max_memory_bytes: Some(1024 * 1024), // 1MB
            max_memory_pages: Some(16), // 1MB in pages
            max_table_elements: Some(1000),
            max_instances: Some(10),
            max_tables: Some(5),
            max_memories: Some(3),
        };

        let limiter = StoreLimiter::new(config);
        assert!(limiter.id() > 0);
    }

    #[test]
    fn test_memory_limit_enforcement() {
        let config = ResourceLimiterConfig {
            max_memory_pages: Some(10),
            ..Default::default()
        };

        let limiter = StoreLimiter::new(config);

        // Should allow growth within limits
        assert!(limiter.allow_memory_grow(0, 5));

        // Should allow more growth within limits
        assert!(limiter.allow_memory_grow(5, 5));

        // Should deny growth beyond limits
        assert!(!limiter.allow_memory_grow(10, 1));

        let stats = limiter.stats();
        assert_eq!(stats.memory_grow_requests, 3);
        assert_eq!(stats.memory_grow_denials, 1);
    }

    #[test]
    fn test_table_limit_enforcement() {
        let config = ResourceLimiterConfig {
            max_table_elements: Some(100),
            ..Default::default()
        };

        let limiter = StoreLimiter::new(config);

        // Should allow growth within limits
        assert!(limiter.allow_table_grow(0, 50));

        // Should allow more growth within limits
        assert!(limiter.allow_table_grow(50, 50));

        // Should deny growth beyond limits
        assert!(!limiter.allow_table_grow(100, 1));

        let stats = limiter.stats();
        assert_eq!(stats.table_grow_requests, 3);
        assert_eq!(stats.table_grow_denials, 1);
    }

    #[test]
    fn test_limiter_registry() {
        let config = ResourceLimiterConfig::default();
        let limiter = StoreLimiter::new(config);
        let id = register_limiter(limiter).expect("Failed to register limiter");

        // Should be able to get the limiter
        let retrieved = get_limiter(id).expect("Failed to get limiter");
        assert_eq!(retrieved.id(), id);

        // Should be able to unregister
        unregister_limiter(id).expect("Failed to unregister limiter");

        // Should fail to get after unregistering
        assert!(get_limiter(id).is_err());
    }

    #[test]
    fn test_reset_stats() {
        let config = ResourceLimiterConfig {
            max_memory_pages: Some(100),
            ..Default::default()
        };

        let limiter = StoreLimiter::new(config);

        // Generate some stats
        limiter.allow_memory_grow(0, 10);
        limiter.allow_memory_grow(10, 20);

        let stats = limiter.stats();
        assert!(stats.memory_grow_requests > 0);

        // Reset stats
        limiter.reset_stats();

        let stats = limiter.stats();
        assert_eq!(stats.memory_grow_requests, 0);
        assert_eq!(stats.total_memory_bytes, 0);
    }
}
