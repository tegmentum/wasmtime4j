package ai.tegmentum.wasmtime4j.async;

import java.time.Instant;

/**
 * Resource usage information during WebAssembly instance instantiation.
 *
 * <p>This class tracks memory usage, CPU utilization, and other resource
 * metrics during the instantiation process for monitoring and optimization.
 *
 * @since 1.0.0
 */
public final class InstantiationResourceUsage {

  private final long memoryUsed;
  private final long peakMemoryUsed;
  private final double cpuUtilization;
  private final long nativeMemoryUsed;
  private final int functionsLinked;
  private final int tablesAllocated;
  private final int globalsInitialized;
  private final long compilationCacheSize;
  private final Instant measurementTime;

  /**
   * Creates new instantiation resource usage information.
   *
   * @param memoryUsed current memory usage in bytes
   * @param peakMemoryUsed peak memory usage in bytes
   * @param cpuUtilization CPU utilization percentage (0.0 - 1.0)
   * @param nativeMemoryUsed native memory usage in bytes
   * @param functionsLinked number of functions linked
   * @param tablesAllocated number of tables allocated
   * @param globalsInitialized number of globals initialized
   * @param compilationCacheSize compilation cache size in bytes
   * @param measurementTime when this measurement was taken
   */
  public InstantiationResourceUsage(
      final long memoryUsed,
      final long peakMemoryUsed,
      final double cpuUtilization,
      final long nativeMemoryUsed,
      final int functionsLinked,
      final int tablesAllocated,
      final int globalsInitialized,
      final long compilationCacheSize,
      final Instant measurementTime) {
    this.memoryUsed = memoryUsed;
    this.peakMemoryUsed = Math.max(peakMemoryUsed, memoryUsed);
    this.cpuUtilization = Math.max(0.0, Math.min(1.0, cpuUtilization));
    this.nativeMemoryUsed = nativeMemoryUsed;
    this.functionsLinked = functionsLinked;
    this.tablesAllocated = tablesAllocated;
    this.globalsInitialized = globalsInitialized;
    this.compilationCacheSize = compilationCacheSize;
    this.measurementTime = measurementTime;
  }

  /**
   * Creates an empty resource usage measurement.
   *
   * @return empty resource usage
   */
  public static InstantiationResourceUsage empty() {
    return new InstantiationResourceUsage(0L, 0L, 0.0, 0L, 0, 0, 0, 0L, Instant.now());
  }

  /**
   * Creates a basic resource usage measurement with memory only.
   *
   * @param memoryUsed memory usage in bytes
   * @return basic resource usage
   */
  public static InstantiationResourceUsage withMemory(final long memoryUsed) {
    return new InstantiationResourceUsage(
        memoryUsed, memoryUsed, 0.0, 0L, 0, 0, 0, 0L, Instant.now());
  }

  /**
   * Gets the current memory usage.
   *
   * @return memory usage in bytes
   */
  public long getMemoryUsed() {
    return memoryUsed;
  }

  /**
   * Gets the peak memory usage.
   *
   * @return peak memory usage in bytes
   */
  public long getPeakMemoryUsed() {
    return peakMemoryUsed;
  }

  /**
   * Gets the CPU utilization percentage.
   *
   * @return CPU utilization (0.0 - 1.0)
   */
  public double getCpuUtilization() {
    return cpuUtilization;
  }

  /**
   * Gets the native memory usage.
   *
   * @return native memory usage in bytes
   */
  public long getNativeMemoryUsed() {
    return nativeMemoryUsed;
  }

  /**
   * Gets the number of functions linked.
   *
   * @return functions linked count
   */
  public int getFunctionsLinked() {
    return functionsLinked;
  }

  /**
   * Gets the number of tables allocated.
   *
   * @return tables allocated count
   */
  public int getTablesAllocated() {
    return tablesAllocated;
  }

  /**
   * Gets the number of globals initialized.
   *
   * @return globals initialized count
   */
  public int getGlobalsInitialized() {
    return globalsInitialized;
  }

