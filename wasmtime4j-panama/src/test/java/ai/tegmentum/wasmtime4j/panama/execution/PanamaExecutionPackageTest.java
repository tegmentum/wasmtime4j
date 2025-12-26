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
 * Comprehensive package-level tests for the Panama execution package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * execution-related classes in the panama.execution package using reflection-based testing to avoid
 * native library loading.
 *
 * @since 1.0.0
 */
@DisplayName("Panama Execution Package Tests")
public class PanamaExecutionPackageTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaExecutionPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.panama.execution.";

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
  @DisplayName("PanamaResourceLimiter Tests")
  class PanamaResourceLimiterTests {

    @Test
    @DisplayName("Should be a final class implementing ResourceLimiter")
    void shouldBeFinalAndImplementResourceLimiter() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaResourceLimiter");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaResourceLimiter should be final");

      // Check that it implements ResourceLimiter interface
      boolean implementsResourceLimiter = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("ResourceLimiter".equals(iface.getSimpleName())) {
          implementsResourceLimiter = true;
          break;
        }
      }
      assertTrue(
          implementsResourceLimiter, "PanamaResourceLimiter should implement ResourceLimiter");
    }

    @Test
    @DisplayName("Should have limiter state fields")
    void shouldHaveLimiterStateFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaResourceLimiter");
      boolean hasLimiterId = false;
      boolean hasConfig = false;
      boolean hasClosed = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("limiterId".equals(fieldName)) {
          hasLimiterId = true;
        }
        if ("config".equals(fieldName)) {
          hasConfig = true;
        }
        if ("closed".equals(fieldName)) {
          hasClosed = true;
        }
      }

      assertTrue(hasLimiterId, "Should have limiterId field");
      assertTrue(hasConfig, "Should have config field");
      assertTrue(hasClosed, "Should have closed field");
    }

    @Test
    @DisplayName("Should have static factory methods")
    void shouldHaveStaticFactoryMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaResourceLimiter");
      boolean hasCreate = false;
      boolean hasCreateDefault = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("create".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasCreate = true;
          assertEquals(1, method.getParameterCount(), "create should have 1 parameter");
        }
        if ("createDefault".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasCreateDefault = true;
          assertEquals(0, method.getParameterCount(), "createDefault should have no parameters");
        }
      }

      assertTrue(hasCreate, "Should have static create method");
      assertTrue(hasCreateDefault, "Should have static createDefault method");
    }

    @Test
    @DisplayName("Should have resource limit check methods")
    void shouldHaveResourceLimitCheckMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaResourceLimiter");
      boolean hasAllowMemoryGrow = false;
      boolean hasAllowTableGrow = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("allowMemoryGrow".equals(methodName)) {
          hasAllowMemoryGrow = true;
          assertEquals(2, method.getParameterCount(), "allowMemoryGrow should have 2 parameters");
        }
        if ("allowTableGrow".equals(methodName)) {
          hasAllowTableGrow = true;
          assertEquals(2, method.getParameterCount(), "allowTableGrow should have 2 parameters");
        }
      }

      assertTrue(hasAllowMemoryGrow, "Should have allowMemoryGrow method");
      assertTrue(hasAllowTableGrow, "Should have allowTableGrow method");
    }

    @Test
    @DisplayName("Should have stats and lifecycle methods")
    void shouldHaveStatsAndLifecycleMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaResourceLimiter");
      boolean hasGetId = false;
      boolean hasGetConfig = false;
      boolean hasGetStats = false;
      boolean hasResetStats = false;
      boolean hasClose = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getId".equals(methodName)) {
          hasGetId = true;
        }
        if ("getConfig".equals(methodName)) {
          hasGetConfig = true;
        }
        if ("getStats".equals(methodName)) {
          hasGetStats = true;
        }
        if ("resetStats".equals(methodName)) {
          hasResetStats = true;
        }
        if ("close".equals(methodName)) {
          hasClose = true;
        }
      }

      assertTrue(hasGetId, "Should have getId method");
      assertTrue(hasGetConfig, "Should have getConfig method");
      assertTrue(hasGetStats, "Should have getStats method");
      assertTrue(hasResetStats, "Should have resetStats method");
      assertTrue(hasClose, "Should have close method");
    }

    @Test
    @DisplayName("Should have static utility methods")
    void shouldHaveStaticUtilityMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaResourceLimiter");
      boolean hasGetLimiterCount = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getLimiterCount".equals(method.getName())
            && Modifier.isStatic(method.getModifiers())) {
          hasGetLimiterCount = true;
          break;
        }
      }

      assertTrue(hasGetLimiterCount, "Should have static getLimiterCount method");
    }
  }

  @Nested
  @DisplayName("PanamaFuelCallbackHandler Tests")
  class PanamaFuelCallbackHandlerTests {

    @Test
    @DisplayName("Should be a final class implementing FuelCallbackHandler")
    void shouldBeFinalAndImplementFuelCallbackHandler() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuelCallbackHandler");
      assertTrue(
          Modifier.isFinal(clazz.getModifiers()), "PanamaFuelCallbackHandler should be final");

      // Check that it implements FuelCallbackHandler interface
      boolean implementsFuelCallbackHandler = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("FuelCallbackHandler".equals(iface.getSimpleName())) {
          implementsFuelCallbackHandler = true;
          break;
        }
      }
      assertTrue(
          implementsFuelCallbackHandler,
          "PanamaFuelCallbackHandler should implement FuelCallbackHandler");
    }

    @Test
    @DisplayName("Should have handler state fields")
    void shouldHaveHandlerStateFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuelCallbackHandler");
      boolean hasHandlerId = false;
      boolean hasStoreId = false;
      boolean hasClosed = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("handlerId".equals(fieldName)) {
          hasHandlerId = true;
        }
        if ("storeId".equals(fieldName)) {
          hasStoreId = true;
        }
        if ("closed".equals(fieldName)) {
          hasClosed = true;
        }
      }

      assertTrue(hasHandlerId, "Should have handlerId field");
      assertTrue(hasStoreId, "Should have storeId field");
      assertTrue(hasClosed, "Should have closed field");
    }

    @Test
    @DisplayName("Should have static factory method")
    void shouldHaveStaticFactoryMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuelCallbackHandler");
      boolean hasCreateAutoRefill = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("createAutoRefill".equals(method.getName())
            && Modifier.isStatic(method.getModifiers())) {
          hasCreateAutoRefill = true;
          assertEquals(3, method.getParameterCount(), "createAutoRefill should have 3 parameters");
          break;
        }
      }

      assertTrue(hasCreateAutoRefill, "Should have static createAutoRefill method");
    }

    @Test
    @DisplayName("Should have callback handler methods")
    void shouldHaveCallbackHandlerMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuelCallbackHandler");
      boolean hasHandleExhaustion = false;
      boolean hasGetId = false;
      boolean hasGetStoreId = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("handleExhaustion".equals(methodName)) {
          hasHandleExhaustion = true;
          assertEquals(1, method.getParameterCount(), "handleExhaustion should have 1 parameter");
        }
        if ("getId".equals(methodName)) {
          hasGetId = true;
        }
        if ("getStoreId".equals(methodName)) {
          hasGetStoreId = true;
        }
      }

      assertTrue(hasHandleExhaustion, "Should have handleExhaustion method");
      assertTrue(hasGetId, "Should have getId method");
      assertTrue(hasGetStoreId, "Should have getStoreId method");
    }

    @Test
    @DisplayName("Should have stats and lifecycle methods")
    void shouldHaveStatsAndLifecycleMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuelCallbackHandler");
      boolean hasGetStats = false;
      boolean hasResetStats = false;
      boolean hasClose = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getStats".equals(methodName)) {
          hasGetStats = true;
        }
        if ("resetStats".equals(methodName)) {
          hasResetStats = true;
        }
        if ("close".equals(methodName)) {
          hasClose = true;
        }
      }

      assertTrue(hasGetStats, "Should have getStats method");
      assertTrue(hasResetStats, "Should have resetStats method");
      assertTrue(hasClose, "Should have close method");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected execution classes should exist")
    void allExpectedExecutionClassesShouldExist() {
      final String[] expectedClasses = {"PanamaResourceLimiter", "PanamaFuelCallbackHandler"};

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found execution class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("All execution classes should be final")
    void allExecutionClassesShouldBeFinal() throws ClassNotFoundException {
      final String[] executionClasses = {"PanamaResourceLimiter", "PanamaFuelCallbackHandler"};

      for (final String className : executionClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        assertTrue(Modifier.isFinal(clazz.getModifiers()), className + " should be final");
      }
    }

    @Test
    @DisplayName("All execution classes should have close method")
    void allExecutionClassesShouldHaveCloseMethod() throws ClassNotFoundException {
      final String[] executionClasses = {"PanamaResourceLimiter", "PanamaFuelCallbackHandler"};

      for (final String className : executionClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasClose = false;

        for (final Method method : clazz.getDeclaredMethods()) {
          if ("close".equals(method.getName())) {
            hasClose = true;
            break;
          }
        }

        assertTrue(hasClose, className + " should have close method");
      }
    }

    @Test
    @DisplayName("All execution classes should have private constructors")
    void allExecutionClassesShouldHavePrivateConstructors() throws ClassNotFoundException {
      final String[] executionClasses = {"PanamaResourceLimiter", "PanamaFuelCallbackHandler"};

      for (final String className : executionClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasPrivateConstructor = false;

        for (final var constructor : clazz.getDeclaredConstructors()) {
          if (Modifier.isPrivate(constructor.getModifiers())) {
            hasPrivateConstructor = true;
            break;
          }
        }

        assertTrue(hasPrivateConstructor, className + " should have private constructor");
      }
    }
  }
}
