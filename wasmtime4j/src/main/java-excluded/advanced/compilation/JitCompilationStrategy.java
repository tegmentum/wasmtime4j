package ai.tegmentum.wasmtime4j.compilation;

/**
 * Advanced JIT compilation strategies for wasmtime4j.
 *
 * <p>This enum defines sophisticated compilation approaches that leverage different optimization
 * levels, execution patterns, and performance characteristics to maximize runtime performance
 * while managing compilation overhead.
 *
 * <p>The compilation strategies are organized in tiers from baseline (fastest compilation,
 * reasonable performance) to highly-optimized (slower compilation, maximum performance).
 *
 * @since 1.0.0
 */
public enum JitCompilationStrategy {

  /**
   * Baseline compilation with minimal optimization.
   *
   * <p>Provides fast compilation times with reasonable runtime performance. Suitable for:
   * - Cold start scenarios requiring immediate execution
   * - Short-lived or rarely executed functions
   * - Development and debugging environments
   * - Resource-constrained environments
   */
  BASELINE(0, "baseline", 100, 50),

  /**
   * Standard compilation with moderate optimization.
   *
   * <p>Balanced compilation approach providing good performance with reasonable compilation time.
   * Suitable for:
   * - General-purpose WebAssembly execution
   * - Functions with moderate execution frequency
   * - Production environments with balanced performance requirements
   */
  STANDARD(1, "standard", 300, 75),

  /**
   * Optimized compilation with aggressive optimization.
   *
   * <p>Applies comprehensive optimizations for high-performance execution. Suitable for:
   * - Frequently executed functions
   * - Performance-critical code paths
   * - Long-running applications
   * - CPU-intensive computations
   */
  OPTIMIZED(2, "optimized", 800, 90),

  /**
   * Highly-optimized compilation with maximum optimization.
   *
   * <p>Applies the most aggressive optimizations available, including profile-guided optimization,
   * advanced vectorization, and speculative optimization. Suitable for:
   * - Hot spots identified through profiling
   * - Mission-critical performance sections
   * - Long-running server applications
   * - High-throughput processing pipelines
   */
  HIGHLY_OPTIMIZED(3, "highly_optimized", 2000, 100),

  /**
   * Adaptive compilation that automatically selects optimization level.
   *
   * <p>Uses runtime profiling and execution patterns to automatically determine the optimal
   * compilation strategy. Starts with baseline compilation and progressively optimizes
   * based on execution frequency and performance characteristics.
   */
  ADAPTIVE(4, "adaptive", 150, 85);

  private final int tier;
  private final String name;
  private final int typicalCompilationTimeMs;
  private final int performanceScore;

  JitCompilationStrategy(final int tier, final String name,
                         final int typicalCompilationTimeMs, final int performanceScore) {
    this.tier = tier;
    this.name = name;
    this.typicalCompilationTimeMs = typicalCompilationTimeMs;
    this.performanceScore = performanceScore;
  }

  /**
   * Gets the compilation tier level (0 = baseline, higher = more optimized).
   *
   * @return tier level
   */
  public int getTier() {
    return tier;
  }

  /**
   * Gets the strategy name for configuration and logging.
   *
   * @return strategy name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the typical compilation time in milliseconds for this strategy.
   *
   * @return typical compilation time in ms
   */
  public int getTypicalCompilationTimeMs() {
    return typicalCompilationTimeMs;
  }

  /**
   * Gets the expected performance score (0-100) for this strategy.
   *
   * @return performance score
   */
  public int getPerformanceScore() {
    return performanceScore;
  }

  /**
   * Determines if this strategy should be used for hot code paths.
   *
   * @return true if suitable for hot paths
   */
  public boolean isSuitableForHotPaths() {
    return tier >= OPTIMIZED.tier;
  }

  /**
   * Determines if this strategy should be used for cold start scenarios.
   *
   * @return true if suitable for cold starts
   */
  public boolean isSuitableForColdStart() {
    return tier <= STANDARD.tier;
  }

