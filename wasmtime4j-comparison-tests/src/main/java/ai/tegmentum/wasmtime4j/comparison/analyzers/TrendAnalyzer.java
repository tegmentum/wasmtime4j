package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Advanced trend analyzer for detecting performance patterns, regressions, and improvements over
 * time. Uses statistical methods to identify significant changes in performance metrics and
 * provides predictive analysis for future performance trends.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Time-series analysis of performance metrics
 *   <li>Statistical significance testing for trend detection
 *   <li>Regression and improvement identification
 *   <li>Baseline drift detection and alerting
 *   <li>Predictive performance modeling
 * </ul>
 */
public final class TrendAnalyzer {
  private static final Logger LOGGER = Logger.getLogger(TrendAnalyzer.class.getName());

  // Trend analysis thresholds
  private static final double SIGNIFICANT_TREND_THRESHOLD = 0.05; // 5% change
  private static final double REGRESSION_THRESHOLD = 0.10; // 10% degradation
  private static final double IMPROVEMENT_THRESHOLD = 0.05; // 5% improvement
  private static final int MIN_DATA_POINTS_FOR_TREND = 5;
  private static final double CONFIDENCE_LEVEL = 0.95; // 95% confidence

  private final Map<String, List<TrendDataPoint>> trendHistory;
  private final Map<String, TrendBaseline> baselines;

  /** Creates a new TrendAnalyzer. */
  public TrendAnalyzer() {
    this.trendHistory = new ConcurrentHashMap<>();
    this.baselines = new ConcurrentHashMap<>();
  }

  /** Represents a single data point in a performance trend. */
  public static final class TrendDataPoint {
    private final Instant timestamp;
    private final double value;
    private final String metric;
    private final String context;
    private final Map<String, Object> metadata;

