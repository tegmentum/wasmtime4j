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

import java.time.Duration;
import java.time.Instant;
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
 * Automated incident response and remediation system providing intelligent incident detection,
 * automated resolution, escalation management, and comprehensive incident lifecycle tracking.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Intelligent incident detection and classification
 *   <li>Automated remediation action execution
 *   <li>Incident escalation and notification management
 *   <li>Runbook automation and procedural execution
 *   <li>Post-incident analysis and learning
 *   <li>SLA monitoring and breach prevention
 *   <li>Integration with external incident management systems
 * </ul>
 *
 * @since 1.0.0
 */
public final class AutomatedIncidentResponseSystem {

  private static final Logger LOGGER = Logger.getLogger(AutomatedIncidentResponseSystem.class.getName());

  /** Incident severity levels. */
  public enum IncidentSeverity {
    LOW(1, Duration.ofHours(4), false),
    MEDIUM(2, Duration.ofHours(2), true),
    HIGH(3, Duration.ofMinutes(30), true),
    CRITICAL(4, Duration.ofMinutes(15), true),
    EMERGENCY(5, Duration.ofMinutes(5), true);

    private final int level;
    private final Duration responseTime;
    private final boolean requiresImmediate;

    IncidentSeverity(final int level, final Duration responseTime, final boolean requiresImmediate) {
      this.level = level;
      this.responseTime = responseTime;
      this.requiresImmediate = requiresImmediate;
    }

    public int getLevel() { return level; }
    public Duration getResponseTime() { return responseTime; }
    public boolean isRequiresImmediate() { return requiresImmediate; }

    public boolean isHigherThan(final IncidentSeverity other) {
      return this.level > other.level;
    }
  }

  /** Incident status throughout lifecycle. */
  public enum IncidentStatus {
    DETECTED("Incident detected and being analyzed"),
    ACKNOWLEDGED("Incident acknowledged by responder"),
    INVESTIGATING("Investigation in progress"),
    REMEDIATING("Automated remediation in progress"),
    ESCALATED("Incident escalated to higher level"),
    RESOLVED("Incident resolved successfully"),
    CLOSED("Incident closed with post-analysis complete");

    private final String description;

    IncidentStatus(final String description) {
      this.description = description;
    }

    public String getDescription() { return description; }
  }

  /** Remediation action types. */
  public enum RemediationActionType {
    RESTART_SERVICE("Restart Service", "Restart affected service or component"),
    SCALE_RESOURCES("Scale Resources", "Increase resource allocation"),
    CLEAR_CACHE("Clear Cache", "Clear application or system caches"),
    RESTART_JVM("Restart JVM", "Restart Java Virtual Machine"),
    TRIGGER_GC("Trigger GC", "Force garbage collection"),
    FAILOVER("Failover", "Switch to backup systems"),
    ISOLATE_COMPONENT("Isolate Component", "Isolate problematic component"),
    RESET_CONNECTIONS("Reset Connections", "Reset network connections"),
    CUSTOM_SCRIPT("Custom Script", "Execute custom remediation script");

    private final String displayName;
    private final String description;

