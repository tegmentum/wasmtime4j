package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Comprehensive tests for StreamingExporter functionality including async operations, progress
 * reporting, and cancellation.
 */
class StreamingExporterTest {

  private StreamingExporter streamingExporter;
  private ComparisonReport testReport;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    final StreamingConfiguration config =
        new StreamingConfiguration.Builder()
            .chunkSize(50)
            .bufferSize(4096)
            .chunkDelayMs(10)
            .threadPoolSize(2)
            .enableProgressReporting(true)
            .build();

    streamingExporter = new StreamingExporter(config);
    testReport = createTestReport();
    outputStream = new ByteArrayOutputStream();
  }

  @AfterEach
  void tearDown() {
    if (streamingExporter != null) {
      streamingExporter.close();
    }
  }

  @Test
  @DisplayName("Should export synchronously with streaming support")
  void shouldExportSynchronouslyWithStreamingSupport() throws IOException, ExportException {
    // Given
    final JsonReporter jsonReporter = new JsonReporter();
    final JsonConfiguration config =
        new JsonConfiguration.Builder()
            .streamingMode(true)
            .detailLevel(JsonDetailLevel.SUMMARY)
            .build();

    final AtomicInteger progressUpdates = new AtomicInteger(0);
    final AtomicReference<ProgressUpdate> lastUpdate = new AtomicReference<>();

    // When
    final ExportResult result =
        streamingExporter.exportSync(
            testReport,
            jsonReporter,
            config,
            outputStream,
            progress -> {
              progressUpdates.incrementAndGet();
              lastUpdate.set(progress);
            });

    // Then
    assertNotNull(result);
    assertTrue(result.isSuccessful());
    assertTrue(result.getItemsProcessed() > 0);
    assertTrue(result.getBytesWritten() > 0);
    assertEquals("Export completed", result.getMessage());

    // Verify progress reporting
    assertTrue(progressUpdates.get() > 0);
    assertNotNull(lastUpdate.get());

    // Verify output was written
    assertTrue(outputStream.size() > 0);
    final String output = outputStream.toString();
    assertTrue(output.contains("\"schema\""));
  }

  @Test
  @DisplayName("Should export asynchronously with progress reporting")
  @Timeout(value = 30, unit = TimeUnit.SECONDS)
  void shouldExportAsynchronouslyWithProgressReporting() throws Exception {
    // Given
    final ComparisonReport largeReport = createLargeTestReport(200);
    final CsvReporter csvReporter = new CsvReporter();
    final CsvConfiguration config =
        new CsvConfiguration.Builder().layout(CsvLayout.SUMMARY).build();

    final AtomicInteger progressUpdates = new AtomicInteger(0);
    final AtomicReference<ProgressUpdate> lastUpdate = new AtomicReference<>();

    // When
    final CompletableFuture<ExportResult> future =
        streamingExporter.exportAsync(
            largeReport,
            csvReporter,
            config,
            outputStream,
            progress -> {
              progressUpdates.incrementAndGet();
              lastUpdate.set(progress);
            });

    final ExportResult result = future.get(25, TimeUnit.SECONDS);

    // Then
    assertNotNull(result);
    assertTrue(result.isSuccessful());
    assertEquals(200, result.getItemsProcessed());
    assertTrue(result.getBytesWritten() > 0);

    // Verify progress reporting
    assertTrue(progressUpdates.get() > 0);
    final ProgressUpdate finalProgress = lastUpdate.get();
    assertNotNull(finalProgress);
    assertEquals(200, finalProgress.getTotalItems());
    assertEquals(200, finalProgress.getItemsProcessed());
    assertEquals(1.0, finalProgress.getProgressPercent(), 0.001);

    // Verify output
    final String csvOutput = outputStream.toString();
    assertTrue(csvOutput.contains("testName"));
    final long lineCount = csvOutput.chars().filter(ch -> ch == '\n').count();
    assertTrue(lineCount > 200); // Header + data rows
  }

  @Test
  @DisplayName("Should handle cancellation gracefully")
  @Timeout(value = 15, unit = TimeUnit.SECONDS)
  void shouldHandleCancellationGracefully() throws Exception {
    // Given
    final ComparisonReport largeReport = createLargeTestReport(1000);
    final JsonReporter jsonReporter = new JsonReporter();
    final JsonConfiguration config =
        new JsonConfiguration.Builder()
            .streamingMode(true)
            .detailLevel(JsonDetailLevel.DETAILED)
            .build();

    final AtomicInteger progressUpdates = new AtomicInteger(0);

    // When
    final CompletableFuture<ExportResult> future =
        streamingExporter.exportAsync(
            largeReport,
            jsonReporter,
            config,
            outputStream,
            progress -> {
              progressUpdates.incrementAndGet();
              // Cancel after a few progress updates
              if (progressUpdates.get() >= 3) {
                streamingExporter.cancel();
              }
            });

    final ExportResult result = future.get(10, TimeUnit.SECONDS);

    // Then
    assertNotNull(result);
    assertFalse(result.isSuccessful());
    assertTrue(result.getMessage().contains("cancelled"));
    assertTrue(result.getItemsProcessed() < 1000); // Should not have processed all items
    assertTrue(streamingExporter.isCancelled());
  }

  @Test
  @DisplayName("Should handle fallback for non-streaming exporters")
  void shouldHandleFallbackForNonStreamingExporters() throws IOException, ExportException {
    // Given
    final TestNonStreamingReporter nonStreamingReporter = new TestNonStreamingReporter();
    final JsonConfiguration config = new JsonConfiguration.Builder().build();

    // When
    final ExportResult result =
        streamingExporter.exportSync(testReport, nonStreamingReporter, config, outputStream, null);

    // Then
    assertNotNull(result);
    assertTrue(result.isSuccessful());
    assertTrue(result.getMessage().contains("non-streaming"));
    assertEquals(testReport.getTestCount(), result.getItemsProcessed());
  }

  @Test
  @DisplayName("Should handle export errors gracefully")
  @Timeout(value = 10, unit = TimeUnit.SECONDS)
  void shouldHandleExportErrorsGracefully() throws Exception {
    // Given
    final TestFailingReporter failingReporter = new TestFailingReporter();
    final JsonConfiguration config = new JsonConfiguration.Builder().build();

    // When
    final CompletableFuture<ExportResult> future =
        streamingExporter.exportAsync(testReport, failingReporter, config, outputStream, null);

    final ExportResult result = future.get(5, TimeUnit.SECONDS);

    // Then
    assertNotNull(result);
    assertFalse(result.isSuccessful());
    assertTrue(result.getMessage().contains("Test export failure"));
    assertEquals(0, result.getItemsProcessed());
    assertEquals(0, result.getBytesWritten());
  }

  @Test
  @DisplayName("Should track bytes written accurately")
  void shouldTrackBytesWrittenAccurately() throws IOException, ExportException {
    // Given
    final JsonReporter jsonReporter = new JsonReporter();
    final JsonConfiguration config = new JsonConfiguration.Builder().streamingMode(false).build();

    // When
    final ExportResult result =
        streamingExporter.exportSync(testReport, jsonReporter, config, outputStream, null);

    // Then
    assertNotNull(result);
    assertTrue(result.isSuccessful());

    final long actualBytes = outputStream.size();
    final long reportedBytes = result.getBytesWritten();

    // Should match exactly or be very close (within buffering tolerance)
    assertTrue(
        Math.abs(actualBytes - reportedBytes) <= 100,
        String.format("Actual: %d, Reported: %d", actualBytes, reportedBytes));
  }

  @Test
  @DisplayName("Should handle null progress callback gracefully")
  void shouldHandleNullProgressCallbackGracefully() throws IOException, ExportException {
    // Given
    final JsonReporter jsonReporter = new JsonReporter();
    final JsonConfiguration config = new JsonConfiguration.Builder().build();

    // When
    final ExportResult result =
        streamingExporter.exportSync(
            testReport, jsonReporter, config, outputStream, null // null progress callback
            );

    // Then
    assertNotNull(result);
    assertTrue(result.isSuccessful());
    assertTrue(result.getItemsProcessed() > 0);
  }

  @Test
  @DisplayName("Should validate input parameters")
  void shouldValidateInputParameters() {
    // Given
    final JsonReporter jsonReporter = new JsonReporter();
    final JsonConfiguration config = new JsonConfiguration.Builder().build();

    // When & Then
    assertThrows(
        NullPointerException.class,
        () -> streamingExporter.exportSync(null, jsonReporter, config, outputStream, null));

    assertThrows(
        NullPointerException.class,
        () -> streamingExporter.exportSync(testReport, null, config, outputStream, null));

    assertThrows(
        NullPointerException.class,
        () -> streamingExporter.exportSync(testReport, jsonReporter, null, outputStream, null));

    assertThrows(
        NullPointerException.class,
        () -> streamingExporter.exportSync(testReport, jsonReporter, config, null, null));
  }

  @Test
  @DisplayName("Should create StreamingConfiguration with default values")
  void shouldCreateStreamingConfigurationWithDefaultValues() {
    // When
    final StreamingConfiguration config = new StreamingConfiguration.Builder().build();

    // Then
    assertNotNull(config);
    assertTrue(config.getChunkSize() > 0);
    assertTrue(config.getBufferSize() > 0);
    assertTrue(config.getChunkDelayMs() >= 0);
    assertTrue(config.getThreadPoolSize() > 0);
  }

  @Test
  @DisplayName("Should create StreamingConfiguration with custom values")
  void shouldCreateStreamingConfigurationWithCustomValues() {
    // When
    final StreamingConfiguration config =
        new StreamingConfiguration.Builder()
            .chunkSize(200)
            .bufferSize(16384)
            .chunkDelayMs(50)
            .threadPoolSize(4)
            .enableProgressReporting(false)
            .build();

    // Then
    assertEquals(200, config.getChunkSize());
    assertEquals(16384, config.getBufferSize());
    assertEquals(50, config.getChunkDelayMs());
    assertEquals(4, config.getThreadPoolSize());
    assertFalse(config.isEnableProgressReporting());
  }

  @Test
  @DisplayName("Should close resources properly")
  void shouldCloseResourcesProperly() {
    // Given
    final StreamingExporter exporter = new StreamingExporter();
    assertFalse(exporter.isCancelled());

    // When
    exporter.close();

    // Then
    assertTrue(exporter.isCancelled());
  }

  // Helper methods and classes

  private ComparisonReport createTestReport() {
    final ReportMetadata metadata =
        new ReportMetadata(
            "Test Suite",
            "1.0.0",
            Instant.now(),
            Duration.ofMinutes(5),
            List.of("JNI", "PANAMA"),
            Map.of("config", "test"),
            "wasmtime4j-1.0.0");

    final ReportSummary summary =
        new ReportSummary(3, 3, 3, 3, 0, 0, 0.95, Map.of("CONSISTENT", 3));

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(List.of("test1", "test2", "test3"))
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
        new ReportSummary(testCount, testCount, testCount, testCount, 0, 0, 0.9, Map.of());

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

  /** Test reporter that doesn't support streaming. */
  private static class TestNonStreamingReporter implements DataExporter<JsonConfiguration> {
    @Override
    public void export(
        ComparisonReport report, JsonConfiguration configuration, java.io.OutputStream output)
        throws IOException, ExportException {
      output.write("Test export completed".getBytes());
    }

    @Override
    public ExportFormat getFormat() {
      return ExportFormat.JSON;
    }

    @Override
    public ExportSchema getSchema() {
      return new ExportSchema(ExportFormat.JSON, "1.0.0", "Test schema", "{}", Map.of());
    }

    @Override
    public boolean supportsStreaming() {
      return false;
    }
  }

  /** Test reporter that always fails. */
  private static class TestFailingReporter implements DataExporter<JsonConfiguration> {
    @Override
    public void export(
        ComparisonReport report, JsonConfiguration configuration, java.io.OutputStream output)
        throws IOException, ExportException {
      throw new ExportException("Test export failure", ExportFormat.JSON, "test");
    }

    @Override
    public ExportFormat getFormat() {
      return ExportFormat.JSON;
    }

    @Override
    public ExportSchema getSchema() {
      return new ExportSchema(ExportFormat.JSON, "1.0.0", "Test schema", "{}", Map.of());
    }

    @Override
    public boolean supportsStreaming() {
      return true;
    }
  }
}
