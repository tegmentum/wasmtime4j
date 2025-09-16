package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Formats comparison analysis results into structured console output with tables, summaries, and
 * detailed breakdowns. Supports both colored and plain text output with configurable verbosity
 * levels.
 *
 * @since 1.0.0
 */
public final class SummaryFormatter {
  private static final DateTimeFormatter TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final String TABLE_BORDER = "─";
  private static final String TABLE_CORNER = "├";
  private static final String TABLE_VERTICAL = "│";
  private static final int DEFAULT_TABLE_WIDTH = 80;

  private final VerbosityLevel verbosity;
  private final boolean useColors;

  /**
   * Creates a new SummaryFormatter with the specified configuration.
   *
   * @param verbosity the verbosity level for output detail
   * @param useColors whether to use ANSI color codes
   */
  public SummaryFormatter(final VerbosityLevel verbosity, final boolean useColors) {
    this.verbosity = Objects.requireNonNull(verbosity, "verbosity cannot be null");
    this.useColors = useColors && ConsoleColors.isColorSupported();
  }

  /**
   * Formats the complete comparison report into a comprehensive summary.
   *
   * @param report the comparison report to format
   * @return formatted summary string
   */
  public String formatCompleteSummary(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();

    // Header
    sb.append(formatHeader("Wasmtime4j Comparison Report"));
    sb.append("\n\n");

    // Executive summary
    sb.append(formatExecutiveSummary(report));
    sb.append("\n\n");

    // Detailed sections based on verbosity
    if (verbosity.includes(VerbosityLevel.NORMAL)) {
      sb.append(formatTestResultsTable(report));
      sb.append("\n\n");

      if (!report.getRecommendationResults().isEmpty()) {
        sb.append(formatHighPriorityRecommendations(report));
        sb.append("\n\n");
      }
    }

    if (verbosity.includes(VerbosityLevel.VERBOSE)) {
      sb.append(formatPerformanceSummary(report));
      sb.append("\n\n");

      sb.append(formatCoverageSummary(report));
      sb.append("\n\n");
    }

    if (verbosity.includes(VerbosityLevel.DEBUG)) {
      sb.append(formatDetailedAnalysis(report));
      sb.append("\n\n");
    }

    // Footer
    sb.append(formatFooter(report));

    return sb.toString();
  }

  /**
   * Formats a quick status summary suitable for progress updates.
   *
   * @param currentTest the name of the current test
   * @param progress progress information (e.g., "5/10")
   * @param status current status
   * @return formatted status line
   */
  public String formatProgressLine(
      final String currentTest, final String progress, final String status) {
    if (verbosity == VerbosityLevel.QUIET) {
      return "";
    }

    final String progressPart = progress != null ? "[" + progress + "] " : "";
    final String testPart = truncateString(currentTest, 40);
    final String statusPart = colorizeStatus(status);

    return String.format("%s%-40s %s", progressPart, testPart, statusPart);
  }

  /**
   * Formats a table of test results with pass/fail status and key metrics.
   *
   * @param report the comparison report
   * @return formatted table
   */
  public String formatTestResultsTable(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(formatSectionHeader("Test Results Summary"));
    sb.append("\n");

    // Table header
    final String[] headers = {"Test Name", "Status", "Score", "Issues", "Verdict"};
    final int[] widths = {35, 12, 8, 8, 15};

    sb.append(formatTableHeader(headers, widths));

    // Table rows
    for (final var entry : report.getBehavioralResults().entrySet()) {
      final String testName = entry.getKey();
      final BehavioralAnalysisResult result = entry.getValue();

      final String status =
          result.isCompatible()
              ? colorize("PASS", ConsoleColors.GREEN)
              : colorize("FAIL", ConsoleColors.RED);

      final String score = String.format("%.2f", result.getConsistencyScore());
      final String issues = String.valueOf(result.getCriticalDiscrepancyCount());
      final String verdict = colorizeVerdict(result.getVerdict().toString());

      final String[] row = {
        truncateString(testName, widths[0] - 2), status, score, issues, verdict
      };

      sb.append(formatTableRow(row, widths));
    }

    sb.append(formatTableFooter(widths));

    return sb.toString();
  }

  /**
   * Formats high-priority recommendations for immediate attention.
   *
   * @param report the comparison report
   * @return formatted recommendations
   */
  public String formatHighPriorityRecommendations(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(formatSectionHeader("High Priority Recommendations"));
    sb.append("\n");

    int count = 0;
    for (final var entry : report.getRecommendationResults().entrySet()) {
      final RecommendationResult recommendations = entry.getValue();
      for (final ActionableRecommendation rec : recommendations.getHighPriorityRecommendations()) {
        count++;
        sb.append(String.format("%d. %s\n", count, colorize(rec.getTitle(), ConsoleColors.BOLD)));
        sb.append(String.format("   %s\n", rec.getDescription()));
        sb.append(String.format("   Affected: %s\n", rec.getAffectedRuntimes()));
        if (verbosity.includes(VerbosityLevel.VERBOSE)) {
          sb.append(String.format("   Priority Score: %.2f\n", rec.getPriorityScore()));
        }
        sb.append("\n");
      }
    }

    if (count == 0) {
      sb.append(colorize("No high priority recommendations found.\n", ConsoleColors.GREEN));
    }

    return sb.toString();
  }

