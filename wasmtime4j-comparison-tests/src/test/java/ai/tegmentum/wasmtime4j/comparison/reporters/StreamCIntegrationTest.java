package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ExecutionPattern;
import ai.tegmentum.wasmtime4j.comparison.analyzers.IssueCategory;
import ai.tegmentum.wasmtime4j.comparison.analyzers.IssueSeverity;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationSummary;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Integration tests for Stream C console and CLI reporting functionality. Tests the complete
 * workflow from analysis results to formatted console output, verifying performance requirements
 * and CI/CD integration.
 */
final class StreamCIntegrationTest {

  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errorStream;
  private PrintStream outputPrint;
  private PrintStream errorPrint;

  @BeforeEach
  void setUp() {
    outputStream = new ByteArrayOutputStream();
    errorStream = new ByteArrayOutputStream();
    outputPrint = new PrintStream(outputStream);
    errorPrint = new PrintStream(errorStream);
  }

  @AfterEach
  void tearDown() {
    outputPrint.close();
    errorPrint.close();
  }

  @Test
  @Timeout(value = 5, unit = java.util.concurrent.TimeUnit.SECONDS)
  void testConsoleReportGenerationPerformance() {
    // Test requirement: Console summary displays < 5 seconds regardless of result set size
    final ComparisonReport largeReport = createLargeTestReport(1000);
    final ConsoleReporter reporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.NORMAL, false);

    final Instant start = Instant.now();
    reporter.generateReport(largeReport);
    final Duration duration = Duration.between(start, Instant.now());

    // Should complete within timeout (5 seconds enforced by @Timeout)
    assertTrue(duration.toMillis() < 5000, "Report generation took too long: " + duration);

