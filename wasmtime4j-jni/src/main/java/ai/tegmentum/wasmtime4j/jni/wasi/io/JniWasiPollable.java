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
package ai.tegmentum.wasmtime4j.jni.wasi.io;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiPollable interface.
 *
 * <p>This class provides access to WASI Preview 2 pollable operations through JNI calls to the
 * native Wasmtime library. Pollables represent events that can be waited on using poll operations.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @implNote Pollable operations ({@link #block()}, {@link #ready()}, and {@link #close()}) are not
 *     yet implemented in the JNI runtime and will throw {@link WasmException} when called. The
 *     Panama runtime has partial real pollable support; this JNI implementation will be enhanced in
 *     a future release.
 * @since 1.0.0
 */
public final class JniWasiPollable extends JniResource implements WasiPollable {

  private static final Logger LOGGER = Logger.getLogger(JniWasiPollable.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiPollable: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI pollable with the given native handles.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws IllegalArgumentException if either handle is 0
   */
  public JniWasiPollable(final long contextHandle, final long pollableHandle) {
    super(pollableHandle);
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    if (LOGGER.isLoggable(java.util.logging.Level.FINE)) {
      LOGGER.fine("Created JNI WASI pollable with handle: " + pollableHandle);
    }
  }

  @Override
  public void block() throws WasmException {
    beginOperation();
    try {
      nativeBlock(contextHandle, nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  public boolean ready() throws WasmException {
    beginOperation();
    try {
      return nativeReady(contextHandle, nativeHandle);
    } finally {
      endOperation();
    }
  }

  @Override
  protected void doClose() throws Exception {
    nativeClose(contextHandle, nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "WasiPollable";
  }

  // Native method declarations

  /**
   * Blocks until this pollable is ready.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws WasmException if the wait operation fails
   */
  private static native void nativeBlock(long contextHandle, long pollableHandle)
      throws WasmException;

  /**
   * Checks if this pollable is currently ready without blocking.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @return true if the pollable is ready, false otherwise
   * @throws WasmException if the ready check fails
   */
  private static native boolean nativeReady(long contextHandle, long pollableHandle)
      throws WasmException;

  /**
   * Closes the pollable.
   *
   * @param contextHandle the native context handle
   * @param pollableHandle the native pollable handle
   * @throws WasmException if the close operation fails
   */
  private static native void nativeClose(long contextHandle, long pollableHandle)
      throws WasmException;
}