  private String formatHeader(final String title) {
    if (useColors) {
      return ConsoleColors.header(title);
    } else {
      final String border = "=".repeat(title.length() + 4);
      return border + "\n  " + title + "\n" + border;
    }
  }

  private String formatSectionHeader(final String title) {
    if (useColors) {
      return colorize(title, ConsoleColors.BOLD + ConsoleColors.BLUE);
    } else {
      return title + "\n" + "-".repeat(title.length());
    }
  }

  private String formatExecutiveSummary(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(formatSectionHeader("Executive Summary"));
    sb.append("\n\n");

    final ComparisonSummary summary = report.getSummary();

    sb.append(String.format("Report ID: %s\n", report.getReportId()));
    sb.append(String.format("Duration: %s\n", formatDuration(report.getTotalDuration())));
    sb.append(
        String.format(
            "Tests: %d total, %d passed, %d failed\n",
            summary.getTotalTests(), summary.getPassedTests(), summary.getFailedTests()));

    final String verdict = colorizeVerdict(summary.getOverallVerdict().toString());
    sb.append(String.format("Overall Verdict: %s\n", verdict));

    if (summary.getHighPriorityIssueCount() > 0) {
      sb.append(
          String.format(
              "High Priority Issues: %s\n",
              colorize(String.valueOf(summary.getHighPriorityIssueCount()), ConsoleColors.RED)));
    }

    sb.append(
        String.format(
            "Average Compatibility Score: %.2f\n", summary.getAverageCompatibilityScore()));
    sb.append(String.format("Completed: %s\n", report.getEndTime().format(TIME_FORMATTER)));

    return sb.toString();
  }

  private String formatPerformanceSummary(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(formatSectionHeader("Performance Summary"));
    sb.append("\n\n");

    int significantDifferences = 0;
    double avgTimingRatio = 0.0;
    int timingComparisons = 0;

    for (final var entry : report.getPerformanceResults().entrySet()) {
      final PerformanceAnalyzer.PerformanceComparisonResult perf = entry.getValue();
      if (perf.hasSignificantDifferences()) {
        significantDifferences++;
      }
      avgTimingRatio += perf.getAverageTimingRatio();
      timingComparisons++;
    }

    if (timingComparisons > 0) {
      avgTimingRatio /= timingComparisons;
    }

    sb.append(
        String.format(
            "Tests with performance differences: %d/%d\n",
            significantDifferences, report.getPerformanceResults().size()));
    sb.append(String.format("Average timing ratio: %.2fx\n", avgTimingRatio));

    return sb.toString();
  }

  private String formatCoverageSummary(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(formatSectionHeader("Coverage Summary"));
    sb.append("\n\n");

    double totalCoverage = 0.0;
    int coverageCount = 0;

    for (final var entry : report.getCoverageResults().entrySet()) {
      final CoverageAnalysisResult coverage = entry.getValue();
      totalCoverage += coverage.getOverallCoveragePercentage();
      coverageCount++;
    }

    if (coverageCount > 0) {
      final double avgCoverage = totalCoverage / coverageCount;
      sb.append(String.format("Average API coverage: %.1f%%\n", avgCoverage));

      final String coverageColor =
          avgCoverage >= 90
              ? ConsoleColors.GREEN
              : avgCoverage >= 70 ? ConsoleColors.YELLOW : ConsoleColors.RED;
      sb.append(
          String.format(
              "Coverage status: %s\n", colorize(getCoverageStatus(avgCoverage), coverageColor)));
    }

    return sb.toString();
  }

  private String formatDetailedAnalysis(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(formatSectionHeader("Detailed Analysis"));
    sb.append("\n\n");

    // Sample of behavioral discrepancies
    sb.append("Sample Behavioral Discrepancies:\n");
    int discrepancyCount = 0;
    for (final var entry : report.getBehavioralResults().entrySet()) {
      if (discrepancyCount >= 5) break; // Limit to first 5 for brevity

      final BehavioralAnalysisResult result = entry.getValue();
      for (final BehavioralDiscrepancy discrepancy : result.getDiscrepancies()) {
        if (discrepancyCount >= 5) break;

        sb.append(
            String.format(
                "- %s: %s (%s)\n",
                entry.getKey(),
                discrepancy.getDescription(),
                colorize(discrepancy.getSeverity().toString(), ConsoleColors.YELLOW)));
        discrepancyCount++;
      }
    }

    if (discrepancyCount == 0) {
      sb.append("No behavioral discrepancies found.\n");
    }

    return sb.toString();
  }

