package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Base class for managing native resources in JNI implementations.
 *
 * <p>This class provides a foundation for managing native resources with automatic cleanup using
 * the AutoCloseable pattern. It implements defensive programming practices to prevent resource
 * leaks and double-free errors.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic resource cleanup via finalizer as safety net
 *   <li>Thread-safe resource state management
 *   <li>Defensive programming with validation checks
 *   <li>Logging for resource lifecycle debugging
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
public abstract class JniResource implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(JniResource.class.getName());

  /** The native handle/pointer for this resource. */
  protected final long nativeHandle;

  /** Flag to track if this resource has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Flag to track if cleanup was called from finalizer. */
  private final AtomicBoolean finalizedCleanup = new AtomicBoolean(false);

  /**
   * Creates a new JNI resource with the specified native handle.
   *
   * @param nativeHandle the native handle/pointer for this resource
   * @throws JniResourceException if the native handle is invalid
   */
  protected JniResource(final long nativeHandle) {
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");
    this.nativeHandle = nativeHandle;
    // Note: Avoid calling getResourceType() here to prevent 'this' escape warning
    LOGGER.fine(String.format("Created JNI resource with handle: 0x%x", nativeHandle));
  }

  /**
   * Gets the native handle for this resource.
   *
   * @return the native handle
   * @throws JniResourceException if the resource has been closed
   */
  public final long getNativeHandle() {
    ensureNotClosed();
    return nativeHandle;
  }

  /**
   * Checks if this resource has been closed.
   *
   * @return true if the resource is closed, false otherwise
   */
  public final boolean isClosed() {
    return closed.get();
  }

  /**
   * Ensures that this resource has not been closed.
   *
   * @throws JniResourceException if the resource has been closed
   */
  protected final void ensureNotClosed() {
    if (isClosed()) {
      throw new JniResourceException(
          String.format(
              "%s resource has been closed (handle: 0x%x)", getResourceType(), nativeHandle));
    }
  }

  /**
   * Closes this resource and releases any native resources.
   *
   * <p>This method is idempotent - calling it multiple times has no additional effect. It is
   * thread-safe and can be called from multiple threads simultaneously.
   */
  @Override
  public final void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        doClose();
        LOGGER.fine(
            String.format("Closed %s resource with handle: 0x%x", getResourceType(), nativeHandle));
      } catch (final Exception e) {
        LOGGER.warning(
            String.format(
                "Error closing %s resource with handle: 0x%x: %s",
                getResourceType(), nativeHandle, e.getMessage()));
        // Don't re-throw exceptions from close() to avoid issues in try-with-resources
      }
    }
  }

  /**
   * Finalizer that ensures resources are cleaned up if close() was not called explicitly.
   *
   * <p>This serves as a safety net to prevent resource leaks. Applications should not rely on
   * finalizers for resource cleanup but should call close() explicitly.
   */
  @Override
  @SuppressWarnings("deprecation") // Finalizers are deprecated but still needed for safety
  protected final void finalize() throws Throwable {
    try {
      if (!isClosed() && finalizedCleanup.compareAndSet(false, true)) {
        LOGGER.warning(
            String.format(
                "Finalizing unclosed %s resource with handle: 0x%x - close() should be called"
                    + " explicitly",
                getResourceType(), nativeHandle));
        close();
      }
    } finally {
      super.finalize();
    }
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>This method is called by {@link #close()} and should release any native resources associated
   * with this object. It will only be called once, even if close() is called multiple times.
   *
   * <p>Implementations should be defensive and handle cases where the native resource may have
   * already been freed or is invalid.
   *
   * @throws Exception if there's an error during cleanup
   */
  protected abstract void doClose() throws Exception;

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name (e.g., "Engine", "Module", "Instance")
   */
  protected abstract String getResourceType();

  @Override
  public String toString() {
    return String.format("%s{handle=0x%x, closed=%s}", getResourceType(), nativeHandle, isClosed());
  }
}
