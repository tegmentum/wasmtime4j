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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive package-level tests for the Panama memory package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * memory-related classes in the panama.memory package using reflection-based testing to avoid
 * native library loading.
 *
 * @since 1.0.0
 */
@DisplayName("Panama Memory Package Tests")
public class PanamaMemoryPackageTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaMemoryPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.panama.memory.";

  /**
   * Loads a class without triggering static initialization.
   *
   * @param className the fully qualified class name
   * @return the loaded class
   * @throws ClassNotFoundException if the class cannot be found
   */
  private Class<?> loadClassWithoutInit(final String className) throws ClassNotFoundException {
    return Class.forName(className, false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("PlatformMemoryManager Tests")
  class PlatformMemoryManagerTests {

    @Test
    @DisplayName("Should be a final class implementing AutoCloseable")
    void shouldBeFinalAndAutoCloseable() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PlatformMemoryManager should be final");
      assertTrue(
          AutoCloseable.class.isAssignableFrom(clazz),
          "PlatformMemoryManager should implement AutoCloseable");
    }

    @Test
    @DisplayName("Should have Config inner class")
    void shouldHaveConfigClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasConfig = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("Config".equals(inner.getSimpleName())) {
          assertTrue(
              Modifier.isPublic(inner.getModifiers())
                  && Modifier.isStatic(inner.getModifiers())
                  && Modifier.isFinal(inner.getModifiers()),
              "Config should be public static final");
          hasConfig = true;
          break;
        }
      }
      assertTrue(hasConfig, "Should have Config inner class");
    }

    @Test
    @DisplayName("Should have PlatformInfo inner class")
    void shouldHavePlatformInfoClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasPlatformInfo = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("PlatformInfo".equals(inner.getSimpleName())) {
          assertTrue(
              Modifier.isPublic(inner.getModifiers())
                  && Modifier.isStatic(inner.getModifiers())
                  && Modifier.isFinal(inner.getModifiers()),
              "PlatformInfo should be public static final");
          hasPlatformInfo = true;
          break;
        }
      }
      assertTrue(hasPlatformInfo, "Should have PlatformInfo inner class");
    }

    @Test
    @DisplayName("Should have MemoryStats inner class")
    void shouldHaveMemoryStatsClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasMemoryStats = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("MemoryStats".equals(inner.getSimpleName())) {
          assertTrue(
              Modifier.isPublic(inner.getModifiers())
                  && Modifier.isStatic(inner.getModifiers())
                  && Modifier.isFinal(inner.getModifiers()),
              "MemoryStats should be public static final");
          hasMemoryStats = true;
          break;
        }
      }
      assertTrue(hasMemoryStats, "Should have MemoryStats inner class");
    }

    @Test
    @DisplayName("Should have memory allocation methods")
    void shouldHaveAllocationMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasAllocate = false;
      boolean hasDeallocate = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("allocate".equals(method.getName())) {
          hasAllocate = true;
        }
        if ("deallocate".equals(method.getName())) {
          hasDeallocate = true;
        }
      }
      assertTrue(hasAllocate, "Should have allocate method");
      assertTrue(hasDeallocate, "Should have deallocate method");
    }

    @Test
    @DisplayName("Should have memory optimization methods")
    void shouldHaveOptimizationMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasPrefetch = false;
      boolean hasCompress = false;
      boolean hasDeduplicate = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("prefetchMemory".equals(method.getName())) {
          hasPrefetch = true;
        }
        if ("compressMemory".equals(method.getName())) {
          hasCompress = true;
        }
        if ("deduplicateMemory".equals(method.getName())) {
          hasDeduplicate = true;
        }
      }
      assertTrue(hasPrefetch, "Should have prefetchMemory method");
      assertTrue(hasCompress, "Should have compressMemory method");
      assertTrue(hasDeduplicate, "Should have deduplicateMemory method");
    }

    @Test
    @DisplayName("Should have info and stats methods")
    void shouldHaveInfoMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasGetStats = false;
      boolean hasGetPlatformInfo = false;
      boolean hasIsClosed = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getStats".equals(method.getName())) {
          hasGetStats = true;
        }
        if ("getPlatformInfo".equals(method.getName())) {
          hasGetPlatformInfo = true;
        }
        if ("isClosed".equals(method.getName())) {
          hasIsClosed = true;
        }
      }
      assertTrue(hasGetStats, "Should have getStats method");
      assertTrue(hasGetPlatformInfo, "Should have getPlatformInfo method");
      assertTrue(hasIsClosed, "Should have isClosed method");
    }

    @Test
    @DisplayName("Should have PageSize enum in Config")
    void shouldHavePageSizeEnum() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasPageSize = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("Config".equals(inner.getSimpleName())) {
          for (final Class<?> configInner : inner.getDeclaredClasses()) {
            if ("PageSize".equals(configInner.getSimpleName()) && configInner.isEnum()) {
              hasPageSize = true;
              // Verify enum has expected constants
              final Object[] constants = configInner.getEnumConstants();
              assertTrue(constants.length >= 3, "PageSize should have at least 3 values");
              break;
            }
          }
          break;
        }
      }
      assertTrue(hasPageSize, "Should have PageSize enum in Config");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected memory classes should exist")
    void allExpectedClassesShouldExist() {
      final String[] expectedClasses = {"PlatformMemoryManager"};

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found memory class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("Memory manager should have native memory layouts")
    void shouldHaveNativeMemoryLayouts() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");

      boolean hasPlatformConfigLayout = false;
      boolean hasMemoryStatsLayout = false;
      boolean hasPlatformInfoLayout = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("PLATFORM_CONFIG_LAYOUT".equals(fieldName)) {
          hasPlatformConfigLayout = true;
        }
        if ("MEMORY_STATS_LAYOUT".equals(fieldName)) {
          hasMemoryStatsLayout = true;
        }
        if ("PLATFORM_INFO_LAYOUT".equals(fieldName)) {
          hasPlatformInfoLayout = true;
        }
      }

      assertTrue(hasPlatformConfigLayout, "Should have PLATFORM_CONFIG_LAYOUT");
      assertTrue(hasMemoryStatsLayout, "Should have MEMORY_STATS_LAYOUT");
      assertTrue(hasPlatformInfoLayout, "Should have PLATFORM_INFO_LAYOUT");
    }
  }

  @Nested
  @DisplayName("Native Binding Tests")
  class NativeBindingTests {

    @Test
    @DisplayName("Should have native method handle fields")
    void shouldHaveMethodHandleFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");

      boolean hasCreateAllocator = false;
      boolean hasAllocateMemory = false;
      boolean hasDeallocateMemory = false;
      boolean hasDestroyAllocator = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("CREATE_ALLOCATOR".equals(fieldName)) {
          hasCreateAllocator = true;
        }
        if ("ALLOCATE_MEMORY".equals(fieldName)) {
          hasAllocateMemory = true;
        }
        if ("DEALLOCATE_MEMORY".equals(fieldName)) {
          hasDeallocateMemory = true;
        }
        if ("DESTROY_ALLOCATOR".equals(fieldName)) {
          hasDestroyAllocator = true;
        }
      }

      assertTrue(hasCreateAllocator, "Should have CREATE_ALLOCATOR method handle");
      assertTrue(hasAllocateMemory, "Should have ALLOCATE_MEMORY method handle");
      assertTrue(hasDeallocateMemory, "Should have DEALLOCATE_MEMORY method handle");
      assertTrue(hasDestroyAllocator, "Should have DESTROY_ALLOCATOR method handle");
    }

    @Test
    @DisplayName("Should have LINKER and LOOKUP fields")
    void shouldHaveLinkerAndLookupFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");

      boolean hasLinker = false;
      boolean hasLookup = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("LINKER".equals(field.getName())) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "LINKER should be static final");
          hasLinker = true;
        }
        if ("LOOKUP".equals(field.getName())) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "LOOKUP should be static final");
          hasLookup = true;
        }
      }

      assertTrue(hasLinker, "Should have LINKER field");
      assertTrue(hasLookup, "Should have LOOKUP field");
    }
  }
}
