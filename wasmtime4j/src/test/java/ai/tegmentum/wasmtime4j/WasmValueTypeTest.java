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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Unit tests for the WasmValueType enum.
 *
 * <p>Tests verify type properties, classification methods, native code conversion, and subtyping
 * rules for all WebAssembly value types.
 */
@DisplayName("WasmValueType Enum Tests")
class WasmValueTypeTest {

  @Nested
  @DisplayName("Integer Type Tests")
  class IntegerTypeTests {

    @Test
    @DisplayName("I32 should have correct properties")
    void i32ShouldHaveCorrectProperties() {
      assertEquals(4, WasmValueType.I32.getSize(), "I32 size should be 4 bytes");
      assertTrue(WasmValueType.I32.isInteger(), "I32 should be an integer type");
      assertFalse(WasmValueType.I32.isFloat(), "I32 should not be a float type");
      assertFalse(WasmValueType.I32.isReference(), "I32 should not be a reference type");
      assertTrue(WasmValueType.I32.isNumeric(), "I32 should be a numeric type");
      assertFalse(WasmValueType.I32.isVector(), "I32 should not be a vector type");
    }

    @Test
    @DisplayName("I64 should have correct properties")
    void i64ShouldHaveCorrectProperties() {
      assertEquals(8, WasmValueType.I64.getSize(), "I64 size should be 8 bytes");
      assertTrue(WasmValueType.I64.isInteger(), "I64 should be an integer type");
      assertFalse(WasmValueType.I64.isFloat(), "I64 should not be a float type");
      assertFalse(WasmValueType.I64.isReference(), "I64 should not be a reference type");
      assertTrue(WasmValueType.I64.isNumeric(), "I64 should be a numeric type");
    }
  }

  @Nested
  @DisplayName("Float Type Tests")
  class FloatTypeTests {

    @Test
    @DisplayName("F32 should have correct properties")
    void f32ShouldHaveCorrectProperties() {
      assertEquals(4, WasmValueType.F32.getSize(), "F32 size should be 4 bytes");
      assertFalse(WasmValueType.F32.isInteger(), "F32 should not be an integer type");
      assertTrue(WasmValueType.F32.isFloat(), "F32 should be a float type");
      assertFalse(WasmValueType.F32.isReference(), "F32 should not be a reference type");
      assertTrue(WasmValueType.F32.isNumeric(), "F32 should be a numeric type");
    }

    @Test
    @DisplayName("F64 should have correct properties")
    void f64ShouldHaveCorrectProperties() {
      assertEquals(8, WasmValueType.F64.getSize(), "F64 size should be 8 bytes");
      assertFalse(WasmValueType.F64.isInteger(), "F64 should not be an integer type");
      assertTrue(WasmValueType.F64.isFloat(), "F64 should be a float type");
      assertFalse(WasmValueType.F64.isReference(), "F64 should not be a reference type");
      assertTrue(WasmValueType.F64.isNumeric(), "F64 should be a numeric type");
    }
  }

  @Nested
  @DisplayName("Vector Type Tests")
  class VectorTypeTests {

    @Test
    @DisplayName("V128 should have correct properties")
    void v128ShouldHaveCorrectProperties() {
      assertEquals(16, WasmValueType.V128.getSize(), "V128 size should be 16 bytes");
      assertFalse(WasmValueType.V128.isInteger(), "V128 should not be an integer type");
      assertFalse(WasmValueType.V128.isFloat(), "V128 should not be a float type");
      assertFalse(WasmValueType.V128.isReference(), "V128 should not be a reference type");
      assertTrue(WasmValueType.V128.isVector(), "V128 should be a vector type");
      assertFalse(WasmValueType.V128.isNumeric(), "V128 should not be a numeric type");
    }
  }

  @Nested
  @DisplayName("Reference Type Tests")
  class ReferenceTypeTests {

