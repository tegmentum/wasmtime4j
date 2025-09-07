package ai.tegmentum.wasmtime4j.webassembly;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Comprehensive results from executing all WebAssembly test suites across multiple runtimes.
 * Provides aggregated statistics and detailed breakdowns by test suite type.
 */
public final class WasmTestSuiteExecutionResults {
  private final Map<WasmTestSuiteLoader.TestSuiteType, WasmTestSuiteResults> suiteResults;
  private final Duration totalExecutionTime;
  private final int totalTestsExecuted;
  private final int totalSuccessfulTests;
  private final int totalFailedTests;
  private final int totalSkippedTests;

  private WasmTestSuiteExecutionResults(final Builder builder) {
    this.suiteResults = Collections.unmodifiableMap(new EnumMap<>(builder.suiteResults));
    this.totalExecutionTime = builder.totalExecutionTime;

    // Calculate aggregated statistics
    int testsExecuted = 0;
    int successfulTests = 0;
    int failedTests = 0;
    int skippedTests = 0;

    for (final WasmTestSuiteResults suiteResult : suiteResults.values()) {
      testsExecuted += suiteResult.getTotalTestsExecuted();
      successfulTests += suiteResult.getSuccessfulTests();
      failedTests += suiteResult.getFailedTests();
      skippedTests += suiteResult.getSkippedTests();
    }

    this.totalTestsExecuted = testsExecuted;
    this.totalSuccessfulTests = successfulTests;
    this.totalFailedTests = failedTests;
    this.totalSkippedTests = skippedTests;
  }

  /**
   * Gets the results for a specific test suite type.
   *
   * @param suiteType the test suite type
   * @return the test suite results, or null if not executed
   */
  public WasmTestSuiteResults getSuiteResults(final WasmTestSuiteLoader.TestSuiteType suiteType) {
    return suiteResults.get(suiteType);
  }

  /**
   * Gets all test suite results.
   *
   * @return map of test suite results by type
   */
  public Map<WasmTestSuiteLoader.TestSuiteType, WasmTestSuiteResults> getAllSuiteResults() {
    return suiteResults;
  }

  /**
   * Gets the total execution time for all test suites.
   *
   * @return the total execution time
   */
  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the total number of tests executed across all suites.
   *
   * @return the total number of tests executed
   */
  public int getTotalTestsExecuted() {
    return totalTestsExecuted;
  }

  /**
   * Gets the total number of successful tests across all suites.
   *
   * @return the total number of successful tests
   */
  public int getTotalSuccessfulTests() {
    return totalSuccessfulTests;
  }

  /**
   * Gets the total number of failed tests across all suites.
   *
   * @return the total number of failed tests
   */
  public int getTotalFailedTests() {
    return totalFailedTests;
  }

  /**
   * Gets the total number of skipped tests across all suites.
   *
   * @return the total number of skipped tests
   */
  public int getTotalSkippedTests() {
    return totalSkippedTests;
  }

  /**
   * Gets the overall success rate across all test suites.
   *
   * @return the success rate as a percentage (0.0 to 100.0)
   */
  public double getOverallSuccessRate() {
    if (totalTestsExecuted == 0) {
      return 0.0;
    }
    return (double) totalSuccessfulTests / totalTestsExecuted * 100.0;
  }

  /**
   * Checks if all executed tests were successful.
   *
   * @return true if all tests were successful
   */
  public boolean allTestsSuccessful() {
    return totalFailedTests == 0 && totalTestsExecuted > 0;
  }

  /**
   * Gets the number of test suite types executed.
   *
   * @return the number of test suite types executed
   */
  public int getExecutedSuiteCount() {
    return suiteResults.size();
  }

  /**
   * Creates a summary report of the execution results.
   *
   * @return a formatted summary report
   */
  public String createSummaryReport() {
    final StringBuilder report = new StringBuilder();
    report.append("WebAssembly Test Suite Execution Summary\n");
    report.append("========================================\n\n");

    report.append(
        String.format("Total Execution Time: %.2fs\n", totalExecutionTime.toMillis() / 1000.0));
    report.append(String.format("Test Suites Executed: %d\n", getExecutedSuiteCount()));
    report.append(String.format("Total Tests: %d\n", totalTestsExecuted));
    report.append(
        String.format("Successful: %d (%.1f%%)\n", totalSuccessfulTests, getOverallSuccessRate()));
    report.append(String.format("Failed: %d\n", totalFailedTests));
    report.append(String.format("Skipped: %d\n\n", totalSkippedTests));

    // Breakdown by test suite
    report.append("Test Suite Breakdown:\n");
    report.append("---------------------\n");
    for (final Map.Entry<WasmTestSuiteLoader.TestSuiteType, WasmTestSuiteResults> entry :
        suiteResults.entrySet()) {
      final WasmTestSuiteLoader.TestSuiteType suiteType = entry.getKey();
      final WasmTestSuiteResults results = entry.getValue();

      report.append(String.format("  %s:\n", suiteType.name()));
      report.append(
          String.format(
              "    Tests: %d, Successful: %d (%.1f%%), Failed: %d, Skipped: %d\n",
              results.getTotalTestsExecuted(),
              results.getSuccessfulTests(),
              results.getSuccessRate(),
              results.getFailedTests(),
              results.getSkippedTests()));
    }

    return report.toString();
  }

  @Override
  public String toString() {
    return String.format(
        "WasmTestSuiteExecutionResults{suites=%d, tests=%d, successful=%d, failed=%d, time=%.2fs}",
        getExecutedSuiteCount(),
        totalTestsExecuted,
        totalSuccessfulTests,
        totalFailedTests,
        totalExecutionTime.toMillis() / 1000.0);
  }

  /** Builder for WasmTestSuiteExecutionResults. */
  public static final class Builder {
    private final Map<WasmTestSuiteLoader.TestSuiteType, WasmTestSuiteResults> suiteResults =
        new EnumMap<>(WasmTestSuiteLoader.TestSuiteType.class);
    private Duration totalExecutionTime = Duration.ZERO;

    /**
     * Adds results for a specific test suite type.
     *
     * @param suiteType the test suite type
     * @param results the test suite results
     * @return this builder
     */
    public Builder addSuiteResults(
        final WasmTestSuiteLoader.TestSuiteType suiteType, final WasmTestSuiteResults results) {
      Objects.requireNonNull(suiteType, "suiteType cannot be null");
      Objects.requireNonNull(results, "results cannot be null");

      suiteResults.put(suiteType, results);
      return this;
    }

    /**
     * Sets the total execution time.
     *
     * @param executionTime the total execution time
     * @return this builder
     */
    public Builder totalExecutionTime(final Duration executionTime) {
      this.totalExecutionTime =
          Objects.requireNonNull(executionTime, "executionTime cannot be null");
      return this;
    }

    /**
     * Builds the execution results.
     *
     * @return the execution results
     */
    public WasmTestSuiteExecutionResults build() {
      return new WasmTestSuiteExecutionResults(this);
    }
  }
}