    private TrendDataPoint(
        final Instant timestamp,
        final double value,
        final String metric,
        final String context,
        final Map<String, Object> metadata) {
      this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
      this.value = value;
      this.metric = Objects.requireNonNull(metric, "metric cannot be null");
      this.context = Objects.requireNonNull(context, "context cannot be null");
      this.metadata = Map.copyOf(metadata);
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public double getValue() {
      return value;
    }

    public String getMetric() {
      return metric;
    }

    public String getContext() {
      return context;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    /** Creates a new trend data point. */
    public static TrendDataPoint create(
        final Instant timestamp, final double value, final String metric, final String context) {
      return new TrendDataPoint(timestamp, value, metric, context, Collections.emptyMap());
    }

    /** Creates a new trend data point with metadata. */
    public static TrendDataPoint create(
        final Instant timestamp,
        final double value,
        final String metric,
        final String context,
        final Map<String, Object> metadata) {
      return new TrendDataPoint(timestamp, value, metric, context, metadata);
    }
  }

  /** Comprehensive trend analysis result. */
  public static final class TrendAnalysisResult {
    private final String trendKey;
    private final List<TrendDataPoint> dataPoints;
    private final TrendDirection direction;
    private final double trendSlope;
    private final double correlationCoefficient;
    private final double pvalue;
    private final boolean isStatisticallySignificant;
    private final TrendType trendType;
    private final double projectedChange;
    private final List<String> anomalies;
    private final Instant analysisTime;

    private TrendAnalysisResult(
        final String trendKey,
        final List<TrendDataPoint> dataPoints,
        final TrendDirection direction,
        final double trendSlope,
        final double correlationCoefficient,
        final double pvalue,
        final boolean isStatisticallySignificant,
        final TrendType trendType,
        final double projectedChange,
        final List<String> anomalies) {
      this.trendKey = trendKey;
      this.dataPoints = new ArrayList<>(dataPoints);
      this.direction = direction;
      this.trendSlope = trendSlope;
      this.correlationCoefficient = correlationCoefficient;
      this.pvalue = pvalue;
      this.isStatisticallySignificant = isStatisticallySignificant;
      this.trendType = trendType;
      this.projectedChange = projectedChange;
      this.anomalies = new ArrayList<>(anomalies);
      this.analysisTime = Instant.now();
    }

    public String getTrendKey() {
      return trendKey;
    }

    public List<TrendDataPoint> getDataPoints() {
      return new ArrayList<>(dataPoints);
    }

    public TrendDirection getDirection() {
      return direction;
    }

    public double getTrendSlope() {
      return trendSlope;
    }

    public double getCorrelationCoefficient() {
      return correlationCoefficient;
    }

    public double getPvalue() {
      return pvalue;
    }

    public boolean isStatisticallySignificant() {
      return isStatisticallySignificant;
    }

    public TrendType getTrendType() {
      return trendType;
    }

    public double getProjectedChange() {
      return projectedChange;
    }

    public List<String> getAnomalies() {
      return new ArrayList<>(anomalies);
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }

    /** Checks if the trend indicates a performance regression. */
    public boolean isRegression() {
      return trendType == TrendType.REGRESSION && isStatisticallySignificant;
    }

    /** Checks if the trend indicates a performance improvement. */
    public boolean isImprovement() {
      return trendType == TrendType.IMPROVEMENT && isStatisticallySignificant;
    }

    /** Gets a human-readable trend description. */
    public String getTrendDescription() {
      if (!isStatisticallySignificant) {
        return "No significant trend detected";
      }

      final String directionStr =
          direction == TrendDirection.INCREASING ? "increasing" : "decreasing";
      final String changePercent = String.format("%.1f%%", Math.abs(projectedChange) * 100);

      switch (trendType) {
        case REGRESSION:
          return String.format(
              "Performance regression detected: %s trend with %s degradation",
              directionStr, changePercent);
        case IMPROVEMENT:
          return String.format(
              "Performance improvement detected: %s trend with %s improvement",
              directionStr, changePercent);
        case STABLE:
          return "Performance is stable with minimal variation";
        default:
          return String.format("Performance trend: %s by %s", directionStr, changePercent);
      }
    }
  }

  /** Trend direction enumeration. */
  public enum TrendDirection {
    INCREASING,
    DECREASING,
    STABLE
  }

  /** Trend type classification. */
  public enum TrendType {
    IMPROVEMENT,
    REGRESSION,
    STABLE,
    NEUTRAL
  }

  /** Performance baseline for trend comparison. */
  public static final class TrendBaseline {
    private final String key;
    private final double baselineValue;
    private final double tolerance;
    private final Instant establishedAt;
    private final int sampleSize;

    /**
     * Creates a new trend baseline.
     *
     * @param key the baseline identifier
     * @param baselineValue the baseline metric value
     * @param tolerance the acceptable drift tolerance
     * @param sampleSize the number of samples used to establish baseline
     */
    public TrendBaseline(
        final String key,
        final double baselineValue,
        final double tolerance,
        final int sampleSize) {
      this.key = key;
      this.baselineValue = baselineValue;
      this.tolerance = tolerance;
      this.establishedAt = Instant.now();
      this.sampleSize = sampleSize;
    }

    public String getKey() {
      return key;
    }

    public double getBaselineValue() {
      return baselineValue;
    }

    public double getTolerance() {
      return tolerance;
    }

    public Instant getEstablishedAt() {
      return establishedAt;
    }

    public int getSampleSize() {
      return sampleSize;
    }

    /** Checks if a value represents drift from the baseline. */
    public boolean isDrift(final double value) {
      final double change = Math.abs(value - baselineValue) / baselineValue;
      return change > tolerance;
    }

    /** Gets the drift percentage for a value. */
    public double getDriftPercentage(final double value) {
      return (value - baselineValue) / baselineValue;
    }
  }

  /** Linear regression result for trend analysis. */
  private static final class LinearRegressionResult {
    private final double slope;
    private final double intercept;
    private final double correlationCoefficient;
    private final double rsquared;
    private final double standardError;

    private LinearRegressionResult(
        final double slope,
        final double intercept,
        final double correlationCoefficient,
        final double rsquared,
        final double standardError) {
      this.slope = slope;
      this.intercept = intercept;
      this.correlationCoefficient = correlationCoefficient;
      this.rsquared = rsquared;
      this.standardError = standardError;
    }
  }

  /**
   * Records a performance metric for trend analysis.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @param metric the metric name
   * @param value the metric value
   */
  public void recordMetric(
      final String testName, final String runtimeType, final String metric, final double value) {
    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    Objects.requireNonNull(metric, "metric cannot be null");

    final String trendKey = createTrendKey(testName, runtimeType, metric);
    final TrendDataPoint dataPoint = TrendDataPoint.create(Instant.now(), value, metric, trendKey);

    trendHistory.computeIfAbsent(trendKey, k -> new ArrayList<>()).add(dataPoint);

    // Maintain reasonable history size (keep last 1000 points)
    final List<TrendDataPoint> history = trendHistory.get(trendKey);
    if (history.size() > 1000) {
      history.subList(0, history.size() - 1000).clear();
    }

    LOGGER.fine(String.format("Recorded trend metric: %s = %.2f", trendKey, value));
  }

  /**
   * Analyzes performance trends for all recorded metrics.
   *
   * @return map of trend analysis results by trend key
   */
  public Map<String, TrendAnalysisResult> analyzeTrends() {
    final Map<String, TrendAnalysisResult> results = new HashMap<>();

    for (final Map.Entry<String, List<TrendDataPoint>> entry : trendHistory.entrySet()) {
      final String trendKey = entry.getKey();
      final List<TrendDataPoint> dataPoints = entry.getValue();

      if (dataPoints.size() >= MIN_DATA_POINTS_FOR_TREND) {
        final TrendAnalysisResult result = analyzeTrend(trendKey, dataPoints);
        results.put(trendKey, result);
      }
    }

    return results;
  }

  /**
   * Analyzes trends for a specific test and runtime combination.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @return map of trend analysis results by metric
   */
  public Map<String, TrendAnalysisResult> analyzeTrends(
      final String testName, final String runtimeType) {
    final String keyPrefix = testName + "_" + runtimeType + "_";

    return trendHistory.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(keyPrefix))
        .filter(entry -> entry.getValue().size() >= MIN_DATA_POINTS_FOR_TREND)
        .collect(
            Collectors.toMap(
                Map.Entry::getKey, entry -> analyzeTrend(entry.getKey(), entry.getValue())));
  }

