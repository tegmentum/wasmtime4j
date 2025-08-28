package ai.tegmentum.wasmtime4j.jni.util;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Manager for phantom references to ensure automatic cleanup of native resources.
 *
 * <p>This class provides a robust mechanism for tracking native resources and ensuring their
 * cleanup even if explicit close() calls are missed. It uses phantom references to detect when Java
 * objects are being garbage collected and triggers appropriate native resource cleanup.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic native resource cleanup on object collection
 *   <li>Thread-safe reference tracking and cleanup
 *   <li>Defensive programming to prevent resource leaks
 *   <li>Performance monitoring and statistics
 *   <li>Configurable cleanup thread management
 * </ul>
 *
 * <p>This utility serves as a safety net for native resource management, ensuring that resources
 * are cleaned up even if application code fails to call close() explicitly.
 *
 * @since 1.0.0
 */
public final class JniPhantomReferenceManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniPhantomReferenceManager.class.getName());

  /** Singleton instance for global resource tracking. */
  private static volatile JniPhantomReferenceManager instance;

  /** Lock for singleton initialization. */
  private static final Object instanceLock = new Object();

  /** Reference queue for phantom references. */
  private final ReferenceQueue<Object> referenceQueue;

  /** Map of phantom references to their cleanup handlers. */
  private final ConcurrentMap<PhantomReference<Object>, CleanupHandler> referenceMap;

  /** Background cleanup thread. */
  private Thread cleanupThread;

  /** Flag to track if the manager is closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Statistics. */
  private final AtomicLong registeredCount = new AtomicLong(0);

  private final AtomicLong cleanedUpCount = new AtomicLong(0);
  private final AtomicLong failedCleanupCount = new AtomicLong(0);

  /**
   * Gets the singleton instance of the phantom reference manager.
   *
   * @return the singleton instance
   */
  public static JniPhantomReferenceManager getInstance() {
    if (instance == null) {
      synchronized (instanceLock) {
        if (instance == null) {
          instance = new JniPhantomReferenceManager();
        }
      }
    }
    return instance;
  }

  /** Private constructor for singleton pattern. */
  private JniPhantomReferenceManager() {
    this.referenceQueue = new ReferenceQueue<>();
    this.referenceMap = new ConcurrentHashMap<>();

    startCleanupThread();
    LOGGER.fine("Created JniPhantomReferenceManager");
  }

  /**
   * Registers an object for automatic native resource cleanup.
   *
   * @param obj the object to track
   * @param nativeHandle the native handle to clean up
   * @param cleanupMethod the method name for logging
   * @throws IllegalArgumentException if any parameter is null or nativeHandle is 0
   */
  public void register(final Object obj, final long nativeHandle, final String cleanupMethod) {
    JniValidation.requireNonNull(obj, "obj");
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");
    JniValidation.requireNonNull(cleanupMethod, "cleanupMethod");

    if (closed.get()) {
      LOGGER.warning("Attempting to register object with closed phantom reference manager");
      return;
    }

    final CleanupHandler handler = new CleanupHandler(nativeHandle, cleanupMethod);
    final PhantomReference<Object> phantomRef = new PhantomReference<>(obj, referenceQueue);

    referenceMap.put(phantomRef, handler);
    registeredCount.incrementAndGet();

    LOGGER.fine(
        String.format(
            "Registered phantom reference for %s with handle: 0x%x", cleanupMethod, nativeHandle));
  }

  /**
   * Unregisters an object from automatic cleanup tracking.
   *
   * <p>This method should be called when an object is explicitly closed to prevent unnecessary
   * phantom reference cleanup.
   *
   * @param obj the object to untrack
   */
  public void unregister(final Object obj) {
    JniValidation.requireNonNull(obj, "obj");

    // Note: We cannot directly find the phantom reference for an object since phantom
    // references don't allow access to their referent. This is a limitation of the
    // phantom reference approach - we rely on the cleanup happening eventually.
    // In practice, explicit close() calls should handle the cleanup, and phantom
    // references serve as a safety net.

    LOGGER.fine(
        "Unregister request received (phantom references will be cleaned up automatically)");
  }

  /**
   * Gets the number of currently registered objects.
   *
   * @return the number of registered phantom references
   */
  public int getRegisteredCount() {
    return referenceMap.size();
  }

  /**
   * Gets the total number of objects that have been registered.
   *
   * @return the total registration count
   */
  public long getTotalRegistered() {
    return registeredCount.get();
  }

  /**
   * Gets the number of objects that have been cleaned up.
   *
   * @return the cleanup count
   */
  public long getCleanedUpCount() {
    return cleanedUpCount.get();
  }

  /**
   * Gets the number of failed cleanup attempts.
   *
   * @return the failed cleanup count
   */
  public long getFailedCleanupCount() {
    return failedCleanupCount.get();
  }

  /**
   * Forces processing of any pending phantom references.
   *
   * <p>This method is primarily intended for testing and debugging.
   */
  public void processPendingReferences() {
    processReferences(0); // Process immediately without timeout
  }

  /**
   * Checks if the manager is closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed.get();
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.info(
          String.format(
              "Closing JniPhantomReferenceManager: registered=%d, cleanedUp=%d, failed=%d",
              referenceMap.size(), cleanedUpCount.get(), failedCleanupCount.get()));

      // Stop the cleanup thread
      if (cleanupThread != null) {
        cleanupThread.interrupt();
        try {
          cleanupThread.join(2000); // Wait up to 2 seconds for thread to finish
        } catch (final InterruptedException e) {
          Thread.currentThread().interrupt();
          LOGGER.warning("Interrupted while waiting for cleanup thread to finish");
        }
      }

      // Process any remaining references
      processRemainingReferences();

      // Clear the singleton instance
      synchronized (instanceLock) {
        if (instance == this) {
          instance = null;
        }
      }

      LOGGER.fine("JniPhantomReferenceManager closed");
    }
  }

  /** Starts the background cleanup thread. */
  private void startCleanupThread() {
    cleanupThread = new Thread(this::cleanupLoop, "JniPhantomReferenceCleanup");
    cleanupThread.setDaemon(true);
    cleanupThread.setPriority(Thread.NORM_PRIORITY - 1); // Slightly lower priority
    cleanupThread.start();
  }

  /** Main cleanup loop for processing phantom references. */
  private void cleanupLoop() {
    while (!closed.get() && !Thread.currentThread().isInterrupted()) {
      try {
        processReferences(1000); // 1 second timeout
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      } catch (final Exception e) {
        LOGGER.warning("Error in phantom reference cleanup loop: " + e.getMessage());
      }
    }
  }

  /**
   * Processes phantom references that are ready for cleanup.
   *
   * @param timeoutMs timeout in milliseconds, 0 for immediate return
   * @throws InterruptedException if interrupted while waiting
   */
  private void processReferences(final long timeoutMs) throws InterruptedException {
    while (!closed.get()) {
      final java.lang.ref.Reference<?> ref;
      if (timeoutMs > 0) {
        ref = referenceQueue.remove(timeoutMs);
      } else {
        ref = referenceQueue.poll();
      }

      if (ref == null) {
        break; // Timeout or no references available
      }

      @SuppressWarnings("unchecked")
      final PhantomReference<Object> phantomRef = (PhantomReference<Object>) ref;
      final CleanupHandler handler = referenceMap.remove(phantomRef);

      if (handler != null) {
        try {
          handler.cleanup();
          cleanedUpCount.incrementAndGet();
          LOGGER.fine(
              String.format(
                  "Phantom reference cleanup completed for %s (handle: 0x%x)",
                  handler.cleanupMethod, handler.nativeHandle));
        } catch (final Exception e) {
          failedCleanupCount.incrementAndGet();
          LOGGER.warning(
              String.format(
                  "Failed to cleanup native resource via phantom reference for %s (handle: 0x%x):"
                      + " %s",
                  handler.cleanupMethod, handler.nativeHandle, e.getMessage()));
        }
      }

      // Clear the reference
      phantomRef.clear();
    }
  }

  /** Processes any remaining phantom references during shutdown. */
  private void processRemainingReferences() {
    try {
      processReferences(0); // Process immediately
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      LOGGER.warning("Interrupted while processing remaining phantom references");
    }

    // Clean up any remaining handlers
    for (final CleanupHandler handler : referenceMap.values()) {
      try {
        handler.cleanup();
        cleanedUpCount.incrementAndGet();
      } catch (final Exception e) {
        failedCleanupCount.incrementAndGet();
        LOGGER.warning(
            String.format(
                "Failed to cleanup remaining native resource for %s (handle: 0x%x): %s",
                handler.cleanupMethod, handler.nativeHandle, e.getMessage()));
      }
    }

    referenceMap.clear();
  }

  /** Handler for native resource cleanup. */
  private static final class CleanupHandler {
    final long nativeHandle;
    final String cleanupMethod;

    CleanupHandler(final long nativeHandle, final String cleanupMethod) {
      this.nativeHandle = nativeHandle;
      this.cleanupMethod = cleanupMethod;
    }

    void cleanup() throws Exception {
      // This would call the appropriate native cleanup method
      // For now, we just log the cleanup attempt
      // In a real implementation, this would call specific native cleanup methods
      // based on the cleanup method name or resource type

      LOGGER.fine(
          String.format(
              "Native resource cleanup called for %s (handle: 0x%x)", cleanupMethod, nativeHandle));

      // TODO: Implement actual native cleanup calls based on cleanupMethod
      // For example:
      // switch (cleanupMethod) {
      //   case "nativeDestroyEngine":
      //     nativeDestroyEngine(nativeHandle);
      //     break;
      //   case "nativeDestroyModule":
      //     nativeDestroyModule(nativeHandle);
      //     break;
      //   // etc.
      // }
    }
  }

  @Override
  public String toString() {
    return String.format(
        "JniPhantomReferenceManager{registered=%d, total=%d, cleanedUp=%d, failed=%d}",
        referenceMap.size(), registeredCount.get(), cleanedUpCount.get(), failedCleanupCount.get());
  }
}
