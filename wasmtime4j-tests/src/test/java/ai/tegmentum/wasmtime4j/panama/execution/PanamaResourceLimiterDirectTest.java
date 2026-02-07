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

package ai.tegmentum.wasmtime4j.panama.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.execution.ResourceLimiter;
import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Direct integration tests for PanamaResourceLimiter.
 *
 * <p>These tests verify class structure and method signatures without creating instances that would
 * invoke native calls.
 */
@DisplayName("Panama Resource Limiter Direct Tests")
public class PanamaResourceLimiterDirectTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaResourceLimiterDirectTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for resource limiter tests");
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class StructureTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PanamaResourceLimiter class structure");

      final Class<?> clazz = PanamaResourceLimiter.class;

      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should implement ResourceLimiter interface")
    void shouldImplementResourceLimiterInterface() {
      LOGGER.info("Testing ResourceLimiter interface implementation");

      assertTrue(
          ResourceLimiter.class.isAssignableFrom(PanamaResourceLimiter.class),
          "Should implement ResourceLimiter");

      LOGGER.info("Interface implementation verified");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("Should have create static factory method")
    void shouldHaveCreateStaticFactoryMethod() {
      LOGGER.info("Testing create factory method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("create")
            && java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          found = true;
          assertEquals(
              PanamaResourceLimiter.class,
              method.getReturnType(),
              "create should return PanamaResourceLimiter");
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(1, params.length, "create should have 1 parameter");
          assertEquals(
              "ResourceLimiterConfig",
              params[0].getSimpleName(),
              "Parameter should be ResourceLimiterConfig");
          LOGGER.info("Found create factory method");
          break;
        }
      }
      assertTrue(found, "Should have create factory method");
    }

    @Test
    @DisplayName("Should have createDefault static factory method")
    void shouldHaveCreateDefaultStaticFactoryMethod() {
      LOGGER.info("Testing createDefault factory method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("createDefault")
            && java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          found = true;
          assertEquals(
              PanamaResourceLimiter.class,
              method.getReturnType(),
              "createDefault should return PanamaResourceLimiter");
          assertEquals(0, method.getParameterCount(), "createDefault should have no parameters");
          LOGGER.info("Found createDefault factory method");
          break;
        }
      }
      assertTrue(found, "Should have createDefault factory method");
    }
  }

  @Nested
  @DisplayName("Interface Method Tests")
  class InterfaceMethodTests {

    @Test
    @DisplayName("Should have getId method")
    void shouldHaveGetIdMethod() {
      LOGGER.info("Testing getId method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("getId") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(long.class, method.getReturnType(), "getId should return long");
          LOGGER.info("Found getId method");
          break;
        }
      }
      assertTrue(found, "Should have getId method");
    }

    @Test
    @DisplayName("Should have getConfig method")
    void shouldHaveGetConfigMethod() {
      LOGGER.info("Testing getConfig method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("getConfig") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(
              "ResourceLimiterConfig",
              method.getReturnType().getSimpleName(),
              "getConfig should return ResourceLimiterConfig");
          LOGGER.info("Found getConfig method");
          break;
        }
      }
      assertTrue(found, "Should have getConfig method");
    }

    @Test
    @DisplayName("Should have allowMemoryGrow method")
    void shouldHaveAllowMemoryGrowMethod() {
      LOGGER.info("Testing allowMemoryGrow method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("allowMemoryGrow")) {
          found = true;
          assertEquals(
              boolean.class, method.getReturnType(), "allowMemoryGrow should return boolean");
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(2, params.length, "allowMemoryGrow should have 2 parameters");
          assertEquals(long.class, params[0], "First param should be long currentPages");
          assertEquals(long.class, params[1], "Second param should be long requestedPages");
          LOGGER.info("Found allowMemoryGrow method");
          break;
        }
      }
      assertTrue(found, "Should have allowMemoryGrow method");
    }

    @Test
    @DisplayName("Should have allowTableGrow method")
    void shouldHaveAllowTableGrowMethod() {
      LOGGER.info("Testing allowTableGrow method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("allowTableGrow")) {
          found = true;
          assertEquals(
              boolean.class, method.getReturnType(), "allowTableGrow should return boolean");
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(2, params.length, "allowTableGrow should have 2 parameters");
          LOGGER.info("Found allowTableGrow method");
          break;
        }
      }
      assertTrue(found, "Should have allowTableGrow method");
    }

    @Test
    @DisplayName("Should have getStats method")
    void shouldHaveGetStatsMethod() {
      LOGGER.info("Testing getStats method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("getStats") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(
              "ResourceLimiterStats",
              method.getReturnType().getSimpleName(),
              "getStats should return ResourceLimiterStats");
          LOGGER.info("Found getStats method");
          break;
        }
      }
      assertTrue(found, "Should have getStats method");
    }

    @Test
    @DisplayName("Should have resetStats method")
    void shouldHaveResetStatsMethod() {
      LOGGER.info("Testing resetStats method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("resetStats") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(void.class, method.getReturnType(), "resetStats should return void");
          LOGGER.info("Found resetStats method");
          break;
        }
      }
      assertTrue(found, "Should have resetStats method");
    }

    @Test
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() {
      LOGGER.info("Testing close method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
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
  @DisplayName("Static Utility Method Tests")
  class StaticUtilityMethodTests {

    @Test
    @DisplayName("Should have getLimiterCount static method")
    void shouldHaveGetLimiterCountStaticMethod() {
      LOGGER.info("Testing getLimiterCount method");

      boolean found = false;
      for (final Method method : PanamaResourceLimiter.class.getMethods()) {
        if (method.getName().equals("getLimiterCount")
            && java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
          found = true;
          assertEquals(int.class, method.getReturnType(), "getLimiterCount should return int");
          assertEquals(0, method.getParameterCount(), "getLimiterCount should have no parameters");
          LOGGER.info("Found getLimiterCount method");
          break;
        }
      }
      assertTrue(found, "Should have getLimiterCount static method");
    }
  }
}