    @Test
    @DisplayName("FUNCREF should have correct properties")
    void funcrefShouldHaveCorrectProperties() {
      assertEquals(-1, WasmValueType.FUNCREF.getSize(), "FUNCREF size should be -1 (reference)");
      assertTrue(WasmValueType.FUNCREF.isReference(), "FUNCREF should be a reference type");
      assertFalse(WasmValueType.FUNCREF.isInteger(), "FUNCREF should not be an integer type");
      assertFalse(WasmValueType.FUNCREF.isFloat(), "FUNCREF should not be a float type");
      assertFalse(
          WasmValueType.FUNCREF.isGcReference(), "FUNCREF should not be a GC reference type");
    }

    @Test
    @DisplayName("EXTERNREF should have correct properties")
    void externrefShouldHaveCorrectProperties() {
      assertEquals(
          -1, WasmValueType.EXTERNREF.getSize(), "EXTERNREF size should be -1 (reference)");
      assertTrue(WasmValueType.EXTERNREF.isReference(), "EXTERNREF should be a reference type");
      assertFalse(
          WasmValueType.EXTERNREF.isGcReference(), "EXTERNREF should not be a GC reference type");
    }
  }

  @Nested
  @DisplayName("GC Reference Type Tests")
  class GcReferenceTypeTests {

    @Test
    @DisplayName("ANYREF should be a GC reference type")
    void anyrefShouldBeGcReference() {
      assertTrue(WasmValueType.ANYREF.isReference(), "ANYREF should be a reference type");
      assertTrue(WasmValueType.ANYREF.isGcReference(), "ANYREF should be a GC reference type");
    }

    @Test
    @DisplayName("EQREF should be a GC reference type")
    void eqrefShouldBeGcReference() {
      assertTrue(WasmValueType.EQREF.isReference(), "EQREF should be a reference type");
      assertTrue(WasmValueType.EQREF.isGcReference(), "EQREF should be a GC reference type");
    }

    @Test
    @DisplayName("I31REF should be a GC reference type")
    void i31refShouldBeGcReference() {
      assertTrue(WasmValueType.I31REF.isReference(), "I31REF should be a reference type");
      assertTrue(WasmValueType.I31REF.isGcReference(), "I31REF should be a GC reference type");
    }

    @Test
    @DisplayName("STRUCTREF should be a GC reference type")
    void structrefShouldBeGcReference() {
      assertTrue(WasmValueType.STRUCTREF.isReference(), "STRUCTREF should be a reference type");
      assertTrue(
          WasmValueType.STRUCTREF.isGcReference(), "STRUCTREF should be a GC reference type");
    }

    @Test
    @DisplayName("ARRAYREF should be a GC reference type")
    void arrayrefShouldBeGcReference() {
      assertTrue(WasmValueType.ARRAYREF.isReference(), "ARRAYREF should be a reference type");
      assertTrue(WasmValueType.ARRAYREF.isGcReference(), "ARRAYREF should be a GC reference type");
    }

    @Test
    @DisplayName("CONTREF should be a GC reference type")
    void contrefShouldBeGcReference() {
      assertTrue(WasmValueType.CONTREF.isReference(), "CONTREF should be a reference type");
      assertTrue(WasmValueType.CONTREF.isGcReference(), "CONTREF should be a GC reference type");
    }

    @Test
    @DisplayName("NULLCONTREF should be a GC reference type")
    void nullcontrefShouldBeGcReference() {
      assertTrue(WasmValueType.NULLCONTREF.isReference(), "NULLCONTREF should be a reference type");
      assertTrue(
          WasmValueType.NULLCONTREF.isGcReference(), "NULLCONTREF should be a GC reference type");
    }

    @Test
    @DisplayName("NULLEXNREF should be a GC reference type")
    void nullexnrefShouldBeGcReference() {
      assertTrue(WasmValueType.NULLEXNREF.isReference(), "NULLEXNREF should be a reference type");
      assertTrue(
          WasmValueType.NULLEXNREF.isGcReference(), "NULLEXNREF should be a GC reference type");
    }

