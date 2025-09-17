package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ComprehensiveCoverageReport;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.InsightAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.InsightConfidenceMetrics;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Comprehensive comparison report containing all analysis results, performance data, behavioral
 * discrepancies, coverage analysis, and recommendations for presentation in various reporting
 * formats including interactive HTML dashboards.
 *
 * @since 1.0.0
 */
public final class ComparisonReport {
  private final String reportId;
  private final Instant generatedAt;
  private final ComparisonMetadata metadata;
  private final ExecutionSummary executionSummary;
  private final List<TestComparisonResult> testResults;
  private final ComprehensiveCoverageReport coverageReport;
  private final PerformanceAnalysisSummary performanceSummary;
  private final List<BehavioralDiscrepancy> behavioralDiscrepancies;
  private final List<RecommendationResult> recommendations;
  private final ReportStatistics statistics;

  // Cached computed values
  private volatile ComparisonSummary cachedSummary;
  private volatile Map<String, BehavioralAnalysisResult> cachedBehavioralResults;
  private volatile Map<String, RecommendationResult> cachedRecommendationResults;
  private volatile Map<String, PerformanceAnalyzer.PerformanceComparisonResult>
      cachedPerformanceResults;
  private volatile Map<String, CoverageAnalysisResult> cachedCoverageResults;

  private ComparisonReport(final Builder builder) {
    this.reportId = Objects.requireNonNull(builder.reportId, "reportId cannot be null");
    this.generatedAt = Instant.now();
    this.metadata = Objects.requireNonNull(builder.metadata, "metadata cannot be null");
    this.executionSummary =
        Objects.requireNonNull(builder.executionSummary, "executionSummary cannot be null");
    this.testResults = List.copyOf(builder.testResults);
    this.coverageReport =
        Objects.requireNonNull(builder.coverageReport, "coverageReport cannot be null");
    this.performanceSummary =
        Objects.requireNonNull(builder.performanceSummary, "performanceSummary cannot be null");
    this.behavioralDiscrepancies = List.copyOf(builder.behavioralDiscrepancies);
    this.recommendations = List.copyOf(builder.recommendations);
    this.statistics = Objects.requireNonNull(builder.statistics, "statistics cannot be null");
  }

  /**
   * Gets the unique identifier for this report.
   *
   * @return report ID
   */
  public String getReportId() {
    return reportId;
  }

  /**
   * Gets the timestamp when this report was generated.
   *
   * @return generation timestamp
   */
  public Instant getGeneratedAt() {
    return generatedAt;
  }

  /**
   * Gets the metadata for this comparison report.
   *
   * @return comparison metadata
   */
  public ComparisonMetadata getMetadata() {
    return metadata;
  }

  /**
   * Gets the execution summary for this comparison.
   *
   * @return execution summary
   */
  public ExecutionSummary getExecutionSummary() {
    return executionSummary;
  }

  /**
   * Gets the test comparison results.
   *
   * @return list of test comparison results
   */
  public List<TestComparisonResult> getTestResults() {
    return testResults;
  }

  /**
   * Gets the comprehensive coverage report.
   *
   * @return coverage report
   */
  public ComprehensiveCoverageReport getCoverageReport() {
    return coverageReport;
  }

  /**
   * Gets the performance analysis summary.
   *
   * @return performance summary
   */
  public PerformanceAnalysisSummary getPerformanceSummary() {
    return performanceSummary;
  }

  /**
   * Gets the list of behavioral discrepancies found in the comparison.
   *
   * @return list of behavioral discrepancies
   */
  public List<BehavioralDiscrepancy> getBehavioralDiscrepancies() {
    return behavioralDiscrepancies;
  }

  /**
   * Gets the recommendations based on the comparison analysis.
   *
   * @return list of recommendation results
   */
  public List<RecommendationResult> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets the statistical summary of the report.
   *
   * @return report statistics
   */
  public ReportStatistics getStatistics() {
    return statistics;
  }

  /**
   * Gets the comparison summary with key metrics and overall verdict.
   *
   * @return comparison summary
   */
  public ComparisonSummary getSummary() {
    if (cachedSummary == null) {
      synchronized (this) {
        if (cachedSummary == null) {
          cachedSummary =
              ComparisonSummary.fromBehavioralResults(
                  getBehavioralResults(), executionSummary.getTotalDuration());
        }
      }
    }
    return cachedSummary;
  }

