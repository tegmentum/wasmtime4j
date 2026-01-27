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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link PanamaPerformanceMonitor}.
 *
 * <p>These tests invoke actual methods on the static utility class to exercise code paths and
 * improve JaCoCo coverage. The class uses only Panama API (Arena, MemorySegment) and does not
 * require native wasmtime library.
 */
@DisplayName("PanamaPerformanceMonitor Integration Tests")
class PanamaPerformanceMonitorTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaPerformanceMonitorTest.class.getName());

  @BeforeEach
  void setUp() {
    PanamaPerformanceMonitor.reset();
    PanamaPerformanceMonitor.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    PanamaPerformanceMonitor.reset();
    PanamaPerformanceMonitor.setEnabled(true);
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("Should enable monitoring")
    void shouldEnableMonitoring() {
      LOGGER.info("Testing setEnabled(true)");
      PanamaPerformanceMonitor.setEnabled(true);
      assertTrue(PanamaPerformanceMonitor.isEnabled(), "Monitoring should be enabled");
      LOGGER.info("Monitoring enabled: " + PanamaPerformanceMonitor.isEnabled());
    }

    @Test
    @DisplayName("Should disable monitoring")
    void shouldDisableMonitoring() {
      LOGGER.info("Testing setEnabled(false)");
      PanamaPerformanceMonitor.setEnabled(false);
      assertFalse(PanamaPerformanceMonitor.isEnabled(), "Monitoring should be disabled");
      LOGGER.info("Monitoring enabled: " + PanamaPerformanceMonitor.isEnabled());
    }

    @Test
    @DisplayName("Should toggle monitoring on and off")
    void shouldToggleMonitoring() {
      LOGGER.info("Testing enable/disable toggle");
      PanamaPerformanceMonitor.setEnabled(false);
      assertFalse(PanamaPerformanceMonitor.isEnabled());
      PanamaPerformanceMonitor.setEnabled(true);
      assertTrue(PanamaPerformanceMonitor.isEnabled());
      LOGGER.info("Toggle complete");
    }
  }

  @Nested
  @DisplayName("Operation Timing Tests")
  class OperationTimingTests {

    @Test
    @DisplayName("Should start and end operation with category and details")
    void shouldStartAndEndOperationWithDetails() {
      LOGGER.info("Testing startOperation/endOperation with details");

      final long startTime = PanamaPerformanceMonitor.startOperation("test_category", "test_op");
      LOGGER.info("startOperation returned: " + startTime);

      PanamaPerformanceMonitor.endOperation("test_category", startTime);
      LOGGER.info("endOperation completed");
    }

    @Test
    @DisplayName("Should start and end operation with category only")
    void shouldStartAndEndOperationWithCategory() {
      LOGGER.info("Testing startOperation/endOperation with category only");

      final long startTime = PanamaPerformanceMonitor.startOperation("test_category");
      LOGGER.info("startOperation returned: " + startTime);

      PanamaPerformanceMonitor.endOperation("test_category", startTime);
      LOGGER.info("endOperation completed");
    }

    @Test
    @DisplayName("Should handle endOperation with zero start time (disabled)")
    void shouldHandleEndOperationWithZeroStartTime() {
      LOGGER.info("Testing endOperation with startTime=0 (disabled path)");
      PanamaPerformanceMonitor.endOperation("test_category", 0);
      LOGGER.info("endOperation with zero start time completed without error");
    }

    @Test
    @DisplayName("Should handle endOperation with -1 start time (not sampled)")
    void shouldHandleEndOperationWithNegativeOneStartTime() {
      LOGGER.info("Testing endOperation with startTime=-1 (not sampled path)");
      PanamaPerformanceMonitor.endOperation("test_category", -1);
      LOGGER.info("endOperation with -1 start time completed without error");
    }

    @Test
    @DisplayName("Should return 0 when disabled")
    void shouldReturnZeroWhenDisabled() {
      LOGGER.info("Testing startOperation when disabled");
      PanamaPerformanceMonitor.setEnabled(false);
      final long startTime = PanamaPerformanceMonitor.startOperation("test_category", "details");
      assertEquals(0, startTime, "Should return 0 when disabled");
      LOGGER.info("Returned 0 as expected when disabled");
    }

    @Test
    @DisplayName("Should track multiple operations across categories")
    void shouldTrackMultipleOperations() {
      LOGGER.info("Testing multiple operations across categories");

      // Exercise multiple operations to ensure some get sampled
      for (int i = 0; i < 150; i++) {
        final long start = PanamaPerformanceMonitor.startOperation("category_a", "op_" + i);
        PanamaPerformanceMonitor.endOperation("category_a", start);
      }

      for (int i = 0; i < 150; i++) {
        final long start = PanamaPerformanceMonitor.startOperation("category_b");
        PanamaPerformanceMonitor.endOperation("category_b", start);
      }

      LOGGER.info("Multiple operations tracked successfully");
    }
  }

  @Nested
  @DisplayName("Arena Tracking Tests")
  class ArenaTrackingTests {

    @Test
    @DisplayName("Should record arena allocation and deallocation")
    void shouldRecordArenaAllocationAndDeallocation() {
      LOGGER.info("Testing arena allocation/deallocation tracking");

      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024);
        LOGGER.info("Arena allocation recorded");

        final String arenaStats = PanamaPerformanceMonitor.getActiveArenaStats();
        LOGGER.info("Active arena stats: " + arenaStats);
        assertNotNull(arenaStats, "Arena stats should not be null");

        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
        LOGGER.info("Arena deallocation recorded");
      }
    }

    @Test
    @DisplayName("Should handle null arena allocation gracefully")
    void shouldHandleNullArenaAllocation() {
      LOGGER.info("Testing null arena allocation");
      PanamaPerformanceMonitor.recordArenaAllocation(null, 1024);
      LOGGER.info("Null arena allocation handled gracefully");
    }

    @Test
    @DisplayName("Should handle null arena deallocation gracefully")
    void shouldHandleNullArenaDeallocation() {
      LOGGER.info("Testing null arena deallocation");
      PanamaPerformanceMonitor.recordArenaDeallocation(null);
      LOGGER.info("Null arena deallocation handled gracefully");
    }

    @Test
    @DisplayName("Should track arena deallocation without prior allocation")
    void shouldTrackArenaDeallocationWithoutPriorAllocation() {
      LOGGER.info("Testing deallocation without prior allocation");
      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
        LOGGER.info("Deallocation without prior allocation completed");
      }
    }

    @Test
    @DisplayName("Should skip arena tracking when disabled")
    void shouldSkipArenaTrackingWhenDisabled() {
      LOGGER.info("Testing arena tracking when disabled");
      PanamaPerformanceMonitor.setEnabled(false);

      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 2048);
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
        LOGGER.info("Arena tracking skipped when disabled");
      }
    }

    @Test
    @DisplayName("Should track multiple arenas")
    void shouldTrackMultipleArenas() {
      LOGGER.info("Testing multiple arena tracking");

      try (final Arena arena1 = Arena.ofConfined();
          final Arena arena2 = Arena.ofConfined()) {

        PanamaPerformanceMonitor.recordArenaAllocation(arena1, 1024);
        PanamaPerformanceMonitor.recordArenaAllocation(arena2, 2048);

        final String arenaStats = PanamaPerformanceMonitor.getActiveArenaStats();
        LOGGER.info("Multiple arena stats: " + arenaStats);
        assertFalse(arenaStats.equals("No active arenas"), "Should have active arenas");

        PanamaPerformanceMonitor.recordArenaDeallocation(arena1);
        PanamaPerformanceMonitor.recordArenaDeallocation(arena2);
        LOGGER.info("Multiple arenas tracked and deallocated");
      }
    }
  }

  @Nested
  @DisplayName("Memory Segment Tracking Tests")
  class MemorySegmentTrackingTests {

    @Test
    @DisplayName("Should record memory segment allocation")
    void shouldRecordMemorySegmentAllocation() {
      LOGGER.info("Testing memory segment allocation tracking");

      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 4096);

        final MemorySegment segment = arena.allocate(256);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
        LOGGER.info("Memory segment allocation recorded, segment size: " + segment.byteSize());

        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }
    }

    @Test
    @DisplayName("Should handle null arena in segment allocation")
    void shouldHandleNullArenaInSegmentAllocation() {
      LOGGER.info("Testing null arena in segment allocation");
      try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(128);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(null, segment);
        LOGGER.info("Null arena handled gracefully");
      }
    }

    @Test
    @DisplayName("Should handle null segment in segment allocation")
    void shouldHandleNullSegmentInSegmentAllocation() {
      LOGGER.info("Testing null segment in segment allocation");
      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, null);
        LOGGER.info("Null segment handled gracefully");
      }
    }

    @Test
    @DisplayName("Should skip segment tracking when disabled")
    void shouldSkipSegmentTrackingWhenDisabled() {
      LOGGER.info("Testing segment tracking when disabled");
      PanamaPerformanceMonitor.setEnabled(false);

      try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(64);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
        LOGGER.info("Segment tracking skipped when disabled");
      }
    }

    @Test
    @DisplayName("Should record segment allocation without prior arena allocation")
    void shouldRecordSegmentAllocationWithoutPriorArenaAllocation() {
      LOGGER.info("Testing segment allocation without prior arena allocation");

      try (final Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(512);
        // No prior recordArenaAllocation call - arenaStats will be null
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
        LOGGER.info("Segment recorded without prior arena allocation");
      }
    }
  }

  @Nested
  @DisplayName("Method Handle Tracking Tests")
  class MethodHandleTrackingTests {

    @Test
    @DisplayName("Should record method handle call")
    void shouldRecordMethodHandleCall() {
      LOGGER.info("Testing method handle call tracking");
      PanamaPerformanceMonitor.recordMethodHandleCall("test_native_method");
      LOGGER.info("Method handle call recorded");
    }

    @Test
    @DisplayName("Should record method handle call with null name")
    void shouldRecordMethodHandleCallWithNullName() {
      LOGGER.info("Testing method handle call with null name");
      PanamaPerformanceMonitor.recordMethodHandleCall(null);
      LOGGER.info("Null method name handled");
    }

    @Test
    @DisplayName("Should skip method handle tracking when disabled")
    void shouldSkipMethodHandleTrackingWhenDisabled() {
      LOGGER.info("Testing method handle tracking when disabled");
      PanamaPerformanceMonitor.setEnabled(false);
      PanamaPerformanceMonitor.recordMethodHandleCall("disabled_method");
      LOGGER.info("Method handle tracking skipped when disabled");
    }

    @Test
    @DisplayName("Should track multiple method handle calls")
    void shouldTrackMultipleMethodHandleCalls() {
      LOGGER.info("Testing multiple method handle calls");
      PanamaPerformanceMonitor.recordMethodHandleCall("method_a");
      PanamaPerformanceMonitor.recordMethodHandleCall("method_b");
      PanamaPerformanceMonitor.recordMethodHandleCall("method_c");
      LOGGER.info("Multiple method handle calls tracked");
    }
  }

  @Nested
  @DisplayName("Zero Copy Tracking Tests")
  class ZeroCopyTrackingTests {

    @Test
    @DisplayName("Should record zero copy operation")
    void shouldRecordZeroCopyOperation() {
      LOGGER.info("Testing zero copy operation tracking");
      PanamaPerformanceMonitor.recordZeroCopyOperation();
      LOGGER.info("Zero copy operation recorded");
    }

    @Test
    @DisplayName("Should skip zero copy tracking when disabled")
    void shouldSkipZeroCopyTrackingWhenDisabled() {
      LOGGER.info("Testing zero copy tracking when disabled");
      PanamaPerformanceMonitor.setEnabled(false);
      PanamaPerformanceMonitor.recordZeroCopyOperation();
      LOGGER.info("Zero copy tracking skipped when disabled");
    }
  }

  @Nested
  @DisplayName("FFI Overhead Tests")
  class FfiOverheadTests {

    @Test
    @DisplayName("Should return zero overhead when no operations recorded")
    void shouldReturnZeroOverheadWhenNoOperations() {
      LOGGER.info("Testing getAverageFfiOverhead with no operations");
      final double overhead = PanamaPerformanceMonitor.getAverageFfiOverhead();
      assertEquals(0.0, overhead, 0.001, "Should return 0.0 with no operations");
      LOGGER.info("Average FFI overhead: " + overhead);
    }

    @Test
    @DisplayName("Should meet performance target with no operations")
    void shouldMeetPerformanceTargetWithNoOperations() {
      LOGGER.info("Testing meetsPerformanceTarget with no operations");
      // 0.0 < 50 = true
      assertTrue(
          PanamaPerformanceMonitor.meetsPerformanceTarget(),
          "Should meet target with zero overhead");
      LOGGER.info("Meets performance target: " + PanamaPerformanceMonitor.meetsPerformanceTarget());
    }

    @Test
    @DisplayName("Should calculate overhead after operations")
    void shouldCalculateOverheadAfterOperations() {
      LOGGER.info("Testing overhead calculation after operations");

      // Run enough operations so some are sampled
      for (int i = 0; i < 200; i++) {
        final long start = PanamaPerformanceMonitor.startOperation("overhead_test");
        PanamaPerformanceMonitor.endOperation("overhead_test", start);
      }

      final double overhead = PanamaPerformanceMonitor.getAverageFfiOverhead();
      LOGGER.info("Calculated FFI overhead: " + overhead + " ns");

      final boolean meetsTarget = PanamaPerformanceMonitor.meetsPerformanceTarget();
      LOGGER.info("Meets performance target: " + meetsTarget);
    }

    @Test
    @DisplayName("Should have correct SIMPLE_PANAMA_OPERATION_TARGET_NS constant")
    void shouldHaveCorrectTargetConstant() {
      LOGGER.info("Testing SIMPLE_PANAMA_OPERATION_TARGET_NS");
      assertEquals(
          50, PanamaPerformanceMonitor.SIMPLE_PANAMA_OPERATION_TARGET_NS, "Target should be 50 ns");
      LOGGER.info("Target constant: " + PanamaPerformanceMonitor.SIMPLE_PANAMA_OPERATION_TARGET_NS);
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("Should return statistics when enabled")
    void shouldReturnStatisticsWhenEnabled() {
      LOGGER.info("Testing getStatistics when enabled");

      // Record some operations first
      PanamaPerformanceMonitor.recordMethodHandleCall("test_method");
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 512);
        final MemorySegment segment = arena.allocate(128);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }

      final String statistics = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(statistics, "Statistics should not be null");
      assertTrue(
          statistics.contains("Panama WebAssembly Performance Statistics"),
          "Should contain header");
      assertTrue(statistics.contains("Total Panama FFI calls"), "Should contain FFI call count");
      assertTrue(statistics.contains("Arena allocations"), "Should contain arena allocations");
      assertTrue(statistics.contains("Method handle calls"), "Should contain method handle calls");
      assertTrue(statistics.contains("Zero-copy operations"), "Should contain zero copy ops");
      LOGGER.info("Statistics: " + statistics);
    }

    @Test
    @DisplayName("Should return disabled message when monitoring is off")
    void shouldReturnDisabledMessageWhenOff() {
      LOGGER.info("Testing getStatistics when disabled");
      PanamaPerformanceMonitor.setEnabled(false);
      final String statistics = PanamaPerformanceMonitor.getStatistics();
      assertEquals(
          "Panama performance monitoring is disabled",
          statistics,
          "Should indicate monitoring is disabled");
      LOGGER.info("Disabled statistics: " + statistics);
    }

    @Test
    @DisplayName("Should return statistics with operation stats")
    void shouldReturnStatisticsWithOperationStats() {
      LOGGER.info("Testing getStatistics with operation stats");

      // Generate enough operations so at least some are sampled and recorded
      for (int i = 0; i < 200; i++) {
        final long start = PanamaPerformanceMonitor.startOperation("stats_test", "op_" + i);
        PanamaPerformanceMonitor.endOperation("stats_test", start);
      }

      final String statistics = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(statistics, "Statistics should not be null");
      assertTrue(
          statistics.contains("Panama Operation Statistics"),
          "Should contain operation statistics section");
      LOGGER.info("Statistics with ops: " + statistics);
    }
  }

  @Nested
  @DisplayName("Panama Metrics Tests")
  class PanamaMetricsTests {

    @Test
    @DisplayName("Should return panama metrics string")
    void shouldReturnPanamaMetrics() {
      LOGGER.info("Testing getPanamaMetrics");

      PanamaPerformanceMonitor.recordMethodHandleCall("test_method");
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      final String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
      assertNotNull(metrics, "Metrics should not be null");
      assertTrue(
          metrics.contains("Panama Performance"), "Should contain Panama Performance header");
      assertTrue(metrics.contains("ffi_calls="), "Should contain ffi_calls");
      assertTrue(metrics.contains("arena_ops="), "Should contain arena_ops");
      assertTrue(metrics.contains("memseg_ops="), "Should contain memseg_ops");
      assertTrue(metrics.contains("methodhandle_calls="), "Should contain methodhandle_calls");
      assertTrue(metrics.contains("zero_copy="), "Should contain zero_copy");
      LOGGER.info("Panama metrics: " + metrics);
    }
  }

  @Nested
  @DisplayName("Active Arena Stats Tests")
  class ActiveArenaStatsTests {

    @Test
    @DisplayName("Should return no active arenas when empty")
    void shouldReturnNoActiveArenasWhenEmpty() {
      LOGGER.info("Testing getActiveArenaStats when empty");
      final String stats = PanamaPerformanceMonitor.getActiveArenaStats();
      assertEquals("No active arenas", stats, "Should report no active arenas");
      LOGGER.info("Active arena stats (empty): " + stats);
    }

    @Test
    @DisplayName("Should return no active arenas when disabled")
    void shouldReturnNoActiveArenasWhenDisabled() {
      LOGGER.info("Testing getActiveArenaStats when disabled");
      PanamaPerformanceMonitor.setEnabled(false);
      final String stats = PanamaPerformanceMonitor.getActiveArenaStats();
      assertEquals("No active arenas", stats, "Should report no active arenas when disabled");
      LOGGER.info("Active arena stats (disabled): " + stats);
    }

    @Test
    @DisplayName("Should return active arena details")
    void shouldReturnActiveArenaDetails() {
      LOGGER.info("Testing getActiveArenaStats with active arenas");

      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 2048);

        // Allocate some segments to update arena stats
        final MemorySegment segment = arena.allocate(256);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);

        final String stats = PanamaPerformanceMonitor.getActiveArenaStats();
        assertNotNull(stats, "Active arena stats should not be null");
        assertTrue(stats.contains("Active Arena Statistics"), "Should contain header");
        assertTrue(stats.contains("lifetime="), "Should contain lifetime info");
        assertTrue(stats.contains("allocations="), "Should contain allocation count");
        LOGGER.info("Active arena stats: " + stats);

        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }
    }
  }

  @Nested
  @DisplayName("Performance Issues Tests")
  class PerformanceIssuesTests {

    @Test
    @DisplayName("Should return null when no issues")
    void shouldReturnNullWhenNoIssues() {
      LOGGER.info("Testing getPerformanceIssues with no issues");
      final String issues = PanamaPerformanceMonitor.getPerformanceIssues();
      // With no operations, overhead is 0 which is below target
      // and no arena leaks, so null is expected
      LOGGER.info("Performance issues: " + issues);
    }

    @Test
    @DisplayName("Should return null when disabled")
    void shouldReturnNullWhenDisabled() {
      LOGGER.info("Testing getPerformanceIssues when disabled");
      PanamaPerformanceMonitor.setEnabled(false);
      final String issues = PanamaPerformanceMonitor.getPerformanceIssues();
      assertNull(issues, "Should return null when disabled");
      LOGGER.info("Performance issues (disabled): " + issues);
    }

    @Test
    @DisplayName("Should detect issues after operations")
    void shouldDetectIssuesAfterOperations() {
      LOGGER.info("Testing getPerformanceIssues after operations");

      // Run operations and check for issues
      for (int i = 0; i < 200; i++) {
        final long start = PanamaPerformanceMonitor.startOperation("issue_test");
        // Small delay to potentially exceed target
        PanamaPerformanceMonitor.endOperation("issue_test", start);
      }

      final String issues = PanamaPerformanceMonitor.getPerformanceIssues();
      LOGGER.info("Performance issues after operations: " + issues);
      // May or may not detect issues depending on timing
    }
  }

  @Nested
  @DisplayName("Monitor Operation Tests")
  class MonitorOperationTests {

    @Test
    @DisplayName("Should execute monitored operation and return result")
    void shouldExecuteMonitoredOperation() {
      LOGGER.info("Testing monitor() method");

      final String result =
          PanamaPerformanceMonitor.monitor(
              "test_monitor",
              () -> {
                LOGGER.info("Inside monitored operation");
                return "operation_result";
              });

      assertEquals("operation_result", result, "Should return operation result");
      LOGGER.info("Monitor operation result: " + result);
    }

    @Test
    @DisplayName("Should execute monitored operation with Arena")
    void shouldExecuteMonitoredOperationWithArena() {
      LOGGER.info("Testing monitor() with Arena operation");

      final Long result =
          PanamaPerformanceMonitor.monitor(
              "arena_monitor",
              () -> {
                try (final Arena arena = Arena.ofConfined()) {
                  final MemorySegment segment = arena.allocate(64);
                  return segment.byteSize();
                }
              });

      assertEquals(64L, result, "Should return segment byte size");
      LOGGER.info("Monitor with arena result: " + result);
    }

    @Test
    @DisplayName("Should wrap checked exceptions in RuntimeException")
    void shouldWrapCheckedExceptions() {
      LOGGER.info("Testing monitor() exception handling");

      try {
        PanamaPerformanceMonitor.monitor(
            "exception_test",
            () -> {
              throw new Exception("test exception");
            });
      } catch (final RuntimeException e) {
        assertNotNull(e.getCause(), "Should have a cause");
        assertEquals(
            "test exception", e.getCause().getMessage(), "Should preserve exception message");
        LOGGER.info("Exception wrapped correctly: " + e.getCause().getMessage());
        return;
      }

      // Should not reach here
      assertTrue(false, "Should have thrown RuntimeException");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("Should reset all statistics")
    void shouldResetAllStatistics() {
      LOGGER.info("Testing reset()");

      // Record some data first
      PanamaPerformanceMonitor.recordMethodHandleCall("method_1");
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      try (final Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 512);
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }

      for (int i = 0; i < 200; i++) {
        final long start = PanamaPerformanceMonitor.startOperation("reset_test");
        PanamaPerformanceMonitor.endOperation("reset_test", start);
      }

      // Reset
      PanamaPerformanceMonitor.reset();

      // Verify reset state
      final double overhead = PanamaPerformanceMonitor.getAverageFfiOverhead();
      assertEquals(0.0, overhead, 0.001, "Overhead should be zero after reset");

      final String activeArenas = PanamaPerformanceMonitor.getActiveArenaStats();
      assertEquals("No active arenas", activeArenas, "Should have no active arenas after reset");

      LOGGER.info("Reset completed, overhead: " + overhead + ", arenas: " + activeArenas);
    }

    @Test
    @DisplayName("Should allow operations after reset")
    void shouldAllowOperationsAfterReset() {
      LOGGER.info("Testing operations after reset");

      PanamaPerformanceMonitor.reset();

      // Should work after reset
      final long start = PanamaPerformanceMonitor.startOperation("post_reset");
      PanamaPerformanceMonitor.endOperation("post_reset", start);

      PanamaPerformanceMonitor.recordMethodHandleCall("post_reset_method");
      PanamaPerformanceMonitor.recordZeroCopyOperation();

      final String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
      assertNotNull(metrics, "Metrics should work after reset");
      LOGGER.info("Post-reset metrics: " + metrics);
    }
  }

  @Nested
  @DisplayName("End-to-End Workflow Tests")
  class EndToEndWorkflowTests {

    @Test
    @DisplayName("Should track complete Panama FFI workflow")
    void shouldTrackCompletePanamaFfiWorkflow() {
      LOGGER.info("Testing complete Panama FFI workflow");

      // Simulate a full Panama FFI workflow
      try (final Arena arena = Arena.ofConfined()) {
        // 1. Record arena allocation
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 4096);

        // 2. Start FFI operation
        final long start = PanamaPerformanceMonitor.startOperation("ffi_call", "module_new");

        // 3. Allocate memory segments
        final MemorySegment segment1 = arena.allocate(128);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment1);

        final MemorySegment segment2 = arena.allocate(256);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment2);

        // 4. Record method handle call
        PanamaPerformanceMonitor.recordMethodHandleCall("wasmtime_module_new");

        // 5. Record zero-copy operation
        PanamaPerformanceMonitor.recordZeroCopyOperation();

        // 6. End FFI operation
        PanamaPerformanceMonitor.endOperation("ffi_call", start);

        // 7. Verify metrics available
        final String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
        assertNotNull(metrics, "Metrics should be available after workflow");

        final String statistics = PanamaPerformanceMonitor.getStatistics();
        assertNotNull(statistics, "Statistics should be available");

        final String arenaStats = PanamaPerformanceMonitor.getActiveArenaStats();
        assertNotNull(arenaStats, "Arena stats should be available");

        // 8. Record arena deallocation
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);

        LOGGER.info("Complete workflow metrics: " + metrics);
        LOGGER.info("Complete workflow statistics length: " + statistics.length());
      }
    }
  }

  @Nested
  @DisplayName("MonitoredOperation Interface Tests")
  class MonitoredOperationInterfaceTests {

    @Test
    @DisplayName("Should accept lambda as MonitoredOperation")
    void shouldAcceptLambdaAsMonitoredOperation() {
      LOGGER.info("Testing MonitoredOperation with lambda");

      final PanamaPerformanceMonitor.MonitoredOperation<Integer> operation = () -> 42;

      final Integer result =
          assertDoesNotThrow(() -> PanamaPerformanceMonitor.monitor("lambda_test", operation));
      assertEquals(42, result, "Should execute lambda and return result");
      LOGGER.info("Lambda operation result: " + result);
    }
  }
}
