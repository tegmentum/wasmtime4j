package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Types of Wasmtime-specific recommendations for coverage and compatibility improvements.
 *
 * @since 1.0.0
 */
public enum WasmtimeRecommendationType {
  /** Recommendation to increase test coverage for a specific category. */
  INCREASE_CATEGORY_COVERAGE,

  /** Recommendation to improve compatibility with Wasmtime behavior. */
  IMPROVE_COMPATIBILITY,

  /** Recommendation to add additional test cases. */
  ADD_TEST_CASES,

  /** Recommendation to fix runtime-specific implementation issues. */
  FIX_RUNTIME_ISSUES,

  /** Recommendation to enhance support for specific features. */
  ENHANCE_FEATURE_SUPPORT
}
