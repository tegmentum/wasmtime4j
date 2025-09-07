package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Results from benchmarking a single WebAssembly test case with a specific runtime. Contains
 * detailed performance metrics, statistics, and failure information.
 */
public final class PerformanceBenchmarkResult {
  private final String testName;
  private final RuntimeType runtimeType;
  private final boolean successful;
  private final Duration warmupTime;
  private final List<WasmPerformanceTestFramework.BenchmarkRun> benchmarkRuns;
  private final WasmPerformanceTestFramework.PerformanceStatistics statistics;
  private final Exception failureException;
  private final Instant benchmarkTime;

  private PerformanceBenchmarkResult(final Builder builder) {
    this.testName = builder.testName;
    this.runtimeType = builder.runtimeType;
    this.successful = builder.successful;
    this.warmupTime = builder.warmupTime;
    this.benchmarkRuns =
        builder.benchmarkRuns != null
            ? java.util.Collections.unmodifiableList(
                new java.util.ArrayList<>(builder.benchmarkRuns))
            : java.util.Collections.emptyList();
    this.statistics = builder.statistics;
    this.failureException = builder.failureException;
    this.benchmarkTime = Instant.now();
  }

  /**
   * Creates a failed benchmark result.
   *
   * @param testName the test name
   * @param runtimeType the runtime type
   * @param exception the failure exception
   * @return the failed benchmark result
   */
  public static PerformanceBenchmarkResult failed(
      final String testName, final RuntimeType runtimeType, final Exception exception) {
    return new Builder(testName, runtimeType).successful(false).failureException(exception).build();
  }

  /**
   * Gets the test name.
   *
   * @return the test name
   */
  public String getTestName() {
    return testName;
  }

  /**
   * Gets the runtime type.
   *
   * @return the runtime type
   */
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  /**
   * Checks if the benchmark was successful.
   *
   * @return true if the benchmark was successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the warmup time.
   *
   * @return the warmup time
   */
  public Duration getWarmupTime() {
    return warmupTime;
  }

  /**
   * Gets the benchmark runs.
   *
   * @return the list of benchmark runs
   */
  public List<WasmPerformanceTestFramework.BenchmarkRun> getBenchmarkRuns() {
    return benchmarkRuns;
  }

  /**
   * Gets the performance statistics.
   *
   * @return the performance statistics
   */
  public WasmPerformanceTestFramework.PerformanceStatistics getStatistics() {
    return statistics;
  }

  /**
   * Gets the failure exception if the benchmark failed.
   *
   * @return the failure exception, or empty if successful
   */
  public Optional<Exception> getFailureException() {
    return Optional.ofNullable(failureException);
  }

  /**
   * Gets the failure reason if the benchmark failed.
   *
   * @return the failure reason, or empty if successful
   */
  public Optional<String> getFailureReason() {
    if (failureException != null) {
      final String message = failureException.getMessage();
      return Optional.of(message != null ? message : failureException.getClass().getSimpleName());
    }
    return Optional.empty();
  }

  /**
   * Gets the time when this benchmark was performed.
   *
   * @return the benchmark time
   */
  public Instant getBenchmarkTime() {
    return benchmarkTime;
  }

  /**
   * Gets the total number of iterations across all benchmark runs.
   *
   * @return the total number of iterations
   */
  public int getTotalIterations() {
    return benchmarkRuns.stream()
        .mapToInt(WasmPerformanceTestFramework.BenchmarkRun::getIterationCount)
        .sum();
  }

  /**
   * Gets the total benchmark execution time (excluding warmup).
   *
   * @return the total benchmark execution time
   */
  public Duration getTotalBenchmarkTime() {
    return benchmarkRuns.stream()
        .map(WasmPerformanceTestFramework.BenchmarkRun::getTotalRunTime)
        .reduce(Duration.ZERO, Duration::plus);
  }

  /**
   * Checks if the benchmark results are statistically significant.
   *
   * @param confidenceThreshold the confidence threshold (e.g., 0.05 for 95% confidence)
   * @return true if results are statistically significant
   */
  public boolean isStatisticallySignificant(final double confidenceThreshold) {
    if (!successful || statistics.getSampleSize() < 30) {
      return false;
    }

    // Simple coefficient of variation check - more sophisticated tests could be implemented
    final double coefficientOfVariation = statistics.getCoefficientOfVariation();
    return coefficientOfVariation < (confidenceThreshold * 100.0);
  }

  /**
   * Compares this benchmark result with another for the same test.
   *
   * @param other the other benchmark result
   * @return performance comparison result
   */
  public PerformanceComparison compareWith(final PerformanceBenchmarkResult other) {
    Objects.requireNonNull(other, "other cannot be null");

    if (!testName.equals(other.testName)) {
      throw new IllegalArgumentException("Cannot compare results from different tests");
    }

    return PerformanceComparison.create(this, other);
  }

