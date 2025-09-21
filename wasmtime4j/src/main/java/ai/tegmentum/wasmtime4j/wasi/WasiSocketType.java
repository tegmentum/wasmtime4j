package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of WASI socket types corresponding to common socket types.
 *
 * <p>Socket types define the communication semantics for network sockets, determining how data
 * is transmitted and received.
 *
 * @since 1.0.0
 */
public enum WasiSocketType {

  /**
   * Stream socket (TCP).
   *
   * <p>Stream sockets provide reliable, ordered, and error-checked delivery of data between
   * applications. They use the Transmission Control Protocol (TCP) and are connection-oriented.
   */
  STREAM(1),

  /**
   * Datagram socket (UDP).
   *
   * <p>Datagram sockets provide connectionless, unreliable communication. They use the User
   * Datagram Protocol (UDP) and are suitable for applications that can tolerate data loss but
   * require low latency.
   */
  DGRAM(2),

  /**
   * Raw socket.
   *
   * <p>Raw sockets provide direct access to the underlying network protocol. They are typically
   * used for implementing custom protocols or network diagnostic tools. Raw sockets usually
   * require special privileges.
   */
  RAW(3),

  /**
   * Sequenced packet socket.
   *
   * <p>Sequenced packet sockets provide reliable, ordered delivery like stream sockets but
   * preserve message boundaries like datagram sockets. Not commonly supported on all platforms.
   */
  SEQPACKET(5);

  private final int value;

  WasiSocketType(final int value) {
    this.value = value;
  }

  /**
   * Gets the numeric value of this socket type.
   *
   * @return the numeric value
   */
  public int getValue() {
    return value;
  }

  /**
   * Converts a numeric value to the corresponding WasiSocketType.
   *
   * @param value the numeric value
   * @return the corresponding WasiSocketType
   * @throws IllegalArgumentException if the value doesn't correspond to a known socket type
   */
  public static WasiSocketType fromValue(final int value) {
    for (final WasiSocketType type : values()) {
      if (type.value == value) {
        return type;
      }
    }
    throw new IllegalArgumentException("Unknown WASI socket type value: " + value);
  }

  /**
   * Checks if this socket type is connection-oriented.
   *
   * <p>Connection-oriented socket types (like STREAM) require establishing a connection before
   * data can be transmitted.
   *
   * @return true if this is a connection-oriented socket type, false otherwise
   */
  public boolean isConnectionOriented() {
    return this == STREAM || this == SEQPACKET;
  }

  /**
   * Checks if this socket type is connectionless.
   *
   * <p>Connectionless socket types (like DGRAM) can send data without establishing a connection.
   *
   * @return true if this is a connectionless socket type, false otherwise
   */
  public boolean isConnectionless() {
    return this == DGRAM || this == RAW;
  }

  /**
   * Checks if this socket type is reliable.
   *
   * <p>Reliable socket types guarantee delivery and ordering of data.
   *
   * @return true if this is a reliable socket type, false otherwise
   */
  public boolean isReliable() {
    return this == STREAM || this == SEQPACKET;
  }

  /**
   * Checks if this socket type preserves message boundaries.
   *
   * <p>Socket types that preserve message boundaries maintain the boundaries between individual
   * send operations.
   *
   * @return true if this socket type preserves message boundaries, false otherwise
   */
  public boolean preservesMessageBoundaries() {
    return this == DGRAM || this == RAW || this == SEQPACKET;
  }

  /**
   * Checks if this socket type typically requires special privileges.
   *
   * <p>Some socket types (like RAW) typically require administrative privileges to create.
   *
   * @return true if this socket type typically requires special privileges, false otherwise
   */
  public boolean requiresPrivileges() {
    return this == RAW;
  }

  @Override
  public String toString() {
    return name() + "(" + value + ")";
  }
}