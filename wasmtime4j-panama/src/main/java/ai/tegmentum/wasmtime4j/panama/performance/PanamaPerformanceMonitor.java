package ai.tegmentum.wasmtime4j.panama.performance;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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
 * Panama-specific performance monitoring and profiling infrastructure for WebAssembly operations.
 *
 * <p>This class provides comprehensive performance monitoring capabilities optimized for Panama
 * Foreign Function API operations including:
 *
 * <ul>
 *   <li>Native call timing and frequency tracking via Panama FFI
 *   <li>Memory segment allocation and deallocation monitoring
 *   <li>Arena lifecycle tracking and resource usage analysis
 *   <li>Zero-copy operation performance measurement
 *   <li>Method handle call optimization tracking
 *   <li>Performance regression detection with Panama-specific metrics
 * </ul>
 *
 * <p>The monitor tracks all major WebAssembly operations through Panama FFI and provides actionable
 * performance data for optimization decisions.
 *
 * <p>Usage Example:
 *
 * <pre>{@code
 * // Start monitoring a Panama FFI operation
 * long startTime = PanamaPerformanceMonitor.startOperation("ffi_call", "wasmtime_module_new");
 * try (Arena arena = Arena.ofConfined()) {
 *   // Perform Panama FFI operation
 *   result = methodHandle.invoke(arena, params);
 * } finally {
 *   PanamaPerformanceMonitor.endOperation("ffi_call", startTime);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PanamaPerformanceMonitor {

  private static final Logger LOGGER = Logger.getLogger(PanamaPerformanceMonitor.class.getName());

  /** Whether performance monitoring is enabled. */
  private static volatile boolean enabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.monitoring", "true"));

  /** Whether detailed profiling is enabled. */
  private static volatile boolean profilingEnabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.performance.profiling", "false"));

  /** Performance target for simple Panama operations in nanoseconds. */
  public static final long SIMPLE_PANAMA_OPERATION_TARGET_NS =
      50; // Panama should be faster than JNI

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

  /** Operation statistics by category. */
  private static final ConcurrentHashMap<String, OperationStats> OPERATION_STATS =
      new ConcurrentHashMap<>();

  /** Panama-specific metrics. */
  private static final AtomicLong ARENA_ALLOCATIONS = new AtomicLong(0);

  private static final AtomicLong ARENA_DEALLOCATIONS = new AtomicLong(0);
  private static final AtomicLong TOTAL_ARENA_SIZE_BYTES = new AtomicLong(0);
  private static final AtomicLong MEMORY_SEGMENT_OPERATIONS = new AtomicLong(0);
  private static final AtomicLong METHOD_HANDLE_CALLS = new AtomicLong(0);
  private static final AtomicLong ZERO_COPY_OPERATIONS = new AtomicLong(0);

  /** FFI call tracking. */
  private static final AtomicLong TOTAL_FFI_CALLS = new AtomicLong(0);

  private static final AtomicLong TOTAL_FFI_TIME_NS = new AtomicLong(0);

  /** Arena performance tracking. */
  private static final ConcurrentHashMap<Arena, ArenaStats> ARENA_STATS = new ConcurrentHashMap<>();

  /** GC monitoring. */
  private static volatile long lastGcTime = getCurrentGcTime();

  private static volatile long lastGcCollections = getCurrentGcCollections();

  /** Monitor start time. */
  private static final long MONITOR_START_TIME = System.currentTimeMillis();

  /** Arena statistics for performance tracking. */
  private static final class ArenaStats {
    final Arena arena;
    final long creationTime;
    volatile long lastUsedTime;
    final AtomicLong allocationCount = new AtomicLong(0);
    final AtomicLong totalAllocatedBytes = new AtomicLong(0);
    volatile boolean closed = false;

    ArenaStats(final Arena arena) {
      this.arena = arena;
      this.creationTime = System.currentTimeMillis();
      this.lastUsedTime = creationTime;
    }

    void recordAllocation(final long bytes) {
      allocationCount.incrementAndGet();
      totalAllocatedBytes.addAndGet(bytes);
      lastUsedTime = System.currentTimeMillis();
    }

    void markClosed() {
      closed = true;
    }

    long getLifetimeMs() {
      return System.currentTimeMillis() - creationTime;
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

    // Panama-specific metrics
    final LongAdder arenaOperations = new LongAdder();
    final LongAdder memorySegmentOperations = new LongAdder();
    final LongAdder methodHandleCalls = new LongAdder();

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

    void recordArenaOperation() {
      arenaOperations.increment();
    }

    void recordMemorySegmentOperation() {
      memorySegmentOperations.increment();
    }

    void recordMethodHandleCall() {
      methodHandleCalls.increment();
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
  private PanamaPerformanceMonitor() {}

  /**
   * Enables or disables performance monitoring.
   *
   * @param enable true to enable monitoring
   */
  public static void setEnabled(final boolean enable) {
    enabled = enable;
    LOGGER.info("Panama performance monitoring " + (enable ? "enabled" : "disabled"));
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
   * Starts monitoring a Panama operation.
   *
   * @param category operation category (e.g., "ffi_call", "memory_segment", "arena_alloc")
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
        return -1; // Not sampled - return special marker
      }
    }

    TOTAL_FFI_CALLS.incrementAndGet();

    if (profilingEnabled && details != null) {
      LOGGER.fine("Starting Panama " + category + ": " + details);
    }

    final long operationStartTime = System.nanoTime();
    final long monitoringOverhead = operationStartTime - monitoringStartTime;
    MONITORING_OVERHEAD_NS.addAndGet(monitoringOverhead);

    return operationStartTime;
  }

  /**
   * Starts monitoring a Panama operation.
   *
   * @param category operation category
   * @return start time in nanoseconds for use with {@link #endOperation}
   */
  public static long startOperation(final String category) {
    return startOperation(category, null);
  }

  /**
   * Ends monitoring a Panama operation and records performance data.
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
      return; // This operation was not sampled in low overhead mode
    }

    final long monitoringStartTime = System.nanoTime();
    final long endTimeNs = System.nanoTime();
    final long durationNs = endTimeNs - startTimeNs;

    // Update FFI timing
    TOTAL_FFI_TIME_NS.addAndGet(durationNs);

    // Update operation statistics
    final OperationStats stats = OPERATION_STATS.computeIfAbsent(category, OperationStats::new);
    stats.recordOperation(durationNs);

    // Log slow operations
    final double durationMs = durationNs / 1_000_000.0;
    if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
      LOGGER.warning(String.format("Slow Panama %s operation: %.2fms", category, durationMs));
    }

    // Log profiling information
    if (profilingEnabled) {
      LOGGER.fine(String.format("Completed Panama %s in %.2fms", category, durationMs));
    }

    // Track monitoring overhead
    final long monitoringOverhead = System.nanoTime() - monitoringStartTime;
    MONITORING_OVERHEAD_NS.addAndGet(monitoringOverhead);
  }

  /**
   * Records an Arena allocation for performance tracking.
   *
   * @param arena the arena that was created
   * @param estimatedSizeBytes estimated size of the arena
   */
  public static void recordArenaAllocation(final Arena arena, final long estimatedSizeBytes) {
    if (!enabled || arena == null) {
      return;
    }

    ARENA_ALLOCATIONS.incrementAndGet();
    TOTAL_ARENA_SIZE_BYTES.addAndGet(estimatedSizeBytes);

    final ArenaStats stats = new ArenaStats(arena);
    ARENA_STATS.put(arena, stats);

    if (profilingEnabled) {
      LOGGER.fine("Arena allocated: " + estimatedSizeBytes + " bytes");
    }
  }

  /**
   * Records an Arena deallocation for performance tracking.
   *
   * @param arena the arena that was closed
   */
  public static void recordArenaDeallocation(final Arena arena) {
    if (!enabled || arena == null) {
      return;
    }

    ARENA_DEALLOCATIONS.incrementAndGet();

    final ArenaStats stats = ARENA_STATS.remove(arena);
    if (stats != null) {
      stats.markClosed();
      final long lifetimeMs = stats.getLifetimeMs();

      if (profilingEnabled) {
        LOGGER.fine(
            "Arena deallocated: lifetime="
                + lifetimeMs
                + "ms, allocations="
                + stats.allocationCount.get()
                + ", total_bytes="
                + stats.totalAllocatedBytes.get());
      }
    }
  }

  /**
   * Records a memory segment allocation within an arena.
   *
   * @param arena the parent arena
   * @param segment the allocated memory segment
   */
  public static void recordMemorySegmentAllocation(final Arena arena, final MemorySegment segment) {
    if (!enabled || arena == null || segment == null) {
      return;
    }

    MEMORY_SEGMENT_OPERATIONS.incrementAndGet();

    final ArenaStats arenaStats = ARENA_STATS.get(arena);
    if (arenaStats != null) {
      arenaStats.recordAllocation(segment.byteSize());
    }

    // Record in operation stats
    final OperationStats stats =
        OPERATION_STATS.computeIfAbsent("memory_segment", OperationStats::new);
    stats.recordMemorySegmentOperation();
  }

  /**
   * Records a method handle call for performance tracking.
   *
   * @param methodName the name of the native method being called
   */
  public static void recordMethodHandleCall(final String methodName) {
    if (!enabled) {
      return;
    }

    METHOD_HANDLE_CALLS.incrementAndGet();

    final OperationStats stats =
        OPERATION_STATS.computeIfAbsent("method_handle", OperationStats::new);
    stats.recordMethodHandleCall();

    if (profilingEnabled && methodName != null) {
      LOGGER.fine("Method handle call: " + methodName);
    }
  }

  /** Records a zero-copy operation for performance tracking. */
  public static void recordZeroCopyOperation() {
    if (!enabled) {
      return;
    }

    ZERO_COPY_OPERATIONS.incrementAndGet();
  }

  /**
   * Gets the current Panama FFI call overhead in nanoseconds per call.
   *
   * @return average FFI overhead per call
   */
  public static double getAverageFfiOverhead() {
    final long totalCalls = TOTAL_FFI_CALLS.get();
    final long totalTime = TOTAL_FFI_TIME_NS.get();
    return totalCalls > 0 ? (double) totalTime / totalCalls : 0.0;
  }

  /**
   * Checks if the current Panama FFI overhead meets the performance target.
   *
   * @return true if overhead is below target
   */
  public static boolean meetsPerformanceTarget() {
    return getAverageFfiOverhead() < SIMPLE_PANAMA_OPERATION_TARGET_NS;
  }

  /**
   * Gets comprehensive Panama performance statistics.
   *
   * @return formatted performance statistics including Panama-specific metrics
   */
  public static String getStatistics() {
    if (!enabled) {
      return "Panama performance monitoring is disabled";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("=== Panama WebAssembly Performance Statistics ===%n"));

    // Overall metrics
    final long uptimeMs = System.currentTimeMillis() - MONITOR_START_TIME;
    final long totalCalls = TOTAL_FFI_CALLS.get();
    final double avgOverhead = getAverageFfiOverhead();
    final boolean meetsTarget = avgOverhead < SIMPLE_PANAMA_OPERATION_TARGET_NS;

    sb.append(String.format("Uptime: %d ms%n", uptimeMs));
    sb.append(String.format("Total Panama FFI calls: %,d%n", totalCalls));
    sb.append(
        String.format(
            "Average FFI overhead: %.0f ns/call %s%n",
            avgOverhead, meetsTarget ? "(✓ meets target)" : "(⚠ exceeds target)"));
    sb.append(String.format("Performance target: %d ns/call%n", SIMPLE_PANAMA_OPERATION_TARGET_NS));
    sb.append(String.format("%n"));

    // Panama-specific metrics
    final long arenaAllocations = ARENA_ALLOCATIONS.get();
    final long arenaDeallocations = ARENA_DEALLOCATIONS.get();
    final long netArenaSize = TOTAL_ARENA_SIZE_BYTES.get();
    final long activeArenas = arenaAllocations - arenaDeallocations;

    sb.append(String.format("=== Panama Memory Statistics ===%n"));
    sb.append(String.format("Arena allocations: %,d%n", arenaAllocations));
    sb.append(String.format("Arena deallocations: %,d%n", arenaDeallocations));
    sb.append(String.format("Active arenas: %,d%n", activeArenas));
    sb.append(String.format("Net arena size: %,d bytes%n", netArenaSize));
    sb.append(String.format("Memory segment operations: %,d%n", MEMORY_SEGMENT_OPERATIONS.get()));
    sb.append(String.format("Method handle calls: %,d%n", METHOD_HANDLE_CALLS.get()));
    sb.append(String.format("Zero-copy operations: %,d%n", ZERO_COPY_OPERATIONS.get()));
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
    sb.append(String.format("=== Panama Operation Statistics ===%n"));
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

  /** Resets all performance statistics. */
  public static void reset() {
    OPERATION_STATS.clear();
    ARENA_STATS.clear();
    ARENA_ALLOCATIONS.set(0);
    ARENA_DEALLOCATIONS.set(0);
    TOTAL_ARENA_SIZE_BYTES.set(0);
    MEMORY_SEGMENT_OPERATIONS.set(0);
    METHOD_HANDLE_CALLS.set(0);
    ZERO_COPY_OPERATIONS.set(0);
    TOTAL_FFI_CALLS.set(0);
    TOTAL_FFI_TIME_NS.set(0);
    lastGcTime = getCurrentGcTime();
    lastGcCollections = getCurrentGcCollections();

    LOGGER.info("Panama performance statistics reset");
  }

  /**
   * Gets Panama-specific performance metrics as a formatted string.
   *
   * @return Panama performance metrics
   */
  public static String getPanamaMetrics() {
    final long totalCalls = TOTAL_FFI_CALLS.get();
    final double avgOverhead = getAverageFfiOverhead();
    final boolean meetsTarget = avgOverhead < SIMPLE_PANAMA_OPERATION_TARGET_NS;
    final long arenaOps = ARENA_ALLOCATIONS.get();
    final long memSegOps = MEMORY_SEGMENT_OPERATIONS.get();
    final long methodHandleCalls = METHOD_HANDLE_CALLS.get();
    final long zeroCopyOps = ZERO_COPY_OPERATIONS.get();

    return String.format(
        "Panama Performance: ffi_calls=%d, avg_overhead=%.0fns %s, "
            + "arena_ops=%d, memseg_ops=%d, methodhandle_calls=%d, zero_copy=%d",
        totalCalls,
        avgOverhead,
        meetsTarget ? "(✓)" : "(⚠)",
        arenaOps,
        memSegOps,
        methodHandleCalls,
        zeroCopyOps);
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
   * Executes a monitored Panama operation with automatic timing.
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
   * Functional interface for monitored Panama operations.
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
   * Gets active arena statistics.
   *
   * @return active arena statistics
   */
  public static String getActiveArenaStats() {
    if (!enabled || ARENA_STATS.isEmpty()) {
      return "No active arenas";
    }

    final StringBuilder sb = new StringBuilder(String.format("Active Arena Statistics:%n"));
    for (final ArenaStats stats : ARENA_STATS.values()) {
      if (!stats.closed) {
        sb.append(
            String.format(
                "  Arena: lifetime=%dms, allocations=%d, total_bytes=%d%n",
                stats.getLifetimeMs(),
                stats.allocationCount.get(),
                stats.totalAllocatedBytes.get()));
      }
    }
    return sb.toString();
  }

  /**
   * Checks for potential Panama performance issues.
   *
   * @return performance issues summary or null if no issues
   */
  public static String getPerformanceIssues() {
    if (!enabled) {
      return null;
    }

    final StringBuilder issues = new StringBuilder();

    // Check FFI overhead
    final double avgOverhead = getAverageFfiOverhead();
    if (avgOverhead > SIMPLE_PANAMA_OPERATION_TARGET_NS) {
      issues.append(
          String.format(
              "• Panama FFI overhead %.0f ns exceeds target %d ns%n",
              avgOverhead, SIMPLE_PANAMA_OPERATION_TARGET_NS));
    }

    // Check for arena leaks
    final long activeArenas = ARENA_ALLOCATIONS.get() - ARENA_DEALLOCATIONS.get();
    if (activeArenas > 1000) { // More than 1000 active arenas might indicate leaks
      issues.append(String.format("• Potential arena leak: %,d active arenas%n", activeArenas));
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
        ? String.format("Panama Performance Issues Detected:%n") + issues.toString()
        : null;
  }
}
