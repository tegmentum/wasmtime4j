package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Automated regression detection system that identifies behavioral changes and performance
 * degradations across test executions. Uses historical data analysis and statistical methods
 * to detect significant deviations from established baselines.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Historical baseline tracking and drift detection
 *   <li>Statistical significance testing for behavioral changes
 *   <li>Performance regression identification
 *   <li>Automated alerts for critical regressions
 *   <li>Trend analysis for early regression detection
 * </ul>
 *
 * @since 1.0.0
 */
public final class RegressionDetector {
  private static final Logger LOGGER = Logger.getLogger(RegressionDetector.class.getName());

  // Regression detection thresholds
  private static final double PERFORMANCE_REGRESSION_THRESHOLD = 0.20; // 20% performance degradation
  private static final double BEHAVIORAL_CHANGE_THRESHOLD = 0.05; // 5% behavioral change
  private static final int MIN_HISTORICAL_SAMPLES = 5; // Minimum samples for regression analysis
  private static final int REGRESSION_DETECTION_WINDOW = 10; // Number of recent samples to analyze

  private final Map<String, List<RegressionDataPoint>> historicalData;
  private final Map<String, RegressionBaseline> baselines;

  /** Creates a new RegressionDetector. */
  public RegressionDetector() {
    this.historicalData = new ConcurrentHashMap<>();
    this.baselines = new ConcurrentHashMap<>();
  }

  /**
   * Detects regression patterns in execution results.
   *
   * @param executionResults map of runtime execution results
   * @return list of detected regressions
   */
  public List<BehavioralDiscrepancy> detectRegressions(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    Objects.requireNonNull(executionResults, "executionResults cannot be null");

    final List<BehavioralDiscrepancy> regressions = new ArrayList<>();

    LOGGER.fine("Detecting regressions for " + executionResults.size() + " runtime results");

    // Record current execution data for future regression analysis
    recordExecutionData(executionResults);

    // Detect performance regressions
    regressions.addAll(detectPerformanceRegressions(executionResults));

    // Detect behavioral regressions
    regressions.addAll(detectBehavioralRegressions(executionResults));

    // Detect systematic failure patterns
    regressions.addAll(detectSystematicRegressions(executionResults));

    LOGGER.fine("Regression detection completed: " + regressions.size() + " regressions found");
    return regressions;
  }

  /**
   * Establishes a baseline for regression detection.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @param metric the metric name
   * @param value the baseline value
   * @param tolerance the acceptable tolerance for regression
   */
  public void establishBaseline(
      final String testName,
      final RuntimeType runtimeType,
      final String metric,
      final double value,
      final double tolerance) {
    final String key = createKey(testName, runtimeType, metric);
    final RegressionBaseline baseline = new RegressionBaseline(key, value, tolerance, Instant.now());
    baselines.put(key, baseline);

    LOGGER.fine(String.format("Established regression baseline for %s: %.2f (+/- %.1f%%)",
        key, value, tolerance * 100));
  }

  /**
   * Clears all historical data and baselines.
   */
  public void clearHistory() {
    historicalData.clear();
    baselines.clear();
    LOGGER.info("Cleared all regression detection history");
  }

  /**
   * Gets the current baselines.
   *
   * @return map of baselines by key
   */
  public Map<String, RegressionBaseline> getBaselines() {
    return new HashMap<>(baselines);
  }

