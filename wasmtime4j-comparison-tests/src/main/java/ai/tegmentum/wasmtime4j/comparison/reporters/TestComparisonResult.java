package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Individual test comparison result.
 *
 * @since 1.0.0
 */
public final class TestComparisonResult {
  private final String testName;
  private final Map<RuntimeType, TestExecutionResult> runtimeResults;
  private final CoverageAnalysisResult coverageAnalysis;
  private final PerformanceAnalyzer.PerformanceComparisonResult performanceComparison;
  private final List<BehavioralDiscrepancy> discrepancies;
  private final TestResultStatus overallStatus;

  public TestComparisonResult(
      final String testName,
      final Map<RuntimeType, TestExecutionResult> runtimeResults,
      final CoverageAnalysisResult coverageAnalysis,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceComparison,
      final List<BehavioralDiscrepancy> discrepancies,
      final TestResultStatus overallStatus) {
    this.testName = Objects.requireNonNull(testName, "testName cannot be null");
    this.runtimeResults = Map.copyOf(runtimeResults);
    this.coverageAnalysis =
        Objects.requireNonNull(coverageAnalysis, "coverageAnalysis cannot be null");
    this.performanceComparison =
        Objects.requireNonNull(performanceComparison, "performanceComparison cannot be null");
    this.discrepancies = List.copyOf(discrepancies);
    this.overallStatus = Objects.requireNonNull(overallStatus, "overallStatus cannot be null");
  }

  public String getTestName() {
    return testName;
  }

  public Map<RuntimeType, TestExecutionResult> getRuntimeResults() {
    return runtimeResults;
  }

  public CoverageAnalysisResult getCoverageAnalysis() {
    return coverageAnalysis;
  }

  public PerformanceAnalyzer.PerformanceComparisonResult getPerformanceComparison() {
    return performanceComparison;
  }

  public List<BehavioralDiscrepancy> getDiscrepancies() {
    return discrepancies;
  }

  public TestResultStatus getOverallStatus() {
    return overallStatus;
  }

  /**
   * Checks if this test has critical issues requiring immediate attention.
   *
   * @return true if the test has critical issues
   */
  public boolean hasCriticalIssues() {
    return overallStatus == TestResultStatus.CRITICAL
        || discrepancies.stream().anyMatch(BehavioralDiscrepancy::isCritical);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TestComparisonResult that = (TestComparisonResult) obj;
    return Objects.equals(testName, that.testName)
        && Objects.equals(runtimeResults, that.runtimeResults)
        && Objects.equals(coverageAnalysis, that.coverageAnalysis)
        && Objects.equals(performanceComparison, that.performanceComparison)
        && Objects.equals(discrepancies, that.discrepancies)
        && overallStatus == that.overallStatus;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testName,
        runtimeResults,
        coverageAnalysis,
        performanceComparison,
        discrepancies,
        overallStatus);
  }

  @Override
  public String toString() {
    return "TestComparisonResult{"
        + "testName='"
        + testName
        + '\''
        + ", status="
        + overallStatus
        + ", discrepancies="
        + discrepancies.size()
        + '}';
  }
}
