package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Security audit log interface for tracking and analyzing security events.
 *
 * <p>Provides comprehensive audit trail functionality including event logging, filtering, analysis,
 * and reporting capabilities for security monitoring.
 *
 * @since 1.0.0
 */
public interface SecurityAuditLog {

  /**
   * Creates a new security audit log with default configuration.
   *
   * @return new audit log instance
   */
  static SecurityAuditLog create() {
    return create(SecurityAuditConfig.defaultConfig());
  }

  /**
   * Creates a new security audit log with specified configuration.
   *
   * @param config the audit log configuration
   * @return new audit log instance
   */
  static SecurityAuditLog create(final SecurityAuditConfig config) {
    // Use runtime selection pattern to find appropriate implementation
    try {
      // Try Panama implementation first
      final Class<?> logClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.security.PanamaSecurityAuditLog");
      return (SecurityAuditLog)
          logClass.getDeclaredConstructor(SecurityAuditConfig.class).newInstance(config);
    } catch (final ClassNotFoundException e) {
      // Panama not available, try JNI implementation
      try {
        final Class<?> logClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.security.JniSecurityAuditLog");
        return (SecurityAuditLog)
            logClass.getDeclaredConstructor(SecurityAuditConfig.class).newInstance(config);
      } catch (final ClassNotFoundException e2) {
        throw new UnsupportedOperationException(
            "No SecurityAuditLog implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create security audit log", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create security audit log", e);
    }
  }

  /**
   * Logs a security event.
   *
   * @param event the security event to log
   */
  void logEvent(final SecurityEvent event);

  /**
   * Logs a security event with additional context.
   *
   * @param eventType the type of security event
   * @param message the event message
   * @param context additional context information
   */
  void logEvent(
      final SecurityEventType eventType, final String message, final Map<String, Object> context);

  /**
   * Logs a security event with severity and context.
   *
   * @param eventType the type of security event
   * @param severity the event severity
   * @param message the event message
   * @param context additional context information
   */
  void logEvent(
      final SecurityEventType eventType,
      final SecuritySeverity severity,
      final String message,
      final Map<String, Object> context);

  /**
   * Gets all security events since the specified timestamp.
   *
   * @param since the timestamp to start from
   * @return list of security events
   */
  List<SecurityEvent> getEventsSince(final Instant since);

  /**
   * Gets security events of specific types since the specified timestamp.
   *
   * @param since the timestamp to start from
   * @param eventTypes the event types to filter by
   * @return list of filtered security events
   */
  List<SecurityEvent> getEventsSince(final Instant since, final SecurityEventType... eventTypes);

  /**
   * Gets security events with minimum severity since the specified timestamp.
   *
   * @param since the timestamp to start from
   * @param minSeverity the minimum severity level
   * @return list of filtered security events
   */
  List<SecurityEvent> getEventsSince(final Instant since, final SecuritySeverity minSeverity);

  /**
   * Gets the most recent security events.
   *
   * @param count the maximum number of events to return
   * @return list of recent security events
   */
  List<SecurityEvent> getRecentEvents(final int count);

  /**
   * Gets security events matching the specified criteria.
   *
   * @param criteria the search criteria
   * @return list of matching security events
   */
  List<SecurityEvent> searchEvents(final SecurityEventCriteria criteria);

  /**
   * Gets security violation events since the specified timestamp.
   *
   * @param since the timestamp to start from
   * @return list of security violation events
   */
  List<SecurityEvent> getViolationsSince(final Instant since);

  /**
   * Gets critical security events since the specified timestamp.
   *
   * @param since the timestamp to start from
   * @return list of critical security events
   */
  List<SecurityEvent> getCriticalEventsSince(final Instant since);

  /**
   * Checks if there are any unacknowledged critical security events.
   *
   * @return true if unacknowledged critical events exist
   */
  boolean hasUnacknowledgedCriticalEvents();

  /**
   * Acknowledges security events by marking them as reviewed.
   *
   * @param eventIds the IDs of events to acknowledge
   */
  void acknowledgeEvents(final List<String> eventIds);

  /**
   * Gets audit statistics for the specified time period.
   *
   * @param since the start of the time period
   * @param until the end of the time period
   * @return audit statistics
   */
  SecurityAuditStatistics getStatistics(final Instant since, final Instant until);

  /**
   * Gets event count by type for the specified time period.
   *
   * @param since the start of the time period
   * @param until the end of the time period
   * @return map of event type to count
   */
  Map<SecurityEventType, Long> getEventCountsByType(final Instant since, final Instant until);

  /**
   * Gets event count by severity for the specified time period.
   *
   * @param since the start of the time period
   * @param until the end of the time period
   * @return map of severity to count
   */
  Map<SecuritySeverity, Long> getEventCountsBySeverity(final Instant since, final Instant until);

  /**
   * Analyzes events for security patterns and anomalies.
   *
   * @param since the start of the analysis period
   * @param until the end of the analysis period
   * @return security analysis report
   */
  SecurityAnalysisReport analyzeEvents(final Instant since, final Instant until);

  /**
   * Detects potential security threats based on event patterns.
   *
   * @param lookbackPeriod the time period to analyze
   * @return list of detected threats
   */
  List<SecurityThreat> detectThreats(final java.time.Duration lookbackPeriod);

  /**
   * Generates a security compliance report.
   *
   * @param reportType the type of compliance report
   * @param since the start of the reporting period
   * @param until the end of the reporting period
   * @return compliance report
   */
  SecurityComplianceReport generateComplianceReport(
      final ComplianceReportType reportType, final Instant since, final Instant until);

  /**
   * Exports audit log events in the specified format.
   *
   * @param format the export format
   * @param since the start of the export period
   * @param until the end of the export period
   * @return exported data as byte array
   */
  byte[] exportEvents(final ExportFormat format, final Instant since, final Instant until);

  /**
   * Archives old audit log events to reduce storage usage.
   *
   * @param olderThan events older than this timestamp will be archived
   * @return number of events archived
   */
  long archiveEvents(final Instant olderThan);

  /**
   * Purges old audit log events permanently.
   *
   * <p><strong>WARNING:</strong> This operation is irreversible.
   *
   * @param olderThan events older than this timestamp will be purged
   * @return number of events purged
   */
  long purgeEvents(final Instant olderThan);

  /**
   * Gets the current audit log configuration.
   *
   * @return audit log configuration
   */
  SecurityAuditConfig getConfig();

  /**
   * Updates the audit log configuration.
   *
   * @param config the new configuration
   */
  void updateConfig(final SecurityAuditConfig config);

  /**
   * Gets the total number of events in the audit log.
   *
   * @return total event count
   */
  long getTotalEventCount();

  /**
   * Gets the size of the audit log in bytes.
   *
   * @return audit log size in bytes
   */
  long getLogSizeBytes();

  /**
   * Gets the audit log retention policy.
   *
   * @return retention policy
   */
  AuditRetentionPolicy getRetentionPolicy();

  /**
   * Validates the integrity of the audit log.
   *
   * @return validation result
   */
  AuditLogValidationResult validateIntegrity();

  /** Closes the audit log and releases resources. */
  void close();

  /** Export formats for audit log data. */
  enum ExportFormat {
    JSON,
    XML,
    CSV,
    BINARY
  }

  /** Types of compliance reports. */
  enum ComplianceReportType {
    SOX,
    HIPAA,
    GDPR,
    PCI_DSS,
    SOC2,
    ISO27001,
    CUSTOM
  }
}
