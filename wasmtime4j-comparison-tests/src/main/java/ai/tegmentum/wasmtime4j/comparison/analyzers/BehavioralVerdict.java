package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Verdict enumeration for behavioral analysis results indicating the level of consistency between
 * different WebAssembly runtime implementations.
 *
 * @since 1.0.0
 */
public enum BehavioralVerdict {
  /** Runtimes behave consistently across all comparisons. */
  CONSISTENT("Consistent behavior"),

  /** Runtimes behave mostly consistently with minor variations. */
  MOSTLY_CONSISTENT("Mostly consistent behavior"),

  /** Runtimes show inconsistent behavior that should be investigated. */
  INCONSISTENT("Inconsistent behavior"),

  /** Runtimes are incompatible and show fundamental differences. */
  INCOMPATIBLE("Incompatible behavior"),

  /** Unable to determine behavioral compatibility. */
  UNKNOWN("Unknown behavior");

  private final String description;

  BehavioralVerdict(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
