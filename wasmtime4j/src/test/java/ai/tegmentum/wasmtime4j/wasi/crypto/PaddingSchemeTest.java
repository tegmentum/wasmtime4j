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
 * Tests for {@link PaddingScheme} enum.
 *
 * <p>PaddingScheme defines padding schemes for asymmetric encryption in WASI-crypto.
 */
@DisplayName("PaddingScheme Tests")
class PaddingSchemeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(PaddingScheme.class.isEnum(), "PaddingScheme should be an enum");
    }

    @Test
    @DisplayName("should have NONE constant")
    void shouldHaveNoneConstant() {
      assertNotNull(PaddingScheme.NONE, "NONE constant should exist");
    }

    @Test
    @DisplayName("should have PKCS1_V15 constant")
    void shouldHavePkcs1V15Constant() {
      assertNotNull(PaddingScheme.PKCS1_V15, "PKCS1_V15 constant should exist");
    }

    @Test
    @DisplayName("should have OAEP_SHA1 constant")
    void shouldHaveOaepSha1Constant() {
      assertNotNull(PaddingScheme.OAEP_SHA1, "OAEP_SHA1 constant should exist");
    }

    @Test
    @DisplayName("should have OAEP_SHA256 constant")
    void shouldHaveOaepSha256Constant() {
      assertNotNull(PaddingScheme.OAEP_SHA256, "OAEP_SHA256 constant should exist");
    }

    @Test
    @DisplayName("should have OAEP_SHA384 constant")
    void shouldHaveOaepSha384Constant() {
      assertNotNull(PaddingScheme.OAEP_SHA384, "OAEP_SHA384 constant should exist");
    }

    @Test
    @DisplayName("should have OAEP_SHA512 constant")
    void shouldHaveOaepSha512Constant() {
      assertNotNull(PaddingScheme.OAEP_SHA512, "OAEP_SHA512 constant should exist");
    }

    @Test
    @DisplayName("should have PSS constant")
    void shouldHavePssConstant() {
      assertNotNull(PaddingScheme.PSS, "PSS constant should exist");
    }

    @Test
    @DisplayName("should have 7 padding schemes")
    void shouldHave7PaddingSchemes() {
      assertEquals(7, PaddingScheme.values().length, "Should have 7 padding schemes");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getSchemeName method")
    void shouldHaveGetSchemeNameMethod() throws NoSuchMethodException {
      final Method method = PaddingScheme.class.getMethod("getSchemeName");
      assertNotNull(method, "getSchemeName method should exist");
      assertEquals(String.class, method.getReturnType(), "getSchemeName should return String");
    }
  }

  @Nested
  @DisplayName("Scheme Properties Tests")
  class SchemePropertiesTests {

    @Test
    @DisplayName("NONE should have correct scheme name")
    void noneShouldHaveCorrectSchemeName() {
      assertEquals("None", PaddingScheme.NONE.getSchemeName(), "Scheme name should match");
    }

    @Test
    @DisplayName("PKCS1_V15 should have correct scheme name")
    void pkcs1V15ShouldHaveCorrectSchemeName() {
      assertEquals(
          "PKCS1-v1.5", PaddingScheme.PKCS1_V15.getSchemeName(), "Scheme name should match");
    }

    @Test
    @DisplayName("OAEP_SHA1 should have correct scheme name")
    void oaepSha1ShouldHaveCorrectSchemeName() {
      assertEquals(
          "OAEP-SHA1", PaddingScheme.OAEP_SHA1.getSchemeName(), "Scheme name should match");
    }

    @Test
    @DisplayName("OAEP_SHA256 should have correct scheme name")
    void oaepSha256ShouldHaveCorrectSchemeName() {
      assertEquals(
          "OAEP-SHA256", PaddingScheme.OAEP_SHA256.getSchemeName(), "Scheme name should match");
    }

    @Test
    @DisplayName("OAEP_SHA384 should have correct scheme name")
    void oaepSha384ShouldHaveCorrectSchemeName() {
      assertEquals(
          "OAEP-SHA384", PaddingScheme.OAEP_SHA384.getSchemeName(), "Scheme name should match");
    }

    @Test
    @DisplayName("OAEP_SHA512 should have correct scheme name")
    void oaepSha512ShouldHaveCorrectSchemeName() {
      assertEquals(
          "OAEP-SHA512", PaddingScheme.OAEP_SHA512.getSchemeName(), "Scheme name should match");
    }

    @Test
    @DisplayName("PSS should have correct scheme name")
    void pssShouldHaveCorrectSchemeName() {
      assertEquals("PSS", PaddingScheme.PSS.getSchemeName(), "Scheme name should match");
    }
  }
}
