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

import ai.tegmentum.wasmtime4j.MemoryType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaMemoryType} class.
 *
 * <p>This test class verifies the Panama implementation of MemoryType interface.
 */
@DisplayName("PanamaMemoryType Tests")
class PanamaMemoryTypeTest {

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
    @DisplayName("PanamaMemoryType should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaMemoryType.class.getModifiers()),
          "PanamaMemoryType should be final");
    }

    @Test
    @DisplayName("PanamaMemoryType should implement MemoryType")
    void shouldImplementMemoryType() {
      assertTrue(
          MemoryType.class.isAssignableFrom(PanamaMemoryType.class),
          "PanamaMemoryType should implement MemoryType");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void constructorShouldAcceptValidParameters() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, 10L, false, false, arena, validHandle);

      assertNotNull(memoryType, "MemoryType should be created");
      assertEquals(1L, memoryType.getMinimum(), "Minimum should be 1");
      assertEquals(Optional.of(10L), memoryType.getMaximum(), "Maximum should be 10");
      assertFalse(memoryType.is64Bit(), "Should not be 64-bit");
      assertFalse(memoryType.isShared(), "Should not be shared");
    }

    @Test
    @DisplayName("Constructor should accept null maximum")
    void constructorShouldAcceptNullMaximum() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertEquals(Optional.empty(), memoryType.getMaximum(), "Maximum should be empty");
    }

    @Test
    @DisplayName("Constructor should accept zero minimum")
    void constructorShouldAcceptZeroMinimum() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(0L, null, false, false, arena, validHandle);

      assertEquals(0L, memoryType.getMinimum(), "Minimum should be 0");
    }

    @Test
    @DisplayName("Constructor should throw for negative minimum")
    void constructorShouldThrowForNegativeMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemoryType(-1L, null, false, false, arena, validHandle),
          "Should throw for negative minimum");
    }

    @Test
    @DisplayName("Constructor should throw when maximum is less than minimum")
    void constructorShouldThrowWhenMaximumIsLessThanMinimum() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemoryType(10L, 5L, false, false, arena, validHandle),
          "Should throw when maximum < minimum");
    }

    @Test
    @DisplayName("Constructor should throw for null arena")
    void constructorShouldThrowForNullArena() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemoryType(1L, null, false, false, null, validHandle),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("Constructor should throw for null native handle")
    void constructorShouldThrowForNullNativeHandle() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaMemoryType(1L, null, false, false, arena, null),
          "Should throw for null handle");
    }

    @Test
    @DisplayName("Constructor should accept equal minimum and maximum")
    void constructorShouldAcceptEqualMinimumAndMaximum() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(5L, 5L, false, false, arena, validHandle);

      assertEquals(5L, memoryType.getMinimum(), "Minimum should be 5");
      assertEquals(Optional.of(5L), memoryType.getMaximum(), "Maximum should be 5");
    }
  }

  @Nested
  @DisplayName("getMinimum Tests")
  class GetMinimumTests {

    @Test
    @DisplayName("getMinimum should return correct value")
    void getMinimumShouldReturnCorrectValue() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(42L, null, false, false, arena, validHandle);

      assertEquals(42L, memoryType.getMinimum(), "Should return correct minimum");
    }
  }

  @Nested
  @DisplayName("getMaximum Tests")
  class GetMaximumTests {

    @Test
    @DisplayName("getMaximum should return present Optional when set")
    void getMaximumShouldReturnPresentOptionalWhenSet() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, 100L, false, false, arena, validHandle);

      assertTrue(memoryType.getMaximum().isPresent(), "Maximum should be present");
      assertEquals(100L, memoryType.getMaximum().get(), "Maximum should be 100");
    }

    @Test
    @DisplayName("getMaximum should return empty Optional when not set")
    void getMaximumShouldReturnEmptyOptionalWhenNotSet() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertFalse(memoryType.getMaximum().isPresent(), "Maximum should not be present");
    }
  }

  @Nested
  @DisplayName("is64Bit Tests")
  class Is64BitTests {

    @Test
    @DisplayName("is64Bit should return true when set")
    void is64BitShouldReturnTrueWhenSet() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, true, false, arena, validHandle);

      assertTrue(memoryType.is64Bit(), "Should be 64-bit");
    }

    @Test
    @DisplayName("is64Bit should return false when not set")
    void is64BitShouldReturnFalseWhenNotSet() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertFalse(memoryType.is64Bit(), "Should not be 64-bit");
    }
  }

  @Nested
  @DisplayName("isShared Tests")
  class IsSharedTests {

    @Test
    @DisplayName("isShared should return true when set")
    void isSharedShouldReturnTrueWhenSet() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, true, arena, validHandle);

      assertTrue(memoryType.isShared(), "Should be shared");
    }

    @Test
    @DisplayName("isShared should return false when not set")
    void isSharedShouldReturnFalseWhenNotSet() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertFalse(memoryType.isShared(), "Should not be shared");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return MEMORY")
    void getKindShouldReturnMemory() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
    }
  }

  @Nested
  @DisplayName("getNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("getNativeHandle should return the handle")
    void getNativeHandleShouldReturnTheHandle() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertSame(validHandle, memoryType.getNativeHandle(), "Should return same handle");
    }
  }

  @Nested
  @DisplayName("getArena Tests")
  class GetArenaTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertSame(arena, memoryType.getArena(), "Should return same arena");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, 10L, false, false, arena, validHandle);

      assertEquals(memoryType, memoryType, "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equal memory types")
    void equalsShouldReturnTrueForEqualMemoryTypes() {
      final PanamaMemoryType type1 =
          new PanamaMemoryType(1L, 10L, true, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaMemoryType type2 =
          new PanamaMemoryType(1L, 10L, true, true, arena, otherHandle);

      assertEquals(type1, type2, "Equal memory types should be equal");
    }

    @Test
    @DisplayName("equals should return false for different minimum")
    void equalsShouldReturnFalseForDifferentMinimum() {
      final PanamaMemoryType type1 =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaMemoryType type2 =
          new PanamaMemoryType(2L, null, false, false, arena, otherHandle);

      assertNotEquals(type1, type2, "Different minimum should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different maximum")
    void equalsShouldReturnFalseForDifferentMaximum() {
      final PanamaMemoryType type1 =
          new PanamaMemoryType(1L, 10L, false, false, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaMemoryType type2 =
          new PanamaMemoryType(1L, 20L, false, false, arena, otherHandle);

      assertNotEquals(type1, type2, "Different maximum should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different 64-bit flag")
    void equalsShouldReturnFalseForDifferent64BitFlag() {
      final PanamaMemoryType type1 =
          new PanamaMemoryType(1L, null, true, false, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaMemoryType type2 =
          new PanamaMemoryType(1L, null, false, false, arena, otherHandle);

      assertNotEquals(type1, type2, "Different 64-bit flag should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different shared flag")
    void equalsShouldReturnFalseForDifferentSharedFlag() {
      final PanamaMemoryType type1 =
          new PanamaMemoryType(1L, null, false, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaMemoryType type2 =
          new PanamaMemoryType(1L, null, false, false, arena, otherHandle);

      assertNotEquals(type1, type2, "Different shared flag should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertFalse(memoryType.equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for non-MemoryType")
    void equalsShouldReturnFalseForNonMemoryType() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      assertFalse(memoryType.equals("not a MemoryType"), "Should not equal non-MemoryType");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, 10L, true, true, arena, validHandle);

      final int hash1 = memoryType.hashCode();
      final int hash2 = memoryType.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("equal objects should have equal hash codes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final PanamaMemoryType type1 =
          new PanamaMemoryType(1L, 10L, true, true, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaMemoryType type2 =
          new PanamaMemoryType(1L, 10L, true, true, arena, otherHandle);

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
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, 10L, true, true, arena, validHandle);
      final String str = memoryType.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("MemoryType"), "Should contain MemoryType");
      assertTrue(str.contains("min"), "Should contain min");
      assertTrue(str.contains("max"), "Should contain max");
      assertTrue(str.contains("64bit"), "Should contain 64bit");
      assertTrue(str.contains("shared"), "Should contain shared");
    }

    @Test
    @DisplayName("toString should handle unlimited maximum")
    void toStringShouldHandleUnlimitedMaximum() {
      final PanamaMemoryType memoryType =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);
      final String str = memoryType.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("unlimited"), "Should contain unlimited for null max");
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

        final PanamaMemoryType memoryType =
            new PanamaMemoryType(1L, 100L, true, true, testArena, handle);

        // Verify all getters work
        assertEquals(1L, memoryType.getMinimum(), "Minimum should be 1");
        assertEquals(Optional.of(100L), memoryType.getMaximum(), "Maximum should be 100");
        assertTrue(memoryType.is64Bit(), "Should be 64-bit");
        assertTrue(memoryType.isShared(), "Should be shared");
        assertEquals(WasmTypeKind.MEMORY, memoryType.getKind(), "Kind should be MEMORY");
        assertSame(handle, memoryType.getNativeHandle(), "Handle should match");
        assertSame(testArena, memoryType.getArena(), "Arena should match");

        // Verify toString works
        assertDoesNotThrow(memoryType::toString, "toString should not throw");

        // Verify hashCode works
        assertDoesNotThrow(memoryType::hashCode, "hashCode should not throw");
      }
    }

    @Test
    @DisplayName("Different memory configurations should be distinguishable")
    void differentMemoryConfigurationsShouldBeDistinguishable() {
      // Basic memory
      final PanamaMemoryType basic =
          new PanamaMemoryType(1L, null, false, false, arena, validHandle);

      // 64-bit memory
      final MemorySegment handle2 = arena.allocate(8);
      final PanamaMemoryType mem64 =
          new PanamaMemoryType(1L, null, true, false, arena, handle2);

      // Shared memory
      final MemorySegment handle3 = arena.allocate(8);
      final PanamaMemoryType shared =
          new PanamaMemoryType(1L, null, false, true, arena, handle3);

      // Bounded memory
      final MemorySegment handle4 = arena.allocate(8);
      final PanamaMemoryType bounded =
          new PanamaMemoryType(1L, 10L, false, false, arena, handle4);

      assertNotEquals(basic, mem64, "Different configs should not be equal");
      assertNotEquals(basic, shared, "Different configs should not be equal");
      assertNotEquals(basic, bounded, "Different configs should not be equal");
      assertNotEquals(mem64, shared, "Different configs should not be equal");
    }
  }
}
