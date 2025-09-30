package ai.tegmentum.wasmtime4j.execution;

/**
 * Controller statistics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ControllerStatistics {

  /**
   * Gets the statistics collection start time.
   *
   * @return start timestamp
   */
  long getStartTime();

  /**
   * Gets the statistics collection end time.
   *
   * @return end timestamp
   */
  long getEndTime();

  /**
   * Gets the total duration of statistics collection.
   *
   * @return duration in milliseconds
   */
  long getDuration();

  /**
   * Gets execution statistics.
   *
   * @return execution statistics
   */
  ExecutionStatistics getExecutionStatistics();

  /**
   * Gets resource management statistics.
   *
   * @return resource management statistics
   */
  ResourceManagementStatistics getResourceManagementStatistics();

  /**
   * Gets performance statistics.
   *
   * @return performance statistics
   */
  PerformanceStatistics getPerformanceStatistics();

  /**
   * Gets error statistics.
   *
   * @return error statistics
   */
  ErrorStatistics getErrorStatistics();

  /**
   * Gets throughput statistics.
   *
   * @return throughput statistics
   */
  ThroughputStatistics getThroughputStatistics();

  /**
   * Gets latency statistics.
   *
   * @return latency statistics
   */
  LatencyStatistics getLatencyStatistics();

  /**
   * Gets queue statistics.
   *
   * @return queue statistics
   */
  QueueStatistics getQueueStatistics();

  /**
   * Gets anomaly detection statistics.
   *
   * @return anomaly detection statistics
   */
  AnomalyDetectionStatistics getAnomalyDetectionStatistics();

  /** Resets all statistics. */
  void reset();

  /**
   * Snapshots current statistics.
   *
   * @return statistics snapshot
   */
  ControllerStatistics snapshot();

  /** Resource management statistics interface. */
  interface ResourceManagementStatistics {
    /**
     * Gets total resource allocations.
     *
     * @return allocation count
     */
    long getTotalAllocations();

    /**
     * Gets total resource deallocations.
     *
     * @return deallocation count
     */
    long getTotalDeallocations();

    /**
     * Gets current active allocations.
     *
     * @return active allocation count
     */
    int getActiveAllocations();

    /**
     * Gets average allocation size.
     *
     * @return average size in bytes
     */
    double getAverageAllocationSize();

    /**
     * Gets peak memory usage.
     *
     * @return peak usage in bytes
     */
    long getPeakMemoryUsage();

    /**
     * Gets current memory usage.
     *
     * @return current usage in bytes
     */
    long getCurrentMemoryUsage();

    /**
     * Gets allocation failure rate.
     *
     * @return failure rate (0.0-1.0)
     */
    double getAllocationFailureRate();
  }

  /** Performance statistics interface. */
  interface PerformanceStatistics {
    /**
     * Gets total instructions executed.
     *
     * @return instruction count
     */
    long getTotalInstructions();

    /**
     * Gets instructions per second.
     *
     * @return instructions per second
     */
    double getInstructionsPerSecond();

    /**
     * Gets total function calls.
     *
     * @return function call count
     */
    long getTotalFunctionCalls();

    /**
     * Gets function calls per second.
     *
     * @return function calls per second
     */
    double getFunctionCallsPerSecond();

    /**
     * Gets average execution time.
     *
     * @return average time in nanoseconds
     */
    double getAverageExecutionTime();

    /**
     * Gets JIT compilation statistics.
     *
     * @return JIT compilation stats
     */
    JitCompilationStatistics getJitCompilationStatistics();
  }

  /** Error statistics interface. */
  interface ErrorStatistics {
    /**
     * Gets total error count.
     *
     * @return error count
     */
    long getTotalErrors();

    /**
     * Gets error rate.
     *
     * @return error rate (0.0-1.0)
     */
    double getErrorRate();

    /**
     * Gets error distribution by type.
     *
     * @return error type distribution
     */
    java.util.Map<String, Integer> getErrorDistribution();

    /**
     * Gets recoverable error count.
     *
     * @return recoverable error count
     */
    long getRecoverableErrors();

    /**
     * Gets fatal error count.
     *
     * @return fatal error count
     */
    long getFatalErrors();

    /**
     * Gets most common errors.
     *
     * @param limit number of top errors
     * @return list of common errors
     */
    java.util.List<ErrorInfo> getMostCommonErrors(int limit);
  }

  /** Throughput statistics interface. */
  interface ThroughputStatistics {
    /**
     * Gets current throughput.
     *
     * @return throughput in requests per second
     */
    double getCurrentThroughput();

    /**
     * Gets average throughput.
     *
     * @return average throughput
     */
    double getAverageThroughput();

    /**
     * Gets peak throughput.
     *
     * @return peak throughput
     */
    double getPeakThroughput();

    /**
     * Gets throughput percentiles.
     *
     * @return percentile map
     */
    java.util.Map<Integer, Double> getThroughputPercentiles();

    /**
     * Gets throughput trend.
     *
     * @return throughput trend data
     */
    ThroughputTrend getTrend();
  }

  /** Latency statistics interface. */
  interface LatencyStatistics {
    /**
     * Gets average latency.
     *
     * @return average latency in microseconds
     */
    double getAverageLatency();

    /**
     * Gets minimum latency.
     *
     * @return minimum latency in microseconds
     */
    double getMinLatency();

    /**
     * Gets maximum latency.
     *
     * @return maximum latency in microseconds
     */
    double getMaxLatency();

    /**
     * Gets latency percentiles.
     *
     * @return percentile map
     */
    java.util.Map<Integer, Double> getLatencyPercentiles();

    /**
     * Gets latency distribution.
     *
     * @return latency distribution
     */
    LatencyDistribution getDistribution();
  }

  /** Queue statistics interface. */
  interface QueueStatistics {
    /**
     * Gets current queue size.
     *
     * @return queue size
     */
    int getCurrentQueueSize();

    /**
     * Gets average queue size.
     *
     * @return average queue size
     */
    double getAverageQueueSize();

    /**
     * Gets peak queue size.
     *
     * @return peak queue size
     */
    int getPeakQueueSize();

    /**
     * Gets average wait time.
     *
     * @return average wait time in milliseconds
     */
    double getAverageWaitTime();

    /**
     * Gets queue utilization.
     *
     * @return utilization (0.0-1.0)
     */
    double getUtilization();

    /**
     * Gets dropped request count.
     *
     * @return dropped requests
     */
    long getDroppedRequests();
  }

  /** Anomaly detection statistics interface. */
  interface AnomalyDetectionStatistics {
    /**
     * Gets total anomalies detected.
     *
     * @return anomaly count
     */
    long getTotalAnomalies();

    /**
     * Gets anomalies by severity.
     *
     * @return severity distribution
     */
    java.util.Map<String, Integer> getAnomaliesBySeverity();

    /**
     * Gets false positive rate.
     *
     * @return false positive rate (0.0-1.0)
     */
    double getFalsePositiveRate();

    /**
     * Gets detection accuracy.
     *
     * @return accuracy (0.0-1.0)
     */
    double getDetectionAccuracy();

    /**
     * Gets average detection time.
     *
     * @return average time in milliseconds
     */
    double getAverageDetectionTime();
  }

  /** JIT compilation statistics interface. */
  interface JitCompilationStatistics {
    /**
     * Gets total compilations.
     *
     * @return compilation count
     */
    long getTotalCompilations();

    /**
     * Gets average compilation time.
     *
     * @return average time in milliseconds
     */
    double getAverageCompilationTime();

    /**
     * Gets compilation cache hit rate.
     *
     * @return cache hit rate (0.0-1.0)
     */
    double getCacheHitRate();

    /**
     * Gets optimized functions count.
     *
     * @return optimized function count
     */
    int getOptimizedFunctions();

    /**
     * Gets deoptimization count.
     *
     * @return deoptimization count
     */
    long getDeoptimizations();
  }

  /** Error information interface. */
  interface ErrorInfo {
    /**
     * Gets error type.
     *
     * @return error type
     */
    String getType();

    /**
     * Gets error count.
     *
     * @return error count
     */
    int getCount();

    /**
     * Gets error percentage.
     *
     * @return percentage (0.0-1.0)
     */
    double getPercentage();

    /**
     * Gets sample error message.
     *
     * @return sample message
     */
    String getSampleMessage();
  }

  /** Throughput trend interface. */
  interface ThroughputTrend {
    /**
     * Gets trend direction.
     *
     * @return trend direction
     */
    TrendDirection getDirection();

    /**
     * Gets trend strength.
     *
     * @return trend strength (0.0-1.0)
     */
    double getStrength();

    /**
     * Gets data points.
     *
     * @return list of data points
     */
    java.util.List<ThroughputDataPoint> getDataPoints();
  }

  /** Latency distribution interface. */
  interface LatencyDistribution {
    /**
     * Gets bucket boundaries.
     *
     * @return bucket boundaries in microseconds
     */
    double[] getBuckets();

    /**
     * Gets bucket counts.
     *
     * @return bucket counts
     */
    long[] getCounts();

    /**
     * Gets total samples.
     *
     * @return total samples
     */
    long getTotalSamples();
  }

  /** Throughput data point interface. */
  interface ThroughputDataPoint {
    /**
     * Gets timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets throughput value.
     *
     * @return throughput
     */
    double getThroughput();
  }

  /** Trend direction enumeration. */
  enum TrendDirection {
    /** Increasing trend. */
    INCREASING,
    /** Decreasing trend. */
    DECREASING,
    /** Stable trend. */
    STABLE,
    /** Volatile trend. */
    VOLATILE
  }
}
