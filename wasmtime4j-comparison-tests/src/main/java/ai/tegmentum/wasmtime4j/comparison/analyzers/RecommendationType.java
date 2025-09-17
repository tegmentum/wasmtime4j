package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Types of coverage recommendations.
 *
 * @since 1.0.0
 */
public enum RecommendationType {
  /** Increase coverage for a specific category. */
  INCREASE_CATEGORY_COVERAGE,

  /** Improve existing coverage for a category. */
  IMPROVE_CATEGORY_COVERAGE,

  /** Improve coverage for a specific runtime. */
  IMPROVE_RUNTIME_COVERAGE,

  /** Add tests for feature interactions. */
  ADD_INTERACTION_TESTS,

  /** Address high-severity coverage gaps. */
  ADDRESS_CRITICAL_GAPS
}
