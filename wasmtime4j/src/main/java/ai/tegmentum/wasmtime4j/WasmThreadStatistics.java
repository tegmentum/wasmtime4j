package ai.tegmentum.wasmtime4j;

/**
 * Performance statistics for WebAssembly threads.
 *
 * <p>This class provides detailed performance metrics and monitoring information for WebAssembly
 * thread execution, including function execution counts, timing information, memory usage, and
 * operation counters.
 *
 * <p>Statistics are collected continuously during thread execution and can be used for:
 *
 * <ul>
 *   <li>Performance monitoring and optimization
 *   <li>Resource usage analysis
 *   <li>Debugging and profiling
 *   <li>Capacity planning
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasmThreadStatistics {

  /** Number of WebAssembly functions executed by this thread. */
  private final long functionsExecuted;

  /** Total execution time in nanoseconds. */
  private final long totalExecutionTime;

  /** Number of atomic memory operations performed. */
  private final long atomicOperations;

  /** Total number of memory accesses (reads and writes). */
  private final long memoryAccesses;

  /** Number of wait/notify synchronization operations. */
  private final long waitNotifyOperations;

  /** Peak memory usage in bytes during thread execution. */
  private final long peakMemoryUsage;

  /**
   * Creates new thread statistics with the specified values.
   *
   * @param functionsExecuted number of functions executed
   * @param totalExecutionTime total execution time in nanoseconds
   * @param atomicOperations number of atomic operations
   * @param memoryAccesses number of memory accesses
   * @param waitNotifyOperations number of wait/notify operations
   * @param peakMemoryUsage peak memory usage in bytes
   */
  public WasmThreadStatistics(
      final long functionsExecuted,
      final long totalExecutionTime,
      final long atomicOperations,
      final long memoryAccesses,
      final long waitNotifyOperations,
      final long peakMemoryUsage) {
    this.functionsExecuted = Math.max(0, functionsExecuted);
    this.totalExecutionTime = Math.max(0, totalExecutionTime);
    this.atomicOperations = Math.max(0, atomicOperations);
    this.memoryAccesses = Math.max(0, memoryAccesses);
    this.waitNotifyOperations = Math.max(0, waitNotifyOperations);
    this.peakMemoryUsage = Math.max(0, peakMemoryUsage);
  }

  /**
   * Gets the number of WebAssembly functions executed by this thread.
   *
   * @return number of functions executed
   */
  public long getFunctionsExecuted() {
    return functionsExecuted;
  }

  /**
   * Gets the total execution time in nanoseconds.
   *
   * @return total execution time in nanoseconds
   */
  public long getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the total execution time in milliseconds.
   *
   * @return total execution time in milliseconds
   */
  public long getTotalExecutionTimeMillis() {
    return totalExecutionTime / 1_000_000;
  }

  /**
   * Gets the average execution time per function in nanoseconds.
   *
   * @return average execution time per function, or 0 if no functions executed
   */
  public long getAverageExecutionTime() {
    return functionsExecuted > 0 ? totalExecutionTime / functionsExecuted : 0;
  }

  /**
   * Gets the number of atomic memory operations performed.
   *
   * @return number of atomic operations
   */
  public long getAtomicOperations() {
    return atomicOperations;
  }

  /**
   * Gets the total number of memory accesses (reads and writes).
   *
   * @return number of memory accesses
   */
  public long getMemoryAccesses() {
    return memoryAccesses;
  }

  /**
   * Gets the number of wait/notify synchronization operations.
   *
   * @return number of wait/notify operations
   */
  public long getWaitNotifyOperations() {
    return waitNotifyOperations;
  }

  /**
   * Gets the peak memory usage in bytes during thread execution.
   *
   * @return peak memory usage in bytes
   */
  public long getPeakMemoryUsage() {
    return peakMemoryUsage;
  }

  /**
   * Gets the peak memory usage in kilobytes.
   *
   * @return peak memory usage in kilobytes
   */
  public long getPeakMemoryUsageKB() {
    return peakMemoryUsage / 1024;
  }

  /**
   * Gets the peak memory usage in megabytes.
   *
   * @return peak memory usage in megabytes
   */
  public long getPeakMemoryUsageMB() {
    return peakMemoryUsage / (1024 * 1024);
  }

  /**
   * Calculates the operations per second based on total execution time.
   *
   * @return operations per second, or 0 if no execution time recorded
   */
  public double getOperationsPerSecond() {
    if (totalExecutionTime == 0) {
      return 0.0;
    }

    final long totalOperations = functionsExecuted + atomicOperations + waitNotifyOperations;
    final double executionTimeSeconds = totalExecutionTime / 1_000_000_000.0;
    return totalOperations / executionTimeSeconds;
  }

  /**
   * Calculates the memory access rate (accesses per second).
   *
   * @return memory accesses per second, or 0 if no execution time recorded
   */
  public double getMemoryAccessRate() {
    if (totalExecutionTime == 0) {
      return 0.0;
    }

    final double executionTimeSeconds = totalExecutionTime / 1_000_000_000.0;
    return memoryAccesses / executionTimeSeconds;
  }

  @Override
  public String toString() {
    return String.format(
        "WasmThreadStatistics{functionsExecuted=%d, totalExecutionTime=%dns (%.2fms), "
            + "atomicOperations=%d, memoryAccesses=%d, waitNotifyOperations=%d, "
            + "peakMemoryUsage=%d bytes (%.2f MB), opsPerSec=%.2f, memAccessRate=%.2f}",
        functionsExecuted,
        totalExecutionTime,
        getTotalExecutionTimeMillis(),
        atomicOperations,
        memoryAccesses,
        waitNotifyOperations,
        peakMemoryUsage,
        getPeakMemoryUsageMB(),
        getOperationsPerSecond(),
        getMemoryAccessRate());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasmThreadStatistics that = (WasmThreadStatistics) obj;
    return functionsExecuted == that.functionsExecuted
        && totalExecutionTime == that.totalExecutionTime
        && atomicOperations == that.atomicOperations
        && memoryAccesses == that.memoryAccesses
        && waitNotifyOperations == that.waitNotifyOperations
        && peakMemoryUsage == that.peakMemoryUsage;
  }

  @Override
  public int hashCode() {
    int result = Long.hashCode(functionsExecuted);
    result = 31 * result + Long.hashCode(totalExecutionTime);
    result = 31 * result + Long.hashCode(atomicOperations);
    result = 31 * result + Long.hashCode(memoryAccesses);
    result = 31 * result + Long.hashCode(waitNotifyOperations);
    result = 31 * result + Long.hashCode(peakMemoryUsage);
    return result;
  }

  /**
   * Creates a new statistics instance with all values set to zero.
   *
   * @return empty statistics instance
   */
  public static WasmThreadStatistics empty() {
    return new WasmThreadStatistics(0, 0, 0, 0, 0, 0);
  }

  /**
   * Combines two statistics instances by summing their values.
   *
   * @param other the other statistics to combine with
   * @return new combined statistics
   * @throws IllegalArgumentException if other is null
   */
  public WasmThreadStatistics combine(final WasmThreadStatistics other) {
    if (other == null) {
      throw new IllegalArgumentException("Other statistics cannot be null");
    }

    return new WasmThreadStatistics(
        this.functionsExecuted + other.functionsExecuted,
        this.totalExecutionTime + other.totalExecutionTime,
        this.atomicOperations + other.atomicOperations,
        this.memoryAccesses + other.memoryAccesses,
        this.waitNotifyOperations + other.waitNotifyOperations,
        Math.max(this.peakMemoryUsage, other.peakMemoryUsage));
  }
}
