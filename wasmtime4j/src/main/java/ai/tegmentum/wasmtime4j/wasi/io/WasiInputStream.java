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

package ai.tegmentum.wasmtime4j.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * WASI Preview 2 input stream interface.
 *
 * <p>An input-stream provides non-blocking read operations on a stream of bytes. Streams are
 * non-blocking to the extent practical on underlying platforms - if fewer bytes are available than
 * requested, the operation returns promptly with available bytes (which could be zero).
 *
 * <p>This interface corresponds to the wasi:io/streams.input-stream resource from the WASI Preview
 * 2 specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiInputStream stream = openInputStream()) {
 *   // Non-blocking read
 *   byte[] data = stream.read(1024);
 *
 *   // Blocking read (waits for data)
 *   byte[] allData = stream.blockingRead(8192);
 *
 *   // Skip bytes
 *   long skipped = stream.skip(512);
 *
 *   // Wait for readability
 *   WasiPollable pollable = stream.subscribe();
 *   pollable.block();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiInputStream extends AutoCloseable {

  /**
   * Reads up to the specified number of bytes from the stream.
   *
   * <p>This is a non-blocking operation that returns immediately with whatever bytes are available,
   * which could be fewer than requested or even zero. If the stream is closed, returns an empty
   * array.
   *
   * @param length maximum number of bytes to read
   * @return bytes read from the stream (may be empty or shorter than requested)
   * @throws WasmException if the read operation fails
   * @throws IllegalArgumentException if length is negative
   * @throws IllegalStateException if the stream is not valid
   */
  byte[] read(final long length) throws WasmException;

  /**
   * Reads bytes from the stream, blocking until at least some data is available.
   *
   * <p>This operation blocks until data becomes available or the stream is closed. It may return
   * fewer bytes than requested if the stream ends before the requested amount is available.
   *
   * @param length maximum number of bytes to read
   * @return bytes read from the stream (guaranteed non-empty unless stream is closed)
   * @throws WasmException if the read operation fails
   * @throws IllegalArgumentException if length is negative
   * @throws IllegalStateException if the stream is not valid
   */
  byte[] blockingRead(final long length) throws WasmException;

  /**
   * Skips up to the specified number of bytes in the stream.
   *
   * <p>This is a non-blocking operation that skips as many bytes as are immediately available,
   * which could be fewer than requested or even zero.
   *
   * @param length maximum number of bytes to skip
   * @return actual number of bytes skipped
   * @throws WasmException if the skip operation fails
   * @throws IllegalArgumentException if length is negative
   * @throws IllegalStateException if the stream is not valid
   */
  long skip(final long length) throws WasmException;

  /**
   * Skips bytes in the stream, blocking until bytes are available to skip.
   *
   * <p>This operation blocks until it can skip at least some bytes or the stream is closed. It may
   * skip fewer bytes than requested if the stream ends.
   *
   * @param length maximum number of bytes to skip
   * @return actual number of bytes skipped (guaranteed non-zero unless stream is closed)
   * @throws WasmException if the skip operation fails
   * @throws IllegalArgumentException if length is negative
   * @throws IllegalStateException if the stream is not valid
   */
  long blockingSkip(final long length) throws WasmException;

  /**
   * Creates a pollable that resolves when the stream has bytes available or is closed.
   *
   * <p>The returned pollable can be used with poll operations to wait for stream readability
   * without blocking the entire component. The pollable is a child resource of this stream and
   * becomes invalid when the stream is closed.
   *
   * @return a pollable for waiting on stream readability
   * @throws WasmException if the pollable cannot be created
   * @throws IllegalStateException if the stream is not valid
   */
  WasiPollable subscribe() throws WasmException;
}
