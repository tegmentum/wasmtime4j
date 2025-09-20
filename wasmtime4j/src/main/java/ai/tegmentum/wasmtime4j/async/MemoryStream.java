package ai.tegmentum.wasmtime4j.async;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced streaming interface for WebAssembly memory operations.
 *
 * <p>A MemoryStream provides sophisticated streaming capabilities for reading from and writing to
 * WebAssembly memory with asynchronous operations, progress tracking, cancellation support, and
 * efficient buffer management.
 *
 * <p>This interface is designed for high-performance scenarios where large amounts of data need to
 * be transferred to/from WebAssembly memory without blocking threads or consuming excessive memory.
 *
 * @since 1.0.0
 */
public interface MemoryStream extends Closeable {

  /**
   * Asynchronously reads data from the memory stream into the provided buffer.
   *
   * <p>This method attempts to read up to buffer.remaining() bytes from the stream into the
   * buffer. The buffer's position will be advanced by the number of bytes read.
   *
   * @param buffer the ByteBuffer to read data into
   * @return a CompletableFuture that completes with the number of bytes read, or -1 if end of stream
   * @throws IllegalArgumentException if buffer is null
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<Integer> readAsync(final ByteBuffer buffer);

  /**
   * Asynchronously reads data with a timeout.
   *
   * @param buffer the ByteBuffer to read data into
   * @param timeout the maximum time to wait for data
   * @return a CompletableFuture that completes with the number of bytes read
   * @throws IllegalArgumentException if buffer is null or timeout is negative
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<Integer> readAsync(final ByteBuffer buffer, final Duration timeout);

  /**
   * Asynchronously reads a specific number of bytes from the stream.
   *
   * <p>This method guarantees to read exactly the specified number of bytes, unless end of stream
   * is reached. It may require multiple internal read operations to fulfill the request.
   *
   * @param numBytes the exact number of bytes to read
   * @return a CompletableFuture that completes with a ByteBuffer containing the read data
   * @throws IllegalArgumentException if numBytes is negative
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<ByteBuffer> readExactly(final int numBytes);

  /**
   * Asynchronously writes data from the buffer to the memory stream.
   *
   * <p>This method writes all remaining bytes from the buffer to the stream. The buffer's position
   * will be advanced by the number of bytes written.
   *
   * @param buffer the ByteBuffer containing data to write
   * @return a CompletableFuture that completes with the number of bytes written
   * @throws IllegalArgumentException if buffer is null
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<Integer> writeAsync(final ByteBuffer buffer);

  /**
   * Asynchronously writes data with a timeout.
   *
   * @param buffer the ByteBuffer containing data to write
   * @param timeout the maximum time to wait for the write to complete
   * @return a CompletableFuture that completes with the number of bytes written
   * @throws IllegalArgumentException if buffer is null or timeout is negative
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<Integer> writeAsync(final ByteBuffer buffer, final Duration timeout);

  /**
   * Asynchronously writes all data from the buffer.
   *
   * <p>This method guarantees to write all remaining bytes from the buffer, potentially requiring
   * multiple internal write operations.
   *
   * @param buffer the ByteBuffer containing data to write
   * @return a CompletableFuture that completes when all data has been written
   * @throws IllegalArgumentException if buffer is null
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<Void> writeAll(final ByteBuffer buffer);

  /**
   * Asynchronously flushes any buffered data to the underlying memory.
   *
   * <p>This method ensures that all pending write operations are committed to the WebAssembly
   * memory, making the data immediately available to WASM code.
   *
   * @return a CompletableFuture that completes when the flush operation finishes
   * @throws IllegalStateException if the stream is closed
   */
  CompletableFuture<Void> flush();

  /**
   * Gets the current position in the memory stream.
   *
   * <p>The position represents the offset from the start of the stream where the next read or
   * write operation will occur.
   *
   * @return the current stream position
   * @throws IllegalStateException if the stream is closed
   */
  long getPosition();

  /**
   * Sets the current position in the memory stream.
   *
   * <p>This method allows seeking to a specific position in the stream for random access
   * operations.
   *
   * @param position the new stream position
   * @return a CompletableFuture that completes when the seek operation finishes
   * @throws IllegalArgumentException if position is negative
   * @throws IllegalStateException if the stream is closed or doesn't support seeking
   */
  CompletableFuture<Void> seek(final long position);

