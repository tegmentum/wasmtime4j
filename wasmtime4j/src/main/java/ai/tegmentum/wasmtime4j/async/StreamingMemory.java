package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.WasmMemory;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Streaming WebAssembly memory interface.
 *
 * <p>A StreamingMemory extends the standard WasmMemory with asynchronous and streaming capabilities
 * for memory operations. This enables non-blocking memory access, bulk operations, and streaming
 * I/O for large data transfers.
 *
 * <p>This interface is particularly useful for applications that need to process large amounts of
 * data through WebAssembly memory without blocking the main thread, or when integrating with
 * streaming data sources and sinks.
 *
 * @since 1.0.0
 */
public interface StreamingMemory extends WasmMemory {

  /**
   * Asynchronously reads data from memory into a ByteBuffer.
   *
   * <p>This method performs non-blocking memory reads, making it suitable for large data transfers
   * or when the calling thread should not be blocked.
   *
   * @param offset the byte offset in memory to start reading from
   * @param length the number of bytes to read
   * @return a CompletableFuture that completes with a ByteBuffer containing the read data
   * @throws IllegalArgumentException if offset or length is negative
   * @throws IndexOutOfBoundsException if offset + length exceeds memory bounds
   */
  CompletableFuture<ByteBuffer> readAsync(final int offset, final int length);

  /**
   * Asynchronously reads data from memory with options.
   *
   * @param offset the byte offset in memory to start reading from
   * @param length the number of bytes to read
   * @param options read operation options
   * @return a CompletableFuture that completes with a ByteBuffer containing the read data
   * @throws IllegalArgumentException if offset, length is negative, or options is null
   */
  CompletableFuture<ByteBuffer> readAsync(
      final int offset, final int length, final ReadOptions options);

  /**
   * Asynchronously writes data from a ByteBuffer to memory.
   *
   * <p>This method performs non-blocking memory writes, enabling high-throughput data transfers
   * without blocking the calling thread.
   *
   * @param offset the byte offset in memory to start writing to
   * @param data the ByteBuffer containing data to write
   * @return a CompletableFuture that completes when the write operation finishes
   * @throws IllegalArgumentException if offset is negative or data is null
   * @throws IndexOutOfBoundsException if offset + data.remaining() exceeds memory bounds
   */
  CompletableFuture<Void> writeAsync(final int offset, final ByteBuffer data);

  /**
   * Asynchronously writes data to memory with options.
   *
   * @param offset the byte offset in memory to start writing to
   * @param data the ByteBuffer containing data to write
   * @param options write operation options
   * @return a CompletableFuture that completes when the write operation finishes
   * @throws IllegalArgumentException if offset is negative, data or options is null
   */
  CompletableFuture<Void> writeAsync(
      final int offset, final ByteBuffer data, final WriteOptions options);

  /**
   * Creates an InputStream for reading from memory.
   *
   * <p>The returned InputStream provides sequential access to memory contents starting from the
   * specified offset. The stream will read up to the specified length of bytes.
   *
   * @param offset the starting byte offset in memory
   * @param length the maximum number of bytes to read, or -1 for unlimited
   * @return an InputStream for reading from memory
   * @throws IllegalArgumentException if offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   */
  InputStream createInputStream(final int offset, final int length);

  /**
   * Creates an OutputStream for writing to memory.
   *
   * <p>The returned OutputStream provides sequential access for writing to memory starting from the
   * specified offset. The stream will allow writing up to maxLength bytes.
   *
   * @param offset the starting byte offset in memory
   * @param maxLength the maximum number of bytes to write, or -1 for unlimited
   * @return an OutputStream for writing to memory
   * @throws IllegalArgumentException if offset is negative
   * @throws IndexOutOfBoundsException if offset exceeds memory bounds
   */
  OutputStream createOutputStream(final int offset, final int maxLength);

  /**
   * Asynchronously copies data from another StreamingMemory.
   *
   * <p>This method performs efficient bulk copying between memory instances, which can be optimized
   * at the native level for better performance than individual read/write operations.
   *
   * @param source the source memory to copy from
   * @param srcOffset the offset in the source memory
   * @param destOffset the offset in this memory
   * @param length the number of bytes to copy
   * @return a CompletableFuture that completes when the copy operation finishes
   * @throws IllegalArgumentException if source is null or any offset/length is negative
   */
  CompletableFuture<Void> bulkCopy(
      final StreamingMemory source, final int srcOffset, final int destOffset, final int length);

  /**
   * Asynchronously fills a region of memory with a specific byte value.
   *
   * <p>This method efficiently sets a contiguous region of memory to the specified value, which is
   * useful for initialization or clearing operations.
   *
   * @param offset the starting byte offset in memory
   * @param length the number of bytes to fill
   * @param value the byte value to fill with
   * @return a CompletableFuture that completes when the fill operation finishes
   * @throws IllegalArgumentException if offset or length is negative
   */
  CompletableFuture<Void> bulkFill(final int offset, final int length, final byte value);

