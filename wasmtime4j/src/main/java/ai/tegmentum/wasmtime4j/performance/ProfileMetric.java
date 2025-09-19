package ai.tegmentum.wasmtime4j.performance;

/**
 * Types of performance metrics that can be monitored by the profiler.
 *
 * <p>Each metric represents a different aspect of WebAssembly execution performance
 * and system resource usage.
 *
 * @since 1.0.0
 */
public enum ProfileMetric {
  /**
   * CPU usage monitoring including user time, system time, and overall utilization.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>CPU utilization percentage</li>
   *   <li>User vs system time distribution</li>
   *   <li>Context switch rates</li>
   *   <li>CPU efficiency metrics</li>
   * </ul>
   */
  CPU_USAGE("CPU Usage", "Monitors CPU utilization and efficiency", 1.0),

  /**
   * Memory usage monitoring including heap, non-heap, and native memory.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>Heap memory utilization</li>
   *   <li>Direct memory usage</li>
   *   <li>Native memory consumption</li>
   *   <li>Garbage collection impact</li>
   * </ul>
   */
  MEMORY_USAGE("Memory Usage", "Monitors memory consumption and efficiency", 1.2),

  /**
   * Function call monitoring including execution times and call frequencies.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>Function execution times</li>
   *   <li>Call frequencies and patterns</li>
   *   <li>Hot function identification</li>
   *   <li>Function-level performance</li>
   * </ul>
   */
  FUNCTION_CALLS("Function Calls", "Monitors function execution performance", 1.5),

  /**
   * Compilation time monitoring for modules and functions.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>Module compilation times</li>
   *   <li>JIT compilation overhead</li>
   *   <li>Compilation cache effectiveness</li>
   *   <li>Code generation efficiency</li>
   * </ul>
   */
  COMPILATION_TIME("Compilation Time", "Monitors compilation performance", 0.8),

  /**
   * Garbage collection activity monitoring.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>GC frequency and duration</li>
   *   <li>GC overhead percentage</li>
   *   <li>Memory pressure indicators</li>
   *   <li>GC efficiency metrics</li>
   * </ul>
   */
  GC_ACTIVITY("GC Activity", "Monitors garbage collection impact", 0.9),

  /**
   * I/O operations monitoring including file and network I/O.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>I/O operation latencies</li>
   *   <li>Throughput measurements</li>
   *   <li>I/O wait times</li>
   *   <li>Operation efficiency</li>
   * </ul>
   */
  I_O_OPERATIONS("I/O Operations", "Monitors I/O performance and latency", 1.1),

  /**
   * Thread activity monitoring including thread pools and contention.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>Thread utilization</li>
   *   <li>Thread contention</li>
   *   <li>Thread pool efficiency</li>
   *   <li>Synchronization overhead</li>
   * </ul>
   */
  THREAD_ACTIVITY("Thread Activity", "Monitors threading and concurrency", 1.3),

  /**
   * Cache performance monitoring for compilation and execution caches.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>Cache hit/miss ratios</li>
   *   <li>Cache efficiency</li>
   *   <li>Cache size and utilization</li>
   *   <li>Cache eviction patterns</li>
   * </ul>
   */
  CACHE_PERFORMANCE("Cache Performance", "Monitors cache effectiveness", 0.7),

  /**
   * JIT activity monitoring including recompilation and optimization.
   *
   * <p>This metric tracks:
   * <ul>
   *   <li>JIT compilation frequency</li>
   *   <li>Optimization effectiveness</li>
   *   <li>Hot code identification</li>
   *   <li>JIT overhead measurement</li>
   * </ul>
   */
  JIT_ACTIVITY("JIT Activity", "Monitors JIT compilation and optimization", 1.4);

  private final String displayName;
  private final String description;
  private final double overheadFactor;

  ProfileMetric(final String displayName, final String description, final double overheadFactor) {
    this.displayName = displayName;
    this.description = description;
    this.overheadFactor = overheadFactor;
  }

  /**
   * Gets the human-readable display name of this metric.
   *
   * @return display name
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Gets the description of what this metric monitors.
   *
   * @return metric description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the relative overhead factor for monitoring this metric.
   *
   * <p>Higher values indicate more overhead. The factor is relative to
   * a baseline of 1.0 for standard CPU usage monitoring.
   *
   * @return overhead factor
   */
  public double getOverheadFactor() {
    return overheadFactor;
  }

  /**
   * Checks if this metric has low monitoring overhead.
   *
   * @return true if overhead factor is less than 1.0
   */
  public boolean isLowOverhead() {
    return overheadFactor < 1.0;
  }

  /**
   * Checks if this metric has high monitoring overhead.
   *
   * @return true if overhead factor is greater than 1.5
   */
  public boolean isHighOverhead() {
    return overheadFactor > 1.5;
  }

  /**
   * Checks if this metric is related to resource usage.
   *
   * @return true if this metric monitors system resources
   */
  public boolean isResourceMetric() {
    return this == CPU_USAGE ||
           this == MEMORY_USAGE ||
           this == I_O_OPERATIONS ||
           this == THREAD_ACTIVITY;
  }

  /**
   * Checks if this metric is related to compilation performance.
   *
   * @return true if this metric monitors compilation aspects
   */
  public boolean isCompilationMetric() {
    return this == COMPILATION_TIME ||
           this == JIT_ACTIVITY ||
           this == CACHE_PERFORMANCE;
  }

  /**
   * Checks if this metric requires frequent sampling for accuracy.
   *
   * @return true if frequent sampling is beneficial
   */
  public boolean requiresFrequentSampling() {
    return this == FUNCTION_CALLS ||
           this == THREAD_ACTIVITY ||
           this == JIT_ACTIVITY;
  }
}