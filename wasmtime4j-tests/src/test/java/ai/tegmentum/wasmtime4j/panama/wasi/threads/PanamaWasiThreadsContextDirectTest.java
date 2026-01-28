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

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wasi.threads.WasiThreadsContext;
import java.lang.foreign.MemorySegment;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Direct integration tests for PanamaWasiThreadsContext.
 *
 * <p>These tests verify class structure and method signatures without creating instances that would
 * invoke native calls.
 */
@DisplayName("Panama WASI Threads Context Direct Tests")
public class PanamaWasiThreadsContextDirectTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaWasiThreadsContextDirectTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI threads tests");
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class StructureTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaWasiThreadsContext class structure");

      final Class<?> clazz = PanamaWasiThreadsContext.class;

      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should implement WasiThreadsContext interface")
    void shouldImplementWasiThreadsContextInterface() {
      LOGGER.info("Testing WasiThreadsContext interface implementation");

      assertTrue(
          WasiThreadsContext.class.isAssignableFrom(PanamaWasiThreadsContext.class),
          "Should implement WasiThreadsContext");

      LOGGER.info("Interface implementation verified");
    }

    @Test
    @DisplayName("Should have package-private constructor")
    void shouldHavePackagePrivateConstructor() throws Exception {
      LOGGER.info("Testing constructor visibility");

      final Constructor<?>[] constructors =
          PanamaWasiThreadsContext.class.getDeclaredConstructors();

      boolean foundConstructor = false;
      for (final Constructor<?> constructor : constructors) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 3
            && params[0].equals(MemorySegment.class)
            && params[1].getSimpleName().equals("Arena")
            && params[2].equals(boolean.class)) {
          foundConstructor = true;
          // Constructor should be package-private (not public)
          final int modifiers = constructor.getModifiers();
          assertTrue(
              !java.lang.reflect.Modifier.isPublic(modifiers), "Constructor should not be public");
          LOGGER.info("Found expected constructor");
          break;
        }
      }
      assertTrue(foundConstructor, "Should have expected constructor");
    }
  }

  @Nested
  @DisplayName("Interface Method Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("Should have spawn method")
    void shouldHaveSpawnMethod() {
      LOGGER.info("Testing spawn method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("spawn")) {
          found = true;
          assertEquals(int.class, method.getReturnType(), "spawn should return int thread ID");
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(1, params.length, "spawn should have 1 parameter");
          assertEquals(int.class, params[0], "Parameter should be int threadStartArg");
          LOGGER.info("Found spawn method");
          break;
        }
      }
      assertTrue(found, "Should have spawn method");
    }

    @Test
    @DisplayName("Should have getThreadCount method")
    void shouldHaveGetThreadCountMethod() {
      LOGGER.info("Testing getThreadCount method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("getThreadCount") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(int.class, method.getReturnType(), "getThreadCount should return int");
          LOGGER.info("Found getThreadCount method");
          break;
        }
      }
      assertTrue(found, "Should have getThreadCount method");
    }

    @Test
    @DisplayName("Should have isEnabled method")
    void shouldHaveIsEnabledMethod() {
      LOGGER.info("Testing isEnabled method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("isEnabled") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(boolean.class, method.getReturnType(), "isEnabled should return boolean");
          LOGGER.info("Found isEnabled method");
          break;
        }
      }
      assertTrue(found, "Should have isEnabled method");
    }

    @Test
    @DisplayName("Should have getMaxThreadId method")
    void shouldHaveGetMaxThreadIdMethod() {
      LOGGER.info("Testing getMaxThreadId method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("getMaxThreadId") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(int.class, method.getReturnType(), "getMaxThreadId should return int");
          LOGGER.info("Found getMaxThreadId method");
          break;
        }
      }
      assertTrue(found, "Should have getMaxThreadId method");
    }

    @Test
    @DisplayName("Should have isValid method")
    void shouldHaveIsValidMethod() {
      LOGGER.info("Testing isValid method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("isValid") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
          LOGGER.info("Found isValid method");
          break;
        }
      }
      assertTrue(found, "Should have isValid method");
    }

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() {
      LOGGER.info("Testing close method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("close") && method.getParameterCount() == 0) {
          found = true;
          LOGGER.info("Found close method");
          break;
        }
      }
      assertTrue(found, "Should have close method");
    }
  }

  @Nested
  @DisplayName("Panama-Specific Method Tests")
  class PanamaSpecificMethodTests {

    @Test
    @DisplayName("Should have getNativeContext method")
    void shouldHaveGetNativeContextMethod() {
      LOGGER.info("Testing getNativeContext method");

      boolean found = false;
      for (final Method method : PanamaWasiThreadsContext.class.getMethods()) {
        if (method.getName().equals("getNativeContext") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(
              MemorySegment.class,
              method.getReturnType(),
              "getNativeContext should return MemorySegment");
          LOGGER.info("Found getNativeContext method");
          break;
        }
      }
      assertTrue(found, "Should have getNativeContext method");
    }

    @Test
    @DisplayName("Should have onThreadCompleted package method")
    void shouldHaveOnThreadCompletedMethod() throws Exception {
      LOGGER.info("Testing onThreadCompleted method");

      final Method method =
          PanamaWasiThreadsContext.class.getDeclaredMethod("onThreadCompleted", int.class);
      assertNotNull(method, "Should have onThreadCompleted method");
      assertEquals(void.class, method.getReturnType(), "onThreadCompleted should return void");

      // Should be package-private (not public)
      final int modifiers = method.getModifiers();
      assertTrue(
          !java.lang.reflect.Modifier.isPublic(modifiers),
          "onThreadCompleted should not be public");

      LOGGER.info("Found onThreadCompleted method");
    }
  }

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("Should have nativeContext field")
    void shouldHaveNativeContextField() throws Exception {
      LOGGER.info("Testing nativeContext field");

      final Field field = PanamaWasiThreadsContext.class.getDeclaredField("nativeContext");
      assertNotNull(field, "Should have nativeContext field");
      assertEquals(MemorySegment.class, field.getType(), "nativeContext should be MemorySegment");

      LOGGER.info("Found nativeContext field");
    }

    @Test
    @DisplayName("Should have enabled field")
    void shouldHaveEnabledField() throws Exception {
      LOGGER.info("Testing enabled field");

      final Field field = PanamaWasiThreadsContext.class.getDeclaredField("enabled");
      assertNotNull(field, "Should have enabled field");
      assertEquals(boolean.class, field.getType(), "enabled should be boolean");

      LOGGER.info("Found enabled field");
    }

    @Test
    @DisplayName("Should have thread tracking fields")
    void shouldHaveThreadTrackingFields() throws Exception {
      LOGGER.info("Testing thread tracking fields");

      final Field maxThreadId = PanamaWasiThreadsContext.class.getDeclaredField("maxThreadId");
      assertNotNull(maxThreadId, "Should have maxThreadId field");

      final Field threadCount = PanamaWasiThreadsContext.class.getDeclaredField("threadCount");
      assertNotNull(threadCount, "Should have threadCount field");

      LOGGER.info("Found thread tracking fields");
    }

    @Test
    @DisplayName("Should have closed flag field")
    void shouldHaveClosedFlagField() throws Exception {
      LOGGER.info("Testing closed field");

      final Field field = PanamaWasiThreadsContext.class.getDeclaredField("closed");
      assertNotNull(field, "Should have closed field");

      LOGGER.info("Found closed field");
    }
  }
}
