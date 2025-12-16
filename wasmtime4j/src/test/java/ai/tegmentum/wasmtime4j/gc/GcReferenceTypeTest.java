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

package ai.tegmentum.wasmtime4j.gc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GcReferenceType} enum.
 *
 * <p>Tests the WebAssembly GC reference type hierarchy including enum values, WebAssembly names,
 * subtyping relationships, and equality comparison support.
 */
@DisplayName("GcReferenceType Tests")
class GcReferenceTypeTest {

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have exactly 8 enum values")
    void shouldHaveExactly8EnumValues() {
      assertEquals(8, GcReferenceType.values().length, "GcReferenceType should have 8 values");
    }

    @Test
    @DisplayName("should have ANY_REF value")
    void shouldHaveAnyRefValue() {
      assertNotNull(GcReferenceType.ANY_REF, "ANY_REF should exist");
      assertNotNull(GcReferenceType.valueOf("ANY_REF"), "valueOf should return ANY_REF");
    }

    @Test
    @DisplayName("should have EQ_REF value")
    void shouldHaveEqRefValue() {
      assertNotNull(GcReferenceType.EQ_REF, "EQ_REF should exist");
      assertNotNull(GcReferenceType.valueOf("EQ_REF"), "valueOf should return EQ_REF");
    }

    @Test
    @DisplayName("should have I31_REF value")
    void shouldHaveI31RefValue() {
      assertNotNull(GcReferenceType.I31_REF, "I31_REF should exist");
      assertNotNull(GcReferenceType.valueOf("I31_REF"), "valueOf should return I31_REF");
    }

    @Test
    @DisplayName("should have STRUCT_REF value")
    void shouldHaveStructRefValue() {
      assertNotNull(GcReferenceType.STRUCT_REF, "STRUCT_REF should exist");
      assertNotNull(GcReferenceType.valueOf("STRUCT_REF"), "valueOf should return STRUCT_REF");
    }

    @Test
    @DisplayName("should have ARRAY_REF value")
    void shouldHaveArrayRefValue() {
      assertNotNull(GcReferenceType.ARRAY_REF, "ARRAY_REF should exist");
      assertNotNull(GcReferenceType.valueOf("ARRAY_REF"), "valueOf should return ARRAY_REF");
    }

    @Test
    @DisplayName("should have NULL_REF value")
    void shouldHaveNullRefValue() {
      assertNotNull(GcReferenceType.NULL_REF, "NULL_REF should exist");
      assertNotNull(GcReferenceType.valueOf("NULL_REF"), "valueOf should return NULL_REF");
    }

    @Test
    @DisplayName("should have NULL_FUNC_REF value")
    void shouldHaveNullFuncRefValue() {
      assertNotNull(GcReferenceType.NULL_FUNC_REF, "NULL_FUNC_REF should exist");
      assertNotNull(
          GcReferenceType.valueOf("NULL_FUNC_REF"), "valueOf should return NULL_FUNC_REF");
    }

