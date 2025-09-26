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
 * Advanced performance baseline tracking and drift detection system providing intelligent
 * performance monitoring, trend analysis, capacity planning, and proactive performance
 * management.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Automated performance baseline establishment and maintenance
 *   <li>Statistical drift detection using multiple algorithms
 *   <li>Performance trend analysis and forecasting
 *   <li>Capacity planning and scaling recommendations
 *   <li>Performance regression detection and alerting
 *   <li>Seasonal pattern recognition and adjustment
 *   <li>Multi-dimensional performance analysis
 * </ul>
 *
 * @since 1.0.0
 */
public final class PerformanceBaselineTracker {

  private static final Logger LOGGER = Logger.getLogger(PerformanceBaselineTracker.class.getName());

  /** Performance drift detection algorithms. */
  public enum DriftDetectionAlgorithm {
    STATISTICAL_CONTROL("Statistical Process Control", "Control chart-based drift detection"),
    CUSUM("Cumulative Sum", "Cumulative sum change detection"),
    EXPONENTIAL_WEIGHTED("Exponentially Weighted Moving Average", "EWMA-based drift detection"),
    TREND_ANALYSIS("Trend Analysis", "Linear regression trend detection"),
    SEASONAL_DECOMPOSITION("Seasonal Decomposition", "Time series decomposition analysis"),
    MACHINE_LEARNING("Machine Learning", "ML-based anomaly detection");

    private final String displayName;
    private final String description;

