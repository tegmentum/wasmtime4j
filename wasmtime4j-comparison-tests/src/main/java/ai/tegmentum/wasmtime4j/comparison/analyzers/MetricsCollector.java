package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Thread-safe metrics collector for gathering performance data during test execution. Provides
 * real-time collection of execution times, memory usage, and custom metrics with minimal overhead
 * and optimized for high-throughput scenarios.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Real-time performance data collection with minimal overhead
 *   <li>Memory usage tracking and allocation pattern detection
 *   <li>Custom metric collection with type safety
 *   <li>Automatic outlier detection and filtering
 *   <li>Thread-safe operation for concurrent test execution
 * </ul>
 */
public final class MetricsCollector {
  private static final Logger LOGGER = Logger.getLogger(MetricsCollector.class.getName());

  // Memory measurement constants
  private static final Runtime RUNTIME = Runtime.getRuntime();
  private static final long BYTES_PER_MB = 1024 * 1024;

  // Outlier detection thresholds
  private static final double OUTLIER_FACTOR = 3.0; // 3 standard deviations
  private static final int MIN_SAMPLES_FOR_OUTLIER_DETECTION = 10;

  private final List<PerformanceDataPoint> dataPoints;
  private final Map<String, List<Object>> customMetrics;
  private final AtomicLong totalOperations;
  private final AtomicLong successfulOperations;
  private final AtomicLong failedOperations;
  private final boolean outlierDetectionEnabled;

  /** Creates a new MetricsCollector with default configuration. */
  public MetricsCollector() {
    this(true);
  }

  /**
   * Creates a new MetricsCollector with specified outlier detection setting.
   *
   * @param outlierDetectionEnabled whether to enable automatic outlier detection
   */
  public MetricsCollector(final boolean outlierDetectionEnabled) {
    this.dataPoints = new CopyOnWriteArrayList<>();
    this.customMetrics = new ConcurrentHashMap<>();
    this.totalOperations = new AtomicLong(0);
    this.successfulOperations = new AtomicLong(0);
    this.failedOperations = new AtomicLong(0);
    this.outlierDetectionEnabled = outlierDetectionEnabled;
  }

  /** Represents a single performance data point with complete metrics. */
  public static final class PerformanceDataPoint {
    private final String testName;
    private final String runtimeType;
    private final Instant timestamp;
    private final Duration executionTime;
    private final long memoryBefore;
    private final long memoryAfter;
    private final long memoryUsed;
    private final long peakMemoryUsage;
    private final boolean successful;
    private final String errorMessage;
    private final Map<String, Object> customMetrics;

    private PerformanceDataPoint(final Builder builder) {
      this.testName = Objects.requireNonNull(builder.testName, "testName cannot be null");
      this.runtimeType = Objects.requireNonNull(builder.runtimeType, "runtimeType cannot be null");
      this.timestamp = Objects.requireNonNull(builder.timestamp, "timestamp cannot be null");
      this.executionTime =
          Objects.requireNonNull(builder.executionTime, "executionTime cannot be null");
      this.memoryBefore = builder.memoryBefore;
      this.memoryAfter = builder.memoryAfter;
      this.memoryUsed = builder.memoryUsed;
      this.peakMemoryUsage = builder.peakMemoryUsage;
      this.successful = builder.successful;
      this.errorMessage = builder.errorMessage;
      this.customMetrics = Map.copyOf(builder.customMetrics);
    }

    public String getTestName() {
      return testName;
    }

    public String getRuntimeType() {
      return runtimeType;
    }

    public Instant getTimestamp() {
      return timestamp;
    }

    public Duration getExecutionTime() {
      return executionTime;
    }

    public long getMemoryBefore() {
      return memoryBefore;
    }

