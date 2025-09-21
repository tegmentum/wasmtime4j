package ai.tegmentum.wasmtime4j.wasi;

/**
 * Rights for file and directory operations in the WASI filesystem.
 *
 * <p>WasiRights define what operations are allowed on file descriptors within the capability-based
 * security model. Rights are hierarchical and must be explicitly granted to perform operations.
 * They form the core of WASI's security model by limiting what WebAssembly modules can do.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Basic file reading rights
 * WasiRights readRights = WasiRights.FD_READ.combine(WasiRights.FD_SEEK);
 *
 * // Full file manipulation rights
 * WasiRights writeRights = WasiRights.of(
 *     WasiRights.FD_READ, WasiRights.FD_WRITE, WasiRights.FD_SEEK, WasiRights.FD_TRUNCATE
 * );
 *
 * // Directory traversal rights
 * WasiRights dirRights = WasiRights.PATH_OPEN.combine(WasiRights.FD_READDIR);
 * }</pre>
 *
 * @since 1.0.0
 */
public enum WasiRights {

  /** The right to invoke `fd_datasync`. */
  FD_DATASYNC(0x0000000000000001L),

  /** The right to invoke `fd_read` and `sock_recv`. */
  FD_READ(0x0000000000000002L),

  /** The right to invoke `fd_seek`. */
  FD_SEEK(0x0000000000000004L),

  /** The right to invoke `fd_fdstat_set_flags`. */
  FD_FDSTAT_SET_FLAGS(0x0000000000000008L),

  /** The right to invoke `fd_sync`. */
  FD_SYNC(0x0000000000000010L),

  /** The right to invoke `fd_tell`. */
  FD_TELL(0x0000000000000020L),

  /** The right to invoke `fd_write` and `sock_send`. */
  FD_WRITE(0x0000000000000040L),

  /** The right to invoke `fd_advise`. */
  FD_ADVISE(0x0000000000000080L),

  /** The right to invoke `fd_allocate`. */
  FD_ALLOCATE(0x0000000000000100L),

  /** The right to invoke `path_create_directory`. */
  PATH_CREATE_DIRECTORY(0x0000000000000200L),

  /** The right to invoke `path_create_file`. */
  PATH_CREATE_FILE(0x0000000000000400L),

  /** The right to invoke `path_link` with the file descriptor as the source directory. */
  PATH_LINK_SOURCE(0x0000000000000800L),

  /** The right to invoke `path_link` with the file descriptor as the target directory. */
  PATH_LINK_TARGET(0x0000000000001000L),

  /** The right to invoke `path_open`. */
  PATH_OPEN(0x0000000000002000L),

  /** The right to invoke `fd_readdir`. */
  FD_READDIR(0x0000000000004000L),

  /** The right to invoke `path_readlink`. */
  PATH_READLINK(0x0000000000008000L),

  /** The right to invoke `path_rename` with the file descriptor as the source directory. */
  PATH_RENAME_SOURCE(0x0000000000010000L),

  /** The right to invoke `path_rename` with the file descriptor as the target directory. */
  PATH_RENAME_TARGET(0x0000000000020000L),

  /** The right to invoke `path_filestat_get`. */
  PATH_FILESTAT_GET(0x0000000000040000L),

  /** The right to invoke `path_filestat_set_size` and `path_filestat_set_times`. */
  PATH_FILESTAT_SET_SIZE(0x0000000000080000L),

  /** The right to invoke `path_filestat_set_times`. */
  PATH_FILESTAT_SET_TIMES(0x0000000000100000L),

  /** The right to invoke `fd_filestat_get`. */
  FD_FILESTAT_GET(0x0000000000200000L),

  /** The right to invoke `fd_filestat_set_size`. */
  FD_FILESTAT_SET_SIZE(0x0000000000400000L),

  /** The right to invoke `fd_filestat_set_times`. */
  FD_FILESTAT_SET_TIMES(0x0000000000800000L),

  /** The right to invoke `path_symlink`. */
  PATH_SYMLINK(0x0000000001000000L),

