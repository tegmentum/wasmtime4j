package ai.tegmentum.wasmtime4j.wasi;

/**
 * Flags for controlling how files are opened in WASI filesystem operations.
 *
 * <p>These flags correspond to the WASI Preview 1 oflags and control various aspects of file
 * opening behavior such as creation, truncation, and synchronization modes.
 *
 * <p>Flags can be combined using bitwise OR operations to specify multiple behaviors.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Open file for writing, create if it doesn't exist, truncate if it does
 * WasiOpenFlags flags = WasiOpenFlags.CREATE | WasiOpenFlags.TRUNCATE;
 * WasiFileHandle handle = filesystem.openFile("/tmp/output.txt", flags, rights);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WasiOpenFlags {

  /** Create the file if it does not exist. */
  public static final int CREATE = 1 << 0;

  /** Fail if the file is not a directory. */
  public static final int DIRECTORY = 1 << 1;

  /** Fail if the file already exists. */
  public static final int EXCLUSIVE = 1 << 2;

  /** Truncate the file to size 0 if it exists. */
  public static final int TRUNCATE = 1 << 3;

  /** Request synchronous I/O operations. */
  public static final int SYNC = 1 << 4;

  /** Request synchronous data I/O operations (metadata may be asynchronous). */
  public static final int DSYNC = 1 << 5;

  /** Request synchronous read operations. */
  public static final int RSYNC = 1 << 6;

  /** Open in append mode (writes always go to end of file). */
  public static final int APPEND = 1 << 7;

  /** Open in non-blocking mode. */
  public static final int NONBLOCK = 1 << 8;

  /** Follow symbolic links when opening. */
  public static final int FOLLOW = 1 << 9;

  /** Do not follow symbolic links when opening. */
  public static final int NOFOLLOW = 1 << 10;

  /** All defined flags combined (for validation). */
  public static final int ALL_FLAGS =
      CREATE
          | DIRECTORY
          | EXCLUSIVE
          | TRUNCATE
          | SYNC
          | DSYNC
          | RSYNC
          | APPEND
          | NONBLOCK
          | FOLLOW
          | NOFOLLOW;

  private WasiOpenFlags() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if the given flags value contains the CREATE flag.
   *
   * @param flags the flags value to check
   * @return true if CREATE flag is set, false otherwise
   */
  public static boolean hasCreate(final int flags) {
    return (flags & CREATE) != 0;
  }

  /**
   * Checks if the given flags value contains the DIRECTORY flag.
   *
   * @param flags the flags value to check
   * @return true if DIRECTORY flag is set, false otherwise
   */
  public static boolean hasDirectory(final int flags) {
    return (flags & DIRECTORY) != 0;
  }

  /**
   * Checks if the given flags value contains the EXCLUSIVE flag.
   *
   * @param flags the flags value to check
   * @return true if EXCLUSIVE flag is set, false otherwise
   */
  public static boolean hasExclusive(final int flags) {
    return (flags & EXCLUSIVE) != 0;
  }

  /**
   * Checks if the given flags value contains the TRUNCATE flag.
   *
   * @param flags the flags value to check
   * @return true if TRUNCATE flag is set, false otherwise
   */
  public static boolean hasTruncate(final int flags) {
    return (flags & TRUNCATE) != 0;
  }

  /**
   * Checks if the given flags value contains the SYNC flag.
   *
   * @param flags the flags value to check
   * @return true if SYNC flag is set, false otherwise
   */
  public static boolean hasSync(final int flags) {
    return (flags & SYNC) != 0;
  }

  /**
   * Checks if the given flags value contains the DSYNC flag.
   *
   * @param flags the flags value to check
   * @return true if DSYNC flag is set, false otherwise
   */
  public static boolean hasDsync(final int flags) {
    return (flags & DSYNC) != 0;
  }

  /**
   * Checks if the given flags value contains the RSYNC flag.
   *
   * @param flags the flags value to check
   * @return true if RSYNC flag is set, false otherwise
   */
  public static boolean hasRsync(final int flags) {
    return (flags & RSYNC) != 0;
  }

  /**
   * Checks if the given flags value contains the APPEND flag.
   *
   * @param flags the flags value to check
   * @return true if APPEND flag is set, false otherwise
   */
  public static boolean hasAppend(final int flags) {
    return (flags & APPEND) != 0;
  }

  /**
   * Checks if the given flags value contains the NONBLOCK flag.
   *
   * @param flags the flags value to check
   * @return true if NONBLOCK flag is set, false otherwise
   */
  public static boolean hasNonblock(final int flags) {
    return (flags & NONBLOCK) != 0;
  }

  /**
   * Checks if the given flags value contains the FOLLOW flag.
   *
   * @param flags the flags value to check
   * @return true if FOLLOW flag is set, false otherwise
   */
  public static boolean hasFollow(final int flags) {
    return (flags & FOLLOW) != 0;
  }

  /**
   * Checks if the given flags value contains the NOFOLLOW flag.
   *
   * @param flags the flags value to check
   * @return true if NOFOLLOW flag is set, false otherwise
   */
  public static boolean hasNofollow(final int flags) {
    return (flags & NOFOLLOW) != 0;
  }

  /**
   * Validates that the given flags value contains only defined flags.
   *
   * @param flags the flags value to validate
   * @return true if all flags are valid, false if there are undefined flags
   */
  public static boolean isValid(final int flags) {
    return (flags & ~ALL_FLAGS) == 0;
  }

  /**
   * Returns a string representation of the given flags value.
   *
   * <p>This method returns a human-readable representation of the flags, showing which individual
   * flags are set.
   *
   * @param flags the flags value to represent
   * @return a string representation of the flags
   */
  public static String toString(final int flags) {
    if (flags == 0) {
      return "NONE";
    }

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    if (hasCreate(flags)) {
      sb.append("CREATE");
      first = false;
    }
    if (hasDirectory(flags)) {
      if (!first) sb.append("|");
      sb.append("DIRECTORY");
      first = false;
    }
    if (hasExclusive(flags)) {
      if (!first) sb.append("|");
      sb.append("EXCLUSIVE");
      first = false;
    }
    if (hasTruncate(flags)) {
      if (!first) sb.append("|");
      sb.append("TRUNCATE");
      first = false;
    }
    if (hasSync(flags)) {
      if (!first) sb.append("|");
      sb.append("SYNC");
      first = false;
    }
    if (hasDsync(flags)) {
      if (!first) sb.append("|");
      sb.append("DSYNC");
      first = false;
    }
    if (hasRsync(flags)) {
      if (!first) sb.append("|");
      sb.append("RSYNC");
      first = false;
    }
    if (hasAppend(flags)) {
      if (!first) sb.append("|");
      sb.append("APPEND");
      first = false;
    }
    if (hasNonblock(flags)) {
      if (!first) sb.append("|");
      sb.append("NONBLOCK");
      first = false;
    }
    if (hasFollow(flags)) {
      if (!first) sb.append("|");
      sb.append("FOLLOW");
      first = false;
    }
    if (hasNofollow(flags)) {
      if (!first) sb.append("|");
      sb.append("NOFOLLOW");
    }

    // Check for undefined flags
    final int undefined = flags & ~ALL_FLAGS;
    if (undefined != 0) {
      if (!first) sb.append("|");
      sb.append("UNDEFINED(0x").append(Integer.toHexString(undefined)).append(")");
    }

    return sb.toString();
  }
}