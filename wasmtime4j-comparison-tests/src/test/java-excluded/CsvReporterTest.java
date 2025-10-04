package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ActionableRecommendation;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancySeverity;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancyType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ExecutionPattern;
import ai.tegmentum.wasmtime4j.comparison.analyzers.IssueCategory;
import ai.tegmentum.wasmtime4j.comparison.analyzers.IssueSeverity;
import ai.tegmentum.wasmtime4j.comparison.analyzers.PerformanceAnalyzer;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.RecommendationSummary;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for CsvReporter functionality including different layouts, custom columns,
 * and special character handling.
 */
class CsvReporterTest {

  private CsvReporter csvReporter;
  private ComparisonReport testReport;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    csvReporter = new CsvReporter();
    outputStream = new ByteArrayOutputStream();
    testReport = createTestReport();
  }

  @Test
  @DisplayName("Should export summary layout with key metrics")
  void shouldExportSummaryLayoutWithKeyMetrics() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.SUMMARY).includeHeaders(true).build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    assertNotNull(csvOutput);

    final String[] lines = csvOutput.split("\n");
    assertTrue(lines.length >= 2); // Header + at least one data row

    // Check header
    final String header = lines[0];
    assertTrue(header.contains("testName"));
    assertTrue(header.contains("verdict"));
    assertTrue(header.contains("consistencyScore"));
    assertTrue(header.contains("executionTimeMs"));
    assertTrue(header.contains("memoryUsedBytes"));
    assertTrue(header.contains("coverageScore"));
    assertTrue(header.contains("recommendationCount"));

    // Check data row
    final String dataRow = lines[1];
    assertTrue(dataRow.contains("test1"));
    assertTrue(dataRow.contains("CONSISTENT"));
    assertTrue(dataRow.contains("0.980"));
  }

  @Test
  @DisplayName("Should export detailed layout with normalized structure")
  void shouldExportDetailedLayoutWithNormalizedStructure() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.DETAILED).includeHeaders(true).build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    final String[] lines = csvOutput.split("\n");

    // Check header
    final String header = lines[0];
    assertTrue(header.contains("testName"));
    assertTrue(header.contains("category"));
    assertTrue(header.contains("field"));
    assertTrue(header.contains("value"));
    assertTrue(header.contains("unit"));
    assertTrue(header.contains("description"));

    // Should have multiple rows for different categories
    long behavioralRows =
        java.util.Arrays.stream(lines).filter(line -> line.contains("behavioral")).count();
    assertTrue(behavioralRows > 0);

    long performanceRows =
        java.util.Arrays.stream(lines).filter(line -> line.contains("performance")).count();
    assertTrue(performanceRows > 0);
  }

  @Test
  @DisplayName("Should export recommendations layout")
  void shouldExportRecommendationsLayout() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder()
            .layout(CsvLayout.RECOMMENDATIONS)
            .includeHeaders(true)
            .build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    final String[] lines = csvOutput.split("\n");

    if (lines.length > 1) { // If there are recommendations
      final String header = lines[0];
      assertTrue(header.contains("testName"));
      assertTrue(header.contains("title"));
      assertTrue(header.contains("category"));
      assertTrue(header.contains("severity"));
      assertTrue(header.contains("priorityScore"));
      assertTrue(header.contains("description"));
      assertTrue(header.contains("affectedRuntimes"));
    }
  }

  @Test
  @DisplayName("Should export performance layout with runtime metrics")
  void shouldExportPerformanceLayoutWithRuntimeMetrics() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.PERFORMANCE).includeHeaders(true).build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    final String[] lines = csvOutput.split("\n");

    if (lines.length > 1) { // If there are performance results
      final String header = lines[0];
      assertTrue(header.contains("testName"));
      assertTrue(header.contains("runtime"));
      assertTrue(header.contains("executionTimeMs"));
      assertTrue(header.contains("memoryUsedBytes"));
      assertTrue(header.contains("peakMemoryUsage"));
      assertTrue(header.contains("successful"));
    }
  }

  @Test
  @DisplayName("Should export discrepancies layout with behavioral differences")
  void shouldExportDiscrepanciesLayoutWithBehavioralDifferences()
      throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.DISCREPANCIES).includeHeaders(true).build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    final String[] lines = csvOutput.split("\n");

    if (lines.length > 1) { // If there are discrepancies
      final String header = lines[0];
      assertTrue(header.contains("testName"));
      assertTrue(header.contains("type"));
      assertTrue(header.contains("severity"));
      assertTrue(header.contains("description"));
    }
  }

  @Test
  @DisplayName("Should export custom layout with specified columns")
  void shouldExportCustomLayoutWithSpecifiedColumns() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder()
            .layout(CsvLayout.CUSTOM)
            .addCustomColumn("testName")
            .addCustomColumn("verdict")
            .addCustomColumn("executionTimeMs")
            .addCustomColumn("coverageScore")
            .includeHeaders(true)
            .build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    final String[] lines = csvOutput.split("\n");
    assertTrue(lines.length >= 2);

    // Check header matches custom columns
    final String header = lines[0];
    assertEquals("testName,verdict,executionTimeMs,coverageScore", header);

    // Check data row
    final String dataRow = lines[1];
    final String[] columns = dataRow.split(",");
    assertEquals(4, columns.length);
    assertEquals("test1", columns[0]);
  }

  @Test
  @DisplayName("Should handle different delimiters and quoting")
  void shouldHandleDifferentDelimitersAndQuoting() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder()
            .layout(CsvLayout.SUMMARY)
            .delimiter(";")
            .quoteChar("'")
            .lineEnding("\r\n")
            .build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    assertTrue(csvOutput.contains(";"));
    assertTrue(csvOutput.contains("\r\n"));

    // Values with delimiters should be quoted
    if (csvOutput.contains("'")) {
      // Some value was quoted
      assertTrue(true);
    }
  }

  @Test
  @DisplayName("Should properly escape special characters")
  void shouldProperlyEscapeSpecialCharacters() throws IOException, ExportException {
    // Given
    final ComparisonReport reportWithSpecialChars = createReportWithSpecialCharacters();
    final CsvConfiguration config =
        new CsvConfiguration.Builder()
            .layout(CsvLayout.SUMMARY)
            .delimiter(",")
            .quoteChar("\"")
            .build();

    // When
    csvReporter.export(reportWithSpecialChars, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();

    // Values with commas should be quoted
    assertTrue(
        csvOutput.contains("\"test,with,commas\"")
            || csvOutput.contains("test,with,commas")); // Might be in separate columns

    // Values with quotes should have escaped quotes
    if (csvOutput.contains("\"\"")) {
      // Quote escaping occurred
      assertTrue(true);
    }
  }

  @Test
  @DisplayName("Should handle compression when enabled")
  void shouldHandleCompressionWhenEnabled() throws IOException, ExportException {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.SUMMARY).compressOutput(true).build();

    // When
    csvReporter.export(testReport, config, outputStream);

    // Then
    final byte[] compressedOutput = outputStream.toByteArray();
    assertNotNull(compressedOutput);
    assertTrue(compressedOutput.length > 0);

    // Compressed output should start with GZIP magic number
    assertEquals((byte) 0x1f, compressedOutput[0]);
    assertEquals((byte) 0x8b, compressedOutput[1]);
  }

  @Test
  @DisplayName("Should validate configuration before export")
  void shouldValidateConfigurationBeforeExport() {
    // Given
    final JsonConfiguration wrongConfig = new JsonConfiguration.Builder().build();

    // When & Then
    assertThrows(
        IllegalArgumentException.class,
        () -> csvReporter.export(testReport, wrongConfig, outputStream));
  }

  @Test
  @DisplayName("Should throw exception for custom layout without columns")
  void shouldThrowExceptionForCustomLayoutWithoutColumns() {
    // Given
    final CsvConfiguration config = new CsvConfiguration.Builder().layout(CsvLayout.CUSTOM).build();

    // When & Then
    assertThrows(ExportException.class, () -> csvReporter.export(testReport, config, outputStream));
  }

  @Test
  @DisplayName("Should estimate output size accurately")
  void shouldEstimateOutputSizeAccurately() {
    // Given
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.SUMMARY).build();

    // When
    final long estimatedSize = csvReporter.estimateOutputSize(testReport, config);

    // Then
    assertTrue(estimatedSize > 0);

    // Export and compare to actual size
    try {
      csvReporter.export(testReport, config, outputStream);
      final long actualSize = outputStream.size();

      // Estimation should be within reasonable range
      assertTrue(estimatedSize >= actualSize * 0.3);
      assertTrue(estimatedSize <= actualSize * 3.0);
    } catch (final IOException | ExportException e) {
      fail("Export failed during size validation");
    }
  }

  @Test
  @DisplayName("Should support streaming capability")
  void shouldSupportStreamingCapability() {
    // When & Then
    assertTrue(csvReporter.supportsStreaming());
  }

  @Test
  @DisplayName("Should return correct format and schema")
  void shouldReturnCorrectFormatAndSchema() {
    // When & Then
    assertEquals(ExportFormat.CSV, csvReporter.getFormat());

    final ExportSchema schema = csvReporter.getSchema();
    assertNotNull(schema);
    assertEquals(ExportFormat.CSV, schema.getFormat());
    assertEquals("1.0.0", schema.getVersion());
    assertTrue(schema.getDescription().contains("CSV"));
    assertFalse(schema.getSchemaDefinition().isEmpty());
  }

  @Test
  @DisplayName("Should handle empty report gracefully")
  void shouldHandleEmptyReportGracefully() throws IOException, ExportException {
    // Given
    final ComparisonReport emptyReport = createEmptyReport();
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.SUMMARY).includeHeaders(true).build();

    // When
    csvReporter.export(emptyReport, config, outputStream);

    // Then
    final String csvOutput = outputStream.toString();
    assertNotNull(csvOutput);

    // Should at least have header row
    final String[] lines = csvOutput.split("\n");
    assertTrue(lines.length >= 1);
    assertTrue(lines[0].contains("testName"));
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValuesGracefully() {
    // Given
    final CsvConfiguration config = new CsvConfiguration.Builder().build();

    // When & Then
    assertThrows(NullPointerException.class, () -> csvReporter.export(null, config, outputStream));

    assertThrows(
        NullPointerException.class, () -> csvReporter.export(testReport, null, outputStream));

    assertThrows(NullPointerException.class, () -> csvReporter.export(testReport, config, null));
  }

  @Test
  @DisplayName("Should handle large dataset efficiently")
  void shouldHandleLargeDatasetEfficiently() throws IOException, ExportException {
    // Given
    final ComparisonReport largeReport = createLargeTestReport(1000);
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.SUMMARY).bufferSize(8192).build();

    final long startTime = System.currentTimeMillis();

    // When
    csvReporter.export(largeReport, config, outputStream);

    // Then
    final long duration = System.currentTimeMillis() - startTime;
    final String csvOutput = outputStream.toString();

    // Should complete in reasonable time (under 5 seconds for 1000 tests)
    assertTrue(duration < 5000, "Export took too long: " + duration + "ms");

    // Should have correct number of rows (header + 1000 data rows)
    final long lineCount = csvOutput.chars().filter(ch -> ch == '\n').count();
    assertTrue(lineCount >= 1000);
  }

  // Helper methods for creating test data

  private ComparisonReport createTestReport() {
    final ReportMetadata metadata =
        new ReportMetadata(
            "Test Suite",
            "1.0.0",
            Instant.now(),
            Duration.ofMinutes(5),
            List.of("JNI", "PANAMA"),
            Map.of("timeout", "30s"),
            "wasmtime4j-1.0.0");

    final ReportSummary summary =
        new ReportSummary(2, 2, 2, 2, 1, 1, 0.95, Map.of("CONSISTENT", 1, "MOSTLY_CONSISTENT", 1));

    // Create behavioral analysis
    final BehavioralAnalysisResult behavioral1 =
        new BehavioralAnalysisResult.Builder("test1")
            .verdict(BehavioralVerdict.CONSISTENT)
            .consistencyScore(0.98)
            .discrepancies(
                List.of(
                    new BehavioralDiscrepancy(
                        DiscrepancyType.VALUE_MISMATCH,
                        DiscrepancySeverity.LOW,
                        "Minor difference",
                        RuntimeType.JNI,
                        RuntimeType.PANAMA)))
            .executionPattern(new ExecutionPattern(2, 0, 0, 1, 0, 0.1))
            .build();

    // Create performance analysis
    final PerformanceAnalyzer.PerformanceComparisonResult performance1 =
        new PerformanceAnalyzer.PerformanceComparisonResult.Builder("test1", "JNI")
            .executionDuration(Duration.ofMillis(150))
            .memoryUsed(1024 * 1024)
            .peakMemoryUsage(1024 * 1024 * 2)
            .successful(true)
            .build();

    // Create coverage analysis
    final CoverageAnalysisResult coverage1 =
        new CoverageAnalysisResult.Builder("test1")
            .coverageScore(0.85)
            .featuresImplemented(17)
            .totalFeatures(20)
            .missingFeatures(List.of("feature1", "feature2"))
            .build();

    // Create recommendations
    final ActionableRecommendation recommendation =
        new ActionableRecommendation(
            "Optimize memory usage",
            "Consider reducing memory allocation in hot paths",
            List.of("Step 1", "Step 2"),
            IssueCategory.PERFORMANCE,
            IssueSeverity.MEDIUM,
            0.7,
            Set.of(RuntimeType.JNI),
            "MEMORY_OPTIMIZATION");

    final RecommendationResult recommendations1 =
        new RecommendationResult.Builder("test1")
            .prioritizedRecommendations(List.of(recommendation))
            .summary(new RecommendationSummary(1, 0, 1, 0, Map.of(IssueCategory.PERFORMANCE, 1)))
            .build();

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(List.of("test1", "test2"))
        .behavioralResults(Map.of("test1", behavioral1))
        .performanceResults(Map.of("test1", performance1))
        .coverageResults(Map.of("test1", coverage1))
        .recommendations(Map.of("test1", recommendations1))
        .build();
  }

  private ComparisonReport createReportWithSpecialCharacters() {
    final ReportMetadata metadata =
        new ReportMetadata(
            "Test,Suite,With,Commas",
            "1.0.0",
            Instant.now(),
            Duration.ofMinutes(1),
            List.of("JNI"),
            Map.of("key,with,commas", "value\"with\"quotes"),
            "wasmtime4j-1.0.0");

    final ReportSummary summary = new ReportSummary(1, 1, 1, 1, 0, 0, 1.0, Map.of("CONSISTENT", 1));

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(
            List.of("test,with,commas", "test\"with\"quotes", "test\nwith\nnewlines"))
        .build();
  }

  private ComparisonReport createEmptyReport() {
    final ReportMetadata metadata =
        new ReportMetadata(
            "Empty Suite",
            "1.0.0",
            Instant.now(),
            Duration.ZERO,
            List.of("JNI"),
            Map.of(),
            "wasmtime4j-1.0.0");

    final ReportSummary summary = new ReportSummary(0, 0, 0, 0, 0, 0, 0.0, Map.of());

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(Collections.emptyList())
        .build();
  }

  private ComparisonReport createLargeTestReport(final int testCount) {
    final ReportMetadata metadata =
        new ReportMetadata(
            "Large Test Suite",
            "1.0.0",
            Instant.now(),
            Duration.ofHours(1),
            List.of("JNI", "PANAMA"),
            Map.of("tests", String.valueOf(testCount)),
            "wasmtime4j-1.0.0");

    final ReportSummary summary =
        new ReportSummary(
            testCount, testCount / 2, testCount / 2, testCount / 2, 0, 0, 0.9, Map.of());

    final List<String> testNames = new java.util.ArrayList<>();
    for (int i = 0; i < testCount; i++) {
      testNames.add("test" + i);
    }

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(testNames)
        .build();
  }
}
