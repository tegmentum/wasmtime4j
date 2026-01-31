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

/** Tests for {@link CryptoKeyType} enum. */
@DisplayName("CryptoKeyType Tests")
class CryptoKeyTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          CryptoKeyType.class.isEnum(),
          "CryptoKeyType should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 4 enum values")
    void shouldHaveExactlyFourEnumValues() {
      final CryptoKeyType[] values = CryptoKeyType.values();

      assertEquals(4, values.length, "CryptoKeyType should have exactly 4 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have SYMMETRIC value")
    void shouldHaveSymmetricValue() {
      assertNotNull(CryptoKeyType.SYMMETRIC, "SYMMETRIC should not be null");
    }

    @Test
    @DisplayName("should have PUBLIC value")
    void shouldHavePublicValue() {
      assertNotNull(CryptoKeyType.PUBLIC, "PUBLIC should not be null");
    }

    @Test
    @DisplayName("should have PRIVATE value")
    void shouldHavePrivateValue() {
      assertNotNull(CryptoKeyType.PRIVATE, "PRIVATE should not be null");
    }

    @Test
    @DisplayName("should have KEY_PAIR value")
    void shouldHaveKeyPairValue() {
      assertNotNull(CryptoKeyType.KEY_PAIR, "KEY_PAIR should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final CryptoKeyType keyType : CryptoKeyType.values()) {
        ordinals.add(keyType.ordinal());
      }

      assertEquals(
          CryptoKeyType.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final CryptoKeyType[] values = CryptoKeyType.values();

      for (int i = 0; i < values.length; i++) {
        assertEquals(
            i,
            values[i].ordinal(),
            "Ordinal should be " + i + " for " + values[i]);
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should round-trip all values through valueOf")
    void shouldRoundTripAllValuesThroughValueOf() {
      for (final CryptoKeyType keyType : CryptoKeyType.values()) {
        assertEquals(
            keyType,
            CryptoKeyType.valueOf(keyType.name()),
            "valueOf should round-trip for " + keyType.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> CryptoKeyType.valueOf("INVALID_TYPE"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final CryptoKeyType[] first = CryptoKeyType.values();
      final CryptoKeyType[] second = CryptoKeyType.values();

      assertNotSame(first, second, "values() should return a new array on each call");
      assertArrayEquals(
          first,
          second,
          "values() arrays should contain the same elements");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return name from toString for all values")
    void shouldReturnNameFromToStringForAllValues() {
      for (final CryptoKeyType keyType : CryptoKeyType.values()) {
        assertEquals(
            keyType.name(),
            keyType.toString(),
            "toString should return name for " + keyType.name());
      }
    }
  }
}
