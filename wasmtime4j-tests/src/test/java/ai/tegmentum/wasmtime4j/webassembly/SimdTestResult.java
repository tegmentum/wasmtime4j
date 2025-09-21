package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the result of executing a single SIMD test case, including execution details,
 * performance metrics, and validation outcomes.
 *
 * <p>This class provides comprehensive information about SIMD test execution to support detailed
 * analysis and reporting of vector operation testing results.
 */
public final class SimdTestResult {
  private final WasmTestCase testCase;
  private final String category;
  private final boolean successful;
  private final Optional<String> failureReason;
  private final Duration executionTime;
  private final Instant executionTimestamp;
  private final Optional<RuntimeType> runtimeType;
  private final Optional<SimdPerformanceMetrics> performanceMetrics;

  private SimdTestResult(
      final WasmTestCase testCase,
      final String category,
      final boolean successful,
      final Optional<String> failureReason,
      final Duration executionTime,
      final Instant executionTimestamp,
      final Optional<RuntimeType> runtimeType,
      final Optional<SimdPerformanceMetrics> performanceMetrics) {

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
   * Creates a successful SIMD test result.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param executionTime the execution duration
   * @param runtimeType the runtime type used
   * @return successful test result
   */
  public static SimdTestResult successful(
      final WasmTestCase testCase,
      final String category,
      final Duration executionTime,
      final RuntimeType runtimeType) {

    return new SimdTestResult(
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
   * Creates a successful SIMD test result with performance metrics.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param executionTime the execution duration
   * @param runtimeType the runtime type used
   * @param performanceMetrics the performance metrics
   * @return successful test result with performance data
   */
  public static SimdTestResult successfulWithMetrics(
      final WasmTestCase testCase,
      final String category,
      final Duration executionTime,
      final RuntimeType runtimeType,
      final SimdPerformanceMetrics performanceMetrics) {

    return new SimdTestResult(
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
   * Creates a failed SIMD test result.
   *
   * @param testCase the test case that was executed
   * @param category the test category
   * @param failureReason the reason for failure
   * @param executionTime the execution duration
   * @return failed test result
   */
  public static SimdTestResult failed(
      final WasmTestCase testCase,
      final String category,
      final String failureReason,
      final Duration executionTime) {

    return new SimdTestResult(
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
   * Gets the SIMD test category.
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
  public Optional<SimdPerformanceMetrics> getPerformanceMetrics() {
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
   * Checks if this is a fast test (executes quickly).
   *
   * @return true if execution time is less than 1 second
   */
  public boolean isFastTest() {
    return executionTime.toMillis() < 1000;
  }

  /**
   * Checks if this is a slow test (takes significant time).
   *
   * @return true if execution time is more than 10 seconds
   */
  public boolean isSlowTest() {
    return executionTime.toSeconds() > 10;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SimdTestResult that = (SimdTestResult) obj;
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
    final StringBuilder sb = new StringBuilder("SimdTestResult{");
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

  /** Performance metrics specific to SIMD operations for detailed analysis. */
  public static final class SimdPerformanceMetrics {
    private final long vectorOperationsPerSecond;
    private final long memoryBandwidthMBps;
    private final double cpuUtilizationPercent;
    private final long peakMemoryUsageBytes;

    /**
     * Creates SIMD performance metrics.
     *
     * @param vectorOperationsPerSecond vector operations per second
     * @param memoryBandwidthMBps memory bandwidth in MB/s
     * @param cpuUtilizationPercent CPU utilization percentage
     * @param peakMemoryUsageBytes peak memory usage in bytes
     */
    public SimdPerformanceMetrics(
        final long vectorOperationsPerSecond,
        final long memoryBandwidthMBps,
        final double cpuUtilizationPercent,
        final long peakMemoryUsageBytes) {

      this.vectorOperationsPerSecond = vectorOperationsPerSecond;
      this.memoryBandwidthMBps = memoryBandwidthMBps;
      this.cpuUtilizationPercent = cpuUtilizationPercent;
      this.peakMemoryUsageBytes = peakMemoryUsageBytes;
    }

    /**
     * Gets the vector operations per second.
     *
     * @return vector operations per second
     */
    public long getVectorOperationsPerSecond() {
      return vectorOperationsPerSecond;
    }

    /**
     * Gets the memory bandwidth in MB/s.
     *
     * @return memory bandwidth in MB/s
     */
    public long getMemoryBandwidthMBps() {
      return memoryBandwidthMBps;
    }

    /**
     * Gets the CPU utilization percentage.
     *
     * @return CPU utilization percentage
     */
    public double getCpuUtilizationPercent() {
      return cpuUtilizationPercent;
    }

    /**
     * Gets the peak memory usage in bytes.
     *
     * @return peak memory usage in bytes
     */
    public long getPeakMemoryUsageBytes() {
      return peakMemoryUsageBytes;
    }

    @Override
    public String toString() {
      return "SimdPerformanceMetrics{"
          + "vectorOps/sec="
          + vectorOperationsPerSecond
          + ", bandwidth="
          + memoryBandwidthMBps
          + "MB/s"
          + ", cpu="
          + String.format("%.1f", cpuUtilizationPercent)
          + "%"
          + ", peakMem="
          + (peakMemoryUsageBytes / 1024 / 1024)
          + "MB"
          + '}';
    }
  }
}
