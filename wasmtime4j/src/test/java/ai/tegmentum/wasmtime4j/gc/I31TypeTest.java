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

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link I31Type} utility class.
 *
 * <p>I31Type provides utilities for working with 31-bit signed integer values stored as references.
 */
@DisplayName("I31Type Tests")
class I31TypeTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(I31Type.class.getModifiers()), "I31Type should be final");
    }

    @Test
    @DisplayName("should not be instantiable")
    void shouldNotBeInstantiable() throws NoSuchMethodException {
      final var constructor = I31Type.class.getDeclaredConstructor();
      assertTrue(Modifier.isPrivate(constructor.getModifiers()), "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Constants Tests")
  class ConstantsTests {

    @Test
    @DisplayName("MIN_VALUE should be -2^30")
    void minValueShouldBeNeg2To30() {
      assertEquals(-(1 << 30), I31Type.MIN_VALUE, "MIN_VALUE should be -(1 << 30)");
    }

    @Test
    @DisplayName("MAX_VALUE should be 2^30 - 1")
    void maxValueShouldBe2To30Minus1() {
      assertEquals((1 << 30) - 1, I31Type.MAX_VALUE, "MAX_VALUE should be (1 << 30) - 1");
    }

    @Test
    @DisplayName("BIT_WIDTH should be 31")
    void bitWidthShouldBe31() {
      assertEquals(31, I31Type.BIT_WIDTH, "BIT_WIDTH should be 31");
    }
  }

  @Nested
  @DisplayName("IsValidValue Tests")
  class IsValidValueTests {

    @Test
    @DisplayName("zero should be valid")
    void zeroShouldBeValid() {
      assertTrue(I31Type.isValidValue(0), "Zero should be valid");
    }

    @Test
    @DisplayName("MIN_VALUE should be valid")
    void minValueShouldBeValid() {
      assertTrue(I31Type.isValidValue(I31Type.MIN_VALUE), "MIN_VALUE should be valid");
    }

    @Test
    @DisplayName("MAX_VALUE should be valid")
    void maxValueShouldBeValid() {
      assertTrue(I31Type.isValidValue(I31Type.MAX_VALUE), "MAX_VALUE should be valid");
    }

    @Test
    @DisplayName("value below MIN_VALUE should be invalid")
    void valueBelowMinShouldBeInvalid() {
      assertFalse(
          I31Type.isValidValue(I31Type.MIN_VALUE - 1), "Value below MIN_VALUE should be invalid");
    }

    @Test
    @DisplayName("value above MAX_VALUE should be invalid")
    void valueAboveMaxShouldBeInvalid() {
      assertFalse(
          I31Type.isValidValue(I31Type.MAX_VALUE + 1), "Value above MAX_VALUE should be invalid");
    }

    @Test
    @DisplayName("long zero should be valid")
    void longZeroShouldBeValid() {
      assertTrue(I31Type.isValidValue(0L), "Long zero should be valid");
    }

    @Test
    @DisplayName("long value above range should be invalid")
    void longValueAboveRangeShouldBeInvalid() {
      assertFalse(I31Type.isValidValue(Long.MAX_VALUE), "Long.MAX_VALUE should be invalid for I31");
    }
  }

  @Nested
  @DisplayName("ValidateValue Tests")
  class ValidateValueTests {

    @Test
    @DisplayName("valid int should return same value")
    void validIntShouldReturnSameValue() {
      assertEquals(42, I31Type.validateValue(42), "validateValue should return same value");
    }

    @Test
    @DisplayName("invalid int should throw IllegalArgumentException")
    void invalidIntShouldThrowIae() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.validateValue(Integer.MAX_VALUE),
          "Out of range value should throw IllegalArgumentException");
    }

    @Test
    @DisplayName("valid long should return int")
    void validLongShouldReturnInt() {
      assertEquals(100, I31Type.validateValue(100L), "validateValue(long) should return int");
    }

    @Test
    @DisplayName("invalid long should throw IllegalArgumentException")
    void invalidLongShouldThrowIae() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.validateValue(Long.MAX_VALUE),
          "Out of range long value should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("ClampValue Tests")
  class ClampValueTests {

    @Test
    @DisplayName("in-range value should not be clamped")
    void inRangeValueShouldNotBeClamped() {
      assertEquals(42, I31Type.clampValue(42), "In-range value should not change");
    }

    @Test
    @DisplayName("value below MIN should clamp to MIN")
    void valueBelowMinShouldClampToMin() {
      assertEquals(
          I31Type.MIN_VALUE,
          I31Type.clampValue(Integer.MIN_VALUE),
          "Below-min should clamp to MIN_VALUE");
    }

    @Test
    @DisplayName("value above MAX should clamp to MAX")
    void valueAboveMaxShouldClampToMax() {
      assertEquals(
          I31Type.MAX_VALUE,
          I31Type.clampValue(Integer.MAX_VALUE),
          "Above-max should clamp to MAX_VALUE");
    }

    @Test
    @DisplayName("long value below MIN should clamp to MIN")
    void longValueBelowMinShouldClampToMin() {
      assertEquals(
          I31Type.MIN_VALUE,
          I31Type.clampValue(Long.MIN_VALUE),
          "Long below-min should clamp to MIN_VALUE");
    }

    @Test
    @DisplayName("long value above MAX should clamp to MAX")
    void longValueAboveMaxShouldClampToMax() {
      assertEquals(
          I31Type.MAX_VALUE,
          I31Type.clampValue(Long.MAX_VALUE),
          "Long above-max should clamp to MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("getMinValue should return MIN_VALUE")
    void getMinValueShouldReturnMinValue() {
      assertEquals(I31Type.MIN_VALUE, I31Type.getMinValue(), "getMinValue should return MIN_VALUE");
    }

    @Test
    @DisplayName("getMaxValue should return MAX_VALUE")
    void getMaxValueShouldReturnMaxValue() {
      assertEquals(I31Type.MAX_VALUE, I31Type.getMaxValue(), "getMaxValue should return MAX_VALUE");
    }

    @Test
    @DisplayName("getBitWidth should return BIT_WIDTH")
    void getBitWidthShouldReturnBitWidth() {
      assertEquals(I31Type.BIT_WIDTH, I31Type.getBitWidth(), "getBitWidth should return BIT_WIDTH");
    }

    @Test
    @DisplayName("getRange should contain MIN and MAX")
    void getRangeShouldContainMinAndMax() {
      final String range = I31Type.getRange();
      assertNotNull(range, "getRange should not return null");
      assertTrue(
          range.contains(String.valueOf(I31Type.MIN_VALUE)), "Range should contain MIN_VALUE");
      assertTrue(
          range.contains(String.valueOf(I31Type.MAX_VALUE)), "Range should contain MAX_VALUE");
    }
  }

  @Nested
  @DisplayName("Equals and Compare Tests")
  class EqualsAndCompareTests {

    @Test
    @DisplayName("equal values should return true from equals")
    void equalValuesShouldReturnTrue() {
      assertTrue(I31Type.equals(42, 42), "Same values should be equal");
    }

    @Test
    @DisplayName("different values should return false from equals")
    void differentValuesShouldReturnFalse() {
      assertFalse(I31Type.equals(42, 43), "Different values should not be equal");
    }

    @Test
    @DisplayName("compare should return negative for smaller first value")
    void compareShouldReturnNegativeForSmaller() {
      assertTrue(I31Type.compare(10, 20) < 0, "compare(10, 20) should be negative");
    }

    @Test
    @DisplayName("compare should return zero for equal values")
    void compareShouldReturnZeroForEqual() {
      assertEquals(0, I31Type.compare(10, 10), "compare(10, 10) should be zero");
    }

    @Test
    @DisplayName("compare should return positive for larger first value")
    void compareShouldReturnPositiveForLarger() {
      assertTrue(I31Type.compare(20, 10) > 0, "compare(20, 10) should be positive");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should format value correctly")
    void toStringShouldFormatCorrectly() {
      final String result = I31Type.toString(42);
      assertNotNull(result, "toString should not return null");
      assertTrue(result.contains("42"), "toString should contain the value");
      assertTrue(result.contains("i31"), "toString should contain 'i31'");
    }
  }

  @Nested
  @DisplayName("ToUnsigned and FromUnsigned Tests")
  class UnsignedConversionTests {

    @Test
    @DisplayName("toUnsigned of zero should be zero")
    void toUnsignedOfZeroShouldBeZero() {
      assertEquals(0, I31Type.toUnsigned(0), "toUnsigned(0) should be 0");
    }

    @Test
    @DisplayName("toUnsigned of positive should preserve value")
    void toUnsignedOfPositiveShouldPreserveValue() {
      assertEquals(42, I31Type.toUnsigned(42), "toUnsigned(42) should be 42");
    }

    @Test
    @DisplayName("fromUnsigned of zero should be zero")
    void fromUnsignedOfZeroShouldBeZero() {
      assertEquals(0, I31Type.fromUnsigned(0), "fromUnsigned(0) should be 0");
    }

    @Test
    @DisplayName("fromUnsigned with negative should throw IllegalArgumentException")
    void fromUnsignedWithNegativeShouldThrowIae() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.fromUnsigned(-1),
          "fromUnsigned(-1) should throw IllegalArgumentException");
    }
  }

  @Nested
  @DisplayName("I31Value Inner Class Tests")
  class I31ValueTests {

    @Test
    @DisplayName("of should create I31Value")
    void ofShouldCreateI31Value() {
      final I31Type.I31Value val = I31Type.I31Value.of(42);
      assertNotNull(val, "I31Value should be created");
      assertEquals(42, val.getValue(), "getValue should return 42");
    }

    @Test
    @DisplayName("of long should create I31Value")
    void ofLongShouldCreateI31Value() {
      final I31Type.I31Value val = I31Type.I31Value.of(100L);
      assertEquals(100, val.getValue(), "getValue should return 100");
    }

    @Test
    @DisplayName("of out of range should throw IllegalArgumentException")
    void ofOutOfRangeShouldThrowIae() {
      assertThrows(
          IllegalArgumentException.class,
          () -> I31Type.I31Value.of(Integer.MAX_VALUE),
          "Out of range value should throw");
    }

    @Test
    @DisplayName("getUnsigned should work for positive value")
    void getUnsignedShouldWorkForPositive() {
      final I31Type.I31Value val = I31Type.I31Value.of(42);
      assertEquals(42, val.getUnsigned(), "getUnsigned of 42 should be 42");
    }

    @Test
    @DisplayName("same value I31Values should be equal")
    void sameValuesShouldBeEqual() {
      final I31Type.I31Value v1 = I31Type.I31Value.of(42);
      final I31Type.I31Value v2 = I31Type.I31Value.of(42);
      assertEquals(v1, v2, "Same value I31Values should be equal");
    }

    @Test
    @DisplayName("different value I31Values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final I31Type.I31Value v1 = I31Type.I31Value.of(42);
      final I31Type.I31Value v2 = I31Type.I31Value.of(43);
      assertNotEquals(v1, v2, "Different value I31Values should not be equal");
    }

    @Test
    @DisplayName("same value I31Values should have same hashCode")
    void sameValuesShouldHaveSameHashCode() {
      final I31Type.I31Value v1 = I31Type.I31Value.of(42);
      final I31Type.I31Value v2 = I31Type.I31Value.of(42);
      assertEquals(v1.hashCode(), v2.hashCode(), "Same value I31Values should have same hashCode");
    }

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final I31Type.I31Value val = I31Type.I31Value.of(42);
      final String str = val.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("42"), "toString should contain the value");
    }
  }
}
