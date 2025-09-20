package ai.tegmentum.wasmtime4j.component;

import java.time.Duration;
import java.util.Map;

/**
 * Detailed performance metrics for component execution.
 *
 * <p>ComponentPerformanceMetrics provides comprehensive performance analysis including throughput,
 * latency distributions, optimization effectiveness, and resource utilization metrics. This data
 * enables performance monitoring, bottleneck identification, and optimization guidance.
 *
 * @since 1.0.0
 */
public interface ComponentPerformanceMetrics {

  /**
   * Gets the current throughput in operations per second.
   *
   * <p>Returns the recent throughput measured over a sliding time window for all component
   * operations.
   *
   * @return current throughput in operations per second
   */
  double getCurrentThroughput();

  /**
   * Gets the peak throughput achieved.
   *
   * <p>Returns the highest throughput rate observed during the component instance's lifetime.
   *
   * @return peak throughput in operations per second
   */
  double getPeakThroughput();

  /**
   * Gets the average throughput over the component's lifetime.
   *
   * <p>Returns the mean throughput calculated across the entire execution period.
   *
   * @return average throughput in operations per second
   */
  double getAverageThroughput();

  /**
   * Gets the 50th percentile (median) latency.
   *
   * <p>Returns the median execution time for component operations.
   *
   * @return 50th percentile latency
   */
  Duration getLatencyP50();

  /**
   * Gets the 90th percentile latency.
   *
   * <p>Returns the latency below which 90% of operations complete.
   *
   * @return 90th percentile latency
   */
  Duration getLatencyP90();

  /**
   * Gets the 95th percentile latency.
   *
   * <p>Returns the latency below which 95% of operations complete.
   *
   * @return 95th percentile latency
   */
  Duration getLatencyP95();

  /**
   * Gets the 99th percentile latency.
   *
   * <p>Returns the latency below which 99% of operations complete.
   *
   * @return 99th percentile latency
   */
  Duration getLatencyP99();

  /**
   * Gets the CPU utilization percentage.
   *
   * <p>Returns the percentage of CPU time used by this component instance relative to available
   * CPU resources.
   *
   * @return CPU utilization percentage (0.0 to 100.0)
   */
  double getCpuUtilization();

  /**
   * Gets the memory efficiency ratio.
   *
   * <p>Returns a metric indicating how efficiently the component uses allocated memory, with
   * higher values indicating better efficiency.
   *
   * @return memory efficiency ratio (0.0 to 1.0)
   */
  double getMemoryEfficiency();

  /**
   * Gets the cache hit rate.
   *
   * <p>Returns the percentage of memory accesses that hit in caches (instruction, data, or
   * component-specific caches).
   *
   * @return cache hit rate percentage (0.0 to 100.0)
   */
  double getCacheHitRate();

  /**
   * Gets the optimization effectiveness score.
   *
   * <p>Returns a metric indicating how well runtime optimizations are performing for this
   * component, with higher scores indicating more effective optimization.
   *
   * @return optimization effectiveness score (0.0 to 100.0)
   */
  double getOptimizationEffectiveness();

  /**
   * Gets branch prediction accuracy.
   *
   * <p>Returns the percentage of branch predictions that were correct during component execution.
   *
   * @return branch prediction accuracy percentage (0.0 to 100.0)
   */
  double getBranchPredictionAccuracy();

  /**
   * Gets instruction-level parallelism utilization.
   *
   * <p>Returns a metric indicating how well the component utilizes available instruction-level
   * parallelism in the CPU.
   *
   * @return ILP utilization ratio (0.0 to 1.0)
   */
  double getInstructionLevelParallelism();

  /**
   * Gets the garbage collection overhead percentage.
   *
   * <p>Returns the percentage of execution time spent in garbage collection operations related
   * to this component.
   *
   * @return GC overhead percentage (0.0 to 100.0)
   */
  double getGarbageCollectionOverhead();

  /**
   * Gets performance metrics by operation type.
   *
   * <p>Returns detailed performance metrics broken down by different types of operations
   * (function calls, resource operations, interface calls, etc.).
   *
   * @return map of operation types to their performance metrics
   */
  Map<String, OperationPerformanceMetrics> getMetricsByOperationType();

  /**
   * Gets hot path identification.
   *
   * <p>Returns information about the most frequently executed code paths and their performance
   * characteristics.
   *
   * @return hot path performance information
   */
  HotPathAnalysis getHotPathAnalysis();

  /**
   * Gets resource contention metrics.
   *
   * <p>Returns information about contention for shared resources such as memory, locks, or
   * external resources.
   *
   * @return resource contention metrics
   */
  ResourceContentionMetrics getResourceContentionMetrics();

  /**
   * Gets scaling efficiency metrics.
   *
   * <p>Returns information about how well performance scales with increased load or parallelism.
   *
   * @return scaling efficiency metrics
   */
  ScalingEfficiencyMetrics getScalingEfficiency();

  /**
   * Gets performance regression indicators.
   *
   * <p>Returns metrics that indicate whether performance has degraded over time compared to
   * baseline measurements.
   *
   * @return performance regression analysis
   */
  PerformanceRegressionAnalysis getRegressionAnalysis();

  /**
   * Resets all performance metrics.
   *
   * <p>Clears all accumulated performance data and restarts measurement from the current point.
   * This can be useful for performance testing and benchmarking.
   */
  void reset();
}