  private String formatFooter(final ComparisonReport report) {
    final StringBuilder sb = new StringBuilder();
    sb.append(TABLE_BORDER.repeat(DEFAULT_TABLE_WIDTH));
    sb.append("\n");
    sb.append(
        String.format(
            "Generated by Wasmtime4j Comparison Tool at %s\n",
            report.getEndTime().format(TIME_FORMATTER)));
    sb.append(
        String.format("Total analysis time: %s\n", formatDuration(report.getTotalDuration())));
    return sb.toString();
  }

  private String formatTableHeader(final String[] headers, final int[] widths) {
    final StringBuilder sb = new StringBuilder();

    // Top border
    sb.append("┌");
    for (int i = 0; i < widths.length; i++) {
      sb.append(TABLE_BORDER.repeat(widths[i]));
      if (i < widths.length - 1) {
        sb.append("┬");
      }
    }
    sb.append("┐\n");

    // Header row
    sb.append(TABLE_VERTICAL);
    for (int i = 0; i < headers.length; i++) {
      sb.append(String.format(" %-" + (widths[i] - 2) + "s ", headers[i]));
      sb.append(TABLE_VERTICAL);
    }
    sb.append("\n");

    // Separator
    sb.append("├");
    for (int i = 0; i < widths.length; i++) {
      sb.append(TABLE_BORDER.repeat(widths[i]));
      if (i < widths.length - 1) {
        sb.append("┼");
      }
    }
    sb.append("┤\n");

    return sb.toString();
  }

  private String formatTableRow(final String[] row, final int[] widths) {
    final StringBuilder sb = new StringBuilder();
    sb.append(TABLE_VERTICAL);
    for (int i = 0; i < row.length; i++) {
      final String cellContent = row[i] != null ? row[i] : "";
      sb.append(String.format(" %-" + (widths[i] - 2) + "s ", cellContent));
      sb.append(TABLE_VERTICAL);
    }
    sb.append("\n");
    return sb.toString();
  }

  private String formatTableFooter(final int[] widths) {
    final StringBuilder sb = new StringBuilder();
    sb.append("└");
    for (int i = 0; i < widths.length; i++) {
      sb.append(TABLE_BORDER.repeat(widths[i]));
      if (i < widths.length - 1) {
        sb.append("┴");
      }
    }
    sb.append("┘\n");
    return sb.toString();
  }

  private String colorize(final String text, final String color) {
    return useColors ? ConsoleColors.colorize(text, color) : text;
  }

  private String colorizeStatus(final String status) {
    if (!useColors) {
      return status;
    }

    return switch (status.toLowerCase()) {
      case "running", "in progress" -> ConsoleColors.info(status);
      case "passed", "success", "completed" -> ConsoleColors.success(status);
      case "failed", "error" -> ConsoleColors.error(status);
      case "warning" -> ConsoleColors.warning(status);
      default -> status;
    };
  }

  private String colorizeVerdict(final String verdict) {
    if (!useColors) {
      return verdict;
    }

    return switch (verdict.toUpperCase()) {
      case "PASSED", "CONSISTENT" -> ConsoleColors.success(verdict);
      case "PASSED_WITH_WARNINGS", "MOSTLY_CONSISTENT" -> ConsoleColors.warning(verdict);
      case "FAILED_WITH_ISSUES", "INCONSISTENT" -> ConsoleColors.error(verdict);
      case "FAILED", "INCOMPATIBLE" -> ConsoleColors.colorize(
          verdict, ConsoleColors.BOLD + ConsoleColors.RED);
      default -> verdict;
    };
  }

  private String getCoverageStatus(final double coverage) {
    if (coverage >= 90) {
      return "Excellent";
    } else if (coverage >= 80) {
      return "Good";
    } else if (coverage >= 70) {
      return "Fair";
    } else {
      return "Poor";
    }
  }

  private String formatDuration(final java.time.Duration duration) {
    final long seconds = duration.getSeconds();
    final long hours = seconds / 3600;
    final long minutes = (seconds % 3600) / 60;
    final long secs = seconds % 60;
    final long millis = duration.toMillis() % 1000;

    if (hours > 0) {
      return String.format("%dh %dm %ds", hours, minutes, secs);
    } else if (minutes > 0) {
      return String.format("%dm %ds", minutes, secs);
    } else if (secs > 0) {
      return String.format("%d.%03ds", secs, millis);
    } else {
      return String.format("%dms", millis);
    }
  }

  private String truncateString(final String str, final int maxLength) {
    if (str == null || str.length() <= maxLength) {
      return str;
    }
    return str.substring(0, maxLength - 3) + "...";
  }
}
