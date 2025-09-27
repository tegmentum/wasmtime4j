package ai.tegmentum.wasmtime4j.async;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Statistics for asynchronous function execution.
 *
 * <p>Tracks execution metrics, performance data, and resource usage
 * for asynchronous WebAssembly function calls.
 *
 * @since 1.0.0
 */
public final class AsyncFunctionStatistics {

  private final AtomicLong totalCalls;
  private final AtomicLong successfulCalls;
  private final AtomicLong failedCalls;
  private final AtomicLong timeoutCalls;
  private final AtomicLong totalExecutionTimeMs;
  private final AtomicLong minExecutionTimeMs;
  private final AtomicLong maxExecutionTimeMs;

  public AsyncFunctionStatistics() {
    this.totalCalls = new AtomicLong(0);
    this.successfulCalls = new AtomicLong(0);
    this.failedCalls = new AtomicLong(0);
    this.timeoutCalls = new AtomicLong(0);
    this.totalExecutionTimeMs = new AtomicLong(0);
    this.minExecutionTimeMs = new AtomicLong(Long.MAX_VALUE);
    this.maxExecutionTimeMs = new AtomicLong(0);
  }

  /**
   * Records a successful function call.
   *
   * @param executionTimeMs the execution time in milliseconds
   */
  public void recordSuccess(final long executionTimeMs) {
    totalCalls.incrementAndGet();
    successfulCalls.incrementAndGet();
    updateExecutionTime(executionTimeMs);
  }

  /**
   * Records a failed function call.
   *
   * @param executionTimeMs the execution time in milliseconds
   */
  public void recordFailure(final long executionTimeMs) {
    totalCalls.incrementAndGet();
    failedCalls.incrementAndGet();
    updateExecutionTime(executionTimeMs);
  }

  /**
   * Records a timed-out function call.
   *
   * @param executionTimeMs the execution time in milliseconds
   */
  public void recordTimeout(final long executionTimeMs) {
    totalCalls.incrementAndGet();
    timeoutCalls.incrementAndGet();
    updateExecutionTime(executionTimeMs);
  }

  private void updateExecutionTime(final long executionTimeMs) {
    totalExecutionTimeMs.addAndGet(executionTimeMs);
    minExecutionTimeMs.updateAndGet(current -> Math.min(current, executionTimeMs));
    maxExecutionTimeMs.updateAndGet(current -> Math.max(current, executionTimeMs));
  }

  /**
   * Gets the total number of function calls.
   *
   * @return total call count
   */
  public long getTotalCalls() {
    return totalCalls.get();
  }

  /**
   * Gets the number of successful function calls.
   *
   * @return successful call count
   */
  public long getSuccessfulCalls() {
    return successfulCalls.get();
  }

  /**
   * Gets the number of failed function calls.
   *
   * @return failed call count
   */
  public long getFailedCalls() {
    return failedCalls.get();
  }

  /**
   * Gets the number of timed-out function calls.
   *
   * @return timeout call count
   */
  public long getTimeoutCalls() {
    return timeoutCalls.get();
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return success rate (0.0 to 1.0)
   */
  public double getSuccessRate() {
    final long total = totalCalls.get();
    return total == 0 ? 0.0 : (double) successfulCalls.get() / total;
  }

  /**
   * Gets the average execution time.
   *
   * @return average execution time
   */
  public Duration getAverageExecutionTime() {
    final long total = totalCalls.get();
    if (total == 0) {
      return Duration.ZERO;
    }
    return Duration.ofMillis(totalExecutionTimeMs.get() / total);
  }

  /**
   * Gets the minimum execution time.
   *
   * @return minimum execution time
   */
  public Duration getMinExecutionTime() {
    final long min = minExecutionTimeMs.get();
    return min == Long.MAX_VALUE ? Duration.ZERO : Duration.ofMillis(min);
  }

  /**
   * Gets the maximum execution time.
   *
   * @return maximum execution time
   */
  public Duration getMaxExecutionTime() {
    return Duration.ofMillis(maxExecutionTimeMs.get());
  }

  /**
   * Resets all statistics.
   */
  public void reset() {
    totalCalls.set(0);
    successfulCalls.set(0);
    failedCalls.set(0);
    timeoutCalls.set(0);
    totalExecutionTimeMs.set(0);
    minExecutionTimeMs.set(Long.MAX_VALUE);
    maxExecutionTimeMs.set(0);
  }
}