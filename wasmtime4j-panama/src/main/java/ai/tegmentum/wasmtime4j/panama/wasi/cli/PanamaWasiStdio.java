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

import ai.tegmentum.wasmtime4j.panama.util.PanamaResource;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiInputStream;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiOutputStream;
import ai.tegmentum.wasmtime4j.wasi.cli.WasiStdio;
import ai.tegmentum.wasmtime4j.wasi.io.WasiInputStream;
import ai.tegmentum.wasmtime4j.wasi.io.WasiOutputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiStdio interface.
 *
 * <p>This class provides access to WASI Preview 2 standard I/O streams through Panama Foreign
 * Function API calls to the native Wasmtime library.
 *
 * <p>This implementation ensures defensive programming to prevent native resource leaks and JVM
 * crashes.
 *
 * @since 1.0.0
 */
public final class PanamaWasiStdio implements WasiStdio {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiStdio.class.getName());

  // Panama FFI function handles
  private static final MethodHandle GET_STDIN_HANDLE;
  private static final MethodHandle GET_STDOUT_HANDLE;
  private static final MethodHandle GET_STDERR_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = PanamaResource.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_stdio_get_stdin(context, out_stream_handle)
      GET_STDIN_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_stdio_get_stdin").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_stdio_get_stdout(context, out_stream_handle)
      GET_STDOUT_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_stdio_get_stdout").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_stdio_get_stderr(context, out_stream_handle)
      GET_STDERR_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_stdio_get_stderr").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiStdio: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI stdio with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiStdio(final MemorySegment contextHandle) {
    PanamaValidation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI stdio");
  }

  @Override
  public WasiInputStream getStdin() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outStreamHandle = arena.allocate(ValueLayout.ADDRESS);

      final int result = (int) GET_STDIN_HANDLE.invoke(contextHandle, outStreamHandle);

      if (result != 0) {
        throw new RuntimeException("Failed to get stdin stream");
      }

      final MemorySegment streamHandle = outStreamHandle.get(ValueLayout.ADDRESS, 0);
      if (streamHandle == null || streamHandle.address() == 0) {
        throw new RuntimeException("Failed to get stdin stream (null handle returned)");
      }

      return new PanamaWasiInputStream(contextHandle, streamHandle);

    } catch (final Throwable e) {
      throw new RuntimeException("Error getting stdin stream: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiOutputStream getStdout() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outStreamHandle = arena.allocate(ValueLayout.ADDRESS);

      final int result = (int) GET_STDOUT_HANDLE.invoke(contextHandle, outStreamHandle);

      if (result != 0) {
        throw new RuntimeException("Failed to get stdout stream");
      }

      final MemorySegment streamHandle = outStreamHandle.get(ValueLayout.ADDRESS, 0);
      if (streamHandle == null || streamHandle.address() == 0) {
        throw new RuntimeException("Failed to get stdout stream (null handle returned)");
      }

      return new PanamaWasiOutputStream(contextHandle, streamHandle);

    } catch (final Throwable e) {
      throw new RuntimeException("Error getting stdout stream: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiOutputStream getStderr() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outStreamHandle = arena.allocate(ValueLayout.ADDRESS);

      final int result = (int) GET_STDERR_HANDLE.invoke(contextHandle, outStreamHandle);

      if (result != 0) {
        throw new RuntimeException("Failed to get stderr stream");
      }

      final MemorySegment streamHandle = outStreamHandle.get(ValueLayout.ADDRESS, 0);
      if (streamHandle == null || streamHandle.address() == 0) {
        throw new RuntimeException("Failed to get stderr stream (null handle returned)");
      }

      return new PanamaWasiOutputStream(contextHandle, streamHandle);

    } catch (final Throwable e) {
      throw new RuntimeException("Error getting stderr stream: " + e.getMessage(), e);
    }
  }
}
