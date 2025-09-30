package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

/**
 * Handle for manual feeding of WebAssembly bytecode during streaming compilation.
 *
 * <p>StreamingFeedHandle allows fine-grained control over the data feeding process during streaming
 * compilation. It supports both synchronous and asynchronous feeding patterns with backpressure
 * management.
 *
 * @since 1.0.0
 */
public interface StreamingFeedHandle extends AutoCloseable {

  /**
   * Feeds a chunk of WebAssembly bytecode to the compiler.
   *
   * <p>This method adds bytecode data to the compilation stream. The data will be processed
   * asynchronously, and the method returns immediately unless backpressure is applied.
   *
   * @param data the bytecode chunk to feed (must not be null)
   * @return a CompletableFuture that completes when the chunk has been accepted for processing
   * @throws IllegalArgumentException if data is null
   * @throws IllegalStateException if the handle is closed or completion has been signaled
   */
  CompletableFuture<Void> feed(ByteBuffer data);

  /**
   * Feeds a chunk of WebAssembly bytecode synchronously.
   *
   * <p>This method blocks until the data chunk has been accepted for processing. Use this method
   * when you need guarantees about data ordering or when implementing synchronous streaming
   * patterns.
   *
   * @param data the bytecode chunk to feed (must not be null)
   * @throws WasmException if feeding fails due to compilation errors
   * @throws IllegalArgumentException if data is null
   * @throws IllegalStateException if the handle is closed or completion has been signaled
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  void feedSync(ByteBuffer data) throws WasmException, InterruptedException;

  /**
   * Signals that no more data will be fed and completes the compilation.
   *
   * <p>This method must be called to indicate that all WebAssembly bytecode has been provided. The
   * returned future will complete when compilation is finished.
   *
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalStateException if completion has already been signaled
   */
  CompletableFuture<Module> complete();

  /**
   * Signals completion and returns the compiled module synchronously.
   *
   * <p>This method blocks until compilation is complete and returns the resulting Module.
   *
   * @return the compiled Module
   * @throws WasmException if compilation fails
   * @throws IllegalStateException if completion has already been signaled
   * @throws InterruptedException if the thread is interrupted while waiting
   */
  Module completeSync() throws WasmException, InterruptedException;

  /**
   * Gets the total number of bytes fed so far.
   *
   * @return total bytes fed
   */
  long getBytesFeeded();

  /**
   * Gets the current compilation statistics.
   *
   * @return current streaming compilation statistics
   */
  StreamingStatistics getStatistics();

  /**
   * Checks if the handle can accept more data without blocking.
   *
   * <p>This method can be used to implement flow control and avoid overwhelming the compiler with
   * data.
   *
   * @return true if data can be fed without blocking
   */
  boolean canFeed();

  /**
   * Gets the current buffer utilization as a percentage.
   *
   * <p>This can be used to monitor backpressure and adjust feeding rate accordingly.
   *
   * @return buffer utilization percentage (0.0 to 1.0)
   */
  double getBufferUtilization();

  /**
   * Registers a callback to be notified when the buffer has available space.
   *
   * <p>This allows implementation of efficient async feeding patterns without polling.
   *
   * @param callback callback to invoke when space becomes available
   * @throws IllegalArgumentException if callback is null
   */
  void onBufferAvailable(Runnable callback);

  /**
   * Cancels the streaming compilation and releases resources.
   *
   * <p>This method cancels any ongoing compilation and makes the handle unusable.
   *
   * @param mayInterruptIfRunning whether to interrupt compilation threads
   * @return true if cancellation was successful
   */
  boolean cancel(boolean mayInterruptIfRunning);

  /**
   * Checks if the compilation has been completed or cancelled.
   *
   * @return true if compilation is done
   */
  boolean isDone();

  /**
   * Closes the feed handle and releases associated resources.
   *
   * <p>If compilation is not complete, this method will attempt to cancel it gracefully.
   */
  @Override
  void close();
}
