package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;

/**
 * Process usage statistics for WASI processes.
 *
 * <p>This class contains information about resource usage for a process, including CPU time, memory
 * usage, and other performance metrics. The availability of specific statistics depends on the host
 * system and WASI implementation.
 *
 * @since 1.0.0
 */
public final class WasiProcessStats {

  private final Duration userCpuTime;
  private final Duration systemCpuTime;
  private final long maxResidentSetSize;
  private final long integralSharedMemorySize;
  private final long integralUnsharedDataSize;
  private final long integralUnsharedStackSize;
  private final long pageReclaims;
  private final long pageFaults;
  private final long swaps;
  private final long blockInputOperations;
  private final long blockOutputOperations;
  private final long messagesSent;
  private final long messagesReceived;
  private final long signalsReceived;
  private final long voluntaryContextSwitches;
  private final long involuntaryContextSwitches;

  /**
   * Creates a new process statistics instance.
   *
   * @param userCpuTime time spent in user mode
   * @param systemCpuTime time spent in system mode
   * @param maxResidentSetSize maximum resident set size (memory) in kilobytes
   * @param integralSharedMemorySize integral shared memory size
   * @param integralUnsharedDataSize integral unshared data size
   * @param integralUnsharedStackSize integral unshared stack size
   * @param pageReclaims page reclaims (soft page faults)
   * @param pageFaults page faults (hard page faults)
   * @param swaps swaps
   * @param blockInputOperations block input operations
   * @param blockOutputOperations block output operations
   * @param messagesSent messages sent
   * @param messagesReceived messages received
   * @param signalsReceived signals received
   * @param voluntaryContextSwitches voluntary context switches
   * @param involuntaryContextSwitches involuntary context switches
   */
  public WasiProcessStats(
      final Duration userCpuTime,
      final Duration systemCpuTime,
      final long maxResidentSetSize,
      final long integralSharedMemorySize,
      final long integralUnsharedDataSize,
      final long integralUnsharedStackSize,
      final long pageReclaims,
      final long pageFaults,
      final long swaps,
      final long blockInputOperations,
      final long blockOutputOperations,
      final long messagesSent,
      final long messagesReceived,
      final long signalsReceived,
      final long voluntaryContextSwitches,
      final long involuntaryContextSwitches) {
    this.userCpuTime = userCpuTime;
    this.systemCpuTime = systemCpuTime;
    this.maxResidentSetSize = maxResidentSetSize;
    this.integralSharedMemorySize = integralSharedMemorySize;
    this.integralUnsharedDataSize = integralUnsharedDataSize;
    this.integralUnsharedStackSize = integralUnsharedStackSize;
    this.pageReclaims = pageReclaims;
    this.pageFaults = pageFaults;
    this.swaps = swaps;
    this.blockInputOperations = blockInputOperations;
    this.blockOutputOperations = blockOutputOperations;
    this.messagesSent = messagesSent;
    this.messagesReceived = messagesReceived;
    this.signalsReceived = signalsReceived;
    this.voluntaryContextSwitches = voluntaryContextSwitches;
    this.involuntaryContextSwitches = involuntaryContextSwitches;
  }

  /**
   * Gets the total time spent executing in user mode.
   *
   * @return the user CPU time
   */
  public Duration getUserCpuTime() {
    return userCpuTime;
  }

  /**
   * Gets the total time spent executing in system mode (kernel).
   *
   * @return the system CPU time
   */
  public Duration getSystemCpuTime() {
    return systemCpuTime;
  }

  /**
   * Gets the total CPU time (user + system).
   *
   * @return the total CPU time
   */
  public Duration getTotalCpuTime() {
    return userCpuTime.plus(systemCpuTime);
  }

  /**
   * Gets the maximum resident set size (peak memory usage) in kilobytes.
   *
   * @return the maximum resident set size in KB
   */
  public long getMaxResidentSetSize() {
    return maxResidentSetSize;
  }

  /**
   * Gets the integral shared memory size.
   *
   * @return the integral shared memory size
   */
  public long getIntegralSharedMemorySize() {
    return integralSharedMemorySize;
  }

  /**
   * Gets the integral unshared data size.
   *
   * @return the integral unshared data size
   */
  public long getIntegralUnsharedDataSize() {
    return integralUnsharedDataSize;
  }

  /**
   * Gets the integral unshared stack size.
   *
   * @return the integral unshared stack size
   */
  public long getIntegralUnsharedStackSize() {
    return integralUnsharedStackSize;
  }

  /**
   * Gets the number of page reclaims (soft page faults).
   *
   * @return the number of page reclaims
   */
  public long getPageReclaims() {
    return pageReclaims;
  }

  /**
   * Gets the number of page faults (hard page faults).
   *
   * @return the number of page faults
   */
  public long getPageFaults() {
    return pageFaults;
  }

  /**
   * Gets the number of swaps.
   *
   * @return the number of swaps
   */
  public long getSwaps() {
    return swaps;
  }

  /**
   * Gets the number of block input operations.
   *
   * @return the number of block input operations
   */
  public long getBlockInputOperations() {
    return blockInputOperations;
  }

  /**
   * Gets the number of block output operations.
   *
   * @return the number of block output operations
   */
  public long getBlockOutputOperations() {
    return blockOutputOperations;
  }

  /**
   * Gets the number of messages sent.
   *
   * @return the number of messages sent
   */
  public long getMessagesSent() {
    return messagesSent;
  }

  /**
   * Gets the number of messages received.
   *
   * @return the number of messages received
   */
  public long getMessagesReceived() {
    return messagesReceived;
  }

  /**
   * Gets the number of signals received.
   *
   * @return the number of signals received
   */
  public long getSignalsReceived() {
    return signalsReceived;
  }

  /**
   * Gets the number of voluntary context switches.
   *
   * @return the number of voluntary context switches
   */
  public long getVoluntaryContextSwitches() {
    return voluntaryContextSwitches;
  }

  /**
   * Gets the number of involuntary context switches.
   *
   * @return the number of involuntary context switches
   */
  public long getInvoluntaryContextSwitches() {
    return involuntaryContextSwitches;
  }

  /**
   * Gets the total number of context switches.
   *
   * @return the total number of context switches
   */
  public long getTotalContextSwitches() {
    return voluntaryContextSwitches + involuntaryContextSwitches;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiProcessStats{userCpuTime=%s, systemCpuTime=%s, maxResidentSetSize=%d KB, "
            + "pageReclaims=%d, pageFaults=%d, blockIO=%d/%d, contextSwitches=%d/%d}",
        userCpuTime,
        systemCpuTime,
        maxResidentSetSize,
        pageReclaims,
        pageFaults,
        blockInputOperations,
        blockOutputOperations,
        voluntaryContextSwitches,
        involuntaryContextSwitches);
  }
}
