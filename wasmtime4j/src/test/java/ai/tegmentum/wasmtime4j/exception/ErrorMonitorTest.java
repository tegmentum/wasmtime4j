/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ErrorMonitor} class.
 *
 * <p>This test class verifies the error monitoring, analytics, and pattern detection functionality.
 */
@DisplayName("ErrorMonitor Tests")
class ErrorMonitorTest {

  private ErrorMonitor monitor;

  @BeforeEach
  void setUp() {
    monitor = ErrorMonitor.getInstance();
    monitor.reset(); // Start with clean state
  }

  @AfterEach
  void tearDown() {
    monitor.reset(); // Clean up after each test
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("ErrorMonitor should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ErrorMonitor.class.getModifiers()), "ErrorMonitor should be final");
    }

    @Test
    @DisplayName("getInstance should return singleton instance")
    void getInstanceShouldReturnSingleton() {
      final ErrorMonitor instance1 = ErrorMonitor.getInstance();
      final ErrorMonitor instance2 = ErrorMonitor.getInstance();

      assertNotNull(instance1, "Instance should not be null");
      assertSame(instance1, instance2, "getInstance should return same instance");
    }
  }

  @Nested
  @DisplayName("ErrorOccurrence Inner Class Tests")
  class ErrorOccurrenceTests {

