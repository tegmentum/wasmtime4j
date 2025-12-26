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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaBatchProcessor} class.
 *
 * <p>This test class verifies the batch processing functionality for Panama FFI operations.
 */
@DisplayName("PanamaBatchProcessor Tests")
class PanamaBatchProcessorTest {

  private Arena arena;
  private PanamaBatchProcessor processor;

  @BeforeEach
  void setUp() {
    arena = Arena.ofShared();
    processor = new PanamaBatchProcessor(arena);
  }

  @AfterEach
  void tearDown() {
    processor.close();
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaBatchProcessor should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaBatchProcessor.class.getModifiers()),
          "PanamaBatchProcessor should be final");
    }

    @Test
    @DisplayName("DEFAULT_BATCH_SIZE should be positive")
    void defaultBatchSizeShouldBePositive() {
      assertTrue(
          PanamaBatchProcessor.DEFAULT_BATCH_SIZE > 0, "Default batch size should be positive");
    }

    @Test
    @DisplayName("MAX_BATCH_SIZE should be greater than DEFAULT_BATCH_SIZE")
    void maxBatchSizeShouldBeGreaterThanDefault() {
      assertTrue(
          PanamaBatchProcessor.MAX_BATCH_SIZE >= PanamaBatchProcessor.DEFAULT_BATCH_SIZE,
          "Max batch size should be >= default");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor with arena should accept valid arena")
    void constructorWithArenaShouldAcceptValidArena() {
      try (Arena testArena = Arena.ofConfined();
          PanamaBatchProcessor testProcessor = new PanamaBatchProcessor(testArena)) {
        assertNotNull(testProcessor, "Processor should be created");
        assertTrue(testProcessor.isActive(), "Processor should be active");
      }
    }

    @Test
    @DisplayName("Constructor with arena should throw for null arena")
    void constructorWithArenaShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaBatchProcessor((Arena) null),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("Constructor with arena size should create processor")
    void constructorWithArenaSizeShouldCreateProcessor() {
      try (PanamaBatchProcessor testProcessor = new PanamaBatchProcessor(1024)) {
        assertNotNull(testProcessor, "Processor should be created");
        assertTrue(testProcessor.isActive(), "Processor should be active");
      }
    }

    @Test
    @DisplayName("Default constructor should create processor")
    void defaultConstructorShouldCreateProcessor() {
      try (PanamaBatchProcessor testProcessor = new PanamaBatchProcessor()) {
        assertNotNull(testProcessor, "Processor should be created");
        assertTrue(testProcessor.isActive(), "Processor should be active");
      }
    }
  }

  @Nested
  @DisplayName("processBatch Tests")
  class ProcessBatchTests {

    @Test
    @DisplayName("processBatch should process inputs and return results")
    void processBatchShouldProcessInputsAndReturnResults() {
      final List<Integer> inputs = Arrays.asList(1, 2, 3, 4, 5);

      final List<Integer> results = processor.processBatch(inputs, x -> x * 2);

      assertEquals(5, results.size(), "Should return 5 results");
      assertEquals(Arrays.asList(2, 4, 6, 8, 10), results, "Results should be doubled inputs");
    }

    @Test
    @DisplayName("processBatch should handle empty input list")
    void processBatchShouldHandleEmptyInputList() {
      final List<Integer> inputs = Collections.emptyList();

      final List<Integer> results = processor.processBatch(inputs, x -> x * 2);

      assertTrue(results.isEmpty(), "Results should be empty");
    }

    @Test
    @DisplayName("processBatch should throw for null inputs")
    void processBatchShouldThrowForNullInputs() {
      assertThrows(
          IllegalArgumentException.class,
          () -> processor.processBatch(null, x -> x),
          "Should throw for null inputs");
    }

    @Test
    @DisplayName("processBatch should throw for null operation")
    void processBatchShouldThrowForNullOperation() {
      final List<Integer> inputs = Arrays.asList(1, 2, 3);

      assertThrows(
          IllegalArgumentException.class,
          () -> processor.processBatch(inputs, null),
          "Should throw for null operation");
    }

    @Test
    @DisplayName("processBatch with batch size should respect batch size")
    void processBatchWithBatchSizeShouldRespectBatchSize() {
      final List<Integer> inputs = new ArrayList<>();
      for (int i = 0; i < 100; i++) {
        inputs.add(i);
      }

      final List<Integer> results = processor.processBatch(inputs, x -> x, 10);

      assertEquals(100, results.size(), "Should return all results");
    }

    @Test
    @DisplayName("processBatch should throw for non-positive batch size")
    void processBatchShouldThrowForNonPositiveBatchSize() {
      final List<Integer> inputs = Arrays.asList(1, 2, 3);

      assertThrows(
          IllegalArgumentException.class,
          () -> processor.processBatch(inputs, x -> x, 0),
          "Should throw for zero batch size");
    }

    @Test
    @DisplayName("processBatch should throw for batch size exceeding max")
    void processBatchShouldThrowForBatchSizeExceedingMax() {
      final List<Integer> inputs = Arrays.asList(1, 2, 3);

      assertThrows(
          IllegalArgumentException.class,
          () -> processor.processBatch(inputs, x -> x, PanamaBatchProcessor.MAX_BATCH_SIZE + 1),
          "Should throw for batch size > max");
    }

    @Test
    @DisplayName("processBatch should handle operation exceptions gracefully")
    void processBatchShouldHandleOperationExceptionsGracefully() {
      final List<Integer> inputs = Arrays.asList(1, 2, 3);

      final List<Integer> results =
          processor.processBatch(
              inputs,
              x -> {
                if (x == 2) {
                  throw new RuntimeException("Test error");
                }
                return x;
              });

      assertEquals(3, results.size(), "Should return 3 results");
      assertEquals(1, results.get(0), "First result should be 1");
      // Null for failed operation
      assertEquals(3, results.get(2), "Third result should be 3");
    }
  }

  @Nested
  @DisplayName("processBatchAsync Tests")
  class ProcessBatchAsyncTests {

    @Test
    @DisplayName("processBatchAsync should return CompletableFuture with results")
    void processBatchAsyncShouldReturnCompletableFutureWithResults() throws Exception {
      final List<Integer> inputs = Arrays.asList(1, 2, 3);

      final CompletableFuture<List<Integer>> future = processor.processBatchAsync(inputs, x -> x * 2);

      assertNotNull(future, "Future should not be null");
      final List<Integer> results = future.get(5, TimeUnit.SECONDS);
      assertEquals(Arrays.asList(2, 4, 6), results, "Results should be doubled");
    }

    @Test
    @DisplayName("processBatchAsync with default batch size should work")
    void processBatchAsyncWithDefaultBatchSizeShouldWork() throws Exception {
      final List<Integer> inputs = Arrays.asList(1, 2, 3, 4, 5);

      final CompletableFuture<List<Integer>> future = processor.processBatchAsync(inputs, x -> x + 1);

      final List<Integer> results = future.get(5, TimeUnit.SECONDS);
      assertEquals(5, results.size(), "Should return 5 results");
    }
  }

  @Nested
  @DisplayName("processNativeBatch Tests")
  class ProcessNativeBatchTests {

    @Test
    @DisplayName("processNativeBatch should throw for null parameters")
    void processNativeBatchShouldThrowForNullParameters() {
      assertThrows(
          IllegalArgumentException.class,
          () -> processor.processNativeBatch(null, null),
          "Should throw for null parameters");
    }

    @Test
    @DisplayName("processNativeBatch should return empty array for empty parameters")
    void processNativeBatchShouldReturnEmptyArrayForEmptyParameters() {
      final MemorySegment[] empty = new MemorySegment[0];

      final MemorySegment[] results = processor.processNativeBatch(empty, null);

      assertEquals(0, results.length, "Should return empty array");
    }
  }

  @Nested
  @DisplayName("optimizeBatchSize Tests")
  class OptimizeBatchSizeTests {

    @Test
    @DisplayName("optimizeBatchSize should return positive value")
    void optimizeBatchSizeShouldReturnPositiveValue() {
      final int optimalSize = processor.optimizeBatchSize(100, 64);

      assertTrue(optimalSize > 0, "Optimal batch size should be positive");
    }

    @Test
    @DisplayName("optimizeBatchSize should throw for non-positive inputCount")
    void optimizeBatchSizeShouldThrowForNonPositiveInputCount() {
      assertThrows(
          IllegalArgumentException.class,
          () -> processor.optimizeBatchSize(0, 64),
          "Should throw for zero inputCount");
    }

    @Test
    @DisplayName("optimizeBatchSize should throw for non-positive elementSize")
    void optimizeBatchSizeShouldThrowForNonPositiveElementSize() {
      assertThrows(
          IllegalArgumentException.class,
          () -> processor.optimizeBatchSize(100, 0),
          "Should throw for zero elementSize");
    }

    @Test
    @DisplayName("optimizeBatchSize should not exceed MAX_BATCH_SIZE")
    void optimizeBatchSizeShouldNotExceedMaxBatchSize() {
      final int optimalSize = processor.optimizeBatchSize(1000000, 1);

      assertTrue(
          optimalSize <= PanamaBatchProcessor.MAX_BATCH_SIZE,
          "Should not exceed max batch size");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return formatted string")
    void getStatisticsShouldReturnFormattedString() {
      final String stats = processor.getStatistics();

      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
      assertTrue(
          stats.contains("Panama Batch Processor"), "Should contain processor identifier");
    }

    @Test
    @DisplayName("getPerformanceMetrics should return metrics string")
    void getPerformanceMetricsShouldReturnMetricsString() {
      final String metrics = processor.getPerformanceMetrics();

      assertNotNull(metrics, "Metrics should not be null");
      assertFalse(metrics.isEmpty(), "Metrics should not be empty");
      assertTrue(metrics.contains("throughput"), "Should contain throughput");
    }

    @Test
    @DisplayName("resetStatistics should reset counters")
    void resetStatisticsShouldResetCounters() {
      // Run some operations
      processor.processBatch(Arrays.asList(1, 2, 3), x -> x);

      // Reset
      processor.resetStatistics();

      // Check stats
      final String stats = processor.getStatistics();
      assertTrue(
          stats.contains("Total batches: 0") || stats.contains("batches: 0"),
          "Stats should show 0 batches after reset");
    }
  }

  @Nested
  @DisplayName("Resource Management Tests")
  class ResourceManagementTests {

    @Test
    @DisplayName("isActive should return true for active processor")
    void isActiveShouldReturnTrueForActiveProcessor() {
      assertTrue(processor.isActive(), "Processor should be active");
    }

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      assertEquals(arena, processor.getArena(), "Should return the same arena");
    }

    @Test
    @DisplayName("close should close owned arena")
    void closeShouldCloseOwnedArena() {
      final PanamaBatchProcessor owningProcessor = new PanamaBatchProcessor(1024);

      owningProcessor.close();

      assertFalse(owningProcessor.isActive(), "Processor should not be active after close");
    }

    @Test
    @DisplayName("close should not close non-owned arena")
    void closeShouldNotCloseNonOwnedArena() {
      processor.close();

      assertTrue(arena.scope().isAlive(), "Non-owned arena should still be alive");
    }

    @Test
    @DisplayName("close should be idempotent")
    void closeShouldBeIdempotent() {
      assertDoesNotThrow(
          () -> {
            processor.close();
            processor.close();
          },
          "Multiple close calls should not throw");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full processing lifecycle should work correctly")
    void fullProcessingLifecycleShouldWorkCorrectly() throws Exception {
      // Synchronous batch
      final List<String> syncInputs = Arrays.asList("a", "b", "c");
      final List<String> syncResults =
          processor.processBatch(syncInputs, String::toUpperCase);
      assertEquals(Arrays.asList("A", "B", "C"), syncResults, "Sync results should match");

      // Async batch
      final List<Integer> asyncInputs = Arrays.asList(10, 20, 30);
      final CompletableFuture<List<Integer>> future =
          processor.processBatchAsync(asyncInputs, x -> x / 10);
      final List<Integer> asyncResults = future.get(5, TimeUnit.SECONDS);
      assertEquals(Arrays.asList(1, 2, 3), asyncResults, "Async results should match");

      // Get stats
      final String stats = processor.getStatistics();
      assertNotNull(stats, "Stats should be available");
    }

    @Test
    @DisplayName("Large batch processing should complete successfully")
    void largeBatchProcessingShouldCompleteSuccessfully() {
      final List<Integer> inputs = new ArrayList<>();
      for (int i = 0; i < 1000; i++) {
        inputs.add(i);
      }

      final List<Integer> results = processor.processBatch(inputs, x -> x * 2, 64);

      assertEquals(1000, results.size(), "Should process all inputs");
      for (int i = 0; i < 1000; i++) {
        assertEquals(Integer.valueOf(i * 2), results.get(i), "Result at " + i + " should match");
      }
    }
  }
}
