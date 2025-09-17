package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Core performance analyzer providing comprehensive statistical analysis and comparison of test
 * execution results across multiple runtime implementations.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Execution time comparison with statistical significance testing
 *   <li>Memory usage analysis and allocation pattern detection
 *   <li>Performance regression detection across test runs
 *   <li>Overhead analysis for JNI vs Panama vs native execution
 *   <li>Performance baseline establishment and drift detection
 * </ul>
 *
 * <p>Thread-safe and optimized for analyzing large result sets while maintaining memory efficiency
 * and fast execution times.
 */
public final class PerformanceAnalyzer {
  private static final Logger LOGGER = Logger.getLogger(PerformanceAnalyzer.class.getName());

  // Statistical significance thresholds
  private static final double DEFAULT_SIGNIFICANCE_LEVEL = 0.05; // 95% confidence
  private static final double REGRESSION_THRESHOLD = 0.05; // 5% performance degradation
  private static final double MIN_SAMPLE_SIZE = 3;

  private final MetricsCollector metricsCollector;
  private final TrendAnalyzer trendAnalyzer;
  private final Map<String, PerformanceBaseline> baselines;

  /** Creates a new PerformanceAnalyzer with default configuration. */
  public PerformanceAnalyzer() {
    this.metricsCollector = new MetricsCollector();
    this.trendAnalyzer = new TrendAnalyzer();
    this.baselines = new ConcurrentHashMap<>();
  }

  /** Represents a test execution result with performance metrics. */
  public static final class TestExecutionResult {
    private final String testName;
    private final String runtimeType;
    private final Instant executionTime;
    private final Duration executionDuration;
    private final long memoryUsed;
    private final long peakMemoryUsage;
    private final boolean successful;
    private final String errorMessage;
    private final Map<String, Object> additionalMetrics;

    private TestExecutionResult(final Builder builder) {
      this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
      this.runtimeType = Objects.requireNonNull(builder.runtimeType, "runtimeType cannot be null");
      this.executionTime =
          Objects.requireNonNull(builder.executionTime, "executionTime cannot be null");
      this.executionDuration =
          Objects.requireNonNull(builder.executionDuration, "executionDuration cannot be null");
      this.memoryUsed = builder.memoryUsed;
      this.peakMemoryUsage = builder.peakMemoryUsage;
      this.successful = builder.successful;
      this.errorMessage = builder.errorMessage;
      this.additionalMetrics = new HashMap<>(builder.additionalMetrics);
    }

    public String getTestName() {
      return testName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public Instant getExecutionTime() {
      return executionTime;
    }

    public Duration getExecutionDuration() {
      return executionDuration;
    }

    public long getMemoryUsed() {
      return memoryUsed;
    }

    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public Map<String, Object> getAdditionalMetrics() {
      return new HashMap<>(additionalMetrics);
    }

    public static Builder builder(final String testName, final String runtimeType) {
      return new Builder(testName, runtimeType);
    }

    /** Builder for TestExecutionResult. */
    public static final class Builder {
      private final String testName;
      private final String runtimeType;
      private Instant executionTime = Instant.now();
      private Duration executionDuration = Duration.ZERO;
      private long memoryUsed = 0;
      private long peakMemoryUsage = 0;
      private boolean successful = true;
      private String errorMessage = null;
      private final Map<String, Object> additionalMetrics = new HashMap<>();

      private Builder(final String testName, final String runtimeType) {
        this.testName = testName;
        this.runtimeType = runtimeType;
      }

      public Builder executionTime(final Instant executionTime) {
        this.executionTime = executionTime;
        return this;
      }

      public Builder executionDuration(final Duration executionDuration) {
        this.executionDuration = executionDuration;
        return this;
      }

      public Builder memoryUsed(final long memoryUsed) {
        this.memoryUsed = memoryUsed;
        return this;
      }

      public Builder peakMemoryUsage(final long peakMemoryUsage) {
        this.peakMemoryUsage = peakMemoryUsage;
        return this;
      }

      public Builder successful(final boolean successful) {
        this.successful = successful;
        return this;
      }

      public Builder errorMessage(final String errorMessage) {
        this.errorMessage = errorMessage;
        return this;
      }

      public Builder additionalMetric(final String key, final Object value) {
        this.additionalMetrics.put(key, value);
        return this;
      }

      public TestExecutionResult build() {
        return new TestExecutionResult(this);
      }
    }
  }

