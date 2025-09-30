package ai.tegmentum.wasmtime4j.compilation;

/**
 * Tiered compilation configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface TieredCompilationConfig {

  /**
   * Checks if tiered compilation is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets tiered compilation enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the baseline tier configuration.
   *
   * @return baseline tier config
   */
  TierConfig getBaselineTier();

  /**
   * Gets the optimized tier configuration.
   *
   * @return optimized tier config
   */
  TierConfig getOptimizedTier();

  /**
   * Gets the tier transition threshold.
   *
   * @return transition threshold
   */
  int getTransitionThreshold();

  /**
   * Sets the tier transition threshold.
   *
   * @param threshold transition threshold
   */
  void setTransitionThreshold(int threshold);

  /**
   * Gets the compilation timeout in milliseconds.
   *
   * @return compilation timeout
   */
  long getCompilationTimeout();

  /**
   * Sets the compilation timeout.
   *
   * @param timeoutMs timeout in milliseconds
   */
  void setCompilationTimeout(long timeoutMs);

  /** Tier configuration interface. */
  interface TierConfig {
    /**
     * Gets the tier name.
     *
     * @return tier name
     */
    String getTierName();

    /**
     * Gets the optimization level.
     *
     * @return optimization level (0-3)
     */
    int getOptimizationLevel();

    /**
     * Checks if inlining is enabled.
     *
     * @return true if inlining enabled
     */
    boolean isInliningEnabled();

    /**
     * Checks if vectorization is enabled.
     *
     * @return true if vectorization enabled
     */
    boolean isVectorizationEnabled();

    /**
     * Gets the compilation strategy.
     *
     * @return compilation strategy
     */
    CompilationStrategy getStrategy();
  }

  /** Compilation strategy enumeration. */
  enum CompilationStrategy {
    /** Fast compilation, lower quality. */
    FAST,
    /** Balanced compilation. */
    BALANCED,
    /** High optimization, slower compilation. */
    OPTIMIZED
  }
}
