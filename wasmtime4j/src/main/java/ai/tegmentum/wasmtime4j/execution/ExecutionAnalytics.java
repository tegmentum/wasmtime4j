package ai.tegmentum.wasmtime4j.execution;

/**
 * Execution analytics interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ExecutionAnalytics {

  /**
   * Gets performance analytics.
   *
   * @return performance analytics
   */
  PerformanceAnalytics getPerformanceAnalytics();

  /**
   * Gets resource analytics.
   *
   * @return resource analytics
   */
  ResourceAnalytics getResourceAnalytics();

  /**
   * Gets error analytics.
   *
   * @return error analytics
   */
  ErrorAnalytics getErrorAnalytics();

  /**
   * Gets trend analytics.
   *
   * @return trend analytics
   */
  TrendAnalytics getTrendAnalytics();

  /**
   * Gets the analytics time range.
   *
   * @return time range
   */
  TimeRange getTimeRange();

  /**
   * Sets the analytics time range.
   *
   * @param timeRange time range
   */
  void setTimeRange(TimeRange timeRange);

  /** Refreshes analytics data. */
  void refresh();

  /**
   * Exports analytics data.
   *
   * @param format export format
   * @return exported data
   */
  byte[] export(ExportFormat format);

  /** Performance analytics interface. */
  interface PerformanceAnalytics {
    /**
     * Gets average execution time.
     *
     * @return average time in nanoseconds
     */
    double getAverageExecutionTime();

    /**
     * Gets execution time percentiles.
     *
     * @return percentile map
     */
    java.util.Map<Integer, Double> getPercentiles();

    /**
     * Gets throughput statistics.
     *
     * @return throughput in executions per second
     */
    double getThroughput();

    /**
     * Gets latency statistics.
     *
     * @return latency statistics
     */
    LatencyStatistics getLatencyStatistics();
  }

  /** Resource analytics interface. */
  interface ResourceAnalytics {
    /**
     * Gets memory usage statistics.
     *
     * @return memory usage stats
     */
    MemoryUsageStatistics getMemoryUsage();

    /**
     * Gets CPU usage statistics.
     *
     * @return CPU usage stats
     */
    CpuUsageStatistics getCpuUsage();

    /**
     * Gets fuel consumption statistics.
     *
     * @return fuel consumption stats
     */
    FuelConsumptionStatistics getFuelConsumption();
  }

  /** Error analytics interface. */
  interface ErrorAnalytics {
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
     * Gets top errors.
     *
     * @param limit number of top errors
     * @return list of top errors
     */
    java.util.List<ErrorInfo> getTopErrors(int limit);
  }

  /** Trend analytics interface. */
  interface TrendAnalytics {
    /**
     * Gets performance trends.
     *
     * @return performance trend data
     */
    TrendData getPerformanceTrends();

    /**
     * Gets resource usage trends.
     *
     * @return resource usage trend data
     */
    TrendData getResourceUsageTrends();

    /**
     * Gets error rate trends.
     *
     * @return error rate trend data
     */
    TrendData getErrorRateTrends();
  }

  /** Time range interface. */
  interface TimeRange {
    /**
     * Gets start time.
     *
     * @return start timestamp
     */
    long getStartTime();

    /**
     * Gets end time.
     *
     * @return end timestamp
     */
    long getEndTime();

    /**
     * Gets duration in milliseconds.
     *
     * @return duration
     */
    long getDuration();
  }

  /** Latency statistics interface. */
  interface LatencyStatistics {
    /**
     * Gets minimum latency.
     *
     * @return min latency in microseconds
     */
    double getMinLatency();

    /**
     * Gets maximum latency.
     *
     * @return max latency in microseconds
     */
    double getMaxLatency();

    /**
     * Gets median latency.
     *
     * @return median latency in microseconds
     */
    double getMedianLatency();
  }

  /** Memory usage statistics interface. */
  interface MemoryUsageStatistics {
    /**
     * Gets average memory usage.
     *
     * @return average usage in bytes
     */
    long getAverageUsage();

    /**
     * Gets peak memory usage.
     *
     * @return peak usage in bytes
     */
    long getPeakUsage();
  }

  /** CPU usage statistics interface. */
  interface CpuUsageStatistics {
    /**
     * Gets average CPU usage.
     *
     * @return average usage (0.0-1.0)
     */
    double getAverageUsage();

    /**
     * Gets peak CPU usage.
     *
     * @return peak usage (0.0-1.0)
     */
    double getPeakUsage();
  }

  /** Fuel consumption statistics interface. */
  interface FuelConsumptionStatistics {
    /**
     * Gets average fuel consumption.
     *
     * @return average consumption
     */
    long getAverageConsumption();

    /**
     * Gets total fuel consumed.
     *
     * @return total consumption
     */
    long getTotalConsumed();
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
     * Gets error message.
     *
     * @return error message
     */
    String getMessage();
  }

  /** Trend data interface. */
  interface TrendData {
    /**
     * Gets data points.
     *
     * @return list of data points
     */
    java.util.List<DataPoint> getDataPoints();

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
  }

  /** Data point interface. */
  interface DataPoint {
    /**
     * Gets timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets value.
     *
     * @return value
     */
    double getValue();
  }

  /** Export format enumeration. */
  enum ExportFormat {
    /** JSON format. */
    JSON,
    /** CSV format. */
    CSV,
    /** Excel format. */
    EXCEL,
    /** PDF format. */
    PDF
  }

  /** Trend direction enumeration. */
  enum TrendDirection {
    /** Upward trend. */
    UPWARD,
    /** Downward trend. */
    DOWNWARD,
    /** Stable trend. */
    STABLE,
    /** No clear trend. */
    UNCLEAR
  }
}
