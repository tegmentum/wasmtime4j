package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ComprehensiveCoverageReport;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
  private volatile Map<String, PerformanceAnalyzer.PerformanceComparisonResult> cachedPerformanceResults;
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

  public String getReportId() {
    return reportId;
  }

  public Instant getGeneratedAt() {
    return generatedAt;
  }

  public ComparisonMetadata getMetadata() {
    return metadata;
  }

  public ExecutionSummary getExecutionSummary() {
    return executionSummary;
  }

  public List<TestComparisonResult> getTestResults() {
    return testResults;
  }

  public ComprehensiveCoverageReport getCoverageReport() {
    return coverageReport;
  }

  public PerformanceAnalysisSummary getPerformanceSummary() {
    return performanceSummary;
  }

  public List<BehavioralDiscrepancy> getBehavioralDiscrepancies() {
    return behavioralDiscrepancies;
  }

  public List<RecommendationResult> getRecommendations() {
    return recommendations;
  }

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
          cachedSummary = ComparisonSummary.fromBehavioralResults(
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
          cachedBehavioralResults = testResults.stream()
              .collect(Collectors.toMap(
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
          cachedRecommendationResults = recommendations.stream()
              .collect(Collectors.toMap(
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
          cachedPerformanceResults = testResults.stream()
              .collect(Collectors.toMap(
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
          cachedCoverageResults = testResults.stream()
              .collect(Collectors.toMap(
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

    public Builder metadata(final ComparisonMetadata metadata) {
      this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
      return this;
    }

    public Builder executionSummary(final ExecutionSummary executionSummary) {
      this.executionSummary =
          Objects.requireNonNull(executionSummary, "executionSummary cannot be null");
      return this;
    }

    public Builder testResults(final List<TestComparisonResult> testResults) {
      this.testResults = Objects.requireNonNull(testResults, "testResults cannot be null");
      return this;
    }

    public Builder coverageReport(final ComprehensiveCoverageReport coverageReport) {
      this.coverageReport = Objects.requireNonNull(coverageReport, "coverageReport cannot be null");
      return this;
    }

    public Builder performanceSummary(final PerformanceAnalysisSummary performanceSummary) {
      this.performanceSummary =
          Objects.requireNonNull(performanceSummary, "performanceSummary cannot be null");
      return this;
    }

    public Builder behavioralDiscrepancies(
        final List<BehavioralDiscrepancy> behavioralDiscrepancies) {
      this.behavioralDiscrepancies =
          Objects.requireNonNull(behavioralDiscrepancies, "behavioralDiscrepancies cannot be null");
      return this;
    }

    public Builder recommendations(final List<RecommendationResult> recommendations) {
      this.recommendations =
          Objects.requireNonNull(recommendations, "recommendations cannot be null");
      return this;
    }

    public Builder statistics(final ReportStatistics statistics) {
      this.statistics = Objects.requireNonNull(statistics, "statistics cannot be null");
      return this;
    }

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
    final boolean isCompatible = testResult.getOverallStatus() == TestResultStatus.SUCCESS
        || testResult.getOverallStatus() == TestResultStatus.WARNING;

    // Calculate consistency score based on status
    final double consistencyScore = switch (testResult.getOverallStatus()) {
      case SUCCESS -> 1.0;
      case WARNING -> 0.75;
      case FAILURE -> 0.25;
      case CRITICAL -> 0.0;
    };

    // Determine verdict based on compatibility and score
    final BehavioralVerdict verdict = isCompatible
        ? (consistencyScore >= 0.9 ? BehavioralVerdict.CONSISTENT : BehavioralVerdict.MOSTLY_CONSISTENT)
        : (consistencyScore > 0.0 ? BehavioralVerdict.INCONSISTENT : BehavioralVerdict.INCOMPATIBLE);

    return new BehavioralAnalysisResult(
        testResult.getTestName(),
        isCompatible,
        consistencyScore,
        testResult.getDiscrepancies(),
        verdict
    );
  }
}

/** Metadata about the comparison report and execution environment. */
final class ComparisonMetadata {
  private final String testSuiteName;
  private final String testSuiteVersion;
  private final Set<RuntimeType> runtimeTypes;
  private final Map<String, String> environmentInfo;
  private final String wasmtime4jVersion;

  public ComparisonMetadata(
      final String testSuiteName,
      final String testSuiteVersion,
      final Set<RuntimeType> runtimeTypes,
      final Map<String, String> environmentInfo,
      final String wasmtime4jVersion) {
    this.testSuiteName = Objects.requireNonNull(testSuiteName, "testSuiteName cannot be null");
    this.testSuiteVersion =
        Objects.requireNonNull(testSuiteVersion, "testSuiteVersion cannot be null");
    this.runtimeTypes = Set.copyOf(runtimeTypes);
    this.environmentInfo = Map.copyOf(environmentInfo);
    this.wasmtime4jVersion =
        Objects.requireNonNull(wasmtime4jVersion, "wasmtime4jVersion cannot be null");
  }

  public String getTestSuiteName() {
    return testSuiteName;
  }

  public String getTestSuiteVersion() {
    return testSuiteVersion;
  }

  public Set<RuntimeType> getRuntimeTypes() {
    return runtimeTypes;
  }

  public Map<String, String> getEnvironmentInfo() {
    return environmentInfo;
  }

  public String getWasmtime4jVersion() {
    return wasmtime4jVersion;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComparisonMetadata that = (ComparisonMetadata) obj;
    return Objects.equals(testSuiteName, that.testSuiteName)
        && Objects.equals(testSuiteVersion, that.testSuiteVersion)
        && Objects.equals(runtimeTypes, that.runtimeTypes)
        && Objects.equals(environmentInfo, that.environmentInfo)
        && Objects.equals(wasmtime4jVersion, that.wasmtime4jVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        testSuiteName, testSuiteVersion, runtimeTypes, environmentInfo, wasmtime4jVersion);
  }

  @Override
  public String toString() {
    return "ComparisonMetadata{"
        + "testSuite='"
        + testSuiteName
        + '\''
        + ", version='"
        + testSuiteVersion
        + '\''
        + ", runtimes="
        + runtimeTypes
        + '}';
  }
}

/** Summary of test execution results. */
final class ExecutionSummary {
  private final int totalTests;
  private final int successfulTests;
  private final int failedTests;
  private final int skippedTests;
  private final java.time.Duration totalDuration;
  private final Instant startTime;
  private final Instant endTime;

  public ExecutionSummary(
      final int totalTests,
      final int successfulTests,
      final int failedTests,
      final int skippedTests,
      final java.time.Duration totalDuration,
      final Instant startTime,
      final Instant endTime) {
    this.totalTests = totalTests;
    this.successfulTests = successfulTests;
    this.failedTests = failedTests;
    this.skippedTests = skippedTests;
    this.totalDuration = Objects.requireNonNull(totalDuration, "totalDuration cannot be null");
    this.startTime = Objects.requireNonNull(startTime, "startTime cannot be null");
    this.endTime = Objects.requireNonNull(endTime, "endTime cannot be null");
  }

  public int getTotalTests() {
    return totalTests;
  }

  public int getSuccessfulTests() {
    return successfulTests;
  }

  public int getFailedTests() {
    return failedTests;
  }

  public int getSkippedTests() {
    return skippedTests;
  }

  public java.time.Duration getTotalDuration() {
    return totalDuration;
  }

  public Instant getStartTime() {
    return startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return success rate percentage
   */
  public double getSuccessRate() {
    return totalTests > 0 ? (double) successfulTests / totalTests * 100.0 : 0.0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExecutionSummary that = (ExecutionSummary) obj;
    return totalTests == that.totalTests
        && successfulTests == that.successfulTests
        && failedTests == that.failedTests
        && skippedTests == that.skippedTests
        && Objects.equals(totalDuration, that.totalDuration)
        && Objects.equals(startTime, that.startTime)
        && Objects.equals(endTime, that.endTime);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalTests, successfulTests, failedTests, skippedTests, totalDuration, startTime, endTime);
  }

  @Override
  public String toString() {
    return "ExecutionSummary{"
        + "total="
        + totalTests
        + ", successful="
        + successfulTests
        + ", failed="
        + failedTests
        + ", duration="
        + totalDuration
        + '}';
  }
}

/** Individual test comparison result. */
final class TestComparisonResult {
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

/** Result of test execution on a specific runtime. */
final class TestExecutionResult {
  private final RuntimeType runtime;
  private final boolean successful;
  private final String output;
  private final String errorMessage;
  private final java.time.Duration executionTime;
  private final Map<String, Object> metrics;

  public TestExecutionResult(
      final RuntimeType runtime,
      final boolean successful,
      final String output,
      final String errorMessage,
      final java.time.Duration executionTime,
      final Map<String, Object> metrics) {
    this.runtime = Objects.requireNonNull(runtime, "runtime cannot be null");
    this.successful = successful;
    this.output = output != null ? output : "";
    this.errorMessage = errorMessage != null ? errorMessage : "";
    this.executionTime = Objects.requireNonNull(executionTime, "executionTime cannot be null");
    this.metrics = Map.copyOf(metrics);
  }

  public RuntimeType getRuntime() {
    return runtime;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public String getOutput() {
    return output;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public java.time.Duration getExecutionTime() {
    return executionTime;
  }

  public Map<String, Object> getMetrics() {
    return metrics;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TestExecutionResult that = (TestExecutionResult) obj;
    return successful == that.successful
        && runtime == that.runtime
        && Objects.equals(output, that.output)
        && Objects.equals(errorMessage, that.errorMessage)
        && Objects.equals(executionTime, that.executionTime)
        && Objects.equals(metrics, that.metrics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(runtime, successful, output, errorMessage, executionTime, metrics);
  }

  @Override
  public String toString() {
    return "TestExecutionResult{"
        + "runtime="
        + runtime
        + ", successful="
        + successful
        + ", executionTime="
        + executionTime
        + '}';
  }
}

/** Status levels for test comparison results. */
enum TestResultStatus {
  /** Test passed successfully across all runtimes. */
  SUCCESS,

  /** Test passed but with warnings or minor discrepancies. */
  WARNING,

  /** Test failed but without critical implications. */
  FAILURE,

  /** Test failed with critical issues requiring immediate attention. */
  CRITICAL
}

/** Summary of performance analysis across all tests. */
final class PerformanceAnalysisSummary {
  private final double averagePerformanceVariance;
  private final double maxPerformanceVariance;
  private final Map<RuntimeType, Double> runtimePerformanceScores;
  private final List<String> outlierTests;
  private final Map<String, Double> performanceTrends;

  public PerformanceAnalysisSummary(
      final double averagePerformanceVariance,
      final double maxPerformanceVariance,
      final Map<RuntimeType, Double> runtimePerformanceScores,
      final List<String> outlierTests,
      final Map<String, Double> performanceTrends) {
    this.averagePerformanceVariance = averagePerformanceVariance;
    this.maxPerformanceVariance = maxPerformanceVariance;
    this.runtimePerformanceScores = Map.copyOf(runtimePerformanceScores);
    this.outlierTests = List.copyOf(outlierTests);
    this.performanceTrends = Map.copyOf(performanceTrends);
  }

  public double getAveragePerformanceVariance() {
    return averagePerformanceVariance;
  }

  public double getMaxPerformanceVariance() {
    return maxPerformanceVariance;
  }

  public Map<RuntimeType, Double> getRuntimePerformanceScores() {
    return runtimePerformanceScores;
  }

  public List<String> getOutlierTests() {
    return outlierTests;
  }

  public Map<String, Double> getPerformanceTrends() {
    return performanceTrends;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final PerformanceAnalysisSummary that = (PerformanceAnalysisSummary) obj;
    return Double.compare(that.averagePerformanceVariance, averagePerformanceVariance) == 0
        && Double.compare(that.maxPerformanceVariance, maxPerformanceVariance) == 0
        && Objects.equals(runtimePerformanceScores, that.runtimePerformanceScores)
        && Objects.equals(outlierTests, that.outlierTests)
        && Objects.equals(performanceTrends, that.performanceTrends);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        averagePerformanceVariance,
        maxPerformanceVariance,
        runtimePerformanceScores,
        outlierTests,
        performanceTrends);
  }

  @Override
  public String toString() {
    return "PerformanceAnalysisSummary{"
        + "avgVariance="
        + String.format("%.2f", averagePerformanceVariance)
        + ", maxVariance="
        + String.format("%.2f", maxPerformanceVariance)
        + ", outliers="
        + outlierTests.size()
        + '}';
  }
}

/** Statistical information about the comparison report. */
final class ReportStatistics {
  private final int totalDataPoints;
  private final long reportSizeBytes;
  private final Map<String, Integer> categoryBreakdown;
  private final double dataQualityScore;

  public ReportStatistics(
      final int totalDataPoints,
      final long reportSizeBytes,
      final Map<String, Integer> categoryBreakdown,
      final double dataQualityScore) {
    this.totalDataPoints = totalDataPoints;
    this.reportSizeBytes = reportSizeBytes;
    this.categoryBreakdown = Map.copyOf(categoryBreakdown);
    this.dataQualityScore = dataQualityScore;
  }

  public int getTotalDataPoints() {
    return totalDataPoints;
  }

  public long getReportSizeBytes() {
    return reportSizeBytes;
  }

  public Map<String, Integer> getCategoryBreakdown() {
    return categoryBreakdown;
  }

  public double getDataQualityScore() {
    return dataQualityScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ReportStatistics that = (ReportStatistics) obj;
    return totalDataPoints == that.totalDataPoints
        && reportSizeBytes == that.reportSizeBytes
        && Double.compare(that.dataQualityScore, dataQualityScore) == 0
        && Objects.equals(categoryBreakdown, that.categoryBreakdown);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalDataPoints, reportSizeBytes, categoryBreakdown, dataQualityScore);
  }

  @Override
  public String toString() {
    return "ReportStatistics{"
        + "dataPoints="
        + totalDataPoints
        + ", sizeBytes="
        + reportSizeBytes
        + ", qualityScore="
        + String.format("%.2f", dataQualityScore)
        + '}';
  }
}
