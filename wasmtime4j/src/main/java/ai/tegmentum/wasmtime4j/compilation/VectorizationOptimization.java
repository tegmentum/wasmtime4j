package ai.tegmentum.wasmtime4j.compilation;

import java.util.List;
import java.util.Map;

/**
 * Vectorization optimization strategy for WebAssembly SIMD operations.
 *
 * <p>Enables automatic vectorization of scalar operations, SIMD instruction generation,
 * and platform-specific vector optimizations. This optimization provides significant
 * performance improvements for data-parallel computations.
 *
 * @since 1.0.0
 */
public final class VectorizationOptimization extends BaseOptimizationStrategy {

  private final int vectorWidth;
  private final boolean enableAutoVectorization;
  private final boolean enableSlpVectorization;
  private final boolean enableMaskedVectorization;
  private final VectorCostModel costModel;
  private final boolean enablePlatformSpecific;

  /**
   * Vector cost models for optimization decisions.
   */
  public enum VectorCostModel {
    /** Basic cost model with simple heuristics. */
    BASIC,
    /** Target-specific cost model using platform characteristics. */
    TARGET_SPECIFIC,
    /** Aggressive vectorization with minimal cost considerations. */
    AGGRESSIVE
  }

  /**
   * Creates vectorization optimization with default parameters.
   */
  public VectorizationOptimization() {
    this(128, true, true, false, VectorCostModel.TARGET_SPECIFIC, true);
  }

  /**
   * Creates vectorization optimization with custom parameters.
   *
   * @param vectorWidth preferred vector width in bits (128, 256, 512)
   * @param enableAutoVectorization enable automatic loop vectorization
   * @param enableSlpVectorization enable straight-line code vectorization
   * @param enableMaskedVectorization enable masked vector operations
   * @param costModel vector cost model for optimization decisions
   * @param enablePlatformSpecific enable platform-specific optimizations
   */
  public VectorizationOptimization(final int vectorWidth,
                                   final boolean enableAutoVectorization,
                                   final boolean enableSlpVectorization,
                                   final boolean enableMaskedVectorization,
                                   final VectorCostModel costModel,
                                   final boolean enablePlatformSpecific) {
    super("vectorization",
          "Enable SIMD vectorization for data-parallel operations",
          8);
    this.vectorWidth = vectorWidth;
    this.enableAutoVectorization = enableAutoVectorization;
    this.enableSlpVectorization = enableSlpVectorization;
    this.enableMaskedVectorization = enableMaskedVectorization;
    this.costModel = costModel;
    this.enablePlatformSpecific = enablePlatformSpecific;
  }

  @Override
  public double estimateCompilationOverhead(final long moduleSize) {
    // Vectorization analysis can be expensive
    double overhead = 1.0;

    // Base overhead for vectorization analysis
    overhead += 0.25; // 25% base overhead

    // Additional overhead for auto-vectorization
    if (enableAutoVectorization) {
      overhead += 0.35; // 35% additional overhead
    }

    // SLP vectorization adds analysis overhead
    if (enableSlpVectorization) {
      overhead += 0.20; // 20% additional overhead
    }

    // Masked vectorization requires complex analysis
    if (enableMaskedVectorization) {
      overhead += 0.15; // 15% additional overhead
    }

    // Platform-specific optimization adds overhead
    if (enablePlatformSpecific) {
      overhead += 0.10; // 10% additional overhead
    }

    // Scale with module size and vector width
    final double sizeModifier = Math.min(moduleSize / (512.0 * 1024.0), 2.5);
    final double widthModifier = vectorWidth / 128.0; // Normalize to 128-bit baseline
    overhead *= (1.0 + 0.1 * sizeModifier * widthModifier);

    return overhead;
  }

  @Override
  public double estimatePerformanceImprovement(final ExecutionProfile executionProfile) {
    if (!isApplicable(executionProfile)) {
      return 1.0; // No improvement if not applicable
    }

    double improvement = 1.0;

    // Base improvement for vector operations
    if (executionProfile.hasVectorOperations()) {
      improvement += 0.30; // 30% base improvement for existing vector ops
    }

    // Auto-vectorization improvement for loops
    if (enableAutoVectorization && executionProfile.hasLoops()) {
      improvement += 0.50; // 50% improvement from loop vectorization
    }

    // SLP vectorization for straight-line code
    if (enableSlpVectorization) {
      improvement += 0.25; // 25% improvement from SLP vectorization
    }

    // Higher improvement for compute-intensive workloads
    if (executionProfile.isComputeIntensive()) {
      improvement += 0.40; // 40% additional improvement
    }

    // Vector width scaling (wider vectors = better performance)
    final double widthBonus = (vectorWidth / 128.0) * 0.15; // 15% per 128-bit width
    improvement += widthBonus;

    // Hot paths benefit significantly from vectorization
    if (executionProfile.isHotPath()) {
      improvement += 0.30; // 30% additional improvement
    }

    // Memory-intensive workloads benefit from vectorized memory operations
    if (executionProfile.isMemoryIntensive()) {
      improvement += 0.20; // 20% improvement from vectorized memory ops
    }

    // Platform-specific optimizations
    if (enablePlatformSpecific) {
      improvement += 0.15; // 15% from platform optimizations
    }

    // Cost model adjustments
    switch (costModel) {
      case AGGRESSIVE:
        improvement *= 1.20; // 20% bonus for aggressive vectorization
        break;
      case TARGET_SPECIFIC:
        improvement *= 1.10; // 10% bonus for target-specific optimization
        break;
      case BASIC:
        improvement *= 0.90; // 10% penalty for basic cost model
        break;
    }

    return Math.min(improvement, 4.0); // Cap at 300% improvement (400% of original)
  }

