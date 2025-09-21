package ai.tegmentum.wasmtime4j.wasi;

/**
 * Flags for controlling socket send operations in WASI.
 *
 * <p>These flags modify the behavior of socket send operations, allowing applications to control
 * aspects like message boundaries, blocking behavior, and urgency.
 *
 * @since 1.0.0
 */
public final class WasiSendFlags {

  /** No special flags - default send behavior. */
  public static final int NONE = 0;

  /** Send out-of-band data. */
  public static final int OOB = 1 << 0;

  /** Don't use gateway to send data. */
  public static final int DONTROUTE = 1 << 1;

  /** End of record marker for protocols that support it. */
  public static final int EOR = 1 << 2;

  /** Don't block if the operation would block. */
  public static final int DONTWAIT = 1 << 3;

  /** All defined flags combined (for validation). */
  public static final int ALL_FLAGS = OOB | DONTROUTE | EOR | DONTWAIT;

  private WasiSendFlags() {
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
   * Checks if the given flags value contains the DONTROUTE flag.
   *
   * @param flags the flags value to check
   * @return true if DONTROUTE flag is set, false otherwise
   */
  public static boolean hasDontRoute(final int flags) {
    return (flags & DONTROUTE) != 0;
  }

  /**
   * Checks if the given flags value contains the EOR flag.
   *
   * @param flags the flags value to check
   * @return true if EOR flag is set, false otherwise
   */
  public static boolean hasEor(final int flags) {
    return (flags & EOR) != 0;
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
    if (hasDontRoute(flags)) {
      if (!first) sb.append("|");
      sb.append("DONTROUTE");
      first = false;
    }
    if (hasEor(flags)) {
      if (!first) sb.append("|");
      sb.append("EOR");
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