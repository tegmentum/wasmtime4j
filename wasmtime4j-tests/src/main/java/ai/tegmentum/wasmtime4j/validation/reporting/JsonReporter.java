package ai.tegmentum.wasmtime4j.validation.reporting;

import ai.tegmentum.wasmtime4j.validation.metrics.BasicMetricsCollector;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * JSON reporter for exporting basic performance metrics.
 *
 * <p>This reporter generates JSON files containing operation metrics for easy integration with
 * CI/CD pipelines and web-based analysis tools. The format follows standard JSON conventions and is
 * designed for programmatic consumption.
 *
 * @since 1.0.0
 */
public final class JsonReporter {

  private static final Logger LOGGER = Logger.getLogger(JsonReporter.class.getName());

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  /** Private constructor to prevent instantiation. */
  private JsonReporter() {
    // Utility class - do not instantiate
  }

  /**
   * Exports metrics to a JSON file.
   *
   * @param metrics the metrics collector to export
   * @param outputPath the path where the JSON file will be written
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if metrics or outputPath is null
   */
  public static void exportToFile(final BasicMetricsCollector metrics, final Path outputPath)
      throws IOException {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    final String jsonContent = generateJsonContent(metrics);

    // Ensure parent directory exists
    final Path parentDir = outputPath.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }

    // Write JSON content to file
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      writer.write(jsonContent);
    }

    LOGGER.info("Exported metrics to JSON file: " + outputPath);
  }

  /**
   * Exports metrics to a JSON string.
   *
   * @param metrics the metrics collector to export
   * @return the JSON content as a string
   * @throws IllegalArgumentException if metrics is null
   */
  public static String exportToString(final BasicMetricsCollector metrics) {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    return generateJsonContent(metrics);
  }

  /**
   * Exports overall metrics summary to JSON.
   *
   * @param overallMetrics the overall metrics to export
   * @param outputPath the path where the JSON file will be written
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if overallMetrics or outputPath is null
   */
  public static void exportOverallMetrics(
      final BasicMetricsCollector.OverallMetrics overallMetrics, final Path outputPath)
      throws IOException {
    Objects.requireNonNull(overallMetrics, "overallMetrics cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    final String jsonContent = generateOverallMetricsJson(overallMetrics);

    // Ensure parent directory exists
    final Path parentDir = outputPath.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }

    // Write JSON content to file
    try (BufferedWriter writer =
        Files.newBufferedWriter(
            outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      writer.write(jsonContent);
    }

    LOGGER.info("Exported overall metrics to JSON file: " + outputPath);
  }

  /**
   * Exports compact metrics suitable for CI/CD status reporting.
   *
   * @param metrics the metrics collector to export
   * @return compact JSON string with essential metrics
   */
  public static String exportCompactStatus(final BasicMetricsCollector metrics) {
    Objects.requireNonNull(metrics, "metrics cannot be null");

    final BasicMetricsCollector.OverallMetrics overall = metrics.getOverallMetrics();
    final String status = overall.getOverallSuccessRate() >= 0.95 ? "PASS" : "FAIL";

    return String.format(
        "{\"status\":\"%s\",\"successRate\":%.4f,\"totalOperations\":%d,"
            + "\"successfulOperations\":%d,\"failedOperations\":%d,\"durationMs\":%d}",
        status,
        overall.getOverallSuccessRate(),
        overall.getTotalOperations(),
        overall.getSuccessfulOperations(),
        overall.getFailedOperations(),
        overall.getTotalDuration().toMillis());
  }

  @SuppressWarnings("PMD.NcssCount")
  private static String generateJsonContent(final BasicMetricsCollector metrics) {
    final StringWriter stringWriter = new StringWriter();

    try (BufferedWriter writer = new BufferedWriter(stringWriter)) {
      final String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());
      final BasicMetricsCollector.OverallMetrics overall = metrics.getOverallMetrics();
      final List<BasicMetricsCollector.OperationMetrics> allMetrics =
          metrics.getAllOperationMetrics();

      writer.write("{");
      writer.newLine();

      // Write metadata
      writer.write("  \"timestamp\": \"" + timestamp + "\",");
      writer.newLine();
      writer.write("  \"reportType\": \"BasicMetrics\",");
      writer.newLine();

      // Write overall metrics
      writer.write("  \"overall\": {");
      writer.newLine();
      writer.write("    \"totalOperations\": " + overall.getTotalOperations() + ",");
      writer.newLine();
      writer.write("    \"successfulOperations\": " + overall.getSuccessfulOperations() + ",");
      writer.newLine();
      writer.write("    \"failedOperations\": " + overall.getFailedOperations() + ",");
      writer.newLine();
      writer.write(
          "    \"overallSuccessRate\": "
              + String.format("%.4f", overall.getOverallSuccessRate())
              + ",");
      writer.newLine();
      writer.write("    \"totalDurationMs\": " + overall.getTotalDuration().toMillis() + ",");
      writer.newLine();
      writer.write("    \"uniqueOperations\": " + overall.getOperationNames().size());
      writer.newLine();
      writer.write("  },");
      writer.newLine();

      // Write operation metrics
      writer.write("  \"operations\": [");
      writer.newLine();

      for (int i = 0; i < allMetrics.size(); i++) {
        final BasicMetricsCollector.OperationMetrics operationMetrics = allMetrics.get(i);
        writer.write("    {");
        writer.newLine();
        writer.write(
            "      \"name\": \"" + escapeJsonString(operationMetrics.getOperationName()) + "\",");
        writer.newLine();
        writer.write("      \"successCount\": " + operationMetrics.getSuccessCount() + ",");
        writer.newLine();
        writer.write("      \"failureCount\": " + operationMetrics.getFailureCount() + ",");
        writer.newLine();
        writer.write("      \"totalCount\": " + operationMetrics.getTotalCount() + ",");
        writer.newLine();
        writer.write(
            "      \"successRate\": "
                + String.format("%.4f", operationMetrics.getSuccessRate())
                + ",");
        writer.newLine();
        writer.write("      \"timing\": {");
        writer.newLine();
        writer.write("        \"minTimeMs\": " + operationMetrics.getMinTime().toMillis() + ",");
        writer.newLine();
        writer.write("        \"maxTimeMs\": " + operationMetrics.getMaxTime().toMillis() + ",");
        writer.newLine();
        writer.write("        \"avgTimeMs\": " + operationMetrics.getAvgTime().toMillis());
        writer.newLine();
        writer.write("      }");
        writer.newLine();
        writer.write("    }");
        if (i < allMetrics.size() - 1) {
          writer.write(",");
        }
        writer.newLine();
      }

      writer.write("  ]");
      writer.newLine();
      writer.write("}");

    } catch (final IOException e) {
      LOGGER.warning("Error generating JSON content: " + e.getMessage());
      return "{}";
    }

    return stringWriter.toString();
  }

  private static String generateOverallMetricsJson(
      final BasicMetricsCollector.OverallMetrics overallMetrics) {
    final StringWriter stringWriter = new StringWriter();

    try (BufferedWriter writer = new BufferedWriter(stringWriter)) {
      final String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());

      writer.write("{");
      writer.newLine();
      writer.write("  \"timestamp\": \"" + timestamp + "\",");
      writer.newLine();
      writer.write("  \"reportType\": \"OverallMetricsSummary\",");
      writer.newLine();
      writer.write("  \"totalOperations\": " + overallMetrics.getTotalOperations() + ",");
      writer.newLine();
      writer.write("  \"successfulOperations\": " + overallMetrics.getSuccessfulOperations() + ",");
      writer.newLine();
      writer.write("  \"failedOperations\": " + overallMetrics.getFailedOperations() + ",");
      writer.newLine();
      writer.write(
          "  \"overallSuccessRate\": "
              + String.format("%.4f", overallMetrics.getOverallSuccessRate())
              + ",");
      writer.newLine();
      writer.write("  \"totalDurationMs\": " + overallMetrics.getTotalDuration().toMillis() + ",");
      writer.newLine();
      writer.write("  \"uniqueOperations\": " + overallMetrics.getOperationNames().size() + ",");
      writer.newLine();
      writer.write("  \"operationNames\": [");
      writer.newLine();

      final List<String> operationNames = overallMetrics.getOperationNames();
      for (int i = 0; i < operationNames.size(); i++) {
        writer.write("    \"" + escapeJsonString(operationNames.get(i)) + "\"");
        if (i < operationNames.size() - 1) {
          writer.write(",");
        }
        writer.newLine();
      }

      writer.write("  ]");
      writer.newLine();
      writer.write("}");

    } catch (final IOException e) {
      LOGGER.warning("Error generating overall metrics JSON content: " + e.getMessage());
      return "{}";
    }

    return stringWriter.toString();
  }

  /**
   * Escapes a string for safe inclusion in JSON.
   *
   * @param value the string to escape
   * @return the escaped string
   */
  private static String escapeJsonString(final String value) {
    if (value == null) {
      return "";
    }

    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\f", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
