package ai.tegmentum.wasmtime4j.wasi;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for WASI Preview 2 stream operations.
 *
 * <p>WASI Preview 2 introduces async stream-based I/O operations that allow for non-blocking
 * reading and writing of data. This interface provides access to both input and output streams with
 * proper async semantics.
 *
 * <p>Streams in WASI Preview 2:
 *
 * <ul>
 *   <li>Support async operations with CompletableFuture
 *   <li>Provide flow control and backpressure handling
 *   <li>Allow for efficient bulk data transfers
 *   <li>Support cancellation and timeout operations
 * </ul>
 *
 * @since 1.0.0
 */
public interface WasiPreview2Stream extends AutoCloseable {

  /**
   * Gets the unique stream identifier.
   *
   * @return the stream ID
   */
  long getStreamId();

  /**
   * Gets the stream type.
   *
   * @return the stream type
   */
  WasiStreamType getStreamType();

  /**
   * Checks if the stream is ready for operations.
   *
   * @return true if the stream is ready, false otherwise
   */
  boolean isReady();

  /**
   * Checks if the stream is closed.
   *
   * @return true if the stream is closed, false otherwise
   */
  boolean isClosed();

  /**
   * Gets the current stream status.
   *
   * @return the stream status
   */
  WasiStreamStatus getStatus();

  /**
   * Reads data asynchronously from the stream.
   *
   * <p>This method is only available for input streams and bidirectional streams.
   *
   * @param buffer the buffer to read data into
   * @return CompletableFuture that resolves to the number of bytes read
   * @throws UnsupportedOperationException if called on an output-only stream
   */
  CompletableFuture<Integer> readAsync(ByteBuffer buffer);

  /**
   * Writes data asynchronously to the stream.
   *
   * <p>This method is only available for output streams and bidirectional streams.
   *
   * @param buffer the buffer containing data to write
   * @return CompletableFuture that resolves to the number of bytes written
   * @throws UnsupportedOperationException if called on an input-only stream
   */
  CompletableFuture<Integer> writeAsync(ByteBuffer buffer);

  /**
   * Flushes any buffered data to the underlying resource.
   *
   * <p>This method is only available for output streams and bidirectional streams.
   *
   * @return CompletableFuture that completes when the flush operation is done
   * @throws UnsupportedOperationException if called on an input-only stream
   */
  CompletableFuture<Void> flushAsync();

  /**
   * Creates a pollable handle for this stream.
   *
   * <p>The pollable can be used with {@link WasiPreview2Context#poll} to wait for the stream to
   * become ready for operations.
   *
   * @return the pollable handle
   */
  long createPollable();

  /**
   * Closes the stream and releases associated resources.
   *
   * <p>After closing, no further operations can be performed on this stream.
   */
  @Override
  void close();

  /** Stream types supported by WASI Preview 2. */
  enum WasiStreamType {
    /** Input stream for reading data. */
    INPUT,
    /** Output stream for writing data. */
    OUTPUT,
    /** Bidirectional stream for reading and writing data. */
    BIDIRECTIONAL
  }

  /** Stream status indicators. */
  enum WasiStreamStatus {
    /** Stream is ready for operations. */
    READY,
    /** Stream is temporarily blocked. */
    BLOCKED,
    /** Stream has reached end-of-file. */
    EOF,
    /** Stream is closed. */
    CLOSED,
    /** Stream has encountered an error. */
    ERROR
  }
}
