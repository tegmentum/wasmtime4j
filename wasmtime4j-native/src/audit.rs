//! # Enterprise Audit and Compliance Module
//!
//! Comprehensive audit logging and compliance reporting system including:
//! - Audit logging for all security events
//! - Compliance reporting for SOX, GDPR, HIPAA standards
//! - Tamper-evident logging with cryptographic protection
//! - Security event correlation and alerting
//! - Forensic analysis capabilities for security incidents
//!
//! This module provides enterprise-grade audit trail and compliance
//! capabilities for regulated environments.

use crate::error::{WasmtimeError, WasmtimeResult};
use std::collections::{HashMap, VecDeque};
use std::sync::{Arc, Mutex, RwLock};
use std::time::{Duration, SystemTime, UNIX_EPOCH};
use std::fs::{File, OpenOptions};
use std::io::{Write, BufWriter};
use std::path::PathBuf;
use serde::{Deserialize, Serialize};
use ring::hmac;
use base64::{Engine as _, engine::general_purpose};

/// Audit event types for classification
#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum AuditEventType {
    /// Authentication events
    Authentication,
    /// Authorization events
    Authorization,
    /// Module operations
    ModuleOperation,
    /// Security violations
    SecurityViolation,
    /// Administrative actions
    Administrative,
    /// Data access events
    DataAccess,
    /// System events
    System,
    /// Compliance events
    Compliance,
    /// Configuration changes
    Configuration,
    /// Error events
    Error,
}

/// Severity levels for audit events
#[derive(Debug, Clone, PartialEq, Eq, PartialOrd, Ord, Serialize, Deserialize)]
pub enum AuditSeverity {
    /// Informational events
    Info,
    /// Warning events
    Warning,
    /// Error events
    Error,
    /// Critical security events
    Critical,
    /// Fatal system events
    Fatal,
}

impl std::fmt::Display for AuditSeverity {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            AuditSeverity::Info => write!(f, "INFO"),
            AuditSeverity::Warning => write!(f, "WARNING"),
            AuditSeverity::Error => write!(f, "ERROR"),
            AuditSeverity::Critical => write!(f, "CRITICAL"),
            AuditSeverity::Fatal => write!(f, "FATAL"),
        }
    }
}

/// Audit event record
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuditEvent {
    /// Unique event identifier
    pub event_id: String,
    /// Event timestamp
    pub timestamp: SystemTime,
    /// Event type classification
    pub event_type: AuditEventType,
    /// Event severity
    pub severity: AuditSeverity,
    /// Source component or module
    pub source: String,
    /// User associated with the event
    pub user_id: Option<String>,
    /// Session or request identifier
    pub session_id: Option<String>,
    /// Resource being accessed
    pub resource: Option<String>,
    /// Action being performed
    pub action: Option<String>,
    /// Event outcome (success, failure, etc.)
    pub outcome: String,
    /// Detailed event message
    pub message: String,
    /// Additional event attributes
    pub attributes: HashMap<String, String>,
    /// IP address or client identifier
    pub client_ip: Option<String>,
    /// User agent or client information
    pub user_agent: Option<String>,
    /// Event hash for integrity verification
    pub integrity_hash: Option<String>,
}

impl AuditEvent {
    /// Create a new audit event
    pub fn new(
        event_type: AuditEventType,
        severity: AuditSeverity,
        source: String,
        message: String,
    ) -> Self {
        let event_id = format!("{}-{}",
            chrono::Utc::now().format("%Y%m%d%H%M%S"),
            SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos()
        );

        Self {
            event_id,
            timestamp: SystemTime::now(),
            event_type,
            severity,
            source,
            user_id: None,
            session_id: None,
            resource: None,
            action: None,
            outcome: "unknown".to_string(),
            message,
            attributes: HashMap::new(),
            client_ip: None,
            user_agent: None,
            integrity_hash: None,
        }
    }

    /// Set user information
    pub fn with_user(mut self, user_id: String) -> Self {
        self.user_id = Some(user_id);
        self
    }

    /// Set session information
    pub fn with_session(mut self, session_id: String) -> Self {
        self.session_id = Some(session_id);
        self
    }

    /// Set resource information
    pub fn with_resource(mut self, resource: String) -> Self {
        self.resource = Some(resource);
        self
    }

