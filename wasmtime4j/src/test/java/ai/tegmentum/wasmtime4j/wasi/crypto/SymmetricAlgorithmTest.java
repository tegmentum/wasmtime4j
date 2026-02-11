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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link SymmetricAlgorithm} enum. */
@DisplayName("SymmetricAlgorithm Tests")
class SymmetricAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(SymmetricAlgorithm.class.isEnum(), "SymmetricAlgorithm should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 7 enum values")
    void shouldHaveExactlySevenEnumValues() {
      final SymmetricAlgorithm[] values = SymmetricAlgorithm.values();

      assertEquals(7, values.length, "SymmetricAlgorithm should have exactly 7 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have AES_128 value")
    void shouldHaveAes128Value() {
      assertNotNull(SymmetricAlgorithm.AES_128, "AES_128 should not be null");
    }

    @Test
    @DisplayName("should have AES_192 value")
    void shouldHaveAes192Value() {
      assertNotNull(SymmetricAlgorithm.AES_192, "AES_192 should not be null");
    }

    @Test
    @DisplayName("should have AES_256 value")
    void shouldHaveAes256Value() {
      assertNotNull(SymmetricAlgorithm.AES_256, "AES_256 should not be null");
    }

    @Test
    @DisplayName("should have CHACHA20 value")
    void shouldHaveChaCha20Value() {
      assertNotNull(SymmetricAlgorithm.CHACHA20, "CHACHA20 should not be null");
    }

    @Test
    @DisplayName("should have XCHACHA20 value")
    void shouldHaveXChaCha20Value() {
      assertNotNull(SymmetricAlgorithm.XCHACHA20, "XCHACHA20 should not be null");
    }

    @Test
    @DisplayName("should have CHACHA20_POLY1305 value")
    void shouldHaveChaCha20Poly1305Value() {
      assertNotNull(SymmetricAlgorithm.CHACHA20_POLY1305, "CHACHA20_POLY1305 should not be null");
    }

    @Test
    @DisplayName("should have XCHACHA20_POLY1305 value")
    void shouldHaveXChaCha20Poly1305Value() {
      assertNotNull(SymmetricAlgorithm.XCHACHA20_POLY1305, "XCHACHA20_POLY1305 should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final SymmetricAlgorithm algorithm : SymmetricAlgorithm.values()) {
        ordinals.add(algorithm.ordinal());
      }

      assertEquals(
          SymmetricAlgorithm.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final SymmetricAlgorithm[] values = SymmetricAlgorithm.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should round-trip all values through valueOf")
    void shouldRoundTripAllValuesThroughValueOf() {
      for (final SymmetricAlgorithm algorithm : SymmetricAlgorithm.values()) {
        assertEquals(
            algorithm,
            SymmetricAlgorithm.valueOf(algorithm.name()),
            "valueOf should round-trip for " + algorithm.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SymmetricAlgorithm.valueOf("INVALID_ALGO"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final SymmetricAlgorithm[] first = SymmetricAlgorithm.values();
      final SymmetricAlgorithm[] second = SymmetricAlgorithm.values();

      assertNotSame(first, second, "values() should return a new array on each call");
      assertArrayEquals(first, second, "values() arrays should contain the same elements");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return name from toString for all values")
    void shouldReturnNameFromToStringForAllValues() {
      for (final SymmetricAlgorithm algorithm : SymmetricAlgorithm.values()) {
        assertEquals(
            algorithm.name(),
            algorithm.toString(),
            "toString should return name for " + algorithm.name());
      }
    }
  }

  @Nested
  @DisplayName("GetAlgorithmName Tests")
  class GetAlgorithmNameTests {

    @Test
    @DisplayName("should return AES-128 for AES_128")
    void shouldReturnCorrectNameForAes128() {
      assertEquals(
          "AES-128",
          SymmetricAlgorithm.AES_128.getAlgorithmName(),
          "AES_128 should have algorithm name AES-128");
    }

    @Test
    @DisplayName("should return AES-192 for AES_192")
    void shouldReturnCorrectNameForAes192() {
      assertEquals(
          "AES-192",
          SymmetricAlgorithm.AES_192.getAlgorithmName(),
          "AES_192 should have algorithm name AES-192");
    }

    @Test
    @DisplayName("should return AES-256 for AES_256")
    void shouldReturnCorrectNameForAes256() {
      assertEquals(
          "AES-256",
          SymmetricAlgorithm.AES_256.getAlgorithmName(),
          "AES_256 should have algorithm name AES-256");
    }

    @Test
    @DisplayName("should return ChaCha20 for CHACHA20")
    void shouldReturnCorrectNameForChaCha20() {
      assertEquals(
          "ChaCha20",
          SymmetricAlgorithm.CHACHA20.getAlgorithmName(),
          "CHACHA20 should have algorithm name ChaCha20");
    }

    @Test
    @DisplayName("should return XChaCha20 for XCHACHA20")
    void shouldReturnCorrectNameForXChaCha20() {
      assertEquals(
          "XChaCha20",
          SymmetricAlgorithm.XCHACHA20.getAlgorithmName(),
          "XCHACHA20 should have algorithm name XChaCha20");
    }

    @Test
    @DisplayName("should return ChaCha20-Poly1305 for CHACHA20_POLY1305")
    void shouldReturnCorrectNameForChaCha20Poly1305() {
      assertEquals(
          "ChaCha20-Poly1305",
          SymmetricAlgorithm.CHACHA20_POLY1305.getAlgorithmName(),
          "CHACHA20_POLY1305 should have algorithm name ChaCha20-Poly1305");
    }

    @Test
    @DisplayName("should return XChaCha20-Poly1305 for XCHACHA20_POLY1305")
    void shouldReturnCorrectNameForXChaCha20Poly1305() {
      assertEquals(
          "XChaCha20-Poly1305",
          SymmetricAlgorithm.XCHACHA20_POLY1305.getAlgorithmName(),
          "XCHACHA20_POLY1305 should have algorithm name XChaCha20-Poly1305");
    }
  }

  @Nested
  @DisplayName("GetKeySize Tests")
  class GetKeySizeTests {

    @Test
    @DisplayName("should return 128 for AES_128")
    void shouldReturnCorrectKeySizeForAes128() {
      assertEquals(
          128, SymmetricAlgorithm.AES_128.getKeySize(), "AES_128 should have key size 128");
    }

    @Test
    @DisplayName("should return 192 for AES_192")
    void shouldReturnCorrectKeySizeForAes192() {
      assertEquals(
          192, SymmetricAlgorithm.AES_192.getKeySize(), "AES_192 should have key size 192");
    }

    @Test
    @DisplayName("should return 256 for AES_256")
    void shouldReturnCorrectKeySizeForAes256() {
      assertEquals(
          256, SymmetricAlgorithm.AES_256.getKeySize(), "AES_256 should have key size 256");
    }

    @Test
    @DisplayName("should return 256 for CHACHA20")
    void shouldReturnCorrectKeySizeForChaCha20() {
      assertEquals(
          256, SymmetricAlgorithm.CHACHA20.getKeySize(), "CHACHA20 should have key size 256");
    }

    @Test
    @DisplayName("should return 256 for XCHACHA20")
    void shouldReturnCorrectKeySizeForXChaCha20() {
      assertEquals(
          256, SymmetricAlgorithm.XCHACHA20.getKeySize(), "XCHACHA20 should have key size 256");
    }

    @Test
    @DisplayName("should return 256 for CHACHA20_POLY1305")
    void shouldReturnCorrectKeySizeForChaCha20Poly1305() {
      assertEquals(
          256,
          SymmetricAlgorithm.CHACHA20_POLY1305.getKeySize(),
          "CHACHA20_POLY1305 should have key size 256");
    }

    @Test
    @DisplayName("should return 256 for XCHACHA20_POLY1305")
    void shouldReturnCorrectKeySizeForXChaCha20Poly1305() {
      assertEquals(
          256,
          SymmetricAlgorithm.XCHACHA20_POLY1305.getKeySize(),
          "XCHACHA20_POLY1305 should have key size 256");
    }
  }
}
