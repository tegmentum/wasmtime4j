package ai.tegmentum.wasmtime4j.panama.wasi;

/**
 * WASI event type constants for polling operations.
 *
 * <p>These constants define the types of events that can be returned by poll_oneoff.
 *
 * @since 1.0.0
 */
public enum WasiEventType {
  /** Clock event. */
  CLOCK(0),

  /** File descriptor read event. */
  FD_READ(1),

  /** File descriptor write event. */
  FD_WRITE(2);

  private final int value;

  WasiEventType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this event type.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets a WasiEventType by its numeric value.
   *
   * @param value the numeric value
   * @return the corresponding WasiEventType
   * @throws IllegalArgumentException if the value is invalid
   */
  public static WasiEventType fromValue(final int value) {
    for (final WasiEventType eventType : values()) {
      if (eventType.value == value) {
        return eventType;
      }
    }
    throw new IllegalArgumentException("Invalid event type: " + value);
  }
}