  /** Records execution data for future regression analysis. */
  private void recordExecutionData(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final Instant timestamp = Instant.now();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry : executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      // Record performance metrics
      final String performanceKey = createKey("performance", runtime, "execution_time");
      final double executionTimeMs = result.getExecutionTime().toNanos() / 1_000_000.0;
      recordDataPoint(performanceKey, executionTimeMs, timestamp, result.isSuccessful());

      // Record memory usage if available
      if (result.getMemoryUsage().isPresent()) {
        final String memoryKey = createKey("memory", runtime, "heap_used");
        final double heapUsedMB = result.getMemoryUsage().get().getHeapUsed() / (1024.0 * 1024.0);
        recordDataPoint(memoryKey, heapUsedMB, timestamp, result.isSuccessful());
      }

      // Record success rate
      final String successKey = createKey("behavior", runtime, "success_rate");
      final double successValue = result.isSuccessful() ? 1.0 : 0.0;
      recordDataPoint(successKey, successValue, timestamp, result.isSuccessful());
    }
  }

  /** Records a single data point for regression tracking. */
  private void recordDataPoint(
      final String key, final double value, final Instant timestamp, final boolean successful) {
    final RegressionDataPoint dataPoint = new RegressionDataPoint(value, timestamp, successful);
    historicalData.computeIfAbsent(key, k -> new ArrayList<>()).add(dataPoint);

    // Maintain reasonable history size
    final List<RegressionDataPoint> history = historicalData.get(key);
    if (history.size() > 1000) {
      history.subList(0, history.size() - 1000).clear();
    }
  }

  /** Detects performance regressions. */
  private List<BehavioralDiscrepancy> detectPerformanceRegressions(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> regressions = new ArrayList<>();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry : executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      final String performanceKey = createKey("performance", runtime, "execution_time");
      final List<RegressionDataPoint> history = historicalData.get(performanceKey);

      if (history != null && history.size() >= MIN_HISTORICAL_SAMPLES) {
        final double currentValue = result.getExecutionTime().toNanos() / 1_000_000.0;
        final double historicalAverage = calculateHistoricalAverage(history);

        if (historicalAverage > 0) {
          final double regressionRatio = (currentValue - historicalAverage) / historicalAverage;

          if (regressionRatio > PERFORMANCE_REGRESSION_THRESHOLD) {
            regressions.add(
                new BehavioralDiscrepancy(
                    DiscrepancyType.PERFORMANCE_DEVIATION,
                    DiscrepancySeverity.MAJOR,
                    "Performance regression detected",
                    String.format("Execution time increased by %.1f%% for runtime %s",
                        regressionRatio * 100, runtime),
                    "Investigate performance degradation and optimize implementation",
                    "performance-regression",
                    java.util.Set.of(runtime)));
          }
        }
      }
    }

    return regressions;
  }

  /** Detects behavioral regressions. */
  private List<BehavioralDiscrepancy> detectBehavioralRegressions(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> regressions = new ArrayList<>();

    for (final Map.Entry<RuntimeType, BehavioralAnalyzer.TestExecutionResult> entry : executionResults.entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final BehavioralAnalyzer.TestExecutionResult result = entry.getValue();

      final String behaviorKey = createKey("behavior", runtime, "success_rate");
      final List<RegressionDataPoint> history = historicalData.get(behaviorKey);

      if (history != null && history.size() >= MIN_HISTORICAL_SAMPLES) {
        final double recentSuccessRate = calculateRecentSuccessRate(history);
        final double historicalSuccessRate = calculateHistoricalAverage(history);

        final double behavioralChange = Math.abs(recentSuccessRate - historicalSuccessRate);

        if (behavioralChange > BEHAVIORAL_CHANGE_THRESHOLD) {
          final DiscrepancySeverity severity = recentSuccessRate < historicalSuccessRate
              ? DiscrepancySeverity.CRITICAL
              : DiscrepancySeverity.MODERATE;

          regressions.add(
              new BehavioralDiscrepancy(
                  DiscrepancyType.SYSTEMATIC_PATTERN,
                  severity,
                  "Behavioral regression detected",
                  String.format("Success rate changed from %.1f%% to %.1f%% for runtime %s",
                      historicalSuccessRate * 100, recentSuccessRate * 100, runtime),
                  "Investigate behavioral changes and verify implementation correctness",
                  "behavioral-regression",
                  java.util.Set.of(runtime)));
        }
      }
    }

    return regressions;
  }

  /** Detects systematic regression patterns. */
  private List<BehavioralDiscrepancy> detectSystematicRegressions(
      final Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult> executionResults) {
    final List<BehavioralDiscrepancy> regressions = new ArrayList<>();

    // Check if multiple runtimes are experiencing regressions simultaneously
    int runtimesWithPerformanceIssues = 0;
    int runtimesWithBehavioralIssues = 0;

    for (final RuntimeType runtime : executionResults.keySet()) {
      if (hasRecentPerformanceRegression(runtime)) {
        runtimesWithPerformanceIssues++;
      }
      if (hasRecentBehavioralRegression(runtime)) {
        runtimesWithBehavioralIssues++;
      }
    }

    // Flag systematic issues affecting multiple runtimes
    if (runtimesWithPerformanceIssues >= 2) {
      regressions.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.SYSTEMATIC_PATTERN,
              DiscrepancySeverity.CRITICAL,
              "Systematic performance regression detected",
              String.format("Performance issues detected across %d runtimes", runtimesWithPerformanceIssues),
              "Investigate system-wide performance degradation and environmental factors",
              "systematic-performance-regression",
              executionResults.keySet()));
    }

    if (runtimesWithBehavioralIssues >= 2) {
      regressions.add(
          new BehavioralDiscrepancy(
              DiscrepancyType.SYSTEMATIC_PATTERN,
              DiscrepancySeverity.CRITICAL,
              "Systematic behavioral regression detected",
              String.format("Behavioral issues detected across %d runtimes", runtimesWithBehavioralIssues),
              "Investigate system-wide behavioral changes and test environment",
              "systematic-behavioral-regression",
              executionResults.keySet()));
    }

    return regressions;
  }

  /** Calculates historical average for a metric. */
  private double calculateHistoricalAverage(final List<RegressionDataPoint> history) {
    return history.stream()
        .mapToDouble(RegressionDataPoint::getValue)
        .average()
        .orElse(0.0);
  }

  /** Calculates recent success rate from regression data points. */
  private double calculateRecentSuccessRate(final List<RegressionDataPoint> history) {
    final int recentWindow = Math.min(REGRESSION_DETECTION_WINDOW, history.size());
    final List<RegressionDataPoint> recentData = history.subList(history.size() - recentWindow, history.size());

    return recentData.stream()
        .mapToDouble(RegressionDataPoint::getValue)
        .average()
        .orElse(0.0);
  }

  /** Checks if a runtime has recent performance regression. */
  private boolean hasRecentPerformanceRegression(final RuntimeType runtime) {
    final String performanceKey = createKey("performance", runtime, "execution_time");
    final List<RegressionDataPoint> history = historicalData.get(performanceKey);

    if (history == null || history.size() < MIN_HISTORICAL_SAMPLES) {
      return false;
    }

    final double recentAverage = calculateRecentAverage(history);
    final double historicalAverage = calculateHistoricalAverage(history);

    return historicalAverage > 0 && (recentAverage - historicalAverage) / historicalAverage > PERFORMANCE_REGRESSION_THRESHOLD;
  }

  /** Checks if a runtime has recent behavioral regression. */
  private boolean hasRecentBehavioralRegression(final RuntimeType runtime) {
    final String behaviorKey = createKey("behavior", runtime, "success_rate");
    final List<RegressionDataPoint> history = historicalData.get(behaviorKey);

    if (history == null || history.size() < MIN_HISTORICAL_SAMPLES) {
      return false;
    }

    final double recentSuccessRate = calculateRecentSuccessRate(history);
    final double historicalSuccessRate = calculateHistoricalAverage(history);

    return Math.abs(recentSuccessRate - historicalSuccessRate) > BEHAVIORAL_CHANGE_THRESHOLD;
  }

  /** Calculates recent average for performance metrics. */
  private double calculateRecentAverage(final List<RegressionDataPoint> history) {
    final int recentWindow = Math.min(REGRESSION_DETECTION_WINDOW, history.size());
    final List<RegressionDataPoint> recentData = history.subList(history.size() - recentWindow, history.size());

    return recentData.stream()
        .mapToDouble(RegressionDataPoint::getValue)
        .average()
        .orElse(0.0);
  }

  /** Creates a unique key for data tracking. */
  private String createKey(final String category, final RuntimeType runtime, final String metric) {
    return String.format("%s_%s_%s", category, runtime.name(), metric);
  }

  /** Represents a single data point for regression analysis. */
  public static final class RegressionDataPoint {
    private final double value;
    private final Instant timestamp;
    private final boolean successful;

    /**
     * Creates a new regression data point.
     *
     * @param value the metric value
     * @param timestamp the timestamp when the data was recorded
     * @param successful whether the execution was successful
     */
    public RegressionDataPoint(final double value, final Instant timestamp, final boolean successful) {
      this.value = value;
      this.timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
      this.successful = successful;
    }

    public double getValue() {
      return value;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public boolean isSuccessful() {
      return successful;
    }
  }

  /** Represents a baseline for regression detection. */
  public static final class RegressionBaseline {
    private final String key;
    private final double baselineValue;
    private final double tolerance;
    private final Instant establishedAt;

    /**
     * Creates a new regression baseline.
     *
     * @param key the baseline identifier
     * @param baselineValue the baseline metric value
     * @param tolerance the acceptable tolerance for regression
     * @param establishedAt when the baseline was established
     */
    public RegressionBaseline(
        final String key, final double baselineValue, final double tolerance, final Instant establishedAt) {
      this.key = Objects.requireNonNull(key, "key cannot be null");
      this.baselineValue = baselineValue;
      this.tolerance = tolerance;
      this.establishedAt = Objects.requireNonNull(establishedAt, "establishedAt cannot be null");
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

    /** Checks if a value represents a regression from the baseline. */
    public boolean isRegression(final double value) {
      return Math.abs(value - baselineValue) / baselineValue > tolerance;
    }

    /** Gets the regression percentage for a value. */
    public double getRegressionPercentage(final double value) {
      return (value - baselineValue) / baselineValue;
    }
  }
}