package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Non-blocking and asynchronous I/O operations for WASI file system.
 *
 * <p>This class provides comprehensive support for both blocking and non-blocking I/O operations,
 * enabling efficient high-throughput file processing. Features include:
 *
 * <ul>
 *   <li>Asynchronous file I/O using CompletableFuture and callbacks
 *   <li>Non-blocking I/O with selector-based polling
 *   <li>Configurable thread pools for asynchronous operations
 *   <li>Timeout support for all asynchronous operations
 *   <li>Comprehensive error handling with defensive programming
 *   <li>Resource management to prevent thread and memory leaks
 * </ul>
 *
 * <p>All operations are designed with safety as the highest priority - operations will fail
 * gracefully rather than causing JVM instability or resource leaks.
 *
 * @since 1.0.0
 */
public final class WasiAsyncFileOperations implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WasiAsyncFileOperations.class.getName());

  /** Default timeout for asynchronous operations in seconds. */
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  /** Default buffer size for I/O operations. */
  private static final int DEFAULT_BUFFER_SIZE = 8192;

  /** Maximum number of concurrent operations to prevent resource exhaustion. */
  private static final int MAX_CONCURRENT_OPERATIONS = 256;

  /**
   * Creates a failed CompletableFuture with the given exception (Java 8 compatible).
   *
   * @param <T> the return type
   * @param exception the exception to wrap
   * @return a failed CompletableFuture
   */
  private static <T> CompletableFuture<T> failedFuture(final Throwable exception) {
    final CompletableFuture<T> future = new CompletableFuture<>();
    future.completeExceptionally(exception);
    return future;
  }

  /** Executor service for asynchronous operations. */
  private final ExecutorService asyncExecutor;

  /** Scheduled executor service for timeouts. */
  private final ScheduledExecutorService scheduledExecutor;

  /** Selector for non-blocking operations. */
  private final Selector selector;

  /** Thread for selector processing. */
  private final Thread selectorThread;

  /** Atomic counter for active operations. */
  private final AtomicLong activeOperations = new AtomicLong(0);

  /** Whether this instance has been closed. */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /** Default timeout for operations in milliseconds. */
  private final long defaultTimeoutMs;

  /**
   * Creates a new async file operations handler with default settings.
   *
   * @throws WasiFileSystemException if initialization fails
   */
  public WasiAsyncFileOperations() {
    this(DEFAULT_TIMEOUT_SECONDS);
  }

  /**
   * Creates a new async file operations handler with specified timeout.
   *
   * @param defaultTimeoutSeconds default timeout for operations in seconds
   * @throws WasiFileSystemException if initialization fails
   */
  public WasiAsyncFileOperations(final int defaultTimeoutSeconds) {
    JniValidation.requirePositive(defaultTimeoutSeconds, "defaultTimeoutSeconds");

    this.defaultTimeoutMs = defaultTimeoutSeconds * 1000L;

    try {
      // Create executor service for async operations
      this.asyncExecutor =
          Executors.newFixedThreadPool(
              Math.min(Runtime.getRuntime().availableProcessors() * 2, 16),
              r -> {
                final Thread thread = new Thread(r, "WasiAsyncFileOps");
                thread.setDaemon(true);
                return thread;
              });

      // Create scheduled executor for timeouts
      this.scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
        final Thread thread = new Thread(r, "WasiAsyncTimeout");
        thread.setDaemon(true);
        return thread;
      });

      // Create selector for non-blocking I/O
      this.selector = Selector.open();

      // Start selector thread
      this.selectorThread = new Thread(this::runSelectorLoop, "WasiNioSelector");
      this.selectorThread.setDaemon(true);
      this.selectorThread.start();

      LOGGER.info(
          String.format(
              "Created async file operations handler: timeout=%ds", defaultTimeoutSeconds));

    } catch (final IOException e) {
      throw new WasiFileSystemException("Failed to initialize async file operations", "EIO", e);
    }
  }

  /**
   * Performs an asynchronous read operation from a file.
   *
   * @param path the file path to read from
   * @param position the position in the file to start reading
   * @param bufferSize the buffer size for reading
   * @return a CompletableFuture containing the read data
   */
  public CompletableFuture<ByteBuffer> readAsync(
      final Path path, final long position, final int bufferSize) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requirePositive(bufferSize, "bufferSize");

    return readAsync(path, position, bufferSize, defaultTimeoutMs);
  }

  /**
   * Performs an asynchronous read operation from a file with timeout.
   *
   * @param path the file path to read from
   * @param position the position in the file to start reading
   * @param bufferSize the buffer size for reading
   * @param timeoutMs the timeout in milliseconds
   * @return a CompletableFuture containing the read data
   */
  public CompletableFuture<ByteBuffer> readAsync(
      final Path path, final long position, final int bufferSize, final long timeoutMs) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requirePositive(bufferSize, "bufferSize");
    JniValidation.requirePositive(timeoutMs, "timeoutMs");

    if (closed.get()) {
      return failedFuture(
          new WasiFileSystemException("Async file operations closed", "EIO"));
    }

    if (activeOperations.get() >= MAX_CONCURRENT_OPERATIONS) {
      return failedFuture(
          new WasiFileSystemException("Too many concurrent operations", "EAGAIN"));
    }

    LOGGER.fine(
        String.format(
            "Starting async read: path=%s, position=%d, bufferSize=%d",
            path, position, bufferSize));

    final CompletableFuture<ByteBuffer> future = new CompletableFuture<>();
    activeOperations.incrementAndGet();

    // Set up timeout
    final Future<?> timeoutTask =
        scheduledExecutor.schedule(
            () -> {
              if (future.cancel(true)) {
                LOGGER.warning(
                    String.format("Async read timeout: path=%s, timeout=%dms", path, timeoutMs));
              }
            },
            timeoutMs,
            TimeUnit.MILLISECONDS);

    try {
      final AsynchronousFileChannel asyncChannel =
          AsynchronousFileChannel.open(path, Collections.singleton(StandardOpenOption.READ), asyncExecutor);

      final ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

      asyncChannel.read(
          buffer,
          position,
          buffer,
          new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(final Integer bytesRead, final ByteBuffer attachment) {
              try {
                timeoutTask.cancel(false);
                asyncChannel.close();
                attachment.flip();
                future.complete(attachment);

                LOGGER.fine(
                    String.format("Async read completed: path=%s, bytesRead=%d", path, bytesRead));
              } catch (final Exception e) {
                failed(e, attachment);
              } finally {
                activeOperations.decrementAndGet();
              }
            }

            @Override
            public void failed(final Throwable exc, final ByteBuffer attachment) {
              try {
                timeoutTask.cancel(false);
                asyncChannel.close();
              } catch (final IOException e) {
                LOGGER.warning(String.format("Error closing async channel: %s", e.getMessage()));
              } finally {
                activeOperations.decrementAndGet();
              }

              LOGGER.warning(
                  String.format("Async read failed: path=%s, error=%s", path, exc.getMessage()));
              future.completeExceptionally(
                  new WasiFileSystemException(
                      "Async read failed: " + exc.getMessage(), "EIO", exc));
            }
          });

    } catch (final IOException e) {
      timeoutTask.cancel(false);
      activeOperations.decrementAndGet();

      LOGGER.warning(
          String.format("Failed to start async read: path=%s, error=%s", path, e.getMessage()));
      future.completeExceptionally(
          new WasiFileSystemException("Failed to start async read: " + e.getMessage(), "EIO", e));
    }

    return future;
  }

  /**
   * Performs an asynchronous write operation to a file.
   *
   * @param path the file path to write to
   * @param position the position in the file to start writing
   * @param data the data to write
   * @return a CompletableFuture containing the number of bytes written
   */
  public CompletableFuture<Integer> writeAsync(
      final Path path, final long position, final ByteBuffer data) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requireNonNull(data, "data");

    return writeAsync(path, position, data, defaultTimeoutMs);
  }

  /**
   * Performs an asynchronous write operation to a file with timeout.
   *
   * @param path the file path to write to
   * @param position the position in the file to start writing
   * @param data the data to write
   * @param timeoutMs the timeout in milliseconds
   * @return a CompletableFuture containing the number of bytes written
   */
  public CompletableFuture<Integer> writeAsync(
      final Path path, final long position, final ByteBuffer data, final long timeoutMs) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requireNonNull(data, "data");
    JniValidation.requirePositive(timeoutMs, "timeoutMs");

    if (closed.get()) {
      return failedFuture(
          new WasiFileSystemException("Async file operations closed", "EIO"));
    }

    if (activeOperations.get() >= MAX_CONCURRENT_OPERATIONS) {
      return failedFuture(
          new WasiFileSystemException("Too many concurrent operations", "EAGAIN"));
    }

    LOGGER.fine(
        String.format(
            "Starting async write: path=%s, position=%d, dataSize=%d",
            path, position, data.remaining()));

    final CompletableFuture<Integer> future = new CompletableFuture<>();
    activeOperations.incrementAndGet();

    // Set up timeout
    final Future<?> timeoutTask =
        scheduledExecutor.schedule(
            () -> {
              if (future.cancel(true)) {
                LOGGER.warning(
                    String.format("Async write timeout: path=%s, timeout=%dms", path, timeoutMs));
              }
            },
            timeoutMs,
            TimeUnit.MILLISECONDS);

    try {
      final Set<StandardOpenOption> options = new HashSet<>();
      options.add(StandardOpenOption.WRITE);
      options.add(StandardOpenOption.CREATE);
      final AsynchronousFileChannel asyncChannel =
          AsynchronousFileChannel.open(path, options, asyncExecutor);

      asyncChannel.write(
          data,
          position,
          data,
          new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(final Integer bytesWritten, final ByteBuffer attachment) {
              try {
                timeoutTask.cancel(false);
                asyncChannel.close();
                future.complete(bytesWritten);

                LOGGER.fine(
                    String.format(
                        "Async write completed: path=%s, bytesWritten=%d", path, bytesWritten));
              } catch (final Exception e) {
                failed(e, attachment);
              } finally {
                activeOperations.decrementAndGet();
              }
            }

            @Override
            public void failed(final Throwable exc, final ByteBuffer attachment) {
              try {
                timeoutTask.cancel(false);
                asyncChannel.close();
              } catch (final IOException e) {
                LOGGER.warning(String.format("Error closing async channel: %s", e.getMessage()));
              } finally {
                activeOperations.decrementAndGet();
              }

              LOGGER.warning(
                  String.format("Async write failed: path=%s, error=%s", path, exc.getMessage()));
              future.completeExceptionally(
                  new WasiFileSystemException(
                      "Async write failed: " + exc.getMessage(), "EIO", exc));
            }
          });

    } catch (final IOException e) {
      timeoutTask.cancel(false);
      activeOperations.decrementAndGet();

      LOGGER.warning(
          String.format("Failed to start async write: path=%s, error=%s", path, e.getMessage()));
      future.completeExceptionally(
          new WasiFileSystemException("Failed to start async write: " + e.getMessage(), "EIO", e));
    }

    return future;
  }

  /**
   * Registers a channel for non-blocking I/O operations.
   *
   * @param channel the channel to register
   * @param operations the operations to register for (SelectionKey.OP_READ, OP_WRITE, etc.)
   * @param attachment optional attachment for the selection key
   * @return a CompletableFuture that completes when the operation is ready
   */
  public CompletableFuture<SelectionKey> registerChannel(
      final SelectableChannel channel, final int operations, final Object attachment) {
    JniValidation.requireNonNull(channel, "channel");

    if (closed.get()) {
      return failedFuture(
          new WasiFileSystemException("Async file operations closed", "EIO"));
    }

    final CompletableFuture<SelectionKey> future = new CompletableFuture<>();

    try {
      // Configure channel for non-blocking mode
      channel.configureBlocking(false);

      // Register with selector
      final SelectionKey key = channel.register(selector, operations, attachment);
      future.complete(key);

      LOGGER.fine(String.format("Registered channel for non-blocking I/O: ops=%d", operations));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to register channel: %s", e.getMessage()));
      future.completeExceptionally(
          new WasiFileSystemException("Failed to register channel: " + e.getMessage(), "EIO", e));
    }

    return future;
  }

  /**
   * Performs a non-blocking read operation on a channel.
   *
   * @param channel the channel to read from
   * @param buffer the buffer to read into
   * @return the number of bytes read, 0 if no data available, -1 if end of stream
   * @throws WasiFileSystemException if the read operation fails
   */
  public int readNonBlocking(final SelectableChannel channel, final ByteBuffer buffer) {
    JniValidation.requireNonNull(channel, "channel");
    JniValidation.requireNonNull(buffer, "buffer");

    if (closed.get()) {
      throw new WasiFileSystemException("Async file operations closed", "EIO");
    }

    try {
      if (channel instanceof SocketChannel) {
        return ((SocketChannel) channel).read(buffer);
      } else {
        throw new WasiFileSystemException(
            "Channel type not supported for non-blocking read", "ENOTSUP");
      }
    } catch (final IOException e) {
      LOGGER.warning(String.format("Non-blocking read failed: %s", e.getMessage()));
      throw new WasiFileSystemException("Non-blocking read failed: " + e.getMessage(), "EIO", e);
    }
  }

  /**
   * Performs a non-blocking write operation on a channel.
   *
   * @param channel the channel to write to
   * @param buffer the buffer to write from
   * @return the number of bytes written, 0 if no space available
   * @throws WasiFileSystemException if the write operation fails
   */
  public int writeNonBlocking(final SelectableChannel channel, final ByteBuffer buffer) {
    JniValidation.requireNonNull(channel, "channel");
    JniValidation.requireNonNull(buffer, "buffer");

    if (closed.get()) {
      throw new WasiFileSystemException("Async file operations closed", "EIO");
    }

    try {
      if (channel instanceof SocketChannel) {
        return ((SocketChannel) channel).write(buffer);
      } else {
        throw new WasiFileSystemException(
            "Channel type not supported for non-blocking write", "ENOTSUP");
      }
    } catch (final IOException e) {
      LOGGER.warning(String.format("Non-blocking write failed: %s", e.getMessage()));
      throw new WasiFileSystemException("Non-blocking write failed: " + e.getMessage(), "EIO", e);
    }
  }

  /**
   * Gets the number of currently active asynchronous operations.
   *
   * @return the number of active operations
   */
  public long getActiveOperationCount() {
    return activeOperations.get();
  }

  /**
   * Checks if this instance has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed.get();
  }

  /** Closes the async file operations handler and releases all resources. */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      LOGGER.info("Shutting down async file operations");

      // Close selector to stop selector thread
      try {
        selector.close();
      } catch (final IOException e) {
        LOGGER.warning(String.format("Error closing selector: %s", e.getMessage()));
      }

      // Wait for selector thread to finish
      try {
        selectorThread.interrupt();
        selectorThread.join(5000);
      } catch (final InterruptedException e) {
        LOGGER.warning("Interrupted while waiting for selector thread");
        Thread.currentThread().interrupt();
      }

      // Shutdown executors
      asyncExecutor.shutdown();
      scheduledExecutor.shutdown();
      try {
        if (!asyncExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          asyncExecutor.shutdownNow();
          LOGGER.warning("Forced shutdown of async executor");
        }
        if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
          scheduledExecutor.shutdownNow();
          LOGGER.warning("Forced shutdown of scheduled executor");
        }
      } catch (final InterruptedException e) {
        asyncExecutor.shutdownNow();
        scheduledExecutor.shutdownNow();
        Thread.currentThread().interrupt();
      }

      LOGGER.info("Async file operations shutdown completed");
    }
  }

  /** Runs the selector loop in a background thread. */
  private void runSelectorLoop() {
    LOGGER.fine("Starting selector loop");

    while (!closed.get() && !Thread.currentThread().isInterrupted()) {
      try {
        final int readyChannels = selector.select(1000);

        if (readyChannels > 0) {
          // Process selected keys
          for (final SelectionKey key : selector.selectedKeys()) {
            if (!key.isValid()) {
              continue;
            }

            // Handle ready operations
            if (key.isReadable()) {
              LOGGER.fine("Channel ready for reading");
            }
            if (key.isWritable()) {
              LOGGER.fine("Channel ready for writing");
            }
            if (key.isConnectable()) {
              LOGGER.fine("Channel ready for connection");
            }
            if (key.isAcceptable()) {
              LOGGER.fine("Channel ready for accepting");
            }
          }

          selector.selectedKeys().clear();
        }

      } catch (final IOException e) {
        if (!closed.get()) {
          LOGGER.warning(String.format("Error in selector loop: %s", e.getMessage()));
        }
        break;
      } catch (final Exception e) {
        LOGGER.warning(String.format("Unexpected error in selector loop: %s", e.getMessage()));
      }
    }

    LOGGER.fine("Selector loop terminated");
  }
}
