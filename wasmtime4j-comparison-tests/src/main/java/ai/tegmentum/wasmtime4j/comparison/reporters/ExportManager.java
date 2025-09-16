package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Comprehensive export manager that provides multiple export formats for comparison
 * results, including individual test exports, bulk exports, and bundled packages.
 * Supports asynchronous export operations and streaming for large datasets.
 *
 * @since 1.0.0
 */
public final class ExportManager {
  private static final Logger LOGGER = Logger.getLogger(ExportManager.class.getName());
  private static final DateTimeFormatter FILENAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

  private final ExportConfiguration configuration;
  private final HtmlReporter htmlReporter;
  private final JsonReporter jsonReporter;
  private final CsvReporter csvReporter;

  public ExportManager(final ExportConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.htmlReporter = new HtmlReporter(createHtmlReporterConfiguration());
    this.jsonReporter = new JsonReporter(createJsonReporterConfiguration());
    this.csvReporter = new CsvReporter(createCsvReporterConfiguration());
  }

  /**
   * Exports a complete comparison report in the specified format.
   *
   * @param report the comparison report to export
   * @param format the export format
   * @param outputPath the output file path
   * @throws IOException if the export fails
   */
  public void exportReport(final ComparisonReport report, final ExportFormat format, final Path outputPath)
      throws IOException {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    LOGGER.info("Exporting report " + report.getReportId() + " to " + format + ": " + outputPath);
    final long startTime = System.currentTimeMillis();

    try {
      // Ensure output directory exists
      if (outputPath.getParent() != null) {
        Files.createDirectories(outputPath.getParent());
      }

      // Export based on format
      switch (format) {
        case HTML -> htmlReporter.generateReport(report, outputPath);
        case JSON -> {
          try (final OutputStream outputStream = Files.newOutputStream(outputPath)) {
            jsonReporter.generateReport(report, outputStream);
          }
        }
        case CSV -> {
          try (final OutputStream outputStream = Files.newOutputStream(outputPath)) {
            csvReporter.generateReport(report, outputStream);
          }
        }
        case BUNDLE -> exportBundle(report, outputPath);
        case PDF -> exportPdf(report, outputPath);
      }

      final long duration = System.currentTimeMillis() - startTime;
      LOGGER.info("Export completed in " + duration + "ms: " + outputPath);

    } catch (final IOException e) {
      LOGGER.log(Level.SEVERE, "Export failed: " + e.getMessage(), e);
      throw e;
    }
  }

  /**
   * Exports a comparison report asynchronously.
   *
   * @param report the comparison report to export
   * @param format the export format
   * @param outputPath the output file path
   * @return CompletableFuture that completes when export is finished
   */
  public CompletableFuture<Void> exportReportAsync(
      final ComparisonReport report,
      final ExportFormat format,
      final Path outputPath) {
    return CompletableFuture.runAsync(() -> {
      try {
        exportReport(report, format, outputPath);
      } catch (final IOException e) {
        throw new RuntimeException("Async export failed", e);
      }
    });
  }

  /**
   * Exports individual test results for detailed analysis.
   *
   * @param testResult the test comparison result to export
   * @param format the export format
   * @param outputPath the output file path
   * @throws IOException if the export fails
   */
  public void exportTestResult(
      final TestComparisonResult testResult,
      final ExportFormat format,
      final Path outputPath) throws IOException {
    Objects.requireNonNull(testResult, "testResult cannot be null");
    Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    LOGGER.fine("Exporting individual test result: " + testResult.getTestName());

    // Create a minimal report for this single test
    final ComparisonReport singleTestReport = createSingleTestReport(testResult);

    exportReport(singleTestReport, format, outputPath);
  }

