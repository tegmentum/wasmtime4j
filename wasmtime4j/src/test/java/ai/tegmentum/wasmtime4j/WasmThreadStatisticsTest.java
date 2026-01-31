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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmThreadStatistics} class.
 *
 * <p>WasmThreadStatistics provides performance metrics for WebAssembly thread execution including
 * function counts, timing, memory usage, and operation counters. Negative values are clamped to
 * zero.
 */
@DisplayName("WasmThreadStatistics Tests")
class WasmThreadStatisticsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasmThreadStatistics.class.getModifiers()),
          "WasmThreadStatistics should be public");
      assertTrue(
          Modifier.isFinal(WasmThreadStatistics.class.getModifiers()),
          "WasmThreadStatistics should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all positive parameters")
    void shouldCreateInstanceWithAllPositiveParameters() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(100L, 5_000_000_000L, 50L, 10000L, 20L, 2048L);

      assertEquals(100L, stats.getFunctionsExecuted(), "Functions executed should be 100");
      assertEquals(5_000_000_000L, stats.getTotalExecutionTime(),
          "Total execution time should be 5000000000 ns");
      assertEquals(50L, stats.getAtomicOperations(), "Atomic operations should be 50");
      assertEquals(10000L, stats.getMemoryAccesses(), "Memory accesses should be 10000");
      assertEquals(20L, stats.getWaitNotifyOperations(), "Wait/notify operations should be 20");
      assertEquals(2048L, stats.getPeakMemoryUsage(), "Peak memory usage should be 2048");
    }

    @Test
    @DisplayName("should clamp negative functionsExecuted to zero")
    void shouldClampNegativeFunctionsExecutedToZero() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(-5L, 0L, 0L, 0L, 0L, 0L);

      assertEquals(0L, stats.getFunctionsExecuted(),
          "Negative functionsExecuted should be clamped to 0");
    }

    @Test
    @DisplayName("should clamp negative totalExecutionTime to zero")
    void shouldClampNegativeTotalExecutionTimeToZero() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(0L, -100L, 0L, 0L, 0L, 0L);

      assertEquals(0L, stats.getTotalExecutionTime(),
          "Negative totalExecutionTime should be clamped to 0");
    }

    @Test
    @DisplayName("should clamp negative atomicOperations to zero")
    void shouldClampNegativeAtomicOperationsToZero() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(0L, 0L, -10L, 0L, 0L, 0L);

      assertEquals(0L, stats.getAtomicOperations(),
          "Negative atomicOperations should be clamped to 0");
    }

    @Test
    @DisplayName("should clamp negative memoryAccesses to zero")
    void shouldClampNegativeMemoryAccessesToZero() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(0L, 0L, 0L, -200L, 0L, 0L);

      assertEquals(0L, stats.getMemoryAccesses(),
          "Negative memoryAccesses should be clamped to 0");
    }

    @Test
    @DisplayName("should clamp negative waitNotifyOperations to zero")
    void shouldClampNegativeWaitNotifyOperationsToZero() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(0L, 0L, 0L, 0L, -3L, 0L);

      assertEquals(0L, stats.getWaitNotifyOperations(),
          "Negative waitNotifyOperations should be clamped to 0");
    }

    @Test
    @DisplayName("should clamp negative peakMemoryUsage to zero")
    void shouldClampNegativePeakMemoryUsageToZero() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, -1024L);

      assertEquals(0L, stats.getPeakMemoryUsage(),
          "Negative peakMemoryUsage should be clamped to 0");
    }

    @Test
    @DisplayName("should clamp all negative values simultaneously")
    void shouldClampAllNegativeValuesSimultaneously() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(-1L, -2L, -3L, -4L, -5L, -6L);

      assertEquals(0L, stats.getFunctionsExecuted(), "functionsExecuted should be clamped to 0");
      assertEquals(0L, stats.getTotalExecutionTime(), "totalExecutionTime should be clamped to 0");
      assertEquals(0L, stats.getAtomicOperations(), "atomicOperations should be clamped to 0");
      assertEquals(0L, stats.getMemoryAccesses(), "memoryAccesses should be clamped to 0");
      assertEquals(0L, stats.getWaitNotifyOperations(),
          "waitNotifyOperations should be clamped to 0");
      assertEquals(0L, stats.getPeakMemoryUsage(), "peakMemoryUsage should be clamped to 0");
    }
  }

  @Nested
  @DisplayName("Derived Metric Tests")
  class DerivedMetricTests {

    @Test
    @DisplayName("getTotalExecutionTimeMillis should convert nanoseconds to milliseconds")
    void getTotalExecutionTimeMillisShouldConvertNsToMs() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 5_000_000L, 0L, 0L, 0L, 0L);

      assertEquals(5L, stats.getTotalExecutionTimeMillis(),
          "5,000,000 ns should convert to 5 ms");
    }

    @Test
    @DisplayName("getTotalExecutionTimeMillis should truncate sub-millisecond time")
    void getTotalExecutionTimeMillisShouldTruncateSubMillisecond() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 1_500_000L, 0L, 0L, 0L, 0L);

      assertEquals(1L, stats.getTotalExecutionTimeMillis(),
          "1,500,000 ns should truncate to 1 ms");
    }

    @Test
    @DisplayName("getAverageExecutionTime should compute correct average")
    void getAverageExecutionTimeShouldComputeCorrectAverage() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(10L, 1000L, 0L, 0L, 0L, 0L);

      assertEquals(100L, stats.getAverageExecutionTime(),
          "Average execution time should be 100 ns per function");
    }

    @Test
    @DisplayName("getAverageExecutionTime should return zero when no functions executed")
    void getAverageExecutionTimeShouldReturnZeroWhenNoFunctions() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 5000L, 0L, 0L, 0L, 0L);

      assertEquals(0L, stats.getAverageExecutionTime(),
          "Average execution time should be 0 when no functions executed");
    }

    @Test
    @DisplayName("getPeakMemoryUsageKB should convert bytes to kilobytes")
    void getPeakMemoryUsageKbShouldConvertBytesToKb() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, 10240L);

      assertEquals(10L, stats.getPeakMemoryUsageKB(),
          "10240 bytes should convert to 10 KB");
    }

    @Test
    @DisplayName("getPeakMemoryUsageMB should convert bytes to megabytes")
    void getPeakMemoryUsageMbShouldConvertBytesToMb() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, 10_485_760L);

      assertEquals(10L, stats.getPeakMemoryUsageMB(),
          "10,485,760 bytes should convert to 10 MB");
    }

    @Test
    @DisplayName("getOperationsPerSecond should compute correct rate")
    void getOperationsPerSecondShouldComputeCorrectRate() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(100L, 1_000_000_000L, 50L, 0L, 20L, 0L);

      final double expectedOpsPerSec = (100.0 + 50.0 + 20.0) / 1.0;
      assertEquals(expectedOpsPerSec, stats.getOperationsPerSecond(), 0.001,
          "Operations per second should be 170.0 for 1 second of execution");
    }

    @Test
    @DisplayName("getOperationsPerSecond should return zero when no execution time")
    void getOperationsPerSecondShouldReturnZeroWhenNoExecutionTime() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(100L, 0L, 50L, 0L, 20L, 0L);

      assertEquals(0.0, stats.getOperationsPerSecond(), 0.001,
          "Operations per second should be 0.0 when no execution time");
    }

    @Test
    @DisplayName("getMemoryAccessRate should compute correct rate")
    void getMemoryAccessRateShouldComputeCorrectRate() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 2_000_000_000L, 0L, 5000L, 0L, 0L);

      final double expectedRate = 5000.0 / 2.0;
      assertEquals(expectedRate, stats.getMemoryAccessRate(), 0.001,
          "Memory access rate should be 2500.0 accesses/sec for 2 seconds");
    }

    @Test
    @DisplayName("getMemoryAccessRate should return zero when no execution time")
    void getMemoryAccessRateShouldReturnZeroWhenNoExecutionTime() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(0L, 0L, 0L, 5000L, 0L, 0L);

      assertEquals(0.0, stats.getMemoryAccessRate(), 0.001,
          "Memory access rate should be 0.0 when no execution time");
    }
  }

  @Nested
  @DisplayName("Empty Factory Tests")
  class EmptyFactoryTests {

    @Test
    @DisplayName("empty should create statistics with all zeros")
    void emptyShouldCreateStatisticsWithAllZeros() {
      final WasmThreadStatistics stats = WasmThreadStatistics.empty();

      assertNotNull(stats, "Empty statistics should not be null");
      assertEquals(0L, stats.getFunctionsExecuted(), "Functions executed should be 0");
      assertEquals(0L, stats.getTotalExecutionTime(), "Total execution time should be 0");
      assertEquals(0L, stats.getAtomicOperations(), "Atomic operations should be 0");
      assertEquals(0L, stats.getMemoryAccesses(), "Memory accesses should be 0");
      assertEquals(0L, stats.getWaitNotifyOperations(), "Wait/notify operations should be 0");
      assertEquals(0L, stats.getPeakMemoryUsage(), "Peak memory usage should be 0");
    }

    @Test
    @DisplayName("empty should create equal objects on multiple calls")
    void emptyShouldCreateEqualObjects() {
      final WasmThreadStatistics empty1 = WasmThreadStatistics.empty();
      final WasmThreadStatistics empty2 = WasmThreadStatistics.empty();

      assertEquals(empty1, empty2, "Multiple empty() calls should produce equal objects");
    }
  }

  @Nested
  @DisplayName("Combine Tests")
  class CombineTests {

    @Test
    @DisplayName("combine should sum all fields except peakMemoryUsage")
    void combineShouldSumAllFieldsExceptPeakMemory() {
      final WasmThreadStatistics stats1 = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(20L, 2000L, 10L, 200L, 3L, 256L);

      final WasmThreadStatistics combined = stats1.combine(stats2);

      assertEquals(30L, combined.getFunctionsExecuted(),
          "Combined functions executed should be 30");
      assertEquals(3000L, combined.getTotalExecutionTime(),
          "Combined total execution time should be 3000");
      assertEquals(15L, combined.getAtomicOperations(),
          "Combined atomic operations should be 15");
      assertEquals(300L, combined.getMemoryAccesses(),
          "Combined memory accesses should be 300");
      assertEquals(5L, combined.getWaitNotifyOperations(),
          "Combined wait/notify operations should be 5");
    }

    @Test
    @DisplayName("combine should use max of peakMemoryUsage")
    void combineShouldUseMaxOfPeakMemoryUsage() {
      final WasmThreadStatistics stats1 = new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, 512L);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, 1024L);

      final WasmThreadStatistics combined = stats1.combine(stats2);

      assertEquals(1024L, combined.getPeakMemoryUsage(),
          "Combined peak memory usage should be the max (1024)");
    }

    @Test
    @DisplayName("combine should use max of peakMemoryUsage when first is larger")
    void combineShouldUseMaxOfPeakMemoryUsageWhenFirstIsLarger() {
      final WasmThreadStatistics stats1 = new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, 2048L);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(0L, 0L, 0L, 0L, 0L, 512L);

      final WasmThreadStatistics combined = stats1.combine(stats2);

      assertEquals(2048L, combined.getPeakMemoryUsage(),
          "Combined peak memory usage should be the max (2048)");
    }

    @Test
    @DisplayName("combine with empty should return equivalent statistics")
    void combineWithEmptyShouldReturnEquivalentStatistics() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);
      final WasmThreadStatistics empty = WasmThreadStatistics.empty();

      final WasmThreadStatistics combined = stats.combine(empty);

      assertEquals(stats, combined,
          "Combining with empty should produce equivalent statistics");
    }

    @Test
    @DisplayName("combine should throw IllegalArgumentException for null")
    void combineShouldThrowIaeForNull() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);

      final IllegalArgumentException exception = assertThrows(
          IllegalArgumentException.class,
          () -> stats.combine(null),
          "combine(null) should throw IllegalArgumentException");
      assertTrue(exception.getMessage().contains("null"),
          "Exception message should mention null");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final WasmThreadStatistics stats1 = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);

      assertEquals(stats1, stats2, "Statistics with identical values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different values")
    void equalsShouldReturnFalseForDifferentValues() {
      final WasmThreadStatistics stats1 = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(20L, 2000L, 10L, 200L, 4L, 1024L);

      assertNotEquals(stats1, stats2, "Statistics with different values should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same reference")
    void equalsShouldReturnTrueForSameReference() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);

      assertEquals(stats, stats, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final WasmThreadStatistics stats = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);

      assertNotEquals(null, stats, "Statistics should not be equal to null");
    }

    @Test
    @DisplayName("hashCode should be consistent for equal objects")
    void hashCodeShouldBeConsistentForEqualObjects() {
      final WasmThreadStatistics stats1 = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);
      final WasmThreadStatistics stats2 = new WasmThreadStatistics(10L, 1000L, 5L, 100L, 2L, 512L);

      assertEquals(stats1.hashCode(), stats2.hashCode(),
          "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain WasmThreadStatistics prefix")
    void toStringShouldContainPrefix() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(10L, 5_000_000L, 5L, 100L, 2L, 1_048_576L);

      assertTrue(stats.toString().startsWith("WasmThreadStatistics{"),
          "toString should start with 'WasmThreadStatistics{'");
    }

    @Test
    @DisplayName("toString should contain key metric values")
    void toStringShouldContainKeyMetricValues() {
      final WasmThreadStatistics stats =
          new WasmThreadStatistics(42L, 5_000_000L, 5L, 100L, 2L, 1_048_576L);

      final String str = stats.toString();

      assertTrue(str.contains("42"), "toString should contain functionsExecuted value");
      assertTrue(str.contains("5000000"), "toString should contain totalExecutionTime value");
    }
  }
}
