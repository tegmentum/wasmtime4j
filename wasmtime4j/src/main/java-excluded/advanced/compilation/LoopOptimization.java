package ai.tegmentum.wasmtime4j.compilation;

import java.util.List;
import java.util.Map;

/**
 * Loop optimization strategy for WebAssembly compilation.
 *
 * <p>Implements sophisticated loop optimizations including unrolling, vectorization,
 * strength reduction, and invariant code motion. These optimizations are particularly
 * effective for compute-intensive workloads with significant loop structures.
 *
 * @since 1.0.0
 */
public final class LoopOptimization extends BaseOptimizationStrategy {

  private final int maxUnrollFactor;
  private final boolean enableVectorization;
  private final boolean enableStrengthReduction;
  private final boolean enableInvariantCodeMotion;
  private final boolean enableLoopInterchange;
  private final int maxLoopDepth;

  /**
   * Creates loop optimization with default parameters.
   */
  public LoopOptimization() {
    this(8, true, true, true, false, 4);
  }

  /**
   * Creates loop optimization with custom parameters.
   *
   * @param maxUnrollFactor maximum loop unroll factor
   * @param enableVectorization enable loop vectorization
   * @param enableStrengthReduction enable strength reduction optimization
   * @param enableInvariantCodeMotion enable loop invariant code motion
   * @param enableLoopInterchange enable loop interchange optimization
   * @param maxLoopDepth maximum loop nesting depth to optimize
   */
  public LoopOptimization(final int maxUnrollFactor,
                          final boolean enableVectorization,
                          final boolean enableStrengthReduction,
                          final boolean enableInvariantCodeMotion,
                          final boolean enableLoopInterchange,
                          final int maxLoopDepth) {
    super("loop_optimization",
          "Optimize loops through unrolling, vectorization, and strength reduction",
          7);
    this.maxUnrollFactor = maxUnrollFactor;
    this.enableVectorization = enableVectorization;
    this.enableStrengthReduction = enableStrengthReduction;
    this.enableInvariantCodeMotion = enableInvariantCodeMotion;
    this.enableLoopInterchange = enableLoopInterchange;
    this.maxLoopDepth = maxLoopDepth;
  }

  @Override
  public double estimateCompilationOverhead(final long moduleSize) {
    // Loop optimization can be expensive, especially with vectorization
    double overhead = 1.0;

    // Base overhead for loop analysis and transformation
    overhead += 0.2; // 20% base overhead

    // Additional overhead for vectorization analysis
    if (enableVectorization) {
      overhead += 0.3; // 30% additional overhead
    }

    // Additional overhead for complex optimizations
    if (enableLoopInterchange) {
      overhead += 0.15; // 15% additional overhead
    }

    // Scale with module size (more loops to analyze)
    final double sizeModifier = Math.min(moduleSize / (1024.0 * 1024.0), 3.0);
    overhead *= (1.0 + 0.1 * sizeModifier);

    return overhead;
  }

  @Override
  public double estimatePerformanceImprovement(final ExecutionProfile executionProfile) {
    if (!isApplicable(executionProfile)) {
      return 1.0; // No improvement if not applicable
    }

    double improvement = 1.0;

    // Base improvement for loop-heavy code
    if (executionProfile.hasLoops()) {
      improvement += 0.20; // 20% base improvement for loops
    }

    // Higher improvement for compute-intensive workloads
    if (executionProfile.isComputeIntensive()) {
      improvement += 0.25; // 25% additional improvement
    }

    // Vectorization provides significant improvement for SIMD operations
    if (enableVectorization && executionProfile.hasVectorOperations()) {
      improvement += 0.40; // 40% improvement for vectorizable code
    }

    // Hot paths benefit more from loop optimization
    if (executionProfile.isHotPath()) {
      improvement += 0.15; // 15% additional improvement
    }

    // Memory-intensive workloads benefit from invariant code motion
    if (enableInvariantCodeMotion && executionProfile.isMemoryIntensive()) {
      improvement += 0.12; // 12% improvement from reduced memory access
    }

    // Long-running functions benefit more from loop optimization
    if (executionProfile.getAverageExecutionTimeMs() > 50.0) {
      improvement += 0.10; // 10% additional improvement
    }

    return Math.min(improvement, 2.5); // Cap at 150% improvement (250% of original)
  }

  @Override
  public boolean isApplicable(final ExecutionProfile profile) {
    // Loop optimization is most beneficial for:
    // 1. Code with loops (obviously)
    // 2. Compute-intensive workloads
    // 3. Long-running functions
    // 4. Hot paths
    return profile.hasLoops() ||
           profile.isComputeIntensive() ||
           profile.getAverageExecutionTimeMs() > 10.0 ||
           profile.isHotPath();
  }

  @Override
  public Map<String, String> getCraneliftFlags() {
    final Map<String, String> flags = new java.util.HashMap<>();

    flags.put("enable_loop_optimization", "true");
    flags.put("max_loop_unroll_factor", String.valueOf(maxUnrollFactor));
    flags.put("max_loop_depth", String.valueOf(maxLoopDepth));

    if (enableVectorization) {
      flags.put("enable_loop_vectorization", "true");
      flags.put("enable_slp_vectorization", "true");
    }

    if (enableStrengthReduction) {
      flags.put("enable_strength_reduction", "true");
    }

    if (enableInvariantCodeMotion) {
      flags.put("enable_licm", "true"); // Loop Invariant Code Motion
    }

    if (enableLoopInterchange) {
      flags.put("enable_loop_interchange", "true");
    }

    return flags;
  }

  @Override
  public List<String> getDependencies() {
    // Loop optimization benefits from prior optimizations
    return List.of("dead_code_elimination", "constant_folding");
  }

  @Override
  public List<String> getConflicts() {
    // No direct conflicts, but some optimizations may interfere
    return List.of();
  }

  /**
   * Creates aggressive loop optimization for high-performance computing.
   *
   * @return aggressive loop optimization
   */
  public static LoopOptimization aggressive() {
    return new LoopOptimization(16, true, true, true, true, 6);
  }

  /**
   * Creates vectorization-focused loop optimization.
   *
   * @return vectorization-focused optimization
   */
  public static LoopOptimization vectorizationFocused() {
    return new LoopOptimization(4, true, true, true, false, 3);
  }

  /**
   * Creates conservative loop optimization for code size.
   *
   * @return conservative loop optimization
   */
  public static LoopOptimization conservative() {
    return new LoopOptimization(2, false, true, true, false, 2);
  }

  /**
   * Creates memory-focused loop optimization.
   *
   * @return memory-focused optimization
   */
  public static LoopOptimization memoryFocused() {
    return new LoopOptimization(4, false, true, true, false, 3);
  }

  public int getMaxUnrollFactor() {
    return maxUnrollFactor;
  }

  public boolean isVectorizationEnabled() {
    return enableVectorization;
  }

  public boolean isStrengthReductionEnabled() {
    return enableStrengthReduction;
  }

  public boolean isInvariantCodeMotionEnabled() {
    return enableInvariantCodeMotion;
  }

  public boolean isLoopInterchangeEnabled() {
    return enableLoopInterchange;
  }

  public int getMaxLoopDepth() {
    return maxLoopDepth;
  }

  @Override
  public String toString() {
    return String.format("%s (unroll=%d, vec=%b, strength=%b, licm=%b, interchange=%b, depth=%d)",
                         super.toString(), maxUnrollFactor, enableVectorization,
                         enableStrengthReduction, enableInvariantCodeMotion,
                         enableLoopInterchange, maxLoopDepth);
  }
}