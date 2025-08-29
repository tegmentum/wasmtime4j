package ai.tegmentum.wasmtime4j.benchmarks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Performance regression detection framework for WebAssembly benchmarks.
 *
 * <p>This class provides automated detection of performance regressions by comparing benchmark
 * results against historical baselines and detecting significant performance degradations.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Statistical analysis of benchmark results with confidence intervals
 *   <li>Baseline establishment and tracking over time
 *   <li>Automated regression detection with configurable thresholds
 *   <li>Performance report generation for CI/CD integration
 *   <li>Cross-runtime performance comparison analysis
 * </ul>
 */
public final class PerformanceRegressionDetector {

  /** Performance threshold for regression detection (5% degradation). */
  private static final double REGRESSION_THRESHOLD = 0.05;

  /** Minimum number of samples required for statistical analysis. */
  private static final int MIN_SAMPLE_SIZE = 5;

  /** Confidence level for statistical tests (95%). */
  private static final double CONFIDENCE_LEVEL = 0.95;

  /** Performance measurement result. */
  public static final class PerformanceMeasurement {
    private final String benchmarkName;
    private final String runtimeType;
    private final double throughput;
    private final double latency;
    private final long memoryUsage;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;

    public PerformanceMeasurement(
        final String benchmarkName,
        final String runtimeType,
        final double throughput,
        final double latency,
        final long memoryUsage) {
      this.benchmarkName = benchmarkName;
      this.runtimeType = runtimeType;
      this.throughput = throughput;
      this.latency = latency;
      this.memoryUsage = memoryUsage;
      this.timestamp = LocalDateTime.now();
      this.metadata = new HashMap<>();
    }

    public PerformanceMeasurement withMetadata(final String key, final Object value) {
      this.metadata.put(key, value);
      return this;
    }

    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public double getThroughput() {
      return throughput;
    }

    public double getLatency() {
      return latency;
    }

    public long getMemoryUsage() {
      return memoryUsage;
    }

    public LocalDateTime getTimestamp() {
      return timestamp;
    }

    public Map<String, Object> getMetadata() {
      return new HashMap<>(metadata);
    }

    @Override
    public String toString() {
      return String.format(
          "PerformanceMeasurement{benchmark='%s', runtime='%s', throughput=%.2f, "
              + "latency=%.2f, memory=%d, timestamp=%s}",
          benchmarkName, runtimeType, throughput, latency, memoryUsage, timestamp);
    }
  }

  /** Statistical summary of performance measurements. */
  public static final class PerformanceStatistics {
    private final double meanThroughput;
    private final double stdDevThroughput;
    private final double meanLatency;
    private final double stdDevLatency;
    private final long meanMemoryUsage;
    private final int sampleCount;
    private final double confidenceInterval;

    public PerformanceStatistics(
        final List<PerformanceMeasurement> measurements, final double confidenceLevel) {
      this.sampleCount = measurements.size();
      this.confidenceInterval = confidenceLevel;

      if (measurements.isEmpty()) {
        this.meanThroughput = 0.0;
        this.stdDevThroughput = 0.0;
        this.meanLatency = 0.0;
        this.stdDevLatency = 0.0;
        this.meanMemoryUsage = 0;
        return;
      }

      // Calculate throughput statistics
      final double sumThroughput =
          measurements.stream().mapToDouble(PerformanceMeasurement::getThroughput).sum();
      this.meanThroughput = sumThroughput / measurements.size();

      final double sumSquaredDiffThroughput =
          measurements.stream()
              .mapToDouble(m -> Math.pow(m.getThroughput() - meanThroughput, 2))
              .sum();
      this.stdDevThroughput =
          measurements.size() > 1
              ? Math.sqrt(sumSquaredDiffThroughput / (measurements.size() - 1))
              : 0.0;

      // Calculate latency statistics
      final double sumLatency =
          measurements.stream().mapToDouble(PerformanceMeasurement::getLatency).sum();
      this.meanLatency = sumLatency / measurements.size();

      final double sumSquaredDiffLatency =
          measurements.stream()
              .mapToDouble(m -> Math.pow(m.getLatency() - meanLatency, 2))
              .sum();
      this.stdDevLatency =
          measurements.size() > 1
              ? Math.sqrt(sumSquaredDiffLatency / (measurements.size() - 1))
              : 0.0;

      // Calculate memory usage statistics
      this.meanMemoryUsage =
          (long) measurements.stream().mapToLong(PerformanceMeasurement::getMemoryUsage).average().orElse(0.0);
    }

