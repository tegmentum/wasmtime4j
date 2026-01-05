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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ExternType enum.
 *
 * <p>ExternType represents the different kinds of external values that can be imported or exported
 * in WebAssembly modules. This test verifies the enum structure, values, and conversion methods.
 */
@DisplayName("ExternType Enum Tests")
class ExternTypeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(ExternType.class.isEnum(), "ExternType should be an enum");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      assertEquals(4, ExternType.values().length, "ExternType should have exactly 4 values");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      Set<String> expectedValues = Set.of("FUNC", "TABLE", "MEMORY", "GLOBAL");
      Set<String> actualValues =
          Arrays.stream(ExternType.values()).map(Enum::name).collect(Collectors.toSet());
      assertEquals(expectedValues, actualValues, "ExternType should have all expected values");
    }
  }

  // ========================================================================
  // Enum Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("FUNC should exist")
    void funcShouldExist() {
      ExternType value = ExternType.FUNC;
      assertNotNull(value, "FUNC should exist");
      assertEquals("FUNC", value.name(), "FUNC should have correct name");
    }

    @Test
    @DisplayName("TABLE should exist")
    void tableShouldExist() {
      ExternType value = ExternType.TABLE;
      assertNotNull(value, "TABLE should exist");
      assertEquals("TABLE", value.name(), "TABLE should have correct name");
    }

    @Test
    @DisplayName("MEMORY should exist")
    void memoryShouldExist() {
      ExternType value = ExternType.MEMORY;
      assertNotNull(value, "MEMORY should exist");
      assertEquals("MEMORY", value.name(), "MEMORY should have correct name");
    }

    @Test
    @DisplayName("GLOBAL should exist")
    void globalShouldExist() {
      ExternType value = ExternType.GLOBAL;
      assertNotNull(value, "GLOBAL should exist");
      assertEquals("GLOBAL", value.name(), "GLOBAL should have correct name");
    }
  }

  // ========================================================================
  // getCode Method Tests
  // ========================================================================

  @Nested
  @DisplayName("getCode Method Tests")
  class GetCodeMethodTests {

    @Test
    @DisplayName("FUNC should have code 0")
    void funcShouldHaveCodeZero() {
      assertEquals(0, ExternType.FUNC.getCode(), "FUNC should have code 0");
    }

    @Test
    @DisplayName("TABLE should have code 1")
    void tableShouldHaveCodeOne() {
      assertEquals(1, ExternType.TABLE.getCode(), "TABLE should have code 1");
    }

    @Test
    @DisplayName("MEMORY should have code 2")
    void memoryShouldHaveCodeTwo() {
      assertEquals(2, ExternType.MEMORY.getCode(), "MEMORY should have code 2");
    }

    @Test
    @DisplayName("GLOBAL should have code 3")
    void globalShouldHaveCodeThree() {
      assertEquals(3, ExternType.GLOBAL.getCode(), "GLOBAL should have code 3");
    }

    @Test
    @DisplayName("all values should have unique codes")
    void allValuesShouldHaveUniqueCodes() {
      int[] codes = Arrays.stream(ExternType.values()).mapToInt(ExternType::getCode).toArray();
      Set<Integer> uniqueCodes = Arrays.stream(codes).boxed().collect(Collectors.toSet());
      assertEquals(
          codes.length, uniqueCodes.size(), "All ExternType values should have unique codes");
    }

    @Test
    @DisplayName("codes should be consecutive starting from 0")
    void codesShouldBeConsecutiveStartingFromZero() {
      int[] codes =
          Arrays.stream(ExternType.values()).mapToInt(ExternType::getCode).sorted().toArray();
      int[] expected = {0, 1, 2, 3};
      assertArrayEquals(expected, codes, "Codes should be consecutive starting from 0");
    }
  }

  // ========================================================================
  // fromCode Method Tests
  // ========================================================================

  @Nested
  @DisplayName("fromCode Method Tests")
  class FromCodeMethodTests {

    @Test
    @DisplayName("fromCode(0) should return FUNC")
    void fromCodeZeroShouldReturnFunc() {
      assertEquals(ExternType.FUNC, ExternType.fromCode(0), "fromCode(0) should return FUNC");
    }

    @Test
    @DisplayName("fromCode(1) should return TABLE")
    void fromCodeOneShouldReturnTable() {
      assertEquals(ExternType.TABLE, ExternType.fromCode(1), "fromCode(1) should return TABLE");
    }

    @Test
    @DisplayName("fromCode(2) should return MEMORY")
    void fromCodeTwoShouldReturnMemory() {
      assertEquals(ExternType.MEMORY, ExternType.fromCode(2), "fromCode(2) should return MEMORY");
    }

    @Test
    @DisplayName("fromCode(3) should return GLOBAL")
    void fromCodeThreeShouldReturnGlobal() {
      assertEquals(ExternType.GLOBAL, ExternType.fromCode(3), "fromCode(3) should return GLOBAL");
    }

    @Test
    @DisplayName("fromCode should throw IllegalArgumentException for invalid code")
    void fromCodeShouldThrowForInvalidCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(4),
          "fromCode(4) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromCode should throw IllegalArgumentException for negative code")
    void fromCodeShouldThrowForNegativeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(-1),
          "fromCode(-1) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromCode should throw IllegalArgumentException for large code")
    void fromCodeShouldThrowForLargeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(100),
          "fromCode(100) should throw IllegalArgumentException");
    }
  }

  // ========================================================================
  // Round-Trip Tests
  // ========================================================================

  @Nested
  @DisplayName("Round-Trip Conversion Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getCode and fromCode should be inverses for all values")
    void getCodeAndFromCodeShouldBeInverses() {
      for (ExternType type : ExternType.values()) {
        int code = type.getCode();
        ExternType roundTrip = ExternType.fromCode(code);
        assertEquals(
            type, roundTrip, "Round-trip conversion should preserve value for " + type.name());
      }
    }
  }

  // ========================================================================
  // Ordinal Tests
  // ========================================================================

  @Nested
  @DisplayName("Ordinal Tests")
  class OrdinalTests {

    @Test
    @DisplayName("ordinal values should match expected order")
    void ordinalValuesShouldMatchExpectedOrder() {
      assertEquals(0, ExternType.FUNC.ordinal(), "FUNC ordinal should be 0");
      assertEquals(1, ExternType.TABLE.ordinal(), "TABLE ordinal should be 1");
      assertEquals(2, ExternType.MEMORY.ordinal(), "MEMORY ordinal should be 2");
      assertEquals(3, ExternType.GLOBAL.ordinal(), "GLOBAL ordinal should be 3");
    }

    @Test
    @DisplayName("getCode should match ordinal for all values")
    void getCodeShouldMatchOrdinal() {
      for (ExternType type : ExternType.values()) {
        assertEquals(
            type.ordinal(), type.getCode(), "getCode should match ordinal for " + type.name());
      }
    }
  }

  // ========================================================================
  // Enum Standard Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Standard Method Tests")
  class EnumStandardMethodTests {

    @Test
    @DisplayName("valueOf should work for all values")
    void valueOfShouldWorkForAllValues() {
      assertEquals(ExternType.FUNC, ExternType.valueOf("FUNC"), "valueOf should work for FUNC");
      assertEquals(ExternType.TABLE, ExternType.valueOf("TABLE"), "valueOf should work for TABLE");
      assertEquals(
          ExternType.MEMORY, ExternType.valueOf("MEMORY"), "valueOf should work for MEMORY");
      assertEquals(
          ExternType.GLOBAL, ExternType.valueOf("GLOBAL"), "valueOf should work for GLOBAL");
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for unknown value")
    void valueOfShouldThrowForUnknownValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.valueOf("UNKNOWN"),
          "valueOf should throw for unknown value");
    }

    @Test
    @DisplayName("toString should return name")
    void toStringShouldReturnName() {
      for (ExternType type : ExternType.values()) {
        assertEquals(
            type.name(), type.toString(), "toString should return name for " + type.name());
      }
    }
  }

  // ========================================================================
  // WebAssembly Semantics Tests
  // ========================================================================

  @Nested
  @DisplayName("WebAssembly Semantics Tests")
  class WebAssemblySemanticsTests {

    @Test
    @DisplayName("FUNC represents function external type")
    void funcRepresentsFunctionExternalType() {
      // FUNC corresponds to functions that can be imported/exported
      ExternType funcType = ExternType.FUNC;
      assertEquals(0, funcType.getCode(), "FUNC should have WebAssembly external kind code 0");
    }

    @Test
    @DisplayName("TABLE represents table external type")
    void tableRepresentsTableExternalType() {
      // TABLE corresponds to tables (typically funcref tables)
      ExternType tableType = ExternType.TABLE;
      assertEquals(1, tableType.getCode(), "TABLE should have WebAssembly external kind code 1");
    }

    @Test
    @DisplayName("MEMORY represents memory external type")
    void memoryRepresentsMemoryExternalType() {
      // MEMORY corresponds to linear memory
      ExternType memoryType = ExternType.MEMORY;
      assertEquals(2, memoryType.getCode(), "MEMORY should have WebAssembly external kind code 2");
    }

    @Test
    @DisplayName("GLOBAL represents global external type")
    void globalRepresentsGlobalExternalType() {
      // GLOBAL corresponds to global variables
      ExternType globalType = ExternType.GLOBAL;
      assertEquals(3, globalType.getCode(), "GLOBAL should have WebAssembly external kind code 3");
    }
  }
}
