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

package ai.tegmentum.wasmtime4j.jni.wasi.cli;

import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiInputStream;
import ai.tegmentum.wasmtime4j.jni.wasi.io.JniWasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.cli.WasiStdio;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiStdio interface.
 *
 * <p>This class provides access to WASI Preview 2 standard I/O streams through JNI calls to the
 * native Wasmtime library.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiStdio implements WasiStdio {

  private static final Logger LOGGER = Logger.getLogger(JniWasiStdio.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiStdio: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI stdio with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiStdio(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI stdio");
  }

  @Override
  public WasiInputStream getStdin() {
    final long streamHandle = nativeGetStdin(contextHandle);
    if (streamHandle == 0) {
      throw new RuntimeException("Failed to get stdin stream");
    }
    return new JniWasiInputStream(contextHandle, streamHandle);
  }

  @Override
  public WasiOutputStream getStdout() {
    final long streamHandle = nativeGetStdout(contextHandle);
    if (streamHandle == 0) {
      throw new RuntimeException("Failed to get stdout stream");
    }
    return new JniWasiOutputStream(contextHandle, streamHandle);
  }

  @Override
  public WasiOutputStream getStderr() {
    final long streamHandle = nativeGetStderr(contextHandle);
    if (streamHandle == 0) {
      throw new RuntimeException("Failed to get stderr stream");
    }
    return new JniWasiOutputStream(contextHandle, streamHandle);
  }

  // Native method declarations

  /**
   * Gets the stdin stream handle.
   *
   * @param contextHandle the native context handle
   * @return the stdin stream handle, or 0 if failed
   */
  private static native long nativeGetStdin(long contextHandle);

  /**
   * Gets the stdout stream handle.
   *
   * @param contextHandle the native context handle
   * @return the stdout stream handle, or 0 if failed
   */
  private static native long nativeGetStdout(long contextHandle);

  /**
   * Gets the stderr stream handle.
   *
   * @param contextHandle the native context handle
   * @return the stderr stream handle, or 0 if failed
   */
  private static native long nativeGetStderr(long contextHandle);
}
