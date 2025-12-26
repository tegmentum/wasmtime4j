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

package ai.tegmentum.wasmtime4j.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmThread;
import ai.tegmentum.wasmtime4j.WasmThreadLocalStorage;
import ai.tegmentum.wasmtime4j.WasmThreadState;
import ai.tegmentum.wasmtime4j.WasmThreadStatistics;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasmThread} class.
 *
 * <p>JniWasmThread provides JNI implementation of WebAssembly thread for multi-threaded execution.
 */
@DisplayName("JniWasmThread Tests")
class JniWasmThreadTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public and final")
    void shouldBePublicAndFinal() {
      assertTrue(
          Modifier.isPublic(JniWasmThread.class.getModifiers()), "JniWasmThread should be public");
      assertTrue(
          Modifier.isFinal(JniWasmThread.class.getModifiers()), "JniWasmThread should be final");
    }

    @Test
    @DisplayName("should implement WasmThread interface")
    void shouldImplementWasmThreadInterface() {
      assertTrue(
          WasmThread.class.isAssignableFrom(JniWasmThread.class),
          "JniWasmThread should implement WasmThread");
    }

    @Test
    @DisplayName("should have constructor with native handle, thread ID, and shared memory")
    void shouldHaveConstructorWithAllParameters() throws NoSuchMethodException {
      final Constructor<?> constructor =
          JniWasmThread.class.getConstructor(long.class, long.class, WasmMemory.class);
      assertNotNull(constructor, "Constructor with all parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Thread Identity Method Tests")
  class ThreadIdentityMethodTests {

    @Test
    @DisplayName("should have getThreadId method")
    void shouldHaveGetThreadIdMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("getThreadId");
      assertNotNull(method, "getThreadId method should exist");
      assertEquals(long.class, method.getReturnType(), "getThreadId should return long");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(WasmThreadState.class, method.getReturnType(), "getState should return WasmThreadState");
    }

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(long.class, method.getReturnType(), "getNativeHandle should return long");
    }
  }

  @Nested
  @DisplayName("Execution Method Tests")
  class ExecutionMethodTests {

    @Test
    @DisplayName("should have executeFunction method")
    void shouldHaveExecuteFunctionMethod() throws NoSuchMethodException {
      final Method method =
          JniWasmThread.class.getMethod("executeFunction", WasmFunction.class, WasmValue[].class);
      assertNotNull(method, "executeFunction method should exist");
      assertEquals(Future.class, method.getReturnType(), "executeFunction should return Future");
    }

    @Test
    @DisplayName("should have executeOperation method")
    void shouldHaveExecuteOperationMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("executeOperation", Supplier.class);
      assertNotNull(method, "executeOperation method should exist");
      assertEquals(Future.class, method.getReturnType(), "executeOperation should return Future");
    }
  }

  @Nested
  @DisplayName("Synchronization Method Tests")
  class SynchronizationMethodTests {

    @Test
    @DisplayName("should have join method without timeout")
    void shouldHaveJoinMethodWithoutTimeout() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("join");
      assertNotNull(method, "join method should exist");
      assertEquals(void.class, method.getReturnType(), "join should return void");
    }

    @Test
    @DisplayName("should have join method with timeout")
    void shouldHaveJoinMethodWithTimeout() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("join", long.class);
      assertNotNull(method, "join method with timeout should exist");
      assertEquals(boolean.class, method.getReturnType(), "join with timeout should return boolean");
    }
  }

  @Nested
  @DisplayName("Termination Method Tests")
  class TerminationMethodTests {

    @Test
    @DisplayName("should have terminate method")
    void shouldHaveTerminateMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("terminate");
      assertNotNull(method, "terminate method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "terminate should return CompletableFuture");
    }

    @Test
    @DisplayName("should have forceTerminate method")
    void shouldHaveForceTerminateMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("forceTerminate");
      assertNotNull(method, "forceTerminate method should exist");
      assertEquals(void.class, method.getReturnType(), "forceTerminate should return void");
    }

    @Test
    @DisplayName("should have isTerminationRequested method")
    void shouldHaveIsTerminationRequestedMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("isTerminationRequested");
      assertNotNull(method, "isTerminationRequested method should exist");
      assertEquals(
          boolean.class, method.getReturnType(), "isTerminationRequested should return boolean");
    }
  }

  @Nested
  @DisplayName("State Query Method Tests")
  class StateQueryMethodTests {

    @Test
    @DisplayName("should have isAlive method")
    void shouldHaveIsAliveMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("isAlive");
      assertNotNull(method, "isAlive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isAlive should return boolean");
    }

    @Test
    @DisplayName("should have getSharedMemory method")
    void shouldHaveGetSharedMemoryMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("getSharedMemory");
      assertNotNull(method, "getSharedMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "getSharedMemory should return WasmMemory");
    }

    @Test
    @DisplayName("should have getThreadLocalStorage method")
    void shouldHaveGetThreadLocalStorageMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("getThreadLocalStorage");
      assertNotNull(method, "getThreadLocalStorage method should exist");
      assertEquals(
          WasmThreadLocalStorage.class,
          method.getReturnType(),
          "getThreadLocalStorage should return WasmThreadLocalStorage");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          WasmThreadStatistics.class,
          method.getReturnType(),
          "getStatistics should return WasmThreadStatistics");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = JniWasmThread.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }
}
