package ai.tegmentum.wasmtime4j.validation.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Basic metrics collector for simple performance validation without BI overhead.
 *
 * <p>This collector tracks fundamental performance metrics including execution times, operation
 * counts, and success rates. It provides simple statistical analysis without complex business
 * intelligence features, making it suitable for CI/CD integration and basic performance validation.
 *
 * @since 1.0.0
 */
public final class BasicMetricsCollector {

  private static final Logger LOGGER = Logger.getLogger(BasicMetricsCollector.class.getName());

  private final ConcurrentMap<String, MetricData> metrics = new ConcurrentHashMap<>();
  private final AtomicLong totalOperations = new AtomicLong(0);
  private final AtomicLong successfulOperations = new AtomicLong(0);
  private final AtomicLong failedOperations = new AtomicLong(0);
  private final Instant startTime = Instant.now();

  /**
   * Records a successful operation with its execution time.
   *
   * @param operationName the name of the operation
   * @param executionTime the execution time
   * @throws IllegalArgumentException if operationName is null or executionTime is negative
   */
  public void recordSuccess(final String operationName, final Duration executionTime) {
    validateOperationName(operationName);
    validateExecutionTime(executionTime);

    final MetricData metricData = metrics.computeIfAbsent(operationName, MetricData::new);
    metricData.recordSuccess(executionTime);

    totalOperations.incrementAndGet();
    successfulOperations.incrementAndGet();

    LOGGER.fine(
        "Recorded successful operation '"
            + operationName
            + "' in "
            + executionTime.toMillis()
            + "ms");
  }

  /**
   * Records a failed operation with its execution time.
   *
   * @param operationName the name of the operation
   * @param executionTime the execution time
   * @throws IllegalArgumentException if operationName is null or executionTime is negative
   */
  public void recordFailure(final String operationName, final Duration executionTime) {
    validateOperationName(operationName);
    validateExecutionTime(executionTime);

    final MetricData metricData = metrics.computeIfAbsent(operationName, MetricData::new);
    metricData.recordFailure(executionTime);

    totalOperations.incrementAndGet();
    failedOperations.incrementAndGet();

    LOGGER.fine(
        "Recorded failed operation '" + operationName + "' in " + executionTime.toMillis() + "ms");
  }

  /**
   * Records an operation with automatic timing.
   *
   * @param operationName the name of the operation
   * @param operation the operation to execute and time
   * @param <T> the return type of the operation
   * @return the result of the operation
   * @throws Exception if the operation throws an exception
   */
  @SuppressWarnings({"PMD.SignatureDeclareThrowsException", "PMD.AvoidCatchingGenericException"})
  public <T> T recordOperation(final String operationName, final TimedOperation<T> operation)
      throws Exception {
    validateOperationName(operationName);
    Objects.requireNonNull(operation, "operation cannot be null");

    final Instant start = Instant.now();
    try {
      final T result = operation.execute();
      final Duration duration = Duration.between(start, Instant.now());
      recordSuccess(operationName, duration);
      return result;
    } catch (final Exception e) {
      final Duration duration = Duration.between(start, Instant.now());
      recordFailure(operationName, duration);
      throw e;
    }
  }

  /**
   * Gets metrics for a specific operation.
   *
   * @param operationName the name of the operation
   * @return the metrics for the operation, or null if no metrics exist
   */
  public OperationMetrics getOperationMetrics(final String operationName) {
    final MetricData metricData = metrics.get(operationName);
    return metricData != null ? metricData.getMetrics() : null;
  }

  /**
   * Gets overall metrics summary.
   *
   * @return the overall metrics summary
   */
  public OverallMetrics getOverallMetrics() {
    final Duration totalDuration = Duration.between(startTime, Instant.now());
    final long total = totalOperations.get();
    final long successful = successfulOperations.get();
    final long failed = failedOperations.get();

    final double successRate = total > 0 ? (double) successful / total : 0.0;

    return new OverallMetrics(
        total, successful, failed, successRate, totalDuration, new ArrayList<>(metrics.keySet()));
  }

  /**
   * Gets metrics for all operations.
   *
   * @return a list of all operation metrics
   */
  public List<OperationMetrics> getAllOperationMetrics() {
    final List<OperationMetrics> allMetrics = new ArrayList<>();
    for (final MetricData metricData : metrics.values()) {
      allMetrics.add(metricData.getMetrics());
    }
    return allMetrics;
  }

  /** Clears all collected metrics. */
  public void clear() {
    metrics.clear();
    totalOperations.set(0);
    successfulOperations.set(0);
    failedOperations.set(0);
    LOGGER.info("Metrics cleared");
  }

  /**
   * Gets the number of unique operations tracked.
   *
   * @return the number of unique operations
   */
  public int getOperationCount() {
    return metrics.size();
  }

  private void validateOperationName(final String operationName) {
    if (operationName == null || operationName.isBlank()) {
      throw new IllegalArgumentException("Operation name cannot be null or empty");
    }
  }

  private void validateExecutionTime(final Duration executionTime) {
    if (executionTime == null || executionTime.isNegative()) {
      throw new IllegalArgumentException("Execution time cannot be null or negative");
    }
  }

  /** Functional interface for timed operations. */
  @FunctionalInterface
  public interface TimedOperation<T> {
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    T execute() throws Exception;
  }

