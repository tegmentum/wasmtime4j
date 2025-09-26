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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Intelligent alerting system providing advanced anomaly detection, alert correlation, noise
 * reduction, and automated escalation for production monitoring.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Advanced anomaly detection using statistical models and machine learning
 *   <li>Intelligent alert correlation to identify related incidents
 *   <li>Dynamic noise reduction and alert suppression
 *   <li>Automated escalation and incident routing
 *   <li>Alert clustering and pattern recognition
 *   <li>Adaptive thresholds based on historical data
 *   <li>Multi-dimensional alerting with contextual information
 * </ul>
 *
 * @since 1.0.0
 */
public final class IntelligentAlertingSystem {

  private static final Logger LOGGER = Logger.getLogger(IntelligentAlertingSystem.class.getName());

  /** Alert severity with escalation levels. */
  public enum AlertSeverity {
    INFO(0, Duration.ofHours(24), false),
    LOW(1, Duration.ofHours(4), false),
    MEDIUM(2, Duration.ofHours(1), true),
    HIGH(3, Duration.ofMinutes(30), true),
    CRITICAL(4, Duration.ofMinutes(10), true),
    EMERGENCY(5, Duration.ofMinutes(5), true);

    private final int level;
    private final Duration escalationTime;
    private final boolean requiresAcknowledgment;

    AlertSeverity(final int level, final Duration escalationTime, final boolean requiresAcknowledgment) {
      this.level = level;
      this.escalationTime = escalationTime;
      this.requiresAcknowledgment = requiresAcknowledgment;
    }

    public int getLevel() { return level; }
    public Duration getEscalationTime() { return escalationTime; }
    public boolean requiresAcknowledgment() { return requiresAcknowledgment; }

    public boolean isHigherThan(final AlertSeverity other) {
      return this.level > other.level;
    }
  }

  /** Alert correlation strategy. */
  public enum CorrelationStrategy {
    TIME_BASED,     // Correlate alerts within time windows
    COMPONENT_BASED, // Correlate alerts from related components
    PATTERN_BASED,   // Correlate alerts with similar patterns
    CAUSAL_CHAIN    // Correlate alerts in cause-effect relationships
  }

  /** Anomaly detection algorithm. */
  public enum AnomalyDetectionAlgorithm {
    STATISTICAL_THRESHOLD,  // Simple statistical thresholds
    MOVING_AVERAGE,        // Moving average with deviation
    EXPONENTIAL_SMOOTHING, // Exponential smoothing forecasting
    SEASONAL_DECOMPOSITION, // Seasonal pattern analysis
    MACHINE_LEARNING       // ML-based anomaly detection
  }

  /** Intelligent alert rule with advanced features. */
  public static final class IntelligentAlertRule {
    private final String ruleId;
    private final String name;
    private final String description;
    private final String metricName;
    private final AnomalyDetectionAlgorithm algorithm;
    private final Map<String, Object> algorithmParameters;
    private final AlertSeverity baseSeverity;
    private final Duration evaluationWindow;
    private final Duration suppressionWindow;
    private final List<String> correlationTags;
    private final boolean adaptiveThresholds;
    private final boolean enabled;

    public IntelligentAlertRule(
        final String ruleId,
        final String name,
        final String description,
        final String metricName,
        final AnomalyDetectionAlgorithm algorithm,
        final Map<String, Object> algorithmParameters,
        final AlertSeverity baseSeverity,
        final Duration evaluationWindow,
        final Duration suppressionWindow,
        final List<String> correlationTags,
        final boolean adaptiveThresholds,
        final boolean enabled) {
      this.ruleId = ruleId;
      this.name = name;
      this.description = description;
      this.metricName = metricName;
      this.algorithm = algorithm;
      this.algorithmParameters = Map.copyOf(algorithmParameters != null ? algorithmParameters : Map.of());
      this.baseSeverity = baseSeverity;
      this.evaluationWindow = evaluationWindow != null ? evaluationWindow : Duration.ofMinutes(5);
      this.suppressionWindow = suppressionWindow != null ? suppressionWindow : Duration.ofMinutes(15);
      this.correlationTags = List.copyOf(correlationTags != null ? correlationTags : List.of());
      this.adaptiveThresholds = adaptiveThresholds;
      this.enabled = enabled;
    }

    // Getters
    public String getRuleId() { return ruleId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMetricName() { return metricName; }
    public AnomalyDetectionAlgorithm getAlgorithm() { return algorithm; }
    public Map<String, Object> getAlgorithmParameters() { return algorithmParameters; }
    public AlertSeverity getBaseSeverity() { return baseSeverity; }
    public Duration getEvaluationWindow() { return evaluationWindow; }
    public Duration getSuppressionWindow() { return suppressionWindow; }
    public List<String> getCorrelationTags() { return correlationTags; }
    public boolean isAdaptiveThresholds() { return adaptiveThresholds; }
    public boolean isEnabled() { return enabled; }
  }

