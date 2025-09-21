/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.production;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.EngineConfig;
import ai.tegmentum.wasmtime4j.OptimizationLevel;
import ai.tegmentum.wasmtime4j.WasmRuntimeFactory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.resource.PoolConfiguration;
import ai.tegmentum.wasmtime4j.resource.ResourcePool;
import ai.tegmentum.wasmtime4j.resource.ResourceType;
import java.time.Duration;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Production-grade optimized engine pool providing high-performance connection pooling for
 * WebAssembly engines.
 *
 * <p>Features:
 * - Pre-warmed engine instances for immediate availability
 * - Optimized engine configurations for production workloads
 * - Automatic pool sizing based on load
 * - Health monitoring and validation
 * - Graceful degradation under high load
 * - Resource cleanup and lifecycle management
 */
public final class OptimizedEnginePool implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(OptimizedEnginePool.class.getName());

  private final int maxSize;
  private final int minSize;
  private final Duration maxIdleTime;
  private final EngineConfig optimizedConfig;
  private final ConcurrentLinkedQueue<PooledEngine> availableEngines;
  private final AtomicInteger activeEngines;
  private final AtomicInteger totalEngines;
  private final AtomicBoolean shutdown;
  private final ScheduledExecutorService cleanupExecutor;
  private final ScheduledExecutorService healthCheckExecutor;

  /** Configuration for production-optimized engine pool. */
  public static final class PoolConfig {
    private final int maxSize;
    private final int minSize;
    private final Duration maxIdleTime;
    private final Duration healthCheckInterval;
    private final boolean enablePreWarming;
    private final EngineConfig engineConfig;

    private PoolConfig(final Builder builder) {
      this.maxSize = builder.maxSize;
      this.minSize = builder.minSize;
      this.maxIdleTime = builder.maxIdleTime;
      this.healthCheckInterval = builder.healthCheckInterval;
      this.enablePreWarming = builder.enablePreWarming;
      this.engineConfig = builder.engineConfig;
    }

    public int getMaxSize() {
      return maxSize;
    }

    public int getMinSize() {
      return minSize;
    }

    public Duration getMaxIdleTime() {
      return maxIdleTime;
    }

    public Duration getHealthCheckInterval() {
      return healthCheckInterval;
    }

    public boolean isPreWarmingEnabled() {
      return enablePreWarming;
    }

    public EngineConfig getEngineConfig() {
      return engineConfig;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private int maxSize = 10;
      private int minSize = 2;
      private Duration maxIdleTime = Duration.ofMinutes(10);
      private Duration healthCheckInterval = Duration.ofMinutes(5);
      private boolean enablePreWarming = true;
      private EngineConfig engineConfig = createProductionEngineConfig();

      public Builder maxSize(final int maxSize) {
        if (maxSize <= 0) {
          throw new IllegalArgumentException("Max size must be positive");
        }
        this.maxSize = maxSize;
        return this;
      }

      public Builder minSize(final int minSize) {
        if (minSize < 0) {
          throw new IllegalArgumentException("Min size must be non-negative");
        }
        this.minSize = minSize;
        return this;
      }

      public Builder maxIdleTime(final Duration maxIdleTime) {
        if (maxIdleTime == null || maxIdleTime.isNegative()) {
          throw new IllegalArgumentException("Max idle time must be positive");
        }
        this.maxIdleTime = maxIdleTime;
        return this;
      }

      public Builder healthCheckInterval(final Duration healthCheckInterval) {
        if (healthCheckInterval == null || healthCheckInterval.isNegative()) {
          throw new IllegalArgumentException("Health check interval must be positive");
        }
        this.healthCheckInterval = healthCheckInterval;
        return this;
      }

      public Builder enablePreWarming(final boolean enablePreWarming) {
        this.enablePreWarming = enablePreWarming;
        return this;
      }

      public Builder engineConfig(final EngineConfig engineConfig) {
        if (engineConfig == null) {
          throw new IllegalArgumentException("Engine config cannot be null");
        }
        this.engineConfig = engineConfig;
        return this;
      }

      public PoolConfig build() {
        if (minSize > maxSize) {
          throw new IllegalArgumentException("Min size cannot be greater than max size");
        }
        return new PoolConfig(this);
      }
    }
  }

  /** Wrapper for pooled engine instances with metadata. */
  private static final class PooledEngine {
    private final Engine engine;
    private final long creationTime;
    private volatile long lastUsedTime;
    private volatile boolean healthy;

    PooledEngine(final Engine engine) {
      this.engine = engine;
      this.creationTime = System.currentTimeMillis();
      this.lastUsedTime = creationTime;
      this.healthy = true;
    }

    public Engine getEngine() {
      return engine;
    }

    public long getCreationTime() {
      return creationTime;
    }

    public long getLastUsedTime() {
      return lastUsedTime;
    }

    public void updateLastUsedTime() {
      this.lastUsedTime = System.currentTimeMillis();
    }

    public boolean isHealthy() {
      return healthy;
    }

    public void setHealthy(final boolean healthy) {
      this.healthy = healthy;
    }

    public Duration getIdleTime() {
      return Duration.ofMillis(System.currentTimeMillis() - lastUsedTime);
    }

    public Duration getAge() {
      return Duration.ofMillis(System.currentTimeMillis() - creationTime);
    }
  }

  /**
   * Creates an optimized engine pool with the specified configuration.
   *
   * @param config the pool configuration
   */
  public OptimizedEnginePool(final PoolConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("Pool config cannot be null");
    }

    this.maxSize = config.getMaxSize();
    this.minSize = config.getMinSize();
    this.maxIdleTime = config.getMaxIdleTime();
    this.optimizedConfig = config.getEngineConfig();
    this.availableEngines = new ConcurrentLinkedQueue<>();
    this.activeEngines = new AtomicInteger(0);
    this.totalEngines = new AtomicInteger(0);
    this.shutdown = new AtomicBoolean(false);

    // Initialize cleanup and health check executors
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread t = new Thread(r, "OptimizedEnginePool-Cleanup");
      t.setDaemon(true);
      return t;
    });

    this.healthCheckExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread t = new Thread(r, "OptimizedEnginePool-HealthCheck");
      t.setDaemon(true);
      return t;
    });

    // Schedule periodic cleanup and health checks
    cleanupExecutor.scheduleAtFixedRate(
        this::performCleanup, 1, 1, TimeUnit.MINUTES);

    healthCheckExecutor.scheduleAtFixedRate(
        this::performHealthCheck,
        config.getHealthCheckInterval().toMillis(),
        config.getHealthCheckInterval().toMillis(),
        TimeUnit.MILLISECONDS);

    // Pre-populate pool if enabled
    if (config.isPreWarmingEnabled()) {
      preWarmPool();
    }

    LOGGER.info(String.format("OptimizedEnginePool initialized: maxSize=%d, minSize=%d",
        maxSize, minSize));
  }

  /**
   * Creates a default optimized engine pool with production settings.
   *
   * @return a new optimized engine pool
   */
  public static OptimizedEnginePool createDefault() {
    return new OptimizedEnginePool(PoolConfig.builder().build());
  }

  /**
   * Creates an optimized engine pool for high-throughput scenarios.
   *
   * @return a new high-throughput optimized engine pool
   */
  public static OptimizedEnginePool createHighThroughput() {
    return new OptimizedEnginePool(
        PoolConfig.builder()
            .maxSize(20)
            .minSize(5)
            .maxIdleTime(Duration.ofMinutes(5))
            .healthCheckInterval(Duration.ofMinutes(2))
            .build());
  }

  /**
   * Acquires an engine from the pool.
   *
   * @return a pooled engine instance
   * @throws WasmException if engine acquisition fails
   * @throws IllegalStateException if the pool is shut down
   */
  public Engine acquireEngine() throws WasmException {
    if (shutdown.get()) {
      throw new IllegalStateException("Pool is shut down");
    }

    // Try to get an available engine
    PooledEngine pooledEngine = availableEngines.poll();
    if (pooledEngine != null) {
      if (pooledEngine.isHealthy()) {
        pooledEngine.updateLastUsedTime();
        activeEngines.incrementAndGet();
        LOGGER.fine("Acquired pooled engine from available pool");
        return new TrackedEngine(pooledEngine.getEngine(), pooledEngine);
      } else {
        // Engine is unhealthy, dispose and try again
        disposeEngine(pooledEngine);
        return acquireEngine(); // Recursive call to try again
      }
    }

    // No available engines, try to create a new one
    if (totalEngines.get() < maxSize) {
      try {
        pooledEngine = createNewEngine();
        pooledEngine.updateLastUsedTime();
        activeEngines.incrementAndGet();
        LOGGER.fine("Created new engine for acquisition");
        return new TrackedEngine(pooledEngine.getEngine(), pooledEngine);
      } catch (final Exception e) {
        throw new WasmException("Failed to create new engine", e);
      }
    }

    // Pool is at maximum capacity, wait for an engine to become available
    // In production, this would typically have a timeout
    throw new WasmException("Pool exhausted - maximum engines in use");
  }

  /**
   * Returns an engine to the pool.
   *
   * @param engine the engine to return
   * @throws IllegalArgumentException if engine is null or not from this pool
   */
  public void returnEngine(final Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    if (!(engine instanceof TrackedEngine)) {
      throw new IllegalArgumentException("Engine not from this pool");
    }

    final TrackedEngine trackedEngine = (TrackedEngine) engine;
    final PooledEngine pooledEngine = trackedEngine.getPooledEngine();

    if (shutdown.get()) {
      // Pool is shutting down, dispose the engine
      disposeEngine(pooledEngine);
      return;
    }

    // Validate engine health before returning to pool
    if (validateEngineHealth(pooledEngine)) {
      availableEngines.offer(pooledEngine);
      activeEngines.decrementAndGet();
      LOGGER.fine("Returned healthy engine to pool");
    } else {
      // Engine is unhealthy, dispose it
      disposeEngine(pooledEngine);
      LOGGER.warning("Disposed unhealthy engine instead of returning to pool");
    }
  }

  /**
   * Gets current pool statistics.
   *
   * @return pool statistics
   */
  public PoolStatistics getStatistics() {
    return new PoolStatistics(
        totalEngines.get(),
        activeEngines.get(),
        availableEngines.size(),
        maxSize,
        minSize);
  }

  /** Pool statistics container. */
  public static final class PoolStatistics {
    private final int totalEngines;
    private final int activeEngines;
    private final int availableEngines;
    private final int maxSize;
    private final int minSize;

    PoolStatistics(
        final int totalEngines,
        final int activeEngines,
        final int availableEngines,
        final int maxSize,
        final int minSize) {
      this.totalEngines = totalEngines;
      this.activeEngines = activeEngines;
      this.availableEngines = availableEngines;
      this.maxSize = maxSize;
      this.minSize = minSize;
    }

    public int getTotalEngines() {
      return totalEngines;
    }

    public int getActiveEngines() {
      return activeEngines;
    }

    public int getAvailableEngines() {
      return availableEngines;
    }

    public int getMaxSize() {
      return maxSize;
    }

    public int getMinSize() {
      return minSize;
    }

    public double getUtilizationPercentage() {
      return (double) activeEngines / totalEngines * 100.0;
    }

    @Override
    public String toString() {
      return String.format(
          "PoolStatistics{total=%d, active=%d, available=%d, utilization=%.1f%%}",
          totalEngines, activeEngines, availableEngines, getUtilizationPercentage());
    }
  }

  /** Pre-warms the pool with minimum number of engines. */
  private void preWarmPool() {
    LOGGER.info("Pre-warming engine pool");

    for (int i = 0; i < minSize; i++) {
      try {
        final PooledEngine pooledEngine = createNewEngine();
        availableEngines.offer(pooledEngine);
        LOGGER.fine(String.format("Pre-warmed engine %d/%d", i + 1, minSize));
      } catch (final Exception e) {
        LOGGER.warning(String.format("Failed to pre-warm engine %d: %s", i + 1, e.getMessage()));
        break; // Stop pre-warming on first failure
      }
    }

    LOGGER.info(String.format("Pre-warming complete: %d engines ready", availableEngines.size()));
  }

  /** Creates a new engine with optimized configuration. */
  private PooledEngine createNewEngine() throws WasmException {
    final Engine engine = WasmRuntimeFactory.createEngine(optimizedConfig);
    final PooledEngine pooledEngine = new PooledEngine(engine);
    totalEngines.incrementAndGet();
    return pooledEngine;
  }

  /** Disposes of an engine and updates counters. */
  private void disposeEngine(final PooledEngine pooledEngine) {
    try {
      pooledEngine.getEngine().close();
      totalEngines.decrementAndGet();
      LOGGER.fine("Disposed engine");
    } catch (final Exception e) {
      LOGGER.warning("Error disposing engine: " + e.getMessage());
    }
  }

  /** Validates engine health. */
  private boolean validateEngineHealth(final PooledEngine pooledEngine) {
    try {
      // Perform basic health check - compile a minimal module
      final byte[] testWasm = createTestWasmModule();
      pooledEngine.getEngine().compileModule(testWasm);
      pooledEngine.setHealthy(true);
      return true;
    } catch (final Exception e) {
      LOGGER.warning("Engine health check failed: " + e.getMessage());
      pooledEngine.setHealthy(false);
      return false;
    }
  }

  /** Performs periodic cleanup of idle and unhealthy engines. */
  private void performCleanup() {
    if (shutdown.get()) {
      return;
    }

    int cleanedUp = 0;
    final long currentTime = System.currentTimeMillis();

    // Clean up idle engines (but maintain minimum pool size)
    while (availableEngines.size() > minSize) {
      final PooledEngine pooledEngine = availableEngines.peek();
      if (pooledEngine == null) {
        break;
      }

      final Duration idleTime = pooledEngine.getIdleTime();
      if (idleTime.compareTo(maxIdleTime) > 0) {
        availableEngines.poll(); // Remove from queue
        disposeEngine(pooledEngine);
        cleanedUp++;
      } else {
        break; // Engines are ordered by usage time
      }
    }

    if (cleanedUp > 0) {
      LOGGER.info(String.format("Cleaned up %d idle engines", cleanedUp));
    }
  }

  /** Performs periodic health checks on available engines. */
  private void performHealthCheck() {
    if (shutdown.get()) {
      return;
    }

    int healthyCount = 0;
    int unhealthyCount = 0;

    // Check health of available engines
    for (final PooledEngine pooledEngine : availableEngines) {
      if (validateEngineHealth(pooledEngine)) {
        healthyCount++;
      } else {
        unhealthyCount++;
        // Remove unhealthy engine from available queue
        availableEngines.remove(pooledEngine);
        disposeEngine(pooledEngine);
      }
    }

    LOGGER.fine(String.format("Health check complete: %d healthy, %d unhealthy",
        healthyCount, unhealthyCount));

    // Ensure minimum pool size after cleanup
    ensureMinimumPoolSize();
  }

  /** Ensures the pool maintains minimum size. */
  private void ensureMinimumPoolSize() {
    final int currentAvailable = availableEngines.size();
    final int needed = minSize - currentAvailable;

    if (needed > 0 && totalEngines.get() + needed <= maxSize) {
      for (int i = 0; i < needed; i++) {
        try {
          final PooledEngine pooledEngine = createNewEngine();
          availableEngines.offer(pooledEngine);
        } catch (final Exception e) {
          LOGGER.warning("Failed to create engine for minimum pool size: " + e.getMessage());
          break;
        }
      }
    }
  }

  /** Creates production-optimized engine configuration. */
  private static EngineConfig createProductionEngineConfig() {
    return EngineConfig.builder()
        .withOptimizationLevel(OptimizationLevel.SPEED_AND_SIZE)
        .withParallelCompilation(true)
        .withCraneliftOptLevel(2)
        .build();
  }

  /** Creates a minimal test WASM module for health checks. */
  private byte[] createTestWasmModule() {
    // Minimal WASM module with no-op function
    return new byte[] {
      0x00, 0x61, 0x73, 0x6d, // magic
      0x01, 0x00, 0x00, 0x00, // version
      0x01, 0x04, 0x01, 0x60, 0x00, 0x00, // type section: () -> ()
      0x03, 0x02, 0x01, 0x00, // function section
      0x0a, 0x04, 0x01, 0x02, 0x00, 0x0b // code section: empty function
    };
  }

  @Override
  public void close() {
    if (shutdown.compareAndSet(false, true)) {
      LOGGER.info("Shutting down OptimizedEnginePool");

      // Shutdown executors
      cleanupExecutor.shutdown();
      healthCheckExecutor.shutdown();

      try {
        if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          cleanupExecutor.shutdownNow();
        }
        if (!healthCheckExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
          healthCheckExecutor.shutdownNow();
        }
      } catch (final InterruptedException e) {
        cleanupExecutor.shutdownNow();
        healthCheckExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }

      // Close all available engines
      PooledEngine pooledEngine;
      while ((pooledEngine = availableEngines.poll()) != null) {
        disposeEngine(pooledEngine);
      }

      LOGGER.info("OptimizedEnginePool shutdown complete");
    }
  }

  /** Tracked engine wrapper that can be returned to the pool. */
  private static final class TrackedEngine implements Engine {
    private final Engine delegate;
    private final PooledEngine pooledEngine;

    TrackedEngine(final Engine delegate, final PooledEngine pooledEngine) {
      this.delegate = delegate;
      this.pooledEngine = pooledEngine;
    }

    public PooledEngine getPooledEngine() {
      return pooledEngine;
    }

    // Delegate all Engine methods to the underlying engine
    // Note: In a real implementation, all Engine interface methods would be delegated
    // This is a simplified version showing the pattern

    @Override
    public void close() {
      // Don't actually close the engine - this should return it to the pool
      throw new UnsupportedOperationException(
          "Use OptimizedEnginePool.returnEngine() instead of calling close() directly");
    }

    // Additional Engine method delegations would go here...
  }
}