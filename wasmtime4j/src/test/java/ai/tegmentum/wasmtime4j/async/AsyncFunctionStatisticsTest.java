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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for AsyncFunctionStatistics class.
 *
 * <p>Verifies statistics tracking for asynchronous function execution.
 */
@DisplayName("AsyncFunctionStatistics Tests")
class AsyncFunctionStatisticsTest {

  private AsyncFunctionStatistics stats;

  @BeforeEach
  void setUp() {
    stats = new AsyncFunctionStatistics();
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create statistics with zero initial values")
    void shouldCreateStatisticsWithZeroInitialValues() {
      AsyncFunctionStatistics newStats = new AsyncFunctionStatistics();

      assertNotNull(newStats, "Stats should not be null");
      assertEquals(0L, newStats.getTotalCalls(), "Total calls should be 0");
      assertEquals(0L, newStats.getSuccessfulCalls(), "Successful calls should be 0");
      assertEquals(0L, newStats.getFailedCalls(), "Failed calls should be 0");
      assertEquals(0L, newStats.getTimeoutCalls(), "Timeout calls should be 0");
      assertEquals(0.0, newStats.getSuccessRate(), 0.001, "Success rate should be 0");
      assertEquals(Duration.ZERO, newStats.getAverageExecutionTime(), "Average time should be 0");
      assertEquals(Duration.ZERO, newStats.getMinExecutionTime(), "Min time should be 0");
      assertEquals(Duration.ZERO, newStats.getMaxExecutionTime(), "Max time should be 0");
    }
  }

  @Nested
  @DisplayName("RecordSuccess Tests")
  class RecordSuccessTests {

    @Test
    @DisplayName("should record successful call")
    void shouldRecordSuccessfulCall() {
      stats.recordSuccess(100L);

      assertEquals(1L, stats.getTotalCalls(), "Total calls should be 1");
      assertEquals(1L, stats.getSuccessfulCalls(), "Successful calls should be 1");
      assertEquals(0L, stats.getFailedCalls(), "Failed calls should be 0");
      assertEquals(0L, stats.getTimeoutCalls(), "Timeout calls should be 0");
    }

    @Test
    @DisplayName("should update execution times")
    void shouldUpdateExecutionTimes() {
      stats.recordSuccess(100L);

      assertEquals(
          Duration.ofMillis(100), stats.getAverageExecutionTime(), "Average should be 100ms");
      assertEquals(Duration.ofMillis(100), stats.getMinExecutionTime(), "Min should be 100ms");
      assertEquals(Duration.ofMillis(100), stats.getMaxExecutionTime(), "Max should be 100ms");
    }

    @Test
    @DisplayName("should track multiple successful calls")
    void shouldTrackMultipleSuccessfulCalls() {
      stats.recordSuccess(50L);
      stats.recordSuccess(100L);
      stats.recordSuccess(150L);

      assertEquals(3L, stats.getTotalCalls(), "Total calls should be 3");
      assertEquals(3L, stats.getSuccessfulCalls(), "Successful calls should be 3");
      assertEquals(
          Duration.ofMillis(100), stats.getAverageExecutionTime(), "Average should be 100ms");
      assertEquals(Duration.ofMillis(50), stats.getMinExecutionTime(), "Min should be 50ms");
      assertEquals(Duration.ofMillis(150), stats.getMaxExecutionTime(), "Max should be 150ms");
    }
  }

  @Nested
  @DisplayName("RecordFailure Tests")
  class RecordFailureTests {

    @Test
    @DisplayName("should record failed call")
    void shouldRecordFailedCall() {
      stats.recordFailure(50L);

      assertEquals(1L, stats.getTotalCalls(), "Total calls should be 1");
      assertEquals(0L, stats.getSuccessfulCalls(), "Successful calls should be 0");
      assertEquals(1L, stats.getFailedCalls(), "Failed calls should be 1");
      assertEquals(0L, stats.getTimeoutCalls(), "Timeout calls should be 0");
    }

    @Test
    @DisplayName("should update execution times for failure")
    void shouldUpdateExecutionTimesForFailure() {
      stats.recordFailure(75L);

      assertEquals(
          Duration.ofMillis(75), stats.getAverageExecutionTime(), "Average should be 75ms");
      assertEquals(Duration.ofMillis(75), stats.getMinExecutionTime(), "Min should be 75ms");
      assertEquals(Duration.ofMillis(75), stats.getMaxExecutionTime(), "Max should be 75ms");
    }