  /**
   * Creates a memory stream for advanced streaming operations.
   *
   * <p>A MemoryStream provides more advanced streaming capabilities than basic InputStream/
   * OutputStream, including async operations, progress tracking, and cancellation support.
   *
   * @param offset the starting offset in memory
   * @param length the length of the stream, or -1 for unlimited
   * @param options stream configuration options
   * @return a MemoryStream for advanced streaming operations
   * @throws IllegalArgumentException if offset is negative or options is null
   */
  MemoryStream createMemoryStream(final int offset, final int length, final StreamOptions options);

  /**
   * Gets statistics about streaming operations performed on this memory.
   *
   * @return streaming memory statistics
   */
  StreamingMemoryStatistics getStreamingStatistics();

  /** Enhanced memory stream interface for streaming operations. */
  interface MemoryStream {
    /**
     * Asynchronously reads data into the provided buffer.
     *
     * @param buffer the buffer to read data into
     * @return a CompletableFuture that completes with the number of bytes read
     */
    CompletableFuture<Integer> readAsync(final ByteBuffer buffer);

    /**
     * Asynchronously writes data from the provided buffer.
     *
     * @param buffer the buffer containing data to write
     * @return a CompletableFuture that completes when the write is finished
     */
    CompletableFuture<Integer> writeAsync(final ByteBuffer buffer);

    /**
     * Asynchronously flushes any buffered data.
     *
     * @return a CompletableFuture that completes when the flush is finished
     */
    CompletableFuture<Void> flush();

    /** Closes the memory stream and releases associated resources. */
    void close();

    /**
     * Gets the current position in the memory stream.
     *
     * @return the current position
     */
    long getPosition();

    /**
     * Sets the position in the memory stream.
     *
     * @param position the new position
     * @throws IllegalArgumentException if position is negative
     */
    void setPosition(final long position);

    /**
     * Gets the remaining bytes available in the stream.
     *
     * @return the number of remaining bytes
     */
    long getRemaining();

    /**
     * Checks if the stream has reached the end.
     *
     * @return true if at end of stream
     */
    boolean isEOF();

    /**
     * Gets the stream configuration.
     *
     * @return stream configuration
     */
    StreamOptions getStreamOptions();
  }

  /** Configuration options for memory read operations. */
  interface ReadOptions {
    /**
     * Gets the timeout for read operations.
     *
     * @return timeout duration, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the custom executor for read operations.
     *
     * @return custom executor, or null to use default
     */
    Executor getExecutor();

    /**
     * Gets the buffer size for chunked reads.
     *
     * @return buffer size in bytes
     */
    int getBufferSize();

    /**
     * Checks if the read operation can be cancelled.
     *
     * @return true if cancellation is supported
     */
    boolean isCancellable();

    /**
     * Gets the priority level for this read operation.
     *
     * @return priority level (higher values indicate higher priority)
     */
    int getPriority();
  }

  /** Configuration options for memory write operations. */
  interface WriteOptions {
    /**
     * Gets the timeout for write operations.
     *
     * @return timeout duration, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the custom executor for write operations.
     *
     * @return custom executor, or null to use default
     */
    Executor getExecutor();

    /**
     * Gets the buffer size for chunked writes.
     *
     * @return buffer size in bytes
     */
    int getBufferSize();

    /**
     * Checks if the write operation can be cancelled.
     *
     * @return true if cancellation is supported
     */
    boolean isCancellable();

    /**
     * Checks if writes should be flushed immediately.
     *
     * @return true if immediate flush is enabled
     */
    boolean isImmediateFlush();

    /**
     * Gets the priority level for this write operation.
     *
     * @return priority level (higher values indicate higher priority)
     */
    int getPriority();
  }

  /** Configuration options for memory streams. */
  interface StreamOptions {
    /**
     * Gets the buffer size for streaming operations.
     *
     * @return buffer size in bytes
     */
    int getBufferSize();

    /**
     * Checks if progress tracking is enabled.
     *
     * @return true if progress tracking is enabled
     */
    boolean isProgressTrackingEnabled();

    /**
     * Gets the timeout for stream operations.
     *
     * @return timeout duration, or null for no timeout
     */
    Duration getTimeout();

    /**
     * Gets the custom executor for stream operations.
     *
     * @return custom executor, or null to use default
     */
    Executor getExecutor();

    /**
     * Checks if the stream operations can be cancelled.
     *
     * @return true if cancellation is supported
     */
    boolean isCancellable();

    /**
     * Gets the read mode for the stream.
     *
     * @return the stream read mode
     */
    StreamMode getReadMode();

    /**
     * Gets the write mode for the stream.
     *
     * @return the stream write mode
     */
    StreamMode getWriteMode();
  }

  /** Stream access modes. */
  enum StreamMode {
    /** Sequential access only. */
    SEQUENTIAL,

    /** Random access allowed. */
    RANDOM,

    /** Buffered access with read-ahead. */
    BUFFERED,

    /** Direct unbuffered access. */
    DIRECT
  }

  /** Statistics for streaming memory operations. */
  interface StreamingMemoryStatistics {
    /**
     * Gets the total number of async read operations started.
     *
     * @return number of async reads
     */
    long getAsyncReadsStarted();

    /**
     * Gets the total number of async read operations completed.
     *
     * @return number of completed async reads
     */
    long getAsyncReadsCompleted();

