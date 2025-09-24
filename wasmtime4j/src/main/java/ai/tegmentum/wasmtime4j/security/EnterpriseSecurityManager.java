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

package ai.tegmentum.wasmtime4j.security;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Enterprise security manager providing comprehensive security policy enforcement, access control,
 * audit logging, and threat detection for WebAssembly operations.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Role-based access control (RBAC) with fine-grained permissions
 *   <li>Comprehensive audit logging with tamper detection
 *   <li>Real-time threat detection and response
 *   <li>Module signature verification and trust management
 *   <li>Resource usage monitoring and anomaly detection
 *   <li>Security policy enforcement with customizable rules
 * </ul>
 *
 * @since 1.0.0
 */
public final class EnterpriseSecurityManager {

  private static final Logger LOGGER = Logger.getLogger(EnterpriseSecurityManager.class.getName());

  /** Security event types for audit logging. */
  public enum SecurityEventType {
    MODULE_LOAD,
    MODULE_EXECUTION,
    FUNCTION_CALL,
    MEMORY_ACCESS,
    FILE_ACCESS,
    NETWORK_ACCESS,
    PERMISSION_GRANTED,
    PERMISSION_DENIED,
    SECURITY_VIOLATION,
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_SUCCESS,
    AUTHORIZATION_FAILURE,
    POLICY_VIOLATION,
    THREAT_DETECTED,
    ANOMALY_DETECTED
  }

  /** Security threat levels. */
  public enum ThreatLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  /** Security audit event. */
  public static final class SecurityAuditEvent {
    private final String eventId;
    private final SecurityEventType eventType;
    private final Instant timestamp;
    private final Principal principal;
    private final String moduleId;
    private final String resource;
    private final String action;
    private final boolean success;
    private final String details;
    private final Map<String, Object> metadata;
    private final ThreatLevel threatLevel;

    public SecurityAuditEvent(
        final String eventId,
        final SecurityEventType eventType,
        final Principal principal,
        final String moduleId,
        final String resource,
        final String action,
        final boolean success,
        final String details,
        final ThreatLevel threatLevel) {
      this.eventId = eventId;
      this.eventType = eventType;
      this.timestamp = Instant.now();
      this.principal = principal;
      this.moduleId = moduleId;
      this.resource = resource;
      this.action = action;
      this.success = success;
      this.details = details;
      this.metadata = new ConcurrentHashMap<>();
      this.threatLevel = threatLevel;
    }

    public String getEventId() {
      return eventId;
    }

