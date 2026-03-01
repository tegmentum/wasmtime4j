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
package ai.tegmentum.wasmtime4j.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * WASI Preview 2 pollable resource interface.
 *
 * <p>A pollable represents an event that can be waited on using poll operations. Pollables are used
 * to implement non-blocking I/O by allowing components to wait for multiple events simultaneously.
 *
 * <p>This interface corresponds to the wasi:io/poll.pollable resource from the WASI Preview 2
 * specification.
 *
 * <p>Pollables are typically created from streams, timers, or other I/O resources and represent
 * readiness for operations like reading, writing, or timeout expiration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Single pollable blocking
 * WasiPollable pollable = stream.subscribe();
 * pollable.block();
 * // Stream is now ready for I/O
 *
 * // Multiple pollables
 * List<WasiPollable> pollables = Arrays.asList(
 *   inputStream.subscribe(),
 *   outputStream.subscribe(),
 *   timer.subscribe()
 * );
 * List<Integer> ready = WasiPollable.poll(pollables);
 * for (int index : ready) {
 *   // Handle ready pollable at index
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiPollable extends AutoCloseable {

  /**
   * Blocks until this pollable is ready.
   *
   * <p>This operation suspends execution until the event represented by this pollable occurs. For
   * streams, this means data is available or the stream is closed. For timers, this means the
   * timeout has expired.
   *
   * <p>If the pollable is already ready, this returns immediately.
   *
   * @throws WasmException if the wait operation fails
   * @throws IllegalStateException if the pollable is not valid
   */
  void block() throws WasmException;

  /**
   * Checks if this pollable is currently ready without blocking.
   *
   * <p>This is a non-blocking check that returns immediately. Use this to check readiness before
   * performing I/O operations to avoid blocking.
   *
   * @return true if the pollable is ready, false otherwise
   * @throws WasmException if the ready check fails
   * @throws IllegalStateException if the pollable is not valid
   */
  boolean ready() throws WasmException;
}
