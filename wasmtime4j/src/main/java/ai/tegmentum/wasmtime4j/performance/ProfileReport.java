package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive profiling report generated after profiling completion.
 *
 * <p>ProfileReport aggregates all profiling data collected during a profiling session,
 * providing detailed analysis of execution patterns, performance characteristics,
 * and optimization opportunities.
 *
 * @since 1.0.0
 */
public interface ProfileReport {

  /**
   * Gets the profiling session information.
   *
   * @return profiling session details
   */
  ProfilingSession getSession();

  /**
   * Gets the start time of the profiling session.
   *
   * @return profiling start timestamp
   */
  Instant getStartTime();

  /**
   * Gets the end time of the profiling session.
   *
   * @return profiling end timestamp
   */
  Instant getEndTime();

  /**
   * Gets the total duration of the profiling session.
   *
   * @return profiling duration
   */
  Duration getDuration();

  /**
   * Gets function-level execution profiles.
   *
   * @return map of function names to their execution profiles
   */
  Map<String, FunctionProfile> getFunctionProfiles();

  /**
   * Gets memory allocation profiling data.
   *
   * @return memory allocation profiles
   */
  MemoryProfile getMemoryProfile();

  /**
   * Gets JIT compilation profiling data.
   *
   * @return compilation profiling information
   */
  CompilationProfile getCompilationProfile();

  /**
   * Gets host function interaction profiles.
   *
   * @return host function profiling data
   */
  HostFunctionProfile getHostFunctionProfile();

  /**
   * Gets all sampled call stacks from the profiling session.
   *
   * @return list of call stack samples
   */
  List<CallStack> getCallStacks();

  /**
   * Gets profiling markers added during the session.
   *
   * @return list of profiling markers in chronological order
   */
  List<ProfilingMarker> getMarkers();

  /**
   * Gets overall profiling statistics.
   *
   * @return comprehensive profiling statistics
   */
  ProfilingStatistics getStatistics();

  /**
   * Gets the profiling options that were used for this session.
   *
   * @return profiling configuration options
   */
  ProfilingOptions getOptions();

  /**
   * Gets the top hottest functions by execution time.
   *
   * @param count maximum number of functions to return
   * @return list of hottest functions ordered by execution time
   */
  List<FunctionProfile> getHottestFunctions(final int count);

  /**
   * Gets the functions with the highest memory allocation.
   *
   * @param count maximum number of functions to return
   * @return list of functions ordered by memory allocation
   */
  List<FunctionProfile> getHighestMemoryAllocators(final int count);

  /**
   * Gets the most frequently called functions.
   *
   * @param count maximum number of functions to return
   * @return list of functions ordered by call frequency
   */
  List<FunctionProfile> getMostCalledFunctions(final int count);

  /**
   * Gets performance bottlenecks identified during profiling.
   *
   * @return list of identified performance bottlenecks
   */
  List<PerformanceBottleneck> getBottlenecks();

  /**
   * Gets optimization recommendations based on profiling data.
   *
   * @return list of optimization recommendations
   */
  List<OptimizationRecommendation> getOptimizationRecommendations();

  /**
   * Gets the overhead introduced by profiling itself.
   *
   * @return profiling overhead metrics
   */
  ProfilingOverhead getProfilingOverhead();

  /**
   * Gets execution timeline data showing performance over time.
   *
   * @return execution timeline information
   */
  ExecutionTimeline getTimeline();

  /**
   * Exports the profiling report in the specified format.
   *
   * @param format export format specification
   * @return exported report data
   * @throws IllegalArgumentException if format is not supported
   */
  String exportReport(final ProfilingDataFormat format);

  /**
   * Gets a summary of the profiling report.
   *
   * @return textual summary of key findings
   */
  String getSummary();

  /**
   * Validates the integrity of the profiling data.
   *
   * @return validation results
   */
  ProfilingValidationResult validateData();
}