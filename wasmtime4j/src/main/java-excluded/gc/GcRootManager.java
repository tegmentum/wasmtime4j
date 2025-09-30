package ai.tegmentum.wasmtime4j.gc;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * GC root management system for WebAssembly GC objects.
 *
 * <p>Manages root references to prevent premature collection of WebAssembly GC objects and
 * coordinates finalization with the Java garbage collector. Provides thread-safe root registration
 * and automatic cleanup.
 *
 * @since 1.0.0
 */
public final class GcRootManager {

  private static final Logger LOGGER = Logger.getLogger(GcRootManager.class.getName());

  /** Global instance of the root manager. */
  private static volatile GcRootManager instance;

  /** Root references to prevent collection. */
  private final Set<GcObject> roots = ConcurrentHashMap.newKeySet();

  /** Weak references for cleanup tracking. */
  private final Set<WeakReference<GcObject>> weakRefs = ConcurrentHashMap.newKeySet();

  /** Phantom references for finalization. */
  private final Set<PhantomReference<GcObject>> phantomRefs = ConcurrentHashMap.newKeySet();

  /** Reference queue for cleanup notifications. */
  private final ReferenceQueue<GcObject> referenceQueue = new ReferenceQueue<>();

  /** Cleanup executor service. */
  private final ScheduledExecutorService cleanupExecutor =
      Executors.newScheduledThreadPool(
          1,
          r -> {
            Thread t = new Thread(r, "GC-Root-Manager-Cleanup");
            t.setDaemon(true);
            return t;
          });

  /** Statistics counters. */
  private final AtomicLong rootsAdded = new AtomicLong(0);

  private final AtomicLong rootsRemoved = new AtomicLong(0);
  private final AtomicLong objectsFinalized = new AtomicLong(0);

  private GcRootManager() {
    startCleanupThread();
  }

  /**
   * Gets the global GC root manager instance.
   *
   * @return the root manager instance
   */
  public static GcRootManager getInstance() {
    if (instance == null) {
      synchronized (GcRootManager.class) {
        if (instance == null) {
          instance = new GcRootManager();
        }
      }
    }
    return instance;
  }

  /**
   * Adds a GC object as a root to prevent collection.
   *
   * @param object the object to add as root
   * @throws IllegalArgumentException if object is null
   */
  public void addRoot(final GcObject object) {
    if (object == null) {
      throw new IllegalArgumentException("Root object cannot be null");
    }

    if (roots.add(object)) {
      rootsAdded.incrementAndGet();
      LOGGER.fine("Added GC root: " + object);

      // Create phantom reference for finalization tracking
      final PhantomReference<GcObject> phantomRef = new PhantomReference<>(object, referenceQueue);
      phantomRefs.add(phantomRef);
    }
  }

  /**
   * Removes a GC object from the roots.
   *
   * @param object the object to remove from roots
   * @return true if the object was a root and was removed
   */
  public boolean removeRoot(final GcObject object) {
    if (object == null) {
      return false;
    }

    final boolean removed = roots.remove(object);
    if (removed) {
      rootsRemoved.incrementAndGet();
      LOGGER.fine("Removed GC root: " + object);
    }
    return removed;
  }

  /**
   * Checks if an object is currently a root.
   *
   * @param object the object to check
   * @return true if the object is a root
   */
  public boolean isRoot(final GcObject object) {
    return object != null && roots.contains(object);
  }

  /**
   * Gets the current number of root objects.
   *
   * @return the number of root objects
   */
  public int getRootCount() {
    return roots.size();
  }

  /**
   * Creates a weak reference to a GC object for tracking without preventing collection.
   *
   * @param object the object to create a weak reference for
   * @return the weak reference
   * @throws IllegalArgumentException if object is null
   */
  public WeakReference<GcObject> createWeakReference(final GcObject object) {
    if (object == null) {
      throw new IllegalArgumentException("Object cannot be null");
    }

    final WeakReference<GcObject> weakRef = new WeakReference<>(object, referenceQueue);
    weakRefs.add(weakRef);
    return weakRef;
  }

  /**
   * Forces cleanup of collected references.
   *
   * @return the number of references cleaned up
   */
  public int cleanup() {
    int cleaned = 0;

    // Process reference queue
    java.lang.ref.Reference<? extends GcObject> ref;
    while ((ref = referenceQueue.poll()) != null) {
      if (ref instanceof WeakReference) {
        weakRefs.remove(ref);
        cleaned++;
      } else if (ref instanceof PhantomReference) {
        phantomRefs.remove(ref);
        objectsFinalized.incrementAndGet();
        cleaned++;
      }
    }

    return cleaned;
  }

  /**
   * Gets root manager statistics.
   *
   * @return the statistics
   */
  public RootManagerStats getStats() {
    return new RootManagerStats(
        rootsAdded.get(),
        rootsRemoved.get(),
        objectsFinalized.get(),
        getRootCount(),
        weakRefs.size(),
        phantomRefs.size());
  }

  /** Shuts down the root manager and cleanup resources. */
  public void shutdown() {
    cleanupExecutor.shutdown();
    try {
      if (!cleanupExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
        cleanupExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      cleanupExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    roots.clear();
    weakRefs.clear();
    phantomRefs.clear();
  }

  private void startCleanupThread() {
    cleanupExecutor.scheduleWithFixedDelay(
        () -> {
          try {
            final int cleaned = cleanup();
            if (cleaned > 0) {
              LOGGER.fine("Cleaned up " + cleaned + " collected references");
            }
          } catch (Exception e) {
            LOGGER.warning("Error during reference cleanup: " + e.getMessage());
          }
        },
        1,
        1,
        TimeUnit.SECONDS);
  }

  /** Statistics for the root manager. */
  public static final class RootManagerStats {
    private final long rootsAdded;
    private final long rootsRemoved;
    private final long objectsFinalized;
    private final int currentRootCount;
    private final int currentWeakRefCount;
    private final int currentPhantomRefCount;

    RootManagerStats(
        final long rootsAdded,
        final long rootsRemoved,
        final long objectsFinalized,
        final int currentRootCount,
        final int currentWeakRefCount,
        final int currentPhantomRefCount) {
      this.rootsAdded = rootsAdded;
      this.rootsRemoved = rootsRemoved;
      this.objectsFinalized = objectsFinalized;
      this.currentRootCount = currentRootCount;
      this.currentWeakRefCount = currentWeakRefCount;
      this.currentPhantomRefCount = currentPhantomRefCount;
    }

    public long getRootsAdded() {
      return rootsAdded;
    }

    public long getRootsRemoved() {
      return rootsRemoved;
    }

    public long getObjectsFinalized() {
      return objectsFinalized;
    }

    public int getCurrentRootCount() {
      return currentRootCount;
    }

    public int getCurrentWeakRefCount() {
      return currentWeakRefCount;
    }

    public int getCurrentPhantomRefCount() {
      return currentPhantomRefCount;
    }

    @Override
    public String toString() {
      return "RootManagerStats{"
          + "rootsAdded="
          + rootsAdded
          + ", rootsRemoved="
          + rootsRemoved
          + ", objectsFinalized="
          + objectsFinalized
          + ", currentRootCount="
          + currentRootCount
          + ", currentWeakRefCount="
          + currentWeakRefCount
          + ", currentPhantomRefCount="
          + currentPhantomRefCount
          + '}';
    }
  }
}