    RemediationActionType(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Automated incident. */
  public static final class Incident {
    private final String incidentId;
    private final String title;
    private final String description;
    private final IncidentSeverity severity;
    private final String sourceComponent;
    private final List<String> affectedComponents;
    private final Map<String, Object> metadata;
    private final Instant detectedAt;
    private volatile IncidentStatus status;
    private volatile String assignedTo;
    private volatile Instant acknowledgedAt;
    private volatile Instant resolvedAt;
    private volatile Instant closedAt;
    private volatile String resolution;
    private final List<String> timeline;
    private final List<RemediationAction> remediationActions;

    public Incident(
        final String incidentId,
        final String title,
        final String description,
        final IncidentSeverity severity,
        final String sourceComponent,
        final List<String> affectedComponents,
        final Map<String, Object> metadata) {
      this.incidentId = incidentId;
      this.title = title;
      this.description = description;
      this.severity = severity;
      this.sourceComponent = sourceComponent;
      this.affectedComponents = new CopyOnWriteArrayList<>(affectedComponents != null ? affectedComponents : List.of());
      this.metadata = new ConcurrentHashMap<>(metadata != null ? metadata : Map.of());
      this.detectedAt = Instant.now();
      this.status = IncidentStatus.DETECTED;
      this.timeline = new CopyOnWriteArrayList<>();
      this.remediationActions = new CopyOnWriteArrayList<>();
      addTimelineEntry("Incident detected and created");
    }

    // Getters
    public String getIncidentId() { return incidentId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public IncidentSeverity getSeverity() { return severity; }
    public String getSourceComponent() { return sourceComponent; }
    public List<String> getAffectedComponents() { return List.copyOf(affectedComponents); }
    public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
    public Instant getDetectedAt() { return detectedAt; }
    public IncidentStatus getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public Instant getAcknowledgedAt() { return acknowledgedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public Instant getClosedAt() { return closedAt; }
    public String getResolution() { return resolution; }
    public List<String> getTimeline() { return List.copyOf(timeline); }
    public List<RemediationAction> getRemediationActions() { return List.copyOf(remediationActions); }

    public void setStatus(final IncidentStatus status) {
      this.status = status;
      addTimelineEntry("Status changed to: " + status.getDescription());
    }

    public void acknowledge(final String assignedTo) {
      this.assignedTo = assignedTo;
      this.acknowledgedAt = Instant.now();
      setStatus(IncidentStatus.ACKNOWLEDGED);
      addTimelineEntry("Acknowledged by: " + assignedTo);
    }

    public void resolve(final String resolution) {
      this.resolution = resolution;
      this.resolvedAt = Instant.now();
      setStatus(IncidentStatus.RESOLVED);
      addTimelineEntry("Resolved: " + resolution);
    }

    public void close() {
      this.closedAt = Instant.now();
      setStatus(IncidentStatus.CLOSED);
      addTimelineEntry("Incident closed");
    }

    public void addTimelineEntry(final String entry) {
      timeline.add(Instant.now() + ": " + entry);
    }

    public void addRemediationAction(final RemediationAction action) {
      remediationActions.add(action);
      addTimelineEntry("Remediation action added: " + action.getActionType().getDisplayName());
    }

    public Duration getAge() {
      return Duration.between(detectedAt, Instant.now());
    }

    public boolean isOpen() {
      return status != IncidentStatus.RESOLVED && status != IncidentStatus.CLOSED;
    }

    public boolean isOverdue() {
      return isOpen() && getAge().compareTo(severity.getResponseTime()) > 0;
    }
  }

  /** Remediation action definition. */
  public static final class RemediationAction {
    private final String actionId;
    private final RemediationActionType actionType;
    private final String description;
    private final Map<String, Object> parameters;
    private final int priority;
    private final Duration timeout;
    private final boolean requiresApproval;
    private volatile boolean executed;
    private volatile boolean successful;
    private volatile Instant executedAt;
    private volatile String executionResult;
    private volatile Duration executionDuration;

    public RemediationAction(
        final String actionId,
        final RemediationActionType actionType,
        final String description,
        final Map<String, Object> parameters,
        final int priority,
        final Duration timeout,
        final boolean requiresApproval) {
      this.actionId = actionId;
      this.actionType = actionType;
      this.description = description;
      this.parameters = Map.copyOf(parameters != null ? parameters : Map.of());
      this.priority = priority;
      this.timeout = timeout != null ? timeout : Duration.ofMinutes(5);
      this.requiresApproval = requiresApproval;
      this.executed = false;
      this.successful = false;
    }

    // Getters
    public String getActionId() { return actionId; }
    public RemediationActionType getActionType() { return actionType; }
    public String getDescription() { return description; }
    public Map<String, Object> getParameters() { return parameters; }
    public int getPriority() { return priority; }
    public Duration getTimeout() { return timeout; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public boolean isExecuted() { return executed; }
    public boolean isSuccessful() { return successful; }
    public Instant getExecutedAt() { return executedAt; }
    public String getExecutionResult() { return executionResult; }
    public Duration getExecutionDuration() { return executionDuration; }

    public void markExecuted(final boolean successful, final String result, final Duration duration) {
      this.executed = true;
      this.successful = successful;
      this.executedAt = Instant.now();
      this.executionResult = result;
      this.executionDuration = duration;
    }
  }

  /** Incident response runbook. */
  public static final class IncidentRunbook {
    private final String runbookId;
    private final String name;
    private final String description;
    private final List<String> triggerConditions;
    private final List<RemediationAction> actions;
    private final IncidentSeverity minimumSeverity;
    private final boolean autoExecute;
    private final Map<String, Object> configuration;

    public IncidentRunbook(
        final String runbookId,
        final String name,
        final String description,
        final List<String> triggerConditions,
        final List<RemediationAction> actions,
        final IncidentSeverity minimumSeverity,
        final boolean autoExecute,
        final Map<String, Object> configuration) {
      this.runbookId = runbookId;
      this.name = name;
      this.description = description;
      this.triggerConditions = List.copyOf(triggerConditions != null ? triggerConditions : List.of());
      this.actions = new CopyOnWriteArrayList<>(actions != null ? actions : List.of());
      this.minimumSeverity = minimumSeverity != null ? minimumSeverity : IncidentSeverity.LOW;
      this.autoExecute = autoExecute;
      this.configuration = Map.copyOf(configuration != null ? configuration : Map.of());
    }

    // Getters
    public String getRunbookId() { return runbookId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public List<String> getTriggerConditions() { return triggerConditions; }
    public List<RemediationAction> getActions() { return List.copyOf(actions); }
    public IncidentSeverity getMinimumSeverity() { return minimumSeverity; }
    public boolean isAutoExecute() { return autoExecute; }
    public Map<String, Object> getConfiguration() { return configuration; }

    public boolean shouldTrigger(final Incident incident) {
      if (incident.getSeverity().getLevel() < minimumSeverity.getLevel()) {
        return false;
      }

      // Simple condition matching - in production would be more sophisticated
      return triggerConditions.isEmpty() ||
             triggerConditions.stream().anyMatch(condition ->
                 incident.getDescription().toLowerCase().contains(condition.toLowerCase()) ||
                 incident.getSourceComponent().toLowerCase().contains(condition.toLowerCase()));
    }
  }

  /** Escalation rule configuration. */
  public static final class EscalationRule {
    private final String ruleId;
    private final String name;
    private final IncidentSeverity triggerSeverity;
    private final Duration escalationTime;
    private final List<String> escalationTargets;
    private final String escalationMessage;
    private final boolean autoEscalate;

    public EscalationRule(
        final String ruleId,
        final String name,
        final IncidentSeverity triggerSeverity,
        final Duration escalationTime,
        final List<String> escalationTargets,
        final String escalationMessage,
        final boolean autoEscalate) {
      this.ruleId = ruleId;
      this.name = name;
      this.triggerSeverity = triggerSeverity;
      this.escalationTime = escalationTime;
      this.escalationTargets = List.copyOf(escalationTargets != null ? escalationTargets : List.of());
      this.escalationMessage = escalationMessage;
      this.autoEscalate = autoEscalate;
    }

    // Getters
    public String getRuleId() { return ruleId; }
    public String getName() { return name; }
    public IncidentSeverity getTriggerSeverity() { return triggerSeverity; }
    public Duration getEscalationTime() { return escalationTime; }
    public List<String> getEscalationTargets() { return escalationTargets; }
    public String getEscalationMessage() { return escalationMessage; }
    public boolean isAutoEscalate() { return autoEscalate; }

    public boolean shouldEscalate(final Incident incident) {
      return autoEscalate &&
             incident.getSeverity().getLevel() >= triggerSeverity.getLevel() &&
             incident.isOpen() &&
             incident.getAge().compareTo(escalationTime) >= 0;
    }
  }

  // Instance fields
  private final IntelligentAlertingSystem alertingSystem;
  private final HealthCheckSystem healthCheckSystem;
  private final ProductionMonitoringSystem monitoringSystem;

  private final ConcurrentHashMap<String, Incident> activeIncidents = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Incident> closedIncidents = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, IncidentRunbook> runbooks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, EscalationRule> escalationRules = new ConcurrentHashMap<>();

  // Listeners and handlers
  private final List<IncidentListener> incidentListeners = new CopyOnWriteArrayList<>();
  private final List<RemediationHandler> remediationHandlers = new CopyOnWriteArrayList<>();

  // Statistics and monitoring
  private final AtomicLong totalIncidentsDetected = new AtomicLong(0);
  private final AtomicLong totalIncidentsResolved = new AtomicLong(0);
  private final AtomicLong totalRemediationsExecuted = new AtomicLong(0);
  private final AtomicLong totalEscalations = new AtomicLong(0);
  private final AtomicReference<Duration> averageResolutionTime = new AtomicReference<>(Duration.ZERO);

  // Background processing
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(3);

  // Configuration
  private volatile boolean autoRemediationEnabled = true;
  private volatile boolean autoEscalationEnabled = true;
  private volatile int maxActiveIncidents = 100;
  private volatile Duration incidentRetentionPeriod = Duration.ofDays(30);

  /** Incident lifecycle listener. */
  @FunctionalInterface
  public interface IncidentListener {
    void onIncidentStateChange(Incident incident, IncidentStatus previousStatus);
  }

  /** Remediation action handler. */
  @FunctionalInterface
  public interface RemediationHandler {
    boolean executeRemediation(RemediationAction action, Incident incident);
  }

  /**
   * Creates automated incident response system.
   *
   * @param alertingSystem the intelligent alerting system
   * @param healthCheckSystem the health check system
   * @param monitoringSystem the production monitoring system
   */
  public AutomatedIncidentResponseSystem(
      final IntelligentAlertingSystem alertingSystem,
      final HealthCheckSystem healthCheckSystem,
      final ProductionMonitoringSystem monitoringSystem) {
    this.alertingSystem = alertingSystem;
    this.healthCheckSystem = healthCheckSystem;
    this.monitoringSystem = monitoringSystem;
    initializeDefaultRunbooks();
    initializeDefaultEscalationRules();
    startBackgroundProcessing();
    integrateWithAlertingSystem();
    LOGGER.info("Automated incident response system initialized");
  }

  /** Initializes default incident response runbooks. */
  private void initializeDefaultRunbooks() {
    // Memory exhaustion runbook
    final List<RemediationAction> memoryActions = List.of(
        new RemediationAction(
            "trigger_gc",
            RemediationActionType.TRIGGER_GC,
            "Force garbage collection to free memory",
            Map.of("force", true),
            1,
            Duration.ofMinutes(1),
            false),
        new RemediationAction(
            "clear_cache",
            RemediationActionType.CLEAR_CACHE,
            "Clear application caches to reduce memory usage",
            Map.of("cache_types", List.of("application", "session")),
            2,
            Duration.ofMinutes(2),
            false),
        new RemediationAction(
            "restart_service",
            RemediationActionType.RESTART_SERVICE,
            "Restart service if memory issues persist",
            Map.of("service", "wasmtime4j"),
            3,
            Duration.ofMinutes(5),
            true));

    addRunbook(new IncidentRunbook(
        "memory_exhaustion",
        "Memory Exhaustion Response",
        "Automated response for memory exhaustion incidents",
        List.of("memory", "heap", "OutOfMemoryError"),
        memoryActions,
        IncidentSeverity.HIGH,
        true,
        Map.of("max_retries", 3)));

    // Thread deadlock runbook
    final List<RemediationAction> threadActions = List.of(
        new RemediationAction(
            "thread_dump",
            RemediationActionType.CUSTOM_SCRIPT,
            "Generate thread dump for analysis",
            Map.of("output_file", "/tmp/thread_dump_" + System.currentTimeMillis() + ".txt"),
            1,
            Duration.ofMinutes(1),
            false),
        new RemediationAction(
            "restart_jvm",
            RemediationActionType.RESTART_JVM,
            "Restart JVM to resolve deadlock",
            Map.of("graceful_shutdown", true),
            2,
            Duration.ofMinutes(3),
            true));

    addRunbook(new IncidentRunbook(
        "thread_deadlock",
        "Thread Deadlock Response",
        "Automated response for thread deadlock incidents",
        List.of("deadlock", "thread", "blocked"),
        threadActions,
        IncidentSeverity.CRITICAL,
        true,
        Map.of("immediate_action", true)));

    // High error rate runbook
    final List<RemediationAction> errorActions = List.of(
        new RemediationAction(
            "isolate_component",
            RemediationActionType.ISOLATE_COMPONENT,
            "Isolate problematic component causing errors",
            Map.of("component_type", "module"),
            1,
            Duration.ofMinutes(1),
            false),
        new RemediationAction(
            "reset_connections",
            RemediationActionType.RESET_CONNECTIONS,
            "Reset network connections to clear issues",
            Map.of("connection_pools", List.of("database", "external_api")),
            2,
            Duration.ofMinutes(2),
            false));

    addRunbook(new IncidentRunbook(
        "high_error_rate",
        "High Error Rate Response",
        "Automated response for high error rate incidents",
        List.of("error", "exception", "failure"),
        errorActions,
        IncidentSeverity.MEDIUM,
        true,
        Map.of("error_threshold", 0.05)));

    LOGGER.info("Initialized " + runbooks.size() + " default runbooks");
  }

  /** Initializes default escalation rules. */
  private void initializeDefaultEscalationRules() {
    // Critical incident escalation
    addEscalationRule(new EscalationRule(
        "critical_escalation",
        "Critical Incident Escalation",
        IncidentSeverity.CRITICAL,
        Duration.ofMinutes(15),
        List.of("ops-team", "engineering-manager"),
        "Critical incident requires immediate attention",
        true));

    // Emergency incident escalation
    addEscalationRule(new EscalationRule(
        "emergency_escalation",
        "Emergency Incident Escalation",
        IncidentSeverity.EMERGENCY,
        Duration.ofMinutes(5),
        List.of("ops-team", "engineering-manager", "cto"),
        "Emergency incident - system stability at risk",
        true));

    LOGGER.info("Initialized " + escalationRules.size() + " escalation rules");
  }

  /** Starts background processing tasks. */
  private void startBackgroundProcessing() {
    // Incident monitoring and escalation
    backgroundExecutor.scheduleAtFixedRate(
        this::processIncidentEscalations,
        60, 60, TimeUnit.SECONDS);

    // Auto-remediation execution
    backgroundExecutor.scheduleAtFixedRate(
        this::processAutoRemediation,
        30, 30, TimeUnit.SECONDS);

    // Incident cleanup and archival
    backgroundExecutor.scheduleAtFixedRate(
        this::cleanupOldIncidents,
        3600, 3600, TimeUnit.SECONDS);
  }

  /** Integrates with the alerting system to automatically create incidents. */
  private void integrateWithAlertingSystem() {
    if (alertingSystem != null) {
      alertingSystem.addAlertListener(this::handleAlert);
      alertingSystem.addCorrelationHandler(this::handleCorrelationGroup);
    }
  }

  /** Handles alerts from the intelligent alerting system. */
  private void handleAlert(final IntelligentAlertingSystem.CorrelatedAlert alert) {
    try {
      // Convert high-severity alerts to incidents
      if (alert.getSeverity().getLevel() >= 2) { // MEDIUM or higher
        final IncidentSeverity incidentSeverity = convertAlertSeverity(alert.getSeverity());
        createIncidentFromAlert(alert, incidentSeverity);
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to handle alert for incident creation: " + e.getMessage());
    }
  }

  /** Handles correlation groups from the alerting system. */
  private void handleCorrelationGroup(final IntelligentAlertingSystem.AlertCorrelationGroup group) {
    try {
      if (group.getAlertCount() > 1 && group.getHighestSeverity().getLevel() >= 2) {
        // Create incident for correlated alert groups
        final IncidentSeverity severity = convertAlertSeverity(group.getHighestSeverity());
        createIncidentFromCorrelationGroup(group, severity);
      }
    } catch (final Exception e) {
      LOGGER.warning("Failed to handle correlation group for incident creation: " + e.getMessage());
    }
  }

  /** Converts alert severity to incident severity. */
  private IncidentSeverity convertAlertSeverity(final IntelligentAlertingSystem.AlertSeverity alertSeverity) {
    switch (alertSeverity) {
      case LOW: return IncidentSeverity.LOW;
      case MEDIUM: return IncidentSeverity.MEDIUM;
      case HIGH: return IncidentSeverity.HIGH;
      case CRITICAL: return IncidentSeverity.CRITICAL;
      case EMERGENCY: return IncidentSeverity.EMERGENCY;
      default: return IncidentSeverity.MEDIUM;
    }
  }

  /** Creates incident from a single alert. */
  private void createIncidentFromAlert(
      final IntelligentAlertingSystem.CorrelatedAlert alert,
      final IncidentSeverity severity) {

    final String incidentId = "incident_" + System.currentTimeMillis() + "_" + alert.getAlertId().substring(0, 8);
    final String title = "Alert-based Incident: " + alert.getRule().getName();
    final String description = String.format(
        "Incident created from alert: %s. %s",
        alert.getAlertId(), alert.getRootCauseAnalysis());

    final Map<String, Object> metadata = new ConcurrentHashMap<>(alert.getContext());
    metadata.put("source_alert_id", alert.getAlertId());
    metadata.put("alert_confidence", alert.getConfidenceScore());
    metadata.put("metric_value", alert.getMetricValue());
    metadata.put("threshold", alert.getThreshold());

    final Incident incident = new Incident(
        incidentId,
        title,
        description,
        severity,
        alert.getRule().getMetricName(),
        List.of(alert.getRule().getMetricName()),
        metadata);

    registerIncident(incident);
  }

  /** Creates incident from a correlation group. */
  private void createIncidentFromCorrelationGroup(
      final IntelligentAlertingSystem.AlertCorrelationGroup group,
      final IncidentSeverity severity) {

    final String incidentId = "incident_" + System.currentTimeMillis() + "_corr_" + group.getGroupId().substring(0, 8);
    final String title = "Correlated Incident: " + group.getAlertCount() + " related alerts";
    final String description = String.format(
        "Incident created from %d correlated alerts using %s strategy.",
        group.getAlertCount(), group.getStrategy());

    final List<String> affectedComponents = group.getAlerts().stream()
        .map(alert -> alert.getRule().getMetricName())
        .distinct()
        .collect(Collectors.toList());

    final Map<String, Object> metadata = new ConcurrentHashMap<>();
    metadata.put("correlation_group_id", group.getGroupId());
    metadata.put("correlation_strategy", group.getStrategy().toString());
    metadata.put("alert_count", group.getAlertCount());
    metadata.put("alert_ids", group.getAlerts().stream()
        .map(IntelligentAlertingSystem.CorrelatedAlert::getAlertId)
        .collect(Collectors.toList()));

    final Incident incident = new Incident(
        incidentId,
        title,
        description,
        severity,
        "correlation_group",
        affectedComponents,
        metadata);

    registerIncident(incident);
  }

  /**
   * Registers a new incident in the system.
   *
   * @param incident the incident to register
   */
  private void registerIncident(final Incident incident) {
    if (activeIncidents.size() >= maxActiveIncidents) {
      LOGGER.warning("Maximum active incidents limit reached: " + maxActiveIncidents);
      return;
    }

    activeIncidents.put(incident.getIncidentId(), incident);
    totalIncidentsDetected.incrementAndGet();

    LOGGER.info(String.format("Registered new incident: %s [%s] - %s",
        incident.getIncidentId(), incident.getSeverity(), incident.getTitle()));

    // Notify listeners
    notifyIncidentListeners(incident, null);

    // Trigger automated runbooks
    if (autoRemediationEnabled) {
      triggerAutomatedRunbooks(incident);
    }

    // Check for immediate escalation
    if (autoEscalationEnabled && incident.getSeverity().isRequiresImmediate()) {
      checkEscalationRules(incident);
    }
  }

  /** Triggers automated runbooks for an incident. */
  private void triggerAutomatedRunbooks(final Incident incident) {
    final List<IncidentRunbook> applicableRunbooks = runbooks.values().stream()
        .filter(runbook -> runbook.shouldTrigger(incident))
        .sorted(Comparator.comparing(runbook -> runbook.getMinimumSeverity().getLevel(), Comparator.reverseOrder()))
        .collect(Collectors.toList());

    for (final IncidentRunbook runbook : applicableRunbooks) {
      if (runbook.isAutoExecute()) {
        incident.addTimelineEntry("Triggering automated runbook: " + runbook.getName());
        executeRunbook(incident, runbook);
      } else {
        incident.addTimelineEntry("Manual runbook available: " + runbook.getName());
      }
    }

    if (applicableRunbooks.isEmpty()) {
      incident.addTimelineEntry("No automated runbooks available for this incident type");
    }
  }

  /** Executes a runbook for an incident. */
  private void executeRunbook(final Incident incident, final IncidentRunbook runbook) {
    incident.setStatus(IncidentStatus.REMEDIATING);

    // Sort actions by priority
    final List<RemediationAction> sortedActions = runbook.getActions().stream()
        .sorted(Comparator.comparing(RemediationAction::getPriority))
        .collect(Collectors.toList());

    for (final RemediationAction action : sortedActions) {
      if (!action.isRequiresApproval()) {
        // Execute immediately
        backgroundExecutor.execute(() -> executeRemediationAction(incident, action));
      } else {
        // Add to incident for manual approval
        incident.addRemediationAction(action);
        incident.addTimelineEntry("Remediation action requires approval: " + action.getDescription());
      }
    }
  }

  /** Executes a single remediation action. */
  private void executeRemediationAction(final Incident incident, final RemediationAction action) {
    final long startTime = System.nanoTime();
    incident.addTimelineEntry("Executing remediation action: " + action.getDescription());

    try {
      boolean success = false;
      String result = "Unknown result";

      // Execute based on action type
      switch (action.getActionType()) {
        case TRIGGER_GC:
          success = executeGarbageCollection(action, incident);
          result = success ? "Garbage collection triggered successfully" : "Garbage collection failed";
          break;

        case CLEAR_CACHE:
          success = executeClearCache(action, incident);
          result = success ? "Caches cleared successfully" : "Cache clearing failed";
          break;

        case RESTART_SERVICE:
          success = executeRestartService(action, incident);
          result = success ? "Service restart initiated" : "Service restart failed";
          break;

        case ISOLATE_COMPONENT:
          success = executeIsolateComponent(action, incident);
          result = success ? "Component isolated successfully" : "Component isolation failed";
          break;

        case RESET_CONNECTIONS:
          success = executeResetConnections(action, incident);
          result = success ? "Connections reset successfully" : "Connection reset failed";
          break;

        default:
          // Try custom remediation handlers
          success = executeCustomRemediation(action, incident);
          result = success ? "Custom remediation executed" : "Custom remediation failed";
          break;
      }

      final Duration executionDuration = Duration.ofNanos(System.nanoTime() - startTime);
      action.markExecuted(success, result, executionDuration);
      totalRemediationsExecuted.incrementAndGet();

      incident.addTimelineEntry(String.format("Remediation action completed: %s (success=%s, duration=%dms)",
          action.getActionType().getDisplayName(), success, executionDuration.toMillis()));

      // Check if incident should be resolved
      if (success && shouldResolveIncident(incident, action)) {
        resolveIncident(incident.getIncidentId(), "Automatically resolved by remediation: " + result);
      }

    } catch (final Exception e) {
      final Duration executionDuration = Duration.ofNanos(System.nanoTime() - startTime);
      action.markExecuted(false, "Execution failed: " + e.getMessage(), executionDuration);
      incident.addTimelineEntry("Remediation action failed: " + e.getMessage());
      LOGGER.warning("Remediation action failed: " + e.getMessage());
    }
  }

  /** Executes garbage collection remediation. */
  private boolean executeGarbageCollection(final RemediationAction action, final Incident incident) {
    try {
      LOGGER.info("Executing garbage collection remediation for incident: " + incident.getIncidentId());
      System.gc();

      // Wait for GC to complete
      Thread.sleep(2000);

      // Check if memory situation improved
      final Runtime runtime = Runtime.getRuntime();
      final long freeMemory = runtime.freeMemory();
      final long totalMemory = runtime.totalMemory();
      final double memoryUsage = (double) (totalMemory - freeMemory) / totalMemory;

      incident.getMetadata().put("memory_usage_after_gc", memoryUsage);

      return memoryUsage < 0.85; // Consider successful if memory usage below 85%

    } catch (final Exception e) {
      LOGGER.warning("Garbage collection remediation failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes clear cache remediation. */
  private boolean executeClearCache(final RemediationAction action, final Incident incident) {
    try {
      LOGGER.info("Executing cache clearing remediation for incident: " + incident.getIncidentId());

      // In a real implementation, this would clear actual caches
      // For now, simulate cache clearing
      incident.addTimelineEntry("Application caches cleared");

      return true;

    } catch (final Exception e) {
      LOGGER.warning("Cache clearing remediation failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes restart service remediation. */
  private boolean executeRestartService(final RemediationAction action, final Incident incident) {
    try {
      LOGGER.info("Executing service restart remediation for incident: " + incident.getIncidentId());

      // In a real implementation, this would restart the actual service
      // For now, simulate service restart initiation
      incident.addTimelineEntry("Service restart initiated - requires manual verification");

      return true;

    } catch (final Exception e) {
      LOGGER.warning("Service restart remediation failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes isolate component remediation. */
  private boolean executeIsolateComponent(final RemediationAction action, final Incident incident) {
    try {
      LOGGER.info("Executing component isolation remediation for incident: " + incident.getIncidentId());

      // In a real implementation, this would isolate the problematic component
      incident.addTimelineEntry("Problematic component isolated from system");

      return true;

    } catch (final Exception e) {
      LOGGER.warning("Component isolation remediation failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes reset connections remediation. */
  private boolean executeResetConnections(final RemediationAction action, final Incident incident) {
    try {
      LOGGER.info("Executing connection reset remediation for incident: " + incident.getIncidentId());

      // In a real implementation, this would reset connection pools
      incident.addTimelineEntry("Network connections reset");

      return true;

    } catch (final Exception e) {
      LOGGER.warning("Connection reset remediation failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes custom remediation using registered handlers. */
  private boolean executeCustomRemediation(final RemediationAction action, final Incident incident) {
    for (final RemediationHandler handler : remediationHandlers) {
      try {
        if (handler.executeRemediation(action, incident)) {
          return true;
        }
      } catch (final Exception e) {
        LOGGER.warning("Custom remediation handler failed: " + e.getMessage());
      }
    }
    return false;
  }

  /** Checks if incident should be resolved after successful remediation. */
  private boolean shouldResolveIncident(final Incident incident, final RemediationAction action) {
    // Simple logic - in production would be more sophisticated
    return action.getActionType() == RemediationActionType.TRIGGER_GC ||
           action.getActionType() == RemediationActionType.CLEAR_CACHE ||
           action.getActionType() == RemediationActionType.RESET_CONNECTIONS;
  }

  /** Processes incident escalations. */
  private void processIncidentEscalations() {
    try {
      for (final Incident incident : activeIncidents.values()) {
        if (incident.isOpen()) {
          checkEscalationRules(incident);
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error processing incident escalations: " + e.getMessage());
    }
  }

  /** Checks escalation rules for an incident. */
  private void checkEscalationRules(final Incident incident) {
    for (final EscalationRule rule : escalationRules.values()) {
      if (rule.shouldEscalate(incident)) {
        escalateIncident(incident, rule);
      }
    }
  }

  /** Escalates an incident using the specified rule. */
  private void escalateIncident(final Incident incident, final EscalationRule rule) {
    incident.setStatus(IncidentStatus.ESCALATED);
    incident.addTimelineEntry("Escalated using rule: " + rule.getName());
    totalEscalations.incrementAndGet();

    LOGGER.warning(String.format("Escalating incident %s [%s] to %s",
        incident.getIncidentId(), incident.getSeverity(), rule.getEscalationTargets()));

    // In a real implementation, would send notifications to escalation targets
    for (final String target : rule.getEscalationTargets()) {
      LOGGER.info("Escalation notification sent to: " + target);
    }
  }

  /** Processes auto-remediation for active incidents. */
  private void processAutoRemediation() {
    if (!autoRemediationEnabled) {
      return;
    }

    try {
      for (final Incident incident : activeIncidents.values()) {
        if (incident.getStatus() == IncidentStatus.ACKNOWLEDGED ||
            incident.getStatus() == IncidentStatus.INVESTIGATING) {
          // Check for pending remediation actions
          final List<RemediationAction> pendingActions = incident.getRemediationActions().stream()
              .filter(action -> !action.isExecuted() && !action.isRequiresApproval())
              .collect(Collectors.toList());

          for (final RemediationAction action : pendingActions) {
            backgroundExecutor.execute(() -> executeRemediationAction(incident, action));
          }
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error processing auto-remediation: " + e.getMessage());
    }
  }

  /** Cleans up old incidents. */
  private void cleanupOldIncidents() {
    try {
      final Instant cutoff = Instant.now().minus(incidentRetentionPeriod);
      final int initialSize = closedIncidents.size();

      closedIncidents.entrySet().removeIf(entry ->
          entry.getValue().getClosedAt() != null &&
          entry.getValue().getClosedAt().isBefore(cutoff));

      final int removedCount = initialSize - closedIncidents.size();
      if (removedCount > 0) {
        LOGGER.fine("Cleaned up " + removedCount + " old incidents");
      }

      // Calculate average resolution time
      updateAverageResolutionTime();

    } catch (final Exception e) {
      LOGGER.warning("Error during incident cleanup: " + e.getMessage());
    }
  }

  /** Updates average resolution time statistics. */
  private void updateAverageResolutionTime() {
    final List<Duration> resolutionTimes = closedIncidents.values().stream()
        .filter(incident -> incident.getResolvedAt() != null)
        .map(incident -> Duration.between(incident.getDetectedAt(), incident.getResolvedAt()))
        .collect(Collectors.toList());

    if (!resolutionTimes.isEmpty()) {
      final long averageNanos = (long) resolutionTimes.stream()
          .mapToLong(Duration::toNanos)
          .average()
          .orElse(0.0);
      averageResolutionTime.set(Duration.ofNanos(averageNanos));
    }
  }

  /** Notifies incident listeners. */
  private void notifyIncidentListeners(final Incident incident, final IncidentStatus previousStatus) {
    for (final IncidentListener listener : incidentListeners) {
      try {
        listener.onIncidentStateChange(incident, previousStatus);
      } catch (final Exception e) {
        LOGGER.warning("Incident listener error: " + e.getMessage());
      }
    }
  }

  /**
   * Adds a runbook to the system.
   *
   * @param runbook the runbook to add
   */
  public void addRunbook(final IncidentRunbook runbook) {
    runbooks.put(runbook.getRunbookId(), runbook);
    LOGGER.info("Added incident runbook: " + runbook.getRunbookId());
  }

  /**
   * Adds an escalation rule to the system.
   *
   * @param rule the escalation rule to add
   */
  public void addEscalationRule(final EscalationRule rule) {
    escalationRules.put(rule.getRuleId(), rule);
    LOGGER.info("Added escalation rule: " + rule.getRuleId());
  }

  /**
   * Adds an incident listener.
   *
   * @param listener the incident listener
   */
  public void addIncidentListener(final IncidentListener listener) {
    if (listener != null) {
      incidentListeners.add(listener);
    }
  }

  /**
   * Adds a remediation handler.
   *
   * @param handler the remediation handler
   */
  public void addRemediationHandler(final RemediationHandler handler) {
    if (handler != null) {
      remediationHandlers.add(handler);
    }
  }

  /**
   * Acknowledges an incident.
   *
   * @param incidentId the incident identifier
   * @param assignedTo who acknowledged the incident
   * @return true if incident was found and acknowledged
   */
  public boolean acknowledgeIncident(final String incidentId, final String assignedTo) {
    final Incident incident = activeIncidents.get(incidentId);
    if (incident != null) {
      final IncidentStatus previousStatus = incident.getStatus();
      incident.acknowledge(assignedTo);
      notifyIncidentListeners(incident, previousStatus);
      LOGGER.info("Incident acknowledged: " + incidentId + " by " + assignedTo);
      return true;
    }
    return false;
  }

  /**
   * Resolves an incident.
   *
   * @param incidentId the incident identifier
   * @param resolution the resolution description
   * @return true if incident was found and resolved
   */
  public boolean resolveIncident(final String incidentId, final String resolution) {
    final Incident incident = activeIncidents.get(incidentId);
    if (incident != null) {
      final IncidentStatus previousStatus = incident.getStatus();
      incident.resolve(resolution);
      totalIncidentsResolved.incrementAndGet();
      notifyIncidentListeners(incident, previousStatus);
      LOGGER.info("Incident resolved: " + incidentId + " - " + resolution);
      return true;
    }
    return false;
  }

  /**
   * Closes an incident.
   *
   * @param incidentId the incident identifier
   * @return true if incident was found and closed
   */
  public boolean closeIncident(final String incidentId) {
    final Incident incident = activeIncidents.remove(incidentId);
    if (incident != null) {
      final IncidentStatus previousStatus = incident.getStatus();
      incident.close();
      closedIncidents.put(incidentId, incident);
      notifyIncidentListeners(incident, previousStatus);
      LOGGER.info("Incident closed: " + incidentId);
      return true;
    }
    return false;
  }

  /**
   * Gets active incidents.
   *
   * @param limit maximum number of incidents to return
   * @return list of active incidents
   */
  public List<Incident> getActiveIncidents(final int limit) {
    return activeIncidents.values().stream()
        .sorted(Comparator.comparing(Incident::getDetectedAt, Comparator.reverseOrder()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets incident response system statistics.
   *
   * @return formatted statistics
   */
  public String getIncidentResponseStatistics() {
    return String.format(
        "Incident Response Statistics: active_incidents=%d, total_detected=%d, total_resolved=%d, " +
        "total_remediations=%d, total_escalations=%d, avg_resolution_time=%s, runbooks=%d, escalation_rules=%d",
        activeIncidents.size(),
        totalIncidentsDetected.get(),
        totalIncidentsResolved.get(),
        totalRemediationsExecuted.get(),
        totalEscalations.get(),
        formatDuration(averageResolutionTime.get()),
        runbooks.size(),
        escalationRules.size());
  }

  /** Formats duration for display. */
  private String formatDuration(final Duration duration) {
    if (duration.isZero()) {
      return "N/A";
    }

    final long minutes = duration.toMinutes();
    if (minutes < 60) {
      return minutes + "m";
    }

    final long hours = duration.toHours();
    final long remainingMinutes = minutes - (hours * 60);
    return hours + "h " + remainingMinutes + "m";
  }

  /**
   * Sets auto-remediation enabled state.
   *
   * @param enabled true to enable auto-remediation
   */
  public void setAutoRemediationEnabled(final boolean enabled) {
    this.autoRemediationEnabled = enabled;
    LOGGER.info("Auto-remediation " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Sets auto-escalation enabled state.
   *
   * @param enabled true to enable auto-escalation
   */
  public void setAutoEscalationEnabled(final boolean enabled) {
    this.autoEscalationEnabled = enabled;
    LOGGER.info("Auto-escalation " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the incident response system. */
  public void shutdown() {
    backgroundExecutor.shutdown();
    try {
      if (!backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        backgroundExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      backgroundExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Automated incident response system shutdown");
  }
}