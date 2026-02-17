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
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiInputStream;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiOutputStream;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpSocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6SocketAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiTcpSocket;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiTcpSocket interface.
 *
 * <p>This class provides access to WASI Preview 2 TCP socket operations through Panama Foreign
 * Function API calls to the native Wasmtime library. TCP sockets provide connection-oriented
 * reliable byte stream communication.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/tcp@0.2.0
 *
 * @since 1.0.0
 */
public final class PanamaWasiTcpSocket implements WasiTcpSocket {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiTcpSocket.class.getName());

  // Panama FFI function handles - connection management
  private static final MethodHandle CREATE_HANDLE;
  private static final MethodHandle START_BIND_HANDLE;
  private static final MethodHandle FINISH_BIND_HANDLE;
  private static final MethodHandle START_CONNECT_HANDLE;
  private static final MethodHandle FINISH_CONNECT_HANDLE;
  private static final MethodHandle START_LISTEN_HANDLE;
  private static final MethodHandle FINISH_LISTEN_HANDLE;
  private static final MethodHandle ACCEPT_HANDLE;

  // Panama FFI function handles - address queries
  private static final MethodHandle LOCAL_ADDRESS_HANDLE;
  private static final MethodHandle REMOTE_ADDRESS_HANDLE;
  private static final MethodHandle ADDRESS_FAMILY_HANDLE;

  // Panama FFI function handles - configuration
  private static final MethodHandle SET_LISTEN_BACKLOG_SIZE_HANDLE;
  private static final MethodHandle SET_KEEP_ALIVE_ENABLED_HANDLE;
  private static final MethodHandle SET_KEEP_ALIVE_IDLE_TIME_HANDLE;
  private static final MethodHandle SET_KEEP_ALIVE_INTERVAL_HANDLE;
  private static final MethodHandle SET_KEEP_ALIVE_COUNT_HANDLE;
  private static final MethodHandle SET_HOP_LIMIT_HANDLE;

  // Panama FFI function handles - buffer management
  private static final MethodHandle RECEIVE_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SET_RECEIVE_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SEND_BUFFER_SIZE_HANDLE;
  private static final MethodHandle SET_SEND_BUFFER_SIZE_HANDLE;

  // Panama FFI function handles - operations
  private static final MethodHandle SUBSCRIBE_HANDLE;
  private static final MethodHandle SHUTDOWN_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_tcp_socket_create(context_handle, is_ipv6, out_handle)
      CREATE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_start_bind(context_handle, socket_handle, is_ipv4,
      // ipv4_octets, ipv6_segments, port, flow_info, scope_id)
      START_BIND_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_start_bind").orElseThrow(),
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

      // int wasmtime4j_panama_wasi_tcp_socket_finish_bind(context_handle, socket_handle)
      FINISH_BIND_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_finish_bind").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_start_connect(context_handle, socket_handle,
      // is_ipv4, ipv4_octets, ipv6_segments, port, flow_info, scope_id)
      START_CONNECT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_start_connect").orElseThrow(),
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

      // int wasmtime4j_panama_wasi_tcp_socket_finish_connect(context_handle, socket_handle,
      // out_input_handle, out_output_handle)
      FINISH_CONNECT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_finish_connect").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_start_listen(context_handle, socket_handle)
      START_LISTEN_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_start_listen").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_finish_listen(context_handle, socket_handle)
      FINISH_LISTEN_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_finish_listen").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_accept(context_handle, socket_handle,
      // out_socket_handle, out_input_handle, out_output_handle)
      ACCEPT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_accept").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_local_address(context_handle, socket_handle,
      // out_is_ipv4, out_addr_buf, out_port, out_flow_info, out_scope_id)
      LOCAL_ADDRESS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_local_address").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_remote_address(context_handle, socket_handle,
      // out_is_ipv4, out_addr_buf, out_port, out_flow_info, out_scope_id)
      REMOTE_ADDRESS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_remote_address").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_address_family(context_handle, socket_handle,
      // out_is_ipv6)
      ADDRESS_FAMILY_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_address_family").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size(context_handle,
      // socket_handle, value)
      SET_LISTEN_BACKLOG_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled(context_handle,
      // socket_handle, value)
      SET_KEEP_ALIVE_ENABLED_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT));

      // int wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time(context_handle,
      // socket_handle, value)
      SET_KEEP_ALIVE_IDLE_TIME_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval(context_handle,
      // socket_handle, value)
      SET_KEEP_ALIVE_INTERVAL_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count(context_handle,
      // socket_handle, value)
      SET_KEEP_ALIVE_COUNT_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT));

      // int wasmtime4j_panama_wasi_tcp_socket_set_hop_limit(context_handle, socket_handle, value)
      SET_HOP_LIMIT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_set_hop_limit").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT));

      // int wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size(context_handle, socket_handle,
      // out_size)
      RECEIVE_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size(context_handle,
      // socket_handle, value)
      SET_RECEIVE_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_send_buffer_size(context_handle, socket_handle,
      // out_size)
      SEND_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_send_buffer_size").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size(context_handle, socket_handle,
      // value)
      SET_SEND_BUFFER_SIZE_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_tcp_socket_subscribe(context_handle, socket_handle,
      // out_pollable_handle)
      SUBSCRIBE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_subscribe").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_tcp_socket_shutdown(context_handle, socket_handle,
      // shutdown_type)
      SHUTDOWN_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_shutdown").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT));

      // void wasmtime4j_panama_wasi_tcp_socket_close(context_handle, socket_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_tcp_socket_close").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiTcpSocket: " + e.getMessage());
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
   * Creates a new Panama WASI TCP socket with the given address family.
   *
   * @param contextHandle the native context handle
   * @param addressFamily the IP address family (IPv4 or IPv6)
   * @return a new WasiTcpSocket instance
   * @throws IllegalArgumentException if context handle is null or address family is null
   * @throws WasmException if socket creation fails
   */
  public static PanamaWasiTcpSocket create(
      final MemorySegment contextHandle, final IpAddressFamily addressFamily) throws WasmException {
    Validation.requireNonNull(contextHandle, "contextHandle");
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
        throw new WasmException("Failed to create TCP socket");
      }

      final long socketHandle = outHandle.get(ValueLayout.JAVA_LONG, 0);
      if (socketHandle <= 0) {
        throw new WasmException("Failed to create TCP socket");
      }

      return new PanamaWasiTcpSocket(contextHandle, socketHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error creating TCP socket: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new Panama WASI TCP socket with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param socketHandle the native socket handle
   * @throws IllegalArgumentException if context handle is null or socket handle is invalid
   */
  PanamaWasiTcpSocket(final MemorySegment contextHandle, final long socketHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
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
            "PanamaWasiTcpSocket",
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetySocket);
              } catch (final Throwable t) {
                throw new Exception("Error closing TCP socket handle: " + safetySocket, t);
              }
            },
            this,
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetySocket);
              } catch (final Throwable t) {
                LOGGER.warning(
                    "Safety net failed to close TCP socket handle " + safetySocket + ": " + t);
              }
            });

    LOGGER.fine(
        "Created Panama WASI TCP socket with context handle: "
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
  public void startConnect(final WasiNetwork network, final IpSocketAddress remoteAddress)
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
              START_CONNECT_HANDLE.invoke(
                  contextHandle,
                  socketHandle,
                  params.isIpv4 ? 1 : 0,
                  params.ipv4Octets,
                  params.ipv6Segments,
                  (short) params.port,
                  params.flowInfo,
                  params.scopeId);

      if (result != 0) {
        throw new WasmException("Failed to start connect");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error starting connect: " + e.getMessage(), e);
    }
  }

  @Override
  public ConnectionStreams finishConnect() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outInputHandle = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outOutputHandle = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              FINISH_CONNECT_HANDLE.invoke(
                  contextHandle, socketHandle, outInputHandle, outOutputHandle);

      if (result != 0) {
        throw new WasmException("Failed to finish connect");
      }

      final long inputHandle = outInputHandle.get(ValueLayout.JAVA_LONG, 0);
      final long outputHandle = outOutputHandle.get(ValueLayout.JAVA_LONG, 0);

      if (inputHandle <= 0 || outputHandle <= 0) {
        throw new WasmException("Failed to create streams");
      }

      return new ConnectionStreams(
          new PanamaWasiInputStream(contextHandle, MemorySegment.ofAddress(inputHandle)),
          new PanamaWasiOutputStream(contextHandle, MemorySegment.ofAddress(outputHandle)));

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error finishing connect: " + e.getMessage(), e);
    }
  }

  @Override
  public void startListen() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result = (int) START_LISTEN_HANDLE.invoke(contextHandle, socketHandle);

      if (result != 0) {
        throw new WasmException("Failed to start listen");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error starting listen: " + e.getMessage(), e);
    }
  }

  @Override
  public void finishListen() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result = (int) FINISH_LISTEN_HANDLE.invoke(contextHandle, socketHandle);

      if (result != 0) {
        throw new WasmException("Failed to finish listen");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error finishing listen: " + e.getMessage(), e);
    }
  }

  @Override
  public AcceptResult accept() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSocketHandle = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outInputHandle = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outOutputHandle = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              ACCEPT_HANDLE.invoke(
                  contextHandle, socketHandle, outSocketHandle, outInputHandle, outOutputHandle);

      if (result != 0) {
        throw new WasmException("Failed to accept connection");
      }

      final long newSocketHandle = outSocketHandle.get(ValueLayout.JAVA_LONG, 0);
      final long inputHandle = outInputHandle.get(ValueLayout.JAVA_LONG, 0);
      final long outputHandle = outOutputHandle.get(ValueLayout.JAVA_LONG, 0);

      if (newSocketHandle <= 0 || inputHandle <= 0 || outputHandle <= 0) {
        throw new WasmException("Invalid handles returned");
      }

      return new AcceptResult(
          new PanamaWasiTcpSocket(contextHandle, newSocketHandle),
          new PanamaWasiInputStream(contextHandle, MemorySegment.ofAddress(inputHandle)),
          new PanamaWasiOutputStream(contextHandle, MemorySegment.ofAddress(outputHandle)));

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error accepting connection: " + e.getMessage(), e);
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
  public void setListenBacklogSize(final long value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_LISTEN_BACKLOG_SIZE_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set listen backlog size");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting listen backlog size: " + e.getMessage(), e);
    }
  }

  @Override
  public void setKeepAliveEnabled(final boolean value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_KEEP_ALIVE_ENABLED_HANDLE.invoke(contextHandle, socketHandle, value ? 1 : 0);

      if (result != 0) {
        throw new WasmException("Failed to set keep alive enabled");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting keep alive enabled: " + e.getMessage(), e);
    }
  }

  @Override
  public void setKeepAliveIdleTime(final long value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_KEEP_ALIVE_IDLE_TIME_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set keep alive idle time");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting keep alive idle time: " + e.getMessage(), e);
    }
  }

  @Override
  public void setKeepAliveInterval(final long value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_KEEP_ALIVE_INTERVAL_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set keep alive interval");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting keep alive interval: " + e.getMessage(), e);
    }
  }

  @Override
  public void setKeepAliveCount(final int value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result =
          (int) SET_KEEP_ALIVE_COUNT_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set keep alive count");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting keep alive count: " + e.getMessage(), e);
    }
  }

  @Override
  public void setHopLimit(final int value) throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new WasmException("Socket is closed");
    }

    try {
      final int result = (int) SET_HOP_LIMIT_HANDLE.invoke(contextHandle, socketHandle, value);

      if (result != 0) {
        throw new WasmException("Failed to set hop limit");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error setting hop limit: " + e.getMessage(), e);
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
  public void shutdown(final ShutdownType shutdownType) throws WasmException {
    if (shutdownType == null) {
      throw new IllegalArgumentException("Shutdown type cannot be null");
    }
    if (resourceHandle.isClosed()) {
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

    try {
      final int result = (int) SHUTDOWN_HANDLE.invoke(contextHandle, socketHandle, type);

      if (result != 0) {
        throw new WasmException("Failed to shutdown socket");
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error shutting down socket: " + e.getMessage(), e);
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
