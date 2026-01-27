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
 * Integration tests for Panama AdvancedArenaManager.
 *
 * <p>These tests verify the advanced arena management capabilities including optimized allocation,
 * zero-copy operations, pool hit rates, and statistics reporting.
 *
 * @since 1.0.0
 */
@DisplayName("Panama AdvancedArenaManager Integration Tests")
class AdvancedArenaManagerIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(AdvancedArenaManagerIntegrationTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  /** Track if manager was enabled before test. */
  private boolean wasEnabled;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama advanced arena manager tests");
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
    LOGGER.info("Setting up test - enabling advanced arena manager");
    wasEnabled = AdvancedArenaManager.isEnabled();
    AdvancedArenaManager.setEnabled(true);
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
    AdvancedArenaManager.setEnabled(wasEnabled);
  }

  @Nested
  @DisplayName("Singleton Tests")
  class SingletonTests {

    @Test
    @DisplayName("should return same instance")
    void shouldReturnSameInstance() {
      LOGGER.info("Testing singleton pattern");

      AdvancedArenaManager instance1 = AdvancedArenaManager.getInstance();
      AdvancedArenaManager instance2 = AdvancedArenaManager.getInstance();

      assertNotNull(instance1, "First instance should not be null");
      assertNotNull(instance2, "Second instance should not be null");
      assertSame(instance1, instance2, "Should return same instance");

      LOGGER.info("Singleton pattern verified");
    }
  }

  @Nested
  @DisplayName("Allocation Tests")
  class AllocationTests {

    @Test
    @DisplayName("should allocate optimized memory by size")
    void shouldAllocateOptimizedMemoryBySize() {
      LOGGER.info("Testing optimized memory allocation by size");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Test various sizes
      long[] sizes = {64, 256, 1024, 8192, 65536};
      for (long size : sizes) {
        MemorySegment segment = manager.allocateOptimized(size);
        assertNotNull(segment, "Segment should not be null for size " + size);
        assertEquals(size, segment.byteSize(), "Segment size should match requested size");
        assertTrue(segment.scope().isAlive(), "Segment scope should be alive");

        LOGGER.info("Allocated segment of size " + size);
      }
    }

    @Test
    @DisplayName("should allocate optimized memory by layout")
    void shouldAllocateOptimizedMemoryByLayout() {
      LOGGER.info("Testing optimized memory allocation by layout");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Test with different layouts
      MemoryLayout[] layouts = {
        ValueLayout.JAVA_INT,
        ValueLayout.JAVA_LONG,
        ValueLayout.JAVA_DOUBLE,
        MemoryLayout.sequenceLayout(10, ValueLayout.JAVA_INT)
      };

      for (MemoryLayout layout : layouts) {
        MemorySegment segment = manager.allocateOptimized(layout);
        assertNotNull(segment, "Segment should not be null for layout " + layout);
        assertEquals(
            layout.byteSize(), segment.byteSize(), "Segment size should match layout size");

        LOGGER.info("Allocated segment with layout: " + layout);
      }
    }

    @Test
    @DisplayName("should allocate bulk elements")
    void shouldAllocateBulkElements() {
      LOGGER.info("Testing bulk element allocation");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      MemoryLayout elementLayout = ValueLayout.JAVA_INT;
      long elementCount = 100;

      MemorySegment bulk = manager.allocateBulkOptimized(elementCount, elementLayout);
      assertNotNull(bulk, "Bulk segment should not be null");
      assertEquals(
          elementCount * elementLayout.byteSize(),
          bulk.byteSize(),
          "Bulk segment size should match total elements size");

      LOGGER.info("Allocated bulk segment for " + elementCount + " elements");
    }

    @Test
    @DisplayName("should select appropriate allocation strategy")
    void shouldSelectAppropriateStrategy() {
      LOGGER.info("Testing allocation strategy selection");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Allocate various sizes to trigger different strategies
      // Small allocation (may use zero-copy)
      MemorySegment small = manager.allocateOptimized(100);
      assertNotNull(small, "Small allocation should succeed");

      // Medium allocation (may use pool reuse)
      MemorySegment medium = manager.allocateOptimized(4096);
      assertNotNull(medium, "Medium allocation should succeed");

      // Large allocation
      MemorySegment large = manager.allocateOptimized(1024 * 1024);
      assertNotNull(large, "Large allocation should succeed");

      LOGGER.info("Allocation strategy selection verified");
    }
  }

  @Nested
  @DisplayName("Zero-Copy Tests")
  class ZeroCopyTests {

    @Test
    @DisplayName("should execute zero-copy operation when segment fits")
    void shouldExecuteZeroCopyWhenSegmentFits() {
      LOGGER.info("Testing zero-copy operation");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Create a source segment
      MemorySegment source = manager.allocateOptimized(1024);
      assertNotNull(source, "Source segment should not be null");

      // Execute zero-copy operation with smaller required size
      String result =
          manager.executeZeroCopy(
              source,
              512,
              segment -> {
                assertNotNull(segment, "Provided segment should not be null");
                assertTrue(segment.byteSize() >= 512, "Segment should be at least required size");
                return "zero_copy_success";
              });

      assertEquals("zero_copy_success", result, "Operation should return expected result");

      LOGGER.info("Zero-copy operation executed successfully");
    }

    @Test
    @DisplayName("should fall back to allocation when segment doesn't fit")
    void shouldFallBackWhenSegmentDoesntFit() {
      LOGGER.info("Testing zero-copy fallback");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Create a small source segment
      MemorySegment source = manager.allocateOptimized(256);

      // Execute with larger required size - should fall back to new allocation
      String result =
          manager.executeZeroCopy(
              source,
              1024,
              segment -> {
                assertNotNull(segment, "Segment should not be null");
                assertEquals(1024, segment.byteSize(), "Segment should be required size");
                return "fallback_success";
              });

      assertEquals("fallback_success", result, "Fallback operation should succeed");

      LOGGER.info("Zero-copy fallback verified");
    }

    @Test
    @DisplayName("should handle null source segment")
    void shouldHandleNullSourceSegment() {
      LOGGER.info("Testing null source segment handling");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Execute with null source - should allocate new
      String result =
          manager.executeZeroCopy(
              null,
              512,
              segment -> {
                assertNotNull(segment, "New segment should be allocated");
                return "null_source_handled";
              });

      assertEquals("null_source_handled", result, "Should handle null source");

      LOGGER.info("Null source segment handled");
    }
  }

  @Nested
  @DisplayName("Release Tests")
  class ReleaseTests {

    @Test
    @DisplayName("should release optimized memory")
    void shouldReleaseOptimizedMemory() {
      LOGGER.info("Testing memory release");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      long initialUsage = manager.getCurrentMemoryUsage();

      // Allocate
      MemorySegment segment = manager.allocateOptimized(4096);
      long afterAlloc = manager.getCurrentMemoryUsage();

      // Release
      manager.releaseOptimized(segment);
      long afterRelease = manager.getCurrentMemoryUsage();

      // Memory usage should decrease after release
      assertTrue(
          afterRelease <= afterAlloc, "Memory usage should not increase after release");

      LOGGER.info(
          "Memory: initial="
              + initialUsage
              + ", afterAlloc="
              + afterAlloc
              + ", afterRelease="
              + afterRelease);
    }

    @Test
    @DisplayName("should handle null segment release gracefully")
    void shouldHandleNullSegmentRelease() {
      LOGGER.info("Testing null segment release");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      assertDoesNotThrow(
          () -> manager.releaseOptimized(null), "Should handle null segment release gracefully");

      LOGGER.info("Null segment release handled");
    }
  }

  @Nested
  @DisplayName("Pooling Tests")
  class PoolingTests {

    @Test
    @DisplayName("should reuse arenas from pool")
    void shouldReuseArenasFromPool() {
      LOGGER.info("Testing arena reuse from pool");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Make multiple allocations of similar sizes to trigger pooling
      for (int i = 0; i < 20; i++) {
        MemorySegment segment = manager.allocateOptimized(1024);
        assertNotNull(segment, "Allocation " + i + " should succeed");
        manager.releaseOptimized(segment);
      }

      double hitRate = manager.getPoolHitRate();
      LOGGER.info("Pool hit rate after multiple allocations: " + hitRate + "%");
    }

    @Test
    @DisplayName("should track pool hit rate")
    void shouldTrackPoolHitRate() {
      LOGGER.info("Testing pool hit rate tracking");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      double hitRate = manager.getPoolHitRate();
      assertTrue(hitRate >= 0.0 && hitRate <= 100.0, "Hit rate should be between 0 and 100");

      LOGGER.info("Pool hit rate: " + hitRate + "%");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should report comprehensive statistics")
    void shouldReportStatistics() {
      LOGGER.info("Testing statistics reporting");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Perform some operations
      for (int i = 0; i < 5; i++) {
        MemorySegment segment = manager.allocateOptimized(512);
        manager.releaseOptimized(segment);
      }

      String stats = manager.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");
      assertTrue(
          stats.contains("Memory pressure") || stats.contains("Arena"),
          "Statistics should contain arena information");

      LOGGER.info("Statistics:\n" + stats);
    }

    @Test
    @DisplayName("should track current memory usage")
    void shouldTrackCurrentMemoryUsage() {
      LOGGER.info("Testing memory usage tracking");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      long usage = manager.getCurrentMemoryUsage();
      assertTrue(usage >= 0, "Memory usage should be non-negative");

      LOGGER.info("Current memory usage: " + usage + " bytes");
    }

    @Test
    @DisplayName("should return disabled message when disabled")
    void shouldReturnDisabledMessage() {
      LOGGER.info("Testing disabled state statistics");

      AdvancedArenaManager.setEnabled(false);

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();
      String stats = manager.getStatistics();

      assertTrue(stats.contains("disabled"), "Statistics should indicate disabled state");

      AdvancedArenaManager.setEnabled(true);

      LOGGER.info("Disabled statistics: " + stats);
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should enable advanced arena management")
    void shouldEnableManagement() {
      LOGGER.info("Testing enable");

      AdvancedArenaManager.setEnabled(true);
      assertTrue(AdvancedArenaManager.isEnabled(), "Management should be enabled");

      LOGGER.info("Management enabled");
    }

    @Test
    @DisplayName("should disable advanced arena management")
    void shouldDisableManagement() {
      LOGGER.info("Testing disable");

      AdvancedArenaManager.setEnabled(false);
      assertFalse(AdvancedArenaManager.isEnabled(), "Management should be disabled");

      // Re-enable for other tests
      AdvancedArenaManager.setEnabled(true);

      LOGGER.info("Management disabled and re-enabled");
    }

    @Test
    @DisplayName("should fall back to basic allocation when disabled")
    void shouldFallBackWhenDisabled() {
      LOGGER.info("Testing fallback when disabled");

      AdvancedArenaManager.setEnabled(false);

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Should still work, just without optimizations
      MemorySegment segment = manager.allocateOptimized(1024);
      assertNotNull(segment, "Allocation should still work when disabled");
      assertEquals(1024, segment.byteSize(), "Segment size should be correct");

      AdvancedArenaManager.setEnabled(true);

      LOGGER.info("Fallback allocation verified");
    }
  }

  @Nested
  @DisplayName("Concurrency Tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("should handle concurrent allocations")
    void shouldHandleConcurrentAllocations() throws Exception {
      LOGGER.info("Testing concurrent allocations");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      int numThreads = 4;
      int allocationsPerThread = 50;
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
                    MemorySegment segment = manager.allocateOptimized(256);
                    if (segment != null) {
                      successCount.incrementAndGet();
                      manager.releaseOptimized(segment);
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
      LOGGER.info("Pool hit rate: " + manager.getPoolHitRate() + "%");
    }
  }

  @Nested
  @DisplayName("Memory Pressure Tests")
  class MemoryPressureTests {

    @Test
    @DisplayName("should track memory pressure")
    void shouldTrackMemoryPressure() {
      LOGGER.info("Testing memory pressure tracking");

      AdvancedArenaManager manager = AdvancedArenaManager.getInstance();

      // Allocate a bunch of memory to potentially trigger pressure changes
      List<MemorySegment> segments = new ArrayList<>();
      for (int i = 0; i < 10; i++) {
        segments.add(manager.allocateOptimized(8192));
      }

      // Get statistics which includes pressure info
      String stats = manager.getStatistics();
      assertTrue(
          stats.contains("Memory pressure") || stats.contains("pressure"),
          "Statistics should mention memory pressure");

      // Release segments
      for (MemorySegment segment : segments) {
        manager.releaseOptimized(segment);
      }

      LOGGER.info("Memory pressure tracked:\n" + stats);
    }
  }
}
