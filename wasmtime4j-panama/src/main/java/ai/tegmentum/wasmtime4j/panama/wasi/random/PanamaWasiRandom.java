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

package ai.tegmentum.wasmtime4j.panama.wasi.random;

import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.random.WasiRandom;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiRandom interface.
 *
 * <p>This class provides access to WASI Preview 2 random data generation through Panama Foreign
 * Function API calls to the native Wasmtime library. It generates cryptographically-secure random
 * data suitable for security-sensitive applications.
 *
 * <p>All random data returned is:
 *
 * <ul>
 *   <li>Cryptographically-secure (equivalent to properly seeded CSPRNG)
 *   <li>Unpredictable and fresh
 *   <li>Non-blocking under all circumstances
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaWasiRandom implements WasiRandom {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiRandom.class.getName());

  // Panama FFI function handles
  private static final MethodHandle GET_BYTES_HANDLE;
  private static final MethodHandle GET_U64_HANDLE;
  private static final MethodHandle FREE_BUFFER_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeResourceHandle.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_random_get_bytes(context_handle, len, out_bytes,
      // out_bytes_len)
      GET_BYTES_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_random_get_bytes").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_random_get_u64(context_handle, out_value)
      GET_U64_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_random_get_u64").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // void wasmtime4j_panama_wasi_random_free_buffer(buffer)
      FREE_BUFFER_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_random_free_buffer").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiRandom: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI random generator with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiRandom(final MemorySegment contextHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI random with context handle: " + contextHandle);
  }

  @Override
  public byte[] getRandomBytes(final long len) {
    if (len < 0) {
      throw new IllegalArgumentException("Length cannot be negative: " + len);
    }
    if (len == 0) {
      return new byte[0];
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outBytes = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outBytesLen = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) GET_BYTES_HANDLE.invoke(contextHandle, len, outBytes, outBytesLen);

      if (result != 0) {
        throw new RuntimeException("Failed to get random bytes");
      }

      final long actualLen = outBytesLen.get(ValueLayout.JAVA_LONG, 0);
      final MemorySegment bytesBuffer = outBytes.get(ValueLayout.ADDRESS, 0);

      // Copy bytes to Java array
      final byte[] bytes = new byte[(int) actualLen];
      MemorySegment.copy(bytesBuffer, ValueLayout.JAVA_BYTE, 0, bytes, 0, (int) actualLen);

      // Free the native buffer
      try {
        FREE_BUFFER_HANDLE.invoke(bytesBuffer);
      } catch (final Throwable e) {
        LOGGER.warning("Failed to free random bytes buffer: " + e.getMessage());
      }

      return bytes;

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting random bytes: " + e.getMessage(), e);
    }
  }

  @Override
  public long getRandomU64() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outValue = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) GET_U64_HANDLE.invoke(contextHandle, outValue);

      if (result != 0) {
        throw new RuntimeException("Failed to get random u64");
      }

      return outValue.get(ValueLayout.JAVA_LONG, 0);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting random u64: " + e.getMessage(), e);
    }
  }
}
