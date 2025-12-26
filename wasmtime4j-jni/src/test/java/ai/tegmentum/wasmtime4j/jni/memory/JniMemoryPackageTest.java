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
 * Comprehensive package-level tests for the JNI memory package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * memory-related classes in the jni.memory package using reflection-based testing to avoid native
 * library loading.
 *
 * @since 1.0.0
 */
@DisplayName("JNI Memory Package Tests")
public class JniMemoryPackageTest {

  private static final Logger LOGGER = Logger.getLogger(JniMemoryPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.jni.memory.";

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
    @DisplayName("Should have AllocationInfo inner class")
    void shouldHaveAllocationInfoClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasAllocationInfo = false;

      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("AllocationInfo".equals(inner.getSimpleName())) {
          assertTrue(
              Modifier.isPublic(inner.getModifiers())
                  && Modifier.isStatic(inner.getModifiers())
                  && Modifier.isFinal(inner.getModifiers()),
              "AllocationInfo should be public static final");
          hasAllocationInfo = true;
          break;
        }
      }

      assertTrue(hasAllocationInfo, "Should have AllocationInfo inner class");
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
    @DisplayName("Should have native handle field")
    void shouldHaveNativeHandleField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasNativeHandle = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("nativeHandle".equals(field.getName())) {
          hasNativeHandle = true;
          break;
        }
      }

      assertTrue(hasNativeHandle, "Should have nativeHandle field");
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
    @DisplayName("Should have close method")
    void shouldHaveCloseMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PlatformMemoryManager");
      boolean hasClose = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("close".equals(method.getName())) {
          hasClose = true;
          break;
        }
      }

      assertTrue(hasClose, "Should have close method");
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
    @DisplayName("All memory classes should be final")
    void allMemoryClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] memoryClasses = {"PlatformMemoryManager"};

      for (final String className : memoryClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All memory classes should implement AutoCloseable")
    void allMemoryClassesShouldImplementAutoCloseable() throws ClassNotFoundException {
      final String[] memoryClasses = {"PlatformMemoryManager"};

      for (final String className : memoryClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(
            AutoCloseable.class.isAssignableFrom(clazz),
            className + " should implement AutoCloseable");
      }
    }
  }
}
