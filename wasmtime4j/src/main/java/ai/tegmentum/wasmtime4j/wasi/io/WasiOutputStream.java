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
 * WASI Preview 2 output stream interface.
 *
 * <p>An output-stream provides non-blocking write operations on a stream of bytes. Writers must
 * check write capacity before writing to avoid blocking or errors.
 *
 * <p>This interface corresponds to the wasi:io/streams.output-stream resource from the WASI Preview
 * 2 specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiOutputStream stream = openOutputStream()) {
 *   // Check write capacity
 *   long capacity = stream.checkWrite();
 *
 *   // Write data (non-blocking)
 *   byte[] data = "Hello, WASI!".getBytes();
 *   stream.write(data);
 *
 *   // Flush to ensure data is written
 *   stream.flush();
 *
 *   // Or use blocking write-and-flush
 *   stream.blockingWriteAndFlush(moreData);
 *
 *   // Splice from input stream
 *   long transferred = stream.splice(inputStream, 1024);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiOutputStream extends AutoCloseable {

  /**
   * Checks the number of bytes that can be written without blocking.
   *
   * <p>Returns the number of bytes that can be written to the stream without blocking. This should
   * be called before {@link #write} to ensure the write won't block or fail due to insufficient
   * capacity.
   *
   * @return number of bytes that can be written immediately
   * @throws WasmException if the check fails
   * @throws IllegalStateException if the stream is not valid
   */
  long checkWrite() throws WasmException;

  /**
   * Writes bytes to the stream without blocking.
   *
   * <p>Writes the provided bytes to the stream. Call {@link #checkWrite} first to ensure sufficient
   * capacity. This operation does not automatically flush - call {@link #flush} to ensure data is
   * transmitted.
   *
   * @param contents bytes to write
   * @throws WasmException if the write operation fails
   * @throws IllegalArgumentException if contents is null
   * @throws IllegalStateException if the stream is not valid or has insufficient capacity
   */
  void write(final byte[] contents) throws WasmException;

  /**
   * Writes bytes to the stream and flushes, blocking until complete.
   *
   * <p>This operation blocks until all bytes are written and flushed to the underlying destination.
   * It combines write and flush operations for convenience.
   *
   * @param contents bytes to write
   * @throws WasmException if the write or flush operation fails
   * @throws IllegalArgumentException if contents is null
   * @throws IllegalStateException if the stream is not valid
   */
  void blockingWriteAndFlush(final byte[] contents) throws WasmException;

  /**
   * Flushes buffered data to the underlying destination.
   *
   * <p>This is a non-blocking operation that initiates a flush. It may return before the flush
   * completes. Use {@link #blockingFlush} to wait for completion.
   *
   * @throws WasmException if the flush operation fails
   * @throws IllegalStateException if the stream is not valid
   */
  void flush() throws WasmException;

  /**
   * Flushes buffered data, blocking until complete.
   *
   * <p>This operation blocks until all buffered data has been written to the underlying
   * destination.
   *
   * @throws WasmException if the flush operation fails
   * @throws IllegalStateException if the stream is not valid
   */
  void blockingFlush() throws WasmException;

  /**
   * Writes the specified number of zero bytes to the stream.
   *
   * <p>This is more efficient than writing an array of zeros. Call {@link #checkWrite} first to
   * ensure sufficient capacity.
   *
   * @param length number of zero bytes to write
   * @throws WasmException if the write operation fails
   * @throws IllegalArgumentException if length is negative
   * @throws IllegalStateException if the stream is not valid or has insufficient capacity
   */
  void writeZeroes(final long length) throws WasmException;

  /**
   * Writes zero bytes and flushes, blocking until complete.
   *
   * <p>Combines {@link #writeZeroes} and {@link #flush} in a blocking operation.
   *
   * @param length number of zero bytes to write
   * @throws WasmException if the write or flush operation fails
   * @throws IllegalArgumentException if length is negative
   * @throws IllegalStateException if the stream is not valid
   */
  void blockingWriteZeroesAndFlush(final long length) throws WasmException;

  /**
   * Transfers bytes from an input stream to this output stream.
   *
   * <p>This is a non-blocking operation that transfers up to the specified number of bytes from the
   * source stream to this stream. It may transfer fewer bytes if insufficient capacity or source
   * data is available.
   *
   * @param source input stream to read from
   * @param length maximum number of bytes to transfer
   * @return actual number of bytes transferred
   * @throws WasmException if the splice operation fails
   * @throws IllegalArgumentException if source is null or length is negative
   * @throws IllegalStateException if either stream is not valid
   */
  long splice(final WasiInputStream source, final long length) throws WasmException;

  /**
   * Transfers bytes from an input stream, blocking until complete.
   *
   * <p>This operation blocks until the specified number of bytes are transferred or the source
   * stream ends. It may transfer fewer bytes if the source stream has insufficient data.
   *
   * @param source input stream to read from
   * @param length maximum number of bytes to transfer
   * @return actual number of bytes transferred
   * @throws WasmException if the splice operation fails
   * @throws IllegalArgumentException if source is null or length is negative
   * @throws IllegalStateException if either stream is not valid
   */
  long blockingSplice(final WasiInputStream source, final long length) throws WasmException;

  /**
   * Creates a pollable that resolves when the stream is ready for writing.
   *
   * <p>The returned pollable can be used with poll operations to wait for write readiness without
   * blocking the entire component. The pollable is a child resource of this stream and becomes
   * invalid when the stream is closed.
   *
   * @return a pollable for waiting on stream writability
   * @throws WasmException if the pollable cannot be created
   * @throws IllegalStateException if the stream is not valid
   */
  WasiPollable subscribe() throws WasmException;
}
