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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TableType} interface.
 *
 * <p>TableType represents the type information of a WebAssembly table.
 */
@DisplayName("TableType Tests")
class TableTypeTest {

  /** Test implementation of TableType for testing purposes. */
  private static class TestTableType implements TableType {
    private final WasmValueType elementType;
    private final long minimum;
    private final Long maximum;

    TestTableType(final WasmValueType elementType, final long minimum, final Long maximum) {
      this.elementType = elementType;
      this.minimum = minimum;
      this.maximum = maximum;
    }

    @Override
    public WasmValueType getElementType() {
      return elementType;
    }

    @Override
    public long getMinimum() {
      return minimum;
    }

    @Override
    public Optional<Long> getMaximum() {
      return Optional.ofNullable(maximum);
    }
  }

  @Nested
  @DisplayName("Default getKind Method Tests")
  class DefaultGetKindMethodTests {

    @Test
    @DisplayName("getKind should return TABLE")
    void getKindShouldReturnTable() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 0, 100L);

      assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "getKind should return TABLE");
    }

    @Test
    @DisplayName("getKind should be a default method")
    void getKindShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = TableType.class.getMethod("getKind");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  @Nested
  @DisplayName("Element Type Tests")
  class ElementTypeTests {

    @Test
    @DisplayName("should handle FUNCREF element type")
    void shouldHandleFuncrefElementType() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 0, null);

      assertEquals(
          WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
    }

    @Test
    @DisplayName("should handle EXTERNREF element type")
    void shouldHandleExternrefElementType() {
      final TableType tableType = new TestTableType(WasmValueType.EXTERNREF, 0, null);

      assertEquals(
          WasmValueType.EXTERNREF, tableType.getElementType(), "Element type should be EXTERNREF");
    }
  }

  @Nested
  @DisplayName("Size Constraint Tests")
  class SizeConstraintTests {

    @Test
    @DisplayName("should handle minimum elements correctly")
    void shouldHandleMinimumElementsCorrectly() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 10, 100L);

      assertEquals(10L, tableType.getMinimum(), "Minimum should be 10 elements");
    }

    @Test
    @DisplayName("should handle maximum elements correctly")
    void shouldHandleMaximumElementsCorrectly() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 0, 256L);

      assertTrue(tableType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(256L, tableType.getMaximum().get(), "Maximum should be 256 elements");
    }

    @Test
    @DisplayName("should handle no maximum (unlimited)")
    void shouldHandleNoMaximum() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 0, null);

      assertFalse(tableType.getMaximum().isPresent(), "Maximum should not be present");
    }

    @Test
    @DisplayName("should handle zero minimum")
    void shouldHandleZeroMinimum() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 0, 10L);

      assertEquals(0L, tableType.getMinimum(), "Minimum should be 0");
    }

    @Test
    @DisplayName("should handle minimum equals maximum")
    void shouldHandleMinimumEqualsMaximum() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 50, 50L);

      assertEquals(50L, tableType.getMinimum(), "Minimum should be 50");
      assertEquals(50L, tableType.getMaximum().get(), "Maximum should be 50");
    }
  }

  @Nested
  @DisplayName("Edge Case Tests")
  class EdgeCaseTests {

    @Test
    @DisplayName("should handle large element counts")
    void shouldHandleLargeElementCounts() {
      final long largeMin = 1000000L;
      final long largeMax = 10000000L;
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, largeMin, largeMax);

      assertEquals(largeMin, tableType.getMinimum(), "Should handle large minimum");
      assertEquals(largeMax, tableType.getMaximum().get(), "Should handle large maximum");
    }

    @Test
    @DisplayName("should handle funcref table with constraints")
    void shouldHandleFuncrefTableWithConstraints() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 10, 1000L);

      assertEquals(
          WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
      assertEquals(10L, tableType.getMinimum(), "Minimum should be 10");
      assertEquals(1000L, tableType.getMaximum().get(), "Maximum should be 1000");
      assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Kind should be TABLE");
    }

    @Test
    @DisplayName("should handle externref table without maximum")
    void shouldHandleExternrefTableWithoutMaximum() {
      final TableType tableType = new TestTableType(WasmValueType.EXTERNREF, 0, null);

      assertEquals(
          WasmValueType.EXTERNREF, tableType.getElementType(), "Element type should be EXTERNREF");
      assertEquals(0L, tableType.getMinimum(), "Minimum should be 0");
      assertFalse(tableType.getMaximum().isPresent(), "Maximum should not be present");
    }
  }

  @Nested
  @DisplayName("WasmType Integration Tests")
  class WasmTypeIntegrationTests {

    @Test
    @DisplayName("TableType should implement WasmType")
    void tableTypeShouldImplementWasmType() {
      final TableType tableType = new TestTableType(WasmValueType.FUNCREF, 0, null);

      assertTrue(tableType instanceof WasmType, "TableType should be instance of WasmType");
    }

    @Test
    @DisplayName("getKind should return TABLE for all configurations")
    void getKindShouldReturnTableForAllConfigurations() {
      final TableType funcrefTable = new TestTableType(WasmValueType.FUNCREF, 0, null);
      final TableType externrefTable = new TestTableType(WasmValueType.EXTERNREF, 10, 100L);
      final TableType unlimitedTable = new TestTableType(WasmValueType.FUNCREF, 0, null);
      final TableType limitedTable = new TestTableType(WasmValueType.FUNCREF, 5, 50L);

      assertEquals(
          WasmTypeKind.TABLE, funcrefTable.getKind(), "FUNCREF table should be TABLE kind");
      assertEquals(
          WasmTypeKind.TABLE, externrefTable.getKind(), "EXTERNREF table should be TABLE kind");
      assertEquals(
          WasmTypeKind.TABLE, unlimitedTable.getKind(), "Unlimited table should be TABLE kind");
      assertEquals(
          WasmTypeKind.TABLE, limitedTable.getKind(), "Limited table should be TABLE kind");
    }
  }
}