    public double getMeanThroughput() {
      return meanThroughput;
    }

    public double getStdDevThroughput() {
      return stdDevThroughput;
    }

    public double getMeanLatency() {
      return meanLatency;
    }

    public double getStdDevLatency() {
      return stdDevLatency;
    }

    public long getMeanMemoryUsage() {
      return meanMemoryUsage;
    }

    public int getSampleCount() {
      return sampleCount;
    }

    public double getThroughputConfidenceInterval() {
      if (sampleCount < 2) {
        return 0.0;
      }
      // Simplified confidence interval calculation (assumes normal distribution)
      final double tValue = getTValueForConfidenceLevel(confidenceInterval, sampleCount - 1);
      return tValue * (stdDevThroughput / Math.sqrt(sampleCount));
    }

    public double getLatencyConfidenceInterval() {
      if (sampleCount < 2) {
        return 0.0;
      }
      final double tValue = getTValueForConfidenceLevel(confidenceLevel, sampleCount - 1);
      return tValue * (stdDevLatency / Math.sqrt(sampleCount));
    }

    private double getTValueForConfidenceLevel(final double confidenceLevel, final int degreesOfFreedom) {
      // Simplified t-value calculation for common confidence levels
      if (confidenceLevel >= 0.95) {
        if (degreesOfFreedom >= 30) {
          return 1.96;
        } else if (degreesOfFreedom >= 10) {
          return 2.23;
        } else {
          return 2.78;
        }
      } else if (confidenceLevel >= 0.90) {
        if (degreesOfFreedom >= 30) {
          return 1.645;
        } else {
          return 1.833;
        }
      }
      return 1.96; // Default to 95% confidence
    }

    @Override
    public String toString() {
      return String.format(
          "PerformanceStatistics{throughput: %.2f ± %.2f, latency: %.2f ± %.2f, "
              + "memory: %d, samples: %d}",
          meanThroughput,
          getThroughputConfidenceInterval(),
          meanLatency,
          getLatencyConfidenceInterval(),
          meanMemoryUsage,
          sampleCount);
    }
  }

  /** Regression detection result. */
  public static final class RegressionResult {
    private final String benchmarkName;
    private final String runtimeType;
    private final boolean isRegression;
    private final double performanceChange;
    private final String description;
    private final PerformanceStatistics baseline;
    private final PerformanceStatistics current;

    public RegressionResult(
        final String benchmarkName,
        final String runtimeType,
        final boolean isRegression,
        final double performanceChange,
        final String description,
        final PerformanceStatistics baseline,
        final PerformanceStatistics current) {
      this.benchmarkName = benchmarkName;
      this.runtimeType = runtimeType;
      this.isRegression = isRegression;
      this.performanceChange = performanceChange;
      this.description = description;
      this.baseline = baseline;
      this.current = current;
    }

    public String getBenchmarkName() {
      return benchmarkName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public boolean isRegression() {
      return isRegression;
    }

    public double getPerformanceChange() {
      return performanceChange;
    }

    public String getDescription() {
      return description;
    }

    public PerformanceStatistics getBaseline() {
      return baseline;
    }

    public PerformanceStatistics getCurrent() {
      return current;
    }

    @Override
    public String toString() {
      return String.format(
          "RegressionResult{benchmark='%s', runtime='%s', regression=%s, change=%.1f%%, '%s'}",
          benchmarkName, runtimeType, isRegression, performanceChange * 100, description);
    }
  }

