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

import ai.tegmentum.wasmtime4j.exception.WasmException;
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
 * <p>WasmThread represents a WebAssembly thread for multi-threaded execution with shared memory and
 * atomic operations.
 */
@DisplayName("WasmThread Interface Tests")
class WasmThreadTest {

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
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend AutoCloseable")
    void shouldExtendAutoCloseable() {
      assertTrue(
          AutoCloseable.class.isAssignableFrom(WasmThread.class),
          "WasmThread should extend AutoCloseable");
    }

    @Test
    @DisplayName("should have exactly 1 interface")
    void shouldHaveExactlyOneInterface() {
      assertEquals(
          1, WasmThread.class.getInterfaces().length, "WasmThread should extend 1 interface");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getThreadId method")
    void shouldHaveGetThreadIdMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("getThreadId");
      assertNotNull(method, "getThreadId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(WasmThreadState.class, method.getReturnType(), "Should return WasmThreadState");
    }

    @Test
    @DisplayName("should have executeFunction method")
    void shouldHaveExecuteFunctionMethod() throws NoSuchMethodException {
      Method method =
          WasmThread.class.getMethod("executeFunction", WasmFunction.class, WasmValue[].class);
      assertNotNull(method, "executeFunction method should exist");
      assertEquals(Future.class, method.getReturnType(), "Should return Future");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have executeOperation method")
    void shouldHaveExecuteOperationMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("executeOperation", Supplier.class);
      assertNotNull(method, "executeOperation method should exist");
      assertEquals(Future.class, method.getReturnType(), "Should return Future");
      assertEquals(1, method.getTypeParameters().length, "Should have type parameter T");
    }

    @Test
    @DisplayName("should have join method without timeout")
    void shouldHaveJoinMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("join");
      assertNotNull(method, "join() method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      Set<Class<?>> exceptions =
          Arrays.stream(method.getExceptionTypes()).collect(Collectors.toSet());
      assertTrue(exceptions.contains(WasmException.class), "Should throw WasmException");
      assertTrue(
          exceptions.contains(InterruptedException.class), "Should throw InterruptedException");
    }

    @Test
    @DisplayName("should have join method with timeout")
    void shouldHaveJoinWithTimeoutMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("join", long.class);
      assertNotNull(method, "join(long) method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have terminate method")
    void shouldHaveTerminateMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("terminate");
      assertNotNull(method, "terminate method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have forceTerminate method")
    void shouldHaveForceTerminateMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("forceTerminate");
      assertNotNull(method, "forceTerminate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      assertTrue(
          Arrays.asList(method.getExceptionTypes()).contains(WasmException.class),
          "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getSharedMemory method")
    void shouldHaveGetSharedMemoryMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("getSharedMemory");
      assertNotNull(method, "getSharedMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("should have getThreadLocalStorage method")
    void shouldHaveGetThreadLocalStorageMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("getThreadLocalStorage");
      assertNotNull(method, "getThreadLocalStorage method should exist");
      assertEquals(
          WasmThreadLocalStorage.class,
          method.getReturnType(),
          "Should return WasmThreadLocalStorage");
    }

    @Test
    @DisplayName("should have isAlive method")
    void shouldHaveIsAliveMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("isAlive");
      assertNotNull(method, "isAlive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isTerminationRequested method")
    void shouldHaveIsTerminationRequestedMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("isTerminationRequested");
      assertNotNull(method, "isTerminationRequested method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          WasmThreadStatistics.class, method.getReturnType(), "Should return WasmThreadStatistics");
    }

    @Test
    @DisplayName("should have close method from AutoCloseable")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasmThread.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have at least 12 abstract methods")
    void shouldHaveExpectedMethodCount() {
      long abstractMethodCount =
          Arrays.stream(WasmThread.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertTrue(abstractMethodCount >= 12, "Should have at least 12 abstract methods");
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
          WasmThread.class.getDeclaredClasses().length,
          "WasmThread should have no nested classes");
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
          WasmThread.class.getDeclaredFields().length,
          "WasmThread should have no declared fields");
    }
  }
}