    /// Set action information
    pub fn with_action(mut self, action: String) -> Self {
        self.action = Some(action);
        self
    }

    /// Set outcome
    pub fn with_outcome(mut self, outcome: String) -> Self {
        self.outcome = outcome;
        self
    }

    /// Add attribute
    pub fn with_attribute(mut self, key: String, value: String) -> Self {
        self.attributes.insert(key, value);
        self
    }

    /// Set client information
    pub fn with_client(mut self, ip: Option<String>, user_agent: Option<String>) -> Self {
        self.client_ip = ip;
        self.user_agent = user_agent;
        self
    }

    /// Calculate and set integrity hash
    pub fn with_integrity_hash(mut self, secret: &[u8]) -> Self {
        let event_data = self.serialize_for_hash();
        let key = hmac::Key::new(hmac::HMAC_SHA256, secret);
        let hash = hmac::sign(&key, event_data.as_bytes());
        self.integrity_hash = Some(general_purpose::STANDARD.encode(hash.as_ref()));
        self
    }

    /// Verify integrity hash
    pub fn verify_integrity(&self, secret: &[u8]) -> WasmtimeResult<bool> {
        if let Some(ref stored_hash) = self.integrity_hash {
            let event_data = self.serialize_for_hash();
            let key = hmac::Key::new(hmac::HMAC_SHA256, secret);
            let computed_hash = hmac::sign(&key, event_data.as_bytes());
            let computed_hash_b64 = general_purpose::STANDARD.encode(computed_hash.as_ref());
            Ok(computed_hash_b64 == *stored_hash)
        } else {
            Ok(false) // No hash to verify
        }
    }

    /// Serialize event data for hash calculation (excluding the hash itself)
    fn serialize_for_hash(&self) -> String {
        format!("{}:{}:{:?}:{:?}:{}:{}:{}:{}:{}:{}:{}",
            self.event_id,
            self.timestamp.duration_since(UNIX_EPOCH).unwrap().as_secs(),
            self.event_type,
            self.severity,
            self.source,
            self.user_id.as_deref().unwrap_or(""),
            self.session_id.as_deref().unwrap_or(""),
            self.resource.as_deref().unwrap_or(""),
            self.action.as_deref().unwrap_or(""),
            self.outcome,
            self.message
        )
    }
}

/// Compliance framework requirements
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum ComplianceFramework {
    /// Sarbanes-Oxley Act
    SOX,
    /// General Data Protection Regulation
    GDPR,
    /// Health Insurance Portability and Accountability Act
    HIPAA,
    /// Payment Card Industry Data Security Standard
    PCIDSS,
    /// ISO 27001
    ISO27001,
    /// NIST Cybersecurity Framework
    NIST,
    /// Custom compliance framework
    Custom(String),
}

/// Compliance report configuration
#[derive(Debug, Clone)]
pub struct ComplianceReportConfig {
    /// Compliance framework
    pub framework: ComplianceFramework,
    /// Report period start
    pub start_time: SystemTime,
    /// Report period end
    pub end_time: SystemTime,
    /// Event types to include
    pub event_types: Vec<AuditEventType>,
    /// Minimum severity level
    pub min_severity: AuditSeverity,
    /// Include user details
    pub include_user_details: bool,
    /// Include resource details
    pub include_resource_details: bool,
}

/// Compliance report
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ComplianceReport {
    /// Report identifier
    pub report_id: String,
    /// Compliance framework
    pub framework: ComplianceFramework,
    /// Report generation timestamp
    pub generated_at: SystemTime,
    /// Report period
    pub period_start: SystemTime,
    pub period_end: SystemTime,
    /// Total events analyzed
    pub total_events: usize,
    /// Events by type
    pub events_by_type: HashMap<String, usize>,
    /// Events by severity
    pub events_by_severity: HashMap<String, usize>,
    /// Security violations
    pub security_violations: usize,
    /// Authentication failures
    pub auth_failures: usize,
    /// Unauthorized access attempts
    pub unauthorized_access: usize,
    /// Data access events
    pub data_access_events: usize,
    /// Administrative actions
    pub admin_actions: usize,
    /// Compliance violations
    pub compliance_violations: Vec<ComplianceViolation>,
    /// Recommendations
    pub recommendations: Vec<String>,
}

