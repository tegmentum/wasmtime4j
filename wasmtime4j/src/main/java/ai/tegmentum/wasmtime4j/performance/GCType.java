package ai.tegmentum.wasmtime4j.performance;

/**
 * Represents different types of garbage collection.
 *
 * <p>Different GC types have different performance characteristics and impact patterns.
 *
 * @since 1.0.0
 */
public enum GCType {

  /**
   * Minor garbage collection - collects young generation objects.
   *
   * <p>Typically fast and frequent, minimal performance impact.
   */
  MINOR("Minor GC"),

  /**
   * Major garbage collection - collects entire heap.
   *
   * <p>Typically slower and less frequent, significant performance impact.
   */
  MAJOR("Major GC"),

  /**
   * Full garbage collection - complete heap cleanup including metadata.
   *
   * <p>Slowest type of GC, maximum performance impact.
   */
  FULL("Full GC"),

  /**
   * Concurrent garbage collection - runs concurrently with application.
   *
   * <p>Lower pause times but may have throughput impact.
   */
  CONCURRENT("Concurrent GC"),

  /**
   * Incremental garbage collection - processes heap in small increments.
   *
   * <p>Reduces pause times by spreading work over multiple cycles.
   */
  INCREMENTAL("Incremental GC"),

  /**
   * Unknown or custom garbage collection type.
   */
  UNKNOWN("Unknown GC");

  private final String displayName;

  GCType(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the human-readable display name for this GC type.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this GC type typically has high performance impact.
   *
   * @return true if this GC type typically causes significant pauses
   */
  public boolean isHighImpact() {
    return this == MAJOR || this == FULL;
  }

  /**
   * Checks if this GC type runs concurrently with application execution.
   *
   * @return true if this GC type is concurrent or incremental
   */
  public boolean isConcurrent() {
    return this == CONCURRENT || this == INCREMENTAL;
  }

  /**
   * Gets the expected performance impact level for this GC type.
   *
   * @return impact level (1 = minimal, 5 = severe)
   */
  public int getImpactLevel() {
    switch (this) {
      case MINOR:
        return 1;
      case CONCURRENT:
      case INCREMENTAL:
        return 2;
      case MAJOR:
        return 4;
      case FULL:
        return 5;
      case UNKNOWN:
      default:
        return 3;
    }
  }
}