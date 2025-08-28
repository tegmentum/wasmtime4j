package ai.tegmentum.wasmtime4j.panama.wasi;

import java.nio.file.attribute.FileTime;

/**
 * File metadata information for WASI file system operations in Panama FFI context.
 *
 * <p>This class encapsulates file metadata that is returned by WASI stat operations, providing
 * comprehensive information about files and directories in a platform-independent manner.
 *
 * @since 1.0.0
 */
public final class WasiFileMetadata {

  /** The file size in bytes. */
  private final long size;

  /** The last modified time. */
  private final FileTime lastModifiedTime;

  /** The last access time. */
  private final FileTime lastAccessTime;

  /** The creation time. */
  private final FileTime creationTime;

  /** Whether this is a regular file. */
  private final boolean isRegularFile;

  /** Whether this is a directory. */
  private final boolean isDirectory;

  /** Whether this is a symbolic link. */
  private final boolean isSymbolicLink;

  /** Whether the file is readable. */
  private final boolean readable;

  /** Whether the file is writable. */
  private final boolean writable;

  /** Whether the file is executable. */
  private final boolean executable;

  /**
   * Creates new file metadata.
   *
   * @param size the file size in bytes
   * @param lastModifiedTime the last modified time
   * @param lastAccessTime the last access time
   * @param creationTime the creation time
   * @param isRegularFile whether this is a regular file
   * @param isDirectory whether this is a directory
   * @param isSymbolicLink whether this is a symbolic link
   * @param readable whether the file is readable
   * @param writable whether the file is writable
   * @param executable whether the file is executable
   */
  public WasiFileMetadata(
      final long size,
      final FileTime lastModifiedTime,
      final FileTime lastAccessTime,
      final FileTime creationTime,
      final boolean isRegularFile,
      final boolean isDirectory,
      final boolean isSymbolicLink,
      final boolean readable,
      final boolean writable,
      final boolean executable) {
    this.size = size;
    this.lastModifiedTime = lastModifiedTime;
    this.lastAccessTime = lastAccessTime;
    this.creationTime = creationTime;
    this.isRegularFile = isRegularFile;
    this.isDirectory = isDirectory;
    this.isSymbolicLink = isSymbolicLink;
    this.readable = readable;
    this.writable = writable;
    this.executable = executable;
  }

  /**
   * Gets the file size in bytes.
   *
   * @return the file size
   */
  public long getSize() {
    return size;
  }

  /**
   * Gets the last modified time.
   *
   * @return the last modified time
   */
  public FileTime getLastModifiedTime() {
    return lastModifiedTime;
  }

  /**
   * Gets the last access time.
   *
   * @return the last access time
   */
  public FileTime getLastAccessTime() {
    return lastAccessTime;
  }

  /**
   * Gets the creation time.
   *
   * @return the creation time
   */
  public FileTime getCreationTime() {
    return creationTime;
  }

  /**
   * Checks if this is a regular file.
   *
   * @return true if regular file, false otherwise
   */
  public boolean isRegularFile() {
    return isRegularFile;
  }

  /**
   * Checks if this is a directory.
   *
   * @return true if directory, false otherwise
   */
  public boolean isDirectory() {
    return isDirectory;
  }

  /**
   * Checks if this is a symbolic link.
   *
   * @return true if symbolic link, false otherwise
   */
  public boolean isSymbolicLink() {
    return isSymbolicLink;
  }

  /**
   * Checks if the file is readable.
   *
   * @return true if readable, false otherwise
   */
  public boolean isReadable() {
    return readable;
  }

  /**
   * Checks if the file is writable.
   *
   * @return true if writable, false otherwise
   */
  public boolean isWritable() {
    return writable;
  }

  /**
   * Checks if the file is executable.
   *
   * @return true if executable, false otherwise
   */
  public boolean isExecutable() {
    return executable;
  }

  /**
   * Gets the last modified time as a Unix timestamp in seconds.
   *
   * @return the last modified time in seconds since epoch
   */
  public long getLastModifiedTimeSeconds() {
    return lastModifiedTime.toInstant().getEpochSecond();
  }

  /**
   * Gets the last modified time nanoseconds component.
   *
   * @return the nanoseconds component of last modified time
   */
  public long getLastModifiedTimeNanos() {
    return lastModifiedTime.toInstant().getNano();
  }

  /**
   * Gets the last access time as a Unix timestamp in seconds.
   *
   * @return the last access time in seconds since epoch
   */
  public long getLastAccessTimeSeconds() {
    return lastAccessTime.toInstant().getEpochSecond();
  }

  /**
   * Gets the last access time nanoseconds component.
   *
   * @return the nanoseconds component of last access time
   */
  public long getLastAccessTimeNanos() {
    return lastAccessTime.toInstant().getNano();
  }

  /**
   * Gets the creation time as a Unix timestamp in seconds.
   *
   * @return the creation time in seconds since epoch
   */
  public long getCreationTimeSeconds() {
    return creationTime.toInstant().getEpochSecond();
  }

  /**
   * Gets the creation time nanoseconds component.
   *
   * @return the nanoseconds component of creation time
   */
  public long getCreationTimeNanos() {
    return creationTime.toInstant().getNano();
  }

  @Override
  public String toString() {
    return String.format(
        "WasiFileMetadata{size=%d, lastModified=%s, lastAccess=%s, creation=%s, regularFile=%s,"
            + " directory=%s, symbolicLink=%s, readable=%s, writable=%s, executable=%s}",
        size,
        lastModifiedTime,
        lastAccessTime,
        creationTime,
        isRegularFile,
        isDirectory,
        isSymbolicLink,
        readable,
        writable,
        executable);
  }
}