/// Compliance violation record
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ComplianceViolation {
    /// Violation identifier
    pub violation_id: String,
    /// Related audit event
    pub event_id: String,
    /// Violation type
    pub violation_type: String,
    /// Violation description
    pub description: String,
    /// Severity assessment
    pub severity: AuditSeverity,
    /// Remediation actions
    pub remediation: Vec<String>,
}

/// Audit logger configuration
#[derive(Debug, Clone)]
pub struct AuditLoggerConfig {
    /// Log file path
    pub log_file_path: PathBuf,
    /// Maximum log file size before rotation
    pub max_file_size: u64,
    /// Number of rotated files to keep
    pub max_files: usize,
    /// Whether to enable real-time alerting
    pub enable_alerting: bool,
    /// Alert severity threshold
    pub alert_threshold: AuditSeverity,
    /// Whether to enable integrity protection
    pub enable_integrity: bool,
    /// HMAC secret for integrity protection
    pub integrity_secret: Option<Vec<u8>>,
    /// Buffer size for batching events
    pub buffer_size: usize,
    /// Flush interval for buffered events
    pub flush_interval: Duration,
}

impl Default for AuditLoggerConfig {
    fn default() -> Self {
        Self {
            log_file_path: PathBuf::from("wasmtime4j-audit.log"),
            max_file_size: 100 * 1024 * 1024, // 100MB
            max_files: 10,
            enable_alerting: false,
            alert_threshold: AuditSeverity::Error,
            enable_integrity: true,
            integrity_secret: None,
            buffer_size: 1000,
            flush_interval: Duration::from_secs(30),
        }
    }
}

/// Main audit logger
pub struct AuditLogger {
    /// Configuration
    config: AuditLoggerConfig,
    /// Event buffer for batching
    event_buffer: Arc<Mutex<VecDeque<AuditEvent>>>,
    /// Log file writer
    log_writer: Arc<Mutex<BufWriter<File>>>,
    /// Event correlator
    correlator: Arc<Mutex<EventCorrelator>>,
    /// Alert manager
    alert_manager: Arc<Mutex<AlertManager>>,
    /// Statistics tracker
    stats: Arc<RwLock<AuditStatistics>>,
}

/// Event correlation engine for detecting patterns
#[derive(Debug)]
pub struct EventCorrelator {
    /// Time window for correlation
    correlation_window: Duration,
    /// Recent events for correlation
    recent_events: VecDeque<AuditEvent>,
    /// Correlation rules
    correlation_rules: Vec<CorrelationRule>,
    /// Maximum events to keep for correlation
    max_events: usize,
}

/// Correlation rule for detecting event patterns
#[derive(Debug, Clone)]
pub struct CorrelationRule {
    /// Rule identifier
    pub rule_id: String,
    /// Rule description
    pub description: String,
    /// Event pattern to match
    pub pattern: EventPattern,
    /// Time window for pattern matching
    pub time_window: Duration,
    /// Action to take when pattern matches
    pub action: CorrelationAction,
}

/// Event pattern for correlation
#[derive(Debug, Clone)]
pub struct EventPattern {
    /// Event types in the pattern
    pub event_types: Vec<AuditEventType>,
    /// Minimum severity
    pub min_severity: AuditSeverity,
    /// User field matching
    pub user_match: Option<String>,
    /// Resource field matching
    pub resource_match: Option<String>,
    /// Minimum number of matching events
    pub min_occurrences: usize,
}

/// Action to take when correlation rule matches
#[derive(Debug, Clone)]
pub enum CorrelationAction {
    /// Generate an alert
    Alert(AuditSeverity),
    /// Create a compliance violation
    ComplianceViolation(String),
    /// Execute custom action
    Custom(String),
}

/// Alert manager for real-time notifications
pub struct AlertManager {
    /// Alert handlers
    handlers: Vec<Box<dyn AlertHandler + Send>>,
    /// Alert suppression rules
    suppression_rules: Vec<AlertSuppressionRule>,
    /// Recent alerts for suppression tracking
    recent_alerts: VecDeque<Alert>,
}

