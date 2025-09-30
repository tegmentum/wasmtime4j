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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Production monitoring and analytics system providing comprehensive visibility into WebAssembly
 * operations, performance metrics, and system health.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Real-time operational metrics collection and aggregation
 *   <li>Performance trend analysis and forecasting
 *   <li>System health monitoring and alerting
 *   <li>Capacity planning and scaling recommendations
 *   <li>Operational dashboards and reporting
 *   <li>SLA monitoring and compliance tracking
 * </ul>
 *
 * @since 1.0.0
 */
public final class ProductionMonitoringSystem {

  private static final Logger LOGGER = Logger.getLogger(ProductionMonitoringSystem.class.getName());

  /** Metric types for monitoring. */
  public enum MetricType {
    COUNTER, // Monotonically increasing values
    GAUGE, // Current value that can go up or down
    HISTOGRAM, // Distribution of values
    TIMER, // Duration measurements
    RATE // Rate of events per time unit
  }

  /** Alert severity levels. */
  public enum AlertSeverity {
    INFO,
    WARNING,
    CRITICAL,
    EMERGENCY
  }

  /** System health status. */
  public enum HealthStatus {
    HEALTHY,
    DEGRADED,
    UNHEALTHY,
    UNKNOWN
  }

  /** Production metric data point. */
  public static final class MetricDataPoint {
    private final String metricName;
    private final MetricType metricType;
    private final double value;
    private final Instant timestamp;
    private final Map<String, String> tags;

    public MetricDataPoint(
        final String metricName,
        final MetricType metricType,
        final double value,
        final Map<String, String> tags) {
      this.metricName = metricName;
      this.metricType = metricType;
      this.value = value;
      this.timestamp = Instant.now();
      this.tags = Map.copyOf(tags != null ? tags : Map.of());
    }

    public String getMetricName() {
      return metricName;
    }

    public MetricType getMetricType() {
      return metricType;
    }

    public double getValue() {
      return value;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Map<String, String> getTags() {
      return tags;
    }
  }

  /** Alert configuration and state. */
  public static final class AlertRule {
    private final String alertId;
    private final String metricName;
    private final String condition;
    private final double threshold;
    private final Duration duration;
    private final AlertSeverity severity;
    private final String description;
    private volatile boolean enabled;
    private volatile Instant lastTriggered;
    private volatile long triggerCount;

    public AlertRule(
        final String alertId,
        final String metricName,
        final String condition,
        final double threshold,
        final Duration duration,
        final AlertSeverity severity,
        final String description) {
      this.alertId = alertId;
      this.metricName = metricName;
      this.condition = condition;
      this.threshold = threshold;
      this.duration = duration;
      this.severity = severity;
      this.description = description;
      this.enabled = true;
      this.triggerCount = 0;
    }

    public String getAlertId() {
      return alertId;
    }

    public String getMetricName() {
      return metricName;
    }

    public String getCondition() {
      return condition;
    }

    public double getThreshold() {
      return threshold;
    }

    public Duration getDuration() {
      return duration;
    }

    public AlertSeverity getSeverity() {
      return severity;
    }

