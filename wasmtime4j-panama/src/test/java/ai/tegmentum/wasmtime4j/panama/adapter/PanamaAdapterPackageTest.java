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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Logger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive package-level tests for the Panama adapter package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * adapter classes in the panama.adapter package using reflection-based testing to avoid native
 * library loading.
 *
 * @since 1.0.0
 */
@DisplayName("Panama Adapter Package Tests")
public class PanamaAdapterPackageTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaAdapterPackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.adapter.";
  private static final String PANAMA_PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.panama.adapter.";

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
  @DisplayName("WasmMemoryToMemoryAdapter Tests")
  class WasmMemoryToMemoryAdapterTests {

    @Test
    @DisplayName("Should be a final class implementing Memory")
    void shouldBeFinalAndImplementMemory() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmMemoryToMemoryAdapter");
      assertTrue(
          Modifier.isFinal(clazz.getModifiers()), "WasmMemoryToMemoryAdapter should be final");

      // Check that it implements Memory interface
      boolean implementsMemory = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("Memory".equals(iface.getSimpleName())) {
          implementsMemory = true;
          break;
        }
      }
      assertTrue(implementsMemory, "WasmMemoryToMemoryAdapter should implement Memory");
    }

    @Test
    @DisplayName("Should have delegate field")
    void shouldHaveDelegateField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmMemoryToMemoryAdapter");
      boolean hasDelegate = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("delegate".equals(field.getName())) {
          hasDelegate = true;
          assertTrue(
              Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "delegate should be private final");
          break;
        }
      }

      assertTrue(hasDelegate, "Should have delegate field");
    }

    @Test
    @DisplayName("Should have Memory interface methods")
    void shouldHaveMemoryInterfaceMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmMemoryToMemoryAdapter");
      boolean hasGetSize = false;
      boolean hasGetSizeInBytes = false;
      boolean hasGrow = false;
      boolean hasRead = false;
      boolean hasWrite = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getSize".equals(methodName)) {
          hasGetSize = true;
          assertEquals(long.class, method.getReturnType(), "getSize should return long");
        }
        if ("getSizeInBytes".equals(methodName)) {
          hasGetSizeInBytes = true;
          assertEquals(long.class, method.getReturnType(), "getSizeInBytes should return long");
        }
        if ("grow".equals(methodName)) {
          hasGrow = true;
        }
        if ("read".equals(methodName)) {
          hasRead = true;
        }
        if ("write".equals(methodName)) {
          hasWrite = true;
        }
      }

      assertTrue(hasGetSize, "Should have getSize method");
      assertTrue(hasGetSizeInBytes, "Should have getSizeInBytes method");
      assertTrue(hasGrow, "Should have grow method");
      assertTrue(hasRead, "Should have read method");
      assertTrue(hasWrite, "Should have write method");
    }

    @Test
    @DisplayName("Should have getDelegate method")
    void shouldHaveGetDelegateMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmMemoryToMemoryAdapter");
      boolean hasGetDelegate = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("getDelegate".equals(method.getName())) {
          hasGetDelegate = true;
          break;
        }
      }

      assertTrue(hasGetDelegate, "Should have getDelegate method");
    }

    @Test
    @DisplayName("Should have public constructor")
    void shouldHavePublicConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmMemoryToMemoryAdapter");
      boolean hasPublicConstructor = false;

      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          hasPublicConstructor = true;
          assertEquals(
              1, constructor.getParameterCount(), "Constructor should have 1 parameter (delegate)");
          break;
        }
      }

      assertTrue(hasPublicConstructor, "Should have public constructor");
    }
  }

  @Nested
  @DisplayName("WasmGlobalToGlobalAdapter Tests")
  class WasmGlobalToGlobalAdapterTests {

    @Test
    @DisplayName("Should be a final class implementing Global")
    void shouldBeFinalAndImplementGlobal() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmGlobalToGlobalAdapter");
      assertTrue(
          Modifier.isFinal(clazz.getModifiers()), "WasmGlobalToGlobalAdapter should be final");

      // Check that it implements Global interface
      boolean implementsGlobal = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("Global".equals(iface.getSimpleName())) {
          implementsGlobal = true;
          break;
        }
      }
      assertTrue(implementsGlobal, "WasmGlobalToGlobalAdapter should implement Global");
    }

    @Test
    @DisplayName("Should have delegate field")
    void shouldHaveDelegateField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmGlobalToGlobalAdapter");
      boolean hasDelegate = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("delegate".equals(field.getName())) {
          hasDelegate = true;
          assertTrue(
              Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "delegate should be private final");
          break;
        }
      }

      assertTrue(hasDelegate, "Should have delegate field");
    }

    @Test
    @DisplayName("Should have Global interface methods")
    void shouldHaveGlobalInterfaceMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmGlobalToGlobalAdapter");
      boolean hasGetValue = false;
      boolean hasSetValue = false;
      boolean hasIsMutable = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getValue".equals(methodName)) {
          hasGetValue = true;
        }
        if ("setValue".equals(methodName)) {
          hasSetValue = true;
        }
        if ("isMutable".equals(methodName)) {
          hasIsMutable = true;
          assertEquals(boolean.class, method.getReturnType(), "isMutable should return boolean");
        }
      }

      assertTrue(hasGetValue, "Should have getValue method");
      assertTrue(hasSetValue, "Should have setValue method");
      assertTrue(hasIsMutable, "Should have isMutable method");
    }

    @Test
    @DisplayName("Should have public constructor")
    void shouldHavePublicConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmGlobalToGlobalAdapter");
      boolean hasPublicConstructor = false;

      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          hasPublicConstructor = true;
          break;
        }
      }

      assertTrue(hasPublicConstructor, "Should have public constructor");
    }
  }

  @Nested
  @DisplayName("WasmFunctionToFunctionAdapter Tests")
  class WasmFunctionToFunctionAdapterTests {

    @Test
    @DisplayName("Should be a final class implementing Function")
    void shouldBeFinalAndImplementFunction() throws ClassNotFoundException {
      final Class<?> clazz =
          loadClassWithoutInit(PANAMA_PACKAGE_PREFIX + "WasmFunctionToFunctionAdapter");
      assertTrue(
          Modifier.isFinal(clazz.getModifiers()), "WasmFunctionToFunctionAdapter should be final");

      // Check that it implements Function interface
      boolean implementsFunction = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("Function".equals(iface.getSimpleName())) {
          implementsFunction = true;
          break;
        }
      }
      assertTrue(implementsFunction, "WasmFunctionToFunctionAdapter should implement Function");
    }

    @Test
    @DisplayName("Should have delegate field")
    void shouldHaveDelegateField() throws ClassNotFoundException {
      final Class<?> clazz =
          loadClassWithoutInit(PANAMA_PACKAGE_PREFIX + "WasmFunctionToFunctionAdapter");
      boolean hasDelegate = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("delegate".equals(field.getName())) {
          hasDelegate = true;
          assertTrue(
              Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "delegate should be private final");
          break;
        }
      }

      assertTrue(hasDelegate, "Should have delegate field");
    }

    @Test
    @DisplayName("Should have Function interface methods")
    void shouldHaveFunctionInterfaceMethods() throws ClassNotFoundException {
      final Class<?> clazz =
          loadClassWithoutInit(PANAMA_PACKAGE_PREFIX + "WasmFunctionToFunctionAdapter");
      boolean hasCall = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("call".equals(method.getName())) {
          hasCall = true;
          break;
        }
      }

      assertTrue(hasCall, "Should have call method");
    }

    @Test
    @DisplayName("Should have public constructor")
    void shouldHavePublicConstructor() throws ClassNotFoundException {
      final Class<?> clazz =
          loadClassWithoutInit(PANAMA_PACKAGE_PREFIX + "WasmFunctionToFunctionAdapter");
      boolean hasPublicConstructor = false;

      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          hasPublicConstructor = true;
          break;
        }
      }

      assertTrue(hasPublicConstructor, "Should have public constructor");
    }
  }

  @Nested
  @DisplayName("WasmTableToTableAdapter Tests")
  class WasmTableToTableAdapterTests {

    @Test
    @DisplayName("Should be a final class implementing Table")
    void shouldBeFinalAndImplementTable() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmTableToTableAdapter");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "WasmTableToTableAdapter should be final");

      // Check that it implements Table interface
      boolean implementsTable = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("Table".equals(iface.getSimpleName())) {
          implementsTable = true;
          break;
        }
      }
      assertTrue(implementsTable, "WasmTableToTableAdapter should implement Table");
    }

    @Test
    @DisplayName("Should have delegate field")
    void shouldHaveDelegateField() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmTableToTableAdapter");
      boolean hasDelegate = false;

      for (final Field field : clazz.getDeclaredFields()) {
        if ("delegate".equals(field.getName())) {
          hasDelegate = true;
          assertTrue(
              Modifier.isPrivate(field.getModifiers()) && Modifier.isFinal(field.getModifiers()),
              "delegate should be private final");
          break;
        }
      }

      assertTrue(hasDelegate, "Should have delegate field");
    }

    @Test
    @DisplayName("Should have Table interface methods")
    void shouldHaveTableInterfaceMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmTableToTableAdapter");
      boolean hasGetSize = false;
      boolean hasGet = false;
      boolean hasSet = false;
      boolean hasGrow = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getSize".equals(methodName)) {
          hasGetSize = true;
          assertEquals(long.class, method.getReturnType(), "getSize should return long");
        }
        if ("get".equals(methodName)) {
          hasGet = true;
        }
        if ("set".equals(methodName)) {
          hasSet = true;
        }
        if ("grow".equals(methodName)) {
          hasGrow = true;
        }
      }

      assertTrue(hasGetSize, "Should have getSize method");
      assertTrue(hasGet, "Should have get method");
      assertTrue(hasSet, "Should have set method");
      assertTrue(hasGrow, "Should have grow method");
    }

    @Test
    @DisplayName("Should have public constructor")
    void shouldHavePublicConstructor() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "WasmTableToTableAdapter");
      boolean hasPublicConstructor = false;

      for (final Constructor<?> constructor : clazz.getConstructors()) {
        if (Modifier.isPublic(constructor.getModifiers())) {
          hasPublicConstructor = true;
          break;
        }
      }

      assertTrue(hasPublicConstructor, "Should have public constructor");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    /**
     * Returns the fully qualified class names of all adapter classes used by the Panama module.
     * Three adapters are in the shared base package; WasmFunctionToFunctionAdapter remains in the
     * Panama adapter package because it has Panama-specific implementation details.
     */
    private String[] allAdapterClassNames() {
      return new String[] {
        PACKAGE_PREFIX + "WasmMemoryToMemoryAdapter",
        PACKAGE_PREFIX + "WasmGlobalToGlobalAdapter",
        PANAMA_PACKAGE_PREFIX + "WasmFunctionToFunctionAdapter",
        PACKAGE_PREFIX + "WasmTableToTableAdapter"
      };
    }

    @Test
    @DisplayName("All expected adapter classes should exist")
    void allExpectedAdapterClassesShouldExist() {
      for (final String fqcn : allAdapterClassNames()) {
        try {
          final Class<?> clazz = loadClassWithoutInit(fqcn);
          assertNotNull(clazz, fqcn + " should exist");
          LOGGER.fine("Found adapter class: " + fqcn);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + fqcn, e);
        }
      }
    }

    @Test
    @DisplayName("All adapter classes should be final")
    void allAdapterClassesShouldBeFinal() throws ClassNotFoundException {
      for (final String fqcn : allAdapterClassNames()) {
        final Class<?> clazz = loadClassWithoutInit(fqcn);
        assertTrue(
            Modifier.isFinal(clazz.getModifiers()), clazz.getSimpleName() + " should be final");
      }
    }

    @Test
    @DisplayName("All adapter classes should not be interfaces")
    void allAdapterClassesShouldNotBeInterfaces() throws ClassNotFoundException {
      for (final String fqcn : allAdapterClassNames()) {
        final Class<?> clazz = loadClassWithoutInit(fqcn);
        assertFalse(clazz.isInterface(), clazz.getSimpleName() + " should not be an interface");
      }
    }

    @Test
    @DisplayName("All adapter classes should have public constructors")
    void allAdapterClassesShouldHavePublicConstructors() throws ClassNotFoundException {
      for (final String fqcn : allAdapterClassNames()) {
        final Class<?> clazz = loadClassWithoutInit(fqcn);
        boolean hasPublicConstructor = false;

        for (final Constructor<?> constructor : clazz.getConstructors()) {
          if (Modifier.isPublic(constructor.getModifiers())) {
            hasPublicConstructor = true;
            break;
          }
        }

        assertTrue(hasPublicConstructor, clazz.getSimpleName() + " should have public constructor");
      }
    }

    @Test
    @DisplayName("All adapter classes should have delegate field")
    void allAdapterClassesShouldHaveDelegateField() throws ClassNotFoundException {
      for (final String fqcn : allAdapterClassNames()) {
        final Class<?> clazz = loadClassWithoutInit(fqcn);
        boolean hasDelegate = false;

        for (final Field field : clazz.getDeclaredFields()) {
          if ("delegate".equals(field.getName())) {
            hasDelegate = true;
            break;
          }
        }

        assertTrue(hasDelegate, clazz.getSimpleName() + " should have delegate field");
      }
    }
  }
}