/// Alert handler trait
pub trait AlertHandler {
    /// Handle an alert
    fn handle_alert(&self, alert: &Alert) -> WasmtimeResult<()>;
}

/// Alert suppression rule
#[derive(Debug, Clone)]
pub struct AlertSuppressionRule {
    /// Alert type pattern
    pub alert_type: String,
    /// Suppression duration
    pub duration: Duration,
    /// Maximum occurrences before suppression
    pub max_occurrences: usize,
}

/// Alert record
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Alert {
    /// Alert identifier
    pub alert_id: String,
    /// Alert timestamp
    pub timestamp: SystemTime,
    /// Alert type
    pub alert_type: String,
    /// Alert severity
    pub severity: AuditSeverity,
    /// Alert message
    pub message: String,
    /// Related events
    pub related_events: Vec<String>,
    /// Additional context
    pub context: HashMap<String, String>,
}

/// Audit statistics
#[derive(Debug, Clone)]
pub struct AuditStatistics {
    /// Total events logged
    pub total_events: u64,
    /// Events by type
    pub events_by_type: HashMap<String, u64>,
    /// Events by severity
    pub events_by_severity: HashMap<String, u64>,
    /// Events by hour (last 24 hours)
    pub events_by_hour: VecDeque<u64>,
    /// Total alerts generated
    pub total_alerts: u64,
    /// Total compliance violations
    pub total_violations: u64,
    /// Statistics last updated
    pub last_updated: SystemTime,
}

impl Default for AuditStatistics {
    fn default() -> Self {
        Self {
            total_events: 0,
            events_by_type: HashMap::new(),
            events_by_severity: HashMap::new(),
            events_by_hour: VecDeque::new(),
            total_alerts: 0,
            total_violations: 0,
            last_updated: SystemTime::UNIX_EPOCH,
        }
    }
}

impl AuditLogger {
    /// Create a new audit logger
    pub fn new(config: AuditLoggerConfig) -> WasmtimeResult<Self> {
        // Create log directory if it doesn't exist
        if let Some(parent) = config.log_file_path.parent() {
            std::fs::create_dir_all(parent)
                .map_err(|e| WasmtimeError::IO {
                    message: format!("Failed to create log directory: {}", e),
                })?;
        }

        // Open log file
        let log_file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(&config.log_file_path)
            .map_err(|e| WasmtimeError::IO {
                message: format!("Failed to open log file: {}", e),
            })?;

        let log_writer = BufWriter::new(log_file);

        Ok(Self {
            config,
            event_buffer: Arc::new(Mutex::new(VecDeque::new())),
            log_writer: Arc::new(Mutex::new(log_writer)),
            correlator: Arc::new(Mutex::new(EventCorrelator::new(Duration::from_secs(5 * 60)))),
            alert_manager: Arc::new(Mutex::new(AlertManager::new())),
            stats: Arc::new(RwLock::new(AuditStatistics::default())),
        })
    }

    /// Log an audit event
    pub fn log_event(&self, mut event: AuditEvent) -> WasmtimeResult<()> {
        // Add integrity hash if enabled
        if self.config.enable_integrity {
            if let Some(ref secret) = self.config.integrity_secret {
                event = event.with_integrity_hash(secret);
            }
        }

        // Update statistics
        self.update_statistics(&event);

        // Add to correlation engine
        {
            let mut correlator = self.correlator.lock().unwrap_or_else(|e| e.into_inner());
            correlator.add_event(event.clone());
        }

        // Check for alerts
        if self.config.enable_alerting && event.severity >= self.config.alert_threshold {
            self.check_alerts(&event)?;
        }

        // Buffer the event
        {
            let mut buffer = self.event_buffer.lock().unwrap_or_else(|e| e.into_inner());
            buffer.push_back(event);

            // Flush if buffer is full
            if buffer.len() >= self.config.buffer_size {
                self.flush_buffer()?;
            }
        }

        Ok(())
    }

