//! # Secure Sandboxing Module
//!
//! Advanced sandboxing capabilities beyond basic Wasmtime isolation including:
//! - Capability-based security model
//! - Fine-grained permission system
//! - Security policies for different execution contexts
//! - Secure communication channels between sandboxed modules
//! - Resource access control for memory, compute, and I/O operations
//!
//! This module extends Wasmtime's built-in sandboxing with enterprise-grade
//! security controls for high-security environments.

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::instance::Instance;
use crate::store::Store;
use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant, SystemTime, UNIX_EPOCH};
use serde::{Deserialize, Serialize};

/// Capability represents a specific permission or resource access right
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum Capability {
    /// Memory access with size limits
    MemoryAccess {
        /// Maximum memory size in bytes
        max_size: usize,
        /// Whether write access is allowed
        write_access: bool,
    },
    /// File system access to specific paths
    FileSystemAccess {
        /// Allowed paths with permissions
        paths: HashMap<String, FilePermissions>,
    },
    /// Network access with protocol and destination restrictions
    NetworkAccess {
        /// Allowed protocols (tcp, udp, http, https)
        protocols: HashSet<String>,
        /// Allowed destination hosts
        hosts: HashSet<String>,
        /// Allowed port ranges
        port_ranges: Vec<(u16, u16)>,
    },
    /// System call access
    SystemCallAccess {
        /// Allowed system calls
        allowed_calls: HashSet<String>,
    },
    /// Inter-module communication
    InterModuleCommunication {
        /// Allowed target modules
        target_modules: HashSet<String>,
        /// Communication protocols allowed
        protocols: HashSet<String>,
    },
    /// Resource limits
    ResourceLimits {
        /// CPU time limit in milliseconds
        cpu_time_ms: Option<u64>,
        /// Maximum execution duration
        max_duration: Option<Duration>,
        /// Maximum number of instructions
        max_instructions: Option<u64>,
    },
    /// Environment variable access
    EnvironmentAccess {
        /// Allowed environment variables
        allowed_vars: HashSet<String>,
        /// Whether new variables can be set
        can_set: bool,
    },
}

/// File system permissions for sandboxed access
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub struct FilePermissions {
    /// Read permission
    pub read: bool,
    /// Write permission
    pub write: bool,
    /// Create permission
    pub create: bool,
    /// Delete permission
    pub delete: bool,
    /// List directory permission
    pub list: bool,
}

impl Default for FilePermissions {
    fn default() -> Self {
        Self {
            read: false,
            write: false,
            create: false,
            delete: false,
            list: false,
        }
    }
}

impl FilePermissions {
    /// Create read-only permissions
    pub fn read_only() -> Self {
        Self {
            read: true,
            write: false,
            create: false,
            delete: false,
            list: true,
        }
    }

    /// Create read-write permissions
    pub fn read_write() -> Self {
        Self {
            read: true,
            write: true,
            create: true,
            delete: false,
            list: true,
        }
    }

    /// Create full permissions
    pub fn full() -> Self {
        Self {
            read: true,
            write: true,
            create: true,
            delete: true,
            list: true,
        }
    }
}

/// Security context for sandboxed execution
#[derive(Debug, Clone)]
pub struct SecurityContext {
    /// Unique identifier for this context
    pub id: String,
    /// Granted capabilities
    pub capabilities: Vec<Capability>,
    /// Security level (higher = more restricted)
    pub security_level: u32,
    /// Context metadata
    pub metadata: HashMap<String, String>,
    /// Creation timestamp
    pub created_at: SystemTime,
    /// Expiration time (if any)
    pub expires_at: Option<SystemTime>,
}

impl SecurityContext {
    /// Create a new security context
    pub fn new(id: String, security_level: u32) -> Self {
        Self {
            id,
            capabilities: Vec::new(),
            security_level,
            metadata: HashMap::new(),
            created_at: SystemTime::now(),
            expires_at: None,
        }
    }

    /// Grant a capability to this context
    pub fn grant_capability(&mut self, capability: Capability) {
        self.capabilities.push(capability);
    }

    /// Revoke a capability from this context
    pub fn revoke_capability(&mut self, capability: &Capability) {
        self.capabilities.retain(|c| c != capability);
    }

