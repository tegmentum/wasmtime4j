package ai.tegmentum.wasmtime4j.compilation;

import java.util.List;
import java.util.Map;

/**
 * Dead code elimination optimization strategy.
 *
 * <p>Removes unreachable code, unused variables, and redundant computations
 * to reduce module size and improve performance. This optimization is beneficial
 * for all types of modules and has low compilation overhead.
 *
 * @since 1.0.0
 */
public final class DeadCodeEliminationOptimization extends BaseOptimizationStrategy {

  private final boolean enableAggressiveElimination;
  private final boolean enableUnusedFunctionElimination;
  private final boolean enableRedundantLoadElimination;

  /**
   * Creates a dead code elimination optimization with default parameters.
   */
  public DeadCodeEliminationOptimization() {
    this(true, true, true);
  }

  /**
   * Creates a dead code elimination optimization with custom parameters.
   *
   * @param enableAggressiveElimination enable aggressive elimination passes
   * @param enableUnusedFunctionElimination eliminate unused functions
   * @param enableRedundantLoadElimination eliminate redundant memory loads
   */
  public DeadCodeEliminationOptimization(final boolean enableAggressiveElimination,
                                         final boolean enableUnusedFunctionElimination,
                                         final boolean enableRedundantLoadElimination) {
    super("dead_code_elimination",
          "Remove unreachable code, unused variables, and redundant computations",
          4);
    this.enableAggressiveElimination = enableAggressiveElimination;
    this.enableUnusedFunctionElimination = enableUnusedFunctionElimination;
    this.enableRedundantLoadElimination = enableRedundantLoadElimination;
  }

  @Override
  public double estimateCompilationOverhead(final long moduleSize) {
    // DCE has very low overhead - it mostly involves analysis passes
    // Overhead scales slightly with module size due to more code to analyze
    final double sizeModifier = Math.min(moduleSize / (2.0 * 1024 * 1024), 1.5); // Up to 1.5x for large modules
    return 1.0 + (0.05 * sizeModifier); // 5% base overhead, scaled by size
  }

  @Override
  public double estimatePerformanceImprovement(final ExecutionProfile executionProfile) {
    if (!isApplicable(executionProfile)) {
      return 1.0; // No improvement if not applicable
    }

    double improvement = 1.0;

    // Base improvement from code size reduction
    improvement += 0.05; // 5% base improvement

    // Higher improvement for larger modules (more dead code likely)
    if (executionProfile.getModuleSize() > 1024 * 1024) { // 1MB+
      improvement += 0.08; // 8% additional improvement
    }

    // Higher improvement for modules with many functions (more unused code)
    if (executionProfile.getFunctionCount() > 20) {
      improvement += 0.06; // 6% additional improvement
    }

    // Memory-intensive workloads benefit from redundant load elimination
    if (enableRedundantLoadElimination && executionProfile.isMemoryIntensive()) {
      improvement += 0.10; // 10% additional improvement
    }

    // Hot paths benefit more from cleaner code
    if (executionProfile.isHotPath()) {
      improvement += 0.04; // 4% additional improvement
    }

    return Math.min(improvement, 1.3); // Cap at 30% improvement
  }

  @Override
  public boolean isApplicable(final ExecutionProfile profile) {
    // DCE is applicable to virtually all modules
    // Even small modules can benefit from redundant code removal
    return true;
  }

  @Override
  public Map<String, String> getCraneliftFlags() {
    final Map<String, String> flags = new java.util.HashMap<>();

    flags.put("enable_dce", "true");
    flags.put("enable_unreachable_code_elimination", "true");

    if (enableAggressiveElimination) {
      flags.put("aggressive_dce", "true");
      flags.put("enable_global_dce", "true");
    }

    if (enableUnusedFunctionElimination) {
      flags.put("eliminate_unused_functions", "true");
    }

    if (enableRedundantLoadElimination) {
      flags.put("eliminate_redundant_loads", "true");
      flags.put("enable_load_store_optimization", "true");
    }

    return flags;
  }

  @Override
  public List<String> getDependencies() {
    // DCE should run early, but after basic analysis
    return List.of();
  }

  @Override
  public List<String> getConflicts() {
    // No conflicts - DCE is complementary to other optimizations
    return List.of();
  }

  /**
   * Creates an aggressive dead code elimination configuration.
   *
   * @return aggressive DCE optimization
   */
  public static DeadCodeEliminationOptimization aggressive() {
    return new DeadCodeEliminationOptimization(true, true, true);
  }

  /**
   * Creates a conservative dead code elimination configuration.
   *
   * @return conservative DCE optimization
   */
  public static DeadCodeEliminationOptimization conservative() {
    return new DeadCodeEliminationOptimization(false, false, true);
  }

  /**
   * Creates a memory-focused dead code elimination configuration.
   *
   * @return memory-focused DCE optimization
   */
  public static DeadCodeEliminationOptimization memoryFocused() {
    return new DeadCodeEliminationOptimization(true, true, true);
  }

  public boolean isAggressiveEliminationEnabled() {
    return enableAggressiveElimination;
  }

  public boolean isUnusedFunctionEliminationEnabled() {
    return enableUnusedFunctionElimination;
  }

  public boolean isRedundantLoadEliminationEnabled() {
    return enableRedundantLoadElimination;
  }

  @Override
  public String toString() {
    return String.format("%s (aggressive=%b, unusedFunc=%b, redundantLoad=%b)",
                         super.toString(), enableAggressiveElimination,
                         enableUnusedFunctionElimination, enableRedundantLoadElimination);
  }
}