package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ExecutionPattern;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationSummary;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for ConsoleReporter main reporting functionality. Tests exit codes,
 * CI/CD integration, output formatting, and error handling.
 */
final class ConsoleReporterTest {

  private ByteArrayOutputStream outputStream;
  private ByteArrayOutputStream errorStream;
  private PrintStream outputPrint;
  private PrintStream errorPrint;
  private ConsoleReporter reporter;
  private ConsoleReporter quietReporter;
  private ConsoleReporter cicdReporter;

  @BeforeEach
  void setUp() {
    outputStream = new ByteArrayOutputStream();
    errorStream = new ByteArrayOutputStream();
    outputPrint = new PrintStream(outputStream);
    errorPrint = new PrintStream(errorStream);

    reporter = new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, false);
    quietReporter = new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.QUIET, false);
    cicdReporter = ConsoleReporter.forCiCd();
  }

  @AfterEach
  void tearDown() {
    outputPrint.close();
    errorPrint.close();
  }

  @Test
  void testSuccessfulReportGeneration() {
    final ComparisonReport successReport = createSuccessfulReport();

    reporter.generateReport(successReport);

    final String output = outputStream.toString();
    assertNotNull(output);
    assertTrue(output.contains("Wasmtime4j Comparison Report"));
    assertTrue(output.contains("Executive Summary"));
    assertTrue(output.contains(successReport.getReportId()));

    // Should have success exit code
    assertEquals(0, reporter.getExitCode());
  }

  @Test
  void testReportWithWarnings() {
    final ComparisonReport warningReport = createReportWithWarnings();

    reporter.generateReport(warningReport);

    final String output = outputStream.toString();
    assertNotNull(output);
    assertTrue(output.contains("Wasmtime4j Comparison Report"));

    // Should have warning exit code
    assertEquals(1, reporter.getExitCode());
  }

  @Test
  void testReportWithFailures() {
    final ComparisonReport failureReport = createReportWithFailures();

    reporter.generateReport(failureReport);

    final String output = outputStream.toString();
    assertNotNull(output);
    assertTrue(output.contains("Wasmtime4j Comparison Report"));

    // Should have failure exit code
    assertEquals(2, reporter.getExitCode());
  }

  @Test
  void testQuietModeOutput() {
    final ComparisonReport report = createSuccessfulReport();

    quietReporter.generateReport(report);

    final String output = outputStream.toString();
    // Quiet mode should produce minimal output
    assertTrue(output.length() < 500); // Arbitrary small size for quiet output
  }

  @Test
  void testProgressReporterIntegration() {
    final ProgressReporter progressReporter = reporter.getProgressReporter();
    assertNotNull(progressReporter);

    // Test that progress reporter works with the console reporter
    progressReporter.onOperationStarted("Test Operation", 5);
    progressReporter.onProgress(2, "Step 2");
    progressReporter.onOperationCompleted("Test Operation", "Success");

    final String output = outputStream.toString();
    assertTrue(output.contains("Test Operation"));
  }

  @Test
  void testQuickStatus() {
    reporter.showQuickStatus("Processing tests", 5, 10);

    final String output = outputStream.toString();
    assertTrue(output.contains("Processing tests"));
    assertTrue(output.contains("5/10"));
  }

  @Test
  void testQuickStatusInQuietMode() {
    quietReporter.showQuickStatus("Processing tests", 5, 10);

    final String output = outputStream.toString();
    // Quiet mode should not show quick status
    assertTrue(output.isEmpty() || !output.contains("Processing tests"));
  }

  @Test
  void testErrorReporting() {
    reporter.showError("Test error message", "Additional error details");

    final String errorOutput = errorStream.toString();
    assertTrue(errorOutput.contains("Test error message"));
    assertTrue(errorOutput.contains("Additional error details"));

    // Should set error exit code
    assertEquals(2, reporter.getExitCode());
  }

  @Test
  void testWarningReporting() {
    reporter.showWarning("Test warning message");

    final String output = outputStream.toString();
    assertTrue(output.contains("Test warning message"));

    // Should set warning exit code
    assertEquals(1, reporter.getExitCode());
  }

  @Test
  void testWarningReportingInQuietMode() {
    quietReporter.showWarning("Test warning message");

    final String output = outputStream.toString();
    // Quiet mode should not show warnings
    assertTrue(output.isEmpty() || !output.contains("Test warning message"));
  }

  @Test
  void testInfoReporting() {
    reporter.showInfo("Test info message");

    final String output = outputStream.toString();
    assertTrue(output.contains("Test info message"));

    // Info messages should not affect exit code
    assertEquals(0, reporter.getExitCode());
  }

  @Test
  void testExitCodePriority() {
    // Test that error codes take priority over warning codes
    reporter.showWarning("Warning first");
    assertEquals(1, reporter.getExitCode());

    reporter.showError("Error second", null);
    assertEquals(2, reporter.getExitCode());

    // Additional warnings should not downgrade error code
    reporter.showWarning("Warning after error");
    assertEquals(2, reporter.getExitCode());
  }

  @Test
  void testCiCdFactoryMethod() {
    assertNotNull(cicdReporter);
    // CI/CD reporter should function without exceptions
    final ComparisonReport report = createSuccessfulReport();
    cicdReporter.generateReport(report);
  }

  @Test
  void testStandardOutputFactoryMethod() {
    final ConsoleReporter stdReporter =
        ConsoleReporter.forStandardOutput(VerbosityLevel.NORMAL, true);
    assertNotNull(stdReporter);

    // Should be able to use the reporter
    stdReporter.showInfo("Factory test");
    assertEquals(0, stdReporter.getExitCode());
  }

  @Test
  void testReportGenerationException() {
    // Test with null report to trigger exception handling
    try {
      reporter.generateReport(null);
    } catch (final NullPointerException e) {
      // Expected - null check should catch this
    }

    // Exit code should indicate technical failure
    assertEquals(2, reporter.getExitCode());
  }

  @Test
  void testErrorWithoutDetails() {
    reporter.showError("Error without details", null);

    final String errorOutput = errorStream.toString();
    assertTrue(errorOutput.contains("Error without details"));
    // Should not show "null" or crash
    assertTrue(!errorOutput.contains("null"));
  }

  @Test
  void testVerbosityLevelImpactOnOutput() {
    final ComparisonReport report = createSuccessfulReport();

    // Generate reports with different verbosity levels
    final ConsoleReporter normalReporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.NORMAL, false);
    final ConsoleReporter verboseReporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.VERBOSE, false);
    final ConsoleReporter debugReporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.DEBUG, false);

    // Reset stream for each test
    outputStream.reset();
    normalReporter.generateReport(report);
    final String normalOutput = outputStream.toString();

    outputStream.reset();
    verboseReporter.generateReport(report);
    final String verboseOutput = outputStream.toString();

    outputStream.reset();
    debugReporter.generateReport(report);
    final String debugOutput = outputStream.toString();

    // Verbose should have more content than normal
    assertTrue(verboseOutput.length() >= normalOutput.length());
    // Debug should have more content than verbose
    assertTrue(debugOutput.length() >= verboseOutput.length());
  }

  @Test
  void testColorOutputHandling() {
    final ConsoleReporter colorReporter =
        new ConsoleReporter(outputPrint, errorPrint, VerbosityLevel.NORMAL, true);

    colorReporter.showError("Colored error", null);
    colorReporter.showWarning("Colored warning");
    colorReporter.showInfo("Colored info");

    final String output = outputStream.toString();
    final String errorOutput = errorStream.toString();

    assertNotNull(output);
    assertNotNull(errorOutput);
    // Output should contain the messages regardless of color support
    assertTrue(output.contains("Colored warning") || output.contains("Colored info"));
    assertTrue(errorOutput.contains("Colored error"));
  }

  @Test
  void testMultipleOperationStatuses() {
    reporter.showQuickStatus("Starting", 0, 10);
    reporter.showQuickStatus("In progress", 5, 10);
    reporter.showQuickStatus("Completing", 10, 10);

    final String output = outputStream.toString();
    assertTrue(output.contains("Starting"));
    assertTrue(output.contains("In progress"));
    assertTrue(output.contains("Completing"));
  }

  private ComparisonReport createSuccessfulReport() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(2));
    final Instant endTime = Instant.now();

    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("successful-test")
            .executionPattern(new ExecutionPattern(1, 0, 0, 1, 0, 0.05))
            .consistencyScore(0.98)
            .verdict(BehavioralVerdict.CONSISTENT)
            .build();

    final RecommendationResult recommendationResult =
        new RecommendationResult.Builder("successful-test")
            .summary(new RecommendationSummary(0, 0, 0, 0, Collections.emptyMap()))
            .build();

    return new ComparisonReport.Builder("success-report-001", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("successful-test", behavioralResult))
        .recommendationResults(Map.of("successful-test", recommendationResult))
        .summary(new ComparisonSummary(ComparisonVerdict.PASSED, 1, 1, 0, 0, 0, 0, 0, 0.98))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, false, true, "console"))
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

    final RecommendationResult recommendationResult =
        new RecommendationResult.Builder("warning-test")
            .summary(new RecommendationSummary(2, 0, 2, 0, Collections.emptyMap()))
            .build();

    return new ComparisonReport.Builder("warning-report-001", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("warning-test", behavioralResult))
        .recommendationResults(Map.of("warning-test", recommendationResult))
        .summary(
            new ComparisonSummary(
                ComparisonVerdict.PASSED_WITH_WARNINGS, 1, 1, 0, 0, 0, 2, 0, 0.85))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, false, true, "console"))
        .build();
  }

  private ComparisonReport createReportWithFailures() {
    final Instant startTime = Instant.now().minus(Duration.ofMinutes(5));
    final Instant endTime = Instant.now();

    final BehavioralAnalysisResult behavioralResult =
        new BehavioralAnalysisResult.Builder("failing-test")
            .executionPattern(new ExecutionPattern(0, 1, 0, 3, 2, 0.8))
            .consistencyScore(0.45)
            .verdict(BehavioralVerdict.INCOMPATIBLE)
            .build();

    final RecommendationResult recommendationResult =
        new RecommendationResult.Builder("failing-test")
            .summary(new RecommendationSummary(5, 3, 2, 0, Collections.emptyMap()))
            .build();

    return new ComparisonReport.Builder("failure-report-001", startTime)
        .endTime(endTime)
        .behavioralResults(Map.of("failing-test", behavioralResult))
        .recommendationResults(Map.of("failing-test", recommendationResult))
        .summary(
            new ComparisonSummary(ComparisonVerdict.FAILED_WITH_ISSUES, 1, 0, 1, 0, 3, 2, 0, 0.45))
        .configuration(new ComparisonConfiguration(VerbosityLevel.VERBOSE, false, true, "console"))
        .build();
  }
}