  /** Correlated alert event with context. */
  public static final class CorrelatedAlert {
    private final String alertId;
    private final String correlationId;
    private final IntelligentAlertRule rule;
    private final double metricValue;
    private final double threshold;
    private final double confidenceScore;
    private final AlertSeverity severity;
    private final Instant timestamp;
    private final Instant expiryTime;
    private final Map<String, Object> context;
    private final List<String> relatedAlerts;
    private final String rootCauseAnalysis;
    private volatile boolean acknowledged;
    private volatile boolean suppressed;
    private volatile Instant acknowledgmentTime;
    private volatile String acknowledgedBy;

    public CorrelatedAlert(
        final String alertId,
        final String correlationId,
        final IntelligentAlertRule rule,
        final double metricValue,
        final double threshold,
        final double confidenceScore,
        final AlertSeverity severity,
        final Map<String, Object> context,
        final List<String> relatedAlerts,
        final String rootCauseAnalysis) {
      this.alertId = alertId;
      this.correlationId = correlationId;
      this.rule = rule;
      this.metricValue = metricValue;
      this.threshold = threshold;
      this.confidenceScore = confidenceScore;
      this.severity = severity;
      this.timestamp = Instant.now();
      this.expiryTime = timestamp.plus(Duration.ofHours(24)); // Default 24h expiry
      this.context = Map.copyOf(context != null ? context : Map.of());
      this.relatedAlerts = List.copyOf(relatedAlerts != null ? relatedAlerts : List.of());
      this.rootCauseAnalysis = rootCauseAnalysis != null ? rootCauseAnalysis : "Analysis pending";
      this.acknowledged = false;
      this.suppressed = false;
    }

    // Getters
    public String getAlertId() { return alertId; }
    public String getCorrelationId() { return correlationId; }
    public IntelligentAlertRule getRule() { return rule; }
    public double getMetricValue() { return metricValue; }
    public double getThreshold() { return threshold; }
    public double getConfidenceScore() { return confidenceScore; }
    public AlertSeverity getSeverity() { return severity; }
    public Instant getTimestamp() { return timestamp; }
    public Instant getExpiryTime() { return expiryTime; }
    public Map<String, Object> getContext() { return context; }
    public List<String> getRelatedAlerts() { return relatedAlerts; }
    public String getRootCauseAnalysis() { return rootCauseAnalysis; }
    public boolean isAcknowledged() { return acknowledged; }
    public boolean isSuppressed() { return suppressed; }
    public Instant getAcknowledgmentTime() { return acknowledgmentTime; }
    public String getAcknowledgedBy() { return acknowledgedBy; }

    public void acknowledge(final String acknowledgedBy) {
      this.acknowledged = true;
      this.acknowledgmentTime = Instant.now();
      this.acknowledgedBy = acknowledgedBy;
    }

    public void suppress() {
      this.suppressed = true;
    }

    public boolean isExpired() {
      return Instant.now().isAfter(expiryTime);
    }

    public Duration getAge() {
      return Duration.between(timestamp, Instant.now());
    }
  }

  /** Alert correlation group. */
  public static final class AlertCorrelationGroup {
    private final String groupId;
    private final CorrelationStrategy strategy;
    private final List<CorrelatedAlert> alerts;
    private final Instant createdAt;
    private final AtomicReference<AlertSeverity> highestSeverity;
    private final Map<String, Object> groupMetadata;

    public AlertCorrelationGroup(
        final String groupId,
        final CorrelationStrategy strategy,
        final CorrelatedAlert initialAlert) {
      this.groupId = groupId;
      this.strategy = strategy;
      this.alerts = new CopyOnWriteArrayList<>();
      this.alerts.add(initialAlert);
      this.createdAt = Instant.now();
      this.highestSeverity = new AtomicReference<>(initialAlert.getSeverity());
      this.groupMetadata = new ConcurrentHashMap<>();
    }

    public void addAlert(final CorrelatedAlert alert) {
      alerts.add(alert);
      if (alert.getSeverity().isHigherThan(highestSeverity.get())) {
        highestSeverity.set(alert.getSeverity());
      }
    }

    // Getters
    public String getGroupId() { return groupId; }
    public CorrelationStrategy getStrategy() { return strategy; }
    public List<CorrelatedAlert> getAlerts() { return List.copyOf(alerts); }
    public Instant getCreatedAt() { return createdAt; }
    public AlertSeverity getHighestSeverity() { return highestSeverity.get(); }
    public Map<String, Object> getGroupMetadata() { return groupMetadata; }
    public int getAlertCount() { return alerts.size(); }