    @Test
    @DisplayName("should have NULL_EXTERN_REF value")
    void shouldHaveNullExternRefValue() {
      assertNotNull(GcReferenceType.NULL_EXTERN_REF, "NULL_EXTERN_REF should exist");
      assertNotNull(
          GcReferenceType.valueOf("NULL_EXTERN_REF"), "valueOf should return NULL_EXTERN_REF");
    }
  }

  @Nested
  @DisplayName("WebAssembly Name Tests")
  class WasmNameTests {

    @Test
    @DisplayName("ANY_REF should have wasm name 'anyref'")
    void anyRefShouldHaveWasmName() {
      assertEquals("anyref", GcReferenceType.ANY_REF.getWasmName(), "ANY_REF wasm name mismatch");
    }

    @Test
    @DisplayName("EQ_REF should have wasm name 'eqref'")
    void eqRefShouldHaveWasmName() {
      assertEquals("eqref", GcReferenceType.EQ_REF.getWasmName(), "EQ_REF wasm name mismatch");
    }

    @Test
    @DisplayName("I31_REF should have wasm name 'i31ref'")
    void i31RefShouldHaveWasmName() {
      assertEquals("i31ref", GcReferenceType.I31_REF.getWasmName(), "I31_REF wasm name mismatch");
    }

    @Test
    @DisplayName("STRUCT_REF should have wasm name 'structref'")
    void structRefShouldHaveWasmName() {
      assertEquals(
          "structref", GcReferenceType.STRUCT_REF.getWasmName(), "STRUCT_REF wasm name mismatch");
    }

    @Test
    @DisplayName("ARRAY_REF should have wasm name 'arrayref'")
    void arrayRefShouldHaveWasmName() {
      assertEquals(
          "arrayref", GcReferenceType.ARRAY_REF.getWasmName(), "ARRAY_REF wasm name mismatch");
    }

    @Test
    @DisplayName("NULL_REF should have wasm name 'nullref'")
    void nullRefShouldHaveWasmName() {
      assertEquals(
          "nullref", GcReferenceType.NULL_REF.getWasmName(), "NULL_REF wasm name mismatch");
    }

    @Test
    @DisplayName("NULL_FUNC_REF should have wasm name 'nullfuncref'")
    void nullFuncRefShouldHaveWasmName() {
      assertEquals(
          "nullfuncref",
          GcReferenceType.NULL_FUNC_REF.getWasmName(),
          "NULL_FUNC_REF wasm name mismatch");
    }

    @Test
    @DisplayName("NULL_EXTERN_REF should have wasm name 'nullexternref'")
    void nullExternRefShouldHaveWasmName() {
      assertEquals(
          "nullexternref",
          GcReferenceType.NULL_EXTERN_REF.getWasmName(),
          "NULL_EXTERN_REF wasm name mismatch");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - ANY_REF")
  class AnyRefSubtypingTests {

    @Test
    @DisplayName("ANY_REF should be subtype of itself")
    void anyRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "ANY_REF should be subtype of itself");
    }

    @Test
    @DisplayName("ANY_REF should not be subtype of EQ_REF")
    void anyRefShouldNotBeSubtypeOfEqRef() {
      assertFalse(
          GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "ANY_REF should not be subtype of EQ_REF");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - EQ_REF")
  class EqRefSubtypingTests {

    @Test
    @DisplayName("EQ_REF should be subtype of ANY_REF")
    void eqRefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "EQ_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("EQ_REF should be subtype of itself")
    void eqRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "EQ_REF should be subtype of itself");
    }

    @Test
    @DisplayName("EQ_REF should not be subtype of I31_REF")
    void eqRefShouldNotBeSubtypeOfI31Ref() {
      assertFalse(
          GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.I31_REF),
          "EQ_REF should not be subtype of I31_REF");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - I31_REF")
  class I31RefSubtypingTests {

    @Test
    @DisplayName("I31_REF should be subtype of ANY_REF")
    void i31RefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "I31_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("I31_REF should be subtype of EQ_REF")
    void i31RefShouldBeSubtypeOfEqRef() {
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "I31_REF should be subtype of EQ_REF");
    }

    @Test
    @DisplayName("I31_REF should be subtype of itself")
    void i31RefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.I31_REF),
          "I31_REF should be subtype of itself");
    }

    @Test
    @DisplayName("I31_REF should not be subtype of STRUCT_REF")
    void i31RefShouldNotBeSubtypeOfStructRef() {
      assertFalse(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.STRUCT_REF),
          "I31_REF should not be subtype of STRUCT_REF");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - STRUCT_REF")
  class StructRefSubtypingTests {

    @Test
    @DisplayName("STRUCT_REF should be subtype of ANY_REF")
    void structRefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "STRUCT_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("STRUCT_REF should be subtype of EQ_REF")
    void structRefShouldBeSubtypeOfEqRef() {
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "STRUCT_REF should be subtype of EQ_REF");
    }

    @Test
    @DisplayName("STRUCT_REF should be subtype of itself")
    void structRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.STRUCT_REF),
          "STRUCT_REF should be subtype of itself");
    }

    @Test
    @DisplayName("STRUCT_REF should not be subtype of ARRAY_REF")
    void structRefShouldNotBeSubtypeOfArrayRef() {
      assertFalse(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ARRAY_REF),
          "STRUCT_REF should not be subtype of ARRAY_REF");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - ARRAY_REF")
  class ArrayRefSubtypingTests {

    @Test
    @DisplayName("ARRAY_REF should be subtype of ANY_REF")
    void arrayRefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "ARRAY_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("ARRAY_REF should be subtype of EQ_REF")
    void arrayRefShouldBeSubtypeOfEqRef() {
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "ARRAY_REF should be subtype of EQ_REF");
    }

    @Test
    @DisplayName("ARRAY_REF should be subtype of itself")
    void arrayRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ARRAY_REF),
          "ARRAY_REF should be subtype of itself");
    }

    @Test
    @DisplayName("ARRAY_REF should not be subtype of STRUCT_REF")
    void arrayRefShouldNotBeSubtypeOfStructRef() {
      assertFalse(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.STRUCT_REF),
          "ARRAY_REF should not be subtype of STRUCT_REF");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - NULL_REF")
  class NullRefSubtypingTests {

    @Test
    @DisplayName("NULL_REF should be subtype of ANY_REF")
    void nullRefShouldBeSubtypeOfAnyRef() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("NULL_REF should be subtype of EQ_REF")
    void nullRefShouldBeSubtypeOfEqRef() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "NULL_REF should be subtype of EQ_REF");
    }

    @Test
    @DisplayName("NULL_REF should be subtype of I31_REF")
    void nullRefShouldBeSubtypeOfI31Ref() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.I31_REF),
          "NULL_REF should be subtype of I31_REF");
    }

    @Test
    @DisplayName("NULL_REF should be subtype of STRUCT_REF")
    void nullRefShouldBeSubtypeOfStructRef() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.STRUCT_REF),
          "NULL_REF should be subtype of STRUCT_REF");
    }

    @Test
    @DisplayName("NULL_REF should be subtype of ARRAY_REF")
    void nullRefShouldBeSubtypeOfArrayRef() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ARRAY_REF),
          "NULL_REF should be subtype of ARRAY_REF");
    }

    @Test
    @DisplayName("NULL_REF should be subtype of itself")
    void nullRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.NULL_REF),
          "NULL_REF should be subtype of itself");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - NULL_FUNC_REF")
  class NullFuncRefSubtypingTests {

    @Test
    @DisplayName("NULL_FUNC_REF should be subtype of itself")
    void nullFuncRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.NULL_FUNC_REF.isSubtypeOf(GcReferenceType.NULL_FUNC_REF),
          "NULL_FUNC_REF should be subtype of itself");
    }

    @Test
    @DisplayName("NULL_FUNC_REF should not be subtype of ANY_REF")
    void nullFuncRefShouldNotBeSubtypeOfAnyRef() {
      assertFalse(
          GcReferenceType.NULL_FUNC_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_FUNC_REF should not be subtype of ANY_REF");
    }
  }

  @Nested
  @DisplayName("Subtyping Tests - NULL_EXTERN_REF")
  class NullExternRefSubtypingTests {

    @Test
    @DisplayName("NULL_EXTERN_REF should be subtype of itself")
    void nullExternRefShouldBeSubtypeOfItself() {
      assertTrue(
          GcReferenceType.NULL_EXTERN_REF.isSubtypeOf(GcReferenceType.NULL_EXTERN_REF),
          "NULL_EXTERN_REF should be subtype of itself");
    }

    @Test
    @DisplayName("NULL_EXTERN_REF should not be subtype of ANY_REF")
    void nullExternRefShouldNotBeSubtypeOfAnyRef() {
      assertFalse(
          GcReferenceType.NULL_EXTERN_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_EXTERN_REF should not be subtype of ANY_REF");
    }
  }

  @Nested
  @DisplayName("Equality Support Tests")
  class EqualitySupportTests {

    @Test
    @DisplayName("ANY_REF should not support equality")
    void anyRefShouldNotSupportEquality() {
      assertFalse(
          GcReferenceType.ANY_REF.supportsEquality(), "ANY_REF should not support equality");
    }

    @Test
    @DisplayName("EQ_REF should support equality")
    void eqRefShouldSupportEquality() {
      assertTrue(GcReferenceType.EQ_REF.supportsEquality(), "EQ_REF should support equality");
    }

    @Test
    @DisplayName("I31_REF should support equality")
    void i31RefShouldSupportEquality() {
      assertTrue(
          GcReferenceType.I31_REF.supportsEquality(),
          "I31_REF should support equality (subtype of EQ_REF)");
    }

    @Test
    @DisplayName("STRUCT_REF should support equality")
    void structRefShouldSupportEquality() {
      assertTrue(
          GcReferenceType.STRUCT_REF.supportsEquality(),
          "STRUCT_REF should support equality (subtype of EQ_REF)");
    }

    @Test
    @DisplayName("ARRAY_REF should support equality")
    void arrayRefShouldSupportEquality() {
      assertTrue(
          GcReferenceType.ARRAY_REF.supportsEquality(),
          "ARRAY_REF should support equality (subtype of EQ_REF)");
    }

    @Test
    @DisplayName("NULL_REF should support equality")
    void nullRefShouldSupportEquality() {
      assertTrue(
          GcReferenceType.NULL_REF.supportsEquality(),
          "NULL_REF should support equality (subtype of EQ_REF)");
    }

    @Test
    @DisplayName("NULL_FUNC_REF should not support equality")
    void nullFuncRefShouldNotSupportEquality() {
      assertFalse(
          GcReferenceType.NULL_FUNC_REF.supportsEquality(),
          "NULL_FUNC_REF should not support equality");
    }

    @Test
    @DisplayName("NULL_EXTERN_REF should not support equality")
    void nullExternRefShouldNotSupportEquality() {
      assertFalse(
          GcReferenceType.NULL_EXTERN_REF.supportsEquality(),
          "NULL_EXTERN_REF should not support equality");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString() should return wasm name for ANY_REF")
    void toStringShouldReturnWasmNameForAnyRef() {
      assertEquals(
          "anyref", GcReferenceType.ANY_REF.toString(), "toString should return wasm name");
    }

    @Test
    @DisplayName("toString() should return wasm name for EQ_REF")
    void toStringShouldReturnWasmNameForEqRef() {
      assertEquals("eqref", GcReferenceType.EQ_REF.toString(), "toString should return wasm name");
    }

    @Test
    @DisplayName("toString() should return wasm name for I31_REF")
    void toStringShouldReturnWasmNameForI31Ref() {
      assertEquals(
          "i31ref", GcReferenceType.I31_REF.toString(), "toString should return wasm name");
    }

    @Test
    @DisplayName("toString() should return wasm name for STRUCT_REF")
    void toStringShouldReturnWasmNameForStructRef() {
      assertEquals(
          "structref", GcReferenceType.STRUCT_REF.toString(), "toString should return wasm name");
    }

    @Test
    @DisplayName("toString() should return wasm name for ARRAY_REF")
    void toStringShouldReturnWasmNameForArrayRef() {
      assertEquals(
          "arrayref", GcReferenceType.ARRAY_REF.toString(), "toString should return wasm name");
    }
  }

  @Nested
  @DisplayName("Type Hierarchy Tests")
  class TypeHierarchyTests {

    @Test
    @DisplayName("ALL GC types should be subtypes of ANY_REF except function and extern nulls")
    void allGcTypesShouldBeSubtypesOfAnyRef() {
      assertTrue(
          GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "ANY_REF should be subtype of ANY_REF");
      assertTrue(
          GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "EQ_REF should be subtype of ANY_REF");
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "I31_REF should be subtype of ANY_REF");
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "STRUCT_REF should be subtype of ANY_REF");
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "ARRAY_REF should be subtype of ANY_REF");
      assertTrue(
          GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_REF should be subtype of ANY_REF");
    }

    @Test
    @DisplayName("function and extern null refs should not be subtypes of ANY_REF")
    void funcAndExternNullsShouldNotBeSubtypesOfAnyRef() {
      assertFalse(
          GcReferenceType.NULL_FUNC_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_FUNC_REF should not be subtype of ANY_REF");
      assertFalse(
          GcReferenceType.NULL_EXTERN_REF.isSubtypeOf(GcReferenceType.ANY_REF),
          "NULL_EXTERN_REF should not be subtype of ANY_REF");
    }

    @Test
    @DisplayName("concrete GC types should be subtypes of EQ_REF")
    void concreteGcTypesShouldBeSubtypesOfEqRef() {
      assertTrue(
          GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "I31_REF should be subtype of EQ_REF");
      assertTrue(
          GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "STRUCT_REF should be subtype of EQ_REF");
      assertTrue(
          GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.EQ_REF),
          "ARRAY_REF should be subtype of EQ_REF");
    }
  }
}
