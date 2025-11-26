/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiUdpSocket interface.
 *
 * <p>This class provides access to WASI Preview 2 UDP socket operations through JNI calls to the
 * native Wasmtime library. UDP sockets provide connectionless datagram communication.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/udp@0.2.0
 *
 * @since 1.0.0
 */
public final class JniWasiUdpSocket implements WasiUdpSocket {

  private static final Logger LOGGER = Logger.getLogger(JniWasiUdpSocket.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiUdpSocket: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /** The native socket handle. */
  private final long socketHandle;

  /** Whether this socket has been closed. */
  private boolean closed = false;

  /**
   * Creates a new JNI WASI UDP socket with the given address family.
   *
   * @param contextHandle the native context handle
   * @param addressFamily the IP address family (IPv4 or IPv6)
   * @return a new WasiUdpSocket instance
   * @throws IllegalArgumentException if context handle is 0 or address family is null
   * @throws WasmException if socket creation fails
   */
  public static JniWasiUdpSocket create(
      final long contextHandle, final IpAddressFamily addressFamily) throws WasmException {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    if (addressFamily == null) {
      throw new IllegalArgumentException("Address family cannot be null");
    }

    final long socketHandle =
        nativeCreate(contextHandle, addressFamily == IpAddressFamily.IPV6);
    if (socketHandle <= 0) {
      throw new WasmException("Failed to create UDP socket");
    }

    return new JniWasiUdpSocket(contextHandle, socketHandle);
  }

  /**
   * Creates a new JNI WASI UDP socket with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param socketHandle the native socket handle
   * @throws IllegalArgumentException if context handle is 0 or socket handle is invalid
   */
  private JniWasiUdpSocket(final long contextHandle, final long socketHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    if (socketHandle <= 0) {
      throw new IllegalArgumentException("Socket handle must be positive: " + socketHandle);
    }
    this.contextHandle = contextHandle;
    this.socketHandle = socketHandle;
    LOGGER.fine(
        "Created JNI WASI UDP socket with context handle: "
            + contextHandle
            + ", socket handle: "
            + socketHandle);
  }

  @Override
  public void startBind(final WasiNetwork network, final IpSocketAddress localAddress)
      throws WasmException {
    if (network == null) {
      throw new IllegalArgumentException("Network cannot be null");
    }
    if (localAddress == null) {
      throw new IllegalArgumentException("Local address cannot be null");
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    if (!(network instanceof JniWasiNetwork)) {
      throw new IllegalArgumentException("Network must be a JniWasiNetwork instance");
    }

    final AddressParams params = encodeAddress(localAddress);
    nativeStartBind(
        contextHandle,
        socketHandle,
        ((JniWasiNetwork) network).getNetworkHandle(),
        params.isIpv4,
        params.ipv4Octets,
        params.ipv6Segments,
        params.port,
        params.flowInfo,
        params.scopeId);
  }

  @Override
  public void finishBind() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    nativeFinishBind(contextHandle, socketHandle);
  }

  @Override
  public void stream(final WasiNetwork network, final IpSocketAddress remoteAddress)
      throws WasmException {
    if (network == null) {
      throw new IllegalArgumentException("Network cannot be null");
    }
    if (remoteAddress == null) {
      throw new IllegalArgumentException("Remote address cannot be null");
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    if (!(network instanceof JniWasiNetwork)) {
      throw new IllegalArgumentException("Network must be a JniWasiNetwork instance");
    }

    final AddressParams params = encodeAddress(remoteAddress);
    nativeStream(
        contextHandle,
        socketHandle,
        ((JniWasiNetwork) network).getNetworkHandle(),
        params.isIpv4,
        params.ipv4Octets,
        params.ipv6Segments,
        params.port,
        params.flowInfo,
        params.scopeId);
  }

  @Override
  public IpSocketAddress localAddress() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final long[] encoded = nativeLocalAddress(contextHandle, socketHandle);
    if (encoded == null) {
      throw new WasmException("Failed to get local address");
    }

    return decodeAddress(encoded);
  }

  @Override
  public IpSocketAddress remoteAddress() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final long[] encoded = nativeRemoteAddress(contextHandle, socketHandle);
    if (encoded == null) {
      throw new WasmException("Failed to get remote address");
    }

    return decodeAddress(encoded);
  }

