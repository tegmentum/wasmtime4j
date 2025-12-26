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
 * Tests for {@link CryptoKeyType} enum.
 *
 * <p>CryptoKeyType defines the types of cryptographic keys in WASI-crypto.
 */
@DisplayName("CryptoKeyType Tests")
class CryptoKeyTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeEnum() {
      assertTrue(CryptoKeyType.class.isEnum(), "CryptoKeyType should be an enum");
    }

    @Test
    @DisplayName("should have SYMMETRIC constant")
    void shouldHaveSymmetricConstant() {
      assertNotNull(CryptoKeyType.SYMMETRIC, "SYMMETRIC constant should exist");
    }

    @Test
    @DisplayName("should have PUBLIC constant")
    void shouldHavePublicConstant() {
      assertNotNull(CryptoKeyType.PUBLIC, "PUBLIC constant should exist");
    }

    @Test
    @DisplayName("should have PRIVATE constant")
    void shouldHavePrivateConstant() {
      assertNotNull(CryptoKeyType.PRIVATE, "PRIVATE constant should exist");
    }

    @Test
    @DisplayName("should have KEY_PAIR constant")
    void shouldHaveKeyPairConstant() {
      assertNotNull(CryptoKeyType.KEY_PAIR, "KEY_PAIR constant should exist");
    }

    @Test
    @DisplayName("should have 4 key types")
    void shouldHave4KeyTypes() {
      assertEquals(4, CryptoKeyType.values().length, "Should have 4 key types");
    }
  }

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("SYMMETRIC should be retrievable by name")
    void symmetricShouldBeRetrievableByName() {
      assertEquals(CryptoKeyType.SYMMETRIC, CryptoKeyType.valueOf("SYMMETRIC"));
    }

    @Test
    @DisplayName("PUBLIC should be retrievable by name")
    void publicShouldBeRetrievableByName() {
      assertEquals(CryptoKeyType.PUBLIC, CryptoKeyType.valueOf("PUBLIC"));
    }

    @Test
    @DisplayName("PRIVATE should be retrievable by name")
    void privateShouldBeRetrievableByName() {
      assertEquals(CryptoKeyType.PRIVATE, CryptoKeyType.valueOf("PRIVATE"));
    }

    @Test
    @DisplayName("KEY_PAIR should be retrievable by name")
    void keyPairShouldBeRetrievableByName() {
      assertEquals(CryptoKeyType.KEY_PAIR, CryptoKeyType.valueOf("KEY_PAIR"));
    }
  }

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("SYMMETRIC should have ordinal 0")
    void symmetricShouldHaveOrdinal0() {
      assertEquals(0, CryptoKeyType.SYMMETRIC.ordinal(), "SYMMETRIC should have ordinal 0");
    }

    @Test
    @DisplayName("PUBLIC should have ordinal 1")
    void publicShouldHaveOrdinal1() {
      assertEquals(1, CryptoKeyType.PUBLIC.ordinal(), "PUBLIC should have ordinal 1");
    }

    @Test
    @DisplayName("PRIVATE should have ordinal 2")
    void privateShouldHaveOrdinal2() {
      assertEquals(2, CryptoKeyType.PRIVATE.ordinal(), "PRIVATE should have ordinal 2");
    }

    @Test
    @DisplayName("KEY_PAIR should have ordinal 3")
    void keyPairShouldHaveOrdinal3() {
      assertEquals(3, CryptoKeyType.KEY_PAIR.ordinal(), "KEY_PAIR should have ordinal 3");
    }
  }
}
