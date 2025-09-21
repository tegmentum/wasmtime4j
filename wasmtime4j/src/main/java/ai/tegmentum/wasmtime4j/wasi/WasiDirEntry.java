package ai.tegmentum.wasmtime4j.wasi;

/**
 * Represents a directory entry returned by directory reading operations.
 *
 * <p>A directory entry contains information about a file or subdirectory within a directory,
 * including its name, type, and basic metadata. This corresponds to the WASI Preview 1 dirent
 * structure.
 *
 * @since 1.0.0
 */
public final class WasiDirEntry {

  private final String name;
  private final WasiFileType type;
  private final long inode;
  private final long cookie;

  /**
   * Creates a new directory entry.
   *
   * @param name the name of the file or directory
   * @param type the type of the entry (file, directory, symlink, etc.)
   * @param inode the inode number (filesystem-specific identifier)
   * @param cookie the directory reading position cookie
   */
  public WasiDirEntry(
      final String name, final WasiFileType type, final long inode, final long cookie) {
    this.name = name;
    this.type = type;
    this.inode = inode;
    this.cookie = cookie;
  }

  /**
   * Gets the name of this directory entry.
   *
   * <p>This is the filename or directory name without any path components.
   *
   * @return the entry name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the type of this directory entry.
   *
   * <p>The type indicates whether this entry is a regular file, directory, symbolic link, or
   * other special file type.
   *
   * @return the entry type
   */
  public WasiFileType getType() {
    return type;
  }

  /**
   * Gets the inode number for this entry.
   *
   * <p>The inode is a filesystem-specific identifier that uniquely identifies the file or
   * directory. It may be 0 if the filesystem doesn't support inodes.
   *
   * @return the inode number
   */
  public long getInode() {
    return inode;
  }

  /**
   * Gets the directory reading position cookie.
   *
   * <p>The cookie represents the position in the directory reading sequence. It can be used to
   * resume reading from a specific position.
   *
   * @return the position cookie
   */
  public long getCookie() {
    return cookie;
  }

  /**
   * Checks if this entry represents a regular file.
   *
   * @return true if this is a regular file, false otherwise
   */
  public boolean isFile() {
    return type == WasiFileType.REGULAR_FILE;
  }

  /**
   * Checks if this entry represents a directory.
   *
   * @return true if this is a directory, false otherwise
   */
  public boolean isDirectory() {
    return type == WasiFileType.DIRECTORY;
  }

  /**
   * Checks if this entry represents a symbolic link.
   *
   * @return true if this is a symbolic link, false otherwise
   */
  public boolean isSymbolicLink() {
    return type == WasiFileType.SYMBOLIC_LINK;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiDirEntry other = (WasiDirEntry) obj;
    return inode == other.inode
        && cookie == other.cookie
        && name.equals(other.name)
        && type == other.type;
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + Long.hashCode(inode);
    result = 31 * result + Long.hashCode(cookie);
    return result;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiDirEntry{name='%s', type=%s, inode=%d, cookie=%d}", name, type, inode, cookie);
  }
}