  /** Performance baseline storage and management. */
  private final Map<String, List<PerformanceMeasurement>> baselineData;
  private final Path baselineStoragePath;

  public PerformanceRegressionDetector() {
    this.baselineData = new HashMap<>();
    this.baselineStoragePath = Paths.get(System.getProperty("user.home"), ".wasmtime4j-benchmarks");
  }

  public PerformanceRegressionDetector(final Path baselineStoragePath) {
    this.baselineData = new HashMap<>();
    this.baselineStoragePath = baselineStoragePath;
  }

  /**
   * Establishes a performance baseline from a set of measurements.
   *
   * @param measurements the baseline measurements
   */
  public void establishBaseline(final List<PerformanceMeasurement> measurements) {
    for (final PerformanceMeasurement measurement : measurements) {
      final String key = getBaselineKey(measurement.getBenchmarkName(), measurement.getRuntimeType());
      baselineData.computeIfAbsent(key, k -> new ArrayList<>()).add(measurement);
    }
    saveBaselines();
  }

  /**
   * Detects performance regressions by comparing current measurements against baselines.
   *
   * @param currentMeasurements the current performance measurements
   * @return list of regression detection results
   */
  public List<RegressionResult> detectRegressions(final List<PerformanceMeasurement> currentMeasurements) {
    loadBaselines();

    final List<RegressionResult> results = new ArrayList<>();
    final Map<String, List<PerformanceMeasurement>> currentData = groupMeasurements(currentMeasurements);

    for (final Map.Entry<String, List<PerformanceMeasurement>> entry : currentData.entrySet()) {
      final String key = entry.getKey();
      final List<PerformanceMeasurement> current = entry.getValue();
      final List<PerformanceMeasurement> baseline = baselineData.get(key);

      if (baseline == null || baseline.size() < MIN_SAMPLE_SIZE) {
        continue; // No baseline or insufficient data
      }

      if (current.size() < MIN_SAMPLE_SIZE) {
        continue; // Insufficient current data
      }

      final PerformanceStatistics baselineStats = new PerformanceStatistics(baseline, CONFIDENCE_LEVEL);
      final PerformanceStatistics currentStats = new PerformanceStatistics(current, CONFIDENCE_LEVEL);

      final RegressionResult result = analyzeRegression(key, baselineStats, currentStats);
      results.add(result);
    }

    return results;
  }

  /**
   * Generates a performance report with statistics and regression analysis.
   *
   * @param measurements the performance measurements to analyze
   * @return formatted performance report
   */
  public String generatePerformanceReport(final List<PerformanceMeasurement> measurements) {
    final StringBuilder report = new StringBuilder();
    report.append("WebAssembly Performance Report\n");
    report.append("Generated: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n");
    report.append("="

).append("\n\n");

    final Map<String, List<PerformanceMeasurement>> groupedData = groupMeasurements(measurements);

    for (final Map.Entry<String, List<PerformanceMeasurement>> entry : groupedData.entrySet()) {
      final String key = entry.getKey();
      final List<PerformanceMeasurement> data = entry.getValue();
      final String[] parts = key.split(":");
      final String benchmarkName = parts[0];
      final String runtimeType = parts[1];

      report.append(String.format("Benchmark: %s (%s Runtime)\n", benchmarkName, runtimeType));
      report.append("---------------------------------------------\n");

      final PerformanceStatistics stats = new PerformanceStatistics(data, CONFIDENCE_LEVEL);
      report.append(String.format("Samples: %d\n", stats.getSampleCount()));
      report.append(String.format("Throughput: %.2f ± %.2f ops/sec\n", 
          stats.getMeanThroughput(), stats.getThroughputConfidenceInterval()));
      report.append(String.format("Latency: %.2f ± %.2f ms\n", 
          stats.getMeanLatency(), stats.getLatencyConfidenceInterval()));
      report.append(String.format("Memory Usage: %d bytes\n", stats.getMeanMemoryUsage()));
      report.append("\n");
    }

    // Add regression analysis if baselines exist
    final List<RegressionResult> regressions = detectRegressions(measurements);
    if (!regressions.isEmpty()) {
      report.append("Regression Analysis\n");
      report.append("==================\n");

      for (final RegressionResult regression : regressions) {
        if (regression.isRegression()) {
          report.append(String.format("⚠️  REGRESSION DETECTED: %s\n", regression));
        } else {
          report.append(String.format("✅ No regression: %s\n", regression));
        }
      }
      report.append("\n");
    }

    return report.toString();
  }

