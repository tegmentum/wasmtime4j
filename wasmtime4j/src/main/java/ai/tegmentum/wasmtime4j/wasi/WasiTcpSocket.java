/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;

/**
 * WASI Preview 2 TCP Socket interface.
 *
 * <p>Provides TCP socket operations following the WASI socket interface. Operations are typically
 * non-blocking and use a two-phase pattern (start/finish) for async operations.
 *
 * <p>This interface maps to the wasi:sockets/tcp@0.2.0 WIT interface.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a TCP socket
 * WasiTcpSocket socket = runtime.createTcpSocket(WasiAddressFamily.IPV4);
 *
 * // Bind to a local address
 * socket.startBind(WasiSocketAddress.ipv4(new byte[]{0, 0, 0, 0}, 8080));
 * socket.finishBind();
 *
 * // Listen for connections
 * socket.startListen();
 * socket.finishListen();
 *
 * // Accept a connection
 * WasiTcpSocket client = socket.accept();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiTcpSocket extends Closeable {

  /**
   * Starts the bind operation for this socket.
   *
   * <p>This is a non-blocking operation. Call {@link #finishBind()} to complete.
   *
   * @param localAddress the local address to bind to
   * @throws WasmException if the bind operation cannot be started
   */
  void startBind(WasiSocketAddress localAddress) throws WasmException;

  /**
   * Finishes the bind operation.
   *
   * @throws WasmException if the bind operation failed
   */
  void finishBind() throws WasmException;

  /**
   * Starts the connect operation to a remote address.
   *
   * <p>This is a non-blocking operation. Call {@link #finishConnect()} to complete.
   *
   * @param remoteAddress the remote address to connect to
   * @throws WasmException if the connect operation cannot be started
   */
  void startConnect(WasiSocketAddress remoteAddress) throws WasmException;

  /**
   * Finishes the connect operation.
   *
   * @throws WasmException if the connect operation failed
   */
  void finishConnect() throws WasmException;

  /**
   * Starts listening for incoming connections.
   *
   * <p>This is a non-blocking operation. Call {@link #finishListen()} to complete.
   *
   * @throws WasmException if the listen operation cannot be started
   */
  void startListen() throws WasmException;

  /**
   * Finishes the listen operation.
   *
   * @throws WasmException if the listen operation failed
   */
  void finishListen() throws WasmException;

  /**
   * Accepts an incoming connection.
   *
   * @return a new socket for the accepted connection
   * @throws WasmException if no connection is available or an error occurred
   */
  WasiTcpSocket accept() throws WasmException;

  /**
   * Gets the local address of this socket.
   *
   * @return the local socket address
   * @throws WasmException if the address cannot be retrieved
   */
  WasiSocketAddress getLocalAddress() throws WasmException;

  /**
   * Gets the remote address of this socket.
   *
   * @return the remote socket address
   * @throws WasmException if the address cannot be retrieved or not connected
   */
  WasiSocketAddress getRemoteAddress() throws WasmException;

  /**
   * Gets the address family of this socket.
   *
   * @return the address family
   */
  WasiAddressFamily getAddressFamily();

  /**
   * Sets the listen backlog size.
   *
   * @param size the backlog size
   * @throws WasmException if the operation failed
   */
  void setListenBacklogSize(long size) throws WasmException;

  /**
   * Checks if keep-alive is enabled.
   *
   * @return true if keep-alive is enabled
   * @throws WasmException if the value cannot be retrieved
   */
  boolean isKeepAliveEnabled() throws WasmException;

  /**
   * Sets whether keep-alive is enabled.
   *
   * @param enabled true to enable keep-alive
   * @throws WasmException if the operation failed
   */
  void setKeepAliveEnabled(boolean enabled) throws WasmException;

  /**
   * Gets the keep-alive idle time in nanoseconds.
   *
   * @return the idle time in nanoseconds
   * @throws WasmException if the value cannot be retrieved
   */
  long getKeepAliveIdleTime() throws WasmException;

  /**
   * Sets the keep-alive idle time.
   *
   * @param nanos the idle time in nanoseconds
   * @throws WasmException if the operation failed
   */
  void setKeepAliveIdleTime(long nanos) throws WasmException;

  /**
   * Gets the keep-alive interval in nanoseconds.
   *
   * @return the interval in nanoseconds
   * @throws WasmException if the value cannot be retrieved
   */
  long getKeepAliveInterval() throws WasmException;

  /**
   * Sets the keep-alive interval.
   *
   * @param nanos the interval in nanoseconds
   * @throws WasmException if the operation failed
   */
  void setKeepAliveInterval(long nanos) throws WasmException;

  /**
   * Gets the keep-alive probe count.
   *
   * @return the probe count
   * @throws WasmException if the value cannot be retrieved
   */
  int getKeepAliveCount() throws WasmException;

  /**
   * Sets the keep-alive probe count.
   *
   * @param count the probe count
   * @throws WasmException if the operation failed
   */
  void setKeepAliveCount(int count) throws WasmException;

  /**
   * Gets the hop limit (TTL).
   *
   * @return the hop limit
   * @throws WasmException if the value cannot be retrieved
   */
  int getHopLimit() throws WasmException;

  /**
   * Sets the hop limit (TTL).
   *
   * @param hopLimit the hop limit
   * @throws WasmException if the operation failed
   */
  void setHopLimit(int hopLimit) throws WasmException;

  /**
   * Gets the receive buffer size in bytes.
   *
   * @return the receive buffer size
   * @throws WasmException if the value cannot be retrieved
   */
  long getReceiveBufferSize() throws WasmException;

  /**
   * Sets the receive buffer size.
   *
   * @param size the buffer size in bytes
   * @throws WasmException if the operation failed
   */
  void setReceiveBufferSize(long size) throws WasmException;

  /**
   * Gets the send buffer size in bytes.
   *
   * @return the send buffer size
   * @throws WasmException if the value cannot be retrieved
   */
  long getSendBufferSize() throws WasmException;

  /**
   * Sets the send buffer size.
   *
   * @param size the buffer size in bytes
   * @throws WasmException if the operation failed
   */
  void setSendBufferSize(long size) throws WasmException;

  /**
   * Gets a pollable handle for this socket.
   *
   * @return a pollable handle for use with wasi:io/poll
   * @throws WasmException if the handle cannot be retrieved
   */
  long subscribe() throws WasmException;

  /**
   * Shuts down the socket.
   *
   * @param shutdownType the type of shutdown (read, write, or both)
   * @throws WasmException if the shutdown failed
   */
  void shutdown(ShutdownType shutdownType) throws WasmException;

  /** Closes this socket and releases all resources. */
  @Override
  void close();

  /** The type of socket shutdown. */
  enum ShutdownType {
    /** Shutdown reading from the socket. */
    RECEIVE,
    /** Shutdown writing to the socket. */
    SEND,
    /** Shutdown both reading and writing. */
    BOTH
  }
}
