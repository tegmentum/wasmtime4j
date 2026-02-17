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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitU64} class.
 *
 * <p>WitU64 represents a WIT unsigned 64-bit integer value (0 to 2^64-1). Since Java has no
 * unsigned primitives, values are stored as signed longs but interpreted as unsigned. Use
 * toUnsignedBigInteger() for the full range.
 */
@DisplayName("WitU64 Tests")
class WitU64Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitU64 from long value")
    void shouldCreateFromLongValue() {
      final WitU64 value = WitU64.of(10000000000L);
      assertNotNull(value, "Should create WitU64");
      assertEquals(10000000000L, value.getValue(), "Value should be 10000000000");
    }

    @Test
    @DisplayName("should create WitU64 from zero")
    void shouldCreateFromZero() {
      final WitU64 value = WitU64.of(0L);
      assertEquals(0L, value.getValue(), "Value should be 0");
      assertEquals(BigInteger.ZERO, value.toUnsignedBigInteger(), "Unsigned value should be 0");
    }

    @Test
    @DisplayName("should create WitU64 from max signed long")
    void shouldCreateFromMaxSignedLong() {
      final WitU64 value = WitU64.of(Long.MAX_VALUE);
      assertEquals(Long.MAX_VALUE, value.getValue(), "Value should be max long");
      assertEquals(
          BigInteger.valueOf(Long.MAX_VALUE),
          value.toUnsignedBigInteger(),
          "Unsigned value should be max long");
    }

    @Test
    @DisplayName("should create WitU64 from negative long representing unsigned value")
    void shouldCreateFromNegativeLongRepresentingUnsigned() {
      // -1 as signed long represents 2^64-1 as unsigned
      final WitU64 value = WitU64.of(-1L);
      assertEquals(-1L, value.getValue(), "Raw value should be -1");
      assertEquals(
          new BigInteger("18446744073709551615"),
          value.toUnsignedBigInteger(),
          "Unsigned value should be 2^64-1");
    }

    @Test
    @DisplayName("should create WitU64 using ofUnsigned for large value")
    void shouldCreateUsingOfUnsignedForLargeValue() {
      final BigInteger largeValue = new BigInteger("10000000000000000000");
      final WitU64 value = WitU64.ofUnsigned(largeValue);
      assertEquals(largeValue, value.toUnsignedBigInteger(), "Unsigned value should match input");
    }

    @Test
    @DisplayName("should create WitU64 using ofUnsigned for max value")
    void shouldCreateUsingOfUnsignedForMaxValue() {
      final BigInteger maxU64 = new BigInteger("18446744073709551615");
      final WitU64 value = WitU64.ofUnsigned(maxU64);
      assertEquals(maxU64, value.toUnsignedBigInteger(), "Unsigned value should be max u64");
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("ofUnsigned should reject negative value")
    void ofUnsignedShouldRejectNegativeValue() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU64.ofUnsigned(BigInteger.valueOf(-1)),
          "Should reject negative value");
    }

    @Test
    @DisplayName("ofUnsigned should reject value above max")
    void ofUnsignedShouldRejectValueAboveMax() {
      final BigInteger tooLarge = new BigInteger("18446744073709551616");
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU64.ofUnsigned(tooLarge),
          "Should reject value above max");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct signed value")
    void getValueShouldReturnCorrectSignedValue() {
      final WitU64 value = WitU64.ofUnsigned(new BigInteger("10000000000000000000"));
      // This converts to a negative signed long
      assertTrue(value.getValue() < 0, "Should return negative signed value");
    }

    @Test
    @DisplayName("toUnsignedBigInteger should return correct unsigned value")
    void toUnsignedBigIntegerShouldReturnCorrectUnsignedValue() {
      final WitU64 value = WitU64.of(-1L);
      assertEquals(
          new BigInteger("18446744073709551615"),
          value.toUnsignedBigInteger(),
          "Should return correct unsigned value");
    }

    @Test
    @DisplayName("toJava should return Long")
    void toJavaShouldReturnLong() {
      final WitU64 value = WitU64.of(10000000000L);
      assertEquals(Long.valueOf(10000000000L), value.toJava(), "toJava should return Long");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitU64 value1 = WitU64.of(10000000000L);
      final WitU64 value2 = WitU64.of(10000000000L);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitU64 value1 = WitU64.of(10000000000L);
      final WitU64 value2 = WitU64.of(10000000001L);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitU64 value1 = WitU64.of(10000000000L);
      final WitU64 value2 = WitU64.of(10000000000L);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitU64 value = WitU64.of(10000000000L);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("10000000000"), "toString should contain value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitU64 value = WitU64.of(42L);
      assertNotNull(value.getType(), "Should have WitType");
    }

    @Test
    @DisplayName("should report as unsigned")
    void shouldReportAsUnsigned() {
      final WitU64 value = WitU64.of(42L);
      assertTrue(value.isUnsigned(), "Should report as unsigned");
    }
  }
}
