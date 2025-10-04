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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a stream of data flowing through a component pipeline.
 *
 * <p>This interface provides methods for processing data through pipeline stages asynchronously.
 *
 * @param <T> the type of data in the stream
 * @since 1.0.0
 */
public interface ComponentPipelineStream<T> extends AutoCloseable {

  /**
   * Sends data through the pipeline.
   *
   * @param data the data to send
   * @return future that completes when data is processed
   * @throws WasmException if sending fails
   */
  CompletableFuture<Void> send(T data) throws WasmException;

  /**
   * Receives processed data from the pipeline.
   *
   * @return future containing the processed data
   * @throws WasmException if receiving fails
   */
  CompletableFuture<T> receive() throws WasmException;

  /**
   * Checks if the stream is open.
   *
   * @return true if stream is open
   */
  boolean isOpen();

  /**
   * Closes the pipeline stream.
   *
   * @throws WasmException if closing fails
   */
  @Override
  void close() throws WasmException;
}
