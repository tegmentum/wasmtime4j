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
 * Tests for {@link AsymmetricAlgorithm} enum.
 *
 * <p>AsymmetricAlgorithm defines asymmetric encryption algorithms supported by WASI-crypto.
 */
@DisplayName("AsymmetricAlgorithm Tests")
class AsymmetricAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(AsymmetricAlgorithm.class.isEnum(), "AsymmetricAlgorithm should be an enum");
    }

    @Test
    @DisplayName("should have RSA_2048 constant")
    void shouldHaveRsa2048Constant() {
      assertNotNull(AsymmetricAlgorithm.RSA_2048, "RSA_2048 constant should exist");
    }

    @Test
    @DisplayName("should have RSA_3072 constant")
    void shouldHaveRsa3072Constant() {
      assertNotNull(AsymmetricAlgorithm.RSA_3072, "RSA_3072 constant should exist");
    }

    @Test
    @DisplayName("should have RSA_4096 constant")
    void shouldHaveRsa4096Constant() {
      assertNotNull(AsymmetricAlgorithm.RSA_4096, "RSA_4096 constant should exist");
    }

    @Test
    @DisplayName("should have X25519 constant")
    void shouldHaveX25519Constant() {
      assertNotNull(AsymmetricAlgorithm.X25519, "X25519 constant should exist");
    }

    @Test
    @DisplayName("should have X448 constant")
    void shouldHaveX448Constant() {
      assertNotNull(AsymmetricAlgorithm.X448, "X448 constant should exist");
    }

    @Test
    @DisplayName("should have ECDH_P256 constant")
    void shouldHaveEcdhP256Constant() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P256, "ECDH_P256 constant should exist");
    }

    @Test
    @DisplayName("should have ECDH_P384 constant")
    void shouldHaveEcdhP384Constant() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P384, "ECDH_P384 constant should exist");
    }

    @Test
    @DisplayName("should have ECDH_P521 constant")
    void shouldHaveEcdhP521Constant() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P521, "ECDH_P521 constant should exist");
    }

    @Test
    @DisplayName("should have ECDH_SECP256K1 constant")
    void shouldHaveEcdhSecp256k1Constant() {
      assertNotNull(AsymmetricAlgorithm.ECDH_SECP256K1, "ECDH_SECP256K1 constant should exist");
    }

    @Test
    @DisplayName("should have 9 asymmetric algorithm types")
    void shouldHave9AsymmetricAlgorithmTypes() {
      assertEquals(
          9, AsymmetricAlgorithm.values().length, "Should have 9 asymmetric algorithm types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() throws NoSuchMethodException {
      final Method method = AsymmetricAlgorithm.class.getMethod("getAlgorithmName");
      assertNotNull(method, "getAlgorithmName method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithmName should return String");
    }

    @Test
    @DisplayName("should have getKeySize method")
    void shouldHaveGetKeySizeMethod() throws NoSuchMethodException {
      final Method method = AsymmetricAlgorithm.class.getMethod("getKeySize");
      assertNotNull(method, "getKeySize method should exist");
      assertEquals(int.class, method.getReturnType(), "getKeySize should return int");
    }
  }

  @Nested
  @DisplayName("Algorithm Properties Tests")
  class AlgorithmPropertiesTests {

    @Test
    @DisplayName("RSA_2048 should have correct properties")
    void rsa2048ShouldHaveCorrectProperties() {
      assertEquals(
          "RSA-2048",
          AsymmetricAlgorithm.RSA_2048.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(2048, AsymmetricAlgorithm.RSA_2048.getKeySize(), "Key size should be 2048 bits");
    }

    @Test
    @DisplayName("RSA_3072 should have correct properties")
    void rsa3072ShouldHaveCorrectProperties() {
      assertEquals(
          "RSA-3072",
          AsymmetricAlgorithm.RSA_3072.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(3072, AsymmetricAlgorithm.RSA_3072.getKeySize(), "Key size should be 3072 bits");
    }

    @Test
    @DisplayName("RSA_4096 should have correct properties")
    void rsa4096ShouldHaveCorrectProperties() {
      assertEquals(
          "RSA-4096",
          AsymmetricAlgorithm.RSA_4096.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(4096, AsymmetricAlgorithm.RSA_4096.getKeySize(), "Key size should be 4096 bits");
    }

    @Test
    @DisplayName("X25519 should have correct properties")
    void x25519ShouldHaveCorrectProperties() {
      assertEquals(
          "X25519", AsymmetricAlgorithm.X25519.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, AsymmetricAlgorithm.X25519.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("X448 should have correct properties")
    void x448ShouldHaveCorrectProperties() {
      assertEquals(
          "X448", AsymmetricAlgorithm.X448.getAlgorithmName(), "Algorithm name should match");
      assertEquals(448, AsymmetricAlgorithm.X448.getKeySize(), "Key size should be 448 bits");
    }

    @Test
    @DisplayName("ECDH_P256 should have correct properties")
    void ecdhP256ShouldHaveCorrectProperties() {
      assertEquals(
          "ECDH-P256",
          AsymmetricAlgorithm.ECDH_P256.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(256, AsymmetricAlgorithm.ECDH_P256.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("ECDH_P384 should have correct properties")
    void ecdhP384ShouldHaveCorrectProperties() {
      assertEquals(
          "ECDH-P384",
          AsymmetricAlgorithm.ECDH_P384.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(384, AsymmetricAlgorithm.ECDH_P384.getKeySize(), "Key size should be 384 bits");
    }

    @Test
    @DisplayName("ECDH_P521 should have correct properties")
    void ecdhP521ShouldHaveCorrectProperties() {
      assertEquals(
          "ECDH-P521",
          AsymmetricAlgorithm.ECDH_P521.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(521, AsymmetricAlgorithm.ECDH_P521.getKeySize(), "Key size should be 521 bits");
    }

    @Test
    @DisplayName("ECDH_SECP256K1 should have correct properties")
    void ecdhSecp256k1ShouldHaveCorrectProperties() {
      assertEquals(
          "ECDH-secp256k1",
          AsymmetricAlgorithm.ECDH_SECP256K1.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          256, AsymmetricAlgorithm.ECDH_SECP256K1.getKeySize(), "Key size should be 256 bits");
    }
  }

  @Nested
  @DisplayName("Algorithm Category Tests")
  class AlgorithmCategoryTests {

    @Test
    @DisplayName("should have RSA algorithms")
    void shouldHaveRsaAlgorithms() {
      assertNotNull(AsymmetricAlgorithm.RSA_2048, "RSA_2048 should exist");
      assertNotNull(AsymmetricAlgorithm.RSA_3072, "RSA_3072 should exist");
      assertNotNull(AsymmetricAlgorithm.RSA_4096, "RSA_4096 should exist");
    }

    @Test
    @DisplayName("should have curve25519 algorithms")
    void shouldHaveCurve25519Algorithms() {
      assertNotNull(AsymmetricAlgorithm.X25519, "X25519 should exist");
      assertNotNull(AsymmetricAlgorithm.X448, "X448 should exist");
    }

    @Test
    @DisplayName("should have ECDH algorithms")
    void shouldHaveEcdhAlgorithms() {
      assertNotNull(AsymmetricAlgorithm.ECDH_P256, "ECDH_P256 should exist");
      assertNotNull(AsymmetricAlgorithm.ECDH_P384, "ECDH_P384 should exist");
      assertNotNull(AsymmetricAlgorithm.ECDH_P521, "ECDH_P521 should exist");
      assertNotNull(AsymmetricAlgorithm.ECDH_SECP256K1, "ECDH_SECP256K1 should exist");
    }
  }
}
