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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
