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
package ai.tegmentum.wasmtime4j.jni.pool;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.pool.PoolStatistics;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocator;
import ai.tegmentum.wasmtime4j.pool.PoolingAllocatorConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * JNI implementation of {@link PoolingAllocator}.
 *
 * <p>This implementation provides high-performance pooling allocation for WebAssembly execution by
 * reusing pre-allocated memory pools for instances, stacks, and tables.
 *
 * @since 1.0.0
 */
public final class JniPoolingAllocator implements PoolingAllocator {

  private static final Logger LOGGER = Logger.getLogger(JniPoolingAllocator.class.getName());

  static {
    NativeLibraryLoader.loadLibrary();
  }

  private final PoolingAllocatorConfig config;
  private final long nativeHandle;
  private final Instant createdAt;
  private volatile boolean closed = false;

  /**
   * Creates a new JniPoolingAllocator with the specified configuration.
   *
   * @param config the allocator configuration
   * @throws WasmException if the allocator cannot be created
   */
  public JniPoolingAllocator(final PoolingAllocatorConfig config) throws WasmException {
    if (config == null) {
      throw new IllegalArgumentException("config cannot be null");
    }
    this.config = config;
    this.createdAt = Instant.now();

    // Create native allocator with configuration
    this.nativeHandle =
        nativeCreateWithConfig(
            config.getInstancePoolSize(),
            config.getMaxMemoryPerInstance(),
            config.getStackSize(),
            config.getMaxStacks(),
            config.getMaxTablesPerInstance(),
            config.getMaxTables(),
            config.isMemoryDecommitEnabled(),
            config.isPoolWarmingEnabled(),
            config.getPoolWarmingPercentage());

    if (this.nativeHandle == 0) {
      throw new WasmException("Failed to create native pooling allocator");
    }

    LOGGER.fine("Created JNI pooling allocator with config: " + config);
  }

  @Override
  public PoolingAllocatorConfig getConfig() {
    return config;
  }

  @Override
  public long allocateInstance() throws WasmException {
    ensureNotClosed();

    final long instanceId = nativeAllocateInstance(nativeHandle);
    if (instanceId < 0) {
      throw new WasmException("Failed to allocate instance from pool");
    }

    return instanceId;
  }

  @Override
  public void reuseInstance(final long instanceId) throws WasmException {
    ensureNotClosed();

    final boolean success = nativeReuseInstance(nativeHandle, instanceId);
    if (!success) {
      throw new WasmException("Failed to reuse instance: " + instanceId);
    }
  }

  @Override
  public void releaseInstance(final long instanceId) throws WasmException {
    ensureNotClosed();

    final boolean success = nativeReleaseInstance(nativeHandle, instanceId);
    if (!success) {
      throw new WasmException("Failed to release instance: " + instanceId);
    }
  }

  @Override
  public PoolStatistics getStatistics() {
    ensureNotClosed();

    // Statistics returned as a 12-element array matching PoolingAllocatorMetrics fields
    final long[] stats = nativeGetStatistics(nativeHandle);
    if (stats == null || stats.length < 12) {
      LOGGER.warning("Failed to get pool statistics, returning empty statistics");
      return new JniPoolStatistics();
    }

    return new JniPoolStatistics(stats);
  }

  @Override
  public void resetStatistics() throws WasmException {
    ensureNotClosed();

    final boolean success = nativeResetStatistics(nativeHandle);
    if (!success) {
      throw new WasmException("Failed to reset pool statistics");
    }
  }

  @Override
  public void warmPools() throws WasmException {
    ensureNotClosed();

    final boolean success = nativeWarmPools(nativeHandle);
    if (!success) {
      throw new WasmException("Failed to warm pools");
    }
  }

  @Override
  public void performMaintenance() throws WasmException {
    ensureNotClosed();

    final boolean success = nativePerformMaintenance(nativeHandle);
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
    return !closed && nativeHandle != 0;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;

    if (nativeHandle != 0) {
      nativeDestroy(nativeHandle);
    }

    LOGGER.fine("Closed JNI pooling allocator");
  }

  /**
   * Gets the native handle for internal use.
   *
   * @return the native handle
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Pooling allocator has been closed");
    }
  }

  // Native methods

  private static native long nativeCreateWithConfig(
      int instancePoolSize,
      long maxMemoryPerInstance,
      int stackSize,
      int maxStacks,
      int maxTablesPerInstance,
      int maxTables,
      boolean memoryDecommitEnabled,
      boolean poolWarmingEnabled,
      float poolWarmingPercentage);

  private static native long nativeAllocateInstance(long allocatorHandle);

  private static native boolean nativeReuseInstance(long allocatorHandle, long instanceId);

  private static native boolean nativeReleaseInstance(long allocatorHandle, long instanceId);

  private static native long[] nativeGetStatistics(long allocatorHandle);

  private static native boolean nativeResetStatistics(long allocatorHandle);

  private static native boolean nativeWarmPools(long allocatorHandle);

  private static native boolean nativePerformMaintenance(long allocatorHandle);

  private static native void nativeDestroy(long allocatorHandle);
}
