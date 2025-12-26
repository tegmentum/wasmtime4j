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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CryptoErrorCode} enum.
 *
 * <p>CryptoErrorCode defines error codes for WASI-crypto operations.
 */
@DisplayName("CryptoErrorCode Tests")
class CryptoErrorCodeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(CryptoErrorCode.class.isEnum(), "CryptoErrorCode should be an enum");
    }

    @Test
    @DisplayName("should have UNKNOWN constant")
    void shouldHaveUnknownConstant() {
      assertNotNull(CryptoErrorCode.UNKNOWN, "UNKNOWN constant should exist");
    }

    @Test
    @DisplayName("should have UNSUPPORTED_ALGORITHM constant")
    void shouldHaveUnsupportedAlgorithmConstant() {
      assertNotNull(
          CryptoErrorCode.UNSUPPORTED_ALGORITHM, "UNSUPPORTED_ALGORITHM constant should exist");
    }

    @Test
    @DisplayName("should have UNSUPPORTED_OPERATION constant")
    void shouldHaveUnsupportedOperationConstant() {
      assertNotNull(
          CryptoErrorCode.UNSUPPORTED_OPERATION, "UNSUPPORTED_OPERATION constant should exist");
    }

    @Test
    @DisplayName("should have INVALID_KEY constant")
    void shouldHaveInvalidKeyConstant() {
      assertNotNull(CryptoErrorCode.INVALID_KEY, "INVALID_KEY constant should exist");
    }

    @Test
    @DisplayName("should have KEY_NOT_FOUND constant")
    void shouldHaveKeyNotFoundConstant() {
      assertNotNull(CryptoErrorCode.KEY_NOT_FOUND, "KEY_NOT_FOUND constant should exist");
    }

    @Test
    @DisplayName("should have INVALID_SIGNATURE constant")
    void shouldHaveInvalidSignatureConstant() {
      assertNotNull(CryptoErrorCode.INVALID_SIGNATURE, "INVALID_SIGNATURE constant should exist");
    }

    @Test
    @DisplayName("should have VERIFICATION_FAILED constant")
    void shouldHaveVerificationFailedConstant() {
      assertNotNull(
          CryptoErrorCode.VERIFICATION_FAILED, "VERIFICATION_FAILED constant should exist");
    }

    @Test
    @DisplayName("should have INVALID_INPUT constant")
    void shouldHaveInvalidInputConstant() {
      assertNotNull(CryptoErrorCode.INVALID_INPUT, "INVALID_INPUT constant should exist");
    }

    @Test
    @DisplayName("should have AUTHENTICATION_FAILED constant")
    void shouldHaveAuthenticationFailedConstant() {
      assertNotNull(
          CryptoErrorCode.AUTHENTICATION_FAILED, "AUTHENTICATION_FAILED constant should exist");
    }

    @Test
    @DisplayName("should have ENCRYPTION_FAILED constant")
    void shouldHaveEncryptionFailedConstant() {
      assertNotNull(CryptoErrorCode.ENCRYPTION_FAILED, "ENCRYPTION_FAILED constant should exist");
    }

    @Test
    @DisplayName("should have DECRYPTION_FAILED constant")
    void shouldHaveDecryptionFailedConstant() {
      assertNotNull(CryptoErrorCode.DECRYPTION_FAILED, "DECRYPTION_FAILED constant should exist");
    }

    @Test
    @DisplayName("should have KEY_GENERATION_FAILED constant")
    void shouldHaveKeyGenerationFailedConstant() {
      assertNotNull(
          CryptoErrorCode.KEY_GENERATION_FAILED, "KEY_GENERATION_FAILED constant should exist");
    }

    @Test
    @DisplayName("should have RNG_FAILED constant")
    void shouldHaveRngFailedConstant() {
      assertNotNull(CryptoErrorCode.RNG_FAILED, "RNG_FAILED constant should exist");
    }

    @Test
    @DisplayName("should have INTERNAL_ERROR constant")
    void shouldHaveInternalErrorConstant() {
      assertNotNull(CryptoErrorCode.INTERNAL_ERROR, "INTERNAL_ERROR constant should exist");
    }

    @Test
    @DisplayName("should have 20 error code types")
    void shouldHave20ErrorCodeTypes() {
      assertEquals(20, CryptoErrorCode.values().length, "Should have 20 error code types");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("should have all key-related error codes")
    void shouldHaveAllKeyRelatedErrorCodes() {
      assertNotNull(CryptoErrorCode.INVALID_KEY, "INVALID_KEY should exist");
      assertNotNull(CryptoErrorCode.KEY_NOT_FOUND, "KEY_NOT_FOUND should exist");
      assertNotNull(CryptoErrorCode.KEY_GENERATION_FAILED, "KEY_GENERATION_FAILED should exist");
    }

    @Test
    @DisplayName("should have all crypto operation error codes")
    void shouldHaveAllCryptoOperationErrorCodes() {
      assertNotNull(CryptoErrorCode.ENCRYPTION_FAILED, "ENCRYPTION_FAILED should exist");
      assertNotNull(CryptoErrorCode.DECRYPTION_FAILED, "DECRYPTION_FAILED should exist");
      assertNotNull(CryptoErrorCode.AUTHENTICATION_FAILED, "AUTHENTICATION_FAILED should exist");
      assertNotNull(CryptoErrorCode.VERIFICATION_FAILED, "VERIFICATION_FAILED should exist");
    }

    @Test
    @DisplayName("should have all validation error codes")
    void shouldHaveAllValidationErrorCodes() {
      assertNotNull(CryptoErrorCode.INVALID_INPUT, "INVALID_INPUT should exist");
      assertNotNull(CryptoErrorCode.INVALID_OUTPUT_SIZE, "INVALID_OUTPUT_SIZE should exist");
      assertNotNull(CryptoErrorCode.INVALID_NONCE, "INVALID_NONCE should exist");
      assertNotNull(CryptoErrorCode.INVALID_SIGNATURE, "INVALID_SIGNATURE should exist");
    }

    @Test
    @DisplayName("should have all system error codes")
    void shouldHaveAllSystemErrorCodes() {
      assertNotNull(CryptoErrorCode.HARDWARE_UNAVAILABLE, "HARDWARE_UNAVAILABLE should exist");
      assertNotNull(CryptoErrorCode.NOT_PERMITTED, "NOT_PERMITTED should exist");
      assertNotNull(CryptoErrorCode.RESOURCE_EXHAUSTED, "RESOURCE_EXHAUSTED should exist");
      assertNotNull(CryptoErrorCode.TIMEOUT, "TIMEOUT should exist");
      assertNotNull(CryptoErrorCode.INTERNAL_ERROR, "INTERNAL_ERROR should exist");
    }
  }
}
