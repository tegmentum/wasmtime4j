package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;

/**
 * Comprehensive execution statistics for WebAssembly functions.
 *
 * <p>This immutable class contains detailed performance metrics collected during
 * function execution, used for tier transition analysis and performance monitoring.
 *
 * @since 1.0.0
 */
public final class ExecutionStatistics {

  private final String functionId;
  private final long executionCount;
  private final Duration totalExecutionTime;
  private final Duration averageExecutionTime;
  private final Duration minExecutionTime;
  private final Duration maxExecutionTime;
  private final long totalMemoryUsage;
  private final Instant firstExecution;
  private final Instant lastExecution;
  private final long transitionFailures;

  // Recent performance window data
  private final long recentExecutionCount;
  private final Duration recentTotalExecutionTime;
  private final Duration recentAverageExecutionTime;
  private final Instant performanceWindowStart;

  /**
   * Creates new execution statistics.
   *
   * @param functionId function identifier
   * @param executionCount total execution count
   * @param totalExecutionTime total execution time
   * @param averageExecutionTime average execution time
   * @param minExecutionTime minimum execution time
   * @param maxExecutionTime maximum execution time
   * @param totalMemoryUsage total memory usage
   * @param firstExecution first execution timestamp
   * @param lastExecution last execution timestamp
   * @param transitionFailures number of failed tier transitions
   * @param recentExecutionCount recent execution count
   * @param recentTotalExecutionTime recent total execution time
   * @param recentAverageExecutionTime recent average execution time
   * @param performanceWindowStart start of recent performance window
   */
  public ExecutionStatistics(final String functionId,
                             final long executionCount,
                             final Duration totalExecutionTime,
                             final Duration averageExecutionTime,
                             final Duration minExecutionTime,
                             final Duration maxExecutionTime,
                             final long totalMemoryUsage,
                             final Instant firstExecution,
                             final Instant lastExecution,
                             final long transitionFailures,
                             final long recentExecutionCount,
                             final Duration recentTotalExecutionTime,
                             final Duration recentAverageExecutionTime,
                             final Instant performanceWindowStart) {
    this.functionId = functionId;
    this.executionCount = executionCount;
    this.totalExecutionTime = totalExecutionTime;
    this.averageExecutionTime = averageExecutionTime;
    this.minExecutionTime = minExecutionTime;
    this.maxExecutionTime = maxExecutionTime;
    this.totalMemoryUsage = totalMemoryUsage;
    this.firstExecution = firstExecution;
    this.lastExecution = lastExecution;
    this.transitionFailures = transitionFailures;
    this.recentExecutionCount = recentExecutionCount;
    this.recentTotalExecutionTime = recentTotalExecutionTime;
    this.recentAverageExecutionTime = recentAverageExecutionTime;
    this.performanceWindowStart = performanceWindowStart;
  }

  public String getFunctionId() {
    return functionId;
  }

  public long getExecutionCount() {
    return executionCount;
  }

  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  public Duration getAverageExecutionTime() {
    return averageExecutionTime;
  }

  public Duration getMinExecutionTime() {
    return minExecutionTime;
  }

  public Duration getMaxExecutionTime() {
    return maxExecutionTime;
  }

  public long getTotalMemoryUsage() {
    return totalMemoryUsage;
  }

  public Instant getFirstExecution() {
    return firstExecution;
  }

  public Instant getLastExecution() {
    return lastExecution;
  }

  public long getTransitionFailures() {
    return transitionFailures;
  }

  public long getRecentExecutionCount() {
    return recentExecutionCount;
  }

  public Duration getRecentTotalExecutionTime() {
    return recentTotalExecutionTime;
  }

  public Duration getRecentAverageExecutionTime() {
    return recentAverageExecutionTime;
  }

  public Instant getPerformanceWindowStart() {
    return performanceWindowStart;
  }

  /**
   * Gets the total duration this function has been tracked.
   *
   * @return tracking duration or Duration.ZERO if never executed
   */
  public Duration getTrackingDuration() {
    if (firstExecution == null || lastExecution == null) {
      return Duration.ZERO;
    }
    return Duration.between(firstExecution, lastExecution);
  }

