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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the FuncType interface.
 *
 * <p>FuncType represents the type signature of a WebAssembly function, providing access to
 * parameter types and return types. This test verifies the interface structure and method
 * signatures.
 */
@DisplayName("FuncType Interface Tests")
class FuncTypeTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(FuncType.class.isInterface(), "FuncType should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(FuncType.class.getModifiers()), "FuncType should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(FuncType.class.getModifiers()),
          "FuncType should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(FuncType.class), "FuncType should extend WasmType");
    }

    @Test
    @DisplayName("should have exactly 1 parent interface")
    void shouldHaveExactly1ParentInterface() {
      assertEquals(
          1, FuncType.class.getInterfaces().length, "FuncType should extend exactly 1 interface");
    }

    @Test
    @DisplayName("parent interface should be WasmType")
    void parentInterfaceShouldBeWasmType() {
      assertEquals(
          WasmType.class,
          FuncType.class.getInterfaces()[0],
          "FuncType's parent interface should be WasmType");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getParams abstract method")
    void shouldHaveGetParamsMethod() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getParams");
      assertNotNull(method, "getParams method should exist");
      assertEquals(List.class, method.getReturnType(), "getParams should return List");
      assertFalse(method.isDefault(), "getParams should be abstract (not default)");
    }

    @Test
    @DisplayName("getParams should return List<WasmValueType>")
    void getParamsShouldReturnListOfWasmValueType() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getParams");
      final Type returnType = method.getGenericReturnType();
      assertTrue(
          returnType instanceof ParameterizedType, "getParams return type should be parameterized");
      final ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      assertEquals(
          WasmValueType.class,
          paramType.getActualTypeArguments()[0],
          "Type argument should be WasmValueType");
    }

    @Test
    @DisplayName("should have getResults abstract method")
    void shouldHaveGetResultsMethod() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getResults");
      assertNotNull(method, "getResults method should exist");
      assertEquals(List.class, method.getReturnType(), "getResults should return List");
      assertFalse(method.isDefault(), "getResults should be abstract (not default)");
    }

    @Test
    @DisplayName("getResults should return List<WasmValueType>")
    void getResultsShouldReturnListOfWasmValueType() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getResults");
      final Type returnType = method.getGenericReturnType();
      assertTrue(
          returnType instanceof ParameterizedType,
          "getResults return type should be parameterized");
      final ParameterizedType paramType = (ParameterizedType) returnType;
      assertEquals(List.class, paramType.getRawType(), "Raw type should be List");
      assertEquals(
          WasmValueType.class,
          paramType.getActualTypeArguments()[0],
          "Type argument should be WasmValueType");
    }

    @Test
    @DisplayName("should have exactly 2 abstract methods")
    void shouldHaveExactly2AbstractMethods() {
      long abstractMethods =
          Arrays.stream(FuncType.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(2, abstractMethods, "FuncType should have exactly 2 abstract methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have getParamCount default method")
    void shouldHaveGetParamCountMethod() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getParamCount");
      assertNotNull(method, "getParamCount method should exist");
      assertTrue(method.isDefault(), "getParamCount should be a default method");
      assertEquals(int.class, method.getReturnType(), "getParamCount should return int");
    }

    @Test
    @DisplayName("should have getResultCount default method")
    void shouldHaveGetResultCountMethod() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getResultCount");
      assertNotNull(method, "getResultCount method should exist");
      assertTrue(method.isDefault(), "getResultCount should be a default method");
      assertEquals(int.class, method.getReturnType(), "getResultCount should return int");
    }

    @Test
    @DisplayName("should have getKind default method")
    void shouldHaveGetKindMethod() throws NoSuchMethodException {
      final Method method = FuncType.class.getMethod("getKind");
      assertNotNull(method, "getKind method should exist");
      assertTrue(method.isDefault(), "getKind should be a default method");
      assertEquals(
          WasmTypeKind.class, method.getReturnType(), "getKind should return WasmTypeKind");
    }

    @Test
    @DisplayName("should have exactly 3 default methods")
    void shouldHaveExactly3DefaultMethods() {
      long defaultMethods =
          Arrays.stream(FuncType.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(3, defaultMethods, "FuncType should have exactly 3 default methods");
    }

    @Test
    @DisplayName("all default methods should have no parameters")
    void allDefaultMethodsShouldHaveNoParameters() throws NoSuchMethodException {
      String[] methodNames = {"getParamCount", "getResultCount", "getKind"};
      for (String name : methodNames) {
        Method method = FuncType.class.getMethod(name);
        assertEquals(0, method.getParameterCount(), name + " should have no parameters");
      }
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of("getParams", "getResults", "getParamCount", "getResultCount", "getKind");

      Set<String> actualMethods =
          Arrays.stream(FuncType.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "FuncType should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 5 declared methods")
    void shouldHaveExactly5DeclaredMethods() {
      long declaredMethods =
          Arrays.stream(FuncType.class.getDeclaredMethods()).filter(m -> !m.isSynthetic()).count();
      assertEquals(5, declaredMethods, "FuncType should have exactly 5 declared methods");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("getParamCount should return primitive int")
    void getParamCountShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getParamCount");
      assertEquals(int.class, method.getReturnType(), "getParamCount should return primitive int");
      assertFalse(
          method.getReturnType().equals(Integer.class),
          "getParamCount should not return Integer wrapper");
    }

    @Test
    @DisplayName("getResultCount should return primitive int")
    void getResultCountShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getResultCount");
      assertEquals(int.class, method.getReturnType(), "getResultCount should return primitive int");
      assertFalse(
          method.getReturnType().equals(Integer.class),
          "getResultCount should not return Integer wrapper");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(FuncType.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "FuncType should have no static methods");
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
          0, FuncType.class.getDeclaredClasses().length, "FuncType should have no nested classes");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0, FuncType.class.getDeclaredFields().length, "FuncType should have no declared fields");
    }
  }

  // ========================================================================
  // Method Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Semantic Tests")
  class MethodSemanticTests {

    @Test
    @DisplayName("getParams should be public")
    void getParamsShouldBePublic() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getParams");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "getParams should be public (inherited from interface)");
    }

    @Test
    @DisplayName("getResults should be public")
    void getResultsShouldBePublic() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getResults");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "getResults should be public (inherited from interface)");
    }

    @Test
    @DisplayName("getKind should be public")
    void getKindShouldBePublic() throws NoSuchMethodException {
      Method method = FuncType.class.getMethod("getKind");
      assertTrue(
          Modifier.isPublic(method.getModifiers()),
          "getKind should be public (inherited from interface)");
    }
  }

  // ========================================================================
  // WasmType Override Tests
  // ========================================================================

  @Nested
  @DisplayName("WasmType Override Tests")
  class WasmTypeOverrideTests {

    @Test
    @DisplayName("getKind should override WasmType.getKind")
    void getKindShouldOverrideWasmTypeGetKind() throws NoSuchMethodException {
      Method funcTypeMethod = FuncType.class.getMethod("getKind");
      Method wasmTypeMethod = WasmType.class.getMethod("getKind");

      assertEquals(
          funcTypeMethod.getReturnType(),
          wasmTypeMethod.getReturnType(),
          "getKind return types should match");
    }

    @Test
    @DisplayName("getKind default implementation should be declared in FuncType")
    void getKindDefaultShouldBeDeclaredInFuncType() throws NoSuchMethodException {
      Method method = FuncType.class.getDeclaredMethod("getKind");
      assertTrue(method.isDefault(), "getKind should be a default method in FuncType");
    }
  }
}
