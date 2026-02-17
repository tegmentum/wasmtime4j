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

package ai.tegmentum.wasmtime4j.jni.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link JniPoolStatistics}. */
@DisplayName("JniPoolStatistics Tests")
class JniPoolStatisticsTest {

  @Nested
  @DisplayName("Default Constructor Tests")
  class DefaultConstructorTests {

    @Test
    @DisplayName("Default constructor should create statistics with zero values")
    void defaultConstructorShouldCreateStatisticsWithZeroValues() {
      final JniPoolStatistics stats = new JniPoolStatistics();

      assertEquals(0, stats.getInstancesAllocated(), "instancesAllocated should be 0");
      assertEquals(0, stats.getInstancesReused(), "instancesReused should be 0");
      assertEquals(0, stats.getInstancesCreated(), "instancesCreated should be 0");
      assertEquals(0, stats.getMemoryPoolsAllocated(), "memoryPoolsAllocated should be 0");
      assertEquals(0, stats.getMemoryPoolsReused(), "memoryPoolsReused should be 0");
      assertEquals(0, stats.getStackPoolsAllocated(), "stackPoolsAllocated should be 0");
      assertEquals(0, stats.getStackPoolsReused(), "stackPoolsReused should be 0");
      assertEquals(0, stats.getTablePoolsAllocated(), "tablePoolsAllocated should be 0");
      assertEquals(0, stats.getTablePoolsReused(), "tablePoolsReused should be 0");
      assertEquals(0, stats.getPeakMemoryUsage(), "peakMemoryUsage should be 0");
      assertEquals(0, stats.getCurrentMemoryUsage(), "currentMemoryUsage should be 0");
      assertEquals(0, stats.getAllocationFailures(), "allocationFailures should be 0");
    }

    @Test
    @DisplayName("Default constructor should create zero durations")
    void defaultConstructorShouldCreateZeroDurations() {
      final JniPoolStatistics stats = new JniPoolStatistics();

      assertEquals(Duration.ZERO, stats.getPoolWarmingTime(), "poolWarmingTime should be zero");
      assertEquals(
          Duration.ZERO, stats.getAverageAllocationTime(), "averageAllocationTime should be zero");
    }
  }

  @Nested
  @DisplayName("Full Constructor Tests")
  class FullConstructorTests {

    @Test
    @DisplayName("Full constructor should set all values correctly")
    void fullConstructorShouldSetAllValuesCorrectly() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(
              100L, // instancesAllocated
              80L, // instancesReused
              20L, // instancesCreated
              50L, // memoryPoolsAllocated
              40L, // memoryPoolsReused
              30L, // stackPoolsAllocated
              25L, // stackPoolsReused
              15L, // tablePoolsAllocated
              10L, // tablePoolsReused
              1024L * 1024 * 100, // peakMemoryUsage (100MB)
              1024L * 1024 * 50, // currentMemoryUsage (50MB)
              5L, // allocationFailures
              1_000_000_000L, // poolWarmingTimeNanos (1 second)
              100_000L // averageAllocationTimeNanos (100 microseconds)
              );

      assertEquals(100L, stats.getInstancesAllocated(), "instancesAllocated should match");
      assertEquals(80L, stats.getInstancesReused(), "instancesReused should match");
      assertEquals(20L, stats.getInstancesCreated(), "instancesCreated should match");
      assertEquals(50L, stats.getMemoryPoolsAllocated(), "memoryPoolsAllocated should match");
      assertEquals(40L, stats.getMemoryPoolsReused(), "memoryPoolsReused should match");
      assertEquals(30L, stats.getStackPoolsAllocated(), "stackPoolsAllocated should match");
      assertEquals(25L, stats.getStackPoolsReused(), "stackPoolsReused should match");
      assertEquals(15L, stats.getTablePoolsAllocated(), "tablePoolsAllocated should match");
      assertEquals(10L, stats.getTablePoolsReused(), "tablePoolsReused should match");
      assertEquals(1024L * 1024 * 100, stats.getPeakMemoryUsage(), "peakMemoryUsage should match");
      assertEquals(
          1024L * 1024 * 50, stats.getCurrentMemoryUsage(), "currentMemoryUsage should match");
      assertEquals(5L, stats.getAllocationFailures(), "allocationFailures should match");
    }

