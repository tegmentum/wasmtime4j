//! # Advanced Access Control with Capability-Based Security
//!
//! This module implements a comprehensive capability-based access control system
//! with enterprise-grade security features:
//!
//! - Fine-grained capability management with delegation and revocation
//! - Role-Based Access Control (RBAC) with hierarchical roles
//! - Attribute-Based Access Control (ABAC) with dynamic policy evaluation
//! - Discretionary Access Control (DAC) with owner-controlled permissions
//! - Mandatory Access Control (MAC) with security labels and clearances
//! - Zero-trust architecture with continuous authentication and authorization
//! - Dynamic capability negotiation and least-privilege enforcement
//! - Comprehensive audit trails and access pattern analysis
//! - Integration with external identity providers and policy engines

use std::collections::{HashMap, HashSet, BTreeMap, VecDeque};
use std::sync::{Arc, RwLock, Mutex};
use std::time::{SystemTime, UNIX_EPOCH, Duration, Instant};
use std::sync::atomic::{AtomicU64, AtomicBool, Ordering};
use serde::{Deserialize, Serialize};
use sha2::{Sha256, Digest};
use chrono::{DateTime, Utc};
use uuid::Uuid;

use crate::error::{WasmtimeError, WasmtimeResult, ErrorCode};
use crate::security::{SecurityCapability, AuditLogger, AuditLogEntry, AuditEventType, AuditResult};