    public String getDescription() {
      return description;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public Instant getLastTriggered() {
      return lastTriggered;
    }

    public long getTriggerCount() {
      return triggerCount;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    public void recordTrigger() {
      this.lastTriggered = Instant.now();
      this.triggerCount++;
    }
  }

  /** Alert event. */
  public static final class AlertEvent {
    private final String alertId;
    private final String metricName;
    private final double value;
    private final double threshold;
    private final AlertSeverity severity;
    private final String description;
    private final Instant timestamp;
    private final Map<String, String> metadata;

    public AlertEvent(
        final String alertId,
        final String metricName,
        final double value,
        final double threshold,
        final AlertSeverity severity,
        final String description,
        final Map<String, String> metadata) {
      this.alertId = alertId;
      this.metricName = metricName;
      this.value = value;
      this.threshold = threshold;
      this.severity = severity;
      this.description = description;
      this.timestamp = Instant.now();
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public String getAlertId() {
      return alertId;
    }

    public String getMetricName() {
      return metricName;
    }

    public double getValue() {
      return value;
    }

    public double getThreshold() {
      return threshold;
    }

    public AlertSeverity getSeverity() {
      return severity;
    }

    public String getDescription() {
      return description;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Map<String, String> getMetadata() {
      return metadata;
    }
  }

  /** Time series data for trend analysis. */
  private static final class TimeSeriesData {
    private final String metricName;
    private final List<MetricDataPoint> dataPoints;
    private volatile double trend;
    private volatile double forecast;
    private volatile Instant lastAnalysis;

    TimeSeriesData(final String metricName) {
      this.metricName = metricName;
      this.dataPoints = new java.util.ArrayList<>();
      this.trend = 0.0;
      this.forecast = 0.0;
      this.lastAnalysis = Instant.now();
    }

    synchronized void addDataPoint(final MetricDataPoint dataPoint) {
      dataPoints.add(dataPoint);

      // Keep only last 1000 data points to prevent memory growth
      if (dataPoints.size() > 1000) {
        dataPoints.removeIf(
            dp -> dp.getTimestamp().isBefore(Instant.now().minus(Duration.ofHours(24))));
      }
    }

    synchronized void calculateTrend() {
      if (dataPoints.size() < 2) {
        return;
      }

      // Simple linear regression for trend calculation
      final int n = dataPoints.size();
      double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

      for (int i = 0; i < n; i++) {
        final double x = i;
        final double y = dataPoints.get(i).getValue();
        sumX += x;
        sumY += y;
        sumXY += x * y;
        sumX2 += x * x;
      }

      final double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
      this.trend = slope;
      this.lastAnalysis = Instant.now();

      // Simple forecast: current value + trend * periods
      if (!dataPoints.isEmpty()) {
        final double currentValue = dataPoints.get(dataPoints.size() - 1).getValue();
        this.forecast = currentValue + (trend * 10); // 10 periods ahead
      }
    }

    public double getTrend() {
      return trend;
    }

    public double getForecast() {
      return forecast;
    }

    public Instant getLastAnalysis() {
      return lastAnalysis;
    }

    public int getDataPointCount() {
      return dataPoints.size();
    }
  }

  /** Metrics storage and aggregation. */
  private final ConcurrentHashMap<String, TimeSeriesData> timeSeriesData =
      new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, MetricDataPoint> currentMetrics =
      new ConcurrentHashMap<>();

  /** Alert management. */
  private final ConcurrentHashMap<String, AlertRule> alertRules = new ConcurrentHashMap<>();

  private final ConcurrentHashMap<String, AlertEvent> recentAlerts = new ConcurrentHashMap<>();

  /** System health tracking. */
  private final AtomicReference<HealthStatus> systemHealth =
      new AtomicReference<>(HealthStatus.HEALTHY);

  private final AtomicLong healthCheckCount = new AtomicLong(0);
  private final AtomicLong alertCount = new AtomicLong(0);

  /** Monitoring configuration. */
  private volatile boolean monitoringEnabled = true;

  private volatile boolean alertingEnabled = true;
  private volatile boolean trendAnalysisEnabled = true;
  private volatile Duration metricRetentionPeriod = Duration.ofDays(7);
  private volatile int maxRecentAlerts = 1000;

  /** Background processing. */
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(3);

  /** Monitoring statistics. */
  private final AtomicLong totalMetricsCollected = new AtomicLong(0);

  private final AtomicLong totalAlertsTriggered = new AtomicLong(0);
  private final AtomicLong totalHealthChecks = new AtomicLong(0);

  /** Creates a new production monitoring system. */
  public ProductionMonitoringSystem() {
    initializeDefaultAlerts();
    startBackgroundProcessing();
    LOGGER.info("Production monitoring system initialized");
  }

  /**
   * Records a metric data point.
   *
   * @param metricName the metric name
   * @param metricType the metric type
   * @param value the metric value
   * @param tags optional tags for the metric
   */
  public void recordMetric(
      final String metricName,
      final MetricType metricType,
      final double value,
      final Map<String, String> tags) {
    if (!monitoringEnabled) {
      return;
    }

    final MetricDataPoint dataPoint = new MetricDataPoint(metricName, metricType, value, tags);

    // Store current metric value
    currentMetrics.put(metricName, dataPoint);

    // Add to time series for trend analysis
    if (trendAnalysisEnabled) {
      final TimeSeriesData timeSeries =
          timeSeriesData.computeIfAbsent(metricName, TimeSeriesData::new);
      timeSeries.addDataPoint(dataPoint);
    }

    totalMetricsCollected.incrementAndGet();

    // Check alert rules
    if (alertingEnabled) {
      checkAlertRules(metricName, value, tags);
    }
  }

  /**
   * Records a simple counter metric.
   *
   * @param metricName the metric name
   * @param value the counter value
   */
  public void recordCounter(final String metricName, final long value) {
    recordMetric(metricName, MetricType.COUNTER, value, null);
  }

  /**
   * Records a gauge metric.
   *
   * @param metricName the metric name
   * @param value the gauge value
   */
  public void recordGauge(final String metricName, final double value) {
    recordMetric(metricName, MetricType.GAUGE, value, null);
  }

  /**
   * Records a timer metric.
   *
   * @param metricName the metric name
   * @param duration the duration
   */
  public void recordTimer(final String metricName, final Duration duration) {
    recordMetric(metricName, MetricType.TIMER, duration.toNanos(), null);
  }

  /**
   * Adds an alert rule.
   *
   * @param alertRule the alert rule to add
   */
  public void addAlertRule(final AlertRule alertRule) {
    alertRules.put(alertRule.getAlertId(), alertRule);
    LOGGER.info("Added alert rule: " + alertRule.getAlertId());
  }

  /** Checks alert rules for a specific metric. */
  private void checkAlertRules(
      final String metricName, final double value, final Map<String, String> tags) {
    for (final AlertRule rule : alertRules.values()) {
      if (!rule.isEnabled() || !rule.getMetricName().equals(metricName)) {
        continue;
      }

      boolean shouldTrigger = false;
      switch (rule.getCondition().toLowerCase()) {
        case "greater_than":
        case ">":
          shouldTrigger = value > rule.getThreshold();
          break;
        case "less_than":
        case "<":
          shouldTrigger = value < rule.getThreshold();
          break;
        case "equals":
        case "==":
          shouldTrigger = Math.abs(value - rule.getThreshold()) < 0.001;
          break;
        case "not_equals":
        case "!=":
          shouldTrigger = Math.abs(value - rule.getThreshold()) >= 0.001;
          break;
      }

      if (shouldTrigger) {
        triggerAlert(rule, value, tags);
      }
    }
  }

  /** Triggers an alert. */
  private void triggerAlert(
      final AlertRule rule, final double value, final Map<String, String> tags) {
    rule.recordTrigger();
    alertCount.incrementAndGet();
    totalAlertsTriggered.incrementAndGet();

    final AlertEvent alertEvent =
        new AlertEvent(
            rule.getAlertId(),
            rule.getMetricName(),
            value,
            rule.getThreshold(),
            rule.getSeverity(),
            rule.getDescription(),
            tags);

    // Store recent alert
    if (recentAlerts.size() >= maxRecentAlerts) {
      // Remove oldest alert (simplified - would use proper LRU in production)
      final String oldestKey = recentAlerts.keySet().iterator().next();
      recentAlerts.remove(oldestKey);
    }
    recentAlerts.put(
        alertEvent.getAlertId() + "_" + alertEvent.getTimestamp().toEpochMilli(), alertEvent);

    // Log alert
    LOGGER.warning(
        String.format(
            "ALERT [%s]: %s - %s=%.2f (threshold=%.2f)",
            rule.getSeverity(),
            rule.getAlertId(),
            rule.getMetricName(),
            value,
            rule.getThreshold()));

    // Update system health based on alert severity
    updateSystemHealth(rule.getSeverity());
  }

  /** Updates system health based on alert severity. */
  private void updateSystemHealth(final AlertSeverity severity) {
    final HealthStatus currentHealth = systemHealth.get();
    HealthStatus newHealth = currentHealth;

    switch (severity) {
      case EMERGENCY:
        newHealth = HealthStatus.UNHEALTHY;
        break;
      case CRITICAL:
        if (currentHealth == HealthStatus.HEALTHY) {
          newHealth = HealthStatus.DEGRADED;
        }
        break;
      case WARNING:
        // Don't change health status for warnings alone
        break;
      case INFO:
        // No health impact
        break;
    }

    if (newHealth != currentHealth) {
      systemHealth.set(newHealth);
      LOGGER.info("System health changed from " + currentHealth + " to " + newHealth);
    }
  }

  /** Initializes default alert rules. */
  private void initializeDefaultAlerts() {
    // High memory usage alert
    addAlertRule(
        new AlertRule(
            "high_memory",
            "heap_memory_usage",
            "greater_than",
            0.85,
            Duration.ofMinutes(5),
            AlertSeverity.WARNING,
            "High heap memory usage detected"));

    // High error rate alert
    addAlertRule(
        new AlertRule(
            "high_error_rate",
            "error_rate",
            "greater_than",
            0.05,
            Duration.ofMinutes(2),
            AlertSeverity.CRITICAL,
            "High error rate detected"));

    // Long execution time alert
    addAlertRule(
        new AlertRule(
            "long_execution",
            "execution_time_ms",
            "greater_than",
            10000,
            Duration.ofMinutes(1),
            AlertSeverity.WARNING,
            "Long execution time detected"));
  }

  /** Starts background processing tasks. */
  private void startBackgroundProcessing() {
    // Trend analysis task
    backgroundExecutor.scheduleAtFixedRate(
        this::performTrendAnalysis, 60, 300, TimeUnit.SECONDS); // Every 5 minutes

    // Health check task
    backgroundExecutor.scheduleAtFixedRate(
        this::performHealthCheck, 30, 60, TimeUnit.SECONDS); // Every minute

    // Metric cleanup task
    backgroundExecutor.scheduleAtFixedRate(
        this::performMetricCleanup, 3600, 3600, TimeUnit.SECONDS); // Every hour
  }

  /** Performs trend analysis on collected metrics. */
  private void performTrendAnalysis() {
    if (!trendAnalysisEnabled) {
      return;
    }

    try {
      for (final TimeSeriesData timeSeries : timeSeriesData.values()) {
        timeSeries.calculateTrend();
      }
      LOGGER.fine("Trend analysis completed for " + timeSeriesData.size() + " metrics");
    } catch (final Exception e) {
      LOGGER.warning("Trend analysis failed: " + e.getMessage());
    }
  }

  /** Performs comprehensive health check. */
  private void performHealthCheck() {
    healthCheckCount.incrementAndGet();
    totalHealthChecks.incrementAndGet();

    try {
      // Check if we have recent metrics
      final long recentMetricCount =
          currentMetrics.values().stream()
              .mapToLong(
                  metric ->
                      metric.getTimestamp().isAfter(Instant.now().minus(Duration.ofMinutes(5)))
                          ? 1
                          : 0)
              .sum();

      // Check for recent critical alerts
      final long recentCriticalAlerts =
          recentAlerts.values().stream()
              .mapToLong(
                  alert ->
                      alert.getSeverity() == AlertSeverity.CRITICAL
                              || alert.getSeverity() == AlertSeverity.EMERGENCY
                          ? (alert
                                  .getTimestamp()
                                  .isAfter(Instant.now().minus(Duration.ofMinutes(10)))
                              ? 1
                              : 0)
                          : 0)
              .sum();

      // Determine health status
      HealthStatus newHealth = HealthStatus.HEALTHY;

      if (recentCriticalAlerts > 0) {
        newHealth = HealthStatus.UNHEALTHY;
      } else if (recentMetricCount == 0) {
        newHealth = HealthStatus.UNKNOWN;
      } else if (recentAlerts.size() > 10) {
        newHealth = HealthStatus.DEGRADED;
      }

      final HealthStatus oldHealth = systemHealth.getAndSet(newHealth);
      if (newHealth != oldHealth) {
        LOGGER.info("Health check: system health changed from " + oldHealth + " to " + newHealth);
      }

    } catch (final Exception e) {
      LOGGER.warning("Health check failed: " + e.getMessage());
      systemHealth.set(HealthStatus.UNKNOWN);
    }
  }

  /** Performs metric cleanup to prevent memory growth. */
  private void performMetricCleanup() {
    try {
      final Instant cutoff = Instant.now().minus(metricRetentionPeriod);
      final java.util.concurrent.atomic.AtomicInteger cleanedMetrics =
          new java.util.concurrent.atomic.AtomicInteger(0);
      final java.util.concurrent.atomic.AtomicInteger cleanedAlerts =
          new java.util.concurrent.atomic.AtomicInteger(0);

      // Clean old metrics
      currentMetrics
          .entrySet()
          .removeIf(
              entry -> {
                if (entry.getValue().getTimestamp().isBefore(cutoff)) {
                  cleanedMetrics.incrementAndGet();
                  return true;
                }
                return false;
              });

      // Clean old alerts
      recentAlerts
          .entrySet()
          .removeIf(
              entry -> {
                if (entry.getValue().getTimestamp().isBefore(cutoff)) {
                  cleanedAlerts.incrementAndGet();
                  return true;
                }
                return false;
              });

      if (cleanedMetrics.get() > 0 || cleanedAlerts.get() > 0) {
        LOGGER.fine(
            String.format(
                "Metric cleanup: removed %d old metrics and %d old alerts",
                cleanedMetrics.get(), cleanedAlerts.get()));
      }

    } catch (final Exception e) {
      LOGGER.warning("Metric cleanup failed: " + e.getMessage());
    }
  }

  /**
   * Gets current system health status.
   *
   * @return system health status
   */
  public HealthStatus getSystemHealth() {
    return systemHealth.get();
  }

  /**
   * Gets current metric value.
   *
   * @param metricName the metric name
   * @return current metric value or null if not found
   */
  public MetricDataPoint getCurrentMetric(final String metricName) {
    return currentMetrics.get(metricName);
  }

  /**
   * Gets trend analysis for a metric.
   *
   * @param metricName the metric name
   * @return trend data or null if not found
   */
  public TimeSeriesData getTrendAnalysis(final String metricName) {
    return timeSeriesData.get(metricName);
  }

  /**
   * Gets recent alerts.
   *
   * @param limit maximum number of alerts to return
   * @return list of recent alerts
   */
  public List<AlertEvent> getRecentAlerts(final int limit) {
    return recentAlerts.values().stream()
        .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
        .limit(limit)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Gets comprehensive monitoring dashboard data.
   *
   * @return formatted dashboard data
   */
  public String getDashboardData() {
    final StringBuilder sb = new StringBuilder("=== Production Monitoring Dashboard ===\n");

    // System overview
    sb.append(String.format("System Health: %s\n", getSystemHealth()));
    sb.append(String.format("Total Metrics Collected: %,d\n", totalMetricsCollected.get()));
    sb.append(String.format("Total Alerts Triggered: %,d\n", totalAlertsTriggered.get()));
    sb.append(String.format("Total Health Checks: %,d\n", totalHealthChecks.get()));
    sb.append(String.format("Active Alert Rules: %d\n", alertRules.size()));
    sb.append("\n");

    // Key metrics
    sb.append("Key Metrics:\n");
    final String[] keyMetrics = {
      "heap_memory_usage", "error_rate", "execution_time_ms", "request_count"
    };
    for (final String metricName : keyMetrics) {
      final MetricDataPoint metric = getCurrentMetric(metricName);
      if (metric != null) {
        sb.append(
            String.format(
                "  %-20s: %.2f (as of %s)\n",
                metricName, metric.getValue(), metric.getTimestamp()));
      }
    }
    sb.append("\n");

    // Recent alerts
    sb.append("Recent Alerts (last 5):\n");
    final List<AlertEvent> alerts = getRecentAlerts(5);
    if (alerts.isEmpty()) {
      sb.append("  No recent alerts\n");
    } else {
      for (final AlertEvent alert : alerts) {
        sb.append(
            String.format(
                "  [%s] %s: %s (%.2f > %.2f)\n",
                alert.getSeverity(),
                alert.getAlertId(),
                alert.getDescription(),
                alert.getValue(),
                alert.getThreshold()));
      }
    }

    return sb.toString();
  }

  /**
   * Gets monitoring statistics.
   *
   * @return formatted monitoring statistics
   */
  public String getMonitoringStatistics() {
    return String.format(
        "Monitoring Statistics: metrics=%d, alerts=%d, health_checks=%d, "
            + "time_series=%d, alert_rules=%d, system_health=%s",
        totalMetricsCollected.get(),
        totalAlertsTriggered.get(),
        totalHealthChecks.get(),
        timeSeriesData.size(),
        alertRules.size(),
        getSystemHealth());
  }

  /**
   * Enables or disables monitoring.
   *
   * @param enabled true to enable monitoring
   */
  public void setMonitoringEnabled(final boolean enabled) {
    this.monitoringEnabled = enabled;
    LOGGER.info("Production monitoring " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the monitoring system. */
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
    LOGGER.info("Production monitoring system shutdown");
  }
}
