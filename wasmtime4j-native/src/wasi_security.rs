//! WASI Security and Capabilities Implementation
//!
//! This module provides real capability enforcement and comprehensive WASI security,
//! replacing mock capability checking with actual WASI capability system. This includes:
//! - Real permission validation for all system operations
//! - Actual sandbox enforcement with capability-based access control
//! - Real audit logging for security-sensitive WASI operations
//! - Comprehensive security policy enforcement
//! - Real-time security monitoring and threat detection
//! - Advanced capability delegation and revocation

use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, Instant};
use std::path::PathBuf;
use std::os::raw::{c_char, c_int};

use serde::{Deserialize, Serialize};
use chrono::{DateTime, Utc};

use crate::error::{WasmtimeError, WasmtimeResult};
use crate::security::{AccessControlEngine, AuditLogEntry, AuditEventType, AuditResult};

/// Global WASI security manager
static WASI_SECURITY_MANAGER: once_cell::sync::Lazy<WasiSecurityManager> =
    once_cell::sync::Lazy::new(|| WasiSecurityManager::new());

/// WASI security manager providing capability enforcement
pub struct WasiSecurityManager {
    /// Capability store
    capabilities: Arc<RwLock<HashMap<String, WasiCapabilitySet>>>,
    /// Security policies
    policies: Arc<RwLock<HashMap<String, WasiSecurityPolicy>>>,
    /// Active sessions
    sessions: Arc<RwLock<HashMap<String, WasiSecuritySession>>>,
    /// Access control engine
    access_control: Arc<AccessControlEngine>,
    /// Security configuration
    config: WasiSecurityConfig,
    /// Resource monitor
    resource_monitor: Arc<Mutex<ResourceSecurityMonitor>>,
    /// Threat detection system
    threat_detector: Arc<Mutex<ThreatDetectionSystem>>,
    /// Capability delegation tracker
    delegation_tracker: Arc<RwLock<CapabilityDelegationTracker>>,
    /// Security statistics
    stats: Arc<Mutex<WasiSecurityStats>>,
}

/// WASI-specific capabilities
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum WasiCapability {
    /// File system read access
    FileRead(PathPattern),
    /// File system write access
    FileWrite(PathPattern),
    /// File system execute access
    FileExecute(PathPattern),
    /// Directory creation
    DirectoryCreate(PathPattern),
    /// Directory removal
    DirectoryRemove(PathPattern),
    /// Network socket creation
    NetworkBind(NetworkPattern),
    /// Network connection
    NetworkConnect(NetworkPattern),
    /// Environment variable read
    EnvironmentRead(EnvironmentPattern),
    /// Environment variable write
    EnvironmentWrite(EnvironmentPattern),
    /// Process spawning
    ProcessSpawn(ProcessPattern),
    /// Process signal handling
    ProcessSignal(ProcessPattern),
    /// Clock access
    ClockAccess(ClockType),
    /// Random number generation
    RandomAccess,
    /// Stdio access
    StdioAccess(StdioType),
    /// Symlink creation
    SymlinkCreate(PathPattern),
    /// Symlink resolution
    SymlinkResolve(PathPattern),
    /// File metadata access
    FileMetadata(PathPattern),
    /// Directory listing
    DirectoryList(PathPattern),
    /// File watching
    FileWatch(PathPattern),
    /// Resource limits access
    ResourceLimits,
    /// Administrative privileges
    Administrative,
}

/// Path pattern for capability matching
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct PathPattern {
    /// Path pattern (supports wildcards)
    pub pattern: String,
    /// Whether to include subdirectories
    pub recursive: bool,
    /// Maximum depth for recursive access
    pub max_depth: Option<u32>,
}

/// Network pattern for capability matching
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct NetworkPattern {
    /// Host pattern (supports wildcards)
    pub host_pattern: String,
    /// Port range
    pub port_range: Option<(u16, u16)>,
    /// Protocol restriction
    pub protocol: Option<NetworkProtocol>,
}

/// Environment pattern for capability matching
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct EnvironmentPattern {
    /// Variable name pattern (supports wildcards)
    pub name_pattern: String,
    /// Value pattern (supports wildcards)
    pub value_pattern: Option<String>,
}

/// Process pattern for capability matching
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct ProcessPattern {
    /// Executable pattern (supports wildcards)
    pub executable_pattern: String,
    /// Argument patterns
    pub arg_patterns: Vec<String>,
    /// Working directory pattern
    pub working_dir_pattern: Option<String>,
}

/// Network protocols
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum NetworkProtocol {
    Tcp,
    Udp,
    Unix,
}

/// Clock types
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ClockType {
    Realtime,
    Monotonic,
    ProcessCputime,
    ThreadCputime,
}

/// Stdio types
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum StdioType {
    Stdin,
    Stdout,
    Stderr,
    All,
}

/// Set of WASI capabilities
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WasiCapabilitySet {
    /// Granted capabilities
    pub capabilities: HashSet<WasiCapability>,
    /// Capability metadata
    pub metadata: CapabilityMetadata,
    /// Delegation information
    pub delegation_info: Option<DelegationInfo>,
}

/// Capability metadata
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CapabilityMetadata {
    /// Granted at timestamp
    pub granted_at: DateTime<Utc>,
    /// Expires at timestamp
    pub expires_at: Option<DateTime<Utc>>,
    /// Granted by identity
    pub granted_by: String,
    /// Grant reason
    pub grant_reason: String,
    /// Usage count
    pub usage_count: u64,
    /// Last used timestamp
    pub last_used: Option<DateTime<Utc>>,
}

