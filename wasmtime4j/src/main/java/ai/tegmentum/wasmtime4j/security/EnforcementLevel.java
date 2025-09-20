package ai.tegmentum.wasmtime4j.security;

/**
 * Security policy enforcement levels.
 *
 * @since 1.0.0
 */
public enum EnforcementLevel {

  /** Strict enforcement - all violations result in immediate termination. */
  STRICT("Strict enforcement with immediate termination"),

  /** Permissive enforcement - violations are logged but execution continues. */
  PERMISSIVE("Permissive enforcement with logging only"),

  /** Audit-only mode - no enforcement, only logging for monitoring. */
  AUDIT_ONLY("Audit-only mode with no enforcement"),

  /** Graceful degradation - violations trigger warnings and limited functionality. */
  GRACEFUL("Graceful degradation with warnings");

  private final String description;

  EnforcementLevel(final String description) {
    this.description = description;
  }

  /**
   * Gets the description of this enforcement level.
   *
   * @return enforcement level description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this enforcement level allows continuation after violations.
   *
   * @return true if violations allow continued execution
   */
  public boolean allowsContinuation() {
    return this == PERMISSIVE || this == AUDIT_ONLY || this == GRACEFUL;
  }

  /**
   * Checks if this enforcement level terminates on violations.
   *
   * @return true if violations result in termination
   */
  public boolean terminatesOnViolation() {
    return this == STRICT;
  }
}
