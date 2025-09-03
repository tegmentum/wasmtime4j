package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comprehensive results from WebAssembly performance testing across multiple runtimes.
 * Provides detailed performance comparisons, statistical analysis, and regression detection.
 */
public final class WasmPerformanceTestResults {
  private final Map<RuntimeType, Map<String, PerformanceBenchmarkResult>> runtimeResults;
  private final Duration totalExecutionTime;
  private final Instant testTime;
  private final int totalTestCases;
  private final int successfulTestCases;
  private final int failedTestCases;

  private WasmPerformanceTestResults(final Builder builder) {
    this.runtimeResults = Collections.unmodifiableMap(new EnumMap<>(builder.runtimeResults));
    this.totalExecutionTime = builder.totalExecutionTime;
    this.testTime = Instant.now();
    
    // Calculate aggregated statistics
    int totalTests = 0;
    int successful = 0;
    int failed = 0;

    for (final Map<String, PerformanceBenchmarkResult> runtimeTestResults : runtimeResults.values()) {
      totalTests += runtimeTestResults.size();
      for (final PerformanceBenchmarkResult result : runtimeTestResults.values()) {
        if (result.isSuccessful()) {
          successful++;
        } else {
          failed++;
        }
      }
    }

    this.totalTestCases = totalTests;
    this.successfulTestCases = successful;
    this.failedTestCases = failed;
  }

  /**
   * Gets the performance results for a specific runtime.
   *
   * @param runtimeType the runtime type
   * @return the performance results map, or empty map if runtime not tested
   */
  public Map<String, PerformanceBenchmarkResult> getRuntimeResults(final RuntimeType runtimeType) {
    return runtimeResults.getOrDefault(runtimeType, Collections.emptyMap());
  }

  /**
   * Gets all runtime results.
   *
   * @return map of all runtime performance results
   */
  public Map<RuntimeType, Map<String, PerformanceBenchmarkResult>> getAllRuntimeResults() {
    return runtimeResults;
  }

  /**
   * Gets the set of tested runtime types.
   *
   * @return set of tested runtimes
   */
  public Set<RuntimeType> getTestedRuntimes() {
    return runtimeResults.keySet();
  }

  /**
   * Gets the performance result for a specific test case and runtime.
   *
   * @param testName the test case name
   * @param runtimeType the runtime type
   * @return the performance result, or null if not found
   */
  public PerformanceBenchmarkResult getResult(final String testName, final RuntimeType runtimeType) {
    final Map<String, PerformanceBenchmarkResult> runtimeTestResults = getRuntimeResults(runtimeType);
    return runtimeTestResults.get(testName);
  }

  /**
   * Gets the total execution time for all performance tests.
   *
   * @return the total execution time
   */
  public Duration getTotalExecutionTime() {
    return totalExecutionTime;
  }

  /**
   * Gets the time when these tests were executed.
   *
   * @return the test execution time
   */
  public Instant getTestTime() {
    return testTime;
  }

  /**
   * Gets the total number of test cases executed across all runtimes.
   *
   * @return the total number of test cases
   */
  public int getTotalTestCases() {
    return totalTestCases;
  }

  /**
   * Gets the number of successful test cases.
   *
   * @return the number of successful test cases
   */
  public int getSuccessfulTestCases() {
    return successfulTestCases;
  }

