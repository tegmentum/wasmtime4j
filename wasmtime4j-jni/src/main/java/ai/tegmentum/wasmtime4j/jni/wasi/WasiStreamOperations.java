package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI Preview 2 stream operations.
 *
 * <p>This class implements the WASI Preview 2 stream operations as defined in the WIT interface
 * `wasi:io/streams`. It provides async I/O capabilities for input and output streams with proper
 * resource management and error handling.
 *
 * <p>Supported stream operations:
 *
 * <ul>
 *   <li>Input stream creation and reading with async support
 *   <li>Output stream creation and writing with async support
 *   <li>Stream blocking and non-blocking modes
 *   <li>Stream resource lifecycle management
 *   <li>Error handling and stream state management
 * </ul>
 *
 * @since 1.0.0
 */
public final class WasiStreamOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiStreamOperations.class.getName());

  /** Maximum buffer size for stream operations. */
  private static final int MAX_BUFFER_SIZE = 64 * 1024; // 64KB

  /** The WASI context this stream operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Executor service for async operations. */
  private final ExecutorService asyncExecutor;

  /** Stream handle generator. */
  private final AtomicLong streamHandleGenerator = new AtomicLong(1);

  /** Active streams tracking. */
  private final Map<Long, StreamInfo> activeStreams = new ConcurrentHashMap<>();

  /**
   * Creates a new WASI stream operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param asyncExecutor the executor service for async operations
   * @throws JniException if parameters are null
   */
  public WasiStreamOperations(final WasiContext wasiContext, final ExecutorService asyncExecutor) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(asyncExecutor, "asyncExecutor");

    this.wasiContext = wasiContext;
    this.asyncExecutor = asyncExecutor;

    LOGGER.info("Created WASI stream operations handler");
  }

  /**
   * Opens an input stream for the specified resource.
   *
   * <p>WIT interface: wasi:io/streams.input-stream
   *
   * @param resourceHandle the resource handle to create an input stream for
   * @return the input stream handle
   * @throws WasiException if stream creation fails
   */
  public long openInputStream(final long resourceHandle) throws WasiException {
    LOGGER.fine(() -> String.format("Opening input stream for resource: %d", resourceHandle));

    try {
      final long streamHandle = streamHandleGenerator.getAndIncrement();

      final int result =
          nativeOpenInputStream(wasiContext.getNativeHandle(), streamHandle, resourceHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to open input stream: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final StreamInfo streamInfo = new StreamInfo(streamHandle, StreamType.INPUT, resourceHandle);
      activeStreams.put(streamHandle, streamInfo);

      LOGGER.fine(
          () ->
              String.format(
                  "Opened input stream: handle=%d, resource=%d", streamHandle, resourceHandle));
      return streamHandle;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to open input stream for resource: " + resourceHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to open input stream for resource: " + resourceHandle, e);
      throw new WasiException("Input stream creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Opens an output stream for the specified resource.
   *
   * <p>WIT interface: wasi:io/streams.output-stream
   *
   * @param resourceHandle the resource handle to create an output stream for
   * @return the output stream handle
   * @throws WasiException if stream creation fails
   */
  public long openOutputStream(final long resourceHandle) throws WasiException {
    LOGGER.fine(() -> String.format("Opening output stream for resource: %d", resourceHandle));

    try {
      final long streamHandle = streamHandleGenerator.getAndIncrement();

      final int result =
          nativeOpenOutputStream(wasiContext.getNativeHandle(), streamHandle, resourceHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Failed to open output stream: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      final StreamInfo streamInfo = new StreamInfo(streamHandle, StreamType.OUTPUT, resourceHandle);
      activeStreams.put(streamHandle, streamInfo);

      LOGGER.fine(
          () ->
              String.format(
                  "Opened output stream: handle=%d, resource=%d", streamHandle, resourceHandle));
      return streamHandle;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to open output stream for resource: " + resourceHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to open output stream for resource: " + resourceHandle, e);
      throw new WasiException(
          "Output stream creation failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Reads data from an input stream.
   *
   * <p>WIT interface: wasi:io/streams.input-stream.read
   *
   * @param streamHandle the input stream handle
   * @param buffer the buffer to read data into
   * @return the number of bytes read
   * @throws WasiException if the read operation fails
   */
  public int read(final long streamHandle, final ByteBuffer buffer) throws WasiException {
    JniValidation.requireNonNull(buffer, "buffer");
    validateInputStream(streamHandle);

    final int requestedBytes = Math.min(buffer.remaining(), MAX_BUFFER_SIZE);
    if (requestedBytes == 0) {
      return 0;
    }

    LOGGER.fine(
        () ->
            String.format(
                "Reading from stream: handle=%d, bytes=%d", streamHandle, requestedBytes));

    try {
      final byte[] tempBuffer = new byte[requestedBytes];
      final int bytesRead =
          nativeReadStream(wasiContext.getNativeHandle(), streamHandle, tempBuffer, requestedBytes);

      if (bytesRead < 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(-bytesRead);
        throw new WasiException(
            "Stream read failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      if (bytesRead > 0) {
        buffer.put(tempBuffer, 0, bytesRead);
      }

      LOGGER.fine(
          () ->
              String.format("Read from stream: handle=%d, bytesRead=%d", streamHandle, bytesRead));
      return bytesRead;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to read from stream: " + streamHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to read from stream: " + streamHandle, e);
      throw new WasiException("Stream read failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Writes data to an output stream.
   *
   * <p>WIT interface: wasi:io/streams.output-stream.write
   *
   * @param streamHandle the output stream handle
   * @param buffer the buffer containing data to write
   * @return the number of bytes written
   * @throws WasiException if the write operation fails
   */
  public int write(final long streamHandle, final ByteBuffer buffer) throws WasiException {
    JniValidation.requireNonNull(buffer, "buffer");
    validateOutputStream(streamHandle);

    final int dataSize = Math.min(buffer.remaining(), MAX_BUFFER_SIZE);
    if (dataSize == 0) {
      return 0;
    }

    LOGGER.fine(
        () -> String.format("Writing to stream: handle=%d, bytes=%d", streamHandle, dataSize));

    try {
      final byte[] tempBuffer = new byte[dataSize];
      buffer.get(tempBuffer);

      final int bytesWritten =
          nativeWriteStream(wasiContext.getNativeHandle(), streamHandle, tempBuffer, dataSize);

      if (bytesWritten < 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(-bytesWritten);
        throw new WasiException(
            "Stream write failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      // Adjust buffer position if partial write
      if (bytesWritten < dataSize) {
        buffer.position(buffer.position() - (dataSize - bytesWritten));
      }

      LOGGER.fine(
          () ->
              String.format(
                  "Wrote to stream: handle=%d, bytesWritten=%d", streamHandle, bytesWritten));
      return bytesWritten;

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to write to stream: " + streamHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to write to stream: " + streamHandle, e);
      throw new WasiException("Stream write failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Flushes an output stream to ensure all buffered data is written.
   *
   * <p>WIT interface: wasi:io/streams.output-stream.flush
   *
   * @param streamHandle the output stream handle
   * @throws WasiException if the flush operation fails
   */
  public void flush(final long streamHandle) throws WasiException {
    validateOutputStream(streamHandle);

    LOGGER.fine(() -> String.format("Flushing stream: handle=%d", streamHandle));

    try {
      final int result = nativeFlushStream(wasiContext.getNativeHandle(), streamHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        throw new WasiException(
            "Stream flush failed: "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"),
            errorCode != null ? errorCode : WasiErrorCode.EIO);
      }

      LOGGER.fine(() -> String.format("Flushed stream: handle=%d", streamHandle));

    } catch (final WasiException e) {
      LOGGER.log(Level.WARNING, "Failed to flush stream: " + streamHandle, e);
      throw e;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to flush stream: " + streamHandle, e);
      throw new WasiException("Stream flush failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Closes a stream and releases its resources.
   *
   * @param streamHandle the stream handle to close
   * @throws WasiException if the close operation fails
   */
  public void closeStream(final long streamHandle) {
    final StreamInfo streamInfo = activeStreams.get(streamHandle);
    if (streamInfo == null) {
      LOGGER.fine(() -> String.format("Stream already closed or invalid: handle=%d", streamHandle));
      return;
    }

    LOGGER.fine(() -> String.format("Closing stream: handle=%d", streamHandle));

    try {
      final int result = nativeCloseStream(wasiContext.getNativeHandle(), streamHandle);

      if (result != 0) {
        final WasiErrorCode errorCode = WasiErrorCode.fromErrnoOrNull(result);
        LOGGER.warning(
            "Failed to close stream "
                + streamHandle
                + ": "
                + (errorCode != null ? errorCode.getDescription() : "Unknown error"));
      }

      activeStreams.remove(streamHandle);
      LOGGER.fine(() -> String.format("Closed stream: handle=%d", streamHandle));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing stream: " + streamHandle, e);
      activeStreams.remove(streamHandle); // Remove anyway to prevent leaks
    }
  }

  /**
   * Gets information about a stream.
   *
   * @param streamHandle the stream handle
   * @return the stream information, or null if stream doesn't exist
   */
  public StreamInfo getStreamInfo(final long streamHandle) {
    return activeStreams.get(streamHandle);
  }

  /** Closes all active streams and releases resources. */
  public void close() {
    LOGGER.info("Closing all stream operations");

    for (final Long streamHandle : activeStreams.keySet()) {
      try {
        closeStream(streamHandle);
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Error closing stream during shutdown: " + streamHandle, e);
      }
    }

    activeStreams.clear();
    LOGGER.info("Stream operations closed successfully");
  }

  /** Validates that a stream is an input stream. */
  private void validateInputStream(final long streamHandle) throws WasiException {
    final StreamInfo streamInfo = activeStreams.get(streamHandle);
    if (streamInfo == null) {
      throw new WasiException("Invalid stream handle: " + streamHandle, WasiErrorCode.EBADF);
    }
    if (streamInfo.type != StreamType.INPUT) {
      throw new WasiException(
          "Stream is not an input stream: " + streamHandle, WasiErrorCode.EBADF);
    }
  }

  /** Validates that a stream is an output stream. */
  private void validateOutputStream(final long streamHandle) throws WasiException {
    final StreamInfo streamInfo = activeStreams.get(streamHandle);
    if (streamInfo == null) {
      throw new WasiException("Invalid stream handle: " + streamHandle, WasiErrorCode.EBADF);
    }
    if (streamInfo.type != StreamType.OUTPUT) {
      throw new WasiException(
          "Stream is not an output stream: " + streamHandle, WasiErrorCode.EBADF);
    }
  }

  /**
   * Native method to open an input stream.
   *
   * @param contextHandle the WASI context handle
   * @param streamHandle the stream handle
   * @param resourceHandle the resource handle
   * @return 0 for success, error code for failure
   */
  private static native int nativeOpenInputStream(
      long contextHandle, long streamHandle, long resourceHandle);

  /**
   * Native method to open an output stream.
   *
   * @param contextHandle the WASI context handle
   * @param streamHandle the stream handle
   * @param resourceHandle the resource handle
   * @return 0 for success, error code for failure
   */
  private static native int nativeOpenOutputStream(
      long contextHandle, long streamHandle, long resourceHandle);

  /**
   * Native method to read from a stream.
   *
   * @param contextHandle the WASI context handle
   * @param streamHandle the stream handle
   * @param buffer the buffer to read into
   * @param bufferSize the buffer size
   * @return number of bytes read, or negative error code
   */
  private static native int nativeReadStream(
      long contextHandle, long streamHandle, byte[] buffer, int bufferSize);

  /**
   * Native method to write to a stream.
   *
   * @param contextHandle the WASI context handle
   * @param streamHandle the stream handle
   * @param buffer the buffer to write from
   * @param bufferSize the buffer size
   * @return number of bytes written, or negative error code
   */
  private static native int nativeWriteStream(
      long contextHandle, long streamHandle, byte[] buffer, int bufferSize);

  /**
   * Native method to flush a stream.
   *
   * @param contextHandle the WASI context handle
   * @param streamHandle the stream handle
   * @return 0 for success, error code for failure
   */
  private static native int nativeFlushStream(long contextHandle, long streamHandle);

  /**
   * Native method to close a stream.
   *
   * @param contextHandle the WASI context handle
   * @param streamHandle the stream handle
   * @return 0 for success, error code for failure
   */
  private static native int nativeCloseStream(long contextHandle, long streamHandle);

  /** Stream type enumeration. */
  public enum StreamType {
    INPUT,
    OUTPUT
  }

  /** Stream information class. */
  public static final class StreamInfo {
    public final long handle;
    public final StreamType type;
    public final long resourceHandle;
    public final long createdAt;

    /**
     * Creates stream info.
     *
     * @param handle the stream handle
     * @param type the stream type
     * @param resourceHandle the resource handle
     */
    public StreamInfo(final long handle, final StreamType type, final long resourceHandle) {
      this.handle = handle;
      this.type = type;
      this.resourceHandle = resourceHandle;
      this.createdAt = System.currentTimeMillis();
    }

    @Override
    public String toString() {
      return String.format(
          "StreamInfo{handle=%d, type=%s, resource=%d, created=%d}",
          handle, type, resourceHandle, createdAt);
    }
  }
}
