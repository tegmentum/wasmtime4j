package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * Handle for an open file in the WASI filesystem.
 *
 * <p>A WasiFileHandle represents an open file descriptor within the WASI sandbox. It provides
 * access to file operations while enforcing the capability-based security model. File handles
 * should be properly closed when no longer needed to free system resources.
 *
 * <p>File handles are automatically managed by the WASI runtime and should not be created directly
 * by user code. They are obtained through {@link WasiFilesystem#openFile} operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiFileHandle handle = filesystem.openFile("/data/file.txt", WasiOpenFlags.READ, WasiRights.FD_READ);
 * try {
 *     // Use the file handle for operations
 *     ByteBuffer buffer = ByteBuffer.allocate(1024);
 *     long bytesRead = filesystem.readFile(handle, buffer, 0);
 * } finally {
 *     filesystem.closeFile(handle);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiFileHandle extends Closeable {

  /**
   * Gets the unique identifier for this file handle.
   *
   * @return the file descriptor number
   */
  int getFileDescriptor();

  /**
   * Gets the path that was used to open this file.
   *
   * @return the original file path
   */
  String getPath();

  /**
   * Gets the rights associated with this file handle.
   *
   * @return the file rights
   */
  WasiRights getRights();

  /**
   * Gets the flags used to open this file.
   *
   * @return the open flags
   */
  WasiOpenFlags getOpenFlags();

  /**
   * Checks if this file handle is still valid and usable.
   *
   * @return true if the handle is valid, false if it has been closed
   */
  boolean isValid();

  /**
   * Gets the current file position for read/write operations.
   *
   * @return the current file position in bytes
   * @throws WasmException if the position cannot be determined
   */
  long getPosition() throws WasmException;

  /**
   * Sets the current file position for read/write operations.
   *
   * @param position the new file position in bytes
   * @throws WasmException if the position cannot be set
   * @throws IllegalArgumentException if position is negative
   */
  void setPosition(final long position) throws WasmException;

  /**
   * Gets the size of the file in bytes.
   *
   * @return the file size in bytes
   * @throws WasmException if the file size cannot be determined
   */
  long getSize() throws WasmException;

  /**
   * Truncates or extends the file to the specified size.
   *
   * @param size the new file size in bytes
   * @throws WasmException if the file cannot be resized
   * @throws IllegalArgumentException if size is negative
   */
  void setSize(final long size) throws WasmException;

  /**
   * Closes this file handle and releases its resources.
   *
   * <p>After calling this method, the handle becomes invalid and should not be used. Any attempt to
   * use the handle after closing may result in exceptions.
   */
  @Override
  void close();
}