  /**
   * Gets the execution rate (calls per second) over total tracking period.
   *
   * @return execution rate
   */
  public double getOverallExecutionRate() {
    final Duration trackingDuration = getTrackingDuration();
    if (trackingDuration.toMillis() <= 0) {
      return 0.0;
    }
    return (double) executionCount / trackingDuration.toSeconds();
  }

  /**
   * Gets the recent execution rate (calls per second) over performance window.
   *
   * @return recent execution rate
   */
  public double getRecentExecutionRate() {
    if (performanceWindowStart == null) {
      return 0.0;
    }

    final Duration windowDuration = Duration.between(performanceWindowStart, Instant.now());
    if (windowDuration.toMillis() <= 0) {
      return 0.0;
    }

    return (double) recentExecutionCount / windowDuration.toSeconds();
  }

  /**
   * Gets the average memory usage per execution.
   *
   * @return average memory usage
   */
  public long getAverageMemoryUsage() {
    return executionCount > 0 ? totalMemoryUsage / executionCount : 0;
  }

  /**
   * Calculates the coefficient of variation for execution times.
   * Higher values indicate more variable execution times.
   *
   * @return coefficient of variation (0.0 to 1.0+)
   */
  public double getExecutionTimeVariability() {
    if (executionCount <= 1) {
      return 0.0;
    }

    final double avgMs = averageExecutionTime.toNanos() / 1_000_000.0;
    final double minMs = minExecutionTime.toNanos() / 1_000_000.0;
    final double maxMs = maxExecutionTime.toNanos() / 1_000_000.0;

    if (avgMs <= 0.0) {
      return 0.0;
    }

    // Approximate standard deviation using min/max range
    final double range = maxMs - minMs;
    final double approximateStdDev = range / 4.0; // Rough approximation

    return approximateStdDev / avgMs;
  }

  /**
   * Determines if recent performance has improved compared to overall average.
   *
   * @return true if recent performance is better
   */
  public boolean hasRecentPerformanceImproved() {
    if (recentExecutionCount < 10) {
      return false; // Insufficient recent data
    }

    final long recentAvgNs = recentAverageExecutionTime.toNanos();
    final long overallAvgNs = averageExecutionTime.toNanos();

    return recentAvgNs < overallAvgNs * 0.95; // 5% improvement threshold
  }

  /**
   * Determines if recent performance has degraded compared to overall average.
   *
   * @return true if recent performance is worse
   */
  public boolean hasRecentPerformanceDegraded() {
    if (recentExecutionCount < 10) {
      return false; // Insufficient recent data
    }

    final long recentAvgNs = recentAverageExecutionTime.toNanos();
    final long overallAvgNs = averageExecutionTime.toNanos();

    return recentAvgNs > overallAvgNs * 1.15; // 15% degradation threshold
  }

  /**
   * Gets performance trend indicator.
   *
   * @return positive for improving, negative for degrading, zero for stable
   */
  public int getPerformanceTrend() {
    if (hasRecentPerformanceImproved()) {
      return 1;
    } else if (hasRecentPerformanceDegraded()) {
      return -1;
    }
    return 0;
  }

  @Override
  public String toString() {
    return String.format(
        "ExecutionStatistics{functionId='%s', count=%d, avgTime=%.2fms, " +
        "totalTime=%dms, memoryUsage=%d, rate=%.1f/s, trend=%d}",
        functionId,
        executionCount,
        averageExecutionTime.toNanos() / 1_000_000.0,
        totalExecutionTime.toMillis(),
        totalMemoryUsage,
        getOverallExecutionRate(),
        getPerformanceTrend()
    );
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    final ExecutionStatistics that = (ExecutionStatistics) obj;
    return executionCount == that.executionCount &&
           totalMemoryUsage == that.totalMemoryUsage &&
           transitionFailures == that.transitionFailures &&
           recentExecutionCount == that.recentExecutionCount &&
           functionId.equals(that.functionId) &&
           totalExecutionTime.equals(that.totalExecutionTime) &&
           averageExecutionTime.equals(that.averageExecutionTime);
  }

  @Override
  public int hashCode() {
    int result = functionId.hashCode();
    result = 31 * result + Long.hashCode(executionCount);
    result = 31 * result + totalExecutionTime.hashCode();
    result = 31 * result + averageExecutionTime.hashCode();
    result = 31 * result + Long.hashCode(totalMemoryUsage);
    return result;
  }
}