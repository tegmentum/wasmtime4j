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
import java.lang.foreign.MemoryLayout;
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
 * Arena-based resource management foundation for Panama FFI operations.
 *
 * <p>This class provides automatic resource cleanup and lifecycle management for native resources
 * using Arena-based memory allocation. It ensures proper cleanup of native resources to prevent
 * memory leaks and provides comprehensive tracking capabilities.
 *
 * <p>The resource manager uses Java's Cleaner API as a safety net for resources that are not
 * properly closed, while encouraging explicit resource management through try-with-resources
 * patterns.
 */
public final class ArenaResourceManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(ArenaResourceManager.class.getName());

  // Global cleaner for resource cleanup
  private static final Cleaner CLEANER = Cleaner.create();

  // Resource tracking
  private final Arena arena;
  private final ConcurrentHashMap<Long, ManagedResource> resources;
  private final List<ManagedNativeResource> managedResources;
  private final AtomicLong resourceIdGenerator;
  private final boolean trackingEnabled;

  // Status tracking
  private volatile boolean closed = false;

  /** Creates a new arena resource manager with a shared arena. */
  public ArenaResourceManager() {
    this(Arena.ofShared(), true);
  }

  /**
   * Creates a new arena resource manager with the specified arena.
   *
   * @param arena the arena to use for resource management
   * @param trackingEnabled whether to enable resource tracking
   */
  public ArenaResourceManager(final Arena arena, final boolean trackingEnabled) {
    this.arena = Objects.requireNonNull(arena, "Arena cannot be null");
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
   * Allocates memory in the arena with the specified layout.
   *
   * @param layout the memory layout to allocate
   * @return managed memory segment
   * @throws IllegalStateException if the manager is closed
   */
  public ManagedMemorySegment allocate(final MemoryLayout layout) {
    Objects.requireNonNull(layout, "Memory layout cannot be null");
    checkNotClosed();

    try {
      MemorySegment segment = arena.allocate(layout);
      return createManagedSegment(segment, "allocated(" + layout + ")");
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to allocate memory with layout: " + layout, e);
      throw e;
    }
  }

  /**
   * Allocates memory in the arena with the specified size and alignment.
   *
   * @param size the size in bytes
   * @param alignment the alignment in bytes
   * @return managed memory segment
   * @throws IllegalStateException if the manager is closed
   */
  public ManagedMemorySegment allocate(final long size, final long alignment) {
    if (size <= 0) {
      throw new IllegalArgumentException("Size must be positive: " + size);
    }
    if (alignment <= 0) {
      throw new IllegalArgumentException("Alignment must be positive: " + alignment);
    }
    checkNotClosed();

    try {
      MemorySegment segment = arena.allocate(size, alignment);
      return createManagedSegment(segment, "allocated(" + size + "," + alignment + ")");
    } catch (Exception e) {
      LOGGER.log(
          Level.WARNING, "Failed to allocate memory: size=" + size + ", alignment=" + alignment, e);
      throw e;
    }
  }

  /**
   * Allocates memory in the arena with the specified size.
   *
   * @param size the size in bytes
   * @return managed memory segment
   * @throws IllegalStateException if the manager is closed
   */
  public ManagedMemorySegment allocate(final long size) {
    if (size <= 0) {
      throw new IllegalArgumentException("Size must be positive: " + size);
    }
    checkNotClosed();

    try {
      MemorySegment segment = arena.allocate(size);
      return createManagedSegment(segment, "allocated(" + size + ")");
    } catch (Exception e) {
      LOGGER.log(Level.WARNING, "Failed to allocate memory with size: " + size, e);
      throw e;
    }
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
   * Gets the number of tracked resources.
   *
   * @return number of tracked resources, or -1 if tracking is disabled
   */
  public int getResourceCount() {
    return trackingEnabled ? resources.size() : -1;
  }

  /**
   * Gets resource tracking information.
   *
   * @return resource tracking info, or empty string if tracking is disabled
   */
  public String getResourceTrackingInfo() {
    if (!trackingEnabled) {
      return "Resource tracking is disabled";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("Tracked resources (").append(resources.size()).append("):\n");

    resources.forEach(
        (id, resource) -> {
          long ageMs = System.currentTimeMillis() - resource.getCreationTime();
          sb.append("  - ID ")
              .append(id)
              .append(": ")
              .append(resource.getDescription())
              .append(" (age: ")
              .append(ageMs)
              .append("ms)\n");
        });

    return sb.toString();
  }

  /**
   * Checks if the manager is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
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
        LOGGER.warning(
            "Closing resource manager with "
                + resources.size()
                + " unclosed resources:\n"
                + getResourceTrackingInfo());
      }

      // Close the arena (this will free all allocated memory)
      if (arena.scope().isAlive()) {
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
   * Creates a managed memory segment wrapper.
   *
   * @param segment the memory segment to wrap
   * @param description description for tracking
   * @return managed memory segment
   */
  private ManagedMemorySegment createManagedSegment(
      final MemorySegment segment, final String description) {
    ManagedMemorySegment managedSegment = new ManagedMemorySegment(segment, description);

    if (trackingEnabled) {
      long resourceId = resourceIdGenerator.getAndIncrement();
      ManagedResource managedResource =
          new ManagedResource(resourceId, description, System.currentTimeMillis());
      resources.put(resourceId, managedResource);

      LOGGER.finest("Created managed memory segment: " + description + " (id=" + resourceId + ")");
    }

    return managedSegment;
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

  /** Managed memory segment that tracks its lifecycle. */
  public static final class ManagedMemorySegment {
    private final MemorySegment segment;
    private final String description;

    private ManagedMemorySegment(final MemorySegment segment, final String description) {
      this.segment = Objects.requireNonNull(segment);
      this.description = Objects.requireNonNull(description);
    }

    /**
     * Gets the underlying memory segment.
     *
     * @return the memory segment
     */
    public MemorySegment getSegment() {
      return segment;
    }

    /**
     * Gets the description of this managed segment.
     *
     * @return the description
     */
    public String getDescription() {
      return description;
    }

    /**
     * Gets the size of the memory segment.
     *
     * @return size in bytes
     */
    public long size() {
      return segment.byteSize();
    }

    /**
     * Reinterprets the segment with a new size.
     *
     * @param newSize the new size
     * @return reinterpreted segment
     */
    public MemorySegment reinterpret(final long newSize) {
      return segment.reinterpret(newSize);
    }

    /**
     * Creates a slice of the segment.
     *
     * @param offset the offset
     * @param newSize the new size
     * @return slice of the segment
     */
    public MemorySegment asSlice(final long offset, final long newSize) {
      return segment.asSlice(offset, newSize);
    }

    @Override
    public String toString() {
      return "ManagedMemorySegment{description='"
          + description
          + "', size="
          + segment.byteSize()
          + "}";
    }
  }

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
    public MemorySegment getNativePointer() {
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
     * Checks if the resource is closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
      return closed;
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

  /** Internal resource tracking information. */
  private static final class ManagedResource {
    private final long id;
    private final String description;
    private final long creationTime;

    ManagedResource(final long id, final String description, final long creationTime) {
      this.id = id;
      this.description = description;
      this.creationTime = creationTime;
    }

    long getId() {
      return id;
    }

    String getDescription() {
      return description;
    }

    long getCreationTime() {
      return creationTime;
    }
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

    synchronized (managedResources) {
      managedResources.add(resource);
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
}
