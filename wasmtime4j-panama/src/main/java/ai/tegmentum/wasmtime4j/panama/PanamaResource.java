package ai.tegmentum.wasmtime4j.panama.util;

import ai.tegmentum.wasmtime4j.panama.exception.PanamaResourceException;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Base class for managing native resources in Panama FFI implementations.
 *
 * <p>This class provides a foundation for managing native resources with automatic cleanup using
 * the AutoCloseable pattern. It implements defensive programming practices to prevent resource
 * leaks and double-free errors.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource cleanup via phantom references (safer than finalizers)
 *   <li>Thread-safe resource state management with synchronization
 *   <li>Defensive programming with validation checks
 *   <li>Double-free protection to prevent JVM crashes
 *   <li>Arena-based memory management for Panama FFI
 *   <li>Centralized native library loading
 * </ul>
 *
 * <p>Subclasses must implement:
 *
 * <ul>
 *   <li>{@link #doClose()} - Actual native resource cleanup
 *   <li>{@link #getResourceType()} - Resource type for logging
 * </ul>
 *
 * @since 1.0.0
 */
public abstract class PanamaResource implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PanamaResource.class.getName());

  /**
   * Reference queue for phantom references to trigger native resource cleanup when objects are
   * garbage collected.
   */
  private static final ReferenceQueue<PanamaResource> REFERENCE_QUEUE = new ReferenceQueue<>();

  /** Map to track phantom references and their associated cleanup data. */
  private static final ConcurrentHashMap<PhantomReference<PanamaResource>, ResourceCleanup>
      PHANTOM_REFS = new ConcurrentHashMap<>();

  /** Cleanup thread that processes phantom references to free native resources. */
  private static final Thread CLEANUP_THREAD;

  /** Cached native library for all Panama implementations. */
  private static volatile SymbolLookup nativeLibrary;

  static {
    CLEANUP_THREAD = new Thread(PanamaResource::processCleanupQueue, "Panama-Resource-Cleanup");
    CLEANUP_THREAD.setDaemon(true);
    CLEANUP_THREAD.start();
  }

  /** The native memory segment for this resource. */
  protected final MemorySegment nativeHandle;

  /** Flag to track if this resource has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Phantom reference for safe automatic cleanup. */
  private final PhantomReference<PanamaResource> phantomRef;

  /** Data holder for cleanup information associated with phantom references. */
  private static final class ResourceCleanup {
    final MemorySegment nativeHandle;
    final String resourceType;
    final PanamaResourceCleanup cleanup;

    ResourceCleanup(
        final MemorySegment nativeHandle,
        final String resourceType,
        final PanamaResourceCleanup cleanup) {
      this.nativeHandle = nativeHandle;
      this.resourceType = resourceType;
      this.cleanup = cleanup;
    }
  }

  /** Interface for native resource cleanup operations. */
  @FunctionalInterface
  private interface PanamaResourceCleanup {
    void cleanup(MemorySegment nativeHandle) throws Exception;
  }

  /**
   * Creates a new Panama resource with the specified native handle.
   *
   * @param nativeHandle the native memory segment for this resource
   * @throws PanamaResourceException if the native handle is invalid
   */
  @SuppressWarnings("this-escape")
  protected PanamaResource(final MemorySegment nativeHandle) {
    PanamaValidation.requireNonNull(nativeHandle, "nativeHandle");
    this.nativeHandle = nativeHandle;

    // Set up phantom reference for automatic cleanup
    final ResourceCleanup cleanupData =
        new ResourceCleanup(nativeHandle, getClass().getSimpleName(), this::doCleanupSafely);
    final PhantomReference<PanamaResource> phantom = new PhantomReference<>(this, REFERENCE_QUEUE);
    this.phantomRef = phantom;
    PHANTOM_REFS.put(this.phantomRef, cleanupData);

    LOGGER.fine(String.format("Created Panama resource with handle: %s", nativeHandle));
  }

  /**
   * Gets the native handle for this resource.
   *
   * @return the native memory segment
   * @throws PanamaResourceException if the resource has been closed
   */
  public final MemorySegment getNativeHandle() throws PanamaResourceException {
    ensureNotClosed();
    return nativeHandle;
  }

  /**
   * Gets the shared native library for all Panama implementations.
   *
   * @return the native library symbol lookup
   */
  public static synchronized SymbolLookup getNativeLibrary() {
    if (nativeLibrary == null) {
      nativeLibrary = SymbolLookup.loaderLookup();
    }
    return nativeLibrary;
  }

  /**
   * Checks if this resource has been closed.
   *
   * @return true if the resource is closed, false otherwise
   */
  public final boolean isClosed() {
    return closed.get();
  }

  /** Marks this resource as closed for testing purposes without calling native cleanup. */
  public void markClosedForTesting() {
    closed.set(true);
  }

  /**
   * Ensures that this resource has not been closed.
   *
   * @throws PanamaResourceException if the resource has been closed
   */
  protected final void ensureNotClosed() throws PanamaResourceException {
    if (isClosed()) {
      throw new PanamaResourceException(
          String.format(
              "%s resource has been closed (handle: %s)", getResourceType(), nativeHandle));
    }
  }

  @Override
  public final void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        doClose();
        LOGGER.fine(
            String.format("Closed %s resource with handle: %s", getResourceType(), nativeHandle));
      } catch (final Exception e) {
        LOGGER.warning(
            String.format(
                "Error closing %s resource with handle: %s - %s",
                getResourceType(), nativeHandle, e.getMessage()));
      } finally {
        // Clean up phantom reference
        if (phantomRef != null) {
          PHANTOM_REFS.remove(phantomRef);
          phantomRef.clear();
        }
      }
    }
  }

  /**
   * Performs the actual resource cleanup. Subclasses must implement this method to handle native
   * resource cleanup.
   *
   * @throws Exception if cleanup fails
   */
  protected abstract void doClose() throws Exception;

  /**
   * Returns the resource type name for logging purposes.
   *
   * @return the resource type name
   */
  protected abstract String getResourceType();

  /**
   * Safe cleanup wrapper that handles exceptions during cleanup.
   *
   * @param nativeHandle the native handle to clean up
   */
  private void doCleanupSafely(final MemorySegment nativeHandle) {
    try {
      doClose();
    } catch (final Exception e) {
      LOGGER.warning(
          String.format(
              "Error during phantom cleanup of %s resource: %s",
              getResourceType(), e.getMessage()));
    }
  }

  /** Processes the cleanup queue to free native resources when objects are garbage collected. */
  private static void processCleanupQueue() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        @SuppressWarnings("unchecked")
        final PhantomReference<PanamaResource> ref =
            (PhantomReference<PanamaResource>) REFERENCE_QUEUE.remove();
        final ResourceCleanup cleanup = PHANTOM_REFS.remove(ref);
        if (cleanup != null) {
          try {
            cleanup.cleanup.cleanup(cleanup.nativeHandle);
            LOGGER.fine(
                String.format("Phantom cleanup completed for %s resource", cleanup.resourceType));
          } catch (final Exception e) {
            LOGGER.warning(
                String.format(
                    "Error during phantom cleanup of %s resource: %s",
                    cleanup.resourceType, e.getMessage()));
          }
        }
        ref.clear();
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        break;
      }
    }
  }
}
