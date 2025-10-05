package ai.tegmentum.wasmtime4j.wasi;

/**
 * Flags for opening files in the WASI filesystem.
 *
 * <p>WasiOpenFlags define how a file should be opened and what operations are intended. These flags
 * control the behavior of file opening operations and must be compatible with the rights requested
 * for the file handle.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Open for reading only
 * WasiOpenFlags readFlags = WasiOpenFlags.READ;
 *
 * // Open for writing with creation
 * WasiOpenFlags writeFlags = WasiOpenFlags.WRITE.combine(WasiOpenFlags.CREATE);
 *
 * // Open for append operations
 * WasiOpenFlags appendFlags = WasiOpenFlags.WRITE.combine(WasiOpenFlags.APPEND);
 * }</pre>
 *
 * @since 1.0.0
 */
public enum WasiOpenFlags {

  /** Open the file for reading. */
  READ(0x01),

  /** Open the file for writing. */
  WRITE(0x02),

  /** Create the file if it does not exist. */
  CREATE(0x04),

  /** Fail if the file already exists (used with CREATE). */
  EXCLUSIVE(0x08),

  /** Truncate the file to zero length if it exists. */
  TRUNCATE(0x10),

  /** Open the file for append operations (writes go to end of file). */
  APPEND(0x20),

  /** Open the file in synchronous mode (data is written immediately to storage). */
  SYNC(0x40),

  /** Open the file with data synchronization (data but not metadata is synced). */
  DSYNC(0x80),

  /** Open the file in non-blocking mode. */
  NONBLOCK(0x100),

  /** Follow symbolic links when opening. */
  FOLLOW_SYMLINKS(0x200),

  /** Do not follow symbolic links when opening. */
  NO_FOLLOW_SYMLINKS(0x400);

  private final int value;

  WasiOpenFlags(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this flag.
   *
   * @return the flag value
   */
  public int getValue() {
    return value;
  }

  /**
   * Combines this flag with another flag.
   *
   * @param other the other flag to combine with
   * @return a new combined flags object
   * @throws IllegalArgumentException if other is null
   */
  public WasiOpenFlagsSet combine(final WasiOpenFlags other) {
    if (other == null) {
      throw new IllegalArgumentException("Other flag cannot be null");
    }
    return new WasiOpenFlagsSet(this.value | other.value);
  }

  /**
   * Creates a flags set from multiple individual flags.
   *
   * @param flags the flags to combine
   * @return a new flags set containing all the specified flags
   * @throws IllegalArgumentException if flags array is null
   */
  public static WasiOpenFlagsSet of(final WasiOpenFlags... flags) {
    if (flags == null) {
      throw new IllegalArgumentException("Flags array cannot be null");
    }

    int combined = 0;
    for (final WasiOpenFlags flag : flags) {
      if (flag != null) {
        combined |= flag.value;
      }
    }
    return new WasiOpenFlagsSet(combined);
  }

  /**
   * Checks if this flag is set in the given flags value.
   *
   * @param flags the flags value to check
   * @return true if this flag is set
   */
  public boolean isSet(final int flags) {
    return (flags & this.value) != 0;
  }

  /** A set of combined open flags. */
  public static final class WasiOpenFlagsSet {
    private final int value;

    WasiOpenFlagsSet(final int value) {
      this.value = value;
    }

    /**
     * Gets the combined numeric value of all flags in this set.
     *
     * @return the combined flags value
     */
    public int getValue() {
      return value;
    }

    /**
     * Combines this flags set with another flag.
     *
     * @param flag the flag to add to this set
     * @return a new flags set with the additional flag
     * @throws IllegalArgumentException if flag is null
     */
    public WasiOpenFlagsSet combine(final WasiOpenFlags flag) {
      if (flag == null) {
        throw new IllegalArgumentException("Flag cannot be null");
      }
      return new WasiOpenFlagsSet(this.value | flag.value);
    }

    /**
     * Checks if the specified flag is set in this flags set.
     *
     * @param flag the flag to check for
     * @return true if the flag is set
     * @throws IllegalArgumentException if flag is null
     */
    public boolean contains(final WasiOpenFlags flag) {
      if (flag == null) {
        throw new IllegalArgumentException("Flag cannot be null");
      }
      return flag.isSet(this.value);
    }

    /**
     * Checks if this flags set is empty (no flags set).
     *
     * @return true if no flags are set
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
      final WasiOpenFlagsSet that = (WasiOpenFlagsSet) obj;
      return value == that.value;
    }

    @Override
    public int hashCode() {
      return Integer.hashCode(value);
    }

    @Override
    public String toString() {
      if (value == 0) {
        return "WasiOpenFlagsSet[]";
      }

      final StringBuilder sb = new StringBuilder("WasiOpenFlagsSet[");
      boolean first = true;
      for (final WasiOpenFlags flag : WasiOpenFlags.values()) {
        if (flag.isSet(value)) {
          if (!first) {
            sb.append(", ");
          }
          sb.append(flag.name());
          first = false;
        }
      }
      sb.append("]");
      return sb.toString();
    }
  }
}
