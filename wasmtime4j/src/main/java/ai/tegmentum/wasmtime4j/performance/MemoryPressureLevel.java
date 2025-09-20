package ai.tegmentum.wasmtime4j.performance;

/**
 * Represents the current memory pressure level in the system.
 *
 * <p>Memory pressure affects performance characteristics and optimization decisions.
 * High memory pressure may trigger more aggressive garbage collection or
 * influence JIT compilation strategies.
 *
 * @since 1.0.0
 */
public enum MemoryPressureLevel {

  /**
   * Low memory pressure - abundant memory available.
   *
   * <p>Optimal conditions for performance, aggressive optimizations can be applied.
   */
  LOW("Low", 0.0, 0.6),

  /**
   * Moderate memory pressure - some memory constraints.
   *
   * <p>Normal operating conditions, standard optimizations apply.
   */
  MODERATE("Moderate", 0.6, 0.8),

  /**
   * High memory pressure - limited memory available.
   *
   * <p>Performance may be impacted, conservative optimizations preferred.
   */
  HIGH("High", 0.8, 0.95),

  /**
   * Critical memory pressure - very limited memory.
   *
   * <p>Significant performance impact, emergency memory management active.
   */
  CRITICAL("Critical", 0.95, 1.0),

  /**
   * Memory pressure level cannot be determined.
   */
  UNKNOWN("Unknown", 0.0, 1.0);

  private final String displayName;
  private final double minThreshold;
  private final double maxThreshold;

  MemoryPressureLevel(final String displayName, final double minThreshold, final double maxThreshold) {
    this.displayName = displayName;
    this.minThreshold = minThreshold;
    this.maxThreshold = maxThreshold;
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
   * Gets the minimum memory usage threshold for this pressure level.
   *
   * @return minimum threshold as a percentage (0.0 to 1.0)
   */
  public double getMinThreshold() {
    return minThreshold;
  }

  /**
   * Gets the maximum memory usage threshold for this pressure level.
   *
   * @return maximum threshold as a percentage (0.0 to 1.0)
   */
  public double getMaxThreshold() {
    return maxThreshold;
  }

  /**
   * Determines the memory pressure level from a memory usage percentage.
   *
   * @param memoryUsagePercent memory usage as a percentage (0.0 to 1.0)
   * @return corresponding memory pressure level
   * @throws IllegalArgumentException if memoryUsagePercent is out of range
   */
  public static MemoryPressureLevel fromUsagePercent(final double memoryUsagePercent) {
    if (memoryUsagePercent < 0.0 || memoryUsagePercent > 1.0) {
      throw new IllegalArgumentException("Memory usage percent must be between 0.0 and 1.0");
    }

    for (final MemoryPressureLevel level : values()) {
      if (level != UNKNOWN && memoryUsagePercent >= level.minThreshold && memoryUsagePercent < level.maxThreshold) {
        return level;
      }
    }

    return CRITICAL; // Handle edge case where usage equals 1.0
  }

  /**
   * Checks if this pressure level indicates a problematic memory situation.
   *
   * @return true if memory pressure is high or critical
   */
  public boolean isProblematic() {
    return this == HIGH || this == CRITICAL;
  }

  /**
   * Checks if optimizations should be conservative under this pressure level.
   *
   * @return true if conservative optimizations are recommended
   */
  public boolean shouldUseConservativeOptimizations() {
    return this == HIGH || this == CRITICAL;
  }

  /**
   * Gets the impact factor for performance under this pressure level.
   *
   * @return impact factor (1.0 = no impact, higher values = more impact)
   */
  public double getPerformanceImpactFactor() {
    switch (this) {
      case LOW:
        return 1.0;
      case MODERATE:
        return 1.1;
      case HIGH:
        return 1.3;
      case CRITICAL:
        return 1.8;
      case UNKNOWN:
      default:
        return 1.0;
    }
  }
}