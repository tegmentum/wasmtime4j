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

import ai.tegmentum.wasmtime4j.wasi.WasiAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.WasiSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.WasiUdpSocket;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama FFI implementation of WasiUdpSocket.
 *
 * <p>Provides UDP socket functionality using Panama Foreign Function API bindings to the native
 * Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiUdpSocket implements WasiUdpSocket {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();
  private static final int ADDRESS_BUFFER_SIZE = 256;

  private final long socketHandle;
  private final WasiAddressFamily addressFamily;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new UDP socket with the specified address family.
   *
   * @param addressFamily the address family (IPv4 or IPv6)
   * @throws WasmException if the socket cannot be created
   */
  public PanamaWasiUdpSocket(final WasiAddressFamily addressFamily) throws WasmException {
    if (addressFamily == null) {
      throw new IllegalArgumentException("Address family cannot be null");
    }
    this.addressFamily = addressFamily;

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outHandle = arena.allocate(ValueLayout.JAVA_LONG);
      final boolean isIpv6 = addressFamily == WasiAddressFamily.IPV6;
      final int result = NATIVE_BINDINGS.wasiUdpSocketCreate(isIpv6, outHandle);
      if (result != 0) {
        throw new WasmException("Failed to create UDP socket, error code: " + result);
      }
      this.socketHandle = outHandle.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  /**
   * Creates a UDP socket wrapper from an existing native handle.
   *
   * @param socketHandle the native socket handle
   * @param addressFamily the address family
   */
  PanamaWasiUdpSocket(final long socketHandle, final WasiAddressFamily addressFamily) {
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
          NATIVE_BINDINGS.wasiUdpSocketStartBind(
              socketHandle, addressSegment, localAddress.getPort());
      if (result != 0) {
        throw new WasmException("Failed to start UDP bind, error code: " + result);
      }
    }
  }

  @Override
  public void finishBind() throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiUdpSocketFinishBind(socketHandle);
    if (result != 0) {
      throw new WasmException("Failed to finish UDP bind, error code: " + result);
    }
  }

  @Override
  public void startConnect(final WasiSocketAddress remoteAddress) throws WasmException {
    // UDP uses stream-based approach for "connection" which is just setting the default destination
    // The WASI UDP API uses a stream method instead of connect
    ensureNotClosed();
    // No-op for now as the native API doesn't have direct UDP connect
  }

  @Override
  public void finishConnect() throws WasmException {
    ensureNotClosed();
    // No-op for UDP - connection is handled through stream method
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
            NATIVE_BINDINGS.wasiUdpSocketLocalAddress(
                socketHandle, outAddress, ADDRESS_BUFFER_SIZE, outPort);
      } else {
        result =
            NATIVE_BINDINGS.wasiUdpSocketRemoteAddress(
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
  public int getUnicastHopLimit() throws WasmException {
    // No getter available in native API - return default
    ensureNotClosed();
    return 0;
  }

  @Override
  public void setUnicastHopLimit(final int hopLimit) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiUdpSocketSetUnicastHopLimit(socketHandle, hopLimit);
    if (result != 0) {
      throw new WasmException("Failed to set unicast hop limit, error code: " + result);
    }
  }

  @Override
  public long getReceiveBufferSize() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outSize = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiUdpSocketReceiveBufferSize(socketHandle, outSize);
      if (result != 0) {
        throw new WasmException("Failed to get receive buffer size, error code: " + result);
      }
      return outSize.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void setReceiveBufferSize(final long size) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiUdpSocketSetReceiveBufferSize(socketHandle, size);
    if (result != 0) {
      throw new WasmException("Failed to set receive buffer size, error code: " + result);
    }
  }

  @Override
  public long getSendBufferSize() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outSize = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiUdpSocketSendBufferSize(socketHandle, outSize);
      if (result != 0) {
        throw new WasmException("Failed to get send buffer size, error code: " + result);
      }
      return outSize.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void setSendBufferSize(final long size) throws WasmException {
    ensureNotClosed();
    final int result = NATIVE_BINDINGS.wasiUdpSocketSetSendBufferSize(socketHandle, size);
    if (result != 0) {
      throw new WasmException("Failed to set send buffer size, error code: " + result);
    }
  }

  @Override
  public long subscribe() throws WasmException {
    ensureNotClosed();
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollable = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiUdpSocketSubscribe(socketHandle, outPollable);
      if (result != 0) {
        throw new WasmException("Failed to subscribe to UDP socket, error code: " + result);
      }
      return outPollable.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      NATIVE_BINDINGS.wasiUdpSocketClose(socketHandle);
    }
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("UDP socket is closed");
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