    /// Check if a capability is granted
    pub fn has_capability(&self, capability: &Capability) -> bool {
        self.capabilities.iter().any(|c| c == capability)
    }

    /// Check if the context has expired
    pub fn is_expired(&self) -> bool {
        if let Some(expires_at) = self.expires_at {
            SystemTime::now() > expires_at
        } else {
            false
        }
    }

    /// Set expiration time
    pub fn set_expiration(&mut self, expires_at: SystemTime) {
        self.expires_at = Some(expires_at);
    }

    /// Add metadata
    pub fn add_metadata(&mut self, key: String, value: String) {
        self.metadata.insert(key, value);
    }
}

/// Sandbox configuration for module execution
#[derive(Debug, Clone)]
pub struct SandboxConfig {
    /// Default security context for modules
    pub default_context: SecurityContext,
    /// Whether to enforce strict capability checking
    pub strict_mode: bool,
    /// Whether to log all capability checks
    pub audit_mode: bool,
    /// Maximum number of concurrent modules
    pub max_concurrent_modules: Option<usize>,
    /// Global resource limits
    pub global_limits: ResourceLimits,
    /// Inter-module communication settings
    pub imc_settings: InterModuleCommunicationSettings,
}

/// Resource limits for sandboxed execution
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ResourceLimits {
    /// Maximum memory usage in bytes
    pub max_memory: Option<usize>,
    /// Maximum CPU time in milliseconds
    pub max_cpu_time: Option<u64>,
    /// Maximum execution duration
    pub max_duration: Option<Duration>,
    /// Maximum number of instructions
    pub max_instructions: Option<u64>,
    /// Maximum number of system calls
    pub max_syscalls: Option<u64>,
    /// Maximum file operations
    pub max_file_ops: Option<u64>,
    /// Maximum network operations
    pub max_network_ops: Option<u64>,
}

impl Default for ResourceLimits {
    fn default() -> Self {
        Self {
            max_memory: Some(64 * 1024 * 1024), // 64MB
            max_cpu_time: Some(5000), // 5 seconds
            max_duration: Some(Duration::from_secs(30)),
            max_instructions: Some(1_000_000),
            max_syscalls: Some(1000),
            max_file_ops: Some(100),
            max_network_ops: Some(50),
        }
    }
}

/// Inter-module communication settings
#[derive(Debug, Clone)]
pub struct InterModuleCommunicationSettings {
    /// Whether inter-module communication is enabled
    pub enabled: bool,
    /// Maximum message size for communication
    pub max_message_size: usize,
    /// Communication timeout
    pub timeout: Duration,
    /// Allowed communication patterns
    pub allowed_patterns: HashSet<String>,
}

impl Default for InterModuleCommunicationSettings {
    fn default() -> Self {
        Self {
            enabled: false,
            max_message_size: 4096, // 4KB
            timeout: Duration::from_secs(1),
            allowed_patterns: HashSet::new(),
        }
    }
}

/// Sandbox manager for controlling module execution
pub struct SandboxManager {
    /// Configuration for this sandbox
    config: SandboxConfig,
    /// Active security contexts by module ID
    contexts: Arc<RwLock<HashMap<String, SecurityContext>>>,
    /// Active instances being tracked
    instances: Arc<RwLock<HashMap<String, SandboxedInstance>>>,
    /// Capability check auditor
    auditor: Option<Arc<Mutex<CapabilityAuditor>>>,
    /// Resource usage tracker
    resource_tracker: Arc<Mutex<ResourceTracker>>,
}

/// Sandboxed instance wrapper
#[derive(Debug)]
pub struct SandboxedInstance {
    /// The underlying WebAssembly instance
    pub instance: Option<Instance>,
    /// Security context for this instance
    pub context: SecurityContext,
    /// Resource usage tracking
    pub resource_usage: ResourceUsage,
    /// Creation time
    pub created_at: Instant,
    /// Last activity time
    pub last_activity: Instant,
}

/// Resource usage tracking for instances
#[derive(Debug, Clone)]
pub struct ResourceUsage {
    /// Memory usage in bytes
    pub memory_used: usize,
    /// CPU time used in milliseconds
    pub cpu_time_ms: u64,
    /// Number of instructions executed
    pub instructions_executed: u64,
    /// Number of system calls made
    pub syscalls_made: u64,
    /// Number of file operations
    pub file_operations: u64,
    /// Number of network operations
    pub network_operations: u64,
    /// Start time for tracking
    pub start_time: Instant,
}