  /**
   * Gets the total size of the memory stream.
   *
   * @return the stream size in bytes, or -1 if unknown
   * @throws IllegalStateException if the stream is closed
   */
  long getSize();

  /**
   * Checks if the stream has reached the end.
   *
   * @return true if at end of stream
   * @throws IllegalStateException if the stream is closed
   */
  boolean isAtEnd();

  /**
   * Checks if the stream supports seeking operations.
   *
   * @return true if seeking is supported
   */
  boolean isSeekable();

  /**
   * Checks if the stream supports reading operations.
   *
   * @return true if reading is supported
   */
  boolean isReadable();

  /**
   * Checks if the stream supports writing operations.
   *
   * @return true if writing is supported
   */
  boolean isWritable();

  /**
   * Checks if the stream is currently open.
   *
   * @return true if the stream is open
   */
  boolean isOpen();

  /**
   * Gets progress information for the current stream operations.
   *
   * <p>This method returns progress information if progress tracking is enabled for the stream,
   * otherwise returns null.
   *
   * @return progress information, or null if not available
   */
  StreamProgress getProgress();

  /**
   * Cancels any pending operations on this stream.
   *
   * <p>This method attempts to cancel all pending read and write operations. Cancelled operations
   * will complete with a CancellationException.
   *
   * @return a CompletableFuture that completes when cancellation is finished
   */
  CompletableFuture<Void> cancel();

  /**
   * Gets statistics about operations performed on this stream.
   *
   * @return stream operation statistics
   */
  MemoryStreamStatistics getStatistics();

  /**
   * Closes the memory stream and releases associated resources.
   *
   * <p>After closing, all pending operations will be cancelled and no new operations can be
   * started on this stream.
   */
  @Override
  void close();

  /** Progress information for memory stream operations. */
  interface StreamProgress {
    /**
     * Gets the total number of bytes processed.
     *
     * @return bytes processed
     */
    long getBytesProcessed();

    /**
     * Gets the total number of bytes to process.
     *
     * @return total bytes, or -1 if unknown
     */
    long getTotalBytes();

    /**
     * Gets the current progress as a percentage.
     *
     * @return progress percentage (0.0 to 100.0)
     */
    double getProgressPercentage();

    /**
     * Gets the current operation type.
     *
     * @return the current operation
     */
    StreamOperation getCurrentOperation();

    /**
     * Gets the elapsed time for the current operation.
     *
     * @return elapsed duration
     */
    Duration getElapsedTime();

    /**
     * Gets an estimate of the remaining time.
     *
     * @return estimated remaining duration, or null if unknown
     */
    Duration getEstimatedTimeRemaining();

    /**
     * Gets the current transfer rate in bytes per second.
     *
     * @return transfer rate, or -1 if unknown
     */
    double getTransferRateBytesPerSecond();
  }

  /** Types of stream operations. */
  enum StreamOperation {
    /** Stream is idle with no active operations. */
    IDLE("Idle"),

    /** Reading data from memory. */
    READING("Reading"),

    /** Writing data to memory. */
    WRITING("Writing"),

    /** Flushing buffered data. */
    FLUSHING("Flushing"),

    /** Seeking to a new position. */
    SEEKING("Seeking"),

    /** Closing the stream. */
    CLOSING("Closing");

    private final String displayName;

    StreamOperation(final String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this operation.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }
  }

  /** Statistics for memory stream operations. */
  interface MemoryStreamStatistics {
    /**
     * Gets the total number of read operations performed.
     *
     * @return number of read operations
     */
    long getReadOperations();

    /**
     * Gets the total number of write operations performed.
     *
     * @return number of write operations
     */
    long getWriteOperations();

    /**
     * Gets the total number of flush operations performed.
     *
     * @return number of flush operations
     */
    long getFlushOperations();

    /**
     * Gets the total number of seek operations performed.
     *
     * @return number of seek operations
     */
    long getSeekOperations();

    /**
     * Gets the total number of bytes read from the stream.
     *
     * @return total bytes read
     */
    long getTotalBytesRead();

    /**
     * Gets the total number of bytes written to the stream.
     *
     * @return total bytes written
     */
    long getTotalBytesWritten();

    /**
     * Gets the average read operation time in milliseconds.
     *
     * @return average read time
     */
    double getAverageReadTimeMs();

    /**
     * Gets the average write operation time in milliseconds.
     *
     * @return average write time
     */
    double getAverageWriteTimeMs();

