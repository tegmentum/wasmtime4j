package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Comprehensive console reporter that provides colored, formatted output for comparison analysis
 * results. Supports multiple verbosity levels, real-time progress reporting, and CI/CD integration
 * through appropriate exit codes.
 *
 * <p>Features:
 *
 * <ul>
 *   <li>ANSI color support with automatic fallback
 *   <li>Table-formatted output for structured data
 *   <li>Real-time progress indicators during long operations
 *   <li>Configurable verbosity levels (quiet, normal, verbose, debug)
 *   <li>CI/CD integration with appropriate exit codes
 * </ul>
 *
 * @since 1.0.0
 */
public final class ConsoleReporter {
  private static final Logger LOGGER = Logger.getLogger(ConsoleReporter.class.getName());

  private final PrintStream output;
  private final PrintStream errorOutput;
  private final SummaryFormatter formatter;
  private final ProgressReporter progressReporter;
  private final VerbosityLevel verbosity;
  private final boolean useColors;

  // CI/CD integration
  private volatile int exitCode = 0;
  private volatile boolean hasHighPriorityIssues = false;
  private volatile boolean hasComparisonFailures = false;

  /**
   * Creates a new ConsoleReporter with the specified configuration.
   *
   * @param output the output stream for normal output
   * @param errorOutput the output stream for error output
   * @param verbosity the verbosity level for controlling output detail
   * @param useColors whether to use ANSI color codes
   */
  public ConsoleReporter(
      final PrintStream output,
      final PrintStream errorOutput,
      final VerbosityLevel verbosity,
      final boolean useColors) {
    this.output = Objects.requireNonNull(output, "output cannot be null");
    this.errorOutput = Objects.requireNonNull(errorOutput, "errorOutput cannot be null");
    this.verbosity = Objects.requireNonNull(verbosity, "verbosity cannot be null");
    this.useColors = useColors && ConsoleColors.isColorSupported();
    this.formatter = new SummaryFormatter(verbosity, this.useColors);
    this.progressReporter = new ProgressReporter(output, verbosity, this.useColors);
  }

  /**
   * Creates a ConsoleReporter that outputs to System.out and System.err.
   *
   * @param verbosity the verbosity level
   * @param useColors whether to use colors
   * @return new ConsoleReporter instance
   */
  public static ConsoleReporter forStandardOutput(
      final VerbosityLevel verbosity, final boolean useColors) {
    return new ConsoleReporter(System.out, System.err, verbosity, useColors);
  }

  /**
   * Creates a ConsoleReporter with configuration suitable for CI/CD environments.
   *
   * @return new ConsoleReporter instance optimized for CI/CD
   */
  public static ConsoleReporter forCiCd() {
    // CI/CD environments typically prefer no colors and normal verbosity
    final boolean useColors = System.getenv("FORCE_COLOR") != null;
    final VerbosityLevel verbosity =
        VerbosityLevel.fromString(System.getenv().getOrDefault("WASMTIME4J_VERBOSITY", "normal"));

    final ConsoleReporter reporter = forStandardOutput(verbosity, useColors);
    reporter.progressReporter.setProgressBarEnabled(false); // No progress bars in CI
    reporter.progressReporter.setTimestampsEnabled(true); // Timestamps useful for CI logs

    return reporter;
  }

  /**
   * Generates and displays a comprehensive comparison report.
   *
   * @param report the comparison report to display
   */
  public void generateReport(final ComparisonReport report) {
    Objects.requireNonNull(report, "report cannot be null");

    try {
      final Instant reportStart = Instant.now();

      // Show report header
      showReportHeader(report);

      // Analyze report for CI/CD exit code determination
      analyzeReportForExitCode(report);

      // Generate formatted summary
      final String summary = formatter.formatCompleteSummary(report);
      output.print(summary);

      // Show additional sections based on verbosity
      if (verbosity.includes(VerbosityLevel.VERBOSE)) {
        showDetailedRecommendations(report);
        showPerformanceAnalysis(report);
      }

      if (verbosity.includes(VerbosityLevel.DEBUG)) {
        showDebugInformation(report);
        showOperationStatistics();
      }

      // Show final status and exit code information
      showReportFooter(report, reportStart);

    } catch (final Exception e) {
      LOGGER.severe("Failed to generate console report: " + e.getMessage());
      errorOutput.printf("Error generating report: %s%n", e.getMessage());
      if (verbosity.includes(VerbosityLevel.DEBUG)) {
        e.printStackTrace(errorOutput);
      }
      exitCode = 2; // Technical failure
    }
  }

  /**
   * Gets the progress reporter for real-time operation updates.
   *
   * @return the progress reporter instance
   */
  public ProgressReporter getProgressReporter() {
    return progressReporter;
  }

  /**
   * Gets the exit code determined by the analysis results. This code is suitable for CI/CD pipeline
   * integration.
   *
   * @return exit code (0 = success, 1 = warnings/issues, 2 = failures/errors)
   */
  public int getExitCode() {
    return exitCode;
  }

