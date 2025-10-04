package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.webassembly.WasmTestSuiteLoader;
import java.util.Map;

/**
 * Coverage analysis for Wasmtime test suite execution.
 *
 * @since 1.0.0
 */
public final class WasmtimeTestSuiteCoverage {
  private final int executedTests;
  private final double coveragePercentage;
  private final Map<WasmTestSuiteLoader.TestSuiteType, Integer> testSuiteDistribution;

  /**
   * Creates a new Wasmtime test suite coverage instance.
   *
   * @param executedTests number of tests executed
   * @param coveragePercentage coverage percentage achieved
   * @param testSuiteDistribution distribution of tests across suite types
   */
  public WasmtimeTestSuiteCoverage(
      final int executedTests,
      final double coveragePercentage,
      final Map<WasmTestSuiteLoader.TestSuiteType, Integer> testSuiteDistribution) {
    this.executedTests = executedTests;
    this.coveragePercentage = coveragePercentage;
    this.testSuiteDistribution = Map.copyOf(testSuiteDistribution);
  }

  public int getExecutedTests() {
    return executedTests;
  }

  public double getCoveragePercentage() {
    return coveragePercentage;
  }

  public Map<WasmTestSuiteLoader.TestSuiteType, Integer> getTestSuiteDistribution() {
    return testSuiteDistribution;
  }

  public boolean meets95PercentTarget() {
    return coveragePercentage >= 95.0;
  }
}
