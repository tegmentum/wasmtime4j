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

package ai.tegmentum.wasmtime4j.panama;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Native function bindings for WASI Preview 2 operations.
 *
 * <p>Provides type-safe wrappers for all WASI Preview 2 native functions including monotonic clock,
 * wall clock, random number generation, TCP sockets, and UDP sockets.
 */
public final class NativeWasiPreview2Bindings extends NativeBindingsBase {

  private static final Logger LOGGER = Logger.getLogger(NativeWasiPreview2Bindings.class.getName());

  private static volatile NativeWasiPreview2Bindings instance;
  private static final Object INSTANCE_LOCK = new Object();

  private NativeWasiPreview2Bindings() {
    super();
    initializeBindings();
    markInitialized();
    LOGGER.fine("Initialized NativeWasiPreview2Bindings successfully");
  }

  /**
   * Gets the singleton instance.
   *
   * @return the singleton instance
   * @throws RuntimeException if initialization fails
   */
  public static NativeWasiPreview2Bindings getInstance() {
    NativeWasiPreview2Bindings result = instance;
    if (result == null) {
      synchronized (INSTANCE_LOCK) {
        result = instance;
        if (result == null) {
          instance = result = new NativeWasiPreview2Bindings();
        }
      }
    }
    return result;
  }

