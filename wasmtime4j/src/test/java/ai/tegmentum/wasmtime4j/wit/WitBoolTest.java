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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitBool} class.
 *
 * <p>WitBool represents a WIT boolean value and provides type-safe conversion to/from the WIT bool
 * type. Values are immutable and thread-safe.
 */
@DisplayName("WitBool Tests")
class WitBoolTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(Modifier.isFinal(WitBool.class.getModifiers()), "WitBool should be final");
    }

    @Test
    @DisplayName("should extend WitPrimitiveValue")
    void shouldExtendWitPrimitiveValue() {
      assertTrue(
          WitPrimitiveValue.class.isAssignableFrom(WitBool.class),
          "WitBool should extend WitPrimitiveValue");
    }
  }

  @Nested
  @DisplayName("Static Constants Tests")
  class StaticConstantsTests {

    @Test
    @DisplayName("should have TRUE constant")
    void shouldHaveTrueConstant() {
      assertNotNull(WitBool.TRUE, "TRUE constant should exist");
      assertTrue(WitBool.TRUE.getValue(), "TRUE should have true value");
    }

    @Test
    @DisplayName("should have FALSE constant")
    void shouldHaveFalseConstant() {
      assertNotNull(WitBool.FALSE, "FALSE constant should exist");
      assertFalse(WitBool.FALSE.getValue(), "FALSE should have false value");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("of(true) should return TRUE constant")
    void ofTrueShouldReturnTrueConstant() {
      final WitBool result = WitBool.of(true);
      assertTrue(result == WitBool.TRUE, "of(true) should return cached TRUE");
    }

    @Test
    @DisplayName("of(false) should return FALSE constant")
    void ofFalseShouldReturnFalseConstant() {
      final WitBool result = WitBool.of(false);
      assertTrue(result == WitBool.FALSE, "of(false) should return cached FALSE");
    }
  }

  @Nested
  @DisplayName("Value Access Tests")
  class ValueAccessTests {

    @Test
    @DisplayName("getValue should return true for TRUE")
    void getValueShouldReturnTrueForTrue() {
      assertTrue(WitBool.TRUE.getValue(), "getValue should return true for TRUE");
    }

    @Test
    @DisplayName("getValue should return false for FALSE")
    void getValueShouldReturnFalseForFalse() {
      assertFalse(WitBool.FALSE.getValue(), "getValue should return false for FALSE");
    }

    @Test
    @DisplayName("toJava should return Boolean.TRUE for true value")
    void toJavaShouldReturnBooleanTrueForTrueValue() {
      assertEquals(Boolean.TRUE, WitBool.TRUE.toJava(), "toJava should return Boolean.TRUE");
    }

    @Test
    @DisplayName("toJava should return Boolean.FALSE for false value")
    void toJavaShouldReturnBooleanFalseForFalseValue() {
      assertEquals(Boolean.FALSE, WitBool.FALSE.toJava(), "toJava should return Boolean.FALSE");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("same boolean value should be equal")
    void sameBooleanValueShouldBeEqual() {
      final WitBool bool1 = WitBool.of(true);
      final WitBool bool2 = WitBool.of(true);
      assertEquals(bool1, bool2, "Same boolean values should be equal");
    }

    @Test
    @DisplayName("different boolean values should not be equal")
    void differentBooleanValuesShouldNotBeEqual() {
      assertNotEquals(WitBool.TRUE, WitBool.FALSE, "Different boolean values should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      assertNotEquals(null, WitBool.TRUE, "Should not equal null");
    }

    @Test
    @DisplayName("should not equal object of different type")
    void shouldNotEqualDifferentType() {
      assertNotEquals("true", WitBool.TRUE, "Should not equal String");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("same boolean value should have same hash code")
    void sameBooleanValueShouldHaveSameHashCode() {
      final WitBool bool1 = WitBool.of(true);
      final WitBool bool2 = WitBool.of(true);
      assertEquals(bool1.hashCode(), bool2.hashCode(), "Same values should have same hash code");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain value for true")
    void toStringShouldContainValueForTrue() {
      final String str = WitBool.TRUE.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("true"), "toString should contain 'true'");
    }

    @Test
    @DisplayName("toString should contain value for false")
    void toStringShouldContainValueForFalse() {
      final String str = WitBool.FALSE.toString();
      assertNotNull(str, "toString should not return null");
      assertTrue(str.contains("false"), "toString should contain 'false'");
    }
  }

  @Nested
  @DisplayName("Type Tests")
  class TypeTests {

    @Test
    @DisplayName("should have WitType")
    void shouldHaveWitType() {
      assertNotNull(WitBool.TRUE.getType(), "Should have WitType");
    }
  }
}
