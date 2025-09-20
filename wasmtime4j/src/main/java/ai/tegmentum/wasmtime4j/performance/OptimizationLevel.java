package ai.tegmentum.wasmtime4j.performance;

/**
 * Enumeration of optimization levels for WebAssembly performance optimization.
 *
 * <p>OptimizationLevel defines different tiers of optimization aggressiveness, each with
 * different trade-offs between compilation time, memory usage, and runtime performance.
 *
 * @since 1.0.0
 */
public enum OptimizationLevel {

  /**
   * No optimization applied.
   *
   * <p>Fastest compilation time with minimal performance optimization. Suitable for
   * development, debugging, or when compilation speed is more important than runtime performance.
   *
   * <ul>
   *   <li>Compilation time: Fastest</li>
   *   <li>Memory usage: Lowest</li>
   *   <li>Runtime performance: Baseline</li>
   *   <li>Use cases: Development, debugging, quick prototyping</li>
   * </ul>
   */
  NONE("No optimization", 0, "Fastest compilation, baseline performance"),

  /**
   * Basic optimizations with minimal compilation overhead.
   *
   * <p>Applies fundamental optimizations that provide good performance improvements
   * with minimal increase in compilation time. Good balance for general use.
   *
   * <ul>
   *   <li>Compilation time: Fast</li>
   *   <li>Memory usage: Low</li>
   *   <li>Runtime performance: Good</li>
   *   <li>Use cases: General production use, moderate performance requirements</li>
   * </ul>
   */
  BASIC("Basic optimization", 1, "Good performance with fast compilation"),

  /**
   * Aggressive optimizations for high-performance scenarios.
   *
   * <p>Applies more sophisticated optimizations that significantly improve runtime
   * performance at the cost of increased compilation time and memory usage.
   *
   * <ul>
   *   <li>Compilation time: Moderate</li>
   *   <li>Memory usage: Moderate</li>
   *   <li>Runtime performance: High</li>
   *   <li>Use cases: Performance-critical applications, long-running processes</li>
   * </ul>
   */
  AGGRESSIVE("Aggressive optimization", 2, "High performance with moderate compilation cost"),

  /**
   * Maximum optimization for the highest possible performance.
   *
   * <p>Applies all available optimizations including experimental and advanced techniques.
   * Provides the best possible runtime performance but with significant compilation overhead.
   *
   * <ul>
   *   <li>Compilation time: Slow</li>
   *   <li>Memory usage: High</li>
   *   <li>Runtime performance: Maximum</li>
   *   <li>Use cases: Performance-critical code, server applications, compute-intensive workloads</li>
   * </ul>
   */
  MAXIMUM("Maximum optimization", 3, "Best performance with highest compilation cost");

  private final String displayName;
  private final int level;
  private final String description;

  OptimizationLevel(final String displayName, final int level, final String description) {
    this.displayName = displayName;
    this.level = level;
    this.description = description;
  }

  /**
   * Gets the human-readable display name for this optimization level.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the numeric level value for comparison and ordering.
   *
   * <p>Higher values indicate more aggressive optimization.
   *
   * @return numeric optimization level (0-3)
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets a description of this optimization level's characteristics.
   *
   * @return optimization level description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this optimization level is more aggressive than another.
   *
   * @param other optimization level to compare with
   * @return true if this level is more aggressive
   * @throws IllegalArgumentException if other is null
   */
  public boolean isMoreAggressiveThan(final OptimizationLevel other) {
    if (other == null) {
      throw new IllegalArgumentException("Other optimization level cannot be null");
    }
    return this.level > other.level;
  }

  /**
   * Checks if this optimization level is less aggressive than another.
   *
   * @param other optimization level to compare with
   * @return true if this level is less aggressive
   * @throws IllegalArgumentException if other is null
   */
  public boolean isLessAggressiveThan(final OptimizationLevel other) {
    if (other == null) {
      throw new IllegalArgumentException("Other optimization level cannot be null");
    }
    return this.level < other.level;
  }

  /**
   * Gets the estimated compilation time multiplier for this optimization level.
   *
   * <p>Returns a relative multiplier compared to no optimization (NONE = 1.0).
   *
   * @return compilation time multiplier
   */
  public double getCompilationTimeMultiplier() {
    switch (this) {
      case NONE:
        return 1.0;
      case BASIC:
        return 1.5;
      case AGGRESSIVE:
        return 3.0;
      case MAXIMUM:
        return 6.0;
      default:
        return 1.0;
    }
  }

  /**
   * Gets the estimated memory usage multiplier for this optimization level.
   *
   * <p>Returns a relative multiplier compared to no optimization (NONE = 1.0).
   *
   * @return memory usage multiplier
   */
  public double getMemoryUsageMultiplier() {
    switch (this) {
      case NONE:
        return 1.0;
      case BASIC:
        return 1.2;
      case AGGRESSIVE:
        return 1.8;
      case MAXIMUM:
        return 2.5;
      default:
        return 1.0;
    }
  }

  /**
   * Gets the estimated performance improvement factor for this optimization level.
   *
   * <p>Returns a relative performance multiplier compared to no optimization (NONE = 1.0).
   *
   * @return performance improvement factor
   */
  public double getPerformanceImprovementFactor() {
    switch (this) {
      case NONE:
        return 1.0;
      case BASIC:
        return 1.3;
      case AGGRESSIVE:
        return 2.0;
      case MAXIMUM:
        return 3.0;
      default:
        return 1.0;
    }
  }

  /**
   * Determines the recommended optimization level based on use case requirements.
   *
   * @param prioritizeCompilationSpeed true if compilation speed is more important than runtime performance
   * @param isLongRunning true if the application runs for extended periods
   * @param isPerformanceCritical true if runtime performance is critical
   * @return recommended optimization level
   */
  public static OptimizationLevel recommend(
      final boolean prioritizeCompilationSpeed,
      final boolean isLongRunning,
      final boolean isPerformanceCritical) {

    if (prioritizeCompilationSpeed) {
      return NONE;
    }

    if (isPerformanceCritical && isLongRunning) {
      return MAXIMUM;
    }

    if (isPerformanceCritical || isLongRunning) {
      return AGGRESSIVE;
    }

    return BASIC;
  }

  @Override
  public String toString() {
    return displayName + " (Level " + level + "): " + description;
  }
}