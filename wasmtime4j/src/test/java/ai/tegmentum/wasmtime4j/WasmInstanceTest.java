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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmInstance interface.
 *
 * <p>WasmInstance represents an instantiated WebAssembly module with access to its exports. This
 * test verifies the interface structure and method signatures.
 */
@DisplayName("WasmInstance Interface Tests")
class WasmInstanceTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmInstance.class.isInterface(), "WasmInstance should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmInstance.class.getModifiers()), "WasmInstance should be public");
    }

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasmInstance.class),
          "WasmInstance should extend Closeable");
    }
  }

  // ========================================================================
  // Export Getter Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Export Getter Methods Tests")
  class ExportGetterMethodsTests {

    @Test
    @DisplayName("should have getFunction method")
    void shouldHaveGetFunctionMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getFunction", String.class);
      assertNotNull(method, "getFunction method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getFunction should return Optional");
    }

    @Test
    @DisplayName("should have getMemory method")
    void shouldHaveGetMemoryMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getMemory", String.class);
      assertNotNull(method, "getMemory method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getMemory should return Optional");
    }

    @Test
    @DisplayName("should have getTable method")
    void shouldHaveGetTableMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getTable", String.class);
      assertNotNull(method, "getTable method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getTable should return Optional");
    }

    @Test
    @DisplayName("should have getGlobal method")
    void shouldHaveGetGlobalMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getGlobal", String.class);
      assertNotNull(method, "getGlobal method should exist");
      assertEquals(Optional.class, method.getReturnType(), "getGlobal should return Optional");
    }

    @Test
    @DisplayName("should have getDefaultMemory method")
    void shouldHaveGetDefaultMemoryMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getDefaultMemory");
      assertNotNull(method, "getDefaultMemory method should exist");
      assertEquals(
          Optional.class, method.getReturnType(), "getDefaultMemory should return Optional");
    }
  }

  // ========================================================================
  // Export Name List Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Export Name List Methods Tests")
  class ExportNameListMethodsTests {

    @Test
    @DisplayName("should have getFunctionNames method")
    void shouldHaveGetFunctionNamesMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getFunctionNames");
      assertNotNull(method, "getFunctionNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getFunctionNames should return List");
    }

    @Test
    @DisplayName("should have getMemoryNames method")
    void shouldHaveGetMemoryNamesMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getMemoryNames");
      assertNotNull(method, "getMemoryNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getMemoryNames should return List");
    }

    @Test
    @DisplayName("should have getTableNames method")
    void shouldHaveGetTableNamesMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getTableNames");
      assertNotNull(method, "getTableNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getTableNames should return List");
    }

    @Test
    @DisplayName("should have getGlobalNames method")
    void shouldHaveGetGlobalNamesMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getGlobalNames");
      assertNotNull(method, "getGlobalNames method should exist");
      assertEquals(List.class, method.getReturnType(), "getGlobalNames should return List");
    }
  }

  // ========================================================================
  // Other Instance Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Other Instance Methods Tests")
  class OtherInstanceMethodsTests {

    @Test
    @DisplayName("should have getExports method")
    void shouldHaveGetExportsMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getExports");
      assertNotNull(method, "getExports method should exist");
      assertEquals(Map.class, method.getReturnType(), "getExports should return Map");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isValid should return boolean");
    }

    @Test
    @DisplayName("should have getModule method")
    void shouldHaveGetModuleMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getModule");
      assertNotNull(method, "getModule method should exist");
      assertEquals(Module.class, method.getReturnType(), "getModule should return Module");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "close should return void");
    }
  }

  // ========================================================================
  // Inner Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportType Enum Tests")
  class ExportTypeEnumTests {

    @Test
    @DisplayName("should have ExportType inner enum")
    void shouldHaveExportTypeEnum() {
      Class<?>[] innerClasses = WasmInstance.class.getDeclaredClasses();
      boolean hasExportType =
          Arrays.stream(innerClasses)
              .anyMatch(c -> c.getSimpleName().equals("ExportType") && c.isEnum());
      assertTrue(hasExportType, "WasmInstance should have ExportType inner enum");
    }

    @Test
    @DisplayName("ExportType should have FUNCTION value")
    void exportTypeShouldHaveFunctionValue() {
      WasmInstance.ExportType exportType = WasmInstance.ExportType.FUNCTION;
      assertNotNull(exportType, "ExportType.FUNCTION should exist");
    }

    @Test
    @DisplayName("ExportType should have MEMORY value")
    void exportTypeShouldHaveMemoryValue() {
      WasmInstance.ExportType exportType = WasmInstance.ExportType.MEMORY;
      assertNotNull(exportType, "ExportType.MEMORY should exist");
    }

    @Test
    @DisplayName("ExportType should have TABLE value")
    void exportTypeShouldHaveTableValue() {
      WasmInstance.ExportType exportType = WasmInstance.ExportType.TABLE;
      assertNotNull(exportType, "ExportType.TABLE should exist");
    }

    @Test
    @DisplayName("ExportType should have GLOBAL value")
    void exportTypeShouldHaveGlobalValue() {
      WasmInstance.ExportType exportType = WasmInstance.ExportType.GLOBAL;
      assertNotNull(exportType, "ExportType.GLOBAL should exist");
    }

    @Test
    @DisplayName("ExportType should have exactly 4 values")
    void exportTypeShouldHaveExactly4Values() {
      assertEquals(
          4, WasmInstance.ExportType.values().length, "ExportType should have exactly 4 values");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("getFunction should have 1 String parameter")
    void getFunctionShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getFunction", String.class);
      assertEquals(1, method.getParameterCount(), "getFunction should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getMemory should have 1 String parameter")
    void getMemoryShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getMemory", String.class);
      assertEquals(1, method.getParameterCount(), "getMemory should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("getDefaultMemory should have no parameters")
    void getDefaultMemoryShouldHaveNoParameters() throws NoSuchMethodException {
      final Method method = WasmInstance.class.getMethod("getDefaultMemory");
      assertEquals(0, method.getParameterCount(), "getDefaultMemory should have 0 parameters");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getFunction",
              "getMemory",
              "getTable",
              "getGlobal",
              "getDefaultMemory",
              "getFunctionNames",
              "getMemoryNames",
              "getTableNames",
              "getGlobalNames",
              "getExports",
              "isValid",
              "getModule",
              "close");

      Set<String> actualMethods =
          Arrays.stream(WasmInstance.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "WasmInstance should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 13 declared methods")
    void shouldHaveAtLeast13DeclaredMethods() {
      assertTrue(
          WasmInstance.class.getDeclaredMethods().length >= 13,
          "WasmInstance should have at least 13 methods");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Closeable interface")
    void shouldExtendCloseableInterface() {
      Class<?>[] interfaces = WasmInstance.class.getInterfaces();
      boolean extendsCloseable = Arrays.stream(interfaces).anyMatch(i -> i.equals(Closeable.class));
      assertTrue(extendsCloseable, "WasmInstance should extend Closeable interface");
    }
  }
}
