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

package ai.tegmentum.wasmtime4j.jni.pool;

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfigBuilder;

/**
 * JNI implementation of {@link PoolingAllocatorConfigBuilder}.
 *
 * <p>This class provides a builder pattern for constructing {@link JniPoolingAllocatorConfig}
 * instances.
 *
 * @since 1.0.0
 */
public final class JniPoolingAllocatorConfigBuilder implements PoolingAllocatorConfigBuilder {

  private int instancePoolSize = PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE;
  private long maxMemoryPerInstance = PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PER_INSTANCE;
  private int stackSize = PoolingAllocatorConfig.DEFAULT_STACK_SIZE;
  private int maxStacks = PoolingAllocatorConfig.DEFAULT_MAX_STACKS;
  private int maxTablesPerInstance = PoolingAllocatorConfig.DEFAULT_MAX_TABLES_PER_INSTANCE;
  private int maxTables = PoolingAllocatorConfig.DEFAULT_MAX_TABLES;
  private boolean memoryDecommitEnabled = true;
  private boolean poolWarmingEnabled = true;
  private float poolWarmingPercentage = PoolingAllocatorConfig.DEFAULT_POOL_WARMING_PERCENTAGE;
  private int totalCoreInstances = PoolingAllocatorConfig.DEFAULT_TOTAL_CORE_INSTANCES;
  private int totalComponentInstances = PoolingAllocatorConfig.DEFAULT_TOTAL_COMPONENT_INSTANCES;
  private int maxCoreInstancesPerComponent =
      PoolingAllocatorConfig.DEFAULT_MAX_CORE_INSTANCES_PER_COMPONENT;
  private int totalGcHeaps = PoolingAllocatorConfig.DEFAULT_TOTAL_GC_HEAPS;
  private long maxMemorySize = PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_SIZE;
  private int maxUnusedWarmSlots = 0;
  private int decommitBatchSize = 1;
  private long linearMemoryKeepResident = 0;
  private long tableKeepResident = 0;
  private long asyncStackKeepResident = 0;
  private int totalMemories = 1000;
  private long maxCoreInstanceSize = 1024 * 1024;
  private long maxComponentInstanceSize = 1024 * 1024;
  private int maxMemoriesPerModule = 1;
  private int maxMemoriesPerComponent = 1;
  private int tableElements = 10000;
  private boolean memoryProtectionKeysEnabled = false;
  private int maxMemoryProtectionKeys = 0;
  private boolean pagemapScanEnabled = false;

  @Override
  public PoolingAllocatorConfigBuilder instancePoolSize(final int instancePoolSize) {
    this.instancePoolSize = instancePoolSize;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoryPerInstance(final long maxMemoryPerInstance) {
    this.maxMemoryPerInstance = maxMemoryPerInstance;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder stackSize(final int stackSize) {
    this.stackSize = stackSize;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxStacks(final int maxStacks) {
    this.maxStacks = maxStacks;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxTablesPerInstance(final int maxTablesPerInstance) {
    this.maxTablesPerInstance = maxTablesPerInstance;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxTables(final int maxTables) {
    this.maxTables = maxTables;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder memoryDecommitEnabled(final boolean memoryDecommitEnabled) {
    this.memoryDecommitEnabled = memoryDecommitEnabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder poolWarmingEnabled(final boolean poolWarmingEnabled) {
    this.poolWarmingEnabled = poolWarmingEnabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder poolWarmingPercentage(final float poolWarmingPercentage) {
    this.poolWarmingPercentage = poolWarmingPercentage;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalCoreInstances(final int totalCoreInstances) {
    this.totalCoreInstances = totalCoreInstances;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalComponentInstances(final int totalComponentInstances) {
    this.totalComponentInstances = totalComponentInstances;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxCoreInstancesPerComponent(
      final int maxCoreInstancesPerComponent) {
    this.maxCoreInstancesPerComponent = maxCoreInstancesPerComponent;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalGcHeaps(final int totalGcHeaps) {
    this.totalGcHeaps = totalGcHeaps;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemorySize(final long maxMemorySize) {
    this.maxMemorySize = maxMemorySize;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxUnusedWarmSlots(final int maxUnusedWarmSlots) {
    this.maxUnusedWarmSlots = maxUnusedWarmSlots;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder decommitBatchSize(final int decommitBatchSize) {
    this.decommitBatchSize = decommitBatchSize;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder linearMemoryKeepResident(
      final long linearMemoryKeepResident) {
    this.linearMemoryKeepResident = linearMemoryKeepResident;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder tableKeepResident(final long tableKeepResident) {
    this.tableKeepResident = tableKeepResident;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder asyncStackKeepResident(final long asyncStackKeepResident) {
    this.asyncStackKeepResident = asyncStackKeepResident;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder totalMemories(final int totalMemories) {
    this.totalMemories = totalMemories;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxCoreInstanceSize(final long maxCoreInstanceSize) {
    this.maxCoreInstanceSize = maxCoreInstanceSize;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxComponentInstanceSize(
      final long maxComponentInstanceSize) {
    this.maxComponentInstanceSize = maxComponentInstanceSize;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoriesPerModule(final int maxMemoriesPerModule) {
    this.maxMemoriesPerModule = maxMemoriesPerModule;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoriesPerComponent(final int maxMemoriesPerComponent) {
    this.maxMemoriesPerComponent = maxMemoriesPerComponent;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder tableElements(final int tableElements) {
    this.tableElements = tableElements;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder memoryProtectionKeysEnabled(
      final boolean memoryProtectionKeysEnabled) {
    this.memoryProtectionKeysEnabled = memoryProtectionKeysEnabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoryProtectionKeys(final int maxMemoryProtectionKeys) {
    this.maxMemoryProtectionKeys = maxMemoryProtectionKeys;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder pagemapScanEnabled(final boolean pagemapScanEnabled) {
    this.pagemapScanEnabled = pagemapScanEnabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfig build() {
    final JniPoolingAllocatorConfig config =
        new JniPoolingAllocatorConfig(
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
    config.validate();
    return config;
  }
}
