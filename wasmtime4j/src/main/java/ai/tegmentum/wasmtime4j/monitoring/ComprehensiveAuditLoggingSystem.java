/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.monitoring;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Comprehensive audit logging and compliance reporting system providing complete traceability,
 * regulatory compliance, security auditing, and forensic analysis capabilities.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Complete audit trail of all system operations and changes
 *   <li>Compliance reporting for various regulations (SOX, GDPR, HIPAA, etc.)
 *   <li>Security event logging and forensic analysis
 *   <li>Data integrity verification and tamper detection
 *   <li>Automated compliance validation and reporting
 *   <li>Long-term audit log retention and archival
 *   <li>Real-time audit monitoring and alerting
 * </ul>
 *
 * @since 1.0.0
 */
public final class ComprehensiveAuditLoggingSystem {

  private static final Logger LOGGER = Logger.getLogger(ComprehensiveAuditLoggingSystem.class.getName());

  /** Audit event types for categorization and filtering. */
  public enum AuditEventType {
    SYSTEM_STARTUP("System Startup", "System initialization and startup events"),
    SYSTEM_SHUTDOWN("System Shutdown", "System shutdown and cleanup events"),
    CONFIGURATION_CHANGE("Configuration Change", "System configuration modifications"),
    SECURITY_EVENT("Security Event", "Security-related events and violations"),
    ACCESS_CONTROL("Access Control", "Authentication and authorization events"),
    DATA_ACCESS("Data Access", "Data read and write operations"),
    ADMINISTRATIVE_ACTION("Administrative Action", "Administrative operations"),
    ERROR_EVENT("Error Event", "System errors and exceptions"),
    PERFORMANCE_EVENT("Performance Event", "Performance-related events"),
    COMPLIANCE_EVENT("Compliance Event", "Compliance and regulatory events"),
    MONITORING_EVENT("Monitoring Event", "Monitoring system operations"),
    BACKUP_RESTORE("Backup/Restore", "Backup and restore operations"),
    NETWORK_EVENT("Network Event", "Network-related events"),
    RESOURCE_USAGE("Resource Usage", "Resource allocation and usage events");

    private final String displayName;
    private final String description;