  /**
   * Exports filtered results based on filter criteria.
   *
   * @param report the complete comparison report
   * @param filterResult the filtered results
   * @param format the export format
   * @param outputPath the output file path
   * @throws IOException if the export fails
   */
  public void exportFilteredResults(
      final ComparisonReport report,
      final FilterResult filterResult,
      final ExportFormat format,
      final Path outputPath) throws IOException {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(filterResult, "filterResult cannot be null");
    Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    LOGGER.info("Exporting filtered results (" + filterResult.getFilteredResults().size() + " tests)");

    // Create a filtered report
    final ComparisonReport filteredReport = createFilteredReport(report, filterResult);

    exportReport(filteredReport, format, outputPath);
  }

  /**
   * Exports a summary report with key metrics and insights.
   *
   * @param report the comparison report
   * @param outputPath the output file path
   * @throws IOException if the export fails
   */
  public void exportSummary(final ComparisonReport report, final Path outputPath) throws IOException {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    LOGGER.info("Exporting summary report: " + outputPath);

    final String summaryContent = createSummaryContent(report);

    // Write summary to file
    Files.writeString(outputPath, summaryContent);
  }

  /**
   * Exports a comprehensive bundle with multiple formats.
   *
   * @param report the comparison report to export
   * @param outputPath the output ZIP file path
   * @throws IOException if the export fails
   */
  private void exportBundle(final ComparisonReport report, final Path outputPath) throws IOException {
    try (final ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(outputPath))) {

      // Export HTML report
      final ByteArrayOutputStream htmlOutput = new ByteArrayOutputStream();
      htmlReporter.generateReport(report, htmlOutput);
      addZipEntry(zipOut, "report.html", htmlOutput.toByteArray());

      // Export JSON report
      final ByteArrayOutputStream jsonOutput = new ByteArrayOutputStream();
      jsonReporter.generateReport(report, jsonOutput);
      addZipEntry(zipOut, "report.json", jsonOutput.toByteArray());

      // Export CSV report
      final ByteArrayOutputStream csvOutput = new ByteArrayOutputStream();
      csvReporter.generateReport(report, csvOutput);
      addZipEntry(zipOut, "report.csv", csvOutput.toByteArray());

      // Export executive summary
      final String summary = createSummaryContent(report);
      addZipEntry(zipOut, "executive-summary.txt", summary.getBytes());

      // Export individual test results
      exportIndividualTests(report, zipOut);

      // Export metadata
      final String metadata = createMetadataContent(report);
      addZipEntry(zipOut, "metadata.json", metadata.getBytes());
    }
  }

  /**
   * Exports individual test results to the ZIP bundle.
   *
   * @param report the comparison report
   * @param zipOut the ZIP output stream
   * @throws IOException if the export fails
   */
  private void exportIndividualTests(final ComparisonReport report, final ZipOutputStream zipOut)
      throws IOException {

    final String testsDir = "tests/";

    for (final TestComparisonResult testResult : report.getTestResults()) {
      final String safeTestName = sanitizeFileName(testResult.getTestName());

      // Export test as JSON
      final ByteArrayOutputStream testJsonOutput = new ByteArrayOutputStream();
      final ComparisonReport singleTestReport = createSingleTestReport(testResult);
      jsonReporter.generateReport(singleTestReport, testJsonOutput);
      addZipEntry(zipOut, testsDir + safeTestName + ".json", testJsonOutput.toByteArray());

      // Export detailed comparison view
      final ComparisonViewBuilder viewBuilder = new ComparisonViewBuilder();
      final Map<String, Object> comparisonView = viewBuilder.createSideBySideComparison(testResult);
      final String comparisonJson = jsonReporter.serializeToJson(comparisonView);
      addZipEntry(zipOut, testsDir + safeTestName + "-comparison.json", comparisonJson.getBytes());
    }
  }

  /**
   * Exports a report as PDF (placeholder implementation).
   *
   * @param report the comparison report
   * @param outputPath the output PDF path
   * @throws IOException if the export fails
   */
  private void exportPdf(final ComparisonReport report, final Path outputPath) throws IOException {
    // This would use a PDF generation library like iText or PDFBox
    // For now, we'll create a simple text-based PDF export
    final String pdfContent = createPdfContent(report);
    Files.writeString(outputPath, pdfContent);

    LOGGER.info("PDF export completed (simplified text format): " + outputPath);
  }

  /**
   * Creates a single-test report from a test comparison result.
   *
   * @param testResult the test comparison result
   * @return comparison report containing only this test
   */
  private ComparisonReport createSingleTestReport(final TestComparisonResult testResult) {
    // Create minimal metadata
    final ComparisonMetadata metadata = new ComparisonMetadata(
        "Single Test Export",
        "1.0.0",
        testResult.getRuntimeResults().keySet(),
        Map.of("exportType", "singleTest"),
        "1.0.0"
    );

    // Create minimal execution summary
    final ExecutionSummary executionSummary = new ExecutionSummary(
        1,
        testResult.getOverallStatus() == TestResultStatus.SUCCESS ? 1 : 0,
        testResult.getOverallStatus() != TestResultStatus.SUCCESS ? 1 : 0,
        0,
        java.time.Duration.ofMillis(testResult.getRuntimeResults().values().stream()
            .mapToLong(r -> r.getExecutionTime().toMillis())
            .sum()),
        java.time.Instant.now().minusSeconds(60),
        java.time.Instant.now()
    );

    // Create minimal coverage report (placeholder)
    final ComprehensiveCoverageReport coverageReport = createMinimalCoverageReport();

    // Create minimal performance summary
    final PerformanceAnalysisSummary performanceSummary = createMinimalPerformanceSummary(testResult);

    // Create minimal statistics
    final ReportStatistics statistics = new ReportStatistics(
        1, 1024, Map.of("singleTest", 1), 100.0
    );

    return new ComparisonReport.Builder("single-test-" + testResult.getTestName())
        .metadata(metadata)
        .executionSummary(executionSummary)
        .testResults(List.of(testResult))
        .coverageReport(coverageReport)
        .performanceSummary(performanceSummary)
        .behavioralDiscrepancies(testResult.getDiscrepancies())
        .recommendations(List.of())
        .statistics(statistics)
        .build();
  }

  /**
   * Creates a filtered report from filter results.
   *
   * @param originalReport the original comparison report
   * @param filterResult the filter results
   * @return filtered comparison report
   */
  private ComparisonReport createFilteredReport(
      final ComparisonReport originalReport,
      final FilterResult filterResult) {

    // Use original metadata but update test count
    final Map<String, String> updatedEnvInfo = new HashMap<>(originalReport.getMetadata().getEnvironmentInfo());
    updatedEnvInfo.put("filterApplied", "true");
    updatedEnvInfo.put("originalTestCount", String.valueOf(originalReport.getTestResults().size()));
    updatedEnvInfo.put("filteredTestCount", String.valueOf(filterResult.getFilteredResults().size()));

    final ComparisonMetadata metadata = new ComparisonMetadata(
        originalReport.getMetadata().getTestSuiteName() + " (Filtered)",
        originalReport.getMetadata().getTestSuiteVersion(),
        originalReport.getMetadata().getRuntimeTypes(),
        updatedEnvInfo,
        originalReport.getMetadata().getWasmtime4jVersion()
    );

    // Update execution summary
    final int filteredCount = filterResult.getFilteredResults().size();
    final int successCount = (int) filterResult.getFilteredResults().stream()
        .mapToLong(test -> test.getOverallStatus() == TestResultStatus.SUCCESS ? 1 : 0)
        .sum();
    final int failureCount = filteredCount - successCount;

    final ExecutionSummary executionSummary = new ExecutionSummary(
        filteredCount,
        successCount,
        failureCount,
        0,
        originalReport.getExecutionSummary().getTotalDuration(),
        originalReport.getExecutionSummary().getStartTime(),
        originalReport.getExecutionSummary().getEndTime()
    );

    // Create filtered statistics
    final ReportStatistics statistics = new ReportStatistics(
        filteredCount,
        originalReport.getStatistics().getReportSizeBytes(),
        filterResult.getMetadata().entrySet().stream()
            .filter(entry -> entry.getValue() instanceof Integer)
            .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), (Integer) entry.getValue()), HashMap::putAll),
        originalReport.getStatistics().getDataQualityScore()
    );

    return new ComparisonReport.Builder("filtered-" + originalReport.getReportId())
        .metadata(metadata)
        .executionSummary(executionSummary)
        .testResults(filterResult.getFilteredResults())
        .coverageReport(originalReport.getCoverageReport())
        .performanceSummary(originalReport.getPerformanceSummary())
        .behavioralDiscrepancies(filterResult.getFilteredResults().stream()
            .flatMap(test -> test.getDiscrepancies().stream())
            .toList())
        .recommendations(originalReport.getRecommendations())
        .statistics(statistics)
        .build();
  }

  /**
   * Creates summary content for text export.
   *
   * @param report the comparison report
   * @return formatted summary content
   */
  private String createSummaryContent(final ComparisonReport report) {
    final StringBuilder summary = new StringBuilder();

    summary.append("WASMTIME4J COMPARISON REPORT SUMMARY\n");
    summary.append("====================================\n\n");

    summary.append("Report ID: ").append(report.getReportId()).append("\n");
    summary.append("Generated: ").append(report.getGeneratedAt()).append("\n");
    summary.append("Test Suite: ").append(report.getMetadata().getTestSuiteName()).append("\n");
    summary.append("Version: ").append(report.getMetadata().getTestSuiteVersion()).append("\n");
    summary.append("Runtimes: ").append(report.getMetadata().getRuntimeTypes()).append("\n\n");

    // Execution summary
    summary.append("EXECUTION SUMMARY\n");
    summary.append("-----------------\n");
    final ExecutionSummary exec = report.getExecutionSummary();
    summary.append("Total Tests: ").append(exec.getTotalTests()).append("\n");
    summary.append("Successful: ").append(exec.getSuccessfulTests()).append("\n");
    summary.append("Failed: ").append(exec.getFailedTests()).append("\n");
    summary.append("Success Rate: ").append(String.format("%.1f%%", exec.getSuccessRate())).append("\n");
    summary.append("Duration: ").append(exec.getTotalDuration()).append("\n\n");

    // Critical issues
    summary.append("CRITICAL ISSUES\n");
    summary.append("---------------\n");
    summary.append("Tests with Critical Issues: ").append(report.getCriticalTestResults().size()).append("\n");
    summary.append("Behavioral Discrepancies: ").append(report.getBehavioralDiscrepancies().size()).append("\n");
    summary.append("High-Priority Recommendations: ").append(
        report.getRecommendations().stream().mapToLong(r -> r.getHighPriorityRecommendations().size()).sum()).append("\n\n");

    // Coverage analysis
    summary.append("COVERAGE ANALYSIS\n");
    summary.append("-----------------\n");
    summary.append("Overall Coverage: ").append(String.format("%.1f%%", report.getCoverageReport().getOverallCoverageScore())).append("\n");
    summary.append("Uncovered Features: ").append(report.getCoverageReport().getUncoveredFeatures().size()).append("\n\n");

    // Performance summary
    summary.append("PERFORMANCE SUMMARY\n");
    summary.append("-------------------\n");
    final PerformanceAnalysisSummary perf = report.getPerformanceSummary();
    summary.append("Average Variance: ").append(String.format("%.2fx", perf.getAveragePerformanceVariance())).append("\n");
    summary.append("Maximum Variance: ").append(String.format("%.2fx", perf.getMaxPerformanceVariance())).append("\n");
    summary.append("Outlier Tests: ").append(perf.getOutlierTests().size()).append("\n\n");

    return summary.toString();
  }

  /**
   * Creates metadata content for JSON export.
   *
   * @param report the comparison report
   * @return JSON metadata content
   */
  private String createMetadataContent(final ComparisonReport report) {
    final Map<String, Object> metadata = new HashMap<>();

    metadata.put("reportId", report.getReportId());
    metadata.put("generatedAt", report.getGeneratedAt().toString());
    metadata.put("exportedAt", java.time.Instant.now().toString());
    metadata.put("exportConfiguration", configuration);
    metadata.put("metadata", report.getMetadata());
    metadata.put("statistics", report.getStatistics());

    try {
      return jsonReporter.serializeToJson(metadata);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to serialize metadata", e);
      return "{}";
    }
  }

  /**
   * Creates PDF content (simplified text-based format).
   *
   * @param report the comparison report
   * @return PDF content as text
   */
  private String createPdfContent(final ComparisonReport report) {
    // This is a simplified PDF export - a real implementation would use PDF libraries
    return "PDF Export (Text Format)\n" +
           "=========================\n\n" +
           createSummaryContent(report) +
           "\n\nNote: This is a simplified text-based PDF export. " +
           "A full implementation would use PDF generation libraries.";
  }

  /**
   * Creates minimal coverage report for single test exports.
   *
   * @return minimal coverage report
   */
  private ComprehensiveCoverageReport createMinimalCoverageReport() {
    // Placeholder implementation - would be more sophisticated in real usage
    return new ComprehensiveCoverageReport(
        Map.of("test", 100.0),
        List.of(),
        Map.of(),
        List.of(),
        new ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageTrend(100.0, 100.0, 0.0,
            ai.tegmentum.wasmtime4j.comparison.analyzers.TrendDirection.STABLE),
        1,
        java.time.Instant.now()
    );
  }

  /**
   * Creates minimal performance summary for single test exports.
   *
   * @param testResult the test result
   * @return minimal performance summary
   */
  private PerformanceAnalysisSummary createMinimalPerformanceSummary(final TestComparisonResult testResult) {
    final Map<ai.tegmentum.wasmtime4j.RuntimeType, Double> runtimeScores = new HashMap<>();
    testResult.getRuntimeResults().forEach((runtime, result) ->
        runtimeScores.put(runtime, (double) result.getExecutionTime().toMillis()));

    return new PerformanceAnalysisSummary(
        1.0, 1.0, runtimeScores, List.of(), Map.of()
    );
  }

  /**
   * Creates HTML reporter configuration.
   *
   * @return HTML reporter configuration
   */
  private HtmlReporterConfiguration createHtmlReporterConfiguration() {
    return HtmlReporterConfiguration.builder()
        .reportTitle(configuration.getReportTitle())
        .theme(configuration.getTheme())
        .enableInteractiveFeatures(configuration.isIncludeInteractiveFeatures())
        .enablePerformanceCharts(configuration.isIncludeCharts())
        .enableCoverageAnalysis(configuration.isIncludeCoverageAnalysis())
        .includeStaticResources(configuration.isIncludeStaticResources())
        .verbosityLevel(configuration.getVerbosityLevel())
        .build();
  }

  /**
   * Creates JSON reporter configuration.
   *
   * @return JSON reporter configuration
   */
  private JsonReporterConfiguration createJsonReporterConfiguration() {
    return JsonReporterConfiguration.builder()
        .prettyPrint(configuration.isPrettyPrintJson())
        .includeMetadata(configuration.isIncludeMetadata())
        .includeRawData(configuration.isIncludeRawData())
        .verbosityLevel(configuration.getVerbosityLevel())
        .build();
  }

  /**
   * Creates CSV reporter configuration.
   *
   * @return CSV reporter configuration
   */
  private CsvReporterConfiguration createCsvReporterConfiguration() {
    return CsvReporterConfiguration.builder()
        .includeHeaders(configuration.isIncludeHeaders())
        .delimiter(configuration.getCsvDelimiter())
        .verbosityLevel(configuration.getVerbosityLevel())
        .build();
  }

  /**
   * Adds an entry to a ZIP output stream.
   *
   * @param zipOut the ZIP output stream
   * @param entryName the entry name
   * @param data the entry data
   * @throws IOException if the entry cannot be added
   */
  private void addZipEntry(final ZipOutputStream zipOut, final String entryName, final byte[] data)
      throws IOException {
    final ZipEntry entry = new ZipEntry(entryName);
    zipOut.putNextEntry(entry);
    zipOut.write(data);
    zipOut.closeEntry();
  }

  /**
   * Sanitizes a filename for safe use in file systems.
   *
   * @param filename the original filename
   * @return sanitized filename
   */
  private String sanitizeFileName(final String filename) {
    return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
  }

  /**
   * Gets the current export configuration.
   *
   * @return export configuration
   */
  public ExportConfiguration getConfiguration() {
    return configuration;
  }
}