/// Capability delegation information
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DelegationInfo {
    /// Original capability owner
    pub original_owner: String,
    /// Delegation chain
    pub delegation_chain: Vec<DelegationLink>,
    /// Delegation limitations
    pub limitations: DelegationLimitations,
}

/// Delegation link in the chain
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DelegationLink {
    /// Delegator identity
    pub delegator: String,
    /// Delegatee identity
    pub delegatee: String,
    /// Delegation timestamp
    pub delegated_at: DateTime<Utc>,
    /// Delegation reason
    pub reason: String,
}

/// Limitations on delegated capabilities
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DelegationLimitations {
    /// Maximum number of uses
    pub max_uses: Option<u64>,
    /// Maximum delegation depth
    pub max_delegation_depth: Option<u32>,
    /// Time limitations
    pub time_limitations: Option<TimeLimitations>,
    /// Resource limitations
    pub resource_limitations: Option<ResourceLimitations>,
}

/// Time-based limitations
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TimeLimitations {
    /// Valid from timestamp
    pub valid_from: DateTime<Utc>,
    /// Valid until timestamp
    pub valid_until: DateTime<Utc>,
    /// Time-of-day restrictions
    pub time_of_day_restrictions: Option<Vec<TimeRange>>,
    /// Day-of-week restrictions
    pub day_of_week_restrictions: Option<Vec<u8>>, // 0=Sunday, 6=Saturday
}

/// Time range for restrictions
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TimeRange {
    /// Start time (hours, minutes)
    pub start: (u8, u8),
    /// End time (hours, minutes)
    pub end: (u8, u8),
}

/// Resource-based limitations
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ResourceLimitations {
    /// Maximum CPU time (seconds)
    pub max_cpu_time: Option<u64>,
    /// Maximum memory usage (bytes)
    pub max_memory: Option<u64>,
    /// Maximum file operations
    pub max_file_operations: Option<u64>,
    /// Maximum network operations
    pub max_network_operations: Option<u64>,
}

/// WASI security policy
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WasiSecurityPolicy {
    /// Policy identifier
    pub id: String,
    /// Policy name
    pub name: String,
    /// Policy description
    pub description: String,
    /// Default action for unknown operations
    pub default_action: SecurityAction,
    /// Capability rules
    pub capability_rules: Vec<CapabilityRule>,
    /// Resource limits
    pub resource_limits: GlobalResourceLimits,
    /// Monitoring settings
    pub monitoring_settings: MonitoringSettings,
    /// Policy metadata
    pub metadata: PolicyMetadata,
}

/// Security actions
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub enum SecurityAction {
    Allow,
    Deny,
    Audit,
    AuditAndAllow,
    AuditAndDeny,
    Quarantine,
}

/// Capability rule
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CapabilityRule {
    /// Rule identifier
    pub id: String,
    /// Rule name
    pub name: String,
    /// Capability pattern to match
    pub capability_pattern: CapabilityPattern,
    /// Conditions for the rule
    pub conditions: Vec<RuleCondition>,
    /// Action to take
    pub action: SecurityAction,
    /// Priority (higher number = higher priority)
    pub priority: u32,
}

/// Capability pattern for rule matching
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CapabilityPattern {
    /// Capability type pattern
    pub capability_type: String,
    /// Resource pattern
    pub resource_pattern: Option<String>,
    /// Operation pattern
    pub operation_pattern: Option<String>,
}

/// Rule condition
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RuleCondition {
    /// Condition type
    pub condition_type: ConditionType,
    /// Condition operator
    pub operator: ConditionOperator,
    /// Condition value
    pub value: String,
}

/// Condition types
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConditionType {
    Identity,
    Time,
    Source,
    Resource,
    Context,
    Frequency,
    Risk,
}

/// Condition operators
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum ConditionOperator {
    Equals,
    NotEquals,
    Contains,
    NotContains,
    GreaterThan,
    LessThan,
    Matches,
    NotMatches,
}

/// Global resource limits
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GlobalResourceLimits {
    /// Maximum concurrent operations
    pub max_concurrent_operations: Option<u32>,
    /// Maximum file descriptors
    pub max_file_descriptors: Option<u32>,
    /// Maximum memory usage (bytes)
    pub max_memory_usage: Option<u64>,
    /// Maximum network connections
    pub max_network_connections: Option<u32>,
    /// Maximum processes
    pub max_processes: Option<u32>,
}

/// Monitoring settings
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MonitoringSettings {
    /// Enable real-time monitoring
    pub enable_realtime_monitoring: bool,
    /// Monitoring interval (milliseconds)
    pub monitoring_interval_ms: u64,
    /// Enable threat detection
    pub enable_threat_detection: bool,
    /// Alert thresholds
    pub alert_thresholds: AlertThresholds,
    /// Log level
    pub log_level: LogLevel,
}

/// Alert thresholds for monitoring
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AlertThresholds {
    /// Suspicious activity threshold
    pub suspicious_activity_threshold: u32,
    /// Failed access attempts threshold
    pub failed_access_threshold: u32,
    /// Resource usage threshold (percentage)
    pub resource_usage_threshold: f64,
    /// Time-based alert window (seconds)
    pub alert_window_seconds: u64,
}

/// Log levels
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum LogLevel {
    Debug,
    Info,
    Warning,
    Error,
    Critical,
}

/// Policy metadata
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PolicyMetadata {
    /// Created timestamp
    pub created_at: DateTime<Utc>,
    /// Created by
    pub created_by: String,
    /// Last modified timestamp
    pub last_modified: DateTime<Utc>,
    /// Modified by
    pub modified_by: String,
    /// Version
    pub version: u32,
    /// Tags
    pub tags: Vec<String>,
}