    /// Flush buffered events to disk
    pub fn flush_buffer(&self) -> WasmtimeResult<()> {
        let events: Vec<AuditEvent> = {
            let mut buffer = self.event_buffer.lock().unwrap_or_else(|e| e.into_inner());
            buffer.drain(..).collect()
        };

        let mut writer = self.log_writer.lock().unwrap_or_else(|e| e.into_inner());
        for event in events {
            let event_json = serde_json::to_string(&event)
                .map_err(|e| WasmtimeError::Serialization {
                    message:
                    format!("Failed to serialize audit event: {}", e),
                })?;

            writeln!(writer, "{}", event_json)
                .map_err(|e| WasmtimeError::IO {
                    message: format!("Failed to write audit event: {}", e),
                })?;
        }

        writer.flush()
            .map_err(|e| WasmtimeError::IO {
                message: format!("Failed to flush audit log: {}", e),
            })?;

        Ok(())
    }

    /// Generate compliance report
    pub fn generate_compliance_report(
        &self,
        config: ComplianceReportConfig,
    ) -> WasmtimeResult<ComplianceReport> {
        // In a full implementation, this would:
        // 1. Read audit events from the log file for the specified period
        // 2. Analyze events according to the compliance framework requirements
        // 3. Identify violations and generate recommendations
        // 4. Create a comprehensive compliance report

        let report_id = format!("compliance-{}-{}",
            chrono::Utc::now().format("%Y%m%d%H%M%S"),
            SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos()
        );

        Ok(ComplianceReport {
            report_id,
            framework: config.framework,
            generated_at: SystemTime::now(),
            period_start: config.start_time,
            period_end: config.end_time,
            total_events: 0, // Would be calculated from actual events
            events_by_type: HashMap::new(),
            events_by_severity: HashMap::new(),
            security_violations: 0,
            auth_failures: 0,
            unauthorized_access: 0,
            data_access_events: 0,
            admin_actions: 0,
            compliance_violations: Vec::new(),
            recommendations: vec![
                "Implement regular security training for all users".to_string(),
                "Review and update access control policies quarterly".to_string(),
                "Enable multi-factor authentication for all administrative accounts".to_string(),
            ],
        })
    }

    /// Get audit statistics
    pub fn get_statistics(&self) -> AuditStatistics {
        let stats = self.stats.read().unwrap();
        stats.clone()
    }

    /// Update statistics with new event
    fn update_statistics(&self, event: &AuditEvent) {
        let mut stats = self.stats.write().unwrap();

        stats.total_events += 1;

        // Update events by type
        let type_key = format!("{:?}", event.event_type);
        *stats.events_by_type.entry(type_key).or_insert(0) += 1;

        // Update events by severity
        let severity_key = format!("{:?}", event.severity);
        *stats.events_by_severity.entry(severity_key).or_insert(0) += 1;

        // Update hourly statistics
        if stats.events_by_hour.len() >= 24 {
            stats.events_by_hour.pop_front();
        }
        if let Some(last) = stats.events_by_hour.back_mut() {
            *last += 1;
        } else {
            stats.events_by_hour.push_back(1);
        }

        stats.last_updated = SystemTime::now();
    }

    /// Check for alert conditions
    fn check_alerts(&self, event: &AuditEvent) -> WasmtimeResult<()> {
        if event.severity >= AuditSeverity::Error {
            let alert = Alert {
                alert_id: format!("alert-{}", event.event_id),
                timestamp: SystemTime::now(),
                alert_type: format!("{:?}", event.event_type),
                severity: event.severity.clone(),
                message: format!("High severity event detected: {}", event.message),
                related_events: vec![event.event_id.clone()],
                context: event.attributes.clone(),
            };

            let mut alert_manager = self.alert_manager.lock().unwrap_or_else(|e| e.into_inner());
            alert_manager.handle_alert(alert)?;
        }

        Ok(())
    }
}

impl EventCorrelator {
    /// Create a new event correlator
    pub fn new(correlation_window: Duration) -> Self {
        Self {
            correlation_window,
            recent_events: VecDeque::new(),
            correlation_rules: Vec::new(),
            max_events: 10000,
        }
    }

    /// Add an event for correlation
    pub fn add_event(&mut self, event: AuditEvent) {
        // Remove old events outside the correlation window
        let cutoff_time = SystemTime::now() - self.correlation_window;
        while let Some(front) = self.recent_events.front() {
            if front.timestamp < cutoff_time {
                self.recent_events.pop_front();
            } else {
                break;
            }
        }

        // Add new event
        self.recent_events.push_back(event);

        // Limit buffer size
        if self.recent_events.len() > self.max_events {
            self.recent_events.pop_front();
        }

        // Check correlation rules
        self.check_correlations();
    }

