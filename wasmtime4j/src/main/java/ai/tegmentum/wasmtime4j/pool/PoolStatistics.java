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

/**
 * Statistics for monitoring pooling allocator usage and performance.
 *
 * <p>PoolStatistics provides real-time metrics about pool utilization, including slot counts for
 * instances, memories, tables, stacks, and GC heaps, as well as warm/unused resource tracking.
 *
 * <p>These statistics correspond directly to Wasmtime's {@code PoolingAllocatorMetrics} API.
 *
 * @since 1.0.0
 */
public interface PoolStatistics {

  /**
   * Gets the number of core module instances currently allocated.
   *
   * @return the number of active core module instance slots
   */
  long getCoreInstances();

  /**
   * Gets the number of component instances currently allocated.
   *
   * @return the number of active component instance slots
   */
  long getComponentInstances();

  /**
   * Gets the total number of linear memory slots in use.
   *
   * @return the number of memory slots
   */
  long getMemories();

  /**
   * Gets the total number of table slots in use.
   *
   * @return the number of table slots
   */
  long getTables();

  /**
   * Gets the total number of stack slots in use.
   *
   * @return the number of stack slots
   */
  long getStacks();

  /**
   * Gets the total number of GC heap slots in use.
   *
   * @return the number of GC heap slots
   */
  long getGcHeaps();

  /**
   * Gets the number of unused warm memory slots.
   *
   * <p>These are memory slots that have been allocated and warmed (made resident) but are not
   * currently in use by any instance.
   *
   * @return the number of unused warm memory slots
   */
  long getUnusedWarmMemories();

  /**
   * Gets the total resident bytes of unused warm memory slots.
   *
   * @return the number of resident bytes across all unused warm memory slots
   */
  long getUnusedMemoryBytesResident();

  /**
   * Gets the number of unused warm table slots.
   *
   * <p>These are table slots that have been allocated and warmed but are not currently in use.
   *
   * @return the number of unused warm table slots
   */
  long getUnusedWarmTables();

  /**
   * Gets the total resident bytes of unused warm table slots.
   *
   * @return the number of resident bytes across all unused warm table slots
   */
  long getUnusedTableBytesResident();

  /**
   * Gets the number of unused warm stack slots.
   *
   * <p>These are stack slots that have been allocated and warmed but are not currently in use.
   *
   * @return the number of unused warm stack slots
   */
  long getUnusedWarmStacks();

  /**
   * Gets the total resident bytes of unused warm stack slots, if available.
   *
   * <p>This metric may not be available on all platforms. Returns -1 if not available.
   *
   * @return the number of resident bytes across all unused warm stack slots, or -1 if unavailable
   */
  long getUnusedStackBytesResident();

  /**
   * Gets the total number of instance slots (core + component).
   *
   * @return the total number of instance slots
   */
  default long getTotalInstances() {
    return getCoreInstances() + getComponentInstances();
  }
}
