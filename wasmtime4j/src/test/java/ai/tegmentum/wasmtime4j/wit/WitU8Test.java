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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitU8} class.
 *
 * <p>WitU8 represents a WIT unsigned 8-bit integer value (0 to 255). Since Java has no unsigned
 * primitives, values are stored as signed bytes but interpreted as unsigned.
 */
@DisplayName("WitU8 Tests")
class WitU8Test {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitU8.class.getModifiers()), "WitU8 should be final");
    }

    @Test
    @DisplayName("should extend WitPrimitiveValue")
    void shouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitU8.class),
          "WitU8 should extend WitPrimitiveValue");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("should have of factory method")
    void shouldHaveOfFactoryMethod() throws NoSuchMethodException {
      final Method method = WitU8.class.getMethod("of", byte.class);
      assertNotNull(method, "Should have of(byte) method");
      assertEquals(WitU8.class, method.getReturnType(), "Should return WitU8");
    }

    @Test
    @DisplayName("should have ofUnsigned factory method")
    void shouldHaveOfUnsignedFactoryMethod() throws NoSuchMethodException {
      final Method method = WitU8.class.getMethod("ofUnsigned", int.class);
      assertNotNull(method, "Should have ofUnsigned(int) method");
      assertEquals(WitU8.class, method.getReturnType(), "Should return WitU8");
    }

    @Test
    @DisplayName("should create WitU8 from byte value")
    void shouldCreateFromByteValue() {
      final WitU8 value = WitU8.of((byte) 42);
      assertNotNull(value, "Should create WitU8");
      assertEquals((byte) 42, value.getValue(), "Value should be 42");
    }

    @Test
    @DisplayName("should create WitU8 from zero")
    void shouldCreateFromZero() {
      final WitU8 value = WitU8.of((byte) 0);
      assertEquals((byte) 0, value.getValue(), "Value should be 0");
      assertEquals(0, value.toUnsignedInt(), "Unsigned value should be 0");
    }

    @Test
    @DisplayName("should create WitU8 from max signed byte")
    void shouldCreateFromMaxSignedByte() {
      final WitU8 value = WitU8.of(Byte.MAX_VALUE);
      assertEquals(Byte.MAX_VALUE, value.getValue(), "Value should be max byte");
      assertEquals(127, value.toUnsignedInt(), "Unsigned value should be 127");
    }

    @Test
    @DisplayName("should create WitU8 from negative byte representing unsigned value")
    void shouldCreateFromNegativeByteRepresentingUnsigned() {
      // -1 as signed byte represents 255 as unsigned
      final WitU8 value = WitU8.of((byte) -1);
      assertEquals((byte) -1, value.getValue(), "Raw value should be -1");
      assertEquals(255, value.toUnsignedInt(), "Unsigned value should be 255");
    }

    @Test
    @DisplayName("should create WitU8 using ofUnsigned for value 200")
    void shouldCreateUsingOfUnsignedFor200() {
      final WitU8 value = WitU8.ofUnsigned(200);
      assertEquals(200, value.toUnsignedInt(), "Unsigned value should be 200");
    }

    @Test
    @DisplayName("should create WitU8 using ofUnsigned for max value 255")
    void shouldCreateUsingOfUnsignedForMaxValue() {
      final WitU8 value = WitU8.ofUnsigned(255);
      assertEquals(255, value.toUnsignedInt(), "Unsigned value should be 255");
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
          () -> WitU8.ofUnsigned(-1),
          "Should reject negative value");
    }

    @Test
    @DisplayName("ofUnsigned should reject value above 255")
    void ofUnsignedShouldRejectValueAbove255() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WitU8.ofUnsigned(256),
          "Should reject value above 255");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return correct signed value")
    void getValueShouldReturnCorrectSignedValue() {
      final WitU8 value = WitU8.ofUnsigned(128);
      // 128 unsigned = -128 as signed byte
      assertEquals((byte) -128, value.getValue(), "Should return signed value");
    }

    @Test
    @DisplayName("toUnsignedInt should return correct unsigned value")
    void toUnsignedIntShouldReturnCorrectUnsignedValue() {
      final WitU8 value = WitU8.of((byte) -128);
      assertEquals(128, value.toUnsignedInt(), "Should return unsigned value");
    }

    @Test
    @DisplayName("toJava should return Byte")
    void toJavaShouldReturnByte() {
      final WitU8 value = WitU8.of((byte) 42);
      assertEquals(Byte.valueOf((byte) 42), value.toJava(), "toJava should return Byte");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same value should be equal")
    void sameValueShouldBeEqual() {
      final WitU8 value1 = WitU8.of((byte) 42);
      final WitU8 value2 = WitU8.of((byte) 42);
      assertEquals(value1, value2, "Same values should be equal");
    }

    @Test
    @DisplayName("different values should not be equal")
    void differentValuesShouldNotBeEqual() {
      final WitU8 value1 = WitU8.of((byte) 42);
      final WitU8 value2 = WitU8.of((byte) 43);
      assertNotEquals(value1, value2, "Different values should not be equal");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same value should have same hash code")
    void sameValueShouldHaveSameHashCode() {
      final WitU8 value1 = WitU8.of((byte) 42);
      final WitU8 value2 = WitU8.of((byte) 42);
      assertEquals(value1.hashCode(), value2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value")
    void toStringShouldContainValue() {
      final WitU8 value = WitU8.ofUnsigned(200);
      final String str = value.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("200"), "toString should contain unsigned value");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      final WitU8 value = WitU8.of((byte) 42);
      assertNotNull(value.getType(), "Should have WitType");
    }

    @Test
    @DisplayName("should report as unsigned")
    void shouldReportAsUnsigned() {
      final WitU8 value = WitU8.of((byte) 42);
      assertTrue(value.isUnsigned(), "Should report as unsigned");
    }
  }
}