/** Export format enumeration. */
enum ExportFormat {
  /** HTML report format. */
  HTML("html"),

  /** JSON data format. */
  JSON("json"),

  /** CSV spreadsheet format. */
  CSV("csv"),

  /** PDF document format. */
  PDF("pdf"),

  /** Bundle with multiple formats. */
  BUNDLE("zip");

  private final String fileExtension;

  ExportFormat(final String fileExtension) {
    this.fileExtension = fileExtension;
  }

  public String getFileExtension() {
    return fileExtension;
  }
}

/** Configuration for export operations. */
final class ExportConfiguration {
  private final String reportTitle;
  private final String theme;
  private final boolean includeStaticResources;
  private final boolean includeInteractiveFeatures;
  private final boolean includeCharts;
  private final boolean includeCoverageAnalysis;
  private final boolean includeMetadata;
  private final boolean includeRawData;
  private final boolean includeHeaders;
  private final boolean prettyPrintJson;
  private final String csvDelimiter;
  private final VerbosityLevel verbosityLevel;

  private ExportConfiguration(final Builder builder) {
    this.reportTitle = Objects.requireNonNull(builder.reportTitle, "reportTitle cannot be null");
    this.theme = Objects.requireNonNull(builder.theme, "theme cannot be null");
    this.includeStaticResources = builder.includeStaticResources;
    this.includeInteractiveFeatures = builder.includeInteractiveFeatures;
    this.includeCharts = builder.includeCharts;
    this.includeCoverageAnalysis = builder.includeCoverageAnalysis;
    this.includeMetadata = builder.includeMetadata;
    this.includeRawData = builder.includeRawData;
    this.includeHeaders = builder.includeHeaders;
    this.prettyPrintJson = builder.prettyPrintJson;
    this.csvDelimiter = Objects.requireNonNull(builder.csvDelimiter, "csvDelimiter cannot be null");
    this.verbosityLevel = Objects.requireNonNull(builder.verbosityLevel, "verbosityLevel cannot be null");
  }

