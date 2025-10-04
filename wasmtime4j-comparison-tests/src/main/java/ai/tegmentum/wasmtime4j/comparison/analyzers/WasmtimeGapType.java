package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Types of Wasmtime-specific coverage gaps that can be detected during analysis.
 *
 * @since 1.0.0
 */
public enum WasmtimeGapType {
  /** Gap in compatibility between implementations. */
  COMPATIBILITY_GAP,

  /** Missing runtime implementation for a feature. */
  RUNTIME_MISSING,

  /** Entire category without test coverage. */
  CATEGORY_UNTESTED,

  /** Incomplete feature implementation. */
  FEATURE_INCOMPLETE,

  /** Incomplete test suite coverage. */
  TEST_SUITE_INCOMPLETE
}
