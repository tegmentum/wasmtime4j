package ai.tegmentum.wasmtime4j.compilation;

/**
 * Compilation tiers for WebAssembly functions.
 *
 * <p>Represents different levels of optimization and compilation effort,
 * allowing for tiered compilation strategies that balance compilation time
 * with execution performance.
 *
 * @since 1.0.0
 */
public enum CompilationTier {
  /**
   * Baseline tier - fast compilation with minimal optimization.
   * Used for initial compilation or cold functions.
   */
  BASELINE(0, "baseline", "Fast compilation, minimal optimization"),

  /**
   * Optimized tier - moderate compilation time with good optimization.
   * Used for warm functions that are executed regularly.
   */
  OPTIMIZED(1, "optimized", "Balanced compilation time and performance"),

  /**
   * Highly optimized tier - slow compilation with maximum optimization.
   * Used for hot functions that are executed very frequently.
   */
  HIGHLY_OPTIMIZED(2, "highly-optimized", "Maximum optimization, longer compilation time");

  private final int level;
  private final String name;
  private final String description;

  CompilationTier(final int level, final String name, final String description) {
    this.level = level;
    this.name = name;
    this.description = description;
  }

  /**
   * Gets the tier level (higher numbers indicate more optimization).
   *
   * @return the tier level
   */
  public int getLevel() {
    return level;
  }

  /**
   * Gets the tier name.
   *
   * @return the tier name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the tier description.
   *
   * @return the tier description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this tier is higher than another tier.
   *
   * @param other the other tier to compare
   * @return true if this tier is higher, false otherwise
   */
  public boolean isHigherThan(final CompilationTier other) {
    return this.level > other.level;
  }

  /**
   * Checks if this tier is lower than another tier.
   *
   * @param other the other tier to compare
   * @return true if this tier is lower, false otherwise
   */
  public boolean isLowerThan(final CompilationTier other) {
    return this.level < other.level;
  }

  /**
   * Gets the next higher tier, if available.
   *
   * @return the next higher tier, or null if this is the highest
   */
  public CompilationTier getNextTier() {
    return switch (this) {
      case BASELINE -> OPTIMIZED;
      case OPTIMIZED -> HIGHLY_OPTIMIZED;
      case HIGHLY_OPTIMIZED -> null;
    };
  }

  /**
   * Gets the previous lower tier, if available.
   *
   * @return the previous lower tier, or null if this is the lowest
   */
  public CompilationTier getPreviousTier() {
    return switch (this) {
      case BASELINE -> null;
      case OPTIMIZED -> BASELINE;
      case HIGHLY_OPTIMIZED -> OPTIMIZED;
    };
  }

  /**
   * Determines the appropriate tier based on execution count.
   *
   * @param executionCount the number of times the function has been executed
   * @return recommended compilation tier
   */
  public static CompilationTier fromExecutionCount(final long executionCount) {
    if (executionCount >= 10000) {
      return HIGHLY_OPTIMIZED;
    } else if (executionCount >= 1000) {
      return OPTIMIZED;
    } else {
      return BASELINE;
    }
  }

  /**
   * Determines the appropriate tier based on function hotness.
   *
   * @param callsPerSecond the rate of function calls
   * @return recommended compilation tier
   */
  public static CompilationTier fromHotness(final double callsPerSecond) {
    if (callsPerSecond >= 50.0) {
      return HIGHLY_OPTIMIZED;
    } else if (callsPerSecond >= 5.0) {
      return OPTIMIZED;
    } else {
      return BASELINE;
    }
  }
}