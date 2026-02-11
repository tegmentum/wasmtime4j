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

/** Tests for {@link CryptoErrorCode} enum. */
@DisplayName("CryptoErrorCode Tests")
class CryptoErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(CryptoErrorCode.class.isEnum(), "CryptoErrorCode should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 20 enum values")
    void shouldHaveExactlyTwentyEnumValues() {
      final CryptoErrorCode[] values = CryptoErrorCode.values();

      assertEquals(20, values.length, "CryptoErrorCode should have exactly 20 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have UNKNOWN value")
    void shouldHaveUnknownValue() {
      assertNotNull(CryptoErrorCode.UNKNOWN, "UNKNOWN should not be null");
    }

    @Test
    @DisplayName("should have UNSUPPORTED_ALGORITHM value")
    void shouldHaveUnsupportedAlgorithmValue() {
      assertNotNull(
          CryptoErrorCode.UNSUPPORTED_ALGORITHM, "UNSUPPORTED_ALGORITHM should not be null");
    }

    @Test
    @DisplayName("should have UNSUPPORTED_OPERATION value")
    void shouldHaveUnsupportedOperationValue() {
      assertNotNull(
          CryptoErrorCode.UNSUPPORTED_OPERATION, "UNSUPPORTED_OPERATION should not be null");
    }

    @Test
    @DisplayName("should have INVALID_KEY value")
    void shouldHaveInvalidKeyValue() {
      assertNotNull(CryptoErrorCode.INVALID_KEY, "INVALID_KEY should not be null");
    }

    @Test
    @DisplayName("should have KEY_NOT_FOUND value")
    void shouldHaveKeyNotFoundValue() {
      assertNotNull(CryptoErrorCode.KEY_NOT_FOUND, "KEY_NOT_FOUND should not be null");
    }

    @Test
    @DisplayName("should have INVALID_SIGNATURE value")
    void shouldHaveInvalidSignatureValue() {
      assertNotNull(CryptoErrorCode.INVALID_SIGNATURE, "INVALID_SIGNATURE should not be null");
    }

    @Test
    @DisplayName("should have VERIFICATION_FAILED value")
    void shouldHaveVerificationFailedValue() {
      assertNotNull(CryptoErrorCode.VERIFICATION_FAILED, "VERIFICATION_FAILED should not be null");
    }

    @Test
    @DisplayName("should have INVALID_INPUT value")
    void shouldHaveInvalidInputValue() {
      assertNotNull(CryptoErrorCode.INVALID_INPUT, "INVALID_INPUT should not be null");
    }

    @Test
    @DisplayName("should have INVALID_OUTPUT_SIZE value")
    void shouldHaveInvalidOutputSizeValue() {
      assertNotNull(CryptoErrorCode.INVALID_OUTPUT_SIZE, "INVALID_OUTPUT_SIZE should not be null");
    }

    @Test
    @DisplayName("should have INVALID_NONCE value")
    void shouldHaveInvalidNonceValue() {
      assertNotNull(CryptoErrorCode.INVALID_NONCE, "INVALID_NONCE should not be null");
    }

    @Test
    @DisplayName("should have AUTHENTICATION_FAILED value")
    void shouldHaveAuthenticationFailedValue() {
      assertNotNull(
          CryptoErrorCode.AUTHENTICATION_FAILED, "AUTHENTICATION_FAILED should not be null");
    }

    @Test
    @DisplayName("should have ENCRYPTION_FAILED value")
    void shouldHaveEncryptionFailedValue() {
      assertNotNull(CryptoErrorCode.ENCRYPTION_FAILED, "ENCRYPTION_FAILED should not be null");
    }

    @Test
    @DisplayName("should have DECRYPTION_FAILED value")
    void shouldHaveDecryptionFailedValue() {
      assertNotNull(CryptoErrorCode.DECRYPTION_FAILED, "DECRYPTION_FAILED should not be null");
    }

    @Test
    @DisplayName("should have KEY_GENERATION_FAILED value")
    void shouldHaveKeyGenerationFailedValue() {
      assertNotNull(
          CryptoErrorCode.KEY_GENERATION_FAILED, "KEY_GENERATION_FAILED should not be null");
    }

    @Test
    @DisplayName("should have RNG_FAILED value")
    void shouldHaveRngFailedValue() {
      assertNotNull(CryptoErrorCode.RNG_FAILED, "RNG_FAILED should not be null");
    }

    @Test
    @DisplayName("should have HARDWARE_UNAVAILABLE value")
    void shouldHaveHardwareUnavailableValue() {
      assertNotNull(
          CryptoErrorCode.HARDWARE_UNAVAILABLE, "HARDWARE_UNAVAILABLE should not be null");
    }

    @Test
    @DisplayName("should have NOT_PERMITTED value")
    void shouldHaveNotPermittedValue() {
      assertNotNull(CryptoErrorCode.NOT_PERMITTED, "NOT_PERMITTED should not be null");
    }

    @Test
    @DisplayName("should have RESOURCE_EXHAUSTED value")
    void shouldHaveResourceExhaustedValue() {
      assertNotNull(CryptoErrorCode.RESOURCE_EXHAUSTED, "RESOURCE_EXHAUSTED should not be null");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(CryptoErrorCode.TIMEOUT, "TIMEOUT should not be null");
    }

    @Test
    @DisplayName("should have INTERNAL_ERROR value")
    void shouldHaveInternalErrorValue() {
      assertNotNull(CryptoErrorCode.INTERNAL_ERROR, "INTERNAL_ERROR should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final CryptoErrorCode code : CryptoErrorCode.values()) {
        ordinals.add(code.ordinal());
      }

      assertEquals(
          CryptoErrorCode.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final CryptoErrorCode[] values = CryptoErrorCode.values();

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
      for (final CryptoErrorCode code : CryptoErrorCode.values()) {
        assertEquals(
            code,
            CryptoErrorCode.valueOf(code.name()),
            "valueOf should round-trip for " + code.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CryptoErrorCode.valueOf("INVALID_CODE"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final CryptoErrorCode[] first = CryptoErrorCode.values();
      final CryptoErrorCode[] second = CryptoErrorCode.values();

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
      for (final CryptoErrorCode code : CryptoErrorCode.values()) {
        assertEquals(
            code.name(), code.toString(), "toString should return name for " + code.name());
      }
    }
  }
}