  /**
   * Gets the number of failed test cases.
   *
   * @return the number of failed test cases
   */
  public int getFailedTestCases() {
    return failedTestCases;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return the success rate (0.0 to 100.0)
   */
  public double getSuccessRate() {
    if (totalTestCases == 0) {
      return 0.0;
    }
    return (double) successfulTestCases / totalTestCases * 100.0;
  }

  /**
   * Gets all unique test case names.
   *
   * @return set of unique test case names
   */
  public Set<String> getTestCaseNames() {
    return runtimeResults.values().stream()
        .flatMap(results -> results.keySet().stream())
        .collect(Collectors.toSet());
  }

  /**
   * Compares performance between two runtimes for all common test cases.
   *
   * @param runtime1 the first runtime
   * @param runtime2 the second runtime
   * @return performance comparison results
   */
  public RuntimePerformanceComparison compareRuntimes(final RuntimeType runtime1, 
                                                      final RuntimeType runtime2) {
    final Map<String, PerformanceBenchmarkResult> results1 = getRuntimeResults(runtime1);
    final Map<String, PerformanceBenchmarkResult> results2 = getRuntimeResults(runtime2);

    return RuntimePerformanceComparison.create(runtime1, runtime2, results1, results2);
  }

  /**
   * Gets performance regressions by comparing against baseline results.
   *
   * @param baselineResults the baseline performance results
   * @param regressionThreshold the threshold for detecting regressions (e.g., 0.1 for 10%)
   * @return list of detected performance regressions
   */
  public List<PerformanceRegression> detectRegressions(
      final WasmPerformanceTestResults baselineResults,
      final double regressionThreshold) {
    
    final List<PerformanceRegression> regressions = new java.util.ArrayList<>();

    for (final Map.Entry<RuntimeType, Map<String, PerformanceBenchmarkResult>> entry : 
         runtimeResults.entrySet()) {
      final RuntimeType runtimeType = entry.getKey();
      final Map<String, PerformanceBenchmarkResult> currentResults = entry.getValue();
      final Map<String, PerformanceBenchmarkResult> baselineRuntimeResults = 
          baselineResults.getRuntimeResults(runtimeType);

      for (final Map.Entry<String, PerformanceBenchmarkResult> testEntry : currentResults.entrySet()) {
        final String testName = testEntry.getKey();
        final PerformanceBenchmarkResult currentResult = testEntry.getValue();
        final PerformanceBenchmarkResult baselineResult = baselineRuntimeResults.get(testName);

        if (baselineResult != null && currentResult.isSuccessful() && baselineResult.isSuccessful()) {
          final Duration currentMean = currentResult.getStatistics().getMeanExecutionTime();
          final Duration baselineMean = baselineResult.getStatistics().getMeanExecutionTime();

          final double performanceChange = 
              (currentMean.toNanos() - baselineMean.toNanos()) / (double) baselineMean.toNanos();

          if (performanceChange > regressionThreshold) {
            regressions.add(new PerformanceRegression(
                testName, runtimeType, baselineMean, currentMean, performanceChange));
          }
        }
      }
    }

    return regressions;
  }

  /**
   * Creates a comprehensive performance report.
   *
   * @return a formatted performance report
   */
  public String createComprehensiveReport() {
    final StringBuilder report = new StringBuilder();
    report.append("WebAssembly Performance Test Results\n");
    report.append("====================================\n\n");
    
    report.append(String.format("Test Execution Time: %s\n", testTime));
    report.append(String.format("Total Execution Time: %.2fs\n", totalExecutionTime.toMillis() / 1000.0));
    report.append(String.format("Runtimes Tested: %s\n", 
        getTestedRuntimes().stream().map(Enum::name).collect(Collectors.joining(", "))));
    report.append(String.format("Test Cases: %d total, %d successful (%.1f%%), %d failed\n\n", 
        totalTestCases, successfulTestCases, getSuccessRate(), failedTestCases));

    // Runtime-by-runtime breakdown
    for (final RuntimeType runtimeType : getTestedRuntimes()) {
      report.append(createRuntimeReport(runtimeType));
      report.append("\n");
    }

    // Cross-runtime comparisons
    final List<RuntimeType> runtimes = getTestedRuntimes().stream().collect(Collectors.toList());
    if (runtimes.size() >= 2) {
      report.append("Cross-Runtime Performance Comparison:\n");
      report.append("------------------------------------\n");
      
      for (int i = 0; i < runtimes.size(); i++) {
        for (int j = i + 1; j < runtimes.size(); j++) {
          final RuntimePerformanceComparison comparison = compareRuntimes(runtimes.get(i), runtimes.get(j));
          report.append(comparison.createSummaryReport());
          report.append("\n");
        }
      }
    }

    return report.toString();
  }

  /**
   * Creates a detailed report for a specific runtime.
   *
   * @param runtimeType the runtime type
   * @return the runtime performance report
   */
  private String createRuntimeReport(final RuntimeType runtimeType) {
    final StringBuilder report = new StringBuilder();
    final Map<String, PerformanceBenchmarkResult> results = getRuntimeResults(runtimeType);

    report.append(String.format("%s Runtime Performance:\n", runtimeType.name()));
    report.append("-".repeat(runtimeType.name().length() + 20)).append("\n");

    if (results.isEmpty()) {
      report.append("  No test results available\n");
      return report.toString();
    }

    final List<PerformanceBenchmarkResult> successfulResults = results.values().stream()
        .filter(PerformanceBenchmarkResult::isSuccessful)
        .collect(Collectors.toList());

    final List<PerformanceBenchmarkResult> failedResults = results.values().stream()
        .filter(result -> !result.isSuccessful())
        .collect(Collectors.toList());

    report.append(String.format("  Tests: %d total, %d successful, %d failed\n", 
        results.size(), successfulResults.size(), failedResults.size()));

    if (!successfulResults.isEmpty()) {
      // Calculate aggregate statistics
      final double avgMeanTime = successfulResults.stream()
          .mapToDouble(result -> result.getStatistics().getMeanExecutionTime().toNanos())
          .average()
          .orElse(0.0);

      final double avgMedianTime = successfulResults.stream()
          .mapToDouble(result -> result.getStatistics().getMedianExecutionTime().toNanos())
          .average()
          .orElse(0.0);

      report.append(String.format("  Average Mean Execution Time: %.3fms\n", avgMeanTime / 1_000_000.0));
      report.append(String.format("  Average Median Execution Time: %.3fms\n", avgMedianTime / 1_000_000.0));

      // Top performing and worst performing tests
      final PerformanceBenchmarkResult fastest = successfulResults.stream()
          .min((a, b) -> a.getStatistics().getMeanExecutionTime()
              .compareTo(b.getStatistics().getMeanExecutionTime()))
          .orElse(null);

      final PerformanceBenchmarkResult slowest = successfulResults.stream()
          .max((a, b) -> a.getStatistics().getMeanExecutionTime()
              .compareTo(b.getStatistics().getMeanExecutionTime()))
          .orElse(null);

      if (fastest != null) {
        report.append(String.format("  Fastest Test: %s (%.3fms mean)\n", 
            fastest.getTestName(), 
            fastest.getStatistics().getMeanExecutionTime().toNanos() / 1_000_000.0));
      }

      if (slowest != null) {
        report.append(String.format("  Slowest Test: %s (%.3fms mean)\n", 
            slowest.getTestName(), 
            slowest.getStatistics().getMeanExecutionTime().toNanos() / 1_000_000.0));
      }
    }

    if (!failedResults.isEmpty()) {
      report.append("  Failed Tests:\n");
      for (final PerformanceBenchmarkResult result : failedResults) {
        report.append(String.format("    %s: %s\n", 
            result.getTestName(), 
            result.getFailureReason().orElse("Unknown failure")));
      }
    }

    return report.toString();
  }

  @Override
  public String toString() {
    return String.format("WasmPerformanceTestResults{runtimes=%d, tests=%d, successful=%d, time=%.2fs}",
        getTestedRuntimes().size(),
        totalTestCases,
        successfulTestCases,
        totalExecutionTime.toMillis() / 1000.0);
  }

  /**
   * Builder for WasmPerformanceTestResults.
   */
  public static final class Builder {
    private final Map<RuntimeType, Map<String, PerformanceBenchmarkResult>> runtimeResults = new EnumMap<>(
        RuntimeType.class);
    private Duration totalExecutionTime = Duration.ZERO;

    /**
     * Adds performance results for a specific runtime.
     *
     * @param runtimeType the runtime type
     * @param results the performance results
     * @return this builder
     */
    public Builder addRuntimeResults(final RuntimeType runtimeType, 
                                     final Map<String, PerformanceBenchmarkResult> results) {
      Objects.requireNonNull(runtimeType, "runtimeType cannot be null");
      Objects.requireNonNull(results, "results cannot be null");
      
      runtimeResults.put(runtimeType, new HashMap<>(results));
      return this;
    }

    /**
     * Sets the total execution time.
     *
     * @param executionTime the total execution time
     * @return this builder
     */
    public Builder totalExecutionTime(final Duration executionTime) {
      this.totalExecutionTime = Objects.requireNonNull(executionTime, "executionTime cannot be null");
      return this;
    }

    /**
     * Builds the performance test results.
     *
     * @return the performance test results
     */
    public WasmPerformanceTestResults build() {
      return new WasmPerformanceTestResults(this);
    }
  }
}