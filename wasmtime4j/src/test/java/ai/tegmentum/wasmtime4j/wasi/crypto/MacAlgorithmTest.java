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

/** Tests for {@link MacAlgorithm} enum. */
@DisplayName("MacAlgorithm Tests")
class MacAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(MacAlgorithm.class.isEnum(), "MacAlgorithm should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 10 enum values")
    void shouldHaveExactlyTenEnumValues() {
      final MacAlgorithm[] values = MacAlgorithm.values();

      assertEquals(10, values.length, "MacAlgorithm should have exactly 10 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have HMAC_SHA256 value")
    void shouldHaveHmacSha256Value() {
      assertNotNull(MacAlgorithm.HMAC_SHA256, "HMAC_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have HMAC_SHA384 value")
    void shouldHaveHmacSha384Value() {
      assertNotNull(MacAlgorithm.HMAC_SHA384, "HMAC_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have HMAC_SHA512 value")
    void shouldHaveHmacSha512Value() {
      assertNotNull(MacAlgorithm.HMAC_SHA512, "HMAC_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have HMAC_SHA3_256 value")
    void shouldHaveHmacSha3256Value() {
      assertNotNull(MacAlgorithm.HMAC_SHA3_256, "HMAC_SHA3_256 should not be null");
    }

    @Test
    @DisplayName("should have POLY1305 value")
    void shouldHavePoly1305Value() {
      assertNotNull(MacAlgorithm.POLY1305, "POLY1305 should not be null");
    }

    @Test
    @DisplayName("should have BLAKE2B_MAC value")
    void shouldHaveBlake2bMacValue() {
      assertNotNull(MacAlgorithm.BLAKE2B_MAC, "BLAKE2B_MAC should not be null");
    }

    @Test
    @DisplayName("should have BLAKE2S_MAC value")
    void shouldHaveBlake2sMacValue() {
      assertNotNull(MacAlgorithm.BLAKE2S_MAC, "BLAKE2S_MAC should not be null");
    }

    @Test
    @DisplayName("should have AES_CMAC value")
    void shouldHaveAesCmacValue() {
      assertNotNull(MacAlgorithm.AES_CMAC, "AES_CMAC should not be null");
    }

    @Test
    @DisplayName("should have KMAC128 value")
    void shouldHaveKmac128Value() {
      assertNotNull(MacAlgorithm.KMAC128, "KMAC128 should not be null");
    }

    @Test
    @DisplayName("should have KMAC256 value")
    void shouldHaveKmac256Value() {
      assertNotNull(MacAlgorithm.KMAC256, "KMAC256 should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final MacAlgorithm algorithm : MacAlgorithm.values()) {
        ordinals.add(algorithm.ordinal());
      }

      assertEquals(
          MacAlgorithm.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final MacAlgorithm[] values = MacAlgorithm.values();

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
      for (final MacAlgorithm algorithm : MacAlgorithm.values()) {
        assertEquals(
            algorithm,
            MacAlgorithm.valueOf(algorithm.name()),
            "valueOf should round-trip for " + algorithm.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> MacAlgorithm.valueOf("INVALID_MAC"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final MacAlgorithm[] first = MacAlgorithm.values();
      final MacAlgorithm[] second = MacAlgorithm.values();

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
      for (final MacAlgorithm algorithm : MacAlgorithm.values()) {
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
    @DisplayName("should return HMAC-SHA256 for HMAC_SHA256")
    void shouldReturnCorrectNameForHmacSha256() {
      assertEquals(
          "HMAC-SHA256",
          MacAlgorithm.HMAC_SHA256.getAlgorithmName(),
          "HMAC_SHA256 should have algorithm name HMAC-SHA256");
    }

    @Test
    @DisplayName("should return HMAC-SHA384 for HMAC_SHA384")
    void shouldReturnCorrectNameForHmacSha384() {
      assertEquals(
          "HMAC-SHA384",
          MacAlgorithm.HMAC_SHA384.getAlgorithmName(),
          "HMAC_SHA384 should have algorithm name HMAC-SHA384");
    }

    @Test
    @DisplayName("should return HMAC-SHA512 for HMAC_SHA512")
    void shouldReturnCorrectNameForHmacSha512() {
      assertEquals(
          "HMAC-SHA512",
          MacAlgorithm.HMAC_SHA512.getAlgorithmName(),
          "HMAC_SHA512 should have algorithm name HMAC-SHA512");
    }

    @Test
    @DisplayName("should return HMAC-SHA3-256 for HMAC_SHA3_256")
    void shouldReturnCorrectNameForHmacSha3256() {
      assertEquals(
          "HMAC-SHA3-256",
          MacAlgorithm.HMAC_SHA3_256.getAlgorithmName(),
          "HMAC_SHA3_256 should have algorithm name HMAC-SHA3-256");
    }

    @Test
    @DisplayName("should return Poly1305 for POLY1305")
    void shouldReturnCorrectNameForPoly1305() {
      assertEquals(
          "Poly1305",
          MacAlgorithm.POLY1305.getAlgorithmName(),
          "POLY1305 should have algorithm name Poly1305");
    }

    @Test
    @DisplayName("should return BLAKE2b-MAC for BLAKE2B_MAC")
    void shouldReturnCorrectNameForBlake2bMac() {
      assertEquals(
          "BLAKE2b-MAC",
          MacAlgorithm.BLAKE2B_MAC.getAlgorithmName(),
          "BLAKE2B_MAC should have algorithm name BLAKE2b-MAC");
    }

    @Test
    @DisplayName("should return BLAKE2s-MAC for BLAKE2S_MAC")
    void shouldReturnCorrectNameForBlake2sMac() {
      assertEquals(
          "BLAKE2s-MAC",
          MacAlgorithm.BLAKE2S_MAC.getAlgorithmName(),
          "BLAKE2S_MAC should have algorithm name BLAKE2s-MAC");
    }

    @Test
    @DisplayName("should return AES-CMAC for AES_CMAC")
    void shouldReturnCorrectNameForAesCmac() {
      assertEquals(
          "AES-CMAC",
          MacAlgorithm.AES_CMAC.getAlgorithmName(),
          "AES_CMAC should have algorithm name AES-CMAC");
    }

    @Test
    @DisplayName("should return KMAC128 for KMAC128")
    void shouldReturnCorrectNameForKmac128() {
      assertEquals(
          "KMAC128",
          MacAlgorithm.KMAC128.getAlgorithmName(),
          "KMAC128 should have algorithm name KMAC128");
    }

    @Test
    @DisplayName("should return KMAC256 for KMAC256")
    void shouldReturnCorrectNameForKmac256() {
      assertEquals(
          "KMAC256",
          MacAlgorithm.KMAC256.getAlgorithmName(),
          "KMAC256 should have algorithm name KMAC256");
    }
  }

  @Nested
  @DisplayName("GetOutputSize Tests")
  class GetOutputSizeTests {

    @Test
    @DisplayName("should return 256 for HMAC_SHA256")
    void shouldReturnCorrectOutputSizeForHmacSha256() {
      assertEquals(
          256, MacAlgorithm.HMAC_SHA256.getOutputSize(), "HMAC_SHA256 should have output size 256");
    }

    @Test
    @DisplayName("should return 384 for HMAC_SHA384")
    void shouldReturnCorrectOutputSizeForHmacSha384() {
      assertEquals(
          384, MacAlgorithm.HMAC_SHA384.getOutputSize(), "HMAC_SHA384 should have output size 384");
    }

    @Test
    @DisplayName("should return 512 for HMAC_SHA512")
    void shouldReturnCorrectOutputSizeForHmacSha512() {
      assertEquals(
          512, MacAlgorithm.HMAC_SHA512.getOutputSize(), "HMAC_SHA512 should have output size 512");
    }

    @Test
    @DisplayName("should return 256 for HMAC_SHA3_256")
    void shouldReturnCorrectOutputSizeForHmacSha3256() {
      assertEquals(
          256,
          MacAlgorithm.HMAC_SHA3_256.getOutputSize(),
          "HMAC_SHA3_256 should have output size 256");
    }

    @Test
    @DisplayName("should return 128 for POLY1305")
    void shouldReturnCorrectOutputSizeForPoly1305() {
      assertEquals(
          128, MacAlgorithm.POLY1305.getOutputSize(), "POLY1305 should have output size 128");
    }

    @Test
    @DisplayName("should return 512 for BLAKE2B_MAC")
    void shouldReturnCorrectOutputSizeForBlake2bMac() {
      assertEquals(
          512, MacAlgorithm.BLAKE2B_MAC.getOutputSize(), "BLAKE2B_MAC should have output size 512");
    }

    @Test
    @DisplayName("should return 256 for BLAKE2S_MAC")
    void shouldReturnCorrectOutputSizeForBlake2sMac() {
      assertEquals(
          256, MacAlgorithm.BLAKE2S_MAC.getOutputSize(), "BLAKE2S_MAC should have output size 256");
    }

    @Test
    @DisplayName("should return 128 for AES_CMAC")
    void shouldReturnCorrectOutputSizeForAesCmac() {
      assertEquals(
          128, MacAlgorithm.AES_CMAC.getOutputSize(), "AES_CMAC should have output size 128");
    }

    @Test
    @DisplayName("should return 256 for KMAC128")
    void shouldReturnCorrectOutputSizeForKmac128() {
      assertEquals(
          256, MacAlgorithm.KMAC128.getOutputSize(), "KMAC128 should have output size 256");
    }

    @Test
    @DisplayName("should return 512 for KMAC256")
    void shouldReturnCorrectOutputSizeForKmac256() {
      assertEquals(
          512, MacAlgorithm.KMAC256.getOutputSize(), "KMAC256 should have output size 512");
    }
  }
}
