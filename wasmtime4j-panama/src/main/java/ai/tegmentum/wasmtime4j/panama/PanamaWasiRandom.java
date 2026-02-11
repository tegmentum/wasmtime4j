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

import ai.tegmentum.wasmtime4j.wasi.WasiRandom;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Panama FFI implementation of WasiRandom.
 *
 * <p>Provides cryptographically secure random number generation using Panama Foreign Function API
 * bindings to the native Wasmtime WASI implementation.
 *
 * @since 1.0.0
 */
public final class PanamaWasiRandom implements WasiRandom {

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  /** Creates a new Panama WASI Random. */
  public PanamaWasiRandom() {
    // No initialization required - uses global WASI random
  }

  @Override
  public byte[] getRandomBytes(final int length) {
    if (length < 0) {
      throw new IllegalArgumentException("Length cannot be negative");
    }
    if (length == 0) {
      return new byte[0];
    }

    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment buffer = arena.allocate(length);
      final MemorySegment outActualLength = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.wasiRandomGetBytes(buffer, length, outActualLength);
      if (result != 0) {
        throw new RuntimeException("Failed to get random bytes, error code: " + result);
      }

      final long actualLength = outActualLength.get(ValueLayout.JAVA_LONG, 0);
      final byte[] data = new byte[(int) actualLength];
      for (int i = 0; i < actualLength; i++) {
        data[i] = buffer.get(ValueLayout.JAVA_BYTE, i);
      }

      return data;
    }
  }

  @Override
  public long getRandomU64() {
    try (Arena arena = Arena.ofConfined()) {
      final MemorySegment outValue = arena.allocate(ValueLayout.JAVA_LONG);

      final int result = NATIVE_BINDINGS.wasiRandomGetU64(outValue);
      if (result != 0) {
        throw new RuntimeException("Failed to get random u64, error code: " + result);
      }

      return outValue.get(ValueLayout.JAVA_LONG, 0);
    }
  }
}
