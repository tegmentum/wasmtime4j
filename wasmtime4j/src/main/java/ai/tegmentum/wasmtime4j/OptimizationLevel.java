package ai.tegmentum.wasmtime4j;

/**
 * Optimization levels for WebAssembly compilation.
 *
 * <p>These levels control the trade-off between compilation time and runtime performance. Higher
 * optimization levels may take longer to compile but produce faster code.
 *
 * @since 1.0.0
 */
public enum OptimizationLevel {
  /** No optimization - fastest compilation, slowest execution. */
  NONE(0),

  /** Optimize for runtime speed - slower compilation, fastest execution. */
  SPEED(1),

  /** Optimize for code size - moderate compilation time, smaller code size. */
  SIZE(2),

  /** Optimize for both speed and size - balanced approach. */
  SPEED_AND_SIZE(2);

  private final int value;

  OptimizationLevel(final int value) {
    this.value = value;
  }

  /**
   * Gets the native value for this optimization level.
   *
   * @return the native optimization level value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets the optimization level from a native value.
   *
   * @param value the native optimization level value
   * @return the corresponding OptimizationLevel
   * @throws IllegalArgumentException if the value is not valid
   */
  public static OptimizationLevel fromValue(final int value) {
    switch (value) {
      case 0:
        return NONE;
      case 1:
        return SPEED;
      case 2:
        return SIZE; // Note: SPEED_AND_SIZE also maps to 2
      default:
        throw new IllegalArgumentException("Invalid optimization level: " + value);
    }
  }
}