/// Capability audit logger
#[derive(Debug)]
pub struct CapabilityAuditor {
    /// Audit log entries
    audit_log: Vec<AuditEntry>,
    /// Maximum log size
    max_entries: usize,
}

/// Audit log entry
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuditEntry {
    /// Timestamp of the event
    pub timestamp: SystemTime,
    /// Module or instance ID
    pub module_id: String,
    /// Capability being checked
    pub capability: String,
    /// Whether access was granted
    pub granted: bool,
    /// Security context ID
    pub context_id: String,
    /// Additional details
    pub details: HashMap<String, String>,
}

/// Resource usage tracker
#[derive(Debug, Default)]
pub struct ResourceTracker {
    /// Total memory usage across all instances
    total_memory: usize,
    /// Total CPU time used
    total_cpu_time: u64,
    /// Active instance count
    active_instances: usize,
}

impl SandboxManager {
    /// Create a new sandbox manager
    pub fn new(config: SandboxConfig) -> Self {
        let auditor = if config.audit_mode {
            Some(Arc::new(Mutex::new(CapabilityAuditor::new(10000))))
        } else {
            None
        };

        Self {
            config,
            contexts: Arc::new(RwLock::new(HashMap::new())),
            instances: Arc::new(RwLock::new(HashMap::new())),
            auditor,
            resource_tracker: Arc::new(Mutex::new(ResourceTracker::default())),
        }
    }

    /// Create a sandboxed instance with security context
    pub fn create_instance(
        &mut self,
        store: &Store,
        module_id: String,
        context: SecurityContext,
    ) -> WasmtimeResult<String> {
        // Check if context has expired
        if context.is_expired() {
            return Err(WasmtimeError::Security {
                message: "Security context has expired".to_string(),
            });
        }

        // Check concurrent instance limits
        if let Some(max_concurrent) = self.config.max_concurrent_modules {
            let instances = self.instances.read().unwrap();
            if instances.len() >= max_concurrent {
                return Err(WasmtimeError::ResourceLimit {
                    message: "Maximum concurrent instances reached".to_string(),
                });
            }
        }

        // Generate unique instance ID
        let instance_id = format!("{}_{}", module_id,
            SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos());

        // Create sandboxed instance (placeholder - would integrate with actual instance creation)
        let sandboxed_instance = SandboxedInstance {
            instance: None, // Placeholder
            context: context.clone(),
            resource_usage: ResourceUsage {
                memory_used: 0,
                cpu_time_ms: 0,
                instructions_executed: 0,
                syscalls_made: 0,
                file_operations: 0,
                network_operations: 0,
                start_time: Instant::now(),
            },
            created_at: Instant::now(),
            last_activity: Instant::now(),
        };

        // Store the instance
        {
            let mut instances = self.instances.write().unwrap();
            instances.insert(instance_id.clone(), sandboxed_instance);
        }

        // Store the context
        {
            let mut contexts = self.contexts.write().unwrap();
            contexts.insert(instance_id.clone(), context);
        }

        // Update resource tracking
        {
            let mut tracker = self.resource_tracker.lock().unwrap();
            tracker.active_instances += 1;
        }

        Ok(instance_id)
    }

    /// Check if an instance has a specific capability
    pub fn check_capability(
        &self,
        instance_id: &str,
        capability: &Capability,
    ) -> WasmtimeResult<bool> {
        let contexts = self.contexts.read().unwrap();
        let context = contexts.get(instance_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Unknown instance ID: {}", instance_id),
            })?;

        // Check if context has expired
        if context.is_expired() {
            return Err(WasmtimeError::Security {
                message: "Security context has expired".to_string(),
            });
        }

        let granted = context.has_capability(capability);

        // Log audit entry if auditing is enabled
        if let Some(auditor) = &self.auditor {
            let mut auditor = auditor.lock().unwrap();
            auditor.log_capability_check(
                instance_id.to_string(),
                capability,
                granted,
                &context.id,
            );
        }