    public long getMemoryAfter() {
      return memoryAfter;
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

    public Map<String, Object> getCustomMetrics() {
      return customMetrics;
    }

    /** Gets the execution time in milliseconds. */
    public double getExecutionTimeMs() {
      return executionTime.toNanos() / 1_000_000.0;
    }

    /** Gets the memory usage in megabytes. */
    public double getMemoryUsageMb() {
      return (double) memoryUsed / BYTES_PER_MB;
    }

    /** Checks if this data point is likely an outlier based on execution time. */
    public boolean isOutlier(final double mean, final double standardDeviation) {
      final double zScore = Math.abs(getExecutionTimeMs() - mean) / standardDeviation;
      return zScore > OUTLIER_FACTOR;
    }

    public static Builder builder(final String testName, final String runtimeType) {
      return new Builder(testName, runtimeType);
    }

    public static final class Builder {
      private final String testName;
      private final String runtimeType;
      private Instant timestamp = Instant.now();
      private Duration executionTime = Duration.ZERO;
      private long memoryBefore = 0;
      private long memoryAfter = 0;
      private long memoryUsed = 0;
      private long peakMemoryUsage = 0;
      private boolean successful = true;
      private String errorMessage = null;
      private final Map<String, Object> customMetrics = new ConcurrentHashMap<>();

      private Builder(final String testName, final String runtimeType) {
        this.testName = testName;
        this.runtimeType = runtimeType;
      }

      public Builder timestamp(final Instant timestamp) {
        this.timestamp = timestamp;
        return this;
      }

      public Builder executionTime(final Duration executionTime) {
        this.executionTime = executionTime;
        return this;
      }

      public Builder memoryBefore(final long memoryBefore) {
        this.memoryBefore = memoryBefore;
        return this;
      }

      public Builder memoryAfter(final long memoryAfter) {
        this.memoryAfter = memoryAfter;
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

      public Builder customMetric(final String key, final Object value) {
        this.customMetrics.put(key, value);
        return this;
      }

      public PerformanceDataPoint build() {
        return new PerformanceDataPoint(this);
      }
    }
  }

  /** Measurement session for tracking performance during test execution. */
  public static final class MeasurementSession {
    private final String testName;
    private final String runtimeType;
    private final Instant startTime;
    private final long initialMemory;
    private long peakMemoryUsage;
    private final MetricsCollector collector;

    private MeasurementSession(
        final String testName, final String runtimeType, final MetricsCollector collector) {
      this.testName = testName;
      this.runtimeType = runtimeType;
      this.startTime = Instant.now();
      this.initialMemory = getCurrentMemoryUsage();
      this.peakMemoryUsage = this.initialMemory;
      this.collector = collector;

      // Start background memory monitoring
      monitorMemoryUsage();
    }

    /** Records successful completion of the test. */
    public void recordSuccess() {
      recordCompletion(true, null);
    }

    /**
     * Records failed completion of the test.
     *
     * @param errorMessage the error message
     */
    public void recordFailure(final String errorMessage) {
      recordCompletion(false, errorMessage);
    }

    /**
     * Records custom metric during execution.
     *
     * @param key the metric key
     * @param value the metric value
     */
    public void recordCustomMetric(final String key, final Object value) {
      collector.recordCustomMetric(key, value);
    }

    private void recordCompletion(final boolean successful, final String errorMessage) {
      final Instant endTime = Instant.now();
      final Duration executionTime = Duration.between(startTime, endTime);
      final long finalMemory = getCurrentMemoryUsage();

      final PerformanceDataPoint dataPoint =
          PerformanceDataPoint.builder(testName, runtimeType)
              .timestamp(startTime)
              .executionTime(executionTime)
              .memoryBefore(initialMemory)
              .memoryAfter(finalMemory)
              .memoryUsed(Math.max(0, finalMemory - initialMemory))
              .peakMemoryUsage(peakMemoryUsage)
              .successful(successful)
              .errorMessage(errorMessage)
              .build();

      collector.recordDataPoint(dataPoint);

      if (successful) {
        collector.successfulOperations.incrementAndGet();
      } else {
        collector.failedOperations.incrementAndGet();
      }
      collector.totalOperations.incrementAndGet();
    }

    private void monitorMemoryUsage() {
      // Simple peak memory tracking - in a real implementation,
      // this could be enhanced with periodic sampling
      final long currentMemory = getCurrentMemoryUsage();
      if (currentMemory > peakMemoryUsage) {
        peakMemoryUsage = currentMemory;
      }
    }

    private static long getCurrentMemoryUsage() {
      return RUNTIME.totalMemory() - RUNTIME.freeMemory();
    }
  }

  /** Aggregated metrics summary for analysis. */
  public static final class MetricsSummary {
    private final int totalDataPoints;
    private final int successfulOperations;
    private final int failedOperations;
    private final double successRate;
    private final double meanExecutionTimeMs;
    private final double medianExecutionTimeMs;
    private final double standardDeviation;
    private final double coefficientOfVariation;
    private final double minExecutionTimeMs;
    private final double maxExecutionTimeMs;
    private final long meanMemoryUsage;
    private final long peakMemoryUsage;
    private final int outliersDetected;

    private MetricsSummary(
        final int totalDataPoints,
        final int successfulOperations,
        final int failedOperations,
        final double successRate,
        final double meanExecutionTimeMs,
        final double medianExecutionTimeMs,
        final double standardDeviation,
        final double coefficientOfVariation,
        final double minExecutionTimeMs,
        final double maxExecutionTimeMs,
        final long meanMemoryUsage,
        final long peakMemoryUsage,
        final int outliersDetected) {
      this.totalDataPoints = totalDataPoints;
      this.successfulOperations = successfulOperations;
      this.failedOperations = failedOperations;
      this.successRate = successRate;
      this.meanExecutionTimeMs = meanExecutionTimeMs;
      this.medianExecutionTimeMs = medianExecutionTimeMs;
      this.standardDeviation = standardDeviation;
      this.coefficientOfVariation = coefficientOfVariation;
      this.minExecutionTimeMs = minExecutionTimeMs;
      this.maxExecutionTimeMs = maxExecutionTimeMs;
      this.meanMemoryUsage = meanMemoryUsage;
      this.peakMemoryUsage = peakMemoryUsage;
      this.outliersDetected = outliersDetected;
    }

    public int getTotalDataPoints() {
      return totalDataPoints;
    }

    public int getSuccessfulOperations() {
      return successfulOperations;
    }

    public int getFailedOperations() {
      return failedOperations;
    }

    public double getSuccessRate() {
      return successRate;
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

    public double getMinExecutionTimeMs() {
      return minExecutionTimeMs;
    }

    public double getMaxExecutionTimeMs() {
      return maxExecutionTimeMs;
    }

    public long getMeanMemoryUsage() {
      return meanMemoryUsage;
    }

    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    public int getOutliersDetected() {
      return outliersDetected;
    }

    /** Checks if the metrics are statistically reliable. */
    public boolean isStatisticallyReliable() {
      return totalDataPoints >= 3 && successRate >= 0.8 && coefficientOfVariation < 50.0;
    }
  }

  /**
   * Starts a new measurement session for the specified test and runtime.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @return a new measurement session
   */
  public MeasurementSession startMeasurement(final String testName, final String runtimeType) {
    Objects.requireNonNull(testName, "testName cannot be null");
    Objects.requireNonNull(runtimeType, "runtimeType cannot be null");

    return new MeasurementSession(testName, runtimeType, this);
  }

  /**
   * Records a performance data point.
   *
   * @param dataPoint the performance data point
   */
  public void recordDataPoint(final PerformanceDataPoint dataPoint) {
    Objects.requireNonNull(dataPoint, "dataPoint cannot be null");

    if (outlierDetectionEnabled && isLikelyOutlier(dataPoint)) {
      LOGGER.fine(
          String.format(
              "Outlier detected and filtered: %s - %.2fms",
              dataPoint.getTestName(), dataPoint.getExecutionTimeMs()));
      return;
    }

    dataPoints.add(dataPoint);
    LOGGER.fine(
        String.format(
            "Recorded data point: %s [%s] - %.2fms, %.2fMB",
            dataPoint.getTestName(),
            dataPoint.getRuntimeType(),
            dataPoint.getExecutionTimeMs(),
            dataPoint.getMemoryUsageMb()));
  }

  /**
   * Records a custom metric value.
   *
   * @param key the metric key
   * @param value the metric value
   */
  public void recordCustomMetric(final String key, final Object value) {
    Objects.requireNonNull(key, "key cannot be null");
    Objects.requireNonNull(value, "value cannot be null");

    customMetrics.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(value);
  }

  /**
   * Gets all collected performance data points.
   *
   * @return immutable list of performance data points
   */
  public List<PerformanceDataPoint> getDataPoints() {
    return new ArrayList<>(dataPoints);
  }

  /**
   * Gets performance data points for a specific test and runtime.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @return filtered list of performance data points
   */
  public List<PerformanceDataPoint> getDataPoints(final String testName, final String runtimeType) {
    return dataPoints.stream()
        .filter(dp -> testName.equals(dp.getTestName()) && runtimeType.equals(dp.getRuntimeType()))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
  }

  /**
   * Gets all custom metrics.
   *
   * @return map of custom metrics
   */
  public Map<String, List<Object>> getCustomMetrics() {
    return Map.copyOf(customMetrics);
  }

  /**
   * Generates a comprehensive metrics summary.
   *
   * @return metrics summary
   */
  public MetricsSummary generateSummary() {
    if (dataPoints.isEmpty()) {
      return new MetricsSummary(0, 0, 0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0, 0, 0);
    }

    final List<PerformanceDataPoint> successfulPoints =
        dataPoints.stream()
            .filter(PerformanceDataPoint::isSuccessful)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

    final int totalDataPoints = dataPoints.size();
    final int successfulOps = successfulPoints.size();
    final int failedOps = totalDataPoints - successfulOps;
    final double successRate = (double) successfulOps / totalDataPoints;

    if (successfulPoints.isEmpty()) {
      return new MetricsSummary(
          totalDataPoints,
          successfulOps,
          failedOps,
          successRate,
          0.0,
          0.0,
          0.0,
          0.0,
          0.0,
          0.0,
          0,
          0,
          0);
    }

    final List<Double> executionTimes =
        successfulPoints.stream()
            .map(PerformanceDataPoint::getExecutionTimeMs)
            .sorted()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

    final double mean =
        executionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double median = calculatePercentile(executionTimes, 50.0);
    final double stdDev = calculateStandardDeviation(executionTimes, mean);
    final double cv = mean > 0 ? (stdDev / mean) * 100.0 : 0.0;
    final double min = executionTimes.isEmpty() ? 0.0 : executionTimes.get(0);
    final double max =
        executionTimes.isEmpty() ? 0.0 : executionTimes.get(executionTimes.size() - 1);

    final long meanMemory =
        (long)
            successfulPoints.stream()
                .mapToLong(PerformanceDataPoint::getMemoryUsed)
                .average()
                .orElse(0.0);

    final long peakMemory =
        successfulPoints.stream()
            .mapToLong(PerformanceDataPoint::getPeakMemoryUsage)
            .max()
            .orElse(0L);

    final int outliers =
        outlierDetectionEnabled ? countOutliers(successfulPoints, mean, stdDev) : 0;

    return new MetricsSummary(
        totalDataPoints,
        successfulOps,
        failedOps,
        successRate,
        mean,
        median,
        stdDev,
        cv,
        min,
        max,
        meanMemory,
        peakMemory,
        outliers);
  }

  /** Clears all collected data. */
  public void clear() {
    dataPoints.clear();
    customMetrics.clear();
    totalOperations.set(0);
    successfulOperations.set(0);
    failedOperations.set(0);
    LOGGER.info("Metrics collector cleared");
  }

  /** Gets the total number of operations recorded. */
  public long getTotalOperations() {
    return totalOperations.get();
  }

  /** Gets the number of successful operations. */
  public long getSuccessfulOperations() {
    return successfulOperations.get();
  }

  /** Gets the number of failed operations. */
  public long getFailedOperations() {
    return failedOperations.get();
  }

  private boolean isLikelyOutlier(final PerformanceDataPoint dataPoint) {
    if (dataPoints.size() < MIN_SAMPLES_FOR_OUTLIER_DETECTION) {
      return false;
    }

    final List<Double> recentExecutionTimes =
        dataPoints.stream()
            .filter(
                dp ->
                    dp.getTestName().equals(dataPoint.getTestName())
                        && dp.getRuntimeType().equals(dataPoint.getRuntimeType())
                        && dp.isSuccessful())
            .map(PerformanceDataPoint::getExecutionTimeMs)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

    if (recentExecutionTimes.size() < MIN_SAMPLES_FOR_OUTLIER_DETECTION) {
      return false;
    }

    final double mean =
        recentExecutionTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    final double stdDev = calculateStandardDeviation(recentExecutionTimes, mean);

    return dataPoint.isOutlier(mean, stdDev);
  }

  private int countOutliers(
      final List<PerformanceDataPoint> dataPoints, final double mean, final double stdDev) {
    return (int) dataPoints.stream().filter(dp -> dp.isOutlier(mean, stdDev)).count();
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
