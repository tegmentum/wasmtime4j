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
 * Comprehensive resource observability and monitoring system providing dashboards, performance
 * metrics, alerts, trend analysis, forecasting, and health diagnostics.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Real-time resource usage dashboards and visualization
 *   <li>Performance metrics collection and analysis
 *   <li>Intelligent alerting and threshold monitoring
 *   <li>Trend analysis and forecasting capabilities
 *   <li>Health checks and diagnostic monitoring
 *   <li>Custom metrics and extensible monitoring framework
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceObservabilityManager {

  private static final Logger LOGGER =
      Logger.getLogger(ResourceObservabilityManager.class.getName());

  /** Resource metric definition. */
  public static final class ResourceMetric {
    private final String metricId;
    private final String name;
    private final String description;
    private final MetricType type;
    private final String unit;
    private final Map<String, String> labels;
    private final Instant timestamp;
    private final double value;
    private final MetricSource source;

    /**
     * Creates a new ResourceMetric.
     *
     * @param metricId the unique metric identifier
     * @param name the metric name
     * @param description the metric description
     * @param type the metric type
     * @param unit the metric unit
     * @param labels the metric labels
     * @param value the metric value
     * @param source the metric source
     */
    public ResourceMetric(
        final String metricId,
        final String name,
        final String description,
        final MetricType type,
        final String unit,
        final Map<String, String> labels,
        final double value,
        final MetricSource source) {
      this.metricId = metricId;
      this.name = name;
      this.description = description;
      this.type = type;
      this.unit = unit;
      this.labels = Map.copyOf(labels != null ? labels : Map.of());
      this.timestamp = Instant.now();
      this.value = value;
      this.source = source;
    }

    // Getters
    public String getMetricId() {
      return metricId;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public MetricType getType() {
      return type;
    }

    public String getUnit() {
      return unit;
    }

    public Map<String, String> getLabels() {
      return labels;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public double getValue() {
      return value;
    }

    public MetricSource getSource() {
      return source;
    }
  }

  /** Metric types for observability. */
  public enum MetricType {
    GAUGE, // Current value
    COUNTER, // Monotonically increasing
    HISTOGRAM, // Distribution of values
    SUMMARY, // Summary statistics
    RATE, // Rate of change
    RATIO // Ratio between values
  }

  /** Metric sources. */
  public enum MetricSource {
    QUOTA_MANAGER, // From quota management
    SCHEDULER, // From resource scheduler
    POOL_MANAGER, // From pool management
    OPTIMIZATION_ENGINE, // From optimization engine
    SECURITY_MANAGER, // From security management
    SYSTEM, // System metrics
    APPLICATION, // Application metrics
    CUSTOM // Custom metrics
  }

  /** Alert definition and configuration. */
  public static final class AlertRule {
    private final String alertId;
    private final String name;
    private final String description;
    private final String metricQuery;
    private final AlertCondition condition;
    private final double threshold;
    private final Duration evaluationWindow;
    private final AlertSeverity severity;
    private final List<String> notificationChannels;
    private final Map<String, Object> metadata;
    private final boolean enabled;

    private AlertRule(final Builder builder) {
      this.alertId = builder.alertId;
      this.name = builder.name;
      this.description = builder.description;
      this.metricQuery = builder.metricQuery;
      this.condition = builder.condition;
      this.threshold = builder.threshold;
      this.evaluationWindow = builder.evaluationWindow;
      this.severity = builder.severity;
      this.notificationChannels = List.copyOf(builder.notificationChannels);
      this.metadata = Map.copyOf(builder.metadata);
      this.enabled = builder.enabled;
    }

    public static Builder builder(final String alertId, final String name) {
      return new Builder(alertId, name);
    }

    /** Builder for creating custom alert configurations. */
    public static final class Builder {
      private final String alertId;
      private final String name;
      private String description = "";
      private String metricQuery = "";
      private AlertCondition condition = AlertCondition.GREATER_THAN;
      private double threshold = 0.0;
      private Duration evaluationWindow = Duration.ofMinutes(5);
      private AlertSeverity severity = AlertSeverity.WARNING;
      private List<String> notificationChannels = new ArrayList<>();
      private Map<String, Object> metadata = new ConcurrentHashMap<>();
      private boolean enabled = true;

      private Builder(final String alertId, final String name) {
        this.alertId = alertId;
        this.name = name;
      }

      public Builder withDescription(final String description) {
        this.description = description;
        return this;
      }

      public Builder withMetricQuery(final String metricQuery) {
        this.metricQuery = metricQuery;
        return this;
      }

      public Builder withCondition(final AlertCondition condition) {
        this.condition = condition;
        return this;
      }

      public Builder withThreshold(final double threshold) {
        this.threshold = threshold;
        return this;
      }

      public Builder withEvaluationWindow(final Duration evaluationWindow) {
        this.evaluationWindow = evaluationWindow;
        return this;
      }

      public Builder withSeverity(final AlertSeverity severity) {
        this.severity = severity;
        return this;
      }

      public Builder withNotificationChannel(final String channel) {
        this.notificationChannels.add(channel);
        return this;
      }

      public Builder withMetadata(final String key, final Object value) {
        this.metadata.put(key, value);
        return this;
      }

      public Builder withEnabled(final boolean enabled) {
        this.enabled = enabled;
        return this;
      }

      public AlertRule build() {
        return new AlertRule(this);
      }
    }

    // Getters
    public String getAlertId() {
      return alertId;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getMetricQuery() {
      return metricQuery;
    }

    public AlertCondition getCondition() {
      return condition;
    }

    public double getThreshold() {
      return threshold;
    }

    public Duration getEvaluationWindow() {
      return evaluationWindow;
    }

    public AlertSeverity getSeverity() {
      return severity;
    }

    public List<String> getNotificationChannels() {
      return notificationChannels;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /** Alert conditions. */
  public enum AlertCondition {
    GREATER_THAN,
    LESS_THAN,
    EQUAL_TO,
    NOT_EQUAL_TO,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN_OR_EQUAL,
    CHANGE_RATE_ABOVE,
    CHANGE_RATE_BELOW
  }

  /** Alert severity levels. */
  public enum AlertSeverity {
    CRITICAL,
    WARNING,
    INFO
  }

  /** Alert instance representing a fired alert. */
  public static final class AlertInstance {
    private final String instanceId;
    private final AlertRule rule;
    private final double actualValue;
    private final String description;
    private final Instant firedAt;
    private final Map<String, Object> context;
    private volatile Instant resolvedAt;
    private volatile AlertStatus status = AlertStatus.FIRING;

    /**
     * Creates a new AlertInstance.
     *
     * @param instanceId the unique instance identifier
     * @param rule the alert rule
     * @param actualValue the actual metric value
     * @param description the alert description
     * @param context additional context information
     */
    public AlertInstance(
        final String instanceId,
        final AlertRule rule,
        final double actualValue,
        final String description,
        final Map<String, Object> context) {
      this.instanceId = instanceId;
      this.rule = rule;
      this.actualValue = actualValue;
      this.description = description;
      this.firedAt = Instant.now();
      this.context = Map.copyOf(context != null ? context : Map.of());
    }

    // Getters
    public String getInstanceId() {
      return instanceId;
    }

    public AlertRule getRule() {
      return rule;
    }

    public double getActualValue() {
      return actualValue;
    }

    public String getDescription() {
      return description;
    }

    public Instant getFiredAt() {
      return firedAt;
    }

    public Instant getResolvedAt() {
      return resolvedAt;
    }

    public Map<String, Object> getContext() {
      return context;
    }

    public AlertStatus getStatus() {
      return status;
    }

    public Duration getDuration() {
      final Instant end = resolvedAt != null ? resolvedAt : Instant.now();
      return Duration.between(firedAt, end);
    }

    void resolve() {
      this.status = AlertStatus.RESOLVED;
      this.resolvedAt = Instant.now();
    }
  }

  /** Alert status. */
  public enum AlertStatus {
    FIRING, // Alert is currently active
    RESOLVED // Alert has been resolved
  }

  /** Dashboard widget definition. */
  public static final class DashboardWidget {
    private final String widgetId;
    private final String title;
    private final WidgetType type;
    private final String dataQuery;
    private final Map<String, Object> configuration;
    private final int refreshInterval; // seconds
    private final boolean enabled;

    /**
     * Creates a new DashboardWidget.
     *
     * @param widgetId the unique widget identifier
     * @param title the widget title
     * @param type the widget type
     * @param dataQuery the data query for the widget
     * @param configuration the widget configuration
     * @param refreshInterval the refresh interval in seconds
     */
    public DashboardWidget(
        final String widgetId,
        final String title,
        final WidgetType type,
        final String dataQuery,
        final Map<String, Object> configuration,
        final int refreshInterval) {
      this.widgetId = widgetId;
      this.title = title;
      this.type = type;
      this.dataQuery = dataQuery;
      this.configuration = Map.copyOf(configuration != null ? configuration : Map.of());
      this.refreshInterval = refreshInterval;
      this.enabled = true;
    }

    // Getters
    public String getWidgetId() {
      return widgetId;
    }

    public String getTitle() {
      return title;
    }

    public WidgetType getType() {
      return type;
    }

    public String getDataQuery() {
      return dataQuery;
    }

    public Map<String, Object> getConfiguration() {
      return configuration;
    }

    public int getRefreshInterval() {
      return refreshInterval;
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /** Dashboard widget types. */
  public enum WidgetType {
    LINE_CHART, // Time series line chart
    BAR_CHART, // Bar chart
    PIE_CHART, // Pie chart
    GAUGE, // Gauge meter
    TABLE, // Data table
    STAT, // Single statistic
    HEATMAP, // Heat map
    HISTOGRAM // Histogram
  }

  /** Health check definition. */
  public static final class HealthCheck {
    private final String checkId;
    private final String name;
    private final String description;
    private final HealthChecker checker;
    private final Duration interval;
    private final Duration timeout;
    private final boolean critical;
    private volatile HealthStatus lastStatus = HealthStatus.UNKNOWN;
    private volatile Instant lastCheck = Instant.now();
    private volatile String lastMessage = "";

    /**
     * Creates a new HealthCheck.
     *
     * @param checkId the unique check identifier
     * @param name the check name
     * @param description the check description
     * @param checker the health checker implementation
     * @param interval the check interval
     * @param timeout the check timeout
     * @param critical whether this is a critical check
     */
    public HealthCheck(
        final String checkId,
        final String name,
        final String description,
        final HealthChecker checker,
        final Duration interval,
        final Duration timeout,
        final boolean critical) {
      this.checkId = checkId;
      this.name = name;
      this.description = description;
      this.checker = checker;
      this.interval = interval;
      this.timeout = timeout;
      this.critical = critical;
    }

    // Getters
    public String getCheckId() {
      return checkId;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public HealthChecker getChecker() {
      return checker;
    }

    public Duration getInterval() {
      return interval;
    }

    public Duration getTimeout() {
      return timeout;
    }

    public boolean isCritical() {
      return critical;
    }

    public HealthStatus getLastStatus() {
      return lastStatus;
    }

    public Instant getLastCheck() {
      return lastCheck;
    }

    public String getLastMessage() {
      return lastMessage;
    }

    void updateStatus(final HealthStatus status, final String message) {
      this.lastStatus = status;
      this.lastCheck = Instant.now();
      this.lastMessage = message;
    }
  }

  /** Health check functional interface. */
  @FunctionalInterface
  public interface HealthChecker {
    HealthCheckResult check() throws Exception;
  }

  /** Health check result. */
  public static final class HealthCheckResult {
    private final HealthStatus status;
    private final String message;
    private final Map<String, Object> details;

    /**
     * Creates a new HealthCheckResult.
     *
     * @param status the health status
     * @param message the status message
     * @param details additional details
     */
    public HealthCheckResult(
        final HealthStatus status, final String message, final Map<String, Object> details) {
      this.status = status;
      this.message = message;
      this.details = Map.copyOf(details != null ? details : Map.of());
    }

    public static HealthCheckResult healthy(final String message) {
      return new HealthCheckResult(HealthStatus.HEALTHY, message, Map.of());
    }

    public static HealthCheckResult unhealthy(final String message) {
      return new HealthCheckResult(HealthStatus.UNHEALTHY, message, Map.of());
    }

    public static HealthCheckResult degraded(final String message) {
      return new HealthCheckResult(HealthStatus.DEGRADED, message, Map.of());
    }

    // Getters
    public HealthStatus getStatus() {
      return status;
    }

    public String getMessage() {
      return message;
    }

    public Map<String, Object> getDetails() {
      return details;
    }
  }

  /** Health status values. */
  public enum HealthStatus {
    HEALTHY, // System is healthy
    DEGRADED, // System has issues but is functional
    UNHEALTHY, // System has critical issues
    UNKNOWN // Status cannot be determined
  }

  /** Performance trend analysis. */
  public static final class TrendAnalysis {
    private final String metricName;
    private final Duration analysisWindow;
    private final TrendDirection direction;
    private final double slope;
    private final double confidence;
    private final List<Double> dataPoints;
    private final Instant analysisTime;

    /**
     * Creates a new TrendAnalysis.
     *
     * @param metricName the metric name
     * @param analysisWindow the analysis window
     * @param direction the trend direction
     * @param slope the trend slope
     * @param confidence the confidence level
     * @param dataPoints the data points used in analysis
     */
    public TrendAnalysis(
        final String metricName,
        final Duration analysisWindow,
        final TrendDirection direction,
        final double slope,
        final double confidence,
        final List<Double> dataPoints) {
      this.metricName = metricName;
      this.analysisWindow = analysisWindow;
      this.direction = direction;
      this.slope = slope;
      this.confidence = confidence;
      this.dataPoints = List.copyOf(dataPoints);
      this.analysisTime = Instant.now();
    }

    // Getters
    public String getMetricName() {
      return metricName;
    }

    public Duration getAnalysisWindow() {
      return analysisWindow;
    }

    public TrendDirection getDirection() {
      return direction;
    }

    public double getSlope() {
      return slope;
    }

    public double getConfidence() {
      return confidence;
    }

    public List<Double> getDataPoints() {
      return dataPoints;
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }
  }

  /** Trend directions. */
  public enum TrendDirection {
    INCREASING,
    DECREASING,
    STABLE,
    VOLATILE
  }

  // Instance fields
  private final ConcurrentHashMap<String, List<ResourceMetric>> metricHistory =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AlertRule> alertRules = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AlertInstance> activeAlerts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, DashboardWidget> widgets = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, HealthCheck> healthChecks = new ConcurrentHashMap<>();
  private final CopyOnWriteArrayList<AlertListener> alertListeners = new CopyOnWriteArrayList<>();

  private final ScheduledExecutorService observabilityExecutor =
      Executors.newScheduledThreadPool(4);
  private final AtomicLong totalMetrics = new AtomicLong(0);
  private final AtomicLong totalAlerts = new AtomicLong(0);
  private final AtomicLong activeAlertCount = new AtomicLong(0);
  private final AtomicReference<Instant> lastHealthCheck = new AtomicReference<>(Instant.now());

  private volatile boolean enabled = true;
  private volatile Duration metricRetention = Duration.ofHours(24);
  private volatile int maxMetricsPerSeries = 10000;

  /** Alert listener interface. */
  public interface AlertListener {
    void onAlertFired(AlertInstance alert);

    void onAlertResolved(AlertInstance alert);
  }

  /** Creates a new ResourceObservabilityManager. */
  public ResourceObservabilityManager() {
    initializeDefaultAlerts();
    initializeDefaultHealthChecks();
    initializeDefaultDashboard();
    startObservabilityTasks();
    LOGGER.info("Resource observability manager initialized");
  }

  /**
   * Records a resource metric.
   *
   * @param metric the resource metric to record
   */
  public void recordMetric(final ResourceMetric metric) {
    if (!enabled) {
      return;
    }

    final String seriesKey = createSeriesKey(metric.getName(), metric.getLabels());
    final List<ResourceMetric> series =
        metricHistory.computeIfAbsent(seriesKey, k -> new ArrayList<>());

    synchronized (series) {
      series.add(metric);
      // Maintain size limit
      if (series.size() > maxMetricsPerSeries) {
        series.remove(0);
      }
    }

    totalMetrics.incrementAndGet();

    LOGGER.fine(
        String.format(
            "Recorded metric: %s = %f %s", metric.getName(), metric.getValue(), metric.getUnit()));
  }

  /**
   * Creates and registers an alert rule.
   *
   * @param alertRule the alert rule to register
   */
  public void addAlertRule(final AlertRule alertRule) {
    alertRules.put(alertRule.getAlertId(), alertRule);
    LOGGER.info(
        String.format("Added alert rule: %s - %s", alertRule.getAlertId(), alertRule.getName()));
  }

  /**
   * Adds an alert listener.
   *
   * @param listener the alert listener
   */
  public void addAlertListener(final AlertListener listener) {
    alertListeners.add(listener);
  }

  /**
   * Creates and registers a dashboard widget.
   *
   * @param widget the dashboard widget
   */
  public void addDashboardWidget(final DashboardWidget widget) {
    widgets.put(widget.getWidgetId(), widget);
    LOGGER.info(
        String.format("Added dashboard widget: %s - %s", widget.getWidgetId(), widget.getTitle()));
  }

  /**
   * Adds a health check.
   *
   * @param healthCheck the health check to add
   */
  public void addHealthCheck(final HealthCheck healthCheck) {
    healthChecks.put(healthCheck.getCheckId(), healthCheck);
    LOGGER.info(
        String.format(
            "Added health check: %s - %s", healthCheck.getCheckId(), healthCheck.getName()));
  }

  /**
   * Gets metrics for a specific metric name and time range.
   *
   * @param metricName metric name
   * @param labels metric labels (can be null for all)
   * @param from start time
   * @param to end time
   * @return list of matching metrics
   */
  public List<ResourceMetric> getMetrics(
      final String metricName,
      final Map<String, String> labels,
      final Instant from,
      final Instant to) {
    final String seriesKey = createSeriesKey(metricName, labels);
    final List<ResourceMetric> series = metricHistory.get(seriesKey);

    if (series == null) {
      return List.of();
    }

    synchronized (series) {
      return series.stream()
          .filter(
              metric -> metric.getTimestamp().isAfter(from) && metric.getTimestamp().isBefore(to))
          .collect(Collectors.toList());
    }
  }

  /**
   * Gets current active alerts.
   *
   * @return list of active alerts
   */
  public List<AlertInstance> getActiveAlerts() {
    return activeAlerts.values().stream()
        .filter(alert -> alert.getStatus() == AlertStatus.FIRING)
        .sorted(Comparator.comparing(AlertInstance::getFiredAt).reversed())
        .collect(Collectors.toList());
  }

  /**
   * Gets all alerts (active and resolved) for a time period.
   *
   * @param from start time
   * @param to end time
   * @return list of alerts
   */
  public List<AlertInstance> getAlerts(final Instant from, final Instant to) {
    return activeAlerts.values().stream()
        .filter(alert -> alert.getFiredAt().isAfter(from) && alert.getFiredAt().isBefore(to))
        .sorted(Comparator.comparing(AlertInstance::getFiredAt).reversed())
        .collect(Collectors.toList());
  }

  /**
   * Gets current health status of all checks.
   *
   * @return overall health status
   */
  public HealthStatus getOverallHealth() {
    final List<HealthCheck> criticalChecks =
        healthChecks.values().stream().filter(HealthCheck::isCritical).collect(Collectors.toList());

    if (criticalChecks.isEmpty()) {
      return HealthStatus.HEALTHY;
    }

    final boolean hasUnhealthy =
        criticalChecks.stream().anyMatch(check -> check.getLastStatus() == HealthStatus.UNHEALTHY);
    if (hasUnhealthy) {
      return HealthStatus.UNHEALTHY;
    }

    final boolean hasDegraded =
        criticalChecks.stream().anyMatch(check -> check.getLastStatus() == HealthStatus.DEGRADED);
    if (hasDegraded) {
      return HealthStatus.DEGRADED;
    }

    return HealthStatus.HEALTHY;
  }

  /**
   * Performs trend analysis on a metric.
   *
   * @param metricName metric name
   * @param labels metric labels
   * @param analysisWindow time window for analysis
   * @return trend analysis result
   */
  public TrendAnalysis analyzeTrend(
      final String metricName, final Map<String, String> labels, final Duration analysisWindow) {
    final Instant from = Instant.now().minus(analysisWindow);
    final Instant to = Instant.now();
    final List<ResourceMetric> metrics = getMetrics(metricName, labels, from, to);

    if (metrics.size() < 3) {
      return new TrendAnalysis(
          metricName, analysisWindow, TrendDirection.STABLE, 0.0, 0.0, List.of());
    }

    final List<Double> values =
        metrics.stream()
            .sorted(Comparator.comparing(ResourceMetric::getTimestamp))
            .map(ResourceMetric::getValue)
            .collect(Collectors.toList());

    final double slope = calculateSlope(values);
    final double confidence = calculateTrendConfidence(values);
    final TrendDirection direction = determineTrendDirection(slope, values);

    return new TrendAnalysis(metricName, analysisWindow, direction, slope, confidence, values);
  }

  /**
   * Gets comprehensive observability statistics.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Observability Statistics ===\n");

    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Metric retention: %s\n", metricRetention));
    sb.append(String.format("Max metrics per series: %,d\n", maxMetricsPerSeries));
    sb.append("\n");

    sb.append(String.format("Total metrics recorded: %,d\n", totalMetrics.get()));
    sb.append(String.format("Metric series: %d\n", metricHistory.size()));
    sb.append(String.format("Alert rules: %d\n", alertRules.size()));
    sb.append(String.format("Active alerts: %,d\n", activeAlertCount.get()));
    sb.append(String.format("Total alerts fired: %,d\n", totalAlerts.get()));
    sb.append(String.format("Dashboard widgets: %d\n", widgets.size()));
    sb.append(String.format("Health checks: %d\n", healthChecks.size()));
    sb.append(String.format("Overall health: %s\n", getOverallHealth()));
    sb.append(String.format("Last health check: %s\n", lastHealthCheck.get()));
    sb.append("\n");

    sb.append("Active Alerts:\n");
    getActiveAlerts().stream()
        .limit(5)
        .forEach(
            alert ->
                sb.append(
                    String.format(
                        "  %s: %s (%.2f)\n",
                        alert.getRule().getSeverity(),
                        alert.getRule().getName(),
                        alert.getActualValue())));

    sb.append("\nHealth Checks:\n");
    healthChecks
        .values()
        .forEach(
            check ->
                sb.append(
                    String.format(
                        "  %s: %s (%s)\n",
                        check.getName(), check.getLastStatus(), check.getLastMessage())));

    return sb.toString();
  }

  /**
   * Exports dashboard data for visualization.
   *
   * @param widgetId widget identifier
   * @return dashboard data
   */
  public Map<String, Object> getDashboardData(final String widgetId) {
    final DashboardWidget widget = widgets.get(widgetId);
    if (widget == null || !widget.isEnabled()) {
      return Map.of();
    }

    // Simplified data query execution
    final Map<String, Object> data = new ConcurrentHashMap<>();
    data.put("widget_id", widgetId);
    data.put("title", widget.getTitle());
    data.put("type", widget.getType());
    data.put("timestamp", Instant.now());

    // Execute data query (simplified)
    final List<ResourceMetric> queryResults = executeDataQuery(widget.getDataQuery());
    data.put("data", queryResults);
    data.put("count", queryResults.size());

    return data;
  }

  private String createSeriesKey(final String metricName, final Map<String, String> labels) {
    if (labels == null || labels.isEmpty()) {
      return metricName;
    }

    final StringBuilder sb = new StringBuilder(metricName);
    sb.append("{");
    labels.entrySet().stream()
        .sorted(Map.Entry.comparingByKey())
        .forEach(
            entry -> sb.append(entry.getKey()).append("=").append(entry.getValue()).append(","));
    if (sb.charAt(sb.length() - 1) == ',') {
      sb.setLength(sb.length() - 1);
    }
    sb.append("}");
    return sb.toString();
  }

  private void evaluateAlerts() {
    if (!enabled) {
      return;
    }

    for (final AlertRule rule : alertRules.values()) {
      if (!rule.isEnabled()) {
        continue;
      }

      try {
        final List<ResourceMetric> metrics =
            executeAlertQuery(rule.getMetricQuery(), rule.getEvaluationWindow());
        if (metrics.isEmpty()) {
          continue;
        }

        final double currentValue = calculateAggregateValue(metrics);
        final boolean shouldAlert =
            evaluateAlertCondition(rule.getCondition(), currentValue, rule.getThreshold());

        final String instanceId = rule.getAlertId() + "-" + System.currentTimeMillis();
        final AlertInstance existingAlert = findActiveAlert(rule.getAlertId());

        if (shouldAlert && existingAlert == null) {
          // Fire new alert
          final AlertInstance alert =
              new AlertInstance(
                  instanceId,
                  rule,
                  currentValue,
                  String.format(
                      "Alert %s fired: %s %f %f",
                      rule.getName(), rule.getCondition(), currentValue, rule.getThreshold()),
                  Map.of("evaluation_window", rule.getEvaluationWindow()));

          activeAlerts.put(instanceId, alert);
          activeAlertCount.incrementAndGet();
          totalAlerts.incrementAndGet();

          // Notify listeners
          for (final AlertListener listener : alertListeners) {
            try {
              listener.onAlertFired(alert);
            } catch (final Exception e) {
              LOGGER.warning(String.format("Alert listener error: %s", e.getMessage()));
            }
          }

          LOGGER.warning(
              String.format(
                  "Alert fired: %s - %s (value: %f, threshold: %f)",
                  rule.getAlertId(), rule.getName(), currentValue, rule.getThreshold()));

        } else if (!shouldAlert && existingAlert != null) {
          // Resolve alert
          existingAlert.resolve();
          activeAlertCount.decrementAndGet();

          // Notify listeners
          for (final AlertListener listener : alertListeners) {
            try {
              listener.onAlertResolved(existingAlert);
            } catch (final Exception e) {
              LOGGER.warning(String.format("Alert listener error: %s", e.getMessage()));
            }
          }

          LOGGER.info(String.format("Alert resolved: %s - %s", rule.getAlertId(), rule.getName()));
        }

      } catch (final Exception e) {
        LOGGER.warning(
            String.format("Error evaluating alert %s: %s", rule.getAlertId(), e.getMessage()));
      }
    }
  }

  private List<ResourceMetric> executeAlertQuery(final String query, final Duration window) {
    // Simplified query execution - in real implementation would have query parser
    final Instant from = Instant.now().minus(window);
    final Instant to = Instant.now();

    // Extract metric name from query (simplified)
    final String metricName = extractMetricName(query);
    return getMetrics(metricName, null, from, to);
  }

  private List<ResourceMetric> executeDataQuery(final String query) {
    // Simplified query execution for dashboard widgets
    final Instant from = Instant.now().minus(Duration.ofHours(1));
    final Instant to = Instant.now();

    final String metricName = extractMetricName(query);
    return getMetrics(metricName, null, from, to);
  }

  private String extractMetricName(final String query) {
    // Simplified metric name extraction
    final String[] parts = query.split("\\s+");
    return parts.length > 0 ? parts[0] : "unknown";
  }

  private double calculateAggregateValue(final List<ResourceMetric> metrics) {
    // Simple average aggregation
    return metrics.stream().mapToDouble(ResourceMetric::getValue).average().orElse(0.0);
  }

  private boolean evaluateAlertCondition(
      final AlertCondition condition, final double value, final double threshold) {
    switch (condition) {
      case GREATER_THAN:
        return value > threshold;
      case LESS_THAN:
        return value < threshold;
      case EQUAL_TO:
        return Math.abs(value - threshold) < 0.001;
      case NOT_EQUAL_TO:
        return Math.abs(value - threshold) >= 0.001;
      case GREATER_THAN_OR_EQUAL:
        return value >= threshold;
      case LESS_THAN_OR_EQUAL:
        return value <= threshold;
      default:
        return false;
    }
  }

  private AlertInstance findActiveAlert(final String alertId) {
    return activeAlerts.values().stream()
        .filter(alert -> alert.getRule().getAlertId().equals(alertId))
        .filter(alert -> alert.getStatus() == AlertStatus.FIRING)
        .findFirst()
        .orElse(null);
  }

  private void performHealthChecks() {
    if (!enabled) {
      return;
    }

    for (final HealthCheck check : healthChecks.values()) {
      try {
        final HealthCheckResult result = check.getChecker().check();
        check.updateStatus(result.getStatus(), result.getMessage());

        LOGGER.fine(
            String.format(
                "Health check %s: %s - %s",
                check.getName(), result.getStatus(), result.getMessage()));

      } catch (final Exception e) {
        check.updateStatus(HealthStatus.UNHEALTHY, "Health check failed: " + e.getMessage());
        LOGGER.warning(
            String.format("Health check %s failed: %s", check.getName(), e.getMessage()));
      }
    }

    lastHealthCheck.set(Instant.now());
  }

  private void cleanupOldMetrics() {
    final Instant cutoff = Instant.now().minus(metricRetention);

    for (final List<ResourceMetric> series : metricHistory.values()) {
      synchronized (series) {
        series.removeIf(metric -> metric.getTimestamp().isBefore(cutoff));
      }
    }

    // Remove empty series
    metricHistory.entrySet().removeIf(entry -> entry.getValue().isEmpty());
  }

  private void cleanupResolvedAlerts() {
    final Instant cutoff =
        Instant.now().minus(Duration.ofDays(7)); // Keep resolved alerts for 7 days

    activeAlerts
        .entrySet()
        .removeIf(
            entry -> {
              final AlertInstance alert = entry.getValue();
              return alert.getStatus() == AlertStatus.RESOLVED
                  && alert.getResolvedAt().isBefore(cutoff);
            });
  }

  private double calculateSlope(final List<Double> values) {
    if (values.size() < 2) {
      return 0.0;
    }

    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumX2 = 0;
    final int n = values.size();

    for (int i = 0; i < n; i++) {
      sumX += i;
      sumY += values.get(i);
      sumXY += i * values.get(i);
      sumX2 += i * i;
    }

    return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
  }

  private double calculateTrendConfidence(final List<Double> values) {
    if (values.size() < 3) {
      return 0.0;
    }

    final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double variance =
        values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0);

    final double coefficientOfVariation = Math.sqrt(variance) / mean;
    return Math.max(0.0, 1.0 - coefficientOfVariation);
  }

  private TrendDirection determineTrendDirection(final double slope, final List<Double> values) {
    final double threshold = 0.01;

    if (Math.abs(slope) < threshold) {
      return TrendDirection.STABLE;
    }

    // Check for volatility
    final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double stdDev =
        Math.sqrt(values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0.0));
    final double cv = stdDev / mean;

    if (cv > 0.3) {
      return TrendDirection.VOLATILE;
    }

    return slope > 0 ? TrendDirection.INCREASING : TrendDirection.DECREASING;
  }

  private void initializeDefaultAlerts() {
    // High CPU usage alert
    addAlertRule(
        AlertRule.builder("high-cpu-usage", "High CPU Usage")
            .withDescription("CPU usage is above threshold")
            .withMetricQuery("cpu_usage_percent")
            .withCondition(AlertCondition.GREATER_THAN)
            .withThreshold(80.0)
            .withSeverity(AlertSeverity.WARNING)
            .withEvaluationWindow(Duration.ofMinutes(5))
            .build());

    // Memory pressure alert
    addAlertRule(
        AlertRule.builder("high-memory-usage", "High Memory Usage")
            .withDescription("Memory usage is above threshold")
            .withMetricQuery("memory_usage_percent")
            .withCondition(AlertCondition.GREATER_THAN)
            .withThreshold(90.0)
            .withSeverity(AlertSeverity.CRITICAL)
            .withEvaluationWindow(Duration.ofMinutes(3))
            .build());

    // Resource pool exhaustion alert
    addAlertRule(
        AlertRule.builder("resource-pool-exhaustion", "Resource Pool Exhaustion")
            .withDescription("Resource pool is nearly exhausted")
            .withMetricQuery("resource_pool_utilization")
            .withCondition(AlertCondition.GREATER_THAN)
            .withThreshold(95.0)
            .withSeverity(AlertSeverity.CRITICAL)
            .withEvaluationWindow(Duration.ofMinutes(2))
            .build());
  }

  private void initializeDefaultHealthChecks() {
    // Resource manager health check
    addHealthCheck(
        new HealthCheck(
            "resource-manager-health",
            "Resource Manager Health",
            "Checks if resource manager is functioning properly",
            () -> HealthCheckResult.healthy("Resource manager is operational"),
            Duration.ofMinutes(1),
            Duration.ofSeconds(30),
            true));

    // Metric collection health check
    addHealthCheck(
        new HealthCheck(
            "metric-collection-health",
            "Metric Collection Health",
            "Checks if metrics are being collected",
            () -> {
              final long recentMetrics = totalMetrics.get();
              if (recentMetrics > 0) {
                return HealthCheckResult.healthy("Metrics are being collected");
              } else {
                return HealthCheckResult.degraded("No recent metrics collected");
              }
            },
            Duration.ofMinutes(2),
            Duration.ofSeconds(30),
            false));

    // Alert system health check
    addHealthCheck(
        new HealthCheck(
            "alert-system-health",
            "Alert System Health",
            "Checks if alert system is functioning",
            () -> HealthCheckResult.healthy("Alert system is operational"),
            Duration.ofMinutes(5),
            Duration.ofSeconds(30),
            true));
  }

  private void initializeDefaultDashboard() {
    // Resource utilization widget
    addDashboardWidget(
        new DashboardWidget(
            "resource-utilization",
            "Resource Utilization",
            WidgetType.LINE_CHART,
            "resource_utilization_percent",
            Map.of("chart_type", "time_series", "unit", "percent"),
            30));

    // Active alerts widget
    addDashboardWidget(
        new DashboardWidget(
            "active-alerts",
            "Active Alerts",
            WidgetType.STAT,
            "active_alerts_count",
            Map.of("stat_type", "single_value"),
            60));

    // Health status widget
    addDashboardWidget(
        new DashboardWidget(
            "health-status",
            "System Health",
            WidgetType.GAUGE,
            "system_health_status",
            Map.of(
                "gauge_type",
                "health",
                "thresholds",
                Map.of("healthy", "green", "degraded", "yellow", "unhealthy", "red")),
            30));

    // Performance trends widget
    addDashboardWidget(
        new DashboardWidget(
            "performance-trends",
            "Performance Trends",
            WidgetType.LINE_CHART,
            "performance_metrics",
            Map.of("chart_type", "multi_series", "time_range", "24h"),
            60));
  }

  private void startObservabilityTasks() {
    // Alert evaluation
    observabilityExecutor.scheduleAtFixedRate(this::evaluateAlerts, 30, 30, TimeUnit.SECONDS);

    // Health checks
    observabilityExecutor.scheduleAtFixedRate(this::performHealthChecks, 60, 60, TimeUnit.SECONDS);

    // Metric cleanup
    observabilityExecutor.scheduleAtFixedRate(this::cleanupOldMetrics, 600, 600, TimeUnit.SECONDS);

    // Alert cleanup
    observabilityExecutor.scheduleAtFixedRate(
        this::cleanupResolvedAlerts, 1800, 1800, TimeUnit.SECONDS);
  }

  /**
   * Sets the metric retention period.
   *
   * @param metricRetention retention duration
   */
  public void setMetricRetention(final Duration metricRetention) {
    this.metricRetention = metricRetention;
    LOGGER.info("Metric retention set to " + metricRetention);
  }

  /**
   * Enables or disables observability.
   *
   * @param enabled true to enable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource observability " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the observability manager. */
  public void shutdown() {
    enabled = false;

    observabilityExecutor.shutdown();
    try {
      if (!observabilityExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        observabilityExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      observabilityExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Resource observability manager shutdown");
  }
}
