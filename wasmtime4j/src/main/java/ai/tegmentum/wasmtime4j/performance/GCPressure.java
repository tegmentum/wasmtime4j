package ai.tegmentum.wasmtime4j.performance;

/**
 * Represents the current garbage collection pressure level.
 *
 * <p>GC pressure indicates how much strain the garbage collector is under, which affects
 * performance characteristics and optimization strategies.
 *
 * @since 1.0.0
 */
public enum GCPressure {

  /**
   * Low GC pressure - minimal garbage collection activity.
   *
   * <p>Optimal conditions, infrequent GC with short pauses.
   */
  LOW("Low"),

  /**
   * Moderate GC pressure - normal garbage collection activity.
   *
   * <p>Regular GC activity within expected parameters.
   */
  MODERATE("Moderate"),

  /**
   * High GC pressure - frequent garbage collection activity.
   *
   * <p>GC is running frequently, may impact performance.
   */
  HIGH("High"),

  /**
   * Critical GC pressure - severe garbage collection stress.
   *
   * <p>GC is under severe pressure, significant performance impact.
   */
  CRITICAL("Critical"),

  /** GC pressure level cannot be determined. */
  UNKNOWN("Unknown");

  private final String displayName;

  GCPressure(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the human-readable display name for this pressure level.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this pressure level indicates problematic GC activity.
   *
   * @return true if GC pressure is high or critical
   */
  public boolean isProblematic() {
    return this == HIGH || this == CRITICAL;
  }

  /**
   * Gets the performance impact factor for this pressure level.
   *
   * @return impact factor (1.0 = no impact, higher values = more impact)
   */
  public double getPerformanceImpactFactor() {
    switch (this) {
      case LOW:
        return 1.0;
      case MODERATE:
        return 1.05;
      case HIGH:
        return 1.2;
      case CRITICAL:
        return 1.5;
      case UNKNOWN:
      default:
        return 1.0;
    }
  }

  /**
   * Checks if memory allocations should be conservative under this pressure.
   *
   * @return true if conservative allocation strategies are recommended
   */
  public boolean shouldUseConservativeAllocation() {
    return this == HIGH || this == CRITICAL;
  }
}
