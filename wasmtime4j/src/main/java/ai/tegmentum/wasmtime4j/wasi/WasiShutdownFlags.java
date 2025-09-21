package ai.tegmentum.wasmtime4j.wasi;

/**
 * Flags for controlling socket shutdown operations in WASI.
 *
 * <p>These flags specify which direction(s) of a socket connection should be shut down,
 * allowing for graceful connection closure.
 *
 * @since 1.0.0
 */
public final class WasiShutdownFlags {

  /** No shutdown - socket remains fully operational. */
  public static final int NONE = 0;

  /** Shut down the read (receiving) side of the connection. */
  public static final int READ = 1 << 0;

  /** Shut down the write (sending) side of the connection. */
  public static final int WRITE = 1 << 1;

  /** Shut down both read and write sides of the connection. */
  public static final int BOTH = READ | WRITE;

  /** All defined flags combined (for validation). */
  public static final int ALL_FLAGS = READ | WRITE;

  private WasiShutdownFlags() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if the given flags value includes read shutdown.
   *
   * @param flags the flags value to check
   * @return true if read shutdown is included, false otherwise
   */
  public static boolean hasRead(final int flags) {
    return (flags & READ) != 0;
  }

  /**
   * Checks if the given flags value includes write shutdown.
   *
   * @param flags the flags value to check
   * @return true if write shutdown is included, false otherwise
   */
  public static boolean hasWrite(final int flags) {
    return (flags & WRITE) != 0;
  }

  /**
   * Checks if the given flags value includes both read and write shutdown.
   *
   * @param flags the flags value to check
   * @return true if both directions are shut down, false otherwise
   */
  public static boolean hasBoth(final int flags) {
    return (flags & BOTH) == BOTH;
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
   * @param flags the flags value to represent
   * @return a string representation of the flags
   */
  public static String toString(final int flags) {
    if (flags == 0) {
      return "NONE";
    }

    if (hasBoth(flags)) {
      return "BOTH";
    }

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    if (hasRead(flags)) {
      sb.append("READ");
      first = false;
    }
    if (hasWrite(flags)) {
      if (!first) sb.append("|");
      sb.append("WRITE");
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