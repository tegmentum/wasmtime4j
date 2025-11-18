package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * WASI clock identifiers for time operations.
 *
 * <p>These identifiers specify which clock to use for time-related WASI operations.
 *
 * @since 1.0.0
 */
public enum WasiClockId {
  /** Realtime clock, measuring real (i.e., wall-clock) time. */
  REALTIME(0),

  /** Monotonic clock, measuring elapsed time. */
  MONOTONIC(1),

  /** Process CPU-time clock. */
  PROCESS_CPUTIME_ID(2),

  /** Thread CPU-time clock. */
  THREAD_CPUTIME_ID(3);

  private final int value;

  WasiClockId(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this clock ID.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets a WasiClockId by its numeric value.
   *
   * @param value the numeric value
   * @return the corresponding WasiClockId
   * @throws IllegalArgumentException if the value is invalid
   */
  public static WasiClockId fromValue(final int value) {
    for (final WasiClockId clockId : values()) {
      if (clockId.value == value) {
        return clockId;
      }
    }
    throw new IllegalArgumentException("Invalid clock ID: " + value);
  }
}