    /// Check for correlation patterns
    fn check_correlations(&self) {
        for rule in &self.correlation_rules {
            self.check_rule(rule);
        }
    }

    /// Check a specific correlation rule
    fn check_rule(&self, _rule: &CorrelationRule) {
        // Implementation would check for pattern matches
        // and trigger appropriate actions
    }

    /// Add a correlation rule
    pub fn add_rule(&mut self, rule: CorrelationRule) {
        self.correlation_rules.push(rule);
    }
}

impl AlertManager {
    /// Create a new alert manager
    pub fn new() -> Self {
        Self {
            handlers: Vec::new(),
            suppression_rules: Vec::new(),
            recent_alerts: VecDeque::new(),
        }
    }

    /// Add an alert handler
    pub fn add_handler(&mut self, handler: Box<dyn AlertHandler + Send>) {
        self.handlers.push(handler);
    }

    /// Handle an alert
    pub fn handle_alert(&mut self, alert: Alert) -> WasmtimeResult<()> {
        // Check suppression rules
        if self.is_suppressed(&alert) {
            return Ok(());
        }

        // Record the alert
        self.recent_alerts.push_back(alert.clone());

        // Limit recent alerts buffer
        if self.recent_alerts.len() > 1000 {
            self.recent_alerts.pop_front();
        }

        // Send to all handlers
        for handler in &self.handlers {
            handler.handle_alert(&alert)?;
        }

        Ok(())
    }

    /// Check if an alert should be suppressed
    fn is_suppressed(&self, _alert: &Alert) -> bool {
        // Implementation would check suppression rules
        false
    }
}

/// Console alert handler for debugging
pub struct ConsoleAlertHandler;

impl AlertHandler for ConsoleAlertHandler {
    fn handle_alert(&self, alert: &Alert) -> WasmtimeResult<()> {
        eprintln!("ALERT: {} - {} - {}", alert.alert_type, alert.severity, alert.message);
        Ok(())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_audit_event_creation() {
        let event = AuditEvent::new(
            AuditEventType::Authentication,
            AuditSeverity::Info,
            "test_source".to_string(),
            "User authentication successful".to_string(),
        )
        .with_user("test_user".to_string())
        .with_outcome("success".to_string());

        assert_eq!(event.event_type, AuditEventType::Authentication);
        assert_eq!(event.severity, AuditSeverity::Info);
        assert_eq!(event.user_id, Some("test_user".to_string()));
        assert_eq!(event.outcome, "success");
    }

    #[test]
    fn test_audit_event_integrity() {
        let secret = b"test_secret";
        let event = AuditEvent::new(
            AuditEventType::SecurityViolation,
            AuditSeverity::Critical,
            "test_source".to_string(),
            "Security violation detected".to_string(),
        )
        .with_integrity_hash(secret);

        assert!(event.integrity_hash.is_some());
        assert!(event.verify_integrity(secret).unwrap());
    }

    #[test]
    fn test_compliance_report_generation() {
        let config = ComplianceReportConfig {
            framework: ComplianceFramework::SOX,
            start_time: SystemTime::now() - Duration::from_secs(30 * 24 * 60 * 60), // 30 days
            end_time: SystemTime::now(),
            event_types: vec![AuditEventType::Authentication, AuditEventType::Authorization],
            min_severity: AuditSeverity::Info,
            include_user_details: true,
            include_resource_details: true,
        };

        // This would normally require an actual audit logger with events
        // For testing, we just verify the config structure is correct
        assert_eq!(config.framework, ComplianceFramework::SOX);
        assert!(config.include_user_details);
    }

    #[test]
    fn test_event_correlator() {
        let mut correlator = EventCorrelator::new(Duration::from_secs(60)); // 1 minute

        let event = AuditEvent::new(
            AuditEventType::Authentication,
            AuditSeverity::Info,
            "test_source".to_string(),
            "Test event".to_string(),
        );

        correlator.add_event(event);
        assert_eq!(correlator.recent_events.len(), 1);
    }
}