    @Test
    @DisplayName("should track multiple failed calls")
    void shouldTrackMultipleFailedCalls() {
      stats.recordFailure(25L);
      stats.recordFailure(75L);

      assertEquals(2L, stats.getTotalCalls(), "Total calls should be 2");
      assertEquals(2L, stats.getFailedCalls(), "Failed calls should be 2");
    }
  }

  @Nested
  @DisplayName("RecordTimeout Tests")
  class RecordTimeoutTests {

    @Test
    @DisplayName("should record timed-out call")
    void shouldRecordTimedOutCall() {
      stats.recordTimeout(5000L);

      assertEquals(1L, stats.getTotalCalls(), "Total calls should be 1");
      assertEquals(0L, stats.getSuccessfulCalls(), "Successful calls should be 0");
      assertEquals(0L, stats.getFailedCalls(), "Failed calls should be 0");
      assertEquals(1L, stats.getTimeoutCalls(), "Timeout calls should be 1");
    }

    @Test
    @DisplayName("should update execution times for timeout")
    void shouldUpdateExecutionTimesForTimeout() {
      stats.recordTimeout(5000L);

      assertEquals(
          Duration.ofMillis(5000), stats.getAverageExecutionTime(), "Average should be 5000ms");
      assertEquals(Duration.ofMillis(5000), stats.getMaxExecutionTime(), "Max should be 5000ms");
    }
  }

  @Nested
  @DisplayName("GetSuccessRate Tests")
  class GetSuccessRateTests {

    @Test
    @DisplayName("should return zero when no calls")
    void shouldReturnZeroWhenNoCalls() {
      assertEquals(0.0, stats.getSuccessRate(), 0.001, "Success rate should be 0");
    }

    @Test
    @DisplayName("should return 1.0 when all successful")
    void shouldReturnOneWhenAllSuccessful() {
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);

      assertEquals(1.0, stats.getSuccessRate(), 0.001, "Success rate should be 1.0");
    }

    @Test
    @DisplayName("should return 0.0 when all failed")
    void shouldReturnZeroWhenAllFailed() {
      stats.recordFailure(100L);
      stats.recordFailure(100L);

      assertEquals(0.0, stats.getSuccessRate(), 0.001, "Success rate should be 0.0");
    }

    @Test
    @DisplayName("should calculate mixed success rate")
    void shouldCalculateMixedSuccessRate() {
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);
      stats.recordFailure(100L);
      stats.recordTimeout(100L);

