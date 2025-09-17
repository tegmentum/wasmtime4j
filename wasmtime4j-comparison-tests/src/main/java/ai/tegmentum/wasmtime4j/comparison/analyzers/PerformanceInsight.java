package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.List;
import java.util.Objects;

/**
 * Performance optimization insight with specific recommendations.
 *
 * @since 1.0.0
 */
public final class PerformanceInsight {
  private final PerformanceInsightType type;
  private final String description;
  private final String runtime;
  private final List<String> recommendations;
  private final InsightSeverity severity;
  private final double confidenceScore;

  /**
   * Creates a new performance insight.
   *
   * @param type the type of performance insight
   * @param description the description of the insight
   * @param runtime the runtime this insight applies to
   * @param recommendations the list of recommendations
   * @param severity the severity level
   * @param confidenceScore the confidence score (0.0 to 1.0)
   */
  public PerformanceInsight(
      final PerformanceInsightType type,
      final String description,
      final String runtime,
      final List<String> recommendations,
      final InsightSeverity severity,
      final double confidenceScore) {
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.recommendations = List.copyOf(recommendations);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.confidenceScore = confidenceScore;
  }

  /**
   * Gets the performance insight type.
   *
   * @return the insight type
   */
  public PerformanceInsightType getType() {
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
   * Gets the runtime this insight applies to.
   *
   * @return the runtime name
   */
  public String getRuntime() {
    return runtime;
  }

  /**
   * Gets the recommendations for this insight.
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
    final PerformanceInsight that = (PerformanceInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0
        && type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(runtime, that.runtime)
        && Objects.equals(recommendations, that.recommendations)
        && severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, description, runtime, recommendations, severity, confidenceScore);
  }

  @Override
  public String toString() {
    return "PerformanceInsight{"
        + "type="
        + type
        + ", runtime='"
        + runtime
        + '\''
        + ", severity="
        + severity
        + '}';
  }
}
