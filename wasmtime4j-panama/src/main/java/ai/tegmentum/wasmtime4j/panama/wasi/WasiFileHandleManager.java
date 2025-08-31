package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.exception.WasiFileSystemException;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Comprehensive file handle management and resource cleanup system for WASI file operations in
 * Panama FFI context.
 *
 * <p>This class provides robust file handle lifecycle management with automatic resource cleanup to
 * prevent resource leaks and ensure proper system resource management. Features include:
 *
 * <ul>
 *   <li>Automatic resource leak detection using phantom references
 *   <li>Scheduled cleanup of unused file handles
 *   <li>Resource usage tracking and limits enforcement
 *   <li>Handle validation and corruption detection
 *   <li>Background garbage collection of orphaned resources
 *   <li>Comprehensive metrics and monitoring
 * </ul>
 *
 * <p>All operations are designed with defensive programming principles to prevent JVM crashes and
 * ensure system stability even under resource pressure.
 *
 * @since 1.0.0
 */
public final class WasiFileHandleManager implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WasiFileHandleManager.class.getName());

  /** Default maximum number of open file handles. */
  private static final int DEFAULT_MAX_HANDLES = 1024;

  /** Default cleanup interval in seconds. */
  private static final int DEFAULT_CLEANUP_INTERVAL_SECONDS = 30;

  /** Default handle timeout in seconds. */
  private static final int DEFAULT_HANDLE_TIMEOUT_SECONDS = 300;

  /** Active file handles by descriptor. */
  private final Map<Integer, ManagedFileHandle> activeHandles = new ConcurrentHashMap<>();

  /** Phantom references for automatic cleanup. */
  private final Map<PhantomReference<WasiFileHandle>, Integer> phantomReferences =
      new ConcurrentHashMap<>();

  /** Reference queue for garbage collection notifications. */
  private final ReferenceQueue<WasiFileHandle> referenceQueue = new ReferenceQueue<>();

  /** Lock for thread-safe handle management. */
  private final ReadWriteLock handleLock = new ReentrantReadWriteLock();

  /** Scheduled executor for background cleanup tasks. */
  private final ScheduledExecutorService cleanupExecutor;

  /** Maximum number of open file handles allowed. */
  private final int maxHandles;

  /** Handle timeout in milliseconds. */
  private final long handleTimeoutMs;

  /** Next available file descriptor. */
  private final AtomicInteger nextFileDescriptor = new AtomicInteger(3);

  /** Total number of handles created. */
  private final AtomicLong totalHandlesCreated = new AtomicLong(0);

  /** Total number of handles closed. */
  private final AtomicLong totalHandlesClosed = new AtomicLong(0);

  /** Total number of handles cleaned up by garbage collection. */
  private final AtomicLong totalHandlesGarbageCollected = new AtomicLong(0);

  /** Background cleanup task. */
  private final ScheduledFuture<?> cleanupTask;

  /** Whether this manager has been shut down. */
  private volatile boolean shutdown = false;

  /** Creates a new file handle manager with default settings. */
  public WasiFileHandleManager() {
    this(DEFAULT_MAX_HANDLES, DEFAULT_HANDLE_TIMEOUT_SECONDS);
  }

  /**
   * Creates a new file handle manager with the specified settings.
   *
   * @param maxHandles the maximum number of open file handles
   * @param handleTimeoutSeconds the handle timeout in seconds
   */
  public WasiFileHandleManager(final int maxHandles, final int handleTimeoutSeconds) {
    PanamaValidation.requirePositive(maxHandles, "maxHandles");
    PanamaValidation.requirePositive(handleTimeoutSeconds, "handleTimeoutSeconds");

    this.maxHandles = maxHandles;
    this.handleTimeoutMs = handleTimeoutSeconds * 1000L;
    this.cleanupExecutor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              final Thread thread = new Thread(r, "WasiFileHandleManager-Cleanup");
              thread.setDaemon(true);
              return thread;
            });

    // Start background cleanup task
    this.cleanupTask =
        cleanupExecutor.scheduleAtFixedRate(
            this::performBackgroundCleanup,
            DEFAULT_CLEANUP_INTERVAL_SECONDS,
            DEFAULT_CLEANUP_INTERVAL_SECONDS,
            TimeUnit.SECONDS);

    LOGGER.info(
        String.format(
            "Created file handle manager: maxHandles=%d, timeout=%ds",
            maxHandles, handleTimeoutSeconds));
  }

  /**
   * Registers a new file handle for management.
   *
   * @param handle the file handle to register
   * @return the managed file handle wrapper
   * @throws WasiFileSystemException if the handle cannot be registered
   */
  public ManagedFileHandle registerHandle(final WasiFileHandle handle)
      throws WasiFileSystemException {
    PanamaValidation.requireNonNull(handle, "handle");

    if (shutdown) {
      throw new WasiFileSystemException("File handle manager is shut down", "EIO");
    }

    handleLock.writeLock().lock();
    try {
      // Check handle limits
      if (activeHandles.size() >= maxHandles) {
        throw new WasiFileSystemException("Too many open file handles", "EMFILE");
      }

      final int fileDescriptor = handle.getFileDescriptor();

      // Check for duplicate descriptor
      if (activeHandles.containsKey(fileDescriptor)) {
        throw new WasiFileSystemException(
            "File descriptor already in use: " + fileDescriptor, "EBADF");
      }

      final ManagedFileHandle managedHandle =
          new ManagedFileHandle(handle, System.currentTimeMillis());

      // Register handle
      activeHandles.put(fileDescriptor, managedHandle);

      // Create phantom reference for automatic cleanup
      final PhantomReference<WasiFileHandle> phantomRef =
          new PhantomReference<>(handle, referenceQueue);
      phantomReferences.put(phantomRef, fileDescriptor);

      totalHandlesCreated.incrementAndGet();

      LOGGER.fine(
          String.format(
              "Registered file handle: fd=%d, total=%d", fileDescriptor, activeHandles.size()));

      return managedHandle;

    } finally {
      handleLock.writeLock().unlock();
    }
  }

  /**
   * Gets a managed file handle by descriptor.
   *
   * @param fileDescriptor the file descriptor
   * @return the managed file handle
   * @throws WasiFileSystemException if the handle is not found or invalid
   */
  public ManagedFileHandle getHandle(final int fileDescriptor) throws WasiFileSystemException {
    if (shutdown) {
      throw new WasiFileSystemException("File handle manager is shut down", "EIO");
    }

    handleLock.readLock().lock();
    try {
      final ManagedFileHandle managedHandle = activeHandles.get(fileDescriptor);
      if (managedHandle == null) {
        throw new WasiFileSystemException("Invalid file descriptor: " + fileDescriptor, "EBADF");
      }

      // Update last access time
      managedHandle.updateLastAccess();

      return managedHandle;

    } finally {
      handleLock.readLock().unlock();
    }
  }

  /**
   * Unregisters and closes a file handle.
   *
   * @param fileDescriptor the file descriptor to close
   * @throws WasiFileSystemException if the handle cannot be closed
   */
  public void unregisterHandle(final int fileDescriptor) {
    if (shutdown) {
      return; // Silently ignore during shutdown
    }

    handleLock.writeLock().lock();
    try {
      final ManagedFileHandle managedHandle = activeHandles.remove(fileDescriptor);
      if (managedHandle == null) {
        LOGGER.warning(
            String.format("Attempted to unregister unknown file descriptor: %d", fileDescriptor));
        return;
      }

      // Close the handle
      try {
        managedHandle.getHandle().close();
        totalHandlesClosed.incrementAndGet();
      } catch (final Exception e) {
        LOGGER.warning(
            String.format(
                "Error closing file handle: fd=%d, error=%s", fileDescriptor, e.getMessage()));
      }

      // Remove phantom reference
      phantomReferences.entrySet().removeIf(entry -> entry.getValue().equals(fileDescriptor));

      LOGGER.fine(
          String.format(
              "Unregistered file handle: fd=%d, remaining=%d",
              fileDescriptor, activeHandles.size()));

    } finally {
      handleLock.writeLock().unlock();
    }
  }

  /**
   * Gets the current number of active file handles.
   *
   * @return the number of active handles
   */
  public int getActiveHandleCount() {
    handleLock.readLock().lock();
    try {
      return activeHandles.size();
    } finally {
      handleLock.readLock().unlock();
    }
  }

  /**
   * Gets comprehensive handle manager statistics.
   *
   * @return the handle manager statistics
   */
  public HandleManagerStats getStats() {
    handleLock.readLock().lock();
    try {
      return new HandleManagerStats(
          activeHandles.size(),
          maxHandles,
          totalHandlesCreated.get(),
          totalHandlesClosed.get(),
          totalHandlesGarbageCollected.get(),
          phantomReferences.size());
    } finally {
      handleLock.readLock().unlock();
    }
  }

  /**
   * Forces cleanup of all expired handles.
   *
   * @return the number of handles cleaned up
   */
  public int forceCleanup() {
    if (shutdown) {
      return 0;
    }

    return performExpiredHandleCleanup();
  }

  /** Closes the file handle manager and releases all resources. */
  @Override
  public void close() {
    if (shutdown) {
      return;
    }

    LOGGER.info("Shutting down file handle manager");
    shutdown = true;

    // Cancel cleanup task
    if (cleanupTask != null) {
      cleanupTask.cancel(false);
    }

    // Close all remaining handles
    handleLock.writeLock().lock();
    try {
      for (final Map.Entry<Integer, ManagedFileHandle> entry : activeHandles.entrySet()) {
        try {
          entry.getValue().getHandle().close();
        } catch (final Exception e) {
          LOGGER.warning(
              String.format(
                  "Error closing handle during shutdown: fd=%d, error=%s",
                  entry.getKey(), e.getMessage()));
        }
      }
      activeHandles.clear();
      phantomReferences.clear();
    } finally {
      handleLock.writeLock().unlock();
    }

    // Shutdown cleanup executor
    cleanupExecutor.shutdown();
    try {
      if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      cleanupExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("File handle manager shut down completed");
  }

  /** Performs background cleanup of expired and garbage collected handles. */
  private void performBackgroundCleanup() {
    if (shutdown) {
      return;
    }

    try {
      final int expiredHandles = performExpiredHandleCleanup();
      final int gcHandles = performGarbageCollectionCleanup();

      if (expiredHandles > 0 || gcHandles > 0) {
        LOGGER.fine(
            String.format(
                "Background cleanup completed: expired=%d, gc=%d", expiredHandles, gcHandles));
      }
    } catch (final Exception e) {
      LOGGER.warning(String.format("Error during background cleanup: %s", e.getMessage()));
    }
  }

  /** Cleans up expired handles based on timeout. */
  private int performExpiredHandleCleanup() {
    final long currentTime = System.currentTimeMillis();
    final Set<Integer> expiredDescriptors = ConcurrentHashMap.newKeySet();

    handleLock.readLock().lock();
    try {
      for (final Map.Entry<Integer, ManagedFileHandle> entry : activeHandles.entrySet()) {
        if (currentTime - entry.getValue().getLastAccessTime() > handleTimeoutMs) {
          expiredDescriptors.add(entry.getKey());
        }
      }
    } finally {
      handleLock.readLock().unlock();
    }

    // Close expired handles
    for (final Integer fileDescriptor : expiredDescriptors) {
      try {
        LOGGER.fine(String.format("Cleaning up expired handle: fd=%d", fileDescriptor));
        unregisterHandle(fileDescriptor);
      } catch (final Exception e) {
        LOGGER.warning(
            String.format(
                "Error cleaning up expired handle: fd=%d, error=%s",
                fileDescriptor, e.getMessage()));
      }
    }

    return expiredDescriptors.size();
  }

  /** Cleans up handles that have been garbage collected. */
  private int performGarbageCollectionCleanup() {
    int cleanedUp = 0;
    PhantomReference<WasiFileHandle> phantomRef;

    PhantomReference<WasiFileHandle> ref;
    while ((ref = castToPhantomReference(referenceQueue.poll())) != null) {
      phantomRef = ref;
      final Integer fileDescriptor = phantomReferences.remove(phantomRef);
      if (fileDescriptor != null) {
        try {
          LOGGER.fine(String.format("Cleaning up garbage collected handle: fd=%d", fileDescriptor));
          unregisterHandle(fileDescriptor);
          totalHandlesGarbageCollected.incrementAndGet();
          cleanedUp++;
        } catch (final Exception e) {
          LOGGER.warning(
              String.format(
                  "Error cleaning up garbage collected handle: fd=%d, error=%s",
                  fileDescriptor, e.getMessage()));
        }
      }
      phantomRef.clear();
    }

    return cleanedUp;
  }

  /** Managed file handle wrapper with lifecycle tracking. */
  public static final class ManagedFileHandle {
    private final WasiFileHandle handle;
    private volatile long lastAccessTime;

    private ManagedFileHandle(final WasiFileHandle handle, final long creationTime) {
      this.handle = handle;
      this.lastAccessTime = creationTime;
    }

    /**
     * Gets the underlying file handle.
     *
     * @return the file handle
     */
    public WasiFileHandle getHandle() {
      updateLastAccess();
      return handle;
    }

    /**
     * Gets the last access time.
     *
     * @return the last access time in milliseconds
     */
    public long getLastAccessTime() {
      return lastAccessTime;
    }

    /** Updates the last access time to the current time. */
    void updateLastAccess() {
      lastAccessTime = System.currentTimeMillis();
    }
  }

  /** Handle manager statistics. */
  public static final class HandleManagerStats {
    private final int activeHandles;
    private final int maxHandles;
    private final long totalHandlesCreated;
    private final long totalHandlesClosed;
    private final long totalHandlesGarbageCollected;
    private final int phantomReferences;

    private HandleManagerStats(
        final int activeHandles,
        final int maxHandles,
        final long totalHandlesCreated,
        final long totalHandlesClosed,
        final long totalHandlesGarbageCollected,
        final int phantomReferences) {
      this.activeHandles = activeHandles;
      this.maxHandles = maxHandles;
      this.totalHandlesCreated = totalHandlesCreated;
      this.totalHandlesClosed = totalHandlesClosed;
      this.totalHandlesGarbageCollected = totalHandlesGarbageCollected;
      this.phantomReferences = phantomReferences;
    }

    public int getActiveHandles() {
      return activeHandles;
    }

    public int getMaxHandles() {
      return maxHandles;
    }

    public long getTotalHandlesCreated() {
      return totalHandlesCreated;
    }

    public long getTotalHandlesClosed() {
      return totalHandlesClosed;
    }

    public long getTotalHandlesGarbageCollected() {
      return totalHandlesGarbageCollected;
    }

    public int getPhantomReferences() {
      return phantomReferences;
    }

    @Override
    public String toString() {
      return String.format(
          "HandleManagerStats{active=%d, max=%d, created=%d, closed=%d, gc=%d, phantom=%d}",
          activeHandles,
          maxHandles,
          totalHandlesCreated,
          totalHandlesClosed,
          totalHandlesGarbageCollected,
          phantomReferences);
    }
  }

  /**
   * Helper method to safely cast Reference to PhantomReference. This suppresses the unchecked cast
   * warning in one place.
   */
  @SuppressWarnings("unchecked")
  private PhantomReference<WasiFileHandle> castToPhantomReference(
      final Reference<? extends WasiFileHandle> ref) {
    return (PhantomReference<WasiFileHandle>) ref;
  }
}
