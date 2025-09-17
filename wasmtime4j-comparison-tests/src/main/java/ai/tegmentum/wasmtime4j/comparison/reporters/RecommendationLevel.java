package ai.tegmentum.wasmtime4j.comparison.reporters;

/**
 * Recommendation level enum.
 *
 * @since 1.0.0
 */
public enum RecommendationLevel {
  /** Include only high priority recommendations. */
  HIGH,

  /** Include high and medium priority recommendations. */
  MEDIUM,

  /** Include all recommendations. */
  ALL,

  /** Include no recommendations. */
  NONE
}
