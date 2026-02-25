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

import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Thread-safe resource lifecycle handle using composition.
 *
 * <p>This class encapsulates the closed-flag lifecycle pattern used across all Panama FFI classes.
 * Each Panama class holds a NativeResourceHandle as a field and delegates close/ensureNotClosed to
 * it, replacing the duplicated volatile boolean + ensureNotClosed boilerplate.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Thread-safe close via {@link AtomicBoolean#compareAndSet} (no double-free)
 *   <li>Optional {@link Cleaner} safety net for native resources not explicitly closed
 *   <li>Idempotent {@link #close()} — safe to call multiple times
 * </ul>
 *
 * <p><b>Safety net pattern:</b> When using the Cleaner safety net, the {@code safetyNetAction} must
 * NOT capture a reference to the owning object. Capture only extracted local values (e.g., a
 * MemorySegment field). Otherwise the Cleaner prevents GC of the owning object and the safety net
 * never fires.
 *
 * @since 1.0.0
 */
public final class NativeResourceHandle implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(NativeResourceHandle.class.getName());
  private static final Cleaner CLEANER = Cleaner.create();

  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final String resourceType;
  private final CleanupAction closeAction;
  private final Cleaner.Cleanable cleanable;

  /**
   * Functional interface for resource cleanup operations.
   *
   * <p>Unlike {@link Runnable}, this interface allows checked exceptions to propagate from native
   * cleanup code.
   */
  @FunctionalInterface
  public interface CleanupAction {

    /**
     * Performs resource cleanup.
     *
     * @throws Exception if cleanup fails
     */
    void cleanup() throws Exception;
  }

  /**
   * Creates a resource handle with a Cleaner safety net.
   *
   * <p><b>CRITICAL:</b> The {@code safetyNetAction} Runnable must NOT capture a reference to the
   * owning object. Capture only extracted primitive values or handles. If the Runnable captures
   * {@code this} (the owning object), the Cleaner will prevent the object from being GC'd and the
   * safety net will never fire.
   *
   * @param resourceType human-readable resource type for logging and error messages
   * @param closeAction the cleanup action to run on explicit close
   * @param safetyNetOwner the object whose GC triggers the safety net (typically the owning object)
   * @param safetyNetAction cleanup to run if the owner is GC'd without close (must not capture
   *     owner)
   */
  public NativeResourceHandle(
      final String resourceType,
      final CleanupAction closeAction,
      final Object safetyNetOwner,
      final Runnable safetyNetAction) {
    Validation.requireNonNull(resourceType, "resourceType");
    Validation.requireNonNull(closeAction, "closeAction");
    Validation.requireNonNull(safetyNetOwner, "safetyNetOwner");
    Validation.requireNonNull(safetyNetAction, "safetyNetAction");

    this.resourceType = resourceType;

    // Guard prevents double-free: when close() calls closeAction then cleanable.clean(),
    // the safety net action becomes a no-op since the resource was already destroyed.
    // When GC fires without close(), the safety net still destroys the resource.
    final AtomicBoolean destroyed = new AtomicBoolean(false);
    this.closeAction =
        () -> {
          destroyed.set(true);
          closeAction.cleanup();
        };
    this.cleanable =
        CLEANER.register(
            safetyNetOwner,
            () -> {
              if (!destroyed.get()) {
                safetyNetAction.run();
              }
            });
  }

  /**
   * Creates a resource handle without a Cleaner safety net.
   *
   * <p>Use this constructor for resources that don't hold native pointers directly, or where the
   * Arena already provides cleanup guarantees.
   *
   * @param resourceType human-readable resource type for logging and error messages
   * @param closeAction the cleanup action to run on explicit close
   */
  public NativeResourceHandle(final String resourceType, final CleanupAction closeAction) {
    Validation.requireNonNull(resourceType, "resourceType");
    Validation.requireNonNull(closeAction, "closeAction");

    this.resourceType = resourceType;
    this.closeAction = closeAction;
    this.cleanable = null;
  }

  /**
   * Ensures the resource is not closed.
   *
   * @throws IllegalStateException if the resource has been closed
   */
  public void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException(resourceType + " has been closed");
    }
  }

  /**
   * Checks if this resource has been closed.
   *
   * @return true if the resource is closed, false otherwise
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Closes the resource. Thread-safe and idempotent — only the first call executes the cleanup
   * action. Subsequent calls are no-ops.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        closeAction.cleanup();
        LOGGER.fine("Closed " + resourceType);
      } catch (final Exception e) {
        LOGGER.warning("Error closing " + resourceType + ": " + e.getMessage());
      } finally {
        if (cleanable != null) {
          cleanable.clean();
        }
      }
    }
  }
}
