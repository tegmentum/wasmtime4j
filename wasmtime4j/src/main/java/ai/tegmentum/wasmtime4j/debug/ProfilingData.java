package ai.tegmentum.wasmtime4j.debug;

/**
 * Profiling data interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ProfilingData {

  /**
   * Gets the profiling session ID.
   *
   * @return session ID
   */
  String getSessionId();

  /**
   * Gets the total execution time.
   *
   * @return execution time in nanoseconds
   */
  long getTotalExecutionTime();

  /**
   * Gets function call statistics.
   *
   * @return function call statistics
   */
  java.util.Map<String, FunctionStatistics> getFunctionStatistics();

  /**
   * Gets memory usage statistics.
   *
   * @return memory statistics
   */
  MemoryProfilingData getMemoryData();

  /**
   * Gets the profiling start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets the profiling end time.
   *
   * @return end timestamp
   */
  long getEndTime();

  /** Function statistics interface. */
  interface FunctionStatistics {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the total call count.
     *
     * @return call count
     */
    long getCallCount();

    /**
     * Gets the total execution time.
     *
     * @return execution time in nanoseconds
     */
    long getTotalTime();

    /**
     * Gets the average execution time per call.
     *
     * @return average time in nanoseconds
     */
    long getAverageTime();
  }

  /** Memory profiling data interface. */
  interface MemoryProfilingData {
    /**
     * Gets the peak memory usage.
     *
     * @return peak usage in bytes
     */
    long getPeakUsage();

    /**
     * Gets the current memory usage.
     *
     * @return current usage in bytes
     */
    long getCurrentUsage();

    /**
     * Gets allocation statistics.
     *
     * @return allocation statistics
     */
    AllocationStatistics getAllocationStats();

    /** Allocation statistics interface. */
    interface AllocationStatistics {
      /**
       * Gets the total allocations.
       *
       * @return allocation count
       */
      long getTotalAllocations();

      /**
       * Gets the total deallocations.
       *
       * @return deallocation count
       */
      long getTotalDeallocations();

      /**
       * Gets the bytes allocated.
       *
       * @return bytes allocated
       */
      long getBytesAllocated();

      /**
       * Gets the bytes deallocated.
       *
       * @return bytes deallocated
       */
      long getBytesDeallocated();
    }
  }
}
