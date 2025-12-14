package ai.tegmentum.wasmtime4j.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * TCP socket interface providing connection-oriented network communication.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/tcp@0.2.0
 *
 * <p>A TCP socket can be in one of the following states:
 *
 * <ul>
 *   <li>Unbound
 *   <li>Bind-in-progress
 *   <li>Bound (not listening)
 *   <li>Listen-in-progress
 *   <li>Listening
 *   <li>Connect-in-progress
 *   <li>Connected
 *   <li>Closed
 * </ul>
 */
public interface WasiTcpSocket {
  /**
   * Bind this socket to a specific network interface and/or port.
   *
   * @param network the network resource
   * @param localAddress the local address to bind to
   * @throws WasmException if binding fails
   */
  void startBind(WasiNetwork network, IpSocketAddress localAddress) throws WasmException;

  /**
   * Complete the bind operation started by startBind.
   *
   * @throws WasmException if finishing bind fails
   */
  void finishBind() throws WasmException;

  /**
   * Connect to a remote endpoint.
   *
   * @param network the network resource
   * @param remoteAddress the remote address to connect to
   * @throws WasmException if starting connection fails
   */
  void startConnect(WasiNetwork network, IpSocketAddress remoteAddress) throws WasmException;

  /**
   * Complete the connect operation started by startConnect.
   *
   * @return tuple of input and output streams for the connection
   * @throws WasmException if finishing connect fails
   */
  ConnectionStreams finishConnect() throws WasmException;

  /**
   * Start listening for incoming connections.
   *
   * @throws WasmException if starting listen fails
   */
  void startListen() throws WasmException;

  /**
   * Complete the listen operation started by startListen.
   *
   * @throws WasmException if finishing listen fails
   */
  void finishListen() throws WasmException;

  /**
   * Accept a new client socket from a listening socket.
   *
   * @return tuple of the new socket and its streams
   * @throws WasmException if accept fails
   */
  AcceptResult accept() throws WasmException;

  /**
   * Get the bound local address.
   *
   * @return the local address
   * @throws WasmException if getting address fails or socket not bound
   */
  IpSocketAddress localAddress() throws WasmException;

  /**
   * Get the remote peer address.
   *
   * @return the remote address
   * @throws WasmException if getting address fails or socket not connected
   */
  IpSocketAddress remoteAddress() throws WasmException;

  /**
   * Get the IP address family (IPv4 or IPv6).
   *
   * @return the address family
   * @throws WasmException if getting family fails
   */
  IpAddressFamily addressFamily() throws WasmException;

  /**
   * Set the size of the listen backlog.
   *
   * @param value the backlog size
   * @throws WasmException if setting value fails
   */
  void setListenBacklogSize(long value) throws WasmException;

  /**
   * Enable or disable keep-alive.
   *
   * @param value true to enable, false to disable
   * @throws WasmException if setting value fails
   */
  void setKeepAliveEnabled(boolean value) throws WasmException;

  /**
   * Set the keep-alive idle time in nanoseconds.
   *
   * @param value the idle time
   * @throws WasmException if setting value fails
   */
  void setKeepAliveIdleTime(long value) throws WasmException;

  /**
   * Set the keep-alive interval in nanoseconds.
   *
   * @param value the interval
   * @throws WasmException if setting value fails
   */
  void setKeepAliveInterval(long value) throws WasmException;

  /**
   * Set the keep-alive count.
   *
   * @param value the count
   * @throws WasmException if setting value fails
   */
  void setKeepAliveCount(int value) throws WasmException;

  /**
   * Set the hop limit (TTL for IPv4, hop limit for IPv6).
   *
   * @param value the hop limit
   * @throws WasmException if setting value fails
   */
  void setHopLimit(int value) throws WasmException;

  /**
   * Get the receive buffer size.
   *
   * @return the buffer size in bytes
   * @throws WasmException if getting value fails
   */
  long receiveBufferSize() throws WasmException;

  /**
   * Set the receive buffer size.
   *
   * @param value the buffer size in bytes
   * @throws WasmException if setting value fails
   */
  void setReceiveBufferSize(long value) throws WasmException;

  /**
   * Get the send buffer size.
   *
   * @return the buffer size in bytes
   * @throws WasmException if getting value fails
   */
  long sendBufferSize() throws WasmException;

  /**
   * Set the send buffer size.
   *
   * @param value the buffer size in bytes
   * @throws WasmException if setting value fails
   */
  void setSendBufferSize(long value) throws WasmException;

  /**
   * Create a pollable for this socket.
   *
   * @return a pollable resource
   * @throws WasmException if subscribe fails
   */
  WasiPollable subscribe() throws WasmException;

  /**
   * Shut down the socket's send and/or receive operations.
   *
   * @param shutdownType the type of shutdown
   * @throws WasmException if shutdown fails
   */
  void shutdown(ShutdownType shutdownType) throws WasmException;

  /**
   * Close and dispose of this socket resource.
   *
   * @throws WasmException if closing fails
   */
  void close() throws WasmException;

  /** Result of a successful connection containing input and output streams. */
  @SuppressFBWarnings(
      value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
      justification =
          "I/O streams are intentionally shared resources; copying would break semantics")
  final class ConnectionStreams {
    private final WasiInputStream inputStream;
    private final WasiOutputStream outputStream;

    /**
     * Creates a ConnectionStreams result.
     *
     * @param inputStream the input stream for reading
     * @param outputStream the output stream for writing
     */
    public ConnectionStreams(
        final WasiInputStream inputStream, final WasiOutputStream outputStream) {
      if (inputStream == null) {
        throw new IllegalArgumentException("inputStream cannot be null");
      }
      if (outputStream == null) {
        throw new IllegalArgumentException("outputStream cannot be null");
      }
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    public WasiInputStream getInputStream() {
      return inputStream;
    }

    public WasiOutputStream getOutputStream() {
      return outputStream;
    }
  }

  /** Result of accepting a connection containing the new socket and its streams. */
  @SuppressFBWarnings(
      value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
      justification =
          "Socket and I/O streams are intentionally shared resources; copying would break"
              + " semantics")
  final class AcceptResult {
    private final WasiTcpSocket socket;
    private final WasiInputStream inputStream;
    private final WasiOutputStream outputStream;

    /**
     * Creates an AcceptResult.
     *
     * @param socket the accepted socket
     * @param inputStream the input stream for reading
     * @param outputStream the output stream for writing
     */
    public AcceptResult(
        final WasiTcpSocket socket,
        final WasiInputStream inputStream,
        final WasiOutputStream outputStream) {
      if (socket == null) {
        throw new IllegalArgumentException("socket cannot be null");
      }
      if (inputStream == null) {
        throw new IllegalArgumentException("inputStream cannot be null");
      }
      if (outputStream == null) {
        throw new IllegalArgumentException("outputStream cannot be null");
      }
      this.socket = socket;
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    public WasiTcpSocket getSocket() {
      return socket;
    }

    public WasiInputStream getInputStream() {
      return inputStream;
    }

    public WasiOutputStream getOutputStream() {
      return outputStream;
    }
  }

  /** Socket shutdown types. */
  enum ShutdownType {
    /** Disable further receive operations. */
    RECEIVE,
    /** Disable further send operations. */
    SEND,
    /** Disable both receive and send operations. */
    BOTH
  }
}
