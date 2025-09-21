package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of socket options that can be configured in WASI.
 *
 * <p>Socket options allow fine-tuning of socket behavior including timeouts, buffer sizes,
 * and protocol-specific settings.
 *
 * @since 1.0.0
 */
public enum WasiSocketOption {

  /**
   * Socket receive timeout.
   *
   * <p>Controls how long a receive operation will wait for data before timing out.
   * Value type: Duration
   */
  RECEIVE_TIMEOUT,

  /**
   * Socket send timeout.
   *
   * <p>Controls how long a send operation will wait before timing out.
   * Value type: Duration
   */
  SEND_TIMEOUT,

  /**
   * Socket receive buffer size.
   *
   * <p>Controls the size of the receive buffer for the socket.
   * Value type: Integer
   */
  RECEIVE_BUFFER_SIZE,

  /**
   * Socket send buffer size.
   *
   * <p>Controls the size of the send buffer for the socket.
   * Value type: Integer
   */
  SEND_BUFFER_SIZE,

  /**
   * Allow reuse of local addresses.
   *
   * <p>Controls whether the socket can be bound to an address that is already in use.
   * Value type: Boolean
   */
  REUSE_ADDRESS,

  /**
   * Allow reuse of local ports.
   *
   * <p>Controls whether the socket can be bound to a port that is already in use.
   * Value type: Boolean
   */
  REUSE_PORT,

  /**
   * Keep connections alive.
   *
   * <p>Controls whether the socket will send keep-alive packets on idle connections.
   * Value type: Boolean
   */
  KEEP_ALIVE,

  /**
   * Disable Nagle's algorithm.
   *
   * <p>Controls whether the socket will immediately send small packets rather than
   * buffering them. Only applicable to TCP sockets.
   * Value type: Boolean
   */
  NO_DELAY,

  /**
   * Socket linger behavior on close.
   *
   * <p>Controls whether the socket will linger when closed to ensure data is sent.
   * Value type: Duration (null to disable lingering)
   */
  LINGER,

  /**
   * Broadcast permission.
   *
   * <p>Controls whether the socket can send broadcast packets. Only applicable to
   * UDP sockets.
   * Value type: Boolean
   */
  BROADCAST,

  /**
   * Out-of-band data inline.
   *
   * <p>Controls whether out-of-band data is received inline with normal data.
   * Value type: Boolean
   */
  OOB_INLINE,

  /**
   * IPv6 only mode.
   *
   * <p>Controls whether an IPv6 socket accepts only IPv6 connections or both
   * IPv4 and IPv6. Only applicable to IPv6 sockets.
   * Value type: Boolean
   */
  IPV6_ONLY;

  /**
   * Checks if this option is applicable to the specified socket type.
   *
   * @param socketType the socket type to check
   * @return true if the option is applicable, false otherwise
   */
  public boolean isApplicableTo(final WasiSocketType socketType) {
    switch (this) {
      case NO_DELAY:
        return socketType == WasiSocketType.STREAM;
      case BROADCAST:
        return socketType == WasiSocketType.DGRAM;
      case IPV6_ONLY:
        // Applies to IPv6 sockets (determined by address family, not socket type)
        return true;
      default:
        return true; // Most options apply to all socket types
    }
  }

  /**
   * Checks if this option is applicable to the specified protocol.
   *
   * @param protocol the protocol to check
   * @return true if the option is applicable, false otherwise
   */
  public boolean isApplicableTo(final WasiProtocol protocol) {
    switch (this) {
      case NO_DELAY:
      case KEEP_ALIVE:
        return protocol == WasiProtocol.TCP;
      case BROADCAST:
        return protocol == WasiProtocol.UDP;
      case IPV6_ONLY:
        return protocol == WasiProtocol.IPV6;
      default:
        return true; // Most options apply to all protocols
    }
  }

  /**
   * Gets the expected value type for this socket option.
   *
   * @return the class representing the expected value type
   */
  public Class<?> getValueType() {
    switch (this) {
      case RECEIVE_TIMEOUT:
      case SEND_TIMEOUT:
      case LINGER:
        return java.time.Duration.class;
      case RECEIVE_BUFFER_SIZE:
      case SEND_BUFFER_SIZE:
        return Integer.class;
      case REUSE_ADDRESS:
      case REUSE_PORT:
      case KEEP_ALIVE:
      case NO_DELAY:
      case BROADCAST:
      case OOB_INLINE:
      case IPV6_ONLY:
        return Boolean.class;
      default:
        return Object.class;
    }
  }

  /**
   * Validates that the given value is appropriate for this socket option.
   *
   * @param value the value to validate
   * @return true if the value is valid, false otherwise
   */
  public boolean isValidValue(final Object value) {
    if (value == null) {
      // Only LINGER allows null values (to disable lingering)
      return this == LINGER;
    }

    final Class<?> expectedType = getValueType();
    if (!expectedType.isInstance(value)) {
      return false;
    }

    // Additional validation for specific types
    switch (this) {
      case RECEIVE_BUFFER_SIZE:
      case SEND_BUFFER_SIZE:
        final Integer intValue = (Integer) value;
        return intValue > 0; // Buffer sizes must be positive
      default:
        return true;
    }
  }
}