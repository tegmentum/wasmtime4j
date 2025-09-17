package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Severity levels for coverage gaps.
 *
 * @since 1.0.0
 */
public enum GapSeverity {
  /** Low severity gap that should be addressed eventually. */
  LOW,

  /** Medium severity gap that should be addressed soon. */
  MEDIUM,

  /** High severity gap that requires immediate attention. */
  HIGH
}