  /**
   * Gets behavioral analysis results mapped by test name.
   *
   * @return map of behavioral results
   */
  public Map<String, BehavioralAnalysisResult> getBehavioralResults() {
    if (cachedBehavioralResults == null) {
      synchronized (this) {
        if (cachedBehavioralResults == null) {
          cachedBehavioralResults =
              testResults.stream()
                  .collect(
                      Collectors.toMap(
                          TestComparisonResult::getTestName,
                          result -> createBehavioralAnalysisResult(result)));
        }
      }
    }
    return cachedBehavioralResults;
  }

  /**
   * Gets recommendation results mapped by test name.
   *
   * @return map of recommendation results
   */
  public Map<String, RecommendationResult> getRecommendationResults() {
    if (cachedRecommendationResults == null) {
      synchronized (this) {
        if (cachedRecommendationResults == null) {
          cachedRecommendationResults =
              recommendations.stream()
                  .collect(
                      Collectors.toMap(
                          result -> result.getTestName() != null ? result.getTestName() : "unknown",
                          result -> result));
        }
      }
    }
    return cachedRecommendationResults;
  }

  /**
   * Gets performance analysis results mapped by test name.
   *
   * @return map of performance results
   */
  public Map<String, PerformanceAnalyzer.PerformanceComparisonResult> getPerformanceResults() {
    if (cachedPerformanceResults == null) {
      synchronized (this) {
        if (cachedPerformanceResults == null) {
          cachedPerformanceResults =
              testResults.stream()
                  .collect(
                      Collectors.toMap(
                          TestComparisonResult::getTestName,
                          TestComparisonResult::getPerformanceComparison));
        }
      }
    }
    return cachedPerformanceResults;
  }

  /**
   * Gets coverage analysis results mapped by test name.
   *
   * @return map of coverage results
   */
  public Map<String, CoverageAnalysisResult> getCoverageResults() {
    if (cachedCoverageResults == null) {
      synchronized (this) {
        if (cachedCoverageResults == null) {
          cachedCoverageResults =
              testResults.stream()
                  .collect(
                      Collectors.toMap(
                          TestComparisonResult::getTestName,
                          TestComparisonResult::getCoverageAnalysis));
        }
      }
    }
    return cachedCoverageResults;
  }

  /**
   * Checks if the overall comparison was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    final ComparisonSummary summary = getSummary();
    return summary.getOverallVerdict() == OverallVerdict.PASSED
        || summary.getOverallVerdict() == OverallVerdict.PASSED_WITH_WARNINGS;
  }

  /**
   * Gets the end time of the comparison analysis.
   *
   * @return end time
   */
  public Instant getEndTime() {
    return executionSummary.getEndTime();
  }

  /**
   * Gets the total duration of the comparison analysis.
   *
   * @return total duration
   */
  public java.time.Duration getTotalDuration() {
    return executionSummary.getTotalDuration();
  }

  /**
   * Gets the configuration used for this comparison report.
   *
   * @return report configuration
   */
  public ReportConfiguration getConfiguration() {
    // Return a default configuration for now - this would normally be passed in
    return ReportConfiguration.defaultConfiguration();
  }

  /**
   * Gets high-priority test results that require immediate attention.
   *
   * @return list of test results with critical issues
   */
  public List<TestComparisonResult> getCriticalTestResults() {
    return testResults.stream().filter(TestComparisonResult::hasCriticalIssues).toList();
  }

  /**
   * Gets behavioral discrepancies grouped by severity.
   *
   * @return map of discrepancies grouped by severity
   */
  public Map<String, List<BehavioralDiscrepancy>> getDiscrepanciesBySeverity() {
    return behavioralDiscrepancies.stream()
        .collect(
            java.util.stream.Collectors.groupingBy(
                discrepancy -> discrepancy.getSeverity().toString()));
  }

