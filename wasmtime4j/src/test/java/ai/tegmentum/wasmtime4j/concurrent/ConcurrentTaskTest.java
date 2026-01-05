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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the ConcurrentTask interface.
 *
 * <p>ConcurrentTask is a functional interface that represents a task that runs in the context of a
 * store's concurrent execution environment.
 */
@DisplayName("ConcurrentTask Interface Tests")
class ConcurrentTaskTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ConcurrentTask.class.isInterface(), "ConcurrentTask should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ConcurrentTask.class.getModifiers()),
          "ConcurrentTask should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(ConcurrentTask.class.getModifiers()),
          "ConcurrentTask should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should be a generic interface with 2 type parameters")
    void shouldBeAGenericInterfaceWith2TypeParameters() {
      TypeVariable<?>[] typeParams = ConcurrentTask.class.getTypeParameters();
      assertEquals(2, typeParams.length, "ConcurrentTask should have exactly 2 type parameters");
      assertEquals("T", typeParams[0].getName(), "First type parameter should be named T");
      assertEquals("R", typeParams[1].getName(), "Second type parameter should be named R");
    }

    @Test
    @DisplayName("should be annotated with @FunctionalInterface")
    void shouldBeAnnotatedWithFunctionalInterface() {
      assertTrue(
          ConcurrentTask.class.isAnnotationPresent(FunctionalInterface.class),
          "ConcurrentTask should be annotated with @FunctionalInterface");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          ConcurrentTask.class.getInterfaces().length,
          "ConcurrentTask should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have execute method")
    void shouldHaveExecuteMethod() throws NoSuchMethodException {
      Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      assertNotNull(method, "execute method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (erased R)");
      assertFalse(method.isDefault(), "execute should be abstract");
    }

    @Test
    @DisplayName("should have exactly 1 abstract method")
    void shouldHaveExactly1AbstractMethod() {
      long abstractMethods =
          Arrays.stream(ConcurrentTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(1, abstractMethods, "ConcurrentTask should have exactly 1 abstract method");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(ConcurrentTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "ConcurrentTask should have no default methods");
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
      Set<String> expectedMethods = Set.of("execute");

      Set<String> actualMethods =
          Arrays.stream(ConcurrentTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "ConcurrentTask should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 1 declared method")
    void shouldHaveExactly1DeclaredMethod() {
      long methodCount =
          Arrays.stream(ConcurrentTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(1, methodCount, "ConcurrentTask should have exactly 1 declared method");
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
          Arrays.stream(ConcurrentTask.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "ConcurrentTask should have no static methods");
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
          ConcurrentTask.class.getDeclaredFields().length,
          "ConcurrentTask should have no declared fields");
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
          ConcurrentTask.class.getDeclaredClasses().length,
          "ConcurrentTask should have no nested classes");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("execute should have 1 parameter")
    void executeShouldHave1Parameter() throws NoSuchMethodException {
      Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      assertEquals(1, method.getParameterCount(), "execute should have 1 parameter");
    }

    @Test
    @DisplayName("execute parameter should be Accessor")
    void executeParameterShouldBeAccessor() throws NoSuchMethodException {
      Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(Accessor.class, paramTypes[0], "execute param should be Accessor");
    }

    @Test
    @DisplayName("execute parameter should be Accessor<T>")
    void executeParameterShouldBeAccessorOfT() throws NoSuchMethodException {
      Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      Type[] genericParamTypes = method.getGenericParameterTypes();
      assertEquals(1, genericParamTypes.length, "Should have 1 generic param type");

      // The parameter type should be Accessor<T>
      assertTrue(
          genericParamTypes[0] instanceof ParameterizedType,
          "Parameter should be parameterized type");
      ParameterizedType paramType = (ParameterizedType) genericParamTypes[0];
      assertEquals(Accessor.class, paramType.getRawType(), "Raw type should be Accessor");

      // Check the type argument is T
      Type[] typeArgs = paramType.getActualTypeArguments();
      assertEquals(1, typeArgs.length, "Should have 1 type argument");
      assertTrue(typeArgs[0] instanceof TypeVariable, "Type argument should be TypeVariable T");
      assertEquals("T", ((TypeVariable<?>) typeArgs[0]).getName(), "Type argument should be T");
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
      Arrays.stream(ConcurrentTask.class.getDeclaredMethods())
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
    @DisplayName("execute should return R")
    void executeShouldReturnR() throws NoSuchMethodException {
      Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      String genericReturn = method.getGenericReturnType().getTypeName();
      assertEquals("R", genericReturn, "execute should return R");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("execute should declare WasmException")
    void executeShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "execute should declare 1 exception");
      assertEquals(WasmException.class, exceptions[0], "execute should declare WasmException");
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
      TypeVariable<?>[] typeParams = ConcurrentTask.class.getTypeParameters();
      assertEquals(2, typeParams.length, "Should have 2 type parameters");
      assertEquals(1, typeParams[0].getBounds().length, "T should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[0].getBounds()[0], "T's bound should be Object");
    }

    @Test
    @DisplayName("type parameter R should have no bounds")
    void typeParameterRShouldHaveNoBounds() {
      TypeVariable<?>[] typeParams = ConcurrentTask.class.getTypeParameters();
      assertEquals(2, typeParams.length, "Should have 2 type parameters");
      assertEquals(1, typeParams[1].getBounds().length, "R should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[1].getBounds()[0], "R's bound should be Object");
    }
  }
}
