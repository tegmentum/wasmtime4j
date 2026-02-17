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

package ai.tegmentum.wasmtime4j.panama.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddress;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv4Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.Ipv6Address;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the ResolveAddressStream interface.
 *
 * <p>This class provides access to resolved IP addresses from a DNS lookup through Panama Foreign
 * Function API calls to the native Wasmtime library. The stream returns IP addresses one at a time
 * and can be iterated until exhausted.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/ip-name-lookup@0.2.0
 *
 * @since 1.0.0
 */
public final class PanamaResolveAddressStream implements ResolveAddressStream {

  private static final Logger LOGGER = Logger.getLogger(PanamaResolveAddressStream.class.getName());

  // Panama FFI function handles
  private static final MethodHandle GET_NEXT_ADDRESS_HANDLE;
  private static final MethodHandle SUBSCRIBE_HANDLE;
  private static final MethodHandle IS_CLOSED_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_resolve_stream_next(context_handle, stream_handle, out_result)
      // out_result is int[14]: [hasAddress, isIpv4, ipv4[4], ipv6[8]]
      GET_NEXT_ADDRESS_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_resolve_stream_next").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // void wasmtime4j_panama_wasi_resolve_stream_subscribe(context_handle, stream_handle)
      SUBSCRIBE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_resolve_stream_subscribe").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // int wasmtime4j_panama_wasi_resolve_stream_is_closed(context_handle, stream_handle)
      IS_CLOSED_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_resolve_stream_is_closed").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

      // void wasmtime4j_panama_wasi_resolve_stream_close(context_handle, stream_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_resolve_stream_close").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    } catch (final Throwable e) {
      LOGGER.severe(
          "Failed to initialize Panama FFI handles for ResolveAddressStream: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /** The native stream handle. */
  private final long streamHandle;

  /** Resource lifecycle handle. */
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama resolve address stream with the given handles.
   *
   * @param contextHandle the native context handle
   * @param streamHandle the native stream handle
   * @throws IllegalArgumentException if context handle is null or stream handle is invalid
   */
  PanamaResolveAddressStream(final MemorySegment contextHandle, final long streamHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    if (streamHandle <= 0) {
      throw new IllegalArgumentException("Stream handle must be positive: " + streamHandle);
    }
    this.contextHandle = contextHandle;
    this.streamHandle = streamHandle;

    // Capture handle values for safety net (must not capture 'this')
    final MemorySegment safetyCtx = contextHandle;
    final long safetyStream = streamHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaResolveAddressStream",
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetyStream);
              } catch (final Throwable t) {
                throw new Exception("Error closing resolve stream handle: " + safetyStream, t);
              }
            },
            this,
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetyStream);
              } catch (final Throwable t) {
                LOGGER.warning(
                    "Safety net failed to close resolve stream handle " + safetyStream + ": " + t);
              }
            });

    LOGGER.fine(
        "Created Panama resolve address stream with context handle: "
            + contextHandle
            + ", stream handle: "
            + streamHandle);
  }

  @Override
  public Optional<IpAddress> resolveNextAddress() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new IllegalStateException("Stream has been closed");
    }

    try (final Arena arena = Arena.ofConfined()) {
      // Allocate result array: [hasAddress, isIpv4, ipv4_b0-b3, ipv6_s0-s7]
      final MemorySegment resultSegment = arena.allocate(ValueLayout.JAVA_INT, 14);

      final int status =
          (int) GET_NEXT_ADDRESS_HANDLE.invoke(contextHandle, streamHandle, resultSegment);

      if (status != 0) {
        throw new WasmException("Failed to get next address from stream");
      }

      final int hasAddress = resultSegment.get(ValueLayout.JAVA_INT, 0);
      if (hasAddress == 0) {
        // No more addresses
        return Optional.empty();
      }

      final int isIpv4 = resultSegment.get(ValueLayout.JAVA_INT, 4);

      if (isIpv4 == 1) {
        final byte[] ipv4Bytes = new byte[4];
        ipv4Bytes[0] = (byte) resultSegment.get(ValueLayout.JAVA_INT, 8);
        ipv4Bytes[1] = (byte) resultSegment.get(ValueLayout.JAVA_INT, 12);
        ipv4Bytes[2] = (byte) resultSegment.get(ValueLayout.JAVA_INT, 16);
        ipv4Bytes[3] = (byte) resultSegment.get(ValueLayout.JAVA_INT, 20);
        LOGGER.fine(
            "Resolved IPv4 address: "
                + (ipv4Bytes[0] & 0xFF)
                + "."
                + (ipv4Bytes[1] & 0xFF)
                + "."
                + (ipv4Bytes[2] & 0xFF)
                + "."
                + (ipv4Bytes[3] & 0xFF));
        return Optional.of(IpAddress.ipv4(new Ipv4Address(ipv4Bytes)));
      } else {
        final short[] ipv6Segments = new short[8];
        for (int i = 0; i < 8; i++) {
          ipv6Segments[i] = (short) resultSegment.get(ValueLayout.JAVA_INT, 24 + i * 4);
        }
        LOGGER.fine("Resolved IPv6 address with first segment: " + (ipv6Segments[0] & 0xFFFF));
        return Optional.of(IpAddress.ipv6(new Ipv6Address(ipv6Segments)));
      }

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting next address: " + e.getMessage(), e);
    }
  }

  @Override
  public void subscribe() throws WasmException {
    if (resourceHandle.isClosed()) {
      throw new IllegalStateException("Stream has been closed");
    }

    try {
      SUBSCRIBE_HANDLE.invoke(contextHandle, streamHandle);
    } catch (final Throwable e) {
      throw new RuntimeException("Error subscribing to stream: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean isClosed() {
    if (resourceHandle.isClosed()) {
      return true;
    }

    try {
      final int result = (int) IS_CLOSED_HANDLE.invoke(contextHandle, streamHandle);
      return result != 0;
    } catch (final Throwable e) {
      LOGGER.warning("Error checking if stream is closed: " + e.getMessage());
      return false;
    }
  }

  @Override
  public void close() throws WasmException {
    resourceHandle.close();
  }
}
