package ai.tegmentum.wasmtime4j.wasi.extensions;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.extensions.networking.SocketFamily;
import ai.tegmentum.wasmtime4j.wasi.extensions.networking.SocketType;
import ai.tegmentum.wasmtime4j.wasi.extensions.networking.WasiHttpClient;
import ai.tegmentum.wasmtime4j.wasi.extensions.networking.WasiHttpServer;
import ai.tegmentum.wasmtime4j.wasi.extensions.networking.WasiSocket;
import ai.tegmentum.wasmtime4j.wasi.extensions.networking.WasiSocketAddress;

/**
 * WASI Networking extension interface providing network programming capabilities.
 *
 * <p>This interface extends WASI with comprehensive networking support including:
 * <ul>
 *   <li>Socket operations (TCP/UDP)
 *   <li>HTTP client and server capabilities
 *   <li>Network configuration and management
 *   <li>Secure connection support
 * </ul>
 *
 * <p>All networking operations respect capability-based security and require appropriate
 * permissions to be granted through the WASI security policy.
 *
 * <p>Example usage:
 * <pre>{@code
 * try (WasiContext context = WasiFactory.createContext()) {
 *     WasiNetworking networking = context.getNetworking();
 *
 *     // Create a TCP socket
 *     WasiSocket socket = networking.createSocket(SocketType.STREAM, SocketFamily.INET);
 *
 *     // Connect to a server
 *     WasiSocketAddress address = WasiSocketAddress.create("127.0.0.1", 8080);
 *     socket.connect(address);
 *
 *     // Use the socket...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiNetworking {

  /**
   * Creates a new socket with the specified type and address family.
   *
   * <p>This method creates a socket for network communication. The socket type determines
   * the communication semantics (stream for TCP, datagram for UDP), while the family
   * determines the address family (IPv4, IPv6, Unix domain sockets).
   *
   * @param type the socket type (STREAM for TCP, DGRAM for UDP)
   * @param family the address family (INET for IPv4, INET6 for IPv6, UNIX for domain sockets)
   * @return a new WasiSocket instance
   * @throws WasmException if socket creation fails or permission is denied
   * @throws IllegalArgumentException if type or family is null
   */
  WasiSocket createSocket(final SocketType type, final SocketFamily family) throws WasmException;

  /**
   * Binds a socket to a specific address and port.
   *
   * <p>This method associates a socket with a local address and port, making it available
   * for incoming connections (for server sockets) or outgoing connections from a specific
   * local address.
   *
   * @param socket the socket to bind
   * @param address the local address and port to bind to
   * @throws WasmException if binding fails or permission is denied
   * @throws IllegalArgumentException if socket or address is null
   * @throws IllegalStateException if socket is already bound or closed
   */
  void bind(final WasiSocket socket, final WasiSocketAddress address) throws WasmException;

  /**
   * Puts a socket into listening mode for incoming connections.
   *
   * <p>This method configures a bound socket to listen for incoming connection requests.
   * The backlog parameter specifies the maximum number of pending connections that can
   * be queued.
   *
   * @param socket the socket to put into listening mode
   * @param backlog the maximum number of pending connections (typically 1-128)
   * @throws WasmException if listen operation fails or permission is denied
   * @throws IllegalArgumentException if socket is null or backlog is negative
   * @throws IllegalStateException if socket is not bound or already listening
   */
  void listen(final WasiSocket socket, final int backlog) throws WasmException;

  /**
   * Accepts an incoming connection on a listening socket.
   *
   * <p>This method blocks until an incoming connection arrives on the specified listening
   * socket, then returns a new socket representing the accepted connection. The original
   * socket remains in listening state for additional connections.
   *
   * @param socket the listening socket to accept connections on
   * @return a new WasiSocket representing the accepted connection
   * @throws WasmException if accept operation fails or permission is denied
   * @throws IllegalArgumentException if socket is null
   * @throws IllegalStateException if socket is not in listening state
   */
  WasiSocket accept(final WasiSocket socket) throws WasmException;

  /**
   * Creates a new HTTP client for making HTTP requests.
   *
   * <p>The HTTP client provides high-level HTTP functionality built on top of the
   * socket layer, supporting features like connection pooling, redirects, and
   * common HTTP methods.
   *
   * @return a new WasiHttpClient instance
   * @throws WasmException if HTTP client creation fails or permission is denied
   */
  WasiHttpClient createHttpClient() throws WasmException;

  /**
   * Creates a new HTTP server for handling HTTP requests.
   *
   * <p>The HTTP server provides high-level HTTP server functionality, supporting
   * features like request routing, middleware, and response handling.
   *
   * @return a new WasiHttpServer instance
   * @throws WasmException if HTTP server creation fails or permission is denied
   */
  WasiHttpServer createHttpServer() throws WasmException;

  /**
   * Checks if networking capabilities are available and properly initialized.
   *
   * <p>This method can be used to verify that the networking extension is functional
   * and that the necessary permissions have been granted.
   *
   * @return true if networking is available, false otherwise
   */
  boolean isAvailable();

  /**
   * Gets networking statistics and usage information.
   *
   * <p>This method returns information about current network usage, including
   * number of active connections, data transfer statistics, and performance metrics.
   *
   * @return networking statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiNetworkingStats getStats() throws WasmException;
}