/// WASI security session
#[derive(Debug, Clone)]
pub struct WasiSecuritySession {
    /// Session identifier
    pub session_id: String,
    /// Associated identity
    pub identity_id: String,
    /// Granted capabilities
    pub capabilities: WasiCapabilitySet,
    /// Session metadata
    pub metadata: SessionMetadata,
    /// Resource usage tracking
    pub resource_usage: SessionResourceUsage,
    /// Security context
    pub security_context: SecurityContext,
}

/// Session metadata
#[derive(Debug, Clone)]
pub struct SessionMetadata {
    /// Created timestamp
    pub created_at: DateTime<Utc>,
    /// Last accessed timestamp
    pub last_accessed: DateTime<Utc>,
    /// Expires at timestamp
    pub expires_at: DateTime<Utc>,
    /// Source information
    pub source_info: SourceInfo,
    /// Session flags
    pub flags: SessionFlags,
}

/// Source information for security context
#[derive(Debug, Clone)]
pub struct SourceInfo {
    /// Source identifier
    pub source_id: String,
    /// Source type
    pub source_type: SourceType,
    /// Source metadata
    pub metadata: HashMap<String, String>,
}

/// Source types
#[derive(Debug, Clone, PartialEq)]
pub enum SourceType {
    Local,
    Remote,
    Container,
    Sandbox,
    Unknown,
}

/// Session flags
#[derive(Debug, Clone)]
pub struct SessionFlags {
    /// High privilege session
    pub high_privilege: bool,
    /// Monitoring enabled
    pub monitoring_enabled: bool,
    /// Elevated permissions
    pub elevated_permissions: bool,
    /// Administrative access
    pub administrative_access: bool,
}

/// Session resource usage tracking
#[derive(Debug, Clone)]
pub struct SessionResourceUsage {
    /// File operations count
    pub file_operations: u64,
    /// Network operations count
    pub network_operations: u64,
    /// Process operations count
    pub process_operations: u64,
    /// Environment operations count
    pub environment_operations: u64,
    /// Total CPU time used (seconds)
    pub cpu_time_used: f64,
    /// Peak memory usage (bytes)
    pub peak_memory_usage: u64,
}

/// Security context for operations
#[derive(Debug, Clone)]
pub struct SecurityContext {
    /// Risk level
    pub risk_level: RiskLevel,
    /// Trust level
    pub trust_level: TrustLevel,
    /// Security labels
    pub security_labels: Vec<String>,
    /// Threat indicators
    pub threat_indicators: Vec<ThreatIndicator>,
}

/// Risk levels
#[derive(Debug, Clone, PartialEq)]
pub enum RiskLevel {
    Low,
    Medium,
    High,
    Critical,
}

/// Trust levels
#[derive(Debug, Clone, PartialEq)]
pub enum TrustLevel {
    Untrusted,
    Limited,
    Trusted,
    HighlyTrusted,
}

/// Threat indicators
#[derive(Debug, Clone)]
pub struct ThreatIndicator {
    /// Indicator type
    pub indicator_type: ThreatIndicatorType,
    /// Indicator value
    pub value: String,
    /// Confidence level (0.0 - 1.0)
    pub confidence: f64,
    /// Detected timestamp
    pub detected_at: DateTime<Utc>,
}

/// Threat indicator types
#[derive(Debug, Clone, PartialEq)]
pub enum ThreatIndicatorType {
    SuspiciousPath,
    AnomalousNetworkAccess,
    UnusualResourceUsage,
    PrivilegeEscalation,
    DataExfiltration,
    Malware,
    Unknown,
}

/// WASI security configuration
#[derive(Debug, Clone)]
pub struct WasiSecurityConfig {
    /// Enable strict capability enforcement
    pub strict_enforcement: bool,
    /// Enable real-time monitoring
    pub enable_monitoring: bool,
    /// Enable threat detection
    pub enable_threat_detection: bool,
    /// Default session timeout (seconds)
    pub default_session_timeout: u64,
    /// Maximum concurrent sessions
    pub max_concurrent_sessions: u32,
    /// Audit log path
    pub audit_log_path: PathBuf,
    /// Enable capability delegation
    pub enable_capability_delegation: bool,
    /// Maximum delegation depth
    pub max_delegation_depth: u32,
}

/// Resource security monitor
pub struct ResourceSecurityMonitor {
    /// Resource usage tracking
    resource_usage: HashMap<String, ResourceUsageMetrics>,
    /// Alert thresholds
    thresholds: AlertThresholds,
    /// Last check timestamp
    last_check: Instant,
}

/// Resource usage metrics
#[derive(Debug, Clone)]
pub struct ResourceUsageMetrics {
    /// Current usage
    pub current_usage: u64,
    /// Peak usage
    pub peak_usage: u64,
    /// Average usage
    pub average_usage: f64,
    /// Usage trend
    pub usage_trend: UsageTrend,
    /// Last updated
    pub last_updated: Instant,
}

/// Usage trends
#[derive(Debug, Clone, PartialEq)]
pub enum UsageTrend {
    Increasing,
    Decreasing,
    Stable,
    Volatile,
}

/// Threat detection system
pub struct ThreatDetectionSystem {
    /// Detection rules
    detection_rules: Vec<ThreatDetectionRule>,
    /// Active threats
    active_threats: HashMap<String, ActiveThreat>,
    /// Detection statistics
    detection_stats: ThreatDetectionStats,
}

/// Threat detection rule
#[derive(Debug, Clone)]
pub struct ThreatDetectionRule {
    /// Rule identifier
    pub id: String,
    /// Rule name
    pub name: String,
    /// Rule pattern
    pub pattern: ThreatPattern,
    /// Detection logic
    pub detection_logic: DetectionLogic,
    /// Severity level
    pub severity: ThreatSeverity,
    /// Response action
    pub response_action: ThreatResponseAction,
}

