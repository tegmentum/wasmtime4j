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

package ai.tegmentum.wasmtime4j.panama.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.TableType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaTableType} class.
 *
 * <p>This test class verifies the Panama implementation of TableType interface.
 */
@DisplayName("PanamaTableType Tests")
class PanamaTableTypeTest {

  private Arena arena;
  private MemorySegment validHandle;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
    validHandle = arena.allocate(8);
  }

  @AfterEach
  void tearDown() {
    if (arena.scope().isAlive()) {
      arena.close();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("PanamaTableType should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaTableType.class.getModifiers()),
          "PanamaTableType should be final");
    }

    @Test
    @DisplayName("PanamaTableType should implement TableType")
    void shouldImplementTableType() {
      assertTrue(
          TableType.class.isAssignableFrom(PanamaTableType.class),
          "PanamaTableType should implement TableType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters with FUNCREF")
    void constructorShouldAcceptValidParametersWithFuncref() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);

      assertNotNull(tableType, "TableType should be created");
      assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
      assertEquals(1L, tableType.getMinimum(), "Minimum should be 1");
      assertEquals(Optional.of(10L), tableType.getMaximum(), "Maximum should be 10");
    }

    @Test
    @DisplayName("Constructor should accept valid parameters with EXTERNREF")
    void constructorShouldAcceptValidParametersWithExternref() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.EXTERNREF, 0L, null, arena, validHandle);

      assertEquals(WasmValueType.EXTERNREF, tableType.getElementType(), "Element type should be EXTERNREF");
    }

    @Test
    @DisplayName("Constructor should accept null maximum")
    void constructorShouldAcceptNullMaximum() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertEquals(Optional.empty(), tableType.getMaximum(), "Maximum should be empty");
    }

    @Test
    @DisplayName("Constructor should accept zero minimum")
    void constructorShouldAcceptZeroMinimum() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 0L, null, arena, validHandle);

      assertEquals(0L, tableType.getMinimum(), "Minimum should be 0");
    }

    @Test
    @DisplayName("Constructor should throw for null element type")
    void constructorShouldThrowForNullElementType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaTableType(null, 1L, null, arena, validHandle),
          "Should throw for null element type");
    }

    @Test
    @DisplayName("Constructor should throw for non-reference element type")
    void constructorShouldThrowForNonReferenceElementType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaTableType(WasmValueType.I32, 1L, null, arena, validHandle),
          "Should throw for non-reference element type");
    }

    @Test
    @DisplayName("Constructor should throw for negative minimum")
    void constructorShouldThrowForNegativeMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaTableType(WasmValueType.FUNCREF, -1L, null, arena, validHandle),
          "Should throw for negative minimum");
    }

    @Test
    @DisplayName("Constructor should throw when maximum is less than minimum")
    void constructorShouldThrowWhenMaximumIsLessThanMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaTableType(WasmValueType.FUNCREF, 10L, 5L, arena, validHandle),
          "Should throw when maximum < minimum");
    }

    @Test
    @DisplayName("Constructor should throw for null arena")
    void constructorShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaTableType(WasmValueType.FUNCREF, 1L, null, null, validHandle),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("Constructor should throw for null native handle")
    void constructorShouldThrowForNullNativeHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, null),
          "Should throw for null handle");
    }

    @Test
    @DisplayName("Constructor should accept equal minimum and maximum")
    void constructorShouldAcceptEqualMinimumAndMaximum() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 5L, 5L, arena, validHandle);

      assertEquals(5L, tableType.getMinimum(), "Minimum should be 5");
      assertEquals(Optional.of(5L), tableType.getMaximum(), "Maximum should be 5");
    }
  }

  @Nested
  @DisplayName("getElementType Tests")
  class GetElementTypeTests {

    @Test
    @DisplayName("getElementType should return FUNCREF")
    void getElementTypeShouldReturnFuncref() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Should return FUNCREF");
    }

    @Test
    @DisplayName("getElementType should return EXTERNREF")
    void getElementTypeShouldReturnExternref() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.EXTERNREF, 1L, null, arena, validHandle);

      assertEquals(WasmValueType.EXTERNREF, tableType.getElementType(), "Should return EXTERNREF");
    }
  }

  @Nested
  @DisplayName("getMinimum Tests")
  class GetMinimumTests {

    @Test
    @DisplayName("getMinimum should return correct value")
    void getMinimumShouldReturnCorrectValue() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 42L, null, arena, validHandle);

      assertEquals(42L, tableType.getMinimum(), "Should return correct minimum");
    }
  }

  @Nested
  @DisplayName("getMaximum Tests")
  class GetMaximumTests {

    @Test
    @DisplayName("getMaximum should return present Optional when set")
    void getMaximumShouldReturnPresentOptionalWhenSet() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 100L, arena, validHandle);

      assertTrue(tableType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(100L, tableType.getMaximum().get(), "Maximum should be 100");
    }

    @Test
    @DisplayName("getMaximum should return empty Optional when not set")
    void getMaximumShouldReturnEmptyOptionalWhenNotSet() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertFalse(tableType.getMaximum().isPresent(), "Maximum should not be present");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return TABLE")
    void getKindShouldReturnTable() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Kind should be TABLE");
    }
  }

  @Nested
  @DisplayName("getNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("getNativeHandle should return the handle")
    void getNativeHandleShouldReturnTheHandle() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertSame(validHandle, tableType.getNativeHandle(), "Should return same handle");
    }
  }

  @Nested
  @DisplayName("getArena Tests")
  class GetArenaTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertSame(arena, tableType.getArena(), "Should return same arena");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);

      assertEquals(tableType, tableType, "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equal table types")
    void equalsShouldReturnTrueForEqualTableTypes() {
      final PanamaTableType type1 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaTableType type2 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, otherHandle);

      assertEquals(type1, type2, "Equal table types should be equal");
    }

    @Test
    @DisplayName("equals should return false for different element types")
    void equalsShouldReturnFalseForDifferentElementTypes() {
      final PanamaTableType type1 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaTableType type2 =
          new PanamaTableType(WasmValueType.EXTERNREF, 1L, null, arena, otherHandle);

      assertNotEquals(type1, type2, "Different element types should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different minimum")
    void equalsShouldReturnFalseForDifferentMinimum() {
      final PanamaTableType type1 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaTableType type2 =
          new PanamaTableType(WasmValueType.FUNCREF, 2L, null, arena, otherHandle);

      assertNotEquals(type1, type2, "Different minimum should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different maximum")
    void equalsShouldReturnFalseForDifferentMaximum() {
      final PanamaTableType type1 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaTableType type2 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 20L, arena, otherHandle);

      assertNotEquals(type1, type2, "Different maximum should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertFalse(tableType.equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for non-TableType")
    void equalsShouldReturnFalseForNonTableType() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      assertFalse(tableType.equals("not a TableType"), "Should not equal non-TableType");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);

      final int hash1 = tableType.hashCode();
      final int hash2 = tableType.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("equal objects should have equal hash codes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final PanamaTableType type1 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaTableType type2 =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, otherHandle);

      assertEquals(
          type1.hashCode(), type2.hashCode(), "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, validHandle);
      final String str = tableType.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("TableType"), "Should contain TableType");
      assertTrue(str.contains("element"), "Should contain element");
      assertTrue(str.contains("min"), "Should contain min");
      assertTrue(str.contains("max"), "Should contain max");
    }

    @Test
    @DisplayName("toString should handle unlimited maximum")
    void toStringShouldHandleUnlimitedMaximum() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);
      final String str = tableType.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("unlimited"), "Should contain unlimited for null max");
    }

    @Test
    @DisplayName("toString should include element type name")
    void toStringShouldIncludeElementTypeName() {
      final PanamaTableType tableType =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);
      final String str = tableType.toString();

      assertTrue(str.contains("FUNCREF"), "Should contain element type name");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("Full lifecycle should work correctly")
    void fullLifecycleShouldWorkCorrectly() {
      try (Arena testArena = Arena.ofConfined()) {
        final MemorySegment handle = testArena.allocate(8);

        final PanamaTableType tableType =
            new PanamaTableType(WasmValueType.FUNCREF, 1L, 100L, testArena, handle);

        // Verify all getters work
        assertEquals(WasmValueType.FUNCREF, tableType.getElementType(), "Element type should be FUNCREF");
        assertEquals(1L, tableType.getMinimum(), "Minimum should be 1");
        assertEquals(Optional.of(100L), tableType.getMaximum(), "Maximum should be 100");
        assertEquals(WasmTypeKind.TABLE, tableType.getKind(), "Kind should be TABLE");
        assertSame(handle, tableType.getNativeHandle(), "Handle should match");
        assertSame(testArena, tableType.getArena(), "Arena should match");

        // Verify toString works
        assertDoesNotThrow(tableType::toString, "toString should not throw");

        // Verify hashCode works
        assertDoesNotThrow(tableType::hashCode, "hashCode should not throw");
      }
    }

    @Test
    @DisplayName("Different table configurations should be distinguishable")
    void differentTableConfigurationsShouldBeDistinguishable() {
      // FUNCREF table
      final PanamaTableType funcrefTable =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, null, arena, validHandle);

      // EXTERNREF table
      final MemorySegment handle2 = arena.allocate(8);
      final PanamaTableType externrefTable =
          new PanamaTableType(WasmValueType.EXTERNREF, 1L, null, arena, handle2);

      // Bounded table
      final MemorySegment handle3 = arena.allocate(8);
      final PanamaTableType boundedTable =
          new PanamaTableType(WasmValueType.FUNCREF, 1L, 10L, arena, handle3);

      // Larger minimum table
      final MemorySegment handle4 = arena.allocate(8);
      final PanamaTableType largerMinTable =
          new PanamaTableType(WasmValueType.FUNCREF, 10L, null, arena, handle4);

      assertNotEquals(funcrefTable, externrefTable, "Different element types should not be equal");
      assertNotEquals(funcrefTable, boundedTable, "Different bounds should not be equal");
      assertNotEquals(funcrefTable, largerMinTable, "Different minimums should not be equal");
    }

    @Test
    @DisplayName("Reference types validation should work correctly")
    void referenceTypesValidationShouldWorkCorrectly() {
      // FUNCREF should work
      assertDoesNotThrow(
          () -> new PanamaTableType(WasmValueType.FUNCREF, 0L, null, arena, validHandle),
          "FUNCREF should be valid");

      // EXTERNREF should work
      final MemorySegment handle2 = arena.allocate(8);
      assertDoesNotThrow(
          () -> new PanamaTableType(WasmValueType.EXTERNREF, 0L, null, arena, handle2),
          "EXTERNREF should be valid");

      // Non-reference types should fail
      final WasmValueType[] nonRefTypes = {
          WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
      };

      for (WasmValueType type : nonRefTypes) {
        final MemorySegment handle = arena.allocate(8);
        assertThrows(
            IllegalArgumentException.class,
            () -> new PanamaTableType(type, 0L, null, arena, handle),
            type + " should not be valid as table element type");
      }
    }
  }
}
