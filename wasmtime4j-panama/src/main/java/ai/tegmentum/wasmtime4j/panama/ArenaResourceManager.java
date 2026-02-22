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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.ref.Cleaner;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Arena-based resource management for Panama FFI operations.
 *
 * <p>This class provides automatic resource cleanup and lifecycle management for native resources
 * using Arena-based memory allocation. It ensures proper cleanup of native resources to prevent
 * memory leaks.
 *
 * <p>The resource manager uses Java's Cleaner API as a safety net for resources that are not
 * properly closed, while encouraging explicit resource management through try-with-resources
 * patterns.
 */
public final class ArenaResourceManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(ArenaResourceManager.class.getName());

  private static final Cleaner CLEANER = Cleaner.create();

  private final Arena arena;
  private final boolean ownsArena;
  private final ConcurrentHashMap<Long, ManagedResource> resources;
  private final List<ManagedNativeResource> managedResources;
  private final AtomicLong resourceIdGenerator;
  private final boolean trackingEnabled;

  private volatile boolean closed = false;

  /** Creates a new arena resource manager with a shared arena and tracking enabled. */
  public ArenaResourceManager() {
    this(Arena.ofShared(), true, true);
  }

  /**
   * Creates a new arena resource manager with the specified arena.
   *
   * <p>The arena is NOT owned by this manager and will NOT be closed when the manager is closed.
   * The caller is responsible for closing the arena.
   *
   * @param arena the arena to use for resource management
   * @param trackingEnabled whether to enable resource tracking
   */
  public ArenaResourceManager(final Arena arena, final boolean trackingEnabled) {
    this(arena, trackingEnabled, false);
  }

  /**
   * Creates a new arena resource manager with the specified arena and ownership.
   *
   * @param arena the arena to use for resource management
   * @param trackingEnabled whether to enable resource tracking
   * @param ownsArena whether this manager owns the arena and should close it
   */
  private ArenaResourceManager(
      final Arena arena, final boolean trackingEnabled, final boolean ownsArena) {
    this.arena = Objects.requireNonNull(arena, "Arena cannot be null");
    this.ownsArena = ownsArena;
    this.trackingEnabled = trackingEnabled;
    this.resources = trackingEnabled ? new ConcurrentHashMap<>() : null;
    this.managedResources = trackingEnabled ? new CopyOnWriteArrayList<>() : null;
    this.resourceIdGenerator = trackingEnabled ? new AtomicLong(1) : null;

    LOGGER.fine("Created ArenaResourceManager with tracking: " + trackingEnabled);
  }

  /**
   * Gets the underlying arena.
   *
   * @return the arena
   * @throws IllegalStateException if the manager is closed
   */
  public Arena getArena() {
    checkNotClosed();
    return arena;
  }

  /**
   * Creates a managed wrapper around an existing native resource.
   *
   * @param nativePointer pointer to the native resource
   * @param cleanup cleanup action to perform when resource is released
   * @param description description of the resource for tracking
   * @return managed native resource
   * @throws IllegalStateException if the manager is closed
   */
  public ManagedNativeResource manageNativeResource(
      final MemorySegment nativePointer, final Runnable cleanup, final String description) {
    Objects.requireNonNull(nativePointer, "Native pointer cannot be null");
    Objects.requireNonNull(cleanup, "Cleanup action cannot be null");
    Objects.requireNonNull(description, "Description cannot be null");
    checkNotClosed();

    ManagedNativeResource resource = new ManagedNativeResource(nativePointer, cleanup, description);

    if (trackingEnabled) {
      long resourceId = resourceIdGenerator.getAndIncrement();
      ManagedResource managedResource =
          new ManagedResource(resourceId, description, System.currentTimeMillis());
      resources.put(resourceId, managedResource);

      // Register cleaner for safety net
      CLEANER.register(
          resource,
          () -> {
            LOGGER.warning("Resource was not properly closed, cleaning up: " + description);
            cleanup.run();
            if (trackingEnabled && resources != null) {
              resources.remove(resourceId);
            }
          });

      LOGGER.finest("Created managed native resource: " + description + " (id=" + resourceId + ")");
    }

    return resource;
  }

  /**
   * Registers a managed native resource with a cleanup action.
   *
   * @param owner the resource owner
   * @param nativeHandle the native handle
   * @param cleanupAction the cleanup action to run when the resource is closed
   * @return a managed native resource
   */
  public ManagedNativeResource registerManagedNativeResource(
      final Object owner, final MemorySegment nativeHandle, final Runnable cleanupAction) {
    checkNotClosed();

    if (owner == null) {
      throw new IllegalArgumentException("Owner cannot be null");
    }
    if (nativeHandle == null || nativeHandle.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native handle cannot be null");
    }
    if (cleanupAction == null) {
      throw new IllegalArgumentException("Cleanup action cannot be null");
    }

    ManagedNativeResource resource =
        new ManagedNativeResource(nativeHandle, cleanupAction, owner.getClass().getSimpleName());

    if (trackingEnabled && managedResources != null) {
      synchronized (managedResources) {
        managedResources.add(resource);
      }
    }

    LOGGER.fine("Registered managed native resource: " + owner.getClass().getSimpleName());
    return resource;
  }

  /**
   * Unregisters a managed resource.
   *
   * @param owner the resource owner to unregister
   */
  public void unregisterManagedResource(final Object owner) {
    if (owner == null) {
      return;
    }

    if (!trackingEnabled || managedResources == null) {
      return;
    }

    String ownerName = owner.getClass().getSimpleName();
    synchronized (managedResources) {
      managedResources.removeIf(
          resource -> {
            if (resource instanceof ManagedNativeResource managedNativeResource) {
              return ownerName.equals(managedNativeResource.getDescription());
            }
            return false;
          });
    }

    LOGGER.fine("Unregistered managed resource: " + ownerName);
  }

  /**
   * Checks if the resource manager is valid (not closed).
   *
   * @return true if valid, false if closed
   */
  public boolean isValid() {
    return !closed;
  }

  /** Closes the resource manager and cleans up all resources. */
  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      // Log resource leaks if any
      if (trackingEnabled && !resources.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append("Closing resource manager with ")
            .append(resources.size())
            .append(" unclosed resources:\n");
        resources.forEach(
            (id, resource) -> {
              long ageMs = System.currentTimeMillis() - resource.creationTime();
              sb.append("  - ID ")
                  .append(id)
                  .append(": ")
                  .append(resource.description())
                  .append(" (age: ")
                  .append(ageMs)
                  .append("ms)\n");
            });
        LOGGER.warning(sb.toString());
      }

      // Close the arena only if we own it (this will free all allocated memory)
      if (ownsArena && arena.scope().isAlive()) {
        arena.close();
      }

      // Clear resource tracking
      if (trackingEnabled) {
        resources.clear();
      }

      closed = true;
      LOGGER.fine("Closed ArenaResourceManager");
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Error during resource manager shutdown", e);
    }
  }

  /**
   * Checks that the resource manager is not closed.
   *
   * @throws IllegalStateException if the manager is closed
   */
  private void checkNotClosed() {
    if (closed) {
      throw new IllegalStateException("Resource manager is closed");
    }
  }

  /** Simple tracking record for managed resources used in leak detection. */
  private record ManagedResource(long id, String description, long creationTime) {}

  /** Managed native resource that tracks its lifecycle. */
  public static final class ManagedNativeResource implements AutoCloseable {
    private final MemorySegment nativePointer;
    private final Runnable cleanup;
    private final String description;
    private volatile boolean closed = false;

    private ManagedNativeResource(
        final MemorySegment nativePointer, final Runnable cleanup, final String description) {
      this.nativePointer = Objects.requireNonNull(nativePointer);
      this.cleanup = Objects.requireNonNull(cleanup);
      this.description = Objects.requireNonNull(description);
    }

    /**
     * Gets the native pointer.
     *
     * @return the native pointer
     * @throws IllegalStateException if the resource is closed
     */
    public MemorySegment resource() {
      if (closed) {
        throw new IllegalStateException("Native resource is closed: " + description);
      }
      return nativePointer;
    }

    /**
     * Gets the description of this managed resource.
     *
     * @return the description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Checks if the resource is valid (not closed).
     *
     * @return true if valid, false if closed
     */
    public boolean isValid() {
      return !closed;
    }

    /** Closes the native resource. */
    @Override
    public void close() {
      if (!closed) {
        closed = true;
        try {
          cleanup.run();
          LOGGER.finest("Closed managed native resource: " + description);
        } catch (Exception e) {
          LOGGER.log(Level.WARNING, "Error closing native resource: " + description, e);
        }
      }
    }

    @Override
    public String toString() {
      return "ManagedNativeResource{description='" + description + "', closed=" + closed + "}";
    }
  }

}
