package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Handle for an open directory in the WASI filesystem.
 *
 * <p>A WasiDirectoryHandle represents an open directory descriptor within the WASI sandbox. It
 * provides access to directory operations while enforcing the capability-based security model.
 * Directory handles should be properly closed when no longer needed to free system resources.
 *
 * <p>Directory handles are automatically managed by the WASI runtime and should not be created
 * directly by user code. They are obtained through {@link WasiFilesystem#openDirectory} operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiDirectoryHandle handle = filesystem.openDirectory("/data", WasiRights.PATH_OPEN);
 * try {
 *     List<WasiDirEntry> entries = filesystem.readDirectory(handle);
 *     for (WasiDirEntry entry : entries) {
 *         System.out.println(entry.getName() + " - " + entry.getType());
 *     }
 * } finally {
 *     handle.close();
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiDirectoryHandle {

  /**
   * Gets the unique identifier for this directory handle.
   *
   * @return the file descriptor number
   */
  int getFileDescriptor();

  /**
   * Gets the path that was used to open this directory.
   *
   * @return the original directory path
   */
  String getPath();

  /**
   * Gets the rights associated with this directory handle.
   *
   * @return the directory rights
   */
  WasiRights getRights();

  /**
   * Checks if this directory handle is still valid and usable.
   *
   * @return true if the handle is valid, false if it has been closed
   */
  boolean isValid();

  /**
   * Gets the current position in the directory for readdir operations.
   *
   * @return the current directory position
   * @throws WasmException if the position cannot be determined
   */
  long getPosition() throws WasmException;

  /**
   * Sets the current position in the directory for readdir operations.
   *
   * @param position the new directory position
   * @throws WasmException if the position cannot be set
   * @throws IllegalArgumentException if position is negative
   */
  void setPosition(final long position) throws WasmException;

  /**
   * Rewinds the directory to the beginning for readdir operations.
   *
   * @throws WasmException if the directory cannot be rewound
   */
  void rewind() throws WasmException;

  /**
   * Closes this directory handle and releases its resources.
   *
   * <p>After calling this method, the handle becomes invalid and should not be used. Any attempt to
   * use the handle after closing may result in exceptions.
   *
   * @throws WasmException if the directory cannot be closed
   */
  void close() throws WasmException;
}
