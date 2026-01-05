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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the JoinHandle interface.
 *
 * <p>JoinHandle represents a spawned asynchronous task and provides methods to wait for its
 * completion, cancel it, or check its status.
 */
@DisplayName("JoinHandle Interface Tests")
class JoinHandleTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(JoinHandle.class.isInterface(), "JoinHandle should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(JoinHandle.class.getModifiers()), "JoinHandle should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(JoinHandle.class.getModifiers()),
          "JoinHandle should not be final (interfaces cannot be final)");
    }

    @Test
    @DisplayName("should be a generic interface with 1 type parameter")
    void shouldBeAGenericInterfaceWith1TypeParameter() {
      TypeVariable<?>[] typeParams = JoinHandle.class.getTypeParameters();
      assertEquals(1, typeParams.length, "JoinHandle should have exactly 1 type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
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
          JoinHandle.class.getInterfaces().length,
          "JoinHandle should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have join() method")
    void shouldHaveJoinMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join");
      assertNotNull(method, "join() method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (erased T)");
      assertFalse(method.isDefault(), "join() should be abstract");
    }

    @Test
    @DisplayName("should have join(long, TimeUnit) method")
    void shouldHaveJoinWithTimeoutMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      assertNotNull(method, "join(long, TimeUnit) method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (erased T)");
      assertFalse(method.isDefault(), "join(long, TimeUnit) should be abstract");
    }

    @Test
    @DisplayName("should have toFuture method")
    void shouldHaveToFutureMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("toFuture");
      assertNotNull(method, "toFuture method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
      assertFalse(method.isDefault(), "toFuture should be abstract");
    }

    @Test
    @DisplayName("should have isDone method")
    void shouldHaveIsDoneMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isDone");
      assertNotNull(method, "isDone method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isDone should be abstract");
    }

    @Test
    @DisplayName("should have isCancelled method")
    void shouldHaveIsCancelledMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isCancelled");
      assertNotNull(method, "isCancelled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "isCancelled should be abstract");
    }

    @Test
    @DisplayName("should have cancel method")
    void shouldHaveCancelMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      assertNotNull(method, "cancel method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
      assertFalse(method.isDefault(), "cancel should be abstract");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getId should be abstract");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertFalse(method.isDefault(), "getStatus should be abstract");
    }

    @Test
    @DisplayName("should have exactly 8 abstract methods")
    void shouldHaveExactly8AbstractMethods() {
      long abstractMethods =
          Arrays.stream(JoinHandle.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(8, abstractMethods, "JoinHandle should have exactly 8 abstract methods");
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
          Arrays.stream(JoinHandle.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "JoinHandle should have no default methods");
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
          Set.of("join", "toFuture", "isDone", "isCancelled", "cancel", "getId", "getStatus");

      Set<String> actualMethods =
          Arrays.stream(JoinHandle.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "JoinHandle should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 8 declared methods")
    void shouldHaveExactly8DeclaredMethods() {
      long methodCount =
          Arrays.stream(JoinHandle.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(8, methodCount, "JoinHandle should have exactly 8 declared methods");
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
          Arrays.stream(JoinHandle.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "JoinHandle should have no static methods");
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
          JoinHandle.class.getDeclaredFields().length,
          "JoinHandle should have no declared fields");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have exactly 1 nested class")
    void shouldHaveExactly1NestedClass() {
      assertEquals(
          1,
          JoinHandle.class.getDeclaredClasses().length,
          "JoinHandle should have exactly 1 nested class");
    }

    @Test
    @DisplayName("should have TaskStatus nested enum")
    void shouldHaveTaskStatusNestedEnum() {
      Class<?>[] nestedClasses = JoinHandle.class.getDeclaredClasses();
      assertEquals(1, nestedClasses.length, "Should have 1 nested class");
      assertEquals(
          "TaskStatus", nestedClasses[0].getSimpleName(), "Nested class should be TaskStatus");
      assertTrue(nestedClasses[0].isEnum(), "TaskStatus should be an enum");
    }

    @Test
    @DisplayName("TaskStatus should be public")
    void taskStatusShouldBePublic() {
      Class<?>[] nestedClasses = JoinHandle.class.getDeclaredClasses();
      assertTrue(Modifier.isPublic(nestedClasses[0].getModifiers()), "TaskStatus should be public");
    }

    @Test
    @DisplayName("TaskStatus should have expected values")
    void taskStatusShouldHaveExpectedValues() {
      Class<?>[] nestedClasses = JoinHandle.class.getDeclaredClasses();
      Class<?> taskStatusClass = nestedClasses[0];
      assertTrue(taskStatusClass.isEnum(), "TaskStatus should be an enum");

      Set<String> expectedValues = Set.of("PENDING", "RUNNING", "COMPLETED", "FAILED", "CANCELLED");
      Set<String> actualValues =
          Arrays.stream(taskStatusClass.getEnumConstants())
              .map(e -> ((Enum<?>) e).name())
              .collect(Collectors.toSet());

      assertEquals(expectedValues, actualValues, "TaskStatus should have expected enum values");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("join() should have no parameters")
    void joinShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join");
      assertEquals(0, method.getParameterCount(), "join() should have no parameters");
    }

    @Test
    @DisplayName("join(long, TimeUnit) should have 2 parameters")
    void joinWithTimeoutShouldHave2Parameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      assertEquals(2, method.getParameterCount(), "join(long, TimeUnit) should have 2 parameters");
    }

    @Test
    @DisplayName("join(long, TimeUnit) parameters should be correct types")
    void joinWithTimeoutParametersShouldBeCorrectTypes() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(long.class, paramTypes[0], "First param should be long");
      assertEquals(TimeUnit.class, paramTypes[1], "Second param should be TimeUnit");
    }

    @Test
    @DisplayName("cancel should have 1 parameter")
    void cancelShouldHave1Parameter() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      assertEquals(1, method.getParameterCount(), "cancel should have 1 parameter");
    }

    @Test
    @DisplayName("cancel parameter should be boolean")
    void cancelParameterShouldBeBoolean() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      Class<?>[] paramTypes = method.getParameterTypes();
      assertEquals(boolean.class, paramTypes[0], "cancel param should be primitive boolean");
    }

    @Test
    @DisplayName("toFuture should have no parameters")
    void toFutureShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("toFuture");
      assertEquals(0, method.getParameterCount(), "toFuture should have no parameters");
    }

    @Test
    @DisplayName("isDone should have no parameters")
    void isDoneShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isDone");
      assertEquals(0, method.getParameterCount(), "isDone should have no parameters");
    }

    @Test
    @DisplayName("isCancelled should have no parameters")
    void isCancelledShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isCancelled");
      assertEquals(0, method.getParameterCount(), "isCancelled should have no parameters");
    }

    @Test
    @DisplayName("getId should have no parameters")
    void getIdShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getId");
      assertEquals(0, method.getParameterCount(), "getId should have no parameters");
    }

    @Test
    @DisplayName("getStatus should have no parameters")
    void getStatusShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getStatus");
      assertEquals(0, method.getParameterCount(), "getStatus should have no parameters");
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
      Arrays.stream(JoinHandle.class.getDeclaredMethods())
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
    @DisplayName("join() should return T")
    void joinShouldReturnT() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join");
      String genericReturn = method.getGenericReturnType().getTypeName();
      assertEquals("T", genericReturn, "join() should return T");
    }

    @Test
    @DisplayName("join(long, TimeUnit) should return T")
    void joinWithTimeoutShouldReturnT() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      String genericReturn = method.getGenericReturnType().getTypeName();
      assertEquals("T", genericReturn, "join(long, TimeUnit) should return T");
    }

    @Test
    @DisplayName("isDone should return primitive boolean")
    void isDoneShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isDone");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isDone should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("isCancelled should return primitive boolean")
    void isCancelledShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isCancelled");
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "isCancelled should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("cancel should return primitive boolean")
    void cancelShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      assertEquals(
          boolean.class,
          method.getReturnType(),
          "cancel should return primitive boolean, not Boolean");
    }

    @Test
    @DisplayName("getId should return primitive long")
    void getIdShouldReturnPrimitiveLong() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getId");
      assertEquals(
          long.class, method.getReturnType(), "getId should return primitive long, not Long");
    }
  }

  // ========================================================================
  // Exception Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Tests")
  class ExceptionTests {

    @Test
    @DisplayName("join() should declare WasmException and InterruptedException")
    void joinShouldDeclareExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join");
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(2, exceptions.length, "join() should declare 2 exceptions");
      Set<Class<?>> exceptionSet = Set.of(exceptions);
      assertTrue(exceptionSet.contains(WasmException.class), "Should declare WasmException");
      assertTrue(
          exceptionSet.contains(InterruptedException.class), "Should declare InterruptedException");
    }

    @Test
    @DisplayName("join(long, TimeUnit) should declare WasmException and InterruptedException")
    void joinWithTimeoutShouldDeclareExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(2, exceptions.length, "join(long, TimeUnit) should declare 2 exceptions");
      Set<Class<?>> exceptionSet = Set.of(exceptions);
      assertTrue(exceptionSet.contains(WasmException.class), "Should declare WasmException");
      assertTrue(
          exceptionSet.contains(InterruptedException.class), "Should declare InterruptedException");
    }

    @Test
    @DisplayName("toFuture should not declare any exceptions")
    void toFutureShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("toFuture");
      assertEquals(0, method.getExceptionTypes().length, "toFuture should not declare exceptions");
    }

    @Test
    @DisplayName("isDone should not declare any exceptions")
    void isDoneShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isDone");
      assertEquals(0, method.getExceptionTypes().length, "isDone should not declare exceptions");
    }

    @Test
    @DisplayName("isCancelled should not declare any exceptions")
    void isCancelledShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("isCancelled");
      assertEquals(
          0, method.getExceptionTypes().length, "isCancelled should not declare exceptions");
    }

    @Test
    @DisplayName("cancel should not declare any exceptions")
    void cancelShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      assertEquals(0, method.getExceptionTypes().length, "cancel should not declare exceptions");
    }

    @Test
    @DisplayName("getId should not declare any exceptions")
    void getIdShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getId");
      assertEquals(0, method.getExceptionTypes().length, "getId should not declare exceptions");
    }

    @Test
    @DisplayName("getStatus should not declare any exceptions")
    void getStatusShouldNotDeclareAnyExceptions() throws NoSuchMethodException {
      Method method = JoinHandle.class.getMethod("getStatus");
      assertEquals(0, method.getExceptionTypes().length, "getStatus should not declare exceptions");
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
      TypeVariable<?>[] typeParams = JoinHandle.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have 1 type parameter");
      assertEquals(1, typeParams[0].getBounds().length, "T should have 1 bound (Object)");
      assertEquals(Object.class, typeParams[0].getBounds()[0], "T's bound should be Object");
    }
  }
}
