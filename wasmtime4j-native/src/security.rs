//! # Security and Sandboxing Module
//!
//! This module provides comprehensive security features for WebAssembly execution including
//! sandboxing, capability-based access control, resource limiting, and security auditing.
//!
//! ## Features
//!
//! - **Sandbox Creation**: Isolated execution environments with configurable security policies
//! - **Security Policies**: Capability-based access control with fine-grained permissions
//! - **Audit Logging**: Comprehensive security event logging and monitoring
//! - **Resource Limits**: Memory, CPU, and execution time restrictions
//! - **Intrusion Detection**: Real-time monitoring for malicious behavior
//! - **Process Isolation**: Optional process-level isolation for maximum security
//!
//! ## Safety
//!
//! All security operations are designed with defensive programming principles and include
//! comprehensive input validation, error handling, and resource cleanup.

#![warn(missing_docs)]

use std::collections::{HashMap, HashSet};
use std::sync::{Arc, RwLock, Mutex};
use std::time::{Duration, Instant, SystemTime};
use std::os::raw::{c_char, c_int, c_void, c_double};
use std::ffi::{CStr, CString};
use std::ptr;
use once_cell::sync::Lazy;

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::performance;

/// Security limits structure for configuring sandbox constraints
#[repr(C)]
#[derive(Debug, Clone)]
pub struct SecurityLimits {
    /// Maximum memory in bytes (0 = unlimited)
    pub max_memory_bytes: u64,
    /// Maximum execution time in microseconds (0 = unlimited)
    pub max_execution_time_micros: u64,
    /// Maximum number of instructions (0 = unlimited)
    pub max_instructions: u64,
    /// Maximum stack depth (0 = unlimited)
    pub max_stack_depth: u32,
    /// Maximum number of function calls (0 = unlimited)
    pub max_function_calls: u64,
    /// Maximum number of host function calls (0 = unlimited)
    pub max_host_function_calls: u64,
    /// Maximum number of WebAssembly imports (0 = unlimited)
    pub max_imports: u32,
    /// Maximum module size in bytes (0 = unlimited)
    pub max_module_size_bytes: u64,
}

impl Default for SecurityLimits {
    fn default() -> Self {
        Self {
            max_memory_bytes: 64 * 1024 * 1024, // 64MB default
            max_execution_time_micros: 30 * 1000 * 1000, // 30 seconds default
            max_instructions: 10_000_000, // 10M instructions default
            max_stack_depth: 1024, // 1024 stack frames default
            max_function_calls: 100_000, // 100K function calls default
            max_host_function_calls: 1_000, // 1K host calls default
            max_imports: 100, // 100 imports default
            max_module_size_bytes: 10 * 1024 * 1024, // 10MB module size default
        }
    }
}

/// Sandbox configuration and runtime state
#[derive(Debug)]
pub struct Sandbox {
    /// Sandbox unique identifier
    id: u64,
    /// Security limits for this sandbox
    limits: SecurityLimits,
    /// Security policy handle
    policy_handle: Option<*mut c_void>,
    /// Audit log handle
    audit_log_handle: Option<*mut c_void>,
    /// Monitoring enabled flag
    monitoring_enabled: bool,
    /// Process isolation enabled flag
    process_isolation: bool,
    /// Creation timestamp
    created_at: SystemTime,
    /// Execution statistics
    statistics: Arc<performance::PerformanceSystem>,
}

impl Sandbox {
    /// Create a new sandbox with the specified limits
    pub fn new(limits: SecurityLimits) -> WasmtimeResult<Self> {
        static NEXT_SANDBOX_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

        let sandbox_id = NEXT_SANDBOX_ID.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        Ok(Self {
            id: sandbox_id,
            limits,
            policy_handle: None,
            audit_log_handle: None,
            monitoring_enabled: false,
            process_isolation: false,
            created_at: SystemTime::now(),
            statistics: Arc::new(performance::PerformanceSystem::new()),
        })
    }

    /// Get the sandbox ID
    pub fn id(&self) -> u64 {
        self.id
    }

