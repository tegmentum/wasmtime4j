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
 * Tests for {@link SignatureAlgorithm} enum.
 *
 * <p>SignatureAlgorithm defines digital signature algorithms supported by WASI-crypto.
 */
@DisplayName("SignatureAlgorithm Tests")
class SignatureAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(SignatureAlgorithm.class.isEnum(), "SignatureAlgorithm should be an enum");
    }

    @Test
    @DisplayName("should have RSA_PKCS1_SHA256 constant")
    void shouldHaveRsaPkcs1Sha256Constant() {
      assertNotNull(SignatureAlgorithm.RSA_PKCS1_SHA256, "RSA_PKCS1_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have RSA_PSS_SHA256 constant")
    void shouldHaveRsaPssSha256Constant() {
      assertNotNull(SignatureAlgorithm.RSA_PSS_SHA256, "RSA_PSS_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have ECDSA_P256_SHA256 constant")
    void shouldHaveEcdsaP256Sha256Constant() {
      assertNotNull(
          SignatureAlgorithm.ECDSA_P256_SHA256, "ECDSA_P256_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have ED25519 constant")
    void shouldHaveEd25519Constant() {
      assertNotNull(SignatureAlgorithm.ED25519, "ED25519 constant should exist");
    }

    @Test
    @DisplayName("should have ED448 constant")
    void shouldHaveEd448Constant() {
      assertNotNull(SignatureAlgorithm.ED448, "ED448 constant should exist");
    }

    @Test
    @DisplayName("should have 12 signature algorithm types")
    void shouldHave12SignatureAlgorithmTypes() {
      assertEquals(
          12, SignatureAlgorithm.values().length, "Should have 12 signature algorithm types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() throws NoSuchMethodException {
      final Method method = SignatureAlgorithm.class.getMethod("getAlgorithmName");
      assertNotNull(method, "getAlgorithmName method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithmName should return String");
    }

    @Test
    @DisplayName("should have getKeySize method")
    void shouldHaveGetKeySizeMethod() throws NoSuchMethodException {
      final Method method = SignatureAlgorithm.class.getMethod("getKeySize");
      assertNotNull(method, "getKeySize method should exist");
      assertEquals(int.class, method.getReturnType(), "getKeySize should return int");
    }
  }

  @Nested
  @DisplayName("Algorithm Properties Tests")
  class AlgorithmPropertiesTests {

    @Test
    @DisplayName("RSA_PKCS1_SHA256 should have correct properties")
    void rsaPkcs1Sha256ShouldHaveCorrectProperties() {
      assertEquals(
          "RSA-PKCS1-SHA256",
          SignatureAlgorithm.RSA_PKCS1_SHA256.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          2048, SignatureAlgorithm.RSA_PKCS1_SHA256.getKeySize(), "Key size should be 2048 bits");
    }

    @Test
    @DisplayName("RSA_PSS_SHA512 should have correct properties")
    void rsaPssSha512ShouldHaveCorrectProperties() {
      assertEquals(
          "RSA-PSS-SHA512",
          SignatureAlgorithm.RSA_PSS_SHA512.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          4096, SignatureAlgorithm.RSA_PSS_SHA512.getKeySize(), "Key size should be 4096 bits");
    }

    @Test
    @DisplayName("ECDSA_P256_SHA256 should have correct properties")
    void ecdsaP256Sha256ShouldHaveCorrectProperties() {
      assertEquals(
          "ECDSA-P256-SHA256",
          SignatureAlgorithm.ECDSA_P256_SHA256.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          256, SignatureAlgorithm.ECDSA_P256_SHA256.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("ED25519 should have correct properties")
    void ed25519ShouldHaveCorrectProperties() {
      assertEquals(
          "Ed25519", SignatureAlgorithm.ED25519.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, SignatureAlgorithm.ED25519.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("ED448 should have correct properties")
    void ed448ShouldHaveCorrectProperties() {
      assertEquals(
          "Ed448", SignatureAlgorithm.ED448.getAlgorithmName(), "Algorithm name should match");
      assertEquals(448, SignatureAlgorithm.ED448.getKeySize(), "Key size should be 448 bits");
    }

    @Test
    @DisplayName("ECDSA_SECP256K1_SHA256 should have correct properties")
    void ecdsaSecp256k1Sha256ShouldHaveCorrectProperties() {
      assertEquals(
          "ECDSA-secp256k1-SHA256",
          SignatureAlgorithm.ECDSA_SECP256K1_SHA256.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          256,
          SignatureAlgorithm.ECDSA_SECP256K1_SHA256.getKeySize(),
          "Key size should be 256 bits");
    }
  }
}