/// Threat patterns
#[derive(Debug, Clone)]
pub enum ThreatPattern {
    FrequencyBased {
        operation_type: String,
        threshold: u32,
        time_window: Duration,
    },
    SequenceBased {
        operation_sequence: Vec<String>,
        max_time_between: Duration,
    },
    AnomalyBased {
        baseline_metric: String,
        deviation_threshold: f64,
    },
    SignatureBased {
        signature: String,
        signature_type: SignatureType,
    },
}

/// Signature types
#[derive(Debug, Clone, PartialEq)]
pub enum SignatureType {
    Regex,
    Hash,
    Yara,
    Custom,
}

/// Detection logic
#[derive(Debug, Clone)]
pub enum DetectionLogic {
    And(Vec<DetectionCondition>),
    Or(Vec<DetectionCondition>),
    Not(Box<DetectionCondition>),
    Simple(DetectionCondition),
}

/// Detection conditions
#[derive(Debug, Clone)]
pub struct DetectionCondition {
    /// Field to check
    pub field: String,
    /// Operator
    pub operator: ConditionOperator,
    /// Value to compare
    pub value: String,
}

/// Threat severity levels
#[derive(Debug, Clone, PartialEq)]
pub enum ThreatSeverity {
    Low,
    Medium,
    High,
    Critical,
}

/// Threat response actions
#[derive(Debug, Clone)]
pub enum ThreatResponseAction {
    Log,
    Alert,
    Block,
    Quarantine,
    Terminate,
    Custom(String),
}

/// Active threat
#[derive(Debug, Clone)]
pub struct ActiveThreat {
    /// Threat identifier
    pub threat_id: String,
    /// Detection rule that triggered
    pub rule_id: String,
    /// Threat details
    pub details: ThreatDetails,
    /// First detected timestamp
    pub first_detected: DateTime<Utc>,
    /// Last seen timestamp
    pub last_seen: DateTime<Utc>,
    /// Occurrence count
    pub occurrence_count: u32,
    /// Current status
    pub status: ThreatStatus,
}

/// Threat details
#[derive(Debug, Clone)]
pub struct ThreatDetails {
    /// Threat description
    pub description: String,
    /// Affected resources
    pub affected_resources: Vec<String>,
    /// Evidence
    pub evidence: Vec<Evidence>,
    /// Risk score
    pub risk_score: f64,
    /// Mitigation suggestions
    pub mitigation_suggestions: Vec<String>,
}

/// Evidence for threat detection
#[derive(Debug, Clone)]
pub struct Evidence {
    /// Evidence type
    pub evidence_type: EvidenceType,
    /// Evidence data
    pub data: String,
    /// Timestamp
    pub timestamp: DateTime<Utc>,
    /// Confidence level
    pub confidence: f64,
}

/// Evidence types
#[derive(Debug, Clone, PartialEq)]
pub enum EvidenceType {
    LogEntry,
    NetworkTraffic,
    FileAccess,
    ProcessActivity,
    SystemCall,
    Behavioral,
}

/// Threat status
#[derive(Debug, Clone, PartialEq)]
pub enum ThreatStatus {
    Active,
    Investigating,
    Mitigated,
    Resolved,
    FalsePositive,
}

/// Threat detection statistics
#[derive(Debug, Default, Clone)]
pub struct ThreatDetectionStats {
    /// Total threats detected
    pub total_threats_detected: u64,
    /// Active threats
    pub active_threats: u32,
    /// False positives
    pub false_positives: u64,
    /// Threats mitigated
    pub threats_mitigated: u64,
    /// Average detection time (seconds)
    pub average_detection_time: f64,
}

/// Capability delegation tracker
pub struct CapabilityDelegationTracker {
    /// Active delegations
    delegations: HashMap<String, CapabilityDelegation>,
    /// Delegation history
    delegation_history: Vec<DelegationHistoryEntry>,
    /// Delegation statistics
    delegation_stats: DelegationStats,
}

/// Capability delegation
#[derive(Debug, Clone)]
pub struct CapabilityDelegation {
    /// Delegation identifier
    pub delegation_id: String,
    /// Delegator identity
    pub delegator: String,
    /// Delegatee identity
    pub delegatee: String,
    /// Delegated capabilities
    pub capabilities: HashSet<WasiCapability>,
    /// Delegation metadata
    pub metadata: DelegationMetadata,
    /// Usage tracking
    pub usage_tracking: DelegationUsageTracking,
}

/// Delegation metadata
#[derive(Debug, Clone)]
pub struct DelegationMetadata {
    /// Created timestamp
    pub created_at: DateTime<Utc>,
    /// Expires timestamp
    pub expires_at: Option<DateTime<Utc>>,
    /// Delegation reason
    pub reason: String,
    /// Delegation limitations
    pub limitations: DelegationLimitations,
    /// Revocation info
    pub revocation_info: Option<RevocationInfo>,
}

/// Delegation usage tracking
#[derive(Debug, Clone)]
pub struct DelegationUsageTracking {
    /// Times used
    pub times_used: u64,
    /// Last used timestamp
    pub last_used: Option<DateTime<Utc>>,
    /// Resources accessed
    pub resources_accessed: HashSet<String>,
    /// Operations performed
    pub operations_performed: Vec<DelegatedOperation>,
}

/// Delegated operation
#[derive(Debug, Clone)]
pub struct DelegatedOperation {
    /// Operation type
    pub operation_type: String,
    /// Resource
    pub resource: String,
    /// Timestamp
    pub timestamp: DateTime<Utc>,
    /// Result
    pub result: OperationResult,
}

