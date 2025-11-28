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

import ai.tegmentum.wasmtime4j.WasiMonotonicClock;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Panama FFI implementation of WasiMonotonicClock.
 *
 * <p>Provides monotonic clock functionality using Panama Foreign Function API bindings to the
 * native Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiMonotonicClock implements WasiMonotonicClock {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  /** Creates a new Panama WASI Monotonic Clock. */
  public PanamaWasiMonotonicClock() {
    // No initialization required - uses global WASI clock
  }

  @Override
  public long now() {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outTime = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiMonotonicClockNow(outTime);
      if (result != 0) {
        throw new RuntimeException("Failed to get monotonic clock time, error code: " + result);
      }
      return outTime.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public long resolution() {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outResolution = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiMonotonicClockResolution(outResolution);
      if (result != 0) {
        throw new RuntimeException(
            "Failed to get monotonic clock resolution, error code: " + result);
      }
      return outResolution.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public long subscribeInstant(final long when) {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollable = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiMonotonicClockSubscribeInstant(when, outPollable);
      if (result != 0) {
        throw new RuntimeException(
            "Failed to subscribe to monotonic clock instant, error code: " + result);
      }
      return outPollable.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public long subscribeDuration(final long duration) {
    if (duration < 0) {
      throw new IllegalArgumentException("Duration cannot be negative");
    }
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outPollable = arena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.wasiMonotonicClockSubscribeDuration(duration, outPollable);
      if (result != 0) {
        throw new RuntimeException(
            "Failed to subscribe to monotonic clock duration, error code: " + result);
      }
      return outPollable.get(ValueLayout.JAVA_LONG, 0);
    }
  }
}
