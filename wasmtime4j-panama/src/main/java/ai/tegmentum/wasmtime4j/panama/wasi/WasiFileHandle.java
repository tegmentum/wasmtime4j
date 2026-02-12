package ai.tegmentum.wasmtime4j.panama.wasi;

import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * File handle wrapper for WASI file system operations with resource management in Panama FFI
 * context.
 *
 * <p>This class encapsulates an open file handle in the WASI context, providing proper resource
 * management and metadata tracking. It ensures that native file resources are properly cleaned up
 * when the handle is closed or garbage collected.
 *
 * @since 1.0.0
 */
public final class WasiFileHandle implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(WasiFileHandle.class.getName());

  /** The file descriptor number. */
  private final int fileDescriptor;

  /** The absolute path to the file. */
  private final Path path;

  /** The underlying byte channel for I/O operations. */
  private final SeekableByteChannel channel;

  /** The file channel if available (for advanced operations like seeking). */
  private final FileChannel fileChannel;

  /** The file operation type this handle supports. */
  private final WasiFileOperation operation;

  /** Whether this handle has been closed. */
  private volatile boolean closed = false;

  /**
   * Creates a new WASI file handle.
   *
   * @param fileDescriptor the file descriptor number
   * @param path the absolute path to the file
   * @param channel the byte channel for I/O operations
   * @param fileChannel the file channel for advanced operations (may be null)
   * @param operation the file operation type
   */
  public WasiFileHandle(
      final int fileDescriptor,
      final Path path,
      final SeekableByteChannel channel,
      final FileChannel fileChannel,
      final WasiFileOperation operation) {
    PanamaValidation.requireNonNull(path, "path");
    PanamaValidation.requireNonNull(channel, "channel");
    PanamaValidation.requireNonNull(operation, "operation");

    this.fileDescriptor = fileDescriptor;
    this.path = path.toAbsolutePath();
    this.channel = channel;
    this.fileChannel = fileChannel;
    this.operation = operation;

    LOGGER.fine(
        String.format(
            "Created file handle: fd=%d, path=%s, operation=%s", fileDescriptor, path, operation));
  }

  /**
   * Gets the file descriptor number.
   *
   * @return the file descriptor
   */
  public int getFileDescriptor() {
    ensureNotClosed();
    return fileDescriptor;
  }

  /**
   * Gets the absolute path to the file.
   *
   * @return the file path
   */
  public Path getPath() {
    ensureNotClosed();
    return path;
  }

  /**
   * Gets the byte channel for I/O operations.
   *
   * @return the byte channel
   */
  public SeekableByteChannel getChannel() {
    ensureNotClosed();
    return channel;
  }

  /**
   * Gets the file channel for advanced operations.
   *
   * @return the file channel, or null if not available
   */
  public FileChannel getFileChannel() {
    ensureNotClosed();
    return fileChannel;
  }

  /**
   * Gets the file operation type this handle supports.
   *
   * @return the file operation
   */
  public WasiFileOperation getOperation() {
    ensureNotClosed();
    return operation;
  }

  /**
   * Checks if this file handle has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /**
   * Closes this file handle and releases its resources.
   *
   * <p>This method is idempotent - calling it multiple times has no effect after the first call.
   */
  @Override
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      LOGGER.fine(String.format("Closing file handle: fd=%d, path=%s", fileDescriptor, path));

      try {
        channel.close();
        LOGGER.fine(String.format("File handle closed successfully: fd=%d", fileDescriptor));
      } catch (final IOException e) {
        LOGGER.warning(
            String.format(
                "Error closing file handle: fd=%d, error=%s", fileDescriptor, e.getMessage()));
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Ensures this handle is not closed, throwing an exception if it is.
   *
   * @throws IllegalStateException if the handle is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException(
          String.format("File handle is closed: fd=%d, path=%s", fileDescriptor, path));
    }
  }

  // Note: finalize() method removed to avoid deprecation warnings.
  // Resources should be properly closed using try-with-resources or explicit close() calls.

  @Override
  public String toString() {
    return String.format(
        "WasiFileHandle{fd=%d, path=%s, operation=%s, closed=%s}",
        fileDescriptor, path, operation, closed);
  }
}
