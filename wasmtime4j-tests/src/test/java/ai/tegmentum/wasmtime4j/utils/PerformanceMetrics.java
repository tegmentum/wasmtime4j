package ai.tegmentum.wasmtime4j.utils;

import java.time.Duration;

/**
 * Utility class for capturing and reporting performance metrics from duration measurements.
 *
 * <p>This class calculates and stores statistical measures including minimum, maximum, average,
 * median, 95th percentile, and 99th percentile durations from a collection of measurements.
 */
public final class PerformanceMetrics {
  private final Duration min;
  private final Duration max;
  private final Duration average;
  private final Duration median;
  private final Duration p95;
  private final Duration p99;

  /**
   * Creates a new PerformanceMetrics instance with the specified statistical measures.
   *
   * @param min the minimum duration
   * @param max the maximum duration
   * @param average the average duration
   * @param median the median duration
   * @param p95 the 95th percentile duration
   * @param p99 the 99th percentile duration
   */
  public PerformanceMetrics(
      final Duration min,
      final Duration max,
      final Duration average,
      final Duration median,
      final Duration p95,
      final Duration p99) {
    this.min = min;
    this.max = max;
    this.average = average;
    this.median = median;
    this.p95 = p95;
    this.p99 = p99;
  }

  /**
   * Gets the minimum duration.
   *
   * @return the minimum duration
   */
  public Duration getMin() {
    return min;
  }

  /**
   * Gets the maximum duration.
   *
   * @return the maximum duration
   */
  public Duration getMax() {
    return max;
  }

  /**
   * Gets the average duration.
   *
   * @return the average duration
   */
  public Duration getAverage() {
    return average;
  }

  /**
   * Gets the median duration.
   *
   * @return the median duration
   */
  public Duration getMedian() {
    return median;
  }

  /**
   * Gets the 95th percentile duration.
   *
   * @return the 95th percentile duration
   */
  public Duration getP95() {
    return p95;
  }

  /**
   * Gets the 99th percentile duration.
   *
   * @return the 99th percentile duration
   */
  public Duration getP99() {
    return p99;
  }

  @Override
  public String toString() {
    return String.format(
        "PerformanceMetrics{min=%dms, max=%dms, avg=%dms, median=%dms, p95=%dms, p99=%dms}",
        min.toMillis(),
        max.toMillis(),
        average.toMillis(),
        median.toMillis(),
        p95.toMillis(),
        p99.toMillis());
  }
}
