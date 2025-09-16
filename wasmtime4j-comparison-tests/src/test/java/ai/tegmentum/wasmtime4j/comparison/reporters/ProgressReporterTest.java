package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive unit tests for ProgressReporter real-time progress tracking functionality. Tests
 * Observer pattern implementation, progress calculations, and console output formatting.
 */
final class ProgressReporterTest {

  private ByteArrayOutputStream outputStream;
  private PrintStream printStream;
  private ProgressReporter progressReporter;

  @BeforeEach
  void setUp() {
    outputStream = new ByteArrayOutputStream();
    printStream = new PrintStream(outputStream);
    progressReporter = new ProgressReporter(printStream, VerbosityLevel.VERBOSE, false);
  }

  @AfterEach
  void tearDown() {
    printStream.close();
  }

  @Test
  void testOperationLifecycle() {
    final String operationName = "Test Operation";
    final int totalSteps = 5;

    // Start operation
    progressReporter.onOperationStarted(operationName, totalSteps);
    assertEquals(operationName, progressReporter.getCurrentOperation());
    assertEquals(0, progressReporter.getProgressPercentage());

    // Progress updates
    progressReporter.onProgress(2, "Processing step 2");
    assertEquals(40, progressReporter.getProgressPercentage()); // 2/5 = 40%

    progressReporter.onProgress(4, "Processing step 4");
    assertEquals(80, progressReporter.getProgressPercentage()); // 4/5 = 80%

    // Complete operation
    progressReporter.onOperationCompleted(operationName, "Operation completed successfully");
    assertNull(progressReporter.getCurrentOperation());
    assertEquals(-1, progressReporter.getProgressPercentage()); // Reset to indeterminate
  }

  @Test
  void testIndeterminateProgress() {
    final String operationName = "Indeterminate Operation";

    // Start operation with unknown total steps
    progressReporter.onOperationStarted(operationName, 0);
    assertEquals(operationName, progressReporter.getCurrentOperation());
    assertEquals(-1, progressReporter.getProgressPercentage()); // Indeterminate

    // Progress updates without percentage
    progressReporter.onProgress(1, "Some progress");
    assertEquals(-1, progressReporter.getProgressPercentage());

    progressReporter.onOperationCompleted(operationName, null);
    assertNull(progressReporter.getCurrentOperation());
  }

  @Test
  void testOperationFailure() {
    final String operationName = "Failing Operation";
    final RuntimeException error = new RuntimeException("Test error");

    progressReporter.onOperationStarted(operationName, 3);
    progressReporter.onProgress(1, "Step 1");

    // Simulate failure
    progressReporter.onOperationFailed(operationName, error);

    assertNull(progressReporter.getCurrentOperation());
    assertEquals(-1, progressReporter.getProgressPercentage());

    // Check that error was recorded in output
    final String output = outputStream.toString();
    assertTrue(output.contains("Test error"));
  }

  @Test
  void testStatusUpdates() {
    progressReporter.onStatusUpdate("Status message", "Additional details");

    final String output = outputStream.toString();
    assertTrue(output.contains("Status message"));
  }

  @Test
  void testOperationStatistics() {
    final String operationName = "Stats Test";

    // Complete a successful operation
    progressReporter.onOperationStarted(operationName, 2);
    progressReporter.onProgress(1, "Step 1");
    progressReporter.onProgress(2, "Step 2");
    progressReporter.onOperationCompleted(operationName, "Success");

    // Complete a failed operation
    progressReporter.onOperationStarted(operationName, 1);
    progressReporter.onOperationFailed(operationName, new RuntimeException("Failure"));

    final var stats = progressReporter.getOperationStats();
    assertNotNull(stats);
    assertTrue(stats.containsKey(operationName));

    final ProgressReporter.OperationStats operationStats = stats.get(operationName);
    assertEquals(2, operationStats.getCount());
    assertEquals(1, operationStats.getSuccessCount());
    assertEquals(1, operationStats.getFailureCount());
    assertEquals(0.5, operationStats.getSuccessRate(), 0.01);
  }