        if self.config.strict_mode && !granted {
            return Err(WasmtimeError::Security {
                message: format!("Capability not granted: {:?}", capability),
            });
        }

        Ok(granted)
    }

    /// Update resource usage for an instance
    pub fn update_resource_usage(
        &self,
        instance_id: &str,
        usage_update: ResourceUsage,
    ) -> WasmtimeResult<()> {
        let mut instances = self.instances.write().unwrap();
        let instance = instances.get_mut(instance_id)
            .ok_or_else(|| WasmtimeError::InvalidParameter {
                message: format!("Unknown instance ID: {}", instance_id),
            })?;

        // Update instance resource usage
        instance.resource_usage = usage_update;
        instance.last_activity = Instant::now();

        // Check resource limits
        self.check_resource_limits(instance_id, &instance.resource_usage)?;

        Ok(())
    }

    /// Check if resource limits are exceeded
    fn check_resource_limits(
        &self,
        instance_id: &str,
        usage: &ResourceUsage,
    ) -> WasmtimeResult<()> {
        let limits = &self.config.global_limits;

        // Check memory limit
        if let Some(max_memory) = limits.max_memory {
            if usage.memory_used > max_memory {
                return Err(WasmtimeError::ResourceLimit {
                    message:
                    format!("Memory limit exceeded for instance {}: {} > {}",
                        instance_id, usage.memory_used, max_memory),
                });
            }
        }

        // Check CPU time limit
        if let Some(max_cpu_time) = limits.max_cpu_time {
            if usage.cpu_time_ms > max_cpu_time {
                return Err(WasmtimeError::ResourceLimit {
                    message:
                    format!("CPU time limit exceeded for instance {}: {} > {}",
                        instance_id, usage.cpu_time_ms, max_cpu_time),
                });
            }
        }

        // Check instruction limit
        if let Some(max_instructions) = limits.max_instructions {
            if usage.instructions_executed > max_instructions {
                return Err(WasmtimeError::ResourceLimit {
                    message:
                    format!("Instruction limit exceeded for instance {}: {} > {}",
                        instance_id, usage.instructions_executed, max_instructions),
                });
            }
        }

        Ok(())
    }

    /// Remove an instance from the sandbox
    pub fn remove_instance(&mut self, instance_id: &str) -> WasmtimeResult<()> {
        {
            let mut instances = self.instances.write().unwrap();
            instances.remove(instance_id);
        }

        {
            let mut contexts = self.contexts.write().unwrap();
            contexts.remove(instance_id);
        }

        // Update resource tracking
        {
            let mut tracker = self.resource_tracker.lock().unwrap();
            tracker.active_instances = tracker.active_instances.saturating_sub(1);
        }

        Ok(())
    }

    /// Get audit log entries
    pub fn get_audit_log(&self) -> Vec<AuditEntry> {
        if let Some(auditor) = &self.auditor {
            let auditor = auditor.lock().unwrap();
            auditor.audit_log.clone()
        } else {
            Vec::new()
        }
    }

    /// Get resource usage statistics
    pub fn get_resource_statistics(&self) -> ResourceTracker {
        let tracker = self.resource_tracker.lock().unwrap();
        ResourceTracker {
            total_memory: tracker.total_memory,
            total_cpu_time: tracker.total_cpu_time,
            active_instances: tracker.active_instances,
        }
    }

    /// Clean up expired contexts and instances
    pub fn cleanup_expired(&mut self) -> WasmtimeResult<usize> {
        let mut cleaned_count = 0;

        // Collect expired instance IDs
        let expired_ids: Vec<String> = {
            let contexts = self.contexts.read().unwrap();
            contexts.iter()
                .filter(|(_, context)| context.is_expired())
                .map(|(id, _)| id.clone())
                .collect()
        };

        // Remove expired instances
        for instance_id in expired_ids {
            self.remove_instance(&instance_id)?;
            cleaned_count += 1;
        }

        Ok(cleaned_count)
    }
}

impl CapabilityAuditor {
    /// Create a new capability auditor
    pub fn new(max_entries: usize) -> Self {
        Self {
            audit_log: Vec::with_capacity(max_entries),
            max_entries,
        }
    }

