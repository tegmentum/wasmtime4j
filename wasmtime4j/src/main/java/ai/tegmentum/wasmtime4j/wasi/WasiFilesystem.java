package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * WASI Preview 1 filesystem interface providing comprehensive file and directory operations.
 *
 * <p>This interface provides access to filesystem operations within the WASI sandbox, including
 * file I/O, directory management, and metadata operations. All operations respect capability-based
 * security and only allow access to pre-opened directories and their contents.
 *
 * <p>File operations include:
 *
 * <ul>
 *   <li>Opening, reading, writing, and closing files
 *   <li>Creating and removing files and directories
 *   <li>File metadata access (size, permissions, timestamps)
 *   <li>Path resolution and canonicalization
 *   <li>Symbolic link operations
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiContext context = WasiFactory.createContext();
 * WasiFilesystem fs = context.getFilesystem();
 *
 * // Open a file for writing
 * WasiFileHandle handle = fs.openFile("/tmp/output.txt",
 *     WasiOpenFlags.CREATE | WasiOpenFlags.WRITE,
 *     WasiRights.FD_WRITE | WasiRights.FD_SEEK);
 *
 * // Write data
 * ByteBuffer data = ByteBuffer.wrap("Hello, WASI!".getBytes());
 * long bytesWritten = fs.writeFile(handle, data, 0);
 *
 * // Close the file
 * fs.closeFile(handle);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiFilesystem {

  /**
   * Opens a file at the specified path with the given flags and rights.
   *
   * <p>This method opens a file for subsequent I/O operations. The flags parameter controls how the
   * file is opened (read, write, create, truncate, etc.), while the rights parameter specifies what
   * operations are allowed on the resulting file descriptor.
   *
   * @param path the path to the file relative to a pre-opened directory
   * @param flags flags controlling how the file is opened
   * @param rights rights that will be available on the returned file handle
   * @return a file handle for subsequent operations
   * @throws WasmException if the file cannot be opened or permission is denied
   * @throws IllegalArgumentException if path is null or invalid
   */
  WasiFileHandle openFile(final String path, final WasiOpenFlags flags, final WasiRights rights)
      throws WasmException;

  /**
   * Closes a file handle and releases associated resources.
   *
   * <p>After calling this method, the file handle becomes invalid and should not be used for
   * further operations. Any attempt to use a closed handle will result in an exception.
   *
   * @param handle the file handle to close
   * @throws WasmException if closing the file fails
   * @throws IllegalArgumentException if handle is null
   * @throws IllegalStateException if the handle is already closed
   */
  void closeFile(final WasiFileHandle handle) throws WasmException;

  /**
   * Reads data from a file into the provided buffer.
   *
   * <p>This method reads data from the file at the current position (or specified offset) into the
   * provided ByteBuffer. The buffer's position and limit determine how much data is read.
   *
   * @param handle the file handle to read from
   * @param buffer the buffer to read data into
   * @param offset the file offset to read from, or -1 to read from current position
   * @return the number of bytes actually read, or 0 if end of file
   * @throws WasmException if reading fails or permission is denied
   * @throws IllegalArgumentException if handle or buffer is null
   * @throws IllegalStateException if the handle is closed or not readable
   */
  long readFile(final WasiFileHandle handle, final ByteBuffer buffer, final long offset)
      throws WasmException;

  /**
   * Writes data from the buffer to a file.
   *
   * <p>This method writes data from the provided ByteBuffer to the file at the current position (or
   * specified offset). The buffer's position and limit determine how much data is written.
   *
   * @param handle the file handle to write to
   * @param buffer the buffer containing data to write
   * @param offset the file offset to write to, or -1 to write at current position
   * @return the number of bytes actually written
   * @throws WasmException if writing fails or permission is denied
   * @throws IllegalArgumentException if handle or buffer is null
   * @throws IllegalStateException if the handle is closed or not writable
   */
  long writeFile(final WasiFileHandle handle, final ByteBuffer buffer, final long offset)
      throws WasmException;

  /**
   * Opens a directory for reading its contents.
   *
   * <p>This method opens a directory to allow reading its contents with readDirectory. The returned
   * handle can be used to iterate through directory entries.
   *
   * @param path the path to the directory relative to a pre-opened directory
   * @param rights rights that will be available on the returned directory handle
   * @return a directory handle for reading directory contents
   * @throws WasmException if the directory cannot be opened or permission is denied
   * @throws IllegalArgumentException if path is null
   */
  WasiDirectoryHandle openDirectory(final String path, final WasiRights rights)
      throws WasmException;

  /**
   * Reads the contents of a directory.
   *
   * <p>This method returns a list of directory entries (files and subdirectories) contained in the
   * specified directory. Each entry includes name, type, and basic metadata.
   *
   * @param handle the directory handle to read from
   * @return a list of directory entries
   * @throws WasmException if reading the directory fails or permission is denied
   * @throws IllegalArgumentException if handle is null
   * @throws IllegalStateException if the handle is closed
   */
  List<WasiDirEntry> readDirectory(final WasiDirectoryHandle handle) throws WasmException;

  /**
   * Creates a new directory at the specified path.
   *
   * <p>This method creates a new directory with the specified permissions. The parent directory
   * must exist and the caller must have appropriate creation rights.
   *
   * @param path the path where the directory should be created
   * @param permissions the permissions to set on the new directory
   * @throws WasmException if directory creation fails or permission is denied
   * @throws IllegalArgumentException if path is null
   */
  void createDirectory(final String path, final WasiPermissions permissions) throws WasmException;

  /**
   * Removes an empty directory at the specified path.
   *
   * <p>This method removes a directory that must be empty. To remove a directory with contents, all
   * files and subdirectories must be removed first.
   *
   * @param path the path to the directory to remove
   * @throws WasmException if directory removal fails or permission is denied
   * @throws IllegalArgumentException if path is null
   * @throws IllegalStateException if the directory is not empty
   */
  void removeDirectory(final String path) throws WasmException;

  /**
   * Gets file or directory metadata (stats).
   *
   * <p>This method retrieves detailed metadata about a file or directory, including size,
   * permissions, timestamps, and type information.
   *
   * @param path the path to the file or directory
   * @return file statistics and metadata
   * @throws WasmException if getting stats fails or permission is denied
   * @throws IllegalArgumentException if path is null
   */
  WasiFileStats getFileStats(final String path) throws WasmException;

  /**
   * Sets file or directory metadata.
   *
   * <p>This method updates the metadata of a file or directory. Not all metadata fields may be
   * modifiable on all filesystems.
   *
   * @param path the path to the file or directory
   * @param stats the new metadata to set
   * @throws WasmException if setting stats fails or permission is denied
   * @throws IllegalArgumentException if path or stats is null
   */
  void setFileStats(final String path, final WasiFileStats stats) throws WasmException;

  /**
   * Sets file or directory permissions.
   *
   * <p>This method updates the permissions of a file or directory. The exact meaning of permissions
   * may vary by filesystem and platform.
   *
   * @param path the path to the file or directory
   * @param permissions the new permissions to set
   * @throws WasmException if setting permissions fails or permission is denied
   * @throws IllegalArgumentException if path or permissions is null
   */
  void setFilePermissions(final String path, final WasiPermissions permissions)
      throws WasmException;

  /**
   * Canonicalizes a path by resolving symbolic links and relative components.
   *
   * <p>This method returns the canonical (absolute) path for the given path, resolving any symbolic
   * links, "." and ".." components along the way.
   *
   * @param path the path to canonicalize
   * @return the canonical absolute path
   * @throws WasmException if path resolution fails or permission is denied
   * @throws IllegalArgumentException if path is null
   */
  String canonicalizePath(final String path) throws WasmException;

  /**
   * Creates a symbolic link from oldPath to newPath.
   *
   * <p>This method creates a symbolic link where newPath points to oldPath. The target (oldPath)
   * does not need to exist at the time the link is created.
   *
   * @param oldPath the target path the symlink will point to
   * @param newPath the path where the symlink will be created
   * @throws WasmException if symlink creation fails or permission is denied
   * @throws IllegalArgumentException if oldPath or newPath is null
   */
  void symlinkCreate(final String oldPath, final String newPath) throws WasmException;

  /**
   * Reads the target of a symbolic link.
   *
   * <p>This method returns the target path that a symbolic link points to. The returned path may be
   * relative or absolute depending on how the link was created.
   *
   * @param path the path to the symbolic link
   * @return the target path the symbolic link points to
   * @throws WasmException if reading the symlink fails or permission is denied
   * @throws IllegalArgumentException if path is null
   * @throws IllegalStateException if the path is not a symbolic link
   */
  String readSymlink(final String path) throws WasmException;

  /**
   * Renames or moves a file or directory from oldPath to newPath.
   *
   * <p>This method moves a file or directory from one location to another, potentially renaming it
   * in the process. The operation is atomic if both paths are on the same filesystem.
   *
   * @param oldPath the current path of the file or directory
   * @param newPath the new path for the file or directory
   * @throws WasmException if the rename operation fails or permission is denied
   * @throws IllegalArgumentException if oldPath or newPath is null
   */
  void rename(final String oldPath, final String newPath) throws WasmException;

  /**
   * Removes a file (unlinks it from the filesystem).
   *
   * <p>This method removes a file from the filesystem. For directories, use removeDirectory
   * instead.
   *
   * @param path the path to the file to remove
   * @throws WasmException if file removal fails or permission is denied
   * @throws IllegalArgumentException if path is null
   * @throws IllegalStateException if the path refers to a directory
   */
  void unlink(final String path) throws WasmException;
}
