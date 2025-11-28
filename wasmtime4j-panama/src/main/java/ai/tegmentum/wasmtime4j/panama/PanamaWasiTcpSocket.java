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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasiAddressFamily;
import ai.tegmentum.wasmtime4j.WasiSocketAddress;
import ai.tegmentum.wasmtime4j.WasiTcpSocket;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama FFI implementation of WasiTcpSocket.
 *
 * <p>Provides TCP socket functionality using Panama Foreign Function API bindings to the native
 * Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiTcpSocket implements WasiTcpSocket {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();
  private static final int ADDRESS_BUFFER_SIZE = 256;

  private final long socketHandle;
  private final WasiAddressFamily addressFamily;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new TCP socket with the specified address family.
   *
   * @param addressFamily the address family (IPv4 or IPv6)
   * @throws WasmException if the socket cannot be created
   */
  public PanamaWasiTcpSocket(final WasiAddressFamily addressFamily) throws WasmException {
    if (addressFamily == null) {
      throw new IllegalArgumentException("Address family cannot be null");
    }
    this.addressFamily = addressFamily;

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outHandle = arena.allocate(ValueLayout.JAVA_LONG);
      final boolean isIpv6 = addressFamily == WasiAddressFamily.IPV6;
      final int result = NATIVE_BINDINGS.wasiTcpSocketCreate(isIpv6, outHandle);
      if (result != 0) {
        throw new WasmException("Failed to create TCP socket, error code: " + result);
      }
      this.socketHandle = outHandle.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  /**
   * Creates a TCP socket wrapper from an existing native handle.
   *
   * @param socketHandle the native socket handle
   * @param addressFamily the address family
   */
  PanamaWasiTcpSocket(final long socketHandle, final WasiAddressFamily addressFamily) {
    this.socketHandle = socketHandle;
    this.addressFamily = addressFamily;
  }

  @Override
  public void startBind(final WasiSocketAddress localAddress) throws WasmException {
    if (localAddress == null) {
      throw new IllegalArgumentException("Local address cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final String addressStr = formatAddress(localAddress);
      final MemorySegment addressSegment = arena.allocateFrom(addressStr);

      final int result =
          NATIVE_BINDINGS.wasiTcpSocketStartBind(
              socketHandle, addressSegment, localAddress.getPort());
      if (result != 0) {
        throw new WasmException("Failed to start TCP bind, error code: " + result);
      }
    }
  }

  @Override
  public void finishBind() throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketFinishBind(socketHandle);
    if (result != 0) {
      throw new WasmException("Failed to finish TCP bind, error code: " + result);
    }
  }

  @Override
  public void startConnect(final WasiSocketAddress remoteAddress) throws WasmException {
    if (remoteAddress == null) {
      throw new IllegalArgumentException("Remote address cannot be null");
    }
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final String addressStr = formatAddress(remoteAddress);
      final MemorySegment addressSegment = arena.allocateFrom(addressStr);

      final int result =
          NATIVE_BINDINGS.wasiTcpSocketStartConnect(
              socketHandle, addressSegment, remoteAddress.getPort());
      if (result != 0) {
        throw new WasmException("Failed to start TCP connect, error code: " + result);
      }
    }
  }

  @Override
  public void finishConnect() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outInputStream = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outOutputStream = arena.allocate(ValueLayout.JAVA_LONG);
      final int result =
          NATIVE_BINDINGS.wasiTcpSocketFinishConnect(socketHandle, outInputStream, outOutputStream);
      if (result != 0) {
        throw new WasmException("Failed to finish TCP connect, error code: " + result);
      }
      // Stream handles are returned but not currently used
    }
  }

  @Override
  public void startListen() throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketStartListen(socketHandle);
    if (result != 0) {
      throw new WasmException("Failed to start TCP listen, error code: " + result);
    }
  }

  @Override
  public void finishListen() throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketFinishListen(socketHandle);
    if (result != 0) {
      throw new WasmException("Failed to finish TCP listen, error code: " + result);
    }
  }

  @Override
  public WasiTcpSocket accept() throws WasmException {
    ensureNotClosed();

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outClientSocket = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outInputStream = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outOutputStream = arena.allocate(ValueLayout.JAVA_LONG);
      final int result =
          NATIVE_BINDINGS.wasiTcpSocketAccept(
              socketHandle, outClientSocket, outInputStream, outOutputStream);
      if (result != 0) {
        throw new WasmException("Failed to accept TCP connection, error code: " + result);
      }
      final long clientHandle = outClientSocket.get(ValueLayout.JAVA_LONG, 0);
      return new PanamaWasiTcpSocket(clientHandle, addressFamily);
    }
  }

  @Override
  public WasiSocketAddress getLocalAddress() throws WasmException {
    ensureNotClosed();
    return getSocketAddress(true);
  }

  @Override
  public WasiSocketAddress getRemoteAddress() throws WasmException {
    ensureNotClosed();
    return getSocketAddress(false);
  }

  private WasiSocketAddress getSocketAddress(final boolean local) throws WasmException {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outAddress = arena.allocate(ADDRESS_BUFFER_SIZE);
      final MemorySegment outPort = arena.allocate(ValueLayout.JAVA_INT);

      final int result;
      if (local) {
        result =
            NATIVE_BINDINGS.wasiTcpSocketLocalAddress(
                socketHandle, outAddress, ADDRESS_BUFFER_SIZE, outPort);
      } else {
        result =
            NATIVE_BINDINGS.wasiTcpSocketRemoteAddress(
                socketHandle, outAddress, ADDRESS_BUFFER_SIZE, outPort);
      }

      if (result != 0) {
        throw new WasmException(
            "Failed to get " + (local ? "local" : "remote") + " address, error code: " + result);
      }

      final String addressStr = outAddress.getString(0);
      final int port = outPort.get(ValueLayout.JAVA_INT, 0);

      return parseAddress(addressStr, port);
    }
  }

  @Override
  public WasiAddressFamily getAddressFamily() {
    return addressFamily;
  }

  @Override
  public void setListenBacklogSize(final long size) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetListenBacklogSize(socketHandle, size);
    if (result != 0) {
      throw new WasmException("Failed to set listen backlog size, error code: " + result);
    }
  }

  @Override
  public boolean isKeepAliveEnabled() throws WasmException {
    // No getter available in native API - return default
    ensureNotClosed();
    return false;
  }

  @Override
  public void setKeepAliveEnabled(final boolean enabled) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetKeepAliveEnabled(socketHandle, enabled);
    if (result != 0) {
      throw new WasmException("Failed to set keep-alive enabled, error code: " + result);
    }
  }

  @Override
  public long getKeepAliveIdleTime() throws WasmException {
    // No getter available in native API - return default
    ensureNotClosed();
    return 0;
  }

  @Override
  public void setKeepAliveIdleTime(final long nanos) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetKeepAliveIdleTime(socketHandle, nanos);
    if (result != 0) {
      throw new WasmException("Failed to set keep-alive idle time, error code: " + result);
    }
  }

  @Override
  public long getKeepAliveInterval() throws WasmException {
    // No getter available in native API - return default
    ensureNotClosed();
    return 0;
  }

  @Override
  public void setKeepAliveInterval(final long nanos) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetKeepAliveInterval(socketHandle, nanos);
    if (result != 0) {
      throw new WasmException("Failed to set keep-alive interval, error code: " + result);
    }
  }

  @Override
  public int getKeepAliveCount() throws WasmException {
    // No getter available in native API - return default
    ensureNotClosed();
    return 0;
  }

  @Override
  public void setKeepAliveCount(final int count) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetKeepAliveCount(socketHandle, count);
    if (result != 0) {
      throw new WasmException("Failed to set keep-alive count, error code: " + result);
    }
  }

  @Override
  public int getHopLimit() throws WasmException {
    // No getter available in native API - return default
    ensureNotClosed();
    return 0;
  }

  @Override
  public void setHopLimit(final int hopLimit) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetHopLimit(socketHandle, hopLimit);
    if (result != 0) {
      throw new WasmException("Failed to set hop limit, error code: " + result);
    }
  }

  @Override
  public long getReceiveBufferSize() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outSize = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiTcpSocketReceiveBufferSize(socketHandle, outSize);
      if (result != 0) {
        throw new WasmException("Failed to get receive buffer size, error code: " + result);
      }
      return outSize.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void setReceiveBufferSize(final long size) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetReceiveBufferSize(socketHandle, size);
    if (result != 0) {
      throw new WasmException("Failed to set receive buffer size, error code: " + result);
    }
  }

  @Override
  public long getSendBufferSize() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outSize = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiTcpSocketSendBufferSize(socketHandle, outSize);
      if (result != 0) {
        throw new WasmException("Failed to get send buffer size, error code: " + result);
      }
      return outSize.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void setSendBufferSize(final long size) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiTcpSocketSetSendBufferSize(socketHandle, size);
    if (result != 0) {
      throw new WasmException("Failed to set send buffer size, error code: " + result);
    }
  }

  @Override
  public long subscribe() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollable = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiTcpSocketSubscribe(socketHandle, outPollable);
      if (result != 0) {
        throw new WasmException("Failed to subscribe to TCP socket, error code: " + result);
      }
      return outPollable.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void shutdown(final ShutdownType shutdownType) throws WasmException {
    if (shutdownType == null) {
      throw new IllegalArgumentException("Shutdown type cannot be null");
    }
    ensureNotClosed();

    final int shutdownValue;
    switch (shutdownType) {
      case RECEIVE:
        shutdownValue = 0;
        break;
      case SEND:
        shutdownValue = 1;
        break;
      case BOTH:
        shutdownValue = 2;
        break;
      default:
        throw new IllegalArgumentException("Unknown shutdown type: " + shutdownType);
    }

    final int result = NATIVE_BINDINGS.wasiTcpSocketShutdown(socketHandle, shutdownValue);
    if (result != 0) {
      throw new WasmException("Failed to shutdown TCP socket, error code: " + result);
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      NATIVE_BINDINGS.wasiTcpSocketClose(socketHandle);
    }
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("TCP socket is closed");
    }
  }

  private String formatAddress(final WasiSocketAddress address) {
    byte[] raw = address.getAddress();
    if (address.getFamily() == WasiAddressFamily.IPV4) {
      return String.format(
          "%d.%d.%d.%d", raw[0] & 0xFF, raw[1] & 0xFF, raw[2] & 0xFF, raw[3] & 0xFF);
    } else {
      // Format IPv6 address
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 16; i += 2) {
        if (i > 0) {
          sb.append(":");
        }
        sb.append(String.format("%02x%02x", raw[i] & 0xFF, raw[i + 1] & 0xFF));
      }
      return sb.toString();
    }
  }

  private WasiSocketAddress parseAddress(final String addressStr, final int port)
      throws WasmException {
    try {
      InetAddress addr = InetAddress.getByName(addressStr);
      byte[] raw = addr.getAddress();
      if (raw.length == 4) {
        return WasiSocketAddress.ipv4(raw, port);
      } else {
        return WasiSocketAddress.ipv6(raw, port);
      }
    } catch (UnknownHostException e) {
      throw new WasmException("Failed to parse address: " + addressStr, e);
    }
  }
}
