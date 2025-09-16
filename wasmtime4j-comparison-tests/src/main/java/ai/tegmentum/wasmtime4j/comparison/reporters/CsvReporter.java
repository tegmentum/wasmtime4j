package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RuntimeComparison;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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
      - testName,verdict,consistencyScore,executionTimeMs,memoryUsedBytes,coverageScore,recommendationCount,highPriorityCount

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
          + report.getRecommendations().values().stream()
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
        row.add(String.valueOf(performance.getExecutionDuration().toMillis()));
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
      final RecommendationResult recommendations = report.getRecommendations().get(testName);
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
      final RecommendationResult recommendations = report.getRecommendations().get(testName);
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
        row.add(String.valueOf(performance.getExecutionDuration().toMillis()));
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

        // Runtime comparisons
        for (final RuntimeComparison comp : behavioral.getPairwiseComparisons()) {
          final List<String> row = new ArrayList<>();
          row.add(testName);
          row.add("COMPARISON");
          row.add(comp.getComparisonResult().isEquivalent() ? "LOW" : "HIGH");
          row.add("Runtime comparison result");
          row.add(comp.getRuntime1().name());
          row.add(comp.getRuntime2().name());
          row.add(String.format("%.3f", comp.getComparisonResult().getOverallScore()));

          writeCsvRow(writer, row, configuration);
        }
      }
    }
  }

  /** Exports custom layout based on user-specified columns. */
  private void exportCustomLayout(
      final ComparisonReport report,
      final CsvConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

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
          String.valueOf(performance.getExecutionDuration().toMillis()),
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
        yield performance != null
            ? String.valueOf(performance.getExecutionDuration().toMillis())
            : "";
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
        final RecommendationResult recommendations = report.getRecommendations().get(testName);
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

/** Configuration for CSV export operations. */
final class CsvConfiguration extends ExportConfiguration {
  private final CsvLayout layout;
  private final String delimiter;
  private final String quoteChar;
  private final String lineEnding;
  private final List<String> customColumns;
  private final boolean includeHeaders;

  private CsvConfiguration(final Builder builder) {
    super(builder.format, builder.includeMetadata, builder.compressOutput, builder.bufferSize);
    this.layout = builder.layout;
    this.delimiter = builder.delimiter;
    this.quoteChar = builder.quoteChar;
    this.lineEnding = builder.lineEnding;
    this.customColumns = List.copyOf(builder.customColumns);
    this.includeHeaders = builder.includeHeaders;
  }

  public CsvLayout getLayout() {
    return layout;
  }

  public String getDelimiter() {
    return delimiter;
  }

  public String getQuoteChar() {
    return quoteChar;
  }

  public String getLineEnding() {
    return lineEnding;
  }

  public List<String> getCustomColumns() {
    return customColumns;
  }

  public boolean isIncludeHeaders() {
    return includeHeaders;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }

    final CsvConfiguration that = (CsvConfiguration) obj;
    return includeHeaders == that.includeHeaders
        && layout == that.layout
        && Objects.equals(delimiter, that.delimiter)
        && Objects.equals(quoteChar, that.quoteChar)
        && Objects.equals(lineEnding, that.lineEnding)
        && Objects.equals(customColumns, that.customColumns);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        super.hashCode(), layout, delimiter, quoteChar, lineEnding, customColumns, includeHeaders);
  }

  /** Builder for CsvConfiguration. */
  public static final class Builder {
    private final ExportFormat format = ExportFormat.CSV;
    private boolean includeMetadata = true;
    private boolean compressOutput = false;
    private int bufferSize = 8192;
    private CsvLayout layout = CsvLayout.SUMMARY;
    private String delimiter = ",";
    private String quoteChar = "\"";
    private String lineEnding = "\n";
    private Set<String> customColumns = new LinkedHashSet<>();
    private boolean includeHeaders = true;

    public Builder includeMetadata(final boolean includeMetadata) {
      this.includeMetadata = includeMetadata;
      return this;
    }

    public Builder compressOutput(final boolean compressOutput) {
      this.compressOutput = compressOutput;
      return this;
    }

    public Builder bufferSize(final int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    public Builder layout(final CsvLayout layout) {
      this.layout = Objects.requireNonNull(layout, "layout cannot be null");
      return this;
    }

    public Builder delimiter(final String delimiter) {
      this.delimiter = Objects.requireNonNull(delimiter, "delimiter cannot be null");
      return this;
    }

    public Builder quoteChar(final String quoteChar) {
      this.quoteChar = Objects.requireNonNull(quoteChar, "quoteChar cannot be null");
      return this;
    }

    public Builder lineEnding(final String lineEnding) {
      this.lineEnding = Objects.requireNonNull(lineEnding, "lineEnding cannot be null");
      return this;
    }

    public Builder customColumns(final List<String> customColumns) {
      this.customColumns = new LinkedHashSet<>(customColumns);
      return this;
    }

    public Builder addCustomColumn(final String column) {
      this.customColumns.add(Objects.requireNonNull(column, "column cannot be null"));
      return this;
    }

    public Builder includeHeaders(final boolean includeHeaders) {
      this.includeHeaders = includeHeaders;
      return this;
    }

    public CsvConfiguration build() {
      return new CsvConfiguration(this);
    }
  }
}

/** CSV layout options for different analysis needs. */
enum CsvLayout {
  /** Summary view with key metrics per test. */
  SUMMARY,

  /** Detailed view with normalized key-value structure. */
  DETAILED,

  /** Recommendations-focused view. */
  RECOMMENDATIONS,

  /** Performance-focused view. */
  PERFORMANCE,

  /** Discrepancies-focused view. */
  DISCREPANCIES,

  /** Custom view with user-specified columns. */
  CUSTOM
}
