package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.nio.file.attribute.FileTime;

/**
 * Directory entry information for WASI directory listing operations.
 *
 * <p>This class represents a single entry in a directory listing, providing essential information
 * about files and subdirectories in a platform-independent manner.
 *
 * @since 1.0.0
 */
public final class WasiDirectoryEntry {

  /** The name of the file or directory. */
  private final String name;

  /** Whether this entry is a regular file. */
  private final boolean isRegularFile;

  /** Whether this entry is a directory. */
  private final boolean isDirectory;

  /** Whether this entry is a symbolic link. */
  private final boolean isSymbolicLink;

  /** The size of the file in bytes (0 for directories). */
  private final long size;

  /** The last modified time. */
  private final FileTime lastModifiedTime;

  /**
   * Creates a new directory entry.
   *
   * @param name the name of the file or directory
   * @param isRegularFile whether this is a regular file
   * @param isDirectory whether this is a directory
   * @param isSymbolicLink whether this is a symbolic link
   * @param size the size of the file in bytes
   * @param lastModifiedTime the last modified time
   */
  public WasiDirectoryEntry(final String name, final boolean isRegularFile,
      final boolean isDirectory, final boolean isSymbolicLink, final long size,
      final FileTime lastModifiedTime) {
    JniValidation.requireNonEmpty(name, "name");
    JniValidation.requireNonNull(lastModifiedTime, "lastModifiedTime");

    this.name = name;
    this.isRegularFile = isRegularFile;
    this.isDirectory = isDirectory;
    this.isSymbolicLink = isSymbolicLink;
    this.size = size;
    this.lastModifiedTime = lastModifiedTime;
  }

  /**
   * Gets the name of the file or directory.
   *
   * @return the entry name
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if this entry is a regular file.
   *
   * @return true if regular file, false otherwise
   */
  public boolean isRegularFile() {
    return isRegularFile;
  }

  /**
   * Checks if this entry is a directory.
   *
   * @return true if directory, false otherwise
   */
  public boolean isDirectory() {
    return isDirectory;
  }

  /**
   * Checks if this entry is a symbolic link.
   *
   * @return true if symbolic link, false otherwise
   */
  public boolean isSymbolicLink() {
    return isSymbolicLink;
  }

  /**
   * Gets the size of the file in bytes.
   *
   * @return the file size (0 for directories)
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
   * Gets the file type as a WASI file type constant.
   *
   * @return the WASI file type
   */
  public int getWasiFileType() {
    if (isRegularFile) {
      return 4; // WASI_FILETYPE_REGULAR_FILE
    } else if (isDirectory) {
      return 3; // WASI_FILETYPE_DIRECTORY
    } else if (isSymbolicLink) {
      return 7; // WASI_FILETYPE_SYMBOLIC_LINK
    } else {
      return 0; // WASI_FILETYPE_UNKNOWN
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiDirectoryEntry other = (WasiDirectoryEntry) obj;
    return name.equals(other.name)
        && isRegularFile == other.isRegularFile
        && isDirectory == other.isDirectory
        && isSymbolicLink == other.isSymbolicLink
        && size == other.size
        && lastModifiedTime.equals(other.lastModifiedTime);
  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + Boolean.hashCode(isRegularFile);
    result = 31 * result + Boolean.hashCode(isDirectory);
    result = 31 * result + Boolean.hashCode(isSymbolicLink);
    result = 31 * result + Long.hashCode(size);
    result = 31 * result + lastModifiedTime.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final String type;
    if (isRegularFile) {
      type = "FILE";
    } else if (isDirectory) {
      type = "DIR";
    } else if (isSymbolicLink) {
      type = "LINK";
    } else {
      type = "UNKNOWN";
    }

    return String.format("WasiDirectoryEntry{name='%s', type=%s, size=%d, lastModified=%s}",
        name, type, size, lastModifiedTime);
  }
}