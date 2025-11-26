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
import ai.tegmentum.wasmtime4j.wasi.sockets.WasiNetwork;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiNetwork interface.
 *
 * <p>This class provides access to WASI Preview 2 network operations through JNI calls to the
 * native Wasmtime library. The network resource represents an opaque handle to a network instance
 * and is used to group sockets into a "network".
 *
 * <p>WASI Preview 2 specification: wasi:sockets/network@0.2.0
 *
 * @since 1.0.0
 */
public final class JniWasiNetwork implements WasiNetwork {

  private static final Logger LOGGER = Logger.getLogger(JniWasiNetwork.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiNetwork: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /** The native network handle. */
  private final long networkHandle;

  /** Whether this network has been closed. */
  private boolean closed = false;

  /**
   * Creates a new JNI WASI network with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @return a new WasiNetwork instance
   * @throws IllegalArgumentException if context handle is 0
   * @throws WasmException if network creation fails
   */
  public static JniWasiNetwork create(final long contextHandle) throws WasmException {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }

    final long networkHandle = nativeCreate(contextHandle);
    if (networkHandle <= 0) {
      throw new WasmException("Failed to create network resource");
    }

    return new JniWasiNetwork(contextHandle, networkHandle);
  }

  /**
   * Creates a new JNI WASI network with the given native context and network handles.
   *
   * @param contextHandle the native context handle
   * @param networkHandle the native network handle
   * @throws IllegalArgumentException if context handle is 0 or network handle is invalid
   */
  private JniWasiNetwork(final long contextHandle, final long networkHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    if (networkHandle <= 0) {
      throw new IllegalArgumentException("Network handle must be positive: " + networkHandle);
    }
    this.contextHandle = contextHandle;
    this.networkHandle = networkHandle;
    LOGGER.fine(
        "Created JNI WASI network with context handle: "
            + contextHandle
            + ", network handle: "
            + networkHandle);
  }

  @Override
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    nativeClose(contextHandle, networkHandle);
    closed = true;
    LOGGER.fine("Closed JNI WASI network with handle: " + networkHandle);
  }

  /**
   * Gets the network handle for use by socket implementations.
   *
   * @return the native network handle
   */
  public long getNetworkHandle() {
    return networkHandle;
  }

  /**
   * Native method to create a network resource.
   *
   * @param contextHandle the native context handle
   * @return the native network handle
   */
  private static native long nativeCreate(long contextHandle);

  /**
   * Native method to close a network resource.
   *
   * @param contextHandle the native context handle
   * @param networkHandle the native network handle
   */
  private static native void nativeClose(long contextHandle, long networkHandle);
}
