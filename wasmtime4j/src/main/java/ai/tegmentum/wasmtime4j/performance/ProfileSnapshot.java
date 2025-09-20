package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive performance snapshot capturing system state at a specific moment.
 *
 * <p>A profile snapshot provides a complete view of performance metrics, resource usage, and system
 * characteristics at the time of capture. This enables:
 *
 * <ul>
 *   <li>Point-in-time performance analysis
 *   <li>Historical trend tracking
 *   <li>Performance comparison across different time periods
 *   <li>System state correlation with performance characteristics
 * </ul>
 *
 * <p>Snapshots are automatically captured during profiling sessions and can also be manually
 * triggered for specific analysis needs.
 *
 * @since 1.0.0
 */
public interface ProfileSnapshot {

  /**
   * Gets the timestamp when this snapshot was captured.
   *
   * @return capture timestamp
   */
  Instant getCaptureTime();

  /**
   * Gets the duration of the profiling session up to this snapshot.
   *
   * @return session duration
   */
  Duration getSessionDuration();

  /**
   * Gets the overall performance score for this snapshot.
   *
   * <p>The performance score is a composite metric that combines multiple performance indicators
   * into a single value for easy comparison. Higher scores indicate better performance.
   *
   * @return performance score (0-100 scale)
   */
  double getPerformanceScore();

  /**
   * Gets CPU usage metrics at the time of capture.
   *
   * @return CPU usage metrics
   */
  CpuUsageMetrics getCpuUsage();

  /**
   * Gets memory usage metrics at the time of capture.
   *
   * @return memory usage metrics
   */
  MemoryUsageMetrics getMemoryUsage();

  /**
   * Gets garbage collection metrics at the time of capture.
   *
   * @return GC metrics
   */
  GcImpactMetrics getGcMetrics();

  /**
   * Gets function execution profiles for WebAssembly functions.
   *
   * @return list of function profiles sorted by execution time
   */
  List<FunctionProfile> getFunctionProfiles();

  /**
   * Gets thread activity metrics at the time of capture.
   *
   * @return thread activity metrics
   */
  ThreadActivityMetrics getThreadActivity();

  /**
   * Gets I/O operation metrics at the time of capture.
   *
   * @return I/O metrics
   */
  IoMetrics getIoMetrics();

  /**
   * Gets compilation and JIT activity metrics.
   *
   * @return compilation metrics
   */
  CompilationMetrics getCompilationMetrics();

  /**
   * Gets custom metrics that were enabled during profiling.
   *
   * @return map of custom metric names to values
   */
  Map<String, Object> getCustomMetrics();

  /**
   * Gets the profiler configuration that was active when this snapshot was captured.
   *
   * @return profiler configuration
   */
  ProfilerConfig getProfilerConfig();

  /**
   * Checks if this snapshot indicates any performance problems.
   *
   * @return true if performance issues are detected
   */
  boolean hasPerformanceIssues();

  /**
   * Gets a list of performance warnings identified in this snapshot.
   *
   * @return list of performance warnings
   */
  List<String> getPerformanceWarnings();

  /**
   * Gets recommendations for performance improvements based on this snapshot.
   *
   * @return list of performance recommendations
   */
  List<String> getPerformanceRecommendations();

  /**
   * Compares this snapshot with another snapshot to identify changes.
   *
   * @param other the snapshot to compare against
   * @return comparison result highlighting differences
   */
  SnapshotComparison compareTo(ProfileSnapshot other);

  /**
   * Exports this snapshot in the specified format.
   *
   * @param format the export format
   * @return exported data as string
   */
  String export(ExportFormat format);

  /**
   * Gets a human-readable summary of this snapshot.
   *
   * @return snapshot summary
   */
  String getSummary();

  /** CPU usage metrics captured in a snapshot. */
  interface CpuUsageMetrics {
    /**
     * Gets the overall CPU utilization percentage.
     *
     * @return CPU utilization (0-100)
     */
    double getCpuUtilization();

    /**
     * Gets the user CPU time percentage.
     *
     * @return user CPU time (0-100)
     */
    double getUserCpuTime();

    /**
     * Gets the system CPU time percentage.
     *
     * @return system CPU time (0-100)
     */
    double getSystemCpuTime();

    /**
     * Gets the process CPU load average.
     *
     * @return CPU load average
     */
    double getLoadAverage();

    /**
     * Gets the number of available CPU cores.
     *
     * @return available CPU cores
     */
    int getAvailableCores();
  }

  /** Memory usage metrics captured in a snapshot. */
  interface MemoryUsageMetrics {
    /**
     * Gets the heap memory usage in bytes.
     *
     * @return heap memory used
     */
    long getHeapMemoryUsed();

    /**
     * Gets the maximum heap memory in bytes.
     *
     * @return maximum heap memory
     */
    long getMaxHeapMemory();

    /**
     * Gets the heap memory utilization percentage.
     *
     * @return heap utilization (0-100)
     */
    double getHeapUtilization();

