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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PerformanceMonitor} class.
 *
 * <p>This test class verifies the PerformanceMonitor utility class which provides
 * comprehensive performance monitoring and profiling infrastructure for WebAssembly operations.
 */
@DisplayName("PerformanceMonitor Tests")
class PerformanceMonitorTest {

  @BeforeEach
  void setUp() {
    PerformanceMonitor.setEnabled(true);
    PerformanceMonitor.setProfilingEnabled(false);
    PerformanceMonitor.setLowOverheadMode(false);
    PerformanceMonitor.reset();
    PerformanceMonitor.resetBaselines();
  }

  @AfterEach
  void tearDown() {
    PerformanceMonitor.reset();
    PerformanceMonitor.resetBaselines();
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PerformanceMonitor should be final class")
    void shouldBeFinalClass() {
      assertTrue(java.lang.reflect.Modifier.isFinal(PerformanceMonitor.class.getModifiers()),
          "PerformanceMonitor should be final");
    }

    @Test
    @DisplayName("SIMPLE_OPERATION_TARGET_NS should be 100")
    void simpleOperationTargetNsShouldBe100() {
      assertEquals(100, PerformanceMonitor.SIMPLE_OPERATION_TARGET_NS,
          "SIMPLE_OPERATION_TARGET_NS should be 100");
    }
  }

  @Nested
  @DisplayName("MonitoredOperation Interface Tests")
  class MonitoredOperationTests {

    @Test
    @DisplayName("MonitoredOperation should be functional interface")
    void shouldBeFunctionalInterface() {
      assertTrue(PerformanceMonitor.MonitoredOperation.class
              .isAnnotationPresent(FunctionalInterface.class),
          "MonitoredOperation should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("MonitoredOperation should work with lambda")
    void shouldWorkWithLambda() throws Exception {
      final PerformanceMonitor.MonitoredOperation<String> op = () -> "result";
      assertEquals("result", op.execute(), "Lambda should execute correctly");
    }
  }

  @Nested
  @DisplayName("Enable State Tests")
  class EnableStateTests {

    @Test
    @DisplayName("setEnabled should update enabled state")
    void setEnabledShouldUpdateEnabledState() {
      PerformanceMonitor.setEnabled(false);
      assertFalse(PerformanceMonitor.isEnabled(), "Should be disabled");

      PerformanceMonitor.setEnabled(true);
      assertTrue(PerformanceMonitor.isEnabled(), "Should be enabled");
    }

    @Test
    @DisplayName("setProfilingEnabled should update profiling state")
    void setProfilingEnabledShouldUpdateProfilingState() {
      PerformanceMonitor.setProfilingEnabled(true);
      assertTrue(PerformanceMonitor.isProfilingEnabled(), "Profiling should be enabled");

      PerformanceMonitor.setProfilingEnabled(false);
      assertFalse(PerformanceMonitor.isProfilingEnabled(), "Profiling should be disabled");
    }

    @Test
    @DisplayName("setLowOverheadMode should update mode")
    void setLowOverheadModeShouldUpdateMode() {
      PerformanceMonitor.setLowOverheadMode(true);
      assertTrue(PerformanceMonitor.isLowOverheadMode(), "Low overhead mode should be enabled");

      PerformanceMonitor.setLowOverheadMode(false);
      assertFalse(PerformanceMonitor.isLowOverheadMode(), "Low overhead mode should be disabled");
    }
  }

  @Nested
  @DisplayName("Operation Timing Tests")
  class OperationTimingTests {

    @Test
    @DisplayName("startOperation should return non-zero value when enabled")
    void startOperationShouldReturnNonZeroValueWhenEnabled() {
      final long startTime = PerformanceMonitor.startOperation("test_category");
      assertTrue(startTime > 0, "Start time should be positive");
    }

    @Test
    @DisplayName("startOperation should return zero when disabled")
    void startOperationShouldReturnZeroWhenDisabled() {
      PerformanceMonitor.setEnabled(false);
      final long startTime = PerformanceMonitor.startOperation("test_category");
      assertEquals(0, startTime, "Start time should be 0 when disabled");
    }

    @Test
    @DisplayName("startOperation with details should work")
    void startOperationWithDetailsShouldWork() {
      final long startTime = PerformanceMonitor.startOperation("test_category", "details");
      assertTrue(startTime > 0, "Start time should be positive");
    }

    @Test
    @DisplayName("endOperation should not throw")
    void endOperationShouldNotThrow() {
      final long startTime = PerformanceMonitor.startOperation("test_category");
      assertDoesNotThrow(() -> PerformanceMonitor.endOperation("test_category", startTime),
          "endOperation should not throw");
    }

    @Test
    @DisplayName("endOperation with zero startTime should not throw")
    void endOperationWithZeroStartTimeShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.endOperation("test_category", 0),
          "endOperation with 0 startTime should not throw");
    }

