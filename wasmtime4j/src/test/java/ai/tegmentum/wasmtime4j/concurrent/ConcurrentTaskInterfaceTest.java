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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ConcurrentTask} interface.
 *
 * <p>ConcurrentTask is a functional interface for tasks executed with store data access.
 */
@DisplayName("ConcurrentTask Interface Tests")
class ConcurrentTaskInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ConcurrentTask.class.isInterface(), "ConcurrentTask should be an interface");
    }

    @Test
    @DisplayName("should be a functional interface")
    void shouldBeAFunctionalInterface() {
      assertTrue(
          ConcurrentTask.class.isAnnotationPresent(FunctionalInterface.class),
          "ConcurrentTask should be annotated with @FunctionalInterface");
    }

    @Test
    @DisplayName("should have two generic type parameters")
    void shouldHaveTwoGenericTypeParameters() {
      final TypeVariable<?>[] typeParams = ConcurrentTask.class.getTypeParameters();
      assertEquals(2, typeParams.length, "Should have two type parameters");
      assertEquals("T", typeParams[0].getName(), "First type parameter should be named T");
      assertEquals("R", typeParams[1].getName(), "Second type parameter should be named R");
    }

    @Test
    @DisplayName("should have execute method")
    void shouldHaveExecuteMethod() throws NoSuchMethodException {
      final Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      assertNotNull(method, "execute method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("execute should have one parameter")
    void executeShouldHaveOneParameter() throws NoSuchMethodException {
      final Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      assertEquals(1, method.getParameterCount(), "execute should have one parameter");
    }

    @Test
    @DisplayName("execute parameter should be Accessor")
    void executeParameterShouldBeAccessor() throws NoSuchMethodException {
      final Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      assertEquals(Accessor.class, method.getParameterTypes()[0], "Parameter should be Accessor");
    }

    @Test
    @DisplayName("execute should not be default")
    void executeShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      assertFalse(method.isDefault(), "execute should not be a default method");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("execute should declare WasmException")
    void executeShouldDeclareWasmException() throws NoSuchMethodException {
      final Method method = ConcurrentTask.class.getMethod("execute", Accessor.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
          break;
        }
      }
      assertTrue(hasWasmException, "execute method should declare WasmException");
    }
  }

  @Nested
  @DisplayName("Functional Interface Tests")
  class FunctionalInterfaceTests {

    @Test
    @DisplayName("should have exactly one abstract method")
    void shouldHaveExactlyOneAbstractMethod() {
      int abstractMethodCount = 0;
      for (final Method method : ConcurrentTask.class.getDeclaredMethods()) {
        if (!method.isDefault() && !Modifier.isStatic(method.getModifiers())) {
          abstractMethodCount++;
        }
      }
      assertEquals(1, abstractMethodCount, "Should have exactly one abstract method (execute)");
    }
  }
}
