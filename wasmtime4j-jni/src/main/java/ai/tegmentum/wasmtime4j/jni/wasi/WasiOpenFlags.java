package ai.tegmentum.wasmtime4j.jni.wasi;

/**
 * WASI file open flags for path_open operations.
 *
 * <p>These flags control how files are opened and created through WASI path operations.
 *
 * @since 1.0.0
 */
public enum WasiOpenFlags {
  /** Create file if it does not exist. */
  CREAT(1),

  /** Fail if not a directory. */
  DIRECTORY(2),

  /** Fail if file already exists. */
  EXCL(4),

  /** Truncate file to size 0. */
  TRUNC(8);

  private final int value;

  WasiOpenFlags(final int value) {
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
  public static int combine(final WasiOpenFlags... flags) {
    int mask = 0;
    for (final WasiOpenFlags flag : flags) {
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
  public static boolean contains(final int mask, final WasiOpenFlags flag) {
    return (mask & flag.value) != 0;
  }
}
