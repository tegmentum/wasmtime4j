/*
 * Copyright 2024 Tegmentum AI
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

/**
 * Statistics for monitoring pooling allocator usage and performance.
 *
 * <p>PoolStatistics provides real-time metrics about pool utilization, including allocation counts,
 * reuse rates, memory usage, and timing information. These statistics are useful for monitoring
 * pool health and tuning configuration parameters.
 *
 * @since 1.0.0
 */
public interface PoolStatistics {

  /**
   * Gets the total number of instances allocated from the pool.
   *
   * @return the total instances allocated
   */
  long getInstancesAllocated();

  /**
   * Gets the number of instances that were reused from the pool.
   *
   * @return the instances reused count
   */
  long getInstancesReused();

  /**
   * Gets the total number of new instances created.
   *
   * @return the instances created count
   */
  long getInstancesCreated();

  /**
   * Gets the number of memory pools allocated.
   *
   * @return the memory pools allocated count
   */
  long getMemoryPoolsAllocated();

  /**
   * Gets the number of memory pools reused.
   *
   * @return the memory pools reused count
   */
  long getMemoryPoolsReused();

  /**
   * Gets the number of stack pools allocated.
   *
   * @return the stack pools allocated count
   */
  long getStackPoolsAllocated();

  /**
   * Gets the number of stack pools reused.
   *
   * @return the stack pools reused count
   */
  long getStackPoolsReused();

  /**
   * Gets the number of table pools allocated.
   *
   * @return the table pools allocated count
   */
  long getTablePoolsAllocated();

  /**
   * Gets the number of table pools reused.
   *
   * @return the table pools reused count
   */
  long getTablePoolsReused();

  /**
   * Gets the peak memory usage in bytes.
   *
   * @return the peak memory usage
   */
  long getPeakMemoryUsage();

  /**
   * Gets the current memory usage in bytes.
   *
   * @return the current memory usage
   */
  long getCurrentMemoryUsage();

  /**
   * Gets the number of allocation failures.
   *
   * @return the allocation failures count
   */
  long getAllocationFailures();

  /**
   * Gets the time spent warming the pools during initialization.
   *
   * @return the pool warming duration
   */
  Duration getPoolWarmingTime();

  /**
   * Gets the average time for an allocation operation.
   *
   * @return the average allocation duration
   */
  Duration getAverageAllocationTime();

  /**
   * Calculates the reuse ratio (reused / total allocations).
   *
   * @return the reuse ratio between 0.0 and 1.0
   */
  default double getReuseRatio() {
    final long total = getInstancesAllocated() + getInstancesReused();
    if (total == 0) {
      return 0.0;
    }
    return (double) getInstancesReused() / total;
  }

  /**
   * Calculates the memory utilization ratio (current / peak).
   *
   * @return the memory utilization ratio between 0.0 and 1.0
   */
  default double getMemoryUtilization() {
    final long peak = getPeakMemoryUsage();
    if (peak == 0) {
      return 0.0;
    }
    return (double) getCurrentMemoryUsage() / peak;
  }
}
