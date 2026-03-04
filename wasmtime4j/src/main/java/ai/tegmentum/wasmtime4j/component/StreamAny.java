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
 * Represents a type-erased stream handle in the WebAssembly Component Model.
 *
 * <p>This interface corresponds to Wasmtime's type-erased stream representation, wrapping a {@code
 * Val::Stream} value via the {@code AsyncValRegistry}. Streams are used in the async component
 * model for passing sequences of values between components.
 *
 * <p>A {@code StreamAny} holds an opaque handle that can be converted to a {@link ComponentVal} via
 * {@link ComponentVal#stream(long)} for passing to component function calls.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StreamAny stream = StreamAny.create(handle);
 * ComponentVal val = ComponentVal.stream(stream.getHandle());
 * // Pass val to a component function...
 * stream.close();
 * }</pre>
 *
 * @since 1.1.0
 */
public interface StreamAny extends AutoCloseable {

  /**
   * Gets the opaque handle ID for this stream.
   *
   * <p>The handle ID corresponds to an entry in the native {@code AsyncValRegistry}. It can be used
   * with {@link ComponentVal#stream(long)} to create a component value for function calls.
   *
   * @return the opaque handle ID
   */
  long getHandle();

  /**
   * Checks whether this stream handle is still valid.
   *
   * <p>A handle becomes invalid after {@link #close()} is called or after the underlying value has
   * been consumed by a component function call.
   *
   * @return true if the handle is still valid
   */
  boolean isValid();

  /**
   * Closes this stream, releasing the underlying native resource.
   *
   * <p>After calling this method, {@link #isValid()} will return false and the handle should not be
   * used for further operations. If a native close action was provided at creation time, it will be
   * invoked to remove the handle from the {@code AsyncValRegistry}.
   *
   * <p>This method is idempotent: calling it multiple times has no additional effect.
   */
  @Override
  void close();

  /**
   * Creates a new stream handle wrapping the given handle ID.
   *
   * <p>The returned handle has no native close action; calling {@link #close()} only invalidates
   * the Java-side wrapper.
   *
   * @param handle the opaque handle ID from the native {@code AsyncValRegistry}
   * @return a new StreamAny
   * @throws IllegalArgumentException if handle is not positive
   */
  static StreamAny create(final long handle) {
    if (handle <= 0) {
      throw new IllegalArgumentException("handle must be positive, got: " + handle);
    }
    return new DefaultStreamAny(handle, null);
  }

  /**
   * Creates a new stream handle wrapping the given handle ID with a native close action.
   *
   * <p>When {@link #close()} is called, the provided {@code closeAction} will be invoked exactly
   * once to release the native resource from the {@code AsyncValRegistry}.
   *
   * @param handle the opaque handle ID from the native {@code AsyncValRegistry}
   * @param closeAction action to invoke on close to release native resources, or null for no-op
   * @return a new StreamAny
   * @throws IllegalArgumentException if handle is not positive
   */
  static StreamAny create(final long handle, final Runnable closeAction) {
    if (handle <= 0) {
      throw new IllegalArgumentException("handle must be positive, got: " + handle);
    }
    return new DefaultStreamAny(handle, closeAction);
  }

  /** Default implementation of StreamAny. */
  final class DefaultStreamAny implements StreamAny {

    private final long handle;
    private final Runnable closeAction;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    DefaultStreamAny(final long handle, final Runnable closeAction) {
      this.handle = handle;
      this.closeAction = closeAction;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public boolean isValid() {
      return !closed.get();
    }

    @Override
    public void close() {
      if (closed.compareAndSet(false, true)) {
        if (closeAction != null) {
          closeAction.run();
        }
      }
    }

    @Override
    public String toString() {
      return "StreamAny{handle=" + handle + ", valid=" + !closed.get() + "}";
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof DefaultStreamAny)) {
        return false;
      }
      final DefaultStreamAny other = (DefaultStreamAny) obj;
      return handle == other.handle;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(handle);
    }
  }
}
