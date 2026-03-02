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
 * Tests for {@link WitU16} class.
 *
 * <p>WitU16 represents a WIT unsigned 16-bit integer value (0 to 65,535). Since Java has no
 * unsigned primitives, values are stored as signed shorts but interpreted as unsigned.
 */
@DisplayName("WitU16 Tests")
class WitU16Test {

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should create WitU16 from short value")
    void shouldCreateFromShortValue() {
      final WitU16 value = WitU16.of((short) 1000);
      assertNotNull(value, "Should create WitU16");
      assertEquals((short) 1000, value.getValue(), "Value should be 1000");
    }

    @Test
    @DisplayName("should create WitU16 from zero")
    void shouldCreateFromZero() {
      final WitU16 value = WitU16.of((short) 0);
      assertEquals((short) 0, value.getValue(), "Value should be 0");
      assertEquals(0, value.toUnsignedInt(), "Unsigned value should be 0");
    }

    @Test
    @DisplayName("should create WitU16 from max signed short")
    void shouldCreateFromMaxSignedShort() {
      final WitU16 value = WitU16.of(Short.MAX_VALUE);
      assertEquals(Short.MAX_VALUE, value.getValue(), "Value should be max short");
      assertEquals(32767, value.toUnsignedInt(), "Unsigned value should be 32767");
    }

    @Test
    @DisplayName("should create WitU16 from negative short representing unsigned value")
    void shouldCreateFromNegativeShortRepresentingUnsigned() {
      // -1 as signed short represents 65535 as unsigned
      final WitU16 value = WitU16.of((short) -1);
      assertEquals((short) -1, value.getValue(), "Raw value should be -1");
      assertEquals(65535, value.toUnsignedInt(), "Unsigned value should be 65535");
    }

    @Test
    @DisplayName("should create WitU16 using ofUnsigned for value 50000")
    void shouldCreateUsingOfUnsignedFor50000() {
      final WitU16 value = WitU16.ofUnsigned(50000);
      assertEquals(50000, value.toUnsignedInt(), "Unsigned value should be 50000");
    }

    @Test
    @DisplayName("should create WitU16 using ofUnsigned for max value 65535")
    void shouldCreateUsingOfUnsignedForMaxValue() {
      final WitU16 value = WitU16.ofUnsigned(65535);
      assertEquals(65535, value.toUnsignedInt(), "Unsigned value should be 65535");
    }

    @Test
    @DisplayName("ofUnsigned at zero boundary should return correct value")
    void ofUnsignedAtZeroBoundaryShouldReturnCorrectValue() {
      final WitU16 value = WitU16.ofUnsigned(0);
      assertEquals(0, value.toUnsignedInt(), "Unsigned value at zero boundary should be 0");
    }

    @Test
    @DisplayName("ofUnsigned at exact max should equal getValue cast to unsigned")
    void ofUnsignedAtExactMaxShouldEqualGetValueCast() {
      final WitU16 value = WitU16.ofUnsigned(65535);
      assertEquals((short) -1, value.getValue(), "Raw short for 65535 should be -1");
      assertEquals(
          65535,
          Short.toUnsignedInt(value.getValue()),
          "Short.toUnsignedInt of raw value should be 65535");
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
          () -> WitU16.ofUnsigned(-1),
          "Should reject negative value");
    }

    @Test
    @DisplayName("ofUnsigned should reject value above 65535")
    void ofUnsignedShouldRejectValueAbove65535() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU16.ofUnsigned(65536),
          "Should reject value above 65535");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct signed value")
    void getValueShouldReturnCorrectSignedValue() {
      final WitU16 value = WitU16.ofUnsigned(40000);
      // 40000 unsigned = -25536 as signed short
      assertEquals((short) -25536, value.getValue(), "Should return signed value");
    }

    @Test
    @DisplayName("toUnsignedInt should return correct unsigned value")
    void toUnsignedIntShouldReturnCorrectUnsignedValue() {
      final WitU16 value = WitU16.of((short) -25536);
      assertEquals(40000, value.toUnsignedInt(), "Should return unsigned value");
    }

    @Test
    @DisplayName("toJava should return Short")
    void toJavaShouldReturnShort() {
      final WitU16 value = WitU16.of((short) 1000);
      assertEquals(Short.valueOf((short) 1000), value.toJava(), "toJava should return Short");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitU16 value1 = WitU16.of((short) 1000);
      final WitU16 value2 = WitU16.of((short) 1000);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitU16 value1 = WitU16.of((short) 1000);
      final WitU16 value2 = WitU16.of((short) 1001);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitU16 value1 = WitU16.of((short) 1000);
      final WitU16 value2 = WitU16.of((short) 1000);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitU16 value = WitU16.ofUnsigned(50000);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("50000"), "toString should contain unsigned value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitU16 value = WitU16.of((short) 1000);
      assertNotNull(value.getType(), "Should have WitType");
    }

    @Test
    @DisplayName("should report as unsigned")
    void shouldReportAsUnsigned() {
      final WitU16 value = WitU16.of((short) 1000);
      assertTrue(value.isUnsigned(), "Should report as unsigned");
    }
  }
}
