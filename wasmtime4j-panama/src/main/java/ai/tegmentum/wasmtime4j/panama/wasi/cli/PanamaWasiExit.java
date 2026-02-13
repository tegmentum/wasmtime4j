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

package ai.tegmentum.wasmtime4j.panama.wasi.cli;

import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.wasi.cli.WasiExit;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiExit interface.
 *
 * <p>This class provides access to WASI Preview 2 program termination operations through Panama
 * Foreign Function API calls to the native Wasmtime library.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiExit implements WasiExit {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiExit.class.getName());

  // Panama FFI function handle
  private static final MethodHandle EXIT_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeResourceHandle.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_exit(context, status_code)
      EXIT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_exit").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiExit: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI exit with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiExit(final MemorySegment contextHandle) {
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI exit");
  }

  @Override
  public void exit(final int statusCode) {
    try {
      final int result = (int) EXIT_HANDLE.invoke(contextHandle, statusCode);

      if (result != 0) {
        LOGGER.warning("Failed to exit with status code: " + statusCode);
      }

      // Note: If the native implementation properly terminates the WASM instance,
      // this method may not return. The return value check above is for cases
      // where the exit operation itself fails.

    } catch (final Throwable e) {
      LOGGER.severe("Error exiting with status code " + statusCode + ": " + e.getMessage());
      throw new RuntimeException("Error exiting: " + e.getMessage(), e);
    }
  }
}
