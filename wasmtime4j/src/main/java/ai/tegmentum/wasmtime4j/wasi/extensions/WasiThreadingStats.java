package ai.tegmentum.wasmtime4j.wasi.extensions;

/**
 * Statistics and performance metrics for WASI threading operations.
 *
 * <p>This class provides comprehensive information about threading usage,
 * performance characteristics, and resource utilization across all
 * threading operations in a WASI context.
 *
 * <p>Instances of this class are immutable and represent a snapshot
 * of threading statistics at the time of creation.
 *
 * @since 1.0.0
 */
public final class WasiThreadingStats {

  private final long totalThreadsCreated;
  private final int activeThreadsCount;
  private final int daemonThreadsCount;
  private final long totalMutexesCreated;
  private final int activeMutexesCount;
  private final long totalConditionVariablesCreated;
  private final int activeConditionVariablesCount;
  private final long totalSemaphoresCreated;
  private final int activeSemaphoresCount;
  private final long totalThreadPoolsCreated;
  private final int activeThreadPoolsCount;
  private final long totalTasksSubmitted;
  private final long totalTasksCompleted;
  private final long totalTasksFailed;
  private final long totalLockAcquisitions;
  private final long totalLockContentions;
  private final long totalWaitOperations;
  private final long totalSignalOperations;
  private final double averageTaskExecutionTime;
  private final double averageLockHoldTime;
  private final double averageWaitTime;
  private final long peakActiveThreads;
  private final long startTime;

  /**
   * Creates a new threading statistics snapshot.
   *
   * @param totalThreadsCreated total number of threads created
   * @param activeThreadsCount current number of active threads
   * @param daemonThreadsCount current number of daemon threads
   * @param totalMutexesCreated total number of mutexes created
   * @param activeMutexesCount current number of active mutexes
   * @param totalConditionVariablesCreated total condition variables created
   * @param activeConditionVariablesCount current active condition variables
   * @param totalSemaphoresCreated total number of semaphores created
   * @param activeSemaphoresCount current number of active semaphores
   * @param totalThreadPoolsCreated total number of thread pools created
   * @param activeThreadPoolsCount current number of active thread pools
   * @param totalTasksSubmitted total tasks submitted to thread pools
   * @param totalTasksCompleted total tasks completed successfully
   * @param totalTasksFailed total tasks that failed with exceptions
   * @param totalLockAcquisitions total lock acquisition operations
   * @param totalLockContentions total lock contention events
   * @param totalWaitOperations total wait operations on condition variables
   * @param totalSignalOperations total signal/broadcast operations
   * @param averageTaskExecutionTime average task execution time (ms)
   * @param averageLockHoldTime average time locks are held (ms)
   * @param averageWaitTime average waiting time for conditions (ms)
   * @param peakActiveThreads peak number of simultaneous active threads
   * @param startTime timestamp when threading was initialized
   */
  public WasiThreadingStats(final long totalThreadsCreated, final int activeThreadsCount,
                          final int daemonThreadsCount, final long totalMutexesCreated,
                          final int activeMutexesCount, final long totalConditionVariablesCreated,
                          final int activeConditionVariablesCount, final long totalSemaphoresCreated,
                          final int activeSemaphoresCount, final long totalThreadPoolsCreated,
                          final int activeThreadPoolsCount, final long totalTasksSubmitted,
                          final long totalTasksCompleted, final long totalTasksFailed,
                          final long totalLockAcquisitions, final long totalLockContentions,
                          final long totalWaitOperations, final long totalSignalOperations,
                          final double averageTaskExecutionTime, final double averageLockHoldTime,
                          final double averageWaitTime, final long peakActiveThreads,
                          final long startTime) {
    this.totalThreadsCreated = totalThreadsCreated;
    this.activeThreadsCount = activeThreadsCount;
    this.daemonThreadsCount = daemonThreadsCount;
    this.totalMutexesCreated = totalMutexesCreated;
    this.activeMutexesCount = activeMutexesCount;
    this.totalConditionVariablesCreated = totalConditionVariablesCreated;
    this.activeConditionVariablesCount = activeConditionVariablesCount;
    this.totalSemaphoresCreated = totalSemaphoresCreated;
    this.activeSemaphoresCount = activeSemaphoresCount;
    this.totalThreadPoolsCreated = totalThreadPoolsCreated;
    this.activeThreadPoolsCount = activeThreadPoolsCount;
    this.totalTasksSubmitted = totalTasksSubmitted;
    this.totalTasksCompleted = totalTasksCompleted;
    this.totalTasksFailed = totalTasksFailed;
    this.totalLockAcquisitions = totalLockAcquisitions;
    this.totalLockContentions = totalLockContentions;
    this.totalWaitOperations = totalWaitOperations;
    this.totalSignalOperations = totalSignalOperations;
    this.averageTaskExecutionTime = averageTaskExecutionTime;
    this.averageLockHoldTime = averageLockHoldTime;
    this.averageWaitTime = averageWaitTime;
    this.peakActiveThreads = peakActiveThreads;
    this.startTime = startTime;
  }

