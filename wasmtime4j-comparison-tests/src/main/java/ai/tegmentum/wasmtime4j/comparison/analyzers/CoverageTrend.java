package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Coverage trend analysis over time.
 *
 * @since 1.0.0
 */
public final class CoverageTrend {
  private final double currentCoverage;
  private final double previousCoverage;
  private final double change;
  private final TrendDirection direction;

  /**
   * Constructs a new CoverageTrend with the specified coverage data.
   *
   * @param currentCoverage the current coverage value
   * @param previousCoverage the previous coverage value
   * @param change the change in coverage
   * @param direction the trend direction
   */
  public CoverageTrend(
      final double currentCoverage,
      final double previousCoverage,
      final double change,
      final TrendDirection direction) {
    this.currentCoverage = currentCoverage;
    this.previousCoverage = previousCoverage;
    this.change = change;
    this.direction = Objects.requireNonNull(direction, "direction cannot be null");
  }

  public double getCurrentCoverage() {
    return currentCoverage;
  }

  public double getPreviousCoverage() {
    return previousCoverage;
  }

  public double getChange() {
    return change;
  }

  public TrendDirection getDirection() {
    return direction;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final CoverageTrend that = (CoverageTrend) obj;
    return Double.compare(that.currentCoverage, currentCoverage) == 0
        && Double.compare(that.previousCoverage, previousCoverage) == 0
        && Double.compare(that.change, change) == 0
        && direction == that.direction;
  }

  @Override
  public int hashCode() {
    return Objects.hash(currentCoverage, previousCoverage, change, direction);
  }

  @Override
  public String toString() {
    return "CoverageTrend{"
        + "current="
        + String.format("%.1f%%", currentCoverage)
        + ", change="
        + String.format("%.1f%%", change)
        + ", direction="
        + direction
        + '}';
  }
}
