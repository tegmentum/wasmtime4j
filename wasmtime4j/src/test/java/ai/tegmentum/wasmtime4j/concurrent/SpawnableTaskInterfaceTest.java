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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link SpawnableTask} interface.
 *
 * <p>SpawnableTask is a functional interface for spawnable concurrent tasks.
 */
@DisplayName("SpawnableTask Interface Tests")
class SpawnableTaskInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(SpawnableTask.class.isInterface(), "SpawnableTask should be an interface");
    }

    @Test
    @DisplayName("should be a functional interface")
    void shouldBeAFunctionalInterface() {
      assertTrue(
          SpawnableTask.class.isAnnotationPresent(FunctionalInterface.class),
          "SpawnableTask should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("should extend Callable")
    void shouldExtendCallable() {
      assertTrue(
          Callable.class.isAssignableFrom(SpawnableTask.class),
          "SpawnableTask should extend Callable");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameterT() {
      final TypeVariable<?>[] typeParams = SpawnableTask.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have run method")
    void shouldHaveRunMethod() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("run");
      assertNotNull(method, "run method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }

    @Test
    @DisplayName("should have call method from Callable")
    void shouldHaveCallMethodFromCallable() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("call");
      assertNotNull(method, "call method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("run method should have no parameters")
    void runMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("run");
      assertEquals(0, method.getParameterCount(), "run should have no parameters");
    }

    @Test
    @DisplayName("call method should have no parameters")
    void callMethodShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("call");
      assertEquals(0, method.getParameterCount(), "call should have no parameters");
    }

    @Test
    @DisplayName("call method should be default")
    void callMethodShouldBeDefault() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("call");
      assertTrue(method.isDefault(), "call should be a default method");
    }

    @Test
    @DisplayName("run method should not be default")
    void runMethodShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("run");
      assertTrue(!method.isDefault(), "run should not be a default method");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("run method should declare WasmException")
    void runMethodShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("run");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "run method should declare WasmException");
    }

    @Test
    @DisplayName("call method should declare Exception")
    void callMethodShouldDeclareException() throws NoSuchMethodException {
      final Method method = SpawnableTask.class.getMethod("call");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType == Exception.class || Exception.class.isAssignableFrom(exType)) {
          hasException = true;
          break;
        }
      }
      assertTrue(hasException, "call method should declare Exception");
    }
  }

  @Nested
  @DisplayName("Functional Interface Tests")
  class FunctionalInterfaceTests {

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      int abstractMethodCount = 0;
      for (final Method method : SpawnableTask.class.getDeclaredMethods()) {
        if (!method.isDefault() && !java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          abstractMethodCount++;
        }
      }
      assertEquals(1, abstractMethodCount, "Should have exactly one abstract method (run)");
    }
  }
}
