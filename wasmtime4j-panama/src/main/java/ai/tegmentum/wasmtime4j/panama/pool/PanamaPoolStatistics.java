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

import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import java.time.Duration;

/**
 * Panama implementation of {@link PoolStatistics}.
 *
 * <p>This class provides statistics for monitoring pooling allocator usage and performance.
 *
 * @since 1.0.0
 */
public final class PanamaPoolStatistics implements PoolStatistics {

  private final long instancesAllocated;
  private final long instancesReused;
  private final long instancesCreated;
  private final long memoryPoolsAllocated;
  private final long memoryPoolsReused;
  private final long stackPoolsAllocated;
  private final long stackPoolsReused;
  private final long tablePoolsAllocated;
  private final long tablePoolsReused;
  private final long peakMemoryUsage;
  private final long currentMemoryUsage;
  private final long allocationFailures;
  private final Duration poolWarmingTime;
  private final Duration averageAllocationTime;

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
    this.instancesAllocated = instancesAllocated;
    this.instancesReused = instancesReused;
    this.instancesCreated = instancesCreated;
    this.memoryPoolsAllocated = memoryPoolsAllocated;
    this.memoryPoolsReused = memoryPoolsReused;
    this.stackPoolsAllocated = stackPoolsAllocated;
    this.stackPoolsReused = stackPoolsReused;
    this.tablePoolsAllocated = tablePoolsAllocated;
    this.tablePoolsReused = tablePoolsReused;
    this.peakMemoryUsage = peakMemoryUsage;
    this.currentMemoryUsage = currentMemoryUsage;
    this.allocationFailures = allocationFailures;
    this.poolWarmingTime = Duration.ofNanos(poolWarmingTimeNanos);
    this.averageAllocationTime = Duration.ofNanos(averageAllocationTimeNanos);
  }

  /** Creates empty statistics with all values set to zero. */
  public PanamaPoolStatistics() {
    this(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
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
    return instancesCreated;
  }

  @Override
  public long getMemoryPoolsAllocated() {
    return memoryPoolsAllocated;
  }

  @Override
  public long getMemoryPoolsReused() {
    return memoryPoolsReused;
  }

  @Override
  public long getStackPoolsAllocated() {
    return stackPoolsAllocated;
  }

  @Override
  public long getStackPoolsReused() {
    return stackPoolsReused;
  }

  @Override
  public long getTablePoolsAllocated() {
    return tablePoolsAllocated;
  }

  @Override
  public long getTablePoolsReused() {
    return tablePoolsReused;
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
    return allocationFailures;
  }

  @Override
  public Duration getPoolWarmingTime() {
    return poolWarmingTime;
  }

  @Override
  public Duration getAverageAllocationTime() {
    return averageAllocationTime;
  }

  @Override
  public String toString() {
    return "PanamaPoolStatistics{"
        + "instancesAllocated="
        + instancesAllocated
        + ", instancesReused="
        + instancesReused
        + ", instancesCreated="
        + instancesCreated
        + ", memoryPoolsAllocated="
        + memoryPoolsAllocated
        + ", memoryPoolsReused="
        + memoryPoolsReused
        + ", stackPoolsAllocated="
        + stackPoolsAllocated
        + ", stackPoolsReused="
        + stackPoolsReused
        + ", tablePoolsAllocated="
        + tablePoolsAllocated
        + ", tablePoolsReused="
        + tablePoolsReused
        + ", peakMemoryUsage="
        + peakMemoryUsage
        + ", currentMemoryUsage="
        + currentMemoryUsage
        + ", allocationFailures="
        + allocationFailures
        + ", poolWarmingTime="
        + poolWarmingTime
        + ", averageAllocationTime="
        + averageAllocationTime
        + ", reuseRatio="
        + getReuseRatio()
        + ", memoryUtilization="
        + getMemoryUtilization()
        + '}';
  }
}
