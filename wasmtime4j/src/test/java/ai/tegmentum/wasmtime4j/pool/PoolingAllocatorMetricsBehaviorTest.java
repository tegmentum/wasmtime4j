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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Behavioral tests for PoolingAllocatorMetrics default method implementations.
 *
 * <p>These tests verify the behavior of default methods in the PoolingAllocatorMetrics interface.
 */
@DisplayName("PoolingAllocatorMetrics Behavior Tests")
class PoolingAllocatorMetricsBehaviorTest {

  /** Test implementation of PoolingAllocatorMetrics for testing default methods. */
  private static class TestMetrics implements PoolingAllocatorMetrics {
    private final long activeInstanceCount;
    private final long totalInstanceCount;
    private final long peakInstanceCount;
    private final long totalReservedMemory;
    private final long activeMemoryUsage;
    private final long peakMemoryUsage;
    private final long activeStackCount;
    private final long totalStackCount;
    private final long activeTableCount;
    private final long totalTableCount;
    private final long activeGcHeapCount;
    private final long totalGcHeapCount;
    private final long allocationCount;
    private final long deallocationCount;
    private final long allocationFailures;
    private final Duration avgAllocationLatency;
    private final Duration maxAllocationLatency;
    private final long activeComponentInstanceCount;
    private final long totalComponentInstanceCount;
    private final Instant creationTime;
    private final Instant lastAllocationTime;
    private final Instant lastDeallocationTime;

    TestMetrics(
        final long activeInstanceCount,
        final long totalInstanceCount,
        final long peakInstanceCount,
        final long totalReservedMemory,
        final long activeMemoryUsage,
        final long peakMemoryUsage,
        final long activeStackCount,
        final long totalStackCount) {
      this.activeInstanceCount = activeInstanceCount;
      this.totalInstanceCount = totalInstanceCount;
      this.peakInstanceCount = peakInstanceCount;
      this.totalReservedMemory = totalReservedMemory;
      this.activeMemoryUsage = activeMemoryUsage;
      this.peakMemoryUsage = peakMemoryUsage;
      this.activeStackCount = activeStackCount;
      this.totalStackCount = totalStackCount;
      this.activeTableCount = 0;
      this.totalTableCount = 0;
      this.activeGcHeapCount = 0;
      this.totalGcHeapCount = 0;
      this.allocationCount = 0;
      this.deallocationCount = 0;
      this.allocationFailures = 0;
      this.avgAllocationLatency = Duration.ZERO;
      this.maxAllocationLatency = Duration.ZERO;
      this.activeComponentInstanceCount = 0;
      this.totalComponentInstanceCount = 0;
      this.creationTime = Instant.now();
      this.lastAllocationTime = null;
      this.lastDeallocationTime = null;
    }

    @Override
    public long getActiveInstanceCount() {
      return activeInstanceCount;
    }

    @Override
    public long getTotalInstanceCount() {
      return totalInstanceCount;
    }

    @Override
    public long getPeakInstanceCount() {
      return peakInstanceCount;
    }

    @Override
    public long getTotalReservedMemory() {
      return totalReservedMemory;
    }

    @Override
    public long getActiveMemoryUsage() {
      return activeMemoryUsage;
    }

    @Override
    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }

    @Override
    public long getActiveStackCount() {
      return activeStackCount;
    }

    @Override
    public long getTotalStackCount() {
      return totalStackCount;
    }

    @Override
    public long getActiveTableCount() {
      return activeTableCount;
    }

    @Override
    public long getTotalTableCount() {
      return totalTableCount;
    }

    @Override
    public long getActiveGcHeapCount() {
      return activeGcHeapCount;
    }

    @Override
    public long getTotalGcHeapCount() {
      return totalGcHeapCount;
    }

    @Override
    public long getAllocationCount() {
      return allocationCount;
    }

    @Override
    public long getDeallocationCount() {
      return deallocationCount;
    }

    @Override
    public long getAllocationFailures() {
      return allocationFailures;
    }

    @Override
    public Duration getAverageAllocationLatency() {
      return avgAllocationLatency;
    }

    @Override
    public Duration getMaxAllocationLatency() {
      return maxAllocationLatency;
    }

    @Override
    public long getActiveComponentInstanceCount() {
      return activeComponentInstanceCount;
    }

    @Override
    public long getTotalComponentInstanceCount() {
      return totalComponentInstanceCount;
    }

    @Override
    public Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public Instant getLastAllocationTime() {
      return lastAllocationTime;
    }

    @Override
    public Instant getLastDeallocationTime() {
      return lastDeallocationTime;
    }

