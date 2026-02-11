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

import ai.tegmentum.wasmtime4j.type.HeapType;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Comprehensive test suite for the HeapType enum.
 *
 * <p>HeapType represents the different kinds of heap types available in the WebAssembly GC
 * proposal. This includes abstract types like ANY, EQ, and concrete type placeholders. This test
 * verifies the enum structure, values, type hierarchy relationships, and conversion methods.
 */
@DisplayName("HeapType Enum Tests")
class HeapTypeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(HeapType.class.isEnum(), "HeapType should be an enum");
    }

    @Test
    @DisplayName("should have exactly 11 values")
    void shouldHaveExactlyElevenValues() {
      assertEquals(11, HeapType.values().length, "HeapType should have exactly 11 values");
    }

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues() {
      Set<String> expectedValues =
          Set.of(
              "ANY",
              "EQ",
              "I31",
              "STRUCT",
              "ARRAY",
              "FUNC",
              "NOFUNC",
              "EXTERN",
              "NOEXTERN",
              "NONE",
              "CONCRETE");
      Set<String> actualValues =
          Arrays.stream(HeapType.values()).map(Enum::name).collect(Collectors.toSet());
      assertEquals(expectedValues, actualValues, "HeapType should have all expected values");
    }
  }

  // ========================================================================
  // Enum Value Tests
  // ========================================================================

  @Nested
  @DisplayName("Enum Value Tests")
  class EnumValueTests {

    @Test
    @DisplayName("ANY should exist")
    void anyShouldExist() {
      HeapType value = HeapType.ANY;
      assertNotNull(value, "ANY should exist");
      assertEquals("ANY", value.name(), "ANY should have correct name");
    }

    @Test
    @DisplayName("EQ should exist")
    void eqShouldExist() {
      HeapType value = HeapType.EQ;
      assertNotNull(value, "EQ should exist");
      assertEquals("EQ", value.name(), "EQ should have correct name");
    }

    @Test
    @DisplayName("I31 should exist")
    void i31ShouldExist() {
      HeapType value = HeapType.I31;
      assertNotNull(value, "I31 should exist");
      assertEquals("I31", value.name(), "I31 should have correct name");
    }

    @Test
    @DisplayName("STRUCT should exist")
    void structShouldExist() {
      HeapType value = HeapType.STRUCT;
      assertNotNull(value, "STRUCT should exist");
      assertEquals("STRUCT", value.name(), "STRUCT should have correct name");
    }

    @Test
    @DisplayName("ARRAY should exist")
    void arrayShouldExist() {
      HeapType value = HeapType.ARRAY;
      assertNotNull(value, "ARRAY should exist");
      assertEquals("ARRAY", value.name(), "ARRAY should have correct name");
    }

    @Test
    @DisplayName("FUNC should exist")
    void funcShouldExist() {
      HeapType value = HeapType.FUNC;
      assertNotNull(value, "FUNC should exist");
      assertEquals("FUNC", value.name(), "FUNC should have correct name");
    }

    @Test
    @DisplayName("NOFUNC should exist")
    void nofuncShouldExist() {
      HeapType value = HeapType.NOFUNC;
      assertNotNull(value, "NOFUNC should exist");
      assertEquals("NOFUNC", value.name(), "NOFUNC should have correct name");
    }

    @Test
    @DisplayName("EXTERN should exist")
    void externShouldExist() {
      HeapType value = HeapType.EXTERN;
      assertNotNull(value, "EXTERN should exist");
      assertEquals("EXTERN", value.name(), "EXTERN should have correct name");
    }

    @Test
    @DisplayName("NOEXTERN should exist")
    void noexternShouldExist() {
      HeapType value = HeapType.NOEXTERN;
      assertNotNull(value, "NOEXTERN should exist");
      assertEquals("NOEXTERN", value.name(), "NOEXTERN should have correct name");
    }

    @Test
    @DisplayName("NONE should exist")
    void noneShouldExist() {
      HeapType value = HeapType.NONE;
      assertNotNull(value, "NONE should exist");
      assertEquals("NONE", value.name(), "NONE should have correct name");
    }

    @Test
    @DisplayName("CONCRETE should exist")
    void concreteShouldExist() {
      HeapType value = HeapType.CONCRETE;
      assertNotNull(value, "CONCRETE should exist");
      assertEquals("CONCRETE", value.name(), "CONCRETE should have correct name");
    }
  }

  // ========================================================================
  // getWasmName Method Tests
  // ========================================================================

  @Nested
  @DisplayName("getWasmName Method Tests")
  class GetWasmNameMethodTests {

    @Test
    @DisplayName("ANY should have wasm name 'any'")
    void anyShouldHaveWasmNameAny() {
      assertEquals("any", HeapType.ANY.getWasmName(), "ANY should have wasm name 'any'");
    }

    @Test
    @DisplayName("EQ should have wasm name 'eq'")
    void eqShouldHaveWasmNameEq() {
      assertEquals("eq", HeapType.EQ.getWasmName(), "EQ should have wasm name 'eq'");
    }

    @Test
    @DisplayName("I31 should have wasm name 'i31'")
    void i31ShouldHaveWasmNameI31() {
      assertEquals("i31", HeapType.I31.getWasmName(), "I31 should have wasm name 'i31'");
    }

    @Test
    @DisplayName("STRUCT should have wasm name 'struct'")
    void structShouldHaveWasmNameStruct() {
      assertEquals(
          "struct", HeapType.STRUCT.getWasmName(), "STRUCT should have wasm name 'struct'");
    }

    @Test
    @DisplayName("ARRAY should have wasm name 'array'")
    void arrayShouldHaveWasmNameArray() {
      assertEquals("array", HeapType.ARRAY.getWasmName(), "ARRAY should have wasm name 'array'");
    }

    @Test
    @DisplayName("FUNC should have wasm name 'func'")
    void funcShouldHaveWasmNameFunc() {
      assertEquals("func", HeapType.FUNC.getWasmName(), "FUNC should have wasm name 'func'");
    }

    @Test
    @DisplayName("NOFUNC should have wasm name 'nofunc'")
    void nofuncShouldHaveWasmNameNofunc() {
      assertEquals(
          "nofunc", HeapType.NOFUNC.getWasmName(), "NOFUNC should have wasm name 'nofunc'");
    }

    @Test
    @DisplayName("EXTERN should have wasm name 'extern'")
    void externShouldHaveWasmNameExtern() {
      assertEquals(
          "extern", HeapType.EXTERN.getWasmName(), "EXTERN should have wasm name 'extern'");
    }

    @Test
    @DisplayName("NOEXTERN should have wasm name 'noextern'")
    void noexternShouldHaveWasmNameNoextern() {
      assertEquals(
          "noextern", HeapType.NOEXTERN.getWasmName(), "NOEXTERN should have wasm name 'noextern'");
    }

    @Test
    @DisplayName("NONE should have wasm name 'none'")
    void noneShouldHaveWasmNameNone() {
      assertEquals("none", HeapType.NONE.getWasmName(), "NONE should have wasm name 'none'");
    }

    @Test
    @DisplayName("CONCRETE should have wasm name 'concrete'")
    void concreteShouldHaveWasmNameConcrete() {
      assertEquals(
          "concrete", HeapType.CONCRETE.getWasmName(), "CONCRETE should have wasm name 'concrete'");
    }
  }

  // ========================================================================
  // fromWasmName Method Tests
  // ========================================================================

  @Nested
  @DisplayName("fromWasmName Method Tests")
  class FromWasmNameMethodTests {

    @Test
    @DisplayName("fromWasmName('any') should return ANY")
    void fromWasmNameAnyShouldReturnAny() {
      assertEquals(
          HeapType.ANY, HeapType.fromWasmName("any"), "fromWasmName('any') should return ANY");
    }

    @Test
    @DisplayName("fromWasmName('eq') should return EQ")
    void fromWasmNameEqShouldReturnEq() {
      assertEquals(HeapType.EQ, HeapType.fromWasmName("eq"), "fromWasmName('eq') should return EQ");
    }

    @Test
    @DisplayName("fromWasmName('i31') should return I31")
    void fromWasmNameI31ShouldReturnI31() {
      assertEquals(
          HeapType.I31, HeapType.fromWasmName("i31"), "fromWasmName('i31') should return I31");
    }

    @Test
    @DisplayName("fromWasmName('struct') should return STRUCT")
    void fromWasmNameStructShouldReturnStruct() {
      assertEquals(
          HeapType.STRUCT,
          HeapType.fromWasmName("struct"),
          "fromWasmName('struct') should return STRUCT");
    }

    @Test
    @DisplayName("fromWasmName('array') should return ARRAY")
    void fromWasmNameArrayShouldReturnArray() {
      assertEquals(
          HeapType.ARRAY,
          HeapType.fromWasmName("array"),
          "fromWasmName('array') should return ARRAY");
    }

    @Test
    @DisplayName("fromWasmName('func') should return FUNC")
    void fromWasmNameFuncShouldReturnFunc() {
      assertEquals(
          HeapType.FUNC, HeapType.fromWasmName("func"), "fromWasmName('func') should return FUNC");
    }

    @Test
    @DisplayName("fromWasmName should throw IllegalArgumentException for unknown name")
    void fromWasmNameShouldThrowForUnknown() {
      assertThrows(
          IllegalArgumentException.class,
          () -> HeapType.fromWasmName("unknown"),
          "fromWasmName('unknown') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromWasmName should throw IllegalArgumentException for empty string")
    void fromWasmNameShouldThrowForEmpty() {
      assertThrows(
          IllegalArgumentException.class,
          () -> HeapType.fromWasmName(""),
          "fromWasmName('') should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("fromWasmName should throw IllegalArgumentException for null input")
    void fromWasmNameShouldThrowForNullInput() {
      assertThrows(
          IllegalArgumentException.class,
          () -> HeapType.fromWasmName(null),
          "fromWasmName(null) should throw IllegalArgumentException");
    }
  }

  // ========================================================================
  // isAbstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("isAbstract Method Tests")
  class IsAbstractMethodTests {

    @Test
    @DisplayName("ANY should be abstract")
    void anyShouldBeAbstract() {
      assertTrue(HeapType.ANY.isAbstract(), "ANY should be abstract");
    }

    @Test
    @DisplayName("EQ should be abstract")
    void eqShouldBeAbstract() {
      assertTrue(HeapType.EQ.isAbstract(), "EQ should be abstract");
    }

    @Test
    @DisplayName("FUNC should be abstract")
    void funcShouldBeAbstract() {
      assertTrue(HeapType.FUNC.isAbstract(), "FUNC should be abstract");
    }

    @Test
    @DisplayName("EXTERN should be abstract")
    void externShouldBeAbstract() {
      assertTrue(HeapType.EXTERN.isAbstract(), "EXTERN should be abstract");
    }

    @Test
    @DisplayName("CONCRETE should not be abstract")
    void concreteShouldNotBeAbstract() {
      assertFalse(HeapType.CONCRETE.isAbstract(), "CONCRETE should not be abstract");
    }
  }

  // ========================================================================
  // isBottom Method Tests
  // ========================================================================

  @Nested
  @DisplayName("isBottom Method Tests")
  class IsBottomMethodTests {

    @Test
    @DisplayName("NONE should be bottom")
    void noneShouldBeBottom() {
      assertTrue(HeapType.NONE.isBottom(), "NONE should be bottom");
    }

    @Test
    @DisplayName("NOFUNC should be bottom")
    void nofuncShouldBeBottom() {
      assertTrue(HeapType.NOFUNC.isBottom(), "NOFUNC should be bottom");
    }

    @Test
    @DisplayName("NOEXTERN should be bottom")
    void noexternShouldBeBottom() {
      assertTrue(HeapType.NOEXTERN.isBottom(), "NOEXTERN should be bottom");
    }

    @Test
    @DisplayName("ANY should not be bottom")
    void anyShouldNotBeBottom() {
      assertFalse(HeapType.ANY.isBottom(), "ANY should not be bottom");
    }

    @Test
    @DisplayName("FUNC should not be bottom")
    void funcShouldNotBeBottom() {
      assertFalse(HeapType.FUNC.isBottom(), "FUNC should not be bottom");
    }

    @Test
    @DisplayName("CONCRETE should not be bottom")
    void concreteShouldNotBeBottom() {
      assertFalse(HeapType.CONCRETE.isBottom(), "CONCRETE should not be bottom");
    }
  }

  // ========================================================================
  // supportsEquality Method Tests
  // ========================================================================

  @Nested
  @DisplayName("supportsEquality Method Tests")
  class SupportsEqualityMethodTests {

    @Test
    @DisplayName("EQ should support equality")
    void eqShouldSupportEquality() {
      assertTrue(HeapType.EQ.supportsEquality(), "EQ should support equality");
    }

    @Test
    @DisplayName("I31 should support equality")
    void i31ShouldSupportEquality() {
      assertTrue(HeapType.I31.supportsEquality(), "I31 should support equality");
    }

    @Test
    @DisplayName("STRUCT should support equality")
    void structShouldSupportEquality() {
      assertTrue(HeapType.STRUCT.supportsEquality(), "STRUCT should support equality");
    }

    @Test
    @DisplayName("ARRAY should support equality")
    void arrayShouldSupportEquality() {
      assertTrue(HeapType.ARRAY.supportsEquality(), "ARRAY should support equality");
    }

    @Test
    @DisplayName("FUNC should not support equality")
    void funcShouldNotSupportEquality() {
      assertFalse(HeapType.FUNC.supportsEquality(), "FUNC should not support equality");
    }

    @Test
    @DisplayName("EXTERN should not support equality")
    void externShouldNotSupportEquality() {
      assertFalse(HeapType.EXTERN.supportsEquality(), "EXTERN should not support equality");
    }
  }

  // ========================================================================
  // getBottomType Method Tests
  // ========================================================================

  @Nested
  @DisplayName("getBottomType Method Tests")
  class GetBottomTypeMethodTests {

    @Test
    @DisplayName("ANY should have bottom type NONE")
    void anyShouldHaveBottomTypeNone() {
      assertEquals(HeapType.NONE, HeapType.ANY.getBottomType(), "ANY should have bottom type NONE");
    }

    @Test
    @DisplayName("EQ should have bottom type NONE")
    void eqShouldHaveBottomTypeNone() {
      assertEquals(HeapType.NONE, HeapType.EQ.getBottomType(), "EQ should have bottom type NONE");
    }

    @Test
    @DisplayName("FUNC should have bottom type NOFUNC")
    void funcShouldHaveBottomTypeNofunc() {
      assertEquals(
          HeapType.NOFUNC, HeapType.FUNC.getBottomType(), "FUNC should have bottom type NOFUNC");
    }

    @Test
    @DisplayName("EXTERN should have bottom type NOEXTERN")
    void externShouldHaveBottomTypeNoextern() {
      assertEquals(
          HeapType.NOEXTERN,
          HeapType.EXTERN.getBottomType(),
          "EXTERN should have bottom type NOEXTERN");
    }

    @Test
    @DisplayName("NONE should have bottom type NONE (self)")
    void noneShouldHaveBottomTypeNone() {
      assertEquals(
          HeapType.NONE, HeapType.NONE.getBottomType(), "NONE should have bottom type NONE");
    }

    @Test
    @DisplayName("NOFUNC should have bottom type NOFUNC (self)")
    void nofuncShouldHaveBottomTypeNofunc() {
      assertEquals(
          HeapType.NOFUNC,
          HeapType.NOFUNC.getBottomType(),
          "NOFUNC should have bottom type NOFUNC");
    }

    @Test
    @DisplayName("NOEXTERN should have bottom type NOEXTERN (self)")
    void noexternShouldHaveBottomTypeNoextern() {
      assertEquals(
          HeapType.NOEXTERN,
          HeapType.NOEXTERN.getBottomType(),
          "NOEXTERN should have bottom type NOEXTERN");
    }
  }

  // ========================================================================
  // isSubtypeOf Method Tests
  // ========================================================================

  @Nested
  @DisplayName("isSubtypeOf Method Tests")
  class IsSubtypeOfMethodTests {

    @Test
    @DisplayName("EQ should be subtype of ANY")
    void eqShouldBeSubtypeOfAny() {
      assertTrue(HeapType.EQ.isSubtypeOf(HeapType.ANY), "EQ should be subtype of ANY");
    }

    @Test
    @DisplayName("I31 should be subtype of EQ")
    void i31ShouldBeSubtypeOfEq() {
      assertTrue(HeapType.I31.isSubtypeOf(HeapType.EQ), "I31 should be subtype of EQ");
    }

    @Test
    @DisplayName("STRUCT should be subtype of EQ")
    void structShouldBeSubtypeOfEq() {
      assertTrue(HeapType.STRUCT.isSubtypeOf(HeapType.EQ), "STRUCT should be subtype of EQ");
    }

    @Test
    @DisplayName("ARRAY should be subtype of EQ")
    void arrayShouldBeSubtypeOfEq() {
      assertTrue(HeapType.ARRAY.isSubtypeOf(HeapType.EQ), "ARRAY should be subtype of EQ");
    }

    @Test
    @DisplayName("NOFUNC should be subtype of FUNC")
    void nofuncShouldBeSubtypeOfFunc() {
      assertTrue(HeapType.NOFUNC.isSubtypeOf(HeapType.FUNC), "NOFUNC should be subtype of FUNC");
    }

    @Test
    @DisplayName("NOEXTERN should be subtype of EXTERN")
    void noexternShouldBeSubtypeOfExtern() {
      assertTrue(
          HeapType.NOEXTERN.isSubtypeOf(HeapType.EXTERN), "NOEXTERN should be subtype of EXTERN");
    }

    @Test
    @DisplayName("NONE should be subtype of EQ")
    void noneShouldBeSubtypeOfEq() {
      assertTrue(HeapType.NONE.isSubtypeOf(HeapType.EQ), "NONE should be subtype of EQ");
    }

    @Test
    @DisplayName("FUNC should not be subtype of ANY")
    void funcShouldNotBeSubtypeOfAny() {
      assertFalse(HeapType.FUNC.isSubtypeOf(HeapType.ANY), "FUNC should not be subtype of ANY");
    }

    @Test
    @DisplayName("EXTERN should not be subtype of ANY")
    void externShouldNotBeSubtypeOfAny() {
      assertFalse(HeapType.EXTERN.isSubtypeOf(HeapType.ANY), "EXTERN should not be subtype of ANY");
    }

    @Test
    @DisplayName("every type should be subtype of itself")
    void everyTypeShouldBeSubtypeOfItself() {
      for (HeapType type : HeapType.values()) {
        assertTrue(type.isSubtypeOf(type), type.name() + " should be subtype of itself");
      }
    }
  }

  // ========================================================================
  // Round-Trip Tests
  // ========================================================================

  @Nested
  @DisplayName("Round-Trip Conversion Tests")
  class RoundTripTests {

    @Test
    @DisplayName("getWasmName and fromWasmName should be inverses for all values")
    void getWasmNameAndFromWasmNameShouldBeInverses() {
      for (HeapType type : HeapType.values()) {
        String wasmName = type.getWasmName();
        HeapType roundTrip = HeapType.fromWasmName(wasmName);
        assertEquals(
            type, roundTrip, "Round-trip conversion should preserve value for " + type.name());
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
      for (HeapType type : HeapType.values()) {
        assertEquals(type, HeapType.valueOf(type.name()), "valueOf should work for " + type.name());
      }
    }

    @Test
    @DisplayName("valueOf should throw IllegalArgumentException for unknown value")
    void valueOfShouldThrowForUnknownValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> HeapType.valueOf("UNKNOWN"),
          "valueOf should throw for unknown value");
    }

    @Test
    @DisplayName("toString should return wasm name")
    void toStringShouldReturnWasmName() {
      for (HeapType type : HeapType.values()) {
        assertEquals(
            type.getWasmName(),
            type.toString(),
            "toString should return wasm name for " + type.name());
      }
    }
  }

  // ========================================================================
  // Type Hierarchy Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Hierarchy Tests")
  class TypeHierarchyTests {

    @Test
    @DisplayName("internal hierarchy should have ANY at top")
    void internalHierarchyShouldHaveAnyAtTop() {
      // In the internal type hierarchy (any/eq/struct/array/i31/none),
      // ANY is the top type
      assertTrue(HeapType.EQ.isSubtypeOf(HeapType.ANY), "EQ should be subtype of ANY");
      assertTrue(HeapType.I31.isSubtypeOf(HeapType.ANY), "I31 should be subtype of ANY");
      assertTrue(HeapType.STRUCT.isSubtypeOf(HeapType.ANY), "STRUCT should be subtype of ANY");
      assertTrue(HeapType.ARRAY.isSubtypeOf(HeapType.ANY), "ARRAY should be subtype of ANY");
      assertTrue(HeapType.NONE.isSubtypeOf(HeapType.ANY), "NONE should be subtype of ANY");
    }

    @Test
    @DisplayName("func hierarchy should be separate from internal hierarchy")
    void funcHierarchyShouldBeSeparateFromInternalHierarchy() {
      // FUNC and NOFUNC form a separate hierarchy
      assertFalse(HeapType.FUNC.isSubtypeOf(HeapType.ANY), "FUNC should not be subtype of ANY");
      assertFalse(HeapType.FUNC.isSubtypeOf(HeapType.EQ), "FUNC should not be subtype of EQ");
      assertTrue(HeapType.NOFUNC.isSubtypeOf(HeapType.FUNC), "NOFUNC should be subtype of FUNC");
    }

    @Test
    @DisplayName("extern hierarchy should be separate from internal hierarchy")
    void externHierarchyShouldBeSeparateFromInternalHierarchy() {
      // EXTERN and NOEXTERN form a separate hierarchy
      assertFalse(HeapType.EXTERN.isSubtypeOf(HeapType.ANY), "EXTERN should not be subtype of ANY");
      assertFalse(HeapType.EXTERN.isSubtypeOf(HeapType.EQ), "EXTERN should not be subtype of EQ");
      assertTrue(
          HeapType.NOEXTERN.isSubtypeOf(HeapType.EXTERN), "NOEXTERN should be subtype of EXTERN");
    }
  }
}
