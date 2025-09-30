package ai.tegmentum.wasmtime4j.jni.profiling;

import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler;
import ai.tegmentum.wasmtime4j.profiling.FlameGraphGenerator;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of advanced profiling capabilities for wasmtime4j.
 *
 * <p>This class provides JNI-based profiling with native code integration, offering low-overhead
 * performance monitoring and flame graph generation specifically optimized for JNI-based
 * WebAssembly operations.
 *
 * @since 1.0.0
 */
public final class JniAdvancedProfiler implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniAdvancedProfiler.class.getName());

  /** Native profiler handle. */
  private long nativeProfilerHandle;

  /** Native flame graph collector handle. */
  private long nativeFlameGraphHandle;

  private final AdvancedProfiler.ProfilerConfiguration config;
  private final FlameGraphGenerator flameGraphGenerator;
  private final AtomicLong totalSamples = new AtomicLong(0);
  private volatile boolean profiling = false;
  private volatile Instant profilingStartTime;

  // Native method declarations
  private static native long nativeCreateProfiler(
      long samplingIntervalMs,
      int maxSamples,
      boolean enableMemoryProfiling,
      boolean enableJfrIntegration,
      double maxOverheadPercent);

  private static native boolean nativeStartProfiling(long profilerHandle);

  private static native boolean nativeStopProfiling(long profilerHandle);

  private static native void nativeDestroyProfiler(long profilerHandle);

  private static native long nativeCreateFlameGraphCollector(
      int maxSamples, long samplingIntervalMs);

  private static native boolean nativeStartFlameGraphCollection(long collectorHandle);

  private static native boolean nativeStopFlameGraphCollection(long collectorHandle);

  private static native boolean nativeRecordStackSample(
      long collectorHandle,
      String functionName,
      String fileName,
      int lineNumber,
      long durationNanos);

  private static native String nativeExportFlameGraphSvg(
      long collectorHandle, int width, int height);

  private static native void nativeDestroyFlameGraphCollector(long collectorHandle);

  private static native boolean nativeRecordFunctionExecution(
      long profilerHandle, String functionName, long executionTimeNanos, long memoryDelta);

  private static native long nativeRecordMemoryAllocation(
      long profilerHandle, long size, String allocationType);

  private static native void nativeRecordMemoryDeallocation(long profilerHandle, long allocationId);

  private static native ProfilerStatistics nativeGetStatistics(long profilerHandle);

  private static native void nativeReset(long profilerHandle);

  // Statistics holder for native data transfer
  public static final class ProfilerStatistics {
    public final long totalSamples;
    public final long functionCalls;
    public final long totalExecutionTimeNanos;
    public final long memoryAllocations;
    public final long totalAllocatedBytes;
    public final double cpuUsagePercent;
    public final long activeThreads;

    public ProfilerStatistics(
        final long totalSamples,
        final long functionCalls,
        final long totalExecutionTimeNanos,
        final long memoryAllocations,
        final long totalAllocatedBytes,
        final double cpuUsagePercent,
        final long activeThreads) {
      this.totalSamples = totalSamples;
      this.functionCalls = functionCalls;
      this.totalExecutionTimeNanos = totalExecutionTimeNanos;
      this.memoryAllocations = memoryAllocations;
      this.totalAllocatedBytes = totalAllocatedBytes;
      this.cpuUsagePercent = cpuUsagePercent;
      this.activeThreads = activeThreads;
    }
  }

  public JniAdvancedProfiler() {
    this(AdvancedProfiler.ProfilerConfiguration.builder().build());
  }

  public JniAdvancedProfiler(final AdvancedProfiler.ProfilerConfiguration config) {
    this.config = Objects.requireNonNull(config);
    this.flameGraphGenerator = new FlameGraphGenerator();

    // Create native profiler
    nativeProfilerHandle =
        nativeCreateProfiler(
            config.getSamplingInterval().toMillis(),
            config.getMaxSamples(),
            config.isMemoryProfilingEnabled(),
            config.isJfrIntegrationEnabled(),
            config.getMaxOverheadPercent());

    if (nativeProfilerHandle == 0) {
      throw new RuntimeException("Failed to create native profiler");
    }

    // Create native flame graph collector if enabled
    if (config.isFlameGraphsEnabled()) {
      nativeFlameGraphHandle =
          nativeCreateFlameGraphCollector(
              config.getMaxSamples(), config.getSamplingInterval().toMillis());

      if (nativeFlameGraphHandle == 0) {
        LOGGER.warning("Failed to create native flame graph collector");
      }
    }

    LOGGER.info("JNI advanced profiler initialized");
  }

  /**
   * Starts profiling with the configured settings.
   *
   * @return profiling session handle
   */
  public ProfilingSession startProfiling() {
    return startProfiling(Duration.ofMinutes(5));
  }

  /**
   * Starts profiling session with specified duration.
   *
   * @param duration maximum profiling duration
   * @return profiling session handle
   */
  public ProfilingSession startProfiling(final Duration duration) {
    if (profiling) {
      throw new IllegalStateException("Profiling is already active");
    }

    // Start native profiling
    if (!nativeStartProfiling(nativeProfilerHandle)) {
      throw new RuntimeException("Failed to start native profiling");
    }

    // Start flame graph collection if available
    if (nativeFlameGraphHandle != 0) {
      nativeStartFlameGraphCollection(nativeFlameGraphHandle);
    }

    profiling = true;
    profilingStartTime = Instant.now();
    flameGraphGenerator.startCollection();

    LOGGER.info("Started JNI profiling session for " + duration);

    return new ProfilingSession(this, duration);
  }

  /** Stops profiling and collects final results. */
  public void stopProfiling() {
    if (!profiling) {
      return;
    }

    profiling = false;
    flameGraphGenerator.stopCollection();

    // Stop native profiling
    nativeStopProfiling(nativeProfilerHandle);

    // Stop flame graph collection if available
    if (nativeFlameGraphHandle != 0) {
      nativeStopFlameGraphCollection(nativeFlameGraphHandle);
    }

    final Duration totalTime = Duration.between(profilingStartTime, Instant.now());
    LOGGER.info("Stopped JNI profiling session after " + totalTime);
  }

  /**
   * Records a function execution for profiling.
   *
   * @param functionName name of the executed function
   * @param executionTime execution duration
   * @param memoryAllocated memory allocated during execution
   */
  public void recordFunctionExecution(
      final String functionName, final Duration executionTime, final long memoryAllocated) {
    if (!profiling) return;

    totalSamples.incrementAndGet();

    // Record in native profiler
    nativeRecordFunctionExecution(
        nativeProfilerHandle, functionName, executionTime.toNanos(), memoryAllocated);

    // Record stack trace for flame graph if enabled
    if (nativeFlameGraphHandle != 0 && config.isStackTraceCollectionEnabled()) {
      recordStackTraceForFlameGraph(functionName, executionTime);
    }
  }

  /**
   * Records memory allocation for profiling.
   *
   * @param size allocation size in bytes
   * @param allocationType type of allocation
   * @return allocation ID for tracking
   */
  public long recordMemoryAllocation(final long size, final String allocationType) {
    if (!profiling) return 0;

    return nativeRecordMemoryAllocation(nativeProfilerHandle, size, allocationType);
  }

  /**
   * Records memory deallocation.
   *
   * @param allocationId allocation ID to deallocate
   */
  public void recordMemoryDeallocation(final long allocationId) {
    if (!profiling) {
      return;
    }

    nativeRecordMemoryDeallocation(nativeProfilerHandle, allocationId);
  }

  /**
   * Profiles a JNI operation with automatic measurement.
   *
   * @param operationName name of the operation
   * @param jniCall JNI operation to profile
   * @param <T> return type
   * @return operation result
   */
  public <T> T profileJniOperation(final String operationName, final JniOperation<T> jniCall) {
    if (!profiling) {
      return jniCall.execute();
    }

    final long startTime = System.nanoTime();
    final long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    try {
      final T result = jniCall.execute();
      final long endTime = System.nanoTime();
      final long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      final Duration executionTime = Duration.ofNanos(endTime - startTime);
      final long memoryDelta = Math.max(0, endMemory - startMemory);

      recordFunctionExecution(operationName, executionTime, memoryDelta);
      return result;
    } catch (Exception e) {
      final long endTime = System.nanoTime();
      final Duration executionTime = Duration.ofNanos(endTime - startTime);
      recordFunctionExecution(operationName + "(failed)", executionTime, 0);
      throw new RuntimeException("JNI operation failed: " + operationName, e);
    }
  }

  /**
   * Generates flame graph from collected profiling data.
   *
   * @return flame graph root frame
   */
  public FlameGraphGenerator.FlameFrame generateFlameGraph() {
    if (!config.isFlameGraphsEnabled() || nativeFlameGraphHandle == 0) {
      throw new IllegalStateException(
          "Flame graphs are not enabled or native collector is not available");
    }

    return flameGraphGenerator.generateFlameGraph();
  }

  /**
   * Generates and saves flame graph as SVG using native implementation.
   *
   * @param outputPath path to save the flame graph
   * @param width SVG width in pixels
   * @param height SVG height in pixels
   * @throws IOException if saving fails
   */
  public void saveNativeFlameGraphAsSvg(final Path outputPath, final int width, final int height)
      throws IOException {
    if (nativeFlameGraphHandle == 0) {
      throw new IllegalStateException("Native flame graph collector is not available");
    }

    final String svgContent = nativeExportFlameGraphSvg(nativeFlameGraphHandle, width, height);
    if (svgContent == null || svgContent.isEmpty()) {
      throw new IOException("Failed to generate SVG from native flame graph collector");
    }

    java.nio.file.Files.writeString(outputPath, svgContent);
    LOGGER.info("Saved native flame graph to: " + outputPath);
  }

  /**
   * Gets comprehensive profiling statistics from native implementation.
   *
   * @return native profiling statistics
   */
  public ProfilerStatistics getNativeStatistics() {
    return nativeGetStatistics(nativeProfilerHandle);
  }

  /**
   * Gets Java-side profiling statistics.
   *
   * @return Java profiling statistics
   */
  public AdvancedProfiler.ProfilingStatistics getJavaStatistics() {
    final ProfilerStatistics nativeStats = getNativeStatistics();
    final Duration totalTime =
        profilingStartTime != null
            ? Duration.between(profilingStartTime, Instant.now())
            : Duration.ZERO;

    return new AdvancedProfiler.ProfilingStatistics(
        totalTime,
        nativeStats.totalSamples,
        nativeStats.functionCalls,
        nativeStats.totalExecutionTimeNanos,
        nativeStats.memoryAllocations,
        nativeStats.totalAllocatedBytes,
        0, // JNI calls (tracked separately)
        0, // Panama calls (not applicable)
        totalSamples.get(),
        nativeStats.totalAllocatedBytes,
        Collections.emptyMap());
  }

  /** Resets all profiling data. */
  public void reset() {
    nativeReset(nativeProfilerHandle);
    totalSamples.set(0);
    flameGraphGenerator.getStackTraceCollector().clear();
    LOGGER.info("Reset JNI profiling data");
  }

  @Override
  public void close() {
    stopProfiling();

    if (nativeFlameGraphHandle != 0) {
      nativeDestroyFlameGraphCollector(nativeFlameGraphHandle);
      nativeFlameGraphHandle = 0;
    }

    if (nativeProfilerHandle != 0) {
      nativeDestroyProfiler(nativeProfilerHandle);
      nativeProfilerHandle = 0;
    }

    LOGGER.info("JNI advanced profiler closed");
  }

  // Private helper methods

  private void recordStackTraceForFlameGraph(
      final String functionName, final Duration executionTime) {
    // Get current stack trace
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    // Record in native flame graph collector
    for (int i = stackTrace.length - 1; i >= 2; i--) { // Skip getStackTrace and this method
      final StackTraceElement element = stackTrace[i];
      nativeRecordStackSample(
          nativeFlameGraphHandle,
          element.getMethodName(),
          element.getFileName() != null ? element.getFileName() : "unknown",
          element.getLineNumber() > 0 ? element.getLineNumber() : 0,
          executionTime.toNanos() / stackTrace.length // Distribute time across stack
      );
    }

    // Also record in Java flame graph generator
    final List<String> stackTraceStrings =
        Arrays.stream(stackTrace)
            .skip(2) // Skip getStackTrace and this method
            .map(StackTraceElement::toString)
            .collect(java.util.stream.Collectors.toList());

    flameGraphGenerator.recordSample(
        executionTime,
        stackTraceStrings,
        Thread.currentThread().getName(),
        Map.of("function_name", functionName, "runtime", "JNI"));
  }

  /** Functional interface for JNI operations. */
  @FunctionalInterface
  public interface JniOperation<T> {
    T execute() throws Exception;
  }

  /** Profiling session handle for JNI-based profiling. */
  public static final class ProfilingSession implements AutoCloseable {
    private final JniAdvancedProfiler profiler;
    private final Duration maxDuration;
    private final Instant startTime;
    private volatile boolean closed = false;

    private ProfilingSession(final JniAdvancedProfiler profiler, final Duration maxDuration) {
      this.profiler = profiler;
      this.maxDuration = maxDuration;
      this.startTime = Instant.now();
    }

    /** Stops the profiling session. */
    public void stop() {
      if (!closed) {
        closed = true;
        profiler.stopProfiling();
      }
    }

    /**
     * Gets current profiling statistics.
     *
     * @return profiling statistics
     */
    public AdvancedProfiler.ProfilingStatistics getStatistics() {
      return profiler.getJavaStatistics();
    }

    /**
     * Gets native profiling statistics.
     *
     * @return native profiling statistics
     */
    public ProfilerStatistics getNativeStatistics() {
      return profiler.getNativeStatistics();
    }

    /**
     * Generates flame graph from current session data.
     *
     * @return flame graph root frame
     */
    public FlameGraphGenerator.FlameFrame generateFlameGraph() {
      return profiler.generateFlameGraph();
    }

    /**
     * Saves native flame graph as SVG.
     *
     * @param outputPath output file path
     * @param width SVG width
     * @param height SVG height
     * @throws IOException if saving fails
     */
    public void saveNativeFlameGraphAsSvg(final Path outputPath, final int width, final int height)
        throws IOException {
      profiler.saveNativeFlameGraphAsSvg(outputPath, width, height);
    }

    /**
     * Checks if the session has exceeded its maximum duration.
     *
     * @return true if session should be stopped
     */
    public boolean hasExceededDuration() {
      return Duration.between(startTime, Instant.now()).compareTo(maxDuration) > 0;
    }

    @Override
    public void close() {
      stop();
    }
  }

  // Static initialization
  static {
    try {
      System.loadLibrary("wasmtime4j");
      LOGGER.info("Loaded wasmtime4j native library for JNI advanced profiling");
    } catch (UnsatisfiedLinkError e) {
      LOGGER.severe("Failed to load wasmtime4j native library: " + e.getMessage());
      throw new RuntimeException("Failed to load native library for JNI advanced profiling", e);
    }
  }

  public AdvancedProfiler.ProfilerConfiguration getConfig() {
    return config;
  }

  public boolean isProfiling() {
    return profiling;
  }

  public long getTotalSamples() {
    return totalSamples.get();
  }
}
