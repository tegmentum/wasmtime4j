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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FunctionTypeValidator utility class.
 *
 * <p>FunctionTypeValidator provides function type validation and subtyping rules for WebAssembly
 * typed function references.
 */
@DisplayName("FunctionTypeValidator Class Tests")
class FunctionTypeValidatorTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(FunctionTypeValidator.class.isInterface(), "Should be a class, not interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FunctionTypeValidator.class.getModifiers()),
          "FunctionTypeValidator should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(FunctionTypeValidator.class.getModifiers()),
          "FunctionTypeValidator should be final (utility class)");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = FunctionTypeValidator.class.getDeclaredConstructor();
      assertTrue(
          Modifier.isPrivate(constructor.getModifiers()),
          "Constructor should be private (utility class)");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have isSubtype method")
    void shouldHaveIsSubtypeMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "isSubtype", FunctionType.class, FunctionType.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "isSubtype should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isSubtype should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isCompatible method")
    void shouldHaveIsCompatibleMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "isCompatible", FunctionType.class, FunctionType.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "isCompatible should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isCompatible should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSubtypeOf method")
    void shouldHaveIsSubtypeOfMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "isSubtypeOf", WasmValueType.class, WasmValueType.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "isSubtypeOf should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isSubtypeOf should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have validateCall method")
    void shouldHaveValidateCallMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "validateCall", FunctionType.class, FunctionType.class, WasmValue[].class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "validateCall should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "validateCall should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have validateAssignment method")
    void shouldHaveValidateAssignmentMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "validateAssignment", FunctionType.class, FunctionType.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "validateAssignment should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "validateAssignment should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getCommonSupertype method")
    void shouldHaveGetCommonSupertypeMethod() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "getCommonSupertype", FunctionType.class, FunctionType.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "getCommonSupertype should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getCommonSupertype should be public");
      assertEquals(FunctionType.class, method.getReturnType(), "Should return FunctionType");
    }

    @Test
    @DisplayName("should have exactly 6 public static methods")
    void shouldHaveExactly6PublicStaticMethods() {
      long publicStaticMethods =
          Arrays.stream(FunctionTypeValidator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(6, publicStaticMethods, "Should have exactly 6 public static methods");
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
      Set<String> expectedMethods =
          Set.of(
              "isSubtype",
              "isCompatible",
              "isSubtypeOf",
              "validateCall",
              "validateAssignment",
              "getCommonSupertype");

      Set<String> actualMethods =
          Arrays.stream(FunctionTypeValidator.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Exception Declaration Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("validateCall should declare ValidationException")
    void validateCallShouldDeclareValidationException() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "validateCall", FunctionType.class, FunctionType.class, WasmValue[].class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "Should declare 1 exception");
      assertEquals(
          ai.tegmentum.wasmtime4j.exception.ValidationException.class,
          exceptions[0],
          "Should declare ValidationException");
    }

    @Test
    @DisplayName("validateAssignment should declare ValidationException")
    void validateAssignmentShouldDeclareValidationException() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "validateAssignment", FunctionType.class, FunctionType.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "Should declare 1 exception");
      assertEquals(
          ai.tegmentum.wasmtime4j.exception.ValidationException.class,
          exceptions[0],
          "Should declare ValidationException");
    }

    @Test
    @DisplayName("isSubtype should not declare exceptions")
    void isSubtypeShouldNotDeclareExceptions() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "isSubtype", FunctionType.class, FunctionType.class);
      assertEquals(0, method.getExceptionTypes().length, "isSubtype should not declare exceptions");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("isSubtype should have 2 FunctionType parameters")
    void isSubtypeShouldHave2FunctionTypeParameters() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "isSubtype", FunctionType.class, FunctionType.class);
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
      assertEquals(
          FunctionType.class, method.getParameterTypes()[0], "First param is FunctionType");
      assertEquals(
          FunctionType.class, method.getParameterTypes()[1], "Second param is FunctionType");
    }

    @Test
    @DisplayName("isSubtypeOf should have 2 WasmValueType parameters")
    void isSubtypeOfShouldHave2WasmValueTypeParameters() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "isSubtypeOf", WasmValueType.class, WasmValueType.class);
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
      assertEquals(
          WasmValueType.class, method.getParameterTypes()[0], "First param is WasmValueType");
      assertEquals(
          WasmValueType.class, method.getParameterTypes()[1], "Second param is WasmValueType");
    }

    @Test
    @DisplayName("validateCall should have 3 parameters")
    void validateCallShouldHave3Parameters() throws NoSuchMethodException {
      Method method =
          FunctionTypeValidator.class.getMethod(
              "validateCall", FunctionType.class, FunctionType.class, WasmValue[].class);
      assertEquals(3, method.getParameterCount(), "Should have 3 parameters");
      assertEquals(
          FunctionType.class, method.getParameterTypes()[0], "First param is FunctionType");
      assertEquals(
          FunctionType.class, method.getParameterTypes()[1], "Second param is FunctionType");
      assertEquals(WasmValue[].class, method.getParameterTypes()[2], "Third param is WasmValue[]");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no public fields")
    void shouldHaveNoPublicFields() {
      long publicFields =
          Arrays.stream(FunctionTypeValidator.class.getDeclaredFields())
              .filter(f -> Modifier.isPublic(f.getModifiers()))
              .count();
      assertEquals(0, publicFields, "Utility class should have no public fields");
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
          FunctionTypeValidator.class.getDeclaredClasses().length,
          "FunctionTypeValidator should have no nested classes");
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
          FunctionTypeValidator.class.getSuperclass(),
          "Should extend Object directly");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          FunctionTypeValidator.class.getInterfaces().length,
          "Utility class should not implement any interfaces");
    }
  }
}