    AuditEventType(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Compliance framework types. */
  public enum ComplianceFramework {
    SOX("Sarbanes-Oxley Act", "Financial reporting compliance"),
    GDPR("General Data Protection Regulation", "EU data protection regulation"),
    HIPAA("Health Insurance Portability and Accountability Act", "Healthcare data protection"),
    PCI_DSS("Payment Card Industry Data Security Standard", "Credit card data protection"),
    ISO_27001("ISO/IEC 27001", "Information security management"),
    NIST("NIST Cybersecurity Framework", "US cybersecurity framework"),
    COBIT("Control Objectives for Information and Related Technologies", "IT governance framework"),
    CUSTOM("Custom Framework", "Organization-specific compliance framework");

    private final String displayName;
    private final String description;

    ComplianceFramework(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Audit event severity levels. */
  public enum AuditSeverity {
    INFO(1, "Informational"),
    LOW(2, "Low impact"),
    MEDIUM(3, "Medium impact"),
    HIGH(4, "High impact"),
    CRITICAL(5, "Critical impact");

    private final int level;
    private final String description;

    AuditSeverity(final int level, final String description) {
      this.level = level;
      this.description = description;
    }

    public int getLevel() { return level; }
    public String getDescription() { return description; }

    public boolean isHigherThan(final AuditSeverity other) {
      return this.level > other.level;
    }
  }

  /** Comprehensive audit log entry. */
  public static final class AuditLogEntry {
    private final String entryId;
    private final AuditEventType eventType;
    private final AuditSeverity severity;
    private final String event;
    private final String description;
    private final String userId;
    private final String sessionId;
    private final String sourceIp;
    private final String userAgent;
    private final String component;
    private final String resource;
    private final String action;
    private final Map<String, Object> eventData;
    private final Instant timestamp;
    private final String checksum;
    private final List<ComplianceFramework> applicableFrameworks;

    public AuditLogEntry(
        final String entryId,
        final AuditEventType eventType,
        final AuditSeverity severity,
        final String event,
        final String description,
        final String userId,
        final String sessionId,
        final String sourceIp,
        final String userAgent,
        final String component,
        final String resource,
        final String action,
        final Map<String, Object> eventData,
        final List<ComplianceFramework> applicableFrameworks) {
      this.entryId = entryId;
      this.eventType = eventType;
      this.severity = severity;
      this.event = event;
      this.description = description;
      this.userId = userId;
      this.sessionId = sessionId;
      this.sourceIp = sourceIp;
      this.userAgent = userAgent;
      this.component = component;
      this.resource = resource;
      this.action = action;
      this.eventData = Map.copyOf(eventData != null ? eventData : Map.of());
      this.timestamp = Instant.now();
      this.applicableFrameworks = List.copyOf(applicableFrameworks != null ? applicableFrameworks : List.of());
      this.checksum = calculateChecksum();
    }

    /** Calculates checksum for integrity verification. */
    private String calculateChecksum() {
      try {
        final MessageDigest digest = MessageDigest.getInstance("SHA-256");
        final String data = String.format("%s|%s|%s|%s|%s|%s|%s|%s",
            entryId, eventType, event, userId, component, action, timestamp, eventData);
        final byte[] hash = digest.digest(data.getBytes());

        final StringBuilder hexString = new StringBuilder();
        for (final byte b : hash) {
          final String hex = Integer.toHexString(0xff & b);
          if (hex.length() == 1) {
            hexString.append('0');
          }
          hexString.append(hex);
        }
        return hexString.toString();

      } catch (final NoSuchAlgorithmException e) {
        return "checksum_failed";
      }
    }

    // Getters
    public String getEntryId() { return entryId; }
    public AuditEventType getEventType() { return eventType; }
    public AuditSeverity getSeverity() { return severity; }
    public String getEvent() { return event; }
    public String getDescription() { return description; }
    public String getUserId() { return userId; }
    public String getSessionId() { return sessionId; }
    public String getSourceIp() { return sourceIp; }
    public String getUserAgent() { return userAgent; }
    public String getComponent() { return component; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public Map<String, Object> getEventData() { return eventData; }
    public Instant getTimestamp() { return timestamp; }
    public String getChecksum() { return checksum; }
    public List<ComplianceFramework> getApplicableFrameworks() { return applicableFrameworks; }

    /** Validates the integrity of this audit entry. */
    public boolean validateIntegrity() {
      return checksum.equals(calculateChecksum());
    }

    /** Formats the audit entry for logging. */
    public String formatForLogging() {
      return String.format("[%s] %s|%s|%s|%s|%s|%s|%s|%s|%s",
          timestamp.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
          entryId,
          eventType,
          severity,
          event,
          userId != null ? userId : "SYSTEM",
          component,
          action,
          description,
          checksum);
    }

    /** Formats the audit entry as JSON. */
    public String formatAsJson() {
      final StringBuilder json = new StringBuilder();
      json.append("{\n");
      json.append("  \"entryId\": \"").append(entryId).append("\",\n");
      json.append("  \"eventType\": \"").append(eventType).append("\",\n");
      json.append("  \"severity\": \"").append(severity).append("\",\n");
      json.append("  \"event\": \"").append(event).append("\",\n");
      json.append("  \"description\": \"").append(description != null ? description : "").append("\",\n");
      json.append("  \"userId\": \"").append(userId != null ? userId : "").append("\",\n");
      json.append("  \"sessionId\": \"").append(sessionId != null ? sessionId : "").append("\",\n");
      json.append("  \"sourceIp\": \"").append(sourceIp != null ? sourceIp : "").append("\",\n");
      json.append("  \"component\": \"").append(component != null ? component : "").append("\",\n");
      json.append("  \"resource\": \"").append(resource != null ? resource : "").append("\",\n");
      json.append("  \"action\": \"").append(action != null ? action : "").append("\",\n");
      json.append("  \"timestamp\": \"").append(timestamp.toString()).append("\",\n");
      json.append("  \"checksum\": \"").append(checksum).append("\",\n");
      json.append("  \"applicableFrameworks\": ").append(applicableFrameworks.toString()).append(",\n");
      json.append("  \"eventData\": ").append(eventData.toString()).append("\n");
      json.append("}");
      return json.toString();
    }
  }

  /** Compliance report generation. */
  public static final class ComplianceReport {
    private final String reportId;
    private final ComplianceFramework framework;
    private final Instant generatedAt;
    private final Instant periodStart;
    private final Instant periodEnd;
    private final List<String> complianceViolations;
    private final List<String> recommendations;
    private final Map<AuditEventType, Long> eventCounts;
    private final Map<String, Object> metrics;
    private final boolean compliant;

    public ComplianceReport(
        final String reportId,
        final ComplianceFramework framework,
        final Instant periodStart,
        final Instant periodEnd,
        final List<String> complianceViolations,
        final List<String> recommendations,
        final Map<AuditEventType, Long> eventCounts,
        final Map<String, Object> metrics) {
      this.reportId = reportId;
      this.framework = framework;
      this.generatedAt = Instant.now();
      this.periodStart = periodStart;
      this.periodEnd = periodEnd;
      this.complianceViolations = List.copyOf(complianceViolations != null ? complianceViolations : List.of());
      this.recommendations = List.copyOf(recommendations != null ? recommendations : List.of());
      this.eventCounts = Map.copyOf(eventCounts != null ? eventCounts : Map.of());
      this.metrics = Map.copyOf(metrics != null ? metrics : Map.of());
      this.compliant = this.complianceViolations.isEmpty();
    }

    // Getters
    public String getReportId() { return reportId; }
    public ComplianceFramework getFramework() { return framework; }
    public Instant getGeneratedAt() { return generatedAt; }
    public Instant getPeriodStart() { return periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public List<String> getComplianceViolations() { return complianceViolations; }
    public List<String> getRecommendations() { return recommendations; }
    public Map<AuditEventType, Long> getEventCounts() { return eventCounts; }
    public Map<String, Object> getMetrics() { return metrics; }
    public boolean isCompliant() { return compliant; }

    /** Formats the compliance report. */
    public String formatReport() {
      final StringBuilder report = new StringBuilder();
      report.append("=== Compliance Report ===\n");
      report.append("Report ID: ").append(reportId).append("\n");
      report.append("Framework: ").append(framework.getDisplayName()).append("\n");
      report.append("Generated: ").append(generatedAt.atZone(ZoneId.systemDefault())
          .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
      report.append("Period: ").append(periodStart.atZone(ZoneId.systemDefault())
          .format(DateTimeFormatter.ISO_LOCAL_DATE))
          .append(" to ").append(periodEnd.atZone(ZoneId.systemDefault())
          .format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
      report.append("Status: ").append(compliant ? "COMPLIANT" : "NON-COMPLIANT").append("\n");
      report.append("\n");

      if (!complianceViolations.isEmpty()) {
        report.append("Compliance Violations:\n");
        for (int i = 0; i < complianceViolations.size(); i++) {
          report.append("  ").append(i + 1).append(". ").append(complianceViolations.get(i)).append("\n");
        }
        report.append("\n");
      }

      if (!recommendations.isEmpty()) {
        report.append("Recommendations:\n");
        for (int i = 0; i < recommendations.size(); i++) {
          report.append("  ").append(i + 1).append(". ").append(recommendations.get(i)).append("\n");
        }
        report.append("\n");
      }

      report.append("Event Summary:\n");
      for (final Map.Entry<AuditEventType, Long> entry : eventCounts.entrySet()) {
        report.append("  ").append(entry.getKey().getDisplayName()).append(": ")
            .append(entry.getValue()).append("\n");
      }
      report.append("\n");

      if (!metrics.isEmpty()) {
        report.append("Metrics:\n");
        for (final Map.Entry<String, Object> entry : metrics.entrySet()) {
          report.append("  ").append(entry.getKey()).append(": ")
              .append(entry.getValue()).append("\n");
        }
      }

      return report.toString();
    }
  }

  /** Audit query filter for searching and filtering audit logs. */
  public static final class AuditQueryFilter {
    private final Instant startTime;
    private final Instant endTime;
    private final List<AuditEventType> eventTypes;
    private final List<AuditSeverity> severities;
    private final String userId;
    private final String component;
    private final String resource;
    private final String action;
    private final List<ComplianceFramework> frameworks;
    private final int limit;

    public AuditQueryFilter(
        final Instant startTime,
        final Instant endTime,
        final List<AuditEventType> eventTypes,
        final List<AuditSeverity> severities,
        final String userId,
        final String component,
        final String resource,
        final String action,
        final List<ComplianceFramework> frameworks,
        final int limit) {
      this.startTime = startTime;
      this.endTime = endTime;
      this.eventTypes = List.copyOf(eventTypes != null ? eventTypes : List.of());
      this.severities = List.copyOf(severities != null ? severities : List.of());
      this.userId = userId;
      this.component = component;
      this.resource = resource;
      this.action = action;
      this.frameworks = List.copyOf(frameworks != null ? frameworks : List.of());
      this.limit = limit > 0 ? limit : 1000;
    }

    // Getters
    public Instant getStartTime() { return startTime; }
    public Instant getEndTime() { return endTime; }
    public List<AuditEventType> getEventTypes() { return eventTypes; }
    public List<AuditSeverity> getSeverities() { return severities; }
    public String getUserId() { return userId; }
    public String getComponent() { return component; }
    public String getResource() { return resource; }
    public String getAction() { return action; }
    public List<ComplianceFramework> getFrameworks() { return frameworks; }
    public int getLimit() { return limit; }

    /** Checks if an audit entry matches this filter. */
    public boolean matches(final AuditLogEntry entry) {
      if (startTime != null && entry.getTimestamp().isBefore(startTime)) {
        return false;
      }
      if (endTime != null && entry.getTimestamp().isAfter(endTime)) {
        return false;
      }
      if (!eventTypes.isEmpty() && !eventTypes.contains(entry.getEventType())) {
        return false;
      }
      if (!severities.isEmpty() && !severities.contains(entry.getSeverity())) {
        return false;
      }
      if (userId != null && !userId.equals(entry.getUserId())) {
        return false;
      }
      if (component != null && !component.equals(entry.getComponent())) {
        return false;
      }
      if (resource != null && !resource.equals(entry.getResource())) {
        return false;
      }
      if (action != null && !action.equals(entry.getAction())) {
        return false;
      }
      if (!frameworks.isEmpty()) {
        final boolean hasFramework = entry.getApplicableFrameworks().stream()
            .anyMatch(frameworks::contains);
        if (!hasFramework) {
          return false;
        }
      }
      return true;
    }
  }

  // Instance fields
  private final ConcurrentHashMap<String, AuditLogEntry> auditEntries = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<AuditLogEntry> auditLog = new CopyOnWriteArrayList<>();
  private final ConcurrentHashMap<String, ComplianceReport> complianceReports = new ConcurrentHashMap<>();

  // Configuration
  private final Path auditLogDirectory;
  private final boolean persistToDisk;
  private final Duration logRetentionPeriod;
  private final int maxInMemoryEntries;

  // Statistics and monitoring
  private final AtomicLong totalAuditEntries = new AtomicLong(0);
  private final AtomicLong securityEventCount = new AtomicLong(0);
  private final AtomicLong complianceViolationCount = new AtomicLong(0);
  private final AtomicReference<Instant> lastIntegrityCheck = new AtomicReference<>(Instant.now());

  // Background processing
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(2);

  // Audit listeners
  private final List<AuditEventListener> auditListeners = new CopyOnWriteArrayList<>();

  /** Audit event listener interface. */
  @FunctionalInterface
  public interface AuditEventListener {
    void onAuditEvent(AuditLogEntry entry);
  }

  /**
   * Creates comprehensive audit logging system.
   *
   * @param auditLogDirectory directory for persistent audit logs
   * @param persistToDisk whether to persist audit logs to disk
   * @param logRetentionPeriod how long to retain audit logs
   * @param maxInMemoryEntries maximum audit entries to keep in memory
   */
  public ComprehensiveAuditLoggingSystem(
      final Path auditLogDirectory,
      final boolean persistToDisk,
      final Duration logRetentionPeriod,
      final int maxInMemoryEntries) {
    this.auditLogDirectory = auditLogDirectory != null ? auditLogDirectory : Paths.get("audit-logs");
    this.persistToDisk = persistToDisk;
    this.logRetentionPeriod = logRetentionPeriod != null ? logRetentionPeriod : Duration.ofDays(365);
    this.maxInMemoryEntries = maxInMemoryEntries > 0 ? maxInMemoryEntries : 10000;

    initializeAuditSystem();
    startBackgroundProcessing();
    logSystemStartup();
    LOGGER.info("Comprehensive audit logging system initialized");
  }

  /** Default constructor with standard settings. */
  public ComprehensiveAuditLoggingSystem() {
    this(null, true, null, 0);
  }

  /** Initializes the audit system. */
  private void initializeAuditSystem() {
    if (persistToDisk) {
      try {
        Files.createDirectories(auditLogDirectory);
        LOGGER.info("Audit log directory initialized: " + auditLogDirectory);
      } catch (final IOException e) {
        LOGGER.severe("Failed to create audit log directory: " + e.getMessage());
      }
    }
  }

  /** Starts background processing tasks. */
  private void startBackgroundProcessing() {
    // Audit log maintenance
    backgroundExecutor.scheduleAtFixedRate(
        this::performAuditMaintenance,
        3600, 3600, TimeUnit.SECONDS); // Every hour

    // Integrity checks
    backgroundExecutor.scheduleAtFixedRate(
        this::performIntegrityCheck,
        21600, 21600, TimeUnit.SECONDS); // Every 6 hours
  }

  /** Logs system startup event. */
  private void logSystemStartup() {
    logAuditEvent(
        AuditEventType.SYSTEM_STARTUP,
        AuditSeverity.INFO,
        "System Startup",
        "Wasmtime4j monitoring and audit system started",
        null,
        null,
        null,
        null,
        "audit-system",
        "system",
        "startup",
        Map.of(
            "version", "1.0.0",
            "javaVersion", System.getProperty("java.version"),
            "os", System.getProperty("os.name")),
        List.of(ComplianceFramework.ISO_27001, ComplianceFramework.SOX));
  }

  /**
   * Logs an audit event.
   *
   * @param eventType the type of audit event
   * @param severity the severity level
   * @param event the event name
   * @param description detailed description of the event
   * @param userId the user identifier
   * @param sessionId the session identifier
   * @param sourceIp the source IP address
   * @param userAgent the user agent string
   * @param component the system component
   * @param resource the resource being accessed
   * @param action the action being performed
   * @param eventData additional event data
   * @param applicableFrameworks compliance frameworks this event relates to
   * @return the created audit log entry
   */
  public AuditLogEntry logAuditEvent(
      final AuditEventType eventType,
      final AuditSeverity severity,
      final String event,
      final String description,
      final String userId,
      final String sessionId,
      final String sourceIp,
      final String userAgent,
      final String component,
      final String resource,
      final String action,
      final Map<String, Object> eventData,
      final List<ComplianceFramework> applicableFrameworks) {

    final String entryId = generateEntryId();
    final AuditLogEntry entry = new AuditLogEntry(
        entryId,
        eventType,
        severity,
        event,
        description,
        userId,
        sessionId,
        sourceIp,
        userAgent,
        component,
        resource,
        action,
        eventData,
        applicableFrameworks);

    // Store in memory
    auditEntries.put(entryId, entry);
    auditLog.add(entry);
    totalAuditEntries.incrementAndGet();

    // Update statistics
    if (eventType == AuditEventType.SECURITY_EVENT) {
      securityEventCount.incrementAndGet();
    }

    // Maintain memory limits
    if (auditLog.size() > maxInMemoryEntries) {
      final AuditLogEntry oldest = auditLog.remove(0);
      auditEntries.remove(oldest.getEntryId());
    }

    // Persist to disk if enabled
    if (persistToDisk) {
      persistAuditEntry(entry);
    }

    // Notify listeners
    notifyAuditListeners(entry);

    // Check for compliance violations
    checkComplianceViolations(entry);

    LOGGER.fine(String.format("Audit event logged: %s [%s] - %s",
        entryId, eventType, event));

    return entry;
  }

  /** Generates a unique entry ID. */
  private String generateEntryId() {
    return "audit_" + System.currentTimeMillis() + "_" +
           Integer.toHexString((int) (Math.random() * 0x10000));
  }

  /** Persists audit entry to disk. */
  private void persistAuditEntry(final AuditLogEntry entry) {
    if (!persistToDisk) {
      return;
    }

    try {
      final String fileName = String.format("audit-%s.log",
          entry.getTimestamp().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
      final Path logFile = auditLogDirectory.resolve(fileName);

      final String logLine = entry.formatForLogging() + "\n";
      Files.write(logFile, logLine.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

    } catch (final IOException e) {
      LOGGER.warning("Failed to persist audit entry to disk: " + e.getMessage());
    }
  }

  /** Notifies audit event listeners. */
  private void notifyAuditListeners(final AuditLogEntry entry) {
    for (final AuditEventListener listener : auditListeners) {
      try {
        listener.onAuditEvent(entry);
      } catch (final Exception e) {
        LOGGER.warning("Audit event listener error: " + e.getMessage());
      }
    }
  }

  /** Checks for compliance violations in audit events. */
  private void checkComplianceViolations(final AuditLogEntry entry) {
    // Simple compliance checks - in production would be more sophisticated
    if (entry.getEventType() == AuditEventType.SECURITY_EVENT &&
        entry.getSeverity().isHigherThan(AuditSeverity.MEDIUM)) {
      complianceViolationCount.incrementAndGet();

      // Log compliance violation
      logAuditEvent(
          AuditEventType.COMPLIANCE_EVENT,
          AuditSeverity.HIGH,
          "Compliance Violation Detected",
          "Security event triggered compliance violation: " + entry.getEvent(),
          "SYSTEM",
          null,
          null,
          null,
          "compliance-monitor",
          "compliance",
          "violation_detected",
          Map.of("triggering_event", entry.getEntryId()),
          entry.getApplicableFrameworks());
    }

    // Check for failed access attempts
    if (entry.getAction() != null && entry.getAction().contains("login") &&
        entry.getDescription() != null && entry.getDescription().toLowerCase().contains("failed")) {

      // Log potential security issue
      logAuditEvent(
          AuditEventType.SECURITY_EVENT,
          AuditSeverity.MEDIUM,
          "Failed Login Attempt",
          "Failed login detected for user: " + entry.getUserId(),
          entry.getUserId(),
          entry.getSessionId(),
          entry.getSourceIp(),
          entry.getUserAgent(),
          "authentication",
          "login",
          "failed_attempt",
          Map.of("original_event", entry.getEntryId()),
          List.of(ComplianceFramework.ISO_27001, ComplianceFramework.NIST));
    }
  }

  /**
   * Queries audit logs based on filter criteria.
   *
   * @param filter the query filter
   * @return list of matching audit entries
   */
  public List<AuditLogEntry> queryAuditLogs(final AuditQueryFilter filter) {
    return auditLog.stream()
        .filter(filter::matches)
        .sorted(Comparator.comparing(AuditLogEntry::getTimestamp, Comparator.reverseOrder()))
        .limit(filter.getLimit())
        .collect(Collectors.toList());
  }

  /**
   * Generates a compliance report for the specified framework and period.
   *
   * @param framework the compliance framework
   * @param periodStart the start of the reporting period
   * @param periodEnd the end of the reporting period
   * @return generated compliance report
   */
  public ComplianceReport generateComplianceReport(
      final ComplianceFramework framework,
      final Instant periodStart,
      final Instant periodEnd) {

    final String reportId = "report_" + framework.name().toLowerCase() + "_" + System.currentTimeMillis();

    // Filter audit logs for the specified period and framework
    final AuditQueryFilter filter = new AuditQueryFilter(
        periodStart,
        periodEnd,
        null,
        null,
        null,
        null,
        null,
        null,
        List.of(framework),
        Integer.MAX_VALUE);

    final List<AuditLogEntry> relevantEntries = queryAuditLogs(filter);

    // Analyze for compliance violations
    final List<String> violations = new ArrayList<>();
    final List<String> recommendations = new ArrayList<>();

    // Count events by type
    final Map<AuditEventType, Long> eventCounts = relevantEntries.stream()
        .collect(Collectors.groupingBy(
            AuditLogEntry::getEventType,
            Collectors.counting()));

    // Framework-specific compliance checks
    switch (framework) {
      case SOX:
        violations.addAll(checkSoxCompliance(relevantEntries));
        recommendations.addAll(generateSoxRecommendations(violations));
        break;
      case GDPR:
        violations.addAll(checkGdprCompliance(relevantEntries));
        recommendations.addAll(generateGdprRecommendations(violations));
        break;
      case ISO_27001:
        violations.addAll(checkIsoCompliance(relevantEntries));
        recommendations.addAll(generateIsoRecommendations(violations));
        break;
      default:
        violations.addAll(checkGeneralCompliance(relevantEntries));
        recommendations.addAll(generateGeneralRecommendations(violations));
        break;
    }

    // Calculate metrics
    final Map<String, Object> metrics = new ConcurrentHashMap<>();
    metrics.put("totalEvents", relevantEntries.size());
    metrics.put("securityEvents", eventCounts.getOrDefault(AuditEventType.SECURITY_EVENT, 0L));
    metrics.put("complianceEvents", eventCounts.getOrDefault(AuditEventType.COMPLIANCE_EVENT, 0L));
    metrics.put("accessControlEvents", eventCounts.getOrDefault(AuditEventType.ACCESS_CONTROL, 0L));
    metrics.put("configurationChanges", eventCounts.getOrDefault(AuditEventType.CONFIGURATION_CHANGE, 0L));
    metrics.put("reportPeriodDays", Duration.between(periodStart, periodEnd).toDays());

    final ComplianceReport report = new ComplianceReport(
        reportId,
        framework,
        periodStart,
        periodEnd,
        violations,
        recommendations,
        eventCounts,
        metrics);

    complianceReports.put(reportId, report);

    // Log compliance report generation
    logAuditEvent(
        AuditEventType.COMPLIANCE_EVENT,
        AuditSeverity.INFO,
        "Compliance Report Generated",
        String.format("Generated %s compliance report for period %s to %s",
            framework.getDisplayName(),
            periodStart.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE),
            periodEnd.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE)),
        "SYSTEM",
        null,
        null,
        null,
        "compliance-generator",
        "compliance-report",
        "generate",
        Map.of("reportId", reportId, "framework", framework.name(), "compliant", report.isCompliant()),
        List.of(framework));

    return report;
  }

  /** Checks SOX compliance violations. */
  private List<String> checkSoxCompliance(final List<AuditLogEntry> entries) {
    final List<String> violations = new ArrayList<>();

    // Check for unauthorized configuration changes
    final long configChanges = entries.stream()
        .filter(e -> e.getEventType() == AuditEventType.CONFIGURATION_CHANGE)
        .filter(e -> e.getUserId() == null || e.getUserId().equals("SYSTEM"))
        .count();

    if (configChanges > 0) {
      violations.add("Unauthorized configuration changes detected: " + configChanges + " events");
    }

    // Check for administrative actions without proper authorization
    final long unauthorizedActions = entries.stream()
        .filter(e -> e.getEventType() == AuditEventType.ADMINISTRATIVE_ACTION)
        .filter(e -> e.getSeverity().isHigherThan(AuditSeverity.MEDIUM))
        .filter(e -> e.getUserId() == null)
        .count();

    if (unauthorizedActions > 0) {
      violations.add("Administrative actions without user identification: " + unauthorizedActions + " events");
    }

    return violations;
  }

  /** Generates SOX compliance recommendations. */
  private List<String> generateSoxRecommendations(final List<String> violations) {
    final List<String> recommendations = new ArrayList<>();

    if (violations.stream().anyMatch(v -> v.contains("configuration changes"))) {
      recommendations.add("Implement stronger access controls for configuration changes");
      recommendations.add("Require multi-factor authentication for administrative operations");
    }

    if (violations.stream().anyMatch(v -> v.contains("user identification"))) {
      recommendations.add("Ensure all administrative actions are properly attributed to users");
      recommendations.add("Implement session tracking for all administrative operations");
    }

    return recommendations;
  }

  /** Checks GDPR compliance violations. */
  private List<String> checkGdprCompliance(final List<AuditLogEntry> entries) {
    final List<String> violations = new ArrayList<>();

    // Check for data access without proper logging
    final long dataAccessEvents = entries.stream()
        .filter(e -> e.getEventType() == AuditEventType.DATA_ACCESS)
        .count();

    // In GDPR, all data access should be logged
    if (dataAccessEvents == 0) {
      violations.add("No data access events logged - potential GDPR compliance gap");
    }

    // Check for security incidents
    final long securityIncidents = entries.stream()
        .filter(e -> e.getEventType() == AuditEventType.SECURITY_EVENT)
        .filter(e -> e.getSeverity().isHigherThan(AuditSeverity.MEDIUM))
        .count();

    if (securityIncidents > 0) {
      violations.add("Security incidents detected: " + securityIncidents + " events require GDPR breach assessment");
    }

    return violations;
  }

  /** Generates GDPR compliance recommendations. */
  private List<String> generateGdprRecommendations(final List<String> violations) {
    final List<String> recommendations = new ArrayList<>();

    if (violations.stream().anyMatch(v -> v.contains("data access"))) {
      recommendations.add("Implement comprehensive data access logging");
      recommendations.add("Ensure all personal data processing activities are logged");
    }

    if (violations.stream().anyMatch(v -> v.contains("Security incidents"))) {
      recommendations.add("Establish security incident response procedures");
      recommendations.add("Implement automated breach notification systems");
    }

    return recommendations;
  }

  /** Checks ISO 27001 compliance violations. */
  private List<String> checkIsoCompliance(final List<AuditLogEntry> entries) {
    final List<String> violations = new ArrayList<>();

    // Check for security events without proper classification
    final long unclassifiedSecurity = entries.stream()
        .filter(e -> e.getEventType() == AuditEventType.SECURITY_EVENT)
        .filter(e -> e.getSeverity() == AuditSeverity.INFO)
        .count();

    if (unclassifiedSecurity > 10) {
      violations.add("High number of low-severity security events may indicate insufficient classification");
    }

    return violations;
  }

  /** Generates ISO 27001 compliance recommendations. */
  private List<String> generateIsoRecommendations(final List<String> violations) {
    final List<String> recommendations = new ArrayList<>();

    if (violations.stream().anyMatch(v -> v.contains("classification"))) {
      recommendations.add("Implement proper security event classification procedures");
      recommendations.add("Review and update security event severity criteria");
    }

    return recommendations;
  }

  /** Checks general compliance violations. */
  private List<String> checkGeneralCompliance(final List<AuditLogEntry> entries) {
    final List<String> violations = new ArrayList<>();

    // Check for high-severity errors
    final long criticalErrors = entries.stream()
        .filter(e -> e.getSeverity() == AuditSeverity.CRITICAL)
        .count();

    if (criticalErrors > 0) {
      violations.add("Critical severity events detected: " + criticalErrors + " events require investigation");
    }

    return violations;
  }

  /** Generates general compliance recommendations. */
  private List<String> generateGeneralRecommendations(final List<String> violations) {
    final List<String> recommendations = new ArrayList<>();

    if (violations.stream().anyMatch(v -> v.contains("Critical severity"))) {
      recommendations.add("Investigate and resolve all critical severity events");
      recommendations.add("Implement preventive measures to reduce critical events");
    }

    return recommendations;
  }

  /** Performs audit log maintenance. */
  private void performAuditMaintenance() {
    try {
      final int initialSize = auditLog.size();
      final Instant cutoff = Instant.now().minus(logRetentionPeriod);

      // Remove old entries from memory
      auditLog.removeIf(entry -> entry.getTimestamp().isBefore(cutoff));
      auditEntries.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));

      final int removedCount = initialSize - auditLog.size();
      if (removedCount > 0) {
        LOGGER.fine("Audit maintenance: removed " + removedCount + " old entries");
      }

      // Clean old disk files if configured
      if (persistToDisk) {
        cleanOldLogFiles(cutoff);
      }

    } catch (final Exception e) {
      LOGGER.warning("Error during audit maintenance: " + e.getMessage());
    }
  }

  /** Cleans old log files from disk. */
  private void cleanOldLogFiles(final Instant cutoff) {
    try {
      Files.list(auditLogDirectory)
          .filter(path -> path.getFileName().toString().startsWith("audit-"))
          .filter(path -> {
            try {
              return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
            } catch (final IOException e) {
              return false;
            }
          })
          .forEach(path -> {
            try {
              Files.delete(path);
              LOGGER.fine("Deleted old audit log file: " + path);
            } catch (final IOException e) {
              LOGGER.warning("Failed to delete old audit log file " + path + ": " + e.getMessage());
            }
          });
    } catch (final IOException e) {
      LOGGER.warning("Error cleaning old log files: " + e.getMessage());
    }
  }

  /** Performs integrity check on audit logs. */
  private void performIntegrityCheck() {
    try {
      int checkedEntries = 0;
      int integrityViolations = 0;

      for (final AuditLogEntry entry : auditLog) {
        if (!entry.validateIntegrity()) {
          integrityViolations++;

          // Log integrity violation
          logAuditEvent(
              AuditEventType.SECURITY_EVENT,
              AuditSeverity.CRITICAL,
              "Audit Log Integrity Violation",
              "Audit entry integrity check failed: " + entry.getEntryId(),
              "SYSTEM",
              null,
              null,
              null,
              "audit-system",
              "audit-log",
              "integrity_check",
              Map.of("violating_entry", entry.getEntryId()),
              List.of(ComplianceFramework.ISO_27001, ComplianceFramework.SOX));
        }
        checkedEntries++;
      }

      lastIntegrityCheck.set(Instant.now());

      LOGGER.fine(String.format("Integrity check completed: %d entries checked, %d violations",
          checkedEntries, integrityViolations));

      if (integrityViolations > 0) {
        LOGGER.severe("SECURITY ALERT: " + integrityViolations + " audit log integrity violations detected");
      }

    } catch (final Exception e) {
      LOGGER.warning("Error during integrity check: " + e.getMessage());
    }
  }

  /**
   * Adds an audit event listener.
   *
   * @param listener the audit event listener
   */
  public void addAuditEventListener(final AuditEventListener listener) {
    if (listener != null) {
      auditListeners.add(listener);
    }
  }

  /**
   * Removes an audit event listener.
   *
   * @param listener the audit event listener to remove
   */
  public void removeAuditEventListener(final AuditEventListener listener) {
    auditListeners.remove(listener);
  }

  /**
   * Gets audit system statistics.
   *
   * @return formatted statistics
   */
  public String getAuditStatistics() {
    return String.format(
        "Audit System Statistics: total_entries=%d, memory_entries=%d, security_events=%d, " +
        "compliance_violations=%d, compliance_reports=%d, last_integrity_check=%s",
        totalAuditEntries.get(),
        auditLog.size(),
        securityEventCount.get(),
        complianceViolationCount.get(),
        complianceReports.size(),
        lastIntegrityCheck.get().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME));
  }

  /**
   * Gets a compliance report by ID.
   *
   * @param reportId the report identifier
   * @return compliance report or null if not found
   */
  public ComplianceReport getComplianceReport(final String reportId) {
    return complianceReports.get(reportId);
  }

  /**
   * Lists all generated compliance reports.
   *
   * @return list of compliance reports
   */
  public List<ComplianceReport> listComplianceReports() {
    return complianceReports.values().stream()
        .sorted(Comparator.comparing(ComplianceReport::getGeneratedAt, Comparator.reverseOrder()))
        .collect(Collectors.toList());
  }

  /** Shuts down the audit logging system. */
  public void shutdown() {
    // Log system shutdown
    logAuditEvent(
        AuditEventType.SYSTEM_SHUTDOWN,
        AuditSeverity.INFO,
        "System Shutdown",
        "Wasmtime4j monitoring and audit system shutting down",
        null,
        null,
        null,
        null,
        "audit-system",
        "system",
        "shutdown",
        Map.of("totalAuditEntries", totalAuditEntries.get()),
        List.of(ComplianceFramework.ISO_27001, ComplianceFramework.SOX));

    // Shutdown background executor
    backgroundExecutor.shutdown();
    try {
      if (!backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        backgroundExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      backgroundExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Comprehensive audit logging system shutdown");
  }
}