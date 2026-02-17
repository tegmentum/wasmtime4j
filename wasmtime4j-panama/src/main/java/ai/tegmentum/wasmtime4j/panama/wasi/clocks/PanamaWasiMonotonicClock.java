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

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.util.Validation;
import ai.tegmentum.wasmtime4j.panama.wasi.io.PanamaWasiPollable;
import ai.tegmentum.wasmtime4j.wasi.clocks.WasiMonotonicClock;
import ai.tegmentum.wasmtime4j.wasi.io.WasiPollable;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WasiMonotonicClock interface.
 *
 * <p>This class provides access to WASI Preview 2 monotonic clock operations through Panama Foreign
 * Function API calls to the native Wasmtime library. The monotonic clock measures elapsed time with
 * nanosecond precision and is guaranteed to be non-decreasing.
 *
 * <p>Use cases:
 *
 * <ul>
 *   <li>Performance measurement and profiling
 *   <li>Timeout and deadline tracking
 *   <li>Rate limiting and throttling
 *   <li>Scheduling and timing operations
 * </ul>
 *
 * @since 1.0.0
 */
public final class PanamaWasiMonotonicClock implements WasiMonotonicClock {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiMonotonicClock.class.getName());

  // Panama FFI function handles
  private static final MethodHandle NOW_HANDLE;
  private static final MethodHandle RESOLUTION_HANDLE;
  private static final MethodHandle SUBSCRIBE_INSTANT_HANDLE;
  private static final MethodHandle SUBSCRIBE_DURATION_HANDLE;

  static {
    try {
      final SymbolLookup nativeLib = NativeLibraryLoader.getInstance().getSymbolLookup();
      final Linker linker = Linker.nativeLinker();

      // int wasmtime4j_panama_wasi_monotonic_clock_now(context_handle, out_instant)
      NOW_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_monotonic_clock_now").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_monotonic_clock_resolution(context_handle, out_resolution)
      RESOLUTION_HANDLE =
          linker.downcallHandle(
              nativeLib.find("wasmtime4j_panama_wasi_monotonic_clock_resolution").orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant(context_handle, when,
      // out_pollable_id)
      SUBSCRIBE_INSTANT_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_monotonic_clock_subscribe_instant")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

      // int wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration(context_handle, duration,
      // out_pollable_id)
      SUBSCRIBE_DURATION_HANDLE =
          linker.downcallHandle(
              nativeLib
                  .find("wasmtime4j_panama_wasi_monotonic_clock_subscribe_duration")
                  .orElseThrow(),
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS,
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS));

    } catch (final Throwable e) {
      LOGGER.severe(
          "Failed to initialize Panama FFI handles for WasiMonotonicClock: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The native context handle. */
  private final MemorySegment contextHandle;

  /**
   * Creates a new Panama WASI monotonic clock with the given native context handle.
   *
   * @param contextHandle the native context handle
   * @throws IllegalArgumentException if context handle is null
   */
  public PanamaWasiMonotonicClock(final MemorySegment contextHandle) {
    Validation.requireNonNull(contextHandle, "contextHandle");
    this.contextHandle = contextHandle;
    LOGGER.fine("Created Panama WASI monotonic clock with context handle: " + contextHandle);
  }

  @Override
  public long now() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outInstant = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) NOW_HANDLE.invoke(contextHandle, outInstant);

      if (result != 0) {
        throw new RuntimeException("Failed to get monotonic clock instant");
      }

      return outInstant.get(ValueLayout.JAVA_LONG, 0);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting monotonic clock instant: " + e.getMessage(), e);
    }
  }

  @Override
  public long resolution() {
    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outResolution = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) RESOLUTION_HANDLE.invoke(contextHandle, outResolution);

      if (result != 0) {
        throw new RuntimeException("Failed to get monotonic clock resolution");
      }

      return outResolution.get(ValueLayout.JAVA_LONG, 0);

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error getting monotonic clock resolution: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiPollable subscribeInstant(final long when) {
    if (when < 0) {
      throw new IllegalArgumentException("Instant cannot be negative: " + when);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollableId = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = (int) SUBSCRIBE_INSTANT_HANDLE.invoke(contextHandle, when, outPollableId);

      if (result != 0) {
        throw new RuntimeException("Failed to subscribe to instant");
      }

      final long pollableId = outPollableId.get(ValueLayout.JAVA_LONG, 0);
      if (pollableId <= 0) {
        throw new RuntimeException("Failed to create pollable for instant");
      }

      return new PanamaWasiPollable(contextHandle, MemorySegment.ofAddress(pollableId));

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error subscribing to instant: " + e.getMessage(), e);
    }
  }

  @Override
  public WasiPollable subscribeDuration(final long duration) {
    if (duration < 0) {
      throw new IllegalArgumentException("Duration cannot be negative: " + duration);
    }

    try (final Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollableId = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int) SUBSCRIBE_DURATION_HANDLE.invoke(contextHandle, duration, outPollableId);

      if (result != 0) {
        throw new RuntimeException("Failed to subscribe to duration");
      }

      final long pollableId = outPollableId.get(ValueLayout.JAVA_LONG, 0);
      if (pollableId <= 0) {
        throw new RuntimeException("Failed to create pollable for duration");
      }

      return new PanamaWasiPollable(contextHandle, MemorySegment.ofAddress(pollableId));

    } catch (final RuntimeException e) {
      throw e;
    } catch (final Throwable e) {
      throw new RuntimeException("Error subscribing to duration: " + e.getMessage(), e);
    }
  }
}
