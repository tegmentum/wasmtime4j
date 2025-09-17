package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * CSV data exporter optimized for spreadsheet analysis and data processing.
 *
 * <p>Generates CSV output suitable for:
 *
 * <ul>
 *   <li>Excel and Google Sheets analysis
 *   <li>Tableau and Power BI data visualization
 *   <li>Statistical analysis tools (R, Python pandas)
 *   <li>Database import and ETL processes
 * </ul>
 *
 * <p>Supports configurable column selection, streaming output for large datasets, and multiple CSV
 * layouts (summary, detailed, pivot tables).
 *
 * @since 1.0.0
 */
public final class CsvReporter implements DataExporter<CsvConfiguration> {
  private static final Logger LOGGER = Logger.getLogger(CsvReporter.class.getName());

  private static final String SCHEMA_VERSION = "1.0.0";
  private static final String SCHEMA_DEFINITION =
      """
      CSV Schema for Wasmtime4j Comparison Report:

      SUMMARY Format:
      - testName,verdict,consistencyScore,executionTimeMs,memoryUsedBytes,coverageScore,
        recommendationCount,highPriorityCount

      DETAILED Format:
      - testName,category,field,value,unit,description

      RECOMMENDATIONS Format:
      - testName,title,category,severity,priorityScore,description,affectedRuntimes

      PERFORMANCE Format:
      - testName,runtime,executionTimeMs,memoryUsedBytes,peakMemoryUsage,successful,errorMessage

      DISCREPANCIES Format:
      - testName,type,severity,description,runtime1,runtime2,score
      """;

  private final ExportSchema schema;

  /** Creates a new CSV reporter with default configuration. */
  public CsvReporter() {
    this.schema =
        new ExportSchema(
            ExportFormat.CSV,
            SCHEMA_VERSION,
            "Wasmtime4j comparison report in CSV format",
            SCHEMA_DEFINITION);
  }

  @Override
  public void export(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final OutputStream output)
      throws IOException, ExportException {
    validateConfiguration(configuration);
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(output, "output cannot be null");

    try {
      final OutputStream finalOutput =
          configuration.isCompressOutput() ? new GZIPOutputStream(output) : output;

      try (final BufferedWriter writer =
          new BufferedWriter(
              new OutputStreamWriter(finalOutput, StandardCharsets.UTF_8),
              configuration.getBufferSize())) {

        switch (configuration.getLayout()) {
          case SUMMARY -> exportSummaryLayout(report, configuration, writer);
          case DETAILED -> exportDetailedLayout(report, configuration, writer);
          case RECOMMENDATIONS -> exportRecommendationsLayout(report, configuration, writer);
          case PERFORMANCE -> exportPerformanceLayout(report, configuration, writer);
          case DISCREPANCIES -> exportDiscrepanciesLayout(report, configuration, writer);
          case CUSTOM -> exportCustomLayout(report, configuration, writer);
          default -> throw new IllegalArgumentException(
              "Unsupported CSV layout: " + configuration.getLayout());
        }

        writer.flush();
      }

      if (configuration.isCompressOutput()) {
        ((GZIPOutputStream) finalOutput).finish();
      }

    } catch (final IOException e) {
      throw new ExportException("Failed to export CSV report", e, ExportFormat.CSV, "export");
    }

    LOGGER.info(
        String.format(
            "Exported CSV report for %d tests in %s layout",
            report.getTestCount(), configuration.getLayout()));
  }

  @Override
  public ExportFormat getFormat() {
    return ExportFormat.CSV;
  }

  @Override
  public ExportSchema getSchema() {
    return schema;
  }

  @Override
  public boolean supportsStreaming() {
    return true;
  }