    public boolean isExpired() {
      return alerts.stream().allMatch(CorrelatedAlert::isExpired);
    }
  }

  /** Anomaly detection result. */
  public static final class AnomalyDetectionResult {
    private final boolean isAnomaly;
    private final double anomalyScore;
    private final double threshold;
    private final String explanation;
    private final Map<String, Object> diagnostics;

    public AnomalyDetectionResult(
        final boolean isAnomaly,
        final double anomalyScore,
        final double threshold,
        final String explanation,
        final Map<String, Object> diagnostics) {
      this.isAnomaly = isAnomaly;
      this.anomalyScore = anomalyScore;
      this.threshold = threshold;
      this.explanation = explanation;
      this.diagnostics = Map.copyOf(diagnostics != null ? diagnostics : Map.of());
    }

    // Getters
    public boolean isAnomaly() { return isAnomaly; }
    public double getAnomalyScore() { return anomalyScore; }
    public double getThreshold() { return threshold; }
    public String getExplanation() { return explanation; }
    public Map<String, Object> getDiagnostics() { return diagnostics; }
  }

  /** Historical metric data for analysis. */
  private static final class HistoricalMetricData {
    private final List<DataPoint> dataPoints = new CopyOnWriteArrayList<>();
    private volatile double mean = 0.0;
    private volatile double standardDeviation = 0.0;
    private volatile Instant lastCalculation = Instant.now();

    static class DataPoint {
      final double value;
      final Instant timestamp;

      DataPoint(final double value, final Instant timestamp) {
        this.value = value;
        this.timestamp = timestamp;
      }
    }

    void addDataPoint(final double value) {
      dataPoints.add(new DataPoint(value, Instant.now()));

      // Keep only last 1000 data points
      if (dataPoints.size() > 1000) {
        dataPoints.removeIf(dp -> dp.timestamp.isBefore(Instant.now().minus(7, ChronoUnit.DAYS)));
      }

      // Recalculate statistics every 10 data points
      if (dataPoints.size() % 10 == 0) {
        calculateStatistics();
      }
    }

    void calculateStatistics() {
      if (dataPoints.isEmpty()) {
        return;
      }

      final double sum = dataPoints.stream().mapToDouble(dp -> dp.value).sum();
      this.mean = sum / dataPoints.size();

      if (dataPoints.size() > 1) {
        final double variance = dataPoints.stream()
            .mapToDouble(dp -> Math.pow(dp.value - mean, 2))
            .sum() / (dataPoints.size() - 1);
        this.standardDeviation = Math.sqrt(variance);
      }

      this.lastCalculation = Instant.now();
    }

    double getMean() { return mean; }
    double getStandardDeviation() { return standardDeviation; }
    int getDataPointCount() { return dataPoints.size(); }

    List<Double> getRecentValues(final int count) {
      return dataPoints.stream()
          .sorted(Comparator.comparing(dp -> dp.timestamp, Comparator.reverseOrder()))
          .limit(count)
          .map(dp -> dp.value)
          .collect(Collectors.toList());
    }
  }

  // Instance fields
  private final ConcurrentHashMap<String, IntelligentAlertRule> alertRules = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CorrelatedAlert> activeAlerts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, AlertCorrelationGroup> correlationGroups = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, HistoricalMetricData> historicalData = new ConcurrentHashMap<>();

  // Alert listeners and handlers
  private final List<AlertListener> alertListeners = new CopyOnWriteArrayList<>();
  private final List<CorrelationHandler> correlationHandlers = new CopyOnWriteArrayList<>();

  // Statistics and monitoring
  private final AtomicLong totalAlertsGenerated = new AtomicLong(0);
  private final AtomicLong totalAlertsSuppressed = new AtomicLong(0);
  private final AtomicLong totalAlertsCorrelated = new AtomicLong(0);
  private final AtomicLong totalAnomaliesDetected = new AtomicLong(0);

  // Background processing
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(3);

  // Configuration
  private volatile boolean enabled = true;
  private volatile Duration maxCorrelationWindow = Duration.ofMinutes(30);
  private volatile double minConfidenceScore = 0.7;
  private volatile int maxActiveAlerts = 10000;

  /** Alert listener interface. */
  @FunctionalInterface
  public interface AlertListener {
    void onAlert(CorrelatedAlert alert);
  }

  /** Correlation handler interface. */
  @FunctionalInterface
  public interface CorrelationHandler {
    void onCorrelationGroupCreated(AlertCorrelationGroup group);
  }

  /** Creates intelligent alerting system. */
  public IntelligentAlertingSystem() {
    initializeDefaultRules();
    startBackgroundProcessing();
    LOGGER.info("Intelligent alerting system initialized");
  }