  /**
   * Displays a quick status summary during operation.
   *
   * @param message the status message
   * @param testsPassed number of tests passed
   * @param testsTotal total number of tests
   */
  public void showQuickStatus(final String message, final int testsPassed, final int testsTotal) {
    if (verbosity == VerbosityLevel.QUIET) {
      return;
    }

    final String progress = testsTotal > 0 ? String.format("%d/%d", testsPassed, testsTotal) : "";
    final String line = formatter.formatProgressLine("Comparison", progress, message);
    output.println(line);
  }

  /**
   * Displays an error message with appropriate formatting.
   *
   * @param message the error message
   * @param details optional error details
   */
  public void showError(final String message, final String details) {
    if (useColors) {
      errorOutput.printf("%s %s%n", ConsoleColors.error("✗"), message);
    } else {
      errorOutput.printf("ERROR: %s%n", message);
    }

    if (details != null && verbosity.includes(VerbosityLevel.VERBOSE)) {
      errorOutput.printf("  %s%n", details);
    }

    exitCode = Math.max(exitCode, 2); // Set error exit code
  }

  /**
   * Displays a warning message with appropriate formatting.
   *
   * @param message the warning message
   */
  public void showWarning(final String message) {
    if (verbosity == VerbosityLevel.QUIET) {
      return;
    }

    if (useColors) {
      output.printf("%s %s%n", ConsoleColors.warning("⚠"), message);
    } else {
      output.printf("WARNING: %s%n", message);
    }

    exitCode = Math.max(exitCode, 1); // Set warning exit code
  }

  /**
   * Displays an informational message.
   *
   * @param message the information message
   */
  public void showInfo(final String message) {
    if (verbosity.includes(VerbosityLevel.NORMAL)) {
      if (useColors) {
        output.printf("%s %s%n", ConsoleColors.info("ℹ"), message);
      } else {
        output.printf("INFO: %s%n", message);
      }
    }
  }

  private void showReportHeader(final ComparisonReport report) {
    if (verbosity == VerbosityLevel.QUIET) {
      return;
    }

    output.println();
    if (useColors) {
      output.println(ConsoleColors.header("Wasmtime4j Comparison Analysis Report"));
    } else {
      output.println("=".repeat(50));
      output.println("  Wasmtime4j Comparison Analysis Report");
      output.println("=".repeat(50));
    }
    output.println();
  }

  private void analyzeReportForExitCode(final ComparisonReport report) {
    final ComparisonSummary summary = report.getSummary();

    // Check for high priority issues
    hasHighPriorityIssues = summary.getHighPriorityIssueCount() > 0;

    // Check for comparison failures
    hasComparisonFailures = summary.getFailedTests() > 0 || !report.isSuccessful();

    // Determine exit code based on findings
    if (hasComparisonFailures) {
      exitCode = 2; // Failures
    } else if (hasHighPriorityIssues || summary.getMediumPriorityIssueCount() > 0) {
      exitCode = 1; // Warnings/issues
    } else {
      exitCode = 0; // Success
    }

    // Override for specific verdicts
    switch (summary.getOverallVerdict()) {
      case FAILED -> exitCode = 2;
      case FAILED_WITH_ISSUES -> exitCode = 2;
      case PASSED_WITH_WARNINGS -> exitCode = Math.max(exitCode, 1);
      case PASSED -> exitCode = Math.min(exitCode, 0);
    }
  }

  private void showDetailedRecommendations(final ComparisonReport report) {
    output.println();
    output.println(formatSectionHeader("Detailed Recommendations"));
    output.println();

    int totalRecommendations = 0;
    for (final var entry : report.getRecommendationResults().entrySet()) {
      final String testName = entry.getKey();
      final RecommendationResult recommendations = entry.getValue();

      if (!recommendations.getPrioritizedRecommendations().isEmpty()) {
        output.printf("Test: %s%n", formatTestName(testName));

        for (final ActionableRecommendation rec : recommendations.getPrioritizedRecommendations()) {
          totalRecommendations++;
          showRecommendation(rec);
        }
        output.println();
      }
    }

    if (totalRecommendations == 0) {
      output.println(colorize("No specific recommendations generated.", ConsoleColors.GREEN));
    }
  }

  private void showRecommendation(final ActionableRecommendation recommendation) {
    final String priority = colorizeRecommendationPriority(recommendation.getSeverity().toString());
    output.printf(
        "  %s [%s] %s%n", colorize("•", ConsoleColors.BLUE), priority, recommendation.getTitle());

    if (verbosity.includes(VerbosityLevel.DEBUG)) {
      output.printf("    %s%n", recommendation.getDescription());
      output.printf("    Affected: %s%n", recommendation.getAffectedRuntimes());
      output.printf("    Priority Score: %.2f%n", recommendation.getPriorityScore());
    }
  }