  @Override
  public long estimateOutputSize(
      final ComparisonReport report, final CsvConfiguration configuration) {
    // Rough estimate based on layout and number of tests
    final long baseSize = 1000; // Headers and metadata
    final long testCount = report.getTestCount();

    return switch (configuration.getLayout()) {
      case SUMMARY -> baseSize + (testCount * 200); // ~200 bytes per test summary
      case DETAILED -> baseSize + (testCount * 1000); // ~1KB per test detailed
      case RECOMMENDATIONS -> baseSize
          + report.getRecommendations().stream()
                  .mapToInt(r -> r.getPrioritizedRecommendations().size())
                  .sum()
              * 300; // ~300 bytes per recommendation
      case PERFORMANCE -> baseSize + (testCount * 150); // ~150 bytes per performance record
      case DISCREPANCIES -> baseSize
          + report.getBehavioralResults().values().stream()
                  .mapToInt(r -> r.getDiscrepancies().size())
                  .sum()
              * 250; // ~250 bytes per discrepancy
      case CUSTOM -> baseSize + (testCount * 500); // Conservative estimate
    };
  }

  /** Exports summary layout with key metrics per test. */
  private void exportSummaryLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    // Write header
    final List<String> headers =
        Arrays.asList(
            "testName",
            "verdict",
            "consistencyScore",
            "executionTimeMs",
            "memoryUsedBytes",
            "coverageScore",
            "recommendationCount",
            "highPriorityCount");
    writeCsvRow(writer, headers, configuration);

