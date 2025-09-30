package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution statistics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionStatistics {

  /**
   * Gets the total execution time in nanoseconds.
   *
   * @return execution time
   */
  long getExecutionTime();

  /**
   * Gets the total instruction count.
   *
   * @return instruction count
   */
  long getInstructionCount();

  /**
   * Gets the function call count.
   *
   * @return function call count
   */
  long getFunctionCallCount();

  /**
   * Gets the memory allocation count.
   *
   * @return allocation count
   */
  long getMemoryAllocations();

  /**
   * Gets the peak memory usage in bytes.
   *
   * @return peak memory usage
   */
  long getPeakMemoryUsage();

  /**
   * Gets the current memory usage in bytes.
   *
   * @return current memory usage
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the average instructions per second.
   *
   * @return instructions per second
   */
  double getInstructionsPerSecond();

  /**
   * Gets the average function calls per second.
   *
   * @return function calls per second
   */
  double getFunctionCallsPerSecond();

  /**
   * Gets CPU usage statistics.
   *
   * @return CPU usage statistics
   */
  CpuUsage getCpuUsage();

  /**
   * Gets memory usage statistics.
   *
   * @return memory usage statistics
   */
  MemoryUsage getMemoryUsage();

  /**
   * Gets performance metrics.
   *
   * @return performance metrics
   */
  PerformanceMetrics getPerformanceMetrics();

  /** CPU usage statistics interface. */
  interface CpuUsage {
    /**
     * Gets the total CPU time in nanoseconds.
     *
     * @return CPU time
     */
    long getTotalCpuTime();

    /**
     * Gets the user CPU time in nanoseconds.
     *
     * @return user CPU time
     */
    long getUserCpuTime();

    /**
     * Gets the system CPU time in nanoseconds.
     *
     * @return system CPU time
     */
    long getSystemCpuTime();

    /**
     * Gets the CPU usage percentage.
     *
     * @return CPU usage (0.0-1.0)
     */
    double getCpuUsagePercentage();
  }

  /** Memory usage statistics interface. */
  interface MemoryUsage {
    /**
     * Gets the heap memory usage.
     *
     * @return heap usage in bytes
     */
    long getHeapUsage();

    /**
     * Gets the stack memory usage.
     *
     * @return stack usage in bytes
     */
    long getStackUsage();

    /**
     * Gets the total allocated memory.
     *
     * @return allocated memory in bytes
     */
    long getTotalAllocated();

    /**
     * Gets the total freed memory.
     *
     * @return freed memory in bytes
     */
    long getTotalFreed();

    /**
     * Gets the garbage collection count.
     *
     * @return GC count
     */
    int getGcCount();

    /**
     * Gets the total garbage collection time.
     *
     * @return GC time in milliseconds
     */
    long getGcTime();
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
     * @return average latency
     */
    double getAverageLatency();

    /**
     * Gets the 95th percentile latency in microseconds.
     *
     * @return p95 latency
     */
    double getP95Latency();

    /**
     * Gets the 99th percentile latency in microseconds.
     *
     * @return p99 latency
     */
    double getP99Latency();
  }
}
