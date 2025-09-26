package ai.tegmentum.wasmtime4j.compilation;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tracks execution statistics for individual WebAssembly functions.
 *
 * <p>This class maintains comprehensive execution metrics that are used by the
 * {@link TierTransitionManager} to make intelligent tier transition decisions.
 *
 * <p>Thread-safe implementation uses atomic operations for concurrent access
 * from multiple execution threads while maintaining accurate statistics.
 *
 * @since 1.0.0
 */
public final class FunctionExecutionTracker {

  private final String functionId;
  private final AtomicReference<JitCompilationStrategy> currentStrategy;
  private final AtomicLong executionCount;
  private final AtomicLong totalExecutionTimeNs;
  private final AtomicLong minExecutionTimeNs;
  private final AtomicLong maxExecutionTimeNs;
  private final AtomicLong totalMemoryUsage;
  private final AtomicReference<Instant> firstExecution;
  private final AtomicReference<Instant> lastExecution;
  private final AtomicLong transitionFailures;

  // Performance degradation tracking
  private final AtomicLong recentExecutionCount;
  private final AtomicLong recentExecutionTimeNs;
  private final AtomicReference<Instant> performanceWindowStart;

  /**
   * Creates a new function execution tracker.
   *
   * @param functionId unique function identifier
   * @param initialStrategy initial compilation strategy
   */
  public FunctionExecutionTracker(final String functionId,
                                  final JitCompilationStrategy initialStrategy) {
    this.functionId = functionId;
    this.currentStrategy = new AtomicReference<>(initialStrategy);
    this.executionCount = new AtomicLong(0);
    this.totalExecutionTimeNs = new AtomicLong(0);
    this.minExecutionTimeNs = new AtomicLong(Long.MAX_VALUE);
    this.maxExecutionTimeNs = new AtomicLong(0);
    this.totalMemoryUsage = new AtomicLong(0);
    this.firstExecution = new AtomicReference<>();
    this.lastExecution = new AtomicReference<>();
    this.transitionFailures = new AtomicLong(0);

    // Performance window for recent tracking (last 5 minutes)
    this.recentExecutionCount = new AtomicLong(0);
    this.recentExecutionTimeNs = new AtomicLong(0);
    this.performanceWindowStart = new AtomicReference<>(Instant.now());
  }

  /**
   * Records a function execution.
   *
   * @param executionTimeNs execution time in nanoseconds
   * @param memoryUsage memory usage during execution
   */
  public void recordExecution(final long executionTimeNs, final long memoryUsage) {
    final Instant now = Instant.now();

    // Update overall statistics
    executionCount.incrementAndGet();
    totalExecutionTimeNs.addAndGet(executionTimeNs);
    totalMemoryUsage.addAndGet(memoryUsage);
    lastExecution.set(now);

    // Set first execution time if not already set
    firstExecution.compareAndSet(null, now);

    // Update min/max execution times
    updateMinExecutionTime(executionTimeNs);
    updateMaxExecutionTime(executionTimeNs);

    // Update recent performance window
    updateRecentPerformance(executionTimeNs, now);
  }

  /**
   * Updates minimum execution time atomically.
   */
  private void updateMinExecutionTime(final long executionTimeNs) {
    long current = minExecutionTimeNs.get();
    while (executionTimeNs < current) {
      if (minExecutionTimeNs.compareAndSet(current, executionTimeNs)) {
        break;
      }
      current = minExecutionTimeNs.get();
    }
  }

  /**
   * Updates maximum execution time atomically.
   */
  private void updateMaxExecutionTime(final long executionTimeNs) {
    long current = maxExecutionTimeNs.get();
    while (executionTimeNs > current) {
      if (maxExecutionTimeNs.compareAndSet(current, executionTimeNs)) {
        break;
      }
      current = maxExecutionTimeNs.get();
    }
  }

  /**
   * Updates recent performance tracking with time window management.
   */
  private void updateRecentPerformance(final long executionTimeNs, final Instant now) {
    final Instant windowStart = performanceWindowStart.get();

    // Reset window if older than 5 minutes
    if (Duration.between(windowStart, now).toMinutes() >= 5) {
      if (performanceWindowStart.compareAndSet(windowStart, now)) {
        recentExecutionCount.set(1);
        recentExecutionTimeNs.set(executionTimeNs);
      } else {
        // Another thread reset the window, add to new window
        recentExecutionCount.incrementAndGet();
        recentExecutionTimeNs.addAndGet(executionTimeNs);
      }
    } else {
      // Add to current window
      recentExecutionCount.incrementAndGet();
      recentExecutionTimeNs.addAndGet(executionTimeNs);
    }
  }

  /**
   * Transitions to a new compilation strategy.
   *
   * @param newStrategy new compilation strategy
   */
  public void transitionToStrategy(final JitCompilationStrategy newStrategy) {
    currentStrategy.set(newStrategy);

    // Reset recent performance tracking after strategy change
    final Instant now = Instant.now();
    performanceWindowStart.set(now);
    recentExecutionCount.set(0);
    recentExecutionTimeNs.set(0);
  }

