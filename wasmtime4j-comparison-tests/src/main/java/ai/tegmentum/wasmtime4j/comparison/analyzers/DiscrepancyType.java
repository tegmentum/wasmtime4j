package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Types of behavioral discrepancies that can be detected during test execution result comparison.
 *
 * @since 1.0.0
 */
public enum DiscrepancyType {
  /** Execution status differs between runtimes (success vs failure). */
  EXECUTION_STATUS_MISMATCH("Execution status inconsistency"),

  /** Return values differ between successful executions. */
  RETURN_VALUE_MISMATCH("Return value inconsistency"),

  /** Exception types differ between failed executions. */
  EXCEPTION_TYPE_MISMATCH("Exception type inconsistency"),

  /** Exception messages differ between failed executions. */
  EXCEPTION_MESSAGE_MISMATCH("Exception message inconsistency"),

  /** Significant performance differences between runtimes. */
  PERFORMANCE_DEVIATION("Performance deviation"),

  /** Memory usage differs significantly between runtimes. */
  MEMORY_USAGE_DEVIATION("Memory usage deviation"),

  /** Some runtimes skip test while others execute it. */
  SKIP_INCONSISTENCY("Skip behavior inconsistency"),

  /** Systematic pattern of failures detected. */
  SYSTEMATIC_PATTERN("Systematic failure pattern"),

  /** Other unclassified discrepancy. */
  OTHER("Other discrepancy");

  private final String description;

  DiscrepancyType(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
