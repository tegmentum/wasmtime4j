package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Strategic insight for high-level decision making.
 *
 * @since 1.0.0
 */
public final class StrategicInsight {
  private final StrategicInsightType type;
  private final String description;
  private final List<String> observations;
  private final List<String> recommendations;
  private final StrategicImportance importance;
  private final double confidenceScore;

  /**
   * Creates a new strategic insight.
   *
   * @param type the type of strategic insight
   * @param description the description of the insight
   * @param observations the list of observations
   * @param recommendations the list of recommendations
   * @param importance the strategic importance level
   * @param confidenceScore the confidence score (0.0 to 1.0)
   */
  public StrategicInsight(
      final StrategicInsightType type,
      final String description,
      final List<String> observations,
      final List<String> recommendations,
      final StrategicImportance importance,
      final double confidenceScore) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.observations = List.copyOf(observations);
    this.recommendations = List.copyOf(recommendations);
    this.importance = Objects.requireNonNull(importance, "importance cannot be null");
    this.confidenceScore = confidenceScore;
  }

  /**
   * Gets the insight type.
   *
   * @return the insight type
   */
  public StrategicInsightType getType() {
    return type;
  }

  /**
   * Gets the insight description.
   *
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the observations.
   *
   * @return the list of observations
   */
  public List<String> getObservations() {
    return observations;
  }

  /**
   * Gets the recommendations.
   *
   * @return the list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets the strategic importance level.
   *
   * @return the importance level
   */
  public StrategicImportance getImportance() {
    return importance;
  }

  /**
   * Gets the confidence score.
   *
   * @return the confidence score
   */
  public double getConfidenceScore() {
    return confidenceScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final StrategicInsight that = (StrategicInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0
        && type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(observations, that.observations)
        && Objects.equals(recommendations, that.recommendations)
        && importance == that.importance;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type, description, observations, recommendations, importance, confidenceScore);
  }

  @Override
  public String toString() {
    return "StrategicInsight{" + "type=" + type + ", importance=" + importance + '}';
  }
}
