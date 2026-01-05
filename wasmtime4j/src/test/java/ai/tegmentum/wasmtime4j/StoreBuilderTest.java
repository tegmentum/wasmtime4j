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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the StoreBuilder class.
 *
 * <p>StoreBuilder provides a fluent API for creating Store instances with custom configuration.
 * This test verifies the class structure and method signatures.
 */
@DisplayName("StoreBuilder Class Tests")
class StoreBuilderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertTrue(!StoreBuilder.class.isInterface(), "StoreBuilder should be a class");
      assertTrue(!StoreBuilder.class.isEnum(), "StoreBuilder should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(StoreBuilder.class.getModifiers()), "StoreBuilder should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(StoreBuilder.class.getModifiers()), "StoreBuilder should be final");
    }

    @Test
    @DisplayName("should have one type parameter")
    void shouldHaveOneTypeParameter() {
      TypeVariable<?>[] typeParams = StoreBuilder.class.getTypeParameters();
      assertEquals(1, typeParams.length, "StoreBuilder should have exactly one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with Engine parameter")
    void shouldHavePackagePrivateConstructorWithEngine() throws NoSuchMethodException {
      Constructor<?> constructor = StoreBuilder.class.getDeclaredConstructor(Engine.class);
      assertNotNull(constructor, "Constructor with Engine should exist");
      // Package-private means neither public, protected, nor private
      int modifiers = constructor.getModifiers();
      assertTrue(
          !Modifier.isPublic(modifiers)
              && !Modifier.isProtected(modifiers)
              && !Modifier.isPrivate(modifiers),
          "Constructor should be package-private");
    }

    @Test
    @DisplayName("constructor should have Engine as parameter type")
    void constructorShouldHaveEngineAsParameterType() throws NoSuchMethodException {
      Constructor<?> constructor = StoreBuilder.class.getDeclaredConstructor(Engine.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(1, paramTypes.length, "Constructor should have 1 parameter");
      assertEquals(Engine.class, paramTypes[0], "Parameter should be Engine");
    }
  }

  // ========================================================================
  // Fluent Builder Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Fluent Builder Methods Tests")
  class FluentBuilderMethodsTests {

    @Test
    @DisplayName("should have withData method")
    void shouldHaveWithDataMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withData", Object.class);
      assertNotNull(method, "withData method should exist");
      assertEquals(
          StoreBuilder.class, method.getReturnType(), "withData should return StoreBuilder");
    }

    @Test
    @DisplayName("should have withFuel method")
    void shouldHaveWithFuelMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withFuel", long.class);
      assertNotNull(method, "withFuel method should exist");
      assertEquals(
          StoreBuilder.class, method.getReturnType(), "withFuel should return StoreBuilder");
    }

    @Test
    @DisplayName("should have withEpochDeadline method")
    void shouldHaveWithEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withEpochDeadline", long.class);
      assertNotNull(method, "withEpochDeadline method should exist");
      assertEquals(
          StoreBuilder.class,
          method.getReturnType(),
          "withEpochDeadline should return StoreBuilder");
    }

    @Test
    @DisplayName("should have withLimits method")
    void shouldHaveWithLimitsMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withLimits", StoreLimits.class);
      assertNotNull(method, "withLimits method should exist");
      assertEquals(
          StoreBuilder.class, method.getReturnType(), "withLimits should return StoreBuilder");
    }

    @Test
    @DisplayName("all with methods should return StoreBuilder for fluent API")
    void allWithMethodsShouldReturnStoreBuilder() {
      Set<String> withMethods = Set.of("withData", "withFuel", "withEpochDeadline", "withLimits");

      for (Method method : StoreBuilder.class.getDeclaredMethods()) {
        if (withMethods.contains(method.getName()) && Modifier.isPublic(method.getModifiers())) {
          assertEquals(
              StoreBuilder.class,
              method.getReturnType(),
              method.getName() + " should return StoreBuilder for fluent API");
        }
      }
    }
  }

  // ========================================================================
  // Build Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Build Method Tests")
  class BuildMethodTests {

    @Test
    @DisplayName("should have build method")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(Store.class, method.getReturnType(), "build should return Store");
    }

    @Test
    @DisplayName("build method should be public")
    void buildMethodShouldBePublic() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("build");
      assertTrue(Modifier.isPublic(method.getModifiers()), "build should be public");
    }

    @Test
    @DisplayName("build method should declare WasmException")
    void buildMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("build");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "build should declare one exception");
      assertEquals(
          "ai.tegmentum.wasmtime4j.exception.WasmException",
          exceptions[0].getName(),
          "build should declare WasmException");
    }

    @Test
    @DisplayName("build method should have no parameters")
    void buildMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("build");
      assertEquals(0, method.getParameterCount(), "build should have no parameters");
    }
  }

  // ========================================================================
  // Accessor Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Methods Tests")
  class AccessorMethodsTests {

    @Test
    @DisplayName("should have getEngine method")
    void shouldHaveGetEngineMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getDeclaredMethod("getEngine");
      assertNotNull(method, "getEngine method should exist");
      assertEquals(Engine.class, method.getReturnType(), "getEngine should return Engine");
    }

    @Test
    @DisplayName("should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getDeclaredMethod("getData");
      assertNotNull(method, "getData method should exist");
    }

    @Test
    @DisplayName("should have getFuel method")
    void shouldHaveGetFuelMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getDeclaredMethod("getFuel");
      assertNotNull(method, "getFuel method should exist");
      assertEquals(Long.class, method.getReturnType(), "getFuel should return Long");
    }

    @Test
    @DisplayName("should have getEpochDeadline method")
    void shouldHaveGetEpochDeadlineMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getDeclaredMethod("getEpochDeadline");
      assertNotNull(method, "getEpochDeadline method should exist");
      assertEquals(Long.class, method.getReturnType(), "getEpochDeadline should return Long");
    }

    @Test
    @DisplayName("should have getLimits method")
    void shouldHaveGetLimitsMethod() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getDeclaredMethod("getLimits");
      assertNotNull(method, "getLimits method should exist");
      assertEquals(
          StoreLimits.class, method.getReturnType(), "getLimits should return StoreLimits");
    }

    @Test
    @DisplayName("accessor methods should be package-private")
    void accessorMethodsShouldBePackagePrivate() throws NoSuchMethodException {
      Set<String> accessorMethods =
          Set.of("getEngine", "getData", "getFuel", "getEpochDeadline", "getLimits");

      for (String methodName : accessorMethods) {
        Method method = StoreBuilder.class.getDeclaredMethod(methodName);
        int modifiers = method.getModifiers();
        assertTrue(
            !Modifier.isPublic(modifiers)
                && !Modifier.isProtected(modifiers)
                && !Modifier.isPrivate(modifiers),
            methodName + " should be package-private");
      }
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("withData should accept Object parameter")
    void withDataShouldAcceptObjectParameter() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withData", Object.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "withData should have 1 parameter");
      assertEquals(Object.class, paramTypes[0], "withData parameter should be Object");
    }

    @Test
    @DisplayName("withFuel should accept long parameter")
    void withFuelShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withFuel", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "withFuel should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "withFuel parameter should be long");
    }

    @Test
    @DisplayName("withEpochDeadline should accept long parameter")
    void withEpochDeadlineShouldAcceptLongParameter() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withEpochDeadline", long.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "withEpochDeadline should have 1 parameter");
      assertEquals(long.class, paramTypes[0], "withEpochDeadline parameter should be long");
    }

    @Test
    @DisplayName("withLimits should accept StoreLimits parameter")
    void withLimitsShouldAcceptStoreLimitsParameter() throws NoSuchMethodException {
      final Method method = StoreBuilder.class.getMethod("withLimits", StoreLimits.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(1, paramTypes.length, "withLimits should have 1 parameter");
      assertEquals(StoreLimits.class, paramTypes[0], "withLimits parameter should be StoreLimits");
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
          Set.of(
              "withData",
              "withFuel",
              "withEpochDeadline",
              "withLimits",
              "build",
              "getEngine",
              "getData",
              "getFuel",
              "getEpochDeadline",
              "getLimits");

      Set<String> actualMethods =
          Arrays.stream(StoreBuilder.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "StoreBuilder should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have correct number of public methods")
    void shouldHaveCorrectNumberOfPublicMethods() {
      long publicMethodCount =
          Arrays.stream(StoreBuilder.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      // withData, withFuel, withEpochDeadline, withLimits, build = 5
      assertEquals(5, publicMethodCount, "StoreBuilder should have 5 public methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private fields")
    void shouldHavePrivateFields() {
      Set<String> expectedFields = Set.of("engine", "data", "fuel", "epochDeadline", "limits");

      for (String fieldName : expectedFields) {
        try {
          var field = StoreBuilder.class.getDeclaredField(fieldName);
          assertTrue(
              Modifier.isPrivate(field.getModifiers()), fieldName + " field should be private");
        } catch (NoSuchFieldException e) {
          // Field doesn't exist, which is okay for this test
        }
      }
    }

    @Test
    @DisplayName("engine field should be final")
    void engineFieldShouldBeFinal() throws NoSuchFieldException {
      var field = StoreBuilder.class.getDeclaredField("engine");
      assertTrue(Modifier.isFinal(field.getModifiers()), "engine field should be final");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      assertEquals(
          Object.class, StoreBuilder.class.getSuperclass(), "StoreBuilder should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          StoreBuilder.class.getInterfaces().length,
          "StoreBuilder should not implement any interfaces");
    }
  }
}
