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

/** Tests for {@link KeyDerivationAlgorithm} enum. */
@DisplayName("KeyDerivationAlgorithm Tests")
class KeyDerivationAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          KeyDerivationAlgorithm.class.isEnum(), "KeyDerivationAlgorithm should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 10 enum values")
    void shouldHaveExactlyTenEnumValues() {
      final KeyDerivationAlgorithm[] values = KeyDerivationAlgorithm.values();

      assertEquals(10, values.length, "KeyDerivationAlgorithm should have exactly 10 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have HKDF_SHA256 value")
    void shouldHaveHkdfSha256Value() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA256, "HKDF_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have HKDF_SHA384 value")
    void shouldHaveHkdfSha384Value() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA384, "HKDF_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have HKDF_SHA512 value")
    void shouldHaveHkdfSha512Value() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA512, "HKDF_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have PBKDF2_HMAC_SHA256 value")
    void shouldHavePbkdf2HmacSha256Value() {
      assertNotNull(
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA256, "PBKDF2_HMAC_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have PBKDF2_HMAC_SHA384 value")
    void shouldHavePbkdf2HmacSha384Value() {
      assertNotNull(
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA384, "PBKDF2_HMAC_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have PBKDF2_HMAC_SHA512 value")
    void shouldHavePbkdf2HmacSha512Value() {
      assertNotNull(
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA512, "PBKDF2_HMAC_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have ARGON2ID value")
    void shouldHaveArgon2idValue() {
      assertNotNull(KeyDerivationAlgorithm.ARGON2ID, "ARGON2ID should not be null");
    }

    @Test
    @DisplayName("should have ARGON2I value")
    void shouldHaveArgon2iValue() {
      assertNotNull(KeyDerivationAlgorithm.ARGON2I, "ARGON2I should not be null");
    }

    @Test
    @DisplayName("should have SCRYPT value")
    void shouldHaveScryptValue() {
      assertNotNull(KeyDerivationAlgorithm.SCRYPT, "SCRYPT should not be null");
    }

    @Test
    @DisplayName("should have BCRYPT value")
    void shouldHaveBcryptValue() {
      assertNotNull(KeyDerivationAlgorithm.BCRYPT, "BCRYPT should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final KeyDerivationAlgorithm algorithm : KeyDerivationAlgorithm.values()) {
        ordinals.add(algorithm.ordinal());
      }

      assertEquals(
          KeyDerivationAlgorithm.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final KeyDerivationAlgorithm[] values = KeyDerivationAlgorithm.values();

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
      for (final KeyDerivationAlgorithm algorithm : KeyDerivationAlgorithm.values()) {
        assertEquals(
            algorithm,
            KeyDerivationAlgorithm.valueOf(algorithm.name()),
            "valueOf should round-trip for " + algorithm.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> KeyDerivationAlgorithm.valueOf("INVALID_KDF"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final KeyDerivationAlgorithm[] first = KeyDerivationAlgorithm.values();
      final KeyDerivationAlgorithm[] second = KeyDerivationAlgorithm.values();

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
      for (final KeyDerivationAlgorithm algorithm : KeyDerivationAlgorithm.values()) {
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
    @DisplayName("should return HKDF-SHA256 for HKDF_SHA256")
    void shouldReturnCorrectNameForHkdfSha256() {
      assertEquals(
          "HKDF-SHA256",
          KeyDerivationAlgorithm.HKDF_SHA256.getAlgorithmName(),
          "HKDF_SHA256 should have algorithm name HKDF-SHA256");
    }

    @Test
    @DisplayName("should return HKDF-SHA384 for HKDF_SHA384")
    void shouldReturnCorrectNameForHkdfSha384() {
      assertEquals(
          "HKDF-SHA384",
          KeyDerivationAlgorithm.HKDF_SHA384.getAlgorithmName(),
          "HKDF_SHA384 should have algorithm name HKDF-SHA384");
    }

    @Test
    @DisplayName("should return HKDF-SHA512 for HKDF_SHA512")
    void shouldReturnCorrectNameForHkdfSha512() {
      assertEquals(
          "HKDF-SHA512",
          KeyDerivationAlgorithm.HKDF_SHA512.getAlgorithmName(),
          "HKDF_SHA512 should have algorithm name HKDF-SHA512");
    }

    @Test
    @DisplayName("should return PBKDF2-HMAC-SHA256 for PBKDF2_HMAC_SHA256")
    void shouldReturnCorrectNameForPbkdf2HmacSha256() {
      assertEquals(
          "PBKDF2-HMAC-SHA256",
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA256.getAlgorithmName(),
          "PBKDF2_HMAC_SHA256 should have algorithm name PBKDF2-HMAC-SHA256");
    }

    @Test
    @DisplayName("should return PBKDF2-HMAC-SHA384 for PBKDF2_HMAC_SHA384")
    void shouldReturnCorrectNameForPbkdf2HmacSha384() {
      assertEquals(
          "PBKDF2-HMAC-SHA384",
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA384.getAlgorithmName(),
          "PBKDF2_HMAC_SHA384 should have algorithm name PBKDF2-HMAC-SHA384");
    }

    @Test
    @DisplayName("should return PBKDF2-HMAC-SHA512 for PBKDF2_HMAC_SHA512")
    void shouldReturnCorrectNameForPbkdf2HmacSha512() {
      assertEquals(
          "PBKDF2-HMAC-SHA512",
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA512.getAlgorithmName(),
          "PBKDF2_HMAC_SHA512 should have algorithm name PBKDF2-HMAC-SHA512");
    }

    @Test
    @DisplayName("should return Argon2id for ARGON2ID")
    void shouldReturnCorrectNameForArgon2id() {
      assertEquals(
          "Argon2id",
          KeyDerivationAlgorithm.ARGON2ID.getAlgorithmName(),
          "ARGON2ID should have algorithm name Argon2id");
    }

    @Test
    @DisplayName("should return Argon2i for ARGON2I")
    void shouldReturnCorrectNameForArgon2i() {
      assertEquals(
          "Argon2i",
          KeyDerivationAlgorithm.ARGON2I.getAlgorithmName(),
          "ARGON2I should have algorithm name Argon2i");
    }

    @Test
    @DisplayName("should return scrypt for SCRYPT")
    void shouldReturnCorrectNameForScrypt() {
      assertEquals(
          "scrypt",
          KeyDerivationAlgorithm.SCRYPT.getAlgorithmName(),
          "SCRYPT should have algorithm name scrypt");
    }

    @Test
    @DisplayName("should return bcrypt for BCRYPT")
    void shouldReturnCorrectNameForBcrypt() {
      assertEquals(
          "bcrypt",
          KeyDerivationAlgorithm.BCRYPT.getAlgorithmName(),
          "BCRYPT should have algorithm name bcrypt");
    }
  }
}
