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

package ai.tegmentum.wasmtime4j.panama.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiUdpSocket;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiUdpSocket interface.
 *
 * <p>This class provides access to WASI Preview 2 UDP socket operations through Panama Foreign
 * Function API calls to the native Wasmtime library. UDP sockets provide connectionless datagram
 * communication.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/udp@0.2.0
 *
 * @since 1.0.0
 */
public final class PanamaWasiUdpSocket implements WasiUdpSocket {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiUdpSocket.class.getName());

  // Panama FFI function handles
  private static final MethodHandle CREATE_HANDLE;
  private static final MethodHandle START_BIND_HANDLE;
  private static final MethodHandle FINISH_BIND_HANDLE;
  private static final MethodHandle STREAM_HANDLE;
  private static final MethodHandle LOCAL_ADDRESS_HANDLE;
  private static final MethodHandle REMOTE_ADDRESS_HANDLE;
  private static final MethodHandle ADDRESS_FAMILY_HANDLE;
  private static final MethodHandle SET_UNICAST_HOP_LIMIT_HANDLE;
  private static final MethodHandle RECEIVE_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SET_RECEIVE_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SEND_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SET_SEND_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SUBSCRIBE_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;
  private static final MethodHandle RECEIVE_HANDLE;
  private static final MethodHandle SEND_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeResourceHandle.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_udp_socket_create(context_handle, is_ipv6, out_handle)
      CREATE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_udp_socket_start_bind(context_handle, socket_handle, is_ipv4,
      // ipv4_octets, ipv6_segments, port, flow_info, scope_id)
      START_BIND_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_start_bind").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_SHORT,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_INT));

      // int wasmtime4j_panama_wasi_udp_socket_finish_bind(context_handle, socket_handle)
      FINISH_BIND_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_finish_bind").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_udp_socket_stream(context_handle, socket_handle, is_ipv4,
      // ipv4_octets, ipv6_segments, port, flow_info, scope_id)
      STREAM_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_stream").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_SHORT,
                  ValueLayout.JAVA_INT,
                  ValueLayout.JAVA_INT));

      // int wasmtime4j_panama_wasi_udp_socket_local_address(context_handle, socket_handle,
      // out_is_ipv4, out_addr_buf, out_port, out_flow_info, out_scope_id)
      LOCAL_ADDRESS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_local_address").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_udp_socket_remote_address(context_handle, socket_handle,
      // out_is_ipv4, out_addr_buf, out_port, out_flow_info, out_scope_id)
      REMOTE_ADDRESS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_remote_address").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_udp_socket_address_family(context_handle, socket_handle,
      // out_is_ipv6)
      ADDRESS_FAMILY_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_address_family").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit(context_handle,
      // socket_handle, value)
      SET_UNICAST_HOP_LIMIT_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT));

      // int wasmtime4j_panama_wasi_udp_socket_receive_buffer_size(context_handle, socket_handle,
      // out_size)
      RECEIVE_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_receive_buffer_size").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size(context_handle,
      // socket_handle, value)
      SET_RECEIVE_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_udp_socket_send_buffer_size(context_handle, socket_handle,
      // out_size)
      SEND_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_send_buffer_size").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size(context_handle, socket_handle,
      // value)
      SET_SEND_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_udp_socket_subscribe(context_handle, socket_handle,
      // out_pollable_handle)
      SUBSCRIBE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_subscribe").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // void wasmtime4j_panama_wasi_udp_socket_close(context_handle, socket_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_close").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_udp_socket_receive(context_handle, socket_handle, max_results,
      // out_count, out_datagrams_data, out_datagrams_len, out_is_ipv4, out_ipv4_octets,
      // out_ipv6_segments, out_ports, out_flow_info, out_scope_id)
      RECEIVE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_receive").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context_handle
                  ValueLayout.JAVA_LONG, // socket_handle
                  ValueLayout.JAVA_LONG, // max_results
                  ValueLayout.ADDRESS, // out_count
                  ValueLayout.ADDRESS, // out_datagrams_data
                  ValueLayout.ADDRESS, // out_datagrams_len
                  ValueLayout.ADDRESS, // out_is_ipv4
                  ValueLayout.ADDRESS, // out_ipv4_octets
                  ValueLayout.ADDRESS, // out_ipv6_segments
                  ValueLayout.ADDRESS, // out_ports
                  ValueLayout.ADDRESS, // out_flow_info
                  ValueLayout.ADDRESS)); // out_scope_id

      // int wasmtime4j_panama_wasi_udp_socket_send(context_handle, socket_handle, datagram_count,
      // datagram_data, datagram_lengths, has_remote_address, is_ipv4, ipv4_octets, ipv6_segments,
      // ports, flow_info, scope_id, out_sent_count)
      SEND_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_udp_socket_send").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // context_handle
                  ValueLayout.JAVA_LONG, // socket_handle
                  ValueLayout.JAVA_LONG, // datagram_count
                  ValueLayout.ADDRESS, // datagram_data
                  ValueLayout.ADDRESS, // datagram_lengths
                  ValueLayout.ADDRESS, // has_remote_address
                  ValueLayout.ADDRESS, // is_ipv4
                  ValueLayout.ADDRESS, // ipv4_octets
                  ValueLayout.ADDRESS, // ipv6_segments
                  ValueLayout.ADDRESS, // ports
                  ValueLayout.ADDRESS, // flow_info
                  ValueLayout.ADDRESS, // scope_id
                  ValueLayout.ADDRESS)); // out_sent_count

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiUdpSocket: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /** The native socket handle. */
  private final long socketHandle;

  /** Resource lifecycle handle. */
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama WASI UDP socket with the given address family.
   *
   * @param contextHandle the native context handle
   * @param addressFamily the IP address family (IPv4 or IPv6)
   * @return a new WasiUdpSocket instance
   * @throws IllegalArgumentException if context handle is null or address family is null
   * @throws WasmException if socket creation fails
   */
  public static PanamaWasiUdpSocket create(
      final MemorySegment contextHandle, final IpAddressFamily addressFamily) throws WasmException {
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    if (addressFamily == null) {
      throw new IllegalArgumentException("Address family cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outHandle = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              CREATE_HANDLE.invoke(
                  contextHandle, addressFamily == IpAddressFamily.IPV6 ? 1 : 0, outHandle);

      if (result != 0) {
        throw new WasmException("Failed to create UDP socket");
      }

      final long socketHandle = outHandle.get(ValueLayout.JAVA_LONG, 0);
      if (socketHandle <= 0) {
        throw new WasmException("Failed to create UDP socket");
      }

      return new PanamaWasiUdpSocket(contextHandle, socketHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error creating UDP socket: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new Panama WASI UDP socket with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param socketHandle the native socket handle
   * @throws IllegalArgumentException if context handle is null or socket handle is invalid
   */
  private PanamaWasiUdpSocket(final MemorySegment contextHandle, final long socketHandle) {
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    if (socketHandle <= 0) {
      throw new IllegalArgumentException("Socket handle must be positive: " + socketHandle);
    }
    this.contextHandle = contextHandle;
    this.socketHandle = socketHandle;

    // Capture handle values for safety net (must not capture 'this')
    final MemorySegment safetyCtx = contextHandle;
    final long safetySocket = socketHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiUdpSocket",
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetySocket);
              } catch (final Throwable t) {
                throw new Exception("Error closing UDP socket handle: " + safetySocket, t);
              }
            },
            this,
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetySocket);
              } catch (final Throwable t) {
                LOGGER.warning(
                    "Safety net failed to close UDP socket handle " + safetySocket + ": " + t);
              }
            });

    LOGGER.fine(
        "Created Panama WASI UDP socket with context handle: "
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
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    if (!(network instanceof PanamaWasiNetwork)) {
      throw new IllegalArgumentException("Network must be a PanamaWasiNetwork instance");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final AddressParams params = encodeAddress(localAddress, arena);

      final int result =
          (int)
              START_BIND_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  params.isIpv4 ? 1 : 0,
                  params.ipv4Octets,
                  params.ipv6Segments,
                  (short) params.port,
                  params.flowInfo,
                  params.scopeId);

      if (result != 0) {
        throw new WasmException("Failed to start bind");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error starting bind: " + e.getMessage(), e);
    }
  }

  @Override
  public void finishBind() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result = (int) FINISH_BIND_HANDLE.invoke(contextHandle, socketHandle);

      if (result != 0) {
        throw new WasmException("Failed to finish bind");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error finishing bind: " + e.getMessage(), e);
    }
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
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    if (!(network instanceof PanamaWasiNetwork)) {
      throw new IllegalArgumentException("Network must be a PanamaWasiNetwork instance");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final AddressParams params = encodeAddress(remoteAddress, arena);

      final int result =
          (int)
              STREAM_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  params.isIpv4 ? 1 : 0,
                  params.ipv4Octets,
                  params.ipv6Segments,
                  (short) params.port,
                  params.flowInfo,
                  params.scopeId);

      if (result != 0) {
        throw new WasmException("Failed to stream");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error streaming: " + e.getMessage(), e);
    }
  }

  @Override
  public IpSocketAddress localAddress() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outIsIpv4 = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment outAddrBuf = arena.allocate(16); // Max size for IPv6
      final MemorySegment outPort = arena.allocate(ValueLayout.JAVA_SHORT);
      final MemorySegment outFlowInfo = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment outScopeId = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              LOCAL_ADDRESS_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  outIsIpv4,
                  outAddrBuf,
                  outPort,
                  outFlowInfo,
                  outScopeId);

      if (result != 0) {
        throw new WasmException("Failed to get local address");
      }

      return decodeAddress(outIsIpv4, outAddrBuf, outPort, outFlowInfo, outScopeId);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting local address: " + e.getMessage(), e);
    }
  }

  @Override
  public IpSocketAddress remoteAddress() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outIsIpv4 = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment outAddrBuf = arena.allocate(16); // Max size for IPv6
      final MemorySegment outPort = arena.allocate(ValueLayout.JAVA_SHORT);
      final MemorySegment outFlowInfo = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment outScopeId = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              REMOTE_ADDRESS_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  outIsIpv4,
                  outAddrBuf,
                  outPort,
                  outFlowInfo,
                  outScopeId);

      if (result != 0) {
        throw new WasmException("Failed to get remote address");
      }

      return decodeAddress(outIsIpv4, outAddrBuf, outPort, outFlowInfo, outScopeId);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting remote address: " + e.getMessage(), e);
    }
  }

  @Override
  public IpAddressFamily addressFamily() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outIsIpv6 = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) ADDRESS_FAMILY_HANDLE.invoke(contextHandle, socketHandle, outIsIpv6);

      if (result != 0) {
        throw new WasmException("Failed to get address family");
      }

      return outIsIpv6.get(ValueLayout.JAVA_INT, 0) != 0
          ? IpAddressFamily.IPV6
          : IpAddressFamily.IPV4;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting address family: " + e.getMessage(), e);
    }
  }

  @Override
  public void setUnicastHopLimit(final int value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_UNICAST_HOP_LIMIT_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set unicast hop limit");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting unicast hop limit: " + e.getMessage(), e);
    }
  }

  @Override
  public long receiveBufferSize() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSize = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int) RECEIVE_BUFFER_SIZE_HANDLE.invoke(contextHandle, socketHandle, outSize);

      if (result != 0) {
        throw new WasmException("Failed to get receive buffer size");
      }

      return outSize.get(ValueLayout.JAVA_LONG, 0);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting receive buffer size: " + e.getMessage(), e);
    }
  }

  @Override
  public void setReceiveBufferSize(final long value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_RECEIVE_BUFFER_SIZE_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set receive buffer size");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting receive buffer size: " + e.getMessage(), e);
    }
  }

  @Override
  public long sendBufferSize() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSize = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) SEND_BUFFER_SIZE_HANDLE.invoke(contextHandle, socketHandle, outSize);

      if (result != 0) {
        throw new WasmException("Failed to get send buffer size");
      }

      return outSize.get(ValueLayout.JAVA_LONG, 0);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting send buffer size: " + e.getMessage(), e);
    }
  }

  @Override
  public void setSendBufferSize(final long value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_SEND_BUFFER_SIZE_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set send buffer size");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting send buffer size: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiPollable subscribe() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollableHandle = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int) SUBSCRIBE_HANDLE.invoke(contextHandle, socketHandle, outPollableHandle);

      if (result != 0) {
        throw new WasmException("Failed to create pollable");
      }

      final long pollableHandle = outPollableHandle.get(ValueLayout.JAVA_LONG, 0);
      if (pollableHandle <= 0) {
        throw new WasmException("Failed to create pollable");
      }

      return new PanamaWasiPollable(contextHandle, MemorySegment.ofAddress(pollableHandle));

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error creating pollable: " + e.getMessage(), e);
    }
  }

  @Override
  public IncomingDatagram[] receive(final long maxResults) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }
    if (maxResults <= 0) {
      return new IncomingDatagram[0];
    }

    // Maximum UDP datagram size (65535 - IP header - UDP header)
    final int maxDatagramSize = 65507;
    final int maxResultsInt = (int) Math.min(maxResults, 64);

    try (final Arena arena = Arena.ofConfined()) {
      // Allocate output buffers
      final MemorySegment outCount = arena.allocate(ValueLayout.JAVA_LONG);

      // Allocate arrays for datagram data pointers and lengths
      final MemorySegment outDatagramsData = arena.allocate(ValueLayout.ADDRESS, maxResultsInt);
      final MemorySegment outDatagramsLen = arena.allocate(ValueLayout.JAVA_LONG, maxResultsInt);

      // Pre-allocate data buffers for each potential datagram
      final MemorySegment[] dataBuffers = new MemorySegment[maxResultsInt];
      for (int i = 0; i < maxResultsInt; i++) {
        dataBuffers[i] = arena.allocate(maxDatagramSize);
        outDatagramsData.setAtIndex(ValueLayout.ADDRESS, i, dataBuffers[i]);
      }

      // Allocate address information arrays
      final MemorySegment outIsIpv4 = arena.allocate(ValueLayout.JAVA_INT, maxResultsInt);
      final MemorySegment outIpv4Octets = arena.allocate(4L * maxResultsInt);
      final MemorySegment outIpv6Segments =
          arena.allocate(ValueLayout.JAVA_SHORT, 8L * maxResultsInt);
      final MemorySegment outPorts = arena.allocate(ValueLayout.JAVA_SHORT, maxResultsInt);
      final MemorySegment outFlowInfo = arena.allocate(ValueLayout.JAVA_INT, maxResultsInt);
      final MemorySegment outScopeId = arena.allocate(ValueLayout.JAVA_INT, maxResultsInt);

      // Call native function
      final int result =
          (int)
              RECEIVE_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  (long) maxResultsInt,
                  outCount,
                  outDatagramsData,
                  outDatagramsLen,
                  outIsIpv4,
                  outIpv4Octets,
                  outIpv6Segments,
                  outPorts,
                  outFlowInfo,
                  outScopeId);

      if (result != 0) {
        throw new WasmException("Failed to receive datagrams");
      }

      // Read the count of received datagrams
      final int count = (int) outCount.get(ValueLayout.JAVA_LONG, 0);
      if (count <= 0) {
        return new IncomingDatagram[0];
      }

      // Build result array
      final IncomingDatagram[] datagrams = new IncomingDatagram[count];
      for (int i = 0; i < count; i++) {
        // Read datagram data
        final int dataLen = (int) outDatagramsLen.getAtIndex(ValueLayout.JAVA_LONG, i);
        final byte[] data = new byte[dataLen];
        MemorySegment.copy(dataBuffers[i], ValueLayout.JAVA_BYTE, 0, data, 0, dataLen);

        // Read address information
        final int isIpv4 = outIsIpv4.getAtIndex(ValueLayout.JAVA_INT, i);
        final int port = outPorts.getAtIndex(ValueLayout.JAVA_SHORT, i) & 0xFFFF;

        final IpSocketAddress remoteAddress;
        if (isIpv4 != 0) {
          // IPv4 address
          final byte[] octets = new byte[4];
          for (int j = 0; j < 4; j++) {
            octets[j] = outIpv4Octets.get(ValueLayout.JAVA_BYTE, i * 4L + j);
          }
          remoteAddress =
              IpSocketAddress.ipv4(new Ipv4SocketAddress(port, new Ipv4Address(octets)));
        } else {
          // IPv6 address
          final short[] segments = new short[8];
          for (int j = 0; j < 8; j++) {
            segments[j] = outIpv6Segments.getAtIndex(ValueLayout.JAVA_SHORT, i * 8L + j);
          }
          final int flowInfo = outFlowInfo.getAtIndex(ValueLayout.JAVA_INT, i);
          final int scopeId = outScopeId.getAtIndex(ValueLayout.JAVA_INT, i);
          remoteAddress =
              IpSocketAddress.ipv6(
                  new Ipv6SocketAddress(port, flowInfo, new Ipv6Address(segments), scopeId));
        }

        datagrams[i] = new IncomingDatagram(data, remoteAddress);
      }

      LOGGER.fine("Received " + count + " datagrams on UDP socket");
      return datagrams;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error receiving datagrams: " + e.getMessage(), e);
    }
  }

  @Override
  public long send(final OutgoingDatagram[] datagrams) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }
    if (datagrams == null || datagrams.length == 0) {
      return 0;
    }

    final int count = datagrams.length;

    try (final Arena arena = Arena.ofConfined()) {
      // Allocate arrays for datagram data
      final MemorySegment datagramDataPtrs = arena.allocate(ValueLayout.ADDRESS, count);
      final MemorySegment datagramLengths = arena.allocate(ValueLayout.JAVA_LONG, count);

      // Allocate arrays for address information
      final MemorySegment hasRemoteAddress = arena.allocate(ValueLayout.JAVA_INT, count);
      final MemorySegment isIpv4 = arena.allocate(ValueLayout.JAVA_INT, count);
      final MemorySegment ipv4Octets = arena.allocate(4L * count);
      final MemorySegment ipv6Segments = arena.allocate(ValueLayout.JAVA_SHORT, 8L * count);
      final MemorySegment ports = arena.allocate(ValueLayout.JAVA_SHORT, count);
      final MemorySegment flowInfo = arena.allocate(ValueLayout.JAVA_INT, count);
      final MemorySegment scopeId = arena.allocate(ValueLayout.JAVA_INT, count);

      // Output parameter
      final MemorySegment outSentCount = arena.allocate(ValueLayout.JAVA_LONG);

      // Populate arrays from datagrams
      for (int i = 0; i < count; i++) {
        final OutgoingDatagram datagram = datagrams[i];
        if (datagram == null) {
          throw new IllegalArgumentException("Datagram at index " + i + " is null");
        }

        // Copy datagram data
        final byte[] data = datagram.getData();
        final MemorySegment dataSegment = arena.allocate(data.length);
        MemorySegment.copy(data, 0, dataSegment, ValueLayout.JAVA_BYTE, 0, data.length);
        datagramDataPtrs.setAtIndex(ValueLayout.ADDRESS, i, dataSegment);
        datagramLengths.setAtIndex(ValueLayout.JAVA_LONG, i, data.length);

        // Set address information
        if (datagram.hasRemoteAddress()) {
          hasRemoteAddress.setAtIndex(ValueLayout.JAVA_INT, i, 1);
          final IpSocketAddress addr = datagram.getRemoteAddress();

          if (addr.isIpv4()) {
            isIpv4.setAtIndex(ValueLayout.JAVA_INT, i, 1);
            final Ipv4SocketAddress ipv4Addr = addr.getIpv4();
            final byte[] octets = ipv4Addr.getAddress().getOctets();
            for (int j = 0; j < 4; j++) {
              ipv4Octets.set(ValueLayout.JAVA_BYTE, i * 4L + j, octets[j]);
            }
            ports.setAtIndex(ValueLayout.JAVA_SHORT, i, (short) ipv4Addr.getPort());
          } else {
            isIpv4.setAtIndex(ValueLayout.JAVA_INT, i, 0);
            final Ipv6SocketAddress ipv6Addr = addr.getIpv6();
            final short[] segments = ipv6Addr.getAddress().getSegments();
            for (int j = 0; j < 8; j++) {
              ipv6Segments.setAtIndex(ValueLayout.JAVA_SHORT, i * 8L + j, segments[j]);
            }
            ports.setAtIndex(ValueLayout.JAVA_SHORT, i, (short) ipv6Addr.getPort());
            flowInfo.setAtIndex(ValueLayout.JAVA_INT, i, ipv6Addr.getFlowInfo());
            scopeId.setAtIndex(ValueLayout.JAVA_INT, i, ipv6Addr.getScopeId());
          }
        } else {
          hasRemoteAddress.setAtIndex(ValueLayout.JAVA_INT, i, 0);
        }
      }

      // Call native function
      final int result =
          (int)
              SEND_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  (long) count,
                  datagramDataPtrs,
                  datagramLengths,
                  hasRemoteAddress,
                  isIpv4,
                  ipv4Octets,
                  ipv6Segments,
                  ports,
                  flowInfo,
                  scopeId,
                  outSentCount);

      if (result != 0) {
        throw new WasmException("Failed to send datagrams");
      }

      final long sentCount = outSentCount.get(ValueLayout.JAVA_LONG, 0);
      LOGGER.fine("Sent " + sentCount + " datagrams on UDP socket");
      return sentCount;

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error sending datagrams: " + e.getMessage(), e);
    }
  }

  @Override
  public void close() throws WasmException {
    resourceHandle.close();
  }

  // Helper class to hold address parameters for encoding
  private static final class AddressParams {
    final boolean isIpv4;
    final MemorySegment ipv4Octets;
    final MemorySegment ipv6Segments;
    final int port;
    final int flowInfo;
    final int scopeId;

    AddressParams(
        final boolean isIpv4,
        final MemorySegment ipv4Octets,
        final MemorySegment ipv6Segments,
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

  // Helper method to encode IpSocketAddress to memory segments
  private static AddressParams encodeAddress(final IpSocketAddress address, final Arena arena) {
    if (address.isIpv4()) {
      final Ipv4SocketAddress ipv4 = address.getIpv4();
      final byte[] octets = ipv4.getAddress().getOctets();
      final MemorySegment octetsSegment = arena.allocate(4);
      for (int i = 0; i < 4; i++) {
        octetsSegment.set(ValueLayout.JAVA_BYTE, i, octets[i]);
      }
      return new AddressParams(
          true,
          octetsSegment,
          MemorySegment.NULL,
          ipv4.getPort(),
          0, // IPv4 doesn't have flow info
          0 // IPv4 doesn't have scope ID
          );
    } else {
      final Ipv6SocketAddress ipv6 = address.getIpv6();
      final short[] segments = ipv6.getAddress().getSegments();
      final MemorySegment segmentsSegment = arena.allocate(ValueLayout.JAVA_SHORT, 8);
      for (int i = 0; i < 8; i++) {
        segmentsSegment.setAtIndex(ValueLayout.JAVA_SHORT, i, segments[i]);
      }
      return new AddressParams(
          false,
          MemorySegment.NULL,
          segmentsSegment,
          ipv6.getPort(),
          ipv6.getFlowInfo(),
          ipv6.getScopeId());
    }
  }

  // Helper method to decode memory segments to IpSocketAddress
  private static IpSocketAddress decodeAddress(
      final MemorySegment outIsIpv4,
      final MemorySegment outAddrBuf,
      final MemorySegment outPort,
      final MemorySegment outFlowInfo,
      final MemorySegment outScopeId) {
    final boolean isIpv4 = outIsIpv4.get(ValueLayout.JAVA_INT, 0) != 0;

    if (isIpv4) {
      final byte[] octets = new byte[4];
      for (int i = 0; i < 4; i++) {
        octets[i] = outAddrBuf.get(ValueLayout.JAVA_BYTE, i);
      }
      final int port = outPort.get(ValueLayout.JAVA_SHORT, 0) & 0xFFFF;

      final ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address ipv4Address =
          new ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address(octets);
      return IpSocketAddress.ipv4(new Ipv4SocketAddress(port, ipv4Address));
    } else {
      final short[] segments = new short[8];
      for (int i = 0; i < 8; i++) {
        segments[i] = outAddrBuf.getAtIndex(ValueLayout.JAVA_SHORT, i);
      }
      final int port = outPort.get(ValueLayout.JAVA_SHORT, 0) & 0xFFFF;
      final int flowInfo = outFlowInfo.get(ValueLayout.JAVA_INT, 0);
      final int scopeId = outScopeId.get(ValueLayout.JAVA_INT, 0);

      final ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address ipv6Address =
          new ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address(segments);
      return IpSocketAddress.ipv6(new Ipv6SocketAddress(port, flowInfo, ipv6Address, scopeId));
    }
  }
}