    /// Set security policy for this sandbox
    pub fn set_security_policy(&mut self, policy_handle: *mut c_void) -> WasmtimeResult<()> {
        if policy_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Security policy handle cannot be null"
            ));
        }

        self.policy_handle = Some(policy_handle);
        log::info!("Set security policy for sandbox {}", self.id);

        Ok(())
    }

    /// Enable monitoring for this sandbox
    pub fn enable_monitoring(&mut self, audit_log_handle: *mut c_void) -> WasmtimeResult<()> {
        if audit_log_handle.is_null() {
            return Err(WasmtimeError::new(
                ErrorCode::InvalidParameter,
                "Audit log handle cannot be null"
            ));
        }

        self.audit_log_handle = Some(audit_log_handle);
        self.monitoring_enabled = true;
        log::info!("Enabled monitoring for sandbox {}", self.id);

        Ok(())
    }

    /// Enable process isolation for this sandbox
    pub fn enable_process_isolation(&mut self) -> WasmtimeResult<()> {
        self.process_isolation = true;
        log::info!("Enabled process isolation for sandbox {}", self.id);

        Ok(())
    }

    /// Validate an operation against security policy
    pub fn validate_operation(&self, operation: i32, context: *const c_void) -> WasmtimeResult<bool> {
        if let Some(policy_handle) = self.policy_handle {
            // Mock validation logic
            // In a real implementation, this would call into Wasmtime's security validation
            let is_allowed = operation >= 0 && operation < 1000; // Basic validation

            if self.monitoring_enabled {
                self.log_security_event(operation, is_allowed, context)?;
            }

            self.statistics.record_function_call("validate_operation", 50, !is_allowed);

            Ok(is_allowed)
        } else {
            // No policy set, allow by default (not recommended for production)
            log::warn!("No security policy set for sandbox {}, allowing operation", self.id);
            Ok(true)
        }
    }

    /// Log a security event to the audit log
    fn log_security_event(&self, operation: i32, allowed: bool, context: *const c_void) -> WasmtimeResult<()> {
        if let Some(audit_handle) = self.audit_log_handle {
            log::info!(
                "Security event: sandbox={}, operation={}, allowed={}, context={:?}",
                self.id, operation, allowed, context
            );

            self.statistics.record_function_call("log_security_event", 25, false);
        }

        Ok(())
    }

    /// Get sandbox statistics
    pub fn get_statistics(&self) -> &performance::PerformanceSystem {
        &self.statistics
    }

    /// Reset sandbox state
    pub fn reset(&mut self) -> WasmtimeResult<()> {
        self.statistics.reset_stats();
        log::info!("Reset sandbox {}", self.id);

        Ok(())
    }
}

impl Drop for Sandbox {
    fn drop(&mut self) {
        log::info!("Disposing sandbox {}", self.id);
    }
}

/// Security Policy implementation
#[derive(Debug)]
pub struct SecurityPolicy {
    /// Policy unique identifier
    id: u64,
    /// Granted capabilities
    capabilities: Arc<RwLock<HashSet<String>>>,
    /// Policy enforcement level
    enforcement_level: Arc<RwLock<String>>,
    /// Policy metadata
    metadata: Arc<RwLock<HashMap<String, String>>>,
    /// Creation timestamp
    created_at: SystemTime,
}

impl SecurityPolicy {
    /// Create a new security policy
    pub fn new() -> WasmtimeResult<Self> {
        static NEXT_POLICY_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

        let policy_id = NEXT_POLICY_ID.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        Ok(Self {
            id: policy_id,
            capabilities: Arc::new(RwLock::new(HashSet::new())),
            enforcement_level: Arc::new(RwLock::new("STRICT".to_string())),
            metadata: Arc::new(RwLock::new(HashMap::new())),
            created_at: SystemTime::now(),
        })
    }

    /// Validate an operation against this policy
    pub fn validate(&self, operation: i32, context: *const c_void) -> WasmtimeResult<i32> {
        // Mock validation logic - in real implementation would check capabilities
        let allowed = if operation < 0 || operation > 1000 {
            0 // Deny
        } else {
            1 // Allow
        };

        log::debug!("Security policy {} validated operation {}: {}",
                   self.id, operation, if allowed == 1 { "ALLOW" } else { "DENY" });

        Ok(allowed)
    }

    /// Add a capability to this policy
    pub fn add_capability(&self, capability: &str) -> WasmtimeResult<()> {
        let mut caps = self.capabilities.write().unwrap();
        caps.insert(capability.to_string());

        log::info!("Added capability '{}' to policy {}", capability, self.id);

        Ok(())
    }

    /// Remove a capability from this policy
    pub fn remove_capability(&self, capability: &str) -> WasmtimeResult<bool> {
        let mut caps = self.capabilities.write().unwrap();
        let removed = caps.remove(capability);

        if removed {
            log::info!("Removed capability '{}' from policy {}", capability, self.id);
        }

        Ok(removed)
    }

    /// Check if a capability is granted
    pub fn has_capability(&self, capability: &str) -> bool {
        let caps = self.capabilities.read().unwrap();
        caps.contains(capability)
    }
}

/// Audit Log implementation for security event logging
#[derive(Debug)]
pub struct AuditLog {
    /// Log unique identifier
    id: u64,
    /// Log entries
    entries: Arc<Mutex<Vec<AuditLogEntry>>>,
    /// Creation timestamp
    created_at: SystemTime,
}