    // Write data rows
    for (final String testName : report.getTestNames()) {
      final List<String> row = new ArrayList<>();
      row.add(testName);

      // Behavioral verdict
      final BehavioralAnalysisResult behavioral = report.getBehavioralResults().get(testName);
      if (behavioral != null) {
        row.add(behavioral.getVerdict().name());
        row.add(String.format("%.3f", behavioral.getConsistencyScore()));
      } else {
        row.add("");
        row.add("");
      }

      // Performance metrics
      final PerformanceAnalyzer.PerformanceComparisonResult performance =
          report.getPerformanceResults().get(testName);
      if (performance != null) {
        row.add(String.valueOf(performance.getExecutionDuration()));
        row.add(String.valueOf(performance.getMemoryUsed()));
      } else {
        row.add("");
        row.add("");
      }

      // Coverage score
      final CoverageAnalysisResult coverage = report.getCoverageResults().get(testName);
      if (coverage != null) {
        row.add(String.format("%.3f", coverage.getCoverageScore()));
      } else {
        row.add("");
      }

      // Recommendations
      final RecommendationResult recommendations = report.getRecommendationResults().get(testName);
      if (recommendations != null) {
        row.add(String.valueOf(recommendations.getSummary().getTotalRecommendations()));
        row.add(String.valueOf(recommendations.getSummary().getHighPriorityCount()));
      } else {
        row.add("0");
        row.add("0");
      }

      writeCsvRow(writer, row, configuration);
    }
  }

  /** Exports detailed layout with normalized key-value structure. */
  private void exportDetailedLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    // Write header
    final List<String> headers =
        Arrays.asList("testName", "category", "field", "value", "unit", "description");
    writeCsvRow(writer, headers, configuration);

    // Write data rows for each test
    for (final String testName : report.getTestNames()) {
      writeDetailedTestData(report, testName, writer, configuration);
    }
  }

  /** Exports recommendations layout focusing on actionable recommendations. */
  private void exportRecommendationsLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    // Write header
    final List<String> headers =
        Arrays.asList(
            "testName",
            "title",
            "category",
            "severity",
            "priorityScore",
            "description",
            "affectedRuntimes");
    writeCsvRow(writer, headers, configuration);

    // Write recommendation rows
    for (final String testName : report.getTestNames()) {
      final RecommendationResult recommendations = report.getRecommendationResults().get(testName);
      if (recommendations != null) {
        for (final ActionableRecommendation rec : recommendations.getPrioritizedRecommendations()) {
          final List<String> row = new ArrayList<>();
          row.add(testName);
          row.add(rec.getTitle());
          row.add(rec.getCategory().name());
          row.add(rec.getSeverity().name());
          row.add(String.format("%.3f", rec.getPriorityScore()));
          row.add(rec.getDescription());
          row.add(
              rec.getAffectedRuntimes().stream()
                  .map(Object::toString)
                  .collect(Collectors.joining(";")));

          writeCsvRow(writer, row, configuration);
        }
      }
    }
  }

  /** Exports performance layout with runtime-specific metrics. */
  private void exportPerformanceLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    // Write header
    final List<String> headers =
        Arrays.asList(
            "testName",
            "runtime",
            "executionTimeMs",
            "memoryUsedBytes",
            "peakMemoryUsage",
            "successful",
            "errorMessage");
    writeCsvRow(writer, headers, configuration);

    // Write performance rows
    for (final String testName : report.getTestNames()) {
      final PerformanceAnalyzer.PerformanceComparisonResult performance =
          report.getPerformanceResults().get(testName);
      if (performance != null) {
        final List<String> row = new ArrayList<>();
        row.add(testName);
        row.add(performance.getRuntimeType());
        row.add(String.valueOf(performance.getExecutionDuration()));
        row.add(String.valueOf(performance.getMemoryUsed()));
        row.add(String.valueOf(performance.getPeakMemoryUsage()));
        row.add(String.valueOf(performance.isSuccessful()));
        row.add(performance.getErrorMessage() != null ? performance.getErrorMessage() : "");

        writeCsvRow(writer, row, configuration);
      }
    }
  }

  /** Exports discrepancies layout focusing on behavioral differences. */
  private void exportDiscrepanciesLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    // Write header
    final List<String> headers =
        Arrays.asList(
            "testName", "type", "severity", "description", "runtime1", "runtime2", "score");
    writeCsvRow(writer, headers, configuration);

    // Write discrepancy rows
    for (final String testName : report.getTestNames()) {
      final BehavioralAnalysisResult behavioral = report.getBehavioralResults().get(testName);
      if (behavioral != null) {
        // Discrepancies
        for (final BehavioralDiscrepancy disc : behavioral.getDiscrepancies()) {
          final List<String> row = new ArrayList<>();
          row.add(testName);
          row.add(disc.getType().name());
          row.add(disc.getSeverity().name());
          row.add(disc.getDescription());
          row.add(""); // runtime1 - not available in BehavioralDiscrepancy
          row.add(""); // runtime2 - not available in BehavioralDiscrepancy
          row.add(""); // score - not available in BehavioralDiscrepancy

          writeCsvRow(writer, row, configuration);
        }

        // TODO: Add runtime comparisons when RuntimeComparison is accessible from reporters package
      }
    }
  }

  /** Exports custom layout based on user-specified columns. */
  private void exportCustomLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException, ExportException {

    final List<String> customColumns = configuration.getCustomColumns();
    if (customColumns.isEmpty()) {
      throw new ExportException(
          "Custom layout requires custom columns to be specified",
          ExportFormat.CSV,
          "configuration");
    }

    // Write header
    writeCsvRow(writer, customColumns, configuration);

    // Write data rows
    for (final String testName : report.getTestNames()) {
      final List<String> row = new ArrayList<>();
      for (final String column : customColumns) {
        row.add(extractCustomColumnValue(report, testName, column));
      }
      writeCsvRow(writer, row, configuration);
    }
  }

  /** Writes detailed test data in key-value format. */
  private void writeDetailedTestData(
      final ComparisonReport report,
      final String testName,
      final BufferedWriter writer,
      final CsvConfiguration configuration)
      throws IOException {

    // Behavioral analysis details
    final BehavioralAnalysisResult behavioral = report.getBehavioralResults().get(testName);
    if (behavioral != null) {
      writeDetailedRow(
          writer,
          testName,
          "behavioral",
          "verdict",
          behavioral.getVerdict().name(),
          "",
          "Behavioral compatibility verdict",
          configuration);
      writeDetailedRow(
          writer,
          testName,
          "behavioral",
          "consistencyScore",
          String.format("%.3f", behavioral.getConsistencyScore()),
          "score",
          "Consistency score across runtimes",
          configuration);
      writeDetailedRow(
          writer,
          testName,
          "behavioral",
          "discrepancyCount",
          String.valueOf(behavioral.getDiscrepancies().size()),
          "count",
          "Number of behavioral discrepancies",
          configuration);
    }

    // Performance analysis details
    final PerformanceAnalyzer.PerformanceComparisonResult performance =
        report.getPerformanceResults().get(testName);
    if (performance != null) {
      writeDetailedRow(
          writer,
          testName,
          "performance",
          "executionTime",
          String.valueOf(performance.getExecutionDuration()),
          "ms",
          "Test execution duration",
          configuration);
      writeDetailedRow(
          writer,
          testName,
          "performance",
          "memoryUsed",
          String.valueOf(performance.getMemoryUsed()),
          "bytes",
          "Memory consumed during execution",
          configuration);
      writeDetailedRow(
          writer,
          testName,
          "performance",
          "successful",
          String.valueOf(performance.isSuccessful()),
          "boolean",
          "Whether execution completed successfully",
          configuration);
    }

    // Coverage analysis details
    final CoverageAnalysisResult coverage = report.getCoverageResults().get(testName);
    if (coverage != null) {
      writeDetailedRow(
          writer,
          testName,
          "coverage",
          "coverageScore",
          String.format("%.3f", coverage.getCoverageScore()),
          "score",
          "Feature coverage score",
          configuration);
      writeDetailedRow(
          writer,
          testName,
          "coverage",
          "featuresImplemented",
          String.valueOf(coverage.getFeaturesImplemented()),
          "count",
          "Number of implemented features",
          configuration);
    }
  }

  /** Writes a single detailed row in key-value format. */
  private void writeDetailedRow(
      final BufferedWriter writer,
      final String testName,
      final String category,
      final String field,
      final String value,
      final String unit,
      final String description,
      final CsvConfiguration configuration)
      throws IOException {

    final List<String> row = Arrays.asList(testName, category, field, value, unit, description);
    writeCsvRow(writer, row, configuration);
  }

  /** Extracts value for custom column specification. */
  private String extractCustomColumnValue(
      final ComparisonReport report, final String testName, final String column) {
    // Simple column mapping - could be extended with expression evaluation
    return switch (column.toLowerCase()) {
      case "testname" -> testName;
      case "verdict" -> {
        final BehavioralAnalysisResult behavioral = report.getBehavioralResults().get(testName);
        yield behavioral != null ? behavioral.getVerdict().name() : "";
      }
      case "consistencyscore" -> {
        final BehavioralAnalysisResult behavioral = report.getBehavioralResults().get(testName);
        yield behavioral != null ? String.format("%.3f", behavioral.getConsistencyScore()) : "";
      }
      case "executiontimems" -> {
        final PerformanceAnalyzer.PerformanceComparisonResult performance =
            report.getPerformanceResults().get(testName);
        yield performance != null ? String.valueOf(performance.getExecutionDuration()) : "";
      }
      case "memoryusedbytes" -> {
        final PerformanceAnalyzer.PerformanceComparisonResult performance =
            report.getPerformanceResults().get(testName);
        yield performance != null ? String.valueOf(performance.getMemoryUsed()) : "";
      }
      case "coveragescore" -> {
        final CoverageAnalysisResult coverage = report.getCoverageResults().get(testName);
        yield coverage != null ? String.format("%.3f", coverage.getCoverageScore()) : "";
      }
      case "recommendationcount" -> {
        final RecommendationResult recommendations =
            report.getRecommendationResults().get(testName);
        yield recommendations != null
            ? String.valueOf(recommendations.getSummary().getTotalRecommendations())
            : "0";
      }
      default -> ""; // Unknown column
    };
  }

  /** Writes a CSV row with proper escaping and delimiter handling. */
  private void writeCsvRow(
      final BufferedWriter writer, final List<String> values, final CsvConfiguration configuration)
      throws IOException {

    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < values.size(); i++) {
      if (i > 0) {
        sb.append(configuration.getDelimiter());
      }

      final String value = values.get(i);
      if (needsQuoting(value, configuration)) {
        sb.append(configuration.getQuoteChar());
        // Escape quote characters within the value
        sb.append(
            value.replace(
                configuration.getQuoteChar(),
                configuration.getQuoteChar() + configuration.getQuoteChar()));
        sb.append(configuration.getQuoteChar());
      } else {
        sb.append(value);
      }
    }

    writer.write(sb.toString());
    writer.write(configuration.getLineEnding());
  }

  /** Determines if a value needs to be quoted in CSV. */
  private boolean needsQuoting(final String value, final CsvConfiguration configuration) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    return value.contains(configuration.getDelimiter())
        || value.contains(configuration.getQuoteChar())
        || value.contains("\n")
        || value.contains("\r");
  }
}
