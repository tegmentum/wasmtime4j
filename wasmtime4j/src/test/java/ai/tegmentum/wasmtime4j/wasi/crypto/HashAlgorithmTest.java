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
 * Tests for {@link HashAlgorithm} enum.
 *
 * <p>HashAlgorithm defines cryptographic hash algorithms supported by WASI-crypto.
 */
@DisplayName("HashAlgorithm Tests")
class HashAlgorithmTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(HashAlgorithm.class.isEnum(), "HashAlgorithm should be an enum");
    }

    @Test
    @DisplayName("should have SHA_256 constant")
    void shouldHaveSha256Constant() {
      assertNotNull(HashAlgorithm.SHA_256, "SHA_256 constant should exist");
    }

    @Test
    @DisplayName("should have SHA_384 constant")
    void shouldHaveSha384Constant() {
      assertNotNull(HashAlgorithm.SHA_384, "SHA_384 constant should exist");
    }

    @Test
    @DisplayName("should have SHA_512 constant")
    void shouldHaveSha512Constant() {
      assertNotNull(HashAlgorithm.SHA_512, "SHA_512 constant should exist");
    }

    @Test
    @DisplayName("should have SHA3_256 constant")
    void shouldHaveSha3256Constant() {
      assertNotNull(HashAlgorithm.SHA3_256, "SHA3_256 constant should exist");
    }

    @Test
    @DisplayName("should have BLAKE2B constant")
    void shouldHaveBlake2bConstant() {
      assertNotNull(HashAlgorithm.BLAKE2B, "BLAKE2B constant should exist");
    }

    @Test
    @DisplayName("should have BLAKE3 constant")
    void shouldHaveBlake3Constant() {
      assertNotNull(HashAlgorithm.BLAKE3, "BLAKE3 constant should exist");
    }

    @Test
    @DisplayName("should have 10 hash algorithm types")
    void shouldHave10HashAlgorithmTypes() {
      assertEquals(10, HashAlgorithm.values().length, "Should have 10 hash algorithm types");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getAlgorithmName method")
    void shouldHaveGetAlgorithmNameMethod() throws NoSuchMethodException {
      final Method method = HashAlgorithm.class.getMethod("getAlgorithmName");
      assertNotNull(method, "getAlgorithmName method should exist");
      assertEquals(String.class, method.getReturnType(), "getAlgorithmName should return String");
    }

    @Test
    @DisplayName("should have getOutputSize method")
    void shouldHaveGetOutputSizeMethod() throws NoSuchMethodException {
      final Method method = HashAlgorithm.class.getMethod("getOutputSize");
      assertNotNull(method, "getOutputSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getOutputSize should return int");
    }
  }

  @Nested
  @DisplayName("Algorithm Properties Tests")
  class AlgorithmPropertiesTests {

    @Test
    @DisplayName("SHA_256 should have correct properties")
    void sha256ShouldHaveCorrectProperties() {
      assertEquals(
          "SHA-256", HashAlgorithm.SHA_256.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, HashAlgorithm.SHA_256.getOutputSize(), "Output size should be 256 bits");
    }

    @Test
    @DisplayName("SHA_384 should have correct properties")
    void sha384ShouldHaveCorrectProperties() {
      assertEquals(
          "SHA-384", HashAlgorithm.SHA_384.getAlgorithmName(), "Algorithm name should match");
      assertEquals(384, HashAlgorithm.SHA_384.getOutputSize(), "Output size should be 384 bits");
    }

    @Test
    @DisplayName("SHA_512 should have correct properties")
    void sha512ShouldHaveCorrectProperties() {
      assertEquals(
          "SHA-512", HashAlgorithm.SHA_512.getAlgorithmName(), "Algorithm name should match");
      assertEquals(512, HashAlgorithm.SHA_512.getOutputSize(), "Output size should be 512 bits");
    }

    @Test
    @DisplayName("SHA3_256 should have correct properties")
    void sha3256ShouldHaveCorrectProperties() {
      assertEquals(
          "SHA3-256", HashAlgorithm.SHA3_256.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, HashAlgorithm.SHA3_256.getOutputSize(), "Output size should be 256 bits");
    }

    @Test
    @DisplayName("BLAKE2B should have correct properties")
    void blake2bShouldHaveCorrectProperties() {
      assertEquals(
          "BLAKE2b", HashAlgorithm.BLAKE2B.getAlgorithmName(), "Algorithm name should match");
      assertEquals(512, HashAlgorithm.BLAKE2B.getOutputSize(), "Output size should be 512 bits");
    }

    @Test
    @DisplayName("BLAKE3 should have correct properties")
    void blake3ShouldHaveCorrectProperties() {
      assertEquals(
          "BLAKE3", HashAlgorithm.BLAKE3.getAlgorithmName(), "Algorithm name should match");
      assertEquals(256, HashAlgorithm.BLAKE3.getOutputSize(), "Output size should be 256 bits");
    }
  }
}
