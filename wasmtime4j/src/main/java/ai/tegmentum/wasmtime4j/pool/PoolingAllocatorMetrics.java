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

import java.time.Duration;
import java.time.Instant;

/**
 * Runtime metrics for the pooling allocator.
 *
 * <p>PoolingAllocatorMetrics provides real-time statistics about the pooling allocator's usage,
 * performance, and health. These metrics are useful for:
 *
 * <ul>
 *   <li>Monitoring pool utilization
 *   <li>Detecting resource exhaustion
 *   <li>Performance tuning
 *   <li>Capacity planning
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PoolingAllocator allocator = PoolingAllocator.create(config);
 * PoolingAllocatorMetrics metrics = allocator.getMetrics();
 *
 * System.out.println("Active instances: " + metrics.getActiveInstanceCount());
 * System.out.println("Pool utilization: " + metrics.getInstanceUtilization() * 100 + "%");
 * System.out.println("Allocation failures: " + metrics.getAllocationFailures());
 * }</pre>
 *
 * @since 1.1.0
 */
public interface PoolingAllocatorMetrics {

  // ===== Instance Pool Metrics =====

  /**
   * Gets the number of currently active (in-use) instances.
   *
   * @return the active instance count
   */
  long getActiveInstanceCount();

  /**
   * Gets the total number of instances in the pool (both active and available).
   *
   * @return the total instance count
   */
  long getTotalInstanceCount();

  /**
   * Gets the number of available (free) instances in the pool.
   *
   * @return the available instance count
   */
  default long getAvailableInstanceCount() {
    return getTotalInstanceCount() - getActiveInstanceCount();
  }

  /**
   * Gets the peak number of concurrently active instances.
   *
   * @return the peak instance count
   */
  long getPeakInstanceCount();

  /**
   * Gets the instance pool utilization as a ratio (0.0 to 1.0).
   *
   * @return the utilization ratio
   */
  default double getInstanceUtilization() {
    long total = getTotalInstanceCount();
    return total > 0 ? (double) getActiveInstanceCount() / total : 0.0;
  }

  // ===== Memory Metrics =====

  /**
   * Gets the total memory reserved by the pool in bytes.
   *
   * @return the total reserved memory
   */
  long getTotalReservedMemory();

  /**
   * Gets the memory currently in use by active instances.
   *
   * @return the active memory usage in bytes
   */
  long getActiveMemoryUsage();

  /**
   * Gets the peak memory usage.
   *
   * @return the peak memory usage in bytes
   */
  long getPeakMemoryUsage();

  /**
   * Gets the memory utilization as a ratio (0.0 to 1.0).
   *
   * @return the memory utilization ratio
   */
  default double getMemoryUtilization() {
    long reserved = getTotalReservedMemory();
    return reserved > 0 ? (double) getActiveMemoryUsage() / reserved : 0.0;
  }

  // ===== Stack Metrics =====

  /**
   * Gets the number of currently active stacks.
   *
   * @return the active stack count
   */
  long getActiveStackCount();

  /**
   * Gets the total number of stacks in the pool.
   *
   * @return the total stack count
   */
  long getTotalStackCount();

  /**
   * Gets the stack pool utilization as a ratio (0.0 to 1.0).
   *
   * @return the stack utilization ratio
   */
  default double getStackUtilization() {
    long total = getTotalStackCount();
    return total > 0 ? (double) getActiveStackCount() / total : 0.0;
  }

  // ===== Table Metrics =====

  /**
   * Gets the number of currently active tables.
   *
   * @return the active table count
   */
  long getActiveTableCount();

  /**
   * Gets the total number of tables in the pool.
   *
   * @return the total table count
   */
  long getTotalTableCount();

  // ===== GC Heap Metrics =====

  /**
   * Gets the number of currently active GC heaps.
   *
   * @return the active GC heap count
   */
  long getActiveGcHeapCount();

  /**
   * Gets the total number of GC heaps in the pool.
   *
   * @return the total GC heap count
   */
  long getTotalGcHeapCount();

  // ===== Allocation Statistics =====

  /**
   * Gets the total number of successful allocations.
   *
   * @return the allocation count
   */
  long getAllocationCount();

  /**
   * Gets the total number of deallocations.
   *
   * @return the deallocation count
   */
  long getDeallocationCount();

  /**
   * Gets the number of allocation failures due to pool exhaustion.
   *
   * @return the failure count
   */
  long getAllocationFailures();

  /**
   * Gets the average allocation latency.
   *
   * @return the average allocation duration
   */
  Duration getAverageAllocationLatency();

  /**
   * Gets the maximum allocation latency observed.
   *
   * @return the maximum allocation duration
   */
  Duration getMaxAllocationLatency();

  // ===== Component Instance Metrics =====

  /**
   * Gets the number of active component instances.
   *
   * @return the active component instance count
   */
  long getActiveComponentInstanceCount();

  /**
   * Gets the total component instances in the pool.
   *
   * @return the total component instance count
   */
  long getTotalComponentInstanceCount();

  // ===== Time-based Metrics =====

  /**
   * Gets the time when the pool was created.
   *
   * @return the creation timestamp
   */
  Instant getCreationTime();

  /**
   * Gets the duration since the pool was created.
   *
   * @return the uptime duration
   */
  default Duration getUptime() {
    return Duration.between(getCreationTime(), Instant.now());
  }

  /**
   * Gets the time of the last allocation.
   *
   * @return the last allocation timestamp, or null if no allocations
   */
  Instant getLastAllocationTime();

  /**
   * Gets the time of the last deallocation.
   *
   * @return the last deallocation timestamp, or null if no deallocations
   */
  Instant getLastDeallocationTime();

  // ===== Snapshot =====

  /**
   * Creates a point-in-time snapshot of all metrics.
   *
   * <p>This is useful for consistent reporting when multiple metrics need to be captured together.
   *
   * @return a snapshot of current metrics
   */
  MetricsSnapshot snapshot();

  /**
   * Resets the statistical counters (allocation count, failures, peak values).
   *
   * <p>This does not affect the actual pool state, only the statistics.
   */
  void resetStatistics();

  /** An immutable snapshot of metrics at a point in time. */
  interface MetricsSnapshot {
    /** Gets the snapshot timestamp. */
    Instant getTimestamp();

    /** Gets the active instance count at snapshot time. */
    long getActiveInstanceCount();

    /** Gets the total instance count at snapshot time. */
    long getTotalInstanceCount();

    /** Gets the active memory usage at snapshot time. */
    long getActiveMemoryUsage();

    /** Gets the total reserved memory at snapshot time. */
    long getTotalReservedMemory();

    /** Gets the allocation count at snapshot time. */
    long getAllocationCount();

    /** Gets the deallocation count at snapshot time. */
    long getDeallocationCount();

    /** Gets the allocation failures at snapshot time. */
    long getAllocationFailures();
  }
}
