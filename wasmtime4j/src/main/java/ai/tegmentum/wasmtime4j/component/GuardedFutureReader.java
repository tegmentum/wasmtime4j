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
package ai.tegmentum.wasmtime4j.component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RAII guard for a {@link FutureAny} handle that ensures the underlying future is closed when this
 * guard is closed.
 *
 * <p>This class implements {@link AutoCloseable} so it can be used with try-with-resources to
 * guarantee that the wrapped future handle is properly released. The guard owns the future by
 * default; when the guard is closed, the future is closed too.
 *
 * <p>Ownership can be transferred out of the guard via {@link #intoFuture()}, which prevents the
 * guard from closing the future. After calling {@code intoFuture()}, the guard is considered closed
 * and further access will throw {@link IllegalStateException}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (GuardedFutureReader guard = new GuardedFutureReader(future)) {
 *     FutureAny f = guard.getFuture();
 *     // use f...
 * } // future is automatically closed here
 * }</pre>
 *
 * <p>Transferring ownership:
 *
 * <pre>{@code
 * FutureAny transferred;
 * try (GuardedFutureReader guard = new GuardedFutureReader(future)) {
 *     // ... do some validation ...
 *     transferred = guard.intoFuture(); // guard no longer owns future
 * } // guard.close() is a no-op since ownership was transferred
 * // transferred is still valid and must be closed by the caller
 * }</pre>
 *
 * @since 1.1.0
 */
public final class GuardedFutureReader implements AutoCloseable {

  private final FutureAny future;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new guard wrapping the given future.
   *
   * @param future the future to guard; must not be null
   * @throws IllegalArgumentException if future is null
   */
  public GuardedFutureReader(final FutureAny future) {
    if (future == null) {
      throw new IllegalArgumentException("future must not be null");
    }
    this.future = future;
  }

  /**
   * Returns the underlying future without transferring ownership.
   *
   * <p>The guard still owns the future and will close it when the guard is closed.
   *
   * @return the guarded future
   * @throws IllegalStateException if this guard has been closed or ownership was transferred
   */
  public FutureAny getFuture() {
    if (closed.get()) {
      throw new IllegalStateException("GuardedFutureReader has been closed");
    }
    return future;
  }

  /**
   * Gets the payload type of the guarded future, if known.
   *
   * <p>Delegates to {@link FutureAny#getPayloadType()}.
   *
   * @return the payload type descriptor, or empty
   */
  public Optional<ComponentTypeDescriptor> getPayloadType() {
    return future.getPayloadType();
  }

  /**
   * Transfers ownership of the underlying future out of this guard.
   *
   * <p>After calling this method, the guard will not close the future when {@link #close()} is
   * called. The caller takes responsibility for closing the returned future.
   *
   * @return the underlying future
   * @throws IllegalStateException if this guard has been closed or ownership was already
   *     transferred
   */
  public FutureAny intoFuture() {
    if (!closed.compareAndSet(false, true)) {
      throw new IllegalStateException("GuardedFutureReader has been closed");
    }
    return future;
  }

  /**
   * Closes this guard. If ownership has not been transferred via {@link #intoFuture()}, the
   * underlying future is closed.
   *
   * <p>This method is idempotent: calling it multiple times has no additional effect.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      future.close();
    }
  }

  /**
   * Returns whether this guard has been closed or ownership transferred.
   *
   * @return true if this guard is still active
   */
  public boolean isActive() {
    return !closed.get();
  }
}