    @Override
    public MetricsSnapshot snapshot() {
      final Instant now = Instant.now();
      final long active = activeInstanceCount;
      final long total = totalInstanceCount;
      final long memory = activeMemoryUsage;
      final long reserved = totalReservedMemory;
      final long allocs = allocationCount;
      final long deallocs = deallocationCount;
      final long failures = allocationFailures;

      return new MetricsSnapshot() {
        @Override
        public Instant getTimestamp() {
          return now;
        }

        @Override
        public long getActiveInstanceCount() {
          return active;
        }

        @Override
        public long getTotalInstanceCount() {
          return total;
        }

        @Override
        public long getActiveMemoryUsage() {
          return memory;
        }

        @Override
        public long getTotalReservedMemory() {
          return reserved;
        }

        @Override
        public long getAllocationCount() {
          return allocs;
        }

        @Override
        public long getDeallocationCount() {
          return deallocs;
        }

        @Override
        public long getAllocationFailures() {
          return failures;
        }
      };
    }

    @Override
    public void resetStatistics() {
      // No-op for test implementation
    }
  }

  // ========================================================================
  // getAvailableInstanceCount Tests
  // ========================================================================

  @Nested
  @DisplayName("getAvailableInstanceCount Tests")
  class GetAvailableInstanceCountTests {

    @Test
    @DisplayName("should calculate available instances correctly")
    void shouldCalculateAvailableInstancesCorrectly() {
      final TestMetrics metrics = new TestMetrics(3, 10, 5, 0, 0, 0, 0, 0);
      assertEquals(7, metrics.getAvailableInstanceCount(), "Available should be total - active");
    }

    @Test
    @DisplayName("should return zero when all instances are active")
    void shouldReturnZeroWhenAllInstancesAreActive() {
      final TestMetrics metrics = new TestMetrics(10, 10, 10, 0, 0, 0, 0, 0);
      assertEquals(0, metrics.getAvailableInstanceCount(), "Available should be 0");
    }

    @Test
    @DisplayName("should return total when no instances are active")
    void shouldReturnTotalWhenNoInstancesAreActive() {
      final TestMetrics metrics = new TestMetrics(0, 10, 0, 0, 0, 0, 0, 0);
      assertEquals(10, metrics.getAvailableInstanceCount(), "Available should equal total");
    }

    @Test
    @DisplayName("should handle zero total instances")
    void shouldHandleZeroTotalInstances() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      assertEquals(0, metrics.getAvailableInstanceCount(), "Available should be 0");
    }

    @Test
    @DisplayName("should handle large numbers")
    void shouldHandleLargeNumbers() {
      final TestMetrics metrics = new TestMetrics(1000000L, 5000000L, 0, 0, 0, 0, 0, 0);
      assertEquals(4000000L, metrics.getAvailableInstanceCount(), "Should handle large numbers");
    }
  }

  // ========================================================================
  // getInstanceUtilization Tests
  // ========================================================================

  @Nested
  @DisplayName("getInstanceUtilization Tests")
  class GetInstanceUtilizationTests {

    @Test
    @DisplayName("should return 0.0 when no total instances")
    void shouldReturnZeroWhenNoTotalInstances() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      assertEquals(0.0, metrics.getInstanceUtilization(), 0.001, "Utilization should be 0.0");
    }

    @Test
    @DisplayName("should return 1.0 when fully utilized")
    void shouldReturnOneWhenFullyUtilized() {
      final TestMetrics metrics = new TestMetrics(10, 10, 10, 0, 0, 0, 0, 0);
      assertEquals(1.0, metrics.getInstanceUtilization(), 0.001, "Utilization should be 1.0");
    }

    @Test
    @DisplayName("should return 0.5 when half utilized")
    void shouldReturnHalfWhenHalfUtilized() {
      final TestMetrics metrics = new TestMetrics(5, 10, 5, 0, 0, 0, 0, 0);
      assertEquals(0.5, metrics.getInstanceUtilization(), 0.001, "Utilization should be 0.5");
    }

    @Test
    @DisplayName("should calculate correct ratio for various values")
    void shouldCalculateCorrectRatioForVariousValues() {
      final TestMetrics metrics = new TestMetrics(75, 100, 75, 0, 0, 0, 0, 0);
      assertEquals(0.75, metrics.getInstanceUtilization(), 0.001, "Utilization should be 0.75");
    }
  }

  // ========================================================================
  // getMemoryUtilization Tests
  // ========================================================================

  @Nested
  @DisplayName("getMemoryUtilization Tests")
  class GetMemoryUtilizationTests {

    @Test
    @DisplayName("should return 0.0 when no reserved memory")
    void shouldReturnZeroWhenNoReservedMemory() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 100, 0, 0, 0);
      assertEquals(0.0, metrics.getMemoryUtilization(), 0.001, "Memory utilization should be 0.0");
    }

    @Test
    @DisplayName("should return 1.0 when fully utilized")
    void shouldReturnOneWhenFullyUtilized() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 1000, 1000, 0, 0, 0);
      assertEquals(1.0, metrics.getMemoryUtilization(), 0.001, "Memory utilization should be 1.0");
    }

    @Test
    @DisplayName("should return 0.5 when half utilized")
    void shouldReturnHalfWhenHalfUtilized() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 1000, 500, 0, 0, 0);
      assertEquals(0.5, metrics.getMemoryUtilization(), 0.001, "Memory utilization should be 0.5");
    }

    @Test
    @DisplayName("should handle large memory values")
    void shouldHandleLargeMemoryValues() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 10000000000L, 2500000000L, 0, 0, 0);
      assertEquals(0.25, metrics.getMemoryUtilization(), 0.001, "Should handle large values");
    }
  }

  // ========================================================================
  // getStackUtilization Tests
  // ========================================================================

  @Nested
  @DisplayName("getStackUtilization Tests")
  class GetStackUtilizationTests {

    @Test
    @DisplayName("should return 0.0 when no total stacks")
    void shouldReturnZeroWhenNoTotalStacks() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 5, 0);
      assertEquals(0.0, metrics.getStackUtilization(), 0.001, "Stack utilization should be 0.0");
    }

    @Test
    @DisplayName("should return 1.0 when fully utilized")
    void shouldReturnOneWhenFullyUtilized() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 10, 10);
      assertEquals(1.0, metrics.getStackUtilization(), 0.001, "Stack utilization should be 1.0");
    }

    @Test
    @DisplayName("should return 0.5 when half utilized")
    void shouldReturnHalfWhenHalfUtilized() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 5, 10);
      assertEquals(0.5, metrics.getStackUtilization(), 0.001, "Stack utilization should be 0.5");
    }

    @Test
    @DisplayName("should calculate correct ratio")
    void shouldCalculateCorrectRatio() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 3, 12);
      assertEquals(0.25, metrics.getStackUtilization(), 0.001, "Stack utilization should be 0.25");
    }
  }

  // ========================================================================
  // getUptime Tests
  // ========================================================================

  @Nested
  @DisplayName("getUptime Tests")
  class GetUptimeTests {

    @Test
    @DisplayName("should return non-negative uptime")
    void shouldReturnNonNegativeUptime() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      final Duration uptime = metrics.getUptime();
      assertNotNull(uptime, "Uptime should not be null");
      assertTrue(!uptime.isNegative(), "Uptime should not be negative");
    }

    @Test
    @DisplayName("should calculate uptime from creation time")
    void shouldCalculateUptimeFromCreationTime() throws InterruptedException {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      Thread.sleep(10);
      final Duration uptime = metrics.getUptime();
      assertTrue(uptime.toMillis() >= 10, "Uptime should be at least 10ms");
    }

    @Test
    @DisplayName("should increase over time")
    void shouldIncreaseOverTime() throws InterruptedException {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      final Duration uptime1 = metrics.getUptime();
      Thread.sleep(20);
      final Duration uptime2 = metrics.getUptime();
      assertTrue(uptime2.compareTo(uptime1) > 0, "Uptime should increase over time");
    }
  }

  // ========================================================================
  // MetricsSnapshot Tests
  // ========================================================================

  @Nested
  @DisplayName("MetricsSnapshot Tests")
  class MetricsSnapshotTests {

    @Test
    @DisplayName("should create snapshot with current values")
    void shouldCreateSnapshotWithCurrentValues() {
      final TestMetrics metrics = new TestMetrics(5, 10, 8, 1000, 500, 600, 3, 6);
      final PoolingAllocatorMetrics.MetricsSnapshot snapshot = metrics.snapshot();

      assertNotNull(snapshot, "Snapshot should not be null");
      assertEquals(5, snapshot.getActiveInstanceCount(), "Active instances should match");
      assertEquals(10, snapshot.getTotalInstanceCount(), "Total instances should match");
      assertEquals(500, snapshot.getActiveMemoryUsage(), "Memory usage should match");
      assertEquals(1000, snapshot.getTotalReservedMemory(), "Reserved memory should match");
    }

    @Test
    @DisplayName("should have timestamp")
    void shouldHaveTimestamp() {
      final Instant before = Instant.now();
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      final PoolingAllocatorMetrics.MetricsSnapshot snapshot = metrics.snapshot();
      final Instant after = Instant.now();

      assertNotNull(snapshot.getTimestamp(), "Timestamp should not be null");
      assertTrue(
          !snapshot.getTimestamp().isBefore(before), "Timestamp should be >= creation start");
      assertTrue(!snapshot.getTimestamp().isAfter(after), "Timestamp should be <= creation end");
    }

    @Test
    @DisplayName("should capture allocation and deallocation counts")
    void shouldCaptureAllocationAndDeallocationCounts() {
      final TestMetrics metrics = new TestMetrics(0, 0, 0, 0, 0, 0, 0, 0);
      final PoolingAllocatorMetrics.MetricsSnapshot snapshot = metrics.snapshot();

      assertEquals(0, snapshot.getAllocationCount(), "Allocation count should be 0");
      assertEquals(0, snapshot.getDeallocationCount(), "Deallocation count should be 0");
      assertEquals(0, snapshot.getAllocationFailures(), "Failures should be 0");
    }
  }
}
