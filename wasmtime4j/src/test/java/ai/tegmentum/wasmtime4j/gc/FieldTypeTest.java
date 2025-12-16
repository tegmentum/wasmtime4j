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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FieldType} class.
 *
 * <p>Tests the WebAssembly GC field type representation including factory methods, type kinds,
 * reference types, size calculation, compatibility checks, and conversions.
 */
@DisplayName("FieldType Tests")
class FieldTypeTest {

  @Nested
  @DisplayName("Factory Methods Tests")
  class FactoryMethodsTests {

    @Test
    @DisplayName("i32() should create I32 field type")
    void i32ShouldCreateI32FieldType() {
      FieldType fieldType = FieldType.i32();
      assertEquals(FieldType.ValueTypeKind.I32, fieldType.getKind(), "Kind should be I32");
      assertFalse(fieldType.isReference(), "i32 should not be a reference");
      assertFalse(fieldType.isPacked(), "i32 should not be packed");
    }

    @Test
    @DisplayName("i64() should create I64 field type")
    void i64ShouldCreateI64FieldType() {
      FieldType fieldType = FieldType.i64();
      assertEquals(FieldType.ValueTypeKind.I64, fieldType.getKind(), "Kind should be I64");
      assertFalse(fieldType.isReference(), "i64 should not be a reference");
      assertFalse(fieldType.isPacked(), "i64 should not be packed");
    }

    @Test
    @DisplayName("f32() should create F32 field type")
    void f32ShouldCreateF32FieldType() {
      FieldType fieldType = FieldType.f32();
      assertEquals(FieldType.ValueTypeKind.F32, fieldType.getKind(), "Kind should be F32");
      assertFalse(fieldType.isReference(), "f32 should not be a reference");
      assertFalse(fieldType.isPacked(), "f32 should not be packed");
    }

    @Test
    @DisplayName("f64() should create F64 field type")
    void f64ShouldCreateF64FieldType() {
      FieldType fieldType = FieldType.f64();
      assertEquals(FieldType.ValueTypeKind.F64, fieldType.getKind(), "Kind should be F64");
      assertFalse(fieldType.isReference(), "f64 should not be a reference");
      assertFalse(fieldType.isPacked(), "f64 should not be packed");
    }

    @Test
    @DisplayName("v128() should create V128 field type")
    void v128ShouldCreateV128FieldType() {
      FieldType fieldType = FieldType.v128();
      assertEquals(FieldType.ValueTypeKind.V128, fieldType.getKind(), "Kind should be V128");
      assertFalse(fieldType.isReference(), "v128 should not be a reference");
      assertFalse(fieldType.isPacked(), "v128 should not be packed");
    }

    @Test
    @DisplayName("packedI8() should create PACKED_I8 field type")
    void packedI8ShouldCreatePackedI8FieldType() {
      FieldType fieldType = FieldType.packedI8();
      assertEquals(
          FieldType.ValueTypeKind.PACKED_I8, fieldType.getKind(), "Kind should be PACKED_I8");
      assertFalse(fieldType.isReference(), "packed i8 should not be a reference");
      assertTrue(fieldType.isPacked(), "packed i8 should be packed");
    }

    @Test
    @DisplayName("packedI16() should create PACKED_I16 field type")
    void packedI16ShouldCreatePackedI16FieldType() {
      FieldType fieldType = FieldType.packedI16();
      assertEquals(
          FieldType.ValueTypeKind.PACKED_I16, fieldType.getKind(), "Kind should be PACKED_I16");
      assertFalse(fieldType.isReference(), "packed i16 should not be a reference");
      assertTrue(fieldType.isPacked(), "packed i16 should be packed");
    }

    @Test
    @DisplayName("reference() should create reference field type")
    void referenceShouldCreateReferenceFieldType() {
      FieldType fieldType = FieldType.reference(GcReferenceType.ANY_REF, true);
      assertEquals(
          FieldType.ValueTypeKind.REFERENCE, fieldType.getKind(), "Kind should be REFERENCE");
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertTrue(fieldType.isNullable(), "Should be nullable");
      assertEquals(
          GcReferenceType.ANY_REF, fieldType.getReferenceType(), "Reference type should match");
    }

    @Test
    @DisplayName("reference() without nullable should create non-nullable reference")
    void referenceWithoutNullableShouldCreateNonNullable() {
      FieldType fieldType = FieldType.reference(GcReferenceType.STRUCT_REF);
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertFalse(fieldType.isNullable(), "Should not be nullable by default");
    }