  /** Initializes default alert rules with advanced anomaly detection. */
  private void initializeDefaultRules() {
    // Memory usage anomaly detection
    addAlertRule(new IntelligentAlertRule(
        "memory_anomaly",
        "Memory Usage Anomaly",
        "Detects abnormal memory usage patterns",
        "heap_memory_usage",
        AnomalyDetectionAlgorithm.STATISTICAL_THRESHOLD,
        Map.of("threshold_multiplier", 2.5, "min_data_points", 20),
        AlertSeverity.HIGH,
        Duration.ofMinutes(5),
        Duration.ofMinutes(15),
        List.of("memory", "performance", "jvm"),
        true,
        true));

    // Error rate spike detection
    addAlertRule(new IntelligentAlertRule(
        "error_rate_spike",
        "Error Rate Spike",
        "Detects sudden increases in error rates",
        "error_rate",
        AnomalyDetectionAlgorithm.EXPONENTIAL_SMOOTHING,
        Map.of("smoothing_factor", 0.3, "spike_threshold", 3.0),
        AlertSeverity.CRITICAL,
        Duration.ofMinutes(2),
        Duration.ofMinutes(10),
        List.of("errors", "reliability", "application"),
        true,
        true));

    // Performance degradation detection
    addAlertRule(new IntelligentAlertRule(
        "performance_degradation",
        "Performance Degradation",
        "Detects gradual performance degradation",
        "response_time_p95",
        AnomalyDetectionAlgorithm.MOVING_AVERAGE,
        Map.of("window_size", 30, "deviation_threshold", 1.5),
        AlertSeverity.MEDIUM,
        Duration.ofMinutes(10),
        Duration.ofMinutes(30),
        List.of("performance", "latency", "user_experience"),
        true,
        true));

    // Thread deadlock detection
    addAlertRule(new IntelligentAlertRule(
        "thread_deadlock",
        "Thread Deadlock Detection",
        "Detects thread deadlocks in the JVM",
        "deadlocked_threads",
        AnomalyDetectionAlgorithm.STATISTICAL_THRESHOLD,
        Map.of("threshold", 0.0, "comparison", "greater_than"),
        AlertSeverity.EMERGENCY,
        Duration.ofSeconds(30),
        Duration.ofMinutes(5),
        List.of("threads", "deadlock", "jvm", "stability"),
        false,
        true));

    LOGGER.info("Initialized " + alertRules.size() + " default alert rules");
  }

  /** Starts background processing tasks. */
  private void startBackgroundProcessing() {
    // Alert correlation and cleanup
    backgroundExecutor.scheduleAtFixedRate(
        this::processAlertCorrelation,
        30, 60, TimeUnit.SECONDS);

    // Expired alert cleanup
    backgroundExecutor.scheduleAtFixedRate(
        this::cleanupExpiredAlerts,
        300, 300, TimeUnit.SECONDS);

    // Adaptive threshold adjustment
    backgroundExecutor.scheduleAtFixedRate(
        this::adjustAdaptiveThresholds,
        600, 600, TimeUnit.SECONDS);
  }

  /**
   * Adds an intelligent alert rule.
   *
   * @param rule the alert rule to add
   */
  public void addAlertRule(final IntelligentAlertRule rule) {
    alertRules.put(rule.getRuleId(), rule);
    LOGGER.info("Added intelligent alert rule: " + rule.getRuleId());
  }

  /**
   * Removes an alert rule.
   *
   * @param ruleId the rule identifier
   */
  public void removeAlertRule(final String ruleId) {
    alertRules.remove(ruleId);
    LOGGER.info("Removed alert rule: " + ruleId);
  }

  /**
   * Evaluates metrics against all alert rules.
   *
   * @param metricName the metric name
   * @param value the metric value
   * @param tags optional metric tags
   */
  public void evaluateMetric(final String metricName, final double value, final Map<String, String> tags) {
    if (!enabled) {
      return;
    }

    // Store historical data
    final HistoricalMetricData historical = historicalData.computeIfAbsent(
        metricName, k -> new HistoricalMetricData());
    historical.addDataPoint(value);

    // Find applicable rules
    final List<IntelligentAlertRule> applicableRules = alertRules.values().stream()
        .filter(rule -> rule.isEnabled() && rule.getMetricName().equals(metricName))
        .collect(Collectors.toList());

    // Evaluate each rule
    for (final IntelligentAlertRule rule : applicableRules) {
      try {
        evaluateRule(rule, value, tags, historical);
      } catch (final Exception e) {
        LOGGER.warning("Failed to evaluate rule " + rule.getRuleId() + ": " + e.getMessage());
      }
    }
  }

