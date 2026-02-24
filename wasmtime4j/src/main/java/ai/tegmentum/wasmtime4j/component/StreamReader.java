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
 * Reads values from a Component Model async stream.
 *
 * <p>Streams are the primary mechanism for transferring sequences of values between components
 * asynchronously in the Component Model async proposal. A StreamReader receives values that were
 * written by a corresponding {@link StreamWriter}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StreamReader reader = ...;
 * while (true) {
 *     StreamResult result = reader.readAsync(10).join();
 *     if (result.isClosed()) break;
 *     for (ComponentVal val : result.getValues()) {
 *         process(val);
 *     }
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public interface StreamReader extends AutoCloseable {

  /**
   * Gets the opaque native handle for this stream reader.
   *
   * @return the native stream handle
   */
  long getHandle();

  /**
   * Asynchronously reads up to {@code maxCount} values from the stream.
   *
   * <p>The returned future completes when at least one value is available or the stream is closed.
   * The result may contain fewer values than requested.
   *
   * @param maxCount the maximum number of values to read
   * @return a future completing with the read result
   * @throws IllegalArgumentException if maxCount is not positive
   * @throws WasmException if the read operation fails
   */
  CompletableFuture<StreamResult> readAsync(int maxCount) throws WasmException;

  /**
   * Cancels a pending read operation.
   *
   * <p>If a read is currently pending, it will be completed with an empty result.
   *
   * @throws WasmException if cancellation fails
   */
  void cancelRead() throws WasmException;

  /**
   * Checks if the stream has been closed by the writer.
   *
   * @return true if no more values will be produced
   */
  boolean isClosed();

  /**
   * Closes this stream reader, releasing associated resources.
   */
  @Override
  void close();
}
