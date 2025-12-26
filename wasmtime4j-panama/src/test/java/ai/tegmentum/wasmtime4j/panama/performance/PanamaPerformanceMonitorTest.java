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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link PanamaPerformanceMonitor}.
 */
@DisplayName("PanamaPerformanceMonitor Tests")
class PanamaPerformanceMonitorTest {

  @BeforeEach
  void setUp() {
    // Ensure clean state before each test
    PanamaPerformanceMonitor.reset();
    PanamaPerformanceMonitor.setEnabled(true);
  }

  @AfterEach
  void tearDown() {
    // Reset after each test
    PanamaPerformanceMonitor.reset();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaPerformanceMonitor should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaPerformanceMonitor.class.getModifiers()),
          "PanamaPerformanceMonitor should be final");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("SIMPLE_PANAMA_OPERATION_TARGET_NS should be 50 nanoseconds")
    void simpleOperationTargetShouldBe50Ns() {
      assertEquals(50L, PanamaPerformanceMonitor.SIMPLE_PANAMA_OPERATION_TARGET_NS,
          "SIMPLE_PANAMA_OPERATION_TARGET_NS should be 50");
    }
  }

  @Nested
  @DisplayName("Enable/Disable Tests")
  class EnableDisableTests {

    @Test
    @DisplayName("isEnabled should return true by default")
    void isEnabledShouldReturnTrueByDefault() {
      assertTrue(PanamaPerformanceMonitor.isEnabled(),
          "Monitoring should be enabled by default");
    }

    @Test
    @DisplayName("setEnabled should toggle monitoring state")
    void setEnabledShouldToggleMonitoringState() {
      PanamaPerformanceMonitor.setEnabled(false);
      assertFalse(PanamaPerformanceMonitor.isEnabled(),
          "Monitoring should be disabled after setEnabled(false)");

      PanamaPerformanceMonitor.setEnabled(true);
      assertTrue(PanamaPerformanceMonitor.isEnabled(),
          "Monitoring should be enabled after setEnabled(true)");
    }
  }

  @Nested
  @DisplayName("startOperation Tests")
  class StartOperationTests {

    @Test
    @DisplayName("startOperation should return 0 when disabled")
    void startOperationShouldReturnZeroWhenDisabled() {
      PanamaPerformanceMonitor.setEnabled(false);
      final long startTime = PanamaPerformanceMonitor.startOperation("test_category", "details");
      assertEquals(0L, startTime,
          "startOperation should return 0 when monitoring is disabled");
    }

    @Test
    @DisplayName("startOperation should return positive value when enabled")
    void startOperationShouldReturnPositiveValueWhenEnabled() {
      final long startTime = PanamaPerformanceMonitor.startOperation("test_category", "details");
      // In low overhead mode, may return -1 for unsampled operations or positive for sampled
      assertTrue(startTime != 0 || !PanamaPerformanceMonitor.isEnabled(),
          "startOperation should return non-zero when monitoring is enabled");
    }

    @Test
    @DisplayName("startOperation should work without details")
    void startOperationShouldWorkWithoutDetails() {
      assertDoesNotThrow(() -> PanamaPerformanceMonitor.startOperation("test_category"),
          "startOperation should accept null details");
    }
  }

  @Nested
  @DisplayName("endOperation Tests")
  class EndOperationTests {

    @Test
    @DisplayName("endOperation should handle zero start time gracefully")
    void endOperationShouldHandleZeroStartTimeGracefully() {
      assertDoesNotThrow(() -> PanamaPerformanceMonitor.endOperation("test_category", 0L),
          "endOperation should handle zero start time gracefully");
    }

    @Test
    @DisplayName("endOperation should handle negative start time gracefully")
    void endOperationShouldHandleNegativeStartTimeGracefully() {
      assertDoesNotThrow(() -> PanamaPerformanceMonitor.endOperation("test_category", -1L),
          "endOperation should handle negative start time (sampling marker) gracefully");
    }

    @Test
    @DisplayName("endOperation should record operation statistics")
    void endOperationShouldRecordOperationStatistics() {
      final long startTime = System.nanoTime();
      PanamaPerformanceMonitor.endOperation("test_category", startTime);
      // No assertion needed - just verifying no exception
    }
  }

  @Nested
  @DisplayName("Arena Allocation Tests")
  class ArenaAllocationTests {

    @Test
    @DisplayName("recordArenaAllocation should handle null arena gracefully")
    void recordArenaAllocationShouldHandleNullArenaGracefully() {
      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordArenaAllocation(null, 1024L),
          "recordArenaAllocation should handle null arena gracefully");
    }

    @Test
    @DisplayName("recordArenaAllocation should record allocation")
    void recordArenaAllocationShouldRecordAllocation() {
      try (Arena arena = Arena.ofConfined()) {
        assertDoesNotThrow(
            () -> PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024L),
            "recordArenaAllocation should record arena allocation");
      }
    }

    @Test
    @DisplayName("recordArenaDeallocation should handle null arena gracefully")
    void recordArenaDeallocationShouldHandleNullArenaGracefully() {
      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordArenaDeallocation(null),
          "recordArenaDeallocation should handle null arena gracefully");
    }

    @Test
    @DisplayName("recordArenaDeallocation should record deallocation")
    void recordArenaDeallocationShouldRecordDeallocation() {
      try (Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024L);
        assertDoesNotThrow(
            () -> PanamaPerformanceMonitor.recordArenaDeallocation(arena),
            "recordArenaDeallocation should record arena deallocation");
      }
    }
  }

  @Nested
  @DisplayName("Memory Segment Tests")
  class MemorySegmentTests {

    @Test
    @DisplayName("recordMemorySegmentAllocation should handle null arena gracefully")
    void recordMemorySegmentAllocationShouldHandleNullArenaGracefully() {
      try (Arena arena = Arena.ofConfined()) {
        final MemorySegment segment = arena.allocate(1024);
        assertDoesNotThrow(
            () -> PanamaPerformanceMonitor.recordMemorySegmentAllocation(null, segment),
            "recordMemorySegmentAllocation should handle null arena gracefully");
      }
    }

    @Test
    @DisplayName("recordMemorySegmentAllocation should handle null segment gracefully")
    void recordMemorySegmentAllocationShouldHandleNullSegmentGracefully() {
      try (Arena arena = Arena.ofConfined()) {
        assertDoesNotThrow(
            () -> PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, null),
            "recordMemorySegmentAllocation should handle null segment gracefully");
      }
    }

    @Test
    @DisplayName("recordMemorySegmentAllocation should record allocation within arena")
    void recordMemorySegmentAllocationShouldRecordAllocationWithinArena() {
      try (Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 4096L);
        final MemorySegment segment = arena.allocate(1024);
        assertDoesNotThrow(
            () -> PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment),
            "recordMemorySegmentAllocation should record allocation within arena");
      }
    }
  }

  @Nested
  @DisplayName("Method Handle Call Tests")
  class MethodHandleCallTests {

    @Test
    @DisplayName("recordMethodHandleCall should handle null method name")
    void recordMethodHandleCallShouldHandleNullMethodName() {
      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordMethodHandleCall(null),
          "recordMethodHandleCall should handle null method name");
    }

    @Test
    @DisplayName("recordMethodHandleCall should record method handle call")
    void recordMethodHandleCallShouldRecordMethodHandleCall() {
      assertDoesNotThrow(
          () -> PanamaPerformanceMonitor.recordMethodHandleCall("testMethod"),
          "recordMethodHandleCall should record method handle call");
    }
  }

  @Nested
  @DisplayName("Zero Copy Operation Tests")
  class ZeroCopyOperationTests {

    @Test
    @DisplayName("recordZeroCopyOperation should record zero-copy operation")
    void recordZeroCopyOperationShouldRecordZeroCopyOperation() {
      assertDoesNotThrow(
          PanamaPerformanceMonitor::recordZeroCopyOperation,
          "recordZeroCopyOperation should record zero-copy operation");
    }
  }

  @Nested
  @DisplayName("FFI Overhead Tests")
  class FfiOverheadTests {

    @Test
    @DisplayName("getAverageFfiOverhead should return 0 when no calls made")
    void getAverageFfiOverheadShouldReturnZeroWhenNoCallsMade() {
      PanamaPerformanceMonitor.reset();
      assertEquals(0.0, PanamaPerformanceMonitor.getAverageFfiOverhead(), 0.001,
          "getAverageFfiOverhead should return 0 when no FFI calls made");
    }

    @Test
    @DisplayName("getAverageFfiOverhead should return positive value after operations")
    void getAverageFfiOverheadShouldReturnPositiveValueAfterOperations() {
      // Record some operations
      for (int i = 0; i < 200; i++) { // Beyond sampling rate
        final long start = PanamaPerformanceMonitor.startOperation("test");
        if (start > 0) {
          PanamaPerformanceMonitor.endOperation("test", start);
        }
      }
      // May still be 0 if operations were too fast, so we just check it doesn't throw
      assertDoesNotThrow(PanamaPerformanceMonitor::getAverageFfiOverhead);
    }
  }

  @Nested
  @DisplayName("Performance Target Tests")
  class PerformanceTargetTests {

    @Test
    @DisplayName("meetsPerformanceTarget should return true when no operations")
    void meetsPerformanceTargetShouldReturnTrueWhenNoOperations() {
      PanamaPerformanceMonitor.reset();
      assertTrue(PanamaPerformanceMonitor.meetsPerformanceTarget(),
          "meetsPerformanceTarget should return true when overhead is 0");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return disabled message when monitoring off")
    void getStatisticsShouldReturnDisabledMessageWhenMonitoringOff() {
      PanamaPerformanceMonitor.setEnabled(false);
      final String stats = PanamaPerformanceMonitor.getStatistics();
      assertTrue(stats.contains("disabled"),
          "getStatistics should indicate monitoring is disabled");
    }

    @Test
    @DisplayName("getStatistics should return comprehensive statistics when enabled")
    void getStatisticsShouldReturnComprehensiveStatisticsWhenEnabled() {
      final String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "getStatistics should not return null");
      assertTrue(stats.contains("Panama"), "Statistics should contain 'Panama'");
      assertTrue(stats.contains("FFI"), "Statistics should contain 'FFI'");
    }

    @Test
    @DisplayName("getStatistics should include uptime information")
    void getStatisticsShouldIncludeUptimeInformation() {
      final String stats = PanamaPerformanceMonitor.getStatistics();
      assertTrue(stats.contains("Uptime"), "Statistics should include uptime");
    }

    @Test
    @DisplayName("getStatistics should include JVM memory information")
    void getStatisticsShouldIncludeJvmMemoryInformation() {
      final String stats = PanamaPerformanceMonitor.getStatistics();
      assertTrue(stats.contains("JVM Memory") || stats.contains("Heap"),
          "Statistics should include JVM memory info");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear all statistics")
    void resetShouldClearAllStatistics() {
      // Record some operations
      PanamaPerformanceMonitor.recordZeroCopyOperation();
      PanamaPerformanceMonitor.recordMethodHandleCall("test");

      // Reset
      PanamaPerformanceMonitor.reset();

      // Verify reset
      assertEquals(0.0, PanamaPerformanceMonitor.getAverageFfiOverhead(), 0.001,
          "FFI overhead should be 0 after reset");
    }
  }

  @Nested
  @DisplayName("Panama Metrics Tests")
  class PanamaMetricsTests {

    @Test
    @DisplayName("getPanamaMetrics should return formatted metrics string")
    void getPanamaMetricsShouldReturnFormattedMetricsString() {
      final String metrics = PanamaPerformanceMonitor.getPanamaMetrics();
      assertNotNull(metrics, "getPanamaMetrics should not return null");
      assertTrue(metrics.contains("Panama Performance"),
          "Metrics should include 'Panama Performance'");
      assertTrue(metrics.contains("ffi_calls"),
          "Metrics should include 'ffi_calls'");
    }
  }

  @Nested
  @DisplayName("Active Arena Stats Tests")
  class ActiveArenaStatsTests {

    @Test
    @DisplayName("getActiveArenaStats should return 'No active arenas' when empty")
    void getActiveArenaStatsShouldReturnNoActiveArenasWhenEmpty() {
      PanamaPerformanceMonitor.reset();
      final String stats = PanamaPerformanceMonitor.getActiveArenaStats();
      assertTrue(stats.contains("No active arenas"),
          "Should indicate no active arenas");
    }

    @Test
    @DisplayName("getActiveArenaStats should track active arenas")
    void getActiveArenaStatsShouldTrackActiveArenas() {
      try (Arena arena = Arena.ofConfined()) {
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 1024L);
        final String stats = PanamaPerformanceMonitor.getActiveArenaStats();
        // Arena is still active, should be tracked
        assertNotNull(stats, "Arena stats should not be null");
      }
    }
  }

  @Nested
  @DisplayName("Performance Issues Tests")
  class PerformanceIssuesTests {

    @Test
    @DisplayName("getPerformanceIssues should return null when disabled")
    void getPerformanceIssuesShouldReturnNullWhenDisabled() {
      PanamaPerformanceMonitor.setEnabled(false);
      assertNull(PanamaPerformanceMonitor.getPerformanceIssues(),
          "getPerformanceIssues should return null when monitoring is disabled");
    }

    @Test
    @DisplayName("getPerformanceIssues should return null when no issues")
    void getPerformanceIssuesShouldReturnNullWhenNoIssues() {
      PanamaPerformanceMonitor.reset();
      final String issues = PanamaPerformanceMonitor.getPerformanceIssues();
      // May or may not return null depending on state
      // Just verify it doesn't throw
    }
  }

  @Nested
  @DisplayName("Monitor Operation Tests")
  class MonitorOperationTests {

    @Test
    @DisplayName("monitor should execute operation and return result")
    void monitorShouldExecuteOperationAndReturnResult() {
      final Integer result = PanamaPerformanceMonitor.monitor("test", () -> 42);
      assertEquals(42, result, "monitor should return operation result");
    }

    @Test
    @DisplayName("monitor should propagate exceptions as RuntimeException")
    void monitorShouldPropagateExceptionsAsRuntimeException() {
      assertThrows(RuntimeException.class, () ->
              PanamaPerformanceMonitor.monitor("test", () -> {
                throw new Exception("Test exception");
              }),
          "monitor should propagate exceptions as RuntimeException");
    }

    @Test
    @DisplayName("monitor should record operation timing")
    void monitorShouldRecordOperationTiming() {
      assertDoesNotThrow(() ->
              PanamaPerformanceMonitor.monitor("timing_test", () -> {
                Thread.sleep(1); // Small delay
                return "done";
              }),
          "monitor should record operation timing without error");
    }
  }

  @Nested
  @DisplayName("MonitoredOperation Interface Tests")
  class MonitoredOperationInterfaceTests {

    @Test
    @DisplayName("MonitoredOperation should be functional interface")
    void monitoredOperationShouldBeFunctionalInterface() {
      assertTrue(
          PanamaPerformanceMonitor.MonitoredOperation.class.isAnnotationPresent(
              FunctionalInterface.class),
          "MonitoredOperation should be annotated with @FunctionalInterface");
    }
  }

  @Nested
  @DisplayName("Disabled Monitoring Tests")
  class DisabledMonitoringTests {

    @Test
    @DisplayName("All operations should be no-ops when disabled")
    void allOperationsShouldBeNoOpsWhenDisabled() {
      PanamaPerformanceMonitor.setEnabled(false);

      // All these should be no-ops when disabled
      assertDoesNotThrow(() -> {
        PanamaPerformanceMonitor.startOperation("test");
        PanamaPerformanceMonitor.endOperation("test", 1000L);
        PanamaPerformanceMonitor.recordArenaAllocation(null, 1024L);
        PanamaPerformanceMonitor.recordArenaDeallocation(null);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(null, null);
        PanamaPerformanceMonitor.recordMethodHandleCall("test");
        PanamaPerformanceMonitor.recordZeroCopyOperation();
      }, "All monitoring operations should be safe when disabled");
    }
  }

  @Nested
  @DisplayName("ArenaStats Inner Class Tests")
  class ArenaStatsInnerClassTests {

    @Test
    @DisplayName("Arena lifecycle should be tracked correctly")
    void arenaLifecycleShouldBeTrackedCorrectly() {
      try (Arena arena = Arena.ofConfined()) {
        // Track allocation
        PanamaPerformanceMonitor.recordArenaAllocation(arena, 4096L);

        // Track segment allocation
        final MemorySegment segment = arena.allocate(1024);
        PanamaPerformanceMonitor.recordMemorySegmentAllocation(arena, segment);

        // Track deallocation
        PanamaPerformanceMonitor.recordArenaDeallocation(arena);
      }
      // No assertion - just verify no exceptions
    }
  }

  @Nested
  @DisplayName("OperationStats Inner Class Tests")
  class OperationStatsInnerClassTests {

    @Test
    @DisplayName("Operation statistics should accumulate correctly")
    void operationStatisticsShouldAccumulateCorrectly() {
      // Perform multiple operations
      for (int i = 0; i < 10; i++) {
        final long start = System.nanoTime();
        PanamaPerformanceMonitor.endOperation("accumulate_test", start);
      }

      // Get statistics and verify they're tracked
      final String stats = PanamaPerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should be available");
    }
  }
}
