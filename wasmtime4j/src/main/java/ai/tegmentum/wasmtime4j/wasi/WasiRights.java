package ai.tegmentum.wasmtime4j.wasi;

/**
 * Rights (capabilities) for WASI file descriptors and filesystem operations.
 *
 * <p>Rights in WASI represent capabilities that control what operations can be performed on file
 * descriptors. They implement capability-based security where each file descriptor has an
 * associated set of rights that determine allowed operations.
 *
 * <p>Rights can be combined using bitwise OR operations to specify multiple capabilities.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Rights for reading and seeking in a file
 * long rights = WasiRights.FD_READ | WasiRights.FD_SEEK;
 * WasiFileHandle handle = filesystem.openFile("/tmp/input.txt", flags, rights);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiRights {

  // File descriptor rights
  /** The right to invoke fd_datasync. */
  public static final long FD_DATASYNC = 1L << 0;

  /** The right to invoke fd_read and sock_recv. */
  public static final long FD_READ = 1L << 1;

  /** The right to invoke fd_seek. */
  public static final long FD_SEEK = 1L << 2;

  /** The right to invoke fd_fdstat_set_flags. */
  public static final long FD_FDSTAT_SET_FLAGS = 1L << 3;

  /** The right to invoke fd_sync. */
  public static final long FD_SYNC = 1L << 4;

  /** The right to invoke fd_tell. */
  public static final long FD_TELL = 1L << 5;

  /** The right to invoke fd_write and sock_send. */
  public static final long FD_WRITE = 1L << 6;

  /** The right to invoke fd_advise. */
  public static final long FD_ADVISE = 1L << 7;

  /** The right to invoke fd_allocate. */
  public static final long FD_ALLOCATE = 1L << 8;

  // Path rights
  /** The right to invoke path_create_directory. */
  public static final long PATH_CREATE_DIRECTORY = 1L << 9;

  /** The right to invoke path_create_file. */
  public static final long PATH_CREATE_FILE = 1L << 10;

  /** The right to invoke path_link with source. */
  public static final long PATH_LINK_SOURCE = 1L << 11;

  /** The right to invoke path_link with target. */
  public static final long PATH_LINK_TARGET = 1L << 12;

  /** The right to invoke path_open. */
  public static final long PATH_OPEN = 1L << 13;

  /** The right to invoke fd_readdir. */
  public static final long FD_READDIR = 1L << 14;

  /** The right to invoke path_readlink. */
  public static final long PATH_READLINK = 1L << 15;

  /** The right to invoke path_rename with source. */
  public static final long PATH_RENAME_SOURCE = 1L << 16;

  /** The right to invoke path_rename with target. */
  public static final long PATH_RENAME_TARGET = 1L << 17;

  /** The right to invoke path_filestat_get. */
  public static final long PATH_FILESTAT_GET = 1L << 18;

  /** The right to invoke path_filestat_set_size. */
  public static final long PATH_FILESTAT_SET_SIZE = 1L << 19;

  /** The right to invoke path_filestat_set_times. */
  public static final long PATH_FILESTAT_SET_TIMES = 1L << 20;

  /** The right to invoke fd_filestat_get. */
  public static final long FD_FILESTAT_GET = 1L << 21;

  /** The right to invoke fd_filestat_set_size. */
  public static final long FD_FILESTAT_SET_SIZE = 1L << 22;

  /** The right to invoke fd_filestat_set_times. */
  public static final long FD_FILESTAT_SET_TIMES = 1L << 23;

  /** The right to invoke path_symlink. */
  public static final long PATH_SYMLINK = 1L << 24;

  /** The right to invoke path_remove_directory. */
  public static final long PATH_REMOVE_DIRECTORY = 1L << 25;

  /** The right to invoke path_unlink_file. */
  public static final long PATH_UNLINK_FILE = 1L << 26;

  // Poll rights
  /** The right to invoke poll_oneoff to subscribe to FD_READ. */
  public static final long POLL_FD_READ = 1L << 27;

  /** The right to invoke poll_oneoff to subscribe to FD_WRITE. */
  public static final long POLL_FD_WRITE = 1L << 28;

  // Socket rights
  /** The right to invoke sock_shutdown. */
  public static final long SOCK_SHUTDOWN = 1L << 29;

  /** The right to invoke sock_accept. */
  public static final long SOCK_ACCEPT = 1L << 30;

  /** The right to invoke sock_connect. */
  public static final long SOCK_CONNECT = 1L << 31;

  /** The right to invoke sock_listen. */
  public static final long SOCK_LISTEN = 1L << 32;

  /** The right to invoke sock_bind. */
  public static final long SOCK_BIND = 1L << 33;

  /** The right to invoke sock_recv_from. */
  public static final long SOCK_RECV_FROM = 1L << 34;

  /** The right to invoke sock_send_to. */
  public static final long SOCK_SEND_TO = 1L << 35;

  // Common right combinations
  /** Rights for reading files: read, seek, tell, filestat_get. */
  public static final long FILE_READ_RIGHTS =
      FD_READ | FD_SEEK | FD_TELL | FD_FILESTAT_GET | POLL_FD_READ;

  /** Rights for writing files: write, seek, tell, filestat_get, filestat_set_size, sync. */
  public static final long FILE_WRITE_RIGHTS =
      FD_WRITE
          | FD_SEEK
          | FD_TELL
          | FD_FILESTAT_GET
          | FD_FILESTAT_SET_SIZE
          | FD_SYNC
          | FD_DATASYNC
          | POLL_FD_WRITE;

  /** Rights for reading and writing files. */
  public static final long FILE_READ_WRITE_RIGHTS = FILE_READ_RIGHTS | FILE_WRITE_RIGHTS;

  /** Rights for directory operations: readdir, path operations. */
  public static final long DIRECTORY_RIGHTS =
      FD_READDIR
          | PATH_OPEN
          | PATH_FILESTAT_GET
          | PATH_CREATE_DIRECTORY
          | PATH_CREATE_FILE
          | PATH_LINK_SOURCE
          | PATH_LINK_TARGET
          | PATH_SYMLINK
          | PATH_READLINK
          | PATH_RENAME_SOURCE
          | PATH_RENAME_TARGET
          | PATH_REMOVE_DIRECTORY
          | PATH_UNLINK_FILE
          | PATH_FILESTAT_SET_SIZE
          | PATH_FILESTAT_SET_TIMES;

  /** Rights for socket operations: connect, send, receive, shutdown. */
  public static final long SOCKET_CLIENT_RIGHTS =
      FD_READ | FD_WRITE | SOCK_CONNECT | SOCK_SHUTDOWN | POLL_FD_READ | POLL_FD_WRITE;

  /** Rights for server socket operations: bind, listen, accept. */
  public static final long SOCKET_SERVER_RIGHTS =
      SOCK_BIND | SOCK_LISTEN | SOCK_ACCEPT | SOCK_SHUTDOWN | POLL_FD_READ;

  /** Rights for UDP socket operations: bind, send_to, recv_from. */
  public static final long SOCKET_UDP_RIGHTS =
      SOCK_BIND | SOCK_SEND_TO | SOCK_RECV_FROM | POLL_FD_READ | POLL_FD_WRITE;

  /** All defined rights combined. */
  public static final long ALL_RIGHTS =
      FD_DATASYNC
          | FD_READ
          | FD_SEEK
          | FD_FDSTAT_SET_FLAGS
          | FD_SYNC
          | FD_TELL
          | FD_WRITE
          | FD_ADVISE
          | FD_ALLOCATE
          | PATH_CREATE_DIRECTORY
          | PATH_CREATE_FILE
          | PATH_LINK_SOURCE
          | PATH_LINK_TARGET
          | PATH_OPEN
          | FD_READDIR
          | PATH_READLINK
          | PATH_RENAME_SOURCE
          | PATH_RENAME_TARGET
          | PATH_FILESTAT_GET
          | PATH_FILESTAT_SET_SIZE
          | PATH_FILESTAT_SET_TIMES
          | FD_FILESTAT_GET
          | FD_FILESTAT_SET_SIZE
          | FD_FILESTAT_SET_TIMES
          | PATH_SYMLINK
          | PATH_REMOVE_DIRECTORY
          | PATH_UNLINK_FILE
          | POLL_FD_READ
          | POLL_FD_WRITE
          | SOCK_SHUTDOWN
          | SOCK_ACCEPT
          | SOCK_CONNECT
          | SOCK_LISTEN
          | SOCK_BIND
          | SOCK_RECV_FROM
          | SOCK_SEND_TO;

  private WasiRights() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if the given rights value contains the specified right.
   *
   * @param rights the rights value to check
   * @param right the specific right to check for
   * @return true if the right is present, false otherwise
   */
  public static boolean hasRight(final long rights, final long right) {
    return (rights & right) != 0;
  }

  /**
   * Checks if the given rights value contains all of the specified rights.
   *
   * @param rights the rights value to check
   * @param requiredRights the rights that must all be present
   * @return true if all required rights are present, false otherwise
   */
  public static boolean hasAllRights(final long rights, final long requiredRights) {
    return (rights & requiredRights) == requiredRights;
  }

  /**
   * Checks if the given rights value contains any of the specified rights.
   *
   * @param rights the rights value to check
   * @param someRights the rights to check for (at least one must be present)
   * @return true if any of the specified rights are present, false otherwise
   */
  public static boolean hasAnyRight(final long rights, final long someRights) {
    return (rights & someRights) != 0;
  }

  /**
   * Adds the specified rights to the given rights value.
   *
   * @param rights the current rights value
   * @param newRights the rights to add
   * @return the combined rights value
   */
  public static long addRights(final long rights, final long newRights) {
    return rights | newRights;
  }

  /**
   * Removes the specified rights from the given rights value.
   *
   * @param rights the current rights value
   * @param rightsToRemove the rights to remove
   * @return the rights value with specified rights removed
   */
  public static long removeRights(final long rights, final long rightsToRemove) {
    return rights & ~rightsToRemove;
  }

  /**
   * Returns the intersection of two rights values (rights present in both).
   *
   * @param rights1 the first rights value
   * @param rights2 the second rights value
   * @return the rights present in both values
   */
  public static long intersectRights(final long rights1, final long rights2) {
    return rights1 & rights2;
  }

  /**
   * Validates that the given rights value contains only defined rights.
   *
   * @param rights the rights value to validate
   * @return true if all rights are valid, false if there are undefined rights
   */
  public static boolean isValid(final long rights) {
    return (rights & ~ALL_RIGHTS) == 0;
  }

  /**
   * Returns a string representation of the given rights value.
   *
   * <p>This method returns a human-readable representation of the rights, showing which individual
   * rights are set.
   *
   * @param rights the rights value to represent
   * @return a string representation of the rights
   */
  public static String toString(final long rights) {
    if (rights == 0) {
      return "NONE";
    }

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    // File descriptor rights
    if (hasRight(rights, FD_DATASYNC)) {
      sb.append("FD_DATASYNC");
      first = false;
    }
    if (hasRight(rights, FD_READ)) {
      if (!first) sb.append("|");
      sb.append("FD_READ");
      first = false;
    }
    if (hasRight(rights, FD_SEEK)) {
      if (!first) sb.append("|");
      sb.append("FD_SEEK");
      first = false;
    }
    if (hasRight(rights, FD_WRITE)) {
      if (!first) sb.append("|");
      sb.append("FD_WRITE");
      first = false;
    }

    // Path rights (abbreviated for readability)
    if (hasRight(rights, PATH_OPEN)) {
      if (!first) sb.append("|");
      sb.append("PATH_OPEN");
      first = false;
    }
    if (hasRight(rights, FD_READDIR)) {
      if (!first) sb.append("|");
      sb.append("FD_READDIR");
      first = false;
    }

    // Socket rights
    if (hasRight(rights, SOCK_CONNECT)) {
      if (!first) sb.append("|");
      sb.append("SOCK_CONNECT");
      first = false;
    }
    if (hasRight(rights, SOCK_ACCEPT)) {
      if (!first) sb.append("|");
      sb.append("SOCK_ACCEPT");
      first = false;
    }

    // Check for undefined rights
    final long undefined = rights & ~ALL_RIGHTS;
    if (undefined != 0) {
      if (!first) sb.append("|");
      sb.append("UNDEFINED(0x").append(Long.toHexString(undefined)).append(")");
    }

    return sb.toString();
  }
}
