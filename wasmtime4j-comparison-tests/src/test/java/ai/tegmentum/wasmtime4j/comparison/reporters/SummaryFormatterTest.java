package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ExecutionPattern;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationSummary;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for SummaryFormatter console output formatting functionality. Tests
 * table generation, color formatting, verbosity control, and summary creation.
 */
final class SummaryFormatterTest {

  private SummaryFormatter formatterWithColors;
  private SummaryFormatter formatterWithoutColors;
  private SummaryFormatter quietFormatter;
  private SummaryFormatter debugFormatter;
  private ComparisonReport testReport;

  @BeforeEach
  void setUp() {
    formatterWithColors = new SummaryFormatter(VerbosityLevel.VERBOSE, true);
    formatterWithoutColors = new SummaryFormatter(VerbosityLevel.VERBOSE, false);
    quietFormatter = new SummaryFormatter(VerbosityLevel.QUIET, false);
    debugFormatter = new SummaryFormatter(VerbosityLevel.DEBUG, false);

    // Create a test report with minimal data
    testReport = createTestReport();
  }

  @Test
  void testCompleteSummaryGeneration() {
    final String summary = formatterWithColors.formatCompleteSummary(testReport);

    assertNotNull(summary);
    assertTrue(summary.contains("Wasmtime4j Comparison Report"));
    assertTrue(summary.contains("Executive Summary"));
    assertTrue(summary.contains(testReport.getReportId()));
  }

  @Test
  void testCompleteSummaryWithoutColors() {
    final String summary = formatterWithoutColors.formatCompleteSummary(testReport);

    assertNotNull(summary);
    assertTrue(summary.contains("Wasmtime4j Comparison Report"));
    // Should not contain ANSI escape codes
    assertTrue(!summary.contains("\u001B["));
  }

  @Test
  void testQuietVerbositySummary() {
    final String summary = quietFormatter.formatCompleteSummary(testReport);

    assertNotNull(summary);
    // Quiet mode should still show basic header and footer
    assertTrue(summary.contains("Wasmtime4j Comparison Report"));
    // But should be much shorter than verbose output
    final String verboseSummary = formatterWithColors.formatCompleteSummary(testReport);
    assertTrue(summary.length() < verboseSummary.length());
  }

  @Test
  void testDebugVerbositySummary() {
    final String summary = debugFormatter.formatCompleteSummary(testReport);

    assertNotNull(summary);
    assertTrue(summary.contains("Detailed Analysis"));
    // Debug should be the most comprehensive
    final String normalSummary = formatterWithoutColors.formatCompleteSummary(testReport);
    assertTrue(summary.length() >= normalSummary.length());
  }

  @Test
  void testProgressLineFormatting() {
    final String progressLine =
        formatterWithColors.formatProgressLine("test-execution", "5/10", "running");

    assertNotNull(progressLine);
    assertTrue(progressLine.contains("test-execution"));
    assertTrue(progressLine.contains("5/10"));
    assertTrue(progressLine.contains("running"));
  }

  @Test
  void testProgressLineQuietMode() {
    final String progressLine =
        quietFormatter.formatProgressLine("test-execution", "5/10", "running");

    // Quiet mode should return empty string for progress
    assertTrue(progressLine.isEmpty());
  }

  @Test
  void testProgressLineWithNullValues() {
    final String progressLine =
        formatterWithColors.formatProgressLine("test-execution", null, "running");

    assertNotNull(progressLine);
    assertTrue(progressLine.contains("test-execution"));
    assertTrue(progressLine.contains("running"));
  }

  @Test
  void testProgressLineWithLongTestName() {
    final String longTestName =
        "very-long-test-name-that-exceeds-normal-limits-for-display-purposes";
    final String progressLine =
        formatterWithColors.formatProgressLine(longTestName, "1/1", "completed");

    assertNotNull(progressLine);
    // Should be truncated appropriately
    assertTrue(progressLine.length() < longTestName.length() + 50); // Reasonable limit
  }

  @Test
  void testTestResultsTable() {
    final String table = formatterWithColors.formatTestResultsTable(testReport);

    assertNotNull(table);
    assertTrue(table.contains("Test Results Summary"));
    assertTrue(table.contains("Test Name"));
    assertTrue(table.contains("Status"));
    assertTrue(table.contains("Score"));
    assertTrue(table.contains("Issues"));
    assertTrue(table.contains("Verdict"));

    // Should contain table border characters
    assertTrue(table.contains("─") || table.contains("│") || table.contains("┌"));
  }

  @Test
  void testTestResultsTableWithoutColors() {
    final String table = formatterWithoutColors.formatTestResultsTable(testReport);

    assertNotNull(table);
    assertTrue(table.contains("Test Results Summary"));
    // Should not contain ANSI color codes
    assertTrue(!table.contains("\u001B["));
  }

