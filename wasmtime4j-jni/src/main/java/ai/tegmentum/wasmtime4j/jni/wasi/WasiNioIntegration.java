package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;

/**
 * Java NIO integration for efficient WASI file operations.
 *
 * <p>This class provides efficient file I/O operations using Java NIO channels, memory-mapped
 * files, and asynchronous I/O capabilities. It includes:
 *
 * <ul>
 *   <li>High-performance bulk file operations using direct ByteBuffers
 *   <li>Memory-mapped file I/O for large files
 *   <li>Asynchronous file operations for non-blocking I/O
 *   <li>File locking mechanisms for concurrent access control
 *   <li>Vectored I/O operations (scatter/gather)
 *   <li>Zero-copy file transfers when possible
 * </ul>
 *
 * <p>All operations are performed with proper error handling and resource management to prevent
 * resource leaks and ensure defensive programming practices.
 *
 * @since 1.0.0
 */
public final class WasiNioIntegration {

  private static final Logger LOGGER = Logger.getLogger(WasiNioIntegration.class.getName());

  /** Default buffer size for I/O operations. */
  private static final int DEFAULT_BUFFER_SIZE = 65536; // 64KB

  /** Maximum buffer size for memory mapping. */
  private static final int MAX_MEMORY_MAP_SIZE = 128 * 1024 * 1024; // 128MB

  /** Executor service for asynchronous operations. */
  private final ExecutorService asyncExecutor;

  /** Whether to use direct buffers for better performance. */
  private final boolean useDirectBuffers;

  /** Whether to use memory mapping for large files. */
  private final boolean useMemoryMapping;

  /**
   * Creates a new NIO integration with default settings.
   */
  public WasiNioIntegration() {
    this(true, true, Executors.newCachedThreadPool(r -> {
      final Thread thread = new Thread(r, "WasiNioIntegration");
      thread.setDaemon(true);
      return thread;
    }));
  }

  /**
   * Creates a new NIO integration with the specified settings.
   *
   * @param useDirectBuffers whether to use direct buffers for better performance
   * @param useMemoryMapping whether to use memory mapping for large files
   * @param asyncExecutor the executor service for asynchronous operations
   */
  public WasiNioIntegration(final boolean useDirectBuffers, final boolean useMemoryMapping,
      final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.useDirectBuffers = useDirectBuffers;
    this.useMemoryMapping = useMemoryMapping;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info(String.format("Created WASI NIO integration: directBuffers=%s, memoryMapping=%s",
        useDirectBuffers, useMemoryMapping));
  }

  /**
   * Performs a high-performance bulk read operation from a file.
   *
   * @param fileChannel the file channel to read from
   * @param position the file position to start reading from
   * @param bufferSize the buffer size to use for reading
   * @return a ByteBuffer containing the read data
   * @throws WasiFileSystemException if the read operation fails
   */
  public ByteBuffer bulkRead(final FileChannel fileChannel, final long position,
      final int bufferSize) {
    JniValidation.requireNonNull(fileChannel, "fileChannel");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requirePositive(bufferSize, "bufferSize");

    LOGGER.fine(String.format("Performing bulk read: position=%d, bufferSize=%d", position,
        bufferSize));

    try {
      final ByteBuffer buffer = allocateBuffer(bufferSize);
      final long originalPosition = fileChannel.position();

      try {
        fileChannel.position(position);
        final int bytesRead = fileChannel.read(buffer);

        buffer.flip();

        LOGGER.fine(String.format("Bulk read completed: %d bytes read", bytesRead));
        return buffer;

      } finally {
        // Restore original position
        fileChannel.position(originalPosition);
      }

    } catch (final IOException e) {
      LOGGER.warning(String.format("Bulk read failed: %s", e.getMessage()));
      throw new WasiFileSystemException("Bulk read failed: " + e.getMessage(), "EIO");
    }
  }

