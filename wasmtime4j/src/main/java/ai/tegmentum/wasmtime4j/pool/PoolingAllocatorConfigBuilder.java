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
   * Sets the maximum number of concurrent core module instances.
   *
   * <p>This value has a direct impact on the amount of memory allocated by the pooling allocator.
   *
   * @param count the total core instances
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   */
  PoolingAllocatorConfigBuilder totalCoreInstances(int count);

  /**
   * Sets the maximum number of concurrent component instances.
   *
   * <p>This value has a direct impact on the amount of memory allocated by the pooling allocator.
   *
   * @param count the total component instances
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   */
  PoolingAllocatorConfigBuilder totalComponentInstances(int count);

  /**
   * Sets the maximum number of core instances a single component can create.
   *
   * @param count the max core instances per component
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   */
  PoolingAllocatorConfigBuilder maxCoreInstancesPerComponent(int count);

  /**
   * Sets the maximum number of concurrent GC heaps.
   *
   * <p>This value has a direct impact on the amount of memory allocated by the pooling allocator.
   * The GC heap pool contains the space needed for each GC heap used by a store.
   *
   * @param count the total GC heaps
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   */
  PoolingAllocatorConfigBuilder totalGcHeaps(int count);

  /**
   * Sets the maximum size in bytes that a linear memory can grow to.
   *
   * <p>This is the maximum size of any memory in the pool.
   *
   * @param bytes the max memory size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is not positive
   */
  PoolingAllocatorConfigBuilder maxMemorySize(long bytes);

  /**
   * Sets the maximum number of unused warm slots to retain.
   *
   * <p>When using the pooling allocator with affine slot selection, this configures
   * how many "warm" slots are retained for reuse by an affine allocation.
   *
   * @param count the max unused warm slots
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is negative
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder maxUnusedWarmSlots(int count);

  /**
   * Sets the decommit batch size.
   *
   * <p>This controls how many pages are decommitted at once, which can help amortize
   * the cost of decommit operations.
   *
   * @param size the decommit batch size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if size is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder decommitBatchSize(int size);

  /**
   * Sets the amount of linear memory to keep resident in bytes.
   *
   * <p>This controls the minimum amount of memory that will remain resident
   * after an instance is deallocated, avoiding expensive decommit/recommit cycles.
   * A value of 0 means use the system default.
   *
   * @param bytes the linear memory keep resident size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder linearMemoryKeepResident(long bytes);

  /**
   * Sets the amount of table memory to keep resident in bytes.
   *
   * <p>This controls the minimum amount of table memory that will remain resident
   * after an instance is deallocated. A value of 0 means use the system default.
   *
   * @param bytes the table keep resident size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder tableKeepResident(long bytes);

  /**
   * Sets the amount of async stack memory to keep resident in bytes.
   *
   * <p>This controls the minimum amount of async stack memory that will remain resident
   * after an instance is deallocated. A value of 0 means use the system default.
   *
   * @param bytes the async stack keep resident size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is negative
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder asyncStackKeepResident(long bytes);

  /**
   * Sets the total number of memories that can be allocated in the pool.
   *
   * <p>This is separate from instance count and controls the total memory slots
   * available across all instances.
   *
   * @param count the total memories
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder totalMemories(int count);

  /**
   * Sets the maximum size of core instance metadata in bytes.
   *
   * <p>This limits the amount of memory used for instance metadata like globals,
   * exported function trampolines, etc.
   *
   * @param bytes the max core instance size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder maxCoreInstanceSize(long bytes);

  /**
   * Sets the maximum size of component instance metadata in bytes.
   *
   * <p>This limits the amount of memory used for component instance metadata.
   *
   * @param bytes the max component instance size
   * @return this builder for method chaining
   * @throws IllegalArgumentException if bytes is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder maxComponentInstanceSize(long bytes);

  /**
   * Sets the maximum number of memories per module.
   *
   * <p>This limits how many linear memories a single module can define.
   *
   * @param count the max memories per module
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder maxMemoriesPerModule(int count);

  /**
   * Sets the maximum number of memories per component.
   *
   * <p>This limits how many linear memories a single component can use across
   * all of its internal modules.
   *
   * @param count the max memories per component
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder maxMemoriesPerComponent(int count);

  /**
   * Sets the maximum number of table elements.
   *
   * <p>This is the maximum size of any table in the pool.
   *
   * @param count the max table elements
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is not positive
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder tableElements(int count);

  /**
   * Enables or disables memory protection keys (MPK).
   *
   * <p>Memory protection keys are a Linux/x86-specific feature that provides
   * hardware-assisted memory isolation between instances.
   *
   * @param enabled true to enable MPK
   * @return this builder for method chaining
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder memoryProtectionKeysEnabled(boolean enabled);

  /**
   * Sets the maximum number of memory protection keys to use.
   *
   * <p>This is only relevant on Linux/x86 systems that support MPK.
   * A value of 0 means MPK is disabled.
   *
   * @param count the max memory protection keys
   * @return this builder for method chaining
   * @throws IllegalArgumentException if count is negative
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder maxMemoryProtectionKeys(int count);

  /**
   * Enables or disables PAGEMAP_SCAN ioctl for memory tracking.
   *
   * <p>This is a Linux 6.7+ feature that enables more efficient memory tracking.
   *
   * @param enabled true to enable PAGEMAP_SCAN
   * @return this builder for method chaining
   * @since 1.1.0
   */
  PoolingAllocatorConfigBuilder pagemapScanEnabled(boolean enabled);

  /**
   * Builds the configuration with the specified settings.
   *
   * @return a new PoolingAllocatorConfig instance
   * @throws IllegalStateException if the configuration is invalid
   */
  PoolingAllocatorConfig build();
}