  /**
   * Creates a detailed report of this benchmark result.
   *
   * @return a formatted report
   */
  public String createDetailedReport() {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Benchmark Result\n");
    report.append("===========================\n\n");

    report.append(String.format("Test: %s\n", testName));
    report.append(String.format("Runtime: %s\n", runtimeType.name()));
    report.append(String.format("Status: %s\n", successful ? "SUCCESSFUL" : "FAILED"));
    report.append(String.format("Benchmark Time: %s\n\n", benchmarkTime));

    if (successful) {
      report.append(String.format("Warmup Time: %.3fms\n", warmupTime.toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "Total Benchmark Time: %.3fs\n", getTotalBenchmarkTime().toMillis() / 1000.0));
      report.append(String.format("Total Iterations: %d\n", getTotalIterations()));
      report.append(String.format("Benchmark Runs: %d\n\n", benchmarkRuns.size()));

      // Performance statistics
      report.append("Performance Statistics:\n");
      report.append("-----------------------\n");
      report.append(
          String.format(
              "Mean Execution Time: %.3fms\n",
              statistics.getMeanExecutionTime().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "Median Execution Time: %.3fms\n",
              statistics.getMedianExecutionTime().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "Min Execution Time: %.3fms\n",
              statistics.getMinExecutionTime().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "Max Execution Time: %.3fms\n",
              statistics.getMaxExecutionTime().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "95th Percentile: %.3fms\n",
              statistics.getP95ExecutionTime().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "99th Percentile: %.3fms\n",
              statistics.getP99ExecutionTime().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "Standard Deviation: %.3fms\n",
              statistics.getStandardDeviation().toNanos() / 1_000_000.0));
      report.append(
          String.format(
              "Coefficient of Variation: %.2f%%\n", statistics.getCoefficientOfVariation()));
      report.append(String.format("Sample Size: %d\n", statistics.getSampleSize()));

      // Benchmark run breakdown
      report.append("\nBenchmark Run Details:\n");
      report.append("----------------------\n");
      for (int i = 0; i < benchmarkRuns.size(); i++) {
        final WasmPerformanceTestFramework.BenchmarkRun run = benchmarkRuns.get(i);
        report.append(
            String.format(
                "Run %d: %d iterations, %.3fms average, %.3fs total\n",
                i + 1,
                run.getIterationCount(),
                run.getAverageExecutionTime().toNanos() / 1_000_000.0,
                run.getTotalRunTime().toMillis() / 1000.0));
      }

    } else {
      report.append("Failure Information:\n");
      report.append("-------------------\n");
      getFailureReason().ifPresent(reason -> report.append(String.format("Reason: %s\n", reason)));

      if (failureException != null) {
        report.append(
            String.format("Exception: %s\n", failureException.getClass().getSimpleName()));
        if (failureException.getMessage() != null) {
          report.append(String.format("Message: %s\n", failureException.getMessage()));
        }
      }
    }

    return report.toString();
  }

  /**
   * Creates a brief summary suitable for logging or console output.
   *
   * @return a brief summary
   */
  public String createBriefSummary() {
    if (successful) {
      return String.format(
          "[%s] %s: %.3fms mean (±%.2f%%)",
          runtimeType.name(),
          testName,
          statistics.getMeanExecutionTime().toNanos() / 1_000_000.0,
          statistics.getCoefficientOfVariation());
    } else {
      return String.format(
          "[%s] %s: FAILED - %s",
          runtimeType.name(), testName, getFailureReason().orElse("Unknown error"));
    }
  }

  @Override
  public String toString() {
    return String.format(
        "PerformanceBenchmarkResult{test=%s, runtime=%s, successful=%s}",
        testName, runtimeType.name(), successful);
  }

  /** Builder for PerformanceBenchmarkResult. */
  public static final class Builder {
    private final String testName;
    private final RuntimeType runtimeType;
    private boolean successful = true;
    private Duration warmupTime = Duration.ZERO;
    private List<WasmPerformanceTestFramework.BenchmarkRun> benchmarkRuns;
    private WasmPerformanceTestFramework.PerformanceStatistics statistics =
        WasmPerformanceTestFramework.PerformanceStatistics.empty();
    private Exception failureException;

    /**
     * Creates a builder for the specified test and runtime.
     *
     * @param testName the test name
     * @param runtimeType the runtime type
     */
    public Builder(final String testName, final RuntimeType runtimeType) {
      this.testName = Objects.requireNonNull(testName, "testName cannot be null");
      this.runtimeType = Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
    }

    /**
     * Sets whether the benchmark was successful.
     *
     * @param successful true if successful
     * @return this builder
     */
    public Builder successful(final boolean successful) {
      this.successful = successful;
      return this;
    }

    /**
     * Sets the warmup time.
     *
     * @param warmupTime the warmup time
     * @return this builder
     */
    public Builder warmupTime(final Duration warmupTime) {
      this.warmupTime = Objects.requireNonNull(warmupTime, "warmupTime cannot be null");
      return this;
    }

    /**
     * Sets the benchmark runs.
     *
     * @param benchmarkRuns the benchmark runs
     * @return this builder
     */
    public Builder benchmarkRuns(
        final List<WasmPerformanceTestFramework.BenchmarkRun> benchmarkRuns) {
      this.benchmarkRuns = benchmarkRuns;
      return this;
    }

    /**
     * Sets the performance statistics.
     *
     * @param statistics the performance statistics
     * @return this builder
     */
    public Builder statistics(final WasmPerformanceTestFramework.PerformanceStatistics statistics) {
      this.statistics = Objects.requireNonNull(statistics, "statistics cannot be null");
      return this;
    }

    /**
     * Sets the failure exception.
     *
     * @param failureException the failure exception
     * @return this builder
     */
    public Builder failureException(final Exception failureException) {
      this.failureException = failureException;
      return this;
    }

    /**
     * Builds the performance benchmark result.
     *
     * @return the performance benchmark result
     */
    public PerformanceBenchmarkResult build() {
      return new PerformanceBenchmarkResult(this);
    }
  }
}