    @Test
    @DisplayName("Full constructor should convert nanoseconds to Duration")
    void fullConstructorShouldConvertNanosecondsToDuration() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              2_500_000_000L, // 2.5 seconds
              500_000L // 0.5 milliseconds
              );

      assertEquals(
          Duration.ofNanos(2_500_000_000L),
          stats.getPoolWarmingTime(),
          "poolWarmingTime should be 2.5 seconds");
      assertEquals(
          Duration.ofNanos(500_000L),
          stats.getAverageAllocationTime(),
          "averageAllocationTime should be 500 microseconds");
    }
  }

  @Nested
  @DisplayName("Reuse Ratio Tests")
  class ReuseRatioTests {

    @Test
    @DisplayName("getReuseRatio should return correct ratio")
    void getReuseRatioShouldReturnCorrectRatio() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(100L, 80L, 20L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

      // Reuse ratio = instancesReused / (instancesAllocated + instancesReused) = 80/180 ≈ 0.444
      final double expectedRatio = 80.0 / (100.0 + 80.0);
      assertEquals(expectedRatio, stats.getReuseRatio(), 0.001, "Reuse ratio should match formula");
    }

    @Test
    @DisplayName("getReuseRatio should return 0.0 when no allocations")
    void getReuseRatioShouldReturnZeroWhenNoAllocations() {
      final JniPoolStatistics stats = new JniPoolStatistics();

      // When total (instancesAllocated + instancesReused) is 0, ratio should be 0.0
      assertEquals(
          0.0, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.0 when no allocations");
    }

    @Test
    @DisplayName("getReuseRatio should return 0.5 when allocated equals reused")
    void getReuseRatioShouldReturnHalfWhenAllocatedEqualsReused() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(100L, 100L, 0L, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

      // Reuse ratio = instancesReused / (instancesAllocated + instancesReused) = 100/200 = 0.5
      assertEquals(
          0.5,
          stats.getReuseRatio(),
          0.001,
          "Reuse ratio should be 0.5 when allocated equals reused");
    }
  }

  @Nested
  @DisplayName("Memory Utilization Tests")
  class MemoryUtilizationTests {

    @Test
    @DisplayName("getMemoryUtilization should return correct ratio")
    void getMemoryUtilizationShouldReturnCorrectRatio() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              1024L * 1024 * 100, // peak 100MB
              1024L * 1024 * 50, // current 50MB
              0,
              0,
              0);

      // Memory utilization = current / peak = 50MB / 100MB = 0.5
      assertEquals(0.5, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.5");
    }

    @Test
    @DisplayName("getMemoryUtilization should return 0.0 when no peak memory")
    void getMemoryUtilizationShouldReturnZeroWhenNoPeakMemory() {
      final JniPoolStatistics stats = new JniPoolStatistics();

      assertEquals(
          0.0,
          stats.getMemoryUtilization(),
          0.001,
          "Memory utilization should be 0.0 when no peak memory");
    }

    @Test
    @DisplayName("getMemoryUtilization should return 1.0 when at peak")
    void getMemoryUtilizationShouldReturnOneWhenAtPeak() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              0,
              1024L * 1024 * 100, // peak 100MB
              1024L * 1024 * 100, // current 100MB
              0,
              0,
              0);

      assertEquals(
          1.0,
          stats.getMemoryUtilization(),
          0.001,
          "Memory utilization should be 1.0 when at peak");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should include all statistics")
    void toStringShouldIncludeAllStatistics() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(
              100L,
              80L,
              20L,
              50L,
              40L,
              30L,
              25L,
              15L,
              10L,
              1024L * 1024 * 100,
              1024L * 1024 * 50,
              5L,
              1_000_000_000L,
              100_000L);

      final String str = stats.toString();

      assertTrue(str.contains("instancesAllocated=100"), "Should contain instancesAllocated");
      assertTrue(str.contains("instancesReused=80"), "Should contain instancesReused");
      assertTrue(str.contains("instancesCreated=20"), "Should contain instancesCreated");
      assertTrue(str.contains("memoryPoolsAllocated=50"), "Should contain memoryPoolsAllocated");
      assertTrue(str.contains("memoryPoolsReused=40"), "Should contain memoryPoolsReused");
      assertTrue(str.contains("stackPoolsAllocated=30"), "Should contain stackPoolsAllocated");
      assertTrue(str.contains("stackPoolsReused=25"), "Should contain stackPoolsReused");
      assertTrue(str.contains("tablePoolsAllocated=15"), "Should contain tablePoolsAllocated");
      assertTrue(str.contains("tablePoolsReused=10"), "Should contain tablePoolsReused");
      assertTrue(str.contains("allocationFailures=5"), "Should contain allocationFailures");
      assertTrue(str.contains("reuseRatio="), "Should contain reuseRatio");
      assertTrue(str.contains("memoryUtilization="), "Should contain memoryUtilization");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("Should handle maximum long values")
    void shouldHandleMaximumLongValues() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE,
              Long.MAX_VALUE);

      assertEquals(
          Long.MAX_VALUE,
          stats.getInstancesAllocated(),
          "Should handle max long for instancesAllocated");
      assertNotNull(stats.getPoolWarmingTime(), "poolWarmingTime should not be null");
    }

    @Test
    @DisplayName("Should handle zero values in all fields")
    void shouldHandleZeroValuesInAllFields() {
      final JniPoolStatistics stats =
          new JniPoolStatistics(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

      assertEquals(0L, stats.getInstancesAllocated(), "Should handle zero");
      assertEquals(Duration.ZERO, stats.getPoolWarmingTime(), "Should handle zero duration");
      assertEquals(0.0, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.0");
      assertEquals(0.0, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.0");
    }

    @Test
    @DisplayName("Should handle negative nanosecond values")
    void shouldHandleNegativeNanosecondValues() {
      // Negative values would result in negative Duration, which is valid
      final JniPoolStatistics stats =
          new JniPoolStatistics(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, -1000L, -1000L);

      assertNotNull(stats.getPoolWarmingTime(), "Should handle negative nanoseconds");
      assertTrue(stats.getPoolWarmingTime().isNegative(), "Duration should be negative");
    }
  }
}
