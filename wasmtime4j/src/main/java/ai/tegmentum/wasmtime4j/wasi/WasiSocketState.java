package ai.tegmentum.wasmtime4j.wasi;

/**
 * Enumeration of socket states in the WASI environment.
 *
 * <p>Socket states represent the current operational state of a network socket, tracking its
 * lifecycle from creation through closure.
 *
 * @since 1.0.0
 */
public enum WasiSocketState {

  /**
   * Socket has been created but not yet bound to an address.
   *
   * <p>This is the initial state when a socket is first created. The socket can be bound to
   * a local address or connected to a remote address from this state.
   */
  UNBOUND,

  /**
   * Socket has been bound to a local address.
   *
   * <p>The socket is associated with a specific local address and port. For server sockets,
   * this is typically followed by putting the socket into listening state.
   */
  BOUND,

  /**
   * Socket is listening for incoming connections.
   *
   * <p>This state is only applicable to connection-oriented sockets (like TCP). The socket
   * is ready to accept incoming connection requests.
   */
  LISTENING,

  /**
   * Socket is connected to a remote address.
   *
   * <p>For connection-oriented sockets, this means a connection has been established with
   * a remote peer. For connectionless sockets, this means a default destination has been set.
   */
  CONNECTED,

  /**
   * Socket is in the process of establishing a connection.
   *
   * <p>This is a transitional state during non-blocking connection establishment. The socket
   * is attempting to connect but the connection is not yet complete.
   */
  CONNECTING,

  /**
   * Socket connection is being closed.
   *
   * <p>This is a transitional state during graceful connection closure. The socket may still
   * be able to receive data but cannot send new data.
   */
  CLOSING,

  /**
   * Socket has been closed and is no longer usable.
   *
   * <p>This is the final state of a socket. No operations can be performed on a closed socket.
   */
  CLOSED;

  /**
   * Checks if the socket can be bound to an address in this state.
   *
   * @return true if binding is allowed, false otherwise
   */
  public boolean canBind() {
    return this == UNBOUND;
  }

  /**
   * Checks if the socket can be put into listening mode in this state.
   *
   * @return true if listening is allowed, false otherwise
   */
  public boolean canListen() {
    return this == BOUND;
  }

  /**
   * Checks if the socket can connect to a remote address in this state.
   *
   * @return true if connecting is allowed, false otherwise
   */
  public boolean canConnect() {
    return this == UNBOUND || this == BOUND;
  }

  /**
   * Checks if the socket can accept incoming connections in this state.
   *
   * @return true if accepting is allowed, false otherwise
   */
  public boolean canAccept() {
    return this == LISTENING;
  }

  /**
   * Checks if the socket can send data in this state.
   *
   * @return true if sending is allowed, false otherwise
   */
  public boolean canSend() {
    return this == CONNECTED;
  }

  /**
   * Checks if the socket can receive data in this state.
   *
   * @return true if receiving is allowed, false otherwise
   */
  public boolean canReceive() {
    return this == CONNECTED || this == CLOSING;
  }

  /**
   * Checks if the socket is in an active state.
   *
   * <p>Active states are those where the socket can perform operations.
   *
   * @return true if the socket is active, false otherwise
   */
  public boolean isActive() {
    return this != CLOSED;
  }

  /**
   * Checks if the socket is in a transitional state.
   *
   * <p>Transitional states are temporary states during connection establishment or closure.
   *
   * @return true if the socket is in a transitional state, false otherwise
   */
  public boolean isTransitional() {
    return this == CONNECTING || this == CLOSING;
  }

  /**
   * Checks if the socket is ready for I/O operations.
   *
   * @return true if the socket is ready for I/O, false otherwise
   */
  public boolean isReadyForIO() {
    return this == CONNECTED;
  }
}