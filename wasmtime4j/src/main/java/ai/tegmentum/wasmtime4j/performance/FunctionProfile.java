package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed performance profile for a WebAssembly function.
 *
 * <p>FunctionProfile provides comprehensive execution statistics and performance analysis
 * for individual WebAssembly functions including timing, call patterns, memory usage,
 * and optimization opportunities.
 *
 * @since 1.0.0
 */
public interface FunctionProfile {

  /**
   * Gets the name of the profiled function.
   *
   * <p>This is the WebAssembly function name as exported or defined in the module.
   *
   * @return function name
   */
  String getFunctionName();

  /**
   * Gets the module name where this function is defined.
   *
   * @return module name, or null if not available
   */
  String getModuleName();

  /**
   * Gets the total number of times this function was called.
   *
   * <p>This includes both direct calls and calls through function pointers or tables.
   *
   * @return total call count
   */
  long getCallCount();

  /**
   * Gets the total execution time spent in this function.
   *
   * <p>This includes all calls to the function accumulated over the profiling session.
   * It represents actual CPU time spent executing the function's instructions.
   *
   * @return total execution time
   */
  Duration getTotalTime();

  /**
   * Gets the average execution time per function call.
   *
   * <p>This is calculated as total time divided by call count and provides insight
   * into typical function performance.
   *
   * @return average execution time per call
   */
  Duration getAverageTime();

  /**
   * Gets the minimum execution time recorded for a single call.
   *
   * <p>This represents the fastest execution of this function during profiling.
   *
   * @return minimum execution time
   */
  Duration getMinTime();

  /**
   * Gets the maximum execution time recorded for a single call.
   *
   * <p>This represents the slowest execution of this function during profiling.
   *
   * @return maximum execution time
   */
  Duration getMaxTime();

  /**
   * Gets the median execution time for function calls.
   *
   * <p>The median provides a measure of typical performance that is less affected
   * by outliers than the average.
   *
   * @return median execution time
   */
  Duration getMedianTime();

  /**
   * Gets the 95th percentile execution time.
   *
   * <p>This indicates that 95% of function calls completed faster than this time.
   *
   * @return 95th percentile execution time
   */
  Duration getP95Time();

  /**
   * Gets the 99th percentile execution time.
   *
   * <p>This indicates that 99% of function calls completed faster than this time.
   *
   * @return 99th percentile execution time
   */
  Duration getP99Time();

  /**
   * Gets the total memory allocated by this function.
   *
   * <p>This includes both WebAssembly linear memory allocations and any native
   * memory allocations made during function execution.
   *
   * @return total memory usage in bytes
   */
  long getMemoryUsage();

  /**
   * Gets the average memory allocation per function call.
   *
   * @return average memory allocation in bytes per call
   */
  long getAverageMemoryPerCall();

  /**
   * Gets the peak memory usage recorded during a single function call.
   *
   * @return peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the number of WebAssembly instructions executed by this function.
   *
   * <p>This represents the total instruction count across all calls to the function.
   *
   * @return total instruction count
   */
  long getInstructionCount();

  /**
   * Gets the average instructions per second execution rate.
   *
   * <p>This provides a measure of execution efficiency for the function.
   *
   * @return instructions per second
   */
  double getInstructionsPerSecond();

  /**
   * Gets the call frequency (calls per second) for this function.
   *
   * <p>This indicates how often the function is invoked during execution.
   *
   * @return calls per second
   */
  double getCallFrequency();

  /**
   * Gets the percentage of total execution time spent in this function.
   *
   * <p>This helps identify functions that consume the most execution time.
   *
   * @return percentage of total execution time (0.0 to 100.0)
   */
  double getExecutionTimePercentage();

  /**
   * Gets the number of unique callers of this function.
   *
   * <p>This provides insight into function usage patterns and call graph complexity.
   *
   * @return unique caller count
   */
  int getUniqueCallerCount();

  /**
   * Gets detailed statistics about the callers of this function.
   *
   * <p>The map contains caller function names as keys and call counts as values.
   *
   * @return map of caller functions to call counts
   */
  Map<String, Long> getCallerStatistics();

  /**
   * Gets statistics about functions called by this function.
   *
   * <p>The map contains called function names as keys and call counts as values.
   *
   * @return map of called functions to call counts
   */
  Map<String, Long> getCalleeStatistics();

  /**
   * Gets the timestamp of the first call to this function.
   *
   * @return first call timestamp
   */
  Instant getFirstCallTime();

  /**
   * Gets the timestamp of the most recent call to this function.
   *
   * @return last call timestamp
   */
  Instant getLastCallTime();

  /**
   * Gets JIT compilation information for this function.
   *
   * <p>This includes compilation times, optimization levels, and code generation statistics.
   *
   * @return JIT compilation info, or null if not compiled or not available
   */
  JitCompilationInfo getJitInfo();

  /**
   * Gets source location information for this function if available.
   *
   * <p>Source locations help correlate profiling data with original source code.
   *
   * @return source location info, or null if not available
   */
  SourceLocationInfo getSourceLocation();

  /**
   * Gets performance anomalies detected for this function.
   *
   * <p>Anomalies include unusual execution times, memory patterns, or other
   * performance irregularities that may indicate optimization opportunities.
   *
   * @return list of detected performance anomalies
   */
  List<PerformanceAnomaly> getAnomalies();

  /**
   * Gets optimization recommendations for this function.
   *
   * <p>Based on profiling data, provides suggestions for improving function performance.
   *
   * @return list of optimization recommendations
   */
  List<OptimizationRecommendation> getOptimizationRecommendations();

  /**
   * Gets execution time distribution histogram.
   *
   * <p>The histogram shows the distribution of execution times across different
   * time ranges, helping identify performance patterns.
   *
   * @return execution time histogram
   */
  ExecutionTimeHistogram getTimeHistogram();

  /**
   * Gets the recursion depth statistics for this function.
   *
   * <p>For recursive functions, provides information about recursion patterns
   * and depth distribution.
   *
   * @return recursion statistics, or null if function is not recursive
   */
  RecursionStatistics getRecursionStatistics();

  /**
   * Gets custom metadata associated with this function profile.
   *
   * <p>This includes any additional profiling data specific to the function
   * or implementation-specific metrics.
   *
   * @return map of custom metadata
   */
  Map<String, Object> getCustomMetadata();

  /**
   * Creates a summary string representation of this function profile.
   *
   * <p>The summary includes key performance metrics in a human-readable format.
   *
   * @return formatted function profile summary
   */
  String getSummary();

  /**
   * Compares this function profile with another for performance regression analysis.
   *
   * @param other function profile to compare with
   * @return comparison analysis
   * @throws IllegalArgumentException if other profile is for a different function
   */
  FunctionProfileComparison compareWith(final FunctionProfile other);
}