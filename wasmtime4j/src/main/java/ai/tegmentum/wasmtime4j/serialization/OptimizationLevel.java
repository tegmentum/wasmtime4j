package ai.tegmentum.wasmtime4j.serialization;

/**
 * Optimization levels that can be applied during module serialization.
 *
 * <p>Optimization levels control various compiler optimizations that affect code generation,
 * performance, and debugging capabilities. Higher optimization levels typically produce faster
 * code but may increase compilation time and reduce debugging information.
 *
 * @since 1.0.0
 */
public enum OptimizationLevel {

  /**
   * No optimizations applied.
   *
   * <p>Fastest compilation with largest code size and best debugging experience. All debug
   * information is preserved and code generation is straightforward without optimizations.
   */
  NONE("none", 0),

  /**
   * Basic optimizations applied.
   *
   * <p>Standard optimizations that provide good performance improvement with minimal impact on
   * compilation time and debugging. This is the recommended default for most scenarios.
   */
  BASIC("basic", 1),

  /**
   * Speed-focused optimizations.
   *
   * <p>Aggressive optimizations focused on runtime performance. May significantly increase
   * compilation time and reduce debugging capabilities. Best for production code where
   * performance is critical.
   */
  SPEED("speed", 2),

  /**
   * Size-focused optimizations.
   *
   * <p>Optimizations focused on reducing code size rather than maximizing speed. Useful for
   * memory-constrained environments or when minimizing serialized module size is important.
   */
  SIZE("size", 3),

  /**
   * Advanced optimizations.
   *
   * <p>Experimental and advanced optimizations that may provide additional performance benefits
   * but could be less stable or have higher compilation overhead. Use with caution in production.
   */
  ADVANCED("advanced", 4);

  private final String name;
  private final int level;

  OptimizationLevel(final String name, final int level) {
    this.name = name;
    this.level = level;
  }

  /**
   * Gets the string name of this optimization level.
   *
   * @return the optimization level name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the numeric level of this optimization.
   *
   * @return the optimization level value
   */
  public int getLevel() {
    return level;
  }

  /**
   * Checks if this optimization level is more aggressive than another.
   *
   * @param other the optimization level to compare with
   * @return true if this level is more aggressive than the other
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
   * @param other the optimization level to compare with
   * @return true if this level is less aggressive than the other
   * @throws IllegalArgumentException if other is null
   */
  public boolean isLessAggressiveThan(final OptimizationLevel other) {
    if (other == null) {
      throw new IllegalArgumentException("Other optimization level cannot be null");
    }
    return this.level < other.level;
  }

  /**
   * Gets an OptimizationLevel by its string name.
   *
   * @param name the optimization level name
   * @return the corresponding OptimizationLevel
   * @throws IllegalArgumentException if the name is not recognized
   */
  public static OptimizationLevel fromName(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Optimization level name cannot be null");
    }

    for (final OptimizationLevel level : values()) {
      if (level.name.equals(name)) {
        return level;
      }
    }

    throw new IllegalArgumentException("Unknown optimization level: " + name);
  }

  /**
   * Gets an OptimizationLevel by its numeric level.
   *
   * @param level the optimization level value
   * @return the corresponding OptimizationLevel
   * @throws IllegalArgumentException if the level is not recognized
   */
  public static OptimizationLevel fromLevel(final int level) {
    for (final OptimizationLevel opt : values()) {
      if (opt.level == level) {
        return opt;
      }
    }

    throw new IllegalArgumentException("Unknown optimization level: " + level);
  }

  @Override
  public String toString() {
    return name + "(" + level + ")";
  }
}