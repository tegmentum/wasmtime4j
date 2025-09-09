package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Results from executing a single WebAssembly test suite type across multiple runtimes. Provides
 * detailed statistics and cross-runtime comparison data.
 */
public final class WasmTestSuiteResults {
  private final WasmTestSuiteLoader.TestSuiteType suiteType;
  private final Map<RuntimeType, Map<String, RuntimeTestExecution>> runtimeResults;
  private final int totalTestsExecuted;
  private final int successfulTests;
  private final int failedTests;
  private final int skippedTests;
  private final Duration totalExecutionTime;

  private WasmTestSuiteResults(final Builder builder) {
    this.suiteType = builder.suiteType;
    this.runtimeResults = Collections.unmodifiableMap(new EnumMap<>(builder.runtimeResults));

    // Calculate aggregated statistics
    int testsExecuted = 0;
    int successful = 0;
    int failed = 0;
    int skipped = 0;
    Duration totalTime = Duration.ZERO;

    for (final Map<String, RuntimeTestExecution> runtimeExecutions : runtimeResults.values()) {
      testsExecuted += runtimeExecutions.size();

      for (final RuntimeTestExecution execution : runtimeExecutions.values()) {
        if (execution.isSuccessful()) {
          successful++;
        } else if (execution.isSkipped()) {
          skipped++;
        } else {
          failed++;
        }

        totalTime = totalTime.plus(execution.getDuration());
      }
    }

    this.totalTestsExecuted = testsExecuted;
    this.successfulTests = successful;
    this.failedTests = failed;
    this.skippedTests = skipped;
    this.totalExecutionTime = totalTime;
  }

  /**
   * Creates an empty result set for a test suite with no tests.
   *
   * @param suiteType the test suite type
   * @return an empty test suite result
   */
  public static WasmTestSuiteResults empty(final WasmTestSuiteLoader.TestSuiteType suiteType) {
    return new Builder(suiteType).build();
  }

  /**
   * Gets the test suite type.
   *
   * @return the test suite type
   */
  public WasmTestSuiteLoader.TestSuiteType getSuiteType() {
    return suiteType;
  }

  /**
   * Gets the results for a specific runtime type.
   *
   * @param runtimeType the runtime type
   * @return the runtime results, or an empty map if not executed
   */
  public Map<String, RuntimeTestExecution> getRuntimeResults(final RuntimeType runtimeType) {
    return runtimeResults.getOrDefault(runtimeType, Collections.emptyMap());
  }

  /**
   * Gets all runtime results.
   *
   * @return map of runtime results by type
   */
  public Map<RuntimeType, Map<String, RuntimeTestExecution>> getAllRuntimeResults() {
    return runtimeResults;
  }

  /**
   * Gets the total number of tests executed.
   *
   * @return the total number of tests executed
   */
  public int getTotalTestsExecuted() {
    return totalTestsExecuted;
  }

  /**
   * Gets the number of successful tests.
   *
   * @return the number of successful tests
   */
  public int getSuccessfulTests() {
    return successfulTests;
  }

  /**
   * Gets the number of failed tests.
   *
   * @return the number of failed tests
   */
  public int getFailedTests() {
    return failedTests;
  }

  /**
   * Gets tests that failed on any runtime.
   *
   * @return map of failed test names to runtime types
   */
  public Map<String, java.util.Set<RuntimeType>> getFailedTestsByRuntime() {
    final Map<String, java.util.Set<RuntimeType>> failedTests = new java.util.HashMap<>();

    for (final Map.Entry<RuntimeType, Map<String, RuntimeTestExecution>> entry :
        runtimeResults.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final Map<String, RuntimeTestExecution> results = entry.getValue();

      for (final Map.Entry<String, RuntimeTestExecution> testEntry : results.entrySet()) {
        final String testName = testEntry.getKey();
        final RuntimeTestExecution execution = testEntry.getValue();

        if (!execution.isSuccessful() && !execution.isSkipped()) {
          failedTests.computeIfAbsent(testName, k -> new java.util.HashSet<>()).add(runtimeType);
        }
      }
    }

    return failedTests;
  }

  /**
   * Gets the number of skipped tests.
   *
   * @return the number of skipped tests
   */
  public int getSkippedTests() {
    return skippedTests;
  }

  /**
   * Gets the total execution time for all tests in this suite.
   *
   * @return the total execution time
   */
  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return the success rate (0.0 to 100.0)
   */
  public double getSuccessRate() {
    if (totalTestsExecuted == 0) {
      return 0.0;
    }
    return (double) successfulTests / totalTestsExecuted * 100.0;
  }

  /**
   * Gets the runtime types that were executed.
   *
   * @return the set of executed runtime types
   */
  public java.util.Set<RuntimeType> getExecutedRuntimes() {
    return runtimeResults.keySet();
  }

  /**
   * Gets the number of unique test cases (distinct test names).
   *
   * @return the number of unique test cases
   */
  public int getUniqueTestCount() {
    return runtimeResults.values().stream()
        .flatMap(results -> results.keySet().stream())
        .collect(java.util.stream.Collectors.toSet())
        .size();
  }