    /**
     * Gets the total number of async read operations that failed.
     *
     * @return number of failed async reads
     */
    long getAsyncReadsFailed();

    /**
     * Gets the total number of async write operations started.
     *
     * @return number of async writes
     */
    long getAsyncWritesStarted();

    /**
     * Gets the total number of async write operations completed.
     *
     * @return number of completed async writes
     */
    long getAsyncWritesCompleted();

    /**
     * Gets the total number of async write operations that failed.
     *
     * @return number of failed async writes
     */
    long getAsyncWritesFailed();

    /**
     * Gets the total number of bulk copy operations performed.
     *
     * @return number of bulk copy operations
     */
    long getBulkCopyOperations();

    /**
     * Gets the total number of bulk fill operations performed.
     *
     * @return number of bulk fill operations
     */
    long getBulkFillOperations();

    /**
     * Gets the total number of bytes read asynchronously.
     *
     * @return total bytes read
     */
    long getTotalBytesRead();

    /**
     * Gets the total number of bytes written asynchronously.
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
     * Gets the current number of active streaming operations.
     *
     * @return number of active operations
     */
    int getActiveStreamingOperations();

    /**
     * Gets the peak memory usage for streaming operations.
     *
     * @return peak memory usage in bytes
     */
    long getPeakStreamingMemoryUsage();
  }

  // Factory methods for creating options

  /**
   * Creates default read options.
   *
   * @return default read options
   */
  static ReadOptions createDefaultReadOptions() {
    return new ReadOptionsImpl(null, null, 8192, true, 1);
  }

  /**
   * Creates default write options.
   *
   * @return default write options
   */
  static WriteOptions createDefaultWriteOptions() {
    return new WriteOptionsImpl(null, null, 8192, true, false, 1);
  }

  /**
   * Creates default stream options.
   *
   * @return default stream options
   */
  static StreamOptions createDefaultStreamOptions() {
    return new StreamOptionsImpl(
        8192, false, null, null, true, StreamMode.SEQUENTIAL, StreamMode.SEQUENTIAL);
  }

  // Implementation classes

  /** Default implementation of ReadOptions. */
  final class ReadOptionsImpl implements ReadOptions {
    private final Duration timeout;
    private final Executor executor;
    private final int bufferSize;
    private final boolean cancellable;
    private final int priority;

    ReadOptionsImpl(
        final Duration timeout,
        final Executor executor,
        final int bufferSize,
        final boolean cancellable,
        final int priority) {
      this.timeout = timeout;
      this.executor = executor;
      this.bufferSize = bufferSize;
      this.cancellable = cancellable;
      this.priority = priority;
    }

    @Override
    public Duration getTimeout() {
      return timeout;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public boolean isCancellable() {
      return cancellable;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }

  /** Default implementation of WriteOptions. */
  final class WriteOptionsImpl implements WriteOptions {
    private final Duration timeout;
    private final Executor executor;
    private final int bufferSize;
    private final boolean cancellable;
    private final boolean immediateFlush;
    private final int priority;

    WriteOptionsImpl(
        final Duration timeout,
        final Executor executor,
        final int bufferSize,
        final boolean cancellable,
        final boolean immediateFlush,
        final int priority) {
      this.timeout = timeout;
      this.executor = executor;
      this.bufferSize = bufferSize;
      this.cancellable = cancellable;
      this.immediateFlush = immediateFlush;
      this.priority = priority;
    }

    @Override
    public Duration getTimeout() {
      return timeout;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public boolean isCancellable() {
      return cancellable;
    }

    @Override
    public boolean isImmediateFlush() {
      return immediateFlush;
    }

    @Override
    public int getPriority() {
      return priority;
    }
  }

  /** Default implementation of StreamOptions. */
  final class StreamOptionsImpl implements StreamOptions {
    private final int bufferSize;
    private final boolean progressTrackingEnabled;
    private final Duration timeout;
    private final Executor executor;
    private final boolean cancellable;
    private final StreamMode readMode;
    private final StreamMode writeMode;

    StreamOptionsImpl(
        final int bufferSize,
        final boolean progressTrackingEnabled,
        final Duration timeout,
        final Executor executor,
        final boolean cancellable,
        final StreamMode readMode,
        final StreamMode writeMode) {
      this.bufferSize = bufferSize;
      this.progressTrackingEnabled = progressTrackingEnabled;
      this.timeout = timeout;
      this.executor = executor;
      this.cancellable = cancellable;
      this.readMode = readMode;
      this.writeMode = writeMode;
    }

    @Override
    public int getBufferSize() {
      return bufferSize;
    }

    @Override
    public boolean isProgressTrackingEnabled() {
      return progressTrackingEnabled;
    }

    @Override
    public Duration getTimeout() {
      return timeout;
    }

    @Override
    public Executor getExecutor() {
      return executor;
    }

    @Override
    public boolean isCancellable() {
      return cancellable;
    }

    @Override
    public StreamMode getReadMode() {
      return readMode;
    }

    @Override
    public StreamMode getWriteMode() {
      return writeMode;
    }
  }
}