  private void showPerformanceAnalysis(final ComparisonReport report) {
    output.println();
    output.println(formatSectionHeader("Performance Analysis"));
    output.println();

    if (report.getPerformanceResults().isEmpty()) {
      output.println("No performance data available.");
      return;
    }

    // Show performance summary
    int significantDifferences = 0;
    for (final var entry : report.getPerformanceResults().entrySet()) {
      if (entry.getValue().hasSignificantDifferences()) {
        significantDifferences++;
      }
    }

    output.printf(
        "Tests with performance differences: %d/%d%n",
        significantDifferences, report.getPerformanceResults().size());

    if (significantDifferences > 0) {
      output.println("See detailed performance report for specific metrics.");
    }
  }

  private void showDebugInformation(final ComparisonReport report) {
    output.println();
    output.println(formatSectionHeader("Debug Information"));
    output.println();

    // Show configuration
    output.printf("Configuration:%n");
    output.printf("  Name: %s%n", report.getConfiguration().getConfigurationName());
    output.printf(
        "  Content: Summary=%s, Metadata=%s%n",
        report.getConfiguration().getContentConfig().isIncludeSummary(),
        report.getConfiguration().getContentConfig().isIncludeMetadata());
    output.println();

    // Show test breakdown
    output.printf("Test Results Breakdown:%n");
    int compatible = 0;
    int incompatible = 0;

    for (final BehavioralAnalysisResult result : report.getBehavioralResults().values()) {
      if (result.isCompatible()) {
        compatible++;
      } else {
        incompatible++;
      }
    }

    output.printf("  Compatible: %d%n", compatible);
    output.printf("  Incompatible: %d%n", incompatible);
    output.printf("  Total: %d%n", compatible + incompatible);
  }

  private void showOperationStatistics() {
    output.println();
    output.println(formatSectionHeader("Operation Statistics"));
    output.println();

    final var stats = progressReporter.getOperationStats();
    if (stats.isEmpty()) {
      output.println("No operation statistics available.");
      return;
    }

    for (final var entry : stats.entrySet()) {
      final String operation = entry.getKey();
      final ProgressReporter.OperationStats stat = entry.getValue();

      output.printf("  %s:%n", operation);
      output.printf(
          "    Count: %d (%.1f%% success)%n", stat.getCount(), stat.getSuccessRate() * 100);
      output.printf(
          "    Timing: avg=%s, min=%s, max=%s%n",
          formatDuration(stat.getAverageDuration()),
          formatDuration(stat.getMinDuration()),
          formatDuration(stat.getMaxDuration()));
    }
  }

  private void showReportFooter(final ComparisonReport report, final Instant reportStart) {
    output.println();
    output.println("─".repeat(80));

    final String exitCodeColor =
        switch (exitCode) {
          case 0 -> ConsoleColors.GREEN;
          case 1 -> ConsoleColors.YELLOW;
          default -> ConsoleColors.RED;
        };

    final String exitCodeMessage =
        switch (exitCode) {
          case 0 -> "All tests passed successfully";
          case 1 -> "Tests completed with warnings";
          case 2 -> "Tests failed or encountered errors";
          default -> "Unknown status";
        };

    if (useColors) {
      output.printf(
          "Exit Code: %s (%s)%n",
          colorize(String.valueOf(exitCode), exitCodeColor),
          colorize(exitCodeMessage, exitCodeColor));
    } else {
      output.printf("Exit Code: %d (%s)%n", exitCode, exitCodeMessage);
    }

    final java.time.Duration reportDuration =
        java.time.Duration.between(reportStart, Instant.now());
    output.printf("Report generated in %s%n", formatDuration(reportDuration));
    output.printf("Analysis completed at %s%n", Instant.now());
  }

  private String formatSectionHeader(final String title) {
    if (useColors) {
      return ConsoleColors.colorize(title, ConsoleColors.BOLD + ConsoleColors.BLUE);
    } else {
      return title + "\n" + "-".repeat(title.length());
    }
  }

  private String formatTestName(final String testName) {
    return useColors ? ConsoleColors.bold(testName) : testName;
  }

  private String colorize(final String text, final String color) {
    return useColors ? ConsoleColors.colorize(text, color) : text;
  }

  private String colorizeRecommendationPriority(final String priority) {
    if (!useColors) {
      return priority;
    }

    return switch (priority.toUpperCase()) {
      case "HIGH" -> ConsoleColors.error(priority);
      case "MEDIUM" -> ConsoleColors.warning(priority);
      case "LOW" -> ConsoleColors.info(priority);
      default -> priority;
    };
  }

  private String formatDuration(final java.time.Duration duration) {
    final long millis = duration.toMillis();
    if (millis < 1000) {
      return millis + "ms";
    } else if (millis < 60000) {
      return String.format("%.1fs", millis / 1000.0);
    } else {
      final long seconds = duration.getSeconds();
      final long minutes = seconds / 60;
      final long remainingSeconds = seconds % 60;
      return String.format("%dm %ds", minutes, remainingSeconds);
    }
  }
}