  /**
   * Checks if a specific test was executed successfully on all runtimes.
   *
   * @param testName the test name
   * @return true if the test was successful on all executed runtimes
   */
  public boolean isTestSuccessfulOnAllRuntimes(final String testName) {
    if (runtimeResults.isEmpty()) {
      return false;
    }

    for (final Map<String, RuntimeTestExecution> results : runtimeResults.values()) {
      final RuntimeTestExecution execution = results.get(testName);
      if (execution == null || !execution.isSuccessful()) {
        return false;
      }
    }

    return true;
  }

  /**
   * Gets tests that have inconsistent results between runtimes.
   *
   * @return set of test names with inconsistent results
   */
  public java.util.Set<String> getInconsistentTests() {
    final java.util.Set<String> inconsistentTests = new java.util.HashSet<>();

    // Get all unique test names
    final java.util.Set<String> allTestNames =
        runtimeResults.values().stream()
            .flatMap(results -> results.keySet().stream())
            .collect(java.util.stream.Collectors.toSet());

    for (final String testName : allTestNames) {
      boolean hasSuccess = false;
      boolean hasFailure = false;

      for (final Map<String, RuntimeTestExecution> results : runtimeResults.values()) {
        final RuntimeTestExecution execution = results.get(testName);
        if (execution != null && !execution.isSkipped()) {
          if (execution.isSuccessful()) {
            hasSuccess = true;
          } else {
            hasFailure = true;
          }
        }
      }

      if (hasSuccess && hasFailure) {
        inconsistentTests.add(testName);
      }
    }

    return inconsistentTests;
  }

  /**
   * Creates a detailed report of the test suite results.
   *
   * @return a formatted report
   */
  public String createDetailedReport() {
    final StringBuilder report = new StringBuilder();
    report.append(String.format("Test Suite: %s\n", suiteType.name()));
    report.append("=".repeat(suiteType.name().length() + 12)).append("\n\n");

    report.append(String.format("Total Tests: %d\n", getTotalTestsExecuted()));
    report.append(String.format("Unique Test Cases: %d\n", getUniqueTestCount()));
    report.append(String.format("Successful: %d (%.1f%%)\n", successfulTests, getSuccessRate()));
    report.append(String.format("Failed: %d\n", failedTests));
    report.append(String.format("Skipped: %d\n", skippedTests));
    report.append(
        String.format("Total Execution Time: %.2fs\n\n", totalExecutionTime.toMillis() / 1000.0));

    // Runtime breakdown
    report.append("Runtime Breakdown:\n");
    report.append("------------------\n");
    for (final Map.Entry<RuntimeType, Map<String, RuntimeTestExecution>> entry :
        runtimeResults.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final Map<String, RuntimeTestExecution> results = entry.getValue();

      final long successful =
          results.values().stream().mapToLong(e -> e.isSuccessful() ? 1 : 0).sum();
      final long failed =
          results.values().stream()
              .mapToLong(e -> !e.isSuccessful() && !e.isSkipped() ? 1 : 0)
              .sum();
      final long skipped = results.values().stream().mapToLong(e -> e.isSkipped() ? 1 : 0).sum();

      report.append(
          String.format(
              "  %s: %d tests, %d successful, %d failed, %d skipped\n",
              runtimeType.name(), results.size(), successful, failed, skipped));
    }

    // Inconsistent tests
    final java.util.Set<String> inconsistentTests = getInconsistentTests();
    if (!inconsistentTests.isEmpty()) {
      report.append("\nInconsistent Test Results:\n");
      report.append("--------------------------\n");
      for (final String testName : inconsistentTests) {
        report.append(String.format("  %s\n", testName));
      }
    }

    return report.toString();
  }

  @Override
  public String toString() {
    return String.format(
        "WasmTestSuiteResults{suite=%s, tests=%d, successful=%d, failed=%d, skipped=%d}",
        suiteType.name(), totalTestsExecuted, successfulTests, failedTests, skippedTests);
  }

  /** Builder for WasmTestSuiteResults. */
  public static final class Builder {
    private final WasmTestSuiteLoader.TestSuiteType suiteType;
    private final Map<RuntimeType, Map<String, RuntimeTestExecution>> runtimeResults =
        new EnumMap<>(RuntimeType.class);

    /**
     * Creates a builder for the specified test suite type.
     *
     * @param suiteType the test suite type
     */
    public Builder(final WasmTestSuiteLoader.TestSuiteType suiteType) {
      this.suiteType = Objects.requireNonNull(suiteType, "suiteType cannot be null");
    }

    /**
     * Adds results for a specific runtime type.
     *
     * @param runtimeType the runtime type
     * @param results the runtime test results
     * @return this builder
     */
    public Builder addRuntimeResults(
        final RuntimeType runtimeType, final Map<String, RuntimeTestExecution> results) {
      Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
      Objects.requireNonNull(results, "results cannot be null");

      runtimeResults.put(runtimeType, new java.util.HashMap<>(results));
      return this;
    }

    /**
     * Builds the test suite results.
     *
     * @return the test suite results
     */
    public WasmTestSuiteResults build() {
      return new WasmTestSuiteResults(this);
    }
  }
}
