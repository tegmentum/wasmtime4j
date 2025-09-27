package ai.tegmentum.wasmtime4j.testsuite;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Reporter for generating WebAssembly test suite reports in multiple formats.
 */
public final class TestReporter {

    private static final Logger LOGGER = Logger.getLogger(TestReporter.class.getName());

    private final TestSuiteConfiguration configuration;
    private final ObjectMapper objectMapper;

    public TestReporter(final TestSuiteConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.configuration = configuration;
        this.objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Generates console report for immediate feedback.
     *
     * @param results test execution results
     * @param analysisReport analysis report
     */
    public void generateConsoleReport(final TestExecutionResults results,
                                    final TestAnalysisReport analysisReport) {
        LOGGER.info("Generating console report");

        final StringBuilder report = new StringBuilder();
        report.append("\n");
        report.append("=====================================\n");
        report.append("WebAssembly Test Suite Results\n");
        report.append("=====================================\n");

        // Overall summary
        report.append(String.format("Total Tests: %d\n", results.getTotalTestCount()));
        report.append(String.format("Passed: %d\n", results.getTotalPassedCount()));
        report.append(String.format("Failed: %d\n", results.getTotalFailedCount()));
        report.append(String.format("Pass Rate: %.1f%%\n", results.getPassRate()));
        report.append(String.format("Execution Time: %d ms\n", results.getTotalExecutionTimeMs()));

        // Runtime breakdown
        report.append("\nRuntime Results:\n");
        for (final TestRuntime runtime : results.getRuntimeResults().keySet()) {
            final var runtimeResults = results.getResultsForRuntime(runtime);
            final long passed = runtimeResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            final long failed = runtimeResults.stream().mapToLong(r -> r.isFailure() ? 1 : 0).sum();

            report.append(String.format("  %s: %d tests (%d passed, %d failed)\n",
                runtime.getDisplayName(), runtimeResults.size(), passed, failed));
        }

        // Analysis summary
        if (analysisReport != null) {
            report.append("\nAnalysis Summary:\n");

            if (analysisReport.getCrossRuntimeAnalysis() != null &&
                analysisReport.getCrossRuntimeAnalysis().hasDiscrepancies()) {
                report.append("  Cross-runtime discrepancies detected: ")
                      .append(analysisReport.getCrossRuntimeAnalysis().getDiscrepancies().size())
                      .append("\n");
            }

            if (analysisReport.getPerformanceAnalysis() != null &&
                analysisReport.getPerformanceAnalysis().hasSignificantRegressions()) {
                report.append("  Performance issues detected: ")
                      .append(analysisReport.getPerformanceAnalysis().getPerformanceIssues().size())
                      .append("\n");
            }

            if (analysisReport.getCoverageAnalysis() != null &&
                analysisReport.getCoverageAnalysis().hasCoverageGaps()) {
                report.append("  Coverage gaps detected: ")
                      .append(analysisReport.getCoverageAnalysis().getCoverageGaps().size())
                      .append("\n");
            }
        }

        report.append("=====================================\n");

        System.out.println(report.toString());
    }

    /**
     * Generates detailed HTML dashboard report.
     *
     * @param results test execution results
     * @param analysisReport analysis report
     * @return path to generated HTML report
     * @throws TestSuiteException if report generation fails
     */
    public Path generateHtmlReport(final TestExecutionResults results,
                                 final TestAnalysisReport analysisReport) throws TestSuiteException {
        LOGGER.info("Generating HTML report");

        try {
            final Path outputDir = configuration.getOutputDirectory();
            Files.createDirectories(outputDir);

            final Path htmlReportPath = outputDir.resolve("test-report.html");

            final StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n");
            html.append("<html>\n");
            html.append("<head>\n");
            html.append("  <title>WebAssembly Test Suite Report</title>\n");
            html.append("  <meta charset=\"UTF-8\">\n");
            html.append("  <style>\n");
            html.append(generateHtmlStyles());
            html.append("  </style>\n");
            html.append("</head>\n");
            html.append("<body>\n");

            html.append("  <div class=\"container\">\n");
            html.append("    <h1>WebAssembly Test Suite Report</h1>\n");
            html.append("    <div class=\"timestamp\">Generated: ").append(
                results.getExecutionEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</div>\n");

            // Summary section
            html.append("    <div class=\"summary\">\n");
            html.append("      <h2>Summary</h2>\n");
            html.append("      <table>\n");
            html.append("        <tr><td>Total Tests:</td><td>").append(results.getTotalTestCount()).append("</td></tr>\n");
            html.append("        <tr><td>Passed:</td><td class=\"passed\">").append(results.getTotalPassedCount()).append("</td></tr>\n");
            html.append("        <tr><td>Failed:</td><td class=\"failed\">").append(results.getTotalFailedCount()).append("</td></tr>\n");
            html.append("        <tr><td>Pass Rate:</td><td>").append(String.format("%.1f%%", results.getPassRate())).append("</td></tr>\n");
            html.append("        <tr><td>Execution Time:</td><td>").append(results.getTotalExecutionTimeMs()).append(" ms</td></tr>\n");
            html.append("      </table>\n");
            html.append("    </div>\n");

            // Runtime results
            html.append("    <div class=\"runtime-results\">\n");
            html.append("      <h2>Runtime Results</h2>\n");
            html.append("      <table>\n");
            html.append("        <tr><th>Runtime</th><th>Total</th><th>Passed</th><th>Failed</th><th>Pass Rate</th></tr>\n");

            for (final TestRuntime runtime : results.getRuntimeResults().keySet()) {
                final var runtimeResults = results.getResultsForRuntime(runtime);
                final long passed = runtimeResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                final long failed = runtimeResults.stream().mapToLong(r -> r.isFailure() ? 1 : 0).sum();
                final double passRate = (double) passed / runtimeResults.size() * 100.0;

                html.append("        <tr>\n");
                html.append("          <td>").append(runtime.getDisplayName()).append("</td>\n");
                html.append("          <td>").append(runtimeResults.size()).append("</td>\n");
                html.append("          <td class=\"passed\">").append(passed).append("</td>\n");
                html.append("          <td class=\"failed\">").append(failed).append("</td>\n");
                html.append("          <td>").append(String.format("%.1f%%", passRate)).append("</td>\n");
                html.append("        </tr>\n");
            }

            html.append("      </table>\n");
            html.append("    </div>\n");

            // Analysis results if available
            if (analysisReport != null) {
                html.append(generateAnalysisHtmlSection(analysisReport));
            }

            html.append("  </div>\n");
            html.append("</body>\n");
            html.append("</html>\n");

            Files.writeString(htmlReportPath, html.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return htmlReportPath;

        } catch (final IOException e) {
            throw new TestSuiteException("Failed to generate HTML report", e);
        }
    }

    /**
     * Generates JSON report for CI integration.
     *
     * @param results test execution results
     * @param analysisReport analysis report
     * @return path to generated JSON report
     * @throws TestSuiteException if report generation fails
     */
    public Path generateJsonReport(final TestExecutionResults results,
                                 final TestAnalysisReport analysisReport) throws TestSuiteException {
        LOGGER.info("Generating JSON report");

        try {
            final Path outputDir = configuration.getOutputDirectory();
            Files.createDirectories(outputDir);

            final Path jsonReportPath = outputDir.resolve("test-report.json");

            final Map<String, Object> reportData = new HashMap<>();
            reportData.put("summary", createSummaryData(results));
            reportData.put("runtimeResults", createRuntimeResultsData(results));

            if (analysisReport != null) {
                reportData.put("analysis", createAnalysisData(analysisReport));
            }

            objectMapper.writeValue(jsonReportPath.toFile(), reportData);

            return jsonReportPath;

        } catch (final IOException e) {
            throw new TestSuiteException("Failed to generate JSON report", e);
        }
    }

    /**
     * Generates XML report for test frameworks.
     *
     * @param results test execution results
     * @param analysisReport analysis report
     * @return path to generated XML report
     * @throws TestSuiteException if report generation fails
     */
    public Path generateXmlReport(final TestExecutionResults results,
                                final TestAnalysisReport analysisReport) throws TestSuiteException {
        LOGGER.info("Generating XML report");

        try {
            final Path outputDir = configuration.getOutputDirectory();
            Files.createDirectories(outputDir);

            final Path xmlReportPath = outputDir.resolve("test-report.xml");

            final StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<testsuiteResults>\n");
            xml.append("  <summary>\n");
            xml.append("    <totalTests>").append(results.getTotalTestCount()).append("</totalTests>\n");
            xml.append("    <passedTests>").append(results.getTotalPassedCount()).append("</passedTests>\n");
            xml.append("    <failedTests>").append(results.getTotalFailedCount()).append("</failedTests>\n");
            xml.append("    <passRate>").append(String.format("%.1f", results.getPassRate())).append("</passRate>\n");
            xml.append("    <executionTime>").append(results.getTotalExecutionTimeMs()).append("</executionTime>\n");
            xml.append("  </summary>\n");

            xml.append("  <runtimeResults>\n");
            for (final TestRuntime runtime : results.getRuntimeResults().keySet()) {
                final var runtimeResults = results.getResultsForRuntime(runtime);
                final long passed = runtimeResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
                final long failed = runtimeResults.stream().mapToLong(r -> r.isFailure() ? 1 : 0).sum();

                xml.append("    <runtime name=\"").append(escapeXml(runtime.getDisplayName())).append("\">\n");
                xml.append("      <totalTests>").append(runtimeResults.size()).append("</totalTests>\n");
                xml.append("      <passedTests>").append(passed).append("</passedTests>\n");
                xml.append("      <failedTests>").append(failed).append("</failedTests>\n");
                xml.append("    </runtime>\n");
            }
            xml.append("  </runtimeResults>\n");

            xml.append("</testsuiteResults>\n");

            Files.writeString(xmlReportPath, xml.toString(),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            return xmlReportPath;

        } catch (final IOException e) {
            throw new TestSuiteException("Failed to generate XML report", e);
        }
    }

    private String generateHtmlStyles() {
        return """
            body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background: #f5f5f5; }
            .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
            h1 { color: #333; border-bottom: 2px solid #007acc; padding-bottom: 10px; }
            h2 { color: #555; margin-top: 30px; }
            .timestamp { color: #666; font-size: 14px; margin-bottom: 20px; }
            table { width: 100%; border-collapse: collapse; margin: 15px 0; }
            th, td { padding: 10px; text-align: left; border-bottom: 1px solid #ddd; }
            th { background-color: #f8f9fa; font-weight: bold; }
            .passed { color: #28a745; font-weight: bold; }
            .failed { color: #dc3545; font-weight: bold; }
            .summary table { max-width: 400px; }
            .analysis { margin-top: 30px; }
            .analysis ul { margin: 10px 0; padding-left: 20px; }
            """;
    }

    private String generateAnalysisHtmlSection(final TestAnalysisReport analysisReport) {
        final StringBuilder html = new StringBuilder();
        html.append("    <div class=\"analysis\">\n");
        html.append("      <h2>Analysis Results</h2>\n");

        if (analysisReport.getCrossRuntimeAnalysis() != null &&
            analysisReport.getCrossRuntimeAnalysis().hasDiscrepancies()) {
            html.append("      <h3>Cross-Runtime Discrepancies</h3>\n");
            html.append("      <ul>\n");
            for (final String discrepancy : analysisReport.getCrossRuntimeAnalysis().getDiscrepancies()) {
                html.append("        <li>").append(escapeHtml(discrepancy)).append("</li>\n");
            }
            html.append("      </ul>\n");
        }

        if (analysisReport.getInsights() != null && analysisReport.getInsights().hasInsights()) {
            html.append("      <h3>Insights</h3>\n");
            html.append("      <ul>\n");
            for (final String insight : analysisReport.getInsights().getInsights()) {
                html.append("        <li>").append(escapeHtml(insight)).append("</li>\n");
            }
            html.append("      </ul>\n");
        }

        html.append("    </div>\n");
        return html.toString();
    }

    private Map<String, Object> createSummaryData(final TestExecutionResults results) {
        final Map<String, Object> summary = new HashMap<>();
        summary.put("totalTests", results.getTotalTestCount());
        summary.put("passedTests", results.getTotalPassedCount());
        summary.put("failedTests", results.getTotalFailedCount());
        summary.put("passRate", results.getPassRate());
        summary.put("executionTime", results.getTotalExecutionTimeMs());
        summary.put("executionStart", results.getExecutionStartTime());
        summary.put("executionEnd", results.getExecutionEndTime());
        return summary;
    }

    private Map<String, Object> createRuntimeResultsData(final TestExecutionResults results) {
        final Map<String, Object> runtimeData = new HashMap<>();

        for (final TestRuntime runtime : results.getRuntimeResults().keySet()) {
            final var runtimeResults = results.getResultsForRuntime(runtime);
            final long passed = runtimeResults.stream().mapToLong(r -> r.isSuccess() ? 1 : 0).sum();
            final long failed = runtimeResults.stream().mapToLong(r -> r.isFailure() ? 1 : 0).sum();

            final Map<String, Object> runtimeInfo = new HashMap<>();
            runtimeInfo.put("totalTests", runtimeResults.size());
            runtimeInfo.put("passedTests", passed);
            runtimeInfo.put("failedTests", failed);
            runtimeInfo.put("passRate", (double) passed / runtimeResults.size() * 100.0);

            runtimeData.put(runtime.getId(), runtimeInfo);
        }

        return runtimeData;
    }

    private Map<String, Object> createAnalysisData(final TestAnalysisReport analysisReport) {
        final Map<String, Object> analysisData = new HashMap<>();

        if (analysisReport.getCrossRuntimeAnalysis() != null) {
            final Map<String, Object> crossRuntimeData = new HashMap<>();
            crossRuntimeData.put("hasDiscrepancies", analysisReport.getCrossRuntimeAnalysis().hasDiscrepancies());
            crossRuntimeData.put("discrepancies", analysisReport.getCrossRuntimeAnalysis().getDiscrepancies());
            analysisData.put("crossRuntime", crossRuntimeData);
        }

        if (analysisReport.getInsights() != null) {
            final Map<String, Object> insightsData = new HashMap<>();
            insightsData.put("insights", analysisReport.getInsights().getInsights());
            insightsData.put("recommendations", analysisReport.getInsights().getRecommendations());
            analysisData.put("insights", insightsData);
        }

        return analysisData;
    }

    private String escapeHtml(final String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "<")
                   .replace(">", ">")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }

    private String escapeXml(final String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "<")
                   .replace(">", ">")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}