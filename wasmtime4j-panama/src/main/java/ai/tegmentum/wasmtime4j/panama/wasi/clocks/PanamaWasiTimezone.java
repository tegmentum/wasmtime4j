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
package ai.tegmentum.wasmtime4j.panama.wasi.clocks;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import ai.tegmentum.wasmtime4j.wasi.clocks.TimezoneDisplay;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiTimezone;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiTimezone interface.
 *
 * <p>This class provides access to WASI Preview 2 timezone operations through Panama Foreign
 * Function API calls to the native Wasmtime library. It provides timezone information including UTC
 * offsets, timezone names, and daylight saving time status.
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Converting between UTC and local time
 *   <li>Displaying timezone information to users
 *   <li>Handling daylight saving time transitions
 *   <li>Calendar and scheduling applications
 * </ul>
 *
 * <p><b>Note:</b> This interface is marked as unstable in WASI Preview 2.
 *
 * @since 1.0.0
 */
public final class PanamaWasiTimezone implements WasiTimezone {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiTimezone.class.getName());

  // Panama FFI function handles
  private static final MethodHandle DISPLAY_HANDLE;
  private static final MethodHandle UTC_OFFSET_HANDLE;
  private static final MethodHandle FREE_NAME_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_timezone_display(context_handle, seconds, nanoseconds,
      // out_utc_offset, out_name, out_name_len, out_in_dst)
      DISPLAY_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_timezone_display").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_timezone_utc_offset(context_handle, seconds, nanoseconds,
      // out_offset)
      UTC_OFFSET_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_timezone_utc_offset").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS));

      // void wasmtime4j_panama_wasi_timezone_free_name(name_ptr)
      FREE_NAME_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_timezone_free_name").orElseThrow(),
              FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe("Failed to initialize Panama FFI handles for WasiTimezone: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI timezone with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiTimezone(final MemorySegment contextHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI timezone with context handle: " + contextHandle);
  }

  @Override
  public TimezoneDisplay display(final DateTime when) {
    if (when == null) {
      throw new IllegalArgumentException("DateTime cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outUtcOffset = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment outName = arena.allocate(ValueLayout.ADDRESS);
      final MemorySegment outNameLen = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outInDst = arena.allocate(ValueLayout.JAVA_BYTE);

      final int result =
          (int)
              DISPLAY_HANDLE.invoke(
                  contextHandle,
                  when.getSeconds(),
                  when.getNanoseconds(),
                  outUtcOffset,
                  outName,
                  outNameLen,
                  outInDst);

      if (result != 0) {
        throw new RuntimeException("Failed to get timezone display");
      }

      final int utcOffset = outUtcOffset.get(ValueLayout.JAVA_INT, 0);
      final MemorySegment namePtr = outName.get(ValueLayout.ADDRESS, 0);
      final long nameLen = outNameLen.get(ValueLayout.JAVA_LONG, 0);
      final boolean inDst = outInDst.get(ValueLayout.JAVA_BYTE, 0) != 0;

      // Read timezone name from native buffer
      final String name = namePtr.getString(0);

      // Free the native buffer
      try {
        FREE_NAME_HANDLE.invoke(namePtr);
      } catch (final Throwable e) {
        LOGGER.warning("Failed to free timezone name buffer: " + e.getMessage());
      }

      return new TimezoneDisplay(utcOffset, name, inDst);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting timezone display: " + e.getMessage(), e);
    }
  }

  @Override
  public int utcOffset(final DateTime when) {
    if (when == null) {
      throw new IllegalArgumentException("DateTime cannot be null");
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outOffset = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              UTC_OFFSET_HANDLE.invoke(
                  contextHandle, when.getSeconds(), when.getNanoseconds(), outOffset);

      if (result != 0) {
        throw new RuntimeException("Failed to get UTC offset");
      }

      return outOffset.get(ValueLayout.JAVA_INT, 0);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting UTC offset: " + e.getMessage(), e);
    }
  }
}