  /**
   * Performs a high-performance bulk write operation to a file.
   *
   * @param fileChannel the file channel to write to
   * @param position the file position to start writing at
   * @param data the data to write
   * @return the number of bytes written
   * @throws WasiFileSystemException if the write operation fails
   */
  public int bulkWrite(final FileChannel fileChannel, final long position, final ByteBuffer data) {
    JniValidation.requireNonNull(fileChannel, "fileChannel");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requireNonNull(data, "data");

    LOGGER.fine(String.format("Performing bulk write: position=%d, dataSize=%d", position,
        data.remaining()));

    try {
      final long originalPosition = fileChannel.position();

      try {
        fileChannel.position(position);
        final int bytesWritten = fileChannel.write(data);

        LOGGER.fine(String.format("Bulk write completed: %d bytes written", bytesWritten));
        return bytesWritten;

      } finally {
        // Restore original position
        fileChannel.position(originalPosition);
      }

    } catch (final IOException e) {
      LOGGER.warning(String.format("Bulk write failed: %s", e.getMessage()));
      throw new WasiFileSystemException("Bulk write failed: " + e.getMessage(), "EIO");
    }
  }

  /**
   * Performs vectored I/O (scatter/gather) read operation.
   *
   * @param fileChannel the file channel to read from
   * @param buffers the array of buffers to read into
   * @param position the file position to start reading from
   * @return the total number of bytes read
   * @throws WasiFileSystemException if the read operation fails
   */
  public long vectoredRead(final FileChannel fileChannel, final ByteBuffer[] buffers,
      final long position) {
    JniValidation.requireNonNull(fileChannel, "fileChannel");
    JniValidation.requireNonNull(buffers, "buffers");
    JniValidation.requireNonNegative(position, "position");

    LOGGER.fine(String.format("Performing vectored read: position=%d, bufferCount=%d", position,
        buffers.length));

    try {
      final long originalPosition = fileChannel.position();

      try {
        fileChannel.position(position);
        final long bytesRead = fileChannel.read(buffers);

        LOGGER.fine(String.format("Vectored read completed: %d bytes read", bytesRead));
        return bytesRead;

      } finally {
        // Restore original position
        fileChannel.position(originalPosition);
      }

    } catch (final IOException e) {
      LOGGER.warning(String.format("Vectored read failed: %s", e.getMessage()));
      throw new WasiFileSystemException("Vectored read failed: " + e.getMessage(), "EIO");
    }
  }

  /**
   * Performs vectored I/O (scatter/gather) write operation.
   *
   * @param fileChannel the file channel to write to
   * @param buffers the array of buffers to write from
   * @param position the file position to start writing at
   * @return the total number of bytes written
   * @throws WasiFileSystemException if the write operation fails
   */
  public long vectoredWrite(final FileChannel fileChannel, final ByteBuffer[] buffers,
      final long position) {
    JniValidation.requireNonNull(fileChannel, "fileChannel");
    JniValidation.requireNonNull(buffers, "buffers");
    JniValidation.requireNonNegative(position, "position");

    LOGGER.fine(String.format("Performing vectored write: position=%d, bufferCount=%d", position,
        buffers.length));

    try {
      final long originalPosition = fileChannel.position();

      try {
        fileChannel.position(position);
        final long bytesWritten = fileChannel.write(buffers);

        LOGGER.fine(String.format("Vectored write completed: %d bytes written", bytesWritten));
        return bytesWritten;

      } finally {
        // Restore original position
        fileChannel.position(originalPosition);
      }

    } catch (final IOException e) {
      LOGGER.warning(String.format("Vectored write failed: %s", e.getMessage()));
      throw new WasiFileSystemException("Vectored write failed: " + e.getMessage(), "EIO");
    }
  }

  /**
   * Performs an asynchronous read operation.
   *
   * @param path the file path to read from
   * @param position the file position to start reading from
   * @param bufferSize the buffer size to use
   * @return a CompletableFuture containing the read data
   */
  public CompletableFuture<ByteBuffer> asyncRead(final Path path, final long position,
      final int bufferSize) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requirePositive(bufferSize, "bufferSize");

    LOGGER.fine(String.format("Starting async read: path=%s, position=%d, bufferSize=%d",
        path, position, bufferSize));

    final CompletableFuture<ByteBuffer> future = new CompletableFuture<>();

