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
 * Configuration for the pooling allocator.
 *
 * <p>The pooling allocator provides significant performance improvements for allocation-heavy
 * workloads by reusing pre-allocated memory pools for WebAssembly instances, stacks, and tables.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PoolingAllocatorConfig config = PoolingAllocatorConfig.builder()
 *     .instancePoolSize(1000)
 *     .maxMemoryPerInstance(64 * 1024 * 1024)  // 64MB
 *     .stackSize(1024 * 1024)  // 1MB
 *     .poolWarmingEnabled(true)
 *     .build();
 *
 * PoolingAllocator allocator = PoolingAllocator.create(config);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface PoolingAllocatorConfig {

  /** Default instance pool size. */
  int DEFAULT_INSTANCE_POOL_SIZE = 1000;

  /** Default maximum memory per instance (1GB). */
  long DEFAULT_MAX_MEMORY_PER_INSTANCE = 1024L * 1024L * 1024L;

  /** Default stack size (1MB). */
  int DEFAULT_STACK_SIZE = 1024 * 1024;

  /** Default maximum number of stacks. */
  int DEFAULT_MAX_STACKS = 1000;

  /** Default maximum tables per instance. */
  int DEFAULT_MAX_TABLES_PER_INSTANCE = 10;

  /** Default maximum total tables. */
  int DEFAULT_MAX_TABLES = 10000;

  /** Default pool warming percentage. */
  float DEFAULT_POOL_WARMING_PERCENTAGE = 0.2f;

  /** Default total core instances. */
  int DEFAULT_TOTAL_CORE_INSTANCES = 1000;

  /** Default total component instances. */
  int DEFAULT_TOTAL_COMPONENT_INSTANCES = 1000;

  /** Default max core instances per component. */
  int DEFAULT_MAX_CORE_INSTANCES_PER_COMPONENT = 20;

  /** Default total GC heaps. */
  int DEFAULT_TOTAL_GC_HEAPS = 1000;

  /** Default max memory size (2GB). */
  long DEFAULT_MAX_MEMORY_SIZE = 2L * 1024L * 1024L * 1024L;

  /** Default max unused warm slots. */
  int DEFAULT_MAX_UNUSED_WARM_SLOTS = 100;

  /** Default decommit batch size. */
  int DEFAULT_DECOMMIT_BATCH_SIZE = 1;

  /** Default linear memory keep resident (0 means use system default). */
  long DEFAULT_LINEAR_MEMORY_KEEP_RESIDENT = 0;

  /** Default table keep resident (0 means use system default). */
  long DEFAULT_TABLE_KEEP_RESIDENT = 0;

  /** Default async stack keep resident (0 means use system default). */
  long DEFAULT_ASYNC_STACK_KEEP_RESIDENT = 0;

  /** Default total memories. */
  int DEFAULT_TOTAL_MEMORIES = 1000;

  /** Default max core instance size (1MB). */
  long DEFAULT_MAX_CORE_INSTANCE_SIZE = 1024L * 1024L;

  /** Default max component instance size (2MB). */
  long DEFAULT_MAX_COMPONENT_INSTANCE_SIZE = 2L * 1024L * 1024L;

  /** Default max memories per module. */
  int DEFAULT_MAX_MEMORIES_PER_MODULE = 1;

  /** Default max memories per component. */
  int DEFAULT_MAX_MEMORIES_PER_COMPONENT = 1;

  /** Default table elements (10000). */
  int DEFAULT_TABLE_ELEMENTS = 10000;

  /** Default max memory protection keys. */
  int DEFAULT_MAX_MEMORY_PROTECTION_KEYS = 0;

  /**
   * Creates a new configuration builder with default values.
   *
   * @return a new builder instance
   */
  static PoolingAllocatorConfigBuilder builder() {
    try {
      final Class<?> builderClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.pool.PanamaPoolingAllocatorConfigBuilder");
      return (PoolingAllocatorConfigBuilder) builderClass.getDeclaredConstructor().newInstance();
    } catch (final ClassNotFoundException e) {
      try {
        final Class<?> builderClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.pool.JniPoolingAllocatorConfigBuilder");
        return (PoolingAllocatorConfigBuilder) builderClass.getDeclaredConstructor().newInstance();
      } catch (final ClassNotFoundException e2) {
        throw new RuntimeException(
            "No PoolingAllocatorConfigBuilder implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new RuntimeException("Failed to create pooling allocator config builder", e2);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Failed to create pooling allocator config builder", e);
    }
  }

  /**
   * Creates a default configuration.
   *
   * @return a default configuration instance
   */
  static PoolingAllocatorConfig defaultConfig() {
    return builder().build();
  }

  /**
   * Gets the number of instances in the pool.
   *
   * @return the instance pool size
   */
  int getInstancePoolSize();

  /**
   * Gets the maximum memory per instance in bytes.
   *
   * @return the maximum memory per instance
   */
  long getMaxMemoryPerInstance();

  /**
   * Gets the stack size for WebAssembly execution in bytes.
   *
   * @return the stack size
   */
  int getStackSize();

  /**
   * Gets the maximum number of stacks in the pool.
   *
   * @return the maximum stacks
   */
  int getMaxStacks();

  /**
   * Gets the maximum number of tables per instance.
   *
   * @return the maximum tables per instance
   */
  int getMaxTablesPerInstance();

  /**
   * Gets the maximum total tables in the pool.
   *
   * @return the maximum tables
   */
  int getMaxTables();

  /**
   * Gets whether memory decommit optimization is enabled.
   *
   * @return true if memory decommit is enabled
   */
  boolean isMemoryDecommitEnabled();

  /**
   * Gets whether pool warming is enabled on startup.
   *
   * @return true if pool warming is enabled
   */
  boolean isPoolWarmingEnabled();

  /**
   * Gets the pool warming percentage (0.0 to 1.0).
   *
   * @return the pool warming percentage
   */
  float getPoolWarmingPercentage();

  /**
   * Gets the maximum number of concurrent core module instances.
   *
   * <p>This value has a direct impact on the amount of memory allocated by the pooling allocator.
   *
   * @return the total core instances
   */
  int getTotalCoreInstances();

  /**
   * Gets the maximum number of concurrent component instances.
   *
   * <p>This value has a direct impact on the amount of memory allocated by the pooling allocator.
   *
   * @return the total component instances
   */
  int getTotalComponentInstances();

  /**
   * Gets the maximum number of core instances a single component can create.
   *
   * @return the max core instances per component
   */
  int getMaxCoreInstancesPerComponent();

  /**
   * Gets the maximum number of concurrent GC heaps.
   *
   * <p>This value has a direct impact on the amount of memory allocated by the pooling allocator.
   * The GC heap pool contains the space needed for each GC heap used by a store.
   *
   * @return the total GC heaps
   */
  int getTotalGcHeaps();

  /**
   * Gets the maximum size in bytes that a linear memory can grow to.
   *
   * <p>This is the maximum size of any memory in the pool.
   *
   * @return the max memory size
   */
  long getMaxMemorySize();

  /**
   * Gets the maximum number of unused warm slots to retain.
   *
   * <p>When using the pooling allocator with affine slot selection, this configures
   * how many "warm" slots are retained for reuse by an affine allocation.
   *
   * @return the max unused warm slots
   * @since 1.1.0
   */
  int getMaxUnusedWarmSlots();

  /**
   * Gets the decommit batch size.
   *
   * <p>This controls how many pages are decommitted at once, which can help amortize
   * the cost of decommit operations.
   *
   * @return the decommit batch size
   * @since 1.1.0
   */
  int getDecommitBatchSize();

  /**
   * Gets the amount of linear memory to keep resident in bytes.
   *
   * <p>This controls the minimum amount of memory that will remain resident
   * after an instance is deallocated, avoiding expensive decommit/recommit cycles.
   * A value of 0 means use the system default.
   *
   * @return the linear memory keep resident size
   * @since 1.1.0
   */
  long getLinearMemoryKeepResident();

  /**
   * Gets the amount of table memory to keep resident in bytes.
   *
   * <p>This controls the minimum amount of table memory that will remain resident
   * after an instance is deallocated. A value of 0 means use the system default.
   *
   * @return the table keep resident size
   * @since 1.1.0
   */
  long getTableKeepResident();

  /**
   * Gets the amount of async stack memory to keep resident in bytes.
   *
   * <p>This controls the minimum amount of async stack memory that will remain resident
   * after an instance is deallocated. A value of 0 means use the system default.
   *
   * @return the async stack keep resident size
   * @since 1.1.0
   */
  long getAsyncStackKeepResident();

  /**
   * Gets the total number of memories that can be allocated in the pool.
   *
   * <p>This is separate from instance count and controls the total memory slots
   * available across all instances.
   *
   * @return the total memories
   * @since 1.1.0
   */
  int getTotalMemories();

  /**
   * Gets the maximum size of core instance metadata in bytes.
   *
   * <p>This limits the amount of memory used for instance metadata like globals,
   * exported function trampolines, etc.
   *
   * @return the max core instance size
   * @since 1.1.0
   */
  long getMaxCoreInstanceSize();

  /**
   * Gets the maximum size of component instance metadata in bytes.
   *
   * <p>This limits the amount of memory used for component instance metadata.
   *
   * @return the max component instance size
   * @since 1.1.0
   */
  long getMaxComponentInstanceSize();

  /**
   * Gets the maximum number of memories per module.
   *
   * <p>This limits how many linear memories a single module can define.
   *
   * @return the max memories per module
   * @since 1.1.0
   */
  int getMaxMemoriesPerModule();

  /**
   * Gets the maximum number of memories per component.
   *
   * <p>This limits how many linear memories a single component can use across
   * all of its internal modules.
   *
   * @return the max memories per component
   * @since 1.1.0
   */
  int getMaxMemoriesPerComponent();

  /**
   * Gets the maximum number of table elements.
   *
   * <p>This is the maximum size of any table in the pool.
   *
   * @return the max table elements
   * @since 1.1.0
   */
  int getTableElements();

  /**
   * Gets whether memory protection keys (MPK) are enabled.
   *
   * <p>Memory protection keys are a Linux/x86-specific feature that provides
   * hardware-assisted memory isolation between instances.
   *
   * @return true if MPK is enabled
   * @since 1.1.0
   */
  boolean isMemoryProtectionKeysEnabled();

  /**
   * Gets the maximum number of memory protection keys to use.
   *
   * <p>This is only relevant on Linux/x86 systems that support MPK.
   * A value of 0 means MPK is disabled.
   *
   * @return the max memory protection keys
   * @since 1.1.0
   */
  int getMaxMemoryProtectionKeys();

  /**
   * Gets whether PAGEMAP_SCAN ioctl is enabled for memory tracking.
   *
   * <p>This is a Linux 6.7+ feature that enables more efficient memory tracking.
   *
   * @return true if PAGEMAP_SCAN is enabled
   * @since 1.1.0
   */
  boolean isPagemapScanEnabled();

  /**
   * Validates this configuration for consistency.
   *
   * @throws IllegalArgumentException if the configuration is invalid
   */
  void validate();
}
