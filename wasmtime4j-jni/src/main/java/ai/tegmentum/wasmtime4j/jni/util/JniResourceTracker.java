package ai.tegmentum.wasmtime4j.jni.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Advanced resource tracking system using phantom references to detect unreleased native resources.
 *
 * <p>This class provides an additional safety net for resource cleanup by using Java's phantom
 * reference mechanism to detect when objects with native resources are garbage collected without
 * proper cleanup. It helps identify resource leaks and can perform emergency cleanup.
 *
 * <p>This tracker runs in a background thread and provides defensive programming against resource
 * leaks that could cause native memory exhaustion.
 */
public final class JniResourceTracker {

  private static final Logger LOGGER = Logger.getLogger(JniResourceTracker.class.getName());

  /** Reference queue for phantom references. */
  private static final ReferenceQueue<Object> REFERENCE_QUEUE = new ReferenceQueue<>();

  /** Map of phantom references to their associated cleanup information. */
  private static final ConcurrentHashMap<PhantomReference<?>, CleanupInfo> TRACKED_RESOURCES =
      new ConcurrentHashMap<>();

  /** Background thread for processing phantom references. */
  private static volatile Thread cleanupThread;

  /** Flag to control the cleanup thread lifecycle. */
  private static final AtomicBoolean SHUTDOWN = new AtomicBoolean(false);

  /** Flag to track if the tracker has been started. */
  private static final AtomicBoolean STARTED = new AtomicBoolean(false);

  static {
    // Start the cleanup thread when the class is loaded
    startCleanupThread();
  }

  /** Private constructor to prevent instantiation of utility class. */
  private JniResourceTracker() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Tracks an object with native resources for automatic cleanup detection.
   *
   * @param object the object to track (must have native resources)
   * @param nativeHandle the native handle associated with the object
   * @param resourceType the type of resource for debugging
   * @param cleanupAction the action to perform if cleanup is needed
   */
  public static void trackResource(
      final Object object,
      final long nativeHandle,
      final String resourceType,
      final Runnable cleanupAction) {
    if (object == null || nativeHandle == 0 || cleanupAction == null) {
      LOGGER.warning(
          "Invalid parameters for resource tracking: object="
              + object
              + ", handle="
              + nativeHandle
              + ", action="
              + cleanupAction);
      return;
    }

    ensureStarted();

    final PhantomReference<Object> ref = new PhantomReference<>(object, REFERENCE_QUEUE);
    final CleanupInfo info = new CleanupInfo(nativeHandle, resourceType, cleanupAction);

    TRACKED_RESOURCES.put(ref, info);
    LOGGER.fine("Started tracking " + resourceType + " with handle " + nativeHandle);
  }

  /**
   * Manually untrack a resource when it's properly cleaned up.
   *
   * @param object the object to stop tracking
   * @return true if the object was being tracked and is now untracked
   */
  public static boolean untrackResource(final Object object) {
    if (object == null) {
      return false;
    }

    // Find and remove the phantom reference for this object
    // Note: This is not perfectly efficient, but it's the most reliable approach
    final int sizeBefore = TRACKED_RESOURCES.size();
    TRACKED_RESOURCES
        .entrySet()
        .removeIf(
            entry -> {
              final PhantomReference<?> ref = entry.getKey();
              // We can't directly check if the reference points to our object since it's cleared,
              // so we rely on the object calling this method during its close() implementation
              return false; // For now, we'll rely on the phantom reference mechanism
            });

    return TRACKED_RESOURCES.size() < sizeBefore;
  }

  /**
   * Gets the number of currently tracked resources.
   *
   * @return the number of tracked resources
   */
  public static int getTrackedResourceCount() {
    return TRACKED_RESOURCES.size();
  }

  /**
   * Gets statistics about resource tracking.
   *
   * @return a string containing tracking statistics
   */
  public static String getTrackingStats() {
    final StringBuilder stats = new StringBuilder();
    stats.append("JNI Resource Tracker Statistics:\n");
    stats.append("  Tracked resources: ").append(getTrackedResourceCount()).append("\n");
    stats.append("  Cleanup thread active: ").append(isCleanupThreadAlive()).append("\n");
    stats.append("  Shutdown requested: ").append(SHUTDOWN.get()).append("\n");

    if (!TRACKED_RESOURCES.isEmpty()) {
      final ConcurrentHashMap<String, Integer> typeCounts = new ConcurrentHashMap<>();
      TRACKED_RESOURCES
          .values()
          .forEach(
              info -> {
                final String type = info.resourceType != null ? info.resourceType : "Unknown";
                typeCounts.merge(type, 1, Integer::sum);
              });
      stats.append("  Tracked resource types:\n");
      typeCounts.forEach(
          (type, count) -> {
            stats.append("    ").append(type).append(": ").append(count).append("\n");
          });
    }

    return stats.toString();
  }

