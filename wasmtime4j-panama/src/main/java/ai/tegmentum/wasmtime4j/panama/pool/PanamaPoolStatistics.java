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

package ai.tegmentum.wasmtime4j.panama.pool;

import ai.tegmentum.wasmtime4j.pool.AbstractPoolStatistics;
import ai.tegmentum.wasmtime4j.pool.PoolStatistics;

/**
 * Panama implementation of {@link PoolStatistics}.
 *
 * <p>This class provides statistics for monitoring pooling allocator usage and performance.
 *
 * @since 1.0.0
 */
public final class PanamaPoolStatistics extends AbstractPoolStatistics {

  /**
   * Creates a new PanamaPoolStatistics instance.
   *
   * @param instancesAllocated total instances allocated
   * @param instancesReused instances reused from pool
   * @param instancesCreated new instances created
   * @param memoryPoolsAllocated memory pools allocated
   * @param memoryPoolsReused memory pools reused
   * @param stackPoolsAllocated stack pools allocated
   * @param stackPoolsReused stack pools reused
   * @param tablePoolsAllocated table pools allocated
   * @param tablePoolsReused table pools reused
   * @param peakMemoryUsage peak memory usage in bytes
   * @param currentMemoryUsage current memory usage in bytes
   * @param allocationFailures number of allocation failures
   * @param poolWarmingTimeNanos pool warming time in nanoseconds
   * @param averageAllocationTimeNanos average allocation time in nanoseconds
   */
  public PanamaPoolStatistics(
      final long instancesAllocated,
      final long instancesReused,
      final long instancesCreated,
      final long memoryPoolsAllocated,
      final long memoryPoolsReused,
      final long stackPoolsAllocated,
      final long stackPoolsReused,
      final long tablePoolsAllocated,
      final long tablePoolsReused,
      final long peakMemoryUsage,
      final long currentMemoryUsage,
      final long allocationFailures,
      final long poolWarmingTimeNanos,
      final long averageAllocationTimeNanos) {
    super(
        instancesAllocated,
        instancesReused,
        instancesCreated,
        memoryPoolsAllocated,
        memoryPoolsReused,
        stackPoolsAllocated,
        stackPoolsReused,
        tablePoolsAllocated,
        tablePoolsReused,
        peakMemoryUsage,
        currentMemoryUsage,
        allocationFailures,
        poolWarmingTimeNanos,
        averageAllocationTimeNanos);
  }

  /** Creates empty statistics with all values set to zero. */
  public PanamaPoolStatistics() {
    super();
  }
}
