package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.InsightAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RuntimeComparison;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * JSON data exporter providing structured output with schema versioning for API consumption.
 *
 * <p>Generates JSON output suitable for:
 *
 * <ul>
 *   <li>REST API responses and integration
 *   <li>Data processing pipelines and ETL
 *   <li>Business intelligence and analytics tools
 *   <li>CI/CD pipeline integration and reporting
 * </ul>
 *
 * <p>Supports streaming output for large datasets and optional compression for bandwidth
 * efficiency.
 *
 * @since 1.0.0
 */
public final class JsonReporter implements DataExporter<JsonConfiguration> {
  private static final Logger LOGGER = Logger.getLogger(JsonReporter.class.getName());

  private static final String SCHEMA_VERSION = "1.0.0";
  private static final String SCHEMA_DEFINITION =
      """
      {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "title": "Wasmtime4j Comparison Report",
        "type": "object",
        "required": ["schema", "metadata", "summary", "results"],
        "properties": {
          "schema": {
            "type": "object",
            "properties": {
              "version": {"type": "string"},
              "format": {"type": "string"}
            }
          },
          "metadata": {
            "type": "object",
            "properties": {
              "suiteName": {"type": "string"},
              "generatedAt": {"type": "string", "format": "date-time"},
              "runtimesCompared": {"type": "array", "items": {"type": "string"}}
            }
          },
          "results": {
            "type": "object",
            "patternProperties": {
              ".*": {
                "type": "object",
                "properties": {
                  "behavioral": {"type": "object"},
                  "performance": {"type": "object"},
                  "coverage": {"type": "object"},
                  "recommendations": {"type": "object"}
                }
              }
            }
          }
        }
      }
      """;

  private final ExportSchema schema;

  public JsonReporter() {
    this.schema =
        new ExportSchema(
            ExportFormat.JSON,
            SCHEMA_VERSION,
            "Wasmtime4j comparison report in JSON format",
            SCHEMA_DEFINITION);
  }

  @Override
  public void export(
      final ComparisonReport report,
      final JsonConfiguration configuration,
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

        if (configuration.isStreamingMode()) {
          exportStreamingJson(report, configuration, writer);
        } else {
          exportBufferedJson(report, configuration, writer);
        }

        writer.flush();
      }

