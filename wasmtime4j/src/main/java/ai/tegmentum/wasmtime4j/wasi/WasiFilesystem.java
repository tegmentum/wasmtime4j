package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * WASI Preview 1 filesystem operations interface.
 *
 * <p>Provides complete filesystem access capabilities within the WASI sandbox, including file and
 * directory operations, metadata management, and path manipulation. All operations are subject to
 * the capability-based security model configured in the WASI context.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (WasiContext context = WasiFactory.createContext()) {
 *     WasiFilesystem fs = context.getFilesystem();
 *
 *     // Open a file for reading
 *     WasiFileHandle file = fs.openFile("/data/input.txt", WasiOpenFlags.READ, WasiRights.FD_READ);
 *
 *     // Read file contents
 *     ByteBuffer buffer = ByteBuffer.allocate(1024);
 *     long bytesRead = fs.readFile(file, buffer, 0);
 *
 *     // Close the file
 *     fs.closeFile(file);
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiFilesystem {

  /**
   * Opens a file with the specified flags and rights.
   *
   * @param path the path to the file to open
   * @param flags the open flags specifying how to open the file
   * @param rights the rights requested for the file descriptor
   * @return a file handle for the opened file
   * @throws WasmException if the file cannot be opened
   * @throws IllegalArgumentException if any parameter is null
   */
  WasiFileHandle openFile(final String path, final WasiOpenFlags flags, final WasiRights rights)
      throws WasmException;

  /**
   * Closes an open file handle and releases its resources.
   *
   * @param handle the file handle to close
   * @throws WasmException if the file cannot be closed
   * @throws IllegalArgumentException if handle is null
   */
  void closeFile(final WasiFileHandle handle) throws WasmException;

  /**
   * Reads data from a file into the provided buffer.
   *
   * @param handle the file handle to read from
   * @param buffer the buffer to read data into
   * @param offset the offset in the file to start reading from
   * @return the number of bytes actually read
   * @throws WasmException if the read operation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  long readFile(final WasiFileHandle handle, final ByteBuffer buffer, final long offset)
      throws WasmException;

  /**
   * Writes data from the provided buffer to a file.
   *
   * @param handle the file handle to write to
   * @param buffer the buffer containing data to write
   * @param offset the offset in the file to start writing at
   * @return the number of bytes actually written
   * @throws WasmException if the write operation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  long writeFile(final WasiFileHandle handle, final ByteBuffer buffer, final long offset)
      throws WasmException;

  /**
   * Opens a directory with the specified rights.
   *
   * @param path the path to the directory to open
   * @param rights the rights requested for the directory handle
   * @return a directory handle for the opened directory
   * @throws WasmException if the directory cannot be opened
   * @throws IllegalArgumentException if any parameter is null
   */
  WasiDirectoryHandle openDirectory(final String path, final WasiRights rights)
      throws WasmException;

  /**
   * Reads the contents of a directory.
   *
   * @param handle the directory handle to read from
   * @return a list of directory entries
   * @throws WasmException if the directory cannot be read
   * @throws IllegalArgumentException if handle is null
   */
  List<WasiDirEntry> readDirectory(final WasiDirectoryHandle handle) throws WasmException;

  /**
   * Creates a new directory with the specified permissions.
   *
   * @param path the path where the directory should be created
   * @param permissions the permissions to set on the new directory
   * @throws WasmException if the directory cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  void createDirectory(final String path, final WasiPermissions permissions) throws WasmException;

  /**
   * Removes an empty directory.
   *
   * @param path the path of the directory to remove
   * @throws WasmException if the directory cannot be removed
   * @throws IllegalArgumentException if path is null
   */
  void removeDirectory(final String path) throws WasmException;

  /**
   * Gets metadata information for a file or directory.
   *
   * @param path the path to get metadata for
   * @return file statistics and metadata
   * @throws WasmException if metadata cannot be retrieved
   * @throws IllegalArgumentException if path is null
   */
  WasiFileStats getFileStats(final String path) throws WasmException;

  /**
   * Sets metadata information for a file or directory.
   *
   * @param path the path to set metadata for
   * @param stats the new file statistics and metadata
   * @throws WasmException if metadata cannot be set
   * @throws IllegalArgumentException if any parameter is null
   */
  void setFileStats(final String path, final WasiFileStats stats) throws WasmException;

  /**
   * Sets permissions on a file or directory.
   *
   * @param path the path to set permissions for
   * @param permissions the new permissions to set
   * @throws WasmException if permissions cannot be set
   * @throws IllegalArgumentException if any parameter is null
   */
  void setFilePermissions(final String path, final WasiPermissions permissions)
      throws WasmException;

  /**
   * Canonicalizes a path by resolving symbolic links and relative path components.
   *
   * @param path the path to canonicalize
   * @return the canonical absolute path
   * @throws WasmException if the path cannot be canonicalized
   * @throws IllegalArgumentException if path is null
   */
  String canonicalizePath(final String path) throws WasmException;

  /**
   * Creates a symbolic link.
   *
   * @param oldPath the target path that the symlink should point to
   * @param newPath the path where the symlink should be created
   * @throws WasmException if the symlink cannot be created
   * @throws IllegalArgumentException if any parameter is null
   */
  void symlinkCreate(final String oldPath, final String newPath) throws WasmException;

  /**
   * Reads the target of a symbolic link.
   *
   * @param path the path of the symlink to read
   * @return the target path that the symlink points to
   * @throws WasmException if the symlink cannot be read
   * @throws IllegalArgumentException if path is null
   */
  String readSymlink(final String path) throws WasmException;

  /**
   * Renames or moves a file or directory.
   *
   * @param oldPath the current path of the file or directory
   * @param newPath the new path for the file or directory
   * @throws WasmException if the rename operation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  void rename(final String oldPath, final String newPath) throws WasmException;

  /**
   * Removes a file or symbolic link.
   *
   * @param path the path of the file to remove
   * @throws WasmException if the file cannot be removed
   * @throws IllegalArgumentException if path is null
   */
  void unlink(final String path) throws WasmException;

  /**
   * Synchronizes file data to storage.
   *
   * @param handle the file handle to synchronize
   * @throws WasmException if the sync operation fails
   * @throws IllegalArgumentException if handle is null
   */
  void syncFile(final WasiFileHandle handle) throws WasmException;

  /**
   * Gets the current working directory.
   *
   * @return the current working directory path
   * @throws WasmException if the working directory cannot be determined
   */
  String getCurrentWorkingDirectory() throws WasmException;

  /**
   * Changes the current working directory.
   *
   * @param path the new working directory path
   * @throws WasmException if the working directory cannot be changed
   * @throws IllegalArgumentException if path is null
   */
  void setCurrentWorkingDirectory(final String path) throws WasmException;
}