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
package ai.tegmentum.wasmtime4j.jni.wasi.sockets;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.sockets.IpAddressFamily;
import ai.tegmentum.wasmtime4j.wasi.sockets.ResolveAddressStream;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiIpNameLookup;
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiIpNameLookup interface.
 *
 * <p>This class provides DNS name resolution capabilities through JNI calls to the native Wasmtime
 * library. It resolves hostnames to IP addresses using the WASI Preview 2 ip-name-lookup interface.
 *
 * <p>WASI Preview 2 specification: wasi:sockets/ip-name-lookup@0.2.0
 *
 * @since 1.0.0
 */
public final class JniWasiIpNameLookup implements WasiIpNameLookup {

  private static final Logger LOGGER = Logger.getLogger(JniWasiIpNameLookup.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiIpNameLookup: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI IP name lookup with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiIpNameLookup(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI IP name lookup with context handle: " + contextHandle);
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
    if (network instanceof JniWasiNetwork) {
      networkHandle = ((JniWasiNetwork) network).getNetworkHandle();
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

    final long streamHandle =
        nativeResolveAddresses(contextHandle, networkHandle, name, familyCode);
    if (streamHandle <= 0) {
      throw new WasmException("Failed to initiate DNS resolution for hostname: " + name);
    }

    return new JniResolveAddressStream(contextHandle, streamHandle);
  }

  /**
   * Native method to initiate DNS resolution.
   *
   * @param contextHandle the native context handle
   * @param networkHandle the native network handle
   * @param hostname the hostname to resolve
   * @param addressFamily the address family filter (0=all, 4=IPv4, 6=IPv6)
   * @return the stream handle for retrieving addresses
   */
  private static native long nativeResolveAddresses(
      long contextHandle, long networkHandle, String hostname, byte addressFamily);
}
