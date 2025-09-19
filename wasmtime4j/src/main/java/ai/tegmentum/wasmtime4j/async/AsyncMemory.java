package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.WasmMemory;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Asynchronous WebAssembly memory interface.
 *
 * <p>An AsyncMemory extends the standard WasmMemory with non-blocking asynchronous operations for
 * memory read/write operations and streaming access. This enables better performance when
 * processing large amounts of data or when memory operations need to be performed without blocking
 * the main execution thread.
 *
 * <p>Async memory operations are particularly useful for I/O-heavy applications and scenarios
 * requiring high throughput data processing.
 *
 * @since 1.0.0
 */
public interface AsyncMemory extends WasmMemory {

  /**
   * Asynchronously reads data from memory at the specified offset.
   *
   * <p>This method performs a non-blocking read operation, returning a CompletableFuture that
   * completes with a ByteBuffer containing the data.
   *
   * @param offset the byte offset in memory to read from
   * @param length the number of bytes to read
   * @return a CompletableFuture that completes with the read data
   * @throws IllegalArgumentException if offset or length is negative
   */
  CompletableFuture<ByteBuffer> readAsync(final int offset, final int length);

  /**
   * Asynchronously writes data to memory at the specified offset.
   *
   * <p>This method performs a non-blocking write operation, returning a CompletableFuture that
   * completes when the write is finished.
   *
   * @param offset the byte offset in memory to write to
   * @param data the data to write
   * @return a CompletableFuture that completes when the write is finished
   * @throws IllegalArgumentException if offset is negative or data is null
   */
  CompletableFuture<Void> writeAsync(final int offset, final ByteBuffer data);

  /**
   * Asynchronously writes byte array data to memory at the specified offset.
   *
   * @param offset the byte offset in memory to write to
   * @param data the byte array to write
   * @return a CompletableFuture that completes when the write is finished
   * @throws IllegalArgumentException if offset is negative or data is null
   */
  CompletableFuture<Void> writeAsync(final int offset, final byte[] data);

  /**
   * Asynchronously performs bulk copy operation from another memory region.
   *
   * <p>This method efficiently copies data between memory regions without blocking the calling
   * thread, optimized for large data transfers.
   *
   * @param source the source memory to copy from
   * @param srcOffset the offset in the source memory
   * @param destOffset the offset in this memory
   * @param length the number of bytes to copy
   * @return a CompletableFuture that completes when the copy is finished
   * @throws IllegalArgumentException if source is null or offsets/length are invalid
   */
  CompletableFuture<Void> bulkCopyAsync(
      final AsyncMemory source, final int srcOffset, final int destOffset, final int length);

  /**
   * Asynchronously performs bulk fill operation.
   *
   * <p>This method efficiently fills a memory region with a specified value without blocking the
   * calling thread.
   *
   * @param offset the starting offset in memory
   * @param length the number of bytes to fill
   * @param value the byte value to fill with
   * @return a CompletableFuture that completes when the fill is finished
   * @throws IllegalArgumentException if offset or length is negative
   */
  CompletableFuture<Void> bulkFillAsync(final int offset, final int length, final byte value);

  /**
   * Asynchronously compares memory regions.
   *
   * <p>This method performs a non-blocking comparison of memory regions, returning the comparison
   * result asynchronously.
   *
   * @param other the other memory to compare with
   * @param thisOffset the offset in this memory
   * @param otherOffset the offset in the other memory
   * @param length the number of bytes to compare
   * @return a CompletableFuture that completes with the comparison result (0 if equal)
   * @throws IllegalArgumentException if other is null or offsets/length are invalid
   */
  CompletableFuture<Integer> compareAsync(
      final AsyncMemory other, final int thisOffset, final int otherOffset, final int length);

  /**
   * Creates an asynchronous InputStream for reading from memory.
   *
   * <p>This method creates a stream that can read data from memory asynchronously, useful for large
   * data processing scenarios.
   *
   * @param offset the starting offset in memory
   * @param length the maximum number of bytes to read
   * @return an InputStream that reads from memory asynchronously
   * @throws IllegalArgumentException if offset or length is negative
   */
  AsyncInputStream createAsyncInputStream(final int offset, final int length);