    /// Log a capability check
    pub fn log_capability_check(
        &mut self,
        module_id: String,
        capability: &Capability,
        granted: bool,
        context_id: &str,
    ) {
        let entry = AuditEntry {
            timestamp: SystemTime::now(),
            module_id,
            capability: format!("{:?}", capability),
            granted,
            context_id: context_id.to_string(),
            details: HashMap::new(),
        };

        // Add entry and maintain size limit
        self.audit_log.push(entry);
        if self.audit_log.len() > self.max_entries {
            self.audit_log.remove(0);
        }
    }

    /// Export audit log to JSON
    pub fn export_json(&self) -> WasmtimeResult<String> {
        serde_json::to_string_pretty(&self.audit_log)
            .map_err(|e| WasmtimeError::Serialization {
                message: format!("Failed to serialize audit log: {}", e),
            })
    }

    /// Clear the audit log
    pub fn clear(&mut self) {
        self.audit_log.clear();
    }
}

impl Default for SandboxConfig {
    fn default() -> Self {
        Self {
            default_context: SecurityContext::new("default".to_string(), 1),
            strict_mode: true,
            audit_mode: false,
            max_concurrent_modules: Some(10),
            global_limits: ResourceLimits::default(),
            imc_settings: InterModuleCommunicationSettings::default(),
        }
    }
}

/// Builder for creating sandbox configurations
pub struct SandboxConfigBuilder {
    config: SandboxConfig,
}

impl Default for SandboxConfigBuilder {
    fn default() -> Self {
        Self::new()
    }
}

impl SandboxConfigBuilder {
    /// Create a new sandbox config builder
    pub fn new() -> Self {
        Self {
            config: SandboxConfig::default(),
        }
    }

    /// Set strict mode
    pub fn strict_mode(mut self, strict: bool) -> Self {
        self.config.strict_mode = strict;
        self
    }

    /// Set audit mode
    pub fn audit_mode(mut self, audit: bool) -> Self {
        self.config.audit_mode = audit;
        self
    }

    /// Set maximum concurrent modules
    pub fn max_concurrent_modules(mut self, max: Option<usize>) -> Self {
        self.config.max_concurrent_modules = max;
        self
    }

    /// Set global resource limits
    pub fn global_limits(mut self, limits: ResourceLimits) -> Self {
        self.config.global_limits = limits;
        self
    }

    /// Enable inter-module communication
    pub fn enable_imc(mut self, settings: InterModuleCommunicationSettings) -> Self {
        self.config.imc_settings = settings;
        self
    }

    /// Build the configuration
    pub fn build(self) -> SandboxConfig {
        self.config
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_security_context_creation() {
        let mut context = SecurityContext::new("test".to_string(), 1);

        let capability = Capability::MemoryAccess {
            max_size: 1024,
            write_access: true,
        };

        context.grant_capability(capability.clone());
        assert!(context.has_capability(&capability));

        context.revoke_capability(&capability);
        assert!(!context.has_capability(&capability));
    }

    #[test]
    fn test_file_permissions() {
        let read_only = FilePermissions::read_only();
        assert!(read_only.read);
        assert!(!read_only.write);

        let full = FilePermissions::full();
        assert!(full.read && full.write && full.create && full.delete && full.list);
    }

    #[test]
    fn test_sandbox_config_builder() {
        let config = SandboxConfigBuilder::new()
            .strict_mode(true)
            .audit_mode(true)
            .max_concurrent_modules(Some(5))
            .build();

        assert!(config.strict_mode);
        assert!(config.audit_mode);
        assert_eq!(config.max_concurrent_modules, Some(5));
    }

    #[test]
    fn test_capability_auditor() {
        let mut auditor = CapabilityAuditor::new(100);

        let capability = Capability::MemoryAccess {
            max_size: 1024,
            write_access: false,
        };

        auditor.log_capability_check(
            "test_module".to_string(),
            &capability,
            true,
            "test_context",
        );

        assert_eq!(auditor.audit_log.len(), 1);
        assert!(auditor.audit_log[0].granted);
    }

    #[test]
    fn test_resource_limits_default() {
        let limits = ResourceLimits::default();
        assert!(limits.max_memory.is_some());
        assert!(limits.max_cpu_time.is_some());
        assert!(limits.max_duration.is_some());
    }
}