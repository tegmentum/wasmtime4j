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

package ai.tegmentum.wasmtime4j.panama.type;

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
 * Comprehensive package-level tests for the Panama type package.
 *
 * <p>This test class verifies the API contracts, class structure, and method signatures of all
 * type-related classes in the panama.type package using reflection-based testing to avoid native
 * library loading.
 *
 * @since 1.0.0
 */
@DisplayName("Panama Type Package Tests")
public class PanamaTypePackageTest {

  private static final Logger LOGGER = Logger.getLogger(PanamaTypePackageTest.class.getName());

  private static final String PACKAGE_PREFIX = "ai.tegmentum.wasmtime4j.panama.type.";

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
  @DisplayName("PanamaFuncType Tests")
  class PanamaFuncTypeTests {

    @Test
    @DisplayName("Should be a final class implementing FuncType")
    void shouldBeFinalAndImplementFuncType() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuncType");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaFuncType should be final");

      // Check that it implements FuncType interface
      boolean implementsFuncType = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("FuncType".equals(iface.getSimpleName())) {
          implementsFuncType = true;
          break;
        }
      }
      assertTrue(implementsFuncType, "PanamaFuncType should implement FuncType");
    }

    @Test
    @DisplayName("Should have params and results fields")
    void shouldHaveParamsAndResultsFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuncType");
      boolean hasParams = false;
      boolean hasResults = false;
      boolean hasArena = false;
      boolean hasNativeHandle = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("params".equals(fieldName)) {
          hasParams = true;
        }
        if ("results".equals(fieldName)) {
          hasResults = true;
        }
        if ("arena".equals(fieldName)) {
          hasArena = true;
        }
        if ("nativeHandle".equals(fieldName)) {
          hasNativeHandle = true;
        }
      }

      assertTrue(hasParams, "Should have params field");
      assertTrue(hasResults, "Should have results field");
      assertTrue(hasArena, "Should have arena field");
      assertTrue(hasNativeHandle, "Should have nativeHandle field");
    }

    @Test
    @DisplayName("Should have getter methods")
    void shouldHaveGetterMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuncType");
      boolean hasGetParams = false;
      boolean hasGetResults = false;
      boolean hasGetKind = false;
      boolean hasGetNativeHandle = false;
      boolean hasGetArena = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getParams".equals(methodName)) {
          hasGetParams = true;
        }
        if ("getResults".equals(methodName)) {
          hasGetResults = true;
        }
        if ("getKind".equals(methodName)) {
          hasGetKind = true;
        }
        if ("getNativeHandle".equals(methodName)) {
          hasGetNativeHandle = true;
        }
        if ("getArena".equals(methodName)) {
          hasGetArena = true;
        }
      }

      assertTrue(hasGetParams, "Should have getParams method");
      assertTrue(hasGetResults, "Should have getResults method");
      assertTrue(hasGetKind, "Should have getKind method");
      assertTrue(hasGetNativeHandle, "Should have getNativeHandle method");
      assertTrue(hasGetArena, "Should have getArena method");
    }

    @Test
    @DisplayName("Should have fromNative static factory method")
    void shouldHaveFromNativeMethod() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuncType");
      boolean hasFromNative = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        if ("fromNative".equals(method.getName()) && Modifier.isStatic(method.getModifiers())) {
          hasFromNative = true;
          assertEquals(2, method.getParameterCount(), "fromNative should have 2 parameters");
          break;
        }
      }

      assertTrue(hasFromNative, "Should have static fromNative method");
    }

    @Test
    @DisplayName("Should have equals, hashCode, and toString")
    void shouldHaveObjectMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaFuncType");
      boolean hasEquals = false;
      boolean hasHashCode = false;
      boolean hasToString = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("equals".equals(methodName)) {
          hasEquals = true;
        }
        if ("hashCode".equals(methodName)) {
          hasHashCode = true;
        }
        if ("toString".equals(methodName)) {
          hasToString = true;
        }
      }

      assertTrue(hasEquals, "Should have equals method");
      assertTrue(hasHashCode, "Should have hashCode method");
      assertTrue(hasToString, "Should have toString method");
    }
  }

  @Nested
  @DisplayName("PanamaMemoryType Tests")
  class PanamaMemoryTypeTests {

    @Test
    @DisplayName("Should be a final class implementing MemoryType")
    void shouldBeFinalAndImplementMemoryType() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaMemoryType");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaMemoryType should be final");

      // Check that it implements MemoryType interface
      boolean implementsMemoryType = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("MemoryType".equals(iface.getSimpleName())) {
          implementsMemoryType = true;
          break;
        }
      }
      assertTrue(implementsMemoryType, "PanamaMemoryType should implement MemoryType");
    }

    @Test
    @DisplayName("Should have memory constraint fields")
    void shouldHaveMemoryConstraintFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaMemoryType");
      boolean hasMinimum = false;
      boolean hasMaximum = false;
      boolean hasIs64Bit = false;
      boolean hasIsShared = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("minimum".equals(fieldName)) {
          hasMinimum = true;
        }
        if ("maximum".equals(fieldName)) {
          hasMaximum = true;
        }
        if ("is64Bit".equals(fieldName)) {
          hasIs64Bit = true;
        }
        if ("isShared".equals(fieldName)) {
          hasIsShared = true;
        }
      }

      assertTrue(hasMinimum, "Should have minimum field");
      assertTrue(hasMaximum, "Should have maximum field");
      assertTrue(hasIs64Bit, "Should have is64Bit field");
      assertTrue(hasIsShared, "Should have isShared field");
    }

    @Test
    @DisplayName("Should have getter methods")
    void shouldHaveGetterMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaMemoryType");
      boolean hasGetMinimum = false;
      boolean hasGetMaximum = false;
      boolean hasIs64Bit = false;
      boolean hasIsShared = false;
      boolean hasGetKind = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getMinimum".equals(methodName)) {
          hasGetMinimum = true;
        }
        if ("getMaximum".equals(methodName)) {
          hasGetMaximum = true;
        }
        if ("is64Bit".equals(methodName)) {
          hasIs64Bit = true;
        }
        if ("isShared".equals(methodName)) {
          hasIsShared = true;
        }
        if ("getKind".equals(methodName)) {
          hasGetKind = true;
        }
      }

      assertTrue(hasGetMinimum, "Should have getMinimum method");
      assertTrue(hasGetMaximum, "Should have getMaximum method");
      assertTrue(hasIs64Bit, "Should have is64Bit method");
      assertTrue(hasIsShared, "Should have isShared method");
      assertTrue(hasGetKind, "Should have getKind method");
    }
  }

  @Nested
  @DisplayName("PanamaGlobalType Tests")
  class PanamaGlobalTypeTests {

    @Test
    @DisplayName("Should be a final class implementing GlobalType")
    void shouldBeFinalAndImplementGlobalType() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaGlobalType");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaGlobalType should be final");

      // Check that it implements GlobalType interface
      boolean implementsGlobalType = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("GlobalType".equals(iface.getSimpleName())) {
          implementsGlobalType = true;
          break;
        }
      }
      assertTrue(implementsGlobalType, "PanamaGlobalType should implement GlobalType");
    }

    @Test
    @DisplayName("Should have valueType and mutability fields")
    void shouldHaveTypeAndMutabilityFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaGlobalType");
      boolean hasValueType = false;
      boolean hasIsMutable = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("valueType".equals(fieldName)) {
          hasValueType = true;
        }
        if ("isMutable".equals(fieldName)) {
          hasIsMutable = true;
        }
      }

      assertTrue(hasValueType, "Should have valueType field");
      assertTrue(hasIsMutable, "Should have isMutable field");
    }

    @Test
    @DisplayName("Should have getter methods")
    void shouldHaveGetterMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaGlobalType");
      boolean hasGetValueType = false;
      boolean hasIsMutable = false;
      boolean hasGetKind = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getValueType".equals(methodName)) {
          hasGetValueType = true;
        }
        if ("isMutable".equals(methodName)) {
          hasIsMutable = true;
        }
        if ("getKind".equals(methodName)) {
          hasGetKind = true;
        }
      }

      assertTrue(hasGetValueType, "Should have getValueType method");
      assertTrue(hasIsMutable, "Should have isMutable method");
      assertTrue(hasGetKind, "Should have getKind method");
    }
  }

  @Nested
  @DisplayName("PanamaTableType Tests")
  class PanamaTableTypeTests {

    @Test
    @DisplayName("Should be a final class implementing TableType")
    void shouldBeFinalAndImplementTableType() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaTableType");
      assertTrue(Modifier.isFinal(clazz.getModifiers()), "PanamaTableType should be final");

      // Check that it implements TableType interface
      boolean implementsTableType = false;
      for (final Class<?> iface : clazz.getInterfaces()) {
        if ("TableType".equals(iface.getSimpleName())) {
          implementsTableType = true;
          break;
        }
      }
      assertTrue(implementsTableType, "PanamaTableType should implement TableType");
    }

    @Test
    @DisplayName("Should have table constraint fields")
    void shouldHaveTableConstraintFields() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaTableType");
      boolean hasElementType = false;
      boolean hasMinimum = false;
      boolean hasMaximum = false;

      for (final Field field : clazz.getDeclaredFields()) {
        final String fieldName = field.getName();
        if ("elementType".equals(fieldName)) {
          hasElementType = true;
        }
        if ("minimum".equals(fieldName)) {
          hasMinimum = true;
        }
        if ("maximum".equals(fieldName)) {
          hasMaximum = true;
        }
      }

      assertTrue(hasElementType, "Should have elementType field");
      assertTrue(hasMinimum, "Should have minimum field");
      assertTrue(hasMaximum, "Should have maximum field");
    }

    @Test
    @DisplayName("Should have getter methods")
    void shouldHaveGetterMethods() throws ClassNotFoundException {
      final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + "PanamaTableType");
      boolean hasGetElementType = false;
      boolean hasGetMinimum = false;
      boolean hasGetMaximum = false;
      boolean hasGetKind = false;

      for (final Method method : clazz.getDeclaredMethods()) {
        final String methodName = method.getName();
        if ("getElementType".equals(methodName)) {
          hasGetElementType = true;
        }
        if ("getMinimum".equals(methodName)) {
          hasGetMinimum = true;
        }
        if ("getMaximum".equals(methodName)) {
          hasGetMaximum = true;
        }
        if ("getKind".equals(methodName)) {
          hasGetKind = true;
        }
      }

      assertTrue(hasGetElementType, "Should have getElementType method");
      assertTrue(hasGetMinimum, "Should have getMinimum method");
      assertTrue(hasGetMaximum, "Should have getMaximum method");
      assertTrue(hasGetKind, "Should have getKind method");
    }
  }

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All expected type classes should exist")
    void allExpectedTypeClassesShouldExist() {
      final String[] expectedClasses = {
        "PanamaFuncType", "PanamaMemoryType", "PanamaGlobalType", "PanamaTableType"
      };

      for (final String className : expectedClasses) {
        try {
          final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
          assertNotNull(clazz, className + " should exist");
          LOGGER.fine("Found type class: " + className);
        } catch (final ClassNotFoundException e) {
          throw new AssertionError("Expected class not found: " + className, e);
        }
      }
    }

    @Test
    @DisplayName("All type classes should have native methods for type info")
    void allTypeClassesShouldHaveNativeMethods() throws ClassNotFoundException {
      final String[] typeClasses = {
        "PanamaFuncType", "PanamaMemoryType", "PanamaGlobalType", "PanamaTableType"
      };

      for (final String className : typeClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasNativeMethod = false;

        for (final Method method : clazz.getDeclaredMethods()) {
          if (Modifier.isNative(method.getModifiers())) {
            hasNativeMethod = true;
            break;
          }
        }

        assertTrue(hasNativeMethod, className + " should have native method for type info");
      }
    }

    @Test
    @DisplayName("All type classes should implement WasmType (have getKind method)")
    void allTypeClassesShouldHaveGetKindMethod() throws ClassNotFoundException {
      final String[] typeClasses = {
        "PanamaFuncType", "PanamaMemoryType", "PanamaGlobalType", "PanamaTableType"
      };

      for (final String className : typeClasses) {
        final Class<?> clazz = loadClassWithoutInit(PACKAGE_PREFIX + className);
        boolean hasGetKind = false;

        for (final Method method : clazz.getDeclaredMethods()) {
          if ("getKind".equals(method.getName())) {
            hasGetKind = true;
            break;
          }
        }

        assertTrue(hasGetKind, className + " should have getKind method");
      }
    }
  }
}
