package ai.tegmentum.wasmtime4j.performance;

import java.io.Closeable;
import java.util.List;
import java.util.Map;

/**
 * Interface for comprehensive WebAssembly execution profiling.
 *
 * <p>WasmProfiler provides detailed profiling capabilities including function-level execution
 * analysis, memory allocation profiling, call stack sampling, and performance bottleneck
 * identification.
 *
 * <p>Profiling is more detailed than monitoring but has higher overhead. It should be used for
 * development, debugging, and optimization analysis rather than production monitoring.
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * try (WasmProfiler profiler = engine.createProfiler()) {
 *   ProfilingOptions options = ProfilingOptions.builder()
 *       .enableFunctionProfiling(true)
 *       .enableMemoryProfiling(true)
 *       .samplingInterval(Duration.ofMillis(1))
 *       .build();
 *
 *   profiler.startProfiling(options);
 *
 *   // Execute WebAssembly operations
 *   instance.getFunction("complexCalculation").call();
 *
 *   ProfileReport report = profiler.stopProfiling();
 *   System.out.println("Function profiles: " + report.getFunctionProfiles());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasmProfiler extends Closeable {

  /**
   * Starts profiling with the specified options.
   *
   * <p>Profiling will collect detailed execution data according to the provided configuration.
   * Multiple profiling sessions can be started and stopped to profile different execution phases.
   *
   * @param options profiling configuration options
   * @throws IllegalStateException if profiling is already active
   * @throws IllegalArgumentException if options is null
   */
  void startProfiling(final ProfilingOptions options);

  /**
   * Stops profiling and generates a comprehensive profile report.
   *
   * <p>This finalizes data collection and analysis, returning detailed profiling results. After
   * stopping, profiling can be restarted with new options if needed.
   *
   * @return comprehensive profiling report
   * @throws IllegalStateException if profiling is not active
   */
  ProfileReport stopProfiling();

  /**
   * Checks if profiling is currently active.
   *
   * @return true if profiling is active, false otherwise
   */
  boolean isProfiling();

  /**
   * Gets function-level execution profiles for the current or last profiling session.
   *
   * <p>Function profiles include execution times, call counts, and performance characteristics for
   * each WebAssembly function.
   *
   * @return map of function names to their execution profiles
   * @throws IllegalStateException if no profiling data is available
   */
  Map<String, FunctionProfile> getFunctionProfiles();

  /**
   * Gets call stack samples collected during profiling.
   *
   * <p>Call stack samples provide insight into execution patterns and help identify performance
   * bottlenecks in complex call hierarchies.
   *
   * @return list of sampled call stacks
   * @throws IllegalStateException if no profiling data is available
   */
  List<CallStack> getSampleCallStacks();

  /**
   * Gets memory allocation profiles for the profiling session.
   *
   * <p>Memory profiles include allocation patterns, sizes, and timing information for both
   * WebAssembly linear memory and native allocations.
   *
   * @return memory allocation profiling data
   * @throws IllegalStateException if no profiling data is available
   */
  MemoryProfile getMemoryProfile();

  /**
   * Gets JIT compilation profiling data.
   *
   * <p>Compilation profiles include timing, optimization levels, and code generation statistics for
   * JIT compilation events during execution.
   *
   * @return JIT compilation profiling data
   * @throws IllegalStateException if no profiling data is available
   */
  CompilationProfile getCompilationProfile();

  /**
   * Gets host function interaction profiles.
   *
   * <p>Host function profiles include call frequencies, execution times, and overhead analysis for
   * calls between WebAssembly and host functions.
   *
   * @return host function interaction profiles
   * @throws IllegalStateException if no profiling data is available
   */
  HostFunctionProfile getHostFunctionProfile();

  /**
   * Adds a custom profiling marker at the current execution point.
   *
   * <p>Custom markers help correlate profiling data with application-specific events or execution
   * phases.
   *
   * @param markerName descriptive name for the marker
   * @param metadata optional metadata associated with the marker
   * @throws IllegalArgumentException if markerName is null or empty
   */
  void addMarker(final String markerName, final Map<String, Object> metadata);

  /**
   * Adds a simple profiling marker with just a name.
   *
   * @param markerName descriptive name for the marker
   * @throws IllegalArgumentException if markerName is null or empty
   */
  void addMarker(final String markerName);

  /**
   * Gets all profiling markers added during the session.
   *
   * @return list of profiling markers in chronological order
   */
  List<ProfilingMarker> getMarkers();

  /**
   * Resets all collected profiling data while maintaining current configuration.
   *
   * <p>This allows starting fresh data collection without recreating the profiler. Profiling state
   * (active/inactive) is preserved.
   */
  void reset();

  /**
   * Gets the current profiling options configuration.
   *
   * @return current profiling options, or null if not configured
   */
  ProfilingOptions getCurrentOptions();

  /**
   * Gets real-time profiling statistics without stopping profiling.
   *
   * <p>This provides a snapshot of current profiling progress and basic statistics without the
   * overhead of generating a full report.
   *
   * @return current profiling statistics
   * @throws IllegalStateException if profiling is not active
   */
  ProfilingStatistics getCurrentStatistics();

  /**
   * Sets the maximum amount of profiling data to retain.
   *
   * <p>This helps control memory usage during long profiling sessions by limiting the amount of
   * historical data kept in memory.
   *
   * @param maxDataPoints maximum number of data points to retain
   * @throws IllegalArgumentException if maxDataPoints is negative
   */
  void setMaxDataRetention(final int maxDataPoints);

  /**
   * Gets the current maximum data retention setting.
   *
   * @return maximum data points retained
   */
  int getMaxDataRetention();

  /**
   * Exports profiling data in the specified format.
   *
   * <p>Supported formats may include standard profiling formats for integration with external
   * analysis tools.
   *
   * @param format export format specification
   * @return exported profiling data
   * @throws IllegalArgumentException if format is not supported
   * @throws IllegalStateException if no profiling data is available
   */
  String exportData(final ProfilingDataFormat format);
}