  private void initializeBindings() {
    // WASI Preview 2 Monotonic Clock Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_now",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_time pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_resolution",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_resolution pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // when (nanoseconds)
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // duration (nanoseconds)
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    // WASI Preview 2 Wall Clock Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_wall_clock_now",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // out_seconds pointer (u64)
            ValueLayout.ADDRESS)); // out_nanoseconds pointer (u32)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_wall_clock_resolution",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // out_seconds pointer (u64)
            ValueLayout.ADDRESS)); // out_nanoseconds pointer (u32)

    // WASI Preview 2 Random Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_random_get_bytes",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS, // buffer pointer
            ValueLayout.JAVA_LONG, // length
            ValueLayout.ADDRESS)); // out_actual_length pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_random_get_u64",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.ADDRESS)); // out_value pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_random_free_buffer",
        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)); // buffer

    // WASI Preview 2 TCP Socket Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_INT, // is_ipv6 (boolean as int)
            ValueLayout.ADDRESS)); // out_handle pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_start_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // address (C string)
            ValueLayout.JAVA_INT)); // port

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_finish_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_start_connect",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // address (C string)
            ValueLayout.JAVA_INT)); // port

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_finish_connect",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_input_stream pointer (u64)
            ValueLayout.ADDRESS)); // out_output_stream pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_start_listen",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_finish_listen",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_accept",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_client_socket pointer (u64)
            ValueLayout.ADDRESS, // out_input_stream pointer (u64)
            ValueLayout.ADDRESS)); // out_output_stream pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_local_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_remote_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_address_family",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_family pointer (u8: 0=IPv4, 1=IPv6)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // backlog_size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // enabled (boolean as int)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // idle_time (nanoseconds)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // interval (nanoseconds)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // count

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_hop_limit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // hop_limit

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_subscribe",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_shutdown",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // shutdown_type (0=receive, 1=send, 2=both)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_tcp_socket_close",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    // WASI Preview 2 UDP Socket Functions
    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_create",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_INT, // is_ipv6 (boolean as int)
            ValueLayout.ADDRESS)); // out_handle pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_start_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // address (C string)
            ValueLayout.JAVA_INT)); // port

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_finish_bind",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_stream",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // remote_address (C string, nullable)
            ValueLayout.JAVA_INT)); // remote_port (-1 for no remote)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_local_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_remote_address",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // out_address buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_port pointer (u16)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_address_family",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_family pointer (u8: 0=IPv4, 1=IPv6)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_INT)); // hop_limit

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_size pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG)); // size

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_subscribe",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS)); // out_pollable pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_receive",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.JAVA_LONG, // max_datagrams
            ValueLayout.ADDRESS, // out_datagrams buffer
            ValueLayout.JAVA_LONG, // buffer_size
            ValueLayout.ADDRESS)); // out_count pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_send",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG, // socket_handle
            ValueLayout.ADDRESS, // datagrams buffer
            ValueLayout.JAVA_LONG, // count
            ValueLayout.ADDRESS)); // out_sent_count pointer (u64)

    addFunctionBinding(
        "wasmtime4j_panama_wasi_udp_socket_close",
        FunctionDescriptor.of(
            ValueLayout.JAVA_INT, // return code
            ValueLayout.JAVA_LONG)); // socket_handle
  }

  // ===== WASI Preview 2 Monotonic Clock Functions =====

  /**
   * Gets the current monotonic clock time in nanoseconds.
   *
   * @param outTime pointer to store the time value (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockNow(final MemorySegment outTime) {
    validatePointer(outTime, "outTime");
    return callNativeFunction("wasmtime4j_panama_wasi_monotonic_clock_now", Integer.class, outTime);
  }

  /**
   * Gets the monotonic clock resolution in nanoseconds.
   *
   * @param outResolution pointer to store the resolution value (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockResolution(final MemorySegment outResolution) {
    validatePointer(outResolution, "outResolution");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_monotonic_clock_resolution", Integer.class, outResolution);
  }

  /**
   * Subscribes to the monotonic clock for a specific instant.
   *
   * @param when the instant to subscribe to (nanoseconds)
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockSubscribeInstant(final long when, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant",
        Integer.class,
        when,
        outPollable);
  }

  /**
   * Subscribes to the monotonic clock for a duration.
   *
   * @param duration the duration to wait (nanoseconds)
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiMonotonicClockSubscribeDuration(
      final long duration, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration",
        Integer.class,
        duration,
        outPollable);
  }

  // ===== WASI Preview 2 Wall Clock Functions =====

  /**
   * Gets the current wall clock time.
   *
   * @param outSeconds pointer to store the seconds since Unix epoch (u64)
   * @param outNanoseconds pointer to store the nanoseconds within the second (u32)
   * @return 0 on success, non-zero on failure
   */
  public int wasiWallClockNow(final MemorySegment outSeconds, final MemorySegment outNanoseconds) {
    validatePointer(outSeconds, "outSeconds");
    validatePointer(outNanoseconds, "outNanoseconds");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_wall_clock_now", Integer.class, outSeconds, outNanoseconds);
  }

  /**
   * Gets the wall clock resolution.
   *
   * @param outSeconds pointer to store the seconds component (u64)
   * @param outNanoseconds pointer to store the nanoseconds component (u32)
   * @return 0 on success, non-zero on failure
   */
  public int wasiWallClockResolution(
      final MemorySegment outSeconds, final MemorySegment outNanoseconds) {
    validatePointer(outSeconds, "outSeconds");
    validatePointer(outNanoseconds, "outNanoseconds");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_wall_clock_resolution", Integer.class, outSeconds, outNanoseconds);
  }

  // ===== WASI Preview 2 Random Functions =====

  /**
   * Generates cryptographically secure random bytes.
   *
   * @param buffer pointer to the buffer to fill with random bytes
   * @param length number of random bytes to generate
   * @param outActualLength pointer to store the actual number of bytes generated (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiRandomGetBytes(
      final MemorySegment buffer, final long length, final MemorySegment outActualLength) {
    validatePointer(buffer, "buffer");
    validatePointer(outActualLength, "outActualLength");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_random_get_bytes", Integer.class, buffer, length, outActualLength);
  }

  /**
   * Generates a cryptographically secure random 64-bit value.
   *
   * @param outValue pointer to store the random value (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiRandomGetU64(final MemorySegment outValue) {
    validatePointer(outValue, "outValue");
    return callNativeFunction("wasmtime4j_panama_wasi_random_get_u64", Integer.class, outValue);
  }

  /**
   * Frees a buffer allocated by wasiRandomGetBytes.
   *
   * @param buffer pointer to the buffer to free
   */
  public void wasiRandomFreeBuffer(final MemorySegment buffer) {
    callNativeFunction("wasmtime4j_panama_wasi_random_free_buffer", Void.class, buffer);
  }

  // ===== WASI Preview 2 TCP Socket Functions =====

  /**
   * Creates a new TCP socket.
   *
   * @param isIpv6 true for IPv6, false for IPv4
   * @param outHandle pointer to store the socket handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketCreate(final boolean isIpv6, final MemorySegment outHandle) {
    validatePointer(outHandle, "outHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_create", Integer.class, isIpv6 ? 1 : 0, outHandle);
  }

  /**
   * Starts binding a TCP socket to an address.
   *
   * @param socketHandle the socket handle
   * @param address the address to bind to (C string)
   * @param port the port to bind to
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketStartBind(
      final long socketHandle, final MemorySegment address, final int port) {
    validatePointer(address, "address");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_start_bind", Integer.class, socketHandle, address, port);
  }

  /**
   * Finishes binding a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketFinishBind(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_finish_bind", Integer.class, socketHandle);
  }

  /**
   * Starts connecting a TCP socket to an address.
   *
   * @param socketHandle the socket handle
   * @param address the address to connect to (C string)
   * @param port the port to connect to
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketStartConnect(
      final long socketHandle, final MemorySegment address, final int port) {
    validatePointer(address, "address");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_start_connect",
        Integer.class,
        socketHandle,
        address,
        port);
  }

  /**
   * Finishes connecting a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outInputStream pointer to store the input stream handle (u64)
   * @param outOutputStream pointer to store the output stream handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketFinishConnect(
      final long socketHandle,
      final MemorySegment outInputStream,
      final MemorySegment outOutputStream) {
    validatePointer(outInputStream, "outInputStream");
    validatePointer(outOutputStream, "outOutputStream");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_finish_connect",
        Integer.class,
        socketHandle,
        outInputStream,
        outOutputStream);
  }

  /**
   * Starts listening on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketStartListen(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_start_listen", Integer.class, socketHandle);
  }

  /**
   * Finishes listening on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketFinishListen(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_finish_listen", Integer.class, socketHandle);
  }

  /**
   * Accepts an incoming connection on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outClientSocket pointer to store the client socket handle (u64)
   * @param outInputStream pointer to store the input stream handle (u64)
   * @param outOutputStream pointer to store the output stream handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketAccept(
      final long socketHandle,
      final MemorySegment outClientSocket,
      final MemorySegment outInputStream,
      final MemorySegment outOutputStream) {
    validatePointer(outClientSocket, "outClientSocket");
    validatePointer(outInputStream, "outInputStream");
    validatePointer(outOutputStream, "outOutputStream");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_accept",
        Integer.class,
        socketHandle,
        outClientSocket,
        outInputStream,
        outOutputStream);
  }

  /**
   * Gets the local address of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketLocalAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_local_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the remote address of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketRemoteAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_remote_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the address family of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outFamily pointer to store the family (0=IPv4, 1=IPv6)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketAddressFamily(final long socketHandle, final MemorySegment outFamily) {
    validatePointer(outFamily, "outFamily");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_address_family", Integer.class, socketHandle, outFamily);
  }

  /**
   * Sets the listen backlog size for a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param backlogSize the backlog size
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetListenBacklogSize(final long socketHandle, final long backlogSize) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_listen_backlog_size",
        Integer.class,
        socketHandle,
        backlogSize);
  }

  /**
   * Enables or disables keep-alive on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param enabled true to enable, false to disable
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveEnabled(final long socketHandle, final boolean enabled) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_enabled",
        Integer.class,
        socketHandle,
        enabled ? 1 : 0);
  }

  /**
   * Sets the keep-alive idle time on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param idleTime the idle time in nanoseconds
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveIdleTime(final long socketHandle, final long idleTime) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_idle_time",
        Integer.class,
        socketHandle,
        idleTime);
  }

  /**
   * Sets the keep-alive interval on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param interval the interval in nanoseconds
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveInterval(final long socketHandle, final long interval) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_interval",
        Integer.class,
        socketHandle,
        interval);
  }

  /**
   * Sets the keep-alive probe count on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param count the probe count
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetKeepAliveCount(final long socketHandle, final int count) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_keep_alive_count",
        Integer.class,
        socketHandle,
        count);
  }

  /**
   * Sets the hop limit (TTL) on a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param hopLimit the hop limit
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetHopLimit(final long socketHandle, final int hopLimit) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_hop_limit", Integer.class, socketHandle, hopLimit);
  }

  /**
   * Gets the receive buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketReceiveBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_receive_buffer_size",
        Integer.class,
        socketHandle,
        outSize);
  }

  /**
   * Sets the receive buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetReceiveBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_receive_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Gets the send buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSendBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_send_buffer_size", Integer.class, socketHandle, outSize);
  }

  /**
   * Sets the send buffer size of a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSetSendBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_set_send_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Creates a pollable for a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketSubscribe(final long socketHandle, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_subscribe", Integer.class, socketHandle, outPollable);
  }

  /**
   * Shuts down a TCP socket.
   *
   * @param socketHandle the socket handle
   * @param shutdownType 0=receive, 1=send, 2=both
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketShutdown(final long socketHandle, final int shutdownType) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_shutdown", Integer.class, socketHandle, shutdownType);
  }

  /**
   * Closes a TCP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiTcpSocketClose(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_tcp_socket_close", Integer.class, socketHandle);
  }

  // ===== WASI Preview 2 UDP Socket Functions =====

  /**
   * Creates a new UDP socket.
   *
   * @param isIpv6 true for IPv6, false for IPv4
   * @param outHandle pointer to store the socket handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketCreate(final boolean isIpv6, final MemorySegment outHandle) {
    validatePointer(outHandle, "outHandle");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_create", Integer.class, isIpv6 ? 1 : 0, outHandle);
  }

  /**
   * Starts binding a UDP socket to an address.
   *
   * @param socketHandle the socket handle
   * @param address the address to bind to (C string)
   * @param port the port to bind to
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketStartBind(
      final long socketHandle, final MemorySegment address, final int port) {
    validatePointer(address, "address");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_start_bind", Integer.class, socketHandle, address, port);
  }

  /**
   * Finishes binding a UDP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketFinishBind(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_finish_bind", Integer.class, socketHandle);
  }

  /**
   * Sets the remote address for a connected UDP socket.
   *
   * @param socketHandle the socket handle
   * @param remoteAddress the remote address (C string, or NULL)
   * @param remotePort the remote port (-1 to clear)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketStream(
      final long socketHandle, final MemorySegment remoteAddress, final int remotePort) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_stream",
        Integer.class,
        socketHandle,
        remoteAddress,
        remotePort);
  }

  /**
   * Gets the local address of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketLocalAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_local_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the remote address of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outAddress buffer to store the address string
   * @param bufferSize size of the buffer
   * @param outPort pointer to store the port (u16)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketRemoteAddress(
      final long socketHandle,
      final MemorySegment outAddress,
      final long bufferSize,
      final MemorySegment outPort) {
    validatePointer(outAddress, "outAddress");
    validatePointer(outPort, "outPort");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_remote_address",
        Integer.class,
        socketHandle,
        outAddress,
        bufferSize,
        outPort);
  }

  /**
   * Gets the address family of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outFamily pointer to store the family (0=IPv4, 1=IPv6)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketAddressFamily(final long socketHandle, final MemorySegment outFamily) {
    validatePointer(outFamily, "outFamily");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_address_family", Integer.class, socketHandle, outFamily);
  }

  /**
   * Sets the unicast hop limit (TTL) on a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param hopLimit the hop limit
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSetUnicastHopLimit(final long socketHandle, final int hopLimit) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_set_unicast_hop_limit",
        Integer.class,
        socketHandle,
        hopLimit);
  }

  /**
   * Gets the receive buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketReceiveBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_receive_buffer_size",
        Integer.class,
        socketHandle,
        outSize);
  }

  /**
   * Sets the receive buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSetReceiveBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_set_receive_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Gets the send buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outSize pointer to store the size (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSendBufferSize(final long socketHandle, final MemorySegment outSize) {
    validatePointer(outSize, "outSize");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_send_buffer_size", Integer.class, socketHandle, outSize);
  }

  /**
   * Sets the send buffer size of a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param size the buffer size
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSetSendBufferSize(final long socketHandle, final long size) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_set_send_buffer_size",
        Integer.class,
        socketHandle,
        size);
  }

  /**
   * Creates a pollable for a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param outPollable pointer to store the pollable handle (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSubscribe(final long socketHandle, final MemorySegment outPollable) {
    validatePointer(outPollable, "outPollable");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_subscribe", Integer.class, socketHandle, outPollable);
  }

  /**
   * Receives datagrams from a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param maxDatagrams maximum number of datagrams to receive
   * @param outDatagrams buffer to store the datagrams
   * @param bufferSize size of the buffer
   * @param outCount pointer to store the number of datagrams received (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketReceive(
      final long socketHandle,
      final long maxDatagrams,
      final MemorySegment outDatagrams,
      final long bufferSize,
      final MemorySegment outCount) {
    validatePointer(outDatagrams, "outDatagrams");
    validatePointer(outCount, "outCount");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_receive",
        Integer.class,
        socketHandle,
        maxDatagrams,
        outDatagrams,
        bufferSize,
        outCount);
  }

  /**
   * Sends datagrams on a UDP socket.
   *
   * @param socketHandle the socket handle
   * @param datagrams buffer containing the datagrams to send
   * @param count number of datagrams to send
   * @param outSentCount pointer to store the number of datagrams sent (u64)
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketSend(
      final long socketHandle,
      final MemorySegment datagrams,
      final long count,
      final MemorySegment outSentCount) {
    validatePointer(datagrams, "datagrams");
    validatePointer(outSentCount, "outSentCount");
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_send",
        Integer.class,
        socketHandle,
        datagrams,
        count,
        outSentCount);
  }

  /**
   * Closes a UDP socket.
   *
   * @param socketHandle the socket handle
   * @return 0 on success, non-zero on failure
   */
  public int wasiUdpSocketClose(final long socketHandle) {
    return callNativeFunction(
        "wasmtime4j_panama_wasi_udp_socket_close", Integer.class, socketHandle);
  }
}
