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

package ai.tegmentum.wasmtime4j.panama.wasi.clocks;

import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiWallClock;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiWallClock interface.
 *
 * <p>This class provides access to WASI Preview 2 wall clock operations through Panama Foreign
 * Function API calls to the native Wasmtime library. The wall clock reports real-world time as
 * seconds and nanoseconds since the Unix epoch (1970-01-01T00:00:00Z).
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Timestamping log entries and events
 *   <li>Displaying current date and time to users
 *   <li>Calendar and scheduling operations
 *   <li>Audit trails and transaction records
 * </ul>
 *
 * <p><b>Note:</b> This clock is not monotonic and may be affected by system time adjustments. For
 * measuring elapsed time, use {@link PanamaWasiMonotonicClock} instead.
 *
 * @since 1.0.0
 */
public final class PanamaWasiWallClock implements WasiWallClock {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiWallClock.class.getName());

  // Panama FFI function handles
  private static final MethodHandle NOW_HANDLE;
  private static final MethodHandle RESOLUTION_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeResourceHandle.getNativeLibrary();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_wall_clock_now(context_handle, out_seconds, out_nanoseconds)
      NOW_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_wall_clock_now").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_wall_clock_resolution(context_handle, out_seconds,
      // out_nanoseconds)
      RESOLUTION_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_wall_clock_resolution").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiWallClock: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI wall clock with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiWallClock(final MemorySegment contextHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI wall clock with context handle: " + contextHandle);
  }

  @Override
  public DateTime now() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSeconds = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outNanoseconds = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) NOW_HANDLE.invoke(contextHandle, outSeconds, outNanoseconds);

      if (result != 0) {
        throw new RuntimeException("Failed to get wall clock time");
      }

      final long seconds = outSeconds.get(ValueLayout.JAVA_LONG, 0);
      final int nanoseconds = outNanoseconds.get(ValueLayout.JAVA_INT, 0);

      return new DateTime(seconds, nanoseconds);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting wall clock time: " + e.getMessage(), e);
    }
  }

  @Override
  public DateTime resolution() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outSeconds = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outNanoseconds = arena.allocate(ValueLayout.JAVA_INT);

      final int result = (int) RESOLUTION_HANDLE.invoke(contextHandle, outSeconds, outNanoseconds);

      if (result != 0) {
        throw new RuntimeException("Failed to get wall clock resolution");
      }

      final long seconds = outSeconds.get(ValueLayout.JAVA_LONG, 0);
      final int nanoseconds = outNanoseconds.get(ValueLayout.JAVA_INT, 0);

      return new DateTime(seconds, nanoseconds);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting wall clock resolution: " + e.getMessage(), e);
    }
  }
}
