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

package ai.tegmentum.wasmtime4j.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Test suite for the default methods in PoolStatistics interface.
 *
 * <p>Tests the getReuseRatio and getMemoryUtilization default method implementations.
 */
@DisplayName("PoolStatistics Default Methods Tests")
class PoolStatisticsDefaultMethodsTest {

  /** Test implementation of PoolStatistics for testing default methods. */
  private static class TestPoolStatistics implements PoolStatistics {
    private final long instancesAllocated;
    private final long instancesReused;
    private final long currentMemoryUsage;
    private final long peakMemoryUsage;

    TestPoolStatistics(
        final long instancesAllocated,
        final long instancesReused,
        final long currentMemoryUsage,
        final long peakMemoryUsage) {
      this.instancesAllocated = instancesAllocated;
      this.instancesReused = instancesReused;
      this.currentMemoryUsage = currentMemoryUsage;
      this.peakMemoryUsage = peakMemoryUsage;
    }

    @Override
    public long getInstancesAllocated() {
      return instancesAllocated;
    }

    @Override
    public long getInstancesReused() {
      return instancesReused;
    }

    @Override
    public long getInstancesCreated() {
      return 0;
    }

    @Override
    public long getMemoryPoolsAllocated() {
      return 0;
    }

    @Override
    public long getMemoryPoolsReused() {
      return 0;
    }

    @Override
    public long getStackPoolsAllocated() {
      return 0;
    }

    @Override
    public long getStackPoolsReused() {
      return 0;
    }

    @Override
    public long getTablePoolsAllocated() {
      return 0;
    }

    @Override
    public long getTablePoolsReused() {
      return 0;
    }

    @Override
    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    @Override
    public long getCurrentMemoryUsage() {
      return currentMemoryUsage;
    }

    @Override
    public long getAllocationFailures() {
      return 0;
    }

    @Override
    public Duration getPoolWarmingTime() {
      return Duration.ZERO;
    }

    @Override
    public Duration getAverageAllocationTime() {
      return Duration.ZERO;
    }
  }

  // ========================================================================
  // getReuseRatio Tests
  // ========================================================================

  @Nested
  @DisplayName("getReuseRatio Tests")
  class GetReuseRatioTests {

    @Test
    @DisplayName("should return 0.0 when no allocations or reuses")
    void shouldReturnZeroWhenNoAllocationsOrReuses() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 0, 0);
      assertEquals(0.0, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.0");
    }

    @Test
    @DisplayName("should return 0.0 when no reuses")
    void shouldReturnZeroWhenNoReuses() {
      PoolStatistics stats = new TestPoolStatistics(10, 0, 0, 0);
      assertEquals(0.0, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.0");
    }

    @Test
    @DisplayName("should return 1.0 when all reuses")
    void shouldReturnOneWhenAllReuses() {
      PoolStatistics stats = new TestPoolStatistics(0, 10, 0, 0);
      assertEquals(1.0, stats.getReuseRatio(), 0.001, "Reuse ratio should be 1.0");
    }

    @Test
    @DisplayName("should return 0.5 when half reused")
    void shouldReturnHalfWhenHalfReused() {
      PoolStatistics stats = new TestPoolStatistics(5, 5, 0, 0);
      assertEquals(0.5, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.5");
    }

    @Test
    @DisplayName("should calculate correct ratio for various values")
    void shouldCalculateCorrectRatioForVariousValues() {
      PoolStatistics stats = new TestPoolStatistics(30, 70, 0, 0);
      assertEquals(0.7, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.7");
    }

    @Test
    @DisplayName("should handle large numbers")
    void shouldHandleLargeNumbers() {
      PoolStatistics stats = new TestPoolStatistics(1000000, 9000000, 0, 0);
      assertEquals(0.9, stats.getReuseRatio(), 0.001, "Reuse ratio should be 0.9");
    }
  }

  // ========================================================================
  // getMemoryUtilization Tests
  // ========================================================================

  @Nested
  @DisplayName("getMemoryUtilization Tests")
  class GetMemoryUtilizationTests {

    @Test
    @DisplayName("should return 0.0 when peak is zero")
    void shouldReturnZeroWhenPeakIsZero() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 100, 0);
      assertEquals(0.0, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.0");
    }

    @Test
    @DisplayName("should return 0.0 when current is zero")
    void shouldReturnZeroWhenCurrentIsZero() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 0, 100);
      assertEquals(0.0, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.0");
    }

    @Test
    @DisplayName("should return 1.0 when current equals peak")
    void shouldReturnOneWhenCurrentEqualsPeak() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 100, 100);
      assertEquals(1.0, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 1.0");
    }

    @Test
    @DisplayName("should return 0.5 when current is half of peak")
    void shouldReturnHalfWhenCurrentIsHalfOfPeak() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 50, 100);
      assertEquals(0.5, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.5");
    }

    @Test
    @DisplayName("should calculate correct utilization for various values")
    void shouldCalculateCorrectUtilizationForVariousValues() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 75, 100);
      assertEquals(0.75, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.75");
    }

    @Test
    @DisplayName("should handle large numbers")
    void shouldHandleLargeNumbers() {
      PoolStatistics stats = new TestPoolStatistics(0, 0, 500000000L, 1000000000L);
      assertEquals(0.5, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 0.5");
    }

    @Test
    @DisplayName("should handle current greater than peak")
    void shouldHandleCurrentGreaterThanPeak() {
      // This scenario shouldn't happen in practice but the implementation should handle it
      PoolStatistics stats = new TestPoolStatistics(0, 0, 200, 100);
      assertEquals(2.0, stats.getMemoryUtilization(), 0.001, "Memory utilization should be 2.0");
    }
  }
}
