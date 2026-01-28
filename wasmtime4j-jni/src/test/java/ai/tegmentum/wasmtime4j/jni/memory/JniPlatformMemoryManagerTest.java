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

package ai.tegmentum.wasmtime4j.jni.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PlatformMemoryManager} class.
 *
 * <p>PlatformMemoryManager provides JNI-based platform memory management with features like huge
 * pages, NUMA awareness, compression, deduplication, and leak detection. These tests verify class
 * structure, method signatures, and API contracts without native library loading.
 */
@DisplayName("PlatformMemoryManager Tests")
class JniPlatformMemoryManagerTest {

  private static final Logger LOGGER =
      Logger.getLogger(JniPlatformMemoryManagerTest.class.getName());

  /**
   * Loads the class without triggering static initialization.
   *
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit() throws ClassNotFoundException {
    return Class.forName(
        "ai.tegmentum.wasmtime4j.jni.memory.PlatformMemoryManager",
        false,
        getClass().getClassLoader());
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      LOGGER.info("Testing PlatformMemoryManager class modifiers");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PlatformMemoryManager should be final");
      LOGGER.info("PlatformMemoryManager is correctly marked as final");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() throws ClassNotFoundException {
      LOGGER.info("Testing PlatformMemoryManager visibility");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "PlatformMemoryManager should be public");
      LOGGER.info("PlatformMemoryManager is correctly marked as public");
    }

    @Test
    @DisplayName("should implement AutoCloseable")
    void shouldImplementAutoCloseable() throws ClassNotFoundException {
      LOGGER.info("Testing AutoCloseable interface implementation");
      final Class<?> clazz = loadClassWithoutInit();
      assertTrue(
          AutoCloseable.class.isAssignableFrom(clazz),
          "PlatformMemoryManager should implement AutoCloseable");
      LOGGER.info("PlatformMemoryManager implements AutoCloseable");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have no-arg constructor")
    void shouldHaveNoArgConstructor() throws ClassNotFoundException {
      LOGGER.info("Testing no-arg constructor");
      final Class<?> clazz = loadClassWithoutInit();

      boolean hasNoArgConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (constructor.getParameterCount() == 0) {
          hasNoArgConstructor = true;
          assertTrue(
              Modifier.isPublic(constructor.getModifiers()), "No-arg constructor should be public");
          break;
        }
      }

      assertTrue(hasNoArgConstructor, "Should have no-arg constructor for default config");
      LOGGER.info("No-arg constructor verified");
    }

    @Test
    @DisplayName("should have constructor with Config parameter")
    void shouldHaveConfigConstructor() throws ClassNotFoundException {
      LOGGER.info("Testing Config constructor");
      final Class<?> clazz = loadClassWithoutInit();

      boolean hasConfigConstructor = false;
      for (final Constructor<?> constructor : clazz.getConstructors()) {
        final Class<?>[] params = constructor.getParameterTypes();
        if (params.length == 1 && params[0].getSimpleName().equals("Config")) {
          hasConfigConstructor = true;
          assertTrue(
              Modifier.isPublic(constructor.getModifiers()), "Config constructor should be public");
          break;
        }
      }

      assertTrue(hasConfigConstructor, "Should have constructor with Config parameter");
      LOGGER.info("Config constructor verified");
    }
  }

  @Nested
  @DisplayName("Allocation Method Tests")
  class AllocationMethodTests {

    @Test
    @DisplayName("should have allocate method returning long")
    void shouldHaveAllocateMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing allocate method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("allocate", long.class, int.class);

      assertNotNull(method, "allocate method should exist");
      assertEquals(long.class, method.getReturnType(), "allocate should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "allocate should be public");
      LOGGER.info("allocate method signature verified: " + method);
    }

    @Test
    @DisplayName("should have deallocate method returning void")
    void shouldHaveDeallocateMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing deallocate method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("deallocate", long.class);

      assertNotNull(method, "deallocate method should exist");
      assertEquals(void.class, method.getReturnType(), "deallocate should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "deallocate should be public");
      LOGGER.info("deallocate method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Statistics Method Tests")
  class StatisticsMethodTests {

    @Test
    @DisplayName("should have getStats method")
    void shouldHaveGetStatsMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getStats method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getStats");

      assertNotNull(method, "getStats method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getStats should be public");
      assertTrue(
          method.getReturnType().getSimpleName().equals("MemoryStats"),
          "getStats should return MemoryStats");
      LOGGER.info("getStats method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Platform Info Method Tests")
  class PlatformInfoMethodTests {

    @Test
    @DisplayName("should have getPlatformInfo method")
    void shouldHaveGetPlatformInfoMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing getPlatformInfo method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("getPlatformInfo");

      assertNotNull(method, "getPlatformInfo method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getPlatformInfo should be public");
      assertTrue(
          method.getReturnType().getSimpleName().equals("PlatformInfo"),
          "getPlatformInfo should return PlatformInfo");
      LOGGER.info("getPlatformInfo method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Leak Detection Method Tests")
  class LeakDetectionMethodTests {

    @Test
    @DisplayName("should have detectLeaks method returning array")
    void shouldHaveDetectLeaksMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing detectLeaks method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("detectLeaks");

      assertNotNull(method, "detectLeaks method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "detectLeaks should be public");
      assertTrue(method.getReturnType().isArray(), "detectLeaks should return array");
      LOGGER.info("detectLeaks method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Lifecycle Method Tests")
  class LifecycleMethodTests {

    @Test
    @DisplayName("should have isClosed method returning boolean")
    void shouldHaveIsClosedMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing isClosed method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("isClosed");

      assertNotNull(method, "isClosed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isClosed should return boolean");
      assertTrue(Modifier.isPublic(method.getModifiers()), "isClosed should be public");
      LOGGER.info("isClosed method signature verified: " + method);
    }

    @Test
    @DisplayName("should have close method returning void")
    void shouldHaveCloseMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing close method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("close");

      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "close should be public");
      LOGGER.info("close method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Advanced Feature Method Tests")
  class AdvancedFeatureMethodTests {

    @Test
    @DisplayName("should have prefetchMemory method returning void")
    void shouldHavePrefetchMemoryMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing prefetchMemory method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("prefetchMemory", long.class, long.class);

      assertNotNull(method, "prefetchMemory method should exist");
      assertEquals(void.class, method.getReturnType(), "prefetchMemory should return void");
      assertTrue(Modifier.isPublic(method.getModifiers()), "prefetchMemory should be public");
      LOGGER.info("prefetchMemory method signature verified: " + method);
    }

    @Test
    @DisplayName("should have compressMemory method returning byte array")
    void shouldHaveCompressMemoryMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing compressMemory method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("compressMemory", byte[].class);

      assertNotNull(method, "compressMemory method should exist");
      assertEquals(byte[].class, method.getReturnType(), "compressMemory should return byte[]");
      assertTrue(Modifier.isPublic(method.getModifiers()), "compressMemory should be public");
      LOGGER.info("compressMemory method signature verified: " + method);
    }

    @Test
    @DisplayName("should have deduplicateMemory method returning long")
    void shouldHaveDeduplicateMemoryMethod() throws ClassNotFoundException, NoSuchMethodException {
      LOGGER.info("Testing deduplicateMemory method signature");
      final Class<?> clazz = loadClassWithoutInit();
      final Method method = clazz.getMethod("deduplicateMemory", byte[].class);

      assertNotNull(method, "deduplicateMemory method should exist");
      assertEquals(long.class, method.getReturnType(), "deduplicateMemory should return long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "deduplicateMemory should be public");
      LOGGER.info("deduplicateMemory method signature verified: " + method);
    }
  }

  @Nested
  @DisplayName("Inner Type Tests")
  class InnerTypeTests {

    @Test
    @DisplayName("should have Config inner class")
    void shouldHaveConfigInnerClass() throws ClassNotFoundException {
      LOGGER.info("Testing Config inner class");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundConfig = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("Config")) {
          foundConfig = true;
          assertTrue(Modifier.isStatic(innerClass.getModifiers()), "Config should be static");
          assertTrue(Modifier.isFinal(innerClass.getModifiers()), "Config should be final");
          assertTrue(Modifier.isPublic(innerClass.getModifiers()), "Config should be public");
          LOGGER.info("Found Config class: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundConfig, "Should have Config inner class");
    }

    @Test
    @DisplayName("should have MemoryStats inner class")
    void shouldHaveMemoryStatsInnerClass() throws ClassNotFoundException {
      LOGGER.info("Testing MemoryStats inner class");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundMemoryStats = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("MemoryStats")) {
          foundMemoryStats = true;
          assertTrue(Modifier.isStatic(innerClass.getModifiers()), "MemoryStats should be static");
          assertTrue(Modifier.isFinal(innerClass.getModifiers()), "MemoryStats should be final");
          assertTrue(Modifier.isPublic(innerClass.getModifiers()), "MemoryStats should be public");
          LOGGER.info("Found MemoryStats class: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundMemoryStats, "Should have MemoryStats inner class");
    }

    @Test
    @DisplayName("should have PlatformInfo inner class")
    void shouldHavePlatformInfoInnerClass() throws ClassNotFoundException {
      LOGGER.info("Testing PlatformInfo inner class");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundPlatformInfo = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("PlatformInfo")) {
          foundPlatformInfo = true;
          assertTrue(Modifier.isStatic(innerClass.getModifiers()), "PlatformInfo should be static");
          assertTrue(Modifier.isFinal(innerClass.getModifiers()), "PlatformInfo should be final");
          assertTrue(Modifier.isPublic(innerClass.getModifiers()), "PlatformInfo should be public");
          LOGGER.info("Found PlatformInfo class: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundPlatformInfo, "Should have PlatformInfo inner class");
    }

    @Test
    @DisplayName("should have AllocationInfo inner class")
    void shouldHaveAllocationInfoInnerClass() throws ClassNotFoundException {
      LOGGER.info("Testing AllocationInfo inner class");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundAllocationInfo = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("AllocationInfo")) {
          foundAllocationInfo = true;
          assertTrue(
              Modifier.isStatic(innerClass.getModifiers()), "AllocationInfo should be static");
          assertTrue(Modifier.isFinal(innerClass.getModifiers()), "AllocationInfo should be final");
          assertTrue(
              Modifier.isPublic(innerClass.getModifiers()), "AllocationInfo should be public");
          LOGGER.info("Found AllocationInfo class: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundAllocationInfo, "Should have AllocationInfo inner class");
    }

    @Test
    @DisplayName("should have MemoryLeak inner class")
    void shouldHaveMemoryLeakInnerClass() throws ClassNotFoundException {
      LOGGER.info("Testing MemoryLeak inner class");
      final Class<?> clazz = loadClassWithoutInit();

      Class<?>[] declaredClasses = clazz.getDeclaredClasses();
      boolean foundMemoryLeak = false;

      for (final Class<?> innerClass : declaredClasses) {
        if (innerClass.getSimpleName().equals("MemoryLeak")) {
          foundMemoryLeak = true;
          assertTrue(Modifier.isStatic(innerClass.getModifiers()), "MemoryLeak should be static");
          assertTrue(Modifier.isFinal(innerClass.getModifiers()), "MemoryLeak should be final");
          assertTrue(Modifier.isPublic(innerClass.getModifiers()), "MemoryLeak should be public");
          LOGGER.info("Found MemoryLeak class: " + innerClass.getName());
          break;
        }
      }

      assertTrue(foundMemoryLeak, "Should have MemoryLeak inner class");
    }
  }

  @Nested
  @DisplayName("Private Field Tests")
  class PrivateFieldTests {

    @Test
    @DisplayName("should have LOGGER field")
    void shouldHaveLoggerField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing LOGGER field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("LOGGER");

      assertNotNull(field, "LOGGER field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "LOGGER should be private");
      assertTrue(Modifier.isStatic(field.getModifiers()), "LOGGER should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "LOGGER should be final");
      assertEquals(Logger.class, field.getType(), "LOGGER should be Logger type");
      LOGGER.info("LOGGER field verified");
    }

    @Test
    @DisplayName("should have nativeHandle field")
    void shouldHaveNativeHandleField() throws ClassNotFoundException, NoSuchFieldException {
      LOGGER.info("Testing nativeHandle field");
      final Class<?> clazz = loadClassWithoutInit();
      final Field field = clazz.getDeclaredField("nativeHandle");

      assertNotNull(field, "nativeHandle field should exist");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "nativeHandle should be private");
      assertEquals(long.class, field.getType(), "nativeHandle should be long type");
      LOGGER.info("nativeHandle field verified");
    }
  }

  @Nested
  @DisplayName("Method Count Verification Tests")
  class MethodCountVerificationTests {

    @Test
    @DisplayName("should have expected public methods")
    void shouldHaveExpectedPublicMethods() throws ClassNotFoundException {
      LOGGER.info("Testing public method count");
      final Class<?> clazz = loadClassWithoutInit();

      final Set<String> expectedMethods =
          new HashSet<>(
              Arrays.asList(
                  "allocate",
                  "deallocate",
                  "getStats",
                  "getPlatformInfo",
                  "detectLeaks",
                  "prefetchMemory",
                  "compressMemory",
                  "deduplicateMemory",
                  "isClosed",
                  "close"));

      int foundMethodCount = 0;
      for (final Method method : clazz.getMethods()) {
        if (method.getDeclaringClass() == clazz && Modifier.isPublic(method.getModifiers())) {
          foundMethodCount++;
          LOGGER.info("Found public method: " + method.getName());
        }
      }

      assertTrue(
          foundMethodCount >= expectedMethods.size(),
          "Should have at least "
              + expectedMethods.size()
              + " public methods, found: "
              + foundMethodCount);
      LOGGER.info("Public method count verified: " + foundMethodCount);
    }
  }
}
