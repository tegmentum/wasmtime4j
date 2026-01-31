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

/** Tests for {@link AsymmetricAlgorithm} enum. */
@DisplayName("AsymmetricAlgorithm Tests")
class AsymmetricAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          AsymmetricAlgorithm.class.isEnum(),
          "AsymmetricAlgorithm should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 9 enum values")
    void shouldHaveExactlyNineEnumValues() {
      final AsymmetricAlgorithm[] values = AsymmetricAlgorithm.values();

      assertEquals(9, values.length, "AsymmetricAlgorithm should have exactly 9 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have RSA_2048 value")
    void shouldHaveRsa2048Value() {
      assertNotNull(AsymmetricAlgorithm.RSA_2048, "RSA_2048 should not be null");
    }

    @Test
    @DisplayName("should have RSA_3072 value")
    void shouldHaveRsa3072Value() {
      assertNotNull(AsymmetricAlgorithm.RSA_3072, "RSA_3072 should not be null");
    }

    @Test
    @DisplayName("should have RSA_4096 value")
    void shouldHaveRsa4096Value() {
      assertNotNull(AsymmetricAlgorithm.RSA_4096, "RSA_4096 should not be null");
    }

    @Test
    @DisplayName("should have X25519 value")
    void shouldHaveX25519Value() {
      assertNotNull(AsymmetricAlgorithm.X25519, "X25519 should not be null");
    }

    @Test
    @DisplayName("should have X448 value")
    void shouldHaveX448Value() {
      assertNotNull(AsymmetricAlgorithm.X448, "X448 should not be null");
    }

    @Test
    @DisplayName("should have ECDH_P256 value")
    void shouldHaveEcdhP256Value() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P256, "ECDH_P256 should not be null");
    }

    @Test
    @DisplayName("should have ECDH_P384 value")
    void shouldHaveEcdhP384Value() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P384, "ECDH_P384 should not be null");
    }

    @Test
    @DisplayName("should have ECDH_P521 value")
    void shouldHaveEcdhP521Value() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P521, "ECDH_P521 should not be null");
    }

    @Test
    @DisplayName("should have ECDH_SECP256K1 value")
    void shouldHaveEcdhSecp256k1Value() {
      assertNotNull(AsymmetricAlgorithm.ECDH_SECP256K1, "ECDH_SECP256K1 should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final AsymmetricAlgorithm algorithm : AsymmetricAlgorithm.values()) {
        ordinals.add(algorithm.ordinal());
      }

      assertEquals(
          AsymmetricAlgorithm.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final AsymmetricAlgorithm[] values = AsymmetricAlgorithm.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i,
            values[i].ordinal(),
            "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should round-trip all values through valueOf")
    void shouldRoundTripAllValuesThroughValueOf() {
      for (final AsymmetricAlgorithm algorithm : AsymmetricAlgorithm.values()) {
        assertEquals(
            algorithm,
            AsymmetricAlgorithm.valueOf(algorithm.name()),
            "valueOf should round-trip for " + algorithm.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> AsymmetricAlgorithm.valueOf("INVALID_ALGORITHM"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final AsymmetricAlgorithm[] first = AsymmetricAlgorithm.values();
      final AsymmetricAlgorithm[] second = AsymmetricAlgorithm.values();

      assertNotSame(first, second, "values() should return a new array on each call");
      assertArrayEquals(
          first,
          second,
          "values() arrays should contain the same elements");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return name from toString for all values")
    void shouldReturnNameFromToStringForAllValues() {
      for (final AsymmetricAlgorithm algorithm : AsymmetricAlgorithm.values()) {
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
    @DisplayName("should return RSA-2048 for RSA_2048")
    void shouldReturnCorrectNameForRsa2048() {
      assertEquals(
          "RSA-2048",
          AsymmetricAlgorithm.RSA_2048.getAlgorithmName(),
          "RSA_2048 should have algorithm name RSA-2048");
    }

    @Test
    @DisplayName("should return RSA-3072 for RSA_3072")
    void shouldReturnCorrectNameForRsa3072() {
      assertEquals(
          "RSA-3072",
          AsymmetricAlgorithm.RSA_3072.getAlgorithmName(),
          "RSA_3072 should have algorithm name RSA-3072");
    }

    @Test
    @DisplayName("should return RSA-4096 for RSA_4096")
    void shouldReturnCorrectNameForRsa4096() {
      assertEquals(
          "RSA-4096",
          AsymmetricAlgorithm.RSA_4096.getAlgorithmName(),
          "RSA_4096 should have algorithm name RSA-4096");
    }

    @Test
    @DisplayName("should return X25519 for X25519")
    void shouldReturnCorrectNameForX25519() {
      assertEquals(
          "X25519",
          AsymmetricAlgorithm.X25519.getAlgorithmName(),
          "X25519 should have algorithm name X25519");
    }

    @Test
    @DisplayName("should return X448 for X448")
    void shouldReturnCorrectNameForX448() {
      assertEquals(
          "X448",
          AsymmetricAlgorithm.X448.getAlgorithmName(),
          "X448 should have algorithm name X448");
    }

    @Test
    @DisplayName("should return ECDH-P256 for ECDH_P256")
    void shouldReturnCorrectNameForEcdhP256() {
      assertEquals(
          "ECDH-P256",
          AsymmetricAlgorithm.ECDH_P256.getAlgorithmName(),
          "ECDH_P256 should have algorithm name ECDH-P256");
    }

    @Test
    @DisplayName("should return ECDH-P384 for ECDH_P384")
    void shouldReturnCorrectNameForEcdhP384() {
      assertEquals(
          "ECDH-P384",
          AsymmetricAlgorithm.ECDH_P384.getAlgorithmName(),
          "ECDH_P384 should have algorithm name ECDH-P384");
    }

    @Test
    @DisplayName("should return ECDH-P521 for ECDH_P521")
    void shouldReturnCorrectNameForEcdhP521() {
      assertEquals(
          "ECDH-P521",
          AsymmetricAlgorithm.ECDH_P521.getAlgorithmName(),
          "ECDH_P521 should have algorithm name ECDH-P521");
    }

    @Test
    @DisplayName("should return ECDH-secp256k1 for ECDH_SECP256K1")
    void shouldReturnCorrectNameForEcdhSecp256k1() {
      assertEquals(
          "ECDH-secp256k1",
          AsymmetricAlgorithm.ECDH_SECP256K1.getAlgorithmName(),
          "ECDH_SECP256K1 should have algorithm name ECDH-secp256k1");
    }
  }

  @Nested
  @DisplayName("GetKeySize Tests")
  class GetKeySizeTests {

    @Test
    @DisplayName("should return 2048 for RSA_2048")
    void shouldReturnCorrectKeySizeForRsa2048() {
      assertEquals(
          2048,
          AsymmetricAlgorithm.RSA_2048.getKeySize(),
          "RSA_2048 should have key size 2048");
    }

    @Test
    @DisplayName("should return 3072 for RSA_3072")
    void shouldReturnCorrectKeySizeForRsa3072() {
      assertEquals(
          3072,
          AsymmetricAlgorithm.RSA_3072.getKeySize(),
          "RSA_3072 should have key size 3072");
    }

    @Test
    @DisplayName("should return 4096 for RSA_4096")
    void shouldReturnCorrectKeySizeForRsa4096() {
      assertEquals(
          4096,
          AsymmetricAlgorithm.RSA_4096.getKeySize(),
          "RSA_4096 should have key size 4096");
    }

    @Test
    @DisplayName("should return 256 for X25519")
    void shouldReturnCorrectKeySizeForX25519() {
      assertEquals(
          256,
          AsymmetricAlgorithm.X25519.getKeySize(),
          "X25519 should have key size 256");
    }

    @Test
    @DisplayName("should return 448 for X448")
    void shouldReturnCorrectKeySizeForX448() {
      assertEquals(
          448,
          AsymmetricAlgorithm.X448.getKeySize(),
          "X448 should have key size 448");
    }

    @Test
    @DisplayName("should return 256 for ECDH_P256")
    void shouldReturnCorrectKeySizeForEcdhP256() {
      assertEquals(
          256,
          AsymmetricAlgorithm.ECDH_P256.getKeySize(),
          "ECDH_P256 should have key size 256");
    }

    @Test
    @DisplayName("should return 384 for ECDH_P384")
    void shouldReturnCorrectKeySizeForEcdhP384() {
      assertEquals(
          384,
          AsymmetricAlgorithm.ECDH_P384.getKeySize(),
          "ECDH_P384 should have key size 384");
    }

    @Test
    @DisplayName("should return 521 for ECDH_P521")
    void shouldReturnCorrectKeySizeForEcdhP521() {
      assertEquals(
          521,
          AsymmetricAlgorithm.ECDH_P521.getKeySize(),
          "ECDH_P521 should have key size 521");
    }

    @Test
    @DisplayName("should return 256 for ECDH_SECP256K1")
    void shouldReturnCorrectKeySizeForEcdhSecp256k1() {
      assertEquals(
          256,
          AsymmetricAlgorithm.ECDH_SECP256K1.getKeySize(),
          "ECDH_SECP256K1 should have key size 256");
    }
  }
}
