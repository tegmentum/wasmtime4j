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

/**
 * Builder for creating {@link PoolingAllocatorConfig} instances.
 *
 * <p>This builder provides a fluent API for configuring pooling allocator settings including pool
 * sizes, memory limits, and warming options.
 *
 * @since 1.0.0
 */
public interface PoolingAllocatorConfigBuilder {

  /**
   * Sets the number of instances in the pool.
   *
   * @param size the instance pool size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if size is not positive
   */
  PoolingAllocatorConfigBuilder instancePoolSize(int size);

  /**
   * Sets the maximum memory per instance in bytes.
   *
   * @param bytes the maximum memory per instance
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is not positive
   */
  PoolingAllocatorConfigBuilder maxMemoryPerInstance(long bytes);

  /**
   * Sets the stack size for WebAssembly execution in bytes.
   *
   * @param bytes the stack size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is not positive
   */
  PoolingAllocatorConfigBuilder stackSize(int bytes);

  /**
   * Sets the maximum number of stacks in the pool.
   *
   * @param count the maximum stacks
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   */
  PoolingAllocatorConfigBuilder maxStacks(int count);

  /**
   * Sets the maximum number of tables per instance.
   *
   * @param count the maximum tables per instance
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is negative
   */
  PoolingAllocatorConfigBuilder maxTablesPerInstance(int count);

  /**
   * Sets the maximum total tables in the pool.
   *
   * @param count the maximum tables
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   */
  PoolingAllocatorConfigBuilder maxTables(int count);

  /**
   * Enables or disables memory decommit optimization.
   *
   * <p>When enabled, unused memory pages are returned to the operating system, reducing memory
   * footprint but potentially increasing allocation latency.
   *
   * @param enabled true to enable memory decommit
   * @return this builder for method chaining
   */
  PoolingAllocatorConfigBuilder memoryDecommitEnabled(boolean enabled);

  /**
   * Enables or disables pool warming on startup.
   *
   * <p>When enabled, a percentage of the pool is pre-allocated during initialization to reduce
   * first-request latency.
   *
   * @param enabled true to enable pool warming
   * @return this builder for method chaining
   */
  PoolingAllocatorConfigBuilder poolWarmingEnabled(boolean enabled);

  /**
   * Sets the pool warming percentage (0.0 to 1.0).
   *
   * <p>This determines what fraction of the pool is pre-allocated during initialization.
   *
   * @param percentage the warming percentage between 0.0 and 1.0
   * @return this builder for method chaining
   * @throws IllegalArgumentException if percentage is not between 0.0 and 1.0
   */
  PoolingAllocatorConfigBuilder poolWarmingPercentage(float percentage);

  /**
   * Builds the configuration with the specified settings.
   *
   * @return a new PoolingAllocatorConfig instance
   * @throws IllegalStateException if the configuration is invalid
   */
  PoolingAllocatorConfig build();
}
