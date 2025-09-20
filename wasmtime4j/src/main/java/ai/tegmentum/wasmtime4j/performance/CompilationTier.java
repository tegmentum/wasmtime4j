package ai.tegmentum.wasmtime4j.performance;

/**
 * Represents the compilation tier of WebAssembly code execution.
 *
 * <p>Different compilation tiers provide different performance characteristics and compilation
 * overhead. Understanding the current tier helps interpret performance metrics and optimization
 * opportunities.
 *
 * @since 1.0.0
 */
public enum CompilationTier {

  /**
   * Code is being interpreted without compilation.
   *
   * <p>Slowest execution but fastest startup time.
   */
  INTERPRETED("Interpreted"),

  /**
   * Code is compiled with basic optimizations for fast compilation.
   *
   * <p>Moderate execution speed with fast compilation time.
   */
  BASELINE("Baseline JIT"),

  /**
   * Code is compiled with aggressive optimizations.
   *
   * <p>Fast execution speed but slower compilation time.
   */
  OPTIMIZED("Optimized JIT"),

  /**
   * Code is ahead-of-time compiled.
   *
   * <p>Fast execution with no runtime compilation overhead.
   */
  AOT("Ahead-of-Time"),

  /** Compilation tier is unknown or not applicable. */
  UNKNOWN("Unknown");

  private final String displayName;

  CompilationTier(final String displayName) {
    this.displayName = displayName;
  }

  /**
   * Gets the human-readable display name for this compilation tier.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Checks if this tier involves just-in-time compilation.
   *
   * @return true if this is a JIT compilation tier
   */
  public boolean isJIT() {
    return this == BASELINE || this == OPTIMIZED;
  }

  /**
   * Checks if this tier is optimized for performance.
   *
   * @return true if this tier provides optimized execution
   */
  public boolean isOptimized() {
    return this == OPTIMIZED || this == AOT;
  }

  /**
   * Gets the relative performance level of this tier.
   *
   * @return performance level (higher numbers indicate better performance)
   */
  public int getPerformanceLevel() {
    switch (this) {
      case INTERPRETED:
        return 1;
      case BASELINE:
        return 2;
      case OPTIMIZED:
        return 4;
      case AOT:
        return 5;
      case UNKNOWN:
      default:
        return 0;
    }
  }

  /**
   * Gets the relative compilation overhead of this tier.
   *
   * @return compilation overhead level (higher numbers indicate more overhead)
   */
  public int getCompilationOverhead() {
    switch (this) {
      case INTERPRETED:
        return 0;
      case BASELINE:
        return 1;
      case OPTIMIZED:
        return 3;
      case AOT:
        return 4;
      case UNKNOWN:
      default:
        return 0;
    }
  }
}
