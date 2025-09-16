package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralVerdict;
import ai.tegmentum.wasmtime4j.comparison.analyzers.CoverageAnalysisResult;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancySeverity;
import ai.tegmentum.wasmtime4j.comparison.analyzers.DiscrepancyType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.ExecutionPattern;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive tests for JsonReporter functionality including schema validation,
 * streaming export, and error handling.
 */
class JsonReporterTest {

  private JsonReporter jsonReporter;
  private ComparisonReport testReport;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    jsonReporter = new JsonReporter();
    outputStream = new ByteArrayOutputStream();
    testReport = createTestReport();
  }

  @Test
  @DisplayName("Should export summary-level JSON successfully")
  void shouldExportSummaryJsonSuccessfully() throws IOException, ExportException {
    // Given
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.SUMMARY)
        .streamingMode(false)
        .includeMetadata(true)
        .build();

    // When
    jsonReporter.export(testReport, config, outputStream);

    // Then
    final String jsonOutput = outputStream.toString();
    assertNotNull(jsonOutput);
    assertFalse(jsonOutput.isEmpty());

    // Verify JSON structure
    assertTrue(jsonOutput.contains("\"schema\""));
    assertTrue(jsonOutput.contains("\"metadata\""));
    assertTrue(jsonOutput.contains("\"summary\""));
    assertTrue(jsonOutput.contains("\"results\""));

    // Verify schema version
    assertTrue(jsonOutput.contains("\"version\": \"1.0.0\""));

    // Verify test results are present
    assertTrue(jsonOutput.contains("\"test1\""));
    assertTrue(jsonOutput.contains("\"test2\""));
  }

  @Test
  @DisplayName("Should export detailed JSON with all analysis results")
  void shouldExportDetailedJsonWithAllResults() throws IOException, ExportException {
    // Given
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.DETAILED)
        .streamingMode(false)
        .includeMetadata(true)
        .build();

    // When
    jsonReporter.export(testReport, config, outputStream);

    // Then
    final String jsonOutput = outputStream.toString();

    // Verify detailed behavioral analysis
    assertTrue(jsonOutput.contains("\"pairwiseComparisons\""));
    assertTrue(jsonOutput.contains("\"discrepancies\""));

    // Verify performance details
    assertTrue(jsonOutput.contains("\"peakMemoryUsage\""));

    // Verify coverage details
    assertTrue(jsonOutput.contains("\"missingFeatures\""));

    // Verify recommendation details
    assertTrue(jsonOutput.contains("\"prioritizedRecommendations\""));
  }

  @Test
  @DisplayName("Should support streaming mode for large datasets")
  void shouldSupportStreamingModeForLargeDatasets() throws IOException, ExportException {
    // Given
    final ComparisonReport largeReport = createLargeTestReport(1000);
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.SUMMARY)
        .streamingMode(true)
        .bufferSize(4096)
        .build();

    // When
    jsonReporter.export(largeReport, config, outputStream);

    // Then
    final String jsonOutput = outputStream.toString();
    assertNotNull(jsonOutput);
    assertTrue(jsonOutput.length() > 1000); // Should be substantial

    // Verify structure is maintained in streaming mode
    assertTrue(jsonOutput.startsWith("{"));
    assertTrue(jsonOutput.endsWith("}\n"));
    assertTrue(jsonOutput.contains("\"results\""));
  }

  @Test
  @DisplayName("Should handle compression when enabled")
  void shouldHandleCompressionWhenEnabled() throws IOException, ExportException {
    // Given
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.SUMMARY)
        .compressOutput(true)
        .build();

    // When
    jsonReporter.export(testReport, config, outputStream);

    // Then
    final byte[] compressedOutput = outputStream.toByteArray();
    assertNotNull(compressedOutput);
    assertTrue(compressedOutput.length > 0);

    // Compressed output should be binary (not readable text)
    // First few bytes should be GZIP magic number
    assertEquals((byte) 0x1f, compressedOutput[0]);
    assertEquals((byte) 0x8b, compressedOutput[1]);
  }

  @Test
  @DisplayName("Should validate configuration before export")
  void shouldValidateConfigurationBeforeExport() {
    // Given
    final CsvConfiguration wrongConfig = new CsvConfiguration.Builder().build();

    // When & Then
    assertThrows(IllegalArgumentException.class, () ->
        jsonReporter.export(testReport, wrongConfig, outputStream));
  }

  @Test
  @DisplayName("Should estimate output size accurately")
  void shouldEstimateOutputSizeAccurately() {
    // Given
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.SUMMARY)
        .build();

    // When
    final long estimatedSize = jsonReporter.estimateOutputSize(testReport, config);

    // Then
    assertTrue(estimatedSize > 0);

    // Export and compare to actual size
    try {
      jsonReporter.export(testReport, config, outputStream);
      final long actualSize = outputStream.size();

      // Estimation should be within reasonable range (±50%)
      assertTrue(estimatedSize >= actualSize * 0.5);
      assertTrue(estimatedSize <= actualSize * 2.0);
    } catch (final IOException | ExportException e) {
      fail("Export failed during size validation");
    }
  }

  @Test
  @DisplayName("Should support streaming capability")
  void shouldSupportStreamingCapability() {
    // When & Then
    assertTrue(jsonReporter.supportsStreaming());
  }

  @Test
  @DisplayName("Should return correct format and schema")
  void shouldReturnCorrectFormatAndSchema() {
    // When & Then
    assertEquals(ExportFormat.JSON, jsonReporter.getFormat());

    final ExportSchema schema = jsonReporter.getSchema();
    assertNotNull(schema);
    assertEquals(ExportFormat.JSON, schema.getFormat());
    assertEquals("1.0.0", schema.getVersion());
    assertTrue(schema.getDescription().contains("JSON"));
    assertFalse(schema.getSchemaDefinition().isEmpty());
  }

  @Test
  @DisplayName("Should escape special characters in JSON output")
  void shouldEscapeSpecialCharactersInJsonOutput() throws IOException, ExportException {
    // Given
    final ComparisonReport reportWithSpecialChars = createReportWithSpecialCharacters();
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.DETAILED)
        .build();

    // When
    jsonReporter.export(reportWithSpecialChars, config, outputStream);

    // Then
    final String jsonOutput = outputStream.toString();

    // Verify special characters are properly escaped
    assertTrue(jsonOutput.contains("\\\"quoted\\\""));
    assertTrue(jsonOutput.contains("\\nnewline"));
    assertTrue(jsonOutput.contains("\\ttab"));
    assertTrue(jsonOutput.contains("\\\\backslash"));
  }

  @Test
  @DisplayName("Should handle empty report gracefully")
  void shouldHandleEmptyReportGracefully() throws IOException, ExportException {
    // Given
    final ComparisonReport emptyReport = createEmptyReport();
    final JsonConfiguration config = new JsonConfiguration.Builder()
        .detailLevel(JsonDetailLevel.SUMMARY)
        .build();

    // When
    jsonReporter.export(emptyReport, config, outputStream);

    // Then
    final String jsonOutput = outputStream.toString();
    assertNotNull(jsonOutput);

    // Should still have valid JSON structure
    assertTrue(jsonOutput.contains("\"schema\""));
    assertTrue(jsonOutput.contains("\"metadata\""));
    assertTrue(jsonOutput.contains("\"summary\""));
    assertTrue(jsonOutput.contains("\"results\": {}"));
  }

  @Test
  @DisplayName("Should handle null values gracefully")
  void shouldHandleNullValuesGracefully() {
    // Given
    final JsonConfiguration config = new JsonConfiguration.Builder().build();

    // When & Then
    assertThrows(NullPointerException.class, () ->
        jsonReporter.export(null, config, outputStream));

    assertThrows(NullPointerException.class, () ->
        jsonReporter.export(testReport, null, outputStream));

    assertThrows(NullPointerException.class, () ->
        jsonReporter.export(testReport, config, null));
  }

  // Helper methods for creating test data

  private ComparisonReport createTestReport() {
    final ReportMetadata metadata = new ReportMetadata(
        "Test Suite",
        "1.0.0",
        Instant.now(),
        Duration.ofMinutes(5),
        List.of("JNI", "PANAMA"),
        Map.of("timeout", "30s", "iterations", "3"),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        2, 2, 2, 2, 1, 1, 0.95,
        Map.of("CONSISTENT", 1, "MOSTLY_CONSISTENT", 1)
    );

    // Create behavioral analysis for test1
    final BehavioralAnalysisResult behavioral1 = new BehavioralAnalysisResult.Builder("test1")
        .verdict(BehavioralVerdict.CONSISTENT)
        .consistencyScore(0.98)
        .discrepancies(List.of(
            new BehavioralDiscrepancy(
                DiscrepancyType.VALUE_MISMATCH,
                DiscrepancySeverity.LOW,
                "Minor floating point difference",
                RuntimeType.JNI,
                RuntimeType.PANAMA
            )
        ))
        .executionPattern(new ExecutionPattern(2, 0, 0, 1, 0, 0.1))
        .build();

    // Create performance analysis for test1
    final PerformanceAnalyzer.PerformanceComparisonResult performance1 =
        new PerformanceAnalyzer.PerformanceComparisonResult.Builder("test1", "JNI")
            .executionDuration(Duration.ofMillis(150))
            .memoryUsed(1024 * 1024)
            .peakMemoryUsage(1024 * 1024 * 2)
            .successful(true)
            .build();

    // Create coverage analysis for test1
    final CoverageAnalysisResult coverage1 = new CoverageAnalysisResult.Builder("test1")
        .coverageScore(0.85)
        .featuresImplemented(17)
        .totalFeatures(20)
        .missingFeatures(List.of("feature1", "feature2", "feature3"))
        .build();

    // Create recommendations for test1
    final RecommendationResult recommendations1 = new RecommendationResult.Builder("test1")
        .summary(new RecommendationSummary(1, 1, 0, 0, Map.of()))
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

  private ComparisonReport createLargeTestReport(final int testCount) {
    final ReportMetadata metadata = new ReportMetadata(
        "Large Test Suite",
        "1.0.0",
        Instant.now(),
        Duration.ofHours(1),
        List.of("JNI", "PANAMA"),
        Map.of("tests", String.valueOf(testCount)),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        testCount, testCount, testCount, testCount, testCount / 2, testCount / 10, 0.92,
        Map.of("CONSISTENT", testCount / 2, "MOSTLY_CONSISTENT", testCount / 2)
    );

    final List<String> testNames = new java.util.ArrayList<>();
    for (int i = 0; i < testCount; i++) {
      testNames.add("test" + i);
    }

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(testNames)
        .behavioralResults(Collections.emptyMap())
        .performanceResults(Collections.emptyMap())
        .coverageResults(Collections.emptyMap())
        .recommendations(Collections.emptyMap())
        .build();
  }

  private ComparisonReport createReportWithSpecialCharacters() {
    final ReportMetadata metadata = new ReportMetadata(
        "Test \"quoted\" suite\nwith newlines\tand tabs\\and backslashes",
        "1.0.0",
        Instant.now(),
        Duration.ofMinutes(1),
        List.of("JNI"),
        Map.of("special", "value\"with\\chars\n"),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        1, 1, 1, 1, 0, 0, 1.0, Map.of("CONSISTENT", 1)
    );

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(List.of("test\"with\\special\nchars"))
        .build();
  }

  private ComparisonReport createEmptyReport() {
    final ReportMetadata metadata = new ReportMetadata(
        "Empty Suite",
        "1.0.0",
        Instant.now(),
        Duration.ZERO,
        List.of("JNI"),
        Map.of(),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        0, 0, 0, 0, 0, 0, 0.0, Map.of()
    );

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(Collections.emptyList())
        .build();
  }
}