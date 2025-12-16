package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.*;

import ai.tegmentum.wasmtime4j.exception.MultiValueException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the WasmValue class.
 *
 * <p>Tests verify value creation, type checking, value extraction, multi-value operations, and edge
 * cases for all WebAssembly value types.
 */
@DisplayName("WasmValue Tests")
class WasmValueTest {

  @Nested
  @DisplayName("I32 Value Tests")
  class I32ValueTests {

    @Test
    @DisplayName("should create i32 value")
    void shouldCreateI32Value() {
      final WasmValue value = WasmValue.i32(42);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.I32, value.getType(), "Type should be I32");
      assertEquals(42, value.asI32(), "Value should be 42");
      assertEquals(42, value.asInt(), "asInt() should return same value");
    }

    @Test
    @DisplayName("should create i32 value using ofI32")
    void shouldCreateI32ValueUsingOfI32() {
      final WasmValue value = WasmValue.ofI32(100);
      assertEquals(WasmValueType.I32, value.getType(), "Type should be I32");
      assertEquals(100, value.asI32(), "Value should be 100");
    }

    @Test
    @DisplayName("should handle maximum i32 value")
    void shouldHandleMaximumI32Value() {
      final WasmValue value = WasmValue.i32(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, value.asI32(), "Should handle MAX_VALUE");
    }

    @Test
    @DisplayName("should handle minimum i32 value")
    void shouldHandleMinimumI32Value() {
      final WasmValue value = WasmValue.i32(Integer.MIN_VALUE);
      assertEquals(Integer.MIN_VALUE, value.asI32(), "Should handle MIN_VALUE");
    }

    @Test
    @DisplayName("should handle negative i32 values")
    void shouldHandleNegativeI32Values() {
      final WasmValue value = WasmValue.i32(-12345);
      assertEquals(-12345, value.asI32(), "Should handle negative value");
    }

    @Test
    @DisplayName("should handle zero i32 value")
    void shouldHandleZeroI32Value() {
      final WasmValue value = WasmValue.i32(0);
      assertEquals(0, value.asI32(), "Should handle zero");
    }