  /**
   * Generates an executive summary of the comparison report.
   *
   * @return formatted executive summary
   */
  public String getExecutiveSummary() {
    return String.format(
        "Comparison Report Executive Summary%n"
            + "===================================%n"
            + "Report ID: %s%n"
            + "Generated: %s%n"
            + "Test Suite: %s%n"
            + "Runtimes Compared: %s%n%n"
            + "Execution Summary:%n"
            + "  Total Tests: %d%n"
            + "  Successful: %d%n"
            + "  Failed: %d%n"
            + "  Duration: %s%n%n"
            + "Critical Issues:%n"
            + "  Behavioral Discrepancies: %d%n"
            + "  Critical Test Failures: %d%n"
            + "  High-Priority Recommendations: %d%n%n"
            + "Coverage Analysis:%n"
            + "  Overall Coverage: %.1f%%%n"
            + "  Uncovered Features: %d%n%n"
            + "Performance Summary:%n"
            + "  Performance Variance: %.2fx%n"
            + "  Outlier Tests: %d",
        reportId,
        generatedAt,
        metadata.getTestSuiteName(),
        metadata.getRuntimeTypes(),
        executionSummary.getTotalTests(),
        executionSummary.getSuccessfulTests(),
        executionSummary.getFailedTests(),
        executionSummary.getTotalDuration(),
        behavioralDiscrepancies.size(),
        getCriticalTestResults().size(),
        recommendations.stream().mapToLong(r -> r.getHighPriorityRecommendations().size()).sum(),
        coverageReport.getOverallCoverageScore(),
        coverageReport.getUncoveredFeatures().size(),
        performanceSummary.getMaxPerformanceVariance(),
        performanceSummary.getOutlierTests().size());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComparisonReport that = (ComparisonReport) obj;
    return Objects.equals(reportId, that.reportId)
        && Objects.equals(metadata, that.metadata)
        && Objects.equals(executionSummary, that.executionSummary)
        && Objects.equals(testResults, that.testResults)
        && Objects.equals(coverageReport, that.coverageReport)
        && Objects.equals(performanceSummary, that.performanceSummary)
        && Objects.equals(behavioralDiscrepancies, that.behavioralDiscrepancies)
        && Objects.equals(recommendations, that.recommendations)
        && Objects.equals(statistics, that.statistics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        reportId,
        metadata,
        executionSummary,
        testResults,
        coverageReport,
        performanceSummary,
        behavioralDiscrepancies,
        recommendations,
        statistics);
  }

  @Override
  public String toString() {
    return "ComparisonReport{"
        + "reportId='"
        + reportId
        + '\''
        + ", tests="
        + testResults.size()
        + ", discrepancies="
        + behavioralDiscrepancies.size()
        + ", recommendations="
        + recommendations.size()
        + ", generatedAt="
        + generatedAt
        + '}';
  }

  /**
   * Gets tests that have performance issues.
   *
   * @return list of test names with performance issues
   */
  public List<String> getTestsWithPerformanceIssues() {
    return testResults.stream()
        .filter(
            test ->
                test.getOverallStatus() == TestResultStatus.WARNING
                    || test.getOverallStatus() == TestResultStatus.CRITICAL)
        .map(test -> test.getTestName())
        .toList();
  }

  /**
   * Gets tests that have coverage gaps.
   *
   * @return list of test names with coverage gaps
   */
  public List<String> getTestsWithCoverageGaps() {
    return testResults.stream()
        .filter(
            test ->
                test.getOverallStatus() == TestResultStatus.FAILURE
                    || test.getOverallStatus() == TestResultStatus.CRITICAL)
        .map(test -> test.getTestName())
        .toList();
  }

  /**
   * Gets the total number of tests in this report.
   *
   * @return test count
   */
  public int getTestCount() {
    return testResults.size();
  }

  /**
   * Gets all test names in this report.
   *
   * @return list of test names
   */
  public List<String> getTestNames() {
    return testResults.stream().map(TestComparisonResult::getTestName).toList();
  }

  /**
   * Gets insights analysis result for this report.
   *
   * @return insight analysis result
   */
  public InsightAnalysisResult getInsights() {
    // Create a simple insights result based on available data
    return new InsightAnalysisResult.Builder(reportId)
        .confidenceMetrics(new InsightConfidenceMetrics.Builder().build())
        .build();
  }

  /**
   * Gets the test suite name for this report.
   *
   * @return test suite name
   */
  public String getTestSuiteName() {
    return metadata.getTestSuite();
  }

  /**
   * Gets global recommendations that apply across all tests.
   *
   * @return list of global recommendations
   */
  public List<RecommendationResult> getGlobalRecommendations() {
    return recommendations;
  }

  /**
   * Gets the report generation time.
   *
   * @return generation time as instant
   */
  public Instant getGenerationTime() {
    return generatedAt;
  }

  /**
   * Gets tests that have behavioral issues.
   *
   * @return list of test names with behavioral issues
   */
  public List<String> getTestsWithBehavioralIssues() {
    return behavioralDiscrepancies.stream()
        .map(discrepancy -> discrepancy.getTestName())
        .distinct()
        .toList();
  }

  /** Builder for ComparisonReport. */
  public static final class Builder {
    private final String reportId;
    private ComparisonMetadata metadata;
    private ExecutionSummary executionSummary;
    private List<TestComparisonResult> testResults = List.of();
    private ComprehensiveCoverageReport coverageReport;
    private PerformanceAnalysisSummary performanceSummary;
    private List<BehavioralDiscrepancy> behavioralDiscrepancies = List.of();
    private List<RecommendationResult> recommendations = List.of();
    private ReportStatistics statistics;

