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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Reads a single value from a Component Model async future.
 *
 * <p>Futures represent a single asynchronous value in the Component Model async proposal.
 * A FutureReader receives the value when it becomes available from a corresponding
 * {@link FutureWriter}.
 *
 * @since 1.1.0
 */
public interface FutureReader extends AutoCloseable {

  /**
   * Gets the opaque native handle for this future reader.
   *
   * @return the native future handle
   */
  long getHandle();

  /**
   * Asynchronously reads the value from this future.
   *
   * <p>The returned CompletableFuture completes when the value is available or the future is
   * cancelled. Returns empty if the future was cancelled or closed without producing a value.
   *
   * @return a future completing with the value, or empty if cancelled/closed
   * @throws WasmException if the read operation fails
   */
  CompletableFuture<Optional<ComponentVal>> readAsync() throws WasmException;

  /**
   * Cancels the pending read.
   *
   * @throws WasmException if cancellation fails
   */
  void cancelRead() throws WasmException;

  /**
   * Checks if the future has been resolved (value available or closed).
   *
   * @return true if the future has been resolved
   */
  boolean isResolved();

  /**
   * Closes this future reader, releasing associated resources.
   */
  @Override
  void close();
}
