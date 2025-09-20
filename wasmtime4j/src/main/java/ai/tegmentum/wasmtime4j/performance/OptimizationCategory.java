package ai.tegmentum.wasmtime4j.performance;

/**
 * Categories of optimization strategies based on their primary optimization target.
 *
 * <p>Optimization categories help organize optimization strategies by their main focus area, making
 * it easier to select appropriate optimizations for specific performance goals.
 *
 * @since 1.0.0
 */
public enum OptimizationCategory {

  /**
   * Optimizations focused on improving execution speed.
   *
   * <p>These optimizations aim to reduce instruction count, improve CPU pipeline utilization, and
   * minimize execution time.
   */
  EXECUTION_SPEED("Execution Speed"),

  /**
   * Optimizations focused on reducing memory usage.
   *
   * <p>These optimizations aim to minimize memory footprint, improve cache performance, and
   * optimize memory access patterns.
   */
  MEMORY_EFFICIENCY("Memory Efficiency"),

  /**
   * Optimizations focused on reducing code size.
   *
   * <p>These optimizations aim to minimize the size of compiled code, which can improve load times
   * and cache performance.
   */
  CODE_SIZE("Code Size"),

  /**
   * Optimizations focused on improving compilation speed.
   *
   * <p>These optimizations aim to reduce compilation time and improve startup performance.
   */
  COMPILATION_SPEED("Compilation Speed"),

  /**
   * Optimizations focused on improving startup time.
   *
   * <p>These optimizations aim to reduce initialization overhead and improve application launch
   * performance.
   */
  STARTUP_TIME("Startup Time"),

  /**
   * Optimizations focused on improving energy efficiency.
   *
   * <p>These optimizations aim to reduce power consumption and improve battery life on mobile and
   * embedded devices.
   */
  ENERGY_EFFICIENCY("Energy Efficiency"),

  /**
   * General optimizations that may affect multiple categories.
   *
   * <p>These optimizations provide broad improvements across multiple performance dimensions.
   */
  GENERAL("General");

  private final String displayName;

  OptimizationCategory(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the human-readable display name for this optimization category.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this category focuses on runtime performance.
   *
   * @return true if this category targets runtime performance improvements
   */
  public boolean isRuntimePerformance() {
    return this == EXECUTION_SPEED || this == MEMORY_EFFICIENCY || this == ENERGY_EFFICIENCY;
  }

  /**
   * Checks if this category focuses on compile-time performance.
   *
   * @return true if this category targets compile-time performance improvements
   */
  public boolean isCompileTimePerformance() {
    return this == COMPILATION_SPEED || this == STARTUP_TIME;
  }

  /**
   * Gets the priority level for this optimization category.
   *
   * @return priority level (1 = highest, 5 = lowest)
   */
  public int getPriorityLevel() {
    switch (this) {
      case EXECUTION_SPEED:
        return 1;
      case MEMORY_EFFICIENCY:
        return 2;
      case STARTUP_TIME:
        return 3;
      case CODE_SIZE:
      case COMPILATION_SPEED:
        return 4;
      case ENERGY_EFFICIENCY:
      case GENERAL:
        return 5;
      default:
        return 5;
    }
  }
}
