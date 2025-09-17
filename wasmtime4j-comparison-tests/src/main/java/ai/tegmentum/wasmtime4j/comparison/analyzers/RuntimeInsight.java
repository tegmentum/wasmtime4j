package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.List;
import java.util.Objects;

/**
 * Runtime-specific insight with targeted recommendations.
 *
 * @since 1.0.0
 */
public final class RuntimeInsight {
  private final RuntimeType runtime;
  private final RuntimeInsightType type;
  private final List<String> observations;
  private final List<String> recommendations;
  private final InsightSeverity severity;
  private final double confidenceScore;

  /**
   * Creates a new runtime insight.
   *
   * @param runtime the runtime type
   * @param type the type of runtime insight
   * @param observations the list of observations
   * @param recommendations the list of recommendations
   * @param severity the severity level
   * @param confidenceScore the confidence score (0.0 to 1.0)
   */
  public RuntimeInsight(
      final RuntimeType runtime,
      final RuntimeInsightType type,
      final List<String> observations,
      final List<String> recommendations,
      final InsightSeverity severity,
      final double confidenceScore) {
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
    this.observations = List.copyOf(observations);
    this.recommendations = List.copyOf(recommendations);
    this.severity = Objects.requireNonNull(severity, "severity cannot be null");
    this.confidenceScore = confidenceScore;
  }

  /**
   * Gets the runtime type.
   *
   * @return the runtime
   */
  public RuntimeType getRuntime() {
    return runtime;
  }

  /**
   * Gets the insight type.
   *
   * @return the insight type
   */
  public RuntimeInsightType getType() {
    return type;
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
    final RuntimeInsight that = (RuntimeInsight) obj;
    return Double.compare(that.confidenceScore, confidenceScore) == 0
        && runtime == that.runtime
        && type == that.type
        && Objects.equals(observations, that.observations)
        && Objects.equals(recommendations, that.recommendations)
        && severity == that.severity;
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtime, type, observations, recommendations, severity, confidenceScore);
  }

  @Override
  public String toString() {
    return "RuntimeInsight{"
        + "runtime="
        + runtime
        + ", type="
        + type
        + ", severity="
        + severity
        + '}';
  }
}
