package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import java.util.Objects;

/**
 * High-level summary of comparison analysis results, providing key metrics and verdicts for
 * quick assessment of overall test compatibility and performance.
 *
 * @since 1.0.0
 */
public final class ComparisonSummary {
  private final int totalTests;
  private final int passedTests;
  private final int failedTests;
  private final int skippedTests;
  private final int highPriorityIssueCount;
  private final int mediumPriorityIssueCount;
  private final int lowPriorityIssueCount;
  private final double averageCompatibilityScore;
  private final OverallVerdict overallVerdict;
  private final java.time.Duration totalDuration;

  /**
   * Creates a new ComparisonSummary with the specified metrics.
   *
   * @param totalTests total number of tests executed
   * @param passedTests number of tests that passed
   * @param failedTests number of tests that failed
   * @param skippedTests number of tests that were skipped
   * @param highPriorityIssueCount number of high priority issues found
   * @param mediumPriorityIssueCount number of medium priority issues found
   * @param lowPriorityIssueCount number of low priority issues found
   * @param averageCompatibilityScore average compatibility score across all tests
   * @param overallVerdict overall verdict of the comparison
   * @param totalDuration total time taken for the comparison
   */
  public ComparisonSummary(
      final int totalTests,
      final int passedTests,
      final int failedTests,
      final int skippedTests,
      final int highPriorityIssueCount,
      final int mediumPriorityIssueCount,
      final int lowPriorityIssueCount,
      final double averageCompatibilityScore,
      final OverallVerdict overallVerdict,
      final java.time.Duration totalDuration) {
    this.totalTests = totalTests;
    this.passedTests = passedTests;
    this.failedTests = failedTests;
    this.skippedTests = skippedTests;
    this.highPriorityIssueCount = highPriorityIssueCount;
    this.mediumPriorityIssueCount = mediumPriorityIssueCount;
    this.lowPriorityIssueCount = lowPriorityIssueCount;
    this.averageCompatibilityScore = averageCompatibilityScore;
    this.overallVerdict = Objects.requireNonNull(overallVerdict, "overallVerdict cannot be null");
    this.totalDuration = Objects.requireNonNull(totalDuration, "totalDuration cannot be null");
  }

  /**
   * Gets the total number of tests executed.
   *
   * @return total test count
   */
  public int getTotalTests() {
    return totalTests;
  }

  /**
   * Gets the number of tests that passed.
   *
   * @return passed test count
   */
  public int getPassedTests() {
    return passedTests;
  }

  /**
   * Gets the number of tests that failed.
   *
   * @return failed test count
   */
  public int getFailedTests() {
    return failedTests;
  }

  /**
   * Gets the number of tests that were skipped.
   *
   * @return skipped test count
   */
  public int getSkippedTests() {
    return skippedTests;
  }

  /**
   * Gets the number of high priority issues found.
   *
   * @return high priority issue count
   */
  public int getHighPriorityIssueCount() {
    return highPriorityIssueCount;
  }

  /**
   * Gets the number of medium priority issues found.
   *
   * @return medium priority issue count
   */
  public int getMediumPriorityIssueCount() {
    return mediumPriorityIssueCount;
  }

  /**
   * Gets the number of low priority issues found.
   *
   * @return low priority issue count
   */
  public int getLowPriorityIssueCount() {
    return lowPriorityIssueCount;
  }

  /**
   * Gets the average compatibility score across all tests.
   *
   * @return average compatibility score (0.0 to 1.0)
   */
  public double getAverageCompatibilityScore() {
    return averageCompatibilityScore;
  }

  /**
   * Gets the overall verdict of the comparison analysis.
   *
   * @return overall verdict
   */
  public OverallVerdict getOverallVerdict() {
    return overallVerdict;
  }

  /**
   * Gets the total duration of the comparison analysis.
   *
   * @return total duration
   */
  public java.time.Duration getTotalDuration() {
    return totalDuration;
  }

  /**
   * Gets the success rate as a percentage.
   *
   * @return success rate (0.0 to 100.0)
   */
  public double getSuccessRate() {
    return totalTests > 0 ? ((double) passedTests / totalTests) * 100.0 : 0.0;
  }

