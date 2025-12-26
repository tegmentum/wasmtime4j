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

package ai.tegmentum.wasmtime4j.jni.performance;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Comprehensive package-level tests for the JNI performance package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * performance-related classes in the jni.performance package using reflection-based testing to
 * avoid native library loading.
 *
 * @since 1.0.0
 */
@DisplayName("JNI Performance Package Tests")
public class JniPerformancePackageTest {

  private static final Logger LOGGER = Logger.getLogger(JniPerformancePackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.jni.performance.";

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
  @DisplayName("PerformanceMonitor Tests")
  class PerformanceMonitorTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PerformanceMonitor");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PerformanceMonitor should be final");
    }

    @Test
    @DisplayName("Should have static monitoring fields")
    void shouldHaveStaticMonitoringFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PerformanceMonitor");
      boolean hasEnabled = false;
      boolean hasProfilingEnabled = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("enabled".equals(fieldName) && Modifier.isStatic(field.getModifiers())) {
          hasEnabled = true;
        }
        if ("profilingEnabled".equals(fieldName) && Modifier.isStatic(field.getModifiers())) {
          hasProfilingEnabled = true;
        }
      }

      assertTrue(hasEnabled, "Should have static enabled field");
      assertTrue(hasProfilingEnabled, "Should have static profilingEnabled field");
    }

    @Test
    @DisplayName("Should have static utility methods")
    void shouldHaveStaticUtilityMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PerformanceMonitor");
      boolean hasStartOperation = false;
      boolean hasEndOperation = false;
      boolean hasGetStatistics = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("startOperation".equals(methodName) && Modifier.isStatic(method.getModifiers())) {
          hasStartOperation = true;
        }
        if ("endOperation".equals(methodName) && Modifier.isStatic(method.getModifiers())) {
          hasEndOperation = true;
        }
        if ("getStatistics".equals(methodName) && Modifier.isStatic(method.getModifiers())) {
          hasGetStatistics = true;
        }
      }

      assertTrue(hasStartOperation, "Should have static startOperation method");
      assertTrue(hasEndOperation, "Should have static endOperation method");
      assertTrue(hasGetStatistics, "Should have static getStatistics method");
    }
  }

  @Nested
  @DisplayName("NativeCallOptimizer Tests")
  class NativeCallOptimizerTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "NativeCallOptimizer");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "NativeCallOptimizer should be final");
    }

    @Test
    @DisplayName("Should have optimization methods")
    void shouldHaveOptimizationMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "NativeCallOptimizer");
      boolean hasOptimize = false;
      boolean hasBatch = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if (methodName.contains("optimize") || methodName.contains("Optimize")) {
          hasOptimize = true;
        }
        if (methodName.contains("batch") || methodName.contains("Batch")) {
          hasBatch = true;
        }
      }

      assertTrue(hasOptimize || hasBatch, "Should have optimization or batching methods");
    }
  }

  @Nested
  @DisplayName("CompilationCache Tests")
  class CompilationCacheTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "CompilationCache");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "CompilationCache should be final");
    }

    @Test
    @DisplayName("Should have cache management methods")
    void shouldHaveCacheManagementMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "CompilationCache");
      boolean hasGet = false;
      boolean hasPut = false;
      boolean hasClear = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("get".equals(methodName) || methodName.startsWith("get")) {
          hasGet = true;
        }
        if ("put".equals(methodName) || methodName.startsWith("put")
            || methodName.contains("cache") || methodName.contains("Cache")) {
          hasPut = true;
        }
        if ("clear".equals(methodName) || "clearCache".equals(methodName)) {
          hasClear = true;
        }
      }

      assertTrue(hasGet || hasPut, "Should have cache get/put methods");
    }
  }

  @Nested
  @DisplayName("NativeObjectPool Tests")
  class NativeObjectPoolTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "NativeObjectPool");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "NativeObjectPool should be final");
    }

    @Test
    @DisplayName("Should have pool management methods")
    void shouldHavePoolManagementMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "NativeObjectPool");
      boolean hasAcquire = false;
      boolean hasRelease = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("acquire".equals(methodName) || methodName.contains("acquire")
            || methodName.contains("borrow") || methodName.contains("get")) {
          hasAcquire = true;
        }
        if ("release".equals(methodName) || methodName.contains("release")
            || methodName.contains("return") || methodName.contains("Return")) {
          hasRelease = true;
        }
      }

      assertTrue(hasAcquire || hasRelease, "Should have pool acquire/release methods");
    }
  }

  @Nested
  @DisplayName("JniOptimizationEngine Tests")
  class JniOptimizationEngineTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniOptimizationEngine");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "JniOptimizationEngine should be final");
    }

    @Test
    @DisplayName("Should have optimization methods")
    void shouldHaveOptimizationMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "JniOptimizationEngine");
      boolean hasOptimize = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if (method.getName().toLowerCase().contains("optim")) {
          hasOptimize = true;
          break;
        }
      }

      assertTrue(hasOptimize, "Should have optimization methods");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected performance classes should exist")
    void allExpectedPerformanceClassesShouldExist() {
      final String[] expectedClasses = {
        "PerformanceMonitor",
        "NativeCallOptimizer",
        "CompilationCache",
        "NativeObjectPool",
        "JniOptimizationEngine",
        "OptimizedMarshalling",
        "CallBatch"
      };

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found performance class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("All performance classes should be final")
    void allPerformanceClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] performanceClasses = {
        "PerformanceMonitor",
        "NativeCallOptimizer",
        "CompilationCache",
        "NativeObjectPool",
        "JniOptimizationEngine"
      };

      for (final String className : performanceClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All performance classes should not be interfaces")
    void allPerformanceClassesShouldNotBeInterfaces() throws ClassNotFoundException {
      final String[] performanceClasses = {
        "PerformanceMonitor",
        "NativeCallOptimizer",
        "CompilationCache",
        "NativeObjectPool",
        "JniOptimizationEngine"
      };

      for (final String className : performanceClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertFalse(clazz.isInterface(), className + " should not be an interface");
      }
    }
  }
}
