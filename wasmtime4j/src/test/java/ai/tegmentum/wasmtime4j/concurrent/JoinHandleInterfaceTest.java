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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JoinHandle} interface.
 *
 * <p>JoinHandle represents a handle to a spawned concurrent task.
 */
@DisplayName("JoinHandle Interface Tests")
class JoinHandleInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(JoinHandle.class.isInterface(), "JoinHandle should be an interface");
    }

    @Test
    @DisplayName("should have generic type parameter T")
    void shouldHaveGenericTypeParameterT() {
      final TypeVariable<?>[] typeParams = JoinHandle.class.getTypeParameters();
      assertEquals(1, typeParams.length, "Should have one type parameter");
      assertEquals("T", typeParams[0].getName(), "Type parameter should be named T");
    }

    @Test
    @DisplayName("should have join method without parameters")
    void shouldHaveJoinMethodWithoutParameters() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("join");
      assertNotNull(method, "join method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }

    @Test
    @DisplayName("should have join method with timeout parameters")
    void shouldHaveJoinMethodWithTimeoutParameters() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      assertNotNull(method, "join(long, TimeUnit) method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object (due to erasure)");
    }

    @Test
    @DisplayName("should have toFuture method")
    void shouldHaveToFutureMethod() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("toFuture");
      assertNotNull(method, "toFuture method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have isDone method")
    void shouldHaveIsDoneMethod() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("isDone");
      assertNotNull(method, "isDone method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isCancelled method")
    void shouldHaveIsCancelledMethod() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("isCancelled");
      assertNotNull(method, "isCancelled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have cancel method")
    void shouldHaveCancelMethod() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      assertNotNull(method, "cancel method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("getId");
      assertNotNull(method, "getId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(JoinHandle.TaskStatus.class, method.getReturnType(), "Should return TaskStatus");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("join without parameters should have no parameters")
    void joinWithoutParametersShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("join");
      assertEquals(0, method.getParameterCount(), "join() should have no parameters");
    }

    @Test
    @DisplayName("join with timeout should have two parameters")
    void joinWithTimeoutShouldHaveTwoParameters() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      assertEquals(
          2, method.getParameterCount(), "join(long, TimeUnit) should have two parameters");
      assertEquals(long.class, method.getParameterTypes()[0], "First parameter should be long");
      assertEquals(
          TimeUnit.class, method.getParameterTypes()[1], "Second parameter should be TimeUnit");
    }

    @Test
    @DisplayName("cancel should have mayInterruptIfRunning parameter")
    void cancelShouldHaveMayInterruptIfRunningParameter() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("cancel", boolean.class);
      assertEquals(1, method.getParameterCount(), "cancel should have one parameter");
      assertEquals(boolean.class, method.getParameterTypes()[0], "Parameter should be boolean");
    }
  }

  @Nested
  @DisplayName("Exception Declaration Tests")
  class ExceptionDeclarationTests {

    @Test
    @DisplayName("join should declare WasmException and InterruptedException")
    void joinShouldDeclareExceptions() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("join");
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      boolean hasInterruptedException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
        }
        if (exType == InterruptedException.class) {
          hasInterruptedException = true;
        }
      }
      assertTrue(hasWasmException, "join method should declare WasmException");
      assertTrue(hasInterruptedException, "join method should declare InterruptedException");
    }

    @Test
    @DisplayName("join with timeout should declare WasmException and InterruptedException")
    void joinWithTimeoutShouldDeclareExceptions() throws NoSuchMethodException {
      final Method method = JoinHandle.class.getMethod("join", long.class, TimeUnit.class);
      final Class<?>[] exceptionTypes = method.getExceptionTypes();

      boolean hasWasmException = false;
      boolean hasInterruptedException = false;
      for (final Class<?> exType : exceptionTypes) {
        if (exType.getSimpleName().equals("WasmException")) {
          hasWasmException = true;
        }
        if (exType == InterruptedException.class) {
          hasInterruptedException = true;
        }
      }
      assertTrue(hasWasmException, "join(long, TimeUnit) method should declare WasmException");
      assertTrue(
          hasInterruptedException,
          "join(long, TimeUnit) method should declare InterruptedException");
    }
  }

  @Nested
  @DisplayName("TaskStatus Enum Tests")
  class TaskStatusEnumTests {

    @Test
    @DisplayName("TaskStatus should be an enum")
    void taskStatusShouldBeAnEnum() {
      assertTrue(JoinHandle.TaskStatus.class.isEnum(), "TaskStatus should be an enum");
    }

    @Test
    @DisplayName("TaskStatus should have PENDING value")
    void taskStatusShouldHavePendingValue() {
      assertNotNull(JoinHandle.TaskStatus.valueOf("PENDING"), "Should have PENDING value");
    }

    @Test
    @DisplayName("TaskStatus should have RUNNING value")
    void taskStatusShouldHaveRunningValue() {
      assertNotNull(JoinHandle.TaskStatus.valueOf("RUNNING"), "Should have RUNNING value");
    }

    @Test
    @DisplayName("TaskStatus should have COMPLETED value")
    void taskStatusShouldHaveCompletedValue() {
      assertNotNull(JoinHandle.TaskStatus.valueOf("COMPLETED"), "Should have COMPLETED value");
    }

    @Test
    @DisplayName("TaskStatus should have FAILED value")
    void taskStatusShouldHaveFailedValue() {
      assertNotNull(JoinHandle.TaskStatus.valueOf("FAILED"), "Should have FAILED value");
    }

    @Test
    @DisplayName("TaskStatus should have CANCELLED value")
    void taskStatusShouldHaveCancelledValue() {
      assertNotNull(JoinHandle.TaskStatus.valueOf("CANCELLED"), "Should have CANCELLED value");
    }

    @Test
    @DisplayName("TaskStatus should have exactly 5 values")
    void taskStatusShouldHaveFiveValues() {
      assertEquals(5, JoinHandle.TaskStatus.values().length, "Should have exactly 5 status values");
    }

    @Test
    @DisplayName("TaskStatus values should be in correct order")
    void taskStatusValuesShouldBeInCorrectOrder() {
      final JoinHandle.TaskStatus[] values = JoinHandle.TaskStatus.values();
      assertEquals(JoinHandle.TaskStatus.PENDING, values[0], "First value should be PENDING");
      assertEquals(JoinHandle.TaskStatus.RUNNING, values[1], "Second value should be RUNNING");
      assertEquals(JoinHandle.TaskStatus.COMPLETED, values[2], "Third value should be COMPLETED");
      assertEquals(JoinHandle.TaskStatus.FAILED, values[3], "Fourth value should be FAILED");
      assertEquals(JoinHandle.TaskStatus.CANCELLED, values[4], "Fifth value should be CANCELLED");
    }
  }
}