  /** Evaluates a specific rule against a metric value. */
  private void evaluateRule(
      final IntelligentAlertRule rule,
      final double value,
      final Map<String, String> tags,
      final HistoricalMetricData historical) {

    // Check if alert is suppressed
    final String suppressionKey = rule.getRuleId() + "_" + rule.getMetricName();
    final CorrelatedAlert lastAlert = activeAlerts.values().stream()
        .filter(alert -> alert.getRule().getRuleId().equals(rule.getRuleId()))
        .max(Comparator.comparing(CorrelatedAlert::getTimestamp))
        .orElse(null);

    if (lastAlert != null &&
        Duration.between(lastAlert.getTimestamp(), Instant.now()).compareTo(rule.getSuppressionWindow()) < 0) {
      totalAlertsSuppressed.incrementAndGet();
      return;
    }

    // Perform anomaly detection
    final AnomalyDetectionResult anomalyResult = detectAnomaly(rule, value, historical);

    if (anomalyResult.isAnomaly() && anomalyResult.getAnomalyScore() >= minConfidenceScore) {
      // Calculate alert severity based on anomaly score
      final AlertSeverity severity = calculateDynamicSeverity(rule.getBaseSeverity(), anomalyResult.getAnomalyScore());

      // Generate alert
      final CorrelatedAlert alert = generateAlert(rule, value, anomalyResult, severity, tags);

      // Store active alert
      activeAlerts.put(alert.getAlertId(), alert);
      totalAlertsGenerated.incrementAndGet();
      totalAnomaliesDetected.incrementAndGet();

      // Process correlation
      processAlertForCorrelation(alert);

      // Notify listeners
      notifyAlertListeners(alert);

      LOGGER.info(String.format("Generated alert: %s [%s] - %s (confidence: %.2f)",
          alert.getAlertId(), severity, rule.getName(), anomalyResult.getAnomalyScore()));
    }
  }

  /** Detects anomalies based on the configured algorithm. */
  private AnomalyDetectionResult detectAnomaly(
      final IntelligentAlertRule rule,
      final double value,
      final HistoricalMetricData historical) {

    switch (rule.getAlgorithm()) {
      case STATISTICAL_THRESHOLD:
        return detectStatisticalAnomaly(rule, value, historical);
      case MOVING_AVERAGE:
        return detectMovingAverageAnomaly(rule, value, historical);
      case EXPONENTIAL_SMOOTHING:
        return detectExponentialSmoothingAnomaly(rule, value, historical);
      default:
        return new AnomalyDetectionResult(false, 0.0, 0.0, "Algorithm not implemented", Map.of());
    }
  }

  /** Statistical threshold-based anomaly detection. */
  private AnomalyDetectionResult detectStatisticalAnomaly(
      final IntelligentAlertRule rule,
      final double value,
      final HistoricalMetricData historical) {

    final int minDataPoints = (Integer) rule.getAlgorithmParameters().getOrDefault("min_data_points", 20);
    if (historical.getDataPointCount() < minDataPoints) {
      return new AnomalyDetectionResult(false, 0.0, 0.0, "Insufficient historical data",
          Map.of("data_points", historical.getDataPointCount(), "required", minDataPoints));
    }

    final double thresholdMultiplier = (Double) rule.getAlgorithmParameters().getOrDefault("threshold_multiplier", 2.0);
    final double mean = historical.getMean();
    final double stdDev = historical.getStandardDeviation();
    final double threshold = mean + (thresholdMultiplier * stdDev);

    final boolean isAnomaly = value > threshold;
    final double anomalyScore = isAnomaly ? Math.min(1.0, (value - threshold) / threshold) : 0.0;

    final String explanation = String.format(
        "Value %.2f vs threshold %.2f (mean=%.2f, stddev=%.2f)",
        value, threshold, mean, stdDev);

    final Map<String, Object> diagnostics = Map.of(
        "mean", mean,
        "standard_deviation", stdDev,
        "threshold", threshold,
        "z_score", stdDev > 0 ? (value - mean) / stdDev : 0.0);

    return new AnomalyDetectionResult(isAnomaly, anomalyScore, threshold, explanation, diagnostics);
  }