  /**
   * Establishes a performance baseline for future comparison.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @param metric the metric name
   * @param tolerance the acceptable drift percentage
   */
  public void establishBaseline(
      final String testName,
      final String runtimeType,
      final String metric,
      final double tolerance) {
    final String trendKey = createTrendKey(testName, runtimeType, metric);
    final List<TrendDataPoint> dataPoints = trendHistory.get(trendKey);

    if (dataPoints == null || dataPoints.size() < MIN_DATA_POINTS_FOR_TREND) {
      LOGGER.warning(String.format("Insufficient data to establish baseline for %s", trendKey));
      return;
    }

    // Use recent data points for baseline calculation
    final List<TrendDataPoint> recentPoints =
        dataPoints.subList(Math.max(0, dataPoints.size() - 20), dataPoints.size());

    final double baselineValue =
        recentPoints.stream().mapToDouble(TrendDataPoint::getValue).average().orElse(0.0);

    final TrendBaseline baseline =
        new TrendBaseline(trendKey, baselineValue, tolerance, recentPoints.size());
    baselines.put(trendKey, baseline);

    LOGGER.info(
        String.format(
            "Established baseline for %s: %.2f (±%.1f%%)",
            trendKey, baselineValue, tolerance * 100));
  }

  /**
   * Detects baseline drift for all established baselines.
   *
   * @return map of drift results by trend key
   */
  public Map<String, Double> detectBaselineDrift() {
    final Map<String, Double> driftResults = new HashMap<>();

    for (final Map.Entry<String, TrendBaseline> entry : baselines.entrySet()) {
      final String trendKey = entry.getKey();
      final TrendBaseline baseline = entry.getValue();
      final List<TrendDataPoint> dataPoints = trendHistory.get(trendKey);

      if (dataPoints != null && !dataPoints.isEmpty()) {
        final TrendDataPoint latestPoint = dataPoints.get(dataPoints.size() - 1);
        final double driftPercentage = baseline.getDriftPercentage(latestPoint.getValue());

        if (baseline.isDrift(latestPoint.getValue())) {
          driftResults.put(trendKey, driftPercentage);
          LOGGER.warning(
              String.format(
                  "Baseline drift detected for %s: %.1f%%", trendKey, driftPercentage * 100));
        }
      }
    }

    return driftResults;
  }