  /** Metrics data for a specific operation. */
  private static final class MetricData {
    private final String operationName;
    private final List<Duration> successTimes = Collections.synchronizedList(new ArrayList<>());
    private final List<Duration> failureTimes = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    /**
     * Creates metric data for the specified operation.
     *
     * @param operationName the name of the operation
     */
    MetricData(final String operationName) {
      this.operationName = operationName;
    }

    void recordSuccess(final Duration executionTime) {
      successTimes.add(executionTime);
      successCount.incrementAndGet();
    }

    void recordFailure(final Duration executionTime) {
      failureTimes.add(executionTime);
      failureCount.incrementAndGet();
    }

    /**
     * Gets the operation metrics for this metric data.
     *
     * @return the operation metrics
     */
    OperationMetrics getMetrics() {
      final List<Duration> allSuccessTimes = new ArrayList<>(successTimes);

      final long totalCount = successCount.get() + failureCount.get();
      final double successRate = totalCount > 0 ? (double) successCount.get() / totalCount : 0.0;

      // Calculate success time statistics
      final Duration minSuccessTime =
          allSuccessTimes.stream().min(Duration::compareTo).orElse(Duration.ZERO);

      final Duration maxSuccessTime =
          allSuccessTimes.stream().max(Duration::compareTo).orElse(Duration.ZERO);

      final Duration avgSuccessTime =
          allSuccessTimes.isEmpty()
              ? Duration.ZERO
              : Duration.ofNanos(
                  allSuccessTimes.stream().mapToLong(Duration::toNanos).sum()
                      / allSuccessTimes.size());

      return new OperationMetrics(
          operationName,
          successCount.get(),
          failureCount.get(),
          totalCount,
          successRate,
          minSuccessTime,
          maxSuccessTime,
          avgSuccessTime);
    }
  }

  /** Metrics for a specific operation. */
  public static final class OperationMetrics {
    private final String operationName;
    private final long successCount;
    private final long failureCount;
    private final long totalCount;
    private final double successRate;
    private final Duration minTime;
    private final Duration maxTime;
    private final Duration avgTime;

    /**
     * Creates operation metrics with the specified values.
     *
     * @param operationName the operation name
     * @param successCount the success count
     * @param failureCount the failure count
     * @param totalCount the total count
     * @param successRate the success rate
     * @param minTime the minimum execution time
     * @param maxTime the maximum execution time
     * @param avgTime the average execution time
     */
    public OperationMetrics(
        final String operationName,
        final long successCount,
        final long failureCount,
        final long totalCount,
        final double successRate,
        final Duration minTime,
        final Duration maxTime,
        final Duration avgTime) {
      this.operationName = operationName;
      this.successCount = successCount;
      this.failureCount = failureCount;
      this.totalCount = totalCount;
      this.successRate = successRate;
      this.minTime = minTime;
      this.maxTime = maxTime;
      this.avgTime = avgTime;
    }

    public String getOperationName() {
      return operationName;
    }

    public long getSuccessCount() {
      return successCount;
    }

    public long getFailureCount() {
      return failureCount;
    }

    public long getTotalCount() {
      return totalCount;
    }

    public double getSuccessRate() {
      return successRate;
    }

    public Duration getMinTime() {
      return minTime;
    }

    public Duration getMaxTime() {
      return maxTime;
    }

    public Duration getAvgTime() {
      return avgTime;
    }

    @Override
    public String toString() {
      return String.format(
          "OperationMetrics{name='%s', total=%d, success=%d, failure=%d, successRate=%.2f%%, "
              + "avgTime=%dms, minTime=%dms, maxTime=%dms}",
          operationName,
          totalCount,
          successCount,
          failureCount,
          successRate * 100,
          avgTime.toMillis(),
          minTime.toMillis(),
          maxTime.toMillis());
    }
  }

  /** Overall metrics summary. */
  public static final class OverallMetrics {
    private final long totalOperations;
    private final long successfulOperations;
    private final long failedOperations;
    private final double overallSuccessRate;
    private final Duration totalDuration;
    private final List<String> operationNames;

    /**
     * Creates overall metrics with the specified values.
     *
     * @param totalOperations the total operation count
     * @param successfulOperations the successful operation count
     * @param failedOperations the failed operation count
     * @param overallSuccessRate the overall success rate
     * @param totalDuration the total duration
     * @param operationNames the list of operation names
     */
    public OverallMetrics(
        final long totalOperations,
        final long successfulOperations,
        final long failedOperations,
        final double overallSuccessRate,
        final Duration totalDuration,
        final List<String> operationNames) {
      this.totalOperations = totalOperations;
      this.successfulOperations = successfulOperations;
      this.failedOperations = failedOperations;
      this.overallSuccessRate = overallSuccessRate;
      this.totalDuration = totalDuration;
      this.operationNames = List.copyOf(operationNames);
    }

    public long getTotalOperations() {
      return totalOperations;
    }

    public long getSuccessfulOperations() {
      return successfulOperations;
    }

    public long getFailedOperations() {
      return failedOperations;
    }

    public double getOverallSuccessRate() {
      return overallSuccessRate;
    }

    public Duration getTotalDuration() {
      return totalDuration;
    }

    public List<String> getOperationNames() {
      return operationNames;
    }

    @Override
    public String toString() {
      return String.format(
          "OverallMetrics{total=%d, success=%d, failed=%d, successRate=%.2f%%, "
              + "duration=%dms, operations=%d}",
          totalOperations,
          successfulOperations,
          failedOperations,
          overallSuccessRate * 100,
          totalDuration.toMillis(),
          operationNames.size());
    }
  }
}