    @Test
    @DisplayName("EXNREF should be a reference type")
    void exnrefShouldBeReferenceType() {
      assertTrue(WasmValueType.EXNREF.isReference(), "EXNREF should be a reference type");
    }

    @ParameterizedTest(name = "{0} should be a GC reference type")
    @EnumSource(
        value = WasmValueType.class,
        names = {
          "ANYREF",
          "EQREF",
          "I31REF",
          "STRUCTREF",
          "ARRAYREF",
          "CONTREF",
          "NULLCONTREF",
          "NULLEXNREF"
        })
    @DisplayName("All GC reference types should return true for isGcReference")
    void gcReferenceTypesShouldReturnTrue(WasmValueType type) {
      assertTrue(type.isReference(), type.name() + " should be a reference type");
      assertTrue(type.isGcReference(), type.name() + " should be a GC reference type");
    }
  }

  @Nested
  @DisplayName("Nullable Reference Type Tests")
  class NullableReferenceTypeTests {

    @Test
    @DisplayName("NULLREF should be a nullable reference type")
    void nullrefShouldBeNullable() {
      assertTrue(WasmValueType.NULLREF.isReference(), "NULLREF should be a reference type");
      assertTrue(WasmValueType.NULLREF.isNullableReference(), "NULLREF should be nullable");
    }

    @Test
    @DisplayName("NULLFUNCREF should be a nullable reference type")
    void nullfuncrefShouldBeNullable() {
      assertTrue(WasmValueType.NULLFUNCREF.isReference(), "NULLFUNCREF should be a reference type");
      assertTrue(WasmValueType.NULLFUNCREF.isNullableReference(), "NULLFUNCREF should be nullable");
    }

    @Test
    @DisplayName("NULLEXTERNREF should be a nullable reference type")
    void nullexternrefShouldBeNullable() {
      assertTrue(
          WasmValueType.NULLEXTERNREF.isReference(), "NULLEXTERNREF should be a reference type");
      assertTrue(
          WasmValueType.NULLEXTERNREF.isNullableReference(), "NULLEXTERNREF should be nullable");
    }

    @Test
    @DisplayName("NULLEXNREF should be a nullable reference type")
    void nullexnrefShouldBeNullable() {
      assertTrue(WasmValueType.NULLEXNREF.isReference(), "NULLEXNREF should be a reference type");
      assertTrue(WasmValueType.NULLEXNREF.isNullableReference(), "NULLEXNREF should be nullable");
    }

    @Test
    @DisplayName("NULLCONTREF should be a nullable reference type")
    void nullcontrefShouldBeNullable() {
      assertTrue(WasmValueType.NULLCONTREF.isReference(), "NULLCONTREF should be a reference type");
      assertTrue(WasmValueType.NULLCONTREF.isNullableReference(), "NULLCONTREF should be nullable");
    }

    @Test
    @DisplayName("Non-null types should not be nullable")
    void nonNullTypesShouldNotBeNullable() {
      assertFalse(WasmValueType.I32.isNullableReference(), "I32 should not be nullable");
      assertFalse(WasmValueType.FUNCREF.isNullableReference(), "FUNCREF should not be nullable");
      assertFalse(WasmValueType.ANYREF.isNullableReference(), "ANYREF should not be nullable");
    }

    @ParameterizedTest(name = "{0} should be a nullable reference type")
    @EnumSource(
        value = WasmValueType.class,
        names = {"NULLREF", "NULLFUNCREF", "NULLEXTERNREF", "NULLEXNREF", "NULLCONTREF"})
    @DisplayName("All nullable reference types should return true for isNullableReference")
    void nullableReferenceTypesShouldReturnTrue(WasmValueType type) {
      assertTrue(type.isReference(), type.name() + " should be a reference type");
      assertTrue(type.isNullableReference(), type.name() + " should be nullable");
    }
  }

