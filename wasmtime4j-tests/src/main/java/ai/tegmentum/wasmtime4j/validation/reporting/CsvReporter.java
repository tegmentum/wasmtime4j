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
 * CSV reporter for exporting basic performance metrics.
 *
 * <p>This reporter generates CSV files containing operation metrics for easy integration with CI/CD
 * pipelines and external analysis tools. The format is simple and compatible with standard
 * spreadsheet applications.
 *
 * @since 1.0.0
 */
public final class CsvReporter {

  private static final Logger logger = Logger.getLogger(CsvReporter.class.getName());

  private static final String CSV_HEADER =
      "Timestamp,Operation,SuccessCount,FailureCount,TotalCount,SuccessRate,MinTimeMs,MaxTimeMs,AvgTimeMs";

  private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  /**
   * Exports metrics to a CSV file.
   *
   * @param metrics the metrics collector to export
   * @param outputPath the path where the CSV file will be written
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if metrics or outputPath is null
   */
  public static void exportToFile(final BasicMetricsCollector metrics, final Path outputPath)
      throws IOException {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    final String csvContent = generateCsvContent(metrics);

    // Ensure parent directory exists
    final Path parentDir = outputPath.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }

    // Write CSV content to file
    try (final BufferedWriter writer =
        Files.newBufferedWriter(
            outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      writer.write(csvContent);
    }

    logger.info("Exported metrics to CSV file: " + outputPath);
  }

  /**
   * Exports metrics to a CSV string.
   *
   * @param metrics the metrics collector to export
   * @return the CSV content as a string
   * @throws IllegalArgumentException if metrics is null
   */
  public static String exportToString(final BasicMetricsCollector metrics) {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    return generateCsvContent(metrics);
  }

  /**
   * Appends metrics to an existing CSV file.
   *
   * @param metrics the metrics collector to export
   * @param outputPath the path to the existing CSV file
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if metrics or outputPath is null
   */
  public static void appendToFile(final BasicMetricsCollector metrics, final Path outputPath)
      throws IOException {
    Objects.requireNonNull(metrics, "metrics cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    final boolean fileExists = Files.exists(outputPath);
    final String csvContent = generateCsvContent(metrics, !fileExists);

    // Ensure parent directory exists
    final Path parentDir = outputPath.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }

    // Append CSV content to file
    try (final BufferedWriter writer =
        Files.newBufferedWriter(outputPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writer.write(csvContent);
    }

    logger.info("Appended metrics to CSV file: " + outputPath);
  }

  /**
   * Exports overall metrics summary to CSV.
   *
   * @param overallMetrics the overall metrics to export
   * @param outputPath the path where the CSV file will be written
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if overallMetrics or outputPath is null
   */
  public static void exportOverallMetrics(
      final BasicMetricsCollector.OverallMetrics overallMetrics, final Path outputPath)
      throws IOException {
    Objects.requireNonNull(overallMetrics, "overallMetrics cannot be null");
    Objects.requireNonNull(outputPath, "outputPath cannot be null");

    final String csvContent = generateOverallMetricsCsv(overallMetrics);

    // Ensure parent directory exists
    final Path parentDir = outputPath.getParent();
    if (parentDir != null) {
      Files.createDirectories(parentDir);
    }

    // Write CSV content to file
    try (final BufferedWriter writer =
        Files.newBufferedWriter(
            outputPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
      writer.write(csvContent);
    }

    logger.info("Exported overall metrics to CSV file: " + outputPath);
  }

  private static String generateCsvContent(final BasicMetricsCollector metrics) {
    return generateCsvContent(metrics, true);
  }

  private static String generateCsvContent(
      final BasicMetricsCollector metrics, final boolean includeHeader) {
    final StringWriter stringWriter = new StringWriter();
    final String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());

    try (final BufferedWriter writer = new BufferedWriter(stringWriter)) {
      // Write header if requested
      if (includeHeader) {
        writer.write(CSV_HEADER);
        writer.newLine();
      }

      // Write operation metrics
      final List<BasicMetricsCollector.OperationMetrics> allMetrics =
          metrics.getAllOperationMetrics();

      for (final BasicMetricsCollector.OperationMetrics operationMetrics : allMetrics) {
        writer.write(formatOperationMetricsCsvRow(timestamp, operationMetrics));
        writer.newLine();
      }

      // If no operation metrics, write a summary row
      if (allMetrics.isEmpty()) {
        final BasicMetricsCollector.OverallMetrics overall = metrics.getOverallMetrics();
        writer.write(formatSummaryRow(timestamp, overall));
        writer.newLine();
      }

    } catch (final IOException e) {
      logger.warning("Error generating CSV content: " + e.getMessage());
      return "";
    }

    return stringWriter.toString();
  }

  private static String generateOverallMetricsCsv(
      final BasicMetricsCollector.OverallMetrics overallMetrics) {
    final StringWriter stringWriter = new StringWriter();
    final String timestamp = TIMESTAMP_FORMATTER.format(Instant.now());

    try (final BufferedWriter writer = new BufferedWriter(stringWriter)) {
      // Write header for overall metrics
      writer.write(
          "Timestamp,TotalOperations,SuccessfulOperations,FailedOperations,"
              + "OverallSuccessRate,TotalDurationMs,UniqueOperations");
      writer.newLine();

      // Write overall metrics row
      writer.write(
          String.format(
              "%s,%d,%d,%d,%.4f,%d,%d",
              timestamp,
              overallMetrics.getTotalOperations(),
              overallMetrics.getSuccessfulOperations(),
              overallMetrics.getFailedOperations(),
              overallMetrics.getOverallSuccessRate(),
              overallMetrics.getTotalDuration().toMillis(),
              overallMetrics.getOperationNames().size()));
      writer.newLine();

    } catch (final IOException e) {
      logger.warning("Error generating overall metrics CSV content: " + e.getMessage());
      return "";
    }

    return stringWriter.toString();
  }

  private static String formatOperationMetricsCsvRow(
      final String timestamp, final BasicMetricsCollector.OperationMetrics metrics) {
    return String.format(
        "%s,%s,%d,%d,%d,%.4f,%d,%d,%d",
        timestamp,
        escapeCsvValue(metrics.getOperationName()),
        metrics.getSuccessCount(),
        metrics.getFailureCount(),
        metrics.getTotalCount(),
        metrics.getSuccessRate(),
        metrics.getMinTime().toMillis(),
        metrics.getMaxTime().toMillis(),
        metrics.getAvgTime().toMillis());
  }

  private static String formatSummaryRow(
      final String timestamp, final BasicMetricsCollector.OverallMetrics overall) {
    return String.format(
        "%s,OVERALL,%d,%d,%d,%.4f,0,0,0",
        timestamp,
        overall.getSuccessfulOperations(),
        overall.getFailedOperations(),
        overall.getTotalOperations(),
        overall.getOverallSuccessRate());
  }

  /**
   * Escapes a value for safe inclusion in CSV.
   *
   * @param value the value to escape
   * @return the escaped value
   */
  private static String escapeCsvValue(final String value) {
    if (value == null) {
      return "";
    }

    final String sanitized = value.replace("\"", "\"\"");

    // Quote the value if it contains commas, quotes, or newlines
    if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
      return "\"" + sanitized + "\"";
    }

    return sanitized;
  }
}
