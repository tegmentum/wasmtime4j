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
package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.util.Validation;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
 *   <li>Automatic resource cleanup via phantom references (safer than finalizers)
 *   <li>Thread-safe resource state management with synchronization
 *   <li>Defensive programming with validation checks
 *   <li>Double-free protection to prevent JVM crashes
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

  /**
   * Reference queue for phantom references to trigger native resource cleanup when objects are
   * garbage collected.
   */
  private static final ReferenceQueue<JniResource> REFERENCE_QUEUE = new ReferenceQueue<>();

  /** Map to track phantom references and their associated cleanup data. */
  private static final ConcurrentHashMap<PhantomReference<JniResource>, ResourceCleanup>
      PHANTOM_REFS = new ConcurrentHashMap<>();

  /** Cleanup thread that processes phantom references to free native resources. */
  private static final Thread CLEANUP_THREAD;

  static {
    CLEANUP_THREAD = new Thread(JniResource::processCleanupQueue, "JNI-Resource-Cleanup");
    CLEANUP_THREAD.setDaemon(true);
    CLEANUP_THREAD.start();
  }

  /** The native handle/pointer for this resource. */
  protected final long nativeHandle;

  /** Flag to track if this resource has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Read-write lock protecting against use-after-free race conditions. Operations acquire the read
   * lock (shared, non-exclusive), while {@link #close()} acquires the write lock (exclusive). This
   * prevents the native handle from being freed while operations are in-flight.
   */
  private final ReentrantReadWriteLock closeLock = new ReentrantReadWriteLock();

  /** Phantom reference for safe automatic cleanup. */
  private final PhantomReference<JniResource> phantomRef;

  /** Data holder for cleanup information associated with phantom references. */
  private static final class ResourceCleanup {
    final long nativeHandle;
    final String resourceType;
    final JniResourceCleanup cleanup;

    ResourceCleanup(
        final long nativeHandle, final String resourceType, final JniResourceCleanup cleanup) {
      this.nativeHandle = nativeHandle;
      this.resourceType = resourceType;
      this.cleanup = cleanup;
    }
  }

  /** Interface for native resource cleanup operations. */
  @FunctionalInterface
  private interface JniResourceCleanup {
    void cleanup(long nativeHandle) throws Exception;
  }

  /**
   * Creates a new JNI resource with the specified native handle.
   *
   * @param nativeHandle the native handle/pointer for this resource
   * @throws JniResourceException if the native handle is invalid
   */
  @SuppressWarnings("this-escape")
  @SuppressFBWarnings(
      value = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR",
      justification =
          "Method reference this::doCleanupSafely is stored for later execution, not called in"
              + " constructor. The actual execution happens during phantom reference cleanup after"
              + " object is fully initialized.")
  protected JniResource(final long nativeHandle) {
    Validation.requireValidHandle(nativeHandle, "nativeHandle");
    this.nativeHandle = nativeHandle;

    // Set up phantom reference for automatic cleanup
    final ResourceCleanup cleanupData =
        new ResourceCleanup(
            nativeHandle,
            getClass()
                .getSimpleName(), // Use class name instead of getResourceType() to avoid 'this'
            // escape
            this::doCleanupSafely);
    final PhantomReference<JniResource> phantom = new PhantomReference<>(this, REFERENCE_QUEUE);
    this.phantomRef = phantom;
    PHANTOM_REFS.put(this.phantomRef, cleanupData);

    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine(String.format("Created JNI resource with handle: 0x%x", nativeHandle));
    }
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
   * Checks whether a native handle value looks like a plausible heap pointer.
   *
   * <p>Test handles like 0x1, 0x1111, 0x2222 are small values that cannot be real heap pointers.
   * Real native pointers on 64-bit systems are typically above 0x100000000 (4 GB).
   *
   * @param handle the native handle to check
   * @return true if the handle is nonzero and above the 4 GB threshold
   */
  public static boolean isNativeHandleReasonable(final long handle) {
    if (handle == 0) {
      return false;
    }
    final long minReasonablePtr = 0x100000000L;
    return handle >= minReasonablePtr;
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
   * Marks this resource as closed for testing purposes without calling native cleanup. This method
   * should only be used in unit tests to simulate closed resource behavior without requiring actual
   * native resources.
   */
  public void markClosedForTesting() {
    closed.set(true);
  }

  /**
   * Ensures that this resource has not been closed.
   *
   * <p><b>Warning:</b> This method does NOT acquire the close lock. For thread-safe operation
   * guarding, use {@link #beginOperation()} / {@link #endOperation()} instead to prevent
   * use-after-free race conditions with concurrent {@link #close()} calls.
   *
   * @throws IllegalStateException if the resource has been closed
   */
  protected final void ensureNotClosed() {
    if (isClosed()) {
      throw new IllegalStateException(
          String.format(
              "%s resource has been closed (handle: 0x%x)", getResourceType(), nativeHandle));
    }
  }

  /**
   * Begins a guarded operation by acquiring the read lock and verifying the resource is not closed.
   *
   * <p>Every call to {@code beginOperation()} <b>must</b> be paired with {@link #endOperation()} in
   * a {@code finally} block:
   *
   * <pre>{@code
   * beginOperation();
   * try {
   *     return nativeDoSomething(nativeHandle);
   * } finally {
   *     endOperation();
   * }
   * }</pre>
   *
   * @throws IllegalStateException if the resource has been closed
   */
  protected final void beginOperation() {
    closeLock.readLock().lock();
    if (closed.get()) {
      closeLock.readLock().unlock();
      throw new IllegalStateException(
          String.format(
              "%s resource has been closed (handle: 0x%x)", getResourceType(), nativeHandle));
    }
  }

  /**
   * Ends a guarded operation by releasing the read lock.
   *
   * <p>Must be called in a {@code finally} block after {@link #beginOperation()}.
   */
  protected final void endOperation() {
    closeLock.readLock().unlock();
  }

  /**
   * Attempts to begin a guarded operation. Returns {@code false} if the resource is closed, without
   * throwing. If this returns {@code true}, the caller <b>must</b> call {@link #endOperation()} in
   * a {@code finally} block.
   *
   * <p>Use this for methods that return a default value when closed instead of throwing.
   *
   * @return {@code true} if the operation can proceed (lock acquired), {@code false} if closed
   */
  protected final boolean tryBeginOperation() {
    closeLock.readLock().lock();
    if (closed.get()) {
      closeLock.readLock().unlock();
      return false;
    }
    return true;
  }

  /**
   * Closes this resource and releases any native resources.
   *
   * <p>This method is idempotent - calling it multiple times has no additional effect. It is
   * thread-safe and can be called from multiple threads simultaneously.
   */
  @Override
  public final void close() {
    closeLock.writeLock().lock();
    try {
      if (closed.compareAndSet(false, true)) {
        try {
          // Clean up phantom reference tracking
          PHANTOM_REFS.remove(phantomRef);
          phantomRef.clear();

          // Perform actual resource cleanup
          doClose();
          if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
            LOGGER.fine(
                String.format(
                    "Closed %s resource with handle: 0x%x", getResourceType(), nativeHandle));
          }
        } catch (final Exception e) {
          LOGGER.warning(
              String.format(
                  "Error closing %s resource with handle: 0x%x: %s",
                  getResourceType(), nativeHandle, e.getMessage()));
          // Don't re-throw exceptions from close() to avoid issues in try-with-resources
        }
      }
    } finally {
      closeLock.writeLock().unlock();
    }
  }

  /**
   * Processes the cleanup queue for phantom references. This method runs in a background daemon
   * thread.
   */
  @SuppressWarnings("InfiniteLoopStatement")
  private static void processCleanupQueue() {
    while (true) {
      try {
        // Block until a phantom reference is available
        @SuppressWarnings("unchecked")
        final PhantomReference<JniResource> ref =
            (PhantomReference<JniResource>) REFERENCE_QUEUE.remove();

        final ResourceCleanup cleanupData = PHANTOM_REFS.remove(ref);
        if (cleanupData != null) {
          try {
            LOGGER.warning(
                String.format(
                    "Cleaning up unclosed %s resource with handle: 0x%x - close() should be called"
                        + " explicitly",
                    cleanupData.resourceType, cleanupData.nativeHandle));
            cleanupData.cleanup.cleanup(cleanupData.nativeHandle);
          } catch (final Exception e) {
            LOGGER.severe(
                String.format(
                    "Error during phantom reference cleanup for %s resource with handle: 0x%x: %s",
                    cleanupData.resourceType, cleanupData.nativeHandle, e.getMessage()));
          }
        }

        // Clear the phantom reference
        ref.clear();

      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
        LOGGER.info("JNI resource cleanup thread interrupted");
        break;
      } catch (final Exception e) {
        LOGGER.severe("Unexpected error in JNI resource cleanup thread: " + e.getMessage());
      }
    }
  }

  /**
   * Safely performs native resource cleanup with additional defensive checks. This method is used
   * by phantom reference cleanup to prevent double-free errors.
   */
  private void doCleanupSafely(final long nativeHandle) throws Exception {
    // Additional defensive check - only cleanup if not already closed
    if (!isClosed()) {
      doClose();
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
