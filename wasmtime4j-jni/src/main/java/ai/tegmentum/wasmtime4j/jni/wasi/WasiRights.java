package ai.tegmentum.wasmtime4j.jni.wasi;

/**
 * WASI file descriptor rights constants.
 *
 * <p>These constants define the permissions that can be granted to file descriptors in WASI,
 * enabling fine-grained access control for file system operations.
 *
 * @since 1.0.0
 */
public enum WasiRights {
  /** The right to invoke fd_datasync. */
  FD_DATASYNC(1L << 0),

  /** The right to invoke fd_read and sock_recv. */
  FD_READ(1L << 1),

  /** The right to invoke fd_seek. */
  FD_SEEK(1L << 2),

  /** The right to invoke fd_fdstat_set_flags. */
  FD_FDSTAT_SET_FLAGS(1L << 3),

  /** The right to invoke fd_sync. */
  FD_SYNC(1L << 4),

  /** The right to invoke fd_tell. */
  FD_TELL(1L << 5),

  /** The right to invoke fd_write and sock_send. */
  FD_WRITE(1L << 6),

  /** The right to invoke fd_advise. */
  FD_ADVISE(1L << 7),

  /** The right to invoke fd_allocate. */
  FD_ALLOCATE(1L << 8),

  /** The right to invoke path_create_directory. */
  PATH_CREATE_DIRECTORY(1L << 9),

  /** The right to invoke path_create_file. */
  PATH_CREATE_FILE(1L << 10),

  /** The right to invoke path_link with the file descriptor as the source directory. */
  PATH_LINK_SOURCE(1L << 11),

  /** The right to invoke path_link with the file descriptor as the target directory. */
  PATH_LINK_TARGET(1L << 12),

  /** The right to invoke path_open. */
  PATH_OPEN(1L << 13),

  /** The right to invoke fd_readdir. */
  FD_READDIR(1L << 14),

  /** The right to invoke path_readlink. */
  PATH_READLINK(1L << 15),

  /** The right to invoke path_rename with the file descriptor as the source directory. */
  PATH_RENAME_SOURCE(1L << 16),

  /** The right to invoke path_rename with the file descriptor as the target directory. */
  PATH_RENAME_TARGET(1L << 17),

  /** The right to invoke path_filestat_get. */
  PATH_FILESTAT_GET(1L << 18),

  /** The right to change a file's size. */
  PATH_FILESTAT_SET_SIZE(1L << 19),

  /** The right to invoke path_filestat_set_times. */
  PATH_FILESTAT_SET_TIMES(1L << 20),

  /** The right to invoke fd_filestat_get. */
  FD_FILESTAT_GET(1L << 21),

  /** The right to invoke fd_filestat_set_size. */
  FD_FILESTAT_SET_SIZE(1L << 22),

  /** The right to invoke fd_filestat_set_times. */
  FD_FILESTAT_SET_TIMES(1L << 23),

  /** The right to invoke path_symlink. */
  PATH_SYMLINK(1L << 24),

  /** The right to invoke path_remove_directory. */
  PATH_REMOVE_DIRECTORY(1L << 25),

  /** The right to invoke path_unlink_file. */
  PATH_UNLINK_FILE(1L << 26),

  /** The right to invoke poll_oneoff. */
  POLL_FD_READWRITE(1L << 27),

  /** The right to invoke sock_shutdown. */
  SOCK_SHUTDOWN(1L << 28);

  private final long value;

  WasiRights(final long value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this right.
   *
   * @return the numeric value
   */
  public long getValue() {
    return value;
  }

  /**
   * Creates a rights mask from multiple rights.
   *
   * @param rights the rights to combine
   * @return the combined rights mask
   */
  public static long combine(final WasiRights... rights) {
    long mask = 0;
    for (final WasiRights right : rights) {
      mask |= right.value;
    }
    return mask;
  }

  /**
   * Checks if a rights mask contains a specific right.
   *
   * @param mask the rights mask to check
   * @param right the right to check for
   * @return true if the mask contains the right, false otherwise
   */
  public static boolean contains(final long mask, final WasiRights right) {
    return (mask & right.value) != 0;
  }
}
