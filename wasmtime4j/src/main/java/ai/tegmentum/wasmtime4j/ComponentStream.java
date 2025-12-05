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
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Flow;

/**
 * Represents a Component Model stream.
 *
 * <p>A ComponentStream represents an asynchronous sequence of values that can be read or written
 * incrementally. This corresponds to the {@code stream<T>} type in the WebAssembly Component Model.
 *
 * <p>Component Model streams are different from Java streams in several ways:
 *
 * <ul>
 *   <li>They are asynchronous and support polling
 *   <li>They can be either readable (input) or writable (output)
 *   <li>They integrate with the WASI polling model
 *   <li>They support backpressure through the read/write API
 * </ul>
 *
 * <p>Example usage for reading:
 *
 * <pre>{@code
 * // Read stream
 * ComponentStream<String> stream = getInputStream();
 *
 * // Poll for data
 * while (!stream.isEnded()) {
 *     WasiPollable pollable = stream.subscribe();
 *     pollable.block();
 *
 *     List<String> items = stream.read(10);
 *     for (String item : items) {
 *         process(item);
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage for writing:
 *
 * <pre>{@code
 * // Write stream
 * ComponentStream<String> stream = getOutputStream();
 *
 * List<String> data = getData();
 * for (String item : data) {
 *     while (!stream.write(item)) {
 *         // Backpressure - wait for space
 *         stream.subscribe().block();
 *     }
 * }
 * stream.end();
 * }</pre>
 *
 * @param <T> the type of elements in the stream
 * @since 1.0.0
 */
public interface ComponentStream<T> extends AutoCloseable {

  /**
   * Gets the WIT type of elements in this stream.
   *
   * @return the element type
   */
  WitType getElementType();

  /**
   * Gets the direction of this stream.
   *
   * @return the stream direction
   */
  Direction getDirection();

  /**
   * Checks if the stream is readable (can be read from).
   *
   * @return true if readable
   */
  default boolean isReadable() {
    return getDirection() == Direction.READ || getDirection() == Direction.BIDIRECTIONAL;
  }

  /**
   * Checks if the stream is writable (can be written to).
   *
   * @return true if writable
   */
  default boolean isWritable() {
    return getDirection() == Direction.WRITE || getDirection() == Direction.BIDIRECTIONAL;
  }

  /**
   * Reads up to the specified number of elements from the stream.
   *
   * <p>This method returns immediately with whatever elements are currently available, up to the
   * requested count. If no elements are available, returns an empty list.
   *
   * <p>To wait for elements to become available, use {@link #subscribe()} to get a pollable and
   * block on it before calling read.
   *
   * @param maxCount the maximum number of elements to read
   * @return a list of elements (may be empty if none available)
   * @throws WasmException if reading fails
   * @throws UnsupportedOperationException if the stream is not readable
   * @throws IllegalStateException if the stream is closed
   */
  List<T> read(int maxCount) throws WasmException;

  /**
   * Reads a single element from the stream.
   *
   * <p>This method returns immediately. If no element is available, returns empty.
   *
   * @return the element if available, or empty if none ready
   * @throws WasmException if reading fails
   * @throws UnsupportedOperationException if the stream is not readable
   * @throws IllegalStateException if the stream is closed
   */
  default Optional<T> readOne() throws WasmException {
    List<T> result = read(1);
    return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
  }

  /**
   * Reads a single element from the stream, blocking until available.
   *
   * @return the element, or empty if stream ended
   * @throws WasmException if reading fails
   * @throws UnsupportedOperationException if the stream is not readable
   * @throws IllegalStateException if the stream is closed
   */
  Optional<T> readBlocking() throws WasmException;

  /**
   * Writes elements to the stream.
   *
   * <p>This method attempts to write as many elements as possible without blocking. It returns the
   * number of elements actually written, which may be less than requested due to backpressure.
   *
   * <p>To handle backpressure, check the return value and use {@link #subscribe()} to wait for more
   * capacity.
   *
   * @param elements the elements to write
   * @return the number of elements actually written
   * @throws WasmException if writing fails
   * @throws UnsupportedOperationException if the stream is not writable
   * @throws IllegalStateException if the stream is closed or ended
   */
  int write(List<T> elements) throws WasmException;

