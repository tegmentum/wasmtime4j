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
package ai.tegmentum.wasmtime4j.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.Mutability;
import ai.tegmentum.wasmtime4j.type.TableType;
import java.util.OptionalLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ModuleValidationSupport} utility class.
 *
 * <p>Tests formatting methods and type matching methods using factory-created type objects.
 */
@DisplayName("ModuleValidationSupport Tests")
class ModuleValidationSupportTest {

  @Nested
  @DisplayName("formatGlobalType Tests")
  class FormatGlobalTypeTests {

    @Test
    @DisplayName("should format immutable i32 global")
    void shouldFormatImmutableI32() {
      final GlobalType type = GlobalType.of(WasmValueType.I32, Mutability.CONST);
      final String formatted = ModuleValidationSupport.formatGlobalType(type);

      assertTrue(formatted.contains("I32"), "Should contain value type I32, got: " + formatted);
      assertTrue(
          formatted.contains("immutable"),
          "Should contain 'immutable' for CONST, got: " + formatted);
    }

    @Test
    @DisplayName("should format mutable f64 global")
    void shouldFormatMutableF64() {
      final GlobalType type = GlobalType.of(WasmValueType.F64, Mutability.VAR);
      final String formatted = ModuleValidationSupport.formatGlobalType(type);

      assertTrue(formatted.contains("F64"), "Should contain value type F64, got: " + formatted);
      assertTrue(
          formatted.contains("mutable"), "Should contain 'mutable' for VAR, got: " + formatted);
    }

    @Test
    @DisplayName("should follow Global(type, mutability) format")
    void shouldFollowExpectedFormat() {
      final GlobalType type = GlobalType.of(WasmValueType.I64, Mutability.CONST);
      final String formatted = ModuleValidationSupport.formatGlobalType(type);

      assertTrue(formatted.startsWith("Global("), "Should start with 'Global(', got: " + formatted);
      assertTrue(formatted.endsWith(")"), "Should end with ')', got: " + formatted);
    }
  }

  @Nested
  @DisplayName("formatTableType Tests")
  class FormatTableTypeTests {

    @Test
    @DisplayName("should format table with minimum and no maximum")
    void shouldFormatTableNoMax() {
      final TableType type = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.empty());
      final String formatted = ModuleValidationSupport.formatTableType(type);

      assertTrue(
          formatted.contains("FUNCREF"), "Should contain element type FUNCREF, got: " + formatted);
      assertTrue(formatted.contains("min=10"), "Should contain min=10, got: " + formatted);
      assertTrue(
          formatted.contains("max=none"),
          "Should contain max=none for unbounded, got: " + formatted);
    }

    @Test
    @DisplayName("should format table with minimum and maximum")
    void shouldFormatTableWithMax() {
      final TableType type = TableType.of(WasmValueType.EXTERNREF, 5, OptionalLong.of(100));
      final String formatted = ModuleValidationSupport.formatTableType(type);

      assertTrue(formatted.contains("min=5"), "Should contain min=5, got: " + formatted);
      assertTrue(formatted.contains("max=100"), "Should contain max=100, got: " + formatted);
    }

