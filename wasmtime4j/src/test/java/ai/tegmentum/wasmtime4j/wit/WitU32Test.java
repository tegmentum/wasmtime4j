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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitU32} class.
 *
 * <p>WitU32 represents a WIT unsigned 32-bit integer value (0 to 4,294,967,295). Since Java has no
 * unsigned primitives, values are stored as signed ints but interpreted as unsigned.
 */
@DisplayName("WitU32 Tests")
class WitU32Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitU32 from int value")
    void shouldCreateFromIntValue() {
      final WitU32 value = WitU32.of(100000);
      assertNotNull(value, "Should create WitU32");
      assertEquals(100000, value.getValue(), "Value should be 100000");
    }

    @Test
    @DisplayName("should create WitU32 from zero")
    void shouldCreateFromZero() {
      final WitU32 value = WitU32.of(0);
      assertEquals(0, value.getValue(), "Value should be 0");
      assertEquals(0L, value.toUnsignedLong(), "Unsigned value should be 0");
    }

    @Test
    @DisplayName("should create WitU32 from max signed int")
    void shouldCreateFromMaxSignedInt() {
      final WitU32 value = WitU32.of(Integer.MAX_VALUE);
      assertEquals(Integer.MAX_VALUE, value.getValue(), "Value should be max int");
      assertEquals(2147483647L, value.toUnsignedLong(), "Unsigned value should be 2147483647");
    }

    @Test
    @DisplayName("should create WitU32 from negative int representing unsigned value")
    void shouldCreateFromNegativeIntRepresentingUnsigned() {
      // -1 as signed int represents 4294967295 as unsigned
      final WitU32 value = WitU32.of(-1);
      assertEquals(-1, value.getValue(), "Raw value should be -1");
      assertEquals(4294967295L, value.toUnsignedLong(), "Unsigned value should be 4294967295");
    }

    @Test
    @DisplayName("should create WitU32 using ofUnsigned for value 3000000000")
    void shouldCreateUsingOfUnsignedFor3Billion() {
      final WitU32 value = WitU32.ofUnsigned(3000000000L);
      assertEquals(3000000000L, value.toUnsignedLong(), "Unsigned value should be 3000000000");
    }

    @Test
    @DisplayName("should create WitU32 using ofUnsigned for max value")
    void shouldCreateUsingOfUnsignedForMaxValue() {
      final WitU32 value = WitU32.ofUnsigned(4294967295L);
      assertEquals(4294967295L, value.toUnsignedLong(), "Unsigned value should be 4294967295");
    }

    @Test
    @DisplayName("ofUnsigned at zero boundary should return correct value")
    void ofUnsignedAtZeroBoundaryShouldReturnCorrectValue() {
      final WitU32 value = WitU32.ofUnsigned(0L);
      assertEquals(0L, value.toUnsignedLong(), "Unsigned value at zero boundary should be 0");
    }

    @Test
    @DisplayName("ofUnsigned at exact max should equal getValue cast to unsigned")
    void ofUnsignedAtExactMaxShouldEqualGetValueCast() {
      final WitU32 value = WitU32.ofUnsigned(4294967295L);
      assertEquals(-1, value.getValue(), "Raw int for max u32 should be -1");
      assertEquals(
          4294967295L,
          Integer.toUnsignedLong(value.getValue()),
          "Integer.toUnsignedLong of raw value should be max u32");
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
          () -> WitU32.ofUnsigned(-1L),
          "Should reject negative value");
    }

    @Test
    @DisplayName("ofUnsigned should reject value above max")
    void ofUnsignedShouldRejectValueAboveMax() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU32.ofUnsigned(4294967296L),
          "Should reject value above max");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct signed value")
    void getValueShouldReturnCorrectSignedValue() {
      final WitU32 value = WitU32.ofUnsigned(3000000000L);
      // 3000000000 unsigned = -1294967296 as signed int
      assertEquals(-1294967296, value.getValue(), "Should return signed value");
    }

    @Test
    @DisplayName("toUnsignedLong should return correct unsigned value")
    void toUnsignedLongShouldReturnCorrectUnsignedValue() {
      final WitU32 value = WitU32.of(-1294967296);
      assertEquals(3000000000L, value.toUnsignedLong(), "Should return unsigned value");
    }

    @Test
    @DisplayName("toJava should return Integer")
    void toJavaShouldReturnInteger() {
      final WitU32 value = WitU32.of(100000);
      assertEquals(Integer.valueOf(100000), value.toJava(), "toJava should return Integer");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitU32 value1 = WitU32.of(100000);
      final WitU32 value2 = WitU32.of(100000);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitU32 value1 = WitU32.of(100000);
      final WitU32 value2 = WitU32.of(100001);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitU32 value1 = WitU32.of(100000);
      final WitU32 value2 = WitU32.of(100000);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain unsigned value")
    void toStringShouldContainUnsignedValue() {
      final WitU32 value = WitU32.ofUnsigned(3000000000L);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("3000000000"), "toString should contain unsigned value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitU32 value = WitU32.of(100000);
      assertNotNull(value.getType(), "Should have WitType");
    }

    @Test
    @DisplayName("should report as unsigned")
    void shouldReportAsUnsigned() {
      final WitU32 value = WitU32.of(100000);
      assertTrue(value.isUnsigned(), "Should report as unsigned");
    }
  }
}