  /** The right to invoke `path_remove_directory`. */
  PATH_REMOVE_DIRECTORY(0x0000000002000000L),

  /** The right to invoke `path_unlink_file`. */
  PATH_UNLINK_FILE(0x0000000004000000L),

  /** The right to invoke `poll_oneoff` with a subscription that has a file descriptor. */
  POLL_FD_READWRITE(0x0000000008000000L),

  /** The right to invoke `sock_shutdown`. */
  SOCK_SHUTDOWN(0x0000000010000000L),

  /** The right to invoke `fd_truncate`. */
  FD_TRUNCATE(0x0000000020000000L);

  private final long value;

  WasiRights(final long value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this right.
   *
   * @return the right value
   */
  public long getValue() {
    return value;
  }

  /**
   * Combines this right with another right.
   *
   * @param other the other right to combine with
   * @return a new combined rights object
   * @throws IllegalArgumentException if other is null
   */
  public WasiRightsSet combine(final WasiRights other) {
    if (other == null) {
      throw new IllegalArgumentException("Other right cannot be null");
    }
    return new WasiRightsSet(this.value | other.value);
  }

  /**
   * Creates a rights set from multiple individual rights.
   *
   * @param rights the rights to combine
   * @return a new rights set containing all the specified rights
   * @throws IllegalArgumentException if rights array is null or empty
   */
  public static WasiRightsSet of(final WasiRights... rights) {
    if (rights == null || rights.length == 0) {
      throw new IllegalArgumentException("Rights array cannot be null or empty");
    }

    long combined = 0;
    for (final WasiRights right : rights) {
      if (right != null) {
        combined |= right.value;
      }
    }
    return new WasiRightsSet(combined);
  }

  /**
   * Checks if this right is set in the given rights value.
   *
   * @param rights the rights value to check
   * @return true if this right is set
   */
  public boolean isSet(final long rights) {
    return (rights & this.value) != 0;
  }

  /** A set of combined rights. */
  public static final class WasiRightsSet {
    private final long value;

    WasiRightsSet(final long value) {
      this.value = value;
    }

    /**
     * Gets the combined numeric value of all rights in this set.
     *
     * @return the combined rights value
     */
    public long getValue() {
      return value;
    }

    /**
     * Combines this rights set with another right.
     *
     * @param right the right to add to this set
     * @return a new rights set with the additional right
     * @throws IllegalArgumentException if right is null
     */
    public WasiRightsSet combine(final WasiRights right) {
      if (right == null) {
        throw new IllegalArgumentException("Right cannot be null");
      }
      return new WasiRightsSet(this.value | right.value);
    }

    /**
     * Checks if the specified right is set in this rights set.
     *
     * @param right the right to check for
     * @return true if the right is set
     * @throws IllegalArgumentException if right is null
     */
    public boolean contains(final WasiRights right) {
      if (right == null) {
        throw new IllegalArgumentException("Right cannot be null");
      }
      return right.isSet(this.value);
    }

    /**
     * Checks if this rights set contains all the rights in another set.
     *
     * @param other the other rights set to check
     * @return true if this set contains all rights from the other set
     * @throws IllegalArgumentException if other is null
     */
    public boolean containsAll(final WasiRightsSet other) {
      if (other == null) {
        throw new IllegalArgumentException("Other rights set cannot be null");
      }
      return (this.value & other.value) == other.value;
    }

    /**
     * Checks if this rights set is empty (no rights set).
     *
     * @return true if no rights are set
     */
    public boolean isEmpty() {
      return value == 0;
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final WasiRightsSet that = (WasiRightsSet) obj;
      return value == that.value;
    }

    @Override
    public int hashCode() {
      return Long.hashCode(value);
    }

    @Override
    public String toString() {
      if (value == 0) {
        return "WasiRightsSet[]";
      }

      final StringBuilder sb = new StringBuilder("WasiRightsSet[");
      boolean first = true;
      for (final WasiRights right : WasiRights.values()) {
        if (right.isSet(value)) {
          if (!first) {
            sb.append(", ");
          }
          sb.append(right.name());
          first = false;
        }
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