    @Test
    @DisplayName("isI32 should return true for i32")
    void isI32ShouldReturnTrueForI32() {
      final WasmValue value = WasmValue.i32(42);
      assertTrue(value.isI32(), "isI32() should return true");
      assertFalse(value.isI64(), "isI64() should return false");
      assertFalse(value.isF32(), "isF32() should return false");
      assertFalse(value.isF64(), "isF64() should return false");
    }
  }

  @Nested
  @DisplayName("I64 Value Tests")
  class I64ValueTests {

    @Test
    @DisplayName("should create i64 value")
    void shouldCreateI64Value() {
      final WasmValue value = WasmValue.i64(123456789012345L);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.I64, value.getType(), "Type should be I64");
      assertEquals(123456789012345L, value.asI64(), "Value should match");
      assertEquals(123456789012345L, value.asLong(), "asLong() should return same value");
    }

    @Test
    @DisplayName("should create i64 value using ofI64")
    void shouldCreateI64ValueUsingOfI64() {
      final WasmValue value = WasmValue.ofI64(999999999999L);
      assertEquals(WasmValueType.I64, value.getType(), "Type should be I64");
      assertEquals(999999999999L, value.asI64(), "Value should match");
    }

    @Test
    @DisplayName("should handle maximum i64 value")
    void shouldHandleMaximumI64Value() {
      final WasmValue value = WasmValue.i64(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, value.asI64(), "Should handle MAX_VALUE");
    }

    @Test
    @DisplayName("should handle minimum i64 value")
    void shouldHandleMinimumI64Value() {
      final WasmValue value = WasmValue.i64(Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, value.asI64(), "Should handle MIN_VALUE");
    }

    @Test
    @DisplayName("isI64 should return true for i64")
    void isI64ShouldReturnTrueForI64() {
      final WasmValue value = WasmValue.i64(42L);
      assertTrue(value.isI64(), "isI64() should return true");
      assertFalse(value.isI32(), "isI32() should return false");
    }
  }

  @Nested
  @DisplayName("F32 Value Tests")
  class F32ValueTests {

    @Test
    @DisplayName("should create f32 value")
    void shouldCreateF32Value() {
      final WasmValue value = WasmValue.f32(3.14159f);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.F32, value.getType(), "Type should be F32");
      assertEquals(3.14159f, value.asF32(), 0.00001f, "Value should match");
      assertEquals(3.14159f, value.asFloat(), 0.00001f, "asFloat() should return same value");
    }

    @Test
    @DisplayName("should create f32 value using ofF32")
    void shouldCreateF32ValueUsingOfF32() {
      final WasmValue value = WasmValue.ofF32(2.71828f);
      assertEquals(WasmValueType.F32, value.getType(), "Type should be F32");
      assertEquals(2.71828f, value.asF32(), 0.00001f, "Value should match");
    }

    @Test
    @DisplayName("should handle special f32 values")
    void shouldHandleSpecialF32Values() {
      final WasmValue nanValue = WasmValue.f32(Float.NaN);
      assertTrue(Float.isNaN(nanValue.asF32()), "Should preserve NaN");

      final WasmValue posInf = WasmValue.f32(Float.POSITIVE_INFINITY);
      assertEquals(Float.POSITIVE_INFINITY, posInf.asF32(), "Should preserve positive infinity");

      final WasmValue negInf = WasmValue.f32(Float.NEGATIVE_INFINITY);
      assertEquals(Float.NEGATIVE_INFINITY, negInf.asF32(), "Should preserve negative infinity");
    }

    @Test
    @DisplayName("should handle negative f32 values")
    void shouldHandleNegativeF32Values() {
      final WasmValue value = WasmValue.f32(-123.456f);
      assertEquals(-123.456f, value.asF32(), 0.001f, "Should handle negative value");
    }

    @Test
    @DisplayName("isF32 should return true for f32")
    void isF32ShouldReturnTrueForF32() {
      final WasmValue value = WasmValue.f32(1.0f);
      assertTrue(value.isF32(), "isF32() should return true");
      assertFalse(value.isF64(), "isF64() should return false");
    }
  }

  @Nested
  @DisplayName("F64 Value Tests")
  class F64ValueTests {

    @Test
    @DisplayName("should create f64 value")
    void shouldCreateF64Value() {
      final WasmValue value = WasmValue.f64(3.141592653589793);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.F64, value.getType(), "Type should be F64");
      assertEquals(3.141592653589793, value.asF64(), 0.000000000001, "Value should match");
      assertEquals(
          3.141592653589793,
          value.asDouble(),
          0.000000000001,
          "asDouble() should return same value");
    }

    @Test
    @DisplayName("should create f64 value using ofF64")
    void shouldCreateF64ValueUsingOfF64() {
      final WasmValue value = WasmValue.ofF64(2.718281828459045);
      assertEquals(WasmValueType.F64, value.getType(), "Type should be F64");
      assertEquals(2.718281828459045, value.asF64(), 0.000000000001, "Value should match");
    }

    @Test
    @DisplayName("should handle special f64 values")
    void shouldHandleSpecialF64Values() {
      final WasmValue nanValue = WasmValue.f64(Double.NaN);
      assertTrue(Double.isNaN(nanValue.asF64()), "Should preserve NaN");

      final WasmValue posInf = WasmValue.f64(Double.POSITIVE_INFINITY);
      assertEquals(Double.POSITIVE_INFINITY, posInf.asF64(), "Should preserve positive infinity");

      final WasmValue negInf = WasmValue.f64(Double.NEGATIVE_INFINITY);
      assertEquals(Double.NEGATIVE_INFINITY, negInf.asF64(), "Should preserve negative infinity");
    }

    @Test
    @DisplayName("isF64 should return true for f64")
    void isF64ShouldReturnTrueForF64() {
      final WasmValue value = WasmValue.f64(1.0);
      assertTrue(value.isF64(), "isF64() should return true");
      assertFalse(value.isF32(), "isF32() should return false");
    }
  }

  @Nested
  @DisplayName("V128 Value Tests")
  class V128ValueTests {

    @Test
    @DisplayName("should create v128 value from byte array")
    void shouldCreateV128ValueFromByteArray() {
      final byte[] bytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        bytes[i] = (byte) i;
      }
      final WasmValue value = WasmValue.v128(bytes);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.V128, value.getType(), "Type should be V128");
      assertArrayEquals(bytes, value.asV128(), "Value should match");
    }

    @Test
    @DisplayName("should create v128 value from two longs")
    void shouldCreateV128ValueFromTwoLongs() {
      final WasmValue value = WasmValue.v128(0x0102030405060708L, 0x090A0B0C0D0E0F10L);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.V128, value.getType(), "Type should be V128");
      assertEquals(16, value.asV128().length, "V128 should be 16 bytes");
    }

    @Test
    @DisplayName("should throw for invalid v128 byte array")
    void shouldThrowForInvalidV128ByteArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValue.v128(new byte[15]),
          "Should throw for wrong size array");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValue.v128(new byte[17]),
          "Should throw for wrong size array");
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValue.v128((byte[]) null),
          "Should throw for null array");
    }

    @Test
    @DisplayName("should return defensive copy of v128")
    void shouldReturnDefensiveCopyOfV128() {
      final byte[] bytes = new byte[16];
      final WasmValue value = WasmValue.v128(bytes);
      final byte[] copy1 = value.asV128();
      final byte[] copy2 = value.asV128();
      assertNotSame(copy1, copy2, "Should return different array instances");
    }

    @Test
    @DisplayName("isV128 should return true for v128")
    void isV128ShouldReturnTrueForV128() {
      final WasmValue value = WasmValue.v128(new byte[16]);
      assertTrue(value.isV128(), "isV128() should return true");
      assertFalse(value.isI32(), "isI32() should return false");
    }
  }

  @Nested
  @DisplayName("Reference Type Tests")
  class ReferenceTypeTests {

    @Test
    @DisplayName("should create funcref value")
    void shouldCreateFuncrefValue() {
      final Object ref = new Object();
      final WasmValue value = WasmValue.funcref(ref);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.FUNCREF, value.getType(), "Type should be FUNCREF");
      assertSame(ref, value.asFuncref(), "Reference should be the same object");
    }

    @Test
    @DisplayName("should create null funcref value")
    void shouldCreateNullFuncrefValue() {
      final WasmValue value = WasmValue.nullFuncref();
      assertEquals(WasmValueType.FUNCREF, value.getType(), "Type should be FUNCREF");
      assertNull(value.asFuncref(), "Reference should be null");
    }

    @Test
    @DisplayName("should create externref value")
    void shouldCreateExternrefValue() {
      final String ref = "test-reference";
      final WasmValue value = WasmValue.externref(ref);
      assertNotNull(value, "Value should not be null");
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertEquals(ref, value.asExternref(), "Reference should match");
    }

    @Test
    @DisplayName("should create externref value using externRef alias")
    void shouldCreateExternrefValueUsingExternRefAlias() {
      final Object ref = new Object();
      final WasmValue value = WasmValue.externRef(ref);
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertSame(ref, value.asExternref(), "Reference should be the same object");
    }

    @Test
    @DisplayName("should create null externref value")
    void shouldCreateNullExternrefValue() {
      final WasmValue value = WasmValue.nullExternref();
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertNull(value.asExternref(), "Reference should be null");
    }

    @Test
    @DisplayName("should throw when accessing funcref as externref")
    void shouldThrowWhenAccessingFuncrefAsExternref() {
      final WasmValue value = WasmValue.funcref(new Object());
      assertThrows(ClassCastException.class, value::asExternref, "Should throw ClassCastException");
    }

    @Test
    @DisplayName("should throw when accessing externref as funcref")
    void shouldThrowWhenAccessingExternrefAsFuncref() {
      final WasmValue value = WasmValue.externref(new Object());
      assertThrows(ClassCastException.class, value::asFuncref, "Should throw ClassCastException");
    }

    @Test
    @DisplayName("isFuncref should return true for funcref")
    void isFuncrefShouldReturnTrueForFuncref() {
      final WasmValue value = WasmValue.funcref(null);
      assertTrue(value.isFuncref(), "isFuncref() should return true");
      assertFalse(value.isExternref(), "isExternref() should return false");
    }

    @Test
    @DisplayName("isExternref should return true for externref")
    void isExternrefShouldReturnTrueForExternref() {
      final WasmValue value = WasmValue.externref(null);
      assertTrue(value.isExternref(), "isExternref() should return true");
      assertFalse(value.isFuncref(), "isFuncref() should return false");
    }

    @Test
    @DisplayName("asReference should work for both reference types")
    void asReferenceShouldWorkForBothReferenceTypes() {
      final Object ref = new Object();
      final WasmValue funcrefValue = WasmValue.funcref(ref);
      final WasmValue externrefValue = WasmValue.externref(ref);

      assertSame(ref, funcrefValue.asReference(), "asReference() should work for funcref");
      assertSame(ref, externrefValue.asReference(), "asReference() should work for externref");
    }

    @Test
    @DisplayName("asReference should throw for non-reference types")
    void asReferenceShouldThrowForNonReferenceTypes() {
      final WasmValue value = WasmValue.i32(42);
      assertThrows(ClassCastException.class, value::asReference, "Should throw ClassCastException");
    }
  }

  @Nested
  @DisplayName("Type Classification Tests")
  class TypeClassificationTests {

    @Test
    @DisplayName("isNumeric should identify numeric types")
    void isNumericShouldIdentifyNumericTypes() {
      assertTrue(WasmValue.i32(0).isNumeric(), "I32 should be numeric");
      assertTrue(WasmValue.i64(0).isNumeric(), "I64 should be numeric");
      assertTrue(WasmValue.f32(0).isNumeric(), "F32 should be numeric");
      assertTrue(WasmValue.f64(0).isNumeric(), "F64 should be numeric");
      assertFalse(WasmValue.v128(new byte[16]).isNumeric(), "V128 should not be numeric");
      assertFalse(WasmValue.funcref(null).isNumeric(), "FUNCREF should not be numeric");
      assertFalse(WasmValue.externref(null).isNumeric(), "EXTERNREF should not be numeric");
    }

    @Test
    @DisplayName("isReference should identify reference types")
    void isReferenceShouldIdentifyReferenceTypes() {
      assertTrue(WasmValue.funcref(null).isReference(), "FUNCREF should be reference");
      assertTrue(WasmValue.externref(null).isReference(), "EXTERNREF should be reference");
      assertFalse(WasmValue.i32(0).isReference(), "I32 should not be reference");
      assertFalse(WasmValue.v128(new byte[16]).isReference(), "V128 should not be reference");
    }

    @Test
    @DisplayName("isVector should identify vector types")
    void isVectorShouldIdentifyVectorTypes() {
      assertTrue(WasmValue.v128(new byte[16]).isVector(), "V128 should be vector");
      assertFalse(WasmValue.i32(0).isVector(), "I32 should not be vector");
      assertFalse(WasmValue.funcref(null).isVector(), "FUNCREF should not be vector");
    }
  }

  @Nested
  @DisplayName("Type Validation Tests")
  class TypeValidationTests {

    @Test
    @DisplayName("validateType should pass for matching type")
    void validateTypeShouldPassForMatchingType() {
      final WasmValue value = WasmValue.i32(42);
      assertDoesNotThrow(
          () -> value.validateType(WasmValueType.I32), "Should not throw for matching type");
    }

    @Test
    @DisplayName("validateType should throw for mismatched type")
    void validateTypeShouldThrowForMismatchedType() {
      final WasmValue value = WasmValue.i32(42);
      assertThrows(
          IllegalArgumentException.class,
          () -> value.validateType(WasmValueType.I64),
          "Should throw for mismatched type");
    }

    @Test
    @DisplayName("validateType should throw for null expected type")
    void validateTypeShouldThrowForNullExpectedType() {
      final WasmValue value = WasmValue.i32(42);
      assertThrows(
          IllegalArgumentException.class,
          () -> value.validateType(null),
          "Should throw for null expected type");
    }
  }

  @Nested
  @DisplayName("Multi-Value Tests")
  class MultiValueTests {

    @Test
    @DisplayName("should create multi-value array")
    void shouldCreateMultiValueArray() {
      final WasmValue[] values =
          WasmValue.multiValue(WasmValue.i32(1), WasmValue.i64(2L), WasmValue.f32(3.0f));
      assertEquals(3, values.length, "Should have 3 values");
      assertEquals(WasmValueType.I32, values[0].getType(), "First should be I32");
      assertEquals(WasmValueType.I64, values[1].getType(), "Second should be I64");
      assertEquals(WasmValueType.F32, values[2].getType(), "Third should be F32");
    }

    @Test
    @DisplayName("multiValue should throw for null array")
    void multiValueShouldThrowForNullArray() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValue.multiValue((WasmValue[]) null),
          "Should throw for null array");
    }

    @Test
    @DisplayName("validateMultiValue should pass for matching types")
    void validateMultiValueShouldPassForMatchingTypes() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), WasmValue.i64(2L)};
      final WasmValueType[] types = new WasmValueType[] {WasmValueType.I32, WasmValueType.I64};
      assertDoesNotThrow(
          () -> WasmValue.validateMultiValue(values, types), "Should not throw for matching types");
    }

    @Test
    @DisplayName("validateMultiValue should throw for mismatched count")
    void validateMultiValueShouldThrowForMismatchedCount() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1)};
      final WasmValueType[] types = new WasmValueType[] {WasmValueType.I32, WasmValueType.I64};
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValue.validateMultiValue(values, types),
          "Should throw for mismatched count");
    }

    @Test
    @DisplayName("validateMultiValue should throw for null value in array")
    void validateMultiValueShouldThrowForNullValueInArray() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), null};
      final WasmValueType[] types = new WasmValueType[] {WasmValueType.I32, WasmValueType.I64};
      assertThrows(
          IllegalArgumentException.class,
          () -> WasmValue.validateMultiValue(values, types),
          "Should throw for null value");
    }

    @Test
    @DisplayName("isMultiValue should detect multi-value arrays")
    void isMultiValueShouldDetectMultiValueArrays() {
      assertFalse(WasmValue.isMultiValue(null), "Null should not be multi-value");
      assertFalse(WasmValue.isMultiValue(new WasmValue[0]), "Empty should not be multi-value");
      assertFalse(
          WasmValue.isMultiValue(new WasmValue[] {WasmValue.i32(1)}),
          "Single value should not be multi-value");
      assertTrue(
          WasmValue.isMultiValue(new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)}),
          "Two values should be multi-value");
    }

    @Test
    @DisplayName("getFirstValue should return first value")
    void getFirstValueShouldReturnFirstValue() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
      final WasmValue first = WasmValue.getFirstValue(values);
      assertEquals(1, first.asI32(), "Should return first value");
    }

    @Test
    @DisplayName("getFirstValue should return null for empty array")
    void getFirstValueShouldReturnNullForEmptyArray() {
      assertNull(WasmValue.getFirstValue(null), "Null array should return null");
      assertNull(WasmValue.getFirstValue(new WasmValue[0]), "Empty array should return null");
    }

    @Test
    @DisplayName("getLastValue should return last value")
    void getLastValueShouldReturnLastValue() {
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
      final WasmValue last = WasmValue.getLastValue(values);
      assertEquals(2, last.asI32(), "Should return last value");
    }

    @Test
    @DisplayName("extractByType should filter values by type")
    void extractByTypeShouldFilterValuesByType() {
      final WasmValue[] values =
          new WasmValue[] {WasmValue.i32(1), WasmValue.i64(2L), WasmValue.i32(3)};
      final WasmValue[] i32Values = WasmValue.extractByType(values, WasmValueType.I32);
      assertEquals(2, i32Values.length, "Should have 2 I32 values");
      assertEquals(1, i32Values[0].asI32(), "First I32 should be 1");
      assertEquals(3, i32Values[1].asI32(), "Second I32 should be 3");
    }

    @Test
    @DisplayName("validateMultiValueLimits should enforce limits")
    void validateMultiValueLimitsShouldEnforceLimits() throws MultiValueException {
      final WasmValue[] validValues = new WasmValue[16];
      for (int i = 0; i < 16; i++) {
        validValues[i] = WasmValue.i32(i);
      }
      assertDoesNotThrow(
          () -> WasmValue.validateMultiValueLimits(validValues), "16 values should be valid");

      final WasmValue[] invalidValues = new WasmValue[17];
      for (int i = 0; i < 17; i++) {
        invalidValues[i] = WasmValue.i32(i);
      }
      assertThrows(
          MultiValueException.class,
          () -> WasmValue.validateMultiValueLimits(invalidValues),
          "17 values should exceed limit");
    }

    @Test
    @DisplayName("copyMultiValue should create copy")
    void copyMultiValueShouldCreateCopy() {
      final WasmValue[] original = new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
      final WasmValue[] copy = WasmValue.copyMultiValue(original);
      assertNotSame(original, copy, "Should be different array instances");
      assertEquals(original.length, copy.length, "Should have same length");
    }

    @Test
    @DisplayName("copyMultiValue should return null for null input")
    void copyMultiValueShouldReturnNullForNullInput() {
      assertNull(WasmValue.copyMultiValue(null), "Should return null for null input");
    }

    @Test
    @DisplayName("multiValueToString should format correctly")
    void multiValueToStringShouldFormatCorrectly() {
      assertEquals("null", WasmValue.multiValueToString(null), "Null should return 'null'");
      assertEquals(
          "[]", WasmValue.multiValueToString(new WasmValue[0]), "Empty should return '[]'");
      final WasmValue[] values = new WasmValue[] {WasmValue.i32(1), WasmValue.i32(2)};
      final String str = WasmValue.multiValueToString(values);
      assertTrue(str.startsWith("["), "Should start with '['");
      assertTrue(str.endsWith("]"), "Should end with ']'");
      assertTrue(str.contains(","), "Should contain comma separator");
    }
  }

  @Nested
  @DisplayName("Equality and HashCode Tests")
  class EqualityAndHashCodeTests {

    @Test
    @DisplayName("should be equal for same i32 values")
    void shouldBeEqualForSameI32Values() {
      final WasmValue value1 = WasmValue.i32(42);
      final WasmValue value2 = WasmValue.i32(42);
      assertEquals(value1, value2, "Same i32 values should be equal");
      assertEquals(value1.hashCode(), value2.hashCode(), "Hash codes should match");
    }

    @Test
    @DisplayName("should not be equal for different i32 values")
    void shouldNotBeEqualForDifferentI32Values() {
      final WasmValue value1 = WasmValue.i32(42);
      final WasmValue value2 = WasmValue.i32(43);
      assertNotEquals(value1, value2, "Different i32 values should not be equal");
    }

    @Test
    @DisplayName("should not be equal for different types")
    void shouldNotBeEqualForDifferentTypes() {
      final WasmValue i32Value = WasmValue.i32(42);
      final WasmValue i64Value = WasmValue.i64(42L);
      assertNotEquals(i32Value, i64Value, "Different types should not be equal");
    }

    @Test
    @DisplayName("should be equal for same v128 values")
    void shouldBeEqualForSameV128Values() {
      final byte[] bytes = new byte[16];
      for (int i = 0; i < 16; i++) {
        bytes[i] = (byte) i;
      }
      final WasmValue value1 = WasmValue.v128(bytes);
      final WasmValue value2 = WasmValue.v128(bytes.clone());
      assertEquals(value1, value2, "Same v128 values should be equal");
      assertEquals(value1.hashCode(), value2.hashCode(), "Hash codes should match");
    }

    @Test
    @DisplayName("should be equal to itself")
    void shouldBeEqualToItself() {
      final WasmValue value = WasmValue.i32(42);
      assertEquals(value, value, "Value should be equal to itself");
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      final WasmValue value = WasmValue.i32(42);
      assertNotEquals(null, value, "Value should not be equal to null");
    }

    @Test
    @DisplayName("should not be equal to different class")
    void shouldNotBeEqualToDifferentClass() {
      final WasmValue value = WasmValue.i32(42);
      assertNotEquals("42", value, "Value should not be equal to different class");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce readable string for i32")
    void shouldProduceReadableStringForI32() {
      final WasmValue value = WasmValue.i32(42);
      final String str = value.toString();
      assertNotNull(str, "String should not be null");
      assertTrue(str.contains("I32"), "Should contain type");
      assertTrue(str.contains("42"), "Should contain value");
    }

    @Test
    @DisplayName("should produce readable string for v128")
    void shouldProduceReadableStringForV128() {
      final byte[] bytes = new byte[16];
      bytes[0] = (byte) 0xAB;
      final WasmValue value = WasmValue.v128(bytes);
      final String str = value.toString();
      assertNotNull(str, "String should not be null");
      assertTrue(str.contains("V128"), "Should contain type");
      assertTrue(str.contains("0x"), "Should contain hex prefix");
    }
  }

  @Nested
  @DisplayName("Type Casting Exception Tests")
  class TypeCastingExceptionTests {

    @Test
    @DisplayName("should throw when accessing i32 as i64")
    void shouldThrowWhenAccessingI32AsI64() {
      final WasmValue value = WasmValue.i32(42);
      assertThrows(ClassCastException.class, value::asLong, "Should throw ClassCastException");
    }

    @Test
    @DisplayName("should throw when accessing i64 as i32")
    void shouldThrowWhenAccessingI64AsI32() {
      final WasmValue value = WasmValue.i64(42L);
      assertThrows(ClassCastException.class, value::asInt, "Should throw ClassCastException");
    }

    @Test
    @DisplayName("should throw when accessing f32 as i32")
    void shouldThrowWhenAccessingF32AsI32() {
      final WasmValue value = WasmValue.f32(1.0f);
      assertThrows(ClassCastException.class, value::asInt, "Should throw ClassCastException");
    }
  }

  @Nested
  @DisplayName("getValue Tests")
  class GetValueTests {

    @Test
    @DisplayName("getValue should return raw value object")
    void getValueShouldReturnRawValueObject() {
      final WasmValue i32Value = WasmValue.i32(42);
      assertEquals(Integer.valueOf(42), i32Value.getValue(), "getValue should return Integer");

      final WasmValue i64Value = WasmValue.i64(42L);
      assertEquals(Long.valueOf(42L), i64Value.getValue(), "getValue should return Long");

      final WasmValue f32Value = WasmValue.f32(3.14f);
      assertEquals(Float.valueOf(3.14f), f32Value.getValue(), "getValue should return Float");

      final WasmValue f64Value = WasmValue.f64(3.14);
      assertEquals(Double.valueOf(3.14), f64Value.getValue(), "getValue should return Double");
    }
  }

  @Nested
  @DisplayName("Null Reference Value Tests")
  class NullReferenceValueTests {

    @Test
    @DisplayName("should create null anyref")
    void shouldCreateNullAnyref() {
      final WasmValue value = WasmValue.nullAnyRef();
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertNull(value.asExternref(), "Value should be null");
    }

    @Test
    @DisplayName("should create null eqref")
    void shouldCreateNullEqref() {
      final WasmValue value = WasmValue.nullEqRef();
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertNull(value.asExternref(), "Value should be null");
    }

    @Test
    @DisplayName("should create null structref")
    void shouldCreateNullStructref() {
      final WasmValue value = WasmValue.nullStructRef();
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertNull(value.asExternref(), "Value should be null");
    }

    @Test
    @DisplayName("should create null arrayref")
    void shouldCreateNullArrayref() {
      final WasmValue value = WasmValue.nullArrayRef();
      assertEquals(WasmValueType.EXTERNREF, value.getType(), "Type should be EXTERNREF");
      assertNull(value.asExternref(), "Value should be null");
    }
  }
}