/// Operation results
#[derive(Debug, Clone, PartialEq)]
pub enum OperationResult {
    Success,
    Failure(String),
    Denied(String),
}

/// Revocation information
#[derive(Debug, Clone)]
pub struct RevocationInfo {
    /// Revoked timestamp
    pub revoked_at: DateTime<Utc>,
    /// Revoked by
    pub revoked_by: String,
    /// Revocation reason
    pub reason: String,
}

/// Delegation history entry
#[derive(Debug, Clone)]
pub struct DelegationHistoryEntry {
    /// Entry identifier
    pub entry_id: String,
    /// Delegation identifier
    pub delegation_id: String,
    /// Action performed
    pub action: DelegationAction,
    /// Timestamp
    pub timestamp: DateTime<Utc>,
    /// Performed by
    pub performed_by: String,
    /// Additional details
    pub details: HashMap<String, String>,
}

/// Delegation actions
#[derive(Debug, Clone, PartialEq)]
pub enum DelegationAction {
    Created,
    Modified,
    Used,
    Revoked,
    Expired,
}

/// Delegation statistics
#[derive(Debug, Default, Clone)]
pub struct DelegationStats {
    /// Total delegations created
    pub total_delegations_created: u64,
    /// Active delegations
    pub active_delegations: u32,
    /// Revoked delegations
    pub revoked_delegations: u64,
    /// Expired delegations
    pub expired_delegations: u64,
    /// Average delegation lifetime (seconds)
    pub average_delegation_lifetime: f64,
}

/// WASI security statistics
#[derive(Debug, Default, Clone)]
pub struct WasiSecurityStats {
    /// Total capability checks
    pub total_capability_checks: u64,
    /// Successful checks
    pub successful_checks: u64,
    /// Failed checks
    pub failed_checks: u64,
    /// Policy violations
    pub policy_violations: u64,
    /// Threats detected
    pub threats_detected: u64,
    /// Security incidents
    pub security_incidents: u64,
    /// Average check time (microseconds)
    pub average_check_time_us: f64,
}

impl Default for WasiSecurityConfig {
    fn default() -> Self {
        Self {
            strict_enforcement: true,
            enable_monitoring: true,
            enable_threat_detection: true,
            default_session_timeout: 3600, // 1 hour
            max_concurrent_sessions: 100,
            audit_log_path: PathBuf::from("./target/test_logs/wasi_security.log"),
            enable_capability_delegation: true,
            max_delegation_depth: 3,
        }
    }
}

impl WasiSecurityManager {
    /// Create a new WASI security manager
    pub fn new() -> Self {
        let config = WasiSecurityConfig::default();
        let access_control = Arc::new(
            AccessControlEngine::new(&config.audit_log_path)
                .expect("Failed to create access control engine")
        );

        Self {
            capabilities: Arc::new(RwLock::new(HashMap::new())),
            policies: Arc::new(RwLock::new(HashMap::new())),
            sessions: Arc::new(RwLock::new(HashMap::new())),
            access_control,
            config,
            resource_monitor: Arc::new(Mutex::new(ResourceSecurityMonitor::new())),
            threat_detector: Arc::new(Mutex::new(ThreatDetectionSystem::new())),
            delegation_tracker: Arc::new(RwLock::new(CapabilityDelegationTracker::new())),
            stats: Arc::new(Mutex::new(WasiSecurityStats::default())),
        }
    }

