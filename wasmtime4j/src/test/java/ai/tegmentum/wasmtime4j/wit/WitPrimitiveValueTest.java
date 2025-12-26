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
 * <p>WitPrimitiveValue is the base class for all WIT primitive type values (bool, integers, floats,
 * char, string).
 */
@DisplayName("WitPrimitiveValue Tests")
class WitPrimitiveValueTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be an abstract class")
    void shouldBeAbstractClass() {
      assertTrue(
          Modifier.isAbstract(WitPrimitiveValue.class.getModifiers()),
          "WitPrimitiveValue should be abstract");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WitPrimitiveValue.class.getModifiers()),
          "WitPrimitiveValue should be public");
    }

    @Test
    @DisplayName("should extend WitValue")
    void shouldExtendWitValue() {
      assertTrue(
          WitValue.class.isAssignableFrom(WitPrimitiveValue.class),
          "WitPrimitiveValue should extend WitValue");
    }
  }

  @Nested
  @DisplayName("Type Check Method Tests")
  class TypeCheckMethodTests {

    @Test
    @DisplayName("should have isNumeric method")
    void shouldHaveIsNumericMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isNumeric");
      assertNotNull(method, "isNumeric method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isNumeric should return boolean");
    }

    @Test
    @DisplayName("should have isInteger method")
    void shouldHaveIsIntegerMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isInteger");
      assertNotNull(method, "isInteger method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isInteger should return boolean");
    }

    @Test
    @DisplayName("should have isFloatingPoint method")
    void shouldHaveIsFloatingPointMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isFloatingPoint");
      assertNotNull(method, "isFloatingPoint method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isFloatingPoint should return boolean");
    }

    @Test
    @DisplayName("should have isUnsigned method")
    void shouldHaveIsUnsignedMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isUnsigned");
      assertNotNull(method, "isUnsigned method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isUnsigned should return boolean");
    }

    @Test
    @DisplayName("should have isSigned method")
    void shouldHaveIsSignedMethod() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isSigned");
      assertNotNull(method, "isSigned method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isSigned should return boolean");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("isNumeric should take no parameters")
    void isNumericShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isNumeric");
      assertEquals(0, method.getParameterCount(), "isNumeric should take no parameters");
    }

    @Test
    @DisplayName("isInteger should take no parameters")
    void isIntegerShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isInteger");
      assertEquals(0, method.getParameterCount(), "isInteger should take no parameters");
    }

    @Test
    @DisplayName("isFloatingPoint should take no parameters")
    void isFloatingPointShouldTakeNoParameters() throws NoSuchMethodException {
      final Method method = WitPrimitiveValue.class.getMethod("isFloatingPoint");
      assertEquals(0, method.getParameterCount(), "isFloatingPoint should take no parameters");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have protected constructor")
    void shouldHaveProtectedConstructor() throws NoSuchMethodException {
      final var constructor =
          WitPrimitiveValue.class.getDeclaredConstructor(ai.tegmentum.wasmtime4j.WitType.class);
      assertNotNull(constructor, "WitType constructor should exist");
      assertTrue(
          Modifier.isProtected(constructor.getModifiers()), "Constructor should be protected");
    }
  }
}
