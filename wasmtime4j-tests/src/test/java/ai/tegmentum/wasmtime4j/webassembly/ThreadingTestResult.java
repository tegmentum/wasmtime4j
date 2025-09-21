package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of executing a single Threading/Atomic operations test case, including
 * execution details, thread safety validation, and performance metrics.
 *
 * <p>This class provides comprehensive information about threading test execution to support
 * detailed analysis and reporting of atomic operation and shared memory testing results.
 */
public final class ThreadingTestResult {
  private final WasmTestCase testCase;
  private final String category;
  private final boolean successful;
  private final Optional<String> failureReason;
  private final Duration executionTime;
  private final Instant executionTimestamp;
  private final Optional<RuntimeType> runtimeType;
  private final Optional<ThreadingPerformanceMetrics> performanceMetrics;

  private ThreadingTestResult(
      final WasmTestCase testCase,
      final String category,
      final boolean successful,
      final Optional<String> failureReason,
      final Duration executionTime,
      final Instant executionTimestamp,
      final Optional<RuntimeType> runtimeType,
      final Optional<ThreadingPerformanceMetrics> performanceMetrics) {

    this.testCase = Objects.requireNonNull(testCase, "testCase cannot be null");
    this.category = Objects.requireNonNull(category, "category cannot be null");
    this.successful = successful;
    this.failureReason = Objects.requireNonNull(failureReason, "failureReason cannot be null");
    this.executionTime = Objects.requireNonNull(executionTime, "executionTime cannot be null");
    this.executionTimestamp =
        Objects.requireNonNull(executionTimestamp, "executionTimestamp cannot be null");
    this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    this.performanceMetrics =
        Objects.requireNonNull(performanceMetrics, "performanceMetrics cannot be null");
  }

  /**
   * Creates a successful threading test result.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param executionTime the execution duration
   * @param runtimeType the runtime type used
   * @return successful test result
   */
  public static ThreadingTestResult successful(
      final WasmTestCase testCase,
      final String category,
      final Duration executionTime,
      final RuntimeType runtimeType) {

    return new ThreadingTestResult(
        testCase,
        category,
        true,
        Optional.empty(),
        executionTime,
        Instant.now(),
        Optional.of(runtimeType),
        Optional.empty());
  }

  /**
   * Creates a successful threading test result with performance metrics.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param executionTime the execution duration
   * @param runtimeType the runtime type used
   * @param performanceMetrics the performance metrics
   * @return successful test result with performance data
   */
  public static ThreadingTestResult successfulWithMetrics(
      final WasmTestCase testCase,
      final String category,
      final Duration executionTime,
      final RuntimeType runtimeType,
      final ThreadingPerformanceMetrics performanceMetrics) {

    return new ThreadingTestResult(
        testCase,
        category,
        true,
        Optional.empty(),
        executionTime,
        Instant.now(),
        Optional.of(runtimeType),
        Optional.of(performanceMetrics));
  }

  /**
   * Creates a failed threading test result.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param failureReason the reason for failure
   * @param executionTime the execution duration
   * @return failed test result
   */
  public static ThreadingTestResult failed(
      final WasmTestCase testCase,
      final String category,
      final String failureReason,
      final Duration executionTime) {

    return new ThreadingTestResult(
        testCase,
        category,
        false,
        Optional.of(failureReason),
        executionTime,
        Instant.now(),
        Optional.empty(),
        Optional.empty());
  }

  /**
   * Gets the test case that was executed.
   *
   * @return the test case
   */
  public WasmTestCase getTestCase() {
    return testCase;
  }

  /**
   * Gets the threading test category.
   *
   * @return the test category
   */
  public String getCategory() {
    return category;
  }

  /**
   * Checks if the test was successful.
   *
   * @return true if the test passed
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the failure reason if the test failed.
   *
   * @return optional failure reason
   */
  public Optional<String> getFailureReason() {
    return failureReason;
  }

  /**
   * Gets the test execution time.
   *
   * @return the execution duration
   */
  public Duration getExecutionTime() {
    return executionTime;
  }

  /**
   * Gets the execution timestamp.
   *
   * @return the execution timestamp
   */
  public Instant getExecutionTimestamp() {
    return executionTimestamp;
  }

  /**
   * Gets the runtime type used for execution.
   *
   * @return optional runtime type
   */
  public Optional<RuntimeType> getRuntimeType() {
    return runtimeType;
  }

  /**
   * Gets the performance metrics if available.
   *
   * @return optional performance metrics
   */
  public Optional<ThreadingPerformanceMetrics> getPerformanceMetrics() {
    return performanceMetrics;
  }

  /**
   * Gets a display name for this test result.
   *
   * @return the display name
   */
  public String getDisplayName() {
    return category + ":" + testCase.getDisplayName();
  }

  /**
   * Checks if this test result has performance metrics.
   *
   * @return true if performance metrics are available
   */
  public boolean hasPerformanceMetrics() {
    return performanceMetrics.isPresent();
  }

