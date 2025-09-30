package ai.tegmentum.wasmtime4j.compilation;

import java.util.List;
import java.util.Map;

/**
 * Function inlining optimization strategy.
 *
 * <p>Inlines small, frequently called functions to reduce function call overhead
 * and enable further optimizations. This optimization is particularly effective
 * for hot paths with many small function calls.
 *
 * @since 1.0.0
 */
public final class InliningOptimization extends BaseOptimizationStrategy {

  private final int maxInlineDepth;
  private final int maxInlineSizeBytes;
  private final double callFrequencyThreshold;

  /**
   * Creates an inlining optimization with default parameters.
   */
  public InliningOptimization() {
    this(4, 256, 0.1); // Default: depth 4, max 256 bytes, 10% call frequency
  }

  /**
   * Creates an inlining optimization with custom parameters.
   *
   * @param maxInlineDepth maximum inlining depth
   * @param maxInlineSizeBytes maximum function size to inline (bytes)
   * @param callFrequencyThreshold minimum call frequency for inlining
   */
  public InliningOptimization(final int maxInlineDepth,
                              final int maxInlineSizeBytes,
                              final double callFrequencyThreshold) {
    super("inlining",
          "Inline small frequently called functions to reduce call overhead",
          6);
    this.maxInlineDepth = maxInlineDepth;
    this.maxInlineSizeBytes = maxInlineSizeBytes;
    this.callFrequencyThreshold = callFrequencyThreshold;
  }

  @Override
  public double estimateCompilationOverhead(final long moduleSize) {
    // Inlining increases compilation time but the overhead is moderate
    // Larger modules have higher relative overhead due to more inline opportunities
    final double sizeModifier = Math.min(moduleSize / (1024.0 * 1024.0), 2.0); // Up to 2x for large modules
    return 1.0 + (0.3 * sizeModifier); // 30% base overhead, scaled by size
  }

  @Override
  public double estimatePerformanceImprovement(final ExecutionProfile executionProfile) {
    if (!isApplicable(executionProfile)) {
      return 1.0; // No improvement if not applicable
    }

    double improvement = 1.0;

    // Higher improvement for hot paths (more function calls to optimize)
    if (executionProfile.isHotPath()) {
      improvement += 0.15; // 15% improvement for hot paths
    }

    // Additional improvement based on function count (more inlining opportunities)
    if (executionProfile.getFunctionCount() > 10) {
      improvement += 0.10; // 10% additional improvement for multi-function modules
    }

    // Higher improvement for compute-intensive workloads
    if (executionProfile.isComputeIntensive()) {
      improvement += 0.08; // 8% additional improvement
    }

    // Diminishing returns for very large execution counts (already optimized)
    if (executionProfile.getExecutionCount() > 50000) {
      improvement *= 0.8; // Reduce improvement by 20%
    }

    return Math.min(improvement, 1.5); // Cap at 50% improvement
  }

  @Override
  public boolean isApplicable(final ExecutionProfile profile) {
    // Inlining is most beneficial for:
    // 1. Modules with multiple functions
    // 2. Hot paths or frequently executed code
    // 3. Not too large modules (compilation overhead becomes too high)
    return profile.getFunctionCount() > 1 &&
           (profile.isHotPath() || profile.getExecutionCount() > 100) &&
           profile.getModuleSize() < 5 * 1024 * 1024; // 5MB limit
  }

  @Override
  public Map<String, String> getCraneliftFlags() {
    return Map.of(
        "enable_inlining", "true",
        "max_inline_depth", String.valueOf(maxInlineDepth),
        "inline_size_threshold", String.valueOf(maxInlineSizeBytes),
        "inline_call_threshold", String.valueOf((int) (callFrequencyThreshold * 100))
    );
  }

  @Override
  public List<String> getDependencies() {
    // Inlining works better after constant folding and dead code elimination
    return List.of("constant_folding", "dead_code_elimination");
  }

  @Override
  public List<String> getConflicts() {
    // No direct conflicts, but may reduce effectiveness of some optimizations
    return List.of();
  }

  /**
   * Creates an aggressive inlining configuration for hot paths.
   *
   * @return aggressive inlining optimization
   */
  public static InliningOptimization aggressive() {
    return new InliningOptimization(8, 512, 0.05);
  }

  /**
   * Creates a conservative inlining configuration for size-sensitive applications.
   *
   * @return conservative inlining optimization
   */
  public static InliningOptimization conservative() {
    return new InliningOptimization(2, 128, 0.2);
  }

  public int getMaxInlineDepth() {
    return maxInlineDepth;
  }

  public int getMaxInlineSizeBytes() {
    return maxInlineSizeBytes;
  }

  public double getCallFrequencyThreshold() {
    return callFrequencyThreshold;
  }

  @Override
  public String toString() {
    return String.format("%s (depth=%d, maxSize=%d bytes, freqThreshold=%.1f%%)",
                         super.toString(), maxInlineDepth, maxInlineSizeBytes,
                         callFrequencyThreshold * 100);
  }
}