  /**
   * Checks if there are any critical issues that require immediate attention.
   *
   * @return true if critical issues exist
   */
  public boolean hasCriticalIssues() {
    return highPriorityIssueCount > 0 || overallVerdict == OverallVerdict.FAILED
        || overallVerdict == OverallVerdict.FAILED_WITH_ISSUES;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ComparisonSummary that = (ComparisonSummary) obj;
    return totalTests == that.totalTests
        && passedTests == that.passedTests
        && failedTests == that.failedTests
        && skippedTests == that.skippedTests
        && highPriorityIssueCount == that.highPriorityIssueCount
        && mediumPriorityIssueCount == that.mediumPriorityIssueCount
        && lowPriorityIssueCount == that.lowPriorityIssueCount
        && Double.compare(that.averageCompatibilityScore, averageCompatibilityScore) == 0
        && overallVerdict == that.overallVerdict
        && Objects.equals(totalDuration, that.totalDuration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        totalTests,
        passedTests,
        failedTests,
        skippedTests,
        highPriorityIssueCount,
        mediumPriorityIssueCount,
        lowPriorityIssueCount,
        averageCompatibilityScore,
        overallVerdict,
        totalDuration);
  }

  @Override
  public String toString() {
    return "ComparisonSummary{"
        + "totalTests=" + totalTests
        + ", passedTests=" + passedTests
        + ", failedTests=" + failedTests
        + ", averageCompatibilityScore=" + String.format("%.2f", averageCompatibilityScore)
        + ", overallVerdict=" + overallVerdict
        + ", duration=" + totalDuration
        + '}';
  }

  /**
   * Creates a ComparisonSummary from detailed behavioral analysis results.
   *
   * @param behavioralResults map of test results
   * @param totalDuration total execution duration
   * @return computed summary
   */
  public static ComparisonSummary fromBehavioralResults(
      final java.util.Map<String, BehavioralAnalysisResult> behavioralResults,
      final java.time.Duration totalDuration) {
    Objects.requireNonNull(behavioralResults, "behavioralResults cannot be null");
    Objects.requireNonNull(totalDuration, "totalDuration cannot be null");

    final int totalTests = behavioralResults.size();
    int passedTests = 0;
    int failedTests = 0;
    double totalScore = 0.0;
    int highPriorityIssues = 0;
    int mediumPriorityIssues = 0;
    int lowPriorityIssues = 0;

    for (final BehavioralAnalysisResult result : behavioralResults.values()) {
      if (result.isCompatible()) {
        passedTests++;
      } else {
        failedTests++;
      }

      totalScore += result.getConsistencyScore();

      // Count issues by severity (simplified categorization)
      final int criticalDiscrepancies = result.getCriticalDiscrepancyCount();
      if (criticalDiscrepancies > 0) {
        highPriorityIssues += criticalDiscrepancies;
      }

      // Additional discrepancies classified as medium/low priority
      final int totalDiscrepancies = result.getDiscrepancies().size();
      final int remainingDiscrepancies = totalDiscrepancies - criticalDiscrepancies;
      if (remainingDiscrepancies > 0) {
        mediumPriorityIssues += remainingDiscrepancies / 2;
        lowPriorityIssues += remainingDiscrepancies - (remainingDiscrepancies / 2);
      }
    }

    final double averageScore = totalTests > 0 ? totalScore / totalTests : 0.0;
    final OverallVerdict verdict = determineOverallVerdict(
        passedTests, failedTests, highPriorityIssues, mediumPriorityIssues, averageScore);

    return new ComparisonSummary(
        totalTests,
        passedTests,
        failedTests,
        0, // skipped tests - not tracked in current implementation
        highPriorityIssues,
        mediumPriorityIssues,
        lowPriorityIssues,
        averageScore,
        verdict,
        totalDuration);
  }

  private static OverallVerdict determineOverallVerdict(
      final int passedTests,
      final int failedTests,
      final int highPriorityIssues,
      final int mediumPriorityIssues,
      final double averageScore) {
    if (failedTests > 0 || highPriorityIssues > 0) {
      return averageScore > 0.5 ? OverallVerdict.FAILED_WITH_ISSUES : OverallVerdict.FAILED;
    } else if (mediumPriorityIssues > 0 || averageScore < 0.9) {
      return OverallVerdict.PASSED_WITH_WARNINGS;
    } else {
      return OverallVerdict.PASSED;
    }
  }
}

/**
 * Overall verdict levels for comparison analysis results.
 */
enum OverallVerdict {
  /** All tests passed without any significant issues. */
  PASSED,

  /** Tests passed but with warnings or minor issues that should be addressed. */
  PASSED_WITH_WARNINGS,

  /** Some tests failed but the overall system is still functional with known issues. */
  FAILED_WITH_ISSUES,

  /** Critical failures or incompatibilities that prevent proper operation. */
  FAILED
}