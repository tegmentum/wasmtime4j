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

/** Tests for {@link HashAlgorithm} enum. */
@DisplayName("HashAlgorithm Tests")
class HashAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(HashAlgorithm.class.isEnum(), "HashAlgorithm should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 10 enum values")
    void shouldHaveExactlyTenEnumValues() {
      final HashAlgorithm[] values = HashAlgorithm.values();

      assertEquals(10, values.length, "HashAlgorithm should have exactly 10 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have SHA_256 value")
    void shouldHaveSha256Value() {
      assertNotNull(HashAlgorithm.SHA_256, "SHA_256 should not be null");
    }

    @Test
    @DisplayName("should have SHA_384 value")
    void shouldHaveSha384Value() {
      assertNotNull(HashAlgorithm.SHA_384, "SHA_384 should not be null");
    }

    @Test
    @DisplayName("should have SHA_512 value")
    void shouldHaveSha512Value() {
      assertNotNull(HashAlgorithm.SHA_512, "SHA_512 should not be null");
    }

    @Test
    @DisplayName("should have SHA_512_256 value")
    void shouldHaveSha512256Value() {
      assertNotNull(HashAlgorithm.SHA_512_256, "SHA_512_256 should not be null");
    }

    @Test
    @DisplayName("should have SHA3_256 value")
    void shouldHaveSha3256Value() {
      assertNotNull(HashAlgorithm.SHA3_256, "SHA3_256 should not be null");
    }

    @Test
    @DisplayName("should have SHA3_384 value")
    void shouldHaveSha3384Value() {
      assertNotNull(HashAlgorithm.SHA3_384, "SHA3_384 should not be null");
    }

    @Test
    @DisplayName("should have SHA3_512 value")
    void shouldHaveSha3512Value() {
      assertNotNull(HashAlgorithm.SHA3_512, "SHA3_512 should not be null");
    }

    @Test
    @DisplayName("should have BLAKE2B value")
    void shouldHaveBlake2bValue() {
      assertNotNull(HashAlgorithm.BLAKE2B, "BLAKE2B should not be null");
    }

    @Test
    @DisplayName("should have BLAKE2S value")
    void shouldHaveBlake2sValue() {
      assertNotNull(HashAlgorithm.BLAKE2S, "BLAKE2S should not be null");
    }

    @Test
    @DisplayName("should have BLAKE3 value")
    void shouldHaveBlake3Value() {
      assertNotNull(HashAlgorithm.BLAKE3, "BLAKE3 should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final HashAlgorithm algorithm : HashAlgorithm.values()) {
        ordinals.add(algorithm.ordinal());
      }

      assertEquals(
          HashAlgorithm.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final HashAlgorithm[] values = HashAlgorithm.values();

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
      for (final HashAlgorithm algorithm : HashAlgorithm.values()) {
        assertEquals(
            algorithm,
            HashAlgorithm.valueOf(algorithm.name()),
            "valueOf should round-trip for " + algorithm.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> HashAlgorithm.valueOf("INVALID_HASH"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final HashAlgorithm[] first = HashAlgorithm.values();
      final HashAlgorithm[] second = HashAlgorithm.values();

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
      for (final HashAlgorithm algorithm : HashAlgorithm.values()) {
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
    @DisplayName("should return SHA-256 for SHA_256")
    void shouldReturnCorrectNameForSha256() {
      assertEquals(
          "SHA-256",
          HashAlgorithm.SHA_256.getAlgorithmName(),
          "SHA_256 should have algorithm name SHA-256");
    }

    @Test
    @DisplayName("should return SHA-384 for SHA_384")
    void shouldReturnCorrectNameForSha384() {
      assertEquals(
          "SHA-384",
          HashAlgorithm.SHA_384.getAlgorithmName(),
          "SHA_384 should have algorithm name SHA-384");
    }

    @Test
    @DisplayName("should return SHA-512 for SHA_512")
    void shouldReturnCorrectNameForSha512() {
      assertEquals(
          "SHA-512",
          HashAlgorithm.SHA_512.getAlgorithmName(),
          "SHA_512 should have algorithm name SHA-512");
    }

    @Test
    @DisplayName("should return SHA-512/256 for SHA_512_256")
    void shouldReturnCorrectNameForSha512256() {
      assertEquals(
          "SHA-512/256",
          HashAlgorithm.SHA_512_256.getAlgorithmName(),
          "SHA_512_256 should have algorithm name SHA-512/256");
    }

    @Test
    @DisplayName("should return SHA3-256 for SHA3_256")
    void shouldReturnCorrectNameForSha3256() {
      assertEquals(
          "SHA3-256",
          HashAlgorithm.SHA3_256.getAlgorithmName(),
          "SHA3_256 should have algorithm name SHA3-256");
    }

    @Test
    @DisplayName("should return SHA3-384 for SHA3_384")
    void shouldReturnCorrectNameForSha3384() {
      assertEquals(
          "SHA3-384",
          HashAlgorithm.SHA3_384.getAlgorithmName(),
          "SHA3_384 should have algorithm name SHA3-384");
    }

    @Test
    @DisplayName("should return SHA3-512 for SHA3_512")
    void shouldReturnCorrectNameForSha3512() {
      assertEquals(
          "SHA3-512",
          HashAlgorithm.SHA3_512.getAlgorithmName(),
          "SHA3_512 should have algorithm name SHA3-512");
    }

    @Test
    @DisplayName("should return BLAKE2b for BLAKE2B")
    void shouldReturnCorrectNameForBlake2b() {
      assertEquals(
          "BLAKE2b",
          HashAlgorithm.BLAKE2B.getAlgorithmName(),
          "BLAKE2B should have algorithm name BLAKE2b");
    }

    @Test
    @DisplayName("should return BLAKE2s for BLAKE2S")
    void shouldReturnCorrectNameForBlake2s() {
      assertEquals(
          "BLAKE2s",
          HashAlgorithm.BLAKE2S.getAlgorithmName(),
          "BLAKE2S should have algorithm name BLAKE2s");
    }

    @Test
    @DisplayName("should return BLAKE3 for BLAKE3")
    void shouldReturnCorrectNameForBlake3() {
      assertEquals(
          "BLAKE3",
          HashAlgorithm.BLAKE3.getAlgorithmName(),
          "BLAKE3 should have algorithm name BLAKE3");
    }
  }

  @Nested
  @DisplayName("GetOutputSize Tests")
  class GetOutputSizeTests {

    @Test
    @DisplayName("should return 256 for SHA_256")
    void shouldReturnCorrectOutputSizeForSha256() {
      assertEquals(
          256, HashAlgorithm.SHA_256.getOutputSize(), "SHA_256 should have output size 256");
    }

    @Test
    @DisplayName("should return 384 for SHA_384")
    void shouldReturnCorrectOutputSizeForSha384() {
      assertEquals(
          384, HashAlgorithm.SHA_384.getOutputSize(), "SHA_384 should have output size 384");
    }

    @Test
    @DisplayName("should return 512 for SHA_512")
    void shouldReturnCorrectOutputSizeForSha512() {
      assertEquals(
          512, HashAlgorithm.SHA_512.getOutputSize(), "SHA_512 should have output size 512");
    }

    @Test
    @DisplayName("should return 256 for SHA_512_256")
    void shouldReturnCorrectOutputSizeForSha512256() {
      assertEquals(
          256,
          HashAlgorithm.SHA_512_256.getOutputSize(),
          "SHA_512_256 should have output size 256");
    }

    @Test
    @DisplayName("should return 256 for SHA3_256")
    void shouldReturnCorrectOutputSizeForSha3256() {
      assertEquals(
          256, HashAlgorithm.SHA3_256.getOutputSize(), "SHA3_256 should have output size 256");
    }

    @Test
    @DisplayName("should return 384 for SHA3_384")
    void shouldReturnCorrectOutputSizeForSha3384() {
      assertEquals(
          384, HashAlgorithm.SHA3_384.getOutputSize(), "SHA3_384 should have output size 384");
    }

    @Test
    @DisplayName("should return 512 for SHA3_512")
    void shouldReturnCorrectOutputSizeForSha3512() {
      assertEquals(
          512, HashAlgorithm.SHA3_512.getOutputSize(), "SHA3_512 should have output size 512");
    }

    @Test
    @DisplayName("should return 512 for BLAKE2B")
    void shouldReturnCorrectOutputSizeForBlake2b() {
      assertEquals(
          512, HashAlgorithm.BLAKE2B.getOutputSize(), "BLAKE2B should have output size 512");
    }

    @Test
    @DisplayName("should return 256 for BLAKE2S")
    void shouldReturnCorrectOutputSizeForBlake2s() {
      assertEquals(
          256, HashAlgorithm.BLAKE2S.getOutputSize(), "BLAKE2S should have output size 256");
    }

    @Test
    @DisplayName("should return 256 for BLAKE3")
    void shouldReturnCorrectOutputSizeForBlake3() {
      assertEquals(256, HashAlgorithm.BLAKE3.getOutputSize(), "BLAKE3 should have output size 256");
    }
  }
}
