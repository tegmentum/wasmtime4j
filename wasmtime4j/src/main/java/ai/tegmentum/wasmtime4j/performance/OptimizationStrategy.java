package ai.tegmentum.wasmtime4j.performance;

/**
 * Represents different optimization strategies that can be applied to WebAssembly modules.
 *
 * <p>Optimization strategies provide fine-grained control over which optimizations
 * are applied, allowing developers to customize the optimization process based on
 * specific requirements and constraints.
 *
 * @since 1.0.0
 */
public enum OptimizationStrategy {

  /**
   * Dead code elimination - removes unreachable code.
   *
   * <p>Safe optimization that reduces module size and improves performance
   * by eliminating code that cannot be executed.
   */
  DEAD_CODE_ELIMINATION("Dead Code Elimination", OptimizationCategory.CODE_SIZE, OptimizationSafety.SAFE),

  /**
   * Function inlining - inlines small frequently called functions.
   *
   * <p>Reduces function call overhead but may increase code size.
   * Most effective for small, frequently called functions.
   */
  FUNCTION_INLINING("Function Inlining", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.SAFE),

  /**
   * Loop optimization - optimizes loop structures and iterations.
   *
   * <p>Includes loop unrolling, vectorization, and strength reduction.
   * Can significantly improve performance for computation-heavy code.
   */
  LOOP_OPTIMIZATION("Loop Optimization", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.MODERATE),

  /**
   * Constant propagation - replaces variables with their constant values.
   *
   * <p>Eliminates unnecessary variable lookups and enables further optimizations.
   * Safe optimization with moderate performance impact.
   */
  CONSTANT_PROPAGATION("Constant Propagation", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.SAFE),

  /**
   * Register allocation optimization - optimizes variable to register mapping.
   *
   * <p>Improves performance by reducing memory accesses and optimizing
   * register usage patterns.
   */
  REGISTER_ALLOCATION("Register Allocation", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.SAFE),

  /**
   * Memory access optimization - optimizes memory load/store patterns.
   *
   * <p>Reduces memory access overhead through caching, prefetching,
   * and access pattern optimization.
   */
  MEMORY_ACCESS_OPTIMIZATION("Memory Access Optimization", OptimizationCategory.MEMORY_EFFICIENCY, OptimizationSafety.MODERATE),

  /**
   * Instruction scheduling - reorders instructions for better pipeline utilization.
   *
   * <p>Improves CPU pipeline efficiency by reordering instructions
   * to minimize stalls and maximize throughput.
   */
  INSTRUCTION_SCHEDULING("Instruction Scheduling", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.MODERATE),

  /**
   * Branch prediction optimization - optimizes branch patterns.
   *
   * <p>Improves branch prediction accuracy and reduces branch misprediction penalties.
   */
  BRANCH_OPTIMIZATION("Branch Optimization", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.MODERATE),

  /**
   * Vectorization - converts scalar operations to vector operations.
   *
   * <p>Leverages SIMD instructions for improved performance on vectorizable code.
   * Requires careful analysis to ensure correctness.
   */
  VECTORIZATION("Vectorization", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.AGGRESSIVE),

  /**
   * Code motion - moves invariant code out of loops.
   *
   * <p>Improves performance by eliminating redundant computations
   * within loop iterations.
   */
  CODE_MOTION("Code Motion", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.SAFE),

  /**
   * Strength reduction - replaces expensive operations with cheaper equivalents.
   *
   * <p>Replaces multiplication with addition, division with shifts, etc.
   */
  STRENGTH_REDUCTION("Strength Reduction", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.SAFE),

  /**
   * Common subexpression elimination - eliminates redundant computations.
   *
   * <p>Identifies and eliminates repeated calculations to reduce
   * computational overhead.
   */
  COMMON_SUBEXPRESSION_ELIMINATION("Common Subexpression Elimination", OptimizationCategory.EXECUTION_SPEED, OptimizationSafety.SAFE),

  /**
   * Memory layout optimization - optimizes data structure layout.
   *
   * <p>Improves cache performance through better data locality
   * and memory access patterns.
   */
  MEMORY_LAYOUT_OPTIMIZATION("Memory Layout Optimization", OptimizationCategory.MEMORY_EFFICIENCY, OptimizationSafety.MODERATE),

  /**
   * JIT compilation hints - provides hints for runtime optimization.
   *
   * <p>Guides JIT compiler optimization decisions based on
   * static analysis and profiling data.
   */
  JIT_HINTS("JIT Compilation Hints", OptimizationCategory.COMPILATION_SPEED, OptimizationSafety.SAFE);

  private final String displayName;
  private final OptimizationCategory category;
  private final OptimizationSafety safety;

  OptimizationStrategy(final String displayName, final OptimizationCategory category, final OptimizationSafety safety) {
    this.displayName = displayName;
    this.category = category;
    this.safety = safety;
  }

  /**
   * Gets the human-readable display name for this optimization strategy.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the optimization category for this strategy.
   *
   * @return optimization category
   */
  public OptimizationCategory getCategory() {
    return category;
  }

  /**
   * Gets the safety level of this optimization strategy.
   *
   * @return optimization safety level
   */
  public OptimizationSafety getSafety() {
    return safety;
  }

  /**
   * Checks if this optimization is safe to apply without detailed analysis.
   *
   * @return true if this optimization is considered safe
   */
  public boolean isSafe() {
    return safety == OptimizationSafety.SAFE;
  }

  /**
   * Checks if this optimization requires aggressive analysis or may have risks.
   *
   * @return true if this optimization is aggressive
   */
  public boolean isAggressive() {
    return safety == OptimizationSafety.AGGRESSIVE;
  }

  /**
   * Gets the estimated performance impact of this optimization.
   *
   * @return impact level (1 = minimal, 5 = significant)
   */
  public int getEstimatedImpact() {
    switch (this) {
      case DEAD_CODE_ELIMINATION:
      case CONSTANT_PROPAGATION:
      case CODE_MOTION:
      case STRENGTH_REDUCTION:
        return 2;
      case FUNCTION_INLINING:
      case REGISTER_ALLOCATION:
      case COMMON_SUBEXPRESSION_ELIMINATION:
      case INSTRUCTION_SCHEDULING:
        return 3;
      case LOOP_OPTIMIZATION:
      case MEMORY_ACCESS_OPTIMIZATION:
      case BRANCH_OPTIMIZATION:
      case MEMORY_LAYOUT_OPTIMIZATION:
        return 4;
      case VECTORIZATION:
        return 5;
      case JIT_HINTS:
        return 1;
      default:
        return 3;
    }
  }
}