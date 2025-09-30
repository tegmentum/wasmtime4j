package ai.tegmentum.wasmtime4j.compilation;

/**
 * JIT performance monitor interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface JitPerformanceMonitor {

  /** Starts performance monitoring. */
  void startMonitoring();

  /** Stops performance monitoring. */
  void stopMonitoring();

  /**
   * Gets compilation statistics.
   *
   * @return compilation statistics
   */
  CompilationStatistics getCompilationStatistics();

  /**
   * Gets execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getExecutionStatistics();

  /**
   * Gets performance metrics.
   *
   * @return performance metrics
   */
  PerformanceMetrics getPerformanceMetrics();

  /** Resets all statistics. */
  void resetStatistics();

  /**
   * Checks if monitoring is active.
   *
   * @return true if monitoring
   */
  boolean isMonitoring();

  /** Compilation statistics interface. */
  interface CompilationStatistics {
    /**
     * Gets the total compilation time.
     *
     * @return compilation time in milliseconds
     */
    long getTotalCompilationTime();

    /**
     * Gets the number of functions compiled.
     *
     * @return function count
     */
    int getFunctionsCompiled();

    /**
     * Gets the average compilation time per function.
     *
     * @return average time in milliseconds
     */
    double getAverageCompilationTime();

    /**
     * Gets compilation tier statistics.
     *
     * @return tier statistics
     */
    java.util.Map<String, TierStatistics> getTierStatistics();
  }

  /** Execution statistics interface. */
  interface ExecutionStatistics {
    /**
     * Gets the total execution time.
     *
     * @return execution time in nanoseconds
     */
    long getTotalExecutionTime();

    /**
     * Gets the instruction count.
     *
     * @return instruction count
     */
    long getInstructionCount();

    /**
     * Gets the average instructions per second.
     *
     * @return instructions per second
     */
    double getInstructionsPerSecond();

    /**
     * Gets function call statistics.
     *
     * @return call statistics
     */
    java.util.Map<String, FunctionCallStatistics> getFunctionCallStatistics();
  }

  /** Performance metrics interface. */
  interface PerformanceMetrics {
    /**
     * Gets the throughput in operations per second.
     *
     * @return throughput
     */
    double getThroughput();

    /**
     * Gets the average latency in microseconds.
     *
     * @return latency
     */
    double getAverageLatency();

    /**
     * Gets the 95th percentile latency.
     *
     * @return p95 latency in microseconds
     */
    double getP95Latency();

    /**
     * Gets the cache hit ratio.
     *
     * @return cache hit ratio (0.0-1.0)
     */
    double getCacheHitRatio();
  }

  /** Tier statistics interface. */
  interface TierStatistics {
    /**
     * Gets the tier name.
     *
     * @return tier name
     */
    String getTierName();

    /**
     * Gets the compilation count.
     *
     * @return compilation count
     */
    int getCompilationCount();

    /**
     * Gets the total compilation time.
     *
     * @return compilation time in milliseconds
     */
    long getTotalTime();
  }

  /** Function call statistics interface. */
  interface FunctionCallStatistics {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the call count.
     *
     * @return call count
     */
    long getCallCount();

    /**
     * Gets the total execution time.
     *
     * @return execution time in nanoseconds
     */
    long getTotalExecutionTime();

    /**
     * Gets the average execution time per call.
     *
     * @return average time in nanoseconds
     */
    double getAverageExecutionTime();
  }
}