  /** Comprehensive performance comparison result. */
  public static final class PerformanceComparisonResult {
    private final String testName;
    private final List<TestExecutionResult> results;
    private final Map<String, PerformanceMetrics> metricsByRuntime;
    private final List<String> significantDifferences;
    private final List<String> regressionWarnings;
    private final OverheadAnalysis overheadAnalysis;
    private final Instant analysisTime;

    private PerformanceComparisonResult(
        final String testName,
        final List<TestExecutionResult> results,
        final Map<String, PerformanceMetrics> metricsByRuntime,
        final List<String> significantDifferences,
        final List<String> regressionWarnings,
        final OverheadAnalysis overheadAnalysis) {
      this.testName = testName;
      this.results = new ArrayList<>(results);
      this.metricsByRuntime = new HashMap<>(metricsByRuntime);
      this.significantDifferences = new ArrayList<>(significantDifferences);
      this.regressionWarnings = new ArrayList<>(regressionWarnings);
      this.overheadAnalysis = overheadAnalysis;
      this.analysisTime = Instant.now();
    }

    public String getTestName() {
      return testName;
    }

    public List<TestExecutionResult> getResults() {
      return new ArrayList<>(results);
    }

    public Map<String, PerformanceMetrics> getMetricsByRuntime() {
      return new HashMap<>(metricsByRuntime);
    }

    public List<String> getSignificantDifferences() {
      return new ArrayList<>(significantDifferences);
    }

    public List<String> getRegressionWarnings() {
      return new ArrayList<>(regressionWarnings);
    }

    public OverheadAnalysis getOverheadAnalysis() {
      return overheadAnalysis;
    }

    public Instant getAnalysisTime() {
      return analysisTime;
    }

    /** Checks if any significant performance differences were detected. */
    public boolean hasSignificantDifferences() {
      return !significantDifferences.isEmpty();
    }

    /** Checks if any performance regressions were detected. */
    public boolean hasRegressions() {
      return !regressionWarnings.isEmpty();
    }

    /** Gets the fastest runtime for this test. */
    public String getFastestRuntime() {
      return metricsByRuntime.entrySet().stream()
          .min(
              Map.Entry.comparingByValue(
                  (m1, m2) ->
                      Double.compare(m1.getMeanExecutionTimeMs(), m2.getMeanExecutionTimeMs())))
          .map(Map.Entry::getKey)
          .orElse("UNKNOWN");
    }

    /** Gets the most memory-efficient runtime for this test. */
    public String getMostMemoryEfficientRuntime() {
      return metricsByRuntime.entrySet().stream()
          .min(
              Map.Entry.comparingByValue(
                  (m1, m2) -> Long.compare(m1.getMeanMemoryUsage(), m2.getMeanMemoryUsage())))
          .map(Map.Entry::getKey)
          .orElse("UNKNOWN");
    }

    /**
     * Checks if the performance comparison was successful.
     *
     * @return true if analysis completed successfully
     */
    public boolean isSuccessful() {
      return !results.isEmpty() && !metricsByRuntime.isEmpty();
    }

    /**
     * Gets the peak memory usage across all runtimes.
     *
     * @return peak memory usage in bytes
     */
    public long getPeakMemoryUsage() {
      return metricsByRuntime.values().stream()
          .mapToLong(PerformanceMetrics::getPeakMemoryUsage)
          .max()
          .orElse(0L);
    }

    /**
     * Gets any error message from the analysis.
     *
     * @return error message or null if successful
     */
    public String getErrorMessage() {
      if (isSuccessful()) {
        return null;
      }
      if (results.isEmpty()) {
        return "No test results available for analysis";
      }
      if (metricsByRuntime.isEmpty()) {
        return "No performance metrics could be calculated";
      }
      return "Analysis completed with warnings";
    }