    final String output = outputStream.toString();
    assertNotNull(output);
    assertTrue(output.contains("Wasmtime4j Comparison Report"));
  }

  @Test
  void testCompleteReportingWorkflow() {
    final ComparisonReport comprehensiveReport = createComprehensiveTestReport();
    final ConsoleReporter reporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, true);

    // Test complete workflow with progress reporting
    final ProgressReporter progressReporter = reporter.getProgressReporter();

    // Simulate analysis workflow
    progressReporter.onOperationStarted("Behavioral Analysis", 3);
    progressReporter.onProgress(1, "Analyzing runtime compatibility");
    progressReporter.onProgress(2, "Detecting discrepancies");
    progressReporter.onProgress(3, "Generating behavioral verdict");
    progressReporter.onOperationCompleted("Behavioral Analysis", "Found 2 discrepancies");

    progressReporter.onOperationStarted("Performance Analysis", 2);
    progressReporter.onProgress(1, "Measuring execution timing");
    progressReporter.onProgress(2, "Calculating performance ratios");
    progressReporter.onOperationCompleted("Performance Analysis", "Performance analysis complete");

    progressReporter.onOperationStarted("Report Generation", 1);
    reporter.generateReport(comprehensiveReport);
    progressReporter.onOperationCompleted("Report Generation", "Report generated successfully");

    // Verify output contains all expected sections
    final String output = outputStream.toString();
    assertTrue(output.contains("Behavioral Analysis"));
    assertTrue(output.contains("Performance Analysis"));
    assertTrue(output.contains("Report Generation"));
    assertTrue(output.contains("Wasmtime4j Comparison Report"));
    assertTrue(output.contains("Executive Summary"));
    assertTrue(output.contains("Test Results Summary"));

    // Verify appropriate exit code
    assertEquals(1, reporter.getExitCode()); // Should be 1 due to medium priority issues
  }

  @Test
  void testCiCdIntegration() {
    // Test CI/CD specific requirements
    final ConsoleReporter cicdReporter = ConsoleReporter.forCiCd();

    // Test successful scenario
    final ComparisonReport successReport = createSuccessfulReport();
    cicdReporter.generateReport(successReport);
    assertEquals(0, cicdReporter.getExitCode());

    // Test warning scenario
    final ComparisonReport warningReport = createReportWithWarnings();
    final ConsoleReporter warningReporter = ConsoleReporter.forCiCd();
    warningReporter.generateReport(warningReport);
    assertEquals(1, warningReporter.getExitCode());

    // Test failure scenario
    final ComparisonReport failureReport = createReportWithFailures();
    final ConsoleReporter failureReporter = ConsoleReporter.forCiCd();
    failureReporter.generateReport(failureReport);
    assertEquals(2, failureReporter.getExitCode());
  }

  @Test
  void testVerbosityLevelIntegration() {
    final ComparisonReport report = createComprehensiveTestReport();

    // Test all verbosity levels
    testVerbosityLevel(VerbosityLevel.QUIET, report);
    testVerbosityLevel(VerbosityLevel.NORMAL, report);
    testVerbosityLevel(VerbosityLevel.VERBOSE, report);
    testVerbosityLevel(VerbosityLevel.DEBUG, report);
  }

  @Test
  void testColorOutputIntegration() {
    final ComparisonReport report = createComprehensiveTestReport();

    // Test with colors enabled
    final ConsoleReporter colorReporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, true);
    colorReporter.generateReport(report);

    // Test with colors disabled
    outputStream.reset();
    final ConsoleReporter noColorReporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, false);
    noColorReporter.generateReport(report);

    final String noColorOutput = outputStream.toString();
    // No color output should not contain ANSI escape codes
    assertTrue(!noColorOutput.contains("\u001B["));
  }

  @Test
  void testProgressReportingIntegration() {
    final ConsoleReporter reporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, false);
    final ProgressReporter progressReporter = reporter.getProgressReporter();

    // Test various progress scenarios
    progressReporter.onOperationStarted("Multi-step Operation", 5);
    for (int i = 1; i <= 5; i++) {
      progressReporter.onProgress(i, "Step " + i + " description");
      assertTrue(progressReporter.getProgressPercentage() >= 0);
    }
    progressReporter.onOperationCompleted("Multi-step Operation", "All steps completed");

    // Test operation failure
    progressReporter.onOperationStarted("Failing Operation", 2);
    progressReporter.onProgress(1, "First step");
    progressReporter.onOperationFailed("Failing Operation", new RuntimeException("Test failure"));

    // Test indeterminate progress
    progressReporter.onOperationStarted("Unknown Duration", 0);
    progressReporter.onProgress(1, "Unknown progress");
    progressReporter.onOperationCompleted("Unknown Duration", "Completed");

    final String output = outputStream.toString();
    assertTrue(output.contains("Multi-step Operation"));
    assertTrue(output.contains("Failing Operation"));
    assertTrue(output.contains("Unknown Duration"));
  }

  @Test
  void testTableFormattingIntegration() {
    final ComparisonReport report = createReportWithMultipleTests();
    final SummaryFormatter formatter = new SummaryFormatter(VerbosityLevel.VERBOSE, false);

    final String table = formatter.formatTestResultsTable(report);

    assertNotNull(table);
    assertTrue(table.contains("Test Results Summary"));
    // Should contain table structure
    assertTrue(table.contains("─") || table.contains("│"));
    // Should contain test data
    assertTrue(table.contains("test-1"));
    assertTrue(table.contains("test-2"));
    assertTrue(table.contains("test-3"));
  }

  @Test
  void testRecommendationsIntegration() {
    final ComparisonReport reportWithRecommendations = createReportWithRecommendations();
    final ConsoleReporter reporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, false);

    reporter.generateReport(reportWithRecommendations);

    final String output = outputStream.toString();
    assertTrue(
        output.contains("High Priority Recommendations")
            || output.contains("No high priority recommendations"));
  }

  @Test
  void testErrorHandlingIntegration() {
    final ConsoleReporter reporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, false);

    // Test various error scenarios
    reporter.showError("Critical error", "Detailed error information");
    reporter.showWarning("Warning message");
    reporter.showInfo("Information message");

    final String output = outputStream.toString();
    final String errorOutput = errorStream.toString();

    assertTrue(output.contains("Warning message"));
    assertTrue(output.contains("Information message"));
    assertTrue(errorOutput.contains("Critical error"));
    assertTrue(errorOutput.contains("Detailed error information"));

    assertEquals(2, reporter.getExitCode()); // Error should set exit code to 2
  }

  private void testVerbosityLevel(final VerbosityLevel level, final ComparisonReport report) {
    outputStream.reset();
    final ConsoleReporter reporter = new ConsoleReporter(outputPrint, errorPrint, level, false);
    reporter.generateReport(report);

    final String output = outputStream.toString();
    assertNotNull(output);

    // All levels should at least show the basic header
    if (level != VerbosityLevel.QUIET) {
      assertTrue(output.contains("Wasmtime4j Comparison Report"));
    }
  }

  private ComparisonReport createLargeTestReport(final int testCount) {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(10));
    final Instant endTime = Instant.now();

    final Map<String, BehavioralAnalysisResult> behavioralResults =
        Collections.nCopies(testCount, "test").stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    name -> name + "-" + System.nanoTime(),
                    name ->
                        new BehavioralAnalysisResult.Builder(name)
                            .executionPattern(new ExecutionPattern(1, 0, 0, 1, 0, 0.1))
                            .consistencyScore(0.9 + Math.random() * 0.1)
                            .verdict(BehavioralVerdict.CONSISTENT)
                            .build()));

    return new ComparisonReport.Builder("large-report", startTime)
        .endTime(endTime)
        .behavioralResults(behavioralResults)
        .summary(
            new ComparisonSummary(
                ComparisonVerdict.PASSED, testCount, testCount, 0, 0, 0, 0, 0, 0.95))
        .configuration(new ComparisonConfiguration(VerbosityLevel.NORMAL, false, false, "console"))
        .build();
  }

  private ComparisonReport createComprehensiveTestReport() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(5));
    final Instant endTime = Instant.now();

    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("comprehensive-test")
            .executionPattern(new ExecutionPattern(1, 0, 0, 2, 1, 0.2))
            .consistencyScore(0.85)
            .verdict(BehavioralVerdict.MOSTLY_CONSISTENT)
            .build();

    final RecommendationResult recommendationResult =
        new RecommendationResult.Builder("comprehensive-test")
            .summary(new RecommendationSummary(3, 0, 3, 0, Collections.emptyMap()))
            .build();

    return new ComparisonReport.Builder("comprehensive-report", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("comprehensive-test", behavioralResult))
        .recommendationResults(Map.of("comprehensive-test", recommendationResult))
        .summary(
            new ComparisonSummary(
                ComparisonVerdict.PASSED_WITH_WARNINGS, 1, 1, 0, 0, 0, 3, 0, 0.85))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, true, true, "console"))
        .build();
  }

  private ComparisonReport createSuccessfulReport() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(2));
    final Instant endTime = Instant.now();

    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("success-test")
            .executionPattern(new ExecutionPattern(1, 0, 0, 1, 0, 0.05))
            .consistencyScore(0.98)
            .verdict(BehavioralVerdict.CONSISTENT)
            .build();

    return new ComparisonReport.Builder("success-report", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("success-test", behavioralResult))
        .summary(new ComparisonSummary(ComparisonVerdict.PASSED, 1, 1, 0, 0, 0, 0, 0, 0.98))
        .configuration(new ComparisonConfiguration(VerbosityLevel.NORMAL, false, false, "console"))
        .build();
  }

  private ComparisonReport createReportWithWarnings() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(3));
    final Instant endTime = Instant.now();

    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("warning-test")
            .executionPattern(new ExecutionPattern(1, 0, 0, 2, 1, 0.15))
            .consistencyScore(0.85)
            .verdict(BehavioralVerdict.MOSTLY_CONSISTENT)
            .build();

    return new ComparisonReport.Builder("warning-report", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("warning-test", behavioralResult))
        .summary(
            new ComparisonSummary(
                ComparisonVerdict.PASSED_WITH_WARNINGS, 1, 1, 0, 0, 0, 2, 0, 0.85))
        .configuration(new ComparisonConfiguration(VerbosityLevel.NORMAL, false, false, "console"))
        .build();
  }

  private ComparisonReport createReportWithFailures() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(5));
    final Instant endTime = Instant.now();

    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("failure-test")
            .executionPattern(new ExecutionPattern(0, 1, 0, 3, 2, 0.8))
            .consistencyScore(0.45)
            .verdict(BehavioralVerdict.INCOMPATIBLE)
            .build();

    return new ComparisonReport.Builder("failure-report", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("failure-test", behavioralResult))
        .summary(
            new ComparisonSummary(ComparisonVerdict.FAILED_WITH_ISSUES, 1, 0, 1, 0, 3, 2, 0, 0.45))
        .configuration(new ComparisonConfiguration(VerbosityLevel.NORMAL, false, false, "console"))
        .build();
  }

  private ComparisonReport createReportWithMultipleTests() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(5));
    final Instant endTime = Instant.now();

    final Map<String, BehavioralAnalysisResult> behavioralResults =
        Map.of(
            "test-1",
                new BehavioralAnalysisResult.Builder("test-1")
                    .executionPattern(new ExecutionPattern(1, 0, 0, 1, 0, 0.1))
                    .consistencyScore(0.95)
                    .verdict(BehavioralVerdict.CONSISTENT)
                    .build(),
            "test-2",
                new BehavioralAnalysisResult.Builder("test-2")
                    .executionPattern(new ExecutionPattern(1, 0, 0, 2, 1, 0.2))
                    .consistencyScore(0.85)
                    .verdict(BehavioralVerdict.MOSTLY_CONSISTENT)
                    .build(),
            "test-3",
                new BehavioralAnalysisResult.Builder("test-3")
                    .executionPattern(new ExecutionPattern(0, 1, 0, 1, 1, 0.5))
                    .consistencyScore(0.60)
                    .verdict(BehavioralVerdict.INCONSISTENT)
                    .build());

    return new ComparisonReport.Builder("multi-test-report", startTime)
        .endTime(endTime)
        .behavioralResults(behavioralResults)
        .summary(
            new ComparisonSummary(
                ComparisonVerdict.PASSED_WITH_WARNINGS, 3, 2, 1, 0, 1, 2, 0, 0.80))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, false, false, "console"))
        .build();
  }

  private ComparisonReport createReportWithRecommendations() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(3));
    final Instant endTime = Instant.now();

    final ActionableRecommendation highPriorityRec =
        new ActionableRecommendation(
            "Fix critical timing discrepancy",
            "Runtime timing differs significantly between implementations",
            List.of("Analyze timing measurement code", "Check for timing precision differences"),
            IssueCategory.PERFORMANCE,
            IssueSeverity.HIGH,
            0.95,
            Set.of(RuntimeType.JNI, RuntimeType.PANAMA),
            "timing-variance-pattern");

    final RecommendationResult recommendationResult =
        new RecommendationResult.Builder("rec-test")
            .prioritizedRecommendations(List.of(highPriorityRec))
            .summary(new RecommendationSummary(1, 1, 0, 0, Map.of(IssueCategory.PERFORMANCE, 1)))
            .build();

    return new ComparisonReport.Builder("recommendations-report", startTime)
        .endTime(endTime)
        .recommendationResults(Map.of("rec-test", recommendationResult))
        .summary(
            new ComparisonSummary(
                ComparisonVerdict.PASSED_WITH_WARNINGS, 1, 1, 0, 0, 1, 0, 0, 0.85))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, false, false, "console"))
        .build();
  }
}
