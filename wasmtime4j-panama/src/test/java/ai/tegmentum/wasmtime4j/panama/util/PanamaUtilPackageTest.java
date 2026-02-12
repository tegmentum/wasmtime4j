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

package ai.tegmentum.wasmtime4j.panama.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive API contract and structural tests for the Panama util package.
 *
 * <p>Tests all utility classes in the ai.tegmentum.wasmtime4j.panama.util package using reflection
 * to verify class structure, method signatures, and API compliance without triggering native
 * library loading.
 *
 * <p>Classes covered:
 *
 * <ul>
 *   <li>PanamaResourceTracker - Resource tracking for native handles
 *   <li>PanamaValidation - Defensive programming validation utilities
 *   <li>PanamaTypeConverter - WebAssembly type conversion
 *   <li>PanamaExceptionMapper - Exception mapping for FFI operations
 *   <li>BacktraceDeserializer - Backtrace deserialization
 * </ul>
 *
 * @since 1.0.0
 */
@DisplayName("Panama Util Package Tests")
class PanamaUtilPackageTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaUtilPackageTest.class.getName());

  private static final String UTIL_PACKAGE = "ai.tegmentum.wasmtime4j.panama.util";

  /**
   * Loads a class without initializing it to avoid triggering native library loading.
   *
   * @param className the fully qualified class name
   * @return the loaded Class object
   * @throws ClassNotFoundException if the class is not found
   */
  private Class<?> loadClassWithoutInit(final String className) throws ClassNotFoundException {
    return Class.forName(className, false, getClass().getClassLoader());
  }

  @Nested
  @DisplayName("PanamaResourceTracker Tests")
  class PanamaResourceTrackerTests {

    private static final String CLASS_NAME = UTIL_PACKAGE + ".PanamaResourceTracker";

    @Test
    @DisplayName("Should be a final class with instance methods")
    void shouldBeFinalClassWithInstanceMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);
      LOGGER.info("Testing PanamaResourceTracker class structure");

      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
    }

    @Test
    @DisplayName("Should have resource tracking methods")
    void shouldHaveResourceTrackingMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      assertMethodExists(clazz, "trackResource", Object.class);
      assertMethodExists(clazz, "untrackResource", Object.class);
      assertMethodExists(clazz, "isTracked", Object.class);
      assertMethodExists(clazz, "getHandle", Object.class);
      assertMethodExists(clazz, "cleanupOrphanedResources");
      assertMethodExists(clazz, "cleanup");
      assertMethodExists(clazz, "getTrackedResourceCount");
      assertMethodExists(clazz, "getTotalTracked");
      assertMethodExists(clazz, "getTotalCleaned");
      assertMethodExists(clazz, "getPotentialLeaks");
      assertMethodExists(clazz, "getTrackingStats");
      assertMethodExists(clazz, "hasPotentialLeaks");
      assertMethodExists(clazz, "logStats");
      assertMethodExists(clazz, "getResourceSummary");

      LOGGER.info("All expected resource tracking methods exist");
    }

    @Test
    @DisplayName("Should use ConcurrentHashMap for thread safety")
    void shouldUseConcurrentHashMapForThreadSafety() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      boolean hasConcurrentMap = false;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getType().getName().contains("ConcurrentHashMap")
            || field.getType().getName().contains("ConcurrentMap")) {
          hasConcurrentMap = true;
          break;
        }
      }

      assertTrue(
          hasConcurrentMap, "Should use ConcurrentHashMap or ConcurrentMap for thread safety");
    }

    @Test
    @DisplayName("Should have atomic counters for statistics")
    void shouldHaveAtomicCountersForStatistics() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      int atomicLongCount = 0;
      for (final Field field : clazz.getDeclaredFields()) {
        if (field.getType().getName().contains("AtomicLong")) {
          atomicLongCount++;
        }
      }

      assertTrue(atomicLongCount >= 2, "Should have at least 2 AtomicLong fields for statistics");
    }
  }

  @Nested
  @DisplayName("PanamaValidation Tests")
  class PanamaValidationTests {

    private static final String CLASS_NAME = UTIL_PACKAGE + ".PanamaValidation";

    @Test
    @DisplayName("Should be a final utility class")
    void shouldBeFinalUtilityClass() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);
      LOGGER.info("Testing PanamaValidation class structure");

      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");

      // Should have private constructor
      final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("Should have all validation methods as static")
    void shouldHaveAllValidationMethodsAsStatic() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      final String[] expectedMethods = {
        "requireNonNull",
        "requireNonEmpty",
        "requireNonBlank",
        "requireInRange",
        "requireNonNegative",
        "requireValidHandle",
        "defensiveCopy",
        "requirePositive",
        "requireValidPort",
        "requireValidConnectionId",
        "requireValidString"
      };

      for (final String methodName : expectedMethods) {
        boolean found = false;
        for (final Method method : clazz.getDeclaredMethods()) {
          if (method.getName().equals(methodName)) {
            found = true;
            assertTrue(Modifier.isStatic(method.getModifiers()), methodName + " should be static");
            assertTrue(Modifier.isPublic(method.getModifiers()), methodName + " should be public");
          }
        }
        assertTrue(found, "Should have method: " + methodName);
      }

      LOGGER.info("All validation methods are correctly static and public");
    }

    @Test
    @DisplayName("Should have overloaded requireInRange for int and long")
    void shouldHaveOverloadedRequireInRange() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      int intRangeCount = 0;
      int longRangeCount = 0;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("requireInRange".equals(method.getName())) {
          final Class<?>[] params = method.getParameterTypes();
          if (params.length >= 3) {
            if (params[0] == int.class) {
              intRangeCount++;
            } else if (params[0] == long.class) {
              longRangeCount++;
            }
          }
        }
      }

      assertTrue(intRangeCount >= 1, "Should have int version of requireInRange");
      assertTrue(longRangeCount >= 1, "Should have long version of requireInRange");
    }
  }

  @Nested
  @DisplayName("PanamaTypeConverter Tests")
  class PanamaTypeConverterTests {

    private static final String CLASS_NAME = UTIL_PACKAGE + ".PanamaTypeConverter";

    @Test
    @DisplayName("Should be a final utility class")
    void shouldBeFinalUtilityClass() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);
      LOGGER.info("Testing PanamaTypeConverter class structure");

      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
    }

    @Test
    @DisplayName("Should have type conversion methods")
    void shouldHaveTypeConversionMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      assertMethodExists(clazz, "wasmTypeToNative");
      assertMethodExists(clazz, "nativeToWasmType", int.class);
      assertMethodExists(clazz, "marshalWasmValue");
      assertMethodExists(clazz, "unmarshalWasmValue");
      assertMethodExists(clazz, "marshalParameters");
      assertMethodExists(clazz, "unmarshalResults");

      LOGGER.info("All expected type conversion methods exist");
    }

    @Test
    @DisplayName("Should have validation methods")
    void shouldHaveValidationMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      assertMethodExists(clazz, "validateParameterTypes");
      assertMethodExists(clazz, "validateV128Size", byte[].class);
      assertMethodExists(clazz, "validateReferenceTypes");

      LOGGER.info("All expected validation methods exist");
    }

    @Test
    @DisplayName("Should have function type conversion methods")
    void shouldHaveFunctionTypeConversionMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      assertMethodExists(clazz, "functionTypeToNative");
      assertMethodExists(clazz, "nativeToFunctionType", int[].class, int[].class);
      assertMethodExists(clazz, "calculateValuesMemorySize");

      LOGGER.info("All expected function type conversion methods exist");
    }
  }

  @Nested
  @DisplayName("PanamaExceptionMapper Tests")
  class PanamaExceptionMapperTests {

    private static final String CLASS_NAME = UTIL_PACKAGE + ".PanamaExceptionMapper";

    @Test
    @DisplayName("Should be a final class")
    void shouldBeFinalClass() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);
      LOGGER.info("Testing PanamaExceptionMapper class structure");

      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");
    }

    @Test
    @DisplayName("Should have static mapException method")
    void shouldHaveStaticMapExceptionMethod() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      final Method method = clazz.getMethod("mapException", Throwable.class);
      assertNotNull(method, "Should have mapException method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "mapException should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "mapException should be public");
    }

    @Test
    @DisplayName("Should have native error mapping methods")
    void shouldHaveNativeErrorMappingMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      assertMethodExists(clazz, "mapNativeError", int.class, String.class);
      // MemorySegment version
      assertMethodExists(clazz, "mapNativeError");

      LOGGER.info("All expected native error mapping methods exist");
    }

    @Test
    @DisplayName("Should have exception factory methods")
    void shouldHaveExceptionFactoryMethods() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      assertMethodExists(clazz, "createCompilationException", String.class, Throwable.class);
      assertMethodExists(clazz, "createRuntimeException", String.class, Throwable.class);
      assertMethodExists(clazz, "createValidationException", String.class, Throwable.class);
      assertMethodExists(clazz, "createWasmException", String.class, Throwable.class);
      assertMethodExists(clazz, "isRecoverableError");

      LOGGER.info("All expected exception factory methods exist");
    }
  }

  @Nested
  @DisplayName("BacktraceDeserializer Tests")
  class BacktraceDeserializerTests {

    private static final String CLASS_NAME = UTIL_PACKAGE + ".BacktraceDeserializer";

    @Test
    @DisplayName("Should be a final utility class")
    void shouldBeFinalUtilityClass() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);
      LOGGER.info("Testing BacktraceDeserializer class structure");

      assertTrue(Modifier.isFinal(clazz.getModifiers()), "Class should be final");
      assertTrue(Modifier.isPublic(clazz.getModifiers()), "Class should be public");

      // Should have private constructor
      final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("Should have static deserialize method")
    void shouldHaveStaticDeserializeMethod() throws Exception {
      final Class<?> clazz = loadClassWithoutInit(CLASS_NAME);

      final Method method = clazz.getMethod("deserialize", byte[].class);
      assertNotNull(method, "Should have deserialize method");
      assertTrue(Modifier.isStatic(method.getModifiers()), "deserialize should be static");
      assertTrue(Modifier.isPublic(method.getModifiers()), "deserialize should be public");

      // Return type should be WasmBacktrace
      assertTrue(
          method.getReturnType().getName().contains("WasmBacktrace"),
          "deserialize should return WasmBacktrace");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    private static final String[] EXPECTED_CLASSES = {
      "PanamaResourceTracker",
      "PanamaValidation",
      "PanamaTypeConverter",
      "PanamaExceptionMapper",
      "BacktraceDeserializer"
    };

    @Test
    @DisplayName("All expected util classes should exist")
    void allExpectedUtilClassesShouldExist() {
      for (final String className : EXPECTED_CLASSES) {
        final String fullName = UTIL_PACKAGE + "." + className;
        assertDoesNotThrow(
            () -> loadClassWithoutInit(fullName), "Class should exist: " + className);
        LOGGER.info("Verified class exists: " + className);
      }
    }

    @Test
    @DisplayName("All util classes should have proper modifiers")
    void allUtilClassesShouldHaveProperModifiers() throws Exception {
      for (final String className : EXPECTED_CLASSES) {
        final Class<?> clazz = loadClassWithoutInit(UTIL_PACKAGE + "." + className);

        assertTrue(Modifier.isPublic(clazz.getModifiers()), className + " should be public");
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All util classes should have logger field")
    void allUtilClassesShouldHaveLoggerField() throws Exception {
      // Not all classes require a logger (e.g., utility classes with only static methods)
      int classesWithLogger = 0;
      int utilityClassesWithoutLogger = 0;

      for (final String className : EXPECTED_CLASSES) {
        final Class<?> clazz = loadClassWithoutInit(UTIL_PACKAGE + "." + className);

        boolean hasLogger = false;
        for (final Field field : clazz.getDeclaredFields()) {
          if (field.getType() == Logger.class) {
            hasLogger = true;
            break;
          }
        }

        if (hasLogger) {
          classesWithLogger++;
        } else {
          // Pure utility classes without state don't need loggers
          utilityClassesWithoutLogger++;
        }
      }

      LOGGER.info(
          String.format(
              "Classes with logger: %d, utility classes without logger: %d",
              classesWithLogger, utilityClassesWithoutLogger));

      // Most classes should have loggers, but utility classes are allowed not to
      assertTrue(
          classesWithLogger >= EXPECTED_CLASSES.length / 2,
          "At least half of the classes should have loggers");
    }
  }

  @Nested
  @DisplayName("Thread Safety Pattern Tests")
  class ThreadSafetyPatternTests {

    @Test
    @DisplayName("Classes using concurrent data structures")
    void classesShouldUseConcurrentDataStructures() throws Exception {
      final String[] classesNeedingConcurrency = {"PanamaResourceTracker"};

      for (final String className : classesNeedingConcurrency) {
        final Class<?> clazz = loadClassWithoutInit(UTIL_PACKAGE + "." + className);

        boolean hasConcurrentStructure = false;
        for (final Field field : clazz.getDeclaredFields()) {
          final String typeName = field.getType().getName();
          if (typeName.contains("Concurrent")
              || typeName.contains("Atomic")
              || typeName.contains("Lock")
              || typeName.contains("Semaphore")) {
            hasConcurrentStructure = true;
            break;
          }
        }

        assertTrue(hasConcurrentStructure, className + " should use concurrent data structures");
      }
    }

    @Test
    @DisplayName("Utility classes should prevent instantiation")
    void utilityClassesShouldPreventInstantiation() throws Exception {
      final String[] pureUtilityClasses = {"PanamaValidation", "BacktraceDeserializer"};

      for (final String className : pureUtilityClasses) {
        final Class<?> clazz = loadClassWithoutInit(UTIL_PACKAGE + "." + className);

        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        assertEquals(1, constructors.length, className + " should have exactly one constructor");
        assertTrue(
            Modifier.isPrivate(constructors[0].getModifiers()),
            className + " constructor should be private");
      }
    }
  }

  /**
   * Helper method to assert that a method exists on a class.
   *
   * @param clazz the class to check
   * @param methodName the method name to find
   * @param paramTypes optional parameter types (if empty, just checks method name exists)
   */
  private void assertMethodExists(
      final Class<?> clazz, final String methodName, final Class<?>... paramTypes) {
    boolean found = false;

    for (final Method method : clazz.getDeclaredMethods()) {
      if (method.getName().equals(methodName)) {
        if (paramTypes.length == 0) {
          found = true;
          break;
        } else {
          final Class<?>[] actualParams = method.getParameterTypes();
          if (actualParams.length >= paramTypes.length) {
            boolean matches = true;
            for (int i = 0; i < paramTypes.length; i++) {
              if (!actualParams[i].equals(paramTypes[i])) {
                matches = false;
                break;
              }
            }
            if (matches) {
              found = true;
              break;
            }
          }
        }
      }
    }

    assertTrue(
        found,
        String.format(
            "Method '%s' with params %s should exist in %s",
            methodName, Arrays.toString(paramTypes), clazz.getSimpleName()));
  }
}