    /**
     * Gets the average timing ratio between fastest and slowest runtime.
     *
     * @return timing ratio or 1.0 if cannot be calculated
     */
    public double getAverageTimingRatio() {
      if (metricsByRuntime.size() < 2) {
        return 1.0;
      }

      DoubleSummaryStatistics stats =
          metricsByRuntime.values().stream()
              .mapToDouble(PerformanceMetrics::getMeanExecutionTimeMs)
              .summaryStatistics();

      return stats.getMax() / Math.max(stats.getMin(), 1.0);
    }

    /**
     * Gets the execution duration (mean execution time) in milliseconds.
     *
     * @return execution duration in milliseconds
     */
    public double getExecutionDuration() {
      return metricsByRuntime.values().stream()
          .mapToDouble(PerformanceMetrics::getMeanExecutionTimeMs)
          .average()
          .orElse(0.0);
    }

    /**
     * Gets the memory used (mean memory usage) in bytes.
     *
     * @return memory used in bytes
     */
    public long getMemoryUsed() {
      return (long)
          metricsByRuntime.values().stream()
              .mapToLong(PerformanceMetrics::getMeanMemoryUsage)
              .average()
              .orElse(0.0);
    }

    /**
     * Gets the runtime type (fastest runtime).
     *
     * @return runtime type
     */
    public String getRuntimeType() {
      return getFastestRuntime();
    }
  }

  /** Performance metrics for a specific runtime and test combination. */
  public static final class PerformanceMetrics {
    private final String runtimeType;
    private final int sampleSize;
    private final double meanExecutionTimeMs;
    private final double medianExecutionTimeMs;
    private final double standardDeviation;
    private final double coefficientOfVariation;
    private final double percentile95;
    private final double percentile99;
    private final long meanMemoryUsage;
    private final long peakMemoryUsage;
    private final double successRate;

    /**
     * Creates performance metrics from test execution results.
     *
     * @param runtimeType the runtime type
     * @param results the test execution results
     */
    public PerformanceMetrics(final String runtimeType, final List<TestExecutionResult> results) {
      this.runtimeType = runtimeType;

      final List<TestExecutionResult> successfulResults =
          results.stream().filter(TestExecutionResult::isSuccessful).collect(Collectors.toList());

      this.sampleSize = results.size();
      this.successRate = sampleSize > 0 ? (double) successfulResults.size() / sampleSize : 0.0;

      if (successfulResults.isEmpty()) {
        this.meanExecutionTimeMs = 0.0;
        this.medianExecutionTimeMs = 0.0;
        this.standardDeviation = 0.0;
        this.coefficientOfVariation = 0.0;
        this.percentile95 = 0.0;
        this.percentile99 = 0.0;
        this.meanMemoryUsage = 0;
        this.peakMemoryUsage = 0;
        return;
      }

      final List<Double> executionTimes =
          successfulResults.stream()
              .mapToDouble(r -> r.getExecutionDuration().toNanos() / 1_000_000.0)
              .boxed()
              .sorted()
              .collect(Collectors.toList());

      final DoubleSummaryStatistics stats =
          successfulResults.stream()
              .mapToDouble(r -> r.getExecutionDuration().toNanos() / 1_000_000.0)
              .summaryStatistics();

      this.meanExecutionTimeMs = stats.getAverage();
      this.medianExecutionTimeMs = calculatePercentile(executionTimes, 50.0);
      this.standardDeviation = calculateStandardDeviation(executionTimes, meanExecutionTimeMs);
      this.coefficientOfVariation =
          meanExecutionTimeMs > 0 ? (standardDeviation / meanExecutionTimeMs) * 100.0 : 0.0;
      this.percentile95 = calculatePercentile(executionTimes, 95.0);
      this.percentile99 = calculatePercentile(executionTimes, 99.0);

      this.meanMemoryUsage =
          (long)
              successfulResults.stream()
                  .mapToLong(TestExecutionResult::getMemoryUsed)
                  .average()
                  .orElse(0.0);

      this.peakMemoryUsage =
          successfulResults.stream()
              .mapToLong(TestExecutionResult::getPeakMemoryUsage)
              .max()
              .orElse(0L);
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public int getSampleSize() {
      return sampleSize;
    }

    public double getMeanExecutionTimeMs() {
      return meanExecutionTimeMs;
    }

    public double getMedianExecutionTimeMs() {
      return medianExecutionTimeMs;
    }

    public double getStandardDeviation() {
      return standardDeviation;
    }

    public double getCoefficientOfVariation() {
      return coefficientOfVariation;
    }

    public double getPercentile95() {
      return percentile95;
    }

    public double getPercentile99() {
      return percentile99;
    }

    public long getMeanMemoryUsage() {
      return meanMemoryUsage;
    }

    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    public double getSuccessRate() {
      return successRate;
    }

    /** Checks if the metrics are statistically reliable. */
    public boolean isStatisticallyReliable() {
      return sampleSize >= MIN_SAMPLE_SIZE && successRate > 0.8 && coefficientOfVariation < 50.0;
    }

    private static double calculatePercentile(
        final List<Double> sortedValues, final double percentile) {
      if (sortedValues.isEmpty()) {
        return 0.0;
      }

      final int index = (int) Math.ceil(sortedValues.size() * percentile / 100.0) - 1;
      final int safeIndex = Math.max(0, Math.min(index, sortedValues.size() - 1));
      return sortedValues.get(safeIndex);
    }

    private static double calculateStandardDeviation(final List<Double> values, final double mean) {
      if (values.size() <= 1) {
        return 0.0;
      }

      final double variance =
          values.stream().mapToDouble(value -> Math.pow(value - mean, 2)).average().orElse(0.0);

      return Math.sqrt(variance);
    }
  }

