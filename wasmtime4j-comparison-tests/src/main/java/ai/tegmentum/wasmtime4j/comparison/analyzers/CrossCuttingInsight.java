package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Cross-cutting insight spanning multiple areas.
 *
 * @since 1.0.0
 */
public final class CrossCuttingInsight {
  private final CrossCuttingInsightType type;
  private final String description;
  private final List<String> observations;
  private final List<String> recommendations;
  private final InsightSeverity severity;
  private final double confidenceScore;

  /**
   * Creates a new cross-cutting insight.
   *
   * @param type the type of cross-cutting insight
   * @param description the description of the insight
   * @param observations the list of observations
   * @param recommendations the list of recommendations
   * @param severity the severity level
   * @param confidenceScore the confidence score (0.0 to 1.0)
   */
  public CrossCuttingInsight(
      final CrossCuttingInsightType type,
      final String description,
      final List<String> observations,
      final List<String> recommendations,
      final InsightSeverity severity,
      final double confidenceScore) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.observations = List.copyOf(observations);
    this.recommendations = List.copyOf(recommendations);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.confidenceScore = confidenceScore;
  }

  /**
   * Gets the insight type.
   *
   * @return the insight type
   */
  public CrossCuttingInsightType getType() {
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
   * Gets the severity level.
   *
   * @return the severity
   */
  public InsightSeverity getSeverity() {
    return severity;
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
    final CrossCuttingInsight that = (CrossCuttingInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0
        && type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(observations, that.observations)
        && Objects.equals(recommendations, that.recommendations)
        && severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        type, description, observations, recommendations, severity, confidenceScore);
  }

  @Override
  public String toString() {
    return "CrossCuttingInsight{" + "type=" + type + ", severity=" + severity + '}';
  }
}
