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
import java.util.concurrent.CompletableFuture;

/**
 * Writes a single value to a Component Model async future.
 *
 * <p>Futures represent a single asynchronous value in the Component Model async proposal. A
 * FutureWriter produces a value that is consumed by a corresponding {@link FutureReader}.
 *
 * @since 1.1.0
 */
public interface FutureWriter extends AutoCloseable {

  /**
   * Gets the opaque native handle for this future writer.
   *
   * @return the native future handle
   */
  long getHandle();

  /**
   * Asynchronously writes the value to this future, resolving the reader.
   *
   * <p>The returned CompletableFuture completes when the value has been accepted. A future can only
   * be written to once — subsequent writes will fail.
   *
   * @param value the value to write
   * @return a future that completes when the value is accepted
   * @throws IllegalArgumentException if value is null
   * @throws WasmException if the write operation fails or the future was already written
   */
  CompletableFuture<Void> writeAsync(ComponentVal value) throws WasmException;

  /**
   * Cancels the pending write.
   *
   * @throws WasmException if cancellation fails
   */
  void cancelWrite() throws WasmException;

  /**
   * Closes this future writer without producing a value.
   *
   * <p>The corresponding reader will receive an empty result.
   */
  @Override
  void close();
}
