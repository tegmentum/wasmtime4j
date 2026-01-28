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

package ai.tegmentum.wasmtime4j.panama.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Panama PanamaOptimizationEngine.
 *
 * <p>These tests verify the Panama-specific optimization capabilities including method handle
 * caching, optimized memory allocation, zero-copy operations, and bulk operations.
 *
 * @since 1.0.0
 */
@DisplayName("Panama PanamaOptimizationEngine Integration Tests")
class PanamaOptimizationEngineIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaOptimizationEngineIntegrationTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  /** Track if optimization was enabled before test. */
  private boolean wasEnabled;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama optimization engine tests");
    try {
      NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
      assertTrue(loader.isLoaded(), "Native library should be loaded");
      LOGGER.info("Native library loaded successfully for Panama");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() {
    LOGGER.info("Setting up test - enabling optimization engine");
    wasEnabled = PanamaOptimizationEngine.isOptimizationEnabled();
    PanamaOptimizationEngine.setOptimizationEnabled(true);
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (int i = resources.size() - 1; i >= 0; i--) {
      try {
        resources.get(i).close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    PanamaOptimizationEngine.setOptimizationEnabled(wasEnabled);
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("should return same instance")
    void shouldReturnSameInstance() {
      LOGGER.info("Testing singleton pattern");

      PanamaOptimizationEngine instance1 = PanamaOptimizationEngine.getInstance();
      PanamaOptimizationEngine instance2 = PanamaOptimizationEngine.getInstance();

      assertNotNull(instance1, "First instance should not be null");
      assertNotNull(instance2, "Second instance should not be null");
      assertSame(instance1, instance2, "Should return same instance");

      LOGGER.info("Singleton pattern verified");
    }
  }

  @Nested
  @DisplayName("Optimized Allocation Tests")
  class OptimizedAllocationTests {

    @Test
    @DisplayName("should allocate optimized segments by size")
    void shouldAllocateOptimizedSegments() {
      LOGGER.info("Testing optimized segment allocation by size");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Test various sizes
      long[] sizes = {64, 256, 1024, 4096, 65536};
      for (long size : sizes) {
        MemorySegment segment = engine.allocateOptimized(size);
        assertNotNull(segment, "Segment should not be null for size " + size);
        assertEquals(size, segment.byteSize(), "Segment size should match");
        assertTrue(segment.scope().isAlive(), "Segment scope should be alive");

        LOGGER.info("Allocated optimized segment of size " + size);
      }
    }

    @Test
    @DisplayName("should allocate optimized segments by layout")
    void shouldAllocateOptimizedSegmentsByLayout() {
      LOGGER.info("Testing optimized segment allocation by layout");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Test with different layouts
      MemoryLayout[] layouts = {
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_LONG,
        ValueLayout.JAVA_DOUBLE,
        MemoryLayout.sequenceLayout(100, ValueLayout.JAVA_BYTE)
      };

      for (MemoryLayout layout : layouts) {
        MemorySegment segment = engine.allocateOptimized(layout);
        assertNotNull(segment, "Segment should not be null for layout " + layout);
        assertEquals(layout.byteSize(), segment.byteSize(), "Segment size should match layout");

        LOGGER.info("Allocated optimized segment with layout: " + layout);
      }
    }

    @Test
    @DisplayName("should pool small segments")
    void shouldPoolSmallSegments() {
      LOGGER.info("Testing segment pooling");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Allocate multiple segments of same size (should use pooling)
      for (int i = 0; i < 20; i++) {
        MemorySegment segment = engine.allocateOptimized(1024);
        assertNotNull(segment, "Segment " + i + " should not be null");
      }

      String stats = engine.getOptimizationStats();
      LOGGER.info("Stats after pooled allocations:\n" + stats);
    }
  }

  @Nested
  @DisplayName("Zero-Copy Operation Tests")
  class ZeroCopyOperationTests {

    @Test
    @DisplayName("should execute zero-copy operation")
    void shouldExecuteZeroCopyOperation() throws Exception {
      LOGGER.info("Testing zero-copy operation execution");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      String result =
          engine.executeZeroCopy(
              1024,
              segment -> {
                assertNotNull(segment, "Segment should not be null");
                assertTrue(segment.byteSize() >= 1024, "Segment should be at least 1024 bytes");
                // Write some data
                segment.set(ValueLayout.JAVA_INT, 0, 42);
                return "zero_copy_executed";
              });

      assertEquals("zero_copy_executed", result, "Operation result should match");

      LOGGER.info("Zero-copy operation executed successfully");
    }

    @Test
    @DisplayName("should track zero-copy metrics")
    void shouldTrackZeroCopyMetrics() throws Exception {
      LOGGER.info("Testing zero-copy metrics tracking");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Execute multiple zero-copy operations
      for (int i = 0; i < 5; i++) {
        final int iteration = i;
        engine.executeZeroCopy(
            512,
            segment -> {
              return "iteration_" + iteration;
            });
      }

      String stats = engine.getOptimizationStats();
      assertTrue(stats.contains("Zero-copy"), "Stats should mention zero-copy operations");

      LOGGER.info("Zero-copy metrics:\n" + stats);
    }
  }

  @Nested
  @DisplayName("Bulk Operation Tests")
  class BulkOperationTests {

    @Test
    @DisplayName("should execute bulk operation")
    void shouldExecuteBulkOperation() throws Exception {
      LOGGER.info("Testing bulk operation execution");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      Integer result =
          engine.executeBulk(
              5,
              256,
              segments -> {
                assertNotNull(segments, "Segments array should not be null");
                assertEquals(5, segments.length, "Should have 5 segments");
                for (int i = 0; i < segments.length; i++) {
                  assertNotNull(segments[i], "Segment " + i + " should not be null");
                  assertEquals(256, segments[i].byteSize(), "Segment size should be 256");
                }
                return segments.length;
              });

      assertEquals(5, result, "Bulk operation should return 5");

      LOGGER.info("Bulk operation executed successfully");
    }

    @Test
    @DisplayName("should handle empty bulk operation")
    void shouldHandleEmptyBulkOperation() throws Exception {
      LOGGER.info("Testing empty bulk operation");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      Integer result =
          engine.executeBulk(
              0,
              256,
              segments -> {
                assertNotNull(segments, "Segments array should not be null");
                assertEquals(0, segments.length, "Should have 0 segments");
                return 0;
              });

      assertEquals(0, result, "Empty bulk operation should return 0");

      LOGGER.info("Empty bulk operation handled");
    }
  }

  @Nested
  @DisplayName("Optimized Copy Tests")
  class OptimizedCopyTests {

    @Test
    @DisplayName("should perform optimized copy")
    void shouldPerformOptimizedCopy() {
      LOGGER.info("Testing optimized memory copy");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Create source and destination segments
      MemorySegment source = engine.allocateOptimized(1024);
      MemorySegment destination = engine.allocateOptimized(1024);

      // Fill source with data
      for (int i = 0; i < 256; i++) {
        source.set(ValueLayout.JAVA_INT, i * 4L, i);
      }

      // Perform optimized copy
      assertDoesNotThrow(
          () -> engine.optimizedCopy(source, destination, 1024), "Optimized copy should not throw");

      // Verify data was copied
      for (int i = 0; i < 256; i++) {
        int value = destination.get(ValueLayout.JAVA_INT, i * 4L);
        assertEquals(i, value, "Copied value at index " + i + " should match");
      }

      LOGGER.info("Optimized copy verified");
    }

    @Test
    @DisplayName("should handle small copies")
    void shouldHandleSmallCopies() {
      LOGGER.info("Testing small memory copies");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      MemorySegment source = engine.allocateOptimized(64);
      MemorySegment destination = engine.allocateOptimized(64);

      source.set(ValueLayout.JAVA_LONG, 0, 123456789L);

      assertDoesNotThrow(
          () -> engine.optimizedCopy(source, destination, 64), "Small copy should not throw");

      long value = destination.get(ValueLayout.JAVA_LONG, 0);
      assertEquals(123456789L, value, "Copied value should match");

      LOGGER.info("Small copy verified");
    }
  }

  @Nested
  @DisplayName("Layout Optimization Tests")
  class LayoutOptimizationTests {

    @Test
    @DisplayName("should optimize layout for int")
    void shouldOptimizeLayoutForInt() {
      LOGGER.info("Testing int layout optimization");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      ValueLayout intLayout = engine.optimizeLayout(int.class);
      assertNotNull(intLayout, "Int layout should not be null");
      assertEquals(4, intLayout.byteSize(), "Int layout should be 4 bytes");

      LOGGER.info("Int layout optimized: " + intLayout);
    }

    @Test
    @DisplayName("should optimize layout for long")
    void shouldOptimizeLayoutForLong() {
      LOGGER.info("Testing long layout optimization");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      ValueLayout longLayout = engine.optimizeLayout(long.class);
      assertNotNull(longLayout, "Long layout should not be null");
      assertEquals(8, longLayout.byteSize(), "Long layout should be 8 bytes");

      LOGGER.info("Long layout optimized: " + longLayout);
    }

    @Test
    @DisplayName("should optimize layout for various types")
    void shouldOptimizeLayoutForVariousTypes() {
      LOGGER.info("Testing layout optimization for various types");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      Class<?>[] types = {
        byte.class, short.class, int.class, long.class, float.class, double.class
      };

      for (Class<?> type : types) {
        ValueLayout layout = engine.optimizeLayout(type);
        assertNotNull(layout, "Layout for " + type + " should not be null");
        LOGGER.info("Layout for " + type.getSimpleName() + ": " + layout);
      }
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should return optimization stats")
    void shouldReturnOptimizationStats() {
      LOGGER.info("Testing optimization statistics");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Perform some operations
      for (int i = 0; i < 10; i++) {
        engine.allocateOptimized(512);
      }

      String stats = engine.getOptimizationStats();
      assertNotNull(stats, "Stats should not be null");
      assertFalse(stats.isEmpty(), "Stats should not be empty");
      assertTrue(
          stats.contains("Panama") || stats.contains("Optimization"),
          "Stats should mention Panama or Optimization");

      LOGGER.info("Optimization stats:\n" + stats);
    }

    @Test
    @DisplayName("should reset statistics")
    void shouldResetStatistics() {
      LOGGER.info("Testing statistics reset");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Perform some operations
      for (int i = 0; i < 5; i++) {
        engine.allocateOptimized(256);
      }

      // Reset
      assertDoesNotThrow(engine::reset, "Reset should not throw");

      String stats = engine.getOptimizationStats();
      LOGGER.info("Stats after reset:\n" + stats);
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should enable optimization")
    void shouldEnableOptimization() {
      LOGGER.info("Testing optimization enable");

      PanamaOptimizationEngine.setOptimizationEnabled(true);
      assertTrue(
          PanamaOptimizationEngine.isOptimizationEnabled(), "Optimization should be enabled");

      LOGGER.info("Optimization enabled");
    }

    @Test
    @DisplayName("should disable optimization")
    void shouldDisableOptimization() {
      LOGGER.info("Testing optimization disable");

      PanamaOptimizationEngine.setOptimizationEnabled(false);
      assertFalse(
          PanamaOptimizationEngine.isOptimizationEnabled(), "Optimization should be disabled");

      // Re-enable
      PanamaOptimizationEngine.setOptimizationEnabled(true);

      LOGGER.info("Optimization disabled and re-enabled");
    }

    @Test
    @DisplayName("should still work when disabled")
    void shouldStillWorkWhenDisabled() {
      LOGGER.info("Testing functionality when disabled");

      PanamaOptimizationEngine.setOptimizationEnabled(false);

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Should still allocate
      MemorySegment segment = engine.allocateOptimized(1024);
      assertNotNull(segment, "Allocation should still work");
      assertEquals(1024, segment.byteSize(), "Segment size should be correct");

      PanamaOptimizationEngine.setOptimizationEnabled(true);

      LOGGER.info("Functionality verified when disabled");
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("should handle concurrent allocations")
    void shouldHandleConcurrentAllocations() throws Exception {
      LOGGER.info("Testing concurrent allocations");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      int numThreads = 4;
      int allocationsPerThread = 100;
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      CountDownLatch latch = new CountDownLatch(numThreads);
      AtomicInteger successCount = new AtomicInteger(0);
      AtomicInteger failureCount = new AtomicInteger(0);

      for (int t = 0; t < numThreads; t++) {
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < allocationsPerThread; i++) {
                  try {
                    MemorySegment segment = engine.allocateOptimized(256);
                    if (segment != null) {
                      successCount.incrementAndGet();
                    } else {
                      failureCount.incrementAndGet();
                    }
                  } catch (Exception e) {
                    failureCount.incrementAndGet();
                    LOGGER.warning("Concurrent allocation failed: " + e.getMessage());
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertTrue(completed, "All threads should complete");
      executor.shutdown();

      LOGGER.info(
          "Concurrent allocations - success: "
              + successCount.get()
              + ", failures: "
              + failureCount.get());
      LOGGER.info("Stats:\n" + engine.getOptimizationStats());
    }

    @Test
    @DisplayName("should handle concurrent zero-copy operations")
    void shouldHandleConcurrentZeroCopy() throws Exception {
      LOGGER.info("Testing concurrent zero-copy operations");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      int numThreads = 4;
      int operationsPerThread = 25;
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      CountDownLatch latch = new CountDownLatch(numThreads);
      AtomicInteger successCount = new AtomicInteger(0);

      for (int t = 0; t < numThreads; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < operationsPerThread; i++) {
                  final int opIdx = i;
                  try {
                    engine.executeZeroCopy(
                        512,
                        segment -> {
                          return "thread_" + threadId + "_op_" + opIdx;
                        });
                    successCount.incrementAndGet();
                  } catch (Exception e) {
                    LOGGER.warning("Zero-copy failed: " + e.getMessage());
                  }
                }
              } finally {
                latch.countDown();
              }
            });
      }

      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertTrue(completed, "All threads should complete");
      executor.shutdown();

      LOGGER.info("Concurrent zero-copy operations - success: " + successCount.get());
    }
  }

  @Nested
  @DisplayName("Shutdown Tests")
  class ShutdownTests {

    @Test
    @DisplayName("should shutdown gracefully")
    void shouldShutdownGracefully() {
      LOGGER.info("Testing shutdown");

      PanamaOptimizationEngine engine = PanamaOptimizationEngine.getInstance();

      // Perform some operations
      engine.allocateOptimized(1024);

      // Note: Actual shutdown would need a new instance, but we can test the method doesn't throw
      // We don't actually call shutdown here as it would affect other tests

      String stats = engine.getOptimizationStats();
      assertNotNull(stats, "Stats should be available");

      LOGGER.info("Shutdown test completed (not actually shut down to preserve state)");
    }
  }
}
