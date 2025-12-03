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

import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;

/**
 * Panama implementation of {@link PoolingAllocatorConfig}.
 *
 * <p>This class provides configuration for the pooling allocator with various settings for pool
 * sizes, memory limits, and warming options.
 *
 * @since 1.0.0
 */
public final class PanamaPoolingAllocatorConfig implements PoolingAllocatorConfig {

  private final int instancePoolSize;
  private final long maxMemoryPerInstance;
  private final int stackSize;
  private final int maxStacks;
  private final int maxTablesPerInstance;
  private final int maxTables;
  private final boolean memoryDecommitEnabled;
  private final boolean poolWarmingEnabled;
  private final float poolWarmingPercentage;

  /**
   * Creates a new PanamaPoolingAllocatorConfig with all parameters.
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
      final float poolWarmingPercentage) {
    this.instancePoolSize = instancePoolSize;
    this.maxMemoryPerInstance = maxMemoryPerInstance;
    this.stackSize = stackSize;
    this.maxStacks = maxStacks;
    this.maxTablesPerInstance = maxTablesPerInstance;
    this.maxTables = maxTables;
    this.memoryDecommitEnabled = memoryDecommitEnabled;
    this.poolWarmingEnabled = poolWarmingEnabled;
    this.poolWarmingPercentage = poolWarmingPercentage;
  }

  /** Creates a new PanamaPoolingAllocatorConfig with default values. */
  public PanamaPoolingAllocatorConfig() {
    this(
        DEFAULT_INSTANCE_POOL_SIZE,
        DEFAULT_MAX_MEMORY_PER_INSTANCE,
        DEFAULT_STACK_SIZE,
        DEFAULT_MAX_STACKS,
        DEFAULT_MAX_TABLES_PER_INSTANCE,
        DEFAULT_MAX_TABLES,
        true, // memoryDecommitEnabled
        true, // poolWarmingEnabled
        DEFAULT_POOL_WARMING_PERCENTAGE);
  }

  @Override
  public int getInstancePoolSize() {
    return instancePoolSize;
  }

  @Override
  public long getMaxMemoryPerInstance() {
    return maxMemoryPerInstance;
  }

  @Override
  public int getStackSize() {
    return stackSize;
  }

  @Override
  public int getMaxStacks() {
    return maxStacks;
  }

  @Override
  public int getMaxTablesPerInstance() {
    return maxTablesPerInstance;
  }

  @Override
  public int getMaxTables() {
    return maxTables;
  }

  @Override
  public boolean isMemoryDecommitEnabled() {
    return memoryDecommitEnabled;
  }

  @Override
  public boolean isPoolWarmingEnabled() {
    return poolWarmingEnabled;
  }

  @Override
  public float getPoolWarmingPercentage() {
    return poolWarmingPercentage;
  }

  @Override
  public void validate() {
    if (instancePoolSize <= 0) {
      throw new IllegalArgumentException("instancePoolSize must be positive");
    }
    if (maxMemoryPerInstance <= 0) {
      throw new IllegalArgumentException("maxMemoryPerInstance must be positive");
    }
    if (stackSize <= 0) {
      throw new IllegalArgumentException("stackSize must be positive");
    }
    if (maxStacks <= 0) {
      throw new IllegalArgumentException("maxStacks must be positive");
    }
    if (maxTablesPerInstance < 0) {
      throw new IllegalArgumentException("maxTablesPerInstance cannot be negative");
    }
    if (maxTables <= 0) {
      throw new IllegalArgumentException("maxTables must be positive");
    }
    if (poolWarmingPercentage < 0.0f || poolWarmingPercentage > 1.0f) {
      throw new IllegalArgumentException("poolWarmingPercentage must be between 0.0 and 1.0");
    }
  }

  @Override
  public String toString() {
    return "PanamaPoolingAllocatorConfig{"
        + "instancePoolSize="
        + instancePoolSize
        + ", maxMemoryPerInstance="
        + maxMemoryPerInstance
        + ", stackSize="
        + stackSize
        + ", maxStacks="
        + maxStacks
        + ", maxTablesPerInstance="
        + maxTablesPerInstance
        + ", maxTables="
        + maxTables
        + ", memoryDecommitEnabled="
        + memoryDecommitEnabled
        + ", poolWarmingEnabled="
        + poolWarmingEnabled
        + ", poolWarmingPercentage="
        + poolWarmingPercentage
        + '}';
  }
}
