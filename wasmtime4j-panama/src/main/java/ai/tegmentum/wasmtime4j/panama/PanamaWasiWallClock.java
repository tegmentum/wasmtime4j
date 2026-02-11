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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.wasi.WasiWallClock;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Panama FFI implementation of WasiWallClock.
 *
 * <p>Provides wall clock functionality using Panama Foreign Function API bindings to the native
 * Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiWallClock implements WasiWallClock {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  /** Creates a new Panama WASI Wall Clock. */
  public PanamaWasiWallClock() {
    // No initialization required - uses global WASI clock
  }

  @Override
  public Datetime now() {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outSeconds = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outNanos = arena.allocate(ValueLayout.JAVA_INT);
      final int result = NATIVE_BINDINGS.wasiWallClockNow(outSeconds, outNanos);
      if (result != 0) {
        throw new RuntimeException("Failed to get wall clock time, error code: " + result);
      }
      final long seconds = outSeconds.get(ValueLayout.JAVA_LONG, 0);
      final int nanos = outNanos.get(ValueLayout.JAVA_INT, 0);
      return new Datetime(seconds, nanos);
    }
  }

  @Override
  public Datetime resolution() {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outSeconds = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment outNanos = arena.allocate(ValueLayout.JAVA_INT);
      final int result = NATIVE_BINDINGS.wasiWallClockResolution(outSeconds, outNanos);
      if (result != 0) {
        throw new RuntimeException("Failed to get wall clock resolution, error code: " + result);
      }
      final long seconds = outSeconds.get(ValueLayout.JAVA_LONG, 0);
      final int nanos = outNanos.get(ValueLayout.JAVA_INT, 0);
      return new Datetime(seconds, nanos);
    }
  }
}
