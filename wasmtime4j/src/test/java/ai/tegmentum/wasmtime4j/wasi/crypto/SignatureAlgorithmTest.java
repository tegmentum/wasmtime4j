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

/** Tests for {@link SignatureAlgorithm} enum. */
@DisplayName("SignatureAlgorithm Tests")
class SignatureAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(SignatureAlgorithm.class.isEnum(), "SignatureAlgorithm should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 12 enum values")
    void shouldHaveExactlyTwelveEnumValues() {
      final SignatureAlgorithm[] values = SignatureAlgorithm.values();

      assertEquals(12, values.length, "SignatureAlgorithm should have exactly 12 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have RSA_PKCS1_SHA256 value")
    void shouldHaveRsaPkcs1Sha256Value() {
      assertNotNull(SignatureAlgorithm.RSA_PKCS1_SHA256, "RSA_PKCS1_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have RSA_PKCS1_SHA384 value")
    void shouldHaveRsaPkcs1Sha384Value() {
      assertNotNull(SignatureAlgorithm.RSA_PKCS1_SHA384, "RSA_PKCS1_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have RSA_PKCS1_SHA512 value")
    void shouldHaveRsaPkcs1Sha512Value() {
      assertNotNull(SignatureAlgorithm.RSA_PKCS1_SHA512, "RSA_PKCS1_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have RSA_PSS_SHA256 value")
    void shouldHaveRsaPssSha256Value() {
      assertNotNull(SignatureAlgorithm.RSA_PSS_SHA256, "RSA_PSS_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have RSA_PSS_SHA384 value")
    void shouldHaveRsaPssSha384Value() {
      assertNotNull(SignatureAlgorithm.RSA_PSS_SHA384, "RSA_PSS_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have RSA_PSS_SHA512 value")
    void shouldHaveRsaPssSha512Value() {
      assertNotNull(SignatureAlgorithm.RSA_PSS_SHA512, "RSA_PSS_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have ECDSA_P256_SHA256 value")
    void shouldHaveEcdsaP256Sha256Value() {
      assertNotNull(SignatureAlgorithm.ECDSA_P256_SHA256, "ECDSA_P256_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have ECDSA_P384_SHA384 value")
    void shouldHaveEcdsaP384Sha384Value() {
      assertNotNull(SignatureAlgorithm.ECDSA_P384_SHA384, "ECDSA_P384_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have ECDSA_P521_SHA512 value")
    void shouldHaveEcdsaP521Sha512Value() {
      assertNotNull(SignatureAlgorithm.ECDSA_P521_SHA512, "ECDSA_P521_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have ECDSA_SECP256K1_SHA256 value")
    void shouldHaveEcdsaSecp256k1Sha256Value() {
      assertNotNull(
          SignatureAlgorithm.ECDSA_SECP256K1_SHA256, "ECDSA_SECP256K1_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have ED25519 value")
    void shouldHaveEd25519Value() {
      assertNotNull(SignatureAlgorithm.ED25519, "ED25519 should not be null");
    }

    @Test
    @DisplayName("should have ED448 value")
    void shouldHaveEd448Value() {
      assertNotNull(SignatureAlgorithm.ED448, "ED448 should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final SignatureAlgorithm algorithm : SignatureAlgorithm.values()) {
        ordinals.add(algorithm.ordinal());
      }

      assertEquals(
          SignatureAlgorithm.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final SignatureAlgorithm[] values = SignatureAlgorithm.values();

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
      for (final SignatureAlgorithm algorithm : SignatureAlgorithm.values()) {
        assertEquals(
            algorithm,
            SignatureAlgorithm.valueOf(algorithm.name()),
            "valueOf should round-trip for " + algorithm.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> SignatureAlgorithm.valueOf("INVALID_SIG"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final SignatureAlgorithm[] first = SignatureAlgorithm.values();
      final SignatureAlgorithm[] second = SignatureAlgorithm.values();

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
      for (final SignatureAlgorithm algorithm : SignatureAlgorithm.values()) {
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
    @DisplayName("should return RSA-PKCS1-SHA256 for RSA_PKCS1_SHA256")
    void shouldReturnCorrectNameForRsaPkcs1Sha256() {
      assertEquals(
          "RSA-PKCS1-SHA256",
          SignatureAlgorithm.RSA_PKCS1_SHA256.getAlgorithmName(),
          "RSA_PKCS1_SHA256 should have algorithm name RSA-PKCS1-SHA256");
    }

    @Test
    @DisplayName("should return RSA-PKCS1-SHA384 for RSA_PKCS1_SHA384")
    void shouldReturnCorrectNameForRsaPkcs1Sha384() {
      assertEquals(
          "RSA-PKCS1-SHA384",
          SignatureAlgorithm.RSA_PKCS1_SHA384.getAlgorithmName(),
          "RSA_PKCS1_SHA384 should have algorithm name RSA-PKCS1-SHA384");
    }

    @Test
    @DisplayName("should return RSA-PKCS1-SHA512 for RSA_PKCS1_SHA512")
    void shouldReturnCorrectNameForRsaPkcs1Sha512() {
      assertEquals(
          "RSA-PKCS1-SHA512",
          SignatureAlgorithm.RSA_PKCS1_SHA512.getAlgorithmName(),
          "RSA_PKCS1_SHA512 should have algorithm name RSA-PKCS1-SHA512");
    }

    @Test
    @DisplayName("should return RSA-PSS-SHA256 for RSA_PSS_SHA256")
    void shouldReturnCorrectNameForRsaPssSha256() {
      assertEquals(
          "RSA-PSS-SHA256",
          SignatureAlgorithm.RSA_PSS_SHA256.getAlgorithmName(),
          "RSA_PSS_SHA256 should have algorithm name RSA-PSS-SHA256");
    }

    @Test
    @DisplayName("should return RSA-PSS-SHA384 for RSA_PSS_SHA384")
    void shouldReturnCorrectNameForRsaPssSha384() {
      assertEquals(
          "RSA-PSS-SHA384",
          SignatureAlgorithm.RSA_PSS_SHA384.getAlgorithmName(),
          "RSA_PSS_SHA384 should have algorithm name RSA-PSS-SHA384");
    }

    @Test
    @DisplayName("should return RSA-PSS-SHA512 for RSA_PSS_SHA512")
    void shouldReturnCorrectNameForRsaPssSha512() {
      assertEquals(
          "RSA-PSS-SHA512",
          SignatureAlgorithm.RSA_PSS_SHA512.getAlgorithmName(),
          "RSA_PSS_SHA512 should have algorithm name RSA-PSS-SHA512");
    }

    @Test
    @DisplayName("should return ECDSA-P256-SHA256 for ECDSA_P256_SHA256")
    void shouldReturnCorrectNameForEcdsaP256Sha256() {
      assertEquals(
          "ECDSA-P256-SHA256",
          SignatureAlgorithm.ECDSA_P256_SHA256.getAlgorithmName(),
          "ECDSA_P256_SHA256 should have algorithm name ECDSA-P256-SHA256");
    }

    @Test
    @DisplayName("should return ECDSA-P384-SHA384 for ECDSA_P384_SHA384")
    void shouldReturnCorrectNameForEcdsaP384Sha384() {
      assertEquals(
          "ECDSA-P384-SHA384",
          SignatureAlgorithm.ECDSA_P384_SHA384.getAlgorithmName(),
          "ECDSA_P384_SHA384 should have algorithm name ECDSA-P384-SHA384");
    }

    @Test
    @DisplayName("should return ECDSA-P521-SHA512 for ECDSA_P521_SHA512")
    void shouldReturnCorrectNameForEcdsaP521Sha512() {
      assertEquals(
          "ECDSA-P521-SHA512",
          SignatureAlgorithm.ECDSA_P521_SHA512.getAlgorithmName(),
          "ECDSA_P521_SHA512 should have algorithm name ECDSA-P521-SHA512");
    }

    @Test
    @DisplayName("should return ECDSA-secp256k1-SHA256 for ECDSA_SECP256K1_SHA256")
    void shouldReturnCorrectNameForEcdsaSecp256k1Sha256() {
      assertEquals(
          "ECDSA-secp256k1-SHA256",
          SignatureAlgorithm.ECDSA_SECP256K1_SHA256.getAlgorithmName(),
          "ECDSA_SECP256K1_SHA256 should have algorithm name ECDSA-secp256k1-SHA256");
    }

    @Test
    @DisplayName("should return Ed25519 for ED25519")
    void shouldReturnCorrectNameForEd25519() {
      assertEquals(
          "Ed25519",
          SignatureAlgorithm.ED25519.getAlgorithmName(),
          "ED25519 should have algorithm name Ed25519");
    }

    @Test
    @DisplayName("should return Ed448 for ED448")
    void shouldReturnCorrectNameForEd448() {
      assertEquals(
          "Ed448",
          SignatureAlgorithm.ED448.getAlgorithmName(),
          "ED448 should have algorithm name Ed448");
    }
  }

  @Nested
  @DisplayName("GetKeySize Tests")
  class GetKeySizeTests {

    @Test
    @DisplayName("should return 2048 for RSA_PKCS1_SHA256")
    void shouldReturnCorrectKeySizeForRsaPkcs1Sha256() {
      assertEquals(
          2048,
          SignatureAlgorithm.RSA_PKCS1_SHA256.getKeySize(),
          "RSA_PKCS1_SHA256 should have key size 2048");
    }

    @Test
    @DisplayName("should return 3072 for RSA_PKCS1_SHA384")
    void shouldReturnCorrectKeySizeForRsaPkcs1Sha384() {
      assertEquals(
          3072,
          SignatureAlgorithm.RSA_PKCS1_SHA384.getKeySize(),
          "RSA_PKCS1_SHA384 should have key size 3072");
    }

    @Test
    @DisplayName("should return 4096 for RSA_PKCS1_SHA512")
    void shouldReturnCorrectKeySizeForRsaPkcs1Sha512() {
      assertEquals(
          4096,
          SignatureAlgorithm.RSA_PKCS1_SHA512.getKeySize(),
          "RSA_PKCS1_SHA512 should have key size 4096");
    }

    @Test
    @DisplayName("should return 2048 for RSA_PSS_SHA256")
    void shouldReturnCorrectKeySizeForRsaPssSha256() {
      assertEquals(
          2048,
          SignatureAlgorithm.RSA_PSS_SHA256.getKeySize(),
          "RSA_PSS_SHA256 should have key size 2048");
    }

    @Test
    @DisplayName("should return 3072 for RSA_PSS_SHA384")
    void shouldReturnCorrectKeySizeForRsaPssSha384() {
      assertEquals(
          3072,
          SignatureAlgorithm.RSA_PSS_SHA384.getKeySize(),
          "RSA_PSS_SHA384 should have key size 3072");
    }

    @Test
    @DisplayName("should return 4096 for RSA_PSS_SHA512")
    void shouldReturnCorrectKeySizeForRsaPssSha512() {
      assertEquals(
          4096,
          SignatureAlgorithm.RSA_PSS_SHA512.getKeySize(),
          "RSA_PSS_SHA512 should have key size 4096");
    }

    @Test
    @DisplayName("should return 256 for ECDSA_P256_SHA256")
    void shouldReturnCorrectKeySizeForEcdsaP256Sha256() {
      assertEquals(
          256,
          SignatureAlgorithm.ECDSA_P256_SHA256.getKeySize(),
          "ECDSA_P256_SHA256 should have key size 256");
    }

    @Test
    @DisplayName("should return 384 for ECDSA_P384_SHA384")
    void shouldReturnCorrectKeySizeForEcdsaP384Sha384() {
      assertEquals(
          384,
          SignatureAlgorithm.ECDSA_P384_SHA384.getKeySize(),
          "ECDSA_P384_SHA384 should have key size 384");
    }

    @Test
    @DisplayName("should return 521 for ECDSA_P521_SHA512")
    void shouldReturnCorrectKeySizeForEcdsaP521Sha512() {
      assertEquals(
          521,
          SignatureAlgorithm.ECDSA_P521_SHA512.getKeySize(),
          "ECDSA_P521_SHA512 should have key size 521");
    }

    @Test
    @DisplayName("should return 256 for ECDSA_SECP256K1_SHA256")
    void shouldReturnCorrectKeySizeForEcdsaSecp256k1Sha256() {
      assertEquals(
          256,
          SignatureAlgorithm.ECDSA_SECP256K1_SHA256.getKeySize(),
          "ECDSA_SECP256K1_SHA256 should have key size 256");
    }

    @Test
    @DisplayName("should return 256 for ED25519")
    void shouldReturnCorrectKeySizeForEd25519() {
      assertEquals(
          256, SignatureAlgorithm.ED25519.getKeySize(), "ED25519 should have key size 256");
    }

    @Test
    @DisplayName("should return 448 for ED448")
    void shouldReturnCorrectKeySizeForEd448() {
      assertEquals(448, SignatureAlgorithm.ED448.getKeySize(), "ED448 should have key size 448");
    }
  }
}
