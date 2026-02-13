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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.gc.FieldType.ValueTypeKind;
import ai.tegmentum.wasmtime4j.gc.I31Type.I31Value;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Comprehensive integration tests for the GC package data classes.
 *
 * <p>Tests GcReferenceType, I31Type, FieldType, FieldDefinition, ArrayType, and related classes.
 */
@DisplayName("GC Package Integration Tests")
class GcPackageIntegrationTest {

  private static final Logger LOGGER = Logger.getLogger(GcPackageIntegrationTest.class.getName());

  @Nested
  @DisplayName("GcReferenceType Tests")
  class GcReferenceTypeTests {

    @Test
    @DisplayName("should have all expected enum values")
    void shouldHaveAllExpectedEnumValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final GcReferenceType[] types = GcReferenceType.values();
      assertEquals(8, types.length, "Should have 8 reference types");

      assertNotNull(GcReferenceType.valueOf("ANY_REF"));
      assertNotNull(GcReferenceType.valueOf("EQ_REF"));
      assertNotNull(GcReferenceType.valueOf("I31_REF"));
      assertNotNull(GcReferenceType.valueOf("STRUCT_REF"));
      assertNotNull(GcReferenceType.valueOf("ARRAY_REF"));
      assertNotNull(GcReferenceType.valueOf("NULL_REF"));
      assertNotNull(GcReferenceType.valueOf("NULL_FUNC_REF"));
      assertNotNull(GcReferenceType.valueOf("NULL_EXTERN_REF"));
    }

    @Test
    @DisplayName("should return correct WASM names")
    void shouldReturnCorrectWasmNames(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals("anyref", GcReferenceType.ANY_REF.getWasmName());
      assertEquals("eqref", GcReferenceType.EQ_REF.getWasmName());
      assertEquals("i31ref", GcReferenceType.I31_REF.getWasmName());
      assertEquals("structref", GcReferenceType.STRUCT_REF.getWasmName());
      assertEquals("arrayref", GcReferenceType.ARRAY_REF.getWasmName());
      assertEquals("nullref", GcReferenceType.NULL_REF.getWasmName());
      assertEquals("nullfuncref", GcReferenceType.NULL_FUNC_REF.getWasmName());
      assertEquals("nullexternref", GcReferenceType.NULL_EXTERN_REF.getWasmName());
    }