  @Override
  public boolean isApplicable(final ExecutionProfile profile) {
    // Vectorization is beneficial for:
    // 1. Code with existing vector operations
    // 2. Loops (auto-vectorization opportunities)
    // 3. Compute-intensive workloads
    // 4. Memory-intensive workloads (vectorized memory operations)
    return profile.hasVectorOperations() ||
           profile.hasLoops() ||
           profile.isComputeIntensive() ||
           profile.isMemoryIntensive();
  }

  @Override
  public Map<String, String> getCraneliftFlags() {
    final Map<String, String> flags = new java.util.HashMap<>();

    flags.put("enable_vectorization", "true");
    flags.put("vector_width", String.valueOf(vectorWidth));

    if (enableAutoVectorization) {
      flags.put("enable_auto_vectorization", "true");
      flags.put("enable_loop_vectorization", "true");
    }

    if (enableSlpVectorization) {
      flags.put("enable_slp_vectorization", "true");
    }

    if (enableMaskedVectorization) {
      flags.put("enable_masked_vectorization", "true");
    }

    // Cost model configuration
    switch (costModel) {
      case BASIC:
        flags.put("vector_cost_model", "basic");
        break;
      case TARGET_SPECIFIC:
        flags.put("vector_cost_model", "target_specific");
        break;
      case AGGRESSIVE:
        flags.put("vector_cost_model", "aggressive");
        flags.put("aggressive_vectorization", "true");
        break;
    }

    if (enablePlatformSpecific) {
      flags.put("enable_platform_specific_vectors", "true");

      // Platform-specific flags
      final String arch = System.getProperty("os.arch").toLowerCase();
      if (arch.contains("x86") || arch.contains("amd64")) {
        flags.put("enable_sse_optimization", "true");
        flags.put("enable_avx_optimization", "true");
        if (vectorWidth >= 512) {
          flags.put("enable_avx512_optimization", "true");
        }
      } else if (arch.contains("aarch64") || arch.contains("arm")) {
        flags.put("enable_neon_optimization", "true");
        if (vectorWidth >= 256) {
          flags.put("enable_sve_optimization", "true");
        }
      }
    }

    return flags;
  }

  @Override
  public List<String> getDependencies() {
    // Vectorization works best after other optimizations
    return List.of("loop_optimization", "dead_code_elimination");
  }

  @Override
  public List<String> getConflicts() {
    // May conflict with some scalar optimizations
    return List.of();
  }

  /**
   * Creates aggressive vectorization for high-performance computing.
   *
   * @return aggressive vectorization optimization
   */
  public static VectorizationOptimization aggressive() {
    return new VectorizationOptimization(512, true, true, true,
                                         VectorCostModel.AGGRESSIVE, true);
  }

  /**
   * Creates AVX2-optimized vectorization for x86-64.
   *
   * @return AVX2-optimized vectorization
   */
  public static VectorizationOptimization avx2Optimized() {
    return new VectorizationOptimization(256, true, true, false,
                                         VectorCostModel.TARGET_SPECIFIC, true);
  }

  /**
   * Creates NEON-optimized vectorization for ARM64.
   *
   * @return NEON-optimized vectorization
   */
  public static VectorizationOptimization neonOptimized() {
    return new VectorizationOptimization(128, true, true, false,
                                         VectorCostModel.TARGET_SPECIFIC, true);
  }

  /**
   * Creates conservative vectorization for compatibility.
   *
   * @return conservative vectorization
   */
  public static VectorizationOptimization conservative() {
    return new VectorizationOptimization(128, true, false, false,
                                         VectorCostModel.BASIC, false);
  }

  public int getVectorWidth() {
    return vectorWidth;
  }

  public boolean isAutoVectorizationEnabled() {
    return enableAutoVectorization;
  }

  public boolean isSlpVectorizationEnabled() {
    return enableSlpVectorization;
  }

  public boolean isMaskedVectorizationEnabled() {
    return enableMaskedVectorization;
  }

  public VectorCostModel getCostModel() {
    return costModel;
  }

  public boolean isPlatformSpecificEnabled() {
    return enablePlatformSpecific;
  }

  @Override
  public String toString() {
    return String.format("%s (width=%d, auto=%b, slp=%b, masked=%b, cost=%s, platform=%b)",
                         super.toString(), vectorWidth, enableAutoVectorization,
                         enableSlpVectorization, enableMaskedVectorization,
                         costModel, enablePlatformSpecific);
  }
}