    @Test
    @DisplayName("should follow Table(element, min, max) format")
    void shouldFollowExpectedFormat() {
      final TableType type = TableType.of(WasmValueType.FUNCREF, 0, OptionalLong.empty());
      final String formatted = ModuleValidationSupport.formatTableType(type);

      assertTrue(formatted.startsWith("Table("), "Should start with 'Table(', got: " + formatted);
    }
  }

  @Nested
  @DisplayName("formatMemoryType Tests")
  class FormatMemoryTypeTests {

    @Test
    @DisplayName("should format 32-bit memory with no maximum")
    void shouldFormat32BitNoMax() {
      final MemoryType type = MemoryType.of(1, OptionalLong.empty());
      final String formatted = ModuleValidationSupport.formatMemoryType(type);

      assertTrue(formatted.contains("min=1"), "Should contain min=1, got: " + formatted);
      assertTrue(formatted.contains("max=none"), "Should contain max=none, got: " + formatted);
      assertTrue(formatted.contains("32-bit"), "Should contain '32-bit', got: " + formatted);
      assertTrue(
          formatted.contains("not-shared"), "Should contain 'not-shared', got: " + formatted);
    }

    @Test
    @DisplayName("should format 64-bit memory with maximum")
    void shouldFormat64BitWithMax() {
      final MemoryType type = MemoryType.memory64(1, OptionalLong.of(256));
      final String formatted = ModuleValidationSupport.formatMemoryType(type);

      assertTrue(formatted.contains("min=1"), "Should contain min=1, got: " + formatted);
      assertTrue(formatted.contains("max=256"), "Should contain max=256, got: " + formatted);
      assertTrue(formatted.contains("64-bit"), "Should contain '64-bit', got: " + formatted);
    }

    @Test
    @DisplayName("should format shared memory")
    void shouldFormatSharedMemory() {
      final MemoryType type = MemoryType.shared(1, 100);
      final String formatted = ModuleValidationSupport.formatMemoryType(type);

      assertTrue(formatted.contains("shared"), "Should contain 'shared', got: " + formatted);
    }

    @Test
    @DisplayName("should follow Memory(min, max, bits, shared) format")
    void shouldFollowExpectedFormat() {
      final MemoryType type = MemoryType.of(0, OptionalLong.empty());
      final String formatted = ModuleValidationSupport.formatMemoryType(type);

      assertTrue(formatted.startsWith("Memory("), "Should start with 'Memory(', got: " + formatted);
    }
  }

  @Nested
  @DisplayName("globalTypesMatch Tests")
  class GlobalTypesMatchTests {

    @Test
    @DisplayName("should match identical global types")
    void shouldMatchIdentical() {
      final GlobalType type1 = GlobalType.of(WasmValueType.I32, Mutability.CONST);
      final GlobalType type2 = GlobalType.of(WasmValueType.I32, Mutability.CONST);

      assertTrue(
          ModuleValidationSupport.globalTypesMatch(type1, type2),
          "Identical global types should match");
    }

    @Test
    @DisplayName("should not match different value types")
    void shouldNotMatchDifferentValueTypes() {
      final GlobalType type1 = GlobalType.of(WasmValueType.I32, Mutability.CONST);
      final GlobalType type2 = GlobalType.of(WasmValueType.I64, Mutability.CONST);

      assertFalse(
          ModuleValidationSupport.globalTypesMatch(type1, type2),
          "Different value types should not match");
    }

    @Test
    @DisplayName("should not match different mutability")
    void shouldNotMatchDifferentMutability() {
      final GlobalType type1 = GlobalType.of(WasmValueType.I32, Mutability.CONST);
      final GlobalType type2 = GlobalType.of(WasmValueType.I32, Mutability.VAR);

      assertFalse(
          ModuleValidationSupport.globalTypesMatch(type1, type2),
          "Different mutability should not match");
    }
  }

  @Nested
  @DisplayName("tableTypesMatch Tests")
  class TableTypesMatchTests {

    @Test
    @DisplayName("should match compatible table types")
    void shouldMatchCompatible() {
      final TableType expected = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.empty());
      final TableType actual = TableType.of(WasmValueType.FUNCREF, 20, OptionalLong.empty());

      assertTrue(
          ModuleValidationSupport.tableTypesMatch(expected, actual),
          "Actual min >= expected min should match when no max constraint");
    }

    @Test
    @DisplayName("should not match when actual minimum is less than expected")
    void shouldNotMatchInsufficientMinimum() {
      final TableType expected = TableType.of(WasmValueType.FUNCREF, 20, OptionalLong.empty());
      final TableType actual = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.empty());

      assertFalse(
          ModuleValidationSupport.tableTypesMatch(expected, actual),
          "Actual min < expected min should not match");
    }

    @Test
    @DisplayName("should not match different element types")
    void shouldNotMatchDifferentElementTypes() {
      final TableType expected = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.empty());
      final TableType actual = TableType.of(WasmValueType.EXTERNREF, 10, OptionalLong.empty());

      assertFalse(
          ModuleValidationSupport.tableTypesMatch(expected, actual),
          "Different element types should not match");
    }

    @Test
    @DisplayName("should match when expected has no max and actual has max")
    void shouldMatchNoExpectedMaxWithActualMax() {
      final TableType expected = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.empty());
      final TableType actual = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.of(100));

      assertTrue(
          ModuleValidationSupport.tableTypesMatch(expected, actual),
          "No expected max should accept any actual max");
    }

    @Test
    @DisplayName("should not match when expected has max but actual has no max")
    void shouldNotMatchExpectedMaxNoActualMax() {
      final TableType expected = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.of(100));
      final TableType actual = TableType.of(WasmValueType.FUNCREF, 10, OptionalLong.empty());

      assertFalse(
          ModuleValidationSupport.tableTypesMatch(expected, actual),
          "Expected max with no actual max should not match");
    }
  }

  @Nested
  @DisplayName("memoryTypesMatch Tests")
  class MemoryTypesMatchTests {

    @Test
    @DisplayName("should match compatible memory types")
    void shouldMatchCompatible() {
      final MemoryType expected = MemoryType.of(1, OptionalLong.empty());
      final MemoryType actual = MemoryType.of(2, OptionalLong.empty());

      assertTrue(
          ModuleValidationSupport.memoryTypesMatch(expected, actual),
          "Actual min >= expected min should match");
    }

    @Test
    @DisplayName("should not match when actual minimum is less than expected")
    void shouldNotMatchInsufficientMinimum() {
      final MemoryType expected = MemoryType.of(10, OptionalLong.empty());
      final MemoryType actual = MemoryType.of(5, OptionalLong.empty());

      assertFalse(
          ModuleValidationSupport.memoryTypesMatch(expected, actual),
          "Actual min < expected min should not match");
    }

    @Test
    @DisplayName("should not match different bit widths")
    void shouldNotMatchDifferentBitWidths() {
      final MemoryType expected = MemoryType.of(1, OptionalLong.empty());
      final MemoryType actual = MemoryType.memory64(1, OptionalLong.empty());

      assertFalse(
          ModuleValidationSupport.memoryTypesMatch(expected, actual),
          "32-bit and 64-bit should not match");
    }

    @Test
    @DisplayName("should not match different shared status")
    void shouldNotMatchDifferentSharedStatus() {
      final MemoryType expected = MemoryType.of(1, OptionalLong.of(10));
      final MemoryType actual = MemoryType.shared(1, 10);

      assertFalse(
          ModuleValidationSupport.memoryTypesMatch(expected, actual),
          "Non-shared and shared should not match");
    }

    @Test
    @DisplayName("should match when expected has no max and actual has max")
    void shouldMatchNoExpectedMaxWithActualMax() {
      final MemoryType expected = MemoryType.of(1, OptionalLong.empty());
      final MemoryType actual = MemoryType.of(1, OptionalLong.of(100));

      assertTrue(
          ModuleValidationSupport.memoryTypesMatch(expected, actual),
          "No expected max should accept any actual max");
    }

    @Test
    @DisplayName("should match identical memory types exactly")
    void shouldMatchIdentical() {
      final MemoryType expected = MemoryType.of(1, OptionalLong.of(10));
      final MemoryType actual = MemoryType.of(1, OptionalLong.of(10));

      assertTrue(
          ModuleValidationSupport.memoryTypesMatch(expected, actual),
          "Identical memory types should match");

      assertEquals(
          ModuleValidationSupport.formatMemoryType(expected),
          ModuleValidationSupport.formatMemoryType(actual),
          "Identical types should produce identical format strings");
    }
  }
}
