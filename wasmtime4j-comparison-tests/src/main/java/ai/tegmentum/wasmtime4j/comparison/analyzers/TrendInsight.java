package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Trend-based insight showing changes over time.
 *
 * @since 1.0.0
 */
public final class TrendInsight {
  private final String metric;
  private final TrendDirection direction;
  private final double strength;
  private final String description;
  private final List<String> recommendations;

  /**
   * Creates a new trend insight.
   *
   * @param metric the metric being analyzed
   * @param direction the trend direction
   * @param strength the strength of the trend
   * @param description the description of the trend
   * @param recommendations the list of recommendations
   */
  public TrendInsight(
      final String metric,
      final TrendDirection direction,
      final double strength,
      final String description,
      final List<String> recommendations) {
    this.metric = Objects.requireNonNull(metric, "metric cannot be null");
    this.direction = Objects.requireNonNull(direction, "direction cannot be null");
    this.strength = strength;
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.recommendations = List.copyOf(recommendations);
  }

  /**
   * Gets the metric name.
   *
   * @return the metric
   */
  public String getMetric() {
    return metric;
  }

  /**
   * Gets the trend direction.
   *
   * @return the direction
   */
  public TrendDirection getDirection() {
    return direction;
  }

  /**
   * Gets the trend strength.
   *
   * @return the strength
   */
  public double getStrength() {
    return strength;
  }

  /**
   * Gets the trend description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the recommendations.
   *
   * @return the list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final TrendInsight that = (TrendInsight) obj;
    return Double.compare(that.strength, strength) == 0
        && Objects.equals(metric, that.metric)
        && direction == that.direction
        && Objects.equals(description, that.description)
        && Objects.equals(recommendations, that.recommendations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metric, direction, strength, description, recommendations);
  }

  @Override
  public String toString() {
    return "TrendInsight{"
        + "metric='"
        + metric
        + '\''
        + ", direction="
        + direction
        + ", strength="
        + String.format("%.2f", strength)
        + '}';
  }
}
