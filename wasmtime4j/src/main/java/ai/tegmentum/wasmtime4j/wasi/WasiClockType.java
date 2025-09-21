package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI clock types corresponding to the WASI Preview 1 clockid enum.
 *
 * <p>These clock types represent different timing sources available in the WASI environment, each
 * with different characteristics and use cases.
 *
 * @since 1.0.0
 */
public enum WasiClockType {

  /**
   * Real-time clock.
   *
   * <p>This clock represents wall-clock time and corresponds to the system's idea of the current
   * time. It is affected by system clock adjustments and may go backwards. Suitable for timestamps
   * and absolute time calculations.
   */
  REALTIME(0),

  /**
   * Monotonic clock.
   *
   * <p>This clock represents time since some unspecified starting point and is guaranteed to be
   * monotonically increasing. It is not affected by system clock adjustments and is ideal for
   * measuring elapsed time and timeouts.
   */
  MONOTONIC(1),

  /**
   * Process CPU time clock.
   *
   * <p>This clock measures the total CPU time consumed by the current process, including both user
   * and system time. It only advances when the process is actually executing and is useful for
   * performance measurement.
   */
  PROCESS_CPUTIME(2),

  /**
   * Thread CPU time clock.
   *
   * <p>This clock measures the CPU time consumed by the current thread. It only advances when the
   * current thread is actually executing and is useful for thread-specific performance measurement.
   */
  THREAD_CPUTIME(3);

  private final int value;

  WasiClockType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this clock type as defined in WASI Preview 1.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts a numeric value to the corresponding WasiClockType.
   *
   * @param value the numeric value
   * @return the corresponding WasiClockType
   * @throws IllegalArgumentException if the value doesn't correspond to a known clock type
   */
  public static WasiClockType fromValue(final int value) {
    for (final WasiClockType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown WASI clock type value: " + value);
  }

  /**
   * Checks if this clock type represents a realtime clock.
   *
   * <p>Realtime clocks provide absolute timestamps that correspond to wall-clock time.
   *
   * @return true if this is a realtime clock, false otherwise
   */
  public boolean isRealtime() {
    return this == REALTIME;
  }

  /**
   * Checks if this clock type represents a monotonic clock.
   *
   * <p>Monotonic clocks are guaranteed to be strictly increasing and are not affected by system
   * clock adjustments.
   *
   * @return true if this is a monotonic clock, false otherwise
   */
  public boolean isMonotonic() {
    return this == MONOTONIC;
  }

  /**
   * Checks if this clock type measures CPU time.
   *
   * <p>CPU time clocks measure actual execution time rather than wall-clock time.
   *
   * @return true if this is a CPU time clock, false otherwise
   */
  public boolean isCpuTime() {
    return this == PROCESS_CPUTIME || this == THREAD_CPUTIME;
  }

  /**
   * Checks if this clock type is suitable for measuring elapsed time.
   *
   * <p>Monotonic and CPU time clocks are generally suitable for measuring elapsed time, while
   * realtime clocks may be affected by system clock adjustments.
   *
   * @return true if suitable for elapsed time measurement, false otherwise
   */
  public boolean isSuitableForElapsedTime() {
    return isMonotonic() || isCpuTime();
  }

  /**
   * Checks if this clock type is suitable for absolute timestamps.
   *
   * <p>Only realtime clocks provide meaningful absolute timestamps.
   *
   * @return true if suitable for absolute timestamps, false otherwise
   */
  public boolean isSuitableForTimestamps() {
    return isRealtime();
  }

  @Override
  public String toString() {
    return name() + "(" + value + ")";
  }
}
