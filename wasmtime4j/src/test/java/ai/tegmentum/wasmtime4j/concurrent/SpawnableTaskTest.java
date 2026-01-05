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

package ai.tegmentum.wasmtime4j.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the SpawnableTask interface.
 *
 * <p>SpawnableTask is a functional interface that extends Callable and represents a task that can
 * be spawned for concurrent execution.
 */
@DisplayName("SpawnableTask Interface Tests")
class SpawnableTaskTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(SpawnableTask.class.isInterface(), "SpawnableTask should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(SpawnableTask.class.getModifiers()), "SpawnableTask should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(SpawnableTask.class.getModifiers()),
          "SpawnableTask should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should be a generic interface with 1 type parameter")
    void shouldBeAGenericInterfaceWith1TypeParameter() {
      TypeVariable<?>[] typeParams = SpawnableTask.class.getTypeParameters();
      assertEquals(1, typeParams.length, "SpawnableTask should have exactly 1 type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should be annotated with @FunctionalInterface")
    void shouldBeAnnotatedWithFunctionalInterface() {
      assertTrue(
          SpawnableTask.class.isAnnotationPresent(FunctionalInterface.class),
          "SpawnableTask should be annotated with @FunctionalInterface");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Callable")
    void shouldExtendCallable() {
      Class<?>[] interfaces = SpawnableTask.class.getInterfaces();
      assertEquals(1, interfaces.length, "SpawnableTask should implement exactly 1 interface");
      assertEquals(Callable.class, interfaces[0], "SpawnableTask should extend Callable");
    }

    @Test
    @DisplayName("should be assignable to Callable")
    void shouldBeAssignableToCallable() {
      assertTrue(
          Callable.class.isAssignableFrom(SpawnableTask.class),
          "Callable should be assignable from SpawnableTask");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have run method")
    void shouldHaveRunMethod() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("run");
      assertNotNull(method, "run method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (erased T)");
      assertFalse(method.isDefault(), "run should be abstract");
    }

    @Test
    @DisplayName("should have exactly 1 abstract method")
    void shouldHaveExactly1AbstractMethod() {
      long abstractMethods =
          Arrays.stream(SpawnableTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractMethods, "SpawnableTask should have exactly 1 abstract method");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have call method as default")
    void shouldHaveCallMethodAsDefault() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("call");
      assertNotNull(method, "call method should exist");
      assertTrue(method.isDefault(), "call should be a default method");
    }

    @Test
    @DisplayName("should have exactly 1 default method")
    void shouldHaveExactly1DefaultMethod() {
      long defaultMethods =
          Arrays.stream(SpawnableTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(1, defaultMethods, "SpawnableTask should have exactly 1 default method");
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
      Set<String> expectedMethods = Set.of("run", "call");

      Set<String> actualMethods =
          Arrays.stream(SpawnableTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "SpawnableTask should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 2 declared methods")
    void shouldHaveExactly2DeclaredMethods() {
      long methodCount =
          Arrays.stream(SpawnableTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(2, methodCount, "SpawnableTask should have exactly 2 declared methods");
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
          Arrays.stream(SpawnableTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "SpawnableTask should have no static methods");
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
          SpawnableTask.class.getDeclaredFields().length,
          "SpawnableTask should have no declared fields");
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
          SpawnableTask.class.getDeclaredClasses().length,
          "SpawnableTask should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("run should have no parameters")
    void runShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("run");
      assertEquals(0, method.getParameterCount(), "run should have no parameters");
    }

    @Test
    @DisplayName("call should have no parameters")
    void callShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("call");
      assertEquals(0, method.getParameterCount(), "call should have no parameters");
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(SpawnableTask.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Semantic Tests
  // ========================================================================

  @Nested
  @DisplayName("Semantic Tests")
  class SemanticTests {

    @Test
    @DisplayName("run should return T")
    void runShouldReturnT() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("run");
      String genericReturn = method.getGenericReturnType().getTypeName();
      assertEquals("T", genericReturn, "run should return T");
    }

    @Test
    @DisplayName("call should return T")
    void callShouldReturnT() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("call");
      String genericReturn = method.getGenericReturnType().getTypeName();
      assertEquals("T", genericReturn, "call should return T");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("run should declare WasmException")
    void runShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("run");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "run should declare 1 exception");
      assertEquals(WasmException.class, exceptions[0], "run should declare WasmException");
    }

    @Test
    @DisplayName("call should declare Exception")
    void callShouldDeclareException() throws NoSuchMethodException {
      Method method = SpawnableTask.class.getMethod("call");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "call should declare 1 exception");
      assertEquals(Exception.class, exceptions[0], "call should declare Exception");
    }
  }

  // ========================================================================
  // Type Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Parameter Tests")
  class TypeParameterTests {

    @Test
    @DisplayName("type parameter T should have no bounds")
    void typeParameterTShouldHaveNoBounds() {
      TypeVariable<?>[] typeParams = SpawnableTask.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have 1 type parameter");
      assertEquals(1, typeParams[0].getBounds().length, "T should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[0].getBounds()[0], "T's bound should be Object");
    }
  }
}
