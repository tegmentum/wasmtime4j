package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;

/**
 * Clock performance statistics and timing information for WASI operations.
 *
 * <p>This class contains information about clock resolution, performance characteristics, and
 * usage statistics for the various clock types available in the WASI environment.
 *
 * @since 1.0.0
 */
public final class WasiClockStats {

  private final Duration realtimeResolution;
  private final Duration monotonicResolution;
  private final Duration processCpuTimeResolution;
  private final Duration threadCpuTimeResolution;
  private final boolean highResolutionAvailable;
  private final long clockReads;
  private final long sleepOperations;
  private final long pollOperations;

  /**
   * Creates a new clock statistics instance.
   *
   * @param realtimeResolution the resolution of the realtime clock
   * @param monotonicResolution the resolution of the monotonic clock
   * @param processCpuTimeResolution the resolution of the process CPU time clock
   * @param threadCpuTimeResolution the resolution of the thread CPU time clock
   * @param highResolutionAvailable whether high-resolution timing is available
   * @param clockReads the number of clock reads performed
   * @param sleepOperations the number of sleep operations performed
   * @param pollOperations the number of poll operations performed
   */
  public WasiClockStats(
      final Duration realtimeResolution,
      final Duration monotonicResolution,
      final Duration processCpuTimeResolution,
      final Duration threadCpuTimeResolution,
      final boolean highResolutionAvailable,
      final long clockReads,
      final long sleepOperations,
      final long pollOperations) {
    this.realtimeResolution = realtimeResolution;
    this.monotonicResolution = monotonicResolution;
    this.processCpuTimeResolution = processCpuTimeResolution;
    this.threadCpuTimeResolution = threadCpuTimeResolution;
    this.highResolutionAvailable = highResolutionAvailable;
    this.clockReads = clockReads;
    this.sleepOperations = sleepOperations;
    this.pollOperations = pollOperations;
  }

  /**
   * Gets the resolution of the realtime clock.
   *
   * @return the realtime clock resolution
   */
  public Duration getRealtimeResolution() {
    return realtimeResolution;
  }

  /**
   * Gets the resolution of the monotonic clock.
   *
   * @return the monotonic clock resolution
   */
  public Duration getMonotonicResolution() {
    return monotonicResolution;
  }

  /**
   * Gets the resolution of the process CPU time clock.
   *
   * @return the process CPU time clock resolution
   */
  public Duration getProcessCpuTimeResolution() {
    return processCpuTimeResolution;
  }

  /**
   * Gets the resolution of the thread CPU time clock.
   *
   * @return the thread CPU time clock resolution
   */
  public Duration getThreadCpuTimeResolution() {
    return threadCpuTimeResolution;
  }

  /**
   * Gets the resolution for the specified clock type.
   *
   * @param clockType the clock type to get resolution for
   * @return the resolution for the specified clock type
   * @throws IllegalArgumentException if clockType is null or unknown
   */
  public Duration getResolution(final WasiClockType clockType) {
    switch (clockType) {
      case REALTIME:
        return realtimeResolution;
      case MONOTONIC:
        return monotonicResolution;
      case PROCESS_CPUTIME:
        return processCpuTimeResolution;
      case THREAD_CPUTIME:
        return threadCpuTimeResolution;
      default:
        throw new IllegalArgumentException("Unknown clock type: " + clockType);
    }
  }

  /**
   * Checks if high-resolution timing is available.
   *
   * @return true if high-resolution timing is available, false otherwise
   */
  public boolean isHighResolutionAvailable() {
    return highResolutionAvailable;
  }

  /**
   * Gets the number of clock read operations performed.
   *
   * @return the number of clock reads
   */
  public long getClockReads() {
    return clockReads;
  }

  /**
   * Gets the number of sleep operations performed.
   *
   * @return the number of sleep operations
   */
  public long getSleepOperations() {
    return sleepOperations;
  }

  /**
   * Gets the number of poll operations performed.
   *
   * @return the number of poll operations
   */
  public long getPollOperations() {
    return pollOperations;
  }

  /**
   * Gets the best available clock resolution.
   *
   * <p>This returns the finest resolution among all available clock types.
   *
   * @return the best available resolution
   */
  public Duration getBestResolution() {
    Duration best = realtimeResolution;
    if (monotonicResolution.compareTo(best) < 0) {
      best = monotonicResolution;
    }
    if (processCpuTimeResolution.compareTo(best) < 0) {
      best = processCpuTimeResolution;
    }
    if (threadCpuTimeResolution.compareTo(best) < 0) {
      best = threadCpuTimeResolution;
    }
    return best;
  }

  /**
   * Checks if the specified clock type has nanosecond resolution.
   *
   * @param clockType the clock type to check
   * @return true if the clock has nanosecond resolution, false otherwise
   */
  public boolean hasNanosecondResolution(final WasiClockType clockType) {
    return getResolution(clockType).equals(Duration.ofNanos(1));
  }

  @Override
  public String toString() {
    return String.format(
        "WasiClockStats{realtime=%s, monotonic=%s, processCpu=%s, threadCpu=%s, "
            + "highRes=%s, reads=%d, sleeps=%d, polls=%d}",
        realtimeResolution,
        monotonicResolution,
        processCpuTimeResolution,
        threadCpuTimeResolution,
        highResolutionAvailable,
        clockReads,
        sleepOperations,
        pollOperations);
  }
}