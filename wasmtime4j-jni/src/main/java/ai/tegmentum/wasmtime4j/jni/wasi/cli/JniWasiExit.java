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
package ai.tegmentum.wasmtime4j.jni.wasi.cli;

import ai.tegmentum.wasmtime4j.wasi.cli.WasiExit;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasiExit interface.
 *
 * <p>This class provides access to WASI Preview 2 program termination operations through JNI calls
 * to the native Wasmtime library.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class JniWasiExit implements WasiExit {

  private static final Logger LOGGER = Logger.getLogger(JniWasiExit.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniWasiExit: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final long contextHandle;

  /**
   * Creates a new JNI WASI exit with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is 0
   */
  public JniWasiExit(final long contextHandle) {
    if (contextHandle == 0) {
      throw new IllegalArgumentException("Context handle cannot be 0");
    }
    this.contextHandle = contextHandle;
    LOGGER.fine("Created JNI WASI exit");
  }

  @Override
  public void exit(final int statusCode) {
    final int result = nativeExit(contextHandle, statusCode);
    if (result != 0) {
      LOGGER.warning("Failed to exit with status code: " + statusCode);
    }

    // Note: If the native implementation properly terminates the WASM instance,
    // this method may not return. The return value check above is for cases
    // where the exit operation itself fails.
  }

  // Native method declarations

  /**
   * Terminates the program with the specified exit status code.
   *
   * @param contextHandle the native context handle
   * @param statusCode the exit status code
   * @return 0 on success, non-zero on failure
   */
  private static native int nativeExit(long contextHandle, int statusCode);
}
