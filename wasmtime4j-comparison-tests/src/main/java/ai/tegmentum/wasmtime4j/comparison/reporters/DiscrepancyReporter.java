package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancySeverity;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancyType;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Specialized reporter for comprehensive discrepancy analysis and behavioral difference reporting.
 * Provides detailed insights into runtime differences, regression patterns, and executive summaries
 * for zero discrepancy requirement validation.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Detailed discrepancy analysis with severity categorization
 *   <li>Executive summaries for stakeholder reporting
 *   <li>Trend analysis and regression tracking
 *   <li>Zero discrepancy requirement compliance reporting
 *   <li>Actionable recommendations for discrepancy resolution
 * </ul>
 *
 * @since 1.0.0
 */
public final class DiscrepancyReporter {
  private static final Logger LOGGER = Logger.getLogger(DiscrepancyReporter.class.getName());

  private static final String REPORT_HEADER =
      """
      ================================================================================
                            WASMTIME4J DISCREPANCY ANALYSIS REPORT
      ================================================================================
      """;

  private static final String SECTION_SEPARATOR =
      """
      --------------------------------------------------------------------------------
      """;

  /**
   * Generates a comprehensive discrepancy report.
   *
   * @param discrepancies list of detected behavioral discrepancies
   * @param testResults map of test results by test name
   * @param output the output stream to write the report to
   * @throws IOException if writing fails
   */
  public void generateDiscrepancyReport(
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<String, Map<RuntimeType, Object>> testResults,
      final OutputStream output)
      throws IOException {
    Objects.requireNonNull(discrepancies, "discrepancies cannot be null");
    Objects.requireNonNull(testResults, "testResults cannot be null");
    Objects.requireNonNull(output, "output cannot be null");

    try (final BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

      writeReportHeader(writer);
      writeExecutiveSummary(discrepancies, testResults, writer);
      writeDiscrepancyAnalysis(discrepancies, writer);
      writeZeroDiscrepancyCompliance(discrepancies, writer);
      writeTrendAnalysis(discrepancies, writer);
      writeRecommendations(discrepancies, writer);
      writeDetailedFindings(discrepancies, writer);
      writeReportFooter(writer);

      writer.flush();
    }

    LOGGER.info(
        "Generated comprehensive discrepancy report with " + discrepancies.size() + " findings");
  }