    try {
      final AsynchronousFileChannel asyncChannel = AsynchronousFileChannel.open(path,
          Set.of(StandardOpenOption.READ), asyncExecutor);

      final ByteBuffer buffer = allocateBuffer(bufferSize);

      asyncChannel.read(buffer, position, buffer, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(final Integer bytesRead, final ByteBuffer attachment) {
          try {
            asyncChannel.close();
            attachment.flip();
            future.complete(attachment);

            LOGGER.fine(String.format("Async read completed: %d bytes read", bytesRead));
          } catch (final Exception e) {
            failed(e, attachment);
          }
        }

        @Override
        public void failed(final Throwable exc, final ByteBuffer attachment) {
          try {
            asyncChannel.close();
          } catch (final IOException e) {
            LOGGER.warning(String.format("Error closing async channel: %s", e.getMessage()));
          }

          LOGGER.warning(String.format("Async read failed: %s", exc.getMessage()));
          future.completeExceptionally(new WasiFileSystemException(
              "Async read failed: " + exc.getMessage(), "EIO"));
        }
      });

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to start async read: %s", e.getMessage()));
      future.completeExceptionally(new WasiFileSystemException(
          "Failed to start async read: " + e.getMessage(), "EIO"));
    }

    return future;
  }

  /**
   * Performs an asynchronous write operation.
   *
   * @param path the file path to write to
   * @param position the file position to start writing at
   * @param data the data to write
   * @return a CompletableFuture containing the number of bytes written
   */
  public CompletableFuture<Integer> asyncWrite(final Path path, final long position,
      final ByteBuffer data) {
    JniValidation.requireNonNull(path, "path");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requireNonNull(data, "data");

    LOGGER.fine(String.format("Starting async write: path=%s, position=%d, dataSize=%d",
        path, position, data.remaining()));

    final CompletableFuture<Integer> future = new CompletableFuture<>();

    try {
      final AsynchronousFileChannel asyncChannel = AsynchronousFileChannel.open(path,
          Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE), asyncExecutor);

      asyncChannel.write(data, position, data, new CompletionHandler<Integer, ByteBuffer>() {
        @Override
        public void completed(final Integer bytesWritten, final ByteBuffer attachment) {
          try {
            asyncChannel.close();
            future.complete(bytesWritten);

            LOGGER.fine(String.format("Async write completed: %d bytes written", bytesWritten));
          } catch (final Exception e) {
            failed(e, attachment);
          }
        }

        @Override
        public void failed(final Throwable exc, final ByteBuffer attachment) {
          try {
            asyncChannel.close();
          } catch (final IOException e) {
            LOGGER.warning(String.format("Error closing async channel: %s", e.getMessage()));
          }

          LOGGER.warning(String.format("Async write failed: %s", exc.getMessage()));
          future.completeExceptionally(new WasiFileSystemException(
              "Async write failed: " + exc.getMessage(), "EIO"));
        }
      });

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to start async write: %s", e.getMessage()));
      future.completeExceptionally(new WasiFileSystemException(
          "Failed to start async write: " + e.getMessage(), "EIO"));
    }

    return future;
  }

  /**
   * Performs a zero-copy file transfer between two channels.
   *
   * @param sourceChannel the source channel to read from
   * @param sourcePosition the position in the source channel
   * @param count the number of bytes to transfer
   * @param targetChannel the target channel to write to
   * @return the number of bytes transferred
   * @throws WasiFileSystemException if the transfer fails
   */
  public long transferFile(final ReadableByteChannel sourceChannel, final long sourcePosition,
      final long count, final WritableByteChannel targetChannel) {
    JniValidation.requireNonNull(sourceChannel, "sourceChannel");
    JniValidation.requireNonNegative(sourcePosition, "sourcePosition");
    JniValidation.requireNonNegative(count, "count");
    JniValidation.requireNonNull(targetChannel, "targetChannel");

    LOGGER.fine(String.format("Starting file transfer: sourcePos=%d, count=%d",
        sourcePosition, count));

    try {
      if (sourceChannel instanceof FileChannel && targetChannel instanceof FileChannel) {
        // Use efficient transferTo/transferFrom for FileChannel
        final FileChannel sourceFileChannel = (FileChannel) sourceChannel;
        final FileChannel targetFileChannel = (FileChannel) targetChannel;

        final long bytesTransferred = sourceFileChannel.transferTo(sourcePosition, count,
            targetFileChannel);

        LOGGER.fine(String.format("File transfer completed: %d bytes transferred",
            bytesTransferred));
        return bytesTransferred;

      } else {
        // Fallback to manual transfer
        return manualTransfer(sourceChannel, count, targetChannel);
      }

    } catch (final IOException e) {
      LOGGER.warning(String.format("File transfer failed: %s", e.getMessage()));
      throw new WasiFileSystemException("File transfer failed: " + e.getMessage(), "EIO");
    }
  }

  /**
   * Attempts to acquire a file lock on the specified channel.
   *
   * @param fileChannel the file channel to lock
   * @param position the position in the file to start the lock
   * @param size the size of the locked region
   * @param shared whether the lock should be shared (read) or exclusive (write)
   * @return the acquired file lock
   * @throws WasiFileSystemException if the lock cannot be acquired
   */
  public FileLock lockFile(final FileChannel fileChannel, final long position, final long size,
      final boolean shared) {
    JniValidation.requireNonNull(fileChannel, "fileChannel");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requirePositive(size, "size");

    LOGGER.fine(String.format("Acquiring file lock: position=%d, size=%d, shared=%s",
        position, size, shared));

    try {
      final FileLock lock = fileChannel.lock(position, size, shared);

      LOGGER.fine(String.format("File lock acquired: position=%d, size=%d, shared=%s",
          lock.position(), lock.size(), lock.isShared()));

      return lock;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to acquire file lock: %s", e.getMessage()));
      throw new WasiFileSystemException("Failed to acquire file lock: " + e.getMessage(),
          "EAGAIN");
    }
  }

  /**
   * Attempts to acquire a non-blocking file lock on the specified channel.
   *
   * @param fileChannel the file channel to lock
   * @param position the position in the file to start the lock
   * @param size the size of the locked region
   * @param shared whether the lock should be shared (read) or exclusive (write)
   * @return the acquired file lock, or null if the lock cannot be acquired immediately
   * @throws WasiFileSystemException if an error occurs while attempting to lock
   */
  public FileLock tryLockFile(final FileChannel fileChannel, final long position, final long size,
      final boolean shared) {
    JniValidation.requireNonNull(fileChannel, "fileChannel");
    JniValidation.requireNonNegative(position, "position");
    JniValidation.requirePositive(size, "size");

    LOGGER.fine(String.format("Attempting non-blocking file lock: position=%d, size=%d, shared=%s",
        position, size, shared));

    try {
      final FileLock lock = fileChannel.tryLock(position, size, shared);

      if (lock != null) {
        LOGGER.fine(String.format("Non-blocking file lock acquired: position=%d, size=%d, shared=%s",
            lock.position(), lock.size(), lock.isShared()));
      } else {
        LOGGER.fine("Non-blocking file lock not available");
      }

      return lock;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Error attempting file lock: %s", e.getMessage()));
      throw new WasiFileSystemException("Error attempting file lock: " + e.getMessage(), "EIO");
    }
  }

  /**
   * Shuts down the NIO integration and releases resources.
   */
  public void shutdown() {
    LOGGER.info("Shutting down WASI NIO integration");

    asyncExecutor.shutdown();
    try {
      if (!asyncExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
        asyncExecutor.shutdownNow();
        LOGGER.warning("Forced shutdown of async executor");
      }
    } catch (final InterruptedException e) {
      asyncExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("WASI NIO integration shutdown completed");
  }

  /** Allocates an appropriate buffer based on configuration. */
  private ByteBuffer allocateBuffer(final int size) {
    if (useDirectBuffers) {
      return ByteBuffer.allocateDirect(size);
    } else {
      return ByteBuffer.allocate(size);
    }
  }

  /** Performs manual transfer between channels when efficient transfer is not available. */
  private long manualTransfer(final ReadableByteChannel sourceChannel, final long count,
      final WritableByteChannel targetChannel) throws IOException {
    final ByteBuffer buffer = allocateBuffer(
        Math.min(DEFAULT_BUFFER_SIZE, (int) Math.min(count, Integer.MAX_VALUE)));
    
    long totalTransferred = 0;
    long remaining = count;

    while (remaining > 0 && sourceChannel.read(buffer) > 0) {
      buffer.flip();

      while (buffer.hasRemaining()) {
        final int written = targetChannel.write(buffer);
        totalTransferred += written;
        remaining -= written;

        if (remaining <= 0) {
          break;
        }
      }

      buffer.clear();
    }

    return totalTransferred;
  }
}