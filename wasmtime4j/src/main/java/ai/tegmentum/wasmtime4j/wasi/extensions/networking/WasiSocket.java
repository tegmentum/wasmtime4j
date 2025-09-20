package ai.tegmentum.wasmtime4j.wasi.extensions.networking;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.nio.ByteBuffer;

/**
 * Represents a network socket for WASI networking operations.
 *
 * <p>A WasiSocket provides low-level network I/O operations including reading, writing, connecting,
 * and configuration. Sockets are created through the {@link
 * ai.tegmentum.wasmtime4j.wasi.extensions.WasiNetworking} interface and must be properly closed to
 * release system resources.
 *
 * <p>This interface implements AutoCloseable to support try-with-resources:
 *
 * <pre>{@code
 * try (WasiSocket socket = networking.createSocket(SocketType.STREAM, SocketFamily.INET)) {
 *     socket.connect(WasiSocketAddress.create("127.0.0.1", 8080));
 *     // Use socket...
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiSocket extends AutoCloseable {

  /**
   * Connects the socket to a remote address.
   *
   * <p>For stream sockets (TCP), this establishes a connection to the remote peer. For datagram
   * sockets (UDP), this sets the default destination for sends and filters incoming packets to only
   * those from the specified address.
   *
   * @param address the remote address to connect to
   * @throws WasmException if connection fails or permission is denied
   * @throws IllegalArgumentException if address is null
   * @throws IllegalStateException if socket is already connected or closed
   */
  void connect(final WasiSocketAddress address) throws WasmException;

  /**
   * Reads data from the socket into the provided buffer.
   *
   * <p>This method reads available data from the socket into the buffer. The buffer's position will
   * be advanced by the number of bytes read. For stream sockets, this may read fewer bytes than the
   * buffer capacity. For datagram sockets, this reads one complete packet.
   *
   * @param buffer the buffer to read data into
   * @return the number of bytes read, or -1 if end of stream reached
   * @throws WasmException if read operation fails
   * @throws IllegalArgumentException if buffer is null
   * @throws IllegalStateException if socket is not connected or closed
   */
  int read(final ByteBuffer buffer) throws WasmException;

  /**
   * Writes data from the buffer to the socket.
   *
   * <p>This method writes data from the buffer to the socket. The buffer's position will be
   * advanced by the number of bytes written. For stream sockets, this may write fewer bytes than
   * available in the buffer. For datagram sockets, this sends one complete packet.
   *
   * @param buffer the buffer containing data to write
   * @return the number of bytes written
   * @throws WasmException if write operation fails
   * @throws IllegalArgumentException if buffer is null
   * @throws IllegalStateException if socket is not connected or closed
   */
  int write(final ByteBuffer buffer) throws WasmException;

  /**
   * Sets a socket option to configure socket behavior.
   *
   * <p>Socket options control various aspects of socket behavior such as timeouts, buffer sizes,
   * and protocol-specific settings.
   *
   * @param option the socket option to set
   * @param value the value for the option
   * @throws WasmException if setting the option fails
   * @throws IllegalArgumentException if option or value is null/invalid
   * @throws IllegalStateException if socket is closed
   */
  void setOption(final SocketOption option, final Object value) throws WasmException;

  /**
   * Gets the value of a socket option.
   *
   * @param option the socket option to get
   * @return the current value of the option
   * @throws WasmException if getting the option fails
   * @throws IllegalArgumentException if option is null or not supported
   * @throws IllegalStateException if socket is closed
   */
  Object getOption(final SocketOption option) throws WasmException;

  /**
   * Gets the local address and port that this socket is bound to.
   *
   * @return the local socket address, or null if not bound
   * @throws WasmException if operation fails
   * @throws IllegalStateException if socket is closed
   */
  WasiSocketAddress getLocalAddress() throws WasmException;

  /**
   * Gets the remote address and port that this socket is connected to.
   *
   * @return the remote socket address, or null if not connected
   * @throws WasmException if operation fails
   * @throws IllegalStateException if socket is closed
   */
  WasiSocketAddress getRemoteAddress() throws WasmException;

  /**
   * Gets the socket type (STREAM, DGRAM, etc.).
   *
   * @return the socket type
   */
  SocketType getType();

  /**
   * Gets the socket address family (INET, INET6, UNIX).
   *
   * @return the socket family
   */
  SocketFamily getFamily();

  /**
   * Checks if the socket is currently connected to a remote peer.
   *
   * @return true if connected, false otherwise
   */
  boolean isConnected();

  /**
   * Checks if the socket is bound to a local address.
   *
   * @return true if bound, false otherwise
   */
  boolean isBound();

  /**
   * Checks if the socket is closed.
   *
   * @return true if closed, false otherwise
   */
  boolean isClosed();

  /**
   * Shuts down the input side of the socket connection.
   *
   * <p>After calling this method, the socket will not receive any more data. Any attempt to read
   * from the socket will return -1 (end of stream).
   *
   * @throws WasmException if shutdown fails
   * @throws IllegalStateException if socket is not connected or already closed
   */
  void shutdownInput() throws WasmException;

  /**
   * Shuts down the output side of the socket connection.
   *
   * <p>After calling this method, the socket will not send any more data. Any attempt to write to
   * the socket will throw an exception.
   *
   * @throws WasmException if shutdown fails
   * @throws IllegalStateException if socket is not connected or already closed
   */
  void shutdownOutput() throws WasmException;

  /**
   * Gets socket statistics and performance metrics.
   *
   * @return socket statistics object
   * @throws WasmException if statistics retrieval fails
   */
  WasiSocketStats getStats() throws WasmException;

  /**
   * Closes the socket and releases all associated resources.
   *
   * <p>After calling this method, the socket becomes unusable and all subsequent operations will
   * throw exceptions. This method can be called multiple times safely.
   */
  @Override
  void close();
}
