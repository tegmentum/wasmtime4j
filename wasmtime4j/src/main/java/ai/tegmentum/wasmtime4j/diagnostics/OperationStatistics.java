package ai.tegmentum.wasmtime4j.diagnostics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Statistical information about WebAssembly operations of a specific type.
 *
 * <p>This class tracks performance statistics for operations such as compilation, instantiation,
 * and execution. All operations are thread-safe and designed for high-concurrency access.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * OperationStatistics stats = performanceDiagnostics.getOperationStatistics("Compilation");
 * System.out.println("Average duration: " + stats.getAverageDuration() + "ms");
 * System.out.println("Total operations: " + stats.getOperationCount());
 * }</pre>
 *
 * @since 1.0.0
 */
public final class OperationStatistics {

  // Operation count and timing
  private final LongAdder operationCount = new LongAdder();
  private final LongAdder totalDuration = new LongAdder();
  private final AtomicLong minDuration = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong maxDuration = new AtomicLong(0);

  // Memory usage tracking
  private final LongAdder totalMemoryDelta = new LongAdder();
  private final AtomicLong maxMemoryDelta = new AtomicLong(Long.MIN_VALUE);
  private final AtomicLong minMemoryDelta = new AtomicLong(Long.MAX_VALUE);

  // CPU time tracking (when available)
  private final LongAdder totalCpuTime = new LongAdder();
  private final LongAdder cpuTimeOperations = new LongAdder();

  /**
   * Records the completion of an operation with performance metrics.
   *
   * @param duration the operation duration in milliseconds
   * @param memoryDelta the change in memory usage in bytes (positive = increase)
   * @param cpuTime the CPU time used in milliseconds, or -1 if not available
   */
  public void recordOperation(final long duration, final long memoryDelta, final long cpuTime) {
    operationCount.increment();
    totalDuration.add(duration);

    // Update duration bounds
    minDuration.updateAndGet(current -> Math.min(current, duration));
    maxDuration.updateAndGet(current -> Math.max(current, duration));

    // Track memory usage
    totalMemoryDelta.add(memoryDelta);
    minMemoryDelta.updateAndGet(current -> Math.min(current, memoryDelta));
    maxMemoryDelta.updateAndGet(current -> Math.max(current, memoryDelta));

    // Track CPU time if available
    if (cpuTime >= 0) {
      totalCpuTime.add(cpuTime);
      cpuTimeOperations.increment();
    }
  }

  /**
   * Gets the total number of operations recorded.
   *
   * @return the operation count
   */
  public long getOperationCount() {
    return operationCount.sum();
  }

  /**
   * Gets the average operation duration in milliseconds.
   *
   * @return the average duration, or 0 if no operations recorded
   */
  public double getAverageDuration() {
    final long count = operationCount.sum();
    return (count == 0) ? 0.0 : (double) totalDuration.sum() / count;
  }

  /**
   * Gets the minimum operation duration in milliseconds.
   *
   * @return the minimum duration, or 0 if no operations recorded
   */
  public long getMinDuration() {
    final long min = minDuration.get();
    return (min == Long.MAX_VALUE) ? 0 : min;
  }

  /**
   * Gets the maximum operation duration in milliseconds.
   *
   * @return the maximum duration
   */
  public long getMaxDuration() {
    return maxDuration.get();
  }

  /**
   * Gets the total duration of all operations in milliseconds.
   *
   * @return the total duration
   */
  public long getTotalDuration() {
    return totalDuration.sum();
  }

  /**
   * Gets the average memory delta per operation in bytes.
   *
   * @return the average memory delta, or 0 if no operations recorded
   */
  public double getAverageMemoryDelta() {
    final long count = operationCount.sum();
    return (count == 0) ? 0.0 : (double) totalMemoryDelta.sum() / count;
  }

  /**
   * Gets the minimum memory delta in bytes.
   *
   * @return the minimum memory delta, or 0 if no operations recorded
   */
  public long getMinMemoryDelta() {
    final long min = minMemoryDelta.get();
    return (min == Long.MAX_VALUE) ? 0 : min;
  }

