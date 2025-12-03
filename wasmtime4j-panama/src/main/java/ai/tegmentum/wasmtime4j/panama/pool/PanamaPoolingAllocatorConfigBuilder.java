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
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfigBuilder;

/**
 * Panama implementation of {@link PoolingAllocatorConfigBuilder}.
 *
 * <p>This builder provides a fluent API for constructing {@link PanamaPoolingAllocatorConfig}
 * instances with various pool sizes, memory limits, and warming options.
 *
 * @since 1.0.0
 */
public final class PanamaPoolingAllocatorConfigBuilder implements PoolingAllocatorConfigBuilder {

  private int instancePoolSize = PoolingAllocatorConfig.DEFAULT_INSTANCE_POOL_SIZE;
  private long maxMemoryPerInstance = PoolingAllocatorConfig.DEFAULT_MAX_MEMORY_PER_INSTANCE;
  private int stackSize = PoolingAllocatorConfig.DEFAULT_STACK_SIZE;
  private int maxStacks = PoolingAllocatorConfig.DEFAULT_MAX_STACKS;
  private int maxTablesPerInstance = PoolingAllocatorConfig.DEFAULT_MAX_TABLES_PER_INSTANCE;
  private int maxTables = PoolingAllocatorConfig.DEFAULT_MAX_TABLES;
  private boolean memoryDecommitEnabled = true;
  private boolean poolWarmingEnabled = true;
  private float poolWarmingPercentage = PoolingAllocatorConfig.DEFAULT_POOL_WARMING_PERCENTAGE;

  /** Creates a new PanamaPoolingAllocatorConfigBuilder with default settings. */
  public PanamaPoolingAllocatorConfigBuilder() {
    // Default constructor with default values
  }

  @Override
  public PoolingAllocatorConfigBuilder instancePoolSize(final int size) {
    if (size <= 0) {
      throw new IllegalArgumentException("instancePoolSize must be positive");
    }
    this.instancePoolSize = size;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxMemoryPerInstance(final long bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("maxMemoryPerInstance must be positive");
    }
    this.maxMemoryPerInstance = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder stackSize(final int bytes) {
    if (bytes <= 0) {
      throw new IllegalArgumentException("stackSize must be positive");
    }
    this.stackSize = bytes;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxStacks(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxStacks must be positive");
    }
    this.maxStacks = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxTablesPerInstance(final int count) {
    if (count < 0) {
      throw new IllegalArgumentException("maxTablesPerInstance cannot be negative");
    }
    this.maxTablesPerInstance = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder maxTables(final int count) {
    if (count <= 0) {
      throw new IllegalArgumentException("maxTables must be positive");
    }
    this.maxTables = count;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder memoryDecommitEnabled(final boolean enabled) {
    this.memoryDecommitEnabled = enabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder poolWarmingEnabled(final boolean enabled) {
    this.poolWarmingEnabled = enabled;
    return this;
  }

  @Override
  public PoolingAllocatorConfigBuilder poolWarmingPercentage(final float percentage) {
    if (percentage < 0.0f || percentage > 1.0f) {
      throw new IllegalArgumentException("poolWarmingPercentage must be between 0.0 and 1.0");
    }
    this.poolWarmingPercentage = percentage;
    return this;
  }

  @Override
  public PoolingAllocatorConfig build() {
    final PanamaPoolingAllocatorConfig config =
        new PanamaPoolingAllocatorConfig(
            instancePoolSize,
            maxMemoryPerInstance,
            stackSize,
            maxStacks,
            maxTablesPerInstance,
            maxTables,
            memoryDecommitEnabled,
            poolWarmingEnabled,
            poolWarmingPercentage);
    config.validate();
    return config;
  }
}