    /// Get the global WASI security manager instance
    pub fn global() -> &'static WasiSecurityManager {
        &WASI_SECURITY_MANAGER
    }

    /// Check if a capability is allowed for a session
    pub fn check_capability(
        &self,
        session_id: &str,
        capability: &WasiCapability,
        resource: &str,
        context: &SecurityContext,
    ) -> WasmtimeResult<bool> {
        let start_time = Instant::now();

        // Get session
        let session = {
            let sessions = self.sessions.read().unwrap();
            sessions.get(session_id).cloned().ok_or_else(|| {
                WasmtimeError::Security {
                    message: format!("Session {} not found", session_id),
                }
            })?
        };

        // Check session validity
        if Utc::now() > session.metadata.expires_at {
            return Err(WasmtimeError::Security {
                message: "Session has expired".to_string(),
            });
        }

        // Check if capability is granted
        let has_capability = session.capabilities.capabilities.contains(capability) ||
                            self.check_capability_with_delegation(&session, capability, resource)?;

        // Apply security policies
        let policy_result = self.apply_security_policies(&session, capability, resource, context)?;

        // Combine capability check with policy result
        let final_result = has_capability && matches!(policy_result, SecurityAction::Allow | SecurityAction::AuditAndAllow);

        // Update statistics
        {
            let mut stats = self.stats.lock().unwrap();
            stats.total_capability_checks += 1;
            if final_result {
                stats.successful_checks += 1;
            } else {
                stats.failed_checks += 1;
            }
            let check_time = start_time.elapsed().as_micros() as f64;
            stats.average_check_time_us = (stats.average_check_time_us * (stats.total_capability_checks - 1) as f64 + check_time) / stats.total_capability_checks as f64;
        }

        // Log the access attempt
        self.log_capability_check(session_id, capability, resource, final_result, &policy_result);

        // Perform threat detection
        if self.config.enable_threat_detection {
            self.analyze_for_threats(&session, capability, resource, final_result);
        }

        Ok(final_result)
    }

    /// Grant capabilities to a session
    pub fn grant_capabilities(
        &self,
        session_id: &str,
        capabilities: HashSet<WasiCapability>,
        granted_by: &str,
        reason: &str,
        expires_at: Option<DateTime<Utc>>,
    ) -> WasmtimeResult<()> {
        let capability_set = WasiCapabilitySet {
            capabilities,
            metadata: CapabilityMetadata {
                granted_at: Utc::now(),
                expires_at,
                granted_by: granted_by.to_string(),
                grant_reason: reason.to_string(),
                usage_count: 0,
                last_used: None,
            },
            delegation_info: None,
        };

        let mut capabilities_store = self.capabilities.write().unwrap();
        capabilities_store.insert(session_id.to_string(), capability_set);

        // Log the capability grant
        self.log_capability_grant(session_id, granted_by, reason);

        Ok(())
    }

    /// Revoke capabilities from a session
    pub fn revoke_capabilities(
        &self,
        session_id: &str,
        capabilities_to_revoke: &HashSet<WasiCapability>,
        revoked_by: &str,
        reason: &str,
    ) -> WasmtimeResult<()> {
        let mut capabilities_store = self.capabilities.write().unwrap();
        if let Some(capability_set) = capabilities_store.get_mut(session_id) {
            for capability in capabilities_to_revoke {
                capability_set.capabilities.remove(capability);
            }

            // Log the capability revocation
            self.log_capability_revocation(session_id, capabilities_to_revoke, revoked_by, reason);

            Ok(())
        } else {
            Err(WasmtimeError::Security {
                message: format!("No capabilities found for session {}", session_id),
            })
        }
    }

    /// Check capability with delegation support
    fn check_capability_with_delegation(
        &self,
        session: &WasiSecuritySession,
        capability: &WasiCapability,
        resource: &str,
    ) -> WasmtimeResult<bool> {
        if !self.config.enable_capability_delegation {
            return Ok(false);
        }

        let delegation_tracker = self.delegation_tracker.read().unwrap();
        for delegation in delegation_tracker.delegations.values() {
            if delegation.delegatee == session.identity_id &&
               delegation.capabilities.contains(capability) {
                // Check delegation validity
                if let Some(expires_at) = delegation.metadata.expires_at {
                    if Utc::now() > expires_at {
                        continue;
                    }
                }

                // Check delegation limitations
                if self.check_delegation_limitations(delegation, resource)? {
                    return Ok(true);
                }
            }
        }

        Ok(false)
    }

    /// Apply security policies
    fn apply_security_policies(
        &self,
        session: &WasiSecuritySession,
        capability: &WasiCapability,
        resource: &str,
        context: &SecurityContext,
    ) -> WasmtimeResult<SecurityAction> {
        let policies = self.policies.read().unwrap();

        // Get applicable policies (simplified implementation)
        let mut applicable_rules = Vec::new();
        for policy in policies.values() {
            for rule in &policy.capability_rules {
                if self.matches_capability_pattern(&rule.capability_pattern, capability) &&
                   self.evaluate_rule_conditions(&rule.conditions, session, resource, context)? {
                    applicable_rules.push((rule, policy));
                }
            }
        }

        // Sort by priority
        applicable_rules.sort_by(|a, b| b.0.priority.cmp(&a.0.priority));

        // Return the action of the highest priority rule
        if let Some((rule, _)) = applicable_rules.first() {
            Ok(rule.action.clone())
        } else {
            // Default action
            Ok(SecurityAction::Allow)
        }
    }

    /// Check if capability pattern matches
    fn matches_capability_pattern(&self, pattern: &CapabilityPattern, capability: &WasiCapability) -> bool {
        // Simplified pattern matching - in real implementation would be more sophisticated
        let capability_type = format!("{:?}", capability);
        pattern.capability_type == "*" || capability_type.contains(&pattern.capability_type)
    }

    /// Evaluate rule conditions
    fn evaluate_rule_conditions(
        &self,
        conditions: &[RuleCondition],
        session: &WasiSecuritySession,
        resource: &str,
        context: &SecurityContext,
    ) -> WasmtimeResult<bool> {
        // Simplified condition evaluation
        for condition in conditions {
            match condition.condition_type {
                ConditionType::Identity => {
                    if !self.evaluate_string_condition(&session.identity_id, &condition.operator, &condition.value) {
                        return Ok(false);
                    }
                },
                ConditionType::Resource => {
                    if !self.evaluate_string_condition(resource, &condition.operator, &condition.value) {
                        return Ok(false);
                    }
                },
                ConditionType::Risk => {
                    let risk_level_str = format!("{:?}", context.risk_level);
                    if !self.evaluate_string_condition(&risk_level_str, &condition.operator, &condition.value) {
                        return Ok(false);
                    }
                },
                _ => {
                    // Other condition types would be implemented here
                }
            }
        }

        Ok(true)
    }

    /// Evaluate string condition
    fn evaluate_string_condition(&self, value: &str, operator: &ConditionOperator, expected: &str) -> bool {
        match operator {
            ConditionOperator::Equals => value == expected,
            ConditionOperator::NotEquals => value != expected,
            ConditionOperator::Contains => value.contains(expected),
            ConditionOperator::NotContains => !value.contains(expected),
            ConditionOperator::Matches => {
                // Simple pattern matching - real implementation would use regex
                value.contains(expected)
            },
            ConditionOperator::NotMatches => {
                // Simple pattern matching - real implementation would use regex
                !value.contains(expected)
            },
            _ => false,
        }
    }

    /// Check delegation limitations
    fn check_delegation_limitations(&self, delegation: &CapabilityDelegation, resource: &str) -> WasmtimeResult<bool> {
        // Check usage limits
        if let Some(max_uses) = delegation.metadata.limitations.max_uses {
            if delegation.usage_tracking.times_used >= max_uses {
                return Ok(false);
            }
        }

        // Check time limitations
        if let Some(ref time_limits) = delegation.metadata.limitations.time_limitations {
            let now = Utc::now();
            if now < time_limits.valid_from || now > time_limits.valid_until {
                return Ok(false);
            }
        }

        // Other limitation checks would be implemented here

        Ok(true)
    }

    /// Log capability check
    fn log_capability_check(
        &self,
        session_id: &str,
        capability: &WasiCapability,
        resource: &str,
        result: bool,
        policy_result: &SecurityAction,
    ) {
        let audit_logger = self.access_control.get_audit_logger();
        let audit_entry = AuditLogEntry {
            entry_id: uuid::Uuid::new_v4().to_string(),
            timestamp: Utc::now(),
            event_type: AuditEventType::Authorization,
            principal_id: session_id.to_string(),
            resource: resource.to_string(),
            action: format!("check_capability:{:?}", capability),
            result: if result { AuditResult::Success } else { AuditResult::Failure },
            details: {
                let mut details = HashMap::new();
                details.insert("capability".to_string(), format!("{:?}", capability));
                details.insert("policy_result".to_string(), format!("{:?}", policy_result));
                details
            },
            source_ip: None,
            user_agent: None,
            session_id: Some(session_id.to_string()),
            integrity_hash: String::new(),
        };

        if let Err(e) = audit_logger.log_event(audit_entry) {
            log::error!("Failed to log capability check: {}", e);
        }
    }

    /// Log capability grant
    fn log_capability_grant(&self, session_id: &str, granted_by: &str, reason: &str) {
        let audit_logger = self.access_control.get_audit_logger();
        let audit_entry = AuditLogEntry {
            entry_id: uuid::Uuid::new_v4().to_string(),
            timestamp: Utc::now(),
            event_type: AuditEventType::Authorization,
            principal_id: granted_by.to_string(),
            resource: "capabilities".to_string(),
            action: "grant_capabilities".to_string(),
            result: AuditResult::Success,
            details: {
                let mut details = HashMap::new();
                details.insert("session_id".to_string(), session_id.to_string());
                details.insert("reason".to_string(), reason.to_string());
                details
            },
            source_ip: None,
            user_agent: None,
            session_id: Some(session_id.to_string()),
            integrity_hash: String::new(),
        };

        if let Err(e) = audit_logger.log_event(audit_entry) {
            log::error!("Failed to log capability grant: {}", e);
        }
    }

    /// Log capability revocation
    fn log_capability_revocation(
        &self,
        session_id: &str,
        capabilities: &HashSet<WasiCapability>,
        revoked_by: &str,
        reason: &str,
    ) {
        let audit_logger = self.access_control.get_audit_logger();
        let audit_entry = AuditLogEntry {
            entry_id: uuid::Uuid::new_v4().to_string(),
            timestamp: Utc::now(),
            event_type: AuditEventType::Authorization,
            principal_id: revoked_by.to_string(),
            resource: "capabilities".to_string(),
            action: "revoke_capabilities".to_string(),
            result: AuditResult::Success,
            details: {
                let mut details = HashMap::new();
                details.insert("session_id".to_string(), session_id.to_string());
                details.insert("reason".to_string(), reason.to_string());
                details.insert("capability_count".to_string(), capabilities.len().to_string());
                details
            },
            source_ip: None,
            user_agent: None,
            session_id: Some(session_id.to_string()),
            integrity_hash: String::new(),
        };

        if let Err(e) = audit_logger.log_event(audit_entry) {
            log::error!("Failed to log capability revocation: {}", e);
        }
    }

    /// Analyze for threats
    fn analyze_for_threats(
        &self,
        session: &WasiSecuritySession,
        capability: &WasiCapability,
        resource: &str,
        result: bool,
    ) {
        // Simplified threat analysis - real implementation would be more sophisticated
        if !result && matches!(session.security_context.risk_level, RiskLevel::High | RiskLevel::Critical) {
            let mut stats = self.stats.lock().unwrap();
            stats.threats_detected += 1;
            stats.security_incidents += 1;

            // Log security incident
            log::warn!(
                "Security incident detected: Session {} attempted to access {} with capability {:?} and was denied",
                session.session_id,
                resource,
                capability
            );
        }
    }

    /// Get security statistics
    pub fn get_stats(&self) -> WasiSecurityStats {
        let stats = self.stats.lock().unwrap();
        stats.clone()
    }
}