  /** Overhead analysis comparing runtime performance against baseline. */
  public static final class OverheadAnalysis {
    private final Map<String, Double> runtimeOverheads;
    private final String baselineRuntime;
    private final boolean hasSignificantOverhead;

    /**
     * Creates overhead analysis from performance metrics.
     *
     * @param metricsByRuntime the performance metrics by runtime type
     */
    public OverheadAnalysis(final Map<String, PerformanceMetrics> metricsByRuntime) {
      this.runtimeOverheads = new HashMap<>();

      // Find baseline (fastest runtime)
      this.baselineRuntime =
          metricsByRuntime.entrySet().stream()
              .filter(entry -> entry.getValue().isStatisticallyReliable())
              .min(
                  Map.Entry.comparingByValue(
                      (m1, m2) ->
                          Double.compare(m1.getMeanExecutionTimeMs(), m2.getMeanExecutionTimeMs())))
              .map(Map.Entry::getKey)
              .orElse("UNKNOWN");

      if (!"UNKNOWN".equals(baselineRuntime)) {
        final double baselineTime = metricsByRuntime.get(baselineRuntime).getMeanExecutionTimeMs();

        for (final Map.Entry<String, PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
          final String runtime = entry.getKey();
          final double runtimeTime = entry.getValue().getMeanExecutionTimeMs();
          final double overhead =
              baselineTime > 0 ? (runtimeTime - baselineTime) / baselineTime : 0.0;
          runtimeOverheads.put(runtime, overhead);
        }
      }

      this.hasSignificantOverhead =
          runtimeOverheads.values().stream().anyMatch(overhead -> overhead > REGRESSION_THRESHOLD);
    }

    public Map<String, Double> getRuntimeOverheads() {
      return new HashMap<>(runtimeOverheads);
    }

    public String getBaselineRuntime() {
      return baselineRuntime;
    }

    public boolean hasSignificantOverhead() {
      return hasSignificantOverhead;
    }

    /** Gets the overhead percentage for a specific runtime. */
    public double getOverheadPercentage(final String runtime) {
      return runtimeOverheads.getOrDefault(runtime, 0.0) * 100.0;
    }
  }

