package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.StreamingFeedHandle;
import ai.tegmentum.wasmtime4j.StreamingStatistics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Panama implementation of StreamingFeedHandle.
 *
 * <p>This class provides manual feeding of WebAssembly bytecode chunks to a streaming compiler
 * through Panama Foreign Function API calls to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaStreamingFeedHandle implements StreamingFeedHandle {

  private static final Logger LOGGER = Logger.getLogger(PanamaStreamingFeedHandle.class.getName());

  private final PanamaStreamingCompiler compiler;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicBoolean completed = new AtomicBoolean(false);
  private final AtomicLong bytesFeeded = new AtomicLong(0);
  private final Arena arena = Arena.ofShared();
  private volatile Runnable bufferAvailableCallback;

  /**
   * Creates a new Panama streaming feed handle.
   *
   * @param compiler the streaming compiler to feed data to
   */
  PanamaStreamingFeedHandle(final PanamaStreamingCompiler compiler) {
    this.compiler = PanamaValidation.requireNonNull(compiler, "compiler");
    LOGGER.fine("Created Panama streaming feed handle for compiler");
  }

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
  @Override
  public CompletableFuture<Void> feed(final ByteBuffer data) {
    PanamaValidation.requireNonNull(data, "data");
    ensureNotClosed();
    ensureNotCompleted();

    return CompletableFuture.runAsync(() -> {
      try {
        feedSync(data);
      } catch (final WasmException | InterruptedException e) {
        throw new RuntimeException("Failed to feed data chunk", e);
      }
    });
  }

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
  @Override
  public void feedSync(final ByteBuffer data) throws WasmException, InterruptedException {
    PanamaValidation.requireNonNull(data, "data");
    ensureNotClosed();
    ensureNotCompleted();

    final byte[] dataArray = new byte[data.remaining()];
    data.get(dataArray);

    try {
      compiler.feedChunk(arena, dataArray, dataArray.length);
      bytesFeeded.addAndGet(dataArray.length);

      // Notify buffer available callback if set
      final Runnable callback = bufferAvailableCallback;
      if (callback != null) {
        callback.run();
      }
    } catch (final Exception e) {
      throw new WasmException("Failed to feed data chunk: " + e.getMessage(), e);
    }
  }

  /**
   * Signals that no more data will be fed and completes the compilation.
   *
   * <p>This method must be called to indicate that all WebAssembly bytecode has been provided.
   * The returned future will complete when compilation is finished.
   *
   * @return a CompletableFuture that completes with the compiled Module
   * @throws IllegalStateException if completion has already been signaled
   */
  @Override
  public CompletableFuture<Module> complete() {
    ensureNotClosed();
    ensureNotCompleted();

    completed.set(true);

    return CompletableFuture.supplyAsync(() -> {
      try {
        return completeSync();
      } catch (final WasmException | InterruptedException e) {
        throw new RuntimeException("Failed to complete compilation", e);
      }
    });
  }

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
  @Override
  public Module completeSync() throws WasmException, InterruptedException {
    ensureNotClosed();

    if (!completed.get()) {
      completed.set(true);
    }

    try {
      return compiler.complete(arena);
    } catch (final Exception e) {
      throw new WasmException("Failed to complete streaming compilation: " + e.getMessage(), e);
    }
  }

  /**
   * Gets the total number of bytes fed so far.
   *
   * @return total bytes fed
   */
  @Override
  public long getBytesFeeded() {
    return bytesFeeded.get();
  }

  /**
   * Gets the current compilation statistics.
   *
   * @return current streaming compilation statistics
   */
  @Override
  public StreamingStatistics getStatistics() {
    ensureNotClosed();
    return compiler.getStatistics();
  }

  /**
   * Checks if the handle can accept more data without blocking.
   *
   * <p>This method can be used to implement flow control and avoid overwhelming the compiler
   * with data.
   *
   * @return true if data can be fed without blocking
   */
  @Override
  public boolean canFeed() {
    if (closed.get() || completed.get()) {
      return false;
    }

    // For now, assume we can always feed (no backpressure implemented)
    return true;
  }

  /**
   * Gets the current buffer utilization as a percentage.
   *
   * <p>This can be used to monitor backpressure and adjust feeding rate accordingly.
   *
   * @return buffer utilization percentage (0.0 to 1.0)
   */
  @Override
  public double getBufferUtilization() {
    if (closed.get() || completed.get()) {
      return 1.0;
    }

    // For now, return low utilization (no backpressure monitoring implemented)
    return 0.1;
  }

  /**
   * Registers a callback to be notified when the buffer has available space.
   *
   * <p>This allows implementation of efficient async feeding patterns without polling.
   *
   * @param callback callback to invoke when space becomes available
   * @throws IllegalArgumentException if callback is null
   */
  @Override
  public void onBufferAvailable(final Runnable callback) {
    PanamaValidation.requireNonNull(callback, "callback");
    this.bufferAvailableCallback = callback;
  }

  /**
   * Cancels the streaming compilation and releases resources.
   *
   * <p>This method cancels any ongoing compilation and makes the handle unusable.
   *
   * @param mayInterruptIfRunning whether to interrupt compilation threads
   * @return true if cancellation was successful
   */
  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (closed.get()) {
      return false;
    }

    try {
      return compiler.cancel(mayInterruptIfRunning);
    } catch (final Exception e) {
      LOGGER.warning("Failed to cancel streaming compilation: " + e.getMessage());
      return false;
    }
  }

  /**
   * Checks if the compilation has been completed or cancelled.
   *
   * @return true if compilation is done
   */
  @Override
  public boolean isDone() {
    return closed.get() || completed.get() || compiler.isDone();
  }

  /**
   * Closes the feed handle and releases associated resources.
   *
   * <p>If compilation is not complete, this method will attempt to cancel it gracefully.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.fine("Closing Panama streaming feed handle");

      // Cancel compilation if not completed
      if (!completed.get()) {
        cancel(false);
      }

      // Close the arena
      arena.close();
    }
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("StreamingFeedHandle has been closed");
    }
  }

  private void ensureNotCompleted() {
    if (completed.get()) {
      throw new IllegalStateException("Compilation has already been completed");
    }
  }
}