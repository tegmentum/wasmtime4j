package ai.tegmentum.wasmtime4j.wasi;

/**
 * Types of files in the WASI filesystem.
 *
 * <p>WasiFileType represents the different kinds of filesystem entries that can exist within the
 * WASI sandbox. These types correspond to the standard POSIX file types and are used to categorize
 * files, directories, and special filesystem objects.
 *
 * <p>File types are used in directory listings and file metadata operations to help applications
 * understand what kind of filesystem object they are working with.
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

  /** Socket for stream-oriented communication. */
  SOCKET_STREAM(5),

  /** Socket for datagram-oriented communication. */
  SOCKET_DGRAM(6),

  /** Symbolic link. */
  SYMBOLIC_LINK(7);

  private final int value;

  WasiFileType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this file type.
   *
   * @return the file type value
   */
  public int getValue() {
    return value;
  }

  /**
   * Creates a WasiFileType from its numeric value.
   *
   * @param value the numeric file type value
   * @return the corresponding WasiFileType
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static WasiFileType fromValue(final int value) {
    for (final WasiFileType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown file type value: " + value);
  }

  /**
   * Checks if this file type represents a regular file.
   *
   * @return true if this is a regular file
   */
  public boolean isRegularFile() {
    return this == REGULAR_FILE;
  }

  /**
   * Checks if this file type represents a directory.
   *
   * @return true if this is a directory
   */
  public boolean isDirectory() {
    return this == DIRECTORY;
  }

  /**
   * Checks if this file type represents a symbolic link.
   *
   * @return true if this is a symbolic link
   */
  public boolean isSymbolicLink() {
    return this == SYMBOLIC_LINK;
  }

  /**
   * Checks if this file type represents a device file.
   *
   * @return true if this is a block or character device
   */
  public boolean isDevice() {
    return this == BLOCK_DEVICE || this == CHARACTER_DEVICE;
  }

  /**
   * Checks if this file type represents a socket.
   *
   * @return true if this is a stream or datagram socket
   */
  public boolean isSocket() {
    return this == SOCKET_STREAM || this == SOCKET_DGRAM;
  }

  /**
   * Checks if this file type represents a special file.
   *
   * <p>Special files include devices and sockets - anything that is not a regular file,
   * directory, or symbolic link.
   *
   * @return true if this is a special file
   */
  public boolean isSpecialFile() {
    return isDevice() || isSocket();
  }
}