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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmThread interface.
 *
 * <p>WasmThread represents a WebAssembly thread for multi-threaded WebAssembly execution with
 * shared memory and atomic operations. This test verifies the interface structure and method
 * signatures.
 */
@DisplayName("WasmThread Interface Tests")
class WasmThreadInterfaceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmThread.class.isInterface(), "WasmThread should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasmThread.class.getModifiers()), "WasmThread should be public");
    }

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasmThread.class),
          "WasmThread should extend AutoCloseable");
    }
  }

  // ========================================================================
  // Thread Identity and State Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Thread Identity and State Methods Tests")
  class ThreadIdentityAndStateMethodsTests {

    @Test
    @DisplayName("should have getThreadId method")
    void shouldHaveGetThreadIdMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("getThreadId");
      assertNotNull(method, "getThreadId method should exist");
      assertEquals(long.class, method.getReturnType(), "getThreadId should return long");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(
          WasmThreadState.class, method.getReturnType(), "getState should return WasmThreadState");
    }

    @Test
    @DisplayName("should have isAlive method")
    void shouldHaveIsAliveMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("isAlive");
      assertNotNull(method, "isAlive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isAlive should return boolean");
    }

    @Test
    @DisplayName("should have isTerminationRequested method")
    void shouldHaveIsTerminationRequestedMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("isTerminationRequested");
      assertNotNull(method, "isTerminationRequested method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isTerminationRequested should return boolean");
    }
  }

  // ========================================================================
  // Execution Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Execution Methods Tests")
  class ExecutionMethodsTests {

    @Test
    @DisplayName("should have executeFunction method")
    void shouldHaveExecuteFunctionMethod() throws NoSuchMethodException {
      final Method method =
          WasmThread.class.getMethod("executeFunction", WasmFunction.class, WasmValue[].class);
      assertNotNull(method, "executeFunction method should exist");
      assertEquals(Future.class, method.getReturnType(), "executeFunction should return Future");
    }

    @Test
    @DisplayName("should have executeOperation method")
    void shouldHaveExecuteOperationMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("executeOperation", Supplier.class);
      assertNotNull(method, "executeOperation method should exist");
      assertEquals(Future.class, method.getReturnType(), "executeOperation should return Future");
    }
  }

  // ========================================================================
  // Join Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Join Methods Tests")
  class JoinMethodsTests {

    @Test
    @DisplayName("should have join method without timeout")
    void shouldHaveJoinMethodWithoutTimeout() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("join");
      assertNotNull(method, "join method should exist");
      assertEquals(void.class, method.getReturnType(), "join should return void");
    }

    @Test
    @DisplayName("should have join method with timeout")
    void shouldHaveJoinMethodWithTimeout() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("join", long.class);
      assertNotNull(method, "join(long) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "join(long) should return boolean");
    }
  }

  // ========================================================================
  // Termination Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Termination Methods Tests")
  class TerminationMethodsTests {

    @Test
    @DisplayName("should have terminate method")
    void shouldHaveTerminateMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("terminate");
      assertNotNull(method, "terminate method should exist");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "terminate should return CompletableFuture");
    }

    @Test
    @DisplayName("should have forceTerminate method")
    void shouldHaveForceTerminateMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("forceTerminate");
      assertNotNull(method, "forceTerminate method should exist");
      assertEquals(void.class, method.getReturnType(), "forceTerminate should return void");
    }
  }

  // ========================================================================
  // Resource Access Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Resource Access Methods Tests")
  class ResourceAccessMethodsTests {

    @Test
    @DisplayName("should have getSharedMemory method")
    void shouldHaveGetSharedMemoryMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("getSharedMemory");
      assertNotNull(method, "getSharedMemory method should exist");
      assertEquals(
          WasmMemory.class, method.getReturnType(), "getSharedMemory should return WasmMemory");
    }

    @Test
    @DisplayName("should have getThreadLocalStorage method")
    void shouldHaveGetThreadLocalStorageMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("getThreadLocalStorage");
      assertNotNull(method, "getThreadLocalStorage method should exist");
      assertEquals(
          WasmThreadLocalStorage.class,
          method.getReturnType(),
          "getThreadLocalStorage should return WasmThreadLocalStorage");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          WasmThreadStatistics.class,
          method.getReturnType(),
          "getStatistics should return WasmThreadStatistics");
    }
  }

  // ========================================================================
  // Lifecycle Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Lifecycle Methods Tests")
  class LifecycleMethodsTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("executeFunction should have 2 parameters")
    void executeFunctionShouldHave2Parameters() throws NoSuchMethodException {
      final Method method =
          WasmThread.class.getMethod("executeFunction", WasmFunction.class, WasmValue[].class);
      assertEquals(2, method.getParameterCount(), "executeFunction should have 2 parameters");
      assertEquals(
          WasmFunction.class,
          method.getParameterTypes()[0],
          "First parameter should be WasmFunction");
      assertEquals(
          WasmValue[].class,
          method.getParameterTypes()[1],
          "Second parameter should be WasmValue[]");
    }

    @Test
    @DisplayName("executeOperation should have 1 Supplier parameter")
    void executeOperationShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("executeOperation", Supplier.class);
      assertEquals(1, method.getParameterCount(), "executeOperation should have 1 parameter");
      assertEquals(Supplier.class, method.getParameterTypes()[0], "Parameter should be Supplier");
    }

    @Test
    @DisplayName("join with timeout should have 1 long parameter")
    void joinWithTimeoutShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = WasmThread.class.getMethod("join", long.class);
      assertEquals(1, method.getParameterCount(), "join(long) should have 1 parameter");
      assertEquals(long.class, method.getParameterTypes()[0], "Parameter should be long");
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
              "getThreadId",
              "getState",
              "executeFunction",
              "executeOperation",
              "join",
              "terminate",
              "forceTerminate",
              "getSharedMemory",
              "getThreadLocalStorage",
              "isAlive",
              "isTerminationRequested",
              "getStatistics",
              "close");

      Set<String> actualMethods =
          Arrays.stream(WasmThread.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "WasmThread should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 14 declared methods")
    void shouldHaveAtLeast14DeclaredMethods() {
      assertTrue(
          WasmThread.class.getDeclaredMethods().length >= 14,
          "WasmThread should have at least 14 methods (found "
              + WasmThread.class.getDeclaredMethods().length
              + ")");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend AutoCloseable interface")
    void shouldExtendAutoCloseableInterface() {
      Class<?>[] interfaces = WasmThread.class.getInterfaces();
      boolean extendsAutoCloseable =
          Arrays.stream(interfaces).anyMatch(i -> i.equals(AutoCloseable.class));
      assertTrue(extendsAutoCloseable, "WasmThread should extend AutoCloseable interface");
    }
  }
}
