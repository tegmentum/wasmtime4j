package ai.tegmentum.wasmtime4j.jni.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Utility class for managing the lifecycle of JNI resources and preventing memory leaks.
 *
 * <p>This class provides defensive programming by tracking native resource handles and ensuring
 * proper cleanup. It helps prevent native memory leaks and provides debugging information about
 * resource usage.
 *
 * <p>The memory manager is thread-safe and can be used across multiple threads to track and manage
 * native resources.
 */
public final class JniMemoryManager {

  private static final Logger LOGGER = Logger.getLogger(JniMemoryManager.class.getName());

  /** Map to track active native handles and their associated metadata. */
  private static final ConcurrentHashMap<Long, ResourceInfo> ACTIVE_HANDLES =
      new ConcurrentHashMap<>();

  /** Counter for tracking the total number of allocated resources. */
  private static final AtomicLong TOTAL_ALLOCATED = new AtomicLong(0);

  /** Counter for tracking the total number of deallocated resources. */
  private static final AtomicLong TOTAL_DEALLOCATED = new AtomicLong(0);

  /** Private constructor to prevent instantiation of utility class. */
  private JniMemoryManager() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Registers a native handle for tracking.
   *
   * @param handle the native handle to track
   * @param resourceType the type of resource (e.g., "runtime", "engine", "module")
   * @param creationLocation optional location where the resource was created (for debugging)
   */
  public static void registerHandle(
      final long handle, final String resourceType, final String creationLocation) {
    if (handle == 0) {
      LOGGER.warning("Attempt to register null handle for type: " + resourceType);
      return;
    }

    final ResourceInfo info =
        new ResourceInfo(resourceType, creationLocation, System.currentTimeMillis());
    final ResourceInfo existing = ACTIVE_HANDLES.put(handle, info);

    if (existing != null) {
      LOGGER.warning(
          "Handle "
              + handle
              + " was already registered as "
              + existing.resourceType
              + ", now registering as "
              + resourceType);
    }

    TOTAL_ALLOCATED.incrementAndGet();
    LOGGER.fine("Registered native handle " + handle + " for " + resourceType);
  }

  /**
   * Registers a native handle for tracking without creation location.
   *
   * @param handle the native handle to track
   * @param resourceType the type of resource
   */
  public static void registerHandle(final long handle, final String resourceType) {
    registerHandle(handle, resourceType, null);
  }

  /**
   * Unregisters a native handle when it's properly cleaned up.
   *
   * @param handle the native handle to unregister
   * @return true if the handle was found and removed, false if it wasn't registered
   */
  public static boolean unregisterHandle(final long handle) {
    if (handle == 0) {
      return false;
    }

    final ResourceInfo info = ACTIVE_HANDLES.remove(handle);
    if (info != null) {
      TOTAL_DEALLOCATED.incrementAndGet();
      final long lifetime = System.currentTimeMillis() - info.creationTime;
      LOGGER.fine(
          "Unregistered native handle "
              + handle
              + " for "
              + info.resourceType
              + " (lifetime: "
              + lifetime
              + "ms)");
      return true;
    } else {
      LOGGER.warning("Attempt to unregister unknown handle: " + handle);
      return false;
    }
  }

  /**
   * Checks if a handle is currently registered.
   *
   * @param handle the handle to check
   * @return true if the handle is registered, false otherwise
   */
  public static boolean isHandleRegistered(final long handle) {
    return handle != 0 && ACTIVE_HANDLES.containsKey(handle);
  }

  /**
   * Gets the resource type for a registered handle.
   *
   * @param handle the handle to query
   * @return the resource type, or null if the handle is not registered
   */
  public static String getResourceType(final long handle) {
    final ResourceInfo info = ACTIVE_HANDLES.get(handle);
    return info != null ? info.resourceType : null;
  }

  /**
   * Gets the number of currently active (not yet cleaned up) handles.
   *
   * @return the number of active handles
   */
  public static int getActiveHandleCount() {
    return ACTIVE_HANDLES.size();
  }