  /**
   * Gets the execution time in milliseconds.
   *
   * @return execution time in milliseconds
   */
  public long getExecutionTimeMillis() {
    return executionTime.toMillis();
  }

  /**
   * Checks if this is a concurrent test (thread safety validation).
   *
   * @return true if this is a concurrent execution test
   */
  public boolean isConcurrentTest() {
    return category.contains("concurrent") || category.contains("thread-safety");
  }

  /**
   * Checks if this is an atomic operation test.
   *
   * @return true if this tests atomic operations
   */
  public boolean isAtomicOperationTest() {
    return category.contains("atomic") || category.contains("cas");
  }

  /**
   * Checks if this is a shared memory test.
   *
   * @return true if this tests shared memory functionality
   */
  public boolean isSharedMemoryTest() {
    return category.contains("shared") || category.contains("memory");
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ThreadingTestResult that = (ThreadingTestResult) obj;
    return successful == that.successful
        && Objects.equals(testCase, that.testCase)
        && Objects.equals(category, that.category)
        && Objects.equals(executionTimestamp, that.executionTimestamp);
  }

  @Override
  public int hashCode() {
    return Objects.hash(testCase, category, successful, executionTimestamp);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ThreadingTestResult{");
    sb.append("testCase=").append(testCase.getDisplayName());
    sb.append(", category='").append(category).append('\'');
    sb.append(", successful=").append(successful);

    if (!successful && failureReason.isPresent()) {
      sb.append(", failureReason='").append(failureReason.get()).append('\'');
    }

    sb.append(", executionTime=").append(executionTime.toMillis()).append("ms");

    if (runtimeType.isPresent()) {
      sb.append(", runtime=").append(runtimeType.get());
    }

    if (hasPerformanceMetrics()) {
      sb.append(", hasMetrics=true");
    }

    sb.append('}');
    return sb.toString();
  }

  /** Performance metrics specific to threading and atomic operations for detailed analysis. */
  public static final class ThreadingPerformanceMetrics {
    private final long atomicOperationsPerSecond;
    private final long sharedMemoryAccessesPerSecond;
    private final double averageContention;
    private final long totalSynchronizationTime;
    private final int concurrentThreadCount;

    /**
     * Creates threading performance metrics.
     *
     * @param atomicOperationsPerSecond atomic operations per second
     * @param sharedMemoryAccessesPerSecond shared memory accesses per second
     * @param averageContention average contention ratio (0.0 to 1.0)
     * @param totalSynchronizationTime total time spent in synchronization (nanoseconds)
     * @param concurrentThreadCount number of concurrent threads used
     */
    public ThreadingPerformanceMetrics(
        final long atomicOperationsPerSecond,
        final long sharedMemoryAccessesPerSecond,
        final double averageContention,
        final long totalSynchronizationTime,
        final int concurrentThreadCount) {

      this.atomicOperationsPerSecond = atomicOperationsPerSecond;
      this.sharedMemoryAccessesPerSecond = sharedMemoryAccessesPerSecond;
      this.averageContention = averageContention;
      this.totalSynchronizationTime = totalSynchronizationTime;
      this.concurrentThreadCount = concurrentThreadCount;
    }

    /**
     * Gets the atomic operations per second.
     *
     * @return atomic operations per second
     */
    public long getAtomicOperationsPerSecond() {
      return atomicOperationsPerSecond;
    }

    /**
     * Gets the shared memory accesses per second.
     *
     * @return shared memory accesses per second
     */
    public long getSharedMemoryAccessesPerSecond() {
      return sharedMemoryAccessesPerSecond;
    }

    /**
     * Gets the average contention ratio.
     *
     * @return average contention ratio (0.0 to 1.0)
     */
    public double getAverageContention() {
      return averageContention;
    }

    /**
     * Gets the total synchronization time in nanoseconds.
     *
     * @return total synchronization time in nanoseconds
     */
    public long getTotalSynchronizationTime() {
      return totalSynchronizationTime;
    }

    /**
     * Gets the number of concurrent threads used.
     *
     * @return concurrent thread count
     */
    public int getConcurrentThreadCount() {
      return concurrentThreadCount;
    }

    /**
     * Gets the synchronization overhead as a percentage.
     *
     * @param totalExecutionTime the total execution time in nanoseconds
     * @return synchronization overhead percentage
     */
    public double getSynchronizationOverheadPercent(final long totalExecutionTime) {
      if (totalExecutionTime == 0) {
        return 0.0;
      }
      return (double) totalSynchronizationTime / totalExecutionTime * 100.0;
    }

    @Override
    public String toString() {
      return "ThreadingPerformanceMetrics{"
          + "atomicOps/sec="
          + atomicOperationsPerSecond
          + ", memoryAccess/sec="
          + sharedMemoryAccessesPerSecond
          + ", contention="
          + String.format("%.2f", averageContention)
          + ", syncTime="
          + (totalSynchronizationTime / 1_000_000)
          + "ms"
          + ", threads="
          + concurrentThreadCount
          + '}';
    }
  }
}
