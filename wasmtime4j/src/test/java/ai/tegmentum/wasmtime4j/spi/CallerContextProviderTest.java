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

package ai.tegmentum.wasmtime4j.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Caller;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the CallerContextProvider interface.
 *
 * <p>CallerContextProvider is an SPI interface for accessing caller context in host functions. It
 * allows runtime implementations (JNI, Panama) to provide caller context to CallerAwareHostFunction
 * instances.
 */
@DisplayName("CallerContextProvider Interface Tests")
class CallerContextProviderTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          CallerContextProvider.class.isInterface(),
          "CallerContextProvider should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(CallerContextProvider.class.getModifiers()),
          "CallerContextProvider should be public");
    }

    @Test
    @DisplayName("should not be abstract modifier (interfaces are implicitly abstract)")
    void shouldBeInterface() {
      // Interfaces are inherently abstract, but the modifier is not always present
      assertTrue(
          CallerContextProvider.class.isInterface(),
          "CallerContextProvider should be an interface");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0,
          CallerContextProvider.class.getInterfaces().length,
          "CallerContextProvider should not extend any interface");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getCurrentCaller method")
    void shouldHaveGetCurrentCallerMethod() throws NoSuchMethodException {
      Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertNotNull(method, "getCurrentCaller method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertFalse(Modifier.isStatic(method.getModifiers()), "Should not be static");
      assertEquals(Caller.class, method.getReturnType(), "Should return Caller");
    }

    @Test
    @DisplayName("getCurrentCaller should have type parameter T")
    void getCurrentCallerShouldHaveTypeParameter() throws NoSuchMethodException {
      Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      TypeVariable<?>[] typeParams = method.getTypeParameters();
      assertEquals(1, typeParams.length, "getCurrentCaller should have 1 type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("getCurrentCaller should be abstract")
    void getCurrentCallerShouldBeAbstract() throws NoSuchMethodException {
      Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertTrue(
          Modifier.isAbstract(method.getModifiers()),
          "getCurrentCaller should be abstract (interface method)");
    }

    @Test
    @DisplayName("getCurrentCaller should have no parameters")
    void getCurrentCallerShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      assertEquals(0, method.getParameterCount(), "getCurrentCaller should have no parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly one declared method")
    void shouldHaveExactlyOneDeclaredMethod() {
      long declaredMethodCount =
          Arrays.stream(CallerContextProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(1, declaredMethodCount, "Should have exactly 1 declared method");
    }

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      long abstractMethodCount =
          Arrays.stream(CallerContextProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractMethodCount, "Should have exactly 1 abstract method");
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
          0,
          CallerContextProvider.class.getDeclaredFields().length,
          "CallerContextProvider should have no declared fields");
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
          CallerContextProvider.class.getDeclaredClasses().length,
          "CallerContextProvider should have no nested classes");
    }
  }

  // ========================================================================
  // Functional Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Functional Interface Tests")
  class FunctionalInterfaceTests {

    @Test
    @DisplayName("should be a functional interface (single abstract method)")
    void shouldBeFunctionalInterface() {
      // A functional interface has exactly one abstract method
      long abstractMethodCount =
          Arrays.stream(CallerContextProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(
          1,
          abstractMethodCount,
          "CallerContextProvider should qualify as a functional interface (1 abstract method)");
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(CallerContextProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethodCount, "Should have no default methods");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethodCount =
          Arrays.stream(CallerContextProvider.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethodCount, "Should have no static methods");
    }
  }

  // ========================================================================
  // Package Tests
  // ========================================================================

  @Nested
  @DisplayName("Package Tests")
  class PackageTests {

    @Test
    @DisplayName("should be in spi package")
    void shouldBeInSpiPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.spi",
          CallerContextProvider.class.getPackage().getName(),
          "CallerContextProvider should be in ai.tegmentum.wasmtime4j.spi package");
    }
  }

  // ========================================================================
  // Generic Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Generic Return Type Tests")
  class GenericReturnTypeTests {

    @Test
    @DisplayName("getCurrentCaller should return parameterized Caller")
    void getCurrentCallerShouldReturnParameterizedCaller() throws NoSuchMethodException {
      Method method = CallerContextProvider.class.getMethod("getCurrentCaller");
      java.lang.reflect.Type genericReturnType = method.getGenericReturnType();

      // The return type should be Caller<T> where T is the type parameter
      assertTrue(
          genericReturnType instanceof java.lang.reflect.ParameterizedType,
          "Return type should be parameterized");

      java.lang.reflect.ParameterizedType parameterizedType =
          (java.lang.reflect.ParameterizedType) genericReturnType;
      assertEquals(Caller.class, parameterizedType.getRawType(), "Raw type should be Caller");

      java.lang.reflect.Type[] typeArgs = parameterizedType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertTrue(typeArgs[0] instanceof TypeVariable, "Type argument should be a TypeVariable");
      assertEquals(
          "T", ((TypeVariable<?>) typeArgs[0]).getName(), "Type argument should be named T");
    }
  }
}