  /** Performance baseline for regression detection. */
  public static final class PerformanceBaseline {
    private final String testName;
    private final String runtimeType;
    private final double baselineExecutionTimeMs;
    private final long baselineMemoryUsage;
    private final Instant establishedAt;

    /**
     * Creates a performance baseline from metrics.
     *
     * @param testName the test name
     * @param runtimeType the runtime type
     * @param metrics the performance metrics
     */
    public PerformanceBaseline(
        final String testName, final String runtimeType, final PerformanceMetrics metrics) {
      this.testName = testName;
      this.runtimeType = runtimeType;
      this.baselineExecutionTimeMs = metrics.getMeanExecutionTimeMs();
      this.baselineMemoryUsage = metrics.getMeanMemoryUsage();
      this.establishedAt = Instant.now();
    }

    public String getTestName() {
      return testName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public double getBaselineExecutionTimeMs() {
      return baselineExecutionTimeMs;
    }

    public long getBaselineMemoryUsage() {
      return baselineMemoryUsage;
    }

    public Instant getEstablishedAt() {
      return establishedAt;
    }

    /** Checks if current metrics represent a regression from baseline. */
    public boolean isRegression(final PerformanceMetrics currentMetrics) {
      if (!currentMetrics.isStatisticallyReliable()) {
        return false;
      }

      final double executionTimeChange =
          (currentMetrics.getMeanExecutionTimeMs() - baselineExecutionTimeMs)
              / baselineExecutionTimeMs;
      final double memoryChange =
          baselineMemoryUsage > 0
              ? (double) (currentMetrics.getMeanMemoryUsage() - baselineMemoryUsage)
                  / baselineMemoryUsage
              : 0.0;

      return executionTimeChange > REGRESSION_THRESHOLD || memoryChange > REGRESSION_THRESHOLD;
    }

    /** Gets the performance change percentage compared to baseline. */
    public double getPerformanceChange(final PerformanceMetrics currentMetrics) {
      return (currentMetrics.getMeanExecutionTimeMs() - baselineExecutionTimeMs)
          / baselineExecutionTimeMs;
    }
  }

  /**
   * Analyzes performance across multiple test execution results.
   *
   * @param results the test execution results to analyze
   * @return comprehensive performance comparison result
   */
  public PerformanceComparisonResult analyze(final List<TestExecutionResult> results) {
    Objects.requireNonNull(results, "results cannot be null");

    if (results.isEmpty()) {
      LOGGER.warning("No results provided for analysis");
      return createEmptyResult("EMPTY_ANALYSIS");
    }

    final String testName = results.get(0).getTestName();
    LOGGER.info(
        String.format(
            "Analyzing performance for test: %s with %d results", testName, results.size()));

    // Group results by runtime type
    final Map<String, List<TestExecutionResult>> resultsByRuntime =
        results.stream().collect(Collectors.groupingBy(TestExecutionResult::getRuntimeType));

    // Calculate performance metrics for each runtime
    final Map<String, PerformanceMetrics> metricsByRuntime = new HashMap<>();
    for (final Map.Entry<String, List<TestExecutionResult>> entry : resultsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final List<TestExecutionResult> runtimeResults = entry.getValue();
      final PerformanceMetrics metrics = new PerformanceMetrics(runtime, runtimeResults);
      metricsByRuntime.put(runtime, metrics);
    }

    // Detect significant differences
    final List<String> significantDifferences = detectSignificantDifferences(metricsByRuntime);

    // Check for regressions against baselines
    final List<String> regressionWarnings = detectRegressions(testName, metricsByRuntime);

    // Perform overhead analysis
    final OverheadAnalysis overheadAnalysis = new OverheadAnalysis(metricsByRuntime);

    // Update performance baselines
    updateBaselines(testName, metricsByRuntime);

    return new PerformanceComparisonResult(
        testName,
        results,
        metricsByRuntime,
        significantDifferences,
        regressionWarnings,
        overheadAnalysis);
  }