    @Test
    @DisplayName("reference() should throw for null reference type")
    void referenceShouldThrowForNullType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> FieldType.reference(null, true),
          "reference should throw for null reference type");
    }

    @Test
    @DisplayName("anyRef() should create nullable anyref")
    void anyRefShouldCreateNullableAnyRef() {
      FieldType fieldType = FieldType.anyRef();
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertTrue(fieldType.isNullable(), "anyRef should be nullable");
      assertEquals(GcReferenceType.ANY_REF, fieldType.getReferenceType(), "Type should be ANY_REF");
    }

    @Test
    @DisplayName("eqRef() should create nullable eqref")
    void eqRefShouldCreateNullableEqRef() {
      FieldType fieldType = FieldType.eqRef();
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertTrue(fieldType.isNullable(), "eqRef should be nullable");
      assertEquals(GcReferenceType.EQ_REF, fieldType.getReferenceType(), "Type should be EQ_REF");
    }

    @Test
    @DisplayName("i31Ref() should create nullable i31ref")
    void i31RefShouldCreateNullableI31Ref() {
      FieldType fieldType = FieldType.i31Ref();
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertTrue(fieldType.isNullable(), "i31Ref should be nullable");
      assertEquals(GcReferenceType.I31_REF, fieldType.getReferenceType(), "Type should be I31_REF");
    }

    @Test
    @DisplayName("structRef() should create nullable structref")
    void structRefShouldCreateNullableStructRef() {
      FieldType fieldType = FieldType.structRef();
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertTrue(fieldType.isNullable(), "structRef should be nullable");
      assertEquals(
          GcReferenceType.STRUCT_REF, fieldType.getReferenceType(), "Type should be STRUCT_REF");
    }

    @Test
    @DisplayName("arrayRef() should create nullable arrayref")
    void arrayRefShouldCreateNullableArrayRef() {
      FieldType fieldType = FieldType.arrayRef();
      assertTrue(fieldType.isReference(), "Should be a reference");
      assertTrue(fieldType.isNullable(), "arrayRef should be nullable");
      assertEquals(
          GcReferenceType.ARRAY_REF, fieldType.getReferenceType(), "Type should be ARRAY_REF");
    }
  }

  @Nested
  @DisplayName("Size Tests")
  class SizeTests {

    @Test
    @DisplayName("i32 should be 4 bytes")
    void i32ShouldBe4Bytes() {
      assertEquals(4, FieldType.i32().getSizeBytes(), "i32 should be 4 bytes");
    }

    @Test
    @DisplayName("i64 should be 8 bytes")
    void i64ShouldBe8Bytes() {
      assertEquals(8, FieldType.i64().getSizeBytes(), "i64 should be 8 bytes");
    }

    @Test
    @DisplayName("f32 should be 4 bytes")
    void f32ShouldBe4Bytes() {
      assertEquals(4, FieldType.f32().getSizeBytes(), "f32 should be 4 bytes");
    }

    @Test
    @DisplayName("f64 should be 8 bytes")
    void f64ShouldBe8Bytes() {
      assertEquals(8, FieldType.f64().getSizeBytes(), "f64 should be 8 bytes");
    }

    @Test
    @DisplayName("v128 should be 16 bytes")
    void v128ShouldBe16Bytes() {
      assertEquals(16, FieldType.v128().getSizeBytes(), "v128 should be 16 bytes");
    }

    @Test
    @DisplayName("packed i8 should be 1 byte")
    void packedI8ShouldBe1Byte() {
      assertEquals(1, FieldType.packedI8().getSizeBytes(), "packed i8 should be 1 byte");
    }

    @Test
    @DisplayName("packed i16 should be 2 bytes")
    void packedI16ShouldBe2Bytes() {
      assertEquals(2, FieldType.packedI16().getSizeBytes(), "packed i16 should be 2 bytes");
    }

    @Test
    @DisplayName("reference should be 8 bytes")
    void referenceShouldBe8Bytes() {
      assertEquals(8, FieldType.anyRef().getSizeBytes(), "reference should be 8 bytes (pointer)");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getKind() should return correct kind")
    void getKindShouldReturnCorrectKind() {
      assertEquals(FieldType.ValueTypeKind.I32, FieldType.i32().getKind());
      assertEquals(FieldType.ValueTypeKind.I64, FieldType.i64().getKind());
      assertEquals(FieldType.ValueTypeKind.F32, FieldType.f32().getKind());
      assertEquals(FieldType.ValueTypeKind.F64, FieldType.f64().getKind());
      assertEquals(FieldType.ValueTypeKind.V128, FieldType.v128().getKind());
      assertEquals(FieldType.ValueTypeKind.PACKED_I8, FieldType.packedI8().getKind());
      assertEquals(FieldType.ValueTypeKind.PACKED_I16, FieldType.packedI16().getKind());
      assertEquals(FieldType.ValueTypeKind.REFERENCE, FieldType.anyRef().getKind());
    }

    @Test
    @DisplayName("getReferenceType() should return null for non-reference types")
    void getReferenceTypeShouldReturnNullForNonReference() {
      assertNull(FieldType.i32().getReferenceType(), "i32 should have null reference type");
      assertNull(FieldType.f64().getReferenceType(), "f64 should have null reference type");
      assertNull(FieldType.packedI8().getReferenceType(), "packed i8 should have null ref type");
    }

    @Test
    @DisplayName("getReferenceType() should return reference type for references")
    void getReferenceTypeShouldReturnTypeForReference() {
      assertEquals(
          GcReferenceType.ANY_REF,
          FieldType.anyRef().getReferenceType(),
          "anyRef should return ANY_REF");
      assertEquals(
          GcReferenceType.EQ_REF,
          FieldType.eqRef().getReferenceType(),
          "eqRef should return EQ_REF");
      assertEquals(
          GcReferenceType.STRUCT_REF,
          FieldType.structRef().getReferenceType(),
          "structRef should return STRUCT_REF");
    }

    @Test
    @DisplayName("isReference() should correctly identify references")
    void isReferenceShouldCorrectlyIdentifyReferences() {
      assertFalse(FieldType.i32().isReference(), "i32 is not a reference");
      assertFalse(FieldType.f64().isReference(), "f64 is not a reference");
      assertFalse(FieldType.packedI8().isReference(), "packed i8 is not a reference");
      assertTrue(FieldType.anyRef().isReference(), "anyRef is a reference");
      assertTrue(FieldType.structRef().isReference(), "structRef is a reference");
    }

    @Test
    @DisplayName("isNullable() should correctly identify nullability")
    void isNullableShouldCorrectlyIdentifyNullability() {
      assertFalse(FieldType.i32().isNullable(), "i32 is not nullable");
      assertTrue(FieldType.anyRef().isNullable(), "anyRef is nullable");
      assertFalse(
          FieldType.reference(GcReferenceType.STRUCT_REF).isNullable(),
          "non-nullable reference should not be nullable");
    }

    @Test
    @DisplayName("isPacked() should correctly identify packed types")
    void isPackedShouldCorrectlyIdentifyPackedTypes() {
      assertFalse(FieldType.i32().isPacked(), "i32 is not packed");
      assertFalse(FieldType.i64().isPacked(), "i64 is not packed");
      assertFalse(FieldType.anyRef().isPacked(), "anyRef is not packed");
      assertTrue(FieldType.packedI8().isPacked(), "packed i8 is packed");
      assertTrue(FieldType.packedI16().isPacked(), "packed i16 is packed");
    }
  }

  @Nested
  @DisplayName("WasmValueType Conversion Tests")
  class WasmValueTypeConversionTests {

    @Test
    @DisplayName("toWasmValueType() should convert i32 correctly")
    void toWasmValueTypeShouldConvertI32() {
      assertEquals(
          WasmValueType.I32, FieldType.i32().toWasmValueType(), "i32 should convert to I32");
    }

    @Test
    @DisplayName("toWasmValueType() should convert i64 correctly")
    void toWasmValueTypeShouldConvertI64() {
      assertEquals(
          WasmValueType.I64, FieldType.i64().toWasmValueType(), "i64 should convert to I64");
    }

    @Test
    @DisplayName("toWasmValueType() should convert f32 correctly")
    void toWasmValueTypeShouldConvertF32() {
      assertEquals(
          WasmValueType.F32, FieldType.f32().toWasmValueType(), "f32 should convert to F32");
    }

    @Test
    @DisplayName("toWasmValueType() should convert f64 correctly")
    void toWasmValueTypeShouldConvertF64() {
      assertEquals(
          WasmValueType.F64, FieldType.f64().toWasmValueType(), "f64 should convert to F64");
    }

    @Test
    @DisplayName("toWasmValueType() should convert v128 correctly")
    void toWasmValueTypeShouldConvertV128() {
      assertEquals(
          WasmValueType.V128, FieldType.v128().toWasmValueType(), "v128 should convert to V128");
    }

    @Test
    @DisplayName("toWasmValueType() should convert packed i8 to I32")
    void toWasmValueTypeShouldConvertPackedI8ToI32() {
      assertEquals(
          WasmValueType.I32,
          FieldType.packedI8().toWasmValueType(),
          "packed i8 should convert to I32");
    }

    @Test
    @DisplayName("toWasmValueType() should convert packed i16 to I32")
    void toWasmValueTypeShouldConvertPackedI16ToI32() {
      assertEquals(
          WasmValueType.I32,
          FieldType.packedI16().toWasmValueType(),
          "packed i16 should convert to I32");
    }

    @Test
    @DisplayName("toWasmValueType() should throw for reference types")
    void toWasmValueTypeShouldThrowForReference() {
      assertThrows(
          IllegalStateException.class,
          () -> FieldType.anyRef().toWasmValueType(),
          "toWasmValueType should throw for reference types");
    }
  }

  @Nested
  @DisplayName("Compatibility Tests")
  class CompatibilityTests {

    @Test
    @DisplayName("same types should be compatible")
    void sameTypesShouldBeCompatible() {
      assertTrue(
          FieldType.i32().isCompatibleWith(FieldType.i32()), "i32 should be compatible with i32");
      assertTrue(
          FieldType.f64().isCompatibleWith(FieldType.f64()), "f64 should be compatible with f64");
      assertTrue(
          FieldType.anyRef().isCompatibleWith(FieldType.anyRef()),
          "anyRef should be compatible with anyRef");
    }

    @Test
    @DisplayName("different kinds should not be compatible")
    void differentKindsShouldNotBeCompatible() {
      assertFalse(
          FieldType.i32().isCompatibleWith(FieldType.f64()),
          "i32 should not be compatible with f64");
      assertFalse(
          FieldType.i64().isCompatibleWith(FieldType.v128()),
          "i64 should not be compatible with v128");
    }

    @Test
    @DisplayName("reference subtypes should be compatible")
    void referenceSubtypesShouldBeCompatible() {
      FieldType eqRefType = FieldType.eqRef();
      FieldType anyRefType = FieldType.anyRef();

      // EQ_REF is subtype of ANY_REF
      assertTrue(
          eqRefType.isCompatibleWith(anyRefType),
          "EQ_REF reference should be compatible with ANY_REF");
    }

    @Test
    @DisplayName("non-nullable should be compatible with nullable")
    void nonNullableShouldBeCompatibleWithNullable() {
      FieldType nonNullable = FieldType.reference(GcReferenceType.STRUCT_REF, false);
      FieldType nullable = FieldType.reference(GcReferenceType.STRUCT_REF, true);

      assertTrue(
          nonNullable.isCompatibleWith(nullable),
          "Non-nullable should be compatible with nullable");
    }

    @Test
    @DisplayName("nullable should not be compatible with non-nullable")
    void nullableShouldNotBeCompatibleWithNonNullable() {
      FieldType nonNullable = FieldType.reference(GcReferenceType.STRUCT_REF, false);
      FieldType nullable = FieldType.reference(GcReferenceType.STRUCT_REF, true);

      assertFalse(
          nullable.isCompatibleWith(nonNullable),
          "Nullable should not be compatible with non-nullable");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("equals() should return true for same type")
    void equalsShouldReturnTrueForSameType() {
      assertEquals(FieldType.i32(), FieldType.i32(), "Same type should be equal");
      assertEquals(FieldType.anyRef(), FieldType.anyRef(), "Same type should be equal");
    }

    @Test
    @DisplayName("equals() should return false for different types")
    void equalsShouldReturnFalseForDifferentTypes() {
      assertNotEquals(FieldType.i32(), FieldType.i64(), "Different types should not be equal");
      assertNotEquals(FieldType.f32(), FieldType.f64(), "Different types should not be equal");
    }

    @Test
    @DisplayName("equals() should return false for null")
    void equalsShouldReturnFalseForNull() {
      assertNotEquals(null, FieldType.i32(), "FieldType should not equal null");
    }

    @Test
    @DisplayName("equals() should return false for different class")
    void equalsShouldReturnFalseForDifferentClass() {
      assertNotEquals("string", FieldType.i32(), "FieldType should not equal different class");
    }

    @Test
    @DisplayName("hashCode() should be consistent")
    void hashCodeShouldBeConsistent() {
      FieldType type = FieldType.i32();
      assertEquals(type.hashCode(), type.hashCode(), "hashCode should be consistent");
    }

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      assertEquals(
          FieldType.i32().hashCode(),
          FieldType.i32().hashCode(),
          "Equal types should have same hashCode");
    }

    @Test
    @DisplayName("reference types with different nullability should not be equal")
    void referenceTypesWithDifferentNullabilityShouldNotBeEqual() {
      FieldType nullable = FieldType.reference(GcReferenceType.STRUCT_REF, true);
      FieldType nonNullable = FieldType.reference(GcReferenceType.STRUCT_REF, false);

      assertNotEquals(nullable, nonNullable, "Different nullability should not be equal");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString() for i32 should return lowercase kind name")
    void toStringForI32ShouldReturnLowercaseKindName() {
      assertEquals("i32", FieldType.i32().toString(), "toString should return 'i32'");
    }

    @Test
    @DisplayName("toString() for i64 should return lowercase kind name")
    void toStringForI64ShouldReturnLowercaseKindName() {
      assertEquals("i64", FieldType.i64().toString(), "toString should return 'i64'");
    }

    @Test
    @DisplayName("toString() for f32 should return lowercase kind name")
    void toStringForF32ShouldReturnLowercaseKindName() {
      assertEquals("f32", FieldType.f32().toString(), "toString should return 'f32'");
    }

    @Test
    @DisplayName("toString() for f64 should return lowercase kind name")
    void toStringForF64ShouldReturnLowercaseKindName() {
      assertEquals("f64", FieldType.f64().toString(), "toString should return 'f64'");
    }

    @Test
    @DisplayName("toString() for reference should include wasm name and nullability")
    void toStringForReferenceShouldIncludeWasmNameAndNullability() {
      String nullableStr = FieldType.anyRef().toString();
      assertTrue(
          nullableStr.contains("anyref"), "toString should include wasm name for nullable ref");
      assertTrue(nullableStr.contains("?"), "toString should include '?' for nullable ref");

      String nonNullableStr = FieldType.reference(GcReferenceType.STRUCT_REF).toString();
      assertTrue(nonNullableStr.contains("structref"), "toString should include wasm name for ref");
      assertFalse(
          nonNullableStr.contains("?"), "toString should not include '?' for non-nullable ref");
    }
  }

  @Nested
  @DisplayName("ValueTypeKind Enum Tests")
  class ValueTypeKindEnumTests {

    @Test
    @DisplayName("should have exactly 8 enum values")
    void shouldHaveExactly8EnumValues() {
      assertEquals(
          8,
          FieldType.ValueTypeKind.values().length,
          "ValueTypeKind should have 8 values (I32, I64, F32, F64, V128, PACKED_I8, PACKED_I16,"
              + " REFERENCE)");
    }

    @Test
    @DisplayName("should have I32 value")
    void shouldHaveI32Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("I32");
      assertEquals(FieldType.ValueTypeKind.I32, kind, "valueOf should return I32");
    }

    @Test
    @DisplayName("should have I64 value")
    void shouldHaveI64Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("I64");
      assertEquals(FieldType.ValueTypeKind.I64, kind, "valueOf should return I64");
    }

    @Test
    @DisplayName("should have F32 value")
    void shouldHaveF32Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("F32");
      assertEquals(FieldType.ValueTypeKind.F32, kind, "valueOf should return F32");
    }

    @Test
    @DisplayName("should have F64 value")
    void shouldHaveF64Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("F64");
      assertEquals(FieldType.ValueTypeKind.F64, kind, "valueOf should return F64");
    }

    @Test
    @DisplayName("should have V128 value")
    void shouldHaveV128Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("V128");
      assertEquals(FieldType.ValueTypeKind.V128, kind, "valueOf should return V128");
    }

    @Test
    @DisplayName("should have PACKED_I8 value")
    void shouldHavePackedI8Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("PACKED_I8");
      assertEquals(FieldType.ValueTypeKind.PACKED_I8, kind, "valueOf should return PACKED_I8");
    }

    @Test
    @DisplayName("should have PACKED_I16 value")
    void shouldHavePackedI16Value() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("PACKED_I16");
      assertEquals(FieldType.ValueTypeKind.PACKED_I16, kind, "valueOf should return PACKED_I16");
    }

    @Test
    @DisplayName("should have REFERENCE value")
    void shouldHaveReferenceValue() {
      FieldType.ValueTypeKind kind = FieldType.ValueTypeKind.valueOf("REFERENCE");
      assertEquals(FieldType.ValueTypeKind.REFERENCE, kind, "valueOf should return REFERENCE");
    }
  }
}