    DriftDetectionAlgorithm(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Performance drift severity levels. */
  public enum DriftSeverity {
    NORMAL(0, "Within normal variation"),
    MINOR(1, "Minor drift detected"),
    MODERATE(2, "Moderate drift requiring attention"),
    SIGNIFICANT(3, "Significant drift requiring action"),
    CRITICAL(4, "Critical drift requiring immediate action");

    private final int level;
    private final String description;

    DriftSeverity(final int level, final String description) {
      this.level = level;
      this.description = description;
    }

    public int getLevel() { return level; }
    public String getDescription() { return description; }

    public boolean isWorseThan(final DriftSeverity other) {
      return this.level > other.level;
    }
  }

  /** Performance baseline configuration. */
  public static final class BaselineConfig {
    private final String metricName;
    private final Duration baselineWindow;
    private final Duration updateInterval;
    private final int minDataPoints;
    private final double confidenceLevel;
    private final List<DriftDetectionAlgorithm> algorithms;
    private final Map<String, Object> algorithmParameters;
    private final boolean adaptiveBaseline;
    private final boolean seasonalAdjustment;

    public BaselineConfig(
        final String metricName,
        final Duration baselineWindow,
        final Duration updateInterval,
        final int minDataPoints,
        final double confidenceLevel,
        final List<DriftDetectionAlgorithm> algorithms,
        final Map<String, Object> algorithmParameters,
        final boolean adaptiveBaseline,
        final boolean seasonalAdjustment) {
      this.metricName = metricName;
      this.baselineWindow = baselineWindow != null ? baselineWindow : Duration.ofDays(7);
      this.updateInterval = updateInterval != null ? updateInterval : Duration.ofHours(1);
      this.minDataPoints = minDataPoints > 0 ? minDataPoints : 50;
      this.confidenceLevel = confidenceLevel > 0 ? confidenceLevel : 0.95;
      this.algorithms = List.copyOf(algorithms != null ? algorithms :
          List.of(DriftDetectionAlgorithm.STATISTICAL_CONTROL, DriftDetectionAlgorithm.TREND_ANALYSIS));
      this.algorithmParameters = Map.copyOf(algorithmParameters != null ? algorithmParameters : Map.of());
      this.adaptiveBaseline = adaptiveBaseline;
      this.seasonalAdjustment = seasonalAdjustment;
    }

    // Getters
    public String getMetricName() { return metricName; }
    public Duration getBaselineWindow() { return baselineWindow; }
    public Duration getUpdateInterval() { return updateInterval; }
    public int getMinDataPoints() { return minDataPoints; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public List<DriftDetectionAlgorithm> getAlgorithms() { return algorithms; }
    public Map<String, Object> getAlgorithmParameters() { return algorithmParameters; }
    public boolean isAdaptiveBaseline() { return adaptiveBaseline; }
    public boolean isSeasonalAdjustment() { return seasonalAdjustment; }
  }

  /** Performance baseline data. */
  public static final class PerformanceBaseline {
    private final String metricName;
    private final Instant establishedAt;
    private volatile Instant lastUpdated;
    private volatile double mean;
    private volatile double standardDeviation;
    private volatile double median;
    private volatile double percentile95;
    private volatile double percentile99;
    private volatile double upperControlLimit;
    private volatile double lowerControlLimit;
    private volatile int dataPointCount;
    private volatile double trendSlope;
    private volatile double seasonalVariation;
    private final List<DataPoint> historicalData;

    public PerformanceBaseline(final String metricName) {
      this.metricName = metricName;
      this.establishedAt = Instant.now();
      this.lastUpdated = this.establishedAt;
      this.historicalData = new CopyOnWriteArrayList<>();
    }

    // Getters
    public String getMetricName() { return metricName; }
    public Instant getEstablishedAt() { return establishedAt; }
    public Instant getLastUpdated() { return lastUpdated; }
    public double getMean() { return mean; }
    public double getStandardDeviation() { return standardDeviation; }
    public double getMedian() { return median; }
    public double getPercentile95() { return percentile95; }
    public double getPercentile99() { return percentile99; }
    public double getUpperControlLimit() { return upperControlLimit; }
    public double getLowerControlLimit() { return lowerControlLimit; }
    public int getDataPointCount() { return dataPointCount; }
    public double getTrendSlope() { return trendSlope; }
    public double getSeasonalVariation() { return seasonalVariation; }
    public List<DataPoint> getHistoricalData() { return List.copyOf(historicalData); }

    public void addDataPoint(final double value, final Instant timestamp) {
      historicalData.add(new DataPoint(value, timestamp));
      // Keep only recent data points to prevent memory growth
      if (historicalData.size() > 10000) {
        historicalData.removeIf(dp -> dp.timestamp.isBefore(Instant.now().minus(Duration.ofDays(30))));
      }
    }

    public void updateStatistics(
        final double mean,
        final double standardDeviation,
        final double median,
        final double percentile95,
        final double percentile99,
        final double upperControlLimit,
        final double lowerControlLimit,
        final int dataPointCount,
        final double trendSlope,
        final double seasonalVariation) {
      this.mean = mean;
      this.standardDeviation = standardDeviation;
      this.median = median;
      this.percentile95 = percentile95;
      this.percentile99 = percentile99;
      this.upperControlLimit = upperControlLimit;
      this.lowerControlLimit = lowerControlLimit;
      this.dataPointCount = dataPointCount;
      this.trendSlope = trendSlope;
      this.seasonalVariation = seasonalVariation;
      this.lastUpdated = Instant.now();
    }

    public Duration getAge() {
      return Duration.between(establishedAt, Instant.now());
    }

    public boolean isStable() {
      return dataPointCount >= 50 && standardDeviation > 0 && getAge().toDays() >= 1;
    }

    static class DataPoint {
      final double value;
      final Instant timestamp;

      DataPoint(final double value, final Instant timestamp) {
        this.value = value;
        this.timestamp = timestamp;
      }
    }
  }

  /** Performance drift detection result. */
  public static final class DriftDetectionResult {
    private final String metricName;
    private final DriftDetectionAlgorithm algorithm;
    private final DriftSeverity severity;
    private final double currentValue;
    private final double baselineValue;
    private final double driftMagnitude;
    private final double confidenceScore;
    private final String explanation;
    private final Map<String, Object> diagnostics;
    private final Instant detectedAt;
    private final List<String> recommendations;

    public DriftDetectionResult(
        final String metricName,
        final DriftDetectionAlgorithm algorithm,
        final DriftSeverity severity,
        final double currentValue,
        final double baselineValue,
        final double driftMagnitude,
        final double confidenceScore,
        final String explanation,
        final Map<String, Object> diagnostics,
        final List<String> recommendations) {
      this.metricName = metricName;
      this.algorithm = algorithm;
      this.severity = severity;
      this.currentValue = currentValue;
      this.baselineValue = baselineValue;
      this.driftMagnitude = driftMagnitude;
      this.confidenceScore = confidenceScore;
      this.explanation = explanation;
      this.diagnostics = Map.copyOf(diagnostics != null ? diagnostics : Map.of());
      this.detectedAt = Instant.now();
      this.recommendations = List.copyOf(recommendations != null ? recommendations : List.of());
    }

    // Getters
    public String getMetricName() { return metricName; }
    public DriftDetectionAlgorithm getAlgorithm() { return algorithm; }
    public DriftSeverity getSeverity() { return severity; }
    public double getCurrentValue() { return currentValue; }
    public double getBaselineValue() { return baselineValue; }
    public double getDriftMagnitude() { return driftMagnitude; }
    public double getConfidenceScore() { return confidenceScore; }
    public String getExplanation() { return explanation; }
    public Map<String, Object> getDiagnostics() { return diagnostics; }
    public Instant getDetectedAt() { return detectedAt; }
    public List<String> getRecommendations() { return recommendations; }

    public boolean isDriftDetected() { return severity != DriftSeverity.NORMAL; }
  }

  /** Performance forecasting result. */
  public static final class PerformanceForecast {
    private final String metricName;
    private final Duration forecastHorizon;
    private final double forecastValue;
    private final double confidenceInterval;
    private final double upperBound;
    private final double lowerBound;
    private final String forecastMethod;
    private final Map<String, Object> modelParameters;
    private final Instant generatedAt;

    public PerformanceForecast(
        final String metricName,
        final Duration forecastHorizon,
        final double forecastValue,
        final double confidenceInterval,
        final double upperBound,
        final double lowerBound,
        final String forecastMethod,
        final Map<String, Object> modelParameters) {
      this.metricName = metricName;
      this.forecastHorizon = forecastHorizon;
      this.forecastValue = forecastValue;
      this.confidenceInterval = confidenceInterval;
      this.upperBound = upperBound;
      this.lowerBound = lowerBound;
      this.forecastMethod = forecastMethod;
      this.modelParameters = Map.copyOf(modelParameters != null ? modelParameters : Map.of());
      this.generatedAt = Instant.now();
    }

    // Getters
    public String getMetricName() { return metricName; }
    public Duration getForecastHorizon() { return forecastHorizon; }
    public double getForecastValue() { return forecastValue; }
    public double getConfidenceInterval() { return confidenceInterval; }
    public double getUpperBound() { return upperBound; }
    public double getLowerBound() { return lowerBound; }
    public String getForecastMethod() { return forecastMethod; }
    public Map<String, Object> getModelParameters() { return modelParameters; }
    public Instant getGeneratedAt() { return generatedAt; }
  }

  /** Capacity planning recommendation. */
  public static final class CapacityRecommendation {
    private final String metricName;
    private final String recommendationType;
    private final String description;
    private final double currentCapacity;
    private final double recommendedCapacity;
    private final Duration timeToCapacityLimit;
    private final double confidenceLevel;
    private final List<String> actionItems;
    private final Map<String, Object> analysisData;
    private final Instant generatedAt;

    public CapacityRecommendation(
        final String metricName,
        final String recommendationType,
        final String description,
        final double currentCapacity,
        final double recommendedCapacity,
        final Duration timeToCapacityLimit,
        final double confidenceLevel,
        final List<String> actionItems,
        final Map<String, Object> analysisData) {
      this.metricName = metricName;
      this.recommendationType = recommendationType;
      this.description = description;
      this.currentCapacity = currentCapacity;
      this.recommendedCapacity = recommendedCapacity;
      this.timeToCapacityLimit = timeToCapacityLimit;
      this.confidenceLevel = confidenceLevel;
      this.actionItems = List.copyOf(actionItems != null ? actionItems : List.of());
      this.analysisData = Map.copyOf(analysisData != null ? analysisData : Map.of());
      this.generatedAt = Instant.now();
    }

    // Getters
    public String getMetricName() { return metricName; }
    public String getRecommendationType() { return recommendationType; }
    public String getDescription() { return description; }
    public double getCurrentCapacity() { return currentCapacity; }
    public double getRecommendedCapacity() { return recommendedCapacity; }
    public Duration getTimeToCapacityLimit() { return timeToCapacityLimit; }
    public double getConfidenceLevel() { return confidenceLevel; }
    public List<String> getActionItems() { return actionItems; }
    public Map<String, Object> getAnalysisData() { return analysisData; }
    public Instant getGeneratedAt() { return generatedAt; }
  }

  // Instance fields
  private final ConcurrentHashMap<String, BaselineConfig> baselineConfigs = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PerformanceBaseline> baselines = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, List<DriftDetectionResult>> driftHistory = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PerformanceForecast> forecasts = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CapacityRecommendation> capacityRecommendations = new ConcurrentHashMap<>();

  // Drift detection listeners
  private final List<DriftDetectionListener> driftListeners = new CopyOnWriteArrayList<>();

  // Statistics and monitoring
  private final AtomicLong totalDriftDetections = new AtomicLong(0);
  private final AtomicLong totalBaselinesEstablished = new AtomicLong(0);
  private final AtomicLong totalForecastsGenerated = new AtomicLong(0);
  private final AtomicReference<Instant> lastAnalysisRun = new AtomicReference<>(Instant.now());

  // Background processing
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(3);

  // Configuration
  private volatile boolean driftDetectionEnabled = true;
  private volatile boolean adaptiveBaselinesEnabled = true;
  private volatile Duration analysisInterval = Duration.ofMinutes(15);

  /** Drift detection event listener. */
  @FunctionalInterface
  public interface DriftDetectionListener {
    void onDriftDetected(DriftDetectionResult result);
  }

  /** Creates performance baseline tracker. */
  public PerformanceBaselineTracker() {
    initializeDefaultConfigurations();
    startBackgroundProcessing();
    LOGGER.info("Performance baseline tracker initialized");
  }

  /** Initializes default baseline configurations. */
  private void initializeDefaultConfigurations() {
    // Memory usage baseline
    addBaselineConfig(new BaselineConfig(
        "heap_memory_usage",
        Duration.ofDays(7),
        Duration.ofHours(1),
        100,
        0.95,
        List.of(DriftDetectionAlgorithm.STATISTICAL_CONTROL, DriftDetectionAlgorithm.TREND_ANALYSIS),
        Map.of("control_limit_factor", 3.0, "trend_window", 24),
        true,
        true));

    // Response time baseline
    addBaselineConfig(new BaselineConfig(
        "response_time_p95",
        Duration.ofDays(3),
        Duration.ofMinutes(30),
        200,
        0.99,
        List.of(DriftDetectionAlgorithm.EXPONENTIAL_WEIGHTED, DriftDetectionAlgorithm.CUSUM),
        Map.of("ewma_alpha", 0.2, "cusum_threshold", 2.5),
        true,
        false));

    // Error rate baseline
    addBaselineConfig(new BaselineConfig(
        "error_rate",
        Duration.ofDays(5),
        Duration.ofMinutes(15),
        150,
        0.95,
        List.of(DriftDetectionAlgorithm.STATISTICAL_CONTROL, DriftDetectionAlgorithm.CUSUM),
        Map.of("control_limit_factor", 2.5, "cusum_threshold", 1.5),
        true,
        false));

    // Throughput baseline
    addBaselineConfig(new BaselineConfig(
        "requests_per_second",
        Duration.ofDays(7),
        Duration.ofHours(2),
        100,
        0.95,
        List.of(DriftDetectionAlgorithm.SEASONAL_DECOMPOSITION, DriftDetectionAlgorithm.TREND_ANALYSIS),
        Map.of("seasonal_period", 24, "trend_window", 48),
        true,
        true));

    LOGGER.info("Initialized " + baselineConfigs.size() + " default baseline configurations");
  }

  /** Starts background processing tasks. */
  private void startBackgroundProcessing() {
    // Baseline analysis and updates
    backgroundExecutor.scheduleAtFixedRate(
        this::performBaselineAnalysis,
        60, (int) analysisInterval.toSeconds(), TimeUnit.SECONDS);

    // Drift detection
    backgroundExecutor.scheduleAtFixedRate(
        this::performDriftDetection,
        120, 300, TimeUnit.SECONDS); // Every 5 minutes

    // Forecasting and capacity planning
    backgroundExecutor.scheduleAtFixedRate(
        this::performForecastingAndCapacityPlanning,
        300, 3600, TimeUnit.SECONDS); // Every hour
  }

  /**
   * Adds a baseline configuration for a metric.
   *
   * @param config the baseline configuration
   */
  public void addBaselineConfig(final BaselineConfig config) {
    baselineConfigs.put(config.getMetricName(), config);
    baselines.putIfAbsent(config.getMetricName(), new PerformanceBaseline(config.getMetricName()));
    LOGGER.info("Added baseline configuration for metric: " + config.getMetricName());
  }

  /**
   * Records a metric value for baseline tracking.
   *
   * @param metricName the metric name
   * @param value the metric value
   * @param timestamp the timestamp of the measurement
   */
  public void recordMetric(final String metricName, final double value, final Instant timestamp) {
    final PerformanceBaseline baseline = baselines.get(metricName);
    if (baseline != null) {
      baseline.addDataPoint(value, timestamp);
    }
  }

  /**
   * Records a metric value with current timestamp.
   *
   * @param metricName the metric name
   * @param value the metric value
   */
  public void recordMetric(final String metricName, final double value) {
    recordMetric(metricName, value, Instant.now());
  }

  /** Performs baseline analysis and updates. */
  private void performBaselineAnalysis() {
    if (!driftDetectionEnabled) {
      return;
    }

    try {
      for (final BaselineConfig config : baselineConfigs.values()) {
        try {
          updateBaseline(config);
        } catch (final Exception e) {
          LOGGER.warning("Failed to update baseline for " + config.getMetricName() + ": " + e.getMessage());
        }
      }

      lastAnalysisRun.set(Instant.now());

    } catch (final Exception e) {
      LOGGER.warning("Error during baseline analysis: " + e.getMessage());
    }
  }

  /** Updates baseline statistics for a metric. */
  private void updateBaseline(final BaselineConfig config) {
    final PerformanceBaseline baseline = baselines.get(config.getMetricName());
    if (baseline == null) {
      return;
    }

    final List<PerformanceBaseline.DataPoint> recentData = baseline.getHistoricalData().stream()
        .filter(dp -> dp.timestamp.isAfter(Instant.now().minus(config.getBaselineWindow())))
        .sorted(Comparator.comparing(dp -> dp.timestamp))
        .collect(Collectors.toList());

    if (recentData.size() < config.getMinDataPoints()) {
      return; // Not enough data for reliable baseline
    }

    // Calculate basic statistics
    final List<Double> values = recentData.stream().map(dp -> dp.value).collect(Collectors.toList());
    final double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double variance = values.stream()
        .mapToDouble(v -> Math.pow(v - mean, 2))
        .average().orElse(0.0);
    final double standardDeviation = Math.sqrt(variance);

    // Calculate percentiles
    final List<Double> sortedValues = values.stream().sorted().collect(Collectors.toList());
    final double median = calculatePercentile(sortedValues, 50);
    final double percentile95 = calculatePercentile(sortedValues, 95);
    final double percentile99 = calculatePercentile(sortedValues, 99);

    // Calculate control limits
    final double controlLimitFactor = (Double) config.getAlgorithmParameters().getOrDefault("control_limit_factor", 3.0);
    final double upperControlLimit = mean + (controlLimitFactor * standardDeviation);
    final double lowerControlLimit = mean - (controlLimitFactor * standardDeviation);

    // Calculate trend
    final double trendSlope = calculateTrendSlope(recentData);

    // Calculate seasonal variation if enabled
    final double seasonalVariation = config.isSeasonalAdjustment() ?
        calculateSeasonalVariation(recentData) : 0.0;

    baseline.updateStatistics(
        mean,
        standardDeviation,
        median,
        percentile95,
        percentile99,
        upperControlLimit,
        lowerControlLimit,
        recentData.size(),
        trendSlope,
        seasonalVariation);

    if (!baseline.isStable()) {
      totalBaselinesEstablished.incrementAndGet();
      LOGGER.info("Established stable baseline for metric: " + config.getMetricName());
    }
  }

  /** Calculates percentile from sorted values. */
  private double calculatePercentile(final List<Double> sortedValues, final int percentile) {
    if (sortedValues.isEmpty()) {
      return 0.0;
    }

    final int index = (int) Math.ceil((percentile / 100.0) * sortedValues.size()) - 1;
    return sortedValues.get(Math.max(0, Math.min(index, sortedValues.size() - 1)));
  }

  /** Calculates trend slope using linear regression. */
  private double calculateTrendSlope(final List<PerformanceBaseline.DataPoint> dataPoints) {
    if (dataPoints.size() < 2) {
      return 0.0;
    }

    final int n = dataPoints.size();
    double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

    for (int i = 0; i < n; i++) {
      final double x = i; // Use index as x-coordinate
      final double y = dataPoints.get(i).value;
      sumX += x;
      sumY += y;
      sumXY += x * y;
      sumX2 += x * x;
    }

    final double denominator = n * sumX2 - sumX * sumX;
    if (Math.abs(denominator) < 1e-10) {
      return 0.0;
    }

    return (n * sumXY - sumX * sumY) / denominator;
  }

  /** Calculates seasonal variation. */
  private double calculateSeasonalVariation(final List<PerformanceBaseline.DataPoint> dataPoints) {
    if (dataPoints.size() < 24) { // Need at least 24 hours of data
      return 0.0;
    }

    // Simple seasonal variation calculation - in production would use more sophisticated methods
    final Map<Integer, List<Double>> hourlyValues = new ConcurrentHashMap<>();
    for (final PerformanceBaseline.DataPoint dp : dataPoints) {
      final int hour = dp.timestamp.atZone(ZoneId.systemDefault()).getHour();
      hourlyValues.computeIfAbsent(hour, k -> new ArrayList<>()).add(dp.value);
    }

    if (hourlyValues.size() < 12) {
      return 0.0; // Not enough hourly data
    }

    final double globalMean = dataPoints.stream().mapToDouble(dp -> dp.value).average().orElse(0.0);
    return hourlyValues.values().stream()
        .mapToDouble(hourValues -> {
          final double hourMean = hourValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
          return Math.abs(hourMean - globalMean);
        })
        .average().orElse(0.0);
  }

  /** Performs drift detection on all configured metrics. */
  private void performDriftDetection() {
    if (!driftDetectionEnabled) {
      return;
    }

    try {
      for (final BaselineConfig config : baselineConfigs.values()) {
        final PerformanceBaseline baseline = baselines.get(config.getMetricName());
        if (baseline != null && baseline.isStable()) {
          performDriftDetectionForMetric(config, baseline);
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error during drift detection: " + e.getMessage());
    }
  }

  /** Performs drift detection for a specific metric. */
  private void performDriftDetectionForMetric(final BaselineConfig config, final PerformanceBaseline baseline) {
    final List<PerformanceBaseline.DataPoint> recentData = baseline.getHistoricalData().stream()
        .filter(dp -> dp.timestamp.isAfter(Instant.now().minus(Duration.ofMinutes(30))))
        .collect(Collectors.toList());

    if (recentData.isEmpty()) {
      return; // No recent data
    }

    final double currentValue = recentData.stream()
        .mapToDouble(dp -> dp.value)
        .average().orElse(baseline.getMean());

    // Run each configured drift detection algorithm
    for (final DriftDetectionAlgorithm algorithm : config.getAlgorithms()) {
      try {
        final DriftDetectionResult result = runDriftDetectionAlgorithm(
            algorithm, config, baseline, currentValue);

        if (result.isDriftDetected()) {
          // Store drift detection result
          driftHistory.computeIfAbsent(config.getMetricName(), k -> new CopyOnWriteArrayList<>()).add(result);
          totalDriftDetections.incrementAndGet();

          // Notify listeners
          notifyDriftDetectionListeners(result);

          LOGGER.warning(String.format("Performance drift detected: %s [%s] - %s",
              config.getMetricName(), result.getSeverity(), result.getExplanation()));
        }

      } catch (final Exception e) {
        LOGGER.warning("Drift detection algorithm failed for " + config.getMetricName() +
                      " using " + algorithm + ": " + e.getMessage());
      }
    }
  }

  /** Runs a specific drift detection algorithm. */
  private DriftDetectionResult runDriftDetectionAlgorithm(
      final DriftDetectionAlgorithm algorithm,
      final BaselineConfig config,
      final PerformanceBaseline baseline,
      final double currentValue) {

    switch (algorithm) {
      case STATISTICAL_CONTROL:
        return detectStatisticalControlDrift(config, baseline, currentValue);
      case CUSUM:
        return detectCusumDrift(config, baseline, currentValue);
      case EXPONENTIAL_WEIGHTED:
        return detectExponentialWeightedDrift(config, baseline, currentValue);
      case TREND_ANALYSIS:
        return detectTrendDrift(config, baseline, currentValue);
      case SEASONAL_DECOMPOSITION:
        return detectSeasonalDrift(config, baseline, currentValue);
      default:
        return new DriftDetectionResult(
            config.getMetricName(),
            algorithm,
            DriftSeverity.NORMAL,
            currentValue,
            baseline.getMean(),
            0.0,
            0.0,
            "Algorithm not implemented",
            Map.of(),
            List.of());
    }
  }

  /** Statistical process control drift detection. */
  private DriftDetectionResult detectStatisticalControlDrift(
      final BaselineConfig config,
      final PerformanceBaseline baseline,
      final double currentValue) {

    final double deviation = Math.abs(currentValue - baseline.getMean());
    final double standardDeviations = baseline.getStandardDeviation() > 0 ?
        deviation / baseline.getStandardDeviation() : 0.0;

    DriftSeverity severity = DriftSeverity.NORMAL;
    String explanation = "Value within normal statistical control limits";
    final List<String> recommendations = new ArrayList<>();

    if (currentValue > baseline.getUpperControlLimit() || currentValue < baseline.getLowerControlLimit()) {
      if (standardDeviations > 4.0) {
        severity = DriftSeverity.CRITICAL;
        explanation = String.format("Value %.2f is %.1f standard deviations from baseline mean %.2f",
            currentValue, standardDeviations, baseline.getMean());
        recommendations.add("Immediate investigation required - critical performance deviation");
        recommendations.add("Check for system anomalies or configuration changes");
      } else if (standardDeviations > 3.0) {
        severity = DriftSeverity.SIGNIFICANT;
        explanation = String.format("Value %.2f exceeds control limits (%.1f σ from mean)",
            currentValue, standardDeviations);
        recommendations.add("Investigate potential performance regression");
        recommendations.add("Review recent system changes");
      } else {
        severity = DriftSeverity.MODERATE;
        explanation = String.format("Value %.2f approaches control limits (%.1f σ from mean)",
            currentValue, standardDeviations);
        recommendations.add("Monitor closely for continued drift");
      }
    }

    final Map<String, Object> diagnostics = Map.of(
        "baseline_mean", baseline.getMean(),
        "baseline_stddev", baseline.getStandardDeviation(),
        "upper_control_limit", baseline.getUpperControlLimit(),
        "lower_control_limit", baseline.getLowerControlLimit(),
        "standard_deviations", standardDeviations);

    return new DriftDetectionResult(
        config.getMetricName(),
        DriftDetectionAlgorithm.STATISTICAL_CONTROL,
        severity,
        currentValue,
        baseline.getMean(),
        deviation,
        standardDeviations > 0 ? Math.min(1.0, standardDeviations / 5.0) : 0.0,
        explanation,
        diagnostics,
        recommendations);
  }

  /** CUSUM drift detection. */
  private DriftDetectionResult detectCusumDrift(
      final BaselineConfig config,
      final PerformanceBaseline baseline,
      final double currentValue) {

    final double threshold = (Double) config.getAlgorithmParameters().getOrDefault("cusum_threshold", 2.0);
    final double target = baseline.getMean();

    // Simple CUSUM implementation - in production would maintain running state
    final double deviation = currentValue - target;
    final double normalizedDeviation = baseline.getStandardDeviation() > 0 ?
        deviation / baseline.getStandardDeviation() : 0.0;

    DriftSeverity severity = DriftSeverity.NORMAL;
    String explanation = "No significant cumulative deviation detected";
    final List<String> recommendations = new ArrayList<>();

    if (Math.abs(normalizedDeviation) > threshold) {
      if (Math.abs(normalizedDeviation) > threshold * 2) {
        severity = DriftSeverity.CRITICAL;
        explanation = String.format("Critical CUSUM drift: %.2f (threshold: %.2f)",
            normalizedDeviation, threshold);
        recommendations.add("Immediate action required - sustained performance shift");
      } else {
        severity = DriftSeverity.SIGNIFICANT;
        explanation = String.format("Significant CUSUM drift: %.2f (threshold: %.2f)",
            normalizedDeviation, threshold);
        recommendations.add("Investigate sustained performance change");
      }
    }

    final Map<String, Object> diagnostics = Map.of(
        "cusum_value", normalizedDeviation,
        "threshold", threshold,
        "target", target);

    return new DriftDetectionResult(
        config.getMetricName(),
        DriftDetectionAlgorithm.CUSUM,
        severity,
        currentValue,
        target,
        Math.abs(deviation),
        Math.min(1.0, Math.abs(normalizedDeviation) / (threshold * 2)),
        explanation,
        diagnostics,
        recommendations);
  }

  /** Exponentially weighted moving average drift detection. */
  private DriftDetectionResult detectExponentialWeightedDrift(
      final BaselineConfig config,
      final PerformanceBaseline baseline,
      final double currentValue) {

    final double alpha = (Double) config.getAlgorithmParameters().getOrDefault("ewma_alpha", 0.2);

    // Simple EWMA implementation - in production would maintain running state
    final double ewmaValue = alpha * currentValue + (1 - alpha) * baseline.getMean();
    final double deviation = Math.abs(ewmaValue - baseline.getMean());
    final double normalizedDeviation = baseline.getStandardDeviation() > 0 ?
        deviation / baseline.getStandardDeviation() : 0.0;

    DriftSeverity severity = DriftSeverity.NORMAL;
    String explanation = "EWMA within acceptable range";
    final List<String> recommendations = new ArrayList<>();

    if (normalizedDeviation > 2.0) {
      severity = DriftSeverity.SIGNIFICANT;
      explanation = String.format("EWMA drift detected: %.2f standard deviations",
          normalizedDeviation);
      recommendations.add("Monitor for continued drift trend");
    } else if (normalizedDeviation > 1.5) {
      severity = DriftSeverity.MODERATE;
      explanation = String.format("Moderate EWMA drift: %.2f standard deviations",
          normalizedDeviation);
      recommendations.add("Continue monitoring performance trend");
    }

    final Map<String, Object> diagnostics = Map.of(
        "ewma_value", ewmaValue,
        "alpha", alpha,
        "normalized_deviation", normalizedDeviation);

    return new DriftDetectionResult(
        config.getMetricName(),
        DriftDetectionAlgorithm.EXPONENTIAL_WEIGHTED,
        severity,
        currentValue,
        baseline.getMean(),
        deviation,
        Math.min(1.0, normalizedDeviation / 3.0),
        explanation,
        diagnostics,
        recommendations);
  }

  /** Trend analysis drift detection. */
  private DriftDetectionResult detectTrendDrift(
      final BaselineConfig config,
      final PerformanceBaseline baseline,
      final double currentValue) {

    final double trendSlope = baseline.getTrendSlope();
    final double slopeThreshold = baseline.getStandardDeviation() * 0.1; // 10% of stddev per time unit

    DriftSeverity severity = DriftSeverity.NORMAL;
    String explanation = "No significant trend drift detected";
    final List<String> recommendations = new ArrayList<>();

    if (Math.abs(trendSlope) > slopeThreshold) {
      if (Math.abs(trendSlope) > slopeThreshold * 5) {
        severity = DriftSeverity.CRITICAL;
        explanation = String.format("Critical trend drift: slope %.4f (threshold: %.4f)",
            trendSlope, slopeThreshold);
        recommendations.add("Urgent action required - steep performance trend change");
        recommendations.add("Investigate root cause of performance trend");
      } else if (Math.abs(trendSlope) > slopeThreshold * 2) {
        severity = DriftSeverity.SIGNIFICANT;
        explanation = String.format("Significant trend drift: slope %.4f",
            trendSlope);
        recommendations.add("Address performance trend before it becomes critical");
      } else {
        severity = DriftSeverity.MODERATE;
        explanation = String.format("Moderate trend drift detected: slope %.4f",
            trendSlope);
        recommendations.add("Monitor performance trend closely");
      }
    }

    final Map<String, Object> diagnostics = Map.of(
        "trend_slope", trendSlope,
        "slope_threshold", slopeThreshold,
        "trend_direction", trendSlope > 0 ? "increasing" : "decreasing");

    return new DriftDetectionResult(
        config.getMetricName(),
        DriftDetectionAlgorithm.TREND_ANALYSIS,
        severity,
        currentValue,
        baseline.getMean(),
        Math.abs(trendSlope),
        Math.min(1.0, Math.abs(trendSlope) / (slopeThreshold * 5)),
        explanation,
        diagnostics,
        recommendations);
  }

  /** Seasonal decomposition drift detection. */
  private DriftDetectionResult detectSeasonalDrift(
      final BaselineConfig config,
      final PerformanceBaseline baseline,
      final double currentValue) {

    final double seasonalVariation = baseline.getSeasonalVariation();
    final double expectedRange = baseline.getMean() + seasonalVariation;

    DriftSeverity severity = DriftSeverity.NORMAL;
    String explanation = "Value within expected seasonal range";
    final List<String> recommendations = new ArrayList<>();

    final double deviation = Math.abs(currentValue - baseline.getMean());
    if (deviation > expectedRange) {
      severity = DriftSeverity.MODERATE;
      explanation = String.format("Value %.2f outside seasonal range (expected: %.2f ± %.2f)",
          currentValue, baseline.getMean(), seasonalVariation);
      recommendations.add("Check if deviation is seasonal or anomalous");
    }

    final Map<String, Object> diagnostics = Map.of(
        "seasonal_variation", seasonalVariation,
        "expected_range", expectedRange,
        "deviation", deviation);

    return new DriftDetectionResult(
        config.getMetricName(),
        DriftDetectionAlgorithm.SEASONAL_DECOMPOSITION,
        severity,
        currentValue,
        baseline.getMean(),
        deviation,
        deviation > 0 ? Math.min(1.0, deviation / expectedRange) : 0.0,
        explanation,
        diagnostics,
        recommendations);
  }

  /** Performs forecasting and capacity planning analysis. */
  private void performForecastingAndCapacityPlanning() {
    try {
      for (final BaselineConfig config : baselineConfigs.values()) {
        final PerformanceBaseline baseline = baselines.get(config.getMetricName());
        if (baseline != null && baseline.isStable()) {
          generateForecast(config, baseline);
          generateCapacityRecommendation(config, baseline);
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error during forecasting and capacity planning: " + e.getMessage());
    }
  }

  /** Generates performance forecast for a metric. */
  private void generateForecast(final BaselineConfig config, final PerformanceBaseline baseline) {
    final Duration forecastHorizon = Duration.ofHours(24); // 24-hour forecast

    // Simple linear trend extrapolation - in production would use more sophisticated models
    final double forecastValue = baseline.getMean() +
        (baseline.getTrendSlope() * forecastHorizon.toHours());

    final double confidenceInterval = baseline.getStandardDeviation() * 1.96; // 95% CI
    final double upperBound = forecastValue + confidenceInterval;
    final double lowerBound = forecastValue - confidenceInterval;

    final PerformanceForecast forecast = new PerformanceForecast(
        config.getMetricName(),
        forecastHorizon,
        forecastValue,
        confidenceInterval,
        upperBound,
        lowerBound,
        "Linear Trend Extrapolation",
        Map.of(
            "trend_slope", baseline.getTrendSlope(),
            "baseline_mean", baseline.getMean(),
            "baseline_stddev", baseline.getStandardDeviation()));

    forecasts.put(config.getMetricName(), forecast);
    totalForecastsGenerated.incrementAndGet();
  }

  /** Generates capacity recommendation for a metric. */
  private void generateCapacityRecommendation(final BaselineConfig config, final PerformanceBaseline baseline) {
    // Simple capacity analysis - in production would be more sophisticated
    final double currentUtilization = baseline.getMean();
    final double trendSlope = baseline.getTrendSlope();

    if (Math.abs(trendSlope) < 0.001) {
      return; // No significant trend
    }

    final double capacityLimit = baseline.getPercentile95() * 1.2; // Assume 20% buffer above P95
    final double timeToLimit = trendSlope > 0 ?
        (capacityLimit - currentUtilization) / trendSlope : Double.POSITIVE_INFINITY;

    String recommendationType = "MAINTAIN";
    String description = "Current capacity appears adequate";
    final List<String> actionItems = new ArrayList<>();

    if (timeToLimit < 24) { // Less than 24 hours
      recommendationType = "URGENT_SCALE";
      description = "Capacity limit will be reached within 24 hours";
      actionItems.add("Immediately increase system capacity");
      actionItems.add("Implement load balancing or scaling measures");
    } else if (timeToLimit < 168) { // Less than 1 week
      recommendationType = "PLAN_SCALE";
      description = "Capacity limit will be reached within a week";
      actionItems.add("Plan capacity increase for next few days");
      actionItems.add("Monitor resource utilization closely");
    } else if (timeToLimit < 720) { // Less than 30 days
      recommendationType = "MONITOR";
      description = "Capacity adequate for now, but trend indicates future needs";
      actionItems.add("Continue monitoring capacity trends");
      actionItems.add("Prepare scaling procedures");
    }

    final CapacityRecommendation recommendation = new CapacityRecommendation(
        config.getMetricName(),
        recommendationType,
        description,
        currentUtilization,
        capacityLimit * 0.8, // Recommend staying at 80% of limit
        Duration.ofHours((long) Math.max(0, timeToLimit)),
        0.8, // 80% confidence
        actionItems,
        Map.of(
            "current_utilization", currentUtilization,
            "capacity_limit", capacityLimit,
            "trend_slope", trendSlope,
            "time_to_limit_hours", timeToLimit));

    capacityRecommendations.put(config.getMetricName(), recommendation);
  }

  /** Notifies drift detection listeners. */
  private void notifyDriftDetectionListeners(final DriftDetectionResult result) {
    for (final DriftDetectionListener listener : driftListeners) {
      try {
        listener.onDriftDetected(result);
      } catch (final Exception e) {
        LOGGER.warning("Drift detection listener error: " + e.getMessage());
      }
    }
  }

  /**
   * Adds a drift detection listener.
   *
   * @param listener the drift detection listener
   */
  public void addDriftDetectionListener(final DriftDetectionListener listener) {
    if (listener != null) {
      driftListeners.add(listener);
    }
  }

  /**
   * Gets baseline for a metric.
   *
   * @param metricName the metric name
   * @return performance baseline or null if not found
   */
  public PerformanceBaseline getBaseline(final String metricName) {
    return baselines.get(metricName);
  }

  /**
   * Gets drift detection history for a metric.
   *
   * @param metricName the metric name
   * @param limit maximum number of results to return
   * @return list of drift detection results
   */
  public List<DriftDetectionResult> getDriftHistory(final String metricName, final int limit) {
    final List<DriftDetectionResult> history = driftHistory.get(metricName);
    if (history == null) {
      return List.of();
    }

    return history.stream()
        .sorted(Comparator.comparing(DriftDetectionResult::getDetectedAt, Comparator.reverseOrder()))
        .limit(limit)
        .collect(Collectors.toList());
  }

  /**
   * Gets performance forecast for a metric.
   *
   * @param metricName the metric name
   * @return performance forecast or null if not available
   */
  public PerformanceForecast getForecast(final String metricName) {
    return forecasts.get(metricName);
  }

  /**
   * Gets capacity recommendation for a metric.
   *
   * @param metricName the metric name
   * @return capacity recommendation or null if not available
   */
  public CapacityRecommendation getCapacityRecommendation(final String metricName) {
    return capacityRecommendations.get(metricName);
  }

  /**
   * Gets all baselines.
   *
   * @return map of metric names to baselines
   */
  public Map<String, PerformanceBaseline> getAllBaselines() {
    return Map.copyOf(baselines);
  }

  /**
   * Gets all capacity recommendations.
   *
   * @return list of capacity recommendations
   */
  public List<CapacityRecommendation> getAllCapacityRecommendations() {
    return List.copyOf(capacityRecommendations.values());
  }

  /**
   * Gets performance baseline tracker statistics.
   *
   * @return formatted statistics
   */
  public String getBaselineTrackerStatistics() {
    final long stableBaselines = baselines.values().stream()
        .mapToLong(baseline -> baseline.isStable() ? 1 : 0)
        .sum();

    return String.format(
        "Performance Baseline Tracker Statistics: configured_metrics=%d, stable_baselines=%d, " +
        "total_drift_detections=%d, total_forecasts=%d, capacity_recommendations=%d, last_analysis=%s",
        baselineConfigs.size(),
        stableBaselines,
        totalDriftDetections.get(),
        totalForecastsGenerated.get(),
        capacityRecommendations.size(),
        lastAnalysisRun.get().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME));
  }

  /**
   * Sets drift detection enabled state.
   *
   * @param enabled true to enable drift detection
   */
  public void setDriftDetectionEnabled(final boolean enabled) {
    this.driftDetectionEnabled = enabled;
    LOGGER.info("Drift detection " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the performance baseline tracker. */
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
    LOGGER.info("Performance baseline tracker shutdown");
  }
}