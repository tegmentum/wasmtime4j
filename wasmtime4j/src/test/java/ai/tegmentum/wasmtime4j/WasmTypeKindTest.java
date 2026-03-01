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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link WasmTypeKind} enum.
 *
 * <p>This test class verifies WasmTypeKind enum values and behavior.
 */
@DisplayName("WasmTypeKind Tests")
class WasmTypeKindTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("WasmTypeKind should be an enum")
    void shouldBeAnEnum() {
      assertTrue(WasmTypeKind.class.isEnum(), "WasmTypeKind should be an enum");
    }

    @Test
    @DisplayName("WasmTypeKind should have exactly 4 values")
    void shouldHaveExactlyFourValues() {
      assertEquals(5, WasmTypeKind.values().length, "Should have 5 values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("Should have FUNCTION value")
    void shouldHaveFunctionValue() {
      assertNotNull(WasmTypeKind.FUNCTION, "FUNCTION should exist");
      assertEquals("FUNCTION", WasmTypeKind.FUNCTION.name(), "Name should be FUNCTION");
    }

    @Test
    @DisplayName("Should have GLOBAL value")
    void shouldHaveGlobalValue() {
      assertNotNull(WasmTypeKind.GLOBAL, "GLOBAL should exist");
      assertEquals("GLOBAL", WasmTypeKind.GLOBAL.name(), "Name should be GLOBAL");
    }

    @Test
    @DisplayName("Should have MEMORY value")
    void shouldHaveMemoryValue() {
      assertNotNull(WasmTypeKind.MEMORY, "MEMORY should exist");
      assertEquals("MEMORY", WasmTypeKind.MEMORY.name(), "Name should be MEMORY");
    }

    @Test
    @DisplayName("Should have TABLE value")
    void shouldHaveTableValue() {
      assertNotNull(WasmTypeKind.TABLE, "TABLE should exist");
      assertEquals("TABLE", WasmTypeKind.TABLE.name(), "Name should be TABLE");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return FUNCTION for 'FUNCTION'")
    void valueOfShouldReturnFunction() {
      assertEquals(
          WasmTypeKind.FUNCTION, WasmTypeKind.valueOf("FUNCTION"), "Should return FUNCTION");
    }

    @Test
    @DisplayName("valueOf should return GLOBAL for 'GLOBAL'")
    void valueOfShouldReturnGlobal() {
      assertEquals(WasmTypeKind.GLOBAL, WasmTypeKind.valueOf("GLOBAL"), "Should return GLOBAL");
    }

    @Test
    @DisplayName("valueOf should return MEMORY for 'MEMORY'")
    void valueOfShouldReturnMemory() {
      assertEquals(WasmTypeKind.MEMORY, WasmTypeKind.valueOf("MEMORY"), "Should return MEMORY");
    }

    @Test
    @DisplayName("valueOf should return TABLE for 'TABLE'")
    void valueOfShouldReturnTable() {
      assertEquals(WasmTypeKind.TABLE, WasmTypeKind.valueOf("TABLE"), "Should return TABLE");
    }

    @Test
    @DisplayName("valueOf should throw for invalid value")
    void valueOfShouldThrowForInvalidValue() {
      try {
        WasmTypeKind.valueOf("INVALID");
        assertTrue(false, "Should throw IllegalArgumentException");
      } catch (IllegalArgumentException e) {
        // Expected
        assertNotNull(e.getMessage(), "Exception message should not be null");
      }
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final WasmTypeKind[] values = WasmTypeKind.values();
      final Set<WasmTypeKind> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(WasmTypeKind.FUNCTION), "Should contain FUNCTION");
      assertTrue(valueSet.contains(WasmTypeKind.GLOBAL), "Should contain GLOBAL");
      assertTrue(valueSet.contains(WasmTypeKind.MEMORY), "Should contain MEMORY");
      assertTrue(valueSet.contains(WasmTypeKind.TABLE), "Should contain TABLE");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final WasmTypeKind[] first = WasmTypeKind.values();
      final WasmTypeKind[] second = WasmTypeKind.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return enum name for FUNCTION")
    void toStringShouldReturnEnumNameForFunction() {
      assertEquals("FUNCTION", WasmTypeKind.FUNCTION.toString(), "Should return FUNCTION");
    }

    @Test
    @DisplayName("toString should return enum name for GLOBAL")
    void toStringShouldReturnEnumNameForGlobal() {
      assertEquals("GLOBAL", WasmTypeKind.GLOBAL.toString(), "Should return GLOBAL");
    }

    @Test
    @DisplayName("toString should return enum name for MEMORY")
    void toStringShouldReturnEnumNameForMemory() {
      assertEquals("MEMORY", WasmTypeKind.MEMORY.toString(), "Should return MEMORY");
    }

    @Test
    @DisplayName("toString should return enum name for TABLE")
    void toStringShouldReturnEnumNameForTable() {
      assertEquals("TABLE", WasmTypeKind.TABLE.toString(), "Should return TABLE");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("All enum values should be usable in switch statement")
    void allEnumValuesShouldBeUsableInSwitchStatement() {
      for (WasmTypeKind kind : WasmTypeKind.values()) {
        final String result;
        switch (kind) {
          case FUNCTION:
            result = "func";
            break;
          case GLOBAL:
            result = "global";
            break;
          case MEMORY:
            result = "memory";
            break;
          case TABLE:
            result = "table";
            break;
          case TAG:
            result = "tag";
            break;
          default:
            result = "unknown";
            break;
        }
        assertTrue(!result.equals("unknown"), "Should match a case: " + kind);
      }
    }

    @Test
    @DisplayName("Enum values should be comparable")
    void enumValuesShouldBeComparable() {
      assertTrue(
          WasmTypeKind.FUNCTION.compareTo(WasmTypeKind.GLOBAL) != 0,
          "Different enums should be different");
      assertEquals(
          0, WasmTypeKind.FUNCTION.compareTo(WasmTypeKind.FUNCTION), "Same enum should be equal");
    }
  }
}
