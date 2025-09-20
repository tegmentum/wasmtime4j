package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;

/**
 * Represents an entry in a WASI directory listing.
 *
 * <p>WasiDirEntry contains information about a file or directory found during directory traversal
 * operations. It provides essential metadata to identify and categorize filesystem entries within
 * the WASI sandbox.
 *
 * <p>Directory entries are returned by {@link WasiFilesystem#readDirectory} operations and
 * provide a snapshot of the directory contents at the time of the read operation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * List<WasiDirEntry> entries = filesystem.readDirectory(dirHandle);
 * for (WasiDirEntry entry : entries) {
 *     System.out.printf("%s (%s) - %d bytes\n",
 *         entry.getName(), entry.getType(), entry.getSize());
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiDirEntry {

  /**
   * Gets the name of this directory entry.
   *
   * @return the entry name (filename or directory name)
   */
  String getName();

  /**
   * Gets the type of this directory entry.
   *
   * @return the entry type (file, directory, symlink, etc.)
   */
  WasiFileType getType();

  /**
   * Gets the inode number for this entry.
   *
   * @return the inode number, or 0 if not available
   */
  long getInode();

  /**
   * Gets the size of this entry in bytes.
   *
   * @return the size in bytes, or 0 for directories or if not available
   */
  long getSize();

  /**
   * Gets the last access time for this entry.
   *
   * @return the last access time, or null if not available
   */
  Instant getAccessTime();

  /**
   * Gets the last modification time for this entry.
   *
   * @return the last modification time, or null if not available
   */
  Instant getModificationTime();

  /**
   * Gets the creation time for this entry.
   *
   * @return the creation time, or null if not available
   */
  Instant getCreationTime();

  /**
   * Gets the permissions for this entry.
   *
   * @return the file permissions, or null if not available
   */
  WasiPermissions getPermissions();

  /**
   * Checks if this entry represents a regular file.
   *
   * @return true if this is a regular file
   */
  default boolean isFile() {
    return getType() == WasiFileType.REGULAR_FILE;
  }

  /**
   * Checks if this entry represents a directory.
   *
   * @return true if this is a directory
   */
  default boolean isDirectory() {
    return getType() == WasiFileType.DIRECTORY;
  }

  /**
   * Checks if this entry represents a symbolic link.
   *
   * @return true if this is a symbolic link
   */
  default boolean isSymbolicLink() {
    return getType() == WasiFileType.SYMBOLIC_LINK;
  }

  /**
   * Checks if this entry represents a special file (device, socket, etc.).
   *
   * @return true if this is a special file
   */
  default boolean isSpecialFile() {
    final WasiFileType type = getType();
    return type == WasiFileType.BLOCK_DEVICE
        || type == WasiFileType.CHARACTER_DEVICE
        || type == WasiFileType.SOCKET_STREAM
        || type == WasiFileType.SOCKET_DGRAM;
  }
}