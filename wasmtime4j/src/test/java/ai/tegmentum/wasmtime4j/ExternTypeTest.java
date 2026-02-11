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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ExternType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link ExternType} enum.
 *
 * <p>Verifies enum structure, constants, getCode/fromCode, ordinals, and round-trip conversion.
 */
@DisplayName("ExternType Tests")
class ExternTypeTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum type")
    void shouldBeAnEnumType() {
      assertTrue(ExternType.class.isEnum(), "ExternType should be an enum type");
    }

    @Test
    @DisplayName("should have exactly 4 values")
    void shouldHaveExactValueCount() {
      assertEquals(4, ExternType.values().length, "ExternType should have exactly 4 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should contain FUNC")
    void shouldContainFunc() {
      assertNotNull(ExternType.FUNC, "FUNC constant should exist");
      assertEquals("FUNC", ExternType.FUNC.name(), "FUNC name should match");
    }

    @Test
    @DisplayName("should contain TABLE")
    void shouldContainTable() {
      assertNotNull(ExternType.TABLE, "TABLE constant should exist");
      assertEquals("TABLE", ExternType.TABLE.name(), "TABLE name should match");
    }

    @Test
    @DisplayName("should contain MEMORY")
    void shouldContainMemory() {
      assertNotNull(ExternType.MEMORY, "MEMORY constant should exist");
      assertEquals("MEMORY", ExternType.MEMORY.name(), "MEMORY name should match");
    }

    @Test
    @DisplayName("should contain GLOBAL")
    void shouldContainGlobal() {
      assertNotNull(ExternType.GLOBAL, "GLOBAL constant should exist");
      assertEquals("GLOBAL", ExternType.GLOBAL.name(), "GLOBAL name should match");
    }
  }

  @Nested
  @DisplayName("GetCode Tests")
  class GetCodeTests {

    @Test
    @DisplayName("FUNC should have code 0")
    void funcShouldHaveCode0() {
      assertEquals(0, ExternType.FUNC.getCode(), "FUNC should have code 0");
    }

    @Test
    @DisplayName("TABLE should have code 1")
    void tableShouldHaveCode1() {
      assertEquals(1, ExternType.TABLE.getCode(), "TABLE should have code 1");
    }

    @Test
    @DisplayName("MEMORY should have code 2")
    void memoryShouldHaveCode2() {
      assertEquals(2, ExternType.MEMORY.getCode(), "MEMORY should have code 2");
    }

    @Test
    @DisplayName("GLOBAL should have code 3")
    void globalShouldHaveCode3() {
      assertEquals(3, ExternType.GLOBAL.getCode(), "GLOBAL should have code 3");
    }

    @Test
    @DisplayName("should have unique codes across all constants")
    void shouldHaveUniqueCodes() {
      final Set<Integer> codes = new HashSet<>();
      for (final ExternType type : ExternType.values()) {
        codes.add(type.getCode());
      }
      assertEquals(ExternType.values().length, codes.size(), "All codes should be unique");
    }
  }

  @Nested
  @DisplayName("FromCode Tests")
  class FromCodeTests {

    @Test
    @DisplayName("should return FUNC for code 0")
    void shouldReturnFuncForCode0() {
      assertEquals(ExternType.FUNC, ExternType.fromCode(0), "fromCode(0) should return FUNC");
    }

    @Test
    @DisplayName("should return TABLE for code 1")
    void shouldReturnTableForCode1() {
      assertEquals(ExternType.TABLE, ExternType.fromCode(1), "fromCode(1) should return TABLE");
    }

    @Test
    @DisplayName("should return MEMORY for code 2")
    void shouldReturnMemoryForCode2() {
      assertEquals(ExternType.MEMORY, ExternType.fromCode(2), "fromCode(2) should return MEMORY");
    }

    @Test
    @DisplayName("should return GLOBAL for code 3")
    void shouldReturnGlobalForCode3() {
      assertEquals(ExternType.GLOBAL, ExternType.fromCode(3), "fromCode(3) should return GLOBAL");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowForNegativeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(-1),
          "fromCode(-1) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for out-of-range code")
    void shouldThrowForOutOfRangeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.fromCode(99),
          "fromCode(99) should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("should round-trip getCode and fromCode for all constants")
    void shouldRoundTripGetCodeAndFromCode() {
      for (final ExternType type : ExternType.values()) {
        assertEquals(
            type,
            ExternType.fromCode(type.getCode()),
            "Round-trip should return original for " + type.name());
      }
    }
  }

  @Nested
  @DisplayName("ValueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("should resolve all constants via valueOf")
    void shouldResolveAllConstantsViaValueOf() {
      for (final ExternType value : ExternType.values()) {
        assertEquals(
            value, ExternType.valueOf(value.name()), "valueOf should return " + value.name());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid name")
    void shouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ExternType.valueOf("INVALID_CONSTANT"),
          "valueOf with invalid name should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("Enum Ordinal Tests")
  class EnumOrdinalTests {

    @Test
    @DisplayName("should have sequential ordinals starting from 0")
    void shouldHaveSequentialOrdinals() {
      final ExternType[] values = ExternType.values();
      for (int i = 0; i < values.length; i++) {
        assertEquals(i, values[i].ordinal(), "Ordinal of " + values[i].name() + " should be " + i);
      }
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should support native FFI code round-trip via int array")
    void shouldSupportNativeFfiCodeRoundTrip() {
      final int[] nativeCodes = new int[ExternType.values().length];
      for (int i = 0; i < ExternType.values().length; i++) {
        nativeCodes[i] = ExternType.values()[i].getCode();
      }
      for (int i = 0; i < nativeCodes.length; i++) {
        final ExternType resolved = ExternType.fromCode(nativeCodes[i]);
        assertEquals(
            ExternType.values()[i], resolved, "FFI round-trip should preserve enum at index " + i);
      }
    }

    @Test
    @DisplayName("all enum values should be usable in switch statement")
    void allEnumValuesShouldBeUsableInSwitchStatement() {
      for (final ExternType type : ExternType.values()) {
        final String result;
        switch (type) {
          case FUNC:
            result = "func";
            break;
          case TABLE:
            result = "table";
            break;
          case MEMORY:
            result = "memory";
            break;
          case GLOBAL:
            result = "global";
            break;
          default:
            result = "unknown";
            break;
        }
        assertTrue(!result.equals("unknown"), "Should match a case: " + type);
      }
    }
  }
}
