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

/** Tests for {@link PaddingScheme} enum. */
@DisplayName("PaddingScheme Tests")
class PaddingSchemeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(
          PaddingScheme.class.isEnum(),
          "PaddingScheme should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 7 enum values")
    void shouldHaveExactlySevenEnumValues() {
      final PaddingScheme[] values = PaddingScheme.values();

      assertEquals(7, values.length, "PaddingScheme should have exactly 7 enum values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have NONE value")
    void shouldHaveNoneValue() {
      assertNotNull(PaddingScheme.NONE, "NONE should not be null");
    }

    @Test
    @DisplayName("should have PKCS1_V15 value")
    void shouldHavePkcs1V15Value() {
      assertNotNull(PaddingScheme.PKCS1_V15, "PKCS1_V15 should not be null");
    }

    @Test
    @DisplayName("should have OAEP_SHA1 value")
    void shouldHaveOaepSha1Value() {
      assertNotNull(PaddingScheme.OAEP_SHA1, "OAEP_SHA1 should not be null");
    }

    @Test
    @DisplayName("should have OAEP_SHA256 value")
    void shouldHaveOaepSha256Value() {
      assertNotNull(PaddingScheme.OAEP_SHA256, "OAEP_SHA256 should not be null");
    }

    @Test
    @DisplayName("should have OAEP_SHA384 value")
    void shouldHaveOaepSha384Value() {
      assertNotNull(PaddingScheme.OAEP_SHA384, "OAEP_SHA384 should not be null");
    }

    @Test
    @DisplayName("should have OAEP_SHA512 value")
    void shouldHaveOaepSha512Value() {
      assertNotNull(PaddingScheme.OAEP_SHA512, "OAEP_SHA512 should not be null");
    }

    @Test
    @DisplayName("should have PSS value")
    void shouldHavePssValue() {
      assertNotNull(PaddingScheme.PSS, "PSS should not be null");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have unique ordinals for all values")
    void shouldHaveUniqueOrdinalsForAllValues() {
      final Set<Integer> ordinals = new HashSet<>();
      for (final PaddingScheme scheme : PaddingScheme.values()) {
        ordinals.add(scheme.ordinal());
      }

      assertEquals(
          PaddingScheme.values().length,
          ordinals.size(),
          "All enum values should have unique ordinals");
    }

    @Test
    @DisplayName("should have sequential ordinals starting at 0")
    void shouldHaveSequentialOrdinalsStartingAtZero() {
      final PaddingScheme[] values = PaddingScheme.values();

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
      for (final PaddingScheme scheme : PaddingScheme.values()) {
        assertEquals(
            scheme,
            PaddingScheme.valueOf(scheme.name()),
            "valueOf should round-trip for " + scheme.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowIllegalArgumentExceptionForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> PaddingScheme.valueOf("INVALID_SCHEME"),
          "valueOf should throw IllegalArgumentException for invalid name");
    }
  }

  @Nested
  @DisplayName("Values Tests")
  class ValuesTests {

    @Test
    @DisplayName("should return new array on each call to values()")
    void shouldReturnNewArrayOnEachCallToValues() {
      final PaddingScheme[] first = PaddingScheme.values();
      final PaddingScheme[] second = PaddingScheme.values();

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
      for (final PaddingScheme scheme : PaddingScheme.values()) {
        assertEquals(
            scheme.name(),
            scheme.toString(),
            "toString should return name for " + scheme.name());
      }
    }
  }

  @Nested
  @DisplayName("GetSchemeName Tests")
  class GetSchemeNameTests {

    @Test
    @DisplayName("should return None for NONE")
    void shouldReturnCorrectNameForNone() {
      assertEquals(
          "None",
          PaddingScheme.NONE.getSchemeName(),
          "NONE should have scheme name None");
    }

    @Test
    @DisplayName("should return PKCS1-v1.5 for PKCS1_V15")
    void shouldReturnCorrectNameForPkcs1V15() {
      assertEquals(
          "PKCS1-v1.5",
          PaddingScheme.PKCS1_V15.getSchemeName(),
          "PKCS1_V15 should have scheme name PKCS1-v1.5");
    }

    @Test
    @DisplayName("should return OAEP-SHA1 for OAEP_SHA1")
    void shouldReturnCorrectNameForOaepSha1() {
      assertEquals(
          "OAEP-SHA1",
          PaddingScheme.OAEP_SHA1.getSchemeName(),
          "OAEP_SHA1 should have scheme name OAEP-SHA1");
    }

    @Test
    @DisplayName("should return OAEP-SHA256 for OAEP_SHA256")
    void shouldReturnCorrectNameForOaepSha256() {
      assertEquals(
          "OAEP-SHA256",
          PaddingScheme.OAEP_SHA256.getSchemeName(),
          "OAEP_SHA256 should have scheme name OAEP-SHA256");
    }

    @Test
    @DisplayName("should return OAEP-SHA384 for OAEP_SHA384")
    void shouldReturnCorrectNameForOaepSha384() {
      assertEquals(
          "OAEP-SHA384",
          PaddingScheme.OAEP_SHA384.getSchemeName(),
          "OAEP_SHA384 should have scheme name OAEP-SHA384");
    }

    @Test
    @DisplayName("should return OAEP-SHA512 for OAEP_SHA512")
    void shouldReturnCorrectNameForOaepSha512() {
      assertEquals(
          "OAEP-SHA512",
          PaddingScheme.OAEP_SHA512.getSchemeName(),
          "OAEP_SHA512 should have scheme name OAEP-SHA512");
    }

    @Test
    @DisplayName("should return PSS for PSS")
    void shouldReturnCorrectNameForPss() {
      assertEquals(
          "PSS",
          PaddingScheme.PSS.getSchemeName(),
          "PSS should have scheme name PSS");
    }
  }
}
