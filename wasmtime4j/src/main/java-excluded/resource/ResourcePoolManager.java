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

package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Advanced resource pool management system providing dynamic resource pooling,
 * reservation, allocation optimization, and lifecycle management.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Dynamic resource pool sizing with load-based scaling
 *   <li>Resource reservation and advance booking system
 *   <li>Resource sharing and optimization across tenants
 *   <li>Health monitoring and automatic replacement
 *   <li>Resource lifecycle management with aging
 *   <li>Performance optimization and waste reduction
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourcePoolManager {

  private static final Logger LOGGER = Logger.getLogger(ResourcePoolManager.class.getName());

  /** Resource pool configuration. */
  public static final class PoolConfiguration {
    private final ResourceQuotaManager.ResourceType resourceType;
    private final int minSize;
    private final int maxSize;
    private final int initialSize;
    private final Duration maxIdleTime;
    private final Duration maxLifetime;
    private final Duration healthCheckInterval;
    private final boolean allowSharing;
    private final double scalingFactor;
    private final int scalingThreshold;

    private PoolConfiguration(final Builder builder) {
      this.resourceType = builder.resourceType;
      this.minSize = builder.minSize;
      this.maxSize = builder.maxSize;
      this.initialSize = builder.initialSize;
      this.maxIdleTime = builder.maxIdleTime;
      this.maxLifetime = builder.maxLifetime;
      this.healthCheckInterval = builder.healthCheckInterval;
      this.allowSharing = builder.allowSharing;
      this.scalingFactor = builder.scalingFactor;
      this.scalingThreshold = builder.scalingThreshold;
    }

    public static Builder builder(final ResourceQuotaManager.ResourceType resourceType) {
      return new Builder(resourceType);
    }

    public static final class Builder {
      private final ResourceQuotaManager.ResourceType resourceType;
      private int minSize = 1;
      private int maxSize = 10;
      private int initialSize = 2;
      private Duration maxIdleTime = Duration.ofMinutes(30);
      private Duration maxLifetime = Duration.ofHours(2);
      private Duration healthCheckInterval = Duration.ofMinutes(5);
      private boolean allowSharing = true;
      private double scalingFactor = 1.5;
      private int scalingThreshold = 80; // percentage

      private Builder(final ResourceQuotaManager.ResourceType resourceType) {
        this.resourceType = resourceType;
      }

      public Builder withMinSize(final int minSize) {
        this.minSize = minSize;
        return this;
      }

      public Builder withMaxSize(final int maxSize) {
        this.maxSize = maxSize;
        return this;
      }

      public Builder withInitialSize(final int initialSize) {
        this.initialSize = initialSize;
        return this;
      }

      public Builder withMaxIdleTime(final Duration maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
        return this;
      }

      public Builder withMaxLifetime(final Duration maxLifetime) {
        this.maxLifetime = maxLifetime;
        return this;
      }

      public Builder withHealthCheckInterval(final Duration healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
        return this;
      }

      public Builder withSharing(final boolean allowSharing) {
        this.allowSharing = allowSharing;
        return this;
      }

      public Builder withScalingFactor(final double scalingFactor) {
        this.scalingFactor = scalingFactor;
        return this;
      }

      public Builder withScalingThreshold(final int scalingThreshold) {
        this.scalingThreshold = scalingThreshold;
        return this;
      }

      public PoolConfiguration build() {
        return new PoolConfiguration(this);
      }
    }

    // Getters
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public int getMinSize() { return minSize; }
    public int getMaxSize() { return maxSize; }
    public int getInitialSize() { return initialSize; }
    public Duration getMaxIdleTime() { return maxIdleTime; }
    public Duration getMaxLifetime() { return maxLifetime; }
    public Duration getHealthCheckInterval() { return healthCheckInterval; }
    public boolean isAllowSharing() { return allowSharing; }
    public double getScalingFactor() { return scalingFactor; }
    public int getScalingThreshold() { return scalingThreshold; }
  }

  /** Pooled resource wrapper. */
  public static final class PooledResource<T> {
    private final String resourceId;
    private final T resource;
    private final Instant creationTime;
    private final AtomicReference<Instant> lastUsed = new AtomicReference<>(Instant.now());
    private final AtomicBoolean inUse = new AtomicBoolean(false);
    private final AtomicBoolean healthy = new AtomicBoolean(true);
    private final AtomicLong usageCount = new AtomicLong(0);
    private final String tenantId;

    public PooledResource(final String resourceId, final T resource, final String tenantId) {
      this.resourceId = resourceId;
      this.resource = resource;
      this.tenantId = tenantId;
      this.creationTime = Instant.now();
    }

    public String getResourceId() { return resourceId; }
    public T getResource() { return resource; }
    public Instant getCreationTime() { return creationTime; }
    public Instant getLastUsed() { return lastUsed.get(); }
    public boolean isInUse() { return inUse.get(); }
    public boolean isHealthy() { return healthy.get(); }
    public long getUsageCount() { return usageCount.get(); }
    public String getTenantId() { return tenantId; }

    public Duration getAge() {
      return Duration.between(creationTime, Instant.now());
    }

    public Duration getIdleTime() {
      return Duration.between(lastUsed.get(), Instant.now());
    }

    boolean tryAcquire(final String tenantId) {
      if (inUse.compareAndSet(false, true)) {
        lastUsed.set(Instant.now());
        usageCount.incrementAndGet();
        return true;
      }
      return false;
    }

    void release() {
      lastUsed.set(Instant.now());
      inUse.set(false);
    }

    void markUnhealthy() {
      healthy.set(false);
    }

    void markHealthy() {
      healthy.set(true);
    }
  }

  /** Resource pool implementation. */
  private static final class ResourcePool<T> {
    private final PoolConfiguration config;
    private final Supplier<T> resourceFactory;
    private final ResourceHealthChecker<T> healthChecker;
    private final ConcurrentLinkedQueue<PooledResource<T>> availableResources = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, PooledResource<T>> allResources = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock poolLock = new ReentrantReadWriteLock();

    private final AtomicLong totalCreated = new AtomicLong(0);
    private final AtomicLong totalDestroyed = new AtomicLong(0);
    private final AtomicLong totalAcquisitions = new AtomicLong(0);
    private final AtomicLong totalReleases = new AtomicLong(0);
    private final AtomicLong healthCheckFailures = new AtomicLong(0);

    private volatile boolean shutdown = false;

    ResourcePool(final PoolConfiguration config, final Supplier<T> resourceFactory, final ResourceHealthChecker<T> healthChecker) {
      this.config = config;
      this.resourceFactory = resourceFactory;
      this.healthChecker = healthChecker;
      initializePool();
    }

    private void initializePool() {
      for (int i = 0; i < config.getInitialSize(); i++) {
        final PooledResource<T> resource = createResource();
        if (resource != null) {
          availableResources.offer(resource);
          allResources.put(resource.getResourceId(), resource);
        }
      }
      LOGGER.info(String.format("Initialized resource pool for %s with %d resources",
          config.getResourceType(), availableResources.size()));
    }

    public PooledResource<T> acquire(final String tenantId, final Duration maxWaitTime) {
      if (shutdown) {
        return null;
      }

      totalAcquisitions.incrementAndGet();
      final Instant deadline = Instant.now().plus(maxWaitTime);

      while (Instant.now().isBefore(deadline)) {
        // Try to get an available resource
        PooledResource<T> resource = null;

        if (config.isAllowSharing()) {
          // Try to find a resource for this tenant or any shareable resource
          resource = findAvailableResource(tenantId);
        } else {
          // Try to get any available resource
          resource = availableResources.poll();
        }

        if (resource != null && resource.isHealthy() && resource.tryAcquire(tenantId)) {
          return resource;
        }

        // If no available resource, try to create one
        if (canCreateMore()) {
          resource = createResource();
          if (resource != null && resource.tryAcquire(tenantId)) {
            allResources.put(resource.getResourceId(), resource);
            return resource;
          }
        }

        // Wait briefly before retrying
        try {
          Thread.sleep(50);
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }
      }

      return null; // Timeout or interruption
    }

    public void release(final PooledResource<T> resource) {
      if (resource == null) {
        return;
      }

      totalReleases.incrementAndGet();
      resource.release();

      // Return to available pool if healthy and not over-sized
      if (resource.isHealthy() && !isOverSized()) {
        availableResources.offer(resource);
      } else {
        // Remove unhealthy or excess resources
        destroyResource(resource);
      }
    }

    public void performMaintenance() {
      if (shutdown) {
        return;
      }

      poolLock.writeLock().lock();
      try {
        // Remove aged resources
        removeAgedResources();

        // Remove idle resources if over min size
        removeIdleResources();

        // Health check all resources
        performHealthChecks();

        // Scale pool if needed
        scalePoolIfNeeded();

      } finally {
        poolLock.writeLock().unlock();
      }
    }

    private PooledResource<T> findAvailableResource(final String tenantId) {
      // First try to find a resource already used by this tenant
      for (final PooledResource<T> resource : availableResources) {
        if (tenantId.equals(resource.getTenantId()) && resource.isHealthy()) {
          availableResources.remove(resource);
          return resource;
        }
      }

      // Then try any available resource
      return availableResources.poll();
    }

    private boolean canCreateMore() {
      return allResources.size() < config.getMaxSize();
    }

    private boolean isOverSized() {
      return allResources.size() > config.getMaxSize();
    }

    private PooledResource<T> createResource() {
      try {
        final T resource = resourceFactory.get();
        if (resource != null) {
          final String resourceId = "resource-" + totalCreated.incrementAndGet();
          final PooledResource<T> pooledResource = new PooledResource<>(resourceId, resource, null);
          LOGGER.fine(String.format("Created resource %s for pool %s", resourceId, config.getResourceType()));
          return pooledResource;
        }
      } catch (final Exception e) {
        LOGGER.warning(String.format("Failed to create resource for pool %s: %s", config.getResourceType(), e.getMessage()));
      }
      return null;
    }

    private void destroyResource(final PooledResource<T> resource) {
      allResources.remove(resource.getResourceId());
      availableResources.remove(resource);
      totalDestroyed.incrementAndGet();

      try {
        // Call resource-specific cleanup if available
        if (resource.getResource() instanceof AutoCloseable) {
          ((AutoCloseable) resource.getResource()).close();
        }
      } catch (final Exception e) {
        LOGGER.warning(String.format("Error destroying resource %s: %s", resource.getResourceId(), e.getMessage()));
      }

      LOGGER.fine(String.format("Destroyed resource %s from pool %s", resource.getResourceId(), config.getResourceType()));
    }

    private void removeAgedResources() {
      final Instant ageLimit = Instant.now().minus(config.getMaxLifetime());

      allResources.values().removeIf(resource -> {
        if (!resource.isInUse() && resource.getCreationTime().isBefore(ageLimit)) {
          destroyResource(resource);
          return true;
        }
        return false;
      });
    }

    private void removeIdleResources() {
      if (allResources.size() <= config.getMinSize()) {
        return;
      }

      final Instant idleLimit = Instant.now().minus(config.getMaxIdleTime());

      availableResources.removeIf(resource -> {
        if (resource.getLastUsed().isBefore(idleLimit) && allResources.size() > config.getMinSize()) {
          destroyResource(resource);
          return true;
        }
        return false;
      });
    }

    private void performHealthChecks() {
      if (healthChecker == null) {
        return;
      }

      for (final PooledResource<T> resource : allResources.values()) {
        if (!resource.isInUse()) {
          try {
            final boolean healthy = healthChecker.isHealthy(resource.getResource());
            if (!healthy) {
              resource.markUnhealthy();
              healthCheckFailures.incrementAndGet();
              LOGGER.warning(String.format("Resource %s failed health check", resource.getResourceId()));
            } else {
              resource.markHealthy();
            }
          } catch (final Exception e) {
            resource.markUnhealthy();
            healthCheckFailures.incrementAndGet();
            LOGGER.warning(String.format("Health check error for resource %s: %s", resource.getResourceId(), e.getMessage()));
          }
        }
      }

      // Remove unhealthy resources
      allResources.values().removeIf(resource -> {
        if (!resource.isHealthy() && !resource.isInUse()) {
          destroyResource(resource);
          return true;
        }
        return false;
      });
    }

    private void scalePoolIfNeeded() {
      final int currentSize = allResources.size();
      final int availableSize = availableResources.size();
      final int utilizationPercentage = currentSize > 0 ? ((currentSize - availableSize) * 100) / currentSize : 0;

      if (utilizationPercentage > config.getScalingThreshold() && currentSize < config.getMaxSize()) {
        // Scale up
        final int targetSize = Math.min(config.getMaxSize(), (int) (currentSize * config.getScalingFactor()));
        final int resourcesToCreate = targetSize - currentSize;

        for (int i = 0; i < resourcesToCreate; i++) {
          final PooledResource<T> resource = createResource();
          if (resource != null) {
            availableResources.offer(resource);
            allResources.put(resource.getResourceId(), resource);
          }
        }

        LOGGER.info(String.format("Scaled pool %s up from %d to %d resources (utilization: %d%%)",
            config.getResourceType(), currentSize, allResources.size(), utilizationPercentage));

      } else if (utilizationPercentage < (config.getScalingThreshold() / 2) && currentSize > config.getMinSize()) {
        // Scale down
        final int targetSize = Math.max(config.getMinSize(), currentSize / 2);
        final int resourcesToRemove = currentSize - targetSize;

        int removed = 0;
        for (final PooledResource<T> resource : availableResources) {
          if (removed >= resourcesToRemove) {
            break;
          }
          if (!resource.isInUse()) {
            destroyResource(resource);
            removed++;
          }
        }

        if (removed > 0) {
          LOGGER.info(String.format("Scaled pool %s down by %d resources (utilization: %d%%)",
              config.getResourceType(), removed, utilizationPercentage));
        }
      }
    }

    public PoolStatistics getStatistics() {
      return new PoolStatistics(
          config.getResourceType(),
          allResources.size(),
          availableResources.size(),
          allResources.size() - availableResources.size(),
          totalCreated.get(),
          totalDestroyed.get(),
          totalAcquisitions.get(),
          totalReleases.get(),
          healthCheckFailures.get()
      );
    }

    public void shutdown() {
      shutdown = true;
      poolLock.writeLock().lock();
      try {
        // Destroy all resources
        for (final PooledResource<T> resource : allResources.values()) {
          destroyResource(resource);
        }
        availableResources.clear();
        allResources.clear();
      } finally {
        poolLock.writeLock().unlock();
      }
    }
  }

  /** Resource health checker interface. */
  public interface ResourceHealthChecker<T> {
    boolean isHealthy(T resource) throws Exception;
  }

  /** Pool statistics. */
  public static final class PoolStatistics {
    private final ResourceQuotaManager.ResourceType resourceType;
    private final int totalResources;
    private final int availableResources;
    private final int inUseResources;
    private final long totalCreated;
    private final long totalDestroyed;
    private final long totalAcquisitions;
    private final long totalReleases;
    private final long healthCheckFailures;

    public PoolStatistics(final ResourceQuotaManager.ResourceType resourceType, final int totalResources,
                          final int availableResources, final int inUseResources, final long totalCreated,
                          final long totalDestroyed, final long totalAcquisitions, final long totalReleases,
                          final long healthCheckFailures) {
      this.resourceType = resourceType;
      this.totalResources = totalResources;
      this.availableResources = availableResources;
      this.inUseResources = inUseResources;
      this.totalCreated = totalCreated;
      this.totalDestroyed = totalDestroyed;
      this.totalAcquisitions = totalAcquisitions;
      this.totalReleases = totalReleases;
      this.healthCheckFailures = healthCheckFailures;
    }

    // Getters
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public int getTotalResources() { return totalResources; }
    public int getAvailableResources() { return availableResources; }
    public int getInUseResources() { return inUseResources; }
    public long getTotalCreated() { return totalCreated; }
    public long getTotalDestroyed() { return totalDestroyed; }
    public long getTotalAcquisitions() { return totalAcquisitions; }
    public long getTotalReleases() { return totalReleases; }
    public long getHealthCheckFailures() { return healthCheckFailures; }

    public double getUtilizationPercentage() {
      return totalResources > 0 ? (inUseResources * 100.0) / totalResources : 0.0;
    }

    public double getHitRate() {
      return totalAcquisitions > 0 ? ((totalAcquisitions - totalCreated) * 100.0) / totalAcquisitions : 0.0;
    }
  }

  // Instance fields
  private final ConcurrentHashMap<ResourceQuotaManager.ResourceType, ResourcePool<?>> pools = new ConcurrentHashMap<>();
  private final ScheduledExecutorService maintenanceExecutor = Executors.newScheduledThreadPool(2);
  private volatile boolean enabled = true;

  public ResourcePoolManager() {
    startMaintenanceTasks();
    LOGGER.info("Resource pool manager initialized");
  }

  /**
   * Creates a resource pool with the specified configuration.
   *
   * @param config pool configuration
   * @param resourceFactory factory for creating resources
   * @param healthChecker health checker for resources
   * @param <T> resource type
   */
  public <T> void createPool(final PoolConfiguration config, final Supplier<T> resourceFactory,
                           final ResourceHealthChecker<T> healthChecker) {
    if (!enabled) {
      return;
    }

    final ResourcePool<T> pool = new ResourcePool<>(config, resourceFactory, healthChecker);
    pools.put(config.getResourceType(), pool);

    LOGGER.info(String.format("Created resource pool for %s with config: min=%d, max=%d, initial=%d",
        config.getResourceType(), config.getMinSize(), config.getMaxSize(), config.getInitialSize()));
  }

  /**
   * Acquires a resource from the specified pool.
   *
   * @param resourceType resource type
   * @param tenantId tenant identifier
   * @param maxWaitTime maximum wait time
   * @param <T> resource type
   * @return pooled resource or null if not available
   */
  @SuppressWarnings("unchecked")
  public <T> PooledResource<T> acquire(final ResourceQuotaManager.ResourceType resourceType,
                                       final String tenantId, final Duration maxWaitTime) {
    if (!enabled) {
      return null;
    }

    final ResourcePool<T> pool = (ResourcePool<T>) pools.get(resourceType);
    if (pool == null) {
      LOGGER.warning(String.format("No pool configured for resource type %s", resourceType));
      return null;
    }

    return pool.acquire(tenantId, maxWaitTime);
  }

  /**
   * Releases a resource back to its pool.
   *
   * @param resource pooled resource to release
   * @param <T> resource type
   */
  @SuppressWarnings("unchecked")
  public <T> void release(final PooledResource<T> resource) {
    if (resource == null) {
      return;
    }

    // Find the appropriate pool - this could be optimized by storing pool reference in PooledResource
    for (final ResourcePool<?> pool : pools.values()) {
      if (pool.allResources.containsKey(resource.getResourceId())) {
        ((ResourcePool<T>) pool).release(resource);
        return;
      }
    }

    LOGGER.warning(String.format("Could not find pool for resource %s", resource.getResourceId()));
  }

  /**
   * Gets statistics for a specific pool.
   *
   * @param resourceType resource type
   * @return pool statistics or null if pool doesn't exist
   */
  public PoolStatistics getPoolStatistics(final ResourceQuotaManager.ResourceType resourceType) {
    final ResourcePool<?> pool = pools.get(resourceType);
    return pool != null ? pool.getStatistics() : null;
  }

  /**
   * Gets comprehensive statistics for all pools.
   *
   * @return formatted statistics
   */
  public String getAllStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Pool Manager Statistics ===\n");

    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Total pools: %d\n", pools.size()));
    sb.append("\n");

    for (final Map.Entry<ResourceQuotaManager.ResourceType, ResourcePool<?>> entry : pools.entrySet()) {
      final PoolStatistics stats = entry.getValue().getStatistics();
      sb.append(String.format("Pool %s:\n", stats.getResourceType()));
      sb.append(String.format("  Total resources: %d\n", stats.getTotalResources()));
      sb.append(String.format("  Available: %d\n", stats.getAvailableResources()));
      sb.append(String.format("  In use: %d\n", stats.getInUseResources()));
      sb.append(String.format("  Utilization: %.1f%%\n", stats.getUtilizationPercentage()));
      sb.append(String.format("  Hit rate: %.1f%%\n", stats.getHitRate()));
      sb.append(String.format("  Total created: %,d\n", stats.getTotalCreated()));
      sb.append(String.format("  Total destroyed: %,d\n", stats.getTotalDestroyed()));
      sb.append(String.format("  Health check failures: %,d\n", stats.getHealthCheckFailures()));
      sb.append("\n");
    }

    return sb.toString();
  }

  private void startMaintenanceTasks() {
    // Pool maintenance
    maintenanceExecutor.scheduleAtFixedRate(this::performMaintenance, 60, 60, TimeUnit.SECONDS);

    // Statistics logging
    maintenanceExecutor.scheduleAtFixedRate(this::logStatistics, 300, 300, TimeUnit.SECONDS);
  }

  private void performMaintenance() {
    if (!enabled) {
      return;
    }

    for (final ResourcePool<?> pool : pools.values()) {
      try {
        pool.performMaintenance();
      } catch (final Exception e) {
        LOGGER.warning(String.format("Error during pool maintenance: %s", e.getMessage()));
      }
    }
  }

  private void logStatistics() {
    for (final Map.Entry<ResourceQuotaManager.ResourceType, ResourcePool<?>> entry : pools.entrySet()) {
      final PoolStatistics stats = entry.getValue().getStatistics();
      LOGGER.info(String.format("Pool %s: total=%d, available=%d, utilization=%.1f%%, hit_rate=%.1f%%",
          stats.getResourceType(), stats.getTotalResources(), stats.getAvailableResources(),
          stats.getUtilizationPercentage(), stats.getHitRate()));
    }
  }

  /**
   * Removes a resource pool.
   *
   * @param resourceType resource type
   */
  public void removePool(final ResourceQuotaManager.ResourceType resourceType) {
    final ResourcePool<?> pool = pools.remove(resourceType);
    if (pool != null) {
      pool.shutdown();
      LOGGER.info(String.format("Removed resource pool for %s", resourceType));
    }
  }

  /**
   * Enables or disables the pool manager.
   *
   * @param enabled true to enable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource pool manager " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Gets the number of configured pools.
   *
   * @return number of pools
   */
  public int getPoolCount() {
    return pools.size();
  }

  /**
   * Shuts down the pool manager and all pools.
   */
  public void shutdown() {
    enabled = false;

    maintenanceExecutor.shutdown();
    try {
      if (!maintenanceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        maintenanceExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      maintenanceExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    // Shutdown all pools
    for (final ResourcePool<?> pool : pools.values()) {
      pool.shutdown();
    }
    pools.clear();

    LOGGER.info("Resource pool manager shutdown");
  }
}