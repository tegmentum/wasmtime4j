package ai.tegmentum.wasmtime4j.wasi;

/**
 * Flags for controlling socket receive operations in WASI.
 *
 * <p>These flags modify the behavior of socket receive operations, allowing applications to
 * control aspects like message boundaries, blocking behavior, and data handling.
 *
 * @since 1.0.0
 */
public final class WasiReceiveFlags {

  /** No special flags - default receive behavior. */
  public static final int NONE = 0;

  /** Receive out-of-band data. */
  public static final int OOB = 1 << 0;

  /** Peek at incoming data without removing it from the queue. */
  public static final int PEEK = 1 << 1;

  /** Wait for a complete message (for message-oriented protocols). */
  public static final int WAITALL = 1 << 2;

  /** Don't block if the operation would block. */
  public static final int DONTWAIT = 1 << 3;

  /** All defined flags combined (for validation). */
  public static final int ALL_FLAGS = OOB | PEEK | WAITALL | DONTWAIT;

  private WasiReceiveFlags() {
    // Utility class - prevent instantiation
  }

  /**
   * Checks if the given flags value contains the OOB flag.
   *
   * @param flags the flags value to check
   * @return true if OOB flag is set, false otherwise
   */
  public static boolean hasOob(final int flags) {
    return (flags & OOB) != 0;
  }

  /**
   * Checks if the given flags value contains the PEEK flag.
   *
   * @param flags the flags value to check
   * @return true if PEEK flag is set, false otherwise
   */
  public static boolean hasPeek(final int flags) {
    return (flags & PEEK) != 0;
  }

  /**
   * Checks if the given flags value contains the WAITALL flag.
   *
   * @param flags the flags value to check
   * @return true if WAITALL flag is set, false otherwise
   */
  public static boolean hasWaitAll(final int flags) {
    return (flags & WAITALL) != 0;
  }

  /**
   * Checks if the given flags value contains the DONTWAIT flag.
   *
   * @param flags the flags value to check
   * @return true if DONTWAIT flag is set, false otherwise
   */
  public static boolean hasDontWait(final int flags) {
    return (flags & DONTWAIT) != 0;
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

    final StringBuilder sb = new StringBuilder();
    boolean first = true;

    if (hasOob(flags)) {
      sb.append("OOB");
      first = false;
    }
    if (hasPeek(flags)) {
      if (!first) sb.append("|");
      sb.append("PEEK");
      first = false;
    }
    if (hasWaitAll(flags)) {
      if (!first) sb.append("|");
      sb.append("WAITALL");
      first = false;
    }
    if (hasDontWait(flags)) {
      if (!first) sb.append("|");
      sb.append("DONTWAIT");
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