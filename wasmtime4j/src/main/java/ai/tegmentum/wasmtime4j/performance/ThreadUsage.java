package ai.tegmentum.wasmtime4j.performance;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Detailed thread usage information for monitoring thread pool and threading behavior.
 *
 * <p>This class provides comprehensive thread statistics including active threads, peak usage,
 * thread states, and average lifetime information.
 *
 * @since 1.0.0
 */
public final class ThreadUsage {
  private final int activeThreads;
  private final int peakThreads;
  private final int totalStartedThreads;
  private final Map<String, Integer> threadsByState;
  private final Duration averageThreadLifetime;

  /**
   * Creates a thread usage record.
   *
   * @param activeThreads number of currently active threads
   * @param peakThreads peak number of threads
   * @param totalStartedThreads total number of threads started
   * @param threadsByState map of thread state to count
   * @param averageThreadLifetime average lifetime of threads
   */
  public ThreadUsage(
      final int activeThreads,
      final int peakThreads,
      final int totalStartedThreads,
      final Map<String, Integer> threadsByState,
      final Duration averageThreadLifetime) {
    this.activeThreads = Math.max(0, activeThreads);
    this.peakThreads = Math.max(0, peakThreads);
    this.totalStartedThreads = Math.max(0, totalStartedThreads);
    this.threadsByState = Map.copyOf(Objects.requireNonNull(threadsByState, "threadsByState cannot be null"));
    this.averageThreadLifetime = Objects.requireNonNull(averageThreadLifetime, "averageThreadLifetime cannot be null");

    if (averageThreadLifetime.isNegative()) {
      throw new IllegalArgumentException("averageThreadLifetime cannot be negative: " + averageThreadLifetime);
    }
  }

  /**
   * Gets the number of currently active threads.
   *
   * @return active thread count
   */
  public int getActiveThreads() {
    return activeThreads;
  }

  /**
   * Gets the peak number of threads.
   *
   * @return peak thread count
   */
  public int getPeakThreads() {
    return peakThreads;
  }

  /**
   * Gets the total number of threads started.
   *
   * @return total started thread count
   */
  public int getTotalStartedThreads() {
    return totalStartedThreads;
  }

  /**
   * Gets the threads broken down by state.
   *
   * @return map of thread state to count
   */
  public Map<String, Integer> getThreadsByState() {
    return threadsByState;
  }

  /**
   * Gets the average lifetime of threads.
   *
   * @return average thread lifetime
   */
  public Duration getAverageThreadLifetime() {
    return averageThreadLifetime;
  }

  /**
   * Gets the number of threads in a specific state.
   *
   * @param state the thread state
   * @return number of threads in the state
   */
  public int getThreadsInState(final String state) {
    return threadsByState.getOrDefault(state, 0);
  }

  /**
   * Gets the number of runnable threads.
   *
   * @return runnable thread count
   */
  public int getRunnableThreads() {
    return getThreadsInState("RUNNABLE");
  }

  /**
   * Gets the number of blocked threads.
   *
   * @return blocked thread count
   */
  public int getBlockedThreads() {
    return getThreadsInState("BLOCKED");
  }

  /**
   * Gets the number of waiting threads.
   *
   * @return waiting thread count
   */
  public int getWaitingThreads() {
    return getThreadsInState("WAITING") + getThreadsInState("TIMED_WAITING");
  }

  /**
   * Gets the thread utilization (active / peak).
   *
   * @return thread utilization ratio (0.0 to 1.0)
   */
  public double getThreadUtilization() {
    return peakThreads > 0 ? (double) activeThreads / peakThreads : 0.0;
  }

  /**
   * Gets the thread efficiency (runnable / active).
   *
   * @return thread efficiency ratio (0.0 to 1.0)
   */
  public double getThreadEfficiency() {
    return activeThreads > 0 ? (double) getRunnableThreads() / activeThreads : 0.0;
  }

  /**
   * Checks if there is thread contention.
   *
   * <p>Returns true if more than 20% of threads are blocked or waiting.
   *
   * @return true if thread contention is detected
   */
  public boolean hasThreadContention() {
    if (activeThreads == 0) {
      return false;
    }
    final int blockedAndWaiting = getBlockedThreads() + getWaitingThreads();
    return (double) blockedAndWaiting / activeThreads > 0.2;
  }

  /**
   * Checks if thread creation is excessive.
   *
   * <p>Returns true if average thread lifetime is less than 1 second.
   *
   * @return true if excessive thread creation
   */
  public boolean hasExcessiveThreadCreation() {
    return averageThreadLifetime.compareTo(Duration.ofSeconds(1)) < 0;
  }

  /**
   * Checks if thread pool is undersized.
   *
   * <p>Returns true if thread utilization is consistently high (> 90%).
   *
   * @return true if thread pool may be undersized
   */
  public boolean isThreadPoolUndersized() {
    return getThreadUtilization() > 0.9;
  }

  /**
   * Gets the thread management score (0.0 to 1.0).
   *
   * <p>Higher scores indicate better thread management (good utilization, low contention).
   *
   * @return thread management score
   */
  public double getManagementScore() {
    double score = 1.0;

    // Penalize thread contention
    if (hasThreadContention()) {
      score -= 0.3;
    }

    // Penalize excessive thread creation
    if (hasExcessiveThreadCreation()) {
      score -= 0.2;
    }

    // Penalize very low efficiency
    final double efficiency = getThreadEfficiency();
    if (efficiency < 0.3) {
      score -= 0.3;
    } else if (efficiency < 0.5) {
      score -= 0.1;
    }

    // Penalize thread pool sizing issues
    final double utilization = getThreadUtilization();
    if (utilization > 0.95 || utilization < 0.1) {
      score -= 0.2;
    }

    return Math.max(0.0, score);
  }

  /**
   * Gets the thread churn rate (started threads / active threads).
   *
   * @return thread churn rate
   */
  public double getThreadChurnRate() {
    return activeThreads > 0 ? (double) totalStartedThreads / activeThreads : 0.0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ThreadUsage that = (ThreadUsage) obj;
    return activeThreads == that.activeThreads &&
        peakThreads == that.peakThreads &&
        totalStartedThreads == that.totalStartedThreads &&
        Objects.equals(threadsByState, that.threadsByState) &&
        Objects.equals(averageThreadLifetime, that.averageThreadLifetime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(activeThreads, peakThreads, totalStartedThreads, threadsByState, averageThreadLifetime);
  }

  @Override
  public String toString() {
    return String.format(
        "ThreadUsage{active=%d, peak=%d, started=%d, utilization=%.1f%%, " +
        "efficiency=%.1f%%, avgLifetime=%s, states=%s}",
        activeThreads, peakThreads, totalStartedThreads,
        getThreadUtilization() * 100, getThreadEfficiency() * 100,
        averageThreadLifetime, threadsByState);
  }
}