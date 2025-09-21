package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents system state changes observed during test execution, tracking execution success,
 * timing, and system characteristics for cross-runtime consistency analysis.
 *
 * @since 1.0.0
 */
public final class SystemStateChange {
  private final boolean executionSuccessful;
  private final Duration executionDuration;
  private final String systemState;
  private final long timestamp;

  /**
   * Creates a new system state change with the specified characteristics.
   *
   * @param executionSuccessful whether the execution was successful
   * @param executionDuration duration of the execution
   * @param systemState string representation of system state
   */
  public SystemStateChange(
      final boolean executionSuccessful,
      final Duration executionDuration,
      final String systemState) {
    this.executionSuccessful = executionSuccessful;
    this.executionDuration =
        Objects.requireNonNull(executionDuration, "executionDuration cannot be null");
    this.systemState = Objects.requireNonNull(systemState, "systemState cannot be null");
    this.timestamp = System.currentTimeMillis();
  }

  public boolean isExecutionSuccessful() {
    return executionSuccessful;
  }

  public Duration getExecutionDuration() {
    return executionDuration;
  }

  public String getSystemState() {
    return systemState;
  }

  public long getTimestamp() {
    return timestamp;
  }

  /**
   * Compares this system state change with another for consistency analysis.
   *
   * @param other the other system state change to compare with
   * @return comparison result with consistency metrics
   */
  public SystemComparisonMetrics compareWith(final SystemStateChange other) {
    Objects.requireNonNull(other, "other cannot be null");

    final boolean executionStatusMatch = this.executionSuccessful == other.executionSuccessful;
    final boolean systemStateMatch = this.systemState.equals(other.systemState);

    final double durationRatio =
        this.executionDuration.toNanos() > 0 && other.executionDuration.toNanos() > 0
            ? Math.max(this.executionDuration.toNanos(), other.executionDuration.toNanos())
                / (double)
                    Math.min(this.executionDuration.toNanos(), other.executionDuration.toNanos())
            : 1.0;

    return new SystemComparisonMetrics(executionStatusMatch, systemStateMatch, durationRatio);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SystemStateChange that = (SystemStateChange) obj;
    return executionSuccessful == that.executionSuccessful
        && timestamp == that.timestamp
        && Objects.equals(executionDuration, that.executionDuration)
        && Objects.equals(systemState, that.systemState);
  }

  @Override
  public int hashCode() {
    return Objects.hash(executionSuccessful, executionDuration, systemState, timestamp);
  }

  @Override
  public String toString() {
    return "SystemStateChange{"
        + "executionSuccessful="
        + executionSuccessful
        + ", duration="
        + executionDuration.toMillis()
        + "ms"
        + ", systemState='"
        + systemState
        + '\''
        + ", timestamp="
        + timestamp
        + '}';
  }

  /** System comparison metrics for consistency analysis. */
  public static final class SystemComparisonMetrics {
    private final boolean executionStatusMatch;
    private final boolean systemStateMatch;
    private final double durationRatio;

    private SystemComparisonMetrics(
        final boolean executionStatusMatch,
        final boolean systemStateMatch,
        final double durationRatio) {
      this.executionStatusMatch = executionStatusMatch;
      this.systemStateMatch = systemStateMatch;
      this.durationRatio = durationRatio;
    }

    public boolean isExecutionStatusMatch() {
      return executionStatusMatch;
    }

    public boolean isSystemStateMatch() {
      return systemStateMatch;
    }

    public double getDurationRatio() {
      return durationRatio;
    }

    /**
     * Checks if system state changes are considered consistent.
     *
     * @return true if system changes are consistent
     */
    public boolean isConsistent() {
      // System is consistent if execution status matches and duration is within 2x variance
      return executionStatusMatch && durationRatio <= 2.0;
    }

    /**
     * Gets overall consistency score for system state comparison.
     *
     * @return consistency score between 0.0 and 1.0
     */
    public double getConsistencyScore() {
      double score = 0.0;

      // Execution status match is most important (60% weight)
      if (executionStatusMatch) {
        score += 0.6;
      }

      // System state match is moderately important (25% weight)
      if (systemStateMatch) {
        score += 0.25;
      }

      // Duration consistency is least important (15% weight)
      if (durationRatio <= 1.2) {
        score += 0.15; // Within 20% is excellent
      } else if (durationRatio <= 2.0) {
        score += 0.1; // Within 2x is acceptable
      }

      return score;
    }

    @Override
    public String toString() {
      return "SystemComparisonMetrics{"
          + "executionStatusMatch="
          + executionStatusMatch
          + ", systemStateMatch="
          + systemStateMatch
          + ", durationRatio="
          + String.format("%.2f", durationRatio)
          + ", consistent="
          + isConsistent()
          + ", score="
          + String.format("%.2f", getConsistencyScore())
          + '}';
    }
  }
}