      // 2 successful out of 4 total = 0.5
      assertEquals(0.5, stats.getSuccessRate(), 0.001, "Success rate should be 0.5");
    }
  }

  @Nested
  @DisplayName("GetAverageExecutionTime Tests")
  class GetAverageExecutionTimeTests {

    @Test
    @DisplayName("should return zero when no calls")
    void shouldReturnZeroWhenNoCalls() {
      assertEquals(Duration.ZERO, stats.getAverageExecutionTime(), "Average should be 0");
    }

    @Test
    @DisplayName("should calculate average correctly")
    void shouldCalculateAverageCorrectly() {
      stats.recordSuccess(100L);
      stats.recordSuccess(200L);
      stats.recordSuccess(300L);

      // Average of 100, 200, 300 = 200
      assertEquals(
          Duration.ofMillis(200), stats.getAverageExecutionTime(), "Average should be 200ms");
    }
  }

  @Nested
  @DisplayName("GetMinExecutionTime Tests")
  class GetMinExecutionTimeTests {

    @Test
    @DisplayName("should return zero when no calls")
    void shouldReturnZeroWhenNoCalls() {
      assertEquals(Duration.ZERO, stats.getMinExecutionTime(), "Min should be 0");
    }

    @Test
    @DisplayName("should track minimum execution time")
    void shouldTrackMinimumExecutionTime() {
      stats.recordSuccess(200L);
      stats.recordSuccess(50L);
      stats.recordSuccess(150L);

      assertEquals(Duration.ofMillis(50), stats.getMinExecutionTime(), "Min should be 50ms");
    }
  }

  @Nested
  @DisplayName("GetMaxExecutionTime Tests")
  class GetMaxExecutionTimeTests {

    @Test
    @DisplayName("should return zero when no calls")
    void shouldReturnZeroWhenNoCalls() {
      assertEquals(Duration.ZERO, stats.getMaxExecutionTime(), "Max should be 0");
    }

    @Test
    @DisplayName("should track maximum execution time")
    void shouldTrackMaximumExecutionTime() {
      stats.recordSuccess(100L);
      stats.recordSuccess(500L);
      stats.recordSuccess(200L);

      assertEquals(Duration.ofMillis(500), stats.getMaxExecutionTime(), "Max should be 500ms");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("should reset all statistics")
    void shouldResetAllStatistics() {
      stats.recordSuccess(100L);
      stats.recordFailure(200L);
      stats.recordTimeout(5000L);

      stats.reset();

      assertEquals(0L, stats.getTotalCalls(), "Total calls should be 0");
      assertEquals(0L, stats.getSuccessfulCalls(), "Successful calls should be 0");
      assertEquals(0L, stats.getFailedCalls(), "Failed calls should be 0");
      assertEquals(0L, stats.getTimeoutCalls(), "Timeout calls should be 0");
      assertEquals(0.0, stats.getSuccessRate(), 0.001, "Success rate should be 0");
      assertEquals(Duration.ZERO, stats.getAverageExecutionTime(), "Average should be 0");
      assertEquals(Duration.ZERO, stats.getMinExecutionTime(), "Min should be 0");
      assertEquals(Duration.ZERO, stats.getMaxExecutionTime(), "Max should be 0");
    }

    @Test
    @DisplayName("should allow recording after reset")
    void shouldAllowRecordingAfterReset() {
      stats.recordSuccess(100L);
      stats.reset();
      stats.recordSuccess(200L);

      assertEquals(1L, stats.getTotalCalls(), "Total calls should be 1");
      assertEquals(
          Duration.ofMillis(200), stats.getAverageExecutionTime(), "Average should be 200ms");
    }
  }

  @Nested
  @DisplayName("Thread Safety Tests")
  class ThreadSafetyTests {

    @Test
    @DisplayName("should handle concurrent recording")
    void shouldHandleConcurrentRecording() throws InterruptedException {
      int threadCount = 10;
      int recordsPerThread = 100;
      Thread[] threads = new Thread[threadCount];

      for (int i = 0; i < threadCount; i++) {
        threads[i] =
            new Thread(
                () -> {
                  for (int j = 0; j < recordsPerThread; j++) {
                    stats.recordSuccess(100L);
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
          threadCount * recordsPerThread, stats.getTotalCalls(), "All calls should be recorded");
      assertEquals(
          threadCount * recordsPerThread,
          stats.getSuccessfulCalls(),
          "All successful calls should be recorded");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should track realistic execution scenario")
    void shouldTrackRealisticExecutionScenario() {
      // Simulate 100 calls with 90% success, 5% failure, 5% timeout
      for (int i = 0; i < 90; i++) {
        stats.recordSuccess(100L + i);
      }
      for (int i = 0; i < 5; i++) {
        stats.recordFailure(50L);
      }
      for (int i = 0; i < 5; i++) {
        stats.recordTimeout(5000L);
      }

      assertEquals(100L, stats.getTotalCalls(), "Total should be 100");
      assertEquals(90L, stats.getSuccessfulCalls(), "Successful should be 90");
      assertEquals(5L, stats.getFailedCalls(), "Failed should be 5");
      assertEquals(5L, stats.getTimeoutCalls(), "Timeout should be 5");
      assertEquals(0.9, stats.getSuccessRate(), 0.001, "Success rate should be 90%");

      // Min should be 50 (from failures)
      assertEquals(Duration.ofMillis(50), stats.getMinExecutionTime(), "Min should be 50ms");

      // Max should be 5000 (from timeouts)
      assertEquals(Duration.ofMillis(5000), stats.getMaxExecutionTime(), "Max should be 5000ms");
    }

    @Test
    @DisplayName("should calculate meaningful average")
    void shouldCalculateMeaningfulAverage() {
      // Record calls with known total
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);
      stats.recordSuccess(100L);

      Duration avg = stats.getAverageExecutionTime();
      assertEquals(Duration.ofMillis(100), avg, "Average should be exactly 100ms");
    }

    @Test
    @DisplayName("should handle large numbers")
    void shouldHandleLargeNumbers() {
      long largeTime = Long.MAX_VALUE / 2;

      stats.recordSuccess(largeTime);
      stats.recordSuccess(largeTime);

      assertTrue(stats.getTotalCalls() == 2, "Should record 2 calls");
      assertTrue(stats.getSuccessfulCalls() == 2, "Should record 2 successful calls");
    }
  }
}
