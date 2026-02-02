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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WitMarshallingException;
import ai.tegmentum.wasmtime4j.exception.WitRangeException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitPrimitiveValue} abstract class.
 *
 * <p>WitPrimitiveValue is the base class for all WIT primitive scalar types. Since it is abstract,
 * tests exercise behavior through the concrete subclass WitS32.
 */
@DisplayName("WitPrimitiveValue Tests")
class WitPrimitiveValueTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be abstract")
    void shouldBeAbstract() {
      assertTrue(
          Modifier.isAbstract(WitPrimitiveValue.class.getModifiers()),
          "WitPrimitiveValue should be abstract");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitPrimitiveValue.class),
          "WitPrimitiveValue should extend WitValue");
    }

    @Test
    @DisplayName("should have isNumeric method")
    void shouldHaveIsNumericMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isNumeric");
      assertNotNull(method, "Should have isNumeric() method");
    }

    @Test
    @DisplayName("should have isInteger method")
    void shouldHaveIsIntegerMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isInteger");
      assertNotNull(method, "Should have isInteger() method");
    }

    @Test
    @DisplayName("should have isFloatingPoint method")
    void shouldHaveIsFloatingPointMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isFloatingPoint");
      assertNotNull(method, "Should have isFloatingPoint() method");
    }

    @Test
    @DisplayName("should have isUnsigned method")
    void shouldHaveIsUnsignedMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isUnsigned");
      assertNotNull(method, "Should have isUnsigned() method");
    }

    @Test
    @DisplayName("should have isSigned method")
    void shouldHaveIsSignedMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isSigned");
      assertNotNull(method, "Should have isSigned() method");
    }
  }

  @Nested
  @DisplayName("Numeric Classification Tests via WitS32")
  class NumericClassificationTests {

    @Test
    @DisplayName("WitS32 should be numeric")
    void witS32ShouldBeNumeric() {
      final WitS32 value = WitS32.of(42);
      assertTrue(value.isNumeric(), "WitS32 should be numeric");
    }

    @Test
    @DisplayName("WitS32 should be integer")
    void witS32ShouldBeInteger() {
      final WitS32 value = WitS32.of(42);
      assertTrue(value.isInteger(), "WitS32 should be an integer");
    }

    @Test
    @DisplayName("WitS32 should not be floating point")
    void witS32ShouldNotBeFloatingPoint() {
      final WitS32 value = WitS32.of(42);
      assertFalse(value.isFloatingPoint(), "WitS32 should not be floating point");
    }

    @Test
    @DisplayName("WitS32 should be signed")
    void witS32ShouldBeSigned() {
      final WitS32 value = WitS32.of(42);
      assertTrue(value.isSigned(), "WitS32 should be signed");
    }

    @Test
    @DisplayName("WitS32 should not be unsigned")
    void witS32ShouldNotBeUnsigned() {
      final WitS32 value = WitS32.of(42);
      assertFalse(value.isUnsigned(), "WitS32 should not be unsigned");
    }
  }

  @Nested
  @DisplayName("Type Inheritance Tests")
  class TypeInheritanceTests {

    @Test
    @DisplayName("WitS32 should be assignable to WitPrimitiveValue")
    void witS32ShouldBeAssignableToPrimitive() {
      final WitPrimitiveValue pv = WitS32.of(42);
      assertNotNull(pv, "WitS32 should be assignable to WitPrimitiveValue");
    }

    @Test
    @DisplayName("WitS32 should be assignable to WitValue")
    void witS32ShouldBeAssignableToWitValue() {
      final WitValue wv = WitS32.of(42);
      assertNotNull(wv, "WitS32 should be assignable to WitValue");
    }

    @Test
    @DisplayName("primitive value should have a type")
    void primitiveValueShouldHaveType() {
      final WitPrimitiveValue pv = WitS32.of(42);
      assertNotNull(pv.getType(), "Primitive value should have a type");
    }
  }

  @Nested
  @DisplayName("Boolean Type Tests")
  class BooleanTypeTests {

    @Test
    @DisplayName("WitBool should not be numeric")
    void witBoolShouldNotBeNumeric() {
      final WitBool value = WitBool.of(true);
      assertFalse(value.isNumeric(), "WitBool should not be numeric");
      assertFalse(value.isInteger(), "WitBool should not be integer");
      assertFalse(value.isFloatingPoint(), "WitBool should not be floating point");
      assertFalse(value.isSigned(), "WitBool should not be signed");
      assertFalse(value.isUnsigned(), "WitBool should not be unsigned");
    }

    @Test
    @DisplayName("WitBool true should return true value")
    void witBoolTrueShouldReturnTrueValue() {
      final WitBool value = WitBool.of(true);
      assertTrue(value.getValue(), "WitBool.of(true).getValue() should be true");
      assertEquals(Boolean.TRUE, value.toJava(), "toJava() should return Boolean.TRUE");
    }

    @Test
    @DisplayName("WitBool false should return false value")
    void witBoolFalseShouldReturnFalseValue() {
      final WitBool value = WitBool.of(false);
      assertFalse(value.getValue(), "WitBool.of(false).getValue() should be false");
      assertEquals(Boolean.FALSE, value.toJava(), "toJava() should return Boolean.FALSE");
    }

    @Test
    @DisplayName("WitBool should cache TRUE and FALSE constants")
    void witBoolShouldCacheConstants() {
      assertSame(WitBool.of(true), WitBool.of(true), "WitBool.of(true) should return same instance");
      assertSame(
          WitBool.of(false), WitBool.of(false), "WitBool.of(false) should return same instance");
    }

    @Test
    @DisplayName("WitBool equals and hashCode should be value-based")
    void witBoolEqualsAndHashCode() {
      final WitBool trueVal = WitBool.of(true);
      final WitBool falseVal = WitBool.of(false);
      assertEquals(trueVal, WitBool.of(true), "Same boolean value should be equal");
      assertNotEquals(trueVal, falseVal, "Different boolean values should not be equal");
      assertEquals(
          trueVal.hashCode(),
          WitBool.of(true).hashCode(),
          "Same boolean value should have same hashCode");
    }

    @Test
    @DisplayName("WitBool toString should include value")
    void witBoolToStringShouldIncludeValue() {
      assertTrue(
          WitBool.of(true).toString().contains("true"),
          "toString() for true should contain 'true'");
      assertTrue(
          WitBool.of(false).toString().contains("false"),
          "toString() for false should contain 'false'");
    }
  }

  @Nested
  @DisplayName("Signed Integer Type Tests")
  class SignedIntegerTypeTests {

    @Test
    @DisplayName("WitS8 should classify as signed integer")
    void witS8ShouldClassifyAsSignedInteger() {
      final WitS8 value = WitS8.of((byte) -128);
      assertTrue(value.isNumeric(), "WitS8 should be numeric");
      assertTrue(value.isInteger(), "WitS8 should be integer");
      assertFalse(value.isFloatingPoint(), "WitS8 should not be floating point");
      assertTrue(value.isSigned(), "WitS8 should be signed");
      assertFalse(value.isUnsigned(), "WitS8 should not be unsigned");
    }

    @Test
    @DisplayName("WitS8 should preserve byte value")
    void witS8ShouldPreserveValue() {
      final WitS8 value = WitS8.of((byte) -42);
      assertEquals((byte) -42, value.getValue(), "getValue() should return original byte");
      assertEquals(Byte.valueOf((byte) -42), value.toJava(), "toJava() should return Byte");
    }

    @Test
    @DisplayName("WitS16 should preserve short value")
    void witS16ShouldPreserveValue() {
      final WitS16 value = WitS16.of((short) -30000);
      assertEquals((short) -30000, value.getValue(), "getValue() should return original short");
      assertEquals(Short.valueOf((short) -30000), value.toJava(), "toJava() should return Short");
      assertTrue(value.isSigned(), "WitS16 should be signed");
    }

    @Test
    @DisplayName("WitS64 should preserve long value")
    void witS64ShouldPreserveValue() {
      final WitS64 value = WitS64.of(Long.MIN_VALUE);
      assertEquals(Long.MIN_VALUE, value.getValue(), "getValue() should return original long");
      assertEquals(Long.valueOf(Long.MIN_VALUE), value.toJava(), "toJava() should return Long");
      assertTrue(value.isSigned(), "WitS64 should be signed");
    }

    @Test
    @DisplayName("WitS32 equals should distinguish different values")
    void witS32EqualsShouldDistinguishValues() {
      final WitS32 a = WitS32.of(42);
      final WitS32 b = WitS32.of(42);
      final WitS32 c = WitS32.of(99);
      assertEquals(a, b, "Same int value should be equal");
      assertNotEquals(a, c, "Different int values should not be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Equal values should have same hashCode");
    }

    @Test
    @DisplayName("WitS32 should not equal different WitPrimitiveValue types")
    void witS32ShouldNotEqualDifferentTypes() {
      final WitS32 s32 = WitS32.of(42);
      final WitS64 s64 = WitS64.of(42L);
      assertNotEquals(s32, s64, "WitS32 should not equal WitS64 even with same numeric value");
    }
  }

  @Nested
  @DisplayName("Unsigned Integer Type Tests")
  class UnsignedIntegerTypeTests {

    @Test
    @DisplayName("WitU8 should classify as unsigned integer")
    void witU8ShouldClassifyAsUnsignedInteger() {
      final WitU8 value = WitU8.of((byte) 0xFF);
      assertTrue(value.isNumeric(), "WitU8 should be numeric");
      assertTrue(value.isInteger(), "WitU8 should be integer");
      assertFalse(value.isFloatingPoint(), "WitU8 should not be floating point");
      assertFalse(value.isSigned(), "WitU8 should not be signed");
      assertTrue(value.isUnsigned(), "WitU8 should be unsigned");
    }

    @Test
    @DisplayName("WitU8 toUnsignedInt should return positive value for negative byte")
    void witU8ToUnsignedIntShouldReturnPositive() {
      final WitU8 value = WitU8.of((byte) 0xFF);
      assertEquals(255, value.toUnsignedInt(), "0xFF as unsigned should be 255");
    }

    @Test
    @DisplayName("WitU16 toUnsignedInt should return positive value")
    void witU16ToUnsignedIntShouldReturnPositive() {
      final WitU16 value = WitU16.of((short) 0xFFFF);
      assertEquals(65535, value.toUnsignedInt(), "0xFFFF as unsigned should be 65535");
      assertTrue(value.isUnsigned(), "WitU16 should be unsigned");
    }

    @Test
    @DisplayName("WitU32 toUnsignedLong should return positive value")
    void witU32ToUnsignedLongShouldReturnPositive() {
      final WitU32 value = WitU32.of(0xFFFFFFFF);
      assertEquals(4294967295L, value.toUnsignedLong(), "0xFFFFFFFF as unsigned should be max u32");
      assertTrue(value.isUnsigned(), "WitU32 should be unsigned");
    }

    @Test
    @DisplayName("WitU64 toUnsignedBigInteger should return positive value")
    void witU64ToUnsignedBigIntegerShouldReturnPositive() {
      final WitU64 value = WitU64.of(-1L);
      assertEquals(
          java.math.BigInteger.ONE.shiftLeft(64).subtract(java.math.BigInteger.ONE),
          value.toUnsignedBigInteger(),
          "-1L as unsigned u64 should be 2^64 - 1");
      assertTrue(value.isUnsigned(), "WitU64 should be unsigned");
    }

    @Test
    @DisplayName("WitU32 equals should be value-based")
    void witU32EqualsShouldBeValueBased() {
      final WitU32 a = WitU32.of(100);
      final WitU32 b = WitU32.of(100);
      assertEquals(a, b, "Same u32 value should be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Same u32 value should have same hashCode");
    }
  }

  @Nested
  @DisplayName("Floating Point Type Tests")
  class FloatingPointTypeTests {

    @Test
    @DisplayName("WitFloat32 should classify as floating point")
    void witFloat32ShouldClassifyAsFloatingPoint() {
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertTrue(value.isNumeric(), "WitFloat32 should be numeric");
      assertFalse(value.isInteger(), "WitFloat32 should not be integer");
      assertTrue(value.isFloatingPoint(), "WitFloat32 should be floating point");
      assertFalse(value.isSigned(), "WitFloat32 should not be signed");
      assertFalse(value.isUnsigned(), "WitFloat32 should not be unsigned");
    }

    @Test
    @DisplayName("WitFloat32 should preserve float value")
    void witFloat32ShouldPreserveValue() {
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertEquals(3.14f, value.getValue(), 0.001f, "getValue() should return original float");
      assertEquals(Float.valueOf(3.14f), value.toJava(), "toJava() should return Float");
    }

    @Test
    @DisplayName("WitFloat64 should classify as floating point")
    void witFloat64ShouldClassifyAsFloatingPoint() {
      final WitFloat64 value = WitFloat64.of(2.718281828);
      assertTrue(value.isNumeric(), "WitFloat64 should be numeric");
      assertFalse(value.isInteger(), "WitFloat64 should not be integer");
      assertTrue(value.isFloatingPoint(), "WitFloat64 should be floating point");
    }

    @Test
    @DisplayName("WitFloat64 should preserve double value")
    void witFloat64ShouldPreserveValue() {
      final WitFloat64 value = WitFloat64.of(Double.MAX_VALUE);
      assertEquals(
          Double.MAX_VALUE, value.getValue(), 0.0, "getValue() should return original double");
      assertEquals(
          Double.valueOf(Double.MAX_VALUE), value.toJava(), "toJava() should return Double");
    }

    @Test
    @DisplayName("WitFloat32 equals should handle NaN correctly")
    void witFloat32EqualsShouldHandleNan() {
      final WitFloat32 nan1 = WitFloat32.of(Float.NaN);
      final WitFloat32 nan2 = WitFloat32.of(Float.NaN);
      assertEquals(nan1, nan2, "Float NaN values should be equal via floatToIntBits");
    }

    @Test
    @DisplayName("WitFloat64 equals should handle special values")
    void witFloat64EqualsShouldHandleSpecialValues() {
      final WitFloat64 posInf = WitFloat64.of(Double.POSITIVE_INFINITY);
      final WitFloat64 negInf = WitFloat64.of(Double.NEGATIVE_INFINITY);
      assertEquals(posInf, WitFloat64.of(Double.POSITIVE_INFINITY), "+Inf should equal +Inf");
      assertNotEquals(posInf, negInf, "+Inf should not equal -Inf");
    }
  }

  @Nested
  @DisplayName("WitChar Type Tests")
  class WitCharTypeTests {

    @Test
    @DisplayName("WitChar should not be numeric")
    void witCharShouldNotBeNumeric() throws WitRangeException {
      final WitChar value = WitChar.of('A');
      assertFalse(value.isNumeric(), "WitChar should not be numeric");
      assertFalse(value.isInteger(), "WitChar should not be integer");
      assertFalse(value.isFloatingPoint(), "WitChar should not be floating point");
    }

    @Test
    @DisplayName("WitChar should preserve codepoint")
    void witCharShouldPreserveCodepoint() throws WitRangeException {
      final WitChar value = WitChar.of(0x1F600);
      assertEquals(0x1F600, value.getCodepoint(), "getCodepoint() should return original value");
    }

    @Test
    @DisplayName("WitChar should reject surrogate codepoints")
    void witCharShouldRejectSurrogates() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xD800),
          "Should reject low surrogate 0xD800");
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0xDFFF),
          "Should reject high surrogate 0xDFFF");
    }

    @Test
    @DisplayName("WitChar should reject out-of-range codepoints")
    void witCharShouldRejectOutOfRange() {
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(0x110000),
          "Should reject codepoint above U+10FFFF");
      assertThrows(
          WitRangeException.class,
          () -> WitChar.of(-1),
          "Should reject negative codepoint");
    }

    @Test
    @DisplayName("WitChar toString should show unicode codepoint")
    void witCharToStringShouldShowUnicode() throws WitRangeException {
      final String str = WitChar.of('A').toString();
      assertTrue(
          str.contains("0041") || str.contains("41"),
          "toString() should contain hex codepoint: " + str);
    }
  }

  @Nested
  @DisplayName("WitString Type Tests")
  class WitStringTypeTests {

    @Test
    @DisplayName("WitString should not be numeric")
    void witStringShouldNotBeNumeric() throws WitMarshallingException {
      final WitString value = WitString.of("hello");
      assertFalse(value.isNumeric(), "WitString should not be numeric");
      assertFalse(value.isInteger(), "WitString should not be integer");
      assertFalse(value.isFloatingPoint(), "WitString should not be floating point");
    }

    @Test
    @DisplayName("WitString should preserve string value")
    void witStringShouldPreserveValue() throws WitMarshallingException {
      final WitString value = WitString.of("hello world");
      assertEquals("hello world", value.getValue(), "getValue() should return original string");
      assertEquals("hello world", value.toJava(), "toJava() should return String");
    }

    @Test
    @DisplayName("WitString should reject null")
    void witStringShouldRejectNull() {
      assertThrows(
          WitMarshallingException.class,
          () -> WitString.of(null),
          "Should reject null string");
    }

    @Test
    @DisplayName("WitString equals should be value-based")
    void witStringEqualsShouldBeValueBased() throws WitMarshallingException {
      final WitString a = WitString.of("test");
      final WitString b = WitString.of("test");
      final WitString c = WitString.of("other");
      assertEquals(a, b, "Same string value should be equal");
      assertNotEquals(a, c, "Different string values should not be equal");
      assertEquals(a.hashCode(), b.hashCode(), "Same string should have same hashCode");
    }

    @Test
    @DisplayName("WitString should handle empty string")
    void witStringShouldHandleEmptyString() throws WitMarshallingException {
      final WitString value = WitString.of("");
      assertEquals("", value.getValue(), "Empty string should be preserved");
    }
  }

  @Nested
  @DisplayName("Equality Edge Case Tests")
  class EqualityEdgeCaseTests {

    @Test
    @DisplayName("WitS32 equals should return true for same reference")
    void witS32EqualsShouldReturnTrueForSameReference() {
      final WitS32 value = WitS32.of(42);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitS32 equals should return false for null")
    void witS32EqualsShouldReturnFalseForNull() {
      final WitS32 value = WitS32.of(42);
      assertNotEquals(null, value, "WitS32 should not equal null");
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitS32 equals should return false for different class type")
    void witS32EqualsShouldReturnFalseForDifferentClassType() {
      final WitS32 value = WitS32.of(42);
      assertFalse(value.equals("42"), "WitS32 should not equal String");
      assertFalse(value.equals(Integer.valueOf(42)), "WitS32 should not equal Integer");
    }

    @Test
    @DisplayName("WitBool equals should return true for same reference")
    void witBoolEqualsShouldReturnTrueForSameReference() {
      final WitBool value = WitBool.of(true);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitBool equals should return false for null")
    void witBoolEqualsShouldReturnFalseForNull() {
      final WitBool value = WitBool.of(true);
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitBool equals should return false for different class type")
    void witBoolEqualsShouldReturnFalseForDifferentClassType() {
      final WitBool value = WitBool.of(true);
      assertFalse(value.equals(Boolean.TRUE), "WitBool should not equal Boolean");
      assertFalse(value.equals("true"), "WitBool should not equal String");
    }

    @Test
    @DisplayName("WitS64 equals should return true for same reference")
    void witS64EqualsShouldReturnTrueForSameReference() {
      final WitS64 value = WitS64.of(Long.MAX_VALUE);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitS64 equals should return false for null")
    void witS64EqualsShouldReturnFalseForNull() {
      final WitS64 value = WitS64.of(42L);
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitFloat32 equals should return true for same reference")
    void witFloat32EqualsShouldReturnTrueForSameReference() {
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitFloat32 equals should return false for null")
    void witFloat32EqualsShouldReturnFalseForNull() {
      final WitFloat32 value = WitFloat32.of(3.14f);
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitFloat64 equals should return true for same reference")
    void witFloat64EqualsShouldReturnTrueForSameReference() {
      final WitFloat64 value = WitFloat64.of(2.718);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitFloat64 equals should return false for null")
    void witFloat64EqualsShouldReturnFalseForNull() {
      final WitFloat64 value = WitFloat64.of(2.718);
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitU32 equals should return true for same reference")
    void witU32EqualsShouldReturnTrueForSameReference() {
      final WitU32 value = WitU32.of(100);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitU32 equals should return false for null")
    void witU32EqualsShouldReturnFalseForNull() {
      final WitU32 value = WitU32.of(100);
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitU64 equals should return true for same reference")
    void witU64EqualsShouldReturnTrueForSameReference() {
      final WitU64 value = WitU64.of(100L);
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitU64 equals should return false for null")
    void witU64EqualsShouldReturnFalseForNull() {
      final WitU64 value = WitU64.of(100L);
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitString equals should return true for same reference")
    void witStringEqualsShouldReturnTrueForSameReference() throws WitMarshallingException {
      final WitString value = WitString.of("test");
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitString equals should return false for null")
    void witStringEqualsShouldReturnFalseForNull() throws WitMarshallingException {
      final WitString value = WitString.of("test");
      assertFalse(value.equals(null), "equals(null) should return false");
    }

    @Test
    @DisplayName("WitChar equals should return true for same reference")
    void witCharEqualsShouldReturnTrueForSameReference() throws WitRangeException {
      final WitChar value = WitChar.of('A');
      assertEquals(value, value, "Same reference should be equal to itself");
    }

    @Test
    @DisplayName("WitChar equals should return false for null")
    void witCharEqualsShouldReturnFalseForNull() throws WitRangeException {
      final WitChar value = WitChar.of('A');
      assertFalse(value.equals(null), "equals(null) should return false");
    }
  }

  @Nested
  @DisplayName("HashCode Consistency Tests")
  class HashCodeConsistencyTests {

    @Test
    @DisplayName("WitS32 hashCode should be consistent")
    void witS32HashCodeShouldBeConsistent() {
      final WitS32 value = WitS32.of(42);
      final int hash1 = value.hashCode();
      final int hash2 = value.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent on repeated calls");
    }

    @Test
    @DisplayName("WitBool hashCode should be consistent")
    void witBoolHashCodeShouldBeConsistent() {
      final WitBool value = WitBool.of(true);
      final int hash1 = value.hashCode();
      final int hash2 = value.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent on repeated calls");
    }

    @Test
    @DisplayName("WitFloat32 hashCode should be consistent for special values")
    void witFloat32HashCodeShouldBeConsistentForSpecialValues() {
      final WitFloat32 nan = WitFloat32.of(Float.NaN);
      assertEquals(nan.hashCode(), nan.hashCode(), "NaN hashCode should be consistent");

      final WitFloat32 posInf = WitFloat32.of(Float.POSITIVE_INFINITY);
      assertEquals(posInf.hashCode(), posInf.hashCode(), "POSITIVE_INFINITY hashCode should be consistent");

      // Note: -0.0f and 0.0f have different bit representations, so different hashCode
      final WitFloat32 negZero = WitFloat32.of(-0.0f);
      final WitFloat32 posZero = WitFloat32.of(0.0f);
      assertNotEquals(negZero.hashCode(), posZero.hashCode(), "-0.0f and 0.0f have different bit representations");
    }

    @Test
    @DisplayName("WitFloat64 hashCode should be consistent for special values")
    void witFloat64HashCodeShouldBeConsistentForSpecialValues() {
      final WitFloat64 nan = WitFloat64.of(Double.NaN);
      assertEquals(nan.hashCode(), nan.hashCode(), "NaN hashCode should be consistent");

      // Note: -0.0 and 0.0 have different bit representations, so different hashCode
      final WitFloat64 negZero = WitFloat64.of(-0.0);
      final WitFloat64 posZero = WitFloat64.of(0.0);
      assertNotEquals(negZero.hashCode(), posZero.hashCode(), "-0.0 and 0.0 have different bit representations");
    }
  }

  @Nested
  @DisplayName("Boundary Value Tests")
  class BoundaryValueTests {

    @Test
    @DisplayName("WitS8 should handle all boundary values")
    void witS8ShouldHandleAllBoundaryValues() {
      assertEquals(Byte.MIN_VALUE, WitS8.of(Byte.MIN_VALUE).getValue(), "Should handle MIN_VALUE");
      assertEquals(Byte.MAX_VALUE, WitS8.of(Byte.MAX_VALUE).getValue(), "Should handle MAX_VALUE");
      assertEquals((byte) 0, WitS8.of((byte) 0).getValue(), "Should handle 0");
      assertEquals((byte) -1, WitS8.of((byte) -1).getValue(), "Should handle -1");
      assertEquals((byte) 1, WitS8.of((byte) 1).getValue(), "Should handle 1");
    }

    @Test
    @DisplayName("WitS16 should handle all boundary values")
    void witS16ShouldHandleAllBoundaryValues() {
      assertEquals(Short.MIN_VALUE, WitS16.of(Short.MIN_VALUE).getValue(), "Should handle MIN_VALUE");
      assertEquals(Short.MAX_VALUE, WitS16.of(Short.MAX_VALUE).getValue(), "Should handle MAX_VALUE");
      assertEquals((short) 0, WitS16.of((short) 0).getValue(), "Should handle 0");
      assertEquals((short) -1, WitS16.of((short) -1).getValue(), "Should handle -1");
      assertEquals((short) 1, WitS16.of((short) 1).getValue(), "Should handle 1");
    }

    @Test
    @DisplayName("WitU8 toUnsignedInt should handle all boundary values")
    void witU8ToUnsignedIntShouldHandleAllBoundaryValues() {
      assertEquals(0, WitU8.of((byte) 0).toUnsignedInt(), "Should handle 0");
      assertEquals(1, WitU8.of((byte) 1).toUnsignedInt(), "Should handle 1");
      assertEquals(127, WitU8.of((byte) 127).toUnsignedInt(), "Should handle 127");
      assertEquals(128, WitU8.of((byte) -128).toUnsignedInt(), "Should handle 128 (as -128)");
      assertEquals(255, WitU8.of((byte) -1).toUnsignedInt(), "Should handle 255 (as -1)");
    }

    @Test
    @DisplayName("WitU16 toUnsignedInt should handle all boundary values")
    void witU16ToUnsignedIntShouldHandleAllBoundaryValues() {
      assertEquals(0, WitU16.of((short) 0).toUnsignedInt(), "Should handle 0");
      assertEquals(1, WitU16.of((short) 1).toUnsignedInt(), "Should handle 1");
      assertEquals(32767, WitU16.of((short) 32767).toUnsignedInt(), "Should handle 32767");
      assertEquals(32768, WitU16.of((short) -32768).toUnsignedInt(), "Should handle 32768 (as -32768)");
      assertEquals(65535, WitU16.of((short) -1).toUnsignedInt(), "Should handle 65535 (as -1)");
    }

    @Test
    @DisplayName("WitU32 toUnsignedLong should handle all boundary values")
    void witU32ToUnsignedLongShouldHandleAllBoundaryValues() {
      assertEquals(0L, WitU32.of(0).toUnsignedLong(), "Should handle 0");
      assertEquals(1L, WitU32.of(1).toUnsignedLong(), "Should handle 1");
      assertEquals(2147483647L, WitU32.of(Integer.MAX_VALUE).toUnsignedLong(), "Should handle MAX_VALUE");
      assertEquals(2147483648L, WitU32.of(Integer.MIN_VALUE).toUnsignedLong(), "Should handle 2147483648");
      assertEquals(4294967295L, WitU32.of(-1).toUnsignedLong(), "Should handle max u32");
    }
  }

  @Nested
  @DisplayName("Type Classification Edge Cases")
  class TypeClassificationEdgeCases {

    @Test
    @DisplayName("WitFloat32 and WitFloat64 are numeric but not signed or unsigned")
    void floatingPointShouldBeNumericButNotSignedOrUnsigned() {
      final WitFloat32 f32 = WitFloat32.of(1.0f);
      final WitFloat64 f64 = WitFloat64.of(1.0);

      assertTrue(f32.isNumeric(), "Float32 should be numeric");
      assertTrue(f64.isNumeric(), "Float64 should be numeric");
      assertTrue(f32.isFloatingPoint(), "Float32 should be floating point");
      assertTrue(f64.isFloatingPoint(), "Float64 should be floating point");
      assertFalse(f32.isInteger(), "Float32 should not be integer");
      assertFalse(f64.isInteger(), "Float64 should not be integer");
      assertFalse(f32.isSigned(), "Float32 should not be signed integer");
      assertFalse(f64.isSigned(), "Float64 should not be signed integer");
      assertFalse(f32.isUnsigned(), "Float32 should not be unsigned integer");
      assertFalse(f64.isUnsigned(), "Float64 should not be unsigned integer");
    }

    @Test
    @DisplayName("WitChar and WitString are not numeric")
    void charAndStringShouldNotBeNumeric() throws WitRangeException, WitMarshallingException {
      final WitChar c = WitChar.of('A');
      final WitString s = WitString.of("test");

      assertFalse(c.isNumeric(), "Char should not be numeric");
      assertFalse(s.isNumeric(), "String should not be numeric");
      assertFalse(c.isInteger(), "Char should not be integer");
      assertFalse(s.isInteger(), "String should not be integer");
      assertFalse(c.isFloatingPoint(), "Char should not be floating point");
      assertFalse(s.isFloatingPoint(), "String should not be floating point");
      assertFalse(c.isSigned(), "Char should not be signed");
      assertFalse(s.isSigned(), "String should not be signed");
      assertFalse(c.isUnsigned(), "Char should not be unsigned");
      assertFalse(s.isUnsigned(), "String should not be unsigned");
    }

    @Test
    @DisplayName("All signed integer types should report isSigned true")
    void allSignedIntegerTypesShouldReportIsSignedTrue() {
      assertTrue(WitS8.of((byte) 0).isSigned(), "S8 should be signed");
      assertTrue(WitS16.of((short) 0).isSigned(), "S16 should be signed");
      assertTrue(WitS32.of(0).isSigned(), "S32 should be signed");
      assertTrue(WitS64.of(0L).isSigned(), "S64 should be signed");
    }

    @Test
    @DisplayName("All unsigned integer types should report isUnsigned true")
    void allUnsignedIntegerTypesShouldReportIsUnsignedTrue() {
      assertTrue(WitU8.of((byte) 0).isUnsigned(), "U8 should be unsigned");
      assertTrue(WitU16.of((short) 0).isUnsigned(), "U16 should be unsigned");
      assertTrue(WitU32.of(0).isUnsigned(), "U32 should be unsigned");
      assertTrue(WitU64.of(0L).isUnsigned(), "U64 should be unsigned");
    }
  }
}