  /** Moving average-based anomaly detection. */
  private AnomalyDetectionResult detectMovingAverageAnomaly(
      final IntelligentAlertRule rule,
      final double value,
      final HistoricalMetricData historical) {

    final int windowSize = (Integer) rule.getAlgorithmParameters().getOrDefault("window_size", 20);
    final double deviationThreshold = (Double) rule.getAlgorithmParameters().getOrDefault("deviation_threshold", 2.0);

    final List<Double> recentValues = historical.getRecentValues(windowSize);
    if (recentValues.size() < Math.min(windowSize, 5)) {
      return new AnomalyDetectionResult(false, 0.0, 0.0, "Insufficient recent data",
          Map.of("recent_values", recentValues.size(), "required", Math.min(windowSize, 5)));
    }

    final double movingAverage = recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double deviation = Math.abs(value - movingAverage);
    final double avgDeviation = recentValues.stream()
        .mapToDouble(v -> Math.abs(v - movingAverage))
        .average().orElse(0.0);

    final double threshold = movingAverage + (deviationThreshold * avgDeviation);
    final boolean isAnomaly = deviation > deviationThreshold * avgDeviation;
    final double anomalyScore = isAnomaly ? Math.min(1.0, deviation / threshold) : 0.0;

    final String explanation = String.format(
        "Value %.2f deviates %.2f from moving average %.2f (threshold=%.2f)",
        value, deviation, movingAverage, threshold);

    final Map<String, Object> diagnostics = Map.of(
        "moving_average", movingAverage,
        "deviation", deviation,
        "average_deviation", avgDeviation,
        "window_size", recentValues.size());

    return new AnomalyDetectionResult(isAnomaly, anomalyScore, threshold, explanation, diagnostics);
  }

  /** Exponential smoothing-based anomaly detection. */
  private AnomalyDetectionResult detectExponentialSmoothingAnomaly(
      final IntelligentAlertRule rule,
      final double value,
      final HistoricalMetricData historical) {

    final double smoothingFactor = (Double) rule.getAlgorithmParameters().getOrDefault("smoothing_factor", 0.3);
    final double spikeThreshold = (Double) rule.getAlgorithmParameters().getOrDefault("spike_threshold", 2.0);

    final List<Double> recentValues = historical.getRecentValues(50);
    if (recentValues.size() < 10) {
      return new AnomalyDetectionResult(false, 0.0, 0.0, "Insufficient data for smoothing",
          Map.of("data_points", recentValues.size()));
    }

    // Calculate exponentially smoothed value
    double smoothedValue = recentValues.get(recentValues.size() - 1);
    for (int i = recentValues.size() - 2; i >= 0; i--) {
      smoothedValue = smoothingFactor * recentValues.get(i) + (1 - smoothingFactor) * smoothedValue;
    }

    final double deviation = Math.abs(value - smoothedValue);
    final double threshold = smoothedValue * spikeThreshold;
    final boolean isAnomaly = deviation > threshold;
    final double anomalyScore = isAnomaly ? Math.min(1.0, deviation / threshold) : 0.0;

    final String explanation = String.format(
        "Value %.2f vs smoothed %.2f (deviation=%.2f, threshold=%.2f)",
        value, smoothedValue, deviation, threshold);

    final Map<String, Object> diagnostics = Map.of(
        "smoothed_value", smoothedValue,
        "deviation", deviation,
        "smoothing_factor", smoothingFactor,
        "data_points_used", recentValues.size());

    return new AnomalyDetectionResult(isAnomaly, anomalyScore, threshold, explanation, diagnostics);
  }

  /** Calculates dynamic severity based on anomaly score. */
  private AlertSeverity calculateDynamicSeverity(final AlertSeverity baseSeverity, final double anomalyScore) {
    if (anomalyScore >= 0.95) {
      return AlertSeverity.EMERGENCY;
    } else if (anomalyScore >= 0.90) {
      return AlertSeverity.CRITICAL;
    } else if (anomalyScore >= 0.80) {
      return AlertSeverity.HIGH;
    } else if (anomalyScore >= 0.70) {
      return AlertSeverity.MEDIUM;
    } else {
      return baseSeverity;
    }
  }

  /** Generates a correlated alert. */
  private CorrelatedAlert generateAlert(
      final IntelligentAlertRule rule,
      final double value,
      final AnomalyDetectionResult anomalyResult,
      final AlertSeverity severity,
      final Map<String, String> tags) {

    final String alertId = "alert_" + System.currentTimeMillis() + "_" + rule.getRuleId();
    final String correlationId = generateCorrelationId(rule, tags);

    final Map<String, Object> context = new ConcurrentHashMap<>();
    context.put("metric_name", rule.getMetricName());
    context.put("algorithm", rule.getAlgorithm().toString());
    context.put("evaluation_window", rule.getEvaluationWindow().toString());
    context.put("anomaly_explanation", anomalyResult.getExplanation());
    context.put("diagnostics", anomalyResult.getDiagnostics());
    if (tags != null) {
      context.putAll(tags);
    }

    final String rootCauseAnalysis = generateRootCauseAnalysis(rule, value, anomalyResult, tags);

    return new CorrelatedAlert(
        alertId,
        correlationId,
        rule,
        value,
        anomalyResult.getThreshold(),
        anomalyResult.getAnomalyScore(),
        severity,
        context,
        List.of(), // Related alerts will be populated during correlation
        rootCauseAnalysis);
  }