  /** Clears all trend history and baselines. */
  public void clearHistory() {
    trendHistory.clear();
    baselines.clear();
    LOGGER.info("Cleared all trend history and baselines");
  }

  /**
   * Gets the current trend history.
   *
   * @return map of trend data points by trend key
   */
  public Map<String, List<TrendDataPoint>> getTrendHistory() {
    final Map<String, List<TrendDataPoint>> copy = new HashMap<>();
    for (final Map.Entry<String, List<TrendDataPoint>> entry : trendHistory.entrySet()) {
      copy.put(entry.getKey(), new ArrayList<>(entry.getValue()));
    }
    return copy;
  }

  /**
   * Gets all established baselines.
   *
   * @return map of baselines by trend key
   */
  public Map<String, TrendBaseline> getBaselines() {
    return new HashMap<>(baselines);
  }

  private TrendAnalysisResult analyzeTrend(
      final String trendKey, final List<TrendDataPoint> dataPoints) {
    // Sort data points by timestamp
    final List<TrendDataPoint> sortedPoints =
        dataPoints.stream()
            .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
            .collect(Collectors.toList());

    // Prepare data for analysis
    final double[] values = sortedPoints.stream().mapToDouble(TrendDataPoint::getValue).toArray();
    final double[] timeIndices = IntStream.range(0, values.length).asDoubleStream().toArray();

    // Perform linear regression
    final LinearRegressionResult regression = performLinearRegression(timeIndices, values);

    // Determine trend direction
    final TrendDirection direction = determineTrendDirection(regression.slope);

    // Calculate statistical significance
    final double pvalue = calculatePValue(regression, values.length);
    final boolean isSignificant = pvalue < (1.0 - CONFIDENCE_LEVEL);

    // Classify trend type
    final TrendType trendType = classifyTrendType(regression.slope, isSignificant);

    // Calculate projected change
    final double projectedChange = calculateProjectedChange(regression.slope, values.length);

    // Detect anomalies
    final List<String> anomalies = detectAnomalies(sortedPoints, regression);

    return new TrendAnalysisResult(
        trendKey,
        sortedPoints,
        direction,
        regression.slope,
        regression.correlationCoefficient,
        pvalue,
        isSignificant,
        trendType,
        projectedChange,
        anomalies);
  }

