package ai.tegmentum.wasmtime4j;

/**
 * Runtime statistics for a WebAssembly instance.
 *
 * <p>This class provides comprehensive statistics about the runtime behavior and resource usage of
 * a WebAssembly instance.
 *
 * @since 1.0.0
 */
public final class InstanceStatistics {

  private final long functionCallCount;
  private final long totalExecutionTime;
  private final long memoryBytesAllocated;
  private final long peakMemoryUsage;
  private final int activeTableElements;
  private final int activeGlobals;
  private final long fuelConsumed;
  private final long epochTicks;

  /**
   * Creates instance statistics.
   *
   * @param functionCallCount total number of function calls
   * @param totalExecutionTime total execution time in nanoseconds
   * @param memoryBytesAllocated total memory allocated in bytes
   * @param peakMemoryUsage peak memory usage in bytes
   * @param activeTableElements number of active table elements
   * @param activeGlobals number of active globals
   * @param fuelConsumed total fuel consumed
   * @param epochTicks total epoch ticks elapsed
   */
  public InstanceStatistics(
      final long functionCallCount,
      final long totalExecutionTime,
      final long memoryBytesAllocated,
      final long peakMemoryUsage,
      final int activeTableElements,
      final int activeGlobals,
      final long fuelConsumed,
      final long epochTicks) {
    this.functionCallCount = functionCallCount;
    this.totalExecutionTime = totalExecutionTime;
    this.memoryBytesAllocated = memoryBytesAllocated;
    this.peakMemoryUsage = peakMemoryUsage;
    this.activeTableElements = activeTableElements;
    this.activeGlobals = activeGlobals;
    this.fuelConsumed = fuelConsumed;
    this.epochTicks = epochTicks;
  }

  /**
   * Gets the total number of function calls made.
   *
   * @return the function call count
   */
  public long getFunctionCallCount() {
    return functionCallCount;
  }

  /**
   * Gets the total execution time in nanoseconds.
   *
   * @return the total execution time
   */
  public long getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the total memory allocated in bytes.
   *
   * @return the memory bytes allocated
   */
  public long getMemoryBytesAllocated() {
    return memoryBytesAllocated;
  }

  /**
   * Gets the peak memory usage in bytes.
   *
   * @return the peak memory usage
   */
  public long getPeakMemoryUsage() {
    return peakMemoryUsage;
  }

  /**
   * Gets the number of active table elements.
   *
   * @return the active table elements count
   */
  public int getActiveTableElements() {
    return activeTableElements;
  }

  /**
   * Gets the number of active globals.
   *
   * @return the active globals count
   */
  public int getActiveGlobals() {
    return activeGlobals;
  }

  /**
   * Gets the total fuel consumed.
   *
   * @return the fuel consumed
   */
  public long getFuelConsumed() {
    return fuelConsumed;
  }

  /**
   * Gets the total epoch ticks elapsed.
   *
   * @return the epoch ticks
   */
  public long getEpochTicks() {
    return epochTicks;
  }

  /**
   * Gets the average execution time per function call in nanoseconds.
   *
   * @return the average execution time per call, or 0 if no calls were made
   */
  public double getAverageExecutionTimePerCall() {
    return functionCallCount > 0 ? (double) totalExecutionTime / functionCallCount : 0.0;
  }

  /**
   * Gets the fuel consumption rate (fuel per nanosecond).
   *
   * @return the fuel consumption rate, or 0 if no execution time
   */
  public double getFuelConsumptionRate() {
    return totalExecutionTime > 0 ? (double) fuelConsumed / totalExecutionTime : 0.0;
  }

  @Override
  public String toString() {
    return String.format(
        "InstanceStatistics{calls=%d, execTime=%dns, memory=%d bytes, "
            + "peak=%d bytes, tables=%d, globals=%d, fuel=%d, epochs=%d}",
        functionCallCount,
        totalExecutionTime,
        memoryBytesAllocated,
        peakMemoryUsage,
        activeTableElements,
        activeGlobals,
        fuelConsumed,
        epochTicks);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final InstanceStatistics that = (InstanceStatistics) obj;
    return functionCallCount == that.functionCallCount
        && totalExecutionTime == that.totalExecutionTime
        && memoryBytesAllocated == that.memoryBytesAllocated
        && peakMemoryUsage == that.peakMemoryUsage
        && activeTableElements == that.activeTableElements
        && activeGlobals == that.activeGlobals
        && fuelConsumed == that.fuelConsumed
        && epochTicks == that.epochTicks;
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(
        functionCallCount,
        totalExecutionTime,
        memoryBytesAllocated,
        peakMemoryUsage,
        activeTableElements,
        activeGlobals,
        fuelConsumed,
        epochTicks);
  }
}
