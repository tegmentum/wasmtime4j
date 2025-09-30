package ai.tegmentum.wasmtime4j.panama.profiling;

import ai.tegmentum.wasmtime4j.profiling.AdvancedProfiler;
import ai.tegmentum.wasmtime4j.profiling.FlameGraphGenerator;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of advanced profiling capabilities for wasmtime4j.
 *
 * <p>This class provides Panama Foreign Function Interface-based profiling with direct native code
 * integration, offering efficient performance monitoring and flame graph generation optimized for
 * Panama FFI-based WebAssembly operations.
 *
 * @since 1.0.0
 */
public final class PanamaAdvancedProfiler implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PanamaAdvancedProfiler.class.getName());

  // Native library and function handles
  private static final SymbolLookup NATIVE_LOOKUP;
  private static final MethodHandle CREATE_PROFILER_HANDLE;
  private static final MethodHandle START_PROFILING_HANDLE;
  private static final MethodHandle STOP_PROFILING_HANDLE;
  private static final MethodHandle DESTROY_PROFILER_HANDLE;
  private static final MethodHandle CREATE_FLAME_GRAPH_COLLECTOR_HANDLE;
  private static final MethodHandle START_FLAME_GRAPH_COLLECTION_HANDLE;
  private static final MethodHandle STOP_FLAME_GRAPH_COLLECTION_HANDLE;
  private static final MethodHandle RECORD_STACK_SAMPLE_HANDLE;
  private static final MethodHandle EXPORT_FLAME_GRAPH_SVG_HANDLE;
  private static final MethodHandle DESTROY_FLAME_GRAPH_COLLECTOR_HANDLE;
  private static final MethodHandle RECORD_FUNCTION_EXECUTION_HANDLE;
  private static final MethodHandle RECORD_MEMORY_ALLOCATION_HANDLE;
  private static final MethodHandle RECORD_MEMORY_DEALLOCATION_HANDLE;
  private static final MethodHandle GET_DASHBOARD_HANDLE;

  // Memory layouts for native structures
  private static final GroupLayout PROFILER_STATISTICS_LAYOUT =
      MemoryLayout.structLayout(
          ValueLayout.JAVA_LONG.withName("total_samples"),
          ValueLayout.JAVA_LONG.withName("function_calls"),
          ValueLayout.JAVA_LONG.withName("total_execution_time_nanos"),
          ValueLayout.JAVA_LONG.withName("memory_allocations"),
          ValueLayout.JAVA_LONG.withName("total_allocated_bytes"),
          ValueLayout.JAVA_DOUBLE.withName("cpu_usage_percent"),
          ValueLayout.JAVA_LONG.withName("active_threads"));

  static {
    try {
      // Load native library
      System.loadLibrary("wasmtime4j");
      NATIVE_LOOKUP = SymbolLookup.loaderLookup();

      // Initialize method handles
      final Linker linker = Linker.nativeLinker();

      CREATE_PROFILER_HANDLE =
          createHandle(
              linker, "wasmtime4j_profiler_create", MethodType.methodType(MemorySegment.class));

      START_PROFILING_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_start",
              MethodType.methodType(boolean.class, MemorySegment.class));

      STOP_PROFILING_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_stop",
              MethodType.methodType(boolean.class, MemorySegment.class));

      DESTROY_PROFILER_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_destroy",
              MethodType.methodType(void.class, MemorySegment.class));

      CREATE_FLAME_GRAPH_COLLECTOR_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_flame_graph_collector_create",
              MethodType.methodType(MemorySegment.class, int.class, long.class));

      START_FLAME_GRAPH_COLLECTION_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_flame_graph_collector_start",
              MethodType.methodType(boolean.class, MemorySegment.class));

      STOP_FLAME_GRAPH_COLLECTION_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_flame_graph_collector_stop",
              MethodType.methodType(boolean.class, MemorySegment.class));

      RECORD_STACK_SAMPLE_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_flame_graph_collector_record_sample",
              MethodType.methodType(
                  boolean.class,
                  MemorySegment.class,
                  MemorySegment.class,
                  MemorySegment.class,
                  int.class,
                  long.class));

      EXPORT_FLAME_GRAPH_SVG_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_flame_graph_collector_export_svg",
              MethodType.methodType(
                  boolean.class,
                  MemorySegment.class,
                  int.class,
                  int.class,
                  MemorySegment.class,
                  int.class));

      DESTROY_FLAME_GRAPH_COLLECTOR_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_flame_graph_collector_destroy",
              MethodType.methodType(void.class, MemorySegment.class));

      RECORD_FUNCTION_EXECUTION_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_record_function",
              MethodType.methodType(
                  boolean.class, MemorySegment.class, MemorySegment.class, long.class, long.class));

      RECORD_MEMORY_ALLOCATION_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_record_allocation",
              MethodType.methodType(
                  long.class, MemorySegment.class, long.class, MemorySegment.class));

      RECORD_MEMORY_DEALLOCATION_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_record_deallocation",
              MethodType.methodType(void.class, MemorySegment.class, long.class));

      GET_DASHBOARD_HANDLE =
          createHandle(
              linker,
              "wasmtime4j_profiler_get_dashboard",
              MethodType.methodType(boolean.class, MemorySegment.class, MemorySegment.class));

      LOGGER.info("Panama Advanced Profiler initialized successfully");
    } catch (Exception e) {
      LOGGER.severe("Failed to initialize Panama Advanced Profiler: " + e.getMessage());
      throw new RuntimeException("Failed to initialize Panama FFI profiling", e);
    }
  }

  private static MethodHandle createHandle(
      final Linker linker, final String symbolName, final MethodType methodType) {
    return NATIVE_LOOKUP
        .find(symbolName)
        .map(
            addr ->
                linker.downcallHandle(
                    addr,
                    FunctionDescriptor.of(methodType.returnType(), methodType.parameterArray())))
        .orElseThrow(() -> new RuntimeException("Symbol not found: " + symbolName));
  }

  /** Native profiler handle. */
  private MemorySegment nativeProfilerHandle;

  /** Native flame graph collector handle. */
  private MemorySegment nativeFlameGraphHandle;

  private final AdvancedProfiler.ProfilerConfiguration config;
  private final FlameGraphGenerator flameGraphGenerator;
  private final AtomicLong totalSamples = new AtomicLong(0);
  private volatile boolean profiling = false;
  private volatile Instant profilingStartTime;
  private final Arena arena;

  public PanamaAdvancedProfiler() {
    this(AdvancedProfiler.ProfilerConfiguration.builder().build());
  }

  public PanamaAdvancedProfiler(final AdvancedProfiler.ProfilerConfiguration config) {
    this.config = Objects.requireNonNull(config);
    this.flameGraphGenerator = new FlameGraphGenerator();
    this.arena = Arena.openConfined();

    try {
      // Create native profiler
      nativeProfilerHandle = (MemorySegment) CREATE_PROFILER_HANDLE.invoke();
      if (nativeProfilerHandle.address() == 0) {
        throw new RuntimeException("Failed to create native profiler");
      }

      // Create native flame graph collector if enabled
      if (config.isFlameGraphsEnabled()) {
        nativeFlameGraphHandle =
            (MemorySegment)
                CREATE_FLAME_GRAPH_COLLECTOR_HANDLE.invoke(
                    config.getMaxSamples(), config.getSamplingInterval().toMillis());

        if (nativeFlameGraphHandle.address() == 0) {
          LOGGER.warning("Failed to create native flame graph collector");
        }
      }

      LOGGER.info("Panama advanced profiler initialized");
    } catch (Throwable e) {
      throw new RuntimeException("Failed to initialize Panama profiler", e);
    }
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

    try {
      // Start native profiling
      final boolean started = (boolean) START_PROFILING_HANDLE.invoke(nativeProfilerHandle);
      if (!started) {
        throw new RuntimeException("Failed to start native profiling");
      }

      // Start flame graph collection if available
      if (nativeFlameGraphHandle != null && nativeFlameGraphHandle.address() != 0) {
        START_FLAME_GRAPH_COLLECTION_HANDLE.invoke(nativeFlameGraphHandle);
      }

      profiling = true;
      profilingStartTime = Instant.now();
      flameGraphGenerator.startCollection();

      LOGGER.info("Started Panama profiling session for " + duration);

      return new ProfilingSession(this, duration);
    } catch (Throwable e) {
      throw new RuntimeException("Failed to start Panama profiling", e);
    }
  }

  /** Stops profiling and collects final results. */
  public void stopProfiling() {
    if (!profiling) {
      return;
    }

    profiling = false;
    flameGraphGenerator.stopCollection();

    try {
      // Stop native profiling
      STOP_PROFILING_HANDLE.invoke(nativeProfilerHandle);

      // Stop flame graph collection if available
      if (nativeFlameGraphHandle != null && nativeFlameGraphHandle.address() != 0) {
        STOP_FLAME_GRAPH_COLLECTION_HANDLE.invoke(nativeFlameGraphHandle);
      }

      final Duration totalTime = Duration.between(profilingStartTime, Instant.now());
      LOGGER.info("Stopped Panama profiling session after " + totalTime);
    } catch (Throwable e) {
      LOGGER.warning("Error stopping Panama profiling: " + e.getMessage());
    }
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

    try {
      // Create native string for function name
      final MemorySegment functionNameSegment = arena.allocateUtf8String(functionName);

      // Record in native profiler
      RECORD_FUNCTION_EXECUTION_HANDLE.invoke(
          nativeProfilerHandle, functionNameSegment, executionTime.toNanos(), memoryAllocated);

      // Record stack trace for flame graph if enabled
      if (nativeFlameGraphHandle != null
          && nativeFlameGraphHandle.address() != 0
          && config.isStackTraceCollectionEnabled()) {
        recordStackTraceForFlameGraph(functionName, executionTime);
      }
    } catch (Throwable e) {
      LOGGER.warning("Error recording function execution: " + e.getMessage());
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

    try {
      final MemorySegment allocationTypeSegment = arena.allocateUtf8String(allocationType);
      return (long)
          RECORD_MEMORY_ALLOCATION_HANDLE.invoke(nativeProfilerHandle, size, allocationTypeSegment);
    } catch (Throwable e) {
      LOGGER.warning("Error recording memory allocation: " + e.getMessage());
      return 0;
    }
  }

  /**
   * Records memory deallocation.
   *
   * @param allocationId allocation ID to deallocate
   */
  public void recordMemoryDeallocation(final long allocationId) {
    if (!profiling) return;

    try {
      RECORD_MEMORY_DEALLOCATION_HANDLE.invoke(nativeProfilerHandle, allocationId);
    } catch (Throwable e) {
      LOGGER.warning("Error recording memory deallocation: " + e.getMessage());
    }
  }

  /**
   * Profiles a Panama operation with automatic measurement.
   *
   * @param operationName name of the operation
   * @param panamaCall Panama operation to profile
   * @param <T> return type
   * @return operation result
   */
  public <T> T profilePanamaOperation(
      final String operationName, final PanamaOperation<T> panamaCall) {
    if (!profiling) {
      return panamaCall.execute();
    }

    final long startTime = System.nanoTime();
    final long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

    try {
      final T result = panamaCall.execute();
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
      throw new RuntimeException("Panama operation failed: " + operationName, e);
    }
  }

  /**
   * Generates flame graph from collected profiling data.
   *
   * @return flame graph root frame
   */
  public FlameGraphGenerator.FlameFrame generateFlameGraph() {
    if (!config.isFlameGraphsEnabled()
        || nativeFlameGraphHandle == null
        || nativeFlameGraphHandle.address() == 0) {
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
    if (nativeFlameGraphHandle == null || nativeFlameGraphHandle.address() == 0) {
      throw new IllegalStateException("Native flame graph collector is not available");
    }

    try {
      // Allocate buffer for SVG output
      final int bufferSize = 1024 * 1024; // 1MB buffer
      final MemorySegment outputBuffer = arena.allocate(bufferSize);

      final boolean success =
          (boolean)
              EXPORT_FLAME_GRAPH_SVG_HANDLE.invoke(
                  nativeFlameGraphHandle, width, height, outputBuffer, bufferSize);

      if (!success) {
        throw new IOException("Failed to generate SVG from native flame graph collector");
      }

      final String svgContent = outputBuffer.getUtf8String(0);
      java.nio.file.Files.writeString(outputPath, svgContent);
      LOGGER.info("Saved native flame graph to: " + outputPath);
    } catch (Throwable e) {
      throw new IOException("Error saving native flame graph", e);
    }
  }

  /**
   * Gets comprehensive profiling statistics from native implementation.
   *
   * @return native profiling statistics
   */
  public ProfilerStatistics getNativeStatistics() {
    try {
      final MemorySegment statsSegment = arena.allocate(PROFILER_STATISTICS_LAYOUT);
      final boolean success =
          (boolean) GET_DASHBOARD_HANDLE.invoke(nativeProfilerHandle, statsSegment);

      if (!success) {
        throw new RuntimeException("Failed to get native profiling statistics");
      }

      return new ProfilerStatistics(
          statsSegment.get(ValueLayout.JAVA_LONG, 0), // total_samples
          statsSegment.get(ValueLayout.JAVA_LONG, 8), // function_calls
          statsSegment.get(ValueLayout.JAVA_LONG, 16), // total_execution_time_nanos
          statsSegment.get(ValueLayout.JAVA_LONG, 24), // memory_allocations
          statsSegment.get(ValueLayout.JAVA_LONG, 32), // total_allocated_bytes
          statsSegment.get(ValueLayout.JAVA_DOUBLE, 40), // cpu_usage_percent
          statsSegment.get(ValueLayout.JAVA_LONG, 48) // active_threads
          );
    } catch (Throwable e) {
      throw new RuntimeException("Error getting native statistics", e);
    }
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
        0, // JNI calls (not applicable)
        totalSamples.get(), // Panama calls
        totalSamples.get(),
        nativeStats.totalAllocatedBytes,
        Collections.emptyMap());
  }

  /** Resets all profiling data. */
  public void reset() {
    // Native reset would be implemented if supported
    totalSamples.set(0);
    flameGraphGenerator.getStackTraceCollector().clear();
    LOGGER.info("Reset Panama profiling data");
  }

  @Override
  public void close() {
    stopProfiling();

    try {
      if (nativeFlameGraphHandle != null && nativeFlameGraphHandle.address() != 0) {
        DESTROY_FLAME_GRAPH_COLLECTOR_HANDLE.invoke(nativeFlameGraphHandle);
        nativeFlameGraphHandle = null;
      }

      if (nativeProfilerHandle != null && nativeProfilerHandle.address() != 0) {
        DESTROY_PROFILER_HANDLE.invoke(nativeProfilerHandle);
        nativeProfilerHandle = null;
      }

      arena.close();
      LOGGER.info("Panama advanced profiler closed");
    } catch (Throwable e) {
      LOGGER.warning("Error closing Panama profiler: " + e.getMessage());
    }
  }

  // Private helper methods

  private void recordStackTraceForFlameGraph(
      final String functionName, final Duration executionTime) {
    // Get current stack trace
    final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

    try {
      // Record in native flame graph collector
      for (int i = stackTrace.length - 1; i >= 2; i--) { // Skip getStackTrace and this method
        final StackTraceElement element = stackTrace[i];

        final MemorySegment functionNameSegment = arena.allocateUtf8String(element.getMethodName());
        final MemorySegment fileNameSegment =
            arena.allocateUtf8String(
                element.getFileName() != null ? element.getFileName() : "unknown");

        RECORD_STACK_SAMPLE_HANDLE.invoke(
            nativeFlameGraphHandle,
            functionNameSegment,
            fileNameSegment,
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
          Map.of("function_name", functionName, "runtime", "Panama"));
    } catch (Throwable e) {
      LOGGER.warning("Error recording stack trace for flame graph: " + e.getMessage());
    }
  }

  /** Statistics holder for native data transfer. */
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

  /** Functional interface for Panama operations. */
  @FunctionalInterface
  public interface PanamaOperation<T> {
    T execute() throws Exception;
  }

  /** Profiling session handle for Panama-based profiling. */
  public static final class ProfilingSession implements AutoCloseable {
    private final PanamaAdvancedProfiler profiler;
    private final Duration maxDuration;
    private final Instant startTime;
    private volatile boolean closed = false;

    private ProfilingSession(final PanamaAdvancedProfiler profiler, final Duration maxDuration) {
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
