/*
 * Copyright 2024 Tegmentum AI
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Writes values to a Component Model async stream.
 *
 * <p>Streams are the primary mechanism for transferring sequences of values between components
 * asynchronously in the Component Model async proposal. A StreamWriter produces values that are
 * consumed by a corresponding {@link StreamReader}.
 *
 * @since 1.1.0
 */
public interface StreamWriter extends AutoCloseable {

  /**
   * Gets the opaque native handle for this stream writer.
   *
   * @return the native stream handle
   */
  long getHandle();

  /**
   * Asynchronously writes values to the stream.
   *
   * <p>The returned future completes when the values have been accepted by the stream buffer.
   * Back-pressure is applied if the reader is not consuming values fast enough.
   *
   * @param values the values to write
   * @return a future that completes when the values are accepted
   * @throws IllegalArgumentException if values is null or empty
   * @throws WasmException if the write operation fails
   */
  CompletableFuture<Void> writeAsync(List<ComponentVal> values) throws WasmException;

  /**
   * Cancels a pending write operation.
   *
   * @throws WasmException if cancellation fails
   */
  void cancelWrite() throws WasmException;

  /** Closes the stream, signaling to the reader that no more values will be produced. */
  @Override
  void close();
}
