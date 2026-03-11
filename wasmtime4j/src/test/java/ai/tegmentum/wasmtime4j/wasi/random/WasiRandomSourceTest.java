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
package ai.tegmentum.wasmtime4j.wasi.random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for the {@link WasiRandomSource} functional interface. */
@DisplayName("WasiRandomSource Tests")
class WasiRandomSourceTest {

  @Nested
  @DisplayName("Functional Interface Usage")
  class FunctionalInterfaceUsage {

    @Test
    @DisplayName("should be implementable as lambda")
    void shouldBeImplementableAsLambda() {
      WasiRandomSource source =
          (dest) -> {
            for (int i = 0; i < dest.length; i++) {
              dest[i] = (byte) (i & 0xFF);
            }
          };

      byte[] dest = new byte[4];
      source.fillBytes(dest);

      assertArrayEquals(new byte[] {0, 1, 2, 3}, dest);
    }

    @Test
    @DisplayName("should work with java.util.Random")
    void shouldWorkWithJavaUtilRandom() {
      WasiRandomSource deterministic =
          (dest) -> {
            Random rng = new Random(42);
            rng.nextBytes(dest);
          };

      byte[] dest = new byte[16];
      deterministic.fillBytes(dest);

      assertNotNull(dest);
      assertEquals(16, dest.length);
    }

    @Test
    @DisplayName("should handle zero-length array")
    void shouldHandleZeroLengthArray() {
      WasiRandomSource source = (dest) -> {};

      byte[] dest = new byte[0];
      source.fillBytes(dest);

      assertEquals(0, dest.length);
    }

    @Test
    @DisplayName("should fill entire destination array")
    void shouldFillEntireDestinationArray() {
      WasiRandomSource fillAll =
          (dest) -> {
            for (int i = 0; i < dest.length; i++) {
              dest[i] = (byte) 0xFF;
            }
          };

      byte[] dest = new byte[8];
      fillAll.fillBytes(dest);

      for (byte b : dest) {
        assertEquals((byte) 0xFF, b);
      }
    }
  }

  @Nested
  @DisplayName("Deterministic Source")
  class DeterministicSource {

    @Test
    @DisplayName("deterministic source should produce repeatable results")
    void deterministicSourceShouldProduceRepeatableResults() {
      WasiRandomSource source =
          (dest) -> {
            Random rng = new Random(123);
            rng.nextBytes(dest);
          };

      byte[] first = new byte[16];
      byte[] second = new byte[16];
      source.fillBytes(first);
      source.fillBytes(second);

      assertArrayEquals(
          first, second, "Deterministic source with same seed should produce same output");
    }
  }
}
