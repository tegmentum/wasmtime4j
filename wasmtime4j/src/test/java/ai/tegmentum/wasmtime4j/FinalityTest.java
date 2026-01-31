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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Finality}.
 *
 * <p>Verifies enum structure, constants, getWasmKeyword, allowsSubtypes, isFinal,
 * fromWasmKeyword, and toString.
 */
@DisplayName("Finality Tests")
class FinalityTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(Finality.class.isEnum(), "Finality should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 2 values")
    void shouldHaveExactValueCount() {
      assertEquals(2, Finality.values().length,
          "Finality should have exactly 2 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain FINAL")
    void shouldContainFinal() {
      assertNotNull(Finality.FINAL, "FINAL constant should exist");
    }

    @Test
    @DisplayName("should contain NON_FINAL")
    void shouldContainNonFinal() {
      assertNotNull(Finality.NON_FINAL, "NON_FINAL constant should exist");
    }
  }

  @Nested
  @DisplayName("GetWasmKeyword Tests")
  class GetWasmKeywordTests {

    @Test
    @DisplayName("FINAL should have wasm keyword 'final'")
    void finalShouldHaveKeywordFinal() {
      assertEquals("final", Finality.FINAL.getWasmKeyword(),
          "FINAL should have wasm keyword 'final'");
    }

    @Test
    @DisplayName("NON_FINAL should have wasm keyword 'sub'")
    void nonFinalShouldHaveKeywordSub() {
      assertEquals("sub", Finality.NON_FINAL.getWasmKeyword(),
          "NON_FINAL should have wasm keyword 'sub'");
    }
  }

  @Nested
  @DisplayName("AllowsSubtypes Tests")
  class AllowsSubtypesTests {

    @Test
    @DisplayName("FINAL should not allow subtypes")
    void finalShouldNotAllowSubtypes() {
      assertFalse(Finality.FINAL.allowsSubtypes(),
          "FINAL should not allow subtypes");
    }

    @Test
    @DisplayName("NON_FINAL should allow subtypes")
    void nonFinalShouldAllowSubtypes() {
      assertTrue(Finality.NON_FINAL.allowsSubtypes(),
          "NON_FINAL should allow subtypes");
    }
  }

  @Nested
  @DisplayName("IsFinal Tests")
  class IsFinalTests {

    @Test
    @DisplayName("FINAL should return true for isFinal")
    void finalShouldReturnTrueForIsFinal() {
      assertTrue(Finality.FINAL.isFinal(),
          "FINAL.isFinal() should return true");
    }

    @Test
    @DisplayName("NON_FINAL should return false for isFinal")
    void nonFinalShouldReturnFalseForIsFinal() {
      assertFalse(Finality.NON_FINAL.isFinal(),
          "NON_FINAL.isFinal() should return false");
    }
  }

  @Nested
  @DisplayName("FromWasmKeyword Tests")
  class FromWasmKeywordTests {

    @Test
    @DisplayName("should return FINAL for keyword 'final'")
    void shouldReturnFinalForKeywordFinal() {
      assertEquals(Finality.FINAL, Finality.fromWasmKeyword("final"),
          "fromWasmKeyword('final') should return FINAL");
    }

    @Test
    @DisplayName("should return NON_FINAL for keyword 'sub'")
    void shouldReturnNonFinalForKeywordSub() {
      assertEquals(Finality.NON_FINAL, Finality.fromWasmKeyword("sub"),
          "fromWasmKeyword('sub') should return NON_FINAL");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown keyword")
    void shouldThrowForUnknownKeyword() {
      assertThrows(IllegalArgumentException.class,
          () -> Finality.fromWasmKeyword("unknown"),
          "fromWasmKeyword('unknown') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null keyword")
    void shouldThrowForNullKeyword() {
      assertThrows(IllegalArgumentException.class,
          () -> Finality.fromWasmKeyword(null),
          "fromWasmKeyword(null) should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("FINAL toString should return wasm keyword")
    void finalToStringShouldReturnKeyword() {
      assertEquals("final", Finality.FINAL.toString(),
          "FINAL.toString() should return 'final'");
    }

    @Test
    @DisplayName("NON_FINAL toString should return wasm keyword")
    void nonFinalToStringShouldReturnKeyword() {
      assertEquals("sub", Finality.NON_FINAL.toString(),
          "NON_FINAL.toString() should return 'sub'");
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final Finality value : Finality.values()) {
        assertEquals(value, Finality.valueOf(value.name()),
            "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(IllegalArgumentException.class,
          () -> Finality.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("isFinal and allowsSubtypes should be mutually exclusive")
    void isFinalAndAllowsSubtypesShouldBeMutuallyExclusive() {
      for (final Finality value : Finality.values()) {
        assertTrue(value.isFinal() != value.allowsSubtypes(),
            "isFinal and allowsSubtypes should be mutually exclusive for "
                + value.name());
      }
    }
  }
}