  /**
   * Gets the compilation strategy based on execution characteristics.
   *
   * @param callCount number of times function has been called
   * @param executionTimeMs total execution time in milliseconds
   * @param isHotPath true if this is identified as a hot path
   * @return recommended compilation strategy
   */
  public static JitCompilationStrategy selectForExecutionPattern(
      final long callCount, final long executionTimeMs, final boolean isHotPath) {

    // Hot paths get aggressive optimization
    if (isHotPath || callCount > 10000 || executionTimeMs > 5000) {
      return HIGHLY_OPTIMIZED;
    }

    // Frequently called functions get optimization
    if (callCount > 1000 || executionTimeMs > 1000) {
      return OPTIMIZED;
    }

    // Moderately used functions get standard compilation
    if (callCount > 100 || executionTimeMs > 100) {
      return STANDARD;
    }

    // Infrequently used functions get baseline compilation
    return BASELINE;
  }

  /**
   * Gets the compilation strategy based on performance requirements.
   *
   * @param maxCompilationTimeMs maximum acceptable compilation time
   * @param minPerformanceScore minimum required performance score
   * @return best strategy meeting requirements
   */
  public static JitCompilationStrategy selectForRequirements(
      final int maxCompilationTimeMs, final int minPerformanceScore) {

    // Find highest performance strategy within time constraints
    JitCompilationStrategy best = BASELINE;
    for (final JitCompilationStrategy strategy : values()) {
      if (strategy != ADAPTIVE &&
          strategy.typicalCompilationTimeMs <= maxCompilationTimeMs &&
          strategy.performanceScore >= minPerformanceScore &&
          strategy.performanceScore > best.performanceScore) {
        best = strategy;
      }
    }

    return best;
  }

  /**
   * Gets the next higher optimization tier for progressive optimization.
   *
   * @return next tier or null if already at highest
   */
  public JitCompilationStrategy getNextTier() {
    switch (this) {
      case BASELINE:
        return STANDARD;
      case STANDARD:
        return OPTIMIZED;
      case OPTIMIZED:
        return HIGHLY_OPTIMIZED;
      default:
        return null;
    }
  }

  /**
   * Gets the previous lower optimization tier for fallback.
   *
   * @return previous tier or null if already at lowest
   */
  public JitCompilationStrategy getPreviousTier() {
    switch (this) {
      case HIGHLY_OPTIMIZED:
        return OPTIMIZED;
      case OPTIMIZED:
        return STANDARD;
      case STANDARD:
        return BASELINE;
      default:
        return null;
    }
  }

  /**
   * Determines if recompilation from current strategy to target is worthwhile.
   *
   * @param targetStrategy target compilation strategy
   * @param executionCount number of times function has executed
   * @param averageExecutionTimeMs average execution time
   * @return true if recompilation is cost-effective
   */
  public boolean shouldRecompileTo(final JitCompilationStrategy targetStrategy,
                                   final long executionCount,
                                   final double averageExecutionTimeMs) {
    if (targetStrategy.tier <= this.tier) {
      return false; // Don't downgrade or lateral move
    }

    // Estimate performance improvement
    final double performanceGain =
        (targetStrategy.performanceScore - this.performanceScore) / 100.0;
    final double timeSavingsPerExecution =
        averageExecutionTimeMs * performanceGain;

    // Estimate future benefit based on historical execution patterns
    final long estimatedFutureExecutions = Math.max(executionCount, 100);
    final double totalTimeSavings =
        timeSavingsPerExecution * estimatedFutureExecutions;

    // Recompile if total time savings exceed compilation cost
    return totalTimeSavings > targetStrategy.typicalCompilationTimeMs * 2.0;
  }

  @Override
  public String toString() {
    return String.format("%s (tier %d, ~%dms compile, %d%% performance)",
                         name, tier, typicalCompilationTimeMs, performanceScore);
  }
}