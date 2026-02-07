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

package ai.tegmentum.wasmtime4j.panama.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.NativeLibraryLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Direct integration tests for PlatformMemoryManager.
 *
 * <p>These tests verify class structure and method signatures without creating instances that would
 * invoke native calls.
 */
@DisplayName("Platform Memory Manager Direct Tests")
public class PlatformMemoryManagerDirectTest {

  private static final Logger LOGGER =
      Logger.getLogger(PlatformMemoryManagerDirectTest.class.getName());

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for memory manager tests");
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    assertTrue(loader.isLoaded(), "Native library should be loaded");
  }

  @Nested
  @DisplayName("PlatformMemoryManager Structure Tests")
  class StructureTests {

    @Test
    @DisplayName("Should have correct class structure")
    void shouldHaveCorrectClassStructure() {
      LOGGER.info("Testing PlatformMemoryManager class structure");

      final Class<?> clazz = PlatformMemoryManager.class;

      assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(
          java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), "Class should be public");
      assertTrue(AutoCloseable.class.isAssignableFrom(clazz), "Should implement AutoCloseable");

      LOGGER.info("Class structure verified");
    }

    @Test
    @DisplayName("Should have required constructors")
    void shouldHaveRequiredConstructors() {
      LOGGER.info("Testing constructors");

      final Class<?> clazz = PlatformMemoryManager.class;
      final Constructor<?>[] constructors = clazz.getConstructors();

      assertTrue(constructors.length >= 1, "Should have at least 1 public constructor");

      boolean hasDefaultConstructor = false;
      boolean hasConfigConstructor = false;

      for (final Constructor<?> constructor : constructors) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 0) {
          hasDefaultConstructor = true;
          LOGGER.info("Found default constructor");
        } else if (params.length == 1 && params[0].getSimpleName().equals("Config")) {
          hasConfigConstructor = true;
          LOGGER.info("Found Config constructor");
        }
      }

      assertTrue(hasDefaultConstructor, "Should have default constructor");
      assertTrue(hasConfigConstructor, "Should have Config constructor");
    }

    @Test
    @DisplayName("Should have required method handles as static fields")
    void shouldHaveRequiredMethodHandles() throws Exception {
      LOGGER.info("Testing static method handle fields");

      final Class<?> clazz = PlatformMemoryManager.class;
      final String[] expectedHandles = {
        "CREATE_ALLOCATOR",
        "ALLOCATE_MEMORY",
        "DEALLOCATE_MEMORY",
        "GET_STATS",
        "GET_PLATFORM_INFO",
        "DETECT_LEAKS",
        "PREFETCH_MEMORY",
        "COMPRESS_MEMORY",
        "DEDUPLICATE_MEMORY",
        "DESTROY_ALLOCATOR"
      };

      for (final String handleName : expectedHandles) {
        final Field field = clazz.getDeclaredField(handleName);
        field.setAccessible(true);
        assertNotNull(field, "Should have " + handleName + " field");
        assertTrue(
            java.lang.reflect.Modifier.isStatic(field.getModifiers()),
            handleName + " should be static");
        LOGGER.info("Verified " + handleName + " exists");
      }
    }
  }

  @Nested
  @DisplayName("Method Signature Tests")
  class MethodSignatureTests {

    @Test
    @DisplayName("Should have allocate method")
    void shouldHaveAllocateMethod() {
      LOGGER.info("Testing allocate method");

      final Class<?> clazz = PlatformMemoryManager.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("allocate")) {
          found = true;
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(2, params.length, "allocate should have 2 parameters");
          assertEquals(long.class, params[0], "First param should be long size");
          assertEquals(int.class, params[1], "Second param should be int alignment");
          LOGGER.info("Found allocate method");
          break;
        }
      }
      assertTrue(found, "Should have allocate method");
    }

    @Test
    @DisplayName("Should have deallocate method")
    void shouldHaveDeallocateMethod() {
      LOGGER.info("Testing deallocate method");

      final Class<?> clazz = PlatformMemoryManager.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("deallocate")) {
          found = true;
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(1, params.length, "deallocate should have 1 parameter");
          LOGGER.info("Found deallocate method");
          break;
        }
      }
      assertTrue(found, "Should have deallocate method");
    }

    @Test
    @DisplayName("Should have getStats method")
    void shouldHaveGetStatsMethod() {
      LOGGER.info("Testing getStats method");

      final Class<?> clazz = PlatformMemoryManager.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getStats") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(
              "MemoryStats",
              method.getReturnType().getSimpleName(),
              "getStats should return MemoryStats");
          LOGGER.info("Found getStats method");
          break;
        }
      }
      assertTrue(found, "Should have getStats method");
    }

    @Test
    @DisplayName("Should have getPlatformInfo method")
    void shouldHaveGetPlatformInfoMethod() {
      LOGGER.info("Testing getPlatformInfo method");

      final Class<?> clazz = PlatformMemoryManager.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("getPlatformInfo") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(
              "PlatformInfo",
              method.getReturnType().getSimpleName(),
              "getPlatformInfo should return PlatformInfo");
          LOGGER.info("Found getPlatformInfo method");
          break;
        }
      }
      assertTrue(found, "Should have getPlatformInfo method");
    }

    @Test
    @DisplayName("Should have prefetchMemory method")
    void shouldHavePrefetchMemoryMethod() {
      LOGGER.info("Testing prefetchMemory method");

      final Class<?> clazz = PlatformMemoryManager.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("prefetchMemory")) {
          found = true;
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(2, params.length, "prefetchMemory should have 2 parameters");
          LOGGER.info("Found prefetchMemory method");
          break;
        }
      }
      assertTrue(found, "Should have prefetchMemory method");
    }

    @Test
    @DisplayName("Should have compressMemory method")
    void shouldHaveCompressMemoryMethod() {
      LOGGER.info("Testing compressMemory method");

      final Class<?> clazz = PlatformMemoryManager.class;

      boolean found = false;
      for (final Method method : clazz.getMethods()) {
        if (method.getName().equals("compressMemory")) {
          found = true;
          final Class<?>[] params = method.getParameterTypes();
          assertEquals(1, params.length, "compressMemory should have 1 parameter");
          assertEquals(byte[].class, params[0], "Should take byte array");
          assertEquals(byte[].class, method.getReturnType(), "Should return byte array");
          LOGGER.info("Found compressMemory method");
          break;
        }
      }
      assertTrue(found, "Should have compressMemory method");
    }
  }

  @Nested
  @DisplayName("Nested Class Tests")
  class NestedClassTests {

    @Test
    @DisplayName("Should have Config nested class")
    void shouldHaveConfigNestedClass() {
      LOGGER.info("Testing Config nested class");

      final Class<?>[] declaredClasses = PlatformMemoryManager.class.getDeclaredClasses();

      boolean foundConfig = false;
      for (final Class<?> nested : declaredClasses) {
        if (nested.getSimpleName().equals("Config")) {
          foundConfig = true;
          assertTrue(
              java.lang.reflect.Modifier.isPublic(nested.getModifiers()),
              "Config should be public");
          assertTrue(
              java.lang.reflect.Modifier.isStatic(nested.getModifiers()),
              "Config should be static");
          LOGGER.info("Found Config nested class");
          break;
        }
      }
      assertTrue(foundConfig, "Should have Config nested class");
    }

    @Test
    @DisplayName("Should have MemoryStats nested class")
    void shouldHaveMemoryStatsNestedClass() {
      LOGGER.info("Testing MemoryStats nested class");

      final Class<?>[] declaredClasses = PlatformMemoryManager.class.getDeclaredClasses();

      boolean foundStats = false;
      for (final Class<?> nested : declaredClasses) {
        if (nested.getSimpleName().equals("MemoryStats")) {
          foundStats = true;
          assertTrue(
              java.lang.reflect.Modifier.isPublic(nested.getModifiers()),
              "MemoryStats should be public");
          LOGGER.info("Found MemoryStats nested class");
          break;
        }
      }
      assertTrue(foundStats, "Should have MemoryStats nested class");
    }

    @Test
    @DisplayName("Should have PlatformInfo nested class")
    void shouldHavePlatformInfoNestedClass() {
      LOGGER.info("Testing PlatformInfo nested class");

      final Class<?>[] declaredClasses = PlatformMemoryManager.class.getDeclaredClasses();

      boolean foundInfo = false;
      for (final Class<?> nested : declaredClasses) {
        if (nested.getSimpleName().equals("PlatformInfo")) {
          foundInfo = true;
          assertTrue(
              java.lang.reflect.Modifier.isPublic(nested.getModifiers()),
              "PlatformInfo should be public");
          LOGGER.info("Found PlatformInfo nested class");
          break;
        }
      }
      assertTrue(foundInfo, "Should have PlatformInfo nested class");
    }
  }

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("Should implement AutoCloseable")
    void shouldImplementAutoCloseable() {
      LOGGER.info("Testing AutoCloseable implementation");

      assertTrue(
          AutoCloseable.class.isAssignableFrom(PlatformMemoryManager.class),
          "Should implement AutoCloseable");

      // Verify close method exists
      boolean hasClose = false;
      for (final Method method : PlatformMemoryManager.class.getMethods()) {
        if (method.getName().equals("close") && method.getParameterCount() == 0) {
          hasClose = true;
          break;
        }
      }
      assertTrue(hasClose, "Should have close() method");
      LOGGER.info("AutoCloseable compliance verified");
    }

    @Test
    @DisplayName("Should have isClosed method")
    void shouldHaveIsClosedMethod() {
      LOGGER.info("Testing isClosed method");

      boolean found = false;
      for (final Method method : PlatformMemoryManager.class.getMethods()) {
        if (method.getName().equals("isClosed") && method.getParameterCount() == 0) {
          found = true;
          assertEquals(boolean.class, method.getReturnType(), "isClosed should return boolean");
          break;
        }
      }
      assertTrue(found, "Should have isClosed method");
      LOGGER.info("isClosed method verified");
    }
  }
}
