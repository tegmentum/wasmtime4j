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
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiIpNameLookup;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiIpNameLookup interface.
 *
 * <p>This class provides DNS name resolution capabilities through Panama Foreign Function API calls
 * to the native Wasmtime library. It resolves hostnames to IP addresses using the WASI Preview 2
 * ip-name-lookup interface.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/ip-name-lookup@0.2.0
 *
 * @since 1.0.0
 */
public final class PanamaWasiIpNameLookup implements WasiIpNameLookup {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiIpNameLookup.class.getName());

  // Panama FFI function handle
  private static final MethodHandle RESOLVE_ADDRESSES_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeResourceHandle.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // long wasmtime4j_panama_wasi_resolve_addresses(context_handle, network_handle, hostname,
      // hostname_len, address_family)
      RESOLVE_ADDRESSES_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_resolve_addresses").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_BYTE));

    } catch (final Throwable e) {
      LOGGER.severe(
          "Failed to initialize Panama FFI handles for WasiIpNameLookup: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI IP name lookup with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiIpNameLookup(final MemorySegment contextHandle) {
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI IP name lookup with context handle: " + contextHandle);
  }

  @Override
  public ResolveAddressStream resolveAddresses(final WasiNetwork network, final String name)
      throws WasmException {
    return resolveAddresses(network, name, null);
  }

  @Override
  public ResolveAddressStream resolveAddresses(
      final WasiNetwork network, final String name, final IpAddressFamily addressFamily)
      throws WasmException {
    if (network == null) {
      throw new IllegalArgumentException("Network cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }

    // Get the network handle
    long networkHandle = 0;
    if (network instanceof PanamaWasiNetwork) {
      networkHandle = ((PanamaWasiNetwork) network).getNetworkHandle();
    }

    // Convert address family to native code: 0 = all, 4 = IPv4, 6 = IPv6
    byte familyCode = 0;
    if (addressFamily != null) {
      if (addressFamily == IpAddressFamily.IPV4) {
        familyCode = 4;
      } else if (addressFamily == IpAddressFamily.IPV6) {
        familyCode = 6;
      }
    }

    LOGGER.fine(
        "Resolving addresses for hostname: "
            + name
            + " with family: "
            + (addressFamily == null ? "all" : addressFamily));

    try (final Arena arena = Arena.ofConfined()) {
      // Allocate string as native memory
      final byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.UTF_8);
      final MemorySegment nameSegment = arena.allocate(nameBytes.length);
      nameSegment.copyFrom(MemorySegment.ofArray(nameBytes));

      final long streamHandle =
          (long)
              RESOLVE_ADDRESSES_HANDLE.invoke(
                  contextHandle, networkHandle, nameSegment, (long) nameBytes.length, familyCode);

      if (streamHandle <= 0) {
        throw new WasmException("Failed to initiate DNS resolution for hostname: " + name);
      }

      return new PanamaResolveAddressStream(contextHandle, streamHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error resolving addresses: " + e.getMessage(), e);
    }
  }
}