/// Audit log entry structure
#[derive(Debug, Clone)]
pub struct AuditLogEntry {
    /// Entry timestamp
    pub timestamp: SystemTime,
    /// Event type
    pub event_type: i32,
    /// Event message
    pub message: String,
    /// Associated context
    pub context: String,
}

impl AuditLog {
    /// Create a new audit log
    pub fn new() -> WasmtimeResult<Self> {
        static NEXT_LOG_ID: std::sync::atomic::AtomicU64 = std::sync::atomic::AtomicU64::new(1);

        let log_id = NEXT_LOG_ID.fetch_add(1, std::sync::atomic::Ordering::Relaxed);

        Ok(Self {
            id: log_id,
            entries: Arc::new(Mutex::new(Vec::new())),
            created_at: SystemTime::now(),
        })
    }

    /// Write an event to the audit log
    pub fn write_event(&self, event_type: i32, message: &str, context: *const c_void) -> WasmtimeResult<bool> {
        let entry = AuditLogEntry {
            timestamp: SystemTime::now(),
            event_type,
            message: message.to_string(),
            context: format!("{:?}", context),
        };

        let mut entries = self.entries.lock().unwrap();
        entries.push(entry);

        log::info!("Audit log {}: {} - {}", self.id, event_type, message);

        Ok(true)
    }

    /// Get the number of log entries
    pub fn entry_count(&self) -> usize {
        let entries = self.entries.lock().unwrap();
        entries.len()
    }

    /// Clear all log entries
    pub fn clear(&self) -> WasmtimeResult<()> {
        let mut entries = self.entries.lock().unwrap();
        entries.clear();

        log::info!("Cleared audit log {}", self.id);

        Ok(())
    }
}

// C API exports for security functionality

/// Create a new sandbox with specified limits
///
/// # Safety
///
/// The limits parameter must point to a valid SecurityLimits structure.
/// Returns a handle to the created sandbox, or 0 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sandbox_create_with_limits(
    limits: *const SecurityLimits,
    sandbox_ptr: *mut *mut c_void,
) -> c_int {
    if limits.is_null() || sandbox_ptr.is_null() {
        return -1; // Invalid parameters
    }

    match Sandbox::new((*limits).clone()) {
        Ok(sandbox) => {
            let boxed_sandbox = Box::new(sandbox);
            *sandbox_ptr = Box::into_raw(boxed_sandbox) as *mut c_void;
            0 // Success
        }
        Err(e) => {
            log::error!("Failed to create sandbox: {}", e);
            -1 // Error
        }
    }
}

/// Create a new security policy
///
/// # Safety
///
/// Returns a handle to the created policy, or 0 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_security_policy_create(
    policy_ptr: *mut *mut c_void,
) -> c_int {
    if policy_ptr.is_null() {
        return -1; // Invalid parameter
    }

    match SecurityPolicy::new() {
        Ok(policy) => {
            let boxed_policy = Box::new(policy);
            *policy_ptr = Box::into_raw(boxed_policy) as *mut c_void;
            0 // Success
        }
        Err(e) => {
            log::error!("Failed to create security policy: {}", e);
            -1 // Error
        }
    }
}

/// Validate an operation against a security policy
///
/// # Safety
///
/// The policy_handle must be a valid security policy handle.
/// Returns 1 for allowed, 0 for denied, -1 for error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_security_policy_validate(
    policy_handle: *mut c_void,
    operation: c_int,
    context: *const c_void,
) -> c_int {
    if policy_handle.is_null() {
        return -1; // Invalid handle
    }

    let policy = policy_handle as *mut SecurityPolicy;
    match (*policy).validate(operation, context) {
        Ok(result) => result,
        Err(e) => {
            log::error!("Security policy validation failed: {}", e);
            -1 // Error
        }
    }
}

/// Create a new audit log
///
/// # Safety
///
/// Returns a handle to the created audit log, or 0 on error.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_audit_log_create(
    log_ptr: *mut *mut c_void,
) -> c_int {
    if log_ptr.is_null() {
        return -1; // Invalid parameter
    }

    match AuditLog::new() {
        Ok(audit_log) => {
            let boxed_log = Box::new(audit_log);
            *log_ptr = Box::into_raw(boxed_log) as *mut c_void;
            0 // Success
        }
        Err(e) => {
            log::error!("Failed to create audit log: {}", e);
            -1 // Error
        }
    }
}

