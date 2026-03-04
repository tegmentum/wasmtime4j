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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RAII guard for a {@link StreamAny} handle that ensures the underlying stream is closed when this
 * guard is closed.
 *
 * <p>This class implements {@link AutoCloseable} so it can be used with try-with-resources to
 * guarantee that the wrapped stream handle is properly released. The guard owns the stream by
 * default; when the guard is closed, the stream is closed too.
 *
 * <p>Ownership can be transferred out of the guard via {@link #intoStream()}, which prevents the
 * guard from closing the stream. After calling {@code intoStream()}, the guard is considered closed
 * and further access will throw {@link IllegalStateException}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (GuardedStreamReader guard = new GuardedStreamReader(stream)) {
 *     StreamAny s = guard.getStream();
 *     // use s...
 * } // stream is automatically closed here
 * }</pre>
 *
 * <p>Transferring ownership:
 *
 * <pre>{@code
 * StreamAny transferred;
 * try (GuardedStreamReader guard = new GuardedStreamReader(stream)) {
 *     // ... do some validation ...
 *     transferred = guard.intoStream(); // guard no longer owns stream
 * } // guard.close() is a no-op since ownership was transferred
 * // transferred is still valid and must be closed by the caller
 * }</pre>
 *
 * @since 1.1.0
 */
public final class GuardedStreamReader implements AutoCloseable {

  private final StreamAny stream;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new guard wrapping the given stream.
   *
   * @param stream the stream to guard; must not be null
   * @throws IllegalArgumentException if stream is null
   */
  public GuardedStreamReader(final StreamAny stream) {
    if (stream == null) {
      throw new IllegalArgumentException("stream must not be null");
    }
    this.stream = stream;
  }

  /**
   * Returns the underlying stream without transferring ownership.
   *
   * <p>The guard still owns the stream and will close it when the guard is closed.
   *
   * @return the guarded stream
   * @throws IllegalStateException if this guard has been closed or ownership was transferred
   */
  public StreamAny getStream() {
    if (closed.get()) {
      throw new IllegalStateException("GuardedStreamReader has been closed");
    }
    return stream;
  }

  /**
   * Transfers ownership of the underlying stream out of this guard.
   *
   * <p>After calling this method, the guard will not close the stream when {@link #close()} is
   * called. The caller takes responsibility for closing the returned stream.
   *
   * @return the underlying stream
   * @throws IllegalStateException if this guard has been closed or ownership was already
   *     transferred
   */
  public StreamAny intoStream() {
    if (!closed.compareAndSet(false, true)) {
      throw new IllegalStateException("GuardedStreamReader has been closed");
    }
    return stream;
  }

  /**
   * Closes this guard. If ownership has not been transferred via {@link #intoStream()}, the
   * underlying stream is closed.
   *
   * <p>This method is idempotent: calling it multiple times has no additional effect.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      stream.close();
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
