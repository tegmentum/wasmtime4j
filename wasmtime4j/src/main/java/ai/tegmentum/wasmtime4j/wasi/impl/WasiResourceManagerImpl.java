package ai.tegmentum.wasmtime4j.wasi.impl;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.wasi.WasiResource;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceLimits;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceManager;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceUsageStats;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Default implementation of WasiResourceManager for WASI Preview 2 support.
 *
 * <p>This implementation provides thread-safe resource management with automatic cleanup,
 * capability-based security, and comprehensive monitoring. It supports all WASI Preview 2 resource
 * types and implements defensive programming patterns to prevent resource leaks.
 *
 * <p>The implementation uses:
 * <ul>
 *   <li>Concurrent data structures for thread safety</li>
 *   <li>Read-write locks for optimal concurrent access</li>
 *   <li>Automatic resource cleanup and leak detection</li>
 *   <li>Comprehensive validation and error handling</li>
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiResourceManagerImpl implements WasiResourceManager {

  private static final Logger LOGGER = Logger.getLogger(WasiResourceManagerImpl.class.getName());

  // Thread-safe resource management
  private final ConcurrentHashMap<String, WasiResource> resources = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, WasiResource> resourcesById = new ConcurrentHashMap<>();
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  // Resource lifecycle tracking
  private final AtomicLong nextResourceId = new AtomicLong(1);
  private final WasiResourceUsageStatsImpl usageStats = new WasiResourceUsageStatsImpl();

  // Configuration and limits
  private volatile WasiResourceLimits resourceLimits = WasiResourceLimits.defaults();
  private final Instant createdAt = Instant.now();
  private volatile boolean closed = false;

  /**
   * Creates a new WASI resource manager with default configuration.
   */
  public WasiResourceManagerImpl() {
    LOGGER.fine("Created WASI resource manager");
  }

  /**
   * Creates a new WASI resource manager with specific limits.
   *
   * @param limits initial resource limits
   * @throws IllegalArgumentException if limits is null
   */
  public WasiResourceManagerImpl(final WasiResourceLimits limits) {
    this.resourceLimits = Objects.requireNonNull(limits, "limits");
    LOGGER.fine("Created WASI resource manager with custom limits");
  }

  @Override
  public <T extends WasiResource> T createResource(final Class<T> type, final WasiResourceConfig config)
      throws WasmException {
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(config, "config");
    ensureNotClosed();

    final String resourceName = generateResourceName(type);
    return createResource(resourceName, type, config);
  }

  @Override
  public <T extends WasiResource> T createResource(final String name, final Class<T> type,
      final WasiResourceConfig config) throws WasmException {
    validateNonEmpty(name, "name");
    Objects.requireNonNull(type, "type");
    Objects.requireNonNull(config, "config");
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      // Check if resource already exists
      if (resources.containsKey(name)) {
        throw new WasiResourceException("Resource already exists: " + name);
      }

      // Check resource limits
      checkResourceLimits(type);

      // Create the resource
      final long resourceId = nextResourceId.getAndIncrement();
      final T resource = createResourceInstance(resourceId, name, type, config);

      // Register the resource
      resources.put(name, resource);
      resourcesById.put(resourceId, resource);

      // Update statistics
      usageStats.recordResourceCreated(type.getSimpleName());

      LOGGER.fine("Created resource: " + name + " (type: " + type.getSimpleName() + ")");
      return resource;

    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Optional<WasiResource> getResource(final String name) throws WasmException {
    validateNonEmpty(name, "name");
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final WasiResource resource = resources.get(name);
      if (resource != null && resource.isValid()) {
        usageStats.recordResourceAccessed(name);
        return Optional.of(resource);
      }
      return Optional.empty();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends WasiResource> Optional<T> getResource(final String name, final Class<T> type)
      throws WasmException {
    validateNonEmpty(name, "name");
    Objects.requireNonNull(type, "type");
    ensureNotClosed();

    final Optional<WasiResource> resource = getResource(name);
    if (resource.isPresent() && type.isInstance(resource.get())) {
      return Optional.of((T) resource.get());
    }
    return Optional.empty();
  }

  @Override
  public void releaseResource(final WasiResource resource) throws WasmException {
    Objects.requireNonNull(resource, "resource");
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      // Find the resource by reference
      String resourceName = null;
      for (final Map.Entry<String, WasiResource> entry : resources.entrySet()) {
        if (entry.getValue() == resource) {
          resourceName = entry.getKey();
          break;
        }
      }

      if (resourceName != null) {
        releaseResourceInternal(resourceName, resource);
      } else {
        LOGGER.warning("Attempted to release unknown resource: " + resource.getId());
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void releaseResource(final String name) throws WasmException {
    validateNonEmpty(name, "name");
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      final WasiResource resource = resources.get(name);
      if (resource != null) {
        releaseResourceInternal(name, resource);
      } else {
        LOGGER.warning("Attempted to release unknown resource: " + name);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public List<WasiResource> getActiveResources() throws WasmException {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final List<WasiResource> activeResources = new ArrayList<>();
      for (final WasiResource resource : resources.values()) {
        if (resource.isValid()) {
          activeResources.add(resource);
        }
      }
      return Collections.unmodifiableList(activeResources);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends WasiResource> List<T> getActiveResources(final Class<T> type) throws WasmException {
    Objects.requireNonNull(type, "type");
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final List<T> typedResources = new ArrayList<>();
      for (final WasiResource resource : resources.values()) {
        if (resource.isValid() && type.isInstance(resource)) {
          typedResources.add((T) resource);
        }
      }
      return Collections.unmodifiableList(typedResources);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public WasiResourceUsageStats getUsageStats() {
    return usageStats;
  }

  @Override
  public void setResourceLimits(final WasiResourceLimits limits) throws WasmException {
    Objects.requireNonNull(limits, "limits");
    ensureNotClosed();

    // Validate that current resources don't exceed new limits
    validateCurrentResourcesAgainstLimits(limits);

    this.resourceLimits = limits;
    LOGGER.fine("Updated resource limits");
  }

  @Override
  public WasiResourceLimits getResourceLimits() {
    return resourceLimits;
  }

  @Override
  public void validateResources() throws WasmException {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final List<String> invalidResources = new ArrayList<>();

      for (final Map.Entry<String, WasiResource> entry : resources.entrySet()) {
        try {
          final WasiResource resource = entry.getValue();
          if (!resource.isValid()) {
            invalidResources.add(entry.getKey());
          } else {
            // Perform deeper validation by trying to get metadata
            resource.getMetadata();
          }
        } catch (final Exception e) {
          invalidResources.add(entry.getKey());
          LOGGER.log(Level.WARNING, "Resource validation failed for: " + entry.getKey(), e);
        }
      }

      if (!invalidResources.isEmpty()) {
        throw new WasiResourceException(
            "Resource validation failed for: " + String.join(", ", invalidResources));
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int cleanupResources() throws WasmException {
    ensureNotClosed();

    lock.writeLock().lock();
    try {
      int cleaned = 0;
      final List<String> toRemove = new ArrayList<>();

      for (final Map.Entry<String, WasiResource> entry : resources.entrySet()) {
        final WasiResource resource = entry.getValue();
        if (!resource.isValid()) {
          toRemove.add(entry.getKey());
          try {
            if (!resource.isValid()) {
              // Resource is already invalid, just clean up our references
              resourcesById.remove(resource.getId());
            }
          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Error cleaning up resource: " + entry.getKey(), e);
          }
          cleaned++;
        }
      }

      for (final String name : toRemove) {
        resources.remove(name);
        usageStats.recordResourceReleased(name);
      }

      if (cleaned > 0) {
        LOGGER.fine("Cleaned up " + cleaned + " invalid resources");
      }

      return cleaned;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Map<String, WasiResourceMetadata> getResourceMetadata() throws WasmException {
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final Map<String, WasiResourceMetadata> metadata = new HashMap<>();

      for (final Map.Entry<String, WasiResource> entry : resources.entrySet()) {
        try {
          if (entry.getValue().isValid()) {
            metadata.put(entry.getKey(), entry.getValue().getMetadata());
          }
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Failed to get metadata for resource: " + entry.getKey(), e);
        }
      }

      return Collections.unmodifiableMap(metadata);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean hasResource(final String name) throws WasmException {
    validateNonEmpty(name, "name");
    ensureNotClosed();

    lock.readLock().lock();
    try {
      final WasiResource resource = resources.get(name);
      return resource != null && resource.isValid();
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int getActiveResourceCount() {
    lock.readLock().lock();
    try {
      int count = 0;
      for (final WasiResource resource : resources.values()) {
        if (resource.isValid()) {
          count++;
        }
      }
      return count;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int getActiveResourceCount(final Class<? extends WasiResource> type) {
    Objects.requireNonNull(type, "type");

    lock.readLock().lock();
    try {
      int count = 0;
      for (final WasiResource resource : resources.values()) {
        if (resource.isValid() && type.isInstance(resource)) {
          count++;
        }
      }
      return count;
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean isValid() {
    return !closed;
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    lock.writeLock().lock();
    try {
      closed = true;

      // Close all resources
      for (final WasiResource resource : resources.values()) {
        try {
          resource.close();
        } catch (final Exception e) {
          LOGGER.log(Level.WARNING, "Error closing resource during manager shutdown", e);
        }
      }

      resources.clear();
      resourcesById.clear();

      LOGGER.fine("WASI resource manager closed");
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Ensures the manager is not closed.
   *
   * @throws IllegalStateException if the manager is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("WASI resource manager has been closed");
    }
  }

  /**
   * Validates that a string parameter is not null or empty.
   *
   * @param value the value to validate
   * @param name the parameter name
   * @throws IllegalArgumentException if value is null or empty
   */
  private void validateNonEmpty(final String value, final String name) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException(name + " cannot be null or empty");
    }
  }

  /**
   * Generates a unique resource name for the given type.
   *
   * @param type the resource type
   * @return generated resource name
   */
  private String generateResourceName(final Class<? extends WasiResource> type) {
    return type.getSimpleName().toLowerCase() + "-" + nextResourceId.get();
  }

  /**
   * Checks if creating a new resource would violate current limits.
   *
   * @param type the resource type to check
   * @throws WasiResourceException if limits would be exceeded
   */
  private void checkResourceLimits(final Class<? extends WasiResource> type) throws WasiResourceException {
    final int currentCount = getActiveResourceCount();
    if (currentCount >= resourceLimits.getMaxTotalResources()) {
      throw new WasiResourceException(
          "Maximum resource limit exceeded: " + currentCount + " >= " + resourceLimits.getMaxTotalResources());
    }

    final int typeCount = getActiveResourceCount(type);
    final int typeLimit = resourceLimits.getMaxResourcesPerType().getOrDefault(
        type.getSimpleName(), resourceLimits.getMaxTotalResources());

    if (typeCount >= typeLimit) {
      throw new WasiResourceException(
          "Maximum resource limit exceeded for type " + type.getSimpleName() + ": "
          + typeCount + " >= " + typeLimit);
    }
  }

  /**
   * Creates a new resource instance of the specified type.
   *
   * @param <T> the resource type
   * @param resourceId the unique resource ID
   * @param name the resource name
   * @param type the resource type class
   * @param config the resource configuration
   * @return the created resource instance
   * @throws WasmException if resource creation fails
   */
  @SuppressWarnings("unchecked")
  private <T extends WasiResource> T createResourceInstance(final long resourceId, final String name,
      final Class<T> type, final WasiResourceConfig config) throws WasmException {

    // This is a factory method that would create the appropriate resource implementation
    // based on the type and configuration. For now, we'll use a generic implementation.

    try {
      if (type.getSimpleName().contains("File")) {
        return (T) new WasiFileResourceImpl(resourceId, name, config);
      } else if (type.getSimpleName().contains("Socket")) {
        return (T) new WasiSocketResourceImpl(resourceId, name, config);
      } else if (type.getSimpleName().contains("Timer")) {
        return (T) new WasiTimerResourceImpl(resourceId, name, config);
      } else {
        return (T) new WasiGenericResourceImpl(resourceId, name, type.getSimpleName(), config);
      }
    } catch (final Exception e) {
      throw new WasiResourceException("Failed to create resource: " + name, e);
    }
  }

  /**
   * Internal method to release a resource with proper cleanup.
   *
   * @param name the resource name
   * @param resource the resource instance
   */
  private void releaseResourceInternal(final String name, final WasiResource resource) {
    try {
      resource.close();
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing resource during release: " + name, e);
    }

    resources.remove(name);
    resourcesById.remove(resource.getId());
    usageStats.recordResourceReleased(name);

    LOGGER.fine("Released resource: " + name);
  }

  /**
   * Validates that current resources don't exceed new limits.
   *
   * @param newLimits the new limits to validate against
   * @throws WasiResourceException if current resources exceed new limits
   */
  private void validateCurrentResourcesAgainstLimits(final WasiResourceLimits newLimits)
      throws WasiResourceException {
    final int currentCount = getActiveResourceCount();
    if (currentCount > newLimits.getMaxTotalResources()) {
      throw new WasiResourceException(
          "Current resource count " + currentCount + " exceeds new limit " + newLimits.getMaxTotalResources());
    }

    // Check per-type limits
    for (final Map.Entry<String, Integer> entry : newLimits.getMaxResourcesPerType().entrySet()) {
      final String typeName = entry.getKey();
      final int limit = entry.getValue();

      final long currentTypeCount = resources.values().stream()
          .filter(r -> r.isValid() && r.getType().contains(typeName))
          .count();

      if (currentTypeCount > limit) {
        throw new WasiResourceException(
            "Current resource count for type " + typeName + " (" + currentTypeCount
            + ") exceeds new limit " + limit);
      }
    }
  }
}