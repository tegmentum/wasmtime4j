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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the LinkingValidationResult class.
 *
 * <p>LinkingValidationResult represents the result of component linking validation.
 */
@DisplayName("LinkingValidationResult Class Tests")
class LinkingValidationResultTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(LinkingValidationResult.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(LinkingValidationResult.class.getModifiers()),
          "LinkingValidationResult should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(LinkingValidationResult.class.getModifiers()),
          "LinkingValidationResult should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 2 parameters")
    void shouldHavePublicConstructorWith2Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          LinkingValidationResult.class.getConstructor(boolean.class, String.class);
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
      assertEquals(2, constructor.getParameterCount(), "Should have 2 parameters");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          LinkingValidationResult.class.getConstructor(boolean.class, String.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();

      assertEquals(boolean.class, paramTypes[0], "First param should be boolean (valid)");
      assertEquals(String.class, paramTypes[1], "Second param should be String (details)");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have 2 private fields")
    void shouldHave2PrivateFields() {
      long privateFields =
          Arrays.stream(LinkingValidationResult.class.getDeclaredFields())
              .filter(f -> Modifier.isPrivate(f.getModifiers()))
              .filter(f -> !f.isSynthetic())
              .count();
      assertEquals(2, privateFields, "Should have 2 private fields");
    }

    @Test
    @DisplayName("should have valid field")
    void shouldHaveValidField() throws NoSuchFieldException {
      Field field = LinkingValidationResult.class.getDeclaredField("valid");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "valid should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "valid should be final");
      assertEquals(boolean.class, field.getType(), "valid should be boolean");
    }

    @Test
    @DisplayName("should have details field")
    void shouldHaveDetailsField() throws NoSuchFieldException {
      Field field = LinkingValidationResult.class.getDeclaredField("details");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "details should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "details should be final");
      assertEquals(String.class, field.getType(), "details should be String");
    }
  }

  // ========================================================================
  // Static Factory Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have valid factory method")
    void shouldHaveValidFactoryMethod() throws NoSuchMethodException {
      Method method = LinkingValidationResult.class.getMethod("valid", String.class);
      assertNotNull(method, "valid factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "valid should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "valid should be public");
      assertEquals(
          LinkingValidationResult.class,
          method.getReturnType(),
          "Should return LinkingValidationResult");
    }

    @Test
    @DisplayName("should have invalid factory method")
    void shouldHaveInvalidFactoryMethod() throws NoSuchMethodException {
      Method method = LinkingValidationResult.class.getMethod("invalid", String.class);
      assertNotNull(method, "invalid factory method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "invalid should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "invalid should be public");
      assertEquals(
          LinkingValidationResult.class,
          method.getReturnType(),
          "Should return LinkingValidationResult");
    }

    @Test
    @DisplayName("factory methods should have 1 String parameter")
    void factoryMethodsShouldHave1StringParameter() throws NoSuchMethodException {
      Method validMethod = LinkingValidationResult.class.getMethod("valid", String.class);
      Method invalidMethod = LinkingValidationResult.class.getMethod("invalid", String.class);

      assertEquals(1, validMethod.getParameterCount(), "valid should have 1 parameter");
      assertEquals(
          String.class, validMethod.getParameterTypes()[0], "valid param should be String");

      assertEquals(1, invalidMethod.getParameterCount(), "invalid should have 1 parameter");
      assertEquals(
          String.class, invalidMethod.getParameterTypes()[0], "invalid param should be String");
    }
  }

  // ========================================================================
  // Getter Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = LinkingValidationResult.class.getMethod("isValid");
      assertNotNull(method, "isValid should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
      assertFalse(Modifier.isStatic(method.getModifiers()), "isValid should not be static");
    }

    @Test
    @DisplayName("should have getDetails method")
    void shouldHaveGetDetailsMethod() throws NoSuchMethodException {
      Method method = LinkingValidationResult.class.getMethod("getDetails");
      assertNotNull(method, "getDetails should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
      assertFalse(Modifier.isStatic(method.getModifiers()), "getDetails should not be static");
    }
  }

  // ========================================================================
  // Object Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Method Tests")
  class ObjectMethodTests {

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      Method method = LinkingValidationResult.class.getMethod("toString");
      assertEquals(
          LinkingValidationResult.class, method.getDeclaringClass(), "Should override toString");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected public methods")
    void shouldHaveAllExpectedPublicMethods() {
      Set<String> expectedMethods = Set.of("valid", "invalid", "isValid", "getDetails", "toString");

      Set<String> actualMethods =
          Arrays.stream(LinkingValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 5 declared public methods")
    void shouldHaveExactly5DeclaredPublicMethods() {
      long methodCount =
          Arrays.stream(LinkingValidationResult.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(5, methodCount, "Should have exactly 5 declared public methods");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object")
    void shouldExtendObject() {
      assertEquals(
          Object.class,
          LinkingValidationResult.class.getSuperclass(),
          "Should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          LinkingValidationResult.class.getInterfaces().length,
          "Should not implement any interfaces");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          LinkingValidationResult.class.getDeclaredClasses().length,
          "LinkingValidationResult should have no nested classes");
    }
  }
}