    public Builder(final String reportId) {
      this.reportId = Objects.requireNonNull(reportId, "reportId cannot be null");
    }

    /**
     * Sets the metadata for the comparison report.
     *
     * @param metadata comparison metadata
     * @return this builder
     */
    public Builder metadata(final ComparisonMetadata metadata) {
      this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
      return this;
    }

    /**
     * Sets the execution summary for the comparison report.
     *
     * @param executionSummary execution summary
     * @return this builder
     */
    public Builder executionSummary(final ExecutionSummary executionSummary) {
      this.executionSummary =
          Objects.requireNonNull(executionSummary, "executionSummary cannot be null");
      return this;
    }

    /**
     * Sets the test results for the comparison report.
     *
     * @param testResults list of test comparison results
     * @return this builder
     */
    public Builder testResults(final List<TestComparisonResult> testResults) {
      this.testResults = Objects.requireNonNull(testResults, "testResults cannot be null");
      return this;
    }

    /**
     * Sets the coverage report for the comparison.
     *
     * @param coverageReport comprehensive coverage report
     * @return this builder
     */
    public Builder coverageReport(final ComprehensiveCoverageReport coverageReport) {
      this.coverageReport = Objects.requireNonNull(coverageReport, "coverageReport cannot be null");
      return this;
    }

    /**
     * Sets the performance analysis summary.
     *
     * @param performanceSummary performance analysis summary
     * @return this builder
     */
    public Builder performanceSummary(final PerformanceAnalysisSummary performanceSummary) {
      this.performanceSummary =
          Objects.requireNonNull(performanceSummary, "performanceSummary cannot be null");
      return this;
    }

    /**
     * Sets the behavioral discrepancies found during comparison.
     *
     * @param behavioralDiscrepancies list of behavioral discrepancies
     * @return this builder
     */
    public Builder behavioralDiscrepancies(
        final List<BehavioralDiscrepancy> behavioralDiscrepancies) {
      this.behavioralDiscrepancies =
          Objects.requireNonNull(behavioralDiscrepancies, "behavioralDiscrepancies cannot be null");
      return this;
    }

    /**
     * Sets the recommendations based on analysis results.
     *
     * @param recommendations list of recommendation results
     * @return this builder
     */
    public Builder recommendations(final List<RecommendationResult> recommendations) {
      this.recommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    /**
     * Sets the statistical summary for the report.
     *
     * @param statistics report statistics
     * @return this builder
     */
    public Builder statistics(final ReportStatistics statistics) {
      this.statistics = Objects.requireNonNull(statistics, "statistics cannot be null");
      return this;
    }

    /**
     * Builds and returns a new ComparisonReport instance.
     *
     * @return the completed comparison report
     * @throws IllegalStateException if required fields are not set
     */
    public ComparisonReport build() {
      if (metadata == null) {
        throw new IllegalStateException("metadata must be set");
      }
      if (executionSummary == null) {
        throw new IllegalStateException("executionSummary must be set");
      }
      if (coverageReport == null) {
        throw new IllegalStateException("coverageReport must be set");
      }
      if (performanceSummary == null) {
        throw new IllegalStateException("performanceSummary must be set");
      }
      if (statistics == null) {
        throw new IllegalStateException("statistics must be set");
      }
      return new ComparisonReport(this);
    }
  }

  private BehavioralAnalysisResult createBehavioralAnalysisResult(
      final TestComparisonResult testResult) {
    // Create a simple behavioral analysis result from test comparison result
    final boolean isCompatible =
        testResult.getOverallStatus() == TestResultStatus.SUCCESS
            || testResult.getOverallStatus() == TestResultStatus.WARNING;

    // Calculate consistency score based on status
    final double consistencyScore =
        switch (testResult.getOverallStatus()) {
          case SUCCESS -> 1.0;
          case WARNING -> 0.75;
          case FAILURE -> 0.25;
          case CRITICAL -> 0.0;
        };

    // Determine verdict based on compatibility and score
    final BehavioralVerdict verdict =
        isCompatible
            ? (consistencyScore >= 0.9
                ? BehavioralVerdict.CONSISTENT
                : BehavioralVerdict.MOSTLY_CONSISTENT)
            : (consistencyScore > 0.0
                ? BehavioralVerdict.INCONSISTENT
                : BehavioralVerdict.INCOMPATIBLE);

    // TODO: Fix ExecutionPattern access from different package
    return null; // new BehavioralAnalysisResult.Builder(testResult.getTestName())
    // .discrepancies(testResult.getDiscrepancies())
    // .consistencyScore(consistencyScore)
    // .verdict(verdict)
    // .executionPattern(createDefaultExecutionPattern())
    // .build();
  }
}