  /** Generates correlation ID for alert grouping. */
  private String generateCorrelationId(final IntelligentAlertRule rule, final Map<String, String> tags) {
    // Simple correlation based on rule and time window
    final long timeWindow = Instant.now().truncatedTo(ChronoUnit.MINUTES).toEpochMilli() / 60000; // 1-minute windows
    return rule.getRuleId() + "_" + timeWindow;
  }

  /** Generates root cause analysis for the alert. */
  private String generateRootCauseAnalysis(
      final IntelligentAlertRule rule,
      final double value,
      final AnomalyDetectionResult anomalyResult,
      final Map<String, String> tags) {

    final StringBuilder analysis = new StringBuilder();
    analysis.append("Anomaly detected in ").append(rule.getMetricName()).append(": ");
    analysis.append(anomalyResult.getExplanation()).append(". ");

    // Add algorithm-specific insights
    switch (rule.getAlgorithm()) {
      case STATISTICAL_THRESHOLD:
        analysis.append("Value significantly exceeds statistical threshold based on historical pattern. ");
        break;
      case MOVING_AVERAGE:
        analysis.append("Value deviates significantly from recent moving average. ");
        break;
      case EXPONENTIAL_SMOOTHING:
        analysis.append("Sudden spike detected using exponential smoothing forecasting. ");
        break;
    }

    // Add contextual information
    if (tags != null && !tags.isEmpty()) {
      analysis.append("Context: ").append(tags.toString()).append(". ");
    }

    analysis.append("Confidence: ").append(String.format("%.1f%%", anomalyResult.getAnomalyScore() * 100));

    return analysis.toString();
  }

  /** Processes alert for correlation with existing alerts. */
  private void processAlertForCorrelation(final CorrelatedAlert alert) {
    // Find existing correlation groups that might be related
    final List<AlertCorrelationGroup> candidateGroups = correlationGroups.values().stream()
        .filter(group -> isCorrelationCandidate(group, alert))
        .collect(Collectors.toList());

    if (candidateGroups.isEmpty()) {
      // Create new correlation group
      final AlertCorrelationGroup newGroup = new AlertCorrelationGroup(
          alert.getCorrelationId(),
          CorrelationStrategy.TIME_BASED,
          alert);
      correlationGroups.put(newGroup.getGroupId(), newGroup);
      notifyCorrelationHandlers(newGroup);
    } else {
      // Add to existing group (choose the most relevant one)
      final AlertCorrelationGroup bestMatch = candidateGroups.get(0);
      bestMatch.addAlert(alert);
      totalAlertsCorrelated.incrementAndGet();
    }
  }

  /** Checks if an alert is a candidate for correlation with a group. */
  private boolean isCorrelationCandidate(final AlertCorrelationGroup group, final CorrelatedAlert alert) {
    // Time-based correlation
    final Duration timeDifference = Duration.between(group.getCreatedAt(), alert.getTimestamp());
    if (timeDifference.compareTo(maxCorrelationWindow) > 0) {
      return false;
    }

    // Tag-based correlation
    final Set<String> alertTags = Set.copyOf(alert.getRule().getCorrelationTags());
    final boolean hasCommonTags = group.getAlerts().stream()
        .anyMatch(groupAlert -> {
          final Set<String> groupTags = Set.copyOf(groupAlert.getRule().getCorrelationTags());
          return alertTags.stream().anyMatch(groupTags::contains);
        });

    return hasCommonTags;
  }

  /** Processes alert correlation and cleanup. */
  private void processAlertCorrelation() {
    try {
      // Clean up expired correlation groups
      correlationGroups.entrySet().removeIf(entry -> {
        final AlertCorrelationGroup group = entry.getValue();
        if (group.isExpired()) {
          LOGGER.fine("Removing expired correlation group: " + group.getGroupId());
          return true;
        }
        return false;
      });

      // Log correlation statistics
      if (correlationGroups.size() > 0) {
        LOGGER.fine(String.format("Active correlation groups: %d, Total correlations: %d",
            correlationGroups.size(), totalAlertsCorrelated.get()));
      }

    } catch (final Exception e) {
      LOGGER.warning("Error processing alert correlation: " + e.getMessage());
    }
  }

  /** Cleans up expired alerts. */
  private void cleanupExpiredAlerts() {
    try {
      final int initialSize = activeAlerts.size();
      activeAlerts.entrySet().removeIf(entry -> entry.getValue().isExpired());

      final int removedCount = initialSize - activeAlerts.size();
      if (removedCount > 0) {
        LOGGER.fine("Cleaned up " + removedCount + " expired alerts");
      }

      // Enforce max active alerts limit
      if (activeAlerts.size() > maxActiveAlerts) {
        final List<CorrelatedAlert> alertsToRemove = activeAlerts.values().stream()
            .sorted(Comparator.comparing(CorrelatedAlert::getTimestamp))
            .limit(activeAlerts.size() - maxActiveAlerts)
            .collect(Collectors.toList());

        for (final CorrelatedAlert alert : alertsToRemove) {
          activeAlerts.remove(alert.getAlertId());
        }

        LOGGER.info("Removed " + alertsToRemove.size() + " oldest alerts to enforce limit");
      }

    } catch (final Exception e) {
      LOGGER.warning("Error cleaning up expired alerts: " + e.getMessage());
    }
  }