  /**
   * Generates an executive summary for stakeholder reporting.
   *
   * @param discrepancies list of detected behavioral discrepancies
   * @param testResults map of test results by test name
   * @param output the output stream to write the summary to
   * @throws IOException if writing fails
   */
  public void generateExecutiveSummary(
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<String, Map<RuntimeType, Object>> testResults,
      final OutputStream output)
      throws IOException {
    Objects.requireNonNull(discrepancies, "discrepancies cannot be null");
    Objects.requireNonNull(testResults, "testResults cannot be null");
    Objects.requireNonNull(output, "output cannot be null");

    try (final BufferedWriter writer =
        new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8))) {

      writeExecutiveSummaryHeader(writer);
      writeExecutiveSummary(discrepancies, testResults, writer);
      writeExecutiveRecommendations(discrepancies, writer);

      writer.flush();
    }

    LOGGER.info("Generated executive summary for " + discrepancies.size() + " discrepancies");
  }

  /** Writes the main report header. */
  private void writeReportHeader(final BufferedWriter writer) throws IOException {
    writer.write(REPORT_HEADER);
    writer.write("Generated: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + "\n");
    writer.write("Report Type: Comprehensive Discrepancy Analysis\n");
    writer.write(SECTION_SEPARATOR);
    writer.write("\n");
  }

  /** Writes the executive summary section. */
  private void writeExecutiveSummary(
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<String, Map<RuntimeType, Object>> testResults,
      final BufferedWriter writer)
      throws IOException {

    writer.write("EXECUTIVE SUMMARY\n");
    writer.write("=================\n\n");

    final long totalTests = testResults.size();
    final long testsWithDiscrepancies =
        discrepancies.stream().map(BehavioralDiscrepancy::getTestName).distinct().count();

    final long criticalDiscrepancies =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL).count();
    final long majorDiscrepancies =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.MAJOR).count();
    final long moderateDiscrepancies =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.MODERATE).count();

    writer.write(String.format("Total Tests Analyzed: %d%n", totalTests));
    writer.write(
        String.format(
            "Tests with Discrepancies: %d (%.1f%%)%n",
            testsWithDiscrepancies,
            totalTests > 0 ? (testsWithDiscrepancies * 100.0 / totalTests) : 0.0));
    writer.write(String.format("Total Discrepancies Found: %d%n", discrepancies.size()));
    writer.write("\n");

    writer.write("SEVERITY BREAKDOWN:\n");
    writer.write(String.format("  • Critical:  %d discrepancies%n", criticalDiscrepancies));
    writer.write(String.format("  • Major:     %d discrepancies%n", majorDiscrepancies));
    writer.write(String.format("  • Moderate:  %d discrepancies%n", moderateDiscrepancies));
    writer.write("\n");

    // Zero discrepancy requirement status
    final boolean zeroDiscrepancyCompliant = criticalDiscrepancies == 0;
    writer.write("ZERO DISCREPANCY REQUIREMENT STATUS:\n");
    writer.write(
        String.format(
            "  Status: %s%n", zeroDiscrepancyCompliant ? "✓ COMPLIANT" : "✗ NON-COMPLIANT"));

    if (!zeroDiscrepancyCompliant) {
      writer.write(
          String.format(
              "  Action Required: %d critical discrepancies must be resolved%n",
              criticalDiscrepancies));
    }

    writer.write("\n" + SECTION_SEPARATOR + "\n");
  }

  /** Writes the detailed discrepancy analysis. */
  private void writeDiscrepancyAnalysis(
      final List<BehavioralDiscrepancy> discrepancies, final BufferedWriter writer)
      throws IOException {

    writer.write("DISCREPANCY ANALYSIS\n");
    writer.write("====================\n\n");

    // Group discrepancies by type
    final Map<DiscrepancyType, List<BehavioralDiscrepancy>> byType =
        discrepancies.stream().collect(Collectors.groupingBy(BehavioralDiscrepancy::getType));

    writer.write("DISCREPANCY TYPES:\n");
    for (final Map.Entry<DiscrepancyType, List<BehavioralDiscrepancy>> entry : byType.entrySet()) {
      final DiscrepancyType type = entry.getKey();
      final List<BehavioralDiscrepancy> typeDiscrepancies = entry.getValue();

      writer.write(
          String.format("  %s: %d occurrences%n", type.getDescription(), typeDiscrepancies.size()));

      final long criticalCount =
          typeDiscrepancies.stream()
              .filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL)
              .count();
      if (criticalCount > 0) {
        writer.write(
            String.format(
                "    ⚠️  %d critical issues requiring immediate attention%n", criticalCount));
      }
    }

    writer.write("\n");

    // Group discrepancies by affected runtimes
    final Map<Set<RuntimeType>, List<BehavioralDiscrepancy>> byRuntime =
        discrepancies.stream()
            .collect(Collectors.groupingBy(BehavioralDiscrepancy::getAffectedRuntimes));

    writer.write("AFFECTED RUNTIMES:\n");
    for (final Map.Entry<Set<RuntimeType>, List<BehavioralDiscrepancy>> entry :
        byRuntime.entrySet()) {
      final Set<RuntimeType> runtimes = entry.getKey();
      final List<BehavioralDiscrepancy> runtimeDiscrepancies = entry.getValue();

      if (!runtimes.isEmpty()) {
        writer.write(
            String.format(
                "  %s: %d discrepancies%n",
                runtimes.stream().map(RuntimeType::name).collect(Collectors.joining(", ")),
                runtimeDiscrepancies.size()));
      }
    }

    writer.write("\n" + SECTION_SEPARATOR + "\n");
  }

  /** Writes the zero discrepancy compliance section. */
  private void writeZeroDiscrepancyCompliance(
      final List<BehavioralDiscrepancy> discrepancies, final BufferedWriter writer)
      throws IOException {

    writer.write("ZERO DISCREPANCY REQUIREMENT COMPLIANCE\n");
    writer.write("=======================================\n\n");

    final List<BehavioralDiscrepancy> criticalDiscrepancies =
        discrepancies.stream()
            .filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL)
            .toList();

    if (criticalDiscrepancies.isEmpty()) {
      writer.write("✓ COMPLIANCE STATUS: PASSED\n");
      writer.write(
          "All runtimes demonstrate equivalent behavior with no critical discrepancies.\n");
      writer.write("Zero discrepancy requirement is satisfied.\n");
    } else {
      writer.write("✗ COMPLIANCE STATUS: FAILED\n");
      writer.write(
          String.format("Critical discrepancies detected: %d%n", criticalDiscrepancies.size()));
      writer.write("\nCRITICAL ISSUES BLOCKING COMPLIANCE:\n");

      for (int i = 0; i < criticalDiscrepancies.size(); i++) {
        final BehavioralDiscrepancy discrepancy = criticalDiscrepancies.get(i);
        writer.write(String.format("  %d. %s%n", i + 1, discrepancy.getDescription()));
        writer.write(String.format("     Test: %s%n", discrepancy.getTestName()));
        writer.write(String.format("     Recommendation: %s%n", discrepancy.getRecommendation()));
        writer.write("\n");
      }
    }

    writer.write(SECTION_SEPARATOR + "\n");
  }

  /** Writes the trend analysis section. */
  private void writeTrendAnalysis(
      final List<BehavioralDiscrepancy> discrepancies, final BufferedWriter writer)
      throws IOException {

    writer.write("TREND ANALYSIS\n");
    writer.write("==============\n\n");

    // Count regression-related discrepancies
    final long regressionCount =
        discrepancies.stream()
            .filter(
                d ->
                    d.getRecommendation().toLowerCase().contains("regression")
                        || d.getDescription().toLowerCase().contains("regression"))
            .count();

    final long systematicCount =
        discrepancies.stream()
            .filter(d -> d.getType() == DiscrepancyType.SYSTEMATIC_PATTERN)
            .count();

    writer.write("REGRESSION PATTERNS:\n");
    if (regressionCount > 0) {
      writer.write(
          String.format("  ⚠️  %d regression-related discrepancies detected%n", regressionCount));
      writer.write("     This indicates potential degradation in runtime behavior.\n");
    } else {
      writer.write("  ✓ No regression patterns detected in current analysis.\n");
    }

    writer.write("\nSYSTEMATIC ISSUES:\n");
    if (systematicCount > 0) {
      writer.write(String.format("  ⚠️  %d systematic patterns detected%n", systematicCount));
      writer.write("     These may indicate fundamental compatibility issues.\n");
    } else {
      writer.write("  ✓ No systematic patterns detected.\n");
    }

    writer.write("\n" + SECTION_SEPARATOR + "\n");
  }

  /** Writes the recommendations section. */
  private void writeRecommendations(
      final List<BehavioralDiscrepancy> discrepancies, final BufferedWriter writer)
      throws IOException {

    writer.write("ACTIONABLE RECOMMENDATIONS\n");
    writer.write("==========================\n\n");

    final List<BehavioralDiscrepancy> criticalDiscrepancies =
        discrepancies.stream()
            .filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL)
            .toList();

    writer.write("IMMEDIATE ACTIONS (Critical Priority):\n");
    if (criticalDiscrepancies.isEmpty()) {
      writer.write("  ✓ No immediate actions required.\n");
    } else {
      for (int i = 0; i < criticalDiscrepancies.size(); i++) {
        final BehavioralDiscrepancy discrepancy = criticalDiscrepancies.get(i);
        writer.write(String.format("  %d. %s%n", i + 1, discrepancy.getRecommendation()));
        writer.write(String.format("     Context: %s%n", discrepancy.getDescription()));
      }
    }

    writer.write("\nOPTIMIZATION ACTIONS (High Priority):\n");
    final List<BehavioralDiscrepancy> majorDiscrepancies =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.MAJOR).toList();

    if (majorDiscrepancies.isEmpty()) {
      writer.write("  ✓ No high-priority optimizations required.\n");
    } else {
      for (int i = 0; i < Math.min(5, majorDiscrepancies.size()); i++) {
        final BehavioralDiscrepancy discrepancy = majorDiscrepancies.get(i);
        writer.write(String.format("  %d. %s%n", i + 1, discrepancy.getRecommendation()));
      }
      if (majorDiscrepancies.size() > 5) {
        writer.write(
            String.format(
                "     ... and %d additional optimization recommendations%n",
                majorDiscrepancies.size() - 5));
      }
    }

    writer.write("\n" + SECTION_SEPARATOR + "\n");
  }

  /** Writes the detailed findings section. */
  private void writeDetailedFindings(
      final List<BehavioralDiscrepancy> discrepancies, final BufferedWriter writer)
      throws IOException {

    writer.write("DETAILED FINDINGS\n");
    writer.write("=================\n\n");

    // Group by severity for organized presentation
    final Map<DiscrepancySeverity, List<BehavioralDiscrepancy>> bySeverity =
        discrepancies.stream().collect(Collectors.groupingBy(BehavioralDiscrepancy::getSeverity));

    for (final DiscrepancySeverity severity : DiscrepancySeverity.values()) {
      final List<BehavioralDiscrepancy> severityDiscrepancies = bySeverity.get(severity);
      if (severityDiscrepancies == null || severityDiscrepancies.isEmpty()) {
        continue;
      }

      writer.write(
          String.format(
              "%s DISCREPANCIES (%d):\n",
              severity.getDisplayName().toUpperCase(), severityDiscrepancies.size()));
      writer.write("\n");

      for (int i = 0; i < severityDiscrepancies.size(); i++) {
        final BehavioralDiscrepancy discrepancy = severityDiscrepancies.get(i);
        writer.write(String.format("%d. %s%n", i + 1, discrepancy.getDescription()));
        writer.write(String.format("   Type: %s%n", discrepancy.getType().getDescription()));
        writer.write(String.format("   Test: %s%n", discrepancy.getTestName()));
        writer.write(String.format("   Details: %s%n", discrepancy.getDetails()));
        writer.write(String.format("   Recommendation: %s%n", discrepancy.getRecommendation()));
        writer.write(
            String.format(
                "   Detected: %s%n",
                DateTimeFormatter.ISO_INSTANT.format(discrepancy.getDetectedAt())));

        if (!discrepancy.getAffectedRuntimes().isEmpty()) {
          writer.write(
              String.format(
                  "   Affected Runtimes: %s%n",
                  discrepancy.getAffectedRuntimes().stream()
                      .map(RuntimeType::name)
                      .collect(Collectors.joining(", "))));
        }
        writer.write("\n");
      }
    }

    writer.write(SECTION_SEPARATOR + "\n");
  }

  /** Writes the report footer. */
  private void writeReportFooter(final BufferedWriter writer) throws IOException {
    writer.write("END OF REPORT\n");
    writer.write("Generated by Wasmtime4j Discrepancy Analysis System\n");
    writer.write("For questions or support, refer to the project documentation.\n");
    writer.write(
        "================================================================================\n");
  }

  /** Writes the executive summary header. */
  private void writeExecutiveSummaryHeader(final BufferedWriter writer) throws IOException {
    writer.write(
        "================================================================================\n");
    writer.write("                    WASMTIME4J DISCREPANCY EXECUTIVE SUMMARY\n");
    writer.write(
        "================================================================================\n");
    writer.write("Generated: " + DateTimeFormatter.ISO_INSTANT.format(Instant.now()) + "\n");
    writer.write("Report Type: Executive Summary\n");
    writer.write(SECTION_SEPARATOR + "\n");
  }

  /** Writes executive-level recommendations. */
  private void writeExecutiveRecommendations(
      final List<BehavioralDiscrepancy> discrepancies, final BufferedWriter writer)
      throws IOException {

    writer.write("EXECUTIVE RECOMMENDATIONS\n");
    writer.write("=========================\n\n");

    final long criticalCount =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL).count();

    if (criticalCount == 0) {
      writer.write("✓ PROJECT STATUS: ON TRACK\n");
      writer.write("The zero discrepancy requirement is satisfied.\n");
      writer.write("All runtime implementations demonstrate equivalent behavior.\n\n");
      writer.write("RECOMMENDED ACTIONS:\n");
      writer.write("• Continue current testing and validation practices\n");
      writer.write("• Monitor for regression patterns in future releases\n");
      writer.write("• Consider expanding test coverage for edge cases\n");
    } else {
      writer.write("⚠️  PROJECT STATUS: REQUIRES ATTENTION\n");
      writer.write(
          String.format(
              "Critical discrepancies block zero discrepancy compliance: %d issues%n",
              criticalCount));
      writer.write("\nRECOMMENDED ACTIONS:\n");
      writer.write("• Prioritize resolution of critical discrepancies\n");
      writer.write("• Allocate additional engineering resources for compatibility fixes\n");
      writer.write("• Consider delaying release until zero discrepancy requirement is met\n");
      writer.write("• Implement automated regression detection to prevent future issues\n");
    }

    writer.write("\n" + SECTION_SEPARATOR + "\n");
  }
}
