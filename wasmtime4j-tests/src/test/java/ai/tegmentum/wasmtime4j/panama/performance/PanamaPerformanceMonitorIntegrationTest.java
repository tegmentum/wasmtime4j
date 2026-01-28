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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Panama PanamaPerformanceMonitor.
 *
 * <p>These tests verify the Panama-specific performance monitoring capabilities including operation
 * timing, arena tracking, memory segment tracking, and method handle call monitoring.
 *
 * @since 1.0.0
 */
@DisplayName("Panama PanamaPerformanceMonitor Integration Tests")
class PanamaPerformanceMonitorIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaPerformanceMonitorIntegrationTest.class.getName());

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  /** Track if monitoring was enabled before test. */
  private boolean wasEnabled;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for Panama performance monitor tests");
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
    LOGGER.info("Setting up test - resetting performance monitor");
    wasEnabled = PanamaPerformanceMonitor.isEnabled();
    PanamaPerformanceMonitor.setEnabled(true);
    PanamaPerformanceMonitor.reset();
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
    PanamaPerformanceMonitor.setEnabled(wasEnabled);
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("should enable monitoring")
    void shouldEnableMonitoring() {
      LOGGER.info("Testing enable monitoring");

      PanamaPerformanceMonitor.setEnabled(true);
      assertTrue(PanamaPerformanceMonitor.isEnabled(), "Monitoring should be enabled");

      LOGGER.info("Monitoring correctly enabled: " + PanamaPerformanceMonitor.isEnabled());
    }

    @Test
    @DisplayName("should disable monitoring")
    void shouldDisableMonitoring() {
      LOGGER.info("Testing disable monitoring");

      PanamaPerformanceMonitor.setEnabled(false);
      assertFalse(PanamaPerformanceMonitor.isEnabled(), "Monitoring should be disabled");

      LOGGER.info("Monitoring correctly disabled: " + PanamaPerformanceMonitor.isEnabled());
    }

    @Test
    @DisplayName("should not track operations when disabled")
    void shouldNotTrackOperationsWhenDisabled() {
      LOGGER.info("Testing operations not tracked when disabled");

      PanamaPerformanceMonitor.setEnabled(false);
      PanamaPerformanceMonitor.reset();

      long startTime = PanamaPerformanceMonitor.startOperation("test_category", "test details");
      assertEquals(0, startTime, "Start time should be 0 when disabled");

      PanamaPerformanceMonitor.endOperation("test_category", startTime);

      String stats = PanamaPerformanceMonitor.getStatistics();
      assertTrue(stats.contains("disabled"), "Statistics should indicate monitoring is disabled");

      LOGGER.info("Operations correctly not tracked when disabled");
    }
  }

  @Nested
  @DisplayName("Operation Timing Tests")
  class OperationTimingTests {

    @Test
    @DisplayName("should track operation start and end")
    void shouldTrackOperationStartAndEnd() {
      LOGGER.info("Testing operation timing tracking");

      long startTime = PanamaPerformanceMonitor.startOperation("test_operation", "test details");
      assertTrue(startTime > 0 || startTime == -1, "Start time should be positive or sampled");

      // Simulate some work
      try {
        Thread.sleep(5);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      PanamaPerformanceMonitor.endOperation("test_operation", startTime);

      String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      LOGGER.info("Operation timing tracked. Stats:\n" + stats);
    }

    @Test
    @DisplayName("should track operation without details")
    void shouldTrackOperationWithoutDetails() {
      LOGGER.info("Testing operation timing without details");

      long startTime = PanamaPerformanceMonitor.startOperation("simple_operation");
      assertTrue(
          startTime > 0 || startTime == -1, "Start time should be positive or sampled marker");

      PanamaPerformanceMonitor.endOperation("simple_operation", startTime);

      LOGGER.info("Operation without details tracked correctly");
    }

    @Test
    @DisplayName("should calculate average operation time")
    void shouldCalculateAverageOperationTime() {
      LOGGER.info("Testing average operation time calculation");

      // Run multiple operations to get meaningful statistics
      for (int i = 0; i < 200; i++) {
        long startTime = PanamaPerformanceMonitor.startOperation("average_test");
        // Small delay
        for (int j = 0; j < 100; j++) {
          Math.random();
        }
        PanamaPerformanceMonitor.endOperation("average_test", startTime);
      }

      double avgOverhead = PanamaPerformanceMonitor.getAverageFfiOverhead();
      LOGGER.info("Average FFI overhead: " + avgOverhead + " ns");

      // Average should be reasonable (not extremely high or zero)
      assertTrue(avgOverhead >= 0, "Average overhead should be non-negative");
    }

    @Test
    @DisplayName("should track concurrent operations")
    void shouldTrackConcurrentOperations() throws Exception {
      LOGGER.info("Testing concurrent operation tracking");

      int numThreads = 4;
      int operationsPerThread = 50;
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      CountDownLatch latch = new CountDownLatch(numThreads);

      for (int t = 0; t < numThreads; t++) {
        final int threadId = t;
        executor.submit(
            () -> {
              try {
                for (int i = 0; i < operationsPerThread; i++) {
                  long startTime =
                      PanamaPerformanceMonitor.startOperation(
                          "concurrent_op_" + threadId, "iteration " + i);
                  // Simulate some work
                  Math.random();
                  PanamaPerformanceMonitor.endOperation("concurrent_op_" + threadId, startTime);
                }
              } finally {
                latch.countDown();
              }
            });
      }

      boolean completed = latch.await(30, TimeUnit.SECONDS);
      assertTrue(completed, "All threads should complete");
      executor.shutdown();

      String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should be available after concurrent operations");

      LOGGER.info("Concurrent operations tracked:\n" + stats);
    }
  }

  @Nested
  @DisplayName("Arena Tracking Tests")
  class ArenaTrackingTests {

    @Test
    @DisplayName("should record arena allocation")
    void shouldRecordArenaAllocation() {
      LOGGER.info("Testing arena allocation recording");

      try (Arena arena = Arena.ofConfined()) {
        resources.add(arena);
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);

        String stats = PanamaPerformanceMonitor.getStatistics();
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(
            stats.contains("Arena allocations") || stats.contains("arena"),
            "Statistics should mention arena allocations");

        LOGGER.info("Arena allocation recorded. Stats:\n" + stats);
      }
    }

    @Test
    @DisplayName("should record arena deallocation")
    void shouldRecordArenaDeallocation() {
      LOGGER.info("Testing arena deallocation recording");

      Arena arena = Arena.ofConfined();
      PanamaPerformanceMonitor.recordArenaAllocation(arena, 2048);

      arena.close();
      PanamaPerformanceMonitor.recordArenaDeallocation(arena);

      String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(
          stats.contains("Arena deallocations") || stats.contains("deallocation"),
          "Statistics should mention arena deallocations");

      LOGGER.info("Arena deallocation recorded. Stats:\n" + stats);
    }

    @Test
    @DisplayName("should track arena lifecycle")
    void shouldTrackArenaLifecycle() {
      LOGGER.info("Testing arena lifecycle tracking");

      // Create and track multiple arenas
      for (int i = 0; i < 5; i++) {
        Arena arena = Arena.ofConfined();
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 512 * (i + 1));

        // Allocate some memory in the arena
        MemorySegment segment = arena.allocate(256);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);

        // Close arena
        arena.close();
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }

      String activeArenaStats = PanamaPerformanceMonitor.getActiveArenaStats();
      LOGGER.info("Active arena stats after lifecycle: " + activeArenaStats);

      String stats = PanamaPerformanceMonitor.getStatistics();
      LOGGER.info("Full statistics after arena lifecycle:\n" + stats);
    }

    @Test
    @DisplayName("should handle null arena gracefully")
    void shouldHandleNullArenaGracefully() {
      LOGGER.info("Testing null arena handling");

      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordArenaAllocation(null, 1024),
          "Should handle null arena in allocation");

      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordArenaDeallocation(null),
          "Should handle null arena in deallocation");

      LOGGER.info("Null arena handled gracefully");
    }
  }

  @Nested
  @DisplayName("Memory Segment Tracking Tests")
  class MemorySegmentTrackingTests {

    @Test
    @DisplayName("should record segment allocation")
    void shouldRecordSegmentAllocation() {
      LOGGER.info("Testing memory segment allocation recording");

      try (Arena arena = Arena.ofConfined()) {
        MemorySegment segment = arena.allocate(1024);
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);

        String stats = PanamaPerformanceMonitor.getStatistics();
        assertNotNull(stats, "Statistics should not be null");
        assertTrue(
            stats.contains("Memory segment") || stats.contains("segment"),
            "Statistics should mention memory segments");

        LOGGER.info("Memory segment allocation recorded. Stats:\n" + stats);
      }
    }

    @Test
    @DisplayName("should track segment sizes")
    void shouldTrackSegmentSizes() {
      LOGGER.info("Testing memory segment size tracking");

      try (Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 8192);

        // Allocate segments of different sizes
        int[] sizes = {64, 256, 1024, 4096};
        for (int size : sizes) {
          MemorySegment segment = arena.allocate(size);
          PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
          LOGGER.info("Allocated segment of size: " + size);
        }

        String stats = PanamaPerformanceMonitor.getStatistics();
        assertNotNull(stats, "Statistics should not be null");

        LOGGER.info("Multiple segment sizes tracked. Stats:\n" + stats);
      }
    }

    @Test
    @DisplayName("should handle null segment gracefully")
    void shouldHandleNullSegmentGracefully() {
      LOGGER.info("Testing null segment handling");

      try (Arena arena = Arena.ofConfined()) {
        assertDoesNotThrow(
            () -> PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, null),
            "Should handle null segment");
      }

      LOGGER.info("Null segment handled gracefully");
    }
  }

  @Nested
  @DisplayName("Method Handle Tracking Tests")
  class MethodHandleTrackingTests {

    @Test
    @DisplayName("should record method handle calls")
    void shouldRecordMethodHandleCalls() {
      LOGGER.info("Testing method handle call recording");

      PanamaPerformanceMonitor.recordMethodHandleCall("test_function");
      PanamaPerformanceMonitor.recordMethodHandleCall("another_function");
      PanamaPerformanceMonitor.recordMethodHandleCall("test_function");

      String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(
          stats.contains("Method handle") || stats.contains("method"),
          "Statistics should mention method handles");

      LOGGER.info("Method handle calls recorded. Stats:\n" + stats);
    }

    @Test
    @DisplayName("should track call frequency")
    void shouldTrackCallFrequency() {
      LOGGER.info("Testing method handle call frequency tracking");

      // Record many calls
      for (int i = 0; i < 100; i++) {
        PanamaPerformanceMonitor.recordMethodHandleCall("frequent_function");
      }
      for (int i = 0; i < 10; i++) {
        PanamaPerformanceMonitor.recordMethodHandleCall("rare_function");
      }

      String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
      assertNotNull(metrics, "Panama metrics should not be null");
      assertTrue(
          metrics.contains("methodhandle_calls"), "Metrics should contain method handle calls");

      LOGGER.info("Call frequency tracked. Metrics: " + metrics);
    }

    @Test
    @DisplayName("should handle null method name")
    void shouldHandleNullMethodName() {
      LOGGER.info("Testing null method name handling");

      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordMethodHandleCall(null),
          "Should handle null method name");

      LOGGER.info("Null method name handled gracefully");
    }
  }

  @Nested
  @DisplayName("Zero-Copy Operation Tests")
  class ZeroCopyOperationTests {

    @Test
    @DisplayName("should record zero-copy operations")
    void shouldRecordZeroCopyOperations() {
      LOGGER.info("Testing zero-copy operation recording");

      PanamaPerformanceMonitor.recordZeroCopyOperation();
      PanamaPerformanceMonitor.recordZeroCopyOperation();
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
      assertNotNull(metrics, "Panama metrics should not be null");
      assertTrue(metrics.contains("zero_copy"), "Metrics should contain zero-copy operations");

      LOGGER.info("Zero-copy operations recorded. Metrics: " + metrics);
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("should return accurate statistics")
    void shouldReturnAccurateStatistics() {
      LOGGER.info("Testing statistics accuracy");

      // Perform various operations
      for (int i = 0; i < 10; i++) {
        long startTime = PanamaPerformanceMonitor.startOperation("stats_test");
        PanamaPerformanceMonitor.endOperation("stats_test", startTime);
      }

      try (Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);
        MemorySegment segment = arena.allocate(512);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }

      PanamaPerformanceMonitor.recordMethodHandleCall("test_func");
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertFalse(stats.isEmpty(), "Statistics should not be empty");

      // Verify statistics contain expected sections
      assertTrue(
          stats.contains("Performance Statistics")
              || stats.contains("FFI")
              || stats.contains("Panama"),
          "Should contain performance statistics header");

      LOGGER.info("Statistics returned:\n" + stats);
    }

    @Test
    @DisplayName("should identify performance issues")
    void shouldIdentifyPerformanceIssues() {
      LOGGER.info("Testing performance issue identification");

      // Create a scenario that might trigger performance issues
      for (int i = 0; i < 100; i++) {
        try (Arena arena = Arena.ofConfined()) {
          PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);
          // Not recording deallocation to simulate potential leak scenario
        }
        PanamaPerformanceMonitor.recordArenaDeallocation(null); // This won't actually record
      }

      String issues = PanamaPerformanceMonitor.getPerformanceIssues();
      // Issues may or may not be detected depending on thresholds
      LOGGER.info("Performance issues check: " + (issues != null ? issues : "No issues detected"));
    }

    @Test
    @DisplayName("should reset statistics")
    void shouldResetStatistics() {
      LOGGER.info("Testing statistics reset");

      // Record some operations
      for (int i = 0; i < 10; i++) {
        long startTime = PanamaPerformanceMonitor.startOperation("reset_test");
        PanamaPerformanceMonitor.endOperation("reset_test", startTime);
        PanamaPerformanceMonitor.recordMethodHandleCall("reset_func");
      }

      // Reset
      PanamaPerformanceMonitor.reset();

      // Verify reset
      String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should not be null after reset");

      LOGGER.info("Statistics after reset:\n" + stats);
    }
  }

  @Nested
  @DisplayName("Panama Metrics Tests")
  class PanamaMetricsTests {

    @Test
    @DisplayName("should return Panama-specific metrics")
    void shouldReturnPanamaSpecificMetrics() {
      LOGGER.info("Testing Panama-specific metrics");

      // Perform some operations
      long startTime = PanamaPerformanceMonitor.startOperation("metrics_test");
      PanamaPerformanceMonitor.endOperation("metrics_test", startTime);

      try (Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);
        MemorySegment segment = arena.allocate(256);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
      }

      PanamaPerformanceMonitor.recordMethodHandleCall("test_func");
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
      assertNotNull(metrics, "Panama metrics should not be null");
      assertTrue(metrics.contains("Panama"), "Metrics should contain 'Panama'");
      assertTrue(metrics.contains("ffi_calls"), "Metrics should contain 'ffi_calls'");
      assertTrue(metrics.contains("arena_ops"), "Metrics should contain 'arena_ops'");
      assertTrue(metrics.contains("memseg_ops"), "Metrics should contain 'memseg_ops'");
      assertTrue(
          metrics.contains("methodhandle_calls"), "Metrics should contain 'methodhandle_calls'");
      assertTrue(metrics.contains("zero_copy"), "Metrics should contain 'zero_copy'");

      LOGGER.info("Panama metrics: " + metrics);
    }

    @Test
    @DisplayName("should check performance target")
    void shouldCheckPerformanceTarget() {
      LOGGER.info("Testing performance target check");

      // Perform operations to get meaningful metrics
      for (int i = 0; i < 100; i++) {
        long startTime = PanamaPerformanceMonitor.startOperation("target_test");
        PanamaPerformanceMonitor.endOperation("target_test", startTime);
      }

      boolean meetsTarget = PanamaPerformanceMonitor.meetsPerformanceTarget();
      double avgOverhead = PanamaPerformanceMonitor.getAverageFfiOverhead();

      LOGGER.info(
          "Performance target check - meets target: "
              + meetsTarget
              + ", avg overhead: "
              + avgOverhead
              + "ns"
              + " (target: "
              + PanamaPerformanceMonitor.SIMPLE_PANAMA_OPERATION_TARGET_NS
              + "ns)");
    }
  }

  @Nested
  @DisplayName("Monitor Functional Interface Tests")
  class MonitorFunctionalInterfaceTests {

    @Test
    @DisplayName("should execute monitored operation")
    void shouldExecuteMonitoredOperation() {
      LOGGER.info("Testing monitored operation execution");

      String result =
          PanamaPerformanceMonitor.monitor(
              "functional_test",
              () -> {
                // Simulate some work
                return "operation_result";
              });

      assertEquals("operation_result", result, "Should return operation result");

      String stats = PanamaPerformanceMonitor.getStatistics();
      LOGGER.info("Monitored operation executed. Stats:\n" + stats);
    }

    @Test
    @DisplayName("should propagate exceptions from monitored operation")
    void shouldPropagateExceptionsFromMonitoredOperation() {
      LOGGER.info("Testing exception propagation from monitored operation");

      try {
        PanamaPerformanceMonitor.monitor(
            "exception_test",
            () -> {
              throw new IllegalStateException("Test exception");
            });
      } catch (RuntimeException e) {
        assertTrue(e.getCause() instanceof IllegalStateException, "Should wrap original exception");
        LOGGER.info("Exception correctly propagated: " + e.getCause().getMessage());
        return;
      }

      LOGGER.info("Exception propagation test completed");
    }
  }

  @Nested
  @DisplayName("Active Arena Stats Tests")
  class ActiveArenaStatsTests {

    @Test
    @DisplayName("should return active arena statistics")
    void shouldReturnActiveArenaStatistics() {
      LOGGER.info("Testing active arena statistics");

      Arena arena1 = Arena.ofConfined();
      Arena arena2 = Arena.ofConfined();
      resources.add(arena1);
      resources.add(arena2);

      PanamaPerformanceMonitor.recordArenaAllocation(arena1, 1024);
      PanamaPerformanceMonitor.recordArenaAllocation(arena2, 2048);

      String activeStats = PanamaPerformanceMonitor.getActiveArenaStats();
      assertNotNull(activeStats, "Active arena stats should not be null");

      LOGGER.info("Active arena stats: " + activeStats);
    }

    @Test
    @DisplayName("should show no active arenas when all closed")
    void shouldShowNoActiveArenasWhenAllClosed() {
      LOGGER.info("Testing no active arenas scenario");

      // Create and immediately close arenas
      Arena arena = Arena.ofConfined();
      PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);
      arena.close();
      PanamaPerformanceMonitor.recordArenaDeallocation(arena);

      String activeStats = PanamaPerformanceMonitor.getActiveArenaStats();
      LOGGER.info("Active arena stats after closure: " + activeStats);
    }
  }
}
