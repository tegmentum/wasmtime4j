package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI network protocols.
 *
 * <p>Network protocols define the specific communication protocol to use for network sockets.
 * The protocol works in combination with the socket type to determine the exact behavior.
 *
 * @since 1.0.0
 */
public enum WasiProtocol {

  /**
   * Internet Protocol (IP).
   *
   * <p>This is the base Internet Protocol. When used with raw sockets, it provides access to
   * the IP layer.
   */
  IP(0),

  /**
   * Internet Control Message Protocol (ICMP).
   *
   * <p>ICMP is used for error reporting and diagnostic functions in IP networks. It's commonly
   * used for ping and traceroute operations.
   */
  ICMP(1),

  /**
   * Transmission Control Protocol (TCP).
   *
   * <p>TCP provides reliable, ordered, and error-checked delivery of data. It's typically used
   * with stream sockets for applications that require guaranteed delivery.
   */
  TCP(6),

  /**
   * User Datagram Protocol (UDP).
   *
   * <p>UDP provides connectionless, unreliable communication with minimal protocol overhead.
   * It's typically used with datagram sockets for applications that prioritize speed over
   * reliability.
   */
  UDP(17),

  /**
   * Internet Protocol version 6 (IPv6).
   *
   * <p>This represents the IPv6 protocol when used with raw sockets.
   */
  IPV6(41),

  /**
   * Stream Control Transmission Protocol (SCTP).
   *
   * <p>SCTP provides reliable, ordered delivery with message-oriented semantics. It combines
   * features of TCP and UDP and supports multi-homing and multi-streaming.
   */
  SCTP(132);

  private final int value;

  WasiProtocol(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this protocol (IP protocol number).
   *
   * @return the protocol number
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts a numeric value to the corresponding WasiProtocol.
   *
   * @param value the numeric value (IP protocol number)
   * @return the corresponding WasiProtocol
   * @throws IllegalArgumentException if the value doesn't correspond to a known protocol
   */
  public static WasiProtocol fromValue(final int value) {
    for (final WasiProtocol protocol : values()) {
      if (protocol.value == value) {
        return protocol;
      }
    }
    throw new IllegalArgumentException("Unknown WASI protocol value: " + value);
  }

  /**
   * Checks if this protocol is connection-oriented.
   *
   * <p>Connection-oriented protocols require establishing a connection before data transmission.
   *
   * @return true if this is a connection-oriented protocol, false otherwise
   */
  public boolean isConnectionOriented() {
    return this == TCP || this == SCTP;
  }

  /**
   * Checks if this protocol is connectionless.
   *
   * <p>Connectionless protocols can send data without establishing a connection.
   *
   * @return true if this is a connectionless protocol, false otherwise
   */
  public boolean isConnectionless() {
    return this == UDP || this == ICMP || this == IP || this == IPV6;
  }

  /**
   * Checks if this protocol is reliable.
   *
   * <p>Reliable protocols guarantee delivery and ordering of data.
   *
   * @return true if this is a reliable protocol, false otherwise
   */
  public boolean isReliable() {
    return this == TCP || this == SCTP;
  }

  /**
   * Checks if this protocol is suitable for real-time applications.
   *
   * <p>Some protocols have lower overhead and are more suitable for real-time applications
   * where low latency is important.
   *
   * @return true if suitable for real-time applications, false otherwise
   */
  public boolean isSuitableForRealTime() {
    return this == UDP || this == SCTP;
  }

  /**
   * Gets the typical socket type used with this protocol.
   *
   * <p>This returns the socket type that is most commonly used with this protocol.
   *
   * @return the typical socket type for this protocol
   */
  public WasiSocketType getTypicalSocketType() {
    switch (this) {
      case TCP:
        return WasiSocketType.STREAM;
      case UDP:
      case ICMP:
        return WasiSocketType.DGRAM;
      case SCTP:
        return WasiSocketType.SEQPACKET;
      case IP:
      case IPV6:
        return WasiSocketType.RAW;
      default:
        return WasiSocketType.RAW;
    }
  }

  /**
   * Checks if this protocol is compatible with the specified socket type.
   *
   * @param socketType the socket type to check compatibility with
   * @return true if compatible, false otherwise
   */
  public boolean isCompatibleWith(final WasiSocketType socketType) {
    switch (this) {
      case TCP:
        return socketType == WasiSocketType.STREAM;
      case UDP:
        return socketType == WasiSocketType.DGRAM;
      case SCTP:
        return socketType == WasiSocketType.STREAM || socketType == WasiSocketType.SEQPACKET;
      case ICMP:
        return socketType == WasiSocketType.DGRAM || socketType == WasiSocketType.RAW;
      case IP:
      case IPV6:
        return socketType == WasiSocketType.RAW;
      default:
        return true; // Unknown protocols are assumed compatible
    }
  }

  @Override
  public String toString() {
    return name() + "(" + value + ")";
  }
}