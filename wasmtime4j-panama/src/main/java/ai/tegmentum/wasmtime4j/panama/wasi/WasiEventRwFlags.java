package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * WASI event read/write flags for polling operations.
 *
 * <p>These flags control the behavior of file descriptor events in polling.
 *
 * @since 1.0.0
 */
public enum WasiEventRwFlags {
  /** File descriptor read/write hangup flag. */
  FD_READWRITE_HANGUP(1);

  private final int value;

  WasiEventRwFlags(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this flag.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Creates a flags mask from multiple flags.
   *
   * @param flags the flags to combine
   * @return the combined flags mask
   */
  public static int combine(final WasiEventRwFlags... flags) {
    int mask = 0;
    for (final WasiEventRwFlags flag : flags) {
      mask |= flag.value;
    }
    return mask;
  }

  /**
   * Checks if a flags mask contains a specific flag.
   *
   * @param mask the flags mask to check
   * @param flag the flag to check for
   * @return true if the mask contains the flag, false otherwise
   */
  public static boolean contains(final int mask, final WasiEventRwFlags flag) {
    return (mask & flag.value) != 0;
  }
}
