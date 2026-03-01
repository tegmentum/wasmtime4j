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

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.time.Duration;

/**
 * High-performance pooling allocator for WebAssembly execution.
 *
 * <p>The pooling allocator provides significant performance improvements for allocation-heavy
 * workloads by reusing pre-allocated memory pools for WebAssembly instances, stacks, and tables.
 * This is particularly beneficial for serverless and microservice environments where instances are
 * frequently created and destroyed.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create allocator with custom configuration
 * PoolingAllocatorConfig config = PoolingAllocatorConfig.builder()
 *     .instancePoolSize(1000)
 *     .maxMemoryPerInstance(64 * 1024 * 1024)
 *     .poolWarmingEnabled(true)
 *     .build();
 *
 * try (PoolingAllocator allocator = PoolingAllocator.create(config)) {
 *     // Configure engine to use this allocator
 *     EngineConfig engineConfig = EngineConfig.builder().build();
 *     allocator.configureEngine(engineConfig);
 *
 *     Engine engine = Engine.create(engineConfig);
 *     // ... use engine
 *
 *     // Monitor pool statistics
 *     PoolStatistics stats = allocator.getStatistics();
 *     System.out.println("Reuse ratio: " + stats.getReuseRatio());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface PoolingAllocator extends Closeable {

  /**
   * Creates a new pooling allocator with default configuration.
   *
   * @return a new PoolingAllocator instance
   * @throws WasmException if the allocator cannot be created
   */
  static PoolingAllocator create() throws WasmException {
    return create(PoolingAllocatorConfig.defaultConfig());
  }

  /**
   * Creates a new pooling allocator with the specified configuration.
   *
   * @param config the allocator configuration
   * @return a new PoolingAllocator instance
   * @throws WasmException if the allocator cannot be created
   * @throws IllegalArgumentException if config is null
   */
  static PoolingAllocator create(final PoolingAllocatorConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }

    config.validate();

    try {
      final Class<?> allocatorClass =
          Class.forName("ai.tegmentum.wasmtime4j.panama.pool.PanamaPoolingAllocator");
      return (PoolingAllocator)
          allocatorClass.getConstructor(PoolingAllocatorConfig.class).newInstance(config);
    } catch (final ClassNotFoundException e) {
      try {
        final Class<?> allocatorClass =
            Class.forName("ai.tegmentum.wasmtime4j.jni.pool.JniPoolingAllocator");
        return (PoolingAllocator)
            allocatorClass.getConstructor(PoolingAllocatorConfig.class).newInstance(config);
      } catch (final ClassNotFoundException e2) {
        throw new WasmException(
            "No PoolingAllocator implementation available. "
                + "Ensure wasmtime4j-panama or wasmtime4j-jni is on the classpath.");
      } catch (final Exception e2) {
        throw new WasmException("Failed to create pooling allocator: " + e2.getMessage(), e2);
      }
    } catch (final Exception e) {
      throw new WasmException("Failed to create pooling allocator: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the configuration used by this allocator.
   *
   * @return the allocator configuration
   */
  PoolingAllocatorConfig getConfig();

  /**
   * Allocates an instance from the pool.
   *
   * @return the allocated instance ID
   * @throws WasmException if allocation fails (e.g., pool exhausted)
   */
  long allocateInstance() throws WasmException;

  /**
   * Reuses an existing instance from the pool.
   *
   * @param instanceId the instance ID to reuse
   * @throws WasmException if the instance cannot be reused
   */
  void reuseInstance(long instanceId) throws WasmException;

  /**
   * Releases an instance back to the pool.
   *
   * @param instanceId the instance ID to release
   * @throws WasmException if the instance cannot be released
   */
  void releaseInstance(long instanceId) throws WasmException;

  /**
   * Gets the current pool statistics.
   *
   * @return the pool statistics
   */
  PoolStatistics getStatistics();

  /**
   * Resets the pool statistics.
   *
   * @throws WasmException if statistics cannot be reset
   */
  void resetStatistics() throws WasmException;

  /**
   * Warms up the pools by pre-allocating resources.
   *
   * <p>This is typically called automatically during initialization if pool warming is enabled, but
   * can also be called manually to warm additional resources.
   *
   * @throws WasmException if pool warming fails
   */
  void warmPools() throws WasmException;

  /**
   * Performs maintenance operations on the pools.
   *
   * <p>This includes compacting fragmented memory, releasing unused resources, and updating
   * internal statistics. Should be called periodically for long-running applications.
   *
   * @throws WasmException if maintenance fails
   */
  void performMaintenance() throws WasmException;

  /**
   * Configures an engine to use this pooling allocator.
   *
   * <p>This method should be called before creating the engine. The engine will use the pool's
   * memory allocation strategy for all instances.
   *
   * @param engineConfig the engine configuration to modify
   * @throws WasmException if the engine cannot be configured
   * @throws IllegalArgumentException if engineConfig is null
   */
  void configureEngine(EngineConfig engineConfig) throws WasmException;

  /**
   * Gets the allocator's uptime since creation.
   *
   * @return the uptime duration
   */
  Duration getUptime();

  /**
   * Checks if the allocator is still valid and usable.
   *
   * @return true if the allocator is valid
   */
  boolean isValid();

  /**
   * Closes this allocator and releases all pooled resources.
   *
   * <p>After calling this method, the allocator becomes invalid and should not be used. Any
   * instances allocated from this pool should be released before closing.
   */
  @Override
  void close();
}
