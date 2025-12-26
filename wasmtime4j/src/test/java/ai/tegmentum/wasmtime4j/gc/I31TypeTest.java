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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link I31Type} utility class.
 *
 * <p>I31Type provides utilities for working with WebAssembly GC I31 values, which are immediate
 * 31-bit signed integers stored as references.
 */
@DisplayName("I31Type Tests")
class I31TypeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(Modifier.isFinal(I31Type.class.getModifiers()), "I31Type should be final");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor = I31Type.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("should be a utility class")
    void shouldBeAUtilityClass() {
      // Utility class has final class and private constructor
      assertTrue(Modifier.isFinal(I31Type.class.getModifiers()), "Should be final");
    }
  }

  @Nested
  @DisplayName("Constant Tests")
  class ConstantTests {

    @Test
    @DisplayName("MIN_VALUE should be -2^30")
    void minValueShouldBeNegativeTwoToThe30() {
      assertEquals(-(1 << 30), I31Type.MIN_VALUE, "MIN_VALUE should be -2^30");
      assertEquals(-1073741824, I31Type.MIN_VALUE, "MIN_VALUE should be -1073741824");
    }

    @Test
    @DisplayName("MAX_VALUE should be 2^30 - 1")
    void maxValueShouldBeTwoToThe30MinusOne() {
      assertEquals((1 << 30) - 1, I31Type.MAX_VALUE, "MAX_VALUE should be 2^30 - 1");
      assertEquals(1073741823, I31Type.MAX_VALUE, "MAX_VALUE should be 1073741823");
    }

    @Test
    @DisplayName("BIT_WIDTH should be 31")
    void bitWidthShouldBe31() {
      assertEquals(31, I31Type.BIT_WIDTH, "BIT_WIDTH should be 31");
    }
  }

  @Nested
  @DisplayName("isValidValue(int) Tests")
  class IsValidValueIntTests {

    @Test
    @DisplayName("should return true for zero")
    void shouldReturnTrueForZero() {
      assertTrue(I31Type.isValidValue(0), "Zero should be valid");
    }

    @Test
    @DisplayName("should return true for MIN_VALUE")
    void shouldReturnTrueForMinValue() {
      assertTrue(I31Type.isValidValue(I31Type.MIN_VALUE), "MIN_VALUE should be valid");
    }

    @Test
    @DisplayName("should return true for MAX_VALUE")
    void shouldReturnTrueForMaxValue() {
      assertTrue(I31Type.isValidValue(I31Type.MAX_VALUE), "MAX_VALUE should be valid");
    }

    @Test
    @DisplayName("should return false for value greater than MAX_VALUE")
    void shouldReturnFalseForValueGreaterThanMaxValue() {
      assertFalse(I31Type.isValidValue(I31Type.MAX_VALUE + 1), "MAX_VALUE + 1 should be invalid");
      assertFalse(I31Type.isValidValue(Integer.MAX_VALUE), "Integer.MAX_VALUE should be invalid");
    }

    @Test
    @DisplayName("should return false for value less than MIN_VALUE")
    void shouldReturnFalseForValueLessThanMinValue() {
      assertFalse(I31Type.isValidValue(I31Type.MIN_VALUE - 1), "MIN_VALUE - 1 should be invalid");
      assertFalse(I31Type.isValidValue(Integer.MIN_VALUE), "Integer.MIN_VALUE should be invalid");
    }
  }

  @Nested
  @DisplayName("isValidValue(long) Tests")
  class IsValidValueLongTests {

    @Test
    @DisplayName("should return true for zero")
    void shouldReturnTrueForZero() {
      assertTrue(I31Type.isValidValue(0L), "Zero should be valid");
    }

    @Test
    @DisplayName("should return true for values in range")
    void shouldReturnTrueForValuesInRange() {
      assertTrue(I31Type.isValidValue((long) I31Type.MIN_VALUE), "MIN_VALUE should be valid");
      assertTrue(I31Type.isValidValue((long) I31Type.MAX_VALUE), "MAX_VALUE should be valid");
    }

    @Test
    @DisplayName("should return false for values out of range")
    void shouldReturnFalseForValuesOutOfRange() {
      assertFalse(
          I31Type.isValidValue((long) I31Type.MAX_VALUE + 1), "MAX_VALUE + 1 should be invalid");
      assertFalse(
          I31Type.isValidValue((long) I31Type.MIN_VALUE - 1), "MIN_VALUE - 1 should be invalid");
      assertFalse(I31Type.isValidValue(Long.MAX_VALUE), "Long.MAX_VALUE should be invalid");
      assertFalse(I31Type.isValidValue(Long.MIN_VALUE), "Long.MIN_VALUE should be invalid");
    }
  }

  @Nested
  @DisplayName("validateValue(int) Tests")
  class ValidateValueIntTests {

    @Test
    @DisplayName("should return valid value")
    void shouldReturnValidValue() {
      assertEquals(0, I31Type.validateValue(0), "Should return zero");
      assertEquals(42, I31Type.validateValue(42), "Should return 42");
      assertEquals(-42, I31Type.validateValue(-42), "Should return -42");
    }

    @Test
    @DisplayName("should return boundary values")
    void shouldReturnBoundaryValues() {
      assertEquals(
          I31Type.MIN_VALUE, I31Type.validateValue(I31Type.MIN_VALUE), "Should return MIN_VALUE");
      assertEquals(
          I31Type.MAX_VALUE, I31Type.validateValue(I31Type.MAX_VALUE), "Should return MAX_VALUE");
    }

    @Test
    @DisplayName("should throw for values out of range")
    void shouldThrowForValuesOutOfRange() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.validateValue(I31Type.MAX_VALUE + 1),
          "Should throw for MAX_VALUE + 1");

      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.validateValue(I31Type.MIN_VALUE - 1),
          "Should throw for MIN_VALUE - 1");
    }
  }

  @Nested
  @DisplayName("validateValue(long) Tests")
  class ValidateValueLongTests {

    @Test
    @DisplayName("should return valid value as int")
    void shouldReturnValidValueAsInt() {
      assertEquals(0, I31Type.validateValue(0L), "Should return zero");
      assertEquals(42, I31Type.validateValue(42L), "Should return 42");
    }

    @Test
    @DisplayName("should throw for values out of range")
    void shouldThrowForValuesOutOfRange() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.validateValue(Long.MAX_VALUE),
          "Should throw for Long.MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("clampValue Tests")
  class ClampValueTests {

    @Test
    @DisplayName("should not clamp values in range")
    void shouldNotClampValuesInRange() {
      assertEquals(0, I31Type.clampValue(0), "Zero should not be clamped");
      assertEquals(42, I31Type.clampValue(42), "42 should not be clamped");
      assertEquals(-42, I31Type.clampValue(-42), "-42 should not be clamped");
    }

    @Test
    @DisplayName("should clamp values above MAX_VALUE")
    void shouldClampValuesAboveMaxValue() {
      assertEquals(
          I31Type.MAX_VALUE,
          I31Type.clampValue(I31Type.MAX_VALUE + 1),
          "Should clamp to MAX_VALUE");
      assertEquals(
          I31Type.MAX_VALUE,
          I31Type.clampValue(Integer.MAX_VALUE),
          "Should clamp Integer.MAX_VALUE to MAX_VALUE");
    }

    @Test
    @DisplayName("should clamp values below MIN_VALUE")
    void shouldClampValuesBelowMinValue() {
      assertEquals(
          I31Type.MIN_VALUE,
          I31Type.clampValue(I31Type.MIN_VALUE - 1),
          "Should clamp to MIN_VALUE");
      assertEquals(
          I31Type.MIN_VALUE,
          I31Type.clampValue(Integer.MIN_VALUE),
          "Should clamp Integer.MIN_VALUE to MIN_VALUE");
    }

    @Test
    @DisplayName("should clamp long values")
    void shouldClampLongValues() {
      assertEquals(
          I31Type.MAX_VALUE, I31Type.clampValue(Long.MAX_VALUE), "Should clamp Long.MAX_VALUE");
      assertEquals(
          I31Type.MIN_VALUE, I31Type.clampValue(Long.MIN_VALUE), "Should clamp Long.MIN_VALUE");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("getMinValue should return MIN_VALUE")
    void getMinValueShouldReturnMinValue() {
      assertEquals(I31Type.MIN_VALUE, I31Type.getMinValue(), "Should return MIN_VALUE");
    }

    @Test
    @DisplayName("getMaxValue should return MAX_VALUE")
    void getMaxValueShouldReturnMaxValue() {
      assertEquals(I31Type.MAX_VALUE, I31Type.getMaxValue(), "Should return MAX_VALUE");
    }

    @Test
    @DisplayName("getBitWidth should return BIT_WIDTH")
    void getBitWidthShouldReturnBitWidth() {
      assertEquals(I31Type.BIT_WIDTH, I31Type.getBitWidth(), "Should return BIT_WIDTH");
    }

    @Test
    @DisplayName("getRange should return formatted range")
    void getRangeShouldReturnFormattedRange() {
      final String range = I31Type.getRange();
      assertNotNull(range, "Range should not be null");
      assertTrue(range.contains(String.valueOf(I31Type.MIN_VALUE)), "Should contain MIN_VALUE");
      assertTrue(range.contains(String.valueOf(I31Type.MAX_VALUE)), "Should contain MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("toUnsigned Tests")
  class ToUnsignedTests {

    @Test
    @DisplayName("should convert positive values")
    void shouldConvertPositiveValues() {
      assertEquals(0, I31Type.toUnsigned(0), "Zero should convert to 0");
      assertEquals(42, I31Type.toUnsigned(42), "42 should convert to 42");
      assertEquals(
          I31Type.MAX_VALUE,
          I31Type.toUnsigned(I31Type.MAX_VALUE),
          "MAX_VALUE should convert correctly");
    }

    @Test
    @DisplayName("should convert negative values")
    void shouldConvertNegativeValues() {
      final int result = I31Type.toUnsigned(-1);
      assertTrue(result >= 0, "Result should be non-negative");
    }
  }

  @Nested
  @DisplayName("equals Tests")
  class EqualsTests {

    @Test
    @DisplayName("should return true for equal values")
    void shouldReturnTrueForEqualValues() {
      assertTrue(I31Type.equals(0, 0), "Zero should equal zero");
      assertTrue(I31Type.equals(42, 42), "42 should equal 42");
      assertTrue(I31Type.equals(-42, -42), "-42 should equal -42");
    }

    @Test
    @DisplayName("should return false for unequal values")
    void shouldReturnFalseForUnequalValues() {
      assertFalse(I31Type.equals(0, 1), "0 should not equal 1");
      assertFalse(I31Type.equals(42, -42), "42 should not equal -42");
    }
  }

  @Nested
  @DisplayName("compare Tests")
  class CompareTests {

    @Test
    @DisplayName("should return zero for equal values")
    void shouldReturnZeroForEqualValues() {
      assertEquals(0, I31Type.compare(0, 0), "Equal values should compare as 0");
      assertEquals(0, I31Type.compare(42, 42), "Equal values should compare as 0");
    }

    @Test
    @DisplayName("should return negative for smaller first value")
    void shouldReturnNegativeForSmallerFirstValue() {
      assertTrue(I31Type.compare(0, 1) < 0, "0 < 1 should return negative");
      assertTrue(I31Type.compare(-1, 0) < 0, "-1 < 0 should return negative");
    }

    @Test
    @DisplayName("should return positive for larger first value")
    void shouldReturnPositiveForLargerFirstValue() {
      assertTrue(I31Type.compare(1, 0) > 0, "1 > 0 should return positive");
      assertTrue(I31Type.compare(0, -1) > 0, "0 > -1 should return positive");
    }
  }

  @Nested
  @DisplayName("toString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return formatted string")
    void shouldReturnFormattedString() {
      assertEquals("i31(0)", I31Type.toString(0), "Should format zero");
      assertEquals("i31(42)", I31Type.toString(42), "Should format 42");
      assertEquals("i31(-42)", I31Type.toString(-42), "Should format -42");
    }
  }

  @Nested
  @DisplayName("I31Value Wrapper Tests")
  class I31ValueWrapperTests {

    @Test
    @DisplayName("of(int) should create wrapper")
    void ofIntShouldCreateWrapper() {
      final I31Type.I31Value value = I31Type.I31Value.of(42);
      assertNotNull(value, "Should create wrapper");
      assertEquals(42, value.getValue(), "Should have correct value");
    }

    @Test
    @DisplayName("of(long) should create wrapper")
    void ofLongShouldCreateWrapper() {
      final I31Type.I31Value value = I31Type.I31Value.of(42L);
      assertNotNull(value, "Should create wrapper");
      assertEquals(42, value.getValue(), "Should have correct value");
    }

    @Test
    @DisplayName("should throw for invalid values")
    void shouldThrowForInvalidValues() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.I31Value.of(Integer.MAX_VALUE),
          "Should throw for invalid value");
    }

    @Test
    @DisplayName("getUnsigned should return unsigned value")
    void getUnsignedShouldReturnUnsignedValue() {
      final I31Type.I31Value value = I31Type.I31Value.of(42);
      assertEquals(42, value.getUnsigned(), "Should return unsigned value");
    }

    @Test
    @DisplayName("equals should work correctly")
    void equalsShouldWorkCorrectly() {
      final I31Type.I31Value value1 = I31Type.I31Value.of(42);
      final I31Type.I31Value value2 = I31Type.I31Value.of(42);
      final I31Type.I31Value value3 = I31Type.I31Value.of(43);

      assertEquals(value1, value2, "Equal values should be equal");
      assertNotEquals(value1, value3, "Different values should not be equal");
      assertEquals(value1, value1, "Same object should be equal");
      assertNotEquals(value1, null, "Should not equal null");
      assertNotEquals(value1, "42", "Should not equal different type");
    }

    @Test
    @DisplayName("hashCode should be consistent with equals")
    void hashCodeShouldBeConsistentWithEquals() {
      final I31Type.I31Value value1 = I31Type.I31Value.of(42);
      final I31Type.I31Value value2 = I31Type.I31Value.of(42);

      assertEquals(value1.hashCode(), value2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("toString should return formatted string")
    void toStringShouldReturnFormattedString() {
      final I31Type.I31Value value = I31Type.I31Value.of(42);
      assertEquals("i31(42)", value.toString(), "Should return formatted string");
    }
  }
}