  // Thread Statistics

  /**
   * Gets the total number of threads created since threading was initialized.
   *
   * @return total threads created
   */
  public long getTotalThreadsCreated() {
    return totalThreadsCreated;
  }

  /**
   * Gets the current number of active threads.
   *
   * @return active threads count
   */
  public int getActiveThreadsCount() {
    return activeThreadsCount;
  }

  /**
   * Gets the current number of daemon threads.
   *
   * @return daemon threads count
   */
  public int getDaemonThreadsCount() {
    return daemonThreadsCount;
  }

  /**
   * Gets the current number of non-daemon threads.
   *
   * @return non-daemon threads count
   */
  public int getUserThreadsCount() {
    return activeThreadsCount - daemonThreadsCount;
  }

  /**
   * Gets the peak number of simultaneous active threads.
   *
   * @return peak active threads
   */
  public long getPeakActiveThreads() {
    return peakActiveThreads;
  }

  // Synchronization Statistics

  /**
   * Gets the total number of mutexes created.
   *
   * @return total mutexes created
   */
  public long getTotalMutexesCreated() {
    return totalMutexesCreated;
  }

  /**
   * Gets the current number of active mutexes.
   *
   * @return active mutexes count
   */
  public int getActiveMutexesCount() {
    return activeMutexesCount;
  }

  /**
   * Gets the total number of condition variables created.
   *
   * @return total condition variables created
   */
  public long getTotalConditionVariablesCreated() {
    return totalConditionVariablesCreated;
  }

  /**
   * Gets the current number of active condition variables.
   *
   * @return active condition variables count
   */
  public int getActiveConditionVariablesCount() {
    return activeConditionVariablesCount;
  }

  /**
   * Gets the total number of semaphores created.
   *
   * @return total semaphores created
   */
  public long getTotalSemaphoresCreated() {
    return totalSemaphoresCreated;
  }

  /**
   * Gets the current number of active semaphores.
   *
   * @return active semaphores count
   */
  public int getActiveSemaphoresCount() {
    return activeSemaphoresCount;
  }

  // Thread Pool Statistics

  /**
   * Gets the total number of thread pools created.
   *
   * @return total thread pools created
   */
  public long getTotalThreadPoolsCreated() {
    return totalThreadPoolsCreated;
  }

  /**
   * Gets the current number of active thread pools.
   *
   * @return active thread pools count
   */
  public int getActiveThreadPoolsCount() {
    return activeThreadPoolsCount;
  }

  /**
   * Gets the total number of tasks submitted to thread pools.
   *
   * @return total tasks submitted
   */
  public long getTotalTasksSubmitted() {
    return totalTasksSubmitted;
  }

  /**
   * Gets the total number of tasks completed successfully.
   *
   * @return total tasks completed
   */
  public long getTotalTasksCompleted() {
    return totalTasksCompleted;
  }

  /**
   * Gets the total number of tasks that failed with exceptions.
   *
   * @return total tasks failed
   */
  public long getTotalTasksFailed() {
    return totalTasksFailed;
  }

  // Lock Statistics

  /**
   * Gets the total number of lock acquisition operations.
   *
   * @return total lock acquisitions
   */
  public long getTotalLockAcquisitions() {
    return totalLockAcquisitions;
  }