    /**
     * Gets the non-heap memory usage in bytes.
     *
     * @return non-heap memory used
     */
    long getNonHeapMemoryUsed();

    /**
     * Gets the direct memory usage in bytes.
     *
     * @return direct memory used
     */
    long getDirectMemoryUsed();

    /**
     * Gets the native memory usage in bytes (estimated).
     *
     * @return native memory used
     */
    long getNativeMemoryUsed();
  }

  /** Function execution profile for WebAssembly functions. */
  interface FunctionProfile {
    /**
     * Gets the function name or identifier.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the total execution time for this function.
     *
     * @return total execution time
     */
    Duration getTotalExecutionTime();

    /**
     * Gets the number of times this function was called.
     *
     * @return call count
     */
    long getCallCount();

    /**
     * Gets the average execution time per call.
     *
     * @return average execution time
     */
    Duration getAverageExecutionTime();

    /**
     * Gets the percentage of total CPU time spent in this function.
     *
     * @return CPU time percentage
     */
    double getCpuTimePercentage();

    /**
     * Checks if this function is considered a performance hotspot.
     *
     * @return true if this is a hotspot function
     */
    boolean isHotspot();
  }

  /** Thread activity metrics captured in a snapshot. */
  interface ThreadActivityMetrics {
    /**
     * Gets the total number of active threads.
     *
     * @return active thread count
     */
    int getActiveThreadCount();

    /**
     * Gets the number of daemon threads.
     *
     * @return daemon thread count
     */
    int getDaemonThreadCount();

    /**
     * Gets the peak thread count since the start of the JVM.
     *
     * @return peak thread count
     */
    int getPeakThreadCount();

    /**
     * Gets the total thread CPU time.
     *
     * @return total thread CPU time
     */
    Duration getTotalThreadCpuTime();

    /**
     * Gets the blocked thread count.
     *
     * @return blocked thread count
     */
    int getBlockedThreadCount();

    /**
     * Gets the waiting thread count.
     *
     * @return waiting thread count
     */
    int getWaitingThreadCount();
  }

  /** I/O operation metrics captured in a snapshot. */
  interface IoMetrics {
    /**
     * Gets the total bytes read from I/O operations.
     *
     * @return bytes read
     */
    long getBytesRead();

    /**
     * Gets the total bytes written to I/O operations.
     *
     * @return bytes written
     */
    long getBytesWritten();

    /**
     * Gets the number of read operations performed.
     *
     * @return read operation count
     */
    long getReadOperations();

    /**
     * Gets the number of write operations performed.
     *
     * @return write operation count
     */
    long getWriteOperations();

    /**
     * Gets the average I/O latency.
     *
     * @return average I/O latency
     */
    Duration getAverageLatency();

    /**
     * Gets the I/O throughput in bytes per second.
     *
     * @return I/O throughput
     */
    double getThroughput();
  }

  /** Compilation and JIT activity metrics captured in a snapshot. */
  interface CompilationMetrics {
    /**
     * Gets the total compilation time.
     *
     * @return total compilation time
     */
    Duration getTotalCompilationTime();

    /**
     * Gets the number of compiled methods.
     *
     * @return compiled method count
     */
    int getCompiledMethodCount();

    /**
     * Gets the compilation rate (methods per second).
     *
     * @return compilation rate
     */
    double getCompilationRate();

    /**
     * Gets the code cache usage percentage.
     *
     * @return code cache usage (0-100)
     */
    double getCodeCacheUsage();

    /**
     * Gets the number of compilation failures.
     *
     * @return compilation failure count
     */
    int getCompilationFailures();

    /**
     * Checks if the code cache is approaching capacity.
     *
     * @return true if code cache is nearly full
     */
    boolean isCodeCacheNearlyFull();
  }

  /** Result of comparing two performance snapshots. */
  interface SnapshotComparison {
    /**
     * Gets the baseline snapshot (earlier in time).
     *
     * @return baseline snapshot
     */
    ProfileSnapshot getBaseline();

    /**
     * Gets the comparison snapshot (later in time).
     *
     * @return comparison snapshot
     */
    ProfileSnapshot getComparison();

    /**
     * Gets the time difference between snapshots.
     *
     * @return time difference
     */
    Duration getTimeDifference();

    /**
     * Gets the performance score change.
     *
     * @return performance score change (positive indicates improvement)
     */
    double getPerformanceScoreChange();

    /**
     * Gets the CPU usage change.
     *
     * @return CPU usage change percentage
     */
    double getCpuUsageChange();

    /**
     * Gets the memory usage change.
     *
     * @return memory usage change in bytes
     */
    long getMemoryUsageChange();

    /**
     * Checks if there was a significant performance improvement.
     *
     * @return true if performance improved significantly
     */
    boolean hasPerformanceImprovement();

    /**
     * Checks if there was a significant performance regression.
     *
     * @return true if performance regressed significantly
     */
    boolean hasPerformanceRegression();

    /**
     * Gets a summary of changes between snapshots.
     *
     * @return comparison summary
     */
    String getSummary();
  }
}