  @Nested
  @DisplayName("Native Type Code Conversion Tests")
  class NativeTypeCodeConversionTests {

    @Test
    @DisplayName("should convert I32 to and from native code")
    void shouldConvertI32() {
      assertEquals(0, WasmValueType.I32.toNativeTypeCode(), "I32 native code should be 0");
      assertEquals(WasmValueType.I32, WasmValueType.fromNativeTypeCode(0), "Code 0 should be I32");
    }

    @Test
    @DisplayName("should convert I64 to and from native code")
    void shouldConvertI64() {
      assertEquals(1, WasmValueType.I64.toNativeTypeCode(), "I64 native code should be 1");
      assertEquals(WasmValueType.I64, WasmValueType.fromNativeTypeCode(1), "Code 1 should be I64");
    }

    @Test
    @DisplayName("should convert F32 to and from native code")
    void shouldConvertF32() {
      assertEquals(2, WasmValueType.F32.toNativeTypeCode(), "F32 native code should be 2");
      assertEquals(WasmValueType.F32, WasmValueType.fromNativeTypeCode(2), "Code 2 should be F32");
    }

    @Test
    @DisplayName("should convert F64 to and from native code")
    void shouldConvertF64() {
      assertEquals(3, WasmValueType.F64.toNativeTypeCode(), "F64 native code should be 3");
      assertEquals(WasmValueType.F64, WasmValueType.fromNativeTypeCode(3), "Code 3 should be F64");
    }

    @Test
    @DisplayName("should convert V128 to and from native code")
    void shouldConvertV128() {
      assertEquals(4, WasmValueType.V128.toNativeTypeCode(), "V128 native code should be 4");
      assertEquals(
          WasmValueType.V128, WasmValueType.fromNativeTypeCode(4), "Code 4 should be V128");
    }

    @Test
    @DisplayName("should convert reference types to and from native code")
    void shouldConvertReferenceTypes() {
      assertEquals(5, WasmValueType.FUNCREF.toNativeTypeCode(), "FUNCREF native code should be 5");
      assertEquals(
          WasmValueType.FUNCREF, WasmValueType.fromNativeTypeCode(5), "Code 5 should be FUNCREF");

      assertEquals(
          6, WasmValueType.EXTERNREF.toNativeTypeCode(), "EXTERNREF native code should be 6");
      assertEquals(
          WasmValueType.EXTERNREF,
          WasmValueType.fromNativeTypeCode(6),
          "Code 6 should be EXTERNREF");
    }

    @Test
    @DisplayName("should convert new types to and from native code")
    void shouldConvertNewTypes() {
      assertEquals(
          16, WasmValueType.NULLEXNREF.toNativeTypeCode(), "NULLEXNREF native code should be 16");
      assertEquals(
          WasmValueType.NULLEXNREF,
          WasmValueType.fromNativeTypeCode(16),
          "Code 16 should be NULLEXNREF");

      assertEquals(
          17, WasmValueType.CONTREF.toNativeTypeCode(), "CONTREF native code should be 17");
      assertEquals(
          WasmValueType.CONTREF, WasmValueType.fromNativeTypeCode(17), "Code 17 should be CONTREF");

      assertEquals(
          18, WasmValueType.NULLCONTREF.toNativeTypeCode(), "NULLCONTREF native code should be 18");
      assertEquals(
          WasmValueType.NULLCONTREF,
          WasmValueType.fromNativeTypeCode(18),
          "Code 18 should be NULLCONTREF");
    }