  public String getReportTitle() {
    return reportTitle;
  }

  public String getTheme() {
    return theme;
  }

  public boolean isIncludeStaticResources() {
    return includeStaticResources;
  }

  public boolean isIncludeInteractiveFeatures() {
    return includeInteractiveFeatures;
  }

  public boolean isIncludeCharts() {
    return includeCharts;
  }

  public boolean isIncludeCoverageAnalysis() {
    return includeCoverageAnalysis;
  }

  public boolean isIncludeMetadata() {
    return includeMetadata;
  }

  public boolean isIncludeRawData() {
    return includeRawData;
  }

  public boolean isIncludeHeaders() {
    return includeHeaders;
  }

  public boolean isPrettyPrintJson() {
    return prettyPrintJson;
  }

  public String getCsvDelimiter() {
    return csvDelimiter;
  }

  public VerbosityLevel getVerbosityLevel() {
    return verbosityLevel;
  }

  /** Creates a default export configuration. */
  public static ExportConfiguration defaultConfiguration() {
    return builder().build();
  }

  /** Creates a new builder for export configuration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ExportConfiguration. */
  public static final class Builder {
    private String reportTitle = "Wasmtime4j Comparison Report";
    private String theme = "default";
    private boolean includeStaticResources = true;
    private boolean includeInteractiveFeatures = true;
    private boolean includeCharts = true;
    private boolean includeCoverageAnalysis = true;
    private boolean includeMetadata = true;
    private boolean includeRawData = false;
    private boolean includeHeaders = true;
    private boolean prettyPrintJson = true;
    private String csvDelimiter = ",";
    private VerbosityLevel verbosityLevel = VerbosityLevel.NORMAL;

