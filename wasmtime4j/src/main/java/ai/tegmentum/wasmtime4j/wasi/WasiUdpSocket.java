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
 * WASI Preview 2 UDP Socket interface.
 *
 * <p>Provides UDP socket operations following the WASI socket interface. Operations are typically
 * non-blocking and use a two-phase pattern (start/finish) for async operations.
 *
 * <p>This interface maps to the wasi:sockets/udp@0.2.0 WIT interface.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a UDP socket
 * WasiUdpSocket socket = runtime.createUdpSocket(WasiAddressFamily.IPV4);
 *
 * // Bind to a local address
 * socket.startBind(WasiSocketAddress.ipv4(new byte[]{0, 0, 0, 0}, 8080));
 * socket.finishBind();
 *
 * // Send a datagram
 * byte[] data = "Hello".getBytes();
 * WasiSocketAddress dest = WasiSocketAddress.ipv4(new byte[]{127, 0, 0, 1}, 9090);
 * socket.send(data, dest);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiUdpSocket extends Closeable {

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
   * Starts connecting this socket to a remote address.
   *
   * <p>For UDP, "connecting" means setting a default destination for send operations and filtering
   * received datagrams.
   *
   * @param remoteAddress the remote address to connect to, or null to disconnect
   * @throws WasmException if the operation cannot be started
   */
  void startConnect(WasiSocketAddress remoteAddress) throws WasmException;

  /**
   * Finishes the connect operation.
   *
   * @throws WasmException if the connect operation failed
   */
  void finishConnect() throws WasmException;

  /**
   * Gets the local address of this socket.
   *
   * @return the local socket address
   * @throws WasmException if the address cannot be retrieved
   */
  WasiSocketAddress getLocalAddress() throws WasmException;

  /**
   * Gets the remote address of this socket (if connected).
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
   * Gets the unicast hop limit (TTL).
   *
   * @return the hop limit
   * @throws WasmException if the value cannot be retrieved
   */
  int getUnicastHopLimit() throws WasmException;

  /**
   * Sets the unicast hop limit (TTL).
   *
   * @param hopLimit the hop limit
   * @throws WasmException if the operation failed
   */
  void setUnicastHopLimit(int hopLimit) throws WasmException;

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

  /** Closes this socket and releases all resources. */
  @Override
  void close();

  /**
   * Represents a received UDP datagram.
   *
   * <p>Contains the data and the source address of the datagram.
   */
  final class Datagram {
    private final byte[] data;
    private final WasiSocketAddress source;

    /**
     * Creates a new datagram.
     *
     * @param data the datagram payload
     * @param source the source address
     */
    public Datagram(final byte[] data, final WasiSocketAddress source) {
      this.data = data != null ? data.clone() : new byte[0];
      this.source = source;
    }

    /**
     * Gets the datagram payload.
     *
     * @return a copy of the data
     */
    public byte[] getData() {
      return data.clone();
    }

    /**
     * Gets the source address.
     *
     * @return the source address
     */
    public WasiSocketAddress getSource() {
      return source;
    }
  }
}
