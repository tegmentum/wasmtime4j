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

import ai.tegmentum.wasmtime4j.pool.AbstractPoolingAllocatorConfig;

/**
 * Panama implementation of pooling allocator configuration.
 *
 * @since 1.0.0
 */
public final class PanamaPoolingAllocatorConfig extends AbstractPoolingAllocatorConfig {

  /** Creates a new PanamaPoolingAllocatorConfig with default values. */
  public PanamaPoolingAllocatorConfig() {
    super();
  }

  /**
   * Creates a new PanamaPoolingAllocatorConfig with the original 14 parameters.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution in bytes
   * @param maxStacks maximum number of stacks in the pool
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables in the pool
   * @param memoryDecommitEnabled whether memory decommit optimization is enabled
   * @param poolWarmingEnabled whether pool warming is enabled on startup
   * @param poolWarmingPercentage the pool warming percentage (0.0 to 1.0)
   * @param totalCoreInstances maximum concurrent core module instances
   * @param totalComponentInstances maximum concurrent component instances
   * @param maxCoreInstancesPerComponent max core instances per component
   * @param totalGcHeaps maximum concurrent GC heaps
   * @param maxMemorySize maximum memory size for any memory in the pool
   */
  public PanamaPoolingAllocatorConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage,
      final int totalCoreInstances,
      final int totalComponentInstances,
      final int maxCoreInstancesPerComponent,
      final int totalGcHeaps,
      final long maxMemorySize) {
    super(
        instancePoolSize,
        maxMemoryPerInstance,
        stackSize,
        maxStacks,
        maxTablesPerInstance,
        maxTables,
        memoryDecommitEnabled,
        poolWarmingEnabled,
        poolWarmingPercentage,
        totalCoreInstances,
        totalComponentInstances,
        maxCoreInstancesPerComponent,
        totalGcHeaps,
        maxMemorySize);
  }

  /**
   * Creates a new PanamaPoolingAllocatorConfig with all 28 parameters.
   *
   * @param instancePoolSize the number of instances in the pool
   * @param maxMemoryPerInstance maximum memory per instance in bytes
   * @param stackSize stack size for WebAssembly execution in bytes
   * @param maxStacks maximum number of stacks in the pool
   * @param maxTablesPerInstance maximum tables per instance
   * @param maxTables maximum total tables in the pool
   * @param memoryDecommitEnabled whether memory decommit optimization is enabled
   * @param poolWarmingEnabled whether pool warming is enabled on startup
   * @param poolWarmingPercentage the pool warming percentage (0.0 to 1.0)
   * @param totalCoreInstances maximum concurrent core module instances
   * @param totalComponentInstances maximum concurrent component instances
   * @param maxCoreInstancesPerComponent max core instances per component
   * @param totalGcHeaps maximum concurrent GC heaps
   * @param maxMemorySize maximum memory size for any memory in the pool
   * @param maxUnusedWarmSlots maximum unused warm slots
   * @param decommitBatchSize decommit batch size
   * @param linearMemoryKeepResident linear memory keep resident size
   * @param tableKeepResident table keep resident size
   * @param asyncStackKeepResident async stack keep resident size
   * @param totalMemories total memories in the pool
   * @param maxCoreInstanceSize max core instance size
   * @param maxComponentInstanceSize max component instance size
   * @param maxMemoriesPerModule max memories per module
   * @param maxMemoriesPerComponent max memories per component
   * @param tableElements max table elements
   * @param memoryProtectionKeysEnabled whether MPK is enabled
   * @param maxMemoryProtectionKeys max memory protection keys
   * @param pagemapScanEnabled whether pagemap scan is enabled
   */
  @SuppressWarnings("checkstyle:ParameterNumber")
  public PanamaPoolingAllocatorConfig(
      final int instancePoolSize,
      final long maxMemoryPerInstance,
      final int stackSize,
      final int maxStacks,
      final int maxTablesPerInstance,
      final int maxTables,
      final boolean memoryDecommitEnabled,
      final boolean poolWarmingEnabled,
      final float poolWarmingPercentage,
      final int totalCoreInstances,
      final int totalComponentInstances,
      final int maxCoreInstancesPerComponent,
      final int totalGcHeaps,
      final long maxMemorySize,
      final int maxUnusedWarmSlots,
      final int decommitBatchSize,
      final long linearMemoryKeepResident,
      final long tableKeepResident,
      final long asyncStackKeepResident,
      final int totalMemories,
      final long maxCoreInstanceSize,
      final long maxComponentInstanceSize,
      final int maxMemoriesPerModule,
      final int maxMemoriesPerComponent,
      final int tableElements,
      final ai.tegmentum.wasmtime4j.config.Enabled memoryProtectionKeysEnabled,
      final int maxMemoryProtectionKeys,
      final ai.tegmentum.wasmtime4j.config.Enabled pagemapScanEnabled) {
    super(
        instancePoolSize,
        maxMemoryPerInstance,
        stackSize,
        maxStacks,
        maxTablesPerInstance,
        maxTables,
        memoryDecommitEnabled,
        poolWarmingEnabled,
        poolWarmingPercentage,
        totalCoreInstances,
        totalComponentInstances,
        maxCoreInstancesPerComponent,
        totalGcHeaps,
        maxMemorySize,
        maxUnusedWarmSlots,
        decommitBatchSize,
        linearMemoryKeepResident,
        tableKeepResident,
        asyncStackKeepResident,
        totalMemories,
        maxCoreInstanceSize,
        maxComponentInstanceSize,
        maxMemoriesPerModule,
        maxMemoriesPerComponent,
        tableElements,
        memoryProtectionKeysEnabled,
        maxMemoryProtectionKeys,
        pagemapScanEnabled);
  }
}