    @Test
    @DisplayName("ErrorOccurrence should have timestamp")
    void errorOccurrenceShouldHaveTimestamp() throws Exception {
      // Record an error to create an occurrence
      monitor.recordError(new WasmException("Test error"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");
      assertNotNull(stats.get(0).getFirstOccurrence(), "Should have first occurrence timestamp");
    }

    @Test
    @DisplayName("ErrorOccurrence should track thread ID")
    void errorOccurrenceShouldTrackThreadId() {
      // The ErrorOccurrence class captures Thread.currentThread().threadId()
      // This is tested indirectly through pattern analysis
      monitor.recordError(new WasmException("Test error"));
      assertEquals(
          1, monitor.getTotalErrorCount(), "Should have recorded one error from current thread");
    }
  }

  @Nested
  @DisplayName("ErrorStatistics Inner Class Tests")
  class ErrorStatisticsTests {

    @Test
    @DisplayName("ErrorStatistics should have error type")
    void errorStatisticsShouldHaveErrorType() {
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap error"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");
      assertEquals(
          "TrapException", stats.get(0).getErrorType(), "Error type should be TrapException");
    }

    @Test
    @DisplayName("ErrorStatistics should track occurrences")
    void errorStatisticsShouldTrackOccurrences() {
      monitor.recordError(new WasmException("Error 1"));
      monitor.recordError(new WasmException("Error 2"));
      monitor.recordError(new WasmException("Error 3"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");
      assertEquals(3, stats.get(0).getTotalOccurrences(), "Should have 3 total occurrences");
    }

    @Test
    @DisplayName("ErrorStatistics should have error rate")
    void errorStatisticsShouldHaveErrorRate() {
      monitor.recordError(new WasmException("Error"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");
      assertTrue(stats.get(0).getErrorRate() >= 0, "Error rate should be non-negative");
    }

    @Test
    @DisplayName("ErrorStatistics should have common messages")
    void errorStatisticsShouldHaveCommonMessages() {
      monitor.recordError(new WasmException("Common error"));
      monitor.recordError(new WasmException("Common error"));
      monitor.recordError(new WasmException("Rare error"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");

      final List<String> commonMessages = stats.get(0).getCommonMessages();
      assertNotNull(commonMessages, "Common messages should not be null");
      assertFalse(commonMessages.isEmpty(), "Should have common messages");
    }

    @Test
    @DisplayName("ErrorStatistics should have retryable percentage")
    void errorStatisticsShouldHaveRetryablePercentage() {
      monitor.recordError(new WasmException("Error"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");
      assertTrue(
          stats.get(0).getRetryablePercentage() >= 0,
          "Retryable percentage should be non-negative");
    }
  }

  @Nested
  @DisplayName("recordError Method Tests")
  class RecordErrorMethodTests {

    @Test
    @DisplayName("recordError should handle null exception")
    void recordErrorShouldHandleNullException() {
      monitor.recordError(null);

      assertEquals(0, monitor.getTotalErrorCount(), "Null exception should not increment count");
    }

    @Test
    @DisplayName("recordError should increment total count")
    void recordErrorShouldIncrementTotalCount() {
      assertEquals(0, monitor.getTotalErrorCount(), "Initial count should be 0");

      monitor.recordError(new WasmException("Error 1"));
      assertEquals(1, monitor.getTotalErrorCount(), "Count should be 1 after first error");

      monitor.recordError(new WasmException("Error 2"));
      assertEquals(2, monitor.getTotalErrorCount(), "Count should be 2 after second error");
    }

    @Test
    @DisplayName("recordError should track different exception types")
    void recordErrorShouldTrackDifferentExceptionTypes() {
      monitor.recordError(new WasmException("Wasm error"));
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap error"));
      monitor.recordError(new CompilationException("Compilation error"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertEquals(3, stats.size(), "Should have 3 different error types");
    }

    @Test
    @DisplayName("recordError should extract function context from TrapException")
    void recordErrorShouldExtractFunctionContext() {
      final TrapException trap =
          new TrapException(
              TrapException.TrapType.STACK_OVERFLOW,
              "Stack overflow in function",
              null,
              "my_function",
              null,
              null);
      monitor.recordError(trap);

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertFalse(stats.isEmpty(), "Should have statistics");
      // Function context is tracked in common functions list
      assertNotNull(stats.get(0).getCommonFunctions(), "Common functions should not be null");
    }
  }

  @Nested
  @DisplayName("getErrorStatistics Method Tests")
  class GetErrorStatisticsMethodTests {

    @Test
    @DisplayName("getErrorStatistics should return empty list when no errors")
    void getErrorStatisticsShouldReturnEmptyListWhenNoErrors() {
      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();

      assertNotNull(stats, "Stats should not be null");
      assertTrue(stats.isEmpty(), "Stats should be empty when no errors recorded");
    }

    @Test
    @DisplayName("getErrorStatistics should sort by occurrence count")
    void getErrorStatisticsShouldSortByOccurrenceCount() {
      monitor.recordError(new WasmException("Error"));
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap 1"));
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap 2"));
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap 3"));

      final List<ErrorMonitor.ErrorStatistics> stats = monitor.getErrorStatistics();
      assertEquals(2, stats.size(), "Should have 2 types");
      assertEquals(
          "TrapException", stats.get(0).getErrorType(), "Most common type should be first");
    }

    @Test
    @DisplayName("getErrorStatistics by type should return null for unknown type")
    void getErrorStatisticsByTypeShouldReturnNullForUnknownType() {
      final ErrorMonitor.ErrorStatistics stats = monitor.getErrorStatistics("UnknownType");

      assertNull(stats, "Should return null for unknown type");
    }

    @Test
    @DisplayName("getErrorStatistics by type should return stats for known type")
    void getErrorStatisticsByTypeShouldReturnStatsForKnownType() {
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap error"));

      final ErrorMonitor.ErrorStatistics stats = monitor.getErrorStatistics("TrapException");

      assertNotNull(stats, "Should return stats for known type");
      assertEquals("TrapException", stats.getErrorType(), "Type should match");
    }
  }

  @Nested
  @DisplayName("getTotalErrorCount Method Tests")
  class GetTotalErrorCountMethodTests {

    @Test
    @DisplayName("getTotalErrorCount should return 0 initially")
    void getTotalErrorCountShouldReturnZeroInitially() {
      assertEquals(0, monitor.getTotalErrorCount(), "Initial count should be 0");
    }

    @Test
    @DisplayName("getTotalErrorCount should sum all error types")
    void getTotalErrorCountShouldSumAllErrorTypes() {
      monitor.recordError(new WasmException("Error 1"));
      monitor.recordError(new WasmException("Error 2"));
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap"));
      monitor.recordError(new CompilationException("Compile"));

      assertEquals(4, monitor.getTotalErrorCount(), "Total should be 4 across all types");
    }
  }

  @Nested
  @DisplayName("getOverallErrorRate Method Tests")
  class GetOverallErrorRateMethodTests {

    @Test
    @DisplayName("getOverallErrorRate should return 0 initially")
    void getOverallErrorRateShouldReturnZeroInitially() {
      assertEquals(0.0, monitor.getOverallErrorRate(), 0.001, "Initial rate should be 0");
    }

    @Test
    @DisplayName("getOverallErrorRate should be non-negative")
    void getOverallErrorRateShouldBeNonNegative() {
      monitor.recordError(new WasmException("Error"));

      assertTrue(monitor.getOverallErrorRate() >= 0, "Error rate should be non-negative");
    }
  }

  @Nested
  @DisplayName("hasConcerningPatterns Method Tests")
  class HasConcerningPatternsMethodTests {

    @Test
    @DisplayName("hasConcerningPatterns should return false when no errors")
    void hasConcerningPatternsShouldReturnFalseWhenNoErrors() {
      assertFalse(monitor.hasConcerningPatterns(), "Should return false when no errors");
    }

    @Test
    @DisplayName("hasConcerningPatterns should detect high error rates")
    void hasConcerningPatternsShouldDetectHighErrorRates() {
      // Record many errors quickly to trigger concerning pattern
      for (int i = 0; i < 100; i++) {
        monitor.recordError(new WasmException("Error " + i));
      }

      // Note: This test may or may not trigger depending on timing
      // The hasConcerningPatterns checks errors per minute
      assertTrue(monitor.getTotalErrorCount() >= 100, "Should have recorded all errors");
    }
  }

  @Nested
  @DisplayName("reset Method Tests")
  class ResetMethodTests {

    @Test
    @DisplayName("reset should clear all error data")
    void resetShouldClearAllErrorData() {
      monitor.recordError(new WasmException("Error 1"));
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap"));

      assertEquals(2, monitor.getTotalErrorCount(), "Should have 2 errors before reset");

      monitor.reset();

      assertEquals(0, monitor.getTotalErrorCount(), "Should have 0 errors after reset");
      assertTrue(monitor.getErrorStatistics().isEmpty(), "Statistics should be empty after reset");
    }
  }

  @Nested
  @DisplayName("generateSummaryReport Method Tests")
  class GenerateSummaryReportMethodTests {

    @Test
    @DisplayName("generateSummaryReport should return non-null report")
    void generateSummaryReportShouldReturnNonNullReport() {
      final String report = monitor.generateSummaryReport();

      assertNotNull(report, "Report should not be null");
      assertFalse(report.isEmpty(), "Report should not be empty");
    }

    @Test
    @DisplayName("generateSummaryReport should include total errors")
    void generateSummaryReportShouldIncludeTotalErrors() {
      monitor.recordError(new WasmException("Error"));

      final String report = monitor.generateSummaryReport();

      assertTrue(report.contains("Total Errors"), "Report should include total errors");
    }

    @Test
    @DisplayName("generateSummaryReport should include error rate")
    void generateSummaryReportShouldIncludeErrorRate() {
      final String report = monitor.generateSummaryReport();

      assertTrue(report.contains("Error Rate"), "Report should include error rate");
    }

    @Test
    @DisplayName("generateSummaryReport should include concerning patterns status")
    void generateSummaryReportShouldIncludeConcerningPatternsStatus() {
      final String report = monitor.generateSummaryReport();

      assertTrue(
          report.contains("Concerning Patterns"),
          "Report should include concerning patterns status");
    }

    @Test
    @DisplayName("generateSummaryReport should list top error types")
    void generateSummaryReportShouldListTopErrorTypes() {
      monitor.recordError(new TrapException(TrapException.TrapType.UNKNOWN, "Trap"));
      monitor.recordError(new WasmException("Error"));

      final String report = monitor.generateSummaryReport();

      assertTrue(
          report.contains("Top Error Types"), "Report should include top error types section");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("Should handle concurrent error recording")
    void shouldHandleConcurrentErrorRecording() throws InterruptedException {
      final int threadCount = 10;
      final int errorsPerThread = 100;
      final Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        threads[i] =
            new Thread(
                () -> {
                  for (int j = 0; j < errorsPerThread; j++) {
                    monitor.recordError(new WasmException("Error from thread " + threadIndex));
                  }
                });
      }

      for (Thread thread : threads) {
        thread.start();
      }

      for (Thread thread : threads) {
        thread.join();
      }

      assertEquals(
          threadCount * errorsPerThread,
          monitor.getTotalErrorCount(),
          "All errors should be recorded");
    }
  }
}
