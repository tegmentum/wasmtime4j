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

package ai.tegmentum.wasmtime4j.panama.performance;

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
 * Comprehensive package-level tests for the Panama performance package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * performance-related classes in the panama.performance package using reflection-based testing to
 * avoid native library loading.
 *
 * @since 1.0.0
 */
@DisplayName("Panama Performance Package Tests")
public class PanamaPerformancePackageTest {

  private static final Logger LOGGER =
      Logger.getLogger(PanamaPerformancePackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.panama.performance.";

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
  @DisplayName("PanaNativeObjectPool Tests")
  class PanaNativeObjectPoolTests {

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanaNativeObjectPool should be final");
    }

    @Test
    @DisplayName("Should have getPool static method")
    void shouldHaveGetPoolMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");
      boolean hasGetPool = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getPool".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasGetPool = true;
          break;
        }
      }
      assertTrue(hasGetPool, "PanaNativeObjectPool should have static getPool method");
    }

    @Test
    @DisplayName("Should have borrow and return methods")
    void shouldHaveBorrowReturnMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");
      boolean hasBorrow = false;
      boolean hasReturn = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("borrow".equals(method.getName())) {
          hasBorrow = true;
        }
        if ("returnObject".equals(method.getName())) {
          hasReturn = true;
        }
      }
      assertTrue(hasBorrow, "PanaNativeObjectPool should have borrow method");
      assertTrue(hasReturn, "PanaNativeObjectPool should have returnObject method");
    }

    @Test
    @DisplayName("Should have ArenaObjectFactory functional interface")
    void shouldHaveArenaObjectFactory() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");
      boolean hasFactory = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("ArenaObjectFactory".equals(inner.getSimpleName())) {
          assertTrue(inner.isInterface(), "ArenaObjectFactory should be an interface");
          hasFactory = true;
          break;
        }
      }
      assertTrue(hasFactory, "Should have ArenaObjectFactory inner interface");
    }

    @Test
    @DisplayName("Should have pool statistics methods")
    void shouldHaveStatsMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");
      boolean hasGetStats = false;
      boolean hasGetHitRate = false;
      boolean hasClearAllPools = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getStats".equals(method.getName())) {
          hasGetStats = true;
        }
        if ("getHitRate".equals(method.getName())) {
          hasGetHitRate = true;
        }
        if ("clearAllPools".equals(method.getName())) {
          hasClearAllPools = true;
        }
      }
      assertTrue(hasGetStats, "Should have getStats method");
      assertTrue(hasGetHitRate, "Should have getHitRate method");
      assertTrue(hasClearAllPools, "Should have static clearAllPools method");
    }
  }

  @Nested
  @DisplayName("AdvancedArenaManager Tests")
  class AdvancedArenaManagerTests {

    @Test
    @DisplayName("Should be a final class with singleton pattern")
    void shouldBeSingleton() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "AdvancedArenaManager");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "AdvancedArenaManager should be final");

      // Check for getInstance static method
      boolean hasGetInstance = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getInstance".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasGetInstance = true;
          break;
        }
      }
      assertTrue(hasGetInstance, "Should have static getInstance method for singleton");
    }

    @Test
    @DisplayName("Should have MemoryPressure enum")
    void shouldHaveMemoryPressureEnum() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "AdvancedArenaManager");
      boolean hasEnum = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("MemoryPressure".equals(inner.getSimpleName()) && inner.isEnum()) {
          hasEnum = true;
          // Verify enum constants
          final Object[] constants = inner.getEnumConstants();
          assertTrue(constants.length >= 4, "MemoryPressure should have at least 4 levels");
          break;
        }
      }
      assertTrue(hasEnum, "Should have MemoryPressure enum");
    }

    @Test
    @DisplayName("Should have AllocationStrategy enum")
    void shouldHaveAllocationStrategyEnum() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "AdvancedArenaManager");
      boolean hasEnum = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("AllocationStrategy".equals(inner.getSimpleName()) && inner.isEnum()) {
          hasEnum = true;
          break;
        }
      }
      assertTrue(hasEnum, "Should have AllocationStrategy enum");
    }

    @Test
    @DisplayName("Should have allocate optimization methods")
    void shouldHaveAllocateMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "AdvancedArenaManager");
      boolean hasAllocateOptimized = false;
      boolean hasExecuteZeroCopy = false;
      boolean hasAllocateBulkOptimized = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("allocateOptimized".equals(method.getName())) {
          hasAllocateOptimized = true;
        }
        if ("executeZeroCopy".equals(method.getName())) {
          hasExecuteZeroCopy = true;
        }
        if ("allocateBulkOptimized".equals(method.getName())) {
          hasAllocateBulkOptimized = true;
        }
      }
      assertTrue(hasAllocateOptimized, "Should have allocateOptimized method");
      assertTrue(hasExecuteZeroCopy, "Should have executeZeroCopy method");
      assertTrue(hasAllocateBulkOptimized, "Should have allocateBulkOptimized method");
    }

    @Test
    @DisplayName("Should have shutdown and statistics methods")
    void shouldHaveShutdownAndStatsMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "AdvancedArenaManager");
      boolean hasShutdown = false;
      boolean hasGetStatistics = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("shutdown".equals(method.getName())) {
          hasShutdown = true;
        }
        if ("getStatistics".equals(method.getName())) {
          hasGetStatistics = true;
        }
      }
      assertTrue(hasShutdown, "Should have shutdown method");
      assertTrue(hasGetStatistics, "Should have getStatistics method");
    }
  }

  @Nested
  @DisplayName("PanamaPerformanceMonitor Tests")
  class PanamaPerformanceMonitorTests {

    @Test
    @DisplayName("Should be a final utility class")
    void shouldBeFinalUtilityClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaPerformanceMonitor");
      assertTrue(
          Modifier.isFinal(clazz.getModifiers()), "PanamaPerformanceMonitor should be final");

      // All public methods should be static
      for (final Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          assertTrue(
              Modifier.isStatic(method.getModifiers()),
              "Public method " + method.getName() + " should be static");
        }
      }
    }

    @Test
    @DisplayName("Should have MonitoredOperation functional interface")
    void shouldHaveMonitoredOperation() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaPerformanceMonitor");
      boolean hasInterface = false;
      for (final Class<?> inner : clazz.getDeclaredClasses()) {
        if ("MonitoredOperation".equals(inner.getSimpleName()) && inner.isInterface()) {
          hasInterface = true;
          break;
        }
      }
      assertTrue(hasInterface, "Should have MonitoredOperation inner interface");
    }

    @Test
    @DisplayName("Should have operation timing methods")
    void shouldHaveTimingMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaPerformanceMonitor");
      boolean hasStartOperation = false;
      boolean hasEndOperation = false;
      boolean hasMonitor = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("startOperation".equals(method.getName())) {
          hasStartOperation = true;
        }
        if ("endOperation".equals(method.getName())) {
          hasEndOperation = true;
        }
        if ("monitor".equals(method.getName())) {
          hasMonitor = true;
        }
      }
      assertTrue(hasStartOperation, "Should have startOperation method");
      assertTrue(hasEndOperation, "Should have endOperation method");
      assertTrue(hasMonitor, "Should have monitor method");
    }

    @Test
    @DisplayName("Should have Panama-specific recording methods")
    void shouldHaveRecordingMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaPerformanceMonitor");
      boolean hasRecordArena = false;
      boolean hasRecordMemorySegment = false;
      boolean hasRecordMethodHandle = false;
      boolean hasRecordZeroCopy = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("recordArenaAllocation".equals(method.getName())) {
          hasRecordArena = true;
        }
        if ("recordMemorySegmentAllocation".equals(method.getName())) {
          hasRecordMemorySegment = true;
        }
        if ("recordMethodHandleCall".equals(method.getName())) {
          hasRecordMethodHandle = true;
        }
        if ("recordZeroCopyOperation".equals(method.getName())) {
          hasRecordZeroCopy = true;
        }
      }
      assertTrue(hasRecordArena, "Should have recordArenaAllocation method");
      assertTrue(hasRecordMemorySegment, "Should have recordMemorySegmentAllocation method");
      assertTrue(hasRecordMethodHandle, "Should have recordMethodHandleCall method");
      assertTrue(hasRecordZeroCopy, "Should have recordZeroCopyOperation method");
    }

    @Test
    @DisplayName("Should have statistics and control methods")
    void shouldHaveStatsMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaPerformanceMonitor");
      boolean hasGetStatistics = false;
      boolean hasGetPanamaMetrics = false;
      boolean hasReset = false;
      boolean hasSetEnabled = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getStatistics".equals(method.getName())) {
          hasGetStatistics = true;
        }
        if ("getPanamaMetrics".equals(method.getName())) {
          hasGetPanamaMetrics = true;
        }
        if ("reset".equals(method.getName())) {
          hasReset = true;
        }
        if ("setEnabled".equals(method.getName())) {
          hasSetEnabled = true;
        }
      }
      assertTrue(hasGetStatistics, "Should have getStatistics method");
      assertTrue(hasGetPanamaMetrics, "Should have getPanamaMetrics method");
      assertTrue(hasReset, "Should have reset method");
      assertTrue(hasSetEnabled, "Should have setEnabled method");
    }

    @Test
    @DisplayName("Should have performance target constant")
    void shouldHavePerformanceTargetConstant() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaPerformanceMonitor");
      boolean hasTarget = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if ("SIMPLE_PANAMA_OPERATION_TARGET_NS".equals(field.getName())) {
          assertTrue(
              Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "SIMPLE_PANAMA_OPERATION_TARGET_NS should be static final");
          hasTarget = true;
          break;
        }
      }
      assertTrue(hasTarget, "Should have SIMPLE_PANAMA_OPERATION_TARGET_NS constant");
    }
  }

  @Nested
  @DisplayName("CompilationCache Tests")
  class CompilationCacheTests {

    @Test
    @DisplayName("Should be a final utility class")
    void shouldBeFinalUtilityClass() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "CompilationCache");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "CompilationCache should be final");

      // All public methods should be static
      for (final Method method : clazz.getDeclaredMethods()) {
        if (Modifier.isPublic(method.getModifiers())) {
          assertTrue(
              Modifier.isStatic(method.getModifiers()),
              "Public method " + method.getName() + " should be static");
        }
      }
    }

    @Test
    @DisplayName("Should have cache load and store methods")
    void shouldHaveCacheOperationMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "CompilationCache");
      boolean hasLoadFromCache = false;
      boolean hasStoreInCache = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("loadFromCache".equals(method.getName())) {
          hasLoadFromCache = true;
        }
        if ("storeInCache".equals(method.getName())) {
          hasStoreInCache = true;
        }
      }
      assertTrue(hasLoadFromCache, "Should have loadFromCache method");
      assertTrue(hasStoreInCache, "Should have storeInCache method");
    }

    @Test
    @DisplayName("Should have cache management methods")
    void shouldHaveCacheManagementMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "CompilationCache");
      boolean hasClear = false;
      boolean hasSetEnabled = false;
      boolean hasIsEnabled = false;
      boolean hasGetCacheDirectory = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("clear".equals(method.getName())) {
          hasClear = true;
        }
        if ("setEnabled".equals(method.getName())) {
          hasSetEnabled = true;
        }
        if ("isEnabled".equals(method.getName())) {
          hasIsEnabled = true;
        }
        if ("getCacheDirectory".equals(method.getName())) {
          hasGetCacheDirectory = true;
        }
      }
      assertTrue(hasClear, "Should have clear method");
      assertTrue(hasSetEnabled, "Should have setEnabled method");
      assertTrue(hasIsEnabled, "Should have isEnabled method");
      assertTrue(hasGetCacheDirectory, "Should have getCacheDirectory method");
    }

    @Test
    @DisplayName("Should have statistics methods")
    void shouldHaveStatsMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "CompilationCache");
      boolean hasGetStatistics = false;
      boolean hasGetHitRate = false;
      boolean hasGetPerformanceMetrics = false;
      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getStatistics".equals(method.getName())) {
          hasGetStatistics = true;
        }
        if ("getHitRate".equals(method.getName())) {
          hasGetHitRate = true;
        }
        if ("getPerformanceMetrics".equals(method.getName())) {
          hasGetPerformanceMetrics = true;
        }
      }
      assertTrue(hasGetStatistics, "Should have getStatistics method");
      assertTrue(hasGetHitRate, "Should have getHitRate method");
      assertTrue(hasGetPerformanceMetrics, "Should have getPerformanceMetrics method");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected performance classes should exist")
    void allExpectedClassesShouldExist() {
      final String[] expectedClasses = {
        "PanaNativeObjectPool",
        "AdvancedArenaManager",
        "PanamaPerformanceMonitor",
        "CompilationCache"
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
    @DisplayName("All classes should be in final state")
    void allClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] performanceClasses = {
        "PanaNativeObjectPool",
        "AdvancedArenaManager",
        "PanamaPerformanceMonitor",
        "CompilationCache"
      };

      for (final String className : performanceClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be a final class");
      }
    }

    @Test
    @DisplayName("Utility classes should have private constructor")
    void utilityClassesShouldHavePrivateConstructor() throws ClassNotFoundException {
      final String[] utilityClasses = {"PanamaPerformanceMonitor", "CompilationCache"};

      for (final String className : utilityClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasPrivateConstructor = false;
        try {
          final var constructor = clazz.getDeclaredConstructor();
          if (Modifier.isPrivate(constructor.getModifiers())) {
            hasPrivateConstructor = true;
          }
        } catch (final NoSuchMethodException e) {
          // No default constructor - also acceptable
          hasPrivateConstructor = true;
        }
        assertTrue(
            hasPrivateConstructor,
            className + " should have a private no-arg constructor or no default constructor");
      }
    }
  }

  @Nested
  @DisplayName("Thread Safety Pattern Tests")
  class ThreadSafetyPatternTests {

    @Test
    @DisplayName("Singletons should use thread-safe initialization")
    void singletonsShouldBeThreadSafe() throws ClassNotFoundException {
      final String[] singletonClasses = {"AdvancedArenaManager"};

      for (final String className : singletonClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);

        // Check for volatile instance field
        boolean hasVolatileInstance = false;
        boolean hasInstanceLock = false;

        for (final Field field : clazz.getDeclaredFields()) {
          if (field.getName().contains("instance") || field.getName().contains("INSTANCE")) {
            if (Modifier.isVolatile(field.getModifiers())) {
              hasVolatileInstance = true;
            }
          }
          if (field.getName().contains("LOCK") || field.getName().contains("lock")) {
            hasInstanceLock = true;
          }
        }

        assertTrue(
            hasVolatileInstance || hasInstanceLock,
            className + " should use thread-safe singleton pattern (volatile instance or lock)");
      }
    }

    @Test
    @DisplayName("Classes should use concurrent data structures")
    void classesShouldUseConcurrentDataStructures() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");

      boolean usesConcurrentMap = false;
      boolean usesAtomicFields = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String typeName = field.getType().getName();
        if (typeName.contains("ConcurrentHashMap")) {
          usesConcurrentMap = true;
        }
        if (typeName.contains("Atomic")) {
          usesAtomicFields = true;
        }
      }

      assertTrue(usesConcurrentMap, "PanaNativeObjectPool should use ConcurrentHashMap");
      assertTrue(usesAtomicFields, "PanaNativeObjectPool should use atomic fields");
    }
  }

  @Nested
  @DisplayName("Performance Constants Tests")
  class PerformanceConstantsTests {

    @Test
    @DisplayName("PanaNativeObjectPool should have pool size constants")
    void poolShouldHaveSizeConstants() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanaNativeObjectPool");

      boolean hasDefaultMaxPoolSize = false;
      boolean hasDefaultMinPoolSize = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("DEFAULT_MAX_POOL_SIZE".equals(field.getName())) {
          assertTrue(
              Modifier.isPublic(field.getModifiers())
                  && Modifier.isStatic(field.getModifiers())
                  && Modifier.isFinal(field.getModifiers()),
              "DEFAULT_MAX_POOL_SIZE should be public static final");
          hasDefaultMaxPoolSize = true;
        }
        if ("DEFAULT_MIN_POOL_SIZE".equals(field.getName())) {
          assertTrue(
              Modifier.isPublic(field.getModifiers())
                  && Modifier.isStatic(field.getModifiers())
                  && Modifier.isFinal(field.getModifiers()),
              "DEFAULT_MIN_POOL_SIZE should be public static final");
          hasDefaultMinPoolSize = true;
        }
      }

      assertTrue(hasDefaultMaxPoolSize, "Should have DEFAULT_MAX_POOL_SIZE constant");
      assertTrue(hasDefaultMinPoolSize, "Should have DEFAULT_MIN_POOL_SIZE constant");
    }
  }
}
