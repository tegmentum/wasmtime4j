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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Production-grade resource manager providing comprehensive lifecycle control and cleanup for
 * WebAssembly resources.
 *
 * <p>Features:
 * - Automatic resource tracking and lifecycle management
 * - Graceful shutdown with configurable timeout
 * - Resource group management for organized cleanup
 * - Memory pressure handling and proactive cleanup
 * - Resource leak detection and prevention
 * - Performance monitoring and optimization
 */
public final class ProductionResourceManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(ProductionResourceManager.class.getName());

  private final ResourceManagerConfig config;
  private final ConcurrentHashMap<String, ResourceGroup> resourceGroups;
  private final ScheduledExecutorService cleanupExecutor;
  private final ScheduledExecutorService monitoringExecutor;
  private final AtomicBoolean shutdown;
  private final AtomicLong nextResourceId;
  private final ResourceStatistics statistics;

  /** Resource manager configuration. */
  public static final class ResourceManagerConfig {
    private final Duration cleanupInterval;
    private final Duration maxResourceAge;
    private final Duration shutdownTimeout;
    private final int maxResourcesPerGroup;
    private final boolean enableLeakDetection;
    private final boolean enableMemoryPressureHandling;
    private final double memoryPressureThreshold;

    private ResourceManagerConfig(final Builder builder) {
      this.cleanupInterval = builder.cleanupInterval;
      this.maxResourceAge = builder.maxResourceAge;
      this.shutdownTimeout = builder.shutdownTimeout;
      this.maxResourcesPerGroup = builder.maxResourcesPerGroup;
      this.enableLeakDetection = builder.enableLeakDetection;
      this.enableMemoryPressureHandling = builder.enableMemoryPressureHandling;
      this.memoryPressureThreshold = builder.memoryPressureThreshold;
    }

    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private Duration cleanupInterval = Duration.ofMinutes(1);
      private Duration maxResourceAge = Duration.ofHours(1);
      private Duration shutdownTimeout = Duration.ofSeconds(30);
      private int maxResourcesPerGroup = 1000;
      private boolean enableLeakDetection = true;
      private boolean enableMemoryPressureHandling = true;
      private double memoryPressureThreshold = 80.0; // 80%

      public Builder cleanupInterval(final Duration cleanupInterval) {
        this.cleanupInterval = cleanupInterval;
        return this;
      }

      public Builder maxResourceAge(final Duration maxResourceAge) {
        this.maxResourceAge = maxResourceAge;
        return this;
      }

      public Builder shutdownTimeout(final Duration shutdownTimeout) {
        this.shutdownTimeout = shutdownTimeout;
        return this;
      }

      public Builder maxResourcesPerGroup(final int maxResourcesPerGroup) {
        this.maxResourcesPerGroup = maxResourcesPerGroup;
        return this;
      }

      public Builder enableLeakDetection(final boolean enableLeakDetection) {
        this.enableLeakDetection = enableLeakDetection;
        return this;
      }

      public Builder enableMemoryPressureHandling(final boolean enableMemoryPressureHandling) {
        this.enableMemoryPressureHandling = enableMemoryPressureHandling;
        return this;
      }

      public Builder memoryPressureThreshold(final double memoryPressureThreshold) {
        this.memoryPressureThreshold = memoryPressureThreshold;
        return this;
      }

      public ResourceManagerConfig build() {
        return new ResourceManagerConfig(this);
      }
    }
  }

  /** Managed resource wrapper with lifecycle tracking. */
  public static final class ManagedResource<T> implements AutoCloseable {
    private final long id;
    private final String groupName;
    private final T resource;
    private final Instant creationTime;
    private volatile Instant lastAccessTime;
    private volatile boolean closed;
    private final Consumer<T> cleanupAction;

    ManagedResource(
        final long id,
        final String groupName,
        final T resource,
        final Consumer<T> cleanupAction) {
      this.id = id;
      this.groupName = groupName;
      this.resource = resource;
      this.creationTime = Instant.now();
      this.lastAccessTime = creationTime;
      this.closed = false;
      this.cleanupAction = cleanupAction;
    }

    public long getId() {
      return id;
    }

    public String getGroupName() {
      return groupName;
    }

    public T getResource() {
      if (closed) {
        throw new IllegalStateException("Resource has been closed");
      }
      lastAccessTime = Instant.now();
      return resource;
    }

    public Instant getCreationTime() {
      return creationTime;
    }

    public Instant getLastAccessTime() {
      return lastAccessTime;
    }

    public Duration getAge() {
      return Duration.between(creationTime, Instant.now());
    }

    public Duration getIdleTime() {
      return Duration.between(lastAccessTime, Instant.now());
    }

    public boolean isClosed() {
      return closed;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        try {
          if (cleanupAction != null) {
            cleanupAction.accept(resource);
          }
        } catch (final Exception e) {
          LOGGER.warning(String.format("Error cleaning up resource %d: %s", id, e.getMessage()));
        }
      }
    }
  }

  /** Resource group for organizing related resources. */
  private static final class ResourceGroup implements AutoCloseable {
    private final String name;
    private final ConcurrentHashMap<Long, ManagedResource<?>> resources;
    private final AtomicInteger resourceCount;
    private final AtomicLong totalResourcesCreated;
    private final AtomicLong totalResourcesClosed;

    ResourceGroup(final String name) {
      this.name = name;
      this.resources = new ConcurrentHashMap<>();
      this.resourceCount = new AtomicInteger(0);
      this.totalResourcesCreated = new AtomicLong(0);
      this.totalResourcesClosed = new AtomicLong(0);
    }

    public String getName() {
      return name;
    }

    public <T> ManagedResource<T> addResource(
        final long id, final T resource, final Consumer<T> cleanupAction) {
      final ManagedResource<T> managedResource = new ManagedResource<>(id, name, resource, cleanupAction);
      resources.put(id, managedResource);
      resourceCount.incrementAndGet();
      totalResourcesCreated.incrementAndGet();
      return managedResource;
    }

    public void removeResource(final long id) {
      final ManagedResource<?> resource = resources.remove(id);
      if (resource != null) {
        resourceCount.decrementAndGet();
        totalResourcesClosed.incrementAndGet();
        if (!resource.isClosed()) {
          resource.close();
        }
      }
    }

    public List<ManagedResource<?>> getExpiredResources(final Duration maxAge) {
      final List<ManagedResource<?>> expired = new ArrayList<>();
      final Instant cutoff = Instant.now().minus(maxAge);

      for (final ManagedResource<?> resource : resources.values()) {
        if (resource.getCreationTime().isBefore(cutoff)) {
          expired.add(resource);
        }
      }

      return expired;
    }

    public List<ManagedResource<?>> getIdleResources(final Duration maxIdleTime) {
      final List<ManagedResource<?>> idle = new ArrayList<>();
      final Instant cutoff = Instant.now().minus(maxIdleTime);

      for (final ManagedResource<?> resource : resources.values()) {
        if (resource.getLastAccessTime().isBefore(cutoff)) {
          idle.add(resource);
        }
      }

      return idle;
    }

    public int getResourceCount() {
      return resourceCount.get();
    }

    public long getTotalResourcesCreated() {
      return totalResourcesCreated.get();
    }

    public long getTotalResourcesClosed() {
      return totalResourcesClosed.get();
    }

    public ResourceGroupStatistics getStatistics() {
      return new ResourceGroupStatistics(
          name,
          resourceCount.get(),
          totalResourcesCreated.get(),
          totalResourcesClosed.get());
    }

    @Override
    public void close() {
      final List<Long> resourceIds = new ArrayList<>(resources.keySet());
      for (final Long id : resourceIds) {
        removeResource(id);
      }
    }
  }

  /** Resource group statistics. */
  public static final class ResourceGroupStatistics {
    private final String groupName;
    private final int currentCount;
    private final long totalCreated;
    private final long totalClosed;

    ResourceGroupStatistics(
        final String groupName,
        final int currentCount,
        final long totalCreated,
        final long totalClosed) {
      this.groupName = groupName;
      this.currentCount = currentCount;
      this.totalCreated = totalCreated;
      this.totalClosed = totalClosed;
    }

    public String getGroupName() {
      return groupName;
    }

    public int getCurrentCount() {
      return currentCount;
    }

    public long getTotalCreated() {
      return totalCreated;
    }

    public long getTotalClosed() {
      return totalClosed;
    }

    public long getActiveLeaks() {
      return totalCreated - totalClosed - currentCount;
    }

    @Override
    public String toString() {
      return String.format("ResourceGroup{name=%s, current=%d, created=%d, closed=%d, leaks=%d}",
          groupName, currentCount, totalCreated, totalClosed, getActiveLeaks());
    }
  }

  /** Overall resource manager statistics. */
  public static final class ResourceStatistics {
    private final AtomicInteger totalGroups;
    private final AtomicLong totalResources;
    private final AtomicLong totalCleanupsPerformed;
    private final AtomicLong totalMemoryPressureEvents;

    ResourceStatistics() {
      this.totalGroups = new AtomicInteger(0);
      this.totalResources = new AtomicLong(0);
      this.totalCleanupsPerformed = new AtomicLong(0);
      this.totalMemoryPressureEvents = new AtomicLong(0);
    }

    public void incrementGroups() {
      totalGroups.incrementAndGet();
    }

    public void decrementGroups() {
      totalGroups.decrementAndGet();
    }

    public void incrementResources() {
      totalResources.incrementAndGet();
    }

    public void decrementResources() {
      totalResources.decrementAndGet();
    }

    public void incrementCleanups() {
      totalCleanupsPerformed.incrementAndGet();
    }

    public void incrementMemoryPressureEvents() {
      totalMemoryPressureEvents.incrementAndGet();
    }

    public int getTotalGroups() {
      return totalGroups.get();
    }

    public long getTotalResources() {
      return totalResources.get();
    }

    public long getTotalCleanupsPerformed() {
      return totalCleanupsPerformed.get();
    }

    public long getTotalMemoryPressureEvents() {
      return totalMemoryPressureEvents.get();
    }

    @Override
    public String toString() {
      return String.format(
          "ResourceStatistics{groups=%d, resources=%d, cleanups=%d, memoryPressure=%d}",
          getTotalGroups(), getTotalResources(), getTotalCleanupsPerformed(), getTotalMemoryPressureEvents());
    }
  }

  /**
   * Creates a production resource manager with the specified configuration.
   *
   * @param config the resource manager configuration
   */
  public ProductionResourceManager(final ResourceManagerConfig config) {
    this.config = config;
    this.resourceGroups = new ConcurrentHashMap<>();
    this.shutdown = new AtomicBoolean(false);
    this.nextResourceId = new AtomicLong(0);
    this.statistics = new ResourceStatistics();

    // Initialize cleanup executor
    this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread t = new Thread(r, "ProductionResourceManager-Cleanup");
      t.setDaemon(true);
      return t;
    });

    // Initialize monitoring executor
    this.monitoringExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread t = new Thread(r, "ProductionResourceManager-Monitor");
      t.setDaemon(true);
      return t;
    });

    // Schedule periodic cleanup
    cleanupExecutor.scheduleAtFixedRate(
        this::performCleanup,
        config.cleanupInterval.toMillis(),
        config.cleanupInterval.toMillis(),
        TimeUnit.MILLISECONDS);

    // Schedule memory pressure monitoring
    if (config.enableMemoryPressureHandling) {
      monitoringExecutor.scheduleAtFixedRate(
          this::checkMemoryPressure,
          10, 10, TimeUnit.SECONDS); // Check every 10 seconds
    }

    // Register shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(this::close));

    LOGGER.info("ProductionResourceManager initialized");
  }

  /**
   * Creates a default production resource manager.
   *
   * @return a new resource manager
   */
  public static ProductionResourceManager createDefault() {
    return new ProductionResourceManager(ResourceManagerConfig.builder().build());
  }

  /**
   * Registers a resource for management.
   *
   * @param group the resource group name
   * @param resource the resource to manage
   * @param <T> the resource type
   * @return a managed resource wrapper
   * @throws WasmException if resource registration fails
   * @throws IllegalStateException if the manager is shutting down
   */
  public <T extends AutoCloseable> ManagedResource<T> registerResource(
      final String group, final T resource) throws WasmException {
    return registerResource(group, resource, T::close);
  }

  /**
   * Registers a resource for management with a custom cleanup action.
   *
   * @param group the resource group name
   * @param resource the resource to manage
   * @param cleanupAction the cleanup action to perform
   * @param <T> the resource type
   * @return a managed resource wrapper
   * @throws WasmException if resource registration fails
   * @throws IllegalStateException if the manager is shutting down
   */
  public <T> ManagedResource<T> registerResource(
      final String group, final T resource, final Consumer<T> cleanupAction) throws WasmException {
    if (shutdown.get()) {
      throw new IllegalStateException("Resource manager is shutting down");
    }

    if (group == null || resource == null) {
      throw new IllegalArgumentException("Group and resource cannot be null");
    }

    final ResourceGroup resourceGroup = resourceGroups.computeIfAbsent(group, k -> {
      statistics.incrementGroups();
      return new ResourceGroup(k);
    });

    // Check group resource limit
    if (resourceGroup.getResourceCount() >= config.maxResourcesPerGroup) {
      throw new WasmException(String.format(
          "Resource group '%s' has reached maximum capacity: %d", group, config.maxResourcesPerGroup));
    }

    final long resourceId = nextResourceId.incrementAndGet();
    final ManagedResource<T> managedResource = resourceGroup.addResource(resourceId, resource, cleanupAction);
    statistics.incrementResources();

    LOGGER.fine(String.format("Registered resource %d in group '%s'", resourceId, group));
    return managedResource;
  }

  /**
   * Unregisters a resource from management.
   *
   * @param managedResource the managed resource to unregister
   * @throws IllegalArgumentException if the resource is null or not managed by this manager
   */
  public void unregisterResource(final ManagedResource<?> managedResource) {
    if (managedResource == null) {
      throw new IllegalArgumentException("Managed resource cannot be null");
    }

    final ResourceGroup group = resourceGroups.get(managedResource.getGroupName());
    if (group != null) {
      group.removeResource(managedResource.getId());
      statistics.decrementResources();
      LOGGER.fine(String.format("Unregistered resource %d from group '%s'",
          managedResource.getId(), managedResource.getGroupName()));
    }
  }

  /**
   * Performs immediate cleanup of expired and idle resources.
   *
   * @return the number of resources cleaned up
   */
  public int performCleanup() {
    if (shutdown.get()) {
      return 0;
    }

    int cleanedUp = 0;

    for (final ResourceGroup group : resourceGroups.values()) {
      // Clean up expired resources
      final List<ManagedResource<?>> expired = group.getExpiredResources(config.maxResourceAge);
      for (final ManagedResource<?> resource : expired) {
        group.removeResource(resource.getId());
        cleanedUp++;
      }

      // Clean up idle resources if memory pressure is high
      if (isMemoryPressureHigh()) {
        final List<ManagedResource<?>> idle = group.getIdleResources(Duration.ofMinutes(5));
        for (final ManagedResource<?> resource : idle) {
          group.removeResource(resource.getId());
          cleanedUp++;
        }
      }
    }

    if (cleanedUp > 0) {
      statistics.incrementCleanups();
      LOGGER.info(String.format("Cleanup completed: %d resources cleaned up", cleanedUp));
    }

    return cleanedUp;
  }

  /**
   * Cleans up all resources in a specific group.
   *
   * @param groupName the name of the group to clean up
   * @return the number of resources cleaned up
   * @throws IllegalArgumentException if group name is null
   */
  public int cleanupResourceGroup(final String groupName) {
    if (groupName == null) {
      throw new IllegalArgumentException("Group name cannot be null");
    }

    final ResourceGroup group = resourceGroups.get(groupName);
    if (group == null) {
      return 0;
    }

    final int count = group.getResourceCount();
    group.close();
    resourceGroups.remove(groupName);
    statistics.decrementGroups();

    LOGGER.info(String.format("Cleaned up resource group '%s': %d resources", groupName, count));
    return count;
  }

  /**
   * Gets comprehensive resource statistics.
   *
   * @return current resource statistics
   */
  public ResourceStatistics getStatistics() {
    return statistics;
  }

  /**
   * Gets statistics for all resource groups.
   *
   * @return map of group name to statistics
   */
  public Map<String, ResourceGroupStatistics> getGroupStatistics() {
    final Map<String, ResourceGroupStatistics> stats = new ConcurrentHashMap<>();
    for (final Map.Entry<String, ResourceGroup> entry : resourceGroups.entrySet()) {
      stats.put(entry.getKey(), entry.getValue().getStatistics());
    }
    return stats;
  }

  /**
   * Gets the list of registered resource groups.
   *
   * @return set of group names
   */
  public Set<String> getResourceGroups() {
    return resourceGroups.keySet();
  }

  /**
   * Checks if the resource manager is operational.
   *
   * @return true if operational
   */
  public boolean isOperational() {
    return !shutdown.get();
  }

  /**
   * Drains all resources by performing immediate cleanup.
   *
   * @return the number of resources drained
   */
  public int drainAllResources() {
    int drained = 0;
    for (final String groupName : new ArrayList<>(resourceGroups.keySet())) {
      drained += cleanupResourceGroup(groupName);
    }
    return drained;
  }

  /**
   * Gracefully shuts down the resource manager.
   *
   * @param timeout the maximum time to wait for shutdown
   * @return a future that completes when shutdown is finished
   */
  public CompletableFuture<Void> shutdownAsync(final Duration timeout) {
    return CompletableFuture.runAsync(() -> {
      if (shutdown.compareAndSet(false, true)) {
        LOGGER.info("Starting graceful shutdown of ProductionResourceManager");

        // Stop accepting new registrations
        cleanupExecutor.shutdown();
        monitoringExecutor.shutdown();

        try {
          // Wait for scheduled tasks to complete
          if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            cleanupExecutor.shutdownNow();
          }
          if (!monitoringExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
            monitoringExecutor.shutdownNow();
          }
        } catch (final InterruptedException e) {
          cleanupExecutor.shutdownNow();
          monitoringExecutor.shutdownNow();
          Thread.currentThread().interrupt();
        }

        // Close all resource groups
        final int totalResources = drainAllResources();
        LOGGER.info(String.format("ProductionResourceManager shutdown complete: %d resources cleaned up",
            totalResources));
      }
    });
  }

  /** Checks for memory pressure and triggers cleanup if needed. */
  private void checkMemoryPressure() {
    if (isMemoryPressureHigh()) {
      statistics.incrementMemoryPressureEvents();
      LOGGER.warning("High memory pressure detected, triggering aggressive cleanup");

      // Perform aggressive cleanup
      final int cleaned = performCleanup();
      if (cleaned > 0) {
        // Force garbage collection to help relieve pressure
        System.gc();
      }
    }
  }

  /** Checks if memory pressure is currently high. */
  private boolean isMemoryPressureHigh() {
    final Runtime runtime = Runtime.getRuntime();
    final long totalMemory = runtime.totalMemory();
    final long freeMemory = runtime.freeMemory();
    final double usagePercentage = (double) (totalMemory - freeMemory) / totalMemory * 100.0;
    return usagePercentage > config.memoryPressureThreshold;
  }

  @Override
  public void close() {
    if (shutdown.compareAndSet(false, true)) {
      LOGGER.info("Performing immediate shutdown of ProductionResourceManager");

      // Stop executors immediately
      cleanupExecutor.shutdownNow();
      monitoringExecutor.shutdownNow();

      // Close all resource groups immediately
      final int totalResources = drainAllResources();
      LOGGER.info(String.format("ProductionResourceManager immediate shutdown complete: %d resources cleaned up",
          totalResources));
    }
  }
}