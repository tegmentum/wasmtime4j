package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

/**
 * Enumeration of socket types for WASI networking operations.
 *
 * <p>Socket types define the communication semantics and protocol characteristics
 * of network sockets. Each type corresponds to different networking protocols
 * and use cases.
 *
 * @since 1.0.0
 */
public enum SocketType {

  /**
   * Stream socket type (TCP).
   *
   * <p>Provides reliable, connection-oriented, full-duplex communication streams.
   * Data is delivered in order and without duplication. Commonly used for TCP
   * connections and applications requiring reliable data delivery.
   */
  STREAM(1),

  /**
   * Datagram socket type (UDP).
   *
   * <p>Provides connectionless, unreliable message delivery. Messages may be
   * lost, duplicated, or delivered out of order. Commonly used for UDP
   * communications and applications where speed is more important than reliability.
   */
  DGRAM(2),

  /**
   * Raw socket type.
   *
   * <p>Provides direct access to network protocols below the transport layer.
   * Typically requires elevated privileges and is used for network protocol
   * implementation and network diagnostic tools.
   */
  RAW(3);

  private final int value;

  SocketType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this socket type.
   *
   * @return the numeric socket type value
   */
  public int getValue() {
    return value;
  }

  /**
   * Creates a SocketType from its numeric value.
   *
   * @param value the numeric socket type value
   * @return the corresponding SocketType
   * @throws IllegalArgumentException if the value is not recognized
   */
  public static SocketType fromValue(final int value) {
    for (final SocketType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown socket type value: " + value);
  }
}