  @Override
  public IpAddressFamily addressFamily() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final boolean isIpv6 = nativeAddressFamily(contextHandle, socketHandle);
    return isIpv6 ? IpAddressFamily.IPV6 : IpAddressFamily.IPV4;
  }

  @Override
  public void setUnicastHopLimit(final int value) throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    nativeSetUnicastHopLimit(contextHandle, socketHandle, value);
  }

  @Override
  public long receiveBufferSize() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    return nativeReceiveBufferSize(contextHandle, socketHandle);
  }

  @Override
  public void setReceiveBufferSize(final long value) throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    nativeSetReceiveBufferSize(contextHandle, socketHandle, value);
  }

  @Override
  public long sendBufferSize() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    return nativeSendBufferSize(contextHandle, socketHandle);
  }

  @Override
  public void setSendBufferSize(final long value) throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    nativeSetSendBufferSize(contextHandle, socketHandle, value);
  }

  @Override
  public WasiPollable subscribe() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final long pollableHandle = nativeSubscribe(contextHandle, socketHandle);
    if (pollableHandle <= 0) {
      throw new WasmException("Failed to create pollable");
    }

    return new JniWasiPollable(contextHandle, pollableHandle);
  }

  @Override
  public IncomingDatagram[] receive(final long maxResults) throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    // TODO: Implement once native bindings are available
    throw new UnsupportedOperationException("receive() not yet implemented");
  }

  @Override
  public long send(final OutgoingDatagram[] datagrams) throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    // TODO: Implement once native bindings are available
    throw new UnsupportedOperationException("send() not yet implemented");
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    nativeClose(contextHandle, socketHandle);
    closed = true;
    LOGGER.fine("Closed JNI WASI UDP socket with handle: " + socketHandle);
  }

  // Helper class to hold address parameters for encoding
  private static final class AddressParams {
    final boolean isIpv4;
    final byte[] ipv4Octets;
    final byte[] ipv6Segments;
    final int port;
    final int flowInfo;
    final int scopeId;

    AddressParams(
        final boolean isIpv4,
        final byte[] ipv4Octets,
        final byte[] ipv6Segments,
        final int port,
        final int flowInfo,
        final int scopeId) {
      this.isIpv4 = isIpv4;
      this.ipv4Octets = ipv4Octets;
      this.ipv6Segments = ipv6Segments;
      this.port = port;
      this.flowInfo = flowInfo;
      this.scopeId = scopeId;
    }
  }

  // Helper method to encode IpSocketAddress to parameters
  private static AddressParams encodeAddress(final IpSocketAddress address) {
    if (address.isIpv4()) {
      final Ipv4SocketAddress ipv4 = address.getIpv4();
      return new AddressParams(
          true,
          ipv4.getAddress().getOctets(),
          null,
          ipv4.getPort(),
          0, // IPv4 doesn't have flow info
          0 // IPv4 doesn't have scope ID
          );
    } else {
      final Ipv6SocketAddress ipv6 = address.getIpv6();
      final short[] segments = ipv6.getAddress().getSegments();

      // Convert shorts to bytes (big-endian)
      final byte[] segmentBytes = new byte[16];
      for (int i = 0; i < 8; i++) {
        segmentBytes[i * 2] = (byte) (segments[i] >> 8);
        segmentBytes[i * 2 + 1] = (byte) segments[i];
      }

      return new AddressParams(
          false,
          null,
          segmentBytes,
          ipv6.getPort(),
          (int) ipv6.getFlowInfo(),
          (int) ipv6.getScopeId());
    }
  }

  // Helper method to decode long array to IpSocketAddress
  private static IpSocketAddress decodeAddress(final long[] encoded) {
    final boolean isIpv4 = encoded[0] != 0;

    if (isIpv4) {
      // IPv4: [is_ipv4=1, octet0, octet1, octet2, octet3, port, flow_info, scope_id]
      final byte[] octets =
          new byte[] {
            (byte) encoded[1], (byte) encoded[2], (byte) encoded[3], (byte) encoded[4]
          };
      final int port = (int) encoded[5];

      final ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address ipv4Address =
          new ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address(octets);
      return IpSocketAddress.ipv4(new Ipv4SocketAddress(port, ipv4Address));
    } else {
      // IPv6: [is_ipv4=0, seg0, seg1, ..., seg7, port, flow_info, scope_id]
      final short[] segments = new short[8];
      for (int i = 0; i < 8; i++) {
        segments[i] = (short) encoded[i + 1];
      }
      final int port = (int) encoded[9];
      final long flowInfo = encoded[10];
      final long scopeId = encoded[11];

      final ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address ipv6Address =
          new ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address(segments);
      return IpSocketAddress.ipv6(
          new Ipv6SocketAddress(port, (int) flowInfo, ipv6Address, (int) scopeId));
    }
  }

  // Native method declarations
  private static native long nativeCreate(long contextHandle, boolean isIpv6);

  private static native void nativeStartBind(
      long contextHandle,
      long socketHandle,
      long networkHandle,
      boolean isIpv4,
      byte[] ipv4Octets,
      byte[] ipv6Segments,
      int port,
      int flowInfo,
      int scopeId);

  private static native void nativeFinishBind(long contextHandle, long socketHandle);

  private static native void nativeStream(
      long contextHandle,
      long socketHandle,
      long networkHandle,
      boolean isIpv4,
      byte[] ipv4Octets,
      byte[] ipv6Segments,
      int port,
      int flowInfo,
      int scopeId);

  private static native long[] nativeLocalAddress(long contextHandle, long socketHandle);

  private static native long[] nativeRemoteAddress(long contextHandle, long socketHandle);

  private static native boolean nativeAddressFamily(long contextHandle, long socketHandle);

  private static native void nativeSetUnicastHopLimit(
      long contextHandle, long socketHandle, int value);

  private static native long nativeReceiveBufferSize(long contextHandle, long socketHandle);

  private static native void nativeSetReceiveBufferSize(
      long contextHandle, long socketHandle, long value);

  private static native long nativeSendBufferSize(long contextHandle, long socketHandle);

  private static native void nativeSetSendBufferSize(
      long contextHandle, long socketHandle, long value);

  private static native long nativeSubscribe(long contextHandle, long socketHandle);

  private static native void nativeClose(long contextHandle, long socketHandle);
}
