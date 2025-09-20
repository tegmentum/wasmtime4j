package ai.tegmentum.wasmtime4j.performance;

import ai.tegmentum.wasmtime4j.Module;
import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Interface for WebAssembly performance optimization and analysis.
 *
 * <p>PerformanceOptimizer provides automated performance analysis, optimization recommendations,
 * and module optimization capabilities. It analyzes execution patterns, identifies bottlenecks,
 * and can apply various optimization techniques to improve performance.
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (PerformanceOptimizer optimizer = engine.createOptimizer()) {
 *   // Analyze module performance characteristics
 *   OptimizationReport report = optimizer.analyzePerformance(module);
 *   System.out.println("Bottlenecks: " + report.getBottlenecks());
 *
 *   // Apply optimizations
 *   Module optimizedModule = optimizer.applyOptimizations(module, OptimizationLevel.AGGRESSIVE);
 *
 *   // Add JIT hints for hot functions
 *   optimizer.addHotFunction("calculatePrimes");
 *   optimizer.addHotFunction("matrixMultiply");
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface PerformanceOptimizer extends Closeable {

  /**
   * Analyzes performance characteristics of a WebAssembly module.
   *
   * <p>Performs static analysis of the module to identify potential performance issues,
   * optimization opportunities, and resource usage patterns.
   *
   * @param module the WebAssembly module to analyze
   * @return comprehensive optimization analysis report
   * @throws IllegalArgumentException if module is null
   */
  OptimizationReport analyzePerformance(final Module module);

  /**
   * Applies performance optimizations to a WebAssembly module.
   *
   * <p>Creates an optimized version of the module with various performance improvements
   * applied according to the specified optimization level.
   *
   * @param module the source module to optimize
   * @param level the optimization level to apply
   * @return optimized module instance
   * @throws IllegalArgumentException if module or level is null
   */
  Module applyOptimizations(final Module module, final OptimizationLevel level);

  /**
   * Applies specific optimization strategies to a module.
   *
   * <p>Allows fine-grained control over which optimizations are applied by specifying
   * individual optimization strategies.
   *
   * @param module the source module to optimize
   * @param strategies list of optimization strategies to apply
   * @return optimized module instance
   * @throws IllegalArgumentException if module or strategies is null
   */
  Module applyOptimizations(final Module module, final List<OptimizationStrategy> strategies);

  /**
   * Adds a function as a hot function for JIT compilation prioritization.
   *
   * <p>Hot functions are prioritized for aggressive optimization and may be compiled
   * with higher optimization levels or compilation hints.
   *
   * @param functionName the name of the hot function
   * @throws IllegalArgumentException if functionName is null or empty
   */
  void addHotFunction(final String functionName);

  /**
   * Removes a function from the hot function list.
   *
   * @param functionName the name of the function to remove
   * @return true if the function was removed, false if it was not in the list
   */
  boolean removeHotFunction(final String functionName);

  /**
   * Gets the list of currently configured hot functions.
   *
   * @return list of hot function names
   */
  List<String> getHotFunctions();

  /**
   * Sets optimization hints for specific functions or code patterns.
   *
   * <p>Optimization hints provide guidance to the JIT compiler about how to optimize
   * specific functions based on expected usage patterns.
   *
   * @param hints map of function/pattern names to optimization hints
   * @throws IllegalArgumentException if hints is null
   */
  void setOptimizationHints(final Map<String, OptimizationHint> hints);

  /**
   * Adds a single optimization hint.
   *
   * @param target the function or pattern name
   * @param hint the optimization hint to apply
   * @throws IllegalArgumentException if target or hint is null
   */
  void addOptimizationHint(final String target, final OptimizationHint hint);

  /**
   * Gets the current optimization hints configuration.
   *
   * @return map of optimization hints
   */
  Map<String, OptimizationHint> getOptimizationHints();

  /**
   * Analyzes runtime performance data to generate optimization recommendations.
   *
   * <p>Uses profiling data and execution statistics to identify optimization opportunities
   * that may not be apparent from static analysis alone.
   *
   * @param performanceData runtime performance data from monitoring or profiling
   * @return list of optimization recommendations
   * @throws IllegalArgumentException if performanceData is null
   */
  List<OptimizationRecommendation> analyzeRuntimePerformance(final RuntimePerformanceData performanceData);

  /**
   * Validates that optimizations maintain functional correctness.
   *
   * <p>Performs validation checks to ensure that applied optimizations do not change
   * the functional behavior of WebAssembly modules.
   *
   * @param originalModule the original unoptimized module
   * @param optimizedModule the optimized module to validate
   * @return validation results
   * @throws IllegalArgumentException if either module is null
   */
  OptimizationValidationResult validateOptimizations(final Module originalModule, final Module optimizedModule);

  /**
   * Gets performance benchmarking results for optimization comparison.
   *
   * <p>Runs standardized benchmarks to compare performance before and after optimization.
   *
   * @param originalModule the original module
   * @param optimizedModule the optimized module
   * @return benchmark comparison results
   * @throws IllegalArgumentException if either module is null
   */
  OptimizationBenchmarkResult benchmarkOptimizations(final Module originalModule, final Module optimizedModule);

  /**
   * Configures automatic optimization based on runtime feedback.
   *
   * <p>Enables adaptive optimization that adjusts optimization strategies based on
   * observed runtime performance characteristics.
   *
   * @param config adaptive optimization configuration
   * @throws IllegalArgumentException if config is null
   */
  void configureAdaptiveOptimization(final AdaptiveOptimizationConfig config);

  /**
   * Checks if adaptive optimization is currently enabled.
   *
   * @return true if adaptive optimization is enabled
   */
  boolean isAdaptiveOptimizationEnabled();

  /**
   * Gets the current adaptive optimization configuration.
   *
   * @return adaptive optimization config, or null if not enabled
   */
  AdaptiveOptimizationConfig getAdaptiveOptimizationConfig();

  /**
   * Exports optimization statistics and configuration.
   *
   * <p>Provides detailed information about applied optimizations, their impact,
   * and current optimizer configuration for analysis and debugging.
   *
   * @return optimization statistics and configuration data
   */
  OptimizerStatistics getStatistics();

  /**
   * Resets optimizer state and clears all configuration.
   *
   * <p>Removes all hot function markers, optimization hints, and adaptive configuration,
   * returning the optimizer to its default state.
   */
  void reset();

  /**
   * Sets the optimization cache for storing compiled optimization results.
   *
   * <p>Caching can improve performance by reusing optimization results for modules
   * with similar characteristics.
   *
   * @param cache optimization cache implementation
   */
  void setOptimizationCache(final OptimizationCache cache);

  /**
   * Gets the current optimization cache.
   *
   * @return optimization cache, or null if not configured
   */
  OptimizationCache getOptimizationCache();

  /**
   * Creates a snapshot of the current optimizer configuration.
   *
   * <p>Snapshots can be used to save and restore optimizer state for reproducible
   * optimization results.
   *
   * @return optimizer configuration snapshot
   */
  OptimizerSnapshot createSnapshot();

  /**
   * Restores optimizer configuration from a snapshot.
   *
   * @param snapshot the configuration snapshot to restore
   * @throws IllegalArgumentException if snapshot is null or invalid
   */
  void restoreSnapshot(final OptimizerSnapshot snapshot);
}