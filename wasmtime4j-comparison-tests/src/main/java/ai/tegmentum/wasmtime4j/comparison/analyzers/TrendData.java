package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Trend data for time-series analysis.
 *
 * @since 1.0.0
 */
public final class TrendData {
  private final TrendDirection trendDirection;
  private final double trendStrength;
  private final String description;
  private final List<String> recommendations;

  /**
   * Constructs a new TrendData with the specified properties.
   *
   * @param trendDirection the direction of the trend
   * @param trendStrength the strength of the trend
   * @param description the trend description
   * @param recommendations the list of recommendations
   */
  public TrendData(
      final TrendDirection trendDirection,
      final double trendStrength,
      final String description,
      final List<String> recommendations) {
    this.trendDirection = Objects.requireNonNull(trendDirection, "trendDirection cannot be null");
    this.trendStrength = trendStrength;
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.recommendations = List.copyOf(recommendations);
  }

  public TrendDirection getTrendDirection() {
    return trendDirection;
  }

  public double getTrendStrength() {
    return trendStrength;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Checks if this trend is significant enough to warrant attention.
   *
   * @return true if trend is significant
   */
  public boolean hasSignificantTrend() {
    return trendStrength > 5.0 && trendDirection != TrendDirection.STABLE;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TrendData that = (TrendData) obj;
    return Double.compare(that.trendStrength, trendStrength) == 0
        && trendDirection == that.trendDirection
        && Objects.equals(description, that.description)
        && Objects.equals(recommendations, that.recommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(trendDirection, trendStrength, description, recommendations);
  }

  @Override
  public String toString() {
    return "TrendData{"
        + "direction="
        + trendDirection
        + ", strength="
        + String.format("%.2f", trendStrength)
        + '}';
  }
}