    public Builder reportTitle(final String reportTitle) {
      this.reportTitle = Objects.requireNonNull(reportTitle, "reportTitle cannot be null");
      return this;
    }

    public Builder theme(final String theme) {
      this.theme = Objects.requireNonNull(theme, "theme cannot be null");
      return this;
    }

    public Builder includeStaticResources(final boolean includeStaticResources) {
      this.includeStaticResources = includeStaticResources;
      return this;
    }

    public Builder includeInteractiveFeatures(final boolean includeInteractiveFeatures) {
      this.includeInteractiveFeatures = includeInteractiveFeatures;
      return this;
    }

    public Builder includeCharts(final boolean includeCharts) {
      this.includeCharts = includeCharts;
      return this;
    }

    public Builder includeCoverageAnalysis(final boolean includeCoverageAnalysis) {
      this.includeCoverageAnalysis = includeCoverageAnalysis;
      return this;
    }

    public Builder includeMetadata(final boolean includeMetadata) {
      this.includeMetadata = includeMetadata;
      return this;
    }

    public Builder includeRawData(final boolean includeRawData) {
      this.includeRawData = includeRawData;
      return this;
    }

    public Builder includeHeaders(final boolean includeHeaders) {
      this.includeHeaders = includeHeaders;
      return this;
    }