    /**
     * Gets the peak transfer rate achieved.
     *
     * @return peak transfer rate in bytes per second
     */
    double getPeakTransferRateBytesPerSecond();

    /**
     * Gets the number of operations that were cancelled.
     *
     * @return number of cancelled operations
     */
    long getCancelledOperations();

    /**
     * Gets the number of operations that failed with errors.
     *
     * @return number of failed operations
     */
    long getFailedOperations();

    /**
     * Gets the time when the stream was created.
     *
     * @return creation timestamp in milliseconds since epoch
     */
    long getCreationTime();

    /**
     * Gets the total time the stream has been open.
     *
     * @return total open duration
     */
    Duration getTotalOpenTime();
  }

  // Factory methods

  /**
   * Creates a builder for memory stream configuration.
   *
   * @return a new MemoryStreamBuilder
   */
  static MemoryStreamBuilder builder() {
    return new MemoryStreamBuilderImpl();
  }

  /** Builder for configuring memory streams. */
  interface MemoryStreamBuilder {
    /**
     * Sets the buffer size for the stream.
     *
     * @param bufferSize buffer size in bytes
     * @return this builder
     */
    MemoryStreamBuilder bufferSize(final int bufferSize);

    /**
     * Enables or disables progress tracking.
     *
     * @param enabled true to enable progress tracking
     * @return this builder
     */
    MemoryStreamBuilder progressTracking(final boolean enabled);

    /**
     * Sets the default timeout for operations.
     *
     * @param timeout operation timeout
     * @return this builder
     */
    MemoryStreamBuilder timeout(final Duration timeout);

    /**
     * Enables or disables read operations.
     *
     * @param readable true to enable reading
     * @return this builder
     */
    MemoryStreamBuilder readable(final boolean readable);

    /**
     * Enables or disables write operations.
     *
     * @param writable true to enable writing
     * @return this builder
     */
    MemoryStreamBuilder writable(final boolean writable);

    /**
     * Enables or disables seek operations.
     *
     * @param seekable true to enable seeking
     * @return this builder
     */
    MemoryStreamBuilder seekable(final boolean seekable);

    /**
     * Sets the access mode for the stream.
     *
     * @param mode the access mode
     * @return this builder
     */
    MemoryStreamBuilder accessMode(final StreamingMemory.StreamMode mode);

    /**
     * Builds the memory stream configuration.
     *
     * @return stream configuration
     */
    StreamingMemory.StreamOptions build();
  }

  /** Default implementation of MemoryStreamBuilder. */
  final class MemoryStreamBuilderImpl implements MemoryStreamBuilder {
    private int bufferSize = 8192;
    private boolean progressTracking = false;
    private Duration timeout = null;
    private boolean readable = true;
    private boolean writable = true;
    private boolean seekable = true;
    private StreamingMemory.StreamMode accessMode = StreamingMemory.StreamMode.SEQUENTIAL;

    @Override
    public MemoryStreamBuilder bufferSize(final int bufferSize) {
      if (bufferSize <= 0) {
        throw new IllegalArgumentException("Buffer size must be positive");
      }
      this.bufferSize = bufferSize;
      return this;
    }

    @Override
    public MemoryStreamBuilder progressTracking(final boolean enabled) {
      this.progressTracking = enabled;
      return this;
    }

    @Override
    public MemoryStreamBuilder timeout(final Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    @Override
    public MemoryStreamBuilder readable(final boolean readable) {
      this.readable = readable;
      return this;
    }

    @Override
    public MemoryStreamBuilder writable(final boolean writable) {
      this.writable = writable;
      return this;
    }

    @Override
    public MemoryStreamBuilder seekable(final boolean seekable) {
      this.seekable = seekable;
      return this;
    }

    @Override
    public MemoryStreamBuilder accessMode(final StreamingMemory.StreamMode mode) {
      if (mode == null) {
        throw new IllegalArgumentException("Access mode cannot be null");
      }
      this.accessMode = mode;
      return this;
    }

    @Override
    public StreamingMemory.StreamOptions build() {
      return new StreamingMemory.StreamOptionsImpl(
          bufferSize,
          progressTracking,
          timeout,
          null, // executor
          true, // cancellable
          readable ? accessMode : StreamingMemory.StreamMode.SEQUENTIAL,
          writable ? accessMode : StreamingMemory.StreamMode.SEQUENTIAL);
    }
  }
}