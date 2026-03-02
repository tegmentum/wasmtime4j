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
package ai.tegmentum.wasmtime4j.panama.pool;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeExecutionBindings;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
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
  private static final NativeExecutionBindings NATIVE_BINDINGS =
      NativeExecutionBindings.getInstance();

  private final PoolingAllocatorConfig config;
  private final Arena arena;
  private final MemorySegment nativeAllocator;
  private final Instant createdAt;
  private final NativeResourceHandle resourceHandle;

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

    // Create native allocator using JSON config path — all fields are wired
    final byte[] jsonConfig =
        (config instanceof ai.tegmentum.wasmtime4j.pool.AbstractPoolingAllocatorConfig)
            ? ((ai.tegmentum.wasmtime4j.pool.AbstractPoolingAllocatorConfig) config).toJson()
            : null;

    if (jsonConfig != null) {
      this.nativeAllocator = NATIVE_BINDINGS.poolingAllocatorCreateFromJson(jsonConfig);
    } else {
      // Fallback: legacy positional parameter path
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
    }

    if (this.nativeAllocator == null || this.nativeAllocator.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native pooling allocator");
    }

    // Capture local references for the safety net (must not capture 'this')
    final MemorySegment allocatorForCleanup = this.nativeAllocator;
    final Arena arenaForCleanup = this.arena;

    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaPoolingAllocator",
            () -> {
              if (allocatorForCleanup != null && !allocatorForCleanup.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.poolingAllocatorDestroy(allocatorForCleanup);
              }

              arenaForCleanup.close();
              LOGGER.fine("Closed Panama pooling allocator");
            },
            this,
            () -> {
              if (allocatorForCleanup != null && !allocatorForCleanup.equals(MemorySegment.NULL)) {
                NATIVE_BINDINGS.poolingAllocatorDestroy(allocatorForCleanup);
              }
              arenaForCleanup.close();
            });

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
      // Allocate space for 12 i64 values (12 * 8 = 96 bytes)
      final MemorySegment statsOut = localArena.allocate(ValueLayout.JAVA_LONG, 12);

      final boolean success =
          NATIVE_BINDINGS.poolingAllocatorGetStatistics(nativeAllocator, statsOut);

      if (!success) {
        LOGGER.warning("Failed to get pool statistics, returning empty statistics");
        return new PanamaPoolStatistics();
      }

      // Read the 12 metrics values
      final long[] metrics = new long[12];
      for (int i = 0; i < 12; i++) {
        metrics[i] = statsOut.getAtIndex(ValueLayout.JAVA_LONG, i);
      }

      return new PanamaPoolStatistics(metrics);
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
    return !resourceHandle.isClosed()
        && nativeAllocator != null
        && !nativeAllocator.equals(MemorySegment.NULL);
  }

  @Override
  public void close() {
    resourceHandle.close();
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
    resourceHandle.ensureNotClosed();
  }
}