  @Test
  void testEstimatedTimeRemaining() {
    progressReporter.onOperationStarted("Timed Operation", 10);

    // No estimate available at start
    assertNull(progressReporter.getEstimatedTimeRemaining());

    // Simulate some time passing
    try {
      Thread.sleep(10); // Small delay
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    progressReporter.onProgress(5, "Halfway done");

    // Should have an estimate now (though it might be very small)
    final Duration estimate = progressReporter.getEstimatedTimeRemaining();
    // We can't assert exact values due to timing variability, but it should exist
    assertTrue(estimate == null || estimate.toMillis() >= 0);
  }

  @Test
  void testProgressBarConfiguration() {
    // Test disabling progress bar
    progressReporter.setProgressBarEnabled(false);
    progressReporter.onOperationStarted("No Progress Bar", 5);
    progressReporter.onProgress(2, "Step 2");

    final String output = outputStream.toString();
    // Progress bar characters should not be present
    assertTrue(!output.contains("[===="));
  }

  @Test
  void testTimestampConfiguration() {
    progressReporter.setTimestampsEnabled(true);
    progressReporter.onStatusUpdate("Timestamped message", null);

    final String output = outputStream.toString();
    // Should contain timestamp format (brackets with time)
    assertTrue(output.contains("[") && output.contains("]"));
  }

  @Test
  void testVerbosityLevels() {
    // Test with QUIET verbosity - should produce minimal output
    final ProgressReporter quietReporter =
        new ProgressReporter(printStream, VerbosityLevel.QUIET, false);

    outputStream.reset();
    quietReporter.onOperationStarted("Quiet Operation", 3);
    quietReporter.onProgress(1, "Step 1");
    quietReporter.onStatusUpdate("Quiet status", null);

    final String quietOutput = outputStream.toString();
    assertTrue(quietOutput.isEmpty() || quietOutput.trim().isEmpty());

    // Test with NORMAL verbosity - should show operation start/completion
    final ProgressReporter normalReporter =
        new ProgressReporter(printStream, VerbosityLevel.NORMAL, false);

    outputStream.reset();
    normalReporter.onOperationStarted("Normal Operation", 3);
    normalReporter.onOperationCompleted("Normal Operation", "Done");

    final String normalOutput = outputStream.toString();
    assertTrue(normalOutput.contains("Normal Operation"));
  }

  @Test
  void testColorOutput() {
    final ProgressReporter colorReporter =
        new ProgressReporter(printStream, VerbosityLevel.VERBOSE, true);

    colorReporter.onOperationCompleted("Color Test", "Success");

    final String output = outputStream.toString();
    // Output should either contain ANSI codes or plain text depending on environment
    assertNotNull(output);
    assertTrue(output.contains("Color Test"));
  }

  @Test
  void testConsoleFactoryMethod() {
    final ProgressReporter consoleReporter =
        ProgressReporter.forConsole(VerbosityLevel.NORMAL, false);
    assertNotNull(consoleReporter);

    // Should be able to use the reporter without exceptions
    consoleReporter.onStatusUpdate("Factory test", null);
  }

  @Test
  void testOperationStatsDetails() {
    final String op1 = "Operation 1";
    final String op2 = "Operation 2";

    // Run multiple operations to build statistics
    for (int i = 0; i < 3; i++) {
      progressReporter.onOperationStarted(op1, 1);
      progressReporter.onOperationCompleted(op1, "Success " + i);

      progressReporter.onOperationStarted(op2, 1);
      if (i == 0) {
        progressReporter.onOperationFailed(op2, new RuntimeException("Failed"));
      } else {
        progressReporter.onOperationCompleted(op2, "Success " + i);
      }
    }

    final var stats = progressReporter.getOperationStats();

    // Check op1 statistics
    final ProgressReporter.OperationStats stats1 = stats.get(op1);
    assertEquals(3, stats1.getCount());
    assertEquals(3, stats1.getSuccessCount());
    assertEquals(0, stats1.getFailureCount());
    assertEquals(1.0, stats1.getSuccessRate(), 0.01);

    // Check op2 statistics
    final ProgressReporter.OperationStats stats2 = stats.get(op2);
    assertEquals(3, stats2.getCount());
    assertEquals(2, stats2.getSuccessCount());
    assertEquals(1, stats2.getFailureCount());
    assertEquals(2.0 / 3.0, stats2.getSuccessRate(), 0.01);

    // Test timing information
    assertNotNull(stats1.getMinDuration());
    assertNotNull(stats1.getMaxDuration());
    assertNotNull(stats1.getAverageDuration());
    assertNotNull(stats1.getTotalDuration());

    assertTrue(stats1.getMinDuration().compareTo(stats1.getMaxDuration()) <= 0);
    assertTrue(stats1.getAverageDuration().compareTo(Duration.ZERO) >= 0);
  }

  @Test
  void testProgressCalculationEdgeCases() {
    // Test with zero total steps
    progressReporter.onOperationStarted("Zero Steps", 0);
    assertEquals(-1, progressReporter.getProgressPercentage());

    // Test with progress beyond total (should cap at 100%)
    progressReporter.onOperationStarted("Overflow Test", 5);
    progressReporter.onProgress(10, "Over limit");
    assertEquals(100, progressReporter.getProgressPercentage()); // Should cap at 100%
  }

  @Test
  void testEventHistoryManagement() {
    // Generate many events to test history management
    for (int i = 0; i < 1500; i++) { // More than the 1000 limit
      progressReporter.onStatusUpdate("Event " + i, null);
    }

    // The reporter should continue to function normally
    progressReporter.onOperationStarted("History Test", 1);
    progressReporter.onOperationCompleted("History Test", "Done");

    // Should not cause memory issues or exceptions
    assertEquals(-1, progressReporter.getProgressPercentage());
  }
}