  /**
   * Creates an asynchronous OutputStream for writing to memory.
   *
   * <p>This method creates a stream that can write data to memory asynchronously, with automatic
   * bounds checking and growth handling.
   *
   * @param offset the starting offset in memory
   * @param maxLength the maximum number of bytes to write
   * @return an OutputStream that writes to memory asynchronously
   * @throws IllegalArgumentException if offset or maxLength is negative
   */
  AsyncOutputStream createAsyncOutputStream(final int offset, final int maxLength);

  /**
   * Gets statistics about async operations performed on this memory.
   *
   * @return async memory statistics
   */
  AsyncMemoryStatistics getAsyncStatistics();

  /** Asynchronous InputStream for reading from WebAssembly memory. */
  interface AsyncInputStream extends InputStream {
    /**
     * Asynchronously reads data into the provided buffer.
     *
     * @param buffer the buffer to read data into
     * @return a CompletableFuture that completes with the number of bytes read
     */
    CompletableFuture<Integer> readAsync(final ByteBuffer buffer);

    /**
     * Asynchronously reads a specified number of bytes.
     *
     * @param length the number of bytes to read
     * @return a CompletableFuture that completes with the read data
     */
    CompletableFuture<ByteBuffer> readAsync(final int length);

    /**
     * Asynchronously skips a specified number of bytes.
     *
     * @param n the number of bytes to skip
     * @return a CompletableFuture that completes with the actual number of bytes skipped
     */
    CompletableFuture<Long> skipAsync(final long n);

    /**
     * Gets the current position in the memory stream.
     *
     * @return the current position
     */
    long getPosition();

    /**
     * Gets the remaining bytes available for reading.
     *
     * @return the number of remaining bytes
     */
    long getRemaining();
  }

  /** Asynchronous OutputStream for writing to WebAssembly memory. */
  interface AsyncOutputStream extends OutputStream {
    /**
     * Asynchronously writes data from the provided buffer.
     *
     * @param buffer the buffer containing data to write
     * @return a CompletableFuture that completes when the write is finished
     */
    CompletableFuture<Void> writeAsync(final ByteBuffer buffer);

    /**
     * Asynchronously writes a byte array.
     *
     * @param data the data to write
     * @return a CompletableFuture that completes when the write is finished
     */
    CompletableFuture<Void> writeAsync(final byte[] data);

    /**
     * Asynchronously flushes any buffered data.
     *
     * @return a CompletableFuture that completes when the flush is finished
     */
    CompletableFuture<Void> flushAsync();

    /**
     * Gets the current position in the memory stream.
     *
     * @return the current position
     */
    long getPosition();

    /**
     * Gets the remaining space available for writing.
     *
     * @return the number of remaining bytes
     */
    long getRemaining();
  }

  /** Statistics for async memory operations. */
  interface AsyncMemoryStatistics {
    /**
     * Gets the total number of async read operations started.
     *
     * @return number of async reads
     */
    long getAsyncReadsStarted();

    /**
     * Gets the total number of async read operations completed successfully.
     *
     * @return number of successful async reads
     */
    long getAsyncReadsCompleted();

    /**
     * Gets the total number of async write operations started.
     *
     * @return number of async writes
     */
    long getAsyncWritesStarted();

    /**
     * Gets the total number of async write operations completed successfully.
     *
     * @return number of successful async writes
     */
    long getAsyncWritesCompleted();

    /**
     * Gets the total number of bulk operations performed.
     *
     * @return number of bulk operations
     */
    long getBulkOperationsPerformed();

    /**
     * Gets the total bytes read asynchronously.
     *
     * @return total bytes read
     */
    long getTotalBytesReadAsync();

    /**
     * Gets the total bytes written asynchronously.
     *
     * @return total bytes written
     */
    long getTotalBytesWrittenAsync();

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
     * Gets the current number of active async operations.
     *
     * @return number of active async operations
     */
    int getActiveAsyncOperations();

    /**
     * Gets the peak memory throughput recorded.
     *
     * @return peak throughput in bytes per second
     */
    long getPeakThroughputBytesPerSecond();
  }
}