  /**
   * Writes a single element to the stream.
   *
   * @param element the element to write
   * @return true if the element was written, false if backpressure prevents writing
   * @throws WasmException if writing fails
   * @throws UnsupportedOperationException if the stream is not writable
   * @throws IllegalStateException if the stream is closed or ended
   */
  default boolean write(final T element) throws WasmException {
    return write(List.of(element)) > 0;
  }

  /**
   * Writes a single element to the stream, blocking until space is available.
   *
   * @param element the element to write
   * @throws WasmException if writing fails
   * @throws UnsupportedOperationException if the stream is not writable
   * @throws IllegalStateException if the stream is closed or ended
   */
  void writeBlocking(T element) throws WasmException;

  /**
   * Signals the end of writing to this stream.
   *
   * <p>After calling end(), no more elements can be written. Readers will receive the end-of-stream
   * signal after reading all buffered elements.
   *
   * @throws WasmException if ending fails
   * @throws UnsupportedOperationException if the stream is not writable
   * @throws IllegalStateException if the stream is already closed or ended
   */
  void end() throws WasmException;

  /**
   * Checks if the stream has ended (no more elements will be available).
   *
   * <p>For readable streams, this indicates that all elements have been read and the writer has
   * signaled end-of-stream. For writable streams, this indicates that {@link #end()} has been
   * called.
   *
   * @return true if the stream has ended
   */
  boolean isEnded();

  /**
   * Gets the number of elements currently available for reading without blocking.
   *
   * @return the number of available elements, or 0 if none ready
   * @throws UnsupportedOperationException if the stream is not readable
   */
  int available();

  /**
   * Gets the remaining write capacity before backpressure.
   *
   * @return the remaining capacity, or 0 if backpressure is in effect
   * @throws UnsupportedOperationException if the stream is not writable
   */
  int writeCapacity();

  /**
   * Creates a pollable handle for this stream.
   *
   * <p>For readable streams, the pollable becomes ready when data is available to read or when the
   * stream ends. For writable streams, the pollable becomes ready when write capacity becomes
   * available.
   *
   * @return a pollable for this stream
   * @throws WasmException if subscription fails
   * @throws IllegalStateException if the stream is closed
   */
  WasiPollable subscribe() throws WasmException;

  /**
   * Checks if the stream is open for operations.
   *
   * @return true if the stream is open
   */
  boolean isOpen();

  /**
   * Gets the current state of the stream.
   *
   * @return the stream state
   */
  State getState();

  /**
   * Returns an iterator over the elements in a readable stream.
   *
   * <p>The iterator blocks when waiting for new elements and terminates when the stream ends.
   *
   * @return an iterator over stream elements
   * @throws UnsupportedOperationException if the stream is not readable
   */
  Iterator<T> iterator();

  /**
   * Converts this readable stream to a Java Flow.Publisher.
   *
   * <p>This allows integration with Java's reactive streams API.
   *
   * @return a Publisher that publishes elements from this stream
   * @throws UnsupportedOperationException if the stream is not readable
   */
  Flow.Publisher<T> toPublisher();

  /**
   * Closes the stream and releases associated resources.
   *
   * <p>After closing, no further operations can be performed on this stream.
   */
  @Override
  void close();

  /** The direction of a component stream. */
  enum Direction {
    /** Stream can only be read from. */
    READ,
    /** Stream can only be written to. */
    WRITE,
    /** Stream can be both read from and written to. */
    BIDIRECTIONAL
  }

  /** The state of a component stream. */
  enum State {
    /** Stream is open and ready for operations. */
    OPEN,
    /** Stream has reached end-of-stream (readable) or has been ended (writable). */
    ENDED,
    /** Stream has encountered an error. */
    ERROR,
    /** Stream has been closed. */
    CLOSED
  }
}
