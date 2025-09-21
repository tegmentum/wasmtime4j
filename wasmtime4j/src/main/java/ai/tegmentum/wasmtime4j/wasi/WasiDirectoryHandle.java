package ai.tegmentum.wasmtime4j.wasi;

import java.io.Closeable;

/**
 * Handle representing an open directory in the WASI filesystem.
 *
 * <p>A WasiDirectoryHandle represents an open directory descriptor that can be used for directory
 * operations such as reading directory contents. Handles are obtained by calling
 * {@link WasiFilesystem#openDirectory} and should be closed when no longer needed.
 *
 * <p>Directory handles are not thread-safe and should not be shared between threads without
 * external synchronization.
 *
 * @since 1.0.0
 */
public interface WasiDirectoryHandle extends Closeable {

  /**
   * Gets the unique identifier for this directory handle.
   *
   * <p>The file descriptor is a unique integer that identifies this open directory within the
   * WASI context. It can be used for debugging and logging purposes.
   *
   * @return the file descriptor number
   */
  int getFileDescriptor();

  /**
   * Gets the rights (capabilities) available on this directory handle.
   *
   * <p>The rights determine what operations can be performed on this directory handle and its
   * contents. These are set when the directory is opened and cannot be changed.
   *
   * @return the rights bitmask for this handle
   */
  WasiRights getRights();

  /**
   * Gets the path of the directory that this handle represents.
   *
   * <p>This returns the path that was used to open the directory, which may be relative to a
   * pre-opened directory.
   *
   * @return the directory path
   */
  String getPath();

  /**
   * Checks if this directory handle is still valid and open.
   *
   * <p>A directory handle becomes invalid when it is closed either explicitly via {@link #close()}
   * or implicitly when the WASI context is destroyed.
   *
   * @return true if the handle is valid and can be used for operations, false otherwise
   */
  boolean isValid();

  /**
   * Checks if this directory handle supports reading directory contents.
   *
   * <p>Read capability depends on the rights that were specified when the directory was opened.
   *
   * @return true if reading directory contents is supported, false otherwise
   */
  boolean canRead();

  /**
   * Resets the directory reading position to the beginning.
   *
   * <p>This method resets the internal position so that the next call to
   * {@link WasiFilesystem#readDirectory} will start from the beginning of the directory.
   *
   * @throws IllegalStateException if the handle is closed
   */
  void rewind();

  /**
   * Closes the directory handle and releases associated resources.
   *
   * <p>After calling this method, the handle becomes invalid and should not be used for further
   * operations. This method is idempotent - calling it multiple times has no additional effect.
   */
  @Override
  void close();
}