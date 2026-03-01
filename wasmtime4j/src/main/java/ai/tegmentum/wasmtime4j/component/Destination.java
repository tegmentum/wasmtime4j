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
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * An asynchronous destination for Component Model values.
 *
 * <p>A Destination consumes a stream of values written to it asynchronously. This interface models
 * the write side of a Wasmtime async stream, allowing components to produce data at their own pace.
 *
 * @since 1.1.0
 */
public interface Destination extends AutoCloseable {

  /**
   * Asynchronously writes values to this destination.
   *
   * <p>The returned future completes when the values have been accepted by the destination. The
   * destination may buffer values or forward them immediately to a consumer.
   *
   * @param values the values to write
   * @return a future completing when the write is accepted
   * @throws WasmException if the write operation fails
   * @throws IllegalArgumentException if values is null
   */
  CompletableFuture<Void> writeAsync(List<ComponentVal> values) throws WasmException;

  /**
   * Checks if this destination has been closed by the consumer.
   *
   * @return true if no more values will be accepted
   */
  boolean isClosed();

  /** Closes this destination, signaling that no more values will be written. */
  @Override
  void close();
}
