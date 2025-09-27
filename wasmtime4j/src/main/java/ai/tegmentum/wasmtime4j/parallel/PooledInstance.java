package ai.tegmentum.wasmtime4j.parallel;

import ai.tegmentum.wasmtime4j.WasmInstance;
import ai.tegmentum.wasmtime4j.exception.WasmException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A WebAssembly instance managed by an instance pool.
 *
 * <p>Provides additional metadata and lifecycle management for instances
 * that are part of a pool, including usage tracking and health monitoring.
 *
 * @since 1.0.0
 */
public final class PooledInstance implements AutoCloseable {

  private final WasmInstance instance;
  private final String instanceId;
  private final Instant createdAt;
  private final Duration maxIdleTime;
  private volatile Instant lastUsed;
  private volatile boolean isInUse;
  private volatile boolean isHealthy;
  private volatile long useCount;

  /**
   * Creates a new pooled instance.
   *
   * @param instance the wrapped WebAssembly instance
   * @param instanceId unique identifier for this instance
   * @param maxIdleTime maximum time the instance can remain idle
   */
  public PooledInstance(final WasmInstance instance,
                        final String instanceId,
                        final Duration maxIdleTime) {
    this.instance = Objects.requireNonNull(instance);
    this.instanceId = Objects.requireNonNull(instanceId);
    this.maxIdleTime = Objects.requireNonNull(maxIdleTime);
    this.createdAt = Instant.now();
    this.lastUsed = this.createdAt;
    this.isInUse = false;
    this.isHealthy = true;
    this.useCount = 0;
  }

  /**
   * Gets the underlying WebAssembly instance.
   *
   * @return the WebAssembly instance
   */
  public WasmInstance getInstance() {
    return instance;
  }

  /**
   * Gets the unique instance identifier.
   *
   * @return instance ID
   */
  public String getInstanceId() {
    return instanceId;
  }

  /**
   * Gets when this instance was created.
   *
   * @return creation timestamp
   */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Gets when this instance was last used.
   *
   * @return last used timestamp
   */
  public Instant getLastUsed() {
    return lastUsed;
  }

  /**
   * Checks if this instance is currently in use.
   *
   * @return true if in use
   */
  public boolean isInUse() {
    return isInUse;
  }

  /**
   * Checks if this instance is healthy.
   *
   * @return true if healthy
   */
  public boolean isHealthy() {
    return isHealthy;
  }

  /**
   * Gets the total number of times this instance has been used.
   *
   * @return use count
   */
  public long getUseCount() {
    return useCount;
  }

  /**
   * Gets the maximum idle time for this instance.
   *
   * @return maximum idle time
   */
  public Duration getMaxIdleTime() {
    return maxIdleTime;
  }

  /**
   * Checks if this instance has exceeded its maximum idle time.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    final Duration idleTime = Duration.between(lastUsed, Instant.now());
    return idleTime.compareTo(maxIdleTime) > 0;
  }

  /**
   * Marks this instance as in use.
   *
   * @throws WasmException if the instance is already in use or unhealthy
   */
  public synchronized void markInUse() throws WasmException {
    if (isInUse) {
      throw new WasmException("Instance is already in use: " + instanceId);
    }
    if (!isHealthy) {
      throw new WasmException("Instance is not healthy: " + instanceId);
    }
    isInUse = true;
    lastUsed = Instant.now();
    useCount++;
  }

  /**
   * Marks this instance as no longer in use.
   */
  public synchronized void markNotInUse() {
    isInUse = false;
    lastUsed = Instant.now();
  }

  /**
   * Marks this instance as unhealthy.
   *
   * @param reason the reason for marking unhealthy
   */
  public synchronized void markUnhealthy(final String reason) {
    isHealthy = false;
    isInUse = false;
  }

  /**
   * Performs a health check on this instance.
   *
   * @return true if the instance is healthy
   */
  public boolean performHealthCheck() {
    try {
      // Basic health check - verify instance is still accessible
      if (instance == null) {
        markUnhealthy("Instance is null");
        return false;
      }

      // Instance is healthy if we reach here
      return isHealthy;
    } catch (final Exception e) {
      markUnhealthy("Health check failed: " + e.getMessage());
      return false;
    }
  }

  @Override
  public void close() throws Exception {
    if (instance != null) {
      instance.close();
    }
  }

  @Override
  public String toString() {
    return String.format("PooledInstance{id='%s', inUse=%s, healthy=%s, useCount=%d}",
        instanceId, isInUse, isHealthy, useCount);
  }
}