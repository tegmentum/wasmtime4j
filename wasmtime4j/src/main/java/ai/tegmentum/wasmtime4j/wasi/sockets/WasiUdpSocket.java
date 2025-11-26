package ai.tegmentum.wasmtime4j.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;

/**
 * UDP socket interface providing connectionless datagram communication.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/udp@0.2.0
 *
 * <p>A UDP socket can be in one of the following states:
 *
 * <ul>
 *   <li>Unbound
 *   <li>Bind-in-progress
 *   <li>Bound
 *   <li>Connected
 *   <li>Closed
 * </ul>
 */
public interface WasiUdpSocket {
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
   * Set the destination address for subsequent send operations.
   *
   * @param network the network resource
   * @param remoteAddress the remote address to connect to
   * @throws WasmException if connecting fails
   */
  void stream(WasiNetwork network, IpSocketAddress remoteAddress) throws WasmException;

  /**
   * Get the bound local address.
   *
   * @return the local address
   * @throws WasmException if getting address fails or socket not bound
   */
  IpSocketAddress localAddress() throws WasmException;

  /**
   * Get the remote peer address set by stream().
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
   * Set the unicast hop limit (TTL for IPv4, hop limit for IPv6).
   *
   * @param value the hop limit
   * @throws WasmException if setting value fails
   */
  void setUnicastHopLimit(int value) throws WasmException;

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
   * Receive a datagram.
   *
   * @param maxResults maximum number of datagrams to receive
   * @return array of received datagrams
   * @throws WasmException if receive fails
   */
  IncomingDatagram[] receive(long maxResults) throws WasmException;

  /**
   * Send datagrams.
   *
   * @param datagrams the datagrams to send
   * @return number of datagrams sent
   * @throws WasmException if send fails
   */
  long send(OutgoingDatagram[] datagrams) throws WasmException;

  /**
   * Close and dispose of this socket resource.
   *
   * @throws WasmException if closing fails
   */
  void close() throws WasmException;

  /** Incoming datagram containing data and remote address. */
  final class IncomingDatagram {
    private final byte[] data;
    private final IpSocketAddress remoteAddress;

    /**
     * Creates an IncomingDatagram.
     *
     * @param data the received data
     * @param remoteAddress the sender's address
     */
    public IncomingDatagram(final byte[] data, final IpSocketAddress remoteAddress) {
      if (data == null) {
        throw new IllegalArgumentException("data cannot be null");
      }
      if (remoteAddress == null) {
        throw new IllegalArgumentException("remoteAddress cannot be null");
      }
      this.data = data.clone();
      this.remoteAddress = remoteAddress;
    }

    public byte[] getData() {
      return data.clone();
    }

    public IpSocketAddress getRemoteAddress() {
      return remoteAddress;
    }
  }

  /** Outgoing datagram containing data and optional remote address. */
  final class OutgoingDatagram {
    private final byte[] data;
    private final IpSocketAddress remoteAddress;

    /**
     * Creates an outgoing datagram with explicit remote address.
     *
     * @param data the data to send
     * @param remoteAddress the destination address
     */
    public OutgoingDatagram(final byte[] data, final IpSocketAddress remoteAddress) {
      if (data == null) {
        throw new IllegalArgumentException("data cannot be null");
      }
      this.data = data.clone();
      this.remoteAddress = remoteAddress;
    }

    /**
     * Creates an outgoing datagram for a connected socket (remote address from stream()).
     *
     * @param data the data to send
     */
    public OutgoingDatagram(final byte[] data) {
      if (data == null) {
        throw new IllegalArgumentException("data cannot be null");
      }
      this.data = data.clone();
      this.remoteAddress = null;
    }

    public byte[] getData() {
      return data.clone();
    }

    public IpSocketAddress getRemoteAddress() {
      return remoteAddress;
    }

    public boolean hasRemoteAddress() {
      return remoteAddress != null;
    }
  }
}