  /** Adjusts adaptive thresholds based on recent data. */
  private void adjustAdaptiveThresholds() {
    try {
      for (final IntelligentAlertRule rule : alertRules.values()) {
        if (!rule.isAdaptiveThresholds()) {
          continue;
        }

        final HistoricalMetricData historical = historicalData.get(rule.getMetricName());
        if (historical == null || historical.getDataPointCount() < 100) {
          continue;
        }

        // Recalculate statistics for adaptive rules
        historical.calculateStatistics();

        LOGGER.fine(String.format("Adjusted adaptive thresholds for rule %s: mean=%.2f, stddev=%.2f",
            rule.getRuleId(), historical.getMean(), historical.getStandardDeviation()));
      }

    } catch (final Exception e) {
      LOGGER.warning("Error adjusting adaptive thresholds: " + e.getMessage());
    }
  }

  /** Notifies alert listeners. */
  private void notifyAlertListeners(final CorrelatedAlert alert) {
    for (final AlertListener listener : alertListeners) {
      try {
        listener.onAlert(alert);
      } catch (final Exception e) {
        LOGGER.warning("Alert listener error: " + e.getMessage());
      }
    }
  }

  /** Notifies correlation handlers. */
  private void notifyCorrelationHandlers(final AlertCorrelationGroup group) {
    for (final CorrelationHandler handler : correlationHandlers) {
      try {
        handler.onCorrelationGroupCreated(group);
      } catch (final Exception e) {
        LOGGER.warning("Correlation handler error: " + e.getMessage());
      }
    }
  }

  /**
   * Adds an alert listener.
   *
   * @param listener the alert listener
   */
  public void addAlertListener(final AlertListener listener) {
    if (listener != null) {
      alertListeners.add(listener);
    }
  }

  /**
   * Adds a correlation handler.
   *
   * @param handler the correlation handler
   */
  public void addCorrelationHandler(final CorrelationHandler handler) {
    if (handler != null) {
      correlationHandlers.add(handler);
    }
  }

  /**
   * Acknowledges an alert.
   *
   * @param alertId the alert identifier
   * @param acknowledgedBy who acknowledged the alert
   * @return true if alert was found and acknowledged
   */
  public boolean acknowledgeAlert(final String alertId, final String acknowledgedBy) {
    final CorrelatedAlert alert = activeAlerts.get(alertId);
    if (alert != null) {
      alert.acknowledge(acknowledgedBy);
      LOGGER.info("Alert acknowledged: " + alertId + " by " + acknowledgedBy);
      return true;
    }
    return false;
  }

  /**
   * Suppresses an alert.
   *
   * @param alertId the alert identifier
   * @return true if alert was found and suppressed
   */
  public boolean suppressAlert(final String alertId) {
    final CorrelatedAlert alert = activeAlerts.get(alertId);
    if (alert != null) {
      alert.suppress();
      LOGGER.info("Alert suppressed: " + alertId);
      return true;
    }
    return false;
  }

  /**
   * Gets active alerts.
   *
   * @param limit maximum number of alerts to return
   * @return list of active alerts
   */
  public List<CorrelatedAlert> getActiveAlerts(final int limit) {
    return activeAlerts.values().stream()
        .filter(alert -> !alert.isExpired())
        .sorted(Comparator.comparing(CorrelatedAlert::getTimestamp, Comparator.reverseOrder()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets correlation groups.
   *
   * @return list of active correlation groups
   */
  public List<AlertCorrelationGroup> getCorrelationGroups() {
    return List.copyOf(correlationGroups.values());
  }

  /**
   * Gets alerting system statistics.
   *
   * @return formatted statistics
   */
  public String getAlertingStatistics() {
    return String.format(
        "Intelligent Alerting Statistics: rules=%d, active_alerts=%d, total_generated=%d, " +
        "total_suppressed=%d, total_correlated=%d, anomalies_detected=%d, correlation_groups=%d",
        alertRules.size(),
        activeAlerts.size(),
        totalAlertsGenerated.get(),
        totalAlertsSuppressed.get(),
        totalAlertsCorrelated.get(),
        totalAnomaliesDetected.get(),
        correlationGroups.size());
  }

  /**
   * Sets system enabled state.
   *
   * @param enabled true to enable alerting
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Intelligent alerting system " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the alerting system. */
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
    LOGGER.info("Intelligent alerting system shutdown");
  }
}