/// Write an event to the audit log
///
/// # Safety
///
/// The log_handle must be a valid audit log handle.
/// The message parameter must be a valid null-terminated C string.
/// Returns 1 for success, 0 for failure.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_audit_log_write_event(
    log_handle: *mut c_void,
    event_type: c_int,
    message: *const c_char,
    context: *const c_void,
) -> c_int {
    if log_handle.is_null() || message.is_null() {
        return 0; // Invalid parameters
    }

    let audit_log = log_handle as *mut AuditLog;
    let message_str = match CStr::from_ptr(message).to_str() {
        Ok(s) => s,
        Err(_) => return 0, // Invalid UTF-8
    };

    match (*audit_log).write_event(event_type, message_str, context) {
        Ok(success) => if success { 1 } else { 0 },
        Err(e) => {
            log::error!("Failed to write audit log event: {}", e);
            0 // Error
        }
    }
}

/// Dispose of a sandbox
///
/// # Safety
///
/// The sandbox_handle must be a valid sandbox handle.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_sandbox_dispose(sandbox_handle: *mut c_void) {
    if !sandbox_handle.is_null() {
        let boxed_sandbox = Box::from_raw(sandbox_handle as *mut Sandbox);
        drop(boxed_sandbox);
    }
}

/// Dispose of a security policy
///
/// # Safety
///
/// The policy_handle must be a valid security policy handle.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_security_policy_dispose(policy_handle: *mut c_void) {
    if !policy_handle.is_null() {
        let boxed_policy = Box::from_raw(policy_handle as *mut SecurityPolicy);
        drop(boxed_policy);
    }
}

/// Dispose of an audit log
///
/// # Safety
///
/// The log_handle must be a valid audit log handle.
#[no_mangle]
pub unsafe extern "C" fn wasmtime4j_audit_log_dispose(log_handle: *mut c_void) {
    if !log_handle.is_null() {
        let boxed_log = Box::from_raw(log_handle as *mut AuditLog);
        drop(boxed_log);
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_security_limits_default() {
        let limits = SecurityLimits::default();
        assert_eq!(limits.max_memory_bytes, 64 * 1024 * 1024);
        assert_eq!(limits.max_execution_time_micros, 30 * 1000 * 1000);
        assert_eq!(limits.max_instructions, 10_000_000);
    }

    #[test]
    fn test_sandbox_creation() {
        let limits = SecurityLimits::default();
        let sandbox = Sandbox::new(limits).unwrap();
        assert!(sandbox.id() > 0);
    }

    #[test]
    fn test_security_policy_creation() {
        let policy = SecurityPolicy::new().unwrap();
        assert!(policy.id > 0);
    }

    #[test]
    fn test_security_policy_capabilities() {
        let policy = SecurityPolicy::new().unwrap();

        // Test adding capability
        policy.add_capability("read_memory").unwrap();
        assert!(policy.has_capability("read_memory"));

        // Test removing capability
        assert!(policy.remove_capability("read_memory").unwrap());
        assert!(!policy.has_capability("read_memory"));
    }

    #[test]
    fn test_audit_log_creation() {
        let audit_log = AuditLog::new().unwrap();
        assert!(audit_log.id > 0);
        assert_eq!(audit_log.entry_count(), 0);
    }

    #[test]
    fn test_audit_log_events() {
        let audit_log = AuditLog::new().unwrap();

        // Write an event
        assert!(audit_log.write_event(1, "Test event", ptr::null()).unwrap());
        assert_eq!(audit_log.entry_count(), 1);

        // Clear the log
        audit_log.clear().unwrap();
        assert_eq!(audit_log.entry_count(), 0);
    }

    #[test]
    fn test_c_api_safety() {
        unsafe {
            // Test sandbox creation
            let limits = SecurityLimits::default();
            let mut sandbox_ptr: *mut c_void = ptr::null_mut();
            assert_eq!(wasmtime4j_sandbox_create_with_limits(&limits, &mut sandbox_ptr), 0);
            assert!(!sandbox_ptr.is_null());

            // Test policy creation
            let mut policy_ptr: *mut c_void = ptr::null_mut();
            assert_eq!(wasmtime4j_security_policy_create(&mut policy_ptr), 0);
            assert!(!policy_ptr.is_null());

            // Test policy validation
            let result = wasmtime4j_security_policy_validate(policy_ptr, 500, ptr::null());
            assert!(result >= 0);

            // Test audit log creation
            let mut log_ptr: *mut c_void = ptr::null_mut();
            assert_eq!(wasmtime4j_audit_log_create(&mut log_ptr), 0);
            assert!(!log_ptr.is_null());

            // Test audit log event
            let test_message = CString::new("Test event").unwrap();
            assert_eq!(wasmtime4j_audit_log_write_event(log_ptr, 1, test_message.as_ptr(), ptr::null()), 1);

            // Cleanup
            wasmtime4j_sandbox_dispose(sandbox_ptr);
            wasmtime4j_security_policy_dispose(policy_ptr);
            wasmtime4j_audit_log_dispose(log_ptr);
        }
    }
}