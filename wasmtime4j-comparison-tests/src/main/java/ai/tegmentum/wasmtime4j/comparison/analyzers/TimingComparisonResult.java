package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Duration;
import java.util.Objects;

/**
 * Result of comparing execution timing between two executions.
 *
 * @since 1.0.0
 */
public final class TimingComparisonResult {
  private final Duration difference;
  private final boolean withinTolerance;
  private final double ratio;

  /**
   * Constructs a new TimingComparisonResult with the specified timing data.
   *
   * @param difference the time difference between executions
   * @param withinTolerance whether the timing is within acceptable tolerance
   * @param ratio the ratio of execution times
   */
  public TimingComparisonResult(
      final Duration difference, final boolean withinTolerance, final double ratio) {
    this.difference = Objects.requireNonNull(difference, "difference cannot be null");
    this.withinTolerance = withinTolerance;
    this.ratio = ratio;
  }

  public Duration getDifference() {
    return difference;
  }

  public boolean isWithinTolerance() {
    return withinTolerance;
  }

  public double getRatio() {
    return ratio;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TimingComparisonResult that = (TimingComparisonResult) obj;
    return withinTolerance == that.withinTolerance
        && Double.compare(that.ratio, ratio) == 0
        && Objects.equals(difference, that.difference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(difference, withinTolerance, ratio);
  }

  @Override
  public String toString() {
    return "TimingComparisonResult{"
        + "difference="
        + difference.toMillis()
        + "ms"
        + ", withinTolerance="
        + withinTolerance
        + ", ratio="
        + String.format("%.2f", ratio)
        + '}';
  }
}
