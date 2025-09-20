package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;

/**
 * Configuration options for WebAssembly profiling.
 *
 * <p>ProfilingOptions controls what data is collected during profiling, sampling intervals,
 * and various profiling behaviors. Different options have different overhead characteristics.
 *
 * @since 1.0.0
 */
public interface ProfilingOptions {

  /**
   * Checks if function-level profiling is enabled.
   *
   * <p>Function profiling tracks execution times, call counts, and performance metrics
   * for individual WebAssembly functions.
   *
   * @return true if function profiling is enabled
   */
  boolean isEnableFunctionProfiling();

  /**
   * Checks if memory allocation profiling is enabled.
   *
   * <p>Memory profiling tracks allocation patterns, sizes, and timing for both
   * WebAssembly linear memory and native allocations.
   *
   * @return true if memory profiling is enabled
   */
  boolean isEnableMemoryProfiling();

  /**
   * Checks if call stack sampling is enabled.
   *
   * <p>Call stack sampling periodically captures execution call stacks to identify
   * performance hotspots and execution patterns.
   *
   * @return true if call stack sampling is enabled
   */
  boolean isEnableCallStackSampling();

  /**
   * Checks if JIT compilation profiling is enabled.
   *
   * <p>Compilation profiling tracks JIT compilation events, timing, and optimization
   * decisions during WebAssembly execution.
   *
   * @return true if compilation profiling is enabled
   */
  boolean isEnableCompilationProfiling();

  /**
   * Checks if host function interaction profiling is enabled.
   *
   * <p>Host function profiling tracks calls between WebAssembly and host functions,
   * including frequency, timing, and overhead analysis.
   *
   * @return true if host function profiling is enabled
   */
  boolean isEnableHostFunctionProfiling();

  /**
   * Checks if instruction-level profiling is enabled.
   *
   * <p>Instruction profiling provides detailed execution statistics at the individual
   * WebAssembly instruction level. This has high overhead but provides maximum detail.
   *
   * @return true if instruction profiling is enabled
   */
  boolean isEnableInstructionProfiling();

  /**
   * Gets the sampling interval for periodic data collection.
   *
   * <p>Shorter intervals provide more detailed data but increase overhead.
   * Longer intervals reduce overhead but may miss short-duration events.
   *
   * @return sampling interval duration
   */
  Duration getSamplingInterval();

  /**
   * Gets the maximum number of samples to collect.
   *
   * <p>This limits memory usage during profiling by capping the amount of data collected.
   * Older samples may be discarded when the limit is reached.
   *
   * @return maximum sample count
   */
  int getMaxSamples();

  /**
   * Gets the minimum execution time threshold for function profiling.
   *
   * <p>Functions executing faster than this threshold may be excluded from detailed
   * profiling to reduce overhead and focus on significant performance contributors.
   *
   * @return minimum execution time threshold
   */
  Duration getMinExecutionTimeThreshold();

  /**
   * Gets the minimum allocation size threshold for memory profiling.
   *
   * <p>Allocations smaller than this threshold may be excluded from detailed
   * profiling to reduce overhead.
   *
   * @return minimum allocation size in bytes
   */
  long getMinAllocationSizeThreshold();

  /**
   * Checks if profiling data should be aggregated across function calls.
   *
   * <p>Aggregation reduces memory usage but loses timing detail for individual calls.
   *
   * @return true if data should be aggregated
   */
  boolean isAggregateData();

  /**
   * Checks if profiling should include source location information.
   *
   * <p>Source locations help correlate profiling data with original source code
   * but may not be available for all WebAssembly modules.
   *
   * @return true if source locations should be included
   */
  boolean isIncludeSourceLocations();

  /**
   * Gets the buffer size for profiling data collection.
   *
   * <p>Larger buffers can handle burst workloads better but use more memory.
   *
   * @return buffer size in entries
   */
  int getBufferSize();

  /**
   * Checks if profiling should be thread-safe for multi-threaded execution.
   *
   * <p>Thread-safe profiling has higher overhead but is required for accurate
   * profiling in multi-threaded WebAssembly execution.
   *
   * @return true if thread-safe profiling is enabled
   */
  boolean isThreadSafe();

  /**
   * Gets custom profiling filters if configured.
   *
   * <p>Filters can exclude specific functions, modules, or patterns from profiling
   * to focus on areas of interest and reduce overhead.
   *
   * @return profiling filters, or null if none configured
   */
  ProfilingFilter getFilter();

