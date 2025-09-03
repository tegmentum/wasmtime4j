package ai.tegmentum.wasmtime4j.webassembly;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Comparison of performance results between two WebAssembly runtimes.
 * Provides statistical analysis of performance differences and significance testing.
 */
public final class RuntimePerformanceComparison {
  private final RuntimeType runtime1;
  private final RuntimeType runtime2;
  private final Map<String, PerformanceComparison> testComparisons;
  private final int comparedTestCount;
  private final double averagePerformanceDifference;
  private final int significantDifferences;

  private RuntimePerformanceComparison(final RuntimeType runtime1,
                                       final RuntimeType runtime2,
                                       final Map<String, PerformanceComparison> testComparisons) {
    this.runtime1 = runtime1;
    this.runtime2 = runtime2;
    this.testComparisons = Map.copyOf(testComparisons);
    this.comparedTestCount = testComparisons.size();

    // Calculate aggregate statistics
    if (!testComparisons.isEmpty()) {
      this.averagePerformanceDifference = testComparisons.values().stream()
          .filter(comparison -> comparison.isSuccessful())
          .mapToDouble(PerformanceComparison::getPerformanceDifference)
          .average()
          .orElse(0.0);

      this.significantDifferences = (int) testComparisons.values().stream()
          .filter(PerformanceComparison::isStatisticallySignificant)
          .count();
    } else {
      this.averagePerformanceDifference = 0.0;
      this.significantDifferences = 0;
    }
  }

  /**
   * Creates a runtime performance comparison.
   *
   * @param runtime1 the first runtime
   * @param runtime2 the second runtime
   * @param results1 the first runtime results
   * @param results2 the second runtime results
   * @return the performance comparison
   */
  public static RuntimePerformanceComparison create(final RuntimeType runtime1,
                                                    final RuntimeType runtime2,
                                                    final Map<String, PerformanceBenchmarkResult> results1,
                                                    final Map<String, PerformanceBenchmarkResult> results2) {
    Objects.requireNonNull(runtime1, "runtime1 cannot be null");
    Objects.requireNonNull(runtime2, "runtime2 cannot be null");
    Objects.requireNonNull(results1, "results1 cannot be null");
    Objects.requireNonNull(results2, "results2 cannot be null");

    final Map<String, PerformanceComparison> testComparisons = new HashMap<>();

    // Find common test cases
    final Set<String> commonTests = results1.keySet().stream()
        .filter(results2::containsKey)
        .collect(Collectors.toSet());

    for (final String testName : commonTests) {
      final PerformanceBenchmarkResult result1 = results1.get(testName);
      final PerformanceBenchmarkResult result2 = results2.get(testName);

      if (result1 != null && result2 != null) {
        final PerformanceComparison comparison = result1.compareWith(result2);
        testComparisons.put(testName, comparison);
      }
    }

    return new RuntimePerformanceComparison(runtime1, runtime2, testComparisons);
  }

  /**
   * Gets the first runtime type.
   *
   * @return the first runtime type
   */
  public RuntimeType getRuntime1() {
    return runtime1;
  }

  /**
   * Gets the second runtime type.
   *
   * @return the second runtime type
   */
  public RuntimeType getRuntime2() {
    return runtime2;
  }

  /**
   * Gets the performance comparison for a specific test.
   *
   * @param testName the test name
   * @return the performance comparison, or null if not found
   */
  public PerformanceComparison getTestComparison(final String testName) {
    return testComparisons.get(testName);
  }

  /**
   * Gets all test comparisons.
   *
   * @return map of test comparisons
   */
  public Map<String, PerformanceComparison> getTestComparisons() {
    return testComparisons;
  }

  /**
   * Gets the number of compared tests.
   *
   * @return the number of compared tests
   */
  public int getComparedTestCount() {
    return comparedTestCount;
  }

  /**
   * Gets the average performance difference between runtimes.
   *
   * @return the average performance difference as a percentage
   */
  public double getAveragePerformanceDifference() {
    return averagePerformanceDifference;
  }

  /**
   * Gets the number of tests with statistically significant differences.
   *
   * @return the number of significant differences
   */
  public int getSignificantDifferences() {
    return significantDifferences;
  }

  /**
   * Checks if runtime1 is generally faster than runtime2.
   *
   * @return true if runtime1 is generally faster
   */
  public boolean isRuntime1Faster() {
    return averagePerformanceDifference < 0.0;
  }

  /**
   * Checks if there are significant performance differences between runtimes.
   *
   * @return true if there are significant differences
   */
  public boolean hasSignificantDifferences() {
    return significantDifferences > 0;
  }

  /**
   * Creates a summary report of the runtime comparison.
   *
   * @return a formatted summary report
   */
  public String createSummaryReport() {
    final StringBuilder report = new StringBuilder();
    report.append(String.format("Runtime Performance Comparison: %s vs %s\n", 
        runtime1.name(), runtime2.name()));
    report.append("=".repeat(50)).append("\n\n");

    report.append(String.format("Compared Tests: %d\n", comparedTestCount));
    report.append(String.format("Average Performance Difference: %.2f%%\n", averagePerformanceDifference * 100));
    report.append(String.format("Statistically Significant Differences: %d\n", significantDifferences));

    if (comparedTestCount > 0) {
      final String fasterRuntime = isRuntime1Faster() ? runtime1.name() : runtime2.name();
      final double absDifference = Math.abs(averagePerformanceDifference * 100);
      report.append(String.format("Generally Faster: %s (by %.2f%%)\n", fasterRuntime, absDifference));
    }

    if (hasSignificantDifferences()) {
      report.append("\nTests with Significant Performance Differences:\n");
      report.append("-".repeat(45)).append("\n");

      testComparisons.entrySet().stream()
          .filter(entry -> entry.getValue().isStatisticallySignificant())
          .forEach(entry -> {
            final String testName = entry.getKey();
            final PerformanceComparison comparison = entry.getValue();
            report.append(String.format("  %s: %.2f%% difference\n", 
                testName, comparison.getPerformanceDifference() * 100));
          });
    }

    return report.toString();
  }

  @Override
  public String toString() {
    return String.format("RuntimePerformanceComparison{%s vs %s, tests=%d, avgDiff=%.2f%%}",
        runtime1.name(), runtime2.name(), comparedTestCount, averagePerformanceDifference * 100);
  }
}