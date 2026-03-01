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

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * An opaque handle representing a Component Model async future.
 *
 * <p>FutureAny is the type-erased counterpart to typed {@link FutureReader}/{@link FutureWriter}
 * pairs. It allows futures to be passed through component boundaries without static type
 * information, similar to how {@link ResourceAny} works for resources.
 *
 * @since 1.1.0
 */
public interface FutureAny extends AutoCloseable {

  /**
   * Gets the opaque native handle for this future.
   *
   * @return the native handle
   */
  long getHandle();

  /**
   * Attempts to convert this FutureAny into a typed FutureReader.
   *
   * @return a typed FutureReader view of this future
   * @throws WasmException if the conversion fails
   */
  FutureReader tryIntoFutureReader() throws WasmException;

  /** Closes this future, releasing associated native resources. */
  @Override
  void close();

  /**
   * Creates a FutureAny from a typed FutureReader.
   *
   * @param reader the typed future reader
   * @return an opaque FutureAny wrapping the reader
   * @throws IllegalArgumentException if reader is null
   */
  static FutureAny tryFromFutureReader(final FutureReader reader) {
    if (reader == null) {
      throw new IllegalArgumentException("reader cannot be null");
    }
    return new DefaultFutureAny(reader.getHandle(), reader);
  }

  /** Default implementation wrapping an opaque handle. */
  final class DefaultFutureAny implements FutureAny {

    private final long handle;
    private final FutureReader reader;
    private volatile boolean closed;

    DefaultFutureAny(final long handle, final FutureReader reader) {
      this.handle = handle;
      this.reader = reader;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public FutureReader tryIntoFutureReader() throws WasmException {
      if (closed) {
        throw new WasmException("FutureAny is already closed");
      }
      if (reader == null) {
        throw new WasmException("FutureAny does not wrap a typed reader");
      }
      return reader;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        if (reader != null) {
          reader.close();
        }
      }
    }
  }
}
