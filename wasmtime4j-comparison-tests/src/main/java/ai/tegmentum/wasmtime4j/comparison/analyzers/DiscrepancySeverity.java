package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Severity levels for behavioral discrepancies detected during test execution result comparison.
 *
 * @since 1.0.0
 */
public enum DiscrepancySeverity {
  /** Critical discrepancy requiring immediate attention. */
  CRITICAL("Critical", 4),

  /** Major discrepancy that should be addressed. */
  MAJOR("Major", 3),

  /** Moderate discrepancy worth investigating. */
  MODERATE("Moderate", 2),

  /** Minor discrepancy that may be acceptable. */
  MINOR("Minor", 1);

  private final String displayName;
  private final int priority;

  DiscrepancySeverity(final String displayName, final int priority) {
    this.displayName = displayName;
    this.priority = priority;
  }

  public String getDisplayName() {
    return displayName;
  }

  public int getPriority() {
    return priority;
  }

  /**
   * Checks if this severity is higher than another.
   *
   * @param other the other severity
   * @return true if this severity is higher
   */
  public boolean isHigherThan(final DiscrepancySeverity other) {
    return this.priority > other.priority;
  }
}