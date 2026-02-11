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

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaGlobalType} class.
 *
 * <p>This test class verifies the Panama implementation of GlobalType interface.
 */
@DisplayName("PanamaGlobalType Tests")
class PanamaGlobalTypeTest {

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
    @DisplayName("PanamaGlobalType should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaGlobalType.class.getModifiers()),
          "PanamaGlobalType should be final");
    }

    @Test
    @DisplayName("PanamaGlobalType should implement GlobalType")
    void shouldImplementGlobalType() {
      assertTrue(
          GlobalType.class.isAssignableFrom(PanamaGlobalType.class),
          "PanamaGlobalType should implement GlobalType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void constructorShouldAcceptValidParameters() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      assertNotNull(globalType, "GlobalType should be created");
      assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
      assertTrue(globalType.isMutable(), "Should be mutable");
    }

    @Test
    @DisplayName("Constructor should accept immutable global")
    void constructorShouldAcceptImmutableGlobal() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.F64, false, arena, validHandle);

      assertFalse(globalType.isMutable(), "Should be immutable");
    }

    @Test
    @DisplayName("Constructor should throw for null value type")
    void constructorShouldThrowForNullValueType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaGlobalType(null, true, arena, validHandle),
          "Should throw for null value type");
    }

    @Test
    @DisplayName("Constructor should throw for null arena")
    void constructorShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaGlobalType(WasmValueType.I32, true, null, validHandle),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("Constructor should throw for null native handle")
    void constructorShouldThrowForNullNativeHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaGlobalType(WasmValueType.I32, true, arena, null),
          "Should throw for null handle");
    }

    @Test
    @DisplayName("Constructor should accept all value types")
    void constructorShouldAcceptAllValueTypes() {
      for (WasmValueType valueType : WasmValueType.values()) {
        final MemorySegment handle = arena.allocate(8);
        final PanamaGlobalType globalType = new PanamaGlobalType(valueType, false, arena, handle);

        assertEquals(valueType, globalType.getValueType(), "Value type should match");
      }
    }
  }

  @Nested
  @DisplayName("getValueType Tests")
  class GetValueTypeTests {

    @Test
    @DisplayName("getValueType should return correct value type")
    void getValueTypeShouldReturnCorrectValueType() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I64, false, arena, validHandle);

      assertEquals(WasmValueType.I64, globalType.getValueType(), "Should return correct type");
    }

    @Test
    @DisplayName("getValueType should return I32")
    void getValueTypeShouldReturnI32() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, false, arena, validHandle);

      assertEquals(WasmValueType.I32, globalType.getValueType(), "Should return I32");
    }

    @Test
    @DisplayName("getValueType should return F32")
    void getValueTypeShouldReturnF32() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.F32, false, arena, validHandle);

      assertEquals(WasmValueType.F32, globalType.getValueType(), "Should return F32");
    }

    @Test
    @DisplayName("getValueType should return F64")
    void getValueTypeShouldReturnF64() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.F64, false, arena, validHandle);

      assertEquals(WasmValueType.F64, globalType.getValueType(), "Should return F64");
    }
  }

  @Nested
  @DisplayName("isMutable Tests")
  class IsMutableTests {

    @Test
    @DisplayName("isMutable should return true for mutable global")
    void isMutableShouldReturnTrueForMutableGlobal() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      assertTrue(globalType.isMutable(), "Should be mutable");
    }

    @Test
    @DisplayName("isMutable should return false for immutable global")
    void isMutableShouldReturnFalseForImmutableGlobal() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, false, arena, validHandle);

      assertFalse(globalType.isMutable(), "Should be immutable");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return GLOBAL")
    void getKindShouldReturnGlobal() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, false, arena, validHandle);

      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");
    }
  }

  @Nested
  @DisplayName("getNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("getNativeHandle should return the handle")
    void getNativeHandleShouldReturnTheHandle() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, false, arena, validHandle);

      assertSame(validHandle, globalType.getNativeHandle(), "Should return same handle");
    }
  }

  @Nested
  @DisplayName("getArena Tests")
  class GetArenaTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, false, arena, validHandle);

      assertSame(arena, globalType.getArena(), "Should return same arena");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      assertEquals(globalType, globalType, "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equal global types")
    void equalsShouldReturnTrueForEqualGlobalTypes() {
      final PanamaGlobalType type1 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaGlobalType type2 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, otherHandle);

      assertEquals(type1, type2, "Equal global types should be equal");
    }

    @Test
    @DisplayName("equals should return false for different value types")
    void equalsShouldReturnFalseForDifferentValueTypes() {
      final PanamaGlobalType type1 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaGlobalType type2 =
          new PanamaGlobalType(WasmValueType.I64, true, arena, otherHandle);

      assertNotEquals(type1, type2, "Different value types should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different mutability")
    void equalsShouldReturnFalseForDifferentMutability() {
      final PanamaGlobalType type1 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaGlobalType type2 =
          new PanamaGlobalType(WasmValueType.I32, false, arena, otherHandle);

      assertNotEquals(type1, type2, "Different mutability should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      assertFalse(globalType.equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for non-GlobalType")
    void equalsShouldReturnFalseForNonGlobalType() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      assertFalse(globalType.equals("not a GlobalType"), "Should not equal non-GlobalType");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      final int hash1 = globalType.hashCode();
      final int hash2 = globalType.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("equal objects should have equal hash codes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final PanamaGlobalType type1 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaGlobalType type2 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, otherHandle);

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
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);
      final String str = globalType.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("GlobalType"), "Should contain GlobalType");
      assertTrue(str.contains("valueType"), "Should contain valueType");
      assertTrue(str.contains("mutable"), "Should contain mutable");
    }

    @Test
    @DisplayName("toString should include value type name")
    void toStringShouldIncludeValueTypeName() {
      final PanamaGlobalType globalType =
          new PanamaGlobalType(WasmValueType.F64, false, arena, validHandle);
      final String str = globalType.toString();

      assertTrue(str.contains("F64"), "Should contain value type name");
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

        final PanamaGlobalType globalType =
            new PanamaGlobalType(WasmValueType.I64, true, testArena, handle);

        // Verify all getters work
        assertEquals(WasmValueType.I64, globalType.getValueType(), "Value type should be I64");
        assertTrue(globalType.isMutable(), "Should be mutable");
        assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");
        assertSame(handle, globalType.getNativeHandle(), "Handle should match");
        assertSame(testArena, globalType.getArena(), "Arena should match");

        // Verify toString works
        assertDoesNotThrow(globalType::toString, "toString should not throw");

        // Verify hashCode works
        assertDoesNotThrow(globalType::hashCode, "hashCode should not throw");
      }
    }

    @Test
    @DisplayName("All value type and mutability combinations should work")
    void allValueTypeAndMutabilityCombinationsShouldWork() {
      final WasmValueType[] valueTypes = {
        WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64
      };

      for (WasmValueType valueType : valueTypes) {
        for (boolean mutable : new boolean[] {true, false}) {
          final MemorySegment handle = arena.allocate(8);
          final PanamaGlobalType globalType =
              new PanamaGlobalType(valueType, mutable, arena, handle);

          assertEquals(valueType, globalType.getValueType(), "Value type should match");
          assertEquals(mutable, globalType.isMutable(), "Mutability should match");
        }
      }
    }

    @Test
    @DisplayName("Different global configurations should be distinguishable")
    void differentGlobalConfigurationsShouldBeDistinguishable() {
      final PanamaGlobalType mutableI32 =
          new PanamaGlobalType(WasmValueType.I32, true, arena, validHandle);

      final MemorySegment handle2 = arena.allocate(8);
      final PanamaGlobalType immutableI32 =
          new PanamaGlobalType(WasmValueType.I32, false, arena, handle2);

      final MemorySegment handle3 = arena.allocate(8);
      final PanamaGlobalType mutableF64 =
          new PanamaGlobalType(WasmValueType.F64, true, arena, handle3);

      assertNotEquals(mutableI32, immutableI32, "Different mutability should not be equal");
      assertNotEquals(mutableI32, mutableF64, "Different types should not be equal");
      assertNotEquals(immutableI32, mutableF64, "Different configs should not be equal");
    }
  }
}
