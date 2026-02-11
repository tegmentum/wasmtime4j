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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link InstanceStatistics} class.
 *
 * <p>InstanceStatistics provides runtime statistics about WebAssembly instance execution including
 * function call counts, execution time, memory usage, and fuel consumption.
 */
@DisplayName("InstanceStatistics Tests")
class InstanceStatisticsTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(InstanceStatistics.class.getModifiers()),
          "InstanceStatistics should be public");
      assertTrue(
          Modifier.isFinal(InstanceStatistics.class.getModifiers()),
          "InstanceStatistics should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create instance with all parameters")
    void shouldCreateInstanceWithAllParameters() {
      final InstanceStatistics stats =
          new InstanceStatistics(100L, 5000L, 1024L, 2048L, 10, 5, 50L, 3L);

      assertNotNull(stats, "InstanceStatistics should be created");
    }

    @Test
    @DisplayName("should accept zero values for all parameters")
    void shouldAcceptZeroValuesForAllParameters() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(0L, stats.getFunctionCallCount(), "Function call count should be 0");
      assertEquals(0L, stats.getTotalExecutionTime(), "Total execution time should be 0");
      assertEquals(0L, stats.getMemoryBytesAllocated(), "Memory bytes allocated should be 0");
      assertEquals(0L, stats.getPeakMemoryUsage(), "Peak memory usage should be 0");
      assertEquals(0, stats.getActiveTableElements(), "Active table elements should be 0");
      assertEquals(0, stats.getActiveGlobals(), "Active globals should be 0");
      assertEquals(0L, stats.getFuelConsumed(), "Fuel consumed should be 0");
      assertEquals(0L, stats.getEpochTicks(), "Epoch ticks should be 0");
    }

    @Test
    @DisplayName("should accept large values for long parameters")
    void shouldAcceptLargeValuesForLongParameters() {
      final long largeValue = Long.MAX_VALUE;
      final InstanceStatistics stats =
          new InstanceStatistics(
              largeValue,
              largeValue,
              largeValue,
              largeValue,
              Integer.MAX_VALUE,
              Integer.MAX_VALUE,
              largeValue,
              largeValue);

      assertEquals(
          largeValue,
          stats.getFunctionCallCount(),
          "Function call count should handle Long.MAX_VALUE");
      assertEquals(
          largeValue,
          stats.getTotalExecutionTime(),
          "Total execution time should handle Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getFunctionCallCount should return constructor value")
    void getFunctionCallCountShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(42L, 0L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(42L, stats.getFunctionCallCount(), "Function call count should be 42");
    }

    @Test
    @DisplayName("getTotalExecutionTime should return constructor value in nanoseconds")
    void getTotalExecutionTimeShouldReturnConstructorValue() {
      final InstanceStatistics stats =
          new InstanceStatistics(0L, 999_999_999L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(
          999_999_999L,
          stats.getTotalExecutionTime(),
          "Total execution time should be 999999999 ns");
    }

    @Test
    @DisplayName("getMemoryBytesAllocated should return constructor value")
    void getMemoryBytesAllocatedShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 65536L, 0L, 0, 0, 0L, 0L);

      assertEquals(
          65536L, stats.getMemoryBytesAllocated(), "Memory bytes allocated should be 65536");
    }

    @Test
    @DisplayName("getPeakMemoryUsage should return constructor value")
    void getPeakMemoryUsageShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 131072L, 0, 0, 0L, 0L);

      assertEquals(131072L, stats.getPeakMemoryUsage(), "Peak memory usage should be 131072");
    }

    @Test
    @DisplayName("getActiveTableElements should return constructor value")
    void getActiveTableElementsShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 25, 0, 0L, 0L);

      assertEquals(25, stats.getActiveTableElements(), "Active table elements should be 25");
    }

    @Test
    @DisplayName("getActiveGlobals should return constructor value")
    void getActiveGlobalsShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 0, 7, 0L, 0L);

      assertEquals(7, stats.getActiveGlobals(), "Active globals should be 7");
    }

    @Test
    @DisplayName("getFuelConsumed should return constructor value")
    void getFuelConsumedShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 0, 0, 1500L, 0L);

      assertEquals(1500L, stats.getFuelConsumed(), "Fuel consumed should be 1500");
    }

    @Test
    @DisplayName("getEpochTicks should return constructor value")
    void getEpochTicksShouldReturnConstructorValue() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 0, 0, 0L, 99L);

      assertEquals(99L, stats.getEpochTicks(), "Epoch ticks should be 99");
    }
  }

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("getAverageExecutionTimePerCall should compute correct average")
    void getAverageExecutionTimePerCallShouldComputeCorrectAverage() {
      final InstanceStatistics stats = new InstanceStatistics(10L, 1000L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(
          100.0,
          stats.getAverageExecutionTimePerCall(),
          0.001,
          "Average execution time per call should be 100.0 ns");
    }

    @Test
    @DisplayName("getAverageExecutionTimePerCall should return zero when no calls made")
    void getAverageExecutionTimePerCallShouldReturnZeroWhenNoCalls() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 5000L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(
          0.0,
          stats.getAverageExecutionTimePerCall(),
          0.001,
          "Average execution time should be 0.0 when no calls made");
    }

    @Test
    @DisplayName("getAverageExecutionTimePerCall should handle single call")
    void getAverageExecutionTimePerCallShouldHandleSingleCall() {
      final InstanceStatistics stats = new InstanceStatistics(1L, 500L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(
          500.0,
          stats.getAverageExecutionTimePerCall(),
          0.001,
          "Average should equal total time when only one call");
    }

    @Test
    @DisplayName("getFuelConsumptionRate should compute correct rate")
    void getFuelConsumptionRateShouldComputeCorrectRate() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 1000L, 0L, 0L, 0, 0, 500L, 0L);

      assertEquals(
          0.5,
          stats.getFuelConsumptionRate(),
          0.001,
          "Fuel consumption rate should be 0.5 fuel/ns");
    }

    @Test
    @DisplayName("getFuelConsumptionRate should return zero when no execution time")
    void getFuelConsumptionRateShouldReturnZeroWhenNoExecutionTime() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 0, 0, 100L, 0L);

      assertEquals(
          0.0,
          stats.getFuelConsumptionRate(),
          0.001,
          "Fuel consumption rate should be 0.0 when no execution time");
    }

    @Test
    @DisplayName("getFuelConsumptionRate should handle zero fuel with nonzero time")
    void getFuelConsumptionRateShouldHandleZeroFuelWithNonzeroTime() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 1000L, 0L, 0L, 0, 0, 0L, 0L);

      assertEquals(
          0.0,
          stats.getFuelConsumptionRate(),
          0.001,
          "Fuel consumption rate should be 0.0 when no fuel consumed");
    }
  }

  @Nested
  @DisplayName("Equals and HashCode Tests")
  class EqualsAndHashCodeTests {

    @Test
    @DisplayName("equals should return true for identical values")
    void equalsShouldReturnTrueForIdenticalValues() {
      final InstanceStatistics stats1 =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);
      final InstanceStatistics stats2 =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      assertEquals(stats1, stats2, "Statistics with identical values should be equal");
    }

    @Test
    @DisplayName("equals should return false for different function call count")
    void equalsShouldReturnFalseForDifferentFunctionCallCount() {
      final InstanceStatistics stats1 =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);
      final InstanceStatistics stats2 =
          new InstanceStatistics(11L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      assertNotEquals(
          stats1, stats2, "Statistics with different function call counts should not be equal");
    }

    @Test
    @DisplayName("equals should return true for same reference")
    void equalsShouldReturnTrueForSameReference() {
      final InstanceStatistics stats =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      assertEquals(stats, stats, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final InstanceStatistics stats =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      assertNotEquals(null, stats, "Statistics should not be equal to null");
    }

    @Test
    @DisplayName("equals should return false for different type")
    void equalsShouldReturnFalseForDifferentType() {
      final InstanceStatistics stats =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      assertNotEquals(
          "not a stats object", stats, "Statistics should not be equal to a different type");
    }

    @Test
    @DisplayName("hashCode should be equal for equal objects")
    void hashCodeShouldBeEqualForEqualObjects() {
      final InstanceStatistics stats1 =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);
      final InstanceStatistics stats2 =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      assertEquals(
          stats1.hashCode(), stats2.hashCode(), "Equal objects should have equal hash codes");
    }

    @Test
    @DisplayName("hashCode should differ for different objects")
    void hashCodeShouldDifferForDifferentObjects() {
      final InstanceStatistics stats1 =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);
      final InstanceStatistics stats2 =
          new InstanceStatistics(20L, 600L, 2048L, 4096L, 10, 6, 200L, 14L);

      assertNotEquals(
          stats1.hashCode(),
          stats2.hashCode(),
          "Different objects should likely have different hash codes");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain all field values")
    void toStringShouldContainAllFieldValues() {
      final InstanceStatistics stats =
          new InstanceStatistics(10L, 500L, 1024L, 2048L, 5, 3, 100L, 7L);

      final String str = stats.toString();

      assertTrue(str.contains("10"), "toString should contain function call count");
      assertTrue(str.contains("500"), "toString should contain total execution time");
      assertTrue(str.contains("1024"), "toString should contain memory bytes allocated");
      assertTrue(str.contains("2048"), "toString should contain peak memory usage");
      assertTrue(str.contains("5"), "toString should contain active table elements");
      assertTrue(str.contains("3"), "toString should contain active globals");
      assertTrue(str.contains("100"), "toString should contain fuel consumed");
      assertTrue(str.contains("7"), "toString should contain epoch ticks");
    }

    @Test
    @DisplayName("toString should contain InstanceStatistics prefix")
    void toStringShouldContainPrefix() {
      final InstanceStatistics stats = new InstanceStatistics(0L, 0L, 0L, 0L, 0, 0, 0L, 0L);

      assertTrue(
          stats.toString().startsWith("InstanceStatistics{"),
          "toString should start with 'InstanceStatistics{'");
    }
  }
}