  /**
   * Gets the total number of lock contention events.
   *
   * @return total lock contentions
   */
  public long getTotalLockContentions() {
    return totalLockContentions;
  }

  /**
   * Gets the total number of wait operations on condition variables.
   *
   * @return total wait operations
   */
  public long getTotalWaitOperations() {
    return totalWaitOperations;
  }

  /**
   * Gets the total number of signal/broadcast operations.
   *
   * @return total signal operations
   */
  public long getTotalSignalOperations() {
    return totalSignalOperations;
  }

  // Performance Metrics

  /**
   * Gets the average task execution time in milliseconds.
   *
   * @return average task execution time
   */
  public double getAverageTaskExecutionTime() {
    return averageTaskExecutionTime;
  }

  /**
   * Gets the average time locks are held in milliseconds.
   *
   * @return average lock hold time
   */
  public double getAverageLockHoldTime() {
    return averageLockHoldTime;
  }

  /**
   * Gets the average waiting time for conditions in milliseconds.
   *
   * @return average wait time
   */
  public double getAverageWaitTime() {
    return averageWaitTime;
  }

  /**
   * Gets the timestamp when threading was initialized.
   *
   * @return initialization timestamp in milliseconds since epoch
   */
  public long getStartTime() {
    return startTime;
  }

  // Calculated Metrics

  /**
   * Calculates the threading uptime in milliseconds.
   *
   * @return uptime in milliseconds
   */
  public long getUptime() {
    return System.currentTimeMillis() - startTime;
  }

  /**
   * Calculates the task success rate as a percentage.
   *
   * @return success rate (0.0 - 100.0)
   */
  public double getTaskSuccessRate() {
    if (totalTasksSubmitted == 0) {
      return 100.0;
    }
    return ((double) totalTasksCompleted / totalTasksSubmitted) * 100.0;
  }

  /**
   * Calculates the task failure rate as a percentage.
   *
   * @return failure rate (0.0 - 100.0)
   */
  public double getTaskFailureRate() {
    if (totalTasksSubmitted == 0) {
      return 0.0;
    }
    return ((double) totalTasksFailed / totalTasksSubmitted) * 100.0;
  }

  /**
   * Calculates the lock contention rate as a percentage.
   *
   * @return contention rate (0.0 - 100.0)
   */
  public double getLockContentionRate() {
    if (totalLockAcquisitions == 0) {
      return 0.0;
    }
    return ((double) totalLockContentions / totalLockAcquisitions) * 100.0;
  }

  /**
   * Calculates the thread utilization rate.
   *
   * @return utilization rate (0.0 - 1.0)
   */
  public double getThreadUtilization() {
    if (peakActiveThreads == 0) {
      return 0.0;
    }
    return (double) activeThreadsCount / peakActiveThreads;
  }

  /**
   * Calculates the task throughput (tasks per second).
   *
   * @return throughput in tasks per second
   */
  public double getTaskThroughput() {
    final long uptimeSeconds = getUptime() / 1000;
    if (uptimeSeconds == 0) {
      return 0.0;
    }
    return (double) totalTasksCompleted / uptimeSeconds;
  }

  /**
   * Calculates the thread creation rate (threads per second).
   *
   * @return thread creation rate
   */
  public double getThreadCreationRate() {
    final long uptimeSeconds = getUptime() / 1000;
    if (uptimeSeconds == 0) {
      return 0.0;
    }
    return (double) totalThreadsCreated / uptimeSeconds;
  }

  @Override
  public String toString() {
    return "WasiThreadingStats{" +
           "activeThreads=" + activeThreadsCount +
           ", totalThreadsCreated=" + totalThreadsCreated +
           ", activeMutexes=" + activeMutexesCount +
           ", activeCondVars=" + activeConditionVariablesCount +
           ", activeSemaphores=" + activeSemaphoresCount +
           ", activeThreadPools=" + activeThreadPoolsCount +
           ", tasksCompleted=" + totalTasksCompleted +
           ", taskSuccessRate=" + String.format("%.2f%%", getTaskSuccessRate()) +
           ", lockContentionRate=" + String.format("%.2f%%", getLockContentionRate()) +
           ", taskThroughput=" + String.format("%.2f tasks/s", getTaskThroughput()) +
           '}';
  }
}