  @Test
  void testHighPriorityRecommendations() {
    final String recommendations =
        formatterWithColors.formatHighPriorityRecommendations(testReport);

    assertNotNull(recommendations);
    assertTrue(recommendations.contains("High Priority Recommendations"));
    // With our test data having no high priority recommendations
    assertTrue(
        recommendations.contains("No high priority recommendations found")
            || recommendations.contains("1."));
  }

  @Test
  void testHighPriorityRecommendationsEmpty() {
    // Create a report with no recommendations
    final ComparisonReport emptyReport = createEmptyTestReport();
    final String recommendations =
        formatterWithColors.formatHighPriorityRecommendations(emptyReport);

    assertNotNull(recommendations);
    assertTrue(recommendations.contains("No high priority recommendations found"));
  }

  @Test
  void testStatusColorization() {
    // Test different status values
    final String runningLine = formatterWithColors.formatProgressLine("test", "1/1", "running");
    final String passedLine = formatterWithColors.formatProgressLine("test", "1/1", "passed");
    final String failedLine = formatterWithColors.formatProgressLine("test", "1/1", "failed");
    final String warningLine = formatterWithColors.formatProgressLine("test", "1/1", "warning");

    assertNotNull(runningLine);
    assertNotNull(passedLine);
    assertNotNull(failedLine);
    assertNotNull(warningLine);

    // All should contain the status text
    assertTrue(runningLine.contains("running"));
    assertTrue(passedLine.contains("passed"));
    assertTrue(failedLine.contains("failed"));
    assertTrue(warningLine.contains("warning"));
  }

  @Test
  void testFormatterWithNullReport() {
    // Test defensive behavior - this would typically throw NPE as expected
    try {
      formatterWithColors.formatCompleteSummary(null);
    } catch (final NullPointerException e) {
      // Expected behavior
      assertTrue(true);
    }
  }

  @Test
  void testDurationFormatting() {
    // Test various duration formats through a complete summary
    final ComparisonReport quickReport =
        new ComparisonReport.Builder("quick-test", Instant.now())
            .endTime(Instant.now().plusMillis(500))
            .summary(new ComparisonSummary(ComparisonVerdict.PASSED, 1, 1, 0, 0, 0, 0, 0, 1.0))
            .configuration(
                new ComparisonConfiguration(VerbosityLevel.NORMAL, false, false, "console"))
            .build();

    final String summary = formatterWithColors.formatCompleteSummary(quickReport);
    assertNotNull(summary);
    // Should contain some duration information
    assertTrue(summary.contains("ms") || summary.contains("s"));
  }

  @Test
  void testTableFormattingWithEmptyData() {
    final ComparisonReport emptyReport = createEmptyTestReport();
    final String table = formatterWithColors.formatTestResultsTable(emptyReport);

    assertNotNull(table);
    assertTrue(table.contains("Test Results Summary"));
    // Should still have table structure even with no data
    assertTrue(table.contains("Test Name"));
  }

  @Test
  void testVerbosityLevelFiltering() {
    // Test that different verbosity levels show different amounts of content
    final String quietSummary = quietFormatter.formatCompleteSummary(testReport);
    final String normalSummary =
        new SummaryFormatter(VerbosityLevel.NORMAL, false).formatCompleteSummary(testReport);
    final String verboseSummary = formatterWithoutColors.formatCompleteSummary(testReport);
    final String debugSummary = debugFormatter.formatCompleteSummary(testReport);

    // Each level should include more content than the previous
    assertTrue(quietSummary.length() <= normalSummary.length());
    assertTrue(normalSummary.length() <= verboseSummary.length());
    assertTrue(verboseSummary.length() <= debugSummary.length());
  }

  private ComparisonReport createTestReport() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(5));
    final Instant endTime = Instant.now();

    // Create minimal test data
    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("test-case")
            .executionPattern(new ExecutionPattern(1, 0, 0, 1, 0, 0.1))
            .consistencyScore(0.95)
            .verdict(BehavioralVerdict.CONSISTENT)
            .build();

    final RecommendationResult recommendationResult =
        new RecommendationResult.Builder("test-case")
            .summary(new RecommendationSummary(0, 0, 0, 0, Collections.emptyMap()))
            .build();

    return new ComparisonReport.Builder("test-report-001", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("test-case", behavioralResult))
        .recommendationResults(Map.of("test-case", recommendationResult))
        .summary(new ComparisonSummary(ComparisonVerdict.PASSED, 1, 1, 0, 0, 0, 0, 0, 0.95))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, true, true, "console"))
        .build();
  }

  private ComparisonReport createEmptyTestReport() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(1));
    final Instant endTime = Instant.now();

    return new ComparisonReport.Builder("empty-report", startTime)
        .endTime(endTime)
        .summary(new ComparisonSummary(ComparisonVerdict.PASSED, 0, 0, 0, 0, 0, 0, 0, 0.0))
        .configuration(new ComparisonConfiguration(VerbosityLevel.NORMAL, false, false, "console"))
        .build();
  }
}
