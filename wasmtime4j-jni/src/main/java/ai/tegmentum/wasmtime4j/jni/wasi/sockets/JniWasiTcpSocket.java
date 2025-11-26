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
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiInputStream;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiOutputStream;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiTcpSocket interface.
 *
 * <p>This class provides access to WASI Preview 2 TCP socket operations through JNI calls to the
 * native Wasmtime library.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/tcp@0.2.0
 *
 * @since 1.0.0
 */
public final class JniWasiTcpSocket implements WasiTcpSocket {

  private static final Logger LOGGER = Logger.getLogger(JniWasiTcpSocket.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiTcpSocket: " + e.getMessage());
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
   * Creates a new JNI WASI TCP socket for the specified address family.
   *
   * @param contextHandle the native context handle
   * @param addressFamily the IP address family
   * @return a new WasiTcpSocket instance
   * @throws IllegalArgumentException if context handle is 0
   * @throws WasmException if socket creation fails
   */
  public static JniWasiTcpSocket create(
      final long contextHandle, final IpAddressFamily addressFamily) throws WasmException {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    if (addressFamily == null) {
      throw new IllegalArgumentException("Address family cannot be null");
    }

    final long socketHandle = nativeCreate(contextHandle, addressFamily == IpAddressFamily.IPV6);
    if (socketHandle <= 0) {
      throw new WasmException("Failed to create TCP socket");
    }

    return new JniWasiTcpSocket(contextHandle, socketHandle);
  }

  /**
   * Creates a new JNI WASI TCP socket with the given native context and socket handles.
   *
   * @param contextHandle the native context handle
   * @param socketHandle the native socket handle
   * @throws IllegalArgumentException if context handle is 0 or socket handle is invalid
   */
  public JniWasiTcpSocket(final long contextHandle, final long socketHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    if (socketHandle <= 0) {
      throw new IllegalArgumentException("Socket handle must be positive: " + socketHandle);
    }
    this.contextHandle = contextHandle;
    this.socketHandle = socketHandle;
    LOGGER.fine(
        "Created JNI WASI TCP socket with context handle: "
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

    final AddressParams params = encodeAddress(localAddress);
    nativeStartBind(
        contextHandle,
        socketHandle,
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
  public void startConnect(final WasiNetwork network, final IpSocketAddress remoteAddress)
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

    final AddressParams params = encodeAddress(remoteAddress);
    nativeStartConnect(
        contextHandle,
        socketHandle,
        params.isIpv4,
        params.ipv4Octets,
        params.ipv6Segments,
        params.port,
        params.flowInfo,
        params.scopeId);
  }

  @Override
  public ConnectionStreams finishConnect() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final long[] result = nativeFinishConnect(contextHandle, socketHandle);
    if (result == null || result.length != 2) {
      throw new WasmException("Failed to finish connect");
    }

    final long inputHandle = result[0];
    final long outputHandle = result[1];

    if (inputHandle <= 0 || outputHandle <= 0) {
      throw new WasmException("Invalid stream handles returned");
    }

    return new ConnectionStreams(
        new JniWasiInputStream(contextHandle, inputHandle),
        new JniWasiOutputStream(contextHandle, outputHandle));
  }

  @Override
  public void startListen() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeStartListen(contextHandle, socketHandle);
  }

  @Override
  public void finishListen() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeFinishListen(contextHandle, socketHandle);
  }

  @Override
  public AcceptResult accept() throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final long[] result = nativeAccept(contextHandle, socketHandle);
    if (result == null || result.length != 3) {
      throw new WasmException("Failed to accept connection");
    }

    final long newSocketHandle = result[0];
    final long inputHandle = result[1];
    final long outputHandle = result[2];

    if (newSocketHandle <= 0 || inputHandle <= 0 || outputHandle <= 0) {
      throw new WasmException("Invalid handles returned");
    }

