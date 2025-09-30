package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for streaming compilation optimization.
 *
 * <p>StreamingOptimizer provides advanced optimization techniques for streaming WebAssembly
 * compilation, including hot function prioritization, cold code deferral, and profile-guided
 * compilation ordering.
 *
 * @since 1.0.0
 */
public interface StreamingOptimizer extends AutoCloseable {

  /**
   * Analyzes a WebAssembly module to identify hot functions for prioritization.
   *
   * <p>This method examines the module structure, call graphs, and any available profiling data
   * to determine which functions should be compiled with high priority.
   *
   * @param module the module to analyze
   * @return a CompletableFuture that completes with hot function analysis results
   * @throws IllegalArgumentException if module is null
   */
  CompletableFuture<HotFunctionAnalysis> analyzeHotFunctions(Module module);

  /**
   * Creates an optimized compilation plan for streaming compilation.
   *
   * <p>This method generates a detailed plan that specifies the order and priority for compiling
   * different parts of the module based on optimization criteria.
   *
   * @param module the module to create a plan for
   * @param config optimization configuration
   * @return a CompletableFuture that completes with an optimized compilation plan
   * @throws IllegalArgumentException if module or config is null
   */
  CompletableFuture<OptimizedCompilationPlan> createCompilationPlan(
      Module module, OptimizationConfig config);

  /**
   * Applies hot function prioritization to a streaming compilation.
   *
   * <p>This method modifies the compilation order to prioritize functions identified as hot,
   * potentially improving application startup time and responsiveness.
   *
   * @param compiler the streaming compiler to optimize
   * @param hotFunctions list of hot function identifiers
   * @return a CompletableFuture that completes when optimization is applied
   * @throws IllegalArgumentException if compiler or hotFunctions is null
   */
  CompletableFuture<Void> applyHotFunctionPrioritization(
      StreamingCompiler compiler, List<String> hotFunctions);

  /**
   * Applies cold code deferral optimization.
   *
   * <p>This method defers compilation of rarely used functions, allowing hot code to compile
   * first and reducing initial compilation time.
   *
   * @param compiler the streaming compiler to optimize
   * @param coldFunctions list of cold function identifiers
   * @return a CompletableFuture that completes when optimization is applied
   * @throws IllegalArgumentException if compiler or coldFunctions is null
   */
  CompletableFuture<Void> applyColdCodeDeferral(
      StreamingCompiler compiler, List<String> coldFunctions);

  /**
   * Updates optimization strategies based on runtime profiling data.
   *
   * <p>This method incorporates runtime performance data to improve future optimization decisions
   * for similar modules.
   *
   * @param profilingData runtime profiling data collected during execution
   * @return a CompletableFuture that completes when optimization strategies are updated
   * @throws IllegalArgumentException if profilingData is null
   */
  CompletableFuture<Void> updateOptimizationStrategies(ProfilingData profilingData);

  /**
   * Creates a memory-efficient streaming compilation configuration.
   *
   * <p>This method generates a configuration that minimizes memory usage during streaming
   * compilation while maintaining reasonable performance.
   *
   * @param memoryBudget maximum memory budget in bytes
   * @return optimized streaming configuration for memory efficiency
   * @throws IllegalArgumentException if memoryBudget is not positive
   */
  StreamingConfig createMemoryEfficientConfig(long memoryBudget);

  /**
   * Creates a speed-optimized streaming compilation configuration.
   *
   * <p>This method generates a configuration that prioritizes compilation speed, potentially
   * using more memory and system resources.
   *
   * @param availableCores number of CPU cores available for compilation
   * @return optimized streaming configuration for compilation speed
   * @throws IllegalArgumentException if availableCores is not positive
   */
  StreamingConfig createSpeedOptimizedConfig(int availableCores);

  /**
   * Gets optimization statistics and performance metrics.
   *
   * @return current optimization statistics
   */
  OptimizationStatistics getStatistics();

  /**
   * Registers an optimization event listener.
   *
   * @param listener the optimization event listener
   * @throws IllegalArgumentException if listener is null
   */
  void addOptimizationListener(OptimizationListener listener);

  /**
   * Removes a previously registered optimization event listener.
   *
   * @param listener the optimization event listener to remove
   * @throws IllegalArgumentException if listener is null
   */
  void removeOptimizationListener(OptimizationListener listener);

  /**
   * Closes the streaming optimizer and releases associated resources.
   */
  @Override
  void close();
}