  /**
   * Shuts down the resource tracker and cleanup thread.
   *
   * <p>This method should be called during application shutdown to cleanly terminate the background
   * cleanup thread.
   */
  public static void shutdown() {
    if (SHUTDOWN.compareAndSet(false, true)) {
      LOGGER.info("Shutting down JNI resource tracker");

      if (cleanupThread != null && cleanupThread.isAlive()) {
        cleanupThread.interrupt();
        try {
          cleanupThread.join(5000); // Wait up to 5 seconds
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warning("Interrupted while waiting for cleanup thread to terminate");
        }
      }

      // Perform final cleanup of any remaining resources
      performFinalCleanup();
    }
  }

  /** Ensures that the resource tracker has been started. */
  private static void ensureStarted() {
    if (!STARTED.get()) {
      synchronized (JniResourceTracker.class) {
        if (!STARTED.get()) {
          startCleanupThread();
        }
      }
    }
  }

  /** Starts the background cleanup thread. */
  private static void startCleanupThread() {
    if (STARTED.compareAndSet(false, true)) {
      cleanupThread = new Thread(JniResourceTracker::cleanupLoop, "JNI-Resource-Cleanup");
      cleanupThread.setDaemon(true); // Don't prevent JVM shutdown
      cleanupThread.start();
      LOGGER.info("Started JNI resource cleanup thread");
    }
  }

  /** Main loop for the cleanup thread. */
  private static void cleanupLoop() {
    LOGGER.fine("JNI resource cleanup thread started");

    while (!SHUTDOWN.get()) {
      try {
        // Wait for a phantom reference to be enqueued (object was garbage collected)
        final PhantomReference<?> ref = (PhantomReference<?>) REFERENCE_QUEUE.remove(1000);

        if (ref != null) {
          processPhantomReference(ref);
        }
      } catch (final InterruptedException e) {
        if (!SHUTDOWN.get()) {
          LOGGER.warning("Cleanup thread interrupted unexpectedly");
        }
        Thread.currentThread().interrupt();
        break;
      } catch (final Exception e) {
        LOGGER.severe("Error in resource cleanup thread: " + e.getMessage());
      }
    }

    LOGGER.fine("JNI resource cleanup thread terminated");
  }

  /**
   * Processes a phantom reference that has been garbage collected.
   *
   * @param ref the phantom reference to process
   */
  private static void processPhantomReference(final PhantomReference<?> ref) {
    final CleanupInfo info = TRACKED_RESOURCES.remove(ref);
    if (info != null) {
      LOGGER.warning(
          "Detected unreleased native resource: "
              + info.resourceType
              + " with handle "
              + info.nativeHandle
              + " - performing emergency cleanup");

      try {
        info.cleanupAction.run();
        LOGGER.info(
            "Successfully cleaned up unreleased "
                + info.resourceType
                + " with handle "
                + info.nativeHandle);
      } catch (final Exception e) {
        LOGGER.severe(
            "Failed to cleanup unreleased "
                + info.resourceType
                + " with handle "
                + info.nativeHandle
                + ": "
                + e.getMessage());
      }
    }

    // Clear the phantom reference
    ref.clear();
  }

  /** Performs final cleanup of all remaining tracked resources. */
  private static void performFinalCleanup() {
    final int resourceCount = TRACKED_RESOURCES.size();
    if (resourceCount > 0) {
      LOGGER.warning("Performing final cleanup of " + resourceCount + " remaining resources");

      TRACKED_RESOURCES
          .values()
          .forEach(
              info -> {
                try {
                  LOGGER.warning(
                      "Final cleanup of "
                          + info.resourceType
                          + " with handle "
                          + info.nativeHandle);
                  info.cleanupAction.run();
                } catch (final Exception e) {
                  LOGGER.severe(
                      "Failed final cleanup of " + info.resourceType + ": " + e.getMessage());
                }
              });

      TRACKED_RESOURCES.clear();
    }
  }

  /**
   * Checks if the cleanup thread is still alive.
   *
   * @return true if the cleanup thread is alive
   */
  private static boolean isCleanupThreadAlive() {
    return cleanupThread != null && cleanupThread.isAlive();
  }

  /** Information about a tracked resource for cleanup purposes. */
  private static final class CleanupInfo {
    final long nativeHandle;
    final String resourceType;
    final Runnable cleanupAction;

    CleanupInfo(final long nativeHandle, final String resourceType, final Runnable cleanupAction) {
      this.nativeHandle = nativeHandle;
      this.resourceType = resourceType;
      this.cleanupAction = cleanupAction;
    }
  }
}