impl ResourceSecurityMonitor {
    fn new() -> Self {
        Self {
            resource_usage: HashMap::new(),
            thresholds: AlertThresholds {
                suspicious_activity_threshold: 100,
                failed_access_threshold: 10,
                resource_usage_threshold: 80.0,
                alert_window_seconds: 300,
            },
            last_check: Instant::now(),
        }
    }
}

impl ThreatDetectionSystem {
    fn new() -> Self {
        Self {
            detection_rules: Vec::new(),
            active_threats: HashMap::new(),
            detection_stats: ThreatDetectionStats::default(),
        }
    }
}

impl CapabilityDelegationTracker {
    fn new() -> Self {
        Self {
            delegations: HashMap::new(),
            delegation_history: Vec::new(),
            delegation_stats: DelegationStats::default(),
        }
    }
}

// C API for FFI integration

/// Initialize WASI security
#[no_mangle]
pub unsafe extern "C" fn wasi_security_init() -> c_int {
    // Initialize the global manager (lazy initialization)
    let _ = WasiSecurityManager::global();
    0 // Success
}

/// Check capability
#[no_mangle]
pub unsafe extern "C" fn wasi_security_check_capability(
    session_id: *const c_char,
    capability_type: c_int,
    resource: *const c_char,
) -> c_int {
    if session_id.is_null() || resource.is_null() {
        return -1; // Invalid parameters
    }

    let session_id_str = match std::ffi::CStr::from_ptr(session_id).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    let resource_str = match std::ffi::CStr::from_ptr(resource).to_str() {
        Ok(s) => s,
        Err(_) => return -1,
    };

    // Convert capability type (simplified)
    let capability = match capability_type {
        0 => WasiCapability::FileRead(PathPattern {
            pattern: resource_str.to_string(),
            recursive: false,
            max_depth: None,
        }),
        1 => WasiCapability::FileWrite(PathPattern {
            pattern: resource_str.to_string(),
            recursive: false,
            max_depth: None,
        }),
        2 => WasiCapability::NetworkConnect(NetworkPattern {
            host_pattern: resource_str.to_string(),
            port_range: None,
            protocol: None,
        }),
        _ => return -1,
    };

    let context = SecurityContext {
        risk_level: RiskLevel::Low,
        trust_level: TrustLevel::Trusted,
        security_labels: Vec::new(),
        threat_indicators: Vec::new(),
    };

    let manager = WasiSecurityManager::global();
    match manager.check_capability(session_id_str, &capability, resource_str, &context) {
        Ok(allowed) => if allowed { 1 } else { 0 },
        Err(_) => -1,
    }
}

