package ai.tegmentum.wasmtime4j.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for performance testing operations.
 *
 * <p>This class provides static methods for measuring execution time and calculating performance
 * metrics from duration measurements.
 */
public final class PerformanceTestUtils {

  private static final Logger LOGGER = Logger.getLogger(PerformanceTestUtils.class.getName());

  private PerformanceTestUtils() {
    // Utility class
  }

  /**
   * Times the execution of a runnable operation and returns the duration.
   *
   * @param description a description of the operation being timed
   * @param operation the operation to measure
   * @return the duration of the operation
   */
  public static Duration measureExecutionTimeAndReturnDuration(
      final String description, final Runnable operation) {
    final Instant start = Instant.now();
    try {
      operation.run();
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.fine(String.format("%s completed in %d ms", description, duration.toMillis()));
      return duration;
    } catch (final Exception e) {
      final Duration duration = Duration.between(start, Instant.now());
      LOGGER.warning(
          String.format(
              "%s failed after %d ms: %s", description, duration.toMillis(), e.getMessage()));
      throw new RuntimeException(e);
    }
  }

  /**
   * Calculates performance metrics from a list of duration measurements.
   *
   * @param durations the list of duration measurements
   * @return performance metrics including min, max, average, median, p95, and p99
   */
  public static PerformanceMetrics calculateMetrics(final List<Duration> durations) {
    if (durations == null || durations.isEmpty()) {
      throw new IllegalArgumentException("Duration list cannot be null or empty");
    }

    durations.sort(Duration::compareTo);

    final Duration min = durations.get(0);
    final Duration max = durations.get(durations.size() - 1);
    final Duration average =
        Duration.ofNanos(
            (long) durations.stream().mapToLong(Duration::toNanos).average().orElse(0.0));
    final Duration median = durations.get(durations.size() / 2);
    final Duration p95 = durations.get((int) (durations.size() * 0.95));
    final Duration p99 = durations.get((int) (durations.size() * 0.99));

    return new PerformanceMetrics(min, max, average, median, p95, p99);
  }

  /**
   * Logs performance metrics in a formatted way.
   *
   * @param operation description of the operation
   * @param metrics the performance metrics to log
   * @param runtimeType the runtime type being tested
   */
  public static void logPerformanceMetrics(
      final String operation, final PerformanceMetrics metrics, final Object runtimeType) {
    LOGGER.info(
        String.format(
            "%s performance on %s: min=%.1fms, avg=%.1fms, max=%.1fms, p95=%.1fms, p99=%.1fms",
            operation,
            runtimeType,
            metrics.getMin().toMillis(),
            metrics.getAverage().toMillis(),
            metrics.getMax().toMillis(),
            metrics.getP95().toMillis(),
            metrics.getP99().toMillis()));
  }
}