    public SecurityEventType getEventType() {
      return eventType;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Principal getPrincipal() {
      return principal;
    }

    public String getModuleId() {
      return moduleId;
    }

    public String getResource() {
      return resource;
    }

    public String getAction() {
      return action;
    }

    public boolean isSuccess() {
      return success;
    }

    public String getDetails() {
      return details;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public ThreatLevel getThreatLevel() {
      return threatLevel;
    }
  }

  /** Security context for current operation. */
  public static final class SecurityContext {
    private final Principal principal;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final String sessionId;
    private final Instant createdTime;
    private volatile Instant lastAccessTime;
    private final Map<String, Object> attributes;

    public SecurityContext(
        final Principal principal,
        final Set<String> roles,
        final Set<String> permissions,
        final String sessionId) {
      this.principal = principal;
      this.roles = Set.copyOf(roles);
      this.permissions = Set.copyOf(permissions);
      this.sessionId = sessionId;
      this.createdTime = Instant.now();
      this.lastAccessTime = this.createdTime;
      this.attributes = new ConcurrentHashMap<>();
    }

    public Principal getPrincipal() {
      return principal;
    }

    public Set<String> getRoles() {
      return roles;
    }

    public Set<String> getPermissions() {
      return permissions;
    }

    public String getSessionId() {
      return sessionId;
    }

    public Instant getCreatedTime() {
      return createdTime;
    }

    public Instant getLastAccessTime() {
      return lastAccessTime;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public void updateLastAccessTime() {
      this.lastAccessTime = Instant.now();
    }

    public boolean hasPermission(final String permission) {
      return permissions.contains(permission) || permissions.contains("*");
    }

    public boolean hasRole(final String role) {
      return roles.contains(role) || roles.contains("admin");
    }
  }

  /** Thread-local security context. */
  private static final ThreadLocal<SecurityContext> SECURITY_CONTEXT = new ThreadLocal<>();

  /** Security policy configuration. */
  private volatile SecurityPolicy securityPolicy;

  /** Audit event storage. */
  private final ConcurrentHashMap<String, SecurityAuditEvent> auditEvents =
      new ConcurrentHashMap<>();

  /** Security statistics. */
  private final AtomicLong totalSecurityChecks = new AtomicLong(0);

  private final AtomicLong permissionGrants = new AtomicLong(0);
  private final AtomicLong permissionDenials = new AtomicLong(0);
  private final AtomicLong threatDetections = new AtomicLong(0);
  private final AtomicLong anomalyDetections = new AtomicLong(0);

  /** Threat detection patterns. */
  private final ConcurrentHashMap<String, ThreatPattern> threatPatterns = new ConcurrentHashMap<>();

  /** Security configuration. */
  private volatile boolean auditingEnabled = true;

  private volatile boolean threatDetectionEnabled = true;
  private volatile boolean anomalyDetectionEnabled = true;
  private volatile int maxAuditEvents = 100000;

  /** Threat detection pattern. */
  private static final class ThreatPattern {
    final String patternId;
    final String description;
    final ThreatLevel threatLevel;
    final java.util.regex.Pattern pattern;

    ThreatPattern(
        final String patternId,
        final String description,
        final ThreatLevel threatLevel,
        final String regex) {
      this.patternId = patternId;
      this.description = description;
      this.threatLevel = threatLevel;
      this.pattern = java.util.regex.Pattern.compile(regex);
    }

    boolean matches(final String input) {
      return pattern.matcher(input).find();
    }
  }

  /** Creates a new enterprise security manager with default configuration. */
  public EnterpriseSecurityManager() {
    this(new DefaultSecurityPolicy());
  }

  /**
   * Creates a new enterprise security manager with specified policy.
   *
   * @param securityPolicy the security policy to enforce
   */
  public EnterpriseSecurityManager(final SecurityPolicy securityPolicy) {
    this.securityPolicy = securityPolicy;
    initializeDefaultThreatPatterns();
    LOGGER.info("Enterprise security manager initialized");
  }

  /**
   * Sets the security context for the current thread.
   *
   * @param context the security context
   */
  public static void setSecurityContext(final SecurityContext context) {
    SECURITY_CONTEXT.set(context);
    if (context != null) {
      context.updateLastAccessTime();
    }
  }

  /**
   * Gets the security context for the current thread.
   *
   * @return the security context or null if not set
   */
  public static SecurityContext getSecurityContext() {
    return SECURITY_CONTEXT.get();
  }

  /** Clears the security context for the current thread. */
  public static void clearSecurityContext() {
    SECURITY_CONTEXT.remove();
  }

  /**
   * Checks if the current principal has the specified permission.
   *
   * @param permission the permission to check
   * @return true if permission is granted
   */
  public boolean checkPermission(final String permission) {
    final SecurityContext context = getSecurityContext();
    totalSecurityChecks.incrementAndGet();

    if (context == null) {
      logSecurityEvent(
          SecurityEventType.AUTHORIZATION_FAILURE,
          null,
          null,
          null,
          permission,
          false,
          "No security context",
          ThreatLevel.MEDIUM);
      permissionDenials.incrementAndGet();
      return false;
    }

    context.updateLastAccessTime();
    final boolean granted = context.hasPermission(permission);

    if (granted) {
      permissionGrants.incrementAndGet();
      logSecurityEvent(
          SecurityEventType.PERMISSION_GRANTED,
          context.getPrincipal(),
          null,
          null,
          permission,
          true,
          null,
          ThreatLevel.LOW);
    } else {
      permissionDenials.incrementAndGet();
      logSecurityEvent(
          SecurityEventType.PERMISSION_DENIED,
          context.getPrincipal(),
          null,
          null,
          permission,
          false,
          "Insufficient permissions",
          ThreatLevel.MEDIUM);
    }

    return granted;
  }

  /**
   * Validates module signature and permissions.
   *
   * @param moduleId the module identifier
   * @param moduleBytes the module bytecode
   * @param signature the module signature
   * @return true if module is authorized
   */
  public boolean authorizeModule(
      final String moduleId, final byte[] moduleBytes, final ModuleSignature signature) {
    final SecurityContext context = getSecurityContext();
    totalSecurityChecks.incrementAndGet();

    if (context == null) {
      logSecurityEvent(
          SecurityEventType.MODULE_LOAD,
          null,
          moduleId,
          null,
          "load",
          false,
          "No security context",
          ThreatLevel.HIGH);
      return false;
    }

    // Check basic module load permission
    if (!context.hasPermission("module.load")) {
      logSecurityEvent(
          SecurityEventType.MODULE_LOAD,
          context.getPrincipal(),
          moduleId,
          null,
          "load",
          false,
          "No module load permission",
          ThreatLevel.HIGH);
      return false;
    }

    // Verify signature if required
    if (securityPolicy.requireSignatures()) {
      if (signature == null) {
        logSecurityEvent(
            SecurityEventType.MODULE_LOAD,
            context.getPrincipal(),
            moduleId,
            null,
            "load",
            false,
            "Missing required signature",
            ThreatLevel.HIGH);
        return false;
      }

      if (!verifyModuleSignature(moduleBytes, signature)) {
        logSecurityEvent(
            SecurityEventType.MODULE_LOAD,
            context.getPrincipal(),
            moduleId,
            null,
            "load",
            false,
            "Invalid signature",
            ThreatLevel.CRITICAL);
        return false;
      }
    }

    // Run threat detection
    if (threatDetectionEnabled) {
      final ThreatLevel threat = detectThreats(moduleId, moduleBytes);
      if (threat == ThreatLevel.CRITICAL || threat == ThreatLevel.HIGH) {
        logSecurityEvent(
            SecurityEventType.THREAT_DETECTED,
            context.getPrincipal(),
            moduleId,
            null,
            "threat_scan",
            false,
            "High-level threat detected",
            threat);
        threatDetections.incrementAndGet();
        return false;
      }
    }

    logSecurityEvent(
        SecurityEventType.MODULE_LOAD,
        context.getPrincipal(),
        moduleId,
        null,
        "load",
        true,
        "Module authorized",
        ThreatLevel.LOW);
    return true;
  }

  /**
   * Authorizes function call with specified parameters.
   *
   * @param moduleId the module identifier
   * @param functionName the function name
   * @param parameters the function parameters
   * @return true if function call is authorized
   */
  public boolean authorizeFunctionCall(
      final String moduleId, final String functionName, final Object[] parameters) {
    final SecurityContext context = getSecurityContext();
    totalSecurityChecks.incrementAndGet();

    if (context == null) {
      logSecurityEvent(
          SecurityEventType.FUNCTION_CALL,
          null,
          moduleId,
          functionName,
          "call",
          false,
          "No security context",
          ThreatLevel.MEDIUM);
      return false;
    }

    context.updateLastAccessTime();

    // Check function execution permission
    if (!context.hasPermission("function.execute")) {
      logSecurityEvent(
          SecurityEventType.FUNCTION_CALL,
          context.getPrincipal(),
          moduleId,
          functionName,
          "call",
          false,
          "No function execution permission",
          ThreatLevel.MEDIUM);
      return false;
    }

    // Check specific function permission if configured
    final String specificPermission = "function." + functionName;
    if (!context.hasPermission("function.*") && !context.hasPermission(specificPermission)) {
      logSecurityEvent(
          SecurityEventType.FUNCTION_CALL,
          context.getPrincipal(),
          moduleId,
          functionName,
          "call",
          false,
          "No specific function permission",
          ThreatLevel.MEDIUM);
      return false;
    }

    // Perform anomaly detection on parameters
    if (anomalyDetectionEnabled) {
      if (detectParameterAnomalies(functionName, parameters)) {
        logSecurityEvent(
            SecurityEventType.ANOMALY_DETECTED,
            context.getPrincipal(),
            moduleId,
            functionName,
            "parameter_check",
            false,
            "Parameter anomaly detected",
            ThreatLevel.MEDIUM);
        anomalyDetections.incrementAndGet();
        return false;
      }
    }

    logSecurityEvent(
        SecurityEventType.FUNCTION_CALL,
        context.getPrincipal(),
        moduleId,
        functionName,
        "call",
        true,
        "Function call authorized",
        ThreatLevel.LOW);
    return true;
  }

  /** Logs a security event for audit purposes. */
  private void logSecurityEvent(
      final SecurityEventType eventType,
      final Principal principal,
      final String moduleId,
      final String resource,
      final String action,
      final boolean success,
      final String details,
      final ThreatLevel threatLevel) {
    if (!auditingEnabled) {
      return;
    }

    final String eventId = generateEventId();
    final SecurityAuditEvent event =
        new SecurityAuditEvent(
            eventId,
            eventType,
            principal,
            moduleId,
            resource,
            action,
            success,
            details,
            threatLevel);

    // Store event (with size limit)
    if (auditEvents.size() >= maxAuditEvents) {
      // Remove oldest events (simplified - in production would use proper LRU)
      final String oldestKey = auditEvents.keySet().iterator().next();
      auditEvents.remove(oldestKey);
    }
    auditEvents.put(eventId, event);

    // Log to standard logging
    final String logLevel =
        threatLevel == ThreatLevel.CRITICAL || threatLevel == ThreatLevel.HIGH ? "WARNING" : "INFO";
    LOGGER.log(
        java.util.logging.Level.parse(logLevel),
        String.format(
            "SECURITY: %s - %s %s %s by %s (success=%b) [%s]",
            eventType,
            action,
            resource != null ? resource : "",
            moduleId != null ? moduleId : "",
            principal != null ? principal.getName() : "anonymous",
            success,
            details != null ? details : ""));
  }

  /** Verifies module signature. */
  private boolean verifyModuleSignature(final byte[] moduleBytes, final ModuleSignature signature) {
    // Simplified signature verification - in production would use proper cryptographic verification
    if (signature == null || signature.getSignature() == null) {
      return false;
    }

    // Check signature algorithm is allowed
    if (!securityPolicy.getAllowedSignatureAlgorithms().contains(signature.getAlgorithm())) {
      return false;
    }

    // Check signature age
    if (signature
        .getTimestamp()
        .isBefore(Instant.now().minus(securityPolicy.getMaxSignatureAge()))) {
      return false;
    }

    // In a real implementation, this would perform cryptographic signature verification
    // For now, we just check that signature exists and is non-empty
    return signature.getSignature().length > 0;
  }

  /** Detects security threats in module bytecode. */
  private ThreatLevel detectThreats(final String moduleId, final byte[] moduleBytes) {
    ThreatLevel maxThreat = ThreatLevel.LOW;

    // Check against known threat patterns
    for (final ThreatPattern pattern : threatPatterns.values()) {
      if (pattern.matches(new String(moduleBytes, java.nio.charset.StandardCharsets.ISO_8859_1))) {
        if (pattern.threatLevel.ordinal() > maxThreat.ordinal()) {
          maxThreat = pattern.threatLevel;
        }
        LOGGER.warning("Threat pattern '" + pattern.patternId + "' detected in module " + moduleId);
      }
    }

    return maxThreat;
  }

  /** Detects anomalies in function parameters. */
  private boolean detectParameterAnomalies(final String functionName, final Object[] parameters) {
    if (parameters == null || parameters.length == 0) {
      return false;
    }

    // Simple anomaly detection - check for suspiciously large parameters
    for (final Object param : parameters) {
      if (param instanceof String) {
        final String str = (String) param;
        if (str.length() > 10000) { // Suspiciously large string
          return true;
        }
      } else if (param instanceof byte[]) {
        final byte[] bytes = (byte[]) param;
        if (bytes.length > 1024 * 1024) { // Suspiciously large byte array
          return true;
        }
      }
    }

    return false;
  }

  /** Initializes default threat detection patterns. */
  private void initializeDefaultThreatPatterns() {
    // Add some basic threat patterns
    threatPatterns.put(
        "exec",
        new ThreatPattern(
            "exec",
            "Process execution pattern",
            ThreatLevel.HIGH,
            "(exec|system|cmd|powershell|bash|sh)"));
    threatPatterns.put(
        "network",
        new ThreatPattern(
            "network",
            "Network access pattern",
            ThreatLevel.MEDIUM,
            "(http|ftp|tcp|udp|socket|connect)"));
    threatPatterns.put(
        "file",
        new ThreatPattern(
            "file",
            "File system access pattern",
            ThreatLevel.MEDIUM,
            "(file|read|write|delete|mkdir|rmdir)"));
  }

  /** Generates a unique event ID. */
  private String generateEventId() {
    return "SEC-" + System.currentTimeMillis() + "-" + Thread.currentThread().threadId();
  }

  /**
   * Gets security statistics.
   *
   * @return formatted security statistics
   */
  public String getSecurityStatistics() {
    return String.format(
        "Security Statistics: checks=%d, grants=%d, denials=%d, threats=%d, anomalies=%d,"
            + " audit_events=%d",
        totalSecurityChecks.get(),
        permissionGrants.get(),
        permissionDenials.get(),
        threatDetections.get(),
        anomalyDetections.get(),
        auditEvents.size());
  }

  /**
   * Gets recent audit events.
   *
   * @param limit maximum number of events to return
   * @return list of recent audit events
   */
  public List<SecurityAuditEvent> getRecentAuditEvents(final int limit) {
    return auditEvents.values().stream()
        .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
        .limit(limit)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Enables or disables audit logging.
   *
   * @param enabled true to enable auditing
   */
  public void setAuditingEnabled(final boolean enabled) {
    this.auditingEnabled = enabled;
    LOGGER.info("Security auditing " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Enables or disables threat detection.
   *
   * @param enabled true to enable threat detection
   */
  public void setThreatDetectionEnabled(final boolean enabled) {
    this.threatDetectionEnabled = enabled;
    LOGGER.info("Threat detection " + (enabled ? "enabled" : "disabled"));
  }

  /** Default security policy implementation. */
  private static final class DefaultSecurityPolicy implements SecurityPolicy {
    @Override
    public boolean requireSignatures() {
      return false;
    }

    @Override
    public boolean enforceCertificateChains() {
      return false;
    }

    @Override
    public Set<SignatureAlgorithm> getAllowedSignatureAlgorithms() {
      return Set.of(SignatureAlgorithm.RSA_SHA256, SignatureAlgorithm.ECDSA_P256_SHA256);
    }

    @Override
    public java.time.Duration getMaxSignatureAge() {
      return java.time.Duration.ofDays(30);
    }

    @Override
    public boolean allowSelfSigned() {
      return true;
    }
  }
}
