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
   * Validates this configuration for consistency.
   *
   * @throws IllegalArgumentException if the configuration is invalid
   */
  void validate();
}
