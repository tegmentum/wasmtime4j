package ai.tegmentum.wasmtime4j.wasi;

import java.io.Closeable;

/**
 * Handle representing a network socket in the WASI environment.
 *
 * <p>A WasiSocket represents a network socket that can be used for communication. Sockets are
 * obtained by calling {@link WasiNetwork#createSocket} or {@link WasiNetwork#acceptSocket} and
 * should be closed when no longer needed.
 *
 * <p>Sockets are not thread-safe and should not be shared between threads without external
 * synchronization.
 *
 * @since 1.0.0
 */
public interface WasiSocket extends Closeable {

  /**
   * Gets the unique identifier for this socket.
   *
   * <p>The socket descriptor is a unique integer that identifies this socket within the WASI
   * context. It can be used for debugging and logging purposes.
   *
   * @return the socket descriptor number
   */
  int getSocketDescriptor();

  /**
   * Gets the type of this socket.
   *
   * <p>This returns the socket type that was specified when the socket was created (STREAM for
   * TCP, DGRAM for UDP, etc.).
   *
   * @return the socket type
   */
  WasiSocketType getType();

  /**
   * Gets the protocol of this socket.
   *
   * <p>This returns the protocol that was specified when the socket was created (TCP, UDP, etc.).
   *
   * @return the socket protocol
   */
  WasiProtocol getProtocol();

  /**
   * Gets the current state of this socket.
   *
   * <p>The socket state indicates whether the socket is unbound, bound, listening, connected,
   * or closed.
   *
   * @return the current socket state
   */
  WasiSocketState getState();

  /**
   * Checks if this socket is still valid and open.
   *
   * <p>A socket becomes invalid when it is closed either explicitly via {@link #close()} or
   * implicitly when the WASI context is destroyed.
   *
   * @return true if the socket is valid and can be used for operations, false otherwise
   */
  boolean isValid();

  /**
   * Checks if this socket is bound to a local address.
   *
   * @return true if the socket is bound, false otherwise
   */
  boolean isBound();

  /**
   * Checks if this socket is in listening state.
   *
   * @return true if the socket is listening for connections, false otherwise
   */
  boolean isListening();

  /**
   * Checks if this socket is connected to a remote address.
   *
   * @return true if the socket is connected, false otherwise
   */
  boolean isConnected();

  /**
   * Checks if this socket supports reading data.
   *
   * @return true if reading is supported, false otherwise
   */
  boolean canRead();

  /**
   * Checks if this socket supports writing data.
   *
   * @return true if writing is supported, false otherwise
   */
  boolean canWrite();

  /**
   * Gets the local address that this socket is bound to.
   *
   * <p>This returns the local address and port for bound or connected sockets.
   *
   * @return the local socket address, or null if not bound
   */
  WasiSocketAddress getLocalAddress();

  /**
   * Gets the remote address that this socket is connected to.
   *
   * <p>This returns the remote address and port for connected sockets.
   *
   * @return the remote socket address, or null if not connected
   */
  WasiSocketAddress getRemoteAddress();

  /**
   * Sets socket options for this socket.
   *
   * <p>This method allows configuration of various socket options such as timeouts, buffer sizes,
   * and protocol-specific settings.
   *
   * @param option the socket option to set
   * @param value the value to set for the option
   * @throws IllegalArgumentException if option or value is invalid
   * @throws IllegalStateException if the socket is closed
   */
  void setSocketOption(final WasiSocketOption option, final Object value);

  /**
   * Gets the value of a socket option.
   *
   * <p>This method retrieves the current value of a socket option.
   *
   * @param option the socket option to get
   * @return the current value of the option
   * @throws IllegalArgumentException if option is invalid
   * @throws IllegalStateException if the socket is closed
   */
  Object getSocketOption(final WasiSocketOption option);

  /**
   * Closes the socket and releases associated resources.
   *
   * <p>After calling this method, the socket becomes invalid and should not be used for further
   * operations. This method is idempotent - calling it multiple times has no additional effect.
   */
  @Override
  void close();
}