  /**
   * Establishes performance baselines for future regression detection.
   *
   * @param testName the test name
   * @param metricsByRuntime the performance metrics by runtime
   */
  public void establishBaseline(
      final String testName, final Map<String, PerformanceMetrics> metricsByRuntime) {
    for (final Map.Entry<String, PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceMetrics metrics = entry.getValue();

      if (metrics.isStatisticallyReliable()) {
        final String baselineKey = testName + "_" + runtime;
        final PerformanceBaseline baseline = new PerformanceBaseline(testName, runtime, metrics);
        baselines.put(baselineKey, baseline);
        LOGGER.info(
            String.format(
                "Established baseline for %s: %.2fms execution time",
                baselineKey, metrics.getMeanExecutionTimeMs()));
      }
    }
  }

  /** Clears all established baselines. */
  public void clearBaselines() {
    baselines.clear();
    LOGGER.info("Cleared all performance baselines");
  }

  /** Gets all established baselines. */
  public Map<String, PerformanceBaseline> getBaselines() {
    return new HashMap<>(baselines);
  }

  private List<String> detectSignificantDifferences(
      final Map<String, PerformanceMetrics> metricsByRuntime) {
    final List<String> differences = new ArrayList<>();
    final List<String> runtimes = new ArrayList<>(metricsByRuntime.keySet());

    for (int i = 0; i < runtimes.size(); i++) {
      for (int j = i + 1; j < runtimes.size(); j++) {
        final String runtime1 = runtimes.get(i);
        final String runtime2 = runtimes.get(j);
        final PerformanceMetrics metrics1 = metricsByRuntime.get(runtime1);
        final PerformanceMetrics metrics2 = metricsByRuntime.get(runtime2);

        if (metrics1.isStatisticallyReliable() && metrics2.isStatisticallyReliable()) {
          final double difference =
              Math.abs(metrics1.getMeanExecutionTimeMs() - metrics2.getMeanExecutionTimeMs());
          final double relativeDifference =
              difference
                  / Math.min(metrics1.getMeanExecutionTimeMs(), metrics2.getMeanExecutionTimeMs());

          if (relativeDifference > DEFAULT_SIGNIFICANCE_LEVEL) {
            final String fasterRuntime =
                metrics1.getMeanExecutionTimeMs() < metrics2.getMeanExecutionTimeMs()
                    ? runtime1
                    : runtime2;
            final String slowerRuntime = fasterRuntime.equals(runtime1) ? runtime2 : runtime1;
            differences.add(
                String.format(
                    "%s is %.1f%% faster than %s",
                    fasterRuntime, relativeDifference * 100, slowerRuntime));
          }
        }
      }
    }

    return differences;
  }

  private List<String> detectRegressions(
      final String testName, final Map<String, PerformanceMetrics> metricsByRuntime) {
    final List<String> regressions = new ArrayList<>();

    for (final Map.Entry<String, PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceMetrics currentMetrics = entry.getValue();
      final String baselineKey = testName + "_" + runtime;
      final PerformanceBaseline baseline = baselines.get(baselineKey);

      if (baseline != null && baseline.isRegression(currentMetrics)) {
        final double changePercentage = baseline.getPerformanceChange(currentMetrics) * 100;
        regressions.add(
            String.format("%s shows %.1f%% performance regression", runtime, changePercentage));
      }
    }

    return regressions;
  }

  private void updateBaselines(
      final String testName, final Map<String, PerformanceMetrics> metricsByRuntime) {
    for (final Map.Entry<String, PerformanceMetrics> entry : metricsByRuntime.entrySet()) {
      final String runtime = entry.getKey();
      final PerformanceMetrics metrics = entry.getValue();
      final String baselineKey = testName + "_" + runtime;

      if (metrics.isStatisticallyReliable() && !baselines.containsKey(baselineKey)) {
        establishBaseline(testName, Map.of(runtime, metrics));
      }
    }
  }

  private PerformanceComparisonResult createEmptyResult(final String testName) {
    return new PerformanceComparisonResult(
        testName,
        Collections.emptyList(),
        Collections.emptyMap(),
        Collections.emptyList(),
        Collections.emptyList(),
        new OverheadAnalysis(Collections.emptyMap()));
  }
}