  /**
   * Gets the total number of handles allocated since the application started.
   *
   * @return the total number of allocated handles
   */
  public static long getTotalAllocatedCount() {
    return TOTAL_ALLOCATED.get();
  }

  /**
   * Gets the total number of handles deallocated since the application started.
   *
   * @return the total number of deallocated handles
   */
  public static long getTotalDeallocatedCount() {
    return TOTAL_DEALLOCATED.get();
  }

  /**
   * Gets a summary of memory management statistics.
   *
   * @return a string containing memory management statistics
   */
  public static String getMemoryStats() {
    final int active = getActiveHandleCount();
    final long allocated = getTotalAllocatedCount();
    final long deallocated = getTotalDeallocatedCount();

    final StringBuilder stats = new StringBuilder();
    stats.append("JNI Memory Management Statistics:\n");
    stats.append("  Active handles: ").append(active).append("\n");
    stats.append("  Total allocated: ").append(allocated).append("\n");
    stats.append("  Total deallocated: ").append(deallocated).append("\n");
    stats.append("  Potential leaks: ").append(allocated - deallocated).append("\n");

    if (active > 0) {
      stats.append("  Active handle types:\n");
      final ConcurrentHashMap<String, Integer> typeCounts = new ConcurrentHashMap<>();
      ACTIVE_HANDLES
          .values()
          .forEach(
              info -> {
                typeCounts.merge(info.resourceType, 1, Integer::sum);
              });
      typeCounts.forEach(
          (type, count) -> {
            stats.append("    ").append(type).append(": ").append(count).append("\n");
          });
    }

    return stats.toString();
  }

  /**
   * Logs a warning for all currently active handles (potential memory leaks).
   *
   * <p>This method should typically be called during application shutdown to detect resource leaks.
   */
  public static void checkForLeaks() {
    final int activeCount = getActiveHandleCount();
    if (activeCount > 0) {
      LOGGER.warning("Potential memory leaks detected: " + activeCount + " active handles");
      ACTIVE_HANDLES.forEach(
          (handle, info) -> {
            final long lifetime = System.currentTimeMillis() - info.creationTime;
            LOGGER.warning(
                "  Leaked handle "
                    + handle
                    + " ("
                    + info.resourceType
                    + ", age: "
                    + lifetime
                    + "ms"
                    + (info.creationLocation != null
                        ? ", created at: " + info.creationLocation
                        : "")
                    + ")");
          });
    } else {
      LOGGER.info("No memory leaks detected - all native handles properly cleaned up");
    }
  }

  /**
   * Forces cleanup of all registered handles by attempting to call their cleanup methods.
   *
   * <p><strong>WARNING:</strong> This is an emergency cleanup method that should only be used
   * during shutdown. It may cause instability if resources are still in use.
   */
  public static void emergencyCleanup() {
    final int activeCount = getActiveHandleCount();
    if (activeCount == 0) {
      return;
    }

    LOGGER.severe("Performing emergency cleanup of " + activeCount + " active handles");

    // Create a copy to avoid concurrent modification
    final ConcurrentHashMap<Long, ResourceInfo> handlesCopy =
        new ConcurrentHashMap<>(ACTIVE_HANDLES);

    handlesCopy.forEach(
        (handle, info) -> {
          try {
            LOGGER.warning(
                "Emergency cleanup of handle " + handle + " (" + info.resourceType + ")");
            // Note: In a real implementation, you would call the appropriate native cleanup method
            // here
            // For now, just unregister the handle
            unregisterHandle(handle);
          } catch (final Exception e) {
            LOGGER.severe("Failed to cleanup handle " + handle + ": " + e.getMessage());
          }
        });

    LOGGER.warning("Emergency cleanup completed, " + getActiveHandleCount() + " handles remaining");
  }

  /** Information about a registered native resource. */
  private static final class ResourceInfo {
    final String resourceType;
    final String creationLocation;
    final long creationTime;

    ResourceInfo(
        final String resourceType, final String creationLocation, final long creationTime) {
      this.resourceType = resourceType;
      this.creationLocation = creationLocation;
      this.creationTime = creationTime;
    }
  }
}
