package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a phase in the WebAssembly compilation process with timing and metrics.
 *
 * <p>Compilation phases include parsing, validation, optimization, and code generation. Each phase
 * can have associated metrics providing detailed insights into the compilation process.
 *
 * @since 1.0.0
 */
public final class CompilationPhase {
  private final String name;
  private final Duration duration;
  private final Map<String, Object> metrics;

  /**
   * Creates a compilation phase record.
   *
   * @param name the phase name (e.g., "parsing", "validation", "optimization")
   * @param duration the time spent in this phase
   * @param metrics additional metrics for this phase
   * @throws IllegalArgumentException if any parameter is invalid
   */
  public CompilationPhase(
      final String name, final Duration duration, final Map<String, Object> metrics) {
    this.name = Objects.requireNonNull(name, "name cannot be null");
    this.duration = Objects.requireNonNull(duration, "duration cannot be null");
    this.metrics = Map.copyOf(Objects.requireNonNull(metrics, "metrics cannot be null"));

    if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("name cannot be empty");
    }
    if (duration.isNegative()) {
      throw new IllegalArgumentException("duration cannot be negative: " + duration);
    }
  }

  /**
   * Gets the name of this compilation phase.
   *
   * @return phase name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the duration of this compilation phase.
   *
   * @return phase duration
   */
  public Duration getDuration() {
    return duration;
  }

  /**
   * Gets the metrics associated with this compilation phase.
   *
   * @return map of phase-specific metrics
   */
  public Map<String, Object> getMetrics() {
    return metrics;
  }

  /**
   * Gets the percentage of total compilation time spent in this phase.
   *
   * @param totalCompilationTime the total compilation time
   * @return percentage (0.0 to 100.0)
   */
  public double getPercentageOfTotal(final Duration totalCompilationTime) {
    if (totalCompilationTime.isZero()) {
      return 0.0;
    }
    return (duration.toNanos() * 100.0) / totalCompilationTime.toNanos();
  }

  /**
   * Checks if this phase consumed a significant amount of compilation time.
   *
   * <p>Returns true if this phase took more than 20% of total compilation time.
   *
   * @param totalCompilationTime the total compilation time
   * @return true if this phase was significant
   */
  public boolean isSignificantPhase(final Duration totalCompilationTime) {
    return getPercentageOfTotal(totalCompilationTime) > 20.0;
  }

  /**
   * Gets a specific metric value as a string.
   *
   * @param metricName the metric name
   * @return metric value as string, or empty string if not found
   */
  public String getMetricAsString(final String metricName) {
    final Object value = metrics.get(metricName);
    return value != null ? value.toString() : "";
  }

  /**
   * Gets a specific metric value as a number.
   *
   * @param metricName the metric name
   * @return metric value as double, or 0.0 if not found or not numeric
   */
  public double getMetricAsNumber(final String metricName) {
    final Object value = metrics.get(metricName);
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    return 0.0;
  }

  /**
   * Checks if this phase has a specific metric.
   *
   * @param metricName the metric name
   * @return true if the metric exists
   */
  public boolean hasMetric(final String metricName) {
    return metrics.containsKey(metricName);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CompilationPhase that = (CompilationPhase) obj;
    return Objects.equals(name, that.name)
        && Objects.equals(duration, that.duration)
        && Objects.equals(metrics, that.metrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, duration, metrics);
  }

  @Override
  public String toString() {
    return String.format(
        "CompilationPhase{name='%s', duration=%s, metrics=%s}", name, duration, metrics);
  }
}