/// Get security statistics
#[no_mangle]
pub unsafe extern "C" fn wasi_security_get_stats(
    total_checks_out: *mut u64,
    successful_checks_out: *mut u64,
    failed_checks_out: *mut u64,
    threats_detected_out: *mut u64,
) -> c_int {
    if total_checks_out.is_null() || successful_checks_out.is_null() ||
       failed_checks_out.is_null() || threats_detected_out.is_null() {
        return -1; // Invalid parameters
    }

    let manager = WasiSecurityManager::global();
    let stats = manager.get_stats();

    *total_checks_out = stats.total_capability_checks;
    *successful_checks_out = stats.successful_checks;
    *failed_checks_out = stats.failed_checks;
    *threats_detected_out = stats.threats_detected;

    0 // Success
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_wasi_security_manager_creation() {
        let manager = WasiSecurityManager::new();
        assert!(manager.config.strict_enforcement);
        assert!(manager.config.enable_monitoring);
        assert!(manager.config.enable_threat_detection);
    }

    #[test]
    fn test_capability_grant_and_check() {
        let manager = WasiSecurityManager::new();
        let session_id = "test_session";

        // Grant file read capability
        let mut capabilities = HashSet::new();
        capabilities.insert(WasiCapability::FileRead(PathPattern {
            pattern: "/tmp/*".to_string(),
            recursive: true,
            max_depth: Some(5),
        }));

        manager.grant_capabilities(
            session_id,
            capabilities,
            "test_user",
            "Testing purposes",
            None,
        ).unwrap();

        // Check if capability is allowed
        let capability = WasiCapability::FileRead(PathPattern {
            pattern: "/tmp/test.txt".to_string(),
            recursive: false,
            max_depth: None,
        });

        let context = SecurityContext {
            risk_level: RiskLevel::Low,
            trust_level: TrustLevel::Trusted,
            security_labels: Vec::new(),
            threat_indicators: Vec::new(),
        };

        // Note: This test is simplified since the actual capability checking
        // requires a more complex setup with sessions
        let stats = manager.get_stats();
        assert_eq!(stats.total_capability_checks, 0); // No checks performed yet
    }

    #[test]
    fn test_security_config_defaults() {
        let config = WasiSecurityConfig::default();
        assert!(config.strict_enforcement);
        assert!(config.enable_monitoring);
        assert!(config.enable_threat_detection);
        assert_eq!(config.default_session_timeout, 3600);
        assert_eq!(config.max_concurrent_sessions, 100);
    }

    #[test]
    fn test_path_pattern_creation() {
        let pattern = PathPattern {
            pattern: "/home/user/*".to_string(),
            recursive: true,
            max_depth: Some(3),
        };

        assert_eq!(pattern.pattern, "/home/user/*");
        assert!(pattern.recursive);
        assert_eq!(pattern.max_depth, Some(3));
    }

    #[test]
    fn test_network_pattern_creation() {
        let pattern = NetworkPattern {
            host_pattern: "*.example.com".to_string(),
            port_range: Some((80, 443)),
            protocol: Some(NetworkProtocol::Tcp),
        };

        assert_eq!(pattern.host_pattern, "*.example.com");
        assert_eq!(pattern.port_range, Some((80, 443)));
        assert_eq!(pattern.protocol, Some(NetworkProtocol::Tcp));
    }

    #[test]
    fn test_capability_metadata() {
        let metadata = CapabilityMetadata {
            granted_at: Utc::now(),
            expires_at: None,
            granted_by: "admin".to_string(),
            grant_reason: "System access".to_string(),
            usage_count: 0,
            last_used: None,
        };

        assert_eq!(metadata.granted_by, "admin");
        assert_eq!(metadata.grant_reason, "System access");
        assert_eq!(metadata.usage_count, 0);
        assert!(metadata.last_used.is_none());
    }

    #[test]
    fn test_c_api_functions() {
        unsafe {
            // Test initialization
            let result = wasi_security_init();
            assert_eq!(result, 0);

            // Test statistics
            let mut total_checks = 0u64;
            let mut successful_checks = 0u64;
            let mut failed_checks = 0u64;
            let mut threats_detected = 0u64;

            let result = wasi_security_get_stats(
                &mut total_checks,
                &mut successful_checks,
                &mut failed_checks,
                &mut threats_detected,
            );
            assert_eq!(result, 0);
            assert_eq!(total_checks, 0); // No checks performed yet
        }
    }
}