  /**
   * Gets the maximum memory delta in bytes.
   *
   * @return the maximum memory delta
   */
  public long getMaxMemoryDelta() {
    final long max = maxMemoryDelta.get();
    return (max == Long.MIN_VALUE) ? 0 : max;
  }

  /**
   * Gets the total memory delta for all operations in bytes.
   *
   * @return the total memory delta
   */
  public long getTotalMemoryDelta() {
    return totalMemoryDelta.sum();
  }

  /**
   * Gets the average CPU time per operation in milliseconds.
   *
   * @return the average CPU time, or 0 if no CPU time data available
   */
  public double getAverageCpuTime() {
    final long count = cpuTimeOperations.sum();
    return (count == 0) ? 0.0 : (double) totalCpuTime.sum() / count;
  }

  /**
   * Gets the total CPU time for all operations in milliseconds.
   *
   * @return the total CPU time
   */
  public long getTotalCpuTime() {
    return totalCpuTime.sum();
  }

  /**
   * Gets the number of operations with CPU time data.
   *
   * @return the count of operations with CPU time data
   */
  public long getCpuTimeOperationCount() {
    return cpuTimeOperations.sum();
  }

  /**
   * Checks if any operations have been recorded.
   *
   * @return true if operations have been recorded
   */
  public boolean hasOperations() {
    return operationCount.sum() > 0;
  }

  /**
   * Checks if CPU time data is available for any operations.
   *
   * @return true if CPU time data is available
   */
  public boolean hasCpuTimeData() {
    return cpuTimeOperations.sum() > 0;
  }

  /**
   * Gets the operations per second rate based on total duration.
   *
   * @return the operations per second rate, or 0 if total duration is 0
   */
  public double getOperationsPerSecond() {
    final long totalDurationMs = totalDuration.sum();
    final long count = operationCount.sum();

    if (totalDurationMs == 0 || count == 0) {
      return 0.0;
    }

    return (double) count / (totalDurationMs / 1000.0);
  }

  /**
   * Calculates the 95th percentile duration estimate.
   *
   * <p>This is a simple estimate based on the assumption that durations follow a normal
   * distribution. For more accurate percentile calculations, consider using a histogram-based
   * approach.
   *
   * @return the estimated 95th percentile duration in milliseconds
   */
  public double getEstimated95thPercentileDuration() {
    final long count = operationCount.sum();
    if (count == 0) {
      return 0.0;
    }

    final double avg = getAverageDuration();
    final long min = getMinDuration();
    final long max = getMaxDuration();

    // Simple estimation: assume normal distribution and use avg + 1.645 * estimated_std_dev
    // Estimate standard deviation as (max - min) / 4 (rough approximation)
    final double estimatedStdDev = (max - min) / 4.0;
    return avg + (1.645 * estimatedStdDev);
  }

  /** Resets all statistics to their initial state. */
  public void reset() {
    operationCount.reset();
    totalDuration.reset();
    minDuration.set(Long.MAX_VALUE);
    maxDuration.set(0);

    totalMemoryDelta.reset();
    minMemoryDelta.set(Long.MAX_VALUE);
    maxMemoryDelta.set(Long.MIN_VALUE);

    totalCpuTime.reset();
    cpuTimeOperations.reset();
  }

  /**
   * Returns a formatted string representation of the statistics.
   *
   * @return a formatted string with key statistics
   */
  @Override
  public String toString() {
    if (!hasOperations()) {
      return "OperationStatistics{no operations recorded}";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("OperationStatistics{");
    sb.append("operations=").append(getOperationCount());
    sb.append(", avgDuration=").append(String.format("%.1f", getAverageDuration())).append("ms");
    sb.append(", minDuration=").append(getMinDuration()).append("ms");
    sb.append(", maxDuration=").append(getMaxDuration()).append("ms");
    sb.append(", avgMemory=")
        .append(String.format("%.1f", getAverageMemoryDelta() / 1024.0))
        .append("KB");

    if (hasCpuTimeData()) {
      sb.append(", avgCpuTime=").append(String.format("%.1f", getAverageCpuTime())).append("ms");
    }

    sb.append(", ops/sec=").append(String.format("%.1f", getOperationsPerSecond()));
    sb.append("}");

    return sb.toString();
  }
}
