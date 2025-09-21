package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of executing a single Exception handling test case, including execution
 * details, exception flow validation, and performance metrics.
 *
 * <p>This class provides comprehensive information about exception test execution to support
 * detailed analysis and reporting of try/catch/throw operation testing results.
 */
public final class ExceptionTestResult {
  private final WasmTestCase testCase;
  private final String category;
  private final boolean successful;
  private final Optional<String> failureReason;
  private final Duration executionTime;
  private final Instant executionTimestamp;
  private final Optional<RuntimeType> runtimeType;
  private final Optional<ExceptionPerformanceMetrics> performanceMetrics;

  private ExceptionTestResult(
      final WasmTestCase testCase,
      final String category,
      final boolean successful,
      final Optional<String> failureReason,
      final Duration executionTime,
      final Instant executionTimestamp,
      final Optional<RuntimeType> runtimeType,
      final Optional<ExceptionPerformanceMetrics> performanceMetrics) {

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
   * Creates a successful exception test result.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param executionTime the execution duration
   * @param runtimeType the runtime type used
   * @return successful test result
   */
  public static ExceptionTestResult successful(
      final WasmTestCase testCase,
      final String category,
      final Duration executionTime,
      final RuntimeType runtimeType) {

    return new ExceptionTestResult(
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
   * Creates a successful exception test result with performance metrics.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param executionTime the execution duration
   * @param runtimeType the runtime type used
   * @param performanceMetrics the performance metrics
   * @return successful test result with performance data
   */
  public static ExceptionTestResult successfulWithMetrics(
      final WasmTestCase testCase,
      final String category,
      final Duration executionTime,
      final RuntimeType runtimeType,
      final ExceptionPerformanceMetrics performanceMetrics) {

    return new ExceptionTestResult(
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
   * Creates a failed exception test result.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param failureReason the reason for failure
   * @param executionTime the execution duration
   * @return failed test result
   */
  public static ExceptionTestResult failed(
      final WasmTestCase testCase,
      final String category,
      final String failureReason,
      final Duration executionTime) {

    return new ExceptionTestResult(
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
   * Gets the exception test category.
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
  public Optional<ExceptionPerformanceMetrics> getPerformanceMetrics() {
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
   * Checks if this is a basic exception test (try/catch/throw).
   *
   * @return true if this tests basic exception operations
   */
  public boolean isBasicExceptionTest() {
    return category.contains("basic") || category.contains("exception");
  }

  /**
   * Checks if this is an exception type test.
   *
   * @return true if this tests exception types
   */
  public boolean isExceptionTypeTest() {
    return category.contains("type") || category.contains("tag");
  }

  /**
   * Checks if this is a nested exception test.
   *
   * @return true if this tests nested exception handling
   */
  public boolean isNestedExceptionTest() {
    return category.contains("nested") || category.contains("chain");
  }

  /**
   * Checks if this is a cross-module exception test.
   *
   * @return true if this tests cross-module exception propagation
   */
  public boolean isCrossModuleTest() {
    return category.contains("cross") || category.contains("module");
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExceptionTestResult that = (ExceptionTestResult) obj;
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
    final StringBuilder sb = new StringBuilder("ExceptionTestResult{");
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

  /** Performance metrics specific to exception handling operations for detailed analysis. */
  public static final class ExceptionPerformanceMetrics {
    private final long exceptionsPerSecond;
    private final long averageThrowLatencyNanos;
    private final long averageCatchLatencyNanos;
    private final double exceptionOverheadPercent;
    private final int maxNestingDepth;

    /**
     * Creates exception performance metrics.
     *
     * @param exceptionsPerSecond exceptions thrown/caught per second
     * @param averageThrowLatencyNanos average time to throw an exception (nanoseconds)
     * @param averageCatchLatencyNanos average time to catch an exception (nanoseconds)
     * @param exceptionOverheadPercent execution overhead when exceptions are enabled
     * @param maxNestingDepth maximum nesting depth tested
     */
    public ExceptionPerformanceMetrics(
        final long exceptionsPerSecond,
        final long averageThrowLatencyNanos,
        final long averageCatchLatencyNanos,
        final double exceptionOverheadPercent,
        final int maxNestingDepth) {

      this.exceptionsPerSecond = exceptionsPerSecond;
      this.averageThrowLatencyNanos = averageThrowLatencyNanos;
      this.averageCatchLatencyNanos = averageCatchLatencyNanos;
      this.exceptionOverheadPercent = exceptionOverheadPercent;
      this.maxNestingDepth = maxNestingDepth;
    }

    /**
     * Gets the exceptions per second.
     *
     * @return exceptions per second
     */
    public long getExceptionsPerSecond() {
      return exceptionsPerSecond;
    }

    /**
     * Gets the average throw latency in nanoseconds.
     *
     * @return average throw latency in nanoseconds
     */
    public long getAverageThrowLatencyNanos() {
      return averageThrowLatencyNanos;
    }

    /**
     * Gets the average catch latency in nanoseconds.
     *
     * @return average catch latency in nanoseconds
     */
    public long getAverageCatchLatencyNanos() {
      return averageCatchLatencyNanos;
    }

    /**
     * Gets the exception overhead percentage.
     *
     * @return exception overhead percentage
     */
    public double getExceptionOverheadPercent() {
      return exceptionOverheadPercent;
    }

    /**
     * Gets the maximum nesting depth tested.
     *
     * @return maximum nesting depth
     */
    public int getMaxNestingDepth() {
      return maxNestingDepth;
    }

    /**
     * Gets the total exception handling latency.
     *
     * @return total latency in nanoseconds
     */
    public long getTotalHandlingLatencyNanos() {
      return averageThrowLatencyNanos + averageCatchLatencyNanos;
    }

    @Override
    public String toString() {
      return "ExceptionPerformanceMetrics{"
          + "exceptions/sec="
          + exceptionsPerSecond
          + ", throwLatency="
          + (averageThrowLatencyNanos / 1000)
          + "μs"
          + ", catchLatency="
          + (averageCatchLatencyNanos / 1000)
          + "μs"
          + ", overhead="
          + String.format("%.1f", exceptionOverheadPercent)
          + "%"
          + ", maxDepth="
          + maxNestingDepth
          + '}';
    }
  }
}
