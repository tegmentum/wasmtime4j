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

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link KeyDerivationAlgorithm} enum.
 *
 * <p>KeyDerivationAlgorithm defines key derivation function algorithms supported by WASI-crypto.
 */
@DisplayName("KeyDerivationAlgorithm Tests")
class KeyDerivationAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(KeyDerivationAlgorithm.class.isEnum(), "KeyDerivationAlgorithm should be an enum");
    }

    @Test
    @DisplayName("should have HKDF_SHA256 constant")
    void shouldHaveHkdfSha256Constant() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA256, "HKDF_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have HKDF_SHA384 constant")
    void shouldHaveHkdfSha384Constant() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA384, "HKDF_SHA384 constant should exist");
    }

    @Test
    @DisplayName("should have HKDF_SHA512 constant")
    void shouldHaveHkdfSha512Constant() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA512, "HKDF_SHA512 constant should exist");
    }

    @Test
    @DisplayName("should have PBKDF2_HMAC_SHA256 constant")
    void shouldHavePbkdf2HmacSha256Constant() {
      assertNotNull(
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA256, "PBKDF2_HMAC_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have PBKDF2_HMAC_SHA384 constant")
    void shouldHavePbkdf2HmacSha384Constant() {
      assertNotNull(
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA384, "PBKDF2_HMAC_SHA384 constant should exist");
    }

    @Test
    @DisplayName("should have PBKDF2_HMAC_SHA512 constant")
    void shouldHavePbkdf2HmacSha512Constant() {
      assertNotNull(
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA512, "PBKDF2_HMAC_SHA512 constant should exist");
    }

    @Test
    @DisplayName("should have ARGON2ID constant")
    void shouldHaveArgon2idConstant() {
      assertNotNull(KeyDerivationAlgorithm.ARGON2ID, "ARGON2ID constant should exist");
    }

    @Test
    @DisplayName("should have ARGON2I constant")
    void shouldHaveArgon2iConstant() {
      assertNotNull(KeyDerivationAlgorithm.ARGON2I, "ARGON2I constant should exist");
    }

    @Test
    @DisplayName("should have SCRYPT constant")
    void shouldHaveScryptConstant() {
      assertNotNull(KeyDerivationAlgorithm.SCRYPT, "SCRYPT constant should exist");
    }

    @Test
    @DisplayName("should have BCRYPT constant")
    void shouldHaveBcryptConstant() {
      assertNotNull(KeyDerivationAlgorithm.BCRYPT, "BCRYPT constant should exist");
    }

    @Test
    @DisplayName("should have 10 key derivation algorithm types")
    void shouldHave10KeyDerivationAlgorithmTypes() {
      assertEquals(
          10,
          KeyDerivationAlgorithm.values().length,
          "Should have 10 key derivation algorithm types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() throws NoSuchMethodException {
      final Method method = KeyDerivationAlgorithm.class.getMethod("getAlgorithmName");
      assertNotNull(method, "getAlgorithmName method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithmName should return String");
    }
  }

  @Nested
  @DisplayName("Algorithm Properties Tests")
  class AlgorithmPropertiesTests {

    @Test
    @DisplayName("HKDF_SHA256 should have correct algorithm name")
    void hkdfSha256ShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "HKDF-SHA256",
          KeyDerivationAlgorithm.HKDF_SHA256.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("HKDF_SHA384 should have correct algorithm name")
    void hkdfSha384ShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "HKDF-SHA384",
          KeyDerivationAlgorithm.HKDF_SHA384.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("HKDF_SHA512 should have correct algorithm name")
    void hkdfSha512ShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "HKDF-SHA512",
          KeyDerivationAlgorithm.HKDF_SHA512.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("PBKDF2_HMAC_SHA256 should have correct algorithm name")
    void pbkdf2HmacSha256ShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "PBKDF2-HMAC-SHA256",
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA256.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("PBKDF2_HMAC_SHA384 should have correct algorithm name")
    void pbkdf2HmacSha384ShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "PBKDF2-HMAC-SHA384",
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA384.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("PBKDF2_HMAC_SHA512 should have correct algorithm name")
    void pbkdf2HmacSha512ShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "PBKDF2-HMAC-SHA512",
          KeyDerivationAlgorithm.PBKDF2_HMAC_SHA512.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("ARGON2ID should have correct algorithm name")
    void argon2idShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "Argon2id",
          KeyDerivationAlgorithm.ARGON2ID.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("ARGON2I should have correct algorithm name")
    void argon2iShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "Argon2i",
          KeyDerivationAlgorithm.ARGON2I.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("SCRYPT should have correct algorithm name")
    void scryptShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "scrypt",
          KeyDerivationAlgorithm.SCRYPT.getAlgorithmName(),
          "Algorithm name should match");
    }

    @Test
    @DisplayName("BCRYPT should have correct algorithm name")
    void bcryptShouldHaveCorrectAlgorithmName() {
      assertEquals(
          "bcrypt",
          KeyDerivationAlgorithm.BCRYPT.getAlgorithmName(),
          "Algorithm name should match");
    }
  }

  @Nested
  @DisplayName("Algorithm Category Tests")
  class AlgorithmCategoryTests {

    @Test
    @DisplayName("should have HKDF algorithms")
    void shouldHaveHkdfAlgorithms() {
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA256, "HKDF_SHA256 should exist");
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA384, "HKDF_SHA384 should exist");
      assertNotNull(KeyDerivationAlgorithm.HKDF_SHA512, "HKDF_SHA512 should exist");
    }

    @Test
    @DisplayName("should have PBKDF2 algorithms")
    void shouldHavePbkdf2Algorithms() {
      assertNotNull(KeyDerivationAlgorithm.PBKDF2_HMAC_SHA256, "PBKDF2_HMAC_SHA256 should exist");
      assertNotNull(KeyDerivationAlgorithm.PBKDF2_HMAC_SHA384, "PBKDF2_HMAC_SHA384 should exist");
      assertNotNull(KeyDerivationAlgorithm.PBKDF2_HMAC_SHA512, "PBKDF2_HMAC_SHA512 should exist");
    }

    @Test
    @DisplayName("should have password hashing algorithms")
    void shouldHavePasswordHashingAlgorithms() {
      assertNotNull(KeyDerivationAlgorithm.ARGON2ID, "ARGON2ID should exist");
      assertNotNull(KeyDerivationAlgorithm.ARGON2I, "ARGON2I should exist");
      assertNotNull(KeyDerivationAlgorithm.SCRYPT, "SCRYPT should exist");
      assertNotNull(KeyDerivationAlgorithm.BCRYPT, "BCRYPT should exist");
    }
  }
}
