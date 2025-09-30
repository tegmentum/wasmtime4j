package ai.tegmentum.wasmtime4j.diagnostics;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Performance diagnostics and monitoring for WebAssembly operations.
 *
 * <p>This class provides comprehensive performance monitoring capabilities specifically designed
 * for WebAssembly operations, including memory usage tracking, operation timing, garbage
 * collection impact, and thread utilization analysis.
 *
 * <p>Usage example:
 * <pre>{@code
 * PerformanceDiagnostics diagnostics = PerformanceDiagnostics.getInstance();
 * String operationId = diagnostics.startOperation("ModuleCompilation");
 * try {
 *   // ... perform WebAssembly operation
 * } finally {
 *   diagnostics.endOperation(operationId);
 * }
 * PerformanceSnapshot snapshot = diagnostics.captureSnapshot();
 * System.out.println(snapshot.getFormattedReport());
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PerformanceDiagnostics {

  private static final PerformanceDiagnostics INSTANCE = new PerformanceDiagnostics();

  // JMX beans for performance monitoring
  private final MemoryMXBean memoryBean;
  private final RuntimeMXBean runtimeBean;
  private final ThreadMXBean threadBean;
  private final List<GarbageCollectorMXBean> gcBeans;

  // Operation tracking
  private final AtomicLong operationIdCounter = new AtomicLong(0);
  private final Map<String, OperationContext> activeOperations = new ConcurrentHashMap<>();
  private final Map<String, OperationStatistics> operationStats = new ConcurrentHashMap<>();

  // Error handling performance tracking
  private final AtomicLong errorHandlingOverhead = new AtomicLong(0);
  private final AtomicLong errorHandlingOperations = new AtomicLong(0);

  private PerformanceDiagnostics() {
    this.memoryBean = ManagementFactory.getMemoryMXBean();
    this.runtimeBean = ManagementFactory.getRuntimeMXBean();
    this.threadBean = ManagementFactory.getThreadMXBean();
    this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

    // Enable CPU time measurement if supported
    if (threadBean.isCurrentThreadCpuTimeSupported()) {
      threadBean.setThreadCpuTimeEnabled(true);
    }
  }

  /**
   * Gets the singleton instance of the performance diagnostics.
   *
   * @return the performance diagnostics instance
   */
  public static PerformanceDiagnostics getInstance() {
    return INSTANCE;
  }

  /**
   * Starts tracking a WebAssembly operation.
   *
   * @param operationType the type of operation (e.g., "Compilation", "Instantiation", "Execution")
   * @return a unique operation ID for tracking
   * @throws IllegalArgumentException if operationType is null or empty
   */
  public String startOperation(final String operationType) {
    if (operationType == null || operationType.trim().isEmpty()) {
      throw new IllegalArgumentException("Operation type cannot be null or empty");
    }

    final String operationId = operationType + "-" + operationIdCounter.incrementAndGet();
    final OperationContext context = new OperationContext(operationType, Instant.now());

    // Capture initial state
    context.setInitialMemory(memoryBean.getHeapMemoryUsage());
    if (threadBean.isCurrentThreadCpuTimeSupported()) {
      context.setInitialCpuTime(threadBean.getCurrentThreadCpuTime());
    }

    activeOperations.put(operationId, context);
    return operationId;
  }

  /**
   * Ends tracking of a WebAssembly operation.
   *
   * @param operationId the operation ID returned by startOperation
   * @return the duration of the operation in milliseconds, or -1 if operation not found
   */
  public long endOperation(final String operationId) {
    final OperationContext context = activeOperations.remove(operationId);
    if (context == null) {
      return -1;
    }

    final Instant endTime = Instant.now();
    final long duration = Duration.between(context.getStartTime(), endTime).toMillis();

    // Calculate memory usage change
    final MemoryUsage finalMemory = memoryBean.getHeapMemoryUsage();
    final long memoryDelta = finalMemory.getUsed() - context.getInitialMemory().getUsed();

    // Calculate CPU time if available
    long cpuTimeDelta = -1;
    if (threadBean.isCurrentThreadCpuTimeSupported() && context.getInitialCpuTime() >= 0) {
      cpuTimeDelta = (threadBean.getCurrentThreadCpuTime() - context.getInitialCpuTime()) / 1_000_000; // Convert to milliseconds
    }

    // Update statistics
    final OperationStatistics stats = operationStats.computeIfAbsent(
        context.getOperationType(), k -> new OperationStatistics());
    stats.recordOperation(duration, memoryDelta, cpuTimeDelta);

    return duration;
  }

  /**
   * Records error handling overhead for performance analysis.
   *
   * @param overhead the error handling overhead in nanoseconds
   */
  public void recordErrorHandlingOverhead(final long overhead) {
    errorHandlingOverhead.addAndGet(overhead);
    errorHandlingOperations.incrementAndGet();
  }

  /**
   * Gets the average error handling overhead in milliseconds.
   *
   * @return the average error handling overhead, or 0 if no operations recorded
   */
  public double getAverageErrorHandlingOverhead() {
    final long operations = errorHandlingOperations.get();
    if (operations == 0) {
      return 0.0;
    }
    return (double) errorHandlingOverhead.get() / operations / 1_000_000.0; // Convert to milliseconds
  }

  /**
   * Captures a performance snapshot with current system state and operation statistics.
   *
   * @return a performance snapshot
   */
  public PerformanceSnapshot captureSnapshot() {
    final PerformanceSnapshot snapshot = new PerformanceSnapshot();

    // Memory information
    final MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
    final MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
    snapshot.setHeapMemoryUsage(heapMemory);
    snapshot.setNonHeapMemoryUsage(nonHeapMemory);

    // Runtime information
    snapshot.setUptime(runtimeBean.getUptime());

    // Thread information
    snapshot.setThreadCount(threadBean.getThreadCount());
    snapshot.setDaemonThreadCount(threadBean.getDaemonThreadCount());
    snapshot.setPeakThreadCount(threadBean.getPeakThreadCount());

    // Garbage collection information
    long totalGcCollections = 0;
    long totalGcTime = 0;
    for (final GarbageCollectorMXBean gcBean : gcBeans) {
      totalGcCollections += gcBean.getCollectionCount();
      totalGcTime += gcBean.getCollectionTime();
    }
    snapshot.setTotalGcCollections(totalGcCollections);
    snapshot.setTotalGcTime(totalGcTime);

    // Operation statistics
    snapshot.setOperationStatistics(new HashMap<>(operationStats));
    snapshot.setActiveOperationCount(activeOperations.size());

    // Error handling performance
    snapshot.setAverageErrorHandlingOverhead(getAverageErrorHandlingOverhead());
    snapshot.setTotalErrorHandlingOperations(errorHandlingOperations.get());

    snapshot.setCaptureTime(Instant.now());
    return snapshot;
  }

  /**
   * Gets operation statistics for a specific operation type.
   *
   * @param operationType the operation type
   * @return the operation statistics, or null if no operations of this type recorded
   */
  public OperationStatistics getOperationStatistics(final String operationType) {
    return operationStats.get(operationType);
  }

  /**
   * Gets a copy of all operation statistics.
   *
   * @return a map of operation type to statistics
   */
  public Map<String, OperationStatistics> getAllOperationStatistics() {
    return new HashMap<>(operationStats);
  }

  /**
   * Clears all operation statistics and resets counters.
   */
  public void reset() {
    activeOperations.clear();
    operationStats.clear();
    operationIdCounter.set(0);
    errorHandlingOverhead.set(0);
    errorHandlingOperations.set(0);
  }

  /**
   * Checks if performance monitoring is currently enabled.
   *
   * @return true if performance monitoring is enabled
   */
  public boolean isMonitoringEnabled() {
    return DiagnosticConfiguration.getInstance().isPerformanceMonitoringEnabled();
  }

  /**
   * Gets the number of currently active operations.
   *
   * @return the number of active operations
   */
  public int getActiveOperationCount() {
    return activeOperations.size();
  }

  /**
   * Context information for tracking an individual operation.
   */
  private static final class OperationContext {
    private final String operationType;
    private final Instant startTime;
    private MemoryUsage initialMemory;
    private long initialCpuTime = -1;

    OperationContext(final String operationType, final Instant startTime) {
      this.operationType = operationType;
      this.startTime = startTime;
    }

    String getOperationType() {
      return operationType;
    }

    Instant getStartTime() {
      return startTime;
    }

    MemoryUsage getInitialMemory() {
      return initialMemory;
    }

    void setInitialMemory(final MemoryUsage initialMemory) {
      this.initialMemory = initialMemory;
    }

    long getInitialCpuTime() {
      return initialCpuTime;
    }

    void setInitialCpuTime(final long initialCpuTime) {
      this.initialCpuTime = initialCpuTime;
    }
  }
}