    public Builder prettyPrintJson(final boolean prettyPrintJson) {
      this.prettyPrintJson = prettyPrintJson;
      return this;
    }

    public Builder csvDelimiter(final String csvDelimiter) {
      this.csvDelimiter = Objects.requireNonNull(csvDelimiter, "csvDelimiter cannot be null");
      return this;
    }

    public Builder verbosityLevel(final VerbosityLevel verbosityLevel) {
      this.verbosityLevel = Objects.requireNonNull(verbosityLevel, "verbosityLevel cannot be null");
      return this;
    }

    public ExportConfiguration build() {
      return new ExportConfiguration(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExportConfiguration that = (ExportConfiguration) obj;
    return includeStaticResources == that.includeStaticResources &&
           includeInteractiveFeatures == that.includeInteractiveFeatures &&
           includeCharts == that.includeCharts &&
           includeCoverageAnalysis == that.includeCoverageAnalysis &&
           includeMetadata == that.includeMetadata &&
           includeRawData == that.includeRawData &&
           includeHeaders == that.includeHeaders &&
           prettyPrintJson == that.prettyPrintJson &&
           Objects.equals(reportTitle, that.reportTitle) &&
           Objects.equals(theme, that.theme) &&
           Objects.equals(csvDelimiter, that.csvDelimiter) &&
           verbosityLevel == that.verbosityLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportTitle, theme, includeStaticResources, includeInteractiveFeatures,
                       includeCharts, includeCoverageAnalysis, includeMetadata, includeRawData,
                       includeHeaders, prettyPrintJson, csvDelimiter, verbosityLevel);
  }

  @Override
  public String toString() {
    return "ExportConfiguration{" +
           "title='" + reportTitle + '\'' +
           ", theme='" + theme + '\'' +
           ", verbosity=" + verbosityLevel +
           '}';
  }
}