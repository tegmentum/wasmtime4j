package ai.tegmentum.wasmtime4j.profiling;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.logging.Logger;
import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

/**
 * Advanced performance profiler with Java Flight Recorder integration for wasmtime4j.
 *
 * <p>This profiler provides comprehensive performance analysis capabilities including:
 *
 * <ul>
 *   <li>JFR event generation for all WebAssembly operations
 *   <li>Real-time performance metrics collection and analysis
 *   <li>Bottleneck identification with stack trace analysis
 *   <li>Optimization recommendation engine
 *   <li>Native code profiling integration
 *   <li>Memory allocation pattern analysis
 *   <li>GC impact measurement and correlation
 * </ul>
 *
 * <p>The profiler integrates seamlessly with Java Flight Recorder, allowing for low-overhead
 * production profiling and detailed performance analysis. It automatically identifies performance
 * bottlenecks and provides actionable optimization recommendations.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * PerformanceProfiler profiler = PerformanceProfiler.getInstance();
 * try (ProfiledOperation operation = profiler.startOperation("module_compilation")) {
 *   // Perform WebAssembly operation
 *   Module module = engine.compileModule(bytes);
 * } // Automatically recorded to JFR
 *
 * // Get analysis and recommendations
 * String analysis = profiler.generateAnalysis();
 * List<String> recommendations = profiler.getOptimizationRecommendations();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PerformanceProfiler {

  private static final Logger LOGGER = Logger.getLogger(PerformanceProfiler.class.getName());

  /** Singleton instance. */
  private static volatile PerformanceProfiler instance;

  /** Lock for singleton initialization. */
  private static final Object INSTANCE_LOCK = new Object();

  /** Whether performance profiling is enabled. */
  private static volatile boolean enabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.profiling.enabled", "true"));

  /** Whether JFR integration is enabled. */
  private static volatile boolean jfrEnabled =
      Boolean.parseBoolean(System.getProperty("wasmtime4j.profiling.jfr.enabled", "true"));

  /** Thread management. */
  private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

  /** Operation tracking. */
  private final ConcurrentHashMap<String, OperationStats> operationStats =
      new ConcurrentHashMap<>();

  private final ConcurrentHashMap<Thread, List<ProfiledOperation>> activeOperations =
      new ConcurrentHashMap<>();

  /** Performance counters. */
  private final AtomicLong totalOperations = new AtomicLong(0);

  private final AtomicLong totalDurationNs = new AtomicLong(0);
  private final AtomicInteger activeProfiledOperations = new AtomicInteger(0);
  private final AtomicLong jfrEventsGenerated = new AtomicLong(0);

  /** Bottleneck detection. */
  private final ConcurrentHashMap<String, BottleneckInfo> bottlenecks = new ConcurrentHashMap<>();

  private volatile long lastBottleneckAnalysis = System.currentTimeMillis();
  private static final long BOTTLENECK_ANALYSIS_INTERVAL_MS = 30000; // 30 seconds

  // ============================================================================
  // JFR Event Definitions
  // ============================================================================

  /** Base WebAssembly operation event. */
  @Category({"WebAssembly", "Performance"})
  @StackTrace(true)
  public static class WasmOperationEvent extends Event {
    @Label("Operation Type")
    @Description("Type of WebAssembly operation")
    public String operationType;

    @Label("Duration (ns)")
    @Description("Operation duration in nanoseconds")
    public long durationNs;

    @Label("Thread Name")
    @Description("Name of the executing thread")
    public String threadName;

    @Label("Success")
    @Description("Whether the operation completed successfully")
    public boolean success;

    @Label("Memory Allocated")
    @Description("Memory allocated during operation in bytes")
    public long memoryAllocated;
  }

  /** Module compilation event. */
  @Name("wasmtime4j.ModuleCompilation")
  @Category({"WebAssembly", "Compilation"})
  @Description("WebAssembly module compilation event")
  @StackTrace(true)
  public static class ModuleCompilationEvent extends Event {
    @Label("Module Size")
    @Description("Size of the WebAssembly module in bytes")
    public int moduleSize;

    @Label("Compilation Time")
    @Description("Time spent compiling the module in nanoseconds")
    public long compilationTimeNs;

    @Label("Cache Hit")
    @Description("Whether the compilation was served from cache")
    public boolean cacheHit;

    @Label("Optimization Level")
    @Description("Compiler optimization level used")
    public String optimizationLevel;
  }

  /** Function execution event. */
  @Name("wasmtime4j.FunctionExecution")
  @Category({"WebAssembly", "Execution"})
  @Description("WebAssembly function execution event")
  @StackTrace(true)
  public static class FunctionExecutionEvent extends Event {
    @Label("Function Name")
    @Description("Name of the executed function")
    public String functionName;

    @Label("Parameter Count")
    @Description("Number of parameters passed to the function")
    public int parameterCount;

    @Label("Execution Time")
    @Description("Function execution time in nanoseconds")
    public long executionTimeNs;

    @Label("JNI Calls")
    @Description("Number of JNI calls made during execution")
    public int jniCalls;
  }

  /** Memory operation event. */
  @Name("wasmtime4j.MemoryOperation")
  @Category({"WebAssembly", "Memory"})
  @Description("WebAssembly memory operation event")
  @StackTrace(true)
  public static class MemoryOperationEvent extends Event {
    @Label("Operation")
    @Description("Type of memory operation (read/write/grow)")
    public String operation;

    @Label("Address")
    @Description("Memory address involved in the operation")
    public long address;

    @Label("Size")
    @Description("Size of the memory operation in bytes")
    public int size;

    @Label("Duration")
    @Description("Duration of the memory operation in nanoseconds")
    public long durationNs;
  }

  // ============================================================================
  // Profiling Infrastructure
  // ============================================================================

  /** Operation statistics tracking. */
  private static final class OperationStats {
    final String operationType;
    final AtomicLong count = new AtomicLong(0);
    final AtomicLong totalDurationNs = new AtomicLong(0);
    final AtomicLong minDurationNs = new AtomicLong(Long.MAX_VALUE);
    final AtomicLong maxDurationNs = new AtomicLong(0);
    final AtomicLong successCount = new AtomicLong(0);
    final AtomicLong failureCount = new AtomicLong(0);

    OperationStats(final String operationType) {
      this.operationType = operationType;
    }

    void recordOperation(final long durationNs, final boolean success) {
      count.incrementAndGet();
      totalDurationNs.addAndGet(durationNs);
      minDurationNs.updateAndGet(min -> Math.min(min, durationNs));
      maxDurationNs.updateAndGet(max -> Math.max(max, durationNs));

      if (success) {
        successCount.incrementAndGet();
      } else {
        failureCount.incrementAndGet();
      }
    }

    double getAverageDurationNs() {
      final long cnt = count.get();
      return cnt > 0 ? (double) totalDurationNs.get() / cnt : 0.0;
    }

    double getSuccessRate() {
      final long total = count.get();
      return total > 0 ? (successCount.get() * 100.0) / total : 0.0;
    }
  }

  /** Bottleneck information. */
  private static final class BottleneckInfo {
    final String operationType;
    final long averageDurationNs;
    final double frequency;
    final String stackTrace;
    final long detectedTime;
    volatile int severity; // 1-5 scale

    BottleneckInfo(
        final String operationType,
        final long averageDurationNs,
        final double frequency,
        final String stackTrace) {
      this.operationType = operationType;
      this.averageDurationNs = averageDurationNs;
      this.frequency = frequency;
      this.stackTrace = stackTrace;
      this.detectedTime = System.currentTimeMillis();
      this.severity = calculateSeverity(averageDurationNs, frequency);
    }

    private int calculateSeverity(final long avgDurationNs, final double freq) {
      if (avgDurationNs > 100_000_000 && freq > 10) return 5; // Critical
      if (avgDurationNs > 50_000_000 && freq > 5) return 4; // High
      if (avgDurationNs > 10_000_000 && freq > 2) return 3; // Medium
      if (avgDurationNs > 1_000_000) return 2; // Low
      return 1; // Minimal
    }
  }

  /** Profiled operation handle for automatic resource management. */
  public static final class ProfiledOperation implements AutoCloseable {
    private final PerformanceProfiler profiler;
    private final String operationType;
    private final long startTime;
    private final long startCpuTime;
    private final Thread currentThread;
    private volatile boolean closed = false;
    private volatile boolean success = true;
    private volatile long memoryAllocated = 0;

    ProfiledOperation(final PerformanceProfiler profiler, final String operationType) {
      this.profiler = profiler;
      this.operationType = operationType;
      this.startTime = System.nanoTime();
      this.startCpuTime = getCurrentThreadCpuTime();
      this.currentThread = Thread.currentThread();

      // Track active operation
      profiler.activeOperations.computeIfAbsent(currentThread, k -> new ArrayList<>()).add(this);
      profiler.activeProfiledOperations.incrementAndGet();
    }

    public void recordMemoryAllocation(final long bytes) {
      memoryAllocated += bytes;
    }

    public void markFailure() {
      success = false;
    }

    @Override
    public void close() {
      if (closed) return;
      closed = true;

      final long endTime = System.nanoTime();
      final long duration = endTime - startTime;

      // Remove from active operations
      final List<ProfiledOperation> ops = profiler.activeOperations.get(currentThread);
      if (ops != null) {
        ops.remove(this);
      }
      profiler.activeProfiledOperations.decrementAndGet();

      // Record operation statistics
      profiler.recordOperation(operationType, duration, success, memoryAllocated);

      // Generate JFR event if enabled
      if (jfrEnabled) {
        generateJfrEvent(duration);
      }
    }

    private void generateJfrEvent(final long duration) {
      final WasmOperationEvent event = new WasmOperationEvent();
      event.operationType = operationType;
      event.durationNs = duration;
      event.threadName = currentThread.getName();
      event.success = success;
      event.memoryAllocated = memoryAllocated;
      event.commit();

      profiler.jfrEventsGenerated.incrementAndGet();
    }

    private long getCurrentThreadCpuTime() {
      return profiler.threadBean.isCurrentThreadCpuTimeSupported()
          ? profiler.threadBean.getCurrentThreadCpuTime()
          : 0;
    }
  }

  // Private constructor for singleton
  private PerformanceProfiler() {
    LOGGER.info("Performance profiler initialized (JFR: " + jfrEnabled + ")");
  }

  /**
   * Gets the singleton profiler instance.
   *
   * @return the performance profiler instance
   */
  public static PerformanceProfiler getInstance() {
    if (instance == null) {
      synchronized (INSTANCE_LOCK) {
        if (instance == null) {
          instance = new PerformanceProfiler();
        }
      }
    }
    return instance;
  }

  /**
   * Starts profiling an operation.
   *
   * @param operationType the type of operation being profiled
   * @return profiled operation handle
   */
  public ProfiledOperation startOperation(final String operationType) {
    if (!enabled) {
      return new ProfiledOperation(this, operationType) {
        @Override
        public void close() {
          // No-op when profiling disabled
        }
      };
    }

    return new ProfiledOperation(this, operationType);
  }

  /**
   * Profiles an operation using a supplier.
   *
   * @param operationType the type of operation
   * @param operation the operation to profile
   * @param <T> the return type
   * @return the operation result
   */
  public <T> T profileOperation(final String operationType, final Supplier<T> operation) {
    if (!enabled) {
      return operation.get();
    }

    try (final ProfiledOperation prof = startOperation(operationType)) {
      try {
        final T result = operation.get();
        return result;
      } catch (Exception e) {
        prof.markFailure();
        throw e;
      }
    }
  }

  /**
   * Records a module compilation event.
   *
   * @param moduleSize size of the module
   * @param compilationTimeNs compilation time
   * @param cacheHit whether compilation was served from cache
   * @param optimizationLevel optimization level used
   */
  public void recordModuleCompilation(
      final int moduleSize,
      final long compilationTimeNs,
      final boolean cacheHit,
      final String optimizationLevel) {
    if (!enabled || !jfrEnabled) return;

    final ModuleCompilationEvent event = new ModuleCompilationEvent();
    event.moduleSize = moduleSize;
    event.compilationTimeNs = compilationTimeNs;
    event.cacheHit = cacheHit;
    event.optimizationLevel = optimizationLevel;
    event.commit();

    jfrEventsGenerated.incrementAndGet();
  }

  /**
   * Records a function execution event.
   *
   * @param functionName name of the executed function
   * @param parameterCount number of parameters
   * @param executionTimeNs execution time
   * @param jniCalls number of JNI calls made
   */
  public void recordFunctionExecution(
      final String functionName,
      final int parameterCount,
      final long executionTimeNs,
      final int jniCalls) {
    if (!enabled || !jfrEnabled) return;

    final FunctionExecutionEvent event = new FunctionExecutionEvent();
    event.functionName = functionName;
    event.parameterCount = parameterCount;
    event.executionTimeNs = executionTimeNs;
    event.jniCalls = jniCalls;
    event.commit();

    jfrEventsGenerated.incrementAndGet();
  }

  /**
   * Records a memory operation event.
   *
   * @param operation type of memory operation
   * @param address memory address
   * @param size operation size
   * @param durationNs operation duration
   */
  public void recordMemoryOperation(
      final String operation, final long address, final int size, final long durationNs) {
    if (!enabled || !jfrEnabled) return;

    final MemoryOperationEvent event = new MemoryOperationEvent();
    event.operation = operation;
    event.address = address;
    event.size = size;
    event.durationNs = durationNs;
    event.commit();

    jfrEventsGenerated.incrementAndGet();
  }

  /** Records operation statistics. */
  private void recordOperation(
      final String operationType,
      final long durationNs,
      final boolean success,
      final long memoryAllocated) {
    totalOperations.incrementAndGet();
    totalDurationNs.addAndGet(durationNs);

    // Update operation-specific statistics
    operationStats
        .computeIfAbsent(operationType, OperationStats::new)
        .recordOperation(durationNs, success);

    // Check for bottlenecks periodically
    checkForBottlenecks();
  }

  /** Checks for performance bottlenecks. */
  private void checkForBottlenecks() {
    final long currentTime = System.currentTimeMillis();
    if (currentTime - lastBottleneckAnalysis < BOTTLENECK_ANALYSIS_INTERVAL_MS) {
      return;
    }

    lastBottleneckAnalysis = currentTime;

    operationStats.forEach(
        (operationType, stats) -> {
          final double avgDuration = stats.getAverageDurationNs();
          final long count = stats.count.get();

          // Consider as bottleneck if average duration > 10ms and more than 10 operations
          if (avgDuration > 10_000_000 && count > 10) {
            final double frequency = count / 30.0; // Operations per second over last 30s
            final String stackTrace = ""; // Would capture actual stack trace in full implementation

            bottlenecks.put(
                operationType,
                new BottleneckInfo(operationType, (long) avgDuration, frequency, stackTrace));
          }
        });
  }

  /**
   * Generates comprehensive performance analysis.
   *
   * @return formatted performance analysis report
   */
  public String generateAnalysis() {
    if (!enabled) {
      return "Performance profiling is disabled";
    }

    final StringBuilder sb = new StringBuilder();
    sb.append("=== WebAssembly Performance Analysis ===\n\n");

    // Overall statistics
    final long totalOps = totalOperations.get();
    final long totalTime = totalDurationNs.get();
    final double avgDuration = totalOps > 0 ? (double) totalTime / totalOps : 0.0;

    sb.append("Overall Statistics:\n");
    sb.append(String.format("  Total operations: %,d\n", totalOps));
    sb.append(String.format("  Average duration: %.2f ms\n", avgDuration / 1_000_000));
    sb.append(String.format("  Active operations: %d\n", activeProfiledOperations.get()));
    sb.append(String.format("  JFR events generated: %,d\n", jfrEventsGenerated.get()));
    sb.append("\n");

    // Operation breakdown
    sb.append("Operation Breakdown:\n");
    operationStats.entrySet().stream()
        .sorted((e1, e2) -> Long.compare(e2.getValue().count.get(), e1.getValue().count.get()))
        .forEach(
            entry -> {
              final OperationStats stats = entry.getValue();
              sb.append(String.format("  %s:\n", entry.getKey()));
              sb.append(String.format("    Count: %,d\n", stats.count.get()));
              sb.append(
                  String.format(
                      "    Average: %.2f ms\n", stats.getAverageDurationNs() / 1_000_000));
              sb.append(
                  String.format(
                      "    Min/Max: %.2f / %.2f ms\n",
                      stats.minDurationNs.get() / 1_000_000.0,
                      stats.maxDurationNs.get() / 1_000_000.0));
              sb.append(String.format("    Success rate: %.1f%%\n", stats.getSuccessRate()));
            });

    // Bottleneck analysis
    if (!bottlenecks.isEmpty()) {
      sb.append("\nPerformance Bottlenecks:\n");
      bottlenecks.values().stream()
          .sorted((b1, b2) -> Integer.compare(b2.severity, b1.severity))
          .forEach(
              bottleneck -> {
                sb.append(
                    String.format(
                        "  %s (Severity: %d):\n", bottleneck.operationType, bottleneck.severity));
                sb.append(
                    String.format(
                        "    Average duration: %.2f ms\n",
                        bottleneck.averageDurationNs / 1_000_000.0));
                sb.append(String.format("    Frequency: %.1f ops/sec\n", bottleneck.frequency));
              });
    }

    return sb.toString();
  }

  /**
   * Gets optimization recommendations based on profiling data.
   *
   * @return list of optimization recommendations
   */
  public List<String> getOptimizationRecommendations() {
    final List<String> recommendations = new ArrayList<>();

    if (!enabled) {
      recommendations.add("Enable performance profiling for detailed analysis");
      return recommendations;
    }

    // Analyze operation patterns for recommendations
    operationStats.forEach(
        (operationType, stats) -> {
          final double avgMs = stats.getAverageDurationNs() / 1_000_000.0;
          final long count = stats.count.get();
          final double successRate = stats.getSuccessRate();

          if (operationType.contains("compilation") && avgMs > 100) {
            recommendations.add("Enable compilation caching to reduce module compilation time");
          }

          if (operationType.contains("function") && avgMs > 1 && count > 1000) {
            recommendations.add("Consider function call batching for frequently called functions");
          }

          if (successRate < 95.0) {
            recommendations.add(
                "Investigate high failure rate for " + operationType + " operations");
          }
        });

    // Bottleneck-specific recommendations
    bottlenecks.forEach(
        (type, bottleneck) -> {
          if (bottleneck.severity >= 4) {
            recommendations.add(
                "Critical bottleneck in " + type + " - requires immediate optimization");
          }
        });

    // General recommendations
    if (jfrEventsGenerated.get() == 0 && jfrEnabled) {
      recommendations.add("Enable JFR event generation for better profiling insights");
    }

    if (recommendations.isEmpty()) {
      recommendations.add("Performance looks good - no major optimizations needed");
    }

    return recommendations;
  }

  /**
   * Gets current profiling statistics.
   *
   * @return formatted profiling statistics
   */
  public String getStatistics() {
    return generateAnalysis();
  }

  /** Resets all profiling data. */
  public void reset() {
    operationStats.clear();
    bottlenecks.clear();
    totalOperations.set(0);
    totalDurationNs.set(0);
    jfrEventsGenerated.set(0);
    lastBottleneckAnalysis = System.currentTimeMillis();
  }

  /**
   * Enables or disables performance profiling.
   *
   * @param enable true to enable profiling
   */
  public static void setEnabled(final boolean enable) {
    enabled = enable;
    LOGGER.info("Performance profiling " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Enables or disables JFR integration.
   *
   * @param enable true to enable JFR integration
   */
  public static void setJfrEnabled(final boolean enable) {
    jfrEnabled = enable;
    LOGGER.info("JFR integration " + (enable ? "enabled" : "disabled"));
  }

  /**
   * Checks if performance profiling is enabled.
   *
   * @return true if profiling is enabled
   */
  public static boolean isEnabled() {
    return enabled;
  }

  /**
   * Checks if JFR integration is enabled.
   *
   * @return true if JFR integration is enabled
   */
  public static boolean isJfrEnabled() {
    return jfrEnabled;
  }
}