  /**
   * Updates baseline data with new measurements.
   *
   * @param newMeasurements the new measurements to add to baselines
   */
  public void updateBaseline(final List<PerformanceMeasurement> newMeasurements) {
    for (final PerformanceMeasurement measurement : newMeasurements) {
      final String key = getBaselineKey(measurement.getBenchmarkName(), measurement.getRuntimeType());
      final List<PerformanceMeasurement> baseline = baselineData.computeIfAbsent(key, k -> new ArrayList<>());
      
      // Keep only recent measurements (last 100 per benchmark/runtime combination)
      baseline.add(measurement);
      if (baseline.size() > 100) {
        baseline.remove(0);
      }
    }
    saveBaselines();
  }

  private String getBaselineKey(final String benchmarkName, final String runtimeType) {
    return benchmarkName + ":" + runtimeType;
  }

  private Map<String, List<PerformanceMeasurement>> groupMeasurements(
      final List<PerformanceMeasurement> measurements) {
    final Map<String, List<PerformanceMeasurement>> grouped = new HashMap<>();
    for (final PerformanceMeasurement measurement : measurements) {
      final String key = getBaselineKey(measurement.getBenchmarkName(), measurement.getRuntimeType());
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(measurement);
    }
    return grouped;
  }

  private RegressionResult analyzeRegression(
      final String key, final PerformanceStatistics baseline, final PerformanceStatistics current) {
    final String[] parts = key.split(":");
    final String benchmarkName = parts[0];
    final String runtimeType = parts[1];

    // Calculate throughput change (positive means improvement, negative means degradation)
    final double throughputChange = 
        (current.getMeanThroughput() - baseline.getMeanThroughput()) / baseline.getMeanThroughput();

    // Check if the change is statistically significant and represents a regression
    final boolean isRegression = throughputChange < -REGRESSION_THRESHOLD && 
        isStatisticallySignificant(baseline, current);

    final String description = String.format(
        "Throughput changed by %.1f%% from %.2f to %.2f ops/sec",
        throughputChange * 100, baseline.getMeanThroughput(), current.getMeanThroughput());

    return new RegressionResult(
        benchmarkName, runtimeType, isRegression, throughputChange, description, baseline, current);
  }

  private boolean isStatisticallySignificant(
      final PerformanceStatistics baseline, final PerformanceStatistics current) {
    // Simplified statistical significance test
    // In a real implementation, you would use proper t-tests or Mann-Whitney U tests
    final double baselineCI = baseline.getThroughputConfidenceInterval();
    final double currentCI = current.getThroughputConfidenceInterval();
    final double difference = Math.abs(baseline.getMeanThroughput() - current.getMeanThroughput());
    final double combinedCI = Math.sqrt(baselineCI * baselineCI + currentCI * currentCI);
    
    return difference > combinedCI;
  }

  private void saveBaselines() {
    // In a real implementation, this would serialize the baseline data to disk
    // For now, we'll just ensure the storage directory exists
    try {
      Files.createDirectories(baselineStoragePath);
    } catch (final IOException e) {
      // Ignore storage errors for benchmark purposes
    }
  }

  private void loadBaselines() {
    // In a real implementation, this would load baseline data from disk
    // For now, we'll use in-memory storage only
  }
}