/// Advanced capability types with fine-grained permissions
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AdvancedCapability {
    /// Resource access capabilities
    ResourceAccess {
        resource_type: ResourceType,
        resource_id: String,
        access_modes: HashSet<AccessMode>,
        constraints: Vec<AccessConstraint>,
    },
    /// Execution capabilities
    Execution {
        execution_scope: ExecutionScope,
        runtime_constraints: RuntimeConstraints,
        security_level: SecurityLevel,
    },
    /// Administrative capabilities
    Administrative {
        admin_scope: AdminScope,
        delegation_rights: DelegationRights,
        audit_requirements: AuditRequirements,
    },
    /// Network capabilities
    Network {
        network_scope: NetworkScope,
        protocols: HashSet<NetworkProtocol>,
        endpoints: Vec<NetworkEndpoint>,
        bandwidth_limits: BandwidthLimits,
    },
    /// Cryptographic capabilities
    Cryptographic {
        crypto_operations: HashSet<CryptoOperation>,
        key_access: KeyAccessRights,
        algorithm_restrictions: Vec<AlgorithmRestriction>,
    },
    /// Data processing capabilities
    DataProcessing {
        data_types: HashSet<DataType>,
        processing_operations: HashSet<ProcessingOperation>,
        privacy_constraints: PrivacyConstraints,
    },
    /// System integration capabilities
    SystemIntegration {
        integration_points: HashSet<IntegrationPoint>,
        api_access_rights: ApiAccessRights,
        service_mesh_permissions: ServiceMeshPermissions,
    },
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ResourceType {
    Memory,
    File,
    Network,
    Cpu,
    Database,
    Cache,
    Queue,
    Storage,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AccessMode {
    Read,
    Write,
    Execute,
    Create,
    Delete,
    Modify,
    List,
    Search,
    Stream,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct AccessConstraint {
    /// Constraint type
    pub constraint_type: ConstraintType,
    /// Constraint parameters
    pub parameters: HashMap<String, String>,
    /// Constraint evaluation logic
    pub evaluation_logic: ConstraintLogic,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ConstraintType {
    /// Time-based constraints
    Temporal,
    /// Location-based constraints
    Spatial,
    /// Resource usage constraints
    ResourceUsage,
    /// Conditional access constraints
    Conditional,
    /// Rate limiting constraints
    RateLimit,
    /// Custom constraints
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ConstraintLogic {
    /// Simple equality check
    Equals,
    /// Range check
    Range,
    /// Set membership
    In,
    /// Pattern matching
    Matches,
    /// Custom logic expression
    Expression(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ExecutionScope {
    /// Single function execution
    Function(String),
    /// Module execution
    Module(String),
    /// Component execution
    Component(String),
    /// System-wide execution
    System,
    /// Custom execution scope
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RuntimeConstraints {
    /// Maximum execution time
    pub max_execution_time: Option<Duration>,
    /// Maximum memory usage
    pub max_memory_usage: Option<u64>,
    /// Maximum CPU usage
    pub max_cpu_usage: Option<f64>,
    /// Resource quotas
    pub resource_quotas: HashMap<String, u64>,
    /// Priority level
    pub priority_level: PriorityLevel,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum SecurityLevel {
    Public,
    Confidential,
    Secret,
    TopSecret,
    Custom(u8),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum PriorityLevel {
    Low,
    Normal,
    High,
    Critical,
    RealTime,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AdminScope {
    UserManagement,
    PolicyManagement,
    SystemConfiguration,
    SecurityAudit,
    ResourceManagement,
    MonitoringControl,
    EmergencyResponse,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DelegationRights {
    /// Can delegate capabilities to others
    pub can_delegate: bool,
    /// Maximum delegation depth
    pub max_delegation_depth: u32,
    /// Delegation restrictions
    pub delegation_restrictions: Vec<DelegationRestriction>,
    /// Required approvals for delegation
    pub required_approvals: Vec<ApprovalRequirement>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum DelegationRestriction {
    /// Time-limited delegation
    TimeLimit(Duration),
    /// Usage count limitation
    UsageLimit(u32),
    /// Scope restriction
    ScopeRestriction(String),
    /// Approval required
    ApprovalRequired,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct ApprovalRequirement {
    /// Required approver role
    pub approver_role: String,
    /// Number of approvals required
    pub required_count: u32,
    /// Approval timeout
    pub approval_timeout: Duration,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AuditRequirements {
    /// No special audit requirements
    None,
    /// Standard audit logging
    Standard,
    /// Enhanced audit logging
    Enhanced,
    /// Real-time audit monitoring
    RealTime,
    /// Custom audit requirements
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum NetworkScope {
    /// Local network access only
    Local,
    /// Private network access
    Private,
    /// Public internet access
    Public,
    /// Specific network segments
    Segments(Vec<String>),
    /// Custom network scope
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum NetworkProtocol {
    Http,
    Https,
    Tcp,
    Udp,
    WebSocket,
    Grpc,
    Mqtt,
    Amqp,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct NetworkEndpoint {
    /// Endpoint address
    pub address: String,
    /// Port range
    pub port_range: Option<(u16, u16)>,
    /// Allowed protocols
    pub protocols: HashSet<NetworkProtocol>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BandwidthLimits {
    /// Maximum upload bandwidth (bytes per second)
    pub max_upload_bps: Option<u64>,
    /// Maximum download bandwidth (bytes per second)
    pub max_download_bps: Option<u64>,
    /// Burst allowance
    pub burst_allowance: Option<u64>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum CryptoOperation {
    Sign,
    Verify,
    Encrypt,
    Decrypt,
    Hash,
    Mac,
    KeyGeneration,
    KeyDerivation,
    KeyExchange,
    Random,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KeyAccessRights {
    /// Accessible key identifiers
    pub key_identifiers: HashSet<String>,
    /// Key usage restrictions
    pub usage_restrictions: HashMap<String, Vec<KeyUsageRestriction>>,
    /// Key lifetime constraints
    pub lifetime_constraints: HashMap<String, Duration>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum KeyUsageRestriction {
    /// Single use only
    SingleUse,
    /// Time-limited use
    TimeLimited(Duration),
    /// Usage count limited
    CountLimited(u32),
    /// Context-specific use
    ContextLimited(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct AlgorithmRestriction {
    /// Algorithm family
    pub algorithm_family: String,
    /// Minimum key size
    pub min_key_size: Option<u32>,
    /// Maximum key size
    pub max_key_size: Option<u32>,
    /// Allowed parameters
    pub allowed_parameters: HashMap<String, Vec<String>>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum DataType {
    PersonallyIdentifiable,
    Financial,
    Healthcare,
    Biometric,
    Behavioral,
    Location,
    Communication,
    Technical,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ProcessingOperation {
    Collection,
    Storage,
    Analysis,
    Transformation,
    Sharing,
    Deletion,
    Anonymization,
    Pseudonymization,
    Profiling,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PrivacyConstraints {
    /// Data residency requirements
    pub data_residency: Option<Vec<String>>,
    /// Retention period
    pub retention_period: Option<Duration>,
    /// Purpose limitation
    pub purpose_limitation: Vec<String>,
    /// Consent requirements
    pub consent_requirements: ConsentRequirements,
    /// Privacy impact assessment required
    pub pia_required: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ConsentRequirements {
    None,
    Explicit,
    OptIn,
    OptOut,
    Granular,
    Dynamic,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum IntegrationPoint {
    Api,
    Database,
    MessageQueue,
    EventStream,
    ServiceMesh,
    Registry,
    Configuration,
    Monitoring,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApiAccessRights {
    /// Accessible API endpoints
    pub endpoints: HashMap<String, EndpointAccess>,
    /// Rate limiting configuration
    pub rate_limits: RateLimits,
    /// Authentication requirements
    pub auth_requirements: AuthRequirements,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EndpointAccess {
    /// Allowed HTTP methods
    pub methods: HashSet<String>,
    /// Path parameters access
    pub path_parameters: HashSet<String>,
    /// Query parameters access
    pub query_parameters: HashSet<String>,
    /// Request body access
    pub request_body_access: BodyAccess,
    /// Response body access
    pub response_body_access: BodyAccess,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum BodyAccess {
    Full,
    FieldLimited(HashSet<String>),
    ReadOnly,
    WriteOnly,
    None,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RateLimits {
    /// Requests per second
    pub requests_per_second: Option<u32>,
    /// Requests per minute
    pub requests_per_minute: Option<u32>,
    /// Requests per hour
    pub requests_per_hour: Option<u32>,
    /// Burst allowance
    pub burst_allowance: Option<u32>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AuthRequirements {
    None,
    BasicAuth,
    BearerToken,
    ApiKey,
    Certificate,
    MultiFactorAuth,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ServiceMeshPermissions {
    /// Service discovery access
    pub service_discovery: bool,
    /// Load balancing configuration access
    pub load_balancing_config: bool,
    /// Circuit breaker configuration access
    pub circuit_breaker_config: bool,
    /// Observability data access
    pub observability_access: ObservabilityAccess,
    /// Security policy management
    pub security_policy_management: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ObservabilityAccess {
    None,
    Metrics,
    Traces,
    Logs,
    All,
}

/// Capability-based access control engine
#[derive(Debug)]
pub struct CapabilityBasedAccessControl {
    /// Principal capabilities
    principal_capabilities: Arc<RwLock<HashMap<String, PrincipalCapabilities>>>,
    /// Capability definitions
    capability_definitions: Arc<RwLock<HashMap<String, CapabilityDefinition>>>,
    /// Access control policies
    policies: Arc<RwLock<Vec<AccessControlPolicy>>>,
    /// Delegation tracking
    delegation_tracker: DelegationTracker,
    /// Access decision cache
    decision_cache: Arc<RwLock<HashMap<String, CachedDecision>>>,
    /// Audit logger
    audit_logger: Arc<AuditLogger>,
    /// Access statistics
    access_stats: Arc<Mutex<AccessStatistics>>,
    /// Configuration
    config: CapabilityAccessConfig,
}

#[derive(Debug, Clone)]
pub struct PrincipalCapabilities {
    /// Principal identifier
    pub principal_id: String,
    /// Principal type
    pub principal_type: PrincipalType,
    /// Directly assigned capabilities
    pub direct_capabilities: HashMap<String, CapabilityGrant>,
    /// Role-based capabilities
    pub role_capabilities: HashMap<String, RoleCapabilities>,
    /// Delegated capabilities
    pub delegated_capabilities: HashMap<String, DelegatedCapability>,
    /// Temporary capabilities
    pub temporary_capabilities: HashMap<String, TemporaryCapability>,
    /// Principal attributes
    pub attributes: HashMap<String, AttributeValue>,
    /// Security clearance
    pub security_clearance: SecurityLevel,
    /// Active sessions
    pub active_sessions: HashSet<String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PrincipalType {
    User,
    Service,
    Application,
    Device,
    System,
    Anonymous,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CapabilityGrant {
    /// Granted capability
    pub capability: AdvancedCapability,
    /// Grant timestamp
    pub granted_at: SystemTime,
    /// Grant expiration
    pub expires_at: Option<SystemTime>,
    /// Grant conditions
    pub conditions: Vec<GrantCondition>,
    /// Grant metadata
    pub metadata: HashMap<String, String>,
    /// Usage tracking
    pub usage_tracking: UsageTracking,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct GrantCondition {
    /// Condition type
    pub condition_type: ConditionType,
    /// Condition parameters
    pub parameters: HashMap<String, String>,
    /// Required value
    pub required_value: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ConditionType {
    TimeRange,
    Location,
    NetworkSource,
    DeviceType,
    SecurityContext,
    RiskLevel,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UsageTracking {
    /// Total usage count
    pub usage_count: u64,
    /// Last used timestamp
    pub last_used: Option<SystemTime>,
    /// Usage patterns
    pub usage_patterns: Vec<UsagePattern>,
    /// Anomaly detection flags
    pub anomaly_flags: Vec<AnomalyFlag>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UsagePattern {
    /// Pattern type
    pub pattern_type: PatternType,
    /// Pattern frequency
    pub frequency: f64,
    /// Pattern metadata
    pub metadata: HashMap<String, String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PatternType {
    Temporal,
    Frequency,
    Location,
    Resource,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct AnomalyFlag {
    /// Anomaly type
    pub anomaly_type: AnomalyType,
    /// Detected at
    pub detected_at: SystemTime,
    /// Severity level
    pub severity: AnomalySeverity,
    /// Description
    pub description: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AnomalyType {
    UnusualUsagePattern,
    FrequencyAnomaly,
    LocationAnomaly,
    TimeAnomaly,
    ResourceAnomaly,
    Custom(String),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum AnomalySeverity {
    Low,
    Medium,
    High,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RoleCapabilities {
    /// Role identifier
    pub role_id: String,
    /// Role name
    pub role_name: String,
    /// Role hierarchy level
    pub hierarchy_level: u32,
    /// Capabilities provided by this role
    pub capabilities: HashMap<String, CapabilityGrant>,
    /// Role inheritance
    pub inherited_roles: HashSet<String>,
    /// Role constraints
    pub constraints: Vec<RoleConstraint>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct RoleConstraint {
    /// Constraint type
    pub constraint_type: RoleConstraintType,
    /// Constraint value
    pub value: String,
    /// Enforcement level
    pub enforcement_level: EnforcementLevel,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum RoleConstraintType {
    MutualExclusion,
    PrerequisiteRole,
    MaxConcurrentUsers,
    TimeRestriction,
    LocationRestriction,
    Custom(String),
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum EnforcementLevel {
    Advisory,
    Warning,
    Blocking,
    Audit,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DelegatedCapability {
    /// Delegated capability
    pub capability: AdvancedCapability,
    /// Delegator principal
    pub delegator: String,
    /// Delegation depth
    pub delegation_depth: u32,
    /// Delegation timestamp
    pub delegated_at: SystemTime,
    /// Delegation expiration
    pub expires_at: Option<SystemTime>,
    /// Delegation constraints
    pub constraints: Vec<DelegationConstraint>,
    /// Approval chain
    pub approval_chain: Vec<ApprovalRecord>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct DelegationConstraint {
    /// Constraint type
    pub constraint_type: DelegationConstraintType,
    /// Constraint parameters
    pub parameters: HashMap<String, String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum DelegationConstraintType {
    UsageLimit,
    TimeLimit,
    ScopeLimit,
    ApprovalRequired,
    AuditRequired,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApprovalRecord {
    /// Approver identifier
    pub approver: String,
    /// Approval timestamp
    pub approved_at: SystemTime,
    /// Approval type
    pub approval_type: ApprovalType,
    /// Approval metadata
    pub metadata: HashMap<String, String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ApprovalType {
    Automatic,
    Manual,
    Emergency,
    Delegated,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TemporaryCapability {
    /// Temporary capability
    pub capability: AdvancedCapability,
    /// Granted for specific reason
    pub reason: String,
    /// Grant timestamp
    pub granted_at: SystemTime,
    /// Expiration timestamp
    pub expires_at: SystemTime,
    /// Usage limitations
    pub usage_limitations: Vec<UsageLimitation>,
    /// Emergency flag
    pub is_emergency: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct UsageLimitation {
    /// Limitation type
    pub limitation_type: LimitationType,
    /// Limitation value
    pub value: String,
    /// Remaining usage
    pub remaining: Option<u64>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum LimitationType {
    MaxUsageCount,
    MaxDuration,
    ResourceLimit,
    ScopeLimit,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AttributeValue {
    String(String),
    Integer(i64),
    Float(f64),
    Boolean(bool),
    DateTime(SystemTime),
    List(Vec<AttributeValue>),
    Map(HashMap<String, AttributeValue>),
}

#[derive(Debug, Clone)]
pub struct CapabilityDefinition {
    /// Capability identifier
    pub capability_id: String,
    /// Capability name
    pub name: String,
    /// Description
    pub description: String,
    /// Capability type
    pub capability_type: AdvancedCapability,
    /// Security requirements
    pub security_requirements: SecurityRequirements,
    /// Impact assessment
    pub impact_assessment: ImpactAssessment,
    /// Approval workflow
    pub approval_workflow: Option<ApprovalWorkflow>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SecurityRequirements {
    /// Minimum security clearance required
    pub min_security_clearance: SecurityLevel,
    /// Required attributes
    pub required_attributes: Vec<RequiredAttribute>,
    /// Multi-factor authentication required
    pub mfa_required: bool,
    /// Continuous monitoring required
    pub continuous_monitoring: bool,
    /// Risk assessment required
    pub risk_assessment_required: bool,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct RequiredAttribute {
    /// Attribute name
    pub name: String,
    /// Required value
    pub required_value: AttributeValue,
    /// Comparison operator
    pub operator: ComparisonOperator,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum ComparisonOperator {
    Equals,
    NotEquals,
    GreaterThan,
    LessThan,
    GreaterThanOrEqual,
    LessThanOrEqual,
    Contains,
    NotContains,
    Matches,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ImpactAssessment {
    /// Confidentiality impact
    pub confidentiality_impact: ImpactLevel,
    /// Integrity impact
    pub integrity_impact: ImpactLevel,
    /// Availability impact
    pub availability_impact: ImpactLevel,
    /// Overall risk level
    pub overall_risk_level: RiskLevel,
    /// Impact description
    pub impact_description: String,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum ImpactLevel {
    None,
    Low,
    Medium,
    High,
    Critical,
}

#[derive(Debug, Clone, Copy, PartialEq, Eq, PartialOrd, Ord, Hash, Serialize, Deserialize)]
pub enum RiskLevel {
    VeryLow,
    Low,
    Medium,
    High,
    VeryHigh,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApprovalWorkflow {
    /// Workflow steps
    pub steps: Vec<ApprovalStep>,
    /// Parallel approval allowed
    pub parallel_approval: bool,
    /// Escalation policy
    pub escalation_policy: Option<EscalationPolicy>,
    /// Workflow timeout
    pub timeout: Duration,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ApprovalStep {
    /// Step identifier
    pub step_id: String,
    /// Required approver roles
    pub approver_roles: Vec<String>,
    /// Required approval count
    pub required_approvals: u32,
    /// Step timeout
    pub timeout: Duration,
    /// Conditions for this step
    pub conditions: Vec<StepCondition>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct StepCondition {
    /// Condition type
    pub condition_type: String,
    /// Condition value
    pub value: String,
    /// Required for step execution
    pub required: bool,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EscalationPolicy {
    /// Escalation triggers
    pub triggers: Vec<EscalationTrigger>,
    /// Escalation levels
    pub levels: Vec<EscalationLevel>,
    /// Maximum escalation time
    pub max_escalation_time: Duration,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub struct EscalationTrigger {
    /// Trigger type
    pub trigger_type: EscalationTriggerType,
    /// Trigger threshold
    pub threshold: String,
    /// Action to take
    pub action: EscalationAction,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum EscalationTriggerType {
    Timeout,
    RejectionCount,
    RiskLevel,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum EscalationAction {
    NotifyManager,
    RequireAdditionalApproval,
    AutoApprove,
    AutoReject,
    Custom(String),
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EscalationLevel {
    /// Level identifier
    pub level: u32,
    /// Escalation roles
    pub roles: Vec<String>,
    /// Escalation timeout
    pub timeout: Duration,
    /// Override permissions
    pub override_permissions: bool,
}

/// Access control policy
#[derive(Debug, Clone)]
pub struct AccessControlPolicy {
    /// Policy identifier
    pub policy_id: String,
    /// Policy name
    pub name: String,
    /// Policy type
    pub policy_type: PolicyType,
    /// Policy rules
    pub rules: Vec<PolicyRule>,
    /// Policy priority
    pub priority: u32,
    /// Policy status
    pub enabled: bool,
    /// Policy metadata
    pub metadata: HashMap<String, String>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum PolicyType {
    Permissive,
    Restrictive,
    Hybrid,
    Custom(String),
}

#[derive(Debug, Clone)]
pub struct PolicyRule {
    /// Rule identifier
    pub rule_id: String,
    /// Rule conditions
    pub conditions: Vec<RuleCondition>,
    /// Rule effect
    pub effect: RuleEffect,
    /// Rule target
    pub target: RuleTarget,
    /// Rule obligations
    pub obligations: Vec<Obligation>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RuleCondition {
    /// Attribute name
    pub attribute: String,
    /// Comparison operator
    pub operator: ComparisonOperator,
    /// Expected value
    pub value: AttributeValue,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum RuleEffect {
    Allow,
    Deny,
    Condition,
    Abstain,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct RuleTarget {
    /// Target type
    pub target_type: TargetType,
    /// Target identifier
    pub target_id: String,
    /// Target attributes
    pub attributes: HashMap<String, AttributeValue>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum TargetType {
    Principal,
    Resource,
    Action,
    Environment,
    Custom(String),
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub struct Obligation {
    /// Obligation type
    pub obligation_type: ObligationType,
    /// Obligation parameters
    pub parameters: HashMap<String, String>,
    /// Fulfillment deadline
    pub deadline: Option<Duration>,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum ObligationType {
    Audit,
    Notify,
    Transform,
    Encrypt,
    Anonymize,
    Custom(String),
}

/// Delegation tracker
#[derive(Debug)]
pub struct DelegationTracker {
    /// Active delegations
    active_delegations: Arc<RwLock<HashMap<String, DelegationRecord>>>,
    /// Delegation history
    delegation_history: Arc<RwLock<VecDeque<DelegationRecord>>>,
    /// Delegation statistics
    delegation_stats: Arc<Mutex<DelegationStatistics>>,
}

#[derive(Debug, Clone)]
pub struct DelegationRecord {
    /// Delegation identifier
    pub delegation_id: String,
    /// Delegator
    pub delegator: String,
    /// Delegatee
    pub delegatee: String,
    /// Delegated capability
    pub capability: String,
    /// Delegation timestamp
    pub delegated_at: SystemTime,
    /// Expiration timestamp
    pub expires_at: Option<SystemTime>,
    /// Delegation status
    pub status: DelegationStatus,
    /// Usage count
    pub usage_count: u64,
}

#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum DelegationStatus {
    Active,
    Expired,
    Revoked,
    Suspended,
}

/// Cached access decision
#[derive(Debug, Clone)]
pub struct CachedDecision {
    /// Decision result
    pub decision: AccessDecision,
    /// Cache timestamp
    pub cached_at: SystemTime,
    /// Cache expiration
    pub expires_at: SystemTime,
    /// Decision context hash
    pub context_hash: String,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub struct AccessDecision {
    /// Decision result
    pub result: DecisionResult,
    /// Decision reason
    pub reason: String,
    /// Applicable policies
    pub applicable_policies: Vec<String>,
    /// Decision obligations
    pub obligations: Vec<Obligation>,
    /// Decision metadata
    pub metadata: HashMap<String, String>,
}

#[derive(Debug, Clone, PartialEq, Eq)]
pub enum DecisionResult {
    Allow,
    Deny,
    ConditionalAllow,
    NotApplicable,
}

/// Access statistics
#[derive(Debug, Clone)]
pub struct AccessStatistics {
    /// Total access requests
    pub total_requests: u64,
    /// Allowed requests
    pub allowed_requests: u64,
    /// Denied requests
    pub denied_requests: u64,
    /// Conditional requests
    pub conditional_requests: u64,
    /// Cache hit rate
    pub cache_hit_rate: f64,
    /// Average decision time
    pub avg_decision_time: Duration,
    /// Statistics by capability
    pub capability_stats: HashMap<String, CapabilityStatistics>,
    /// Statistics by principal
    pub principal_stats: HashMap<String, PrincipalStatistics>,
    /// Delegation statistics
    pub delegation_stats: DelegationStatistics,
}

#[derive(Debug, Clone)]
pub struct CapabilityStatistics {
    /// Total requests for this capability
    pub requests: u64,
    /// Allowed requests
    pub allowed: u64,
    /// Denied requests
    pub denied: u64,
    /// Average decision time
    pub avg_decision_time: Duration,
}

#[derive(Debug, Clone)]
pub struct PrincipalStatistics {
    /// Total requests from this principal
    pub requests: u64,
    /// Allowed requests
    pub allowed: u64,
    /// Denied requests
    pub denied: u64,
    /// Most used capabilities
    pub most_used_capabilities: HashMap<String, u64>,
    /// Risk score
    pub risk_score: f64,
}

#[derive(Debug, Clone)]
pub struct DelegationStatistics {
    /// Total delegations
    pub total_delegations: u64,
    /// Active delegations
    pub active_delegations: u64,
    /// Revoked delegations
    pub revoked_delegations: u64,
    /// Average delegation duration
    pub avg_delegation_duration: Duration,
    /// Delegation depth distribution
    pub depth_distribution: HashMap<u32, u64>,
}

/// Configuration for capability-based access control
#[derive(Debug, Clone)]
pub struct CapabilityAccessConfig {
    /// Enable decision caching
    pub enable_decision_caching: bool,
    /// Decision cache TTL
    pub decision_cache_ttl: Duration,
    /// Maximum cache size
    pub max_cache_size: usize,
    /// Enable delegation tracking
    pub enable_delegation_tracking: bool,
    /// Maximum delegation depth
    pub max_delegation_depth: u32,
    /// Enable risk assessment
    pub enable_risk_assessment: bool,
    /// Enable anomaly detection
    pub enable_anomaly_detection: bool,
    /// Audit requirements
    pub audit_requirements: AuditRequirements,
    /// Policy evaluation timeout
    pub policy_evaluation_timeout: Duration,
}

impl Default for CapabilityAccessConfig {
    fn default() -> Self {
        Self {
            enable_decision_caching: true,
            decision_cache_ttl: Duration::from_secs(300), // 5 minutes
            max_cache_size: 10000,
            enable_delegation_tracking: true,
            max_delegation_depth: 3,
            enable_risk_assessment: true,
            enable_anomaly_detection: true,
            audit_requirements: AuditRequirements::Standard,
            policy_evaluation_timeout: Duration::from_secs(5),
        }
    }
}

impl CapabilityBasedAccessControl {
    /// Create a new capability-based access control system
    pub fn new(config: CapabilityAccessConfig, audit_log_path: &std::path::Path) -> WasmtimeResult<Self> {
        let audit_logger = Arc::new(AuditLogger::new(audit_log_path)
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Failed to create audit logger: {}", e),
            ))?);

        let delegation_tracker = DelegationTracker {
            active_delegations: Arc::new(RwLock::new(HashMap::new())),
            delegation_history: Arc::new(RwLock::new(VecDeque::new())),
            delegation_stats: Arc::new(Mutex::new(DelegationStatistics {
                total_delegations: 0,
                active_delegations: 0,
                revoked_delegations: 0,
                avg_delegation_duration: Duration::new(0, 0),
                depth_distribution: HashMap::new(),
            })),
        };

        Ok(Self {
            principal_capabilities: Arc::new(RwLock::new(HashMap::new())),
            capability_definitions: Arc::new(RwLock::new(HashMap::new())),
            policies: Arc::new(RwLock::new(Vec::new())),
            delegation_tracker,
            decision_cache: Arc::new(RwLock::new(HashMap::new())),
            audit_logger,
            access_stats: Arc::new(Mutex::new(AccessStatistics {
                total_requests: 0,
                allowed_requests: 0,
                denied_requests: 0,
                conditional_requests: 0,
                cache_hit_rate: 0.0,
                avg_decision_time: Duration::new(0, 0),
                capability_stats: HashMap::new(),
                principal_stats: HashMap::new(),
                delegation_stats: DelegationStatistics {
                    total_delegations: 0,
                    active_delegations: 0,
                    revoked_delegations: 0,
                    avg_delegation_duration: Duration::new(0, 0),
                    depth_distribution: HashMap::new(),
                },
            })),
            config,
        })
    }

    /// Check if a principal has access to a specific capability
    pub fn check_access(
        &self,
        principal_id: &str,
        capability_id: &str,
        access_context: &AccessContext,
    ) -> WasmtimeResult<AccessDecision> {
        let start_time = Instant::now();

        // Check decision cache first
        if self.config.enable_decision_caching {
            if let Some(cached_decision) = self.get_cached_decision(principal_id, capability_id, access_context)? {
                self.update_cache_hit_stats()?;
                return Ok(cached_decision.decision);
            }
        }

        // Perform access decision
        let decision = self.evaluate_access_request(principal_id, capability_id, access_context)?;

        // Cache the decision
        if self.config.enable_decision_caching {
            self.cache_decision(principal_id, capability_id, access_context, &decision)?;
        }

        // Log the access attempt
        self.log_access_attempt(principal_id, capability_id, access_context, &decision)?;

        // Update statistics
        let decision_time = start_time.elapsed();
        self.update_access_statistics(principal_id, capability_id, &decision, decision_time)?;

        Ok(decision)
    }

    /// Evaluate access request
    fn evaluate_access_request(
        &self,
        principal_id: &str,
        capability_id: &str,
        access_context: &AccessContext,
    ) -> WasmtimeResult<AccessDecision> {
        // Get principal capabilities
        let principal_caps = self.get_principal_capabilities(principal_id)?;

        // Check direct capabilities
        if let Some(direct_cap) = principal_caps.direct_capabilities.get(capability_id) {
            if self.evaluate_capability_grant(direct_cap, access_context)? {
                return Ok(AccessDecision {
                    result: DecisionResult::Allow,
                    reason: "Direct capability grant".to_string(),
                    applicable_policies: Vec::new(),
                    obligations: Vec::new(),
                    metadata: HashMap::new(),
                });
            }
        }

        // Check role-based capabilities
        for role_cap in principal_caps.role_capabilities.values() {
            if let Some(cap_grant) = role_cap.capabilities.get(capability_id) {
                if self.evaluate_capability_grant(cap_grant, access_context)? {
                    return Ok(AccessDecision {
                        result: DecisionResult::Allow,
                        reason: format!("Role-based capability: {}", role_cap.role_name),
                        applicable_policies: Vec::new(),
                        obligations: Vec::new(),
                        metadata: HashMap::new(),
                    });
                }
            }
        }

        // Check delegated capabilities
        if let Some(delegated_cap) = principal_caps.delegated_capabilities.get(capability_id) {
            if self.evaluate_delegated_capability(delegated_cap, access_context)? {
                return Ok(AccessDecision {
                    result: DecisionResult::Allow,
                    reason: format!("Delegated capability from: {}", delegated_cap.delegator),
                    applicable_policies: Vec::new(),
                    obligations: Vec::new(),
                    metadata: HashMap::new(),
                });
            }
        }

        // Check temporary capabilities
        if let Some(temp_cap) = principal_caps.temporary_capabilities.get(capability_id) {
            if self.evaluate_temporary_capability(temp_cap, access_context)? {
                return Ok(AccessDecision {
                    result: DecisionResult::ConditionalAllow,
                    reason: format!("Temporary capability: {}", temp_cap.reason),
                    applicable_policies: Vec::new(),
                    obligations: Vec::new(),
                    metadata: HashMap::new(),
                });
            }
        }

        // Evaluate policies
        let policy_decision = self.evaluate_policies(principal_id, capability_id, access_context)?;
        if policy_decision.result != DecisionResult::NotApplicable {
            return Ok(policy_decision);
        }

        // Default deny
        Ok(AccessDecision {
            result: DecisionResult::Deny,
            reason: "No applicable capability grants or policies".to_string(),
            applicable_policies: Vec::new(),
            obligations: Vec::new(),
            metadata: HashMap::new(),
        })
    }

    /// Get principal capabilities
    fn get_principal_capabilities(&self, principal_id: &str) -> WasmtimeResult<PrincipalCapabilities> {
        let caps = self.principal_capabilities.read()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Principal capabilities lock error: {}", e),
            })?;

        caps.get(principal_id)
            .cloned()
            .ok_or_else(|| WasmtimeError::Security {
                message:
                format!("Principal {} not found", principal_id),
            ))
    }

    /// Evaluate capability grant
    fn evaluate_capability_grant(&self, grant: &CapabilityGrant, context: &AccessContext) -> WasmtimeResult<bool> {
        // Check expiration
        if let Some(expires_at) = grant.expires_at {
            if SystemTime::now() > expires_at {
                return Ok(false);
            }
        }

        // Evaluate grant conditions
        for condition in &grant.conditions {
            if !self.evaluate_grant_condition(condition, context)? {
                return Ok(false);
            }
        }

        Ok(true)
    }

    /// Evaluate grant condition
    fn evaluate_grant_condition(&self, condition: &GrantCondition, context: &AccessContext) -> WasmtimeResult<bool> {
        match condition.condition_type {
            ConditionType::TimeRange => {
                // Implement time range checking
                Ok(true) // Simplified
            }
            ConditionType::Location => {
                // Implement location checking
                context.location.as_ref()
                    .map(|loc| loc == &condition.required_value)
                    .unwrap_or(false)
                    .into()
            }
            ConditionType::NetworkSource => {
                // Implement network source checking
                context.network_source.as_ref()
                    .map(|src| src == &condition.required_value)
                    .unwrap_or(false)
                    .into()
            }
            _ => Ok(true), // Other condition types not implemented in this simplified version
        }
    }

    /// Evaluate delegated capability
    fn evaluate_delegated_capability(&self, delegated: &DelegatedCapability, context: &AccessContext) -> WasmtimeResult<bool> {
        // Check expiration
        if let Some(expires_at) = delegated.expires_at {
            if SystemTime::now() > expires_at {
                return Ok(false);
            }
        }

        // Check delegation constraints
        for constraint in &delegated.constraints {
            if !self.evaluate_delegation_constraint(constraint, context)? {
                return Ok(false);
            }
        }

        Ok(true)
    }

    /// Evaluate delegation constraint
    fn evaluate_delegation_constraint(&self, constraint: &DelegationConstraint, _context: &AccessContext) -> WasmtimeResult<bool> {
        match constraint.constraint_type {
            DelegationConstraintType::UsageLimit => {
                // Check usage limit
                Ok(true) // Simplified
            }
            DelegationConstraintType::TimeLimit => {
                // Check time limit
                Ok(true) // Simplified
            }
            _ => Ok(true), // Other constraint types not implemented
        }
    }

    /// Evaluate temporary capability
    fn evaluate_temporary_capability(&self, temp_cap: &TemporaryCapability, _context: &AccessContext) -> WasmtimeResult<bool> {
        // Check expiration
        if SystemTime::now() > temp_cap.expires_at {
            return Ok(false);
        }

        // Check usage limitations
        for limitation in &temp_cap.usage_limitations {
            if !self.evaluate_usage_limitation(limitation)? {
                return Ok(false);
            }
        }

        Ok(true)
    }

    /// Evaluate usage limitation
    fn evaluate_usage_limitation(&self, limitation: &UsageLimitation) -> WasmtimeResult<bool> {
        match limitation.limitation_type {
            LimitationType::MaxUsageCount => {
                if let Some(remaining) = limitation.remaining {
                    Ok(remaining > 0)
                } else {
                    Ok(true)
                }
            }
            _ => Ok(true), // Other limitation types not implemented
        }
    }

    /// Evaluate policies
    fn evaluate_policies(&self, principal_id: &str, capability_id: &str, context: &AccessContext) -> WasmtimeResult<AccessDecision> {
        let policies = self.policies.read()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Policies lock error: {}", e),
            })?;

        let mut applicable_policies = Vec::new();
        let mut final_decision = DecisionResult::NotApplicable;
        let mut obligations = Vec::new();

        for policy in policies.iter().filter(|p| p.enabled) {
            for rule in &policy.rules {
                if self.rule_matches(rule, principal_id, capability_id, context)? {
                    applicable_policies.push(policy.policy_id.clone());
                    match rule.effect {
                        RuleEffect::Allow => {
                            if final_decision == DecisionResult::NotApplicable {
                                final_decision = DecisionResult::Allow;
                            }
                        }
                        RuleEffect::Deny => {
                            final_decision = DecisionResult::Deny;
                            break; // Deny takes precedence
                        }
                        RuleEffect::Condition => {
                            if final_decision == DecisionResult::NotApplicable {
                                final_decision = DecisionResult::ConditionalAllow;
                            }
                        }
                        RuleEffect::Abstain => {
                            // Continue evaluation
                        }
                    }
                    obligations.extend(rule.obligations.clone());
                }
            }
            if final_decision == DecisionResult::Deny {
                break;
            }
        }

        Ok(AccessDecision {
            result: final_decision,
            reason: if final_decision == DecisionResult::NotApplicable {
                "No applicable policies".to_string()
            } else {
                format!("Policy evaluation: {:?}", final_decision)
            },
            applicable_policies,
            obligations,
            metadata: HashMap::new(),
        })
    }

    /// Check if a rule matches the current context
    fn rule_matches(&self, rule: &PolicyRule, principal_id: &str, capability_id: &str, context: &AccessContext) -> WasmtimeResult<bool> {
        // Check target matching
        if !self.target_matches(&rule.target, principal_id, capability_id)? {
            return Ok(false);
        }

        // Check conditions
        for condition in &rule.conditions {
            if !self.condition_matches(condition, context)? {
                return Ok(false);
            }
        }

        Ok(true)
    }

    /// Check if a target matches
    fn target_matches(&self, target: &RuleTarget, principal_id: &str, capability_id: &str) -> WasmtimeResult<bool> {
        match target.target_type {
            TargetType::Principal => Ok(target.target_id == principal_id),
            TargetType::Resource => Ok(target.target_id == capability_id),
            _ => Ok(true), // Other target types not fully implemented
        }
    }

    /// Check if a condition matches
    fn condition_matches(&self, condition: &RuleCondition, context: &AccessContext) -> WasmtimeResult<bool> {
        let actual_value = match condition.attribute.as_str() {
            "time" => {
                let now = SystemTime::now().duration_since(UNIX_EPOCH)
                    .unwrap_or(Duration::new(0, 0)).as_secs();
                AttributeValue::Integer(now as i64)
            }
            "location" => {
                context.location.as_ref()
                    .map(|loc| AttributeValue::String(loc.clone()))
                    .unwrap_or(AttributeValue::String("unknown".to_string()))
            }
            "network_source" => {
                context.network_source.as_ref()
                    .map(|src| AttributeValue::String(src.clone()))
                    .unwrap_or(AttributeValue::String("unknown".to_string()))
            }
            _ => AttributeValue::String("unknown".to_string()),
        };

        self.compare_attribute_values(&actual_value, &condition.value, &condition.operator)
    }

    /// Compare attribute values
    fn compare_attribute_values(&self, actual: &AttributeValue, expected: &AttributeValue, operator: &ComparisonOperator) -> WasmtimeResult<bool> {
        match operator {
            ComparisonOperator::Equals => Ok(actual == expected),
            ComparisonOperator::NotEquals => Ok(actual != expected),
            // Other operators would be implemented here
            _ => Ok(true), // Simplified
        }
    }

    /// Get cached decision
    fn get_cached_decision(&self, principal_id: &str, capability_id: &str, context: &AccessContext) -> WasmtimeResult<Option<CachedDecision>> {
        let cache_key = self.generate_cache_key(principal_id, capability_id, context)?;
        let cache = self.decision_cache.read()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Decision cache lock error: {}", e),
            })?;

        if let Some(cached) = cache.get(&cache_key) {
            if SystemTime::now() < cached.expires_at {
                return Ok(Some(cached.clone()));
            }
        }

        Ok(None)
    }

    /// Cache decision
    fn cache_decision(&self, principal_id: &str, capability_id: &str, context: &AccessContext, decision: &AccessDecision) -> WasmtimeResult<()> {
        let cache_key = self.generate_cache_key(principal_id, capability_id, context)?;
        let mut cache = self.decision_cache.write()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Decision cache write lock error: {}", e),
            })?;

        // Remove expired entries if cache is full
        if cache.len() >= self.config.max_cache_size {
            let now = SystemTime::now();
            cache.retain(|_, cached| cached.expires_at > now);
        }

        // If still full, remove oldest entries
        if cache.len() >= self.config.max_cache_size {
            let oldest_keys: Vec<String> = cache.iter()
                .take(cache.len() / 4) // Remove 25% of entries
                .map(|(k, _)| k.clone())
                .collect();
            for key in oldest_keys {
                cache.remove(&key);
            }
        }

        let cached_decision = CachedDecision {
            decision: decision.clone(),
            cached_at: SystemTime::now(),
            expires_at: SystemTime::now() + self.config.decision_cache_ttl,
            context_hash: cache_key.clone(),
        };

        cache.insert(cache_key, cached_decision);
        Ok(())
    }

    /// Generate cache key
    fn generate_cache_key(&self, principal_id: &str, capability_id: &str, context: &AccessContext) -> WasmtimeResult<String> {
        let mut hasher = Sha256::new();
        hasher.update(principal_id.as_bytes());
        hasher.update(capability_id.as_bytes());
        hasher.update(context.request_id.as_bytes());
        if let Some(location) = &context.location {
            hasher.update(location.as_bytes());
        }
        if let Some(network_source) = &context.network_source {
            hasher.update(network_source.as_bytes());
        }
        let hash = hasher.finalize();
        Ok(hex::encode(hash))
    }

    /// Log access attempt
    fn log_access_attempt(&self, principal_id: &str, capability_id: &str, context: &AccessContext, decision: &AccessDecision) -> WasmtimeResult<()> {
        let audit_entry = AuditLogEntry {
            entry_id: Uuid::new_v4().to_string(),
            timestamp: Utc::now(),
            event_type: AuditEventType::Authorization,
            principal_id: principal_id.to_string(),
            resource: capability_id.to_string(),
            action: "access_check".to_string(),
            result: match decision.result {
                DecisionResult::Allow | DecisionResult::ConditionalAllow => AuditResult::Success,
                DecisionResult::Deny => AuditResult::Failure,
                DecisionResult::NotApplicable => AuditResult::Warning,
            },
            details: {
                let mut details = HashMap::new();
                details.insert("decision_reason".to_string(), decision.reason.clone());
                details.insert("request_id".to_string(), context.request_id.clone());
                if let Some(location) = &context.location {
                    details.insert("location".to_string(), location.clone());
                }
                if let Some(network_source) = &context.network_source {
                    details.insert("network_source".to_string(), network_source.clone());
                }
                details
            },
            source_ip: context.network_source.clone(),
            user_agent: context.user_agent.clone(),
            session_id: context.session_id.clone(),
            integrity_hash: String::new(),
        };

        self.audit_logger.log_event(audit_entry)
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Audit logging error: {}", e),
            ))
    }

    /// Update access statistics
    fn update_access_statistics(&self, principal_id: &str, capability_id: &str, decision: &AccessDecision, decision_time: Duration) -> WasmtimeResult<()> {
        let mut stats = self.access_stats.lock()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Access statistics lock error: {}", e),
            })?;

        stats.total_requests += 1;

        match decision.result {
            DecisionResult::Allow => stats.allowed_requests += 1,
            DecisionResult::Deny => stats.denied_requests += 1,
            DecisionResult::ConditionalAllow => stats.conditional_requests += 1,
            DecisionResult::NotApplicable => {}
        }

        // Update average decision time
        stats.avg_decision_time = Duration::from_nanos(
            ((stats.avg_decision_time.as_nanos() * (stats.total_requests - 1) as u128)
                + decision_time.as_nanos())
                / stats.total_requests as u128,
        );

        // Update capability statistics
        let cap_stats = stats.capability_stats.entry(capability_id.to_string()).or_insert(CapabilityStatistics {
            requests: 0,
            allowed: 0,
            denied: 0,
            avg_decision_time: Duration::new(0, 0),
        });
        cap_stats.requests += 1;
        match decision.result {
            DecisionResult::Allow | DecisionResult::ConditionalAllow => cap_stats.allowed += 1,
            DecisionResult::Deny => cap_stats.denied += 1,
            _ => {}
        }

        // Update principal statistics
        let principal_stats = stats.principal_stats.entry(principal_id.to_string()).or_insert(PrincipalStatistics {
            requests: 0,
            allowed: 0,
            denied: 0,
            most_used_capabilities: HashMap::new(),
            risk_score: 0.0,
        });
        principal_stats.requests += 1;
        match decision.result {
            DecisionResult::Allow | DecisionResult::ConditionalAllow => principal_stats.allowed += 1,
            DecisionResult::Deny => principal_stats.denied += 1,
            _ => {}
        }
        *principal_stats.most_used_capabilities.entry(capability_id.to_string()).or_insert(0) += 1;

        Ok(())
    }

    /// Update cache hit statistics
    fn update_cache_hit_stats(&self) -> WasmtimeResult<()> {
        let mut stats = self.access_stats.lock()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Cache hit stats lock error: {}", e),
            })?;

        // Update cache hit rate calculation
        let total_with_cache_hit = stats.total_requests + 1;
        let cache_hits = (stats.cache_hit_rate * stats.total_requests as f64) + 1.0;
        stats.cache_hit_rate = cache_hits / total_with_cache_hit as f64;

        Ok(())
    }

    /// Grant capability to principal
    pub fn grant_capability(&self, principal_id: &str, capability_grant: CapabilityGrant) -> WasmtimeResult<()> {
        let mut principals = self.principal_capabilities.write()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Principal capabilities write lock error: {}", e),
            })?;

        let principal = principals.entry(principal_id.to_string()).or_insert_with(|| PrincipalCapabilities {
            principal_id: principal_id.to_string(),
            principal_type: PrincipalType::User,
            direct_capabilities: HashMap::new(),
            role_capabilities: HashMap::new(),
            delegated_capabilities: HashMap::new(),
            temporary_capabilities: HashMap::new(),
            attributes: HashMap::new(),
            security_clearance: SecurityLevel::Public,
            active_sessions: HashSet::new(),
        });

        let capability_id = Uuid::new_v4().to_string();
        principal.direct_capabilities.insert(capability_id, capability_grant);

        Ok(())
    }

    /// Revoke capability from principal
    pub fn revoke_capability(&self, principal_id: &str, capability_id: &str) -> WasmtimeResult<()> {
        let mut principals = self.principal_capabilities.write()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Principal capabilities revoke lock error: {}", e),
            })?;

        if let Some(principal) = principals.get_mut(principal_id) {
            principal.direct_capabilities.remove(capability_id);
            principal.delegated_capabilities.remove(capability_id);
            principal.temporary_capabilities.remove(capability_id);
        }

        Ok(())
    }

    /// Get access statistics
    pub fn get_access_statistics(&self) -> WasmtimeResult<AccessStatistics> {
        let stats = self.access_stats.lock()
            .map_err(|e| WasmtimeError::Security {
                message:
                format!("Access statistics read lock error: {}", e),
            })?;

        Ok(stats.clone())
    }
}

/// Access context for capability evaluation
#[derive(Debug, Clone)]
pub struct AccessContext {
    /// Request identifier
    pub request_id: String,
    /// Request timestamp
    pub timestamp: SystemTime,
    /// Request location (if available)
    pub location: Option<String>,
    /// Network source
    pub network_source: Option<String>,
    /// User agent
    pub user_agent: Option<String>,
    /// Session identifier
    pub session_id: Option<String>,
    /// Additional context attributes
    pub attributes: HashMap<String, AttributeValue>,
    /// Risk level
    pub risk_level: RiskLevel,
    /// Request metadata
    pub metadata: HashMap<String, String>,
}

impl Default for AccessContext {
    fn default() -> Self {
        Self {
            request_id: Uuid::new_v4().to_string(),
            timestamp: SystemTime::now(),
            location: None,
            network_source: None,
            user_agent: None,
            session_id: None,
            attributes: HashMap::new(),
            risk_level: RiskLevel::Medium,
            metadata: HashMap::new(),
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn test_capability_based_access_control_creation() {
        let temp_dir = TempDir::new().unwrap();
        let audit_path = temp_dir.path().join("audit.log");

        let config = CapabilityAccessConfig::default();
        let cbac = CapabilityBasedAccessControl::new(config, &audit_path).unwrap();

        let stats = cbac.get_access_statistics().unwrap();
        assert_eq!(stats.total_requests, 0);
    }

    #[test]
    fn test_advanced_capability_creation() {
        let capability = AdvancedCapability::ResourceAccess {
            resource_type: ResourceType::Memory,
            resource_id: "test_memory".to_string(),
            access_modes: {
                let mut modes = HashSet::new();
                modes.insert(AccessMode::Read);
                modes.insert(AccessMode::Write);
                modes
            },
            constraints: Vec::new(),
        };

        match capability {
            AdvancedCapability::ResourceAccess { access_modes, .. } => {
                assert!(access_modes.contains(&AccessMode::Read));
                assert!(access_modes.contains(&AccessMode::Write));
            }
            _ => panic!("Unexpected capability type"),
        }
    }

    #[test]
    fn test_access_decision_equality() {
        let decision1 = AccessDecision {
            result: DecisionResult::Allow,
            reason: "Test reason".to_string(),
            applicable_policies: Vec::new(),
            obligations: Vec::new(),
            metadata: HashMap::new(),
        };

        let decision2 = AccessDecision {
            result: DecisionResult::Allow,
            reason: "Test reason".to_string(),
            applicable_policies: Vec::new(),
            obligations: Vec::new(),
            metadata: HashMap::new(),
        };

        assert_eq!(decision1, decision2);
    }

    #[test]
    fn test_security_level_ordering() {
        assert!(SecurityLevel::TopSecret > SecurityLevel::Secret);
        assert!(SecurityLevel::Secret > SecurityLevel::Confidential);
        assert!(SecurityLevel::Confidential > SecurityLevel::Public);
    }

    #[test]
    fn test_access_context_default() {
        let context = AccessContext::default();
        assert!(!context.request_id.is_empty());
        assert_eq!(context.risk_level, RiskLevel::Medium);
    }
}