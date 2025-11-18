package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * WASI file seek whence values for fd_seek operations.
 *
 * <p>These values specify the reference point for file seeking operations.
 *
 * @since 1.0.0
 */
public enum WasiWhence {
  /** Seek relative to start of file. */
  SET(0),

  /** Seek relative to current position. */
  CUR(1),

  /** Seek relative to end of file. */
  END(2);

  private final int value;

  WasiWhence(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this whence.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets a WasiWhence by its numeric value.
   *
   * @param value the numeric value
   * @return the corresponding WasiWhence
   * @throws IllegalArgumentException if the value is invalid
   */
  public static WasiWhence fromValue(final int value) {
    for (final WasiWhence whence : values()) {
      if (whence.value == value) {
        return whence;
      }
    }
    throw new IllegalArgumentException("Invalid whence value: " + value);
  }
}
