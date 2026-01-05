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

package ai.tegmentum.wasmtime4j.panama.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaWasiThreadsContext} class.
 *
 * <p>PanamaWasiThreadsContext provides Panama FFI implementation of WASI-Threads support.
 */
@DisplayName("PanamaWasiThreadsContext Tests")
class PanamaWasiThreadsContextTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiThreadsContext.class.getModifiers()),
          "PanamaWasiThreadsContext should be public");
      assertTrue(
          Modifier.isFinal(PanamaWasiThreadsContext.class.getModifiers()),
          "PanamaWasiThreadsContext should be final");
    }

    @Test
    @DisplayName("should implement WasiThreadsContext interface")
    void shouldImplementWasiThreadsContextInterface() {
      assertTrue(
          WasiThreadsContext.class.isAssignableFrom(PanamaWasiThreadsContext.class),
          "PanamaWasiThreadsContext should implement WasiThreadsContext");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with MemorySegment, Arena, and boolean")
    void shouldHavePackagePrivateConstructor() throws NoSuchMethodException {
      final Constructor<?> constructor =
          PanamaWasiThreadsContext.class.getDeclaredConstructor(
              MemorySegment.class, Arena.class, boolean.class);
      assertNotNull(constructor, "Constructor with MemorySegment, Arena, boolean should exist");
      // Package-private constructor (not public)
      assertTrue(
          !Modifier.isPublic(constructor.getModifiers()), "Constructor should be package-private");
    }
  }

  @Nested
  @DisplayName("Thread Spawn Method Tests")
  class ThreadSpawnMethodTests {

    @Test
    @DisplayName("should have spawn method")
    void shouldHaveSpawnMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("spawn", int.class);
      assertNotNull(method, "spawn method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Thread Count Method Tests")
  class ThreadCountMethodTests {

    @Test
    @DisplayName("should have getThreadCount method")
    void shouldHaveGetThreadCountMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("getThreadCount");
      assertNotNull(method, "getThreadCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxThreadId method")
    void shouldHaveGetMaxThreadIdMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("getMaxThreadId");
      assertNotNull(method, "getMaxThreadId method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Status Method Tests")
  class StatusMethodTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Native Context Method Tests")
  class NativeContextMethodTests {

    @Test
    @DisplayName("should have getNativeContext method")
    void shouldHaveGetNativeContextMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("getNativeContext");
      assertNotNull(method, "getNativeContext method should exist");
      assertEquals(MemorySegment.class, method.getReturnType(), "Should return MemorySegment");
    }
  }

  @Nested
  @DisplayName("Thread Lifecycle Method Tests")
  class ThreadLifecycleMethodTests {

    @Test
    @DisplayName("should have package-private onThreadCompleted method")
    void shouldHaveOnThreadCompletedMethod() throws NoSuchMethodException {
      final Method method =
          PanamaWasiThreadsContext.class.getDeclaredMethod("onThreadCompleted", int.class);
      assertNotNull(method, "onThreadCompleted method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
      // Package-private method
      assertTrue(
          !Modifier.isPublic(method.getModifiers()), "onThreadCompleted should be package-private");
    }
  }

  @Nested
  @DisplayName("Close Method Tests")
  class CloseMethodTests {

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = PanamaWasiThreadsContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }
}