    return new AcceptResult(
        new JniWasiTcpSocket(contextHandle, newSocketHandle),
        new JniWasiInputStream(contextHandle, inputHandle),
        new JniWasiOutputStream(contextHandle, outputHandle));
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
  public void setListenBacklogSize(final long value) throws WasmException {
    if (value < 0) {
      throw new IllegalArgumentException("Backlog size cannot be negative: " + value);
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeSetListenBacklogSize(contextHandle, socketHandle, value);
  }

  @Override
  public void setKeepAliveEnabled(final boolean value) throws WasmException {
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeSetKeepAliveEnabled(contextHandle, socketHandle, value);
  }

  @Override
  public void setKeepAliveIdleTime(final long value) throws WasmException {
    if (value < 0) {
      throw new IllegalArgumentException("Idle time cannot be negative: " + value);
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeSetKeepAliveIdleTime(contextHandle, socketHandle, value);
  }

  @Override
  public void setKeepAliveInterval(final long value) throws WasmException {
    if (value < 0) {
      throw new IllegalArgumentException("Interval cannot be negative: " + value);
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeSetKeepAliveInterval(contextHandle, socketHandle, value);
  }

  @Override
  public void setKeepAliveCount(final int value) throws WasmException {
    if (value < 0) {
      throw new IllegalArgumentException("Count cannot be negative: " + value);
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeSetKeepAliveCount(contextHandle, socketHandle, value);
  }

  @Override
  public void setHopLimit(final int value) throws WasmException {
    if (value < 0 || value > 255) {
      throw new IllegalArgumentException("Hop limit must be 0-255: " + value);
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }
    nativeSetHopLimit(contextHandle, socketHandle, value);
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
    if (value < 0) {
      throw new IllegalArgumentException("Buffer size cannot be negative: " + value);
    }
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
    if (value < 0) {
      throw new IllegalArgumentException("Buffer size cannot be negative: " + value);
    }
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
  public void shutdown(final ShutdownType shutdownType) throws WasmException {
    if (shutdownType == null) {
      throw new IllegalArgumentException("Shutdown type cannot be null");
    }
    if (closed) {
      throw new WasmException("Socket is closed");
    }

    final int type;
    switch (shutdownType) {
      case RECEIVE:
        type = 0;
        break;
      case SEND:
        type = 1;
        break;
      case BOTH:
        type = 2;
        break;
      default:
        throw new IllegalArgumentException("Unknown shutdown type: " + shutdownType);
    }

    nativeShutdown(contextHandle, socketHandle, type);
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    nativeClose(contextHandle, socketHandle);
    closed = true;
    LOGGER.fine("Closed JNI WASI TCP socket with handle: " + socketHandle);
  }

  // Helper class to hold address encoding parameters
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

  // Helper method to encode an IpSocketAddress for JNI
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

  // Helper method to decode an IpSocketAddress from JNI
  private static IpSocketAddress decodeAddress(final long[] encoded) {
    final boolean isIpv4 = encoded[0] != 0;

    if (isIpv4) {
      // IPv4: [is_ipv4=1, octet0, octet1, octet2, octet3, port, flow_info, scope_id]
      final byte[] octets =
          new byte[] {(byte) encoded[1], (byte) encoded[2], (byte) encoded[3], (byte) encoded[4]};
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
      boolean isIpv4,
      byte[] ipv4Octets,
      byte[] ipv6Segments,
      int port,
      int flowInfo,
      int scopeId);

  private static native void nativeFinishBind(long contextHandle, long socketHandle);

  private static native void nativeStartConnect(
      long contextHandle,
      long socketHandle,
      boolean isIpv4,
      byte[] ipv4Octets,
      byte[] ipv6Segments,
      int port,
      int flowInfo,
      int scopeId);

  private static native long[] nativeFinishConnect(long contextHandle, long socketHandle);

  private static native void nativeStartListen(long contextHandle, long socketHandle);

  private static native void nativeFinishListen(long contextHandle, long socketHandle);

  private static native long[] nativeAccept(long contextHandle, long socketHandle);

  private static native long[] nativeLocalAddress(long contextHandle, long socketHandle);

  private static native long[] nativeRemoteAddress(long contextHandle, long socketHandle);

  private static native boolean nativeAddressFamily(long contextHandle, long socketHandle);

  private static native void nativeSetListenBacklogSize(
      long contextHandle, long socketHandle, long value);

  private static native void nativeSetKeepAliveEnabled(
      long contextHandle, long socketHandle, boolean value);

  private static native void nativeSetKeepAliveIdleTime(
      long contextHandle, long socketHandle, long value);

  private static native void nativeSetKeepAliveInterval(
      long contextHandle, long socketHandle, long value);

  private static native void nativeSetKeepAliveCount(
      long contextHandle, long socketHandle, int value);

  private static native void nativeSetHopLimit(long contextHandle, long socketHandle, int value);

  private static native long nativeReceiveBufferSize(long contextHandle, long socketHandle);

  private static native void nativeSetReceiveBufferSize(
      long contextHandle, long socketHandle, long value);

  private static native long nativeSendBufferSize(long contextHandle, long socketHandle);

  private static native void nativeSetSendBufferSize(
      long contextHandle, long socketHandle, long value);

  private static native long nativeSubscribe(long contextHandle, long socketHandle);

  private static native void nativeShutdown(
      long contextHandle, long socketHandle, int shutdownType);

  private static native void nativeClose(long contextHandle, long socketHandle);
}
