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
 * Tests for {@link MacAlgorithm} enum.
 *
 * <p>MacAlgorithm defines message authentication code algorithms supported by WASI-crypto.
 */
@DisplayName("MacAlgorithm Tests")
class MacAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(MacAlgorithm.class.isEnum(), "MacAlgorithm should be an enum");
    }

    @Test
    @DisplayName("should have HMAC_SHA256 constant")
    void shouldHaveHmacSha256Constant() {
      assertNotNull(MacAlgorithm.HMAC_SHA256, "HMAC_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have HMAC_SHA384 constant")
    void shouldHaveHmacSha384Constant() {
      assertNotNull(MacAlgorithm.HMAC_SHA384, "HMAC_SHA384 constant should exist");
    }

    @Test
    @DisplayName("should have HMAC_SHA512 constant")
    void shouldHaveHmacSha512Constant() {
      assertNotNull(MacAlgorithm.HMAC_SHA512, "HMAC_SHA512 constant should exist");
    }

    @Test
    @DisplayName("should have HMAC_SHA3_256 constant")
    void shouldHaveHmacSha3256Constant() {
      assertNotNull(MacAlgorithm.HMAC_SHA3_256, "HMAC_SHA3_256 constant should exist");
    }

    @Test
    @DisplayName("should have POLY1305 constant")
    void shouldHavePoly1305Constant() {
      assertNotNull(MacAlgorithm.POLY1305, "POLY1305 constant should exist");
    }

    @Test
    @DisplayName("should have BLAKE2B_MAC constant")
    void shouldHaveBlake2bMacConstant() {
      assertNotNull(MacAlgorithm.BLAKE2B_MAC, "BLAKE2B_MAC constant should exist");
    }

    @Test
    @DisplayName("should have BLAKE2S_MAC constant")
    void shouldHaveBlake2sMacConstant() {
      assertNotNull(MacAlgorithm.BLAKE2S_MAC, "BLAKE2S_MAC constant should exist");
    }

    @Test
    @DisplayName("should have AES_CMAC constant")
    void shouldHaveAesCmacConstant() {
      assertNotNull(MacAlgorithm.AES_CMAC, "AES_CMAC constant should exist");
    }

    @Test
    @DisplayName("should have KMAC128 constant")
    void shouldHaveKmac128Constant() {
      assertNotNull(MacAlgorithm.KMAC128, "KMAC128 constant should exist");
    }

    @Test
    @DisplayName("should have KMAC256 constant")
    void shouldHaveKmac256Constant() {
      assertNotNull(MacAlgorithm.KMAC256, "KMAC256 constant should exist");
    }

    @Test
    @DisplayName("should have 10 MAC algorithm types")
    void shouldHave10MacAlgorithmTypes() {
      assertEquals(10, MacAlgorithm.values().length, "Should have 10 MAC algorithm types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() throws NoSuchMethodException {
      final Method method = MacAlgorithm.class.getMethod("getAlgorithmName");
      assertNotNull(method, "getAlgorithmName method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithmName should return String");
    }

    @Test
    @DisplayName("should have getOutputSize method")
    void shouldHaveGetOutputSizeMethod() throws NoSuchMethodException {
      final Method method = MacAlgorithm.class.getMethod("getOutputSize");
      assertNotNull(method, "getOutputSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getOutputSize should return int");
    }
  }

  @Nested
  @DisplayName("Algorithm Properties Tests")
  class AlgorithmPropertiesTests {

    @Test
    @DisplayName("HMAC_SHA256 should have correct properties")
    void hmacSha256ShouldHaveCorrectProperties() {
      assertEquals(
          "HMAC-SHA256",
          MacAlgorithm.HMAC_SHA256.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(256, MacAlgorithm.HMAC_SHA256.getOutputSize(), "Output size should be 256 bits");
    }

    @Test
    @DisplayName("HMAC_SHA384 should have correct properties")
    void hmacSha384ShouldHaveCorrectProperties() {
      assertEquals(
          "HMAC-SHA384",
          MacAlgorithm.HMAC_SHA384.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(384, MacAlgorithm.HMAC_SHA384.getOutputSize(), "Output size should be 384 bits");
    }

    @Test
    @DisplayName("HMAC_SHA512 should have correct properties")
    void hmacSha512ShouldHaveCorrectProperties() {
      assertEquals(
          "HMAC-SHA512",
          MacAlgorithm.HMAC_SHA512.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(512, MacAlgorithm.HMAC_SHA512.getOutputSize(), "Output size should be 512 bits");
    }

    @Test
    @DisplayName("HMAC_SHA3_256 should have correct properties")
    void hmacSha3256ShouldHaveCorrectProperties() {
      assertEquals(
          "HMAC-SHA3-256",
          MacAlgorithm.HMAC_SHA3_256.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          256, MacAlgorithm.HMAC_SHA3_256.getOutputSize(), "Output size should be 256 bits");
    }

    @Test
    @DisplayName("POLY1305 should have correct properties")
    void poly1305ShouldHaveCorrectProperties() {
      assertEquals(
          "Poly1305", MacAlgorithm.POLY1305.getAlgorithmName(), "Algorithm name should match");
      assertEquals(128, MacAlgorithm.POLY1305.getOutputSize(), "Output size should be 128 bits");
    }

    @Test
    @DisplayName("BLAKE2B_MAC should have correct properties")
    void blake2bMacShouldHaveCorrectProperties() {
      assertEquals(
          "BLAKE2b-MAC",
          MacAlgorithm.BLAKE2B_MAC.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(512, MacAlgorithm.BLAKE2B_MAC.getOutputSize(), "Output size should be 512 bits");
    }

    @Test
    @DisplayName("BLAKE2S_MAC should have correct properties")
    void blake2sMacShouldHaveCorrectProperties() {
      assertEquals(
          "BLAKE2s-MAC",
          MacAlgorithm.BLAKE2S_MAC.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(256, MacAlgorithm.BLAKE2S_MAC.getOutputSize(), "Output size should be 256 bits");
    }

    @Test
    @DisplayName("AES_CMAC should have correct properties")
    void aesCmacShouldHaveCorrectProperties() {
      assertEquals(
          "AES-CMAC", MacAlgorithm.AES_CMAC.getAlgorithmName(), "Algorithm name should match");
      assertEquals(128, MacAlgorithm.AES_CMAC.getOutputSize(), "Output size should be 128 bits");
    }

    @Test
    @DisplayName("KMAC128 should have correct properties")
    void kmac128ShouldHaveCorrectProperties() {
      assertEquals(
          "KMAC128", MacAlgorithm.KMAC128.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, MacAlgorithm.KMAC128.getOutputSize(), "Output size should be 256 bits");
    }

    @Test
    @DisplayName("KMAC256 should have correct properties")
    void kmac256ShouldHaveCorrectProperties() {
      assertEquals(
          "KMAC256", MacAlgorithm.KMAC256.getAlgorithmName(), "Algorithm name should match");
      assertEquals(512, MacAlgorithm.KMAC256.getOutputSize(), "Output size should be 512 bits");
    }
  }
}
