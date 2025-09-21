package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;
import java.util.List;

/**
 * WASI Preview 1 networking interface providing socket operations and address resolution.
 *
 * <p>This interface provides access to networking operations within the WASI sandbox, including
 * socket creation, connection management, data transfer, and address resolution. All operations
 * respect capability-based security and require appropriate network permissions.
 *
 * <p>Network operations include:
 *
 * <ul>
 *   <li>Socket creation and management (TCP, UDP)
 *   <li>Connection establishment and listening
 *   <li>Data transmission and reception
 *   <li>Address resolution and hostname lookup
 *   <li>Socket configuration and state management
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiContext context = WasiFactory.createContext();
 * WasiNetwork network = context.getNetwork();
 *
 * // Create a TCP socket
 * WasiSocket socket = network.createSocket(WasiSocketType.STREAM, WasiProtocol.TCP);
 *
 * // Connect to a server
 * WasiSocketAddress address = WasiSocketAddress.create("127.0.0.1", 8080);
 * network.connectSocket(socket, address);
 *
 * // Send data
 * ByteBuffer data = ByteBuffer.wrap("Hello, World!".getBytes());
 * long bytesSent = network.sendSocket(socket, data, WasiSendFlags.NONE);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiNetwork {

  /**
   * Creates a new socket with the specified type and protocol.
   *
   * <p>This method creates a socket for network communication. The socket type determines the
   * communication semantics (stream for TCP, datagram for UDP), while the protocol specifies
   * the exact protocol to use.
   *
   * @param type the socket type (STREAM for TCP, DGRAM for UDP)
   * @param protocol the protocol to use (TCP, UDP, etc.)
   * @return a new WasiSocket instance
   * @throws WasmException if socket creation fails or permission is denied
   * @throws IllegalArgumentException if type or protocol is null
   */
  WasiSocket createSocket(final WasiSocketType type, final WasiProtocol protocol)
      throws WasmException;

  /**
   * Binds a socket to a specific address and port.
   *
   * <p>This method associates a socket with a local address and port, making it available for
   * incoming connections (for server sockets) or outgoing connections from a specific local
   * address.
   *
   * @param socket the socket to bind
   * @param address the local address and port to bind to
   * @throws WasmException if binding fails or permission is denied
   * @throws IllegalArgumentException if socket or address is null
   * @throws IllegalStateException if socket is already bound or closed
   */
  void bindSocket(final WasiSocket socket, final WasiSocketAddress address) throws WasmException;

  /**
   * Puts a socket into listening mode for incoming connections.
   *
   * <p>This method configures a bound socket to listen for incoming connection requests. The
   * backlog parameter specifies the maximum number of pending connections that can be queued.
   *
   * @param socket the socket to put into listening mode
   * @param backlog the maximum number of pending connections (typically 1-128)
   * @throws WasmException if listen operation fails or permission is denied
   * @throws IllegalArgumentException if socket is null or backlog is negative
   * @throws IllegalStateException if socket is not bound or already listening
   */
  void listenSocket(final WasiSocket socket, final int backlog) throws WasmException;

  /**
   * Accepts an incoming connection on a listening socket.
   *
   * <p>This method blocks until an incoming connection arrives on the specified listening socket,
   * then returns a new socket representing the accepted connection. The original socket remains
   * in listening state for additional connections.
   *
   * @param socket the listening socket to accept connections on
   * @return a new WasiSocket representing the accepted connection
   * @throws WasmException if accept operation fails or permission is denied
   * @throws IllegalArgumentException if socket is null
   * @throws IllegalStateException if socket is not in listening state
   */
  WasiSocket acceptSocket(final WasiSocket socket) throws WasmException;

  /**
   * Connects a socket to a remote address.
   *
   * <p>This method establishes a connection from the socket to the specified remote address. For
   * TCP sockets, this performs the three-way handshake. For UDP sockets, this sets the default
   * destination address.
   *
   * @param socket the socket to connect
   * @param address the remote address to connect to
   * @throws WasmException if connection fails or permission is denied
   * @throws IllegalArgumentException if socket or address is null
   * @throws IllegalStateException if socket is already connected or closed
   */
  void connectSocket(final WasiSocket socket, final WasiSocketAddress address)
      throws WasmException;

  /**
   * Sends data through a connected socket.
   *
   * <p>This method sends data from the provided ByteBuffer through the socket. The socket must
   * be connected to a remote address. The buffer's position and limit determine how much data
   * is sent.
   *
   * @param socket the socket to send data through
   * @param data the buffer containing data to send
   * @param flags flags controlling the send operation
   * @return the number of bytes actually sent
   * @throws WasmException if sending fails or permission is denied
   * @throws IllegalArgumentException if socket, data, or flags is null
   * @throws IllegalStateException if socket is not connected or closed
   */
  long sendSocket(final WasiSocket socket, final ByteBuffer data, final WasiSendFlags flags)
      throws WasmException;

  /**
   * Receives data from a connected socket.
   *
   * <p>This method receives data from the socket into the provided ByteBuffer. The socket must
   * be connected to a remote address. The buffer's position and limit determine how much data
   * can be received.
   *
   * @param socket the socket to receive data from
   * @param buffer the buffer to receive data into
   * @param flags flags controlling the receive operation
   * @return the number of bytes actually received, or 0 if connection is closed
   * @throws WasmException if receiving fails or permission is denied
   * @throws IllegalArgumentException if socket, buffer, or flags is null
   * @throws IllegalStateException if socket is not connected or closed
   */
  long receiveSocket(
      final WasiSocket socket, final ByteBuffer buffer, final WasiReceiveFlags flags)
      throws WasmException;

  /**
   * Shuts down part or all of a socket connection.
   *
   * <p>This method shuts down the reading and/or writing side of a socket connection. This is
   * used for graceful connection closure and can signal the remote end that no more data will
   * be sent.
   *
   * @param socket the socket to shut down
   * @param flags flags specifying what to shut down (read, write, or both)
   * @throws WasmException if shutdown operation fails or permission is denied
   * @throws IllegalArgumentException if socket or flags is null
   * @throws IllegalStateException if socket is not connected or already closed
   */
  void shutdownSocket(final WasiSocket socket, final WasiShutdownFlags flags)
      throws WasmException;

  /**
   * Resolves a hostname to a list of socket addresses.
   *
   * <p>This method performs DNS resolution to convert a hostname into one or more IP addresses.
   * The returned list may contain both IPv4 and IPv6 addresses depending on DNS configuration
   * and availability.
   *
   * @param hostname the hostname to resolve
   * @return a list of socket addresses for the hostname
   * @throws WasmException if hostname resolution fails or permission is denied
   * @throws IllegalArgumentException if hostname is null or invalid
   */
  List<WasiSocketAddress> resolveHostname(final String hostname) throws WasmException;

  /**
   * Gets the local address of a bound or connected socket.
   *
   * <p>This method returns the local address and port that the socket is bound to. For connected
   * sockets, this is the local end of the connection.
   *
   * @param socket the socket to get the local address for
   * @return the local socket address as a string
   * @throws WasmException if getting the address fails or permission is denied
   * @throws IllegalArgumentException if socket is null
   * @throws IllegalStateException if socket is not bound or connected
   */
  String getSocketLocalAddress(final WasiSocket socket) throws WasmException;

  /**
   * Gets the remote address of a connected socket.
   *
   * <p>This method returns the remote address and port that the socket is connected to. This
   * is only valid for connected sockets.
   *
   * @param socket the socket to get the remote address for
   * @return the remote socket address as a string
   * @throws WasmException if getting the address fails or permission is denied
   * @throws IllegalArgumentException if socket is null
   * @throws IllegalStateException if socket is not connected
   */
  String getSocketRemoteAddress(final WasiSocket socket) throws WasmException;

  /**
   * Checks if networking capabilities are available and properly initialized.
   *
   * <p>This method can be used to verify that the networking subsystem is functional and that
   * the necessary permissions have been granted.
   *
   * @return true if networking is available, false otherwise
   */
  boolean isAvailable();

  /**
   * Gets networking statistics and usage information.
   *
   * <p>This method returns information about current network usage, including number of active
   * connections, data transfer statistics, and performance metrics.
   *
   * @return networking statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiNetworkStats getStats() throws WasmException;
}