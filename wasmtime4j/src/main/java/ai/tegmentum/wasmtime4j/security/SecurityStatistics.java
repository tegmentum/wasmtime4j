package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;

/**
 * Security statistics and metrics for monitoring and reporting.
 *
 * @since 1.0.0
 */
public interface SecurityStatistics {

  /**
   * Gets the total number of security events logged.
   *
   * @return total security events
   */
  long getTotalSecurityEvents();

  /**
   * Gets the number of security events by type.
   *
   * @return security events by type
   */
  Map<String, Long> getSecurityEventsByType();

  /**
   * Gets the number of active security contexts.
   *
   * @return active security contexts
   */
  int getActiveSecurityContexts();

  /**
   * Gets the number of active sandboxes.
   *
   * @return active sandboxes
   */
  int getActiveSandboxes();

  /**
   * Gets the number of active sessions.
   *
   * @return active sessions
   */
  int getActiveSessions();

  /**
   * Gets the total number of authorization requests.
   *
   * @return total authorization requests
   */
  long getTotalAuthorizationRequests();

  /**
   * Gets the number of denied authorization requests.
   *
   * @return denied authorization requests
   */
  long getDeniedAuthorizationRequests();

  /**
   * Gets the authorization success rate as a percentage.
   *
   * @return authorization success rate (0.0 to 1.0)
   */
  double getAuthorizationSuccessRate();

  /**
   * Gets the total number of signature verifications.
   *
   * @return total signature verifications
   */
  long getTotalSignatureVerifications();

  /**
   * Gets the number of failed signature verifications.
   *
   * @return failed signature verifications
   */
  long getFailedSignatureVerifications();

  /**
   * Gets the signature verification success rate as a percentage.
   *
   * @return signature verification success rate (0.0 to 1.0)
   */
  double getSignatureVerificationSuccessRate();

  /**
   * Gets the total number of capability checks.
   *
   * @return total capability checks
   */
  long getTotalCapabilityChecks();

  /**
   * Gets the number of denied capability checks.
   *
   * @return denied capability checks
   */
  long getDeniedCapabilityChecks();

  /**
   * Gets the total number of audit events.
   *
   * @return total audit events
   */
  long getTotalAuditEvents();

  /**
   * Gets the number of compliance violations detected.
   *
   * @return compliance violations
   */
  long getComplianceViolations();

  /**
   * Gets the number of security alerts generated.
   *
   * @return security alerts
   */
  long getSecurityAlerts();

  /**
   * Gets the timestamp when statistics were last updated.
   *
   * @return last update timestamp
   */
  Instant getLastUpdated();

  /**
   * Gets the security manager uptime.
   *
   * @return uptime duration
   */
  java.time.Duration getUptime();

  /**
   * Gets additional custom metrics.
   *
   * @return custom metrics map
   */
  Map<String, Object> getCustomMetrics();
}