  /**
   * Records a transition failure.
   */
  public void recordTransitionFailure() {
    transitionFailures.incrementAndGet();
  }

  /**
   * Gets the current compilation strategy.
   *
   * @return current strategy
   */
  public JitCompilationStrategy getCurrentStrategy() {
    return currentStrategy.get();
  }

  /**
   * Gets the function identifier.
   *
   * @return function ID
   */
  public String getFunctionId() {
    return functionId;
  }

  /**
   * Gets comprehensive execution statistics.
   *
   * @return execution statistics
   */
  public ExecutionStatistics getStatistics() {
    final long count = executionCount.get();
    final long totalTimeNs = totalExecutionTimeNs.get();
    final long minTimeNs = minExecutionTimeNs.get();
    final long maxTimeNs = maxExecutionTimeNs.get();
    final long memoryUsage = totalMemoryUsage.get();
    final Instant first = firstExecution.get();
    final Instant last = lastExecution.get();
    final long failures = transitionFailures.get();

    final long recentCount = recentExecutionCount.get();
    final long recentTimeNs = recentExecutionTimeNs.get();
    final Instant windowStart = performanceWindowStart.get();

    return new ExecutionStatistics(
        functionId,
        count,
        Duration.ofNanos(totalTimeNs),
        Duration.ofNanos(count > 0 ? totalTimeNs / count : 0),
        Duration.ofNanos(minTimeNs == Long.MAX_VALUE ? 0 : minTimeNs),
        Duration.ofNanos(maxTimeNs),
        memoryUsage,
        first,
        last,
        failures,
        recentCount,
        Duration.ofNanos(recentTimeNs),
        Duration.ofNanos(recentCount > 0 ? recentTimeNs / recentCount : 0),
        windowStart
    );
  }

  /**
   * Detects performance regression after tier transition.
   *
   * @param baselineAvgNs baseline average execution time
   * @param regressionThreshold regression threshold (e.g., 0.2 for 20% slowdown)
   * @return true if performance has regressed
   */
  public boolean hasPerformanceRegressed(final long baselineAvgNs, final double regressionThreshold) {
    final long recentCount = recentExecutionCount.get();
    if (recentCount < 10) {
      return false; // Need sufficient recent data
    }

    final long recentTimeNs = recentExecutionTimeNs.get();
    final long recentAvgNs = recentTimeNs / recentCount;

    final double regressionRatio = (double) recentAvgNs / baselineAvgNs;
    return regressionRatio > (1.0 + regressionThreshold);
  }

  /**
   * Gets execution rate (calls per second) over recent time window.
   *
   * @return execution rate
   */
  public double getRecentExecutionRate() {
    final long recentCount = recentExecutionCount.get();
    final Instant windowStart = performanceWindowStart.get();
    final Duration windowDuration = Duration.between(windowStart, Instant.now());

    if (windowDuration.toMillis() <= 0) {
      return 0.0;
    }

    return recentCount / windowDuration.toSeconds();
  }

  /**
   * Gets overall execution rate since first execution.
   *
   * @return overall execution rate
   */
  public double getOverallExecutionRate() {
    final Instant first = firstExecution.get();
    if (first == null) {
      return 0.0;
    }

    final Duration totalDuration = Duration.between(first, Instant.now());
    if (totalDuration.toMillis() <= 0) {
      return 0.0;
    }

    return executionCount.get() / totalDuration.toSeconds();
  }

  /**
   * Determines if this function is a hot path based on execution patterns.
   *
   * @return true if function is identified as hot path
   */
  public boolean isHotPath() {
    final long count = executionCount.get();
    final long totalTimeMs = totalExecutionTimeNs.get() / 1_000_000;
    final double avgTimeMs = count > 0 ? (double) totalTimeMs / count : 0;
    final double executionRate = getOverallExecutionRate();

    // Hot path criteria:
    // 1. High execution count
    // 2. High total execution time
    // 3. Reasonable average execution time (not just one slow call)
    // 4. High execution rate
    return count > 5000 ||
           totalTimeMs > 10000 ||
           (avgTimeMs > 1.0 && count > 1000) ||
           executionRate > 10.0;
  }

  @Override
  public String toString() {
    final ExecutionStatistics stats = getStatistics();
    return String.format(
        "FunctionExecutionTracker{id=%s, strategy=%s, executions=%d, " +
        "avgTime=%.2fms, totalTime=%dms, rate=%.1f/s, hotPath=%b}",
        functionId,
        currentStrategy.get().getName(),
        stats.getExecutionCount(),
        stats.getAverageExecutionTime().toNanos() / 1_000_000.0,
        stats.getTotalExecutionTime().toMillis(),
        getOverallExecutionRate(),
        isHotPath()
    );
  }
}