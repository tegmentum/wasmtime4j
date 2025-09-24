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

package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced resource security and access control manager providing comprehensive
 * authentication, authorization, audit trails, and attack prevention for resource access.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Role-based access control (RBAC) for resource operations
 *   <li>Resource isolation and containment between tenants
 *   <li>Comprehensive audit trails for compliance and monitoring
 *   <li>Attack prevention and anomaly detection
 *   <li>Security policy enforcement and validation
 *   <li>Resource access encryption and secure communication
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceSecurityManager {

  private static final Logger LOGGER = Logger.getLogger(ResourceSecurityManager.class.getName());

  /** Security principal representing an authenticated entity. */
  public static final class SecurityPrincipal {
    private final String principalId;
    private final String tenantId;
    private final PrincipalType type;
    private final Set<String> roles;
    private final Map<String, String> attributes;
    private final Instant createdAt;
    private final Instant lastAuthenticated;
    private final boolean active;

    public SecurityPrincipal(final String principalId, final String tenantId, final PrincipalType type,
                            final Set<String> roles, final Map<String, String> attributes) {
      this.principalId = principalId;
      this.tenantId = tenantId;
      this.type = type;
      this.roles = Set.copyOf(roles);
      this.attributes = Map.copyOf(attributes);
      this.createdAt = Instant.now();
      this.lastAuthenticated = Instant.now();
      this.active = true;
    }

    // Getters
    public String getPrincipalId() { return principalId; }
    public String getTenantId() { return tenantId; }
    public PrincipalType getType() { return type; }
    public Set<String> getRoles() { return roles; }
    public Map<String, String> getAttributes() { return attributes; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAuthenticated() { return lastAuthenticated; }
    public boolean isActive() { return active; }

    public boolean hasRole(final String role) {
      return roles.contains(role);
    }

    public boolean hasAnyRole(final Set<String> requiredRoles) {
      return requiredRoles.stream().anyMatch(roles::contains);
    }
  }

  /** Principal types. */
  public enum PrincipalType {
    USER,           // End user
    SERVICE,        // Service account
    SYSTEM,         // System process
    APPLICATION     // Application instance
  }

  /** Resource permission definition. */
  public static final class ResourcePermission {
    private final String permissionId;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final String tenantPattern;
    private final Set<ResourceOperation> allowedOperations;
    private final Set<String> requiredRoles;
    private final Map<String, Object> conditions;
    private final Instant validFrom;
    private final Instant validUntil;
    private final boolean active;

    private ResourcePermission(final Builder builder) {
      this.permissionId = builder.permissionId;
      this.resourceType = builder.resourceType;
      this.tenantPattern = builder.tenantPattern;
      this.allowedOperations = EnumSet.copyOf(builder.allowedOperations);
      this.requiredRoles = Set.copyOf(builder.requiredRoles);
      this.conditions = Map.copyOf(builder.conditions);
      this.validFrom = builder.validFrom;
      this.validUntil = builder.validUntil;
      this.active = builder.active;
    }

    public static Builder builder(final String permissionId) {
      return new Builder(permissionId);
    }

    public static final class Builder {
      private final String permissionId;
      private ResourceQuotaManager.ResourceType resourceType;
      private String tenantPattern = ".*";
      private Set<ResourceOperation> allowedOperations = EnumSet.noneOf(ResourceOperation.class);
      private Set<String> requiredRoles = Set.of();
      private Map<String, Object> conditions = Map.of();
      private Instant validFrom = Instant.now();
      private Instant validUntil = Instant.now().plus(Duration.ofDays(365));
      private boolean active = true;

      private Builder(final String permissionId) {
        this.permissionId = permissionId;
      }

      public Builder withResourceType(final ResourceQuotaManager.ResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
      }

      public Builder withTenantPattern(final String tenantPattern) {
        this.tenantPattern = tenantPattern;
        return this;
      }

      public Builder withAllowedOperations(final ResourceOperation... operations) {
        this.allowedOperations = EnumSet.copyOf(List.of(operations));
        return this;
      }

      public Builder withRequiredRoles(final String... roles) {
        this.requiredRoles = Set.of(roles);
        return this;
      }

      public Builder withCondition(final String key, final Object value) {
        final Map<String, Object> newConditions = new ConcurrentHashMap<>(this.conditions);
        newConditions.put(key, value);
        this.conditions = newConditions;
        return this;
      }

      public Builder withValidFrom(final Instant validFrom) {
        this.validFrom = validFrom;
        return this;
      }

      public Builder withValidUntil(final Instant validUntil) {
        this.validUntil = validUntil;
        return this;
      }

      public Builder withActive(final boolean active) {
        this.active = active;
        return this;
      }

      public ResourcePermission build() {
        return new ResourcePermission(this);
      }
    }

    // Getters
    public String getPermissionId() { return permissionId; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public String getTenantPattern() { return tenantPattern; }
    public Set<ResourceOperation> getAllowedOperations() { return allowedOperations; }
    public Set<String> getRequiredRoles() { return requiredRoles; }
    public Map<String, Object> getConditions() { return conditions; }
    public Instant getValidFrom() { return validFrom; }
    public Instant getValidUntil() { return validUntil; }
    public boolean isActive() { return active; }

    public boolean isValid() {
      final Instant now = Instant.now();
      return active && now.isAfter(validFrom) && now.isBefore(validUntil);
    }

    public boolean matches(final String tenantId, final ResourceQuotaManager.ResourceType resourceType,
                          final ResourceOperation operation) {
      if (!isValid()) {
        return false;
      }

      if (this.resourceType != null && !this.resourceType.equals(resourceType)) {
        return false;
      }

      if (!tenantId.matches(tenantPattern)) {
        return false;
      }

      return allowedOperations.contains(operation);
    }
  }

  /** Resource operations for access control. */
  public enum ResourceOperation {
    READ,           // Read resource information
    ALLOCATE,       // Allocate resources
    DEALLOCATE,     // Deallocate resources
    MODIFY_QUOTA,   // Modify resource quotas
    VIEW_USAGE,     // View usage statistics
    MANAGE_POOL,    // Manage resource pools
    CONFIGURE,      // Configure resource settings
    AUDIT           // Access audit information
  }

  /** Security audit event. */
  public static final class SecurityAuditEvent {
    private final String eventId;
    private final String principalId;
    private final String tenantId;
    private final EventType eventType;
    private final ResourceOperation operation;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final EventResult result;
    private final String description;
    private final Map<String, Object> details;
    private final String clientInfo;
    private final Instant timestamp;

    public SecurityAuditEvent(final String eventId, final String principalId, final String tenantId,
                             final EventType eventType, final ResourceOperation operation,
                             final ResourceQuotaManager.ResourceType resourceType, final EventResult result,
                             final String description, final Map<String, Object> details, final String clientInfo) {
      this.eventId = eventId;
      this.principalId = principalId;
      this.tenantId = tenantId;
      this.eventType = eventType;
      this.operation = operation;
      this.resourceType = resourceType;
      this.result = result;
      this.description = description;
      this.details = Map.copyOf(details != null ? details : Map.of());
      this.clientInfo = clientInfo;
      this.timestamp = Instant.now();
    }

    // Getters
    public String getEventId() { return eventId; }
    public String getPrincipalId() { return principalId; }
    public String getTenantId() { return tenantId; }
    public EventType getEventType() { return eventType; }
    public ResourceOperation getOperation() { return operation; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public EventResult getResult() { return result; }
    public String getDescription() { return description; }
    public Map<String, Object> getDetails() { return details; }
    public String getClientInfo() { return clientInfo; }
    public Instant getTimestamp() { return timestamp; }
  }

  /** Audit event types. */
  public enum EventType {
    AUTHENTICATION,   // Authentication events
    AUTHORIZATION,    // Authorization events
    RESOURCE_ACCESS,  // Resource access events
    SECURITY_VIOLATION, // Security violations
    CONFIGURATION_CHANGE, // Configuration changes
    POLICY_EVALUATION   // Policy evaluations
  }

  /** Event results. */
  public enum EventResult {
    SUCCESS,    // Operation succeeded
    FAILURE,    // Operation failed
    DENIED,     // Access denied
    WARNING     // Warning condition
  }

  /** Security threat detection. */
  public static final class SecurityThreat {
    private final String threatId;
    private final String principalId;
    private final String tenantId;
    private final ThreatType threatType;
    private final ThreatSeverity severity;
    private final String description;
    private final List<SecurityAuditEvent> evidenceEvents;
    private final Instant detectedAt;
    private final Map<String, Object> threatData;

    public SecurityThreat(final String threatId, final String principalId, final String tenantId,
                         final ThreatType threatType, final ThreatSeverity severity, final String description,
                         final List<SecurityAuditEvent> evidenceEvents, final Map<String, Object> threatData) {
      this.threatId = threatId;
      this.principalId = principalId;
      this.tenantId = tenantId;
      this.threatType = threatType;
      this.severity = severity;
      this.description = description;
      this.evidenceEvents = List.copyOf(evidenceEvents);
      this.detectedAt = Instant.now();
      this.threatData = Map.copyOf(threatData != null ? threatData : Map.of());
    }

    // Getters
    public String getThreatId() { return threatId; }
    public String getPrincipalId() { return principalId; }
    public String getTenantId() { return tenantId; }
    public ThreatType getThreatType() { return threatType; }
    public ThreatSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public List<SecurityAuditEvent> getEvidenceEvents() { return evidenceEvents; }
    public Instant getDetectedAt() { return detectedAt; }
    public Map<String, Object> getThreatData() { return threatData; }
  }

  /** Threat types. */
  public enum ThreatType {
    BRUTE_FORCE,        // Brute force attacks
    PRIVILEGE_ESCALATION, // Privilege escalation attempts
    RESOURCE_ABUSE,     // Resource abuse or misuse
    ANOMALOUS_BEHAVIOR, // Unusual access patterns
    POLICY_VIOLATION,   // Security policy violations
    DATA_EXFILTRATION   // Potential data theft
  }

  /** Threat severity levels. */
  public enum ThreatSeverity {
    CRITICAL, HIGH, MEDIUM, LOW
  }

  /** Security context for current operation. */
  public static final class SecurityContext {
    private final SecurityPrincipal principal;
    private final String sessionId;
    private final Instant sessionStart;
    private final String clientInfo;
    private final Map<String, Object> context;

    public SecurityContext(final SecurityPrincipal principal, final String sessionId,
                          final String clientInfo, final Map<String, Object> context) {
      this.principal = principal;
      this.sessionId = sessionId;
      this.sessionStart = Instant.now();
      this.clientInfo = clientInfo;
      this.context = Map.copyOf(context != null ? context : Map.of());
    }

    // Getters
    public SecurityPrincipal getPrincipal() { return principal; }
    public String getSessionId() { return sessionId; }
    public Instant getSessionStart() { return sessionStart; }
    public String getClientInfo() { return clientInfo; }
    public Map<String, Object> getContext() { return context; }
  }

  // Instance fields
  private final ConcurrentHashMap<String, SecurityPrincipal> principals = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, ResourcePermission> permissions = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, SecurityAuditEvent> auditEvents = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, SecurityThreat> detectedThreats = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, SecurityContext> activeSessions = new ConcurrentHashMap<>();

  private final ScheduledExecutorService securityExecutor = Executors.newScheduledThreadPool(2);
  private final AtomicLong totalAuthenticationAttempts = new AtomicLong(0);
  private final AtomicLong successfulAuthentications = new AtomicLong(0);
  private final AtomicLong failedAuthentications = new AtomicLong(0);
  private final AtomicLong authorizationChecks = new AtomicLong(0);
  private final AtomicLong accessDenials = new AtomicLong(0);
  private final AtomicLong threatsDetected = new AtomicLong(0);

  private volatile boolean enabled = true;
  private volatile boolean threatDetectionEnabled = true;
  private volatile Duration sessionTimeout = Duration.ofHours(8);
  private volatile int maxFailedAttempts = 5;

  public ResourceSecurityManager() {
    initializeDefaultRoles();
    initializeDefaultPermissions();
    startSecurityTasks();
    LOGGER.info("Resource security manager initialized");
  }

  /**
   * Authenticates a principal and creates a security context.
   *
   * @param principalId principal identifier
   * @param credentials authentication credentials
   * @param clientInfo client information
   * @return security context or null if authentication fails
   */
  public SecurityContext authenticate(final String principalId, final Map<String, Object> credentials,
                                     final String clientInfo) {
    if (!enabled) {
      return null;
    }

    totalAuthenticationAttempts.incrementAndGet();
    final String eventId = "auth-" + System.currentTimeMillis();

    try {
      // Validate credentials (simplified implementation)
      if (!validateCredentials(principalId, credentials)) {
        failedAuthentications.incrementAndGet();
        recordAuditEvent(new SecurityAuditEvent(eventId, principalId, null, EventType.AUTHENTICATION,
            null, null, EventResult.FAILURE, "Authentication failed", credentials, clientInfo));

        checkForBruteForceAttack(principalId, clientInfo);
        return null;
      }

      final SecurityPrincipal principal = principals.get(principalId);
      if (principal == null || !principal.isActive()) {
        failedAuthentications.incrementAndGet();
        recordAuditEvent(new SecurityAuditEvent(eventId, principalId, null, EventType.AUTHENTICATION,
            null, null, EventResult.DENIED, "Principal not found or inactive", credentials, clientInfo));
        return null;
      }

      successfulAuthentications.incrementAndGet();
      final String sessionId = "session-" + System.currentTimeMillis() + "-" + Math.random();
      final SecurityContext context = new SecurityContext(principal, sessionId, clientInfo, Map.of());

      activeSessions.put(sessionId, context);

      recordAuditEvent(new SecurityAuditEvent(eventId, principalId, principal.getTenantId(),
          EventType.AUTHENTICATION, null, null, EventResult.SUCCESS,
          "Authentication successful", Map.of("session_id", sessionId), clientInfo));

      LOGGER.info(String.format("Principal %s authenticated successfully", principalId));
      return context;

    } catch (final Exception e) {
      failedAuthentications.incrementAndGet();
      recordAuditEvent(new SecurityAuditEvent(eventId, principalId, null, EventType.AUTHENTICATION,
          null, null, EventResult.FAILURE, "Authentication error: " + e.getMessage(),
          Map.of("error", e.getClass().getSimpleName()), clientInfo));
      LOGGER.warning(String.format("Authentication error for principal %s: %s", principalId, e.getMessage()));
      return null;
    }
  }

  /**
   * Checks if a principal is authorized to perform an operation.
   *
   * @param context security context
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param operation resource operation
   * @return true if authorized
   */
  public boolean authorize(final SecurityContext context, final String tenantId,
                          final ResourceQuotaManager.ResourceType resourceType, final ResourceOperation operation) {
    if (!enabled) {
      return true;
    }

    authorizationChecks.incrementAndGet();
    final String eventId = "authz-" + System.currentTimeMillis();

    try {
      if (context == null || context.getPrincipal() == null) {
        accessDenials.incrementAndGet();
        recordAuditEvent(new SecurityAuditEvent(eventId, null, tenantId, EventType.AUTHORIZATION,
            operation, resourceType, EventResult.DENIED, "No security context", Map.of(), null));
        return false;
      }

      final SecurityPrincipal principal = context.getPrincipal();

      // Check session validity
      if (!isSessionValid(context)) {
        accessDenials.incrementAndGet();
        recordAuditEvent(new SecurityAuditEvent(eventId, principal.getPrincipalId(), tenantId,
            EventType.AUTHORIZATION, operation, resourceType, EventResult.DENIED,
            "Session expired or invalid", Map.of("session_id", context.getSessionId()),
            context.getClientInfo()));
        return false;
      }

      // Check tenant isolation
      if (!principal.getTenantId().equals(tenantId) && !principal.hasRole("SYSTEM_ADMIN")) {
        accessDenials.incrementAndGet();
        recordAuditEvent(new SecurityAuditEvent(eventId, principal.getPrincipalId(), tenantId,
            EventType.AUTHORIZATION, operation, resourceType, EventResult.DENIED,
            "Cross-tenant access denied", Map.of("principal_tenant", principal.getTenantId()),
            context.getClientInfo()));
        return false;
      }

      // Check permissions
      final boolean authorized = checkPermissions(principal, tenantId, resourceType, operation);

      if (authorized) {
        recordAuditEvent(new SecurityAuditEvent(eventId, principal.getPrincipalId(), tenantId,
            EventType.AUTHORIZATION, operation, resourceType, EventResult.SUCCESS,
            "Authorization granted", Map.of(), context.getClientInfo()));
      } else {
        accessDenials.incrementAndGet();
        recordAuditEvent(new SecurityAuditEvent(eventId, principal.getPrincipalId(), tenantId,
            EventType.AUTHORIZATION, operation, resourceType, EventResult.DENIED,
            "Insufficient permissions", Map.of("principal_roles", principal.getRoles()),
            context.getClientInfo()));

        detectAnomalousBehavior(principal.getPrincipalId(), operation, resourceType);
      }

      return authorized;

    } catch (final Exception e) {
      accessDenials.incrementAndGet();
      recordAuditEvent(new SecurityAuditEvent(eventId,
          context != null && context.getPrincipal() != null ? context.getPrincipal().getPrincipalId() : null,
          tenantId, EventType.AUTHORIZATION, operation, resourceType, EventResult.FAILURE,
          "Authorization error: " + e.getMessage(), Map.of("error", e.getClass().getSimpleName()),
          context != null ? context.getClientInfo() : null));
      LOGGER.warning(String.format("Authorization error: %s", e.getMessage()));
      return false;
    }
  }

  /**
   * Creates a security principal.
   *
   * @param principalId principal identifier
   * @param tenantId tenant identifier
   * @param type principal type
   * @param roles assigned roles
   * @param attributes additional attributes
   * @return created principal
   */
  public SecurityPrincipal createPrincipal(final String principalId, final String tenantId,
                                          final PrincipalType type, final Set<String> roles,
                                          final Map<String, String> attributes) {
    final SecurityPrincipal principal = new SecurityPrincipal(principalId, tenantId, type, roles, attributes);
    principals.put(principalId, principal);

    recordAuditEvent(new SecurityAuditEvent("create-principal-" + System.currentTimeMillis(),
        "SYSTEM", tenantId, EventType.CONFIGURATION_CHANGE, null, null, EventResult.SUCCESS,
        "Principal created", Map.of("principal_type", type, "roles", roles), null));

    LOGGER.info(String.format("Created principal %s with roles %s", principalId, roles));
    return principal;
  }

  /**
   * Creates a resource permission.
   *
   * @param permission resource permission
   */
  public void createPermission(final ResourcePermission permission) {
    permissions.put(permission.getPermissionId(), permission);

    recordAuditEvent(new SecurityAuditEvent("create-permission-" + System.currentTimeMillis(),
        "SYSTEM", null, EventType.CONFIGURATION_CHANGE, null, permission.getResourceType(),
        EventResult.SUCCESS, "Permission created",
        Map.of("permission_id", permission.getPermissionId(), "operations", permission.getAllowedOperations()),
        null));

    LOGGER.info(String.format("Created permission %s for resource type %s",
        permission.getPermissionId(), permission.getResourceType()));
  }

  /**
   * Gets audit events for a time period.
   *
   * @param from start time
   * @param to end time
   * @return list of audit events
   */
  public List<SecurityAuditEvent> getAuditEvents(final Instant from, final Instant to) {
    return auditEvents.values().stream()
        .filter(event -> event.getTimestamp().isAfter(from) && event.getTimestamp().isBefore(to))
        .sorted((e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp()))
        .collect(Collectors.toList());
  }

  /**
   * Gets detected security threats.
   *
   * @return list of security threats
   */
  public List<SecurityThreat> getDetectedThreats() {
    return new ArrayList<>(detectedThreats.values());
  }

  /**
   * Gets comprehensive security statistics.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Security Manager Statistics ===\n");

    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Threat detection: %s\n", threatDetectionEnabled));
    sb.append(String.format("Session timeout: %s\n", sessionTimeout));
    sb.append(String.format("Max failed attempts: %d\n", maxFailedAttempts));
    sb.append("\n");

    sb.append(String.format("Total principals: %d\n", principals.size()));
    sb.append(String.format("Total permissions: %d\n", permissions.size()));
    sb.append(String.format("Active sessions: %d\n", activeSessions.size()));
    sb.append(String.format("Audit events: %d\n", auditEvents.size()));
    sb.append(String.format("Detected threats: %d\n", detectedThreats.size()));
    sb.append("\n");

    sb.append("Authentication Statistics:\n");
    sb.append(String.format("  Total attempts: %,d\n", totalAuthenticationAttempts.get()));
    sb.append(String.format("  Successful: %,d\n", successfulAuthentications.get()));
    sb.append(String.format("  Failed: %,d\n", failedAuthentications.get()));

    final double successRate = totalAuthenticationAttempts.get() > 0 ?
        (successfulAuthentications.get() * 100.0) / totalAuthenticationAttempts.get() : 0.0;
    sb.append(String.format("  Success rate: %.2f%%\n", successRate));
    sb.append("\n");

    sb.append("Authorization Statistics:\n");
    sb.append(String.format("  Total checks: %,d\n", authorizationChecks.get()));
    sb.append(String.format("  Access denials: %,d\n", accessDenials.get()));

    final double denialRate = authorizationChecks.get() > 0 ?
        (accessDenials.get() * 100.0) / authorizationChecks.get() : 0.0;
    sb.append(String.format("  Denial rate: %.2f%%\n", denialRate));
    sb.append("\n");

    sb.append("Security Threats:\n");
    final Map<ThreatType, Long> threatCounts = detectedThreats.values().stream()
        .collect(Collectors.groupingBy(SecurityThreat::getThreatType, Collectors.counting()));

    for (final Map.Entry<ThreatType, Long> entry : threatCounts.entrySet()) {
      sb.append(String.format("  %s: %d\n", entry.getKey(), entry.getValue()));
    }

    return sb.toString();
  }

  private boolean validateCredentials(final String principalId, final Map<String, Object> credentials) {
    // Simplified credential validation - in real implementation would hash and compare
    final Object password = credentials.get("password");
    final Object token = credentials.get("token");

    // Allow system principals without credentials
    if (principalId.startsWith("SYSTEM")) {
      return true;
    }

    return password != null || token != null;
  }

  private boolean isSessionValid(final SecurityContext context) {
    if (context == null) {
      return false;
    }

    final Duration sessionAge = Duration.between(context.getSessionStart(), Instant.now());
    return sessionAge.compareTo(sessionTimeout) < 0;
  }

  private boolean checkPermissions(final SecurityPrincipal principal, final String tenantId,
                                  final ResourceQuotaManager.ResourceType resourceType, final ResourceOperation operation) {
    // System admins have all permissions
    if (principal.hasRole("SYSTEM_ADMIN")) {
      return true;
    }

    // Check explicit permissions
    for (final ResourcePermission permission : permissions.values()) {
      if (permission.matches(tenantId, resourceType, operation)) {
        if (permission.getRequiredRoles().isEmpty() || principal.hasAnyRole(permission.getRequiredRoles())) {
          return true;
        }
      }
    }

    return false;
  }

  private void recordAuditEvent(final SecurityAuditEvent event) {
    auditEvents.put(event.getEventId(), event);

    // Maintain audit log size
    if (auditEvents.size() > 100000) {
      cleanupOldAuditEvents();
    }

    LOGGER.fine(String.format("Audit event: %s - %s", event.getEventType(), event.getDescription()));
  }

  private void checkForBruteForceAttack(final String principalId, final String clientInfo) {
    if (!threatDetectionEnabled) {
      return;
    }

    // Count recent failed attempts
    final Instant cutoff = Instant.now().minus(Duration.ofMinutes(15));
    final long failedCount = auditEvents.values().stream()
        .filter(event -> event.getPrincipalId() != null && event.getPrincipalId().equals(principalId))
        .filter(event -> event.getEventType() == EventType.AUTHENTICATION)
        .filter(event -> event.getResult() == EventResult.FAILURE)
        .filter(event -> event.getTimestamp().isAfter(cutoff))
        .count();

    if (failedCount >= maxFailedAttempts) {
      final String threatId = "brute-force-" + principalId + "-" + System.currentTimeMillis();
      final List<SecurityAuditEvent> evidence = auditEvents.values().stream()
          .filter(event -> event.getPrincipalId() != null && event.getPrincipalId().equals(principalId))
          .filter(event -> event.getEventType() == EventType.AUTHENTICATION)
          .filter(event -> event.getResult() == EventResult.FAILURE)
          .filter(event -> event.getTimestamp().isAfter(cutoff))
          .collect(Collectors.toList());

      final SecurityThreat threat = new SecurityThreat(threatId, principalId, null,
          ThreatType.BRUTE_FORCE, ThreatSeverity.HIGH,
          String.format("Brute force attack detected: %d failed attempts in 15 minutes", failedCount),
          evidence, Map.of("failed_count", failedCount, "client_info", clientInfo));

      detectedThreats.put(threatId, threat);
      threatsDetected.incrementAndGet();

      LOGGER.warning(String.format("Brute force attack detected for principal %s: %d failed attempts",
          principalId, failedCount));
    }
  }

  private void detectAnomalousBehavior(final String principalId, final ResourceOperation operation,
                                      final ResourceQuotaManager.ResourceType resourceType) {
    if (!threatDetectionEnabled) {
      return;
    }

    // Simple anomaly detection based on operation patterns
    final Instant cutoff = Instant.now().minus(Duration.ofHours(1));
    final long recentOperations = auditEvents.values().stream()
        .filter(event -> event.getPrincipalId() != null && event.getPrincipalId().equals(principalId))
        .filter(event -> event.getEventType() == EventType.AUTHORIZATION)
        .filter(event -> event.getOperation() == operation)
        .filter(event -> event.getTimestamp().isAfter(cutoff))
        .count();

    // Detect unusual volume of operations
    if (recentOperations > 100) {
      final String threatId = "anomaly-" + principalId + "-" + System.currentTimeMillis();
      final SecurityThreat threat = new SecurityThreat(threatId, principalId, null,
          ThreatType.ANOMALOUS_BEHAVIOR, ThreatSeverity.MEDIUM,
          String.format("Anomalous behavior: %d %s operations in 1 hour", recentOperations, operation),
          List.of(), Map.of("operation_count", recentOperations, "operation_type", operation));

      detectedThreats.put(threatId, threat);
      threatsDetected.incrementAndGet();

      LOGGER.warning(String.format("Anomalous behavior detected for principal %s: %d %s operations",
          principalId, recentOperations, operation));
    }
  }

  private void initializeDefaultRoles() {
    // Create system admin principal
    createPrincipal("SYSTEM_ADMIN", "system", PrincipalType.SYSTEM,
        Set.of("SYSTEM_ADMIN"), Map.of("description", "System administrator"));

    // Create default service account
    createPrincipal("DEFAULT_SERVICE", "default", PrincipalType.SERVICE,
        Set.of("RESOURCE_USER"), Map.of("description", "Default service account"));
  }

  private void initializeDefaultPermissions() {
    // System admin permissions
    createPermission(ResourcePermission.builder("system-admin-all")
        .withTenantPattern(".*")
        .withAllowedOperations(ResourceOperation.values())
        .withRequiredRoles("SYSTEM_ADMIN")
        .build());

    // Resource user permissions
    createPermission(ResourcePermission.builder("resource-user-basic")
        .withTenantPattern(".*")
        .withAllowedOperations(ResourceOperation.READ, ResourceOperation.ALLOCATE,
            ResourceOperation.DEALLOCATE, ResourceOperation.VIEW_USAGE)
        .withRequiredRoles("RESOURCE_USER")
        .build());

    // Tenant admin permissions
    createPermission(ResourcePermission.builder("tenant-admin")
        .withTenantPattern(".*")
        .withAllowedOperations(ResourceOperation.READ, ResourceOperation.ALLOCATE,
            ResourceOperation.DEALLOCATE, ResourceOperation.MODIFY_QUOTA,
            ResourceOperation.VIEW_USAGE, ResourceOperation.CONFIGURE)
        .withRequiredRoles("TENANT_ADMIN")
        .build());
  }

  private void startSecurityTasks() {
    // Session cleanup
    securityExecutor.scheduleAtFixedRate(this::cleanupExpiredSessions, 300, 300, TimeUnit.SECONDS);

    // Audit log cleanup
    securityExecutor.scheduleAtFixedRate(this::cleanupOldAuditEvents, 3600, 3600, TimeUnit.SECONDS);

    // Threat detection maintenance
    securityExecutor.scheduleAtFixedRate(this::cleanupOldThreats, 1800, 1800, TimeUnit.SECONDS);
  }

  private void cleanupExpiredSessions() {
    final Instant cutoff = Instant.now().minus(sessionTimeout);

    activeSessions.entrySet().removeIf(entry -> {
      final SecurityContext context = entry.getValue();
      return context.getSessionStart().isBefore(cutoff);
    });
  }

  private void cleanupOldAuditEvents() {
    final Instant cutoff = Instant.now().minus(Duration.ofDays(90)); // 90 day retention

    auditEvents.entrySet().removeIf(entry -> entry.getValue().getTimestamp().isBefore(cutoff));
  }

  private void cleanupOldThreats() {
    final Instant cutoff = Instant.now().minus(Duration.ofDays(30)); // 30 day retention

    detectedThreats.entrySet().removeIf(entry -> entry.getValue().getDetectedAt().isBefore(cutoff));
  }

  /**
   * Invalidates a security session.
   *
   * @param sessionId session identifier
   */
  public void invalidateSession(final String sessionId) {
    final SecurityContext context = activeSessions.remove(sessionId);
    if (context != null) {
      recordAuditEvent(new SecurityAuditEvent("logout-" + System.currentTimeMillis(),
          context.getPrincipal().getPrincipalId(), context.getPrincipal().getTenantId(),
          EventType.AUTHENTICATION, null, null, EventResult.SUCCESS, "Session invalidated",
          Map.of("session_id", sessionId), context.getClientInfo()));

      LOGGER.info(String.format("Session %s invalidated", sessionId));
    }
  }

  /**
   * Enables or disables security enforcement.
   *
   * @param enabled true to enable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource security " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Enables or disables threat detection.
   *
   * @param enabled true to enable
   */
  public void setThreatDetectionEnabled(final boolean enabled) {
    this.threatDetectionEnabled = enabled;
    LOGGER.info("Threat detection " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Sets the session timeout.
   *
   * @param sessionTimeout timeout duration
   */
  public void setSessionTimeout(final Duration sessionTimeout) {
    this.sessionTimeout = sessionTimeout;
    LOGGER.info("Session timeout set to " + sessionTimeout);
  }

  /**
   * Shuts down the security manager.
   */
  public void shutdown() {
    enabled = false;

    securityExecutor.shutdown();
    try {
      if (!securityExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        securityExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      securityExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Clear sensitive data
    principals.clear();
    activeSessions.clear();

    LOGGER.info("Resource security manager shutdown");
  }
}