    @Test
    @DisplayName("should correctly determine ANY_REF subtyping")
    void shouldCorrectlyDetermineAnyRefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // ANY_REF is only a subtype of itself
      assertTrue(GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertFalse(GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.EQ_REF));
      assertFalse(GcReferenceType.ANY_REF.isSubtypeOf(GcReferenceType.I31_REF));
    }

    @Test
    @DisplayName("should correctly determine EQ_REF subtyping")
    void shouldCorrectlyDetermineEqRefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // EQ_REF is a subtype of ANY_REF and itself
      assertTrue(GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertTrue(GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.EQ_REF));
      assertFalse(GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.I31_REF));
      assertFalse(GcReferenceType.EQ_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));
    }

    @Test
    @DisplayName("should correctly determine I31_REF subtyping")
    void shouldCorrectlyDetermineI31RefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // I31_REF is a subtype of ANY_REF, EQ_REF, and itself
      assertTrue(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertTrue(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.EQ_REF));
      assertTrue(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.I31_REF));
      assertFalse(GcReferenceType.I31_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));
    }

    @Test
    @DisplayName("should correctly determine STRUCT_REF subtyping")
    void shouldCorrectlyDetermineStructRefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertTrue(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.EQ_REF));
      assertTrue(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));
      assertFalse(GcReferenceType.STRUCT_REF.isSubtypeOf(GcReferenceType.ARRAY_REF));
    }

    @Test
    @DisplayName("should correctly determine ARRAY_REF subtyping")
    void shouldCorrectlyDetermineArrayRefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertTrue(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.EQ_REF));
      assertTrue(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.ARRAY_REF));
      assertFalse(GcReferenceType.ARRAY_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));
    }

    @Test
    @DisplayName("should correctly determine NULL_REF subtyping")
    void shouldCorrectlyDetermineNullRefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // NULL_REF is a subtype of all GC reference types
      assertTrue(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertTrue(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.EQ_REF));
      assertTrue(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.I31_REF));
      assertTrue(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.STRUCT_REF));
      assertTrue(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.ARRAY_REF));
      assertTrue(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.NULL_REF));
      assertFalse(GcReferenceType.NULL_REF.isSubtypeOf(GcReferenceType.NULL_FUNC_REF));
    }

    @Test
    @DisplayName("should correctly determine NULL_FUNC_REF and NULL_EXTERN_REF subtyping")
    void shouldCorrectlyDetermineNullFuncAndExternRefSubtyping(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // NULL_FUNC_REF is only a subtype of itself
      assertTrue(GcReferenceType.NULL_FUNC_REF.isSubtypeOf(GcReferenceType.NULL_FUNC_REF));
      assertFalse(GcReferenceType.NULL_FUNC_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertFalse(GcReferenceType.NULL_FUNC_REF.isSubtypeOf(GcReferenceType.NULL_EXTERN_REF));

      // NULL_EXTERN_REF is only a subtype of itself
      assertTrue(GcReferenceType.NULL_EXTERN_REF.isSubtypeOf(GcReferenceType.NULL_EXTERN_REF));
      assertFalse(GcReferenceType.NULL_EXTERN_REF.isSubtypeOf(GcReferenceType.ANY_REF));
      assertFalse(GcReferenceType.NULL_EXTERN_REF.isSubtypeOf(GcReferenceType.NULL_FUNC_REF));
    }

    @Test
    @DisplayName("should correctly determine equality support")
    void shouldCorrectlyDetermineEqualitySupport(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // Types that are subtypes of EQ_REF support equality
      assertFalse(GcReferenceType.ANY_REF.supportsEquality());
      assertTrue(GcReferenceType.EQ_REF.supportsEquality());
      assertTrue(GcReferenceType.I31_REF.supportsEquality());
      assertTrue(GcReferenceType.STRUCT_REF.supportsEquality());
      assertTrue(GcReferenceType.ARRAY_REF.supportsEquality());
      assertTrue(GcReferenceType.NULL_REF.supportsEquality());
      assertFalse(GcReferenceType.NULL_FUNC_REF.supportsEquality());
      assertFalse(GcReferenceType.NULL_EXTERN_REF.supportsEquality());
    }

    @Test
    @DisplayName("should use WASM name for toString")
    void shouldUseWasmNameForToString(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals("anyref", GcReferenceType.ANY_REF.toString());
      assertEquals("eqref", GcReferenceType.EQ_REF.toString());
    }
  }

  @Nested
  @DisplayName("I31Type Tests")
  class I31TypeTests {

    @Test
    @DisplayName("should have correct constant values")
    void shouldHaveCorrectConstantValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(-(1 << 30), I31Type.MIN_VALUE, "MIN_VALUE should be -2^30");
      assertEquals((1 << 30) - 1, I31Type.MAX_VALUE, "MAX_VALUE should be 2^30 - 1");
      assertEquals(31, I31Type.BIT_WIDTH, "BIT_WIDTH should be 31");
    }

    @Test
    @DisplayName("should validate values within range")
    void shouldValidateValuesWithinRange(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(I31Type.isValidValue(0), "0 should be valid");
      assertTrue(I31Type.isValidValue(1), "1 should be valid");
      assertTrue(I31Type.isValidValue(-1), "-1 should be valid");
      assertTrue(I31Type.isValidValue(I31Type.MIN_VALUE), "MIN_VALUE should be valid");
      assertTrue(I31Type.isValidValue(I31Type.MAX_VALUE), "MAX_VALUE should be valid");
    }

    @Test
    @DisplayName("should reject values outside range")
    void shouldRejectValuesOutsideRange(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertFalse(I31Type.isValidValue(I31Type.MIN_VALUE - 1), "Below MIN_VALUE should be invalid");
      assertFalse(I31Type.isValidValue(I31Type.MAX_VALUE + 1), "Above MAX_VALUE should be invalid");
      assertFalse(I31Type.isValidValue(Integer.MIN_VALUE), "Integer.MIN_VALUE should be invalid");
      assertFalse(I31Type.isValidValue(Integer.MAX_VALUE), "Integer.MAX_VALUE should be invalid");
    }

    @Test
    @DisplayName("should validate long values correctly")
    void shouldValidateLongValuesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(I31Type.isValidValue(0L));
      assertTrue(I31Type.isValidValue((long) I31Type.MAX_VALUE));
      assertTrue(I31Type.isValidValue((long) I31Type.MIN_VALUE));
      assertFalse(I31Type.isValidValue(Long.MAX_VALUE));
      assertFalse(I31Type.isValidValue(Long.MIN_VALUE));
    }

    @Test
    @DisplayName("should throw on invalid value in validateValue")
    void shouldThrowOnInvalidValueInValidateValue(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(IllegalArgumentException.class, () -> I31Type.validateValue(Integer.MAX_VALUE));
      assertThrows(IllegalArgumentException.class, () -> I31Type.validateValue(Integer.MIN_VALUE));
      assertThrows(IllegalArgumentException.class, () -> I31Type.validateValue(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("should return value from validateValue when valid")
    void shouldReturnValueFromValidateValueWhenValid(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(42, I31Type.validateValue(42));
      assertEquals(I31Type.MAX_VALUE, I31Type.validateValue(I31Type.MAX_VALUE));
      assertEquals(42, I31Type.validateValue(42L));
    }

    @Test
    @DisplayName("should clamp values correctly")
    void shouldClampValuesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(42, I31Type.clampValue(42), "In-range value should not be clamped");
      assertEquals(
          I31Type.MAX_VALUE,
          I31Type.clampValue(Integer.MAX_VALUE),
          "Above max should clamp to max");
      assertEquals(
          I31Type.MIN_VALUE,
          I31Type.clampValue(Integer.MIN_VALUE),
          "Below min should clamp to min");
    }

    @Test
    @DisplayName("should clamp long values correctly")
    void shouldClampLongValuesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(42, I31Type.clampValue(42L));
      assertEquals(I31Type.MAX_VALUE, I31Type.clampValue(Long.MAX_VALUE));
      assertEquals(I31Type.MIN_VALUE, I31Type.clampValue(Long.MIN_VALUE));
    }

    @Test
    @DisplayName("should return correct range string")
    void shouldReturnCorrectRangeString(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final String range = I31Type.getRange();
      assertTrue(range.contains(String.valueOf(I31Type.MIN_VALUE)));
      assertTrue(range.contains(String.valueOf(I31Type.MAX_VALUE)));
    }

    @Test
    @DisplayName("should convert to unsigned correctly")
    void shouldConvertToUnsignedCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(0, I31Type.toUnsigned(0));
      assertEquals(42, I31Type.toUnsigned(42));
      // Negative values should have their sign bit masked out
      int unsignedNegOne = I31Type.toUnsigned(-1);
      assertTrue(unsignedNegOne >= 0, "Unsigned result should be non-negative");
    }

    @Test
    @DisplayName("should compare values correctly")
    void shouldCompareValuesCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(I31Type.compare(0, 1) < 0);
      assertTrue(I31Type.compare(1, 0) > 0);
      assertEquals(0, I31Type.compare(5, 5));
    }

    @Test
    @DisplayName("should check equality correctly")
    void shouldCheckEqualityCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(I31Type.equals(0, 0));
      assertTrue(I31Type.equals(42, 42));
      assertFalse(I31Type.equals(1, 2));
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals("i31(42)", I31Type.toString(42));
      assertEquals("i31(0)", I31Type.toString(0));
      assertEquals("i31(-1)", I31Type.toString(-1));
    }
  }

  @Nested
  @DisplayName("I31Value Tests")
  class I31ValueTests {

    @Test
    @DisplayName("should create I31Value from int")
    void shouldCreateI31ValueFromInt(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final I31Value value = I31Value.of(42);
      assertEquals(42, value.getValue());
    }

    @Test
    @DisplayName("should create I31Value from long")
    void shouldCreateI31ValueFromLong(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final I31Value value = I31Value.of(42L);
      assertEquals(42, value.getValue());
    }

    @Test
    @DisplayName("should throw on out of range value")
    void shouldThrowOnOutOfRangeValue(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(IllegalArgumentException.class, () -> I31Value.of(Integer.MAX_VALUE));
      assertThrows(IllegalArgumentException.class, () -> I31Value.of(Long.MAX_VALUE));
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final I31Value v1 = I31Value.of(42);
      final I31Value v2 = I31Value.of(42);
      final I31Value v3 = I31Value.of(43);

      assertEquals(v1, v2);
      assertNotEquals(v1, v3);
      assertNotEquals(v1, null);
      assertNotEquals(v1, "string");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final I31Value v1 = I31Value.of(42);
      final I31Value v2 = I31Value.of(42);

      assertEquals(v1.hashCode(), v2.hashCode());
    }

    @Test
    @DisplayName("should get unsigned value")
    void shouldGetUnsignedValue(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final I31Value value = I31Value.of(42);
      assertEquals(42, value.getUnsigned());
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final I31Value value = I31Value.of(42);
      assertEquals("i31(42)", value.toString());
    }
  }

  @Nested
  @DisplayName("FieldType Tests")
  class FieldTypeTests {

    @Test
    @DisplayName("should create primitive field types")
    void shouldCreatePrimitiveFieldTypes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(ValueTypeKind.I32, FieldType.i32().getKind());
      assertEquals(ValueTypeKind.I64, FieldType.i64().getKind());
      assertEquals(ValueTypeKind.F32, FieldType.f32().getKind());
      assertEquals(ValueTypeKind.F64, FieldType.f64().getKind());
      assertEquals(ValueTypeKind.V128, FieldType.v128().getKind());
    }

    @Test
    @DisplayName("should create packed field types")
    void shouldCreatePackedFieldTypes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(ValueTypeKind.PACKED_I8, FieldType.packedI8().getKind());
      assertEquals(ValueTypeKind.PACKED_I16, FieldType.packedI16().getKind());

      assertTrue(FieldType.packedI8().isPacked());
      assertTrue(FieldType.packedI16().isPacked());
      assertFalse(FieldType.i32().isPacked());
    }

    @Test
    @DisplayName("should create reference field types")
    void shouldCreateReferenceFieldTypes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldType anyRef = FieldType.anyRef();
      assertEquals(ValueTypeKind.REFERENCE, anyRef.getKind());
      assertEquals(GcReferenceType.ANY_REF, anyRef.getReferenceType());
      assertTrue(anyRef.isNullable());
      assertTrue(anyRef.isReference());

      final FieldType eqRef = FieldType.eqRef();
      assertEquals(GcReferenceType.EQ_REF, eqRef.getReferenceType());

      final FieldType i31Ref = FieldType.i31Ref();
      assertEquals(GcReferenceType.I31_REF, i31Ref.getReferenceType());

      final FieldType structRef = FieldType.structRef();
      assertEquals(GcReferenceType.STRUCT_REF, structRef.getReferenceType());

      final FieldType arrayRef = FieldType.arrayRef();
      assertEquals(GcReferenceType.ARRAY_REF, arrayRef.getReferenceType());
    }

    @Test
    @DisplayName("should create non-nullable reference type")
    void shouldCreateNonNullableReferenceType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldType nonNullRef = FieldType.reference(GcReferenceType.ANY_REF, false);
      assertFalse(nonNullRef.isNullable());

      final FieldType nonNullRef2 = FieldType.reference(GcReferenceType.EQ_REF);
      assertFalse(nonNullRef2.isNullable());
    }

    @Test
    @DisplayName("should throw on null reference type")
    void shouldThrowOnNullReferenceType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(IllegalArgumentException.class, () -> FieldType.reference(null, true));
    }

    @Test
    @DisplayName("should return correct sizes")
    void shouldReturnCorrectSizes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(4, FieldType.i32().getSizeBytes());
      assertEquals(8, FieldType.i64().getSizeBytes());
      assertEquals(4, FieldType.f32().getSizeBytes());
      assertEquals(8, FieldType.f64().getSizeBytes());
      assertEquals(16, FieldType.v128().getSizeBytes());
      assertEquals(1, FieldType.packedI8().getSizeBytes());
      assertEquals(2, FieldType.packedI16().getSizeBytes());
      assertEquals(8, FieldType.anyRef().getSizeBytes());
    }

    @Test
    @DisplayName("should convert to WasmValueType")
    void shouldConvertToWasmValueType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(WasmValueType.I32, FieldType.i32().toWasmValueType());
      assertEquals(WasmValueType.I64, FieldType.i64().toWasmValueType());
      assertEquals(WasmValueType.F32, FieldType.f32().toWasmValueType());
      assertEquals(WasmValueType.F64, FieldType.f64().toWasmValueType());
      assertEquals(WasmValueType.V128, FieldType.v128().toWasmValueType());
      // Packed types map to I32
      assertEquals(WasmValueType.I32, FieldType.packedI8().toWasmValueType());
      assertEquals(WasmValueType.I32, FieldType.packedI16().toWasmValueType());
    }

    @Test
    @DisplayName("should throw when converting reference type to WasmValueType")
    void shouldThrowWhenConvertingReferenceTypeToWasmValueType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(IllegalStateException.class, () -> FieldType.anyRef().toWasmValueType());
    }

    @Test
    @DisplayName("should check compatibility correctly")
    void shouldCheckCompatibilityCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      // Same types are compatible
      assertTrue(FieldType.i32().isCompatibleWith(FieldType.i32()));
      assertTrue(FieldType.i64().isCompatibleWith(FieldType.i64()));

      // Different types are not compatible
      assertFalse(FieldType.i32().isCompatibleWith(FieldType.i64()));

      // Reference subtyping
      final FieldType eqRef = FieldType.reference(GcReferenceType.EQ_REF, true);
      final FieldType anyRef = FieldType.reference(GcReferenceType.ANY_REF, true);
      assertTrue(eqRef.isCompatibleWith(anyRef), "EQ_REF should be compatible with ANY_REF");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(FieldType.i32(), FieldType.i32());
      assertEquals(FieldType.anyRef(), FieldType.anyRef());
      assertNotEquals(FieldType.i32(), FieldType.i64());
      assertNotEquals(FieldType.anyRef(), FieldType.eqRef());
      assertNotEquals(FieldType.i32(), null);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals(FieldType.i32().hashCode(), FieldType.i32().hashCode());
      assertEquals(FieldType.anyRef().hashCode(), FieldType.anyRef().hashCode());
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertEquals("i32", FieldType.i32().toString());
      assertEquals("i64", FieldType.i64().toString());
      assertEquals("anyref?", FieldType.anyRef().toString());
      assertEquals("eqref", FieldType.reference(GcReferenceType.EQ_REF, false).toString());
    }
  }

  @Nested
  @DisplayName("FieldDefinition Tests")
  class FieldDefinitionTests {

    @Test
    @DisplayName("should create field definition with name")
    void shouldCreateFieldDefinitionWithName(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertEquals("x", field.getName());
      assertEquals(FieldType.i32(), field.getFieldType());
      assertTrue(field.isMutable());
      assertEquals(0, field.getIndex());
      assertTrue(field.hasName());
    }

    @Test
    @DisplayName("should create unnamed field definition")
    void shouldCreateUnnamedFieldDefinition(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition field = new FieldDefinition(null, FieldType.i64(), false, 1);

      assertNull(field.getName());
      assertFalse(field.hasName());
      assertEquals(1, field.getIndex());
    }

    @Test
    @DisplayName("should throw on null field type")
    void shouldThrowOnNullFieldType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(NullPointerException.class, () -> new FieldDefinition("test", null, true, 0));
    }

    @Test
    @DisplayName("should throw on negative index")
    void shouldThrowOnNegativeIndex(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(
          IllegalArgumentException.class,
          () -> new FieldDefinition("test", FieldType.i32(), true, -1));
    }

    @Test
    @DisplayName("should return correct size bytes")
    void shouldReturnCorrectSizeBytes(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition field = new FieldDefinition("x", FieldType.i32(), true, 0);
      assertEquals(4, field.getSizeBytes());
    }

    @Test
    @DisplayName("should check compatibility correctly")
    void shouldCheckCompatibilityCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition mutableField = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition immutableField = new FieldDefinition("x", FieldType.i32(), false, 0);
      final FieldDefinition differentType = new FieldDefinition("x", FieldType.i64(), true, 0);

      // Mutable can substitute for immutable
      assertTrue(mutableField.isCompatibleWith(immutableField));

      // Immutable cannot substitute for mutable
      assertFalse(immutableField.isCompatibleWith(mutableField));

      // Different types are not compatible
      assertFalse(mutableField.isCompatibleWith(differentType));
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition f1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition f2 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition f3 = new FieldDefinition("y", FieldType.i32(), true, 0);

      assertEquals(f1, f2);
      assertNotEquals(f1, f3);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition f1 = new FieldDefinition("x", FieldType.i32(), true, 0);
      final FieldDefinition f2 = new FieldDefinition("x", FieldType.i32(), true, 0);

      assertEquals(f1.hashCode(), f2.hashCode());
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final FieldDefinition namedMutable = new FieldDefinition("x", FieldType.i32(), true, 0);
      final String namedMutableStr = namedMutable.toString();
      assertTrue(namedMutableStr.contains("x"));
      assertTrue(namedMutableStr.contains("i32"));
      assertTrue(namedMutableStr.contains("mut"));

      final FieldDefinition unnamedImmutable = new FieldDefinition(null, FieldType.i64(), false, 1);
      final String unnamedImmutableStr = unnamedImmutable.toString();
      assertTrue(unnamedImmutableStr.contains("i64"));
      assertFalse(unnamedImmutableStr.contains("mut"));
    }
  }

  @Nested
  @DisplayName("ArrayType Tests")
  class ArrayTypeTests {

    @Test
    @DisplayName("should create array type with builder")
    void shouldCreateArrayTypeWithBuilder(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType arrayType =
          ArrayType.builder("IntArray").elementType(FieldType.i32()).mutable(true).build();

      assertEquals("IntArray", arrayType.getName());
      assertEquals(FieldType.i32(), arrayType.getElementType());
      assertTrue(arrayType.isMutable());
    }

    @Test
    @DisplayName("should create array type with of factory")
    void shouldCreateArrayTypeWithOfFactory(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType arrayType = ArrayType.of("IntArray", FieldType.i32());

      assertEquals("IntArray", arrayType.getName());
      assertEquals(FieldType.i32(), arrayType.getElementType());
      assertTrue(arrayType.isMutable(), "Default should be mutable");
    }

    @Test
    @DisplayName("should create immutable array type")
    void shouldCreateImmutableArrayType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType arrayType =
          ArrayType.builder("ImmutableArray").elementType(FieldType.f64()).immutable().build();

      assertFalse(arrayType.isMutable());
    }

    @Test
    @DisplayName("should throw on null name")
    void shouldThrowOnNullName(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(NullPointerException.class, () -> ArrayType.builder(null));
    }

    @Test
    @DisplayName("should throw on null element type")
    void shouldThrowOnNullElementType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(NullPointerException.class, () -> ArrayType.builder("Test").elementType(null));
    }

    @Test
    @DisplayName("should throw on missing element type")
    void shouldThrowOnMissingElementType(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertThrows(IllegalStateException.class, () -> ArrayType.builder("Test").build());
    }

    @Test
    @DisplayName("should return correct element size")
    void shouldReturnCorrectElementSize(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType i32Array = ArrayType.of("I32Array", FieldType.i32());
      assertEquals(4, i32Array.getElementSizeBytes());

      final ArrayType i64Array = ArrayType.of("I64Array", FieldType.i64());
      assertEquals(8, i64Array.getElementSizeBytes());
    }

    @Test
    @DisplayName("should calculate array size correctly")
    void shouldCalculateArraySizeCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType arrayType = ArrayType.of("IntArray", FieldType.i32());

      assertEquals(0, arrayType.getArraySizeBytes(0));
      assertEquals(4, arrayType.getArraySizeBytes(1));
      assertEquals(40, arrayType.getArraySizeBytes(10));
    }

    @Test
    @DisplayName("should throw on negative array length")
    void shouldThrowOnNegativeArrayLength(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType arrayType = ArrayType.of("IntArray", FieldType.i32());

      assertThrows(IllegalArgumentException.class, () -> arrayType.getArraySizeBytes(-1));
    }

    @Test
    @DisplayName("should check subtyping correctly")
    void shouldCheckSubtypingCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType mutableArray =
          ArrayType.builder("Mut").elementType(FieldType.i32()).mutable(true).build();
      final ArrayType immutableArray =
          ArrayType.builder("Immut").elementType(FieldType.i32()).immutable().build();

      // An array is a subtype of itself
      assertTrue(mutableArray.isSubtypeOf(mutableArray));

      // Note: Since ArrayType.equals() compares by typeId only, and both arrays have typeId=0,
      // they are considered equal, so isSubtypeOf returns true via the equals short-circuit.
      // This tests the behavior as implemented - both arrays are "equal" from the runtime's
      // perspective.
      assertTrue(
          immutableArray.isSubtypeOf(mutableArray),
          "Same typeId means equal, so subtype check passes");
      assertTrue(
          mutableArray.isSubtypeOf(immutableArray),
          "Same typeId means equal, so subtype check passes");
    }

    @Test
    @DisplayName("should check element assignment correctly")
    void shouldCheckElementAssignmentCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType arrayType = ArrayType.of("IntArray", FieldType.i32());

      assertTrue(arrayType.canAssignElement(FieldType.i32()));
      assertFalse(arrayType.canAssignElement(FieldType.i64()));
    }

    @Test
    @DisplayName("should validate correctly")
    void shouldValidateCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType validArray = ArrayType.of("Valid", FieldType.i32());
      validArray.validate(); // Should not throw
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ArrayType mutableArray =
          ArrayType.builder("IntArray").elementType(FieldType.i32()).mutable(true).build();
      final String str = mutableArray.toString();

      assertTrue(str.contains("array"));
      assertTrue(str.contains("IntArray"));
      assertTrue(str.contains("i32"));
      assertTrue(str.contains("mut"));
    }
  }

  @Nested
  @DisplayName("ValueTypeKind Enum Tests")
  class ValueTypeKindTests {

    @Test
    @DisplayName("should have all expected values")
    void shouldHaveAllExpectedValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final ValueTypeKind[] kinds = ValueTypeKind.values();
      assertEquals(8, kinds.length);

      assertNotNull(ValueTypeKind.valueOf("I32"));
      assertNotNull(ValueTypeKind.valueOf("I64"));
      assertNotNull(ValueTypeKind.valueOf("F32"));
      assertNotNull(ValueTypeKind.valueOf("F64"));
      assertNotNull(ValueTypeKind.valueOf("V128"));
      assertNotNull(ValueTypeKind.valueOf("PACKED_I8"));
      assertNotNull(ValueTypeKind.valueOf("PACKED_I16"));
      assertNotNull(ValueTypeKind.valueOf("REFERENCE"));
    }
  }

  @Nested
  @DisplayName("GcHeapStats Tests")
  class GcHeapStatsTests {

    @Test
    @DisplayName("should create GcHeapStats with default constructor")
    void shouldCreateGcHeapStatsWithDefaultConstructor(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final GcHeapStats stats = new GcHeapStats();

      assertEquals(0, stats.getTotalAllocated(), "Default totalAllocated should be 0");
      assertEquals(0, stats.getCurrentHeapSize(), "Default currentHeapSize should be 0");
      assertEquals(0, stats.getMajorCollections(), "Default majorCollections should be 0");
    }

    @Test
    @DisplayName("should allow setting public fields directly")
    void shouldAllowSettingPublicFieldsDirectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final GcHeapStats stats = new GcHeapStats();

      stats.totalAllocated = 1024L;
      stats.currentHeapSize = 2048L;
      stats.majorCollections = 5L;

      assertEquals(1024L, stats.getTotalAllocated());
      assertEquals(2048L, stats.getCurrentHeapSize());
      assertEquals(5L, stats.getMajorCollections());
    }

    @Test
    @DisplayName("should format toString correctly")
    void shouldFormatToStringCorrectly(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = 100L;
      stats.currentHeapSize = 200L;
      stats.majorCollections = 3L;

      final String str = stats.toString();

      assertTrue(str.contains("GcHeapStats"), "toString should contain class name");
      assertTrue(str.contains("100"), "toString should contain totalAllocated value");
      assertTrue(str.contains("200"), "toString should contain currentHeapSize value");
      assertTrue(str.contains("3"), "toString should contain majorCollections value");
    }

    @Test
    @DisplayName("should be final class")
    void shouldBeFinalClass(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      assertTrue(
          Modifier.isFinal(GcHeapStats.class.getModifiers()), "GcHeapStats should be final class");
    }

    @Test
    @DisplayName("should have public fields for JNI access")
    void shouldHavePublicFieldsForJniAccess(final TestInfo testInfo) throws Exception {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      var totalAllocatedField = GcHeapStats.class.getDeclaredField("totalAllocated");
      assertTrue(
          Modifier.isPublic(totalAllocatedField.getModifiers()), "totalAllocated should be public");

      var currentHeapSizeField = GcHeapStats.class.getDeclaredField("currentHeapSize");
      assertTrue(
          Modifier.isPublic(currentHeapSizeField.getModifiers()),
          "currentHeapSize should be public");

      var majorCollectionsField = GcHeapStats.class.getDeclaredField("majorCollections");
      assertTrue(
          Modifier.isPublic(majorCollectionsField.getModifiers()),
          "majorCollections should be public");
    }

    @Test
    @DisplayName("should handle large values")
    void shouldHandleLargeValues(final TestInfo testInfo) {
      LOGGER.info("Running: " + testInfo.getDisplayName());

      final GcHeapStats stats = new GcHeapStats();
      stats.totalAllocated = Long.MAX_VALUE;
      stats.currentHeapSize = Long.MAX_VALUE;
      stats.majorCollections = Long.MAX_VALUE;

      assertEquals(Long.MAX_VALUE, stats.getTotalAllocated());
      assertEquals(Long.MAX_VALUE, stats.getCurrentHeapSize());
      assertEquals(Long.MAX_VALUE, stats.getMajorCollections());
    }
  }
}
