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
 * Tests for {@link SymmetricAlgorithm} enum.
 *
 * <p>SymmetricAlgorithm defines symmetric encryption algorithms supported by WASI-crypto.
 */
@DisplayName("SymmetricAlgorithm Tests")
class SymmetricAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(SymmetricAlgorithm.class.isEnum(), "SymmetricAlgorithm should be an enum");
    }

    @Test
    @DisplayName("should have AES_128 constant")
    void shouldHaveAes128Constant() {
      assertNotNull(SymmetricAlgorithm.AES_128, "AES_128 constant should exist");
    }

    @Test
    @DisplayName("should have AES_192 constant")
    void shouldHaveAes192Constant() {
      assertNotNull(SymmetricAlgorithm.AES_192, "AES_192 constant should exist");
    }

    @Test
    @DisplayName("should have AES_256 constant")
    void shouldHaveAes256Constant() {
      assertNotNull(SymmetricAlgorithm.AES_256, "AES_256 constant should exist");
    }

    @Test
    @DisplayName("should have CHACHA20 constant")
    void shouldHaveChaCha20Constant() {
      assertNotNull(SymmetricAlgorithm.CHACHA20, "CHACHA20 constant should exist");
    }

    @Test
    @DisplayName("should have XCHACHA20 constant")
    void shouldHaveXChaCha20Constant() {
      assertNotNull(SymmetricAlgorithm.XCHACHA20, "XCHACHA20 constant should exist");
    }

    @Test
    @DisplayName("should have CHACHA20_POLY1305 constant")
    void shouldHaveChaCha20Poly1305Constant() {
      assertNotNull(
          SymmetricAlgorithm.CHACHA20_POLY1305, "CHACHA20_POLY1305 constant should exist");
    }

    @Test
    @DisplayName("should have XCHACHA20_POLY1305 constant")
    void shouldHaveXChaCha20Poly1305Constant() {
      assertNotNull(
          SymmetricAlgorithm.XCHACHA20_POLY1305, "XCHACHA20_POLY1305 constant should exist");
    }

    @Test
    @DisplayName("should have 7 algorithm types")
    void shouldHave7AlgorithmTypes() {
      assertEquals(7, SymmetricAlgorithm.values().length, "Should have 7 algorithm types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() throws NoSuchMethodException {
      final Method method = SymmetricAlgorithm.class.getMethod("getAlgorithmName");
      assertNotNull(method, "getAlgorithmName method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithmName should return String");
    }

    @Test
    @DisplayName("should have getKeySize method")
    void shouldHaveGetKeySizeMethod() throws NoSuchMethodException {
      final Method method = SymmetricAlgorithm.class.getMethod("getKeySize");
      assertNotNull(method, "getKeySize method should exist");
      assertEquals(int.class, method.getReturnType(), "getKeySize should return int");
    }
  }

  @Nested
  @DisplayName("Algorithm Properties Tests")
  class AlgorithmPropertiesTests {

    @Test
    @DisplayName("AES_128 should have correct properties")
    void aes128ShouldHaveCorrectProperties() {
      assertEquals(
          "AES-128", SymmetricAlgorithm.AES_128.getAlgorithmName(), "Algorithm name should match");
      assertEquals(128, SymmetricAlgorithm.AES_128.getKeySize(), "Key size should be 128 bits");
    }

    @Test
    @DisplayName("AES_192 should have correct properties")
    void aes192ShouldHaveCorrectProperties() {
      assertEquals(
          "AES-192", SymmetricAlgorithm.AES_192.getAlgorithmName(), "Algorithm name should match");
      assertEquals(192, SymmetricAlgorithm.AES_192.getKeySize(), "Key size should be 192 bits");
    }

    @Test
    @DisplayName("AES_256 should have correct properties")
    void aes256ShouldHaveCorrectProperties() {
      assertEquals(
          "AES-256", SymmetricAlgorithm.AES_256.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, SymmetricAlgorithm.AES_256.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("CHACHA20 should have correct properties")
    void chacha20ShouldHaveCorrectProperties() {
      assertEquals(
          "ChaCha20",
          SymmetricAlgorithm.CHACHA20.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(256, SymmetricAlgorithm.CHACHA20.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("XCHACHA20 should have correct properties")
    void xchacha20ShouldHaveCorrectProperties() {
      assertEquals(
          "XChaCha20",
          SymmetricAlgorithm.XCHACHA20.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(256, SymmetricAlgorithm.XCHACHA20.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("CHACHA20_POLY1305 should have correct properties")
    void chacha20Poly1305ShouldHaveCorrectProperties() {
      assertEquals(
          "ChaCha20-Poly1305",
          SymmetricAlgorithm.CHACHA20_POLY1305.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          256, SymmetricAlgorithm.CHACHA20_POLY1305.getKeySize(), "Key size should be 256 bits");
    }

    @Test
    @DisplayName("XCHACHA20_POLY1305 should have correct properties")
    void xchacha20Poly1305ShouldHaveCorrectProperties() {
      assertEquals(
          "XChaCha20-Poly1305",
          SymmetricAlgorithm.XCHACHA20_POLY1305.getAlgorithmName(),
          "Algorithm name should match");
      assertEquals(
          256, SymmetricAlgorithm.XCHACHA20_POLY1305.getKeySize(), "Key size should be 256 bits");
    }
  }
}
