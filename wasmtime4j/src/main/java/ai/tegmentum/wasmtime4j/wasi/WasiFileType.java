package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI file types corresponding to the WASI Preview 1 filetype enum.
 *
 * <p>These types classify different kinds of filesystem entries and are used in directory
 * listings and file metadata operations.
 *
 * @since 1.0.0
 */
public enum WasiFileType {

  /** The type of the file descriptor or file is unknown or is different from any of the other types specified. */
  UNKNOWN(0),

  /** The file descriptor or file refers to a block device inode. */
  BLOCK_DEVICE(1),

  /** The file descriptor or file refers to a character device inode. */
  CHARACTER_DEVICE(2),

  /** The file descriptor or file refers to a directory inode. */
  DIRECTORY(3),

  /** The file descriptor or file refers to a regular file inode. */
  REGULAR_FILE(4),

  /** The file descriptor or file refers to a datagram socket. */
  SOCKET_DGRAM(5),

  /** The file descriptor or file refers to a byte-stream socket. */
  SOCKET_STREAM(6),

  /** The file refers to a symbolic link inode. */
  SYMBOLIC_LINK(7);

  private final int value;

  WasiFileType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this file type as defined in WASI Preview 1.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts a numeric value to the corresponding WasiFileType.
   *
   * @param value the numeric value
   * @return the corresponding WasiFileType
   * @throws IllegalArgumentException if the value doesn't correspond to a known file type
   */
  public static WasiFileType fromValue(final int value) {
    for (final WasiFileType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown WASI file type value: " + value);
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

  /**
   * Checks if this file type represents a socket.
   *
   * @return true if this is any type of socket, false otherwise
   */
  public boolean isSocket() {
    return this == SOCKET_DGRAM || this == SOCKET_STREAM;
  }

  /**
   * Checks if this file type represents a device.
   *
   * @return true if this is any type of device, false otherwise
   */
  public boolean isDevice() {
    return this == BLOCK_DEVICE || this == CHARACTER_DEVICE;
  }

  @Override
  public String toString() {
    return name() + "(" + value + ")";
  }
}