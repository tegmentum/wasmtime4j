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
 * An asynchronous source of Component Model values.
 *
 * <p>A Source produces a stream of values that can be read asynchronously. This interface models
 * the read side of a Wasmtime async stream, allowing components to consume data at their own pace.
 *
 * @since 1.1.0
 */
public interface Source extends AutoCloseable {

  /**
   * Asynchronously reads the next batch of values from this source.
   *
   * <p>The returned future completes when at least one value is available or the source is
   * exhausted. The result may contain fewer values than the buffer capacity.
   *
   * @param buffer the buffer to read values into
   * @return a future completing with the number of values read, or 0 if exhausted
   * @throws WasmException if the read operation fails
   * @throws IllegalArgumentException if buffer is null
   */
  CompletableFuture<Integer> readAsync(VecBuffer buffer) throws WasmException;

  /**
   * Checks if this source has been exhausted (no more values will be produced).
   *
   * @return true if the source is exhausted
   */
  boolean isExhausted();

  /** Closes this source, releasing associated resources. */
  @Override
  void close();
}
