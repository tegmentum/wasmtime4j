package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * WASI file type constants.
 *
 * <p>These constants define the different types of files that can exist in a WASI file system.
 *
 * @since 1.0.0
 */
public enum WasiFileType {
  /** Unknown file type. */
  UNKNOWN(0),

  /** Block device. */
  BLOCK_DEVICE(1),

  /** Character device. */
  CHARACTER_DEVICE(2),

  /** Directory. */
  DIRECTORY(3),

  /** Regular file. */
  REGULAR_FILE(4),

  /** Socket domain. */
  SOCKET_DGRAM(5),

  /** Socket stream. */
  SOCKET_STREAM(6),

  /** Symbolic link. */
  SYMBOLIC_LINK(7);

  private final int value;

  WasiFileType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this file type.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets a WasiFileType by its numeric value.
   *
   * @param value the numeric value
   * @return the corresponding WasiFileType
   * @throws IllegalArgumentException if the value is invalid
   */
  public static WasiFileType fromValue(final int value) {
    for (final WasiFileType fileType : values()) {
      if (fileType.value == value) {
        return fileType;
      }
    }
    throw new IllegalArgumentException("Invalid file type: " + value);
  }

  /**
   * Checks if this file type represents a regular file.
   *
   * @return true if this is a regular file, false otherwise
   */
  public boolean isRegularFile() {
    return this == REGULAR_FILE;
  }

  /**
   * Checks if this file type represents a directory.
   *
   * @return true if this is a directory, false otherwise
   */
  public boolean isDirectory() {
    return this == DIRECTORY;
  }

  /**
   * Checks if this file type represents a symbolic link.
   *
   * @return true if this is a symbolic link, false otherwise
   */
  public boolean isSymbolicLink() {
    return this == SYMBOLIC_LINK;
  }
}
