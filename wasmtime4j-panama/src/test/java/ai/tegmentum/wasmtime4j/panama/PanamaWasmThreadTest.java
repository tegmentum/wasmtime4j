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

package ai.tegmentum.wasmtime4j.panama;

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
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasmThread} class.
 *
 * <p>PanamaWasmThread provides WebAssembly threading capabilities using Panama FFI.
 */
@DisplayName("PanamaWasmThread Tests")
class PanamaWasmThreadTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasmThread.class.getModifiers()),
          "PanamaWasmThread should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasmThread.class.getModifiers()),
          "PanamaWasmThread should be final");
    }

    @Test
    @DisplayName("should implement WasmThread interface")
    void shouldImplementWasmThreadInterface() {
      assertTrue(
          WasmThread.class.isAssignableFrom(PanamaWasmThread.class),
          "PanamaWasmThread should implement WasmThread");
    }
  }

  @Nested
  @DisplayName("Thread Identity Method Tests")
  class ThreadIdentityMethodTests {

    @Test
    @DisplayName("should have getThreadId method")
    void shouldHaveGetThreadIdMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getThreadId");
      assertNotNull(method, "getThreadId method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getState method")
    void shouldHaveGetStateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getState");
      assertNotNull(method, "getState method should exist");
      assertEquals(WasmThreadState.class, method.getReturnType(), "Should return WasmThreadState");
    }
  }

  @Nested
  @DisplayName("Execution Method Tests")
  class ExecutionMethodTests {

    @Test
    @DisplayName("should have executeFunction method")
    void shouldHaveExecuteFunctionMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasmThread.class.getMethod(
              "executeFunction", WasmFunction.class, WasmValue[].class);
      assertNotNull(method, "executeFunction method should exist");
      assertEquals(Future.class, method.getReturnType(), "Should return Future");
    }

    @Test
    @DisplayName("should have executeOperation method")
    void shouldHaveExecuteOperationMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("executeOperation", Supplier.class);
      assertNotNull(method, "executeOperation method should exist");
      assertEquals(Future.class, method.getReturnType(), "Should return Future");
    }
  }

  @Nested
  @DisplayName("Synchronization Method Tests")
  class SynchronizationMethodTests {

    @Test
    @DisplayName("should have join method")
    void shouldHaveJoinMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("join");
      assertNotNull(method, "join method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have join method with timeout")
    void shouldHaveJoinMethodWithTimeout() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("join", long.class);
      assertNotNull(method, "join method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have terminate method")
    void shouldHaveTerminateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("terminate");
      assertNotNull(method, "terminate method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }

    @Test
    @DisplayName("should have forceTerminate method")
    void shouldHaveForceTerminateMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("forceTerminate");
      assertNotNull(method, "forceTerminate method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Memory Method Tests")
  class MemoryMethodTests {

    @Test
    @DisplayName("should have getSharedMemory method")
    void shouldHaveGetSharedMemoryMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getSharedMemory");
      assertNotNull(method, "getSharedMemory method should exist");
      assertEquals(WasmMemory.class, method.getReturnType(), "Should return WasmMemory");
    }

    @Test
    @DisplayName("should have getThreadLocalStorage method")
    void shouldHaveGetThreadLocalStorageMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getThreadLocalStorage");
      assertNotNull(method, "getThreadLocalStorage method should exist");
      assertEquals(
          WasmThreadLocalStorage.class,
          method.getReturnType(),
          "Should return WasmThreadLocalStorage");
    }
  }

  @Nested
  @DisplayName("Status Method Tests")
  class StatusMethodTests {

    @Test
    @DisplayName("should have isAlive method")
    void shouldHaveIsAliveMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("isAlive");
      assertNotNull(method, "isAlive method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isTerminationRequested method")
    void shouldHaveIsTerminationRequestedMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("isTerminationRequested");
      assertNotNull(method, "isTerminationRequested method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          WasmThreadStatistics.class, method.getReturnType(), "Should return WasmThreadStatistics");
    }
  }

  @Nested
  @DisplayName("Internal Method Tests")
  class InternalMethodTests {

    @Test
    @DisplayName("should have getNativeHandle method")
    void shouldHaveGetNativeHandleMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getNativeHandle");
      assertNotNull(method, "getNativeHandle method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }

    @Test
    @DisplayName("should have getArena method")
    void shouldHaveGetArenaMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("getArena");
      assertNotNull(method, "getArena method should exist");
      assertEquals(Arena.class, method.getReturnType(), "Should return Arena");
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasmThread.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 4 parameters")
    void shouldHavePublicConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaWasmThread.class.getConstructors()) {
        if (constructor.getParameterCount() == 4
            && constructor.getParameterTypes()[0] == MemorySegment.class
            && constructor.getParameterTypes()[1] == long.class
            && constructor.getParameterTypes()[2] == WasmMemory.class
            && constructor.getParameterTypes()[3] == Arena.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(
          hasExpectedConstructor,
          "Should have constructor with MemorySegment, long, WasmMemory, Arena");
    }
  }
}
