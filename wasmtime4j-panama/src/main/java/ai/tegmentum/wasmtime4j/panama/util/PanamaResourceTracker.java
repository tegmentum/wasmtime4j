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

package ai.tegmentum.wasmtime4j.panama.util;

import java.lang.foreign.MemorySegment;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Resource tracking utility for Panama FFI operations.
 *
 * <p>This class tracks native resources and their associated Java objects to ensure proper cleanup
 * and prevent resource leaks. It uses weak references to avoid preventing garbage collection of
 * tracked objects.
 *
 * <p>The tracker helps identify resource leaks during development and provides statistics about
 * resource usage patterns.
 *
 * @since 1.0.0
 */
public final class PanamaResourceTracker {
  private static final Logger logger = Logger.getLogger(PanamaResourceTracker.class.getName());

  private final ConcurrentMap<Object, ResourceInfo> trackedResources = new ConcurrentHashMap<>();
  private final AtomicLong totalTracked = new AtomicLong(0);
  private final AtomicLong totalCleaned = new AtomicLong(0);

  /** Information about a tracked resource. */
  private static final class ResourceInfo {
    final WeakReference<Object> objectRef;
    final MemorySegment handle;
    final String className;
    final long creationTime;
    final StackTraceElement[] creationStack;

    ResourceInfo(final Object object, final MemorySegment handle) {
      this.objectRef = new WeakReference<>(object);
      this.handle = handle;
      this.className = object.getClass().getSimpleName();
      this.creationTime = System.currentTimeMillis();
      this.creationStack = Thread.currentThread().getStackTrace();
    }

    boolean isAlive() {
      return objectRef.get() != null;
    }

    String getCreationInfo() {
      return String.format("%s created at %d", className, creationTime);
    }
  }

  /** Creates a new Panama resource tracker. */
  public PanamaResourceTracker() {
    logger.fine("Created Panama resource tracker");
  }

  /**
   * Tracks a resource and its associated native handle.
   *
   * @param resource the Java object that owns the resource
   * @param handle the native memory handle
   * @throws IllegalArgumentException if resource or handle is null
   */
  public void trackResource(final Object resource, final MemorySegment handle) {
    if (resource == null) {
      throw new IllegalArgumentException("Resource cannot be null");
    }
    if (handle == null) {
      throw new IllegalArgumentException("Handle cannot be null");
    }

    final ResourceInfo info = new ResourceInfo(resource, handle);
    trackedResources.put(resource, info);
    totalTracked.incrementAndGet();

    logger.fine(
        "Tracking resource: "
            + resource.getClass().getSimpleName()
            + " with handle "
            + handle.address());
  }

  /**
   * Stops tracking a resource.
   *
   * @param resource the Java object to untrack
   * @return true if the resource was being tracked, false otherwise
   */
  public boolean untrackResource(final Object resource) {
    if (resource == null) {
      return false;
    }

    final ResourceInfo info = trackedResources.remove(resource);
    if (info != null) {
      totalCleaned.incrementAndGet();
      logger.fine("Untracked resource: " + resource.getClass().getSimpleName());
      return true;
    }

    return false;
  }

  /**
   * Checks if a resource is currently being tracked.
   *
   * @param resource the resource to check
   * @return true if tracked, false otherwise
   */
  public boolean isTracked(final Object resource) {
    return resource != null && trackedResources.containsKey(resource);
  }

  /**
   * Gets the native handle for a tracked resource.
   *
   * @param resource the tracked resource
   * @return the native handle, or null if not tracked
   */
  public MemorySegment getHandle(final Object resource) {
    if (resource == null) {
      return null;
    }

    final ResourceInfo info = trackedResources.get(resource);
    return info != null ? info.handle : null;
  }

  /**
   * Performs cleanup of resources that have been garbage collected.
   *
   * <p>This method should be called periodically to clean up tracking information for objects that
   * have been garbage collected but were not properly untracked.
   *
   * @return the number of orphaned resources cleaned up
   */
  public int cleanupOrphanedResources() {
    int cleaned = 0;

    for (final var entry : trackedResources.entrySet()) {
      final Object key = entry.getKey();
      final ResourceInfo info = entry.getValue();

      if (!info.isAlive()) {
        trackedResources.remove(key);
        cleaned++;

        logger.warning("Cleaned up orphaned resource: " + info.getCreationInfo());
      }
    }

    if (cleaned > 0) {
      logger.info("Cleaned up " + cleaned + " orphaned resources");
    }

    return cleaned;
  }

  /**
   * Performs complete cleanup of all tracked resources.
   *
   * <p>This method should be called when shutting down the runtime to ensure all resources are
   * properly cleaned up.
   */
  public void cleanup() {
    final int resourceCount = trackedResources.size();

    if (resourceCount > 0) {
      logger.info("Cleaning up " + resourceCount + " remaining tracked resources");

      // Log information about resources that were not properly cleaned
      for (final ResourceInfo info : trackedResources.values()) {
        if (info.isAlive()) {
          logger.warning("Resource not properly cleaned: " + info.getCreationInfo());
        }
      }
    }

    trackedResources.clear();
    logger.info("Panama resource tracker cleanup completed");
  }

  /**
   * Gets the number of currently tracked resources.
   *
   * @return the number of tracked resources
   */
  public int getTrackedResourceCount() {
    return trackedResources.size();
  }

  /**
   * Gets the total number of resources that have been tracked.
   *
   * @return the total tracked count
   */
  public long getTotalTracked() {
    return totalTracked.get();
  }

  /**
   * Gets the total number of resources that have been cleaned up.
   *
   * @return the total cleaned count
   */
  public long getTotalCleaned() {
    return totalCleaned.get();
  }

  /**
   * Gets the number of resources that may have leaked.
   *
   * <p>This is the difference between total tracked and total cleaned resources.
   *
   * @return the potential leak count
   */
  public long getPotentialLeaks() {
    return getTotalTracked() - getTotalCleaned() - getTrackedResourceCount();
  }

  /**
   * Gets resource tracking statistics as a formatted string.
   *
   * @return resource tracking statistics
   */
  public String getTrackingStats() {
    return String.format(
        "PanamaResourceTracker: %d active, %d total tracked, %d cleaned, %d potential leaks",
        getTrackedResourceCount(), getTotalTracked(), getTotalCleaned(), getPotentialLeaks());
  }

  /**
   * Checks if there are any potential resource leaks.
   *
   * @return true if there may be resource leaks, false otherwise
   */
  public boolean hasPotentialLeaks() {
    return getPotentialLeaks() > 0;
  }

  /** Logs current resource tracking statistics. */
  public void logStats() {
    logger.info(getTrackingStats());

    if (hasPotentialLeaks()) {
      logger.warning("Potential resource leaks detected!");
    }
  }

  /**
   * Gets a summary of all currently tracked resources by type.
   *
   * @return a map of class names to counts
   */
  public ConcurrentMap<String, Long> getResourceSummary() {
    final ConcurrentMap<String, Long> summary = new ConcurrentHashMap<>();

    for (final ResourceInfo info : trackedResources.values()) {
      summary.merge(info.className, 1L, Long::sum);
    }

    return summary;
  }
}