  /**
   * Gets the compilation cache size.
   *
   * @return compilation cache size in bytes
   */
  public long getCompilationCacheSize() {
    return compilationCacheSize;
  }

  /**
   * Gets the time when this measurement was taken.
   *
   * @return measurement timestamp
   */
  public Instant getMeasurementTime() {
    return measurementTime;
  }

  /**
   * Gets the total memory usage (managed + native).
   *
   * @return total memory usage in bytes
   */
  public long getTotalMemoryUsed() {
    return memoryUsed + nativeMemoryUsed;
  }

  /**
   * Gets the total peak memory usage.
   *
   * @return total peak memory usage in bytes
   */
  public long getTotalPeakMemoryUsed() {
    return peakMemoryUsed + nativeMemoryUsed;
  }

  /**
   * Calculates the memory efficiency ratio.
   *
   * @return memory efficiency (current/peak)
   */
  public double getMemoryEfficiency() {
    return peakMemoryUsed > 0 ? (double) memoryUsed / peakMemoryUsed : 1.0;
  }

  /**
   * Gets the total number of resources allocated.
   *
   * @return total resources count
   */
  public int getTotalResourcesAllocated() {
    return functionsLinked + tablesAllocated + globalsInitialized;
  }

  /**
   * Checks if any resources have been allocated.
   *
   * @return true if resources are allocated
   */
  public boolean hasResourcesAllocated() {
    return getTotalResourcesAllocated() > 0;
  }

  /**
   * Checks if the compilation cache is being used.
   *
   * @return true if compilation cache is in use
   */
  public boolean hasCompilationCache() {
    return compilationCacheSize > 0;
  }

  /**
   * Creates a copy with updated memory usage.
   *
   * @param newMemoryUsed updated memory usage
   * @return updated resource usage
   */
  public InstantiationResourceUsage withMemoryUsed(final long newMemoryUsed) {
    return new InstantiationResourceUsage(
        newMemoryUsed,
        Math.max(peakMemoryUsed, newMemoryUsed),
        cpuUtilization,
        nativeMemoryUsed,
        functionsLinked,
        tablesAllocated,
        globalsInitialized,
        compilationCacheSize,
        Instant.now());
  }

  /**
   * Creates a copy with updated CPU utilization.
   *
   * @param newCpuUtilization updated CPU utilization
   * @return updated resource usage
   */
  public InstantiationResourceUsage withCpuUtilization(final double newCpuUtilization) {
    return new InstantiationResourceUsage(
        memoryUsed,
        peakMemoryUsed,
        newCpuUtilization,
        nativeMemoryUsed,
        functionsLinked,
        tablesAllocated,
        globalsInitialized,
        compilationCacheSize,
        Instant.now());
  }

  /**
   * Creates a copy with updated resource counts.
   *
   * @param newFunctionsLinked updated functions count
   * @param newTablesAllocated updated tables count
   * @param newGlobalsInitialized updated globals count
   * @return updated resource usage
   */
  public InstantiationResourceUsage withResourceCounts(
      final int newFunctionsLinked,
      final int newTablesAllocated,
      final int newGlobalsInitialized) {
    return new InstantiationResourceUsage(
        memoryUsed,
        peakMemoryUsed,
        cpuUtilization,
        nativeMemoryUsed,
        newFunctionsLinked,
        newTablesAllocated,
        newGlobalsInitialized,
        compilationCacheSize,
        Instant.now());
  }

  @Override
  public String toString() {
    return String.format(
        "InstantiationResourceUsage{memory=%s/%s, cpu=%.1f%%, resources=%d, cache=%s}",
        formatBytes(memoryUsed),
        formatBytes(peakMemoryUsed),
        cpuUtilization * 100,
        getTotalResourcesAllocated(),
        formatBytes(compilationCacheSize));
  }

  private String formatBytes(final long bytes) {
    if (bytes < 1024) return bytes + "B";
    if (bytes < 1024 * 1024) return (bytes / 1024) + "KB";
    return (bytes / (1024 * 1024)) + "MB";
  }
}