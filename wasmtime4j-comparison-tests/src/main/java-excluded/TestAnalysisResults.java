package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.util.Objects;

/**
 * Container for all analysis results for a single test.
 *
 * @since 1.0.0
 */
public final class TestAnalysisResults {
  private final BehavioralAnalysisResult behavioralResults;
  private final PerformanceAnalyzer.PerformanceComparisonResult performanceResults;
  private final CoverageAnalysisResult coverageResults;

  /**
   * Creates new test analysis results.
   *
   * @param behavioralResults the behavioral analysis results
   * @param performanceResults the performance comparison results
   * @param coverageResults the coverage analysis results
   */
  public TestAnalysisResults(
      final BehavioralAnalysisResult behavioralResults,
      final PerformanceAnalyzer.PerformanceComparisonResult performanceResults,
      final CoverageAnalysisResult coverageResults) {
    this.behavioralResults =
        Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    this.performanceResults =
        Objects.requireNonNull(performanceResults, "performanceResults cannot be null");
    this.coverageResults =
        Objects.requireNonNull(coverageResults, "coverageResults cannot be null");
  }

  /**
   * Gets the behavioral results.
   *
   * @return the behavioral results
   */
  public BehavioralAnalysisResult getBehavioralResults() {
    return behavioralResults;
  }

  /**
   * Gets the performance results.
   *
   * @return the performance results
   */
  public PerformanceAnalyzer.PerformanceComparisonResult getPerformanceResults() {
    return performanceResults;
  }

  /**
   * Gets the coverage results.
   *
   * @return the coverage results
   */
  public CoverageAnalysisResult getCoverageResults() {
    return coverageResults;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TestAnalysisResults that = (TestAnalysisResults) obj;
    return Objects.equals(behavioralResults, that.behavioralResults)
        && Objects.equals(performanceResults, that.performanceResults)
        && Objects.equals(coverageResults, that.coverageResults);
  }

  @Override
  public int hashCode() {
    return Objects.hash(behavioralResults, performanceResults, coverageResults);
  }

  @Override
  public String toString() {
    return "TestAnalysisResults{"
        + "behavioral="
        + behavioralResults
        + ", performance="
        + performanceResults
        + ", coverage="
        + coverageResults
        + '}';
  }
}
