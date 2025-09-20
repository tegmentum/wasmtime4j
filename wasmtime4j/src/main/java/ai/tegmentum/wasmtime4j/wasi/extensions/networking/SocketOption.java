package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Enumeration of socket options for configuring socket behavior.
 *
 * <p>Socket options control various aspects of network socket operation including timeouts, buffer
 * sizes, and protocol-specific behaviors. Each option has an associated value type that must be
 * used when getting or setting the option.
 *
 * @since 1.0.0
 */
public enum SocketOption {

  /** Socket receive timeout in milliseconds. Value type: Integer Default: 0 (no timeout) */
  SO_RCVTIMEO(1, Integer.class),

  /** Socket send timeout in milliseconds. Value type: Integer Default: 0 (no timeout) */
  SO_SNDTIMEO(2, Integer.class),

  /** Receive buffer size in bytes. Value type: Integer Default: system dependent */
  SO_RCVBUF(3, Integer.class),

  /** Send buffer size in bytes. Value type: Integer Default: system dependent */
  SO_SNDBUF(4, Integer.class),

  /** Allow reuse of local addresses. Value type: Boolean Default: false */
  SO_REUSEADDR(5, Boolean.class),

  /** Keep connections alive with periodic messages. Value type: Boolean Default: false */
  SO_KEEPALIVE(6, Boolean.class),

  /** Disable Nagle's algorithm for TCP sockets. Value type: Boolean Default: false */
  TCP_NODELAY(7, Boolean.class),

  /** Socket blocking mode. Value type: Boolean Default: true (blocking) */
  SO_BLOCKING(8, Boolean.class),

  /**
   * Linger on close - how long to wait for pending data. Value type: Integer (seconds, -1 =
   * disabled) Default: -1
   */
  SO_LINGER(9, Integer.class),

  /** Type of service / traffic class for IP packets. Value type: Integer Default: 0 */
  IP_TOS(10, Integer.class);

  private final int value;
  private final Class<?> valueType;

  SocketOption(final int value, final Class<?> valueType) {
    this.value = value;
    this.valueType = valueType;
  }

  /**
   * Gets the numeric value of this socket option.
   *
   * @return the numeric option value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets the expected value type for this socket option.
   *
   * @return the Java class representing the value type
   */
  public Class<?> getValueType() {
    return valueType;
  }

  /**
   * Validates that a value is compatible with this socket option.
   *
   * @param value the value to validate
   * @throws IllegalArgumentException if the value type is incompatible
   */
  public void validateValue(final Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Socket option value cannot be null");
    }
    if (!valueType.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException(
          "Invalid value type for "
              + this
              + ": expected "
              + valueType.getSimpleName()
              + ", got "
              + value.getClass().getSimpleName());
    }

    // Additional validation for specific options
    switch (this) {
      case SO_RCVTIMEO:
      case SO_SNDTIMEO:
        final int timeout = (Integer) value;
        if (timeout < 0) {
          throw new IllegalArgumentException("Timeout cannot be negative: " + timeout);
        }
        break;
      case SO_RCVBUF:
      case SO_SNDBUF:
        final int bufferSize = (Integer) value;
        if (bufferSize <= 0) {
          throw new IllegalArgumentException("Buffer size must be positive: " + bufferSize);
        }
        break;
      case SO_LINGER:
        final int linger = (Integer) value;
        if (linger < -1) {
          throw new IllegalArgumentException("Linger value must be -1 or non-negative: " + linger);
        }
        break;
      case IP_TOS:
        final int tos = (Integer) value;
        if (tos < 0 || tos > 255) {
          throw new IllegalArgumentException("TOS value must be 0-255: " + tos);
        }
        break;
      default:
        // No additional validation needed
        break;
    }
  }

  /**
   * Creates a SocketOption from its numeric value.
   *
   * @param value the numeric option value
   * @return the corresponding SocketOption
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static SocketOption fromValue(final int value) {
    for (final SocketOption option : values()) {
      if (option.value == value) {
        return option;
      }
    }
    throw new IllegalArgumentException("Unknown socket option value: " + value);
  }
}
