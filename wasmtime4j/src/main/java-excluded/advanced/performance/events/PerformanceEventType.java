package ai.tegmentum.wasmtime4j.performance.events;

/**
 * Types of performance events that can be triggered during WebAssembly operations.
 *
 * <p>These events indicate potential performance issues or noteworthy conditions that should be
 * monitored or addressed.
 *
 * @since 1.0.0
 */
public enum PerformanceEventType {
  /**
   * Triggered when CPU usage exceeds configured thresholds.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>cpuUtilization: Current CPU utilization percentage
   *   <li>threshold: The threshold that was exceeded
   *   <li>duration: How long the high usage has persisted
   * </ul>
   */
  HIGH_CPU_USAGE("High CPU usage detected"),

  /**
   * Triggered when memory usage exceeds configured thresholds.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>memoryUtilization: Current memory utilization percentage
   *   <li>heapUsed: Current heap usage in bytes
   *   <li>threshold: The threshold that was exceeded
   * </ul>
   */
  HIGH_MEMORY_USAGE("High memory usage detected"),

  /**
   * Triggered when a function call takes longer than expected.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>functionName: Name of the slow function
   *   <li>executionTime: Actual execution time
   *   <li>expectedTime: Expected execution time
   *   <li>slowdownFactor: How much slower than expected
   * </ul>
   */
  SLOW_FUNCTION_CALL("Slow function call detected"),

  /**
   * Triggered when module compilation takes longer than expected.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>moduleName: Name of the module being compiled
   *   <li>compilationTime: Actual compilation time
   *   <li>moduleSize: Size of the module in bytes
   *   <li>expectedTime: Expected compilation time
   * </ul>
   */
  LONG_COMPILATION("Long compilation time detected"),

  /**
   * Triggered when garbage collection becomes frequent or time-consuming.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>gcTime: Time spent in garbage collection
   *   <li>gcCount: Number of GC cycles
   *   <li>gcOverhead: GC overhead percentage
   *   <li>gcType: Type of garbage collector
   * </ul>
   */
  FREQUENT_GC("Frequent garbage collection detected"),

  /**
   * Triggered when cache miss rates become problematic.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>cacheHitRatio: Current cache hit ratio
   *   <li>cacheMisses: Number of cache misses
   *   <li>cacheType: Type of cache (compilation, execution, etc.)
   * </ul>
   */
  CACHE_MISS_SPIKE("High cache miss rate detected"),

  /**
   * Triggered when thread contention is detected.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>blockedThreads: Number of blocked threads
   *   <li>waitingThreads: Number of waiting threads
   *   <li>contentionRate: Thread contention rate
   *   <li>lockName: Name of contended lock if available
   * </ul>
   */
  THREAD_CONTENTION("Thread contention detected"),

  /**
   * Triggered when I/O operations become slow or excessive.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>ioLatency: Current I/O latency
   *   <li>ioThroughput: Current I/O throughput
   *   <li>operationType: Type of I/O operation
   * </ul>
   */
  HIGH_IO_LATENCY("High I/O latency detected"),

  /**
   * Triggered when memory leaks are suspected.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>memoryGrowth: Rate of memory growth
   *   <li>allocations: Number of allocations
   *   <li>deallocations: Number of deallocations
   *   <li>netGrowth: Net memory growth
   * </ul>
   */
  MEMORY_LEAK_SUSPECTED("Potential memory leak detected"),

  /**
   * Triggered when JIT compilation becomes excessive.
   *
   * <p>Event data may include:
   *
   * <ul>
   *   <li>jitCompilations: Number of JIT compilations
   *   <li>jitTime: Time spent in JIT compilation
   *   <li>jitOverhead: JIT overhead percentage
   * </ul>
   */
  EXCESSIVE_JIT_COMPILATION("Excessive JIT compilation detected");

  private final String description;

  PerformanceEventType(final String description) {
    this.description = description;
  }

  /**
   * Gets the human-readable description of this event type.
   *
   * @return event description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this event type indicates a critical performance issue.
   *
   * @return true if critical
   */
  public boolean isCritical() {
    return this == HIGH_CPU_USAGE
        || this == HIGH_MEMORY_USAGE
        || this == MEMORY_LEAK_SUSPECTED
        || this == THREAD_CONTENTION;
  }

  /**
   * Checks if this event type relates to resource usage.
   *
   * @return true if resource-related
   */
  public boolean isResourceRelated() {
    return this == HIGH_CPU_USAGE
        || this == HIGH_MEMORY_USAGE
        || this == FREQUENT_GC
        || this == HIGH_IO_LATENCY
        || this == MEMORY_LEAK_SUSPECTED;
  }

  /**
   * Checks if this event type relates to compilation performance.
   *
   * @return true if compilation-related
   */
  public boolean isCompilationRelated() {
    return this == LONG_COMPILATION
        || this == CACHE_MISS_SPIKE
        || this == EXCESSIVE_JIT_COMPILATION;
  }
}