  private LinearRegressionResult performLinearRegression(final double[] x, final double[] y) {
    final int n = x.length;

    if (n < 2) {
      return new LinearRegressionResult(0.0, 0.0, 0.0, 0.0, 0.0);
    }

    final double sumX = java.util.Arrays.stream(x).sum();
    final double sumY = java.util.Arrays.stream(y).sum();
    final double sumXY = IntStream.range(0, n).mapToDouble(i -> x[i] * y[i]).sum();
    final double sumXX = java.util.Arrays.stream(x).map(xi -> xi * xi).sum();
    final double sumYY = java.util.Arrays.stream(y).map(yi -> yi * yi).sum();

    final double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    final double intercept = (sumY - slope * sumX) / n;

    // Calculate correlation coefficient
    final double numerator = n * sumXY - sumX * sumY;
    final double denominatorX = Math.sqrt(n * sumXX - sumX * sumX);
    final double denominatorY = Math.sqrt(n * sumYY - sumY * sumY);
    final double correlation =
        denominatorX * denominatorY != 0 ? numerator / (denominatorX * denominatorY) : 0.0;

    final double rsquared = correlation * correlation;

    // Calculate standard error
    final double meanY = sumY / n;
    final double ssRes =
        IntStream.range(0, n)
            .mapToDouble(
                i -> {
                  final double predicted = slope * x[i] + intercept;
                  return Math.pow(y[i] - predicted, 2);
                })
            .sum();
    final double standardError = n > 2 ? Math.sqrt(ssRes / (n - 2)) : 0.0;

    return new LinearRegressionResult(slope, intercept, correlation, rsquared, standardError);
  }

  private TrendDirection determineTrendDirection(final double slope) {
    if (Math.abs(slope) < SIGNIFICANT_TREND_THRESHOLD) {
      return TrendDirection.STABLE;
    }
    return slope > 0 ? TrendDirection.INCREASING : TrendDirection.DECREASING;
  }

  private double calculatePValue(final LinearRegressionResult regression, final int sampleSize) {
    // Simplified p-value calculation based on correlation coefficient
    // In a full implementation, this would use t-distribution
    final double tStat =
        Math.abs(regression.correlationCoefficient)
            * Math.sqrt((sampleSize - 2) / (1 - regression.rsquared));

    // Approximate p-value for t-distribution (simplified)
    if (sampleSize < 5) {
      return 1.0; // Not enough data for significance
    }

    return Math.max(0.001, 1.0 - Math.tanh(tStat / 3.0)); // Simplified approximation
  }

  private TrendType classifyTrendType(final double slope, final boolean isSignificant) {
    if (!isSignificant) {
      return TrendType.STABLE;
    }

    final double absSlope = Math.abs(slope);

    if (absSlope >= REGRESSION_THRESHOLD) {
      return slope > 0 ? TrendType.REGRESSION : TrendType.IMPROVEMENT;
    } else if (absSlope >= IMPROVEMENT_THRESHOLD) {
      return slope > 0 ? TrendType.IMPROVEMENT : TrendType.REGRESSION;
    } else {
      return TrendType.NEUTRAL;
    }
  }

  private double calculateProjectedChange(final double slope, final int timeWindow) {
    // Project change over the next time window
    return slope * timeWindow;
  }

  private List<String> detectAnomalies(
      final List<TrendDataPoint> dataPoints, final LinearRegressionResult regression) {
    final List<String> anomalies = new ArrayList<>();

    if (dataPoints.size() < 5) {
      return anomalies; // Need more data for anomaly detection
    }

    final double[] values = dataPoints.stream().mapToDouble(TrendDataPoint::getValue).toArray();
    final double mean = java.util.Arrays.stream(values).average().orElse(0.0);
    final double stdDev =
        Math.sqrt(
            java.util.Arrays.stream(values).map(v -> Math.pow(v - mean, 2)).average().orElse(0.0));

    final double threshold = 2.0 * stdDev; // 2 standard deviations

    for (int i = 0; i < dataPoints.size(); i++) {
      final TrendDataPoint point = dataPoints.get(i);
      final double predicted = regression.slope * i + regression.intercept;
      final double residual = Math.abs(point.getValue() - predicted);

      if (residual > threshold) {
        anomalies.add(
            String.format(
                "Anomaly at %s: expected %.2f, actual %.2f",
                point.getTimestamp(), predicted, point.getValue()));
      }
    }

    return anomalies;
  }

  private String createTrendKey(
      final String testName, final String runtimeType, final String metric) {
    return testName + "_" + runtimeType + "_" + metric;
  }
}
