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

import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocator;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * Panama implementation of {@link PoolingAllocator}.
 *
 * <p>This implementation provides high-performance pooling allocation for WebAssembly execution by
 * reusing pre-allocated memory pools for instances, stacks, and tables.
 *
 * @since 1.0.0
 */
public final class PanamaPoolingAllocator implements PoolingAllocator {

  private static final Logger LOGGER = Logger.getLogger(PanamaPoolingAllocator.class.getName());
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final PoolingAllocatorConfig config;
  private final Arena arena;
  private final MemorySegment nativeAllocator;
  private final Instant createdAt;
  private volatile boolean closed = false;

  /**
   * Creates a new PanamaPoolingAllocator with the specified configuration.
   *
   * @param config the allocator configuration
   * @throws WasmException if the allocator cannot be created
   */
  public PanamaPoolingAllocator(final PoolingAllocatorConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }
    this.config = config;
    this.arena = Arena.ofShared();
    this.createdAt = Instant.now();

    // Create native allocator with configuration
    this.nativeAllocator =
        NATIVE_BINDINGS.poolingAllocatorCreateWithConfig(
            config.getInstancePoolSize(),
            config.getMaxMemoryPerInstance(),
            config.getStackSize(),
            config.getMaxStacks(),
            config.getMaxTablesPerInstance(),
            config.getMaxTables(),
            config.isMemoryDecommitEnabled(),
            config.isPoolWarmingEnabled(),
            config.getPoolWarmingPercentage());

    if (this.nativeAllocator == null || this.nativeAllocator.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native pooling allocator");
    }

    LOGGER.fine("Created Panama pooling allocator with config: " + config);
  }

  @Override
  public PoolingAllocatorConfig getConfig() {
    return config;
  }

  @Override
  public long allocateInstance() throws WasmException {
    ensureNotClosed();

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment instanceIdOut = localArena.allocate(ValueLayout.JAVA_LONG);

      final boolean success =
          NATIVE_BINDINGS.poolingAllocatorAllocateInstance(nativeAllocator, instanceIdOut);

      if (!success) {
        throw new WasmException("Failed to allocate instance from pool");
      }

      return instanceIdOut.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void reuseInstance(final long instanceId) throws WasmException {
    ensureNotClosed();

    final boolean success =
        NATIVE_BINDINGS.poolingAllocatorReuseInstance(nativeAllocator, instanceId);

    if (!success) {
      throw new WasmException("Failed to reuse instance: " + instanceId);
    }
  }

  @Override
  public void releaseInstance(final long instanceId) throws WasmException {
    ensureNotClosed();

    final boolean success =
        NATIVE_BINDINGS.poolingAllocatorReleaseInstance(nativeAllocator, instanceId);

    if (!success) {
      throw new WasmException("Failed to release instance: " + instanceId);
    }
  }

  @Override
  public PoolStatistics getStatistics() {
    ensureNotClosed();

    try (Arena localArena = Arena.ofConfined()) {
      // Allocate space for statistics struct (14 fields)
      // Layout: 12 x u64 + 2 x Duration (each Duration = 2 x u64 for secs and nanos)
      // Simplified: 14 x long = 112 bytes
      final MemorySegment statsOut = localArena.allocate(112);

      final boolean success =
          NATIVE_BINDINGS.poolingAllocatorGetStatistics(nativeAllocator, statsOut);

      if (!success) {
        LOGGER.warning("Failed to get pool statistics, returning empty statistics");
        return new PanamaPoolStatistics();
      }

      // Read statistics from native struct
      final long instancesAllocated = statsOut.get(ValueLayout.JAVA_LONG, 0);
      final long instancesReused = statsOut.get(ValueLayout.JAVA_LONG, 8);
      final long instancesCreated = statsOut.get(ValueLayout.JAVA_LONG, 16);
      final long memoryPoolsAllocated = statsOut.get(ValueLayout.JAVA_LONG, 24);
      final long memoryPoolsReused = statsOut.get(ValueLayout.JAVA_LONG, 32);
      final long stackPoolsAllocated = statsOut.get(ValueLayout.JAVA_LONG, 40);
      final long stackPoolsReused = statsOut.get(ValueLayout.JAVA_LONG, 48);
      final long tablePoolsAllocated = statsOut.get(ValueLayout.JAVA_LONG, 56);
      final long tablePoolsReused = statsOut.get(ValueLayout.JAVA_LONG, 64);
      final long peakMemoryUsage = statsOut.get(ValueLayout.JAVA_LONG, 72);
      final long currentMemoryUsage = statsOut.get(ValueLayout.JAVA_LONG, 80);
      final long allocationFailures = statsOut.get(ValueLayout.JAVA_LONG, 88);
      final long poolWarmingTimeNanos = statsOut.get(ValueLayout.JAVA_LONG, 96);
      final long averageAllocationTimeNanos = statsOut.get(ValueLayout.JAVA_LONG, 104);

      return new PanamaPoolStatistics(
          instancesAllocated,
          instancesReused,
          instancesCreated,
          memoryPoolsAllocated,
          memoryPoolsReused,
          stackPoolsAllocated,
          stackPoolsReused,
          tablePoolsAllocated,
          tablePoolsReused,
          peakMemoryUsage,
          currentMemoryUsage,
          allocationFailures,
          poolWarmingTimeNanos,
          averageAllocationTimeNanos);
    }
  }

  @Override
  public void resetStatistics() throws WasmException {
    ensureNotClosed();

    final boolean success = NATIVE_BINDINGS.poolingAllocatorResetStatistics(nativeAllocator);

    if (!success) {
      throw new WasmException("Failed to reset pool statistics");
    }
  }

  @Override
  public void warmPools() throws WasmException {
    ensureNotClosed();

    final boolean success = NATIVE_BINDINGS.poolingAllocatorWarmPools(nativeAllocator);

    if (!success) {
      throw new WasmException("Failed to warm pools");
    }
  }

  @Override
  public void performMaintenance() throws WasmException {
    ensureNotClosed();

    final boolean success = NATIVE_BINDINGS.poolingAllocatorPerformMaintenance(nativeAllocator);

    if (!success) {
      throw new WasmException("Failed to perform pool maintenance");
    }
  }

  @Override
  public void configureEngine(final EngineConfig engineConfig) throws WasmException {
    if (engineConfig == null) {
      throw new IllegalArgumentException("engineConfig cannot be null");
    }
    ensureNotClosed();

    // Set pooling allocator configuration on the engine config
    engineConfig.setPoolingAllocatorEnabled(true);
    engineConfig.setInstancePoolSize(config.getInstancePoolSize());
    engineConfig.setMaxMemoryPerInstance(config.getMaxMemoryPerInstance());

    LOGGER.fine("Configured engine to use pooling allocator");
  }

  @Override
  public Duration getUptime() {
    return Duration.between(createdAt, Instant.now());
  }

  @Override
  public boolean isValid() {
    return !closed && nativeAllocator != null && !nativeAllocator.equals(MemorySegment.NULL);
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    if (nativeAllocator != null && !nativeAllocator.equals(MemorySegment.NULL)) {
      NATIVE_BINDINGS.poolingAllocatorDestroy(nativeAllocator);
    }

    arena.close();
    LOGGER.fine("Closed Panama pooling allocator");
  }

  /**
   * Gets the native allocator pointer for internal use.
   *
   * @return the native allocator pointer
   */
  public MemorySegment getNativeAllocator() {
    return nativeAllocator;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Pooling allocator has been closed");
    }
  }
}
