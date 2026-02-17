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
 * Panama FFI implementation of the WasiNetwork interface.
 *
 * <p>This class provides access to WASI Preview 2 network operations through Panama Foreign
 * Function API calls to the native Wasmtime library. The network resource represents an opaque
 * handle to a network instance and is used to group sockets into a "network".
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 *
 * @since 1.0.0
 */
public final class PanamaWasiNetwork implements WasiNetwork {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiNetwork.class.getName());

  // Panama FFI function handles
  private static final MethodHandle CREATE_HANDLE;
  private static final MethodHandle CLOSE_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_network_create(context_handle, out_network_handle)
      CREATE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_network_create").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // void wasmtime4j_panama_wasi_network_close(context_handle, network_handle)
      CLOSE_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_network_close").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiNetwork: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /** The native network handle. */
  private final long networkHandle;

  /** Resource lifecycle handle. */
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama WASI network with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @return a new WasiNetwork instance
   * @throws IllegalArgumentException if context handle is null
   * @throws WasmException if network creation fails
   */
  public static PanamaWasiNetwork create(final MemorySegment contextHandle) throws WasmException {
    Validation.requireNonNull(contextHandle, "contextHandle");

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outNetworkHandle = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) CREATE_HANDLE.invoke(contextHandle, outNetworkHandle);

      if (result != 0) {
        throw new WasmException("Failed to create network resource");
      }

      final long networkHandle = outNetworkHandle.get(ValueLayout.JAVA_LONG, 0);
      if (networkHandle <= 0) {
        throw new WasmException("Failed to create network resource");
      }

      return new PanamaWasiNetwork(contextHandle, networkHandle);

    } catch (final WasmException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error creating network resource: " + e.getMessage(), e);
    }
  }

  /**
   * Creates a new Panama WASI network with the given native context and network handles.
   *
   * @param contextHandle the native context handle
   * @param networkHandle the native network handle
   * @throws IllegalArgumentException if context handle is null or network handle is invalid
   */
  private PanamaWasiNetwork(final MemorySegment contextHandle, final long networkHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    if (networkHandle <= 0) {
      throw new IllegalArgumentException("Network handle must be positive: " + networkHandle);
    }
    this.contextHandle = contextHandle;
    this.networkHandle = networkHandle;

    // Capture handle values for safety net (must not capture 'this')
    final MemorySegment safetyCtx = contextHandle;
    final long safetyNetwork = networkHandle;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWasiNetwork",
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetyNetwork);
              } catch (final Throwable t) {
                throw new Exception("Error closing network handle: " + safetyNetwork, t);
              }
            },
            this,
            () -> {
              try {
                CLOSE_HANDLE.invoke(safetyCtx, safetyNetwork);
              } catch (final Throwable t) {
                LOGGER.warning(
                    "Safety net failed to close network handle " + safetyNetwork + ": " + t);
              }
            });

    LOGGER.fine(
        "Created Panama WASI network with context handle: "
            + contextHandle
            + ", network handle: "
            + networkHandle);
  }

  @Override
  public void close() throws WasmException {
    resourceHandle.close();
  }

  /**
   * Gets the network handle for use by socket implementations.
   *
   * @return the native network handle
   */
  public long getNetworkHandle() {
    return networkHandle;
  }
}