      if (configuration.isCompressOutput()) {
        ((GZIPOutputStream) finalOutput).finish();
      }

    } catch (final IOException e) {
      throw new ExportException("Failed to export JSON report", e, ExportFormat.JSON, "export");
    }

    LOGGER.info(
        String.format(
            "Exported JSON report for %d tests in %s mode",
            report.getTestCount(), configuration.isStreamingMode() ? "streaming" : "buffered"));
  }

  @Override
  public ExportFormat getFormat() {
    return ExportFormat.JSON;
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
      final ComparisonReport report, final JsonConfiguration configuration) {
    // Rough estimate: 50KB base + 10KB per test + 5KB per recommendation
    final long baseSize = 50_000;
    final long testSize = report.getTestCount() * 10_000L;
    final long recommendationSize =
        report.getRecommendations().values().stream()
                .mapToInt(r -> r.getPrioritizedRecommendations().size())
                .sum()
            * 5_000L;

    return baseSize + testSize + recommendationSize;
  }

  /** Exports JSON using streaming approach for large datasets. */
  private void exportStreamingJson(
      final ComparisonReport report,
      final JsonConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    writer.write("{\n");

    // Schema information
    writeSchemaSection(writer, configuration);
    writer.write(",\n");

    // Metadata
    writeMetadataSection(report, writer, configuration);
    writer.write(",\n");

    // Summary
    writeSummarySection(report, writer, configuration);
    writer.write(",\n");

    // Results - stream one test at a time
    writer.write("  \"results\": {\n");
    final List<String> testNames = report.getTestNames();
    for (int i = 0; i < testNames.size(); i++) {
      final String testName = testNames.get(i);
      writeTestResults(report, testName, writer, configuration);
      if (i < testNames.size() - 1) {
        writer.write(",");
      }
      writer.write("\n");
    }
    writer.write("  }\n");

    writer.write("}\n");
  }

  /** Exports JSON using buffered approach for smaller datasets. */
  private void exportBufferedJson(
      final ComparisonReport report,
      final JsonConfiguration configuration,
      final BufferedWriter writer)
      throws IOException {

    // Build complete JSON structure in memory then write
    final StringBuilder json = new StringBuilder();
    json.append("{\n");

    // Schema
    json.append(buildSchemaSection(configuration));
    json.append(",\n");

    // Metadata
    json.append(buildMetadataSection(report, configuration));
    json.append(",\n");

    // Summary
    json.append(buildSummarySection(report, configuration));
    json.append(",\n");

    // Results
    json.append("  \"results\": {\n");
    final List<String> testNames = report.getTestNames();
    for (int i = 0; i < testNames.size(); i++) {
      final String testName = testNames.get(i);
      json.append(buildTestResults(report, testName, configuration));
      if (i < testNames.size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }
    json.append("  }\n");

    json.append("}\n");

    writer.write(json.toString());
  }

  private void writeSchemaSection(final BufferedWriter writer, final JsonConfiguration configuration)
      throws IOException {
    if (configuration.isIncludeMetadata()) {
      writer.write(buildSchemaSection(configuration));
    } else {
      writer.write("  \"schema\": {\"version\": \"" + SCHEMA_VERSION + "\"}");
    }
  }

  private String buildSchemaSection(final JsonConfiguration configuration) {
    if (configuration.isIncludeMetadata()) {
      return String.format(
          "  \"schema\": {\n"
              + "    \"version\": \"%s\",\n"
              + "    \"format\": \"json\",\n"
              + "    \"description\": \"%s\"\n"
              + "  }",
          SCHEMA_VERSION, schema.getDescription());
    } else {
      return "  \"schema\": {\"version\": \"" + SCHEMA_VERSION + "\"}";
    }
  }

  private void writeMetadataSection(
      final ComparisonReport report,
      final BufferedWriter writer,
      final JsonConfiguration configuration)
      throws IOException {
    writer.write(buildMetadataSection(report, configuration));
  }

  private String buildMetadataSection(
      final ComparisonReport report, final JsonConfiguration configuration) {
    final ReportMetadata metadata = report.getMetadata();
    final StringBuilder sb = new StringBuilder();

    sb.append("  \"metadata\": {\n");
    sb.append("    \"suiteName\": \"").append(escapeJson(metadata.getSuiteName())).append("\",\n");
    sb.append("    \"version\": \"").append(escapeJson(metadata.getVersion())).append("\",\n");
    sb.append("    \"generatedAt\": \"")
        .append(metadata.getGeneratedAt().format(DateTimeFormatter.ISO_INSTANT))
        .append("\",\n");
    sb.append("    \"executionDuration\": \"")
        .append(metadata.getExecutionDuration().toString())
        .append("\",\n");
    sb.append("    \"runtimesCompared\": [")
        .append(
            metadata.getRuntimesCompared().stream()
                .map(r -> "\"" + escapeJson(r) + "\"")
                .collect(Collectors.joining(", ")))
        .append("],\n");
    sb.append("    \"generatorVersion\": \"")
        .append(escapeJson(metadata.getGeneratorVersion()))
        .append("\"");

    if (configuration.isIncludeMetadata() && !metadata.getConfiguration().isEmpty()) {
      sb.append(",\n    \"configuration\": {\n");
      final List<Map.Entry<String, String>> entries =
          metadata.getConfiguration().entrySet().stream().toList();
      for (int i = 0; i < entries.size(); i++) {
        final Map.Entry<String, String> entry = entries.get(i);
        sb.append("      \"")
            .append(escapeJson(entry.getKey()))
            .append("\": \"")
            .append(escapeJson(entry.getValue()))
            .append("\"");
        if (i < entries.size() - 1) {
          sb.append(",");
        }
        sb.append("\n");
      }
      sb.append("    }");
    }

    sb.append("\n  }");
    return sb.toString();
  }

  private void writeSummarySection(
      final ComparisonReport report,
      final BufferedWriter writer,
      final JsonConfiguration configuration)
      throws IOException {
    writer.write(buildSummarySection(report, configuration));
  }

  private String buildSummarySection(
      final ComparisonReport report, final JsonConfiguration configuration) {
    final ReportSummary summary = report.getSummary();
    final StringBuilder sb = new StringBuilder();

    sb.append("  \"summary\": {\n");
    sb.append("    \"totalTests\": ").append(summary.getTotalTests()).append(",\n");
    sb.append("    \"testsWithBehavioralAnalysis\": ")
        .append(summary.getTestsWithBehavioralAnalysis())
        .append(",\n");
    sb.append("    \"testsWithPerformanceAnalysis\": ")
        .append(summary.getTestsWithPerformanceAnalysis())
        .append(",\n");
    sb.append("    \"testsWithCoverageAnalysis\": ")
        .append(summary.getTestsWithCoverageAnalysis())
        .append(",\n");
    sb.append("    \"testsWithRecommendations\": ")
        .append(summary.getTestsWithRecommendations())
        .append(",\n");
    sb.append("    \"highPriorityRecommendations\": ")
        .append(summary.getHighPriorityRecommendations())
        .append(",\n");
    sb.append("    \"overallCompatibilityScore\": ")
        .append(summary.getOverallCompatibilityScore())
        .append(",\n");
    sb.append("    \"verdictCounts\": {\n");

    final List<Map.Entry<String, Integer>> verdictEntries =
        summary.getVerdictCounts().entrySet().stream().toList();
    for (int i = 0; i < verdictEntries.size(); i++) {
      final Map.Entry<String, Integer> entry = verdictEntries.get(i);
      sb.append("      \"")
          .append(escapeJson(entry.getKey()))
          .append("\": ")
          .append(entry.getValue());
      if (i < verdictEntries.size() - 1) {
        sb.append(",");
      }
      sb.append("\n");
    }
    sb.append("    }\n");
    sb.append("  }");

    return sb.toString();
  }

  private void writeTestResults(
      final ComparisonReport report,
      final String testName,
      final BufferedWriter writer,
      final JsonConfiguration configuration)
      throws IOException {
    writer.write(buildTestResults(report, testName, configuration));
  }

  private String buildTestResults(
      final ComparisonReport report, final String testName, final JsonConfiguration configuration) {
    final StringBuilder sb = new StringBuilder();
    sb.append("    \"").append(escapeJson(testName)).append("\": {\n");

    boolean hasContent = false;

    // Behavioral analysis
    if (report.getBehavioralResults().containsKey(testName)) {
      if (hasContent) {
        sb.append(",\n");
      }
      sb.append(buildBehavioralAnalysis(report.getBehavioralResults().get(testName), configuration));
      hasContent = true;
    }

    // Performance analysis
    if (report.getPerformanceResults().containsKey(testName)) {
      if (hasContent) {
        sb.append(",\n");
      }
      sb.append(buildPerformanceAnalysis(report.getPerformanceResults().get(testName), configuration));
      hasContent = true;
    }

    // Coverage analysis
    if (report.getCoverageResults().containsKey(testName)) {
      if (hasContent) {
        sb.append(",\n");
      }
      sb.append(buildCoverageAnalysis(report.getCoverageResults().get(testName), configuration));
      hasContent = true;
    }

    // Recommendations
    if (report.getRecommendations().containsKey(testName)) {
      if (hasContent) {
        sb.append(",\n");
      }
      sb.append(buildRecommendations(report.getRecommendations().get(testName), configuration));
      hasContent = true;
    }

    // Insights
    if (report.getInsights().containsKey(testName)) {
      if (hasContent) {
        sb.append(",\n");
      }
      sb.append(buildInsights(report.getInsights().get(testName), configuration));
    }

    sb.append("\n    }");
    return sb.toString();
  }

  private String buildBehavioralAnalysis(
      final BehavioralAnalysisResult result, final JsonConfiguration configuration) {
    final StringBuilder sb = new StringBuilder();
    sb.append("      \"behavioral\": {\n");
    sb.append("        \"verdict\": \"").append(escapeJson(result.getVerdict().name())).append("\",\n");
    sb.append("        \"consistencyScore\": ").append(result.getConsistencyScore()).append(",\n");
    sb.append("        \"discrepancyCount\": ").append(result.getDiscrepancies().size()).append(",\n");
    sb.append("        \"criticalDiscrepancies\": ").append(result.getCriticalDiscrepancyCount());

    if (configuration.getDetailLevel() == JsonDetailLevel.DETAILED) {
      sb.append(",\n        \"pairwiseComparisons\": [\n");
      final List<RuntimeComparison> comparisons = result.getPairwiseComparisons();
      for (int i = 0; i < comparisons.size(); i++) {
        final RuntimeComparison comp = comparisons.get(i);
        sb.append("          {\n");
        sb.append("            \"runtime1\": \"").append(escapeJson(comp.getRuntime1().name())).append("\",\n");
        sb.append("            \"runtime2\": \"").append(escapeJson(comp.getRuntime2().name())).append("\",\n");
        sb.append("            \"score\": ").append(comp.getComparisonResult().getOverallScore()).append("\n");
        sb.append("          }");
        if (i < comparisons.size() - 1) {
          sb.append(",");
        }
        sb.append("\n");
      }
      sb.append("        ]");

      if (!result.getDiscrepancies().isEmpty()) {
        sb.append(",\n        \"discrepancies\": [\n");
        final List<BehavioralDiscrepancy> discrepancies = result.getDiscrepancies();
        for (int i = 0; i < discrepancies.size(); i++) {
          final BehavioralDiscrepancy disc = discrepancies.get(i);
          sb.append("          {\n");
          sb.append("            \"type\": \"").append(escapeJson(disc.getType().name())).append("\",\n");
          sb.append("            \"severity\": \"").append(escapeJson(disc.getSeverity().name())).append("\",\n");
          sb.append("            \"description\": \"").append(escapeJson(disc.getDescription())).append("\"\n");
          sb.append("          }");
          if (i < discrepancies.size() - 1) {
            sb.append(",");
          }
          sb.append("\n");
        }
        sb.append("        ]");
      }
    }

    sb.append("\n      }");
    return sb.toString();
  }

  private String buildPerformanceAnalysis(
      final PerformanceAnalyzer.PerformanceComparisonResult result, final JsonConfiguration configuration) {
    final StringBuilder sb = new StringBuilder();
    sb.append("      \"performance\": {\n");
    sb.append("        \"executionTimeMs\": ").append(result.getExecutionDuration().toMillis()).append(",\n");
    sb.append("        \"memoryUsedBytes\": ").append(result.getMemoryUsed()).append(",\n");
    sb.append("        \"successful\": ").append(result.isSuccessful());

    if (configuration.getDetailLevel() == JsonDetailLevel.DETAILED) {
      sb.append(",\n        \"peakMemoryUsage\": ").append(result.getPeakMemoryUsage());
      if (result.getErrorMessage() != null) {
        sb.append(",\n        \"errorMessage\": \"").append(escapeJson(result.getErrorMessage())).append("\"");
      }
    }

    sb.append("\n      }");
    return sb.toString();
  }

  private String buildCoverageAnalysis(
      final CoverageAnalysisResult result, final JsonConfiguration configuration) {
    final StringBuilder sb = new StringBuilder();
    sb.append("      \"coverage\": {\n");
    sb.append("        \"coverageScore\": ").append(result.getCoverageScore()).append(",\n");
    sb.append("        \"featuresImplemented\": ").append(result.getFeaturesImplemented()).append(",\n");
    sb.append("        \"totalFeatures\": ").append(result.getTotalFeatures());

    if (configuration.getDetailLevel() == JsonDetailLevel.DETAILED) {
      sb.append(",\n        \"missingFeatures\": [");
      final List<String> missing = result.getMissingFeatures();
      for (int i = 0; i < missing.size(); i++) {
        sb.append("\"").append(escapeJson(missing.get(i))).append("\"");
        if (i < missing.size() - 1) {
          sb.append(", ");
        }
      }
      sb.append("]");
    }

    sb.append("\n      }");
    return sb.toString();
  }

  private String buildRecommendations(
      final RecommendationResult result, final JsonConfiguration configuration) {
    final StringBuilder sb = new StringBuilder();
    sb.append("      \"recommendations\": {\n");
    sb.append("        \"totalRecommendations\": ").append(result.getSummary().getTotalRecommendations()).append(",\n");
    sb.append("        \"highPriorityCount\": ").append(result.getSummary().getHighPriorityCount());

    if (configuration.getDetailLevel() == JsonDetailLevel.DETAILED) {
      sb.append(",\n        \"prioritizedRecommendations\": [\n");
      final List<ActionableRecommendation> recommendations = result.getPrioritizedRecommendations();
      for (int i = 0; i < recommendations.size(); i++) {
        final ActionableRecommendation rec = recommendations.get(i);
        sb.append("          {\n");
        sb.append("            \"title\": \"").append(escapeJson(rec.getTitle())).append("\",\n");
        sb.append("            \"category\": \"").append(escapeJson(rec.getCategory().name())).append("\",\n");
        sb.append("            \"severity\": \"").append(escapeJson(rec.getSeverity().name())).append("\",\n");
        sb.append("            \"priorityScore\": ").append(rec.getPriorityScore()).append("\n");
        sb.append("          }");
        if (i < recommendations.size() - 1) {
          sb.append(",");
        }
        sb.append("\n");
      }
      sb.append("        ]");
    }

    sb.append("\n      }");
    return sb.toString();
  }

  private String buildInsights(
      final InsightAnalysisResult result, final JsonConfiguration configuration) {
    final StringBuilder sb = new StringBuilder();
    sb.append("      \"insights\": {\n");
    sb.append("        \"insightCount\": ").append(result.getInsights().size()).append(",\n");
    sb.append("        \"confidenceScore\": ").append(result.getConfidenceScore());
    sb.append("\n      }");
    return sb.toString();
  }

  /** Escapes special characters for JSON strings. */
  private String escapeJson(final String input) {
    if (input == null) {
      return "";
    }
    return input.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}

/** Configuration for JSON export operations. */
final class JsonConfiguration extends ExportConfiguration {
  private final JsonDetailLevel detailLevel;
  private final boolean streamingMode;
  private final boolean prettyPrint;

  private JsonConfiguration(final Builder builder) {
    super(builder.format, builder.includeMetadata, builder.compressOutput, builder.bufferSize);
    this.detailLevel = builder.detailLevel;
    this.streamingMode = builder.streamingMode;
    this.prettyPrint = builder.prettyPrint;
  }

  public JsonDetailLevel getDetailLevel() {
    return detailLevel;
  }

  public boolean isStreamingMode() {
    return streamingMode;
  }

  public boolean isPrettyPrint() {
    return prettyPrint;
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

    final JsonConfiguration that = (JsonConfiguration) obj;
    return streamingMode == that.streamingMode
        && prettyPrint == that.prettyPrint
        && detailLevel == that.detailLevel;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), detailLevel, streamingMode, prettyPrint);
  }

  /** Builder for JsonConfiguration. */
  public static final class Builder {
    private final ExportFormat format = ExportFormat.JSON;
    private boolean includeMetadata = true;
    private boolean compressOutput = false;
    private int bufferSize = 8192;
    private JsonDetailLevel detailLevel = JsonDetailLevel.SUMMARY;
    private boolean streamingMode = false;
    private boolean prettyPrint = false;

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

    public Builder detailLevel(final JsonDetailLevel detailLevel) {
      this.detailLevel = Objects.requireNonNull(detailLevel, "detailLevel cannot be null");
      return this;
    }

    public Builder streamingMode(final boolean streamingMode) {
      this.streamingMode = streamingMode;
      return this;
    }

    public Builder prettyPrint(final boolean prettyPrint) {
      this.prettyPrint = prettyPrint;
      return this;
    }

    public JsonConfiguration build() {
      return new JsonConfiguration(this);
    }
  }
}

/** Detail levels for JSON export. */
enum JsonDetailLevel {
  /** Summary information only. */
  SUMMARY,

  /** Detailed analysis results. */
  DETAILED,

  /** Complete raw data. */
  RAW
}