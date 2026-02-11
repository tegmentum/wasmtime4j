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

import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link PanamaFuncType} class.
 *
 * <p>This test class verifies the Panama implementation of FuncType interface.
 */
@DisplayName("PanamaFuncType Tests")
class PanamaFuncTypeTest {

  private Arena arena;
  private MemorySegment validHandle;

  @BeforeEach
  void setUp() {
    arena = Arena.ofConfined();
    // Create a valid non-null handle for testing
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
    @DisplayName("PanamaFuncType should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(PanamaFuncType.class.getModifiers()),
          "PanamaFuncType should be final");
    }

    @Test
    @DisplayName("PanamaFuncType should implement FuncType")
    void shouldImplementFuncType() {
      assertTrue(
          FuncType.class.isAssignableFrom(PanamaFuncType.class),
          "PanamaFuncType should implement FuncType");
    }
  }

  @Nested
  @DisplayName("List Constructor Tests")
  class ListConstructorTests {

    @Test
    @DisplayName("Constructor should accept valid parameters")
    void constructorShouldAcceptValidParameters() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final List<WasmValueType> results = Collections.singletonList(WasmValueType.F64);

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertNotNull(funcType, "FuncType should be created");
      assertEquals(2, funcType.getParams().size(), "Should have 2 params");
      assertEquals(1, funcType.getResults().size(), "Should have 1 result");
    }

    @Test
    @DisplayName("Constructor should throw for null params")
    void constructorShouldThrowForNullParams() {
      final List<WasmValueType> results = Collections.emptyList();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaFuncType(null, results, arena, validHandle),
          "Should throw for null params");
    }

    @Test
    @DisplayName("Constructor should throw for null results")
    void constructorShouldThrowForNullResults() {
      final List<WasmValueType> params = Collections.emptyList();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaFuncType(params, null, arena, validHandle),
          "Should throw for null results");
    }

    @Test
    @DisplayName("Constructor should throw for null arena")
    void constructorShouldThrowForNullArena() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaFuncType(params, results, null, validHandle),
          "Should throw for null arena");
    }

    @Test
    @DisplayName("Constructor should throw for null native handle")
    void constructorShouldThrowForNullNativeHandle() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaFuncType(params, results, arena, null),
          "Should throw for null handle");
    }

    @Test
    @DisplayName("Constructor should throw for null param element")
    void constructorShouldThrowForNullParamElement() {
      final List<WasmValueType> params = new ArrayList<>();
      params.add(WasmValueType.I32);
      params.add(null);
      final List<WasmValueType> results = Collections.emptyList();

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaFuncType(params, results, arena, validHandle),
          "Should throw for null param element");
    }

    @Test
    @DisplayName("Constructor should throw for null result element")
    void constructorShouldThrowForNullResultElement() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = new ArrayList<>();
      results.add(null);

      assertThrows(
          IllegalArgumentException.class,
          () -> new PanamaFuncType(params, results, arena, validHandle),
          "Should throw for null result element");
    }

    @Test
    @DisplayName("Constructor should accept empty params and results")
    void constructorShouldAcceptEmptyParamsAndResults() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertTrue(funcType.getParams().isEmpty(), "Params should be empty");
      assertTrue(funcType.getResults().isEmpty(), "Results should be empty");
    }
  }

  @Nested
  @DisplayName("Array Constructor Tests")
  class ArrayConstructorTests {

    @Test
    @DisplayName("Array constructor should accept valid parameters")
    void arrayConstructorShouldAcceptValidParameters() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.F32};
      final WasmValueType[] results = {WasmValueType.I64};

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertNotNull(funcType, "FuncType should be created");
      assertEquals(2, funcType.getParams().size(), "Should have 2 params");
      assertEquals(1, funcType.getResults().size(), "Should have 1 result");
    }

    @Test
    @DisplayName("Array constructor should accept empty arrays")
    void arrayConstructorShouldAcceptEmptyArrays() {
      final WasmValueType[] params = {};
      final WasmValueType[] results = {};

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertTrue(funcType.getParams().isEmpty(), "Params should be empty");
      assertTrue(funcType.getResults().isEmpty(), "Results should be empty");
    }
  }

  @Nested
  @DisplayName("getParams Tests")
  class GetParamsTests {

    @Test
    @DisplayName("getParams should return correct params")
    void getParamsShouldReturnCorrectParams() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertEquals(params, funcType.getParams(), "Should return correct params");
    }

    @Test
    @DisplayName("getParams should return unmodifiable list")
    void getParamsShouldReturnUnmodifiableList() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertThrows(
          UnsupportedOperationException.class,
          () -> funcType.getParams().add(WasmValueType.F32),
          "Params list should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("getResults Tests")
  class GetResultsTests {

    @Test
    @DisplayName("getResults should return correct results")
    void getResultsShouldReturnCorrectResults() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Arrays.asList(WasmValueType.F32, WasmValueType.F64);

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertEquals(results, funcType.getResults(), "Should return correct results");
    }

    @Test
    @DisplayName("getResults should return unmodifiable list")
    void getResultsShouldReturnUnmodifiableList() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I32);

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertThrows(
          UnsupportedOperationException.class,
          () -> funcType.getResults().add(WasmValueType.F32),
          "Results list should be unmodifiable");
    }
  }

  @Nested
  @DisplayName("getKind Tests")
  class GetKindTests {

    @Test
    @DisplayName("getKind should return FUNCTION")
    void getKindShouldReturnFunction() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertEquals(WasmTypeKind.FUNCTION, funcType.getKind(), "Kind should be FUNCTION");
    }
  }

  @Nested
  @DisplayName("getNativeHandle Tests")
  class GetNativeHandleTests {

    @Test
    @DisplayName("getNativeHandle should return the handle")
    void getNativeHandleShouldReturnTheHandle() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertSame(validHandle, funcType.getNativeHandle(), "Should return same handle");
    }
  }

  @Nested
  @DisplayName("getArena Tests")
  class GetArenaTests {

    @Test
    @DisplayName("getArena should return the arena")
    void getArenaShouldReturnTheArena() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertSame(arena, funcType.getArena(), "Should return same arena");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("equals should return true for same instance")
    void equalsShouldReturnTrueForSameInstance() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I64);

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertEquals(funcType, funcType, "Same instance should be equal");
    }

    @Test
    @DisplayName("equals should return true for equal function types")
    void equalsShouldReturnTrueForEqualFunctionTypes() {
      final List<WasmValueType> params1 = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results1 = Arrays.asList(WasmValueType.I64);
      final List<WasmValueType> params2 = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results2 = Arrays.asList(WasmValueType.I64);

      final PanamaFuncType funcType1 = new PanamaFuncType(params1, results1, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaFuncType funcType2 = new PanamaFuncType(params2, results2, arena, otherHandle);

      assertEquals(funcType1, funcType2, "Function types with same params/results should be equal");
    }

    @Test
    @DisplayName("equals should return false for different params")
    void equalsShouldReturnFalseForDifferentParams() {
      final List<WasmValueType> params1 = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> params2 = Arrays.asList(WasmValueType.I64);
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType1 = new PanamaFuncType(params1, results, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaFuncType funcType2 = new PanamaFuncType(params2, results, arena, otherHandle);

      assertNotEquals(funcType1, funcType2, "Different params should not be equal");
    }

    @Test
    @DisplayName("equals should return false for different results")
    void equalsShouldReturnFalseForDifferentResults() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results1 = Arrays.asList(WasmValueType.F32);
      final List<WasmValueType> results2 = Arrays.asList(WasmValueType.F64);

      final PanamaFuncType funcType1 = new PanamaFuncType(params, results1, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaFuncType funcType2 = new PanamaFuncType(params, results2, arena, otherHandle);

      assertNotEquals(funcType1, funcType2, "Different results should not be equal");
    }

    @Test
    @DisplayName("equals should return false for null")
    void equalsShouldReturnFalseForNull() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertFalse(funcType.equals(null), "Should not equal null");
    }

    @Test
    @DisplayName("equals should return false for non-FuncType")
    void equalsShouldReturnFalseForNonFuncType() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      assertFalse(funcType.equals("not a FuncType"), "Should not equal non-FuncType");
    }
  }

  @Nested
  @DisplayName("hashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I64);

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);

      final int hash1 = funcType.hashCode();
      final int hash2 = funcType.hashCode();

      assertEquals(hash1, hash2, "hashCode should be consistent");
    }

    @Test
    @DisplayName("equal objects should have equal hash codes")
    void equalObjectsShouldHaveEqualHashCodes() {
      final List<WasmValueType> params1 = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results1 = Arrays.asList(WasmValueType.I64);
      final List<WasmValueType> params2 = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results2 = Arrays.asList(WasmValueType.I64);

      final PanamaFuncType funcType1 = new PanamaFuncType(params1, results1, arena, validHandle);
      final MemorySegment otherHandle = arena.allocate(8);
      final PanamaFuncType funcType2 = new PanamaFuncType(params2, results2, arena, otherHandle);

      assertEquals(
          funcType1.hashCode(), funcType2.hashCode(), "Equal objects should have equal hash codes");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32);
      final List<WasmValueType> results = Arrays.asList(WasmValueType.I64);

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);
      final String str = funcType.toString();

      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("FuncType"), "Should contain FuncType");
      assertTrue(str.contains("params"), "Should contain params");
      assertTrue(str.contains("results"), "Should contain results");
    }

    @Test
    @DisplayName("toString should handle empty params and results")
    void toStringShouldHandleEmptyParamsAndResults() {
      final List<WasmValueType> params = Collections.emptyList();
      final List<WasmValueType> results = Collections.emptyList();

      final PanamaFuncType funcType = new PanamaFuncType(params, results, arena, validHandle);
      final String str = funcType.toString();

      assertNotNull(str, "toString should not be null");
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

        // Create function type with various param/result types
        final List<WasmValueType> params =
            Arrays.asList(
                WasmValueType.I32, WasmValueType.I64, WasmValueType.F32, WasmValueType.F64);
        final List<WasmValueType> results = Arrays.asList(WasmValueType.I32, WasmValueType.I64);

        final PanamaFuncType funcType = new PanamaFuncType(params, results, testArena, handle);

        // Verify all getters work
        assertEquals(4, funcType.getParams().size(), "Should have 4 params");
        assertEquals(2, funcType.getResults().size(), "Should have 2 results");
        assertEquals(WasmTypeKind.FUNCTION, funcType.getKind(), "Kind should be FUNCTION");
        assertSame(handle, funcType.getNativeHandle(), "Handle should match");
        assertSame(testArena, funcType.getArena(), "Arena should match");

        // Verify toString works
        assertDoesNotThrow(funcType::toString, "toString should not throw");

        // Verify hashCode works
        assertDoesNotThrow(funcType::hashCode, "hashCode should not throw");
      }
    }

    @Test
    @DisplayName("Different function signatures should be distinguishable")
    void differentFunctionSignaturesShouldBeDistinguishable() {
      // void -> void
      final PanamaFuncType voidToVoid =
          new PanamaFuncType(Collections.emptyList(), Collections.emptyList(), arena, validHandle);

      // i32 -> i32
      final MemorySegment handle2 = arena.allocate(8);
      final PanamaFuncType i32ToI32 =
          new PanamaFuncType(
              Collections.singletonList(WasmValueType.I32),
              Collections.singletonList(WasmValueType.I32),
              arena,
              handle2);

      // (i32, i64) -> f64
      final MemorySegment handle3 = arena.allocate(8);
      final PanamaFuncType i32I64ToF64 =
          new PanamaFuncType(
              Arrays.asList(WasmValueType.I32, WasmValueType.I64),
              Collections.singletonList(WasmValueType.F64),
              arena,
              handle3);

      assertNotEquals(voidToVoid, i32ToI32, "Different signatures should not be equal");
      assertNotEquals(i32ToI32, i32I64ToF64, "Different signatures should not be equal");
      assertNotEquals(voidToVoid, i32I64ToF64, "Different signatures should not be equal");
    }
  }
}