  /**
   * Creates a builder for configuring profiling options.
   *
   * @return new profiling options builder
   */
  static ProfilingOptionsBuilder builder() {
    return new DefaultProfilingOptionsBuilder();
  }

  /**
   * Creates profiling options with default settings optimized for low overhead.
   *
   * @return default profiling options
   */
  static ProfilingOptions defaultOptions() {
    return builder()
        .enableFunctionProfiling(true)
        .enableMemoryProfiling(false)
        .enableCallStackSampling(false)
        .samplingInterval(Duration.ofMillis(10))
        .maxSamples(10000)
        .build();
  }

  /**
   * Creates profiling options with comprehensive settings for detailed analysis.
   *
   * <p>These options enable all profiling features but have higher overhead.
   * Recommended for development and optimization analysis.
   *
   * @return comprehensive profiling options
   */
  static ProfilingOptions comprehensiveOptions() {
    return builder()
        .enableFunctionProfiling(true)
        .enableMemoryProfiling(true)
        .enableCallStackSampling(true)
        .enableCompilationProfiling(true)
        .enableHostFunctionProfiling(true)
        .samplingInterval(Duration.ofMillis(1))
        .maxSamples(100000)
        .includeSourceLocations(true)
        .build();
  }

  /**
   * Builder interface for creating profiling option configurations.
   */
  interface ProfilingOptionsBuilder {

    /**
     * Enables or disables function-level profiling.
     *
     * @param enable true to enable function profiling
     * @return this builder
     */
    ProfilingOptionsBuilder enableFunctionProfiling(final boolean enable);

    /**
     * Enables or disables memory allocation profiling.
     *
     * @param enable true to enable memory profiling
     * @return this builder
     */
    ProfilingOptionsBuilder enableMemoryProfiling(final boolean enable);

    /**
     * Enables or disables call stack sampling.
     *
     * @param enable true to enable call stack sampling
     * @return this builder
     */
    ProfilingOptionsBuilder enableCallStackSampling(final boolean enable);

    /**
     * Enables or disables JIT compilation profiling.
     *
     * @param enable true to enable compilation profiling
     * @return this builder
     */
    ProfilingOptionsBuilder enableCompilationProfiling(final boolean enable);

    /**
     * Enables or disables host function interaction profiling.
     *
     * @param enable true to enable host function profiling
     * @return this builder
     */
    ProfilingOptionsBuilder enableHostFunctionProfiling(final boolean enable);

    /**
     * Enables or disables instruction-level profiling.
     *
     * @param enable true to enable instruction profiling
     * @return this builder
     */
    ProfilingOptionsBuilder enableInstructionProfiling(final boolean enable);

    /**
     * Sets the sampling interval for data collection.
     *
     * @param interval sampling interval duration
     * @return this builder
     */
    ProfilingOptionsBuilder samplingInterval(final Duration interval);

    /**
     * Sets the maximum number of samples to collect.
     *
     * @param maxSamples maximum sample count
     * @return this builder
     */
    ProfilingOptionsBuilder maxSamples(final int maxSamples);

    /**
     * Sets the minimum execution time threshold.
     *
     * @param threshold minimum execution time
     * @return this builder
     */
    ProfilingOptionsBuilder minExecutionTimeThreshold(final Duration threshold);

    /**
     * Sets the minimum allocation size threshold.
     *
     * @param threshold minimum allocation size in bytes
     * @return this builder
     */
    ProfilingOptionsBuilder minAllocationSizeThreshold(final long threshold);

    /**
     * Enables or disables data aggregation.
     *
     * @param aggregate true to enable data aggregation
     * @return this builder
     */
    ProfilingOptionsBuilder aggregateData(final boolean aggregate);

    /**
     * Enables or disables source location inclusion.
     *
     * @param include true to include source locations
     * @return this builder
     */
    ProfilingOptionsBuilder includeSourceLocations(final boolean include);

    /**
     * Sets the buffer size for data collection.
     *
     * @param bufferSize buffer size in entries
     * @return this builder
     */
    ProfilingOptionsBuilder bufferSize(final int bufferSize);

    /**
     * Enables or disables thread-safe profiling.
     *
     * @param threadSafe true to enable thread-safe profiling
     * @return this builder
     */
    ProfilingOptionsBuilder threadSafe(final boolean threadSafe);

    /**
     * Sets profiling filters.
     *
     * @param filter profiling filter configuration
     * @return this builder
     */
    ProfilingOptionsBuilder filter(final ProfilingFilter filter);

    /**
     * Builds the profiling options configuration.
     *
     * @return configured profiling options
     */
    ProfilingOptions build();
  }
}