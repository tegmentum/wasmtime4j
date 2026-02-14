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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Resource manager for WebAssembly component resources.
 *
 * <p>This class provides comprehensive resource management for WIT resources, including lifecycle
 * tracking, automatic cleanup, reference counting, and garbage collection integration.
 *
 * @since 1.0.0
 */
@SuppressFBWarnings(
    value = "REC_CATCH_EXCEPTION",
    justification =
        "Broad exception catching for defensive resource cleanup;"
            + " ensures cleanup completes even with unexpected failures")
public final class WitResourceManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WitResourceManager.class.getName());

  private static final int DEFAULT_CLEANUP_INTERVAL_SECONDS = 30;
  private static final int DEFAULT_MAX_RESOURCES = 10000;

  private final String managerId;
  private final AtomicInteger nextResourceId;
  private final AtomicLong totalResourcesCreated;
  private final AtomicLong totalResourcesDestroyed;

  private final Map<Integer, ManagedResource> activeResources;
  private final Map<String, ResourceTypeInfo> resourceTypes;
  private final ReferenceQueue<Object> phantomQueue;
  private final Set<ResourcePhantomReference> phantomReferences;

  private final ScheduledExecutorService cleanupExecutor;
  private final ResourceConfig config;

  private volatile boolean closed = false;

  /** Creates a new WIT resource manager with default configuration. */
  public WitResourceManager() {
    this(new ResourceConfig());
  }

  /**
   * Creates a new WIT resource manager with custom configuration.
   *
   * @param config the resource configuration
   */
  public WitResourceManager(final ResourceConfig config) {
    Objects.requireNonNull(config, "config");

    this.managerId = "wit-resource-manager-" + System.nanoTime();
    this.nextResourceId = new AtomicInteger(1);
    this.totalResourcesCreated = new AtomicLong(0);
    this.totalResourcesDestroyed = new AtomicLong(0);

    this.activeResources = new ConcurrentHashMap<>();
    this.resourceTypes = new ConcurrentHashMap<>();
    this.phantomQueue = new ReferenceQueue<>();
    this.phantomReferences = Collections.newSetFromMap(new ConcurrentHashMap<>());

    this.config = config;
    this.cleanupExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              final Thread t = new Thread(r, "WitResourceManager-Cleanup");
              t.setDaemon(true);
              return t;
            });

    // Start cleanup task
    cleanupExecutor.scheduleAtFixedRate(
        this::performCleanup,
        config.getCleanupIntervalSeconds(),
        config.getCleanupIntervalSeconds(),
        TimeUnit.SECONDS);

    LOGGER.fine("Created WIT resource manager: " + managerId);
  }

  /**
   * Registers a resource type with the manager.
   *
   * @param typeName the resource type name
   * @param typeInfo the resource type information
   */
  public void registerResourceType(final String typeName, final ResourceTypeInfo typeInfo) {
    Objects.requireNonNull(typeName, "typeName");
    Objects.requireNonNull(typeInfo, "typeInfo");

    resourceTypes.put(typeName, typeInfo);
    LOGGER.fine("Registered resource type: " + typeName);
  }

  /**
   * Creates a new managed resource.
   *
   * @param typeName the resource type name
   * @param resource the resource object
   * @return the resource handle
   * @throws WasmException if resource creation fails
   */
  public int createResource(final String typeName, final Object resource) throws WasmException {
    Objects.requireNonNull(typeName, "typeName");
    Objects.requireNonNull(resource, "resource");
    ensureNotClosed();

    // Check resource limit
    if (activeResources.size() >= config.getMaxResources()) {
      throw new WasmException("Maximum resource limit reached: " + config.getMaxResources());
    }

    final ResourceTypeInfo typeInfo = resourceTypes.get(typeName);
    if (typeInfo == null) {
      throw new WasmException("Unknown resource type: " + typeName);
    }

    final int resourceId = nextResourceId.getAndIncrement();
    final long createdAt = System.currentTimeMillis();

    final ManagedResource managedResource =
        new ManagedResource(resourceId, typeName, resource, createdAt, typeInfo);

    activeResources.put(resourceId, managedResource);

    // Create phantom reference for cleanup
    final ResourcePhantomReference phantomRef =
        new ResourcePhantomReference(resource, phantomQueue, resourceId, typeName);
    phantomReferences.add(phantomRef);

    totalResourcesCreated.incrementAndGet();

    LOGGER.fine("Created resource " + resourceId + " of type " + typeName);
    return resourceId;
  }

  /**
   * Gets a managed resource by handle.
   *
   * @param resourceId the resource handle
   * @return the managed resource
   * @throws WasmException if resource not found
   */
  public ManagedResource getResource(final int resourceId) throws WasmException {
    ensureNotClosed();

    final ManagedResource resource = activeResources.get(resourceId);
    if (resource == null) {
      throw new WasmException("Resource not found: " + resourceId);
    }

    // Update last accessed time
    resource.updateLastAccessed();
    return resource;
  }

  /**
   * Gets the actual resource object by handle.
   *
   * @param resourceId the resource handle
   * @param expectedType the expected resource type
   * @param <T> the resource type
   * @return the resource object
   * @throws WasmException if resource not found or wrong type
   */
  @SuppressWarnings("unchecked")
  public <T> T getResourceValue(final int resourceId, final Class<T> expectedType)
      throws WasmException {
    final ManagedResource managedResource = getResource(resourceId);
    final Object resource = managedResource.getResource();

    if (!expectedType.isInstance(resource)) {
      throw new WasmException(
          "Resource " + resourceId + " is not of expected type: " + expectedType.getName());
    }

    return (T) resource;
  }

  /**
   * Increments the reference count for a resource.
   *
   * @param resourceId the resource handle
   * @throws WasmException if resource not found
   */
  public void incrementRefCount(final int resourceId) throws WasmException {
    final ManagedResource resource = getResource(resourceId);
    resource.incrementRefCount();
    LOGGER.fine(
        "Incremented ref count for resource " + resourceId + " to " + resource.getRefCount());
  }

  /**
   * Decrements the reference count for a resource and destroys it if count reaches zero.
   *
   * @param resourceId the resource handle
   * @return true if resource was destroyed, false otherwise
   * @throws WasmException if resource not found
   */
  public boolean decrementRefCount(final int resourceId) throws WasmException {
    final ManagedResource resource = getResource(resourceId);
    final int newRefCount = resource.decrementRefCount();

    LOGGER.fine("Decremented ref count for resource " + resourceId + " to " + newRefCount);

    if (newRefCount <= 0) {
      return destroyResource(resourceId);
    }

    return false;
  }

  /**
   * Destroys a managed resource.
   *
   * @param resourceId the resource handle
   * @return true if resource was destroyed, false if not found
   * @throws WasmException if destruction fails
   */
  public boolean destroyResource(final int resourceId) throws WasmException {
    ensureNotClosed();

    final ManagedResource managedResource = activeResources.remove(resourceId);
    if (managedResource == null) {
      return false;
    }

    try {
      // Call resource type destructor
      final ResourceTypeInfo typeInfo = managedResource.getTypeInfo();
      if (typeInfo.getDestructor() != null) {
        typeInfo.getDestructor().destroy(managedResource.getResource());
      }

      // Remove phantom reference
      phantomReferences.removeIf(ref -> ref.getResourceId() == resourceId);

      totalResourcesDestroyed.incrementAndGet();

      LOGGER.fine("Destroyed resource " + resourceId + " of type " + managedResource.getTypeName());
      return true;

    } catch (final Exception e) {
      throw new WasmException("Failed to destroy resource " + resourceId, e);
    }
  }

  /**
   * Gets all active resource handles.
   *
   * @return set of active resource handles
   */
  public Set<Integer> getActiveResourceIds() {
    return Set.copyOf(activeResources.keySet());
  }

  /**
   * Gets the number of active resources.
   *
   * @return number of active resources
   */
  public int getActiveResourceCount() {
    return activeResources.size();
  }

  /**
   * Gets the number of active resources of a specific type.
   *
   * @param typeName the resource type name
   * @return number of active resources of the type
   */
  public int getActiveResourceCount(final String typeName) {
    return (int)
        activeResources.values().stream()
            .filter(resource -> resource.getTypeName().equals(typeName))
            .count();
  }

  /**
   * Gets resource usage statistics.
   *
   * @return resource usage statistics
   */
  public ResourceUsageStats getUsageStats() {
    final long activeCount = activeResources.size();
    final long totalCreated = totalResourcesCreated.get();
    final long totalDestroyed = totalResourcesDestroyed.get();

    return new ResourceUsageStats(
        activeCount,
        totalCreated,
        totalDestroyed,
        config.getMaxResources(),
        getResourceTypeCounts());
  }

  /**
   * Performs cleanup of orphaned resources.
   *
   * @return number of resources cleaned up
   */
  public int performCleanup() {
    if (closed) {
      return 0;
    }

    int cleanedUp = 0;

    try {
      // Process phantom references
      ResourcePhantomReference phantomRef;
      while ((phantomRef = (ResourcePhantomReference) phantomQueue.poll()) != null) {
        try {
          if (destroyResource(phantomRef.getResourceId())) {
            cleanedUp++;
            LOGGER.fine(
                "Cleaned up orphaned resource "
                    + phantomRef.getResourceId()
                    + " of type "
                    + phantomRef.getTypeName());
          }
        } catch (final Exception e) {
          LOGGER.warning(
              "Failed to clean up orphaned resource "
                  + phantomRef.getResourceId()
                  + ": "
                  + e.getMessage());
        } finally {
          phantomReferences.remove(phantomRef);
        }
      }

      // Clean up expired resources
      final long currentTime = System.currentTimeMillis();
      final long expirationTime = config.getResourceExpirationMillis();

      if (expirationTime > 0) {
        final List<ManagedResource> expired =
            activeResources.values().stream()
                .filter(resource -> (currentTime - resource.getLastAccessedTime()) > expirationTime)
                .filter(resource -> resource.getRefCount() <= 0)
                .collect(java.util.stream.Collectors.toList());

        for (final ManagedResource resource : expired) {
          try {
            if (destroyResource(resource.getResourceId())) {
              cleanedUp++;
              LOGGER.fine("Cleaned up expired resource " + resource.getResourceId());
            }
          } catch (final Exception e) {
            LOGGER.warning(
                "Failed to clean up expired resource "
                    + resource.getResourceId()
                    + ": "
                    + e.getMessage());
          }
        }
      }

      if (cleanedUp > 0) {
        LOGGER.fine("Cleanup completed: " + cleanedUp + " resources cleaned up");
      }

    } catch (final Exception e) {
      LOGGER.warning("Error during resource cleanup: " + e.getMessage());
    }

    return cleanedUp;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Stop cleanup executor
      cleanupExecutor.shutdown();
      if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }

      // Destroy all remaining resources before marking as closed
      final Set<Integer> remainingResources = Set.copyOf(activeResources.keySet());
      for (final int resourceId : remainingResources) {
        try {
          destroyResource(resourceId);
        } catch (final Exception e) {
          LOGGER.warning(
              "Failed to destroy resource " + resourceId + " during cleanup: " + e.getMessage());
        }
      }

      // Clear phantom references
      phantomReferences.clear();

      LOGGER.fine("Closed WIT resource manager: " + managerId);

    } catch (final Exception e) {
      LOGGER.warning("Error during resource manager shutdown: " + e.getMessage());
    } finally {
      // Mark as closed at the very end
      closed = true;
    }
  }

  /**
   * Checks if the resource manager is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Gets the manager ID.
   *
   * @return the manager ID
   */
  public String getManagerId() {
    return managerId;
  }

  /**
   * Ensures the resource manager is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Resource manager is closed");
    }
  }

  /**
   * Gets resource type counts.
   *
   * @return map of resource type to count
   */
  private Map<String, Long> getResourceTypeCounts() {
    final Map<String, Long> counts = new ConcurrentHashMap<>();

    for (final ManagedResource resource : activeResources.values()) {
      counts.merge(resource.getTypeName(), 1L, Long::sum);
    }

    return counts;
  }

  /** Managed resource wrapper. */
  public static final class ManagedResource {
    private final int resourceId;
    private final String typeName;
    private final Object resource;
    private final long createdTime;
    private final ResourceTypeInfo typeInfo;

    private final AtomicInteger refCount;
    private volatile long lastAccessedTime;

    /**
     * Creates a new managed resource.
     *
     * @param resourceId unique resource identifier
     * @param typeName name of the resource type
     * @param resource the actual resource object
     * @param createdTime creation timestamp
     * @param typeInfo resource type information
     */
    public ManagedResource(
        final int resourceId,
        final String typeName,
        final Object resource,
        final long createdTime,
        final ResourceTypeInfo typeInfo) {
      this.resourceId = resourceId;
      this.typeName = typeName;
      this.resource = resource;
      this.createdTime = createdTime;
      this.typeInfo = typeInfo;
      this.refCount = new AtomicInteger(1);
      this.lastAccessedTime = createdTime;
    }

    public int getResourceId() {
      return resourceId;
    }

    public String getTypeName() {
      return typeName;
    }

    public Object getResource() {
      return resource;
    }

    public long getCreatedTime() {
      return createdTime;
    }

    public ResourceTypeInfo getTypeInfo() {
      return typeInfo;
    }

    public int getRefCount() {
      return refCount.get();
    }

    public long getLastAccessedTime() {
      return lastAccessedTime;
    }

    public void updateLastAccessed() {
      this.lastAccessedTime = System.currentTimeMillis();
    }

    public int incrementRefCount() {
      return refCount.incrementAndGet();
    }

    public int decrementRefCount() {
      return refCount.decrementAndGet();
    }
  }

  /** Resource configuration. */
  public static final class ResourceConfig {
    private final int maxResources;
    private final int cleanupIntervalSeconds;
    private final long resourceExpirationMillis;

    public ResourceConfig() {
      this(DEFAULT_MAX_RESOURCES, DEFAULT_CLEANUP_INTERVAL_SECONDS, 0);
    }

    /**
     * Creates a new resource configuration.
     *
     * @param maxResources maximum number of resources
     * @param cleanupIntervalSeconds cleanup interval in seconds
     * @param resourceExpirationMillis resource expiration time in milliseconds
     */
    public ResourceConfig(
        final int maxResources,
        final int cleanupIntervalSeconds,
        final long resourceExpirationMillis) {
      this.maxResources = maxResources;
      this.cleanupIntervalSeconds = cleanupIntervalSeconds;
      this.resourceExpirationMillis = resourceExpirationMillis;
    }

    public int getMaxResources() {
      return maxResources;
    }

    public int getCleanupIntervalSeconds() {
      return cleanupIntervalSeconds;
    }

    public long getResourceExpirationMillis() {
      return resourceExpirationMillis;
    }
  }

  /** Resource type information. */
  public static final class ResourceTypeInfo {
    private final String typeName;
    private final Class<?> resourceClass;
    private final ResourceDestructor destructor;

    /**
     * Creates new resource type information.
     *
     * @param typeName name of the resource type
     * @param resourceClass class of the resource
     * @param destructor resource destructor (can be null)
     */
    public ResourceTypeInfo(
        final String typeName, final Class<?> resourceClass, final ResourceDestructor destructor) {
      this.typeName = Objects.requireNonNull(typeName);
      this.resourceClass = Objects.requireNonNull(resourceClass);
      this.destructor = destructor; // Can be null
    }

    public String getTypeName() {
      return typeName;
    }

    public Class<?> getResourceClass() {
      return resourceClass;
    }

    public ResourceDestructor getDestructor() {
      return destructor;
    }
  }

  /** Resource destructor interface. */
  @FunctionalInterface
  public interface ResourceDestructor {
    /**
     * Destroys a resource.
     *
     * @param resource the resource to destroy
     * @throws Exception if destruction fails
     */
    void destroy(Object resource) throws Exception;
  }

  /** Resource usage statistics. */
  public static final class ResourceUsageStats {
    private final long activeResources;
    private final long totalCreated;
    private final long totalDestroyed;
    private final long maxResources;
    private final Map<String, Long> resourceTypeCounts;

    /**
     * Creates new resource usage statistics.
     *
     * @param activeResources number of currently active resources
     * @param totalCreated total number of resources created
     * @param totalDestroyed total number of resources destroyed
     * @param maxResources maximum number of resources
     * @param resourceTypeCounts counts by resource type
     */
    public ResourceUsageStats(
        final long activeResources,
        final long totalCreated,
        final long totalDestroyed,
        final long maxResources,
        final Map<String, Long> resourceTypeCounts) {
      this.activeResources = activeResources;
      this.totalCreated = totalCreated;
      this.totalDestroyed = totalDestroyed;
      this.maxResources = maxResources;
      this.resourceTypeCounts = Map.copyOf(resourceTypeCounts);
    }

    public long getActiveResources() {
      return activeResources;
    }

    public long getTotalCreated() {
      return totalCreated;
    }

    public long getTotalDestroyed() {
      return totalDestroyed;
    }

    public long getMaxResources() {
      return maxResources;
    }

    public Map<String, Long> getResourceTypeCounts() {
      return resourceTypeCounts;
    }
  }

  /** Phantom reference for resource cleanup. */
  private static final class ResourcePhantomReference extends PhantomReference<Object> {
    private final int resourceId;
    private final String typeName;

    public ResourcePhantomReference(
        final Object referent,
        final ReferenceQueue<? super Object> queue,
        final int resourceId,
        final String typeName) {
      super(referent, queue);
      this.resourceId = resourceId;
      this.typeName = typeName;
    }

    public int getResourceId() {
      return resourceId;
    }

    public String getTypeName() {
      return typeName;
    }
  }
}
