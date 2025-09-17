package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Severity levels for identified issues.
 *
 * @since 1.0.0
 */
public enum IssueSeverity {
  /** Low severity issue that can be addressed when convenient. */
  LOW,

  /** Medium severity issue that should be addressed in near future. */
  MEDIUM,

  /** High severity issue requiring immediate attention. */
  HIGH
}
