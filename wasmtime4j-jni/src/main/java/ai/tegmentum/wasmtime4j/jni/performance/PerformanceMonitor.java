package ai.tegmentum.wasmtime4j.jni.performance;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;

/**
 * Performance monitoring and profiling infrastructure for WebAssembly operations.
 *
 * <p>This class provides comprehensive performance monitoring capabilities including:
 *
 * <ul>
 *   <li>Native call timing and frequency tracking
 *   <li>Memory allocation pattern monitoring
 *   <li>JNI overhead measurement and analysis
 *   <li>Performance regression detection
 *   <li>Real-time performance metrics collection
 *   <li>Detailed profiling hooks for optimization analysis
 * </ul>
 *
 * <p>The monitor tracks all major WebAssembly operations and provides actionable performance data
 * for optimization decisions.
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * // Start monitoring a function call
 * long startTime = PerformanceMonitor.startOperation("function_call", "add_numbers");
 * try {
 *   // Perform WebAssembly function call
 *   result = function.call(params);
 * } finally {
 *   PerformanceMonitor.endOperation("function_call", startTime);
 * }
 *
 * // Get performance statistics
 * String stats = PerformanceMonitor.getStatistics();
 * System.out.println(stats);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PerformanceMonitor {

  private static final Logger LOGGER = Logger.getLogger(PerformanceMonitor.class.getName());

  /** Whether performance monitoring is enabled. */
  private static volatile boolean enabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.monitoring", "true"));

  /** Whether detailed profiling is enabled. */
  private static volatile boolean profilingEnabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.profiling", "false"));

  /** Performance target for simple operations in nanoseconds. */
  public static final long SIMPLE_OPERATION_TARGET_NS = 100;

  /** Threshold for slow operation logging in milliseconds. */
  private static final long SLOW_OPERATION_THRESHOLD_MS = 10;

  /** Ultra-low overhead monitoring. */
  private static volatile boolean lowOverheadMode =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.lowOverhead", "true"));

  /** Sampling rate for low overhead mode (1 in N operations). */
  private static final int LOW_OVERHEAD_SAMPLING_RATE = 100;

  /** Performance monitoring overhead tracking. */
  private static final AtomicLong MONITORING_OVERHEAD_NS = new AtomicLong(0);

  private static final AtomicLong MONITORED_OPERATIONS = new AtomicLong(0);

  /** Performance regression detection. */
  private static final ConcurrentHashMap<String, PerformanceBaseline> BASELINES =
      new ConcurrentHashMap<>();

  private static volatile long lastRegressionCheck = System.currentTimeMillis();
  private static final long REGRESSION_CHECK_INTERVAL_MS = 60_000; // 1 minute

  /** Operation statistics by category. */
  private static final ConcurrentHashMap<String, OperationStats> OPERATION_STATS =
      new ConcurrentHashMap<>();

  /** Memory allocation tracking. */
  private static final AtomicLong NATIVE_ALLOCATIONS = new AtomicLong(0);

  private static final AtomicLong NATIVE_DEALLOCATIONS = new AtomicLong(0);
  private static final AtomicLong TOTAL_ALLOCATED_BYTES = new AtomicLong(0);

  /** JNI call tracking. */
  private static final AtomicLong TOTAL_JNI_CALLS = new AtomicLong(0);

  private static final AtomicLong TOTAL_JNI_TIME_NS = new AtomicLong(0);

  /** GC monitoring. */
  private static volatile long lastGcTime = getCurrentGcTime();

  private static volatile long lastGcCollections = getCurrentGcCollections();

  /** Monitor start time. */
  private static final long MONITOR_START_TIME = System.currentTimeMillis();

  /** Performance baseline for regression detection. */
  private static final class PerformanceBaseline {
    final String category;
    final double baselineAvgNs;
    final double baselineStdDevNs;
    final long baselineCount;
    final long establishedTime;
    volatile boolean stable;

    PerformanceBaseline(
        final String category, final double avgNs, final double stdDevNs, final long count) {
      this.category = category;
      this.baselineAvgNs = avgNs;
      this.baselineStdDevNs = stdDevNs;
      this.baselineCount = count;
      this.establishedTime = System.currentTimeMillis();
      this.stable = count >= 1000; // Stable if we have enough samples
    }

    boolean isRegression(final double currentAvgNs) {
      if (!stable) {
        return false;
      }
      // Consider it a regression if current average is 20% slower than baseline + 2 std dev
      final double threshold = baselineAvgNs + (2 * baselineStdDevNs);
      return currentAvgNs > threshold * 1.2;
    }

    boolean isImprovement(final double currentAvgNs) {
      if (!stable) {
        return false;
      }
      // Consider it an improvement if current average is 10% faster than baseline
      return currentAvgNs < baselineAvgNs * 0.9;
    }
  }

  /** Statistics for a specific operation category. */
  private static final class OperationStats {
    final String category;
    final LongAdder totalCalls = new LongAdder();
    final LongAdder totalTimeNs = new LongAdder();
    final AtomicLong minTimeNs = new AtomicLong(Long.MAX_VALUE);
    final AtomicLong maxTimeNs = new AtomicLong(0);
    final LongAdder slowOperations = new LongAdder();
    final AtomicLong lastCallTime = new AtomicLong(0);

    OperationStats(final String category) {
      this.category = category;
    }

    void recordOperation(final long durationNs) {
      totalCalls.increment();
      totalTimeNs.add(durationNs);
      lastCallTime.set(System.currentTimeMillis());

      // Update min/max times
      minTimeNs.updateAndGet(current -> Math.min(current, durationNs));
      maxTimeNs.updateAndGet(current -> Math.max(current, durationNs));

      // Track slow operations
      if (durationNs > SLOW_OPERATION_THRESHOLD_MS * 1_000_000) {
        slowOperations.increment();
      }
    }

    long getTotalCalls() {
      return totalCalls.sum();
    }

    long getTotalTimeNs() {
      return totalTimeNs.sum();
    }

    double getAverageTimeNs() {
      final long calls = getTotalCalls();
      return calls > 0 ? (double) getTotalTimeNs() / calls : 0.0;
    }

    long getMinTimeNs() {
      final long min = minTimeNs.get();
      return min == Long.MAX_VALUE ? 0 : min;
    }

    long getMaxTimeNs() {
      return maxTimeNs.get();
    }

    long getSlowOperations() {
      return slowOperations.sum();
    }

    double getSlowOperationRate() {
      final long total = getTotalCalls();
      return total > 0 ? (slowOperations.sum() * 100.0) / total : 0.0;
    }
  }

  // Private constructor - utility class
  private PerformanceMonitor() {}

  /**
   * Enables or disables performance monitoring.
   *
   * @param enable true to enable monitoring
   */
  public static void setEnabled(final boolean enable) {
    enabled = enable;
    LOGGER.info("Performance monitoring " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Checks if performance monitoring is enabled.
   *
   * @return true if monitoring is enabled
   */
  public static boolean isEnabled() {
    return enabled;
  }

  /**
   * Enables or disables detailed profiling.
   *
   * @param enable true to enable profiling
   */
  public static void setProfilingEnabled(final boolean enable) {
    profilingEnabled = enable;
    LOGGER.info("Performance profiling " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Checks if detailed profiling is enabled.
   *
   * @return true if profiling is enabled
   */
  public static boolean isProfilingEnabled() {
    return profilingEnabled;
  }

  /**
   * Starts monitoring an operation.
   *
   * @param category operation category (e.g., "function_call", "module_compile")
   * @param details optional operation details
   * @return start time in nanoseconds for use with {@link #endOperation}
   */
  public static long startOperation(final String category, final String details) {
    if (!enabled) {
      return 0;
    }

    final long monitoringStartTime = System.nanoTime();

    // Apply sampling in low overhead mode
    if (lowOverheadMode) {
      final long opCount = MONITORED_OPERATIONS.incrementAndGet();
      if (opCount % LOW_OVERHEAD_SAMPLING_RATE != 0) {
        // Not sampled - return special marker
        return -1;
      }
    }

    TOTAL_JNI_CALLS.incrementAndGet();

    if (profilingEnabled && details != null) {
      LOGGER.fine("Starting " + category + ": " + details);
    }

    final long operationStartTime = System.nanoTime();
    final long monitoringOverhead = operationStartTime - monitoringStartTime;
    MONITORING_OVERHEAD_NS.addAndGet(monitoringOverhead);

    return operationStartTime;
  }

  /**
   * Starts monitoring an operation.
   *
   * @param category operation category
   * @return start time in nanoseconds for use with {@link #endOperation}
   */
  public static long startOperation(final String category) {
    return startOperation(category, null);
  }

  /**
   * Ends monitoring an operation and records performance data.
   *
   * @param category operation category
   * @param startTimeNs start time from {@link #startOperation}
   */
  public static void endOperation(final String category, final long startTimeNs) {
    if (!enabled || startTimeNs == 0) {
      return;
    }

    // Check for sampling marker
    if (startTimeNs == -1) {
      // This operation was not sampled in low overhead mode
      return;
    }

    final long monitoringStartTime = System.nanoTime();
    final long endTimeNs = System.nanoTime();
    final long durationNs = endTimeNs - startTimeNs;

    // Update JNI timing
    TOTAL_JNI_TIME_NS.addAndGet(durationNs);

    // Update operation statistics
    final OperationStats stats = OPERATION_STATS.computeIfAbsent(category, OperationStats::new);
    stats.recordOperation(durationNs);

    // Check for performance regressions
    checkPerformanceRegression(category, stats);

    // Log slow operations
    final double durationMs = durationNs / 1_000_000.0;
    if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
      LOGGER.warning(String.format("Slow %s operation: %.2fms", category, durationMs));
    }

    // Log profiling information
    if (profilingEnabled) {
      LOGGER.fine(String.format("Completed %s in %.2fms", category, durationMs));
    }

    // Track monitoring overhead
    final long monitoringOverhead = System.nanoTime() - monitoringStartTime;
    MONITORING_OVERHEAD_NS.addAndGet(monitoringOverhead);
  }

  /**
   * Records a native memory allocation.
   *
   * @param bytes number of bytes allocated
   */
  public static void recordAllocation(final long bytes) {
    if (!enabled) {
      return;
    }

    NATIVE_ALLOCATIONS.incrementAndGet();
    TOTAL_ALLOCATED_BYTES.addAndGet(bytes);

    if (profilingEnabled) {
      LOGGER.fine("Native allocation: " + bytes + " bytes");
    }
  }

  /**
   * Records a native memory deallocation.
   *
   * @param bytes number of bytes deallocated
   */
  public static void recordDeallocation(final long bytes) {
    if (!enabled) {
      return;
    }

    NATIVE_DEALLOCATIONS.incrementAndGet();
    TOTAL_ALLOCATED_BYTES.addAndGet(-bytes);
  }

  /**
   * Gets the current JNI call overhead in nanoseconds per call.
   *
   * @return average JNI overhead per call
   */
  public static double getAverageJniOverhead() {
    final long totalCalls = TOTAL_JNI_CALLS.get();
    final long totalTime = TOTAL_JNI_TIME_NS.get();
    return totalCalls > 0 ? (double) totalTime / totalCalls : 0.0;
  }

  /**
   * Checks if the current JNI overhead meets the performance target.
   *
   * @return true if overhead is below target
   */
  public static boolean meetsPerformanceTarget() {
    return getAverageJniOverhead() < SIMPLE_OPERATION_TARGET_NS;
  }

  /**
   * Gets comprehensive performance statistics.
   *
   * @return formatted performance statistics
   */
  public static String getStatistics() {
    if (!enabled) {
      return "Performance monitoring is disabled";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("=== WebAssembly Performance Statistics ===\n");

    // Overall metrics
    final long uptimeMs = System.currentTimeMillis() - MONITOR_START_TIME;
    final long totalCalls = TOTAL_JNI_CALLS.get();
    final double avgOverhead = getAverageJniOverhead();
    final boolean meetsTarget = avgOverhead < SIMPLE_OPERATION_TARGET_NS;

    sb.append(String.format("Uptime: %d ms%n", uptimeMs));
    sb.append(String.format("Total JNI calls: %,d%n", totalCalls));
    sb.append(
        String.format(
            "Average JNI overhead: %.0f ns/call %s%n",
            avgOverhead, meetsTarget ? "(✓ meets target)" : "(⚠ exceeds target)"));
    sb.append(String.format("Performance target: %d ns/call%n", SIMPLE_OPERATION_TARGET_NS));
    sb.append(String.format("%n"));

    // Memory statistics
    final long allocations = NATIVE_ALLOCATIONS.get();
    final long deallocations = NATIVE_DEALLOCATIONS.get();
    final long netAllocated = TOTAL_ALLOCATED_BYTES.get();

    sb.append(String.format("=== Memory Statistics ===%n"));
    sb.append(String.format("Native allocations: %,d%n", allocations));
    sb.append(String.format("Native deallocations: %,d%n", deallocations));
    sb.append(String.format("Net allocated bytes: %,d%n", netAllocated));
    sb.append(String.format("%n"));

    // JVM memory information
    final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    final MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

    sb.append(String.format("=== JVM Memory ===%n"));
    sb.append(
        String.format(
            "Heap used: %,d / %,d MB%n",
            heapUsage.getUsed() / (1024 * 1024), heapUsage.getMax() / (1024 * 1024)));

    // GC information
    final long currentGcTime = getCurrentGcTime();
    final long currentGcCollections = getCurrentGcCollections();
    final long gcTimeDelta = currentGcTime - lastGcTime;
    final long gcCollectionsDelta = currentGcCollections - lastGcCollections;

    sb.append(
        String.format(
            "GC time since last check: %d ms (%d collections)%n", gcTimeDelta, gcCollectionsDelta));
    sb.append(String.format("%n"));

    // Operation statistics
    sb.append(String.format("=== Operation Statistics ===%n"));
    if (OPERATION_STATS.isEmpty()) {
      sb.append(String.format("No operations recorded%n"));
    } else {
      for (final OperationStats stats : OPERATION_STATS.values()) {
        sb.append(
            String.format(
                "%-20s: %,8d calls, avg=%.0fns, min=%.0fns, max=%.0fns, slow=%.1f%%%n",
                stats.category,
                stats.getTotalCalls(),
                stats.getAverageTimeNs(),
                (double) stats.getMinTimeNs(),
                (double) stats.getMaxTimeNs(),
                stats.getSlowOperationRate()));
      }
    }

    // Update GC baselines for next call
    lastGcTime = currentGcTime;
    lastGcCollections = currentGcCollections;

    return sb.toString();
  }

  /**
   * Gets operation statistics for a specific category.
   *
   * @param category operation category
   * @return operation statistics or null if not found
   */
  public static String getOperationStats(final String category) {
    final OperationStats stats = OPERATION_STATS.get(category);
    if (stats == null) {
      return "No statistics available for category: " + category;
    }

    return String.format(
        "%s: %,d calls, avg=%.0fns, min=%.0fns, max=%.0fns, slow=%.1f%%",
        category,
        stats.getTotalCalls(),
        stats.getAverageTimeNs(),
        (double) stats.getMinTimeNs(),
        (double) stats.getMaxTimeNs(),
        stats.getSlowOperationRate());
  }

  /** Resets all performance statistics. */
  public static void reset() {
    OPERATION_STATS.clear();
    NATIVE_ALLOCATIONS.set(0);
    NATIVE_DEALLOCATIONS.set(0);
    TOTAL_ALLOCATED_BYTES.set(0);
    TOTAL_JNI_CALLS.set(0);
    TOTAL_JNI_TIME_NS.set(0);
    lastGcTime = getCurrentGcTime();
    lastGcCollections = getCurrentGcCollections();

    LOGGER.info("Performance statistics reset");
  }

  /**
   * Gets the total GC time across all collectors.
   *
   * @return total GC time in milliseconds
   */
  private static long getCurrentGcTime() {
    long totalGcTime = 0;
    final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      final long collectionTime = gcBean.getCollectionTime();
      if (collectionTime > 0) {
        totalGcTime += collectionTime;
      }
    }
    return totalGcTime;
  }

  /**
   * Gets the total number of GC collections across all collectors.
   *
   * @return total number of GC collections
   */
  private static long getCurrentGcCollections() {
    long totalCollections = 0;
    final List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      final long collections = gcBean.getCollectionCount();
      if (collections > 0) {
        totalCollections += collections;
      }
    }
    return totalCollections;
  }

  /**
   * Executes a monitored operation with automatic timing.
   *
   * @param <T> return type
   * @param category operation category
   * @param operation operation to execute
   * @return operation result
   * @throws RuntimeException if operation fails
   */
  public static <T> T monitor(final String category, final MonitoredOperation<T> operation) {
    final long startTime = startOperation(category);
    try {
      return operation.execute();
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      endOperation(category, startTime);
    }
  }

  /**
   * Functional interface for monitored operations.
   *
   * @param <T> return type
   */
  @FunctionalInterface
  public interface MonitoredOperation<T> {
    /**
     * Executes the operation.
     *
     * @return operation result
     * @throws Exception if operation fails
     */
    T execute() throws Exception;
  }

  /**
   * Gets a summary of performance issues if any are detected.
   *
   * @return performance issues summary or null if no issues
   */
  public static String getPerformanceIssues() {
    if (!enabled) {
      return null;
    }

    final StringBuilder issues = new StringBuilder();

    // Check JNI overhead
    final double avgOverhead = getAverageJniOverhead();
    if (avgOverhead > SIMPLE_OPERATION_TARGET_NS) {
      issues.append(
          String.format(
              "• JNI overhead %.0f ns exceeds target %d ns%n",
              avgOverhead, SIMPLE_OPERATION_TARGET_NS));
    }

    // Check memory leaks
    final long netAllocated = TOTAL_ALLOCATED_BYTES.get();
    if (netAllocated > 100 * 1024 * 1024) { // 100MB
      issues.append(
          String.format("• Potential memory leak: %,d bytes net allocated%n", netAllocated));
    }

    // Check for excessive slow operations
    for (final OperationStats stats : OPERATION_STATS.values()) {
      if (stats.getSlowOperationRate() > 5.0) { // More than 5% slow operations
        issues.append(
            String.format(
                "• High slow operation rate for %s: %.1f%%%n",
                stats.category, stats.getSlowOperationRate()));
      }
    }

    return issues.length() > 0
        ? String.format("Performance Issues Detected:%n") + issues.toString()
        : null;
  }

  /**
   * Checks for performance regression and updates baselines.
   *
   * @param category operation category
   * @param stats current operation statistics
   */
  private static void checkPerformanceRegression(
      final String category, final OperationStats stats) {
    final long currentTime = System.currentTimeMillis();
    if (currentTime - lastRegressionCheck < REGRESSION_CHECK_INTERVAL_MS) {
      return;
    }

    // Update check time atomically
    synchronized (PerformanceMonitor.class) {
      if (currentTime - lastRegressionCheck < REGRESSION_CHECK_INTERVAL_MS) {
        return;
      }
      lastRegressionCheck = currentTime;
    }

    final long totalCalls = stats.getTotalCalls();
    if (totalCalls < 100) {
      return; // Not enough data yet
    }

    final double currentAvg = stats.getAverageTimeNs();
    final PerformanceBaseline baseline = BASELINES.get(category);

    if (baseline == null) {
      // Establish new baseline
      final double stdDev = calculateStandardDeviation(stats);
      final PerformanceBaseline newBaseline =
          new PerformanceBaseline(category, currentAvg, stdDev, totalCalls);
      BASELINES.put(category, newBaseline);
      LOGGER.info(
          String.format(
              "Established performance baseline for %s: %.0fns ± %.0fns",
              category, currentAvg, stdDev));
    } else {
      // Check for regression or improvement
      if (baseline.isRegression(currentAvg)) {
        LOGGER.warning(
            String.format(
                "Performance regression detected for %s: %.0fns vs baseline %.0fns",
                category, currentAvg, baseline.baselineAvgNs));
      } else if (baseline.isImprovement(currentAvg)) {
        LOGGER.info(
            String.format(
                "Performance improvement detected for %s: %.0fns vs baseline %.0fns",
                category, currentAvg, baseline.baselineAvgNs));
      }
    }
  }

  /** Calculates standard deviation for operation statistics. */
  private static double calculateStandardDeviation(final OperationStats stats) {
    // Simplified standard deviation calculation
    // In a real implementation, we'd track all individual measurements
    final double avg = stats.getAverageTimeNs();
    final double min = stats.getMinTimeNs();
    final double max = stats.getMaxTimeNs();

    // Rough estimate: assume normal distribution, use range/4 as std dev approximation
    return Math.max((max - min) / 4.0, avg * 0.1); // At least 10% of average
  }

  /**
   * Gets the monitoring overhead as a percentage of total operation time.
   *
   * @return overhead percentage (0.0 to 100.0)
   */
  public static double getMonitoringOverheadPercentage() {
    final long totalMonitoringOverhead = MONITORING_OVERHEAD_NS.get();
    final long totalOperationTime = TOTAL_JNI_TIME_NS.get();

    if (totalOperationTime > 0) {
      return (totalMonitoringOverhead * 100.0) / totalOperationTime;
    }
    return 0.0;
  }

  /**
   * Gets the average monitoring overhead per operation in nanoseconds.
   *
   * @return average overhead per operation
   */
  public static double getAverageMonitoringOverheadNs() {
    final long totalCalls = TOTAL_JNI_CALLS.get();
    final long totalOverhead = MONITORING_OVERHEAD_NS.get();

    return totalCalls > 0 ? (double) totalOverhead / totalCalls : 0.0;
  }

  /**
   * Checks if monitoring overhead meets the target (<5%).
   *
   * @return true if overhead is below 5%
   */
  public static boolean meetsOverheadTarget() {
    return getMonitoringOverheadPercentage() < 5.0;
  }

  /**
   * Enables or disables low overhead monitoring mode.
   *
   * @param enabled true to enable low overhead mode
   */
  public static void setLowOverheadMode(final boolean enabled) {
    lowOverheadMode = enabled;
    if (!enabled) {
      MONITORED_OPERATIONS.set(0);
    }
    LOGGER.info("Low overhead monitoring mode " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Checks if low overhead mode is enabled.
   *
   * @return true if low overhead mode is enabled
   */
  public static boolean isLowOverheadMode() {
    return lowOverheadMode;
  }

  /**
   * Gets comprehensive overhead statistics.
   *
   * @return overhead statistics string
   */
  public static String getOverheadStatistics() {
    final double overheadPercentage = getMonitoringOverheadPercentage();
    final double avgOverheadNs = getAverageMonitoringOverheadNs();
    final boolean meetsTarget = meetsOverheadTarget();
    final long samplingRate = lowOverheadMode ? LOW_OVERHEAD_SAMPLING_RATE : 1;

    return String.format(
        "Monitoring Overhead: %.2f%% %s, avg_overhead=%.0fns/op, sampling_rate=1/%d,"
            + " low_overhead=%b",
        overheadPercentage,
        meetsTarget ? "(✓ meets <5% target)" : "(⚠ exceeds 5% target)",
        avgOverheadNs,
        samplingRate,
        lowOverheadMode);
  }

  /**
   * Gets performance baseline information.
   *
   * @return baseline information string
   */
  public static String getBaselineInformation() {
    if (BASELINES.isEmpty()) {
      return "No performance baselines established yet";
    }

    final StringBuilder sb = new StringBuilder(String.format("Performance Baselines:%n"));
    for (final PerformanceBaseline baseline : BASELINES.values()) {
      sb.append(
          String.format(
              "  %-20s: %.0fns ± %.0fns (%s, %d samples)%n",
              baseline.category,
              baseline.baselineAvgNs,
              baseline.baselineStdDevNs,
              baseline.stable ? "stable" : "establishing",
              baseline.baselineCount));
    }
    return sb.toString();
  }

  /** Forces a performance regression check. */
  public static void forceRegressionCheck() {
    lastRegressionCheck = 0; // Force next check
    for (final OperationStats stats : OPERATION_STATS.values()) {
      checkPerformanceRegression(stats.category, stats);
    }
  }

  /** Resets performance baselines. */
  public static void resetBaselines() {
    BASELINES.clear();
    lastRegressionCheck = System.currentTimeMillis();
    LOGGER.info("Performance baselines reset");
  }
}