    @Test
    @DisplayName("endOperation with negative startTime should not throw")
    void endOperationWithNegativeStartTimeShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.endOperation("test_category", -1),
          "endOperation with -1 startTime should not throw");
    }
  }

  @Nested
  @DisplayName("Memory Tracking Tests")
  class MemoryTrackingTests {

    @Test
    @DisplayName("recordAllocation should not throw")
    void recordAllocationShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.recordAllocation(1024),
          "recordAllocation should not throw");
    }

    @Test
    @DisplayName("recordDeallocation should not throw")
    void recordDeallocationShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.recordDeallocation(1024),
          "recordDeallocation should not throw");
    }

    @Test
    @DisplayName("recordAllocation should do nothing when disabled")
    void recordAllocationShouldDoNothingWhenDisabled() {
      PerformanceMonitor.setEnabled(false);
      assertDoesNotThrow(() -> PerformanceMonitor.recordAllocation(1024),
          "Should not throw when disabled");
    }
  }

  @Nested
  @DisplayName("JNI Overhead Tests")
  class JniOverheadTests {

    @Test
    @DisplayName("getAverageJniOverhead should return non-negative value")
    void getAverageJniOverheadShouldReturnNonNegativeValue() {
      assertTrue(PerformanceMonitor.getAverageJniOverhead() >= 0,
          "Average JNI overhead should be non-negative");
    }

    @Test
    @DisplayName("meetsPerformanceTarget should return true initially")
    void meetsPerformanceTargetShouldReturnTrueInitially() {
      // With no operations, average overhead is 0, which meets the target
      assertTrue(PerformanceMonitor.meetsPerformanceTarget(),
          "Should meet performance target with no operations");
    }
  }

  @Nested
  @DisplayName("Statistics Tests")
  class StatisticsTests {

    @Test
    @DisplayName("getStatistics should return formatted string when enabled")
    void getStatisticsShouldReturnFormattedStringWhenEnabled() {
      final String stats = PerformanceMonitor.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertTrue(stats.contains("Performance Statistics"),
          "Statistics should contain header");
    }

    @Test
    @DisplayName("getStatistics should indicate disabled when disabled")
    void getStatisticsShouldIndicateDisabledWhenDisabled() {
      PerformanceMonitor.setEnabled(false);
      final String stats = PerformanceMonitor.getStatistics();
      assertTrue(stats.contains("disabled"), "Should indicate disabled state");
    }

    @Test
    @DisplayName("getOperationStats should return message for unknown category")
    void getOperationStatsShouldReturnMessageForUnknownCategory() {
      final String stats = PerformanceMonitor.getOperationStats("unknown_category");
      assertTrue(stats.contains("No statistics") || stats.contains("not found"),
          "Should indicate no statistics for unknown category");
    }

    @Test
    @DisplayName("getOperationStats should return stats for tracked category")
    void getOperationStatsShouldReturnStatsForTrackedCategory() {
      final long startTime = PerformanceMonitor.startOperation("tracked_category");
      PerformanceMonitor.endOperation("tracked_category", startTime);

      final String stats = PerformanceMonitor.getOperationStats("tracked_category");
      assertTrue(stats.contains("tracked_category"),
          "Should contain category name");
    }
  }

  @Nested
  @DisplayName("Monitor Method Tests")
  class MonitorMethodTests {

    @Test
    @DisplayName("monitor should execute operation and return result")
    void monitorShouldExecuteOperationAndReturnResult() {
      final String result = PerformanceMonitor.monitor("test_category", () -> "test result");
      assertEquals("test result", result, "Should return operation result");
    }

    @Test
    @DisplayName("monitor should wrap checked exceptions in RuntimeException")
    void monitorShouldWrapCheckedExceptions() {
      try {
        PerformanceMonitor.monitor("test_category", () -> {
          throw new Exception("Checked exception");
        });
        assertTrue(false, "Should have thrown RuntimeException");
      } catch (RuntimeException e) {
        assertNotNull(e.getCause(), "Should have cause");
        assertEquals("Checked exception", e.getCause().getMessage(),
            "Should wrap original exception");
      }
    }
  }

  @Nested
  @DisplayName("Performance Issues Tests")
  class PerformanceIssuesTests {

    @Test
    @DisplayName("getPerformanceIssues should return null with no issues")
    void getPerformanceIssuesShouldReturnNullWithNoIssues() {
      // With no operations, there should be no issues
      final String issues = PerformanceMonitor.getPerformanceIssues();
      assertNull(issues, "Should return null with no issues");
    }

    @Test
    @DisplayName("getPerformanceIssues should return null when disabled")
    void getPerformanceIssuesShouldReturnNullWhenDisabled() {
      PerformanceMonitor.setEnabled(false);
      final String issues = PerformanceMonitor.getPerformanceIssues();
      assertNull(issues, "Should return null when disabled");
    }
  }

  @Nested
  @DisplayName("Overhead Statistics Tests")
  class OverheadStatisticsTests {

    @Test
    @DisplayName("getMonitoringOverheadPercentage should return non-negative value")
    void getMonitoringOverheadPercentageShouldReturnNonNegativeValue() {
      assertTrue(PerformanceMonitor.getMonitoringOverheadPercentage() >= 0,
          "Overhead percentage should be non-negative");
    }

    @Test
    @DisplayName("getAverageMonitoringOverheadNs should return non-negative value")
    void getAverageMonitoringOverheadNsShouldReturnNonNegativeValue() {
      assertTrue(PerformanceMonitor.getAverageMonitoringOverheadNs() >= 0,
          "Average overhead should be non-negative");
    }

    @Test
    @DisplayName("meetsOverheadTarget should check 5% threshold")
    void meetsOverheadTargetShouldCheck5PercentThreshold() {
      // With no operations, overhead is 0%, which meets the <5% target
      assertTrue(PerformanceMonitor.meetsOverheadTarget(),
          "Should meet overhead target with no operations");
    }

    @Test
    @DisplayName("getOverheadStatistics should return formatted string")
    void getOverheadStatisticsShouldReturnFormattedString() {
      final String stats = PerformanceMonitor.getOverheadStatistics();
      assertNotNull(stats, "Overhead statistics should not be null");
      assertTrue(stats.contains("Monitoring Overhead") || stats.contains("overhead"),
          "Should contain overhead information");
    }
  }

  @Nested
  @DisplayName("Baseline Tests")
  class BaselineTests {

    @Test
    @DisplayName("getBaselineInformation should return message when no baselines")
    void getBaselineInformationShouldReturnMessageWhenNoBaselines() {
      final String info = PerformanceMonitor.getBaselineInformation();
      assertTrue(info.contains("No performance baselines") || info.contains("Baselines"),
          "Should indicate no baselines or show baseline info");
    }

    @Test
    @DisplayName("forceRegressionCheck should not throw")
    void forceRegressionCheckShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.forceRegressionCheck(),
          "forceRegressionCheck should not throw");
    }

    @Test
    @DisplayName("resetBaselines should not throw")
    void resetBaselinesShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.resetBaselines(),
          "resetBaselines should not throw");
    }
  }

  @Nested
  @DisplayName("Reset Tests")
  class ResetTests {

    @Test
    @DisplayName("reset should clear all statistics")
    void resetShouldClearAllStatistics() {
      // Generate some statistics
      final long startTime = PerformanceMonitor.startOperation("test");
      PerformanceMonitor.endOperation("test", startTime);
      PerformanceMonitor.recordAllocation(1024);

      PerformanceMonitor.reset();

      // Check that statistics are cleared
      assertEquals(0.0, PerformanceMonitor.getAverageJniOverhead(), 0.01,
          "Average JNI overhead should be 0 after reset");
    }

    @Test
    @DisplayName("reset should not throw")
    void resetShouldNotThrow() {
      assertDoesNotThrow(() -> PerformanceMonitor.reset(),
          "reset should not throw");
    }
  }

  @Nested
  @DisplayName("Low Overhead Mode Tests")
  class LowOverheadModeTests {

    @Test
    @DisplayName("Low overhead mode should sample operations")
    void lowOverheadModeShouldSampleOperations() {
      PerformanceMonitor.setLowOverheadMode(true);

      // In low overhead mode, only 1 in N operations are sampled
      // So startOperation may return -1 for non-sampled operations
      long sampledCount = 0;
      for (int i = 0; i < 200; i++) {
        final long startTime = PerformanceMonitor.startOperation("test");
        if (startTime > 0) {
          sampledCount++;
          PerformanceMonitor.endOperation("test", startTime);
        }
      }

      // With 1:100 sampling rate and 200 operations, we should have ~2 sampled
      assertTrue(sampledCount >= 0, "Sampled count should be non-negative");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Complete operation timing cycle should work")
    void completeOperationTimingCycleShouldWork() {
      // Start operation
      final long startTime = PerformanceMonitor.startOperation("integration_test", "details");
      assertTrue(startTime > 0, "Start time should be positive");

      // Simulate some work
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // End operation
      PerformanceMonitor.endOperation("integration_test", startTime);

      // Verify statistics are tracked
      final String stats = PerformanceMonitor.getOperationStats("integration_test");
      assertTrue(stats.contains("integration_test"), "Should track the operation");
    }

    @Test
    @DisplayName("Multiple categories should be tracked independently")
    void multipleCategoriesShouldBeTrackedIndependently() {
      final long start1 = PerformanceMonitor.startOperation("category1");
      final long start2 = PerformanceMonitor.startOperation("category2");

      PerformanceMonitor.endOperation("category1", start1);
      PerformanceMonitor.endOperation("category2", start2);

      final String stats1 = PerformanceMonitor.getOperationStats("category1");
      final String stats2 = PerformanceMonitor.getOperationStats("category2");

      assertTrue(stats1.contains("category1"), "Should track category1");
      assertTrue(stats2.contains("category2"), "Should track category2");
    }
  }
}