    @Test
    @DisplayName("should throw for unknown native type code")
    void shouldThrowForUnknownCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValueType.fromNativeTypeCode(999),
          "Should throw for unknown type code");
    }

    @Test
    @DisplayName("should round-trip all value types")
    void shouldRoundTripAllTypes() {
      for (WasmValueType type : WasmValueType.values()) {
        final int code = type.toNativeTypeCode();
        final WasmValueType roundTripped = WasmValueType.fromNativeTypeCode(code);
        assertEquals(type, roundTripped, "Round-trip should preserve type: " + type);
      }
    }

    @ParameterizedTest(name = "{0} should round-trip through native type code conversion")
    @EnumSource(WasmValueType.class)
    @DisplayName("All WasmValueType values should round-trip through native type code")
    void allTypesShouldRoundTripThroughNativeCode(WasmValueType type) {
      int code = type.toNativeTypeCode();
      WasmValueType roundTripped = WasmValueType.fromNativeTypeCode(code);
      assertEquals(
          type, roundTripped, type.name() + " should round-trip through native code " + code);
    }

    @ParameterizedTest(name = "{0} should have native type code {1}")
    @CsvSource({"I32, 0", "I64, 1", "F32, 2", "F64, 3", "V128, 4", "FUNCREF, 5", "EXTERNREF, 6"})
    @DisplayName("Core types should have well-known native type codes")
    void coreTypesShouldHaveWellKnownCodes(String typeName, int expectedCode) {
      WasmValueType type = WasmValueType.valueOf(typeName);
      assertEquals(
          expectedCode,
          type.toNativeTypeCode(),
          typeName + " should have native code " + expectedCode);
    }
  }

  @Nested
  @DisplayName("Subtyping Tests")
  class SubtypingTests {

    @Test
    @DisplayName("types should be subtypes of themselves")
    void typesShouldBeSubtypesOfThemselves() {
      for (WasmValueType type : WasmValueType.values()) {
        assertTrue(type.isSubtypeOf(type), type + " should be subtype of itself");
      }
    }

    @Test
    @DisplayName("EQREF should be subtype of ANYREF")
    void eqrefShouldBeSubtypeOfAnyref() {
      assertTrue(
          WasmValueType.EQREF.isSubtypeOf(WasmValueType.ANYREF),
          "EQREF should be subtype of ANYREF");
    }

    @Test
    @DisplayName("I31REF should be subtype of EQREF and ANYREF")
    void i31refShouldBeSubtypeOfEqrefAndAnyref() {
      assertTrue(
          WasmValueType.I31REF.isSubtypeOf(WasmValueType.EQREF),
          "I31REF should be subtype of EQREF");
      assertTrue(
          WasmValueType.I31REF.isSubtypeOf(WasmValueType.ANYREF),
          "I31REF should be subtype of ANYREF");
    }

    @Test
    @DisplayName("STRUCTREF should be subtype of EQREF and ANYREF")
    void structrefShouldBeSubtypeOfEqrefAndAnyref() {
      assertTrue(
          WasmValueType.STRUCTREF.isSubtypeOf(WasmValueType.EQREF),
          "STRUCTREF should be subtype of EQREF");
      assertTrue(
          WasmValueType.STRUCTREF.isSubtypeOf(WasmValueType.ANYREF),
          "STRUCTREF should be subtype of ANYREF");
    }

    @Test
    @DisplayName("ARRAYREF should be subtype of EQREF and ANYREF")
    void arrayrefShouldBeSubtypeOfEqrefAndAnyref() {
      assertTrue(
          WasmValueType.ARRAYREF.isSubtypeOf(WasmValueType.EQREF),
          "ARRAYREF should be subtype of EQREF");
      assertTrue(
          WasmValueType.ARRAYREF.isSubtypeOf(WasmValueType.ANYREF),
          "ARRAYREF should be subtype of ANYREF");
    }

    @Test
    @DisplayName("NULLREF should be subtype of all reference types")
    void nullrefShouldBeSubtypeOfAllReferenceTypes() {
      assertTrue(
          WasmValueType.NULLREF.isSubtypeOf(WasmValueType.ANYREF),
          "NULLREF should be subtype of ANYREF");
      assertTrue(
          WasmValueType.NULLREF.isSubtypeOf(WasmValueType.FUNCREF),
          "NULLREF should be subtype of FUNCREF");
      assertTrue(
          WasmValueType.NULLREF.isSubtypeOf(WasmValueType.EXTERNREF),
          "NULLREF should be subtype of EXTERNREF");
    }

    @Test
    @DisplayName("NULLFUNCREF should be subtype of FUNCREF")
    void nullfuncrefShouldBeSubtypeOfFuncref() {
      assertTrue(
          WasmValueType.NULLFUNCREF.isSubtypeOf(WasmValueType.FUNCREF),
          "NULLFUNCREF should be subtype of FUNCREF");
    }

    @Test
    @DisplayName("NULLEXTERNREF should be subtype of EXTERNREF")
    void nullexternrefShouldBeSubtypeOfExternref() {
      assertTrue(
          WasmValueType.NULLEXTERNREF.isSubtypeOf(WasmValueType.EXTERNREF),
          "NULLEXTERNREF should be subtype of EXTERNREF");
    }

    @Test
    @DisplayName("NULLEXNREF should be subtype of EXNREF")
    void nullexnrefShouldBeSubtypeOfExnref() {
      assertTrue(
          WasmValueType.NULLEXNREF.isSubtypeOf(WasmValueType.EXNREF),
          "NULLEXNREF should be subtype of EXNREF");
    }

    @Test
    @DisplayName("NULLCONTREF should be subtype of CONTREF")
    void nullcontrefShouldBeSubtypeOfContref() {
      assertTrue(
          WasmValueType.NULLCONTREF.isSubtypeOf(WasmValueType.CONTREF),
          "NULLCONTREF should be subtype of CONTREF");
    }

    @Test
    @DisplayName("CONTREF should not be subtype of ANYREF")
    void contrefShouldNotBeSubtypeOfAnyref() {
      assertFalse(
          WasmValueType.CONTREF.isSubtypeOf(WasmValueType.ANYREF),
          "CONTREF should not be subtype of ANYREF");
    }

    @Test
    @DisplayName("CONTREF should not be subtype of FUNCREF")
    void contrefShouldNotBeSubtypeOfFuncref() {
      assertFalse(
          WasmValueType.CONTREF.isSubtypeOf(WasmValueType.FUNCREF),
          "CONTREF should not be subtype of FUNCREF");
    }

    @Test
    @DisplayName("numeric types should not be subtypes of each other")
    void numericTypesShouldNotBeSubtypesOfEachOther() {
      assertFalse(
          WasmValueType.I32.isSubtypeOf(WasmValueType.I64), "I32 should not be subtype of I64");
      assertFalse(
          WasmValueType.F32.isSubtypeOf(WasmValueType.F64), "F32 should not be subtype of F64");
      assertFalse(
          WasmValueType.I32.isSubtypeOf(WasmValueType.F32), "I32 should not be subtype of F32");
    }

    @ParameterizedTest(name = "{0} should be a subtype of itself")
    @EnumSource(WasmValueType.class)
    @DisplayName("All types should be subtypes of themselves (reflexive)")
    void allTypesShouldBeSubtypesOfThemselves(WasmValueType type) {
      assertTrue(type.isSubtypeOf(type), type.name() + " should be subtype of itself");
    }

    @ParameterizedTest(name = "{0} should be a subtype of ANYREF")
    @EnumSource(
        value = WasmValueType.class,
        names = {"ANYREF", "EQREF", "I31REF", "STRUCTREF", "ARRAYREF", "NULLREF"})
    @DisplayName("Types in the ANY hierarchy should be subtypes of ANYREF")
    void anyHierarchySubtypes(WasmValueType type) {
      assertTrue(
          type.isSubtypeOf(WasmValueType.ANYREF), type.name() + " should be subtype of ANYREF");
    }
  }
}
